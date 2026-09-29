package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public final class ZoltraakRenderTypes extends RenderType {
   private static final ResourceLocation TEX_GLOW = ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "textures/spell/zoltraak_glow.png");
   private static final ResourceLocation TEX_CIRCLE = ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "textures/spell/zoltraak_magic_circle.png");
   private static final ResourceLocation TEX_ATLAS = ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "textures/entity/zoltraak_vfx_atlas.png");

   // Use vanilla LIGHTNING_TRANSPARENCY so Iris / Sodium / shaderpacks recognize the additive blending
   // and automatically route it to the emissive glowing HDR bloom pipeline.
   private static final TransparencyStateShard ADD = LIGHTNING_TRANSPARENCY;

   private static ShaderStateShard safeShader(Supplier<ShaderInstance> primary) {
      return new ShaderStateShard(() -> {
         ShaderInstance s = primary.get();
         return s != null ? s : GameRenderer.getPositionTexColorShader();
      });
   }

   private static ShaderStateShard safeShaderColor(Supplier<ShaderInstance> primary) {
      return new ShaderStateShard(() -> {
         ShaderInstance s = primary.get();
         return s != null ? s : GameRenderer.getPositionColorShader();
      });
   }

   public static final RenderType LIGHT = create(
      "zol_light",
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

   public static final RenderType DEFENSE_SURFACE = create(
      "zol_defense_surface",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.TRIANGLES,
      32768,
      false,
      false,
      CompositeState.builder()
         .setShaderState(safeShaderColor(() -> ZoltraakCinematicShaders.defenseBarrier))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );

   public static final RenderType CORONA = create(
      "zol_corona",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      4096,
      false,
      false,
      CompositeState.builder()
         .setShaderState(safeShader(() -> ZoltraakCinematicShaders.corona))
         .setTextureState(new TextureStateShard(TEX_GLOW, true, false))
         .setTransparencyState(ADD)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );

   public static final RenderType LENS = create(
      "zol_lens",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      4096,
      false,
      false,
      CompositeState.builder()
         .setShaderState(safeShader(() -> ZoltraakCinematicShaders.lens))
         .setTextureState(new TextureStateShard(TEX_GLOW, true, false))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );

   public static final RenderType ZOL_CIRCLE = create(
      "zol_circle",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      8192,
      false,
      false,
      CompositeState.builder()
         .setShaderState(safeShader(() -> ZoltraakCinematicShaders.magicCircle))
         .setTextureState(new TextureStateShard(TEX_CIRCLE, true, false))
         .setTransparencyState(ADD)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );

   public static final RenderType ZOL_GLOW = create(
      "zol_glow",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      32768,
      false,
      false,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
         .setTextureState(new TextureStateShard(TEX_GLOW, true, false))
         .setTransparencyState(ADD)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );

   public static final RenderType ZOL_BEAM = create(
      "zol_beam",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      8192,
      false,
      false,
      CompositeState.builder()
         .setShaderState(safeShader(() -> ZoltraakCinematicShaders.zoltraak))
         .setTextureState(new TextureStateShard(TEX_GLOW, true, false))
         .setTransparencyState(ADD)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );

   public static final RenderType BLACK_CIRCLE = create(
      "black_zol_circle",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      8192,
      false,
      false,
      CompositeState.builder()
         .setShaderState(safeShader(() -> ZoltraakCinematicShaders.blackMagicCircle))
         .setTextureState(new TextureStateShard(TEX_CIRCLE, true, false))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );

   public static final RenderType BLACK_GLOW = create(
      "black_zol_glow",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      32768,
      false,
      false,
      CompositeState.builder()
         .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
         .setTextureState(new TextureStateShard(TEX_GLOW, true, false))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );

   public static final RenderType BLACK_CORE = create(
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

   public static final RenderType GREAT_WHITE = create(
      "great_zol_white",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      8192,
      false,
      false,
      CompositeState.builder()
         .setShaderState(safeShader(() -> ZoltraakCinematicShaders.greatWhite))
         .setTextureState(new TextureStateShard(TEX_GLOW, true, false))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );

   public static final RenderType GREAT_PLUME = create(
      "great_zol_plume",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      8192,
      false,
      false,
      CompositeState.builder()
         .setShaderState(safeShader(() -> ZoltraakCinematicShaders.greatPlume))
         .setTextureState(new TextureStateShard(TEX_GLOW, true, false))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );

   public static final RenderType GREAT_BLACK = create(
      "great_zol_black",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      8192,
      false,
      false,
      CompositeState.builder()
         .setShaderState(safeShader(() -> ZoltraakCinematicShaders.greatBlack))
         .setTextureState(new TextureStateShard(TEX_GLOW, true, false))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );

   public static final RenderType BLACK_BEAM = create(
      "black_zol_beam",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      8192,
      false,
      false,
      CompositeState.builder()
         .setShaderState(safeShader(() -> ZoltraakCinematicShaders.blackZoltraak))
         .setTextureState(new TextureStateShard(TEX_GLOW, true, false))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );

   public static final RenderType BLACK_HEAD = create(
      "black_zol_head",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      8192,
      false,
      false,
      CompositeState.builder()
         .setShaderState(safeShader(() -> ZoltraakCinematicShaders.blackZoltraakHead))
         .setTextureState(new TextureStateShard(TEX_GLOW, true, false))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setDepthTestState(LEQUAL_DEPTH_TEST)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false)
   );

   public static final RenderType BARRAGE_CIRCLE = ZOL_CIRCLE;

   public static final RenderType BARRAGE_ATLAS = magicAdditive(TEX_ATLAS);

   public static RenderType magicAdditive(ResourceLocation texture) {
      return create(
         "zol_magic_additive",
         DefaultVertexFormat.POSITION_TEX_COLOR,
         Mode.QUADS,
         4096,
         false,
         true,
         CompositeState.builder()
            .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(ADD)
            .setCullState(NO_CULL)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false)
      );
   }

   public static RenderType magicTranslucent(ResourceLocation texture) {
      return create(
         "zol_magic_translucent",
         DefaultVertexFormat.POSITION_TEX_COLOR,
         Mode.QUADS,
         4096,
         false,
         true,
         CompositeState.builder()
            .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false)
      );
   }

   public static RenderType unsealingCircle(ResourceLocation texture) {
      return create(
         "zol_unsealing_circle",
         DefaultVertexFormat.POSITION_TEX_COLOR,
         Mode.QUADS,
         4096,
         false,
         true,
         CompositeState.builder()
            .setShaderState(safeShader(() -> ZoltraakCinematicShaders.unsealingMagicCircle))
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(ADD)
            .setCullState(NO_CULL)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false)
      );
   }

   public static RenderType unsealingFireBeam(ResourceLocation texture) {
      return create(
         "zol_unsealing_fire_beam",
         DefaultVertexFormat.POSITION_TEX_COLOR,
         Mode.QUADS,
         4096,
         false,
         true,
         CompositeState.builder()
            .setShaderState(safeShader(() -> ZoltraakCinematicShaders.unsealingFireColumn))
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(ADD)
            .setCullState(NO_CULL)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false)
      );
   }

   private ZoltraakRenderTypes(String n, VertexFormat f, Mode m, int s, boolean c, boolean o, Runnable a, Runnable b) {
      super(n, f, m, s, c, o, a, b);
   }
}
