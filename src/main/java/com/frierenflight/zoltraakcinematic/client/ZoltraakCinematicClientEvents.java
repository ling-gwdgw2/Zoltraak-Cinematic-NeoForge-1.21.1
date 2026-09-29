package com.frierenflight.zoltraakcinematic.client;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.client.renderer.ZoltraakPhotonBeamRenderer;
import com.frierenflight.zoltraakcinematic.entity.ZoltraakCinematicBeamEntity;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * ZoltraakCinematicClientEvents
 * Handles client-side entity renderer registration, dynamic camera kickback/shake,
 * KilaGraph material pre-compilation, and first-person cinematic exposure flash.
 */
public class ZoltraakCinematicClientEvents {

    private static int shakeDuration = 0;
    private static float shakeIntensity = 0.0f;

    private static int flashDuration = 0;
    private static float flashIntensity = 0.0f;

    private static int barrageRumbleTicks = 0;
    private static net.minecraft.client.CameraType originalCameraType = null;
    private static boolean forcedThirdPerson = false;
    private static int cameraRestoreDelay = 0;

    public static final net.minecraft.client.KeyMapping KEY_DEFENSE = new net.minecraft.client.KeyMapping(
            "key.zoltraak_cinematic.defense",
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
            com.mojang.blaze3d.platform.InputConstants.KEY_X,
            "key.categories.zoltraak_cinematic"
    );
    private static long lastDefensePressTime = 0;

