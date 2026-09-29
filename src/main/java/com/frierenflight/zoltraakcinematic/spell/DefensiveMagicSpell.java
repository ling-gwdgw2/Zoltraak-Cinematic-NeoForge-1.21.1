package com.frierenflight.zoltraakcinematic.spell;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.entity.DefenseBarrierEntity;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSchools;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public class DefensiveMagicSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "defense");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModCinematicSchools.ORDINARY_MAGIC_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(10.0)
            .build();

    public DefensiveMagicSpell() {
        this.baseManaCost = 200;
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 200;
        this.spellPowerPerLevel = 67;
        this.castTime = 0; // Instant deployment!
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

    public int getDurationTicks(int spellLevel, LivingEntity caster) {
        return 300 + (spellLevel - 1) * 22; // 15s (300 ticks) base up to ~25s (498-500 ticks) at level 10
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.zoltraak_cinematic.defense_capacity", Utils.stringTruncation(getSpellPower(spellLevel, caster), 1)),
                Component.translatable("ui.zoltraak_cinematic.defense_duration", Utils.stringTruncation(getDurationTicks(spellLevel, caster) / 20.0f, 1))
        );
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            DefenseBarrierEntity existing = DefenseBarrierEntity.find(caster);
            int mode = caster.isCrouching() ? DefenseBarrierEntity.MODE_DOME : DefenseBarrierEntity.MODE_DIRECTIONAL;
            float capacity = getSpellPower(spellLevel, caster);
            int duration = getDurationTicks(spellLevel, caster);

            if (existing == null) {
                serverLevel.addFreshEntity(DefenseBarrierEntity.create(serverLevel, caster, capacity, mode, duration));
            } else {
                existing.refresh(capacity, mode, duration);
            }
        }
        super.onCast(level, spellLevel, caster, castSource, playerMagicData);
    }
}
