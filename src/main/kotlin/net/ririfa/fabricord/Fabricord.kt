package net.ririfa.fabricord

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.message.MessageType
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.Text
import net.ririfa.fabricord.discord.DiscordBotManager
import net.ririfa.fabricord.discord.DiscordEmbed
import net.ririfa.fabricord.discord.DiscordPlayerEventHandler
import net.ririfa.fabricord.discord.DiscordPlayerEventHandler.handleMCMessage
import net.ririfa.fabricord.grp.GroupManager
import net.ririfa.fabricord.grp.GroupManager.Command.findPlayerUUIDByName
import net.ririfa.fabricord.grp.GroupManager.Command.joinGroup
import net.ririfa.fabricord.grp.GroupManager.Command.showHelp
import net.ririfa.fabricord.grp.GroupManager.createGroup
import net.ririfa.fabricord.grp.GroupManager.deleteGroup
import net.ririfa.fabricord.grp.GroupManager.getGroupById
import net.ririfa.fabricord.grp.GroupManager.getGroupsByName
import net.ririfa.fabricord.grp.GroupManager.playerInGroupedChat
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.translation.FabricordMessageProvider
import net.ririfa.fabricord.translation.adapt
import net.ririfa.fabricord.utils.ShortUUID
import net.ririfa.fabricord.utils.Utils
import net.ririfa.fabricord.utils.Utils.copyResourceToFile
import net.ririfa.fabricord.utils.Utils.getNullableBoolean
import net.ririfa.fabricord.utils.Utils.getNullableString
import net.ririfa.langman.InitType
import net.ririfa.langman.LangMan
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.layout.PatternLayout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.io.path.notExists

class Fabricord : DedicatedServerModInitializer {
	companion object {
		lateinit var instance: Fabricord

		private const val MOD_ID = "fabricord"
		val logger: Logger = LoggerFactory.getLogger(Fabricord::class.simpleName)

		private val loader: FabricLoader = FabricLoader.getInstance()
		val executorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
		internal lateinit var ca: ConsoleTrackerAppender

		private val serverDir: Path = loader.gameDir.toRealPath()
		private val modDir: Path = serverDir.resolve(MOD_ID)
		private val configFile: Path = modDir.resolve("config.yml")
		private val langDir: File = modDir.resolve("lang").toFile()
		private val availableLang = listOf("en", "ja")

		val grpFile: Path = modDir.resolve("groups.json")

		private val yaml = Yaml()
		private var initializeIsDone = false
		private val localChatToggled = mutableListOf<UUID>()

		//region Configurations
		// Required
		var botToken: String? = null
		var logChannelID: String? = null

		// Optional
		var enableConsoleLog: Boolean? = false
		var consoleLogChannelID: String? = null

		var serverStartMessage: String? = null
		var serverStopMessage: String? = null
		var playerJoinMessage: String? = null
		var playerLeaveMessage: String? = null

		var botOnlineStatus: String? = null
		var botActivityStatus: String? = null
		var botActivityMessage: String? = null

		var messageStyle: String? = null
		var allowMentions: Boolean? = true
		var webHookId: String? = null
		//endregion
	}

	lateinit var langMan: LangMan<FabricordMessageProvider, Text>
	lateinit var server: MinecraftServer

	override fun onInitializeServer() {
		instance = this
		if (!langDir.exists()) {
			langDir.mkdirs()
		}
		extractLangFiles()

		langMan = LangMan.createNew(
			{ Text.of(it) },
			FabricordMessageKey::class,
			true
		)

		langMan.init(InitType.YAML, langDir, availableLang)

		langMan.messages.forEach { (key, value) ->
			logger.info("Loaded message: $key: $value")
		}

		logger.info(langMan.getSysMessage(FabricordMessageKey.System.Initializing))
		DiscordPlayerEventHandler.init()
		checkRequiredFilesAndDirectories()
		loadConfig()

		if (requiredNullCheck()) {
			logger.error(langMan.getSysMessage(FabricordMessageKey.System.MissingRequiredProp.ITEM1))
			logger.error(langMan.getSysMessage(FabricordMessageKey.System.MissingRequiredProp.ITEM2))
			logger.error(langMan.getSysMessage(FabricordMessageKey.System.MissingRequiredProp.ITEM3, configFile))
			return
		}
		nullCheck()

		registerEvents()

		initializeIsDone = true
		logger.info(langMan.getSysMessage(FabricordMessageKey.System.Initialized))
		if (langMan.isDebug) {
			availableLang.forEach { lang ->
				langMan.logMissingKeys(lang)
			}
		}
	}

