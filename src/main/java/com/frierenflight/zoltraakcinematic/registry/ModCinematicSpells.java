package com.frierenflight.zoltraakcinematic.registry;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.spell.ZoltraakCinematicSpell;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCinematicSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, ZoltraakCinematicMod.MODID);

    public static final DeferredHolder<AbstractSpell, AbstractSpell> ZOLTRAAK =
            SPELLS.register("zoltraak", ZoltraakCinematicSpell::new);

    public static final DeferredHolder<AbstractSpell, AbstractSpell> CORRUPTED_ZOLTRAAK =
            SPELLS.register("corrupted_zoltraak", com.frierenflight.zoltraakcinematic.spell.CorruptedZoltraakSpell::new);

    public static final DeferredHolder<AbstractSpell, AbstractSpell> ZOLTRAAK_BARRAGE =
            SPELLS.register("zoltraak_barrage", com.frierenflight.zoltraakcinematic.spell.FernBarrageSpell::new);

    public static final DeferredHolder<AbstractSpell, AbstractSpell> CORRUPTED_BARRAGE =
            SPELLS.register("corrupted_barrage", com.frierenflight.zoltraakcinematic.spell.CorruptedBarrageSpell::new);

    public static final DeferredHolder<AbstractSpell, AbstractSpell> DEFENSE_MAGIC =
            SPELLS.register("defense", com.frierenflight.zoltraakcinematic.spell.DefensiveMagicSpell::new);

    public static final DeferredHolder<AbstractSpell, AbstractSpell> FLIGHT =
            SPELLS.register("flight", com.frierenflight.zoltraakcinematic.spell.FlightMagicSpell::new);

    public static void register(IEventBus eventBus) {
        SPELLS.register(eventBus);
    }
}
