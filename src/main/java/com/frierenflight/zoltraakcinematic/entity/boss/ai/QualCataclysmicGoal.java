package com.frierenflight.zoltraakcinematic.entity.boss.ai;

import com.frierenflight.zoltraakcinematic.entity.GargantuaEntity;
import com.frierenflight.zoltraakcinematic.entity.boss.QualBossEntity;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 📜 QualCataclysmicGoal — Phase 4 Ultimate: Cosmic Singularity (มหาเวทเอกภาวะหลุมดำ - Gargantua Black Hole).
 *
 * Triggered when Qual is at critical health (HP < 20%):
 * 1. CHARGING STANCE (70 ticks / 3.5 seconds):
 *    - Qual halts motionlessly mid-air
 *    - Massive dark mana logarithmic singularity vortex collapses inward towards Qual
 *    - Demonic resonance & spatial tearing audio
 *
 * 2. EVENT HORIZON SUMMON (400 ticks / 20.0 seconds active lifetime):
 *    - Spawns a colossal rotating Kerr black hole (GargantuaEntity) midway towards the player
 *    - Relativistic accretion disk shears anything in range, inward gravitational pull across 100 blocks
 *    - Terminates with an apocalyptic Supernova implosion detonation
 *    - Cooldown: 200 seconds (4000 ticks) as explicitly configured
 */
public class QualCataclysmicGoal extends Goal {
    public static final int COOLDOWN_TICKS = 4000; // 200 seconds cooldown
    private static final int CHARGE_DURATION = 70;  // 3.5 seconds
    private static final int BLACK_HOLE_LIFETIME = 400; // 20.0 seconds

    private final QualBossEntity qual;
    private int cooldown = 200; // Initial delay upon entering Phase 4 (10s buffer)
    private int chargeTimer = 0;

    public QualCataclysmicGoal(QualBossEntity qual) {
        this.qual = qual;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = qual.getTarget();
        if (target == null || !target.isAlive() || qual.getPhase() != 4) {
            return false;
        }

        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        return !qual.isCasting();
    }

    @Override
    public void start() {
        this.chargeTimer = CHARGE_DURATION;
        qual.setCataclysmicCharging(true);
        if (qual.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, qual.blockPosition(), ModCinematicSounds.QUAL_CATACLYSMIC_CHARGE.get(), SoundSource.HOSTILE, 3.0f, 0.95f);
        }
    }

    @Override
    public void stop() {
        qual.setCataclysmicCharging(false);
    }

    @Override
    public boolean canContinueToUse() {
        if (chargeTimer <= 0) return false;
        LivingEntity target = qual.getTarget();
        if (target == null || !target.isAlive()) {
            Player nextPlayer = qual.level().getNearestPlayer(qual, 48.0);
            if (nextPlayer != null && nextPlayer.isAlive() && !nextPlayer.isCreative() && !nextPlayer.isSpectator()) {
                qual.setTarget(nextPlayer);
                qual.faceTargetDirectly(nextPlayer);
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = qual.getTarget();
        if (target == null) return;

        // Lock onto target during ultimate charge
        qual.getLookControl().setLookAt(target, 20.0f, 20.0f);
        qual.setDeltaMovement(Vec3.ZERO); // Stationary floating during charge

        if (qual.level() instanceof ServerLevel serverLevel) {
            if (chargeTimer % 15 == 0) {
                serverLevel.playSound(null, qual.blockPosition(), ModCinematicSounds.SINGULARITY_ACTIVE.get(), SoundSource.HOSTILE, 1.8f, 1.15f);
            }
        }

        chargeTimer--;

        if (chargeTimer <= 0) {
            summonGargantuaSingularity(target);
            qual.setCataclysmicCharging(false);
            cooldown = COOLDOWN_TICKS; // 200 seconds cooldown (4000 ticks)
        }
    }

    private void summonGargantuaSingularity(LivingEntity target) {
        if (!(qual.level() instanceof ServerLevel serverLevel)) return;

        Vec3 bossPos = qual.position().add(0, 1.5, 0);
        Vec3 targetPos = target.position().add(0, 1.0, 0);

        // Spawn singularity elevated midway towards target (65% towards target, 4.5m elevated)
        Vec3 spawnPos = bossPos.lerp(targetPos, 0.65).add(0, 4.5, 0);

        Vec3 spinAxis = new Vec3(0.22d, 0.96d, 0.15d).normalize();
        float spellPower = 600.0f; // Boss cataclysmic tier spell power

        GargantuaEntity blackHole = new GargantuaEntity(serverLevel, qual, spawnPos, spinAxis, spellPower, BLACK_HOLE_LIFETIME);
        serverLevel.addFreshEntity(blackHole);

        // Resonant Spacetime Tear Sound
        serverLevel.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, ModCinematicSounds.SINGULARITY_CHARGE.get(), SoundSource.HOSTILE, 4.5f, 0.90f);
    }
}