	private fun registerEvents() {
		ca = ConsoleTrackerAppender("FabricordConsoleTracker")
		val rootLogger = LogManager.getRootLogger() as org.apache.logging.log4j.core.Logger
		rootLogger.addAppender(ca)

		ServerMessageEvents.CHAT_MESSAGE.register(ServerMessageEvents.ChatMessage { message, sender, params ->
			val uuid = sender.uuid

			if (uuid in localChatToggled) return@ChatMessage

			if (uuid in playerInGroupedChat) {
				val groupID = playerInGroupedChat[uuid] ?: return@ChatMessage
				val group = getGroupById(groupID) ?: return@ChatMessage
				val members = group.members
				val textToSend = Text.literal("${sender.name.string}: ${message.content.string}")

				members.forEach { member ->
					val entity = server.playerManager.getPlayer(member) ?: return@forEach
					entity.sendMessage(textToSend, false)
				}


				return@ChatMessage
			}

			val content = message.content.string
			handleMCMessage(sender, content)
		})

		ServerPlayConnectionEvents.JOIN.register(ServerPlayConnectionEvents.Join { handler, _, _ ->
			val player = handler.player

			if (!DiscordBotManager.botIsInitialized) {
				player.networkHandler.disconnect(
					player.adapt().getMessage(FabricordMessageKey.System.Discord.BotNotInitialized)
				)
				return@Join
			}

			executorService.submit {
				DiscordEmbed.sendPlayerJoinEmbed(player)
			}
		})

		ServerPlayConnectionEvents.DISCONNECT.register(ServerPlayConnectionEvents.Disconnect { handler, _ ->
			executorService.submit {
				val player = handler.player
				DiscordEmbed.sendPlayerLeftEmbed(player)
			}
		})

		ServerLifecycleEvents.SERVER_STARTED.register { server ->
			GroupManager.initialize()
			executorService.submit {
				try {
					val s = DiscordBotManager.init(server)
					this.server = s
				} catch (e: Exception) {
					logger.error(langMan.getSysMessage(FabricordMessageKey.System.Discord.FailedToStartBot), e)
					server.stop(false)
				}
			}
		}

		ServerLifecycleEvents.SERVER_STOPPING.register { _ ->
			try {
				DiscordBotManager.stopBot()
				executorService.shutdown()
				if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
					executorService.shutdownNow()
				}

				rootLogger.removeAppender(ca)
				ca.stop()
				GroupManager.save()
			} catch (e: Exception) {
				logger.error(langMan.getSysMessage(FabricordMessageKey.System.Discord.FailedToStopBot), e)
			}
		}

		CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
			dispatcher.register(
				literal("lc")
					.executes { context ->
						val player = context.source.player ?: return@executes 0
						val uuid = player.uuid
						val current = uuid in localChatToggled
						val newState = !current
						if (newState) {
							localChatToggled.add(uuid)
						} else {
							localChatToggled.remove(uuid)
						}
						player.sendMessage(
							player.adapt().getMessage(FabricordMessageKey.System.SwitchedLocalChatState, newState),
							false
						)
						return@executes 1
					}
					.then(
						argument("message", greedyString())
							.executes { context ->
								val player = context.source.player ?: return@executes 0
								val message = context.getArgument("message", String::class.java)
								val signedMessage = Utils.createSignedMessage(player, message)
								val parameters = MessageType.params(MessageType.CHAT, player)

								player.server.playerManager.broadcast(
									signedMessage,
									player,
									parameters
								)

								return@executes 1
							}
					)
			)
			dispatcher.register(
				literal("grp")
					// region /grp help
					.executes { context ->
						showHelp(context.source)
						1
					}

					.then(
						literal("help")
							.executes { ctx ->
								showHelp(ctx.source)
								1
							}
					)
					// endregion

					// region /grp create
					.then(
						literal("create")
							.then(
								argument("groupName", greedyString())
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
										argument("players", StringArgumentType.string())
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
					// endregion

					//region /grp join <groupNameOrID>
					.then(
						literal("join")
							.then(
								argument("targetGroup", StringArgumentType.string())
									.executes { ctx ->
										val src = ctx.source
										val player = src.player ?: return@executes 0
										val ap = player.adapt()
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
												src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.NoGroupFoundWithID, input))
											} else {
												if (!groupById.open) {
													groupById.addJoinRequest(player.uuid)
													return@executes 1
												}
												joinGroup(player, groupById, src)
											}
											return@executes 1
										}

										// 2) グループ名として検索
										val groupsByName = getGroupsByName(input)
										when {
											groupsByName.isEmpty() -> {
												src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.NoGroupFoundWithName, input))
											}

											groupsByName.size > 1 -> {
												src.sendMessage(
													ap.getMessage(
														FabricordMessageKey.System.GRP.MultipleGroupsFound
													)
												)
											}

											!groupsByName.first().open -> {
												//TODO: Send request to join
												return@executes 1
											}

											else -> {
												joinGroup(player, groupsByName.first(), src)
											}
										}
										1
									}
							)
					)
					//endregion

					//region /grp leave <groupNameOrID>
					.then(
						literal("leave")
							.then(
								argument("targetGroup", StringArgumentType.string())
									.executes { ctx ->
										val src = ctx.source
										val player = src.player ?: return@executes 0
										val ap = player.adapt()
										val input = StringArgumentType.getString(ctx, "targetGroup")

										val shortId = try {
											ShortUUID.fromShortString(input)
										} catch (_: Exception) {
											null
										}

										if (shortId != null) {
											val group = getGroupById(shortId)
											if (group == null) {
												src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.NoGroupFoundWithID, input))
											} else {
												if (group.members.remove(player.uuid)) {
													src.sendMessage(Text.literal("Left group: ${group.name} (ID: ${group.id.toShortString()})"))
												} else {
													src.sendMessage(Text.literal("You are not a member of this group."))
												}
											}
											return@executes 1
										}

										val groupsByName = getGroupsByName(input)
										when {
											groupsByName.isEmpty() -> {
												src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.NoGroupFoundWithName, input))
											}

											groupsByName.size > 1 -> {
												src.sendMessage(
													ap.getMessage(
														FabricordMessageKey.System.GRP.MultipleGroupsFound
													)
												)
											}

											else -> {
												val group = groupsByName.first()
												if (group.members.remove(player.uuid)) {
													src.sendMessage(Text.literal("Left group: ${group.name} (ID: ${group.id.toShortString()})"))
												} else {
													src.sendMessage(Text.literal("You are not a member of this group."))
												}
											}
										}
										1
									}
							)

					)
					//endregion

					//region /grp del <groupNameOrID>
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
					//endregion

					//region /grp accept <player>
					.then(
						literal("accept")
							.then(
								argument("player", StringArgumentType.string())
									.executes { ctx ->
										val src = ctx.source
										val player = src.player ?: return@executes 0
										val ap = player.adapt()
										val playerName = StringArgumentType.getString(ctx, "player")

										val group = GroupManager.groups.values.find { it.owner == player.uuid && it.joinRequests.any { uuid ->
											src.server.playerManager.getPlayer(uuid)?.name?.string == playerName
										} }

										if (group == null) {
											src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.NoPendingRequestsForPlayer, playerName))
											return@executes 1
										}

										val targetUUID = group.joinRequests.find { src.server.playerManager.getPlayer(it)?.name?.string == playerName } ?: return@executes 1
										group.approveJoinRequest(targetUUID)

										src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.JoinRequestApproved, playerName, group.name))
										return@executes 1
									}
							)
					)
					//endregion

					//region /grp deny <player>
					.then(
						literal("deny")
							.then(
								argument("player", StringArgumentType.string())
									.executes { ctx ->
										val src = ctx.source
										val player = src.player ?: return@executes 0
										val ap = player.adapt()
										val playerName = StringArgumentType.getString(ctx, "player")

										val group = GroupManager.groups.values.find { it.owner == player.uuid && it.joinRequests.any { uuid ->
											src.server.playerManager.getPlayer(uuid)?.name?.string == playerName
										} }

										if (group == null) {
											src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.NoPendingRequestsForPlayer, playerName))
											return@executes 1
										}

										val targetUUID = group.joinRequests.find { src.server.playerManager.getPlayer(it)?.name?.string == playerName } ?: return@executes 1
										group.denyJoinRequest(targetUUID)

										src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.JoinRequestDenied, playerName, group.name))
										return@executes 1
									}
							)
					)
					//endregion

					//region /grp switch <groupNameOrID>
					// グループチャットとグローバルチャットの切り替え
					.then(
						literal("switch")
							.then(
								argument("targetGroup", StringArgumentType.string())
									.executes { ctx ->
										val src = ctx.source
										val player = src.player ?: return@executes 0
										val ap = player.adapt()
										val input = StringArgumentType.getString(ctx, "targetGroup")

										val shortId = try {
											ShortUUID.fromShortString(input)
										} catch (_: Exception) {
											null
										}

										if (shortId != null) {
											val group = getGroupById(shortId)
											if (group == null) {
												src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.NoGroupFoundWithID, input))
											} else {
												if (group.members.contains(player.uuid)) {
													playerInGroupedChat[player.uuid] = group.id
													src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.SwitchedToGroupChat, group.name))
												} else {
													src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.NotMemberOfGroup, group.name))
												}
											}
											return@executes 1
										}

										val groupsByName = getGroupsByName(input)
										when {
											groupsByName.isEmpty() -> {
												src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.NoGroupFoundWithName, input))
											}

											groupsByName.size > 1 -> {
												src.sendMessage(
													ap.getMessage(
														FabricordMessageKey.System.GRP.MultipleGroupsFound
													)
												)
											}

											else -> {
												val group = groupsByName.first()
												if (group.members.contains(player.uuid)) {
													playerInGroupedChat[player.uuid] = group.id
													src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.SwitchedToGroupChat, group.name))
												} else {
													src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.NotMemberOfGroup, group.name))
												}
											}
										}
										1
									}
							)
					)
				//endregion

					//region /grp pendinglist
					.then(
						literal("pendinglist")
							.executes { ctx ->
								val src = ctx.source
								val player = src.player ?: return@executes 0
								val ap = player.adapt()

								val ownedGroups = GroupManager.groups.values.filter { it.owner == player.uuid }

								if (ownedGroups.isEmpty()) {
									src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.NoOwnedGroups))
									return@executes 1
								}

								var hasRequests = false

								for (group in ownedGroups) {
									val requestNames = group.joinRequests.mapNotNull {
										src.server.playerManager.getPlayer(it)?.name?.string
									}

									if (requestNames.isEmpty()) {
										src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.NoPendingRequests, group.name))
									} else {
										hasRequests = true
										val message = "Pending requests for ${group.name}: ${requestNames.joinToString(", ")}"
										src.sendMessage(Text.literal(message))
									}
								}

								if (!hasRequests) {
									src.sendMessage(ap.getMessage(FabricordMessageKey.System.GRP.NoPendingRequestsOverall))
								}

								return@executes 1
							}
					)
				//endregion
			)
		}
	}

	internal class ConsoleTrackerAppender(name: String) : AbstractAppender(name, null, PatternLayout.createDefaultLayout(), false, emptyArray()) {
		init {
			start()
		}

		override fun append(event: LogEvent) {
			val rawMessage = event.message.formattedMessage
			val level = event.level

			var safeMessage = rawMessage.replace("```", "`\u200B```")

			if (safeMessage.startsWith("`") || safeMessage.endsWith("`")) {
				safeMessage = "\u200B$safeMessage\u200B"
			}

			val formattedMessage = when (level) {
				Level.INFO, Level.WARN ->

					//
					"""
            ```
            $safeMessage
            ```
            """.trimIndent()
				//

				Level.ERROR -> {
					val errorMessage = safeMessage
						.lineSequence()
						.joinToString("\n") { "- $it" }

					//
					"""
            ```diff
            $errorMessage
            ```
            """.trimIndent()
					//
				}

				else -> return
			}

			DiscordBotManager.sendToDiscordConsole(formattedMessage)
		}
	}

	//>------------------------------------------------------------------------------------------------------------------<\\

	private fun checkRequiredFilesAndDirectories() {
		try {
			if (!Files.exists(modDir)) {
				logger.info(langMan.getSysMessage(FabricordMessageKey.System.CreatingConfigDir, modDir))
				Files.createDirectories(modDir)
			}
			if (configFile.notExists()) {
				copyResourceToFile("config.yml", configFile)
			}
		} catch (e: SecurityException) {
			logger.error(langMan.getSysMessage(FabricordMessageKey.System.FailedToCreateConfigDirBySec), e)
		} catch (e: IOException) {
			logger.error(langMan.getSysMessage(FabricordMessageKey.System.FailedToCreateConfigDirByIO), e)
		} catch (e: Exception) {
			logger.error(langMan.getSysMessage(FabricordMessageKey.System.FailedToCreateConfigDirByUnknown), e)
		}
	}

	private fun extractLangFiles() {
		try {
			val langPath = "assets/fabricord/lang/"
			val classLoader = this::class.java.classLoader

			// It should be not null
			val resourceUrl = classLoader.getResource(langPath)

			val uri = resourceUrl!!.toURI()
			val fs = if (uri.scheme == "jar") FileSystems.newFileSystem(uri, emptyMap<String, Any>()) else null
			val langDirPath = Paths.get(uri)

			Files.walk(langDirPath).use { paths ->
				paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".yml") }.forEach { resourceFile ->
					val targetFile = langDir.resolve(resourceFile.fileName.toString())
					if (!targetFile.exists()) {
						Files.copy(resourceFile, targetFile.toPath())
					}
				}
			}

			fs?.close()
		} catch (e: Exception) {
			logger.error("Failed to extract language files", e)
		}
	}

	private fun loadConfig() {
		try {
			logger.info(langMan.getSysMessage(FabricordMessageKey.System.LoadingConfig))

			if (Files.notExists(configFile)) {
				logger.error(langMan.getSysMessage(FabricordMessageKey.System.ConfigFileNotFound, configFile))
				return
			}

			Files.newInputStream(configFile).use { inputStream ->
				val config: Map<String, Any> = yaml.load(inputStream)

				// Required
				botToken = config.getNullableString("BotToken")
				logChannelID = config.getNullableString("LogChannelID")

				// this feature is not supported in the current version
				enableConsoleLog = config.getNullableBoolean("EnableConsoleLog")
				consoleLogChannelID = config.getNullableString("ConsoleLogChannelID")

				// Optional
				serverStartMessage = config.getNullableString("ServerStartMessage")
				serverStopMessage = config.getNullableString("ServerStopMessage")
				playerJoinMessage = config.getNullableString("PlayerJoinMessage")
				playerLeaveMessage = config.getNullableString("PlayerLeaveMessage")

				botOnlineStatus = config.getNullableString("BotOnlineStatus")
				botActivityStatus = config.getNullableString("BotActivityStatus")
				botActivityMessage = config.getNullableString("BotActivityMessage")

				messageStyle = config.getNullableString("MessageStyle")
				allowMentions = config.getNullableBoolean("AllowMentions")
				webHookId = extractWebhookIdFromUrl(config.getNullableString("WebhookUrl"))
			}
		} catch (e: IOException) {
			logger.error(langMan.getSysMessage(FabricordMessageKey.System.FailedToLoadConfigByIO), e)
		} catch (e: Exception) {
			logger.error(langMan.getSysMessage(FabricordMessageKey.System.FailedToLoadConfigByUnknown), e)
		}
	}

	private fun extractWebhookIdFromUrl(url: String?): String? {
		val regex = Regex("https://discord.com/api/webhooks/([0-9]+)/[a-zA-Z0-9_-]+")
		val matchResult = url?.let { regex.find(it) }
		return matchResult?.groupValues?.get(1)
	}

	private fun nullCheck() {
		if (botActivityMessage.isNullOrBlank()) {
			botActivityMessage = "Minecraft Server"
		}
		if (botActivityStatus.isNullOrBlank()) {
			botActivityStatus = "playing"
		}
		if (botOnlineStatus.isNullOrBlank()) {
			botOnlineStatus = "online"
		}
		if (messageStyle.isNullOrBlank()) {
			messageStyle = "classic"
		}
		if (serverStartMessage.isNullOrBlank()) {
			serverStartMessage = ":white_check_mark: **Server has started!**"
		}
		if (serverStopMessage.isNullOrBlank()) {
			serverStopMessage = ":octagonal_sign: **Server has stopped!**"
		}
		if (playerJoinMessage.isNullOrBlank()) {
			playerJoinMessage = "%player% joined the server"
		}
		if (playerLeaveMessage.isNullOrBlank()) {
			playerLeaveMessage = "%player% left the server"
		}
	}

	private fun requiredNullCheck(): Boolean {
		return botToken.isNullOrBlank() || logChannelID.isNullOrBlank()
	}
}