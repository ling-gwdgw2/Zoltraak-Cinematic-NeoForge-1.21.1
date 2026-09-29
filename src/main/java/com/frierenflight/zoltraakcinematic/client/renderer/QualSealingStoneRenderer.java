package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.block.entity.QualSealingStoneBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 🌟 QualSealingStoneRenderer — High-Fidelity Zoltraak-Grade VFX Engine for Unsealing Ritual.
 *
 * Implements the full Zoltraak rendering architecture:
 * 1. Deferred Emissive Pass (ZoltraakRenderPass integration, Iris/Oculus HDR bloom compatibility)
 * 2. Camera-aligned Anamorphic Bloom Flares & Corona (ZoltraakRenderTypes.ZOL_GLOW)
 * 3. Procedural Collapsing & Swirling Energy Ribbons (ZoltraakRenderTypes.LIGHT)
 * 4. Expanding Shockwave Arc Rings (arc primitives)
 * 5. Volumetric Multi-layer Fire Column (White core + outer sheath + helical fire spirals)
 */
public class QualSealingStoneRenderer implements BlockEntityRenderer<QualSealingStoneBlockEntity> {

    public static final ResourceLocation TEX_MAGIC_CIRCLE = ResourceLocation.fromNamespaceAndPath(
            ZoltraakCinematicMod.MODID, "textures/spell/unsealing_magic_circle.png");
    public static final ResourceLocation TEX_RUNE_RING = ResourceLocation.fromNamespaceAndPath(
            ZoltraakCinematicMod.MODID, "textures/spell/unsealing_rune_ring.png");
    public static final ResourceLocation TEX_FIRE_BEAM = ResourceLocation.fromNamespaceAndPath(
            ZoltraakCinematicMod.MODID, "textures/spell/unsealing_fire_beam.png");

    public static final RenderType RENDER_TYPE_CIRCLE = ZoltraakRenderTypes.unsealingCircle(TEX_MAGIC_CIRCLE);
    public static final RenderType RENDER_TYPE_RUNE_RING = ZoltraakRenderTypes.unsealingCircle(TEX_RUNE_RING);
    public static final RenderType RENDER_TYPE_FIRE_BEAM = ZoltraakRenderTypes.unsealingFireBeam(TEX_FIRE_BEAM);

    private static final List<DeferredSealingDraw> DEFERRED_DRAWS = new ArrayList<>();

    public record DeferredSealingDraw(
            QualSealingStoneBlockEntity blockEntity,
            float partialTick,
            PoseStack pose,
            Matrix4f modelView,
            Matrix4f projection,
            int packedLight,
            int packedOverlay
    ) {}

    public static boolean hasDeferredDraw() {
        return !DEFERRED_DRAWS.isEmpty();
    }

    public static void clearDeferred() {
        DEFERRED_DRAWS.clear();
    }

    public static void renderDeferred(MultiBufferSource.BufferSource bufferSource, Matrix4fStack modelViewStack) {
        if (DEFERRED_DRAWS.isEmpty()) return;
        for (DeferredSealingDraw draw : DEFERRED_DRAWS) {
            if (draw.blockEntity.isRemoved() || !draw.blockEntity.isUnsealing()) continue;
            modelViewStack.set(draw.modelView);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(draw.projection, VertexSorting.DISTANCE_TO_ORIGIN);
            renderInternal(draw.blockEntity, draw.partialTick, draw.pose, bufferSource, draw.packedLight, draw.packedOverlay);
        }
        DEFERRED_DRAWS.clear();
    }

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
    public void render(QualSealingStoneBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!blockEntity.isUnsealing()) {
            return;
        }

        // Zoltraak Deferred Pipeline: defer to ZoltraakRenderPass when shaders are active
        if (ShaderCompatibility.useFullDetailPass()) {
            if (ShaderCompatibility.isShadowPass()) return;

            PoseStack copy = new PoseStack();
            copy.last().pose().set(poseStack.last().pose());
            copy.last().normal().set(poseStack.last().normal());

            DEFERRED_DRAWS.add(new DeferredSealingDraw(
                    blockEntity,
                    partialTick,
                    copy,
                    new Matrix4f(RenderSystem.getModelViewMatrix()),
                    new Matrix4f(RenderSystem.getProjectionMatrix()),
                    packedLight,
                    packedOverlay
            ));
            return;
        }

        // Vanilla / Shaders disabled direct pass
        renderInternal(blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
    }

