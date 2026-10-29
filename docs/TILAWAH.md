# Tilawah «تلاوة» — the Quran, the radio and the adhan

The «تلاوة» screen is part of the **home app** (`sa.urtv.home`), not an app of its own: the pill in the top bar
opens it, and it is never listed among the TV's apps. It plays the Quran only — forty reciters' Hafs
murattal (114 surahs each) and live Quran radio — plus the five daily adhans at Umm al-Qura times.
There is no local/USB music, no Spotify or YouTube Music, no account, no ads and no analytics.

Package `sa.urtv.home` · classes `sa.urtv.home.tilawah` · label «تلاوة» · minSdk 26, targetSdk 30.

## What is in here

| | |
|---|---|
| <img src="screenshots/02-tilawah-reciters.jpg" alt="The reciters tab" width="320"> | <img src="screenshots/03-adhan-times.jpg" alt="The adhan tab" width="320"> |
| The **«القرّاء»** tab — forty reciters, 114 surahs each, and «متابعة التلاوة» when a surah is paused | The **«الأذان»** tab — today's five times at Umm al-Qura, the next one in gold with the time left |
| <img src="screenshots/05-muezzins.jpg" alt="The muezzins" width="320"> | <img src="screenshots/07-surah-grid.jpg" alt="The surah grid" width="320"> |
| The **«المؤذن»** list — 21 named recordings; the chosen one in gold with its city | The **«سورة»** grid — the 114 surahs of a reciter, six columns, right to left |
| <img src="screenshots/06-adhan-player.jpg" alt="The adhan in the player" width="320"> | <img src="screenshots/08-player.jpg" alt="A surah in the player" width="320"> |
| During an adhan — «الأذان» and the muezzin's name; the skip buttons are gone and the arrows do not seek | A surah — title, reciter, progress and the transport row |

## Where it lives

- The «تلاوة» pill in the home screen's top bar opens `tilawah.MainActivity`.
- The four `tilawah` screens are `exported="false"` and have no `LEANBACK_LAUNCHER` category, so
  Android TV never shows them in the app grid; Back returns to the home screen.
- `res/values/config.xml`: `<bool name="show_listening">false</bool>` takes the pill away.

## Screens and remote

- **The screen**: a tab bar under the title — «القرّاء» (the default), «الإذاعات» and «الأذان» —
  read from the right, like the rest of the app. Left/Right switches section from anywhere on the
  screen; Down (or OK on the tab) enters the list and
  Up from the first row goes back to the tabs. «القرّاء» holds «متابعة التلاوة» when a surah is
  paused, then the forty reciters; «الإذاعات» holds the eleven live stations (and the resume row when
  a station was playing); «الأذان» holds today's five times (the next one in gold with the time left,
  OK plays that adhan now), «الأذان التلقائي» (OK switches it on or off), «المؤذن» (OK opens the 21
  recordings) and «صوت الأذان» (Left/Right set it in steps of 10, 10 % to 100 %, thirty by default, with a bar
  and two seconds of the adhan at the new level; OK does nothing). When exact alarms are not allowed (Android 12+) a row
  «قد يتأخر الأذان دقائق: فعّل المنبّهات الدقيقة» appears under the switch; OK opens the system screen for it. The selected row has a
  solid dark card, a thick gold frame and a gold chevron at the end, so it is visible from the couch.
- **Surah grid**: the 114 surahs in reading order, right to left; OK opens the player.
- **Player**: title, reciter/station, progress, and a transport row. The transport controls stay
  left-to-right (previous, play/pause, next) with universal icons; the rest of the screen is RTL.
  During an adhan it shows «الأذان» and the muezzin's name, hides the previous/next buttons, and
  Left/Right do nothing: an adhan is not seekable.
