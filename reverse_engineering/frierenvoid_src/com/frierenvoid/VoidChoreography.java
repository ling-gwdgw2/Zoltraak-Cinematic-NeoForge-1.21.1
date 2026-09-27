package com.frierenvoid;

import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class VoidChoreography {
   public static final int HAND_END = 24;
   public static final int RISE_END = 118;
   public static final int GROW_START = 24;
   public static final int GROW_END = 118;
   public static final int FEED_START = 54;

   public static float ease(double value) {
      float t = (float)Math.max(0.0, Math.min(1.0, value));
      return t * t * (3.0F - 2.0F * t);
   }

   public static Vec3 forward(LivingEntity caster) {
      double yaw = Math.toRadians(caster.getYRot());
      return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
   }

   public static Vec3 palm(LivingEntity caster) {
      Vec3 f = forward(caster);
      Vec3 right = new Vec3(-f.z, 0.0, f.x);
      double side = caster.getMainArm() == HumanoidArm.RIGHT ? 1.0 : -1.0;
      return caster.position().add(0.0, caster.getEyeHeight() - 0.36, 0.0).add(f.scale(0.74)).add(right.scale(0.34 * side));
   }

   public static Vec3 flight(Vec3 origin, Vec3 facing, float age) {
      float rise = ease((age - 24.0F) / 94.0);
      return origin.add(facing.scale(1.6 * rise)).add(0.0, 9.5 * rise, 0.0);
   }

   public static float core(float age) {
      if (age >= 200.0F) {
         return 0.0F;
      }

      float radius = 0.12F + 0.025F * ease(age / 24.0F);
      radius += 2.555F * growth(age);
      return radius * (1.0F - ease((age - 176.0F) / 24.0F));
   }

   public static float growth(float age) {
      return ease((age - 24.0F) / 94.0F);
   }

   public static float reach(float age) {
      return ease((age - 54.0F) / 64.0F);
   }

   private VoidChoreography() {
   }
}
