# Zoltraak: Cinematic Edition — Configuration Specification & Guide (CONFIG)

**Version:** 1.0.3 | **Minecraft:** 1.21.1 | **Mod Loader:** NeoForge 21.1.248+

This document defines the complete configuration architecture and setting specifications for **Zoltraak: Cinematic Edition**. It covers server-wide balance, combat mechanics, client-side accessibility, shader optimization, and modpack customization profiles.

---

## 1. Architecture: Dual-Tier Configuration Model

To ensure seamless multiplayer stability, fairness, and client accessibility, configuration is decoupled into two independent configuration files:

```
.minecraft/
├── config/
│   ├── zoltraak_cinematic-common.toml   <-- Gameplay, Mana, Damage, Combat, AI (Synced/Server)
│   └── zoltraak_cinematic-client.toml   <-- Shaders, Flashes, Screen Shake, Audio (Per-Client)
```

| Config File | NeoForge Type | Target Environment | Scope |
| :--- | :--- | :--- | :--- |
| **`zoltraak_cinematic-common.toml`** | `ModConfig.Type.COMMON` | Dedicated Server & Singleplayer | Game balance, mana costs, damage curves, projectile homing, boss stats, and claim safety. |
| **`zoltraak_cinematic-client.toml`** | `ModConfig.Type.CLIENT` | Local Client Only | Shaders, raymarching step count, camera shake, photosensitive flash toggles, and tinnitus audio. |

---

## 2. Server & Gameplay Settings (`zoltraak_cinematic-common.toml`)

### 2.1 Astral Singularity: Gargantua (`[gargantua]`)
Controls gravitational forces, damage calculations, and block interaction for the Kerr rotating black hole spell.

| Setting Key | Type | Default | Range | Description |
| :--- | :---: | :---: | :---: | :--- |
| `pullRadius` | Double | `100.0` | `10.0` – `300.0` | Gravitational pull radius in blocks. Entities within this radius are pulled toward the singularity. |
| `pullAcceleration` | Double | `0.12` | `0.01` – `2.0` | Base gravitational pull acceleration strength applied to entities per tick. |
| `tidalDamagePercent` | Double | `0.08` | `0.01` – `1.0` | Continuous tidal shear damage dealt every 0.4s inside the event horizon, expressed as fraction of Max HP (0.08 = 8%/tick). |
| `blastDamage` | Double | `800.0` | `10.0` – `10000.0` | Base damage dealt by the final Supernova detonation at tick 1100 (55.0s). |
| `blastRadius` | Double | `32.0` | `5.0` – `200.0` | Shockwave blast radius in blocks for the Supernova detonation. |
| `tearBlocks` | Boolean | `true` | `true` / `false` | When `true`, lifts surface blocks into orbiting accretion disk debris. When `false`, tears zero blocks (pure visual particles & pull). |
| `maxTornDebris` | Integer | `180` | `0` – `1000` | Maximum surface blocks permitted to be torn into debris over the 60s lifetime to safeguard server TPS. |
| `respectMobGriefing` | Boolean | `true` | `true` / `false` | When `true`, Gargantua tears zero blocks if the vanilla `mobGriefing` gamerule is set to `false`. |

---

### 2.2 Frieren Flight Magic (`[flight_system]`)
Controls human flight mechanics, stamina/mana consumption curves, and safety buffers.

