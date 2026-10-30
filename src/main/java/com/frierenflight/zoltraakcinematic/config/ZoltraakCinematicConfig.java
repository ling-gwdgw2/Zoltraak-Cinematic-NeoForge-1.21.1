package com.frierenflight.zoltraakcinematic.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common / Server Gameplay Configuration for Zoltraak: Cinematic Edition.
 * Manages physical block interactions, flight mechanics, damage multipliers,
 * barrage aerodynamics, defense barriers, Qual boss scaling, and relics.
 */
public class ZoltraakCinematicConfig {
    public static final ModConfigSpec SPEC;

    // --- Gargantua Singularity ---
    public static final ModConfigSpec.DoubleValue PULL_RADIUS;
    public static final ModConfigSpec.DoubleValue PULL_ACCELERATION;
    public static final ModConfigSpec.DoubleValue TIDAL_DAMAGE_PERCENT;
    public static final ModConfigSpec.DoubleValue BLAST_DAMAGE;
    public static final ModConfigSpec.DoubleValue BLAST_RADIUS;
    public static final ModConfigSpec.BooleanValue AUTO_RECONSTRUCT_BLOCKS;
    public static final ModConfigSpec.BooleanValue TEAR_BLOCKS;
    public static final ModConfigSpec.IntValue MAX_TORN_DEBRIS;
    public static final ModConfigSpec.BooleanValue RESPECT_MOB_GRIEFING;

    // --- Flight System ---
    public static final ModConfigSpec.DoubleValue BASE_MANA_COST_PER_SEC;
    public static final ModConfigSpec.DoubleValue MANA_REDUCTION_PER_LEVEL;
    public static final ModConfigSpec.BooleanValue ENABLE_SPEED_ALTITUDE_SCALING;
    public static final ModConfigSpec.DoubleValue SPEED_MULTIPLIER_EXPONENT;
    public static final ModConfigSpec.DoubleValue MAX_SPEED_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ALTITUDE_MULTIPLIER_EXPONENT;
    public static final ModConfigSpec.DoubleValue MAX_ALTITUDE_MULTIPLIER;
    public static final ModConfigSpec.IntValue EMERGENCY_BUFFER_TICKS;
    public static final ModConfigSpec.BooleanValue AUTO_ENGAGE_AIRBORNE;
    public static final ModConfigSpec.BooleanValue ENABLE_TELEMETRY_HUD;

    // --- Spells & Beams ---
    public static final ModConfigSpec.DoubleValue BEAM_STANDARD_RANGE;
    public static final ModConfigSpec.DoubleValue BEAM_LARGE_RANGE;
    public static final ModConfigSpec.DoubleValue BEAM_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue DISABLE_SHIELD_ON_HIT;
    public static final ModConfigSpec.DoubleValue BEAM_KNOCKBACK_FACTOR;
    public static final ModConfigSpec.IntValue CORRUPTED_WITHER_DURATION;
    public static final ModConfigSpec.BooleanValue RESPECT_FRIENDLY_FIRE;
    public static final ModConfigSpec.BooleanValue BEAM_DRILL_BLOCKS;
    public static final ModConfigSpec.DoubleValue BEAM_DRILL_RADIUS;

    // --- Barrage Matrix ---
    public static final ModConfigSpec.DoubleValue BARRAGE_HOMING_STRENGTH;
    public static final ModConfigSpec.DoubleValue BARRAGE_PROJECTILE_SPEED;
    public static final ModConfigSpec.IntValue BARRAGE_MAX_LIFETIME_TICKS;
    public static final ModConfigSpec.IntValue BARRAGE_FIRE_INTERVAL;
    public static final ModConfigSpec.DoubleValue BARRAGE_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue BARRAGE_BLAST_RADIUS;

    // --- Defense Barrier ---
    public static final ModConfigSpec.DoubleValue BARRIER_CAPACITY_MULTIPLIER;
    public static final ModConfigSpec.IntValue BARRIER_DURATION_TICKS;
    public static final ModConfigSpec.BooleanValue BARRIER_DEFLECT_ARROWS;
    public static final ModConfigSpec.BooleanValue BARRIER_DEFLECT_MAGIC_PROJECTILES;

