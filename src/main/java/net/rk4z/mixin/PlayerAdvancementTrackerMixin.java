package net.rk4z.mixin;

import net.minecraft.advancement.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.rk4z.fabricord.discord.DiscordEmbed;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {

    @Shadow
    private ServerPlayerEntity owner;

    @Inject(method = "grantCriterion", at = @At("RETURN"))
    public void onAdvancementGranted(AdvancementEntry advancementEntry, String string, @NotNull CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            Advancement advancement = advancementEntry.value();
            Optional<AdvancementDisplay> display = advancement.comp_1913();

            if (display.isPresent() && display.get().shouldAnnounceToChat()) {
                AdvancementProgress progress = ((PlayerAdvancementTracker)(Object)this).getProgress(advancementEntry);
                if (progress.isDone()) {
                    String title = display.get().getTitle().getString();
                    DiscordEmbed.sendPlayerGrantCriterionEmbed(owner, title);
                }
            }
        }
    }
}
