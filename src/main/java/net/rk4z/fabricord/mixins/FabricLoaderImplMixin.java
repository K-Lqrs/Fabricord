package net.rk4z.fabricord.mixins;

import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.rk4z.beacon.EventBus;
import net.rk4z.fabricord.events.ServerStartEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FabricLoaderImpl.class)
public class FabricLoaderImplMixin {

    @Inject(method = "load", at = @At("TAIL"))
    public void onServerStart(CallbackInfo ci) {
        EventBus.callEventAsync(ServerStartEvent.get());
    }
}
