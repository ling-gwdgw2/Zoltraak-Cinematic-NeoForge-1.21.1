package com.frierenvoid.client;

import com.frierenvoid.FrierenVoid;
import com.frierenvoid.SingularityEntity;
import com.frierenvoid.VoidChoreography;
import com.frierenvoid.VoidConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Matrix4f;

public final class SingularityRenderer extends EntityRenderer<SingularityEntity> {
   private static final double TAU = Math.PI * 2;

   public SingularityRenderer(Context c) {
      super(c);
      this.shadowRadius = 0.0F;
   }

   public static float ease(double x) {
      float t = (float)Math.max(0.0, Math.min(1.0, x));
      return t * t * (3.0F - 2.0F * t);
   }

   public static float scale(float t) {
      if (t >= 200.0F) {
         return 0.0F;
      }

      float grow = ease(t / 30.0F);
      return grow * (t < 170.0F ? 1.0F + 0.025F * (float)Math.sin(t * 0.11) : 1.0F - ease((t - 170.0F) / 30.0F));
   }

   public void render(SingularityEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
      float t = e.age(partial);
      float s = e.rising() ? VoidChoreography.core(t) / 2.35F : scale(t);
      float fade = 1.0F - ease((t - 205.0F) / 35.0F);
      pose.pushPose();
      Vec3 displayed = new Vec3(Mth.lerp(partial, e.xo, e.getX()), Mth.lerp(partial, e.yo, e.getY()), Mth.lerp(partial, e.zo, e.getZ()));
      Vec3 correction = e.visualPosition(partial).subtract(displayed);
      pose.translate(correction.x, correction.y, correction.z);
      int quality = (Integer)VoidConfig.QUALITY.get();
      double cameraDistance = this.entityRenderDispatcher.camera.getPosition().distanceTo(e.position());
      if (cameraDistance > 90.0) {
         quality = 0;
      }

      int segments = 48 + quality * 24;
      if (s > 0.001F) {
         if ((Boolean)VoidConfig.REFRACTION.get() && quality > 0 && VoidClient.lens != null) {
            if (buffers instanceof BufferSource source) {
               source.endBatch();
            }

            SceneLens.capture();
            pose.pushPose();
            pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
            float size = 2.35F * s * 2.6F;
            Matrix4f m = pose.last().pose();
            VertexConsumer lens = buffers.getBuffer(VoidRenderTypes.LENS);
            quadVertex(lens, m, -size, -size, 0.0F, 0.0F, t);
            quadVertex(lens, m, size, -size, 1.0F, 0.0F, t);
            quadVertex(lens, m, size, size, 1.0F, 1.0F, t);
            quadVertex(lens, m, -size, size, 0.0F, 1.0F, t);
            flush(buffers, VoidRenderTypes.LENS);
            pose.popPose();
         }

         VertexConsumer core = buffers.getBuffer(VoidRenderTypes.CORE);
         sphere(core, pose.last().pose(), 2.35 * s, segments, 18 + quality * 6);
         flush(buffers, VoidRenderTypes.CORE);
         if (VoidClient.corona != null) {
            pose.pushPose();
            pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
            float size = 2.35F * s * 2.6F;
            float width = 2.35F * s * (2.8F + 9.2F * VoidChoreography.growth(t));
            VertexConsumer corona = buffers.getBuffer(VoidRenderTypes.CORONA);
            Matrix4f m = pose.last().pose();
            quadVertex(corona, m, -width, -size, 0.0F, 0.0F, t);
            quadVertex(corona, m, width, -size, 1.0F, 0.0F, t);
            quadVertex(corona, m, width, size, 1.0F, 1.0F, t);
            quadVertex(corona, m, -width, size, 0.0F, 1.0F, t);
            flush(buffers, VoidRenderTypes.CORONA);
            pose.popPose();
         }

         VertexConsumer glow = buffers.getBuffer(VoidRenderTypes.LIGHT);
         pose.pushPose();
         pose.mulPose(Axis.XP.rotationDegrees(3.0F));
         Matrix4f m = pose.last().pose();

         for (int lane = 0; lane < 10 + quality * 6; lane++) {
            double r = (2.42 + lane * (0.035 + 0.42 * VoidChoreography.growth(t))) * s;
            double speed = t * (0.018 + 0.12 / (1.0 + lane * 0.3));

            for (int j = 0; j < segments; j++) {
               double a = (Math.PI * 2) * j / segments;
               double b = (Math.PI * 2) * (j + 1) / segments;
               float flicker = (float)(0.42 + 0.58 * Math.pow(0.5 + 0.5 * Math.sin(a * 5.0 - speed * 3.0 + lane * 1.9), 3.0));
               float alpha = flicker * (float)Math.exp(-lane * 0.045) * 0.68F;
               Vec3 ra = ring(r, a, speed, lane, s);
               Vec3 rb = ring(r, b, speed, lane, s);
               ribbon(glow, m, ra, rb, 0.16 * s * (1.0 + lane * 0.055), 1.0F, 0.58F - lane * 0.012F, 0.025F, alpha);
               if (quality > 0) {
                  ribbon(glow, m, ra, rb, 0.28 * s, 1.0F, 0.24F, 0.015F, alpha * 0.09F);
               }
            }
         }

         pose.popPose();
         m = pose.last().pose();

         for (int k = 0; k < 3 + quality * 2; k++) {
            double angle = k * 2.399 + t * 0.035;
            double pulse = Math.pow(Math.max(0.0, Math.sin(t * 0.31 + k * 2.1)), 12.0);
            if (pulse > 0.02) {
               ribbon(
                  glow,
                  m,
                  new Vec3(Math.cos(angle) * 2.5 * s, Math.sin(angle) * 2.5 * s, 0.0),
                  new Vec3(Math.cos(angle) * 5.0 * s, Math.sin(angle) * 5.0 * s, Math.sin(k) * s),
                  0.02 * s,
                  1.0F,
                  0.36F,
                  0.025F,
                  (float)pulse * 0.3F * ease((t - 65.0F) / 40.0F)
               );
            }
         }

         flush(buffers, VoidRenderTypes.LIGHT);
         debris(e, pose, buffers, t, s, quality, light);
         wind(e, pose, buffers, t, quality);
      }

      if (t >= 200.0F) {
         if (t < 224.0F && VoidClient.corona != null) {
            pose.pushPose();
            pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
            VertexConsumer flash = buffers.getBuffer(VoidRenderTypes.CORONA);
            Matrix4f fm = pose.last().pose();
            float size = 18.0F + (t - 200.0F) * 1.25F;
            quadVertex(flash, fm, -size, -size, 0.0F, 0.0F, t);
            quadVertex(flash, fm, size, -size, 1.0F, 0.0F, t);
            quadVertex(flash, fm, size, size, 1.0F, 1.0F, t);
            quadVertex(flash, fm, -size, size, 0.0F, 1.0F, t);
            flush(buffers, VoidRenderTypes.CORONA);
            pose.popPose();
         }

         VertexConsumer glow = buffers.getBuffer(VoidRenderTypes.LIGHT);
         Matrix4f m = pose.last().pose();
         double travel = (t - 200.0F) / 40.0;

         for (int wave = 0; wave < 3; wave++) {
            double dt = t - 200.0F - wave * 4;
            if (!(dt < 0.0)) {
               double p = Math.min(1.0, dt / 36.0);
               double r = 0.8 + e.radius() * 3.25 * Math.sqrt(p);
               float envelope = ease(dt / 1.5) * (1.0F - ease((dt - 12.0) / 24.0));
               pose.pushPose();
               pose.mulPose(Axis.XP.rotationDegrees(wave == 0 ? 0.0F : (wave == 1 ? 18.0F : -22.0F)));

               for (int j = 0; j < segments * 2; j++) {
                  double a = j * (Math.PI * 2) / (segments * 2);
                  double b = (j + 1) * (Math.PI * 2) / (segments * 2);
                  float broken = (float)(0.45 + 0.55 * Math.pow(0.5 + 0.5 * Math.sin(a * 7.0 - wave * 2 + dt * 0.16), 2.0));
                  Vec3 pa = new Vec3(Math.cos(a) * r, Math.sin(a * 5.0 + dt * 0.14) * p * 0.7, Math.sin(a) * r);
                  Vec3 pb = new Vec3(Math.cos(b) * r, Math.sin(b * 5.0 + dt * 0.14) * p * 0.7, Math.sin(b) * r);
                  ribbon(glow, pose.last().pose(), pa, pb, 0.25 + p * 0.6, 0.82F, 0.88F, 1.0F, envelope * broken * 0.7F);
                  if (quality > 0) {
                     ribbon(glow, pose.last().pose(), pa, pb, 1.1 + p * 1.8, 0.55F, 0.62F, 1.0F, envelope * broken * 0.08F);
                  }
               }

               pose.popPose();
            }
         }

         if (t < 228.0F) {
            for (int k = 0; k < 32 + quality * 12; k++) {
               double a = k * 2.399963;
               double dt = t - 200.0F;
               float f = 1.0F - ease(dt / 28.0);
               double reach = (18 + k % 7 * 4) * (0.3 + 0.7 * ease(dt / 4.0));
               Vec3 tip = new Vec3(Math.cos(a) * reach, Math.sin(a) * reach, Math.sin(k * 1.7) * reach * 0.65);
               ribbon(glow, m, Vec3.ZERO, tip, 0.42 * f, 0.76F, 0.5F, 1.0F, f * 0.7F);
               ribbon(glow, m, Vec3.ZERO, tip.scale(0.88), 0.16 * f, 1.0F, 0.94F, 1.0F, f);
            }
         }

         for (int k = 0; k < 40 + quality * 50; k++) {
            double a = k * 2.399963 + e.seed();
            double y = 1.0 - 2.0 * (k + 0.5) / (40 + quality * 50);
            double horizontal = Math.sqrt(1.0 - y * y);
            Vec3 direction = new Vec3(Math.cos(a) * horizontal, y, Math.sin(a) * horizontal);
            double speed = 0.7 + k % 11 * 0.075;
            double elapsed = t - 200.0F;
            Vec3 tip = direction.scale(elapsed * speed).add(0.0, -elapsed * elapsed * 0.003, 0.0);
            Vec3 tail = tip.subtract(direction.scale(0.6 + elapsed * 0.06));
            float ember = 1.0F - ease((t - 209.0F) / 31.0F);
            ribbon(glow, m, tail, tip, 0.025 + quality * 0.012, 1.0F, 0.35F + k % 3 * 0.2F, 0.12F, ember);
            if (k % 4 == 0) {
               ribbon(glow, m, tail, tip, 0.1, 0.55F, 0.18F, 1.0F, ember * 0.35F);
            }
         }

         flush(buffers, VoidRenderTypes.LIGHT);
         releaseWind(e, pose, buffers, t, quality);
      }

      pose.popPose();
   }

