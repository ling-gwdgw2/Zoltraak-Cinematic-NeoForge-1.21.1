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
                sl.playSound(null, this.worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5f, 0.6f);
                sl.playSound(null, this.worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.2f, 0.75f);
            }
        }
    }

    public void clientTick() {
        if (!this.unsealing) {
            return;
        }
        this.unsealTicks++;

        double cx = worldPosition.getX() + 0.5;
        double cy = worldPosition.getY() + 0.46;
        double cz = worldPosition.getZ() + 0.5;

        // Stage 1 & 2 (Ticks 0..65): Rising embers & sparks around the magic circle
        if (unsealTicks <= 65) {
            if (level != null && level.random.nextFloat() < 0.6f) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double r = 0.4 + level.random.nextDouble() * 0.9;
                level.addParticle(ParticleTypes.FLAME, cx + Math.cos(angle) * r, cy + 0.05, cz + Math.sin(angle) * r, 0, 0.03 + level.random.nextDouble() * 0.04, 0);
            }
            if (level != null && level.random.nextFloat() < 0.2f) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double r = 0.7 + level.random.nextDouble() * 0.6;
                level.addParticle(ParticleTypes.LAVA, cx + Math.cos(angle) * r, cy + 0.1, cz + Math.sin(angle) * r, 0, 0.02, 0);
            }
        }
        // Stage 3 (Ticks 65..99): Erupting skyward fire pillar sparks & camera rumble
        else if (unsealTicks < TOTAL_UNSEAL_TICKS) {
            if (level != null) {
                for (int i = 0; i < 4; i++) {
                    double angle = level.random.nextDouble() * Math.PI * 2;
                    double r = level.random.nextDouble() * 0.35;
                    double vy = 0.25 + level.random.nextDouble() * 0.45;
                    level.addParticle(ParticleTypes.FLAME, cx + Math.cos(angle) * r, cy + 0.1, cz + Math.sin(angle) * r, (level.random.nextDouble() - 0.5) * 0.06, vy, (level.random.nextDouble() - 0.5) * 0.06);
                }
                if (level.random.nextFloat() < 0.4f) {
                    level.addParticle(ParticleTypes.LAVA, cx + (level.random.nextDouble() - 0.5) * 0.4, cy + 0.2, cz + (level.random.nextDouble() - 0.5) * 0.4, 0, 0.1, 0);
                }
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

        // Periodic sync with clients
        if (unsealTicks % 10 == 0) {
            serverLevel.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }

        // Stage 1 (0.0s – 1.5s, Ticks 0..30): 8-Pointed Star Activation & Embers
        if (unsealTicks <= 30) {
            serverLevel.sendParticles(ParticleTypes.FLAME, cx, cy + 0.2, cz, 4, 0.3, 0.1, 0.3, 0.02);
            if (unsealTicks % 10 == 0) {
                float pitch = 0.8f + (unsealTicks / 30.0f) * 0.3f;
                serverLevel.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2f, pitch);
            }
        }
        // Stage 2 (1.5s – 3.25s, Ticks 30..65): 3D Runic Ring Ascent & Escalation
        else if (unsealTicks <= 65) {
            serverLevel.sendParticles(ParticleTypes.LAVA, cx, cy + 0.5, cz, 2, 0.4, 0.2, 0.4, 0.05);
            serverLevel.sendParticles(ParticleTypes.ENCHANT, cx, cy + 0.8, cz, 6, 0.5, 0.3, 0.5, 0.1);

            if (unsealTicks % 12 == 0) {
                serverLevel.playSound(null, worldPosition, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.1f, 0.85f);
                serverLevel.playSound(null, worldPosition, ModCinematicSounds.QUAL_PHASE_TRANSITION.get(), SoundSource.BLOCKS, 1.0f, 1.2f);
            }
        }
        // Stage 3 (3.25s – 5.0s, Ticks 65..99): Skyward Fire Pillar & Earthquake Rumble
        else if (unsealTicks < TOTAL_UNSEAL_TICKS) {
            for (int y = 0; y < 20; y += 3) {
                serverLevel.sendParticles(ParticleTypes.FLAME, cx, cy + y, cz, 4, 0.3, 0.5, 0.3, 0.08);
                serverLevel.sendParticles(ParticleTypes.SMOKE, cx, cy + y, cz, 2, 0.2, 0.4, 0.2, 0.04);
            }

            if (unsealTicks % 6 == 0) {
                serverLevel.playSound(null, worldPosition, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.4f, 0.65f);
                serverLevel.playSound(null, worldPosition, SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 1.2f, 0.6f);
            }
        }
        // Stage 4 (5.0s, Tick 100): Final Cataclysmic Shatter & Boss Manifestation!
        else {
            // Shatter blast VFX & SFX
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy + 1.0, cz, 3, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.LAVA, cx, cy + 1.5, cz, 40, 1.5, 1.5, 1.5, 0.3);
            serverLevel.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.5f, 0.7f);
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
