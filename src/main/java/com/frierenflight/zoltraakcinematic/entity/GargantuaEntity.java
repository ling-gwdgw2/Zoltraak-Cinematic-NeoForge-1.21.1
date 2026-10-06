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
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.entity.PartEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
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
public class GargantuaEntity extends Entity implements TraceableEntity, OwnableEntity {

    public static final int LIFETIME_TICKS = 1200;      // 60.0 seconds (1 minute total!)
    public static final int TEAR_END_TICK = 60;         // 3.0s opening tear & expansion
    public static final int HOLD_END_TICK = 1060;       // 53.0s active pull & accretion vortex
    public static final int CRITICAL_START_TICK = 1060; // 53.0s criticality onset
    public static final int CRITICAL_END_TICK = 1100;   // 55.0s criticality peak & implosion
    public static final int BLAST_TICK = 1100;          // 55.0s Supernova detonation!
    public static final int FADE_START_TICK = 1115;     // 55.75s cosmic fade begin

    public static final float GRAVITATIONAL_RADIUS = 4.0f; // r_g in blocks
    public static final float SPIN = 0.6f;                // a/M
    public static final float HORIZON_RADIUS = 1.8f;      // 1.8 r_g = 7.2 blocks
    public static final float SHADOW_RADIUS = 5.196f;     // 5.196 r_g = 20.8 blocks
    public static final float DISK_INNER_RADIUS = 3.83f;  // ISCO = 15.3 blocks
    public static final float DISK_OUTER_RADIUS = 13.5f;  // 54.0 blocks
    public static final double PULL_RADIUS = 100.0d;     // Inward gravity field (default 100 blocks)
    public static final double BLAST_RADIUS = 32.0d;      // Apocalyptic blast radius
    private static final double PULL_ACCELERATION = 0.12d;

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
    private static final EntityDataAccessor<Float> DATA_SPELL_POWER =
            SynchedEntityData.defineId(GargantuaEntity.class, EntityDataSerializers.FLOAT);

    private UUID casterUuid;
    private boolean blastResolved = false;
    private boolean clientBlastTriggered = false;
    private final Map<BlockPos, BlockState> tornBlocks = new LinkedHashMap<>();

    public GargantuaEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public GargantuaEntity(Level level, LivingEntity caster, Vec3 center, Vec3 spinAxis) {
        this(level, caster, center, spinAxis, 500.0f);
    }

    public GargantuaEntity(Level level, LivingEntity caster, Vec3 center, Vec3 spinAxis, float spellPower) {
        this(ModCinematicEntities.GARGANTUA.get(), level);
        setPos(center.x, center.y, center.z);
        configure(caster, center, spinAxis, level.random.nextInt(), spellPower);
    }

    public void configure(LivingEntity caster, Vec3 center, Vec3 spinAxis, int seed) {
        configure(caster, center, spinAxis, seed, 500.0f);
    }

    public void configure(LivingEntity caster, Vec3 center, Vec3 spinAxis, int seed, float spellPower) {
        if (caster != null) {
            this.casterUuid = caster.getUUID();
            this.entityData.set(DATA_CASTER_ID, caster.getId());
        }
        this.entityData.set(DATA_START_GAME_TICK, this.level().getGameTime());
        this.entityData.set(DATA_SEED, seed);
        this.entityData.set(DATA_SPELL_POWER, Math.max(1.0f, spellPower));
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
        builder.define(DATA_SPELL_POWER, 500.0f);
    }

    public int getCasterId() {
        return this.entityData.get(DATA_CASTER_ID);
    }

    public float getSpellPower() {
        return this.entityData.get(DATA_SPELL_POWER);
    }

    public void setSpellPower(float power) {
        this.entityData.set(DATA_SPELL_POWER, Math.max(1.0f, power));
    }

    @Override
    public UUID getOwnerUUID() {
        return this.casterUuid;
    }

