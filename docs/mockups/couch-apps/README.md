# The Couch look's apps row: three ways

A mockup only; nothing is built yet. Each is the Couch home with the clock at its one fixed place (digits 266 to 542, centred)
on golden Riyadh (`ksa-cities/1`), with the glass clock in front.

- `couch-apps-a-banners.png`: A. Each app's own TV banner, as Google TV draws it (16:9, 256×144 px at 1080, rounded 16 dp).
  The focused one is drawn bigger with a shadow («تكبير وظل»), and only it has its name under it.
- `couch-apps-b-icons.png`: B. Each app's own launcher icon in a rounded square, couch-tv style: smaller (112 px), with more
  space between them, and the name under the focused one.
- `couch-apps-c-tiles-today.png`: C. Today's coloured name tiles, for comparison.
- `couch-apps-sheet.jpg`: all three, one above the other.

The banners and icons here are neutral stand-ins (a colour and a plain shape), not any app's logo. No logo goes in the
repository. On the TV, the app draws each installed app's own banner (`PackageManager.getApplicationBanner`) or icon
(`getApplicationIcon`) at runtime. An app with no banner falls back to its icon on its own colour.
