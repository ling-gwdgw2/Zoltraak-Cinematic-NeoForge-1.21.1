package com.frierenflight.zoltraakcinematic.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * FlightMobEffect
 * Synced mob effect representing the active Flight Magic (飛行魔法) state.
 * Color: Glowing cyan mana (0x38D6F5).
 */
public class FlightMobEffect extends MobEffect {

    public FlightMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
