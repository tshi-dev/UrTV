"""Create the optional Mirqab black-and-gold TV wallpaper using Pillow."""

from math import cos, pi, sin
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


WIDTH, HEIGHT = 1920, 1080
OUT = Path(__file__).with_name("mirqab-black-gold.jpg")


def star_points(cx, cy, outer, inner, rotation=-pi / 2, count=8):
    points = []
    for i in range(count * 2):
        radius = outer if i % 2 == 0 else inner
        angle = rotation + i * pi / count
        points.append((cx + cos(angle) * radius, cy + sin(angle) * radius))
    return points


def main():
    # A charcoal field with a quiet warm glow toward the center-right.
    pixels = Image.new("RGB", (WIDTH, HEIGHT))
    px = pixels.load()
    for y in range(HEIGHT):
        for x in range(WIDTH):
            glow = max(0.0, 1.0 - (((x - 1300) / 1150) ** 2 + ((y - 535) / 900) ** 2) ** 0.5)
            edge = min(1.0, ((x / WIDTH) ** 2 + ((y - HEIGHT / 2) / HEIGHT) ** 2) ** 0.5)
            px[x, y] = (
                int(5 + glow * 9 + edge * 2),
                int(7 + glow * 7 + edge * 2),
                int(10 + glow * 3 + edge * 3),
            )

    # A repeating eight-point rosette lattice, drawn in subdued antique gold.
    lattice = Image.new("RGBA", pixels.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(lattice)
    gold = (207, 166, 77, 38)
    gold_dim = (177, 136, 54, 26)
    step_x, step_y = 192, 192
    for row, cy in enumerate(range(-96, HEIGHT + step_y, step_y)):
        offset = 0 if row % 2 == 0 else step_x // 2
        for cx0 in range(-step_x, WIDTH + step_x, step_x):
            cx = cx0 + offset
            d.polygon(star_points(cx, cy, 68, 28), outline=gold, width=2)
            d.ellipse((cx - 48, cy - 48, cx + 48, cy + 48), outline=gold_dim, width=1)
            for angle in range(0, 360, 45):
                a = angle * pi / 180
                d.line((cx + cos(a) * 28, cy + sin(a) * 28,
                        cx + cos(a) * 68, cy + sin(a) * 68), fill=gold_dim, width=2)

    # A larger, softly lit rosette contains a football-like geometric core.
    # It is an original decorative emblem, not a reproduction of a club crest.
    cx, cy = 1450, 540
    d.ellipse((cx - 345, cy - 345, cx + 345, cy + 345), outline=(204, 165, 83, 44), width=3)
    d.ellipse((cx - 325, cy - 325, cx + 325, cy + 325), outline=(204, 165, 83, 30), width=2)
    d.polygon(star_points(cx, cy, 305, 245), outline=(218, 180, 96, 54), width=3)
    d.polygon(star_points(cx, cy, 210, 163, rotation=-pi / 2 + pi / 8), outline=(218, 180, 96, 42), width=2)
    ball_r = 112
    d.ellipse((cx - ball_r, cy - ball_r, cx + ball_r, cy + ball_r), outline=(224, 187, 105, 68), width=3)
    d.polygon(star_points(cx, cy, 55, 36, count=5), outline=(224, 187, 105, 72), width=2)
    for i in range(5):
        angle = -pi / 2 + i * 2 * pi / 5
        bx, by = cx + cos(angle) * 86, cy + sin(angle) * 86
        d.polygon(star_points(bx, by, 25, 17, rotation=angle, count=5), outline=(224, 187, 105, 56), width=2)
        d.line((cx + cos(angle) * 55, cy + sin(angle) * 55, bx, by), fill=(224, 187, 105, 48), width=2)

    # A restrained mihrab arch joins the rosette to the surrounding lattice.
    arch = [(cx - 245, cy + 280), (cx - 245, cy - 15)]
    arch += [(cx - 245 * cos(pi * i / 32), cy - 15 - 210 * sin(pi * i / 32)) for i in range(33)]
    arch += [(cx + 245, cy + 280)]
    d.line(arch, fill=(210, 171, 87, 36), width=3, joint="curve")
    inner_arch = [(cx - 226, cy + 270), (cx - 226, cy - 13)]
    inner_arch += [(cx - 226 * cos(pi * i / 32), cy - 13 - 194 * sin(pi * i / 32)) for i in range(33)]
    inner_arch += [(cx + 226, cy + 270)]
    d.line(inner_arch, fill=(210, 171, 87, 24), width=2, joint="curve")

    pixels = Image.alpha_composite(pixels.convert("RGBA"), lattice)
    pixels = pixels.convert("RGB").filter(ImageFilter.GaussianBlur(0.15))
    pixels.save(OUT, "JPEG", quality=92, optimize=True, progressive=True)
    print(f"Created {OUT} ({pixels.width}x{pixels.height})")


if __name__ == "__main__":
    main()
