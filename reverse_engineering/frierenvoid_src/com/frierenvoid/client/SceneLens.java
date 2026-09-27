package com.frierenvoid.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL30;

final class SceneLens {
   private static RenderTarget snapshot;

   static void capture() {
      RenderSystem.assertOnRenderThread();
      RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
      int read = GL30.glGetInteger(36010);
      int draw = GL30.glGetInteger(36006);

      try {
         if (snapshot == null) {
            snapshot = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
         } else if (snapshot.width != main.width || snapshot.height != main.height) {
            snapshot.resize(main.width, main.height, Minecraft.ON_OSX);
         }

         GL30.glBindFramebuffer(36008, main.frameBufferId);
         GL30.glBindFramebuffer(36009, snapshot.frameBufferId);
         GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, snapshot.width, snapshot.height, 16384, 9728);
      } finally {
         GL30.glBindFramebuffer(36008, read);
         GL30.glBindFramebuffer(36009, draw);
      }

      VoidClient.lens.setSampler("SceneSampler", snapshot.getColorTextureId());
      VoidClient.lens.safeGetUniform("Viewport").set(main.width, main.height);
   }

   static void release() {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(SceneLens::release);
      } else {
         if (snapshot != null) {
            snapshot.destroyBuffers();
            snapshot = null;
         }
      }
   }

   private SceneLens() {
   }
}
