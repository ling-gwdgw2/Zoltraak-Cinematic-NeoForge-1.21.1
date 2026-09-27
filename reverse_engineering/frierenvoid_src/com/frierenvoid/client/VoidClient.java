package com.frierenvoid.client;

import com.frierenvoid.FrierenVoid;
import com.frierenvoid.SingularityEntity;
import com.frierenvoid.VoidConfig;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.event.RenderFrameEvent.Pre;
import net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;

public final class VoidClient {
   static ShaderInstance corona;
   static ShaderInstance lens;
   static ShaderInstance zoltraak;
   static ShaderInstance blackZoltraak;
   static ShaderInstance blackZoltraakHead;
   static ShaderInstance greatBlack;
   static ShaderInstance greatWhite;
   static ShaderInstance greatPlume;

   @EventBusSubscriber(modid = "frierenvoid", value = Dist.CLIENT)
   public static final class Events {
      private static final Map<Integer, Integer> phases = new HashMap<>();
      private static SingularityEntity nearest;

      @SubscribeEvent
      public static void tick(Post event) {
         Minecraft mc = Minecraft.getInstance();
         nearest = null;
         PalmGesture.cleanup();
         VoidCutscene.tick();
         if (mc.level != null && mc.player != null) {
            Set<Integer> alive = new HashSet<>();
            double dist = 9216.0;

            for (Entity entity : mc.level.entitiesForRendering()) {
               if (entity instanceof SingularityEntity e && !e.isRemoved()) {
                  alive.add(e.getId());
                  float age = e.age(0.0F);
                  PalmGesture.attach(e);
                  VoidCutscene.consider(e);
                  int phase = age < 30.0F ? 0 : (age < 170.0F ? 1 : (age < 200.0F ? 2 : 3));
                  Integer old = phases.put(e.getId(), phase);
                  if (old == null || old != phase) {
                     SoundEvent sound = switch (phase) {
                        case 0 -> (SoundEvent)FrierenVoid.OPEN.get();
                        case 1 -> (SoundEvent)FrierenVoid.ACCRETION.get();
                        case 2 -> (SoundEvent)FrierenVoid.COLLAPSE.get();
                        default -> (SoundEvent)FrierenVoid.RELEASE.get();
                     };
                     mc.getSoundManager().play(new VoidClient.VoidSound(e, sound, phase));
                  }

                  double d = e.distanceToSqr(mc.player);
                  if (d < dist) {
                     dist = d;
                     nearest = e;
                  }
               }
            }

            phases.keySet().retainAll(alive);
         } else {
            phases.clear();
            PalmGesture.clear();
         }
      }

      @SubscribeEvent
      public static void unload(Unload event) {
         if (event.getLevel().isClientSide()) {
            nearest = null;
            phases.clear();
            SceneLens.release();
            PalmGesture.clear();
            VoidCutscene.clear();
         }
      }

      @SubscribeEvent
      public static void frame(Pre event) {
         VoidCutscene.frame(event.getPartialTick().getGameTimeDeltaPartialTick(false));
      }

      @SubscribeEvent
      public static void hud(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre event) {
         if (VoidCutscene.active()) {
            event.setCanceled(true);
         }
      }

      @SubscribeEvent
      public static void hand(RenderHandEvent event) {
         if (VoidCutscene.active()) {
            event.setCanceled(true);
         }
      }

      @SubscribeEvent
      public static void name(RenderNameTagEvent event) {
         if (VoidCutscene.active()) {
            event.setCanRender(TriState.FALSE);
         }
      }

      @SubscribeEvent
      public static void camera(ComputeCameraAngles event) {
         Minecraft mc = Minecraft.getInstance();
         if (nearest != null && mc.player != null && !mc.isPaused()) {
            float t = nearest.age((float)event.getPartialTick());
            float proximity = (float)Math.max(0.0, 1.0 - Math.sqrt(nearest.distanceToSqr(mc.player)) / 64.0);
            float impulse = t >= 200.0F ? 1.0F - SingularityRenderer.ease((t - 200.0F) / 16.0F) : 0.16F + SingularityRenderer.ease((t - 170.0F) / 30.0F) * 0.5F;
            float strength = proximity * impulse * ((Double)VoidConfig.SHAKE.get()).floatValue();
            event.setPitch(event.getPitch() + (float)Math.sin(t * 2.7) * strength);
            event.setYaw(event.getYaw() + (float)Math.sin(t * 1.93) * strength * 0.5F);
            event.setRoll(event.getRoll() + (float)Math.sin(t * 1.31) * strength * 0.7F);
         }
      }

