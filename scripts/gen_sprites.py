#!/usr/bin/env python3
"""
gen_sprites.py — Generate placeholder pixel-art sprite PNGs for Survivor TD.
Uses Pillow (PIL) to create simple, visually distinct sprites on transparent backgrounds.
All colors are chosen to be bright/readable against dark background (#0A0E1A).
"""

import os
import math
import random
from PIL import Image, ImageDraw

# ─── Paths ───────────────────────────────────────────────────────────────────
BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DRAWABLE = os.path.join(BASE, "app", "src", "main", "res", "drawable")
SCRIPTS_DIR = os.path.dirname(os.path.abspath(__file__))
BG_DIR = SCRIPTS_DIR  # backgrounds go next to the script

os.makedirs(DRAWABLE, exist_ok=True)
os.makedirs(BG_DIR, exist_ok=True)


def make(w, h):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def darker(c, amount=40):
    if len(c) >= 3:
        return tuple(max(0, v - amount) for v in c[:3]) + (c[3] if len(c) > 3 else 255,)
    return c


def save(img, name, subdir=None):
    d = subdir or DRAWABLE
    os.makedirs(d, exist_ok=True)
    path = os.path.join(d, name)
    img.save(path, "PNG")
    print(f"  saved {path}")


def orect(draw, bbox, fill, ow=1):
    x1, y1, x2, y2 = bbox
    draw.rectangle([x1 - ow, y1 - ow, x2 + ow, y2 + ow], fill=darker(fill))
    draw.rectangle(list(bbox), fill=fill)


def oellipse(draw, bbox, fill, ow=1):
    x1, y1, x2, y2 = bbox
    draw.ellipse([x1 - ow, y1 - ow, x2 + ow, y2 + ow], fill=darker(fill))
    draw.ellipse(list(bbox), fill=fill)


# ═══════════════════════════════════════════════════════════════════════════════
# HERO SPRITES (32x48)
# ═══════════════════════════════════════════════════════════════════════════════

def gen_hero_commander():
    img = make(32, 48); d = ImageDraw.Draw(img)
    orect(d, (10, 18, 22, 36), (60, 140, 220))
    oellipse(d, (9, 4, 23, 16), (80, 180, 255))
    d.rectangle([11, 8, 21, 13], fill=(200, 240, 255))
    orect(d, (5, 19, 9, 32), (50, 120, 200))
    orect(d, (23, 19, 27, 32), (50, 120, 200))
    orect(d, (11, 37, 16, 46), (40, 100, 180))
    orect(d, (16, 37, 21, 46), (40, 100, 180))
    d.rectangle([13, 22, 19, 28], fill=(100, 200, 255))
    save(img, "hero_commander.png")


def gen_hero_berserker():
    img = make(32, 48); d = ImageDraw.Draw(img)
    orect(d, (9, 16, 23, 35), (200, 50, 40))
    oellipse(d, (10, 3, 22, 14), (220, 170, 140))
    d.rectangle([10, 4, 22, 8], fill=(255, 40, 30))
    d.rectangle([12, 7, 14, 9], fill=(255, 255, 100))
    d.rectangle([18, 7, 20, 9], fill=(255, 255, 100))
    orect(d, (3, 17, 8, 30), (180, 40, 30))
    orect(d, (24, 17, 29, 30), (180, 40, 30))
    orect(d, (3, 30, 7, 34), (220, 170, 140))
    orect(d, (25, 30, 29, 34), (220, 170, 140))
    orect(d, (11, 36, 15, 46), (160, 40, 30))
    orect(d, (17, 36, 21, 46), (160, 40, 30))
    save(img, "hero_berserker.png")


def gen_hero_engineer():
    img = make(32, 48); d = ImageDraw.Draw(img)
    orect(d, (10, 17, 22, 35), (220, 140, 40))
    oellipse(d, (10, 4, 22, 15), (210, 180, 150))
    d.ellipse([11, 5, 15, 9], fill=(255, 180, 50))
    d.ellipse([17, 5, 21, 9], fill=(255, 180, 50))
    d.line([(14, 6), (16, 6)], fill=(80, 60, 20), width=1)
    d.rectangle([12, 9, 14, 11], fill=(40, 40, 40))
    d.rectangle([18, 9, 20, 11], fill=(40, 40, 40))
    d.rectangle([10, 28, 22, 30], fill=(140, 90, 20))
    d.line([(12, 26), (12, 30)], fill=(180, 180, 200), width=2)
    orect(d, (6, 18, 10, 30), (200, 120, 30))
    orect(d, (22, 18, 26, 30), (200, 120, 30))
    orect(d, (11, 36, 16, 46), (190, 110, 25))
    orect(d, (16, 36, 21, 46), (190, 110, 25))
    save(img, "hero_engineer.png")