    // --- Qual Boss ---
    public static final ModConfigSpec.DoubleValue QUAL_BASE_HEALTH;
    public static final ModConfigSpec.DoubleValue QUAL_HEALTH_PER_EXTRA_PLAYER;
    public static final ModConfigSpec.DoubleValue QUAL_ARMOR;
    public static final ModConfigSpec.DoubleValue QUAL_ARMOR_TOUGHNESS;
    public static final ModConfigSpec.IntValue QUAL_RECOVERY_WINDOW_MIN_TICKS;
    public static final ModConfigSpec.IntValue QUAL_RECOVERY_WINDOW_MAX_TICKS;
    public static final ModConfigSpec.DoubleValue QUAL_DESPAWN_DISTANCE;

    // --- Artifacts & Relics ---
    public static final ModConfigSpec.DoubleValue STAFF_CAST_SPEED_BONUS;
    public static final ModConfigSpec.DoubleValue STAFF_MANA_REGEN_BONUS;
    public static final ModConfigSpec.DoubleValue CHRONOS_WATCH_RADIUS;
    public static final ModConfigSpec.IntValue CHRONOS_WATCH_DURATION_TICKS;
    public static final ModConfigSpec.IntValue CHRONOS_WATCH_COOLDOWN_SECONDS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        // 1. Gargantua Singularity
        builder.comment("Astral Singularity / Kerr Rotating Black Hole Settings").push("gargantua");
        PULL_RADIUS = builder
                .comment("Gravitational attraction pull radius in blocks.")
                .defineInRange("pullRadius", 100.0d, 10.0d, 300.0d);
        PULL_ACCELERATION = builder
                .comment("Base gravitational pull acceleration strength applied to entities.")
                .defineInRange("pullAcceleration", 0.12d, 0.01d, 2.0d);
        TIDAL_DAMAGE_PERCENT = builder
                .comment("Tidal disruption damage dealt to living entities inside event horizon (fraction of Max HP per 0.4s).")
                .defineInRange("tidalDamagePercent", 0.08d, 0.01d, 1.0d);
        BLAST_DAMAGE = builder
                .comment("Base damage dealt by the final Supernova detonation at tick 1100 (55.0s).")
                .defineInRange("blastDamage", 800.0d, 10.0d, 10000.0d);
        BLAST_RADIUS = builder
                .comment("Explosion blast radius in blocks for the final Supernova detonation.")
                .defineInRange("blastRadius", 32.0d, 5.0d, 200.0d);
        AUTO_RECONSTRUCT_BLOCKS = builder
                .comment("Legacy reconstruction toggle. (Note: Block restoration is permanently deactivated for server TPS stability).")
                .define("autoReconstructBlocks", false);
        TEAR_BLOCKS = builder
                .comment("Whether Gargantua should physically tear surface blocks into orbiting accretion disk debris.")
                .define("tearBlocks", true);
        MAX_TORN_DEBRIS = builder
                .comment("Maximum surface blocks permitted to be torn into debris over the 60s lifetime to protect server TPS.")
                .defineInRange("maxTornDebris", 180, 0, 1000);
        RESPECT_MOB_GRIEFING = builder
                .comment("Whether Gargantua respects the vanilla 'mobGriefing' gamerule.")
                .define("respectMobGriefing", true);
        builder.pop();

        // 2. Flight System
        builder.comment("Frieren Human Flight Magic System").push("flight_system");
        BASE_MANA_COST_PER_SEC = builder
                .comment("Base mana consumed per second at Flight Spell Level 1 while hovering or cruising.")
                .defineInRange("baseManaCostPerSecond", 50.0d, 5.0d, 200.0d);
        MANA_REDUCTION_PER_LEVEL = builder
                .comment("Amount of base mana saved per spell level (e.g. Level 5 consumes 40.0 mana/sec).")
                .defineInRange("manaReductionPerLevel", 2.5d, 0.0d, 10.0d);
        ENABLE_SPEED_ALTITUDE_SCALING = builder
                .comment("When true, mana consumption increases exponentially when flying at high speeds or high altitudes.")
                .define("enableSpeedAltitudeScaling", true);
        SPEED_MULTIPLIER_EXPONENT = builder
                .comment("Exponential scaling curve for high-speed cruising beyond 0.10 blocks/tick.")
                .defineInRange("speedMultiplierExponent", 1.8d, 1.0d, 3.0d);
        MAX_SPEED_MULTIPLIER = builder
                .comment("Hard cap on the velocity mana multiplier to prevent overflow during physics glitches.")
                .defineInRange("maxSpeedMultiplier", 8.0d, 1.0d, 20.0d);
        ALTITUDE_MULTIPLIER_EXPONENT = builder
                .comment("Exponential scaling curve for flying high above the ground surface (>3 blocks).")
                .defineInRange("altitudeMultiplierExponent", 1.7d, 1.0d, 3.0d);
        MAX_ALTITUDE_MULTIPLIER = builder
                .comment("Hard cap on altitude mana multiplier when flying high in the sky.")
                .defineInRange("maxAltitudeMultiplier", 15.0d, 1.0d, 30.0d);
        EMERGENCY_BUFFER_TICKS = builder
                .comment("Emergency Slow Falling duration in ticks upon mana depletion (70 ticks = 3.5s) to prevent falling deaths.")
                .defineInRange("emergencyBufferTicks", 70, 0, 200);
        AUTO_ENGAGE_AIRBORNE = builder
                .comment("Automatically engages flight mode when jumping or falling off a ledge while under the Flight effect.")
                .define("autoEngageAirborne", true);
        ENABLE_TELEMETRY_HUD = builder
                .comment("Displays real-time flight telemetry (Mana/s, Altitude, Speed factor) on the action bar.")
                .define("enableTelemetryHUD", true);
        builder.pop();

