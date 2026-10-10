import sys, math, random, subprocess

WIDTH = 540
HEIGHT = 960
FPS = 30
DURATION = 8 # seconds for seamless loop
TOTAL_FRAMES = FPS * DURATION

# Create raw directory if not exists
import os
os.makedirs("app/src/main/res/raw", exist_ok=True)
output_file = "app/src/main/res/raw/login_bg_video.mp4"

# Nodes for plexus
random.seed(42)
num_nodes = 28
nodes = []
for i in range(num_nodes):
    nodes.append({
        'base_x': random.uniform(0.05, 0.95),
        'base_y': random.uniform(0.05, 0.95),
        'speed_x': random.uniform(0.5, 1.5) * random.choice([-1, 1]),
        'speed_y': random.uniform(0.5, 1.5) * random.choice([-1, 1]),
        'radius': random.uniform(2.0, 4.5),
        'color': random.choice([(14, 165, 233), (2, 132, 199), (56, 189, 248), (148, 163, 184)])
    })

# Floating glass cards
cards = [
    {'x': 0.15, 'y': 0.12, 'w': 220, 'h': 340, 'angle': 22, 'speed': 1.0, 'depth': 0.8},
    {'x': 0.85, 'y': 0.08, 'w': 240, 'h': 360, 'angle': -18, 'speed': 1.2, 'depth': 0.9},
    {'x': 0.12, 'y': 0.88, 'w': 230, 'h': 350, 'angle': -25, 'speed': 0.9, 'depth': 0.85},
    {'x': 0.88, 'y': 0.82, 'w': 250, 'h': 380, 'angle': 20, 'speed': 1.1, 'depth': 0.95},
]

# Start ffmpeg process piping raw RGB24
cmd = [
    'ffmpeg', '-y',
    '-f', 'rawvideo',
    '-vcodec', 'rawvideo',
    '-s', f'{WIDTH}x{HEIGHT}',
    '-pix_fmt', 'rgb24',
    '-r', str(FPS),
    '-i', '-',
    '-c:v', 'libx264',
    '-pix_fmt', 'yuv420p',
    '-preset', 'fast',
    '-crf', '22',
    output_file
]

proc = subprocess.Popen(cmd, stdin=subprocess.PIPE)

# Pre-calculate base background gradient (light cyan/white)
# Top: (230, 243, 250), Bottom: (250, 252, 254)
bg_base = bytearray(WIDTH * HEIGHT * 3)
for y in range(HEIGHT):
    factor = y / HEIGHT
    r = int(232 + factor * 22)
    g = int(244 + factor * 10)
    b = int(251 + factor * 4)
    row_bytes = bytes([r, g, b] * WIDTH)
    bg_base[y * WIDTH * 3 : (y + 1) * WIDTH * 3] = row_bytes

def draw_line(buf, x0, y0, x1, y1, r, g, b, alpha):
    # Bresenham-like fast line with alpha blending
    dx = abs(x1 - x0)
    dy = abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx - dy
    inv_a = 1.0 - alpha
    while True:
        if 0 <= x0 < WIDTH and 0 <= y0 < HEIGHT:
            idx = (y0 * WIDTH + x0) * 3
            buf[idx] = int(buf[idx] * inv_a + r * alpha)
            buf[idx+1] = int(buf[idx+1] * inv_a + g * alpha)
            buf[idx+2] = int(buf[idx+2] * inv_a + b * alpha)
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 > -dy:
            err -= dy
            x0 += sx
        if e2 < dx:
            err += dx
            y0 += sy

def draw_filled_circle(buf, cx, cy, radius, r, g, b, alpha):
    r_int = int(radius) + 1
    inv_a = 1.0 - alpha
    for dy in range(-r_int, r_int + 1):
        py = cy + dy
        if 0 <= py < HEIGHT:
            for dx in range(-r_int, r_int + 1):
                px = cx + dx
                dist_sq = dx*dx + dy*dy
                if dist_sq <= radius * radius:
                    if 0 <= px < WIDTH:
                        falloff = (1.0 - (dist_sq / (radius * radius))) * alpha
                        inv_f = 1.0 - falloff
                        idx = (py * WIDTH + px) * 3
                        buf[idx] = int(buf[idx] * inv_f + r * falloff)
                        buf[idx+1] = int(buf[idx+1] * inv_f + g * falloff)
                        buf[idx+2] = int(buf[idx+2] * inv_f + b * falloff)

