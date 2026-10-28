#!/usr/bin/env python3
"""Deterministic flat-illustration generator for LearnThai story scenes.

Reads a story JSON from app/src/main/assets/stories/ and writes one 1024x683 WebP per
card (plus a cover) into app/src/main/assets/stories/art/.

Style: flat storybook, 3-4 colours per scene plus one warm accent, rounded shapes, no
baked-in text (all Thai/English is drawn by Compose so it stays editable).

The output is intentionally generic "ogre and donkey" character art, not film art.

Usage:
    python tools/generate_story_art.py --all
    python tools/generate_story_art.py --story shrek_donkey_streetfood
"""

import argparse
import io
import json
import math
import os

from PIL import Image, ImageDraw

W, H = 1024, 683
GROUND = 612
LEFT_X = 300
RIGHT_X = 726

SKY_TOP = (23, 16, 51)
SKY_BOTTOM = (61, 36, 92)
MOON = (246, 231, 182)
HILL = (34, 22, 54)
GROUND_C = (43, 27, 63)
WOOD = (107, 74, 47)
WOOD_DARK = (74, 50, 32)
WOOD_LIGHT = (139, 100, 66)
CREAM = (242, 227, 198)
RED = (179, 64, 47)
LANTERN = (255, 179, 92)
STEAM = (222, 214, 200)

OGRE = (111, 168, 60)
OGRE_DARK = (78, 122, 46)
OGRE_NOSE = (96, 148, 52)
VEST = (122, 82, 48)
PANTS = (86, 62, 40)

DONKEY = (154, 160, 168)
DONKEY_DARK = (112, 118, 126)
MUZZLE = (216, 220, 225)
EAR_PINK = (201, 138, 138)

SKIN = (224, 176, 138)
HAIR = (58, 42, 32)
APRON = (238, 226, 205)

ART_DIR = os.path.join("app", "src", "main", "assets", "stories", "art")
STORIES_DIR = os.path.join("app", "src", "main", "assets", "stories")


def shade(color, factor):
    """Multiply an RGB colour by factor (used to dim the listener)."""
    return tuple(max(0, min(255, int(c * factor))) for c in color)


def limb(d, p1, p2, width, color):
    d.line([p1, p2], fill=color, width=width, joint="curve")
    r = width / 2.0
    for p in (p1, p2):
        d.ellipse([p[0] - r, p[1] - r, p[0] + r, p[1] + r], fill=color)


def vertical_gradient(img, top, bottom, y0, y1):
    d = ImageDraw.Draw(img)
    span = max(1, y1 - y0)
    for y in range(y0, y1):
        t = (y - y0) / span
        c = tuple(int(top[i] * (1 - t) + bottom[i] * t) for i in range(3))
        d.line([(0, y), (W, y)], fill=c)


