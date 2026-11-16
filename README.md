# Cut-outs for the glass clock (4.4)

`masks/<set>/<name>-fg.webp` and `masks/<set>/index.json`, made by `.github/tools/photo_masks.py` on work/433-design: a depth model
(Depth-Anything-V2-Small, Apache-2.0) and a soft matte (a guided filter at twice the size, the edge in the scenery's own colour: no fringe, no
stair steps), then the gate on every photo. `masks/gate.json` says for each photo FIT / FRONT-ONLY / REJECT, why, its scores (halo, jagged,
islands, holes, the digits' zoom, visible share and crossing) and whether it is published; only FIT ones also checked by eye
(`.github/tools/photo_masks_ok.txt`) have a cut-out here, every other photo keeps the glass clock in front.
Copy `masks/` as is into urtv-data's data branch (it replaces the older cut-outs); the launcher reads
`https://raw.githubusercontent.com/tshi-dev/urtv-data/data/masks/` (jsDelivr as fallback) and checks each file's sha256 against its set's index.