   private static void releaseWind(SingularityEntity e, PoseStack pose, MultiBufferSource buffers, float t, int quality) {
      float dt = t - 200.0F;
      if (!(dt < 0.0F) && !(dt >= 40.0F)) {
         Vec3 center = e.visualPosition(0.0F);
         BlockHitResult ground = e.level().clip(new ClipContext(center, center.add(0.0, -40.0, 0.0), Block.COLLIDER, Fluid.NONE, e));
         double floor = ground.getType() == Type.MISS ? -8.0 : ground.getLocation().y - center.y + 0.3;
         int streams = 20 + quality * 14;
         int steps = 20 + quality * 8;
         VertexConsumer out = buffers.getBuffer(VoidRenderTypes.LIGHT);
         Matrix4f m = pose.last().pose();

         for (int k = 0; k < streams; k++) {
            double elapsed = dt - k % 5 * 1.1;
            if (!(elapsed <= 0.0)) {
               float envelope = ease(elapsed / 3.0) * (1.0F - ease((dt - 18.0F) / 22.0F));
               double head = e.radius() * (0.15 + 3.35 * Math.pow(Math.min(1.0, elapsed / 40.0), 0.72));
               double seed = k * 2.399963 + e.seed() * 0.001;
               double height = k % 3 == 0 ? floor : -3.0 + k % 5 * 1.35;
               Vec3[] points = new Vec3[steps + 1];
               Vec3[] sides = new Vec3[steps + 1];
               float[] opacity = new float[steps + 1];

               for (int j = 0; j <= steps; j++) {
                  points[j] = gustPoint(seed, (double)j / steps, elapsed, head, height, k);
               }

               for (int j = 0; j <= steps; j++) {
                  double p = (double)j / steps;
                  float taper = (float)Math.pow(Math.max(0.0, Math.sin(p * Math.PI)), 0.7);
                  float pulse = (float)(0.65 + 0.35 * Math.sin(p * 13.0 - elapsed * 0.35 + seed));
                  double width = (0.18 + k % 4 * 0.13) * taper * (0.5 + 0.5 * ease(elapsed / 8.0));
                  Vec3 tangent = points[Math.min(steps, j + 1)].subtract(points[Math.max(0, j - 1)]);
                  sides[j] = tangent.cross(new Vec3(0.19, 1.0, 0.13)).normalize().scale(width);
                  opacity[j] = envelope * taper * pulse;
               }

               for (int j = 0; j < steps; j++) {
                  gustStrip(out, m, points[j], points[j + 1], sides[j], sides[j + 1], 0.87F, 0.94F, 1.0F, opacity[j] * 0.65F, opacity[j + 1] * 0.65F);
                  gustStrip(
                     out,
                     m,
                     points[j],
                     points[j + 1],
                     sides[j].scale(3.8),
                     sides[j + 1].scale(3.8),
                     0.66F,
                     0.77F,
                     0.9F,
                     opacity[j] * 0.085F,
                     opacity[j + 1] * 0.085F
                  );
                  if (quality > 0) {
                     gustStrip(
                        out,
                        m,
                        points[j],
                        points[j + 1],
                        sides[j].scale(0.18),
                        sides[j + 1].scale(0.18),
                        1.0F,
                        1.0F,
                        1.0F,
                        opacity[j] * 0.45F,
                        opacity[j + 1] * 0.45F
                     );
                  }
               }
            }
         }

         flush(buffers, VoidRenderTypes.LIGHT);
      }
   }

