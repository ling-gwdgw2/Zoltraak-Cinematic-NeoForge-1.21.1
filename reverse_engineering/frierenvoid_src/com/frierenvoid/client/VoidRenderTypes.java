package com.frierenvoid.client;

import com.frierenvoid.FrierenVoid;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderStateShard.TransparencyStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;

final class VoidRenderTypes extends RenderType {
   private static final TransparencyStateShard ADD = new TransparencyStateShard("void_add", () -> {
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
   }, () -> {
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   });
   static final RenderType DEFENSE_SURFACE = create(
      "defense_surface",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.TRIANGLES,
      32768,
      false,
      false,
      CompositeState.builder()
         .setShaderState(POSITION_COLOR_SHADER)
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   static final RenderType CORE = create(
      "void_core",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.TRIANGLES,
      65536,
      false,
      false,
      CompositeState.builder()
         .setShaderState(POSITION_COLOR_SHADER)
         .setTransparencyState(NO_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_DEPTH_WRITE)
         .createCompositeState(false)
   );
   static final RenderType LIGHT = create(
      "void_light",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.TRIANGLES,
      262144,
      false,
      false,
      CompositeState.builder()
         .setShaderState(POSITION_COLOR_SHADER)
         .setTransparencyState(ADD)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   static final RenderType CORONA = create(
      "void_corona",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      4096,
      false,
      false,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(() -> VoidClient.corona))
         .setTransparencyState(ADD)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   static final RenderType LENS = create(
      "void_lens",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      4096,
      false,
      false,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(() -> VoidClient.lens))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   static final RenderType ZOL_CIRCLE = create(
      "zol_circle",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      8192,
      false,
      false,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
         .setTextureState(new TextureStateShard(FrierenVoid.id("textures/spell/zoltraak_circle.png"), true, false))
         .setTransparencyState(ADD)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   static final RenderType ZOL_GLOW = create(
      "zol_glow",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      32768,
      false,
      false,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
         .setTextureState(new TextureStateShard(FrierenVoid.id("textures/spell/zoltraak_glow.png"), true, false))
         .setTransparencyState(ADD)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   static final RenderType ZOL_BEAM = create(
      "zol_beam",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      8192,
      false,
      false,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(() -> VoidClient.zoltraak))
         .setTransparencyState(ADD)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   static final RenderType BLACK_CIRCLE = create(
      "black_zol_circle",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      8192,
      false,
      false,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
         .setTextureState(new TextureStateShard(FrierenVoid.id("textures/spell/zoltraak_circle.png"), true, false))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   static final RenderType BLACK_GLOW = create(
      "black_zol_glow",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      32768,
      false,
      false,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
         .setTextureState(new TextureStateShard(FrierenVoid.id("textures/spell/zoltraak_glow.png"), true, false))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   static final RenderType BLACK_CORE = create(
      "black_zol_core",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.TRIANGLES,
      32768,
      false,
      false,
      CompositeState.builder()
         .setShaderState(POSITION_COLOR_SHADER)
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   static final RenderType GREAT_WHITE = create(
      "great_zol_white",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      8192,
      false,
      false,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(() -> VoidClient.greatWhite))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   static final RenderType GREAT_PLUME = create(
      "great_zol_plume",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      8192,
      false,
      false,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(() -> VoidClient.greatPlume))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   static final RenderType GREAT_BLACK = create(
      "great_zol_black",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      8192,
      false,
      false,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(() -> VoidClient.greatBlack))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   static final RenderType BLACK_BEAM = create(
      "black_zol_beam",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      8192,
      false,
      false,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(() -> VoidClient.blackZoltraak))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );
   static final RenderType BLACK_HEAD = create(
      "black_zol_head",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      8192,
      false,
      false,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(() -> VoidClient.blackZoltraakHead))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );

   private VoidRenderTypes(String n, VertexFormat f, Mode m, int s, boolean c, boolean o, Runnable a, Runnable b) {
      super(n, f, m, s, c, o, a, b);
   }
}
