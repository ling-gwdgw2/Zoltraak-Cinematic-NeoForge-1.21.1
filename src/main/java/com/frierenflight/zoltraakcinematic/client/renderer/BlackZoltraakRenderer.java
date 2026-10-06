package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BlackZoltraakRenderer<T extends Entity & IZoltraakVisualEntity> extends EntityRenderer<T> {
   public BlackZoltraakRenderer(Context c) {
      super(c);
      this.shadowRadius = 0.0F;
   }

   public ResourceLocation getTextureLocation(T e) {
      return ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "textures/spell/zoltraak_magic_circle.png");
   }

   private static float ease(float v) {
      v = Mth.clamp(v, 0.0F, 1.0F);
      return v * v * (3.0F - 2.0F * v);
   }

   private static Vec3 ring(Vec3 u, Vec3 v, double angle, double radius) {
      return u.scale(Math.cos(angle) * radius).add(v.scale(Math.sin(angle) * radius));
   }

   public void render(T e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
      float t = e.age(partial);
      float shot = t - 14.0F;
      Minecraft mc = Minecraft.getInstance();
      boolean first = mc.player != null && e.casterId() == mc.player.getId() && mc.getCameraEntity() == mc.player && mc.options.getCameraType().isFirstPerson();
      Vec3 origin = e.visualOrigin(partial);
      Vec3 dir = e.visualDirection(partial);
      Vec3 u = dir.cross(new Vec3(0.0, 1.0, 0.0));
      if (u.lengthSqr() < 0.01) {
         u = new Vec3(1.0, 0.0, 0.0);
      } else {
         u = u.normalize();
      }

      Vec3 v = u.cross(dir).normalize();
      Vec3 offset = origin.subtract(e.getPosition(partial));
      pose.pushPose();
      pose.translate(offset.x, offset.y, offset.z);
      Matrix4f mat = pose.last().pose();
      Vec3 camera = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
      Vec3 side = dir.cross(camera).normalize();
      if (side.lengthSqr() < 0.01) {
         side = u;
      }

      Vec3 camRight = new Vec3(new Vector3f(1.0F, 0.0F, 0.0F).rotate(this.entityRenderDispatcher.cameraOrientation()));
      Vec3 camUp = new Vec3(new Vector3f(0.0F, 1.0F, 0.0F).rotate(this.entityRenderDispatcher.cameraOrientation()));
      float circleAlpha = ease(t / 4.0F) * (1.0F - ease((shot - 8.0F) / 12.0F));
      if (e.mode() == ZoltraakMode.RAPID) {
         circleAlpha = 0.72F * ease(t / 4.0F) * (1.0F - ease((shot - 2.0F) / 8.0F));
      }

      double radius = (e.mode() == ZoltraakMode.LARGE ? 6.2 : (e.mode() == ZoltraakMode.RAPID ? 1.15 : 1.85 * e.scale())) * (0.2 + 0.8 * ease(t / 11.0F));
      if (first && e.mode() == ZoltraakMode.SINGLE) {
         radius *= 0.75;
      }

      if (first && e.mode() == ZoltraakMode.RAPID) {
         radius *= 0.6;
      }

      if (circleAlpha > 0.001) {
         VertexConsumer b = buffers.getBuffer(ZoltraakRenderTypes.BLACK_CIRCLE);
         double rotation = 0.016 * Math.sin(t * 0.08);
         Vec3 ru = ring(u, v, rotation, 1.0);
         Vec3 rv = ring(u, v, rotation + (Math.PI / 2), 1.0);
         quad(b, mat, dir.scale(first && e.mode() == ZoltraakMode.SINGLE ? 0.7 : 0.03), ru.scale(radius), rv.scale(radius), 1.0F, 1.0F, 1.0F, circleAlpha);
         flush(buffers, ZoltraakRenderTypes.BLACK_CIRCLE);
      }

      VertexConsumer glow = buffers.getBuffer(ZoltraakRenderTypes.BLACK_GLOW);
      float charge = ease(t / 14.0F);
      float muzzle = shot < 0.0F ? charge : (float)Math.exp(-Math.max(0.0F, shot) * 0.24);
      if (muzzle > 0.003) {
         double size = (0.17 + charge * 0.35 + (shot >= 0.0F ? 0.6 * Math.exp(-shot * 0.22) : 0.0)) * (first ? 0.72 : 1.0) * e.scale();
         if (first) {
            muzzle *= 0.75F;
         }

         quad(glow, mat, Vec3.ZERO, camRight.scale(size * 2.5), camUp.scale(size * 2.5), 0.008F, 0.006F, 0.014F, muzzle * 0.55F);
         quad(glow, mat, dir.scale(0.045), camRight.scale(size), camUp.scale(size), 0.008F, 0.006F, 0.014F, muzzle);
         quad(glow, mat, Vec3.ZERO, camRight.scale(size * 4.0), camUp.scale(size * 0.12), 0.008F, 0.006F, 0.014F, muzzle);
         quad(glow, mat, Vec3.ZERO, camRight.scale(size * 0.1), camUp.scale(size * 2.0), 0.008F, 0.006F, 0.014F, muzzle * 0.7F);
      }

      flush(buffers, ZoltraakRenderTypes.BLACK_GLOW);
      VertexConsumer lines = buffers.getBuffer(ZoltraakRenderTypes.LIGHT);
      if (shot < 0.0F) {
         for (int k = 0; k < 5; k++) {
            for (int j = 0; j < 18; j++) {
               double p = j / 18.0;
               double q = (j + 1) / 18.0;
               double a = k * Math.PI * 2.0 / 5.0 - t * 0.1;
               double collapse = 0.75 * e.scale() * (1.0 - charge * 0.5);
               Vec3 pa = ring(u, v, a + p * 1.4, collapse * (1.0 - p) + 0.05);
               Vec3 pb = ring(u, v, a + q * 1.4, collapse * (1.0 - q) + 0.05);
               ribbon(lines, mat, pa, pb, ring(u, v, a + p * 1.4, 1.0), (float)(0.025 * Math.sin(p * Math.PI)), charge * 0.65F, 0.28F, 0.15F, 0.38F);
            }
         }

         for (int k = 0; k < 26; k++) {
            double p = (t * 0.065 + k * 0.618) % 1.0;
            double a = k * 2.399;
            double r = ((1.0 - p) * 1.8 + 0.08) * e.scale();
            Vec3 head = ring(u, v, a, r).add(dir.scale(-0.35 * (1.0 - p)));
            Vec3 tail = ring(u, v, a, r + 0.08 + 0.25 * p).add(dir.scale(-0.4 * (1.0 - p)));
            ribbon(lines, mat, tail, head, side, 0.012F, (float)(Math.sin(p * Math.PI) * charge * 0.65), 0.28F, 0.15F, 0.38F);
         }
      }

      if (e.fired() && shot >= 0.0F) {
         double head = Math.min(e.length(), shot * 12.0);
         double tail = Math.max(0.0, (shot - 9.0F) * 12.0);
         float energy = 1.0F - ease((shot - 7.0F) / 11.0F);
         if (head > tail && energy > 0.0F) {
            Vec3 a = dir.scale(tail);
            Vec3 b = dir.scale(head);
            flush(buffers, ZoltraakRenderTypes.LIGHT);
            double halfWidth = (first ? 1.05 : 1.45) * e.scale();
            VertexConsumer beam = buffers.getBuffer(ZoltraakRenderTypes.BLACK_BEAM);
            beamQuad(
               beam,
               mat,
               a.subtract(dir.scale(halfWidth)),
               b.add(dir.scale(halfWidth)),
               side.scale(halfWidth),
               t / 64.0F,
               energy,
               (float)(head - tail),
               (float)halfWidth
            );
            flush(buffers, ZoltraakRenderTypes.BLACK_BEAM);
            lines = buffers.getBuffer(ZoltraakRenderTypes.LIGHT);

            for (int k = 0; k < 9; k++) {
               double z = (shot * 7.0F + k * 6.37) % Math.max(1.0, head);
               double r = (0.35 + 0.12 * Math.sin(t * 0.7 + k)) * e.scale();
               if (!(z < tail)) {
                  Vec3 radial = ring(u, v, k * 2.4, r);
                  ribbon(
                     lines, mat, dir.scale(Math.max(tail, z - 2.2)).add(radial), dir.scale(z).add(radial), side, 0.008F, energy * 0.65F, 0.28F, 0.15F, 0.38F
                  );
               }
            }

            for (int k = 0; k < 3; k++) {
               double z = shot * 3.2 - k * 6;
               if (!(z < 0.0) && !(z > head)) {
                  double r = (0.35 + Math.max(0.0F, shot - k * 2) * 0.07) * e.scale();
                  arc(lines, mat, dir.scale(z), u, v, r, 0.015F, energy * 0.35F, 0.28F, 0.15F, 0.38F, Math.PI * 2);
               }
            }
         }

         if (shot > 7.0F && shot < 25.0F) {
            float a = (1.0F - ease((shot - 7.0F) / 18.0F)) * 0.32F;
            ribbon(lines, mat, dir.scale(0.2), dir.scale(e.length()), side, 0.018F * e.scale(), a, 0.28F, 0.15F, 0.38F);
         }
      }

      float dt = t - e.impactAge();
      if (e.fired() && e.impact() && dt >= 0.0F && dt < 26.0F) {
         Vec3 center = e.end().subtract(origin).add(e.normal().scale(0.09));
         float fade = 1.0F - ease(dt / 26.0F);
         Vec3 normal = e.normal();
         Vec3 iu = normal.cross(new Vec3(0.0, 1.0, 0.0));
         if (iu.lengthSqr() < 0.01) {
            iu = u;
         } else {
            iu = iu.normalize();
         }

         Vec3 iv = iu.cross(normal).normalize();


         for (int k = 0; k < 48; k++) {
            double a = k * 2.399;
            double r = (0.2 + dt * (0.15 + k % 7 * 0.033)) * e.scale();
            Vec3 ray = ring(iu, iv, a, 1.0).add(normal.scale(0.2 + k % 5 * 0.15)).normalize();
            Vec3 tip = center.add(ray.scale(r)).add(0.0, -0.006 * dt * dt, 0.0);
            ribbon(lines, mat, tip.subtract(ray.scale(0.15 + 0.45 * fade)), tip, camRight, 0.013F * e.scale(), fade * 0.8F, 0.28F, 0.15F, 0.38F);
         }

         flush(buffers, ZoltraakRenderTypes.LIGHT);
         glow = buffers.getBuffer(ZoltraakRenderTypes.BLACK_GLOW);
         float burst = (float)Math.exp(-dt * 0.27);
         quad(glow, mat, center, iu.scale((3.0 + dt * 0.18) * e.scale()), iv.scale((3.0 + dt * 0.18) * e.scale()), 0.008F, 0.006F, 0.014F, burst * 0.7F);
         quad(glow, mat, center, iu.scale((1.2 + dt * 0.05) * e.scale()), iv.scale((1.2 + dt * 0.05) * e.scale()), 0.008F, 0.006F, 0.014F, burst);
         quad(glow, mat, center, iu.scale((5.0 + dt * 0.2) * e.scale()), iv.scale(0.13 * e.scale()), 0.008F, 0.006F, 0.014F, burst);
         quad(glow, mat, center, iu.scale(0.08 * e.scale()), iv.scale(3.0 * e.scale()), 0.008F, 0.006F, 0.014F, burst);
         flush(buffers, ZoltraakRenderTypes.BLACK_GLOW);

         float progress = Mth.clamp(dt / 26.0F, 0.0F, 1.0F);
         VertexConsumer impact = buffers.getBuffer(ZoltraakRenderTypes.ZOL_IMPACT);
         double sparkSize   = (3.0 + dt * 0.28) * 1.2 * e.scale();
         Vec3 sphereCenter = center.add(normal.scale(sparkSize * 0.35));
         quad(impact, mat, sphereCenter, camRight.scale(sparkSize), camUp.scale(sparkSize), progress, 1.0F, 0.0F, fade);
         flush(buffers, ZoltraakRenderTypes.ZOL_IMPACT);
      } else {
         flush(buffers, ZoltraakRenderTypes.LIGHT);
      }

      pose.popPose();
   }

   private static void arc(
      VertexConsumer b, Matrix4f m, Vec3 center, Vec3 u, Vec3 v, double r, float width, float alpha, float red, float green, float blue, double sweep
   ) {
      for (int i = 0; i < 96; i++) {
         double a = i * sweep / 96.0;
         double c = (i + 1) * sweep / 96.0;
         Vec3 p = center.add(ring(u, v, a, r));
         Vec3 q = center.add(ring(u, v, c, r));
         ribbon(b, m, p, q, ring(u, v, (a + c) / 2.0, 1.0), width, alpha, red, green, blue);
      }
   }

   private static void ribbon(VertexConsumer b, Matrix4f m, Vec3 a, Vec3 c, Vec3 side, float width, float alpha, float r, float g, float blue) {
      Vec3 w = side.scale(width);
      tri(b, m, a.subtract(w), a.add(w), c.add(w), r, g, blue, alpha);
      tri(b, m, a.subtract(w), c.add(w), c.subtract(w), r, g, blue, alpha);
   }

   private static void tri(VertexConsumer b, Matrix4f m, Vec3 a, Vec3 c, Vec3 d, float r, float g, float blue, float alpha) {
      for (Vec3 p : new Vec3[]{a, c, d}) {
         b.addVertex(m, (float)p.x, (float)p.y, (float)p.z).setColor(r, g, blue, alpha);
      }
   }

   private static void quad(VertexConsumer b, Matrix4f m, Vec3 c, Vec3 u, Vec3 v, float r, float g, float blue, float a) {
      tex(b, m, c.subtract(u).subtract(v), 0.0F, 1.0F, r, g, blue, a);
      tex(b, m, c.add(u).subtract(v), 1.0F, 1.0F, r, g, blue, a);
      tex(b, m, c.add(u).add(v), 1.0F, 0.0F, r, g, blue, a);
      tex(b, m, c.subtract(u).add(v), 0.0F, 0.0F, r, g, blue, a);
   }

   private static void beamQuad(VertexConsumer b, Matrix4f m, Vec3 a, Vec3 c, Vec3 w, float age, float alpha, float length, float halfWidth) {
      float extent = halfWidth / Math.max(length, 0.001F);
      tex(b, m, a.subtract(w), -extent, 0.0F, age, length / 128.0F, halfWidth / 16.0F, alpha);
      tex(b, m, c.subtract(w), 1.0F + extent, 0.0F, age, length / 128.0F, halfWidth / 16.0F, alpha);
      tex(b, m, c.add(w), 1.0F + extent, 1.0F, age, length / 128.0F, halfWidth / 16.0F, alpha);
      tex(b, m, a.add(w), -extent, 1.0F, age, length / 128.0F, halfWidth / 16.0F, alpha);
   }

   private static void tex(VertexConsumer b, Matrix4f m, Vec3 p, float u, float v, float r, float g, float blue, float a) {
      b.addVertex(m, (float)p.x, (float)p.y, (float)p.z).setUv(u, v).setColor(r, g, blue, a);
   }

   private static void flush(MultiBufferSource b, RenderType t) {
      if (b instanceof BufferSource s) {
         s.endBatch(t);
      }
   }
}
