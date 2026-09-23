package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.client.fx.ZoltraakKilaGraphBridge;
import com.frierenflight.zoltraakcinematic.client.fx.ZoltraakPhotonFXBridge;
import com.frierenflight.zoltraakcinematic.entity.ZoltraakCinematicBeamEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class ZoltraakPhotonBeamRenderer extends EntityRenderer<ZoltraakCinematicBeamEntity> {
    public static final ResourceLocation TEX_MAGIC_CIRCLE =
            ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "textures/entity/zoltraak_magic_circle.png");
    public static final ResourceLocation TEX_VFX_ATLAS =
            ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "textures/entity/zoltraak_vfx_atlas.png");

    public ZoltraakPhotonBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(ZoltraakCinematicBeamEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(ZoltraakCinematicBeamEntity entity) {
        return TEX_MAGIC_CIRCLE;
    }

    @Override
    public void render(ZoltraakCinematicBeamEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float age = entity.tickCount + partialTicks;
        if (age < 0.1f || age >= ZoltraakCinematicBeamEntity.LIFETIME) {
            return;
        }

        // Attach Photon GPU particle emitter if Photon mod is active
        if (entity.tickCount == 1) {
            ZoltraakPhotonFXBridge.attachPhotonFX(entity);
        }

        // Update KilaGraph dynamic shader uniforms (GameTime, DistortionStrength, VortexSpeed)
        ZoltraakKilaGraphBridge.updateUniforms(age / 20.0f, Math.min(1.0f, age / 8.0f));

        float beamLength = entity.getBeamLength();
        float yRot = entity.getYRot();
        float xRot = entity.getXRot();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));

        // ---------------------------------------------------------------------
        // 1. SINGLE MAGIC CIRCLE (Anime-accurate: strictly 1 single magic circle)
        // ---------------------------------------------------------------------
        float circleScale = 1.0f;
        float circleAlpha = 1.0f;
        if (age < 6.0f) {
            circleScale = Mth.lerp(age / 6.0f, 0.15f, 1.05f);
            circleAlpha = Math.min(1.0f, age / 4.0f);
        } else if (age < 8.0f) {
            circleScale = Mth.lerp((age - 6.0f) / 2.0f, 1.05f, 1.0f);
            circleAlpha = 1.0f;
        } else if (age > 18.0f) {
            circleAlpha = Mth.clamp(1.0f - (age - 18.0f) / 8.0f, 0.0f, 1.0f);
            circleScale = Mth.clamp(1.0f - (age - 18.0f) / 10.0f, 0.7f, 1.0f);
        }

        int circleAlphaByte = (int) (circleAlpha * 255);
        if (circleAlphaByte > 2) {
            VertexConsumer circleBuilder = bufferSource.getBuffer(ZoltraakKilaGraphBridge.getSwirlingVortexRenderType(TEX_MAGIC_CIRCLE));
            poseStack.pushPose();
            poseStack.translate(0, 0, -0.18f);
            poseStack.mulPose(Axis.ZP.rotationDegrees(age * 2.2f));
            renderDisc(poseStack, circleBuilder, 0.8f * circleScale, circleAlphaByte);
            poseStack.popPose();
        }

        VertexConsumer atlasBuilder = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(TEX_VFX_ATLAS));

        // ---------------------------------------------------------------------
        // 1.1 PRE-CAST APERTURE CROSS FLARE & PIERCING SPINDLE (Unity VFX 1:1)
        // ---------------------------------------------------------------------
        if (age < 8.0f && age >= 2.0f) {
            float flareProgress = (age - 2.0f) / 6.0f;
            float flareAlpha = Mth.clamp(flareProgress / 0.25f, 0.0f, 1.0f);
            int flareAlphaByte = (int) (flareAlpha * 255);

            // A. 4-Point Cross Star Flare at center aperture (UV: 0.00..0.25, 0.25..0.50)
            poseStack.pushPose();
            poseStack.translate(0, 0, -0.16f);
            poseStack.mulPose(Axis.ZP.rotationDegrees(age * 4.0f));
            float flareSize = Mth.lerp(flareProgress, 0.25f, 1.05f);
            renderTexturedPlane(poseStack, atlasBuilder, flareSize, flareSize, 0.00f, 0.25f, 0.25f, 0.50f, 255, 255, 255, flareAlphaByte);
            poseStack.popPose();

            // B. Hypersonic Spindle Needle (UV: 0.50..0.75, 0.00..0.25)
            if (age >= 5.0f) {
                float needleT = (age - 5.0f) / 3.0f;
                float needleAlpha = Math.min(1.0f, needleT / 0.30f);
                int needleAlphaByte = (int) (needleAlpha * 255);
                float needleLength = Mth.lerp(needleT, 0.8f, 3.5f);
                float needleWidth = 0.16f * (1.0f - needleT * 0.25f);

                poseStack.pushPose();
                poseStack.translate(0, 0, -0.14f);
                renderTexturedPlane(poseStack, atlasBuilder, needleWidth, needleLength, 0.50f, 0.00f, 0.75f, 0.25f, 255, 255, 255, needleAlphaByte);
                renderTexturedPlane(poseStack, atlasBuilder, needleLength, needleWidth, 0.75f, 0.00f, 1.00f, 0.25f, 255, 255, 255, needleAlphaByte);
                poseStack.popPose();
            }
        }

        // ---------------------------------------------------------------------
        // 2. 64-METER PIERCING BEAM (Hypersonic Surge & Detachment Traversal)
        // ---------------------------------------------------------------------
        if (age >= 8.0f) {
            float beamProgress = age - 8.0f;
            float zStart = 0.0f;
            float zEnd = beamLength;
            float beamAlpha = 1.0f;

            if (beamProgress < 2.5f) {
                float t = beamProgress / 2.5f;
                float surge = 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);
                zStart = 0.0f;
                zEnd = Math.max(1.0f, beamLength * surge);
                beamAlpha = Math.min(1.0f, beamProgress / 0.5f);
            } else if (beamProgress <= 10.5f) {
                zStart = 0.0f;
                zEnd = beamLength;
                beamAlpha = 1.0f;
            } else {
                float t = (beamProgress - 10.5f) / 7.5f;
                zStart = beamLength * (t * t);
                zEnd = beamLength;
                beamAlpha = Mth.clamp(1.0f - t, 0.0f, 1.0f);
            }

            int beamAlphaByte = (int) (beamAlpha * 255);
            if (beamAlphaByte > 2 && zEnd > zStart) {
                // A. Muzzle Aperture Flash at circle center (Ticks 8 to 14)
                if (beamProgress < 6.0f) {
                    float muzzleFade = 1.0f - beamProgress / 6.0f;
                    int muzzleAlpha = (int) (muzzleFade * 255);
                    if (muzzleAlpha > 5) {
                        poseStack.pushPose();
                        poseStack.translate(0, 0, 0.05f);
                        poseStack.mulPose(Axis.ZP.rotationDegrees(age * 12.0f));
                        float muzzleRadius = 0.95f * (1.0f + beamProgress * 0.15f);
                        renderTexturedPlane(poseStack, atlasBuilder, muzzleRadius, muzzleRadius, 0.25f, 0.25f, 0.50f, 0.50f, 255, 255, 255, muzzleAlpha);
                        poseStack.popPose();
                    }
                }

                // Taper radii
                float coreRStart = Mth.lerp(zStart / beamLength, 0.26f, 0.38f);
                float coreREnd   = Mth.lerp(zEnd / beamLength, 0.26f, 0.38f);

                float sheathRStart = Mth.lerp(zStart / beamLength, 0.58f, 0.78f);
                float sheathREnd   = Mth.lerp(zEnd / beamLength, 0.58f, 0.78f);

                float haloRStart = Mth.lerp(zStart / beamLength, 0.75f, 1.00f);
                float haloREnd   = Mth.lerp(zEnd / beamLength, 0.75f, 1.00f);

                // B1. Dark Mana Absorption Core & Fresnel Rim Glow (KilaGraph Singularity Shader)
                VertexConsumer absorbBuilder = bufferSource.getBuffer(ZoltraakKilaGraphBridge.getEnergyAbsorptionRenderType(TEX_VFX_ATLAS));
                poseStack.pushPose();
                renderBeamCylinder(poseStack, absorbBuilder, coreRStart * 1.15f, coreREnd * 1.15f, zStart, zEnd, 0.00f, 0.00f, 0.25f, 0.25f, 40, 120, 220, (int) (beamAlphaByte * 0.75f), 4, -age * 1.2f);
                poseStack.popPose();

                // B. Pure White Emissive Core Beam (4 Intersecting Planes, UV: 0.00..0.25, 0.00..0.25)
                poseStack.pushPose();
                renderBeamCylinder(poseStack, atlasBuilder, coreRStart, coreREnd, zStart, zEnd, 0.00f, 0.00f, 0.25f, 0.25f, 255, 255, 255, beamAlphaByte, 4, age * 1.5f);
                poseStack.popPose();

                // C. Iridescent Cyan-Violet Soft Sheath (4 Intersecting Planes, UV: 0.25..0.50, 0.00..0.25)
                poseStack.pushPose();
                renderBeamCylinder(poseStack, atlasBuilder, sheathRStart, sheathREnd, zStart, zEnd, 0.25f, 0.00f, 0.50f, 0.25f, 230, 245, 255, (int) (beamAlphaByte * 0.85f), 4, -age * 2.0f);
                poseStack.popPose();

                // D. Outer Ambient Cyan-Violet Glow Sheath (3 Intersecting Planes, UV: 0.25..0.50, 0.00..0.25)
                poseStack.pushPose();
                renderBeamCylinder(poseStack, atlasBuilder, haloRStart, haloREnd, zStart, zEnd, 0.25f, 0.00f, 0.50f, 0.25f, 180, 235, 255, (int) (beamAlphaByte * 0.45f), 3, age * 1.0f);
                poseStack.popPose();

                // E. Atmospheric Heat Distortion Envelope (KilaGraph Screen-Space Refraction Shader)
                float distRStart = haloRStart * 1.30f;
                float distREnd   = haloREnd * 1.30f;
                VertexConsumer distBuilder = bufferSource.getBuffer(ZoltraakKilaGraphBridge.getHeatDistortionRenderType(TEX_VFX_ATLAS));
                poseStack.pushPose();
                renderBeamCylinder(poseStack, distBuilder, distRStart, distREnd, zStart, zEnd, 0.25f, 0.00f, 0.50f, 0.25f, 255, 255, 255, (int) (beamAlphaByte * 0.70f), 3, age * 0.8f);
                poseStack.popPose();

                // F. Terminal 10-Block Impact Detonation (Unity VFX 1:1)
                if (zEnd >= beamLength * 0.85f) {
                    poseStack.pushPose();
                    poseStack.translate(0, 0, zEnd);

                    float blastExpansion = Math.min(1.0f, beamProgress / 1.5f);

                    // F1. Multi-Point Radial Starburst (UV: 0.25..0.50, 0.25..0.50 - 10m diameter)
                    float impactRadius = Mth.lerp(blastExpansion, 2.5f, 5.0f) + (float) Math.sin(age * 0.6f) * 0.35f;
                    for (int plane = 0; plane < 3; plane++) {
                        poseStack.pushPose();
                        poseStack.mulPose(Axis.ZP.rotationDegrees(plane * 60.0f - age * 8.0f));
                        renderTexturedPlane(poseStack, atlasBuilder, impactRadius, impactRadius, 0.25f, 0.25f, 0.50f, 0.50f, 255, 255, 255, beamAlphaByte);
                        poseStack.popPose();
                    }

                    // F2. Horizontal Anamorphic Lens Flare Blade (UV: 0.75..1.00, 0.00..0.25 - 17m wide blade)
                    poseStack.pushPose();
                    float bladeWidth = Mth.lerp(blastExpansion, 4.5f, 8.5f) + (float) Math.sin(age * 0.8f) * 0.5f;
                    float bladeHeight = 0.55f;
                    renderTexturedPlane(poseStack, atlasBuilder, bladeWidth, bladeHeight, 0.75f, 0.00f, 1.00f, 0.25f, 255, 255, 255, beamAlphaByte);
                    poseStack.popPose();

                    // F3. Vertical Needle Spike (UV: 0.50..0.75, 0.00..0.25 - 12m vertical spike)
                    poseStack.pushPose();
                    float needleH = Mth.lerp(blastExpansion, 3.0f, 6.0f);
                    float needleW = 0.45f;
                    renderTexturedPlane(poseStack, atlasBuilder, needleW, needleH, 0.50f, 0.00f, 0.75f, 0.25f, 255, 255, 255, (int)(beamAlphaByte * 0.85f));
                    poseStack.popPose();

                    // F4. Sparkling Particle Shockwave Halo Disc (UV: 0.50..0.75, 0.25..0.50 - 13m shockwave ring)
                    poseStack.pushPose();
                    float haloRadius = Mth.lerp(blastExpansion, 2.0f, 6.5f) * (1.0f + (float) Math.sin(age * 0.5f) * 0.10f);
                    poseStack.mulPose(Axis.ZP.rotationDegrees(age * 14.0f));
                    renderTexturedPlane(poseStack, atlasBuilder, haloRadius, haloRadius, 0.50f, 0.25f, 0.75f, 0.50f, 255, 255, 255, (int)(beamAlphaByte * 0.90f));
                    poseStack.popPose();

                    // F5. Piercing Spindle Extension (2.5m penetration past target)
                    poseStack.pushPose();
                    renderBeamCylinder(poseStack, atlasBuilder, 0.30f, 0.02f, 0.0f, 2.5f, 0.50f, 0.00f, 0.75f, 0.25f, 255, 255, 255, (int)(beamAlphaByte * 0.85f), 2, 0.0f);
                    poseStack.popPose();

                    poseStack.popPose();
                }
            }
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    private static void renderDisc(PoseStack poseStack, VertexConsumer builder, float radius, int alpha) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();

        renderVertex(builder, mat, pose, -radius, -radius, 0, 0.0f, 1.0f, 255, 255, 255, alpha);
        renderVertex(builder, mat, pose,  radius, -radius, 0, 1.0f, 1.0f, 255, 255, 255, alpha);
        renderVertex(builder, mat, pose,  radius,  radius, 0, 1.0f, 0.0f, 255, 255, 255, alpha);
        renderVertex(builder, mat, pose, -radius,  radius, 0, 0.0f, 0.0f, 255, 255, 255, alpha);

        renderVertex(builder, mat, pose, -radius,  radius, 0, 0.0f, 0.0f, 255, 255, 255, alpha);
        renderVertex(builder, mat, pose,  radius,  radius, 0, 1.0f, 0.0f, 255, 255, 255, alpha);
        renderVertex(builder, mat, pose,  radius, -radius, 0, 1.0f, 1.0f, 255, 255, 255, alpha);
        renderVertex(builder, mat, pose, -radius, -radius, 0, 0.0f, 1.0f, 255, 255, 255, alpha);
    }

    private static void renderTexturedPlane(PoseStack poseStack, VertexConsumer builder, float halfWidth, float halfHeight,
                                            float u0, float v0, float u1, float v1,
                                            int r, int g, int b, int a) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat = pose.pose();

        renderVertex(builder, mat, pose, -halfWidth, -halfHeight, 0, u0, v1, r, g, b, a);
        renderVertex(builder, mat, pose,  halfWidth, -halfHeight, 0, u1, v1, r, g, b, a);
        renderVertex(builder, mat, pose,  halfWidth,  halfHeight, 0, u1, v0, r, g, b, a);
        renderVertex(builder, mat, pose, -halfWidth,  halfHeight, 0, u0, v0, r, g, b, a);

        renderVertex(builder, mat, pose, -halfWidth,  halfHeight, 0, u0, v0, r, g, b, a);
        renderVertex(builder, mat, pose,  halfWidth,  halfHeight, 0, u1, v0, r, g, b, a);
        renderVertex(builder, mat, pose,  halfWidth, -halfHeight, 0, u1, v1, r, g, b, a);
        renderVertex(builder, mat, pose, -halfWidth, -halfHeight, 0, u0, v1, r, g, b, a);
    }

    private static void renderBeamCylinder(PoseStack poseStack, VertexConsumer builder,
                                          float rStart, float rEnd, float z0, float z1,
                                          float u0, float v0, float u1, float v1,
                                          int red, int green, int blue, int alpha,
                                          int planeCount, float rollDegrees) {
        float angleStep = 180.0f / planeCount;

        for (int i = 0; i < planeCount; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(rollDegrees + i * angleStep));

            PoseStack.Pose pose = poseStack.last();
            Matrix4f mat = pose.pose();

            renderVertex(builder, mat, pose, -rStart, 0, z0, u0, v0, red, green, blue, alpha);
            renderVertex(builder, mat, pose,  rStart, 0, z0, u1, v0, red, green, blue, alpha);
            renderVertex(builder, mat, pose,  rEnd,   0, z1, u1, v1, red, green, blue, alpha);
            renderVertex(builder, mat, pose, -rEnd,   0, z1, u0, v1, red, green, blue, alpha);

            renderVertex(builder, mat, pose, -rEnd,   0, z1, u0, v1, red, green, blue, alpha);
            renderVertex(builder, mat, pose,  rEnd,   0, z1, u1, v1, red, green, blue, alpha);
            renderVertex(builder, mat, pose,  rStart, 0, z0, u1, v0, red, green, blue, alpha);
            renderVertex(builder, mat, pose, -rStart, 0, z0, u0, v0, red, green, blue, alpha);

            poseStack.popPose();
        }
    }

    private static void renderVertex(VertexConsumer builder, Matrix4f mat, PoseStack.Pose pose,
                                     float x, float y, float z, float u, float v,
                                     int r, int g, int b, int a) {
        builder.addVertex(mat, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880) // Full bright emissive
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }
}
