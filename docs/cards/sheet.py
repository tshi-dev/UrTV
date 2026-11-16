"""The review's second picture per card: its other photos side by side (bright, dark, the glass-clock home...), each at half size.
   python3 docs/cards/sheet.py SHOTS_DIR OUT_DIR   (after shoot.mjs with SHEET=SHOTS_DIR)"""
import re, sys, pathlib
from PIL import Image, ImageDraw

src, out = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
groups = {}
for f in sorted(src.glob('*.png')):
    m = re.match(r'(\d\d-[a-z0-9-]+?-(?:before|after))-(.+)\.png$', f.name)
    if m: groups.setdefault(m.group(1), []).append(f)
for key, files in groups.items():
    n = len(files); cols = 2; rows = (n + 1) // 2
    sheet = Image.new('RGB', (1920, 540 * rows), (16, 16, 16))
    for i, f in enumerate(files):
        im = Image.open(f).convert('RGB').resize((960, 540), Image.LANCZOS)
        x, y = (1 - i % cols) * 960, (i // cols) * 540   # right to left, as Arabic reads
        sheet.paste(im, (x, y))
        ImageDraw.Draw(sheet).rectangle((x, y, x + 959, y + 539), outline=(16, 16, 16), width=3)
    sheet.save(out / f'{key}-more.png', optimize=True)
    print('wrote', key + '-more.png')
