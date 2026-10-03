package com.frierenflight.zoltraakcinematic.spell;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.entity.GargantuaEntity;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSounds;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * 🌌 Gargantua — Supreme Endgame Black Hole Magic.
 * Manifests a supermassive Kerr black hole with general relativistic gravitational lensing,
 * volumetric accretion disk, photon rings, and cataclysmic event horizon collapse.
 */
public class GargantuaSpell extends AbstractSpell {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "gargantua");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(300.0)
            .setAllowCrafting(true)
            .build();

    public GargantuaSpell() {
        this.baseManaCost = 2200;
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 100;
        this.spellPowerPerLevel = 0;
        this.castTime = 60; // 3.0 seconds
    }

    @Override
    public ResourceLocation getSpellResource() {
        return ID;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
    }

    @Override
    public Optional<net.minecraft.sounds.SoundEvent> getCastStartSound() {
        return Optional.of(ModCinematicSounds.SINGULARITY_CHARGE.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.CHARGE_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return SpellAnimations.FINISH_ANIMATION;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            Vec3 eyePos = entity.getEyePosition();
            Vec3 lookVec = entity.getViewVector(1.0f);
            double range = 48.0d;
            Vec3 traceEnd = eyePos.add(lookVec.scale(range));

            HitResult hit = level.clip(new ClipContext(eyePos, traceEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            Vec3 targetPos;
            if (hit.getType() != HitResult.Type.MISS) {
                targetPos = hit.getLocation().add(0, 14.0d, 0);
            } else {
                targetPos = eyePos.add(lookVec.scale(28.0d));
            }

            // Spawn Gargantua singularity entity
            Vec3 spinAxis = new Vec3(0, 1, 0);
            GargantuaEntity blackHole = new GargantuaEntity(serverLevel, entity, targetPos, spinAxis);
            serverLevel.addFreshEntity(blackHole);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("spell.zoltraak_cinematic.gargantua.desc"),
                Component.translatable("ui.irons_spellbooks.radius", GargantuaEntity.PULL_RADIUS),
                Component.translatable("ui.irons_spellbooks.damage", 95.0f)
        );
    }
}
