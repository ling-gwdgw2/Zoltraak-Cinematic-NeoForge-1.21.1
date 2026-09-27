package com.frierenvoid.client;

import com.frierenvoid.VoidConfig;
import com.frierenvoid.ZoltraakEntity;
import com.frierenvoid.ZoltraakMode;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;

@EventBusSubscriber(modid = "frierenvoid", value = Dist.CLIENT)
public final class ZoltraakClient {
   private static ZoltraakEntity own;

   @SubscribeEvent
   public static void tick(Post e) {
      ZoltraakGesture.cleanup();
      own = null;
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ZoltraakEntity z && !z.isRemoved()) {
               ZoltraakGesture.attach(z);
               if (z.casterId() == mc.player.getId() && z.rawAge(0.0F) >= 0.0F && (own == null || z.fired() && (!own.fired() || z.age(0.0F) < own.age(0.0F)))) {
                  own = z;
               }
            }
         }
      } else {
         ZoltraakGesture.clear();
      }
   }

   @SubscribeEvent
   public static void unload(Unload e) {
      if (e.getLevel().isClientSide()) {
         own = null;
         ZoltraakGesture.clear();
      }
   }

   @SubscribeEvent
   public static void camera(ComputeCameraAngles e) {
      Minecraft mc = Minecraft.getInstance();
      if (own != null && !mc.isPaused() && mc.getCameraEntity() == mc.player) {
         float dt = own.mode() == ZoltraakMode.LARGE ? own.rawAge((float)e.getPartialTick()) - 20.0F : own.age((float)e.getPartialTick()) - 14.0F;
         if (!(dt < 0.0F) && !(dt > 12.0F)) {
            float strength = ((Double)VoidConfig.SHAKE.get()).floatValue() * (float)Math.exp(-dt * 0.35);
            e.setPitch(e.getPitch() - strength * 1.2F);
            e.setYaw(e.getYaw() + (float)Math.sin(dt * 2.0F) * strength * 0.3F);
         }
      }
   }

   @SubscribeEvent
   public static void overlay(net.neoforged.neoforge.client.event.RenderGuiEvent.Post e) {
      Minecraft mc = Minecraft.getInstance();
      if (own != null && mc.screen == null && mc.getCameraEntity() == mc.player) {
         float dt = own.mode() == ZoltraakMode.LARGE
            ? own.rawAge(e.getPartialTick().getGameTimeDeltaPartialTick(false)) - 20.0F
            : own.age(e.getPartialTick().getGameTimeDeltaPartialTick(false)) - 14.0F;
         if (!(dt < 0.0F) && !(dt > 8.0F)) {
            int alpha = (int)((own.mode() == ZoltraakMode.LARGE ? 165 : 45) * (Double)VoidConfig.FLASH.get() * Math.exp(-dt * 0.6));
            e.getGuiGraphics()
               .fill(
                  0,
                  0,
                  e.getGuiGraphics().guiWidth(),
                  e.getGuiGraphics().guiHeight(),
                  alpha << 24 | (own.mode() == ZoltraakMode.LARGE ? 14547199 : (own.black() ? 3483716 : 14348031))
               );
         }
      }
   }
}