   private static void gustStrip(VertexConsumer v, Matrix4f m, Vec3 a, Vec3 b, Vec3 sa, Vec3 sb, float red, float green, float blue, float aa, float ab) {
      Vec3 left = a.subtract(sa);
      Vec3 right = a.add(sa);
      Vec3 nextLeft = b.subtract(sb);
      Vec3 nextRight = b.add(sb);
      vertex(v, m, left, red, green, blue, aa);
      vertex(v, m, nextLeft, red, green, blue, ab);
      vertex(v, m, nextRight, red, green, blue, ab);
      vertex(v, m, left, red, green, blue, aa);
      vertex(v, m, nextRight, red, green, blue, ab);
      vertex(v, m, right, red, green, blue, aa);
   }

   private static Vec3 gustPoint(double seed, double p, double elapsed, double head, double height, int lane) {
      double bend = (lane % 2 == 0 ? 1 : -1) * (0.35 + lane % 4 * 0.16);
      double angle = seed + bend * (1.0 - p) * (1.0 - p) + Math.sin(seed + elapsed * 0.16) * 0.1 * (1.0 - p);
      double radius = head * (0.22 + 0.78 * p);
      double y = height + Math.sin(p * Math.PI) * (0.35 + lane % 4 * 0.45) + Math.sin(radius * 0.2 + seed + elapsed * 0.17) * 0.25 * p;
      return new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
   }

