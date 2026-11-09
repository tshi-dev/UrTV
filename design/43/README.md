# 4.3 design round: mockups (no app code)

1920×1080 pictures of the TV screen, Arabic, RTL. Fonts: the real Thmanyah files are private, so these use **DejaVu Sans** (stand-in; letter shapes differ, widths are close). The wallpaper is the home's own photo. Regenerate: `python3 design/43/gen.py /tmp/h && node design/43/render.js /tmp/h design/43`.

Rules shown: no capsules (rounded rectangles of 12–24 px, circles only for dots), no small label above a title, 8-scale spacing with 16 px minimum inside a box, TV safe area (x 96–1824, y 54–1026), the D-pad focus state on one item of every screen.

| # | File | What it shows |
|---|------|---------------|
| 1 | 01-quick-buttons-rest, 02-quick-buttons-focus | the top-left buttons as one bar of five 56 px squares, top-aligned with the greeting at y 54; focus = solid fill + its name below |
| 2 | 03-appearance-main, 04-appearance-looks-open | one «المظهر» page: six category tiles each with a live preview; then «الشكل» open (8 looks), the accent row and light/dark/auto under it |
| 3 | 05-new-looks-grid, 06-accents-and-controls | 4 new looks (سينمائي، أفق، شفق، بسيط) beside our 4 (the current glass look stays the default); the 8 accent colours; bar opacity, wallpaper dim, focus style |
| 4 | 07-home-graphite, 08-home-sand, 09-home-horizon | cards and apps bar from the same tokens (panel, line, tile, focus) in three looks |
| 5 | 10-setup-plan, 11-setup-progress, 12-setup-done | the installer: per-app ticks, progress, done |
| 6 | 13-suggested-apps | «تطبيقات مقترحة»: tile, one line, install / installed |
| 7 | 14-home-banner | «خلّ تلفزيونك يفتح على UrTV» with «لاحقاً», as built |

## From Clean Your TV (MIT, already credited)
Taken: more named looks (its cinematic, minimal, aurora and ambient-glow/horizon skins → سينمائي، بسيط، شفق، أفق); **dock opacity 30–100** (شفافية الشريط); **wallpaper dim 0–90** (تعتيم الخلفية); **focus style** (its glass/rim/halo → ممتلئ، إطار، توهّج); apps-per-row 4–7 (we have 5–8, kept as «الحجم والكثافة»).
Not in the pictures, worth a later round: wallpaper rotation minutes and shuffle/sequential, wallpaper by time of day (morning/evening/night), reduce-motion switch, rows below the dock.
Left out: festival skins, Shamsi/lunar dates, stats overlay, an accent that only works with skins (ours is global, 8 colours).

## Round 2
Glass stays the default. Genuinely different looks (own card shape, bar, clock and focus), not recolours: هدوء (type only), نقش (sand and wood, star lattice, small corners, gold frame), سينما (big posters, no bar), باستيل (soft light, round cards, lifted focus), حاد (black and yellow, thick frames, inverted focus), ليل (dim amber only, for late hours), plus أفق.
| File | What |
|---|---|
| 05-new-looks-grid | glass (default) + 7 looks, each tile a live mini of its own home |
| 15 / 16 / 17 | full homes: نقش، سينما، باستيل |
| 18 / 19 | settings «اجعل UrTV الشاشة الرئيسية»: not the Home (opens the setup) and the Home («✓ UrTV هو الشاشة الرئيسية») |
| 20 / 21 / 22 | «ألعاب العائلة»: the hub (سؤال وجواب، ذاكرة الأزواج، خمّن الكلمة), a quiz screen with four teams, and the home card + bar tile |

## Round 3: «مركز التحكم» as a compact side panel (23–26)
A 560 px panel in the TV's glass, flush with the safe area on the start edge (right in Arabic), full height; the home stays visible. A row of four round toggles (Bluetooth, screensaver now, sleep timer, power off), then five rows: icon, one short label, the current value as a small word («الشبكة  ATS5G»), no sentences. «تخصيص» at the top opens a list with a tick per item, arrows to reorder and «٥ من ٦» (default at most six). Focus is the solid accent with the look's ink (readable), 150–200 ms ease-out; the panel slides in 220 ms.
23 rest · 24 focused row · 25 customise · 26 light look. Source: `design/43b/src/control.js`.

## Phone page: «على الشاشة» (mockup only, awaiting approval)
`phone-{ar,en}-{dark,light}.png`: before (left in the picture's reading order: «قبل») and after, 360 px wide. The text message, «ارسم» and «صوري» become one card «على الشاشة» (message field + send, then two 56 px rows with icon, label and a short line); the bare links «محفظتي», the light/dark and language switches become real buttons under «إعدادات سريعة». CSS is read from the app's own phone page; the real fonts are private, so the pictures use stand-in fonts. Source: `design/43/phone-src/` (`python3 gen.py /tmp/ph`, then `node render.js /tmp/ph design/43`).

## «ألعاب العائلة» in our style (mockups only)
`games-01…27-*.png`, source `games.py` (`python3 design/43/games.py /tmp/hg && node design/43/render.js /tmp/hg design/43`). Home's glass panels, tray tiles, solid-fill focus; no phones by default (one remote, pass-and-play, «غطّوا عيونكم» hand-over screens), phones as the second choice («بالجوالات»). 01 hub (7 games + the mode card), 02 quick pick; per game 3 screens: name-animal-plant 03-05, «واحد مو معنا» 06-09, «بدون كلام» 10-12, «الكلمة المفقودة» 13-15, hex letters 16-18, dominoes 19-21, «خمّن الكلمة» 22-24; 25 hub light, 26 hub English, 27 motion timings. Real fonts are private (stand-in used). Game specs were not on work/games-data yet (STUDY.md only): rules shown follow the brief and will need a pass against each SPEC.md.
