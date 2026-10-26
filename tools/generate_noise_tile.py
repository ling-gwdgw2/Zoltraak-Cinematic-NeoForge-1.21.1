"""
Procedural 512x512 Seamless Noise Texture Generator for Zoltraak Cinematic Edition.
Architected to supply multi-channel procedural volumetrics:
  - Channel R: Seamless Multi-octave FBM Perlin Noise (Macro cloud density & billowing masses)
  - Channel G: Seamless Cellular Worley / Voronoi F1 Distance Noise (Rounded cauliflower cloud lobes)
  - Channel B: High-pass Blue Noise / Dither (Micro-surface turbulent wisps & raymarch dither)
  - Channel A: 255 (Fully opaque)

Usage:
  python tools/generate_noise_tile.py
"""

import numpy as np
from PIL import Image

SIZE = 512

def generate_seamless_fbm(size=SIZE, base_grid=8, octaves=5, persistence=0.52):
    res = np.zeros((size, size), dtype=np.float32)
    weight = 0.0
    for oct in range(octaves):
        freq = base_grid * (2 ** oct)
        grid = np.random.RandomState(100 + oct * 37).rand(freq, freq).astype(np.float32)
        
        x = np.linspace(0, freq, size, endpoint=False)
        y = np.linspace(0, freq, size, endpoint=False)
        xi = np.floor(x).astype(int)
        yi = np.floor(y).astype(int)
        xf = (x - xi).astype(np.float32)
        yf = (y - yi).astype(np.float32)
        
        # Quintic smoothstep for smooth gradients: 6t^5 - 15t^4 + 10t^3
        u = xf * xf * xf * (xf * (xf * 6.0 - 15.0) + 10.0)
        v = yf * yf * yf * (yf * (yf * 6.0 - 15.0) + 10.0)
        
        x0 = xi % freq
        x1 = (xi + 1) % freq
        y0 = yi % freq
        y1 = (yi + 1) % freq
        
        c00 = grid[np.ix_(y0, x0)]
        c10 = grid[np.ix_(y0, x1)]
        c01 = grid[np.ix_(y1, x0)]
        c11 = grid[np.ix_(y1, x1)]
        
        top = c00 * (1 - u) + c10 * u
        bot = c01 * (1 - u) + c11 * u
        layer = top * (1 - v[:, None]) + bot * v[:, None]
        
        amp = persistence ** oct
        res += layer * amp
        weight += amp
    return res / weight

def generate_seamless_worley(size=SIZE, k=18, seed=777):
    rng = np.random.RandomState(seed)
    pts = rng.rand(k, k, 2).astype(np.float32)
    
    x = np.linspace(0, k, size, endpoint=False, dtype=np.float32)
    y = np.linspace(0, k, size, endpoint=False, dtype=np.float32)
    X, Y = np.meshgrid(x, y)
    
    cx = np.floor(X).astype(int)
    cy = np.floor(Y).astype(int)
    
    min_d = np.full((size, size), 10.0, dtype=np.float32)
    
    for dy in [-1, 0, 1]:
        for dx in [-1, 0, 1]:
            nx = (cx + dx) % k
            ny = (cy + dy) % k
            px = cx + dx + pts[ny, nx, 0]
            py = cy + dy + pts[ny, nx, 1]
            dist = np.sqrt((X - px)**2 + (Y - py)**2)
            min_d = np.minimum(min_d, dist)
            
    return np.clip((min_d / 1.05) * 175.0, 0, 255).astype(np.uint8)

def generate_seamless_blue_noise(size=SIZE, seed=2026):
    rng = np.random.RandomState(seed)
    wn = rng.rand(size, size).astype(np.float32)
    f = np.fft.fft2(wn)
    fshift = np.fft.fftshift(f)
    
    y, x = np.ogrid[-size//2:size//2, -size//2:size//2]
    r = np.sqrt(x*x + y*y)
    mask = 1.0 - np.exp(-(r / 18.0)**2)
    fshift_filtered = fshift * mask
    f_filtered = np.fft.ifftshift(fshift_filtered)
    blue = np.real(np.fft.ifft2(f_filtered))
    
    flat = blue.flatten()
    order = flat.argsort().argsort()
    return (order.reshape(size, size) / (size * size - 1) * 255.0).astype(np.uint8)

def main():
    r_fbm = generate_seamless_fbm()
    r_channel = np.clip(r_fbm * 140.0 + 48.0, 0, 255).astype(np.uint8)
    g_channel = generate_seamless_worley()
    b_channel = generate_seamless_blue_noise()
    a_channel = np.full((SIZE, SIZE), 255, dtype=np.uint8)
    
    rgba = np.stack([r_channel, g_channel, b_channel, a_channel], axis=-1)
    img = Image.fromarray(rgba, 'RGBA')
    
    target = 'src/main/resources/assets/zoltraak_cinematic/textures/effect/noise_tile.png'
    img.save(target)
    print(f'[SUCCESS] Generated custom 512x512 seamless noise texture at: {target}')

if __name__ == '__main__':
    main()