   private static void wind(SingularityEntity e, PoseStack pose, MultiBufferSource buffers, float t, int quality) {
      if (!(t < 24.0F) && !(t >= 198.0F)) {
         float growth = VoidChoreography.growth(t);
         float strength = growth * (1.0F - ease((t - 185.0F) / 13.0F));
         double scale = VoidChoreography.core(t) / 2.35;
         double outer = (2.42 + (9 + quality * 6) * (0.035 + 0.42 * growth)) * scale;
         int streams = 12 + quality * 8;
         int steps = 32 + quality * 10;
         pose.pushPose();
         pose.mulPose(Axis.XP.rotationDegrees(3.0F));
         VertexConsumer v = buffers.getBuffer(VoidRenderTypes.LIGHT);
         Matrix4f m = pose.last().pose();

         for (int k = 0; k < streams; k++) {
            for (int j = 0; j < steps; j++) {
               double p = (double)j / steps;
               double b = (double)(j + 1) / steps;
               double seed = k * 2.399963 + e.seed() * 0.001;
               Vec3 a = ringWindPoint(seed, p, t, outer);
               Vec3 end = ringWindPoint(seed, b, t, outer);
               float packet = (float)Math.pow(Math.max(0.0, Math.sin(p * 9.0 - t * 0.24 + k * 1.7)), 2.0);
               float alpha = strength * (float)Math.sin(p * Math.PI) * (0.055F + 0.38F * packet);
               double width = (0.035 + 0.07 * growth) * (0.35 + Math.sin(p * Math.PI));
               ribbon(v, m, a, end, width, 0.83F, 0.87F, 0.9F, alpha);
               if (quality > 0) {
                  ribbon(v, m, a, end, width * 3.0, 0.68F, 0.76F, 0.83F, alpha * 0.12F);
               }
            }
         }

         flush(buffers, VoidRenderTypes.LIGHT);
         pose.popPose();
      }
   }

   private static Vec3 ringWindPoint(double seed, double p, float t, double outer) {
      double a = seed - t * 0.085 + p * 2.25;
      double r = outer * (1.58 - 0.84 * p);
      double y = Math.sin(seed * 1.7) * 0.55 * (1.0 - p) + Math.sin(a * 3.0) * 0.08;
      return new Vec3(Math.cos(a) * r, y, Math.sin(a) * r);
   }

   private static Vec3 ring(double r, double a, double time, int lane, float s) {
      double wave = Math.sin(a * 7.0 + time * 2.0 + lane) * 0.045 * s;
      return new Vec3(Math.cos(a + time) * (r + wave), Math.sin(a * 3.0 - time + lane) * 0.045 * s, Math.sin(a + time) * (r + wave));
   }

