package com.frierenflight.zoltraakcinematic.block;

import com.frierenflight.zoltraakcinematic.block.entity.QualSealingStoneBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 📜 QualSealingStoneBlock — Frieren's Ancient Sealing Monolith (ศิลาสะกดมารของฟรีเรน).
 *
 * Indestructible runic monolith holding the sealed essence of Qual for 80 years.
 * Interacting with a magic focus or grimoire begins the 5.0-second Unsealing Ritual.
 */
public class QualSealingStoneBlock extends Block implements EntityBlock {

    public QualSealingStoneBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.ANCIENT_DEBRIS)
                .strength(-1.0f, 3600000.0f) // Indestructible
                .noOcclusion()
                .lightLevel(state -> 6)
        );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QualSealingStoneBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : (lvl, p, st, be) -> {
            if (be instanceof QualSealingStoneBlockEntity tile) {
                tile.tick();
            }
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return triggerUnseal(level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        ItemInteractionResult res = triggerUnseal(level, pos, player);
        return res.result();
    }

    private ItemInteractionResult triggerUnseal(Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof QualSealingStoneBlockEntity tile) {
            if (tile.isUnsealing()) {
                return ItemInteractionResult.CONSUME;
            }

            if (!level.isClientSide) {
                tile.startUnsealing();
                player.displayClientMessage(Component.translatable("message.zoltraak_cinematic.unsealing_started"), true);
            }
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
