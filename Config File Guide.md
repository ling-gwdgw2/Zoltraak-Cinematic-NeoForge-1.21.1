# ⚙️ Zoltraak: Cinematic Edition — Configuration Guide

Welcome to the official configuration guide for **Zoltraak: Cinematic Edition (NeoForge 1.21.1)**!

This guide provides a complete, easy-to-read reference for all settings available in the mod. Whether you are optimizing performance for an integrated GPU, configuring an anti-grief survival server, or tuning the Qual raid boss encounter for your modpack, you will find every parameter, recommended preset, and default value explained below.

---

## 📂 Quick Start: File Locations

The mod generates two distinct TOML configuration files inside your Minecraft directory:

| Config File | Typical Path | Applied On | Description |
| :--- | :--- | :--- | :--- |
| **`zoltraak_cinematic-client.toml`** | `.minecraft/config/` | **Client only** | Visual accessibility, camera shake, post-processing shaders, Gargantua disk scale, and audio preferences. |
| **`zoltraak_cinematic-common.toml`** | `.minecraft/config/` | **Server / Singleplayer** | Gameplay mechanics, damage multipliers, flight mana scaling, block griefing, boss stats, and spell balances. |

> [!NOTE]
> * **Singleplayer:** Both files apply to your game.
> * **Dedicated Multiplayer Servers:** Only `zoltraak_cinematic-common.toml` is used on the server. Each player customizes their own `zoltraak_cinematic-client.toml` locally to suit their hardware and accessibility preferences.
> * **Applying Changes:** Most client visual settings update immediately upon re-entering a world or typing `/reload`. For major common config changes, restarting the world/server is recommended.

---

## 🎨 1. Client Configuration (`zoltraak_cinematic-client.toml`)

Controls all visuals, accessibility safeguards, GPU shader presets, and sound dynamics.

### 🌟 `[visual_accessibility]`
Settings for screen comfort, photosensitivity safety, and cinematic immersion.

| Key | Type | Default | Range | Description |
| :--- | :---: | :---: | :---: | :--- |
| `enableScreenFlash` | `boolean` | `true` | `true` / `false` | Master toggle for full-screen blinding white flash overlays (Supernova, Great Zoltraak). Set to `false` for photosensitive or epileptic players. |
| `screenFlashIntensity` | `double` | `1.0` | `0.0` – `1.0` | Opacity/brightness intensity multiplier for screen flash overlays (`0.0` = completely off, `1.0` = full cinematic flare). |
| `enableCameraShake` | `boolean` | `true` | `true` / `false` | Whether camera shake vibrations occur during massive detonations, spell releases, and Gargantua's gravitational pull. |
| `cameraShakeMultiplier` | `double` | `1.0` | `0.0` – `3.0` | Intensity multiplier for camera rumble (`0.0` = static camera, `1.0` = default rumble, `2.0+` = violent cataclysm). |
| `enableFlightCameraRoll` | `boolean` | `true` | `true` / `false` | Whether the camera tilts with aerodynamic bank roll (±6.5°) when steering during flight. |

---

### 🔮 `[shader_performance]`
Settings for raymarching fidelity, spacetime distortions, and post-processing.

| Key | Type | Default | Range / Options | Description |
| :--- | :---: | :---: | :---: | :--- |
| `blackHoleRaymarchSteps` | `string` | `"MEDIUM"` | `LOW`, `MEDIUM`, `HIGH`, `ULTRA` | Geodesic leapfrog raymarching quality preset for Gargantua's accretion disk.<br>• `LOW`: 32 steps (best for handhelds / Steam Deck / iGPUs)<br>• `MEDIUM`: 64 steps (balanced performance & smooth gas)<br>• `HIGH`: 96 steps (crisp filaments)<br>• `ULTRA`: 128 steps (film-grade fidelity) |
| `accretionDiskOuterRadius` | `double` | `22.0` | `8.0` – `40.0` | Outer radius of Gargantua's accretion disk in gravitational radii ($r_g$).<br>• `13.5`: Original compact radius (~54 blocks)<br>• `22.0`: Default expanded radius (~88 blocks, 176 blocks diameter)<br>• `30.0+`: Massive cosmic proportions |
| `enableGravitationalLensing` | `boolean` | `true` | `true` / `false` | Enables full-screen Einstein deflection and spacetime distortion around the event horizon. Turn off if experiencing GPU bottlenecks. |
| `enableAnamorphicLensFlares` | `boolean` | `true` | `true` / `false` | Enables horizontal 16-sample anamorphic optical streak flares on beam cores. |
| `enableChromaticAberration` | `boolean` | `true` | `true` / `false` | Enables chromatic color fringe dispersion on high-energy beam impacts and screen borders. |