    /**
     * Master internal renderer implementing Zoltraak's visual complexity.
     */
    private static void renderInternal(QualSealingStoneBlockEntity blockEntity, float partialTick,
                                       PoseStack poseStack, MultiBufferSource bufferSource,
                                       int packedLight, int packedOverlay) {
        float progress = blockEntity.getUnsealProgress(partialTick);
        float ticks = blockEntity.getUnsealTicks() + partialTick;

        Minecraft mc = Minecraft.getInstance();
        Quaternionf camRot = mc.getEntityRenderDispatcher().cameraOrientation();
        Vec3 camRight = new Vec3(new Vector3f(1.0F, 0.0F, 0.0F).rotate(camRot));
        Vec3 camUp = new Vec3(new Vector3f(0.0F, 1.0F, 0.0F).rotate(camRot));

        poseStack.pushPose();
        // Translate to the center top of the stone altar plinth (Y = 0.465)
        poseStack.translate(0.5, 0.465, 0.5);

        renderGroundMagicCircle(poseStack, bufferSource, progress, ticks, camRight, camUp);
        renderFloatingRuneRing(poseStack, bufferSource, progress, ticks, camRight, camUp);
        renderFireBeamAndShockwaves(poseStack, bufferSource, progress, ticks, camRight, camUp);

        poseStack.popPose();
    }

