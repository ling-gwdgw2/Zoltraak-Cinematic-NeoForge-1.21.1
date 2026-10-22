# Zoltraak: Cinematic Edition - Task Board and Hotfix Scratchpad (TODO)

This document serves as an immediate, fast-turnaround scratchpad for short-term engineering subtasks, emergency hotfixes, and tactical tweaks. Once tasks are completed and stabilized in a release, move them to CHANGELOG.md.

---

## 1. Emergency Hotfixes and Critical Stability [P0]

Urgent bug fixes, crash preventions, and high-impact gameplay regressions.

- [x] **Elimination of Volatile Singularity Reconstruction Engine**
  - Context: Removed in-memory `tornBlocks` map and batch restoration routines (`reconstructBlocks`, `restoreAllRemainingBlocks`) from `GargantuaEntity.java`.
  - Result: Eliminates server memory footprint, prevents permanent craters from ungraceful shutdowns, avoids synchronous chunk load loops during entity unload, and maintains 100% compatibility with land protection mods (FTB Chunks).

- [x] **Zoltraak Beam Griefing Purge & Land Claim Compatibility**
  - Context: Removed direct block destruction loop from `ZoltraakCinematicBeamEntity.java`.
  - Result: Beam now deals pure spell/kinetic blast damage without destroying blocks, preventing griefing in claimed bases or towns.

- [x] **Barrage Projectile Double-Stepping & Physics Fix**
  - Context: Resolved issue where `ZoltraakBarrageProjectileEntity` called `super.tick()` in `ThrowableProjectile`, causing double movement (2x speed), duplicate raycasts, and vanilla water bubble particles.
  - Result: Replaced with `super.baseTick()`, ensuring exact aerodynamic homing and single-pass supersonic movement.

- [x] **Flight Thread-Safety, Reconnection Safety & Zero-Particle Compliance**
  - Context: Converted `activeFlyers` and `landingProtection` to `ConcurrentHashMap.newKeySet()`, preserved landing protection across logout, added login airborne protection, and purged remaining vanilla particles (`ELECTRIC_SPARK`, `CLOUD`) in `FlightEvents.java`.

- [x] **Dead Code Cleanup in ZoltraakDamage**
  - Context: Removed unused `saved` variable from `ZoltraakDamage.java`.

- [ ] **Multiplayer Beam Anchor Sync on High Latency**
  - Context: Observe whether high network latency (>150ms) causes noticeable visual separation between the remote player's 3D staff tip and the origin of `ZoltraakCinematicBeamEntity`.
  - Target Files: `src/main/java/com/frierenflight/zoltraakcinematic/entity/ZoltraakCinematicBeamEntity.java`, `ZoltraakPhotonBeamRenderer.java`

- [ ] **Audio Channel Clipping on Supernova Detonation**
  - Context: Review `ModCinematicSounds.SINGULARITY_EXPLODE` (Volume 8.0) and `TINNITUS` (Volume 5.0) to ensure high sound volume does not distort or overwhelm OpenAL audio buffers on standard headphones.
  - Target Files: `src/main/java/com/frierenflight/zoltraakcinematic/entity/GargantuaEntity.java`, `sounds.json`

---

## 2. Immediate Short-Term Subtasks [P1]

High-priority tasks planned for the next micro-patch (v1.0.3).

- [ ] **Dual-Tier Configuration Engine Implementation (Common & Client)**
  - Context: Implement full configuration settings designed in `CONFIG.md` covering flight mana drain, beam properties, barrage aerodynamics, defense barriers, Qual boss scaling, relics, and client visual accessibility (screen flash toggles, camera shake, raymarching steps).
  - Target Files: `src/main/java/com/frierenflight/zoltraakcinematic/config/ZoltraakCinematicConfig.java`, `ZoltraakCinematicClientConfig.java`, `ZoltraakCinematicMod.java`, `CONFIG.md`

- [ ] **First-Cast Shader Warmup Routine**
  - Context: First cast of Zoltraak or Gargantua in a new game session can cause a 50ms micro-stutter while OpenGL compiles and links shaders on demand.
  - Action: Pre-bind and link all registered `ShaderInstance` objects in `ZoltraakCinematicClientEvents` during client setup stage.
  - Target Files: `src/main/java/com/frierenflight/zoltraakcinematic/client/ZoltraakCinematicClientEvents.java`

