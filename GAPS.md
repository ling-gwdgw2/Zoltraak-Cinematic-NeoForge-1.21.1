# Zoltraak: Cinematic Edition - Architectural Limitations and Technical Gaps

## 1. Introduction and Scope

This document provides a transparent disclosure of known architectural limitations, design compromises, and engineering trade-offs present in Zoltraak: Cinematic Edition (version 1.0.2). These limitations arise from constraints within Minecraft's legacy rendering engine (Blaze3D / OpenGL 3.2), single-threaded server tick architecture, and multiplayer networking environments.

---

## 2. Rendering and Graphics Subsystem Limitations

### 2.1 Legacy OpenGL 3.2 Profile and Lack of Compute Shaders
* Core Issue: Minecraft's Blaze3D pipeline is strictly bound to OpenGL 3.2 Core (`#version 150`).
* Technical Impact:
  * Lack of native Compute Shader access prevents offloading expensive numerical integration tasks to the GPU compute pipeline without requiring heavyweight third-party runtime frameworks.
  * Complex mathematical algorithms, such as the 4th-order geodesic leapfrog integration for Kerr black holes ($d^2u/d\phi^2 + u = 3Mu^2$) and procedural FBM noise sampling, must be executed entirely within fragment shaders (`gargantua.fsh`, `black_zoltraak_beam.fsh`).
  * On high-resolution displays (1440p, 4K), fullscreen fragment processing incurs substantial fill-rate overhead that cannot be mitigated by GPU compute workgroups or tile-based execution.

### 2.2 Fragment-Stage Raymarching Fill-Rate Penalties
* Core Issue: The raymarching loop in `gargantua.fsh` evaluates between 48 and 112 numerical steps per pixel.
* Technical Impact:
  * While dedicated modern GPUs (RTX 3000/4000 series, RX 6000/7000 series) sustain 60+ FPS during active singularities, integrated GPUs (Intel Iris Xe, AMD Radeon 600M/700M series) and entry-level graphics cards experience measurable frame rate drops when the black hole occupies a large portion of the viewport.
  * Dynamic adaptive resolution scaling is currently unsupported because Blaze3D lacks dynamic viewport sub-sampling interfaces.

### 2.3 Translucent Depth Sorting and Lack of Order-Independent Transparency (OIT)
* Core Issue: Minecraft does not natively support Order-Independent Transparency (OIT).
* Technical Impact:
  * While `ZoltraakRenderPass` successfully intercepts `GameRenderer#renderLevel` to resolve entity-to-terrain depth clipping, sorting between multiple intersecting translucent objects relies on CPU-side distance sorting.
  * When multiple geodesic defense domes (`DefenseBarrierEntity`) overlap, or when a dense beam core passes through a translucent barrier, minor alpha blending artifacts and localized Z-fighting can occur at the intersection boundaries.

### 2.4 Shaderpack Interoperability and Dynamic Feature Fallbacks
* Core Issue: Third-party shader loaders (Iris, Oculus) and deferred shaderpacks (such as SEUS, Complementary, Kappa) alter Minecraft's internal G-buffers and depth textures.
* Technical Impact:
  * Custom screen-space refraction passes requiring off-screen depth sampling (`SceneLens.java`) can conflict with shaderpacks that rewrite the main framebuffer format.
  * To guarantee zero game crashes, `ShaderCompatibility.java` utilizes dynamic reflection to detect active shaderpacks and automatically disables screen-space lens distortion. As a result, players with shaderpacks active receive flat alpha blending rather than physical gravitational lensing.
  * Future Solution: This architectural limitation will be fundamentally eliminated upon migrating to Aperture (the Vulkan-native successor to Iris on Minecraft 26.2 with Sodium), where custom render passes will register directly into Aperture's unified, barrier-synchronized Render Graph.

### 2.5 Framebuffer Ping-Pong Memory Bandwidth
* Core Issue: The post-processing pipeline (`GargantuaPostProcessor.java`) requires dual-buffered framebuffer copying (`sceneCopy` to `postTarget` to `activeDrawFbo`) to prevent OpenGL sampling feedback loops.
* Technical Impact:
  * Copying full-resolution framebuffers every frame introduces memory bandwidth overhead, particularly on configurations with shared system memory (unified memory architectures and laptops).

---

## 3. Server Threading and Kinematics Limitations