| Setting Key | Type | Default | Range | Description |
| :--- | :---: | :---: | :---: | :--- |
| `baseManaCostPerSecond` | Double | `50.0` | `5.0` – `200.0` | Base mana consumed per second at Flight Spell Level 1 while hovering or cruising. |
| `manaReductionPerLevel` | Double | `2.5` | `0.0` – `10.0` | Amount of base mana saved per spell level (e.g. Level 5 consumes 40.0 mana/sec). |
| `enableSpeedAltitudeScaling` | Boolean | `true` | `true` / `false` | When `true`, mana consumption increases exponentially when flying at high speeds or high altitudes. |
| `speedMultiplierExponent` | Double | `1.8` | `1.0` – `3.0` | Exponential scaling curve for high-speed cruising beyond 0.10 blocks/tick. |
| `maxSpeedMultiplier` | Double | `8.0` | `1.0` – `20.0` | Hard cap on the velocity mana multiplier to prevent overflow during physics glitches. |
| `altitudeMultiplierExponent` | Double | `1.7` | `1.0` – `3.0` | Exponential scaling curve for flying high above the ground surface (>3 blocks). |
| `maxAltitudeMultiplier` | Double | `15.0` | `1.0` – `30.0` | Hard cap on altitude mana multiplier when flying in the stratosphere. |
| `emergencyBufferTicks` | Integer | `70` | `0` – `200` | Emergency Slow Falling duration (70 ticks = 3.5s) awarded upon mana depletion to prevent falling deaths. |
| `autoEngageAirborne` | Boolean | `true` | `true` / `false` | Automatically engages flight mode when jumping or falling off a ledge while under the Flight effect. |
| `enableTelemetryHUD` | Boolean | `true` | `true` / `false` | Displays real-time flight telemetry (Mana/s, Altitude, Speed factor) on the action bar. |

---

### 2.3 Zoltraak Offensive Magic & Beams (`[spells_and_beams]`)
Controls beam range, damage multipliers, and shield-penetration properties.

| Setting Key | Type | Default | Range | Description |
| :--- | :---: | :---: | :---: | :--- |
| `beamStandardRange` | Double | `64.0` | `16.0` – `128.0` | Maximum piercing beam length in blocks for standard Zoltraak and Corrupted Zoltraak. |
| `beamLargeRange` | Double | `96.0` | `32.0` – `256.0` | Maximum beam length in blocks for charged Great Zoltraak. |
| `beamDamageMultiplier` | Double | `1.0` | `0.1` – `10.0` | Global damage multiplier applied to all continuous Zoltraak beams. |
| `disableShieldOnHit` | Boolean | `true` | `true` / `false` | If `true`, beam impact disables the target's shield blocking (canonical shield penetration). |
| `beamKnockbackFactor` | Double | `1.5` | `0.0` – `5.0` | Knockback impulse applied to targets caught in the beam shockwave. |
| `corruptedWitherDuration` | Integer | `120` | `0` – `600` | Wither debuff duration in ticks (120 ticks = 6.0s) applied by Corrupted/Black Zoltraak. |
| `respectFriendlyFire` | Boolean | `true` | `true` / `false` | Prevents beams from damaging team members, party members, or tamed pets of the caster. |
| `beamDrillBlocks` | Boolean | `true` | `true` / `false` | If `true` and `mobGriefing` is enabled, Zoltraak beams physically drill/blast a cylindrical hole through blocks along their trajectory. |
| `beamDrillRadius` | Double | `1.25` | `0.5` – `4.0` | Base radius in blocks of the borehole drilled by standard Zoltraak (Great Zoltraak scales by 1.85x). |

---

### 2.4 Fern's Grand Phalanx Barrage (`[barrage_matrix]`)
Controls projectile homing aerodynamics, salvo cadence, and projectile attributes.

| Setting Key | Type | Default | Range | Description |
| :--- | :---: | :---: | :---: | :--- |
| `barrageHomingStrength` | Double | `0.34` | `0.0` – `1.0` | Aerodynamic steering rate toward target (0.0 = straight unguided lance, 1.0 = instant tracking). |
| `barrageProjectileSpeed` | Double | `1.75` | `0.5` – `5.0` | Flight velocity of individual barrage light lances in blocks/tick. |
| `barrageMaxLifetimeTicks` | Integer | `60` | `20` – `200` | Maximum projectile lifetime before despawning (60 ticks = 3.0s). |
| `barrageFireInterval` | Integer | `10` | `4` – `30` | Interval between barrage volleys in ticks (10 ticks = 0.5s = 2 volleys/sec). |
| `barrageDamageMultiplier` | Double | `1.0` | `0.1` – `5.0` | Damage scaling factor for barrage projectiles. |
| `barrageBlastRadius` | Double | `1.2` | `0.0` – `4.0` | Area-of-effect impact splash radius in blocks. |

---

