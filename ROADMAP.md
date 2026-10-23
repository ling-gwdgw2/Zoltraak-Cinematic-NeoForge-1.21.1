# Zoltraak: Cinematic Edition - Architectural and Macro Roadmap

## 1. Executive Summary and Macro Strategy

Zoltraak: Cinematic Edition is engineered to bridge anime-grade cinematics with robust Minecraft gameplay mechanics. To maintain long-term viability across evolving Minecraft versions and underlying graphics backends, this roadmap outlines strategic development milestones from current version 1.0.2 to future macro architectures, including the long-term transition toward Vulkan in modern Minecraft versions (Minecraft 26.x and beyond).

---

## 2. Near-Term Horizon: Versions 1.0.3 - 1.1.0

### 2.1 Graphics and Shader Profiling
* Performance Quality Profiles:
  * Low Profile: Disables secondary anamorphic flares and caps raymarch iterations to 32 steps for low-end GPUs and handheld systems.
  * Medium Profile: Standard 64-step raymarching with optimized procedural noise.
  * Ultra Profile: Full 112-step raymarching, dual photon ring interpolation, and full 16-tap Gaussian anamorphic bloom.
* Shader Warmup Cache: Pre-compiles and links all 17 Core GLSL shaders during client game initialization to eliminate first-cast stutter.

### 2.2 Boss AI and Combat Interaction Refinements
* Advanced Flanking Heuristics:
  * Expansion of `QualAntiBarrierGoal` to evaluate multi-player barrier overlaps.
  * Adaptive target switching based on accumulated threat and DPS output rather than proximity alone.
* Spell Reflection and Barrier Polarization:
  * Interaction mechanics where direct hits from standard Zoltraak beams against `DefenseBarrierEntity` produce localized reflective deflection when timed perfectly (parry mechanic).

### 2.3 Anime-Accurate Zoltraak VFX Overhaul (1:1 Madhouse Animation Fidelity)
Comprehensive visual and shader realignment to achieve pixel-perfect fidelity with Madhouse's *Sousou no Frieren* anime animation direction (Keiichirō Saitō / Tōru Iwazawa / Ken'ichirō Aoki):

* **2.3.1 Rapid Zoltraak & Phalanx Barrage Projectiles [COMPLETED]:**
  * **Shader Architecture Refactor:** Engineered dedicated `rapid_zoltraak_bullet.json`, `rapid_zoltraak_bullet.vsh`, and `rapid_zoltraak_bullet.fsh` GLSL 150 pipeline registered via `RAPID_BULLET` render type with additive blending.
  * **Piercing Needle-Laser Geometry & Anti-Ball Refactor:**
    * Fixed sub-pixel culling by expanding quad base width to 0.76m (`baseWidth = 0.38m`) and supersonic streak length to 7.2 blocks (`targetNeedleLen = 7.2F`).
    * Implemented 7-point curve resampling (`samplePathAtDistance`) along `pathHistory`, ensuring smooth 3D volumetric ribboning with both camera-facing primary billboards and perpendicular 3D cross-planes.
    * **Eliminated In-Flight Explosion Sphere:** Removed the extraneous in-flight `ZOL_IMPACT` (3D spherical mana explosion) billboard that previously rendered as a floating ball at the projectile head, reserving it strictly for true target impacts.
    * Replaced round tip bloom with a sharp 4-point diamond star sparkle glint (13cm radius, 2cm thin).
    * Implemented super-Gaussian ($e^{-(1.85 \cdot \text{dist})^4}$) searing solid white plasma core with supersonic ionization ripples, an aerodynamic diamond arrowhead taper ($x \in [0.84, 1.0]$), and smooth atmospheric dissolution ($x \in [0.0, 0.20]$).
    * Dual-tone support: Fern's Demon-Slaying (Pure White Core + Hyper-Ionized Electric Cyan Sheath) and Qual's Human-Killing (Abyssal Void Core + Blinding Electric Magenta/Crimson Sheath).
  * **Aperture Muzzle Acoustics & Blast Rings:** Sharpened per-circle discharge rings in `barrage_fx.fsh` into high-velocity staccato shockwave pops matching Fern's rapid-fire cadence.

* **2.3.2 Corrupted / Human-Killing Zoltraak (Elder Sage Qual) [1:1 ANIME COLOR OVERHAUL COMPLETED]:**
  * **100% Anime-Accurate Pitch-Black Void Core:**
    * Completely purged all neon magenta/hot-pink coloration from 
