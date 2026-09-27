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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
