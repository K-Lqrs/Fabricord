package net.ririfa.fabricord

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.translation.adapt

object CommandManager {
	fun registerAll(dispatcher: CommandDispatcher<ServerCommandSource>) {
		Commands.entries.forEach { it.register(dispatcher) }
	}

	fun showHelpToPlayer(source: ServerCommandSource) {
		val player = source.player ?: run {
			source.sendError(TODO())
			return
		}

		val commandHelpMap = FabricordMessageKey.Command.Help.Group::class
			.sealedSubclasses
			.associateWith { subclass ->
				val about = subclass.sealedSubclasses.find { it.simpleName == "About" }?.objectInstance
				val usage = subclass.sealedSubclasses.find { it.simpleName == "Usage" }?.objectInstance
				about to usage
			}

		commandHelpMap.forEach { (command, pair) ->
			val aboutMessage = pair.first?.let { player.adapt().getMessage(it) }
			val usageMessage = pair.second?.let { player.adapt().getMessage(it) }

			player.sendMessage(Text.of("§b/${command.simpleName?.lowercase()}§r - $aboutMessage"))
			player.sendMessage(Text.of("\n"))
			player.sendMessage(Text.of("  §7Usage: $usageMessage"))
		}
	}

	private enum class Commands(val register: (dispatcher: CommandDispatcher<ServerCommandSource>) -> Unit) {
		HELP({ dispatcher ->
			dispatcher.register(
				literal("grp")
					.then(
						literal("help")
							.executes { context ->
								showHelpToPlayer(context.source)
								1
							}
					)
			)
		}),

		CREATE({ dispatcher ->

		}),

		DELETE({ dispatcher ->

		}),

		JOIN({ dispatcher ->

		}),

		LEAVE({ dispatcher ->

		}),
	}
}