    /**
     * 1. Ground 8-Pointed Star Magic Circle & Inward-Collapsing Runic Energy Lines:
     * - Rotating 8-pointed star disc
     * - Procedural collapsing swirl ribbons (ZoltraakRenderTypes.LIGHT)
     * - Concentric glowing border arcs
     * - Center camera-aligned Anamorphic Flare & Soft Corona (ZoltraakRenderTypes.ZOL_GLOW)
     */
    private static void renderGroundMagicCircle(PoseStack poseStack, MultiBufferSource bufferSource,
                                                float progress, float ticks, Vec3 camRight, Vec3 camUp) {
        float scale = Math.min(progress * 4.0f, 1.0f) * 1.65f; // Radius up to 1.65m
        if (scale <= 0.01f) return;

        float pulse = 0.82f + 0.18f * Mth.sin(ticks * 0.35f);
        float alpha = Math.min(progress * 5.0f, 1.0f) * pulse;
        float rot = ticks * 14.0f; // Smooth clockwise rotation

        // A. Base 8-pointed Star Magic Circle Disc
        poseStack.pushPose();
        poseStack.translate(0.0, 0.015, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer circleBuilder = bufferSource.getBuffer(RENDER_TYPE_CIRCLE);

        float r = 1.0f;
        float g = 0.65f + 0.15f * pulse;
        float b = 0.22f;

        circleBuilder.addVertex(pose, -scale, 0.0f, -scale).setUv(0.0f, 0.0f).setColor(r, g, b, alpha);
        circleBuilder.addVertex(pose,  scale, 0.0f, -scale).setUv(1.0f, 0.0f).setColor(r, g, b, alpha);
        circleBuilder.addVertex(pose,  scale, 0.0f,  scale).setUv(1.0f, 1.0f).setColor(r, g, b, alpha);
        circleBuilder.addVertex(pose, -scale, 0.0f,  scale).setUv(0.0f, 1.0f).setColor(r, g, b, alpha);
        poseStack.popPose();

        flush(bufferSource, RENDER_TYPE_CIRCLE);

        // B. Procedural Inward-Swirling Energy Ribbons (ZoltraakRenderTypes.LIGHT)
        VertexConsumer lines = bufferSource.getBuffer(ZoltraakRenderTypes.LIGHT);
        Matrix4f mat = poseStack.last().pose();
        Vec3 u = new Vec3(1.0, 0.0, 0.0);
        Vec3 v = new Vec3(0.0, 0.0, 1.0);

        // Outer concentric runic border circle
        arc(lines, mat, new Vec3(0, 0.02, 0), u, v, scale * 1.02, 0.022f, alpha * 0.85f, 1.0f, 0.72f, 0.25f, Math.PI * 2);
        // Inner counter-rotating boundary circle
        arc(lines, mat, new Vec3(0, 0.02, 0), u, v, scale * 0.58, 0.016f, alpha * 0.70f, 1.0f, 0.85f, 0.35f, Math.PI * 2);

        // 6 Collapsing / Inward-spiraling energy charge ribbons (Zoltraak style)
        for (int k = 0; k < 6; k++) {
            for (int j = 0; j < 14; j++) {
                double p = j / 14.0;
                double q = (j + 1) / 14.0;
                double a = k * Math.PI * 2.0 / 6.0 - ticks * 0.14;
                double rP = (scale * (1.0 - p) + 0.08);
                double rQ = (scale * (1.0 - q) + 0.08);
                Vec3 pa = new Vec3(Math.cos(a + p * 1.5) * rP, 0.025, Math.sin(a + p * 1.5) * rP);
                Vec3 pb = new Vec3(Math.cos(a + q * 1.5) * rQ, 0.025, Math.sin(a + q * 1.5) * rQ);
                ribbon(lines, mat, pa, pb, new Vec3(0, 1, 0), (float) (0.024 * Math.sin(p * Math.PI)), alpha * 0.75f, 1.0f, 0.78f, 0.25f);
            }
        }
        flush(bufferSource, ZoltraakRenderTypes.LIGHT);

        // C. Center Camera-Aligned Anamorphic Flare & Soft Corona (ZoltraakRenderTypes.ZOL_GLOW)
        VertexConsumer glow = bufferSource.getBuffer(ZoltraakRenderTypes.ZOL_GLOW);
        float glowSize = (0.35f + 0.25f * pulse) * Math.min(progress * 4.0f, 1.0f);
        Vec3 center = new Vec3(0.0, 0.04, 0.0);

        // Outer soft corona orb
        quad(glow, mat, center, camRight.scale(glowSize * 2.6), camUp.scale(glowSize * 2.6), 1.0f, 0.52f, 0.15f, alpha * 0.55f);
        // White-hot glint core
        quad(glow, mat, center, camRight.scale(glowSize * 0.85), camUp.scale(glowSize * 0.85), 1.0f, 0.96f, 0.82f, alpha * 0.95f);
        // Horizontal anamorphic lens streak
        quad(glow, mat, center, camRight.scale(glowSize * 4.6), camUp.scale(glowSize * 0.12), 1.0f, 0.72f, 0.22f, alpha * 0.80f);
        // Vertical glint streak
        quad(glow, mat, center, camRight.scale(glowSize * 0.12), camUp.scale(glowSize * 2.0), 1.0f, 0.58f, 0.16f, alpha * 0.65f);

        flush(bufferSource, ZoltraakRenderTypes.ZOL_GLOW);
    }

    /**
     * 2. Floating 3D Cylindrical Runic Ring & Ascending Double-Helix Spirals:
     * - 16-segment ribbon cylinder lifting into mid-air (RENDER_TYPE_RUNE_RING)
     * - Ascending helical fire ribbons coiling upward (ZoltraakRenderTypes.LIGHT)
     * - Floating mid-air anamorphic star flare (ZoltraakRenderTypes.ZOL_GLOW)
     */
    private static void renderFloatingRuneRing(PoseStack poseStack, MultiBufferSource bufferSource,
                                               float progress, float ticks, Vec3 camRight, Vec3 camUp) {
        if (progress < 0.15f) return;

        float liftProgress = Mth.clamp((progress - 0.15f) / 0.5f, 0.0f, 1.0f);
        float ringY = liftProgress * 1.05f + 0.05f;
        float ringH = 0.38f;
        float radius = 1.35f;

        float alpha = Mth.clamp((progress - 0.15f) * 3.0f, 0.0f, 1.0f) * 0.95f;
        float rot = -ticks * 20.0f; // Counter-clockwise rotation

        // A. Cylindrical Runic Ribbon Quads
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer ringBuilder = bufferSource.getBuffer(RENDER_TYPE_RUNE_RING);

        float r = 1.0f;
        float g = 0.58f;
        float b = 0.22f;

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

            ringBuilder.addVertex(pose, x1, ringY, z1).setUv(u1, 1.0f).setColor(r, g, b, alpha);
            ringBuilder.addVertex(pose, x2, ringY, z2).setUv(u2, 1.0f).setColor(r, g, b, alpha);
            ringBuilder.addVertex(pose, x2, ringY + ringH, z2).setUv(u2, 0.0f).setColor(r, g, b, alpha);
            ringBuilder.addVertex(pose, x1, ringY + ringH, z1).setUv(u1, 0.0f).setColor(r, g, b, alpha);
        }
        poseStack.popPose();
        flush(bufferSource, RENDER_TYPE_RUNE_RING);

        // B. Ascending Double-Helix Fire Spirals (ZoltraakRenderTypes.LIGHT)
        VertexConsumer lines = bufferSource.getBuffer(ZoltraakRenderTypes.LIGHT);
        Matrix4f mat = poseStack.last().pose();
        Vec3 u = new Vec3(1.0, 0.0, 0.0);
        Vec3 v = new Vec3(0.0, 0.0, 1.0);

        // Rim rings on top and bottom of the floating cylinder
        arc(lines, mat, new Vec3(0, ringY, 0), u, v, radius + 0.01, 0.015f, alpha * 0.85f, 1.0f, 0.85f, 0.35f, Math.PI * 2);
        arc(lines, mat, new Vec3(0, ringY + ringH, 0), u, v, radius + 0.01, 0.015f, alpha * 0.85f, 1.0f, 0.85f, 0.35f, Math.PI * 2);

        // Double helical fire spirals wrapping around the cylinder from bottom
        for (int h = 0; h < 2; h++) {
            double offsetAngle = h * Math.PI;
            for (int j = 0; j < 20; j++) {
                double p = j / 20.0;
                double q = (j + 1) / 20.0;
                double a1 = p * Math.PI * 3.5 + ticks * 0.22 + offsetAngle;
                double a2 = q * Math.PI * 3.5 + ticks * 0.22 + offsetAngle;
                double r1 = radius + 0.06;
                double r2 = radius + 0.06;
                double y1 = p * (ringY + ringH);
                double y2 = q * (ringY + ringH);
                Vec3 pa = new Vec3(Math.cos(a1) * r1, y1, Math.sin(a1) * r1);
                Vec3 pb = new Vec3(Math.cos(a2) * r2, y2, Math.sin(a2) * r2);
                Vec3 normal = new Vec3(Math.cos((a1 + a2) * 0.5), 0, Math.sin((a1 + a2) * 0.5));
                ribbon(lines, mat, pa, pb, normal, 0.018f, alpha * 0.70f, 1.0f, 0.65f, 0.18f);
            }
        }
        flush(bufferSource, ZoltraakRenderTypes.LIGHT);

        // C. Mid-Air Floating Ring Center Flare (ZoltraakRenderTypes.ZOL_GLOW)
        VertexConsumer glow = bufferSource.getBuffer(ZoltraakRenderTypes.ZOL_GLOW);
        Vec3 ringCenter = new Vec3(0.0, ringY + ringH * 0.5, 0.0);
        float ringGlowSize = 0.5f * alpha;
        quad(glow, mat, ringCenter, camRight.scale(ringGlowSize * 2.2), camUp.scale(ringGlowSize * 2.2), 1.0f, 0.55f, 0.15f, alpha * 0.5f);
        quad(glow, mat, ringCenter, camRight.scale(ringGlowSize * 0.7), camUp.scale(ringGlowSize * 0.7), 1.0f, 0.95f, 0.8f, alpha * 0.9f);
        quad(glow, mat, ringCenter, camRight.scale(ringGlowSize * 3.2), camUp.scale(ringGlowSize * 0.1), 1.0f, 0.75f, 0.25f, alpha * 0.7f);
        flush(bufferSource, ZoltraakRenderTypes.ZOL_GLOW);
    }

