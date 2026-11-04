step 9, part A — in progress (A3 typing and A5 left)

Version: before 3.148, after "version: 3.149" (install.sh all; «Home is already the new launcher: nothing to switch»; no !! line).

| # | OK/problem | shots | note |
|---|---|---|---|
| A1 | OK | look-row, look-glass, look-graphite, look-navy, look-clean, light-pick | «المظهر» is the first row of «الشاشة الرئيسية» («زجاجي · داكن»); it opens «الشكل» / «الإضاءة»; each of the 4 shapes shows its own preview; «الإضاءة»: داكن، فاتح، تلقائي. Small: in the glass preview the focused mini-tile «الموقع» is white text on white |
| A2 | OK | look-<look>-<dark|light>-<home|hub|row>, looks-<look>-<dark|light>-dialog | all 4 looks x dark/light: main menu, section with a focused row and «طريقة الحساب» dialog all readable; focused item readable (gold/amber/dark/green fill with dark or white text); dialogs solid. The home screen looks the same in every look (only the settings change) |
| B1 | shots made | 32 shots | made with a demo city (مكة المكرمة), real city put back, look glass + dark. Sent to the owner in the chat to choose. NOT pushed to GitHub: the owner's own rule for this run is text only; waiting for his OK. Same for the icons task (X1/X2), and ثمانية and سين are not installed on this TV |
| (3.150) | installed | — | 3.150 installed for A3-A5 |
| A3 pair | OK | p9-ask, p9-allowed | owner typed the code in the PC's Chrome; TV asked «سماح/رفض» showing the PC's address; «سماح» → page «متصل · بقي 30 دقيقة»; no screensaver during pairing |
| A3 type | blocked | p9-yt3, p9-yt4 | YouTube search page open (owner said to pick his profile). «قرآن» sent from the page; log: `text request: mode=replace search=false len=4` / `foreground=other` / `service=off` / `result=service_off`. The typing service «تلفزيونك: الكتابة من الجوال» is Off in Accessibility (start line has only Key Mapper). Turning it on is a security setting: left for the owner (one OK press, he is away). The page shows a clear hint to turn it on: good |
| A4 | problem | — | «الصلاة القادمة» + «حفظ البطاقات»: `UrTVPhone: home_cards sent=1 saved=1 same=true`, home_panel nextprayer: OK. Then «بدون» + «حفظ البطاقات»: FIRST press did nothing (no UrTVPhone line, home_panel still nextprayer); SECOND press: `home_cards sent=0 saved=0 same=true`, home_panel none. Guess: the page's /api/state refresh re-renders the selects (element refs were replaced) and the first save is lost |
| A5 | waiting | — | phone control still on (session ends by itself after 30 min); will turn off and drop the pairing after A3 |
| note | check | p9-acc | the screensaver came on about 3 s after Android's Accessibility page was opened by am start (TV had been idle before) |

## Voice (V1-V3)
| # | Result |
|---|---|
| V1 | /v1/models gave 401 (key cannot list models). eleven_v3 with mp3_44100_192 gave 403: "Output format mp3_44100_192 is only available on the Creator tier and above". Ran with EL_MODEL=eleven_v4 and mp3_44100_128 (only change, local copy): 26/26 ok |
| V2 | 26 files, none under 5 KB (1.3 MB total); owner listened: "That's good" |
| V3 | pushed tester/voice/01.mp3 ... 26.mp3 |

## Voice round 3 (N2)
| # | Result |
|---|---|
| N2 | voice 0 (current) + voices 1-4, each on eleven_v4 and eleven_v3 (mp3_44100_128), 2 lines each: 20 files in tester/voice-test/ (voice-<n>-<1|2>.mp3 = v4, voice-<n>-v3-<1|2>.mp3 = v3). All ok |
| N3 | waiting: v4 files sent to the owner to pick |