def build_background(img, glow, lantern_count, moon_visible=True, bg="market"):
    d = ImageDraw.Draw(img)
    g = ImageDraw.Draw(glow)

    if bg == "room":
        # Warm interior at night: wall, floor, moonlit window, lamp.
        vertical_gradient(img, (64, 50, 68), (52, 38, 56), 0, GROUND)
        d.rectangle([0, GROUND, W, H], fill=(40, 30, 46))
        d.line([(0, GROUND), (W, GROUND)], fill=(28, 20, 34), width=6)

        wx0, wy0, wx1, wy1 = 690, 120, 950, 400
        d.rectangle([wx0 - 10, wy0 - 10, wx1 + 10, wy1 + 10], fill=(44, 34, 48))
        d.rectangle([wx0, wy0, wx1, wy1], fill=(36, 44, 78))
        d.ellipse([wx0 + 150, wy0 + 40, wx0 + 210, wy0 + 100], fill=MOON)
        d.line([(wx0, (wy0 + wy1) // 2), (wx1, (wy0 + wy1) // 2)], fill=CREAM, width=6)
        d.line([((wx0 + wx1) // 2, wy0), ((wx0 + wx1) // 2, wy1)], fill=CREAM, width=6)
        for radius, alpha in ((120, 12), (80, 18)):
            g.ellipse([wx0 + 180 - radius, wy0 + 70 - radius, wx0 + 180 + radius, wy0 + 70 + radius],
                      fill=(246, 231, 182, alpha))

        for radius, alpha in ((150, 14), (90, 20), (50, 26)):
            g.ellipse([220 - radius, 300 - radius, 220 + radius, 300 + radius], fill=(255, 179, 92, alpha))
        d.rectangle([205, 296, 235, 328], fill=(255, 190, 120))
        d.rectangle([198, 328, 242, 342], fill=(90, 70, 60))
        return d

    if bg == "forest":
        vertical_gradient(img, (14, 24, 34), (30, 46, 40), 0, GROUND)
        for radius, alpha in ((110, 12), (82, 18), (56, 26)):
            g.ellipse([820 - radius, 120 - radius, 820 + radius, 120 + radius],
                      fill=(240, 244, 220, alpha))
        d.ellipse([790, 90, 850, 150], fill=(238, 240, 215))

        for x, w, top in ((40, 44, 250), (200, 34, 300), (420, 50, 230), (880, 38, 280)):
            d.rectangle([x, top, x + w, GROUND], fill=(20, 30, 26))
            d.ellipse([x - 80, top - 110, x + w + 80, top + 40], fill=(18, 34, 26))
            d.ellipse([x - 50, top - 70, x + w + 40, top + 20], fill=(22, 42, 30))

        for fx, fy in ((160, 420), (330, 350), (610, 300), (740, 380), (950, 330), (520, 200)):
            g.ellipse([fx - 16, fy - 16, fx + 16, fy + 16], fill=(220, 240, 160, 26))
            d.ellipse([fx - 3, fy - 3, fx + 3, fy + 3], fill=(236, 244, 176))

        vertical_gradient(img, (26, 40, 32), (16, 26, 22), GROUND, H)
        return d

    if bg == "street":
        vertical_gradient(img, SKY_TOP, SKY_BOTTOM, 0, GROUND)
        for radius, alpha in ((96, 14), (74, 20), (52, 26)):
            g.ellipse([848 - radius, 122 - radius, 848 + radius, 122 + radius],
                      fill=(246, 231, 182, alpha))
        d.ellipse([822, 90, 886, 154], fill=MOON)
        # Village buildings with warm windows.
        for x, w, top in ((10, 150, 330), (172, 118, 384), (296, 176, 296),
                          (700, 130, 360), (830, 190, 318)):
            d.rectangle([x, top, x + w, GROUND], fill=(30, 22, 44))
            for wy in range(top + 26, GROUND - 24, 48):
                for wx in range(x + 18, x + w - 20, 42):
                    d.rectangle([wx, wy, wx + 16, wy + 22], fill=(255, 190, 120))
        vertical_gradient(img, GROUND_C, (28, 18, 42), GROUND, H)
        return d

    vertical_gradient(img, SKY_TOP, SKY_BOTTOM, 0, GROUND)

    # Distant hill line.
    d.ellipse([-260, GROUND - 130, 520, GROUND + 190], fill=HILL)
    d.ellipse([420, GROUND - 90, 1300, GROUND + 220], fill=HILL)

    if moon_visible:
        for radius, alpha in ((96, 14), (74, 20), (52, 26)):
            g.ellipse(
                [848 - radius, 122 - radius, 848 + radius, 122 + radius],
                fill=(246, 231, 182, alpha),
            )
        d.ellipse([822, 90, 886, 154], fill=MOON)

    # Lantern strings.
    positions = [(120, 150), (250, 132), (390, 150), (560, 126), (700, 146), (930, 168)]
    for i in range(lantern_count):
        x, y = positions[i % len(positions)]
        for radius, alpha in ((58, 12), (42, 18), (30, 26)):
            g.ellipse(
                [x - radius, y - radius, x + radius, y + radius],
                fill=(255, 179, 92, alpha),
            )

    # Ground.
    vertical_gradient(img, GROUND_C, (30, 19, 45), GROUND, H)

    return d


def draw_lanterns(img, lantern_count):
    d = ImageDraw.Draw(img)
    d.line([(0, 118), (W, 118)], fill=(70, 48, 96), width=3)
    positions = [(120, 150), (250, 132), (390, 150), (560, 126), (700, 146), (930, 168)]
    for i in range(lantern_count):
        x, y = positions[i % len(positions)]
        d.line([(x, 118), (x, y - 20)], fill=(70, 48, 96), width=2)
        d.ellipse([x - 17, y - 20, x + 17, y + 22], fill=LANTERN)
        d.rectangle([x - 9, y - 25, x + 9, y - 19], fill=shade(LANTERN, 0.8))
        d.rectangle([x - 8, y + 20, x + 8, y + 26], fill=shade(LANTERN, 0.8))


def draw_distant_stalls(d):
    for x in (30, 250, 470):
        top = GROUND - 150
        # Stall body.
        d.rectangle([x + 6, top + 34, x + 174, GROUND], fill=shade(HILL, 1.08))
        # Warm counter strip.
        d.rectangle([x + 16, top + 52, x + 164, top + 64], fill=shade(LANTERN, 0.62))
        # Striped awning.
        band_w = 28
        for i in range(6):
            color = shade(CREAM, 0.55) if i % 2 == 0 else shade(RED, 0.72)
            d.polygon(
                [
                    (x + i * band_w, top + 30),
                    (x + (i + 1) * band_w, top + 30),
                    (x + 12 + (i + 1) * band_w, top),
                    (x + 12 + i * band_w, top),
                ],
                fill=color,
            )
        d.line([(x + 6, top + 34), (x + 186, top + 34)], fill=shade(WOOD_DARK, 1.6), width=4)
        # Posts.
        for px in (x + 10, x + 168):
            d.rectangle([px, top + 34, px + 8, GROUND], fill=shade(WOOD_DARK, 1.2))


def ogre(d, cx, ground, s=1.0, facing=1, pose="idle", mood="happy", dim=1.0):
    def C(c):
        return shade(c, dim)

    body_top = ground - 320 * s
    body_bottom = ground - 120 * s
    head_cx = cx + 10 * facing * s
    head_cy = body_top - 58 * s

    # Legs.
    for side in (-1, 1):
        hip = (cx + side * 44 * s, body_bottom - 6 * s)
        foot = (cx + side * 50 * s, ground)
        limb(d, hip, foot, int(40 * s), C(PANTS))
        d.ellipse(
            [foot[0] - 32 * s, ground - 16 * s, foot[0] + 32 * s, ground + 12 * s],
            fill=C(shade(PANTS, 0.8)),
        )

    # Arms.
    shoulder_back = (cx - 78 * facing * s, body_top + 42 * s)
    elbow_pose = {
        "wave": (shoulder_back[0] - 30 * facing * s, shoulder_back[1] + 70 * s),
        "point": (shoulder_back[0] + 70 * facing * s, shoulder_back[1] + 55 * s),
        "chest": (shoulder_back[0] + 40 * facing * s, shoulder_back[1] + 80 * s),
        "fingers": (shoulder_back[0] + 20 * facing * s, shoulder_back[1] + 62 * s),
        "clasp": (shoulder_back[0] + 55 * facing * s, shoulder_back[1] + 75 * s),
        "sniff": (shoulder_back[0] + 40 * facing * s, shoulder_back[1] + 78 * s),
    }.get(pose, (shoulder_back[0] + 34 * facing * s, shoulder_back[1] + 92 * s))
    limb(d, shoulder_back, elbow_pose, int(30 * s), C(OGRE))

    # Torso.
    d.rounded_rectangle(
        [cx - 86 * s, body_top, cx + 86 * s, body_bottom],
        radius=int(38 * s),
        fill=C(VEST),
    )
    d.rounded_rectangle(
        [cx - 52 * s, body_top + 16 * s, cx + 52 * s, body_bottom - 10 * s],
        radius=int(26 * s),
        fill=C(shade(OGRE, 0.92)),
    )

    # Head.
    d.rounded_rectangle(
        [head_cx - 76 * s, head_cy - 64 * s, head_cx + 76 * s, head_cy + 62 * s],
        radius=int(42 * s),
        fill=C(OGRE),
    )
    # Ears.
    for side in (-1, 1):
        ex = head_cx + side * 78 * s
        d.rounded_rectangle(
            [ex - 12 * s, head_cy - 26 * s, ex + 12 * s, head_cy + 18 * s],
            radius=int(10 * s),
            fill=C(shade(OGRE, 0.94)),
        )
        d.ellipse(
            [ex - 8 * s, head_cy - 18 * s, ex + 8 * s, head_cy + 8 * s],
            fill=C(shade(OGRE, 0.82)),
        )

    # Brow.
    brow_y = head_cy - 16 * s
    d.rounded_rectangle(
        [head_cx - 56 * s, brow_y - 9 * s, head_cx + 56 * s, brow_y + 2 * s],
        radius=int(6 * s),
        fill=C(OGRE_DARK),
    )

    # Eyes.
    for side in (-1, 1):
        ex = head_cx + side * 30 * s
        d.ellipse(
            [ex - 17 * s, head_cy - 12 * s, ex + 17 * s, head_cy + 12 * s],
            fill=C((250, 248, 240)),
        )
        if mood == "love":
            hx, hy = ex, head_cy
            d.polygon(
                [
                    (hx, hy + 8 * s),
                    (hx - 10 * s, hy - 2 * s),
                    (hx - 5 * s, hy - 10 * s),
                    (hx, hy - 4 * s),
                    (hx + 5 * s, hy - 10 * s),
                    (hx + 10 * s, hy - 2 * s),
                ],
                fill=(214, 74, 96),
            )
        else:
            d.ellipse(
                [
                    ex - 6 * s + facing * 3 * s,
                    head_cy - 5 * s,
                    ex + 6 * s + facing * 3 * s,
                    head_cy + 7 * s,
                ],
                fill=C((38, 32, 28)),
            )

    # Nose.
    d.rounded_rectangle(
        [head_cx - 18 * s, head_cy + 10 * s, head_cx + 18 * s, head_cy + 34 * s],
        radius=int(12 * s),
        fill=C(OGRE_NOSE),
    )
    for side in (-1, 1):
        d.ellipse(
            [head_cx + side * 7 * s - 4 * s, head_cy + 26 * s, head_cx + side * 7 * s + 4 * s, head_cy + 33 * s],
            fill=C(shade(OGRE_DARK, 0.9)),
        )

    # Mouth.
    mouth_box = [head_cx - 34 * s, head_cy + 32 * s, head_cx + 34 * s, head_cy + 58 * s]
    if mood in ("happy", "generous", "love"):
        d.arc(mouth_box, start=12, end=168, fill=C((70, 42, 30)), width=int(5 * s))
    elif mood == "sad":
        d.arc(mouth_box, start=192, end=348, fill=C((70, 42, 30)), width=int(5 * s))
    else:
        d.line(
            [(head_cx - 24 * s, head_cy + 46 * s), (head_cx + 24 * s, head_cy + 46 * s)],
            fill=C((70, 42, 30)),
            width=int(5 * s),
        )

    # Front arm + hand.
    shoulder_front = (cx + 78 * facing * s, body_top + 44 * s)
    if pose == "wave":
        hand = (shoulder_front[0] + 44 * facing * s, shoulder_front[1] - 118 * s)
    elif pose == "fingers":
        hand = (shoulder_front[0] + 50 * facing * s, shoulder_front[1] - 104 * s)
    elif pose == "point":
        hand = (shoulder_front[0] + 112 * facing * s, shoulder_front[1] + 26 * s)
    elif pose == "chest":
        hand = (cx + 20 * facing * s, body_top + 96 * s)
    elif pose == "clasp":
        hand = (cx + 14 * facing * s, body_top + 104 * s)
    else:
        hand = (shoulder_front[0] + 26 * facing * s, shoulder_front[1] + 112 * s)

    limb(d, shoulder_front, hand, int(28 * s), C(OGRE))
    d.ellipse(
        [hand[0] - 18 * s, hand[1] - 18 * s, hand[0] + 18 * s, hand[1] + 18 * s],
        fill=C(shade(OGRE, 0.96)),
    )
    if pose == "wave":
        for i in range(3):
            fx = hand[0] + (i - 1) * 9 * s
            d.line(
                [(fx, hand[1] - 12 * s), (fx + facing * 5 * s, hand[1] - 24 * s)],
                fill=C(shade(OGRE, 0.9)),
                width=int(7 * s),
            )
    if pose == "fingers":
        for i in range(2):
            fx = hand[0] + i * 9 * s + facing * 2 * s
            d.line(
                [(fx, hand[1] - 10 * s), (fx + facing * 3 * s, hand[1] - 30 * s)],
                fill=C(shade(OGRE, 0.9)),
                width=int(8 * s),
            )


def donkey(d, cx, ground, s=1.0, facing=1, pose="idle", mood="happy", dim=1.0):
    def C(c):
        return shade(c, dim)

    body_top = ground - 168 * s
    body_bottom = ground - 92 * s
    head_cx = cx + 40 * facing * s
    head_cy = ground - 232 * s

    # Legs.
    for i, off in enumerate((-46, -20, 22, 48)):
        top = (cx + off * s, body_bottom - 4 * s)
        bottom = (cx + off * s + (6 * facing * s if i % 2 else -4 * facing * s), ground)
        limb(d, top, bottom, int(11 * s), C(shade(DONKEY_DARK, 0.95)))
        d.ellipse(
            [bottom[0] - 8 * s, ground - 6 * s, bottom[0] + 8 * s, ground + 6 * s],
            fill=C(shade(DONKEY_DARK, 0.85)),
        )

    # Tail.
    tail = (cx - 74 * facing * s, body_top + 18 * s)
    d.line([tail, (tail[0] - 14 * facing * s, tail[1] + 34 * s)], fill=C(DONKEY_DARK), width=int(7 * s))
    tx0 = tail[0] - 22 * facing * s
    tx1 = tail[0] - 4 * facing * s
    d.ellipse(
        [min(tx0, tx1), tail[1] + 26 * s, max(tx0, tx1), tail[1] + 46 * s],
        fill=C(DONKEY_DARK),
    )

    # Body.
    d.rounded_rectangle(
        [cx - 78 * s, body_top, cx + 78 * s, body_bottom],
        radius=int(40 * s),
        fill=C(DONKEY),
    )
    # Neck.
    d.polygon(
        [
            (cx + 20 * facing * s, body_top + 4 * s),
            (cx + 66 * facing * s, body_top + 4 * s),
            (head_cx - 6 * facing * s, head_cy + 30 * s),
            (head_cx - 40 * facing * s, head_cy + 34 * s),
        ],
        fill=C(shade(DONKEY, 0.97)),
    )

    # Ears.
    for side in (-1, 1):
        base = (head_cx + side * 24 * s, head_cy - 40 * s)
        tip = (head_cx + side * 40 * s, head_cy - 128 * s)
        limb(d, base, tip, int(19 * s), C(DONKEY))
        limb(
            d,
            (base[0] + side * 2 * s, base[1] - 6 * s),
            (tip[0] - side * 3 * s, tip[1] + 18 * s),
            int(8 * s),
            C(EAR_PINK),
        )

    # Head.
    d.rounded_rectangle(
        [head_cx - 50 * s, head_cy - 48 * s, head_cx + 50 * s, head_cy + 40 * s],
        radius=int(26 * s),
        fill=C(DONKEY),
    )
    # Mane.
    for i in range(3):
        d.ellipse(
            [
                head_cx - 10 * s + i * 10 * s,
                head_cy - 58 * s,
                head_cx + 4 * s + i * 10 * s,
                head_cy - 44 * s,
            ],
            fill=C(DONKEY_DARK),
        )

    # Muzzle.
    m0 = head_cx + min(-30 * s, facing * 54 * s)
    m1 = head_cx + max(30 * s, facing * 54 * s)
    d.ellipse([m0, head_cy + 8 * s, m1, head_cy + 66 * s], fill=C(MUZZLE))
    for side in (-1, 1):
        nx = head_cx + facing * 28 * s + side * 10 * s
        d.ellipse(
            [nx - 4 * s, head_cy + 34 * s, nx + 4 * s, head_cy + 42 * s],
            fill=C(shade(MUZZLE, 0.6)),
        )

    # Eyes.
    for side in (-1, 1):
        ex = head_cx + side * 22 * s - facing * 4 * s
        d.ellipse(
            [ex - 13 * s, head_cy - 24 * s, ex + 13 * s, head_cy + 2 * s],
            fill=C((250, 248, 240)),
        )
        if mood == "love":
            d.polygon(
                [
                    (ex, head_cy - 4 * s),
                    (ex - 9 * s, head_cy - 14 * s),
                    (ex - 4 * s, head_cy - 21 * s),
                    (ex, head_cy - 15 * s),
                    (ex + 4 * s, head_cy - 21 * s),
                    (ex + 9 * s, head_cy - 14 * s),
                ],
                fill=(214, 74, 96),
            )
        else:
            d.ellipse(
                [ex - 5 * s, head_cy - 16 * s, ex + 6 * s, head_cy - 2 * s],
                fill=C((38, 32, 28)),
            )
        # Brows carry the mood.
        brow_y = head_cy - 32 * s
        if mood == "sad":
            d.line(
                [
                    (ex - 12 * s, brow_y - 2 * s),
                    (ex + 12 * s, brow_y + 6 * s),
                ],
                fill=C(DONKEY_DARK),
                width=int(5 * s),
            )
        elif mood == "surprised":
            d.arc(
                [ex - 12 * s, brow_y - 8 * s, ex + 12 * s, brow_y + 6 * s],
                start=200,
                end=340,
                fill=C(DONKEY_DARK),
                width=int(4 * s),
            )

    # Mouth.
    mouth = [head_cx + facing * 12 * s - 16 * s, head_cy + 44 * s,
             head_cx + facing * 12 * s + 16 * s, head_cy + 60 * s]
    if mood in ("happy", "love", "generous"):
        d.arc(mouth, start=15, end=165, fill=C((60, 44, 38)), width=int(4 * s))

    # Front legs / arms for poses.
    shoulder = (cx + 46 * facing * s, body_top + 24 * s)
    if pose == "point":
        hand = (cx + 132 * facing * s, body_top + 52 * s)
    elif pose == "sniff":
        hand = (cx + 34 * facing * s, body_top - 6 * s)
    elif pose == "clasp":
        hand = (cx + 30 * facing * s, body_top + 66 * s)
    elif pose == "pocket":
        hand = (cx + 8 * facing * s, body_top + 62 * s)
    else:
        hand = (cx + 54 * facing * s, body_top + 92 * s)
    limb(d, shoulder, hand, int(13 * s), C(DONKEY))
    d.ellipse(
        [hand[0] - 9 * s, hand[1] - 9 * s, hand[0] + 9 * s, hand[1] + 9 * s],
        fill=C(shade(DONKEY, 0.95)),
    )


def vendor(d, cx, ground, s=1.0, facing=1, pose="wai", mood="happy", dim=1.0):
    def C(c):
        return shade(c, dim)

    head_cy = ground - 246 * s
    d.ellipse(
        [cx - 52 * s, head_cy - 34 * s, cx + 52 * s, head_cy + 66 * s],
        fill=C(HAIR),
    )
    d.ellipse(
        [cx - 36 * s, head_cy - 42 * s, cx - 4 * s, head_cy - 14 * s],
        fill=C(shade(HAIR, 0.85)),
    )
    d.ellipse(
        [cx - 33 * s, head_cy - 30 * s, cx + 33 * s, head_cy + 36 * s],
        fill=C(SKIN),
    )
    # Hair fringe.
    d.arc(
        [cx - 34 * s, head_cy - 34 * s, cx + 34 * s, head_cy + 10 * s],
        start=185,
        end=355,
        fill=C(HAIR),
        width=int(9 * s),
    )
    for side in (-1, 1):
        ex = cx + side * 13 * s
        d.ellipse([ex - 3 * s, head_cy - 4 * s, ex + 3 * s, head_cy + 3 * s], fill=C((40, 34, 30)))
    d.arc(
        [cx - 12 * s, head_cy + 8 * s, cx + 12 * s, head_cy + 24 * s],
        start=15,
        end=165,
        fill=C((120, 60, 50)),
        width=int(3 * s),
    )

    # Body.
    body_top = ground - 214 * s
    d.rounded_rectangle(
        [cx - 58 * s, body_top, cx + 58 * s, ground - 44 * s],
        radius=int(26 * s),
        fill=C((194, 98, 46)),
    )
    d.rounded_rectangle(
        [cx - 30 * s, body_top + 18 * s, cx + 30 * s, ground - 40 * s],
        radius=int(10 * s),
        fill=C(APRON),
    )

    # Arms: wai (palms together) or resting on the counter.
    if pose == "wai":
        for side in (-1, 1):
            shoulder = (cx + side * 52 * s, body_top + 22 * s)
            hand = (cx + side * 5 * s, body_top + 58 * s)
            limb(d, shoulder, hand, int(15 * s), C(SKIN))
    elif pose == "wok":
        limb(d, (cx + 46 * facing * s, body_top + 26 * s), (cx + 82 * facing * s, body_top + 62 * s),
             int(15 * s), C(SKIN))
    else:
        for side in (-1, 1):
            limb(d, (cx + side * 52 * s, body_top + 26 * s), (cx + side * 58 * s, body_top + 74 * s),
                 int(15 * s), C(SKIN))


def hex_rgb(value, fallback=(74, 96, 140)):
    try:
        clean = str(value).lstrip("#")
        if len(clean) == 6:
            return tuple(int(clean[i:i + 2], 16) for i in (0, 2, 4))
    except Exception:
        pass
    return fallback


SKIN_LIGHT = (232, 190, 152)
GREY_HAIR = (208, 208, 212)
WOOD_SKIN = (214, 176, 118)


def person(d, cx, ground, s=1.0, facing=1, pose="idle", mood="happy", dim=1.0,
           skin=SKIN_LIGHT, hair=(58, 42, 32), hair_style="short",
           outfit=(74, 96, 140), accent=(238, 234, 226), hat=None,
           hat_color=(236, 196, 84), glasses=False, mustache=False,
           nose=0.0, dress=False, height=1.0):
    """Flat cartoon human. All story people are this rig with different parameters."""
    def C(c):
        return shade(c, dim)

    k = s * height
    head_cy = ground - 236 * k
    head_r = 34 * k
    body_top = head_cy + head_r + 4 * k
    body_bottom = ground - 70 * k
    pants = shade(outfit, 0.52)

    # Legs and shoes.
    for side in (-1, 1):
        hip = (cx + side * 17 * k, body_bottom - 4 * k)
        foot = (cx + side * 20 * k, ground - 8 * k)
        limb(d, hip, foot, int(15 * k), C(pants))
        d.ellipse([foot[0] - 14 * k, ground - 14 * k, foot[0] + 14 * k, ground + 2 * k],
                  fill=C((46, 38, 34)))

    # Torso.
    if dress:
        d.polygon([(cx - 34 * k, body_top), (cx + 34 * k, body_top),
                   (cx + 58 * k, body_bottom), (cx - 58 * k, body_bottom)], fill=C(outfit))
        d.polygon([(cx - 30 * k, body_top), (cx + 30 * k, body_top),
                   (cx + 34 * k, body_top + 20 * k), (cx - 34 * k, body_top + 20 * k)],
                  fill=C(accent))
    else:
        d.rounded_rectangle([cx - 36 * k, body_top, cx + 36 * k, body_bottom],
                            radius=int(18 * k), fill=C(outfit))
        d.polygon([(cx - 15 * k, body_top + 2 * k), (cx + 15 * k, body_top + 2 * k),
                   (cx, body_top + 26 * k)], fill=C(accent))

    # Arms.
    shoulder_r = (cx + 34 * k, body_top + 18 * k)
    shoulder_l = (cx - 34 * k, body_top + 18 * k)

    def hand(shoulder, target):
        limb(d, shoulder, target, int(13 * k), C(skin))
        d.ellipse([target[0] - 9 * k, target[1] - 9 * k,
                   target[0] + 9 * k, target[1] + 9 * k], fill=C(skin))

    if pose == "wave":
        hand(shoulder_r, (cx + 52 * k, body_top - 44 * k))
        hand(shoulder_l, (cx - 46 * k, body_bottom - 30 * k))
    elif pose == "point":
        hand(shoulder_r, (cx + 80 * k, body_top + 40 * k))
        hand(shoulder_l, (cx - 44 * k, body_bottom - 28 * k))
    elif pose == "chest":
        hand(shoulder_r, (cx + 6 * k, body_top + 44 * k))
        hand(shoulder_l, (cx - 44 * k, body_bottom - 30 * k))
    elif pose == "clasp":
        hand(shoulder_r, (cx + 7 * k, body_top + 54 * k))
        hand(shoulder_l, (cx - 7 * k, body_top + 54 * k))
    elif pose == "type":
        hand(shoulder_r, (cx + 36 * k, body_top + 72 * k))
        hand(shoulder_l, (cx + 20 * k, body_top + 68 * k))
    else:
        hand(shoulder_r, (cx + 40 * k, body_bottom - 26 * k))
        hand(shoulder_l, (cx - 40 * k, body_bottom - 26 * k))

    # Hair back layer.
    if hair_style == "bob":
        d.rounded_rectangle([cx - head_r - 7 * k, head_cy - head_r - 8 * k,
                             cx + head_r + 7 * k, head_cy + head_r + 12 * k],
                            radius=int(24 * k), fill=C(hair))
    if hair_style in ("bun", "grey_bun"):
        d.ellipse([cx - 13 * k, head_cy - head_r - 26 * k, cx + 13 * k, head_cy - head_r],
                  fill=C(hair))

    # Head and ears.
    d.ellipse([cx - head_r, head_cy - head_r, cx + head_r, head_cy + head_r], fill=C(skin))
    for side in (-1, 1):
        d.ellipse([cx + side * head_r - 7 * k, head_cy + 2 * k,
                   cx + side * head_r + 7 * k, head_cy + 16 * k], fill=C(skin))

    # Hair / hood front.
    if hair_style in ("short", "bun", "grey_bun"):
        d.pieslice([cx - head_r - 1 * k, head_cy - head_r - 3 * k,
                    cx + head_r + 1 * k, head_cy + head_r - 12 * k], 180, 360, fill=C(hair))
    elif hair_style == "bob":
        d.pieslice([cx - head_r - 2 * k, head_cy - head_r - 2 * k,
                    cx + head_r + 2 * k, head_cy + head_r - 8 * k], 185, 355, fill=C(hair))
    elif hair_style == "hood":
        d.pieslice([cx - head_r - 8 * k, head_cy - head_r - 10 * k,
                    cx + head_r + 8 * k, head_cy + head_r + 2 * k], 180, 360,
                   fill=C((190, 52, 52)))

    if hat == "cap":
        d.pieslice([cx - head_r - 4 * k, head_cy - head_r - 12 * k,
                    cx + head_r + 4 * k, head_cy + head_r - 20 * k], 180, 360, fill=C(hat_color))
        d.rounded_rectangle([cx - 6 * k, head_cy - head_r - 16 * k,
                             cx + 26 * k, head_cy - head_r - 8 * k], radius=int(4 * k),
                            fill=C(hat_color))

    # Face.
    eye_y = head_cy - 4 * k
    for side in (-1, 1):
        ex = cx + side * 13 * k
        d.ellipse([ex - 8 * k, eye_y - 7 * k, ex + 8 * k, eye_y + 7 * k], fill=C((250, 248, 240)))
        d.ellipse([ex - 3.5 * k + facing * 1.5 * k, eye_y - 3.5 * k,
                   ex + 3.5 * k + facing * 1.5 * k, eye_y + 3.5 * k], fill=C((40, 34, 30)))

    if glasses:
        for side in (-1, 1):
            ex = cx + side * 13 * k
            d.ellipse([ex - 11 * k, eye_y - 10 * k, ex + 11 * k, eye_y + 10 * k],
                      outline=C((72, 66, 62)), width=max(1, int(2 * k)))

    if nose > 0:
        nx0 = cx + facing * 2 * k
        nx1 = cx + facing * (2 + nose) * k
        x0, x1 = min(nx0, nx1), max(nx0, nx1)
        d.rounded_rectangle([x0, head_cy + 2 * k, x1, head_cy + 15 * k], radius=int(6 * k),
                            fill=C(shade(skin, 0.92)),
                            outline=C(shade(skin, 0.68)),
                            width=max(1, int(2 * k)))
    else:
        d.ellipse([cx - 5 * k, head_cy + 4 * k, cx + 5 * k, head_cy + 14 * k],
                  fill=C(shade(skin, 0.9)))

    if mustache:
        d.rounded_rectangle([cx - 15 * k, head_cy + 14 * k, cx + 15 * k, head_cy + 22 * k],
                            radius=int(4 * k), fill=C(hair))

    mouth_y = head_cy + 24 * k
    if mood in ("happy", "generous", "love"):
        d.arc([cx - 12 * k, mouth_y - 7 * k, cx + 12 * k, mouth_y + 8 * k], 15, 165,
              fill=C((122, 62, 52)), width=max(1, int(3 * k)))
    elif mood == "sad":
        d.arc([cx - 12 * k, mouth_y + 1 * k, cx + 12 * k, mouth_y + 14 * k], 195, 345,
              fill=C((122, 62, 52)), width=max(1, int(3 * k)))
    else:
        d.line([(cx - 8 * k, mouth_y + 4 * k), (cx + 8 * k, mouth_y + 4 * k)],
               fill=C((122, 62, 52)), width=max(1, int(3 * k)))


def boy(d, cx, ground, s=1.0, facing=1, pose="idle", mood="happy", dim=1.0,
        outfit=(196, 74, 62)):
    person(d, cx, ground, s=s, facing=facing, pose=pose, mood=mood, dim=dim,
           skin=WOOD_SKIN, hair=(96, 70, 44), hair_style="short",
           outfit=outfit, accent=(244, 236, 216), hat="cap", nose=44.0, height=0.9)


def oldman(d, cx, ground, s=1.0, facing=1, pose="idle", mood="happy", dim=1.0,
           outfit=(120, 92, 62)):
    person(d, cx, ground, s=s, facing=facing, pose=pose, mood=mood, dim=dim,
           skin=SKIN_LIGHT, hair=GREY_HAIR, hair_style="short",
           outfit=outfit, accent=(228, 218, 196), mustache=True, glasses=True, height=0.98)


def girl(d, cx, ground, s=1.0, facing=1, pose="idle", mood="happy", dim=1.0,
         outfit=(178, 70, 110)):
    person(d, cx, ground, s=s, facing=facing, pose=pose, mood=mood, dim=dim,
           hair=(72, 46, 34), hair_style="bob", outfit=outfit,
           accent=(246, 238, 232), dress=True, height=0.94)


def girl_hood(d, cx, ground, s=1.0, facing=1, pose="idle", mood="happy", dim=1.0,
              outfit=(190, 52, 52)):
    person(d, cx, ground, s=s, facing=facing, pose=pose, mood=mood, dim=dim,
           hair=(214, 214, 218), hair_style="hood", outfit=outfit,
           accent=(246, 238, 232), dress=True, height=0.9)


def man(d, cx, ground, s=1.0, facing=1, pose="idle", mood="happy", dim=1.0,
        outfit=(48, 84, 150)):
    person(d, cx, ground, s=s, facing=facing, pose=pose, mood=mood, dim=dim,
           hair=(52, 38, 30), hair_style="short", outfit=outfit,
           accent=(226, 230, 238), height=1.0)


def cat(d, cx, ground, s=1.0, facing=1, pose="sit", mood="happy", dim=1.0,
        outfit=(206, 126, 62)):
    def C(c):
        return shade(c, dim)

    k = s
    fur = outfit

    # Tail.
    d.line([(cx - 58 * k, ground - 44 * k), (cx - 104 * k, ground - 96 * k),
            (cx - 90 * k, ground - 140 * k)],
           fill=C(fur), width=int(13 * k), joint="curve")

    # Body and chest.
    d.ellipse([cx - 62 * k, ground - 112 * k, cx + 62 * k, ground], fill=C(fur))
    d.ellipse([cx - 28 * k, ground - 90 * k, cx + 28 * k, ground - 6 * k],
              fill=C(shade(fur, 1.14)))

    # Paws.
    for side in (-1, 1):
        d.ellipse([cx + side * 32 * k - 14 * k, ground - 16 * k,
                   cx + side * 32 * k + 14 * k, ground + 2 * k], fill=C(shade(fur, 1.14)))

    # Head.
    hy = ground - 154 * k
    for side in (-1, 1):
        d.polygon([(cx + side * 20 * k, hy - 26 * k), (cx + side * 40 * k, hy - 68 * k),
                   (cx + side * 48 * k, hy - 22 * k)], fill=C(fur))
        d.polygon([(cx + side * 27 * k, hy - 30 * k), (cx + side * 38 * k, hy - 56 * k),
                   (cx + side * 43 * k, hy - 28 * k)], fill=C((226, 160, 160)))
    d.ellipse([cx - 44 * k, hy - 42 * k, cx + 44 * k, hy + 42 * k], fill=C(fur))

    for side in (-1, 1):
        ex = cx + side * 17 * k
        d.ellipse([ex - 9 * k, hy - 12 * k, ex + 9 * k, hy + 4 * k], fill=C((250, 248, 240)))
        d.ellipse([ex - 4 * k + facing * 2 * k, hy - 8 * k, ex + 4 * k + facing * 2 * k, hy],
                  fill=C((40, 34, 30)))

    d.polygon([(cx - 6 * k, hy + 12 * k), (cx + 6 * k, hy + 12 * k), (cx, hy + 20 * k)],
              fill=C((200, 120, 120)))
    d.arc([cx - 15 * k, hy + 14 * k, cx + 2 * k, hy + 30 * k], 10, 80,
          fill=C((90, 60, 50)), width=max(1, int(3 * k)))
    d.arc([cx - 2 * k, hy + 14 * k, cx + 15 * k, hy + 30 * k], 100, 170,
          fill=C((90, 60, 50)), width=max(1, int(3 * k)))

    for side in (-1, 1):
        for i in (-1, 0, 1):
            d.line([(cx + side * 10 * k, hy + 18 * k + i * 4 * k),
                    (cx + side * 52 * k, hy + 10 * k + i * 10 * k)],
                   fill=C((240, 236, 228)), width=max(1, int(2 * k)))

    if pose == "paw_up":
        d.ellipse([cx + 26 * k, hy + 12 * k, cx + 60 * k, hy + 44 * k], fill=C(shade(fur, 1.12)))
        d.ellipse([cx + 34 * k, hy + 18 * k, cx + 52 * k, hy + 34 * k], fill=C((226, 160, 160)))
    if mood == "sad":
        for side in (-1, 1):
            ex = cx + side * 17 * k
            d.line([(ex - 9 * k, hy - 20 * k), (ex + 9 * k, hy - 16 * k)],
                   fill=C(shade(fur, 0.7)), width=max(1, int(3 * k)))


def wolf(d, cx, ground, s=1.0, facing=1, pose="idle", mood="happy", dim=1.0,
         outfit=(108, 110, 120)):
    def C(c):
        return shade(c, dim)

    k = s
    fur = outfit
    dark = shade(fur, 0.84)

    # Tail.
    d.line([(cx - 66 * k, ground - 120 * k), (cx - 118 * k, ground - 158 * k),
            (cx - 122 * k, ground - 208 * k)],
           fill=C(dark), width=int(20 * k), joint="curve")

    # Legs.
    for off in (-52, -20, 24, 56):
        limb(d, (cx + off * k, ground - 122 * k), (cx + off * k, ground),
             int(16 * k), C(shade(dark, 0.95)))

    # Body.
    d.ellipse([cx - 78 * k, ground - 204 * k, cx + 70 * k, ground - 100 * k], fill=C(fur))

    # Neck and head.
    hx = cx + 62 * facing * k
    hy = ground - 240 * k
    d.polygon([(cx + 18 * k, ground - 182 * k), (cx + 62 * k, ground - 212 * k),
               (hx - 8 * k, hy + 22 * k), (hx - 34 * k, hy + 8 * k)], fill=C(fur))

    for side in (-1, 1):
        d.polygon([(hx + side * 18 * k, hy - 28 * k), (hx + side * 30 * k, hy - 72 * k),
                   (hx + side * 44 * k, hy - 24 * k)], fill=C(fur))
    d.ellipse([hx - 40 * k, hy - 42 * k, hx + 40 * k, hy + 40 * k], fill=C(fur))

    # Snout.
    sx0 = hx + min(-6 * k, facing * 62 * k)
    sx1 = hx + max(24 * k, facing * 62 * k)
    d.ellipse([sx0, hy + 2 * k, sx1, hy + 36 * k], fill=C((152, 154, 162)))
    nx = hx + facing * 50 * k
    d.ellipse([nx - 5 * k, hy + 26 * k, nx + 5 * k, hy + 37 * k], fill=C((40, 36, 40)))

    for side in (-1, 1):
        ex = hx + side * 18 * k
        d.ellipse([ex - 8 * k, hy - 14 * k, ex + 8 * k, hy + 2 * k], fill=C((246, 240, 220)))
        d.ellipse([ex - 3 * k + facing * 2 * k, hy - 10 * k, ex + 3 * k + facing * 2 * k, hy - 2 * k],
                  fill=C((40, 34, 30)))

    d.arc([hx - 18 * k, hy + 20 * k, hx + 18 * k, hy + 44 * k], 10, 170,
          fill=C((60, 40, 40)), width=max(1, int(3 * k)))
    if mood in ("happy", "generous"):
        d.polygon([(hx - 9 * k, hy + 26 * k), (hx - 3 * k, hy + 26 * k), (hx - 6 * k, hy + 36 * k)],
                  fill=C((250, 250, 250)))
        d.polygon([(hx + 4 * k, hy + 26 * k), (hx + 10 * k, hy + 26 * k), (hx + 7 * k, hy + 36 * k)],
                  fill=C((250, 250, 250)))


def girl_blonde(d, cx, ground, s=1.0, facing=1, pose="idle", mood="happy", dim=1.0,
                outfit=(200, 84, 63)):
    person(d, cx, ground, s=s, facing=facing, pose=pose, mood=mood, dim=dim,
           hair=(226, 196, 120), hair_style="bob", outfit=outfit,
           accent=(246, 238, 232), dress=True, height=0.9)


def doctor(d, cx, ground, s=1.0, facing=1, pose="idle", mood="happy", dim=1.0,
           outfit=(238, 238, 242)):
    person(d, cx, ground, s=s, facing=facing, pose=pose, mood=mood, dim=dim,
           hair=(52, 38, 30), hair_style="short", outfit=outfit,
           accent=(90, 150, 160), height=1.0)


def bear(d, cx, ground, s=1.0, facing=1, pose="idle", mood="happy", dim=1.0,
         outfit=(122, 82, 48)):
    def C(c):
        return shade(c, dim)

    k = s
    fur = outfit
    dark = shade(fur, 0.82)

    for off in (-40, 34):
        limb(d, (cx + off * k, ground - 96 * k), (cx + off * k, ground), int(24 * k), C(dark))
        d.ellipse([cx + off * k - 22 * k, ground - 14 * k, cx + off * k + 22 * k, ground + 8 * k],
                  fill=C(dark))

    d.ellipse([cx - 78 * k, ground - 252 * k, cx + 78 * k, ground - 70 * k], fill=C(fur))
    d.ellipse([cx - 46 * k, ground - 212 * k, cx + 46 * k, ground - 92 * k],
              fill=C(shade(fur, 1.12)))

    if pose == "point":
        limb(d, (cx + 68 * k, ground - 214 * k), (cx + 116 * k, ground - 190 * k), int(24 * k), C(fur))
    else:
        limb(d, (cx - 68 * k, ground - 214 * k), (cx - 84 * k, ground - 152 * k), int(24 * k), C(fur))

    hy = ground - 302 * k
    for side in (-1, 1):
        d.ellipse([cx + side * 46 * k - 17 * k, hy - 48 * k, cx + side * 46 * k + 17 * k, hy - 14 * k],
                  fill=C(dark))
        d.ellipse([cx + side * 46 * k - 9 * k, hy - 40 * k, cx + side * 46 * k + 9 * k, hy - 22 * k],
                  fill=C(shade(fur, 1.18)))

    d.ellipse([cx - 58 * k, hy - 54 * k, cx + 58 * k, hy + 50 * k], fill=C(fur))
    d.ellipse([cx - 30 * k, hy + 6 * k, cx + 30 * k, hy + 44 * k], fill=C(shade(fur, 1.16)))
    d.ellipse([cx - 9 * k, hy + 10 * k, cx + 9 * k, hy + 26 * k], fill=C((58, 42, 36)))

    for side in (-1, 1):
        ex = cx + side * 24 * k
        d.ellipse([ex - 9 * k, hy - 18 * k, ex + 9 * k, hy - 2 * k], fill=C((250, 248, 240)))
        d.ellipse([ex - 3.5 * k + facing * 1.5 * k, hy - 14 * k,
                   ex + 3.5 * k + facing * 1.5 * k, hy - 5 * k], fill=C((40, 34, 30)))

    if mood == "sad":
        d.arc([cx - 16 * k, hy + 24 * k, cx + 16 * k, hy + 42 * k], 195, 345,
              fill=C((70, 50, 44)), width=max(1, int(3 * k)))
    else:
        d.arc([cx - 16 * k, hy + 18 * k, cx + 16 * k, hy + 40 * k], 10, 170,
              fill=C((70, 50, 44)), width=max(1, int(3 * k)))


def pig(d, cx, ground, s=1.0, facing=1, pose="idle", mood="happy", dim=1.0,
        outfit=(230, 154, 154)):
    def C(c):
        return shade(c, dim)

    k = s
    fur = outfit
    dark = shade(fur, 0.85)

    for off in (-30, 26):
        limb(d, (cx + off * k, ground - 74 * k), (cx + off * k, ground), int(20 * k), C(dark))

    d.ellipse([cx - 64 * k, ground - 174 * k, cx + 64 * k, ground - 58 * k], fill=C(fur))
    d.ellipse([cx - 40 * k, ground - 140 * k, cx + 40 * k, ground - 66 * k],
              fill=C(shade(fur, 1.1)))

    hy = ground - 212 * k
    for side in (-1, 1):
        d.polygon([(cx + side * 26 * k, hy - 34 * k), (cx + side * 40 * k, hy - 72 * k),
                   (cx + side * 54 * k, hy - 30 * k)], fill=C(dark))

    d.ellipse([cx - 50 * k, hy - 48 * k, cx + 50 * k, hy + 46 * k], fill=C(fur))
    d.ellipse([cx - 22 * k, hy + 8 * k, cx + 22 * k, hy + 42 * k], fill=C(shade(fur, 1.16)))
    for side in (-1, 1):
        d.ellipse([cx + side * 9 * k - 3 * k, hy + 20 * k, cx + side * 9 * k + 3 * k, hy + 30 * k],
                  fill=C(dark))

    for side in (-1, 1):
        ex = cx + side * 22 * k
        d.ellipse([ex - 8 * k, hy - 20 * k, ex + 8 * k, hy - 4 * k], fill=C((250, 248, 240)))
        d.ellipse([ex - 3 * k + facing * 1.5 * k, hy - 16 * k, ex + 3 * k + facing * 1.5 * k, hy - 7 * k],
                  fill=C((40, 34, 30)))

    if mood == "sad":
        d.arc([cx - 16 * k, hy + 26 * k, cx + 16 * k, hy + 42 * k], 195, 345,
              fill=C((120, 70, 70)), width=max(1, int(3 * k)))
    else:
        d.arc([cx - 16 * k, hy + 22 * k, cx + 16 * k, hy + 40 * k], 10, 170,
              fill=C((120, 70, 70)), width=max(1, int(3 * k)))


def draw_car(d, cx, base, body=(196, 72, 62)):
    d.rounded_rectangle([cx - 170, base - 92, cx + 170, base - 18], radius=28, fill=body)
    d.polygon([(cx - 92, base - 92), (cx - 48, base - 164), (cx + 58, base - 164),
               (cx + 100, base - 92)], fill=shade(body, 1.14))
    d.polygon([(cx - 78, base - 98), (cx - 46, base - 156), (cx + 52, base - 156),
               (cx + 84, base - 98)], fill=(122, 152, 172))
    for wx in (cx - 108, cx + 108):
        d.ellipse([wx - 38, base - 46, wx + 38, base + 24], fill=(42, 38, 42))
        d.ellipse([wx - 17, base - 25, wx + 17, base + 5], fill=(124, 120, 124))
    d.rounded_rectangle([cx - 26, base - 186, cx + 26, base - 166], radius=6, fill=(240, 220, 120))


def draw_house(d, cx, base, material="straw", w=180, h=150):
    walls = {"straw": (214, 186, 110), "wood": (150, 104, 62), "brick": (168, 82, 66)}
    wall = walls.get(material, walls["straw"])
    d.rectangle([cx - w, base - h, cx + w, base], fill=wall)
    d.polygon([(cx - w - 20, base - h), (cx, base - h - 92), (cx + w + 20, base - h)],
              fill=(128, 62, 52))
    d.rounded_rectangle([cx - 32, base - 96, cx + 32, base], radius=8, fill=(84, 56, 40))
    d.rectangle([cx + 56, base - h + 42, cx + 116, base - h + 100], fill=(240, 214, 150))
    d.line([(cx + 86, base - h + 42), (cx + 86, base - h + 100)], fill=(120, 80, 50), width=4)


def draw_desk_keyboard(d, x0, y0, x1, y1):
    """Table with a small keyboard, for the typing story."""
    d.rectangle([x0, y0, x1, y1], fill=(88, 64, 48))
    d.rectangle([x0, y0, x1, y0 + 12], fill=(116, 86, 64))

    # Little keyboard on the table.
    kx0 = (x0 + x1) / 2 - 96
    ky = y0 - 24
    d.rounded_rectangle([kx0, ky, kx0 + 192, ky + 24], radius=6, fill=(228, 224, 214))
    for row in range(2):
        for col in range(10):
            d.rectangle([kx0 + 10 + col * 17, ky + 5 + row * 9,
                         kx0 + 20 + col * 17, ky + 10 + row * 9], fill=(150, 146, 138))


def draw_cart(d, x0, y0, x1, y1, prop="plate"):
    d.rectangle([x0, y0, x1, y1], fill=WOOD)
    d.rectangle([x0, y0, x1, y0 + 16], fill=WOOD_LIGHT)
    for i in range(3):
        gx = x0 + 18 + i * ((x1 - x0 - 36) / 3.0)
        d.line([(gx, y0 + 24), (gx, y1 - 10)], fill=WOOD_DARK, width=4)
    # Wheels.
    for wx in (x0 + 54, x1 - 54):
        d.ellipse([wx - 30, y1 - 18, wx + 30, y1 + 42], fill=WOOD_DARK)
        d.ellipse([wx - 12, y1, wx + 12, y1 + 24], fill=WOOD_LIGHT)

    if prop == "plate":
        px, py = (x0 + x1) / 2, y0 - 6
        d.ellipse([px - 62, py - 20, px + 62, py + 18], fill=CREAM)
        for i in range(3):
            d.arc(
                [px - 44 + i * 8, py - 18, px + 20 + i * 8, py + 6],
                start=200,
                end=340,
                fill=(214, 126, 58),
                width=5,
            )
    elif prop == "wok":
        px, py = (x0 + x1) / 2, y0 - 4
        d.ellipse([px - 56, py - 26, px + 56, py + 20], fill=(52, 46, 46))
        d.ellipse([px - 44, py - 20, px + 44, py + 12], fill=(76, 66, 62))
        limb(d, (px + 50, py - 6), (px + 110, py - 26), 9, (52, 46, 46))


def draw_steam(img, cx, cy, width=110, height=110):
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    g = ImageDraw.Draw(glow)
    for i in range(3):
        x = cx - width / 2 + i * (width / 2)
        pts = []
        for step in range(16):
            t = step / 15.0
            y = cy - t * height
            xx = x + 16 * math.sin(t * math.pi * 2.2 + i * 1.4) * t
            pts.append((xx, y))
        g.line(pts, fill=(240, 233, 220, 95), width=7, joint="curve")
    return Image.alpha_composite(img, glow)


def ground_shadow(d, cx, rx=105):
    d.ellipse([cx - rx, GROUND - 12, cx + rx, GROUND + 20], fill=(22, 13, 34))


def draw_vignette(img):
    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(overlay)
    for y in range(int(H * 0.62), H):
        a = int((y - H * 0.62) / (H * 0.38) * 105)
        d.line([(0, y), (W, y)], fill=(8, 5, 16, a))
    return Image.alpha_composite(img, overlay)


SCENES = {
    # Shrek & Donkey — night market
    "sfs_01": dict(pose="wave", mood="happy", listener_pose="idle", listener_mood="happy",
                   cart="right", prop="plate", lanterns=6),
    "sfs_02": dict(pose="pocket", mood="sad", listener_pose="idle", listener_mood="neutral",
                   cart="right", prop="plate", lanterns=5),
    "sfs_03": dict(pose="chest", mood="generous", listener_pose="idle", listener_mood="happy",
                   cart="right", prop="plate", lanterns=6),
    "sfs_04": dict(pose="clasp", mood="love", listener_pose="idle", listener_mood="happy",
                   cart="right", prop="plate", lanterns=6),
    "sfs_05": dict(pose="wai", mood="happy", listener_pose="idle", listener_mood="happy",
                   cart="right", prop="wok", lanterns=5, speaker_side="right"),
    "sfs_06": dict(pose="fingers", mood="happy", listener_pose="wai", listener_mood="happy",
                   cart="right", prop="wok", lanterns=6),
    "sfs_07": dict(pose="wok", mood="happy", listener_pose="idle", listener_mood="happy",
                   cart="right", prop="wok", lanterns=5, speaker_side="right"),
    "sfs_08": dict(pose="sniff", mood="happy", listener_pose="idle", listener_mood="happy",
                   cart="right", prop="plate", lanterns=6),

    # Pinocchio — street outside the workshop
    "pin_01": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="happy"),
    "pin_02": dict(pose="clasp", mood="happy", listener_pose="idle", listener_mood="neutral"),
    "pin_03": dict(pose="chest", mood="surprised", listener_pose="idle", listener_mood="happy"),
    "pin_04": dict(pose="chest", mood="happy", listener_pose="point", listener_mood="surprised"),
    "pin_05": dict(pose="point", mood="surprised", listener_pose="clasp", listener_mood="sad"),
    "pin_06": dict(pose="clasp", mood="sad", listener_pose="point", listener_mood="neutral"),
    "pin_07": dict(pose="point", mood="neutral", listener_pose="clasp", listener_mood="sad"),
    "pin_08": dict(pose="clasp", mood="sad", listener_pose="idle", listener_mood="neutral"),

    # Little Red Riding Hood — forest
    "lrr_01": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="neutral",
                   speaker_side="right"),
    "lrr_02": dict(pose="clasp", mood="happy", listener_pose="idle", listener_mood="happy"),
    "lrr_03": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="happy",
                   speaker_side="right"),
    "lrr_04": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="neutral"),
    "lrr_05": dict(pose="point", mood="happy", listener_pose="chest", listener_mood="surprised",
                   speaker_side="right"),
    "lrr_06": dict(pose="chest", mood="surprised", listener_pose="idle", listener_mood="happy"),
    "lrr_07": dict(pose="point", mood="happy", listener_pose="clasp", listener_mood="sad",
                   speaker_side="right"),
    "lrr_08": dict(pose="wave", mood="sad", listener_pose="idle", listener_mood="happy"),

    # Asking her out — evening street
    "date_01": dict(pose="wave", mood="happy", listener_pose="idle", listener_mood="happy"),
    "date_02": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="happy",
                    speaker_side="right"),
    "date_03": dict(pose="chest", mood="generous", listener_pose="idle", listener_mood="happy"),
    "date_04": dict(pose="clasp", mood="happy", listener_pose="idle", listener_mood="happy",
                    speaker_side="right"),
    "date_05": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="neutral"),
    "date_06": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="happy",
                    speaker_side="right"),
    "date_07": dict(pose="chest", mood="happy", listener_pose="idle", listener_mood="neutral"),
    "date_08": dict(pose="clasp", mood="happy", listener_pose="idle", listener_mood="happy",
                    speaker_side="right"),

    # The typing cat — room with a desk
    "cat_01": dict(pose="paw_up", mood="happy", listener_pose="type", listener_mood="happy"),
    "cat_02": dict(pose="type", mood="happy", listener_pose="paw_up", listener_mood="happy",
                   speaker_side="right"),
    "cat_03": dict(pose="paw_up", mood="happy", listener_pose="type", listener_mood="happy"),
    "cat_04": dict(pose="type", mood="happy", listener_pose="paw_up", listener_mood="happy",
                   speaker_side="right"),
    "cat_05": dict(pose="paw_up", mood="happy", listener_pose="type", listener_mood="neutral"),
    "cat_06": dict(pose="type", mood="happy", listener_pose="paw_up", listener_mood="happy",
                   speaker_side="right"),
    "cat_07": dict(pose="sit", mood="sad", listener_pose="type", listener_mood="neutral"),
    "cat_08": dict(pose="clasp", mood="happy", listener_pose="paw_up", listener_mood="happy",
                   speaker_side="right"),

    # Goldilocks — cottage interior
    "gold_01": dict(pose="chest", mood="happy", listener_pose="point", listener_mood="neutral",
                    speaker_side="left"),
    "gold_02": dict(pose="point", mood="surprised", listener_pose="idle", listener_mood="sad"),
    "gold_03": dict(pose="clasp", mood="neutral", listener_pose="idle", listener_mood="happy"),
    "gold_04": dict(pose="clasp", mood="happy", listener_pose="idle", listener_mood="neutral",
                    speaker_side="right"),
    "gold_05": dict(pose="point", mood="sad", listener_pose="idle", listener_mood="neutral"),
    "gold_06": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="happy",
                    speaker_side="right"),
    "gold_07": dict(pose="chest", mood="happy", listener_pose="idle", listener_mood="sad",
                    speaker_side="right"),
    "gold_08": dict(pose="clasp", mood="sad", listener_pose="idle", listener_mood="neutral"),

    # Cinderella — clothes stall
    "cind_01": dict(pose="chest", mood="happy", listener_pose="idle", listener_mood="happy",
                    cart="right", prop="none"),
    "cind_02": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="neutral",
                    cart="right", prop="none", speaker_side="right"),
    "cind_03": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="happy",
                    cart="right", prop="none"),
    "cind_04": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="happy",
                    cart="right", prop="none", speaker_side="right"),
    "cind_05": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="neutral",
                    cart="right", prop="none"),
    "cind_06": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="happy",
                    cart="right", prop="none", speaker_side="right"),
    "cind_07": dict(pose="chest", mood="happy", listener_pose="idle", listener_mood="happy",
                    cart="right", prop="none"),
    "cind_08": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="happy",
                    cart="right", prop="none", speaker_side="right"),

    # Three little pigs — forest, house per material
    "pig_01": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="neutral",
                   house="straw"),
    "pig_02": dict(pose="chest", mood="happy", listener_pose="idle", listener_mood="neutral",
                   house="wood", speaker_side="right"),
    "pig_03": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="sad",
                   speaker_side="right"),
    "pig_04": dict(pose="chest", mood="neutral", listener_pose="idle", listener_mood="happy"),
    "pig_05": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="sad",
                   speaker_side="right"),
    "pig_06": dict(pose="chest", mood="neutral", listener_pose="idle", listener_mood="neutral",
                   house="wood"),
    "pig_07": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="happy",
                   house="brick"),
    "pig_08": dict(pose="point", mood="surprised", listener_pose="idle", listener_mood="happy",
                   house="brick", speaker_side="right"),

    # Taxi — street with a car behind
    "taxi_01": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="happy",
                    car=True),
    "taxi_02": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="neutral",
                    car=True, speaker_side="right"),
    "taxi_03": dict(pose="chest", mood="neutral", listener_pose="idle", listener_mood="happy",
                    car=True),
    "taxi_04": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="neutral",
                    car=True, speaker_side="right"),
    "taxi_05": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="happy",
                    car=True),
    "taxi_06": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="neutral",
                    car=True, speaker_side="right"),
    "taxi_07": dict(pose="chest", mood="sad", listener_pose="idle", listener_mood="happy",
                    car=True),
    "taxi_08": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="happy",
                    car=True, speaker_side="right"),

    # Doctor — clinic room
    "doc_01": dict(pose="idle", mood="neutral", listener_pose="idle", listener_mood="sad",
                   speaker_side="right"),
    "doc_02": dict(pose="chest", mood="sad", listener_pose="idle", listener_mood="neutral"),
    "doc_03": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="sad",
                   speaker_side="right"),
    "doc_04": dict(pose="chest", mood="neutral", listener_pose="idle", listener_mood="happy"),
    "doc_05": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="sad",
                   speaker_side="right"),
    "doc_06": dict(pose="chest", mood="neutral", listener_pose="idle", listener_mood="happy"),
    "doc_07": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="neutral",
                   speaker_side="right"),
    "doc_08": dict(pose="clasp", mood="happy", listener_pose="idle", listener_mood="happy"),

    # Meeting the family — living room
    "fam_01": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="happy"),
    "fam_02": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="neutral",
                   speaker_side="right"),
    "fam_03": dict(pose="clasp", mood="happy", listener_pose="idle", listener_mood="happy"),
    "fam_04": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="happy",
                   speaker_side="right"),
    "fam_05": dict(pose="chest", mood="happy", listener_pose="idle", listener_mood="neutral"),
    "fam_06": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="happy",
                   speaker_side="right"),
    "fam_07": dict(pose="clasp", mood="happy", listener_pose="idle", listener_mood="happy"),
    "fam_08": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="happy"),

    # A day in Bangkok — evening street
    "day_01": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="happy",
                   car=True),
    "day_02": dict(pose="chest", mood="happy", listener_pose="idle", listener_mood="neutral",
                   car=True, speaker_side="right"),
    "day_03": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="happy",
                   car=True),
    "day_04": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="neutral",
                   car=True, speaker_side="right"),
    "day_05": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="happy",
                   car=True),
    "day_06": dict(pose="chest", mood="happy", listener_pose="idle", listener_mood="neutral",
                   car=True, speaker_side="right"),
    "day_07": dict(pose="idle", mood="neutral", listener_pose="idle", listener_mood="happy",
                   car=True),
    "day_08": dict(pose="clasp", mood="happy", listener_pose="idle", listener_mood="happy",
                   car=True, speaker_side="right"),

    # Hotel check-in — lobby
    "hotel_01": dict(pose="chest", mood="neutral", listener_pose="idle", listener_mood="happy"),
    "hotel_02": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="neutral",
                     speaker_side="right"),
    "hotel_03": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="happy"),
    "hotel_04": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="neutral",
                     speaker_side="right"),
    "hotel_05": dict(pose="chest", mood="neutral", listener_pose="idle", listener_mood="happy"),
    "hotel_06": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="neutral",
                     speaker_side="right"),
    "hotel_07": dict(pose="clasp", mood="happy", listener_pose="idle", listener_mood="happy"),
    "hotel_08": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="happy",
                     speaker_side="right"),

    # First Thai lesson — classroom
    "lesson_01": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="neutral"),
    "lesson_02": dict(pose="chest", mood="neutral", listener_pose="idle", listener_mood="happy",
                      speaker_side="right"),
    "lesson_03": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="neutral"),
    "lesson_04": dict(pose="chest", mood="neutral", listener_pose="idle", listener_mood="happy",
                      speaker_side="right"),
    "lesson_05": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="neutral"),
    "lesson_06": dict(pose="chest", mood="neutral", listener_pose="idle", listener_mood="happy",
                      speaker_side="right"),
    "lesson_07": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="neutral"),
    "lesson_08": dict(pose="clasp", mood="happy", listener_pose="idle", listener_mood="happy",
                      speaker_side="right"),

    # Something's wrong — street restaurant
    "comp_01": dict(pose="point", mood="sad", listener_pose="idle", listener_mood="neutral",
                    cart="right", prop="plate"),
    "comp_02": dict(pose="chest", mood="neutral", listener_pose="idle", listener_mood="sad",
                    cart="right", prop="plate", speaker_side="right"),
    "comp_03": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="surprised",
                    cart="right", prop="plate"),
    "comp_04": dict(pose="chest", mood="neutral", listener_pose="idle", listener_mood="neutral",
                    cart="right", prop="plate", speaker_side="right"),
    "comp_05": dict(pose="point", mood="sad", listener_pose="idle", listener_mood="neutral",
                    cart="right", prop="plate"),
    "comp_06": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="neutral",
                    cart="right", prop="plate", speaker_side="right"),
    "comp_07": dict(pose="chest", mood="neutral", listener_pose="idle", listener_mood="happy",
                    cart="right", prop="plate"),
    "comp_08": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="happy",
                    cart="right", prop="plate", speaker_side="right"),

    # Repair phrases — sidewalk
    "rep_01": dict(pose="chest", mood="sad", listener_pose="idle", listener_mood="happy"),
    "rep_02": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="neutral",
                   speaker_side="right"),
    "rep_03": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="happy"),
    "rep_04": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="neutral",
                   speaker_side="right"),
    "rep_05": dict(pose="chest", mood="neutral", listener_pose="idle", listener_mood="happy"),
    "rep_06": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="neutral",
                   speaker_side="right"),
    "rep_07": dict(pose="clasp", mood="happy", listener_pose="idle", listener_mood="happy"),
    "rep_08": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="happy",
                   speaker_side="right"),

    # Small talk — café street
    "talk_01": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="neutral", car=True),
    "talk_02": dict(pose="chest", mood="happy", listener_pose="idle", listener_mood="happy",
                    car=True, speaker_side="right"),
    "talk_03": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="neutral", car=True),
    "talk_04": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="happy",
                    car=True, speaker_side="right"),
    "talk_05": dict(pose="chest", mood="happy", listener_pose="idle", listener_mood="neutral", car=True),
    "talk_06": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="happy",
                    car=True, speaker_side="right"),
    "talk_07": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="neutral", car=True),
    "talk_08": dict(pose="clasp", mood="happy", listener_pose="idle", listener_mood="happy",
                    car=True, speaker_side="right"),

    # Making plans — evening street
    "plan_01": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="neutral", car=True),
    "plan_02": dict(pose="chest", mood="happy", listener_pose="idle", listener_mood="happy",
                    car=True, speaker_side="right"),
    "plan_03": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="neutral", car=True),
    "plan_04": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="happy",
                    car=True, speaker_side="right"),
    "plan_05": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="neutral", car=True),
    "plan_06": dict(pose="chest", mood="happy", listener_pose="idle", listener_mood="happy",
                    car=True, speaker_side="right"),
    "plan_07": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="neutral", car=True),
    "plan_08": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="happy",
                    car=True, speaker_side="right"),

    # Fresh market — fruit stall
    "mkt_01": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="happy",
                   cart="right", prop="none"),
    "mkt_02": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="neutral",
                   cart="right", prop="none", speaker_side="right"),
    "mkt_03": dict(pose="chest", mood="neutral", listener_pose="idle", listener_mood="happy",
                   cart="right", prop="none"),
    "mkt_04": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="neutral",
                   cart="right", prop="none", speaker_side="right"),
    "mkt_05": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="happy",
                   cart="right", prop="none"),
    "mkt_06": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="happy",
                   cart="right", prop="none", speaker_side="right"),
    "mkt_07": dict(pose="clasp", mood="happy", listener_pose="idle", listener_mood="happy",
                   cart="right", prop="none"),
    "mkt_08": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="happy",
                   cart="right", prop="none", speaker_side="right"),

    # Lost — city street
    "lost_01": dict(pose="chest", mood="sad", listener_pose="idle", listener_mood="neutral", car=True),
    "lost_02": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="sad",
                    car=True, speaker_side="right"),
    "lost_03": dict(pose="point", mood="neutral", listener_pose="idle", listener_mood="happy",
                    car=True),
    "lost_04": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="neutral",
                    car=True, speaker_side="right"),
    "lost_05": dict(pose="chest", mood="neutral", listener_pose="idle", listener_mood="happy",
                    car=True),
    "lost_06": dict(pose="point", mood="happy", listener_pose="idle", listener_mood="neutral",
                    car=True, speaker_side="right"),
    "lost_07": dict(pose="clasp", mood="happy", listener_pose="idle", listener_mood="happy",
                    car=True),
    "lost_08": dict(pose="idle", mood="happy", listener_pose="idle", listener_mood="happy",
                    car=True, speaker_side="right"),
}

