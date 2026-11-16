# scenes/ (for tshi-dev/urtv-data, branch data)

UrTV 4.4: the scene packs the launcher downloads once (SceneDownload) the first time the scenes show: `couch` («مشاهد Couch TV») and `saudi`
(the four Saudi scenes not in the app: AlUla, the dunes, the Red Sea, Hisma; Riyadh is in the app). `<pack>.json` gives `<pack>.zip`'s sha256
and size; the app checks both before it opens the zip. The zip holds only `scenes.tsv`, `CREDITS.txt`
and each scene's `<id>.jpg`, `<id>-glass.jpg`, `<id>-fg.webp` (any other name and the app keeps nothing). Change the zip and the index together.
Credits: couch-tv's scene backgrounds (https://github.com/Seagey/couch-tv), made with an AI image model, MIT License, Copyright (c) 2026 Seagey.

v4 (4.4): the cut-outs made again with the depth model, the soft matte and the gate (work/433-design .github/tools/photo_masks.py --scenes);
Hisma has none now (only a sliver of the digits crossed: the clock in front) and the waterline keeps none. The launcher keeps a pack in
scenes/<pack>.2 (SceneDownload.DATA), so a TV that has v3 takes v4 once.

v4, the owner's rule (2026-11-16): each scene's clock place in digits_top/digits_bottom and a 10th column digits_x (the digits' centre, px of 1920; absent = 960), settled by the gate; golden, Riyadh and the Red Sea have no cut-out (the clock in front).
