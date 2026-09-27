package com.frierenflight.zoltraakcinematic.client.renderer;

import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum ZoltraakMode {
   SINGLE(14, 9, 1.5F, 1.0F, 1),
   RAPID(10, 10, 1.0F, 0.25F, 6),
   LARGE(20, 80, 6.0F, 2.0F, 1);

   public final int charge;
   public final int pulse;
   public final int shots;
   public final float scale;
   public final float damage;

   ZoltraakMode(int charge, int pulse, float scale, float damage, int shots) {
      this.charge = charge;
      this.pulse = pulse;
      this.scale = scale;
      this.damage = damage;
      this.shots = shots;
   }

   public String id() {
      return this.name().toLowerCase(Locale.ROOT);
   }

   public Component label() {
      return Component.translatable("ui.zoltraak_cinematic.mode." + this.id());
   }

   public static ZoltraakMode from(int id) {
      return id >= 0 && id < values().length ? values()[id] : SINGLE;
   }
}
