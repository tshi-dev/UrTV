# Cut-outs for the glass clock (4.4)

`masks/<set>/<name>-fg.webp` and `masks/<set>/index.json`, made by `.github/tools/photo_masks.py` on work/433-design (only the photos in
`.github/tools/photo_masks_ok.txt`, checked by eye). Copy `masks/` as is into urtv-data's data branch; the launcher reads
`https://raw.githubusercontent.com/tshi-dev/urtv-data/data/masks/` (jsDelivr as fallback) and checks each file's sha256 against its set's index.
