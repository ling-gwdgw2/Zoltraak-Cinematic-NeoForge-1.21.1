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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