# The typing story shows a desk and keyboard on every card.
for _scene_id in list(SCENES):
    if _scene_id.startswith("cat_"):
        SCENES[_scene_id]["prop"] = "keyboard"

RIGS = {
    "ogre": ogre,
    "donkey": donkey,
    "vendor": vendor,
    "boy": boy,
    "oldman": oldman,
    "girl": girl,
    "girl_hood": girl_hood,
    "girl_blonde": girl_blonde,
    "man": man,
    "doctor": doctor,
    "cat": cat,
    "wolf": wolf,
    "bear": bear,
    "pig": pig,
}

# Rigs that take an outfit colour (so each character can look different).
COLOR_RIGS = {"boy", "oldman", "girl", "girl_hood", "girl_blonde", "man", "doctor",
              "cat", "wolf", "bear", "pig"}

RIG_SCALE = {"ogre": 0.98, "donkey": 0.9, "vendor": 1.12, "cat": 1.05, "wolf": 1.0,
             "bear": 1.0, "pig": 1.0}


def draw_rig(d, rig_name, x, facing, pose, mood, dim=1.0, colour=None, scale=None):
    rig = RIGS.get(rig_name, ogre)
    kwargs = dict(s=scale if scale is not None else RIG_SCALE.get(rig_name, 1.0),
                  facing=facing, pose=pose, mood=mood, dim=dim)
    if rig_name in COLOR_RIGS and colour:
        kwargs["outfit"] = hex_rgb(colour)
    rig(d, x, GROUND, **kwargs)


