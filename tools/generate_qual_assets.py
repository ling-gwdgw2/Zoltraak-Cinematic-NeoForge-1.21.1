import os
import json
import math
from PIL import Image, ImageDraw

base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
geo_path = os.path.join(base_dir, 'src', 'main', 'resources', 'assets', 'zoltraak_cinematic', 'geo', 'entity', 'qual_boss.geo.json')
anim_path = os.path.join(base_dir, 'src', 'main', 'resources', 'assets', 'zoltraak_cinematic', 'animations', 'entity', 'qual_boss.animation.json')
tex_path = os.path.join(base_dir, 'src', 'main', 'resources', 'assets', 'zoltraak_cinematic', 'textures', 'entity', 'qual_boss.png')
glow_path = os.path.join(base_dir, 'src', 'main', 'resources', 'assets', 'zoltraak_cinematic', 'textures', 'entity', 'qual_boss_glowmask.png')
bbmodel_path = os.path.join(base_dir, 'qual_boss.bbmodel')

os.makedirs(os.path.dirname(geo_path), exist_ok=True)
os.makedirs(os.path.dirname(anim_path), exist_ok=True)
os.makedirs(os.path.dirname(tex_path), exist_ok=True)

print("Regenerating refined Qual Boss 3D Assets according to official reference...")

