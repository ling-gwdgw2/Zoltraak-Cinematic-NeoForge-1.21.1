package com.frierenflight.zoltraakcinematic.client.renderer;

import net.minecraft.world.phys.Vec3;
import java.util.List;

public interface IZoltraakVisualEntity {
   ZoltraakMode mode();
   boolean black();
   float age(float partial);
   float rawAge(float partial);
   Vec3 visualOrigin(float partial);
   Vec3 visualDirection(float partial);
   Vec3 getPosition(float partial);
   int casterId();
   double length();
   float scale();
   boolean fired();
   boolean impact();
   float impactAge();
   float largeImpactTime();
   Vec3 end();
   Vec3 normal();
   double beamFront(float partial);
   float guidedFade(float partial);
   List<Vec3> guidedPath(float partial);
   int getId();
   boolean isRemoved();
}
