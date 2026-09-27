package com.frierenvoid.client;

import com.frierenvoid.DefenseEntity;
import com.frierenvoid.FrierenVoid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class DefenseRenderer extends EntityRenderer<DefenseEntity> {
   public DefenseRenderer(Context c) {
      super(c);
      this.shadowRadius = 0.0F;
   }

   public ResourceLocation getTextureLocation(DefenseEntity e) {
      return FrierenVoid.id("textures/gui/spell_icons/defense.png");
   }

   public void render(DefenseEntity e, float yaw, float partial, PoseStack stack, MultiBufferSource buffers, int light) {
      float age = e.age(partial);
      float fade = e.fade(partial);
      float life = Mth.clamp(e.capacity() / Math.max(1.0F, e.maximum()), 0.0F, 1.0F);
      if (!(fade <= 0.0F)) {
         DefenseEntity.VisualPose pose = e.visualPose(partial);
         Vec3 u = pose.right();
         Vec3 v = pose.up();
         Vec3 normal = pose.normal();
         Vec3 hit = e.visualHit(pose);
         Vec3 offset = pose.center()
            .subtract(new Vec3(Mth.lerp(partial, e.xOld, e.getX()), Mth.lerp(partial, e.yOld, e.getY()), Mth.lerp(partial, e.zOld, e.getZ())));
         stack.pushPose();
         stack.translate(offset.x, offset.y, offset.z);
         Matrix4f m = stack.last().pose();
         VertexConsumer surface = buffers.getBuffer(VoidRenderTypes.DEFENSE_SURFACE);
         int index = 0;

         for (Vec3 cell : DefenseEntity.cells()) {
            float grow = Mth.clamp((age - (float)cell.length() * 1.2F) / 4.0F, 0.0F, 1.0F);
            double size = 0.62 * grow;
            Vec3 center = u.scale(cell.x).add(v.scale(cell.y));
            if (life == 0.0F) {
               center = center.add(u.scale(cell.x * (1.0F - fade) * 1.5))
                  .add(v.scale(cell.y * (1.0F - fade) * 1.5))
                  .add(normal.scale(Math.sin(index * 2.4) * (1.0F - fade) * 2.0));
            }

            double glow = Math.exp(-e.hitAge(partial) * 0.35) * Math.exp(-center.distanceToSqr(hit) * 0.55);
            float alpha = (float)(0.045 + 0.21 * glow) * fade * grow;

            for (int k = 0; k < 6; k++) {
               tri(surface, m, center, center.add(radial(u, v, k, size)), center.add(radial(u, v, k + 1, size)), 0.35F, 0.8F, 0.98F, alpha);
            }

            index++;
         }

         flush(buffers, VoidRenderTypes.DEFENSE_SURFACE);
         VertexConsumer lines = buffers.getBuffer(VoidRenderTypes.LIGHT);
         index = 0;

         for (Vec3 cell : DefenseEntity.cells()) {
            float grow = Mth.clamp((age - (float)cell.length() * 1.2F) / 4.0F, 0.0F, 1.0F);
            double size = 0.62 * grow;
            Vec3 center = u.scale(cell.x).add(v.scale(cell.y));
            if (life == 0.0F) {
               center = center.add(u.scale(cell.x * (1.0F - fade) * 1.5))
                  .add(v.scale(cell.y * (1.0F - fade) * 1.5))
                  .add(normal.scale(Math.sin(index * 2.4) * (1.0F - fade) * 2.0));
            }

            float flash = (float)(Math.exp(-e.hitAge(partial) * 0.28) * Math.exp(-center.distanceToSqr(hit) * 0.4));
            float power = fade * grow * (0.7F + 0.3F * flash);

            for (int k = 0; k < 6; k++) {
               Vec3 a = center.add(radial(u, v, k, size));
               Vec3 b = center.add(radial(u, v, k + 1, size));
               line(lines, m, a, b, normal, 0.065, power * 0.075F, 0.3F, 0.8F, 1.0F);
               line(lines, m, a, b, normal, 0.025, power * 0.23F, 0.5F, 0.93F, 1.0F);
               line(lines, m, a, b, normal, 0.008, power, 0.78F, 0.98F, 1.0F);
            }

            if (life < 0.4 && index % 3 == 0) {
               Vec3 a = center.add(u.scale(-size * 0.5));
               Vec3 b = center.add(v.scale(size * 0.17));
               Vec3 c = center.add(u.scale(size * 0.3)).add(v.scale(-size * 0.55));
               line(lines, m, a, b, normal, 0.012, power * 0.8F, 0.8F, 0.96F, 1.0F);
               line(lines, m, b, c, normal, 0.009, power * 0.8F, 0.8F, 0.96F, 1.0F);
            }

            index++;
         }

         flush(buffers, VoidRenderTypes.LIGHT);
         float impact = e.hitAge(partial);
         if (impact < 12.0F) {
            VertexConsumer glow = buffers.getBuffer(VoidRenderTypes.ZOL_GLOW);
            double size = 0.4 + impact * 0.1;
            Vec3 at = hit.add(normal.scale(0.02));
            quad(glow, m, at, u.scale(size), v.scale(size), (float)Math.exp(-impact * 0.32) * fade * 0.8F);
            flush(buffers, VoidRenderTypes.ZOL_GLOW);
         }

         stack.popPose();
      }
   }

   private static Vec3 radial(Vec3 u, Vec3 v, int k, double size) {
      double a = (Math.PI / 6) + k * Math.PI / 3.0;
      return u.scale(Math.cos(a) * size).add(v.scale(Math.sin(a) * size));
   }

   private static void line(VertexConsumer b, Matrix4f m, Vec3 a, Vec3 c, Vec3 normal, double width, float alpha, float r, float g, float blue) {
      Vec3 side = c.subtract(a).cross(normal).normalize().scale(width);
      tri(b, m, a.subtract(side), a.add(side), c.add(side), r, g, blue, alpha);
      tri(b, m, a.subtract(side), c.add(side), c.subtract(side), r, g, blue, alpha);
   }

   private static void tri(VertexConsumer b, Matrix4f m, Vec3 a, Vec3 c, Vec3 d, float r, float g, float blue, float alpha) {
      for (Vec3 p : new Vec3[]{a, c, d}) {
         b.addVertex(m, (float)p.x, (float)p.y, (float)p.z).setColor(r, g, blue, alpha);
      }
   }

   private static void quad(VertexConsumer b, Matrix4f m, Vec3 c, Vec3 u, Vec3 v, float alpha) {
      Vec3[] p = new Vec3[]{c.subtract(u).subtract(v), c.add(u).subtract(v), c.add(u).add(v), c.subtract(u).add(v)};
      float[][] uv = new float[][]{{0.0F, 1.0F}, {1.0F, 1.0F}, {1.0F, 0.0F}, {0.0F, 0.0F}};

      for (int i = 0; i < 4; i++) {
         b.addVertex(m, (float)p[i].x, (float)p[i].y, (float)p[i].z).setUv(uv[i][0], uv[i][1]).setColor(0.65F, 0.95F, 1.0F, alpha);
      }
   }

   private static void flush(MultiBufferSource b, RenderType t) {
      if (b instanceof BufferSource s) {
         s.endBatch(t);
      }
   }
}