# 1. Cube definitions (clean joints without disconnected cube rotations)
cubes_data = [
    # Body
    {"name": "chest", "parent": "body", "size": [14, 8, 8], "origin": [-7, 26, -4]},
    {"name": "abdomen", "parent": "body", "size": [12, 6, 7], "origin": [-6, 20, -3.5]},
    {"name": "neck", "parent": "body", "size": [7, 2, 6], "origin": [-3.5, 34, -3]},

    # Fur Collar / Mantle
    {"name": "fur_main", "parent": "fur_collar", "size": [20, 6, 12], "origin": [-10, 31, -6]},
    {"name": "fur_top", "parent": "fur_collar", "size": [16, 3, 10], "origin": [-8, 36, -5]},
    {"name": "fur_shoulder_left", "parent": "fur_collar", "size": [4, 5, 10], "origin": [8, 31, -5]},
    {"name": "fur_shoulder_right", "parent": "fur_collar", "size": [4, 5, 10], "origin": [-12, 31, -5]},

    # Head & Mask
    {"name": "head_base", "parent": "head", "size": [8, 8, 8], "origin": [-4, 36, -4]},
    {"name": "mask", "parent": "head", "size": [9, 9, 1], "origin": [-4.5, 35.5, -4.8]},
    {"name": "mask_jaw", "parent": "head", "size": [7, 3, 1], "origin": [-3.5, 35, -5.3]},
    {"name": "hair_top", "parent": "head", "size": [9, 3, 9], "origin": [-4.5, 43.5, -4]},
    {"name": "hair_side_l", "parent": "head", "size": [2, 7, 5], "origin": [4, 36, -2]},
    {"name": "hair_side_r", "parent": "head", "size": [2, 7, 5], "origin": [-6, 36, -2]},

    # Beard
    {"name": "beard_upper", "parent": "beard", "size": [5, 6, 2], "origin": [-2.5, 30, -5.2]},
    {"name": "beard_mid", "parent": "beard", "size": [4, 6, 1.6], "origin": [-2, 24, -5.0]},
    {"name": "beard_tip", "parent": "beard", "size": [2, 5, 1], "origin": [-1, 19, -4.7]},

    # Massive Back Hair Mane
    {"name": "hair_mane_upper", "parent": "hair_mane", "size": [14, 11, 3], "origin": [-7, 33, 3.5]},
    {"name": "hair_mane_mid", "parent": "hair_mane", "size": [16, 11, 3], "origin": [-8, 22, 3.8]},
    {"name": "hair_mane_lower", "parent": "hair_mane", "size": [14, 9, 3], "origin": [-7, 13, 3.5]},
    {"name": "hair_mane_tips", "parent": "hair_mane", "size": [10, 6, 2.5], "origin": [-5, 7, 3.2]},

    # Left Horn Chain
    {"name": "horn_l1", "parent": "horn_left", "size": [3, 4, 3], "origin": [3.5, 43, -0.5]},
    {"name": "horn_l2", "parent": "horn_left_mid", "size": [2.5, 4, 2.5], "origin": [5.5, 46.5, -1.8]},
    {"name": "horn_l3", "parent": "horn_left_tip", "size": [2, 4, 2], "origin": [7.0, 49.5, -3.5]},

    # Right Horn Chain
    {"name": "horn_r1", "parent": "horn_right", "size": [3, 4, 3], "origin": [-6.5, 43, -0.5]},
    {"name": "horn_r2", "parent": "horn_right_mid", "size": [2.5, 4, 2.5], "origin": [-8.0, 46.5, -1.8]},
    {"name": "horn_r3", "parent": "horn_right_tip", "size": [2, 4, 2], "origin": [-9.0, 49.5, -3.5]},

    # Left Arm
    {"name": "left_upper_arm", "parent": "left_arm", "size": [4.5, 8, 5], "origin": [7, 25, -2.5]},
    {"name": "left_forearm_cube", "parent": "left_forearm", "size": [4, 7, 4], "origin": [7.2, 18, -2]},
    {"name": "left_wrist_fur", "parent": "left_forearm", "size": [5, 3.5, 5], "origin": [6.7, 17, -2.5]},
    {"name": "left_hand", "parent": "left_forearm", "size": [3, 5, 4], "origin": [7.7, 12, -2]},

    # Right Arm
    {"name": "right_upper_arm", "parent": "right_arm", "size": [4.5, 8, 5], "origin": [-11.5, 25, -2.5]},
    {"name": "right_forearm_cube", "parent": "right_forearm", "size": [4, 7, 4], "origin": [-11.2, 18, -2]},
    {"name": "right_wrist_fur", "parent": "right_forearm", "size": [5, 3.5, 5], "origin": [-11.7, 17, -2.5]},
    {"name": "right_hand", "parent": "right_forearm", "size": [3, 5, 4], "origin": [-10.7, 12, -2]},

    # Waist Belt
    {"name": "belt_cloth", "parent": "waist_belt", "size": [13, 3, 8], "origin": [-6.5, 18, -4]},
    {"name": "belt_fangs", "parent": "waist_belt", "size": [14, 2.5, 9], "origin": [-7, 16.5, -4.5]},
    {"name": "belt_chain", "parent": "waist_belt", "size": [2, 12, 2], "origin": [3.5, 6, -4.2]},

    # Robe Skirt
    {"name": "skirt_upper", "parent": "robe_skirt", "size": [14, 6, 9], "origin": [-7, 12, -4.5]},
    {"name": "skirt_mid", "parent": "robe_skirt", "size": [17, 6, 11], "origin": [-8.5, 6, -5.5]},
    {"name": "skirt_lower", "parent": "robe_skirt", "size": [20, 6, 14], "origin": [-10, 0, -7]}
]

# 2. UV Bin Packing
rects = []
for c in cubes_data:
    s = c["size"]
    w = math.ceil(2 * (s[2] + s[0]))
    h = math.ceil(s[2] + s[1])
    rects.append({'name': c['name'], 'w': w, 'h': h, 'cube': c})

rects.sort(key=lambda r: (r['w'] * r['h'], r['h']), reverse=True)

free_rects = [{'x': 0, 'y': 0, 'w': 256, 'h': 256}]
uv_map = {}

