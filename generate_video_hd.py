import sys, math, random, subprocess, os

WIDTH = 720
HEIGHT = 1280
FPS = 30
DURATION = 8
TOTAL_FRAMES = FPS * DURATION

os.makedirs("app/src/main/res/raw", exist_ok=True)
output_file = "app/src/main/res/raw/login_bg_video.mp4"

random.seed(1337)
num_nodes = 36
nodes = []
for i in range(num_nodes):
    nodes.append({
        'base_x': random.uniform(0.02, 0.98),
        'base_y': random.uniform(0.02, 0.98),
        'speed_x': random.uniform(0.6, 1.4) * random.choice([-1, 1]),
        'speed_y': random.uniform(0.6, 1.4) * random.choice([-1, 1]),
        'phase': random.uniform(0, math.pi * 2),
        'radius': random.uniform(2.5, 5.0),
        'color': random.choice([(14, 165, 233), (2, 132, 199), (56, 189, 248), (125, 211, 252)])
    })

# 4 floating glass phone / slate panels
cards = [
    {'x': 0.12, 'y': 0.18, 'w': 280, 'h': 420, 'angle': 24, 'speed': 1.0, 'r': 24},
    {'x': 0.88, 'y': 0.10, 'w': 310, 'h': 460, 'angle': -18, 'speed': 1.15, 'r': 28},
    {'x': 0.08, 'y': 0.86, 'w': 300, 'h': 440, 'angle': -26, 'speed': 0.9, 'r': 26},
    {'x': 0.92, 'y': 0.82, 'w': 320, 'h': 480, 'angle': 22, 'speed': 1.05, 'r': 30},
]

# ffmpeg command
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
    '-crf', '20',
    output_file
]

proc = subprocess.Popen(cmd, stdin=subprocess.PIPE)

# Precompute soft vertical background gradient: light blue-gray at top to pure soft white at bottom
bg_base = bytearray(WIDTH * HEIGHT * 3)
for y in range(HEIGHT):
    factor = y / HEIGHT
    # Subtle soft ice blue to pure clean white
    r = int(228 + factor * 26)
    g = int(240 + factor * 14)
    b = int(248 + factor * 7)
    row_bytes = bytes([r, g, b] * WIDTH)
    bg_base[y * WIDTH * 3 : (y + 1) * WIDTH * 3] = row_bytes

def draw_line(buf, x0, y0, x1, y1, r, g, b, alpha):
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