    @Override
    public LivingEntity getOwner() {
        if (this.level() instanceof ServerLevel serverLevel) {
            return resolveCaster(serverLevel);
        }
        int id = getCasterId();
        if (id >= 0) {
            Entity e = this.level().getEntity(id);
            if (e instanceof LivingEntity living) return living;
        }
        return null;
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
        float age = getVisualAgeTicks(partialTicks);
        if (age <= 0.0f) return 0.15f;
        if (age < (float) TEAR_END_TICK) {
            float p = smoothstep(age / (float) TEAR_END_TICK);
            return Mth.clamp(0.15f + (GRAVITATIONAL_RADIUS - 0.15f) * p, 0.15f, GRAVITATIONAL_RADIUS);
        }
        if (age <= (float) HOLD_END_TICK) {
            return GRAVITATIONAL_RADIUS;
        }
        if (age <= (float) CRITICAL_END_TICK) {
            // Critical gravitational implosion: compresses inward before detonating
            float p = smoothstep((age - (float) HOLD_END_TICK) / ((float) CRITICAL_END_TICK - (float) HOLD_END_TICK));
            return Mth.clamp(GRAVITATIONAL_RADIUS - 2.2f * p, 1.8f, GRAVITATIONAL_RADIUS);
        }
        // Supernova shock expansion: core rapidly blows open into expanding cosmic remnant
        float postBlast = age - (float) BLAST_TICK;
        float expand = Mth.clamp(postBlast / 85.0f, 0.0f, 1.0f);
        float easeExpand = 1.0f - (1.0f - expand) * (1.0f - expand);
        return 1.8f + (GRAVITATIONAL_RADIUS * 2.2f - 1.8f) * easeExpand;
    }

    public float opened(float partialTicks) {
        float age = getVisualAgeTicks(partialTicks);
        if (age <= 0.0f) return 0.0f;
        if (age < (float) TEAR_END_TICK) {
            float progress = age / (float) TEAR_END_TICK;
            return Mth.clamp(smoothstep(progress), 0.0f, 1.0f);
        }
        if (age <= (float) BLAST_TICK + 15.0f) {
            return 1.0f; // Stays fully open through peak detonation
        }
        if (age >= (float) LIFETIME_TICKS) {
            return 0.0f; // Completely closed and healed
        }
        // Seamless spacetime healing & dissolution from tick 1115 to 1200
        float dissolveProgress = (age - ((float) BLAST_TICK + 15.0f)) / ((float) LIFETIME_TICKS - ((float) BLAST_TICK + 15.0f));
        return Mth.clamp(1.0f - smoothstep(dissolveProgress), 0.0f, 1.0f);
    }

    public float criticality(float partialTicks) {
        float age = getVisualAgeTicks(partialTicks);
        if (age <= (float) HOLD_END_TICK) return 0.0f;
        if (age >= (float) CRITICAL_END_TICK) return 1.0f;
        return smoothstep((age - (float) HOLD_END_TICK) / ((float) CRITICAL_END_TICK - (float) HOLD_END_TICK));
    }

    public float blastFlash(float partialTicks) {
        float age = getVisualAgeTicks(partialTicks) - (float) BLAST_TICK;
        if (age < 0.0f || age > 75.0f) return 0.0f;
        float p = age / 75.0f;
        return (1.0f - p) * (1.0f - p);
    }

    public float fade(float partialTicks) {
        float age = getVisualAgeTicks(partialTicks);
        if (age <= (float) FADE_START_TICK) return 0.0f;
        return Mth.clamp((age - (float) FADE_START_TICK) / ((float) LIFETIME_TICKS - (float) FADE_START_TICK), 0.0f, 1.0f);
    }