- **The muezzins**: twenty-one recordings from the public-domain «Azaan of World Mosques» collection —
  seven muezzins of Makkah (عبدالملك الملا، عبدالرحمن خاشقجي، عبدالعزيز بخاري and others), ناجي قزاز
  of Jerusalem, and the mosques and cities: الحرم المكي، المسجد الأقصى، القاهرة، عجمان، أم القيوين،
  عُمان، تونس، ماليزيا، المالديف. The chosen one is in gold with its city under the name; OK picks it
  and plays twelve seconds of it.

| Key | Action |
|---|---|
| OK / Enter | play / pause (in the lists: open / choose) |
| ◀ / ▶ | seek −10 s / +10 s (switches the section on the تلاوة screen); held, one step of 10 s every 250 ms, never faster (the service also drops any burst closer than 100 ms) |
| ▼ | sleep timer (30, 60 or 90 minutes; off; stop now) |
| Back | back to the list — playback continues |
| media play/pause, next, previous | work through the media session |

During an adhan, seek and next/previous are ignored, and the hint line says so.

The sleep timer fades the volume out at the end and shows the remaining minutes in gold on the
player. Cancelling it during the fade stops the fade and the sound comes back. Paused (or with another app holding the
sound) for 30 minutes, the player stops by itself; the Wi-Fi lock and the position tick are held only while something plays,
and the place in the surah is saved when it pauses or stops, not every few seconds. On Android 13+ the app asks for the notification permission on first launch; playback
works without it but the media notification is hidden.

## Right to left, and the arrows

The launcher declares `android:supportsRtl="true"`, the four `tilawah` layouts carry
`android:layoutDirection="rtl"` and the home screen (built in code) sets
`View.LAYOUT_DIRECTION_RTL` on its root. So the whole app reads from the right: the title and the
first tab sit on the right, the chevron of a row sits on its left, the surah grid fills from the
right, and the pills, the favourites and the app rows of the home screen do the same.

Android mirrors the D-pad focus search in an RTL layout (there, «right» would move the highlight to
the left), so the screens walk their rows by hand: the home moves the focus to the neighbour on the
side that was pressed, the surah grid does the same for the cells, and the «تلاوة» tabs are
switched by hand as well. In every screen of the app the right arrow moves the highlight to the
right and the left arrow to the left.

## The adhan

- **The times** are the calculation of the city's country (Umm al-Qura for Saudi Arabia and anywhere outside the Gulf; Kuwait, Qatar, the UAE, Bahrain and Oman each their own; the Shia Ja'fari one when «طريقة الحساب» says so: `docs/SETTINGS.md`), computed on the TV with no network. For Umm al-Qura: Fajr at 18.5°, Maghrib at
  sunset, Asr at shadow factor 1, Isha 90 minutes after Maghrib (120 in Ramadan, from the
  Umm al-Qura calendar) — an absolute moment, so a Maghrib near midnight at a high latitude gives an Isha after midnight,
  never an hour that has already gone. This is the same arithmetic as the screensaver's page and the same method
  Aladhan calls `method=4`. Once a day the times are quietly replaced by Aladhan's (asked for the city's own date, kept under a key that names the
  city, so a city change the same day asks again; a time written «5:12 (+03)» is read as 5:12), the alarm is set again for
  them, and Isha is always recomputed from Maghrib so Ramadan stays right.
- **The city** is the one picked in the screensaver (its `ConfigProvider`: `lat`, `lon`, `tz`);
  without the screensaver, `config.xml`'s `latitude`, `longitude` and `timezone` are used.
- **The switch is off by default.** «الأذان التلقائي» turns it on; the choice is remembered.
- **The alarm** is one `AlarmManager` alarm for the next of the five prayers (action `ADHAN_ALARM`, naming the receiver; the
  shell's `sa.urtv.home.ADHAN` is «adhan now» and reaches `MusicReceiver` only). It is exact when Android allows it; from
  Android 12 without the exact-alarm permission the system may deliver it minutes late, and the adhan tab says so. It is put
  back when the adhan has played, after a reboot, after a clock or timezone change, after an update and when Aladhan's times
  arrive. The same prayer's adhan never plays twice (judged by the prayer's own time, so a preview of a muezzin five
  minutes before does not silence the real one); a preview that is playing gives way to the real adhan.
