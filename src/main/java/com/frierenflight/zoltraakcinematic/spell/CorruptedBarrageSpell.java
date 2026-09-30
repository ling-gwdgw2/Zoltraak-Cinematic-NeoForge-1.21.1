package com.frierenflight.zoltraakcinematic.spell;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Corrupted Rapid Barrage (Human-Killing Dark Matter Projectiles).
 * Fires rapid dark thorn projectiles that wither targets and bypass magical barriers.
 */
public class CorruptedBarrageSpell extends FernBarrageSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "corrupted_barrage");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(com.frierenflight.zoltraakcinematic.registry.ModCinematicSchools.ORDINARY_MAGIC_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(1.8)
            .build();

    public CorruptedBarrageSpell() {
        super();
        this.baseManaCost = 300;
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 7;
        this.spellPowerPerLevel = 8;
        this.castTime = 200; // Continuous channel up to 10 seconds
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
    protected int getColorTheme() {
        return 1; // 1 = HUMAN_KILLING / Dark Purple
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float bulletDmg = getBulletDamage(spellLevel, caster);
        float salvoDmg = bulletDmg * 24.0f;
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(salvoDmg, 1) + " (" + Utils.stringTruncation(bulletDmg, 1) + "x24)"),
                Component.translatable("spell.zoltraak_cinematic.barrage_rate", "24x Guided Dark Thorns"),
                Component.translatable("spell.zoltraak_cinematic.corrupted_barrage.perk")
        );
    }
}
