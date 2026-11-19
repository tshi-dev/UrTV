# Third-party notices

Code and ideas taken from other projects, with their licenses. Fonts, photos and lists carry their own notices next to them:
`assets/fonts/*-NOTICE.txt` and `*-OFL.txt`, `docs/PHOTO-CREDITS.md`, `optimize/lists/community/LICENSE-CleanYourTV.txt`, and the
launcher's origin in `android/launcher/LICENSE-tv-home`.

## التفسير الميسر

«التفسير الميسر» under the verse of the day (only with «التفسير الميسر» on, `#tafsir=1`): نخبة من العلماء، مجمع الملك فهد لطباعة المصحف الشريف
(King Fahd Glorious Quran Printing Complex), fetched by `.github/tools/fetch_verses.py` from api.alquran.cloud (edition `ar.muyassar`) into
`assets/shared/verses.json`, and shipped as `dist/tafsir.json`. The text is shown whole and unchanged (never shortened, nothing added),
with its name above it; QuranEnc.com publishes the same tafsir on these terms (re-publication allowed with no modification, addition or
deletion, the publisher and source named).

## TVmaze

«جديد المسلسلات» (the series card, and the settings' series search) reads the TVmaze API (api.tvmaze.com), whose data is licensed under
CC BY-SA 4.0 (https://www.tvmaze.com/api). The card credits it with a tiny «TVmaze» at the end of its header line, and the settings' «مصادر
البيانات» (حول) names the licence with its link (tvmaze.com).

## couch-tv

The 4.3.3 scene home proposal (`docs/mockups/`) follows couch-tv's design (https://github.com/Seagey/couch-tv: scenes with a glass
clock behind the scenery, settings with side sections), and its mockups use couch-tv's five scene photos, their cut-outs and glass
copies (`tv-home/web/backgrounds/scene-*`, made for couch-tv with an AI image model, "use them however you like"), under the MIT
License, Copyright (c) 2026 Seagey, the same text as below. With the look «منظر وساعة» the settings follow couch-tv's settings screen
(`tv-home/web/app.css`, «Home settings»: its sizes, colours, kinds of row and motion; `android/app/src/main/java/sa/urtv/screensaver/CouchStyle.java`;
no code of it is used), and the scene packs' rows show a thumbnail of one of couch-tv's scene photos (`assets/scene-thumbs/couch.jpg`), under the
same license.

## talkies

The home screen's spring motion (`android/launcher/src/main/java/sa/urtv/home/Spring.java`) and the ideas for the channel and
«أكمل المشاهدة» rows and the automatic light and dark look are from talkies (https://github.com/rajupp/talkies), under this license:

```
MIT License

Copyright (c) 2026 TechPP

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

### What else of talkies is in UrTV, and what it follows

- the **Control Center** (the round pill and its panel: sleep timer, turn off now, network, speed test, sound output, screensaver,
  settings) is adapted from talkies' Control Center (`Sleep.java`, `SleepService.java` and the Control Center code of `HomeActivity.java`), which
  is itself adapted from tv-vibehack's launcher (https://github.com/samdegan2311/tv-vibehack, branch `pr/control-center`, MIT, © 2026 Gathin);
- the **speed test** (`SpeedTest`) is adapted from talkies' `SpeedTest.java`, which is adapted from Matinee (https://github.com/Jarvis322/matinee,
  `system/SpeedTest.kt`, MIT, © 2026 Yiğit Can Polat);
- the **HDMI-CEC device names and standby state** in «المصادر» and the **Play Store update bell** follow talkies' approach, which follows Matinee's.

What UrTV added or changed is UrTV's own (see LICENSE). The licenses of tv-vibehack and Matinee follow (talkies' own is above).

## tv-vibehack

```
MIT License

Copyright (c) 2026 Gathin

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

---

The Outfit typeface in launcher/app/src/main/res/font/outfit.ttf is copyright
The Outfit Project Authors and licensed under the SIL Open Font License 1.1,
see launcher/FONT-LICENSE-OFL.txt.
```

## Matinee

```
MIT License

Copyright (c) 2026 Yiğit Can Polat

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Free My TV

The home screen's setup («إعداد تلفزيوني», `android/launcher/src/main/java/sa/urtv/home/setup/AdbClient.java` and `AdbKeys.java`) is adapted from Free My TV's
`app/src/io/freemytv/Adb.java`: the way an app talks to the TV's own adb daemon at 127.0.0.1:5555 (the RSA key, the "Allow USB debugging?" prompt, the
Android 8 to 10 handling of the answer). Both files keep their credit header. The idea of running the setup from the TV itself follows Free My TV's
`HomeMode.java`; the commands and lists are UrTV's own (the ones `optimize/` runs from a computer).

The LG webOS and Samsung Tizen packaging in `ports/webos/` and `ports/tizen/` (`build.py`, `install.py`, `appinfo.json` and `config.xml` as a
starting point) is also adapted from Free My TV: how to pack an `.ipk` and a `.wgt` with Python only, how to install over Developer
Mode (LG's Key Server and SSH, Samsung's SDB), and which key codes and manifest entries a TV app needs. Each adapted file says so in
its header.

«أزرار الريموت» (`android/launcher/src/main/java/sa/urtv/home/RemoteButtons.java`, and its copy in the screensaver app) takes Free My TV's
per-brand remote table from `RemoteKeys.java`, `Prefs.java` and `SabrinaRemote.java` (release 1.5.0): which TVs (by maker, brand,
model and Android version), which key and scan codes each app button sends, the onn and Chromecast remote checks, Xiaomi's burst
and TCL Android 11's filter rule. Only the app buttons are taken. The file says so in its header; the licence is the one below.

talkies has `Spring.java` from Free My TV (https://github.com/simbasjungles/free-my-tv, `app/src/io/freemytv/Spring.java`), under
this license:

```
MIT License

Copyright (c) 2026 Free My TV contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Clean Your TV (remote keys, protected packages, the input guard, the community lists)

The home also answers TCL's vendor remote keys (4062, 4065, 4099) from the idea in Clean Your TV commit e525f084 (vendor keys of 1000 and up
reach only the window in front, so the home must handle them), and `com.android.sdksandbox` / `com.android.location.fused` are never disabled
after its commits bf505124 and f5c37c36. The TCL Guard key (4140), `com.tcl.xian.StartandroidService` (TCL's firmware updater) and Xiaomi's
`com.xiaomi.android.tvsetup.partnercustomizer` are from its TCL G03 and Xiaomi notes (up to commit 3cd274f8). The input guard of
`optimize/disable.sh` and `InputGuard.java` (a package that asks for TV_INPUT_HARDWARE or serves a TV input is never disabled, fail closed)
follows its `tools/disable.sh` (commits b8fcbabe and 66e83196, issue #321); the code is ours. The lists in `optimize/lists/community/` are its
per-model lists, packages only, re-taken at commit 3cd274f8 (2 Nov 2026). Clean Your TV (https://github.com/Gathin23/cleanyourtv) is MIT licensed; its license text is in
`optimize/lists/community/LICENSE-CleanYourTV.txt`.

## Open Shelf (suggested app, 4.3.1)

«تطبيقات مقترحة» offers Open Shelf (https://github.com/wingedpanther/openshelf, GPL-3.0; its font under the SIL Open Font License 1.1) as one entry. We only link to it: the settings download its own release file
(`OpenShelf-<version>.apk` from that repository's releases), check that it is that package, and hand it to Android's installer, which asks the person first. Nothing of Open Shelf is bundled in our apps, and we
have not vetted the apps it installs (the line on its tile says so).

## GeoNames and Wikidata (the offline city search)

`android/app/src/main/assets/cities-more.txt` (the places the settings' city search and `saver.sh city` find without the internet, built by
`.github/tools/cities-more/build.py`) and the world part of the built-in list (`Cities.java`) are made from:

- **GeoNames** (https://www.geonames.org/), cities500, through the `geonamescache` package: names, coordinates, populations and time zones,
  under the Creative Commons Attribution 4.0 License (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/). Changes: places chosen by
  country and population, places already in the built-in list left out, coordinates rounded to four decimals.
- **Wikidata** (https://www.wikidata.org/): the Arabic names and other Arabic spellings of those GeoNames places (by their GeoNames id, P1566)
  and the seats of the Saudi governorates, under CC0 1.0 (public domain dedication). A snapshot is kept in `.github/tools/cities-more/sources/`.

## The kick-off whistle (4.4, not third-party)

`android/launcher/src/main/res/raw/kickoff_whistle.ogg` (1.2 s, about 10 KB) is not taken from anywhere: it is synthesised by
`.github/tools/make_whistle.py` (a pea-whistle tone with its trill and a short release, numpy and ffmpeg) and dedicated to the
public domain under CC0 1.0. Run the script again to make the same file.

## The iqama chime (4.4, not third-party)

`android/launcher/src/main/res/raw/iqama_chime.ogg` (2.4 s, about 9 KB) is not taken from anywhere: it is synthesised by
`.github/tools/make_iqama_chime.py` (a soft two-note bell, G5 then E5, numpy and ffmpeg) and dedicated to the public domain under CC0 1.0.
Run the script again to make the same file.

## Wireless-debugging pairing (4.4 beta 3, in «إضافات تلفزيونك» only): libadb-android, spake2-android, Conscrypt, BouncyCastle

For Google TV and Chromecast sets that offer only «تصحيح الأخطاء اللاسلكي» (no adb on port 5555), «إعداد تلفزيوني» can pair the TV by wireless debugging, with the six digits typed on
the phone page. These libraries are in the ADD-ON only (`sa.urtv.extras`, `android/extras/build.gradle`), never in the home or the screensaver; the home reaches them only through
the add-on's signature-guarded provider. Every library file is pinned by SHA-256 in `android/extras/pins.txt` and checked before each build. The whole feature in the add-on is the package
`android/extras/src/main/java/sa/urtv/home/pair/`; take it, its provider calls and the four `implementation` lines out together and the add-on builds and works as before.

- **libadb-android** 3.1.1 (https://github.com/MuntashirAkon/libadb-android), Copyright 2021 Muntashir Al-Islam: dual licensed GPL-3.0-or-later OR Apache-2.0; **we use it under the
  Apache-2.0 terms** (`docs/licenses/Apache-2.0.txt`). Used unmodified.
- **spake2-android** (https://github.com/MuntashirAkon/spake2-java), Copyright 2021 Muntashir Al-Islam, a dependency of libadb-android: **LGPL-3.0** (`docs/licenses/LGPL-3.0.txt`,
  which refers to the GPL-3.0 text in `docs/licenses/GPL-3.0.txt`). Used unmodified, and it stays a separate library: our code never copies from it, it is linked as an ordinary
  dependency of the add-on. **Its source**: the repository above, at the version `android/extras/pins.txt` names (JitPack builds that tag). **To replace it** with a modified or
  another build: change the version or the source of `com.github.MuntashirAkon:libadb-android` and its `spake2-android` dependency in `android/extras/build.gradle` (and its line in
  `android/extras/pins.txt`) and build the add-on as `docs/DEVELOPERS.md` says; the whole source of the app is in this repository, so any version can be relinked.
- **Conscrypt** 2.5.3 (https://github.com/google/conscrypt), Copyright The Android Open Source Project and others: Apache-2.0. It gives TLS 1.3, which the pairing needs.
- **BouncyCastle** 1.81 (bcprov, bcpkix; https://www.bouncycastle.org/), Copyright The Legion of the Bouncy Castle Inc.: the Bouncy Castle licence (MIT style). It makes the add-on's
  self-signed certificate.
- The licence texts in `docs/licenses/` are the ones libadb-android and spake2-android ship.
