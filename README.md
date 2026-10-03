# Zoltraak: Cinematic Edition

<p align="center">
  <img src="logo.png" alt="Zoltraak Cinematic Logo" width="160" height="160" />
</p>

<p align="center">
  <strong>Ultra-Realistic & Cinematic Zoltraak (一般攻撃魔法 - ゾルトラーク) for Iron's Spells 'n Spellbooks</strong><br>
  Faithfully translated from high-end Unity VFX into Minecraft NeoForge 1.21.1 with 1:1 visual precision!
</p>

<p align="center">
  <a href="https://www.minecraft.net/"><img src="https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg" alt="Minecraft 1.21.1" /></a>
  <a href="https://neoforged.net/"><img src="https://img.shields.io/badge/Mod%20Loader-NeoForge%2021.1.248+-orange.svg" alt="NeoForge 21.1.248+" /></a>
  <a href="https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks"><img src="https://img.shields.io/badge/Requires-Iron's%20Spells%20'n%20Spellbooks-blue.svg" alt="Requires Iron's Spells" /></a>
  <a href="https://modrinth.com/mod/iris"><img src="https://img.shields.io/badge/Compatible-Iris%20%2F%20Oculus-9cf.svg" alt="Iris / Oculus Compatible" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-AGPL%20v3.0-blueviolet.svg" alt="License: AGPL-3.0" /></a>
</p>

---

## Overview / ภาพรวมม็อด


---

### Self-Contained Procedural Geometry & Core GLSL Shaders
(ระบบคำนวณเรขาคณิตสามมิติ และ Core Shader ประจำม็อดแบบ Standalone 100%)
เดิมทีระบบเคยพึ่งพาโมดูลภายนอกอย่าง Photon และ KilaGraph แต่ในสถาปัตยกรรมปัจจุบัน ม็อดได้รับการพัฒนาสู่ **Pure Procedural Geometry & Dedicated GLSL 150 Shader Architecture**:
- **Procedural Vector Mesh Engine (Blaze3D & JOML)**: คำนวณโครงสร้าง Mesh สดแบบ Real-Time ด้วยหลักคณิตศาสตร์เรขาคณิต (Dynamic Ribbons, Arcs, Spiral Rings, Tapered Cylinders) จึงให้ความลื่นไหลระดับภาพยนตร์โดยไม่ต้องพึ่งโมเดลภายนอก
- **Native Core GLSL 150 Shaders**:
  - `zoltraak_beam`: Procedural Sine Ripple & Photonic Tapering
  - `black_zoltraak_beam`: FBM (Fractal Brownian Motion) Dark Matter Soot & Screen-Derivative `fwidth` Particulate Grain
  - `great_zoltraak_plume`: Volumetric Mana Erosion Plumes
  - `corona`: Multi-axis Runic Aperture Flare
  - `lens`: Gravitational Lensing & Chromatic Aberration
- **SceneLens FBO Capture**: ระบบตรวจจับ Framebuffer ของตัวเกมผ่าน `glBlitFramebuffer` ส่งเข้า `SceneSampler` เพื่อสร้างมิติการบิดเบือนมิติ (Space Bending/Heat Refraction) ได้ด้วยตัวเกมเพียวๆ โดยไม่ต้องลง Mod หักเหแสงภายนอก
- **ZoltraakRenderPass (Deferred Queue)**: จัดคิวการเรนเดอร์แยกพิเศษ (`AFTER_SKY` -> `AFTER_LEVEL`) แก้ปัญหา Translucent Sorting และ Z-Fighting อย่างสมบูรณ์แบบ
- **Iris / Oculus Dynamic Compatibility (`ShaderCompatibility`)**: มีระบบตรวจจับ Shaderpack อัตโนมัติ ปรับระดับการเรนเดอร์ให้เข้ากับม็อด Shader (Oculus/Iris) โดยไม่เรนเดอร์ใน Shadow Pass และไม่มีปัญหาเกมแครช

---

## Installation / การติดตั้ง

1. ติดตั้ง **Minecraft 1.21.1**
2. ติดตั้ง **[NeoForge](https://neoforged.net/) 21.1.248+**
3. ติดตั้ง Mod ที่จำเป็น:
   - **[Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks)** (1.21.1)
   *(ไม่จำเป็นต้องติดตั้ง Photon หรือ KilaGraph เพิ่มเติมอีกต่อไป ม็อดมี Shader & Geometry Engine ในตัว 100%)*
4. *(รองรับ)* เข้ากันได้ดีกับ **Oculus / Iris Shaders**
5. ดาวน์โหลดไฟล์ `zoltraak_cinematic-neoforge-1.21.1-1.0.0.jar` แล้ววางลงในโฟลเดอร์ `.minecraft/mods`

---

## Building from Source / วิธีการบิลด์

โปรเจกต์นี้มีสคริปต์คอมไพล์อัตโนมัติด้วย **Python** และ **JDK 21**:

```bash
# 1. ตรวจสอบว่าติดตั้ง JDK 21 และ Python 3 เรียบร้อยแล้ว
python --version
javac --version

# 2. รันสคริปต์บิลด์
python build.py
```

ผลลัพธ์ไฟล์ JAR จะถูกสร้างขึ้นที่:
`./zoltraak_cinematic-neoforge-1.21.1-1.0.0.jar`

---

## License & Credits

- **Source Code License**: [GNU Affero General Public License v3.0 (AGPL-3.0)](LICENSE)
- **Mod Author**: Aling
