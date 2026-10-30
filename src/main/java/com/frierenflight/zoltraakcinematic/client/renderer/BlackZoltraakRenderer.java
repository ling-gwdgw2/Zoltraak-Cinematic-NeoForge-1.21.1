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

      float formation = Mth.clamp(t / 4.5F, 0.0F, 1.0F);
      Vec3 primaryCenter = dir.scale(first && e.mode() == ZoltraakMode.SINGLE ? 0.7 : 0.03);
      if (circleAlpha > 0.001) {
         VertexConsumer b = buffers.getBuffer(ZoltraakRenderTypes.BLACK_CIRCLE);
         quad(b, mat, primaryCenter, u.scale(radius), v.scale(radius), formation, 1.0F, 1.0F, circleAlpha);
         flush(buffers, ZoltraakRenderTypes.BLACK_CIRCLE);
      }

      VertexConsumer glow = buffers.getBuffer(ZoltraakRenderTypes.BLACK_GLOW);
      float charge = ease(t / 14.0F);
      float energy = e.fired() && shot >= 0.0F ? (1.0F - ease((shot - 7.0F) / 11.0F)) : 0.0F;
      float muzzle = shot < 0.0F ? charge : Math.max((float)Math.exp(-Math.max(0.0F, shot) * 0.24), energy * 0.85F);
      if (muzzle > 0.003) {
         double size = (0.17 + charge * 0.35 + (shot >= 0.0F ? 0.6 * Math.exp(-shot * 0.22) : 0.0)) * (first ? 0.72 : 1.0) * e.scale();
         if (first) {
            muzzle *= 0.75F;
         }

         if (shot >= 0.0F) {
            quad(glow, mat, primaryCenter, camRight.scale(size * 2.5), camUp.scale(size * 2.5), 0.008F, 0.006F, 0.014F, muzzle * 0.55F);
            quad(glow, mat, primaryCenter.add(dir.scale(0.045)), camRight.scale(size), camUp.scale(size), 0.008F, 0.006F, 0.014F, muzzle);
            quad(glow, mat, primaryCenter, camRight.scale(size * 4.0), camUp.scale(size * 0.12), 0.008F, 0.006F, 0.014F, muzzle);
            quad(glow, mat, primaryCenter, camRight.scale(size * 0.1), camUp.scale(size * 2.0), 0.008F, 0.006F, 0.014F, muzzle * 0.7F);
         } else {
            quad(glow, mat, primaryCenter.add(dir.scale(0.02)), camRight.scale(size * 0.4), camUp.scale(size * 0.4), 0.25F, 0.12F, 0.35F, muzzle * 0.35F);
         }
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
         double head = Math.min(e.length(), shot * 24.0);
         double tail = 0.0;
         if (head > tail && energy > 0.0F) {
            Vec3 a = primaryCenter;
            Vec3 b = a.add(dir.scale(head));
            flush(buffers, ZoltraakRenderTypes.LIGHT);
            double halfWidth = (first ? 1.05 : 1.45) * e.scale();
            VertexConsumer beam = buffers.getBuffer(ZoltraakRenderTypes.BLACK_BEAM);
            beamQuad(
               beam,
               mat,
               a.subtract(dir.scale(halfWidth * 0.2)),
               b.add(dir.scale(halfWidth * 0.2)),
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
               Vec3 radial = ring(u, v, k * 2.4, r);
               ribbon(
                  lines, mat, a.add(dir.scale(Math.max(0.0, z - 2.2))).add(radial), a.add(dir.scale(z)).add(radial), side, 0.008F, energy * 0.65F, 0.28F, 0.15F, 0.38F
               );
            }

            for (int k = 0; k < 3; k++) {
               double z = shot * 3.2 - k * 6;
               if (!(z < 0.0) && !(z > head)) {
                  double r = (0.35 + Math.max(0.0F, shot - k * 2) * 0.07) * e.scale();
                  arc(lines, mat, a.add(dir.scale(z)), u, v, r, 0.015F, energy * 0.35F, 0.28F, 0.15F, 0.38F, Math.PI * 2);
               }
            }
         }

         if (shot > 5.0F && shot < 18.0F) {
            float aRibbon = (1.0F - ease((shot - 5.0F) / 13.0F)) * 0.22F * energy;
            ribbon(lines, mat, primaryCenter.add(dir.scale(0.2)), primaryCenter.add(dir.scale(head)), side, 0.018F * e.scale(), aRibbon, 0.28F, 0.15F, 0.38F);
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

         // 1. 3D Expanding Hemispherical Shockwave Dome (Abyssal Void Shockwave)
         double domeRadius = (1.5 + dt * 0.36) * 1.2 * e.scale();
         float domeAlpha = fade * (float)Math.exp(-dt * 0.10) * 0.55F;
         dome(lines, mat, center, iu, iv, normal, domeRadius, domeAlpha, 0.38F, 0.18F, 0.55F);

         // 2. 3D Kinetic Electric Arc & Spark Fountain
         for (int k = 0; k < 56; k++) {
            double a = k * 2.399;
            double elevation = 0.20 + (k % 7) * 0.10;
            double r = (0.3 + dt * (0.22 + (k % 5) * 0.040)) * e.scale();
            Vec3 ray = ring(iu, iv, a, Math.cos(elevation)).add(normal.scale(Math.sin(elevation) + 0.30)).normalize();
            Vec3 tip = center.add(ray.scale(r)).add(0.0, -0.007 * dt * dt, 0.0);
            double sparkLen = (0.25 + 0.45 * fade) * e.scale();
            Vec3 tail = tip.subtract(ray.scale(sparkLen));
            float sparkWidth = 0.020F * (float)e.scale();
            ribbon(lines, mat, tail, tip, camRight, sparkWidth, fade * 0.85F, 0.38F, 0.18F, 0.55F);
            ribbon(lines, mat, tail, tip, camRight, sparkWidth * 0.45F, fade * 0.95F, 0.88F, 0.72F, 0.98F);
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
         VertexConsumer impact = buffers.getBuffer(ZoltraakRenderTypes.BLACK_IMPACT);

         // 3. Multi-Lobe Volumetric Rising Anime Puffs (Black Soot & Void Clouds)
         Vec3 corePos = center.add(normal.scale(0.35 + dt * 0.15));
         double coreSize = (2.2 + dt * 0.20) * 1.1 * e.scale();
         quad(impact, mat, corePos, camRight.scale(coreSize), camUp.scale(coreSize), progress, 1.0F, 0.0F, fade);

         for (int k = 0; k < 6; k++) {
            double angle = k * (Math.PI * 2.0 / 6.0) + t * 0.06;
            double puffDist = (0.7 + dt * 0.16) * 1.0 * e.scale();
            double puffHeight = (0.25 + dt * 0.12 + Math.sin(k * 1.5) * 0.15) * e.scale();
            Vec3 puffPos = center.add(ring(iu, iv, angle, puffDist)).add(normal.scale(puffHeight));
            double puffSize = (1.5 + dt * 0.16) * 1.0 * e.scale();
            float puffProgress = Mth.clamp((dt + k * 0.7F) / 26.0F, 0.0F, 1.0F);
            quad(impact, mat, puffPos, camRight.scale(puffSize), camUp.scale(puffSize), puffProgress, 1.0F, 0.0F, fade * 0.75F);
         }

         // 4. Localized Surface Planar Shockwave & Puncture Crater (Mode 1: isSurface = 1.0F)
         double surfRadius = (1.4 + dt * 0.10) * 1.0 * e.scale();
         Vec3 surfCenter = center.add(normal.scale(0.04));
         quad(impact, mat, surfCenter, iu.scale(surfRadius), iv.scale(surfRadius), progress, 1.0F, 1.0F, fade * 0.85F);
         flush(buffers, ZoltraakRenderTypes.BLACK_IMPACT);
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

   private static void dome(
      VertexConsumer b, Matrix4f m, Vec3 center, Vec3 iu, Vec3 iv, Vec3 normal, double radius, float alpha, float red, float green, float blue
   ) {
      if (alpha < 0.003F) return;
      int rings = 6;
      int slices = 16;
      for (int ringIdx = 0; ringIdx < rings; ringIdx++) {
         double phi1 = (ringIdx / (double)rings) * (Math.PI * 0.5);
         double phi2 = ((ringIdx + 1) / (double)rings) * (Math.PI * 0.5);
         double cos1 = Math.cos(phi1);
         double sin1 = Math.sin(phi1);
         double cos2 = Math.cos(phi2);
         double sin2 = Math.sin(phi2);

         float a1 = alpha * (float)(1.0 - 0.35 * sin1);
         float a2 = alpha * (float)(1.0 - 0.35 * sin2);

         for (int sliceIdx = 0; sliceIdx < slices; sliceIdx++) {
            double theta1 = sliceIdx * (Math.PI * 2.0 / slices);
            double theta2 = (sliceIdx + 1) * (Math.PI * 2.0 / slices);

            Vec3 p00 = center.add(ring(iu, iv, theta1, radius * cos1)).add(normal.scale(radius * sin1));
            Vec3 p10 = center.add(ring(iu, iv, theta2, radius * cos1)).add(normal.scale(radius * sin1));
            Vec3 p01 = center.add(ring(iu, iv, theta1, radius * cos2)).add(normal.scale(radius * sin2));
            Vec3 p11 = center.add(ring(iu, iv, theta2, radius * cos2)).add(normal.scale(radius * sin2));

            tri(b, m, p00, p10, p11, red, green, blue, (a1 + a2) * 0.5F);
            tri(b, m, p00, p11, p01, red, green, blue, (a1 + a2) * 0.5F);
         }
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