    public float brightness(float partialTicks) {
        float age = getVisualAgeTicks(partialTicks);
        if (age <= (float) TEAR_END_TICK) {
            return 0.5f + 0.5f * opened(partialTicks);
        }
        if (age <= (float) HOLD_END_TICK) {
            return 1.0f;
        }
        if (age <= (float) CRITICAL_END_TICK) {
            return 1.0f + 3.0f * criticality(partialTicks);
        }
        if (age <= (float) LIFETIME_TICKS) {
            float blast = blastFlash(partialTicks);
            float open = opened(partialTicks);
            return (1.0f + 3.5f * blast) * open;
        }
        return 0.0f;
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

        // Server-side Audio Cues (100% Custom Mod Sounds)
        if (!this.level().isClientSide()) {
            Vec3 c = centre(1.0f);

            // 1. Spacetime Rupture Opening (Tick 1)
            if (this.tickCount == 1) {
                this.level().playSound(null, c.x, c.y, c.z, ModCinematicSounds.SINGULARITY_CHARGE.get(), SoundSource.WEATHER, 4.0f, 0.95f);
            }

            // 2. Active Cosmic Hum (Ticks 60 - 1060)
            if (this.tickCount > TEAR_END_TICK && this.tickCount <= HOLD_END_TICK && this.tickCount % 40 == 0) {
                this.level().playSound(null, c.x, c.y, c.z, ModCinematicSounds.SINGULARITY_ACTIVE.get(), SoundSource.WEATHER, 2.5f, 1.0f);
            }
        }

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            // Surface block tearing during active phase
            if (this.tickCount >= TEAR_END_TICK && this.tickCount <= HOLD_END_TICK) {
                tearSurfaceBlocks(serverLevel);
            }

            // Gravitational pull applies until criticality ends
            if (this.tickCount <= CRITICAL_END_TICK) {
                applyPull(serverLevel);
            }

            // Supernova Detonation at BLAST_TICK
            if (this.tickCount >= BLAST_TICK && !this.blastResolved) {
                resolveBlast(serverLevel);
                this.blastResolved = true;
            }

            // Pristine World Auto-Reconstruction (Ticks 1108 - 1200)
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

            // Supernova Detonation Screen Flash (Rendered smoothly via triggerFlash)
            if (entity.getVisualAgeTicks(0.0f) >= GargantuaEntity.BLAST_TICK && !entity.clientBlastTriggered) {
                entity.clientBlastTriggered = true;
                com.frierenflight.zoltraakcinematic.client.ZoltraakCinematicClientEvents.triggerFlash(45, 1.0f);
            }
        }

