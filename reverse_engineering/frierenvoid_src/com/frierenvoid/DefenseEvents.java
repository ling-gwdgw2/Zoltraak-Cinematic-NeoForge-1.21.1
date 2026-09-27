package com.frierenvoid;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = "frierenvoid")
public final class DefenseEvents {
   @SubscribeEvent(priority = EventPriority.LOW)
   public static void hurt(LivingIncomingDamageEvent e) {
      if (!e.getEntity().level().isClientSide && !(e.getAmount() <= 0.0F)) {
         DamageSource source = e.getSource();
         Entity direct = source.getDirectEntity();
         Entity attacker = source.getEntity();
         if (direct instanceof Projectile || attacker instanceof LivingEntity) {
            if (!(direct instanceof ZoltraakEntity) && !(direct instanceof SingularityEntity)) {
               Vec3 from = source.getSourcePosition();
               if (from != null) {
                  if (direct instanceof Projectile p && attacker != null) {
                     from = attacker.getEyePosition();
                  }

                  Vec3 to = e.getEntity().getEyePosition();
                  Vec3 origin = from;
                  List<DefenseEntity> barriers = e.getEntity().level().getEntitiesOfClass(DefenseEntity.class, new AABB(from, to).inflate(3.0));
                  barriers.sort(Comparator.comparingDouble(x -> x.position().distanceToSqr(origin)));
                  float amount = e.getAmount();

                  for (DefenseEntity barrier : barriers) {
                     if (!barrier.friendly(attacker)) {
                        Optional<Vec3> contact = barrier.intersection(from, to);
                        if (contact.isPresent()) {
                           amount -= barrier.absorb(amount, contact.get());
                        }

                        if (amount <= 0.0F) {
                           break;
                        }
                     }
                  }

                  e.setAmount(Math.max(0.0F, amount));
                  if (amount <= 0.0F) {
                     e.setCanceled(true);
                  }
               }
            }
         }
      }
   }
}
