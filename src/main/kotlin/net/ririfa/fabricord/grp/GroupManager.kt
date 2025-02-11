@file:Suppress("DuplicatedCode", "SpellCheckingInspection")

package net.ririfa.fabricord.grp

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType.bool
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.ririfa.fabricord.Fabricord
import net.ririfa.fabricord.Fabricord.Companion.grpFile
import net.ririfa.fabricord.discord.DiscordBotManager.server
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.translation.FabricordMessageProvider
import net.ririfa.fabricord.translation.adapt
import net.ririfa.fabricord.utils.ShortUUID
import net.ririfa.fabricord.utils.ShortUUIDTypeAdapter
import net.ririfa.fabricord.utils.UUIDTypeAdapter
import net.ririfa.langman.LangMan
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*

object GroupManager {
	private val logger: Logger = LoggerFactory.getLogger(this::class.simpleName)
	lateinit var langMan: LangMan<FabricordMessageProvider, Text>

	@JvmField
	internal val groups = mutableListOf<Group>()
	@JvmField
	val playerInGroupedChat = mutableMapOf<UUID, ShortUUID>()
	val playerDefaultGroup = mutableMapOf<UUID, ShortUUID?>()

	private val gson: Gson = GsonBuilder()
		.registerTypeAdapter(UUID::class.java, UUIDTypeAdapter())
		.registerTypeAdapter(ShortUUID::class.java, ShortUUIDTypeAdapter())
		.setPrettyPrinting()
		.create()

	fun initialize() {
		if (!Files.exists(grpFile)) {
			Files.createFile(grpFile)
			Files.write(grpFile, "[]".toByteArray(StandardCharsets.UTF_8), StandardOpenOption.WRITE)
		}

		langMan = Fabricord.instance.langMan

		logger.info(langMan.getSysMessage(FabricordMessageKey.System.GRP.LoadingGroupFile, grpFile))

		try {
			val jsonString = Files.readString(grpFile)
			val type = object : TypeToken<List<Group>>() {}.type
			val loaded = gson.fromJson<List<Group>>(jsonString, type)
			groups.clear()
			groups.addAll(loaded)
			logger.info(langMan.getSysMessage(FabricordMessageKey.System.GRP.LoadedGroups, groups.size))
		} catch (e: Exception) {
			logger.error(langMan.getSysMessage(FabricordMessageKey.System.GRP.FailedToLoadGroupFile, e.message ?: ""), e)
		}
	}

	fun save() {
		try {
			val gjs = gson.toJson(groups)
			Files.writeString(grpFile, gjs)
			PlayerGroupStorage.save()
			logger.info(langMan.getSysMessage(FabricordMessageKey.System.GRP.GroupFileSaved, grpFile))
		} catch (e: Exception) {
			logger.error(langMan.getSysMessage(FabricordMessageKey.System.GRP.FailedToSaveGroupFile, e.message ?: ""), e)
		}
	}

	fun createGroup(name: String, owner: UUID, open: Boolean = false, addMembers: List<UUID> = emptyList()): Group {
		val id = ShortUUID.randomUUID()
		val group = Group(id, name, owner, mutableSetOf(owner).also { it.addAll(addMembers) }, open)
		groups.add(group)
		save()
		return group
	}

	@JvmStatic
	fun getGroupById(id: ShortUUID): Group? {
		return groups.firstOrNull { it.id == id }
	}

	fun getGroupsByName(name: String): List<Group> {
		return groups.filter { it.name.equals(name, ignoreCase = true) }
	}

	fun deleteGroup(id: ShortUUID) {
		groups.removeIf { it.id == id }
		save()
	}

	internal fun findPlayerByName(name: String): ServerPlayerEntity? {
		return server?.playerManager?.playerList?.firstOrNull { it.name.string.equals(name, ignoreCase = true) }
	}