def render_card(story, card):
    scenario = SCENES.get(card["id"], dict(pose="idle", mood="happy", listener_pose="idle",
                                          listener_mood="happy"))
    bg = scenario.get("bg") or story.get("bg", "market")
    lanterns = scenario.get("lanterns", 5)

    img = Image.new("RGBA", (W, H))
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    build_background(img, glow, lanterns, bg=bg)
    img = Image.alpha_composite(img, glow)

    d = ImageDraw.Draw(img)
    if bg == "market":
        draw_lanterns(img, lanterns)
        d = ImageDraw.Draw(img)
        draw_distant_stalls(d)

    if scenario.get("house"):
        draw_house(d, 770, GROUND - 6, scenario["house"])
    if scenario.get("car"):
        draw_car(d, 500, GROUND - 8)

    characters = {c["id"]: c for c in story["characters"]}
    speaker_id = card["speaker"]
    listener_id = card.get("listener")

    speaker = characters[speaker_id]
    listener = characters[listener_id] if listener_id else None

    speaker_right = scenario.get("speaker_side", "left") == "right"
    speaker_x = RIGHT_X if speaker_right else LEFT_X
    listener_x = LEFT_X if speaker_right else RIGHT_X

    if listener:
        ground_shadow(d, listener_x, rx=88)
        draw_rig(
            d, listener.get("art_rig", "donkey"), listener_x,
            facing=-1 if speaker_right else 1,
            pose=scenario.get("listener_pose", "idle"),
            mood=scenario.get("listener_mood", "happy"),
            dim=0.68,
            colour=listener.get("color"),
            scale=RIG_SCALE.get(listener.get("art_rig"), 1.0) * 0.9,
        )

    cart = scenario.get("cart")
    cart_x0, cart_x1 = (640, 1000) if cart == "right" else (30, 390)

    def draw_speaker():
        ground_shadow(d, speaker_x, rx=100)
        draw_rig(
            d, speaker.get("art_rig", "ogre"), speaker_x,
            facing=-1 if speaker_right else 1,
            pose=scenario.get("pose", "idle"),
            mood=scenario.get("mood", "happy"),
            colour=speaker.get("color"),
        )

    if bg == "market" and cart:
        if speaker.get("art_rig") == "vendor":
            draw_speaker()
            draw_cart(d, cart_x0, GROUND - 92, cart_x1, GROUND + 26, scenario.get("prop", "plate"))
        else:
            draw_cart(d, cart_x0, GROUND - 92, cart_x1, GROUND + 26, scenario.get("prop", "plate"))
            draw_speaker()
    else:
        draw_speaker()

    if bg == "room" and scenario.get("prop") == "keyboard":
        draw_desk_keyboard(d, 540, GROUND - 86, 1000, GROUND + 34)

    if bg == "market" and cart and scenario.get("prop") in ("plate", "wok"):
        img = draw_steam(img, (cart_x0 + cart_x1) / 2, GROUND - 96, width=120, height=120)

    img = draw_vignette(img)
    return img.convert("RGB")


