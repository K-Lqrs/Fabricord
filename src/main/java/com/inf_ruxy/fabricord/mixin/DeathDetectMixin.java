package com.inf_ruxy.fabricord.mixin;

// Importing necessary classes and packages
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

// Importing the discordEmbed from FabricordApi
import static com.inf_ruxy.fabricord.FabricordApi.discordEmbed;

/**
 * This mixin class is used to detect the death of a player in the game.
 * It is applied to the ServerPlayerEntity class.
 */
@Mixin(ServerPlayerEntity.class)
public abstract class DeathDetectMixin {

    /**
     * This method is injected at the end of the onDeath method of the ServerPlayerEntity class.
     * It sends a death message to a Discord server using the DiscordEmbed class.
     *
     * @param info The callback information provided by the Mixin framework.
     */
    @Inject(method = "onDeath", at = @At("TAIL"))
    public void onDeathInject(DamageSource source, CallbackInfo info) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        String playerName = player.getName().getString();
        String deathCause = source.getName();
        String deathMessage = String.format("%s died due to %s", playerName, deathCause);
        UUID playerUUID = player.getUuid();

        // Sending the death message to the Discord server
        discordEmbed.sendPlayerDeathEmbed(playerName, playerUUID, deathMessage);
    }

}