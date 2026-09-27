package com.frierenvoid;

import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class ZoltraakDamage {
   public static boolean apply(LivingEntity target, float amount, DamageSource source, boolean rapid) {
      if (amount <= 0.0F) {
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
}