    /**
     * 3. Blazing Skyward Fire Pillar (28m), Helical Spirals, Rushing Rings & Ground Shockwaves:
     * - Central white-hot core column
     * - 4-Way volumetric rotating cross flame sheath (RENDER_TYPE_FIRE_BEAM)
     * - 6 Helical spiraling flame tendrils (ZoltraakRenderTypes.LIGHT)
     * - 4 Rushing skyward shockwave rings (arc)
     * - Ground expanding shockwave rings (arc)
     * - Massive base & apex anamorphic eruption flares (ZoltraakRenderTypes.ZOL_GLOW)
     */
    private static void renderFireBeamAndShockwaves(PoseStack poseStack, MultiBufferSource bufferSource,
                                                   float progress, float ticks, Vec3 camRight, Vec3 camUp) {
        if (progress < 0.60f) return;

        float beamProgress = Mth.clamp((progress - 0.60f) / 0.35f, 0.0f, 1.0f);
        float height = beamProgress * 28.0f;
        float halfW = (0.52f + 0.12f * Mth.sin(ticks * 0.7f)) * beamProgress;
        float vScroll = -ticks * 0.18f;

        Matrix4f mat = poseStack.last().pose();

        // A. 4-Way Cross Quads Volumetric Flame Column
        poseStack.pushPose();
        VertexConsumer beamBuilder = bufferSource.getBuffer(RENDER_TYPE_FIRE_BEAM);

        float r = 1.0f;
        float g = 0.88f;
        float b = 0.55f;
        float alpha = beamProgress * 0.95f;

        for (int i = 0; i < 4; i++) {
            float angle = i * 45.0f + ticks * 4.0f;
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            Matrix4f p = poseStack.last().pose();

            beamBuilder.addVertex(p, -halfW, 0.0f, 0.0f).setUv(0.0f, vScroll + 2.0f).setColor(r, g, b, alpha);
            beamBuilder.addVertex(p,  halfW, 0.0f, 0.0f).setUv(1.0f, vScroll + 2.0f).setColor(r, g, b, alpha);
            beamBuilder.addVertex(p,  halfW, height, 0.0f).setUv(1.0f, vScroll).setColor(r, g, b, alpha * 0.6f);
            beamBuilder.addVertex(p, -halfW, height, 0.0f).setUv(0.0f, vScroll).setColor(r, g, b, alpha * 0.6f);

            poseStack.popPose();
        }
        poseStack.popPose();
        flush(bufferSource, RENDER_TYPE_FIRE_BEAM);

        // B. Zoltraak Procedural Lines & Spirals (ZoltraakRenderTypes.LIGHT)
        VertexConsumer lines = bufferSource.getBuffer(ZoltraakRenderTypes.LIGHT);
        Vec3 u = new Vec3(1.0, 0.0, 0.0);
        Vec3 v = new Vec3(0.0, 0.0, 1.0);

        // B1. 6 Helical Spiraling Flame Tendrils coiling around the pillar up into the sky
        for (int k = 0; k < 6; k++) {
            double baseAngle = k * Math.PI / 3.0;
            for (int j = 0; j < 32; j++) {
                double p = j / 32.0;
                double q = (j + 1) / 32.0;
                double y1 = p * height;
                double y2 = q * height;
                double a1 = y1 * 0.42 + ticks * 0.22 + baseAngle;
                double a2 = y2 * 0.42 + ticks * 0.22 + baseAngle;
                double r1 = (0.58 + 0.12 * Math.sin(y1 * 0.35 + ticks * 0.5)) * beamProgress;
                double r2 = (0.58 + 0.12 * Math.sin(y2 * 0.35 + ticks * 0.5)) * beamProgress;
                Vec3 pa = new Vec3(Math.cos(a1) * r1, y1, Math.sin(a1) * r1);
                Vec3 pb = new Vec3(Math.cos(a2) * r2, y2, Math.sin(a2) * r2);
                Vec3 norm = new Vec3(Math.cos((a1 + a2) * 0.5), 0, Math.sin((a1 + a2) * 0.5));
                ribbon(lines, mat, pa, pb, norm, 0.022f * beamProgress, beamProgress * 0.75f, 1.0f, 0.72f, 0.22f);
            }
        }

        // B2. 4 Rushing Skyward Shockwave Rings shooting up the beam into the clouds
        for (int k = 0; k < 4; k++) {
            double ringY = ((ticks * 0.85 + k * 7.0) % Math.max(1.0, height));
            double ringRadius = (0.60 + 0.10 * (ringY / height)) * beamProgress;
            float ringFade = (float) (1.0 - (ringY / height));
            arc(lines, mat, new Vec3(0, ringY, 0), u, v, ringRadius, 0.028f, beamProgress * ringFade * 0.75f, 1.0f, 0.92f, 0.45f, Math.PI * 2);
        }

        // B3. Ground Expanding Shockwave Rings (radiating outward across the ground on thunder strikes)
        for (int k = 0; k < 3; k++) {
            double waveAge = (ticks * 0.40 + k * 8.0) % 24.0;
            double waveR = (0.6 + waveAge * 0.28) * beamProgress;
            float waveAlpha = (float) Math.max(0.0, (1.0 - waveAge / 24.0) * beamProgress * 0.65);
            arc(lines, mat, new Vec3(0, 0.03, 0), u, v, waveR, 0.026f, waveAlpha, 1.0f, 0.68f, 0.18f, Math.PI * 2);
        }

        flush(bufferSource, ZoltraakRenderTypes.LIGHT);

        // C. Massive Eruption Anamorphic Bloom Flare (ZoltraakRenderTypes.ZOL_GLOW)
        VertexConsumer glow = bufferSource.getBuffer(ZoltraakRenderTypes.ZOL_GLOW);
        Vec3 basePos = new Vec3(0.0, 0.25, 0.0);
        float eruptionSize = 1.4f * beamProgress;

        // Base colossal anamorphic flare
        quad(glow, mat, basePos, camRight.scale(eruptionSize * 3.2), camUp.scale(eruptionSize * 3.2), 1.0f, 0.52f, 0.15f, beamProgress * 0.65f);
        quad(glow, mat, basePos, camRight.scale(eruptionSize * 1.1), camUp.scale(eruptionSize * 1.1), 1.0f, 0.98f, 0.85f, beamProgress * 0.95f);
        quad(glow, mat, basePos, camRight.scale(eruptionSize * 6.5), camUp.scale(eruptionSize * 0.18), 1.0f, 0.75f, 0.25f, beamProgress * 0.90f);
        quad(glow, mat, basePos, camRight.scale(eruptionSize * 0.18), camUp.scale(eruptionSize * 3.8), 1.0f, 0.60f, 0.18f, beamProgress * 0.70f);

        // Apex flare at the top of the 28-block pillar
        Vec3 topPos = new Vec3(0.0, height, 0.0);
        float topSize = 0.9f * beamProgress;
        quad(glow, mat, topPos, camRight.scale(topSize * 2.0), camUp.scale(topSize * 2.0), 1.0f, 0.65f, 0.25f, beamProgress * 0.60f);
        quad(glow, mat, topPos, camRight.scale(topSize * 3.5), camUp.scale(topSize * 0.12), 1.0f, 0.85f, 0.40f, beamProgress * 0.75f);

        flush(bufferSource, ZoltraakRenderTypes.ZOL_GLOW);
    }

