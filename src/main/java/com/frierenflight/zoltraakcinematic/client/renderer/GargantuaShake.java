package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.entity.GargantuaEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * GargantuaShake
 * Computes camera shake, spacetime rupture jolts, and cataclysmic supernova rumbling for all active Gargantua singularities.
 */
public final class GargantuaShake {
    private static final float SHAKE_TICKS = 55.0f;
    private static final float SHAKE_STRENGTH = 18.0f;
    private static final double SHAKE_RANGE = 128.0d;

    private GargantuaShake() {}

    public static float currentShake(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameRenderer == null || mc.gameRenderer.getMainCamera() == null) {
            return 0.0f;
        }

        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        float maxShake = 0.0f;

        for (GargantuaEntity gargantua : GargantuaPostProcessor.getActiveClientEntities(mc)) {
            if (gargantua.isAlive()) {
                float visualAge = gargantua.getVisualAgeTicks(partialTick);
                double dist = Math.sqrt(camPos.distanceToSqr(gargantua.centre(partialTick)));

                // 1. Spacetime Rupture Opening Jolt (ticks 0 - 60)
                float openingShake = 0.0f;
                if (visualAge >= 0.0f && visualAge <= (float) GargantuaEntity.TEAR_END_TICK) {
                    float openProgress = visualAge / (float) GargantuaEntity.TEAR_END_TICK;
                    float distFactor = (float) Mth.clamp(1.0 - dist / 80.0d, 0.0, 1.0);
                    float jolt = (float) (Math.sin(openProgress * Math.PI) * 2.8f);
                    openingShake = jolt * distFactor;
                }

                // 2. Continuous cosmic low rumble while active, escalating exponentially into critical collapse
                float rumble = 0.0f;
                if (visualAge > (float) GargantuaEntity.TEAR_END_TICK && visualAge < (float) GargantuaEntity.CRITICAL_END_TICK) {
                    float distFactor = (float) Mth.clamp(1.0 - dist / 80.0d, 0.0, 1.0);
                    float baseRumble = 0.20f;
                    float crit = gargantua.criticality(partialTick);
                    rumble = (baseRumble + 3.8f * (crit * crit)) * distFactor;
                }

                // 3. Colossal Supernova Detonation Shockwave at BLAST_TICK (1100 ticks)
                float blastElapsed = visualAge - (float) GargantuaEntity.BLAST_TICK;
                float blastShake = 0.0f;
                if (blastElapsed >= 0.0f && blastElapsed <= SHAKE_TICKS) {
                    float timeFactor = 1.0f - blastElapsed / SHAKE_TICKS;
                    float distFactor = (float) Mth.clamp(1.0 - dist / SHAKE_RANGE, 0.0, 1.0);
                    blastShake = SHAKE_STRENGTH * (timeFactor * timeFactor) * distFactor;
                }

                maxShake = Math.max(maxShake, Math.max(openingShake, Math.max(rumble, blastShake)));
            }
        }

        return maxShake;
    }
}
