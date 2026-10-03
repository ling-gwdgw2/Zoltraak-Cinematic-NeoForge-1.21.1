package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.entity.GargantuaEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * GargantuaShake
 * Computes camera shake and harmonic rumbling for all active Gargantua singularities.
 */
public final class GargantuaShake {
    private static final float SHAKE_TICKS = 26.0f;
    private static final float SHAKE_STRENGTH = 8.5f;
    private static final double SHAKE_RANGE = 96.0d;

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

                // Continuous ominous low rumble while active, intensifying during criticality
                float rumble = 0.0f;
                if (visualAge > 10.0f && visualAge < (float) GargantuaEntity.CRITICAL_END_TICK) {
                    double dist = Math.sqrt(camPos.distanceToSqr(gargantua.centre(partialTick)));
                    float distFactor = (float) Mth.clamp(1.0 - dist / 64.0d, 0.0, 1.0);
                    rumble = (0.25f + 0.75f * gargantua.criticality(partialTick)) * distFactor;
                }

                // Colossal detonation shockwave at tick 290
                float blastElapsed = visualAge - (float) GargantuaEntity.BLAST_TICK;
                float blastShake = 0.0f;
                if (blastElapsed >= 0.0f && blastElapsed <= SHAKE_TICKS) {
                    float timeFactor = 1.0f - blastElapsed / SHAKE_TICKS;
                    double dist = Math.sqrt(camPos.distanceToSqr(gargantua.centre(partialTick)));
                    float distFactor = (float) Mth.clamp(1.0 - dist / SHAKE_RANGE, 0.0, 1.0);
                    blastShake = SHAKE_STRENGTH * (timeFactor * timeFactor) * distFactor;
                }

                maxShake = Math.max(maxShake, Math.max(rumble, blastShake));
            }
        }

        return maxShake;
    }
}
