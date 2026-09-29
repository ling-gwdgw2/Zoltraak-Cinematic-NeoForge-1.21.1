package com.frierenflight.zoltraakcinematic.entity.boss.ai;

import com.frierenflight.zoltraakcinematic.entity.boss.QualBossEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 📜 QualFollowPlayerGoal — 3D Aerial Flight, Pursuit & Player Following AI.
 *
 * Implements intelligent demonic/witch flight navigation:
 * 1. COMBAT PURSUIT & POSITIONING:
 *    - Far range (> 20 blocks): Enters High-Speed Aerial Pursuit (speed 1.4x), chasing the target in 3D air.
 *    - Mid range (8–20 blocks): Enters Tactical Orbital Strafing (circling at 14–18 blocks, hovering 4.5–8 blocks above).
 *    - Close range (< 7.5 blocks): Enters Mage Evasive Kiting (glides backward/upward to maintain casting distance).
 *
 * 2. NON-COMBAT / CREATIVE / OBSERVATION FOLLOWING:
 *    - When untargeted, tracks the nearest player within 48 blocks.
 *    - Follows the player gracefully through the air, matching altitude and hovering nearby (like Elaina observing travelers).
 *
 * 3. 3D TERRAIN COLLISION & ELEVATION PROTECTION:
 *    - Prevents clipping into terrain, trees, or mountains.
 *    - Dynamically ascends over obstacles.
 */
public class QualFollowPlayerGoal extends Goal {
    private final QualBossEntity qual;
    private double currentOrbitAngle;
    private int orbitDirection = 1;
    private int directionChangeTimer = 120;
    private int altitudeWobbleTimer = 0;
    private double altitudeOffset = 0.0;

    public QualFollowPlayerGoal(QualBossEntity qual) {
        this.qual = qual;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.currentOrbitAngle = qual.getRandom().nextDouble() * Math.PI * 2.0;
    }

    @Override
    public boolean canUse() {
        return qual.isAlive() && !qual.isCataclysmicCharging();
    }

    @Override
    public boolean canContinueToUse() {
        return qual.isAlive() && !qual.isCataclysmicCharging();
    }

    @Override
    public void tick() {
        LivingEntity combatTarget = qual.getTarget();

        if (combatTarget != null && combatTarget.isAlive()) {
            tickCombatFlight(combatTarget);
        } else {
            tickNonCombatFlight();
        }
    }

    /**
     * Combat flight behavior: pursuit, orbital strafe, and evasive kite.
     */
    private void tickCombatFlight(LivingEntity target) {
        qual.getLookControl().setLookAt(target, 35.0f, 35.0f);

        double dist = qual.distanceTo(target);
        int phase = qual.getPhase();

        // Subtle altitude oscillation so the boss is a living, breathing target
        if (--altitudeWobbleTimer <= 0) {
            altitudeOffset = (qual.getRandom().nextDouble() - 0.5) * 2.0;
            altitudeWobbleTimer = 40 + qual.getRandom().nextInt(40);
        }

        double baseAltitude = (phase == 2) ? 8.5 : 5.0;
        double desiredAltitude = target.getY() + baseAltitude + altitudeOffset;

        if (dist > 20.0) {
            // High-Speed Aerial Pursuit: Target is running away, flying, or far away
            double pursuitX = target.getX();
            double pursuitZ = target.getZ();
            double pursuitY = adjustAltitudeForTerrain(pursuitX, desiredAltitude, pursuitZ);

            qual.getMoveControl().setWantedPosition(pursuitX, pursuitY, pursuitZ, 1.4);
        } else if (dist < 7.5) {
            // Evasive Backpedal / Kite: Target has rushed into melee range
            Vec3 retreatDir = qual.position().subtract(target.position()).normalize();
            if (retreatDir.lengthSqr() < 0.01) {
                retreatDir = new Vec3(1, 0, 0);
            }
            double kiteX = target.getX() + retreatDir.x * 14.0;
            double kiteZ = target.getZ() + retreatDir.z * 14.0;
            double kiteY = adjustAltitudeForTerrain(kiteX, target.getY() + 6.0, kiteZ);

            qual.getMoveControl().setWantedPosition(kiteX, kiteY, kiteZ, 1.25);
        } else {
            // Tactical Orbital Strafing & Dynamic Repositioning
            if (--directionChangeTimer <= 0) {
                orbitDirection = (qual.getRandom().nextBoolean()) ? 1 : -1;
                directionChangeTimer = 100 + qual.getRandom().nextInt(80);
            }

            double desiredDistance = (phase == 2) ? 19.0 : 15.5;
            currentOrbitAngle += orbitDirection * 0.025;

            double targetX = target.getX() + Math.cos(currentOrbitAngle) * desiredDistance;
            double targetZ = target.getZ() + Math.sin(currentOrbitAngle) * desiredDistance;
            double targetY = adjustAltitudeForTerrain(targetX, desiredAltitude, targetZ);

            qual.getMoveControl().setWantedPosition(targetX, targetY, targetZ, 1.0);
        }
    }

