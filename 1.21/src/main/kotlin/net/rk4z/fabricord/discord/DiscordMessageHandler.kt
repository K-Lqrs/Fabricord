package net.rk4z.fabricord.discord

import net.dv8tion.jda.api.entities.Role
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.rk4z.beacon.Listener
import net.rk4z.beacon.handler
import net.rk4z.fabricord.Fabricord.logChannelID
import net.rk4z.fabricord.events.DiscordMessageReceiveEvent
import java.awt.Color
import java.util.function.Consumer

@Suppress("unused")
class DiscordMessageHandler : Listener {

    val handleDiscordMessage = handler<DiscordMessageReceiveEvent> { event ->
        val channelId: String = logChannelID!!
        if (event.c.channel.id != channelId || event.c.author.isBot) {
            return@handler
        }

        val member = event.c.member
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
            .append(Component.text(" » " + event.c.message.contentDisplay))

        val json = GsonComponentSerializer.gson().serialize(componentMessage)

        val textMessage: Text? = Text.Serialization.fromJson(json, event.server.registryManager)

        event.server.playerManager.playerList.forEach(
            Consumer { player: ServerPlayerEntity ->
                player.sendMessage(
                    textMessage,
                    false
                )
            }
        )
    }

}