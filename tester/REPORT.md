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
