package com.frierenflight.zoltraakcinematic.entity;

import com.frierenflight.zoltraakcinematic.client.renderer.IZoltraakVisualEntity;
import com.frierenflight.zoltraakcinematic.client.renderer.ZoltraakMode;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicEntities;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSchools;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSounds;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSpells;
import net.minecraft.sounds.SoundEvents;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.frierenflight.zoltraakcinematic.ZoltraakDamage;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ZoltraakCinematicBeamEntity extends Entity implements IZoltraakVisualEntity {
    private static final EntityDataAccessor<Float> DATA_LENGTH =
            SynchedEntityData.defineId(ZoltraakCinematicBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(ZoltraakCinematicBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COLOR_THEME =
            SynchedEntityData.defineId(ZoltraakCinematicBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(ZoltraakCinematicBeamEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DID_IMPACT =
            SynchedEntityData.defineId(ZoltraakCinematicBeamEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_IMPACT_DIST =
            SynchedEntityData.defineId(ZoltraakCinematicBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_IMPACT_NORMAL_X =
            SynchedEntityData.defineId(ZoltraakCinematicBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_IMPACT_NORMAL_Y =
            SynchedEntityData.defineId(ZoltraakCinematicBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_IMPACT_NORMAL_Z =
            SynchedEntityData.defineId(ZoltraakCinematicBeamEntity.class, EntityDataSerializers.FLOAT);

    public static final int LIFETIME = 46; // Full 2.3 seconds duration
    public static final int FIRE_TICK = 14; // Exact 0.70 seconds charge matching frierenvoid

    private LivingEntity owner;
    private UUID ownerUUID;
    private float damage = 35.0f;
    private float maxRange = 64.0f;
    private boolean soundPlayed = false;
    private boolean barrierAbsorbed = false;
    private final Set<Integer> hitEntityIds = new HashSet<>();

    public ZoltraakCinematicBeamEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public ZoltraakCinematicBeamEntity(Level level, LivingEntity owner, float damage, float maxRange) {
        this(level, owner, damage, maxRange, 0);
    }

    public ZoltraakCinematicBeamEntity(Level level, LivingEntity owner, float damage, float maxRange, int colorTheme) {
        this(ModCinematicEntities.ZOLTRAAK_BEAM.get(), level);
        this.owner = owner;
        if (owner != null) {
            this.ownerUUID = owner.getUUID();
        }
        this.damage = damage;
        this.maxRange = maxRange;
        this.entityData.set(DATA_LENGTH, maxRange);
        this.entityData.set(DATA_OWNER_ID, owner != null ? owner.getId() : -1);
        this.entityData.set(DATA_COLOR_THEME, colorTheme);
        this.entityData.set(DATA_MODE, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LENGTH, 64.0f);
        builder.define(DATA_OWNER_ID, -1);
        builder.define(DATA_COLOR_THEME, 0);
        builder.define(DATA_MODE, 0);
        builder.define(DATA_DID_IMPACT, false);
        builder.define(DATA_IMPACT_DIST, 64.0f);
        builder.define(DATA_IMPACT_NORMAL_X, 0.0f);
        builder.define(DATA_IMPACT_NORMAL_Y, 1.0f);
        builder.define(DATA_IMPACT_NORMAL_Z, 0.0f);
    }

    public float getImpactDist() {
        return this.entityData.get(DATA_IMPACT_DIST);
    }

    public Vec3 getImpactNormal() {
        return new Vec3(
            this.entityData.get(DATA_IMPACT_NORMAL_X),
            this.entityData.get(DATA_IMPACT_NORMAL_Y),
            this.entityData.get(DATA_IMPACT_NORMAL_Z)
        );
    }

    public int getColorTheme() {
        return this.entityData.get(DATA_COLOR_THEME);
    }

    public void setColorTheme(int theme) {
        this.entityData.set(DATA_COLOR_THEME, theme);
    }

    @Override
    public ZoltraakMode mode() {
        return ZoltraakMode.from(this.entityData.get(DATA_MODE));
    }

    public void setMode(ZoltraakMode mode) {
        this.entityData.set(DATA_MODE, mode.ordinal());
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
        float a = rawAge(partial);
        if (mode() == ZoltraakMode.LARGE) {
            if (a < 20.0F) {
                return Mth.clamp(a * 14.0F / 20.0F, 0.0F, 14.0F);
            } else {
                return a < 100.0F ? 14.0F + (a - 20.0F) * 9.0F / 80.0F : Mth.clamp(23.0F + (a - 100.0F) * 23.0F / 26.0F, 23.0F, 46.0F);
            }
        } else {
            return Mth.clamp(a < (float)mode().charge ? a * 14.0F / (float)mode().charge : 14.0F + (a - (float)mode().charge) * 9.0F / (float)mode().pulse, 0.0F, 46.0F);
        }
    }

    @Override
    public Vec3 visualOrigin(float partial) {
        if (!fired() && owner != null && owner.isAlive()) {
            return getStaffAnchorPos(owner, partial);
        }
        return getPosition(partial);
    }

    @Override
    public Vec3 visualDirection(float partial) {
        if (!fired() && owner != null && owner.isAlive()) {
            return owner.getViewVector(partial).normalize();
        }
        return Vec3.directionFromRotation(getXRot(), getYRot()).normalize();
    }

    @Override
    public int casterId() {
        return owner != null ? owner.getId() : this.entityData.get(DATA_OWNER_ID);
    }

    @Override
    public double length() {
        return (double) getBeamLength();
    }

    @Override
    public float scale() {
        return mode().scale;
    }

    @Override
    public boolean fired() {
        return (float) this.tickCount >= (float) mode().charge;
    }

    @Override
    public boolean impact() {
        return this.entityData.get(DATA_DID_IMPACT);
    }

    @Override
    public float impactAge() {
        float dist = impact() ? getImpactDist() : (float) length();
        return mode() == ZoltraakMode.LARGE ? largeImpactTime() : 14.0F + (dist / 12.0F);
    }

    @Override
    public float largeImpactTime() {
        float dist = impact() ? getImpactDist() : (float) length();
        return 20.0F + (dist / 2.5F);
    }

    @Override
    public Vec3 end() {
        if (impact()) {
            return visualOrigin(1.0f).add(visualDirection(1.0f).scale((double) getImpactDist()));
        }
        return visualOrigin(1.0f).add(visualDirection(1.0f).scale(length()));
    }

    @Override
    public Vec3 normal() {
        if (impact()) {
            Vec3 n = getImpactNormal();
            if (n.lengthSqr() > 0.001) {
                return n.normalize();
            }
        }
        return visualDirection(1.0f).scale(-1.0);
    }

    @Override
    public double beamFront(float partial) {
        return Math.min(length(), Math.max(0.0, mode() == ZoltraakMode.LARGE ? (rawAge(partial) - 20.0F) * 2.5 : (age(partial) - 14.0F) * 12.0));
    }

    @Override
    public float guidedFade(float partial) {
        return 1.0f;
    }

    @Override
    public List<Vec3> guidedPath(float partial) {
        return Collections.emptyList();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    public float getBeamLength() {
        return this.entityData.get(DATA_LENGTH);
    }

    public LivingEntity getOwner() {
        return this.owner;
    }

    public static Vec3 getStaffAnchorPos(LivingEntity caster, float partial) {
        Vec3 eyePos = caster.getEyePosition(partial);
        Vec3 look = caster.getViewVector(partial);
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = look.cross(up);
        if (right.lengthSqr() < 0.001) {
            right = new Vec3(1, 0, 0);
        } else {
            right = right.normalize();
        }
        return eyePos.add(right.scale(0.32)).add(0, -0.22, 0).add(look.scale(1.25));
    }

    public static Vec3 getStaffAnchorPos(LivingEntity caster) {
        return getStaffAnchorPos(caster, 1.0f);
    }

    @Override
    public void tick() {
        super.tick();

        int maxLifetime = mode() == ZoltraakMode.LARGE ? 110 : LIFETIME;
        if (tickCount >= maxLifetime) {
            discard();
            return;
        }

        if (owner == null && level().isClientSide) {
            int ownerId = this.entityData.get(DATA_OWNER_ID);
            if (ownerId != -1 && level().getEntity(ownerId) instanceof LivingEntity living) {
                this.owner = living;
            }
        }

        if (owner != null && owner.isAlive() && !fired()) {
            Vec3 staffPos = getStaffAnchorPos(owner);
            setPos(staffPos.x, staffPos.y, staffPos.z);
            setYRot(owner.getYRot());
            setXRot(owner.getXRot());
        }

        Vec3 start = position();
        Vec3 look = Vec3.directionFromRotation(getXRot(), getYRot());

        float currentLength = getBeamLength();
        Vec3 end = start.add(look.scale(currentLength));
        setBoundingBox(new AABB(start, end).inflate(5.25));

        if (tickCount == 1) {
            float pitch = black() ? 0.75f : 1.0f;
            level().playSound(null, getX(), getY(), getZ(), ModCinematicSounds.ZOLTRAAK_CHARGE.get(), SoundSource.PLAYERS, 1.4f, pitch);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.2f, 0.85f * pitch);
        }

        int fireTick = mode().charge;
        if (level().isClientSide) {
            if (tickCount >= fireTick && tickCount <= fireTick + mode().pulse) {
                float len = getBeamLength();

                // Ground physical debris tracer
                for (float d = 2.5f; d < len; d += 1.8f) {
                    Vec3 bpWorld = start.add(look.scale(d));
                    for (double dy = 0.2; dy >= -2.4; dy -= 0.6) {
                        BlockPos checkBp = BlockPos.containing(bpWorld.x, bpWorld.y + dy, bpWorld.z);
                        BlockState bs = level().getBlockState(checkBp);
                        if (!bs.isAir() && bs.isSolidRender(level(), checkBp)) {
                            double groundY = checkBp.getY() + 1.0;
                            double beamHeightAboveGround = bpWorld.y - groundY;

                            if (beamHeightAboveGround >= -0.5 && beamHeightAboveGround <= 2.2) {
                                if (random.nextFloat() < 0.45f) {
                                    level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, bs),
                                            bpWorld.x + (random.nextDouble() - 0.5) * 0.65,
                                            groundY + 0.1,
                                            bpWorld.z + (random.nextDouble() - 0.5) * 0.65,
                                            (random.nextDouble() - 0.5) * 0.32,
                                            0.22 + random.nextDouble() * 0.30,
                                            (random.nextDouble() - 0.5) * 0.32);
                                }
                                if (random.nextFloat() < 0.35f) {
                                    level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                            bpWorld.x + (random.nextDouble() - 0.5) * 0.4,
                                            groundY + 0.15,
                                            bpWorld.z + (random.nextDouble() - 0.5) * 0.4,
                                            (random.nextDouble() - 0.5) * 0.08,
                                            0.06 + random.nextDouble() * 0.08,
                                            (random.nextDouble() - 0.5) * 0.08);
                                }
                                if (random.nextFloat() < 0.25f) {
                                    level().addParticle(black() ? ParticleTypes.PORTAL : ParticleTypes.ELECTRIC_SPARK,
                                            bpWorld.x, groundY + 0.1, bpWorld.z,
                                            (random.nextDouble() - 0.5) * 0.2, 0.15, (random.nextDouble() - 0.5) * 0.2);
                                }
                            }
                            break;
                        }
                    }
                }
            }
            return;
        }

        if (tickCount >= fireTick && tickCount <= fireTick + mode().pulse) {
            if (!soundPlayed) {
                soundPlayed = true;

                float actualLength = calculatePiercingDistance(start, look, maxRange);
                this.entityData.set(DATA_LENGTH, actualLength);
                currentLength = actualLength;
                end = start.add(look.scale(currentLength));

                boolean isLarge = mode() == ZoltraakMode.LARGE;
                float boomPitch = isLarge ? 1.05f : (black() ? 1.25f : 1.45f);
                float lightningPitch = isLarge ? 1.4f : (black() ? 1.6f : 1.9f);

                // Origin / Caster discharge sounds
                level().playSound(null, getX(), getY(), getZ(),
                        (isLarge ? ModCinematicSounds.ZOLTRAAK_GREAT_FIRE.get() : ModCinematicSounds.ZOLTRAAK_FIRE.get()),
                        SoundSource.PLAYERS, isLarge ? 2.6f : 1.8f,
                        isLarge ? (black() ? 0.9f : 1.0f) : (black() ? 0.78f : 1.0f));
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, isLarge ? 2.5f : 1.8f, boomPitch);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, isLarge ? 1.8f : 1.3f, lightningPitch);

                boolean didImpact = this.entityData.get(DATA_DID_IMPACT);
                float impactDist = this.entityData.get(DATA_IMPACT_DIST);
                Vec3 impactPos = start.add(look.scale(impactDist));

                // Target impact explosion sounds ONLY if an obstacle was hit
                if (didImpact) {
                    level().playSound(null, impactPos.x, impactPos.y, impactPos.z,
                            ModCinematicSounds.ZOLTRAAK_IMPACT.get(),
                            SoundSource.PLAYERS, isLarge ? 2.5f : 1.3f,
                            (black() ? 0.75f : 1.0f) * (isLarge ? 0.65f : 1.0f));
                    level().playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, isLarge ? 3.0f : 2.0f, isLarge ? 0.9f : 1.15f);
                    level().playSound(null, impactPos.x, impactPos.y, impactPos.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, isLarge ? 2.5f : 1.8f, isLarge ? 0.95f : 1.25f);
                }

                if (owner != null) {
                    Vec3 recoil = look.scale(mode() == ZoltraakMode.LARGE ? -0.32 : -0.16);
                    owner.push(recoil.x, 0.04, recoil.z);
                    owner.hurtMarked = true;
                }

                if (level() instanceof ServerLevel serverLevel) {

                    if (didImpact) {
                        double blastRadius = mode() == ZoltraakMode.LARGE ? 21.0 : 12.0;
                        AABB blastBox = new AABB(impactPos.x - blastRadius, impactPos.y - blastRadius, impactPos.z - blastRadius,
                                impactPos.x + blastRadius, impactPos.y + blastRadius, impactPos.z + blastRadius);
                        List<LivingEntity> blastTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, blastBox,
                                e -> e != owner && e.isAlive() && (owner == null || !DamageSources.isFriendlyFireBetween(owner, e))
                        );

                        float directBeamDamage = damage * (mode() == ZoltraakMode.LARGE ? 2.0f : 1.0f) * (float) com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.getBeamDamageMultiplier();
                        net.minecraft.world.damagesource.DamageSource spellSource = getSpellDamageSource();

                        for (LivingEntity target : blastTargets) {
                            if (barrierAbsorbed) {
                                List<DefenseBarrierEntity> prot = serverLevel.getEntitiesOfClass(
                                        DefenseBarrierEntity.class, target.getBoundingBox().inflate(3.5),
                                        b -> b.active() && b.friendly(target));
                                if (!prot.isEmpty()) {
                                    continue;
                                }
                            }

                            double dist = target.position().distanceTo(impactPos);
                            if (dist <= blastRadius) {
                                if (hitEntityIds.contains(target.getId())) {
                                    continue; // Already took full direct core beam damage
                                }
                                hitEntityIds.add(target.getId());

                                float factor = (float) (1.0 - (dist / blastRadius));
                                float blastDmg = Math.max(12.0f, directBeamDamage * factor);

                                if (owner != null) {
                                    ZoltraakDamage.apply(target, blastDmg, spellSource, ModCinematicSchools.ORDINARY_MAGIC.get(), true);
                                } else {
                                    target.hurt(damageSources().magic(), blastDmg);
                                }

                                if (black()) {
                                    int witherTicks = com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.getCorruptedWitherDuration();
                                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, witherTicks, 1));
                                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                                }

                                if (com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.isDisableShieldOnHit() && target.isBlocking() && target instanceof Player p) {
                                    p.disableShield();
                                }

                                Vec3 knockVec = target.position().subtract(impactPos);
                                if (knockVec.lengthSqr() > 0.001) {
                                    double knockFactor = com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.getBeamKnockbackFactor();
                                    knockVec = knockVec.normalize().scale(Math.max(0.4, factor * knockFactor));
                                    target.push(knockVec.x, factor * 0.45, knockVec.z);
                                    target.hurtMarked = true;
                                }
                            }
                        }

                        // Impact explosion particles at the real obstacle contact point
                        if (black()) {
                            serverLevel.sendParticles(ParticleTypes.WITCH, impactPos.x, impactPos.y, impactPos.z, 28, 1.2, 1.2, 1.2, 0.35);
                            serverLevel.sendParticles(ParticleTypes.SMOKE, impactPos.x, impactPos.y, impactPos.z, 20, 0.8, 0.8, 0.8, 0.08);
                            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, impactPos.x, impactPos.y, impactPos.z, 10, 0.6, 0.6, 0.6, 0.05);
                        } else {
                            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, impactPos.x, impactPos.y, impactPos.z, 36, 1.2, 1.2, 1.2, 0.45);
                            serverLevel.sendParticles(ParticleTypes.POOF, impactPos.x, impactPos.y, impactPos.z, 16, 0.8, 0.8, 0.8, 0.12);
                            serverLevel.sendParticles(ParticleTypes.FLASH, impactPos.x, impactPos.y, impactPos.z, 1, 0.0, 0.0, 0.0, 0.0);
                        }
                    }

                    // 1:1 Anime Piercing Borehole (เจาะบล็อกเป็นรูทรงกระบอกตามแนวลำแสง)
                    boolean canGrief = serverLevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
                    if (!barrierAbsorbed && canGrief && com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.isBeamDrillBlocks()) {
                        double drillR = com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.getBeamDrillRadius()
                                * (mode() == ZoltraakMode.LARGE ? 1.85 : 1.0);
                        int rInt = (int) Math.ceil(drillR);
                        float stepD = 0.85f;
                        Set<BlockPos> drilledPositions = new HashSet<>();

                        for (float d = 0.5f; d < currentLength; d += stepD) {
                            Vec3 rayPoint = start.add(look.scale(d));
                            BlockPos centerBp = BlockPos.containing(rayPoint);

                            for (int dx = -rInt; dx <= rInt; dx++) {
                                for (int dy = -rInt; dy <= rInt; dy++) {
                                    for (int dz = -rInt; dz <= rInt; dz++) {
                                        Vec3 checkV = new Vec3(centerBp.getX() + dx + 0.5, centerBp.getY() + dy + 0.5, centerBp.getZ() + dz + 0.5);
                                        Vec3 diff = checkV.subtract(start);
                                        double proj = diff.dot(look);
                                        if (proj >= 0.0 && proj <= currentLength) {
                                            Vec3 closestOnRay = start.add(look.scale(proj));
                                            if (checkV.distanceToSqr(closestOnRay) <= drillR * drillR) {
                                                BlockPos bp = centerBp.offset(dx, dy, dz);
                                                if (drilledPositions.add(bp)) {
                                                    BlockState state = serverLevel.getBlockState(bp);
                                                    if (!state.isAir() && state.getDestroySpeed(serverLevel, bp) >= 0.0f && state.getDestroySpeed(serverLevel, bp) < 55.0f) {
                                                        if (random.nextFloat() < 0.20f) {
                                                            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                                                                    bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.15);
                                                        }
                                                        serverLevel.setBlock(bp, Blocks.AIR.defaultBlockState(), 3);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Piercing beam active raycast damage (Full Base Power dealt once per pierced entity)
            if (level() instanceof ServerLevel serverLevel) {
                float beamDamage = damage * (mode() == ZoltraakMode.LARGE ? 2.0f : 1.0f) * (float) com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.getBeamDamageMultiplier();
                Vec3 pStart = start;
                Vec3 pEnd = start.add(look.scale(getBeamLength()));
                AABB beamBox = new AABB(pStart, pEnd).inflate(mode() == ZoltraakMode.LARGE ? 5.25 : 2.25);
                boolean respectFF = com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.isRespectFriendlyFire();
                List<LivingEntity> beamTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, beamBox,
                        e -> e != owner && e.isAlive() && (owner == null || !respectFF || !DamageSources.isFriendlyFireBetween(owner, e))
                );

                net.minecraft.world.damagesource.DamageSource spellSource = getSpellDamageSource();

                for (LivingEntity target : beamTargets) {
                    if (hitEntityIds.contains(target.getId())) {
                        continue;
                    }

                    Vec3 toTarget = target.getBoundingBox().getCenter().subtract(pStart);
                    double proj = toTarget.dot(look);
                    if (proj >= 0 && proj <= getBeamLength()) {
                        Vec3 closest = pStart.add(look.scale(proj));
                        if (target.getBoundingBox().distanceToSqr(closest) <= (mode() == ZoltraakMode.LARGE ? 20.25 : 4.05)) {
                            hitEntityIds.add(target.getId());

                            if (owner != null) {
                                ZoltraakDamage.apply(target, beamDamage, spellSource, ModCinematicSchools.ORDINARY_MAGIC.get(), true);
                            } else {
                                target.hurt(damageSources().magic(), beamDamage);
                            }

                            if (black()) {
                                int witherTicks = com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.getCorruptedWitherDuration();
                                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, witherTicks, 1));
                                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                            }

                            if (com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.isDisableShieldOnHit() && target.isBlocking() && target instanceof Player p) {
                                p.disableShield();
                            }

                            Vec3 pushVec = look.normalize().scale(mode() == ZoltraakMode.LARGE ? 1.5 : 0.8);
                            target.push(pushVec.x, 0.25, pushVec.z);
                            target.hurtMarked = true;
                        }
                    }
                }
            }
        }
    }

    private net.minecraft.world.damagesource.DamageSource getSpellDamageSource() {
        if (owner != null) {
            return black()
                    ? ModCinematicSpells.CORRUPTED_ZOLTRAAK.get().getDamageSource(this, owner)
                    : ModCinematicSpells.ZOLTRAAK.get().getDamageSource(this, owner);
        }
        return damageSources().magic();
    }

    private float calculatePiercingDistance(Vec3 start, Vec3 look, float maxDist) {
        Vec3 end = start.add(look.scale(maxDist));

        // 1. Check Defense Barriers
        List<DefenseBarrierEntity> barriers = level().getEntitiesOfClass(
                DefenseBarrierEntity.class,
                new AABB(start, end).inflate(3.0),
                b -> b.active() && !b.friendly(owner)
        );

        float barrierHitDist = Float.MAX_VALUE;
        DefenseBarrierEntity hitBarrier = null;
        Vec3 barrierContact = null;

        for (DefenseBarrierEntity barrier : barriers) {
            Optional<Vec3> hit = barrier.intersection(start, end);
            if (hit.isPresent()) {
                float dist = (float) hit.get().distanceTo(start);
                if (dist < barrierHitDist) {
                    barrierHitDist = dist;
                    hitBarrier = barrier;
                    barrierContact = hit.get();
                }
            }
        }

        // 2. Check Solid Blocks using Minecraft native voxel raycaster
        ClipContext clipContext = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this);
        BlockHitResult blockHit = level().clip(clipContext);

        float blockHitDist = Float.MAX_VALUE;
        Vec3 blockHitPos = null;
        Vec3 blockNormal = null;
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            blockHitPos = blockHit.getLocation();
            blockHitDist = (float) start.distanceTo(blockHitPos);
            Direction dir = blockHit.getDirection();
            blockNormal = new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ());
        }

        // 3. Determine first contact surface
        boolean hitAnyObstacle = false;
        float firstHitDist = maxDist;
        Vec3 firstHitNormal = look.scale(-1.0);

        if (hitBarrier != null && barrierHitDist <= blockHitDist) {
            hitAnyObstacle = true;
            firstHitDist = barrierHitDist;
            Vec3 bNorm = hitBarrier.normal();
            firstHitNormal = (bNorm != null && bNorm.lengthSqr() > 0.001) ? bNorm : look.scale(-1.0);

            float beamDamage = this.damage * (mode() == ZoltraakMode.LARGE ? 2.0f : 1.0f);
            float absorbed = hitBarrier.absorb(beamDamage, barrierContact);
            if (absorbed >= beamDamage || hitBarrier.active()) {
                this.barrierAbsorbed = true;
            }

            this.entityData.set(DATA_DID_IMPACT, true);
            this.entityData.set(DATA_IMPACT_DIST, firstHitDist);
            this.entityData.set(DATA_IMPACT_NORMAL_X, (float) firstHitNormal.x);
            this.entityData.set(DATA_IMPACT_NORMAL_Y, (float) firstHitNormal.y);
            this.entityData.set(DATA_IMPACT_NORMAL_Z, (float) firstHitNormal.z);

            return firstHitDist;
        } else if (blockHitPos != null && blockHitDist < maxDist) {
            hitAnyObstacle = true;
            firstHitDist = blockHitDist;
            firstHitNormal = blockNormal;
        }

        if (!hitAnyObstacle) {
            // Empty sky / air - clean laser traversal with NO fake impact
            this.entityData.set(DATA_DID_IMPACT, false);
            this.entityData.set(DATA_IMPACT_DIST, maxDist);
            this.entityData.set(DATA_IMPACT_NORMAL_X, (float) -look.x);
            this.entityData.set(DATA_IMPACT_NORMAL_Y, (float) -look.y);
            this.entityData.set(DATA_IMPACT_NORMAL_Z, (float) -look.z);
            return maxDist;
        }

        // Real block obstacle was struck:
        this.entityData.set(DATA_DID_IMPACT, true);
        this.entityData.set(DATA_IMPACT_DIST, firstHitDist);
        this.entityData.set(DATA_IMPACT_NORMAL_X, (float) firstHitNormal.x);
        this.entityData.set(DATA_IMPACT_NORMAL_Y, (float) firstHitNormal.y);
        this.entityData.set(DATA_IMPACT_NORMAL_Z, (float) firstHitNormal.z);

        boolean canDrill = com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.isBeamDrillBlocks();
        if (level() instanceof ServerLevel sl) {
            canDrill = canDrill && sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        }

        if (!canDrill) {
            return firstHitDist;
        }

        // In drilling mode, find how far the beam penetrates through destructible blocks
        float step = 0.5f;
        for (float d = firstHitDist; d < maxDist; d += step) {
            Vec3 checkPos = start.add(look.scale(d));
            BlockPos bp = BlockPos.containing(checkPos);
            BlockState bs = level().getBlockState(bp);
            if (!bs.isAir() && bs.blocksMotion()) {
                if (bs.getDestroySpeed(level(), bp) < 0.0f || bs.getDestroySpeed(level(), bp) >= 55.0f) {
                    return d;
                }
            }
        }

        return maxDist;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.damage = compound.getFloat("Damage");
        this.maxRange = compound.getFloat("MaxRange");
        setColorTheme(compound.getInt("ColorTheme"));
        setMode(ZoltraakMode.from(compound.getInt("Mode")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putFloat("Damage", this.damage);
        compound.putFloat("MaxRange", this.maxRange);
        compound.putInt("ColorTheme", getColorTheme());
        compound.putInt("Mode", mode().ordinal());
    }
}
