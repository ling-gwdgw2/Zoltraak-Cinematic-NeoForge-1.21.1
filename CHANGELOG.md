# Changelog — Zoltraak: Cinematic Edition

All notable changes to the **Zoltraak: Cinematic Edition** mod project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.4] - 2026-10-23

### 100% Clean-Room Overhaul & 1:1 Anime Visual Fidelity (ยกเครื่องโค้ดและเชดเดอร์ใหม่ทั้งหมด 100% Original)

#### Clean Room Codebase & Asset Independence (การเขียนทดแทนใหม่ทั้งหมด 100% Original)
- **100% Original Shaders & Configurations Written from Scratch**:
  - `zoltraak_beam.fsh`, `.vsh`, `.json`: Completely rewritten with original mathematical formulation featuring a super-Gaussian white plasma core ($e^{-(y/\text{core})^4}$), supersonic longitudinal ionization waves, and aerodynamic arrowhead tapering.
  - `black_zoltraak_beam.fsh`, `.vsh`, `.json`: Completely rewritten with pure obsidian void core, crackling ash-lightning filaments, and royal violet / silver-lavender boundary ionization.
  - `great_zoltraak_white.fsh`, `.vsh`, `.json`: Completely rewritten featuring 3D cylindrical volumetric raymarching, Fresnel facing modulation, and catastrophic plasma discharge.
  - `great_zoltraak_black.fsh`, `.vsh`, `.json`: Completely rewritten featuring 3D cylindrical volumetric void raymarching, charcoal-violet soot turbulence, and silver-lavender rim discharge.
  - `great_zoltraak_plume.fsh`, `.vsh`, `.json`: Completely rewritten with lateral fluid plume wave dynamics, dual-tone light/dark mode support, and dielectric spark breaking.
  - `corona.fsh`, `.vsh`, `.json`: Completely rewritten from scratch as an original Demonic Gravitational Singularity Accretion Corona for Qual's cataclysmic singularity phase, featuring logarithmic inflow spirals, event horizon absorption cores, and outward coronal plasma streamers.
- **100% Original Synthesized Audio Effects (`zoltraak_*.ogg`)**:
  - `zoltraak_charge.ogg`: 100% mathematically synthesized stereo rising mana swell with exponential sub-bass pitch glide (55Hz -> 140Hz), vibrato-modulated harmonic sweeps, and sparkling mana shimmer.
  - `zoltraak_fire.ogg`: Synthesized supersonic laser snap transient, sub-bass 110Hz punch, beating 520Hz/525Hz photonic drone, and turbulent plasma discharge hiss.
  - `zoltraak_great_fire.ogg`: Cataclysmic seismic 38Hz sub rumble, twin detuned saw/sine energy roar (62Hz & 65.5Hz), and cavernous stereo reverb wash.
  - `zoltraak_impact.ogg`: Kinetic matter-fracture pop, 35Hz crater shockwave rumble, and debris crackle burst.
- **Custom-Crafted Spell Icons & GUI Textures**:
  - `defense.png`: Brand-new custom 64x64 spell icon depicting a glowing cyan hexagonal mana barrier with golden runic border, inset mana crystals, and smooth alpha transparency.
- **Dead Shaders & Texture Atlases Purged**:
  - Permanently deleted unused legacy shaders: `black_zoltraak_head.fsh`, `black_zoltraak_head.json`, `lens.fsh`, and `lens.json`.
  - Permanently deleted `zoltraak_vfx_atlas.png` and severed all references across `ZoltraakRenderTypes.java`, `ZoltraakPhotonBeamRenderer.java`, and `SceneLens.java`.
  - Replaced `zoltraak_glow.png` with a mathematically synthesized procedural HDR Gaussian-quartic radial bloom texture generated via clean-room mathematical script.
  - Replaced `zoltraak_magic_circle.png` with original 2048x2048 high-definition artwork with clean luminous alpha channel.
- **Complete Autonomy & Independence**:
  - The entire repository now consists solely of 100% original code, shaders, configurations, audio effects, and textures, with zero borrowed assets.

