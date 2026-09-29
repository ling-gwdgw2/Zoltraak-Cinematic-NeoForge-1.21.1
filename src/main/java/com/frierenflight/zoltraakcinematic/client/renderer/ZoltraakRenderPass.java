package com.frierenflight.zoltraakcinematic.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

public final class ZoltraakRenderPass {
   private static final Map<Integer, ZoltraakRenderPass.Draw> DRAWS = new LinkedHashMap<>();
   private static final BufferSource BUFFERS = MultiBufferSource.immediate(new ByteBufferBuilder(262144));
   private static boolean replaying;

   public static void begin() {
      DRAWS.clear();
      QualSealingStoneRenderer.clearDeferred();
   }

   @SuppressWarnings("unchecked")
   public static <T extends Entity & IZoltraakVisualEntity> boolean defer(
      EntityRenderer<T> renderer, T entity, float yaw, float partial, PoseStack pose, int light
   ) {
      if (replaying || !ShaderCompatibility.useFullDetailPass()) {
         return false;
      }

      if (ShaderCompatibility.isShadowPass()) {
         return true;
      }

      PoseStack copy = new PoseStack();
      copy.last().pose().set(pose.last().pose());
      copy.last().normal().set(pose.last().normal());
      DRAWS.put(
         entity.getId(),
         new ZoltraakRenderPass.Draw(
            (EntityRenderer)renderer, entity, yaw, partial, copy, new Matrix4f(RenderSystem.getModelViewMatrix()), new Matrix4f(RenderSystem.getProjectionMatrix()), light
         )
      );
      return true;
   }

   public static void finish() {
      boolean hasBarrageDeferred = ZoltraakBarrageArrayRenderer.hasDeferredDraw();
      boolean hasSealingDeferred = QualSealingStoneRenderer.hasDeferredDraw();
      if (!DRAWS.isEmpty() || hasBarrageDeferred || hasSealingDeferred) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level == null) {
            DRAWS.clear();
            ZoltraakBarrageArrayRenderer.clearDeferred();
            QualSealingStoneRenderer.clearDeferred();
         } else {
            ShaderInstance previousShader = RenderSystem.getShader();
            float[] color = (float[])RenderSystem.getShaderColor().clone();
            RenderSystem.backupProjectionMatrix();
            Matrix4fStack modelView = RenderSystem.getModelViewStack();
            modelView.pushMatrix();

            try {
               replaying = true;
               mc.getMainRenderTarget().bindWrite(false);
               RenderSystem.enableDepthTest();
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

               for (ZoltraakRenderPass.Draw draw : DRAWS.values()) {
                  if (!draw.entity.isRemoved()) {
                     modelView.set(draw.modelView);
                     RenderSystem.applyModelViewMatrix();
                     RenderSystem.setProjectionMatrix(draw.projection, VertexSorting.DISTANCE_TO_ORIGIN);
                     draw.renderer.render(draw.entity, draw.yaw, draw.partial, draw.pose, BUFFERS, draw.light);
                  }
               }

               if (hasBarrageDeferred) {
                  ZoltraakBarrageArrayRenderer.renderDeferred(BUFFERS, modelView);
               }

               if (hasSealingDeferred) {
                  QualSealingStoneRenderer.renderDeferred(BUFFERS, modelView);
               }

               BUFFERS.endBatch();
            } finally {
               replaying = false;
               DRAWS.clear();
               ZoltraakBarrageArrayRenderer.clearDeferred();
               QualSealingStoneRenderer.clearDeferred();
               modelView.popMatrix();
               RenderSystem.applyModelViewMatrix();
               RenderSystem.restoreProjectionMatrix();
               RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
               RenderSystem.setShader(() -> previousShader);
               RenderSystem.depthMask(true);
               RenderSystem.enableCull();
               RenderSystem.disableBlend();
               RenderSystem.defaultBlendFunc();
            }
         }
      }
   }

   private ZoltraakRenderPass() {}

   private record Draw(
      EntityRenderer renderer, Entity entity, float yaw, float partial, PoseStack pose, Matrix4f modelView, Matrix4f projection, int light
   ) {}
}