for r in rects:
    best_idx = -1
    best_score = 999999
    for i, fr in enumerate(free_rects):
        if fr['w'] >= r['w'] and fr['h'] >= r['h']:
            score = (fr['w'] - r['w']) * (fr['h'] - r['h'])
            if score < best_score:
                best_score = score
                best_idx = i
    if best_idx == -1:
        raise Exception(f"Could not pack cube {r['name']} into 256x256!")
    fr = free_rects.pop(best_idx)
    uv_map[r['name']] = [fr['x'], fr['y']]
    r['cube']['uv'] = [fr['x'], fr['y']]
    if fr['w'] > r['w']:
        free_rects.append({'x': fr['x'] + r['w'], 'y': fr['y'], 'w': fr['w'] - r['w'], 'h': r['h']})
    if fr['h'] > r['h']:
        free_rects.append({'x': fr['x'], 'y': fr['y'] + r['h'], 'w': fr['w'], 'h': fr['h'] - r['h']})

print(f"Packed {len(uv_map)} cubes into 256x256 UV layout successfully!")

# 3. Generate High-Res 256x256 Textures
tex_img = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
glow_img = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
d_tex = ImageDraw.Draw(tex_img)
d_glow = ImageDraw.Draw(glow_img)

# Palettes
C_SKIN_BASE = (69, 64, 64, 255)
C_SKIN_SHADOW = (53, 48, 48, 255)
C_SKIN_HIGHLIGHT = (85, 80, 80, 255)
C_STITCH = (26, 24, 24, 255)
C_STITCH_RED = (90, 32, 32, 255)
C_TATTOO = (18, 16, 16, 255)

C_FUR_BASE = (244, 240, 230, 255)
C_FUR_SHADOW = (226, 221, 208, 255)
C_FUR_DEEP = (198, 190, 174, 255)

C_MASK_BONE = (218, 208, 190, 255)
C_MASK_SHADOW = (184, 172, 152, 255)
C_MASK_STRIPE = (24, 22, 22, 255)
C_MASK_EYE = (14, 12, 12, 255)
C_MASK_TEETH = (252, 250, 244, 255)
C_GLOW_EYE = (235, 48, 255, 255)

C_HAIR_BASE = (216, 204, 176, 255)
C_HAIR_LIGHT = (238, 228, 204, 255)
C_HAIR_SHADOW = (176, 163, 134, 255)

C_HORN_BASE = (184, 170, 140, 255)
C_HORN_RIDGE = (118, 105, 78, 255)

C_ROBE_BASE = (36, 36, 40, 255)
C_ROBE_SHADOW = (26, 26, 30, 255)
C_ROBE_LIGHT = (46, 46, 52, 255)

C_CHAIN = (122, 128, 140, 255)
C_FANG = (237, 232, 220, 255)

def safe_putpixel(img, x, y, color):
    if 0 <= x < 256 and 0 <= y < 256:
        img.putpixel((x, y), color)