#### Added
- **Dedicated Supersonic Rapid Bullet Shaders (`rapid_zoltraak_bullet.json`, `.vsh`, `.fsh`)**:
  - Implemented high-velocity aerodynamic laser-needle bullets with super-Gaussian white plasma core and aerodynamic diamond arrowhead taper ($x \in [0.84, 1.0]$).
  - Multi-tone rendering for Demon-Slaying Zoltraak (Fern: Cyan/White) and Human-Killing Zoltraak (Qual: Obsidian/Violet).
- **High-Definition Anime Magic Circle Asset**:
  - Switched magic circle to high-definition 2048x2048 artwork (`Gemini_Generated_Image_bpll5mbpll5mbpll.png` / `zoltraak_magic_circle.png`).
  - Removed AI watermarks and cleanly extracted luminous alpha transparency for seamless in-game blending.

#### Changed & Refactored
- **Single Magic Circle Simplification (ลดเหลือวงเวทย์ชั้นเดียวตามอนิเมะ)**:
  - Removed the secondary coaxial magic circle layer in `ZoltraakRenderer.java`, `BlackZoltraakRenderer.java`, and `GreatZoltraakRenderer.java`, keeping only a single clean magic circle matching the anime.
- **Center Muzzle Glare & Diffraction Spike Timing (แก้แสงแฟลชจ้าบดบังลายวงเวทย์ตรงกลาง)**:
  - Restricted the massive horizontal and vertical 4.0x lens flare spikes in `ZoltraakRenderer.java` and `BlackZoltraakRenderer.java` to only trigger upon beam discharge (`shot >= 0.0F`).
  - During charging (`shot < 0.0F`), replaced blinding glare with a soft, subtle core glint, preserving full visibility of the intricate eye/flower geometry in the magic circle's center.
- **Anime-Accurate Borehole Block Piercing & Hit Detection (ระบบตรวจจับการกระทบและการเจาะทะลุ)**:
  - Overhauled continuous swept-cylinder collision detection in `ZoltraakCinematicBeamEntity.java`, preventing beams from tunneling through terrain without interaction.
  - Implemented cylindrical borehole terrain penetration matching the anime: blocks along the beam path are drilled and pulverized into fine debris without spawning entity items, strictly respecting `mobGriefing` and stop conditions on bedrock or defensive barrier magic.

#### Removed & Fixed
- **Floating Perimeter Ribbon Ring Purged (ลบขอบริบบิ้นลอยสีเขียว/ม่วง)**:
  - Completely purged the 18-polygon perimeter ember loop from `ZoltraakRenderer.java` and `BlackZoltraakRenderer.java`, removing the disconnected green/purple polygonal wireframe ring that hovered above the magic circle plane.
- **Moiré Grating & Interference Ripple Elimination (แก้ลายคลื่นริ้วบนวงเวทย์สีม่วง)**:
  - Purged spatial sinusoidal frequency grating (`sin(theta * 14.0 + r * 26.0 - t * 5.0)`) from `black_magic_circle.fsh` and `magic_circle.fsh`, replacing it with smooth temporal pulsation to eliminate aliasing, zebra stripes, and moiré patterns.
  - Replaced stepped color clamps with smooth cubic color ramps (`smoothstep`) across dark obsidian violet, royal purple, and luminous silver-lavender.
  - Removed synthetic blurry outer rings (`outerRing`) and procedural center needle crosses that clashed with the texture's native mandala.
- **Non-Canon Impact & Muzzle Rings Purged (ลบวงแหวนคลื่นกระแทกที่ไม่ตรงกับอนิเมะ)**:
  - Completely removed expanding spherical donut rings (`ringWave` / `abs(r - waveR)`) from `zoltraak_impact.fsh` and muzzle rings in `ZoltraakBarrageArrayRenderer.java`. Impact now displays cel-shaded billowing dust clouds, plasma bursts, and shattered dielectric sparks.
  - Fixed color corruption on impact surfaces from magenta to pure cyan or demonic obsidian/purple.

---

## [1.0.3] - 2026-10-22

### Dual-Tier Configuration Engine & Critical Stability Overhaul

