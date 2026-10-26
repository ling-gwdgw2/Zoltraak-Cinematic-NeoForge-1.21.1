package com.frierenflight.zoltraakcinematic.mixin;

import com.frierenflight.zoltraakcinematic.client.cinematic.GargantuaCameraDirector;
import com.frierenflight.zoltraakcinematic.client.renderer.GargantuaShake;
import com.frierenflight.zoltraakcinematic.client.renderer.ZoltraakRenderPass;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void zoltraak$beginWorld(CallbackInfo ci) {
        ZoltraakRenderPass.begin();
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            shift = Shift.AFTER
        )
    )
    private void zoltraak$finishWorld(CallbackInfo ci) {
        ZoltraakRenderPass.finish();
    }

    @Inject(method = "bobHurt", at = @At("HEAD"))
    private void zoltraak$harmonicScreenShake(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        GargantuaShake.applyHarmonicShake(poseStack, partialTick);
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void zoltraak$cinematicFov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        if (useFovSetting && GargantuaCameraDirector.isActive()) {
            cir.setReturnValue((double) GargantuaCameraDirector.getFov(((Double) cir.getReturnValue()).floatValue()));
        }
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void zoltraak$hideHandDuringCutscene(Camera camera, float partialTick, Matrix4f projection, CallbackInfo ci) {
        if (GargantuaCameraDirector.isActive()) {
            ci.cancel();
        }
    }
}
