#!/usr/bin/env python3
"""4.4 «الساعة: زجاجية خلف الصورة» on the person's own photo: makes its mask on THIS computer, with the same offline tool CI runs on our
photos (.github/tools/photo_masks.py). Nothing is sent anywhere: the photo is read here and one PNG is written beside it.

    python3 optimize/photo_mask.py PHOTO.jpg [MASK.png]

MASK.png (default: PHOTO-mask.png): the photo's size, white = what stands in front of the clock (buildings, mountains, people, trees),
black = the sky. Send the two from the phone page: «صوري» ← «صورة مع قناعها» (docs/OWN-PHOTO-MASK.md).
It works only on a photo whose sky has a clean edge (a clear or evenly coloured sky above the scenery); for any other photo it says so
and writes nothing: an AI image tool can still make one (the guide has the prompt), or the photo shows with the clock in front.
Needs numpy, scipy, opencv-python-headless and Pillow (not in optimize/requirements.txt: a separate venv is fine):
    python3 -m venv mask-venv && mask-venv/bin/pip install numpy scipy opencv-python-headless Pillow
    mask-venv/bin/python optimize/photo_mask.py PHOTO.jpg"""
import importlib.util
import sys
from pathlib import Path

TOOL = Path(__file__).resolve().parents[1] / ".github/tools/photo_masks.py"


def main(argv):
    if len(argv) not in (2, 3) or argv[1].startswith("-"):
        print(__doc__.split("\n\n")[1].strip())
        return 2
    photo = Path(argv[1])
    out = Path(argv[2]) if len(argv) == 3 else photo.with_name(photo.stem + "-mask.png")
    if not photo.is_file():
        print(f"!! photo_mask.py: no such photo: {photo.name}")
        return 1
    try:
        spec = importlib.util.spec_from_file_location("photo_masks", TOOL)
        tool = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(tool)
        from PIL import Image
    except ImportError as e:
        print(f"!! photo_mask.py needs {e.name}: python3 -m venv mask-venv && mask-venv/bin/pip install numpy scipy opencv-python-headless Pillow,"
              " then run it with mask-venv/bin/python")
        return 1
    src = Image.open(photo)
    if abs(src.width * 9 - src.height * 16) > src.width * 0.02:
        print(f"!! photo_mask.py: the photo is {src.width}x{src.height}, not 16:9 like the TV: crop it to 16:9 first (the TV fills the screen with it)")
        return 1
    sky = tool.cut(photo)
    if sky is None:
        print("no clean sky edge in this photo: no mask written (an AI image tool can make one: docs/OWN-PHOTO-MASK.md)")
        return 1
    front = Image.fromarray((~sky).astype("uint8") * 255).resize(src.size, Image.NEAREST)   # white: the scenery, in front of the clock
    front.convert("1").save(out, optimize=True)
    print(f"✓ {out.name}: {src.width}x{src.height}, {out.stat().st_size // 1024} KB; send it with the photo from the phone page: «صوري» ← «صورة مع قناعها»")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
