package com.frierenflight.zoltraakcinematic.entity.boss.ai;

import com.frierenflight.zoltraakcinematic.entity.boss.QualBossEntity;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSpells;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * 📜 QualCombatGoal — 4-Phase Combat Progression Spellcasting AI.
 *
 * Implements Qual's canonical attack cadence:
 * - Phase 1 (Probing Stance): Precise single Corrupted Zoltraak beams
 * - Phase 2 (Demonic Barrage): 24-circle Corrupted Phalanx Barrage
 * - Phase 3 (Tactical Adaptation): High-tempo alternating barrages and beams
 * - Phase 4 (Overdrive): Relentless pressure between Cataclysmic cooldowns
 */
public class QualCombatGoal extends Goal {
    private final QualBossEntity qual;
    private int attackTimer = 35;

    public QualCombatGoal(QualBossEntity qual) {
        this.qual = qual;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = qual.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void tick() {
        LivingEntity target = qual.getTarget();
        if (target == null || !target.isAlive()) return;

        qual.getLookControl().setLookAt(target, 35.0f, 35.0f);

        if (!qual.isCasting()) {
            if (attackTimer > 0) {
                attackTimer--;
            } else {
                executeAttack(target);
            }
        }
    }

    private void executeAttack(LivingEntity target) {
        int phase = qual.getPhase();

        switch (phase) {
            case 1 -> {
                // Phase 1: Probing Stance — Single Corrupted Zoltraak
                qual.initiateCastSpell(ModCinematicSpells.CORRUPTED_ZOLTRAAK.get(), 6);
                attackTimer = 65 + qual.getRandom().nextInt(25); // ~3.5 - 4.5s
            }
            case 2 -> {
                // Phase 2: Demonic Barrage Matrix
                if (qual.getRandom().nextFloat() < 0.65f) {
                    qual.initiateCastSpell(ModCinematicSpells.CORRUPTED_BARRAGE.get(), 1);
                    attackTimer = 100 + qual.getRandom().nextInt(30); // ~5 - 6.5s
                } else {
                    qual.initiateCastSpell(ModCinematicSpells.CORRUPTED_ZOLTRAAK.get(), 7);
                    attackTimer = 50 + qual.getRandom().nextInt(20); // ~2.5 - 3.5s
                }
            }
            case 3 -> {
                // Phase 3: Tactical Adaptation
                if (qual.getRandom().nextFloat() < 0.55f) {
                    qual.initiateCastSpell(ModCinematicSpells.CORRUPTED_BARRAGE.get(), 1);
                    attackTimer = 85 + qual.getRandom().nextInt(25);
                } else {
                    qual.initiateCastSpell(ModCinematicSpells.CORRUPTED_ZOLTRAAK.get(), 9);
                    attackTimer = 40 + qual.getRandom().nextInt(15);
                }
            }
            case 4 -> {
                // Phase 4: Overdrive Sustained Fire
                if (qual.getRandom().nextFloat() < 0.5f) {
                    qual.initiateCastSpell(ModCinematicSpells.CORRUPTED_BARRAGE.get(), 1);
                    attackTimer = 70 + qual.getRandom().nextInt(20);
                } else {
                    qual.initiateCastSpell(ModCinematicSpells.CORRUPTED_ZOLTRAAK.get(), 10);
                    attackTimer = 35 + qual.getRandom().nextInt(15);
                }
            }
        }
    }
}
