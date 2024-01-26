package com.inf_ruxy.several.mods.mods.several.fabricord.mixin;

import com.inf_ruxy.several.mods.fabricord.discord.DiscordEmbed;
import net.minecraft.entity.damage.DamageTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

import static com.inf_ruxy.several.mods.fabricord.FabricordApi.discordEmbed;

@Mixin(ServerPlayerEntity.class)
public abstract class DeathDetect {

	@Shadow
	public abstract DamageTracker getDamageTracker();

	@Inject(method = "onDeath", at = @At("TAIL"))
	private void onDeathInject(CallbackInfo info) {
		ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
		String playerName = player.getName().getString();
		UUID playerUUID = player.getUuid();
		String deathMessage = getDamageTracker().getDeathMessage().getString();

		DiscordEmbed discordEmbedJava = discordEmbed;
		discordEmbedJava.sendPlayerDeathEmbed(playerName, playerUUID, deathMessage);
	}

}


