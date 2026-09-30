# Zoltraak Cinematic Edition — Mod Wiki & Compendium

> *A cinematic, high-fidelity Iron's Spells 'n Spellbooks addon faithfully recreating the iconic magic of **Sousou no Frieren** (Frieren: Beyond Journey's End) in Minecraft NeoForge 1.21.1.*

---

## 🌐 Language Navigation / สารบัญภาษา
- [🇬🇧 English Documentation](#-english-documentation)
  - [Magic School & Attributes](#1-magic-school--custom-attributes)
  - [Spell Compendium](#2-spell-compendium)
  - [Boss: Qual, the Elder Sage](#3-boss-qual-the-elder-sage-of-corruption)
  - [Sealing Monolith & Ritual](#4-quals-ancient-sealing-monolith)
  - [Equipment, Grimoires & Artifacts](#5-equipment-grimoires--artifacts)
  - [Endgame Frieren Build](#6-endgame-build-synergy)
- [🇹🇭 คู่มือภาษาไทย (Thai Documentation)](#-คู่มือภาษาไทย-thai-documentation)
  - [สายเวทและคุณสมบัติพิเศษ](#1-สายเวทมนตร์-magic-school--คุณสมบัติพิเศษ)
  - [สารานุกรมเวทมนตร์ 6 บท](#2-สารานุกรมเวทมนตร์-spells)
  - [บอส: มหาจอมเวทควาล](#3-มหาจอมเวทควาล-qual-elder-sage-of-corruption)
  - [ศิลาสะกดมารและพิธีกรรมปลดผนึก](#4-ศิลาสะกดมาร-quals-sealing-monolith)
  - [สมบัติ คัมภีร์เวท และอาวุธระดับตำนาน](#5-สมบัติ-คัมภีร์เวท-และอาวุธระดับตำนาน)
  - [การจัดเซ็ตอุปกรณ์ขั้นสุดยอด](#6-การจัดเซ็ตอุปกรณ์ขั้นสุดยอด-endgame-build)

---

# 🇬🇧 English Documentation

## 1. Magic School & Custom Attributes

### School: Ordinary Magic (`school.zoltraak_cinematic.ordinary_magic`)
Mankind's standardized magic system, reverse-engineered and perfected by humanity and Frieren after researching Qual's devastating Killing Magic. Characterized by near-instantaneous casting cadence, hypersonic velocity, razor-sharp precision, and catastrophic energy detonation.

### ⚔️ Custom Attributes
| Attribute | Resource Location | Function & Gameplay Impact |
| :--- | :--- | :--- |
| **Zoltraak Spell Power** | `zoltraak_cinematic:zoltraak_spell_power` | Multiplicative power scaling specifically for all Zoltraak-class spells (Beams & Barrages). Multiplies on top of universal Spell Power. |
| **Zoltraak Resistance** | `zoltraak_cinematic:zoltraak_magic_resist` | Percentage damage reduction against incoming Zoltraak beams, dark thorn barrages, and boss attacks. Prevents lethal one-shots. |

---

## 2. Spell Compendium

### 1. Zoltraak: Frieren Blast (一般攻撃魔法 - ゾルトラーク)
> *"Ordinary Offensive Magic perfected by Frieren. Discharges an ultra-dense photonic particle beam with hypersonic penetration and a devastating 12-block terminal shockwave."*

- **Spell ID**: `zoltraak_cinematic:zoltraak`
- **Cast Type**: Instant (`INSTANT`, Cast Time = 0)
- **Rarity**: Epic (`EPIC`)
- **Max Level**: 10
- **Cooldown**: 4.0 seconds
- **Mana Cost**: 200 (+6 per level)
- **Base Power**: 80 Damage (+8 per level, scales with Spell Power)
- **Range**: 64 blocks
- **Terminal Shockwave Radius**: 12 blocks
- **Visual & Audio FX**:
  - Tri-layer rotating white-gold celestial rune rings with dynamic acceleration.
  - Anamorphic optical lens flare with chromatic fringes and twin-core piercing photonic beam.
  - Realistic camera shake, optical bloom, and radial shockwave dome at the impact point.

---

### 2. Corrupted Zoltraak: Qual (人を殺す魔法 - クヴァール)
> *"Qual's original forbidden Killing Magic. Unleashes an Event-Horizon dark singularity beam wrapped in violent electric magenta lightning that shreds armor and withers flesh."*

- **Spell ID**: `zoltraak_cinematic:corrupted_zoltraak`
- **Cast Type**: Instant (`INSTANT`, Cast Time = 0)
- **Rarity**: Epic (`EPIC`)
- **Max Level**: 10
- **Cooldown**: 4.5 seconds
- **Mana Cost**: 250 (+7 per level)
- **Base Power**: 100 Damage (+9 per level, scales with Spell Power)
- **Range**: 64 blocks
- **Special Perk**: **100% Armor-Piercing** & inflicts **Wither II** for 6.0 seconds.
- **Visual & Audio FX**:
  - Pitch-black void aperture with rotating abyssal runes.
  - Heavy bass void-resonance audio that rattles the battlefield upon discharge.

---

### 3. Zoltraak: Grand Phalanx Barrage (連続魔法爆撃 - フェルン)
> *"Fern's signature high-speed bombardment. Deploys a magnificent 24-circle celestial matrix, unleashing continuous guided Zoltraak light lances converging upon the crosshair."*

- **Spell ID**: `zoltraak_cinematic:zoltraak_barrage`
- **Cast Type**: Continuous Hold-to-Channel (`CONTINUOUS`, up to 10s channel duration)
- **Rarity**: Epic (`EPIC`)
- **Max Level**: 1
- **Cooldown**: 1.5 seconds
- **Mana Cost**: 250 Base Activation (+4 per level)
- **Base Power**: 5.0 Damage per bullet (24 bullets per volley = **120.0 Base Damage per volley**)
- **Firing Cadence**: Opens circle blossom in 8 ticks (~0.4s), then fires 1 full volley of 24 projectiles every 10 ticks (0.5s = **2 volleys / 48 bullets per second**) as long as right-click is held!
- **Special Mechanics**:
  - **Parabolic Celestial Wings**: 24 rune circles arranged in an anime-accurate 3D parabolic wing formation spanning $\pm 4.25\text{m}$ horizontally, elevated up to $+2.40\text{m}$ overhead.
  - **Staggered Diamond Matrix**: 5 staggered rows (5-4-6-4-5) that prevent circles from occluding one another.
  - **Dynamic Parallax Convergence**: Each circle automatically calculates its 3D pitch and yaw normals to tilt and converge upon the 48-meter target focal point.
  - **3D Guided Trajectory**: Bullets dynamically curve towards the target dummy or targeted entity in flight.
  - **First-Person Viewport Optimization**: Centers of vision remain unobstructed for pinpoint aiming.

---

### 4. Corrupted Zoltraak: Grand Phalanx Barrage (暗黒連弾魔法)
> *"Manifests 24 dark singularity circles, continuously firing armor-piercing dark matter thorns that dissolve barriers and wither targets into ash."*

- **Spell ID**: `zoltraak_cinematic:corrupted_barrage`
- **Cast Type**: Continuous Hold-to-Channel (`CONTINUOUS`)
- **Rarity**: Epic (`EPIC`)
- **Max Level**: 1
- **Cooldown**: 1.8 seconds
- **Mana Cost**: 300 Base
- **Base Power**: 7.0 Damage per bullet (24 bullets per volley = **168.0 Base Damage per volley**)
- **Special Perk**: Barrier-piercing dark thorns with guaranteed Wither debuff.

---

### 5. Defensive Magic: Hexagonal Aegis (防御魔法)
> *"Modern standard defensive magic co-developed by humanity and Frieren to counteract Killing Magic. Manifests an active geodesic honeycomb barrier while leaving both hands free for combat."*

- **Spell ID**: `zoltraak_cinematic:defense`
- **Cast Type**: Instant (`INSTANT`, Cast Time = 0)
- **Rarity**: Uncommon – Rare (`UNCOMMON` / `RARE`)
- **Max Level**: 10
- **Cooldown**: 10.0 seconds
- **Mana Cost**: 200 (+10 per level)
- **Duration**: 15 – 25 seconds (300 – 500 ticks, scales with level)
- **Absorption Capacity**: 150 – 350+ Damage (scales with level and Spell Power)
- **Key Features**:
  - **100% Free Hands**: Cast Zoltraak, attack with weapons, or sprint without interrupting the barrier!
  - **Dual Stances**:
    - **Directional Aegis (Normal Cast)**: 19 hexagonal plates aligned dynamically with player sightline.
    - **Omnidirectional 360° Spherical Dome (Sneak Cast)**: 92 hexagonal plates enclosing the caster in a full sphere protecting from all angles (sky and ground).
  - **Dynamic FX**: Brilliant white-cyan impact flashes, amethyst chime resonation, progressive stress cracks under 40% health, and floating explosive shard disintegration upon expiration.

---

### 6. Flight Magic: Demonic Transmutation (飛行魔法 - 人類の魔法)
> *"One of humanity's greatest magical triumphs: reverse-engineering demonic flight magic into a standard skill for modern mages, granting free 3D aerial navigation."*

- **Spell ID**: `zoltraak_cinematic:flight`
- **Cast Type**: Instant Toggle On/Off (`INSTANT`)
- **Rarity**: Rare (`RARE`, Levels 1–5)
- **Cooldown**: 1.0 second
- **Ignition Cost**: 25 Mana
- **Controls**:
  - **Right-Click**: Toggle flight on/off.
  - **Keybind 'V'**: Instant toggle directly from equipped spellbook without holding the scroll.
  - **Flight Maneuvers**: Space (Ascend), Shift (Descend), Ctrl+W (High-speed aerial sprint).
- **Mana Consumption Mechanics**:
  - **Base Hover Drain**: 50 mana/s (Lv 1) down to 40 mana/s (Lv 5).
  - **Speed Multiplier**: Sprinting accelerates drain up to **2.5x – 6.0x+** (150–300+ mana/s).
  - **Altitude Multiplier**: Flying higher into the cloud layer increases drain exponentially up to **5.0x – 15.0x+**.
  - **Safety Parachute**: If mana depletes in midair, emergency Slow Falling (3.5s) activates automatically to prevent fatal impacts.
  - **Fall Damage Immunity**: Complete immunity to fall damage while flight is active.

---

## 3. Boss: Qual, the Elder Sage of Corruption

> *"The Elder Sage of Corruption (腐敗の賢老 クヴァール) — the demon who invented Killing Magic (Zoltraak) and slaughtered 40% of all mages and 70% of adventurers during the war before being sealed for 80 years by the Hero Party."*

### Boss Attributes
- **Health**: **1,500 HP** (+200 HP per additional player in 48-block radius)
- **Armor / Toughness**: 30 Armor / 10 Toughness
- **Magic Resistances**: +40% General Spell Resist / **+40% Zoltraak Resistance**
- **Spell Power**: +40% General / **+50% Zoltraak Spell Power**
- **Speed**: 0.35 (True 3D Omnidirectional Flight)
- **Immunities**: Fire Immune, Fall Damage Immune

### 4 Combat Phases
1. **Phase 1: Probing Stance (100% – 75% HP)**: Hovers 5–12m above ground, circling at 16m range, firing single Corrupted Zoltraak beams every 3.5s.
2. **Phase 2: Demonic Barrage Matrix (75% – 40% HP)**: Ascends into the upper atmosphere, manifests 24 dark singularity circles, and unleashes rapid Corrupted Barrages.
3. **Phase 3: Tactical Adaptation (40% – 20% HP)**: Activates Demonic Dispersion Shield (35% chance to deflect 50% damage) and casts faster every 2.0s.
4. **Phase 4: Cataclysmic Overdrive (< 20% HP)**: Charges for 3.0s and discharges the **Original Cataclysmic Zoltraak** (4m diameter beam, 95 base damage, 80-block piercing line, 20-block detonation shockwave).

### Anti-Barrier AI
- Detects directional shields and uses **Shadow Blink** to teleport directly behind the player.
- Detects full domes and focuses concentrated heavy beam fire to rapidly exhaust barrier capacity.

### Mythic Boss Drops
- `zoltraak_cinematic:corruption_core` × 1 (Guaranteed)
- `zoltraak_cinematic:horn_of_corruption` × 2–4 (Scales with raid player count)

---

## 4. Qual's Ancient Sealing Monolith

- **Block ID**: `zoltraak_cinematic:qual_sealing_stone`
- **Hardness**: Indestructible, Emits Level 6 Light
- **Crafting Recipe**:
  ```
  [ Amethyst Shard ]  [ Crying Obsidian ]  [ Amethyst Shard ]
  [ Crying Obsidian ] [ Nether Star ]      [ Crying Obsidian ]
  [ Deepslate ]       [ Crying Obsidian ]  [ Deepslate ]
  ```
- **Unsealing Ritual (5.0s Sequence)**:
  - Right-clicking triggers a 5-second ritual:
    - **0.0s – 2.0s**: Blue runes turn blood-red, vibrating violently.
    - **2.0s – 3.5s**: Four cardinal seal chains shatter.
    - **3.5s – 5.0s**: A 24-block dark pillar pierces the heavens with thunderclaps.
    - **5.0s**: The monolith detonates, and Qual emerges into the sky!

---

## 5. Equipment, Grimoires & Artifacts

### 1. Frieren's Staff (คทาของฟรีเรน — Mythic Endgame Weapon)
The legendary thousand-year staff of Frieren the Slayer, forged to channel elven mana of catastrophic scale.

- **Item ID**: `zoltraak_cinematic:frieren_staff`
- **Slot**: Mainhand Weapon / Two-Handed Staff
- **Rarity**: Epic (`EPIC`, Fire Resistant)
- **Attributes**:
  - **All Spell Power**: **+45%**
  - **Zoltraak Spell Power**: **+35%** (Combined **+80%** for Zoltraak spells)
  - **Max Mana**: **+500**
  - **Cooldown Reduction**: **+20%**
  - **Attack Damage**: 6.0
- **Post-Qual Mythic Recipe (Crafting Table 3x3)**:
  ```
  [ Corruption Core ]   [ Nether Star ]      [ Horn of Corruption ]
  [ Divine Pearl ]      [ Frosted Helve ]    [ Dragonskin ]
  [ Frosted Helve ]     [ Netherite Ingot ]  [       -      ]
  ```

---

### 2. Grimoire of Ordinary Magic (บันทึกมหาเวทโจมตีสามัญ)
Humanity's foundational grimoire compiling the complete theorems of Ordinary Offensive Magic. Upgraded from an Enchanted Diamond Spellbook into an elite 10-slot casting focus.

- **Item ID**: `zoltraak_cinematic:ordinary_grimoire`
- **Slot**: Curios `Spellbook` or Off-hand
- **Rarity**: Epic (`EPIC`, Fire Resistant)
- **Attributes**:
  - **Spell Inscription Slots**: **10 Slots**
  - **Zoltraak Spell Power**: **+30%**
  - **All Spell Power**: **+15%**
  - **Max Mana**: **+300**
  - **Cooldown Reduction**: **+15%**
- **Diamond Tier Upgrade Recipe**:
  ```
  [ Amethyst Shard ]  [ Arcane Rune ]        [ Amethyst Shard ]
  [ Arcane Ingot ]    [ Diamond Spellbook ]  [ Arcane Ingot ]
  [ Amethyst Shard ]  [ Arcane Cloth ]       [ Amethyst Shard ]
  ```

---

### 3. Grimoire of the Elder Sage (บันทึกมหาเวทของควาล)
Qual's original forbidden spellbook containing raw calculations of Killing Magic.

- **Item ID**: `zoltraak_cinematic:elder_sage_grimoire`
- **Slot**: Curios `Spellbook` or Off-hand
- **Rarity**: Epic (`EPIC`, Fire Resistant)
- **Attributes**:
  - **Spell Inscription Slots**: **12 Slots**
  - **Zoltraak Spell Power**: **+50%**
  - **All Spell Power**: **+20%**
  - **Max Mana**: **+1,000**
  - **Cooldown Reduction**: **+30%**
  - **Cast Time Reduction**: **+20%**
- **Upgrade Recipe**:
  ```
  [ Horn of Corruption ] [ Netherite Ingot ]   [ Horn of Corruption ]
  [ Horn of Corruption ] [ Ordinary Grimoire ] [ Horn of Corruption ]
  [ Horn of Corruption ] [ Corruption Core ]   [ Horn of Corruption ]
  ```

---

### 4. Ring of the Mirror Lotus (แหวนดอกบัวกระจกเงา)
Silver ring engraved with the mirror lotus flower gifted by Himmel to Frieren, symbolizing eternal devotion.

- **Item ID**: `zoltraak_cinematic:mirror_lotus_ring`
- **Slot**: Curios `Ring`
- **Rarity**: Epic (`EPIC`, Fire Resistant)
- **Attributes**:
  - **Zoltraak Spell Power**: **+25%**
  - **Cooldown Reduction**: **+15%**
  - **Max Mana**: **+150**
  - **Mana Regen**: **+1.0 / sec**
- **Recipe**:
  ```
  [ - ]              [ Mana Ring ]          [ - ]
  [ Cast Time Ring ] [ Horn of Corruption ] [ Silver Ring ]
  [ - ]              [ Cooldown Ring ]      [ - ]
  ```

---

### 5. Mana Concealment Pendant (จี้มนตราซ่อนมานา)
Pendant embodying the elven technique taught by Flamme: compressing mana leakage to complete imperceptibility.

- **Item ID**: `zoltraak_cinematic:mana_concealment_pendant`
- **Slot**: Curios `Necklace`
- **Rarity**: Epic (`EPIC`, Fire Resistant)
- **Attributes**:
  - **Zoltraak Resistance**: **+20%**
  - **Cast Time Reduction**: **+10%**
  - **Max Mana**: **+200**
- **Recipe**:
  ```
  [ - ]           [ Iron Nugget ]        [ - ]
  [ Iron Nugget ] [ Shriving Stone ]     [ Iron Nugget ]
  [ - ]           [ Horn of Corruption ] [ - ]
  ```

---

### 6. Qual's Core of Corruption (แก่นแท้แห่งการเน่าเปื่อย)
Black hole singularity core harvested directly from Qual's chest cavity.

- **Item ID**: `zoltraak_cinematic:corruption_core`
- **Slot**: Curios `Necklace`
- **Rarity**: Epic (`EPIC`, Fire Resistant)
- **Attributes**:
  - **Zoltraak Spell Power**: **+35%**
  - **Max Mana**: **+350**
  - **Mana Regen**: **+2.5 / sec**

---

## 6. Endgame Build Synergy

When fully equipped with the complete Frieren Artifact Set:
- **Main Hand**: Frieren's Staff (+45% All, +35% Zoltraak, +500 Mana, +20% CDR)
- **Off-hand / Curios**: Ordinary Grimoire (+15% All, +30% Zoltraak, +300 Mana, +15% CDR)
- **Ring**: Ring of the Mirror Lotus (+25% Zoltraak, +150 Mana, +15% CDR, +1.0 Regen)
- **Necklace**: Mana Concealment Pendant (+20% Zoltraak Resist, +10% Cast Time Reduction, +200 Mana)

### Net Stat Summary:
- **Total Zoltraak Spell Power**: **+90%** (Nearly double base output!)
- **Total Universal Spell Power**: **+60%**
- **Total Max Mana Bonus**: **+1,150 Mana**
- **Total Cooldown Reduction**: **+50%** (Zoltraak fires every 2.0s; Barrage fires continuously with instant recovery!)
- **Total Zoltraak Resistance**: **+20%**

---
---

# 🇹🇭 คู่มือภาษาไทย (Thai Documentation)

## 1. สายเวทมนตร์ (Magic School) & คุณสมบัติพิเศษ

### School: Ordinary Magic (人類の魔法 - เวทมนตร์สามัญ)
เวทมนตร์สามัญที่ถูกพัฒนาขึ้นโดยมนุษยชาติและฟรีเรนจากการวิจัยเวทมนตร์สังหารของควาล โดดเด่นด้านความเร็วในการร่าย, ความแม่นยำสูง, ลำแสงความเร็วเหนือเสียง (Hypersonic Light Beam) และการระเบิดพลังงานเวทแบบเข้มข้น

### ⚔️ Custom Attributes
| คุณสมบัติ (Attribute) | Resource Location | คำอธิบาย & ผลลัพธ์ในเกม |
| :--- | :--- | :--- |
| **Zoltraak Spell Power** | `zoltraak_cinematic:zoltraak_spell_power` | เพิ่มพลังโจมตีเวทมนตร์ตระกูล Zoltraak โดยเฉพาะ ทวีคูณความเสียหายทั้งลำแสงปกติและห่าฝนกระสุน |
| **Zoltraak Resistance** | `zoltraak_cinematic:zoltraak_magic_resist` | ลดทอนความเสียหายที่ได้รับจากเวทมนตร์ Zoltraak ป้องกันการถูกโจมตีแบบ One-Shot จากเวทมนตร์ประเภทเดียวกัน |

---

## 2. สารานุกรมเวทมนตร์ (Spells)

### 1. Zoltraak: Frieren Blast (一般攻撃魔法 - ゾルトラーク)
> *"เวทมนตร์โจมตีสามัญที่ถูกปรับปรุงจนสมบูรณ์แบบโดยฟรีเรน ยิงลำแสงโฟตอนบริสุทธิ์ทะลวงเป้าหมายด้วยความเร็วแสง พร้อมคลื่นช็อคเวฟทำลายล้างรอบจุดกระทบ"*

- **Spell ID**: `zoltraak_cinematic:zoltraak`
- **ประเภทการร่าย (Cast Type)**: ร่ายทันที (`INSTANT`)
- **ความหายากขั้นต่ำ (Min Rarity)**: มหากาพย์ (`EPIC`)
- **เลเวลสูงสุด (Max Level)**: 10
- **คูลดาวน์ (Cooldown)**: 4.0 วินาที
- **ค่าใช้จ่ายมานา (Mana Cost)**: 200 มานา (+6 ต่อเลเวล)
- **พลังโจมตีพื้นฐาน (Base Power)**: 80 ดาเมจ (+8 ต่อเลเวล)
- **ระยะหวังผล (Range)**: 64 บล็อก
- **รัศมีช็อคเวฟ (Terminal Blast Radius)**: 12 บล็อก
- **คุณสมบัติภาพ (VFX/SFX)**:
  - วงเวทย์รันสีขาว-ทองหมุนวน 3 ชั้น
  - ลำแสงคู่ (Twin Core) พร้อมแสงสะท้อนรอบตัวผู้ร่าย
  - ระบบหน้าจอสั่น (Camera Shake) และจอวาบแสง (Screen Flash) แบบ Cinematic
  - ช็อคเวฟทรงโดมระเบิดที่จุดกระทบ พร้อมเสียงกระหึ่มแบบ 3 มิติ

---

### 2. Corrupted Zoltraak: Qual (腐敗の賢老 クヴァール - 人を殺す魔法)
> *"เวทมนตร์สังหารมนุษย์ดั้งเดิมของควาล มหาจอมเวทผู้คิดค้นเวทมนตร์สังหาร ปลดปล่อยลำแสงหลุมดำมวลยิ่งยวดพร้อมประกายอัศนีสีม่วงเข้ม ทะลวงเกราะและกัดกร่อนสิ่งมีชีวิต"*

- **Spell ID**: `zoltraak_cinematic:corrupted_zoltraak`
- **ประเภทการร่าย (Cast Type)**: ร่ายทันที (`INSTANT`)
- **ความหายากขั้นต่ำ (Min Rarity)**: มหากาพย์ (`EPIC`)
- **เลเวลสูงสุด (Max Level)**: 10
- **คูลดาวน์ (Cooldown)**: 4.5 วินาที
- **ค่าใช้จ่ายมานา (Mana Cost)**: 250 มานา (+7 ต่อเลเวล)
- **พลังโจมตีพื้นฐาน (Base Power)**: 100 ดาเมจ (+9 ต่อเลเวล)
- **ระยะหวังผล (Range)**: 64 บล็อก
- **สถานะผิดปกติ (Debuff/Perk)**: **ทะลวงเกราะ (Armor-Piercing)** และติดพิษเหี่ยวเฉา (**Wither II** นาน 6 วินาที)
- **คุณสมบัติภาพ (VFX/SFX)**:
  - วงเวทย์อักขระทมิฬสีดำ-ม่วง (Event Horizon Magic Circle)
  - ลำแสงสีดำทมิฬล้อมรอบด้วยเปลวไฟพลาสมาสีม่วงเข้ม (Dark Matter Plasma)
  - เสียงยิงหนักแน่นลึกและเสียงสะท้อนแบบ Void Resonator

---

### 3. Zoltraak: Grand Phalanx Barrage (フェルン - 連続魔法爆撃)
> *"กระบวนท่าระดมยิงห่าฝนกระสุนเวทมนตร์ความเร็วสูงของเฟิร์น กางวงเวทย์ลอยฟ้า 24 วงล้อมรอบตัวผู้ร่าย ยิงกระสุนแสงนำวิถีพร้อมกัน พุ่งกระหน่ำใส่จุดเล็ง"*

- **Spell ID**: `zoltraak_cinematic:zoltraak_barrage`
- **ประเภทการร่าย (Cast Type)**: กดค้างเพื่อยิงต่อเนื่อง (`CONTINUOUS`, สูงสุด 10 วินาที)
- **ความหายากขั้นต่ำ (Min Rarity)**: มหากาพย์ (`EPIC`)
- **เลเวลสูงสุด (Max Level)**: 1
- **คูลดาวน์ (Cooldown)**: 1.5 วินาที
- **ค่าใช้จ่ายมานา (Mana Cost)**: 250 มานา (+4 ต่อเลเวล)
- **พลังโจมตีพื้นฐาน (Base Power)**: 5 ดาเมจ ต่อนัด (24 นัดต่อระลอก = **120.0 ดาเมจ ต่อระลอก**)
- **จังหวะการยิง (Cadence)**: กางวงเวทย์ 8 Ticks (~0.4s) จากนั้นยิงระลอกละ 24 นัดทุกๆ 10 Ticks (0.5 วินาที = **2 ระลอก / 48 นัดต่อวินาที**) ตราบใดที่ยังกดค้างและมีมานาเพียงพอ
- **ระบบพิเศษ (Smart Mechanics)**:
  - **Parabolic Celestial Wings**: วงเวทย์ 24 วงจัดเรียงเป็นทรงปีกนกเรขาคณิต 3 มิติกว้าง $\pm 4.25$ เมตร ยกสูงถึง $+2.40$ เมตร
  - **Staggered Diamond Matrix**: การสับหว่าง 5 แถว (5-4-6-4-5) ป้องกันขอบวงเวทย์ซ้อนทับกัน
  - **Dynamic Parallax Convergence**: วงเวทย์ทั้ง 24 วงปรับองศาเอียงหันเข้าหาเป้าหมายระยะ 48 เมตรโดยอัตโนมัติ
  - **3D Guided Projectiles**: กระสุนเวทโค้งเข้าหาเป้าหมายที่เล็งอย่างแม่นยำ
  - **Perspective Viewport Optimization**: เปิดช่องว่างตรงกลางในมุมมองบุคคลที่หนึ่ง ไม่บังเป้าเล็ง

---

### 4. Corrupted Zoltraak: Grand Phalanx Barrage (暗黒連弾魔法)
> *"การปลดปล่อยวงเวทย์หลุมดำทมิฬ 24 วง ระดมยิงหนามสสารมืดพุ่งทำลายล้างม่านบาเรียเวทมนตร์และกัดกร่อนศัตรูให้สลายกลายเป็นเถ้าถ่าน"*

- **Spell ID**: `zoltraak_cinematic:corrupted_barrage`
- **ประเภทการร่าย (Cast Type)**: กดค้างเพื่อยิงต่อเนื่อง (`CONTINUOUS`)
- **ความหายากขั้นต่ำ (Min Rarity)**: มหากาพย์ (`EPIC`)
- **เลเวลสูงสุด (Max Level)**: 1
- **คูลดาวน์ (Cooldown)**: 1.8 วินาที
- **ค่าใช้จ่ายมานา (Mana Cost)**: 300 มานา
- **พลังโจมตีพื้นฐาน (Base Power)**: 7 ดาเมจ ต่อนัด (24 นัด = **168.0 ดาเมจ ต่อระลอก**)
- **สถานะผิดปกติ (Debuff/Perk)**: หนามสสารมืดติดสถานะ **Wither** และมีเอฟเฟกต์ Dark Singularity ทะลวงบาเรีย

---

### 5. Defensive Magic: เวทป้องกันมาตรฐาน (防御魔法)
> *"หนึ่งในสอง 'เวทมนตร์พื้นฐาน' ของจอมเวทยุคปัจจุบันร่วมกับโซลทราค ถูกวิจัยและพัฒนาขึ้นโดยมนุษยชาติร่วมกับฟรีเรน โดยมีเป้าหมายหลักในการป้องกันและหักล้างเวทมนตร์สังหารโดยเฉพาะ"*

- **Spell ID**: `zoltraak_cinematic:defense`
- **ประเภทการร่าย (Cast Type)**: ร่ายทันทีแบบกำหนดระยะเวลาคงอยู่ (`INSTANT`, Cast Time = 0)
- **ความหายากขั้นต่ำ (Min Rarity)**: พิเศษ (`UNCOMMON` - `RARE`)
- **เลเวลสูงสุด (Max Level)**: 10
- **คูลดาวน์ (Cooldown)**: 10.0 วินาที
- **ค่าใช้จ่ายมานา (Mana Cost)**: 200 มานา (+10 ต่อเลเวล)
- **ระยะเวลาคงอยู่ (Duration)**: 15 - 25 วินาที (300 - 500 Ticks สเกลตามเลเวล)
- **ความจุการป้องกัน (Absorption Capacity)**: 150 - 350+ ดาเมจ (สเกลตาม `Spell Power` และเลเวลเวท)
- **จุดเด่นสำคัญ (Key Feature)**:
  - **มือเป็นอิสระ 100%!** ผู้เล่นสามารถร่ายเวทโจมตีสวนกลับ หรือสวิงอาวุธทะลุม่านบาเรียของตนเองได้อย่างอิสระ
  - **2 โหมดการใช้งาน**:
    - **แผงป้องกันทิศทางเดียว (Directional Aegis - ร่ายปกติ)**: แผ่นหกเหลี่ยม 19 แผ่นหันตามมุมสายตา
    - **โดมกลมสมบูรณ์แบบ 360 องศา (Full Spherical Dome - ร่ายขณะย่อตัว Sneak)**: แผ่นหกเหลี่ยม 92 แผ่นปกป้องรอบทิศทั้งเบื้องสูงและเบื้องต่ำ
  - **Dynamic FX**: แสงแฟลร์สีขาว-ฟ้าเมื่อปะทะ, รอยแตกร้าวเมื่อ HP < 40%, และสะเก็ดเศษแก้วระเบิดกระจายสลายตัวเมื่อบาเรียแตก

---

### 6. Flight Magic: เวทบินแห่งมนุษยชาติ (飛行魔法)
> *"หนึ่งในความสำเร็จสูงสุดของการวิจัยเวทมนตร์ โดยการถอดรหัสเวทบินของเผ่ามารจนกลายเป็นเวทมนตร์มาตรฐาน ช่วยให้ผู้ร่ายบินและพุ่งทะยานในอากาศ 360 องศาได้อย่างอิสระ"*

- **Spell ID**: `zoltraak_cinematic:flight`
- **ประเภทการร่าย (Cast Type)**: ร่ายทันทีแบบเปิด/ปิดสลับสถานะ (`INSTANT`, Toggle On/Off)
- **ความหายาก (Rarity)**: หายาก (`RARE`, เลเวล 1–5)
- **คูลดาวน์ (Cooldown)**: 1.0 วินาที
- **ค่าจุดชนวนเริ่มต้น (Ignition Cost)**: 25 มานา
- **การควบคุม (Controls)**:
  - **คลิกขวา**: เปิด/ปิดเวทบิน
  - **ปุ่มลัด 'V'**: กดเปิด/ปิดได้ทันทีจากสมุดเวทที่สวมใส่
  - **ปุ่มบังคับ**: Space (บินขึ้น), Shift (ลดระดับ), Ctrl+W (พุ่งทะยานความเร็วสูง)
- **กลไกการเผาผลาญมานา (Mana Drain Mechanics)**:
  - **อัตราลอยตัวพื้นฐาน**: 50 มานา/วินาที (Lv 1) ถึง 40 มานา/วินาที (Lv 5)
  - **ตัวคูณความเร็ว (Speed Penalty)**: พุ่งบินความเร็วสูงเพิ่มอัตราผลาญขึ้น **2.5x – 6.0x+** (150–300+ มานา/วินาที)
  - **ตัวคูณความสูง (High Sky Penalty)**: บินสู่ชั้นเมฆเพิ่มอัตราผลาญขึ้น **5.0x – 15.0x+**
  - **ร่มชูชีพฉุกเฉิน**: หากมานาหมดกลางอากาศ จะมีบัฟ Slow Falling 3.5 วินาทีช่วยร่อนลงพื้นอย่างปลอดภัย
  - **ภูมิคุ้มกันดาเมจตกจากที่สูง**: ไม่ได้รับ Fall Damage 100% ขณะเวทบินทำงาน

---

## 3. มหาจอมเวทควาล (Boss: Qual, Elder Sage of Corruption)

> *"จอมเวทผู้ยิ่งใหญ่แห่งเผ่ามาร 'ควาลผู้เฒ่าเน่าเปื่อย' (腐敗の賢老 クヴァール) ผู้คิดค้นเวทมนตร์สังหาร (โซลทราค) จนสังหารเหล่าจอมเวทไปถึง 40% และนักผจญภัยถึง 70% ในยุคสงคราม ก่อนจะถูกผู้กล้าฮิมเมลและฟรีเรนผนึกไว้นานกว่า 80 ปี"*

### สถิติบอส (Boss Attributes)
- **พลังชีวิตพื้นฐาน (Health)**: **1,500 HP** (+200 HP ต่อผู้เล่นทุกคนในระยะ 48 บล็อก)
- **เกราะ (Armor / Toughness)**: 30 Armor / 10 Toughness
- **ความต้านทานเวทมนตร์**: +40% เวททั่วไป / **+40% Zoltraak Resistance**
- **พลังโจมตีเวทมนตร์**: +40% เวททั่วไป / **+50% Zoltraak Spell Power**
- **ความเร็วบิน (Flying Speed)**: 0.35 (บิน 3 มิติ 360 องศา)
- **ภูมิคุ้มกัน**: ไม่ตกจากที่สูง, ภูมิคุ้มกันไฟ

### พฤติกรรมการต่อสู้ 4 เฟส
1. **Phase 1: Probing Stance (100% – 75% HP)**: ลอยตัวคุมเชิง บินวนรอบตัวผู้เล่นที่ระยะ 16 บล็อก ยิง Corrupted Zoltraak ทุก 3.5 วินาที
2. **Phase 2: Demonic Barrage Matrix (75% – 40% HP)**: ไต่ระดับความสูง กางวงเวทย์หลุมดำ 24 วง ระดมยิง Corrupted Barrage
3. **Phase 3: Tactical Adaptation (40% – 20% HP)**: เปิดเกราะ Demonic Dispersion Shield (โอกาส 35% สลายดาเมจลง 50%) และเร่งความเร็วร่ายเวททุก 2.0 วินาที
4. **Phase 4: Cataclysmic Overdrive (< 20% HP)**: ชาร์จพลังงาน 3.0 วินาที ยิง **Original Cataclysmic Zoltraak** (ลำแสงกว้าง 4 เมตร, 95 ดาเมจพื้นฐาน, ทะลวง 80 บล็อก, ระเบิด 20 บล็อก)

### ระบบสติปัญญาต่อต้านบาเรีย
- หากผู้เล่นกางโล่ทิศทางเดียว ควาลจะใช้ **Shadow Blink** เทเลพอร์ตข้ามไปข้างหลังผู้เล่นทันที
- หากผู้เล่นกางโดม 360 องศา ควาลจะระดมยิงบีมหนักเจาะลงบนโดมเพื่อเผาผลาญหลอด Absorption Capacity ให้แตกสลาย

### ของดรอประดับตำนาน
- `zoltraak_cinematic:corruption_core` × 1 (การันตี)
- `zoltraak_cinematic:horn_of_corruption` × 2–4 ชิ้น

---

## 4. ศิลาสะกดมาร (Qual's Sealing Monolith)

- **Block ID**: `zoltraak_cinematic:qual_sealing_stone`
- **ความทนทาน**: ไร้เทียมทาน (Indestructible) เปล่งแสงระดับ 6
- **สูตรคราฟต์อัญเชิญบอส**:
  ```
  [ Amethyst Shard ]  [ Crying Obsidian ]  [ Amethyst Shard ]
  [ Crying Obsidian ] [ Nether Star ]      [ Crying Obsidian ]
  [ Deepslate ]       [ Crying Obsidian ]  [ Deepslate ]
  ```
- **พิธีกรรมปลดผนึก (5.0s Sequence)**:
  - คลิกขวาเพื่อเริ่มพิธีกรรม:
    - **0.0s – 2.0s**: อักขระเวทสีฟ้าของฟรีเรนเปลี่ยนเป็นสีเลือด สั่นสะเทือนด้วยความถี่สูง
    - **2.0s – 3.5s**: โซ่ตรวนเวทมนตร์ 4 ทิศแตกสลาย
    - **3.5s – 5.0s**: เสาพลังงานมืดพุ่งทะยานสู่ท้องฟ้า 24 บล็อกพร้อมเสียงฟ้าร้อง
    - **5.0s**: ศิลาแตกออก ควาลลอยตัวขึ้นสู่ท้องฟ้าและเริ่มการต่อสู้!

---

## 5. สมบัติ คัมภีร์เวท และอาวุธระดับตำนาน

### 1. Frieren's Staff (คทาของฟรีเรน — อาวุธระดับตำนาน End Game)
คทาเวทมนตร์โบราณประจำตัวของฟรีเรนจอมเวทเอลฟ์ผู้มีอายุยืนยาวนับพันปี สร้างขึ้นเพื่อรองรับและขยายพลังมานามหาศาลโดยเฉพาะ

- **Item ID**: `zoltraak_cinematic:frieren_staff`
- **ประเภท**: อาวุธคทาเวทมนตร์ (Staff - ถือสองมือหรือมือหลัก)
- **ความหายาก**: มหากาพย์ (`EPIC`, ทนไฟไม่ไหม้ลาวา)
- **คุณสมบัติสถานะ**:
  - **All Spell Power**: **+45%** (ขยายพลังเวททุกสายในเกม)
  - **Zoltraak Spell Power**: **+35%** (โบนัสซ้อนทับเฉพาะสายโซลทราค รวมสูงถึง **+80%**)
  - **Max Mana**: **+500** หน่วย
  - **Cooldown Reduction**: **+20%**
  - **Attack Damage**: 6.0
- **สูตรคราฟต์ระดับตำนาน (Post-Qual Mythic End Game Recipe)**:
  ```
  [ Corruption Core ]   [ Nether Star ]      [ Horn of Corruption ]
  [ Divine Pearl ]      [ Frosted Helve ]    [ Dragonskin ]
  [ Frosted Helve ]     [ Netherite Ingot ]  [       -      ]
  ```

---

### 2. Grimoire of Ordinary Magic (บันทึกมหาเวทโจมตีสามัญ)
คัมภีร์เวทมนตร์มาตรฐานของจอมเวทยุคปัจจุบัน รวบรวมทฤษฎีและโครงสร้างการร่ายของมหาเวทสังหารที่มนุษยชาติและฟรีเรนร่วมกันพัฒนา อัปเกรดจากสมุดเวทมนตร์ Enchanted Spell Book (Diamond Tier) เพื่อก้าวสู่ระดับจอมเวทชั้นสูง 10 ช่อง

- **Item ID**: `zoltraak_cinematic:ordinary_grimoire`
- **ประเภท**: สมุดเวทมนตร์ (Spellbook - 10 ช่องใส่เวท)
- **ความหายาก**: มหากาพย์ (`EPIC`, ทนไฟไม่ไหม้ลาวา)
- **คุณสมบัติสถานะ**:
  - **จำนวนช่องเวทมนตร์**: **10 ช่อง**
  - **Zoltraak Spell Power**: **+30%**
  - **All Spell Power**: **+15%**
  - **Max Mana**: **+300** หน่วย
  - **Cooldown Reduction**: **+15%**
- **สูตรคราฟต์อัปเกรด (Diamond Tier Upgrade Recipe)**:
  ```
  [ Amethyst Shard ]  [ Arcane Rune ]        [ Amethyst Shard ]
  [ Arcane Ingot ]    [ Diamond Spellbook ]  [ Arcane Ingot ]
  [ Amethyst Shard ]  [ Arcane Cloth ]       [ Amethyst Shard ]
  ```

---

### 3. Grimoire of the Elder Sage (บันทึกมหาเวทของควาล)
คัมภีร์เวทต้องห้ามดั้งเดิมของควาล บันทึกสูตรโครงสร้างดั้งเดิมของมหาเวทสังหารมนุษย์ (อัปเกรดจาก Ordinary Grimoire ด้วยวัตถุดิบดรอปจากบอสควาล)

- **Item ID**: `zoltraak_cinematic:elder_sage_grimoire`
- **ประเภท**: สมุดเวทมนตร์ (Spellbook - 12 ช่องใส่เวท)
- **ความหายาก**: มหากาพย์ (`EPIC`, ทนไฟ)
- **คุณสมบัติสถานะ**:
  - **จำนวนช่องเวทมนตร์**: **12 ช่อง**
  - **Zoltraak Spell Power**: **+50%**
  - **All Spell Power**: **+20%**
  - **Max Mana**: **+1,000** หน่วย
  - **Cooldown Reduction**: **+30%**
  - **Cast Time Reduction**: **+20%**
- **สูตรคราฟต์อัปเกรด**:
  ```
  [ Horn of Corruption ] [ Netherite Ingot ]   [ Horn of Corruption ]
  [ Horn of Corruption ] [ Ordinary Grimoire ] [ Horn of Corruption ]
  [ Horn of Corruption ] [ Corruption Core ]   [ Horn of Corruption ]
  ```

---

### 4. Ring of the Mirror Lotus (แหวนดอกบัวกระจกเงา)
แหวนเงินสลักลายดอกบัวกระจกเงาที่ฮิมเมลมอบให้แก่ฟรีเรน สัญลักษณ์แห่งความผูกพันชั่วนิรันดร์และจิตใจที่สงบนิ่ง

- **Item ID**: `zoltraak_cinematic:mirror_lotus_ring`
- **ประเภท**: เครื่องประดับ Curios (ช่องสวมใส่: `Ring`)
- **ความหายาก**: มหากาพย์ (`EPIC`, ทนไฟ)
- **คุณสมบัติสถานะ**:
  - **Zoltraak Spell Power**: **+25%**
  - **Cooldown Reduction**: **+15%**
  - **Max Mana**: **+150** หน่วย
  - **Mana Regen**: **+1.0** หน่วย/วินาที
- **สูตรคราฟต์**:
  ```
  [ - ]              [ Mana Ring ]          [ - ]
  [ Cast Time Ring ] [ Horn of Corruption ] [ Silver Ring ]
  [ - ]              [ Cooldown Ring ]      [ - ]
  ```

---

### 5. Mana Concealment Pendant (จี้มนตราซ่อนมานา)
จี้สร้อยคอที่บรรจุเคล็ดวิชาของฟลามเม่และฟรีเรนในการบีบอัดและซ่อนเร้นออร่ามานาจนดูเหมือนมนุษย์ธรรมดา

- **Item ID**: `zoltraak_cinematic:mana_concealment_pendant`
- **ประเภท**: เครื่องประดับ Curios (ช่องสวมใส่: `Necklace`)
- **ความหายาก**: มหากาพย์ (`EPIC`, ทนไฟ)
- **คุณสมบัติสถานะ**:
  - **Zoltraak Resistance**: **+20%**
  - **Cast Time Reduction**: **+10%**
  - **Max Mana**: **+200** หน่วย
- **สูตรคราฟต์**:
  ```
  [ - ]           [ Iron Nugget ]        [ - ]
  [ Iron Nugget ] [ Shriving Stone ]     [ Iron Nugget ]
  [ - ]           [ Horn of Corruption ] [ - ]
  ```

---

### 6. Qual's Core of Corruption (แก่นแท้แห่งการเน่าเปื่อย)
แก่นผลึกหลุมดำที่สกัดจากทรวงอกของควาล กักเก็บพลังเวทสังหารบริสุทธิ์

- **Item ID**: `zoltraak_cinematic:corruption_core`
- **ประเภท**: เครื่องประดับ Curios (ช่องสวมใส่: `Necklace`)
- **ความหายาก**: มหากาพย์ (`EPIC`, ทนไฟ)
- **คุณสมบัติสถานะ**:
  - **Zoltraak Spell Power**: **+35%**
  - **Max Mana**: **+350** หน่วย
  - **Mana Regen**: **+2.5** หน่วย/วินาที

---

## 6. การจัดเซ็ตอุปกรณ์ขั้นสุดยอด (Endgame Build)

เมื่อสวมใส่อุปกรณ์ครบเซ็ตของฟรีเรน:
1. **คทา Frieren's Staff** (มือหลัก): พลังเวททั่วไป +45%, พลัง Zoltraak +35%, มานา +500, ลดคูลดาวน์ 20%
2. **สมุดเวท Ordinary Grimoire** (มือรอง/Curios): พลังเวททั่วไป +15%, พลัง Zoltraak +30%, มานา +300, ลดคูลดาวน์ 15%
3. **แหวน Mirror Lotus Ring** (Curios Ring): พลัง Zoltraak +25%, ลดคูลดาวน์ 15%, มานา +150, ฟื้นมานา +1.0
4. **จี้ Mana Concealment Pendant** (Curios Necklace): ต้านทาน Zoltraak +20%, ลดเวลาร่าย 10%, มานา +200

### สรุปผลรวมสเตตัสสุทธิ (Full Frieren Set):
- **Zoltraak Spell Power รวม**: **+90%** (เกือบสองเท่าของพลังปกติ!)
- **All Spell Power รวม**: **+60%**
- **Max Mana เพิ่มขึ้น**: **+1,150** หน่วย
- **Cooldown Reduction รวม**: **+50%** (ยิง Zoltraak ซ้ำได้ทุกๆ 2.0 วินาที และยิง Barrage ได้อย่างราบรื่นต่อเนื่อง!)
- **Zoltraak Resistance**: **+20%**
