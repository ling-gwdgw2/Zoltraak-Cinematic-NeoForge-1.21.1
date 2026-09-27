package com.frierenvoid.client;

import com.frierenvoid.FrierenVoid;
import com.frierenvoid.ZoltraakMode;
import com.frierenvoid.ZoltraakNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent.Key;
import net.neoforged.neoforge.client.event.InputEvent.MouseButton.Post;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

@EventBusSubscriber(modid = "frierenvoid", value = Dist.CLIENT)
public final class ZoltraakModeInput {
   public static final KeyMapping CYCLE = new KeyMapping(
      "key.frierenvoid.cycle_mode", KeyConflictContext.IN_GAME, Type.KEYSYM, 340, "key.categories.frierenvoid"
   );

   @SubscribeEvent
   public static void key(Key e) {
      if (e.getAction() == 1 && CYCLE.isActiveAndMatches(InputConstants.getKey(e.getKey(), e.getScanCode()))) {
         cycle();
      }
   }

   @SubscribeEvent
   public static void mouse(Post e) {
      if (e.getAction() == 1 && CYCLE.isActiveAndMatches(Type.MOUSE.getOrCreate(e.getButton()))) {
         cycle();
      }
   }

   private static void cycle() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.level != null && mc.screen == null && mc.isWindowActive() && !mc.player.isSpectator()) {
         SpellSelectionManager selection = ClientMagicData.getSpellSelectionManager();
         if (selection != null) {
            AbstractSpell spell = selection.getSelectedSpellData().getSpell();
            if (spell == FrierenVoid.ZOLTRAAK.get() || spell == FrierenVoid.BLACK_ZOLTRAAK.get()) {
               ZoltraakNetwork.cycle();
            }
         }
      }
   }

   public static void receive(int mode) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         ZoltraakMode.select(mc.player, ZoltraakMode.from(mode));
      }
   }
}