    public static final net.minecraft.client.KeyMapping KEY_FLIGHT = new net.minecraft.client.KeyMapping(
            "key.zoltraak_cinematic.flight",
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
            com.mojang.blaze3d.platform.InputConstants.KEY_V,
            "key.categories.zoltraak_cinematic"
    );

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ZoltraakCinematicClientEvents::onRegisterEntityRenderers);
        modEventBus.addListener(com.frierenflight.zoltraakcinematic.client.renderer.ZoltraakCinematicShaders::onRegisterShaders);
        modEventBus.addListener(ZoltraakCinematicClientEvents::onRegisterKeyMappings);

        // Register gameplay client events on the NeoForge Event Bus
        NeoForge.EVENT_BUS.addListener(ZoltraakCinematicClientEvents::onCameraAngles);
        NeoForge.EVENT_BUS.addListener(ZoltraakCinematicClientEvents::onRenderGui);
        NeoForge.EVENT_BUS.addListener(ZoltraakCinematicClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ZoltraakCinematicClientEvents::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(com.frierenflight.zoltraakcinematic.client.renderer.ZoltraakBarrageArrayRenderer::renderArray);
    }

    public static void onRegisterKeyMappings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(KEY_DEFENSE);
        event.register(KEY_FLIGHT);
    }

    public static void onLevelUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            shakeDuration = 0;
            shakeIntensity = 0.0f;
            flashDuration = 0;
            flashIntensity = 0.0f;
            barrageRumbleTicks = 0;
            if (forcedThirdPerson && originalCameraType != null) {
                Minecraft.getInstance().options.setCameraType(originalCameraType);
                originalCameraType = null;
            }
            forcedThirdPerson = false;
            cameraRestoreDelay = 0;
            com.frierenflight.zoltraakcinematic.client.renderer.SceneLens.release();
        }
    }

    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModCinematicEntities.ZOLTRAAK_BEAM.get(), ZoltraakPhotonBeamRenderer::new);
        event.registerEntityRenderer(ModCinematicEntities.ZOLTRAAK_BARRAGE_PROJECTILE.get(), com.frierenflight.zoltraakcinematic.client.renderer.ZoltraakBarrageProjectileRenderer::new);
        event.registerEntityRenderer(ModCinematicEntities.DEFENSE_BARRIER.get(), com.frierenflight.zoltraakcinematic.client.renderer.DefenseBarrierRenderer::new);
        event.registerEntityRenderer(ModCinematicEntities.QUAL_BOSS.get(), com.frierenflight.zoltraakcinematic.client.renderer.QualBossRenderer::new);
    }

    /**
     * Triggers cinematic screen kickback and flash when Zoltraak discharges (Tick 8).
     */
    public static void triggerFireShock(Entity beam, Vec3 beamPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double dist = mc.player.position().distanceTo(beamPos);
        boolean isCaster = (beam instanceof ZoltraakCinematicBeamEntity b && b.getOwner() == mc.player) || dist < 2.5;

        if (isCaster) {
            shakeDuration = 6; // 6 ticks (~0.30s)
            shakeIntensity = 1.0f;
            flashDuration = 3; // 3 ticks (~0.15s)
            flashIntensity = 1.0f;
        } else if (dist < 32.0) {
            float factor = (float) (1.0 - dist / 32.0);
            shakeDuration = Math.max(shakeDuration, (int) (4 * factor));
            shakeIntensity = Math.max(shakeIntensity, 0.65f * factor);
        }
    }

    /**
     * Triggers continuous micro-rumble screen vibration during rapid barrage fire.
     */
    public static void triggerBarrageMicroRumble() {
        barrageRumbleTicks = Math.max(barrageRumbleTicks, 3);
    }

    /**
     * Applies high-speed camera kickback & harmonic screen vibration during spell release.
     */
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shakeDuration > 0) {
            float partial = (float) event.getPartialTick();
            float t = (6 - shakeDuration) + partial;
            float progress = Math.max(0.0f, (shakeDuration - partial) / 6.0f) * shakeIntensity;

            // Sharp upward kick from immense spell recoil, decaying smoothly
            float pitchKick = -2.8f * (float) Math.exp(-t * 0.9) * shakeIntensity;

            // Multi-frequency harmonic jitter for cinematic impact feel
            float pitchJitter = (float) Math.sin(t * 26.0) * 0.95f * progress;
            float yawJitter   = (float) Math.cos(t * 21.0) * 0.75f * progress;
            float rollJitter  = (float) Math.sin(t * 16.0) * 1.35f * progress;

            event.setPitch(event.getPitch() + pitchKick + pitchJitter);
            event.setYaw(event.getYaw() + yawJitter);
            event.setRoll(event.getRoll() + rollJitter);
        }

        if (barrageRumbleTicks > 0) {
            float rumbleTime = (float) event.getPartialTick() + (3 - barrageRumbleTicks);
            float rumblePitch = (float) Math.sin(rumbleTime * 34.0) * 0.16f;
            float rumbleYaw   = (float) Math.cos(rumbleTime * 28.0) * 0.13f;
            float rumbleRoll  = (float) Math.sin(rumbleTime * 22.0) * 0.10f;

            event.setPitch(event.getPitch() + rumblePitch);
            event.setYaw(event.getYaw() + rumbleYaw);
            event.setRoll(event.getRoll() + rumbleRoll);
        }
    }

    /**
     * Renders a brief high-exposure blinding white flash and chromatic edge fringe in First-Person.
     */
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (flashDuration > 0) {
            float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            float progress = Math.max(0.0f, (flashDuration - partial) / 3.0f) * flashIntensity;
            int alpha = (int) (progress * 135);
            if (alpha > 0) {
                int w = event.getGuiGraphics().guiWidth();
                int h = event.getGuiGraphics().guiHeight();

                // High-exposure blinding white/cyan flash overlay
                int flashColor = (alpha << 24) | 0xE8F8FF;
                event.getGuiGraphics().fill(0, 0, w, h, flashColor);

                // Subtle chromatic fringe at screen borders (cyan left, violet right)
                int borderAlpha = (int) (progress * 75);
                if (borderAlpha > 0) {
                    int cyanEdge = (borderAlpha << 24) | 0x00E5FF;
                    int violetEdge = (borderAlpha << 24) | 0xC040FF;
                    event.getGuiGraphics().fill(0, 0, 8, h, cyanEdge);
                    event.getGuiGraphics().fill(w - 8, 0, w, h, violetEdge);
                }
            }
        }
    }

    /**
     * Decays active camera shake and flash timers every client tick and observes entities for discharge VFX.
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        if (shakeDuration > 0) {
            shakeDuration--;
            if (shakeDuration == 0) shakeIntensity = 0.0f;
        }
        if (flashDuration > 0) {
            flashDuration--;
            if (flashDuration == 0) flashIntensity = 0.0f;
        }
        if (barrageRumbleTicks > 0) {
            barrageRumbleTicks--;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && !mc.isPaused()) {
            if (mc.screen == null && mc.player != null) {
                while (KEY_DEFENSE.consumeClick()) {
                    long now = System.currentTimeMillis();
                    long elapsed = now - lastDefensePressTime;

                    int mode;
                    if (mc.player.isCrouching()) {
                        // Sneak + X -> Instant 360-degree Dome shortcut!
                        mode = com.frierenflight.zoltraakcinematic.entity.DefenseBarrierEntity.MODE_DOME;
                        lastDefensePressTime = 0;
                    } else if (elapsed <= 350) {
                        // Double tap X -> 360-degree Geodesic Honeycomb Dome!
                        mode = com.frierenflight.zoltraakcinematic.entity.DefenseBarrierEntity.MODE_DOME;
                        lastDefensePressTime = 0;
                    } else {
                        // Single tap X -> Flat 19-cell Honeycomb Shield in front!
                        mode = com.frierenflight.zoltraakcinematic.entity.DefenseBarrierEntity.MODE_DIRECTIONAL;
                        lastDefensePressTime = now;
                    }

                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                            new com.frierenflight.zoltraakcinematic.network.DefenseCastPayload(mode)
                    );
                }

                while (KEY_FLIGHT.consumeClick()) {
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                            new com.frierenflight.zoltraakcinematic.network.FlightTogglePayload()
                    );
                }

                // Seamless zero-latency client flight re-engagement when airborne
                if (mc.player.hasEffect(com.frierenflight.zoltraakcinematic.registry.ModCinematicMobEffects.FLIGHT)) {
                    if (!mc.player.onGround() && !mc.player.isInWater() && !mc.player.isInLava() && !mc.player.getAbilities().flying) {
                        mc.player.getAbilities().flying = true;
                    }
                }
            }

            // Automatic Third-Person Perspective Switching for Barrage Casting
            boolean isBarrageCasting = false;
            if (mc.player != null && io.redspace.ironsspellbooks.player.ClientMagicData.isCasting()) {
                String spellId = io.redspace.ironsspellbooks.player.ClientMagicData.getCastingSpellId();
                if (spellId != null && (spellId.endsWith("zoltraak_barrage") || spellId.endsWith("corrupted_barrage"))) {
                    isBarrageCasting = true;
                }
            }

            if (isBarrageCasting) {
                if (!forcedThirdPerson) {
                    originalCameraType = mc.options.getCameraType();
                    // Force cinematic third-person back view so player is framed by the 24-circle matrix
                    if (originalCameraType.isFirstPerson()) {
                        mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                    }
                    forcedThirdPerson = true;
                }
                cameraRestoreDelay = 10; // Keep delay primed while casting
            } else {
                if (forcedThirdPerson) {
                    if (cameraRestoreDelay > 0) {
                        cameraRestoreDelay--;
                    } else {
                        if (originalCameraType != null) {
                            mc.options.setCameraType(originalCameraType);
                            originalCameraType = null;
                        }
                        forcedThirdPerson = false;
                    }
                }
            }

            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof ZoltraakCinematicBeamEntity beam && !beam.isRemoved()) {
                    if (beam.tickCount == ZoltraakCinematicBeamEntity.FIRE_TICK) {
                        triggerFireShock(beam, beam.position());
                    }
                } else if (entity instanceof com.frierenflight.zoltraakcinematic.entity.ZoltraakBarrageProjectileEntity proj && !proj.isRemoved()) {
                    if (proj.markClientEffectsTriggered()) {
                        Entity shooter = proj.getOwner();
                        if (shooter instanceof net.minecraft.world.entity.LivingEntity living) {
                            com.frierenflight.zoltraakcinematic.client.renderer.ZoltraakBarrageArrayRenderer.onCircleFired(living, proj.getCircleIndex());
                        }
                        if (shooter == mc.player) {
                            triggerBarrageMicroRumble();
                        }
                    }
                }
            }
        }

        com.frierenflight.zoltraakcinematic.client.renderer.ZoltraakBarrageArrayRenderer.clientTick();
    }
}
