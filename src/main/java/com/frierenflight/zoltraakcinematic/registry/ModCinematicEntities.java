package com.frierenflight.zoltraakcinematic.registry;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.entity.ZoltraakCinematicBeamEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCinematicEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ZoltraakCinematicMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ZoltraakCinematicBeamEntity>> ZOLTRAAK_BEAM =
            ENTITIES.register("zoltraak_beam", () -> EntityType.Builder.<ZoltraakCinematicBeamEntity>of(
                            ZoltraakCinematicBeamEntity::new, MobCategory.MISC)
                    .sized(1.2f, 1.2f)
                    .clientTrackingRange(128)
                    .updateInterval(1)
                    .build(ZoltraakCinematicMod.MODID + ":zoltraak_beam"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
