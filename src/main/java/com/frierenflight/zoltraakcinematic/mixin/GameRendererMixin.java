package com.frierenflight.zoltraakcinematic.mixin;

import com.frierenflight.zoltraakcinematic.client.renderer.ZoltraakRenderPass;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, remap = false)
public abstract class GameRendererMixin {
   @Inject(method = "renderLevel", at = @At("HEAD"), remap = false, require = 1)
   private void zoltraak$beginWorld(CallbackInfo ci) {
      ZoltraakRenderPass.begin();
   }

   @Inject(
      method = "renderLevel",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
         shift = Shift.AFTER,
         remap = false
      ),
      remap = false,
      require = 1
   )
   private void zoltraak$finishWorld(CallbackInfo ci) {
      ZoltraakRenderPass.finish();
   }
}
