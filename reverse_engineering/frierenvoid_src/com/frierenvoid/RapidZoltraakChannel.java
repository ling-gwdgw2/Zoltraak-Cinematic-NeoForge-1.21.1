package com.frierenvoid;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;

@EventBusSubscriber(modid = "frierenvoid")
public final class RapidZoltraakChannel {
   private static final Map<UUID, RapidZoltraakChannel.Intent> HELD = new HashMap<>();
   private static final Map<UUID, RapidZoltraakChannel.Channel> ACTIVE = new HashMap<>();

   public static void hold(ServerPlayer p, boolean down, boolean black) {
      if (!down) {
         clear(p);
      } else if (p.isAlive() && !p.isSpectator() && ZoltraakMode.selected(p) == ZoltraakMode.RAPID) {
         HELD.put(p.getUUID(), new RapidZoltraakChannel.Intent(p.serverLevel().getGameTime(), black));
      } else {
         clear(p);
      }
   }

   public static boolean held(ServerPlayer p, boolean black) {
      RapidZoltraakChannel.Intent i = HELD.get(p.getUUID());
      return i != null && i.black == black && p.serverLevel().getGameTime() - i.tick <= 12L;
   }

   public static void begin(ServerPlayer p, boolean black, float power, float mana) {
      if (held(p, black) && !ACTIVE.containsKey(p.getUUID())) {
         ACTIVE.put(
            p.getUUID(), new RapidZoltraakChannel.Channel(black, power * ZoltraakMode.RAPID.damage, Math.max(1.0F, mana / 6.0F), p.serverLevel().getGameTime())
         );
         pulse(p);
      }
   }

   public static boolean active(ServerPlayer p) {
      return ACTIVE.containsKey(p.getUUID());
   }

   public static void clear(ServerPlayer p) {
      HELD.remove(p.getUUID());
      ACTIVE.remove(p.getUUID());

      for (Entity e : p.serverLevel().getAllEntities()) {
         if (e instanceof ZoltraakEntity z && z.casterId() == p.getId() && z.mode() == ZoltraakMode.RAPID && !z.fired()) {
            z.discard();
         }
      }
   }

   @SubscribeEvent
   public static void stopped(ServerStoppedEvent e) {
      HELD.clear();
      ACTIVE.clear();
   }

   @SubscribeEvent
   public static void tick(Post e) {
      if (e.getEntity() instanceof ServerPlayer p) {
         pulse(p);
      }
   }

   private static void pulse(ServerPlayer p) {
      RapidZoltraakChannel.Channel c = ACTIVE.get(p.getUUID());
      if (c == null) {
         RapidZoltraakChannel.Intent i = HELD.get(p.getUUID());
         if (i != null && p.serverLevel().getGameTime() - i.tick > 12L) {
            HELD.remove(p.getUUID());
         }
      } else if (held(p, c.black) && p.isAlive() && !p.isSpectator() && ZoltraakMode.selected(p) == ZoltraakMode.RAPID) {
         long now = p.serverLevel().getGameTime();
         if (now >= c.next) {
            int count = 0;

            for (Entity e : p.serverLevel().getAllEntities()) {
               if (e instanceof ZoltraakEntity && !e.isRemoved()) {
                  count++;
               }
            }

            if (count >= 24) {
               clear(p);
            } else {
               MagicData mana = MagicData.getPlayerMagicData(p);
               if (c.shots >= 6 && !p.isCreative()) {
                  if (mana.getMana() < c.cost) {
                     clear(p);
                     return;
                  }

                  mana.setMana(mana.getMana() - c.cost);
               }

               p.serverLevel().addFreshEntity(ZoltraakEntity.createRapid(p.serverLevel(), p, c.damage, c.black, c.shots++));
               c.next = now + 4L;
            }
         }
      } else {
         clear(p);
      }
   }

   @SubscribeEvent
   public static void logout(PlayerLoggedOutEvent e) {
      if (e.getEntity() instanceof ServerPlayer p) {
         clear(p);
      }
   }

   @SubscribeEvent
   public static void dimension(PlayerChangedDimensionEvent e) {
      if (e.getEntity() instanceof ServerPlayer p) {
         clear(p);
      }
   }

   private static final class Channel {
      final boolean black;
      final float damage;
      final float cost;
      int shots;
      long next;

      Channel(boolean b, float d, float c, long n) {
         this.black = b;
         this.damage = d;
         this.cost = c;
         this.next = n;
      }
   }

   private record Intent(long tick, boolean black) {
   }
}