    // =========================================================================
    // Zoltraak Geometric & Billboard Primitives
    // =========================================================================

    private static Vec3 ring(Vec3 u, Vec3 v, double angle, double radius) {
        return u.scale(Math.cos(angle) * radius).add(v.scale(Math.sin(angle) * radius));
    }

    private static void arc(VertexConsumer b, Matrix4f m, Vec3 center, Vec3 u, Vec3 v,
                            double r, float width, float alpha, float red, float green, float blue, double sweep) {
        int segments = 48;
        for (int i = 0; i < segments; i++) {
            double a = i * sweep / (double) segments;
            double c = (i + 1) * sweep / (double) segments;
            Vec3 p = center.add(ring(u, v, a, r));
            Vec3 q = center.add(ring(u, v, c, r));
            ribbon(b, m, p, q, ring(u, v, (a + c) / 2.0, 1.0), width, alpha, red, green, blue);
        }
    }

    private static void ribbon(VertexConsumer b, Matrix4f m, Vec3 a, Vec3 c, Vec3 side,
                               float width, float alpha, float r, float g, float blue) {
        Vec3 w = side.scale(width);
        tri(b, m, a.subtract(w), a.add(w), c.add(w), r, g, blue, alpha);
        tri(b, m, a.subtract(w), c.add(w), c.subtract(w), r, g, blue, alpha);
    }

    private static void tri(VertexConsumer b, Matrix4f m, Vec3 a, Vec3 c, Vec3 d,
                            float r, float g, float blue, float alpha) {
        for (Vec3 p : new Vec3[]{a, c, d}) {
            b.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, blue, alpha);
        }
    }

    private static void quad(VertexConsumer b, Matrix4f m, Vec3 c, Vec3 u, Vec3 v,
                             float r, float g, float blue, float a) {
        tex(b, m, c.subtract(u).subtract(v), 0.0F, 1.0F, r, g, blue, a);
        tex(b, m, c.add(u).subtract(v), 1.0F, 1.0F, r, g, blue, a);
        tex(b, m, c.add(u).add(v), 1.0F, 0.0F, r, g, blue, a);
        tex(b, m, c.subtract(u).add(v), 0.0F, 0.0F, r, g, blue, a);
    }

    private static void tex(VertexConsumer b, Matrix4f m, Vec3 p, float u, float v,
                            float r, float g, float blue, float a) {
        b.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setUv(u, v).setColor(r, g, blue, a);
    }

    private static void flush(MultiBufferSource b, RenderType t) {
        if (b instanceof MultiBufferSource.BufferSource s) {
            s.endBatch(t);
        }
    }
}
