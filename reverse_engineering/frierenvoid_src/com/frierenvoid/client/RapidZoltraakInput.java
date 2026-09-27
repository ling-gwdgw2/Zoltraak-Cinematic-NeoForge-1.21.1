package com.frierenvoid.client;

import com.frierenvoid.FrierenVoid;
import com.frierenvoid.ZoltraakMode;
import com.frierenvoid.ZoltraakNetwork;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.player.ExtendedKeyMapping;
import io.redspace.ironsspellbooks.player.KeyMappings;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Pre;

@EventBusSubscriber(modid = "frierenvoid", value = Dist.CLIENT)
public final class RapidZoltraakInput {
   private static boolean sent;
   private static boolean colour;
   private static int ticks;

   @SubscribeEvent
   public static void tick(Pre e) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null) {
         SpellSelectionManager manager = ClientMagicData.getSpellSelectionManager();
         AbstractSpell spell = manager == null ? null : manager.getSelectedSpellData().getSpell();
         int index = manager == null ? -1 : manager.getGlobalSelectionIndex();
         boolean quick = index >= 0
            && index < KeyMappings.QUICK_CAST_MAPPINGS.size()
            && ((ExtendedKeyMapping)KeyMappings.QUICK_CAST_MAPPINGS.get(index)).isDown();
         boolean black = spell == FrierenVoid.BLACK_ZOLTRAAK.get();
         boolean down = mc.screen == null
            && mc.isWindowActive()
            && !mc.player.isSpectator()
            && ZoltraakMode.selected(mc.player) == ZoltraakMode.RAPID
            && (black || spell == FrierenVoid.ZOLTRAAK.get())
            && (KeyMappings.SPELLBOOK_CAST_ACTIVE_KEYMAP.isDown() || mc.options.keyUse.isDown() || quick);
         if (down) {
            if (!sent || colour != black || ++ticks >= 4) {
               ZoltraakNetwork.hold(true, black);
               sent = true;
               colour = black;
               ticks = 0;
            }
         } else if (sent) {
            ZoltraakNetwork.hold(false, colour);
            sent = false;
            ticks = 0;
         }
      } else {
         sent = false;
         ticks = 0;
      }
   }
}
