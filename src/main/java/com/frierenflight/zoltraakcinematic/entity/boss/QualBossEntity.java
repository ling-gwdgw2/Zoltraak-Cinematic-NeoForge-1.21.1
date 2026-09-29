package com.frierenflight.zoltraakcinematic.entity.boss;

import com.frierenflight.zoltraakcinematic.entity.boss.ai.QualAntiBarrierGoal;
import com.frierenflight.zoltraakcinematic.entity.boss.ai.QualCataclysmicGoal;
import com.frierenflight.zoltraakcinematic.entity.boss.ai.QualCombatGoal;
import com.frierenflight.zoltraakcinematic.entity.boss.ai.QualFollowPlayerGoal;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicAttributes;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSpells;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.MoveControl;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSounds;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;

/**
 * 📜 QualBossEntity — มหาจอมเวทควาล (Qual, The Elder Sage of Corruption)
 * Originator of Zoltraak (Killing Magic).
 *
 * Core Specifications:
 * - Base HP: 1500 (Scales +200 HP per nearby player within 48 blocks)
 * - Armor: 30, Armor Toughness: 10, Knockback Resistance: 0.8
 * - Magic Resistance: +40%, Spell Power: +40% (Zoltraak +50%)
 * - Movement: 3D Demonic Aerial Flight (Hovering 4–15 blocks above terrain)
 * - Boss Bar: PURPLE, PROGRESS, World Fog & Darken Screen enabled
 * - 4-Phase Combat Progression State Machine (100% -> 75% -> 40% -> 20% -> 0%)
 */
public class QualBossEntity extends AbstractSpellCastingMob implements Enemy, GeoEntity {

