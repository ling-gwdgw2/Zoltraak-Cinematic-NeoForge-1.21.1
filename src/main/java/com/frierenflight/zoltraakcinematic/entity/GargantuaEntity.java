package com.frierenflight.zoltraakcinematic.entity;

import com.frierenflight.zoltraakcinematic.ZoltraakDamage;
import com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicEntities;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.GameRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;

/**
 * 🌌 GargantuaEntity
 * A supermassive rotating Kerr black hole with general relativistic gravitational lensing,
 * volumetric accretion disk, photon rings, and cataclysmic event horizon collapse.
 */
public class GargantuaEntity extends Entity {

    public static final int LIFETIME_TICKS = 400; // 20.0 seconds
    public static final int TEAR_END_TICK = 30;   // 1.5s opening tear
    public static final int HOLD_END_TICK = 260;  // 13.0s active pull
    public static final int CRITICAL_END_TICK = 290; // 14.5s criticality
    public static final int BLAST_TICK = 290;     // Supernova detonation
    public static final int FADE_START_TICK = 320;// 16.0s fade begin

    public static final float GRAVITATIONAL_RADIUS = 4.0f; // r_g in blocks
    public static final float SPIN = 0.6f;                // a/M
    public static final float HORIZON_RADIUS = 1.8f;      // 1.8 r_g = 7.2 blocks
    public static final float SHADOW_RADIUS = 5.196f;     // 5.196 r_g = 20.8 blocks
    public static final float DISK_INNER_RADIUS = 3.83f;  // ISCO = 15.3 blocks
    public static final float DISK_OUTER_RADIUS = 13.5f;  // 54.0 blocks
    public static final double HOVER_HEIGHT = 0.0d;       // Center is the physical position
    public static final double PULL_RADIUS = 28.0d;       // Inward gravity field
    public static final double BLAST_RADIUS = 24.0d;      // Apocalyptic blast radius
    private static final double PULL_ACCELERATION = 0.065d;

