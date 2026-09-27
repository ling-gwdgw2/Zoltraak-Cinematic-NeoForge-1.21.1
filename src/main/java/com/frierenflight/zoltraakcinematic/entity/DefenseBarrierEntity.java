package com.frierenflight.zoltraakcinematic.entity;

import com.frierenflight.zoltraakcinematic.registry.ModCinematicEntities;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class DefenseBarrierEntity extends Entity {
    private static final EntityDataAccessor<CompoundTag> STATE =
            SynchedEntityData.defineId(DefenseBarrierEntity.class, EntityDataSerializers.COMPOUND_TAG);

    public static final double CELL_RADIUS = 0.62;
    public static final double BARRIER_RADIUS = 2.8;
    public static final double DOME_RADIUS = 3.2;

    public static final int MODE_DIRECTIONAL = 0;
    public static final int MODE_DOME = 1;

    private UUID ownerUUID;
    private VisualPose lastVisualPose;

    public DefenseBarrierEntity(EntityType<? extends DefenseBarrierEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static DefenseBarrierEntity create(ServerLevel level, LivingEntity caster, float capacity, int mode) {
        return create(level, caster, capacity, mode, 300);
    }

    public static DefenseBarrierEntity create(ServerLevel level, LivingEntity caster, float capacity, int mode, int duration) {
        DefenseBarrierEntity entity = new DefenseBarrierEntity(ModCinematicEntities.DEFENSE_BARRIER.get(), level);
        entity.ownerUUID = caster.getUUID();

        CompoundTag tag = new CompoundTag();
        tag.putInt("Owner", caster.getId());
        tag.putFloat("Capacity", capacity);
        tag.putFloat("Maximum", capacity);
        tag.putLong("Birth", level.getGameTime());
        tag.putInt("Duration", duration);
        tag.putInt("Mode", mode);

        Vec3 look = caster.getLookAngle().normalize();
        tag.putDouble("NX", look.x);
        tag.putDouble("NY", look.y);
        tag.putDouble("NZ", look.z);

        entity.entityData.set(STATE, tag);

        if (mode == MODE_DOME) {
            entity.setPos(caster.getX(), caster.getY(), caster.getZ());
        } else {
            entity.follow(caster);
        }

        level.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.35F);
        level.playSound(null, entity.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.6F);
        return entity;
    }

    public static DefenseBarrierEntity find(LivingEntity owner) {
        if (owner == null) return null;
        for (DefenseBarrierEntity e : owner.level().getEntitiesOfClass(DefenseBarrierEntity.class, owner.getBoundingBox().inflate(10.0))) {
            if (e.active() && (owner.getUUID().equals(e.ownerUUID) || e.owner() == owner)) {
                return e;
            }
        }
        return null;
    }

    public LivingEntity owner() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            CompoundTag t = this.entityData.get(STATE);
            if (t.contains("Owner")) {
                Entity e = this.level().getEntity(t.getInt("Owner"));
                return e instanceof LivingEntity living ? living : null;
            }
            return null;
        } else {
            return this.ownerUUID != null && serverLevel.getEntity(this.ownerUUID) instanceof LivingEntity living ? living : null;
        }
    }

    public float capacity() {
        return this.entityData.get(STATE).getFloat("Capacity");
    }

    public float maximum() {
        return Math.max(1.0F, this.entityData.get(STATE).getFloat("Maximum"));
    }

    public int mode() {
        return this.entityData.get(STATE).getInt("Mode");
    }

    public float age(float partial) {
        CompoundTag t = this.entityData.get(STATE);
        long birth = t.contains("Birth") ? t.getLong("Birth") : (t.contains("Start") ? t.getLong("Start") : this.level().getGameTime());
        return (float) (this.level().getGameTime() - birth) + partial;
    }

    public boolean isDissolving() {
        CompoundTag t = this.entityData.get(STATE);
        return t.contains("Break") || t.contains("Dissolve");
    }

    public boolean active() {
        return !this.isRemoved() && this.capacity() > 0.0F && !isDissolving() && this.owner() != null && this.owner().isAlive();
    }

    public Vec3 normal() {
        CompoundTag t = this.entityData.get(STATE);
        return new Vec3(t.getDouble("NX"), t.getDouble("NY"), t.getDouble("NZ")).normalize();
    }

    public Vec3 right() {
        Vec3 norm = this.normal();
        Vec3 u = norm.cross(new Vec3(0.0, 1.0, 0.0));
        return u.lengthSqr() < 0.001 ? new Vec3(1.0, 0.0, 0.0) : u.normalize();
    }

    public Vec3 up() {
        return this.right().cross(this.normal()).normalize();
    }

    public float hitAge(float partial) {
        CompoundTag t = this.entityData.get(STATE);
        return t.contains("Hit") ? (float) (this.level().getGameTime() - t.getLong("Hit")) + partial : 100.0F;
    }

    public Vec3 hitPoint() {
        CompoundTag t = this.entityData.get(STATE);
        return new Vec3(t.getDouble("HX"), t.getDouble("HY"), t.getDouble("HZ"));
    }

    public float fade(float partial) {
        CompoundTag t = this.entityData.get(STATE);
        if (t.contains("Break")) {
            return Math.max(0.0F, 1.0F - ((float) (this.level().getGameTime() - t.getLong("Break")) + partial) / 12.0F);
        } else if (t.contains("Dissolve")) {
            return Math.max(0.0F, 1.0F - ((float) (this.level().getGameTime() - t.getLong("Dissolve")) + partial) / 12.0F);
        } else {
            int duration = t.contains("Duration") ? t.getInt("Duration") : 300;
            float remaining = duration - this.age(partial);
            if (remaining < 12.0F) {
                return Math.max(0.0F, remaining / 12.0F);
            }
            return 1.0F;
        }
    }

    public void refresh(float newCapacity, int desiredMode, int newDuration) {
        CompoundTag t = this.entityData.get(STATE).copy();
        t.putLong("Birth", this.level().getGameTime());
        t.remove("Dissolve");
        t.remove("Break");
        t.putInt("Mode", desiredMode);
        t.putInt("Duration", newDuration);
        t.putFloat("Capacity", Math.max(capacity(), newCapacity));
        t.putFloat("Maximum", Math.max(maximum(), newCapacity));
        this.entityData.set(STATE, t);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.1F, 1.5F);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.8F);
        }
    }

    public void dismiss() {
        if (this.active() && this.level() instanceof ServerLevel) {
            CompoundTag copy = this.entityData.get(STATE).copy();
            copy.putLong("Dissolve", this.level().getGameTime());
            this.entityData.set(STATE, copy);
        }
    }

    public VisualPose visualPose(float partial) {
        LivingEntity caster = this.owner();
        if (this.active() && caster != null) {
            if (mode() == MODE_DOME) {
                Vec3 center = new Vec3(
                        Mth.lerp(partial, caster.xOld, caster.getX()),
                        Mth.lerp(partial, caster.yOld, caster.getY()),
                        Mth.lerp(partial, caster.zOld, caster.getZ())
                );
                this.lastVisualPose = new VisualPose(center, new Vec3(0, 1, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 1));
            } else {
                Vec3 n = Vec3.directionFromRotation(caster.getViewXRot(partial), Mth.rotLerp(partial, caster.yRotO, caster.getYRot())).normalize();
                Vec3 eye = caster.getEyePosition(partial);
                Vec3 u = n.cross(new Vec3(0.0, 1.0, 0.0));
                u = u.lengthSqr() < 0.001 ? new Vec3(1.0, 0.0, 0.0) : u.normalize();
                this.lastVisualPose = new VisualPose(this.clippedCenter(caster, eye, n), n, u, u.cross(n).normalize());
            }
        }

        return this.lastVisualPose != null
                ? this.lastVisualPose
                : new VisualPose(this.position(), this.normal(), this.right(), this.up());
    }

    private Vec3 clippedCenter(LivingEntity caster, Vec3 eye, Vec3 n) {
        Vec3 target = eye.add(n.scale(1.7));
        BlockHitResult clip = this.level().clip(new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        return clip.getType() == HitResult.Type.MISS ? target : clip.getLocation().subtract(n.scale(0.08));
    }

    public Vec3 visualHit(VisualPose pose) {
        CompoundTag t = this.entityData.get(STATE);
        Vec3 diff = this.hitPoint().subtract(this.position());
        double x = t.contains("HitU") ? t.getDouble("HitU") : diff.dot(this.right());
        double y = t.contains("HitV") ? t.getDouble("HitV") : diff.dot(this.up());
        return pose.right().scale(x).add(pose.up().scale(y));
    }

    /**
     * 19 Hexagonal cells (q = -2..2, r = -2..2, |q+r| <= 2)
     */
    public static List<Vec3> cells() {
        List<Vec3> cells = new ArrayList<>();
        for (int q = -2; q <= 2; q++) {
            for (int r = -2; r <= 2; r++) {
                if (Math.abs(q + r) <= 2) {
                    cells.add(new Vec3(Math.sqrt(3.0) * CELL_RADIUS * (q + r * 0.5), 0.93 * r, 0.0));
                }
            }
        }
        return cells;
    }

    public static boolean inside(double x, double y) {
        for (Vec3 c : cells()) {
            double dx = Math.abs(x - c.x);
            double dy = Math.abs(y - c.y);
            if (dx <= Math.sqrt(3.0) * CELL_RADIUS * 0.5 && dy <= CELL_RADIUS && (dx / Math.sqrt(3.0) + dy) <= CELL_RADIUS) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ray intersection test with either flat 19-cell shield or 3D hemisphere dome
     */
    public Optional<Vec3> intersection(Vec3 from, Vec3 to) {
        if (!this.active()) {
            return Optional.empty();
        }

        if (mode() == MODE_DOME) {
            Vec3 center = this.position();
            double r = DOME_RADIUS;
            Vec3 d = to.subtract(from);
            Vec3 f = from.subtract(center);

            double a = d.dot(d);
            double b = 2.0 * f.dot(d);
            double c = f.dot(f) - r * r;
            double discriminant = b * b - 4.0 * a * c;

            if (discriminant >= 0) {
                discriminant = Math.sqrt(discriminant);
                double t1 = (-b - discriminant) / (2.0 * a);
                if (t1 >= 0.0 && t1 <= 1.0) {
                    Vec3 hit = from.add(d.scale(t1));
                    if (hit.y >= center.y - 0.2) {
                        return Optional.of(hit);
                    }
                }
                double t2 = (-b + discriminant) / (2.0 * a);
                if (t2 >= 0.0 && t2 <= 1.0) {
                    Vec3 hit = from.add(d.scale(t2));
                    if (hit.y >= center.y - 0.2) {
                        return Optional.of(hit);
                    }
                }
            }
            return Optional.empty();
        } else {
            Vec3 n = this.normal();
            double a = from.subtract(this.position()).dot(n);
            double b = to.subtract(this.position()).dot(n);
            if (a > 1.0E-4 && b <= 0.0 && (a - b) >= 1.0E-5) {
                Vec3 p = from.lerp(to, a / (a - b));
                Vec3 local = p.subtract(this.position());
                return inside(local.dot(this.right()), local.dot(this.up())) ? Optional.of(p) : Optional.empty();
            }
            return Optional.empty();
        }
    }

    public boolean friendly(Entity source) {
        LivingEntity c = this.owner();
        return source != null && c != null && (source == c || source.isAlliedTo(c) || c.isAlliedTo(source) || DamageSources.isFriendlyFireBetween(source, c));
    }

    public float absorb(float amount, Vec3 point) {
        if (!this.active() || amount <= 0.0F) {
            return 0.0F;
        }

        float taken = Math.min(this.capacity(), amount);
        CompoundTag t = this.entityData.get(STATE).copy();
        float newCap = this.capacity() - taken;
        t.putFloat("Capacity", newCap);
        t.putLong("Hit", this.level().getGameTime());
        t.putDouble("HX", point.x);
        t.putDouble("HY", point.y);
        t.putDouble("HZ", point.z);

        Vec3 localHit = point.subtract(this.position());
        t.putDouble("HitU", localHit.dot(this.right()));
        t.putDouble("HitV", localHit.dot(this.up()));

        boolean broken = newCap <= 0.001f;
        if (broken) {
            t.putLong("Break", this.level().getGameTime());
        }

        this.entityData.set(STATE, t);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, broken ? 35 : 10, 0.4, 0.4, 0.4, 0.08);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, broken ? 25 : 8, 0.3, 0.3, 0.3, 0.1);
            serverLevel.playSound(
                    null,
                    point.x, point.y, point.z,
                    broken ? SoundEvents.GLASS_BREAK : SoundEvents.AMETHYST_BLOCK_HIT,
                    SoundSource.PLAYERS,
                    broken ? 1.2F : 0.8F,
                    broken ? 0.85F : 1.4F
            );
            if (broken) {
                serverLevel.playSound(null, point.x, point.y, point.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.2F);
            }
        }

        return taken;
    }

    private void follow(LivingEntity caster) {
        Vec3 n = caster.getLookAngle().normalize();
        this.setPos(this.clippedCenter(caster, caster.getEyePosition(), n));
        CompoundTag t = this.entityData.get(STATE).copy();
        t.putDouble("NX", n.x);
        t.putDouble("NY", n.y);
        t.putDouble("NZ", n.z);
        this.entityData.set(STATE, t);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level() instanceof ServerLevel serverLevel) {
            LivingEntity caster = this.owner();
            if (caster != null && caster.isAlive() && this.fade(0.0F) > 0.0F) {
                CompoundTag stateTag = this.entityData.get(STATE);
                int duration = stateTag.contains("Duration") ? stateTag.getInt("Duration") : 300;

                // When duration is reached, start graceful dissolve
                if (this.age(0.0F) >= duration && !stateTag.contains("Dissolve") && !stateTag.contains("Break")) {
                    CompoundTag copy = stateTag.copy();
                    copy.putLong("Dissolve", this.level().getGameTime());
                    this.entityData.set(STATE, copy);
                }

                if (this.active()) {
                    if (mode() == MODE_DOME) {
                        this.setPos(caster.getX(), caster.getY(), caster.getZ());
                    } else {
                        this.follow(caster);
                    }

                    double checkRadius = mode() == MODE_DOME ? DOME_RADIUS + 2.0 : BARRIER_RADIUS + 2.5;
                    for (Projectile p : serverLevel.getEntitiesOfClass(Projectile.class, this.getBoundingBox().inflate(checkRadius))) {
                        if (!p.isRemoved() && !this.friendly(p.getOwner())) {
                            Vec3 curPos = p.position();
                            Vec3 nextPos = curPos.add(p.getDeltaMovement());
                            Optional<Vec3> contact = this.intersection(curPos, nextPos);
                            if (contact.isEmpty()) {
                                contact = this.intersection(new Vec3(p.xo, p.yo, p.zo), curPos);
                            }

                            if (contact.isPresent()) {
                                float pDmg = 6.0f;
                                if (p instanceof AbstractMagicProjectile m) {
                                    pDmg = m.getDamage();
                                } else if (p instanceof AbstractArrow arrow) {
                                    pDmg = (float) (arrow.getBaseDamage() * p.getDeltaMovement().length());
                                } else if (p instanceof ZoltraakBarrageProjectileEntity bp) {
                                    pDmg = bp.getDamage();
                                    bp.stopOnImpact(contact.get());
                                }

                                float absorbed = this.absorb(pDmg, contact.get());
                                if (absorbed >= pDmg && pDmg > 0.0F) {
                                    if (p instanceof ZoltraakBarrageProjectileEntity bp) {
                                        bp.stopOnImpact(contact.get());
                                    } else {
                                        p.discard();
                                    }
                                } else if (p instanceof AbstractMagicProjectile m) {
                                    m.setDamage(Math.max(0.0F, pDmg - absorbed));
                                } else if (p instanceof AbstractArrow arrow && pDmg > 0.0F) {
                                    arrow.setBaseDamage(arrow.getBaseDamage() * (1.0F - absorbed / pDmg));
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

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE, new CompoundTag());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.put("State", this.entityData.get(STATE).copy());
        if (this.ownerUUID != null) {
            t.putUUID("Owner", this.ownerUUID);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        this.entityData.set(STATE, t.getCompound("State"));
        this.ownerUUID = t.hasUUID("Owner") ? t.getUUID("Owner") : null;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        LivingEntity c = this.owner();
        return c != null && this.active()
                ? this.getBoundingBox().minmax(c.getBoundingBox().inflate(6.0))
                : this.getBoundingBox().inflate(5.0);
    }

    public record VisualPose(Vec3 center, Vec3 normal, Vec3 right, Vec3 up) {
    }
}
