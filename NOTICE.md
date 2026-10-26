# Notices & Copyright Information

**Zoltraak: Cinematic Edition (NeoForge 1.21.1)**  
Copyright (c) 2026 Aling (`ling-gwdgw2`) and Zoltraak Cinematic Contributors.

---

## 1. Primary License

This project is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)**.  
You may view the full license terms in the [LICENSE](file:///c:/Users/vivo9/Desktop/Minecraft%20my%20mod/Zoltraak%20Cinematic%20-%20NeoForge%201.21.1/LICENSE) file in the root directory of this repository.

---

## 2. Clean-Room Implementation & Originality Statement

As of **Version 1.0.4** and all subsequent releases:

- **100% Original Codebase**: All Java source code, procedural vector mesh algorithms (Blaze3D / JOML), GLSL 150 core shaders, and shader JSON configurations are 100% original, clean-room creations developed independently.
- **Dedicated Custom Shaders**:
  - `zoltraak_beam` (`.fsh`, `.vsh`, `.json`): Custom super-Gaussian core formulation ($e^{-(y/\text{core})^4}$), supersonic longitudinal ionization ripples, and diamond arrowhead tapering.
  - `black_zoltraak_beam` (`.fsh`, `.vsh`, `.json`): Pure obsidian void core, crackling ash-lightning filaments, and silver-lavender boundary ionization.
  - `great_zoltraak_white` (`.fsh`, `.vsh`, `.json`): 3D cylindrical volumetric raymarching, Fresnel facing modulation, and plasma discharge.
  - `great_zoltraak_black` (`.fsh`, `.vsh`, `.json`): 3D cylindrical volumetric void raymarching and charcoal-violet soot turbulence.
  - `great_zoltraak_plume` (`.fsh`, `.vsh`, `.json`): Lateral fluid plume wave dynamics with dual-tone support and dielectric spark breaking.
  - `corona` (`.fsh`, `.vsh`, `.json`): Demonic gravitational singularity accretion corona with logarithmic inflow spiral and photonic photon sphere.
  - `rapid_zoltraak_bullet` (`.fsh`, `.vsh`, `.json`): Supersonic aerodynamic laser-needle projectile shader with diamond taper.
  - `magic_circle` / `black_magic_circle` (`.fsh`, `.vsh`, `.json`): Procedural runic mandala and anime magic circles.
  - `zoltraak_impact` (`.fsh`, `.vsh`, `.json`): Dynamic crater shockwave, expanding plasma bloom, and dust billow.
  - `cinematic_flare` (`.fsh`, `.vsh`, `.json`): Anamorphic camera-aligned diffraction glare.
  - `defense_barrier` (`.fsh`, `.vsh`, `.json`): Hexagonal energy barrier with impact wave dispersion.
  - `gargantua` (`.fsh`, `.vsh`, `.json`): Gravitational lensing black hole accretion raymarching.
  - `unsealing_fire_column` (`.fsh`, `.vsh`, `.json`): Ritual arcane fire pillar and runic rings.
- **Custom Textures & Spell Icons**:
  - `zoltraak_glow.png`: Procedurally synthesized 512x512 HDR Gaussian-quartic radial bloom texture.
  - `zoltraak_magic_circle.png`: High-definition 2048x2048 anime magic circle with clean luminous alpha channel.
  - `defense.png`: Custom-crafted 64x64 hexagonal mana crystal barrier spell icon.
- **100% Original Synthesized Audio**:
  - All Zoltraak sound effects (`zoltraak_charge.ogg`, `zoltraak_fire.ogg`, `zoltraak_great_fire.ogg`, and `zoltraak_impact.ogg`) are 100% mathematically synthesized in-house using digital signal processing (DSP), FM/additive resonance, and spatial stereo convolution.

---

## 3. Historical Prototype Clarification (v1.0.0 – v1.0.2fix)

During early experimental prototyping prior to version 1.0.4, preliminary shader prototypes were tested that drew inspiration from *Frieren Spells* by sho09 (licensed under the MIT License):

```text
Copyright (c) 2026 sho9 (Frieren Spells - MIT License)
```

To establish complete visual originality, architectural autonomy, and strict licensing hygiene, **all legacy prototypes, borrowed shaders, atlas textures, and obsolete configurations were completely decommissioned, deleted, and replaced from scratch in Version 1.0.4**. The current codebase contains zero lines of legacy shader code or borrowed textures.

---

## 4. Dependencies & Third-Party Platform Acknowledgments

This mod integrates with and builds upon several open-source libraries and platforms:

- **[Iron's Spells 'n Spellbooks](https://github.com/iron431/Irons-Spells-n-Spellbooks)** by Iron431  
  Provides the spellcasting framework, school system, and spellbook mechanics.
- **[NeoForge](https://neoforged.net/)** by the NeoForged Team  
  Open-source Minecraft modding API and loader.
- **[Minecraft](https://www.minecraft.net/)** (c) 2011–2026 Mojang AB / Microsoft Corporation.

---

## 5. Non-Commercial Fan Work Disclaimer

*Zoltraak: Cinematic Edition* is a non-commercial, transformative fan modification inspired by the magic system in the manga and anime series **"Sousou no Frieren" (Frieren: Beyond Journey's End)**, created by **Kanehito Yamada** and illustrated by **Tsukasa Abe**.

All referenced spell names, visual inspirations, and lore elements belong to their respective copyright holders. This mod is not affiliated with, endorsed by, or associated with Shogakukan, Madhouse, or any of their affiliates.