def render_cover(story):
    bg = story.get("bg", "market")
    img = Image.new("RGBA", (W, H))
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    build_background(img, glow, 6, bg=bg)
    img = Image.alpha_composite(img, glow)
    d = ImageDraw.Draw(img)

    if bg == "market":
        draw_lanterns(img, 6)
        d = ImageDraw.Draw(img)
        draw_distant_stalls(d)
        draw_cart(d, 620, GROUND - 92, 1000, GROUND + 26, "wok")

    chars = story["characters"]
    left = chars[0]
    right = chars[1] if len(chars) > 1 else None

    if right:
        draw_rig(d, right.get("art_rig", "donkey"), 700, facing=-1, pose="idle", mood="happy",
                 colour=right.get("color"),
                 scale=RIG_SCALE.get(right.get("art_rig"), 1.0) * 0.95)
    draw_rig(d, left.get("art_rig", "ogre"), 330, facing=1, pose="wave", mood="generous",
             colour=left.get("color"))

    if bg == "street":
        draw_car(d, 500, GROUND - 8)
    if bg == "market":
        img = draw_steam(img, 810, GROUND - 96, width=120, height=110)

    img = draw_vignette(img).convert("RGB")
    return img


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, "WEBP", quality=82, method=4)
    return os.path.getsize(path)


