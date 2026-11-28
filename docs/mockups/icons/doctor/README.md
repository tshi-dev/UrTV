# فحص تلفزيونك: أيقونة العائلة (للنسخ إلى مستودع التطبيق)

Made by `.github/tools/icons/make.js` (one model for the whole family). Copy `res/` into the app's `src/main/res/` and set
`android:icon="@mipmap/ic_launcher"`.
- `mipmap-anydpi-v26/ic_launcher.xml`: the adaptive icon (background, foreground, monochrome). Android 8 and later read it.
- `drawable-v26/`: its three vector layers. The gradients need API 24, so they are only in `-v26`.
- `mipmap-mdpi` … `mipmap-xxxhdpi/ic_launcher.png`: the flattened icon, which minSdk 21 up to Android 7 reads.

The banner (320×180, with the name «فحص تلفزيونك» in Thmanyah) comes separately: it needs the font.
