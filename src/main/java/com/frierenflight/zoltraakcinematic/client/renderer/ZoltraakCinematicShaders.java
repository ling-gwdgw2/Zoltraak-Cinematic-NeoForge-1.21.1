package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

public final class ZoltraakCinematicShaders {

   public static ShaderInstance corona;
   public static ShaderInstance lens;
   public static ShaderInstance zoltraak;
   public static ShaderInstance blackZoltraak;
   public static ShaderInstance blackZoltraakHead;
   public static ShaderInstance greatBlack;
   public static ShaderInstance greatWhite;
   public static ShaderInstance greatPlume;
   public static ShaderInstance magicCircle;
   public static ShaderInstance blackMagicCircle;

   @SubscribeEvent
   public static void onRegisterShaders(RegisterShadersEvent e) {
      registerSafe(e, "corona", s -> corona = s);
      registerSafe(e, "lens", s -> lens = s);
      registerSafe(e, "zoltraak_beam", s -> zoltraak = s);
      registerSafe(e, "black_zoltraak_beam", s -> blackZoltraak = s);
      registerSafe(e, "black_zoltraak_head", s -> blackZoltraakHead = s);
      registerSafe(e, "great_zoltraak_black", s -> greatBlack = s);
      registerSafe(e, "great_zoltraak_white", s -> greatWhite = s);
      registerSafe(e, "great_zoltraak_plume", s -> greatPlume = s);
      registerSafe(e, "magic_circle", s -> magicCircle = s);
      registerSafe(e, "black_magic_circle", s -> blackMagicCircle = s);
   }

   private static void registerSafe(RegisterShadersEvent e, String name, java.util.function.Consumer<ShaderInstance> onLoaded) {
      try {
         e.registerShader(new ShaderInstance(e.getResourceProvider(), id(name), DefaultVertexFormat.POSITION_TEX_COLOR), onLoaded);
      } catch (IOException ex) {
         System.err.println("[ZoltraakCinematic] Failed to register core shader '" + name + "': " + ex.getMessage());
      }
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, path);
   }

   private ZoltraakCinematicShaders() {}
}
