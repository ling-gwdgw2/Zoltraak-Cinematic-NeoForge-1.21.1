package com.frierenflight.zoltraakcinematic;

import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Clean damage application utility modeled after frierenvoid prototype.
 * Safely handles rapid invulnerability frame bypassing for continuous barrage spells.
 */
public final class ZoltraakDamage {

    public static boolean apply(LivingEntity target, float amount, DamageSource source, boolean rapid) {
        if (amount <= 0.0F || target == null || target.isDeadOrDying()) {
            return false;
        }

        int saved = target.invulnerableTime;

        try {
            if (rapid) {
                target.invulnerableTime = 0;
            }

            return DamageSources.applyDamage(target, amount, source);
        } finally {
            if (rapid) {
                target.invulnerableTime = Math.max(saved, target.invulnerableTime);
            }
        }
    }

    private ZoltraakDamage() {}
}