    private static final EntityDataAccessor<Integer> DATA_CASTER_ID =
            SynchedEntityData.defineId(GargantuaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_START_GAME_TICK =
            SynchedEntityData.defineId(GargantuaEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_SEED =
            SynchedEntityData.defineId(GargantuaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_AXIS_X =
            SynchedEntityData.defineId(GargantuaEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_AXIS_Y =
            SynchedEntityData.defineId(GargantuaEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_AXIS_Z =
            SynchedEntityData.defineId(GargantuaEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_SWALLOWED =
            SynchedEntityData.defineId(GargantuaEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DISPLAY =
            SynchedEntityData.defineId(GargantuaEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID casterUuid;
    private boolean blastResolved = false;
    private final Map<BlockPos, BlockState> tornBlocks = new LinkedHashMap<>();

    public GargantuaEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public GargantuaEntity(Level level, LivingEntity caster, Vec3 center, Vec3 spinAxis) {
        this(ModCinematicEntities.GARGANTUA.get(), level);
        setPos(center.x, center.y, center.z);
        configure(caster, center, spinAxis, level.random.nextInt());
    }

    public void configure(LivingEntity caster, Vec3 center, Vec3 spinAxis, int seed) {
        if (caster != null) {
            this.casterUuid = caster.getUUID();
            this.entityData.set(DATA_CASTER_ID, caster.getId());
        }
        this.entityData.set(DATA_START_GAME_TICK, this.level().getGameTime());
        this.entityData.set(DATA_SEED, seed);
        Vec3 normAxis = (spinAxis != null && spinAxis.lengthSqr() > 0.001) ? spinAxis.normalize() : new Vec3(0, 1, 0);
        this.entityData.set(DATA_AXIS_X, (float) normAxis.x);
        this.entityData.set(DATA_AXIS_Y, (float) normAxis.y);
        this.entityData.set(DATA_AXIS_Z, (float) normAxis.z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CASTER_ID, -1);
        builder.define(DATA_START_GAME_TICK, -1L);
        builder.define(DATA_SEED, 0);
        builder.define(DATA_AXIS_X, 0.0f);
        builder.define(DATA_AXIS_Y, 1.0f);
        builder.define(DATA_AXIS_Z, 0.0f);
        builder.define(DATA_SWALLOWED, 0);
        builder.define(DATA_DISPLAY, false);
    }

    public int getCasterId() {
        return this.entityData.get(DATA_CASTER_ID);
    }

    public int getSwallowedCount() {
        return this.entityData.get(DATA_SWALLOWED);
    }

    public void noteSwallowed() {
        this.entityData.set(DATA_SWALLOWED, this.getSwallowedCount() + 1);
    }

    public Vec3 spinAxis() {
        return new Vec3(
                this.entityData.get(DATA_AXIS_X),
                this.entityData.get(DATA_AXIS_Y),
                this.entityData.get(DATA_AXIS_Z)
        );
    }

    public int getTimelineAgeTicks() {
        return this.tickCount;
    }

    public float getVisualAgeTicks(float partialTicks) {
        long start = this.entityData.get(DATA_START_GAME_TICK);
        if (start < 0) {
            return (float) this.tickCount + partialTicks;
        }
        float age = (float) (this.level().getGameTime() - start) + partialTicks;
        return Math.max(0.0f, age);
    }

    public Vec3 centre(float partialTicks) {
        double x = Mth.lerp((double) partialTicks, this.xo, this.getX());
        double y = Mth.lerp((double) partialTicks, this.yo, this.getY());
        double z = Mth.lerp((double) partialTicks, this.zo, this.getZ());
        return new Vec3(x, y, z);
    }

    public float gravitationalRadius(float partialTicks) {
        return GRAVITATIONAL_RADIUS;
    }

    public float opened(float partialTicks) {
        float age = getVisualAgeTicks(partialTicks);
        if (age <= 0.0f) return 0.15f;
        if (age >= (float) TEAR_END_TICK) return 1.0f;
        float progress = age / (float) TEAR_END_TICK;
        return Mth.clamp(0.15f + 0.85f * smoothstep(progress), 0.0f, 1.0f);
    }

    public float criticality(float partialTicks) {
        float age = getVisualAgeTicks(partialTicks);
        if (age <= (float) HOLD_END_TICK) return 0.0f;
        if (age >= (float) CRITICAL_END_TICK) return 1.0f;
        return smoothstep((age - (float) HOLD_END_TICK) / ((float) CRITICAL_END_TICK - (float) HOLD_END_TICK));
    }

    public float blastFlash(float partialTicks) {
        float age = getVisualAgeTicks(partialTicks) - (float) BLAST_TICK;
        if (age < 0.0f || age > 18.0f) return 0.0f;
        return 1.0f - age / 18.0f;
    }

    public float fade(float partialTicks) {
        float age = getVisualAgeTicks(partialTicks);
        if (age <= (float) FADE_START_TICK) return 0.0f;
        return Mth.clamp((age - (float) FADE_START_TICK) / ((float) LIFETIME_TICKS - (float) FADE_START_TICK), 0.0f, 1.0f);
    }

    public float brightness(float partialTicks) {
        float age = getVisualAgeTicks(partialTicks);
        if (age <= (float) TEAR_END_TICK) {
            return Math.max(0.15f, opened(partialTicks));
        }
        if (age <= (float) HOLD_END_TICK) {
            return 1.0f;
        }
        if (age <= (float) CRITICAL_END_TICK) {
            return 1.0f + 1.6f * criticality(partialTicks);
        }
        return Math.max(0.0f, 1.0f - fade(partialTicks));
    }

    private static float smoothstep(float x) {
        float t = Mth.clamp(x, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        Vec3 c = centre(1.0f);
        double r = 160.0;
        return new AABB(c.x - r, c.y - r, c.z - r, c.x + r, c.y + r, c.z + r);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide() && net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            ClientHandler.onClientTick(this);
        }

        if (this.tickCount == 1 && !this.level().isClientSide()) {
            Vec3 c = centre(1.0f);
            this.level().playSound(null, c.x, c.y, c.z, ModCinematicSounds.SINGULARITY_CHARGE.get(), SoundSource.WEATHER, 3.5f, 0.95f);
        }

        if (this.tickCount > 0 && this.tickCount <= HOLD_END_TICK && this.tickCount % 40 == 0 && !this.level().isClientSide()) {
            Vec3 c = centre(1.0f);
            this.level().playSound(null, c.x, c.y, c.z, ModCinematicSounds.SINGULARITY_ACTIVE.get(), SoundSource.WEATHER, 2.5f, 1.0f);
        }

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            if (this.tickCount >= 24 && this.tickCount <= HOLD_END_TICK) {
                tearSurfaceBlocks(serverLevel);
            }

            if (this.tickCount <= CRITICAL_END_TICK) {
                applyPull(serverLevel);
            }

            if (this.tickCount >= BLAST_TICK && !this.blastResolved) {
                resolveBlast(serverLevel);
                this.blastResolved = true;
            }

            if (this.tickCount >= BLAST_TICK + 8) {
                reconstructBlocks(serverLevel);
            }
        }

        if (this.tickCount >= LIFETIME_TICKS) {
            this.discard();
        }
    }

    @Override
    public void onClientRemoval() {
        super.onClientRemoval();
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            ClientHandler.onClientRemove(this);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        restoreAllRemainingBlocks();
        super.remove(reason);
        if (this.level().isClientSide() && net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            ClientHandler.onClientRemove(this);
        }
    }

    private static final class ClientHandler {
        private static void onClientTick(GargantuaEntity entity) {
            com.frierenflight.zoltraakcinematic.client.renderer.GargantuaPostProcessor.registerClientInstance(entity);

            if (entity.tickCount > 15 && entity.tickCount < GargantuaEntity.CRITICAL_END_TICK) {
                Level level = entity.level();
                Vec3 center = entity.centre(1.0f);
                Vec3 axis = entity.spinAxis();

                for (int i = 0; i < 3; i++) {
                    double theta = level.random.nextDouble() * Math.PI * 2.0;
                    double r = 6.0 + level.random.nextDouble() * 20.0;
                    double yOff = (level.random.nextDouble() - 0.5) * 3.0;

                    Vec3 p = center.add(Math.cos(theta) * r, yOff, Math.sin(theta) * r);
                    Vec3 inward = center.subtract(p).normalize();
                    Vec3 tangent = axis.cross(inward).normalize();
                    Vec3 vel = inward.scale(0.30).add(tangent.scale(0.45));

                    level.addParticle(ParticleTypes.REVERSE_PORTAL,
                            p.x, p.y, p.z,
                            vel.x, vel.y, vel.z);
                }
            }
        }

        private static void onClientRemove(GargantuaEntity entity) {
            com.frierenflight.zoltraakcinematic.client.renderer.GargantuaPostProcessor.unregisterClientInstance(entity);
        }
    }

    private void tearSurfaceBlocks(ServerLevel level) {
        if (!ZoltraakCinematicConfig.TEAR_BLOCKS.get()) return;

        if (ZoltraakCinematicConfig.RESPECT_MOB_GRIEFING.get() &&
                !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }

        if (this.tickCount % 2 != 0) return;

        Vec3 center = centre(1.0f);
        double minRadius = 2.5d;
        double maxRadius = 18.0d;

        int attempts = 3;
        for (int i = 0; i < attempts; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double r = minRadius + Math.sqrt(this.random.nextDouble()) * (maxRadius - minRadius);

            int x = Mth.floor(center.x + Math.cos(angle) * r);
            int z = Mth.floor(center.z + Math.sin(angle) * r);

            BlockPos topPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z)).below();
            BlockPos targetPos = topPos;

            if (!canTearBlock(level, targetPos)) {
                targetPos = topPos.below();
                if (!canTearBlock(level, targetPos)) continue;
            }

            double normR = r / maxRadius;
            double maxDepth = Math.max(1.0, 4.0 * (1.0 - normR * normR));
            double groundY = center.y - 14.0;
            if (targetPos.getY() < groundY - maxDepth) continue;

            BlockState state = level.getBlockState(targetPos);
            this.tornBlocks.put(targetPos.immutable(), state);

            FallingBlockEntity falling = FallingBlockEntity.fall(level, targetPos, state);
            falling.dropItem = false;
            falling.disableDrop();
            falling.time = 1;
            falling.setNoGravity(true);
            falling.addTag("gargantua_debris");

            Vec3 delta = center.subtract(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
            Vec3 inward = delta.normalize();
            Vec3 tangent = spinAxis().cross(inward).normalize();

            Vec3 initVel = new Vec3(
                    tangent.x * 0.35 + inward.x * 0.15,
                    0.42 + this.random.nextDouble() * 0.15,
                    tangent.z * 0.35 + inward.z * 0.15
            );
            falling.setDeltaMovement(initVel);
            falling.hurtMarked = true;
        }
    }

    private boolean canTearBlock(ServerLevel level, BlockPos pos) {
        if (this.tornBlocks.containsKey(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty()) return false;
        if (state.getDestroySpeed(level, pos) < 0) return false;
        if (level.getBlockEntity(pos) != null) return false;
        if (pos.getY() <= level.getMinBuildHeight() + 2) return false;
        return true;
    }

    private void reconstructBlocks(ServerLevel level) {
        if (!ZoltraakCinematicConfig.AUTO_RECONSTRUCT_BLOCKS.get()) {
            this.tornBlocks.clear();
            return;
        }

        if (this.tornBlocks.isEmpty()) return;

        int batchSize = Math.max(2, (int) Math.ceil((double) this.tornBlocks.size() / 25.0));

        List<BlockPos> sorted = new ArrayList<>(this.tornBlocks.keySet());
        sorted.sort(Comparator.comparingInt(Vec3i::getY));

        int count = 0;
        for (BlockPos pos : sorted) {
            if (count >= batchSize) break;
            BlockState state = this.tornBlocks.remove(pos);
            if (state != null) {
                level.setBlock(pos, state, 3);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        3, 0.2, 0.2, 0.2, 0.02);
                count++;
            }
        }

        if (count > 0 && this.tickCount % 6 == 0) {
            Vec3 center = centre(1.0f);
            level.playSound(null, center.x, center.y, center.z,
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7f, 1.2f);
        }
    }

    private void restoreAllRemainingBlocks() {
        if (!ZoltraakCinematicConfig.AUTO_RECONSTRUCT_BLOCKS.get()) {
            this.tornBlocks.clear();
            return;
        }

        if (this.tornBlocks.isEmpty()) return;
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        List<BlockPos> sorted = new ArrayList<>(this.tornBlocks.keySet());
        sorted.sort(Comparator.comparingInt(Vec3i::getY));

        for (BlockPos pos : sorted) {
            BlockState state = this.tornBlocks.remove(pos);
            if (state != null) {
                serverLevel.setBlock(pos, state, 3);
            }
        }
        this.tornBlocks.clear();
    }

    private void applyPull(ServerLevel level) {
        Vec3 center = centre(1.0f);
        double eventHorizonDist = (double) (HORIZON_RADIUS * GRAVITATIONAL_RADIUS);
        double pullDist = PULL_RADIUS;
        LivingEntity caster = resolveCaster(level);

        AABB box = new AABB(center.x - pullDist, center.y - pullDist, center.z - pullDist,
                center.x + pullDist, center.y + pullDist, center.z + pullDist);

        List<Entity> list = level.getEntities(this, box, e -> e.isAlive() && e != caster);
        DamageSource damageSource = GargantuaDamage.source(level, this, caster != null ? caster : this);

        for (Entity e : list) {
            Vec3 delta = center.subtract(e.position().add(0, e.getBbHeight() * 0.5, 0));
            double d = delta.length();
            if (d < 0.001) continue;

            if (d <= eventHorizonDist) {
                // Inside Event Horizon: Complete Gravitational Annihilation
                if (e instanceof FallingBlockEntity falling) {
                    noteSwallowed();
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, falling.getBlockState()),
                            falling.getX(), falling.getY(), falling.getZ(),
                            10, 0.4, 0.4, 0.4, 0.15);
                    if (this.tickCount % 6 == 0) {
                        level.playSound(null, center.x, center.y, center.z,
                                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.WEATHER, 0.6f, 0.5f);
                    }
                    falling.discard();
                    continue;
                }
                if (e instanceof Projectile || e instanceof ItemEntity) {
                    noteSwallowed();
                    e.discard();
                    continue;
                }
                if (e instanceof LivingEntity living) {
                    if (this.tickCount % 8 == 0) {
                        float tidalDmg = Math.max(12.0f, living.getMaxHealth() * 0.15f);
                        ZoltraakDamage.apply(living, tidalDmg, damageSource, true);
                        noteSwallowed();
                    }
                }
            } else {
                // Outside Event Horizon: Inward Relativistic Acceleration + Frame Dragging Swirl
                if (e instanceof FallingBlockEntity falling) {
                    falling.time = 1;
                    falling.setNoGravity(true);

                    Vec3 inward = delta.normalize();
                    Vec3 axis = spinAxis();
                    Vec3 tangent = axis.cross(inward).normalize();

                    double proximity = Mth.clamp((pullDist - d) / (pullDist - eventHorizonDist), 0.0, 1.0);
                    double inwardSpeed = 0.22 + proximity * 0.45;
                    double tangentSpeed = 0.40 + proximity * 0.75;

                    double verticalDist = center.y - (falling.getY() + 0.5);
                    double verticalSpeed = Mth.clamp(verticalDist * 0.12, -0.4, 0.4);

                    Vec3 targetVel = new Vec3(
                            inward.x * inwardSpeed + tangent.x * tangentSpeed,
                            verticalSpeed + inward.y * (inwardSpeed * 0.5),
                            inward.z * inwardSpeed + tangent.z * tangentSpeed
                    );

                    falling.setDeltaMovement(falling.getDeltaMovement().scale(0.60).add(targetVel.scale(0.40)));
                    falling.hurtMarked = true;
                    continue;
                }

                double strength = PULL_ACCELERATION * (1.0 + (pullDist - d) / pullDist * 2.2);
                Vec3 accel = delta.normalize().scale(strength);

                if (e instanceof Projectile) {
                    e.setDeltaMovement(e.getDeltaMovement().scale(0.85).add(accel.scale(2.5)));
                } else {
                    e.setDeltaMovement(e.getDeltaMovement().scale(0.90).add(accel));
                    e.hurtMarked = true;
                }
            }
        }
    }

    private void resolveBlast(ServerLevel level) {
        Vec3 center = centre(1.0f);
        LivingEntity caster = resolveCaster(level);
        DamageSource damageSource = GargantuaDamage.source(level, this, caster != null ? caster : this);

        // Thunderous Apocalyptic Sound across the entire dimension
        level.playSound(null, center.x, center.y, center.z, ModCinematicSounds.SINGULARITY_EXPLODE.get(), SoundSource.WEATHER, 6.0f, 0.75f);
        level.playSound(null, center.x, center.y, center.z, ModCinematicSounds.TINNITUS.get(), SoundSource.WEATHER, 3.0f, 1.0f);

        // Huge shockwave particles
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 5, 2.0, 2.0, 2.0, 0.1);
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 2, 0, 0, 0, 0);

        AABB blastBox = new AABB(center.x - BLAST_RADIUS, center.y - BLAST_RADIUS, center.z - BLAST_RADIUS,
                center.x + BLAST_RADIUS, center.y + BLAST_RADIUS, center.z + BLAST_RADIUS);

        // Vaporize any remaining swirling debris at detonation
        List<FallingBlockEntity> debrisList = level.getEntitiesOfClass(FallingBlockEntity.class, blastBox,
                f -> f.getTags().contains("gargantua_debris"));
        for (FallingBlockEntity debris : debrisList) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, debris.getBlockState()),
                    debris.getX(), debris.getY(), debris.getZ(), 8, 0.4, 0.4, 0.4, 0.2);
            debris.discard();
        }

        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, blastBox, e -> e.isAlive() && e != caster);

        float swallowedBonus = Math.min((float) getSwallowedCount() * 5.0f, 80.0f);
        float baseDamage = 95.0f + swallowedBonus;

        for (LivingEntity target : victims) {
            double dist = target.position().distanceTo(center);
            float falloff = (float) Math.max(0.2, 1.0 - dist / BLAST_RADIUS);
            float finalDamage = baseDamage * falloff;
            ZoltraakDamage.apply(target, finalDamage, damageSource, false);

            Vec3 push = target.position().subtract(center).normalize().scale(2.5 * falloff);
            target.setDeltaMovement(target.getDeltaMovement().add(push));
            target.hurtMarked = true;
        }
    }

    private LivingEntity resolveCaster(ServerLevel level) {
        if (this.casterUuid != null) {
            Entity e = level.getEntity(this.casterUuid);
            if (e instanceof LivingEntity living) return living;
        }
        int id = getCasterId();
        if (id >= 0) {
            Entity e = level.getEntity(id);
            if (e instanceof LivingEntity living) return living;
        }
        return null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Caster")) {
            this.casterUuid = tag.getUUID("Caster");
        }
        if (tag.contains("Swallowed")) {
            this.entityData.set(DATA_SWALLOWED, tag.getInt("Swallowed"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.casterUuid != null) {
            tag.putUUID("Caster", this.casterUuid);
        }
        tag.putInt("Swallowed", getSwallowedCount());
    }
}
