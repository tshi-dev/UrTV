# Live backgrounds on weak TVs: the plan (4.3.3 scene home, proposal)

**بالعربي باختصار:** المشهد صورة ثابتة واحدة على كل التلفزيونات، والحركة إضافة صغيرة تُطفأ وحدها. في المستوى «ضعيف» (خفيف) يظهر المشهد ثابتًا
بلا حركة ولا طبقة عمق، وبحجم ١٢٨٠، والساعة الزجاجية تُرسم مرة في الدقيقة. لا ضبابية حيّة في أي مستوى: الزجاج صورة محسوبة مسبقًا.

Nothing here is built yet. The numbers are budgets for the build, checked by tests then.

## What a scene is (files, per scene)

| File | What | 1080 budget | 4K (`@2x`) |
|---|---|---|---|
| `scene-<id>.jpg` | the photo, 1920x1080, JPEG q 82, progressive off | ≤ 320 KB (a detailed photo, AlUla, needed more than 220 KB) | ≤ 1.1 MB, NORMAL tier on a 4K panel only |
| `scene-<id>-glass.jpg` | a blurred, brightened copy: the clock's glass fill and the cards' glass | ≤ 80 KB (it is a blur: 960x540 scaled up is enough) | none |
| `scene-<id>-fg.webp` | the subject mask: everything in front of the sky, the sky transparent, drawn over the clock | ≤ 200 KB, lossy WebP with alpha | ≤ 700 KB |
| `scenes.tsv` (one line a scene) | the digits' top and bottom, the mask's bounding box, dark or light clock, the credit | < 1 KB | |

Each pack of five scenes at 1080 is about 2 to 2.5 MB. «مشاهد السعودية» ships one scene, Riyadh, in the launcher APK (0.52 MB; its four others, 1.9 MB, are downloaded the first time the scenes show, 4.4); «مشاهد Couch TV» (1.9 MB) is downloaded once
when chosen, from urtv-data's scenes/ (index.json gives its sha256 and size, checked before the zip is opened); the 4K copies
are fetched on first use by a 4K NORMAL TV only and cached, never bundled.

The subject masks are made once, offline, never on the TV (no segmentation model in the app). Only the pixels of the photo are kept;
the mask's bounding box lets the TV draw only that rectangle instead of a full screen of alpha.

## Drawing (native launcher, one frame)

1. Photo: one `Bitmap`, decoded at the screen's size (`inSampleSize`, `RGB_565` on LOW and MID: half the memory, no alpha needed).
2. Clock: a custom view drawing the time as text with a `BitmapShader` of the glass copy (the same trick as couch-tv's CSS
   `background-clip: text`), brightened, plus a light 2 px edge, a soft dark shadow so it reads on any scene, and a sheen gradient. It redraws once a minute (once a second only if «إظهار الثواني» is on,
   and then only the seconds' box is invalidated).
3. Subject mask: one `ARGB_8888` bitmap, cropped to its bounding box, drawn over the clock.
4. Cards and the app row: glass panels filled with the matching crop of the glass copy plus a translucent tint. **No live blur**
   (`RenderEffect` is Android 12+ only and costs a full-screen pass per frame on these GPUs).

Memory at 1080: photo 4 MB (565) or 8 MB, glass 1 MB (half size), mask about 3 MB (cropped): about 8 to 12 MB for the scene on
screen. Only the scene showing and the next one are decoded; the rest are files. LOW keeps only the one showing.

## Motion («المشاهد الحيّة»), and its limits

Motion is an extra, never what makes the home work:

| Kind | Limit | Tier |
|---|---|---|
| Crossfade between scenes | 1.6 s, every «بقاء كل مشهد» (default 20 s, at least 10 s) | MID and NORMAL; LOW cuts without a fade and changes at most every 5 minutes |
| Slow drift (Ken Burns), photo only | ≤ 2% scale over the whole stay, the mask moves with it | NORMAL only |
| A scene's own clip (water, clouds) | H.264 1080p, ≤ 12 s loop, ≤ 6 Mbps, ≤ 8 MB, played by the hardware decoder in a `TextureView` under the mask; no animated WebP for full-screen motion (it is decoded on the CPU, frame by frame) | NORMAL only, and only while the home is in front and the TV is not playing anything |
| Animated WebP | only for small things (≤ 400x400, ≤ 24 frames, ≤ 300 KB), never full screen | MID and NORMAL |
| Weather over the scene («المشهد يتبع الطقس»: rain, fog drift) | ≤ 60 particles, 30 fps, stops when the home is behind another app | NORMAL only, off by default |

Every animation stops when the home is not in front (`onStop`), when the screensaver starts, and when an app is opening.

## The «خفيف» profile (LOW tier) and the static fallback

The memory tiers already exist (`dock.sh ramtier`, docs/SETTINGS.md): LOW is Android's low-RAM flag or under 2000 MB. On LOW the
scene home is the static fallback, by itself, with nothing to set:

- the photo at 1280x720 (`inSampleSize` 2 on the 1080 file), `RGB_565`;
- **no subject mask**: the clock is drawn over the scenery (no depth, no extra layer, no memory for it); the glass fill stays (it is a
  shader, not a layer);
- no crossfade, no drift, no clip, no weather; the scene changes at most every 5 minutes;
- the cards are a flat translucent tint, no glass crop;
- the clock redraws once a minute, seconds forced off on the home.

MID: everything except the clips, the drift and the weather. NORMAL: everything. `optimize/dock.sh ramtier low` shows the fallback
on any TV for a test.

## Guards the build adds (tests, then a field check)

- tests/scene_home_test.py fails the build if a scene file is over its budget above, a file is missing or extra, a cut-out does not match its box,
  or a tier's rules change (clips: none yet; when they come, over 8 MB or 12 s fails too).
- The launcher's start time with the scene home on LOW stays within 10% of today's classic home (measured on the weakest TV we
  have), and the home's memory after a minute stays under today's plus 15 MB on NORMAL, plus 6 MB on LOW.
- If decoding a scene fails or memory is short (`onTrimMemory` at `RUNNING_LOW`), the home drops to the static photo, then to the
  classic home's background, and never shows a blank screen.
- The saver page (`tv.html`, budget 746,000 bytes) is not touched: the scenes live in the launcher only.