### 2.5 Defensive Barrier Magic (`[defense_barrier]`)
Controls shield durability, physical arrow deflection, and dome longevity.

| Setting Key | Type | Default | Range | Description |
| :--- | :---: | :---: | :---: | :--- |
| `barrierCapacityMultiplier` | Double | `1.0` | `0.1` – `10.0` | Durability multiplier for barrier shield HP (both Directional Aegis and Geodesic Dome). |
| `barrierDurationTicks` | Integer | `300` | `60` – `1200` | Base barrier lifetime in ticks (300 ticks = 15.0s) before naturally dissipating. |
| `barrierDeflectArrows` | Boolean | `true` | `true` / `false` | When `true`, physically deflects mundane projectile weapons (arrows, tridents). |
| `barrierDeflectMagicProjectiles` | Boolean | `true` | `true` / `false` | When `true`, absorbs and neutralizes incoming hostile magical projectiles. |

---

### 2.6 The Elder Sage of Corruption: Qual Boss (`[qual_boss]`)
Controls boss attributes, multiplayer scaling, and AI combat pacing.

| Setting Key | Type | Default | Range | Description |
| :--- | :---: | :---: | :---: | :--- |
| `qualBaseHealth` | Double | `1500.0` | `200.0` – `10000.0` | Qual's baseline max health for solo encounters. |
| `qualHealthPerExtraPlayer` | Double | `200.0` | `0.0` – `2000.0` | Additional health dynamically granted per nearby player in multiplayer boss fights. |
| `qualArmor` | Double | `30.0` | `0.0` – `100.0` | Base armor value for Qual. |
| `qualArmorToughness` | Double | `10.0` | `0.0` – `50.0` | Base armor toughness for Qual. |
| `qualRecoveryWindowMinTicks` | Integer | `200` | `60` – `400` | Minimum recovery window (200 ticks = 10.0s) after Qual executes his 4s Corrupted Barrage. |
| `qualRecoveryWindowMaxTicks` | Integer | `240` | `80` – `600` | Maximum recovery window (240 ticks = 12.0s) during which players have a melee opening. |
| `qualDespawnDistance` | Double | `96.0` | `32.0` – `256.0` | Maximum distance from players before Qual resets and re-seals into the sealing stone. |

---

### 2.7 Magical Artifacts & Relics (`[artifacts_and_relics]`)
Controls special attributes, radii, and cooldowns for iconic items.

| Setting Key | Type | Default | Range | Description |
| :--- | :---: | :---: | :---: | :--- |
| `staffCastSpeedBonus` | Double | `0.15` | `0.0` – `1.0` | Cast time reduction (+15%) granted by Frieren's Staff. |
| `staffManaRegenBonus` | Double | `5.0` | `0.0` – `50.0` | Flat mana regeneration bonus (mana/s) granted by Frieren's Staff. |
| `chronosWatchRadius` | Double | `16.0` | `4.0` – `64.0` | Time dilation spherical bubble radius for the Chronos Dilatation Watch. |
| `chronosWatchDurationTicks` | Integer | `200` | `20` – `400` | Time dilation duration in ticks (200 ticks = 10.0s). |
| `chronosWatchCooldownSeconds`| Integer | `60` | `10` – `600` | Cooldown in seconds before the Chronos Dilatation Watch can be reactivated. |

---

## 3. Client Accessibility & Performance Settings (`zoltraak_cinematic-client.toml`)

### 3.1 Visual Accessibility & Screen Dynamics (`[visual_accessibility]`)
Designed to safeguard players with photosensitivity, epilepsy, or motion sickness.

| Setting Key | Type | Default | Range | Description |
| :--- | :---: | :---: | :---: | :--- |
| `enableScreenFlash` | Boolean | `true` | `true` / `false` | Master toggle for fullscreen blinding white flashes (Supernova, Great Zoltraak). Set to `false` for photosensitive players. |
| `screenFlashIntensity` | Double | `1.0` | `0.0` – `1.0` | Opacity/brightness multiplier for screen flashes (0.0 = completely off, 0.5 = muted). |
| `enableCameraShake` | Boolean | `true` | `true` / `false` | Toggles camera shake vibrations during massive detonations and continuous beam firing. |
| `cameraShakeMultiplier` | Double | `1.0` | `0.0` – `3.0` | Adjusts the physical strength of camera shake (0.0 = static camera, 2.0 = cinematic heavy rumble). |
| `enableFlightCameraRoll` | Boolean | `true` | `true` / `false` | Aerodynamic camera roll banking ($\pm 6.5^\circ$) when steering during flight. Can be disabled to avoid motion sickness. |

