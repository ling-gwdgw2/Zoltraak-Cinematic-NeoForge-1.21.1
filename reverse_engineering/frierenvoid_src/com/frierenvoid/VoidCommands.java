package com.frierenvoid;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = "frierenvoid")
public final class VoidCommands {
   @SubscribeEvent
   public static void register(RegisterCommandsEvent e) {
      LiteralArgumentBuilder<CommandSourceStack> modes = Commands.literal("zoltraak_mode");

      for (ZoltraakMode mode : ZoltraakMode.values()) {
         modes.then(Commands.literal(mode.id()).executes(c -> {
            ServerPlayer p = ((CommandSourceStack)c.getSource()).getPlayerOrException();
            ZoltraakNetwork.select(p, mode);
            p.displayClientMessage(Component.translatable("ui.frierenvoid.mode.selected", new Object[]{mode.label()}), true);
            return 1;
         }));
      }

      e.getDispatcher().register(modes);
      e.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                       "frierenvoid"
                                    )
                                    .requires(s -> s.hasPermission(2)))
                                 .then(
                                    ((LiteralArgumentBuilder)Commands.literal("scroll")
                                          .executes(c -> scroll(((CommandSourceStack)c.getSource()).getPlayerOrException(), 1)))
                                       .then(
                                          Commands.argument("level", IntegerArgumentType.integer(1, 3))
                                             .executes(
                                                c -> scroll(
                                                   ((CommandSourceStack)c.getSource()).getPlayerOrException(), IntegerArgumentType.getInteger(c, "level")
                                                )
                                             )
                                       )
                                 ))
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("defense")
                                       .executes(c -> defenseScroll(((CommandSourceStack)c.getSource()).getPlayerOrException(), 1)))
                                    .then(
                                       Commands.argument("level", IntegerArgumentType.integer(1, 3))
                                          .executes(
                                             c -> defenseScroll(
                                                ((CommandSourceStack)c.getSource()).getPlayerOrException(), IntegerArgumentType.getInteger(c, "level")
                                             )
                                          )
                                    )
                              ))
                           .then(
                              ((LiteralArgumentBuilder)Commands.literal("black_zoltraak")
                                    .executes(c -> blackScroll(((CommandSourceStack)c.getSource()).getPlayerOrException(), 1)))
                                 .then(
                                    Commands.argument("level", IntegerArgumentType.integer(1, 3))
                                       .executes(
                                          c -> blackScroll(
                                             ((CommandSourceStack)c.getSource()).getPlayerOrException(), IntegerArgumentType.getInteger(c, "level")
                                          )
                                       )
                                 )
                           ))
                        .then(Commands.literal("preview_black_zoltraak").executes(c -> {
                           ServerPlayer p = ((CommandSourceStack)c.getSource()).getPlayerOrException();
                           return ZoltraakEntity.launch(p.serverLevel(), p, 0.0F, true) ? 1 : 0;
                        })))
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("zoltraak")
                              .executes(c -> zoltraakScroll(((CommandSourceStack)c.getSource()).getPlayerOrException(), 1)))
                           .then(
                              Commands.argument("level", IntegerArgumentType.integer(1, 3))
                                 .executes(
                                    c -> zoltraakScroll(((CommandSourceStack)c.getSource()).getPlayerOrException(), IntegerArgumentType.getInteger(c, "level"))
                                 )
                           )
                     ))
                  .then(Commands.literal("preview_zoltraak").executes(c -> {
                     ServerPlayer p = ((CommandSourceStack)c.getSource()).getPlayerOrException();
                     return ZoltraakEntity.launch(p.serverLevel(), p, 0.0F, false) ? 1 : 0;
                  })))
               .then(
                  Commands.literal("preview")
                     .executes(
                        c -> {
                           ServerPlayer p = ((CommandSourceStack)c.getSource()).getPlayerOrException();
                           if (!SingularityEntity.canStart(p.serverLevel(), p.getUUID())) {
                              ((CommandSourceStack)c.getSource()).sendFailure(Component.translatable("message.frierenvoid.occupied"));
                              return 0;
                           } else {
                              boolean added = p.serverLevel()
                                 .addFreshEntity(SingularityEntity.fromHand(p.serverLevel(), p, 0.0F, ((Double)VoidConfig.RADIUS.get()).floatValue()));
                              return added ? 1 : 0;
                           }
                        }
                     )
               )
         );
   }

   private static int defenseScroll(ServerPlayer p, int level) {
      Item item = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "scroll"));
      if (item == null) {
         return 0;
      }

      ItemStack stack = new ItemStack(item);
      ISpellContainer.createScrollContainer((AbstractSpell)FrierenVoid.DEFENSE.get(), level, stack);
      if (!p.getInventory().add(stack)) {
         p.drop(stack, false);
      }

      return 1;
   }

   private static int blackScroll(ServerPlayer p, int level) {
      Item item = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "scroll"));
      if (item == null) {
         return 0;
      }

      ItemStack stack = new ItemStack(item);
      ISpellContainer.createScrollContainer((AbstractSpell)FrierenVoid.BLACK_ZOLTRAAK.get(), level, stack);
      if (!p.getInventory().add(stack)) {
         p.drop(stack, false);
      }

      return 1;
   }

   private static int zoltraakScroll(ServerPlayer p, int level) {
      Item item = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "scroll"));
      if (item == null) {
         return 0;
      }

      ItemStack stack = new ItemStack(item);
      ISpellContainer.createScrollContainer((AbstractSpell)FrierenVoid.ZOLTRAAK.get(), level, stack);
      if (!p.getInventory().add(stack)) {
         p.drop(stack, false);
      }

      return 1;
   }

   private static int scroll(ServerPlayer p, int level) {
      Item item = (Item)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "scroll"));
      if (item == null) {
         return 0;
      }

      ItemStack stack = new ItemStack(item);
      ISpellContainer.createScrollContainer((AbstractSpell)FrierenVoid.SINGULARITY.get(), level, stack);
      if (!p.getInventory().add(stack)) {
         p.drop(stack, false);
      }

      return 1;
   }
}