for c in cubes_data:
    name = c['name']
    u, v = c['uv']
    w, h, d = [int(math.ceil(x)) for x in c['size']]
    x1 = min(255, u + 2*(d+w) - 1)
    y1 = min(255, v + d + h - 1)

    if 'fur' in name or 'wrist' in name:
        d_tex.rectangle([u, v, x1, y1], fill=C_FUR_BASE)
        for y in range(v, y1 + 1, 2):
            for x in range(u, x1 + 1, 3):
                if (x + y) % 5 == 0:
                    safe_putpixel(tex_img, x, y, C_FUR_SHADOW)
                elif (x + y) % 7 == 0:
                    safe_putpixel(tex_img, x, y, C_FUR_DEEP)

    elif 'hair' in name or 'beard' in name:
        d_tex.rectangle([u, v, x1, y1], fill=C_HAIR_BASE)
        for x in range(u, x1 + 1):
            col = C_HAIR_LIGHT if x % 3 == 0 else (C_HAIR_SHADOW if x % 3 == 1 else C_HAIR_BASE)
            for y in range(v, y1 + 1):
                if (x + y) % 4 != 0:
                    safe_putpixel(tex_img, x, y, col)

    elif 'horn' in name:
        d_tex.rectangle([u, v, x1, y1], fill=C_HORN_BASE)
        for y in range(v, y1 + 1):
            if y % 3 == 0:
                for x in range(u, x1 + 1):
                    safe_putpixel(tex_img, x, y, C_HORN_RIDGE)

    elif 'skirt' in name or 'belt_cloth' in name:
        d_tex.rectangle([u, v, x1, y1], fill=C_ROBE_BASE)
        for x in range(u, x1 + 1):
            fold_c = C_ROBE_LIGHT if x % 4 == 0 else (C_ROBE_SHADOW if x % 4 == 2 else C_ROBE_BASE)
            for y in range(v, y1 + 1):
                safe_putpixel(tex_img, x, y, fold_c)

    elif 'chain' in name:
        d_tex.rectangle([u, v, x1, y1], fill=C_ROBE_SHADOW)
        for y in range(v, y1 + 1):
            if y % 2 == 0:
                for x in range(u, x1 + 1):
                    safe_putpixel(tex_img, x, y, C_CHAIN)

    elif 'fangs' in name:
        d_tex.rectangle([u, v, x1, y1], fill=C_ROBE_BASE)
        for x in range(u, x1 + 1):
            if x % 3 != 0:
                for y in range(v + d, y1 + 1):
                    safe_putpixel(tex_img, x, y, C_FANG)

    else:
        d_tex.rectangle([u, v, x1, y1], fill=C_SKIN_BASE)

# Mask Details (Front face)
mask_c = next(x for x in cubes_data if x['name'] == 'mask')
mu, mv = mask_c['uv']
mw, mh, md = [int(math.ceil(x)) for x in mask_c['size']]
fx, fy = mu + md, mv + md
d_tex.rectangle([fx, fy, fx + mw - 1, fy + mh - 1], fill=C_MASK_BONE)

# Mask Tiger Stripes
stripes = [
    (fx + 2, fy + 1), (fx + 2, fy + 2), (fx + 3, fy + 3),
    (fx + 6, fy + 1), (fx + 6, fy + 2), (fx + 5, fy + 3),
    (fx + 4, fy + 1), (fx + 4, fy + 2),
    (fx + 1, fy + 4), (fx + 2, fy + 5),
    (fx + 7, fy + 4), (fx + 6, fy + 5),
]
for sx, sy in stripes:
    if fx <= sx < fx + mw and fy <= sy < fy + mh:
        tex_img.putpixel((sx, sy), C_MASK_STRIPE)

# Mask Eyes
eye_pixels = [
    (fx + 2, fy + 3), (fx + 3, fy + 4),
    (fx + 6, fy + 3), (fx + 5, fy + 4),
]
for ex, ey in eye_pixels:
    tex_img.putpixel((ex, ey), C_MASK_EYE)
    glow_img.putpixel((ex, ey), C_GLOW_EYE)

# Mask Grin Jaw
jaw_c = next(x for x in cubes_data if x['name'] == 'mask_jaw')
ju, jv = jaw_c['uv']
jw, jh, jd = [int(math.ceil(x)) for x in jaw_c['size']]
j_fx, j_fy = ju + jd, jv + jd
d_tex.rectangle([j_fx, j_fy, j_fx + jw - 1, j_fy + jh - 1], fill=C_MASK_STRIPE)
for tx in range(j_fx, j_fx + jw):
    if (tx - j_fx) % 2 == 0:
        tex_img.putpixel((tx, j_fy), C_MASK_TEETH)
        tex_img.putpixel((tx, j_fy + 2), C_MASK_TEETH)
        glow_img.putpixel((tx, j_fy + 1), (180, 20, 220, 180))