apid_zoltraak_bullet.fsh, ZoltraakColorTheme.java, lack_magic_circle.fsh, and zoltraak_impact.fsh.
    * Implemented solid, light-devouring pitch-black obsidian void needles (ec3(0.003, 0.004, 0.008)) with lpha = 1.0 rendered via BLACK_RAPID_BULLET (TRANSLUCENT_TRANSPARENCY), allowing black projectiles to completely occlude the sky and background without being washed out by additive blending.
    * Added dark charcoal soot mantle (ec3(0.045, 0.038, 0.055)), muted dark violet rim (ec3(0.20, 0.11, 0.32)), and fine mineral disintegration dust (ec3(0.48, 0.35, 0.65)).
    * Removed tip glint and neon charging flares on black projectiles for razor-sharp, ominous obsidian flight.
  * **Sinister Magic Circle & Disintegration Impact:**
    * Realigned lack_magic_circle.fsh from magenta to dark obsidian void, muted ash violet, and silver-ash runes.
    * Routed impacts to BLACK_IMPACT with translucent blending to create dark soot/ash clouds upon target impact.
  * **Atmospheric & Ground Disintegration Wake:** Add scorched ground trails and drifting black cinder particle emissions along the beam's supersonic flight trajectory.

* **2.3.3 Standard White Zoltraak (Ordinary Offensive Magic):**
  * **Arrowhead Penetration Wavefront:** Upgrade the leading tip of the beam from a flat quad cutoff to a razor-sharp, aerodynamic plasma arrowhead that visibly pierces the atmosphere.
  * **Cylindrical Volume Shading:** Augment billboard quads with pseudo-cylindrical normal mapping (`dFdx`/`dFdy`) to eliminate the 2D "paper-beam" look when viewed at acute player angles.
  * **Anamorphic Streak Lens Flare:** Implement Madhouse's signature horizontal anamorphic flare streak across the screen on staff discharge via `cinematic_flare.fsh`.

* **2.3.4 Great Zoltraak (Cataclysmic Volumetric Cannon):**
  * **Lateral Fluid Plume Splash:** Enhance `great_zoltraak_plume.fsh` to dynamically spawn 90-degree lateral tidal wave plumes when the 64-sided beam impacts terrain or defensive barriers.
  * **High-Key Exposure Burst:** Brief screen-space overexposure flash and severe low-frequency camera rumble during the beam's initial detonation frame.

* **2.3.5 Dynamic Cross-Spell Barrier Interaction (Zoltraak vs. Defense Magic):**
  * **Hexagonal Honeycomb Impact Ripples:** Connect `ZoltraakCinematicBeamEntity` impact coordinates directly to `DefenseBarrierRenderer.java`:
    * Impact point triggers incandescent white energy wavefronts dispersing radially across neighboring hexagonal tiles on `defense_barrier.fsh`.
  * **Resonant Crystalline Audio:** Distinct high-tension glass humming audio feedback when a high-yield beam grinds against barrier capacity.

* **2.3.6 Anime-Accurate Zoltraak Impact Explosion, Borehole Piercing & Ring Purge [COMPLETED]:**
  * **Complete Purge of Non-Canon Circular Donut Rings:**
    * **Muzzle Shockwave Rings Purged:** Removed Pass 2 from `ZoltraakBarrageArrayRenderer.java` that was rendering expanding toothed shockwave rings in front of the caster.
    * **Impact Shockwave Rings Purged:** Completely removed the expanding circular ring formulas (`abs(r - ringRadius)` / `abs(r - waveR)`) from `zoltraak_impact.fsh`. Replaced with pure Madhouse-style billowing cel-shaded anime dust smoke lobes, searing plasma fire, and flying stone fragments.
    * **Traveling Beam Rings Purged:** Removed traveling circular arc loops from `ZoltraakRenderer.java`.
  * **Cylindrical Borehole Block Piercing (เจาะบล็อกเป็นรูทรงกระบอกตามแนวลำแสง):**
    * In `ZoltraakCinematicBeamEntity.java`, Zoltraak now pierces through destructible blocks (stone, dirt, wood, leaves, ores, glass, etc.) along a cylindrical borehole trajectory rather than stopping dead on the surface.
    * Pulverizes blocks into block debris particles and removes them cleanly without dropping loose entity items that cause TPS lag.
    * Only unbreakable blocks (Bedrock, Barrier blocks) and active human Defense Magic (`DefenseBarrierEntity`) absorb and stop the beam.
    * Fully configurable via `beamDrillBlocks` (default: `true`) and `beamDrillRadius` (default: `1.25` blocks, scaled 1.85x for Great Zoltraak), and strictly respects `mobGriefing`.
  * **Anime-Accurate Dust Billows & Flying Debris:**
    * When striking surfaces, raises dense billowing clouds of pale-cyan/mint dust with 2-tone cel-shaded lighting and sprays high-velocity dielectric sparks and shattered stone chunks.

---

## 3. Mid-Term Horizon: Minecraft 1.22+ and Modern NeoForge

### 3.1 Platform and Engine Modernization
* NeoForge Lifecycle Migration:
  * Upgrading to Minecraft 1.22+ data component pipelines and revised network payload registries.
  * Migration from legacy tag formats to modern registry-driven damage types and school properties.
* Modular Spellcrafting and Runic Matrix:
  * Runic socketing mechanics for Frieren's Staff and Ordinary Grimoires.
  * Socketable modifiers altering beam divergence, shockwave radius, projectile velocity, and mana consumption curves.

