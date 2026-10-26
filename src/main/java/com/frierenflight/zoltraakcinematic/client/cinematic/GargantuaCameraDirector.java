package com.frierenflight.zoltraakcinematic.client.cinematic;

import com.frierenflight.zoltraakcinematic.client.renderer.GargantuaPostProcessor;
import com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicClientConfig;
import com.frierenflight.zoltraakcinematic.entity.GargantuaEntity;
import com.frierenflight.zoltraakcinematic.spell.GargantuaSpell;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.List;

/**
 * 🎬 GargantuaCameraDirector
 * Controls camera takeover, smooth cinematic blending, terrain obstacle avoidance (clipping),
 * FOV modulation, and player skip functionality.
 */
public final class GargantuaCameraDirector {

    private static boolean enabled = true;
    private static boolean skipped = false;
    private static float currentWeight = 0.0f;
    private static float lastRoll = 0.0f;
    private static float lastFov = 70.0f;

    private GargantuaCameraDirector() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void skip() {
        skipped = true;
    }

    public static void resetSkip() {
        skipped = false;
    }

    public static boolean isActive() {
        return enabled && !skipped && currentWeight > 0.001f;
    }

    public static float getRoll() {
        return lastRoll * currentWeight;
    }

    public static float getFov(float originalFov) {
        if (!isActive()) {
            return originalFov;
        }
        return Mth.lerp(currentWeight, originalFov, lastFov);
    }

    /**
     * Called every camera setup tick to compute the active cinematic camera pose.
     * Returns null if no cinematic scene is active.
     */
    public static CameraPose apply(Vec3 vanillaPos, float vanillaYaw, float vanillaPitch, float partialTick) {
        if (!enabled || !ZoltraakCinematicClientConfig.isCinematicCameraEnabled()) {
            currentWeight = Math.max(0.0f, currentWeight - 0.1f);
            skipped = false;
            return null;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            currentWeight = 0.0f;
            skipped = false;
            return null;
        }

        CameraPose targetShot = null;

        // 1. Check if the local player is currently casting Gargantua (Act 0: Incantation)
        boolean isCasting = ClientMagicData.isCasting() && GargantuaSpell.ID.toString().equals(ClientMagicData.getCastingSpellId());
        if (isCasting) {
            float progress = ClientMagicData.getCastCompletionPercent();
            targetShot = GargantuaCutscene.getCastingPose(mc.player, progress, partialTick);
        }

        // 2. Check if there are active Gargantua entities nearby
        GargantuaEntity closest = null;
        if (targetShot == null) {
            List<GargantuaEntity> activeSingularities = GargantuaPostProcessor.getActiveClientEntities(mc);
            if (!activeSingularities.isEmpty()) {
                double closestDistSq = Double.MAX_VALUE;
                for (GargantuaEntity g : activeSingularities) {
                    double d = g.distanceToSqr(mc.player);
                    if (d < closestDistSq && d <= (240.0 * 240.0)) {
                        closestDistSq = d;
                        closest = g;
                    }
                }
                if (closest != null) {
                    targetShot = GargantuaCutscene.getEntityPose(closest, partialTick);
                }
            }
        }

        // Auto-reset skip when neither casting nor near any active black hole
        if (!isCasting && closest == null) {
            skipped = false;
        }

        if (skipped) {
            currentWeight = Math.max(0.0f, currentWeight - 0.1f);
            return null;
        }

        if (targetShot == null) {
            currentWeight = Math.max(0.0f, currentWeight - 0.08f);
            if (currentWeight <= 0.001f) {
                return null;
            }
        } else {
            currentWeight = Math.min(1.0f, currentWeight + 0.12f);
        }

        if (targetShot == null) return null;

        // Perform smart block collision clipping
        CameraPose clippedShot = clip(mc.level, targetShot, mc.player);

        lastRoll = clippedShot.roll();
        lastFov = clippedShot.fov();

        // Construct vanilla camera pose for smooth lerping
        double yawRad = Math.toRadians(vanillaYaw);
        double pitchRad = Math.toRadians(vanillaPitch);
        Vec3 vanillaLook = vanillaPos.add(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        );
        CameraPose vanillaPose = new CameraPose(vanillaPos, vanillaLook, vanillaYaw, vanillaPitch, lastFov, 0.0f);

        return vanillaPose.lerp(clippedShot, currentWeight);
    }

    /**
     * Prevents the camera from clipping into terrain, ceilings, or cave walls.
     */
    private static CameraPose clip(Level level, CameraPose pose, Entity viewer) {
        Vec3 from = pose.look().add(0.0d, 0.5d, 0.0d);
        Vec3 toward = pose.position().subtract(from);
        double length = toward.length();
        if (length < 0.001d) return pose;

        Vec3 step = toward.scale(1.0d / length);
        double walked = 0.0d;
        while (isSolid(level, from)) {
            if (walked >= length) return pose;
            from = from.add(step);
            walked += 1.0d;
        }

        BlockHitResult hit = level.clip(new ClipContext(
                from,
                pose.position(),
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                viewer
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 dir = toward.normalize();
            Vec3 hitLoc = hit.getLocation();
            double hitDist = hitLoc.distanceTo(from);

            // If an obstacle would force the camera closer than 60 blocks (inside the 54-block disk),
            // elevate camera upward to clear the obstacle while maintaining majestic distance
            if (hitDist < 60.0d) {
                Vec3 elevatedPos = pose.position().add(0.0d, 24.0d, 0.0d);
                BlockHitResult elevatedHit = level.clip(new ClipContext(
                        from, elevatedPos,
                        ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, viewer
                ));
                if (elevatedHit.getType() != HitResult.Type.BLOCK || elevatedHit.getLocation().distanceTo(from) > hitDist) {
                    Vec3 safeElevated = (elevatedHit.getType() == HitResult.Type.BLOCK)
                            ? elevatedHit.getLocation().subtract(dir.scale(0.5d))
                            : elevatedPos;
                    return new CameraPose(safeElevated, pose.look(), pose.fov(), pose.roll());
                }
            }

            Vec3 safePos = hitLoc.subtract(dir.scale(0.45d));
            return new CameraPose(safePos, pose.look(), pose.fov(), pose.roll());
        }

        return pose;
    }

    private static boolean isSolid(Level level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at.x, at.y, at.z);
        return pos.getY() < level.getMinBuildHeight() ||
                !level.getBlockState(pos).getVisualShape(level, pos, CollisionContext.empty()).isEmpty();
    }
}