def gen_hero_medic():
    img = make(32, 48); d = ImageDraw.Draw(img)
    orect(d, (9, 16, 23, 36), (230, 230, 240))
    oellipse(d, (10, 3, 22, 14), (210, 190, 170))
    d.rectangle([9, 4, 23, 7], fill=(240, 240, 250))
    d.rectangle([12, 8, 14, 10], fill=(40, 120, 60))
    d.rectangle([18, 8, 20, 10], fill=(40, 120, 60))
    d.rectangle([14, 19, 18, 25], fill=(40, 200, 80))
    d.rectangle([12, 21, 20, 23], fill=(40, 200, 80))
    d.line([(25, 12), (25, 45)], fill=(180, 160, 100), width=2)
    d.ellipse([22, 8, 28, 14], fill=(40, 200, 80))
    orect(d, (5, 17, 9, 30), (220, 220, 230))
    orect(d, (23, 17, 27, 30), (220, 220, 230))
    orect(d, (11, 37, 16, 46), (200, 200, 210))
    orect(d, (16, 37, 21, 46), (200, 200, 210))
    save(img, "hero_medic.png")


def gen_hero_scout():
    img = make(32, 48); d = ImageDraw.Draw(img)
    orect(d, (11, 17, 21, 34), (240, 220, 50))
    oellipse(d, (11, 3, 21, 14), (200, 180, 140))
    d.rectangle([11, 2, 14, 4], fill=(60, 40, 20))
    d.rectangle([17, 2, 21, 4], fill=(60, 40, 20))
    d.rectangle([12, 7, 14, 9], fill=(40, 180, 40))
    d.rectangle([18, 7, 20, 9], fill=(40, 180, 40))
    orect(d, (7, 18, 11, 28), (220, 200, 40))
    orect(d, (21, 18, 25, 28), (220, 200, 40))
    orect(d, (12, 35, 15, 47), (200, 180, 30))
    orect(d, (17, 35, 20, 47), (200, 180, 30))
    d.rectangle([10, 15, 22, 17], fill=(255, 100, 50))
    save(img, "hero_scout.png")


def gen_hero_shielder():
    img = make(32, 48); d = ImageDraw.Draw(img)
    orect(d, (9, 16, 23, 36), (120, 60, 180))
    oellipse(d, (10, 3, 22, 15), (180, 160, 200))
    d.rectangle([11, 7, 21, 12], fill=(100, 200, 255))
    orect(d, (1, 14, 9, 38), (100, 40, 160))
    orect(d, (2, 16, 8, 36), (140, 80, 200))
    d.rectangle([3, 20, 7, 32], fill=(180, 120, 255))
    orect(d, (23, 17, 27, 30), (100, 40, 160))
    orect(d, (11, 37, 16, 46), (90, 40, 150))
    orect(d, (16, 37, 21, 46), (90, 40, 150))
    orect(d, (8, 15, 11, 19), (160, 100, 220))
    orect(d, (21, 15, 24, 19), (160, 100, 220))
    save(img, "hero_shielder.png")


# ═══════════════════════════════════════════════════════════════════════════════
# ENEMY SPRITES
# ═══════════════════════════════════════════════════════════════════════════════

def gen_enemy_zombie():
    img = make(24, 32); d = ImageDraw.Draw(img)
    orect(d, (7, 12, 17, 24), (100, 130, 80))
    oellipse(d, (7, 2, 17, 12), (120, 140, 90))
    d.rectangle([9, 5, 11, 7], fill=(200, 50, 50))
    d.rectangle([13, 5, 15, 7], fill=(200, 50, 50))
    orect(d, (1, 13, 6, 18), (90, 120, 70))
    orect(d, (18, 11, 23, 16), (90, 120, 70))
    orect(d, (8, 25, 11, 31), (80, 100, 60))
    orect(d, (13, 25, 16, 31), (80, 100, 60))
    save(img, "enemy_zombie.png")


def gen_enemy_runner():
    img = make(20, 28); d = ImageDraw.Draw(img)
    orect(d, (6, 10, 14, 20), (200, 40, 30))
    oellipse(d, (6, 2, 14, 10), (180, 60, 50))
    d.rectangle([8, 4, 10, 6], fill=(255, 255, 0))
    d.rectangle([12, 4, 13, 6], fill=(255, 255, 0))
    orect(d, (6, 21, 9, 27), (170, 30, 20))
    orect(d, (11, 21, 14, 27), (170, 30, 20))
    orect(d, (2, 11, 5, 17), (180, 50, 40))
    orect(d, (15, 9, 18, 15), (180, 50, 40))
    save(img, "enemy_runner.png")


def gen_enemy_brute():
    img = make(32, 40); d = ImageDraw.Draw(img)
    orect(d, (8, 14, 24, 32), (160, 30, 20))
    oellipse(d, (9, 2, 23, 14), (140, 40, 30))
    d.rectangle([12, 5, 15, 8], fill=(255, 100, 0))
    d.rectangle([17, 5, 20, 8], fill=(255, 100, 0))
    orect(d, (2, 15, 7, 28), (140, 25, 15))
    orect(d, (25, 15, 30, 28), (140, 25, 15))
    oellipse(d, (2, 28, 6, 32), (160, 40, 30))
    oellipse(d, (26, 28, 30, 32), (160, 40, 30))
    orect(d, (10, 33, 15, 39), (120, 20, 10))
    orect(d, (17, 33, 22, 39), (120, 20, 10))
    save(img, "enemy_brute.png")


