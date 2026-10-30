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



public final class ZoltraakRenderer<T extends Entity & IZoltraakVisualEntity> extends EntityRenderer<T> {

   private final BlackZoltraakRenderer<T> blackRenderer;

   private final GreatZoltraakRenderer<T> greatRenderer;

   private final RapidZoltraakRenderer<T> rapidRenderer;



   public ZoltraakRenderer(Context c) {

      super(c);

      this.shadowRadius = 0.0F;

      this.blackRenderer = new BlackZoltraakRenderer<>(c);

      this.greatRenderer = new GreatZoltraakRenderer<>(c);

      this.rapidRenderer = new RapidZoltraakRenderer<>(c);

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

      if (!ZoltraakRenderPass.defer(this, e, yaw, partial, pose, light)) {

         if (e.mode() == ZoltraakMode.LARGE) {

            this.greatRenderer.render(e, yaw, partial, pose, buffers, light);

         } else if (e.mode() == ZoltraakMode.RAPID) {

            this.rapidRenderer.render(e, yaw, partial, pose, buffers, light);

         } else if (e.black()) {

            this.blackRenderer.render(e, yaw, partial, pose, buffers, light);

         } else {

            float t = e.age(partial);

            float shot = t - 14.0F;

            Minecraft mc = Minecraft.getInstance();

            boolean first = mc.player != null

               && e.casterId() == mc.player.getId()

               && mc.getCameraEntity() == mc.player

               && mc.options.getCameraType().isFirstPerson();

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

               RenderType circleType = e.black() ? ZoltraakRenderTypes.BLACK_CIRCLE : ZoltraakRenderTypes.ZOL_CIRCLE;

               VertexConsumer b = buffers.getBuffer(circleType);

               // Single magic circle

               quad(b, mat, primaryCenter, u.scale(radius), v.scale(radius), formation, 1.0F, 1.0F, circleAlpha);

               flush(buffers, circleType);

            }



            RenderType glowType = e.black() ? ZoltraakRenderTypes.BLACK_GLOW : ZoltraakRenderTypes.ZOL_GLOW;

            VertexConsumer glow = buffers.getBuffer(glowType);

            float charge = ease(t / 14.0F);

            float energy = e.fired() && shot >= 0.0F ? (1.0F - ease((shot - 7.0F) / 11.0F)) : 0.0F;

            float muzzle = shot < 0.0F ? charge * 0.55F : Math.max((float)Math.exp(-Math.max(0.0F, shot) * 0.24), energy * 0.85F);

            if (muzzle > 0.003) {

               double size = (0.17 + charge * 0.35 + (shot >= 0.0F ? 0.6 * Math.exp(-shot * 0.22) : 0.0)) * (first ? 0.72 : 1.0) * e.scale();

               if (first) {

                  muzzle *= 0.75F;

               }



               if (e.black()) {

                  if (shot >= 0.0F) {

                     quad(glow, mat, primaryCenter, camRight.scale(size * 2.5), camUp.scale(size * 2.5), 0.008F, 0.006F, 0.014F, muzzle * 0.55F);

                     quad(glow, mat, primaryCenter.add(dir.scale(0.045)), camRight.scale(size), camUp.scale(size), 0.008F, 0.006F, 0.014F, muzzle);

                     quad(glow, mat, primaryCenter, camRight.scale(size * 4.0), camUp.scale(size * 0.12), 0.008F, 0.006F, 0.014F, muzzle);

                     quad(glow, mat, primaryCenter, camRight.scale(size * 0.1), camUp.scale(size * 2.0), 0.008F, 0.006F, 0.014F, muzzle * 0.7F);

                  } else {

                     quad(glow, mat, primaryCenter.add(dir.scale(0.02)), camRight.scale(size * 0.4), camUp.scale(size * 0.4), 0.25F, 0.12F, 0.35F, muzzle * 0.35F);

                  }

               } else {

                  if (shot >= 0.0F) {

                     quad(glow, mat, primaryCenter, camRight.scale(size * 2.5), camUp.scale(size * 2.5), 0.44F, 0.72F, 1.0F, muzzle * 0.55F);

                     quad(glow, mat, primaryCenter.add(dir.scale(0.045)), camRight.scale(size), camUp.scale(size), 1.0F, 1.0F, 1.0F, muzzle);

                     quad(glow, mat, primaryCenter, camRight.scale(size * 4.0), camUp.scale(size * 0.12), 0.8F, 0.93F, 1.0F, muzzle);

                     quad(glow, mat, primaryCenter, camRight.scale(size * 0.1), camUp.scale(size * 2.0), 0.9F, 0.97F, 1.0F, muzzle * 0.7F);

                  } else {

                     quad(glow, mat, primaryCenter.add(dir.scale(0.02)), camRight.scale(size * 0.4), camUp.scale(size * 0.4), 0.65F, 0.88F, 1.0F, muzzle * 0.35F);

                  }

               }

            }



            flush(buffers, glowType);

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

                     ribbon(lines, mat, pa, pb, ring(u, v, a + p * 1.4, 1.0), (float)(0.025 * Math.sin(p * Math.PI)), charge * 0.65F, 0.8F, 0.94F, 1.0F);

                  }

               }