def draw_thick_line(buf, x0, y0, x1, y1, r, g, b, alpha, thickness=2):
    for offset in range(-thickness//2, thickness//2 + 1):
        draw_line(buf, x0 + offset, y0, x1 + offset, y1, r, g, b, alpha)
        draw_line(buf, x0, y0 + offset, x1, y1 + offset, r, g, b, alpha)

def draw_filled_circle(buf, cx, cy, radius, r, g, b, alpha):
    r_int = int(radius) + 1
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

def draw_rounded_rect_card(buf, cx, cy, w, h, angle_deg, corner_r):
    rad = math.radians(angle_deg)
    cos_a = math.cos(rad)
    sin_a = math.sin(rad)
    
    hw = w / 2
    hh = h / 2
    
    # Generate points along rounded rectangle perimeter
    pts = []
    steps = 14
    # Top-right corner arc
    for i in range(steps + 1):
        th = math.pi * 1.5 + (math.pi * 0.5) * (i / steps)
        lx = (hw - corner_r) + corner_r * math.cos(th)
        ly = -(hh - corner_r) + corner_r * math.sin(th)
        pts.append((lx, ly))
    # Bottom-right corner arc
    for i in range(steps + 1):
        th = 0 + (math.pi * 0.5) * (i / steps)
        lx = (hw - corner_r) + corner_r * math.cos(th)
        ly = (hh - corner_r) + corner_r * math.sin(th)
        pts.append((lx, ly))
    # Bottom-left corner arc
    for i in range(steps + 1):
        th = math.pi * 0.5 + (math.pi * 0.5) * (i / steps)
        lx = -(hw - corner_r) + corner_r * math.cos(th)
        ly = (hh - corner_r) + corner_r * math.sin(th)
        pts.append((lx, ly))
    # Top-left corner arc
    for i in range(steps + 1):
        th = math.pi + (math.pi * 0.5) * (i / steps)
        lx = -(hw - corner_r) + corner_r * math.cos(th)
        ly = -(hh - corner_r) + corner_r * math.sin(th)
        pts.append((lx, ly))
        
    # Rotate and translate
    world_pts = []
    for (lx, ly) in pts:
        rx = int(cx + (lx * cos_a - ly * sin_a))
        ry = int(cy + (lx * sin_a + ly * cos_a))
        world_pts.append((rx, ry))
        
    # Draw perimeter with glassy bevel
    for i in range(len(world_pts)):
        p1 = world_pts[i]
        p2 = world_pts[(i + 1) % len(world_pts)]
        # Outer glossy cyan stroke
        draw_thick_line(buf, p1[0], p1[1], p2[0], p2[1], 186, 230, 253, 0.7, thickness=3)
        # Core white highlight
        draw_line(buf, p1[0], p1[1], p2[0], p2[1], 255, 255, 255, 0.85)

for frame in range(TOTAL_FRAMES):
    t = frame / TOTAL_FRAMES
    time_rad = t * 2 * math.pi
    
    frame_buf = bytearray(bg_base)
    
    # Calculate nodes
    curr_nodes = []
    for n in nodes:
        nx = n['base_x'] + 0.05 * math.sin(time_rad * n['speed_x'] + n['phase'])
        ny = n['base_y'] + 0.05 * math.cos(time_rad * n['speed_y'] + n['phase'])
        px = int(nx * WIDTH)
        py = int(ny * HEIGHT)
        curr_nodes.append((px, py, n))
    
    # Draw Plexus network
    for i in range(len(curr_nodes)):
        x1, y1, n1 = curr_nodes[i]
        for j in range(i + 1, len(curr_nodes)):
            x2, y2, n2 = curr_nodes[j]
            dist = math.hypot(x2 - x1, y2 - y1)
            if dist < 170:
                alpha = (1.0 - (dist / 170.0)) * 0.4
                draw_line(frame_buf, x1, y1, x2, y2, 14, 165, 233, alpha)
                
    # Draw luminous flowing waves (ribbon)
    for wave_idx, (base_ratio, amp, freq, color, alpha_val) in enumerate([
        (0.70, 42, 3.5, (56, 189, 248), 0.5),
        (0.74, 32, 4.0, (14, 165, 233), 0.35),
        (0.66, 26, 2.5, (186, 230, 253), 0.45)
    ]):
        wave_y_base = HEIGHT * base_ratio
        prev_wx, prev_wy = None, None
        for wx in range(0, WIDTH, 2):
            wy = int(wave_y_base + amp * math.sin((wx / WIDTH) * freq * math.pi + time_rad + wave_idx) + (amp*0.4) * math.cos((wx / WIDTH) * 2 * math.pi - time_rad))
            if prev_wx is not None:
                draw_thick_line(frame_buf, prev_wx, prev_wy, wx, wy, color[0], color[1], color[2], alpha_val, thickness=2)
            prev_wx, prev_wy = wx, wy
    
    # Draw glowing nodes
    for (px, py, n) in curr_nodes:
        r, g, b = n['color']
        # Outer soft glow
        draw_filled_circle(frame_buf, px, py, n['radius'] * 2.5, 56, 189, 248, 0.25)
        # Inner solid point
        draw_filled_circle(frame_buf, px, py, n['radius'], r, g, b, 0.85)
        draw_filled_circle(frame_buf, px, py, n['radius'] * 0.4, 255, 255, 255, 0.95)
    
    # Draw floating glass cards
    for c in cards:
        drift_x = 24 * math.sin(time_rad * c['speed'])
        drift_y = 18 * math.cos(time_rad * c['speed'])
        drift_angle = c['angle'] + 5 * math.sin(time_rad * c['speed'] * 0.7)
        cx = int(c['x'] * WIDTH + drift_x)
        cy = int(c['y'] * HEIGHT + drift_y)
        draw_rounded_rect_card(frame_buf, cx, cy, c['w'], c['h'], drift_angle, c['r'])
        
    proc.stdin.write(frame_buf)

proc.stdin.close()
proc.wait()
print("HD Background Video rendered successfully!")
