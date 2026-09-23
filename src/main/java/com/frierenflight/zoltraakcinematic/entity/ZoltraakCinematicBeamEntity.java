package com.frierenflight.zoltraakcinematic.entity;

import com.frierenflight.zoltraakcinematic.registry.ModCinematicEntities;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSpells;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

import java.util.List;
import java.util.UUID;

public class ZoltraakCinematicBeamEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_LENGTH =
            SynchedEntityData.defineId(ZoltraakCinematicBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(ZoltraakCinematicBeamEntity.class, EntityDataSerializers.INT);

    public static final int LIFETIME = 28; // 1.40 seconds total
    public static final int FIRE_TICK = 8;  // 0.40 seconds charge

    private LivingEntity owner;
    private UUID ownerUUID;
    private float damage = 35.0f;
    private float maxRange = 64.0f;
    private boolean soundPlayed = false;

    public ZoltraakCinematicBeamEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public ZoltraakCinematicBeamEntity(Level level, LivingEntity owner, float damage, float maxRange) {
        this(ModCinematicEntities.ZOLTRAAK_BEAM.get(), level);
        this.owner = owner;
        if (owner != null) {
            this.ownerUUID = owner.getUUID();
        }
        this.damage = damage;
        this.maxRange = maxRange;
        this.entityData.set(DATA_LENGTH, maxRange);
        this.entityData.set(DATA_OWNER_ID, owner != null ? owner.getId() : -1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LENGTH, 64.0f);
        builder.define(DATA_OWNER_ID, -1);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    public float getBeamLength() {
        return this.entityData.get(DATA_LENGTH);
    }

    public static Vec3 getStaffAnchorPos(LivingEntity caster) {
        Vec3 eyePos = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = look.cross(up);
        if (right.lengthSqr() < 0.001) {
            right = new Vec3(1, 0, 0);
        } else {
            right = right.normalize();
        }
        return eyePos.add(right.scale(0.32)).add(0, -0.22, 0).add(look.scale(1.25));
    }

    @Override
    public void tick() {
        super.tick();

        if (tickCount >= LIFETIME) {
            discard();
            return;
        }

        if (owner == null && level().isClientSide) {
            int ownerId = this.entityData.get(DATA_OWNER_ID);
            if (ownerId != -1 && level().getEntity(ownerId) instanceof LivingEntity living) {
                this.owner = living;
            }
        }

        if (owner != null && owner.isAlive() && tickCount < FIRE_TICK) {
            Vec3 staffPos = getStaffAnchorPos(owner);
            setPos(staffPos.x, staffPos.y, staffPos.z);
            setYRot(owner.getYRot());
            setXRot(owner.getXRot());
        }

        Vec3 start = position();
        Vec3 look = Vec3.directionFromRotation(getXRot(), getYRot());

        float currentLength = getBeamLength();
        Vec3 end = start.add(look.scale(currentLength));
        setBoundingBox(new AABB(start, end).inflate(2.5));

        if (tickCount == 1) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.3f, 1.8f);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9f, 1.5f);
        }

        if (level().isClientSide) {
            if (tickCount < FIRE_TICK) {
                Vec3 right = Vec3.directionFromRotation(0, getYRot() + 90);
                Vec3 up = Vec3.directionFromRotation(getXRot() - 90, getYRot());
                for (int i = 0; i < 3; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double dist = 0.6 + random.nextDouble() * 0.5;
                    double ox = Math.cos(angle) * dist;
                    double oy = Math.sin(angle) * dist;
                    Vec3 p = start.add(right.scale(ox)).add(up.scale(oy));
                    Vec3 vel = start.subtract(p).scale(0.25);
                    level().addParticle(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, vel.x, vel.y, vel.z);
                }
            } else if (tickCount >= FIRE_TICK && tickCount <= 19) {
                float len = getBeamLength();
                for (float d = 2.0f; d < len; d += 6.0f) {
                    double jitter = (random.nextDouble() - 0.5) * 0.3;
                    Vec3 pt = start.add(look.scale(d + random.nextDouble() * 2.0));
                    level().addParticle(ParticleTypes.ELECTRIC_SPARK, pt.x + jitter, pt.y + jitter, pt.z + jitter, 0, 0.01, 0);
                }
            } else if (tickCount > 19) {
                float len = getBeamLength();
                for (float d = 3.0f; d < len; d += 8.0f) {
                    Vec3 pt = start.add(look.scale(d + random.nextDouble() * 3.0));
                    double driftX = (random.nextDouble() - 0.5) * 0.03;
                    double driftY = -0.01 - random.nextDouble() * 0.02;
                    double driftZ = (random.nextDouble() - 0.5) * 0.03;
                    level().addParticle(ParticleTypes.END_ROD, pt.x, pt.y, pt.z, driftX, driftY, driftZ);
                }
            }
            return;
        }

        if (tickCount >= FIRE_TICK && tickCount <= 19) {
            if (!soundPlayed) {
                soundPlayed = true;

                float actualLength = calculatePiercingDistance(start, look, maxRange);
                this.entityData.set(DATA_LENGTH, actualLength);
                currentLength = actualLength;
                end = start.add(look.scale(currentLength));

                level().playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.8f, 1.5f);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.2f, 1.9f);
                level().playSound(null, end.x, end.y, end.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.5f, 1.1f);
                level().playSound(null, end.x, end.y, end.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0f, 1.3f);

                if (owner != null) {
                    Vec3 recoil = look.scale(-0.16);
                    owner.push(recoil.x, 0.04, recoil.z);
                    owner.hurtMarked = true;
                }

                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 6, 0.6, 0.6, 0.6, 0.0);
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 80, 4.0, 4.0, 4.0, 0.5);
                    serverLevel.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 40, 3.0, 3.0, 3.0, 0.2);

                    double blastRadius = 10.0;
                    AABB blastBox = new AABB(end.x - blastRadius, end.y - blastRadius, end.z - blastRadius,
                            end.x + blastRadius, end.y + blastRadius, end.z + blastRadius);
                    List<LivingEntity> blastTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, blastBox,
                            e -> e != owner && e.isAlive() && (owner == null || !DamageSources.isFriendlyFireBetween(owner, e))
                    );

                    for (LivingEntity target : blastTargets) {
                        double dist = target.position().distanceTo(end);
                        if (dist <= blastRadius) {
                            float factor = (float) (1.0 - (dist / blastRadius));
                            float blastDmg = Math.max(12.0f, damage * factor);

                            if (owner != null) {
                                DamageSources.applyDamage(target, blastDmg, ModCinematicSpells.ZOLTRAAK.get().getDamageSource(this, owner));
                            } else {
                                target.hurt(damageSources().magic(), blastDmg);
                            }

                            if (target.isBlocking()) {
                                if (target instanceof Player p) {
                                    p.disableShield();
                                }
                            }

                            Vec3 knockVec = target.position().subtract(end);
                            if (knockVec.lengthSqr() > 0.001) {
                                knockVec = knockVec.normalize().scale(Math.max(0.4, factor * 1.5));
                                target.push(knockVec.x, factor * 0.45, knockVec.z);
                                target.hurtMarked = true;
                            }
                        }
                    }

                    if (serverLevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                        int craterR = 3;
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

            // Piercing beam active raycast damage
            if (level() instanceof ServerLevel serverLevel) {
                float tickDamage = damage / 6.0f;
                Vec3 pStart = start;
                Vec3 pEnd = start.add(look.scale(getBeamLength()));
                AABB beamBox = new AABB(pStart, pEnd).inflate(1.2);
                List<LivingEntity> beamTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, beamBox,
                        e -> e != owner && e.isAlive() && (owner == null || !DamageSources.isFriendlyFireBetween(owner, e))
                );

                for (LivingEntity target : beamTargets) {
                    Vec3 toTarget = target.getBoundingBox().getCenter().subtract(pStart);
                    double proj = toTarget.dot(look);
                    if (proj >= 0 && proj <= getBeamLength()) {
                        Vec3 closest = pStart.add(look.scale(proj));
                        if (target.getBoundingBox().distanceToSqr(closest) <= 1.44) {
                            if (owner != null) {
                                DamageSources.applyDamage(target, tickDamage, ModCinematicSpells.ZOLTRAAK.get().getDamageSource(this, owner));
                            } else {
                                target.hurt(damageSources().magic(), tickDamage);
                            }
                            target.hurtMarked = true;
                        }
                    }
                }
            }
        }
    }

    private float calculatePiercingDistance(Vec3 start, Vec3 look, float maxDist) {
        float step = 1.0f;
        for (float d = 1.0f; d < maxDist; d += step) {
            Vec3 checkPos = start.add(look.scale(d));
            BlockPos bp = BlockPos.containing(checkPos);
            BlockState bs = level().getBlockState(bp);
            if (!bs.isAir() && bs.isSolidRender(level(), bp)) {
                if (bs.getDestroySpeed(level(), bp) > 1.5f) {
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
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putFloat("Damage", this.damage);
        compound.putFloat("MaxRange", this.maxRange);
    }
}
