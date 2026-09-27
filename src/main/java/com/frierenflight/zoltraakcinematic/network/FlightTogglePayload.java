package com.frierenflight.zoltraakcinematic.network;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.event.FlightEvents;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicMobEffects;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSpells;
import com.frierenflight.zoltraakcinematic.spell.FlightMagicSpell;
import io.netty.buffer.ByteBuf;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * FlightTogglePayload
 * C2S packet sent when the dedicated Flight Magic key ('V') is pressed.
 * Toggles Flight Magic ON/OFF passively if inscribed in the player's spellbook.
 */
public record FlightTogglePayload() implements CustomPacketPayload {
    public static final Type<FlightTogglePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "flight_toggle"));

    public static final StreamCodec<ByteBuf, FlightTogglePayload> STREAM_CODEC = StreamCodec.unit(new FlightTogglePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FlightTogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            int spellLevel = getPlayerFlightSpellLevel(player);
            if (spellLevel <= 0) {
                player.displayClientMessage(
                        Component.translatable("ui.zoltraak_cinematic.flight_not_in_spellbook").withStyle(ChatFormatting.RED),
                        true
                );
                player.level().playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.2F);
                return;
            }

            FlightMagicSpell spell = (FlightMagicSpell) ModCinematicSpells.FLIGHT.get();
            MagicData magicData = MagicData.getPlayerMagicData(player);

            if (player.hasEffect(ModCinematicMobEffects.FLIGHT)) {
                // Toggle OFF
                player.removeEffect(ModCinematicMobEffects.FLIGHT);
                FlightEvents.disableFlight(player);
                player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9F, 0.8F);
                player.displayClientMessage(
                        Component.translatable("ui.zoltraak_cinematic.flight_deactivated").withStyle(ChatFormatting.AQUA),
                        true
                );
            } else {
                // Check cooldown
                if (magicData.getPlayerCooldowns().isOnCooldown(spell)) {
                    return;
                }

                // Check mana
                int manaCost = spell.getManaCost(spellLevel);
                if (!player.isCreative() && magicData.getMana() < manaCost) {
                    player.displayClientMessage(
                            Component.translatable("ui.irons_spellbooks.not_enough_mana").withStyle(ChatFormatting.RED),
                            true
                    );
                    return;
                }

                // Deduct ignition mana
                if (!player.isCreative()) {
                    magicData.setMana(Math.max(0.0F, magicData.getMana() - manaCost));
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new io.redspace.ironsspellbooks.network.SyncManaPacket(magicData));
                }

                // Apply cooldown
                io.redspace.ironsspellbooks.api.magic.MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, io.redspace.ironsspellbooks.api.spells.CastSource.SPELLBOOK);

                // Toggle ON
                player.addEffect(new MobEffectInstance(ModCinematicMobEffects.FLIGHT, MobEffectInstance.INFINITE_DURATION, spellLevel - 1, false, false, true));
                FlightEvents.enableFlight(player);
                player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.7F);
                player.level().playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 1.5F);
                player.displayClientMessage(
                        Component.translatable("ui.zoltraak_cinematic.flight_activated").withStyle(ChatFormatting.AQUA),
                        true
                );
            }
        });
    }

    /**
     * Inspects the player's equipped spellbook (via Curios/hands) and fallback inventory.
     * Returns the highest inscribed level of Flight Magic found, or 0 if not present.
     */
    public static int getPlayerFlightSpellLevel(ServerPlayer player) {
        FlightMagicSpell spell = (FlightMagicSpell) ModCinematicSpells.FLIGHT.get();
        int bestLevel = 0;

        // 1. Primary: Curios spellbook, mainhand, and offhand
        SpellSelectionManager ssm = new SpellSelectionManager(player);
        for (SpellSelectionManager.SelectionOption option : ssm.getAllSpells()) {
            if (option != null && option.spellData != null && option.spellData.getSpell() == spell) {
                bestLevel = Math.max(bestLevel, option.spellData.getLevel());
            }
        }

        // 2. Secondary fallback: player inventory containers/scrolls
        if (bestLevel <= 0) {
            for (ItemStack stack : player.getInventory().items) {
                if (ISpellContainer.isSpellContainer(stack)) {
                    ISpellContainer container = ISpellContainer.get(stack);
                    if (container != null) {
                        int idx = container.getIndexForSpell(spell);
                        if (idx >= 0) {
                            SpellData data = container.getSpellAtIndex(idx);
                            if (data != null) {
                                bestLevel = Math.max(bestLevel, data.getLevel());
                            }
                        }
                    }
                }
            }
        }

        return bestLevel;
    }
}
