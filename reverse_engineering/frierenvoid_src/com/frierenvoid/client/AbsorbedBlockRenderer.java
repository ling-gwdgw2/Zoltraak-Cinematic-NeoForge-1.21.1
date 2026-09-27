package com.frierenvoid.client;

import com.frierenvoid.AbsorbedBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

public final class AbsorbedBlockRenderer extends EntityRenderer<AbsorbedBlockEntity> {
   public AbsorbedBlockRenderer(Context context) {
      super(context);
   }

   public void render(AbsorbedBlockEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
      pose.pushPose();
      float t = e.tickCount + partial;
      pose.mulPose(Axis.XP.rotation(t * 0.045F + e.getId()));
      pose.mulPose(Axis.ZP.rotation(t * 0.032F));
      pose.translate(-0.5, -0.5, -0.5);
      Minecraft.getInstance().getBlockRenderer().renderSingleBlock(e.blockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
      pose.popPose();
   }

   public ResourceLocation getTextureLocation(AbsorbedBlockEntity e) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}
