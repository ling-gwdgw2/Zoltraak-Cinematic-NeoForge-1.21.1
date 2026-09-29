package com.frierenflight.zoltraakcinematic.entity.boss.ai;

import com.frierenflight.zoltraakcinematic.entity.DefenseBarrierEntity;
import com.frierenflight.zoltraakcinematic.entity.boss.QualBossEntity;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSpells;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSounds;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 📜 QualAntiBarrierGoal — Anti-Barrier Intelligence & Tactical Adaptation.
 *
 * Demonstrates Qual's analytical demonic mastery:
 * 1. DIRECTIONAL SHIELD DETECTED:
 *    - Detects when the player raises a front-facing shield
 *    - Calculates the shielded angle: if attacking the front, Qual laughs and triggers "Shadow Blink"
 *    - Teleports 8–10 blocks behind the player's back to bypass the barrier completely!
 *    - Instantly casts Corrupted Zoltraak into the exposed back
 *
 * 2. 360-DEGREE DOME DETECTED:
 *    - Recognizes that there are no angular blind spots
 *    - Switches to "Focused Piercing Pressure" — casts heavy Corrupted Barrage and charged beams
 *      to rapidly exhaust the dome's structural absorption capacity!
 */
public class QualAntiBarrierGoal extends Goal {
    private final QualBossEntity qual;
    private int blinkCooldown = 60;
    private int pressureTimer = 40;

    public QualAntiBarrierGoal(QualBossEntity qual) {
        this.qual = qual;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = qual.getTarget();
        if (target == null || !target.isAlive() || qual.getPhase() < 2) {
            return false;
        }

        DefenseBarrierEntity barrier = DefenseBarrierEntity.find(target);
        return barrier != null && barrier.active();
    }

    @Override
    public void tick() {
        LivingEntity target = qual.getTarget();
        if (target == null || !target.isAlive()) return;

        DefenseBarrierEntity barrier = DefenseBarrierEntity.find(target);
        if (barrier == null || !barrier.active()) return;

        qual.getLookControl().setLookAt(target, 40.0f, 40.0f);

        if (barrier.mode() == DefenseBarrierEntity.MODE_DIRECTIONAL) {
            handleDirectionalShieldTactics(target, barrier);
        } else if (barrier.mode() == DefenseBarrierEntity.MODE_DOME) {
            handleDomeTactics(target, barrier);
        }
    }

    /**
     * Executes Shadow Blink to flank or get behind a directional shield.
     */
    private void handleDirectionalShieldTactics(LivingEntity target, DefenseBarrierEntity barrier) {
        if (blinkCooldown > 0) {
            blinkCooldown--;
            return;
        }

        Vec3 barrierNorm = barrier.normal();
        Vec3 qualToTarget = target.position().subtract(qual.position()).normalize();

        // Check if Qual is facing into the shielded front arc (dot product < 0)
        boolean facingShieldFront = qualToTarget.dot(barrierNorm) < 0.2;

        if (facingShieldFront && !qual.isCasting()) {
            executeShadowBlink(target);
            blinkCooldown = 75 + qual.getRandom().nextInt(35); // 3.75s - 5.5s cooldown
        }
    }

    /**
     * Teleports behind the player with dark smoke VFX and fires immediately.
     */
    private void executeShadowBlink(LivingEntity target) {
        if (!(qual.level() instanceof ServerLevel serverLevel)) return;

        Vec3 origin = qual.position();

        // Calculate position directly behind player's look vector
        Vec3 targetLook = target.getLookAngle().normalize();
        double blinkDistance = 7.5 + qual.getRandom().nextDouble() * 2.5;
        Vec3 destination = target.position()
                .subtract(targetLook.scale(blinkDistance))
                .add(0, 2.2 + qual.getRandom().nextDouble() * 1.5, 0);

        // Ensure Qual does not teleport inside solid blocks (e.g. wall behind player)
        net.minecraft.core.BlockPos destBlock = net.minecraft.core.BlockPos.containing(destination);
        if (!serverLevel.getBlockState(destBlock).isAir() || !serverLevel.getBlockState(destBlock.above()).isAir()) {
            destination = target.position().add(0, 4.5, 0);
        }

        // Origin Disintegration VFX & SFX
        serverLevel.sendParticles(ParticleTypes.SQUID_INK, origin.x, origin.y + 1.6, origin.z, 25, 0.6, 1.2, 0.6, 0.08);
        serverLevel.sendParticles(ParticleTypes.WITCH, origin.x, origin.y + 1.6, origin.z, 20, 0.5, 1.0, 0.5, 0.05);
        serverLevel.playSound(null, qual.blockPosition(), ModCinematicSounds.QUAL_TELEPORT.get(), SoundSource.HOSTILE, 1.5f, 0.95f);

        // Teleport to flank/rear
        qual.teleportTo(destination.x, destination.y, destination.z);

        // Destination Materialization VFX
        serverLevel.sendParticles(ParticleTypes.PORTAL, destination.x, destination.y + 1.6, destination.z, 30, 0.8, 1.2, 0.8, 0.1);
        serverLevel.sendParticles(ParticleTypes.WITCH, destination.x, destination.y + 1.6, destination.z, 15, 0.4, 0.8, 0.4, 0.04);
        serverLevel.playSound(null, qual.blockPosition(), ModCinematicSounds.QUAL_TELEPORT.get(), SoundSource.HOSTILE, 1.5f, 1.05f);

        // Immediately face player's exposed back
        qual.getLookControl().setLookAt(target, 180.0f, 180.0f);

        // Immediate punitive ambush cast
        if (qual.getRandom().nextFloat() < 0.7f) {
            qual.initiateCastSpell(ModCinematicSpells.CORRUPTED_ZOLTRAAK.get(), 8);
        } else {
            qual.initiateCastSpell(ModCinematicSpells.CORRUPTED_BARRAGE.get(), 1);
        }
    }

    /**
     * Executes continuous heavy pressure against 360-degree Geodesic Domes.
     */
    private void handleDomeTactics(LivingEntity target, DefenseBarrierEntity barrier) {
        if (pressureTimer > 0) {
            pressureTimer--;
            return;
        }

        if (!qual.isCasting()) {
            // Sustained barrage or heavy beam to overwhelm capacity
            if (qual.getRandom().nextFloat() < 0.65f) {
                qual.initiateCastSpell(ModCinematicSpells.CORRUPTED_BARRAGE.get(), 1);
                pressureTimer = 80 + qual.getRandom().nextInt(20);
            } else {
                qual.initiateCastSpell(ModCinematicSpells.CORRUPTED_ZOLTRAAK.get(), 9);
                pressureTimer = 45 + qual.getRandom().nextInt(15);
            }
        }
    }
}
