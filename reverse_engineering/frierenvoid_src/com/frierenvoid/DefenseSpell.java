package com.frierenvoid;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public final class DefenseSpell extends AbstractSpell {
   private final DefaultConfig config = new DefaultConfig()
      .setMinRarity(SpellRarity.UNCOMMON)
      .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
      .setMaxLevel(3)
      .setCooldownSeconds(10.0)
      .build();

   public DefenseSpell() {
      this.baseManaCost = 35;
      this.manaCostPerLevel = 10;
      this.baseSpellPower = 18;
      this.spellPowerPerLevel = 6;
      this.castTime = 2;
   }

   public ResourceLocation getSpellResource() {
      return FrierenVoid.id("defense");
   }

   public DefaultConfig getDefaultConfig() {
      return this.config;
   }

   public CastType getCastType() {
      return CastType.LONG;
   }

   public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
      return List.of(
         Component.translatable("ui.frierenvoid.defense_capacity", new Object[]{Utils.stringTruncation(this.getSpellPower(level, caster), 1)}),
         Component.translatable("ui.frierenvoid.defense_duration")
      );
   }

   public boolean checkPreCastConditions(Level level, int levelIndex, LivingEntity caster, MagicData data) {
      return DefenseEntity.find(caster) == null && super.checkPreCastConditions(level, levelIndex, caster, data);
   }

   public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
      if (level instanceof ServerLevel s && DefenseEntity.find(caster) == null) {
         s.addFreshEntity(DefenseEntity.create(s, caster, this.getSpellPower(spellLevel, caster)));
      }

      super.onCast(level, spellLevel, caster, source, data);
   }
}