def cover_paths():
    """id -> cover asset path, read from the library index (covers are declared there)."""
    try:
        with io.open(os.path.join(STORIES_DIR, "index.json"), encoding="utf-8") as fh:
            index = json.load(fh)
        return {s.get("id"): s.get("cover", "") for s in index.get("stories", [])}
    except Exception:
        return {}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--story", help="story id (json filename without extension)")
    parser.add_argument("--all", action="store_true", help="regenerate every story")
    parser.add_argument("--out", default=ART_DIR)
    args = parser.parse_args()

    if not args.story and not args.all:
        parser.error("pass --story <id> or --all")

    if args.all:
        ids = [
            os.path.splitext(f)[0]
            for f in sorted(os.listdir(STORIES_DIR))
            if f.endswith(".json") and f != "index.json"
        ]
    else:
        ids = [args.story]

    covers = cover_paths()
    total = 0
    for story_id in ids:
        path = os.path.join(STORIES_DIR, story_id + ".json")
        with io.open(path, encoding="utf-8") as fh:
            story = json.load(fh)

        print(f"{story_id}: {len(story['cards'])} cards")
        cover = render_cover(story)
        cover_rel = covers.get(story_id) or f"art/{story_id}_cover.webp"
        cover_name = os.path.basename(cover_rel).rsplit(".", 1)[0] + ".webp"
        total += save(cover, os.path.join(args.out, cover_name))

        for card in story["cards"]:
            scene = render_card(story, card)
            name = os.path.basename(card["image"]).rsplit(".", 1)[0] + ".webp"
            total += save(scene, os.path.join(args.out, name))
            print(f"  {name}")

    print(f"total art: {total / 1024:.1f} KB")


if __name__ == "__main__":
    main()
