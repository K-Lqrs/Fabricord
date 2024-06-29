package net.rk4z.fabricord.mixins;

import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.rk4z.beacon.EventBus;
import net.rk4z.fabricord.events.PlayerGrantCriterionEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancementTracker.class)
public class PlayerAdvancementTrackerMixin {

    @Shadow
    private ServerPlayerEntity owner;

    @Inject(method = "grantCriterion", at = @At("RETURN"))
    public void onGrantCriterion(AdvancementEntry advancementEntry, String string, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }

        AdvancementProgress progress = getProgress(advancementEntry);

        if (progress.isDone() && shouldAnnounceToChat(advancementEntry.value())) {
            EventBus.callEventAsync(PlayerGrantCriterionEvent.get(owner, string));
        }
    }

    @Unique
    public AdvancementProgress getProgress(AdvancementEntry advancement) {
        return owner.getAdvancementTracker().getProgress(advancement);
    }

    @Unique
    private boolean shouldAnnounceToChat(Advancement advancement) {
        return advancement.comp_1913().isPresent() && advancement.comp_1913().get().shouldAnnounceToChat();
    }
}
