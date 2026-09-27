package com.frierenvoid;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class ZoltraakSpell extends AbstractSpell {
   private final DefaultConfig config = new DefaultConfig()
      .setMinRarity(SpellRarity.RARE)
      .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
      .setMaxLevel(3)
      .setCooldownSeconds(5.0)
      .build();

   public ZoltraakSpell() {
      this.baseManaCost = 35;
      this.manaCostPerLevel = 10;
      this.baseSpellPower = 12;
      this.spellPowerPerLevel = 4;
      this.castTime = 8;
   }

   public ResourceLocation getSpellResource() {
      return FrierenVoid.id("zoltraak");
   }

   public DefaultConfig getDefaultConfig() {
      return this.config;
   }

   public CastType getCastType() {
      return CastType.LONG;
   }

   public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
      return List.of(
         Component.translatable("ui.frierenvoid.mode.selected", new Object[]{ZoltraakMode.selected(caster).label()}),
         Component.translatable(
            "ui.frierenvoid.zoltraak_damage", new Object[]{Utils.stringTruncation(this.getSpellPower(level, caster) * ZoltraakMode.selected(caster).damage, 1)}
         ),
         Component.translatable("ui.frierenvoid.zoltraak_range", new Object[]{64}),
         Component.translatable(
            "ui.frierenvoid.zoltraak_mana", new Object[]{this.getManaCost(level) * (ZoltraakMode.selected(caster) == ZoltraakMode.LARGE ? 2 : 1)}
         )
      );
   }

   public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
      return !(level instanceof ServerLevel s && !ZoltraakEntity.canStart(s, caster)) && super.checkPreCastConditions(level, spellLevel, caster, data);
   }

   public void castSpell(Level level, int spellLevel, ServerPlayer player, CastSource source, boolean cooldown) {
      boolean charge = source.consumesMana() && (source != CastSource.SWORD || (Boolean)ServerConfigs.SWORDS_CONSUME_MANA.get());
      if (this.checkPreCastConditions(level, spellLevel, player, MagicData.getPlayerMagicData(player))
         && (
            !charge
               || player.isCreative()
               || ZoltraakMode.selected(player) != ZoltraakMode.LARGE
               || !(MagicData.getPlayerMagicData(player).getMana() < this.getManaCost(spellLevel) * 2)
         )) {
         super.castSpell(level, spellLevel, player, source, cooldown);
      } else {
         Utils.serverSideCancelCast(player);
      }
   }

   public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
      if (level instanceof ServerLevel s && ZoltraakEntity.canStart(s, caster)) {
         boolean black = this.getSpellResource().getPath().equals("black_zoltraak");
         if (caster instanceof ServerPlayer p && ZoltraakMode.selected(caster) == ZoltraakMode.RAPID && RapidZoltraakChannel.held(p, black)) {
            RapidZoltraakChannel.begin(p, black, this.getSpellPower(spellLevel, caster), this.getManaCost(spellLevel));
         } else if (caster instanceof ServerPlayer && ZoltraakMode.selected(caster) == ZoltraakMode.RAPID) {
            s.addFreshEntity(ZoltraakEntity.createRapid(s, caster, this.getSpellPower(spellLevel, caster) * ZoltraakMode.RAPID.damage, black, 0));
         } else {
            ZoltraakEntity.launch(s, caster, this.getSpellPower(spellLevel, caster), black);
         }
      }

      super.onCast(level, spellLevel, caster, source, data);
   }
}
