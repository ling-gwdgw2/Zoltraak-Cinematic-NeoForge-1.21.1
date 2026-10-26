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

        // Dolly Zoom: Camera pulls backward from 2.8 blocks to 6.2 blocks
        double distance = Mth.lerp((double) k, 2.8d, 6.2d);
        double heightOffset = Mth.lerp((double) k, 0.15d, 0.65d);
        double sideOffset = Mth.lerp((double) k, 1.2d, 1.8d);

        // Position camera in front-right quarter of the caster
        Vec3 camPos = eyePos.add(look.scale(distance))
                .add(right.scale(sideOffset))
                .add(0.0d, heightOffset, 0.0d);

        // Focal point centers on caster's upper torso / staff
        Vec3 focalPoint = eyePos.subtract(0.0d, 0.2d, 0.0d);

        // Vertigo FOV compression: FOV narrows from 72 deg to 46 deg
        float fov = Mth.lerp(k, 72.0f, 46.0f);
        float roll = Mth.lerp(k, 0.0f, -3.2f);

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

            // Camera sweeps back from 18 blocks to 38 blocks with upward lift
            double dist = Mth.lerp((double) smoothK, 18.0d, 38.0d);
            double height = Mth.lerp((double) smoothK, -2.0d, 8.0d);

            Vec3 camPos = center.add(dist * 0.85d, height, dist * 0.52d);
            float fov = Mth.lerp(smoothK, 78.0f, 68.0f);
            float roll = (float) Math.sin(smoothK * Math.PI) * -4.5f;

            return new CameraPose(camPos, center, fov, roll);
        }

        // -------------------------------------------------------------
        // Act 3: Critical Implosion (critStart -> blastTick)
        // -------------------------------------------------------------
        if (visualAge > (float) critStart && visualAge <= (float) blastTick) {
            float span = Math.max(1.0f, (float) (blastTick - critStart));
            float k = Mth.clamp((visualAge - (float) critStart) / span, 0.0f, 1.0f);
            float implosionK = (float) Math.pow(k, 2.5); // Accelerates inward

            // Camera zooms in from 45 blocks to 24 blocks
            double dist = Mth.lerp((double) implosionK, 45.0d, 24.0d);
            double height = Mth.lerp((double) implosionK, 6.0d, 14.0d);

            double angle = (double) visualAge * 0.012d;
            Vec3 camPos = center.add(Math.cos(angle) * dist, height, Math.sin(angle) * dist);

            // Hyper-frequency micro tremor in roll
            float tremor = (float) Math.sin((double) visualAge * 1.8d) * 1.5f * implosionK;
            float fov = Mth.lerp(implosionK, 68.0f, 76.0f);

            return new CameraPose(camPos, center, fov, tremor);
        }

        // -------------------------------------------------------------
        // Act 4: Supernova Detonation & Wide Landscape Panorama (blastTick -> blastTick + 80)
        // -------------------------------------------------------------
        if (visualAge > (float) blastTick && visualAge <= (float) blastTick + 80.0f) {
            float k = Mth.clamp((visualAge - (float) blastTick) / 80.0f, 0.0f, 1.0f);
            float smoothK = k * (2.0f - k); // Deceleration curve

            // Majestic wide panoramic shot at 78 blocks distance
            double dist = Mth.lerp((double) smoothK, 62.0d, 82.0d);
            double height = Mth.lerp((double) smoothK, 16.0d, 28.0d);

            Vec3 camPos = center.add(dist * 0.707d, height, dist * 0.707d);
            float fov = Mth.lerp(smoothK, 84.0f, 72.0f);
            float roll = Mth.lerp(smoothK, -3.0f, 0.0f);

            return new CameraPose(camPos, center, fov, roll);
        }

        // -------------------------------------------------------------
        // Act 2: Grand Accretion Orbit & Gravitational Lensing (Holding Phase)
        // -------------------------------------------------------------
        double orbitAngle = (double) (visualAge - (float) tearEnd) * 0.008d;
        double dist = 48.0d + Math.sin((double) visualAge * 0.015d) * 4.0d;
        double height = 6.0d + Math.cos((double) visualAge * 0.02d) * 5.0d;

        Vec3 camPos = center.add(
                Math.cos(orbitAngle) * dist,
                height,
                Math.sin(orbitAngle) * dist
        );

        float orbitRoll = (float) Math.sin(orbitAngle * 2.0d) * 1.8f;
        return new CameraPose(camPos, center, 68.0f, orbitRoll);
    }
}