def gen_enemy_spitter():
    img = make(24, 32); d = ImageDraw.Draw(img)
    orect(d, (7, 12, 17, 24), (120, 40, 160))
    oellipse(d, (6, 1, 18, 13), (150, 50, 200))
    d.ellipse([9, 7, 15, 12], fill=(40, 0, 60))
    d.rectangle([11, 12, 13, 15], fill=(100, 255, 50))
    orect(d, (2, 13, 6, 20), (100, 30, 140))
    orect(d, (18, 13, 22, 20), (100, 30, 140))
    orect(d, (8, 25, 11, 31), (90, 25, 130))
    orect(d, (13, 25, 16, 31), (90, 25, 130))
    save(img, "enemy_spitter.png")


def gen_enemy_bomber():
    img = make(24, 32); d = ImageDraw.Draw(img)
    oellipse(d, (5, 10, 19, 25), (220, 140, 30))
    oellipse(d, (7, 2, 17, 11), (200, 120, 20))
    d.ellipse([9, 14, 15, 20], fill=(255, 220, 100))
    d.rectangle([9, 5, 11, 7], fill=(255, 50, 50))
    d.rectangle([13, 5, 15, 7], fill=(255, 50, 50))
    orect(d, (2, 12, 5, 18), (200, 110, 20))
    orect(d, (19, 12, 22, 18), (200, 110, 20))
    orect(d, (8, 26, 11, 31), (180, 100, 15))
    orect(d, (13, 26, 16, 31), (180, 100, 15))
    save(img, "enemy_bomber.png")


def gen_enemy_healer():
    img = make(24, 32); d = ImageDraw.Draw(img)
    orect(d, (7, 12, 17, 24), (240, 180, 200))
    oellipse(d, (7, 2, 17, 12), (255, 210, 220))
    d.rectangle([10, 14, 14, 20], fill=(255, 255, 255))
    d.rectangle([8, 16, 16, 18], fill=(255, 255, 255))
    d.rectangle([9, 5, 11, 7], fill=(100, 60, 120))
    d.rectangle([13, 5, 15, 7], fill=(100, 60, 120))
    orect(d, (3, 13, 6, 20), (230, 170, 190))
    orect(d, (18, 13, 21, 20), (230, 170, 190))
    orect(d, (8, 25, 11, 31), (220, 160, 180))
    orect(d, (13, 25, 16, 31), (220, 160, 180))
    save(img, "enemy_healer.png")


def gen_enemy_shielder():
    img = make(24, 32); d = ImageDraw.Draw(img)
    orect(d, (7, 12, 17, 24), (40, 100, 200))
    d.polygon([(10, 10), (14, 10), (16, 14), (16, 22), (12, 26), (8, 22), (8, 14)],
              fill=(60, 140, 240))
    oellipse(d, (7, 2, 17, 11), (60, 120, 220))
    d.rectangle([9, 5, 11, 7], fill=(200, 220, 255))
    d.rectangle([13, 5, 15, 7], fill=(200, 220, 255))
    orect(d, (8, 25, 11, 31), (30, 80, 170))
    orect(d, (13, 25, 16, 31), (30, 80, 170))
    save(img, "enemy_shielder.png")


def gen_enemy_flyer():
    img = make(20, 28); d = ImageDraw.Draw(img)
    oellipse(d, (6, 8, 14, 20), (200, 200, 220))
    d.ellipse([0, 6, 6, 16], fill=(180, 180, 210))
    d.ellipse([14, 6, 20, 16], fill=(180, 180, 210))
    oellipse(d, (7, 1, 13, 9), (210, 210, 230))
    d.rectangle([8, 4, 9, 5], fill=(100, 100, 200))
    d.rectangle([11, 4, 12, 5], fill=(100, 100, 200))
    d.line([(10, 20), (10, 27)], fill=(180, 180, 200), width=1)
    save(img, "enemy_flyer.png")


def gen_enemy_elite():
    img = make(24, 32); d = ImageDraw.Draw(img)
    orect(d, (6, 11, 18, 25), (180, 140, 30))
    oellipse(d, (7, 2, 17, 12), (200, 160, 40))
    d.rectangle([7, 2, 17, 5], fill=(255, 215, 0))
    d.rectangle([8, 0, 10, 3], fill=(255, 215, 0))
    d.rectangle([14, 0, 16, 3], fill=(255, 215, 0))
    d.rectangle([9, 6, 11, 8], fill=(255, 50, 50))
    d.rectangle([13, 6, 15, 8], fill=(255, 50, 50))
    orect(d, (2, 12, 5, 20), (160, 120, 20))
    orect(d, (19, 12, 22, 20), (160, 120, 20))
    orect(d, (8, 26, 11, 31), (140, 100, 15))
    orect(d, (13, 26, 16, 31), (140, 100, 15))
    d.rectangle([5, 10, 19, 26], outline=(255, 215, 0), width=1)
    save(img, "enemy_elite.png")