   private static void debris(SingularityEntity e, PoseStack p, MultiBufferSource b, float t, float s, int q, int light) {
      if (!(t < 54.0F) && !(t >= 193.0F)) {
         for (int i = 0; i < 10 + q * 14; i++) {
            double offset = i * 0.61803398875 % 1.0;
            double phase = ((t - 45.0F) * 0.006 + offset) % 1.0;
            double r = (2.6 + (e.radius() - 2.6) * (1.0 - phase) * (1.0 - phase)) * s;
            double a = i * 2.399963 + t * (0.025 + phase * 0.1);
            double size = (0.32 + i % 5 * 0.21) * ease((t - 54.0F) / 30.0F) * (1.0F - ease((phase - 0.8) / 0.2));
            p.pushPose();
            p.translate(Math.cos(a) * r, Math.sin(i * 1.4) * (1.0 - phase) * r * 0.35, Math.sin(a) * r);
            p.mulPose(Axis.XP.rotation((float)(a * 2.0 + i)));
            p.mulPose(Axis.ZP.rotation((float)(t * 0.06 + i)));
            p.scale((float)size, (float)(size * 0.68), (float)size);
            p.translate(-0.5, -0.5, -0.5);
            Minecraft.getInstance()
               .getBlockRenderer()
               .renderSingleBlock((i % 3 == 0 ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState(), p, b, light, OverlayTexture.NO_OVERLAY);
            p.popPose();
         }
      }
   }

   private static void sphere(VertexConsumer v, Matrix4f m, double r, int n, int lat) {
      for (int y = 0; y < lat; y++) {
         for (int x = 0; x < n; x++) {
            Vec3 a = point(r, (Math.PI * 2) * x / n, Math.PI * y / lat);
            Vec3 b = point(r, (Math.PI * 2) * (x + 1) / n, Math.PI * y / lat);
            Vec3 c = point(r, (Math.PI * 2) * (x + 1) / n, Math.PI * (y + 1) / lat);
            Vec3 d = point(r, (Math.PI * 2) * x / n, Math.PI * (y + 1) / lat);
            vertex(v, m, a, 0.0F, 0.0F, 0.0F, 1.0F);
            vertex(v, m, b, 0.0F, 0.0F, 0.0F, 1.0F);
            vertex(v, m, c, 0.0F, 0.0F, 0.0F, 1.0F);
            vertex(v, m, a, 0.0F, 0.0F, 0.0F, 1.0F);
            vertex(v, m, c, 0.0F, 0.0F, 0.0F, 1.0F);
            vertex(v, m, d, 0.0F, 0.0F, 0.0F, 1.0F);
         }
      }
   }

   private static Vec3 point(double r, double a, double p) {
      return new Vec3(r * Math.sin(p) * Math.cos(a), r * Math.cos(p), r * Math.sin(p) * Math.sin(a));
   }

   private static void ribbon(VertexConsumer v, Matrix4f m, Vec3 a, Vec3 b, double w, float r, float g, float blue, float alpha) {
      Vec3 delta = b.subtract(a);
      Vec3 side = delta.cross(new Vec3(0.19, 1.0, 0.13)).normalize().scale(w);
      Vec3 c = b.add(side);
      Vec3 d = a.add(side);
      a = a.subtract(side);
      b = b.subtract(side);
      vertex(v, m, a, r, g, blue, alpha);
      vertex(v, m, b, r, g, blue, alpha);
      vertex(v, m, c, r, g, blue, alpha);
      vertex(v, m, a, r, g, blue, alpha);
      vertex(v, m, c, r, g, blue, alpha);
      vertex(v, m, d, r, g, blue, alpha);
   }

   private static void vertex(VertexConsumer v, Matrix4f m, Vec3 p, float r, float g, float b, float a) {
      v.addVertex(m, (float)p.x, (float)p.y, (float)p.z).setColor(r, g, b, Math.max(0.0F, Math.min(1.0F, a)));
   }

   private static void quadVertex(VertexConsumer v, Matrix4f m, float x, float y, float u, float w, float t) {
      v.addVertex(m, x, y, 0.0F).setUv(u, w).setColor(t / 240.0F, 1.0F, 1.0F, 1.0F);
   }

   private static void flush(MultiBufferSource b, RenderType type) {
      if (b instanceof BufferSource source) {
         source.endBatch(type);
      }
   }

   public ResourceLocation getTextureLocation(SingularityEntity e) {
      return FrierenVoid.id("textures/spell/singularity.png");
   }
}