### 3.1 Single-Threaded Gravitational Field Processing
* Core Issue: Minecraft servers execute all entity logic synchronously on the main thread (`ServerLevel#tick`).
* Technical Impact:
  * `GargantuaEntity` computes relativistic inward acceleration and frame-dragging tangential vectors across an attraction radius of up to 100 blocks.
  * In standard gameplay environments, iterating through entities within this bounding box is negligible. However, in heavily populated environments (such as high-density mob grinders with 300+ entities), computing vector kinematics on the main thread can cause server tick duration to exceed 50 milliseconds, resulting in temporary TPS drops below 20.

### 3.2 Elimination of Block Reconstruction and Server Memory Queues (Resolved)
* Core Design Decision: The terrain reconstruction engine (`reconstructBlocks`, `restoreAllRemainingBlocks`, and the volatile `tornBlocks` map) has been completely decommissioned and removed.
* Rationale & Architecture Impact:
  * In multiplayer and modpack environments, players predominantly utilize land protection systems (such as FTB Chunks, WorldGuard, or OpenPartiesAndClaims) to safeguard their builds.
  * Maintaining volatile in-memory block queues across the black hole's 60-second lifespan introduced unnecessary memory overhead, risks of permanent terrain damage on ungraceful server shutdowns, and chunk loading loops during entity removal.
  * Zoltraak Beam craters and Gargantua no longer execute arbitrary block restoration loops, eliminating main-thread block update spikes (`setBlock` flag 3) and preserving peak server TPS under all multiplayer conditions.

### 3.3 I-Frame Bypass Workaround
* Core Issue: Vanilla Minecraft imposes a hardcoded 20-tick (1.0-second) damage immunity window (`invulnerableTime = 20`) on living entities following any damage event.
* Technical Impact:
  * To enable high-frequency spell barrages (such as Fern's 10-shot-per-second barrage), `ZoltraakDamage.java` temporarily resets `target.invulnerableTime = 0` during damage dispatch and restores it within a `finally` block.
  * While this ensures all projectiles register damage, it overrides vanilla damage sequencing. Edge-case conflicts may arise with third-party RPG mods that attach custom listeners to entity hurt timers or calculate multi-phase damage mitigation based on vanilla immunity ticks.

---

## 4. Multiplayer Synchronization and Network Latency

### 4.1 Supersonic Projectile Latency Desynchronization
* Core Issue: Barrage projectiles (`ZoltraakBarrageProjectileEntity`) travel at 56 meters per second (2.8 blocks per tick).
* Technical Impact:
  * When a client operates with network latency exceeding 120ms, client-side visual trail interpolation can visually outpace or diverge from server-authoritative raycast collision points.
  * In high-latency PvP scenarios, a target may visually perceive a near-miss or slight delay between the visual projectile impact and server damage packet acknowledgment.

### 4.2 Look-Vector Staff Anchor Extrapolation
* Core Issue: The position of continuous beam anchors (`ZoltraakCinematicBeamEntity`) is calculated dynamically using the caster's eye position and camera look vector.
* Technical Impact:
  * On multiplayer servers, remote players only receive periodic rotation and position synchronization packets. Rapid camera rotation by a remote caster can cause the origin of the beam to visibly lag behind the physical 3D staff model for other players observing the cast.

---

## 5. Ecosystem Coupling and Dependency Constraints

### 5.1 Upstream API Tight Coupling
* Core Issue: The mod relies directly on internal APIs from Iron's Spells 'n Spellbooks (ISS) and GeckoLib 4.
* Technical Impact:
  * Upstream breaking changes—such as modifications to ISS spell registration structures, school attribute mapping, or GeckoLib animation controller interfaces—require immediate code refactoring and prevent cross-version binary compatibility.

### 5.2 Zero-Vanilla-Particle Fallback Trade-Off
* Core Issue: To maintain anime-grade visual authenticity, all vanilla particle systems (`FLAME`, `LAVA`, `SMOKE`, `END_ROD`) have been completely purged from the codebase.
* Technical Impact:
  * All spell impacts, shockwaves, and barrier lattices rely entirely on custom Core GLSL shaders.
  * If a client runs on corrupted GPU drivers, unsupported virtual machines, or non-compliant OpenGL wrappers that fail shader compilation, visual effects will render completely invisible rather than degrading into fallback sprite particles.
