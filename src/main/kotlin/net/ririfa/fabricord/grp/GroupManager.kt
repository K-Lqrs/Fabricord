package net.ririfa.fabricord.grp

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.ririfa.fabricord.Fabricord.Companion.grpFile
import net.ririfa.fabricord.discord.DiscordBotManager.server
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.translation.adapt
import net.ririfa.fabricord.utils.ShortUUID
import net.ririfa.fabricord.utils.ShortUUIDTypeAdapter
import net.ririfa.fabricord.utils.UUIDTypeAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.util.UUID

object GroupManager {
	private val logger: Logger = LoggerFactory.getLogger(this::class.simpleName)

	private val groups = mutableMapOf<ShortUUID, Group>()

	private val gson: Gson = GsonBuilder()
		.registerTypeAdapter(UUID::class.java, UUIDTypeAdapter())
		.registerTypeAdapter(ShortUUID::class.java, ShortUUIDTypeAdapter())
		.setPrettyPrinting()
		.create()

	//TODO: Use LangMan for messages
	fun initialize() {
		if (Files.exists(grpFile)) {
			logger.info("Loading from $grpFile")

			try {
				val jsonString = Files.readString(grpFile)
				val type = object : TypeToken<Map<ShortUUID, Group>>() {}.type
				val loaded = gson.fromJson<Map<ShortUUID, Group>>(jsonString, type)
				groups.clear()
				groups.putAll(loaded)
				logger.info("Loaded ${groups.size} groups")
			} catch (e: Exception) {
				logger.error("Failed to load groups.json: ${e.message}", e)
			}
		} else {
			logger.info("groups.json not found, starting fresh.")
		}
	}

	fun save() {
		try {
			val jsonString = gson.toJson(groups)
			Files.writeString(grpFile, jsonString)
			logger.info("Saved groups to $grpFile")
		} catch (e: Exception) {
			logger.error("Failed to save groups.json: ${e.message}", e)
		}
	}

	fun createGroup(name: String, owner: UUID, open: Boolean = false, addMembers: List<UUID> = emptyList()): Group {
		val id = ShortUUID.randomUUID()
		val group = Group(id, name, owner, mutableSetOf(owner).also { it.addAll(addMembers) }, open)
		groups[id] = group
		return group
	}

	fun getGroupById(id: ShortUUID): Group? {
		return groups[id]
	}

	fun getGroupsByName(name: String): List<Group> {
		return groups.values.filter { it.name == name }
	}

	fun deleteGroup(id: ShortUUID) {
		groups.remove(id)
	}

