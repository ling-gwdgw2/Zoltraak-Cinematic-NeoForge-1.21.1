package com.frierenflight.zoltraakcinematic.entity.boss.ai;

import com.frierenflight.zoltraakcinematic.client.renderer.ZoltraakMode;
import com.frierenflight.zoltraakcinematic.entity.ZoltraakCinematicBeamEntity;
import com.frierenflight.zoltraakcinematic.entity.boss.QualBossEntity;
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
 * 📜 QualCataclysmicGoal — Phase 4 Ultimate: Original Cataclysmic Zoltraak (極大破滅ゾルトラーク).
 *
 * Triggered when Qual is at critical health (HP < 20%):
 * 1. CHARGING STANCE (60 ticks / 3.0 seconds):
 *    - Qual halts motionlessly mid-air
 *    - Massive dark mana singularity collapses inward toward Qual
 *    - Resonant ancient humming & thunderous charge VFX
 *
 * 2. CATACLYSMIC DISCHARGE:
 *    - Fires a colossal 4-meter diameter beam (ZoltraakMode.LARGE)
 *    - 95 Base Armor-Piercing Damage sweeping across 80 blocks
 *    - 20-block terminal shockwave detonation
 *    - Demands either a full 360-degree Geodesic Dome or thick subterranean bedrock to survive!
 */
public class QualCataclysmicGoal extends Goal {
    private final QualBossEntity qual;
    private int cooldown = 140;
    private int chargeTimer = 0;
    private static final int CHARGE_DURATION = 60; // 3.0 seconds

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
            serverLevel.playSound(null, qual.blockPosition(), ModCinematicSounds.QUAL_CATACLYSMIC_CHARGE.get(), SoundSource.HOSTILE, 2.5f, 1.0f);
        }
    }

    @Override
    public void stop() {
        qual.setCataclysmicCharging(false);
    }

    @Override
    public boolean canContinueToUse() {
        return chargeTimer > 0 && qual.getTarget() != null && qual.getTarget().isAlive();
    }

    @Override
    public void tick() {
        LivingEntity target = qual.getTarget();
        if (target == null) return;

        // Lock onto target during ultimate charge
        qual.getLookControl().setLookAt(target, 20.0f, 20.0f);
        qual.setDeltaMovement(Vec3.ZERO); // Stationary floating during charge

        if (qual.level() instanceof ServerLevel serverLevel) {
            Vec3 anchor = ZoltraakCinematicBeamEntity.getStaffAnchorPos(qual);

            // Inward-converging singularity vortex particles
            for (int i = 0; i < 6; i++) {
                double ox = (qual.getRandom().nextDouble() - 0.5) * 3.5;
                double oy = (qual.getRandom().nextDouble() - 0.5) * 3.5;
                double oz = (qual.getRandom().nextDouble() - 0.5) * 3.5;
                serverLevel.sendParticles(ParticleTypes.PORTAL, anchor.x + ox, anchor.y + oy, anchor.z + oz, 1, -ox * 0.4, -oy * 0.4, -oz * 0.4, 0.2);
            }

            if (chargeTimer % 10 == 0) {
                serverLevel.sendParticles(ParticleTypes.WITCH, anchor.x, anchor.y, anchor.z, 15, 0.6, 0.6, 0.6, 0.05);
                serverLevel.playSound(null, qual.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 0.8f, 0.45f + (CHARGE_DURATION - chargeTimer) * 0.01f);
            }
        }

        chargeTimer--;

        if (chargeTimer <= 0) {
            fireCataclysmicBeam(target);
            qual.setCataclysmicCharging(false);
            cooldown = 180 + qual.getRandom().nextInt(40); // 9 - 11s cooldown
        }
    }

    private void fireCataclysmicBeam(LivingEntity target) {
        if (!(qual.level() instanceof ServerLevel serverLevel)) return;

        Vec3 anchor = ZoltraakCinematicBeamEntity.getStaffAnchorPos(qual);

        // Spawn Colossal Corrupted Beam (Mode.LARGE, ColorTheme 1 = Corrupted Void)
        ZoltraakCinematicBeamEntity beam = new ZoltraakCinematicBeamEntity(serverLevel, qual, 95.0f, 80.0f, 1);
        beam.setMode(ZoltraakMode.LARGE);
        beam.setPos(anchor.x, anchor.y, anchor.z);
        beam.setYRot(qual.getYRot());
        beam.setXRot(qual.getXRot());
        serverLevel.addFreshEntity(beam);

        // Apocalyptic Thunder & Shockwave SFX
        serverLevel.playSound(null, qual.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.5f, 0.65f);
        serverLevel.playSound(null, qual.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0f, 0.70f);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, anchor.x, anchor.y, anchor.z, 2, 0, 0, 0, 0);
    }
}
