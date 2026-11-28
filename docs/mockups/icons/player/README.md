# مشغّل تلفزيونك: أيقونة العائلة (للنسخ إلى مستودع المشغّل)

Made by `.github/tools/icons/make.js`. Copy `res/` into the player's `src/main/res/` and set `android:icon="@mipmap/ic_launcher"`.
- `mipmap-anydpi-v26/ic_launcher.xml` with `drawable-v26/ic_launcher_{bg,fg,mono}.xml`: the adaptive icon. The gradients need API 24.
- `mipmap-*/ic_launcher.png`: the flattened icon for older TVs.
- `drawable-{xhdpi,xxhdpi,xxxhdpi}/banner.png`: «مشغّل تلفزيونك» in Thmanyah, added by `.github/tools/icons/banners.js` where the fonts are.
  Set `android:banner="@drawable/banner"`.
