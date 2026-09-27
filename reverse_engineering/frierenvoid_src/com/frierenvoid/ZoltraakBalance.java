package com.frierenvoid;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "frierenvoid")
public final class ZoltraakBalance {
   @SubscribeEvent
   public static void cast(SpellOnCastEvent e) {
      if ((e.getSpellId().equals("frierenvoid:zoltraak") || e.getSpellId().equals("frierenvoid:black_zoltraak"))
         && ZoltraakMode.selected(e.getEntity()) == ZoltraakMode.LARGE) {
         e.setManaCost(e.getManaCost() * 2);
      }
   }
}