---

### 🔊 `[audio_accessibility]`
Settings for hearing comfort and custom sound balancing.

| Key | Type | Default | Range | Description |
| :--- | :---: | :---: | :---: | :--- |
| `enableTinnitusSound` | `boolean` | `true` | `true` / `false` | Plays a realistic high-frequency ear ringing sound after surviving a point-blank detonation. |
| `tinnitusVolumeMultiplier` | `double` | `1.0` | `0.0` – `2.0` | Volume scaling multiplier specifically for the tinnitus ear ringing sound effect. |
| `cinematicMasterAudioVolume` | `double` | `1.0` | `0.0` – `2.0` | Master volume scaling multiplier for all custom cinematic spells, beams, and boss audio. |

---

## ⚔️ 2. Common & Server Configuration (`zoltraak_cinematic-common.toml`)

Controls gameplay balance, damage output, spell behavior, flight physics, and boss scaling.

### 🌌 `[gargantua]`
Settings for the Kerr Rotating Black Hole / Astral Singularity.

| Key | Type | Default | Range | Description |
| :--- | :---: | :---: | :---: | :--- |
| `pullRadius` | `double` | `100.0` | `10.0` – `300.0` | Maximum radius in blocks within which entities are pulled toward the singularity. |
| `pullAcceleration` | `double` | `0.12` | `0.01` – `2.0` | Base gravitational pull acceleration strength applied to entities per tick. |
| `tidalDamagePercent` | `double` | `0.08` | `0.01` – `1.0` | Spaghettification damage dealt inside the event horizon (fraction of Max HP every 8 ticks / 0.4s). `0.08` = 8% Max HP per tick window. |
| `blastDamage` | `double` | `800.0` | `10.0` – `10000.0` | Base damage dealt by the final Supernova core detonation at 55 seconds. |
| `blastRadius` | `double` | `32.0` | `5.0` – `200.0` | Physical blast shockwave radius in blocks for the Supernova detonation. |
| `tearBlocks` | `boolean` | `true` | `true` / `false` | Whether Gargantua physically tears terrain surface blocks into orbiting accretion disk debris. |
| `maxTornDebris` | `integer` | `180` | `0` – `1000` | Maximum surface blocks permitted to be torn into debris over the 60s lifetime to protect server TPS. |
| `respectMobGriefing` | `boolean` | `true` | `true` / `false` | When `true`, Gargantua will not destroy or tear terrain blocks if the world gamerule `/gamerule mobGriefing false` is set. |

---

### 🕊️ `[flight_system]`
Settings for Frieren's human flight magic and aerodynamic speed scaling.

| Key | Type | Default | Range | Description |
| :--- | :---: | :---: | :---: | :--- |
| `baseManaCostPerSecond` | `double` | `50.0` | `5.0` – `200.0` | Base mana consumed per second at Flight Spell Level 1 while hovering or cruising. |
| `manaReductionPerLevel` | `double` | `2.5` | `0.0` – `10.0` | Amount of base mana saved per spell level (e.g., Level 5 consumes `40.0` mana/sec). |
| `enableSpeedAltitudeScaling` | `boolean` | `true` | `true` / `false` | When `true`, flying at high speeds or very high altitudes expends proportionally more mana. |
| `speedMultiplierExponent` | `double` | `1.8` | `1.0` – `3.0` | Exponential scaling curve for high-speed cruising beyond 0.10 blocks/tick. |
| `maxSpeedMultiplier` | `double` | `8.0` | `1.0` – `20.0` | Hard cap on the velocity mana multiplier to prevent overflow during physics anomalies. |
| `altitudeMultiplierExponent` | `double` | `1.7` | `1.0` – `3.0` | Exponential scaling curve for flying high above the ground surface (>3 blocks). |
| `maxAltitudeMultiplier` | `double` | `15.0` | `1.0` – `30.0` | Hard cap on altitude mana multiplier when soaring high in the stratosphere. |
| `emergencyBufferTicks` | `integer` | `70` | `0` – `200` | Emergency Slow Falling duration in ticks upon mana depletion (70 ticks = 3.5s) to prevent lethal fall damage. |
| `autoEngageAirborne` | `boolean` | `true` | `true` / `false` | Automatically engages flight mode when jumping or walking off a ledge while under the Flight effect. |
| `enableTelemetryHUD` | `boolean` | `true` | `true` / `false` | Displays real-time flight telemetry (Mana/s, Altitude, Speed factor) on the action bar. |

