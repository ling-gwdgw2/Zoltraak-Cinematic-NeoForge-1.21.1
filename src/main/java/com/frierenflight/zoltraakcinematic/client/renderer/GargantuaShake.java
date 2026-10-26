package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.client.cinematic.GargantuaCameraDirector;
import com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicClientConfig;
import com.frierenflight.zoltraakcinematic.entity.GargantuaEntity;
import com.frierenflight.zoltraakcinematic.spell.GargantuaSpell;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 🌌 GargantuaShake
 * Computes camera shake, spacetime rupture jolts, and cataclysmic supernova rumbling for all active Gargantua singularities,
 * as well as casting incantation micro-tremors.
 */
public final class GargantuaShake {
    private static final float SHAKE_TICKS = 55.0f;
    private static final float SHAKE_STRENGTH = 18.0f;
    private static final double SHAKE_RANGE = 128.0d;

    private GargantuaShake() {}

    public static float currentShake(float partialTick) {
        if (!ZoltraakCinematicClientConfig.isCameraShakeEnabled()) {
            return 0.0f;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null || mc.gameRenderer.getMainCamera() == null) {
            return 0.0f;
        }

        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        float maxShake = 0.0f;

        // 0. Incantation Micro-Tremor while casting Gargantua
        if (ClientMagicData.isCasting() && GargantuaSpell.ID.toString().equals(ClientMagicData.getCastingSpellId())) {
            float progress = ClientMagicData.getCastCompletionPercent();
            // Builds exponentially toward the 4.0-second climax
            float castTremor = (float) Math.pow(progress, 2.5) * 0.45f;
            maxShake = Math.max(maxShake, castTremor);
        }

        for (GargantuaEntity gargantua : GargantuaPostProcessor.getActiveClientEntities(mc)) {
            if (gargantua.isAlive()) {
                float visualAge = gargantua.getVisualAgeTicks(partialTick);
                double dist = Math.sqrt(camPos.distanceToSqr(gargantua.centre(partialTick)));

                // 1. Spacetime Rupture Opening Jolt
                float openingShake = 0.0f;
                int tearEnd = gargantua.getTearEndTick();
                if (tearEnd > 0 && visualAge >= 0.0f && visualAge <= (float) tearEnd) {
                    float openProgress = visualAge / (float) tearEnd;
                    float distFactor = (float) Mth.clamp(1.0 - dist / 80.0d, 0.0, 1.0);
                    float jolt = (float) (Math.sin(openProgress * Math.PI) * 3.5f);
                    openingShake = jolt * distFactor;
                }

                // 2. Continuous cosmic low rumble while active, escalating exponentially into critical collapse
                float rumble = 0.0f;
                int critEnd = gargantua.getCriticalEndTick();
                if (visualAge > (float) tearEnd && visualAge < (float) critEnd) {
                    float distFactor = (float) Mth.clamp(1.0 - dist / 80.0d, 0.0, 1.0);
                    float baseRumble = 0.20f;
                    float crit = gargantua.criticality(partialTick);
                    rumble = (baseRumble + 4.5f * (crit * crit)) * distFactor;
                }

                // 3. Colossal Supernova Detonation Shockwave at blast tick
                float blastElapsed = visualAge - (float) gargantua.getBlastTick();
                float blastShake = 0.0f;
                if (blastElapsed >= 0.0f && blastElapsed <= SHAKE_TICKS) {
                    float timeFactor = 1.0f - blastElapsed / SHAKE_TICKS;
                    float distFactor = (float) Mth.clamp(1.0 - dist / SHAKE_RANGE, 0.0, 1.0);
                    blastShake = SHAKE_STRENGTH * (timeFactor * timeFactor) * distFactor;
                }

                maxShake = Math.max(maxShake, Math.max(openingShake, Math.max(rumble, blastShake)));
            }
        }

        float mult = (float) ZoltraakCinematicClientConfig.getCameraShakeMultiplier();
        return maxShake * mult;
    }

    /**
     * Applies harmonic multi-frequency camera shake and cinematic roll directly to the PoseStack.
     */
    public static void applyHarmonicShake(PoseStack poseStack, float partialTick) {
        // Apply cinematic camera roll if a director cutscene is active
        float cinematicRoll = GargantuaCameraDirector.getRoll();
        if (Math.abs(cinematicRoll) > 0.001f) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(cinematicRoll));
        }

        float shake = currentShake(partialTick);
        if (shake <= 0.001f) return;

        float t = (float) (System.nanoTime() % 100000000000L) / 1.0E9f;

        // Harmonic multi-frequency sines for realistic organic rumble
        float yaw   = (float) (Math.sin(t * 37.0) * 0.6 + Math.sin(t * 61.0) * 0.4) * 2.6f * shake;
        float pitch = (float) (Math.sin(t * 43.0 + 1.3) * 0.6 + Math.sin(t * 71.0) * 0.4) * 2.2f * shake;
        float roll  = (float) Math.sin(t * 29.0 + 0.7) * 3.0f * shake;

        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
    }
}
