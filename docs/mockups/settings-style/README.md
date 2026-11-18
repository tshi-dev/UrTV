# «شكل شاشة الإعدادات» (4.4 beta 2): where the row sits, for the owner's OK

| Picture | What it shows |
|---|---|
| [01-buttons-look-page.png](01-buttons-look-page.png) | The buttons screen (4.3.2, the default): «المظهر والخلفيات», the new row right after «أشكال جاهزة», value «أزرار» |
| [02-couch-look-section.png](02-couch-look-section.png) | The «كنبة» side list: «المظهر», the same row in the same place, value «كنبة»; so the person can always go back to the buttons |
| [03-couch-choice-page.png](03-couch-choice-page.png) | Its choices: «أزرار» (4.3's big sections) and «كنبة» (side list, compact rows); choosing opens the settings again in the other style with the focus on this row. «tvOS» is added here only once its screen is approved and built |

Rules: the looks (أشكال جاهزة) no longer change the settings screen; after the update each TV keeps the screen it shows now
(«كنبة» if the look «كنبة» was chosen, else «أزرار»); `optimize/saver.sh settingsstyle buttons|couch`; search finds it by «شكل الإعدادات», «القائمة», «الأزرار».