    /**
     * Non-combat flight behavior: follows nearby players (Creative / testing / neutral).
     */
    private void tickNonCombatFlight() {
        Player nearestPlayer = qual.level().getNearestPlayer(qual, 48.0);

        if (nearestPlayer != null && nearestPlayer.isAlive()) {
            qual.getLookControl().setLookAt(nearestPlayer, 30.0f, 30.0f);

            double dist = qual.distanceTo(nearestPlayer);
            double targetY = nearestPlayer.getY() + 2.5;

            if (dist > 30.0) {
                // Far: fly quickly towards player
                double tx = nearestPlayer.getX();
                double tz = nearestPlayer.getZ();
                double ty = adjustAltitudeForTerrain(tx, targetY + 1.5, tz);
                qual.getMoveControl().setWantedPosition(tx, ty, tz, 1.35);
            } else if (dist > 6.0) {
                // Medium: smoothly fly to player's side
                double followAngle = currentOrbitAngle;
                double tx = nearestPlayer.getX() + Math.cos(followAngle) * 4.5;
                double tz = nearestPlayer.getZ() + Math.sin(followAngle) * 4.5;
                double ty = adjustAltitudeForTerrain(tx, targetY, tz);
                qual.getMoveControl().setWantedPosition(tx, ty, tz, 1.05);
            } else {
                // Close: gentle curious hovering orbit near player
                currentOrbitAngle += 0.015;
                double tx = nearestPlayer.getX() + Math.cos(currentOrbitAngle) * 4.5;
                double tz = nearestPlayer.getZ() + Math.sin(currentOrbitAngle) * 4.5;
                double ty = adjustAltitudeForTerrain(tx, targetY, tz);
                qual.getMoveControl().setWantedPosition(tx, ty, tz, 0.7);
            }
        } else {
            // Idle wander when no player within 48 blocks
            if (qual.tickCount % 50 == 0) {
                double wanderAngle = qual.getRandom().nextDouble() * Math.PI * 2.0;
                double wanderDist = 4.0 + qual.getRandom().nextDouble() * 5.0;
                double wx = qual.getX() + Math.cos(wanderAngle) * wanderDist;
                double wz = qual.getZ() + Math.sin(wanderAngle) * wanderDist;
                double wy = adjustAltitudeForTerrain(wx, qual.getY(), wz);

                qual.getMoveControl().setWantedPosition(wx, wy, wz, 0.6);
            }
        }
    }

    /**
     * Prevents clipping into terrain, leaves, or mountains, keeping Qual airborne.
     */
    private double adjustAltitudeForTerrain(double x, double preferredY, double z) {
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos(x, preferredY, z);

        // If preferred position is inside solid blocks, push upward
        while (mpos.getY() < qual.level().getMaxBuildHeight() && !qual.level().isEmptyBlock(mpos)) {
            mpos.move(0, 1, 0);
        }
        double clearY = mpos.getY();

        // Scan downwards to ensure clearance from terrain (minimum 3.5 blocks above ground)
        int steps = 0;
        while (steps < 20 && mpos.getY() > qual.level().getMinBuildHeight() && qual.level().isEmptyBlock(mpos)) {
            mpos.move(0, -1, 0);
            steps++;
        }

        if (steps < 20) {
            double groundY = mpos.getY() + 1.0;
            return Math.max(clearY, groundY + 3.5);
        }
        return Math.max(preferredY, clearY);
    }
}