### 3.2 Content Expansion: The Sages of Destruction
* Macht of the Golden Land (El Dorado):
  * Universal transmutation spell school: gold transmutation mechanics with procedural metallic surface shaders.
  * Non-destructive visual block transmutation with rollback guarantees.
* Aura the Guillotine (The Scales of Obedience):
  * Soul weight balance mechanics tied to target max mana and current stamina.
  * Specialized mind-control and spectral entity summoning mechanics.

### 3.3 Advanced Translucency Architecture
* Weighted, Blended Order-Independent Transparency (WBOIT):
  * Implementation of depth-weighted OIT in Blaze3D to resolve alpha-sorting artifacts between overlapping defense domes, beam cores, and projectile ribbon trails.

---

## 4. Long-Term Macro Vision: Minecraft 26.2, Vulkan Migration and Aperture

With the official confirmation of Aperture (the successor to Iris running on Minecraft 26.2 with Vulkan and Sodium), the Minecraft graphics ecosystem is formally transitioning from legacy OpenGL 3.2 Core profiles to a modern, low-overhead Vulkan pipeline. Zoltraak: Cinematic Edition will leverage Aperture's architecture to unlock desktop-class compute and raytracing capabilities.

### 4.1 Transition from OpenGL 3.2 / GLSL 150 to SPIR-V and Vulkan
* SPIR-V Bytecode Compilation:
  * Transition from runtime GLSL 150 text parsing (`#version 150`) to offline-compiled SPIR-V binary modules natively consumed by Vulkan and Aperture.
  * Unified shader pipeline with cross-compilation support via SPIRV-Cross for multi-backend execution.
* Explicit Pipeline State Objects (PSO):
  * Pre-baking blend modes, depth-stencil states, and vertex layouts into immutable PSOs, removing OpenGL state validation overhead from the render loop.

### 4.2 Compute Shader Relativistic Raymarching under Aperture
* Decoupling from the Fragment Stage:
  * In OpenGL 150, the Kerr black hole raymarching is constrained to fullscreen fragment shaders (`gargantua.fsh`), incurring heavy rasterization and fill-rate penalties.
  * In the Aperture Vulkan architecture, raymarching of the Thorne geodesic equations ($d^2u/d\phi^2 + u = 3Mu^2$) will be offloaded entirely to asynchronous GPU Compute Shaders.
* Variable Rate Shading (VRS) and Tiled Compute:
  * Compute dispatch will utilize 16x16 tile workgroups with hierarchical bounding box checks, executing geodesic leapfrog calculations only on pixels intersecting the gravitational lensing envelope.
  * High-density accretion disk simulations will execute independently of the game frame rate on asynchronous compute queues.

### 4.3 Native Aperture Render Graph Integration
* Aperture Render Graph Hooking:
  * Replacing custom mixin hooks (`GameRendererMixin`) and procedural draw passes (`ZoltraakRenderPass`) with Aperture's declarative, barrier-synchronized Render Graph.
  * Resolves the current shaderpack limitation: screen-space gravitational lensing (`SceneLens`) and post-processing passes will integrate directly into Aperture's G-buffer pipeline without disabling refractive features.
  * Automated resource aliasing and barrier transitions for off-screen FBO ping-pong buffers (`SceneLens`, post-processing passes).

### 4.4 High-Performance Kinematics via Project Loom and Panama
* Parallel Entity Kinematics:
  * Offloading wide-area gravitational acceleration fields (100m radius across hundreds of entities) to Java Virtual Threads (Project Loom).
* SIMD Vector API Acceleration (Project Panama):
  * Vectorized vector-math operations for geodesic polyhedron generation, raycast collision, and terrain reconstruction batches.

---

## 5. Milestone Matrix

| Horizon | Target Platform | Graphics Backend | Core Architectural Deliverables |
| :--- | :--- | :--- | :--- |
| Phase 1: Polish & Optimization | MC 1.21.1 / NeoForge 21.1.x | OpenGL 3.2 (GLSL 150) | Quality presets, shader warmup cache, 1:1 anime Zoltraak VFX overhaul (laser needles, fractal lightning, hex impact ripples), boss parry mechanics, memory optimizations. |
| Phase 2: Engine Expansion | MC 1.22.x / NeoForge Next | OpenGL 3.2 / Early WBOIT | Order-independent transparency, modular runic matrix, Macht and Aura boss encounters. |
| Phase 3: Modern API Preparation | MC 25.x / Next-Gen Loader | Hybrid OpenGL / Vulkan Stubs | SPIR-V shader toolchain, Render Graph abstraction, Virtual Threaded kinematics. |
| Phase 4: Full Vulkan Architecture | MC 26.2 / Aperture + Sodium | Vulkan 1.3 / SPIR-V | Compute shader geodesic raymarching, Aperture Render Graph integration, immutable PSOs, full raytracing interoperability. |
