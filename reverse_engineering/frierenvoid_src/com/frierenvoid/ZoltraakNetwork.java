package com.frierenvoid;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = "frierenvoid")
public final class ZoltraakNetwork {
   public static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("1");
      registrar.playToServer(ZoltraakNetwork.Cycle.TYPE, ZoltraakNetwork.Cycle.CODEC, (m, c) -> {
         if (c.player() instanceof ServerPlayer p) {
            select(p, ZoltraakMode.from((ZoltraakMode.selected(p).ordinal() + 1) % 3));
            p.displayClientMessage(Component.translatable("ui.frierenvoid.mode.selected", new Object[]{ZoltraakMode.selected(p).label()}), true);
         }
      });
      registrar.playToServer(ZoltraakNetwork.Hold.TYPE, ZoltraakNetwork.Hold.CODEC, (m, c) -> {
         if (c.player() instanceof ServerPlayer p) {
            RapidZoltraakChannel.hold(p, m.down(), m.black());
         }
      });
      registrar.playToClient(ZoltraakNetwork.Sync.TYPE, ZoltraakNetwork.Sync.CODEC, (m, c) -> ZoltraakMode.select(c.player(), ZoltraakMode.from(m.mode())));
   }

   public static void select(ServerPlayer p, ZoltraakMode mode) {
      ZoltraakMode.select(p, mode);
      sync(p);
   }

   private static void sync(ServerPlayer p) {
      PacketDistributor.sendToPlayer(p, new ZoltraakNetwork.Sync(ZoltraakMode.selected(p).ordinal()), new CustomPacketPayload[0]);
   }

   public static void hold(boolean down, boolean black) {
      PacketDistributor.sendToServer(new ZoltraakNetwork.Hold(down, black), new CustomPacketPayload[0]);
   }

   public static void cycle() {
      PacketDistributor.sendToServer(new ZoltraakNetwork.Cycle(), new CustomPacketPayload[0]);
   }

   @SubscribeEvent
   public static void clone(Clone e) {
      ZoltraakMode.select(e.getEntity(), ZoltraakMode.selected(e.getOriginal()));
   }

   @SubscribeEvent
   public static void login(PlayerLoggedInEvent e) {
      if (e.getEntity() instanceof ServerPlayer p) {
         sync(p);
      }
   }

   @SubscribeEvent
   public static void respawn(PlayerRespawnEvent e) {
      if (e.getEntity() instanceof ServerPlayer p) {
         sync(p);
      }
   }

   @SubscribeEvent
   public static void dimension(PlayerChangedDimensionEvent e) {
      if (e.getEntity() instanceof ServerPlayer p) {
         sync(p);
      }
   }

   public record Cycle() implements CustomPacketPayload {
      public static final Type<ZoltraakNetwork.Cycle> TYPE = new Type(FrierenVoid.id("cycle"));
      public static final StreamCodec<RegistryFriendlyByteBuf, ZoltraakNetwork.Cycle> CODEC = StreamCodec.unit(new ZoltraakNetwork.Cycle());

      public Type<ZoltraakNetwork.Cycle> type() {
         return TYPE;
      }
   }

   public record Hold(boolean down, boolean black) implements CustomPacketPayload {
      public static final Type<ZoltraakNetwork.Hold> TYPE = new Type(FrierenVoid.id("hold"));
      public static final StreamCodec<RegistryFriendlyByteBuf, ZoltraakNetwork.Hold> CODEC = StreamCodec.of((b, m) -> {
         b.writeBoolean(m.down());
         b.writeBoolean(m.black());
      }, b -> new ZoltraakNetwork.Hold(b.readBoolean(), b.readBoolean()));

      public Type<ZoltraakNetwork.Hold> type() {
         return TYPE;
      }
   }

   public record Sync(int mode) implements CustomPacketPayload {
      public static final Type<ZoltraakNetwork.Sync> TYPE = new Type(FrierenVoid.id("sync"));
      public static final StreamCodec<RegistryFriendlyByteBuf, ZoltraakNetwork.Sync> CODEC = StreamCodec.of(
         (b, m) -> b.writeVarInt(m.mode()), b -> new ZoltraakNetwork.Sync(b.readVarInt())
      );

      public Type<ZoltraakNetwork.Sync> type() {
         return TYPE;
      }
   }
}
