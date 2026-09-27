package com.frierenflight.zoltraakcinematic.registry;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCinematicSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, ZoltraakCinematicMod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ZOLTRAAK_CHARGE =
            SOUND_EVENTS.register("zoltraak_charge", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "zoltraak_charge")));

    public static final DeferredHolder<SoundEvent, SoundEvent> ZOLTRAAK_FIRE =
            SOUND_EVENTS.register("zoltraak_fire", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "zoltraak_fire")));

    public static final DeferredHolder<SoundEvent, SoundEvent> ZOLTRAAK_IMPACT =
            SOUND_EVENTS.register("zoltraak_impact", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "zoltraak_impact")));

    public static final DeferredHolder<SoundEvent, SoundEvent> ZOLTRAAK_GREAT_FIRE =
            SOUND_EVENTS.register("zoltraak_great_fire", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "zoltraak_great_fire")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
