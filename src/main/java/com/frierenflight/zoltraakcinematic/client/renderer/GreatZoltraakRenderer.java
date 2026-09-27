package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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

public final class GreatZoltraakRenderer<T extends Entity & IZoltraakVisualEntity> extends EntityRenderer<T> {
   public GreatZoltraakRenderer(Context c) {
      super(c);
      this.shadowRadius = 0.0F;
   }

   public ResourceLocation getTextureLocation(T e) {
      return ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "textures/spell/zoltraak_magic_circle.png");
   }

   private static float ease(double a) {
      float x = Mth.clamp((float)a, 0.0F, 1.0F);
      return x * x * (3.0F - 2.0F * x);
   }

   private static Vec3 radial(Vec3 u, Vec3 v, double a, double r) {
      return u.scale(Math.cos(a) * r).add(v.scale(Math.sin(a) * r));
   }

   public void render(T e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
      float t = e.rawAge(partial);
      float release = t - 20.0F;
      float charge = ease(t / 18.0F);
      float fade = 1.0F - ease((t - 100.0F) / 26.0F);
      if (!(t < 0.0F) && !(fade <= 0.0F)) {
         Vec3 origin = e.visualOrigin(partial);
         Vec3 dir = e.visualDirection(partial);
         Vec3 u = dir.cross(new Vec3(0.0, 1.0, 0.0));
         u = u.lengthSqr() < 0.01 ? new Vec3(1.0, 0.0, 0.0) : u.normalize();
         Vec3 v = u.cross(dir).normalize();
         Vec3 shift = origin.subtract(e.getPosition(partial));
         pose.pushPose();
         pose.translate(shift.x, shift.y, shift.z);
         Matrix4f mat = pose.last().pose();
         Vec3 camera = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
         Vec3 side = dir.cross(camera);
         side = side.lengthSqr() < 0.01 ? u : side.normalize();
         Vec3 cameraRight = new Vec3(new Vector3f(1.0F, 0.0F, 0.0F).rotate(this.entityRenderDispatcher.cameraOrientation()));
         Vec3 cameraUp = new Vec3(new Vector3f(0.0F, 1.0F, 0.0F).rotate(this.entityRenderDispatcher.cameraOrientation()));
         VertexConsumer haze = buffers.getBuffer(ZoltraakRenderTypes.ZOL_GLOW);

         for (int k = 0; k < 14; k++) {
            double phase = (t * 0.009 + k * 0.618) % 1.0;
            double a = k * 2.399;
            Vec3 p = radial(u, v, a, 3.0 + phase * 4.0).add(v.scale(-3.8)).add(dir.scale(-1.0 - phase * 2.0));
            double size = 1.7 + phase * 2.0;
            quad(
               haze, mat, p, cameraRight.scale(size), cameraUp.scale(size * 0.65), 0.72F, 0.83F, 1.0F, charge * fade * (float)Math.sin(phase * Math.PI) * 0.22F
            );
         }

         flush(buffers, ZoltraakRenderTypes.ZOL_GLOW);
         double sealRadius = 9.3 * (0.65 + 0.35 * ease(t / 13.0F));
         boolean behindSeal = camera.dot(dir) < -0.08;
         if (!behindSeal) {
            seal(buffers, mat, dir, u, v, sealRadius, ease(t / 3.0F) * fade);
         }

         double inflate = ease((t - 10.0F) / 10.0F);
         double front = e.fired() ? e.beamFront(partial) : 0.025;
         double tail = release > 80.0F ? (release - 80.0F) * 2.5 : 0.0;
         float bodyFade = 1.0F - ease((release - 80.0F) / 12.0F);
         if (inflate > 0.0 && front > tail && bodyFade > 0.0F) {
            RenderType type = e.black() ? ZoltraakRenderTypes.GREAT_BLACK : ZoltraakRenderTypes.GREAT_WHITE;
            VertexConsumer beam = buffers.getBuffer(type);
            volumeBeam(beam, mat, dir, u, v, camera, tail, Math.max(tail + 0.51, front), 22.5 * inflate, t, bodyFade);
            rimBeam(beam, mat, dir.scale(tail), dir.scale(Math.max(tail + 0.51, front)), side, 22.5 * inflate, t, bodyFade);
            flush(buffers, type);
         }

         if (release >= 0.0F && release < 10.0F) {
            VertexConsumer burst = buffers.getBuffer(ZoltraakRenderTypes.ZOL_GLOW);
            float power = (float)Math.exp(-release * 0.38);
            quad(burst, mat, dir.scale(0.1), u.scale(13.5), v.scale(13.5), 0.86F, 0.96F, 1.0F, power * 0.9F);
            quad(burst, mat, dir.scale(0.12), u.scale(19.5), v.scale(0.36), 1.0F, 1.0F, 1.0F, power);
            flush(buffers, ZoltraakRenderTypes.ZOL_GLOW);
         }

         VertexConsumer lightBuffer = buffers.getBuffer(ZoltraakRenderTypes.LIGHT);
         if (release >= 0.0F && release < 86.0F) {
            for (int k = 0; k < 3; k++) {
               double z = (release * 2.5 - k * 12) % (e.length() + 12.0);
               if (!(z < 0.2) && !(z > front - 0.1) && !(z > e.length())) {
                  float a = (1.0F - ease((release - 76.0F) / 10.0F)) * 0.9F;
                  ring(lightBuffer, mat, dir.scale(z), u, v, 7.125 + 0.255 * Math.sin(t * 0.35 + k), 0.0825F, a, 0.88F, 1.0F, 1.0F);
                  ring(lightBuffer, mat, dir.scale(z + 0.11), u, v, 7.23, 0.024F, a * 0.7F, 1.0F, 1.0F, 1.0F);
               }
            }
         }

         flush(buffers, ZoltraakRenderTypes.LIGHT);
         float impact = t - e.largeImpactTime();
         if (e.fired() && e.impact() && impact >= 0.0F) {
            Vec3 normal = e.normal();
            Vec3 center = e.end().subtract(origin).add(normal.scale(0.18));
            Vec3 iu = normal.cross(new Vec3(0.0, 1.0, 0.0));
            iu = iu.lengthSqr() < 0.01 ? u : iu.normalize();
            Vec3 iv = iu.cross(normal).normalize();
            float spread = ease(impact / 13.0F);
            float impactFade = fade * ease(impact / 2.0F);

            for (int k = 0; k < 10; k++) {
               double a = k * Math.PI * 2.0 / 10.0 + 0.08 * Math.sin(t * 0.13 + k);
               double r = (13.5 + k % 3 * 1.95) * spread;
               Vec3 axis = radial(iu, iv, a, 1.0);
               Vec3 curl = radial(iu, iv, a + 0.85, 1.0);
               Vec3 p = center.subtract(dir.scale(5.0)).add(axis.scale(2.475));
               Vec3 q = p.add(dir.scale(5.0));
               Vec3 r2 = center.add(curl.scale(r * 0.7)).add(normal.scale(2.4 + 0.3 * Math.sin(t * 0.2 + k)));
               Vec3 end = center.add(radial(iu, iv, a + 0.65, r)).add(normal.scale(4.8));
               plume(buffers, mat, p, q, r2, end, camera, t + k * 9, 4.2F, impactFade, e.black(), true);
            }

            lightBuffer = buffers.getBuffer(ZoltraakRenderTypes.LIGHT);

            for (int k = 0; k < 84; k++) {
               double p = (t * 0.027 + k * 0.618) % 1.0;
               double a = k * 2.399;
               double r = (1.5 + p * 13.5) * spread;
               Vec3 at = center.add(radial(iu, iv, a, r)).add(normal.scale(p * 4.0));
               ribbon(
                  lightBuffer, mat, at, at.add(radial(iu, iv, a, 0.225 + p * 1.2)), side, 0.027F, (float)Math.sin(p * Math.PI) * impactFade, 0.35F, 0.83F, 1.0F
               );
            }

            flush(buffers, ZoltraakRenderTypes.LIGHT);
            VertexConsumer glow = buffers.getBuffer(ZoltraakRenderTypes.ZOL_GLOW);
            float flash = (float)Math.exp(-impact * 0.15) * fade;
            quad(glow, mat, center, iu.scale(13.5), iv.scale(13.5), 0.3F, 0.75F, 1.0F, flash * 0.85F);
            quad(glow, mat, center, iu.scale(18.0), iv.scale(0.33), 0.8F, 1.0F, 1.0F, flash);
            flush(buffers, ZoltraakRenderTypes.ZOL_GLOW);
         }

         if (behindSeal) {
            seal(buffers, mat, dir, u, v, sealRadius, ease(t / 3.0F) * fade);
         }

         pose.popPose();
      }
   }

   private static Vec3 bez(Vec3 a, Vec3 b, Vec3 c, Vec3 d, double t) {
      double s = 1.0 - t;
      return a.scale(s * s * s).add(b.scale(3.0 * s * s * t)).add(c.scale(3.0 * s * t * t)).add(d.scale(t * t * t));
   }

   private static void plume(
      MultiBufferSource buffers, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 camera, float t, float width, float alpha, boolean black, boolean attached
   ) {
      if (!(alpha < 0.002)) {
         Vec3[] points = new Vec3[49];

         for (int i = 0; i <= 48; i++) {
            points[i] = bez(a, b, c, d, i / 48.0);
         }

         streamMesh(buffers, m, points, camera, t, width, alpha, black, attached);
      }
   }

   private static void streamMesh(
      MultiBufferSource buffers, Matrix4f m, Vec3[] points, Vec3 camera, float t, float width, float alpha, boolean black, boolean attached
   ) {
      VertexConsumer mesh = buffers.getBuffer(ZoltraakRenderTypes.GREAT_PLUME);
      Vec3[] edges = new Vec3[49];

      for (int i = 0; i <= 48; i++) {
         double p = i / 48.0;
         Vec3 x = points[i];
         Vec3 tangent = points[Math.min(48, i + 1)].subtract(points[Math.max(0, i - 1)]);
         Vec3 edge = tangent.cross(camera.subtract(x));
         edge = edge.lengthSqr() < 1.0E-5 ? new Vec3(1.0, 0.0, 0.0) : edge.normalize();
         double profile = attached ? (0.82 + 0.25 * Math.sin(Math.PI * p)) * (1.0F - ease((p - 0.58) / 0.42)) : 0.025 + Math.pow(Math.sin(Math.PI * p), 0.7);
         double w = width * profile * (0.95 + 0.05 * Math.sin(p * 31.0 - t * 0.22));
         edges[i] = edge.scale(w);
      }

      for (int i = 0; i < 48; i++) {
         float p = i / 48.0F;
         float q = (i + 1) / 48.0F;
         vertex(mesh, m, points[i].subtract(edges[i]), p, 0.0F, t / 255.0F, black ? 1.0F : 0.0F, attached ? 0.5F : (width < 0.3 ? 0.0F : 1.0F), alpha);
         vertex(mesh, m, points[i + 1].subtract(edges[i + 1]), q, 0.0F, t / 255.0F, black ? 1.0F : 0.0F, attached ? 0.5F : (width < 0.3 ? 0.0F : 1.0F), alpha);
         vertex(mesh, m, points[i + 1].add(edges[i + 1]), q, 1.0F, t / 255.0F, black ? 1.0F : 0.0F, attached ? 0.5F : (width < 0.3 ? 0.0F : 1.0F), alpha);
         vertex(mesh, m, points[i].add(edges[i]), p, 1.0F, t / 255.0F, black ? 1.0F : 0.0F, attached ? 0.5F : (width < 0.3 ? 0.0F : 1.0F), alpha);
      }

      flush(buffers, ZoltraakRenderTypes.GREAT_PLUME);
   }

   private static void volumeBeam(
      VertexConsumer b, Matrix4f m, Vec3 axis, Vec3 u, Vec3 v, Vec3 camera, double tail, double front, double width, float t, float alpha
   ) {
      int sides = 64;
      int body = 32;
      int cap = 12;
      double length = front - tail;
      double endRadius = width * 0.29;
      Vec3[][] rings = new Vec3[body + cap + 1][sides + 1];
      float[] xs = new float[rings.length];

      for (int j = 0; j < rings.length; j++) {
         double p = Math.min(1.0, (double)j / body);
         double z = tail + length * p;
         double radius = width * (0.255 + 0.105 * (1.0F - ease((p - 0.01) / 0.19)) + 0.035 * ease((p - 0.55) / 0.45));
         if (j > body) {
            double a = (j - body) * Math.PI / (2 * cap);
            z = front + endRadius * Math.sin(a);
            radius = endRadius * Math.cos(a);
         }

         xs[j] = (float)((z - tail) / length);

         for (int k = 0; k <= sides; k++) {
            double angle = k * Math.PI * 2.0 / sides;
            double ripple = 1.0 + 0.012 * Math.sin(p * 31.0 - t * 0.21 + Math.cos(angle) * 2.0) + 0.007 * Math.cos(angle * 5.0 + p * 19.0 - t * 0.17);
            rings[j][k] = axis.scale(z).add(radial(u, v, angle, radius * ripple));
         }
      }

      for (int j = 0; j < rings.length - 1; j++) {
         for (int k = 0; k < sides; k++) {
            Vec3 a = rings[j][k];
            Vec3 c = rings[j + 1][k];
            Vec3 d = rings[j + 1][k + 1];
            Vec3 e = rings[j][k + 1];
            Vec3 normal = c.subtract(a).cross(e.subtract(a));
            Vec3 center = a.add(c).add(d).add(e).scale(0.25);
            Vec3 rad = center.subtract(axis.scale(center.dot(axis)));
            if (normal.dot(rad) < 0.0) {
               normal = normal.scale(-1.0);
            }

            if (!(normal.dot(camera.subtract(center)) <= 0.0)) {
               float y = 2.0F + (float)k / sides;
               float q = 2.0F + (float)(k + 1) / sides;
               vertex(b, m, a, xs[j], y, t / 255.0F, (float)length / 128.0F, (float)width / 16.0F, alpha);
               vertex(b, m, c, xs[j + 1], y, t / 255.0F, (float)length / 128.0F, (float)width / 16.0F, alpha);
               vertex(b, m, d, xs[j + 1], q, t / 255.0F, (float)length / 128.0F, (float)width / 16.0F, alpha);
               vertex(b, m, e, xs[j], q, t / 255.0F, (float)length / 128.0F, (float)width / 16.0F, alpha);
            }
         }
      }

      if (camera.subtract(axis.scale(tail)).dot(axis) < 0.0) {
         Vec3 center = axis.scale(tail);

         for (int k = 0; k < sides; k++) {
            float y = 2.0F + (k + 0.5F) / sides;
            vertex(b, m, center, 0.0F, y, t / 255.0F, (float)length / 128.0F, (float)width / 16.0F, alpha);
            vertex(b, m, rings[0][k], 0.0F, 2.0F + (float)k / sides, t / 255.0F, (float)length / 128.0F, (float)width / 16.0F, alpha);
            vertex(b, m, rings[0][k + 1], 0.0F, 2.0F + (float)(k + 1) / sides, t / 255.0F, (float)length / 128.0F, (float)width / 16.0F, alpha);
            vertex(b, m, center, 0.0F, y, t / 255.0F, (float)length / 128.0F, (float)width / 16.0F, alpha);
         }
      }
   }

   private static void rimBeam(VertexConsumer b, Matrix4f m, Vec3 a, Vec3 c, Vec3 side, double width, float t, float alpha) {
      Vec3 axis = c.subtract(a).normalize();
      float length = (float)a.distanceTo(c);
      float extent = (float)(width / Math.max(0.025, length));
      Vec3 w = side.scale(width);
      Vec3 end = c.add(axis.scale(width));
      vertex(b, m, a.subtract(w), 0.0F, 0.0F, t / 255.0F, length / 128.0F, (float)width / 16.0F, alpha);
      vertex(b, m, end.subtract(w), 1.0F + extent, 0.0F, t / 255.0F, length / 128.0F, (float)width / 16.0F, alpha);
      vertex(b, m, end.add(w), 1.0F + extent, 1.0F, t / 255.0F, length / 128.0F, (float)width / 16.0F, alpha);
      vertex(b, m, a.add(w), 0.0F, 1.0F, t / 255.0F, length / 128.0F, (float)width / 16.0F, alpha);
   }

   private static void seal(MultiBufferSource buffers, Matrix4f m, Vec3 dir, Vec3 u, Vec3 v, double radius, float alpha) {
      VertexConsumer b = buffers.getBuffer(ZoltraakRenderTypes.ZOL_CIRCLE);
      quad(b, m, dir.scale(-0.08), u.scale(radius), v.scale(radius), 1.0F, 1.0F, 1.0F, alpha);
      flush(buffers, ZoltraakRenderTypes.ZOL_CIRCLE);
   }

   private static void ring(VertexConsumer b, Matrix4f m, Vec3 c, Vec3 u, Vec3 v, double r, float w, float alpha, float red, float green, float blue) {
      for (int i = 0; i < 128; i++) {
         double a = i * Math.PI / 64.0;
         double z = (i + 1) * Math.PI / 64.0;
         ribbon(b, m, c.add(radial(u, v, a, r)), c.add(radial(u, v, z, r)), radial(u, v, (a + z) / 2.0, 1.0), w, alpha, red, green, blue);
      }
   }

   private static void ribbon(VertexConsumer b, Matrix4f m, Vec3 a, Vec3 c, Vec3 side, float w, float alpha, float r, float g, float blue) {
      Vec3 s = side.scale(w);
      tri(b, m, a.subtract(s), a.add(s), c.add(s), r, g, blue, alpha);
      tri(b, m, a.subtract(s), c.add(s), c.subtract(s), r, g, blue, alpha);
   }

   private static void tri(VertexConsumer b, Matrix4f m, Vec3 a, Vec3 c, Vec3 d, float r, float g, float blue, float alpha) {
      for (Vec3 p : new Vec3[]{a, c, d}) {
         b.addVertex(m, (float)p.x, (float)p.y, (float)p.z).setColor(r, g, blue, alpha);
      }
   }

   private static void quad(VertexConsumer b, Matrix4f m, Vec3 c, Vec3 u, Vec3 v, float r, float g, float blue, float a) {
      vertex(b, m, c.subtract(u).subtract(v), 0.0F, 1.0F, r, g, blue, a);
      vertex(b, m, c.add(u).subtract(v), 1.0F, 1.0F, r, g, blue, a);
      vertex(b, m, c.add(u).add(v), 1.0F, 0.0F, r, g, blue, a);
      vertex(b, m, c.subtract(u).add(v), 0.0F, 0.0F, r, g, blue, a);
   }

   private static void vertex(VertexConsumer b, Matrix4f m, Vec3 p, float u, float v, float r, float g, float blue, float a) {
      b.addVertex(m, (float)p.x, (float)p.y, (float)p.z).setUv(u, v).setColor(r, g, blue, a);
   }

   private static void flush(MultiBufferSource b, RenderType t) {
      if (b instanceof BufferSource s) {
         s.endBatch(t);
      }
   }
}
