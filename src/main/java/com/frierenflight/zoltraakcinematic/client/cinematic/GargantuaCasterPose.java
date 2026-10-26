package com.frierenflight.zoltraakcinematic.client.cinematic;

import com.frierenflight.zoltraakcinematic.spell.GargantuaSpell;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 🪄 GargantuaCasterPose
 * Computes elegant arcane casting postures, arm rotations, and gravitational levitation offsets
 * for entities channeling Gargantua.
 */
public final class GargantuaCasterPose {

    private static float prevActiveWeight = 0.0f;
    private static float activeWeight = 0.0f;

    private GargantuaCasterPose() {}

    public static void clientTick(Minecraft mc) {
        prevActiveWeight = activeWeight;
        if (mc.player == null) {
            activeWeight = 0.0f;
            prevActiveWeight = 0.0f;
            return;
        }

        boolean isCastingGargantua = ClientMagicData.isCasting() &&
                GargantuaSpell.ID.toString().equals(ClientMagicData.getCastingSpellId());

        if (isCastingGargantua) {
            activeWeight = Math.min(1.0f, activeWeight + 0.10f);
        } else {
            activeWeight = Math.max(0.0f, activeWeight - 0.15f);
        }
    }

    public static float getPoseWeight(LivingEntity entity) {
        if (!(entity instanceof Player player)) return 0.0f;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getId() != player.getId()) {
            return 0.0f;
        }

        float partial = 1.0f;
        if (mc.level != null && !mc.isPaused()) {
            try {
                partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
            } catch (Throwable ignored) {
                partial = 1.0f;
            }
        }

        return Mth.lerp(partial, prevActiveWeight, activeWeight);
    }

    /**
     * Applies the majestic dual-arm celestial channel pose to the player's model.
     */
    public static void apply(LivingEntity entity, ModelPart rightArm, ModelPart leftArm, float headPitch, float netHeadYaw) {
        float weight = getPoseWeight(entity);
        if (weight <= 0.001f) return;

        // Smooth curve
        float w = weight * weight * (3.0f - 2.0f * weight);

        // Right arm holding staff aloft: pointed forward-upward along gaze with arcane tilt
        float lookPitchRad = (float) Math.toRadians(headPitch);
        float lookYawRad = (float) Math.toRadians(netHeadYaw);

        float targetRightX = lookPitchRad - 1.35f; // Raised high with staff
        float targetRightY = lookYawRad * 0.4f - 0.25f;
        float targetRightZ = 0.35f;

        // Left arm outstretched in channeling gesture
        float targetLeftX = lookPitchRad - 0.95f;
        float targetLeftY = lookYawRad * 0.4f + 0.35f;
        float targetLeftZ = -0.45f;

        rightArm.xRot = Mth.lerp(w, rightArm.xRot, targetRightX);
        rightArm.yRot = Mth.lerp(w, rightArm.yRot, targetRightY);
        rightArm.zRot = Mth.lerp(w, rightArm.zRot, targetRightZ);

        leftArm.xRot = Mth.lerp(w, leftArm.xRot, targetLeftX);
        leftArm.yRot = Mth.lerp(w, leftArm.yRot, targetLeftY);
        leftArm.zRot = Mth.lerp(w, leftArm.zRot, targetLeftZ);
    }
}
