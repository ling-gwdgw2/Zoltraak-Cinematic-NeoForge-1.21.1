package com.frierenvoid.client;

import com.frierenvoid.SingularityEntity;
import com.frierenvoid.VoidChoreography;
import com.frierenvoid.VoidConfig;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class VoidCutscene {
   public static final KeyMapping SKIP = new KeyMapping("key.frierenvoid.skip_cutscene", Type.KEYSYM, 86, "key.categories.frierenvoid");
   private static final Set<UUID> SEEN = new HashSet<>();
   private static SingularityEntity hole;
   private static ArmorStand camera;
   private static Entity previous;
   private static CameraType previousType;
   private static Vec3 origin;
   private static Vec3 forward;
   private static Vec3 right;

   public static boolean active() {
      return hole != null;
   }

   public static void consider(SingularityEntity e) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && e.casterId() == mc.player.getId() && e.rising()) {
         if (SEEN.add(e.getUUID())
            && !active()
            && !(e.age(0.0F) > 12.0F)
            && (Boolean)VoidConfig.CUTSCENE.get()
            && mc.screen == null
            && mc.getCameraEntity() == mc.player) {
            hole = e;
            previous = mc.getCameraEntity();
            previousType = mc.options.getCameraType();
            origin = mc.player.position();
            forward = VoidChoreography.forward(mc.player);
            right = new Vec3(-forward.z, 0.0, forward.x);
            camera = new ArmorStand(mc.level, origin.x, origin.y, origin.z);
            camera.setInvisible(true);
            camera.setNoGravity(true);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.setCameraEntity(camera);
            frame(0.0F);
         }
      }
   }

   public static void tick() {
      Minecraft mc = Minecraft.getInstance();

      while (SKIP.consumeClick()) {
         stop();
      }

      if (mc.level == null) {
         clear();
      } else {
         SEEN.removeIf(id -> !StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), false).anyMatch(e -> e.getUUID().equals(id)));
         if (active()) {
            if (mc.player != null
               && mc.player.isAlive()
               && mc.player.hurtTime <= 0
               && !hole.isRemoved()
               && hole.level() == mc.level
               && !(hole.age(0.0F) >= 238.0F)
               && mc.screen == null
               && (Boolean)VoidConfig.CUTSCENE.get()
               && mc.getCameraEntity() == camera
               && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
               if (mc.options.keyUp.isDown()
                  || mc.options.keyDown.isDown()
                  || mc.options.keyLeft.isDown()
                  || mc.options.keyRight.isDown()
                  || mc.options.keyJump.isDown()
                  || mc.options.keyShift.isDown()) {
                  stop();
               }
            } else {
               stop();
            }
         }
      }
   }

   public static void frame(float partial) {
      if (active()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level == hole.level()) {
            float t = hole.age(partial);
            float rise = VoidChoreography.ease((t - 24.0F) / 94.0F);
            float growth = VoidChoreography.growth(t);
            float release = VoidChoreography.ease((t - 174.0F) / 25.0F);
            double orbit = VoidChoreography.ease((t - 108.0F) / 65.0F) * 0.16;
            Vec3 f = forward.scale(Math.cos(orbit)).add(right.scale(Math.sin(orbit)));
            Vec3 r = right.scale(Math.cos(orbit)).subtract(forward.scale(Math.sin(orbit)));
            Vec3 desired = origin.add(f.scale(3.8 + 3.0F * rise + 24.0F * growth + 5.0F * release))
               .add(r.scale(2.1 + 2.0F * rise + 10.0F * growth + 2.0F * release))
               .add(0.0, 1.6 + 9.2 * rise - 3.0F * growth + 2.0F * release, 0.0);
            Vec3 target = origin.add(0.0, 1.3 + 9.5 * rise - 3.3 * growth + 1.2 * release, 0.0).add(forward.scale(0.45 + 1.4 * rise));
            BlockHitResult hit = mc.level.clip(new ClipContext(target, desired, Block.COLLIDER, Fluid.NONE, mc.player));
            if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
               desired = hit.getLocation().add(target.subtract(desired).normalize().scale(0.4));
            }

            if (!(desired.distanceToSqr(target) < 1.0) && mc.level.hasChunkAt(BlockPos.containing(desired))) {
               Vec3 look = target.subtract(desired);
               Vec3 pos = desired.subtract(0.0, camera.getEyeHeight(), 0.0);
               float yaw = (float)Math.toDegrees(Math.atan2(-look.x, look.z));
               float pitch = (float)(-Math.toDegrees(Math.atan2(look.y, Math.hypot(look.x, look.z))));
               camera.setPos(pos);
               camera.xo = pos.x;
               camera.yo = pos.y;
               camera.zo = pos.z;
               camera.setYRot(yaw);
               camera.yRotO = yaw;
               camera.setYHeadRot(yaw);
               camera.yHeadRotO = yaw;
               camera.setXRot(pitch);
               camera.xRotO = pitch;
            } else {
               stop();
            }
         }
      }
   }

   public static void draw(GuiGraphics gui, float partial) {
      if (active()) {
         Minecraft mc = Minecraft.getInstance();
         float t = hole.age(partial);
         int w = gui.guiWidth();
         int h = gui.guiHeight();
         int bar = (int)(h * 0.085 * VoidChoreography.ease(t / 10.0F) * (1.0F - VoidChoreography.ease((t - 225.0F) / 13.0F)));
         gui.fill(0, 0, w, bar, -16448504);
         gui.fill(0, h - bar, w, h, -16448504);
         if (bar > 12) {
            gui.drawString(
               mc.font, Component.translatable("ui.frierenvoid.skip_cutscene", new Object[]{SKIP.getTranslatedKeyMessage()}), 10, h - bar + 4, -3686707, false
            );
         }
      }
   }

   public static void stop() {
      if (active()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.getCameraEntity() == camera) {
            Entity restore = (Entity)(previous != null && !previous.isRemoved() && previous.level() == mc.level ? previous : mc.player);
            mc.setCameraEntity(restore);
            if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
               mc.options.setCameraType(previousType);
            }
         }

         hole = null;
         camera = null;
         previous = null;
         previousType = null;
      }
   }

   public static void clear() {
      stop();
      SEEN.clear();
   }
}
