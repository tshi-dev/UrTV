# Changelog

Newest first. `next` is where changes land first; a release is what was tested there.

## 4.2 (on `next`)

- «إعداد تلفزيوني»: while a run waits for the TV's «السماح بتصحيح USB؟», our screensaver stays away (a prompt sent under it stays stuck until a restart, the tester's diagnosis; TCL ignores the screen's keep-on flag, since the prompt's window, not ours, is in front then). Two ways, both inside the app: a screen wake lock the app holds itself for the wait (WAKE_LOCK, already declared; Android starts no screensaver while one is held, and does not look at which window is in front), and a flag the screensaver asks the launcher for when it starts (`content://sa.urtv.home.nowplaying/setup_hold`, signature permission): while it is set the dream ends at once. Poking the TV's user activity needs DEVICE_POWER, which a normal app cannot hold. The hold ends when the answer comes or the wait gives up, and by itself after 240 s in any case. Log tags `UrTVSetup` and `UrTVSaver`.
- «مركز التحكم»: a tile «البلوتوث» («الريموت والملحقات») opens the TV's remotes-and-accessories screen: `android.settings.BLUETOOTH_SETTINGS`, else Android TV's accessories action `com.google.android.intent.action.CONNECT_INPUT`, the first that resolves (both in the manifest's `<queries>`); no tile when neither does. Locked by `@tvsettings`. It sits after «انعكاس الشاشة»; with ten tiles the panel stays inside the screen on 1080p, 720p and 540p (four to a row, or two rows of five on a small screen).
- «الرجوع يفتح شاشة التوقف», off by default: Back on our home (nothing to go back to) starts the screensaver's preview, as Clean Your TV does (c63d36a1), the way the «شاشة التوقف» pill starts it and behind its lock (`@saver`). Only as the TV's home and at the top (Back in the grid still goes to the dock first; a home opened from the «تلفزيونك» tile still leaves). `optimize/dock.sh backsaver on|off`; no TV row yet (the settings screens are on another branch).
- The remote's Settings key on our home (a user's request): a short press opens the TV's own settings (behind `@tvsettings`), a long press (held 500 ms) opens ours (behind `@urtv`, the TV's own if the screensaver app is missing). The key now waits for its release, or for a repeat that late, before it acts; a remote that sends no repeats is judged by how long it was held. Outside our home it is unchanged (`SettingsKey`: the TV's settings).
- No trailing «…» on menu items: the app menu's «تحديث تلفزيوني», «تلاوة وأذان», «إعدادات الأذان», «الرقابة الأبوية», «كل المصادر» in the inputs list, and in the screensaver's settings lists «بحث عن مدينة أو قرية», «تصفح الدوريات», «بحث عن أي فريق أو منتخب» (only the dots there). A text that is still working («جارٍ التحقق…») keeps them.
- The app's long-press menu on our home (a user's request): under its last item a row of two small buttons, «تفاصيل التطبيق» (Android's app-details screen for that app) and «حذف التطبيق» (Android's own uninstall, which asks to confirm; not shown for a system app or our two apps). None in kids mode; both behind the family lock (`MENU_APP`: «قفل الإعدادات», the TV's settings' lock and the app's own). No «إيقاف إجباري»: a normal app cannot force-stop another, and the TV's details screen cannot be opened on its «فرض الإيقاف», so it would only have been the same screen again. The launcher now declares `REQUEST_DELETE_PACKAGES` (a normal permission; without it Android 9+ drops the uninstall request).

Four options for the adhan, each a switch in «تلاوة وأذان» ← الأذان and each off until it is turned on. The adhan itself stays off by default.
- «الأذان على شاشة التوقف فقط» (#49): the adhan plays only over the screensaver or our home screen; another app or an input in front gets none. `optimize/music.sh adhan saver-only on|off`.
- «تنبيه عند الأذان»: a calm text notice for 5 seconds at the prayer's time, «حان وقت صلاة العصر», over whatever is in front, even when the adhan is off or skipped. A system toast (no permission). `optimize/music.sh adhan notice on|off`; the status says what the last alarm did (`adhan_alarm_last=`).
- «تكبير فقط»: a third length next to «أذان مختصر». Only the opening takbir plays (cut at 14 seconds, a 2-second fade), the same time for every recording. `optimize/music.sh adhan takbir on|off`.
- «شاشة التوقف وقت الأذان»: at the prayer's time the screensaver starts so the clock and prayer screen show, only when our home is in front, never over another app. `optimize/music.sh adhan saver-at on|off`.

- «المهام» (issue #48): a text field with «إضافة» inside the screen, so a task can be added on a Google TV where the system's speech window gives nothing (its search box never reaches our app). The field takes the TV keyboard's own mic (Gboard, Leanback), its action key is «إضافة» (not search) and Enter adds; the D-pad reaches it with the usual gold focus. It is the same add as `optimize/saver.sh tasks add` (up to 12 tasks, 60 characters), Arabic and right to left.
- «🎤 تكلّم» is shown only when the TV has a speech recognizer (`SpeechRecognizer.isRecognitionAvailable` or an activity for the speech intent; the manifest now lets Android 11 show us recognizer services); a speech window that opens and closes at once with nothing hides the button for that visit. No dead button.

- **Home switch when the setup wizard owns the Wi-Fi** (TCL G08/G10/G14 on Android 12–14, Google TV sets such as a G7max: the most common reason the home switch was skipped). `optimize/install.sh launcher` no longer just goes on with the wizard kept and ends in «Home switch skipped»: before anything is changed it pauses once (exit 5, `run.sh: paused at launcher`) with an Arabic line for the person: forget the Wi-Fi network in the TV's Settings ← الشبكة and join it again with the same password, then say «كمّل». The next run reads the network's owner again. If it moved off the wizard (seen on a TCL G08, Android 14: it moves to the Settings app at once), the switch goes in the safe order: the wizard off first while the stock home is still Home, the Wi-Fi checked read-only (still connected, no saved network fewer), our home started and confirmed in front, and only then the stock home off and the HOME role ours (the order from CleanYourTV, MIT). A Wi-Fi that changed, or our home not coming up, puts the wizard and the stock home back at once. If the network did not move, it is as before (the wizard stays on; if Home stays with the stock home, one plain line and the «تلفزيونك» tile). It never asks twice, and never forgets, deletes or changes a network itself. The journal (`home-switch.json`) now names each step before it acts.
- The `!! The safety switch put the stock home back` line is printed only when it is true (Home answers with the stock home, it is enabled, and the journal says restored). Otherwise the line says what the TV shows: the stock home back, the stock home still OFF (with `optimize/restore-home.sh`), Home already ours, or that the TV did not say. On 4.1 the old fixed line was printed while `launcherx` stayed disabled.

- Remote: TCL's own keys work on our home: Netflix (4062), YouTube (4065) and Prime Video (4099) open their app when it is installed, and are passed on otherwise; the Settings key of a TCL G05 (4077) is the Settings key like 176 and 83 (they were logged by `UrTVKeys` and dropped).
- Setup on Android 9 and 10 (SDK below 30): `optimize/tweaks.sh` no longer sets the Play Store's standby bucket (6 of 7 Android 9 reports still ended "with problems" because the TV kept `active`). It is skipped with one journal line (`skipped_standby_bucket`), no `!!`. Undo still puts back a bucket an older run recorded.
- Setup no longer turns the cached-apps freezer on (`cached_apps_freezer`): one report linked it to apps hanging after standby, and Android 14 has it on by itself. The Play Store background tweak stays. A TV an older run already set gets its own value back from `optimize/tweaks.sh restore`, from the next apply, and from `reset.sh`.
- Screensaver: the Arabic line under the clock («بتوقيت الرياض») and the card labels («الصلاة القادمة», «المباريات») no longer have letter-spacing: Chromium breaks the joining of Arabic letters when it is not 0, so the letters were cut apart on some TVs.
- Never disabled: `com.android.sdksandbox` is now in the core list (`optimize/lists/core.txt`), beside `com.android.location.fused` (already there; a Clean Your TV report of a boot loop on a TCL). The app's copy of the lists (`SetupLists.java`) is regenerated.

## 4.1

The fixes for what broke after updating to 4.0: on TCL the phone remote stopped working inside other apps and the adhan was not rescheduled after an update (TCL's ASSOC_START op: `optimize/autostart.sh` now allows it; TVs already set up get it from the agent with «حدّث تلفزيوني»); a muted TV stayed at volume 0 after the adhan; the Settings key did nothing on HDMI; the screensaver cut phone pairing. Also in it: the new phone page, «نظام الساعة» 12/24, «حفظ» in long lists, «انعكاس الشاشة», and 3,679 cities in 242 countries and territories.

## 4.0

**Version 4.0:** the release has a name now, the `VERSION` file at the root («4.0»). The apps' `versionName` is exactly `4.0` (what Android's app info shows), the `versionCode` is still the GitHub run number, so every build installs over the last; the screens show «4.0» (the build only in the reports)
(settings «الإصدار», «تحديث تلفزيوني», the phone page, `saver.sh show`). Updates are untouched: `version.txt` is still exactly `3.<build>` (an app installed
before 4.0 (build 142 and later) reads it as before and still sees the update), and the apps compare by build, never by name. Master builds publish `apk-latest` as before and a
numbered release tagged `v4.0.<build>` (only the URL shows it; the title is «UrTV 4.0», the build is in the body); the older `v3.N` ones stay.

**Phone**
- The phone remote's arrows, OK, Back and Home work in every app, through our accessibility service («تلفزيونك: التحكم والكتابة من الجوال», renamed from «…: الكتابة من الجوال»; or the sleep timer's service from Android 13): the dpad global actions on Android 13+, a focus move on the window in front before it (OK = a click on the focused node). Our own screens keep their exact key path; the phone page names the service when it is off. A phone key stops a playing adhan first when «إيقاف الأذان بأي زر» is on. Log tag `UrTVRemote`: the key, the route and the answer only.
- Typing from the phone into other apps: the typing service now declares `typeWindowStateChanged` (read by nobody) so Android tracks the window in front and the field can be found; it also takes the input-focused field first and fields that accept set-text. A fixed-words log (tag `UrTVType`) shows each step.
- «تحكم من الجوال» is the first row of «الجوال والتحديث», and that tile names it.
**Remote**
- The remote's Settings key outside our home (a receiver on HDMI, Live TV, any app) opens the TV's settings, so the picture mode can be changed per input with the stock launcher still off. Through our accessibility service «تلفزيونك: التحكم والكتابة من الجوال»: it asks Android for keys only while our home is not in front and no stock home is enabled, passes every other key on untouched and unlogged; a service that is on needs nothing after the update. On our home the key still opens our settings. «وضع الرسيفر» (the stock launcher kept on) is a design in docs/TVS.md, not built: on a TCL the stock launcher takes Home back.
**Fixes from the TV test**
- After the app updates itself (157 → 159, 159 → 160 on the owner's TCL) Android left our accessibility services unbound and the switch at 0 until a reboot. The home now heals it after an update, at boot and when it first shows (with WRITE_SECURE_SETTINGS: the switch to 1 and the same list written again; other apps' entries untouched, a lost one only logged under `UrTVA11y`), `dock.sh type-setup` does the same when the TV says ours is not bound, and the phone page says «التحكم في التطبيقات الثانية متوقف: …» instead of doing nothing.
- An adhan on a muted TV no longer leaves it at volume 0: the level is put back and the TV muted again.
- The screensaver coming over the pairing screen no longer switches phone control off (only leaving the screen does); «ابدأ» in «إعداد تلفزيوني» has the focus once the screen is drawn.
- Phone page: Arabic counting («8 ساعات و 56 دقيقة», «دقيقتين»), the TV's real screensaver delay (45 s) shown as the current choice, «بدون» saved by the first press again, one swipe = one step, the text field no longer under the tab bar.
- A tile «تلفزيونك» (an activity-alias with LEANBACK_LAUNCHER and our banner) opens the home from the TV's own apps row, for the one setup in four that keeps the stock home (setupwraith / launcherx); it never changes which home is the default, Back at the top of our home then leaves it for the home that opened it, and our own «كل التطبيقات» does not list it.
- The adhan alarm no longer falls back to the bundled place (Riyadh) when the screensaver's settings cannot be read (the owner's TCL kills its process and refuses to start it from the background): the last good place is kept, a failed read never reschedules, and the alarm is set from the place it was judged by (log tag `UrTVAdhan`); the home's other settings keep their last good values meanwhile.
- Phone page, «الإعدادات»: the cards being chosen are no longer put back by a TV answer that was still on its way (a «بدون» pressed soon after a save sent the old card again); the message over the page no longer takes taps.
- The adhkar card with Western digits chosen no longer shows the surahs' Arabic-Indic ayah numbers (the ornament stays empty, as a Western digit would fall outside the circle).
- The screen stays on (only while shown) on the phone-control pairing screen and its «سماح/رفض» prompt, the update screens and «إعداد تلفزيوني», so the screensaver no longer closes them.
- Typing from the phone into YouTube TV (no text field to Android) opens YouTube's own search with the words; where an app has none, the phone page says so. The log (`UrTVType`) now adds the focused node's class name when no field is found.
- Cards saved from the phone (setting the first one back to «بدون» too) reach the home screen in front at once; the TV now asks the screensaver what it holds after the save and tells the phone if it is not what was sent.
- Typing into YouTube TV: its search opens first, on any of its screens (the first version asked Android if the app could be reached, which Android 11+ answers «no» for other apps).
- «حماية العائلة (DNS)»: the first check waits for Private DNS and retries a fresh name (up to 40 s), so a working TV is no longer turned down.
- The adhkar card shows a dhikr at night too (the evening ones when there is no night verse), never only a start time.

**Prayer times and adhan**
- Gulf cities, each country's own prayer method, and Ja'fari. Oman uses its Ministry of Awqaf's method (Fajr and Isha 18°, Dhuhr, Asr and Maghrib +5 minutes, from egyjs/afnon-android-tv-launcher, MIT); the Gulf method stays as «طريقة الحساب» ← الخليج.
- «تعديل وقت الصلاة»: move each prayer by its own minutes.
- The adhan bar names the prayer before the adhan; the Fajr voice and a short adhan; honest buffering.
- The adhan keeps its right time through previews and city changes; the TV's volume comes back after any adhan.
- «أذان من ملفي»: the person's own adhan; the old one stays until the new one is in.

**Home screen and screensaver**
- Settings › «شاشة التوقف» › «نظام الساعة»: «١٢ ساعة» or «٢٤ ساعة», the same setting as `saver.sh clock24`; the screensaver, the home's clock and cards and «تلاوة وأذان» follow it.
- The lists of choices with «حفظ» in the settings («الدوريات كاملة», «الفرق المتابعة», «التصنيفات», «اللغات», «رياضات أخرى», «الأسواق», «مسلسلات مختارة») keep «حفظ» on the screen: the list scrolls in a capped height, ▼ on its last row reaches «حفظ», and Back saves too (a user could not save «الدوريات كاملة» on a TV).
- «مركز التحكم» has «انعكاس الشاشة» when the TV has a screen-mirroring app (TCL's «عرض الشاشة» first), with «من الجوال: شارك الشاشة واختر التلفزيون» under it; with nine tiles the panel may go five to a row.
- Network only while the home is in front and the display is on (weather, hosted photos, Aladhan's times, adhan recordings, the update check); the adhan itself is unchanged. The audit is `docs/NETWORK.md`.
- The «المصادر» list shows the name an HDMI-CEC device gives itself (console, box) instead of «HDMI 2», and « · وضع الاستعداد» when the input is in standby; your own name still wins; `dock.sh input-names` shows both.
- «جرس التحديثات»: a bell in the home screen's top row that shows only on evidence of waiting Play Store updates (none can be read with normal permissions yet, so it stays out of sight), a setting and `saver.sh updatebell`.
- «تعديل المفضلة»: press a favourite to pick it up and move it along the row, × to remove it with «تراجع»; «ترتيب التطبيقات المفضلة» is now in the settings only.
- The glass look (د) on every screen of ours: settings, tasks, phone control and pairing, parental and the PIN, update, «إعداد تلفزيوني», «مركز التحكم», Family protection, the «تلاوة وأذان» screens and the phone page. Smoked glass in dark, frosted white in light, over the home's photo blurred once per photo (no live blur); each photo gets the veil its texts need to read.
- «المظهر» is the first row of the home screen's settings: «الشكل» (زجاجي، فحمي، كحلي، نظيف) for every screen of ours, and «الإضاءة» (داكن، فاتح، تلقائي), each choice previewed live before it is picked; `saver.sh look`.
- From the TV test (build 147): «أكمل المشاهدة» is a row of favourite-sized cards under the favourites again (it could come before them and fill the screen); a focused row in the settings has dark ink on its white fill; the settings never move sideways; the pickers are opaque; the «إعدادات تلفزيونك» tile has its icon.
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

**Thanks (GitHub handles, with the issue or pull request number):**
- Brent oil in the markets bar: hak151617-sys (#39).
- The 24-hour clock «ساعة ٢٤»: saadabdelhafid-cyber (#41).
- The Jordan prayer method: alaaalquda-hub (#34). Egypt's method and cities are in too.
- The fix for the older WebView (66) that stopped the page's JavaScript: ayhankaraalitr-eng posted it first (#21), ckpb4fjvs7-ops sent the same fix again (#43), and omarghxd-art reported the problem (#19).
- The sunrise after Fajr, the bar of the time between two prayers and the humidity and wind: beeshooo2010-jpg (#28).
- The audio-focus fix (the Quran and the adhan no longer stutter or fight other sound): itsAbdulelah (#35); the TV updating itself, earlier: itsAbdulelah (#10).
- The PIN and kids mode («حماية العائلة»): AbdullahCg (#40).
- Club crests: samiharthy (#30).

## 23 October 2026 (51b9680)

Release: everything tested on `next` up to that day became the release.
