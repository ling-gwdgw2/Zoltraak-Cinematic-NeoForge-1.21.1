package com.frierenflight.zoltraakcinematic.client.renderer;

/**
 * AAA-Grade Color Mapping Architecture for Zoltraak.
 * Provides high-energy LUT palettes for:
 * 1. DEMON_SLAYING (Holy Cyan & Overbright White) - Frieren & Fern Standard
 * 2. HUMAN_KILLING (Eldritch Dark Matter & Electric Magenta) - Qual's Original Curse
 */
public enum ZoltraakColorTheme {
    DEMON_SLAYING(
            // Core Beam (Overbright White)
            255, 255, 255, 255,
            // Inner Sheath (Hyper-Ionized Cyan)
            0, 240, 255, 230,
            // Outer Halo (Cosmic Royal Violet)
            138, 43, 226, 140,
            // Spiral 1 (Electric Cyan)
            0, 245, 255, 240,
            // Spiral 2 (Vibrant Violet Helix)
            190, 80, 255, 190,
            // Anamorphic Blade Flare
            220, 255, 255, 255,
            // Radial Starburst
            200, 250, 255, 255,
            // Shockwave Halo Ring
            180, 240, 255, 240,
            // Electric Arcs & Motes
            190, 245, 255, 220,
            // Muzzle Flash
            240, 255, 255, 255,
            // Absorption Core (KilaGraph Twilight Blue)
            20, 100, 220, 190,
            // Accretion Ring Tint (RGB)
            255, 255, 255
    ),
    HUMAN_KILLING(
            // Core Beam (Deep Void / Event Horizon)
            35, 5, 50, 255,
            // Inner Sheath (Electric Neon Magenta)
            235, 15, 220, 230,
            // Outer Halo (Deep Abyssal Purple)
            60, 0, 110, 160,
            // Spiral 1 (Crimson Scarlet Flare)
            255, 30, 80, 240,
            // Spiral 2 (Dark Magenta Helix)
            170, 0, 255, 200,
            // Anamorphic Blade Flare (Blinding Neon Magenta)
            255, 100, 240, 255,
            // Radial Starburst (Vivid Magenta / Violet)
            255, 80, 230, 255,
            // Shockwave Halo Ring (Dark Purple with Magenta Rim)
            200, 30, 240, 240,
            // Electric Arcs & Motes (High-Voltage Violet)
            255, 60, 240, 230,
            // Muzzle Flash (Crimson Flash)
            255, 40, 120, 255,
            // Absorption Core (Super-Black Singularity)
            10, 0, 20, 240,
            // Accretion Ring Tint (Crimson-Purple)
            255, 60, 220
    );

    // RGB + A components
    public final int coreR, coreG, coreB, coreA;
    public final int sheathR, sheathG, sheathB, sheathA;
    public final int haloR, haloG, haloB, haloA;
    public final int spiral1R, spiral1G, spiral1B, spiral1A;
    public final int spiral2R, spiral2G, spiral2B, spiral2A;
    public final int bladeR, bladeG, bladeB, bladeA;
    public final int starR, starG, starB, starA;
    public final int shockR, shockG, shockB, shockA;
    public final int arcR, arcG, arcB, arcA;
    public final int muzzleR, muzzleG, muzzleB, muzzleA;
    public final int absorbR, absorbG, absorbB, absorbA;
    public final int ringR, ringG, ringB;

    ZoltraakColorTheme(
            int coreR, int coreG, int coreB, int coreA,
            int sheathR, int sheathG, int sheathB, int sheathA,
            int haloR, int haloG, int haloB, int haloA,
            int spiral1R, int spiral1G, int spiral1B, int spiral1A,
            int spiral2R, int spiral2G, int spiral2B, int spiral2A,
            int bladeR, int bladeG, int bladeB, int bladeA,
            int starR, int starG, int starB, int starA,
            int shockR, int shockG, int shockB, int shockA,
            int arcR, int arcG, int arcB, int arcA,
            int muzzleR, int muzzleG, int muzzleB, int muzzleA,
            int absorbR, int absorbG, int absorbB, int absorbA,
            int ringR, int ringG, int ringB
    ) {
        this.coreR = coreR; this.coreG = coreG; this.coreB = coreB; this.coreA = coreA;
        this.sheathR = sheathR; this.sheathG = sheathG; this.sheathB = sheathB; this.sheathA = sheathA;
        this.haloR = haloR; this.haloG = haloG; this.haloB = haloB; this.haloA = haloA;
        this.spiral1R = spiral1R; this.spiral1G = spiral1G; this.spiral1B = spiral1B; this.spiral1A = spiral1A;
        this.spiral2R = spiral2R; this.spiral2G = spiral2G; this.spiral2B = spiral2B; this.spiral2A = spiral2A;
        this.bladeR = bladeR; this.bladeG = bladeG; this.bladeB = bladeB; this.bladeA = bladeA;
        this.starR = starR; this.starG = starG; this.starB = starB; this.starA = starA;
        this.shockR = shockR; this.shockG = shockG; this.shockB = shockB; this.shockA = shockA;
        this.arcR = arcR; this.arcG = arcG; this.arcB = arcB; this.arcA = arcA;
        this.muzzleR = muzzleR; this.muzzleG = muzzleG; this.muzzleB = muzzleB; this.muzzleA = muzzleA;
        this.absorbR = absorbR; this.absorbG = absorbG; this.absorbB = absorbB; this.absorbA = absorbA;
        this.ringR = ringR; this.ringG = ringG; this.ringB = ringB;
    }

    public static ZoltraakColorTheme fromId(int id) {
        return id == 1 ? HUMAN_KILLING : DEMON_SLAYING;
    }
}
