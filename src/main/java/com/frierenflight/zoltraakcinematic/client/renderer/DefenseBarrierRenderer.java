package com.frierenflight.zoltraakcinematic.client.renderer;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.entity.DefenseBarrierEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class DefenseBarrierRenderer extends EntityRenderer<DefenseBarrierEntity> {
    private static final ResourceLocation TEX_GROUND_CIRCLE =
            ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "textures/spell/defense_ground_circle.png");
    private static final ResourceLocation TEX_GLOW =
            ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "textures/spell/zoltraak_glow.png");

    public DefenseBarrierRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(DefenseBarrierEntity entity) {
        return TEX_GROUND_CIRCLE;
    }

    @Override
    public void render(DefenseBarrierEntity entity, float yaw, float partial, PoseStack stack, MultiBufferSource buffers, int light) {
        float age = entity.age(partial);
        float fade = entity.fade(partial);
        float life = Mth.clamp(entity.capacity() / entity.maximum(), 0.0F, 1.0F);

        if (fade <= 0.0F) {
            return;
        }

        DefenseBarrierEntity.VisualPose pose = entity.visualPose(partial);
        Vec3 u = pose.right();
        Vec3 v = pose.up();
        Vec3 normal = pose.normal();
        Vec3 hit = entity.visualHit(pose);

        Vec3 currentPos = new Vec3(
                Mth.lerp(partial, entity.xOld, entity.getX()),
                Mth.lerp(partial, entity.yOld, entity.getY()),
                Mth.lerp(partial, entity.zOld, entity.getZ())
        );
        Vec3 offset = pose.center().subtract(currentPos);

        stack.pushPose();
        stack.translate(offset.x, offset.y, offset.z);
        Matrix4f matrix = stack.last().pose();

        if (entity.mode() == DefenseBarrierEntity.MODE_DOME) {
            renderDome(entity, matrix, buffers, age, fade, life, partial, pose, hit);
        } else {
            renderDirectional(entity, matrix, buffers, age, fade, life, partial, pose, hit, u, v, normal);
        }

        stack.popPose();
    }

    // =========================================================================
    // 1. HEMISPHERICAL GEODESIC HONEYCOMB DOME (MODE_DOME)
    // =========================================================================

    private void renderDome(DefenseBarrierEntity entity, Matrix4f m, MultiBufferSource buffers,
                            float age, float fade, float life, float partial,
                            DefenseBarrierEntity.VisualPose pose, Vec3 hit) {
        double R = DefenseBarrierEntity.DOME_RADIUS;

        // --- Layer A: Translucent Energy Dome Shell (Smooth 360° Sphere Body) ---
        if (life > 0.0F) {
            renderEnergyDomeShell(m, buffers, R * 0.985, age, fade, life, partial, hit, entity.hitAge(partial));
        }

        // --- Layer B: Seamless Geodesic Honeycomb Plates & Glowing Cyan Seams (Full 360° Sphere) ---
        renderGeodesicHoneycomb(m, buffers, R, age, fade, life, partial, hit, entity.hitAge(partial));

        // --- Layer C: Sweeping Electric Arc Ribbon ---
        if (life > 0.0F) {
            renderElectricArc(m, buffers, R, age, fade);
        }

        // --- Layer D: Impact Overdrive Flash Glow ---
        float impact = entity.hitAge(partial);
        if (impact < 14.0F) {
            VertexConsumer glow = buffers.getBuffer(ZoltraakRenderTypes.ZOL_GLOW);
            double size = 0.8 + impact * 0.18;
            quad(glow, m, hit.add(hit.normalize().scale(0.08)), new Vec3(size, 0, 0), new Vec3(0, size, 0),
                    (float) Math.exp(-impact * 0.28) * fade * 0.95F);
            flush(buffers, ZoltraakRenderTypes.ZOL_GLOW);
        }
    }

    private void renderEnergyDomeShell(Matrix4f m, MultiBufferSource buffers, double r,
                                       float age, float fade, float life, float partial,
                                       Vec3 hit, float hitAge) {
        VertexConsumer surface = buffers.getBuffer(ZoltraakRenderTypes.DEFENSE_SURFACE);
        int slices = 32;
        int stacks = 14;

        for (int j = -stacks; j < stacks; j++) {
            double phi1 = (j * Math.PI * 0.5) / stacks;
            double phi2 = ((j + 1) * Math.PI * 0.5) / stacks;

            double y1 = Math.sin(phi1) * r;
            double y2 = Math.sin(phi2) * r;
            double r1 = Math.cos(phi1) * r;
            double r2 = Math.cos(phi2) * r;

            // Electric blue gradient from equator to poles
            float t1 = (float) Math.abs(j) / stacks;
            float t2 = (float) Math.abs(j + 1) / stacks;

            float red1 = 0.02F + 0.15F * t1;
            float green1 = 0.25F + 0.55F * t1;
            float blue1 = 0.88F + 0.12F * t1;
            float alpha1 = (0.28F + 0.22F * (1.0F - t1)) * fade; // brighter on horizon/equator rim!

            float red2 = 0.02F + 0.15F * t2;
            float green2 = 0.25F + 0.55F * t2;
            float blue2 = 0.88F + 0.12F * t2;
            float alpha2 = (0.28F + 0.22F * (1.0F - t2)) * fade;

            for (int i = 0; i < slices; i++) {
                double theta1 = (i * Math.PI * 2.0) / slices;
                double theta2 = ((i + 1) * Math.PI * 2.0) / slices;

                Vec3 p1 = new Vec3(Math.cos(theta1) * r1, y1, Math.sin(theta1) * r1);
                Vec3 p2 = new Vec3(Math.cos(theta2) * r1, y1, Math.sin(theta2) * r1);
                Vec3 p3 = new Vec3(Math.cos(theta2) * r2, y2, Math.sin(theta2) * r2);
                Vec3 p4 = new Vec3(Math.cos(theta1) * r2, y2, Math.sin(theta1) * r2);

                tri(surface, m, p1, p2, p3, red1, green1, blue1, alpha1);
                tri(surface, m, p1, p3, p4, red2, green2, blue2, alpha2);
            }
        }

        flush(buffers, ZoltraakRenderTypes.DEFENSE_SURFACE);
    }

    private void renderGeodesicHoneycomb(Matrix4f m, MultiBufferSource buffers, double radius,
                                         float age, float fade, float life, float partial,
                                         Vec3 hit, float hitAge) {
        VertexConsumer surface = buffers.getBuffer(ZoltraakRenderTypes.DEFENSE_SURFACE);
        VertexConsumer lines = buffers.getBuffer(ZoltraakRenderTypes.LIGHT);

        int index = 0;
        float deploy = Mth.clamp(age / 8.0F, 0.0F, 1.0F);
        float grow = 0.2F + 0.8F * (float) Math.sin(deploy * Math.PI * 0.5);

        for (GeodesicDomeData.Cell cell : GeodesicDomeData.CELLS) {
            Vec3 norm = cell.center().normalize();
            Vec3 center = norm.scale(radius * grow);
            Vec3[] verts = cell.vertices();
            int vCount = verts.length;

            // Shatter animation when broken (life == 0)
            if (life == 0.0F) {
                float shatter = 1.0F - fade;
                center = center.add(norm.scale(shatter * (3.0 + Math.sin(index * 2.3) * 2.2)))
                        .add(new Vec3(0, shatter * 2.5, 0));
            }

            // Impact glow feedback
            double distToHit = center.distanceToSqr(hit);
            double hitGlow = Math.exp(-hitAge * 0.32) * Math.exp(-distToHit * 0.40);

            // Active random cell pulsing (like energetic power tiles)
            boolean isPulseCell = (index + (int) (age * 0.20)) % 6 == 0;
            float pulse = isPulseCell ? 0.45F : 0.0F;

            float r = (float) Mth.clamp(0.05F + 0.85F * hitGlow + 0.25F * pulse, 0.0F, 1.0F);
            float g = (float) Mth.clamp(0.45F + 0.50F * hitGlow + 0.50F * pulse, 0.0F, 1.0F);
            float b = 1.0F;
            float faceAlpha = (float) Mth.clamp((0.40F + 0.45F * hitGlow + 0.35F * pulse) * fade * grow, 0.0F, 1.0F);

            // Compute scaled / shattered vertices
            Vec3[] curVerts = new Vec3[vCount];
            Vec3[] insetVerts = new Vec3[vCount];

            for (int k = 0; k < vCount; k++) {
                Vec3 p = verts[k].normalize().scale(radius * grow);
                if (life == 0.0F) {
                    float shatter = 1.0F - fade;
                    p = p.add(norm.scale(shatter * (3.0 + Math.sin(index * 2.3) * 2.2)))
                            .add(new Vec3(0, shatter * 2.5, 0));
                }
                curVerts[k] = p;
                // Inset vertex for chamfered 3D plate bevel effect
                insetVerts[k] = p.scale(0.92).add(center.scale(0.08));
            }

            // 1. Draw Inset Hexagonal Plate Face
            for (int k = 0; k < vCount; k++) {
                int next = (k + 1) % vCount;
                tri(surface, m, center, insetVerts[k], insetVerts[next], r, g, b, faceAlpha);
            }

            // 2. Draw Beveled Edge Facets (slight shade variation creating 3D depth)
            float bevelAlpha = faceAlpha * 0.70F;
            for (int k = 0; k < vCount; k++) {
                int next = (k + 1) % vCount;
                tri(surface, m, curVerts[k], curVerts[next], insetVerts[next], r * 0.8F, g * 0.85F, b, bevelAlpha);
                tri(surface, m, curVerts[k], insetVerts[next], insetVerts[k], r * 0.8F, g * 0.85F, b, bevelAlpha);
            }

            // 3. Draw Glowing Neon Cyan Seams along Hexagon Borders
            float flash = (float) (Math.exp(-hitAge * 0.28) * Math.exp(-distToHit * 0.35));
            float linePower = fade * grow * (0.85F + 0.5F * flash);

            for (int k = 0; k < vCount; k++) {
                int next = (k + 1) % vCount;
                Vec3 p1 = curVerts[k];
                Vec3 p2 = curVerts[next];

                // Outer cyan glow line (width 0.05)
                line(lines, m, p1, p2, norm, 0.045, linePower * 0.25F, 0.0F, 0.85F, 1.0F);
                // Core neon line (width 0.018)
                line(lines, m, p1, p2, norm, 0.016, linePower * 0.85F, 0.25F, 0.95F, 1.0F);
                // Center crisp highlight (width 0.006)
                line(lines, m, p1, p2, norm, 0.005, linePower * 1.0F, 0.85F, 0.98F, 1.0F);
            }

            // Stress Cracks if capacity < 40%
            if (life < 0.4F && index % 3 == 0) {
                Vec3 p1 = curVerts[0].scale(0.8).add(center.scale(0.2));
                Vec3 p2 = center.add(new Vec3(0.15, -0.1, 0.1));
                Vec3 p3 = curVerts[3 % vCount].scale(0.7).add(center.scale(0.3));
                line(lines, m, p1, p2, norm, 0.012, linePower * 0.9F, 0.95F, 0.98F, 1.0F);
                line(lines, m, p2, p3, norm, 0.010, linePower * 0.9F, 0.95F, 0.98F, 1.0F);
            }

            index++;
        }

        flush(buffers, ZoltraakRenderTypes.DEFENSE_SURFACE);
        flush(buffers, ZoltraakRenderTypes.LIGHT);
    }

    private void renderElectricArc(Matrix4f m, MultiBufferSource buffers, double radius, float age, float fade) {
        VertexConsumer lines = buffers.getBuffer(ZoltraakRenderTypes.LIGHT);

        // Sweeping Electric Arc Ribbon (as in Reference Anime frame-20)
        double arcAngleBase = age * 0.04;
        int arcSteps = 24;
        for (int i = 0; i < arcSteps; i++) {
            double frac1 = (double) i / arcSteps;
            double frac2 = (double) (i + 1) / arcSteps;

            double phi1 = Math.toRadians(-40.0 + 80.0 * Math.sin(frac1 * Math.PI));
            double phi2 = Math.toRadians(-40.0 + 80.0 * Math.sin(frac2 * Math.PI));

            double th1 = arcAngleBase + frac1 * Math.PI * 0.75;
            double th2 = arcAngleBase + frac2 * Math.PI * 0.75;

            double arcR1 = Math.cos(phi1) * (radius + 0.03);
            double arcY1 = Math.sin(phi1) * (radius + 0.03);
            double arcR2 = Math.cos(phi2) * (radius + 0.03);
            double arcY2 = Math.sin(phi2) * (radius + 0.03);

            Vec3 ap1 = new Vec3(Math.cos(th1) * arcR1, arcY1, Math.sin(th1) * arcR1);
            Vec3 ap2 = new Vec3(Math.cos(th2) * arcR2, arcY2, Math.sin(th2) * arcR2);

            float arcAlpha = (float) Math.sin(frac1 * Math.PI) * fade * 0.95F;
            line(lines, m, ap1, ap2, ap1.normalize(), 0.050, arcAlpha * 0.35F, 0.0F, 0.85F, 1.0F);
            line(lines, m, ap1, ap2, ap1.normalize(), 0.016, arcAlpha * 1.0F, 0.7F, 0.98F, 1.0F);
        }

        flush(buffers, ZoltraakRenderTypes.LIGHT);
    }

    // =========================================================================
    // 2. DIRECTIONAL AEGIS SHIELD (MODE_DIRECTIONAL)
    // =========================================================================

    private void renderDirectional(DefenseBarrierEntity entity, Matrix4f m, MultiBufferSource buffers,
                                   float age, float fade, float life, float partial,
                                   DefenseBarrierEntity.VisualPose pose, Vec3 hit,
                                   Vec3 u, Vec3 v, Vec3 normal) {
        VertexConsumer surface = buffers.getBuffer(ZoltraakRenderTypes.DEFENSE_SURFACE);
        VertexConsumer lines = buffers.getBuffer(ZoltraakRenderTypes.LIGHT);
        int index = 0;
        float deploy = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
        float grow = 0.2F + 0.8F * (float) Math.sin(deploy * Math.PI * 0.5);

        for (Vec3 cell : DefenseBarrierEntity.cells()) {
            double size = DefenseBarrierEntity.CELL_RADIUS * grow;
            Vec3 center = u.scale(cell.x * grow).add(v.scale(cell.y * grow));

            if (life == 0.0F) {
                float shatterTime = 1.0F - fade;
                center = center.add(u.scale(cell.x * shatterTime * 2.5))
                        .add(v.scale(cell.y * shatterTime * 2.5))
                        .add(normal.scale(Math.sin(index * 2.4) * shatterTime * 3.0));
            }

            double distToHit = center.distanceToSqr(hit);
            double hitGlow = Math.exp(-entity.hitAge(partial) * 0.35) * Math.exp(-distToHit * 0.55);

            boolean isPulse = (index + (int) (age * 0.25)) % 5 == 0;
            float pulse = isPulse ? 0.35F : 0.0F;

            float r = (float) Mth.clamp(0.08F + 0.85F * hitGlow + 0.2F * pulse, 0.0F, 1.0F);
            float g = (float) Mth.clamp(0.55F + 0.40F * hitGlow + 0.4F * pulse, 0.0F, 1.0F);
            float b = 1.0F;
            float alpha = (float) Mth.clamp((0.45F + 0.45F * hitGlow + 0.3F * pulse) * fade * grow, 0.0F, 1.0F);

            // Inset plate face
            double insetSize = size * 0.90;
            for (int k = 0; k < 6; k++) {
                tri(surface, m, center, center.add(radial(u, v, k, insetSize)), center.add(radial(u, v, k + 1, insetSize)), r, g, b, alpha);
            }
            // Bevel facet
            for (int k = 0; k < 6; k++) {
                Vec3 p1 = center.add(radial(u, v, k, size));
                Vec3 p2 = center.add(radial(u, v, k + 1, size));
                Vec3 ip1 = center.add(radial(u, v, k, insetSize));
                Vec3 ip2 = center.add(radial(u, v, k + 1, insetSize));
                tri(surface, m, p1, p2, ip2, r * 0.8F, g * 0.85F, b, alpha * 0.7F);
                tri(surface, m, p1, ip2, ip1, r * 0.8F, g * 0.85F, b, alpha * 0.7F);
            }

            // Neon Borders
            float flash = (float) (Math.exp(-entity.hitAge(partial) * 0.28) * Math.exp(-distToHit * 0.40));
            float power = fade * grow * (0.85F + 0.5F * flash);

            for (int k = 0; k < 6; k++) {
                Vec3 p1 = center.add(radial(u, v, k, size));
                Vec3 p2 = center.add(radial(u, v, k + 1, size));
                line(lines, m, p1, p2, normal, 0.045, power * 0.25F, 0.0F, 0.85F, 1.0F);
                line(lines, m, p1, p2, normal, 0.016, power * 0.85F, 0.25F, 0.95F, 1.0F);
                line(lines, m, p1, p2, normal, 0.005, power * 1.0F, 0.85F, 0.98F, 1.0F);
            }

            if (life < 0.4F && index % 3 == 0) {
                Vec3 p1 = center.add(u.scale(-size * 0.5));
                Vec3 p2 = center.add(v.scale(size * 0.17));
                Vec3 p3 = center.add(u.scale(size * 0.3)).add(v.scale(-size * 0.55));
                line(lines, m, p1, p2, normal, 0.012, power * 0.85F, 0.95F, 0.98F, 1.0F);
                line(lines, m, p2, p3, normal, 0.010, power * 0.85F, 0.95F, 0.98F, 1.0F);
            }

            index++;
        }

        flush(buffers, ZoltraakRenderTypes.DEFENSE_SURFACE);
        flush(buffers, ZoltraakRenderTypes.LIGHT);

        // Impact flash
        float impact = entity.hitAge(partial);
        if (impact < 12.0F) {
            VertexConsumer glow = buffers.getBuffer(ZoltraakRenderTypes.ZOL_GLOW);
            double size = 0.55 + impact * 0.14;
            Vec3 at = hit.add(normal.scale(0.04));
            quad(glow, m, at, u.scale(size), v.scale(size), (float) Math.exp(-impact * 0.32) * fade * 0.9F);
            flush(buffers, ZoltraakRenderTypes.ZOL_GLOW);
        }
    }

    // =========================================================================
    // HELPER PRIMITIVES
    // =========================================================================

    private static Vec3 radial(Vec3 u, Vec3 v, int k, double size) {
        double a = (Math.PI / 6.0) + k * Math.PI / 3.0;
        return u.scale(Math.cos(a) * size).add(v.scale(Math.sin(a) * size));
    }

    private static void line(VertexConsumer b, Matrix4f m, Vec3 a, Vec3 c, Vec3 normal, double width, float alpha, float r, float g, float blue) {
        Vec3 side = c.subtract(a).cross(normal).normalize().scale(width);
        tri(b, m, a.subtract(side), a.add(side), c.add(side), r, g, blue, alpha);
        tri(b, m, a.subtract(side), c.add(side), c.subtract(side), r, g, blue, alpha);
    }

    private static void tri(VertexConsumer b, Matrix4f m, Vec3 a, Vec3 c, Vec3 d, float r, float g, float blue, float alpha) {
        for (Vec3 p : new Vec3[]{a, c, d}) {
            b.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, blue, alpha);
        }
    }

    private static void quad(VertexConsumer b, Matrix4f m, Vec3 c, Vec3 u, Vec3 v, float alpha) {
        Vec3[] p = new Vec3[]{
                c.subtract(u).subtract(v),
                c.add(u).subtract(v),
                c.add(u).add(v),
                c.subtract(u).add(v)
        };
        float[][] uv = new float[][]{{0.0F, 1.0F}, {1.0F, 1.0F}, {1.0F, 0.0F}, {0.0F, 0.0F}};

        for (int i = 0; i < 4; i++) {
            b.addVertex(m, (float) p[i].x, (float) p[i].y, (float) p[i].z).setUv(uv[i][0], uv[i][1]).setColor(0.40F, 0.85F, 1.0F, alpha);
        }
    }

    private static void flush(MultiBufferSource b, RenderType t) {
        if (b instanceof MultiBufferSource.BufferSource s) {
            s.endBatch(t);
        }
    }
}
