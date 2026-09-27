package com.frierenvoid;

import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BeamHitMath {
   public static Vec3 closest(AABB b, Vec3 p) {
      return new Vec3(Mth.clamp(p.x, b.minX, b.maxX), Mth.clamp(p.y, b.minY, b.maxY), Mth.clamp(p.z, b.minZ, b.maxZ));
   }

   public static Optional<Vec3> contact(Vec3 a, Vec3 b, AABB box, double radius) {
      if (a.distanceToSqr(closest(box, a)) <= radius * radius) {
         return Optional.of(a);
      }

      double lo = 0.0;
      double hi = 1.0;

      for (int i = 0; i < 36; i++) {
         double x = (lo * 2.0 + hi) / 3.0;
         double y = (lo + hi * 2.0) / 3.0;
         Vec3 px = a.lerp(b, x);
         Vec3 py = a.lerp(b, y);
         if (px.distanceToSqr(closest(box, px)) < py.distanceToSqr(closest(box, py))) {
            hi = y;
         } else {
            lo = x;
         }
      }

      double t = (lo + hi) / 2.0;
      Vec3 p = a.lerp(b, t);
      if (p.distanceToSqr(closest(box, p)) > radius * radius + 1.0E-8) {
         return Optional.empty();
      }

      lo = 0.0;
      hi = t;

      for (int i = 0; i < 28; i++) {
         double mid = (lo + hi) / 2.0;
         Vec3 q = a.lerp(b, mid);
         if (q.distanceToSqr(closest(box, q)) <= radius * radius) {
            hi = mid;
         } else {
            lo = mid;
         }
      }

      return Optional.of(a.lerp(b, hi));
   }
}
