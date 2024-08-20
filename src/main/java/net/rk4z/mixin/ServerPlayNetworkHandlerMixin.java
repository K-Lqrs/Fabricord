package net.rk4z.mixin;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.rk4z.fabricord.discord.DiscordPlayerEventHandler;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow public abstract ServerPlayerEntity getPlayer();

    @Inject(method = "onChatMessage", at = @At("HEAD"))
    private void onChatMessage(@NotNull ChatMessageC2SPacket packet, CallbackInfo ci) {
        ServerPlayerEntity player = this.getPlayer();
        String message = packet.chatMessage();

        DiscordPlayerEventHandler.handleMCMessage(player, message);
    }
}
