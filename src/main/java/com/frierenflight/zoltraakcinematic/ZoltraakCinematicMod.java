package com.frierenflight.zoltraakcinematic;

import com.frierenflight.zoltraakcinematic.client.ZoltraakCinematicClientEvents;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicEntities;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicItems;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSpells;
import io.redspace.ironsspellbooks.registries.CreativeTabRegistry;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(ZoltraakCinematicMod.MODID)
public class ZoltraakCinematicMod {
    public static final String MODID = "zoltraak_cinematic";
    public static final String MOD_NAME = "Zoltraak: Cinematic Edition";

    public ZoltraakCinematicMod(IEventBus modEventBus) {
        ModCinematicItems.register(modEventBus);
        ModCinematicEntities.register(modEventBus);
        ModCinematicSpells.register(modEventBus);

        modEventBus.addListener(this::buildCreativeModeTabContents);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ZoltraakCinematicClientEvents.register(modEventBus);
        }
    }

    private void buildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeTabRegistry.EQUIPMENT_TAB.getKey()) {
            event.accept(ModCinematicItems.FRIEREN_STAFF.get());
        }
    }
}
