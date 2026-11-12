# Third-party notices

Code and ideas taken from other projects, with their licenses. Fonts, photos and lists carry their own notices next to them:
`assets/fonts/*-NOTICE.txt` and `*-OFL.txt`, `docs/PHOTO-CREDITS.md`, `optimize/lists/community/LICENSE-CleanYourTV.txt`, and the
launcher's origin in `android/launcher/LICENSE-tv-home`.

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

The LG webOS and Samsung Tizen packaging in `webos/` and `tizen/` (`build.py`, `install.py`, `appinfo.json` and `config.xml` as a
starting point) is also adapted from Free My TV: how to pack an `.ipk` and a `.wgt` with Python only, how to install over Developer
Mode (LG's Key Server and SSH, Samsung's SDB), and which key codes and manifest entries a TV app needs. Each adapted file says so in
its header.

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

## Wireless-debugging pairing (4.3.1): libadb-android, spake2-android, Conscrypt, BouncyCastle

For TVs whose own adb never shows «السماح بتصحيح USB؟», «تخلّص من واجهة المصنع» can pair the TV by wireless debugging, with the six digits typed on the phone page. The whole feature is the package
`android/launcher/src/main/java/sa/urtv/home/setup/pair/` with one entry class (`setup/PairRoute.java`); the libraries are the four `implementation` lines at the end of `android/launcher/build.gradle` and
the JitPack line in `android/settings.gradle`. Take those out together and the app builds and works as before.

- **libadb-android** 3.1.1 (https://github.com/MuntashirAkon/libadb-android), Copyright 2021 Muntashir Al-Islam: dual licensed GPL-3.0-or-later OR Apache-2.0; **we use it under the Apache-2.0 terms**
  (`docs/licenses/Apache-2.0.txt`). It is used unmodified.
- **spake2-android** 2.2.1 (https://github.com/MuntashirAkon/spake2-java), Copyright 2021 Muntashir Al-Islam, a dependency of libadb-android: **LGPL-3.0** (`docs/licenses/LGPL-3.0.txt`, which refers to
  the GPL-3.0 text in `docs/licenses/GPL-3.0.txt`). It is used unmodified and stays a separate library: the app's own code never copies from it, it is linked as an ordinary dependency. **To replace it** with a
  modified or another build: change the version (or the source) of `com.github.MuntashirAkon:libadb-android` and its `spake2-android` dependency in `android/launcher/build.gradle` and `android/settings.gradle`
  and build the app as `docs/DEVELOPERS.md` says; the whole source of the app is in this repository, so any version can be relinked. Its source: the repository above.
- **Conscrypt** 2.5.3 (https://github.com/google/conscrypt), Copyright The Android Open Source Project and others: Apache-2.0. It gives TLS 1.3, which the pairing needs.
- **BouncyCastle** 1.81 (bcprov, bcpkix; https://www.bouncycastle.org/), Copyright The Legion of the Bouncy Castle Inc.: the Bouncy Castle licence (MIT style). It makes the app's self-signed certificate.
- The licence texts in `docs/licenses/` are the ones libadb-android and spake2-android ship (REUSE format: a short header, then the licence).
