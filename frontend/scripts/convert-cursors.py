#!/usr/bin/env python3
"""Convert PaperPlane .cur cursor files to web-optimized PNG format."""

import os
import struct
from pathlib import Path
from PIL import Image

CURSOR_DIR = Path(r"D:\单机游戏修改\PaperPlane_Cursors整合\v1.2")
OUTPUT_DIR = Path(r"D:\IDEA.code\YuzhonBlog\frontend\public\cursors")
OUTPUT_SIZE = (32, 32)  # Standard web cursor size

# Cursor mapping: file -> cursor type
# Based on the preview image and AutoSetup.inf
CURSOR_MAP = {
    "01": {"name": "default", "hotspot": (9, 9), "desc": "Normal select (paper plane)"},
    "02": {"name": "help", "hotspot": (9, 9), "desc": "Help select (paper plane + ?)"},
    "03": {"name": "progress", "hotspot": (16, 16), "desc": "Working in background (animated)"},
    "04": {"name": "busy", "hotspot": (16, 16), "desc": "Busy (animated spinner)"},
    "05": {"name": "crosshair", "hotspot": (64, 64), "desc": "Precision select (crosshair)"},
    "06": {"name": "text", "hotspot": (12, 12), "desc": "Text select (I-beam)"},
    "07": {"name": "handwriting", "hotspot": (6, 6), "desc": "Handwriting (pen)"},
    "08": {"name": "not-allowed", "hotspot": (64, 64), "desc": "Not allowed (circle with line)"},
    "09": {"name": "ns-resize", "hotspot": (64, 64), "desc": "Vertical resize"},
    "10": {"name": "ew-resize", "hotspot": (64, 64), "desc": "Horizontal resize"},
    "11": {"name": "nwse-resize", "hotspot": (64, 64), "desc": "Diagonal resize (NW-SE)"},
    "12": {"name": "nesw-resize", "hotspot": (64, 64), "desc": "Diagonal resize (NE-SW)"},
    "13": {"name": "move", "hotspot": (64, 64), "desc": "Move (4-way arrow)"},
    "14": {"name": "pointer", "hotspot": (64, 64), "desc": "Alternate select (up arrow)"},
    "15": {"name": "alias", "hotspot": (16, 16), "desc": "Link select (paper plane + trail)"},
}

def extract_largest_icon(cur_path):
    """Extract the largest icon from a .cur file."""
    with open(cur_path, 'rb') as f:
        # Read CUR header
        reserved = struct.unpack('<H', f.read(2))[0]
        cursor_type = struct.unpack('<H', f.read(2))[0]
        icon_count = struct.unpack('<H', f.read(2))[0]

        if cursor_type != 2:
            print(f"  Warning: {cur_path.name} is not a cursor file (type={cursor_type})")
            return None

        # Read directory entries
        entries = []
        for _ in range(icon_count):
            width = struct.unpack('B', f.read(1))[0]
            height = struct.unpack('B', f.read(1))[0]
            colors = struct.unpack('B', f.read(1))[0]
            reserved = struct.unpack('B', f.read(1))[0]
            hotspot_x = struct.unpack('<H', f.read(2))[0]
            hotspot_y = struct.unpack('<H', f.read(2))[0]
            data_size = struct.unpack('<I', f.read(4))[0]
            data_offset = struct.unpack('<I', f.read(4))[0]
            entries.append({
                'width': width or 256,
                'height': height or 256,
                'colors': colors,
                'hotspot_x': hotspot_x,
                'hotspot_y': hotspot_y,
                'data_size': data_size,
                'data_offset': data_offset,
            })

        # Find largest icon
        largest = max(entries, key=lambda e: e['width'] * e['height'])

        # Read icon data
        f.seek(largest['data_offset'])
        icon_data = f.read(largest['data_size'])

        # Write to temp ICO file and load with Pillow
        import tempfile
        import io
        temp_ico = io.BytesIO()
        # ICO header
        temp_ico.write(struct.pack('<HHH', 0, 1, 1))
        # ICO directory entry
        w = largest['width'] if largest['width'] < 256 else 0
        h = largest['height'] if largest['height'] < 256 else 0
        temp_ico.write(struct.pack('<BBBBHHII', w, h, largest['colors'], 0,
                                  largest['hotspot_x'], largest['hotspot_y'],
                                  len(icon_data), 6 + 16))
        temp_ico.write(icon_data)
        temp_ico.seek(0)
        img = Image.open(temp_ico)
        img.load()  # Force load into memory before BytesIO is garbage collected
        return img, (largest['hotspot_x'], largest['hotspot_y'])

def convert_cursor(file_num, info):
    """Convert a single cursor file to web PNG."""
    cur_file = CURSOR_DIR / f"{file_num}.cur"
    ani_file = CURSOR_DIR / f"{file_num}.ani"

    src_file = cur_file if cur_file.exists() else ani_file
    if not src_file.exists():
        print(f"  Skipping {file_num}: file not found")
        return False

    if src_file.suffix == '.ani':
        print(f"  Skipping {file_num}: .ani (animated) - needs special handling")
        return False

    try:
        result = extract_largest_icon(src_file)
        if result is None:
            return False

        img, hotspot = result

        # Convert to RGBA if needed
        if img.mode != 'RGBA':
            img = img.convert('RGBA')

        # Resize to web-friendly size
        img = img.resize(OUTPUT_SIZE, Image.Resampling.LANCZOS)

        # Save as PNG
        out_path = OUTPUT_DIR / f"{info['name']}.png"
        img.save(out_path, 'PNG', optimize=True)

        # Calculate scaled hotspot
        scale = OUTPUT_SIZE[0] / img.width
        scaled_hotspot = (int(hotspot[0] * scale), int(hotspot[1] * scale))

        print(f"  [OK] {file_num}.cur -> {info['name']}.png ({img.size}, hotspot: {scaled_hotspot})")
        return True

    except Exception as e:
        print(f"  [ERROR] Error converting {file_num}: {e}")
        return False

def main():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    print("Converting PaperPlane cursors to web format...")
    print(f"Source: {CURSOR_DIR}")
    print(f"Output: {OUTPUT_DIR}")
    print(f"Target size: {OUTPUT_SIZE}")
    print()

    success = 0
    failed = 0
    skipped = 0

    for file_num, info in sorted(CURSOR_MAP.items()):
        if info['name'] in ('progress', 'busy'):
            skipped += 1
            continue

        if convert_cursor(file_num, info):
            success += 1
        else:
            failed += 1

    print(f"\nDone: {success} converted, {failed} failed, {skipped} animated (skipped)")
    print(f"\nAnimated cursors (.ani) need special handling:")
    print(f"  - 03.ani (progress) - animated paper plane")
    print(f"  - 04.ani (busy) - animated spinner")

if __name__ == '__main__':
    main()
