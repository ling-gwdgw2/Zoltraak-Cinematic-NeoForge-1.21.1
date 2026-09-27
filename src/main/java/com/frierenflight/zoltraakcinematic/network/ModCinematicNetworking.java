package com.frierenflight.zoltraakcinematic.network;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModCinematicNetworking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ZoltraakCinematicMod.MODID).versioned("1");
        registrar.playToServer(
                DefenseCastPayload.TYPE,
                DefenseCastPayload.STREAM_CODEC,
                DefenseCastPayload::handle
        );
        registrar.playToServer(
                FlightTogglePayload.TYPE,
                FlightTogglePayload.STREAM_CODEC,
                FlightTogglePayload::handle
        );
    }
}
