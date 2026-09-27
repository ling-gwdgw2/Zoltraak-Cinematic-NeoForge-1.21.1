package com.frierenflight.zoltraakcinematic;

import com.frierenflight.zoltraakcinematic.client.ZoltraakCinematicClientEvents;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicEntities;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicItems;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSounds;
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
        com.frierenflight.zoltraakcinematic.registry.ModCinematicAttributes.register(modEventBus);
        com.frierenflight.zoltraakcinematic.registry.ModCinematicSchools.register(modEventBus);
        ModCinematicItems.register(modEventBus);
        ModCinematicSounds.register(modEventBus);
        com.frierenflight.zoltraakcinematic.registry.ModCinematicMobEffects.register(modEventBus);
        ModCinematicEntities.register(modEventBus);
        ModCinematicSpells.register(modEventBus);
        com.frierenflight.zoltraakcinematic.event.DefenseEvents.register();
        com.frierenflight.zoltraakcinematic.event.FlightEvents.register();
        modEventBus.addListener(com.frierenflight.zoltraakcinematic.network.ModCinematicNetworking::register);

        modEventBus.addListener(this::buildCreativeModeTabContents);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ZoltraakCinematicClientEvents.register(modEventBus);
        }
    }

    private void buildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeTabRegistry.EQUIPMENT_TAB.getKey()) {
            event.accept(ModCinematicItems.FRIEREN_STAFF.get());
            event.accept(ModCinematicItems.MIRROR_LOTUS_RING.get());
            event.accept(ModCinematicItems.MANA_CONCEALMENT_PENDANT.get());
            event.accept(ModCinematicItems.ORDINARY_GRIMOIRE.get());
        }
    }
}
