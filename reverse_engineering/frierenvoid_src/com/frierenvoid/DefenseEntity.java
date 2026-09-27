package com.frierenvoid;

import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class DefenseEntity extends Entity {
   private static final EntityDataAccessor<CompoundTag> STATE = SynchedEntityData.defineId(DefenseEntity.class, EntityDataSerializers.COMPOUND_TAG);
   public static final double CELL = 0.62;
   public static final double RADIUS = 2.8;
   private UUID owner;
   private DefenseEntity.VisualPose lastVisualPose;

   public DefenseEntity.VisualPose visualPose(float partial) {
      LivingEntity caster = this.owner();
      if (this.active() && caster != null) {
         Vec3 n = Vec3.directionFromRotation(caster.getViewXRot(partial), Mth.rotLerp(partial, caster.yRotO, caster.getYRot())).normalize();
         Vec3 eye = caster.getEyePosition(partial);
         Vec3 u = n.cross(new Vec3(0.0, 1.0, 0.0));
         u = u.lengthSqr() < 0.001 ? new Vec3(1.0, 0.0, 0.0) : u.normalize();
         this.lastVisualPose = new DefenseEntity.VisualPose(this.clippedCenter(caster, eye, n), n, u, u.cross(n).normalize());
      }

      return this.lastVisualPose != null ? this.lastVisualPose : new DefenseEntity.VisualPose(this.position(), this.normal(), this.right(), this.up());
   }

   private Vec3 clippedCenter(LivingEntity caster, Vec3 eye, Vec3 n) {
      Vec3 wanted = eye.add(n.scale(1.7));
      BlockHitResult clip = this.level().clip(new ClipContext(eye, wanted, Block.COLLIDER, Fluid.NONE, caster));
      return clip.getType() == Type.MISS ? wanted : clip.getLocation().subtract(n.scale(0.08));
   }

   public Vec3 visualHit(DefenseEntity.VisualPose pose) {
      CompoundTag t = (CompoundTag)this.entityData.get(STATE);
      Vec3 old = this.hitPoint().subtract(this.position());
      double x = t.contains("HitU") ? t.getDouble("HitU") : old.dot(this.right());
      double y = t.contains("HitV") ? t.getDouble("HitV") : old.dot(this.up());
      return pose.right().scale(x).add(pose.up().scale(y));
   }

   public DefenseEntity(EntityType<? extends DefenseEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
   }

   public static DefenseEntity create(ServerLevel level, LivingEntity caster, float capacity) {
      DefenseEntity e = new DefenseEntity((EntityType<? extends DefenseEntity>)FrierenVoid.DEFENSE_ENTITY.get(), level);
      e.owner = caster.getUUID();
      CompoundTag t = new CompoundTag();
      t.putInt("Owner", caster.getId());
      t.putFloat("Capacity", capacity);
      t.putFloat("Maximum", capacity);
      t.putLong("Start", level.getGameTime());
      e.entityData.set(STATE, t);
      e.follow(caster);
      level.playSound(null, e.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);
      return e;
   }

   public static DefenseEntity find(LivingEntity owner) {
      for (DefenseEntity e : owner.level().getEntitiesOfClass(DefenseEntity.class, owner.getBoundingBox().inflate(8.0))) {
         if (e.active() && owner.getUUID().equals(e.owner)) {
            return e;
         }
      }

      return null;
   }

   public LivingEntity owner() {
      if (!(this.level() instanceof ServerLevel s)) {
         return this.level().getEntity(((CompoundTag)this.entityData.get(STATE)).getInt("Owner")) instanceof LivingEntity l ? l : null;
      } else {
         return this.owner != null && s.getEntity(this.owner) instanceof LivingEntity l ? l : null;
      }
   }

   public float capacity() {
      return ((CompoundTag)this.entityData.get(STATE)).getFloat("Capacity");
   }

   public float maximum() {
      return ((CompoundTag)this.entityData.get(STATE)).getFloat("Maximum");
   }

   public float age(float partial) {
      return (float)(this.level().getGameTime() - ((CompoundTag)this.entityData.get(STATE)).getLong("Start")) + partial;
   }

   public boolean active() {
      return !this.isRemoved() && this.capacity() > 0.0F && this.age(0.0F) < 80.0F && this.owner() != null && this.owner().isAlive();
   }

   public Vec3 normal() {
      CompoundTag t = (CompoundTag)this.entityData.get(STATE);
      return new Vec3(t.getDouble("NX"), t.getDouble("NY"), t.getDouble("NZ")).normalize();
   }

   public Vec3 right() {
      Vec3 u = this.normal().cross(new Vec3(0.0, 1.0, 0.0));
      return u.lengthSqr() < 0.001 ? new Vec3(1.0, 0.0, 0.0) : u.normalize();
   }

   public Vec3 up() {
      return this.right().cross(this.normal()).normalize();
   }

   public float hitAge(float partial) {
      CompoundTag t = (CompoundTag)this.entityData.get(STATE);
      return t.contains("Hit") ? (float)(this.level().getGameTime() - t.getLong("Hit")) + partial : 100.0F;
   }

   public Vec3 hitPoint() {
      CompoundTag t = (CompoundTag)this.entityData.get(STATE);
      return new Vec3(t.getDouble("HX"), t.getDouble("HY"), t.getDouble("HZ"));
   }

   public float fade(float partial) {
      CompoundTag t = (CompoundTag)this.entityData.get(STATE);
      return t.contains("Break")
         ? Math.max(0.0F, 1.0F - ((float)(this.level().getGameTime() - t.getLong("Break")) + partial) / 8.0F)
         : Math.max(0.0F, Math.min(1.0F, (86.0F - this.age(partial)) / 6.0F));
   }

   public static List<Vec3> cells() {
      List<Vec3> cells = new ArrayList<>();

      for (int q = -2; q <= 2; q++) {
         for (int r = -2; r <= 2; r++) {
            if (Math.abs(q + r) <= 2) {
               cells.add(new Vec3(Math.sqrt(3.0) * 0.62 * (q + r * 0.5), 0.9299999999999999 * r, 0.0));
            }
         }
      }

      return cells;
   }

   public static boolean inside(double x, double y) {
      for (Vec3 c : cells()) {
         double dx = Math.abs(x - c.x);
         double dy = Math.abs(y - c.y);
         if (dx <= Math.sqrt(3.0) * 0.62 * 0.5 && dy <= 0.62 && dx / Math.sqrt(3.0) + dy <= 0.62) {
            return true;
         }
      }

      return false;
   }

   public Optional<Vec3> intersection(Vec3 from, Vec3 to) {
      if (!this.active()) {
         return Optional.empty();
      } else {
         Vec3 n = this.normal();
         double a = from.subtract(this.position()).dot(n);
         double b = to.subtract(this.position()).dot(n);
         if (!(a <= 1.0E-4) && !(b > 0.0) && !(a - b < 1.0E-5)) {
            Vec3 p = from.lerp(to, a / (a - b));
            Vec3 local = p.subtract(this.position());
            return inside(local.dot(this.right()), local.dot(this.up())) ? Optional.of(p) : Optional.empty();
         } else {
            return Optional.empty();
         }
      }
   }

   public boolean friendly(Entity source) {
      LivingEntity c = this.owner();
      return source != null && c != null && (source == c || source.isAlliedTo(c) || c.isAlliedTo(source) || DamageSources.isFriendlyFireBetween(source, c));
   }

   public float absorb(float amount, Vec3 point) {
      if (this.active() && !(amount <= 0.0F)) {
         float taken = Math.min(this.capacity(), amount);
         CompoundTag t = ((CompoundTag)this.entityData.get(STATE)).copy();
         t.putFloat("Capacity", this.capacity() - taken);
         t.putLong("Hit", this.level().getGameTime());
         t.putDouble("HX", point.x);
         t.putDouble("HY", point.y);
         t.putDouble("HZ", point.z);
         Vec3 localHit = point.subtract(this.position());
         t.putDouble("HitU", localHit.dot(this.right()));
         t.putDouble("HitV", localHit.dot(this.up()));
         if (t.getFloat("Capacity") <= 0.001) {
            t.putLong("Break", this.level().getGameTime());
         }

         this.entityData.set(STATE, t);
         if (this.level() instanceof ServerLevel s) {
            boolean broken = this.capacity() <= 0.001;
            s.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, broken ? 30 : 8, 0.35, 0.35, 0.35, 0.08);
            s.playSound(
               null,
               point.x,
               point.y,
               point.z,
               broken ? SoundEvents.GLASS_BREAK : SoundEvents.AMETHYST_BLOCK_HIT,
               SoundSource.PLAYERS,
               broken ? 1.0F : 0.6F,
               broken ? 0.8F : 1.5F
            );
         }

         return taken;
      } else {
         return 0.0F;
      }
   }

   private void follow(LivingEntity c) {
      Vec3 n = c.getLookAngle().normalize();
      this.setPos(this.clippedCenter(c, c.getEyePosition(), n));
      CompoundTag t = ((CompoundTag)this.entityData.get(STATE)).copy();
      t.putDouble("NX", n.x);
      t.putDouble("NY", n.y);
      t.putDouble("NZ", n.z);
      this.entityData.set(STATE, t);
   }

   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel s) {
         LivingEntity c = this.owner();
         if (c != null && c.isAlive() && !(this.age(0.0F) > 86.0F) && !(this.fade(0.0F) <= 0.0F)) {
            if (this.active()) {
               this.follow(c);

               for (Projectile p : s.getEntitiesOfClass(Projectile.class, this.getBoundingBox().inflate(6.0))) {
                  if (!p.isRemoved() && !this.friendly(p.getOwner()) && (p instanceof AbstractArrow || p instanceof AbstractMagicProjectile)) {
                     Optional<Vec3> contact = this.intersection(p.position(), p.position().add(p.getDeltaMovement()));
                     if (contact.isEmpty()) {
                        contact = this.intersection(new Vec3(p.xo, p.yo, p.zo), p.position());
                     }

                     if (!contact.isEmpty()) {
                        float amount = p instanceof AbstractMagicProjectile m
                           ? m.getDamage()
                           : (float)(((AbstractArrow)p).getBaseDamage() * p.getDeltaMovement().length());
                        float taken = this.absorb(amount, contact.get());
                        if (taken >= amount && amount > 0.0F) {
                           p.discard();
                        } else if (p instanceof AbstractMagicProjectile m) {
                           m.setDamage(Math.max(0.0F, amount - taken));
                        } else if (amount > 0.0F) {
                           ((AbstractArrow)p).setBaseDamage(((AbstractArrow)p).getBaseDamage() * (1.0F - taken / amount));
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

   protected void defineSynchedData(Builder builder) {
      builder.define(STATE, new CompoundTag());
   }

   protected void addAdditionalSaveData(CompoundTag t) {
      t.put("State", ((CompoundTag)this.entityData.get(STATE)).copy());
      if (this.owner != null) {
         t.putUUID("Owner", this.owner);
      }
   }

   protected void readAdditionalSaveData(CompoundTag t) {
      this.entityData.set(STATE, t.getCompound("State"));
      this.owner = t.hasUUID("Owner") ? t.getUUID("Owner") : null;
   }

   public AABB getBoundingBoxForCulling() {
      LivingEntity c = this.owner();
      return c != null && this.active() ? this.getBoundingBox().minmax(c.getBoundingBox().inflate(6.0)) : this.getBoundingBox().inflate(4.0);
   }

   public record VisualPose(Vec3 center, Vec3 normal, Vec3 right, Vec3 up) {
   }
}
