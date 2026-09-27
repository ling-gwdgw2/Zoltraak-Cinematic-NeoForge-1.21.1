package com.frierenflight.zoltraakcinematic.registry;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import io.redspace.ironsspellbooks.api.attribute.MagicPercentAttribute;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCinematicAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, ZoltraakCinematicMod.MODID);

    public static final DeferredHolder<Attribute, Attribute> ZOLTRAAK_SPELL_POWER = ATTRIBUTES.register(
            "zoltraak_spell_power",
            () -> new MagicPercentAttribute("attribute.zoltraak_cinematic.zoltraak_spell_power", 1.0D, -100.0D, 100.0D).setSyncable(true)
    );

    public static final DeferredHolder<Attribute, Attribute> ZOLTRAAK_MAGIC_RESIST = ATTRIBUTES.register(
            "zoltraak_magic_resist",
            () -> new MagicPercentAttribute("attribute.zoltraak_cinematic.zoltraak_magic_resist", 1.0D, -100.0D, 100.0D).setSyncable(true)
    );

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(ModCinematicAttributes::modifyEntityAttributes);
    }

    public static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ZOLTRAAK_SPELL_POWER);
        event.add(EntityType.PLAYER, ZOLTRAAK_MAGIC_RESIST);
    }
}
