package com.inf_ruxy.projects.mc.plugin.several.fabricord.discord.events

import com.inf_ruxy.projects.mc.plugin.several.fabricord.FabricordApi.config
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import java.awt.Color
import java.util.function.Consumer

class DiscordMessageListener(private val server: MinecraftServer) {

    fun handleDiscordMessage(event: MessageReceivedEvent) {
        val channelId: String = config.logChannelID!!
        if (event.channel.id != channelId || event.author.isBot) {
            return
        }

        val member = event.member
        val memberName = member?.user?.name ?: "Unknown Name"
        val memberId = member?.user?.id ?: "00000000000000000000"
        val idSuggest = "<@$memberId>"
        val highestRole =
            member?.roles?.stream()?.max(Comparator.comparingInt { obj: Role -> obj.position })?.orElse(null)
        val roleName = highestRole?.name
        val roleId = highestRole?.id
        val rIdSuggest = if (roleId != null) "<@&$roleId>" else null
        val roleColor = if (highestRole != null && highestRole.color != null) highestRole.color else Color.WHITE
        val kyoriRoleColor = TextColor.color(
            roleColor!!.red, roleColor.green, roleColor.blue
        )


        var componentMessage = Component.text("[", TextColor.color(0xFFFFFF))
            .append(Component.text("Discord", TextColor.color(0x55CDFC)))

        componentMessage = if (roleName != null) {
            componentMessage.append(Component.text(" | ", TextColor.color(0xFFFFFF)))
                .append(
                    Component.text(roleName, kyoriRoleColor)
                        .clickEvent(ClickEvent.suggestCommand(rIdSuggest!!))
                )
                .append(Component.text("]", TextColor.color(0xFFFFFF)))
                .append(Component.text(" "))
        } else {
            componentMessage.append(Component.text("]", TextColor.color(0xFFFFFF)))
                .append(Component.text(" "))
        }

        componentMessage = componentMessage.append(
            Component.text(memberName)
                .clickEvent(ClickEvent.suggestCommand(idSuggest))
        )
            .append(Component.text(" » " + event.message.contentDisplay))

        val json = GsonComponentSerializer.gson().serialize(componentMessage)

        val textMessage: Text? = Text.Serializer.fromJson(json)

        server.playerManager.playerList.forEach(
            Consumer { player: ServerPlayerEntity ->
                player.sendMessage(
                    textMessage,
                    false
                )
            }
        )
    }
}