## Voice round 4 FINAL (F1-F2)
| # | Result |
|---|---|
| F1 | voice rpGHcNQJvO8dFNNFNj1v, eleven_v4, lines-v3.txt, EL_SPLIT=1: 38 files, none under 5 KB (1.6 MB) |
| F2 | pushed tester/voice-v3/*.mp3 (38) |

## Adhan (H1-H3)
| # | Result |
|---|---|
| H1 | Wikimedia Commons, Category:Adhan: "Adhan in Istanbul", own work, {{self|cc-zero}}, 50 s city adhan. Only CC0 own-work file in the category (others are CC BY / BY-SA, or PD claimed for a singer) |
| H2 | downloaded; tester/adhan/SOURCE.md written |
| H3 | pushed the audio track (copied, not re-encoded) + SOURCE.md; video not pushed (63 MB) |

## Voice FINAL by the owner (replaces voice-v3)
| # | Result |
|---|---|
| O1 | The owner cloned his own voice (ElevenLabs Instant Voice Clone "UrTV owner", id wfj1guOwi64ozn1ggUGY, from his own 7:41 recording) and made the 26 lines himself on the ElevenLabs site with eleven_v4, with his own small wording fixes (e.g. 11 «أي زر تضغط راح يوقفه», 14 «على قد ما نقدر», 18 «راح يطلب رمز») |
| O2 | Cut at «…» into 38 parts NN-k.mp3 (same names as voice-v3); every part checked by transcription; 18 cut at its real pause |
| O3 | pushed tester/voice-owner/*.mp3 (38). Owner listened: "That's good". Use these instead of tester/voice-v3/ |

## step 10 (apk-next 3.152): R1-R3, R5, Z done; R4 left for daytime
Version: home 3.152, screensaver 3.152 (install.sh all; no new !! line).
Typing service: the owner ran `optimize/dock.sh type-setup` himself; TypeService now in enabled_accessibility_services. Left ON as asked.

| # | OK/problem | shots | note |
|---|---|---|---|
| R1 YouTube | problem (small) | remote-yt…yt6 | On YouTube's profile picker the page's OK and ▶ log `route=global result=done` but nothing moves or opens. Inside YouTube: ▶ moves the focus, OK opens the video, Back returns, Home goes to our home: OK |
| R1 TV settings | OK | remote-settings | ▼▼ moves the focus two rows; Back leaves; Home OK |
| R1 Shahid | OK | remote-shahid2-3 | ▼ and ▲ move the focus on Shahid's first screen; Home OK |
| R1 log | — | — | `UrTVRemote: key=ok/right/down/back/home route=global result=done` (no content). Before the service was on, the page keys did nothing in other apps (expected) |
| R2 | OK | yt-search3 | `text request: mode=replace search=true len=4` / `foreground=other` / `service=on` / `root=present` / `search=started` / `result=searched`; YouTube shows the results for «قرآن» |
| R3 | OK (fixed) | — | «الصلاة القادمة»+save: `home_cards sent=1 saved=1 same=true`, home_panel nextprayer. «بدون»+save ONCE: `home_cards sent=0 saved=0 same=true`, home_panel none on the FIRST press |
| R4 | not done | — | it plays the adhan aloud at night; the owner asked to keep the volume at 0. To do in daytime (needs a new pairing) |
| R5 | OK | p10-r5b | «إيقاف التحكم من الجوال»: back to «تشغيل», the PC's pairing gone; service still on |
| Z | restored: yes (service left on, as asked) | — | home_panel none, glass + dark, updatechannel latest; TV sound muted (owner's wish) |
| note 1 | to look at | — | when YouTube opened, the TCL system killed sa.urtv.screensaver and then refused the home app starting its ConfigProvider: `TclAppBoot … calleeComponent='sa.urtv.screensaver/.ConfigProvider' … isAllow='false', reason='callee_does't_have_OP_ASSOC_START_permission'` and `Failed to find provider info for sa.urtv.screensaver.config` (repeated) |
| note 2 | OK | remote-shahid0 | the screensaver came on ~50 s after the last key while Shahid loaded (45 s setting); one ▼ from the page closed it |

Problems: 1 small (page keys don't act on YouTube's profile picker) + note 1. R4 left.

## step 10, R4 (daytime) — step 10 finished
| # | OK/problem | shots | note |
|---|---|---|---|
| R4 | OK (by design) | r4-adhan | New pairing; YouTube in front; `optimize/music.sh adhan now` → "Adhan playing", our MediaPlayer `state:started` (still playing after 15 s with no key). ▼ from the page STOPS the adhan (playing: 0) — matches «إيقاف الأذان بأي زر» being on; no UrTVRemote line is logged for that ▼ (the key is used to stop the adhan). Started again → page «أوقف الأذان الآن» → stopped. TV sound stayed muted (owner's wish), so checked by playback state, not by ear |
| R5 again | OK | p10-r5d | phone control off again, the PC's pairing gone; service still on |

Step 10 total: 1 small problem (page keys don't act on YouTube's profile picker) + note 1 (TCL blocks the screensaver ConfigProvider after YouTube opens). If ▼ should NOT stop the adhan when it comes from the phone page, that is a second one: owner/supervisor to decide.

## step 11 (apk-next 3.153)
Version: home 3.153, screensaver 3.153 (install.sh all; no !! line). Typing service still on.

| # | OK/problem | note |
|---|---|---|
| P1 | OK | demo city Kuwait, 1 min on the home: next adhan = Kuwait time ok |
| P2 | OK | `am force-stop sa.urtv.screensaver` + YouTube in front, 2 min: Kuwait time ok. The TCL block did NOT happen this time (no `TclAppBoot…ConfigProvider` line, no `UrTVAdhan` line), so the "last good place" path was not exercised. The screensaver started again by itself later |
| P3 | could not run | `am broadcast -a android.intent.action.TIME_SET` from adb is refused: `java.lang.SecurityException: Permission Denial: not allowed to send broadcast android.intent.action.TIME_SET from pid=…, uid=2000`. The tester did not change the TV clock (system setting). Kuwait time ok after it |
| P4 | OK | Home, 1 min: Kuwait time ok |
| P5 | OK | real city put back exactly as the start line, 1 min: the next adhan follows the real city again |
| R4 | done in step 10 | daytime: plays; ▼ from the page stops it (any-key-stop is on); «أوقف الأذان الآن» stops it; phone control off and pairing removed after |
| L1 | pushed | tester/voice-owner/lines-final.txt: the owner's exact text from his ElevenLabs history (newest generation of each line = the audio in voice-owner/). 26 lines. In his text lines 4 and 9 have no «…» and line 12 has «. ..» but is one part. Part boundaries: 01 «شوفوا», 04 «على طول», 07 «مثل», 09 «وأخَفّ», 10 «فيه», 11 «أي», 15 «صار», 17 «بزِرٍّ», 18 «راح», 20 «يتنقّل», 24 «والرجوع», 26 «ركِّب» |
| I1-I4 | pushed | tester/icons/ (see its SOURCE.md) |
| Z | restored: yes | real city, home_panel none, glass + dark, updatechannel latest; service left on; TV sound muted (owner's wish) |

Problems: 0 new. P3 not testable from adb; the TCL block did not happen on its own in P2.

## step 12 (TV's own update 3.153 → build 155 «4.0»)
Start: both apps versionName=3.153 versionCode=153; updatechannel latest → set to next. No new !! line in this step.

| # | OK/problem | shots | note |
|---|---|---|---|
| U1 | OK | u1-update, u1-found | «تحديث تلفزيوني» on 3.153: «الإصدار المثبّت 3.153» → «تحقق من التحديث» → «تحديث جديد 3.155 · اضغط للتحديث» (the old 3.153 app shows the machine number; expected) |
| U2 | OK + note | u2-a…u2-d | two Android screens «Do you want to update this app?»: first «شاشة تلفزيونك», then «تلفزيونك»; focus starts on Cancel, ◀ + OK = Update. Both installed. Note: right after the home app was replaced the TV showed YouTube's profile picker (the last app), not our home; Home brought our home back, and our home is still the default HOME |
| U3 | OK | — | `saver.sh show`: «version: 4.0 (build 155)». sa.urtv.home `versionCode=155` `versionName=4.0`; sa.urtv.screensaver `versionCode=155` `versionName=4.0` |
| U4 | problem (small, text) | u4-tiles, u4-update, u4-check | tile «الجوال والتحديث»: «الإصدار الحالي: ٤٫٠ (بناء ١٥٥)» OK. Update screen: «الإصدار المثبّت» line shows as «(بناء 155) 4.0» (in RTL the build comes before 4.0); after «تحقق من التحديث»: «تلفزيونك محدّث ((بناء 155) 4.0)» — double brackets and the same order. «محدّث» OK; no 3.1xx anywhere a person reads |
| T1 | OK + note | t3-apps | `query-activities … LEANBACK_LAUNCHER`: `sa.urtv.home.HomeTile` label «تلفزيونك», icon and banner set; the banner is visible (in t3-apps). Also listed: `sa.urtv.home.setup.SetupActivity` «إعداد تلفزيوني» with `icon=0x0 banner=0x0` → a second, blank tile in the TV's apps list (intended?). No screenshot of the stock launcher's apps row (no adb intent opens it) |
| T2 | OK | t2-tile, t2-back | `am start -n sa.urtv.home/.HomeTile` → our home (existing task brought to front); Home and Back keep our home |
| T3 | PROBLEM | t3-apps | our own «كل التطبيقات» (scroll down on the home) LISTS the «تلفزيونك UrTV» tile (last row) — it should not |
| Z | restored: yes | — | updatechannel latest, home_panel none, glass + dark; service on; sound muted. Note: `saver.sh show` now says «(newest: 4.0 (build 142))» for the latest channel — build 142 is the old 3.142, but it is named 4.0 |

Problems: 1 (T3) + 1 small text (U4 brackets/order) + notes: blank «إعداد تلفزيوني» tile (T1), YouTube shown right after the update (U2), old build 142 named «4.0» in saver.sh (Z).

## step 13 (TV's own update build 155 → 156, «4.0» alone)
Start: `version: 4.0 (build 155) (newest: 4.0 (build 142))`, updatechannel latest → next. No new !! line.

| # | OK/problem | shots | note |
|---|---|---|---|
| V1 | OK + note | v1-found, v1-b, v1-after | «تحقق من التحديث» found it; Android's two screens (شاشة تلفزيونك, then تلفزيونك): ◀ + OK = Update; both installed. Same note as step 12: right after, YouTube (the last app) was in front, not our home |
| V2 | OK | — | sa.urtv.home `versionCode=156` `versionName=4.0`; sa.urtv.screensaver `versionCode=156` `versionName=4.0`; saver.sh: «version: 4.0 (build 156) (newest: 4.0 (build 156), on the next channel)» |
| V3 | OK | v3-tile, v3-update, v3-check | «الإصدار المثبّت 4.0»; after «تحقق من التحديث»: «تلفزيونك محدّث: 4.0»; tile «الجوال والتحديث»: «الإصدار الحالي: ٤٫٠». No «بناء», no double brackets, no 3.1xx |
| Z | restored: yes (+ note) | — | updatechannel latest, home_panel none, glass + dark; service on; sound muted. saver.sh first line on latest is now just «version: 4.0 (build 156)»: the newest on latest is NOT shown at all (neither «3.142» nor «4.0 (build 142)»). If it should say «3.142», that's a small one |

Problems: 0 (the step 12 text problem is fixed). Notes: YouTube in front right after the self-update; the newest on latest not shown in saver.sh.

## phone page, new look (the first step-14 text, done before step 14 was replaced) — partial, build 159
Update from the TV 157 → 159 OK (both versionName=4.0, versionCode=159). Note: «تحديث جديد 4.0» shown next to «الإصدار المثبّت 4.0» (same name for old and new).
Shots kept locally (text only). Chrome would not go narrower than 500 px without DevTools, so everything was seen at 500 wide, not 390/360.

| # | OK/problem | note |
|---|---|---|
| A11y after self-update | PROBLEM (big) | after the TV updated itself to 159, `accessibility_enabled=0` and `Bound services:{}`: no accessibility service ran (ours and the owner's Key Mapper), so the page's keys and typing did nothing (page got `/api/key` 200, nothing on the TV). The owner re-ran `optimize/dock.sh type-setup` → `accessibility_enabled=1` but still nothing bound; only after `adb reboot` were ours bound («تلفزيونك: إطفاء الشاشة», «تلفزيونك: التحكم والكتابة من الجوال»). Key Mapper still NOT bound after the reboot. SleepService is now in the enabled list (it was not before) |
| Sound | PROBLEM | after the self-update (and again after the reboot) the TV was no longer muted (`Muted: false`); a recitation started from the page played aloud ~8 s before the tester muted it |
| Pairing vs screensaver | PROBLEM (back) | while waiting for the code the screensaver came on and phone control switched itself OFF (page address dead, «تشغيل» shown again): the step-7 problem fixed in 3.148 is back |
| L1 | OK | dark and light: header «التلفزيون · متصل», gold round «اختيار», rounded رجوع/الرئيسية/تشغيل, volume row, field with gold send, «استبدال/إضافة», «بحث». The field is half under the tab bar until you scroll; it scrolls fine |
| L2 | partly | a swipe left on the pad moved the focus left on our home (left is left), but ONE swipe jumped 5 tiles. Tap, hold, Back, Home not done before the step changed |
| L3 | not done | — |
| L4 | OK + notes | play, pause, resume, next, stop all act on the TV. The recitation sleep timer is a dropdown, not chips. Next-adhan card says «بعد 8 ساعة و 56 دقيقة» (should be «ساعات»). «تبدأ تلقائيًا بعد» shows «1 دقيقة» while the TV's screensaver is 45 s (45 s is not in the list) |
| L5 | PROBLEM (back) | card «الصلاة القادمة» saved on the first press (`home_cards sent=1 saved=1`); back to «بدون»: the first press sent NO `/api/set` (home_panel stayed nextprayer), the second press saved (`sent=0 saved=0`). The city search shows a result (not chosen) |
| L6 | could not | 360 not reachable; at 500: `scrollWidth 485 < innerWidth 500`, nothing outside the screen |

Restored: real city exactly, home_panel none; phone control off, the PC's pairing gone; sound muted.

## step 14 (setup without a computer) — started, paused
Start lines: Home holder `sa.urtv.home/.MainActivity`; `adb_enabled=1`; `service.adb.tcp.port=5555` (persist.adb.tcp.port empty) → 5555 is there, so S0 goes on; Android 14; disabled packages 22 (list kept locally); screensaver `sa.urtv.screensaver/.ScreenDream`.
Update from the TV 159 → 160 OK (versionName=4.0, versionCode=160 both).

| # | OK/problem | note |
|---|---|---|
| self-update side effect | PROBLEM (reproduced) | right after the 159 → 160 self-update, again `accessibility_enabled=0` and no bound accessibility service, and the TV unmuted (`Muted: false`). Same as after 157 → 159. Muted again by the tester; a11y left (needs the owner; a reboot brought ours back last time) |
| S0 | OK | «إعداد تلفزيوني» lists 5 steps in order (check device; permissions; «نوقف 25 تطبيقاً من قائمة التطبيقات الآمنة … ونسجّل ما أوقفناه»; screensaver if WebView is enough; make our home Home with a 5-minute safety switch) + how to turn USB debugging on, and «ابدأ» / «تراجع (يرجّع التلفزيون كما كان)» |
| S1–S7 | waiting | every «ابدأ» makes the TV ask «السماح بتصحيح USB؟»; accepting it grants a debugging key, a security permission the tester leaves to the owner, who is not at the TV now. One DPAD_CENTER on «ابدأ» did nothing visible (no prompt, no change). Nothing on the TV changed: Home holder, disabled list and screensaver identical to the start |
| S8 | unknown | PIN state not shown by `dock.sh show`; none set by the tester |

To continue: the owner at the TV to press «السماح دائماً» at each prompt (S1, S2, S5, S6, S7).

## step 14 — closed for now (owner away)
The owner is away and said to skip everything that needs him at the TV: S1–S7 (each «ابدأ» needs him to accept «السماح بتصحيح USB؟», and pressing «ابدأ» from adb was refused on the tester's side) and side task D (he logs into aftv.news himself).
| Z | restored: yes | updatechannel latest; Home holder `sa.urtv.home/.MainActivity` (same as start); disabled packages 22 → 22 (identical list); screensaver `sa.urtv.screensaver/.ScreenDream` (same); `adb_enabled=1` (untouched); real city exact; home_panel none; glass + dark; sound muted. Open: accessibility is OFF since the 160 self-update (`accessibility_enabled=0`, nothing bound) — the owner's Key Mapper and our typing do not work until the owner fixes it (last time a reboot after `dock.sh type-setup` brought ours back, not Key Mapper) |

## side task D (Downloader short codes)
The shortener needs no account (go.aftvnews.com has no login; anyone can submit, and submitted URLs are public). The owner submitted the two URLs himself.

| # | code | short URL | redirect check |
|---|---|---|---|
| D1 | **9378345** | aftv.news/9378345 | `curl -sI https://aftv.news/9378345` → `Location: https://go.aftvnews.com/9378345`; that page (200, HTML) does `window.location = "https://github.com/tshi-dev/UrTV/releases/download/apk-latest/urtv-home.apk"` — exact |
| D2 | **2927902** | aftv.news/2927902 | → `https://go.aftvnews.com/2927902`; `window.location = "https://github.com/tshi-dev/UrTV/releases/download/apk-latest/urtv-screensaver.apk"` — exact |

Note: the redirect is a 301 to go.aftvnews.com and then a JavaScript redirect (not an HTTP Location to GitHub). Downloader resolves codes itself; not tested in Downloader on the TV.

## step 14, S1 — the owner pressed «ابدأ» with an IR remote app on his phone
| # | OK/problem | note |
|---|---|---|
| S1 | PROBLEM (blocker) | «ابدأ» starts: «جارٍ الإعداد… لا تطفئ التلفزيون.» + «• التلفزيون يسأل «السماح بتصحيح USB؟»: اختر «السماح دائماً» بالريموت.» But the TV NEVER shows the prompt. adbd log, 1 s after «ابدأ»: `E adbd : Invalid base64 key` → `prompting user to authorize key` → `adbd_auth: sending prompt with id 170` → nothing on screen (only sa.urtv.home in the window list) → 3 min later `ADB disconnect`. The app then shows «لم تُقبل رسالة «السماح بتصحيح USB؟». اضغط «ابدأ» وافتح الرسالة بالريموت واختر «السماح دائماً».» The key the app sends is not valid base64 for adbd, so Android has nothing to show. Port 5555 is on (`service.adb.tcp.port=5555`). S2–S7 cannot run until this is fixed |
| note | — | the first two OK presses only closed the screensaver (45 s idle) before reaching «ابدأ»; the tester set idle to 30 min for the test and put it back to 45 s after |
| S8 | skipped | no PIN set by the tester |
| Z | restored: yes | nothing changed on the TV: Home `sa.urtv.home/.MainActivity`, disabled 22 = start list, screensaver `.ScreenDream`, screen_off_timeout 45000, adb_enabled 1, updatechannel latest, sound muted. Still open: accessibility off since the 160 self-update |

Problems in step 14: 1 blocker (invalid adb key → no prompt) + the self-update side effects (a11y off, sound on).

## PRIORITY: the TV's own update turns accessibility off (owner's words: "report it")
Owner: when he tried the phone remote at home (before the self-updates) it moved inside apps and typed into YouTube. That matches step 10 (3.152, service on: YouTube search «قرآن» worked, the pad moved inside apps).
What breaks it, seen TWICE: right after «تحديث تلفزيوني» installs a new build (157 → 159, then 159 → 160), Android sets `accessibility_enabled=0` and binds no service (`Bound services:{}`): our «التحكم والكتابة من الجوال» AND the owner's Key Mapper stop. The phone page still answers (`/api/key` 200) but nothing happens on the TV.
- `dock.sh type-setup` puts `accessibility_enabled=1` back, but nothing binds until a reboot.
- After `adb reboot` ours came back; Key Mapper did not.
- Now: ours bound («تلفزيونك: إطفاء الشاشة», «تلفزيونك: التحكم والكتابة من الجوال»); Key Mapper shows «On» in Android's Accessibility screen (listed TWICE there) but is NOT bound; the owner is toggling it off/on.
- Same moment, the TV un-mutes (Muted: false) after each self-update.
Expected: a self-update must leave accessibility (ours and the owner's other services) and the mute exactly as they were. Every user on auto-update loses the phone remote/typing (and their own a11y apps) after each update until a reboot.

## follow-up: Key Mapper and the TCL app-start block
The owner toggled Key Mapper in Android's Accessibility (Enable off → on, «Use Key Mapper?» → OK) with his phone's IR remote. It is back in `enabled_accessibility_services` but still NOT bound. Cause in logcat: `W ActivityManager: [TclAppBoot]:forbid bind service ComponentInfo{io.github.sds100.keymapper/...MyAccessibilityService}` (callerPkgName='android'). TCL's auto-start manager blocks the system from binding a third-party accessibility service; ours bind (allowed), Key Mapper does not. Opening Key Mapper by hand starts its process but the service still is not bound. Key Mapper itself has NO key maps («Create a key map!») and shows «Your phone killed Key Mapper…» and «Disable battery optimization»; the owner chose to leave it as it is.
So the earlier "Key Mapper not bound after reboot" is TCL's block, not our update. Our a11y loss after each self-update (`accessibility_enabled=0`) is still ours to fix (see PRIORITY above).

## state now (end of day)
- Build 160 («4.0») on both apps, updatechannel latest.
- Accessibility: ours bound («تلفزيونك: إطفاء الشاشة», «تلفزيونك: التحكم والكتابة من الجوال»): phone remote and typing work again. Key Mapper enabled but blocked by TCL (owner: leave it).
- Home `sa.urtv.home/.MainActivity`; disabled 22 (start list); screensaver `.ScreenDream`, 45 s; real city; home_panel none; glass + dark; sound muted; phone control off; no PC pairing.
- Open: step 14 S1–S7 blocked by the setup app's invalid adb key (`Invalid base64 key`, no prompt).

Problems found today: self-update turns a11y off + un-mutes (priority); setup app adb key invalid (blocker); «بدون» needs two presses again; pairing stops when the screensaver comes; small texts («8 ساعة», «1 دقيقة» for 45 s); one long swipe jumps 5 tiles.

## REQUEST to the supervisor (owner's words: «اطلب من السبرفايزر الابديت اللي عنده اول»)
Step 15 needs apk-next build 164 or newer; apk-next version.txt still says 3.163 (apk-latest 3.157). The owner asks you to publish your newest update to apk-next first. The tester waits for version.txt ≥ 3.164, then starts step 15 when the owner says «ابدأ الخطوة 15».

## step 15 (build 163) — part 1
Start (build 160): `accessibility_enabled=0` and nothing bound AGAIN (no update since last night: it went off by itself overnight); `AUTO_START: ignore` for sa.urtv.home; `APP_AUTO_START` unknown op on this TV; muted. The owner ran `dock.sh type-setup`, tester rebooted → ours bound.
Note: one extra reboot happened (the tester's `tv.py reboot` kept retrying over a dropped connection and fired twice). Not the app.

| # | OK/problem | note |
|---|---|---|
| F1 | PROBLEM | self-update 160 → 163 OK (both versionName=4.0 versionCode=163). 30 s and 60 s after: NO UrTVA11y line, `accessibility_enabled=0`, nothing bound. Cause: TCL blocked the heal: `TclAppBoot … calleeComponent='sa.urtv.home/.A11yHeal' bootType='broadcast' broadcastAction='android.intent.action.MY_PACKAGE_REPLACED' isAllow='false' reason='callee_does't_have_OP_ASSOC_START_permission'`. Also blocked at the same time: `.tilawah.AdhanReceiver` (MY_PACKAGE_REPLACED — the adhan is not rescheduled after an update!), `.phone.PhoneInstallReceiver`, and `bind_service` for `.TypeService` and `.SleepService`. When our home came to the front (Home): `UrTVA11y: check start: listed=true switch=0 running=false -> heal` / `healed start: switch=1 list kept=true` → `accessibility_enabled=1`, but `Bound services:{}` stays empty (TCL forbids the bind) |
| F2 | PROBLEM | `dock.sh type-setup` by the owner → still nothing bound. Only a reboot binds ours. Key Mapper is never bound (TCL forbid bind service, shown yesterday) |
| F3 | data | the update did NOT unmute (Muted: true after F1). Each reboot DID unmute (twice today: Muted false after boot). TV's behaviour |
| F4–F7 | not yet | need the PC paired (owner's code) |
| F8 | not confirmed | in YouTube, `input keyevent 176` (KEYCODE_SETTINGS) did nothing and logged no UrTVType line; the owner pressed a key on an IR-remote phone app, nothing opened and no key was logged. Needs the real remote |
| F9 | MISSING | no «نظام الساعة» row anywhere in «شاشة التوقف» on 163 (rows: معاينة، الأرقام، الشروق بعد الفجر، شريط الوقت بين الصلاتين، … الرطوبة والرياح، أسماء الأشهر الميلادية، شريط السنة، المناسبات الوطنية، حجم الساعة، … شكل البطاقة، تبديل الصورة كل، صوري، بدء شاشة التوقف بعد ٤٥ ثانية). saver.sh shows `clock24: off` |
| F11 | MISSING | «مركز التحكم» on 163 has 8 tiles (مؤقت النوم، أطفئ الآن، الشبكة، اختبار السرعة، مخرج الصوت، شاشة التوقف، إعدادات تلفزيونك، الإعدادات): no «انعكاس الشاشة» |
| F10, F12 | not yet | — |
| Z | restored: yes | updatechannel latest; real city exact; home_panel none; 45 s; disabled 22; muted; ours bound |

It looks like the new batch (24 h row, mirroring tile, maybe the Settings key) is NOT in build 163. Please publish the build that has it (164+) and I continue F4–F12.

## step 16 (build 174, «4.2»)
Scripts run from a detached worktree of origin/next (the local `next` is 211 behind/ahead after the remote was rewritten; it was not touched).
Before: the TV had updated itself to 4.1 (168) on latest; `accessibility_enabled=0`. Update from the TV («تحديث تلفزيوني», next) 168 → 174: both versionName=4.2 versionCode=174. This update UNMUTED the TV (Muted: false; muted again at once). After it: `UrTVA11y: check replaced: listed=false switch=0 running=true -> not_listed` (the heal now runs on MY_PACKAGE_REPLACED, but our TypeService is no longer in enabled_accessibility_services at all, so nothing to heal) — needs `dock.sh type-setup` by the owner.
Alarms: the next real prayer was hours away and prayeradj is ±20 min, so each alarm used a DEMO city (Moroccan cities whose Maghrib/Isha was 5–25 min ahead: Fès, Meknès, Tanger, Kénitra, Rabat, Casablanca) + prayeradj; the real city put back exactly, prayeradj off, all minutes 0.

| # | OK/problem | adhan_alarm_last / note |
|---|---|---|
| A1 | OK | `front=other play=0 notice=0 saver=0`; `UrTVAdhan: adhan skipped: another app is in front (saver-only is on)` |
| A2 | OK | screensaver showing: `front=dream play=1`; our home: `front=home play=1` (stopped by a key: stop-key on) |
| A3 | OK | adhan OFF, notice on: home idle (screensaver came): `front=dream play=0 notice=1`; app: `front=other play=0 notice=1`, `UrTVAdhan: notice shown: …`. Not seen on screen (the 5 s window was missed). HDMI not tested (no source checked) |
| A4 | OK | short on, then takbir on → `adhan_takbir=on`, `adhan_short=off`. UI row not looked at |
| A5 | OK | `adhan now`: playing at 14 s, stopped at 17 s. Voice-preview part not checked (sound 0) |
| A6 | OK | home: `front=home saver=1` (screensaver started); app: `front=other saver=0` (nothing started) |
| A7 | OK | all four off: `adhan_saver_only=off adhan_notice=off adhan_takbir=off adhan_saver_at=off`; adhan off (as at start) |
| T1 | PROBLEM | on opening «المهام» the focus is on «🎤 تكلّم», NOT the text field. OK on the field opens the TV keyboard; its action key shows ✓ (no «إضافة» label). `input text test` came out «فثسف» (the TV keyboard was on the Arabic layout: adb text goes through it); key 66 added it and emptied the field |
| T2 | OK | «اكتب المهمة أولاً» (toast); nothing added |
| T3 | OK | field ↔ «إضافة» ↔ «تكلّم» all reachable with focus |
| T4 | OK | `am start -a android.speech.action.RECOGNIZE_SPEECH` from adb opens nothing; the in-app «تكلّم» opens Google speech (Arabic SA) and listens («Try again» with no voice). Test task deleted (asks «نعم») |
| K1 | OK (injected) | 4062 → Netflix, 4065 → YouTube, 4099 → Prime Video; `UrTVKeys: key=4062 / 4065 / 4099 scan=0` |
| K2 | CONFLICT (injected) | on our home 4077 and 83 open the TV's settings (`com.android.tv.settings/.MainSettings`), not ours; `UrTVKeys: key=4077 / key=83`. Step 16 expects ours; step 17 H6 says a short press = TV settings — which is right? |
| K3 | OK | `tweaks.sh apply`: no `!!`; `cached_apps_freezer=enabled` before and after |
| K4 | OK | screensaver Arabic lines and card labels joined |
| K5 | OK | disabled list identical to the start (22); no `com.android.sdksandbox` |
| W1 | — | `4 cname=android.uid.system:1000` |
| W2 | OK | Wifi enabled, connected; `list-networks` rows: 2 |
| W3 | PROBLEM | `optimize/install.sh launcher` (default tag) tried to install apk-latest's home over 174: `!! Install of urtv-home.apk failed: Failure [INSTALL_FAILED_VERSION_DOWNGRADE: … 168 is older than current 174]`. With `APK_TAG=apk-next`: «nothing to switch», no `!!` |
| Z | restored: yes | adhan options and on/off as at start; prayeradj off, all 0; real city exact; tasks as at start; disabled 22 = start; Home `sa.urtv.home/.MainActivity`; muted. updatechannel left on next for steps 17–20 (latest at the end) |

Problems: 3 (T1 focus on mic; W3 install.sh launcher downgrade `!!`; the update unmutes + our a11y entry is gone) + K2 needs your answer.

## step 17 (build 174, «4.2»; same install as step 16)
| # | OK/problem | note |
|---|---|---|
| V1 | PROBLEM (small) | «الإصدار الحالي: ٤٫٢» / «الإصدار المتاح: لا يوجد أحدث» — the build number is not shown (the step expects «4.2 and the build») |
| H1 | OK | store app, long OK: under the menu «تفاصيل التطبيق» and «حذف التطبيق» |
| H2 | OK | the TV's app-info page (`AppManagementActivity`); Back returns |
| H3 | OK | Android's «Do you want to uninstall this app?» (focus starts on OK); BACK pressed, not OK; app still there, packages 177 = 177 |
| H4 | OK | system app and «شاشة تلفزيونك»: only «تفاصيل التطبيق», no «حذف التطبيق» |
| H5 | partly | app menu («تعديل التطبيقات، تحديث تلفزيوني، إضافة إلى المفضلة، إعادة المفضلة الافتراضية، الرقابة الأبوية»): no trailing «…». «كل المصادر» and the three search rows not opened |
| H6 | PROBLEM (injected) | short 4077 → the TV's settings (OK). `input keyevent --longpress 4077` → the TV's settings too, not ours. `--duration 1000` is not supported by this TV's `input` (it sent key=0 and key=1000, then 4077). UrTVKeys: `key=4077 scan=0`. Needs the real remote's long press |
| H7 | OK | «البلوتوث · الريموت والملحقات» → `AddAccessoryActivity` («Searching for accessories…»); Back returns; nothing paired |
| H8 | OK | «الرجوع يفتح شاشة التوقف» right after «زر المهام» |
| H9 | OK | one OK → «مفعّل»; Back on our home → screensaver; Back on it → home; `dock.sh backsaver` = on |
| H10 | OK | Back in the apps grid → focus to the favourites, no screensaver |
| H11 | OK | off: Back on the home → nothing; `back-saver: off` |
| H12 | OK | `dock.sh backsaver on` → row «مفعّل»; off → «متوقف» |
| H13 | n/a | no USB prompt came |
| S1 | OK | `verse 3`: card title by minute: 1 حديث, 2–4 آية, 5–7 حديث (changes about every 3 min) |
| S2 | OK | `photos 30s` accepted on this TV (MemTotal 2451284 kB); `photos 1` accepted. The settings row's choices were not opened |
| S3 | OK | photos every minute: 8 one-minute shots, 8 different pictures |
| S4 | OK | `occasion off`: bottom middle empty; prayer and matches unmoved |
| S5 | OK | `nightdim off` → `nightdim: off` (row not opened) |
| S6 | OK | `taskspill off`: the «المهام» button is gone from the top row |
| S7 | OK + small | «تحديث صفحة شاشة التوقف», its line points to «تحديث تلفزيوني» (cut with «..» on screen); «تحديث تلفزيوني» reads «تحديث التطبيقين…». OK on the page row: toast «جارٍ التحقق…», then no «تم التحديث»/«الصفحة محدثة» seen within 3 s |
| S8 | OK | «الإذاعات»: no «البث المباشر من الحرم» row |
| Z | restored: yes | verse day, photos 30, occasion on, nightdim on, taskspill on, backsaver off, slot matches, panel verse, home_panel none — all = start; packages 177; Home `sa.urtv.home/.MainActivity`; muted |

Problems: 2 (V1 no build number; H6 long press opens the TV's settings) + S7 cut line.

## step 18 (from scratch: «جهّز كل شي»), build 174 («4.2»)
Owner was not at the TV; every key and prompt by the tester over adb (the owner gave permission for everything).

| # | OK/problem | note |
|---|---|---|
| 1 | OK | `settings.json` valid, `"schema":1`, 20 keys; all lists saved locally. Photos: 0 of 10 (mode mix); own adhan: none. Disabled 22; Home `sa.urtv.home/.MainActivity`; screensaver `sa.urtv.screensaver/.ScreenDream`; AUTO_START/ASSOC_START allow for both apps; `APP_AUTO_START` unknown op on this TV |
| 2 | OK | `reset.sh` (run with the original run folder, so it had backup.txt / home-switch.json / grants-journal.txt): no `!!`; «removed sa.urtv.home», «removed sa.urtv.screensaver»; stock home in front and HOME role `launcherx`; screensaver `null`; our packages 0; **disabled 22 → 0** |
| 3 | OK | apk-next home: `pm install` Success; versionName 4.2, versionCode 174; screensaver not installed |
| 4 | PROBLEM → 4b | first «جهّز كل شي»: `adbd: E Invalid base64 key` → `sending prompt` → no prompt on screen; after 20 s the screen said «ما ظهرت الرسالة؟ أعد تشغيل التلفزيون…» (good wording). `UrTVSetup: waiting for the debugging prompt: screensaver held back` |
| 4b | OK after 1 restart | `tv.py --reboot` once → «جهّز كل شي» → «Allow USB debugging?» shown; focus on the checkbox → 23 → ticked (screencap); ▼ went to Cancel, ◀ to Allow → 23. `UrTVSetup: debugging prompt answered or given up: screensaver free`. The reboot unmuted the TV (muted again) |
| 5 | OK | plan: «١٠» lines, «نوقف ١٨ تطبيقاً من القائمة الآمنة», «بدون إعادة تشغيل في هذا التلفزيون», «TV Bro و SmartTube مثبّتان من قبل: نتركهما»; disabled still 0 before «ابدأ» |
| 6 | OK + PROBLEM | run ended by itself in ~1 min (report: precheck 0:42, backup 0:01, keep 0:00, before 0:15, grants 0:00, safe 0:09, community 0:00, tweaks 0:03, install-saver 0:08, screensaver 0:01, extras 0:00, autostart 0:02, home 0:09, after 0:13). Every line ✓: فحص التلفزيون، نسخة احتياطية، الميزات المحمية، قياس الذاكرة قبل، أذونات تلفزيونك، القائمة الآمنة، قائمة المجتمع، تسريع التلفزيون، تثبيت شاشة التوقف، تفعيل شاشة التوقف، TV Bro و SmartTube، التشغيل التلقائي، الشاشة الرئيسية، قياس الذاكرة بعد، التقرير. No restart. **Our screensaver showed from ~125 s after «ابدأ»** (at 20 s the stock home was in front, at 49 s our home): that is after the run's end (1 min), so probably not during the run; the watcher could not see the done screen because our home came to the front at the end |
| 7 | no pause | the wizard did not own the network; Wi-Fi stayed connected |
| 8 | OK | done screen: steps, QR, «امسح الرمز بجوالك…», «بعد الإعداد أطفئ التصحيح» (3 steps), «لو صار شي» (4 lines), «تراجع», «تم», «انتهى الإعداد…», «قِسنا الذاكرة بعد الإعداد: ٩٩٤ ميغابايت فاضية». QR read with zxing-cpp (OpenCV could not: very dense, 2238 chars): `https://tshi-dev.github.io/UrTV/share/#…` (page 200). The report in it has NO personal data (TV model, firmware, steps, memory, package lists of what was turned off) |
| 9 | OK | Home `sa.urtv.home/.MainActivity`; screensaver `.ScreenDream`; AUTO_START and ASSOC_START allow for both; TV Bro and SmartTube installed; tweaks: animations 0.5, Play Store RUN_IN_BACKGROUND deny, standby exempted; adhan off (1 alarm of ours). **Disabled 21 = 0 + 18 (plan) + 3 from the home switch** (all «system app»: the stock home, the setup wizard, TCL's own home) |
| 10 | OK | screensaver from the run: 4.1 (168) → apk-next `pm install -r` Success → 4.2 (174); component unchanged |
| 11 | OK + 2 notes | «تراجع»: prompt again did not show (`Invalid base64 key`) → 2nd restart → prompt (a new key fingerprint) → ticked, Allow. After: stock home, screensaver `null`, disabled 0 (= row 2). Note: the undo left `sa.urtv.screensaver` installed (the 2nd plan said «نحدّث شاشة التوقف إن وُجدت نسخة أحدث»). Note: the 2nd «جهّز كل شي» did NOT ask at all: the key from the undo was still authorized (the doc says the key is deleted after setup/undo). 2nd run: all 15 ✓, same home/screensaver/appops, disabled 21 identical to the 1st run |
| 12 | OK | import preview showed 2 (markets style, markets list), applied with `--yes`. Then by command: `watchnext off`, `yearstrip on`, `updatechannel next` (as row 1), dock order (`dock.sh set` with spaces), the 6 locked apps (`pin lock`, no PIN set), family DNS on. `saver.sh show` = row 1; tasks = row 1; adhan = row 1 (off). Differences left: `com.tcl.guard` was disabled before (an older list) and the new setup leaves it on (on purpose: not in tcl-safe); accessibility switch 0 and only Key Mapper in the list (our TypeService was removed with the uninstall) |

Restarts for the prompt: 2 (one for «جهّز كل شي», one for «تراجع»). Lost and accepted: the PIN (there was none to lose except the locked list, restored), photos (0), own adhan (none), phone pairings.

**Owner's request about the QR (his words, Arabic):** the QR design is «too fage» (too big and dense) — please make it better, centred, and use the new design skills he gave the supervisor.

Problems: 3 (the setup app's adb key invalid the first time on each press until a restart; the 2nd run kept an authorized key; the screensaver not removed by «تراجع») + the owner's QR design request.

## step 19 (streams check)
Lists taken from origin/next (d93485c): 11 live stations, 40 reciters, 21 adhan recordings. No Cairo/Kuwait station; no new stream URL found by the grep.

### R1 live stations (from the PC)
HEAD often lies on these servers, so each was also read 3 × 5 s with redirects followed (bytes in 5 s; > 20000 = streams).
| # | HEAD chain / type | ffprobe | 3 × 5 s reads (status:bytes) |
|---|---|---|---|
| L1 radiojar SBA | HEAD 404 (GET: 302 → `http://n0x.radiojar.com/…` plain HTTP) | mp3 / mp3 | 200:112954 200:116070 200:117381 |
| L2 shatri | 200 audio/mpeg | 5XX once | 200:319031 200:318500 200:327223 |
| L3 sudais | 500 audio/mpeg | mp3 | 200:319031 200:323127 200:302440 |
| L4 shuraim | 200 audio/mpeg | mp3 | 200:323127 200:323127 200:319031 |
| L5 maher | 200 audio/mpeg | 5XX once | 200:318452 200:305344 200:314096 |
| L6 alafasi | 500 text/html | mp3 | 200:314093 200:327212 **500:177** |
| L7 ghamdi | 200 audio/mpeg | 5XX once | 200:314080 200:319031 200:314372 |
| L8 dosari | 200 audio/mpeg | mp3 | 200:327223 200:323127 200:327223 |
| L9 abdulbasit | 200 text/html | 5XX once | 200:313965 **500:177 500:177** |
| L10 minshawi | 200 audio/mpeg | mp3 | 200:323127 200:314935 200:289316 |
| L11 ajmy | 200 text/html | mp3 | 200:221282 200:302440 200:317849 |
All 11 stream from the PC, but backup.qurango.net answers 500 (177 bytes) at random (L6 1/3, L9 2/3, and single 5XX in HEAD/ffprobe for L2, L3, L5, L7) — a retry is needed in the app.

### R2 reciters: 40/40 HEAD 200 audio/mpeg (`…/001.mp3`); ffprobe on the first 3: mp3 + cover image. No failure.
### R3 adhan: 21/21 HEAD 200 audio (after archive.org's redirect). (The grep also caught a code comment `https://archive.org/details/…). The file is kept…` — not a recording.)

### R4 on the TV (sound muted the whole time)
- L1 «إذاعة القرآن الكريم - السعودية»: **PROBLEM**. After 30 s: `playing=0 … station_name=إذاعة القرآن الكريم - السعودية … errors=0 last_error= buffering=1` (never starts). Log: `MediaPlayer: setDataSource1 (https://stream.radiojar.com/0tpy1h0kxtzuv)`, `MediaPlayer: getDns: current system don't support uniplayer`, then nothing. Likely cause: radiojar redirects https → **plain http** (`302 http://n05.radiojar.com/…`); Android's MediaPlayer does not follow an https→http redirect (and cleartext may be blocked). No «cannot play» line was shown.
- For comparison L4 (qurango): `playing=1 station_name=سعود الشريم buffering=0 errors=0` within 20 s.

### R5 HaramLive candidates (report only; HaramLive.java untouched)
| which | page | stream | why official | allowed host | live now |
|---|---|---|---|---|---|
| Makkah (قناة القرآن الكريم) | https://www.youtube.com/@SaudiQuranTv/live | YouTube live (today `watch?v=eC4LfEVxvKg`; the `/live` URL follows the current one) | linked from https://www.sba.sa/ footer «القنوات التلفزيونية» | youtube.com ✓ | `isLiveNow: true` |
| Madinah (قناة السنة النبوية) | https://www.youtube.com/@SaudiSunnahTv/live | YouTube live (today `watch?v=Rs7St51oDDc`) | same sba.sa footer | youtube.com ✓ | `isLiveNow: true` |
No `.m3u8` found: www.aloula.sa did not resolve (DNS) from the fetcher; sba.sa links its platform as https://aloula.sba.sa/ (200, needs JavaScript; not on the allowed list as `aloula.sba.sa`). The 30-minute re-check of a token was not needed (no m3u8). Use the channel's `/live` URL rather than a video id (the id changes).

Z: music stopped; sound muted; nothing else changed. restored: yes.
Streams failed: 1 on the TV (L1); qurango intermittent 500.

## step 20 (English), build 174 on both apps
Screens opened with keys by a script (uiautomator dump for focus and text); the texts of each screen saved locally with a shot. Shots NOT pushed (text-only rule for this run).
The owner asked to check especially left-to-right and the app NAMES in English.

| # | OK/problem | note |
|---|---|---|
| E1 | OK (command) | `saver.sh lang en` → `lang: en`; settings and screensaver in English. «Language · English» row exists under More (not pressed) |
| E2 home | PROBLEM | left-to-right, but the focused app's caption is Arabic: «يوتيوب» |
| E2 apps grid | PROBLEM | left to right, but heading «كل التطبيقات» (should be «All apps») and some names stay Arabic: «متجر بلاي», «شاشة تلفزيونك», «يوتيوب», «يوتيوب ميوزك». Other apps keep their own English names |
| E2 Control Center, Tasks | OK | English |
| E2 Tilawah (3 tabs) | PROBLEM | footer stays Arabic: «يمين/يسار بين الأقسام • اختيار للفتح • رجوع للواجهة» (reciter/station names Arabic: allowed) |
| E2 settings hub | OK | «Your TV · Settings», 12 tiles in English, left to right |
| E2 Adhan page | PROBLEM | two Arabic lines: the same footer, and «متوقف • الأذان كاملًا» |
| E2 other settings pages | OK | all 13 pages opened: no Arabic UI words, no trailing «…». «Send a report» not opened |
| E2 screensaver | OK | English, left to right; the hadith text stays Arabic (allowed) |
| E2 «إعداد تلفزيوني» | PROBLEM | the whole done screen stays Arabic in English mode (13 lines) |
| E3 | partly | TV phone-control screen English; page served `<html lang="en" dir="ltr">`, title «Phone control». Not paired (the tester does not type pairing codes) |
| E4 | PROBLEM | settings hub: ▶ right, ◀ left (OK). Favourites row: still right-to-left in English — focus starts far right and ▶ moves LEFT |
| E5 | OK | back to Arabic: every screen's words identical to before; no English left in the UI |
| Z | restored: yes | lang ar; show, dock, backsaver, adhan = start; updatechannel latest; phone control off; Home ours; 45 s; muted |

Problems: 6 (home caption, apps-grid heading and names, Tilawah footer, Adhan page lines, setup done screen, favourites direction).

## QUESTION to the supervisor: do you want the pictures? (owner: «check with the sup if he needs pic you can send»)
Step 20's last line says to push the shots after cleaning them; RULES.md says shots stay on the PC and are never committed. So nothing was pushed. The owner says the tester can send pictures if you need them.
Ready on the PC, if you say yes (each cleaned first: city, prayer times, weather place, phone-page address/code, Wi-Fi name, task text and the owner's photos cropped or blurred):
- step 20, the 6 English problems: home caption «يوتيوب»; apps grid (heading «كل التطبيقات» + Arabic names); Tilawah footer; Adhan page lines; setup done screen in Arabic; favourites row direction
- step 18: the done screen with the QR (for the owner's QR design request); the USB prompt
- step 16 T1 (focus on the mic in «المهام»), step 17 S7
Answer in STEP.md: «pictures: yes» (which ones, and the folder, e.g. tester/shots/) or «pictures: no».

## GO — build 178: steps 16–20 again (re-check of every problem found on 174)
Update from the TV 174 → 178 (both versionName=4.2 versionCode=178). 16–20 were run in full on 174 the day before, and 18 already reinstalled the apps, so on 178 every row that failed on 174 was checked again (16/17 were not re-run in full after 18, as STEP.md says).

| from | problem on 174 | on 178 |
|---|---|---|
| update | accessibility off after a self-update | FIXED: `UrTVA11y: check replaced: listed=true switch=1 running=true -> ok`; ours still bound |
| update | the self-update unmutes the TV | STILL: `Muted: false` right after the update (muted again). Also: after the update the last app (a store video app) opened by itself |
| 16 T1 | focus on the mic when «المهام» opens | FIXED: focus on the text field (EditText) |
| 16 T1 | keyboard action key reads ✓, not «إضافة» | same (TCL keyboard; may be the IME ignoring the label) |
| 16 W3 | `install.sh launcher` tried a downgrade (`!!`) | FIXED: «the TV already has a newer build than apk-latest's urtv-home.apk, so it was kept as it is», no `!!` |
| 16 K2 / 17 H6 | Settings key 4077/83 on our home → the TV's settings; long press too | same: short 4077, `--longpress 4077` and 83 all open `com.android.tv.settings/.MainSettings` (UrTVKeys logs each). Injected keys only |
| 17 V1 | no build number | FIXED: «الإصدار الحالي: ٤٫٢ (١٧٨)» |
| 18 | the prompt does not show on the first «جهّز كل شي» | STILL: `adbd: Invalid base64 key` → no prompt; after 20 s «ما ظهرت الرسالة؟…» (now a longer, clearer text on give-up). Not restarted again (same finding); «تراجع» not re-run |
| 19 R4 | L1 (radiojar) does not play on the TV | STILL: the app now opens the resolved `http://n03.radiojar.com/…` itself (`MediaPlayer: setDataSource1 (http://…)`), but after 30 s `playing=0 buffering=1 errors=0`. Cleartext http probably blocked |
| 20 | home caption Arabic | FIXED: «Hold OK for options» |
| 20 | apps grid heading/names Arabic | FIXED: «All apps», «Play Store», «Your TV Screen», «Prime Video» |
| 20 | Tilawah footer Arabic; Adhan page lines Arabic | FIXED: no Arabic UI words left on Tilawah and its adhan tab |
| 20 | favourites row right-to-left in English | FIXED: focus starts at the left (x 152), ▶ moves right (x 480) |
| 20 | «إعداد تلفزيوني» all Arabic in English | STILL: the whole screen in Arabic |

Restored: lang ar, 45 s, muted, Home ours. Next: step 22, then step 21.

## step 22 (alignment and spacing), build 178
Output: `wm size` physical 3840x2160, override already 1920x1080 (left as is). Method: uiautomator bounds per screen + glyph rows/columns from the screencap where a text box equals its button (local only). Focus-ring pixels not measured; WebView (screensaver) gives no nodes.
Screens measured, Arabic: home (+ below the fold), options box, Control Center, the settings hub and all its pages (adhan, location, clock, Quran & adhan, cards → screensaver + home, appearance, control center, phone control, update, more → matches & markets, apps & favourites, new episodes, parental, site filter, language), tasks, Tilawah + adhan, screensaver seconds on/off, setup; English: home, options, Control Center, hub + every page, Tilawah + adhan; the 5 looks (+ sand light) on home and options; football layout.

| # | result | numbers |
|---|---|---|
| G1 home | PROBLEM | favourites gap 20 px (not on the 8 px scale); below the fold OK; EN same |
| G2 options box | OK | glyphs 17–22 px from the button edges (bounds said 0: the text box fills the button); EN same |
| G3 Control Center | PROBLEM | tile gap 20 px; EN same |
| G4 hub and pages | PROBLEM | the footer hint («الأسهم بين الأقسام…» / «أعلى/أسفل بين الصفوف…», EN «Arrows to move…» / «Up/Down to move…»): box y 974–1080, glyphs end at **y 1037** → outside the 5% safe area (≤1026) on every settings page; list rows: gap 18 px (Screensaver, Update pages: column gaps [126, 18, 18]); «تحكم من الجوال» title top y 40 (<54); «يناير، فبراير، مارس…» / «Standard (January, February…» end with «…» (is it on purpose?) |
| G5 parental | PROBLEM | PIN pad: gaps 20 and 24 (unequal, 20 off-scale). Card «off» page: gap 20 |
| G6 update | OK | «الإصدار الحالي: ٤٫٢ (١٧٨)» / «Current version: 4.2 (178)» not cut |
| G7 tasks | OK (page) | reminder page/cards not tested |
| G8 Tilawah + adhan | PROBLEM | row gap 20; footer «يمين/يسار بين الأقسام…» box ends y 1032 (>1026); EN same |
| G9 football | OK | tray top 808 → 848 with the focus on the matches widget (40 px) and back to 808; home gap 20 as G1 |
| G10, G11 | not done | matches list/detail and markets screens not reached by keys in this run |
| G12 screensaver | not measurable | WebView: no nodes; seconds on/off shots kept locally |
| G13 setup | partly | the done screen only shows after a run; the start screen: column gap 20 |
| G14 looks | OK | glass, graphite, navy, clean, sand (dark and light): every edge and gap identical across looks |
| Z | restored: yes | look glass, appearance dark, lang ar, seconds off, homelayout classic, cards and markets, dock = start; screensaver delay back to 45 s; updatechannel latest; Home ours; muted |

Worst 10: 1) settings footer glyphs to y 1037 (all settings pages); 2) Tilawah/adhan footer to y 1032; 3) favourites gap 20; 4) Control Center gap 20; 5) Tilawah rows gap 20; 6) PIN pad gaps 20/24; 7) settings list rows gap 18; 8) phone-control title at y 40; 9) month line ending in «…»; 10) setup screen gap 20. No crops pushed (pictures: no).

## step 21 (PIN recovery code), build 178
Before: no PIN set; the locked list (6 apps), kids mode off, wrong tries 0. **temporary PIN** set for this step only, removed at the end. The code is in no file (checked: 0 files).
| # | result |
|---|---|
| 1 | worked (178 on both apps; status kept; temporary PIN) |
| 2 | worked: the code typed in the parental PIN prompt with one `input text` → «رمز جديد» («اختر رمزاً من أربعة أرقام») opened; then «تأكيد الرمز»; new PIN set. The 4-dot field during typing was not captured (no dump while the code was in the field) |
| 3 | worked: locked list, kids mode the same; wrong tries 0 after the code (the code is not a wrong try); the new PIN opens the prompt |
| 4 | worked: a wrong 4-digit PIN → stays on «أدخل الرمز»; wrong tries 1 |
| 5 | worked: a wrong 8-digit code → the same; wrong tries 2 |
| 6 | worked: the new PIN once → wrong tries 0; `pin off` → no PIN, the same 6 locked apps, kids off, 0 — as at the start |
Problems: 0.

## step 23 (re-check of 178's problems on 179), build 179 (next b202ae3)
Start: Home ours, `wm size` override 1920x1080, muted, no PIN, lang ar, homelayout classic, no home card.

| # | result | numbers / note |
|---|---|---|
| 1 update 178 → 179 | PROBLEM | both apps versionCode=179; settings «الإصدار الحالي: ٤٫٢ (١٧٩)». Mute after the install: **4 s Muted:false, 11 s Muted:false, 30 s Muted:false** (muted again by the tester). **A store app (YouTube) opened by itself** after the update and was in front at all three reads. The owner, at home, heard music at that moment |
| 2 radiojar L1 | FIXED | `playing=1 … buffering=0 errors=0 rebuffers=0`; MediaPlayer: `notify() prepared` → `start` → `NuPlayer: media rendering started` → `Received media started message`; no error. (The owner thought it was music: checked from the PC, a 30 s sample of the stream is Arabic speech about Quran teaching; icy-name is the server's «Mini2's Broadcast». What he heard was YouTube, row 1) |
| 3 setup in English | FIXED (start screen) | «Your TV setup», «Set everything up», «Undo (puts the TV back as it was)», left to right, no Arabic, no «…». Plan/progress/done screens: not reachable without a run |
| 5 matches path | OK | from «الأذان» ▼×5 → «أرسل تقرير», ◀ → «المزيد», OK → focus «المباريات والأسواق», OK → focus «مباريات اليوم», OK → matches page (focus «المباريات»); OK → a match's detail; Back → «مباريات اليوم», ▼×4 → «صفحة الأسواق», OK → markets (focus «السوق»). Home: stocks card (temporary `home-card stocks`), ▲ once onto it (gold ring), OK → `MarketsActivity`. Football layout: ▲ onto the games widget, OK → `MatchesActivity` |
| G10 | OK | matches list: no problem found; detail: no problem found |
| G11 | OK + notes | market tab: the «الأكثر ارتفاعًا وانخفاضًا» list starts at y 1032+ (below the fold, scrolls; first row under the safe area); watchlist rows 152 px tall, text 16 px from top/bottom (24 needed for a box > 600 px wide). Holdings: 1 share of أرامكو at a made-up cost (form: «الكمية», «متوسط التكلفة», «احفظ»); «أخفِ المبالغ» hides value, profit, quantity and cost («••••»; percentages and the share price stay). Note: pressing «أخفِ/أظهر المبالغ» jumps the tabs to «السوق» (the portfolio tab is left). The stock card (range, open, day range, «عدّل/احذف/أظهره في شريط الأسواق»): no problem found. Holding deleted («أحذف «أرامكو» من محفظتك؟» → «نعم»); «لا أسهم في محفظتك بعد» |
| football widget | small | the games card cuts «…FC Champions League Elite» and «شباب الأه…» |
| Z | restored: yes | lang ar, homelayout classic, no home card, show and dock = start, no PIN (temporary 1234 for the PIN-pad measure, then `pin off`), wrong tries 0, 45 s, updatechannel latest, Home ours, muted |

### worst 10: 178 → 179 (lang ar)
| # | item | 178 | 179 |
|---|---|---|---|
| 1 | settings footer glyph bottom (every settings page) | 1037 | **1002** ✓ (adhan page 1016 ✓) |
| 2 | Tilawah/adhan footer glyph bottom | 1032 | **1023** ✓ |
| 3 | favourites gap | 20 | **20** ✗ |
| 4 | Control Center tile gap | 20 | **no gap problem** ✓ |
| 5 | Tilawah rows gap | 20 | **20** ✗ (row text 1 px from the row's bottom by bounds) |
| 6 | PIN pad gaps (keys row 1 / rows 2–3 / between rows) | 20/24 | **20, 24 / 24, 24 / 24** ✗ (row 1 still 20 then 24) |
| 7 | settings rows gap | 18 | **16** ✓ (but the first gap after the header is 124: off the scale) |
| 8 | phone-control title top | 40 | **box 56, glyphs 80** ✓ |
| 9 | month line «…» | «يناير، فبراير، مارس…» | **«يناير، فبراير، مارس»** ✓ |
| 10 | setup screen gap | 20 | **no problem** ✓ |
Parental card: no problem found.

Problems left: 4 (update unmutes + opens an app; favourites gap 20; Tilawah rows gap 20; PIN pad first row 20) + small: «أخفِ المبالغ» jumps to «السوق», the games card cuts names, the 124 px first gap.

## step 23 addendum: the favourites bar (owner: «the fav bar was not set right»)
Measured on 179, lang ar, home (bounds):
- tray `[120,808][1800,1026]`; tiles 304×170: `[1496..1800]`, `[1168..1472]`, `[840..1144]`, `[512..816]`, `[184..488]`, and a 6th tile cut to **40 px** at `[120..160]` (7 favourites, 5 and a sliver shown).
- the first tile (focused) touches the tray's right edge: **0 px** inside, while top and bottom are 24 px (832−808, 1026−1002). Not centred; the 6th tile is cut by the tray's left edge.
- tile gaps are 24 (equal, on the scale).
- tray bottom 1026 = the safe-area edge exactly.
Correction to steps 22/23: the «gap 20» on the home is NOT the favourites, it is the four round buttons at the top (`[116..184]`, `[204..272]`, `[292..360]`, `[380..448]`: gaps 20).
Expected: tiles inside the tray with the same inset on all sides (24 px), no half tile at the edge (fit whole tiles, or fade the edge), the tray inside the safe area.

## step 26 (final test of 4.2), build 180 — parts A and B (D and C follow)
Start: Home ours, `wm size` override 1920x1080, muted, no PIN, lang ar, homelayout classic, no home card, updatechannel set to next for the update.
Note before part A: found **Muted:false** once at a read before the log was running (after `am force-stop sa.urtv.home`, HOME, opening settings); muted again at once. Repeated the same three actions with a log: stayed muted. Cause not caught.

### Part A, the update 179 → 180 (the app's own «تحديث تلفزيوني», not adb)
Timeline (t=0 log start):
- t+94: «تحديث جديد 4.2 · اضغط للتحديث» pressed; `UrTVUpdate: before the install: muted=true ours listed=false`; installer status=-1 → Android's confirm screen (screensaver app first)
- t+140: screensaver installed (PACKAGE_REPLACED, MY_PACKAGE_REPLACED to sa.urtv.screensaver), status=0; second confirm screen (home app)
- t+163: home installed: `Force stopping sa.urtv.home … installPackageLI` → `Force removing ActivityRecord … UpdateActivity: app died, no saved state`
- t+163: **`am_proc_start … org.jellyfin.androidtv, top-activity`** → Jellyfin's MainActivity resumed. No `START u0` for it: Android resumed the next task in the recents stack when our task died
- t+165: our process back only for `AdhanReceiver` (MY_PACKAGE_REPLACED); our home was **not** brought back to front
- t+174: `UrTVA11y: check replaced: listed=false switch=0 running=true -> not_listed`; `UrTVUpdate: installer answered: status=0`
- t+168 … t+497: Jellyfin stays in front until a key is pressed; muted at every 2 s line; no media session playing

| # | result | note |
|---|---|---|
| A1 muted | OK | `Muted:true` at all 2 s lines (t+0 … t+497); 0 setStreamMute/adjustStreamVolume/setMasterMute lines in the log |
| A2 no app opens by itself | **PROBLEM** | Jellyfin came to the front at t+163 (last-used app's task). Same mechanism as YouTube on 179: when the home package is replaced, its UpdateActivity task dies and Android resumes the next task. Our home is not started again after the install |
| A3 UpdateCarry | **PROBLEM** | no `UpdateCarry` line at all in the log; the only UrTVUpdate lines are «before the install: muted=true ours listed=false» and «installer answered: status=-1/0» |

who unmuted: nobody in this run (muted throughout).
who launched an app: Android's task stack, not a START from anyone: our home's task was force-removed by the install (`installPackageLI`) and the previous app's task (Jellyfin) resumed as `top-activity`. Suggest: after MY_PACKAGE_REPLACED, start our MainActivity (or HOME) when the update was started from UpdateActivity.

### Part B, the open items (1920x1080, dump bounds; Arabic, then English)
| # | result | numbers |
|---|---|---|
| B1 favourites tray | **PROBLEM** | tray [128,768][1792,986] ✓ (y 768–986); tiles 304x170, insets 24 top/bottom/right/left ✓, gaps 24 ✓. But **the 6th tile (Spotify) is drawn outside the tray**: in Arabic at x 0–128 left of the tray, in English at x 1792–1920 right of it (dump bounds clip it to 0 width, the screen shows it). **Lowered state: none**: with a home card (temporary `home-card nextprayer`) and the focus on it, the tray stays at 768–986 (expected 808–1026). The focused tile grows to ~[1452,780][1780,964], 12 px from the tray edge. **The app's name label moves**: it sits above whichever tile has focus («يوتيوب» at x 1708, «شاهد» at x ~735); the owner asks that it stay in one place |
| B2 round buttons | OK | [120..188] [212..280] [304..372] [396..464]: gaps 24, 24, 24 |
| B3 «تلاوة وأذان» | half | rows 414–614, 630–830, 846–950: gaps 16, 16 ✓; footer bottom 1024 ✓. **Tabs gap 20, 20** (الأذان 1083–1291, الإذاعات 1311–1559, القرّاء 1579–1776): off the scale |
| B4 PIN pad | OK | keys 218–219 wide, gaps 24/24 across, 24 between rows, «إلغاء» 24 below; nothing cut (temporary 1234, `pin off` right after; wrong tries 0) |
| B5 settings hub | OK | title box bottom 230 → first row 262 (32); rows 472→496, 706→730: 24, 24. The 124 gap is gone |
| B6 «الأسواق» | small | cards: content 40 px from each side (TASI [745..1824]/[785..1784], trend [96..713]/[136..673]) ✓ equal. Arabic: the change shows as «−(٪٠٫٣٣−)» with the minus signs on the wrong side (bidi). English: «Market direction» wraps to 2 lines and «10:00 AM» wraps («10:00 / AM»), so that card ends at 878 vs 858 for TASI |
| B7 «أخفِ المبالغ» | in part D | (needs the made-up holdings) |
| B8 football names | OK | Arabic «الهلال × شباب الأهلي» full; English «Al-Hilal × Shabab Al-Ahli Dubai», no «…», but the long name wraps onto 2 lines («Shabab Al- / Ahli Dubai») |
| B9 setup language switch | **PROBLEM** | switches both ways at once ✓. **The two switch labels are cut in half** (box [240,191,560,279] / [584,191,904,279], 88 px; only the top of «English»/«العربية» shows). **After OK on the switch the focus jumps to «جهّز كل شي» / «Set everything up»**: a second OK would start the run |
| B10 «مباشر» | small | «مباشر | عادي» / «Live | Normal» shown, not cut, right in both. In Live mode it says «آخر تحديث ١:١٩ م · أسعار متأخرة» / «Updated 1:19 PM · delayed prices» 27 min later: prices did not move; the reason given is «delayed prices» |

## step 26 — part D (marketing screenshots) and B7, build 180
Demo-clean: city «مكة المكرمة», method auto (hub: «مكة المكرمة · تلقائي · أم القرى»); own photos: 0 (built-in only); owner's tasks at start: none; demo tasks 2; dock set to YouTube, Netflix, Shahid (the dock script kept Apple TV and Crunchyroll too); holdings أرامكو 50 and الراجحي 20 (made-up costs). **Apple 5: skipped**: the TV's «أضف سهمًا» list has only the 30 Tadawul shares («أسهل من الجوال» for others). No PIN, code, IP, serial or toast on any shot; the Wi-Fi name on 03 blurred.
Images: 29 marketing + 12 part B shots, checked, on the PC (real city, weather, prayer times and Wi-Fi name blurred). **Not pushed yet**: pushing images to this public repo was stopped on the PC side; waiting for the owner's OK.
- ar-sand 01–12, 14a–c, 15, 16: done. **13 skipped**: OK on the match detail landed on «ذكّرني قبلها بنصف ساعة» and added a task («أضيف إلى المهام»); after that task was deleted, «المباريات» showed **«لا مباريات اليوم»** twice while the football widget still showed today's match
- ar-glass 01, 04, 11, 14a–c and en-glass 01, 04, 11, 14a–c: done

| # | result | note |
|---|---|---|
| B7 «أخفِ المبالغ» | OK | on → value, profit, quantity, cost «••••» (the share price stays); stays on after switching tabs, after Back + reopen, after Home + reopen; the focus stays on «أظهر المبالغ» (no jump to «السوق» any more) |

New problems seen in the shots:
- **«الذهب والنفط» card on «قائمتي»: names drawn one letter per line** (a very narrow text column: «ا / ل / ذ / ه / ب …», «G / o / l / d …»), both languages, both looks
- **the app-name label above the favourites is not tied to the focused tile**: in Arabic with the focus on YouTube (rightmost) «يوتيوب» shows above the leftmost tile (ar-sand-01, ar-glass-01); other times it follows the focus. The owner asks for one fixed place for the name
- the dock in Arabic shows Downloader where English shows Apple TV, same dock setting
- after saving a holding the focus jumps to «مباشر» at the top
- matches page «لا مباريات اليوم» while the widget shows a match (above)

### step 26 — Z (before part C), build 180
restored: yes. Settings imported from the start file (only the city differed), seconds off, lang ar, homelayout classic, no home card; the 2 demo tasks and the match-reminder task deleted (tasks: none, as at the start); the made-up holdings deleted («احذف المحفظة»: the owner had none); dock order = start; no PIN (temporary 1234 for B4 and shot 07, `pin off` each time, wrong tries 0); `wm size` unchanged (override 1920x1080); updatechannel latest; Home ours; YouTube force-stopped; the TV-side log file removed; muted.
Part C: waiting for the owner to give the recovery code again in chat (this session restarted and holds no code). Problems in 26 so far: A2, A3, B1 (tile outside the tray, no lowered state, moving name label), B3 tabs 20, B9 (cut labels, focus jumps to «جهّز كل شي»), the «الذهب والنفط» letters, the empty matches page; small: B6 bidi minus, English wraps, B10 prices not moving in «مباشر», Arabic/English dock difference, focus to «مباشر» after a save.

### step 26 — part C (PIN recovery code, step 21 rows 1–6), build 180
The owner gave the code again in chat. Temporary PIN for this part (no PIN at the start).
| # | result |
|---|---|
| 1 prep | worked: both apps 180; `pin status` saved; temporary PIN set |
| 2 the code | worked: one `input text` command; «رمز جديد» opened; new PIN set and confirmed («تأكيد الرمز») |
| 3 nothing else changed | worked: `pin status` same as row 1 (locked list, kids mode off, wrong tries 0); the new PIN opens «الرقابة الأبوية» |
| 4 wrong 4 digits | worked: «أدخل الرمز» again, wrong tries 0 → 1 (and the code at row 2 did not count: 0 → 0) |
| 5 wrong long code | worked: the same, wrong tries 1 → 2 |
| 6 back as it was | worked: the new PIN once → wrong tries 0; `pin off`; `pin status` identical to the start (no PIN, same locked list, kids mode off, 0) |
Not checked: the dots while the code was in the field (no dump allowed then).
A file on the tester's PC (an old step-21 push script in a temp folder) was found holding the code and deleted; the code is in no branch or commit (`git log --all -S` empty).

### step 26 — final
restored: yes (as in Z above; PIN not set, updatechannel latest, Home ours, muted). Pictures: the owner shares them with the supervisor himself as a zip (29 marketing + 12 measurement shots, blurred where needed).

## step 27 (re-check after the fixes), build 181

Start: at 19:13 (before part A) the TV was found **unmuted** (Muted: false); muted at once (keyevent 164). Not caused by a step-27 action (nothing had run yet but saver.sh show / export / dock.sh show / tasks / photos / pin status).

### Part A, the update («تحديث تلفزيوني», channel next, 180 → 181)
t0 = logcat start (HOME, YouTube force-stopped, muted). Timeline:
- t+13: settings open; t+60: «تحديث تلفزيوني» (UpdateActivity), installed 4.2 (180)
- t+77: «تحقق من التحديث» → «تحديث جديد 4.2 · اضغط للتحديث» (the build number is not shown, only 4.2)
- t+106: pressed → the screensaver's install question (focus on Cancel at first); t+146 Update
- t+151: screensaver installed (181)
- t+153: the home app's install question opened by itself right after; it waited for a key
- t+198: our screensaver (idle 45 s) started OVER the install question; t+250 woke it, the question still there
- t+287: Update; t+289 installPackageLI kills sa.urtv.home
- t+290: **org.jellyfin.androidtv MainActivity restarted and RESUMED** (Android's previous task, "top-activity"), about 4 s
- t+293: carry step armed, HOME intent sent; t+294: home_in_front; then our UpdateActivity restarted, «تم التحديث ✓ 4.2»
- t+294 … t+362: UpdateActivity in front, muted at every line, nothing playing

| # | result | note |
|---|---|---|
| A1 muted | OK | `Muted:true` at every 2 s line from t+0 to t+362, during and after both installs |
| A2 our home in front | **PROBLEM** | at t+290 (home install, `installPackageLI` kills sa.urtv.home at t+289) **org.jellyfin.androidtv MainActivity was restarted and resumed** (`am_proc_start … top-activity`) for ~4 s, until our HOME intent at t+293. After it our **UpdateActivity** («تم التحديث ✓») is in front, not `MainActivity`. Also: the home app's install question opened right after the screensaver's and waited for a key; our screensaver started over it at t+198 |
| A3 carry lines | OK (all stages logged) | `carry stage=before gen=… result=recorded muted=true ours=false` (old process, before the install); `stage=step result=armed why=replaced`; `stage=front result=home_intent_sent why=replaced attempt=1`; `stage=audio result=already why=replaced`; `stage=a11y result=no_a11y_entry why=replaced`; `stage=front result=home_in_front why=start`; `stage=audio result=already why=start`; then `stage=audio result=already` with `why=replaced`, `why=start`, `why=alarm` ×2. Note: `ours=false` at «before» |

### Part B
- B1 favourites tray (7 favourites): tray [128,768][1792,986]; tiles 304x170, insets 24, gaps 24; the row scrolls inside the tray, no tile drawn outside it at either end (screencap checked, AR and EN). Name label: AR right edge 1768 / top 686 for all 7 tiles (left edge follows the text width); EN left edge 152 / top 686 for all 7. OK
- B2 «تلاوة وأذان» tabs: [1579-1776] [1307-1555] [1075-1283] → gaps 24, 24. OK (step 26: 20)
- B3 markets:
  - AR «السوق» header line «⁦−٦٫٧٢  (−٠٫٠٧٪)⁩» (text has LRI…PDI) is DRAWN as «−(٪٠٫٠٧−)  ٦٫٧٢» (left to right on screen): the value's minus goes to the far left end, and inside the brackets the % is before the digits and the minus after them («٪٠٫٠٧−»). ✗ (the single values in «قائمتي» and the gold card draw right: «−٠٫٧٧٪»)
  - EN: no wrapped label, no time split (Tadawul line, «3:00 PM · closing auction to 3:10 PM», «Updated 7:01 PM · delayed prices» all one line). OK
  - «الذهب والنفط» card: names read normally (box 537 wide). OK. Small: the sparkline starts right at the price's end (no gap: «٤٧٥٫٧٩ ر.س» ends x=247, the line touches it)
  - after saving a made-up holding (أرامكو, 10 @ 25) the focus lands on «أخفِ المبالغ», not on the new row or «أضف سهمًا». ✗ (step 26: «مباشر»)
- B4 «إعداد تلفزيوني» switch: boxes 320x115 ([1360,191,1680,306] / [1016,191,1336,306]); both labels fully drawn, about 37 px above and below the glyphs, 80+ px at the sides. OK on «English» → the screen turns English, **focus stays on «English»** ([584,191,904,306]); back to «العربية» the same. Global lang stays ar. OK
- B5 live «مباشر»: Tadawul closed (opens Wednesday 10:00; tested 19:25–19:28 Saudi time, US open but the markets page has no US rows). Live on, 2 minutes: header stays «آخر تحديث ٧:٠١ م · أسعار متأخرة» (honest: not «live»). UrTVMarkets (2 lines in 2 min): `live status=200 asked=11 parsed=0 bytes=1400 reason=parsed_none` ×2: **the live source answers but nothing is parsed**, so even in market hours nothing would move. ✗ (market closed for Saudi; check again in hours). Also: our screensaver starts over the markets page after the 45 s idle (live mode does not keep the screen awake)
- B6 «المباريات» vs football layout: widget «مباريات اليوم · مباراة واحدة: الهلال × شباب الأهلي ٩:١٥ م»; OK on the widget opens «المباريات» with that match's detail; the list behind shows the same match (no «لا مباريات اليوم»). On the detail **no visible focus at all** (dump focus = the window, neither button ringed); one ◀ puts the focus ring on «ذكّرني قبلها بنصف ساعة» (the right button, not «تابع» on the left), ▶ removes it again. Tasks: «no tasks» before and after (nothing added). OK on the matches, small on the focus
- B7 dock AR vs EN: AR (right→left) and EN (left→right) both YouTube, Crunchyroll, Netflix, Shahid, Apple TV, Spotify, MovieBoxPro = the dock setting. OK
- B8 «صرف العملات» (the tab is called «صرف العملات»): default 15 rows, USD first (٣٫٧٥), then KWD ١٢٫١٧, AED/QAR/BHD/OMR/JOD «مربوط بالدولار», EGP, EUR, GBP, TRY, INR, PKR; header «الأسعار مقابل الريال السعودي · آخر تحديث ٧:٠١ م»; footer «أسعار يومية · المصدر: Yahoo Finance…». Converter: 100 USD = **٣٧٥٫٠٠ SAR** ✓; 1 KWD = **١٢٫١٧ SAR**; 375 SAR = **١٠٠٫٠٠ USD** ✓. Extras (EGP/INR/EUR/GBP are already default, so): MAD ٠٫٣٨٢١, PHP ٠٫٠٥٩١, JPY ٠٫٠٢٣٧ → all appear, **no «—»**. OK, with 3 small problems in «اختر العملات»: (1) **the «تم» button has no visible label** (box [384,960,1536,1024], 64 px, empty pill focused or not); (2) **no tick/mark on the chosen currencies** (cannot see which are on); (3) **after «تم» the focus disappears** (no focused node until a ▼)
- B9 quick measures vs step 26: round buttons [120..188][212..280][304..372][396..464] gaps 24; settings hub rows 262→472, 496→706, 730→940 (gaps 24, 24); footer hint ends 1026; PIN pad (temporary 1234, `pin off` right after; now «pin: not set», wrong tries 0) keys 218 wide, gaps 24 across and down, «إلغاء» 24 below. **No regression**

### Part E, exploratory (log: `logcat -b all` to the TV from the start of E; network test skipped: adb is over Wi-Fi (tv.py ip:port), turning Wi-Fi off would cut it)
Own tests (what / why / result):
1. Back ×10 fast on «الأسواق» (after ▼▼▶▶ OK) / Back must always lead out / ends on our home, focus visible. OK
2. `lang en` while «الأسواق» is open / mid-screen language / the open page stays Arabic until it is reopened; no crash. Small: the page does not follow the language until reopened
3. Converter pad odd inputs / numbers pads / ⌫ on empty, «٠٠٠», 20× «٩», 3× «٫», ⌫×26, «٫٥»: input caps at 12 digits (٩٩٩٩٩٩٩٩٩٩٩٩ SAR = ٢٦٦٬٦٦٦٬٦٦٦٬٦٦٦ USD, right), extra dots ignored, «٫٥» → ٠٫٥. Small: 0 shows «= ٠٫٠٠٠٠٠٠ USD» (six decimals), and the amount line has no thousands separators while the result has them
4. HOME in the middle of the converter, then reopen «الأسواق» / Home mid-dialog / home in front, reopens clean on «السوق». OK
5. Open/close «الأسواق» 5× in about 3 s (am start + Back/Home) / lifecycle races / no crash, ends on markets with focus. OK
6. Holding pad: quantity 0 → «احفظ»; 15× «٩»; «احفظ» with no cost / odd inputs / quantity caps at 10 digits; **«احفظ» with quantity 0 or no cost does nothing and says nothing** (no hint what is missing). Small
7. 12 tasks by `saver.sh tasks add test`, then a 13th / the limit / the 13th is refused by the command («at most 12»). OK
8. The 13th from the TV («المهام», text field, `input text test`, Enter) / the limit on screen / **nothing happens and nothing is said**: the text stays in the field, no «12 at most» message. Small. Also the «المهام» list runs below the safe area (rows at y 920–1031 and 1047–1080; it scrolls, fine) and the list's row gaps are 16 then 20 (not equal)
9. 10 favourites (added Prime Video, OSN+, VLC) / many favourites / the row scrolls inside the tray to the 10th (VLC), label stays at the right edge 1768; dock put back to the 7 right after. OK
10. EN + football layout + look sand + seconds on + 24-hour + Hindi digits / rare combination / home draws (clock «٢٠:٢٣», Hindi digits inside English text, prayer times 24-hour). Small: «Shabab Al- / Ahli Dubai» still wraps to 2 lines in the matches card
11. Long screensaver run (home idle, the combination of test 10) / 20 minutes then wake / screensaver (ours) at 1 and 10 min, muted; at ~19 min **the TV went to sleep by itself** (its own `sleep_timeout` = 1139000 ms, not ours); waking it (KEYCODE_WAKEUP) brings our home back, no crash. **On wake the TV was UNMUTED**: at 20:43:55 the TV's own audio service sets `speaker_mute=false` with SCREEN_ON (`STREAM_MUTE_CHANGED muted=false`), no line of ours involved; muted again at 20:44:11. The unmute found at 19:13 before part A is very likely the same (the TV had been asleep). This is the TV's behaviour, but it means a muted TV comes back unmuted after its own sleep
12. «ذكّرني» on the match detail: home (football) → ▲ (widget «مباريات اليوم») → OK → detail → ◀ → OK / reminders / after ◀ **the focus ring is drawn on «ذكّرني» (right button), but OK acted on «تابع شباب الأهلي» (left button)**: the team was added to the followed teams («تتابع شباب الأهلي» shown, the «ذكّرني» button then disappears); no task added. The visible focus is on the wrong button. Repeat: not repeated (it changes the owner's teams; put back in Z by the import). Screen: «المباريات» match detail
13. Long OK on a favourite tile / options box / 1st try (right after HOME, focus on YouTube): **YouTube opened instead of the menu** (force-stopped after); 2nd try (Crunchyroll): the box opens right («انقل», «أخفِ», «اقفل», ⓘ, ★). Repeat: no
14. «الرقابة الأبوية» with no PIN, ▼×10 ▲×10, Back×10 fast / focus and Back / focus stays on «فعّل الرقابة الأبوية», Back×10 lands on our home. OK
15. «إعداد تلفزيوني»: ▼▼▲▲▲◀▶ fast then Back; again then HOME (never OK) / the setup screen / leaves cleanly to our home. OK
16. Restart (`tv.py --reboot`) with «الأسواق» open / restart mid-page / back in ~1 min; our home holds Home; 45 s later our screensaver; wake → our home (the page is not reopened, normal). **The TV came back UNMUTED** (muted again at 20:50:40) — the TV's own start-up, as in test 11
17. Matches day tabs and the setup switch were covered in B; settings hub navigation in B9 (no new finding)
- TV system language and time zone change: **skipped** (changing the TV's own system settings is outside what I do on the owner's TV; nothing else was changed in the TV's settings)
- Network gone for a minute: **skipped: adb over network** (tv.py over Wi-Fi, ip:port)

| # | result |
|---|---|
| E1 | OK: `logcat -b crash` has no `sa.urtv` line (checked after each test and after the restart); no `ANR in sa.urtv` in explore.log (243 k lines). One error: `WindowLeaked: Activity sa.urtv.home.MarketsActivity has leaked window … MarketsActivity.converter` ×2, when HOME is pressed with the converter open (test 4): the dialog is not dismissed when the page goes |
| E2 | **PROBLEM**: the focus disappears after «تم» in «اختر العملات» (B8) and on the match detail when it opens (B6); on the match detail the ring is on the wrong button (test 12). Back always led out; Home always returned to our home |
| E3 | small: the «تم» button label not drawn (B8); «Shabab Al- / Ahli Dubai» wraps (test 10); the «المهام» hint wraps with a lone «/» at the line end; no wrong-direction text except the market header (B3) |

### Part F, from scratch via Downloader
- F1 save: `scratch/settings.json` (`"schema": 1`), and show, adhan, dock (show/names/hidden/input-names/backsaver), pin status, familydns, tasks, photos, the disabled list, the package list, Home holder, screensaver. **Disabled 21, packages 177, processes 408, MemAvailable 915120 kB** (MemTotal 2451284 kB). Note: taken after part E, so it carries the team test 12 added and homelayout football; Z imports the start file anyway
- F2 store apps added with keys in the Play Store: **Plex** (com.plexapp.android, 16 s) and **Anghami** (com.anghami, 16 s). Tried first: Disney+ «This item isn't available in your country», YouTube Kids «Item not found». Netflix/Shahid/OSN+ were already on the TV and enabled
- F3 reset: `reset.sh` read, then run (20:55:45–20:56:33): «Note: there is no record of what our setup turned off and no backup of what the TV had off before it, so every package of the lists is turned on»; «(no optimize/run/backup.txt: screensaver_enabled and screen_off_timeout left as they are)»; removed sa.urtv.home and sa.urtv.screensaver; **2 `!!`**: `appop_RUN_IN_BACKGROUND` and `appop_RUN_ANY_IN_BACKGROUND`: «this TV did not take the value (asked for allow, it says default)». After: no `sa.urtv.*`, stock home (`launcherx/.home.HomeActivity`) on screen and Home holder, **0 disabled apps**; TV Bro and SmartTube stayed (no backup.txt to tell they came from the setup)
- F4 Downloader: the TV has **«Downloader for TV» (com.hkinnovate.downloader_for_tv)**, not AFTVnews Downloader. Screens and times (t = opening it):
  - t+0 home of the app: «Home / Files / Settings / FAQ», a «urlsrt.io/» checkbox, the field «Enter the URL of the File you want to download», «Download»
  - t+57 OK on the field opens the TV keyboard; the full URL typed with `input text`
  - t+89 «Download»: **black screen**, then the app is killed: it read the address as host **«htts»** (`Failed host lookup: 'htts'`), showed «Enter a valid url» toasts in a loop until Android killed it («Too many Binders sent to SYSTEM»). Typed again in 8-character pieces: the same «htts». **The field drops the «p» of «https://»** (seen twice)
  - t+740 typed without the scheme: `github.com/tshi-dev/UrTV/releases/download/apk-next/urtv-home.apk`; t+756 «Download» → within 10 s «Thank You! Your file has been successfully downloaded. Path: /storage/emulated/0/Download/urtv-home.apk» with «Install» / «Done»
  - t+787 «Install» → Android «تلفزيونك · Do you want to install this app?» (focus on Cancel); no «allow installs from this source» (Downloader already had it)
  - t+824 «Install» → **Google Play Protect: «Unsafe app blocked · This app was built for an older version of Android and doesn't include the latest privacy protections.»** with «More details» / «Got it». «More details» adds «Installing this app may put your device at risk» and **«Install anyway»**
  - t+960 «Install anyway» → t+965 «App installed.» (DONE / OPEN)
  - `versionName=4.2 versionCode=181` (the GO build) ✓; screensaver app not installed
  - **Problem for families: Play Protect blocks the home app (targetSdk=30) by default; the install needs «More details» → «Install anyway»**. And the URL needs no «https://» in this Downloader
- F5 setup (Play Store opened then HOME, pid 2704; `logcat -c`):
  - 1st «جهّز كل شي» (t+33): **no USB prompt**; after ~60 s the screen: «ما ظهرت الرسالة؟ أعد تشغيل التلفزيون وافتح «إعداد تلفزيوني» واضغط «جهّز كل شي» وخلك عند التلفزيون». UrTVSetup: `adb key: deleted exists=false`, `waiting for the debugging prompt: screensaver held back`. Same as step 18 row 4
  - 1 restart (`tv.py --reboot`; **unmuted after it**, muted 21:18:49), Play Store again (pid 3990), 2nd «جهّز كل شي» (t+29) → t+36 «Allow USB debugging?» (English, SystemUI `UsbDebuggingActivity`), «Always allow from this computer» → checked; focus Cancel → ▶ Allow → 23 (t+124); `debugging prompt answered or given up: screensaver free`
  - plan (10 lines): «١. نحفظ حالة التلفزيون الآن… ٢. نقيس الذاكرة قبل الإعداد وبعده (بدون إعادة تشغيل في هذا التلفزيون). ٣. … أذوناته … ٤. **نوقف ١٨ تطبيقاً من القائمة الآمنة** (إعلانات وتتبّع وعروض)… ٥. نطابق قائمة المجتمع لطراز تلفزيونك إن وُجدت… ٦. نسرّع التلفزيون… ٧. نثبّت شاشة التوقف… ٨. … العمل عند تشغيل التلفزيون… ٩. نجعل تلفزيونك الشاشة الرئيسية. إن ما اشتغلت خلال ٥ دقائق ترجع الأصلية وحدها. ١٠. نكتب تقريراً…»; «TV Bro و SmartTube مثبّتان من قبل: نتركهما» (no checkbox). Disabled before «ابدأ»: 0 (as F3)
  - «ابدأ» t+154. **12 s later the setup screen was gone** (t+166 and t+190: the stock home in front; t+215 our home in front; t+290 our screensaver). The run's lines and the done screen were only seen by opening «إعداد تلفزيوني» again: all **15 ✓**: فحص التلفزيون، نسخة احتياطية من حالة التلفزيون، الميزات المحمية، قياس الذاكرة قبل، أذونات تلفزيونك، القائمة الآمنة، قائمة المجتمع، تسريع التلفزيون، تثبيت شاشة التوقف، تفعيل شاشة التوقف، TV Bro و SmartTube، التشغيل التلقائي، الشاشة الرئيسية، قياس الذاكرة بعد، التقرير. No ✕, no –. No restart. Wi-Fi pause: did not come. Done screen: QR + «امسح الرمز بجوالك…», «بعد الإعداد أطفئ التصحيح» (3 steps), «لو صار شي» (4 lines), «تراجع», «تم», «انتهى الإعداد…». Our screensaver showed at t+290, after the run had ended (home switched at ~t+215), not during it
- F6: disabled **21** (F1 21; after reset 0): 18 + 3, all «system app», 0 «store app»; Plex/Anghami not touched. MemAvailable 915120 → 970784 kB; processes 408 → 398. Home `sa.urtv.home/.MainActivity`; screensaver `sa.urtv.screensaver/.ScreenDream` (the run installed **4.1, versionCode 168**, from apk-latest); AUTO_START allow, ASSOC_START allow for both (APP_AUTO_START unknown on this TV); TV Bro and SmartTube installed. **The F5 app (Play Store) was killed**: `am_kill … com.android.vending … cached idle & background restricted` at 21:23:28, ~1.5 min after the run: this is the plan's own line 6 («متجر Play لا يعمل في الخلفية»), so Play Store was a bad pick for this check; no other user app was killed
- F7 vs step 18 (build 174): the same plan («١٨ تطبيقاً من القائمة الآمنة», no restart, TV Bro/SmartTube kept), the same 15 ✓ lines, the same disabled 21 (18 + 3 from the home switch). The F2 apps changed nothing: Plex and Anghami are store apps not on the safe list, so not counted and not turned off (right). Which TCL/community list: **not named** anywhere (no UrTVSetup line names a list; the plan says only «نطابق قائمة المجتمع لطراز تلفزيونك إن وُجدت»; the TV-side run logged only 5 UrTVSetup lines)
- F8 back: the 4.2 screensaver from apk-next pushed and `pm install -r` → Success, 4.1 (168) → **4.2 (181)**, component unchanged. `saver.sh import scratch/settings.json`: preview 4 (markets style, markets list, teams, adhan volume) → `--yes`. By command: dock order, family DNS on, the 6 locked apps (no PIN), appearance dark, watchnext off, updatebell on, updatechannel next, homelayout football and yearstrip on (both not carried back by the import). Plex and Anghami removed with the Play Store's own «Uninstall» → «OK». Diff with F1: packages, disabled (21, same list), dock/names/hidden/input names/backsaver, pin, familydns, tasks, photos, Home, screensaver: **identical**. Left: the national team's name reads «السعودية» instead of «المنتخب السعودي» (same id 136137); music `source=radio` → `none`, `adhan_last` → 0 (runtime state, lost with the uninstall, accepted)

### Part D (demo-clean: Makkah + prayermethod auto (Umm al-Qura), 0 own photos (built-in wallpapers only), the 2 demo tasks, favourites YouTube/Netflix/Shahid/Prime Video/Apple TV/Spotify, no PIN/code/Wi-Fi/IP/serial/toast; every image opened and checked; kept on the PC in `optimize/run/step27/marketing/`, none pushed)
- `ar-sand-01-home.png`: done. Wallpaper: aerial Red Sea coast with mountains (no person)
- `ar-glass-01-home.png`: done. Wallpaper: AlUla sandstone rocks at sunset (no person)
- `en-glass-01-home.png`: done. Wallpaper: Kingdom Centre tower at dusk (no person). Small: the English home has no «Press and hold for options» hint; the Arabic one shows «اضغط مطوّلاً للخيارات»
- `ar-sand-13-match.png`: done (الهلال ١ - ٠ شباب الأهلي, live; «تابع شباب الأهلي»; Makkah prayer times). Small: a live match still reads «تبدأ بعد العشاء بساعتين ودقيقة»
- `ar-sand-14b-watchlist.png`: done (the gold card reads normally)
- `ar-sand-17-currencies.png`: **skipped: no rates after the fresh install** of part F: «لا أسعار بعد: يلزم اتصال بالإنترنت», KWD «—», 3 tries over ~10 min, an app restart too; the TV is online (the live markets source answers 200, Yahoo Finance hosts resolve). Before part F the same tab had KWD ١٢٫١٧. A problem
- Holdings: not needed for these shots (none made in D); the made-up one from B3 was wiped by the part F uninstall

### Z
`saver.sh import settings-before.json` (preview: city only → `--yes`); demo tasks cleared (start: no tasks); dock order as at the start; look glass, appearance dark (the import does not carry them); lang ar; homelayout classic; updatechannel latest; PIN «not set», wrong tries 0, the same 6 locked apps; family DNS on (as at the start); disabled list identical to the start (21); Home holder `sa.urtv.home/.MainActivity`; `wm size` override 1920x1080 unchanged; YouTube force-stopped; «محفظتي» empty; the 3 extra currencies gone (wiped with the uninstall); my log files and the downloaded apk removed from the TV; muted.
**restored: yes**, one difference: the followed team 136137 now reads «السعودية» instead of «المنتخب السعودي» (same team id; the name came back from the import after the reinstall). Build is now 181 (the update).

### Problems in step 27: 11 main, 12 small
Main: (1) A2 Jellyfin resumed for ~4 s after the home install, and UpdateActivity (not MainActivity) ends in front; (2) the update's second install question waits and our screensaver covers it; (3) B3 the Arabic «السوق» header draws the change as «−(٪٠٫٠٧−)  ٦٫٧٢»; (4) B3 after saving a holding the focus goes to «أخفِ المبالغ»; (5) B5 live mode parses nothing (`parsed=0 reason=parsed_none`); (6) B6/E12 the match detail opens with no visible focus and its focus ring is on the wrong button (OK followed the team instead of «ذكّرني»); (7) B8 «اختر العملات»: «تم» has no label, chosen currencies have no mark, focus lost after «تم»; (8) F4 Google Play Protect blocks installing the home app from Downloader («built for an older version of Android», targetSdk 30), needs «More details» → «Install anyway»; (9) F5 the setup screen leaves 12 s after «ابدأ», so the run and the done screen are not seen; (10) D: after a fresh install «صرف العملات» shows no rates (KWD «—», «يلزم اتصال بالإنترنت») while online; (11) the TV comes back UNMUTED after its own sleep and after every restart (the TV's audio service, not ours; 3 times today).
Small: 13th task silent on the TV; holding «احفظ» silent with 0/no cost; WindowLeaked from the converter on HOME; an open page does not follow `lang`; one long OK opened YouTube instead of the menu (not repeated); gold card sparkline touches the price; converter «٠٫٠٠٠٠٠٠» for 0 and no separators in the amount; team name «السعودية» after reinstall; live match says «تبدأ بعد العشاء…»; English home lacks the long-press hint; `reset.sh` 2 `!!` (RUN_IN_BACKGROUND appops); the first «جهّز كل شي» never shows the USB prompt (1 restart needed, as in step 18). Downloader for TV dropping the «p» of «https://» is that app's, not ours.
Shots: on the PC in `optimize/run/step27/marketing/` (5 kept, 1 skipped).

## step 28, build 182 (update 181 → 182; targetSdk home 34)
Start: found **unmuted** at 02:48:03 after a night asleep (muted at once); no tasks, Home ours, no PIN, classic, lang ar.

### A, the update («تحديث تلفزيوني», channel next; t0 = log start)
- t+96 «تحديث جديد 4.2 · اضغط للتحديث» pressed; screensaver question (installer status=-1); t+117 Update; t+121 status=0; t+122 the home app's question
- held 80 s on purpose, no keys: **t+167 (45 s later) our screensaver started OVER the second install question** (`START … DreamActivity`, dreams = ScreenDream, top = DreamActivity, t+168 … t+220). A key woke it, the question was still there; t+233 Update
- t+237 home replaced; t+238 `carry stage=front result=home_intent_sent why=replaced attempt=1` → `sa.urtv.home/.MainActivity` in front; t+239 `home_in_front why=start`; t+239 our UpdateActivity back, «تم التحديث ✓ 4.2» with «تم»; OK on «تم» → `START … sa.urtv.home/.MainActivity`, resumed
- other apps resumed at any line: **none** (only installer, our settings, UpdateActivity, MainActivity, DreamActivity). No media session playing. `Muted:true` at all 90 lines (t+0 … t+314)
- carry lines: `stage=before result=recorded muted=true ours=false`; `stage=step result=armed why=replaced`; `stage=front result=home_intent_sent why=replaced attempt=1`; `stage=audio result=already` (why replaced/start/replaced/start/alarm/alarm); `stage=a11y result=no_a11y_entry why=replaced`; `stage=front result=home_in_front why=start`
- **A: muted OK; our home in front by itself OK (Jellyfin problem gone); «تم» → MainActivity OK; ✗ the screensaver still covers the second app's install question.**

### B
- B1 sign: «⁦−٦٫٧٢⁩  ⁦(−٠٫٠٧٪)⁩» draws right («−٦٫٧٢ (−٠٫٠٧٪)»). OK
- B1 saving a made-up holding (أرامكو 10 @ 25): focus goes to the new row ✓ **but the page does not scroll to it**: focused row [128,1018,1792,1026], only 8 px of it visible. ✗ small
- B1 live 2 min (03:00 Saudi, Tadawul closed): `live status=200 asked=7 parsed=1 bytes=1399 reason=ok` every 30 s (4 lines); no `parsed=0`, so no `body_start` line. Labels: «تداول مغلق · يفتح الأربعاء ١٠:٠٠ ص · آخر تحديث ٢:٣٨ ص · أسعار متأخرة». OK; parsed=1 of 7 while closed (the others not read: not checked in hours)
- B1 picker: «تم» label drawn, works; chosen rows marked «مفعّلة» with the mark; focus after «تم» on «اختر العملات» [96,325,346,436]. OK. **✗ small: OK on a row far down (دينار تونسي, the 17th) jumps the focus and the list back to the top («دولار أمريكي»)**, you must walk down again for each pick
- New: **«صرف العملات» and «أخفِ المبالغ» labels are cut in half** (boxes [1233,170,1479,250], [988,170,1209,250], 80 px; only the top of the letters show). ✗
- New, keys: with the currency view open and the focus on «صرف العملات», ▼ or ▲ goes to the tab «محفظتي» and leaves the currency view; only ◀ reaches «حوّل» then «اختر العملات». Small
- Rates are back after the 181 reinstall: KWD ١٢٫٠٧ with «آخر تحديث ٢:٣٨ ص»
- B2 match detail (Ettifaq × Al-Hilal, tomorrow): opens with the ring on the whole dialog; ◀ → ring on «ذكّرني» → OK adds the reminder (`tasks list` shows it; deleted); ◀◀ → ring on «أضف الاتفاق لفرقي» → OK adds the team. OK does what the ring shows. OK. Small: OK with the ring on the dialog does nothing; ◀ from the dialog goes to the RIGHT button first (reading order)

### C
- C1 quick buttons: top row (Arabic right→left) المصادر [580..648], المهام [488..556], مركز التحكم [396..464], **الأسواق [304..372], المباريات [212..280]**, تلاوة وأذان [120..188], all y 72–140, gaps 24; reached with ▲ then ◀; OK opens MarketsActivity / MatchesActivity; Back returns with focus on the button. OK
- C2 match row: the same in both (time at the start, home logo+name, ×, away name+logo; names at the same x within each list; «المغرب ٥:١١ م» under the time). Widget rows 2 aligned, list rows aligned. OK. Small: the widget's title reads «مباريات اليوم» with both rows «غدًا»; **once**, right after adding a team, the list showed only 1 of the 2 groups (Hilal's match missing); reopened: right; not repeated
- C3: Thmanyah not installed → no «شاهد على ثمانية» (the list shows the «ثمانية» badge and «القناة: ثمانية، أو تطبيق ثمانية»); «أضف الاتفاق لفرقي» adds the team (`teams: …136017|الاتفاق`; removed in Z). OK. Not tested: «شاهد على ثمانية» with the app
- C4: controls row aligned (مباشر/عادي/صرف العملات/أخفِ المبالغ y 170–250), no green bar, density even; stock card solid, the page dimmed behind it. OK, **but the labels «صرف العملات» and «أخفِ المبالغ» are cut (see B1)**. **Home «الأسهم» card padding 50 px (card [96,168,1018,…], content x 146–968, top 218), expected 24 ✗**. Note: ◀/▶ on the home card row changes the home card (الأسواق ↔ الأسهم) by keys; the start value is `stocks`, put back in Z
- C5 «تطبيقات في الشريط»: 7: tiles 210×118, gaps 24, inside the tray [128..1792], AR and EN; 10: tiles 140×78, gaps 24, inside, AR and EN. OK. Small: at 7 a 2 px sliver of the next tile shows at the tray's edge (x≈128 AR, x≈1790 EN). Put back to 5 and the 7 favourites right after
- C6 القرّاء: السديس, الشريم, المعيقلي… first (the favourites); index: ▶ from a row goes to the letter («ع»), ▼ moves it, the list follows («ي» → «ياسر الدوسري») with keys. OK. Small: the index letters are about 19 px rows, hard to read on a TV
- B4 own sleep (223, 60 s, 224): muted after; restart (`tv.py --reboot`): muted after, at every read from 03:28:28. **OK both**. UrTVMute: `mute keep stage=remember why=screen_off muted=true`; `mute keep stage=restore why=screen_on result=muted_again after_ms=1501`; `mute keep stage=check why=screen_on result=already_done` ×2; after the restart `mute keep stage=check why=boot result=already_done` ×3

### D
- D1: saved (`scratch/settings.json` schema 1 + the lists; disabled 21, packages 177, processes 403, MemAvailable 999488 kB). `pm uninstall sa.urtv.home` Success; the screensaver stayed. After it: `com.android.tv.settings/.system.FallbackHome` in front (the stock home is one of the disabled apps). Note: our screensaver still started over Downloader during the wait (no harm). Downloader prompts, in order: field typed without `https://` in 12-character pieces (right); **«File Already Exist · Are you sure you want to download the file again?»** (the 181 file from step 27 was still there: Download Again / Cancel); «Thank You! Your file has been successfully downloaded.» (Install / Done); Android «تلفزيونك · Do you want to install this app?» (focus on Cancel; Install / Cancel); «App installed.» (DONE / OPEN). **NO Play Protect block.** `versionCode=182`, targetSdk 34, 3801933 bytes. OK
- Restart for the USB prompt (after the reinstall): **found unmuted** 03:37:39, muted at once (03:37:5x). UrTVMute: `mute keep stage=check why=boot result=no_record` ×3: a fresh install has no «muted» record (the mute was set before the install), so it cannot put it back. Worth checking whether that is what you want
- D1b / B3 «جهّز كل شي» after the reinstall (no reset; disabled still 21): 1st press **no USB prompt** (`waiting for the debugging prompt`, after ~60 s «ما ظهرت الرسالة؟…»), 1 restart, then the prompt 3 s after OK («Allow USB debugging?»; ticked «Always allow from this computer»; ▶ Allow). Plan (10 lines): «٤. القائمة الآمنة: لا شيء منها شغّال في هذا التلفزيون.»; «٥. نطابق قائمة المجتمع… إن وُجدت»; «٧. نحدّث شاشة التوقف إن وُجدت نسخة أحدث، ونفعّلها.»; «٩. تلفزيونك هو الشاشة الرئيسية أصلاً.»; «TV Bro و SmartTube مثبّتان من قبل: نتركهما»; no restart. Counts: 0 from the safe list
  - Run: «ابدأ» at 03:39:21. Every ~20 s: t+94 SetupActivity resumed; **t+117 our home `MainActivity` in front (03:39:23 a KEYCODE_HOME sent from the shell, uid 0, 2 s after «ابدأ», by the run) until 03:39:48**; t+136 SetupActivity again with the done screen (`START SetupActivity` 03:39:48). Dreams: none at every line. **B3: the setup screen left for ~25 s of a ~27 s run ✗** (better than step 27: the done screen now comes back by itself ✓; our screensaver never covered it ✓)
  - Step lines, all ✓: فحص التلفزيون، نسخة احتياطية من حالة التلفزيون، الميزات المحمية، قياس الذاكرة قبل، أذونات تلفزيونك، القائمة الآمنة، قائمة المجتمع، تسريع التلفزيون، تثبيت شاشة التوقف، تفعيل شاشة التوقف، TV Bro و SmartTube، التشغيل التلقائي، الشاشة الرئيسية، قياس الذاكرة بعد، التقرير. No ✕, no –. Wi-Fi pause: did not come. Muted at every line
  - Community list's name: **not found**: the plan names none and UrTVSetup has only `adb key: deleted exists=false`, `waiting for the debugging prompt: screensaver held back`, `debugging prompt answered or given up: screensaver free`, `adb key: deleted exists=false`
- D2: `adhan_exact=1` after the install ✓ (`adhan_next` set); the alarm count below (D5)
- D3: on the first open (station) **no question**: the setup's «أذونات تلفزيونك» had already granted POST_NOTIFICATIONS (`granted=true`). To test it: `pm revoke` + flags cleared + force-stop, then open the player: Android «Allow تلفزيونك to send you notifications?» (Allow / Don't allow) once; «Don't allow» → the player runs; Back and open again: not asked again (`USER_SET`). OK (permission granted back after; a family using the setup never sees this question)
- D4 after «لا»: `playing=1|source=radio|station=1`, no «cannot play», muted. OK. **Problem: station 0 («إذاعة القرآن الكريم - السعودية», the first in the list) never loads: «جارٍ التحميل…» and `playing=0` for over 2 min (`prepareAsync`, no message), tried twice; station 1 and `surah sudais 1` play within 20 s**

### E (demo-clean: Makkah, built-in wallpapers, the 2 demo tasks, common favourites, no PIN/code/Wi-Fi/IP/serial/toast; on the PC in `optimize/run/step28/marketing/`, none pushed; every image opened and checked)
- `ar-sand-11-football.png`: done (الاتفاق × الهلال, الخلود × الخليج, both «غدًا»; Kingdom Centre wallpaper, no person). Small: in «رملي» the 2 new quick buttons are grey-filled while the other 4 are white, and the widget's title reads «مباريات اليوم» above tomorrow's games
- `ar-sand-14a-market.png`: done, but it shows the cut «صرف العملات» label
- `ar-sand-17-currencies.png`: **skipped: no rates**: «لا أسعار بعد: يلزم اتصال بالإنترنت», KWD «—» on the fresh install, at 03:49 and again at 04:25 (46 min after the install; online, the live source answers). Same as step 27. Before the reinstall the same tab had KWD ١٢٫٠٧. ✗ Problem

### D5 (settings back, then the adhan with YouTube in front)
- Restore: `saver.sh import scratch/settings.json` (preview: adhan volume → `--yes`), dock order, the 6 locked apps (no PIN), family DNS on already; lists diff with D1's save: show, dock, pin, tasks, photos, disabled (21, same), packages, Home, screensaver: identical; `source=radio` only (runtime)
- Test: `adhan on`, `prayeradj on`, `fajr -20` (−20 is the limit, so Fajr → 04:22), `adhan_next_clock=04:22`, `dumpsys alarm | grep -c sa.urtv.home` = 6, `adhan_exact=1`, YouTube in front
- Result: `adhan_alarm_last=… front=other play=1 notice=0 saver=0`, `adhan_live=1`, `playing=1|source=adhan`, YouTube stays in front. **Plays with YouTube in front: OK**
- `logcat -s UrTVAdhan`: **no lines at all** (and no UrTV* adhan tag in the log), so the result is from `adhan_alarm_last` and the player state. Small
- **Sound rule broken in this row: the adhan is by design audible on a muted TV: at 04:22:00 it unmuted the TV (`muted=false`), and it played aloud for 115 s until I muted it at 04:23:55 (keyevent 164), TV level 28, adhan volume 20, then stopped it (04:23:57).** I checked the mute only after the row's wait. The row should have warned or used a lower level; **the step's «judged by the log only» cannot hold while the adhan unmutes.** On stop it set muted=true again by itself (04:23:57)
- Note: `streamVolume` was 28 at 03:38 and after; the TV showed 22 on 3 Nov. Not changed by me, cause not found; left at 28
- Adhan off, prayer minutes 0, `prayeradj off` right after

### Z
`import settings-before.json` (preview: home card → stocks, teams → without الاتفاق; `--yes`), tasks none, holdings none, favbar 5, dock order same, PIN «not set» and the locked list same, lang ar, classic, updatechannel latest, Home holder same, disabled list same (21), wm size unchanged, family DNS on, adhan off, minutes 0, YouTube force-stopped, muted. **restored: yes.** Differences: build 182; the show line `favbar: 5` is new in this build; Play Store's background restriction and the rest as the setup leaves them. **Correction to step 27's Z: my log files and the downloaded apk were NOT removed then (`pkill` ended that command); they are removed now.**

### Problems in step 28: 6 main, 11 small
Main: (1) A: our screensaver still covers the second app's install question (45 s, t+167); (2) «صرف العملات» and «أخفِ المبالغ» labels cut in half; (3) B3: the run itself presses HOME 2 s after «ابدأ», so our home is in front for ~25 s of the ~27 s run (the done screen does come back); (4) currencies show no rates on a fresh install (46+ min, KWD «—»); (5) D4 station 0 never loads, no message; (6) C4 home stocks card padding 50, expected 24.
Small: focus goes to the saved row but the page does not scroll to it; picker jumps to the top after each pick; ▲/▼ from «صرف العملات» leaves the currency view; fresh install has no mute record, so a restart after it comes back unmuted (`no_record`); first «جهّز كل شي» has no USB prompt (1 restart); community list's name not found; no UrTVAdhan lines; 2 px sliver at favbar 7; reciter index letters tiny; the 2 quick buttons look grey in «رملي»; «مباريات اليوم» above tomorrow's games (and once a stale list after adding a team).
Fixed since step 27: the header sign, Jellyfin after the update, UpdateCarry lines, «تم» → MainActivity, Play Protect (targetSdk 34: no block), TV's sleep and restart mute (UrTVMute), match detail's focus ring, picker's «تم», marks and focus, live `parsed`.
Shots on the PC in `optimize/run/step28/marketing/` (2 kept, 1 skipped).

## step 28b (step 28 again), build 183 (update 182 → 183, targetSdk home 34)
Start: muted; no tasks; Home ours; no PIN; classic; lang ar; home card stocks; favbar 5.

### A, the update («تحديث تلفزيوني», channel next)
- t+88 first question (screensaver); t+109 Update; t+114 the home app's question
- held on purpose, no keys, **131 s (t+114 … t+245): the screensaver did NOT start** (dreams null at every line, installer in front). ✓ (step 28: covered after 45 s)
- t+245 Update; t+248 UpdateActivity; t+253 `sa.urtv.home/.MainActivity`; t+257 UpdateActivity «تم التحديث ✓ 4.2» with «تم»; OK → `sa.urtv.home/.MainActivity` resumed
- no other app resumed at any line; nothing playing; muted at every line
- **A: all OK.**

### B
- B1 sign: «−٦٫٧٢ (−٠٫٠٧٪)» OK. «صرف العملات» [1233,170,1479,273] now 103 px, label whole; «أخفِ المبالغ» the same. OK
- B1 holding (أرامكو 10 @ 25): focus on the new row [128,833,1792,978], the page scrolled to it, whole. OK
- B1 live 2 min (08:05 Saudi, Tadawul closed): `live status=200 asked=1 parsed=1 bytes=1400 reason=ok` every 30 s on «محفظتي» (no parsed=0, so no body_start). OK. Checked again in market hours below
- B1 picker: «تم» works, marks «مفعّلة»; the pick stays on its row (دينار تونسي, was jumping to the top); after «تم» focus on «اختر العملات». OK
- **Currencies still show no rates** on this install: «لا أسعار بعد: يلزم اتصال بالإنترنت» at 08:08 (installed about 03:32, 4.5 h, one update since). ✗ (see E)
- B2 match detail: opens with the ring on the dialog; ◀ ring on «ذكّرني» → OK adds that reminder (`tasks list`; deleted); ◀◀ ring on «أضف الاتفاق لفرقي» → OK adds the team. OK
- C1: top row الأسواق [304..372], المباريات [212..280], all y 72–140, gaps 24; ▲ then ◀; OK opens MarketsActivity / MatchesActivity. OK **only with no home card. ✗ With the home card on (the owner's own setting, «الأسهم»), ▲ from the tray stops on the card, 3 more ▲ and ◀▲ / ▶▲ never reach the top row** (the card's own box [96,168,1018,668] holds the focus): the top buttons cannot be reached with keys. Repeated
- C2 match row the same in widget and list; title now «المباريات القادمة» ✓. **After adding a team («أضف لفرقي») the list loses the «فرقك» group: only «دوري روشن» (1), Hilal's match gone, while the widget shows it. Repeated (step 28 once, now 3 reads including reopening the page); `am force-stop sa.urtv.home` brings it back (فرقك 1, دوري روشن 2).** ✗
- C3: no «شاهد على ثمانية» (Thmanyah not installed); «أضف لفرقي» OK. Not tested with the app
- C4: controls row aligned, labels whole, no green bar; stock card solid, page dimmed (as 182). **Home stocks card padding about 36 px** (card x 98–1016, text x 134–980; top ≈ 42, bottom ≈ 40), expected 24±2 ✗ (step 28: 50)
- C5 bar 7: tiles 210×118, inside [128..1792], AR and EN; bar 10: 140×78, inside, AR and EN. OK. At 7 with more favourites than fit a 2 px sliver of the next tile shows at the tray's edge (it scrolls). Small
- C6: السديس, الشريم, المعيقلي first; ▶ goes to the index, rows now 45 px; ▼ to «ي» → the list at «ياسر الدوسري». OK
- B4 sleep (223, 60 s, 224): muted; UrTVMute `stage=remember why=screen_off muted=true`, `stage=restore why=screen_on result=muted_again after_ms=1502`, `stage=check why=screen_on result=already_done`. OK
- B4 restart: muted at every read from 08:19:32; `stage=check why=boot result=already_done` ×3. OK

### D
- D1: saved; `pm uninstall sa.urtv.home` Success; Downloader, URL without `https://`: t+49 «Download» → t+56 «Thank You! Your file has been successfully downloaded.» (Install / Done) → Android «تلفزيونك · Do you want to install this app?» (focus on Cancel) → t+97 Install → «App installed.» (DONE / OPEN). **No Play Protect block.** versionCode 183, targetSdk 34. OK
- D1b / B3: USB prompt **on the first press** (t+13; ticked «Always allow from this computer», ▶ Allow). Plan as in step 28 (10 lines; safe list «لا شيء منها شغّال», «تلفزيونك هو الشاشة الرئيسية أصلاً», no restart, TV Bro/SmartTube kept). Run: «ابدأ» t+73; every ~12 s `SetupActivity` resumed with the lines moving (✓ / … / ·) at t+79, 91, 103, 116, 128; dreams none; muted; done screen at t+128. **The setup screen stayed in front the whole run ✓**
  - All 15 ✓: فحص التلفزيون، نسخة احتياطية من حالة التلفزيون، الميزات المحمية، قياس الذاكرة قبل، أذونات تلفزيونك، القائمة الآمنة، قائمة المجتمع، تسريع التلفزيون، تثبيت شاشة التوقف، تفعيل شاشة التوقف، TV Bro و SmartTube، التشغيل التلقائي، الشاشة الرئيسية، قياس الذاكرة بعد، التقرير. No ✕/–. Wi-Fi pause: did not come
  - Community list's name: **still not found** (plan names none; UrTVSetup: `adb key: deleted exists=false`, `waiting for the debugging prompt: screensaver held back`, `debugging prompt answered or given up: screensaver free`, `adb key: deleted exists=false`)
- D2: `adhan_exact=1` after the install. OK (alarm count at D5)
- D3: first open: no question (setup granted POST_NOTIFICATIONS, `granted=true`). After `pm revoke` + flags cleared + force-stop: «Allow تلفزيونك to send you notifications?» once; «Don't allow» → player runs; Back and open again: not asked. OK (granted back after)
- D4 after «لا»: station 1 `playing=1`, no «cannot play». OK. Station 0 («إذاعة القرآن الكريم - السعودية»): now «تعذّر التشغيل» within 28 s ✓ (step 28: «جارٍ التحميل…» forever), but the station itself still does not play (2 tries) ✗: the first station in the list, worth checking its address

### E (demo-clean; on the PC in `optimize/run/step28b/marketing/`; checked; none pushed)
- `ar-sand-11-football.png`: done (AlUla rocks wallpaper, no person; «المباريات القادمة»; the 6 top buttons now all white in «رملي» ✓)
- `ar-sand-14a-market.png`: done (labels whole)

### B1 live in market hours (Tadawul open, 10:05–10:32 Saudi; the header says «تداول مفتوح · يغلق بعد …»)
- 10:05: `live body_start={"^TASI.SR":{"timestamp":[],"symbol":"^TASI.SR","end":null,"previousClose":9882.85,…,"close":null,"fulldayChange":-6.72,…,"fulldayPrice":9876.13,…}}` then `live status=200 asked=7 parsed=0 bytes=236 reason=parsed_none`, once a minute
- 10:30: `live status=200 asked=7 parsed=1 bytes=320 reason=ok`, then `asked=6 parsed=1 bytes=339 reason=ok`, every 30 s
- **Prices did not move** on «قائمتي» (أرامكو ٢٦٫١٦, الراجحي ٦٠٫١٥, stc ٤١٫١٨ the same at 10:06, 10:08, 10:30, 10:32, the same as yesterday's); only 1 of 6–7 is parsed and the header still reads «آخر تحديث ٦:١٢ ص · أسعار متأخرة» (honest). ✗ Could be the source's delay (not checked later than 10:32)
- Currencies at 10:08: still «لا أسعار بعد», KWD «—» (6.5 h after the install) ✗
- `ar-sand-17-currencies.png`: **skipped: no rates** at 08:30, 10:08 and 11:18 (8 h after the install): «لا أسعار بعد: يلزم اتصال بالإنترنت», KWD «—», online. Same since step 27. ✗

### D5 (settings back, then the adhan with YouTube in front)
- Restore: import (preview: adhan volume → `--yes`), dock, the locked apps; diff with D1's save: show, dock, pin, tasks, photos, disabled (21), packages, Home, screensaver, family DNS: identical (adhan: runtime lines only)
- Test: adhan volume set to the minimum 10% for this row and **a guard that polled the mute and pressed mute + stop the moment it opened** (step 28 played aloud for 115 s). `adhan_next_clock=11:17`, `adhan_exact=1`, `dumpsys alarm | grep -c sa.urtv.home` = 4, YouTube in front
- Result: `adhan_alarm_last=… front=other play=1 notice=0 saver=0`; YouTube stayed in front. **OK**. UrTVAdhan: `adhan start: voice=1 prayer=2 live tv_muted_before=false`
- **The adhan still unmutes a muted TV by itself** (`STREAM_MUTE_CHANGED muted=false` at 11:17:00.123, before the `adhan start` line). Muted again by the guard at 11:17:00–03: audible under 3 s at 10%. **`tv_muted_before=false` on a TV that WAS muted** (UrTVMute/dumpsys said muted just before): the value is read after the unmute, so the «mute again after the adhan» part may not know. Not tested to the adhan's end (for the sound rule). ✗ to check
- Adhan off, minutes 0, `prayeradj off` right after

### Z
`import settings-before.json` (preview: home card → stocks, teams → without الاتفاق, adhan volume → 20; `--yes`), tasks none, holdings none (the reinstall), favbar 5, dock, PIN «not set», locked list, lang ar, classic, updatechannel latest, Home holder, disabled list (21), wm size unchanged, adhan off volume 20, minutes 0, YouTube force-stopped, downloaded apk removed, no logs left, muted. **restored: yes**; only the build differs (183).

### Problems in step 28b: 7 main, 2 small
Main: (1) C1 with a home card on, the top buttons (with the new quick ones) cannot be reached with keys; (2) C2 after «أضف لفرقي» the list loses «فرقك» until the app restarts (repeats); (3) currencies show no rates after a fresh install, 8 h later (KWD «—»); (4) station 0 «إذاعة القرآن الكريم - السعودية» fails, now with «تعذّر التشغيل»; (5) home stocks card padding about 36, expected 24; (6) in market hours prices do not move in 30 min (`parsed=0` with body_start, then `parsed=1` of 6–7); (7) the adhan unmutes a muted TV and logs `tv_muted_before=false`.
Small: community list's name not in the plan or the log; 2 px sliver at bar 7.
Fixed since step 28: screensaver no longer covers the install question; setup screen stays in front the whole run and the USB prompt came first time; «صرف العملات» / «أخفِ المبالغ» labels; the page scrolls to a saved holding; picker keeps its row; quick buttons' colour; «المباريات القادمة»; a bigger reciter index; a station that fails now says so.
Shots: `optimize/run/step28b/marketing/` (2 kept, 1 skipped).

## step 29a, PC network checks (no TV touched; run from the owner's PC)
1. kwikmotion (`live.kwikmotion.com/…/playlist.m3u8`): `curl -sI` **HTTP 200**, `application/x-mpegURL`. `ffprobe`: **aac** (one more stream with no codec name, an empty first line). `ffmpeg -t 60`: rc=0, **no errors, 53 s wall**. Reads cleanly
2. radiojar (`stream.radiojar.com/0tpy1h0kxtzuv`): `ffprobe`: **mp3**. `ffmpeg -t 60`: rc=0, no errors, 55 s wall. `curl -sI` (HEAD): **HTTP 404**, no redirect. With GET (`curl -L -r 0-1000`): **HTTP 302 → `http://n04.radiojar.com` (https→http) → HTTP 200 `audio/mpeg`**. So the old stream goes to clear http on GET; a build that blocks cleartext could fail on it, which would fit station 0's «تعذّر التشغيل» on targetSdk 34 (not proven from the TV)
3. kwikmotion `curl -sI` from the PC: 200 (as in 1)
4. Yahoo `query1…/v8/finance/spark?symbols=SAR%3DX,KWD%3DX&range=1d&interval=1d`:
   - curl's default User-Agent: **HTTP 429**, 23 bytes: `Edge: Too Many Requests`
   - with `-A "Mozilla/5.0"` (added by me): **HTTP 200**, 467 bytes, first 200: `{"KWD=X":{"timestamp":[1793730339],"symbol":"KWD=X","end":null,"close":[0.3107],"dataGranularity":300,"chartPreviousClose":0.3107,"start":null,"previousClose":null,"fulldayChange":0.0,"fulldayChangePe`; `SAR=X` close 3.7533, `KWD=X` close 0.3107 (about 12.08 SAR per KWD)
   - So the answer depends on the User-Agent: worth checking what the app sends, given the empty currency tab

## step 29, build 186 (update 183 → 186)
Start: **found unmuted at 16:55:45**, muted at once (the TV had been idle since the morning; cause not known; last step ended muted). No tasks, Home ours, no PIN, classic, lang ar, home card stocks, favbar 5, adhan off.

### A ✓
Second install question held 81 s (t+135 … t+216): dreams null at every line, installer in front. t+216 Update; then UpdateActivity «تم التحديث ✓ 4.2»; «تم» → `sa.urtv.home/.MainActivity`. No other app at any line; muted at every line. Both apps 186.

### B ✓ (home card «الأسهم» on)
▲ from the tray → the card (ring on it) → ▲ → the top row («المصادر», label open). ◀ walks المهام → مركز التحكم → الأسواق → المباريات → تلاوة وأذان; OK opens TasksActivity, the control centre, MarketsActivity, MatchesActivity, tilawah.MainActivity; Back returns.
**Correction to step 28 and 28b:** I reported the top row unreachable with a home card. That was **my reading error, possibly not a bug in those builds**: the dump keeps a second `focused=true` node (the hidden card), and I read the last one. The screenshots here show the ring moving. (Not re-tested on 183.)

### C ✗
Before: list «غدًا»: فرقك 1 (الاتفاق × الهلال) + دوري روشن 2 (screenshot C-0). «أضف الاتفاق لفرقي» (`teams … 136017|الاتفاق`). Reopened 3× (Back, Home, open, 8 s): **all 3 times only «دوري روشن 1» (الخلود × الخليج); «فرقك» and Hilal's match are gone.** Unchanged since 28b

### D
- «صرف العملات» shows rates ✓: USD ٣٫٧٥, KWD **١٢٫٠٧**, AED ١٫٠٢, «آخر تحديث ٥:٠٣ م» (after the app restart; the install is from step 28b). Bounds: مباشر [1636,176,1800,256], عادي [1479,176,1612,256], صرف العملات [1239,170,1479,262], same row
- Tadawul closed at this hour (17:05): `live status=200 asked=7 parsed=1 bytes=1440 reason=ok`. «parsed = asked» and moving prices **not testable** now
- «أخفِ المبالغ» [1001,170,1215,262], «صرف العملات» [1239,170,1479,262], whole labels, beside مباشر | عادي ✓
- Pad «متوسط التكلفة»: ⌫ points right ✓; zero reads as a clear «٠» (hollow) ✓; «٫» key: ١٢٫٥ typed, quantity ٣ → saved, row shows **١٢٫٥٠** ✓ focus on the saved row [128,833,1792,978]. ✗ small: **the bottom row of keys (⌫, ٠, ٫) is cut at the bottom of the glyph**, drawn low in the key

### E ✓
Station 0 «إذاعة القرآن الكريم - السعودية»: `playing=1` at each read over 70 s (17:13:10–17:14:19). Stations 1, 2, 3: `playing=1`. Muted at every read.

### F (10 favourites; shots `marketing/{ar,en}-tray-{5,7,8,all}.png`, demo-clean, PC only)
| size | tiles shown | tile | tiles x | tile y | tray | 
|---|---|---|---|---|---|
| 5 | 5 | 304×170 | 152–1768 | 792–962 | 128–1792, 768–986 |
| 7 | 7 | 210×117 | 152–1768 | 845–962 | 128–1792, 821–986 |
| 8 | 8 | 181×101 | 152–1768 | 861–962 | 128–1792, 837–986 |
| الكل | 8 | 181×101 | 152–1768 | 861–962 | 128–1792, 837–986 |
✓ AR and EN: whole tiles only, **no sliver**, tray height follows the tile (inset 24), «more» triangle on the left in AR and on the right in EN, focused tile inside the tray. EN «Hold OK for options» [306,745,597,805] on the name's line [152,748,274,803]. ✗ question: **«الكل» with 10 favourites shows the same as 8 (8 tiles, a «more» triangle, the rest scroll)**, not all 10

### G
Six quick buttons the same size and look ✓ ([120..188] … [580..648], y 72–140; white in «رملي»). Prayer card [526,319,946,572]: last line bottom about y 553, about 19 px from the bottom ✓. Matches rows centred in their rows, the same x for names ✓. Long-press hint: English «Hold OK for options» legible on dark ✓; **the Arabic «اضغط مطولاً للخيارات» did not show in this session** (after focus moves, 5 s after a restart, dark and light), so its legibility was not judged

### H ✓ (adhan volume 10%, «تكبير فقط» so it ends by itself in about 14 s, guard ready, not needed)
- `adhan now`: unmuted t+2 … t+15, `playing=1`, then muted, `playing=0` at t+17. UrTVAdhan: `adhan start: prayer=-1 live tv_muted_before=true`; `adhan volume: the TV was muted, unmuted for the adhan (it is muted again at its end)`; `adhan stop: tv_muted_now=false`; `adhan end: re-mute done, volume back to 25`
- Back after 5 s: stopped, muted within 2 s; `adhan start … tv_muted_before=true`, `adhan stop: tv_muted_now=false`, `adhan end: re-mute done, volume back to 25`
- Real alarm (Maghrib moved to 17:25): unmuted 17:25:00, muted again 17:25:14; `adhan start: prayer=4 live tv_muted_before=true` … `adhan end: re-mute done, volume back to 25`; `adhan_alarm_last=… front=dream play=1 notice=0 saver=0`
- Audible 14 s + 5 s + 14 s at 10%, as the row asks. Note: «volume back to 25»: the TV's level was 28 yesterday (not read at this run's start); it reads 25 now

### Z
`import settings-before.json` (preview: city, team, adhan volume; `--yes`), the made-up holding deleted by «خيارات محفظتي ← احذف المحفظة ← نعم» («لا أسهم في محفظتك بعد»), tasks none, dock, favbar 5, classic, lang ar, glass dark, PIN «not set» and locked list same, Home holder, disabled list (21), wm size unchanged, adhan off, takbir off, volume 20, minutes 0, updatechannel latest, YouTube force-stopped, no logs left, muted. **restored: yes**; only the build differs (186).

### step 29 summary: ✓ A, B, E, F (with one question), G (Arabic hint not seen), H. ✗ C. D ✓ except the bottom keys cut; live in market hours not testable (closed)
Problems: (1) C «فرقك» disappears after «أضف لفرقي», 3/3 reopens; (2) pad's bottom row (⌫ ٠ ٫) cut at the bottom; (3) question: «الكل» with 10 favourites shows 8, the rest scroll; (4) the Arabic hint «اضغط مطولاً للخيارات» did not show; (5) start: TV found unmuted at 16:55:45, cause unknown (an update to the mute record? nothing in the log kept). Not tested: live prices in Tadawul hours (needs 10:00–15:00 Sun–Thu).
Shots: `optimize/run/step29/marketing/` (8 tray shots, demo-clean, none pushed).

## step 30 — build 188 (updated from 186 with «تحديث تلفزيوني», channel next; TV muted throughout; Wed 19:14–19:46)
Update: no screensaver over the question, «تم» → MainActivity, both apps versionCode=188. 200 watcher reads from the start of the update to t+762: `Muted:true` in all, `Muted:false` 0.

### 1 ✗ «فرقك» still disappears, but only when the page is reopened quickly (improved since 186; the cause looks different)
«أضف الاتفاق لفرقي» (`teams … 136017|الاتفاق`). The group holds الاتفاق × الهلال when it shows. It comes and goes:
| how reopened | teams | «فرقك» shown |
|---|---|---|
| quickly, 13–28 s between opens | 3 | **4 of 12** (✓✗✗✗✗✓✓✓✗✗✗✗) |
| 60 s wait + about 14 s per open | 3 | **4 of 4** |
| quickly, control after the added team was removed | 2 | **1 of 6** (✗✗✗✗✗✓) |
When it is gone, the Hilal game is gone too (only «دوري روشن ١»), and it stays gone for that open (same at 3, 8 and 17 s). The control shows **adding a team is not the trigger**.
My guess, not proven: the sports server refuses quick repeat requests (each open asks 2 per team, 6 with 3 teams). `NEXT_LIST` and `MATCHES` are memory only, so a new page with a refused team has nothing for it (`loadMatches` treats a partial answer as a success; `leagues` is empty on this TV). I have no response codes: no `UrTVMatches` lines, and no WebView console in logcat. A person opening the page twice inside a minute could see it.

### 2 Pad: glyphs ✓, ✗ small: two keys' boxes
⌫ ٠ ٫ glyphs are whole and centred. But the boxes of ⌫ and ٠ are not whole: `⌫ [544,792,811,888]`, 96 high, top 8 px lower than `٫ [1110,784,1376,888]`, 104 high like the other keys, and **their bottom border and round corners are cut off** at y 888. The sheet itself runs y 0–1080, edge to edge (shot D-pad).

### 3 ✓ Arabic hint
«اضغط مطوّلاً للخيارات» [1373,692,1652,752] beside the name «يوتيوب» [1684,695,1768,750], on the same line, legible on the photo; it follows the name (Crunchyroll: [1294,692,1573,752]).

### 4 ✓ Dark pages (home look «رملي» فاتح)
`pagesdark: on` by default: both pages dark, rings and contrast OK (white ring on markets, green on matches). Off (`saver.sh pagesdark off`): both pages, the stock card and the match card follow the sand look, rings and contrast OK. On again with OK on the row in Settings ← البطاقات ← الشاشة الرئيسية («مفعّل · بالمظهر الداكن دائمًا», `pagesdark: on`). Shots: `marketing/ar-sand-markets-dark.png`, `ar-sand-matches-dark.png`, and the P-on / P-off shots.

### 5 Match card ✓, stock card ✗
- **Match card ✓:** ▼ reaches «رجوع», OK closes, Back closes. Focus returns to the same row, tested on the 2nd row too (ring on الخلود × الخليج both ways)
- **Stock card ✗ (the owner's request is not met there):** the card is taller than the screen. **«رجوع» never shows and is not in the dump**, and ▼ stays on the action button. That button is squeezed to `[264,988,1656,1024]`, 36 px high, label unreadable, a plain white pill. Seen on 3 stocks (2222, 1120, 1211), from «قائمتي» and from the movers list. So OK on the only button reachable **changes the list without the person reading it**: mine added 1211 to «قائمتي» (removed in Z), the card closed and focus fell to «مباشر», not to the row
- Remote Back on the stock card ✓: closes, focus on the same row (الراجحي [777,727,1792,881])

### 6 Mute ✓
`logcat -d -s UrTVMute` from before the update to the end: **0 lines**, so no `by=not us`. (No change of ours either: nothing played. So this run does not show that the logging itself works.) Muted at every check.

### 7 Not testable
Tadawul closed (Wednesday evening, the page says it opens Thursday 10:00). Needs Sun–Thu 10:00–15:00.

### Small note, not asked
The home greeting reads «تصبح على خير» at 19:18.

### Z
`import settings-before.json` (preview: only teams changed; `--yes`), stocks back to the 5, look glass, dark, updatechannel latest. `show` before/after differs only in the build and the new `pagesdark: on`. Same: dock, PIN and locked list, Home holder, disabled list (21), packages (177), tasks, adhan, photos, family DNS, saver. Portfolio empty (the pad was left without saving). Muted. **restored: yes**; only the build differs (188).

### step 30 summary: ✓ 3, 4, 6; match card in 5. ✗ 1, stock card in 5; 2 glyphs ✓ with a small box problem. 7 not testable.
Problems: (1) «فرقك» and the team's game vanish on quick reopens (4/12 shown; 4/4 with a minute's wait; 1/6 without the added team); (2) stock card too tall: «رجوع» unseen and unreachable, the action button squeezed to 36 px and unreadable, OK on it changes the list; (3) pad keys ⌫ and ٠ have a cut box; (4) greeting note.
Shots: PC only, none pushed.

## step 30b — live US prices (build 188; TV muted; Wed 21:32–22:00, US market open)
AAPL, NVDA, TSLA added with `saver.sh stocks` (not by the page's own add); «مباشر» was already on. Muted at every check; `UrTVMute` 0 lines.

### 1 ✗ by the rule (parsed ≠ asked), the US symbols themselves ✓
Every line caught on «قائمتي» (9; the log buffer keeps only a few, so **not every line of the 10 min**): 21:34:51, 21:36:22–21:37:54, 21:43:00–21:44:32: all `live status=200 asked=9 parsed=4 reason=ok`, about every 30 s.
Rest: first page «السوق» `asked=6 parsed=1`; again at 21:57–21:58 `asked=9 parsed=4`; one `status=-1 asked=1 reason=SocketTimeoutException` at 21:55:11, next at 21:56:14 `asked=1 parsed=1`.
My reading, not proven (the log gives counts, no names): the 4 are the 3 US + one other; the Tadawul symbols are the ones not parsed, as on step 29 (`asked=7 parsed=1`, market closed). US rows carry a live time («AAPL · ٩:٣٤ م»), Tadawul rows none. Still to see in Tadawul hours.

### 2 ✓ Prices move and match (Google Finance in the browser, same minute)
| Saudi time | AAPL TV / Google | NVDA TV / Google | TSLA TV / Google |
|---|---|---|---|
| +0 21:35 | 352.53 / 352.61 | 203.98 / 203.97 | 389.53 / 389.66 |
| +3 21:38 | 352.80 / 352.75 | 203.66 / 203.68 | 388.72 / 388.80 |
| +6 21:40 | 352.78 / 352.75 | 203.75 / 203.80 | 389.08 / 389.00 |
| +10 21:44 | 352.88 / 352.86 | 203.56 / 203.39 | 389.48 / 389.68 |
All within 0.1% (limit 0.5%). Note: the +10 Google read was 74 s after the TV's, the others about 3 s. % also agree (AAPL +6.21 / +6.24 at +0).

### 3 ✗ Header stays on Tadawul
With only US prices moving, the whole 10 min: «تداول مغلق · يفتح الخميس ١٠:٠٠ ص · آخر تحديث ٥:٥٧ م · أسعار متأخرة». No US session text anywhere (open / closes in …). It also says **«أسعار متأخرة» while the footer says «مباشر · المصدر: Yahoo Finance»** and the rows are live.
S&P 500 / Nasdaq: not shown on «السوق» here (Tadawul only; markets setting has `spx`, seen on no row of that tab).

### 4 ✓ Portfolio: AAPL 2 @ 150 (price then 352.68)
Value `$ ٧٠٥٫٣٥`, total `٢٬٦٤٥٫٠٦ ر.س`: 705.35 × 3.75 = 2645.06 ✓. Profit +405.35 = +135.12% (on 300) ✓, in SAR +1,520.06 = 405.35 × 3.75 ✓. Today +41.55 = 2 × (352.68 − 331.90 Google's previous close) ✓, in SAR +155.81 = 41.55 × 3.75 ✓, +6.26% ✓. (2 × 352.68 = 705.36: one cent off from the hidden decimals, fine.) AAPL is at the end of the «أضف سهمًا» list once in the watchlist.

### 5 Mostly ✓, ✗ small on the home card
- Stock card: amount and % together, right: AAPL 352.75 `+٢٠٫٨٥ (+٦٫٢٨٪)` ✓ (352.75 − 331.90 = 20.85). This does not depend on the option
- «قائمتي» list: always %, with `stkchg amount` too (AAPL +6.27%)
- Home card with `stkchg amount`: US rows show the amount (AAPL ٢٠٫٩٩, NVDA ٣٫٣٨, TSLA ٨٫٩٤; AAPL's is right), but **Tadawul rows keep % in the same column** (stc ٠٫٧٣٪…). Mixed units. Also: US rows there have no «مغلق», Tadawul ones do ✓

### Also found, on the US stock's card ✗
- **The chart's axis reads Tadawul's hours** («١٠:٠٠ ص … ٣:٠٠ م») for AAPL
- **Open and range are wrong**: «الافتتاح» ٣٥٠٫٤٨ against Google's 341.57; «نطاق اليوم» ٣٥٠٫٤٨–٣٥٤٫٣٧ against 340.99–354.50. It looks like the low and the open start from the app's last file (17:57) or a later point, not the session's open (my guess)
- Just after a fresh open, before the first live answer (about 30 s): the file's old price: AAPL ٣٥٠٫٩٠, +١٩٫٠٠
- The same card overflow as step 30: no «رجوع», squeezed action button

### 6 Shots (PC only, none pushed): `marketing/ar-qaimati-live.png`, `ar-mahfaza.png` (the total is scrolled off in that one; its numbers are in 4)

### Z
Holding deleted by «خيارات محفظتي ← احذف المحفظة ← نعم» («لا أسهم في محفظتك بعد»); stocks back to the 5, stkchg pct, «مباشر» on as found; import: «Nothing to change». `show` before/after identical. Same: dock, PIN, Home holder, disabled (21), packages (177), tasks, adhan, photos, family DNS, saver. Muted. **restored: yes**.

### step 30b summary: ✓ 2, 4. ✗ 1 by the rule (`parsed=4` of 9; the US ones all live), ✗ 3, ✗ small in 5
Problems: (1) `parsed` ≠ `asked`, probably the Tadawul symbols; (2) header shows Tadawul and «أسعار متأخرة» while US prices are live; (3) US stock's card: Tadawul hours on the axis, wrong open and day range; (4) home card mixes amount and % with `stkchg amount`; (5) no S&P 500 / Nasdaq on «السوق» (asked «if shown»).

## step 31 — build 191 (updated from 188 with «تحديث تلفزيوني», channel next: the channel gave 191, newer than the 190 asked; TV muted throughout; Thu 01:47–02:24)
Update: no screensaver over the question (45 watcher reads, `dream=null`), «تم» → MainActivity, both apps versionCode=191. `Muted:true` at every read.

### 1 ✓ «فرقك»
Added الاتفاق (3 teams), reopened about every 14.5 s: **12 of 12** with «فرقك» and الاتفاق × الهلال (step 30: 4 of 12). Checked on the 12 shots by eye. One run, at night.

### 2 Stock card: fits ✓, «رجوع» ✗
2222 and 1120 from «قائمتي», 1211 from the movers:
- ✓ Fits the safe area: content [264,56,1656,788], action button [264,828,1656,939], «رجوع» [264,963,1656,1024]. Action button 111 px high, label readable
- ✓ OK on «رجوع» returns to the same row on all 3 (أرامكو, الراجحي [777,727,1792,881], معادن [128,434,1792,545]); remote Back too (2222)
- ✓ The list did not change (`stocks` the same; 1211 stayed out)
- **✗ «رجوع» has no visible label:** the dump has the text, but the screen shows an empty box, and an empty white pill when focused. All 3 stocks. The box is 61 px high against 111 for the button above it. The match card's «رجوع» shows its label. Shots S3-dn2, crop-card
- **✗ First focus is not on «رجوع»:** it is on the scrolling content (`ScrollView` [264,56,1656,788]), with no visible ring. ▼ reaches «رجوع» after 2 presses on 1120, 3 on 2222 and 1211, and the first ▼ on the last two only scrolls the name out of view

### 3 ✓ Pad, with a small note
All 11 keys read from the dump are 104 px high, same rows, borders whole in the shot. ⌫ `[544,784,811,888]`, ٫ `[1110,784,1376,888]`. The middle bottom key is not in the dump as ٠, and in the shot it reads as a hollow «0», close to ٥ two rows above. Digits are `hindi`, where ٠ is a dot. **Small ✗, from the shot and the missing text only.**

### 4 Greeting: not testable now
02:04 shows «تصبح على خير», right for the small hours. «مساء الخير» until 2 h after Isha needs an evening run. The rule in `GreetingText.part` reads as asked; `tests/greeting_test.py` not run (no javac here).

### 5 ✓ Dark pages and match card
With look «رملي»: markets, stock card, matches and match card all dark. With the glass look the markets page keeps the blurred photo behind, as in 30b (the page's darkening varies with the photo). Match card: «رجوع» labelled, ▼ reaches it in one press, OK and Back return to the same row (2nd row, الخلود × الخليج).

### 6 Smoothness: numbers, feel not judged
- **Settings ✓** `Displayed`: cold (after force-stop) 811, 676, 686 ms. Warm: no `Displayed` line; `am start -W` TotalTime 207, 201, 199 ms
- **Markets «مباشر»** 5 min on «قائمتي», prices closed: asks at 02:08:33, 09:37, 10:37, 11:37, 12:38, so **every 60 s** as the weak-TV rule says
- **No redraw log exists**, so I counted frames with `dumpsys gfxinfo`. Page in «عادي», 100 s: **0 frames ✓**. «مباشر», 190 s, 3 asks with unchanged prices: **8 frames**, 1 janky. So a tick still draws something. It may be only the header and footer text: I can't tell from this whether the rows are rebuilt. A live tick with changing prices was not tested (both markets closed)
- **Tray:** focus left and right over the home tiles, 10 presses each way, 2 rounds, 0.35 s apart: **725 frames, 142 janky (19.59%), 50th 42 ms, 90th 53 ms, 99th 69 ms**. No earlier number to compare with, and the photo look was on. That is near 24 fps at the median

### 6b ✗ `parsed` far below `asked`, again
`live status=200 asked=6 parsed=1 bytes=1440 reason=ok` on every ask (30b: asked=9 parsed=4). The list here is Tadawul stocks, with the market closed. Only one symbol comes back and the log gives no names, so I can't say which, or whether it would be 6 during a session.

### 7 Low RAM: on, as far as can be seen
MemTotal 2,451,284 kB (2,394 MB, under the 2,560 limit); `ro.config.low_ram` empty.
- ✓ Markets open in «عادي» after a force-stop (no data wiped: the rule is set on every open; shots L-0)
- **`lowram=1` indirect ✓:** the provider refuses shell reads (Permission Denial), the app is not debuggable, so I can't read the URL. With `seconds on`, the page showed no seconds in 3 shots, as `lowram` requires. `seconds` back off
- **`UrTVType` not testable:** 0 lines. Our typing service is not enabled on this TV (`enabled_accessibility_services` has only another app), and I don't toggle services
- Worse or better by feel: not judged, no person at the TV

### 8 ✗ Chromecast logic on this TCL
`optimize/dock.sh cast status` says: «Chromecast: the setup wizard holds the cast credentials. Wizard: off. Home: sa.urtv.home/.MainActivity. Choice: on». **It should say not a Chromecast.** `getprop ro.product.manufacturer` is TCL, but `com.google.android.tungsten.setupwraith` here does hold `…castinfo.CastInfoProvider`, so the provider test gives a false match.
- ✓ Status changed nothing: disabled list the same (21, wizard still off), Home the same, no `cast.txt`
- Risk, read from the script and not run: `cast on` here would `pm enable` the wizard and restart the cast receiver
- Small: on a clean PC `cast.sh` line 26 prints «optimize/run/cast.txt: No such file or directory», because the `2>` comes after the `<`

### 9 ✓ Mute log
`logcat -d -s UrTVMute`: 0 lines, and 0 `by=not us`, at 02:07 and at 02:23. I cleared the main log twice between (02:08 and about 02:17) for the markets count, so the second read covers only the time after that. Nothing played, so this does not show the logging works. Muted at every check.

### Z
`import settings-before.json` (preview: only teams changed; `--yes`), look glass, seconds off, updatechannel latest. `show` before/after differs only in the build. Same: dock, PIN and locked list, tasks, photos, adhan, family DNS, disabled (21), packages (177), Home holder, saver. Muted. **restored: yes**; only the build differs (191).

### step 31 summary: ✓ 1, 3, 5, 9, settings timing. ✗ 2 (part), 6b, 8. Not testable: 4, `UrTVType`, a live tick with moving prices.
Problems (4 + 1 small): (1) stock card «رجوع» shows no label; (2) stock card first focus is on the content, not «رجوع», with no ring; (3) `cast status` calls this TCL a Chromecast; (4) `parsed=1` of `asked=6`; (small) pad zero looks like a Latin 0. Numbers to weigh: tray 19.59% janky, 8 frames in 190 s on unchanged live ticks.
Shots: PC only, none pushed.