#### Added
- **Dual-Tier Configuration Engine (`zoltraak_cinematic-common.toml` & `zoltraak_cinematic-client.toml`)**:
  - Implemented client-server decoupled configuration architecture (`ZoltraakCinematicConfig.java` & `ZoltraakCinematicClientConfig.java`).
  - **Server / Gameplay Settings (`zoltraak_cinematic-common.toml`)**:
    - `[flight_system]`: Base flight mana drain (`50.0/s`), fast flight drain multiplier (`1.6x`), max vertical velocity, airborne reconnect grace period (`60 ticks`), and action bar telemetry toggle.
    - `[spells_and_beams]`: Standard and Large beam range (`64.0` / `96.0` blocks), kinetic spell damage multipliers, and shield disabling on direct hits.
    - `[barrage_matrix]`: Supersonic projectile aerodynamic homing strength (`0.08`), projectile velocity (`2.8`), and matrix salvo fire intervals.
    - `[defense_barrier]`: Geodesic shield mana capacity multiplier (`1.0x`) and base lifetime duration.
    - `[qual_boss]`: Boss max health scaling (`600.0 HP`), attack delay intervals, and recovery window duration after barrage volleys.
    - `[artifacts_and_relics]`: Relic stats, casting speed buffs, and ring effect scalers.
    - `[gargantua]`: Gravitational pull radius (`100.0` blocks), inward acceleration, event horizon tidal damage percent (`8%/tick`), supernova blast damage (`800.0`), and blast radius (`32.0` blocks).
  - **Client Accessibility & GPU Tuning (`zoltraak_cinematic-client.toml`)**:
    - **Photosensitive Flash Protection**: Full toggle (`enableScreenFlash`) and intensity slider (`screenFlashIntensity`, `0.0` – `1.0`).
    - **Camera Shake Controls**: Global camera shake toggle (`enableCameraShake`) and scale multiplier (`cameraShakeMultiplier`) for Zoltraak recoil, rapid barrage rumble, and black hole gravitational tremor.
    - **Tinnitus Audio Mute**: Dedicated toggle (`enableTinnitusSound`) to silence high-frequency ringing audio post-supernova.
    - **Aerodynamic Flight Roll**: Toggle (`enableFlightCameraRoll`) for 3D banking camera roll during flight.
    - **GPU Shader Optimization**: Configurable raymarching step limits (`blackHoleRaymarchSteps`: 32 to 128) and toggles for anamorphic lens flare quad rendering (`enableAnamorphicLensFlares`) and gravitational lensing background distortion (`enableGravitationalLensing`).

#### Fixed & Stabilized
- **Volatile Singularity Reconstruction Purge (Memory & Crater Fix)**:
  - Completely removed the unstable in-memory block tracking map (`tornBlocks`) and batch restoration routines (`reconstructBlocks`, `restoreAllRemainingBlocks`) in `GargantuaEntity.java`.
  - Prevents severe RAM spikes, eliminates chunk loading deadlock loops during entity unload, prevents permanent craters from ungraceful server stops, and ensures 100% compatibility with land protection mods (e.g. FTB Chunks).
- **Anti-Griefing Zoltraak Beam**:
  - Purged direct block destruction logic from `ZoltraakCinematicBeamEntity.java`. The beam now delivers cinematic kinetic and spell damage without carving holes through claimed player bases or server terrain.
- **Barrage Double-Stepping & Physics Alignment**:
  - Fixed projectile double movement in `ZoltraakBarrageProjectileEntity.java` by replacing `super.tick()` with `super.baseTick()`, eliminating inadvertent 2x speed and unwanted vanilla water bubble particles.
- **Flight Thread-Safety & Login Airborne Protection**:
  - Converted `activeFlyers` and `landingProtection` tracking sets in `FlightEvents.java` to `ConcurrentHashMap.newKeySet()`.
  - Added seamless flight grace period on player login/reconnection and purged legacy vanilla sprite particles (`ELECTRIC_SPARK`, `CLOUD`).
- **Defense Barrier Translucent Depth Sorting**:
  - Refactored `DefenseBarrierRenderer.java` to perform multi-pass isolated vertex buffer flushes between surface polygon faces (`DEFENSE_SURFACE`), glowing borders (`LIGHT`), and mana deflection impacts (`ZOL_IMPACT`), eliminating translucent Z-buffer interleaving.

