# scenes/ (for tshi-dev/urtv-data, branch data)

UrTV 4.4: the scene packs the launcher downloads once (SceneDownload) the first time the scenes show: `couch` («مشاهد Couch TV») and `saudi`
(the four Saudi scenes not in the app: AlUla, the dunes, the Red Sea, Hisma; Riyadh is in the app). `<pack>.json` gives `<pack>.zip`'s sha256
and size; the app checks both before it opens the zip. The zip holds only `scenes.tsv`, `CREDITS.txt`
and each scene's `<id>.jpg`, `<id>-glass.jpg`, `<id>-fg.webp` (any other name and the app keeps nothing). Change the zip and the index together.
Credits: couch-tv's scene backgrounds (https://github.com/Seagey/couch-tv), made with an AI image model, MIT License, Copyright (c) 2026 Seagey.
