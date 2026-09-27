package com.frierenvoid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class AbsorbedBlockEntity extends Entity {
   private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(AbsorbedBlockEntity.class, EntityDataSerializers.INT);
   private int targetId;
   private int lerpSteps;
   private Vec3 lerpTarget = Vec3.ZERO;

   public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
      this.lerpTarget = new Vec3(x, y, z);
      this.lerpSteps = Math.max(1, steps);
   }

   public AbsorbedBlockEntity(EntityType<? extends AbsorbedBlockEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.setNoGravity(true);
   }

   public AbsorbedBlockEntity(SingularityEntity hole, Vec3 start, BlockState state) {
      this((EntityType<? extends AbsorbedBlockEntity>)FrierenVoid.BLOCK_ENTITY.get(), hole.level());
      this.setPos(start);
      this.targetId = hole.getId();
      this.entityData.set(STATE, Block.getId(state));
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(STATE, Block.getId(Blocks.STONE.defaultBlockState()));
   }

   public BlockState blockState() {
      return Block.stateById((Integer)this.entityData.get(STATE));
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide) {
         if (this.lerpSteps > 0) {
            this.setPos(this.position().lerp(this.lerpTarget, 1.0 / this.lerpSteps));
            this.lerpSteps--;
         }
      } else if (this.level().getEntity(this.targetId) instanceof SingularityEntity hole
         && !hole.isRemoved()
         && !(hole.age(0.0F) >= 200.0F)
         && this.tickCount <= 120) {
         Vec3 delta = hole.position().subtract(this.position());
         double distance = delta.length();
         if (distance < Math.max(0.45, VoidChoreography.core(hole.age(0.0F)) * 0.82)) {
            this.discard();
         } else {
            Vec3 radial = delta.normalize();
            Vec3 tangent = new Vec3(-radial.z, 0.0, radial.x);
            Vec3 velocity = this.getDeltaMovement().scale(0.78).add(radial.scale(0.12 + Math.min(0.16, this.tickCount * 0.004))).add(tangent.scale(0.085));
            if (this.tickCount < 12) {
               velocity = velocity.add(0.0, 0.08, 0.0);
            }

            if (velocity.length() > 1.1) {
               velocity = velocity.normalize().scale(1.1);
            }

            this.setDeltaMovement(velocity);
            this.setPos(this.position().add(velocity));
            this.hasImpulse = true;
         }
      } else {
         this.discard();
      }
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
   }
}
