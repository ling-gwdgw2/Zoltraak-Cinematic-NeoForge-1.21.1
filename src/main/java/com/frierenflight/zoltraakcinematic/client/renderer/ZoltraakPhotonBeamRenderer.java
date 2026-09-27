package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.entity.ZoltraakCinematicBeamEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * AAA-Grade Zoltraak Renderer directly wrapping and utilizing frierenvoid's ZoltraakRenderer.
 */
public class ZoltraakPhotonBeamRenderer extends EntityRenderer<ZoltraakCinematicBeamEntity> {
    public static final ResourceLocation TEX_MAGIC_CIRCLE =
            ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "textures/spell/zoltraak_magic_circle.png");
    public static final ResourceLocation TEX_VFX_ATLAS =
            ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "textures/entity/zoltraak_vfx_atlas.png");

    private final ZoltraakRenderer<ZoltraakCinematicBeamEntity> masterRenderer;

    public ZoltraakPhotonBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.masterRenderer = new ZoltraakRenderer<>(context);
    }

    @Override
    public boolean shouldRender(ZoltraakCinematicBeamEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(ZoltraakCinematicBeamEntity entity) {
        return masterRenderer.getTextureLocation(entity);
    }

    @Override
    public void render(ZoltraakCinematicBeamEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        masterRenderer.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }
}
