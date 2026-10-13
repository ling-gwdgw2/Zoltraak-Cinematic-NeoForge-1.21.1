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

## 4. Long-Term Macro Vision: Minecraft 26.x and Vulkan Migration

As the Minecraft rendering ecosystem transitions away from legacy OpenGL 3.2 Core profiles toward modern graphics APIs (Vulkan, DirectX 12, Metal), the visual and mathematical architecture of Zoltraak: Cinematic Edition will undergo fundamental restructuring.

### 4.1 Transition from OpenGL 3.2 / GLSL 150 to SPIR-V and Vulkan
* SPIR-V Bytecode Compilation:
  * Transition from runtime GLSL 150 text parsing (`#version 150`) to offline-compiled SPIR-V binary modules.
  * Unified shader pipeline with cross-compilation support via SPIRV-Cross for multi-backend execution.
* Explicit Pipeline State Objects (PSO):
  * Pre-baking blend modes, depth-stencil states, and vertex layouts into immutable PSOs, removing OpenGL state validation overhead from the render loop.

### 4.2 Compute Shader Relativistic Raymarching
* Decoupling from the Fragment Stage:
  * In OpenGL 150, the Kerr black hole raymarching is constrained to fullscreen fragment shaders (`gargantua.fsh`). This incurs heavy rasterization penalties and pixel overdraw.
  * In the Vulkan architecture, raymarching of the Thorne geodesic equations ($d^2u/d\phi^2 + u = 3Mu^2$) will be offloaded entirely to asynchronous GPU Compute Shaders.
* Variable Rate Shading (VRS) and Tiled Compute:
  * Compute dispatch will utilize 16x16 tile workgroups with hierarchical bounding box checks, executing geodesic leapfrog calculations only on pixels intersecting the gravitational lensing envelope.
  * High-density accretion disk simulations will execute independently of the game frame rate on asynchronous compute queues.

### 4.3 Declarative Render Graph Architecture
* Modern Render Graph Integration:
  * Replacing custom mixin hooks (`GameRendererMixin`) and procedural draw calls (`ZoltraakRenderPass`) with a declarative, barrier-synchronized Render Graph.
  * Native compatibility with modern graphics mods (VulkanMod, Iris Vulkan backend, Sodium Vulkan ports).
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
| Phase 1: Polish & Optimization | MC 1.21.1 / NeoForge 21.1.x | OpenGL 3.2 (GLSL 150) | Quality presets, shader warmup cache, boss parry mechanics, memory optimizations. |
| Phase 2: Engine Expansion | MC 1.22.x / NeoForge Next | OpenGL 3.2 / Early WBOIT | Order-independent transparency, modular runic matrix, Macht and Aura boss encounters. |
| Phase 3: Modern API Preparation | MC 25.x / Next-Gen Loader | Hybrid OpenGL / Vulkan Stubs | SPIR-V shader toolchain, Render Graph abstraction, Virtual Threaded kinematics. |
| Phase 4: Full Vulkan Architecture | MC 26.x / Vulkan Native | Vulkan 1.3 / SPIR-V | Compute shader geodesic raymarching, immutable PSOs, asynchronous compute queues, full raytracing interoperability. |
