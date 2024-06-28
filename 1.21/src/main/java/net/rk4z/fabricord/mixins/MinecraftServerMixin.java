package net.rk4z.fabricord.mixins;

import net.minecraft.server.MinecraftServer;
import net.rk4z.beacon.EventBus;
import net.rk4z.fabricord.events.ServerStartEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    public void StartClient(CallbackInfo ci) {
        EventBus.callEvent(ServerStartEvent.get());
    }


}
