package com.inf_ruxy.fabricord.mixin;

import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.inf_ruxy.fabricord.FabricordApi.discordEmbed;


@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {

    @Shadow
    private ServerPlayerEntity owner;

    // 実績の基準が達成された後に呼び出されるように注入ポイントを変更します。
    @Inject(method = "grantCriterion(Lnet/minecraft/advancement/Advancement;Ljava/lang/String;)Z", at = @At("RETURN"))
    public void onGrantCriterion(Advancement advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return; // 基準が実際に達成されなかった場合は何もしない
        }

        AdvancementProgress progress = this.getProgress(advancement);

        if (progress.isDone() && advancement.getDisplay() != null && advancement.getDisplay().shouldAnnounceToChat()) {
            discordEmbed.sendAdvancementEmbed(this.owner, advancement);
        }
    }


    @Unique
    public AdvancementProgress getProgress(Advancement advancement) {
        return this.owner.getAdvancementTracker().getProgress(advancement);
    }

}

