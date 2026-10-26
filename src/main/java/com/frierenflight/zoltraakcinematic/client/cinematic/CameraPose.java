package com.frierenflight.zoltraakcinematic.client.cinematic;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 🎥 CameraPose
 * High-precision vector representation of a cinematic camera pose,
 * containing 3D position, focal look-target, Field of View (FOV), and camera roll (tilt).
 */
public record CameraPose(Vec3 position, Vec3 look, float yaw, float pitch, float fov, float roll) {

    public CameraPose(Vec3 position, Vec3 look, float fov, float roll) {
        this(position, look, computeYaw(position, look), computePitch(position, look), fov, roll);
    }

    public static float computeYaw(Vec3 pos, Vec3 look) {
        Vec3 d = look.subtract(pos);
        return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
    }

    public static float computePitch(Vec3 pos, Vec3 look) {
        Vec3 d = look.subtract(pos);
        double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
        return (float) (-Math.toDegrees(Math.atan2(d.y, horizontal)));
    }

    public CameraPose lerp(CameraPose other, float k) {
        k = Mth.clamp(k, 0.0f, 1.0f);
        return new CameraPose(
                this.position.lerp(other.position, (double) k),
                this.look.lerp(other.look, (double) k),
                Mth.rotLerp(k, this.yaw, other.yaw),
                Mth.rotLerp(k, this.pitch, other.pitch),
                Mth.lerp(k, this.fov, other.fov),
                Mth.lerp(k, this.roll, other.roll)
        );
    }
}