      @SubscribeEvent
      public static void overlay(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
         Minecraft mc = Minecraft.getInstance();
         if (nearest != null && mc.player != null && mc.screen == null) {
            float t = nearest.age(event.getPartialTick().getGameTimeDeltaPartialTick(false));
            float p = (float)Math.max(0.0, 1.0 - Math.sqrt(nearest.distanceToSqr(mc.player)) / 64.0);
            float a = SingularityRenderer.ease(t / 35.0F) * (1.0F - SingularityRenderer.ease((t - 200.0F) / 35.0F)) * p;
            int w = event.getGuiGraphics().guiWidth();
            int h = event.getGuiGraphics().guiHeight();
            int dark = (int)(a * 45.0F) << 24 | 330008;
            if ((Boolean)VoidConfig.ATMOSPHERE.get()) {
               event.getGuiGraphics().fillGradient(0, 0, w, h / 4, dark, 330008);
               event.getGuiGraphics().fillGradient(0, h * 3 / 4, w, h, 330008, dark);
            }

            float dt = t - 200.0F;
            if (dt >= 0.0F && dt < 22.0F) {
               float intensity = ((Double)VoidConfig.FLASH.get()).floatValue() * p;
               int white = (int)(255.0F * intensity * Math.exp(-dt * 0.34));
               int violet = (int)(58.0F * intensity * (1.0 - Math.exp(-dt * 0.55)) * (1.0F - SingularityRenderer.ease(dt / 22.0F)));
               event.getGuiGraphics().fill(0, 0, w, h, white << 24 | 16775423);
               event.getGuiGraphics().fill(0, 0, w, h, violet << 24 | 11823103);
            }

            VoidCutscene.draw(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
         }
      }
   }

   @EventBusSubscriber(modid = "frierenvoid", bus = Bus.MOD, value = Dist.CLIENT)
   public static final class Registration {
      @SubscribeEvent
      public static void keys(RegisterKeyMappingsEvent event) {
         event.register(VoidCutscene.SKIP);
         event.register(ZoltraakModeInput.CYCLE);
      }

      @SubscribeEvent
      public static void renderers(RegisterRenderers event) {
         event.registerEntityRenderer((EntityType)FrierenVoid.DEFENSE_ENTITY.get(), DefenseRenderer::new);
         event.registerEntityRenderer((EntityType)FrierenVoid.ZOLTRAAK_ENTITY.get(), ZoltraakRenderer::new);
         event.registerEntityRenderer((EntityType)FrierenVoid.ENTITY.get(), SingularityRenderer::new);
         event.registerEntityRenderer((EntityType)FrierenVoid.BLOCK_ENTITY.get(), AbsorbedBlockRenderer::new);
      }

      @SubscribeEvent
      public static void shaders(RegisterShadersEvent e) throws IOException {
         e.registerShader(
            new ShaderInstance(e.getResourceProvider(), FrierenVoid.id("corona"), DefaultVertexFormat.POSITION_TEX_COLOR), s -> VoidClient.corona = s
         );
         e.registerShader(new ShaderInstance(e.getResourceProvider(), FrierenVoid.id("lens"), DefaultVertexFormat.POSITION_TEX_COLOR), s -> VoidClient.lens = s);
         e.registerShader(
            new ShaderInstance(e.getResourceProvider(), FrierenVoid.id("zoltraak_beam"), DefaultVertexFormat.POSITION_TEX_COLOR), s -> VoidClient.zoltraak = s
         );
         e.registerShader(
            new ShaderInstance(e.getResourceProvider(), FrierenVoid.id("black_zoltraak_beam"), DefaultVertexFormat.POSITION_TEX_COLOR),
            s -> VoidClient.blackZoltraak = s
         );
         e.registerShader(
            new ShaderInstance(e.getResourceProvider(), FrierenVoid.id("black_zoltraak_head"), DefaultVertexFormat.POSITION_TEX_COLOR),
            s -> VoidClient.blackZoltraakHead = s
         );
         e.registerShader(
            new ShaderInstance(e.getResourceProvider(), FrierenVoid.id("great_zoltraak_black"), DefaultVertexFormat.POSITION_TEX_COLOR),
            s -> VoidClient.greatBlack = s
         );
         e.registerShader(
            new ShaderInstance(e.getResourceProvider(), FrierenVoid.id("great_zoltraak_white"), DefaultVertexFormat.POSITION_TEX_COLOR),
            s -> VoidClient.greatWhite = s
         );
         e.registerShader(
            new ShaderInstance(e.getResourceProvider(), FrierenVoid.id("great_zoltraak_plume"), DefaultVertexFormat.POSITION_TEX_COLOR),
            s -> VoidClient.greatPlume = s
         );
         VoidClient.Events.phases.clear();
      }
   }

   private static final class VoidSound extends AbstractTickableSoundInstance {
      private final SingularityEntity entity;
      private final int phase;

      VoidSound(SingularityEntity e, SoundEvent sound, int phase) {
         super(sound, SoundSource.PLAYERS, RandomSource.create());
         this.entity = e;
         this.phase = phase;
         this.x = e.getX();
         this.y = e.getY();
         this.z = e.getZ();
         this.looping = phase == 1;
         this.delay = 0;
         this.volume = ((Double)VoidConfig.VOLUME.get()).floatValue() * 2.0F;
      }

      public void tick() {
         float t = this.entity.age(0.0F);
         this.x = this.entity.getX();
         this.y = this.entity.getY();
         this.z = this.entity.getZ();
         if (!this.entity.isRemoved() && this.entity.level() == Minecraft.getInstance().level && (this.phase != 1 || !(t >= 170.0F))) {
            this.volume = ((Double)VoidConfig.VOLUME.get()).floatValue()
               * 2.0F
               * (this.phase == 1 ? SingularityRenderer.ease((t - 30.0F) / 15.0F) * (1.0F - SingularityRenderer.ease((t - 155.0F) / 15.0F)) : 1.0F);
         } else {
            this.stop();
         }
      }
   }
}
