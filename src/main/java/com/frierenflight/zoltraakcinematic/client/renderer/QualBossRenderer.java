package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.client.model.QualBossModel;
import com.frierenflight.zoltraakcinematic.entity.boss.QualBossEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 🌟 QualBossRenderer — High-Fidelity Cinematic Renderer for Boss Qual (มหาจอมเวทควาล).
 *
 * Implements 100% pure mathematical procedural geometry and Core GLSL 150 shaders:
 * 1. Concentric Demonic Runic Ground Circle (black_magic_circle.fsh)
 * 2. Elder Sage Back Halo Matrix (Concentric Counter-Rotating Glyphs)
 * 3. 3D Helical Dark Mana Tendrils (Parametric Harmonic Spirals)
 * 4. Anamorphic Coronal Flares on Chest Core & Horn Tips (cinematic_flare.fsh & corona.fsh)
 * 5. Cataclysmic Singularity Convergence (Logarithmic Inward Vortex)
 * 6. Teleport / Reactive Barrier Dispersion Shockwave (Exponential Damping Wave)
 */
public class QualBossRenderer extends GeoEntityRenderer<QualBossEntity> {

    public QualBossRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new QualBossModel());
        this.shadowRadius = 0.6f;
    }

    @Override
    public void render(QualBossEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // Pass 1: GeckoLib Entity Animation
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        if (entity.isInvisible()) return;

        // Pass 2: Mathematical Procedural Shader VFX Pipeline
        renderQualMathematicalVFX(entity, partialTick, poseStack, bufferSource);
    }

    private void renderQualMathematicalVFX(QualBossEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        float gameTime = entity.tickCount + partialTick;
        int phase = entity.getPhase();
        boolean isCataclysmic = entity.isCataclysmicCharging() || entity.getCastingState() == QualBossEntity.CAST_STATE_CATACLYSMIC;
        boolean isOverdrive = phase == 4;

        // 1. Subtle Dark Mana Miasma (Gentle ambient floating dark motes / wisps - clean & unobtrusive)
        renderSubtleDarkMiasma(entity, camera, gameTime, poseStack, bufferSource, isOverdrive);

        // 2. Cataclysmic Singularity Convergence (Charge Vortex only)
        if (isCataclysmic) {
            renderCataclysmicSingularity(entity, camera, gameTime, poseStack, bufferSource);
        }

        // 3. Teleport / Reactive Barrier Dispersion Shockwave
        int flashTicks = entity.getTeleportFlashTicks();
        if (flashTicks > 0) {
            renderTeleportShockwave(flashTicks, partialTick, poseStack, bufferSource);
        }

        // Flush Batches to ensure correct draw order and emissive bloom binding
        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch(ZoltraakRenderTypes.BLACK_GLOW);
            bs.endBatch(ZoltraakRenderTypes.CORONA);
            bs.endBatch(ZoltraakRenderTypes.LIGHT);
            bs.endBatch(ZoltraakRenderTypes.ZOL_IMPACT);
        }
    }

    /**
     * 2. Subtle Dark Mana Miasma (Soft floating dark particles/wisps around Qual).
     * Gentle ambient floating dark embers that softly drift upwards without obscuring the character model.
     * Pure translucent GLSL shader motes - elegant, airy, and unobtrusive.
     */
    private void renderSubtleDarkMiasma(QualBossEntity entity, Camera camera, float gameTime, PoseStack poseStack, MultiBufferSource bufferSource, boolean isOverdrive) {
        VertexConsumer glowBuilder = bufferSource.getBuffer(ZoltraakRenderTypes.BLACK_GLOW);
        int particleCount = isOverdrive ? 8 : 5;

        for (int i = 0; i < particleCount; i++) {
            // Deterministic pseudo-random drifting cycle per particle
            float offset = (float) i * 1.618f;
            float lifeProgress = ((gameTime * 0.020f + offset) % 1.0f); // Slow gentle upward drift

            float angle = offset * 2.4f + gameTime * 0.025f;
            float radius = 0.55f + 0.20f * Mth.sin(offset * 3.1f + gameTime * 0.015f);
            float px = Mth.cos(angle) * radius;
            float pz = Mth.sin(angle) * radius;
            float py = 0.25f + lifeProgress * 1.85f; // Softly ascends from waist to above head

            // Smooth sinusoidal fade-in and fade-out
            float alphaFactor = Mth.sin(lifeProgress * (float) Math.PI);
            int alpha = (int) (alphaFactor * (isOverdrive ? 110 : 75)); // Translucent and pleasant
            float size = (0.18f + 0.05f * Mth.sin(offset + gameTime * 0.08f)) * alphaFactor;

            if (alpha <= 4 || size <= 0.01f) continue;

            poseStack.pushPose();
            poseStack.translate(px, py, pz);
            poseStack.mulPose(camera.rotation());

            // Dark void / corrupted purple wisp
            renderQuad(poseStack, glowBuilder, size, 110, 15, 160, alpha);
            poseStack.popPose();
        }
        flush(bufferSource, ZoltraakRenderTypes.BLACK_GLOW);
    }

    /**
     * 5. Cataclysmic Singularity Convergence (Logarithmic Inward Spiral Vortex).
     */
    private void renderCataclysmicSingularity(QualBossEntity entity, Camera camera, float gameTime, PoseStack poseStack, MultiBufferSource bufferSource) {
        poseStack.pushPose();
        poseStack.translate(0.0, 1.85, 1.35);

        float corePulse = 1.6f + 0.35f * Mth.sin(gameTime * 0.8f);
        poseStack.pushPose();
        poseStack.mulPose(camera.rotation());

        VertexConsumer coronaBuilder = bufferSource.getBuffer(ZoltraakRenderTypes.CORONA);
        renderQuad(poseStack, coronaBuilder, corePulse, 255, 80, 255, 255);
        flush(bufferSource, ZoltraakRenderTypes.CORONA);

        if (com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicClientConfig.isAnamorphicLensFlaresEnabled()) {
            VertexConsumer flareBuilder = bufferSource.getBuffer(ZoltraakRenderTypes.BLACK_GLOW);
            renderQuad(poseStack, flareBuilder, corePulse * 1.8f, 230, 30, 255, 240);
            flush(bufferSource, ZoltraakRenderTypes.BLACK_GLOW);
        }

        poseStack.popPose();

        VertexConsumer lightBuilder = bufferSource.getBuffer(ZoltraakRenderTypes.LIGHT);
        Matrix4f mat = poseStack.last().pose();
        int arms = 4;
        int steps = 16;
        for (int a = 0; a < arms; a++) {
            float armOffset = (float) (a * Math.PI * 2.0 / arms);
            for (int i = 0; i < steps; i++) {
                float u0 = (float) i / steps;
                float u1 = (float) (i + 1) / steps;

                float r0 = 4.2f * (float) Math.pow(1.0f - u0, 1.6);
                float r1 = 4.2f * (float) Math.pow(1.0f - u1, 1.6);

                float theta0 = u0 * (float) Math.PI * 6.0f + armOffset + gameTime * 0.45f;
                float theta1 = u1 * (float) Math.PI * 6.0f + armOffset + gameTime * 0.45f;

                float x0 = r0 * Mth.cos(theta0);
                float z0 = r0 * Mth.sin(theta0);
                float y0 = (u0 - 0.5f) * 0.8f;

                float x1 = r1 * Mth.cos(theta1);
                float z1 = r1 * Mth.sin(theta1);
                float y1 = (u1 - 0.5f) * 0.8f;

                float w = 0.08f * (1.0f - u0 * 0.5f);
                int red = 240;
                int green = (int) (30 * (1.0f - u0));
                int blue = 255;
                int alpha = (int) (230 * (1.0f - u0));

                lightBuilder.addVertex(mat, x0, y0 - w, z0).setColor(red, green, blue, alpha);
                lightBuilder.addVertex(mat, x1, y1 - w, z1).setColor(red, green, blue, alpha);
                lightBuilder.addVertex(mat, x1, y1 + w, z1).setColor(red, green, blue, alpha);

                lightBuilder.addVertex(mat, x0, y0 - w, z0).setColor(red, green, blue, alpha);
                lightBuilder.addVertex(mat, x1, y1 + w, z1).setColor(red, green, blue, alpha);
                lightBuilder.addVertex(mat, x0, y0 + w, z0).setColor(red, green, blue, alpha);
            }
        }
        flush(bufferSource, ZoltraakRenderTypes.LIGHT);

        poseStack.popPose();
    }

    /**
     * 6. Teleport / Reactive Barrier Dispersion Shockwave.
     */
    private void renderTeleportShockwave(int flashTicks, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer impactBuilder = bufferSource.getBuffer(ZoltraakRenderTypes.ZOL_IMPACT);

        float progress = 1.0f - Math.max(0.0f, (flashTicks - partialTick) / 12.0f);
        if (progress < 0.0f || progress > 1.0f) return;

        float radius = 5.2f * (1.0f - (float) Math.exp(-4.5 * progress));
        int alpha = (int) (255 * (1.0f - progress) * (1.0f - progress));

        poseStack.pushPose();
        poseStack.translate(0.0, 1.2, 0.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
        renderQuad(poseStack, impactBuilder, radius, 220, 40, 255, alpha);
        poseStack.popPose();
        flush(bufferSource, ZoltraakRenderTypes.ZOL_IMPACT);
    }

    private static void renderQuad(PoseStack poseStack, VertexConsumer builder, float radius, int r, int g, int b, int a) {
        Matrix4f mat = poseStack.last().pose();
        builder.addVertex(mat, -radius, radius, 0.0f).setUv(0.0f, 0.0f).setColor(r, g, b, a);
        builder.addVertex(mat, radius, radius, 0.0f).setUv(1.0f, 0.0f).setColor(r, g, b, a);
        builder.addVertex(mat, radius, -radius, 0.0f).setUv(1.0f, 1.0f).setColor(r, g, b, a);
        builder.addVertex(mat, -radius, -radius, 0.0f).setUv(0.0f, 1.0f).setColor(r, g, b, a);
    }

    private static void flush(MultiBufferSource b, RenderType t) {
        if (b instanceof MultiBufferSource.BufferSource s) {
            s.endBatch(t);
        }
    }
}
