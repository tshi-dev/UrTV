"""Makes the review's PNGs small enough to keep in git: 256 colours with dithering (about a third of the size; the words stay sharp).
   python3 docs/cards/pack.py docs/cards/*.png"""
import sys
from PIL import Image
for f in sys.argv[1:]:
    im = Image.open(f)
    if im.mode == 'P': continue   # already packed
    im.convert('RGB').quantize(256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.FLOYDSTEINBERG).save(f, optimize=True)