---

### ⚡ `[spells_and_beams]`
Settings for continuous Zoltraak piercing beams.

| Key | Type | Default | Range | Description |
| :--- | :---: | :---: | :---: | :--- |
| `beamStandardRange` | `double` | `64.0` | `16.0` – `128.0` | Maximum piercing beam length in blocks for Standard Zoltraak and Corrupted Zoltraak. |
| `beamLargeRange` | `double` | `96.0` | `32.0` – `256.0` | Maximum beam length in blocks for fully-charged Great Zoltraak. |
| `beamDamageMultiplier` | `double` | `1.0` | `0.1` – `10.0` | Global damage multiplier applied to all continuous Zoltraak beams. |
| `disableShieldOnHit` | `boolean` | `true` | `true` / `false` | If `true`, beam impact disables the target's shield blocking for standard cooldown. |
| `beamKnockbackFactor` | `double` | `1.5` | `0.0` – `5.0` | Knockback impulse applied to targets caught in the beam shockwave. |
| `corruptedWitherDuration` | `integer` | `120` | `0` – `600` | Wither debuff duration in ticks (120 ticks = 6.0s) applied by Corrupted/Black Zoltraak. |
| `respectFriendlyFire` | `boolean` | `true` | `true` / `false` | Prevents beams from damaging team members, party members, or tamed pets of the caster. |
| `beamDrillBlocks` | `boolean` | `true` | `true` / `false` | If `true` and `mobGriefing` is enabled, Zoltraak beams physically drill/blast a cylindrical hole through blocks along their trajectory. |
| `beamDrillRadius` | `double` | `1.25` | `0.5` – `4.0` | Base borehole radius in blocks drilled by standard Zoltraak (Great Zoltraak drills 2x this radius). |

---

### 🎯 `[barrage_matrix]`
Settings for Fern's Grand Phalanx 24-circle magic barrage.

| Key | Type | Default | Range | Description |
| :--- | :---: | :---: | :---: | :--- |
| `barrageHomingStrength` | `double` | `0.34` | `0.0` – `1.0` | Aerodynamic tracking rate toward target (`0.0` = straight unguided lance, `1.0` = aggressive 90° tracking). |
| `barrageProjectileSpeed` | `double` | `1.75` | `0.5` – `5.0` | Flight velocity of individual barrage light lances in blocks/tick. |
| `barrageMaxLifetimeTicks` | `integer` | `60` | `20` – `200` | Maximum projectile lifetime before despawning (60 ticks = 3.0s). |
| `barrageFireInterval` | `integer` | `10` | `4` – `30` | Interval between barrage volleys in ticks (10 ticks = 0.5s = 2 volleys per second). |
| `barrageDamageMultiplier` | `double` | `1.0` | `0.1` – `5.0` | Damage scaling factor for barrage light lance impacts. |
| `barrageBlastRadius` | `double` | `1.2` | `0.0` – `4.0` | Area-of-effect splash damage radius in blocks upon impact. |

---

### 🛡️ `[defense_barrier]`
Settings for Directional Aegis and Geodesic Hexagonal Dome barriers.

| Key | Type | Default | Range | Description |
| :--- | :---: | :---: | :---: | :--- |
| `barrierCapacityMultiplier` | `double` | `1.0` | `0.1` – `10.0` | Durability health multiplier for barriers before shattering. |
| `barrierDurationTicks` | `integer` | `300` | `60` – `1200` | Base barrier lifetime in ticks (300 ticks = 15.0s) before naturally dissipating. |
| `barrierDeflectArrows` | `boolean` | `true` | `true` / `false` | When `true`, physically deflects mundane projectile weapons (arrows, tridents). |
| `barrierDeflectMagicProjectiles` | `boolean` | `true` | `true` / `false` | When `true`, absorbs and neutralizes incoming hostile magical projectiles (Zoltraak lances, Fireballs). |

---

### 👑 `[qual_boss]`
Settings for The Elder Sage of Corruption: Qual boss encounter.