---

### 3.2 Shader Performance & Post-Processing (`[shader_performance]`)
Optimizes GPU raymarching workloads for integrated graphics (iGPUs) and handheld devices.

| Setting Key | Type | Default | Allowed Values | Description |
| :--- | :---: | :---: | :---: | :--- |
| `blackHoleRaymarchSteps` | String | `"MEDIUM"` | `"LOW"`, `"MEDIUM"`, `"HIGH"`, `"ULTRA"` | Geodesic leapfrog raymarching iterations:<br>• `LOW`: 32 steps (high performance / iGPU / Steam Deck)<br>• `MEDIUM`: 64 steps (balanced default)<br>• `HIGH`: 96 steps (high fidelity)<br>• `ULTRA`: 128 steps (cinematic movie quality) |
| `enableAnamorphicLensFlares` | Boolean | `true` | `true` / `false` | Horizontal 16-sample Gaussian anamorphic streak flares on beam cores. |
| `enableGravitationalLensing` | Boolean | `true` | `true` / `false` | Fullscreen screen-space spacetime distortion around the event horizon. |
| `enableChromaticAberration` | Boolean | `true` | `true` / `false` | Optical dispersion effect on high-energy beam impacts and screen borders. |

---

### 3.3 Audio Accessibility & Dynamics (`[audio_accessibility]`)
Protects players from high-frequency ear strain and sound distortion.

| Setting Key | Type | Default | Range | Description |
| :--- | :---: | :---: | :---: | :--- |
| `enableTinnitusSound` | Boolean | `true` | `true` / `false` | Toggles the high-frequency ear ringing effect after proximity Supernova detonations. |
| `tinnitusVolumeMultiplier` | Double | `1.0` | `0.0` – `2.0` | Volume scaling specifically for the tinnitus ringing sound. |
| `cinematicMasterAudioVolume` | Double | `1.0` | `0.0` – `2.0` | Master volume scaling factor for all custom cinematic spells and boss sounds. |

---

## 4. Modpack Presets & Balancing Profiles

Below are recommended configuration profiles for common modpack archetypes.

### Profile A: Hardcore / Soulslike Survival
*Focus: Scarce mana, lethal boss encounters, deliberate spellcasting.*
```toml
# zoltraak_cinematic-common.toml
[flight_system]
flightBaseManaCostPerSecond = 80.0
maxSpeedMultiplier = 12.0
emergencyBufferTicks = 30

[qual_boss]
qualBaseHealth = 2500.0
qualHealthPerExtraPlayer = 500.0
qualRecoveryWindowMinTicks = 120
qualRecoveryWindowMaxTicks = 160

[spells_and_beams]
beamDamageMultiplier = 1.25
disableShieldOnHit = true
```

### Profile B: Casual / Anime Power Fantasy
*Focus: Extended flight times, massive cinematic explosions, fast mana recovery.*
```toml
# zoltraak_cinematic-common.toml
[flight_system]
flightBaseManaCostPerSecond = 25.0
enableSpeedAltitudeScaling = false
emergencyBufferTicks = 100

[barrage_matrix]
barrageHomingStrength = 0.50
barrageFireInterval = 8

[gargantua]
pullRadius = 150.0
blastDamage = 1500.0
```

### Profile C: Low-End Hardware & Handheld (Steam Deck / iGPU)
*Focus: Smooth 60 FPS, zero stutter, photosensitive-safe.*
```toml
# zoltraak_cinematic-client.toml
[visual_accessibility]
enableScreenFlash = false
enableCameraShake = false

[shader_performance]
blackHoleRaymarchSteps = "LOW"
enableAnamorphicLensFlares = false
enableGravitationalLensing = true
enableChromaticAberration = false
```
