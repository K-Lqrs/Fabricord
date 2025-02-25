package net.ririfa.fabricord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.ririfa.fabricord.annotations.Indexed
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.translation.adapt
import kotlin.reflect.full.findAnnotation

//TODO: Creation of files for storing default groups, reading and writing data and maintaining status.
object GroupManager {
	fun registerAll(dispatcher: CommandDispatcher<ServerCommandSource>) {
		Commands.entries.forEach { it.register(dispatcher) }
	}

	fun showHelpToPlayer(source: ServerCommandSource, page: Int) {
		val player = source.player ?: run {
			val player = (source.entity as ServerPlayerEntity).adapt()
			source.sendError(player.getMessage(FabricordMessageKey.Command.ThisCommandIsPlayerOnly))
			return
		}

		val commandHelpMap = FabricordMessageKey.Command.Help.Group::class
			.sealedSubclasses
			.sortedBy { it.findAnnotation<Indexed>()?.value }
			.associateWith { subclass ->
				val about = subclass.sealedSubclasses.find { it.simpleName == "About" }?.objectInstance
				val usage = subclass.sealedSubclasses.find { it.simpleName == "Usage" }?.objectInstance
				about to usage
			}

		val commandHelpPages = commandHelpMap.entries.chunked(3)
		val totalPages = commandHelpPages.size

		val pageIndex = (page - 1).coerceIn(0, totalPages - 1)
		val commandPage = commandHelpPages[pageIndex]

		val name = player.adapt().getMessage(FabricordMessageKey.Command.Page).string

		player.sendMessage(Text.of("§a=== $name (${pageIndex + 1}/$totalPages) ==="))

		commandPage.forEach { (command, pair) ->
			val aboutMessage = pair.first?.let { player.adapt().getMessage(it) }
			val usageMessage = pair.second?.let { player.adapt().getMessage(it) }

			player.sendMessage(Text.of("§b/${command.simpleName?.lowercase()}§r - ${aboutMessage?.string}"))
			player.sendMessage(Text.of("  §7Usage: ${usageMessage?.string}"))
			player.sendMessage(Text.of(""))
		}
	}

	private enum class Commands(val register: (dispatcher: CommandDispatcher<ServerCommandSource>) -> Unit) {
		HELP({ dispatcher ->
			dispatcher.register(
				literal("grp")
					.then(
						literal("help")
							.then(
								argument("number", IntegerArgumentType.integer(1, 100))
									.executes { context ->
										val page = IntegerArgumentType.getInteger(context, "number")
										showHelpToPlayer(context.source, page)
										1
									}
							)
					)
			)
		}),

		CREATE({ dispatcher ->
			dispatcher.register(
				literal("grp")
					.then(
						literal("create")

					)
			)
		}),

		DELETE({ dispatcher ->

		}),

		JOIN({ dispatcher ->

		}),

		LEAVE({ dispatcher ->

		}),
	}
}