        // 3. Spells & Beams
        builder.comment("Zoltraak Piercing Beams & Spells").push("spells_and_beams");
        BEAM_STANDARD_RANGE = builder
                .comment("Maximum piercing beam length in blocks for standard Zoltraak and Corrupted Zoltraak.")
                .defineInRange("beamStandardRange", 128.0d, 16.0d, 256.0d);
        BEAM_LARGE_RANGE = builder
                .comment("Maximum beam length in blocks for charged Great Zoltraak.")
                .defineInRange("beamLargeRange", 192.0d, 32.0d, 512.0d);
        BEAM_DAMAGE_MULTIPLIER = builder
                .comment("Global damage multiplier applied to all continuous Zoltraak beams.")
                .defineInRange("beamDamageMultiplier", 1.0d, 0.1d, 10.0d);
        DISABLE_SHIELD_ON_HIT = builder
                .comment("If true, beam impact disables the target's shield blocking.")
                .define("disableShieldOnHit", true);
        BEAM_KNOCKBACK_FACTOR = builder
                .comment("Knockback impulse applied to targets caught in the beam shockwave.")
                .defineInRange("beamKnockbackFactor", 1.5d, 0.0d, 5.0d);
        CORRUPTED_WITHER_DURATION = builder
                .comment("Wither debuff duration in ticks (120 ticks = 6.0s) applied by Corrupted/Black Zoltraak.")
                .defineInRange("corruptedWitherDuration", 120, 0, 600);
        RESPECT_FRIENDLY_FIRE = builder
                .comment("Prevents beams from damaging team members, party members, or tamed pets of the caster.")
                .define("respectFriendlyFire", true);
        BEAM_DRILL_BLOCKS = builder
                .comment("If true and mobGriefing is enabled, Zoltraak beams physically drill/blast a cylindrical hole through blocks along their trajectory.")
                .define("beamDrillBlocks", true);
        BEAM_DRILL_RADIUS = builder
                .comment("Base radius in blocks of the borehole drilled by standard Zoltraak (Great Zoltraak scales by 2x).")
                .defineInRange("beamDrillRadius", 1.25d, 0.5d, 4.0d);
        builder.pop();

        // 4. Barrage Matrix
        builder.comment("Fern's Grand Phalanx Barrage (24-Circle Matrix)").push("barrage_matrix");
        BARRAGE_HOMING_STRENGTH = builder
                .comment("Aerodynamic steering rate toward target (0.0 = straight unguided lance, 1.0 = instant tracking).")
                .defineInRange("barrageHomingStrength", 0.34d, 0.0d, 1.0d);
        BARRAGE_PROJECTILE_SPEED = builder
                .comment("Flight velocity of individual barrage light lances in blocks/tick.")
                .defineInRange("barrageProjectileSpeed", 1.75d, 0.5d, 5.0d);
        BARRAGE_MAX_LIFETIME_TICKS = builder
                .comment("Maximum projectile lifetime before despawning (60 ticks = 3.0s).")
                .defineInRange("barrageMaxLifetimeTicks", 60, 20, 200);
        BARRAGE_FIRE_INTERVAL = builder
                .comment("Interval between barrage volleys in ticks (10 ticks = 0.5s = 2 volleys/sec).")
                .defineInRange("barrageFireInterval", 10, 4, 30);
        BARRAGE_DAMAGE_MULTIPLIER = builder
                .comment("Damage scaling factor for barrage projectiles.")
                .defineInRange("barrageDamageMultiplier", 1.0d, 0.1d, 5.0d);
        BARRAGE_BLAST_RADIUS = builder
                .comment("Area-of-effect impact splash radius in blocks.")
                .defineInRange("barrageBlastRadius", 1.2d, 0.0d, 4.0d);
        builder.pop();

