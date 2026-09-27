package com.frierenflight.zoltraakcinematic.entity;

import com.frierenflight.zoltraakcinematic.ZoltraakDamage;
import com.frierenflight.zoltraakcinematic.client.renderer.IZoltraakVisualEntity;
import com.frierenflight.zoltraakcinematic.client.renderer.ZoltraakMode;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicEntities;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSounds;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSpells;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * High-Speed Tapered Projectile for Fern's Rapid Barrage.
 * Implements IZoltraakVisualEntity to leverage frierenvoid's RapidZoltraakRenderer with GREAT_PLUME GLSL shaders.
 */
public class ZoltraakBarrageProjectileEntity extends ThrowableProjectile implements IZoltraakVisualEntity {
    private static final EntityDataAccessor<Integer> DATA_COLOR_THEME =
            SynchedEntityData.defineId(ZoltraakBarrageProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CIRCLE_INDEX =
            SynchedEntityData.defineId(ZoltraakBarrageProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(ZoltraakBarrageProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DONE_AGE =
            SynchedEntityData.defineId(ZoltraakBarrageProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_IMPACT_STOPPED =
            SynchedEntityData.defineId(ZoltraakBarrageProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_TARGET_ID =
            SynchedEntityData.defineId(ZoltraakBarrageProjectileEntity.class, EntityDataSerializers.INT);

    public static final int MAX_LIFETIME = 35; // ~1.75 seconds flight time
    private float damage = 8.5f;
    private Vec3 spawnOrigin;
    private boolean clientEffectsTriggered = false;

    public float getDamage() {
        return this.damage;
    }
    private LivingEntity targetEntity;
    private final List<Vec3> pathHistory = new ArrayList<>();

    public boolean markClientEffectsTriggered() {
        if (!clientEffectsTriggered) {
            clientEffectsTriggered = true;
            return true;
        }
        return false;
    }

    public void setSpawnOrigin(Vec3 origin) {
        this.spawnOrigin = origin;
    }

    public void setTargetEntity(LivingEntity target) {
        this.targetEntity = target;
        this.entityData.set(DATA_TARGET_ID, target != null ? target.getId() : -1);
    }

    public LivingEntity getTargetEntity() {
        if (this.targetEntity != null && this.targetEntity.isAlive()) {
            return this.targetEntity;
        }
        int id = this.entityData.get(DATA_TARGET_ID);
        if (id != -1 && level() != null) {
            Entity e = level().getEntity(id);
            if (e instanceof LivingEntity living && living.isAlive()) {
                this.targetEntity = living;
                return living;
            }
        }
        return null;
    }

    public ZoltraakBarrageProjectileEntity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public ZoltraakBarrageProjectileEntity(Level level, LivingEntity shooter, float damage, int colorTheme) {
        this(ModCinematicEntities.ZOLTRAAK_BARRAGE_PROJECTILE.get(), level);
        setOwner(shooter);
        this.damage = damage;
        this.entityData.set(DATA_COLOR_THEME, colorTheme);
        this.entityData.set(DATA_CIRCLE_INDEX, 0);
        this.entityData.set(DATA_OWNER_ID, shooter != null ? shooter.getId() : -1);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_COLOR_THEME, 0);
        builder.define(DATA_CIRCLE_INDEX, 0);
        builder.define(DATA_OWNER_ID, -1);
        builder.define(DATA_TARGET_ID, -1);
        builder.define(DATA_DONE_AGE, -1.0f);
        builder.define(DATA_IMPACT_STOPPED, false);
    }

    public boolean isImpactStopped() {
        return this.entityData.get(DATA_IMPACT_STOPPED);
    }

    public float getDoneAge() {
        return this.entityData.get(DATA_DONE_AGE);
    }

    public void stopOnImpact(Vec3 hitPos) {
        if (isImpactStopped()) return;
        setPos(hitPos.x, hitPos.y, hitPos.z);
        setDeltaMovement(Vec3.ZERO);
        this.entityData.set(DATA_IMPACT_STOPPED, true);
        this.entityData.set(DATA_DONE_AGE, rawAge(0.0f));
        if (pathHistory.isEmpty() || pathHistory.get(pathHistory.size() - 1).distanceToSqr(hitPos) > 0.01) {
            pathHistory.add(hitPos);
        }
    }

    public int getColorTheme() {
        return this.entityData.get(DATA_COLOR_THEME);
    }

    public void setColorTheme(int theme) {
        this.entityData.set(DATA_COLOR_THEME, theme);
    }

    public int getCircleIndex() {
        return this.entityData.get(DATA_CIRCLE_INDEX);
    }

    public void setCircleIndex(int index) {
        this.entityData.set(DATA_CIRCLE_INDEX, index);
    }

    @Override
    public Entity getOwner() {
        Entity owner = super.getOwner();
        if (owner != null) return owner;
        int id = this.entityData.get(DATA_OWNER_ID);
        if (id != -1 && level() != null) {
            return level().getEntity(id);
        }
        return null;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0;
    }

    @Override
    public ZoltraakMode mode() {
        return ZoltraakMode.RAPID;
    }

    @Override
    public boolean black() {
        return getColorTheme() == 1;
    }

    @Override
    public float rawAge(float partial) {
        return (float) this.tickCount + partial;
    }

    @Override
    public float age(float partial) {
        return (float) this.tickCount + partial;
    }

    public Vec3 computeCircleOrigin(float partial) {
        Entity owner = getOwner();
        if (owner instanceof LivingEntity living && living.isAlive()) {
            Vec3 eyePos = living.getEyePosition(partial);
            float yRot = living.getViewYRot(partial);
            Vec3 flatLook = Vec3.directionFromRotation(0, yRot);
            Vec3 right = Vec3.directionFromRotation(0, yRot + 90);
            Vec3 worldUp = new Vec3(0, 1, 0);

            int idx = getCircleIndex();
            if (idx >= 0 && idx < com.frierenflight.zoltraakcinematic.spell.FernBarrageSpell.BARRAGE_CIRCLE_COUNT) {
                Vec3 local = com.frierenflight.zoltraakcinematic.spell.FernBarrageSpell.BARRAGE_CIRCLE_OFFSETS[idx];
                return eyePos
                        .add(right.scale(local.x))
                        .add(worldUp.scale(local.y))
                        .add(flatLook.scale(local.z));
            }
        }
        if (spawnOrigin != null) {
            return spawnOrigin;
        }
        return pathHistory.isEmpty() ? position() : pathHistory.get(0);
    }

    @Override
    public Vec3 visualOrigin(float partial) {
        return computeCircleOrigin(partial);
    }

    @Override
    public Vec3 visualDirection(float partial) {
        Vec3 vel = getDeltaMovement();
        return vel.lengthSqr() > 0.001 ? vel.normalize() : new Vec3(0, 0, 1);
    }

    @Override
    public int casterId() {
        return this.entityData.get(DATA_OWNER_ID);
    }

    @Override
    public double length() {
        return 12.0;
    }

    @Override
    public float scale() {
        return 0.6f;
    }

    @Override
    public boolean fired() {
        return true;
    }

    @Override
    public boolean impact() {
        return false;
    }

    @Override
    public float impactAge() {
        return 12.0f;
    }

    @Override
    public float largeImpactTime() {
        return 12.0f;
    }

    @Override
    public Vec3 end() {
        return position();
    }

    @Override
    public Vec3 normal() {
        return visualDirection(1.0f).scale(-1.0);
    }

    @Override
    public double beamFront(float partial) {
        return 12.0;
    }

    @Override
    public float guidedFade(float partial) {
        float doneAge = getDoneAge();
        if (doneAge >= 0.0f) {
            return 1.0f - Mth.clamp((rawAge(partial) - doneAge) / 12.0f, 0.0f, 1.0f);
        }
        if (tickCount > MAX_LIFETIME - 12) {
            return Math.max(0.0f, (float)(MAX_LIFETIME - tickCount) / 12.0f);
        }
        return 1.0f;
    }

    @Override
    public List<Vec3> guidedPath(float partial) {
        Vec3 circleOrigin = computeCircleOrigin(partial);
        if (isImpactStopped()) {
            List<Vec3> points = new ArrayList<>();
            points.add(circleOrigin);
            for (Vec3 p : pathHistory) {
                if (points.get(points.size() - 1).distanceToSqr(p) > 0.01) {
                    points.add(p);
                }
            }
            return points.size() > 1 ? points : List.of(circleOrigin, position());
        }
        List<Vec3> points = new ArrayList<>();
        points.add(circleOrigin);
        for (Vec3 p : pathHistory) {
            if (points.get(points.size() - 1).distanceToSqr(p) > 0.01) {
                points.add(p);
            }
        }
        Vec3 currentInterp = getPosition(partial);
        if (points.get(points.size() - 1).distanceToSqr(currentInterp) > 0.01) {
            points.add(currentInterp);
        }
        if (points.size() < 2) {
            points.add(currentInterp.add(getDeltaMovement().scale(0.1)));
        }
        return points;
    }

    @Override
    public void tick() {
        super.tick();

        if (spawnOrigin == null) {
            spawnOrigin = computeCircleOrigin(0.0f);
        }

        if (isImpactStopped()) {
            float doneAge = getDoneAge();
            if (!level().isClientSide && doneAge >= 0.0f && rawAge(0.0f) - doneAge >= 12.0f) {
                discard();
            }
            return;
        }

        Vec3 pos = position();
        if (pathHistory.isEmpty()) {
            pathHistory.add(spawnOrigin);
        }
        if (pathHistory.get(pathHistory.size() - 1).distanceToSqr(pos) > 0.01) {
            pathHistory.add(pos);
        }

        if (spawnOrigin.distanceTo(pos) >= 64.0 || tickCount >= MAX_LIFETIME) {
            if (!level().isClientSide) {
                stopOnImpact(pos);
            }
            return;
        }

        // Homing Guidance: Dynamically steer toward locked target with elegant curving trajectory
        LivingEntity target = getTargetEntity();
        if (target != null && target.isAlive()) {
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            Vec3 toTarget = targetCenter.subtract(pos);
            double dist = toTarget.length();
            if (dist > 0.3) {
                Vec3 desiredDir = toTarget.normalize();
                Vec3 currentMove = getDeltaMovement();
                double speed = currentMove.length();
                if (speed > 0.001) {
                    Vec3 currentDir = currentMove.normalize();
                    // Gentle initial bloom from circle, followed by swift aerodynamic lock-on
                    float steerRate = (tickCount < 2) ? 0.18f : 0.34f;
                    Vec3 newDir = currentDir.lerp(desiredDir, steerRate).normalize();
                    setDeltaMovement(newDir.scale(speed));
                }
            }
        }

        Vec3 movement = getDeltaMovement();
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);

        if (hitResult.getType() != HitResult.Type.MISS) {
            onHit(hitResult);
            return;
        }

        setPos(getX() + movement.x, getY() + movement.y, getZ() + movement.z);

        // Update orientation facing movement
        double horizDist = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
        setYRot((float) (Mth.atan2(movement.x, movement.z) * (180.0 / Math.PI)));
        setXRot((float) (Mth.atan2(movement.y, horizDist) * (180.0 / Math.PI)));
        this.yRotO = getYRot();
        this.xRotO = getXRot();

        // Trail particles on client
        if (level().isClientSide) {
            boolean isPurple = getColorTheme() == 1;
            for (int i = 0; i < 2; i++) {
                double px = getX() - movement.x * (i * 0.4);
                double py = getY() - movement.y * (i * 0.4);
                double pz = getZ() - movement.z * (i * 0.4);
                if (isPurple) {
                    level().addParticle(ParticleTypes.WITCH, px, py, pz, 0, 0, 0);
                } else {
                    level().addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 0, 0, 0);
                    if (i == 0) {
                        level().addParticle(ParticleTypes.END_ROD, px, py, pz, 0, 0, 0);
                    }
                }
            }
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (isImpactStopped()) return;

        Vec3 hitPos = hitResult.getLocation();
        boolean isPurple = getColorTheme() == 1;

        if (!level().isClientSide) {
            stopOnImpact(hitPos);

            if (level() instanceof ServerLevel serverLevel) {
                // Authentic frierenvoid rapid impact particles:
                serverLevel.sendParticles(
                    isPurple ? ParticleTypes.SMOKE : ParticleTypes.END_ROD,
                    hitPos.x, hitPos.y, hitPos.z,
                    9, 0.18, 0.18, 0.18, 0.15
                );

                if (isPurple) {
                    serverLevel.sendParticles(ParticleTypes.WITCH, hitPos.x, hitPos.y, hitPos.z, 10, 0.2, 0.2, 0.2, 0.05);
                } else {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, hitPos.x, hitPos.y, hitPos.z, 12, 0.22, 0.22, 0.22, 0.08);
                }

                if (hitResult instanceof BlockHitResult blockHit) {
                    BlockState state = serverLevel.getBlockState(blockHit.getBlockPos());
                    if (!state.isAir()) {
                        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                            hitPos.x, hitPos.y, hitPos.z, 16, 0.2, 0.2, 0.2, 0.2);
                    }
                }
            }

            // Authentic frierenvoid rapid impact audio:
            float pitch = isPurple ? 0.85f : (1.12f + random.nextFloat() * 0.06f);
            level().playSound(null, hitPos.x, hitPos.y, hitPos.z,
                ModCinematicSounds.ZOLTRAAK_IMPACT.get(), SoundSource.PLAYERS, 0.75f, pitch);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (isImpactStopped()) return;

        Entity target = entityHitResult.getEntity();
        Entity shooter = getOwner();

        if (target instanceof LivingEntity livingTarget && !level().isClientSide) {
            boolean isPurple = getColorTheme() == 1;
            boolean damaged;

            if (shooter instanceof LivingEntity livingShooter) {
                net.minecraft.world.damagesource.DamageSource source = isPurple
                        ? ModCinematicSpells.CORRUPTED_BARRAGE.get().getDamageSource(this, livingShooter)
                        : ModCinematicSpells.ZOLTRAAK_BARRAGE.get().getDamageSource(this, livingShooter);
                damaged = ZoltraakDamage.apply(livingTarget, damage, source, true);
            } else {
                damaged = livingTarget.hurt(damageSources().magic(), damage);
            }

            // Stagger effect and knockback: cancel item use & disable shield when attack is effective
            if (damaged || shooter == null || !DamageSources.isFriendlyFireBetween(shooter, livingTarget)) {
                if (livingTarget.isUsingItem()) {
                    livingTarget.stopUsingItem();
                }
                if (livingTarget instanceof Player p && p.isBlocking()) {
                    p.disableShield();
                }

                // Knockback impulse in movement direction
                Vec3 move = getDeltaMovement().normalize().scale(0.35);
                livingTarget.push(move.x, 0.12, move.z);
                livingTarget.hurtMarked = true;

                // Extra human-killing debuff
                if (isPurple) {
                    livingTarget.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                    livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
                }
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (isImpactStopped()) {
            return false;
        }
        Entity owner = getOwner();
        if (target == null || target == owner || !target.isAlive() || target.isSpectator()) {
            return false;
        }
        if (target instanceof ArmorStand) {
            return false;
        }
        if (target instanceof Player p && (p.isCreative() || (owner instanceof Player op && !op.canHarmPlayer(p)))) {
            return false;
        }
        if (owner != null && DamageSources.isFriendlyFireBetween(owner, target)) {
            return false;
        }
        return super.canHitEntity(target);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("Damage", this.damage);
        compound.putInt("ColorTheme", getColorTheme());
        compound.putInt("CircleIndex", getCircleIndex());
        compound.putInt("OwnerId", this.entityData.get(DATA_OWNER_ID));
        compound.putInt("TargetId", this.entityData.get(DATA_TARGET_ID));
        compound.putFloat("DoneAge", this.entityData.get(DATA_DONE_AGE));
        compound.putBoolean("ImpactStopped", this.entityData.get(DATA_IMPACT_STOPPED));
        if (this.spawnOrigin != null) {
            compound.putDouble("OriginX", this.spawnOrigin.x);
            compound.putDouble("OriginY", this.spawnOrigin.y);
            compound.putDouble("OriginZ", this.spawnOrigin.z);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.damage = compound.getFloat("Damage");
        setColorTheme(compound.getInt("ColorTheme"));
        setCircleIndex(compound.getInt("CircleIndex"));
        if (compound.contains("OwnerId")) {
            this.entityData.set(DATA_OWNER_ID, compound.getInt("OwnerId"));
        }
        if (compound.contains("TargetId")) {
            this.entityData.set(DATA_TARGET_ID, compound.getInt("TargetId"));
        }
        if (compound.contains("DoneAge")) {
            this.entityData.set(DATA_DONE_AGE, compound.getFloat("DoneAge"));
        }
        if (compound.contains("ImpactStopped")) {
            this.entityData.set(DATA_IMPACT_STOPPED, compound.getBoolean("ImpactStopped"));
        }
        if (compound.contains("OriginX")) {
            this.spawnOrigin = new Vec3(compound.getDouble("OriginX"), compound.getDouble("OriginY"), compound.getDouble("OriginZ"));
        }
    }
}
