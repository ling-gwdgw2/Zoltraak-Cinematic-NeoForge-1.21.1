package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
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

public final class RapidZoltraakRenderer<T extends Entity & IZoltraakVisualEntity> extends EntityRenderer<T> {
   private final boolean renderSeal;

   public RapidZoltraakRenderer(Context c) {
      this(c, true);
   }

   public RapidZoltraakRenderer(Context c, boolean renderSeal) {
      super(c);
      this.shadowRadius = 0.0F;
      this.renderSeal = renderSeal;
   }

   public ResourceLocation getTextureLocation(T e) {
      return ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "textures/spell/zoltraak_magic_circle.png");
   }

   public void render(T e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
      float raw = e.rawAge(partial);
      if (!(raw < 0.0F)) {
         Vec3 origin = e.visualOrigin(partial);
         Vec3 dir = e.visualDirection(partial);
         Vec3 shift = origin.subtract(e.getPosition(partial));
         pose.pushPose();
         pose.translate(shift.x, shift.y, shift.z);
         Matrix4f m = pose.last().pose();
         Vec3 camera = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
         Vec3 u = dir.cross(new Vec3(0.0, 1.0, 0.0));
         u = u.lengthSqr() < 0.001 ? new Vec3(1.0, 0.0, 0.0) : u.normalize();
         Vec3 v = u.cross(dir).normalize();
         Vec3 right = new Vec3(new Vector3f(1.0F, 0.0F, 0.0F).rotate(this.entityRenderDispatcher.cameraOrientation()));
         Vec3 up = new Vec3(new Vector3f(0.0F, 1.0F, 0.0F).rotate(this.entityRenderDispatcher.cameraOrientation()));
         float fade = e.guidedFade(partial);
         List<Vec3> path = e.guidedPath(partial);
         if (path.size() > 1 && fade > 0.001) {
            int n = path.size();
            Vec3[] points = new Vec3[n];
            Vec3[] edges = new Vec3[n];
            float[] distance = new float[n];

            for (int i = 0; i < n; i++) {
               points[i] = path.get(i).subtract(origin);
               if (i > 0) {
                  distance[i] = distance[i - 1] + (float)points[i].distanceTo(points[i - 1]);
               }
            }

            for (int i = 0; i < n; i++) {
               Vec3 tangent = points[Math.min(n - 1, i + 1)].subtract(points[Math.max(0, i - 1)]).normalize();
               Vec3 edge = tangent.cross(camera.subtract(points[i]));
               edge = edge.lengthSqr() < 1.0E-4 ? u : edge.normalize();
               if (i > 0 && edge.dot(edges[i - 1]) < 0.0) {
                  edge = edge.scale(-1.0);
               }

               double width = (e.black() ? 0.4 : 0.34) * (0.84 + 0.16 * Math.sin(distance[i] * 0.9 - raw * 0.7)) * Math.sqrt(fade);
               if (i == n - 1) {
                  width *= 0.18;
               }

               edges[i] = edge.scale(width);
            }

            VertexConsumer mesh = buffers.getBuffer(ZoltraakRenderTypes.GREAT_PLUME);

            for (int i = 0; i < n - 1; i++) {
               float a = distance[i] / 64.0F;
               float b = distance[i + 1] / 64.0F;
               vertex(mesh, m, points[i].subtract(edges[i]), a, 0.0F, raw / 255.0F, e.black() ? 1.0F : 0.0F, 1.0F, fade);
               vertex(mesh, m, points[i + 1].subtract(edges[i + 1]), b, 0.0F, raw / 255.0F, e.black() ? 1.0F : 0.0F, 1.0F, fade);
               vertex(mesh, m, points[i + 1].add(edges[i + 1]), b, 1.0F, raw / 255.0F, e.black() ? 1.0F : 0.0F, 1.0F, fade);
               vertex(mesh, m, points[i].add(edges[i]), a, 1.0F, raw / 255.0F, e.black() ? 1.0F : 0.0F, 1.0F, fade);
            }

            flush(buffers, ZoltraakRenderTypes.GREAT_PLUME);
            Vec3 tip = points[n - 1];
            VertexConsumer glow = buffers.getBuffer(ZoltraakRenderTypes.ZOL_GLOW);
            if (e.black()) {
               quad(glow, m, tip, right.scale(0.55), up.scale(0.55), 0.9F, 0.35F, 1.0F, fade * 0.8F);
            } else {
               quad(glow, m, tip, right.scale(0.55), up.scale(0.55), 0.6F, 0.88F, 1.0F, fade * 0.8F);
            }
            flush(buffers, ZoltraakRenderTypes.ZOL_GLOW);

            // Procedural Mathematical Shaders for Barrage Bullets (Replacing Vanilla Particles)
            float isBlack = e.black() ? 1.0F : 0.0F;
            if (e.impact()) {
               float dt = Math.max(0.0F, raw - e.impactAge());
               if (dt < 12.0F) {
                  float impactFade = (1.0F - (dt / 12.0F)) * fade;
                  float impactProgress = Mth.clamp(dt / 12.0F, 0.0F, 1.0F);

                  Vec3 norm = e.normal();

                  VertexConsumer impact = buffers.getBuffer(ZoltraakRenderTypes.ZOL_IMPACT);
                  double sparkSize = (1.10 + dt * 0.14) * e.scale();

                  // Camera-facing 3D Spherical Relativistic Mana Explosion
                  Vec3 sphereCenter = tip.add(norm.scale(sparkSize * 0.35));
                  quad(impact, m, sphereCenter, right.scale(sparkSize), up.scale(sparkSize), impactProgress, isBlack, 0.0F, impactFade);
                  flush(buffers, ZoltraakRenderTypes.ZOL_IMPACT);
               }
            } else {
               // In-flight Procedural Core Flare & Dielectric Sparks
               VertexConsumer impact = buffers.getBuffer(ZoltraakRenderTypes.ZOL_IMPACT);
               float flyingProgress = ((raw * 0.28F) % 1.0F) * 0.35F;
               double flyingSparkSize = 0.55 * e.scale();
               quad(impact, m, tip, right.scale(flyingSparkSize), up.scale(flyingSparkSize), flyingProgress, isBlack, 0.0F, fade * 0.75F);
               flush(buffers, ZoltraakRenderTypes.ZOL_IMPACT);
            }
         }

         if (this.renderSeal) {
            float circle = Mth.clamp(raw / 4.0F, 0.0F, 1.0F) * (1.0F - Mth.clamp((raw - 13.0F) / 10.0F, 0.0F, 1.0F));
            double radius = 1.15 * (0.3 + 0.7 * Mth.clamp(raw / 9.0F, 0.0F, 1.0F));
            RenderType circleType = e.black() ? ZoltraakRenderTypes.BLACK_CIRCLE : ZoltraakRenderTypes.ZOL_CIRCLE;
            VertexConsumer seal = buffers.getBuffer(circleType);
            if (e.black()) {
               quad(seal, m, dir.scale(-0.04), u.scale(radius), v.scale(radius), 1.0F, 1.0F, 1.0F, circle * 0.95F);
            } else {
               quad(seal, m, dir.scale(-0.04), u.scale(radius), v.scale(radius), 1.0F, 1.0F, 1.0F, circle * 0.85F);
            }
            flush(buffers, circleType);
            if (!e.fired()) {
               VertexConsumer glow = buffers.getBuffer(ZoltraakRenderTypes.ZOL_GLOW);
               double size = 0.12 + 0.28 * Mth.clamp(raw / 10.0F, 0.0F, 1.0F);
               if (e.black()) {
                  quad(glow, m, Vec3.ZERO, right.scale(size), up.scale(size), 0.9F, 0.35F, 1.0F, circle);
               } else {
                  quad(glow, m, Vec3.ZERO, right.scale(size), up.scale(size), 0.75F, 0.94F, 1.0F, circle);
               }
               flush(buffers, ZoltraakRenderTypes.ZOL_GLOW);
            }
         }

         pose.popPose();
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
