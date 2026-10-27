package com.frierenflight.zoltraakcinematic.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client Configuration for Zoltraak: Cinematic Edition.
 * Manages visual accessibility, screen flash/shake, shader performance, and audio preferences.
 */
public class ZoltraakCinematicClientConfig {
    public static final ModConfigSpec SPEC;

    // Visual Accessibility
    public static final ModConfigSpec.BooleanValue ENABLE_SCREEN_FLASH;
    public static final ModConfigSpec.DoubleValue SCREEN_FLASH_INTENSITY;
    public static final ModConfigSpec.BooleanValue ENABLE_CAMERA_SHAKE;
    public static final ModConfigSpec.DoubleValue CAMERA_SHAKE_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue ENABLE_FLIGHT_CAMERA_ROLL;
    public static final ModConfigSpec.BooleanValue ENABLE_CINEMATIC_CAMERA;

    // Shader Performance
    public static final ModConfigSpec.ConfigValue<String> BLACK_HOLE_RAYMARCH_STEPS;
    public static final ModConfigSpec.DoubleValue ACCRETION_DISK_OUTER_RADIUS;
    public static final ModConfigSpec.BooleanValue ENABLE_ANAMORPHIC_LENS_FLARES;
    public static final ModConfigSpec.BooleanValue ENABLE_GRAVITATIONAL_LENSING;
    public static final ModConfigSpec.BooleanValue ENABLE_CHROMATIC_ABERRATION;

    // Audio Accessibility
    public static final ModConfigSpec.BooleanValue ENABLE_TINNITUS_SOUND;
    public static final ModConfigSpec.DoubleValue TINNITUS_VOLUME_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue CINEMATIC_MASTER_AUDIO_VOLUME;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Visual Accessibility & Screen Dynamics").push("visual_accessibility");

        ENABLE_SCREEN_FLASH = builder
                .comment("Master toggle for full-screen blinding white flash overlays (Supernova, Great Zoltraak).",
                         "Set to false for photosensitive or epileptic players.")
                .define("enableScreenFlash", true);

        SCREEN_FLASH_INTENSITY = builder
                .comment("Opacity/brightness intensity multiplier for screen flash overlays (0.0 = completely off, 1.0 = full intensity).")
                .defineInRange("screenFlashIntensity", 1.0d, 0.0d, 1.0d);

        ENABLE_CAMERA_SHAKE = builder
                .comment("Whether camera shake vibrations occur during massive detonations and high-energy spell releases.")
                .define("enableCameraShake", true);

        CAMERA_SHAKE_MULTIPLIER = builder
                .comment("Camera shake intensity multiplier (0.0 = static camera, 1.0 = standard cinematic rumble, 2.0 = heavy rumble).")
                .defineInRange("cameraShakeMultiplier", 1.0d, 0.0d, 3.0d);

        ENABLE_FLIGHT_CAMERA_ROLL = builder
                .comment("Whether the camera tilts/banks with aerodynamic roll (+/- 6.5 deg) when turning during flight.")
                .define("enableFlightCameraRoll", true);

        ENABLE_CINEMATIC_CAMERA = builder
                .comment("Whether 3D cinematic camera choreography (Dolly Zoom, orbital tracking, supernova wide shot) plays during Gargantua casting and active singularity.",
                         "Press Jump (Spacebar) in-game to quickly skip an active cinematic camera cutscene.")
                .define("enableCinematicCamera", true);

        builder.pop();

        builder.comment("Shader Performance & Post-Processing").push("shader_performance");

        BLACK_HOLE_RAYMARCH_STEPS = builder
                .comment("Accretion disk geodesic leapfrog raymarching quality preset.",
                         "Allowed values: LOW (32 steps), MEDIUM (64 steps), HIGH (96 steps), ULTRA (128 steps).",
                         "Use LOW for integrated graphics (iGPUs) or handheld devices like Steam Deck.")
                .define("blackHoleRaymarchSteps", "MEDIUM");

        ACCRETION_DISK_OUTER_RADIUS = builder
                .comment("Outer radius of Gargantua's accretion disk in gravitational radii (r_g).",
                         "Higher values create a wider, more majestic interstellar gas disk (Default: 22.0, Original: 13.5).")
                .defineInRange("accretionDiskOuterRadius", 22.0d, 8.0d, 40.0d);

