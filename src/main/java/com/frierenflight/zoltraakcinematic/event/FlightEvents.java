package com.frierenflight.zoltraakcinematic.event;

import com.frierenflight.zoltraakcinematic.registry.ModCinematicMobEffects;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FlightEvents
 * Handles continuous flight mechanics, exponential speed/altitude mana consumption,
 * ability synchronization, particle trails, fall-damage immunity, and safety parachutes.
 */
public final class FlightEvents {
    private static final Set<UUID> activeFlyers = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> landingProtection = ConcurrentHashMap.newKeySet();

    public static void register() {
        NeoForge.EVENT_BUS.addListener(FlightEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, FlightEvents::onLivingIncomingDamage);
        NeoForge.EVENT_BUS.addListener(FlightEvents::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(FlightEvents::onPlayerLoggedIn);
    }

    public static void enableFlight(ServerPlayer player) {
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.fallDistance = 0.0f;
        activeFlyers.add(player.getUUID());
        landingProtection.remove(player.getUUID());

        if (player.onGround()) {
            player.setDeltaMovement(player.getDeltaMovement().add(0, 0.45, 0));
            player.hurtMarked = true;
        }
    }

    public static void disableFlight(ServerPlayer player) {
        activeFlyers.remove(player.getUUID());
        landingProtection.add(player.getUUID());

        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
            player.getAbilities().mayfly = false;
            player.onUpdateAbilities();
        }
        player.fallDistance = 0.0f;
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        activeFlyers.remove(event.getEntity().getUUID());
        // Note: Retain landingProtection across logout so players reconnecting in mid-air do not plunge to their death!
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!player.onGround()) {
                landingProtection.add(player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypes.FALL)) {
            if (event.getEntity() instanceof ServerPlayer player) {
                if (player.hasEffect(ModCinematicMobEffects.FLIGHT) || activeFlyers.contains(player.getUUID()) || landingProtection.remove(player.getUUID())) {
                    event.setCanceled(true);
                    player.fallDistance = 0.0f;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Clear landing protection once player is grounded
        if (player.onGround()) {
            landingProtection.remove(player.getUUID());
        }

        if (player.hasEffect(ModCinematicMobEffects.FLIGHT)) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            activeFlyers.add(player.getUUID());

            boolean isFlying = player.getAbilities().flying;
            boolean isAirborne = !player.onGround() && !player.isInWater() && !player.isInLava();

            // Seamless auto-float engagement when in mid-air
            if (com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.isAutoEngageAirborne() && isAirborne && !isFlying) {
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                isFlying = true;
            }

            if (isFlying || isAirborne) {
                player.fallDistance = 0.0f;

                int spellLevel = player.getEffect(ModCinematicMobEffects.FLIGHT).getAmplifier() + 1;

                // Configurable Base consumption & scaling
                float basePerSec = (float) Math.max(1.0, com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.getBaseManaCostPerSecond()
                        - (spellLevel - 1) * com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.getManaReductionPerLevel());
                float basePerTick = basePerSec / 20.0f;

                // Accurate movement velocity (calculates actual tick displacement on server)
                double dx = player.getX() - player.xOld;
                double dy = player.getY() - player.yOld;
                double dz = player.getZ() - player.zOld;
                double displacementSpeed = Math.sqrt(dx * dx + dy * dy + dz * dz);

                Vec3 vel = player.getDeltaMovement();
                double speed = Math.max(vel.length(), displacementSpeed);
                double verticalClimb = Math.max(vel.y, dy);

                boolean scalingEnabled = com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.isSpeedAltitudeScalingEnabled();

                // Exponential Speed Multiplier
                float speedMult = 1.0f;
                if (scalingEnabled && speed > 0.10) {
                    double excess = speed - 0.10;
                    speedMult += (float) Math.pow(excess / 0.18, com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.getSpeedMultiplierExponent());
                }
                if (player.isSprinting()) {
                    speedMult += 0.5f;
                }
                speedMult = (float) Math.min(speedMult, com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.getMaxSpeedMultiplier());

                // Exponential Altitude Multiplier (Height above ground)
                Vec3 start = player.position();
                Vec3 end = new Vec3(start.x, Math.max(player.level().getMinBuildHeight(), start.y - 128.0), start.z);
                BlockHitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
                double heightAboveGround = (hit.getType() == HitResult.Type.BLOCK) ? Math.max(0.0, start.y - hit.getLocation().y) : 128.0;

                float heightMult = 1.0f;
                if (scalingEnabled && heightAboveGround > 3.0) {
                    double hExcess = heightAboveGround - 3.0;
                    heightMult += (float) Math.pow(hExcess / 10.0, com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.getAltitudeMultiplierExponent());
                }
                // Rapid vertical climb mana surcharge
                if (scalingEnabled && verticalClimb > 0.05) {
                    heightMult += (float) (verticalClimb * 3.0);
                }
                heightMult = (float) Math.min(heightMult, com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.getMaxAltitudeMultiplier());

                // Dynamic Total Mana Drain
                float drainThisTick = basePerTick * speedMult * heightMult;

                if (!player.isCreative()) {
                    MagicData magicData = MagicData.getPlayerMagicData(player);
                    float currentMana = magicData.getMana();

                    if (currentMana < drainThisTick) {
                        // MANA DEPLETED! Collapse flight spell
                        player.removeEffect(ModCinematicMobEffects.FLIGHT);
                        disableFlight(player);
                        magicData.setMana(0.0f);
                        PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));

                        // Emergency feather fall buffer (Configurable)
                        int emergencyTicks = com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.getEmergencyBufferTicks();
                        if (emergencyTicks > 0) {
                            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, emergencyTicks, 0, false, false, false));
                        }

                        ServerLevel sl = player.serverLevel();
                        sl.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.2f, 1.6f);
                        sl.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.4f);

                        player.displayClientMessage(
                                Component.translatable("ui.zoltraak_cinematic.flight_mana_depleted").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                                true
                        );
                        return;
                    } else {
                        magicData.setMana(currentMana - drainThisTick);
                        if (player.tickCount % 4 == 0) {
                            PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
                        }
                    }
                }

                // Action Bar Flight Telemetry (HUD)
                if (com.frierenflight.zoltraakcinematic.config.ZoltraakCinematicConfig.isTelemetryHUDEnabled() && player.tickCount % 8 == 0) {
                    float drainPerSec = drainThisTick * 20.0f;
                    String telemetry = String.format("§b✦ Flight Magic: §f%.0f mana/s  §7|  §eAlt: %.0fm  §7|  §6Spd: %.1fx",
                            drainPerSec, heightAboveGround, speedMult);
                    player.displayClientMessage(Component.literal(telemetry), true);
                }
            } else {
                player.fallDistance = 0.0f;
            }
        } else {
            if (activeFlyers.contains(player.getUUID())) {
                disableFlight(player);
            }
        }
    }
}
