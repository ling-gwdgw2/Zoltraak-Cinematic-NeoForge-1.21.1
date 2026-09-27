package com.frierenvoid;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;

final class GreatZoltraakTerrain {
   static GreatZoltraakTerrain.Result advance(
      ServerLevel level, LivingEntity caster, Vec3 origin, Vec3 dir, double start, double target, double radius, int remaining, boolean enabled
   ) {
      double current = start;
      int removed = 0;
      double halfAxial = (Math.abs(dir.x) + Math.abs(dir.y) + Math.abs(dir.z)) * 0.5;
      Vec3 pad = new Vec3(
         radius * Math.sqrt(Math.max(0.0, 1.0 - dir.x * dir.x)) + 0.5,
         radius * Math.sqrt(Math.max(0.0, 1.0 - dir.y * dir.y)) + 0.5,
         radius * Math.sqrt(Math.max(0.0, 1.0 - dir.z * dir.z)) + 0.5
      );

      while (current < target - 1.0E-4) {
         double next = Math.min(target, current + 0.5);
         Vec3 a = origin.add(dir.scale(current));
         Vec3 b = origin.add(dir.scale(next));
         AABB box = new AABB(a, b).inflate(pad.x, pad.y, pad.z);
         ArrayList<GreatZoltraakTerrain.Cell> cells = new ArrayList<>();

         for (BlockPos mutable : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            Vec3 delta = Vec3.atCenterOf(mutable).subtract(origin);
            double along = delta.dot(dir);
            if (!(along + halfAxial < current) && !(along - halfAxial > next) && !(delta.lengthSqr() - along * along > radius * radius)) {
               if (level.hasChunkAt(mutable) && level.getWorldBorder().isWithinBounds(mutable) && !level.isOutsideBuildHeight(mutable)) {
                  BlockState state = level.getBlockState(mutable);
                  if (state.isAir() || state.getCollisionShape(level, mutable).isEmpty()) {
                     continue;
                  }

                  if (enabled && breakable(level, caster, mutable, state)) {
                     cells.add(new GreatZoltraakTerrain.Cell(mutable.immutable(), state));
                     continue;
                  }

                  return new GreatZoltraakTerrain.Result(current, true, removed);
               }

               return new GreatZoltraakTerrain.Result(current, true, removed);
            }
         }

         if (cells.size() + removed > Math.min(512, remaining)) {
            return new GreatZoltraakTerrain.Result(current, true, removed);
         }

         for (GreatZoltraakTerrain.Cell cell : cells) {
            if (level.getBlockState(cell.pos) != cell.state || level.getBlockEntity(cell.pos) != null) {
               return new GreatZoltraakTerrain.Result(current, true, removed);
            }

            if (!level.setBlock(cell.pos, Blocks.AIR.defaultBlockState(), 3)) {
               return new GreatZoltraakTerrain.Result(current, true, removed);
            }

            if (removed % 8 == 0) {
               Vec3 p = Vec3.atCenterOf(cell.pos);
               level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, cell.state), p.x, p.y, p.z, 5, 0.35, 0.35, 0.35, 0.18);
            }

            removed++;
         }

         current = next;
      }

      return new GreatZoltraakTerrain.Result(current, false, removed);
   }

   private static boolean breakable(ServerLevel level, LivingEntity caster, BlockPos pos, BlockState state) {
      float hardness = state.getDestroySpeed(level, pos);
      if (!(hardness < 0.0F)
         && !(hardness >= 50.0F)
         && !(state.getBlock().getExplosionResistance() >= 1200.0F)
         && !state.hasBlockEntity()
         && state.getFluidState().isEmpty()
         && state.canEntityDestroy(level, pos, caster)) {
         if (caster instanceof Player player) {
            if (player.isSpectator() || !player.getAbilities().mayBuild || !level.mayInteract(player, pos)) {
               return false;
            }

            if (((BreakEvent)NeoForge.EVENT_BUS.post(new BreakEvent(level, pos, state, player))).isCanceled()) {
               return false;
            }
         } else if (!EventHooks.canEntityGrief(level, caster)) {
            return false;
         }

         return level.getBlockState(pos) == state && level.getBlockEntity(pos) == null;
      } else {
         return false;
      }
   }

   private record Cell(BlockPos pos, BlockState state) {
   }

   record Result(double distance, boolean blocked, int removed) {
   }
}