- **Only while the TV is on.** An alarm that arrives in standby is skipped, not played late: there is
  no catch-up after a reboot or a missed prayer, and an adhan that is playing stops when the screen
  goes off.
- **Its own volume.** The adhan plays at 30% of the TV's maximum (`صوت الأذان` in the adhan tab: Left/Right, 10–100% in
  steps of 10; `optimize/music.sh adhan volume 60`), and the volume the TV had comes back when the adhan ends.
- **During an adhan** a playing surah (or station) pauses, the adhan plays, and the surah resumes
  where it stopped. «متابعة التلاوة» keeps pointing at the surah, never at the adhan.
- **The recordings** are twenty-one adhans (muezzins and mosques) from the «Azaan of World Mosques»
  collection on Archive.org, released under the **Public Domain Mark 1.0**
  (<https://archive.org/details/azaan-of-world-mosques_202608>) — free to redistribute, no attribution
  required; the source is credited in the app's adhan screen and here. The table of names and URLs is
  in `Adhan.java`. Each one is kept in `getCacheDir()/adhan/adhan-N.mp3` the first time it is needed
  — the chosen one first, fetched when the alarm is set (not only when the screen opens), the rest lazily, a failed one
  skipped, at most one round a day — so an adhan plays from the saved copy, with the connection down. A file is kept only if
  its length is the server's Content-Length. A stream that drops falls back to the saved copy.
- **«أذان من ملفي»** is the person's own recording (`OwnAdhan.java`), for a muezzin whose recordings are not
  free to redistribute, so none is ever bundled: `optimize/music.sh adhan add FILE` makes a mono AAC of about
  48 kbps, at most 5 minutes, on the computer (ffmpeg), uploads it through `OwnAdhanProvider` (`content write`,
  shell only, write-only) and `MusicReceiver ADHAN_OWN` checks and keeps it as `files/adhan-own.m4a`: one file, a new
  one replaces it. It stays in the app's private folder, is never read out or sent, and plays straight from
  there (no download, no retry from the network). When it exists the adhan screen's first row is «أذان من ملفي»
  (voice number 0); `adhan voice 1-21` goes back to ours and `adhan own-clear` removes it.
- The switch, the muezzin and the volume live in the app's preferences (`tilawah`); the alarm needs
  `SCHEDULE_EXACT_ALARM` and the volume needs `MODIFY_AUDIO_SETTINGS` (both are ordinary permissions;
  the app falls back to an inexact alarm if exact alarms are refused).

## Control it over ADB

`optimize/music.sh` wraps the app's shell-only broadcast receiver (the receiver requires
`android.permission.DUMP`, so only the ADB shell can send it — other apps cannot):

```bash
optimize/music.sh status
optimize/music.sh reciters                 # ids for the next command
optimize/music.sh stations                 # indices for the next command
optimize/music.sh surah sudais 1           # Al-Fatiha by Sudais
optimize/music.sh radio 0                  # Saudi Quran Radio
optimize/music.sh pause | resume | toggle | next | prev | stop
optimize/music.sh seek -10                 # 10 seconds back
optimize/music.sh sleep 30                 # 0 cancels
optimize/music.sh adhan                    # on/off, voice, next time, last played
optimize/music.sh adhan now 3              # play voice 3 right now
optimize/music.sh adhan off | on
optimize/music.sh adhan voice 7            # the recording used at Dhuhr, Asr, Maghrib and Isha (not the Fajr ones: 10 13 17 20)
optimize/music.sh adhan fajr-voice 10      # «مؤذن الفجر»: Fajr's own muezzin (default 10)
optimize/music.sh adhan short on|off       # «أذان مختصر»: stops after the first minute, 3-second fade
optimize/music.sh adhan stopkey on|off     # «إيقاف الأذان بأي زر»: ON by default, any key stops the adhan (volume keys and Power excepted); off: only OK on the adhan bar
optimize/music.sh adhan stop               # stop the adhan that is playing
optimize/music.sh adhan volume 60          # its own volume, 10-100
optimize/music.sh adhan add my-adhan.mp3   # the person's own recording (ffmpeg here makes it small); `adhan voice own`, `adhan own-clear`
```

