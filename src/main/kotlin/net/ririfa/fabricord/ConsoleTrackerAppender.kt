package net.ririfa.fabricord

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.layout.PatternLayout

class ConsoleTrackerAppender(name: String) : AbstractAppender(name, null, PatternLayout.createDefaultLayout(), false, emptyArray()) {
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

		when (level) {
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

		// DiscordBotManager.sendToDiscordConsole(formattedMessage)
	}
}