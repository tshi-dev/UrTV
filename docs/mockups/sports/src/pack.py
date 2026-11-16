"""docs/mockups/sports/*.png to 64 colours, no dither: about a quarter of the size, no visible change on a TV photo."""
from pathlib import Path
from PIL import Image
for p in sorted(Path(__file__).resolve().parents[1].glob("*.png")):
    Image.open(p).convert("RGB").quantize(64, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).save(p, optimize=True)
    print(p.name, p.stat().st_size)
