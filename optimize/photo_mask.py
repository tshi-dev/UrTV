#!/usr/bin/env python3
"""4.4 «الساعة: زجاجية خلف الصورة» on the person's own photo: makes its mask on THIS computer, with the same offline tool CI runs on our
photos (.github/tools/photo_masks.py). Nothing is sent anywhere: the photo is read here and one PNG is written beside it.

    python3 optimize/photo_mask.py PHOTO.jpg [MASK.png]

MASK.png (default: PHOTO-mask.png): the photo's size, white = what stands in front of the clock (buildings, mountains, people, trees),
black = the sky, a soft grey edge between (8-bit). Send the two from the phone page: «صوري» ← «صورة مع قناعها» (docs/OWN-PHOTO-MASK.md).
It runs the same gate as our photos: a mask with a fringe or a ragged edge, or one that cuts through a face, is not written (it says why);
a clean one the clock would not sit behind well is written with a note. The depth model (about 100 MB, from GitHub) is downloaded once
into mask-venv/. Needs numpy, scipy, opencv-python-headless<5, Pillow and onnxruntime (not in optimize/requirements.txt: a separate venv):
    python3 -m venv mask-venv && mask-venv/bin/pip install numpy scipy "opencv-python-headless<5" Pillow onnxruntime
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
        print(f"!! photo_mask.py needs {e.name}: python3 -m venv mask-venv && mask-venv/bin/pip install numpy scipy \"opencv-python-headless<5\" Pillow onnxruntime,"
              " then run it with mask-venv/bin/python")
        return 1
    src = Image.open(photo)
    if abs(src.width * 9 - src.height * 16) > src.width * 0.02:
        print(f"!! photo_mask.py: the photo is {src.width}x{src.height}, not 16:9 like the TV: crop it to 16:9 first (the TV fills the screen with it)")
        return 1
    work = Path(sys.prefix) if sys.prefix != sys.base_prefix else Path.cwd()   # the model is kept in the venv (else here)
    rgb = tool.photo(photo)
    alpha = tool.matte(rgb, work)
    verdict, why, scores, _ = tool.judge(rgb, alpha, tool.scenery_colour(rgb, alpha))
    if verdict == "REJECT":
        print(f"no mask written: {why} (an AI image tool can make one: docs/OWN-PHOTO-MASK.md)")
        return 1
    mask = Image.fromarray((alpha * 255 + 0.5).astype("uint8"), "L").resize(src.size, Image.BILINEAR)   # white: the scenery, in front of the clock
    mask.save(out, optimize=True)
    note = "" if verdict == "FIT" else f" (note: {why}; the TV may show the clock in front)"
    print(f"✓ {out.name}: {src.width}x{src.height}, {out.stat().st_size // 1024} KB{note}; send it with the photo from the phone page: «صوري» ← «صورة مع قناعها»")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