- [ ] **Configurable VFX Performance Presets**
  - Context: Provide client-side config options for players on integrated graphics (iGPUs) to reduce raymarching iterations.
  - Setting: `blackHoleRaymarchSteps`: `LOW (32)`, `MEDIUM (64)`, `ULTRA (112)` (Default: `64`).
  - Target Files: `src/main/java/com/frierenflight/zoltraakcinematic/config/ZoltraakCinematicConfig.java`, `gargantua.fsh`

- [ ] **Boss Recovery Window Pacing Playtest**
  - Context: Field-test the newly implemented 10-12s (200-240 ticks) recovery window after Qual's 4-second Corrupted Barrage in `QualCombatGoal.java`.
  - Check: Ensure Qual stays within reachable combat distance without flying uncontrollably out of player melee range during the recovery window.
  - Target Files: `src/main/java/com/frierenflight/zoltraakcinematic/entity/boss/ai/QualCombatGoal.java`

---

## 3. Quick Polish and Minor Tweaks [P2]

Quality-of-life adjustments, localization audits, and visual refinements.

- [ ] **Localization Completeness Check (EN / TH)**
  - Context: Ensure newly added items and death messages have full translations in both `en_us.json` and `th_th.json`:
    - `item.zoltraak_cinematic.chronos_dilatation_watch`
    - `death.attack.zoltraak_cinematic.gargantua`
    - `death.attack.zoltraak_cinematic.corrupted_zoltraak`
  - Target Files: `src/main/resources/assets/zoltraak_cinematic/lang/en_us.json`, `th_th.json`

- [ ] **Curios Tag Validation for Chronos Dilatation Watch**
  - Context: Validate that `chronos_dilatation_watch` can be equipped seamlessly into the Curios `charm` slot without inventory ghosting.
  - Target Files: `src/main/resources/data/curios/tags/item/charm.json`

- [ ] **Recipe Balancing Audit**
  - Context: Verify that `ordinary_grimoire.json` and `frieren_staff.json` recipes are recognized properly in JEI / EMI / REI crafting lookups.
  - Target Files: `src/main/resources/data/zoltraak_cinematic/recipe/*`

---

## 4. Scratchpad and Debug Notes

Quick notes, command shortcuts, and temporary profiling snippets.

### Quick Testing Commands
```text
/summon zoltraak_cinematic:qual_boss ~ ~ ~
/singularity ~ ~14 ~
/effect give @p irons_spellbooks:instant_mana 1 255
/give @p zoltraak_cinematic:frieren_staff
/give @p zoltraak_cinematic:chronos_dilatation_watch
```

### Known Gotchas
* When modifying shaders in `assets/zoltraak_cinematic/shaders/core/`, run `F3 + T` (Reload Resource Packs) to reload shader pipelines without restarting the client.
* When compiling with `python build.py`, ensure test Minecraft instances are closed if file lock errors occur on the target mod JAR.
* Ensure JDK 21 environment variable (`JAVA_HOME`) points to Eclipse Adoptium / Temurin 21.

---

## 5. Recently Completed and Verified [Archived]

- [x] **v1.0.2 - Defense Barrier Crash Fix**: Resolved `java.lang.IllegalStateException: Not building!` in `DefenseBarrierRenderer.java` and `QualBossRenderer.java` by separating interleaved `DEFENSE_SURFACE` and `LIGHT` buffer accesses into clean sequential render passes.
- [x] **v1.0.2 - Boss Pacing & Recovery Window**: Added 10-12s cooldown after 4s Barrage in `QualCombatGoal.java`.
- [x] **v1.0.2 - Visual Cleanup**: Removed `renderHelicalManaTendrils`, ground circle, and chest flare; added `renderSubtleDarkMiasma`.
- [x] **v1.0.2 - Vanilla Purge**: Purged vanilla audio and particles from unsealing stone and boss entity.
- [x] **v1.0.2 - Tooling Cleanup**: Updated `build.py` to auto-remove outdated 1.0.0 JARs from CurseForge test instances.
- [x] **v1.0.1 - Astral Singularity Balance**: Capped Gargantua %Max HP damage curve and integrated Spell Power and `BLACK_HOLE_MAGIC_RESIST`.
- [x] **v1.0.1 - I-Frame Fix**: Resolved teammate attack lockouts during rapid barrage hits in `ZoltraakDamage.java`.