---

## [1.0.2] - 2026-10-07

### Boss Visual Overhaul & Pacing Refinement

#### Added
- **Subtle Dark Miasma VFX (`renderSubtleDarkMiasma`)**:
  - Implemented an elegant, floating dark mana particle effect in `QualBossRenderer.java`.
  - Emits soft, translucent purple-black motes (`r=110, g=15, b=160, alpha=75..110`) that gently drift upwards around Qual.
  - Designed with pure mathematical procedural GLSL shaders that remain aesthetic and airy without obscuring the boss character model, horns, or robes.

#### Changed & Balanced
- **Pacing & Recovery Window in `QualCombatGoal.java`**:
  - **Mandatory Cooldown Window**: Added a dedicated **10.0 – 12.0 second (200–240 ticks)** recovery cooldown (`attackTimer = 200 + random(40)`) after Qual completes her Corrupted Barrage channel. Qual idly hovers during this window, providing players a fair and rewarding opportunity to counter-attack.
  - **Single Beam Cadence**: Tuned single-beam Zoltraak delays to **3.5 – 5.0 seconds (70–100 ticks)** across Phase 1, Phase 2, and Phase 3 to eliminate beam spamming.
- **Boss Mana Economy in `QualBossEntity.java`**:
  - Rebalanced `AttributeRegistry.MANA_REGEN` from `60.0/s` down to **`20.0/s`**, ensuring mana is a finite tactical resource that drains during volleys and regenerates predictably.
- **Corrupted Barrage Duration in `CorruptedBarrageSpell.java`**:
  - Hard-capped continuous channel duration for Qual's Corrupted Barrage to exactly **4.0 seconds (80 ticks)** (down from 10.0 seconds).
- **Ancient Unsealing Ritual Aesthetic in `QualSealingStoneRenderer.java`**:
  - Replaced the generic orange fire/flame palette with authentic **Celestial Cyan (`0.25, 0.75, 1.0`)** representing Frieren's ancient seal, which shifts and shatters into **Demonic Zoltraak Violet (`0.85, 0.15, 0.95`)**.

#### Removed
- **Thick Helical Tendrils**: Removed `renderHelicalManaTendrils` from `QualBossRenderer.java` 100%, completely eliminating the cluttered 3–4 thick pink spiral ribbons that previously obscured the character model.
- **Ground Magic Circle**: Removed `renderGroundCircle` beneath Qual's feet in `QualBossRenderer.java`, keeping the arena floor clean and uncluttered.
- **Oversized Coronal Flare**: Removed the large 1.35m chest flare quad (`renderCoronalFlares`) from `QualBossRenderer.java` to prevent blinding lens flares over the torso.

#### Fixed
- **Vanilla Audio & Particle Purge**:
  - Completely purged lingering Vanilla sound events (`SoundEvents.WITHER_AMBIENT`, `SoundEvents.AMETHYST_BLOCK_HIT`) from `QualBossEntity.java`, replacing them with custom mod sounds (`ModCinematicSounds.QUAL_AMBIENT`, `ModCinematicSounds.ZOLTRAAK_IMPACT`).
  - Purged Vanilla particle emitters (`ParticleTypes.FLAME`, `LAVA`, `SMOKE`, `EXPLOSION_EMITTER`) and sound events from `QualSealingStoneBlockEntity.java`.
  - Fixed a missing sound reference (`SEAL_CRACK`) by binding to `ModCinematicSounds.SINGULARITY_CHARGE`.

#### Tooling & Deployment
- Updated `neoforge.mods.toml`, `build.py`, `README.md`, and `ARCHITECTURE.md` to version `1.0.2`.
- `build.py` now automatically deletes older `1.0.0` JAR files from target CurseForge mod directories upon deployment to prevent duplicate mod crashes.

---

## [1.0.1] - 2026-10-04

### Astral Singularity & Combat Balance