    public static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(QualBossEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> DATA_CHARGING_CATACLYSMIC =
            SynchedEntityData.defineId(QualBossEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> DATA_CASTING_STATE =
            SynchedEntityData.defineId(QualBossEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> DATA_IS_FLYING =
            SynchedEntityData.defineId(QualBossEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int CAST_STATE_IDLE = 0;
    public static final int CAST_STATE_BEAM = 1;
    public static final int CAST_STATE_BARRAGE = 2;
    public static final int CAST_STATE_CATACLYSMIC = 3;

    public static final double BASE_MAX_HEALTH = 1500.0;
    public static final double HEALTH_PER_PLAYER = 200.0;

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.translatable("entity.zoltraak_cinematic.qual_boss"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS
    ).setDarkenScreen(true).setPlayBossMusic(true).setCreateWorldFog(true);

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_IDLE_FLIGHT = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation ANIM_CAST_BEAM = RawAnimation.begin().thenLoop("cast_beam");
    private static final RawAnimation ANIM_CAST_BARRAGE = RawAnimation.begin().thenLoop("cast_barrage");
    private static final RawAnimation ANIM_CAST_CHARGE = RawAnimation.begin().thenLoop("cast_charge");
    private static final RawAnimation ANIM_DEATH = RawAnimation.begin().thenPlayAndHold("death");

    private boolean scaledHealthInitialized = false;
    private int lastKnownPlayerCount = 1;
    private int healthScalingCheckTimer = 0;
    private int spellCastCooldown = 40;
    private int phaseTransitionTimer = 0;

    public QualBossEntity(EntityType<? extends AbstractSpellCastingMob> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 100;
        this.setPersistenceRequired();

        // Configure 3D Demonic/Witch Levitation & Responsive Flight
        this.moveControl = new QualFlightMoveControl(this);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder prepareAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, BASE_MAX_HEALTH)
                .add(Attributes.ARMOR, 30.0)
                .add(Attributes.ARMOR_TOUGHNESS, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 96.0)
                .add(Attributes.FLYING_SPEED, 0.45)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(AttributeRegistry.MAX_MANA, 6000.0)
                .add(AttributeRegistry.MANA_REGEN, 60.0)
                .add(AttributeRegistry.SPELL_POWER, 1.4)
                .add(AttributeRegistry.SPELL_RESIST, 1.4)
                .add(AttributeRegistry.CAST_TIME_REDUCTION, 0.35)
                .add(AttributeRegistry.COOLDOWN_REDUCTION, 0.30)
                .add(ModCinematicAttributes.ZOLTRAAK_SPELL_POWER, 1.5)
                .add(ModCinematicAttributes.ZOLTRAAK_MAGIC_RESIST, 1.4);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, 1);
        builder.define(DATA_CHARGING_CATACLYSMIC, false);
        builder.define(DATA_CASTING_STATE, CAST_STATE_IDLE);
        builder.define(DATA_IS_FLYING, false);
    }

    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    public void setPhase(int phase) {
        this.entityData.set(PHASE, phase);
    }

    public boolean isCataclysmicCharging() {
        return this.entityData.get(DATA_CHARGING_CATACLYSMIC);
    }

    public void setCataclysmicCharging(boolean charging) {
        this.entityData.set(DATA_CHARGING_CATACLYSMIC, charging);
    }

    public int getCastingState() {
        return this.entityData.get(DATA_CASTING_STATE);
    }

    public void setCastingState(int state) {
        this.entityData.set(DATA_CASTING_STATE, state);
    }

    /**
     * Determines whether the entity is actively flying through the air (for animation switching).
     * Combines server-synced flag with client-side position delta tracking.
     */
    public boolean isFlyingMovement() {
        if (this.entityData.get(DATA_IS_FLYING)) {
            return true;
        }
        double dx = this.getX() - this.xo;
        double dy = this.getY() - this.yo;
        double dz = this.getZ() - this.zo;
        return (dx * dx + dy * dy + dz * dz) > 0.002 || this.getDeltaMovement().lengthSqr() > 0.003;
    }

    /**
     * Client-side fail-safe casting state resolver.
     * Combines our direct SynchedEntityData with Iron's Spells ClientMagicData.
     */
    public int getEffectiveCastingState() {
        int state = this.getCastingState();
        if (state != CAST_STATE_IDLE) {
            return state;
        }
        if (this.level().isClientSide) {
            return com.frierenflight.zoltraakcinematic.client.QualClientHelper.getSyncedSpellState(this);
        }
        return CAST_STATE_IDLE;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        // Target selectors: HurtByTarget takes priority so damaging players draw retaliation aggro
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));

        // Hierarchical Boss AI Goals:
        // Priority 1: Phase 4 Cataclysmic Overdrive Ultimate (HP < 20%)
        this.goalSelector.addGoal(1, new QualCataclysmicGoal(this));
        // Priority 2: Anti-Barrier Intelligence (Shadow Blink & Focused Dome Pressure)
        this.goalSelector.addGoal(2, new QualAntiBarrierGoal(this));
        // Priority 3: 4-Phase Combat Spellcasting Progression (Simultaneous with flight)
        this.goalSelector.addGoal(3, new QualCombatGoal(this));
        // Priority 4: 3D Aerial Flight, Pursuit & Player Following AI
        this.goalSelector.addGoal(4, new QualFollowPlayerGoal(this));
        // Priority 5: Look & Float
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 48.0f));
        this.goalSelector.addGoal(6, new FloatGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        updateDynamicHealthScaling(true);
        return data;
    }

    /**
     * Initializes health scaling for backwards compatibility (e.g. from QualSealingStoneBlockEntity).
     */
    public void initializeHealthScaling() {
        updateDynamicHealthScaling(true);
    }

    /**
     * Dynamically scales Qual's health based on nearby active players within 48 blocks.
     * Smoothly scales up mid-fight if additional players join the battle.
     */
    public void updateDynamicHealthScaling(boolean forceInitial) {
        if (this.level().isClientSide) return;

        List<Player> nearbyPlayers = this.level().getEntitiesOfClass(
                Player.class,
                this.getBoundingBox().inflate(48.0),
                p -> !p.isSpectator() && !p.isCreative() && p.isAlive()
        );

        int playerCount = Math.max(1, nearbyPlayers.size());

        if (forceInitial || !scaledHealthInitialized) {
            this.lastKnownPlayerCount = playerCount;
            double scaledMax = BASE_MAX_HEALTH + (playerCount - 1) * HEALTH_PER_PLAYER;

            AttributeInstance maxHealthAttr = this.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(scaledMax);
                this.setHealth((float) scaledMax);
            }
            this.scaledHealthInitialized = true;
        } else if (playerCount > this.lastKnownPlayerCount) {
            // New players joined the raid! Scale max health up while preserving HP percentage
            double oldMax = this.getMaxHealth();
            double newMax = BASE_MAX_HEALTH + (playerCount - 1) * HEALTH_PER_PLAYER;
            float currentHealth = this.getHealth();
            float hpRatio = (float) (currentHealth / Math.max(1.0, oldMax));

            AttributeInstance maxHealthAttr = this.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttr != null) {
                maxHealthAttr.setBaseValue(newMax);
                this.setHealth(Math.min((float) newMax, (float) (newMax * hpRatio)));
            }
            this.lastKnownPlayerCount = playerCount;

            // Roar of demonic awareness when more challengers arrive
            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 1.6, this.getZ(), 45, 1.2, 1.2, 1.2, 0.1);
                serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.6, this.getZ(), 60, 1.2, 1.6, 1.2, 0.3);
                serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 1.8f, 0.7f);
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!scaledHealthInitialized && this.tickCount > 20) {
            updateDynamicHealthScaling(true);
        }

        // Periodic dynamic health check for late-joining players every 60 ticks (3 seconds)
        if (++healthScalingCheckTimer >= 60) {
            healthScalingCheckTimer = 0;
            updateDynamicHealthScaling(false);
        }

        // Drop invalid targets (dead players, creative/spectator switchers)
        LivingEntity curTarget = this.getTarget();
        if (curTarget != null) {
            if (!curTarget.isAlive() || (curTarget instanceof Player p && (p.isSpectator() || p.isCreative()))) {
                this.setTarget(null);
            }
        }

        // Update Boss Bar progress
        this.bossEvent.setProgress(Math.max(0.0f, Math.min(1.0f, this.getHealth() / this.getMaxHealth())));

        // Determine combat phase from remaining HP percentage
        float hpPercent = this.getHealth() / this.getMaxHealth();
        int targetPhase;
        if (hpPercent > 0.75f) {
            targetPhase = 1; // Probing Stance
        } else if (hpPercent > 0.40f) {
            targetPhase = 2; // Demonic Barrage Matrix
        } else if (hpPercent > 0.20f) {
            targetPhase = 3; // Tactical Adaptation
        } else {
            targetPhase = 4; // Cataclysmic Overdrive
        }

        if (targetPhase != getPhase()) {
            transitionToPhase(targetPhase);
        }

        // Handle Phase Transition Effects & Timers
        if (phaseTransitionTimer > 0) {
            phaseTransitionTimer--;
            if (phaseTransitionTimer % 4 == 0 && this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 1.6, this.getZ(), 10, 0.8, 1.2, 0.8, 0.05);
            }
        }

        // Synchronize Active Combat Casting State to Tracking Clients
        int currentCastState = CAST_STATE_IDLE;
        if (this.isCataclysmicCharging()) {
            currentCastState = CAST_STATE_CATACLYSMIC;
        } else if (this.isCasting()) {
            String spellId = this.getMagicData() != null ? this.getMagicData().getCastingSpellId() : null;
            if (spellId != null && spellId.contains("barrage")) {
                currentCastState = CAST_STATE_BARRAGE;
            } else {
                currentCastState = CAST_STATE_BEAM;
            }
        }
        if (this.getCastingState() != currentCastState) {
            this.setCastingState(currentCastState);
        }

        // Synchronize 3D aerial flight movement state to tracking clients
        boolean isFlying = this.getDeltaMovement().lengthSqr() > 0.005;
        if (this.entityData.get(DATA_IS_FLYING) != isFlying) {
            this.entityData.set(DATA_IS_FLYING, isFlying);
        }

        // Active Head & Body Facing towards Player (Eliminates sideways and backwards angles)
        LivingEntity focus = this.getTarget();
        if (focus == null || !focus.isAlive()) {
            focus = this.level().getNearestPlayer(this, 48.0);
        }
        if (focus != null && focus.isAlive()) {
            faceTargetDirectly(focus);
        }
    }

    @Override
    public void initiateCastSpell(AbstractSpell spell, int spellLevel) {
        super.initiateCastSpell(spell, spellLevel);
        if (spell != null) {
            String spellId = spell.getSpellId();
            if (spellId != null && spellId.contains("barrage")) {
                setCastingState(CAST_STATE_BARRAGE);
            } else {
                setCastingState(CAST_STATE_BEAM);
            }
        }
    }

    @Override
    public void cancelCast() {
        super.cancelCast();
        if (this.getCastingState() == CAST_STATE_BEAM || this.getCastingState() == CAST_STATE_BARRAGE) {
            setCastingState(CAST_STATE_IDLE);
        }
    }

    private void transitionToPhase(int newPhase) {
        setPhase(newPhase);
        phaseTransitionTimer = 30; // 1.5s visual charge

        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), ModCinematicSounds.QUAL_PHASE_TRANSITION.get(), SoundSource.HOSTILE, 1.8f, 1.0f);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.8, this.getZ(), 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.6, this.getZ(), 60, 1.2, 1.8, 1.2, 0.2);
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Boss Aerial Stabilization: Dampen excessive upward vertical velocity from external explosions
        Vec3 dm = this.getDeltaMovement();
        if (dm.y > 0.85) {
            this.setDeltaMovement(dm.x, 0.4, dm.z);
        }

        // Active altitude ceiling: if knocked or pushed higher than 14 blocks above target, pull down to combat range
        if (!this.level().isClientSide) {
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                double maxCombatY = target.getY() + 14.0;
                if (this.getY() > maxCombatY) {
                    this.setDeltaMovement(dm.x, -0.35, dm.z);
                }
            }
        }

        // Visual Demonic Corruption Aura
        if (this.level().isClientSide) {
            double px = this.getX() + (this.getRandom().nextDouble() - 0.5) * 1.4;
            double py = this.getY() + 0.2 + this.getRandom().nextDouble() * 2.8;
            double pz = this.getZ() + (this.getRandom().nextDouble() - 0.5) * 1.4;

            // Black void / Witch purple particles trailing behind Qual
            this.level().addParticle(ParticleTypes.WITCH, px, py, pz, 0, -0.02, 0);
            if (this.getRandom().nextFloat() < 0.35f) {
                this.level().addParticle(ParticleTypes.PORTAL, px, py, pz, (this.getRandom().nextDouble() - 0.5) * 0.2, -0.1, (this.getRandom().nextDouble() - 0.5) * 0.2);
            }
        }
    }

    @Override
    public void knockback(double strength, double x, double z) {
        // Complete Boss Immunity: Qual's demonic levitation cancels all external knockback forces
    }

    @Override
    public void push(net.minecraft.world.entity.Entity entity) {
        // Boss cannot be shoved around by colliding players or mobs
    }

    @Override
    public void push(double x, double y, double z) {
        // Immune to external velocity pushes (wind charges, explosions, etc.)
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // Absolute immunity to fall damage
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", getPhase());
        tag.putBoolean("ScaledHealth", this.scaledHealthInitialized);
        tag.putInt("LastKnownPlayerCount", this.lastKnownPlayerCount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Phase")) {
            setPhase(tag.getInt("Phase"));
        }
        if (tag.contains("ScaledHealth")) {
            this.scaledHealthInitialized = tag.getBoolean("ScaledHealth");
        }
        if (tag.contains("LastKnownPlayerCount")) {
            this.lastKnownPlayerCount = tag.getInt("LastKnownPlayerCount");
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModCinematicSounds.QUAL_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModCinematicSounds.QUAL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModCinematicSounds.QUAL_DEATH.get();
    }

    @Override
    public float getVoicePitch() {
        return 0.65f; // Deep, ancient demonic resonance
    }

    // --- GeckoLib 4 Animatable Implementation ---

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Override and replace default biped controllers with Qual's custom demonic boss animations
        controllers.add(new AnimationController<>(this, "qual_flight_controller", 4, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(ANIM_DEATH);
            }
            int castState = this.getEffectiveCastingState();
            return switch (castState) {
                case CAST_STATE_CATACLYSMIC -> state.setAndContinue(ANIM_CAST_CHARGE);
                case CAST_STATE_BARRAGE -> state.setAndContinue(ANIM_CAST_BARRAGE);
                case CAST_STATE_BEAM -> state.setAndContinue(ANIM_CAST_BEAM);
                default -> {
                    if (this.isFlyingMovement()) {
                        yield state.setAndContinue(ANIM_IDLE_FLIGHT);
                    } else {
                        yield state.setAndContinue(ANIM_IDLE);
                    }
                }
            };
        }));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }

        // Phase 3 & 4: Qual's Demonic Barrier Reactive Dispersion (35% chance against magic/ranged projectiles)
        if (getPhase() >= 3 && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if ((source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE) || source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)) && this.getRandom().nextFloat() < 0.35f) {
                if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.6, this.getZ(), 20, 0.6, 0.8, 0.6, 0.1);
                    serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.6, this.getZ(), 10, 0.4, 0.6, 0.4, 0.05);
                    serverLevel.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 1.4f, 1.8f);
                }
                amount *= 0.5f; // Absorb 50% of the damage
            }
        }

        // Multiplayer Threat Retaliation: Switch aggro towards attacker on significant damage
        net.minecraft.world.entity.Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity livingAttacker && livingAttacker.isAlive() && !(livingAttacker instanceof QualBossEntity)) {
            LivingEntity cur = this.getTarget();
            if (cur != livingAttacker) {
                boolean forceSwitch = cur == null || !cur.isAlive() || amount >= 25.0f;
                float switchChance = forceSwitch ? 1.0f : (amount >= 8.0f ? 0.65f : 0.40f);
                if (this.getRandom().nextFloat() < switchChance) {
                    this.setTarget(livingAttacker);
                    this.setLastHurtByMob(livingAttacker);
                    faceTargetDirectly(livingAttacker);
                }
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel serverLevel, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(serverLevel, damageSource, recentlyHit);

        // Guaranteed Mythic Boss Drops from the Elder Sage
        this.spawnAtLocation(new ItemStack(com.frierenflight.zoltraakcinematic.registry.ModCinematicItems.CORRUPTION_CORE.get()));

        // Horns of Corruption: 2 to 4 base + 1 extra horn per additional player in the raid!
        int bonusHorns = Math.max(0, this.lastKnownPlayerCount - 1);
        int hornCount = 2 + this.getRandom().nextInt(3) + bonusHorns;
        this.spawnAtLocation(new ItemStack(com.frierenflight.zoltraakcinematic.registry.ModCinematicItems.HORN_OF_CORRUPTION.get(), hornCount));

        // Bonus corruption core for large raid teams (>= 3 players)
        if (this.lastKnownPlayerCount >= 3 && this.getRandom().nextFloat() < 0.5f) {
            this.spawnAtLocation(new ItemStack(com.frierenflight.zoltraakcinematic.registry.ModCinematicItems.CORRUPTION_CORE.get()));
        }
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        this.bossEvent.removeAllPlayers();
        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.5, this.getZ(), 3, 0.5, 0.5, 0.5, 0);
            serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.5, this.getZ(), 120, 1.5, 2.0, 1.5, 0.5);
            serverLevel.playSound(null, this.blockPosition(), ModCinematicSounds.QUAL_DEATH.get(), SoundSource.HOSTILE, 2.0f, 1.0f);

            // Award bonus XP orbs scaled with player count
            int bonusXp = (this.lastKnownPlayerCount - 1) * 75;
            if (bonusXp > 0) {
                net.minecraft.world.entity.ExperienceOrb.award(serverLevel, this.position(), bonusXp);
            }
        }
    }

    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        super.remove(reason);
        this.bossEvent.removeAllPlayers();
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isControlledByLocalInstance()) {
            if (this.isInWater()) {
                this.moveRelative(0.02F, travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8D));
            } else if (this.isInLava()) {
                this.moveRelative(0.02F, travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
            } else {
                this.move(MoverType.SELF, this.getDeltaMovement());
            }
        }
        this.calculateEntityAnimation(false);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            // Keep client-side body strictly aligned with head to eliminate sideways drift
            this.yBodyRot = this.yHeadRot;
            this.yBodyRotO = this.yHeadRotO;
        }
    }

    /**
     * Actively rotates the entity, body, and head directly towards the specified entity.
     * Eliminates awkward sideways angles and ensures direct face-to-face contact.
     */
    public void faceTargetDirectly(LivingEntity targetEntity) {
        if (targetEntity == null) return;
        double dx = targetEntity.getX() - this.getX();
        double dz = targetEntity.getZ() - this.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        if (horizDist > 0.05) {
            float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            float smoothYaw = Mth.rotLerp(0.35f, this.getYRot(), targetYaw);
            this.setYRot(smoothYaw);
            this.yRotO = smoothYaw;
            this.yBodyRot = smoothYaw;
            this.yBodyRotO = smoothYaw;
            this.yHeadRot = smoothYaw;
            this.yHeadRotO = smoothYaw;

            double dy = targetEntity.getEyeY() - this.getEyeY();
            float targetPitch = (float) (-(Mth.atan2(dy, horizDist) * (180.0 / Math.PI)));
            this.setXRot(Mth.rotLerp(0.35f, this.getXRot(), targetPitch));
            this.xRotO = this.getXRot();
        }
    }

    /**
     * 📜 QualFlightMoveControl — Dedicated 3D Aerial Kinematics & Flight Controller.
     * Provides smooth, fluid acceleration, deceleration, and 3D flight trajectory towards wanted positions.
     */
    public static class QualFlightMoveControl extends MoveControl {
        private final QualBossEntity qual;

        public QualFlightMoveControl(QualBossEntity qual) {
            super(qual);
            this.qual = qual;
        }

        @Override
        public void tick() {
            if (this.operation == Operation.MOVE_TO) {
                this.operation = Operation.WAIT;

                double dx = this.wantedX - this.qual.getX();
                double dy = this.wantedY - this.qual.getY();
                double dz = this.wantedZ - this.qual.getZ();
                double distSqr = dx * dx + dy * dy + dz * dz;

                if (distSqr < 0.06) {
                    this.qual.setDeltaMovement(this.qual.getDeltaMovement().scale(0.6));
                    return;
                }

                double dist = Math.sqrt(distSqr);
                Vec3 dir = new Vec3(dx / dist, dy / dist, dz / dist);

                // Facing logic: if player/target is nearby, keep looking directly at them; only orient towards flight direction if alone in wilderness
                LivingEntity focus = this.qual.getTarget();
                if (focus == null || !focus.isAlive()) {
                    focus = this.qual.level().getNearestPlayer(this.qual, 48.0);
                }

                if (focus != null && focus.isAlive()) {
                    this.qual.faceTargetDirectly(focus);
                } else if (distSqr > 0.04) {
                    float moveYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                    float smoothYaw = this.rotlerp(this.qual.getYRot(), moveYaw, 15.0F);
                    this.qual.setYRot(smoothYaw);
                    this.qual.yBodyRot = smoothYaw;
                    this.qual.yHeadRot = smoothYaw;
                }

                double baseSpeed = this.qual.getAttributeValue(Attributes.FLYING_SPEED);
                double targetSpeed = baseSpeed * this.speedModifier;

                if (dist > 18.0) {
                    targetSpeed *= 1.35;
                }

                Vec3 targetVelocity = dir.scale(targetSpeed);
                Vec3 currentVelocity = this.qual.getDeltaMovement();
                this.qual.setDeltaMovement(currentVelocity.lerp(targetVelocity, 0.22));
            } else {
                Vec3 dm = this.qual.getDeltaMovement();
                this.qual.setDeltaMovement(dm.x * 0.88, dm.y * 0.88, dm.z * 0.88);
            }
        }
    }
}
