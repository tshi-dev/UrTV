# Changelog

Newest first. `next` is where changes land first; a release is what was tested there.

## On `next` (not released yet)

**Phone**
- Typing from the phone into other apps: the typing service now declares `typeWindowStateChanged` (read by nobody) so Android tracks the window in front and the field can be found; it also takes the input-focused field first and fields that accept set-text. A fixed-words log (tag `UrTVType`) shows each step.
- «تحكم من الجوال» is the first row of «الجوال والتحديث», and that tile names it.
**Fixes from the TV test**
- The screen stays on (only while shown) on the phone-control pairing screen and its «سماح/رفض» prompt, the update screens and «إعداد تلفزيوني», so the screensaver no longer closes them.
- Typing from the phone into YouTube TV (no text field to Android) opens YouTube's own search with the words; where an app has none, the phone page says so. The log (`UrTVType`) now adds the focused node's class name when no field is found.
- Cards saved from the phone (setting the first one back to «بدون» too) reach the home screen in front at once.
- «حماية العائلة (DNS)»: the first check waits for Private DNS and retries a fresh name (up to 40 s), so a working TV is no longer turned down.
- The adhkar card shows a dhikr at night too (the evening ones when there is no night verse), never only a start time.

**Prayer times and adhan**
- Gulf cities, each country's own prayer method, and Ja'fari. Oman uses its Ministry of Awqaf's method (Fajr and Isha 18°, Dhuhr, Asr and Maghrib +5 minutes, from egyjs/afnon-android-tv-launcher, MIT); the Gulf method stays as «طريقة الحساب» ← الخليج.
- «تعديل وقت الصلاة»: move each prayer by its own minutes.
- The adhan bar names the prayer before the adhan; the Fajr voice and a short adhan; honest buffering.
- The adhan keeps its right time through previews and city changes; the TV's volume comes back after any adhan.
- «أذان من ملفي»: the person's own adhan; the old one stays until the new one is in.

**Home screen and screensaver**
- Network only while the home is in front and the display is on (weather, hosted photos, Aladhan's times, adhan recordings, the update check); the adhan itself is unchanged. The audit is `docs/NETWORK.md`.
- The «المصادر» list shows the name an HDMI-CEC device gives itself (console, box) instead of «HDMI 2», and « · وضع الاستعداد» when the input is in standby; your own name still wins; `dock.sh input-names` shows both.
- «جرس التحديثات»: a bell in the home screen's top row that shows only on evidence of waiting Play Store updates (none can be read with normal permissions yet, so it stays out of sight), a setting and `saver.sh updatebell`.
- «تعديل المفضلة»: press a favourite to pick it up and move it along the row, × to remove it with «تراجع»; «ترتيب التطبيقات المفضلة» is now in the settings only.
- The glass look (د) on every screen of ours: settings, tasks, phone control and pairing, parental and the PIN, update, «إعداد تلفزيوني», «مركز التحكم», Family protection, the «تلاوة وأذان» screens and the phone page. Smoked glass in dark, frosted white in light, over the home's photo blurred once per photo (no live blur); each photo gets the veil its texts need to read.
- «المظهر» is the first row of the home screen's settings: «الشكل» (زجاجي، فحمي، كحلي، نظيف) for every screen of ours, and «الإضاءة» (داكن، فاتح، تلقائي), each choice previewed live before it is picked; `saver.sh look`.
- From the TV test of 3.147: «أكمل المشاهدة» is a row of favourite-sized cards under the favourites again (it could come before them and fill the screen); a focused row in the settings has dark ink on its white fill; the settings never move sideways; the pickers are opaque; the «إعدادات تلفزيونك» tile has its icon.
- «تعديل التطبيقات»: «كل التطبيقات» in your own order (pick up and move across the grid), × to hide an app with «تراجع», «المخفية» to bring one back, ▲ from the first row into the favourites; «ترتيب كل التطبيقات» in the settings, with «رجوع للترتيب الأصلي». Not in kids mode, and neither is «تعديل المفضلة» now.
- Prayer times as boxes, new pills (icons only, the name shows on focus), glass panels, softer glow on the focused app.
- «يُتلى الآن» card while تلاوة plays; the screen is now «تلاوة وأذان».
- A year strip under the clock, the date beside the clubs, club crests when fetch is refused, fixes to the league card.
- The screensaver starts from its pill; the page survives a WebView renderer that dies.
- «مركز التحكم»: one round pill opens a panel of tiles (sleep timer, turn off now, network, speed test, sound output, screensaver, our settings, the TV's settings); the sleep, screensaver, Wi-Fi and settings pills moved into it, locked by the same names, plus a new lock `@center`.
- «مركز التحكم»: all tiles fit on 1080p and 720p, it opens on the tile used last, the sound output tile names the real output, «أطفئ الآن» says first when the sleep service is off, a new «إعدادات تلفزيونك» tile (locked by `@urtv`); «حرّر الذاكرة» is gone (no use on Android 14). The remote's Settings key now opens our settings.
- The weather and the dust are told apart; forecast and weather show only as old as they are; the last price is kept and marked.
- The medium and small clocks lay the screen out again (the large one is unchanged); «بطاقة ثانية»: a second card beside the small clock.

**Speed and safety**
- Fetches and the live poll stay inside their deadlines; apps load off the UI thread; downloads are capped.
- The Unsplash key goes only to the Unsplash API.
- App caches are trimmed only when storage is low.
- Install no longer dies on a silent TV; the sleep timer is forgotten on restart; the stock home comes back at once if ours does not open.
- Android's own core packages are never disabled; reset puts back the values the backup recorded.

**Setup and tools**
- The home switch can always be undone: a nonce on the safety switch, a heartbeat check before switching, a journal read on the next run, and `optimize/restore-home.sh`.
- Pair over Wireless debugging, find the TV again, and check first that it is Android TV with a new enough WebView.
- Community lists for TVs our own lists do not know; the setup wizard and TCL auto-start are handled.
- The setup shows what was turned off and lets a person keep any of it.
- Problems are reported through the form, never GitHub; share steps never print a negative time; Windows 11 is told from 10.
- The ADB key and the saved TV stay outside the clone.
- `AGENTS.md` for agents; new rows for the `!!` lines; the models seen in reports in `docs/TVS.md`.
- Reports keep only the lines the script builds, and the scrub (the net for what the scripts printed on `!!` lines)
  also covers IPv6 addresses, emails, serials, tokens, links with tokens, Wi-Fi names and user names.
- `SECURITY.md`, `CONTRIBUTING.md` and this file.
- «تحكم من الجوال»: a much better page (a swipe pad, volume, an Arabic text field, the favourites with their real icons, what plays, the sleep timer and screensaver, a read-only family card) and the endpoints behind it; locked apps ask for the PIN on the TV; Back works over other apps with the accessibility service.
- For testing only: `optimize/saver.sh updatechannel next` makes «تحديث تلفزيوني» read the apk-next build (the CI now publishes its `version.txt`); `tests/android_compile.sh` compiles both apps against API 34 (SKIP when offline) and runs in `tests/run.sh`. It also fails on a source that uses a class hidden in the public SDK (`tests/hidden_api_scan.py`; that is what broke the release build once), and uses the real public `android.jar` when the Android SDK is installed.

## 23 October 2026 (51b9680)

Release: everything tested on `next` up to that day became the release.
