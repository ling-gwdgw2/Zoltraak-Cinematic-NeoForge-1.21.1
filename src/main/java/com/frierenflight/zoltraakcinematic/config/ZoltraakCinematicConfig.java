package com.frierenflight.zoltraakcinematic.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration for Zoltraak: Cinematic Edition.
 * Manages physical block interaction, Pristine World Auto-Reconstruction, and griefing safety.
 */
public class ZoltraakCinematicConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue AUTO_RECONSTRUCT_BLOCKS;
    public static final ModConfigSpec.BooleanValue TEAR_BLOCKS;
    public static final ModConfigSpec.BooleanValue RESPECT_MOB_GRIEFING;

    public static final ModConfigSpec.DoubleValue PULL_RADIUS;
    public static final ModConfigSpec.DoubleValue TIDAL_DAMAGE_PERCENT;
    public static final ModConfigSpec.DoubleValue BLAST_DAMAGE;
    public static final ModConfigSpec.DoubleValue BLAST_RADIUS;
    public static final ModConfigSpec.DoubleValue PULL_ACCELERATION;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Gargantua Singularity / Black Hole Settings").push("gargantua");

        PULL_RADIUS = builder
                .comment("Gravitational attraction pull radius in blocks.",
                         "All living entities, projectiles, and debris within this spherical radius are inexorably pulled into the singularity.")
                .defineInRange("pullRadius", 100.0d, 10.0d, 300.0d);

        TIDAL_DAMAGE_PERCENT = builder
                .comment("Tidal disruption damage dealt to living entities inside the event horizon, expressed as a fraction of target Max HP (e.g. 0.20 = 20% Max HP every 0.4s).")
                .defineInRange("tidalDamagePercent", 0.20d, 0.01d, 1.0d);

        BLAST_DAMAGE = builder
                .comment("Base damage dealt by the final Supernova detonation at tick 1100 (55.0s).")
                .defineInRange("blastDamage", 800.0d, 10.0d, 10000.0d);

        BLAST_RADIUS = builder
                .comment("Explosion blast radius in blocks for the final Supernova detonation.")
                .defineInRange("blastRadius", 32.0d, 5.0d, 200.0d);

        PULL_ACCELERATION = builder
                .comment("Base gravitational pull acceleration strength applied to entities.")
                .defineInRange("pullAcceleration", 0.12d, 0.01d, 2.0d);

        AUTO_RECONSTRUCT_BLOCKS = builder
                .comment("Whether blocks torn and consumed by Gargantua should automatically reconstruct after the blast (Pristine World Auto-Reconstruction).",
                         "true: All torn blocks smoothly reconstruct bottom-up with reverse portal visual effects and chime sounds, leaving terrain 100% pristine.",
                         "false: Blocks consumed by the singularity remain destroyed, leaving a permanent crater after detonation.")
                .define("autoReconstructBlocks", true);

        TEAR_BLOCKS = builder
                .comment("Whether Gargantua should physically tear blocks from the terrain into the orbiting accretion disk vortex.",
                         "true: Blocks within range are torn into swirling debris and swallowed into the event horizon.",
                         "false: No blocks are torn from the world (pure visual particles and entity pull only). Ideal for claim-protected servers or creative plots.")
                .define("tearBlocks", true);

        RESPECT_MOB_GRIEFING = builder
                .comment("Whether Gargantua respects the vanilla 'mobGriefing' gamerule.",
                         "true: If mobGriefing is false in the world, no blocks will be torn.",
                         "false: Gargantua will tear blocks regardless of the mobGriefing gamerule (subject to tearBlocks).")
                .define("respectMobGriefing", true);

        builder.pop();

        SPEC = builder.build();
    }

    public static double getPullRadius() {
        return SPEC.isLoaded() ? PULL_RADIUS.get() : 100.0d;
    }

    public static double getTidalDamagePercent() {
        return SPEC.isLoaded() ? TIDAL_DAMAGE_PERCENT.get() : 0.20d;
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
        return !SPEC.isLoaded() || AUTO_RECONSTRUCT_BLOCKS.get();
    }

    public static boolean isTearBlocks() {
        return !SPEC.isLoaded() || TEAR_BLOCKS.get();
    }

    public static boolean isRespectMobGriefing() {
        return !SPEC.isLoaded() || RESPECT_MOB_GRIEFING.get();
    }
}
