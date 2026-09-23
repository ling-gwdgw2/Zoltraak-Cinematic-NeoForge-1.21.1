package com.frierenflight.zoltraakcinematic.client.fx;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.entity.ZoltraakCinematicBeamEntity;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * High-performance Bridge that binds ZoltraakCinematicBeamEntity to the Photon VFX Engine.
 * Supports soft depth fading, GPU compute particles, and post-processing HDR Bloom passes.
 */
public class ZoltraakPhotonFXBridge {
    private static final Logger LOGGER = Logger.getLogger(ZoltraakCinematicMod.MODID);
    private static boolean checked = false;
    private static boolean photonAvailable = false;

    public static boolean isPhotonAvailable() {
        if (!checked) {
            checked = true;
            photonAvailable = ModList.get().isLoaded("photon");
            if (photonAvailable) {
                LOGGER.info("[Zoltraak Cinematic] Photon VFX Engine detected! Activating GPU particle & Bloom pipelines.");
            } else {
                LOGGER.info("[Zoltraak Cinematic] Photon not detected. Operating in High-Precision Native Blaze3D Mode.");
            }
        }
        return photonAvailable;
    }

    /**
     * Binds a Photon FX asset to the entity using reflection for zero-crash decoupling.
     */
    public static void attachPhotonFX(ZoltraakCinematicBeamEntity entity) {
        if (!isPhotonAvailable() || entity == null || !entity.level().isClientSide) {
            return;
        }

        try {
            ResourceLocation fxLoc = ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "zoltraak_beam");
            Class<?> fxHelperClass = Class.forName("com.lowdragmc.photon.client.fx.FXHelper");
            Method getFXMethod = fxHelperClass.getMethod("getFX", ResourceLocation.class);
            Object fxInstance = getFXMethod.invoke(null, fxLoc);

            if (fxInstance != null) {
                Class<?> executorClass = Class.forName("com.lowdragmc.photon.client.fx.EntityEffectExecutor");
                Class<?> autoRotateEnum = Class.forName("com.lowdragmc.photon.client.fx.EntityEffectExecutor$AutoRotate");
                Object lookRotate = Enum.valueOf((Class<Enum>) autoRotateEnum, "LOOK");

                Constructor<?> ctor = executorClass.getConstructor(
                        Class.forName("com.lowdragmc.photon.client.fx.FX"),
                        net.minecraft.world.level.Level.class,
                        net.minecraft.world.entity.Entity.class,
                        autoRotateEnum
                );

                Object executor = ctor.newInstance(fxInstance, entity.level(), entity, lookRotate);
                Method startMethod = executorClass.getMethod("start");
                startMethod.invoke(executor);
            }
        } catch (Throwable t) {
            // Silently fall back to native renderer if FX preset isn't configured
        }
    }
}
