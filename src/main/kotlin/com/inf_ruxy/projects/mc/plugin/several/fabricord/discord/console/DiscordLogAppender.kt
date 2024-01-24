package com.inf_ruxy.projects.mc.plugin.several.fabricord.discord.console

import com.inf_ruxy.projects.mc.plugin.several.fabricord.FabricordApi.config
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginFactory

@Plugin(name = "DiscordLogAppender", category = "Core", elementType = "appender", printObject = true)
class DiscordLogAppender(
    name: String,
    private val jda: JDA,
) : AbstractAppender(name, null, null, true) {

    override fun append(event: LogEvent) {
        val logChannel: TextChannel? = jda.getTextChannelById(config.consoleLogChannelID!!)
        val message = event.message.formattedMessage
        logChannel?.sendMessage(message)?.queue()
    }

    companion object {
        @PluginFactory
        fun createAppender(
            jda: JDA
        ): DiscordLogAppender {
            return DiscordLogAppender("DiscordLogAppender", jda)
        }
    }
}
