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

    public static final DeferredHolder<EntityType<?>, EntityType<com.frierenflight.zoltraakcinematic.entity.ZoltraakBarrageProjectileEntity>> ZOLTRAAK_BARRAGE_PROJECTILE =
            ENTITIES.register("zoltraak_barrage_projectile", () -> EntityType.Builder.<com.frierenflight.zoltraakcinematic.entity.ZoltraakBarrageProjectileEntity>of(
                            com.frierenflight.zoltraakcinematic.entity.ZoltraakBarrageProjectileEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(ZoltraakCinematicMod.MODID + ":zoltraak_barrage_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.frierenflight.zoltraakcinematic.entity.DefenseBarrierEntity>> DEFENSE_BARRIER =
            ENTITIES.register("defense_barrier", () -> EntityType.Builder.<com.frierenflight.zoltraakcinematic.entity.DefenseBarrierEntity>of(
                            com.frierenflight.zoltraakcinematic.entity.DefenseBarrierEntity::new, MobCategory.MISC)
                    .sized(3.0f, 3.0f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build(ZoltraakCinematicMod.MODID + ":defense_barrier"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
