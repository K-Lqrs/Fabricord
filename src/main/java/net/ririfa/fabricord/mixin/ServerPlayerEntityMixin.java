package net.ririfa.fabricord.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.ririfa.fabricord.discord.DiscordBotManager;
import net.ririfa.fabricord.discord.DiscordEmbed;
import org.jetbrains.annotations.Contract;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Contract(pure = true)
    @Inject(method = "onDeath", at = @At("RETURN"))
    public void onPlayerDeath(DamageSource source, CallbackInfo ci) {
        if (!DiscordBotManager.botIsInitialized) return;

        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        Text message = player.getDamageTracker().getDeathMessage();

        DiscordEmbed.sendPlayerDeathEmbed(player, message);
    }

}