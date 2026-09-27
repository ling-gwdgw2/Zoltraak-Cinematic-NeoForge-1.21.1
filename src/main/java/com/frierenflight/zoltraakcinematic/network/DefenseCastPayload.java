package com.frierenflight.zoltraakcinematic.network;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.entity.DefenseBarrierEntity;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSpells;
import com.frierenflight.zoltraakcinematic.spell.DefensiveMagicSpell;
import io.netty.buffer.ByteBuf;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * DefenseCastPayload
 * C2S packet sent when the dedicated Defensive Magic key ('X') is pressed.
 * Mode 0 = Directional 19-cell shield
 * Mode 1 = 360-degree geodesic honeycomb dome
 */
public record DefenseCastPayload(int mode) implements CustomPacketPayload {
    public static final Type<DefenseCastPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "defense_cast"));

    public static final StreamCodec<ByteBuf, DefenseCastPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> ByteBufCodecs.VAR_INT.encode(buf, payload.mode()),
            buf -> new DefenseCastPayload(ByteBufCodecs.VAR_INT.decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DefenseCastPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            int spellLevel = getPlayerDefensiveSpellLevel(player);
            if (spellLevel <= 0) {
                // Defensive magic not found in equipped spellbook or inventory
                player.displayClientMessage(
                        Component.translatable("ui.zoltraak_cinematic.defense_not_in_spellbook").withStyle(ChatFormatting.RED),
                        true
                );
                player.level().playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.2F);
                return;
            }

            ServerLevel level = player.serverLevel();
            DefensiveMagicSpell spell = (DefensiveMagicSpell) ModCinematicSpells.DEFENSE_MAGIC.get();
            DefenseBarrierEntity existing = DefenseBarrierEntity.find(player);
            MagicData magicData = MagicData.getPlayerMagicData(player);

            // Seamless double-tap upgrade: transform active directional barrier into full dome
            boolean isDoubleTapUpgrade = existing != null
                    && existing.mode() == DefenseBarrierEntity.MODE_DIRECTIONAL
                    && payload.mode() == DefenseBarrierEntity.MODE_DOME
                    && existing.age(0.0F) <= 20; // Within 1.0 second of initial single-tap

            if (isDoubleTapUpgrade) {
                float capacity = spell.getSpellPower(spellLevel, player);
                int duration = spell.getDurationTicks(spellLevel, player);
                existing.refresh(capacity, DefenseBarrierEntity.MODE_DOME, duration);
                level.playSound(null, existing.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.3F, 1.5F);
                level.playSound(null, existing.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9F, 1.8F);
                return;
            }

            // Check spell cooldown
            if (magicData.getPlayerCooldowns().isOnCooldown(spell)) {
                return;
            }

            // Check mana cost
            int manaCost = spell.getManaCost(spellLevel);
            if (!player.isCreative() && magicData.getMana() < manaCost) {
                player.displayClientMessage(
                        Component.translatable("ui.irons_spellbooks.not_enough_mana").withStyle(ChatFormatting.RED),
                        true
                );
                return;
            }

            // Deduct mana
            if (!player.isCreative()) {
                magicData.setMana(Math.max(0.0F, magicData.getMana() - manaCost));
                PacketDistributor.sendToPlayer(player, new SyncManaPacket(magicData));
            }

            // Apply cooldown
            MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, CastSource.SPELLBOOK);

            // Deploy or refresh barrier
            float capacity = spell.getSpellPower(spellLevel, player);
            int duration = spell.getDurationTicks(spellLevel, player);

            if (existing == null) {
                level.addFreshEntity(DefenseBarrierEntity.create(level, player, capacity, payload.mode(), duration));
            } else {
                existing.refresh(capacity, payload.mode(), duration);
            }
        });
    }

    /**
     * Inspects the player's equipped spellbook (via Curios/hands) and fallback inventory.
     * Returns the highest inscribed level of Defensive Magic found, or 0 if not present.
     */
    public static int getPlayerDefensiveSpellLevel(ServerPlayer player) {
        DefensiveMagicSpell spell = (DefensiveMagicSpell) ModCinematicSpells.DEFENSE_MAGIC.get();
        int bestLevel = 0;

        // 1. Primary: Curios spellbook, mainhand, and offhand via Iron's SpellSelectionManager
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
