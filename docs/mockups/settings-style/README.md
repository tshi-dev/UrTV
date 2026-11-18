# «شكل شاشة الإعدادات» (4.4 beta 2): where the row sits, for the owner's OK

| Picture | What it shows |
|---|---|
| [01-buttons-look-page.png](01-buttons-look-page.png) | The buttons screen (4.3.2, the default): «المظهر والخلفيات», the new row right after «أشكال جاهزة», value «أزرار» |
| [02-couch-look-section.png](02-couch-look-section.png) | The «كنبة» side list: «المظهر», the same row in the same place, value «كنبة»; so the person can always go back to the buttons |
| [03-couch-choice-page.png](03-couch-choice-page.png) | Its choices: «أزرار» (4.3's big sections) and «كنبة» (side list, compact rows); choosing opens the settings again in the other style with the focus on this row. «tvOS» is added here only once its screen is approved and built |

Rules: the looks (أشكال جاهزة) no longer change the settings screen; after the update each TV keeps the screen it shows now
(«كنبة» if the look «كنبة» was chosen, else «أزرار»); `optimize/saver.sh settingsstyle buttons|couch`; search finds it by «شكل الإعدادات», «القائمة», «الأزرار».

# The settings screen in the «tvOS» manner (4.4 beta 2, mockups only)

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
