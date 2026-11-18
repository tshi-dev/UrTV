# Design backlog

Looks and ideas the owner chose to do later. Each says what it is, where its mockups are, and what it needs.

## Themes: done in 3.148

The three looks below are now «المظهر ← الشكل» (GlassStyle.GRAPHITE_*, NAVY_*, CLEAN_LIGHT; tests/glass_test.py checks each over
every photo). Their first mockups stay here for the record.

The glass look (د) is the default of every UrTV screen (`GlassStyle`, `GlassPaint` and `GlassSource` in both apps; `tests/glass_test.py`).
Mockups at 1920x1080, drawn as pages, not taken on a TV: the settings' first screen,
«الموقع والصلاة» with a row focused, and the «طريقة الحساب» picker.

| Look | Mockups | Dark, light and auto |
|---|---|---|
| أ · فحمي مع لمسة كهرمان (graphite, amber accent, a ring on the focused item) | `docs/screenshots/themes/graphite-{main,sub,dialog}.jpg` | dark as drawn; light: the same layout on #F3F1EE with ink #1C1B1A, the amber kept; auto follows the home |
| ب · كحلي الليل مع الذهبي (deep navy with gold and a faint starfield, the home's night sky) | `docs/screenshots/themes/navy-{main,sub,dialog}.jpg` | dark as drawn; light: «dawn», #EEF1F7 panels, navy ink, the gold kept; auto follows the home |
| ج · نظيف فاتح (white tiles on soft grey, a deep-teal solid focus) | `docs/screenshots/themes/clean-{main,sub,dialog}.jpg` | the light partner of «المظهر ← فاتح»; dark falls back to أ; auto switches with the home |

What a selectable look needs: a `GlassStyle.Look` of its own (both looks, dark and light) passing the same contrast test over every photo,
and a setting beside «المظهر». No live blur, as now.

## Split tv.html

Move the largest rarely-shown part (measure first: games / sports takeover / settings panels) into a lazy file; target back under 746 KB. 4.4 raised the tv.html budget in tests/check.py from 746 KB to 775 KB (tv.html was 760,217 bytes with every 4.4 branch in).

4.5 (work/45-pagesize, the owner: under 500 KB): the verse and hadith lists (397 KB, the largest part by far) are `dist/quotes.json` beside the page,
in both apps' assets (the screensaver's, the launcher's `web/`) and the TV ports, read once and at once (synchronous XHR of a local file) the first time
a verse or hadith is drawn; the page keeps the 20 shortest of each for a page that cannot read it (then the repository's, in the background).
`dist/urtv.html` keeps them inline. tv.html 770,352 → 381,560 bytes, tv-en.html 734,197 → 345,405; the budget in tests/check.py is 500 KB.
Left for later if needed: the team tables (TEAM_AR 18 KB, TEAM_BY_ID 12 KB) and a whitespace pass (about 13 KB).
