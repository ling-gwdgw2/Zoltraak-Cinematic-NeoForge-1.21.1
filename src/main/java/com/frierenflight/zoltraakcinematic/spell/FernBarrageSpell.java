package com.frierenflight.zoltraakcinematic.spell;

import com.frierenflight.zoltraakcinematic.ZoltraakCinematicMod;
import com.frierenflight.zoltraakcinematic.entity.ZoltraakBarrageProjectileEntity;
import com.frierenflight.zoltraakcinematic.registry.ModCinematicSounds;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Fern's Grand Phalanx Barrage (Ordinary Defensive Magic - Demon Slaying).
 * Manifests an imposing 24-circle celestial matrix, unleashing 4 synchronized volleys
 * of 24 converging Zoltraak light lances (96 total) at the target point.
 */
public class FernBarrageSpell extends AbstractSpell {
    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(ZoltraakCinematicMod.MODID, "zoltraak_barrage");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(com.frierenflight.zoltraakcinematic.registry.ModCinematicSchools.ORDINARY_MAGIC_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(1.5)
            .build();

    public static final int BARRAGE_CIRCLE_COUNT = 24;
    private static final String NBT_SALVO_KEY = "ZoltraakSalvos";

    // 24-Circle Grand Celestial Array (Exact 5-Row Staggered Matrix matching screenshot)
    public static final Vec3[] BARRAGE_CIRCLE_OFFSETS = new Vec3[] {
        // Row 1: Topmost Sky (5 Circles, Y = +1.90m)
        new Vec3(-3.20,  1.90,  0.20), // 0: Far Left
        new Vec3(-1.60,  1.90,  0.20), // 1: Mid Left
        new Vec3( 0.00,  1.95,  0.20), // 2: Center Crown Overhead
        new Vec3( 1.60,  1.90,  0.20), // 3: Mid Right
        new Vec3( 3.20,  1.90,  0.20), // 4: Far Right

        // Row 2: Upper-Mid (4 Circles, Y = +1.20m)
        new Vec3(-2.40,  1.20,  0.25), // 5: Upper-Mid Outer Left
        new Vec3(-0.90,  1.20,  0.25), // 6: Upper-Mid Inner Left
        new Vec3( 0.90,  1.20,  0.25), // 7: Upper-Mid Inner Right
        new Vec3( 2.40,  1.20,  0.25), // 8: Upper-Mid Outer Right

        // Row 3: Mid / Chest Level (6 Circles flanking player, Y = +0.45m)
        new Vec3(-3.30,  0.45,  0.25), // 9: Outer Left
        new Vec3(-2.15,  0.45,  0.30), // 10: Mid Left
        new Vec3(-1.00,  0.45,  0.30), // 11: Inner Left (beside shoulder)
        new Vec3( 1.00,  0.45,  0.30), // 12: Inner Right (beside staff)
        new Vec3( 2.15,  0.45,  0.30), // 13: Mid Right
        new Vec3( 3.30,  0.45,  0.25), // 14: Outer Right

        // Row 4: Lower-Mid / Waist Level (4 Circles, Y = -0.25m)
        new Vec3(-2.50, -0.25,  0.25), // 15: Lower Outer Left
        new Vec3(-1.25, -0.25,  0.25), // 16: Lower Inner Left
        new Vec3( 1.25, -0.25,  0.25), // 17: Lower Inner Right
        new Vec3( 2.50, -0.25,  0.25), // 18: Lower Outer Right

        // Row 5: Bottom Ground Level (5 Circles, Y = -0.85m - safely elevated above terrain)
        new Vec3(-3.00, -0.85,  0.20), // 19: Bottom Outer Left
        new Vec3(-1.50, -0.85,  0.20), // 20: Bottom Inner Left
        new Vec3( 0.00, -0.85,  0.20), // 21: Bottom Center (above hotbar)
        new Vec3( 1.50, -0.85,  0.20), // 22: Bottom Inner Right
        new Vec3( 3.00, -0.85,  0.20)  // 23: Bottom Outer Right
    };

    public static final float[] BARRAGE_CIRCLE_SCALES = new float[] {
        // Row 1 (5 Circles)
        0.40f, 0.42f, 0.44f, 0.42f, 0.40f,
        // Row 2 (4 Circles)
        0.40f, 0.42f, 0.42f, 0.40f,
        // Row 3 (6 Circles)
        0.40f, 0.42f, 0.42f, 0.42f, 0.42f, 0.40f,
        // Row 4 (4 Circles)
        0.40f, 0.40f, 0.40f, 0.40f,
        // Row 5 (5 Circles)
        0.38f, 0.40f, 0.42f, 0.40f, 0.38f
    };

    public FernBarrageSpell() {
        this.baseManaCost = 28;
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 36;
        this.spellPowerPerLevel = 7;
        this.castTime = 60; // 3 seconds continuous channel
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public Optional<net.minecraft.sounds.SoundEvent> getCastStartSound() {
        return Optional.of(ModCinematicSounds.ZOLTRAAK_CHARGE.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ONE_HANDED_RAY_SHOOT;
    }

    protected int getColorTheme() {
        return 0; // 0 = Demon Slaying / Holy White-Cyan
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide) {
            entity.getPersistentData().putInt(NBT_SALVO_KEY, 0);
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);

        int total = Math.max(20, playerMagicData.getCastDuration());
        int remaining = playerMagicData.getCastDurationRemaining();
        int elapsed = total - remaining;
        int salvosFired = entity.getPersistentData().getInt(NBT_SALVO_KEY);

        // 4 deterministic salvos evenly spaced across channeling duration:
        // Salvo 1: at ~22% duration (after opening blossom)
        // Salvo 2: at ~46% duration
        // Salvo 3: at ~70% duration
        // Salvo 4: at ~92% duration (right before channel end)
        int targetSalvo = 0;
        float progress = (float) elapsed / (float) total;
        if (progress >= 0.92f) {
            targetSalvo = 4;
        } else if (progress >= 0.70f) {
            targetSalvo = 3;
        } else if (progress >= 0.46f) {
            targetSalvo = 2;
        } else if (progress >= 0.22f) {
            targetSalvo = 1;
        }

        if (targetSalvo > salvosFired) {
            entity.getPersistentData().putInt(NBT_SALVO_KEY, targetSalvo);
            spawnSimultaneousVolley(level, spellLevel, entity, playerMagicData, getColorTheme());
        }
    }

    public static LivingEntity findAutoTarget(Level level, LivingEntity caster, double maxRange) {
        if (caster == null || level == null) return null;

        Vec3 eyePos = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();

        net.minecraft.world.phys.AABB searchBox = caster.getBoundingBox().inflate(maxRange);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox, target -> {
            if (target == caster || !target.isAlive()) return false;
            if (target instanceof net.minecraft.world.entity.decoration.ArmorStand) return false;
            if (io.redspace.ironsspellbooks.damage.DamageSources.isFriendlyFireBetween(caster, target)) return false;
            if (target instanceof net.minecraft.world.entity.player.Player player) {
                if (player.isCreative() || player.isSpectator()) return false;
            }
            return true;
        });

        LivingEntity bestTarget = null;
        double bestScore = -1.0;

        for (LivingEntity candidate : candidates) {
            Vec3 toTarget = candidate.getBoundingBox().getCenter().subtract(eyePos);
            double dist = toTarget.length();
            if (dist > maxRange || dist < 0.5) continue;

            Vec3 dirToTarget = toTarget.normalize();
            double dot = look.dot(dirToTarget);

            // Wide forward acquisition cone (~78 degrees from look direction)
            if (dot < 0.20) continue;

            double score = (dot * 2.5) + (1.0 - (dist / maxRange));

            // Line of sight check to prioritize unoccluded targets
            HitResult hit = level.clip(new ClipContext(
                    eyePos, candidate.getEyePosition(),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    caster
            ));
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                score *= 0.4;
            }

            if (score > bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }

        return bestTarget;
    }

    protected void spawnSimultaneousVolley(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, int colorTheme) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        Vec3 eyePos = entity.getEyePosition();
        Vec3 look = entity.getLookAngle();
        LivingEntity lockedTarget = findAutoTarget(level, entity, 56.0);

        Vec3 targetPos;
        if (lockedTarget != null) {
            targetPos = lockedTarget.getBoundingBox().getCenter();
        } else {
            targetPos = eyePos.add(look.scale(56.0));
            // Crash-proof block collision check along aim vector
            HitResult blockHit = level.clip(new ClipContext(
                    eyePos, targetPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    entity
            ));
            if (blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
                targetPos = blockHit.getLocation();
            }
        }

        Vec3 flatLook = Vec3.directionFromRotation(0, entity.getYRot());
        Vec3 right = Vec3.directionFromRotation(0, entity.getYRot() + 90);
        Vec3 worldUp = new Vec3(0, 1, 0);

        float spellPower = getSpellPower(spellLevel, entity);
        float bulletDamage = Math.max(1.8f, spellPower / 14.0f);

        // Spawn all 24 projectiles simultaneously from their individual 3D circles
        for (int i = 0; i < BARRAGE_CIRCLE_COUNT; i++) {
            Vec3 localOffset = BARRAGE_CIRCLE_OFFSETS[i];
            Vec3 spawnPos = eyePos
                    .add(right.scale(localOffset.x))
                    .add(worldUp.scale(localOffset.y))
                    .add(flatLook.scale(localOffset.z));

            // Micro polar spread around the convergence focal point
            float spreadRadius = 0.006f;
            float theta = entity.getRandom().nextFloat() * ((float) Math.PI * 2.0f);
            float r = (float) Math.sqrt(entity.getRandom().nextFloat()) * spreadRadius;

            Vec3 deviatedTarget = targetPos
                    .add(right.scale(r * 48.0 * Math.cos(theta)))
                    .add(worldUp.scale(r * 48.0 * Math.sin(theta)));

            Vec3 finalDir = deviatedTarget.subtract(spawnPos).normalize();

            ZoltraakBarrageProjectileEntity bullet = new ZoltraakBarrageProjectileEntity(serverLevel, entity, bulletDamage, colorTheme);
            bullet.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            bullet.setSpawnOrigin(spawnPos);
            bullet.setCircleIndex(i);
            bullet.setTargetEntity(lockedTarget); // Guided homing lock-on!
            bullet.setDeltaMovement(finalDir.scale(3.2)); // 64 m/s hyper-velocity

            serverLevel.addFreshEntity(bullet);
        }

        // Layered acoustic thunderclap for simultaneous 24-bullet discharge
        boolean isPurple = colorTheme == 1;
        float basePitch = isPurple ? 0.76f : 1.0f;
        level.playSound(null, eyePos.x, eyePos.y, eyePos.z,
                ModCinematicSounds.ZOLTRAAK_FIRE.get(), SoundSource.PLAYERS, 1.4f, basePitch * 0.94f);
        level.playSound(null, eyePos.x, eyePos.y, eyePos.z,
                ModCinematicSounds.ZOLTRAAK_FIRE.get(), SoundSource.PLAYERS, 1.0f, basePitch * 1.28f);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float bulletDmg = Math.max(1.8f, getSpellPower(spellLevel, caster) / 14.0f);
        float salvoDmg = bulletDmg * 24.0f;
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(salvoDmg, 1) + " (" + Utils.stringTruncation(bulletDmg, 1) + "x24)"),
                Component.translatable("spell.zoltraak_cinematic.barrage_rate", "4x24 Salvos (96 Total)"),
                Component.translatable("spell.zoltraak_cinematic.fern_barrage.perk")
        );
    }
}
