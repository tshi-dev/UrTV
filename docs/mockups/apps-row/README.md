# «شكل التطبيقات»: the three ways as built (4.4)

Renders of the row as `work/44-cards-build` draws it (FavBar's numbers at 1920×1080), centred in every way:

- `apps-banners.png`: A «بنرات». Each app's TV banner, 256×144, radius 16, 24 apart, no tray. Six fit whole; more scroll with the focus.
  The focused one is bigger with a shadow («تكبير وظل»), its name under the row. An app with no banner shows its icon on its own colour.
- `apps-icons.png`: B «أيقونات». Each app's icon in a 112 rounded square (radius 24), 40 apart, no tray; the same focus and name. «شكل كنبة» picks it.
- `apps-tiles.png`: C «مربعات ملونة». Today's tray of 304×170 tiles, its ring (or «تكبير وظل» where it was chosen); now centred when there are fewer.

The colours and shapes are neutral stand-ins, not any app's logo. On the TV each installed app's own banner or icon is read at runtime
(PackageManager), cached in memory and sized for the screen; nothing is bundled.