	object Command {
		fun register() {
			CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
				dispatcher.register(
					literal("grp")
						// ルート実行時 => ヘルプ表示
						.executes { context ->
							showHelp(context.source)
							1
						}

						// /grp help
						.then(
							literal("help")
								.executes { ctx ->
									showHelp(ctx.source)
									1
								}
						)

						// Main logic for this command

						// /grp create <groupName> [<players...>]
						.then(
							literal("create")
								.then(
									argument("groupName", StringArgumentType.string())
										// /grp create <groupName>
										.executes { ctx ->
											val src = ctx.source
											val player = src.player ?: return@executes 0
											val ap = player.adapt()

											val groupName = StringArgumentType.getString(ctx, "groupName")
											val group = createGroup(groupName, player.uuid)
											// GroupCreate + GroupName + GroupID
											src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.GroupCreated, group.name, group.id.toShortString()))
											1
										}

										// /grp create <groupName> <players...>
										.then(
											argument("players", greedyString())
												.executes { ctx ->
													val src = ctx.source
													val player = src.player ?: return@executes 0
													val ap = player.adapt()

													val groupName = StringArgumentType.getString(ctx, "groupName")
													val playersArg = StringArgumentType.getString(ctx, "players")
													val membersToAdd = mutableListOf<UUID>()

													val names = playersArg.split(" ")
													names.forEach { name ->
														try {
															val uuid = UUID.fromString(name)
															membersToAdd.add(uuid)
														} catch (_: IllegalArgumentException) {
															val uuid = findPlayerUUIDByName(name)
															if (uuid != null) {
																membersToAdd.add(uuid)
															} else {
																src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.PlayerNotFound, name))
															}
														}
													}

													val group = createGroup(groupName, player.uuid, false, membersToAdd)
													src.sendMessage(
														ap.getMessage(
															FabricordMessageKey.System.GRP.GroupCreatedWithMembers,
															group.name,
															group.id.toShortString(),
															group.members.size
														)
													)
													1
												}
										)
								)
						)

						// /grp join <groupNameOrID>
						.then(
							literal("join")
								.then(
									argument("targetGroup", StringArgumentType.string())
										.executes { ctx ->
											val src = ctx.source
											val player = src.player ?: return@executes 0
											val input = StringArgumentType.getString(ctx, "targetGroup")

											// 1) ShortUUIDとして解釈できればID検索
											val shortId = try {
												ShortUUID.fromShortString(input)
											} catch (_: Exception) {
												null
											}

											if (shortId != null) {
												val groupById = getGroupById(shortId)
												if (groupById == null) {
													src.sendMessage(Text.literal("No group found for ID: ${input}"))
												} else {
													joinGroup(player, groupById, src)
												}
												return@executes 1
											}

											// 2) グループ名として検索
											val groupsByName = getGroupsByName(input)
											when {
												groupsByName.isEmpty() -> {
													src.sendMessage(Text.literal("No group found with name: $input"))
												}

												groupsByName.size > 1 -> {
													src.sendMessage(
														Text.literal(
															"Multiple groups found with that name. Please specify group ID instead."
														)
													)
												}

												else -> {
													joinGroup(player, groupsByName.first(), src)
												}
											}
											1
										}
								)
						)

						// /grp del <groupIdOrName>
						.then(
							literal("del")
								.then(
									argument("targetGroup", StringArgumentType.string())
										.executes { ctx ->
											val src = ctx.source
											val player = src.player ?: return@executes 0
											val input = StringArgumentType.getString(ctx, "targetGroup")

											// 同様にIDか名前か判別
											val shortId = try {
												ShortUUID.fromShortString(input)
											} catch (_: Exception) {
												null
											}
											if (shortId != null) {
												val g = getGroupById(shortId)
												if (g != null) {
													if (g.owner == player.uuid) {
														deleteGroup(shortId)
														src.sendMessage(Text.literal("Deleted group: ${g.name} (ID: ${g.id.toShortString()})"))
													} else {
														src.sendMessage(Text.literal("You are not the owner of this group."))
													}
												} else {
													src.sendMessage(Text.literal("No group found for ID: $input"))
												}
												return@executes 1
											}

											// 名前検索→複数ヒットならエラー
											val found = getGroupsByName(input)
											when {
												found.isEmpty() -> {
													src.sendMessage(Text.literal("No group found with name: $input"))
												}

												found.size > 1 -> {
													src.sendMessage(Text.literal("Multiple groups with that name. Use group ID."))
												}

												else -> {
													val group = found.first()
													if (group.owner == player.uuid) {
														deleteGroup(group.id)
														src.sendMessage(Text.literal("Deleted group: ${group.name} (ID: ${group.id.toShortString()})"))
													} else {
														src.sendMessage(Text.literal("You are not the owner of this group."))
													}
												}
											}
											1
										}
								)
						)
				)
			}
		}

		private fun joinGroup(player: ServerPlayerEntity, group: Group, source: ServerCommandSource) {
			if (group.members.contains(player.uuid)) {
				source.sendMessage(Text.literal("You are already a member of this group."))
				return
			}

			if (group.open) {
				group.members.add(player.uuid)
				source.sendMessage(Text.literal("Joined group: ${group.name} (ID: ${group.id.toShortString()})"))
			} else {
				if (group.joinRequests.contains(player.uuid)) {
					source.sendMessage(Text.literal("You have already requested to join this group."))
				} else {
					group.joinRequests.add(player.uuid)
					source.sendMessage(Text.literal("Join request sent to ${group.name}. The group owner must approve your request."))
				}
			}
		}


		private fun showHelp(source: ServerCommandSource) {
			var msg = Component.text("Group commands:")
				.append(Component.text("/grp create <groupName> [<players...>]"))
				.append(Component.text("/grp join <groupNameOrID>"))
				.append(Component.text("/grp del <groupIdOrName>"))

			val s = GsonComponentSerializer.gson().serialize(msg)
			source.sendMessage(Text.Serialization.fromJson(s, server?.registryManager ?: return))
		}

		private fun findPlayerUUIDByName(name: String): UUID? {
			val p = server?.playerManager?.playerList?.firstOrNull { it.name.string.equals(name, ignoreCase = true) } ?: return null
			return p.uuid
		}
	}
}