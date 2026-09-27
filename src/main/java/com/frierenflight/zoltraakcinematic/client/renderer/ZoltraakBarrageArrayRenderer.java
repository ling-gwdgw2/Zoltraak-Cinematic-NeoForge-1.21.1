package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.client.ZoltraakCinematicClientEvents;
import com.frierenflight.zoltraakcinematic.spell.FernBarrageSpell;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.*;

/**
 * 3D Multi-Circle Celestial Matrix Array Renderer for Fern's Grand Barrage.
 * Manifests and animates 24 floating runic magic circles in 3D space around the caster:
 * - Tier 1: Outer Celestial Crown Arc (10 Circles)
 * - Tier 2: Mid Flank Wings (8 Circles)
 * - Tier 3: Inner Dual Cross Arrays (6 Circles)
 * Features:
 * - Dynamic Parallax Convergence: all 24 circles physically tilt and aim toward the target
 * - Viewport Awareness: clear center aperture in first-person, heroic back matrix in third-person
 * - Independent multi-speed celestial rune spinning
 * - Synchronized 24-circle muzzle flash shockwave rings on discharge
 * - Powered by custom Core GLSL 150 magic_circle / black_magic_circle shaders
 * - Delicate electric spark shatter disintegration on channel completion
 */
public class ZoltraakBarrageArrayRenderer {

    public static class ArrayState {
        public int introTicks = 0;
        public final int[] muzzleTicks = new int[FernBarrageSpell.BARRAGE_CIRCLE_COUNT];
        public int lastSeenTick = 0;
        public int colorTheme = 0;
        public boolean wasCasting = false;
    }

    private static final Map<UUID, ArrayState> ACTIVE_ARRAYS = new HashMap<>();

    public static void onCircleFired(LivingEntity caster, int circleIndex) {
        if (caster == null || circleIndex < 0 || circleIndex >= FernBarrageSpell.BARRAGE_CIRCLE_COUNT) return;
        ArrayState state = ACTIVE_ARRAYS.computeIfAbsent(caster.getUUID(), k -> new ArrayState());
        state.muzzleTicks[circleIndex] = 4; // 4 ticks of expanding shockwave ring
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ACTIVE_ARRAYS.clear();
            return;
        }

        int currentTick = mc.player != null ? mc.player.tickCount : 0;
        Iterator<Map.Entry<UUID, ArrayState>> iter = ACTIVE_ARRAYS.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<UUID, ArrayState> entry = iter.next();
            ArrayState state = entry.getValue();

            // Tick muzzle flash timers for all 24 circles
            for (int i = 0; i < FernBarrageSpell.BARRAGE_CIRCLE_COUNT; i++) {
                if (state.muzzleTicks[i] > 0) {
                    state.muzzleTicks[i]--;
                }
            }

