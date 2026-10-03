package com.frierenflight.zoltraakcinematic.command;

import com.frierenflight.zoltraakcinematic.entity.GargantuaEntity;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class GargantuaCommands {

    public static void register() {
        NeoForge.EVENT_BUS.addListener(GargantuaCommands::onRegisterCommands);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("singularity")
                .requires(source -> source.hasPermission(2))
                .executes(context -> spawnGargantua(context.getSource())));

        dispatcher.register(Commands.literal("zoltraak")
                .then(Commands.literal("singularity")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> spawnGargantua(context.getSource()))));

        dispatcher.register(Commands.literal("gargantua")
                .requires(source -> source.hasPermission(2))
                .executes(context -> spawnGargantua(context.getSource())));

        dispatcher.register(Commands.literal("zoltraak")
                .then(Commands.literal("gargantua")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> spawnGargantua(context.getSource()))));
    }

    private static int spawnGargantua(CommandSourceStack source) {
        if (!(source.getLevel() instanceof ServerLevel serverLevel)) return 0;

        Vec3 pos;
        LivingEntity caster = null;
        if (source.getEntity() instanceof ServerPlayer player) {
            caster = player;
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getViewVector(1.0f);
            pos = eye.add(look.scale(24.0d));
        } else {
            pos = source.getPosition().add(0, 5, 0);
        }

        float spellPower = 500.0f;
        if (caster != null) {
            var schoolHolder = com.frierenflight.zoltraakcinematic.registry.ModCinematicSchools.BLACK_HOLE;
            if (schoolHolder != null && schoolHolder.isBound()) {
                spellPower = (float) (500.0 * schoolHolder.get().getPowerFor(caster));
            }
        }

        GargantuaEntity blackHole = new GargantuaEntity(serverLevel, caster, pos, new Vec3(0, 1, 0), spellPower);
        serverLevel.addFreshEntity(blackHole);

        source.sendSuccess(() -> Component.literal("§d[Zoltraak Cinematic] §fManifested Astral Singularity!"), true);
        return 1;
    }
}