| Key | Type | Default | Range | Description |
| :--- | :---: | :---: | :---: | :--- |
| `qualBaseHealth` | `double` | `1500.0` | `200.0` – `10000.0` | Qual's baseline maximum health for solo encounters. |
| `qualHealthPerExtraPlayer` | `double` | `200.0` | `0.0` – `2000.0` | Additional health dynamically granted per nearby player in multiplayer boss fights. |
| `qualArmor` | `double` | `30.0` | `0.0` – `100.0` | Base armor rating for Qual. |
| `qualArmorToughness` | `double` | `10.0` | `0.0` – `50.0` | Base armor toughness rating for Qual. |
| `qualRecoveryWindowMinTicks` | `integer` | `200` | `60` – `400` | Minimum recovery window (200 ticks = 10.0s) after Qual executes his Corrupted Barrage. |
| `qualRecoveryWindowMaxTicks` | `integer` | `240` | `80` – `600` | Maximum recovery window (240 ticks = 12.0s) during which players have a melee attack opening. |
| `qualDespawnDistance` | `double` | `96.0` | `32.0` – `256.0` | Maximum distance from players before Qual resets and re-seals back into the sealing stone. |

---

### ⏳ `[artifacts_and_relics]`
Settings for special artifacts including Frieren's Staff and the Chronos Dilatation Watch.

| Key | Type | Default | Range | Description |
| :--- | :---: | :---: | :---: | :--- |
| `staffCastSpeedBonus` | `double` | `0.15` | `0.0` – `1.0` | Cast time reduction percentage granted by Frieren's Staff (`0.15` = +15% faster casting). |
| `staffManaRegenBonus` | `double` | `5.0` | `0.0` – `50.0` | Flat bonus mana regeneration (mana/sec) granted while wielding Frieren's Staff. |
| `chronosWatchRadius` | `double` | `16.0` | `4.0` – `64.0` | Spherical time dilation bubble radius in blocks for the Chronos Dilatation Watch. |
| `chronosWatchDurationTicks` | `integer` | `200` | `20` – `400` | Time dilation duration in ticks (200 ticks = 10.0s). |
| `chronosWatchCooldownSeconds` | `integer` | `60` | `10` – `600` | Cooldown in seconds before the Chronos Dilatation Watch can be reactivated. |

---

## 💡 Recommended Presets

Here are ready-to-copy configurations for common use cases:

### 🎮 Preset A: Safe / Anti-Grief Server
*Best for public SMPs where players build towns and terrain destruction must be strictly disabled.*

In `zoltraak_cinematic-common.toml`:
```toml
[gargantua]
    tearBlocks = false
    maxTornDebris = 0
    respectMobGriefing = true

[spells_and_beams]
    beamDrillBlocks = false
    respectFriendlyFire = true
```

---

### 💻 Preset B: Low-End Hardware / Steam Deck / iGPU
*Best for maximizing FPS on laptops or integrated graphics while preserving anime aesthetics.*

In `zoltraak_cinematic-client.toml`:
```toml
[visual_accessibility]
    screenFlashIntensity = 0.5
    cameraShakeMultiplier = 0.75

[shader_performance]
    blackHoleRaymarchSteps = "LOW"
    accretionDiskOuterRadius = 16.0
    enableGravitationalLensing = true
    enableAnamorphicLensFlares = false
    enableChromaticAberration = false
```

---

### 🎬 Preset C: Ultra Cinematic / High-End RTX PC
*Best for high-end rigs, content creators, and cinema-grade screenshots.*

In `zoltraak_cinematic-client.toml`:
```toml
[visual_accessibility]
    enableScreenFlash = true
    screenFlashIntensity = 1.0
    enableCameraShake = true
    cameraShakeMultiplier = 1.25
    enableFlightCameraRoll = true

[shader_performance]
    blackHoleRaymarchSteps = "ULTRA"
    accretionDiskOuterRadius = 26.0
    enableGravitationalLensing = true
    enableAnamorphicLensFlares = true
    enableChromaticAberration = true
```

---

## ❓ Frequently Asked Questions (FAQ)

### Q: How do I restore all settings back to default?
Simply delete `zoltraak_cinematic-client.toml` or `zoltraak_cinematic-common.toml` while the game/server is closed. Upon the next startup, the mod will automatically generate fresh files with default values.

### Q: Why did Gargantua destroy my base?
By default, Gargantua tears nearby surface blocks to feed its accretion disk, and standard Zoltraak beams drill holes through obstacles. If you want zero block damage, either:
1. Run `/gamerule mobGriefing false` in your world.
2. Or set `tearBlocks = false` and `beamDrillBlocks = false` in `zoltraak_cinematic-common.toml`.

### Q: Can players change boss health or spell damage from the client config?
**No.** All gameplay balance, damage modifiers, and boss stats are strictly controlled by `zoltraak_cinematic-common.toml` on the server to prevent cheating. Client configs only affect rendering, screen shake, and audio.