def gen_enemy_boss():
    img = make(64, 80); d = ImageDraw.Draw(img)
    orect(d, (18, 20, 46, 55), (180, 20, 10))
    oellipse(d, (18, 2, 46, 22), (200, 30, 20))
    d.polygon([(20, 4), (24, -2), (28, 4)], fill=(255, 200, 0))
    d.polygon([(36, 4), (40, -2), (44, 4)], fill=(255, 200, 0))
    d.rectangle([24, 8, 30, 14], fill=(255, 255, 0))
    d.rectangle([34, 8, 40, 14], fill=(255, 255, 0))
    d.rectangle([26, 10, 28, 12], fill=(0, 0, 0))
    d.rectangle([36, 10, 38, 12], fill=(0, 0, 0))
    d.rectangle([26, 16, 38, 20], fill=(60, 0, 0))
    for x in range(27, 38, 2):
        d.rectangle([x, 16, x + 1, 18], fill=(255, 255, 255))
    orect(d, (2, 22, 17, 48), (160, 15, 5))
    orect(d, (47, 22, 62, 48), (160, 15, 5))
    oellipse(d, (2, 44, 17, 56), (180, 30, 20))
    oellipse(d, (47, 44, 62, 56), (180, 30, 20))
    orect(d, (22, 56, 30, 78), (140, 10, 5))
    orect(d, (34, 56, 42, 78), (140, 10, 5))
    d.ellipse([28, 28, 36, 38], fill=(255, 100, 0))
    d.ellipse([30, 30, 34, 36], fill=(255, 200, 50))
    d.polygon([(16, 22), (18, 16), (20, 22)], fill=(200, 50, 0))
    d.polygon([(44, 22), (46, 16), (48, 22)], fill=(200, 50, 0))
    save(img, "enemy_boss.png")


# ═══════════════════════════════════════════════════════════════════════════════
# PROJECTILE SPRITES (16x16)
# ═══════════════════════════════════════════════════════════════════════════════

def gen_proj_bullet():
    img = make(16, 16); d = ImageDraw.Draw(img)
    oellipse(d, (6, 6, 10, 10), (255, 255, 100))
    save(img, "proj_bullet.png")


def gen_proj_spread():
    img = make(16, 16); d = ImageDraw.Draw(img)
    for x, y in [(4, 4), (10, 4), (7, 8), (4, 12), (10, 12)]:
        oellipse(d, (x, y, x + 2, y + 2), (255, 160, 40))
    save(img, "proj_spread.png")


def gen_proj_slash():
    img = make(16, 16); d = ImageDraw.Draw(img)
    d.arc([2, 2, 14, 14], 200, 340, fill=(255, 255, 255), width=2)
    d.arc([3, 3, 13, 13], 200, 340, fill=(220, 220, 255), width=1)
    save(img, "proj_slash.png")


def gen_proj_lightning():
    img = make(16, 16); d = ImageDraw.Draw(img)
    pts = [(8, 1), (5, 5), (10, 5), (7, 9), (12, 9), (6, 15)]
    d.line(pts, fill=(255, 255, 100), width=2)
    d.line(pts, fill=(200, 200, 50), width=1)
    save(img, "proj_lightning.png")


def gen_proj_rocket():
    img = make(16, 16); d = ImageDraw.Draw(img)
    orect(d, (6, 3, 10, 12), (255, 160, 40))
    d.polygon([(6, 3), (8, 0), (10, 3)], fill=(255, 100, 30))
    d.polygon([(6, 12), (4, 15), (6, 10)], fill=(200, 100, 20))
    d.polygon([(10, 12), (12, 15), (10, 10)], fill=(200, 100, 20))
    d.polygon([(7, 12), (8, 16), (9, 12)], fill=(255, 255, 100))
    save(img, "proj_rocket.png")


def gen_proj_shield():
    img = make(16, 16); d = ImageDraw.Draw(img)
    oellipse(d, (3, 3, 13, 13), (100, 220, 255))
    d.ellipse([5, 5, 11, 11], fill=(180, 240, 255))
    save(img, "proj_shield.png")


def gen_proj_drone():
    img = make(16, 16); d = ImageDraw.Draw(img)
    orect(d, (5, 5, 11, 11), (60, 120, 220))
    d.line([(3, 7), (13, 7)], fill=(200, 200, 200), width=1)
    d.line([(8, 3), (8, 13)], fill=(200, 200, 200), width=1)
    d.ellipse([7, 7, 9, 9], fill=(100, 200, 255))
    save(img, "proj_drone.png")


def gen_proj_frost():
    img = make(16, 16); d = ImageDraw.Draw(img)
    cx, cy = 8, 8
    pts = [(cx, 1), (10, 6), (15, cy), (10, 10), (cx, 15), (6, 10), (1, cy), (6, 6)]
    d.polygon(pts, fill=(100, 180, 255))
    d.polygon(pts, outline=(60, 140, 220))
    save(img, "proj_frost.png")


def gen_proj_boomerang():
    img = make(16, 16); d = ImageDraw.Draw(img)
    d.arc([2, 2, 14, 14], 30, 330, fill=(200, 200, 220), width=3)
    d.arc([4, 3, 13, 13], 60, 300, fill=(240, 240, 255), width=1)
    save(img, "proj_boomerang.png")


def gen_proj_mine():
    img = make(16, 16); d = ImageDraw.Draw(img)
    oellipse(d, (4, 5, 12, 13), (140, 100, 50))
    d.ellipse([6, 7, 10, 11], fill=(255, 40, 40))
    d.line([(8, 3), (8, 5)], fill=(120, 80, 40), width=1)
    d.line([(2, 9), (4, 9)], fill=(120, 80, 40), width=1)
    d.line([(12, 9), (14, 9)], fill=(120, 80, 40), width=1)
    save(img, "proj_mine.png")