# Chest Stitches
chest_c = next(x for x in cubes_data if x['name'] == 'chest')
cu, cv = chest_c['uv']
cw, ch, cd = [int(math.ceil(x)) for x in chest_c['size']]
c_fx, c_fy = cu + cd, cv + cd
for t in range(5):
    tex_img.putpixel((c_fx + 3 + t, c_fy + 2 + t), C_STITCH_RED)
    tex_img.putpixel((c_fx + 2 + t, c_fy + 2 + t), C_STITCH)
    tex_img.putpixel((c_fx + 4 + t, c_fy + 2 + t), C_STITCH)

# Arm Tattoos
for arm_name in ['left_upper_arm', 'right_upper_arm', 'left_forearm_cube', 'right_forearm_cube']:
    ac = next(x for x in cubes_data if x['name'] == arm_name)
    au, av = ac['uv']
    aw, ah, ad = [int(math.ceil(x)) for x in ac['size']]
    band_y = av + ad + 2
    for x in range(au, au + 2*(ad+aw)):
        tex_img.putpixel((x, band_y), C_TATTOO)
        tex_img.putpixel((x, band_y + 2), C_TATTOO)

tex_img.save(tex_path)
glow_img.save(glow_path)
print(f"Saved texture to: {tex_path}")
print(f"Saved glowmask to: {glow_path}")

# 4. Generate Bone Hierarchy for Geo JSON
bones = [
    {"name": "root", "pivot": [0, 0, 0]},
    {"name": "body", "parent": "root", "pivot": [0, 20, 0]},
    {"name": "fur_collar", "parent": "body", "pivot": [0, 34, 0]},
    {"name": "head", "parent": "body", "pivot": [0, 36, 0]},
    {"name": "beard", "parent": "head", "pivot": [0, 36, -4]},
    {"name": "hair_mane", "parent": "head", "pivot": [0, 42, 4]},
    {"name": "horn_left", "parent": "head", "pivot": [3.5, 43, 0]},
    {"name": "horn_left_mid", "parent": "horn_left", "pivot": [5.5, 46.5, -1.8]},
    {"name": "horn_left_tip", "parent": "horn_left_mid", "pivot": [7.0, 49.5, -3.5]},
    {"name": "horn_right", "parent": "head", "pivot": [-3.5, 43, 0]},
    {"name": "horn_right_mid", "parent": "horn_right", "pivot": [-5.5, 46.5, -1.8]},
    {"name": "horn_right_tip", "parent": "horn_right_mid", "pivot": [-7.0, 49.5, -3.5]},
    {"name": "left_arm", "parent": "body", "pivot": [9, 33, 0]},
    {"name": "left_forearm", "parent": "left_arm", "pivot": [9, 25, 0]},
    {"name": "right_arm", "parent": "body", "pivot": [-9, 33, 0]},
    {"name": "right_forearm", "parent": "right_arm", "pivot": [-9, 25, 0]},
    {"name": "waist_belt", "parent": "body", "pivot": [0, 20, 0]},
    {"name": "robe_skirt", "parent": "body", "pivot": [0, 18, 0]}
]

bone_map = {b['name']: b for b in bones}
for b in bones:
    b['cubes'] = []

for c in cubes_data:
    b = bone_map.get(c['parent'])
    if b is not None:
        cube_entry = {
            "origin": c["origin"],
            "size": c["size"],
            "uv": c["uv"]
        }
        b["cubes"].append(cube_entry)

geo_json = {
    "format_version": "1.12.0",
    "minecraft:geometry": [
        {
            "description": {
                "identifier": "geometry.qual_boss",
                "texture_width": 256,
                "texture_height": 256,
                "visible_bounds_width": 4.0,
                "visible_bounds_height": 5.0,
                "visible_bounds_offset": [0, 2.0, 0]
            },
            "bones": bones
        }
    ]
}

with open(geo_path, 'w', encoding='utf-8') as f:
    json.dump(geo_json, f, indent=2)
print(f"Saved refined Geo JSON to: {geo_path}")

print("Regeneration complete!")
