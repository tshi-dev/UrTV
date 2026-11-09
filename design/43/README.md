# 4.3 design round: mockups (no app code)

1920×1080 pictures of the TV screen, Arabic, RTL. Fonts: the real Thmanyah files are private, so these use **DejaVu Sans** (stand-in; letter shapes differ, widths are close). The wallpaper is the home's own photo. Regenerate: `python3 design/43/gen.py /tmp/h && node design/43/render.js /tmp/h design/43`.

Rules shown: no capsules (rounded rectangles of 12–24 px, circles only for dots), no small label above a title, 8-scale spacing with 16 px minimum inside a box, TV safe area (x 96–1824, y 54–1026), the D-pad focus state on one item of every screen.

| # | File | What it shows |
|---|------|---------------|
| 1 | 01-quick-buttons-rest, 02-quick-buttons-focus | the top-left buttons as one bar of six 72 px squares, top-aligned with the greeting at y 54; focus = solid fill + its name below |
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
