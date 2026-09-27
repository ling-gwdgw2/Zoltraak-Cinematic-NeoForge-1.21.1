package com.frierenvoid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;

public final class TerrainAbsorption {
   private TerrainAbsorption() {
   }

   public static boolean lift(SingularityEntity hole, BlockPos pos) {
      if (!(hole.level() instanceof ServerLevel level && hole.canEatTerrain() && level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos))) {
         return false;
      } else {
         if (Vec3.atCenterOf(pos).distanceToSqr(hole.position()) > hole.activeRadius() * hole.activeRadius()) {
            return false;
         }

         BlockState state = level.getBlockState(pos);
         LivingEntity caster = hole.caster();
         if (!state.isAir()
            && !state.hasBlockEntity()
            && state.getFluidState().isEmpty()
            && state.isSolidRender(level, pos)
            && !(state.getDestroySpeed(level, pos) < 0.0F)
            && !(state.getDestroySpeed(level, pos) > (Double)VoidConfig.BLOCK_HARDNESS.get())) {
            if (caster instanceof Player player) {
               if (player.isSpectator() || !player.getAbilities().mayBuild || !level.mayInteract(player, pos)) {
                  return false;
               }

               if (((BreakEvent)NeoForge.EVENT_BUS.post(new BreakEvent(level, pos, state, player))).isCanceled()) {
                  return false;
               }
            } else if (caster == null || !EventHooks.canEntityGrief(level, caster)) {
               return false;
            }

            if (level.getBlockState(pos) == state && level.getBlockEntity(pos) == null) {
               AbsorbedBlockEntity debris = new AbsorbedBlockEntity(hole, Vec3.atCenterOf(pos), state);
               if (!level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) {
                  return false;
               } else if (!level.addFreshEntity(debris)) {
                  level.setBlock(pos, state, 3);
                  return false;
               } else {
                  hole.recordEatenBlock();
                  return true;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public static void pulse(SingularityEntity hole) {
      if (hole.canEatTerrain()) {
         Level level = hole.level();
         double reach = hole.activeRadius();
         int lifted = 0;

         for (int sample = 0; sample < 32 && lifted < VoidConfig.BLOCKS_PER_PULSE.get(); sample++) {
            double a = level.random.nextDouble() * Math.PI * 2.0;
            double r = Math.sqrt(level.random.nextDouble()) * reach * 0.88;
            int x = (int)Math.floor(hole.getX() + Math.cos(a) * r);
            int z = (int)Math.floor(hole.getZ() + Math.sin(a) * r);
            int y = (int)Math.floor(hole.getY() + 2.0);

            while (y >= Math.max(level.getMinBuildHeight(), hole.getY() - reach)) {
               BlockPos pos = new BlockPos(x, y, z);
               if (level.hasChunkAt(pos)) {
                  if (level.getBlockState(pos).isAir()) {
                     y--;
                     continue;
                  } else if (lift(hole, pos)) {
                     lifted++;
                  }
               }
            }
         }
      }
   }
}
