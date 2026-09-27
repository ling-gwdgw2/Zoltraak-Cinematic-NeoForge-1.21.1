package com.frierenvoid;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class VoidConfig {
   public static final EnumValue<VoidConfig.ShaderCompatibilityMode> SHADER_COMPATIBILITY;
   public static final ModConfigSpec SERVER;
   public static final ModConfigSpec CLIENT;
   public static final BooleanValue TERRAIN;
   public static final BooleanValue ZOLTRAAK_TERRAIN;
   public static final BooleanValue CUTSCENE;
   public static final DoubleValue FLASH;
   public static final IntValue MAX_BLOCKS;
   public static final IntValue BLOCKS_PER_PULSE;
   public static final DoubleValue BLOCK_HARDNESS;
   public static final DoubleValue RADIUS;
   public static final DoubleValue DAMAGE;
   public static final DoubleValue PULL;
   public static final DoubleValue VOLUME;
   public static final DoubleValue SHAKE;
   public static final BooleanValue PVP;
   public static final BooleanValue PASSIVE;
   public static final BooleanValue PROJECTILES;
   public static final BooleanValue ATMOSPHERE;
   public static final BooleanValue REFRACTION;
   public static final IntValue MAX_ACTIVE;
   public static final IntValue MAX_TARGETS;
   public static final IntValue QUALITY;

   private VoidConfig() {
   }

   static {
      Builder s = new Builder();
      RADIUS = s.comment("Attraction radius in blocks.").defineInRange("radius", 16.0, 6.0, 32.0);
      TERRAIN = s.comment("Real terrain removal: blocks rise into the singularity and are consumed without drops. Preview never alters terrain.")
         .define("absorbTerrain", true);
      ZOLTRAAK_TERRAIN = s.comment(
            "Large Zoltraak excavates ordinary blocks without drops. Obsidian-strength, unbreakable, container and protected blocks stop it. Maximum 4096 blocks per cast."
         )
         .define("largeZoltraakBreaksTerrain", true);
      MAX_BLOCKS = s.defineInRange("maxBlocksPerCast", 192, 0, 1024);
      BLOCKS_PER_PULSE = s.comment("Every four ticks, with bounded surface sampling.").defineInRange("blocksPerPulse", 5, 1, 16);
      BLOCK_HARDNESS = s.comment("Protect block entities, fluids and unbreakable blocks regardless of hardness.")
         .defineInRange("maxBlockHardness", 5.0, 0.0, 50.0);
      DAMAGE = s.defineInRange("damageMultiplier", 1.0, 0.0, 10.0);
      PULL = s.defineInRange("pullStrength", 1.0, 0.0, 3.0);
      PVP = s.comment("Also respects server PVP and teams.").define("allowPvp", false);
      PASSIVE = s.comment("Allow singularity to pull and damage passive mobs. Aimed Zoltraak spells can always hit non-allied passive mobs.")
         .define("affectPassiveMobs", false);
      PROJECTILES = s.comment("Deflect and consume hostile projectiles. Never consumes dropped items.").define("affectProjectiles", true);
      MAX_ACTIVE = s.defineInRange("maxActivePerDimension", 4, 1, 16);
      MAX_TARGETS = s.defineInRange("maxTargetsPerPulse", 64, 8, 256);
      SERVER = s.build();
      Builder c = new Builder();
      CUTSCENE = c.comment("Automatic local camera sequence for your own spell. V, movement, damage or a menu exits immediately.")
         .define("cinematicCamera", true);
      FLASH = c.comment("Brief white/violet screen flash at detonation. 0 disables it.").defineInRange("detonationFlash", 0.85, 0.0, 1.0);
      QUALITY = c.comment("0 low, 1 balanced, 2 cinematic. Geometry only, no gameplay difference.").defineInRange("quality", 2, 0, 2);
      VOLUME = c.defineInRange("soundVolume", 0.8, 0.0, 1.0);
      SHAKE = c.comment("0 disables camera shake.").defineInRange("cameraShake", 0.35, 0.0, 1.0);
      ATMOSPHERE = c.define("screenAtmosphere", true);
      REFRACTION = c.comment("Local scene refraction. Disable if an external shader pack conflicts with framebuffer effects.")
         .define("gravitationalLensing", true);
      SHADER_COMPATIBILITY = c.comment(
            "Zoltraak shader-pack rendering: AUTO detects Iris/Oculus, ON draws the complete original effects after world composition, OFF uses normal world timing."
         )
         .defineEnum("zoltraakShaderCompatibility", VoidConfig.ShaderCompatibilityMode.AUTO);
      CLIENT = c.build();
   }

   public enum ShaderCompatibilityMode {
      AUTO,
      ON,
      OFF;
   }
}