               for (int k = 0; k < 26; k++) {

                  double p = (t * 0.065 + k * 0.618) % 1.0;

                  double a = k * 2.399;

                  double r = ((1.0 - p) * 1.8 + 0.08) * e.scale();

                  Vec3 head = ring(u, v, a, r).add(dir.scale(-0.35 * (1.0 - p)));

                  Vec3 tail = ring(u, v, a, r + 0.08 + 0.25 * p).add(dir.scale(-0.4 * (1.0 - p)));

                  ribbon(lines, mat, tail, head, side, 0.012F, (float)(Math.sin(p * Math.PI) * charge * 0.65), 0.48F, 0.8F, 1.0F);

               }

            }



            if (e.fired() && shot >= 0.0F) {

               double head = Math.min(e.length(), shot * 24.0);

               double tail = 0.0;

               if (head > tail && energy > 0.0F) {

                  Vec3 a = primaryCenter;

                  Vec3 b = a.add(dir.scale(head));



                  // Camera-Facing Volumetric Radiance Quad (Pure Emissive Anime Shader)

                  flush(buffers, ZoltraakRenderTypes.LIGHT);

                  VertexConsumer beam = buffers.getBuffer(ZoltraakRenderTypes.ZOL_BEAM);

                  double beamHalfWidth = (first ? 1.05 : 1.35) * e.scale();

                  beamQuad(beam, mat, a, b, side.scale(beamHalfWidth), t / 64.0F, energy, (float)head, (float)beamHalfWidth);

                  flush(buffers, ZoltraakRenderTypes.ZOL_BEAM);

                  lines = buffers.getBuffer(ZoltraakRenderTypes.LIGHT);



                  // Kinetic Electric Discharges and Ionized Filaments along perimeter

                  double coreRadius = (first ? 0.22 : 0.32) * e.scale() * energy;

                  for (int k = 0; k < 8; k++) {

                     double z = (shot * 8.5F + k * 5.43) % Math.max(1.0, head);

                     double r = (coreRadius * 1.15 + 0.08 * Math.sin(t * 0.9 + k));

                     Vec3 radial = ring(u, v, k * 2.1 + t * 0.15, r);

                     ribbon(

                        lines,

                        mat,

                        a.add(dir.scale(Math.max(0.0, z - 2.8))).add(radial),

                        a.add(dir.scale(z)).add(radial),

                        side,

                        0.010F * e.scale(),

                        energy * 0.75F,

                        0.25F,

                        0.85F,

                        1.0F

                     );

                  }

               }



               if (shot > 5.0F && shot < 18.0F) {

                  float aRibbon = (1.0F - ease((shot - 5.0F) / 13.0F)) * 0.22F * energy;

                  ribbon(lines, mat, primaryCenter.add(dir.scale(0.2)), primaryCenter.add(dir.scale(head)), side, 0.016F * e.scale(), aRibbon, 0.25F, 0.75F, 1.0F);

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

                  ribbon(lines, mat, tip.subtract(ray.scale(0.15 + 0.45 * fade)), tip, camRight, 0.013F * e.scale(), fade * 0.8F, 0.7F, 0.9F, 1.0F);

               }



               flush(buffers, ZoltraakRenderTypes.LIGHT);

               RenderType impactGlowType = e.black() ? ZoltraakRenderTypes.BLACK_GLOW : ZoltraakRenderTypes.ZOL_GLOW;

               glow = buffers.getBuffer(impactGlowType);

               float burst = (float)Math.exp(-dt * 0.27);

               if (e.black()) {

                  quad(glow, mat, center, iu.scale((3.0 + dt * 0.18) * e.scale()), iv.scale((3.0 + dt * 0.18) * e.scale()), 0.008F, 0.006F, 0.014F, burst * 0.7F);

                  quad(glow, mat, center, iu.scale((1.2 + dt * 0.05) * e.scale()), iv.scale((1.2 + dt * 0.05) * e.scale()), 0.008F, 0.006F, 0.014F, burst);

                  quad(glow, mat, center, iu.scale((5.0 + dt * 0.2) * e.scale()), iv.scale(0.13 * e.scale()), 0.008F, 0.006F, 0.014F, burst);

                  quad(glow, mat, center, iu.scale(0.08 * e.scale()), iv.scale(3.0 * e.scale()), 0.008F, 0.006F, 0.014F, burst);

               } else {

                  quad(glow, mat, center, iu.scale((3.0 + dt * 0.18) * e.scale()), iv.scale((3.0 + dt * 0.18) * e.scale()), 0.4F, 0.67F, 1.0F, burst * 0.7F);

                  quad(glow, mat, center, iu.scale((1.2 + dt * 0.05) * e.scale()), iv.scale((1.2 + dt * 0.05) * e.scale()), 1.0F, 1.0F, 1.0F, burst);

                  quad(glow, mat, center, iu.scale((5.0 + dt * 0.2) * e.scale()), iv.scale(0.13 * e.scale()), 0.8F, 0.95F, 1.0F, burst);

                  quad(glow, mat, center, iu.scale(0.08 * e.scale()), iv.scale(3.0 * e.scale()), 1.0F, 1.0F, 1.0F, burst);

               }

               flush(buffers, impactGlowType);



               float progress = Mth.clamp(dt / 26.0F, 0.0F, 1.0F);

               RenderType impactType = e.black() ? ZoltraakRenderTypes.BLACK_IMPACT : ZoltraakRenderTypes.ZOL_IMPACT;

               VertexConsumer impact = buffers.getBuffer(impactType);

               float isBlackVal = e.black() ? 1.0F : 0.0F;



               // 1. Surface Planar Shockwave Disk & Ground Scorch/Fracture Lines

               double surfRadius = (2.4 + dt * 0.28) * (e.mode() == ZoltraakMode.LARGE ? 2.5 : 1.25) * e.scale();

               Vec3 surfCenter = center.add(normal.scale(0.03));

               quad(impact, mat, surfCenter, iu.scale(surfRadius), iv.scale(surfRadius), progress, isBlackVal, 1.0F, fade);



               // 2. Camera-Facing Volumetric Billowing Anime Dust/Smoke Cloud & Shockwave Crest

               double sparkSize = (3.0 + dt * 0.34) * (e.mode() == ZoltraakMode.LARGE ? 2.4 : 1.2) * e.scale();

               Vec3 sphereCenter = center.add(normal.scale(sparkSize * 0.35));

               quad(impact, mat, sphereCenter, camRight.scale(sparkSize), camUp.scale(sparkSize), progress, isBlackVal, 0.0F, fade);

               flush(buffers, impactType);

            } else {

               flush(buffers, ZoltraakRenderTypes.LIGHT);

            }



            pose.popPose();

         }

      }

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



   private static void beamQuad(VertexConsumer b, Matrix4f m, Vec3 a, Vec3 c, Vec3 w, float age, float alpha, float span, float halfWidth) {

      float spanNorm = Mth.clamp(span / 256.0F, 0.0F, 1.0F);

      float widthNorm = Mth.clamp(halfWidth / 16.0F, 0.0F, 1.0F);

      tex(b, m, a.subtract(w), 0.0F, 0.0F, age, spanNorm, widthNorm, alpha);

      tex(b, m, c.subtract(w), 1.0F, 0.0F, age, spanNorm, widthNorm, alpha);

      tex(b, m, c.add(w), 1.0F, 1.0F, age, spanNorm, widthNorm, alpha);

      tex(b, m, a.add(w), 0.0F, 1.0F, age, spanNorm, widthNorm, alpha);

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