        private static void onClientRemove(GargantuaEntity entity) {
            com.frierenflight.zoltraakcinematic.client.renderer.GargantuaPostProcessor.unregisterClientInstance(entity);
        }
    }

    private void tearSurfaceBlocks(ServerLevel level) {
        if (!ZoltraakCinematicConfig.isTearBlocks()) return;

        if (ZoltraakCinematicConfig.isRespectMobGriefing() &&
                !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }

        // Limit maximum torn blocks to avoid server TPS drop during the 1-minute lifetime
        if (this.tornBlocks.size() >= 180) return;
        if (this.tickCount % 4 != 0) return;

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
        if (!ZoltraakCinematicConfig.isAutoReconstructBlocks()) {
            this.tornBlocks.clear();
            return;
        }

        if (this.tornBlocks.isEmpty()) return;

        // Evenly restore remaining blocks before LIFETIME_TICKS
        int remainingTicks = Math.max(1, LIFETIME_TICKS - this.tickCount);
        int batchSize = Math.max(3, (int) Math.ceil((double) this.tornBlocks.size() / (double) remainingTicks * 2.0));

        List<BlockPos> sorted = new ArrayList<>(this.tornBlocks.keySet());
        sorted.sort(Comparator.comparingInt(Vec3i::getY));

        int count = 0;
        for (BlockPos pos : sorted) {
            if (count >= batchSize) break;
            BlockState state = this.tornBlocks.remove(pos);
            if (state != null) {
                level.setBlock(pos, state, 3);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        4, 0.2, 0.2, 0.2, 0.05);
                count++;
            }
        }
    }

    private void restoreAllRemainingBlocks() {
        if (!ZoltraakCinematicConfig.isAutoReconstructBlocks()) {
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
        double eventHorizonDist = (double) (HORIZON_RADIUS * GRAVITATIONAL_RADIUS); // 7.2 blocks
        double diskRadius = 32.0d; // Continuous damage zone across the entire accretion vortex
        double pullDist = ZoltraakCinematicConfig.getPullRadius(); // 100.0 blocks
        LivingEntity caster = resolveCaster(level);

        AABB box = new AABB(center.x - pullDist, center.y - pullDist, center.z - pullDist,
                center.x + pullDist, center.y + pullDist, center.z + pullDist);

        List<Entity> list = level.getEntities(this, box, e -> e.isAlive() && e != caster);
        float tidalPct = (float) ZoltraakCinematicConfig.getTidalDamagePercent();
        double baseAccel = ZoltraakCinematicConfig.getPullAcceleration();
        Set<UUID> damagedThisTick = new HashSet<>();

        for (Entity e : list) {
            // Distance measured to closest point on bounding box surface (crucial for giant bosses)
            double boxDist = Math.sqrt(e.getBoundingBox().distanceToSqr(center));
            if (boxDist > pullDist) continue;

            Vec3 delta = center.subtract(e.position().add(0, e.getBbHeight() * 0.5, 0));
            double d = delta.length();
            if (d < 0.001) d = 0.001;

            // Resolve target living entity (supports multi-part entities like Ender Dragon and Cataclysm bosses)
            LivingEntity targetLiving = null;
            if (e instanceof LivingEntity living) {
                targetLiving = living;
            } else if (e instanceof PartEntity<?> part) {
                if (part.getParent() instanceof LivingEntity parentLiving) {
                    targetLiving = parentLiving;
                }
            }

            // --- DAMAGE LOGIC (Accretion Disk & Event Horizon) ---
            // Continuous damage applies every 8 ticks (0.4s) to ANY mob or boss within 32 blocks
            if (targetLiving != null && targetLiving.isAlive() && targetLiving != caster && this.tickCount % 8 == 0) {
                if (boxDist <= diskRadius && damagedThisTick.add(targetLiving.getUUID())) {
                    // For monsters and bosses: damageCauser is ALWAYS 'this' (GargantuaEntity).
                    // GargantuaEntity is physically located right next to the boss (boxDist <= 32),
                    // which completely bypasses boss anti-cheese / anti-snipe distance checks at ANY player range!
                    // Calling targetLiving.setLastHurtByPlayer(player) guarantees full player kill credit, loot, and XP.
                    Entity damageCauser = (targetLiving instanceof Player && caster != null) ? caster : this;
                    DamageSource damageSource = GargantuaDamage.source(level, this, damageCauser);
                    if (caster instanceof Player player) {
                        targetLiving.setLastHurtByPlayer(player);
                    }

                    float dmg;
                    if (boxDist <= eventHorizonDist) {
                        // Event horizon core: maximum gravitational crush
                        dmg = Math.max(35.0f, targetLiving.getMaxHealth() * tidalPct);
                    } else {
                        // Accretion disk: relativistic plasma shear (scales smoothly from 30% to 100% of tidal damage)
                        double proximity = (diskRadius - boxDist) / (diskRadius - eventHorizonDist);
                        float plasmaFactor = (float) (0.30 + 0.70 * proximity);
                        dmg = Math.max(20.0f, targetLiving.getMaxHealth() * (tidalPct * plasmaFactor));
                    }

                    ZoltraakDamage.apply(targetLiving, dmg, damageSource, true);
                    noteSwallowed();
                }
            }

            // --- PHYSICAL MOVEMENT & ACCELERATION LOGIC ---
            if (d <= eventHorizonDist) {
                // Inside Event Horizon: Complete Gravitational Annihilation
                if (e instanceof FallingBlockEntity falling) {
                    noteSwallowed();
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, falling.getBlockState()),
                            falling.getX(), falling.getY(), falling.getZ(),
                            10, 0.4, 0.4, 0.4, 0.15);
                    falling.discard();
                    continue;
                }
                if (e instanceof Projectile) {
                    noteSwallowed();
                    e.discard();
                    continue;
                }
                if (e instanceof ItemEntity item) {
                    // ITEMS ARE NEVER DELETED OR SWALLOWED!
                    // Safely dampen velocity so items swirl gently without being destroyed
                    item.setDeltaMovement(item.getDeltaMovement().scale(0.85));
                    continue;
                }
                // Living entities inside horizon: pull gently toward singularity center
                Vec3 inward = delta.normalize().scale(0.15);
                e.setDeltaMovement(e.getDeltaMovement().scale(0.50).add(inward));
                e.hurtMarked = true;
            } else {
                // Outside Event Horizon: Inward Relativistic Acceleration + Frame Dragging Swirl
                if (e instanceof FallingBlockEntity falling) {
                    falling.time = 1;
                    falling.setNoGravity(true);

                    Vec3 inward = delta.normalize();
                    Vec3 axis = spinAxis();
                    Vec3 tangent = axis.cross(inward).normalize();

                    double proximity = Mth.clamp((pullDist - d) / (pullDist - eventHorizonDist), 0.0, 1.0);
                    double inwardSpeed = 0.25 + proximity * 0.55;
                    double tangentSpeed = 0.40 + proximity * 0.85;

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

                // Inward gravity pull: increases as entity gets closer, but maintains strong pull across 100 blocks
                double proximity = (pullDist - d) / pullDist;
                double strength = baseAccel * (1.0 + proximity * 3.5);
                Vec3 accel = delta.normalize().scale(strength);

                if (e instanceof Projectile) {
                    e.setDeltaMovement(e.getDeltaMovement().scale(0.85).add(accel.scale(2.5)));
                } else if (e instanceof ItemEntity item) {
                    // Items are pulled towards the vortex but remain intact and safe to pick up
                    item.setDeltaMovement(item.getDeltaMovement().scale(0.92).add(accel));
                    item.hurtMarked = true;
                } else {
                    e.setDeltaMovement(e.getDeltaMovement().scale(0.92).add(accel));
                    e.hurtMarked = true;
                }
            }
        }
    }

    private void resolveBlast(ServerLevel level) {
        Vec3 center = centre(1.0f);
        LivingEntity caster = resolveCaster(level);

        // Thunderous Apocalyptic Supernova Sound (100% Custom Mod Sounds)
        level.playSound(null, center.x, center.y, center.z, ModCinematicSounds.SINGULARITY_EXPLODE.get(), SoundSource.WEATHER, 8.0f, 0.85f);
        level.playSound(null, center.x, center.y, center.z, ModCinematicSounds.TINNITUS.get(), SoundSource.WEATHER, 5.0f, 1.0f);

        double blastRadius = ZoltraakCinematicConfig.getBlastRadius();
        AABB blastBox = new AABB(center.x - blastRadius, center.y - blastRadius, center.z - blastRadius,
                center.x + blastRadius, center.y + blastRadius, center.z + blastRadius);

        // Vaporize any remaining swirling debris at detonation
        List<FallingBlockEntity> debrisList = level.getEntitiesOfClass(FallingBlockEntity.class, blastBox,
                f -> f.getTags().contains("gargantua_debris"));
        for (FallingBlockEntity debris : debrisList) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, debris.getBlockState()),
                    debris.getX(), debris.getY(), debris.getZ(), 8, 0.4, 0.4, 0.4, 0.2);
            debris.discard();
        }

        List<Entity> list = level.getEntities(this, blastBox, e -> e.isAlive() && e != caster);
        Set<UUID> hitTargets = new HashSet<>();

        float spellPowerMult = Math.max(0.1f, getSpellPower() / 500.0f);
        float swallowedBonus = Math.min((float) getSwallowedCount() * 10.0f, 200.0f);
        float baseDamage = ((float) ZoltraakCinematicConfig.getBlastDamage() * spellPowerMult) + swallowedBonus;

        for (Entity e : list) {
            LivingEntity target = null;
            if (e instanceof LivingEntity living) {
                target = living;
            } else if (e instanceof PartEntity<?> part) {
                if (part.getParent() instanceof LivingEntity parentLiving) {
                    target = parentLiving;
                }
            }
            if (target == null || !target.isAlive() || target == caster) continue;
            if (!hitTargets.add(target.getUUID())) continue;

            Entity damageCauser = (target instanceof Player && caster != null) ? caster : this;
            DamageSource damageSource = GargantuaDamage.source(level, this, damageCauser);
            if (caster instanceof Player player) {
                target.setLastHurtByPlayer(player);
            }

            double dist = Math.sqrt(target.getBoundingBox().distanceToSqr(center));
            float falloff = (float) Math.max(0.25, 1.0 - dist / blastRadius);
            float finalDamage = baseDamage * falloff;
            ZoltraakDamage.apply(target, finalDamage, damageSource, false);

            Vec3 push = target.position().subtract(center).normalize().scale(3.2 * falloff);
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
        if (tag.contains("SpellPower")) {
            setSpellPower(tag.getFloat("SpellPower"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.casterUuid != null) {
            tag.putUUID("Caster", this.casterUuid);
        }
        tag.putInt("Swallowed", getSwallowedCount());
        tag.putFloat("SpellPower", getSpellPower());
    }
}
