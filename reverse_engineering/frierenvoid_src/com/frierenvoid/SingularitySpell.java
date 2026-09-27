package com.frierenvoid;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class SingularitySpell extends AbstractSpell {
   private final DefaultConfig config = new DefaultConfig()
      .setMinRarity(SpellRarity.LEGENDARY)
      .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
      .setMaxLevel(3)
      .setCooldownSeconds(70.0)
      .build();

   public SingularitySpell() {
      this.baseManaCost = 180;
      this.manaCostPerLevel = 40;
      this.baseSpellPower = 8;
      this.spellPowerPerLevel = 3;
      this.castTime = 40;
   }

   public ResourceLocation getSpellResource() {
      return FrierenVoid.id("singularity");
   }

   public DefaultConfig getDefaultConfig() {
      return this.config;
   }

   public CastType getCastType() {
      return CastType.LONG;
   }

   public AnimationHolder getCastStartAnimation() {
      return SpellAnimations.ANIMATION_LONG_CAST;
   }

   public Vector3f getTargetingColor() {
      return new Vector3f(0.55F, 0.8F, 1.0F);
   }

   public List<MutableComponent> getUniqueInfo(int level, LivingEntity caster) {
      return List.of(
         Component.translatable(
            "ui.frierenvoid.damage", new Object[]{Utils.stringTruncation(this.getSpellPower(level, caster) * (Double)VoidConfig.DAMAGE.get(), 1)}
         ),
         Component.translatable("ui.frierenvoid.radius", new Object[]{((Double)VoidConfig.RADIUS.get()).intValue()}),
         Component.translatable("ui.frierenvoid.duration")
      );
   }

   public static Vec3 aim(LivingEntity caster) {
      return VoidChoreography.palm(caster);
   }

   public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData data) {
      if (level instanceof ServerLevel server) {
         if (!SingularityEntity.canStart(server, caster.getUUID())) {
            caster.sendSystemMessage(Component.translatable("message.frierenvoid.occupied"));
            return false;
         }

         Vec3 point = aim(caster);
         if (!server.hasChunkAt(BlockPos.containing(point)) || !server.getWorldBorder().isWithinBounds(BlockPos.containing(point))) {
            return false;
         }
      }

      return super.checkPreCastConditions(level, spellLevel, caster, data);
   }

   public void castSpell(Level level, int spellLevel, ServerPlayer player, CastSource source, boolean cooldown) {
      if (!this.checkPreCastConditions(level, spellLevel, player, MagicData.getPlayerMagicData(player))) {
         Utils.serverSideCancelCast(player);
      } else {
         super.castSpell(level, spellLevel, player, source, cooldown);
      }
   }

   public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
      if (level instanceof ServerLevel server && SingularityEntity.canStart(server, caster.getUUID())) {
         server.addFreshEntity(
            SingularityEntity.fromHand(
               server,
               caster,
               this.getSpellPower(spellLevel, caster) * ((Double)VoidConfig.DAMAGE.get()).floatValue(),
               ((Double)VoidConfig.RADIUS.get()).floatValue()
            )
         );
      }

      super.onCast(level, spellLevel, caster, source, data);
   }
}
