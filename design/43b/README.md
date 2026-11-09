# design/43b: round 2b mockups (no app code)

`RUBRIC.md` (the bar), `SCORES.md` (every screen against it, with the critique log), `src/` (HTML + JS: `index.html?s=<name>`; fonts are `'Thmanyah Sans'` and `'Thmanyah Serif Display'` with stand-in fallbacks), `photos/` (the app's own pack photos, `CREDITS.md`),
`preview/` (stand-in-font PNGs, 1920×1080), `studies/` (three directions per look). Render with real fonts: `node design/43b/render.js <out-dir>` (needs playwright and the fonts installed or an `@font-face` added to `src/index.html`).

Looks (each a full system: type, material, focus, colour logic, how the photo is treated): لوح · مجلس · سينما · إطار · يوم (sky dial, changes from Fajr to Isha) · نقاط · ألوان. Glass stays the default look.
Games: hub, quiz (question, reveal, score animation frames), memory pairs, Arabic word game.