	fun joinGroup(player: ServerPlayerEntity, group: Group, source: ServerCommandSource) {
		val ap = player.adapt()
		if (group.members.contains(player.uuid)) {
			source.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.YouAreAlreadyMember))
			return
		}

		if (group.open) {
			group.members.add(player.uuid)
			source.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.JoinedToGroup, group.name, group.id))
		} else {
			if (group.joinRequests.contains(player.uuid)) {
				source.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.YouAlreadySentRequest))
			} else {
				group.joinRequests.add(player.uuid)
				source.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.JoinRequestSent, group.name))
			}
		}
	}

	fun showHelp(source: ServerCommandSource) {
		var msg = Component.text("Group commands:")
			.appendNewline()
			.append(Component.text("/grp create <groupName> [<players...>]"))
			.appendNewline()
			.append(Component.text("/grp join <groupNameOrID>"))
			.appendNewline()
			.append(Component.text("/grp del <groupNameOrID>"))
			.appendNewline()


		val s = GsonComponentSerializer.gson().serialize(msg)
		source.sendMessage(Text.Serialization.fromJson(s, server?.registryManager ?: return))
	}

	enum class Commands(val register: (dispatcher: CommandDispatcher<ServerCommandSource>) -> Unit) {
		HELP({ dispatcher ->
			dispatcher.register(
				literal("grp")
					.executes {
						showHelp(it.source)
						1
					}
			)
		}),

		CREATE({ dispatcher ->
			dispatcher.register(
				literal("grp")
					.then(
						literal("create")
							.then(
								argument("groupName", StringArgumentType.string())
									.then(
										argument("isOpen", bool())
											.executes { // ← ここでメンバーなしの場合を処理
												val source = it.source
												val player = source.player ?: run {
													source.sendMessage(Text.of("You must be a player to use this command"))
													return@executes 0
												}
												val ap = player.adapt()

												val name = it.getArgument("groupName", String::class.java)
												val isOpen = it.getArgument("isOpen", Boolean::class.java)

												val group = createGroup(name, player.uuid, open = isOpen)

												var rawMessage: Component = Component.text(ap.getRawMessage(FabricordMessageKey.System.GRP.GroupCreated))

												val idComponent = Component.text(group.id.toShortString(), TextColor.color(0x55CDFC))
													.clickEvent(ClickEvent.suggestCommand(group.id.toShortString()))
													.hoverEvent(HoverEvent.showText(Component.text(ap.getRawMessage(FabricordMessageKey.System.GRP.ClickToCopyID))))

												rawMessage = rawMessage.replaceText { builder ->
													builder.match("%name").replacement(Component.text(group.name))
												}
												rawMessage = rawMessage.replaceText { builder ->
													builder.match("%id").replacement(idComponent)
												}

												val json = GsonComponentSerializer.gson().serialize(rawMessage)
												source.sendMessage(Text.Serialization.fromJson(json, server?.registryManager))
												1
											}
											.then(
												argument("members", greedyString())
													.suggests { _, builder ->
														server?.playerManager?.playerList?.forEach {
															builder.suggest(it.name.string)
														}
														builder.buildFuture()
													}
													.executes {
														val source = it.source
														val player = source.player ?: run {
															source.sendMessage(Text.of("You must be a player to use this command"))
															return@executes 0
														}
														val ap = player.adapt()

														val name = it.getArgument("groupName", String::class.java)
														val isOpen = it.getArgument("isOpen", Boolean::class.java)
														val playerNames = it.getArgument("members", String::class.java)
															.split(" ").filter { it.isNotBlank() }

														val players = playerNames.mapNotNull { findPlayerByName(it) }
														val group = createGroup(name, player.uuid, open = isOpen, addMembers = players.map { it.uuid })

														source.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.GroupCreated, group.name, group.id))
														1
													}
											)
									)
							)
					)
			)
		}),

		DELETE({ dispatcher ->
			dispatcher.register(
				literal("grp")
					.then(
						literal("del")
							.then(
								argument("groupNameOrID", StringArgumentType.string())
									.executes {
										val source = it.source
										val player = source.player ?: run {
											source.sendMessage(Text.of("You must be a player to use this command"))
											return@executes 0
										}
										val ap = player.adapt()
										val nameOrID = it.getArgument("groupNameOrID", String::class.java)
										val group = try {
											getGroupById(ShortUUID.fromString(nameOrID))
										} catch (e: Exception) {
											null
										} ?: getGroupsByName(nameOrID).firstOrNull()
										if (group == null) {
											source.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.GroupNotFound, nameOrID))
											return@executes 0
										}
										deleteGroup(group.id)
										source.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.GroupDeleted, group.name, group.id))
										1
									}
							)
					)
			)
		}),

		SETDEFAULT({ dispatcher ->
			dispatcher.register(
				literal("grp")
					.then(
						literal("setDefault")
							.then(
								argument("groupNameOrID", StringArgumentType.string())
									.executes {
										val source = it.source
										val player = source.player ?: run {
											source.sendMessage(Text.of("You must be a player to use this command"))
											return@executes 0
										}
										val ap = player.adapt()
										val nameOrID = it.getArgument("groupNameOrID", String::class.java)
										val group = try {
											getGroupById(ShortUUID.fromString(nameOrID))
										} catch (_: Exception) {
											null
										} ?: getGroupsByName(nameOrID).firstOrNull()
										if (group == null) {
											source.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.GroupNotFound, nameOrID))
											return@executes 0
										}

										PlayerGroupStorage.setDefaultGroup(player.uuid, group.id.toShortString())

										source.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.DefaultGroupSet, group.name, group.id.toShortString()))
										1
									}
							)
					)
			)
		}),

		SWITCH({ dispatcher ->
			dispatcher.register(
				literal("grp")
					.then(
						literal("switch")
							.executes {
								val source = it.source
								val player = source.player ?: run {
									source.sendMessage(Text.of("You must be a player to use this command"))
									return@executes 0
								}
								val ap = player.adapt()

								val currentGroup = PlayerGroupStorage.getCurrentGroup(player.uuid)
								if (currentGroup == null) {
									// グローバル → デフォルトグループに切り替え
									val defaultGroup = PlayerGroupStorage.getDefaultGroup(player.uuid)
									if (defaultGroup != null && groups.any { it.id.toShortString() == defaultGroup }) {
										PlayerGroupStorage.setCurrentGroup(player.uuid, defaultGroup)
										source.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.SwitchedToGroupChat, groups.first { it.id.toShortString() == defaultGroup }.name))
									} else {
										source.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.NoDefaultGroupSet))
									}
								} else {
									PlayerGroupStorage.setCurrentGroup(player.uuid, null)
									source.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.SwitchedToGlobalChat))
								}
								1
							}
							.then(
								argument("groupNameOrID", StringArgumentType.string())
									.executes {
										val source = it.source
										val player = source.player ?: run {
											source.sendMessage(Text.of("You must be a player to use this command"))
											return@executes 0
										}
										val ap = player.adapt()

										val nameOrID = it.getArgument("groupNameOrID", String::class.java)
										val group = try {
											getGroupById(ShortUUID.fromString(nameOrID))
										} catch (_: Exception) {
											null
										} ?: getGroupsByName(nameOrID).firstOrNull()

										if (group == null) {
											source.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.GroupNotFound, nameOrID))
											return@executes 0
										}

										val isCurrentlyInGroup = PlayerGroupStorage.getCurrentGroup(player.uuid) == group.id.toShortString()
										if (isCurrentlyInGroup) {
											PlayerGroupStorage.setCurrentGroup(player.uuid, null)
											source.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.SwitchedToGlobalChat))
										} else {
											PlayerGroupStorage.setCurrentGroup(player.uuid, group.id.toShortString())
											source.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.SwitchedToGroupChat, group.name))
										}
										1
									}
							)
					)
			)
		}),

	}
}