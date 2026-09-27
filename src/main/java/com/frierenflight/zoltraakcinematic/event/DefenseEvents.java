package com.frierenflight.zoltraakcinematic.event;

import com.frierenflight.zoltraakcinematic.entity.DefenseBarrierEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class DefenseEvents {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, DefenseEvents::onLivingIncomingDamage);
    }

    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!event.getEntity().level().isClientSide && event.getAmount() > 0.0F) {
            DamageSource source = event.getSource();
            Entity direct = source.getDirectEntity();
            Entity attacker = source.getEntity();

            if (direct != null || attacker != null || source.getSourcePosition() != null) {
                Vec3 from = source.getSourcePosition();
                if (from == null) {
                    if (direct != null) {
                        from = direct.position();
                    } else if (attacker != null) {
                        from = attacker.getEyePosition();
                    }
                }

                if (from != null) {
                    Vec3 eyeTo = event.getEntity().getEyePosition();
                    Vec3 centerTo = event.getEntity().getBoundingBox().getCenter();
                    Vec3 origin = from;
                    List<DefenseBarrierEntity> barriers = event.getEntity().level()
                            .getEntitiesOfClass(DefenseBarrierEntity.class, new AABB(from, eyeTo).inflate(4.0));
                    barriers.sort(Comparator.comparingDouble(x -> x.position().distanceToSqr(origin)));
                    float amount = event.getAmount();

                    for (DefenseBarrierEntity barrier : barriers) {
                        if (!barrier.friendly(attacker != null ? attacker : direct)) {
                            Optional<Vec3> contact = barrier.intersection(from, eyeTo);
                            if (contact.isEmpty()) {
                                contact = barrier.intersection(from, centerTo);
                            }

                            if (contact.isPresent()) {
                                amount -= barrier.absorb(amount, contact.get());
                            }

                            if (amount <= 0.0F) {
                                break;
                            }
                        }
                    }

                    event.setAmount(Math.max(0.0F, amount));
                    if (amount <= 0.0F) {
                        event.setCanceled(true);
                    }
                }
            }
        }
    }
}