#### Added
- **Astral Singularity (Gargantua Black Hole)**:
  - Added the apocalyptic rotating Kerr black hole ultimate spell (`GargantuaEntity.java`), triggered during Qual's Phase 4 (critical HP < 20%) and available as a master-tier spell.
  - Implemented 4th-order geodesic raymarching, Doppler-beamed volumetric accretion disk, inward gravitational pull across 100 blocks, screen shake (`GargantuaShake.java`), and terminal supernova detonation.
- **Chronos Dilatation Watch**:
  - Added a Curios charm accessory (`ChronosDilatationWatchItem.java`) allowing relativistic manipulation of spacetime and casting speed.
  - Included dedicated pixel-art textures, model definitions, and crafting recipes.

#### Fixed & Balanced
- **Gargantua Damage Curve (`fix.md`)**:
  - Fixed an issue where the event horizon dealt an overpowered 20% Max HP damage every 0.4 seconds, which previously killed any entity in 2 seconds regardless of health pool.
  - Added proper `Spell Power` scaling to the accretion disk damage calculation.
  - Factored in the custom `BLACK_HOLE_MAGIC_RESIST` attribute before final damage application.
  - Scaled the swallowed matter bonus (`swallowedBonus`) dynamically rather than capping at a rigid 200 damage.
- **I-Frame Bypass Bug in `ZoltraakDamage.java`**:
  - Fixed an issue where rapid barrage attacks set targets' `invulnerableTime` to 20 ticks, unintentionally making targets immune to all attacks and spells from teammates.
- **Shader Stability & GPU Safety**:
  - Added `safeNormalize` to the defense barrier shader to prevent GPU division-by-zero crashes.
  - Hardened shaders against NaN values and non-uniform derivatives.
  - Eliminated square bounding box clipping artifacts on optical lens flares.
- **Recipe Fixes**:
  - Corrected an invalid item reference (`arcane_salvage` → `irons_spellbooks:arcane_rune`) in `ordinary_grimoire.json`.
  - Rebalanced the recipes for Frieren's Staff and the Ordinary Grimoire to post-Qual endgame tiers.

---

## [1.0.0] - 2026-09-28

### Initial Cinematic Release

#### Added
- **Core Architecture & Shaders**:
  - Standalone Core GLSL 150 shader pipeline (`black_magic_circle.fsh`, `cinematic_flare.fsh`, `corona.fsh`, `defense_barrier.fsh`, `zoltraak_beam.fsh`).
  - True HDR bloom and soft depth fade without requiring external dependencies (Photon / KilaGraph).
- **Offensive Magic (Zoltraak)**:
  - **Zoltraak: Frieren Blast**: Hypersonic photonic beam with a 12-block terminal shockwave dome.
  - **Corrupted Zoltraak: Qual**: 100% armor-piercing void beam with Wither effect.
  - **Great Zoltraak**: High-yield apocalyptic beam with screen shake and chromatic dispersion.
  - **Zoltraak: Grand Phalanx Barrage**: Fern's 24-circle celestial array firing continuous guided Zoltraak light lances.
- **Defensive Magic**:
  - Geodesic honeycomb barrier dome and directional aegis shields with procedural hexagonal lattice resonance and damage absorption.
- **Human Flight Magic**:
  - Omnidirectional 3D flight with aerodynamic roll/pitch banking and stamina/mana mechanics.
- **Boss Encounter: Qual, The Elder Sage of Corruption**:
  - 4-phase combat progression state machine (100% → 75% → 40% → 20% → 0%).
  - Multi-player scaling (+200 HP per challenger) and tactical target cycling.
  - Custom GeckoLib 3D entity model, textures, and combat animations.
- **Frieren's Ancient Sealing Monolith**:
  - 5-second unsealing ritual block (`QualSealingStoneBlock`) and procedural block entity renderer (`QualSealingStoneRenderer`).
- **Items & Equipment**:
  - Frieren's Staff with custom 3D model and spell scaling.
  - Ordinary Grimoire with custom school attributes (`Zoltraak Spell Power`, `Zoltraak Resistance`).
- **Toolchain**:
  - Automated JDK 21 `build.py` orchestrator with automatic deployment to local CurseForge instances.
