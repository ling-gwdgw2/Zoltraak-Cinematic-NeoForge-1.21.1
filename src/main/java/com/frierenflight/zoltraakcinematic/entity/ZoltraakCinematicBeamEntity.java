package com.frierenflight.zoltraakcinematic.entity;

import com.frierenflight.zoltraakcinematic.client.renderer.IZoltraakVisualEntity;
import com.frierenflight.zoltraakcinematic.client.renderer.ZoltraakMode;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicEntities;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSounds;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSpells;
import net.minecraft.sounds.SoundEvents;
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
        return true;
    }

    @Override
    public float impactAge() {
        return mode() == ZoltraakMode.LARGE ? largeImpactTime() : 14.0F + (float)(length() / 12.0);
    }

    @Override
    public float largeImpactTime() {
        return 20.0F + (float)(length() / 2.5);
    }

    @Override
    public Vec3 end() {
        return visualOrigin(1.0f).add(visualDirection(1.0f).scale(length()));
    }

    @Override
    public Vec3 normal() {
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

                // Target impact explosion sounds
                level().playSound(null, end.x, end.y, end.z,
                        ModCinematicSounds.ZOLTRAAK_IMPACT.get(),
                        SoundSource.PLAYERS, isLarge ? 2.5f : 1.3f,
                        (black() ? 0.75f : 1.0f) * (isLarge ? 0.65f : 1.0f));
                level().playSound(null, end.x, end.y, end.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, isLarge ? 3.0f : 2.0f, isLarge ? 0.9f : 1.15f);
                level().playSound(null, end.x, end.y, end.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, isLarge ? 2.5f : 1.8f, isLarge ? 0.95f : 1.25f);

                if (owner != null) {
                    Vec3 recoil = look.scale(mode() == ZoltraakMode.LARGE ? -0.32 : -0.16);
                    owner.push(recoil.x, 0.04, recoil.z);
                    owner.hurtMarked = true;
                }

                if (level() instanceof ServerLevel serverLevel) {

                    double blastRadius = mode() == ZoltraakMode.LARGE ? 21.0 : 12.0;
                    AABB blastBox = new AABB(end.x - blastRadius, end.y - blastRadius, end.z - blastRadius,
                            end.x + blastRadius, end.y + blastRadius, end.z + blastRadius);
                    List<LivingEntity> blastTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, blastBox,
                            e -> e != owner && e.isAlive() && (owner == null || !DamageSources.isFriendlyFireBetween(owner, e))
                    );

                    float directBeamDamage = damage * (mode() == ZoltraakMode.LARGE ? 2.0f : 1.0f);
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

                        double dist = target.position().distanceTo(end);
                        if (dist <= blastRadius) {
                            if (hitEntityIds.contains(target.getId())) {
                                continue; // Already took full direct core beam damage
                            }
                            hitEntityIds.add(target.getId());

                            float factor = (float) (1.0 - (dist / blastRadius));
                            float blastDmg = Math.max(12.0f, directBeamDamage * factor);

                            if (owner != null) {
                                ZoltraakDamage.apply(target, blastDmg, spellSource, true);
                            } else {
                                target.hurt(damageSources().magic(), blastDmg);
                            }

                            if (black()) {
                                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 120, 1));
                                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                            }

                            if (target.isBlocking() && target instanceof Player p) {
                                p.disableShield();
                            }

                            Vec3 knockVec = target.position().subtract(end);
                            if (knockVec.lengthSqr() > 0.001) {
                                knockVec = knockVec.normalize().scale(Math.max(0.4, factor * 1.5));
                                target.push(knockVec.x, factor * 0.45, knockVec.z);
                                target.hurtMarked = true;
                            }
                        }
                    }

                    if (!barrierAbsorbed && serverLevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                        int craterR = mode() == ZoltraakMode.LARGE ? 7 : 4;
                        BlockPos centerPos = BlockPos.containing(end);
                        for (int x = -craterR; x <= craterR; x++) {
                            for (int y = -craterR; y <= craterR; y++) {
                                for (int z = -craterR; z <= craterR; z++) {
                                    if (x * x + y * y + z * z <= craterR * craterR) {
                                        BlockPos bp = centerPos.offset(x, y, z);
                                        BlockState state = serverLevel.getBlockState(bp);
                                        if (!state.isAir() && state.getDestroySpeed(serverLevel, bp) >= 0 && state.getDestroySpeed(serverLevel, bp) < 45.0f) {
                                            serverLevel.setBlock(bp, Blocks.AIR.defaultBlockState(), 3);
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
                float beamDamage = damage * (mode() == ZoltraakMode.LARGE ? 2.0f : 1.0f);
                Vec3 pStart = start;
                Vec3 pEnd = start.add(look.scale(getBeamLength()));
                AABB beamBox = new AABB(pStart, pEnd).inflate(mode() == ZoltraakMode.LARGE ? 5.25 : 2.25);
                List<LivingEntity> beamTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, beamBox,
                        e -> e != owner && e.isAlive() && (owner == null || !DamageSources.isFriendlyFireBetween(owner, e))
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
                                ZoltraakDamage.apply(target, beamDamage, spellSource, true);
                            } else {
                                target.hurt(damageSources().magic(), beamDamage);
                            }

                            if (black()) {
                                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 120, 1));
                                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                            }

                            if (target.isBlocking() && target instanceof Player p) {
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
        List<DefenseBarrierEntity> barriers = level().getEntitiesOfClass(
                DefenseBarrierEntity.class,
                new AABB(start, end).inflate(3.0),
                b -> b.active() && !b.friendly(owner)
        );

        float barrierHitDist = maxDist;
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

        if (hitBarrier != null && barrierContact != null) {
            float beamDamage = this.damage * (mode() == ZoltraakMode.LARGE ? 2.0f : 1.0f);
            float absorbed = hitBarrier.absorb(beamDamage, barrierContact);
            if (absorbed >= beamDamage || hitBarrier.active()) {
                this.barrierAbsorbed = true;
            }
            return barrierHitDist;
        }

        float step = 0.5f;
        for (float d = 1.0f; d < maxDist; d += step) {
            Vec3 checkPos = start.add(look.scale(d));
            BlockPos bp = BlockPos.containing(checkPos);
            BlockState bs = level().getBlockState(bp);
            if (!bs.isAir() && bs.blocksMotion()) {
                return d;
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
