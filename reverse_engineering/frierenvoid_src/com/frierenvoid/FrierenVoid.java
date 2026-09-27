package com.frierenvoid;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod("frierenvoid")
public final class FrierenVoid {
   public static final String ID = "frierenvoid";
   private static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, "frierenvoid");
   public static final DeferredHolder<AbstractSpell, AbstractSpell> SINGULARITY = SPELLS.register("singularity", SingularitySpell::new);
   public static final DeferredHolder<AbstractSpell, AbstractSpell> ZOLTRAAK = SPELLS.register("zoltraak", ZoltraakSpell::new);
   public static final DeferredHolder<AbstractSpell, AbstractSpell> BLACK_ZOLTRAAK = SPELLS.register("black_zoltraak", BlackZoltraakSpell::new);
   public static final DeferredHolder<AbstractSpell, AbstractSpell> DEFENSE = SPELLS.register("defense", DefenseSpell::new);
   private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, "frierenvoid");
   public static final DeferredHolder<EntityType<?>, EntityType<SingularityEntity>> ENTITY = ENTITIES.register(
      "singularity",
      () -> Builder.of(SingularityEntity::new, MobCategory.MISC)
         .sized(1.0F, 1.0F)
         .fireImmune()
         .noSave()
         .noSummon()
         .clientTrackingRange(12)
         .updateInterval(20)
         .build(id("singularity").toString())
   );
   public static final DeferredHolder<EntityType<?>, EntityType<AbsorbedBlockEntity>> BLOCK_ENTITY = ENTITIES.register(
      "absorbed_block",
      () -> Builder.of(AbsorbedBlockEntity::new, MobCategory.MISC)
         .sized(0.9F, 0.9F)
         .fireImmune()
         .noSave()
         .noSummon()
         .clientTrackingRange(10)
         .updateInterval(2)
         .build(id("absorbed_block").toString())
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ZoltraakEntity>> ZOLTRAAK_ENTITY = ENTITIES.register(
      "zoltraak",
      () -> Builder.of(ZoltraakEntity::new, MobCategory.MISC)
         .sized(0.3F, 0.3F)
         .fireImmune()
         .noSave()
         .noSummon()
         .clientTrackingRange(16)
         .updateInterval(2)
         .build(id("zoltraak").toString())
   );
   public static final DeferredHolder<EntityType<?>, EntityType<DefenseEntity>> DEFENSE_ENTITY = ENTITIES.register(
      "defense",
      () -> Builder.of(DefenseEntity::new, MobCategory.MISC)
         .sized(0.5F, 0.5F)
         .fireImmune()
         .noSave()
         .noSummon()
         .clientTrackingRange(12)
         .updateInterval(1)
         .build(id("defense").toString())
   );
   private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, "frierenvoid");
   public static final DeferredHolder<SoundEvent, SoundEvent> OPEN = sound("open");
   public static final DeferredHolder<SoundEvent, SoundEvent> ACCRETION = sound("accretion");
   public static final DeferredHolder<SoundEvent, SoundEvent> COLLAPSE = sound("collapse");
   public static final DeferredHolder<SoundEvent, SoundEvent> RELEASE = sound("release");
   public static final DeferredHolder<SoundEvent, SoundEvent> ZOLTRAAK_CHARGE = sound("zoltraak_charge");
   public static final DeferredHolder<SoundEvent, SoundEvent> ZOLTRAAK_FIRE = sound("zoltraak_fire");
   public static final DeferredHolder<SoundEvent, SoundEvent> ZOLTRAAK_IMPACT = sound("zoltraak_impact");
   public static final DeferredHolder<SoundEvent, SoundEvent> ZOLTRAAK_GREAT_FIRE = sound("zoltraak_great_fire");

   public FrierenVoid(IEventBus bus, ModContainer container) {
      bus.addListener(ZoltraakNetwork::register);
      SPELLS.register(bus);
      ENTITIES.register(bus);
      SOUNDS.register(bus);
      container.registerConfig(Type.SERVER, VoidConfig.SERVER);
      container.registerConfig(Type.CLIENT, VoidConfig.CLIENT);
   }

   public static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath("frierenvoid", path);
   }

   private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
      return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id(name)));
   }
}
