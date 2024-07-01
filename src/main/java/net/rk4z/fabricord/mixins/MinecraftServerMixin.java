package net.rk4z.fabricord.mixins;

import net.minecraft.server.MinecraftServer;
import net.rk4z.beacon.EventBus;
import net.rk4z.fabricord.Fabricord;
import net.rk4z.fabricord.events.ServerStopEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(method = "runServer", at = @At("TAIL"))
    public void onStartServerThread(CallbackInfo ci) {
        Fabricord.INSTANCE = new Fabricord();
        EventBus.initialize();
        EventBus.registerAllListeners("net.rk4z.fabricord");
    }

    @Inject(method = "shutdown", at = @At("HEAD"))
    public void onServerShutdown(CallbackInfo ci) {
        EventBus.callEventAsync(ServerStopEvent.get());
    }
}
