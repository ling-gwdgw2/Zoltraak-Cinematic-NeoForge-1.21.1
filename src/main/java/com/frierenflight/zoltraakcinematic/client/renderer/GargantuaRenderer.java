package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.entity.GargantuaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * GargantuaRenderer
 * Registered entity renderer for GargantuaEntity.
 * The visual rendering of the black hole is executed by GargantuaPostProcessor
 * via full screen-space general relativistic geodesic raymarching.
 */
public class GargantuaRenderer extends EntityRenderer<GargantuaEntity> {

    private static final ResourceLocation DUMMY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod.MODID, "textures/effect/noise_tile.png");

    public GargantuaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GargantuaEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Safeguard registration for the level post pass
        GargantuaPostProcessor.registerClientInstance(entity);
    }

    @Override
    public ResourceLocation getTextureLocation(GargantuaEntity entity) {
        return DUMMY_TEXTURE;
    }

    @Override
    public boolean shouldRender(GargantuaEntity entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}
