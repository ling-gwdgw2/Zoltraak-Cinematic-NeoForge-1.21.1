package com.frierenflight.zoltraakcinematic.client.cinematic;

import com.frierenflight.zoltraakcinematic.client.renderer.GargantuaPostProcessor;
import com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicClientConfig;
import com.frierenflight.zoltraakcinematic.entity.GargantuaEntity;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 🌌 GargantuaScreenFx
 * Handles full-screen post-processing overlays and acoustic drama:
 * - Act 1: Spacetime rupture flash & chromatic edge dispersion
 * - Act 3: Acoustic vacuum silence & atmospheric darkness vignette
 * - Act 4: Supernova blinding white flash, proximity blast & tinnitus
 */
public final class GargantuaScreenFx {

    private static int supernovaFlashTicks = 0;
    private static int maxFlashTicks = 0;
    private static float flashIntensity = 0.0f;

    private static float currentVignetteAlpha = 0.0f;
    private static final Set<Integer> BLAST_OBSERVED_ENTITIES = new HashSet<>();
    private static final Set<Integer> TEAR_OBSERVED_ENTITIES = new HashSet<>();

    private GargantuaScreenFx() {}

    public static void clientTick(Minecraft mc) {
        if (supernovaFlashTicks > 0) {
            supernovaFlashTicks--;
            if (supernovaFlashTicks == 0) {
                flashIntensity = 0.0f;
                maxFlashTicks = 0;
            }
        }

        if (mc.level == null || mc.player == null) {
            BLAST_OBSERVED_ENTITIES.clear();
            TEAR_OBSERVED_ENTITIES.clear();
            currentVignetteAlpha = 0.0f;
            return;
        }

        List<GargantuaEntity> active = GargantuaPostProcessor.getActiveClientEntities(mc);
        float targetVignette = 0.0f;

        for (GargantuaEntity g : active) {
            if (!g.isAlive()) continue;

            int id = g.getId();
            float visualAge = g.getVisualAgeTicks(0.0f);
            int blastTick = g.getBlastTick();
            int critStart = g.getCriticalStartTick();
            int tearEnd = g.getTearEndTick();
            double dist = g.distanceTo(mc.player);

            // Act 1: Rupture Flash on spawn
            if (visualAge >= 0.0f && visualAge <= 8.0f && !TEAR_OBSERVED_ENTITIES.contains(id)) {
                TEAR_OBSERVED_ENTITIES.add(id);
                if (dist <= 100.0d) {
                    float distFactor = (float) Mth.clamp(1.0 - dist / 100.0d, 0.2, 1.0);
                    triggerFlash(12, 0.65f * distFactor);
                }
            }

            // Act 3: Critical Implosion (Silence & Darkness)
            if (visualAge > (float) critStart && visualAge <= (float) blastTick) {
                if (dist <= 120.0d) {
                    float span = Math.max(1.0f, (float) (blastTick - critStart));
                    float progress = Mth.clamp((visualAge - (float) critStart) / span, 0.0f, 1.0f);
                    float distFactor = (float) Mth.clamp(1.0 - dist / 120.0d, 0.1, 1.0);
                    targetVignette = Math.max(targetVignette, (progress * 0.72f) * distFactor);
                }
            }

            // Act 4: Supernova Detonation (Blast Tick)
            if (visualAge >= (float) blastTick && !BLAST_OBSERVED_ENTITIES.contains(id)) {
                BLAST_OBSERVED_ENTITIES.add(id);
                if (dist <= 160.0d) {
                    float distFactor = (float) Mth.clamp(1.0 - dist / 160.0d, 0.15, 1.0);
                    triggerFlash(45, 1.0f * distFactor);

                    // Play explosive detonation & tinnitus sounds
                    if (ZoltraakCinematicClientConfig.isTinnitusSoundEnabled()) {
                        float vol = (float) ZoltraakCinematicClientConfig.getTinnitusVolumeMultiplier();
                        mc.getSoundManager().play(SimpleSoundInstance.forUI(
                                ModCinematicSounds.TINNITUS.get(),
                                1.0f,
                                0.85f * vol * distFactor
                        ));
                    }
                }
            }
        }

        // Smoothly interpolate vignette
        currentVignetteAlpha = Mth.lerp(0.15f, currentVignetteAlpha, targetVignette);

        // Clean up dead entity IDs
        BLAST_OBSERVED_ENTITIES.removeIf(id -> mc.level.getEntity(id) == null);
        TEAR_OBSERVED_ENTITIES.removeIf(id -> mc.level.getEntity(id) == null);
    }

    public static void triggerFlash(int duration, float intensity) {
        if (!ZoltraakCinematicClientConfig.isScreenFlashEnabled()) return;
        supernovaFlashTicks = duration;
        maxFlashTicks = duration;
        flashIntensity = intensity * (float) ZoltraakCinematicClientConfig.getScreenFlashIntensity();
    }

    public static void renderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();

        // 1. Act 3: Dark Cosmic Implosion Vignette (Darkening the edges of the universe)
        if (currentVignetteAlpha > 0.01f) {
            int vigAlpha = (int) (currentVignetteAlpha * 255.0f);
            int vigColor = (vigAlpha << 24) | 0x05010B; // Deep obsidian violet
            // Border darkening
            int border = Math.max(16, (int) (w * 0.15f));
            graphics.fill(0, 0, w, border, vigColor);
            graphics.fill(0, h - border, w, h, vigColor);
            graphics.fill(0, border, border, h - border, vigColor);
            graphics.fill(w - border, border, w, h - border, vigColor);
        }

        // 2. Act 4: Supernova Pure White Blinding Flash
        if (supernovaFlashTicks > 0 && maxFlashTicks > 0) {
            float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            float progress = Math.max(0.0f, (float) supernovaFlashTicks - partial) / (float) maxFlashTicks * flashIntensity;
            int alpha = (int) (Mth.clamp(progress, 0.0f, 1.0f) * 255.0f);

            if (alpha > 0) {
                // Pure white blinding blast
                int flashColor = (alpha << 24) | 0xFFFFFF;
                graphics.fill(0, 0, w, h, flashColor);

                // Chromatic aberration fringes on edge
                int fringeAlpha = (int) (progress * 110.0f);
                if (fringeAlpha > 0) {
                    graphics.fill(0, 0, 10, h, (fringeAlpha << 24) | 0x00E5FF);
                    graphics.fill(w - 10, 0, w, h, (fringeAlpha << 24) | 0xC040FF);
                }
            }
        }
    }
}
