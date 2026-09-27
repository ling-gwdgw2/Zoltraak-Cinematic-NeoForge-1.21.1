package com.frierenflight.zoltraakcinematic.registry;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCinematicSchools {
    public static final DeferredRegister<SchoolType> SCHOOLS =
            DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, ZoltraakCinematicMod.MODID);

    public static final ResourceLocation ORDINARY_MAGIC_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "ordinary_magic");

    public static final TagKey<Item> ORDINARY_MAGIC_FOCUS =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "ordinary_magic_focus"));

    public static final DeferredHolder<SchoolType, SchoolType> ORDINARY_MAGIC = SCHOOLS.register("ordinary_magic", () ->
            new SchoolType(
                    ORDINARY_MAGIC_RESOURCE,
                    ORDINARY_MAGIC_FOCUS,
                    Component.translatable("school.zoltraak_cinematic.ordinary_magic")
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00E5FF))),
                    ModCinematicAttributes.ZOLTRAAK_SPELL_POWER,
                    ModCinematicAttributes.ZOLTRAAK_MAGIC_RESIST,
                    net.minecraft.core.Holder.direct(SoundEvents.BEACON_ACTIVATE),
                    DamageTypes.MAGIC
            )
    );

    public static void register(IEventBus modEventBus) {
        SCHOOLS.register(modEventBus);
    }
}
