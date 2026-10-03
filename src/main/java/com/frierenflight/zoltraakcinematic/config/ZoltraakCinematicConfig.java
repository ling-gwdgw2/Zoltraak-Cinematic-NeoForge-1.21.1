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

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Gargantua Singularity / Black Hole Settings").push("gargantua");

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
}
