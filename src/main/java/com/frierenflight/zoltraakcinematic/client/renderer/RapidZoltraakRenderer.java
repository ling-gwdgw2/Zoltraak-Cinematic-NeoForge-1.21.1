package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
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
         if (path.size() > 1 && fade > 0.001F) {
            int n = path.size();
            Vec3[] rawPoints = new Vec3[n];
            float[] cumDist = new float[n];

            for (int i = 0; i < n; i++) {
               rawPoints[i] = path.get(i).subtract(origin);
               if (i > 0) {
                  cumDist[i] = cumDist[i - 1] + (float) rawPoints[i].distanceTo(rawPoints[i - 1]);
               }
            }

            float totalPathLen = cumDist[n - 1];
            if (totalPathLen > 0.001F) {
               // 1:1 Anime Continuous Laser Beam Length (Up to 28m continuous beam connecting circle to target)
               float targetNeedleLen = 28.0F * (e.scale() / 0.6F);

               // On impact, the beam collapses smoothly into the impact point
               if (e.impact()) {
                  float dt = Math.max(0.0F, raw - e.impactAge());
                  float collapse = Mth.clamp(1.0F - (dt / 6.0F), 0.0F, 1.0F);
                  targetNeedleLen = Math.max(0.10F, targetNeedleLen * collapse);
               }

               float effectiveNeedleLen = Math.min(totalPathLen, targetNeedleLen);
               float startDist = Math.max(0.0F, totalPathLen - effectiveNeedleLen);

               // Extract aerodynamic needle path points from startDist to totalPathLen
               List<Vec3> needlePts = new ArrayList<>();
               List<Float> needleDistances = new ArrayList<>();

               int startIdx = 0;
               while (startIdx < n - 1 && cumDist[startIdx + 1] <= startDist) {
                  startIdx++;
               }

               if (startIdx < n - 1 && cumDist[startIdx + 1] > startDist) {
                  float segLen = cumDist[startIdx + 1] - cumDist[startIdx];
                  float frac = segLen > 0.0001F ? (startDist - cumDist[startIdx]) / segLen : 0.0F;
                  Vec3 tailPos = rawPoints[startIdx].lerp(rawPoints[startIdx + 1], frac);
                  needlePts.add(tailPos);
                  needleDistances.add(0.0F);
               } else {
                  needlePts.add(rawPoints[0]);
                  needleDistances.add(0.0F);
               }

               for (int i = startIdx + 1; i < n; i++) {
                  needlePts.add(rawPoints[i]);
                  needleDistances.add(cumDist[i] - startDist);
               }

               int rawCount = needlePts.size();
               if (rawCount > 1) {
                  Vec3[] rawArr = needlePts.toArray(new Vec3[0]);
                  float[] rawDists = new float[rawCount];
                  for (int i = 0; i < rawCount; i++) rawDists[i] = needleDistances.get(i);
                  float actualNeedleLen = Math.max(0.001F, rawDists[rawCount - 1]);

                  // Evenly resample 7 aerodynamic points along the curve for perfectly smooth 3D ribboning
                  int SAMPLES = 7;
                  Vec3[] pts = new Vec3[SAMPLES];
                  float[] dists = new float[SAMPLES];
                  for (int s = 0; s < SAMPLES; s++) {
                     float d = (actualNeedleLen * s) / (float) (SAMPLES - 1);
                     pts[s] = samplePathAtDistance(rawArr, rawDists, d);
                     dists[s] = d;
                  }

                  Vec3[] primaryEdges = new Vec3[SAMPLES];
                  Vec3[] perpEdges = new Vec3[SAMPLES];

                  // 1:1 Anime Volumetric Laser Width: ~0.40m diameter for true anime energy presence
                  double baseWidth = (e.black() ? 0.22 : 0.20) * Math.sqrt(fade) * (e.scale() / 0.6F);

                  for (int i = 0; i < SAMPLES; i++) {
                     Vec3 tangent;
                     if (i == 0) {
                        tangent = pts[1].subtract(pts[0]).normalize();
                     } else if (i == SAMPLES - 1) {
                        tangent = pts[SAMPLES - 1].subtract(pts[SAMPLES - 2]).normalize();
                     } else {
                        tangent = pts[i + 1].subtract(pts[i - 1]).normalize();
                     }
                     if (tangent.lengthSqr() < 0.001) tangent = dir;

                     Vec3 edge = tangent.cross(camera.subtract(pts[i]));
                     edge = edge.lengthSqr() < 1.0E-4 ? u : edge.normalize();
                     if (i > 0 && edge.dot(primaryEdges[i - 1]) < 0.0) {
                        edge = edge.scale(-1.0);
                     }
                     primaryEdges[i] = edge.scale(baseWidth);

                     // 3D Cross-plane perpendicular edge for solid volumetric cross-section
                     Vec3 perp = tangent.cross(edge).normalize();
                     perpEdges[i] = perp.scale(baseWidth * 0.88);
                  }

                  RenderType bulletType = e.black() ? ZoltraakRenderTypes.BLACK_RAPID_BULLET : ZoltraakRenderTypes.RAPID_BULLET;
                  VertexConsumer bulletMesh = buffers.getBuffer(bulletType);
                  float darkVal = e.black() ? 1.0F : 0.0F;
                  float ageVal = raw / 255.0F;

                  for (int i = 0; i < SAMPLES - 1; i++) {
                     float uA = dists[i] / actualNeedleLen;
                     float uB = dists[i + 1] / actualNeedleLen;

                     // Primary Billboard Ribbon
                     vertex(bulletMesh, m, pts[i].subtract(primaryEdges[i]), uA, 0.0F, ageVal, darkVal, 1.0F, fade);
                     vertex(bulletMesh, m, pts[i + 1].subtract(primaryEdges[i + 1]), uB, 0.0F, ageVal, darkVal, 1.0F, fade);
                     vertex(bulletMesh, m, pts[i + 1].add(primaryEdges[i + 1]), uB, 1.0F, ageVal, darkVal, 1.0F, fade);
                     vertex(bulletMesh, m, pts[i].add(primaryEdges[i]), uA, 1.0F, ageVal, darkVal, 1.0F, fade);

                     // Perpendicular 3D Cross-Plane
                     vertex(bulletMesh, m, pts[i].subtract(perpEdges[i]), uA, 0.0F, ageVal, darkVal, 1.0F, fade * 0.90F);
                     vertex(bulletMesh, m, pts[i + 1].subtract(perpEdges[i + 1]), uB, 0.0F, ageVal, darkVal, 1.0F, fade * 0.90F);
                     vertex(bulletMesh, m, pts[i + 1].add(perpEdges[i + 1]), uB, 1.0F, ageVal, darkVal, 1.0F, fade * 0.90F);
                     vertex(bulletMesh, m, pts[i].add(perpEdges[i]), uA, 1.0F, ageVal, darkVal, 1.0F, fade * 0.90F);
                  }

                  flush(buffers, bulletType);

                  // Piercing Diamond Needle Tip Glint (Holy cyan glint for Fern; Qual void needles are pure obsidian)
                  Vec3 tip = pts[SAMPLES - 1];
                  if (!e.black()) {
                     // 1:1 Anime 4-Point Diamond Cross Star Glint (+) at beam head
                     VertexConsumer glow = buffers.getBuffer(ZoltraakRenderTypes.ZOL_GLOW);
                     double glintSize = 0.14 * Math.sqrt(fade) * (e.scale() / 0.6F);
                     quad(glow, m, tip, right.scale(glintSize * 2.6), up.scale(glintSize * 0.20), 0.70F, 0.95F, 1.0F, fade * 0.90F);
                     quad(glow, m, tip, right.scale(glintSize * 0.20), up.scale(glintSize * 2.6), 0.70F, 0.95F, 1.0F, fade * 0.90F);
                     flush(buffers, ZoltraakRenderTypes.ZOL_GLOW);
                  }

                  // Relativistic Impact Explosion & Dielectric Particles ONLY when impacting
                  // (Completely eliminates the in-flight spherical impact ball!)
                  float isBlack = e.black() ? 1.0F : 0.0F;
                  if (e.impact()) {
                     float dt = Math.max(0.0F, raw - e.impactAge());
                     if (dt < 6.0F) {
                        float impactFade = (1.0F - (dt / 6.0F)) * fade;
                        float impactProgress = Mth.clamp(dt / 6.0F, 0.0F, 1.0F);

                        Vec3 norm = e.normal();
                        if (norm.lengthSqr() < 1.0E-4) {
                           norm = dir.scale(-1.0);
                        } else {
                           norm = norm.normalize();
                        }

                        Vec3 iu = Math.abs(norm.y) > 0.95 ? norm.cross(new Vec3(1.0, 0.0, 0.0)) : norm.cross(new Vec3(0.0, 1.0, 0.0));
                        if (iu.lengthSqr() < 1.0E-4) {
                           iu = new Vec3(1.0, 0.0, 0.0);
                        } else {
                           iu = iu.normalize();
                        }
                        Vec3 iv = iu.cross(norm).normalize();

                        RenderType impactType = e.black() ? ZoltraakRenderTypes.BLACK_IMPACT : ZoltraakRenderTypes.ZOL_IMPACT;
                        VertexConsumer impact = buffers.getBuffer(impactType);

                        // 1. Surface Planar Shockwave Disk & Mana Scorch Cracks on the impacted surface
                        double surfRadius = (0.75 + dt * 0.18) * (e.scale() / 0.6F);
                        Vec3 surfCenter = tip.add(norm.scale(0.02));
                        quad(impact, m, surfCenter, iu.scale(surfRadius), iv.scale(surfRadius), impactProgress, isBlack, 1.0F, impactFade);

                        // 2. Camera-Facing Volumetric Billowing Anime Dust/Smoke Cloud & Shockwave Crest
                        double billowSize = (0.70 + dt * 0.16) * (e.scale() / 0.6F);
                        Vec3 billowCenter = tip.add(norm.scale(billowSize * 0.32));
                        quad(impact, m, billowCenter, right.scale(billowSize), up.scale(billowSize), impactProgress, isBlack, 0.0F, impactFade);

                        flush(buffers, impactType);

                        // 3. High-Key Detonation Flash Glint at the very first moment of collision (dt < 2.0 ticks)
                        if (dt < 2.0F) {
                           float flashBurst = (1.0F - dt / 2.0F) * fade;
                           RenderType glowType = e.black() ? ZoltraakRenderTypes.BLACK_GLOW : ZoltraakRenderTypes.ZOL_GLOW;
                           VertexConsumer glow = buffers.getBuffer(glowType);
                           double flashSize = 0.50 * (e.scale() / 0.6F);
                           if (e.black()) {
                              quad(glow, m, tip.add(norm.scale(0.04)), right.scale(flashSize), up.scale(flashSize), 0.008F, 0.006F, 0.014F, flashBurst * 0.8F);
                           } else {
                              quad(glow, m, tip.add(norm.scale(0.04)), right.scale(flashSize * 1.6), up.scale(flashSize * 0.14), 0.75F, 0.95F, 1.0F, flashBurst);
                              quad(glow, m, tip.add(norm.scale(0.04)), right.scale(flashSize * 0.14), up.scale(flashSize * 1.6), 0.75F, 0.95F, 1.0F, flashBurst);
                              quad(glow, m, tip.add(norm.scale(0.04)), right.scale(flashSize * 0.85), up.scale(flashSize * 0.85), 1.0F, 1.0F, 1.0F, flashBurst);
                           }
                           flush(buffers, glowType);
                        }
                     }
                  }
               }
            }
         }

         if (this.renderSeal) {
            float circle = Mth.clamp(raw / 4.0F, 0.0F, 1.0F) * (1.0F - Mth.clamp((raw - 13.0F) / 10.0F, 0.0F, 1.0F));
            double radius = 1.15 * (0.3 + 0.7 * Mth.clamp(raw / 9.0F, 0.0F, 1.0F));
            float formation = Mth.clamp(raw / 3.5F, 0.0F, 1.0F);
            RenderType circleType = e.black() ? ZoltraakRenderTypes.BLACK_CIRCLE : ZoltraakRenderTypes.ZOL_CIRCLE;
            VertexConsumer seal = buffers.getBuffer(circleType);
            if (e.black()) {
               quad(seal, m, dir.scale(-0.04), u.scale(radius), v.scale(radius), formation, 1.0F, 1.0F, circle * 0.95F);
            } else {
               quad(seal, m, dir.scale(-0.04), u.scale(radius), v.scale(radius), formation, 1.0F, 1.0F, circle * 0.85F);
            }
            flush(buffers, circleType);
            if (!e.fired()) {
               double size = 0.12 + 0.28 * Mth.clamp(raw / 10.0F, 0.0F, 1.0F);
               if (e.black()) {
                  VertexConsumer glow = buffers.getBuffer(ZoltraakRenderTypes.BLACK_GLOW);
                  quad(glow, m, Vec3.ZERO, right.scale(size), up.scale(size), 0.008F, 0.006F, 0.014F, circle * 0.75F);
                  flush(buffers, ZoltraakRenderTypes.BLACK_GLOW);
               } else {
                  VertexConsumer glow = buffers.getBuffer(ZoltraakRenderTypes.ZOL_GLOW);
                  quad(glow, m, Vec3.ZERO, right.scale(size), up.scale(size), 0.75F, 0.94F, 1.0F, circle);
                  flush(buffers, ZoltraakRenderTypes.ZOL_GLOW);
               }
            }
         }

         pose.popPose();
      }
   }

   private static Vec3 samplePathAtDistance(Vec3[] pts, float[] dists, float targetD) {
      int count = pts.length;
      if (count == 0) return Vec3.ZERO;
      if (count == 1 || targetD <= 0.0F) return pts[0];
      if (targetD >= dists[count - 1]) return pts[count - 1];

      for (int i = 0; i < count - 1; i++) {
         if (targetD <= dists[i + 1]) {
            float span = dists[i + 1] - dists[i];
            float frac = span > 0.0001F ? (targetD - dists[i]) / span : 0.0F;
            return pts[i].lerp(pts[i + 1], frac);
         }
      }
      return pts[count - 1];
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
