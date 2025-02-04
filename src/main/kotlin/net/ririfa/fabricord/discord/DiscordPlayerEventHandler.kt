package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.ririfa.fabricord.Fabricord
import net.ririfa.fabricord.translation.FabricordMessageKey
import net.ririfa.fabricord.translation.FabricordMessageProvider
import net.ririfa.langman.LangMan

object DiscordPlayerEventHandler {
    private lateinit var lm: LangMan<FabricordMessageProvider, Text>

    fun init() {
        lm = Fabricord.instance.langMan
    }

    fun handleMCMessage(player: ServerPlayerEntity, message: String) {
        Fabricord.executorService.submit {
            when (Fabricord.messageStyle) {
                "modern" -> modernStyle(player, message)
                "classic" -> classicStyle(player, message)
                else -> classicStyle(player, message)
            }
        }
    }

    private fun classicStyle(player: ServerPlayerEntity, message: String) {
        val mcId = player.name.string
        val formattedMessage = "$mcId » $message"
        DiscordBotManager.sendToDiscord(formattedMessage)
    }

    private fun modernStyle(player: ServerPlayerEntity, message: String) {
        if (Fabricord.webHookId.isNullOrBlank()) {
            Fabricord.logger.error(lm.getSysMessage(FabricordMessageKey.System.Discord.WebHookUrlNotConfiguredOrBlank))
        }

        try {
            val webHookClient = DiscordBotManager.webHook ?: run {
                Fabricord.logger.error(lm.getSysMessage(FabricordMessageKey.System.Discord.WebHookUrlNotConfigured))
                return
            }

            val data = MessageCreateBuilder()
                .setContent(message)

            if (Fabricord.allowMentions == false) {
                data.setAllowedMentions(emptySet())
            }
        
            webHookClient.sendMessage(data.build())
                .setUsername(player.name.string)
                .setAvatarUrl("https://visage.surgeplay.com/face/256/${player.uuid}")
                .queue()

        } catch (e: Exception) {
            Fabricord.logger.error(lm.getSysMessage(FabricordMessageKey.System.Discord.ErrorDuringWebHookSend, e.localizedMessage), e)
        }
    }
}
