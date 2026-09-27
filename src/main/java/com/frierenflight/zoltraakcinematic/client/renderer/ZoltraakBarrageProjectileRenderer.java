package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.entity.ZoltraakBarrageProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * AAA-Grade Zoltraak Barrage Projectile Renderer cleanly wrapping and utilizing frierenvoid's RapidZoltraakRenderer.
 */
public class ZoltraakBarrageProjectileRenderer extends EntityRenderer<ZoltraakBarrageProjectileEntity> {
    private final RapidZoltraakRenderer<ZoltraakBarrageProjectileEntity> rapidRenderer;

    public ZoltraakBarrageProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.rapidRenderer = new RapidZoltraakRenderer<>(context, false);
    }

    @Override
    public boolean shouldRender(ZoltraakBarrageProjectileEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(ZoltraakBarrageProjectileEntity entity) {
        return rapidRenderer.getTextureLocation(entity);
    }

    @Override
    public void render(ZoltraakBarrageProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!ZoltraakRenderPass.defer(this, entity, entityYaw, partialTicks, poseStack, packedLight)) {
            rapidRenderer.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        }
    }
}