        // 5. Defense Barrier
        builder.comment("Defensive Magic (Directional Aegis & Geodesic Dome)").push("defense_barrier");
        BARRIER_CAPACITY_MULTIPLIER = builder
                .comment("Durability multiplier for barrier shield HP (both Directional Aegis and Geodesic Dome).")
                .defineInRange("barrierCapacityMultiplier", 1.0d, 0.1d, 10.0d);
        BARRIER_DURATION_TICKS = builder
                .comment("Base barrier lifetime in ticks (300 ticks = 15.0s) before naturally dissipating.")
                .defineInRange("barrierDurationTicks", 300, 60, 1200);
        BARRIER_DEFLECT_ARROWS = builder
                .comment("When true, physically deflects mundane projectile weapons (arrows, tridents).")
                .define("barrierDeflectArrows", true);
        BARRIER_DEFLECT_MAGIC_PROJECTILES = builder
                .comment("When true, absorbs and neutralizes incoming hostile magical projectiles.")
                .define("barrierDeflectMagicProjectiles", true);
        builder.pop();

        // 6. Qual Boss
        builder.comment("The Elder Sage of Corruption: Qual Boss Encounter").push("qual_boss");
        QUAL_BASE_HEALTH = builder
                .comment("Qual's baseline max health for solo encounters.")
                .defineInRange("qualBaseHealth", 1500.0d, 200.0d, 10000.0d);
        QUAL_HEALTH_PER_EXTRA_PLAYER = builder
                .comment("Additional health dynamically granted per nearby player in multiplayer boss fights.")
                .defineInRange("qualHealthPerExtraPlayer", 200.0d, 0.0d, 2000.0d);
        QUAL_ARMOR = builder
                .comment("Base armor value for Qual.")
                .defineInRange("qualArmor", 30.0d, 0.0d, 100.0d);
        QUAL_ARMOR_TOUGHNESS = builder
                .comment("Base armor toughness for Qual.")
                .defineInRange("qualArmorToughness", 10.0d, 0.0d, 50.0d);
        QUAL_RECOVERY_WINDOW_MIN_TICKS = builder
                .comment("Minimum recovery window (200 ticks = 10.0s) after Qual executes his 4s Corrupted Barrage.")
                .defineInRange("qualRecoveryWindowMinTicks", 200, 60, 400);
        QUAL_RECOVERY_WINDOW_MAX_TICKS = builder
                .comment("Maximum recovery window (240 ticks = 12.0s) during which players have a melee opening.")
                .defineInRange("qualRecoveryWindowMaxTicks", 240, 80, 600);
        QUAL_DESPAWN_DISTANCE = builder
                .comment("Maximum distance from players before Qual resets and re-seals into the sealing stone.")
                .defineInRange("qualDespawnDistance", 96.0d, 32.0d, 256.0d);
        builder.pop();

        // 7. Artifacts & Relics
        builder.comment("Magical Artifacts & Relics").push("artifacts_and_relics");
        STAFF_CAST_SPEED_BONUS = builder
                .comment("Cast time reduction (+15%) granted by Frieren's Staff.")
                .defineInRange("staffCastSpeedBonus", 0.15d, 0.0d, 1.0d);
        STAFF_MANA_REGEN_BONUS = builder
                .comment("Flat mana regeneration bonus (mana/s) granted by Frieren's Staff.")
                .defineInRange("staffManaRegenBonus", 5.0d, 0.0d, 50.0d);
        CHRONOS_WATCH_RADIUS = builder
                .comment("Time dilation spherical bubble radius for the Chronos Dilatation Watch.")
                .defineInRange("chronosWatchRadius", 16.0d, 4.0d, 64.0d);
        CHRONOS_WATCH_DURATION_TICKS = builder
                .comment("Time dilation duration in ticks (200 ticks = 10.0s).")
                .defineInRange("chronosWatchDurationTicks", 200, 20, 400);
        CHRONOS_WATCH_COOLDOWN_SECONDS = builder
                .comment("Cooldown in seconds before the Chronos Dilatation Watch can be reactivated.")
                .defineInRange("chronosWatchCooldownSeconds", 60, 10, 600);
        builder.pop();