def gen_proj_heal():
    img = make(16, 16); d = ImageDraw.Draw(img)
    oellipse(d, (3, 3, 13, 13), (200, 255, 200))
    d.ellipse([5, 5, 11, 11], fill=(100, 255, 150))
    d.rectangle([7, 6, 9, 10], fill=(255, 255, 255))
    d.rectangle([6, 7, 10, 9], fill=(255, 255, 255))
    save(img, "proj_heal.png")


def gen_proj_laser():
    img = make(16, 16); d = ImageDraw.Draw(img)
    d.rectangle([0, 6, 16, 10], fill=(255, 30, 30))
    d.rectangle([0, 7, 16, 9], fill=(255, 150, 150))
    d.rectangle([0, 7, 16, 8], fill=(255, 220, 220))
    save(img, "proj_laser.png")


# ═══════════════════════════════════════════════════════════════════════════════
# TOWER SPRITES (32x32)
# ═══════════════════════════════════════════════════════════════════════════════

def gen_tower_gun():
    img = make(32, 32); d = ImageDraw.Draw(img)
    orect(d, (8, 18, 24, 30), (120, 120, 130))
    oellipse(d, (10, 12, 22, 22), (160, 160, 170))
    orect(d, (14, 4, 18, 14), (140, 140, 150))
    orect(d, (13, 2, 19, 5), (180, 180, 190))
    save(img, "tower_gun.png")


def gen_tower_cannon():
    img = make(32, 32); d = ImageDraw.Draw(img)
    orect(d, (6, 20, 26, 30), (140, 100, 50))
    oellipse(d, (8, 14, 24, 24), (180, 120, 40))
    orect(d, (12, 4, 20, 16), (160, 100, 30))
    orect(d, (10, 2, 22, 6), (200, 140, 50))
    save(img, "tower_cannon.png")


def gen_tower_frost():
    img = make(32, 32); d = ImageDraw.Draw(img)
    orect(d, (8, 20, 24, 30), (80, 120, 160))
    d.polygon([(16, 2), (22, 12), (20, 22), (12, 22), (10, 12)], fill=(140, 200, 255))
    d.polygon([(16, 4), (20, 12), (16, 20), (12, 12)], fill=(180, 230, 255))
    d.ellipse([14, 8, 18, 12], fill=(220, 245, 255))
    save(img, "tower_frost.png")


def gen_tower_tesla():
    img = make(32, 32); d = ImageDraw.Draw(img)
    orect(d, (8, 20, 24, 30), (120, 110, 40))
    orect(d, (12, 6, 20, 22), (200, 180, 60))
    d.rectangle([10, 8, 22, 10], fill=(255, 240, 100))
    d.rectangle([10, 14, 22, 16], fill=(255, 240, 100))
    oellipse(d, (12, 2, 20, 10), (255, 255, 150))
    d.line([(16, 2), (14, 0), (18, 0)], fill=(255, 255, 200), width=1)
    save(img, "tower_tesla.png")


def gen_tower_poison():
    img = make(32, 32); d = ImageDraw.Draw(img)
    orect(d, (8, 20, 24, 30), (60, 100, 40))
    oellipse(d, (6, 6, 26, 22), (80, 160, 60))
    oellipse(d, (8, 4, 22, 18), (100, 200, 80))
    d.ellipse([12, 8, 16, 12], fill=(140, 230, 100))
    d.ellipse([18, 10, 22, 14], fill=(120, 220, 90))
    d.ellipse([8, 12, 12, 16], fill=(160, 240, 120))
    save(img, "tower_poison.png")


def gen_tower_rocket():
    img = make(32, 32); d = ImageDraw.Draw(img)
    orect(d, (6, 18, 26, 30), (160, 60, 50))
    orect(d, (8, 14, 24, 20), (180, 70, 60))
    orect(d, (9, 4, 14, 16), (200, 80, 40))
    orect(d, (18, 4, 23, 16), (200, 80, 40))
    oellipse(d, (9, 2, 14, 6), (220, 100, 50))
    oellipse(d, (18, 2, 23, 6), (220, 100, 50))
    save(img, "tower_rocket.png")


# ═══════════════════════════════════════════════════════════════════════════════
# PICKUP SPRITES (16x16)
# ═══════════════════════════════════════════════════════════════════════════════

def gen_pickup_xp_small():
    img = make(16, 16); d = ImageDraw.Draw(img)
    cx, cy = 8, 8
    pts = [(cx, 2), (14, cy), (cx, 14), (2, cy)]
    d.polygon(pts, fill=(80, 140, 255))
    d.polygon(pts, outline=(40, 80, 180))
    save(img, "pickup_xp_small.png")


def gen_pickup_xp_medium():
    img = make(16, 16); d = ImageDraw.Draw(img)
    cx, cy = 8, 8
    pts = [(cx, 1), (15, cy), (cx, 15), (1, cy)]
    d.polygon(pts, fill=(60, 220, 100))
    d.polygon(pts, outline=(30, 150, 60))
    save(img, "pickup_xp_medium.png")


