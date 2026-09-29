package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.block.entity.QualSealingStoneBlockEntity;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 🌟 QualSealingStoneRenderer
 * Renders the cinematic 8-pointed fiery Magic Circle ritual (from Magic Circle.mp4):
 * - Stage 1 (0.0s – 1.5s): 8-pointed star ground circle expands & spins with pulsating amber glow.
 * - Stage 2 (1.5s – 3.25s): Floating 3D cylindrical runic ribbon lifts into the air and rotates counter-clockwise.
 * - Stage 3 (3.25s – 5.0s): Skyward blazing fire pillar erupts 28 blocks upward from the center with roaring flame.
 */
public class QualSealingStoneRenderer implements BlockEntityRenderer<QualSealingStoneBlockEntity> {

    public static final ResourceLocation TEX_MAGIC_CIRCLE = ResourceLocation.fromNamespaceAndPath(
            ZoltraakCinematicMod.MODID, "textures/spell/unsealing_magic_circle.png");
    public static final ResourceLocation TEX_RUNE_RING = ResourceLocation.fromNamespaceAndPath(
            ZoltraakCinematicMod.MODID, "textures/spell/unsealing_rune_ring.png");
    public static final ResourceLocation TEX_FIRE_BEAM = ResourceLocation.fromNamespaceAndPath(
            ZoltraakCinematicMod.MODID, "textures/spell/unsealing_fire_beam.png");

    public static final RenderType RENDER_TYPE_CIRCLE = ZoltraakRenderTypes.magicAdditive(TEX_MAGIC_CIRCLE);
    public static final RenderType RENDER_TYPE_RUNE_RING = ZoltraakRenderTypes.magicAdditive(TEX_RUNE_RING);
    public static final RenderType RENDER_TYPE_FIRE_BEAM = ZoltraakRenderTypes.magicAdditive(TEX_FIRE_BEAM);

    public QualSealingStoneRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(QualSealingStoneBlockEntity blockEntity) {
        return blockEntity.isUnsealing();
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public void render(QualSealingStoneBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!blockEntity.isUnsealing()) {
            return;
        }

        float progress = blockEntity.getUnsealProgress(partialTick);
        float ticks = blockEntity.getUnsealTicks() + partialTick;

        poseStack.pushPose();
        // Translate to the center top of the stone altar plinth (Y = 0.46)
        poseStack.translate(0.5, 0.465, 0.5);

        renderGroundCircle(poseStack, bufferSource, progress, ticks);
        renderFloatingRuneRing(poseStack, bufferSource, progress, ticks);
        renderFireBeam(poseStack, bufferSource, progress, ticks);

        poseStack.popPose();
    }

    /**
     * 1. Ground 8-Pointed Star Magic Circle:
     * Expands, spins, and pulsates with intense amber-orange glow on the stone altar.
     */
    private void renderGroundCircle(PoseStack poseStack, MultiBufferSource bufferSource, float progress, float ticks) {
        float scale = Math.min(progress * 4.0f, 1.0f) * 1.6f; // Radius 1.6 blocks (diameter 3.2)
        if (scale <= 0.01f) return;

        float pulse = 0.82f + 0.18f * Mth.sin(ticks * 0.35f);
        float alpha = Math.min(progress * 5.0f, 1.0f) * pulse;
        float rot = ticks * 14.0f; // Smooth clockwise rotation

        poseStack.pushPose();
        poseStack.translate(0.0, 0.015, 0.0); // Slightly above stone surface to prevent z-fighting
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer builder = bufferSource.getBuffer(RENDER_TYPE_CIRCLE);

        float r = 1.0f;
        float g = 0.65f + 0.15f * pulse;
        float b = 0.25f;

        // Quad on horizontal X-Z plane
        builder.addVertex(pose, -scale, 0.0f, -scale).setUv(0.0f, 0.0f).setColor(r, g, b, alpha);
        builder.addVertex(pose,  scale, 0.0f, -scale).setUv(1.0f, 0.0f).setColor(r, g, b, alpha);
        builder.addVertex(pose,  scale, 0.0f,  scale).setUv(1.0f, 1.0f).setColor(r, g, b, alpha);
        builder.addVertex(pose, -scale, 0.0f,  scale).setUv(0.0f, 1.0f).setColor(r, g, b, alpha);

        poseStack.popPose();
    }

