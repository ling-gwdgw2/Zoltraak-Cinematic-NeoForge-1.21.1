package com.frierenvoid.client;

import com.frierenvoid.VoidChoreography;
import com.frierenvoid.ZoltraakEntity;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.HumanoidArm;

final class ZoltraakGesture implements IAnimation {
   private static final Map<UUID, ZoltraakGesture> ACTIVE = new HashMap<>();
   private final AbstractClientPlayer player;
   private final ZoltraakEntity hole;

   private ZoltraakGesture(AbstractClientPlayer p, ZoltraakEntity h) {
      this.player = p;
      this.hole = h;
   }

   static void attach(ZoltraakEntity h) {
      if (!(h.age(0.0F) > 24.0F) && h.level().getEntity(h.casterId()) instanceof AbstractClientPlayer p) {
         if (!ACTIVE.containsKey(p.getUUID())) {
            ZoltraakGesture gesture = new ZoltraakGesture(p, h);
            ACTIVE.put(p.getUUID(), gesture);
            PlayerAnimationAccess.getPlayerAnimLayer(p).addAnimLayer(2500, gesture);
         }
      }
   }

   static void cleanup() {
      ACTIVE.values().removeIf(g -> {
         if (g.isActive()) {
            return false;
         }

         PlayerAnimationAccess.getPlayerAnimLayer(g.player).removeLayer(g);
         return true;
      });
   }

   static void clear() {
      for (ZoltraakGesture g : ACTIVE.values()) {
         PlayerAnimationAccess.getPlayerAnimLayer(g.player).removeLayer(g);
      }

      ACTIVE.clear();
   }

   public boolean isActive() {
      return !this.hole.isRemoved() && !this.player.isRemoved() && this.hole.age(0.0F) < 34.0F;
   }

   public void setupAnim(float partial) {
   }

   public FirstPersonMode getFirstPersonMode(float partial) {
      return FirstPersonMode.THIRD_PERSON_MODEL;
   }

   public FirstPersonConfiguration getFirstPersonConfiguration(float partial) {
      return new FirstPersonConfiguration(true, true, false, false);
   }

   public Vec3f get3DTransform(String part, TransformType type, float partial, Vec3f current) {
      if (type != TransformType.ROTATION) {
         return current;
      }

      boolean right = this.player.getMainArm() == HumanoidArm.RIGHT;
      if (!part.equals(right ? "rightArm" : "leftArm")) {
         return current;
      }

      float age = this.hole.age(partial);
      float weight = VoidChoreography.ease((age + 3.0F) / 7.0F) * (1.0F - VoidChoreography.ease((age - 22.0F) / 12.0F));
      float lift = age >= 14.0F ? (float)Math.exp(-(age - 14.0F) * 0.3) * 0.16F : 0.0F;
      Vec3f target = new Vec3f(-1.5F + this.player.getXRot() * (float) Math.PI / 180.0F - lift, right ? -0.1F : 0.1F, right ? 0.1F : -0.1F);
      return current.scale(1.0F - weight).add(target.scale(weight));
   }
}
