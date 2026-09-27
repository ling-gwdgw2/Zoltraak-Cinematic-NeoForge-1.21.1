package com.frierenvoid;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SingularityEntity extends Entity {
   public static final int COLLAPSE = 170;
   public static final int RELEASE = 200;
   public static final int DURATION = 240;
   private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(SingularityEntity.class, EntityDataSerializers.LONG);
   private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(SingularityEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(SingularityEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<CompoundTag> FLIGHT = SynchedEntityData.defineId(SingularityEntity.class, EntityDataSerializers.COMPOUND_TAG);
   private UUID owner;
   private float damage;
   private int eatenBlocks;

   public int eatenBlocks() {
      return this.eatenBlocks;
   }

   public void recordEatenBlock() {
      this.eatenBlocks++;
   }

   public boolean canEatTerrain() {
      return this.damage > 0.0F
         && this.rising()
         && (Boolean)VoidConfig.TERRAIN.get()
         && this.eatenBlocks < (Integer)VoidConfig.MAX_BLOCKS.get()
         && this.caster() != null
         && this.age(0.0F) >= 54.0F
         && this.age(0.0F) < 192.0F;
   }

   public SingularityEntity(EntityType<? extends SingularityEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
   }

   public SingularityEntity(ServerLevel level, Vec3 center, LivingEntity caster, float damage, float radius) {
      this((EntityType<? extends SingularityEntity>)FrierenVoid.ENTITY.get(), level);
      this.setPos(center);
      this.owner = caster.getUUID();
      this.damage = Math.max(0.0F, damage);
      this.entityData.set(START, level.getGameTime());
      this.entityData.set(RADIUS, radius);
      this.entityData.set(SEED, level.random.nextInt(100000));
   }

   public static SingularityEntity fromHand(ServerLevel level, LivingEntity caster, float damage, float radius) {
      Vec3 palm = VoidChoreography.palm(caster);
      Vec3 facing = VoidChoreography.forward(caster);
      SingularityEntity e = new SingularityEntity(level, palm, caster, damage, radius);
      e.setFlight(palm, facing, caster.getId());
      return e;
   }

   private void setFlight(Vec3 origin, Vec3 facing, int casterId) {
      CompoundTag tag = new CompoundTag();
      tag.putBoolean("Rising", true);
      tag.putInt("CasterId", casterId);
      tag.putDouble("X", origin.x);
      tag.putDouble("Y", origin.y);
      tag.putDouble("Z", origin.z);
      tag.putDouble("DX", facing.x);
      tag.putDouble("DZ", facing.z);
      this.entityData.set(FLIGHT, tag);
   }

   public boolean rising() {
      return ((CompoundTag)this.entityData.get(FLIGHT)).getBoolean("Rising");
   }

   public int casterId() {
      return ((CompoundTag)this.entityData.get(FLIGHT)).getInt("CasterId");
   }

   public Vec3 visualPosition(float partial) {
      if (!this.rising()) {
         return this.position();
      }

      float age = this.age(partial);
      CompoundTag tag = (CompoundTag)this.entityData.get(FLIGHT);
      return age < 24.0F && this.level().getEntity(this.casterId()) instanceof LivingEntity c
         ? VoidChoreography.palm(c)
         : VoidChoreography.flight(
            new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")), new Vec3(tag.getDouble("DX"), 0.0, tag.getDouble("DZ")), age
         );
   }

   public float activeRadius() {
      return this.rising() ? this.radius() * (0.18F + 0.82F * VoidChoreography.reach(this.age(0.0F))) : this.radius();
   }

   public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
      if (!this.rising()) {
         super.lerpTo(x, y, z, yaw, pitch, steps);
      }
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(START, 0L);
      builder.define(RADIUS, 16.0F);
      builder.define(SEED, 0);
      builder.define(FLIGHT, new CompoundTag());
   }

   public float age(float partial) {
      return Mth.clamp((float)(this.level().getGameTime() - (Long)this.entityData.get(START)) + partial, 0.0F, 240.0F);
   }

   public float radius() {
      return (Float)this.entityData.get(RADIUS);
   }

   public int seed() {
      return (Integer)this.entityData.get(SEED);
   }

   public static boolean canStart(ServerLevel level, UUID owner) {
      int count = 0;

      for (Entity e : level.getAllEntities()) {
         if (e instanceof SingularityEntity s && !s.isRemoved()) {
            if (owner.equals(s.owner) || ++count >= (Integer)VoidConfig.MAX_ACTIVE.get()) {
               return false;
            }
         }
      }

      return true;
   }

   public LivingEntity caster() {
      if (this.owner != null && this.level() instanceof ServerLevel s) {
         return s.getEntity(this.owner) instanceof LivingEntity l && l.isAlive() ? l : null;
      } else {
         return null;
      }
   }

   public boolean canAffect(LivingEntity target) {
      LivingEntity caster = this.caster();
      if (caster != null && target != caster && target.isAlive() && !target.isSpectator() && !(target instanceof ArmorStand)) {
         if (!(
            target instanceof Player p
               && (
                  p.isCreative()
                     || !(Boolean)VoidConfig.PVP.get()
                     || !this.level().getServer().isPvpAllowed()
                     || caster instanceof Player cp && !cp.canHarmPlayer(p)
               )
         )) {
            if (target instanceof OwnableEntity pet && pet.getOwnerUUID() != null) {
               if (this.owner.equals(pet.getOwnerUUID())) {
                  return false;
               }

               Entity petOwner = pet.getOwner();
               if (petOwner != null && (petOwner.isAlliedTo(caster) || caster.isAlliedTo(petOwner))) {
                  return false;
               }
            }

            return !target.isAlliedTo(caster)
                  && !caster.isAlliedTo(target)
                  && !DamageSources.isFriendlyFireBetween(caster, target)
                  && !DamageSources.isFriendlyFireBetween(target, caster)
               ? (Boolean)VoidConfig.PASSIVE.get() || target instanceof Enemy || target instanceof Player
               : false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private List<LivingEntity> targets() {
      float reach = this.activeRadius();
      return this.level()
         .getEntitiesOfClass(
            LivingEntity.class, this.getBoundingBox().inflate(reach), e -> e.distanceToSqr(this.position()) <= reach * reach && this.canAffect(e)
         )
         .stream()
         .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(this.position())))
         .limit(((Integer)VoidConfig.MAX_TARGETS.get()).intValue())
         .toList();
   }

   public void tick() {
      super.tick();
      if (this.rising()) {
         if (!this.level().isClientSide && this.age(0.0F) <= 24.0F && this.caster() != null) {
            this.setFlight(VoidChoreography.palm(this.caster()), VoidChoreography.forward(this.caster()), this.caster().getId());
         }

         this.setPos(this.visualPosition(0.0F));
      }

      if (this.level() instanceof ServerLevel) {
         LivingEntity caster = this.caster();
         int age = (int)this.age(0.0F);
         if (caster != null && age < 240) {
            if (age % 4 == 0) {
               TerrainAbsorption.pulse(this);
            }

            if (!(this.damage <= 0.0F) && age >= (this.rising() ? 54 : 30) && age <= 200 && age % 2 == 0) {
               for (LivingEntity target : this.targets()) {
                  Vec3 delta = this.position().subtract(target.getBoundingBox().getCenter());
                  double distance = delta.length();
                  double resist = 1.0 - Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
                  if (age < 200 && distance > 0.2 && resist > 0.0) {
                     Vec3 radial = delta.normalize();
                     Vec3 tangent = new Vec3(-radial.z, 0.0, radial.x);
                     double force = (0.12 + 0.22 * (1.0 - Math.min(1.0, distance / this.activeRadius()))) * (age >= 170 ? 1.6 : 1.0);
                     Vec3 movement = target.getDeltaMovement()
                        .scale(0.8)
                        .add(radial.scale(force * resist * (Double)VoidConfig.PULL.get()))
                        .add(tangent.scale(0.1 * resist * (Double)VoidConfig.PULL.get()));
                     if (movement.lengthSqr() > 1.44) {
                        movement = movement.normalize().scale(1.2);
                     }

                     target.setDeltaMovement(movement);
                     target.hurtMarked = true;
                     target.fallDistance = 0.0F;
                  }

                  if (age < 200 && age % 20 == 0 && distance < 4.5) {
                     DamageSources.applyDamage(target, this.damage, ((AbstractSpell)FrierenVoid.SINGULARITY.get()).getDamageSource(this, caster));
                  }

                  if (age == 200) {
                     boolean hurt = DamageSources.applyDamage(
                        target,
                        this.damage * 2.5F * (float)(1.0 - 0.65 * distance / this.radius()),
                        ((AbstractSpell)FrierenVoid.SINGULARITY.get()).getDamageSource(this, caster)
                     );
                     if (hurt && distance > 0.1) {
                        target.knockback(0.8 * resist, delta.x, delta.z);
                     }
                  }
               }

               if (age < 200 && (Boolean)VoidConfig.PROJECTILES.get()) {
                  int affected = 0;
                  float reach = this.activeRadius();

                  for (Projectile p : this.level().getEntitiesOfClass(Projectile.class, this.getBoundingBox().inflate(reach))) {
                     if (p.getOwner() instanceof LivingEntity shooter && this.canAffect(shooter) && !(p.distanceToSqr(this) > reach * reach)) {
                        if (++affected > (Integer)VoidConfig.MAX_TARGETS.get()) {
                           break;
                        }

                        Vec3 delta = this.position().subtract(p.position());
                        if (delta.lengthSqr() < 4.0) {
                           p.discard();
                        } else {
                           p.setDeltaMovement(p.getDeltaMovement().scale(0.8).add(delta.normalize().scale(0.45)));
                           p.hurtMarked = true;
                        }
                     }
                  }
               }
            }
         } else {
            this.discard();
         }
      }
   }

   public boolean shouldRenderAtSqrDistance(double d) {
      return d < 36864.0;
   }

   public AABB getBoundingBoxForCulling() {
      return this.getBoundingBox().inflate(this.radius() * 4.0F);
   }

   public boolean isPickable() {
      return false;
   }

   public boolean isPushable() {
      return false;
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      if (this.owner != null) {
         tag.putUUID("Owner", this.owner);
      }

      tag.putFloat("Damage", this.damage);
      tag.putLong("Start", (Long)this.entityData.get(START));
      tag.putFloat("Radius", this.radius());
      tag.putInt("Seed", this.seed());
      tag.put("Flight", ((CompoundTag)this.entityData.get(FLIGHT)).copy());
      tag.putInt("EatenBlocks", this.eatenBlocks);
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
      this.damage = Math.max(0.0F, tag.getFloat("Damage"));
      this.entityData.set(START, tag.getLong("Start"));
      this.entityData.set(RADIUS, Mth.clamp(tag.getFloat("Radius"), 6.0F, 32.0F));
      this.entityData.set(SEED, tag.getInt("Seed"));
      this.entityData.set(FLIGHT, tag.getCompound("Flight").copy());
      this.eatenBlocks = Math.max(0, tag.getInt("EatenBlocks"));
   }
}
