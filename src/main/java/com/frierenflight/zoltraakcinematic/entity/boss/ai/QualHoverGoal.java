package com.frierenflight.zoltraakcinematic.entity.boss.ai;

import com.frierenflight.zoltraakcinematic.entity.boss.QualBossEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 📜 QualHoverGoal — 3D Demonic Levitation & Tactical Aerial Positioning.
 *
 * Behaviors:
 * - Maintains 4.5 to 12.0 blocks aerial height above target / ground
 * - Performs smooth orbital strafing (Circling orbit at 14–22 blocks distance)
 * - Dynamic altitude adjustment based on combat phase (Phase 2 ascends higher
 * for Barrage Matrix)
 * - Obstacle collision avoidance for natural flight around structures
 */
public class QualHoverGoal extends Goal {
    private final QualBossEntity qual;
    private double currentOrbitAngle;
    private int orbitDirection = 1; // 1 = clockwise, -1 = counter-clockwise
    private int directionChangeTimer = 120;

    public QualHoverGoal(QualBossEntity qual) {
        this.qual = qual;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.currentOrbitAngle = qual.getRandom().nextDouble() * Math.PI * 2.0;
    }

    @Override
    public boolean canUse() {
        return qual.isAlive();
    }

    @Override
    public void tick() {
        LivingEntity target = qual.getTarget();

        if (target != null && target.isAlive()) {
            qual.getLookControl().setLookAt(target, 35.0f, 35.0f);

            int phase = qual.getPhase();
            double desiredDistance = (phase == 2) ? 19.0 : 15.5;
            double desiredAltitude = target.getY() + ((phase == 2) ? 8.5 : 5.0);

            // Periodically switch strafing orbit direction
            if (--directionChangeTimer <= 0) {
                orbitDirection = (qual.getRandom().nextBoolean()) ? 1 : -1;
                directionChangeTimer = 100 + qual.getRandom().nextInt(80);
            }

            // Advance orbital circling angle
            currentOrbitAngle += orbitDirection * 0.025;

            double targetX = target.getX() + Math.cos(currentOrbitAngle) * desiredDistance;
            double targetZ = target.getZ() + Math.sin(currentOrbitAngle) * desiredDistance;
            double targetY = adjustAltitudeForTerrain(targetX, desiredAltitude, targetZ);

            qual.getMoveControl().setWantedPosition(targetX, targetY, targetZ, 1.0);
        } else {
            // Idle aerial hovering: descend to comfortable cruising height above ground (5 blocks)
            if (qual.tickCount % 40 == 0) {
                double wanderAngle = qual.getRandom().nextDouble() * Math.PI * 2.0;
                double wanderDist = 4.0 + qual.getRandom().nextDouble() * 4.0;
                double wx = qual.getX() + Math.cos(wanderAngle) * wanderDist;
                double wz = qual.getZ() + Math.sin(wanderAngle) * wanderDist;

                // Find ground level below Qual to ensure she hovers 5 blocks above terrain
                BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos(wx, qual.getY(), wz);
                while (mpos.getY() > qual.level().getMinBuildHeight() && qual.level().isEmptyBlock(mpos)) {
                    mpos.move(0, -1, 0);
                }
                double groundY = mpos.getY() + 1.0;
                double wy = groundY + 5.0;

                qual.getMoveControl().setWantedPosition(wx, wy, wz, 0.5);
            }
        }
    }

    /**
     * Prevents Qual from clipping into terrain or hovering too close to the ground.
     */
    private double adjustAltitudeForTerrain(double x, double preferredY, double z) {
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos(x, preferredY, z);

        // Find ground height below the position (bounded to 16 blocks to prevent deep iteration when high)
        int steps = 0;
        while (steps < 16 && mpos.getY() > qual.level().getMinBuildHeight() && qual.level().isEmptyBlock(mpos)) {
            mpos.move(0, -1, 0);
            steps++;
        }

        if (steps < 16) {
            double groundY = mpos.getY() + 1.0;
            return Math.max(preferredY, groundY + 4.5);
        }
        return preferredY;
    }
}
