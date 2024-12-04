package net.rk4z.fabricord.discord

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.minecraft.server.network.ServerPlayerEntity
import net.rk4z.fabricord.Fabricord
import net.rk4z.fabricord.getDiscordID
import net.rk4z.fabricord.utils.System
import net.rk4z.s1.swiftbase.core.CB
import net.rk4z.s1.swiftbase.core.LMB
import net.rk4z.s1.swiftbase.core.Logger

object DiscordPlayerEventHandler {
    fun handleMCMessage(player: ServerPlayerEntity, message: String) {
        CB.executor.executeAsync {
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
            Logger.error(LMB.getSysMessage(System.Log.Bot.WEBHOOK_NOT_CONFIGURED))
            return
        }

        try {
            val webHookClient = DiscordBotManager.webHook

            val data = MessageCreateBuilder()
                .setContent(message)

            val allowedMentions = mutableSetOf<Message.MentionType>().apply {
                val channel = Fabricord.logChannelID?.let { DiscordBotManager.guild.getTextChannelById(it) }
                val playerDiscord = player.getDiscordID()
                val user = DiscordBotManager.guild.getMemberById(playerDiscord)

                val permissions = channel?.getPermissionOverride(user!!)?.allowed ?: user?.permissions

                if (permissions != null) {
                    if (permissions.contains(Permission.MESSAGE_MENTION_EVERYONE)) {
                        add(Message.MentionType.EVERYONE)
                        add(Message.MentionType.ROLE)
                    }
                    add(Message.MentionType.USER)
                } else if (Fabricord.allowMention == true) {
                    if (Fabricord.allowEveryone == true) add(Message.MentionType.EVERYONE)
                    if (Fabricord.allowHere == true) add(Message.MentionType.HERE)
                    if (Fabricord.allowRoleMention == true) add(Message.MentionType.ROLE)
                    if (Fabricord.allowUserMention == true) add(Message.MentionType.USER)
                }
            }

            if (allowedMentions.isNotEmpty()) {
                data.setAllowedMentions(allowedMentions)
            } else {
                data.setAllowedMentions(emptySet())
            }

            webHookClient!!.sendMessage(data.build())
                .setUsername(player.name.string)
                .setAvatarUrl("https://visage.surgeplay.com/face/256/${player.uuid}")
                .queue()

        } catch (e: Exception) {
            Logger.error("An unexpected error occurred while sending message to Discord webhook: ${e.localizedMessage}", e)
        }
    }
}
