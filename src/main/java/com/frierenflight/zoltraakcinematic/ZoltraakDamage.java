package com.frierenflight.zoltraakcinematic;

import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Clean damage application utility modeled after frierenvoid prototype.
 * Safely handles rapid invulnerability frame bypassing for continuous barrage spells
 * and respects school resistance attributes (Magic Resist) with soft-cap formulas.
 */
public final class ZoltraakDamage {

    public static boolean apply(LivingEntity target, float amount, DamageSource source, boolean rapid) {
        return apply(target, amount, source, null, rapid);
    }

    public static boolean apply(LivingEntity target, float amount, DamageSource source, SchoolType school, boolean rapid) {
        if (amount <= 0.0F || target == null || target.isDeadOrDying()) {
            return false;
        }

        // Apply school and spell magic resistance if school is provided
        if (school != null) {
            float resistMult = DamageSources.getResist(target, school);
            amount *= Math.max(0.0f, resistMult);
            if (amount <= 0.0F) {
                return false;
            }
        }

        int saved = target.invulnerableTime;

        try {
            if (rapid) {
                target.invulnerableTime = 0;
            }

            return DamageSources.applyDamage(target, amount, source);
        } finally {
            if (rapid) {
                // Clear invulnerableTime so rapid continuous hits do not lock out teammate attacks
                target.invulnerableTime = 0;
            }
        }
    }

    private ZoltraakDamage() {}
}