        SPEC = builder.build();
    }

    // --- Gargantua Getters ---
    public static double getPullRadius() {
        return SPEC.isLoaded() ? PULL_RADIUS.get() : 100.0d;
    }

    public static double getTidalDamagePercent() {
        return SPEC.isLoaded() ? TIDAL_DAMAGE_PERCENT.get() : 0.08d;
    }

    public static double getBlastDamage() {
        return SPEC.isLoaded() ? BLAST_DAMAGE.get() : 800.0d;
    }

    public static double getBlastRadius() {
        return SPEC.isLoaded() ? BLAST_RADIUS.get() : 32.0d;
    }

    public static double getPullAcceleration() {
        return SPEC.isLoaded() ? PULL_ACCELERATION.get() : 0.12d;
    }

    public static boolean isAutoReconstructBlocks() {
        return SPEC.isLoaded() && AUTO_RECONSTRUCT_BLOCKS.get();
    }

    public static boolean isTearBlocks() {
        return !SPEC.isLoaded() || TEAR_BLOCKS.get();
    }

    public static int getMaxTornDebris() {
        return SPEC.isLoaded() ? MAX_TORN_DEBRIS.get() : 180;
    }

    public static boolean isRespectMobGriefing() {
        return !SPEC.isLoaded() || RESPECT_MOB_GRIEFING.get();
    }

    // --- Flight Getters ---
    public static double getBaseManaCostPerSecond() {
        return SPEC.isLoaded() ? BASE_MANA_COST_PER_SEC.get() : 50.0d;
    }

    public static double getManaReductionPerLevel() {
        return SPEC.isLoaded() ? MANA_REDUCTION_PER_LEVEL.get() : 2.5d;
    }

    public static boolean isSpeedAltitudeScalingEnabled() {
        return !SPEC.isLoaded() || ENABLE_SPEED_ALTITUDE_SCALING.get();
    }

    public static double getSpeedMultiplierExponent() {
        return SPEC.isLoaded() ? SPEED_MULTIPLIER_EXPONENT.get() : 1.8d;
    }

    public static double getMaxSpeedMultiplier() {
        return SPEC.isLoaded() ? MAX_SPEED_MULTIPLIER.get() : 8.0d;
    }

    public static double getAltitudeMultiplierExponent() {
        return SPEC.isLoaded() ? ALTITUDE_MULTIPLIER_EXPONENT.get() : 1.7d;
    }

    public static double getMaxAltitudeMultiplier() {
        return SPEC.isLoaded() ? MAX_ALTITUDE_MULTIPLIER.get() : 15.0d;
    }

    public static int getEmergencyBufferTicks() {
        return SPEC.isLoaded() ? EMERGENCY_BUFFER_TICKS.get() : 70;
    }

    public static boolean isAutoEngageAirborne() {
        return !SPEC.isLoaded() || AUTO_ENGAGE_AIRBORNE.get();
    }

    public static boolean isTelemetryHUDEnabled() {
        return !SPEC.isLoaded() || ENABLE_TELEMETRY_HUD.get();
    }

    // --- Spells & Beams Getters ---
    public static double getBeamStandardRange() {
        return SPEC.isLoaded() ? BEAM_STANDARD_RANGE.get() : 128.0d;
    }

    public static double getBeamLargeRange() {
        return SPEC.isLoaded() ? BEAM_LARGE_RANGE.get() : 192.0d;
    }

    public static double getBeamDamageMultiplier() {
        return SPEC.isLoaded() ? BEAM_DAMAGE_MULTIPLIER.get() : 1.0d;
    }

    public static boolean isDisableShieldOnHit() {
        return !SPEC.isLoaded() || DISABLE_SHIELD_ON_HIT.get();
    }

    public static double getBeamKnockbackFactor() {
        return SPEC.isLoaded() ? BEAM_KNOCKBACK_FACTOR.get() : 1.5d;
    }

    public static int getCorruptedWitherDuration() {
        return SPEC.isLoaded() ? CORRUPTED_WITHER_DURATION.get() : 120;
    }

    public static boolean isRespectFriendlyFire() {
        return !SPEC.isLoaded() || RESPECT_FRIENDLY_FIRE.get();
    }

    public static boolean isBeamDrillBlocks() {
        return !SPEC.isLoaded() || BEAM_DRILL_BLOCKS.get();
    }

    public static double getBeamDrillRadius() {
        return SPEC.isLoaded() ? BEAM_DRILL_RADIUS.get() : 1.25d;
    }

    // --- Barrage Getters ---
    public static double getBarrageHomingStrength() {
        return SPEC.isLoaded() ? BARRAGE_HOMING_STRENGTH.get() : 0.34d;
    }

    public static double getBarrageProjectileSpeed() {
        return SPEC.isLoaded() ? BARRAGE_PROJECTILE_SPEED.get() : 1.75d;
    }

    public static int getBarrageMaxLifetimeTicks() {
        return SPEC.isLoaded() ? BARRAGE_MAX_LIFETIME_TICKS.get() : 60;
    }

    public static int getBarrageFireInterval() {
        return SPEC.isLoaded() ? BARRAGE_FIRE_INTERVAL.get() : 10;
    }

    public static double getBarrageDamageMultiplier() {
        return SPEC.isLoaded() ? BARRAGE_DAMAGE_MULTIPLIER.get() : 1.0d;
    }

    public static double getBarrageBlastRadius() {
        return SPEC.isLoaded() ? BARRAGE_BLAST_RADIUS.get() : 1.2d;
    }

    // --- Defense Barrier Getters ---
    public static double getBarrierCapacityMultiplier() {
        return SPEC.isLoaded() ? BARRIER_CAPACITY_MULTIPLIER.get() : 1.0d;
    }

    public static int getBarrierDurationTicks() {
        return SPEC.isLoaded() ? BARRIER_DURATION_TICKS.get() : 300;
    }

    public static boolean isBarrierDeflectArrows() {
        return !SPEC.isLoaded() || BARRIER_DEFLECT_ARROWS.get();
    }

    public static boolean isBarrierDeflectMagicProjectiles() {
        return !SPEC.isLoaded() || BARRIER_DEFLECT_MAGIC_PROJECTILES.get();
    }

    // --- Qual Boss Getters ---
    public static double getQualBaseHealth() {
        return SPEC.isLoaded() ? QUAL_BASE_HEALTH.get() : 1500.0d;
    }

    public static double getQualHealthPerExtraPlayer() {
        return SPEC.isLoaded() ? QUAL_HEALTH_PER_EXTRA_PLAYER.get() : 200.0d;
    }

    public static double getQualArmor() {
        return SPEC.isLoaded() ? QUAL_ARMOR.get() : 30.0d;
    }

    public static double getQualArmorToughness() {
        return SPEC.isLoaded() ? QUAL_ARMOR_TOUGHNESS.get() : 10.0d;
    }

    public static int getQualRecoveryWindowMinTicks() {
        return SPEC.isLoaded() ? QUAL_RECOVERY_WINDOW_MIN_TICKS.get() : 200;
    }

    public static int getQualRecoveryWindowMaxTicks() {
        return SPEC.isLoaded() ? QUAL_RECOVERY_WINDOW_MAX_TICKS.get() : 240;
    }

    public static double getQualDespawnDistance() {
        return SPEC.isLoaded() ? QUAL_DESPAWN_DISTANCE.get() : 96.0d;
    }

    // --- Artifacts & Relics Getters ---
    public static double getStaffCastSpeedBonus() {
        return SPEC.isLoaded() ? STAFF_CAST_SPEED_BONUS.get() : 0.15d;
    }

    public static double getStaffManaRegenBonus() {
        return SPEC.isLoaded() ? STAFF_MANA_REGEN_BONUS.get() : 5.0d;
    }

    public static double getChronosWatchRadius() {
        return SPEC.isLoaded() ? CHRONOS_WATCH_RADIUS.get() : 16.0d;
    }

    public static int getChronosWatchDurationTicks() {
        return SPEC.isLoaded() ? CHRONOS_WATCH_DURATION_TICKS.get() : 200;
    }

    public static int getChronosWatchCooldownSeconds() {
        return SPEC.isLoaded() ? CHRONOS_WATCH_COOLDOWN_SECONDS.get() : 60;
    }
}
