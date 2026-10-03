package com.frierenflight.zoltraakcinematic.entity;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class GargantuaDamage {
    public static final ResourceKey<DamageType> GARGANTUA_KEY = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "gargantua")
    );

    public static DamageSource source(Level level, Entity directEntity, Entity causingEntity) {
        var opt = level.registryAccess().registry(Registries.DAMAGE_TYPE)
                .flatMap(r -> r.getHolder(GARGANTUA_KEY));
        if (opt.isPresent()) {
            return new DamageSource(opt.get(), directEntity, causingEntity);
        }
        return level.damageSources().magic();
    }

    private GargantuaDamage() {}
}
