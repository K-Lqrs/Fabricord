package com.inf_ruxy.mods.several.fabricord.mixin;

import com.inf_ruxy.mods.several.fabricord.discord.DiscordEmbed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

import static com.inf_ruxy.mods.several.fabricord.FabricordApi.discordEmbed;

@Mixin(ServerPlayerEntity.class)
public abstract class DeathDetectMixin {

	@Inject(method = "onDeath", at = @At("TAIL"))
	private void onDeathInject(CallbackInfo info) {
		ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
		String playerName = player.getName().getString();
		UUID playerUUID = player.getUuid();
		String deathMessage = player.getDamageTracker().getDeathMessage().getString();

		DiscordEmbed discordEmbedJava = discordEmbed;
		discordEmbedJava.sendPlayerDeathEmbed(playerName, playerUUID, deathMessage);
	}
}


