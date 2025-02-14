package net.ririfa.fabricord

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.ririfa.fabricord.translation.FabricordMessageKey

object CommandManager {
	fun showHelpToPlayer(source: ServerCommandSource) {
		val player = source.player

		// Key -> placeHolder: value
		val map: Map<FabricordMessageKey, Map<String, String>> = mapOf(
		)


	}

	enum class Command(register: (dispatcher: CommandDispatcher<ServerCommandSource>) -> Unit) {
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
		})
	}
}