        ENABLE_ANAMORPHIC_LENS_FLARES = builder
                .comment("Whether horizontal 16-sample Gaussian anamorphic streak flares render on beam cores.")
                .define("enableAnamorphicLensFlares", true);

        ENABLE_GRAVITATIONAL_LENSING = builder
                .comment("Whether fullscreen spacetime distortion/gravitational lensing renders around the event horizon.")
                .define("enableGravitationalLensing", true);

        ENABLE_CHROMATIC_ABERRATION = builder
                .comment("Whether chromatic dispersion and optical fringe render on high-energy beam impacts and screen borders.")
                .define("enableChromaticAberration", true);

        builder.pop();

        builder.comment("Audio Accessibility & Dynamics").push("audio_accessibility");

        ENABLE_TINNITUS_SOUND = builder
                .comment("Whether high-frequency ear ringing (tinnitus sound effect) plays after massive proximity detonations.")
                .define("enableTinnitusSound", true);

        TINNITUS_VOLUME_MULTIPLIER = builder
                .comment("Volume scaling multiplier specifically for the tinnitus ear ringing sound effect.")
                .defineInRange("tinnitusVolumeMultiplier", 1.0d, 0.0d, 2.0d);

        CINEMATIC_MASTER_AUDIO_VOLUME = builder
                .comment("Master volume scaling multiplier for all custom cinematic spells and boss sounds.")
                .defineInRange("cinematicMasterAudioVolume", 1.0d, 0.0d, 2.0d);

        builder.pop();

        SPEC = builder.build();
    }

    // --- Safe Getters with Defaults ---

    public static boolean isScreenFlashEnabled() {
        return !SPEC.isLoaded() || ENABLE_SCREEN_FLASH.get();
    }

    public static float getScreenFlashIntensity() {
        return SPEC.isLoaded() ? SCREEN_FLASH_INTENSITY.get().floatValue() : 1.0f;
    }

    public static boolean isCameraShakeEnabled() {
        return !SPEC.isLoaded() || ENABLE_CAMERA_SHAKE.get();
    }

    public static float getCameraShakeMultiplier() {
        return SPEC.isLoaded() ? CAMERA_SHAKE_MULTIPLIER.get().floatValue() : 1.0f;
    }

    public static boolean isFlightCameraRollEnabled() {
        return !SPEC.isLoaded() || ENABLE_FLIGHT_CAMERA_ROLL.get();
    }

    public static boolean isCinematicCameraEnabled() {
        return !SPEC.isLoaded() || ENABLE_CINEMATIC_CAMERA.get();
    }

    public static String getBlackHoleRaymarchStepsPreset() {
        return SPEC.isLoaded() ? BLACK_HOLE_RAYMARCH_STEPS.get() : "MEDIUM";
    }

    public static int getRaymarchStepCount() {
        String preset = getBlackHoleRaymarchStepsPreset().toUpperCase();
        return switch (preset) {
            case "LOW" -> 64;
            case "HIGH" -> 112;
            case "ULTRA" -> 128;
            default -> 96; // MEDIUM
        };
    }

    public static float getAccretionDiskOuterRadius() {
        return SPEC.isLoaded() ? ACCRETION_DISK_OUTER_RADIUS.get().floatValue() : 22.0f;
    }

    public static boolean isAnamorphicLensFlaresEnabled() {
        return !SPEC.isLoaded() || ENABLE_ANAMORPHIC_LENS_FLARES.get();
    }

    public static boolean isGravitationalLensingEnabled() {
        return !SPEC.isLoaded() || ENABLE_GRAVITATIONAL_LENSING.get();
    }

    public static boolean isChromaticAberrationEnabled() {
        return !SPEC.isLoaded() || ENABLE_CHROMATIC_ABERRATION.get();
    }

    public static boolean isTinnitusSoundEnabled() {
        return !SPEC.isLoaded() || ENABLE_TINNITUS_SOUND.get();
    }

    public static float getTinnitusVolumeMultiplier() {
        return SPEC.isLoaded() ? TINNITUS_VOLUME_MULTIPLIER.get().floatValue() : 1.0f;
    }

    public static float getCinematicMasterAudioVolume() {
        return SPEC.isLoaded() ? CINEMATIC_MASTER_AUDIO_VOLUME.get().floatValue() : 1.0f;
    }
}
