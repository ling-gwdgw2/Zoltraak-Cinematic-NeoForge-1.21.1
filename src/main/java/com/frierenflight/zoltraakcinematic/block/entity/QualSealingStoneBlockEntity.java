package com.frierenflight.zoltraakcinematic.block.entity;

import com.frierenflight.zoltraakcinematic.entity.boss.QualBossEntity;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicBlocks;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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
                sl.playSound(null, this.worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2f, 0.7f);
                sl.playSound(null, this.worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 1.4f);
            }
        }
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

        // Phase A (0.0s – 2.0s): Runic Resonating & Frequency Escalation
        if (unsealTicks <= 40) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, cx, cy + 0.6, cz, 8, 0.4, 0.4, 0.4, 0.1);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy + 0.6, cz, 4, 0.3, 0.3, 0.3, 0.05);

            if (unsealTicks % 10 == 0) {
                float pitch = 0.8f + (unsealTicks / 40.0f) * 0.4f;
                serverLevel.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2f, pitch);
            }
        }
        // Phase B (2.0s – 3.5s): Four Cardinal Magic Chains Shattering
        else if (unsealTicks <= 70) {
            serverLevel.sendParticles(ParticleTypes.WITCH, cx, cy + 0.8, cz, 12, 0.5, 0.5, 0.5, 0.05);
            serverLevel.sendParticles(ParticleTypes.END_ROD, cx, cy + 0.8, cz, 6, 0.8, 0.2, 0.8, 0.08);

            if (unsealTicks % 8 == 0) {
                serverLevel.playSound(null, worldPosition, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.4f, 0.75f);
                serverLevel.playSound(null, worldPosition, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0f, 0.85f);
            }
        }
        // Phase C (3.5s – 5.0s): Skyward Dark Mana Pillar & Earthquake
        else if (unsealTicks < TOTAL_UNSEAL_TICKS) {
            for (int y = 0; y < 24; y += 2) {
                serverLevel.sendParticles(ParticleTypes.PORTAL, cx, cy + y, cz, 4, 0.4, 0.4, 0.4, 0.1);
                serverLevel.sendParticles(ParticleTypes.SQUID_INK, cx, cy + y, cz, 2, 0.3, 0.3, 0.3, 0.05);
            }

            if (unsealTicks % 6 == 0) {
                serverLevel.playSound(null, worldPosition, SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 1.2f, 0.55f);
                serverLevel.playSound(null, worldPosition, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.5f, 0.60f);
            }
        }
        // Phase D (5.0s): Final Cataclysmic Shatter & Boss Manifestation!
        else {
            // Shatter blast VFX & SFX
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy + 1.0, cz, 2, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.PORTAL, cx, cy + 1.5, cz, 80, 1.5, 2.0, 1.5, 0.25);
            serverLevel.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0f, 0.7f);
            serverLevel.playSound(null, worldPosition, ModCinematicSounds.QUAL_SPAWN.get(), SoundSource.HOSTILE, 2.5f, 1.0f);

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
}
