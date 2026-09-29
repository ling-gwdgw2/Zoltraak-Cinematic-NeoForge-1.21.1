package com.frierenflight.zoltraakcinematic.registry;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.block.QualSealingStoneBlock;
import com.frierenflight.zoltraakcinematic.block.entity.QualSealingStoneBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCinematicBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ZoltraakCinematicMod.MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ZoltraakCinematicMod.MODID);

    public static final DeferredBlock<Block> QUAL_SEALING_STONE =
            BLOCKS.register("qual_sealing_stone", QualSealingStoneBlock::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<QualSealingStoneBlockEntity>> QUAL_SEALING_STONE_BE =
            BLOCK_ENTITIES.register("qual_sealing_stone", () ->
                    BlockEntityType.Builder.of(QualSealingStoneBlockEntity::new, QUAL_SEALING_STONE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
    }
}
