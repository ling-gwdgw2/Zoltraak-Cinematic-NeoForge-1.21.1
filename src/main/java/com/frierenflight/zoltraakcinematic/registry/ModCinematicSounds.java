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

    // --- Boss Qual (Elder Sage of Corruption) Sound Events ---
    public static final DeferredHolder<SoundEvent, SoundEvent> QUAL_SPAWN =
            SOUND_EVENTS.register("qual_spawn", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "qual_spawn")));

    public static final DeferredHolder<SoundEvent, SoundEvent> QUAL_AMBIENT =
            SOUND_EVENTS.register("qual_ambient", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "qual_ambient")));

    public static final DeferredHolder<SoundEvent, SoundEvent> QUAL_HURT =
            SOUND_EVENTS.register("qual_hurt", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "qual_hurt")));

    public static final DeferredHolder<SoundEvent, SoundEvent> QUAL_DEATH =
            SOUND_EVENTS.register("qual_death", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "qual_death")));

    public static final DeferredHolder<SoundEvent, SoundEvent> QUAL_PHASE_TRANSITION =
            SOUND_EVENTS.register("qual_phase_transition", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "qual_phase_transition")));

    public static final DeferredHolder<SoundEvent, SoundEvent> QUAL_CATACLYSMIC_CHARGE =
            SOUND_EVENTS.register("qual_cataclysmic_charge", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "qual_cataclysmic_charge")));

    public static final DeferredHolder<SoundEvent, SoundEvent> QUAL_TELEPORT =
            SOUND_EVENTS.register("qual_teleport", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "qual_teleport")));

    // --- Gargantua Singularity Sound Events ---
    public static final DeferredHolder<SoundEvent, SoundEvent> SINGULARITY_CHARGE =
            SOUND_EVENTS.register("singularity_charge", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "singularity_charge")));

    public static final DeferredHolder<SoundEvent, SoundEvent> SINGULARITY_ACTIVE =
            SOUND_EVENTS.register("singularity_active", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "singularity_active")));

    public static final DeferredHolder<SoundEvent, SoundEvent> SINGULARITY_EXPLODE =
            SOUND_EVENTS.register("singularity_explode", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "singularity_explode")));

    public static final DeferredHolder<SoundEvent, SoundEvent> TINNITUS =
            SOUND_EVENTS.register("tinnitus", () ->
                    SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "tinnitus")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
