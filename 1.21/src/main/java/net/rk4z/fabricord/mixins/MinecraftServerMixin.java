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

/**
 * This class provides mixins for the MinecraftServer class.
 * Mixins are a way to add, modify, or overwrite methods of a class at runtime.
 */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    /**
     * This method is injected at the end of the startServer method of the MinecraftServer class.
     * It calls the ServerStartEvent on the EventBus.
     *
     * @param function A function that takes a Thread and returns an instance of MinecraftServer or its subclass.
     * @param cir      The callback information returnable.
     * @param <S>      A subclass of MinecraftServer.
     */
    @Inject(method = "startServer", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;start()V", shift = At.Shift.AFTER))
    private static <S extends MinecraftServer> void StartServer(Function<Thread, S> function, CallbackInfoReturnable<S> cir) {
        EventBus.callEvent(ServerStartEvent.get());
    }

    /**
     * This method is injected at the start of the shutdown method of the MinecraftServer class.
     * It calls the ServerStopEvent on the EventBus.
     *
     * @param ci The callback information.
     */
    @Inject(method = "shutdown", at = @At("HEAD"))
    public void StopServer(CallbackInfo ci) {
        EventBus.callEvent(ServerStopEvent.get());
    }
}