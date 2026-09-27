package com.frierenvoid;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.damage.DamageSources;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class ZoltraakEntity extends Entity {
   public static final int CHARGE = 14;
   public static final int PULSE = 9;
   public static final int DURATION = 46;
   public static final double RANGE = 64.0;
   public static final double SPEED = 12.0;
   public static final double HIT_RADIUS = 0.23;
   private static final EntityDataAccessor<CompoundTag> SHOT = SynchedEntityData.defineId(ZoltraakEntity.class, EntityDataSerializers.COMPOUND_TAG);
   private UUID owner;
   private float damage;
   private boolean impactPlayed;
   private int brokenBlocks;
   private final Set<UUID> shieldHits = new HashSet<>();
   private final List<double[]> shieldLosses = new ArrayList<>();
   private final Set<UUID> hit = new HashSet<>();

   public ZoltraakEntity(EntityType<? extends ZoltraakEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
   }

   public static ZoltraakEntity create(ServerLevel level, LivingEntity caster, float damage) {
      return create(level, caster, damage, false);
   }

   public static ZoltraakEntity create(ServerLevel level, LivingEntity caster, float damage, boolean black) {
      return create(level, caster, damage, black, ZoltraakMode.SINGLE, 0);
   }

   public static boolean launch(ServerLevel level, LivingEntity caster, float damage, boolean black) {
      if (!canStart(level, caster)) {
         return false;
      }

      ZoltraakMode mode = ZoltraakMode.selected(caster);

      for (int i = 0; i < mode.shots; i++) {
         level.addFreshEntity(create(level, caster, damage * mode.damage, black, mode, i));
      }

      return true;
   }

   public static ZoltraakEntity create(ServerLevel level, LivingEntity caster, float damage, boolean black, ZoltraakMode mode, int index) {
      ZoltraakEntity e = new ZoltraakEntity((EntityType<? extends ZoltraakEntity>)FrierenVoid.ZOLTRAAK_ENTITY.get(), level);
      e.owner = caster.getUUID();
      e.damage = Math.max(0.0F, damage);
      CompoundTag t = new CompoundTag();
      t.putBoolean("Black", black);
      t.putInt("Mode", mode.ordinal());
      t.putInt("Index", index);
      t.putLong("Start", level.getGameTime() + index * 4L);
      t.putInt("Caster", caster.getId());
      put(t, "Origin", muzzle(caster, 1.0F));
      put(t, "Direction", caster.getLookAngle());
      put(t, "End", muzzle(caster, 1.0F));
      e.entityData.set(SHOT, t);
      e.setPos(e.castOrigin(caster, 1.0F));
      return e;
   }

   public static ZoltraakEntity createRapid(ServerLevel level, LivingEntity caster, float damage, boolean black, int sequence) {
      ZoltraakEntity e = create(level, caster, damage, black, ZoltraakMode.RAPID, sequence % 3);
      CompoundTag t = ((CompoundTag)e.entityData.get(SHOT)).copy();
      t.putLong("Start", level.getGameTime());
      e.entityData.set(SHOT, t);
      return e;
   }

   public static boolean canStart(ServerLevel level, LivingEntity caster) {
      if (caster instanceof ServerPlayer p && RapidZoltraakChannel.active(p)) {
         return false;
      } else {
         int count = 0;

         for (Entity e : level.getAllEntities()) {
            if (e instanceof ZoltraakEntity z && !z.isRemoved()) {
               if (caster.getUUID().equals(z.owner) || ++count + ZoltraakMode.selected(caster).shots > 24) {
                  return false;
               }
            }
         }

         return caster.isAlive();
      }
   }

   public static Vec3 muzzle(LivingEntity caster, float partial) {
      Vec3 eye = caster.getEyePosition(partial);
      Vec3 f = caster.getViewVector(partial).normalize();
      Vec3 right = f.cross(new Vec3(0.0, 1.0, 0.0));
      if (right.lengthSqr() < 0.01) {
         right = new Vec3(1.0, 0.0, 0.0);
      } else {
         right = right.normalize();
      }

      double hand = caster.getMainArm() == HumanoidArm.RIGHT ? 1.0 : -1.0;
      Vec3 wanted = eye.add(f.scale(0.95)).add(right.scale(0.26 * hand)).add(0.0, -0.18, 0.0);
      BlockHitResult clip = caster.level().clip(new ClipContext(eye, wanted, Block.COLLIDER, Fluid.NONE, caster));
      return clip.getType() == Type.MISS ? wanted : clip.getLocation().subtract(f.scale(0.06));
   }

   private static void put(CompoundTag t, String key, Vec3 v) {
      CompoundTag n = new CompoundTag();
      n.putDouble("X", v.x);
      n.putDouble("Y", v.y);
      n.putDouble("Z", v.z);
      t.put(key, n);
   }

   private static Vec3 get(CompoundTag t, String key) {
      CompoundTag n = t.getCompound(key);
      return new Vec3(n.getDouble("X"), n.getDouble("Y"), n.getDouble("Z"));
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(SHOT, new CompoundTag());
   }

   public ZoltraakMode mode() {
      return ZoltraakMode.from(((CompoundTag)this.entityData.get(SHOT)).getInt("Mode"));
   }

   public int shotIndex() {
      return ((CompoundTag)this.entityData.get(SHOT)).getInt("Index");
   }

   public float rawAge(float partial) {
      return (float)(this.level().getGameTime() - ((CompoundTag)this.entityData.get(SHOT)).getLong("Start")) + partial;
   }

   public float age(float partial) {
      return this.choreographyAge(this.rawAge(partial));
   }

   private float choreographyAge(float a) {
      if (this.mode() == ZoltraakMode.LARGE) {
         if (a < 20.0F) {
            return Mth.clamp(a * 14.0F / 20.0F, 0.0F, 14.0F);
         } else {
            return a < 100.0F ? 14.0F + (a - 20.0F) * 9.0F / 80.0F : Mth.clamp(23.0F + (a - 100.0F) * 23.0F / 26.0F, 23.0F, 46.0F);
         }
      } else {
         return Mth.clamp(a < this.mode().charge ? a * 14.0F / this.mode().charge : 14.0F + (a - this.mode().charge) * 9.0F / this.mode().pulse, 0.0F, 46.0F);
      }
   }

   public double beamFront(float partial) {
      CompoundTag t = (CompoundTag)this.entityData.get(SHOT);
      if (this.mode() == ZoltraakMode.LARGE && t.contains("Travel")) {
         double travel = t.getDouble("Travel");
         if (!this.level().isClientSide) {
            return Math.min(this.length(), travel);
         }

         float blend = Mth.clamp((float)(this.level().getGameTime() - t.getLong("TravelTick")) + partial, 0.0F, 1.0F);
         return Math.min(this.length(), Mth.lerp(blend, t.getDouble("PreviousTravel"), travel));
      } else {
         return Math.min(
            this.length(), Math.max(0.0, this.mode() == ZoltraakMode.LARGE ? (this.rawAge(partial) - 20.0F) * 2.5 : (this.age(partial) - 14.0F) * 12.0)
         );
      }
   }

   public float largeImpactTime() {
      CompoundTag t = (CompoundTag)this.entityData.get(SHOT);
      return t.contains("ContactAge") ? t.getFloat("ContactAge") : 20.0F + (float)(this.length() / 2.5);
   }

   public float scale() {
      return this.mode().scale;
   }

   public double hitRadius() {
      return this.mode() == ZoltraakMode.LARGE ? 4.2 : 0.23 * this.scale();
   }

   private Vec3 castOrigin(LivingEntity c, float partial) {
      Vec3 base = muzzle(c, partial);
      if (this.mode() == ZoltraakMode.SINGLE) {
         return base;
      }

      Vec3 f = c.getViewVector(partial).normalize();
      Vec3 u = f.cross(new Vec3(0.0, 1.0, 0.0));
      u = u.lengthSqr() < 0.01 ? new Vec3(1.0, 0.0, 0.0) : u.normalize();
      Vec3 v = u.cross(f).normalize();
      Vec3 wanted;
      if (this.mode() == ZoltraakMode.LARGE) {
         wanted = base.add(f.scale(2.8)).add(v.scale(4.9));
      } else {
         int slot = this.shotIndex() % 3;
         double x = slot == 0 ? -1.25 : (slot == 1 ? 1.2 : 0.0);
         double y = slot == 0 ? 0.65 : (slot == 1 ? 1.35 : -0.45);
         wanted = base.add(u.scale(x)).add(v.scale(y)).add(f.scale(slot * 0.38));
      }

      BlockHitResult clip = this.level().clip(new ClipContext(c.getEyePosition(partial), wanted, Block.COLLIDER, Fluid.NONE, c));
      return clip.getType() == Type.MISS ? wanted : clip.getLocation().subtract(f.scale(0.06));
   }

   public int casterId() {
      return ((CompoundTag)this.entityData.get(SHOT)).getInt("Caster");
   }

   public boolean black() {
      return ((CompoundTag)this.entityData.get(SHOT)).getBoolean("Black");
   }

   public boolean fired() {
      return ((CompoundTag)this.entityData.get(SHOT)).getBoolean("Fired");
   }

   public boolean impact() {
      return ((CompoundTag)this.entityData.get(SHOT)).getBoolean("Impact");
   }

   public Vec3 origin() {
      return get((CompoundTag)this.entityData.get(SHOT), "Origin");
   }

   public Vec3 end() {
      return get((CompoundTag)this.entityData.get(SHOT), "End");
   }

   public Vec3 direction() {
      return get((CompoundTag)this.entityData.get(SHOT), "Direction").normalize();
   }

   public Vec3 normal() {
      return get((CompoundTag)this.entityData.get(SHOT), "Normal");
   }

   public double length() {
      return this.origin().distanceTo(this.end());
   }

   public float impactAge() {
      if (this.mode() == ZoltraakMode.RAPID && ((CompoundTag)this.entityData.get(SHOT)).contains("DoneAge")) {
         return this.choreographyAge(((CompoundTag)this.entityData.get(SHOT)).getFloat("DoneAge"));
      } else {
         return this.mode() == ZoltraakMode.LARGE ? this.choreographyAge(this.largeImpactTime()) : 14.0F + (float)(this.length() / 12.0);
      }
   }

   public Vec3 visualOrigin(float partial) {
      return !this.fired() && this.level().getEntity(this.casterId()) instanceof LivingEntity c ? this.castOrigin(c, partial) : this.origin();
   }

   public Vec3 visualDirection(float partial) {
      return !this.fired() && this.level().getEntity(this.casterId()) instanceof LivingEntity c ? c.getViewVector(partial).normalize() : this.direction();
   }

   public LivingEntity caster() {
      if (this.owner != null && this.level() instanceof ServerLevel s) {
         return s.getEntity(this.owner) instanceof LivingEntity c && c.isAlive() ? c : null;
      } else {
         return null;
      }
   }

   public boolean canHit(LivingEntity target) {
      LivingEntity c = this.caster();
      if (c != null && target != c && target.isAlive() && !target.isSpectator() && !(target instanceof ArmorStand)) {
         if (!(
            target instanceof Player p
               && (
                  p.isCreative()
                     || !(Boolean)VoidConfig.PVP.get()
                     || !this.level().getServer().isPvpAllowed()
                     || c instanceof Player cp && !cp.canHarmPlayer(p)
               )
         )) {
            if (target instanceof OwnableEntity pet && pet.getOwnerUUID() != null) {
               if (this.owner.equals(pet.getOwnerUUID())) {
                  return false;
               }

               Entity o = pet.getOwner();
               if (o != null && (o.isAlliedTo(c) || c.isAlliedTo(o))) {
                  return false;
               }
            }

            return !target.isAlliedTo(c)
               && !c.isAlliedTo(target)
               && !DamageSources.isFriendlyFireBetween(c, target)
               && !DamageSources.isFriendlyFireBetween(target, c);
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private Vec3 loadedEnd(Vec3 from, Vec3 dir) {
      Vec3 end = from;

      for (double d = 0.5; d <= 64.0; d += 0.5) {
         Vec3 p = from.add(dir.scale(d));
         BlockPos b = BlockPos.containing(p);
         if (!this.level().hasChunkAt(b) || !this.level().getWorldBorder().isWithinBounds(b)) {
            break;
         }

         end = p;
      }

      return end;
   }

   private void fire(LivingEntity c) {
      Vec3 from = this.castOrigin(c, 1.0F);
      Vec3 eye = c.getEyePosition();
      Vec3 aim = this.loadedEnd(eye, c.getLookAngle());
      BlockHitResult eyeHit = this.level().clip(new ClipContext(eye, aim, Block.COLLIDER, Fluid.NONE, c));
      Vec3 dir = eyeHit.getLocation().subtract(from).normalize();
      if (dir.lengthSqr() < 0.01) {
         dir = c.getLookAngle();
      }

      BlockHitResult result = this.level().clip(new ClipContext(from, this.loadedEnd(from, dir), Block.COLLIDER, Fluid.NONE, c));
      CompoundTag t = ((CompoundTag)this.entityData.get(SHOT)).copy();
      t.putBoolean("Fired", true);
      put(t, "Origin", from);
      put(t, "Direction", dir);
      put(t, "End", result.getLocation());
      t.putBoolean("Impact", result.getType() == Type.BLOCK);
      put(t, "Normal", Vec3.atLowerCornerOf(result.getDirection().getNormal()));
      this.entityData.set(SHOT, t);
      this.setPos(from);
      if (this.mode() == ZoltraakMode.LARGE) {
         t = ((CompoundTag)this.entityData.get(SHOT)).copy();
         put(t, "End", this.loadedEnd(from, dir));
         t.putBoolean("Impact", false);
         t.putDouble("Travel", 0.0);
         t.putDouble("PreviousTravel", 0.0);
         t.putLong("TravelTick", this.level().getGameTime());
         this.entityData.set(SHOT, t);
      }

      if (this.mode() == ZoltraakMode.RAPID) {
         t = ((CompoundTag)this.entityData.get(SHOT)).copy();
         t.putBoolean("Impact", false);
         put(t, "End", from);
         put(t, "Velocity", dir);
         ListTag path = new ListTag();
         CompoundTag point = new CompoundTag();
         put(point, "P", from);
         path.add(point);
         t.put("Path", path);
         LivingEntity chosen = null;
         double score = -1.0;

         for (LivingEntity candidate : ((ServerLevel)this.level()).getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(48.0), this::canHit)) {
            Vec3 delta = candidate.getEyePosition().subtract(from);
            double distance = delta.length();
            if (!(distance < 1.0) && !(distance > 48.0)) {
               double alignment = delta.normalize().dot(dir);
               if (!(alignment < Math.cos(Math.toRadians(28.0)))
                  && this.level().clip(new ClipContext(from, candidate.getEyePosition(), Block.COLLIDER, Fluid.NONE, c)).getType() == Type.MISS) {
                  double rank = alignment - distance * 0.0015;
                  if (rank > score) {
                     score = rank;
                     chosen = candidate;
                  }
               }
            }
         }

         if (chosen != null) {
            t.putUUID("Target", chosen.getUUID());
         }

         this.entityData.set(SHOT, t);
      }

      this.level()
         .playSound(
            null,
            from.x,
            from.y,
            from.z,
            (SoundEvent)(this.mode() == ZoltraakMode.LARGE ? FrierenVoid.ZOLTRAAK_GREAT_FIRE : FrierenVoid.ZOLTRAAK_FIRE).get(),
            SoundSource.PLAYERS,
            this.mode() == ZoltraakMode.LARGE ? 2.6F : (this.mode() == ZoltraakMode.RAPID ? 1.1F : 1.8F),
            this.mode() == ZoltraakMode.LARGE
               ? (this.black() ? 0.9F : 1.0F)
               : (this.black() ? 0.78F : 1.0F) * (this.mode() == ZoltraakMode.RAPID ? 1.12F : 1.0F)
         );
   }

   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel s) {
         LivingEntity var16 = this.caster();
         float age = this.age(0.0F);
         if (var16 != null && !(age >= 46.0F)) {
            if (!(this.rawAge(0.0F) < 0.0F)) {
               if (this.mode() == ZoltraakMode.LARGE && this.tickCount % 4 == 0 && this.rawAge(0.0F) < 100.0F) {
                  boolean contact = this.fired() && this.impact() && this.rawAge(0.0F) >= this.largeImpactTime();
                  Vec3 p = contact ? this.end().add(this.normal().scale(0.3)) : var16.position().add(var16.getLookAngle().scale(2.8)).add(0.0, 0.2, 0.0);
                  BlockState state = s.getBlockState(
                     contact ? BlockPos.containing(this.end().subtract(this.normal().scale(0.05))) : var16.blockPosition().below()
                  );
                  if (!state.isAir()) {
                     s.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), p.x, p.y, p.z, contact ? 16 : 8, 3.0, 0.6, 2.0, 0.18);
                  }

                  s.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.4, p.z, contact ? 12 : 6, 3.0, 0.7, 2.0, 0.035);
                  s.sendParticles(ParticleTypes.POOF, p.x, p.y, p.z, contact ? 12 : 6, 2.0, 0.5, 2.0, 0.05);
               }

               if (age < 14.0F) {
                  this.setPos(this.castOrigin(var16, 1.0F));
                  if (this.tickCount == 1) {
                     s.playSound(
                        null,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        (SoundEvent)FrierenVoid.ZOLTRAAK_CHARGE.get(),
                        SoundSource.PLAYERS,
                        0.8F,
                        this.black() ? 0.85F : 1.0F
                     );
                  }

                  if (this.tickCount % 2 == 0) {
                     CompoundTag t = ((CompoundTag)this.entityData.get(SHOT)).copy();
                     put(t, "Origin", this.position());
                     put(t, "Direction", var16.getLookAngle());
                     this.entityData.set(SHOT, t);
                  }
               } else {
                  if (!this.fired()) {
                     this.fire(var16);
                  }

                  if (this.mode() == ZoltraakMode.RAPID) {
                     this.tickGuided(s, var16);
                  } else {
                     if (this.mode() == ZoltraakMode.LARGE && age <= 23.0F) {
                        this.advanceGreat(s, var16);
                     }

                     double front = this.beamFront(0.0F);
                     if (age <= 23.0F && front > 0.0) {
                        Vec3 to = this.origin().add(this.direction().scale(front));
                        BlockHitResult obstacle = s.clip(new ClipContext(this.origin(), to, Block.COLLIDER, Fluid.NONE, var16));
                        to = obstacle.getLocation();
                        to = this.shieldFront(s, var16, to);
                        if (obstacle.getType() == Type.BLOCK && this.origin().distanceTo(to) < this.length() - 0.01) {
                           CompoundTag t = ((CompoundTag)this.entityData.get(SHOT)).copy();
                           put(t, "End", to);
                           t.putBoolean("Impact", true);
                           put(t, "Normal", Vec3.atLowerCornerOf(obstacle.getDirection().getNormal()));
                           this.entityData.set(SHOT, t);
                        }

                        if (this.damage > 0.0F) {
                           for (LivingEntity target : s.getEntitiesOfClass(
                              LivingEntity.class, new AABB(this.origin(), to).inflate(this.hitRadius() + 1.0), this::canHit
                           )) {
                              if (!((CompoundTag)this.entityData.get(SHOT)).getBoolean("ShieldStopped")
                                 || !(target.getBoundingBox().getCenter().subtract(this.end()).dot(this.normal()) < -0.001)) {
                                 AABB bounds = target.getBoundingBox();
                                 Optional<Vec3> contact = BeamHitMath.contact(this.origin(), to, bounds, this.hitRadius());
                                 if (this.hit.size() >= 64) {
                                    break;
                                 }

                                 if (!this.hit.contains(target.getUUID()) && contact.isPresent()) {
                                    double projected = target.getBoundingBox().getCenter().subtract(this.origin()).dot(this.direction());
                                    if (this.mode() == ZoltraakMode.LARGE && projected > front) {
                                       this.shieldFront(s, var16, this.origin().add(this.direction().scale(Math.min(this.length(), projected))));
                                    }

                                    if (!((CompoundTag)this.entityData.get(SHOT)).getBoolean("ShieldStopped")
                                       || !(target.getBoundingBox().getCenter().subtract(this.end()).dot(this.normal()) < -0.001)) {
                                       Vec3 nearest = contact.get();
                                       Vec3 surface = BeamHitMath.closest(bounds, nearest);
                                       if (s.clip(new ClipContext(nearest, surface, Block.COLLIDER, Fluid.NONE, var16)).getType() == Type.MISS) {
                                          this.hit.add(target.getUUID());
                                          ZoltraakDamage.apply(
                                             target,
                                             this.damageAt(
                                                Math.max(
                                                   this.origin().distanceTo(contact.get()),
                                                   target.getBoundingBox().getCenter().subtract(this.origin()).dot(this.direction())
                                                )
                                             ),
                                             ((AbstractSpell)(this.black() ? FrierenVoid.BLACK_ZOLTRAAK : FrierenVoid.ZOLTRAAK).get())
                                                .getDamageSource(this, var16),
                                             false
                                          );
                                          s.sendParticles(
                                             this.black() ? ParticleTypes.SMOKE : ParticleTypes.END_ROD,
                                             target.getX(),
                                             target.getY() + target.getBbHeight() * 0.55,
                                             target.getZ(),
                                             7,
                                             0.18,
                                             0.25,
                                             0.18,
                                             0.12
                                          );
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }

                     if (!this.impactPlayed && this.impact() && age >= this.impactAge()) {
                        this.impactPlayed = true;
                        Vec3 p = this.end().add(this.normal().scale(0.08));
                        s.playSound(
                           null,
                           p.x,
                           p.y,
                           p.z,
                           (SoundEvent)FrierenVoid.ZOLTRAAK_IMPACT.get(),
                           SoundSource.PLAYERS,
                           this.mode() == ZoltraakMode.LARGE ? 2.5F : 1.3F,
                           (this.black() ? 0.75F : 1.0F) * (this.mode() == ZoltraakMode.LARGE ? 0.65F : 1.0F)
                        );
                        BlockState state = s.getBlockState(BlockPos.containing(this.end().subtract(this.normal().scale(0.05))));
                        if (!state.isAir()) {
                           s.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), p.x, p.y, p.z, 36, 0.35, 0.35, 0.35, 0.4);
                        }

                        s.sendParticles(ParticleTypes.POOF, p.x, p.y, p.z, 12, 0.3, 0.3, 0.3, 0.1);
                     }
                  }
               }
            }
         } else {
            this.discard();
         }
      }
   }

   public List<Vec3> guidedPath() {
      ArrayList<Vec3> result = new ArrayList<>();
      ListTag path = ((CompoundTag)this.entityData.get(SHOT)).getList("Path", 10);

      for (int i = 0; i < path.size(); i++) {
         result.add(get(path.getCompound(i), "P"));
      }

      return result;
   }

   public List<Vec3> guidedPath(float partial) {
      List<Vec3> points = this.guidedPath();
      CompoundTag t = (CompoundTag)this.entityData.get(SHOT);
      int before = t.getInt("PathBefore");
      if (this.level().isClientSide && before >= 1 && before < points.size()) {
         float blend = Mth.clamp((float)(this.level().getGameTime() - t.getLong("PathTick")) + partial, 0.0F, 1.0F);
         if (blend >= 1.0F) {
            return points;
         }

         double index = before - 1 + (points.size() - before) * blend;
         int last = (int)Math.floor(index);
         ArrayList<Vec3> visible = new ArrayList<>(points.subList(0, last + 1));
         if (last + 1 < points.size()) {
            visible.add(points.get(last).lerp(points.get(last + 1), index - last));
         }

         return visible;
      } else {
         return points;
      }
   }

   public float guidedFade(float partial) {
      CompoundTag t = (CompoundTag)this.entityData.get(SHOT);
      return t.contains("DoneAge") ? 1.0F - Mth.clamp((this.rawAge(partial) - t.getFloat("DoneAge")) / 12.0F, 0.0F, 1.0F) : 1.0F;
   }

   private void tickGuided(ServerLevel s, LivingEntity c) {
      CompoundTag t = ((CompoundTag)this.entityData.get(SHOT)).copy();
      if (t.contains("DoneAge")) {
         if (this.rawAge(0.0F) - t.getFloat("DoneAge") >= 12.0F) {
            this.discard();
         }
      } else {
         Vec3 head = this.end();
         Vec3 velocity = get(t, "Velocity");
         ListTag path = t.getList("Path", 10);
         t.putInt("PathBefore", path.size());
         t.putLong("PathTick", s.getGameTime());
         double travel = t.getDouble("Distance");
         LivingEntity target = t.hasUUID("Target") && s.getEntity(t.getUUID("Target")) instanceof LivingEntity l && this.canHit(l) ? l : null;
         int step = 0;

         while (step < 2) {
            if (target != null) {
               Vec3 delta = target.getEyePosition().subtract(head);
               if (!(delta.length() > 48.0)
                  && !(delta.normalize().dot(velocity) < 0.25)
                  && s.clip(new ClipContext(head, target.getEyePosition(), Block.COLLIDER, Fluid.NONE, c)).getType() == Type.MISS) {
                  Vec3 wanted = delta.normalize();
                  double angle = Math.acos(Mth.clamp(velocity.dot(wanted), -1.0, 1.0));
                  double fraction = angle < 1.0E-5 ? 1.0 : Math.min(1.0, Math.toRadians(3.5) / angle);
                  velocity = velocity.lerp(wanted, fraction).normalize();
               } else {
                  target = null;
                  t.remove("Target");
               }
            }

            double distance = Math.min(1.5, 64.0 - travel);
            Vec3 next = head.add(velocity.scale(distance));
            BlockPos bp = BlockPos.containing(next);
            if (s.hasChunkAt(bp) && s.getWorldBorder().isWithinBounds(bp) && !s.isOutsideBuildHeight(bp)) {
               BlockHitResult wall = s.clip(new ClipContext(head, next, Block.COLLIDER, Fluid.NONE, c));
               next = wall.getLocation();
               LivingEntity victim = null;
               Vec3 contact = next;
               double nearest = head.distanceToSqr(next) + 1.0E-5;

               for (LivingEntity candidate : s.getEntitiesOfClass(LivingEntity.class, new AABB(head, next).inflate(1.23), this::canHit)) {
                  Optional<Vec3> intersection = BeamHitMath.contact(head, next, candidate.getBoundingBox(), 0.23);
                  if (intersection.isPresent()
                     && s.clip(
                              new ClipContext(
                                 intersection.get(), BeamHitMath.closest(candidate.getBoundingBox(), intersection.get()), Block.COLLIDER, Fluid.NONE, c
                              )
                           )
                           .getType()
                        == Type.MISS
                     && head.distanceToSqr(intersection.get()) < nearest) {
                     victim = candidate;
                     contact = intersection.get();
                     nearest = head.distanceToSqr(contact);
                  }
               }

               boolean shieldStop = false;
               Vec3 headNow = head;
               List<DefenseEntity> barriers = s.getEntitiesOfClass(DefenseEntity.class, new AABB(head, contact).inflate(3.0));
               barriers.sort(Comparator.comparingDouble(x -> x.position().distanceToSqr(headNow)));

               for (DefenseEntity barrier : barriers) {
                  if (!barrier.friendly(c) && !this.shieldHits.contains(barrier.getUUID())) {
                     Optional<Vec3> crossing = barrier.intersection(head, contact);
                     if (!crossing.isEmpty()) {
                        this.shieldHits.add(barrier.getUUID());
                        float absorbed = barrier.absorb(this.damage, crossing.get());
                        if (absorbed >= this.damage && this.damage > 0.0F) {
                           contact = crossing.get();
                           victim = null;
                           shieldStop = true;
                           break;
                        }

                        this.damage = Math.max(0.0F, this.damage - absorbed);
                     }
                  }
               }

               boolean stopped = shieldStop || wall.getType() == Type.BLOCK || victim != null;
               travel += head.distanceTo(contact);
               head = contact;
               CompoundTag point = new CompoundTag();
               put(point, "P", head);
               path.add(point);
               if (victim != null && this.damage > 0.0F && this.hit.add(victim.getUUID())) {
                  ZoltraakDamage.apply(
                     victim,
                     this.damage,
                     ((AbstractSpell)(this.black() ? FrierenVoid.BLACK_ZOLTRAAK : FrierenVoid.ZOLTRAAK).get()).getDamageSource(this, c),
                     true
                  );
               }

               if (!stopped && !(travel >= 63.999) && path.size() < 90) {
                  step++;
                  continue;
               }

               t.putFloat("DoneAge", this.rawAge(0.0F));
               t.putBoolean("Impact", stopped);
               put(t, "Normal", victim == null ? Vec3.atLowerCornerOf(wall.getDirection().getNormal()) : velocity.scale(-1.0));
               if (stopped) {
                  s.sendParticles(this.black() ? ParticleTypes.SMOKE : ParticleTypes.END_ROD, head.x, head.y, head.z, 9, 0.18, 0.18, 0.18, 0.15);
                  s.playSound(
                     null, head.x, head.y, head.z, (SoundEvent)FrierenVoid.ZOLTRAAK_IMPACT.get(), SoundSource.PLAYERS, 0.7F, this.black() ? 0.85F : 1.15F
                  );
               }
               break;
            }

            t.putFloat("DoneAge", this.rawAge(0.0F));
            break;
         }

         put(t, "End", head);
         put(t, "Velocity", velocity);
         t.put("Path", path);
         t.putDouble("Distance", travel);
         this.entityData.set(SHOT, t);
      }
   }

   private float damageAt(double distance) {
      float result = this.damage;

      for (double[] loss : this.shieldLosses) {
         if (distance >= loss[0] - 0.001) {
            result = (float)(result - loss[1]);
         }
      }

      return Math.max(0.0F, result);
   }

   private Vec3 shieldFront(ServerLevel s, LivingEntity c, Vec3 to) {
      List<DefenseEntity> barriers = s.getEntitiesOfClass(DefenseEntity.class, new AABB(this.origin(), to).inflate(3.0));
      barriers.sort(Comparator.comparingDouble(x -> x.position().distanceToSqr(this.origin())));

      for (DefenseEntity barrier : barriers) {
         if (!barrier.friendly(c) && !this.shieldHits.contains(barrier.getUUID())) {
            Optional<Vec3> contact = barrier.intersection(this.origin(), to);
            if (!contact.isEmpty()) {
               this.shieldHits.add(barrier.getUUID());
               double distance = this.origin().distanceTo(contact.get());
               float amount = this.damageAt(distance);
               float absorbed = barrier.absorb(amount, contact.get());
               this.shieldLosses.add(new double[]{distance, absorbed});
               if (amount > 0.0F && absorbed >= amount) {
                  CompoundTag state = ((CompoundTag)this.entityData.get(SHOT)).copy();
                  put(state, "End", contact.get());
                  put(state, "Normal", barrier.normal());
                  state.putBoolean("Impact", true);
                  state.putBoolean("ShieldStopped", true);
                  state.putFloat("ContactAge", this.rawAge(0.0F));
                  if (this.mode() == ZoltraakMode.LARGE) {
                     state.putDouble("PreviousTravel", Math.min(state.getDouble("Travel"), distance));
                     state.putDouble("Travel", distance);
                     state.putLong("TravelTick", s.getGameTime());
                  }

                  this.entityData.set(SHOT, state);
                  return contact.get();
               }
            }
         }
      }

      return to;
   }

   private void advanceGreat(ServerLevel s, LivingEntity c) {
      if (!this.impact()) {
         double previous = this.beamFront(0.0F);
         double target = Math.min(this.length(), Math.min(previous + 2.5, Math.max(0.0, (this.rawAge(0.0F) - 20.0F) * 2.5)));
         Vec3 limited = this.shieldFront(s, c, this.origin().add(this.direction().scale(target)));
         if (!this.impact()) {
            BlockHitResult placed = s.clip(new ClipContext(this.origin(), this.origin().add(this.direction().scale(previous)), Block.COLLIDER, Fluid.NONE, c));
            double start = placed.getType() == Type.BLOCK ? Math.max(0.0, this.origin().distanceTo(placed.getLocation()) - 0.5) : previous;
            GreatZoltraakTerrain.Result result = GreatZoltraakTerrain.advance(
               s,
               c,
               this.origin(),
               this.direction(),
               start,
               target,
               this.hitRadius(),
               Math.max(0, 4096 - this.brokenBlocks),
               this.damage > 0.0F && (Boolean)VoidConfig.ZOLTRAAK_TERRAIN.get()
            );
            this.brokenBlocks = this.brokenBlocks + result.removed();
            CompoundTag t = ((CompoundTag)this.entityData.get(SHOT)).copy();
            t.putDouble("PreviousTravel", Math.min(previous, result.distance()));
            t.putDouble("Travel", result.distance());
            t.putLong("TravelTick", s.getGameTime());
            if (result.blocked()) {
               put(t, "End", this.origin().add(this.direction().scale(result.distance())));
               put(t, "Normal", this.direction().scale(-1.0));
               t.putBoolean("Impact", true);
               t.putFloat("ContactAge", this.rawAge(0.0F));
            }

            this.entityData.set(SHOT, t);
         }
      }
   }

   public AABB getBoundingBoxForCulling() {
      return this.mode() == ZoltraakMode.RAPID
         ? new AABB(this.origin(), this.origin()).inflate(66.0)
         : new AABB(this.origin(), this.end()).inflate(this.mode() == ZoltraakMode.LARGE ? 16.0 : 9.0);
   }

   public boolean shouldRenderAtSqrDistance(double d) {
      return d < 36864.0;
   }

   protected void addAdditionalSaveData(CompoundTag t) {
      t.put("Shot", ((CompoundTag)this.entityData.get(SHOT)).copy());
      if (this.owner != null) {
         t.putUUID("Owner", this.owner);
      }

      t.putFloat("Damage", this.damage);
      t.putInt("BrokenBlocks", this.brokenBlocks);
      ListTag ids = new ListTag();

      for (UUID id : this.hit) {
         ids.add(NbtUtils.createUUID(id));
      }

      t.put("HitEntities", ids);
      ListTag losses = new ListTag();

      for (double[] loss : this.shieldLosses) {
         CompoundTag n = new CompoundTag();
         n.putDouble("At", loss[0]);
         n.putDouble("Amount", loss[1]);
         losses.add(n);
      }

      t.put("ShieldLosses", losses);
      ListTag shields = new ListTag();

      for (UUID id : this.shieldHits) {
         shields.add(NbtUtils.createUUID(id));
      }

      t.put("ShieldHits", shields);
   }

   protected void readAdditionalSaveData(CompoundTag t) {
      this.entityData.set(SHOT, t.getCompound("Shot"));
      this.owner = t.hasUUID("Owner") ? t.getUUID("Owner") : null;
      this.damage = t.getFloat("Damage");
      this.brokenBlocks = t.getInt("BrokenBlocks");
      this.hit.clear();

      for (Tag id : t.getList("HitEntities", 11)) {
         this.hit.add(NbtUtils.loadUUID(id));
      }

      this.shieldHits.clear();

      for (Tag id : t.getList("ShieldHits", 11)) {
         this.shieldHits.add(NbtUtils.loadUUID(id));
      }

      this.shieldLosses.clear();

      for (Tag n : t.getList("ShieldLosses", 10)) {
         CompoundTag loss = (CompoundTag)n;
         this.shieldLosses.add(new double[]{loss.getDouble("At"), loss.getDouble("Amount")});
      }
   }
}
