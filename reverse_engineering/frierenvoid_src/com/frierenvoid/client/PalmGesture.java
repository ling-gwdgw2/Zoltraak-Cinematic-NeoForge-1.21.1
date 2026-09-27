package com.frierenvoid.client;

import com.frierenvoid.SingularityEntity;
import com.frierenvoid.VoidChoreography;
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

final class PalmGesture implements IAnimation {
   private static final Map<UUID, PalmGesture> ACTIVE = new HashMap<>();
   private final AbstractClientPlayer player;
   private final SingularityEntity hole;

   private PalmGesture(AbstractClientPlayer p, SingularityEntity h) {
      this.player = p;
      this.hole = h;
   }

   static void attach(SingularityEntity h) {
      if (h.rising() && !(h.age(0.0F) > 70.0F) && h.level().getEntity(h.casterId()) instanceof AbstractClientPlayer p) {
         if (!ACTIVE.containsKey(p.getUUID())) {
            PalmGesture gesture = new PalmGesture(p, h);
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
      for (PalmGesture g : ACTIVE.values()) {
         PlayerAnimationAccess.getPlayerAnimLayer(g.player).removeLayer(g);
      }

      ACTIVE.clear();
   }

   public boolean isActive() {
      return !this.hole.isRemoved() && !this.player.isRemoved() && this.hole.age(0.0F) < 76.0F;
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
      float weight = VoidChoreography.ease((age + 4.0F) / 10.0F) * (1.0F - VoidChoreography.ease((age - 52.0F) / 24.0F));
      float lift = VoidChoreography.ease((age - 18.0F) / 30.0F);
      Vec3f target = new Vec3f(-1.28F - lift * 0.72F, right ? -0.1F : 0.1F, right ? 0.1F : -0.1F);
      return current.scale(1.0F - weight).add(target.scale(weight));
   }
}