def gen_pickup_xp_large():
    img = make(16, 16); d = ImageDraw.Draw(img)
    cx, cy = 8, 8
    pts = [(cx, 0), (16, cy), (cx, 16), (0, cy)]
    d.polygon(pts, fill=(255, 215, 0))
    d.polygon(pts, outline=(200, 160, 0))
    d.ellipse([6, 6, 10, 10], fill=(255, 240, 150))
    save(img, "pickup_xp_large.png")


def gen_pickup_gold():
    img = make(16, 16); d = ImageDraw.Draw(img)
    oellipse(d, (3, 3, 13, 13), (255, 215, 0))
    d.ellipse([5, 5, 11, 11], fill=(255, 235, 80))
    d.line([(8, 5), (8, 11)], fill=(200, 160, 0), width=1)
    d.line([(6, 6), (10, 6)], fill=(200, 160, 0), width=1)
    d.line([(6, 10), (10, 10)], fill=(200, 160, 0), width=1)
    save(img, "pickup_gold.png")


def gen_pickup_health():
    img = make(16, 16); d = ImageDraw.Draw(img)
    d.rectangle([6, 2, 10, 14], fill=(255, 50, 50))
    d.rectangle([2, 6, 14, 10], fill=(255, 50, 50))
    d.rectangle([6, 2, 10, 14], outline=(180, 30, 30))
    d.rectangle([2, 6, 14, 10], outline=(180, 30, 30))
    save(img, "pickup_health.png")


def gen_pickup_scrap():
    img = make(16, 16); d = ImageDraw.Draw(img)
    oellipse(d, (4, 4, 12, 12), (160, 160, 170))
    d.ellipse([6, 6, 10, 10], fill=(100, 100, 110))
    for angle in [0, 90, 180, 270]:
        rad = math.radians(angle)
        x = int(8 + 6 * math.cos(rad))
        y = int(8 + 6 * math.sin(rad))
        d.rectangle([x - 1, y - 1, x + 1, y + 1], fill=(140, 140, 150))
    save(img, "pickup_scrap.png")


# ═══════════════════════════════════════════════════════════════════════════════
# BACKGROUND LAYERS (1280x720)
# ═══════════════════════════════════════════════════════════════════════════════

def gen_background(name, base_color, grid_color, rubble_colors, fog_color=None):
    W, H = 1280, 720
    img = Image.new("RGBA", (W, H), base_color + (255,))
    d = ImageDraw.Draw(img)
    for x in range(0, W, 64):
        d.line([(x, 0), (x, H)], fill=grid_color + (60,), width=1)
    for y in range(0, H, 64):
        d.line([(0, y), (W, y)], fill=grid_color + (60,), width=1)
    random.seed(hash(name))
    for _ in range(30):
        rx = random.randint(0, W - 80)
        ry = random.randint(0, H - 60)
        rw = random.randint(20, 80)
        rh = random.randint(15, 60)
        c = random.choice(rubble_colors)
        d.rectangle([rx, ry, rx + rw, ry + rh], fill=c + (100,))
    for _ in range(5):
        by = random.randint(0, H - 30)
        bw = random.randint(200, 600)
        bx = random.randint(0, W - bw)
        c = random.choice(rubble_colors)
        d.rectangle([bx, by, bx + bw, by + random.randint(5, 20)], fill=c + (50,))
    if fog_color:
        fog = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        fd = ImageDraw.Draw(fog)
        for _ in range(15):
            fx = random.randint(0, W)
            fy = random.randint(0, H)
            fr = random.randint(40, 120)
            fd.ellipse([fx - fr, fy - fr, fx + fr, fy + fr], fill=fog_color + (30,))
        img = Image.alpha_composite(img, fog)
    save(img, f"bg_{name}.png", subdir=BG_DIR)


def gen_bg_wasteland():
    gen_background("wasteland", (40, 25, 15), (80, 50, 25),
                   [(60, 35, 20), (80, 50, 30), (50, 30, 18)])

def gen_bg_swamp():
    gen_background("swamp", (15, 35, 20), (30, 60, 35),
                   [(20, 50, 30), (30, 40, 25), (15, 55, 35)],
                   fog_color=(40, 80, 50))

def gen_bg_city():
    gen_background("city", (20, 22, 30), (40, 45, 60),
                   [(50, 55, 70), (35, 38, 50), (60, 65, 80)])

def gen_bg_lab():
    gen_background("lab", (15, 20, 35), (40, 50, 80),
                   [(30, 40, 60), (50, 55, 80), (35, 45, 70)])

def gen_bg_bunker():
    gen_background("bunker", (25, 10, 10), (50, 20, 20),
                   [(40, 15, 15), (55, 20, 20), (30, 12, 12)])


# ═══════════════════════════════════════════════════════════════════════════════
# HUD ICON SPRITES (48x48)
# ═══════════════════════════════════════════════════════════════════════════════

def gen_icon_assault_rifle():
    img = make(48, 48); d = ImageDraw.Draw(img)
    orect(d, (4, 20, 40, 26), (180, 180, 190))
    orect(d, (34, 22, 44, 32), (140, 140, 150))
    orect(d, (2, 22, 8, 24), (200, 200, 210))
    orect(d, (18, 26, 22, 34), (160, 160, 170))
    orect(d, (22, 26, 28, 36), (150, 150, 160))
    save(img, "icon_assault_rifle.png")


