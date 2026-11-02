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

## Clean Your TV (remote keys, protected packages)

The home also answers TCL's vendor remote keys (4062, 4065, 4099) from the idea in Clean Your TV commit e525f084 (vendor keys of 1000 and up
reach only the window in front, so the home must handle them), and `com.android.sdksandbox` / `com.android.location.fused` are never disabled
after its commits bf505124 and f5c37c36. Clean Your TV (https://github.com/Gathin23/cleanyourtv) is MIT licensed; its license text is in
`optimize/lists/community/LICENSE-CleanYourTV.txt`.
