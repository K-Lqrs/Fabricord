package net.rk4z.fabricord.mixins;

import net.minecraft.server.MinecraftServer;
import net.rk4z.beacon.EventBus;
import net.rk4z.fabricord.events.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(method = "startServer", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;start()V", shift = At.Shift.AFTER))
    private static <S extends MinecraftServer> void StartServer(Function<Thread, S> function, CallbackInfoReturnable<S> cir) {
        EventBus.callEvent(ServerStartEvent.get());
    }

    @Inject(method = "shutdown", at = @At("HEAD"))
    public void StopServer(CallbackInfo ci) {
        EventBus.callEvent(ServerStopEvent.get());
    }
}