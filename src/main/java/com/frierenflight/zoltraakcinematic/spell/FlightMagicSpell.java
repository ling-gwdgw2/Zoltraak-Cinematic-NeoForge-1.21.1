package com.frierenflight.zoltraakcinematic.spell;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.event.FlightEvents;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicMobEffects;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * FlightMagicSpell (人類の魔法 - 飛行魔法)
 * Instant toggle spell allowing the player to freely soar, glide, and hover through 3D space.
 * Continuously consumes mana with exponential scaling at high altitudes and speeds.
 */
public class FlightMagicSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "flight");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(ModCinematicSchools.ORDINARY_MAGIC_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(1.0)
            .build();

    public FlightMagicSpell() {
        this.baseManaCost = 25; // Initial mana ignition
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 1;
        this.castTime = 0; // Instant toggle
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.SELF_CAST_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    public float getBaseManaDrain(int spellLevel) {
        return 50.0f - (spellLevel - 1) * 2.5f; // 50 mana/s at L1 down to 40 mana/s at L5
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
        if (caster != null && caster.hasEffect(ModCinematicMobEffects.FLIGHT)) {
            // Turning off flight requires NO mana and can always be cast
            return true;
        }
        return super.checkPreCastConditions(level, spellLevel, caster, data);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.zoltraak_cinematic.flight_drain_info", String.format("%.0f", getBaseManaDrain(spellLevel))),
                Component.translatable("ui.zoltraak_cinematic.flight_exponential_note")
        );
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide && caster instanceof ServerPlayer player) {
            if (player.hasEffect(ModCinematicMobEffects.FLIGHT)) {
                // Toggle OFF
                player.removeEffect(ModCinematicMobEffects.FLIGHT);
                FlightEvents.disableFlight(player);

                // Refund mana charged by spell cast so deactivation is free
                if (!player.isCreative() && playerMagicData != null) {
                    playerMagicData.setMana(playerMagicData.getMana() + this.getManaCost(spellLevel));
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new io.redspace.ironsspellbooks.network.SyncManaPacket(playerMagicData));
                }

                level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9F, 0.8F);
                player.displayClientMessage(
                        Component.translatable("ui.zoltraak_cinematic.flight_deactivated").withStyle(ChatFormatting.AQUA),
                        true
                );
            } else {
                // Toggle ON
                player.addEffect(new MobEffectInstance(ModCinematicMobEffects.FLIGHT, MobEffectInstance.INFINITE_DURATION, spellLevel - 1, false, false, true));
                FlightEvents.enableFlight(player);
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.7F);
                level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 1.5F);
                player.displayClientMessage(
                        Component.translatable("ui.zoltraak_cinematic.flight_activated").withStyle(ChatFormatting.AQUA),
                        true
                );
            }
        }
        super.onCast(level, spellLevel, caster, castSource, playerMagicData);
    }
}
