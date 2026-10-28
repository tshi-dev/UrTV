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
