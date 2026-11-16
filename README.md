# Cut-outs for the glass clock (4.4)

`masks/<set>/<name>-fg.webp` and `masks/<set>/index.json`, made by `.github/tools/photo_masks.py` on work/433-design: a depth model
(Depth-Anything-V2-Small, Apache-2.0) and a soft matte (a guided filter at twice the size, the edge in the scenery's own colour: no fringe, no
stair steps), then the gate on every photo. `masks/gate.json` says for each photo FIT / FRONT-ONLY / REJECT, why, its scores (halo, jagged,
islands, holes, the digits' visible share and crossing, the frame) and whether it is published; only FIT ones also checked by eye
(`.github/tools/photo_masks_ok.txt`) have a cut-out here, every other photo keeps the glass clock in front.
Copy `masks/` as is into urtv-data's data branch (it replaces the older cut-outs); the launcher reads
`https://raw.githubusercontent.com/tshi-dev/urtv-data/data/masks/` (jsDelivr as fallback) and checks each file's sha256 against its set's index.

The clock has one fixed place (the owner): digits 266 to 542, 718 wide, centred, in the clean and Couch layouts. The photo
moves instead: an entry's `"frame": [x, y, w]` is the part of the photo shown full-screen (absent: the whole photo), and
`"depth"` lists the layouts that draw the cut-out behind the clock. A photo is never upscaled or stretched. Our photos are
1920 px, so each is shown whole. At that fixed place 3 of 145 pass:
- photos/anozjF6uqI0: clean, couch.
- photos/rkRYbmPuhOE: clean, couch.
- ksa-cities/1: clean only; it is the owner's own picture (`owner_ok`).