def gen_icon_spread_gun():
    img = make(48, 48); d = ImageDraw.Draw(img)
    orect(d, (4, 18, 36, 28), (180, 180, 190))
    orect(d, (2, 19, 8, 22), (200, 200, 210))
    orect(d, (2, 24, 8, 27), (200, 200, 210))
    orect(d, (30, 20, 44, 32), (140, 140, 150))
    orect(d, (16, 28, 22, 38), (160, 160, 170))
    save(img, "icon_spread_gun.png")


def gen_icon_melee_sword():
    img = make(48, 48); d = ImageDraw.Draw(img)
    orect(d, (22, 4, 26, 32), (200, 200, 220))
    d.polygon([(22, 4), (24, 0), (26, 4)], fill=(220, 220, 240))
    orect(d, (16, 32, 32, 36), (180, 150, 50))
    orect(d, (22, 36, 26, 44), (120, 80, 40))
    oellipse(d, (22, 42, 26, 46), (180, 150, 50))
    save(img, "icon_melee_sword.png")


def gen_icon_tesla_gun():
    img = make(48, 48); d = ImageDraw.Draw(img)
    orect(d, (14, 12, 34, 36), (60, 60, 140))
    d.rectangle([12, 16, 36, 19], fill=(200, 200, 60))
    d.rectangle([12, 22, 36, 25], fill=(200, 200, 60))
    d.rectangle([12, 28, 36, 31], fill=(200, 200, 60))
    oellipse(d, (16, 4, 32, 16), (255, 255, 150))
    d.line([(18, 6), (14, 2), (20, 0)], fill=(255, 255, 200), width=1)
    d.line([(30, 6), (34, 2), (28, 0)], fill=(255, 255, 200), width=1)
    orect(d, (20, 36, 28, 44), (80, 80, 160))
    save(img, "icon_tesla_gun.png")


def gen_icon_rocket_launcher():
    img = make(48, 48); d = ImageDraw.Draw(img)
    orect(d, (6, 16, 38, 28), (180, 80, 40))
    oellipse(d, (2, 14, 12, 30), (200, 100, 50))
    d.polygon([(8, 18), (10, 14), (12, 18)], fill=(255, 200, 50))
    orect(d, (18, 28, 28, 40), (140, 60, 30))
    orect(d, (24, 28, 26, 38), (120, 50, 20))
    save(img, "icon_rocket_launcher.png")


def gen_icon_shield_projector():
    img = make(48, 48); d = ImageDraw.Draw(img)
    orect(d, (14, 18, 34, 36), (60, 140, 200))
    oellipse(d, (18, 20, 30, 32), (100, 200, 255))
    d.arc([6, 6, 42, 42], 30, 150, fill=(150, 220, 255), width=2)
    orect(d, (20, 36, 28, 44), (40, 100, 160))
    save(img, "icon_shield_projector.png")


def gen_icon_drone_deployer():
    img = make(48, 48); d = ImageDraw.Draw(img)
    orect(d, (14, 14, 34, 34), (80, 80, 160))
    orect(d, (18, 6, 30, 16), (100, 140, 220))
    d.line([(14, 8), (34, 8)], fill=(200, 200, 200), width=2)
    d.ellipse([22, 10, 26, 14], fill=(100, 255, 100))
    orect(d, (20, 34, 28, 44), (60, 60, 130))
    save(img, "icon_drone_deployer.png")


def gen_icon_freeze_ray():
    img = make(48, 48); d = ImageDraw.Draw(img)
    orect(d, (12, 18, 36, 32), (80, 160, 200))
    orect(d, (4, 20, 14, 30), (100, 200, 240))
    d.ellipse([2, 18, 8, 24], fill=(180, 230, 255))
    d.ellipse([2, 26, 8, 32], fill=(180, 230, 255))
    orect(d, (22, 32, 30, 42), (60, 120, 160))
    save(img, "icon_freeze_ray.png")


def gen_icon_boomerang():
    img = make(48, 48); d = ImageDraw.Draw(img)
    d.arc([4, 4, 44, 44], 20, 340, fill=(200, 200, 220), width=4)
    d.arc([8, 6, 42, 42], 30, 330, fill=(240, 240, 255), width=2)
    oellipse(d, (22, 22, 26, 26), (180, 180, 200))
    save(img, "icon_boomerang.png")


def gen_icon_mine_layer():
    img = make(48, 48); d = ImageDraw.Draw(img)
    oellipse(d, (10, 12, 38, 40), (140, 100, 50))
    d.ellipse([18, 18, 30, 30], fill=(180, 130, 60))
    oellipse(d, (21, 21, 27, 27), (255, 40, 40))
    for angle in range(0, 360, 45):
        rad = math.radians(angle)
        cx, cy = 24, 26
        x1 = int(cx + 14 * math.cos(rad))
        y1 = int(cy + 14 * math.sin(rad))
        x2 = int(cx + 18 * math.cos(rad))
        y2 = int(cy + 18 * math.sin(rad))
        d.line([(x1, y1), (x2, y2)], fill=(120, 80, 40), width=2)
    save(img, "icon_mine_layer.png")


