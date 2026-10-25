"""Draws the mod's textures. Run from the repo root: python tools/gen_textures.py"""
import math
import random
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ASSETS = Path("src/main/resources/assets/mystic")
CLEAR = (0, 0, 0, 0)


def save(image, path):
    out = ASSETS / path
    out.parent.mkdir(parents=True, exist_ok=True)
    image.save(out)
    print("wrote", out)


# Spell sigils: white lines with a soft glow around them, on transparent. Glyphs.java tints them to the spell's colour.

SIGIL = 256
SCALE = 4  # drawn large, then shrunk for smooth lines


class Sigil:
    def __init__(self):
        self.size = SIGIL * SCALE
        self.image = Image.new("L", (self.size, self.size), 0)
        self.draw = ImageDraw.Draw(self.image)
        self.c = self.size / 2

    def px(self, r):
        """A radius given as a fraction of the sigil's half-width, in drawing pixels."""
        return r * self.c

    def circle(self, r, width, shade=255):
        r = self.px(r)
        self.draw.ellipse([self.c - r, self.c - r, self.c + r, self.c + r], outline=shade, width=round(width * SCALE))

    def polygon(self, r, sides, turn, width, shade=255):
        r = self.px(r)
        points = [(self.c + r * math.cos(turn + 2 * math.pi * i / sides), self.c + r * math.sin(turn + 2 * math.pi * i / sides))
                  for i in range(sides)]
        self.draw.polygon(points, outline=shade, width=round(width * SCALE))

    def spoke(self, angle, r0, r1, width, shade=255):
        a, b = self.px(r0), self.px(r1)
        self.draw.line([(self.c + a * math.cos(angle), self.c + a * math.sin(angle)),
                        (self.c + b * math.cos(angle), self.c + b * math.sin(angle))], fill=shade, width=round(width * SCALE))

    def arc(self, r, start, end, width, shade=255):
        r = self.px(r)
        self.draw.arc([self.c - r, self.c - r, self.c + r, self.c + r], start, end, fill=shade, width=round(width * SCALE))

    def runes(self, r, count, height, rng, shade=230):
        """A ring of made-up runes, each a few strokes on a small grid, standing upright from the middle outward."""
        for i in range(count):
            angle = 2 * math.pi * i / count
            strokes = rune(rng)
            cx, cy = self.c + self.px(r) * math.cos(angle), self.c + self.px(r) * math.sin(angle)
            h = self.px(height)
            w = h * 0.6
            # The rune's "up" points away from the middle
            ux, uy = math.cos(angle), math.sin(angle)
            vx, vy = -uy, ux
            for (x0, y0), (x1, y1) in strokes:
                p0 = (cx + vx * (x0 - 0.5) * w + ux * (0.5 - y0) * h, cy + vy * (x0 - 0.5) * w + uy * (0.5 - y0) * h)
                p1 = (cx + vx * (x1 - 0.5) * w + ux * (0.5 - y1) * h, cy + vy * (x1 - 0.5) * w + uy * (0.5 - y1) * h)
                self.draw.line([p0, p1], fill=shade, width=round(1.6 * SCALE))

    def finish(self, glow=6.0):
        line = self.image.resize((SIGIL, SIGIL), Image.LANCZOS)
        halo = line.filter(ImageFilter.GaussianBlur(glow)).point(lambda v: min(255, int(v * 1.6)))
        light = ImageChops.add(line, halo.point(lambda v: v // 2))
        out = Image.new("RGBA", (SIGIL, SIGIL), CLEAR)
        pixels = out.load()
        src = light.load()
        for y in range(SIGIL):
            for x in range(SIGIL):
                v = src[x, y]
                if v > 26:
                    pixels[x, y] = (255, 255, 255, v)
        return out


def rune(rng):
    """Two to four strokes between points of a 3x3 grid."""
    points = [(x / 2, y / 2) for x in range(3) for y in range(3)]
    strokes = []
    for _ in range(rng.randint(2, 4)):
        a, b = rng.sample(points, 2)
        strokes.append((a, b))
    return strokes


def mandala():
    """The mandala shield: rune bands, two squares making an eight-pointed star, and a turning wheel inside."""
    rng = random.Random(7)
    s = Sigil()
    s.circle(0.97, 3.0)
    s.circle(0.90, 1.4)
    s.runes(0.935, 28, 0.05, rng)
    s.circle(0.80, 2.2)
    for i in range(48):
        s.spoke(2 * math.pi * i / 48, 0.80, 0.86 if i % 2 else 0.88, 1.2)
    s.polygon(0.78, 4, 0.0, 2.4)
    s.polygon(0.78, 4, math.pi / 4, 2.4)
    s.circle(0.56, 2.0)
    s.circle(0.50, 1.2)
    s.runes(0.53, 16, 0.04, rng, 200)
    s.polygon(0.48, 8, math.pi / 8, 1.6)
    for i in range(8):
        s.spoke(2 * math.pi * i / 8, 0.18, 0.48, 1.2, 180)
    s.circle(0.26, 2.0)
    s.polygon(0.24, 4, math.pi / 4, 1.6)
    s.circle(0.10, 2.2)
    return s.finish()


def time_mandala():
    """The Eye of Agamotto's sigil: a clock face of runes, with hours, minutes and turning arcs."""
    rng = random.Random(12)
    s = Sigil()
    s.circle(0.97, 2.6)
    for i in range(60):
        long = i % 5 == 0
        s.spoke(2 * math.pi * i / 60, 0.86 if long else 0.91, 0.96, 2.4 if long else 1.0)
    s.circle(0.85, 1.6)
    s.runes(0.77, 12, 0.08, rng)
    s.circle(0.68, 2.0)
    for i in range(3):
        start = i * 120 + 10
        s.arc(0.62, start, start + 80, 3.0)
    s.circle(0.55, 1.2)
    s.runes(0.48, 24, 0.045, rng, 190)
    s.circle(0.40, 1.8)
    s.polygon(0.38, 3, -math.pi / 2, 1.6)
    s.polygon(0.38, 3, math.pi / 2, 1.6)
    s.spoke(-math.pi / 2, 0.0, 0.33, 2.4)
    s.spoke(math.pi / 6, 0.0, 0.22, 2.4)
    s.circle(0.08, 2.0)
    return s.finish()


def portal_ring():
    """A sling ring portal's rim: a hot ring of fire with sparks streaking off along it."""
    rng = random.Random(3)
    s = Sigil()
    for r, width, shade in ((0.80, 5.0, 255), (0.80, 9.0, 150), (0.84, 2.0, 200), (0.76, 2.0, 170)):
        s.circle(r, width, shade)
    for _ in range(420):
        angle = rng.uniform(0, 2 * math.pi)
        r = rng.gauss(0.80, 0.045)
        length = rng.uniform(0.05, 0.28)
        shade = rng.randint(120, 255)
        steps = 6
        # Streaks curve along the ring, flung slightly outward as they go
        points = []
        for k in range(steps + 1):
            a = angle + length * k / steps
            rr = r + 0.08 * length * k / steps * rng.uniform(0.5, 1.5)
            points.append((s.c + s.px(rr) * math.cos(a), s.c + s.px(rr) * math.sin(a)))
        s.draw.line(points, fill=shade, width=round(rng.uniform(0.6, 1.8) * SCALE))
    for _ in range(140):
        angle = rng.uniform(0, 2 * math.pi)
        r = rng.uniform(0.86, 0.99)
        d = s.px(0.008)
        x, y = s.c + s.px(r) * math.cos(angle), s.c + s.px(r) * math.sin(angle)
        s.draw.ellipse([x - d, y - d, x + d, y + d], fill=rng.randint(150, 255))
    return s.finish(glow=4.0)


def portal_glow():
    """A faint haze filling the portal, brighter toward the rim, with a slow swirl to it."""
    out = Image.new("RGBA", (SIGIL, SIGIL), CLEAR)
    pixels = out.load()
    c = SIGIL / 2
    for y in range(SIGIL):
        for x in range(SIGIL):
            dx, dy = (x + 0.5 - c) / c, (y + 0.5 - c) / c
            r = math.hypot(dx, dy) / 0.8
            if r >= 1.0:
                continue
            swirl = 0.5 + 0.5 * math.sin(math.atan2(dy, dx) * 5 + r * 9)
            v = round(255 * (0.12 + 0.3 * r ** 3 + 0.1 * swirl * r))
            pixels[x, y] = (255, 255, 255, v)
    return out


# Item and armor pixel art

GOLD_DARK = (122, 78, 18, 255)
GOLD = (214, 160, 48, 255)
GOLD_LIGHT = (255, 222, 120, 255)
OUTLINE = (40, 26, 10, 255)


def paint(rows, colors):
    image = Image.new("RGBA", (16, 16), CLEAR)
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                image.putpixel((x, y), colors[ch])
    return image


def sling_ring():
    """Two gold finger loops joined under a plate, like knuckle-dusters for two fingers."""
    return paint([
        "................",
        "................",
        "................",
        "...kkkkkkkkkk...",
        "..kllllllllllk..",
        "..kgggwwwggggk..",
        "..kdddddddddddk.",
        ".kkgkdkkkkgkdk..",
        "kgk..kgk.kgk..k.",
        "kl....dkkl....dk",
        "kl....dkkl....dk",
        "kg....dkkg....dk",
        ".kg..gk..kg..gk.",
        "..kddk....kddk..",
        "................",
        "................",
    ], {"k": OUTLINE, "d": GOLD_DARK, "g": GOLD, "l": GOLD_LIGHT, "w": (255, 250, 220, 255)})


def cloak_item():
    """The Cloak of Levitation, hanging: a high collar, gold clasps, and red folds flaring at the hem."""
    red_dark = (92, 10, 14, 255)
    red = (176, 24, 28, 255)
    red_light = (222, 64, 56, 255)
    return paint([
        "...kk......kk...",
        "..krrk....krrk..",
        "..krlrkkkkrlrk..",
        "..kdrlrrrrlrdk..",
        "...kdgrrrrgdk...",
        "...krrddddrrk...",
        "..krlrrddrrlrk..",
        "..krlrrddrrlrk..",
        ".krlrrdrrdrrlrk.",
        ".krlrdrrrrdrlrk.",
        ".krlrdrrrrdrlrk.",
        "krlrrdrrrrdrrlrk",
        "krlrdrrrrrrdrlrk",
        "krlrdrrrrrrdrlrk",
        "kdgdkdgdkdgdkdgk",
        ".kk.kk.kk.kk.kk.",
    ], {"k": (40, 6, 8, 255), "d": red_dark, "r": red, "l": red_light, "g": GOLD})


def eye_item():
    """The Eye of Agamotto: a round gold amulet, its lids parted on the green stone inside, on a loop of chain."""
    green_dark = (10, 110, 40, 255)
    green = (40, 220, 90, 255)
    green_light = (190, 255, 200, 255)
    return paint([
        "......kkkk......",
        ".....kd..dk.....",
        "......kddk......",
        "....kkkllkkk....",
        "...kgllggllgk...",
        "..kgldkkkkdlgk..",
        ".kgldkeeeekdlgk.",
        ".kgdkeenweekdgk.",
        ".kgdkeewweekdgk.",
        ".kgldkeeeekdlgk.",
        "..kgldkkkkdlgk..",
        "...kgddggddgk...",
        "....kkgddgkk....",
        "......kddk......",
        ".......kk.......",
        "................",
    ], {"k": OUTLINE, "d": GOLD_DARK, "g": GOLD, "l": GOLD_LIGHT, "e": green, "n": green_dark, "w": green_light})


def paint_box(image, u, v, w, h, d, painter):
    """Paints each face of a model box laid out from (u, v): painter(face, x, y, width, height)."""
    faces = {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }
    for face, (x, y, fw, fh) in faces.items():
        painter(face, x, y, fw, fh)


def tunic():
    """The sorcerer's tunic, worn over the body and arms: dark blue, belted and strapped in brown leather, with
    blue-grey sleeves wrapped at the wrists."""
    rng = random.Random(5)
    image = Image.new("RGBA", (64, 32), CLEAR)
    navy = (28, 38, 82, 255)
    navy_dark = (18, 24, 56, 255)
    navy_light = (46, 60, 116, 255)
    under = (54, 74, 120, 255)
    leather = (86, 52, 28, 255)
    leather_dark = (56, 32, 16, 255)
    sleeve = (70, 88, 122, 255)
    sleeve_dark = (52, 66, 96, 255)

    def cloth(base, dark, light):
        roll = rng.random()
        return dark if roll < 0.12 else light if roll > 0.9 else base

    def body(face, x, y, w, h):
        for j in range(h):
            for i in range(w):
                color = cloth(navy, navy_dark, navy_light)
                if face == "front":
                    # Inner layer showing at the crossed collar
                    if j < 4 and abs(i - 3.5) < 2.5 - j * 0.5:
                        color = under
                    # A strap from the right shoulder to the left hip
                    if abs(i - (j * 0.6)) < 0.8 and j < 8:
                        color = leather
                    # The belt, with a gold buckle
                    if j in (7, 8):
                        color = GOLD if i in (3, 4) else leather
                    # The tunic falls in folds below the belt
                    if j > 8 and i in (1, 6):
                        color = navy_dark
                elif face in ("back", "left", "right") and j in (7, 8):
                    color = leather
                elif face == "back" and abs((w - 1 - i) - (j * 0.6)) < 0.8 and j < 7:
                    color = leather_dark
                elif face == "top":
                    color = navy_dark
                image.putpixel((x + i, y + j), color)

    def arm(face, x, y, w, h):
        for j in range(h):
            for i in range(w):
                color = cloth(sleeve, sleeve_dark, navy_light)
                if j < 3:
                    color = cloth(navy, navy_dark, navy_light)
                if j >= 8:
                    # Leather wrapped round the forearm, laced across
                    color = leather if (i + j) % 3 else leather_dark
                if j == 11:
                    color = leather_dark
                image.putpixel((x + i, y + j), color)

    paint_box(image, 16, 16, 8, 12, 4, body)
    paint_box(image, 40, 16, 4, 12, 4, arm)
    return image


def cloak():
    """The Cloak of Levitation (CloakModel): deep red folds with a gold hem, a darker lining, gold-edged mantles over
    the shoulders, and the high collar."""
    rng = random.Random(9)
    image = Image.new("RGBA", (64, 64), CLEAR)
    red = (164, 20, 26, 255)
    red_dark = (110, 12, 16, 255)
    red_light = (204, 48, 44, 255)
    lining = (78, 8, 12, 255)

    def folds(w, h, hem_rows=1, edges=True):
        def paint(face, x, y, fw, fh):
            for j in range(fh):
                for i in range(fw):
                    fold = i % 3 == 0
                    color = red_dark if fold else red_light if (i % 3 == 1 and rng.random() < 0.5) else red
                    if face in ("top", "bottom"):
                        color = red_dark
                    elif edges and face in ("front", "back") and (i in (0, fw - 1)):
                        color = GOLD if j % 2 == 0 else GOLD_DARK
                    if face != "top" and j >= fh - hem_rows:
                        color = GOLD if (i + j) % 2 == 0 else GOLD_LIGHT
                    image.putpixel((x + i, y + j), color)
        return paint

    # The cloak: its outside is the "front" face, which the model turns to face backward
    def cloak_face(face, x, y, fw, fh):
        if face == "back":
            for j in range(fh):
                for i in range(fw):
                    image.putpixel((x + i, y + j), lining if j < fh - 1 else GOLD_DARK)
        else:
            folds(12, 20)(face, x, y, fw, fh)

    paint_box(image, 0, 0, 12, 20, 1, cloak_face)
    paint_box(image, 0, 24, 7, 5, 7, folds(7, 5, edges=False))
    paint_box(image, 28, 24, 7, 5, 7, folds(7, 5, edges=False))

    def collar(face, x, y, fw, fh):
        for j in range(fh):
            for i in range(fw):
                color = red_dark if i % 2 == 0 else red
                if face in ("top", "bottom") or j == 0:
                    color = GOLD
                image.putpixel((x + i, y + j), color)

    paint_box(image, 0, 40, 11, 7, 1, collar)
    paint_box(image, 24, 40, 1, 7, 6, collar)
    paint_box(image, 38, 40, 1, 7, 6, collar)
    return image


def icon():
    """The mod icon: the Eye of Agamotto's sigil in green around an orange portal ring."""
    ring = portal_ring().resize((128, 128), Image.LANCZOS)
    eye = time_mandala().resize((96, 96), Image.LANCZOS)
    out = Image.new("RGBA", (128, 128), (10, 8, 16, 255))
    for image, offset, tint in ((ring, (0, 0), (255, 140, 30)), (eye, (16, 16), (60, 255, 110))):
        layer = Image.new("RGBA", (128, 128), CLEAR)
        tinted = Image.new("RGBA", image.size, CLEAR)
        src, dst = image.load(), tinted.load()
        for y in range(image.size[1]):
            for x in range(image.size[0]):
                v = src[x, y][3]
                dst[x, y] = (tint[0] * v // 255, tint[1] * v // 255, tint[2] * v // 255, 255)
        layer.paste(tinted, offset)
        out = ImageChops.add(out, layer)
    return out


def main():
    save(mandala(), "textures/effect/mandala.png")
    save(time_mandala(), "textures/effect/time_mandala.png")
    save(portal_ring(), "textures/effect/portal_ring.png")
    save(portal_glow(), "textures/effect/portal_glow.png")
    save(sling_ring(), "textures/item/sling_ring.png")
    save(cloak_item(), "textures/item/cloak_of_levitation.png")
    save(eye_item(), "textures/item/eye_of_agamotto.png")
    save(tunic(), "textures/entity/equipment/humanoid/sorcerer.png")
    save(cloak(), "textures/entity/cloak_of_levitation.png")
    save(icon(), "icon.png")


if __name__ == "__main__":
    main()
