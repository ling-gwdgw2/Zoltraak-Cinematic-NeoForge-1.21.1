package com.frierenflight.zoltraakcinematic.registry;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.item.FrierenStaffItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCinematicItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ZoltraakCinematicMod.MODID);

    public static final DeferredItem<Item> FRIEREN_STAFF =
            ITEMS.register("frieren_staff", FrierenStaffItem::new);

    public static final DeferredItem<Item> MIRROR_LOTUS_RING =
            ITEMS.register("mirror_lotus_ring", com.frierenflight.zoltraakcinematic.item.MirrorLotusRingItem::new);

    public static final DeferredItem<Item> MANA_CONCEALMENT_PENDANT =
            ITEMS.register("mana_concealment_pendant", com.frierenflight.zoltraakcinematic.item.ManaConcealmentPendantItem::new);

    public static final DeferredItem<Item> ORDINARY_GRIMOIRE =
            ITEMS.register("ordinary_grimoire", com.frierenflight.zoltraakcinematic.item.OrdinaryGrimoireItem::new);

    public static final DeferredItem<Item> QUAL_SPAWN_EGG =
            ITEMS.register("qual_spawn_egg", () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(
                    ModCinematicEntities.QUAL_BOSS, 0x1A0926, 0x8A1FB5, new Item.Properties()));

    public static final DeferredItem<Item> QUAL_SEALING_STONE =
            ITEMS.register("qual_sealing_stone", () -> new net.minecraft.world.item.BlockItem(
                    ModCinematicBlocks.QUAL_SEALING_STONE.get(), new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));

    public static final DeferredItem<Item> ELDER_SAGE_GRIMOIRE =
            ITEMS.register("elder_sage_grimoire", com.frierenflight.zoltraakcinematic.item.ElderSageGrimoireItem::new);

    public static final DeferredItem<Item> HORN_OF_CORRUPTION =
            ITEMS.register("horn_of_corruption", com.frierenflight.zoltraakcinematic.item.HornOfCorruptionItem::new);

    public static final DeferredItem<Item> CORRUPTION_CORE =
            ITEMS.register("corruption_core", com.frierenflight.zoltraakcinematic.item.CorruptionCoreItem::new);

    public static final DeferredItem<Item> GARGANTUA_SCROLL =
            ITEMS.register("gargantua_scroll", com.frierenflight.zoltraakcinematic.item.GargantuaScrollItem::new);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