            // Clean up stale arrays if not updated within 10 ticks
            if (currentTick - state.lastSeenTick > 10) {
                iter.remove();
            }
        }
    }

    private static PoseStack deferredPose;
    private static org.joml.Matrix4f deferredModelView;
    private static org.joml.Matrix4f deferredProjection;
    private static Camera deferredCamera;
    private static float deferredPartial;
    private static final List<LivingEntity> deferredEntities = new ArrayList<>();

    public static boolean hasDeferredDraw() {
        return deferredPose != null && !deferredEntities.isEmpty();
    }

    public static void clearDeferred() {
        deferredPose = null;
        deferredModelView = null;
        deferredProjection = null;
        deferredCamera = null;
        deferredEntities.clear();
    }

    public static void renderDeferred(MultiBufferSource.BufferSource bufferSource, org.joml.Matrix4fStack modelView) {
        if (!hasDeferredDraw()) return;
        modelView.set(deferredModelView);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(deferredProjection, com.mojang.blaze3d.vertex.VertexSorting.DISTANCE_TO_ORIGIN);

        renderCirclesAndFlashes(deferredPose, bufferSource, deferredCamera, deferredPartial, deferredEntities);
    }

    public static void renderArray(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        float partialTick = (float) event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        PoseStack poseStack = event.getPoseStack();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        int gameTick = mc.player.tickCount;

        // Check local player and all nearby players
        List<LivingEntity> checkEntities = new ArrayList<>();
        checkEntities.add(mc.player);
        for (net.minecraft.world.entity.player.Player p : mc.level.players()) {
            if (p != mc.player && p.distanceToSqr(mc.player) < 4096.0) {
                checkEntities.add(p);
            }
        }

        // Update state and particles for all entities
        boolean anyActive = false;
        for (LivingEntity living : checkEntities) {
            boolean isBarrage = false;
            int colorTheme = 0;

            if (living == mc.player) {
                if (ClientMagicData.isCasting()) {
                    String spellId = ClientMagicData.getCastingSpellId();
                    if (spellId != null && (spellId.equals("zoltraak_cinematic:zoltraak_barrage") || spellId.endsWith("zoltraak_barrage"))) {
                        isBarrage = true;
                        colorTheme = 0;
                    } else if (spellId != null && (spellId.equals("zoltraak_cinematic:corrupted_barrage") || spellId.endsWith("corrupted_barrage"))) {
                        isBarrage = true;
                        colorTheme = 1;
                    }
                }
            } else {
                SyncedSpellData synced = ClientMagicData.getSyncedSpellData(living);
                if (synced != null && synced.isCasting()) {
                    String spellId = synced.getCastingSpellId();
                    if (spellId != null && (spellId.equals("zoltraak_cinematic:zoltraak_barrage") || spellId.endsWith("zoltraak_barrage"))) {
                        isBarrage = true;
                        colorTheme = 0;
                    } else if (spellId != null && (spellId.equals("zoltraak_cinematic:corrupted_barrage") || spellId.endsWith("corrupted_barrage"))) {
                        isBarrage = true;
                        colorTheme = 1;
                    }
                }
            }

            ArrayState state = ACTIVE_ARRAYS.computeIfAbsent(living.getUUID(), k -> new ArrayState());
            state.lastSeenTick = gameTick;

            if (isBarrage) {
                state.colorTheme = colorTheme;
                state.wasCasting = true;
                if (state.introTicks < 8) state.introTicks++;
            } else {
                if (state.wasCasting) {
                    state.wasCasting = false;
                    spawnShatterParticles(living, state.colorTheme);
                }
                state.introTicks = Math.max(0, state.introTicks - 1);
            }

            if (state.introTicks > 0) {
                anyActive = true;
            }
        }

        if (!anyActive) {
            clearDeferred();
            return;
        }

        // If an external shaderpack (Iris / Oculus) is active, defer rendering to ZoltraakRenderPass.finish()
        if (ShaderCompatibility.useFullDetailPass()) {
            if (ShaderCompatibility.isShadowPass()) return;

            deferredPose = new PoseStack();
            deferredPose.last().pose().set(poseStack.last().pose());
            deferredPose.last().normal().set(poseStack.last().normal());
            deferredModelView = new org.joml.Matrix4f(RenderSystem.getModelViewMatrix());
            deferredProjection = new org.joml.Matrix4f(RenderSystem.getProjectionMatrix());
            deferredCamera = camera;
            deferredPartial = partialTick;
            deferredEntities.clear();
            deferredEntities.addAll(checkEntities);
            return;
        }

        // Normal world rendering pass (Vanilla / Shaders Disabled)
        renderCirclesAndFlashes(poseStack, bufferSource, camera, partialTick, checkEntities);
    }

    private static void renderCirclesAndFlashes(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                                Camera camera, float partialTick, List<LivingEntity> checkEntities) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 camPos = camera.getPosition();
        int gameTick = mc.player != null ? mc.player.tickCount : 0;
        float gameTime = gameTick + partialTick;

        // Pass 1: Render All 24 Runic Magic Circles
        for (LivingEntity living : checkEntities) {
            ArrayState state = ACTIVE_ARRAYS.get(living.getUUID());
            if (state == null || state.introTicks <= 0) continue;

            RenderType circleType = (state.colorTheme == 1) ? ZoltraakRenderTypes.BLACK_CIRCLE : ZoltraakRenderTypes.ZOL_CIRCLE;
            VertexConsumer circleBuilder = bufferSource.getBuffer(circleType);

            float introProgress = Math.min(1.0f, (state.introTicks + partialTick) / 8.0f);
            float scaleEase = introProgress * (2.0f - introProgress);

            Vec3 eyePos = living.getEyePosition(partialTick);
            float yRot = living.getViewYRot(partialTick);
            float xRot = living.getViewXRot(partialTick);

            Vec3 flatLook = Vec3.directionFromRotation(0, yRot);
            Vec3 right = Vec3.directionFromRotation(0, yRot + 90);
            Vec3 worldUp = new Vec3(0, 1, 0);

            ZoltraakColorTheme theme = ZoltraakColorTheme.fromId(state.colorTheme);
            boolean isFirstPerson = (living == mc.player && mc.options.getCameraType().isFirstPerson());

            for (int i = 0; i < FernBarrageSpell.BARRAGE_CIRCLE_COUNT; i++) {
                Vec3 local = FernBarrageSpell.BARRAGE_CIRCLE_OFFSETS[i];
                float baseR = FernBarrageSpell.BARRAGE_CIRCLE_SCALES[i];

                double fwd = isFirstPerson ? Math.max(1.15, local.z + 1.25) : local.z;
                double xPos = isFirstPerson ? local.x * 1.30 : local.x;
                double yPos = isFirstPerson ? local.y * 1.15 : local.y;

                Vec3 circlePos = eyePos
                        .add(right.scale(xPos))
                        .add(worldUp.scale(yPos))
                        .add(flatLook.scale(fwd));

                // Dynamic Parallax Convergence: tilt each circle planar normal toward focal point 48m ahead
                float aimDist = 48.0f;
                float pitchTilt = (float) Math.toDegrees(Math.atan2(-yPos, aimDist));
                float yawTilt = (float) Math.toDegrees(Math.atan2(xPos, aimDist));

                poseStack.pushPose();
                poseStack.translate(circlePos.x - camPos.x, circlePos.y - camPos.y, circlePos.z - camPos.z);
                poseStack.mulPose(Axis.YP.rotationDegrees(-yRot + yawTilt));
                poseStack.mulPose(Axis.XP.rotationDegrees(xRot + pitchTilt));

                float spin = (gameTime * (15.0f + (i % 6) * 4.0f)) * (i % 2 == 0 ? 1 : -1);
                poseStack.mulPose(Axis.ZP.rotationDegrees(spin));

                float currentRadius = baseR * scaleEase;
                int alpha = (int) (245 * scaleEase);

                renderDisc(poseStack, circleBuilder, currentRadius, alpha, theme.ringR, theme.ringG, theme.ringB);
                poseStack.popPose();
            }
        }
        bufferSource.endBatch(ZoltraakRenderTypes.ZOL_CIRCLE);
        bufferSource.endBatch(ZoltraakRenderTypes.BLACK_CIRCLE);

        // Pass 2: Render All Muzzle Flash Shockwave Expanding Rings
        VertexConsumer atlasBuilder = bufferSource.getBuffer(ZoltraakRenderTypes.BARRAGE_ATLAS);
        for (LivingEntity living : checkEntities) {
            ArrayState state = ACTIVE_ARRAYS.get(living.getUUID());
            if (state == null || state.introTicks <= 0) continue;

            Vec3 eyePos = living.getEyePosition(partialTick);
            float yRot = living.getViewYRot(partialTick);
            float xRot = living.getViewXRot(partialTick);

            Vec3 flatLook = Vec3.directionFromRotation(0, yRot);
            Vec3 right = Vec3.directionFromRotation(0, yRot + 90);
            Vec3 worldUp = new Vec3(0, 1, 0);

            ZoltraakColorTheme theme = ZoltraakColorTheme.fromId(state.colorTheme);
            boolean isFirstPerson = (living == mc.player && mc.options.getCameraType().isFirstPerson());

            for (int i = 0; i < FernBarrageSpell.BARRAGE_CIRCLE_COUNT; i++) {
                int muzTick = state.muzzleTicks[i];
                if (muzTick <= 0) continue;

                float muzT = 1.0f - Math.max(0.0f, (muzTick - partialTick) / 4.0f);
                if (muzT < 0.0f || muzT > 1.0f) continue;

                Vec3 local = FernBarrageSpell.BARRAGE_CIRCLE_OFFSETS[i];
                float baseR = FernBarrageSpell.BARRAGE_CIRCLE_SCALES[i];

                double fwd = isFirstPerson ? Math.max(1.15, local.z + 1.25) : local.z;
                double xPos = isFirstPerson ? local.x * 1.30 : local.x;
                double yPos = isFirstPerson ? local.y * 1.15 : local.y;

                Vec3 circlePos = eyePos
                        .add(right.scale(xPos))
                        .add(worldUp.scale(yPos))
                        .add(flatLook.scale(fwd));

                float aimDist = 48.0f;
                float pitchTilt = (float) Math.toDegrees(Math.atan2(-yPos, aimDist));
                float yawTilt = (float) Math.toDegrees(Math.atan2(xPos, aimDist));

                float ringRadius = baseR * (0.8f + muzT * 1.8f);
                int ringAlpha = (int) ((1.0f - muzT) * 250);

                poseStack.pushPose();
                poseStack.translate(circlePos.x - camPos.x, circlePos.y - camPos.y, circlePos.z - camPos.z);
                poseStack.mulPose(Axis.YP.rotationDegrees(-yRot + yawTilt));
                poseStack.mulPose(Axis.XP.rotationDegrees(xRot + pitchTilt));
                poseStack.translate(0, 0, 0.06f + muzT * 0.22f);
                poseStack.mulPose(Axis.ZP.rotationDegrees(muzT * 35.0f));

                renderTexturedPlane(poseStack, atlasBuilder, ringRadius, ringRadius,
                        0.50f, 0.25f, 0.75f, 0.50f, theme.shockR, theme.shockG, theme.shockB, ringAlpha);
                poseStack.popPose();
            }
        }
        bufferSource.endBatch(ZoltraakRenderTypes.BARRAGE_ATLAS);
    }

    private static void spawnShatterParticles(LivingEntity caster, int colorTheme) {
        if (caster == null || caster.level() == null) return;

        Vec3 eyePos = caster.getEyePosition();
        float yRot = caster.getYRot();
        Vec3 flatLook = Vec3.directionFromRotation(0, yRot);
        Vec3 right = Vec3.directionFromRotation(0, yRot + 90);
        Vec3 worldUp = new Vec3(0, 1, 0);

        Random rng = new Random();
        boolean isPurple = colorTheme == 1;

        for (int i = 0; i < FernBarrageSpell.BARRAGE_CIRCLE_COUNT; i++) {
            Vec3 local = FernBarrageSpell.BARRAGE_CIRCLE_OFFSETS[i];
            float baseR = FernBarrageSpell.BARRAGE_CIRCLE_SCALES[i];
            Vec3 circleCenter = eyePos.add(right.scale(local.x)).add(worldUp.scale(local.y)).add(flatLook.scale(local.z));

            for (int k = 0; k < 2; k++) {
                double angle = rng.nextDouble() * Math.PI * 2.0;
                double r = baseR * (0.5 + rng.nextDouble() * 0.5);
                Vec3 p = circleCenter.add(right.scale(Math.cos(angle) * r)).add(worldUp.scale(Math.sin(angle) * r));
                Vec3 vel = worldUp.scale(0.04 + rng.nextDouble() * 0.04)
                        .add(right.scale((rng.nextDouble() - 0.5) * 0.04))
                        .add(flatLook.scale((rng.nextDouble() - 0.5) * 0.04));

                caster.level().addParticle(isPurple ? ParticleTypes.WITCH : ParticleTypes.ELECTRIC_SPARK,
                        p.x, p.y, p.z, vel.x, vel.y, vel.z);
            }
        }
    }

    private static void renderDisc(PoseStack poseStack, VertexConsumer builder, float radius, int alpha, int r, int g, int b) {
        Matrix4f mat = poseStack.last().pose();

        renderVertex(builder, mat, -radius, -radius, 0, 0.0f, 1.0f, r, g, b, alpha);
        renderVertex(builder, mat,  radius, -radius, 0, 1.0f, 1.0f, r, g, b, alpha);
        renderVertex(builder, mat,  radius,  radius, 0, 1.0f, 0.0f, r, g, b, alpha);
        renderVertex(builder, mat, -radius,  radius, 0, 0.0f, 0.0f, r, g, b, alpha);

        renderVertex(builder, mat, -radius,  radius, 0, 0.0f, 0.0f, r, g, b, alpha);
        renderVertex(builder, mat,  radius,  radius, 0, 1.0f, 0.0f, r, g, b, alpha);
        renderVertex(builder, mat,  radius, -radius, 0, 1.0f, 1.0f, r, g, b, alpha);
        renderVertex(builder, mat, -radius, -radius, 0, 0.0f, 1.0f, r, g, b, alpha);
    }

    private static void renderTexturedPlane(PoseStack poseStack, VertexConsumer builder, float halfWidth, float halfHeight,
                                            float u0, float v0, float u1, float v1,
                                            int r, int g, int b, int a) {
        Matrix4f mat = poseStack.last().pose();

        renderVertex(builder, mat, -halfWidth, -halfHeight, 0, u0, v1, r, g, b, a);
        renderVertex(builder, mat,  halfWidth, -halfHeight, 0, u1, v1, r, g, b, a);
        renderVertex(builder, mat,  halfWidth,  halfHeight, 0, u1, v0, r, g, b, a);
        renderVertex(builder, mat, -halfWidth,  halfHeight, 0, u0, v0, r, g, b, a);

        renderVertex(builder, mat, -halfWidth,  halfHeight, 0, u0, v0, r, g, b, a);
        renderVertex(builder, mat,  halfWidth,  halfHeight, 0, u1, v0, r, g, b, a);
        renderVertex(builder, mat,  halfWidth, -halfHeight, 0, u1, v1, r, g, b, a);
        renderVertex(builder, mat, -halfWidth, -halfHeight, 0, u0, v1, r, g, b, a);
    }

    private static void renderVertex(VertexConsumer builder, Matrix4f mat,
                                     float x, float y, float z, float u, float v,
                                     int r, int g, int b, int a) {
        builder.addVertex(mat, x, y, z)
                .setUv(u, v)
                .setColor(r, g, b, a);
    }
}