def draw_rotated_glass_card(buf, cx, cy, w, h, angle_deg, time_phase):
    # 4 corners
    rad = math.radians(angle_deg)
    cos_a = math.cos(rad)
    sin_a = math.sin(rad)
    hw = w / 2
    hh = h / 2
    corners = [
        (-hw, -hh), (hw, -hh), (hw, hh), (-hw, hh)
    ]
    rot_pts = []
    for (x, y) in corners:
        rx = int(cx + (x * cos_a - y * sin_a))
        ry = int(cy + (x * sin_a + y * cos_a))
        rot_pts.append((rx, ry))
    
    # Draw glossy glass borders
    for i in range(4):
        p1 = rot_pts[i]
        p2 = rot_pts[(i + 1) % 4]
        # Cyan-tinted bright glass edge
        draw_line(buf, p1[0], p1[1], p2[0], p2[1], 186, 230, 253, 0.75)
        # Inner highlight
        draw_line(buf, p1[0]+1, p1[1]+1, p2[0]+1, p2[1]+1, 255, 255, 255, 0.6)

for frame in range(TOTAL_FRAMES):
    t = frame / TOTAL_FRAMES
    time_rad = t * 2 * math.pi
    
    frame_buf = bytearray(bg_base)
    
    # Calculate node positions
    curr_nodes = []
    for n in nodes:
        nx = n['base_x'] + 0.06 * math.sin(time_rad * n['speed_x'])
        ny = n['base_y'] + 0.06 * math.cos(time_rad * n['speed_y'])
        px = int(nx * WIDTH)
        py = int(ny * HEIGHT)
        curr_nodes.append((px, py, n))
    
    # Draw plexus connecting lines
    for i in range(len(curr_nodes)):
        x1, y1, n1 = curr_nodes[i]
        for j in range(i + 1, len(curr_nodes)):
            x2, y2, n2 = curr_nodes[j]
            dist = math.hypot(x2 - x1, y2 - y1)
            if dist < 120:
                alpha = (1.0 - (dist / 120.0)) * 0.35
                draw_line(frame_buf, x1, y1, x2, y2, 14, 165, 233, alpha)
    
    # Draw wave ribbon across lower portion
    wave_y_base = HEIGHT * 0.72
    prev_wx, prev_wy = None, None
    for wx in range(0, WIDTH, 3):
        wy = int(wave_y_base + 35 * math.sin((wx / WIDTH) * 4 * math.pi + time_rad) + 15 * math.cos((wx / WIDTH) * 2 * math.pi - time_rad))
        if prev_wx is not None:
            draw_line(frame_buf, prev_wx, prev_wy, wx, wy, 56, 189, 248, 0.45)
            draw_line(frame_buf, prev_wx, prev_wy + 8, wx, wy + 8, 14, 165, 233, 0.25)
        prev_wx, prev_wy = wx, wy
    
    # Draw nodes (glowing dots)
    for (px, py, n) in curr_nodes:
        r, g, b = n['color']
        draw_filled_circle(frame_buf, px, py, n['radius'], r, g, b, 0.8)
        # Soft outer glow
        draw_filled_circle(frame_buf, px, py, n['radius'] * 2.2, 56, 189, 248, 0.2)
    
    # Draw floating glass cards
    for c in cards:
        drift_x = 18 * math.sin(time_rad * c['speed'])
        drift_y = 14 * math.cos(time_rad * c['speed'])
        drift_angle = c['angle'] + 4 * math.sin(time_rad * c['speed'] * 0.8)
        cx = int(c['x'] * WIDTH + drift_x)
        cy = int(c['y'] * HEIGHT + drift_y)
        draw_rotated_glass_card(frame_buf, cx, cy, c['w'], c['h'], drift_angle, time_rad)
    
    proc.stdin.write(frame_buf)

proc.stdin.close()
proc.wait()
print("Video generated successfully at", output_file)
