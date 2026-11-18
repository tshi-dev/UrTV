# «شكل شاشة الإعدادات» (4.4 beta 2): where the row sits, for the owner's OK

| Picture | What it shows |
|---|---|
| [01-buttons-look-page.png](01-buttons-look-page.png) | The buttons screen (4.3.2, the default): «المظهر والخلفيات», the new row right after «أشكال جاهزة», value «أزرار» |
| [02-couch-look-section.png](02-couch-look-section.png) | The «كنبة» side list: «المظهر», the same row in the same place, value «كنبة»; so the person can always go back to the buttons |
| [03-couch-choice-page.png](03-couch-choice-page.png) | Its choices: «أزرار» (4.3's big sections) and «كنبة» (side list, compact rows); choosing opens the settings again in the other style with the focus on this row. «tvOS» is added here only once its screen is approved and built |

Rules: only a full-design look («كنبة»; «tvOS» once built) changes the settings screen too, and «شكلك المعتاد» puts the person's own back; the other looks never do; after the update each TV keeps the screen it shows now
(«كنبة» if the look «كنبة» was chosen, else «أزرار»); `optimize/saver.sh settingsstyle buttons|couch|tvos`; search finds it by «شكل الإعدادات», «القائمة», «الأزرار».

## «كنبة» v2, the first page (the owner's request, for his OK before building)

| Picture | What it shows |
|---|---|
| [couch-v2-first-page.png](couch-v2-first-page.png) | One tall bar per section (92 px instead of 76). The focused bar grows: the section's name large, and under it a short plain line of what is inside. The others show the name only. No icons, no marks, no pills. |
| [couch-v2-720-large.png](couch-v2-720-large.png) | 720p with the TV's large font: the same, the list scrolls, a long line wraps instead of being cut. |
| [couch-v2-light.png](couch-v2-light.png) | A weak TV (automatic): a flat fill instead of glass, no shadow, no growth, no motion. |

Built, it stays CouchStyle (the sizes in rem, the warm dark veil), the lines are each section's own text in SettingsHub.CATEGORIES (shortened),
measured once and set, nothing new on the UI thread; the clock and the home are untouched. HTML in `src/couch-v2-*.html`, `node src/render-v2.js`.

## The settings screen in the «tvOS» manner (4.4 beta 2, mockups only)

Pictures for the owner's approval; nothing is built. The manner is tvOS Launcher's (github.com/techgenius1/tvOS-Launcher, Apache-2.0), named
«tvOS» in the app as the owner decided. No image, logo or code of it is used here: the icons are UrTV's own line icons, the photo is one of
UrTV's backgrounds (assets/backgrounds), the font a stand-in for Thmanyah.

1. `tvos-settings-main.png`: the first screen. One list of large glass rows (the section's icon, its name, its value, ‹), the focused row
   white with dark words and a little bigger; the focused section's icon large on the other side with its one line under it.
2. `tvos-settings-look.png`: inside «المظهر»: the groups as small headings, each row with its value; the line under the icon explains the
   focused row.
3. `tvos-settings-picker.png`: choosing a value: a short list in the middle, the chosen one with a ✓.
4. `tvos-settings-720-large.png`: 720p with the TV's large font: rows grow, no word is cut, the list moves with the focus.
5. `tvos-settings-light.png`: the light version for a weak TV: the photo blurred once (as GlassSource does), flat fills, no shadows, no growth.

Rules kept: Arabic first and right to left, rows are rectangles with rounded corners (no capsules), no «·». The rows, sections and values
are today's (SettingsHub); only their look changes. «شكل شاشة الإعدادات» (the UI session's row) would offer it as «tvOS».
