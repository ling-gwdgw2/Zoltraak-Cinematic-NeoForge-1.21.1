package com.frierenflight.zoltraakcinematic.client.cinematic;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 🎥 CameraPose
 * High-precision vector representation of a cinematic camera pose,
 * containing 3D position, focal look-target, Field of View (FOV), and camera roll (tilt).
 */
public record CameraPose(Vec3 position, Vec3 look, float fov, float roll) {

    public float yaw() {
        Vec3 d = this.look.subtract(this.position);
        return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
    }

    public float pitch() {
        Vec3 d = this.look.subtract(this.position);
        double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
        return (float) (-Math.toDegrees(Math.atan2(d.y, horizontal)));
    }

    public CameraPose lerp(CameraPose other, float k) {
        k = Mth.clamp(k, 0.0f, 1.0f);
        return new CameraPose(
                this.position.lerp(other.position, (double) k),
                this.look.lerp(other.look, (double) k),
                Mth.lerp(k, this.fov, other.fov),
                Mth.lerp(k, this.roll, other.roll)
        );
    }
}
