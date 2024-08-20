package net.rk4z.mixin;

import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.rk4z.fabricord.discord.DiscordEmbed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {

    @Shadow
    private ServerPlayerEntity owner;

    @Inject(method = "grantCriterion", at = @At("RETURN"))
    public void onAdvancementGranted(AdvancementEntry advancementEntry, String string, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            DiscordEmbed.sendPlayerGrantCriterionEmbed(owner, string);
        }
    }
}
