package net.rk4z.fabricord.mixins;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.rk4z.beacon.EventBus;
import net.rk4z.fabricord.Fabricord;
import net.rk4z.fabricord.events.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.text.SimpleDateFormat;
import java.util.Date;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Inject(method = "onPlayerConnect", at = @At("RETURN"))
    public void onPlayerConnect(ClientConnection cc, ServerPlayerEntity player, ConnectedClientData ccData, CallbackInfo ci) {
        EventBus.callEventAsync(PlayerJoinEvent.get(player));
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        String message = timestamp + " > " + player.getName().getString() + " has joined the server.";

        Fabricord.INSTANCE.addLog(message);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    public void onPlayerDisconnect(ServerPlayerEntity player, CallbackInfo ci) {
        EventBus.callEventAsync(PlayerLeaveEvent.get(player));
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        String message = timestamp + " > " + player.getName().getString() + " has left the server.";

        Fabricord.INSTANCE.addLog(message);
    }
}
