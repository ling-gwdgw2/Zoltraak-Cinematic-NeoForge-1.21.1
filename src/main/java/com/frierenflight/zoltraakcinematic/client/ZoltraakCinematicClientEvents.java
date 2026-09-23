package com.frierenflight.zoltraakcinematic.client;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.client.renderer.ZoltraakPhotonBeamRenderer;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class ZoltraakCinematicClientEvents {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ZoltraakCinematicClientEvents::onRegisterEntityRenderers);
    }

    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModCinematicEntities.ZOLTRAAK_BEAM.get(), ZoltraakPhotonBeamRenderer::new);
    }
}