def gen_icon_healing_aura():
    img = make(48, 48); d = ImageDraw.Draw(img)
    oellipse(d, (6, 6, 42, 42), (100, 255, 150))
    oellipse(d, (12, 12, 36, 36), (150, 255, 180))
    d.rectangle([20, 14, 28, 34], fill=(255, 255, 255))
    d.rectangle([14, 20, 34, 28], fill=(255, 255, 255))
    save(img, "icon_healing_aura.png")


def gen_icon_laser_beam():
    img = make(48, 48); d = ImageDraw.Draw(img)
    orect(d, (16, 20, 40, 30), (200, 40, 40))
    oellipse(d, (2, 22, 18, 28), (255, 100, 100))
    d.rectangle([0, 24, 4, 26], fill=(255, 50, 50))
    orect(d, (26, 30, 32, 40), (160, 30, 30))
    save(img, "icon_laser_beam.png")


def gen_icon_hp():
    img = make(48, 48); d = ImageDraw.Draw(img)
    oellipse(d, (10, 8, 26, 24), (255, 50, 60))
    oellipse(d, (22, 8, 38, 24), (255, 50, 60))
    d.polygon([(10, 18), (38, 18), (24, 44)], fill=(255, 50, 60))
    d.ellipse([14, 12, 20, 18], fill=(255, 150, 160))
    save(img, "icon_hp.png")


def gen_icon_gold():
    img = make(48, 48); d = ImageDraw.Draw(img)
    oellipse(d, (8, 8, 40, 40), (255, 215, 0))
    oellipse(d, (12, 12, 36, 36), (255, 235, 80))
    d.line([(24, 14), (24, 34)], fill=(200, 160, 0), width=3)
    d.line([(16, 18), (32, 18)], fill=(200, 160, 0), width=2)
    d.line([(16, 30), (32, 30)], fill=(200, 160, 0), width=2)
    save(img, "icon_gold.png")


def gen_icon_xp():
    img = make(48, 48); d = ImageDraw.Draw(img)
    cx, cy = 24, 24
    pts = [(cx, 4), (42, cy), (cx, 44), (6, cy)]
    d.polygon(pts, fill=(100, 200, 255))
    d.polygon(pts, outline=(60, 140, 200))
    pts2 = [(cx, 10), (36, cy), (cx, 38), (12, cy)]
    d.polygon(pts2, fill=(140, 220, 255))
    d.ellipse([20, 20, 28, 28], fill=(220, 245, 255))
    save(img, "icon_xp.png")


# ═══════════════════════════════════════════════════════════════════════════════
# MAIN
# ═══════════════════════════════════════════════════════════════════════════════

def main():
    print("=" * 60)
    print("Survivor TD — Sprite Generator")
    print("=" * 60)

    print("\n[1/7] Hero sprites (32x48)...")
    gen_hero_commander()
    gen_hero_berserker()
    gen_hero_engineer()
    gen_hero_medic()
    gen_hero_scout()
    gen_hero_shielder()

    print("\n[2/7] Enemy sprites...")
    gen_enemy_zombie()
    gen_enemy_runner()
    gen_enemy_brute()
    gen_enemy_spitter()
    gen_enemy_bomber()
    gen_enemy_healer()
    gen_enemy_shielder()
    gen_enemy_flyer()
    gen_enemy_elite()
    gen_enemy_boss()

    print("\n[3/7] Projectile sprites (16x16)...")
    gen_proj_bullet()
    gen_proj_spread()
    gen_proj_slash()
    gen_proj_lightning()
    gen_proj_rocket()
    gen_proj_shield()
    gen_proj_drone()
    gen_proj_frost()
    gen_proj_boomerang()
    gen_proj_mine()
    gen_proj_heal()
    gen_proj_laser()

    print("\n[4/7] Tower sprites (32x32)...")
    gen_tower_gun()
    gen_tower_cannon()
    gen_tower_frost()
    gen_tower_tesla()
    gen_tower_poison()
    gen_tower_rocket()

    print("\n[5/7] Pickup sprites (16x16)...")
    gen_pickup_xp_small()
    gen_pickup_xp_medium()
    gen_pickup_xp_large()
    gen_pickup_gold()
    gen_pickup_health()
    gen_pickup_scrap()

    print("\n[6/7] Background layers (1280x720)...")
    gen_bg_wasteland()
    gen_bg_swamp()
    gen_bg_city()
    gen_bg_lab()
    gen_bg_bunker()

    print("\n[7/7] HUD icon sprites (48x48)...")
    gen_icon_assault_rifle()
    gen_icon_spread_gun()
    gen_icon_melee_sword()
    gen_icon_tesla_gun()
    gen_icon_rocket_launcher()
    gen_icon_shield_projector()
    gen_icon_drone_deployer()
    gen_icon_freeze_ray()
    gen_icon_boomerang()
    gen_icon_mine_layer()
    gen_icon_healing_aura()
    gen_icon_laser_beam()
    gen_icon_hp()
    gen_icon_gold()
    gen_icon_xp()

    print("\n" + "=" * 60)
    print("Done! All sprites generated successfully.")
    print("=" * 60)


if __name__ == "__main__":
    main()
