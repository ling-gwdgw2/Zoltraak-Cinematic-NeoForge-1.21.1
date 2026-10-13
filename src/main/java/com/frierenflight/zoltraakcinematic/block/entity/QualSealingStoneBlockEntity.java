package com.frierenflight.zoltraakcinematic.block.entity;

import com.frierenflight.zoltraakcinematic.entity.boss.QualBossEntity;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicBlocks;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSounds;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 📜 QualSealingStoneBlockEntity — Orchestrates the 5.0-Second Ancient Unsealing Ritual.
 *
 * Sequence Progression:
 * - 0.0s – 2.0s (Ticks 0..40): Frieren's blue sealing runes turn red & begin vibrating
 * - 2.0s – 3.5s (Ticks 40..70): The 4 cardinal magic chains shatter with glass/chain sounds
 * - 3.5s – 5.0s (Ticks 70..100): Colossal dark void beam erupts skyward with deep earthquake rumble
 * - 5.0s (Tick 100): The monolith shatters! Qual levitates into the sky, initiating boss combat!
 */
public class QualSealingStoneBlockEntity extends BlockEntity {

    private boolean unsealing = false;
    private int unsealTicks = 0;
    private static final int TOTAL_UNSEAL_TICKS = 100; // 5.0 seconds

    public QualSealingStoneBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModCinematicBlocks.QUAL_SEALING_STONE_BE.get(), pos, blockState);
    }

    public boolean isUnsealing() {
        return unsealing;
    }

    public void startUnsealing() {
        if (!this.unsealing && this.level != null && !this.level.isClientSide) {
            this.unsealing = true;
            this.unsealTicks = 0;
            this.setChanged();

            if (this.level instanceof ServerLevel sl) {
                sl.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
                sl.playSound(null, this.worldPosition, ModCinematicSounds.QUAL_PHASE_TRANSITION.get(), SoundSource.BLOCKS, 1.8f, 0.75f);
            }
        }
    }

    public void clientTick() {
        if (!this.unsealing) {
            return;
        }
        this.unsealTicks++;
        // Pure Mathematical Procedural Shader Rendering via QualSealingStoneRenderer (No Vanilla Particles)
    }

    public void tick() {
        if (!this.unsealing || this.level == null || this.level.isClientSide) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) this.level;
        unsealTicks++;

        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 0.5;
        double cz = worldPosition.getZ() + 0.5;

        // Periodic sync with clients
        if (unsealTicks % 10 == 0) {
            serverLevel.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }

        // Stage 1 (0.0s – 1.5s, Ticks 0..30): Sealing Runes Shift & Ancient Crack Sounds
        if (unsealTicks <= 30) {
            if (unsealTicks % 10 == 0) {
                serverLevel.playSound(null, worldPosition, ModCinematicSounds.SINGULARITY_CHARGE.get(), SoundSource.BLOCKS, 1.5f, 0.9f);
            }
        }
        // Stage 2 (1.5s – 3.25s, Ticks 30..65): Harmonic Mana Surge
        else if (unsealTicks <= 65) {
            if (unsealTicks % 12 == 0) {
                serverLevel.playSound(null, worldPosition, ModCinematicSounds.SINGULARITY_ACTIVE.get(), SoundSource.BLOCKS, 1.4f, 1.1f);
            }
        }
        // Stage 3 (3.25s – 5.0s, Ticks 65..99): Spacetime Rupture Resonance
        else if (unsealTicks < TOTAL_UNSEAL_TICKS) {
            if (unsealTicks % 10 == 0) {
                serverLevel.playSound(null, worldPosition, ModCinematicSounds.QUAL_PHASE_TRANSITION.get(), SoundSource.HOSTILE, 1.8f, 1.25f);
            }
        }
        // Stage 4 (5.0s, Tick 100): Final Apocalyptic Shatter & Boss Manifestation!
        else {
            // Shatter detonation (100% Custom Mod Sounds)
            serverLevel.playSound(null, worldPosition, ModCinematicSounds.SINGULARITY_EXPLODE.get(), SoundSource.HOSTILE, 3.5f, 0.85f);
            serverLevel.playSound(null, worldPosition, ModCinematicSounds.QUAL_SPAWN.get(), SoundSource.HOSTILE, 3.0f, 1.0f);

            // Spawn Qual Boss floating in the air
            QualBossEntity qual = ModCinematicEntities.QUAL_BOSS.get().create(serverLevel);
            if (qual != null) {
                qual.moveTo(cx, cy + 3.0, cz, 0.0f, 0.0f);
                qual.initializeHealthScaling();
                serverLevel.addFreshEntity(qual);
            }

            // Replace sealing stone with cracked ruins
            serverLevel.setBlock(worldPosition, Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 3);
        }
    }

    public float getUnsealProgress(float partialTick) {
        if (!unsealing) return 0.0f;
        return net.minecraft.util.Mth.clamp((this.unsealTicks + partialTick) / (float) TOTAL_UNSEAL_TICKS, 0.0f, 1.0f);
    }

    public int getUnsealTicks() {
        return unsealTicks;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Unsealing", this.unsealing);
        tag.putInt("UnsealTicks", this.unsealTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.unsealing = tag.getBoolean("Unsealing");
        this.unsealTicks = tag.getInt("UnsealTicks");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, net.minecraft.core.HolderLookup.Provider registries) {
        super.onDataPacket(net, pkt, registries);
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            this.unsealing = tag.getBoolean("Unsealing");
            this.unsealTicks = tag.getInt("UnsealTicks");
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        this.unsealing = tag.getBoolean("Unsealing");
        this.unsealTicks = tag.getInt("UnsealTicks");
    }
}
