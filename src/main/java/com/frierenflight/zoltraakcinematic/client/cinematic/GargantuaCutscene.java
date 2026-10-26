package com.frierenflight.zoltraakcinematic.client.cinematic;

import com.frierenflight.zoltraakcinematic.entity.GargantuaEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 🌌 GargantuaCutscene
 * Choreographs the 5-act cinematic camera journey for Gargantua:
 * Act 0: Incantation Dolly Zoom & Arcane Levitation
 * Act 1: Spacetime Rupture & Opening Jolt
 * Act 2: Grand Accretion Orbit & Gravitational Lensing
 * Act 3: Critical Implosion & Micro-Singularity Zoom
 * Act 4: Supernova Detonation & Wide Landscape Panorama
 */
public final class GargantuaCutscene {

    private GargantuaCutscene() {}

    /**
     * Calculates the camera pose during spell casting (Act 0: Incantation).
     * @param caster The entity casting Gargantua
     * @param progress Progress of cast from 0.0f (start) to 1.0f (cast finished)
     * @param partialTick Partial game tick
     */
    public static CameraPose getCastingPose(LivingEntity caster, float progress, float partialTick) {
        Vec3 eyePos = caster.getEyePosition(partialTick);
        Vec3 look = caster.getViewVector(partialTick);
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);

        // Smoothstep progress curve
        float k = progress * progress * (3.0f - 2.0f * progress);

        // Cinematic Over-The-Shoulder Aiming:
        // Camera pulls back behind the right shoulder as arcane energy builds up
        double distance = Mth.lerp((double) k, 2.4d, 5.2d);
        double heightOffset = Mth.lerp((double) k, 0.45d, 0.95d);
        double sideOffset = Mth.lerp((double) k, 1.25d, 1.65d);

        // Position camera behind and to the right of the caster
        Vec3 camPos = eyePos.subtract(look.scale(distance))
                .add(right.scale(sideOffset))
                .add(0.0d, heightOffset, 0.0d);

        // Focal point aims far forward along caster's gaze toward target horizon
        Vec3 focalPoint = eyePos.add(look.scale(64.0d)).add(0.0d, 1.2d, 0.0d);

        // Dynamic cinematic FOV focus: narrows from 70 deg to 58 deg as singularity charges
        float fov = Mth.lerp(k, 70.0f, 58.0f);
        float roll = Mth.lerp(k, 0.0f, -2.0f);

        return new CameraPose(camPos, focalPoint, fov, roll);
    }

    /**
     * Calculates the camera pose during active Gargantua singularity lifetime (Acts 1 through 4).
     */
    public static CameraPose getEntityPose(GargantuaEntity gargantua, float partialTick) {
        float visualAge = gargantua.getVisualAgeTicks(partialTick);
        Vec3 center = gargantua.centre(partialTick);
        int blastTick = gargantua.getBlastTick();
        int tearEnd = gargantua.getTearEndTick();
        int critStart = gargantua.getCriticalStartTick();

        // -------------------------------------------------------------
        // Act 1: Spacetime Rupture & Opening Jolt (Age 0 -> tearEnd)
        // -------------------------------------------------------------
        if (visualAge <= (float) tearEnd) {
            float k = Mth.clamp(visualAge / (float) tearEnd, 0.0f, 1.0f);
            float smoothK = k * k * (3.0f - 2.0f * k);

            // Camera sweeps back smoothly from 75 blocks to 110 blocks with majestic upward lift
            double dist = Mth.lerp((double) smoothK, 75.0d, 110.0d);
            double height = Mth.lerp((double) smoothK, 10.0d, 24.0d);

            Vec3 camPos = center.add(dist * 0.85d, height, dist * 0.52d);
            float fov = Mth.lerp(smoothK, 74.0f, 65.0f);
            float roll = (float) Math.sin(smoothK * Math.PI) * -3.5f;

            return new CameraPose(camPos, center, fov, roll);
        }

        // -------------------------------------------------------------
        // Act 3: Critical Implosion (critStart -> blastTick)
        // -------------------------------------------------------------
        if (visualAge > (float) critStart && visualAge <= (float) blastTick) {
            float span = Math.max(1.0f, (float) (blastTick - critStart));
            float k = Mth.clamp((visualAge - (float) critStart) / span, 0.0f, 1.0f);
            float implosionK = (float) Math.pow(k, 2.2); // Accelerates inward

            // Camera zooms in from 110 blocks to 82 blocks
            double dist = Mth.lerp((double) implosionK, 110.0d, 82.0d);
            double height = Mth.lerp((double) implosionK, 22.0d, 30.0d);

            double angle = (double) visualAge * 0.010d;
            Vec3 camPos = center.add(Math.cos(angle) * dist, height, Math.sin(angle) * dist);

            // Hyper-frequency micro tremor in roll
            float tremor = (float) Math.sin((double) visualAge * 1.8d) * 1.4f * implosionK;
            float fov = Mth.lerp(implosionK, 65.0f, 73.0f);

            return new CameraPose(camPos, center, fov, tremor);
        }

        // -------------------------------------------------------------
        // Act 4: Supernova Detonation & Wide Landscape Panorama (blastTick -> End of Lifetime)
        // -------------------------------------------------------------
        if (visualAge > (float) blastTick) {
            float k = Mth.clamp((visualAge - (float) blastTick) / 80.0f, 0.0f, 1.0f);
            float smoothK = k * (2.0f - k); // Deceleration curve

            // Majestic wide panoramic shot pulling back from 118 blocks to 152 blocks
            double dist = Mth.lerp((double) smoothK, 118.0d, 152.0d);
            double height = Mth.lerp((double) smoothK, 28.0d, 48.0d);

            Vec3 camPos = center.add(dist * 0.707d, height, dist * 0.707d);
            float fov = Mth.lerp(smoothK, 78.0f, 68.0f);
            float roll = Mth.lerp(smoothK, -2.5f, 0.0f);

            return new CameraPose(camPos, center, fov, roll);
        }

        // -------------------------------------------------------------
        // Act 2: Grand Accretion Orbit & Gravitational Lensing (Holding Phase)
        // -------------------------------------------------------------
        double orbitAngle = (double) (visualAge - (float) tearEnd) * 0.007d;
        double dist = 110.0d + Math.sin((double) visualAge * 0.012d) * 12.0d;
        double height = 22.0d + Math.cos((double) visualAge * 0.016d) * 8.0d;

        Vec3 camPos = center.add(
                Math.cos(orbitAngle) * dist,
                height,
                Math.sin(orbitAngle) * dist
        );

        float orbitRoll = (float) Math.sin(orbitAngle * 2.0d) * 1.6f;
        return new CameraPose(camPos, center, 65.0f, orbitRoll);
    }
}