    /**
     * 2. Floating 3D Cylindrical Runic Ring:
     * Lifts upward into the air (0.05m to 1.1m) and counter-rotates with ancient runes.
     */
    private void renderFloatingRuneRing(PoseStack poseStack, MultiBufferSource bufferSource, float progress, float ticks) {
        if (progress < 0.15f) return;

        float liftProgress = Mth.clamp((progress - 0.15f) / 0.5f, 0.0f, 1.0f);
        float ringY = liftProgress * 1.05f + 0.05f;
        float ringH = 0.38f;
        float radius = 1.35f;

        float alpha = Mth.clamp((progress - 0.15f) * 3.0f, 0.0f, 1.0f) * 0.95f;
        float rot = -ticks * 20.0f; // Counter-clockwise rotation

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer builder = bufferSource.getBuffer(RENDER_TYPE_RUNE_RING);

        float r = 1.0f;
        float g = 0.55f;
        float b = 0.20f;

        int segments = 16;
        for (int i = 0; i < segments; i++) {
            float a1 = (float) (i * 2.0 * Math.PI / segments);
            float a2 = (float) ((i + 1) * 2.0 * Math.PI / segments);

            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;
            float x2 = Mth.cos(a2) * radius;
            float z2 = Mth.sin(a2) * radius;

            float u1 = (float) i / segments;
            float u2 = (float) (i + 1) / segments;

            builder.addVertex(pose, x1, ringY, z1).setUv(u1, 1.0f).setColor(r, g, b, alpha);
            builder.addVertex(pose, x2, ringY, z2).setUv(u2, 1.0f).setColor(r, g, b, alpha);
            builder.addVertex(pose, x2, ringY + ringH, z2).setUv(u2, 0.0f).setColor(r, g, b, alpha);
            builder.addVertex(pose, x1, ringY + ringH, z1).setUv(u1, 0.0f).setColor(r, g, b, alpha);
        }

        poseStack.popPose();
    }

    /**
     * 3. Blazing Vertical Fire Pillar / Flame Column:
     * Erupts skyward (28 blocks) from the center of the magic circle when unsealing approaches completion.
     */
    private void renderFireBeam(PoseStack poseStack, MultiBufferSource bufferSource, float progress, float ticks) {
        if (progress < 0.60f) return;

        float beamProgress = Mth.clamp((progress - 0.60f) / 0.35f, 0.0f, 1.0f);
        float height = beamProgress * 28.0f;
        float halfW = (0.50f + 0.12f * Mth.sin(ticks * 0.7f)) * beamProgress;
        float vScroll = -ticks * 0.18f;

        poseStack.pushPose();

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer builder = bufferSource.getBuffer(RENDER_TYPE_FIRE_BEAM);

        float r = 1.0f;
        float g = 0.88f;
        float b = 0.55f;
        float alpha = beamProgress * 0.95f;

        // Render 4 intersecting vertical cross quads for a volumetric 3D column of fire
        for (int i = 0; i < 4; i++) {
            float angle = i * 45.0f + ticks * 4.0f;
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            Matrix4f p = poseStack.last().pose();

            builder.addVertex(p, -halfW, 0.0f, 0.0f).setUv(0.0f, vScroll + 2.0f).setColor(r, g, b, alpha);
            builder.addVertex(p,  halfW, 0.0f, 0.0f).setUv(1.0f, vScroll + 2.0f).setColor(r, g, b, alpha);
            builder.addVertex(p,  halfW, height, 0.0f).setUv(1.0f, vScroll).setColor(r, g, b, alpha * 0.6f);
            builder.addVertex(p, -halfW, height, 0.0f).setUv(0.0f, vScroll).setColor(r, g, b, alpha * 0.6f);

            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