`status` prints one line:

```
playing=1|source=surah|reciter=sudais|reciter_name=عبدالرحمن السديس|surah=1|surah_name=الفاتحة|station=-1|station_name=|pos=12400|dur=36075|sleep=0|adhan=on|voice=1|adhan_volume=30|adhan_exact=1|adhan_next=1761000000000|adhan_next_name=المغرب|adhan_next_clock=17:42|adhan_last=0|error=
```

The raw broadcasts are the same actions:

```bash
adb shell am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.GET_PLAYER
adb shell am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.PLAY_SURAH --es reciter sudais --ei surah 1
adb shell am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.PLAY_RADIO --ei station 0
adb shell am broadcast -n sa.urtv.home/.tilawah.MusicReceiver -a sa.urtv.home.ADHAN --ei voice 3
```

## Catalog

The catalog is generated and committed, so the app builds and starts offline:

```bash
python3 .github/tools/fetch_quran.py           # regenerate the three JSON files
python3 .github/tools/fetch_quran.py --check   # ranged GET of every stream URL
```

- **Reciters** are forty curated Hafs murattal entries from [mp3quran.net](https://mp3quran.net);
  the app builds `server + 001.mp3 … 114.mp3` from the server field.
- **Surahs** come from [api.alquran.cloud](https://alquran.cloud); names are stripped of the
  «سُورَةُ» prefix, vowel marks and tatweel.
- **Stations** are eleven live streams from mp3quran/qurango.net, with the Saudi Quran Radio first.
- Streams move or go quiet; the `--check` run verifies one kilobyte of each URL. At the time of
  writing all 51 URLs answered with `audio/mpeg`. A station that fails may be temporarily down;
  the app retries and reports it.

The assets live in `android/launcher/src/main/assets/quran/` (`reciters.json`, `stations.json`,
`surahs.json`). `tests/tilawah_test.py` checks the catalog, the adhan, the cleartext
scope and the manifest.

## Network and privacy

- Runtime requests are the audio streams and their redirects, the adhan recordings
  (`archive.org`), and one Aladhan call a day (`api.aladhan.com`, method 4). There is no account,
  upload, analytics or ad SDK.
- `res/xml/network_security_config.xml` sets `cleartextTrafficPermitted="false"` globally. The Saudi
  Quran Radio (`stream.radiojar.com`) redirects to its CDN over `http://n0X.radiojar.com`; cleartext
  is allowed only for `radiojar.com` and its subdomains. Nothing else may use HTTP.
- Permissions: internet, network/Wi-Fi state (Wi-Fi lock), wake lock, foreground media playback,
  notifications, boot completed (the adhan alarm), exact alarms, and modify audio settings (the
  adhan's own volume). There is no storage permission and no access to local media.

## Build and test

```bash
# with JDK 17 and an Android SDK with platform 34
gradle -p android :launcher:assembleRelease
python3 tests/tilawah_test.py
```

CI builds the home app with the screensaver, renames it to `urtv-home.apk`, and publishes it to
`apk-latest` and the numbered `v4.0.N` releases. The release key comes from the repository's
`ANDROID_KEYSTORE` secret; a local build without it uses the debug key.

## Known limits

- Live streams depend on third parties; a station can be down, move, or be blocked on some networks.
- No downloads/offline mode for surahs, by design (the adhan recordings are cached).
- The adhan times are a calculation (the country's, by the city's time zone), not a local authority's printed
  calendar: Umm al-Qura for a place outside the Gulf, which is the project's choice. `تعديل وقت الصلاة` moves them to a mosque's minutes.
- The UI is Arabic/RTL; the transport buttons are intentionally LTR so the previous/next arrows match
  every other player.
