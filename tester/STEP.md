«✅ GO — steps 47 and 48 on build 218» — thanks for 46 and 45. The build is fixed: apk-next is 218 (hub + clean home + quiz). Run step 47 then step 48 (end of this file) on build 218 or newer. TV muted. The owner is awake now but do not wait on him; in step 47 item 5 stop if the family PIN is asked. The factory-interface step stays forbidden. lang ar stays.

«If the tester isn't polling: the owner starts it with the paste-in instruction the supervisor gave him.»

**1. The PIN recovery code is NOT needed for the next run (step 28): do not ask for it. (When a step needs it again, it will say so here. The rule stays: never in a file, log, commit, script or screenshot.)**

**2. WAIT for the GO line. Every 5 minutes, run `git fetch origin work/tester` and read the first line of `origin/work/tester:tester/STEP.md` (`git show origin/work/tester:tester/STEP.md | head -1`). Don't start until it reads «✅ GO — build N». Then make sure the TV has build N or newer from apk-next, and run **step 28** (at the end of this file): parts A, B, C, D, E, then Z. Steps 16–27 are done; do not run them again.**

**3. Keep the TV MUTED the whole time: check the mute before each step and after every restart, update or install, and mute again at once if it isn't.**

steps 16–20 received; fixes in progress on work/tester-fixes; next step after build 176+

Answer to the question in REPORT.md (do you want the pictures?): «pictures: no». The text report was enough and the fixes are in progress; keep the shots on the PC, push none.

«المالك مو عند التلفزيون؛ المختبر يسوي كل شي» — the owner is not at the TV and has allowed the tester to do everything: every key, every prompt, every restart, by adb (`input keyevent`, `uiautomator dump`, `screencap`). Do the steps in this order: 16, 17, 18, 19, 20; then 22 (build 178 or newer); then 21 last (build 177 or newer). Step 18 uninstalls and reinstalls our apps: it does NOT invalidate 16 and 17 (they run first, on the installed apps, and their Z rows put everything back before 18 saves the settings), but do not run 16 or 17 again after 18 (the TV is then a fresh install with restored settings, not the owner's original one). Step 19 plays a station on the TV, so it comes after 18 has put the home app back.

step: 14
from: UrTV supervisor
what: the first REAL test of setup without a computer: «إعداد تلفزيوني» (docs/PHONE-SETUP.md, path 1) on the owner's TV, with the latest build. The owner asked for it. Follow tester/RULES.md. No owner needed, except the TV's «السماح بتصحيح USB؟» prompt, which you answer with keys over adb. The phone-page check is NOT this step; it comes later. TV sound stays at 0 the whole time.

Read docs/PHONE-SETUP.md section 1 ("The TV alone") and its "The TV test this path still needs" list first. This step is that list.

Before anything:
- Start lines as before, plus who holds Home now: `adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME`, and `adb shell settings get global adb_enabled`.
- `optimize/saver.sh updatechannel next`; update from the TV («تحديث تلفزيوني») to the newest apk-next, as in step 13.
- The app's adb talks to the TV's own adbd on 5555. Check `adb shell getprop service.adb.tcp.port` and `adb shell getprop persist.adb.tcp.port`. If the TV offers ONLY Wireless debugging (no 5555), write that down, answer row S0 and STOP: that is the result.
- Never switch USB debugging off. It would cut your own connection. Row 4 of the doc's list is skipped for that reason.
- If anything leaves the TV without a working Home for more than 10 minutes, stop and put it back with the computer path (`optimize/install.sh` / restore, as you know them). The owner's TV must end exactly as it started.

| # | Do | Shots (local) | Right when |
|---|---|---|---|
| S0 | Open «إعداد تلفزيوني» from the TV's apps row (or `adb shell am start -n sa.urtv.home/.setup.SetupActivity`) | s0-screen | it lists exactly what it will do; «ابدأ» is there |
| S1 | «ابدأ». When «السماح بتصحيح USB؟» appears, choose «السماح دائماً» with keys (`input keyevent` 21/22/20 then 23). Our home is ALREADY Home on this TV, so this is the "already set up" run | s1-prompt, s1-done | the prompt appears once; every step ends ✓ or «تُركت» with a reason; it says our home is already Home; nothing breaks; the done screen tells you to turn debugging off and revoke |
| S2 | «ابدأ» a second time | s2 | the TV asks again (the key was deleted after S1: check that the app's key file is gone, `run-as` if possible, or by the fact that it asks again); a clean repeat |
| S3 | «ابدأ», and this time DENY the prompt (choose «إلغاء/رفض») | s3 | «لم تُقبل رسالة …»; nothing changed (Home, the screensaver, the disabled apps, all the same as before) |
| S4 | «تراجع» | s4 | the stock home is Home again (resolve-activity says so), and what THIS app turned off is back on (if it turned nothing off, nothing changes); the screensaver as the TV had it; no crash |
| S5 | «ابدأ» again: now it really switches Home to ours, with the safety switch | s5 | our home is Home; the journal says so; the safety switch is cancelled once our home is confirmed on screen |
| S6 | Safety switch, restart: «تراجع» (stock home), then «ابدأ», and while the home switch is pending (before it is confirmed) `adb reboot`. Reconnect when the TV is back | s6 | within 5 min of the deadline the stock home comes back by itself; the journal says «restored» |
| S7 | The same as S6 with `adb shell am force-stop sa.urtv.home` instead of the reboot | s7 | the same: the stock home comes back, the journal says «restored» |
| S8 | If a family PIN is set: «ابدأ» and «تراجع» ask for it. If none is set: skip it (do not set one) | — | asks for the PIN |
| Z | Leave our home as Home (a final good «ابدأ» run, or the computer path); `optimize/saver.sh updatechannel latest`; restore everything from the start lines, including the disabled-apps list (compare `pm list packages -d` with the start); USB debugging stays as it was; sound 0 | — | "restored: yes/no", with the Home holder line and the disabled-apps count before and after |

REPORT.md: "step 14 (setup without a computer)", the table, the exact lines the screen showed at each step, the journal/ledger contents (no personal data: no Wi-Fi name, no IPs, no serials), any `!!`, and every surprise. Push the shots to the report branch. Tell the owner in Arabic that it is done and how many problems there were.

---
Side task D (any time, the owner logs in himself): Downloader short codes on aftv.news
- The owner made an aftv.news account. He logs in himself in the PC's browser. Never type, store or report his password or email.
- Register two short codes (aftv.news "shorten URL" for Downloader), exactly these URLs:
  - D1 `https://github.com/tshi-dev/UrTV/releases/download/apk-latest/urtv-home.apk`
  - D2 `https://github.com/tshi-dev/UrTV/releases/download/apk-latest/urtv-screensaver.apk`
- Check each code: open `https://aftv.news/<code>` and it must redirect to the exact URL above (curl -sI shows the Location).
- REPORT.md: "side task D", the two codes and the redirect check. The codes are public, so they're fine in the report; nothing about the account.

---
step: 15 (do it when the owner says «ابدأ الخطوة 15»; it can come before the rest of step 14)
what: confirm the fixes from the 159 test AND the new batch (Settings key, 24-hour row, leagues save, mirroring tile, world cities) on the newest apk-next (build 163 or newer: «Accessibility after the app's own update…» and the rest). TV sound 0 the whole time.
- Start lines; `adb shell settings get secure enabled_accessibility_services`, `settings get secure accessibility_enabled`, `dumpsys accessibility | grep -A5 "Bound services"`, `appops get sa.urtv.home AUTO_START`, `appops get sa.urtv.home APP_AUTO_START`, `dumpsys audio | grep -i -E "muted|master"`. The owner first turns Key Mapper and our services back on if they are off (a restart, or `optimize/dock.sh type-setup`).

| # | Do | Right when |
|---|---|---|
| F1 | Mute the TV. Update from the TV («تحديث تلفزيوني») to the newest build. About 15 s after the install: `logcat -d -s UrTVA11y`, accessibility_enabled, Bound services, appops again | a "heal" line then "healed"; accessibility_enabled=1; ours bound (say whether Key Mapper is bound too) |
| F2 | Only if F1 did not bind ours: `optimize/dock.sh type-setup` | it says the same list was written again, and ours binds. The phone page, if opened before that, says «التحكم في التطبيقات الثانية متوقف: …» |
| F3 | Mute check: after F1, `dumpsys audio`; then mute, `adb reboot`, check again | write down whether the update and the reboot unmuted (that part is the TV's, not ours) |
| F4 | Adhan on a muted TV: mute, `optimize/music.sh adhan now`, stop it from the page («أوقف الأذان الآن») | after it the TV is muted again; unmute shows the old level, not 0 |
| F5 | Phone control on, the code shown, wait past the 45 s screensaver delay, wake the TV | the code screen and the page address still work. Then Home with nobody paired: control stops |
| F6 | «إعداد تلفزيوني»: one OK on «ابدأ» | it starts («جارٍ الإعداد…»); then DENY the USB prompt (no key given). Nothing changes |
| F7 | Phone page (paired from the PC's Chrome): one swipe on the pad; «بدون» + «حفظ البطاقات» once; the next-adhan wording; the screensaver delay shown; the text field with the keyboard open | one swipe = one tile; saved on the first press; «8 ساعات» style counting; «45 ثانية»; the field above the tab bar |
| F8 | Settings key outside our home: `adb logcat -c`, open any other app or input (HDMI if a source is connected, else YouTube), press the remote's Settings key; `logcat -d -s UrTVType` | the TV's settings open (`settings key=open`); arrows/OK/Back/volume act as before with no UrTVType line for them; on our home the key still opens OUR settings |
| F9 | Screensaver settings → «نظام الساعة» → «٢٤ ساعة»; check the screensaver preview, the home clock (within a minute) and «تلاوة وأذان»; then back to «١٢ ساعة» | 24-hour everywhere, then ص/م back |
| F10 | «الدوريات كاملة»: ▼ to the end, ▼ again reaches «حفظ»; tick one league, «حفظ»; reopen and untick it, «حفظ» | «حفظ» visible and reachable; saved; put back exactly |
| F11 | Home → «مركز التحكم» | «انعكاس الشاشة» tile with its line; all tiles inside the screen; OK opens the TV's mirroring app; Back returns |
| F12 | Cities: settings → «الموقع والصلاة» → «الدولة»: scroll the list; pick «تركيا» → a city shows «إسطنبول · Istanbul» style names; then search «London» in «المدينة» (do NOT save); then put the real city back exactly | the long country list scrolls fine; names in both languages; no «إسرائيل» and no «الصحراء الغربية» anywhere in the country list; real city restored |
| Z | Phone control off, the pairing removed; updatechannel latest; restore everything from the start lines; sound 0 | "restored: yes/no" |

REPORT.md: "step 15", the table, the UrTVA11y lines, any `!!`. Push; tell the owner in Arabic.

---
step: 16 (do it first; the owner is not needed)
what: confirm the 4.2 batch on `next` (the adhan options, the text field in «المهام», the TCL remote keys and the safe fixes, and a read-only look at the Wi-Fi owner) on the newest apk-next: build 169 or newer (`version.txt` 3.169; the build after 167). Follow tester/RULES.md. TV sound stays at 0 the whole time: judge the adhan by logcat and the state, never by ear.
- **Keys by adb, the owner is not at the TV.** Every press in this step is yours: `adb shell input keyevent` with DPAD_UP/DOWN/LEFT/RIGHT (19/20/21/22), DPAD_CENTER (23), BACK (4), HOME (3); a long OK is `input keyevent --longpress 23`. Before each OK, check what has the focus: `adb shell uiautomator dump /sdcard/ui.xml && adb shell cat /sdcard/ui.xml` (the node with `focused="true"`), or `adb exec-out screencap -p > <shot>.png` and look. Shots stay on the PC. A restart is `optimize/venv/bin/python -I optimize/tv.py --reboot`, never a plain `reboot`.
- Start lines, plus: `optimize/music.sh adhan` (write down every `adhan_*=` value), `optimize/saver.sh prayeradj` state, the tasks count (`optimize/saver.sh tasks list`; count only), `adb shell pm list packages -d | sort` (keep the list), `adb shell settings get global cached_apps_freezer`, and who holds Home (`adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME`).
- `optimize/saver.sh updatechannel next`; update from the TV («تحديث تلفزيوني») to the newest apk-next; settings «الإصدار» shows the build.
- The adhan state: `optimize/music.sh adhan` → the line `adhan_alarm_last=` reads `front= play= notice= saver=`. To make an alarm come, move the next prayer 1–2 minutes ahead with `optimize/saver.sh prayeradj <prayer> <minutes>` (on, then the minutes), wait for it, read the state and `adb logcat -d -s UrTVAdhan`; put the minutes back right after each row.
- Wi-Fi: read only. Never forget a network, never turn Wi-Fi off, never change it.

| # | Do | Right when |
|---|---|---|
| A1 | «الأذان على شاشة التوقف فقط» on (`optimize/music.sh adhan saver-only on`), the adhan on. An alarm with another app in front (YouTube) | `play=0`, `front=other`; logcat «adhan skipped» |
| A2 | The same with our home in front, then with the screensaver showing | `play=1` both times (`front=home`, `front=dream`); stop it at once («أوقف الأذان الآن» or a key) |
| A3 | saver-only off. «تنبيه عند الأذان» on (`adhan notice on`) with the adhan OFF. An alarm with the home in front; again with an app in front; again on HDMI if a source is connected | a 5-second text notice «حان وقت صلاة …» each time, then it goes by itself; `notice=1`; no sound |
| A4 | «تكبير فقط» on (`adhan takbir on`); look at «تلاوة وأذان» ← الأذان | «أذان مختصر» turned off by it; `adhan_takbir=true`, `adhan_short=false` |
| A5 | `optimize/music.sh adhan now`, then `optimize/music.sh` status every few seconds | it stops by itself at about 14 s (a short fade); a voice preview in the settings is NOT cut |
| A6 | «شاشة التوقف وقت الأذان» on (`adhan saver-at on`). An alarm with our home in front; again with another app in front | home: the screensaver starts (`saver=1`); other app: nothing starts (`saver=0`) |
| A7 | All four off again (saver-only, notice, takbir, saver-at); open «تلاوة وأذان» ← الأذان | all four rows read «متوقف»; `optimize/music.sh adhan` shows them false |
| T1 | Open «المهام» | the text field has the focus (gold, check with the dump); OK opens the TV keyboard; type `test` with `adb shell input text test` (`input text` cannot type Arabic, so the test task is «test», not «تجربة»); the keyboard's action key reads «إضافة» (screencap); press it (DPAD to it and 23, or `input keyevent 66`); the task is added and the field is empty again |
| T2 | OK on «إضافة» with the field empty | «اكتب المهمة أولاً»; nothing added |
| T3 | D-pad ▼▲◀▶ between the field, «إضافة» and the mic | every one is reachable and shows focus; no dead spot |
| T4 | `adb shell am start -a android.speech.action.RECOGNIZE_SPEECH` (say whether a recognizer opens), then back in «المهام» the mic button, if shown | the button either listens, or the window closes and the button hides with a message; no dead button. Delete the test task |
| K1 | `adb logcat -c`; our home in front; send the remote's Netflix, YouTube and Prime Video keys yourself: `adb shell input keyevent 4062`, then HOME, `4065`, HOME, `4099` (HOME between them); `adb logcat -d -s UrTVKeys` | each opens its app; the log shows `key=4062` / `4065` / `4099`. Note: an injected key proves OUR mapping only; that the real remote sends these codes is not checked here (write «injected») |
| K2 | The Settings key on our home: `adb shell input keyevent 4077`, then (after Back) `input keyevent 83`; `logcat -d -s UrTVKeys` | `key=4077` / `83`, and OUR settings open (injected, as in K1) |
| K3 | `optimize/tweaks.sh apply`, then `settings get global cached_apps_freezer` | no change on Android 14 (no `!!`); the freezer value the same as at the start |
| K4 | The screensaver (preview from settings); look at the Arabic line under the clock and the card labels | the Arabic letters are joined, no gaps between them |
| K5 | `adb shell pm list packages -d` | `com.android.sdksandbox` is not in it; the list the same as at the start |
| W1 | `optimize/tv.py "dumpsys wifi" \| grep -o 'cname=[^ ]*' \| sort \| uniq -c` | write the counts only (package names, never a network name) |
| W2 | `adb shell cmd wifi status`, then `adb shell cmd wifi list-networks` | «connected»; a header and rows (count the rows; do not copy names) |
| W3 | `optimize/install.sh launcher` | «nothing to switch» (Home is already ours), no pause, no `!!` |
| Z | All four adhan options as they were at the start; the prayer minutes back exactly; the adhan on/off as at the start; the test task gone; `optimize/saver.sh updatechannel latest`; the disabled-apps list and Home holder as at the start; sound 0 | "restored: yes/no" with the Home holder line and the disabled-apps count before and after |

REPORT.md: "step 16", the build, the table, the `adhan_alarm_last=` values (front/play/notice/saver only, never a prayer time), the UrTVKeys key numbers, the W1 counts, any `!!`. No personal data: no city, prayer times, task text beyond «test», Wi-Fi names, IPs or serials. Push; tell the owner in Arabic that it is done and how many problems there were.

---
step: 17 (do it right after 16; the owner is not needed)
what: confirm the home batch and the screensaver batch on `next` (version 4.2) on the newest apk-next: build 170 or newer (`version.txt` 3.170). Follow tester/RULES.md. TV sound stays at 0 the whole time. Nothing is uninstalled, nothing is forgotten; every setting goes back to its start value at the end.
- **Keys by adb, the owner is not at the TV.** Every press in this step is yours: `adb shell input keyevent` with DPAD_UP/DOWN/LEFT/RIGHT (19/20/21/22), DPAD_CENTER (23), BACK (4), HOME (3); a long OK is `input keyevent --longpress 23`. Before each OK, check what has the focus: `adb shell uiautomator dump /sdcard/ui.xml && adb shell cat /sdcard/ui.xml` (the node with `focused="true"`), or `adb exec-out screencap -p > <shot>.png` and look. Shots stay on the PC. A restart is `optimize/venv/bin/python -I optimize/tv.py --reboot`, never a plain `reboot`.
- Start lines, plus: `optimize/saver.sh show` (write down `verse`, `photos`, `occasion`, `nightdim`, `taskspill`, `backsaver`, `slot`/content of the card beside the clock), `optimize/dock.sh backsaver` (on/off), `adb shell pm list packages | wc -l` (count only), and who holds Home (`adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME`).
- `optimize/saver.sh updatechannel next`; update from the TV («تحديث تلفزيوني») to the newest apk-next.

| # | Do | Right when |
|---|---|---|
| V1 | Settings ← «الجوال والتحديث» ← the version lines | «4.2» and the build number |
| H1 | Our home: DPAD to an installed store app (not ours, not a system app), then a long OK (`input keyevent --longpress 23`) | under the menu's last item two small buttons «تفاصيل التطبيق» and «حذف التطبيق» |
| H2 | «تفاصيل التطبيق» | the TV's app-info page of that app opens; Back returns |
| H3 | Long OK again ← «حذف التطبيق» | Android's own uninstall question appears; move the focus to **Cancel** (never OK) and check in the dump that Cancel (`android:id/button2` or its text) is the focused node BEFORE pressing 23; if the dump cannot show it, use a screencap; if you are not sure, press BACK instead; the app is still there (package count the same) |
| H4 | Long OK on a system app and on «شاشة تلفزيونك» | no «حذف التطبيق» for them |
| H5 | Look at the app menu items («تحديث تلفزيوني», «تلاوة وأذان», «الرقابة الأبوية»), «كل المصادر» in the inputs list, and in the screensaver settings «بحث عن مدينة أو قرية», «تصفح الدوريات», «بحث عن أي فريق أو منتخب» | no trailing «…» on any of them |
| H6 | The remote's Settings key on our home: a short press (`input keyevent 4077`); then a hold (`input keyevent --longpress 4077`, and if that is not seen as long, `input keyevent --duration 1000 4077`) | short: the TV's own settings; long: OUR settings. `adb logcat -d -s UrTVKeys` shows the key (injected, as in step 16 K1) |
| H7 | «مركز التحكم» (the round button at the top) ← tile «البلوتوث» | the TV's remotes-and-accessories screen opens; Back returns. Pair or forget nothing |
| H8 | Settings ← «الشاشة الرئيسية» | «الرجوع يفتح شاشة التوقف» right after «زر المهام», reading «متوقف» (unless the start line said on) |
| H9 | Turn it to «مفعّل»; Home; wait for the home (up to a minute); Back on the home | the screensaver starts; `optimize/dock.sh backsaver` says `back-saver: on`; Back or Home leaves the screensaver |
| H10 | In the apps grid (scrolled down) press Back | the focus goes to the favourites first; no screensaver |
| H11 | Turn the row to «متوقف»; Home; wait a minute; Back on the home | nothing starts; `dock.sh backsaver` says off |
| H12 | `optimize/dock.sh backsaver on`, then open Settings ← «الشاشة الرئيسية» | the row reads «مفعّل»; then `optimize/dock.sh backsaver off` and the row reads «متوقف» |
| H13 | (only if «السماح بتصحيح USB؟» shows up by itself during this step; never force it) | the screensaver does not cover the prompt; `adb logcat -d -s UrTVSaver UrTVSetup` says why |
| S1 | Card beside the clock = the verse card; Settings ← «بجانب الساعة» ← «تبديل الآية والحديث» ← «كل ٣ دقائق»; screensaver preview for 7 minutes | the card changes about every 3 minutes, «آية كريمة» then «حديث شريف» |
| S2 | Settings ← «تبديل الصورة كل» | «دقيقة واحدة» is offered; «٣٠ ثانية» only on a TV with 2 GB. `optimize/saver.sh photos 30s`: accepted, or refused with the MB the TV has (write that line) |
| S3 | Photos every 1 minute (or 30 s), screensaver preview for 3 minutes | the photo changes on time |
| S4 | «العرض» ← «المناسبة القادمة» off; preview | the bottom middle is empty; nothing else moved |
| S5 | «العرض» ← «تعتيم الليل» off | the row reads off; `saver.sh show` says `nightdim: off` |
| S6 | «الشاشة الرئيسية» ← «زر المهام» مخفي; back home (up to a minute) | the «المهام» button is gone from the top row; the «المهام» card still works |
| S7 | Settings ← «الجوال والتحديث» | «تحديث صفحة شاشة التوقف» (not «تحديث الشاشة الآن»), its line points to «تحديث تلفزيوني», which says «تحديث التطبيقين»; OK on the page row gives «تم التحديث» or «الصفحة محدثة» |
| S8 | «تلاوة وأذان» ← الإذاعات | NO row «البث المباشر من الحرم» (its link is still empty, so it must not show) |
| Z | Back to the start values: `verse`, `photos`, `occasion`, `nightdim`, `taskspill`, the card beside the clock, `dock.sh backsaver`; `optimize/saver.sh updatechannel latest`; package count and Home holder as at the start; sound 0 | "restored: yes/no" with the before/after values of each |

REPORT.md: "step 17", the build, the table, the UrTVKeys key numbers, the S2 line (MB only), any `!!`. No personal data: no city, prayer times, app names beyond «store app»/«system app», Wi-Fi names, IPs or serials. Push; tell the owner in Arabic that it is done and how many problems there were.

---
step: 18 (do it after 17; the owner is NOT at the TV: you do every key and every prompt)
what: from scratch: «جهّز كل شي». The whole setup from the TV alone, on the owner's TCL (Android 14), with build 171 or newer from apk-next (`version.txt` 3.171). Our two apps are removed, only the home app is installed again, and the one button does the rest. Follow tester/RULES.md. You work from the PC's adb. TV sound stays at 0 the whole time. Nothing personal goes in the report: no city, prayer times, task text, Wi-Fi names, IPs, serials or app names beyond «store app»/«system app».
- Keep every file of this step in `optimize/run-step18/` on the PC (ignored by git, and `reset.sh` does not move it). Never commit or push anything from it.
- **The owner has accepted the uninstall's data loss:** the parental PIN, his own photos, his own adhan recording and the phone pairings. Do not ask him again; go on. Still save everything you can (row 1) and put back everything you can (row 12).
- Keys by adb: `adb shell input keyevent` DPAD_UP/DOWN/LEFT/RIGHT (19/20/21/22), DPAD_CENTER (23), BACK (4), HOME (3). Before each OK, check the focus: `adb shell uiautomator dump /sdcard/ui.xml && adb shell cat /sdcard/ui.xml` (the node with `focused="true"`); when the dump fails or comes back empty (it can, on a system dialog), `adb exec-out screencap -p > optimize/run-step18/<name>.png` and look at it. A restart is `optimize/venv/bin/python -I optimize/tv.py --reboot`, never a plain `reboot`.
- If the TV is left without a working Home for more than 10 minutes, stop and put it back with the computer path (`optimize/install.sh`, then `optimize/saver.sh import`), as in step 14.
- Never switch USB debugging off during the step: it cuts your own connection, and «تراجع» needs it.
- The TV restarts during the run: reconnect adb after each restart, and only read (no command that changes anything) until the run says it is done.

| # | Do | Right when |
|---|---|---|
| 1 | **Save the owner's settings.** Start lines, then into `optimize/run-step18/`: `optimize/saver.sh export > optimize/run-step18/settings.json` (one JSON file with both apps' settings: city, prayer method and minutes, cards, markets, teams, the adhan; the home app gives the adhan part). It does NOT carry everything, so also save: `optimize/saver.sh show`, `optimize/music.sh adhan` (every `adhan_*=` line, and whether the adhan is on), `optimize/dock.sh show`, `dock.sh names`, `dock.sh hidden`, `dock.sh input-names`, `dock.sh backsaver`, `dock.sh pin status` (the locked list; never the PIN), `dock.sh familydns status`, `optimize/saver.sh tasks list` (private: on the PC only, never in the report), `optimize/photos.sh list` and `photos.sh` mode, `optimize/saver.sh updatechannel` state, and the other `saver.sh show` values the export does not carry (`appearance`, `watchnext`, `updatebell`). And the TV: `adb shell pm list packages -d \| sort`, `adb shell pm list packages \| sort`, `adb shell cmd role get-role-holders android.app.role.HOME`, `adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME`, `adb shell settings get secure screensaver_components`, `settings get secure screensaver_enabled`, `settings get system screen_off_timeout`, `settings get secure enabled_accessibility_services`, `optimize/tweaks.sh status`, and the appops: `appops get sa.urtv.home` and `appops get sa.urtv.screensaver` (all of it), plus `appops get <each> AUTO_START`, `APP_AUTO_START`, `ASSOC_START`. **Lost with the uninstall, accepted by the owner:** the parental PIN (it cannot be read back; the locked list can), his own photos and own adhan recording (they live only inside the apps; write down only how many photos and whether a voice is «own»), the phone pairings | `settings.json` is valid JSON with `"schema":1`; every list above saved |
| 2 | **Reset.** Read `optimize/reset.sh` first. What it does: (1/4) re-enables every package of our lists (`tcl-safe`, `tcl-optional`, any `mine*.txt`, the community list the setup used) with `restore.sh`, presses Home until the stock home is on screen (it stops with `!!` before removing anything if it is not), and turns back on what `keep.sh` protects; (2/4) the TV's own screensaver back (ours deleted, the values `backup.sh` saved in `optimize/run/backup.txt` put back); our accessibility entry out of the list; the family DNS off (Private DNS as it was); the permissions it granted revoked; (3/4) `SCHEDULE_EXACT_ALARM` and `REQUEST_INSTALL_PACKAGES` back to what was recorded, then our apps uninstalled (old and new names), and TV Bro / SmartTube only if the setup installed them (not in the saved package list); (4/4) the tweaks back to the journal's values; then `optimize/run/` is moved to `optimize/run-<date>/`, the kept-on list cleared, and it prints what is still disabled. Run `optimize/reset.sh` and keep its output. If it asks a question, answer it from what this step says (the owner has accepted the loss); if it stops with `!!`, do what the line says, as always. Then `adb uninstall sa.urtv.home` and `adb uninstall sa.urtv.screensaver` (they may say «not installed»: reset removed them). Then `pm list packages -d`, the home role and `screensaver_components` again | no `!!`; the stock home on screen; neither `sa.urtv.home` nor `sa.urtv.screensaver` in `pm list packages`; the disabled list now only the manufacturer's (write the count) |
| 3 | **Install only the home app, from apk-next.** `curl -fLo optimize/run-step18/urtv-home.apk https://github.com/tshi-dev/UrTV/releases/download/apk-next/urtv-home.apk`, then `adb install optimize/run-step18/urtv-home.apk`. (Downloader code 9378345 points at apk-latest, 4.1, which has no «جهّز كل شي» yet: not used here.) `adb shell dumpsys package sa.urtv.home \| grep -E "versionName\|versionCode"` | `Success`; versionName 4.2, versionCode 171 or newer; the screensaver app is NOT installed |
| 4 | **The prompt, answered by you.** `adb logcat -c`. Open «إعداد تلفزيوني» (`adb shell am start -n sa.urtv.home/.setup.SetupActivity`), DPAD to «جهّز كل شي» (check the focus in the dump) and 23. When «السماح بتصحيح USB؟» appears, press nothing for 20 s, then screencap the app's screen behind it (`s4-20s`). Then answer it: (a) dump; find the «السماح دائماً من هذا الكمبيوتر» checkbox (`checkable="true"`) and the «السماح» button (`android:id/button1`, or its text); (b) DPAD (19/20, then 21/22) until the checkbox is `focused="true"`; 23; dump again: it must read `checked="true"`; (c) DPAD to «السماح» until it is `focused="true"`; 23. If the dump fails on this system dialog (empty, or «could not get idle state»), do each move with a screencap instead and look where the focus ring is; never press 23 without having seen the focus on the right control. This is safe: your PC's key is already authorized, so your adb stays up whatever the prompt does. Keep `adb logcat -d -s UrTVSetup` | after 20 s the screen shows «ما ظهرت الرسالة؟ أعد تشغيل التلفزيون…»; the TV asks once; «always allow» was checked before «السماح»; after it the plan screen comes |
| 4b | **A stuck prompt** (only if it comes: the prompt never shows within 60 s, or keys do not reach it, or it stays on screen after «السماح», or the app still waits after it). Screencap and dump it (`s4b`), save `logcat -d -s UrTVSetup`, then restart the TV yourself: `optimize/venv/bin/python -I optimize/tv.py --reboot`. Reconnect, check Home, then do row 4 again from «إعداد تلفزيوني». At most two restarts for this; after that write the result and go to row 12 | after the restart the prompt comes and row 4 passes; write how many restarts it took and what the screen showed |
| 5 | **The plan.** Read every line of the plan screen (dump the text; screencap `s5`). `adb shell pm list packages -d \| sort` again. Leave the box «ثبّت أيضاً TV Bro (متصفح) و SmartTube…» checked (dump: `checked="true"`) | the counts are real numbers for this TV (e.g. «نوقف N تطبيقاً من القائمة الآمنة»), not «نوقف تطبيقات…»; it says the TV restarts twice if it does; the disabled list is EXACTLY as in row 2 (nothing changed before «ابدأ») |
| 6 | **The run.** DPAD to «ابدأ» (check the focus) and 23. Then leave the TV idle (no keys from you). The TV may restart twice: reconnect each time and check the run goes on by itself. Copy every step line as it ends, with its mark (✓ done / – left / ✕ failed), from the dump. Every few minutes: `adb shell dumpsys activity activities \| grep -E "mResumedActivity\|topResumedActivity"` and `adb shell dumpsys dreams \| grep -iE "dreaming\|mCurrentDream"` | it ends by itself; no ✕ (a – has its reason); after each restart it continues; our screensaver never shows during the run (note: the run installs the screensaver from apk-latest, 4.1, which does not ask the launcher whether a setup runs; from then on only the run's wake lock keeps it away, so a screensaver here is a problem to report, with the time in the run) |
| 7 | **The Wi-Fi pause** (only if it comes: it shows when the setup wizard owns the network, «شبكة الواي فاي محفوظة باسم «وسيط الإعداد»…»). Do NOT forget the network or rejoin it: that needs the owner at the TV and the password, which you never touch, know or type (AGENTS.md); write «forget and rejoin: needs the owner (later)». Instead: screencap and dump the pause (`s7`, no network name in the report), save `logcat -d -s UrTVSetup` and `adb shell cmd wifi status`; the screen offers «… اضغط «كمّل» بدونها، ونكمل والوسيط شغّال»: DPAD to «كمّل» (focus checked in the dump) and 23, WITHOUT forgetting anything. The run then goes on with the setup wizard left on. Record what the home switch did in that case: the `step` lines of `logcat -d -s UrTVSetup`, the home role and `resolve-activity` for HOME, whether the stock home was turned off, and whether `setupwraith` is still enabled (`pm list packages -d \| grep -c setupwraith`; read only, never change it). Then `adb shell cmd wifi status` and the home role every 30 s for 3 minutes, and once more after the run ends. If no pause came, write «no pause: the wizard does not own the network» | Wi-Fi «connected» the whole time, during and after the run; the run continues and ends after «كمّل» with the wizard on; the journal says what the home switch did with the wizard on (our home in front, and only then the stock home off, or the stock home kept with a reason); the network was never forgotten |
| 8 | **Done screen.** Read it (dump; screencap `s8`). Read the QR from the screencap on the PC: `zbarimg -q --raw optimize/run-step18/s8.png` (or Python with `pyzbar`/`opencv` if installed; install `zbar-tools` if neither is there). Open the URL it gives on the PC (`curl -sSL` and the PC's browser) | it says what to do next; the QR is a link to the report page (`docs/share`, `https://tshi-dev.github.io/UrTV/share/…`) with the report in it; no personal data on that page (look: no Wi-Fi name, IP, serial, city). If the QR cannot be decoded at all, write «QR not read» |
| 9 | **Check with adb.** `cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME`; `settings get secure screensaver_components`; `appops get sa.urtv.home AUTO_START`, `ASSOC_START`, and the same for `sa.urtv.screensaver` (and `APP_AUTO_START` if the TV knows it); `pm list packages \| grep -E "com.phlox.tvwebbrowser\|org.smarttube"`; `optimize/tweaks.sh status`; `pm list packages -d \| sort \| wc -l` against the plan's count; `optimize/music.sh adhan` and `adb shell dumpsys alarm \| grep -c sa.urtv.home` | home: `sa.urtv.home/.MainActivity`; screensaver: `sa.urtv.screensaver/.ScreenDream`; AUTO_START and ASSOC_START `allow` for both apps; TV Bro and SmartTube installed; the tweaks applied (animations at half, Play Store kept from the background); the disabled count = the row-2 count plus the plan's count (say any difference and which kind of app: «store app»/«system app»); the adhan is off on a fresh install (no alarm needed now; the alarm is checked again in row 12) |
| 10 | **The 4.2 screensaver.** The run installed the screensaver from apk-latest (4.1): the runner always takes `urtv-screensaver.apk` from apk-latest (`SetupInstall.screensaver()`, `UpdateRules.BASE`), whatever the update channel. `dumpsys package sa.urtv.screensaver \| grep -E "versionName\|versionCode"`, then `curl -fLo optimize/run-step18/urtv-screensaver.apk https://github.com/tshi-dev/UrTV/releases/download/apk-next/urtv-screensaver.apk` and `adb install -r optimize/run-step18/urtv-screensaver.apk`; the version again; `settings get secure screensaver_components` | before: 4.1; `Success` (same key, newer build); after: 4.2 and build 171 or newer; the screensaver component unchanged |
| 11 | **Undo and again.** Do it (the owner allowed everything; skip it only if row 7 stopped the run). «تراجع» on «إعداد تلفزيوني» (DPAD, focus checked, 23); answer any prompt as in row 4 (row 4b if it sticks); then the row-2 checks (home role, screensaver, `pm list packages -d`); then «جهّز كل شي» again (prompt as in row 4, plan, «ابدأ»), and row 9 and row 10 again | after «تراجع»: the stock home, the TV's screensaver, the disabled list as in row 2; the second run ends like the first |
| 12 | **Back to the owner's settings.** `optimize/saver.sh import optimize/run-step18/settings.json` (preview only), read it, then the same with `--yes`. Turn the adhan on again if row 1 says it was on (`optimize/music.sh adhan on`), and the four adhan options as in row 1. Put back by command what the export does not carry, from row 1: the dock order (`dock.sh set …`), the names, hidden apps, input names, `dock.sh backsaver`, the family DNS (only if it was on), the locked list (`dock.sh pin lock …` for each item; do NOT set a PIN: you do not know his, and a new one would lock him out; with no PIN nothing asks), the tasks (`saver.sh tasks add` each, from the PC file), `appearance`/`watchnext`/`updatebell` as in row 1, the update channel. Not restorable, accepted by the owner: the PIN, his photos (photo mode stays «mix»), his own adhan (if a voice was «own», leave the default voice), the phone pairings (you never pair a phone). Then save the same lists as in row 1 again and compare them (`diff`). `adb shell dumpsys alarm \| grep -c sa.urtv.home` if the adhan is on. USB debugging: leave it on (the tester needs adb) | the import says it applied, no `!!`; every row-1 value is back except the four accepted losses (the diff shows only those and what the setup changed on purpose: home, screensaver, disabled apps, tweaks, appops, TV Bro and SmartTube); at least one alarm when the adhan is on; sound 0 |
REPORT.md (on `work/tester-report`): "step 18 (from scratch: «جهّز كل شي»)", the build, the table, every step line of the run with its mark (✓ / – / ✕) and the reason of each – and ✕, the plan's lines (counts only), the disabled-apps count at rows 1, 2, 5, 9 and 12, the home and screensaver lines of rows 1, 2, 9 and 12, the appops of row 9, whether the screensaver ever showed during the run, whether the Wi-Fi pause came, what the home switch did after «كمّل» with the wizard on, and what Wi-Fi did after it, whether the QR opened the report page, any `!!`, and "restored: yes/no" with what differs from row 1. No personal data: no city, prayer times, task text, Wi-Fi names, IPs, serials, file paths with a user name. Push; tell the owner in Arabic that it is done and how many problems there were.

---
step: 19 (do it after 18; the owner is not needed): streams check
what: every live stream and radio URL the apps on `next` play, checked from the PC's network, and one played on the TV. Report only: change no code. TV sound stays at 0 the whole time.

**The URLs in the code on origin/next** (found with `git grep -n -i -E 'radiojar|mp3quran|qurango|HaramLive|sba\.sa|aloula|m3u8|\.mp3|\.aac|icecast|/radio/' origin/next -- 'android/**' 'src/**'`; run it again and add any URL that is new):

Live stations, `android/launcher/src/main/assets/quran/stations.json` (`optimize/music.sh radio N`, N = the line number below minus 1):

| # | Station | URL |
|---|---|---|
| L1 | إذاعة القرآن الكريم - السعودية (SBA, radiojar) | `https://stream.radiojar.com/0tpy1h0kxtzuv` |
| L2 | الشيخ أبو بكر الشاطري | `https://backup.qurango.net/radio/shaik_abu_bakr_al_shatri` |
| L3 | عبدالرحمن السديس | `https://backup.qurango.net/radio/abdulrahman_alsudaes` |
| L4 | سعود الشريم | `https://backup.qurango.net/radio/saud_alshuraim` |
| L5 | ماهر المعيقلي | `https://backup.qurango.net/radio/maher` |
| L6 | مشاري العفاسي | `https://backup.qurango.net/radio/mishary_alafasi` |
| L7 | سعد الغامدي | `https://backup.qurango.net/radio/saad_alghamdi` |
| L8 | ياسر الدوسري | `https://backup.qurango.net/radio/yasser_aldosari` |
| L9 | عبدالباسط عبدالصمد | `https://backup.qurango.net/radio/abdulbasit_abdulsamad` |
| L10 | محمد صديق المنشاوي | `https://backup.qurango.net/radio/mohammed_siddiq_alminshawi` |
| L11 | أحمد العجمي | `https://backup.qurango.net/radio/ahmad_alajmy` |

- **Cairo and Kuwait stations: there are none on origin/next** (the only Cairo/Kuwait hits are adhan recordings and prayer methods). Run the grep again with `cairo|kuwait|القاهرة|الكويت`; if you find a station URL, add it to the table as L12…
- **HaramLive:** `android/launcher/src/main/java/sa/urtv/home/tilawah/HaramLive.java` has `URL = ""` (the row is hidden), allowed hosts `sba.sa`, `aloula.sa`, `youtube.com`.
- Not live, but played from the same code: the reciters (`assets/quran/reciters.json`, 40 servers on `https://cdn.mp3quran.net/audio/<reciter>/<r1|r3>/`, a surah is `<server>NNN.mp3`) and the 21 adhan recordings (`tilawah/Adhan.java` `URLS`, on `archive.org/download/azaan-of-world-mosques_202608/`). Check them in row R2/R3 with the loop, not one by one.

Get the lists from the code itself (the repo on the PC, after `git fetch origin next`):
```
git show origin/next:android/launcher/src/main/assets/quran/stations.json | grep -o 'https://[^"]*' > optimize/run/streams-live.txt
git show origin/next:android/launcher/src/main/assets/quran/reciters.json | grep -o 'https://[^"]*' | sed 's/$/001.mp3/' > optimize/run/streams-reciters.txt
git show origin/next:android/launcher/src/main/java/sa/urtv/home/tilawah/Adhan.java | grep -o 'https://archive.org[^"]*' > optimize/run/streams-adhan.txt
```
One URL check (`$u` = the URL), from the PC's network:
```
curl -sSIL --max-time 15 "$u" | grep -iE '^HTTP/|^content-type:|^location:'      # every hop: status, type, redirect
command -v ffprobe && ffprobe -v error -rw_timeout 15000000 -show_entries format=format_name:stream=codec_name -of compact -i "$u"
# without ffprobe: read 5 seconds and count the bytes
curl -s --max-time 5 -o /dev/null -w '%{http_code} %{size_download} %{content_type}\n' "$u"
```
A live stream may answer HEAD with 400/405 and still play: then also do the 5-second read and judge by it (bytes > 20000 in 5 s = it streams).

| # | Do | Right when |
|---|---|---|
| R1 | L1–L11: the curl HEAD, ffprobe (or the 5-second read) for each | final status 200 (after redirects); content-type `audio/mpeg`, `audio/aac`, `audio/aacp`, `application/vnd.apple.mpegurl` or similar audio; ffprobe names a format (mp3, aac, hls) and an audio codec; or > 20000 bytes in 5 s. Write per station: status chain, content-type, format/codec or bytes |
| R2 | The reciters: the loop over `streams-reciters.txt` (HEAD only, plus ffprobe on the first 3) | 200 and `audio/mpeg` for each; list any that fail by its reciter id |
| R3 | The adhan recordings: the loop over `streams-adhan.txt` (HEAD only) | 200 (after archive.org's redirect) and an audio type; list any that fail by its number 1–21 |
| R4 | **One on the TV.** `adb logcat -c`; TV sound 0 (`adb shell cmd media_session volume --stream 3 --set 0`, then check `dumpsys audio`); open «تلاوة وأذان» ← الإذاعات with keys (DPAD, focus checked in the dump), pick L1 and 23; wait 30 s; `optimize/music.sh status`; `adb logcat -d -s Listening:V MediaPlayer:V ExoPlayer:V AudioTrack:W '*:E' \| grep -v -i -E 'wifi\|ssid'` (keep the full log on the PC); then stop it (`optimize/music.sh stop`). If L1 failed in R1, use the first station that passed | status says it plays L1; no «cannot play» line (tag `Listening`) and no player error; the screen shows the live state; sound still 0 |
| R5 | **HaramLive candidates** (report only; never edit `HaramLive.java`). Search the web for the official SBA live feed of Makkah and Madinah: SBA's Quran channel («قناة القرآن الكريم») and Sunnah channel («قناة السنة النبوية») on `sba.sa` / `aloula.sa` (their live pages and any HLS `.m3u8` they load: the browser's network tab), and SBA's official YouTube channels' live streams (the channel must be SBA's own: check it is linked from sba.sa or aloula.sa). Only official sources: no aggregator, no re-hosted stream. For each candidate: the page URL, the stream URL if one, which (Makkah/Madinah), why it is official (where it is linked from), and the R1 checks (curl, ffprobe) on it. Note whether it is on one of the allowed hosts (`sba.sa`, `aloula.sa`, `youtube.com`) and whether it needs a token or expires (try again after 30 minutes) | a list of candidates with their status; or «none found» with what was tried |
| Z | Music stopped; sound 0 as at the start; nothing changed on the TV | "restored: yes/no" |

REPORT.md (on `work/tester-report`): "step 19 (streams check)", the table, per URL the status chain, content-type and format/codec (or bytes), every failure, the R4 log lines (no Wi-Fi names, IPs or serials), and the R5 candidates with their status. Public URLs of the streams are fine in the report. Push; tell the owner in Arabic that it is done and how many streams failed.

---
step: 20 (do it after 19; the owner is not needed): English
what: the full English interface on `next` (d93485c or newer) with build 174 or newer from apk-next (`version.txt` 3.174). Follow tester/RULES.md. TV sound stays at 0 the whole time. Every key is yours (`input keyevent`, focus checked with `uiautomator dump` or `screencap`, as in step 18). Shots stay on the PC (`optimize/run/step20/`) until they are cleaned for the report (see the last line).
- Start lines, plus: `optimize/saver.sh show` (keep all of it, `lang:` included), `optimize/saver.sh export > optimize/run/step20/settings-before.json`, `optimize/music.sh adhan`, `optimize/dock.sh show`, `dock.sh backsaver`, and who holds Home (`cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME`).
- `optimize/saver.sh updatechannel next`; update from the TV («تحديث تلفزيوني») to the newest apk-next; settings «الإصدار» shows 174 or newer for both apps.
- **Arabic first (the "before" set).** With `lang: ar`, take the same shots as in E2 (`ar-<screen>.png`), so E5 can compare them.
- On every English screen check four things and write them per screen: (a) no Arabic left in the UI words (Arabic is fine only in the Quran/verse/hadith text, a reciter's or station's own name and a city's Arabic name: say which); (b) it reads left to right (titles, rows and icons start on the left, the back arrow points left, numbers and times in order); (c) D-pad: ▶ (22) moves the focus to the right and ◀ (21) to the left, ▼▲ as usual (check with the dump: which node is `focused="true"` after each key); (d) nothing is cut off, overlapped, or ends in «…».

| # | Do | Right when |
|---|---|---|
| E1 | `optimize/saver.sh lang en`; then `saver.sh show` (`lang: en`). Also once by hand: back to `ar`, then settings ← «المزيد» ← «اللغة» ← «English» with keys | `lang: en` both ways; the settings screen comes back at once in English |
| E2 | Shots in English (`en-<screen>.png`), each with the four checks: the home; «مركز التحكم» (Control Center); «المهام» (Tasks); «تلاوة وأذان» (Tilawah: reciters, stations) and its adhan page; the settings hub and EVERY page of it (adhan, place, clock, listen, cards → screensaver + home, look, control, phone, update, report, more → content, apps, shows, family, lang; open each with keys and Back); the screensaver page (preview from settings); «إعداد تلفزيوني» (open it, read it, press nothing that starts a run: Back out) | the four checks pass on each; write every failure with the screen and the exact word or place |
| E3 | The phone page in English: settings ← «التحكم من الجوال» ← turn it on with keys; open the address the TV shows in the PC's browser (it must serve `index.en.html`: the page in English; `curl -s <address> \| grep -c lang=\"en\"` or the page source); pair this PC once («سماح» on the TV with keys); shot each of its four screens (remote, favourites, playing, settings) | English, left to right, nothing cut; the pad's ◀▶ move the TV's focus the natural way |
| E4 | D-pad spot checks in English: the home's favourites row, the apps grid, the settings hub's two-to-a-row buttons, the Control Center tiles: from the first item press ▶ three times, then ◀ three times | the focus moves right on ▶ and back on ◀; it ends where it started; no dead key |
| E5 | `optimize/saver.sh lang ar`; take the same shots again (`ar2-<screen>.png`) and compare with the "before" set | Arabic exactly as before: same words, right to left, ◀▶ as before (▶ moves left in RTL as before); nothing left in English |
| Z | Phone control off and this PC's pairing removed; `saver.sh lang` as at the start; every `saver.sh show` value, the adhan, the dock and backsaver as at the start (`optimize/saver.sh import optimize/run/step20/settings-before.json` preview, then `--yes`, if anything differs); `optimize/saver.sh updatechannel latest`; Home holder as at the start; sound 0 | "restored: yes/no" with any value that differs |

REPORT.md (on `work/tester-report`): "step 20 (English)", the build, the table, per screen the four checks (OK / the problem), every Arabic word left, every cut or «…», every D-pad that goes the wrong way, and the E5 comparison. Push the shots to the report branch only after cleaning them: crop or blur the city, prayer times, the weather place, the phone page's address and code (IP and pairing code), any Wi-Fi name, task text and the owner's photos; nothing else personal. Tell the owner in Arabic that it is done and how many problems there were.

---
step: 21 (do it when build 177 or newer is on apk-next; the owner is not needed at the TV): PIN recovery code
**STRICT: the recovery code is NEVER written into any file, report, commit, log, screenshot or saved shell history. The repo is public. The owner gave the code in your chat at the start of the run (see the first line of this file); keep it only in memory. No code given: skip this step, report «step 21: no code given». Before any adb command: no `set -x`, no `script`/tee/session recording, no command-line echo; read it with `read -rs RC` (nothing shows, nothing is saved) and use only `"$RC"`, never the digits typed into a command; `unset RC` at the end. No screencap and no `uiautomator dump` while the code is in the field (only after the screen has moved on); no `logcat` saved to a file while typing it (read logcat only after, and check the code is not in it before keeping any line). If the code ever lands in a file, delete that file at once and say so in the report (without the code).**
what: the family PIN can be recovered with the recovery code typed in any PIN prompt, on build 177 or newer. Follow tester/RULES.md. TV sound stays at 0 the whole time. Every key is yours (`input keyevent`, focus checked with the dump or a screencap, as in step 18), but see the rule above for while the code is in the field.

| # | Do | Right when |
|---|---|---|
| 1 | **Prep.** `optimize/saver.sh updatechannel next`; update both apps from the TV («تحديث تلفزيوني») to apk-next 177 or newer: the version shows «(177)» or higher. Then `optimize/dock.sh pin status` and keep it on the PC: whether a PIN is set, the locked list, kids mode, the wrong-try counter (never the PIN). If NO PIN is set: `optimize/dock.sh pin set 1234` (temporary, only for this step; write «temporary PIN» in the report). If a PIN is set, you do not know it, and that is fine: the code is what is tested | the build is 177 or newer for both apps; `pin status` saved; a PIN is set |
| 2 | **The code.** Open a PIN prompt: settings ← «المزيد» ← «حماية العائلة» ← «غيّر الرمز», or a locked app (lock one for this row with `dock.sh pin lock <a store app>` if nothing is locked, and unlock it in row 6). type the code you were given at the start of the run with ONE command, `adb shell input text <code>`, never in a script or a file (if `input text` does not reach the field, send each digit as a keyevent in a loop over `"$RC"`, KEYCODE_0 = 7 … KEYCODE_9 = 16). Watch nothing being saved. After about 2 s, a dump/screencap | while typing, the field shows 4 dots and no more; about 1.6 s after the last digit «رمز جديد» opens. Set the new PIN 5678 with keys (and again if it asks to confirm) |
| 3 | **Nothing else changed.** `dock.sh pin status` against row 1; open a locked app (or «غيّر الرمز») and type 5678 | the locked list, the allowed apps and kids mode the same as row 1; 5678 opens it |
| 4 | **A wrong 4-digit PIN.** Open the prompt; type `0000`; `dock.sh pin status` | a normal error about 1.6 s after the last digit; the wrong-try counter is one more than before row 4; and the counter after row 2 had NOT gone up (the code is not a wrong try: compare row 1 → row 3 → row 4) |
| 5 | **A wrong long code.** Open the prompt; type `12345678`; `dock.sh pin status` | a normal wrong try: the same error, the counter one more. (Rows 4–5 make 2 wrong tries; the wait starts at 5, so none comes) |
| 6 | **Back as it was.** Type 5678 once in a prompt (the counter should go back to 0: `pin status`). Unlock the app locked in row 2, if you locked one. If there was NO PIN at row 1: `optimize/dock.sh pin off` (no PIN; the locked list and allowed apps stay, as they were with no PIN), then `pin status` against row 1; only if it still differs, `dock.sh pin reset` and put the locked list back with `dock.sh pin lock` for each item of row 1. If there WAS a PIN at row 1: tell the owner in Arabic in your chat that the PIN is now 5678 and ask if he wants his old one back (if yes, he tells you in the chat; `dock.sh pin set` it the same way, read with `read -rs`, never written). `unset RC`; `optimize/saver.sh updatechannel latest`; sound 0 | `pin status` as in row 1 (PIN set or not, list, kids mode, counter); RC unset; the owner told (if he had a PIN) |

REPORT.md (on `work/tester-report`): "step 21 (PIN recovery code)", the build, the table with ONLY worked / didn't work per row, plus any error text that does NOT contain the code (if a line holds the code, leave it out entirely and say «line left out: it held the code»), «temporary PIN» if row 1 set one. Never the code, never any PIN, no screenshots. Push; tell the owner in Arabic that it is done and how many problems there were.

---
step: 22 (do it when the build after work/int-nov3 has landed in next is on apk-next: 178 or newer; run it BEFORE step 21, the code test stays last; the owner is not needed): alignment and spacing
what: measure the alignment and the spacing on every screen against the owner's design rule. Follow tester/RULES.md. TV sound stays at 0 the whole time. Every key is yours (`input keyevent`, focus checked with the dump, as in step 18). Change no code.
- Start lines, plus: `optimize/saver.sh show` (keep all of it: `lang`, `look`, `appearance`, `seconds`, the cards, the markets), `optimize/saver.sh export > optimize/run/step22/settings-before.json`, `optimize/dock.sh show`, `optimize/saver.sh tasks list` (count only in the report), the Home holder, and `adb shell wm size` / `wm density`.
- `optimize/saver.sh updatechannel next`; update both apps from the TV («تحديث تلفزيوني») to 178 or newer.
- **Output 1920×1080.** If `wm size` says another physical size, `adb shell wm size 1920x1080` for this step and `adb shell wm size reset` in Z (write which). Measure in screen pixels.

**Method, per screen** (files in `optimize/run/step22/`, local only): `adb shell uiautomator dump /sdcard/ui.xml && adb shell cat /sdcard/ui.xml > <screen>.xml` and `adb exec-out screencap -p > <screen>.png`. A small script on the PC (in `optimize/run/step22/`, never committed) reads the `bounds="[x1,y1][x2,y2]"` of each node and measures:
- the inner padding of each text inside its box: the text node's bounds against its card/parent box (left, right, top, bottom). A TextView's bounds include its own padding, so also measure the glyphs: in the screencap, crop the text node's box and find the first and last rows/columns that differ from the box's background; take the smaller of the two numbers;
- the gaps between sibling cards (the x or y gap between neighbours in the same parent);
- the edges in a row (the tops) and in a column (the starts: the left edges in English, the right edges in Arabic);
- the focus ring: move the focus onto each item of a row once, screencap, and check the ring's outer pixels against its tray/box;
- WebView screens (the screensaver page, the phone page) may give no inner nodes in the dump: measure them from the screencap alone.

**Pass rules (the owner's design rule):**
- text never touches or crowds a box edge: at least 16 px at 1080p on every side, more on big cards (a card wider than 600 px or taller than 300 px: at least 24 px);
- the gaps between cards are equal within a group (±2 px) and on an 8 px scale (8, 16, 24, 32…, ±2 px);
- siblings share edges: the tops in a row, the starts in a column (±2 px);
- no text cut off or ending in «…» or «..»;
- the focus ring stays fully inside its tray or box;
- nothing off screen, and everything inside the 5% safe area (x 96–1824, y 54–1026), except a full-bleed background;
- Arabic screens right to left, English screens left to right.

| # | Screen (Arabic first on all; then English on the starred ones, `saver.sh lang en`, and back to `ar`) | Right when |
|---|---|---|
| G1 | the home with the favourites bar (FavBar)*; scroll down to «أكمل المشاهدة» below the fold | the rules above, on both |
| G2 | the options box on an app (long OK; the ★/ⓘ row)* | the rules |
| G3 | «مركز التحكم»* (the Wi-Fi tile next to Bluetooth) | the rules; the Wi-Fi and Bluetooth tiles share top and height |
| G4 | the settings hub* and each of its pages; «المزيد» and its pages | the rules on each page |
| G5 | the parental card, off and on (on: `dock.sh pin status` first; if no PIN is set, `dock.sh pin set 1234` only for this row and `dock.sh pin off` right after, as in step 21's restore) | the rules, both states |
| G6 | «تحديث تلفزيوني» (the version and build line) | the rules; the build line not cut |
| G7 | «المهام», and a task's «ذكّرني» page; a reminder card on the home and over the screensaver (add a test task «test» with a reminder 2 minutes ahead; delete it after) | the rules on all four |
| G8 | «تلاوة وأذان», and the adhan page* | the rules |
| G9 | the football home layout (`optimize/saver.sh homelayout football`; if the build has no such command, use the settings row and say so): the focus on the matches widget (the tray lowered 40 px: measure it), then back to the apps; then `homelayout classic` again | the rules; the tray moves by 40 px (±2) and back |
| G10 | «المباريات»: the list and one match's detail | the rules |
| G11 | «الأسواق»: the tabs السوق / قائمتي / محفظتي, a stock card with its range tabs, and «أخفِ المبالغ». Use MADE-UP holdings only (e.g. 1 share of one stock), and delete them afterwards | the rules; «أخفِ المبالغ» hides every amount; the holdings gone at the end |
| G12 | the screensaver page, with `saver.sh seconds on` and `seconds off` | the rules both ways; the seconds do not push the clock out of line |
| G13 | the «إعداد تلفزيوني» done screen with its QR: view only, start no run (if the done screen shows only after a run, write «not reachable without a run» and skip it) | the rules; the QR inside the safe area with a quiet margin |
| G14 | each look in «المظهر» (`saver.sh look glass`, `graphite`, `navy`, `clean`, `sand` = «رملي»), each on the home and on the options box; «رملي» both light and dark (`saver.sh appearance light` / `dark`) | the rules on each; no look moves an edge or a gap |
| Z | the look, appearance, lang, seconds, home layout, cards and markets as at the start (`optimize/saver.sh import optimize/run/step22/settings-before.json` preview, then `--yes`, if anything differs); the test task and the made-up holdings deleted; the PIN state as at the start; `wm size reset` if you changed it; `optimize/saver.sh updatechannel latest`; the Home holder as at the start; sound 0 | "restored: yes/no" with any value that differs |

REPORT.md (on `work/tester-report`): "step 22 (alignment and spacing)", the build, one row per screen (and language): OK, or the problem with the measured numbers (e.g. «title 9 px from the card's top edge», «gap 24 vs 32», «start 1812 vs 1806»); then a ranked list of the worst 10. Screenshots stay local, EXCEPT for the worst 10: the owner allows CLEANED crops of those, pushed to `tester/shots/step22/` on `work/tester-report` (crop to the problem; blur or crop out the city, prayer times, the weather place, any Wi-Fi name, the phone page's address and code, task text and holdings; nothing else personal). No dumps (`.xml`) pushed. Push; tell the owner in Arabic that it is done and how many problems there were.

---
step: 23 (build 179 on apk-next, next b202ae3; the owner is not needed; NO recovery code needed): re-check of build 178's problems on 179
what: the problems the 178 run found, checked again on 179, with numbers. Follow tester/RULES.md. The TV stays MUTED the whole time (`adb shell dumpsys audio | grep -i -E "muted|master"`). Every key is yours (`input keyevent`, focus checked with the dump, as in step 18). Measure as in step 22 (1920×1080, dump bounds + glyphs from the screencap, ±2 px, the 8 px scale, safe area x 96–1824, y 54–1026). Files in `optimize/run/step23/`, local only.
- Start lines, plus: `optimize/saver.sh show` (all of it), `optimize/saver.sh export > optimize/run/step23/settings-before.json`, `optimize/dock.sh show`, the Home holder, `adb shell wm size`.

| # | Do | Right when |
|---|---|---|
| 1 | **Update from the TV, 178 → 179.** `optimize/saver.sh updatechannel next`; «تحديث تلفزيوني» with keys, both apps. Read the mute right after the install, again at 10 s and at 30 s (`dumpsys audio`). `adb shell dumpsys activity activities \| grep -E "mResumedActivity\|topResumedActivity"` at the same three moments; settings «الإصدار» | muted at all three reads; the version shows «(179)» for both apps; write whether any app opened by itself after the update, and which (ours/«store app»/«system app») |
| 2 | **Radiojar station** (L1, «إذاعة القرآن الكريم - السعودية»). `adb logcat -c`; `optimize/music.sh radio 0`; 30 s; `optimize/music.sh status`; `adb logcat -d -s Listening:V MediaPlayer:V MediaPlayerNative:V NuPlayer:V '*:E' \| grep -v -i -E 'wifi\|ssid' > optimize/run/step23/radiojar.log`; `optimize/music.sh stop` | still muted; `playing=1`; no «cannot play» (tag `Listening`); the MediaPlayer lines saved (copy into the report only the lines with an error or the stream's state, no IPs) |
| 3 | **«إعداد تلفزيوني» in English.** `optimize/saver.sh lang en`; open it (`adb shell am start -n sa.urtv.home/.setup.SetupActivity`): the start screen; then the plan and the progress screens ONLY if they show without starting a run (never press «ابدأ»/«Start» on the plan; if the plan or the progress screen needs a run, write «not reachable without a run»). Dump + screencap each | no Arabic UI words on any of them (step 20's rule (a)); left to right; nothing cut, no «…» |
| 4 | **Step 22's worst 10, re-measured** (`lang ar`, as in step 22; write the number for each): (1) the settings footer's glyph bottom, every settings page; (2) the Tilawah/adhan footer's glyph bottom; gaps of (3) the favourites tray, (4) Control Center, (5) the Tilawah rows, (6) the PIN pad (with a temporary PIN 1234 only if none is set, then `dock.sh pin off`), (7) the settings rows, (10) the setup screen, and the parental card; (8) the phone-control title's top; (9) the month line | (1) and (2) ≤ 1026; every gap equal within its group (±2) and on the 8 px scale; (8) ≥ 54; (9) not cut, no «…». Per item: the 178 number (from the step 22 report: 1037, 1032, 20, 20, 20, 20/24, 18, 40, «…», 20) → the 179 number |
| 5 | **Matches and markets by keys**, the path in docs/SETTINGS.md on next: settings, from «الأذان» ▼×5 to «أرسل تقرير», ◀ to «المزيد», OK; OK on «المباريات والأسواق»; the focus is on «مباريات اليوم»: OK opens the matches page; Back, ▼×4 to «صفحة الأسواق», OK opens the markets page. Also from the home: OK on the stocks card (a home card set to «الأسهم»: `optimize/saver.sh home-card … stocks` only if none is set, put back in Z), and OK on the football games widget (`saver.sh homelayout football`, back to the start value in Z). Then G10/G11 as in step 22: the matches list and one match's detail; the السوق / قائمتي / محفظتي tabs and a stock card with its range tabs. For محفظتي use MADE-UP holdings only (e.g. 1 share of one stock) and delete them at the end | every path opens the right page with the focus visible; the step 22 rules pass on the four screens (write the numbers of any failure); the holdings deleted |
| Z | `optimize/saver.sh lang ar`; every value as at the start (`optimize/saver.sh import optimize/run/step23/settings-before.json` preview, then `--yes`, if anything differs: the home card, the home layout, the look); the made-up holdings gone; the PIN state as at the start; `optimize/saver.sh updatechannel latest`; the Home holder as at the start; muted | "restored: yes/no" with any value that differs; muted |

REPORT.md (on `work/tester-report`): "step 23 (re-check of 178's problems on 179)", the build, the table, the three mute reads and any app that opened by itself, `playing=` and the MediaPlayer error/state lines, the English setup screens' result, the worst-10 table 178 → 179 with numbers, the matches/markets paths and the G10/G11 numbers, any `!!`. No personal data; no screenshots. Push; tell the owner in Arabic that it is done and how many problems there were.

---
step: 24 (NOT run on its own any more: it is part A of step 26; read it there): diagnose: the self-update unmutes the TV and opens YouTube
what: DIAGNOSIS ONLY: find who unmutes the TV and who starts YouTube when our app updates itself (the owner heard YouTube music last night). Change no setting beyond what the row says, no code. Follow tester/RULES.md. Keep the TV MUTED: if it is ever found unmuted, write down the time (t+s) FIRST, then mute again at once (`adb shell input keyevent 164`, check with `dumpsys audio`) and go on. Files in `optimize/run/step24/` on the PC, local only, never committed.
- Before anything: press HOME (`adb shell input keyevent 3`) so our home is in front and nothing plays; `adb shell am force-stop com.google.android.youtube.tv`; check muted.

| # | Do | Right when |
|---|---|---|
| 1 | **Before.** `adb shell dumpsys audio \| grep -iE "muted\|mute\|STREAM_MUSIC"`; `adb shell dumpsys media_session` (summary: the sessions, their package and state; `cmd media_session list-sessions` if the TV has it); `adb shell dumpsys activity activities \| grep -E "mResumed\|Hist #"` (the resumed activity and the top task list). Save all three in `before.txt` | muted; our home resumed; no session in state PLAYING (3) |
| 2 | **Log.** `adb logcat -c`; then on the PC start `adb logcat -b all -v threadtime > optimize/run/step24/step24.log &` and write down the PC time as t=0 | the log grows |
| 3 | **Reinstall 179 the way the self-update does it.** First: `optimize/saver.sh updatechannel next`, then «تحديث تلفزيوني» with keys (this is the app's own path: its download and Android's install screen). If it says up to date: `curl -fLo optimize/run/step24/urtv-home.apk https://github.com/tshi-dev/UrTV/releases/download/apk-next/urtv-home.apk` and `adb install -r` it, then the same for `urtv-screensaver.apk` (same key, same build: an in-place update, `PACKAGE_REPLACED` as in a self-update; no uninstall, ever). Write the time of each install and which path you used (note: `adb install -r` is not the app's own installer, so a difference between the two paths is a finding) | `Success` for each; the path written |
| 4 | **Watch, every 2 s for 60 s** after the (last) install, one line each (t+s): mute (`dumpsys audio \| grep -i -m3 muted`), the resumed activity (`dumpsys activity activities \| grep -m1 mResumedActivity`), and whether a media session plays (`dumpsys media_session \| grep -E "package=\|state=PlaybackState \{state=3"`). A loop on the PC writing `watch.txt` | every line written; any unmute or YouTube moment marked |
| 5 | **Stop the log** (kill the logcat). From `step24.log` take ONLY these lines (grep, then read them; keep `relevant.txt`): **volume and mute**: `AudioService\|AudioManager\|adjustStreamVolume\|setStreamMute\|setMasterMute\|MUTE\|[Vv]olume` (keep the calling package/uid where shown); **our code**: `UrTVUpdate\|UpdateCarry\|A11yHeal\|UrTVA11y\|MY_PACKAGE_REPLACED\|PACKAGE_REPLACED` and `TclAppBoot.*isAllow.*sa\.urtv`; **who started YouTube**: `ActivityTaskManager.*START\|START u0` for `com.google.android.youtube.tv` (or any app that came up), with `from uid`, `callingPackage`, `launchedFromPackage`, `intent`; and `ActivityManager.*(Process|Killing|Start proc|died|has died).*sa\.urtv` around the install; **who unmuted**: any line from `com.tcl.*` or the TV's own services (`tvos`, `tvinput`, `audio_hal`, `MediaSessionService`, `MediaButton`) touching audio, volume, mute or media keys. Remove IPs, serials, Wi-Fi names and account names from every line you keep | the lines found (or «none» for a group) |
| Z | Muted (check); `adb shell am force-stop com.google.android.youtube.tv`; HOME, our home in front (the Home holder as before); `optimize/saver.sh updatechannel latest`; the log stopped | "restored: yes/no" |

REPORT.md (on `work/tester-report`): "step 24 (diagnose: update unmutes and opens YouTube)", the build, the path used in row 3 (the app's «تحديث تلفزيوني» or `adb install -r`), a timeline (t+seconds: event: each install, PACKAGE_REPLACED, our process death and restart, each mute change, each activity start, each media session that played), then ONE line «who unmuted: …» and ONE line «who launched YouTube: …», each with its evidence (the log lines, cleaned). If neither happened in this run, say so plainly and which path was used. No personal data, no screenshots, never the full log. Push; tell the owner in Arabic what was found.

---
step: 25 (NOT run on its own any more: it is part D of step 26; read it there): marketing screenshots
what: clean screenshots for ads and promotion, from a demo-clean TV. Follow tester/RULES.md, except that the CHECKED images of this step go to `tester/shots/marketing/` on `work/tester-report` (the owner asked for them). The TV stays MUTED the whole time. Every key is yours (focus checked with the dump, as in step 18). Raw shots stay in `optimize/run/step25/` on the PC.
- **Save first** (restored in Z): `optimize/saver.sh show` (all of it), `optimize/saver.sh export > optimize/run/step25/settings-before.json`, `optimize/dock.sh show`, `optimize/saver.sh tasks list` (on the PC only), `optimize/photos.sh list`, `dock.sh pin status`, the Home holder.

**Make the TV demo-clean:**
- City: `optimize/saver.sh city مكة` (مكة المكرمة) and `optimize/saver.sh prayermethod auto` (Umm al-Qura in Saudi Arabia; `saver.sh show` must say so; if not, set Umm al-Qura by its name in «الموقع والصلاة» with keys).
- Photos: the built-in wallpapers only. If `photos.sh list` shows ANY own photo: do NOT clear or delete them (they are the owner's); write «own photos present: skipped» for every shot where the background is a photo, and take only the screens with no photo behind (or ask the supervisor). With none, go on.
- Tasks: `optimize/saver.sh tasks add 'اشتر خبز'` and `optimize/saver.sh tasks add 'موعد الساعة ٥' --at '<today> 17:00' --where home` (a reminder for shot 8); delete both in Z (`tasks del N`, N from `tasks list`), and leave the owner's own tasks alone. If his tasks show on a screen, skip that shot or wait for the screen without them; never delete his.
- Favourites: `optimize/dock.sh set` with common apps only: YouTube (`com.google.android.youtube.tv`), Netflix, Shahid, ثمانية / TOD if installed (check with `pm list packages`); no personal apps. Put the old order back in Z.
- Holdings: made-up ones in «محفظتي» by keys: أرامكو 50, الراجحي 20, Apple 5. Delete them in Z.
- No PIN on screen, no phone pairing code, no Wi-Fi name, no IP, no serial, no notification or toast showing (wait for a toast to go; take the shot again if one shows).
- Sound muted the whole time.

**Capture** at full resolution: `adb exec-out screencap -p > optimize/run/step25/<name>.png` (PNG, the TV's full size; write the size once).

**Arabic, look «رملي» light** (`saver.sh lang ar`, `saver.sh look sand`, `saver.sh appearance light`), names `ar-sand-NN-<screen>.png`:

| # | Screen | file |
|---|---|---|
| 01 | home | `ar-sand-01-home.png` |
| 02 | home with the options box open on YouTube (long OK on its tile) | `ar-sand-02-options.png` |
| 03 | Control Center | `ar-sand-03-control.png` |
| 04 | settings hub | `ar-sand-04-settings.png` |
| 05 | the adhan page | `ar-sand-05-adhan.png` |
| 06 | «الساعة والأرقام» | `ar-sand-06-clock.png` |
| 07 | the parental card, on: only if NO PIN was set at the start: `dock.sh pin set 1234` just for this shot (the PIN is never on screen; take the card, not a PIN prompt), then `dock.sh pin off` at once. If a PIN WAS set, take the card as it is (on) without typing anything | `ar-sand-07-parental.png` |
| 08 | «المهام» with the reminder (the demo tasks only) | `ar-sand-08-tasks.png` |
| 09 | «تلاوة وأذان» | `ar-sand-09-tilawah.png` |
| 10 | the screensaver page, `saver.sh seconds on` (back in Z) | `ar-sand-10-screensaver.png` |
| 11 | the football home (`saver.sh homelayout football`) | `ar-sand-11-football.png` |
| 12 | «المباريات» list | `ar-sand-12-matches.png` |
| 13 | a match detail | `ar-sand-13-match.png` |
| 14 | «الأسواق»: السوق / قائمتي / محفظتي (one shot per tab: `-14a-market`, `-14b-watchlist`, `-14c-portfolio`) | `ar-sand-14a-market.png` … |
| 15 | the stock card with the year range | `ar-sand-15-stock.png` |
| 16 | «إعداد تلفزيوني» start screen (view only, no run) | `ar-sand-16-setup.png` |

**Repeat 01, 04, 11 and 14** (all three tabs): in the glass look, dark (`saver.sh look glass`, `appearance dark`) → `ar-glass-NN-…png`; and in English (`saver.sh lang en`, glass dark) → `en-glass-NN-…png` (e.g. `en-glass-11-football.png`).

**Check every image before pushing it** (open it and look, at full size): no city other than مكة المكرمة / Makkah, no personal names, no task but the two demo ones, no Wi-Fi name, IP, serial or phone code, no holdings but the made-up ones, no notification or toast, no own photo. Crop or blur anything doubtful; if unsure, leave the image out (write «skipped: <reason>»).

| # | Do | Right when |
|---|---|---|
| P | Push the checked images to `tester/shots/marketing/` on `work/tester-report`, plus `tester/shots/marketing/index.md` listing each file with one line (screen, look, language) | only checked images; index lists every file pushed |
| Z | Everything back: `optimize/saver.sh import optimize/run/step25/settings-before.json` (preview, then `--yes`: the city, method, look, appearance, lang, seconds, home layout, cards); the two demo tasks deleted (the owner's own untouched); the dock order as at the start (`dock.sh set …`); the made-up holdings deleted; the PIN state as at the start; `updatechannel latest`; the Home holder as at the start; muted | "restored: yes/no" with any value that differs; `tasks list` count = the start count |

REPORT.md (on `work/tester-report`): "step 25 (marketing screenshots)", one line per image: done, or skipped with the reason; "restored: yes/no". No personal data. Push; tell the owner in Arabic where the pictures are and how many.

---
step: 26 (the final test of 4.2; run it on the build named in the GO line, once the supervisor writes «✅ GO — build N»; the owner is not needed): final test of 4.2
what: one last run that folds steps 24 and 25 into one, plus the open items and the PIN recovery code. Follow tester/RULES.md (step 25's checked images are the one exception, as written there). **The TV stays MUTED the whole time:** check it before each part and after every restart, update or install, and mute again at once if it isn't (write the time first). Every key is yours (focus checked with the dump, as in step 18). **Order: A, B, D, then C last.** Files in `optimize/run/step26/`, local only.
- Start lines, plus: `optimize/saver.sh show` (all of it), `optimize/saver.sh export > optimize/run/step26/settings-before.json`, `optimize/dock.sh show`, `optimize/saver.sh tasks list` (PC only), `optimize/photos.sh list`, `dock.sh pin status`, the Home holder, `adb shell wm size`.
- The recovery code: you asked the owner for it at the very start (the bold line 1 at the top of this file). Hold it in chat memory only, never in a file, log, commit, script or screenshot. No code given: skip part C and report «part C: no code given».

**Part A, the update (step 24's rows 1–5, with the build N update as row 3).** Before: HOME, force-stop YouTube, muted. Full `adb logcat -b all -v threadtime > optimize/run/step26/update.log` from t=0; then update from the TV («تحديث تلفزيوني», channel `next`) to build N (if it says up to date, `adb install -r` of apk-next's two APKs, and say so); the 2 s watch for 60 s (mute, resumed activity, a media session playing) as in step 24 row 4; then the line groups of step 24 row 5.
| # | Right when |
|---|---|
| A1 | muted at every 2 s line, during and after the install |
| A2 | no app opens by itself (the resumed activity stays our home; no `START u0` for YouTube or any other app that we did not start) |
| A3 | `adb logcat -d -s UrTVUpdate` (from update.log) has UpdateCarry lines with their reason codes: write each code and what it says (no personal data) |

**Part B, the open items** (measure as in step 22: 1920×1080, dump bounds + glyphs in the screencap, ±2 px, the 8 px scale, safe area x 96–1824, y 54–1026; Arabic, then English where the item has words):
| # | Item | Right when |
|---|---|---|
| B1 | the favourites tray, at rest and lowered (focus on the widget above it) | inset 24 px on each side; no sliver of the tray or a tile showing outside it; at rest y 768–986, lowered y 808–1026 (±2) |
| B2 | the round buttons at the top of the home | the gaps between them equal (±2) and on the 8 px scale |
| B3 | «تلاوة وأذان»: the rows | the row gaps equal and on the 8 px scale; the footer ≤ 1026 |
| B4 | the PIN pad (a temporary 1234 only if no PIN is set, `dock.sh pin off` right after) | the key gaps equal and on the 8 px scale; nothing cut |
| B5 | the settings hub: the first gap (title to first row / first row to second) | on the 8 px scale and the same as the gaps below it |
| B6 | «الأسواق»: the insets and the padding of its tabs, lists and stock card | text ≥ 16 px from every box edge (≥ 24 on big cards); insets equal on both sides |
| B7 | «أخفِ المبالغ» on «محفظتي» (made-up holdings, as in part D): turn it on, leave the page, come back; also after Back/Home | it stays on; every amount stays hidden |
| B8 | the football home: the match cards' team names | no name cut, no «…» / «..», in Arabic and English |
| B9 | «إعداد تلفزيوني»: the «العربية \| English» switch (view only, no run) | it switches the screen's language both ways at once; the focus visible; nothing cut |
| B10 | «الأسواق» live mode «مباشر» | the «مباشر» label and its other labels shown, not cut, right in both languages; the prices move or say why not |

**Part D, the marketing screenshots: step 25, all of it** (the demo-clean setup: Makkah + Umm al-Qura, built-in wallpapers only and never the owner's photos, the 2 demo tasks, common apps only, the made-up holdings, no PIN/code/Wi-Fi/IP/serial/toast; the 16 Arabic «رملي» light shots, then 01, 04, 11 and 14 in glass dark and in English; check every image; push the checked ones to `tester/shots/marketing/` on `work/tester-report` with `index.md`). Shots that already exist there from an earlier run: replace them with the new build's.

**Part C, the PIN recovery code, LAST: step 21's rows 1–6** with the code held in memory (step 21's strict rule applies in full: one `adb shell input text <code>` command, no screencap or dump while the code is in the field, no logcat saved while typing, nothing written).

| # | Do | Right when |
|---|---|---|
| Z | Everything back: `optimize/saver.sh import optimize/run/step26/settings-before.json` (preview, then `--yes`); the demo tasks and the made-up holdings deleted (the owner's own untouched); the dock order; `lang ar`; the PIN state as in step 21 row 6; `wm size reset` if changed; `optimize/saver.sh updatechannel latest`; the Home holder as at the start; muted; YouTube force-stopped | "restored: yes/no" with any value that differs; muted |

REPORT.md (on `work/tester-report`): "step 26 (final test of 4.2)", the build; part A: the timeline (t+s: event), A1–A3 with the UpdateCarry reason codes, one line «who unmuted: …» and one «who launched an app: …» (or «nobody»); part B: B1–B10, OK or the problem with numbers; part D: one line per image, done or skipped with the reason; part C: worked / didn't work per row only (never the code or a PIN); "restored: yes/no". No personal data. Push; tell the owner in Arabic that it is done and how many problems there were.

---
step: 27 (the build after work/fix-180 lands in next: 181 or newer; run it on the build named in the GO line; the owner is not needed; NO recovery code): re-check after the fixes
what: the problems of step 26, checked again with numbers, the new «العملات», and a few marketing shots again. Follow tester/RULES.md. **The TV stays MUTED the whole time:** check it before each part and after every restart, update or install, and mute again at once if it isn't (write the time first). Every key is yours (focus checked with the dump, as in step 18). Measure as in step 22 (1920×1080, dump bounds + glyphs in the screencap, ±2 px, the 8 px scale, safe area x 96–1824, y 54–1026). Files in `optimize/run/step27/`, local only.
- Start lines, plus: `optimize/saver.sh show` (all of it), `optimize/saver.sh export > optimize/run/step27/settings-before.json`, `optimize/dock.sh show`, `optimize/saver.sh tasks list` (count only in the report), `optimize/photos.sh list`, `dock.sh pin status`, the Home holder, `adb shell wm size`.

**Part A, the update** with the app's own «تحديث تلفزيوني» (channel `next`; not `adb install`): before it, HOME, force-stop YouTube, muted; full `adb logcat -b all -v threadtime > optimize/run/step27/update.log` from t=0; the 2 s watch for 60 s after the install (mute, the resumed activity, a media session playing), as in step 24 row 4.
| # | Right when |
|---|---|
| A1 | muted at every 2 s line, during and after the install |
| A2 | after the install, OUR home comes to the front by itself (`mResumedActivity` = `sa.urtv.home/.MainActivity`), and no other app is resumed at any line |
| A3 | `UrTVUpdate` has a `carry stage=… why=…` line for every stage: write each stage and its `why` (no personal data) |

**Part B, every step 26 problem again, with numbers** (Arabic, then English where it has words):
| # | Item | Right when |
|---|---|---|
| B1 | the favourites tray with 6 or more favourites (add common apps with `dock.sh set` if fewer; the old order back in Z), AR and EN; move the focus along the whole row | no tile drawn outside the tray (glyph/pixel check at both ends); the app name in ONE fixed place above the tray's start edge (right in AR, left in EN): its bounds the same (±2) for every focused tile |
| B2 | «تلاوة وأذان»: the tabs | the gap between tabs on the 8 px scale, equal |
| B3 | «الأسواق» | the Arabic change sign right (e.g. «−٠٫٣٣٪»: the minus before the digits, never «٪٠٫٣٣-»); in English no wrapped label and no time split over two lines; the «الذهب والنفط» card's names read normally (not one letter per line); after saving a made-up holding the focus stays on that row or on the add button |
| B4 | «إعداد تلفزيوني»: the «العربية \| English» switch (view only, no run) | the labels fully visible, ≥16 px padding; after OK the focus stays on the switch, not on «جهّز كل شي» |
| B5 | live mode «مباشر»: `adb logcat -c`, turn it on, watch 2 minutes, `adb logcat -d -s UrTVMarkets > optimize/run/step27/markets.log` | during market hours the prices of US or Saudi rows change within 2 minutes (outside hours write «market closed» and which); the labels honest (they say live only when live, delayed/closed otherwise); the UrTVMarkets lines copied (cleaned) |
| B6 | «المباريات» against the football widget (`saver.sh homelayout football`, back in Z); open one match's detail | the same matches on both; no «لا مباريات اليوم» while the widget shows matches; on the detail the focus is NOT on «ذكّرني»; `tasks list` count unchanged (no task added by accident) |
| B7 | the dock, AR then EN, the same setting | the same apps in the same order in both languages |
| B8 | **New: «العملات»** in the markets page | the default list has USD first; the pegged ones say «مربوط بالدولار»; KWD shows a real rate with «آخر تحديث»; the converter: 100 USD → SAR = 375 (exactly), 1 KWD → SAR a sensible value (about 12.2; write it), SAR → USD gives the reverse; pick 3 extra currencies (one Arab, e.g. EGP or JOD; one remittance, e.g. INR, PKR or PHP; one major, e.g. EUR or GBP): they appear; write any that shows «—». Remove the 3 extras in Z |
| B9 | step 22's measures, quickly: the footers (≤1026), the round buttons at the top, the PIN pad (a temporary 1234 only if no PIN is set, `dock.sh pin off` right after), the settings gaps | report ONLY what regressed against step 26 (with the numbers); else «no regression» |

**Part E, exploratory** (after B, before D; about 30–45 minutes; muted). Use the TV like a curious family member: random paths, fast key presses (several `input keyevent` in one command), Back spam (10× BACK), Home in the middle of things, switching the language mid-screen (`saver.sh lang en` / `ar` while a page is open), opening and closing pages quickly, odd inputs in the number pads (0, very long numbers, deleting everything), adding and deleting things. Cover every new area: «الأسواق» (all tabs, the live switch «مباشر», «العملات», «محفظتي»), «المباريات», the football layout, reminders («ذكّرني»), the options box, the parental card, the settings hub, «إعداد تلفزيوني» start screen, the screensaver. Run `adb logcat -c` at the start and keep `adb logcat -b all -v threadtime > optimize/run/step27/explore.log` running.
- **Make it unexpected.** Also try things no step lists: odd orders, rare settings combinations (e.g. English + football layout + «رملي» dark + seconds on + 24-hour + Hindi digits), very many favourites (10), tasks (12, the limit, then one more), holdings and currencies (as many as it takes), a long screensaver run (20 minutes, then wake it), the TV's OWN system language and time zone changed and back (through the TV's settings with keys; write the original first, and put it back exactly), the network gone for a minute (see the guard below), a restart mid-page (`optimize/venv/bin/python -I optimize/tv.py --reboot` while a page of ours is open; reconnect, check our home and the page's state), and the phone page if a phone is paired (never pair one for this). **Invent at least 10 tests of your own** beyond these, and list each in the report (what, why, result).
- **The network test's guard:** only if your adb is over USB (`adb devices -l` shows a USB transport, not `ip:port`); over the network, turning Wi-Fi off cuts your own adb and you could not turn it back on: then skip it and write «skipped: adb over network». Over USB: `adb shell cmd wifi set-wifi-enabled disabled`, wait 60 s, look at our screens (home, markets, matches, weather), `adb shell cmd wifi set-wifi-enabled enabled`, then `cmd wifi status` until «connected». Nothing else about Wi-Fi: never forget, add or edit a network, never open its settings (AGENTS.md). If it does not reconnect within 3 minutes, stop part E and report at once.
- **Limits, even here:** never «ابدأ» / «جهّز كل شي» / «تراجع» on the setup screen; never OK on Android's uninstall question or «حذف التطبيق»'s confirm (Cancel/Back); never the TV's Wi-Fi or network settings, «إعادة ضبط», or turning debugging off; never `dock.sh pin reset` or a PIN you do not know (a temporary 1234 only if none is set, `pin off` after); only made-up holdings and the demo/test tasks («test»); delete what you add.
- **Report every** crash («توقف التطبيق»), ANR («لا يستجيب»), frozen focus, focus that disappears, text cut or overlapping, wrong language, wrong direction, or anything that looks broken. For each: the exact key path to reproduce it (from a known screen, e.g. «home → ▲ → ◀◀ → OK → BACK×3 …»), whether it happens again on a second try, `adb logcat -d -b crash` lines for a crash (and the `ANR in` lines from explore.log for an ANR), and the screen (its name; the shot stays on the PC).
| # | Right when |
|---|---|
| E1 | no crash, no ANR (`logcat -b crash` empty for `sa.urtv.*`; no `ANR in sa.urtv`) |
| E2 | the focus never freezes or disappears; Back always leads out; Home always returns to our home |
| E3 | no cut, overlapping, wrong-language or wrong-direction text found |

**Part F, from scratch via Downloader** (after E, before D; muted; the owner accepts the uninstall's data loss as in step 18: the PIN, his photos, his own adhan, the phone pairings). Files in `optimize/run/step27/scratch/`, local only. Step 18's rules hold: reconnect after each restart and only read until the run says done; never switch USB debugging off; if the TV is left without a working Home for more than 10 minutes, put it back with the computer path (`optimize/install.sh`, then `optimize/saver.sh import`).
| # | Do | Right when |
|---|---|---|
| F1 | **Save**, exactly as step 18 row 1 (the export into `scratch/settings.json` and every list there, plus `adb shell cat /proc/meminfo \| grep -E "MemTotal\|MemAvailable"` and `adb shell ps -A \| wc -l`) | everything saved; `"schema":1` |
| F2 | **Different apps first** (row 6 of the owner's list, done now so the run sees them): install 2–3 common apps that are not on the TV from the Play Store with keys (e.g. Shahid, Netflix, ثمانية / TOD; free, no account needed to install), or, if they are installed but disabled, write that. Never uninstall or disable the owner's own apps. `pm list packages \| sort > scratch/packages-f2.txt` | 2–3 apps added; written which |
| F3 | **Reset**, as step 18 row 2: read `optimize/reset.sh`, run it, keep its output; then `adb uninstall sa.urtv.home` and `adb uninstall sa.urtv.screensaver` (they may say «not installed») | no `!!`; the stock home on screen; neither `sa.urtv.*` in `pm list packages` |
| F4 | **Downloader.** Open the Downloader app on the TV (if it is not installed: install it from the Play Store with keys and write so). DPAD to its address box, type `https://github.com/tshi-dev/UrTV/releases/download/apk-next/urtv-home.apk` (`adb shell input text` with the URL, or keys), «Go», then its install. NOT the short code 9378345 (it points at 4.1). Write every screen and prompt Downloader and Android show (e.g. «allow installs from this source», the install question), what you pressed, and the time of each step (t+s) | the home app installed from apk-next; `dumpsys package sa.urtv.home \| grep -E "versionName\|versionCode"` = the GO build; the screens and times written |
| F5 | **The setup.** Before it: open one user app that does not play sound (e.g. the Play Store or TV Bro if present) and Home, so it is in the background; `pidof <it>`. Then «إعداد تلفزيوني» ← «جهّز كل شي»; the USB prompt as in step 18 rows 4/4b; the plan (every line, the counts); «ابدأ»; every step line with its mark (✓ / – / ✕); the Wi-Fi pause as in step 18 row 7 (press «كمّل» without forgetting anything) | it ends by itself; no ✕; the plan's counts real numbers |
| F6 | **What it did.** The disabled apps (`pm list packages -d \| sort` against F1: list the added ones by kind «store app»/«system app» and count), MemAvailable and the process count against F1, whether the F5 user app was closed or killed (`pidof` again, and `logcat -d \| grep -iE "kill\|am_kill\|lowmemorykiller" \| grep <it>`; it should not be: say what you saw), the home and screensaver (`resolve-activity` HOME, `screensaver_components`), `appops get sa.urtv.home AUTO_START` / `ASSOC_START` and the same for the screensaver, TV Bro and SmartTube installed | the home `sa.urtv.home/.MainActivity`; the screensaver ours; AUTO_START / ASSOC_START allow; TV Bro and SmartTube there; no user app killed |
| F7 | **The different apps.** Compare the plan's lines and the safe-list count with step 18's (the report on `work/tester-report`): what changed with the F2 apps; which TCL/community list was picked for this model (`adb logcat -d -s UrTVSetup` lines naming the list, or the plan's own line) | the change explained per F2 app (counted or not, and why); the list's name |
| F8 | **Back**, as step 18 row 12: `optimize/saver.sh import scratch/settings.json` (preview, then `--yes`), and by command what the export does not carry (dock order, names, hidden, input names, backsaver, family DNS, the locked list without a PIN, tasks, appearance/watchnext/updatebell, the update channel); the 4.2 screensaver from apk-next if the run put 4.1 (step 18 row 10). Remove ONLY the apps you added in F2 (with the Play Store's own uninstall, never `pm uninstall`). Save the lists again and `diff` them with F1 | the diff shows only the accepted losses and what the setup changed on purpose |

**Part D, these marketing shots only** (step 25's demo-clean setup and privacy rules in full: Makkah + Umm al-Qura, built-in wallpapers only, the 2 demo tasks, common apps, the made-up holdings, no PIN/code/Wi-Fi/IP/serial/toast; check every image): `ar-sand-13-match.png` (the match detail, skipped last time); `ar-sand-01-home.png`, `ar-glass-01-home.png`, `en-glass-01-home.png` with a built-in wallpaper that has NO person in it (step through the wallpapers until one with no person shows; write which one, by its number or description); `ar-sand-14b-watchlist.png` (the gold card fixed); and a new `ar-sand-17-currencies.png` («العملات»). **Keep them on the PC** in `optimize/run/step27/marketing/` (the owner sends them as a zip): push NO images this time.

| # | Do | Right when |
|---|---|---|
| Z | Everything back: `optimize/saver.sh import optimize/run/step27/settings-before.json` (preview, then `--yes`); the demo tasks, the made-up holdings and the 3 extra currencies and everything added in part E removed (the owner's own untouched); the dock order; the home layout; `lang ar`; the PIN state as at the start; `wm size reset` if changed; `optimize/saver.sh updatechannel latest`; the Home holder as at the start; muted; YouTube force-stopped | "restored: yes/no" with any value that differs; muted |

REPORT.md (on `work/tester-report`): "step 27 (re-check after the fixes)", the build; part A: the timeline (t+s: event), A1–A3 with every `carry stage=… why=…`; part B: B1–B9, OK or the problem with numbers (B5: market open/closed and the price changes; B8: the KWD value, the converter results, any «—»); part E: E1–E3, your own 10+ tests (what, why, result), the network test (or why skipped), and a list of every finding with its key path, repeats yes/no, the crash/ANR lines (cleaned) and the screen's name; part F: F1–F8, the Downloader screens and times, the plan's counts and every step line with its mark, the disabled count, MemAvailable and process count before/after, any user app killed, the list picked, what the F2 apps changed; part D: one line per shot, done (kept on the PC) or skipped with the reason; "restored: yes/no". No personal data; no images pushed. Push; tell the owner in Arabic that it is done, how many problems there were, and that the shots are on the PC in `optimize/run/step27/marketing/`.

---
step: 28 (build 182 or newer, the one named in the GO line; the owner is not needed; NO recovery code)
what: the update, step 27's problems again, the new items, the targetSDK 34 checks, three shots. Follow tester/RULES.md. **MUTED the whole time** (check before each part and after every restart, update or install; mute again at once, time written first). Keys by adb, focus checked with the dump; measure as in step 22. Files in `optimize/run/step28/`, local only.
- Start lines + `saver.sh show`, `saver.sh export > optimize/run/step28/settings-before.json`, `dock.sh show`, `tasks list` (count), `dock.sh pin status`, the Home holder.

| # | Do | Right when |
|---|---|---|
| A | **Update from the TV** (180/181 → 182) with «تحديث تلفزيوني», full logcat, the 2 s watch for 60 s (step 24 row 4) | muted throughout; our home comes to the front by itself; «تم» on the installer leads to `sa.urtv.home/.MainActivity`; our screensaver never covers the second app's install question (dreams + resumed activity at each line) |
| B1 | «الأسواق»: the header's change sign; save a made-up holding; live mode 2 min with `logcat -s UrTVMarkets`; the currency picker | the sign right («−٠٫٣٣٪»); focus stays on the row/add button after saving; UrTVMarkets lines present (if `parsed=0`, a `body_start` line too: copy it, cleaned); the picker's «تم» works, marks and focus right |
| B2 | a match's detail | the focus ring is where OK acts (OK does what the ring shows) |
| B3 | the setup screen during the run of row D1b | see D1b |
| B4 | the TV's own sleep (screen off with `input keyevent 223`, wait 1 min, `224`) and a restart (`tv.py --reboot`); `logcat -d -s UrTVMute` after each | muted again after both; the UrTVMute lines copied |
| C1 | the quick buttons «الأسواق» / «المباريات» at the top left of the home | present, reachable with keys, open the right pages, aligned with the other top buttons |
| C2 | a match row in the football widget and in «المباريات» | aligned (step 22 rules) and the same in both |
| C3 | a match detail's buttons | «شاهد على ثمانية» only if Thmanyah is installed (OK opens it); «أضف لفرقي» adds the team (remove it in Z) |
| C4 | «الأسواق» after the tidy; a stock card; the home «الأسهم» card | the controls row aligned, no green bar, density even; the stock card solid with the page dimmed behind it; the home stocks card has 24 px padding (±2) |
| C5 | «تطبيقات في الشريط» = 7, then 10 | every tile whole and inside the tray (AR and EN); back to the start value in Z |
| C6 | «تلاوة وأذان» ← القرّاء | the favourite reciters first; the letter index works with keys |
| D1 | **targetSDK 34.** Save as step 18 row 1 (the owner accepts the PIN/own adhan/pairing loss as before). `adb uninstall sa.urtv.home` only (the screensaver stays). Downloader: type the URL WITHOUT `https://` (`github.com/tshi-dev/UrTV/releases/download/apk-next/urtv-home.apk`), install | NO Play Protect block (write every prompt shown); versionCode = the GO build |
| D1b | **«جهّز كل شي» run** right after D1's reinstall (no extra reset: the home is freshly installed): «إعداد تلفزيوني» ← «جهّز كل شي»; the USB prompt as in step 18 rows 4/4b; the plan (counts); «ابدأ»; every step line with its mark; the Wi-Fi pause as in step 18 row 7 («كمّل» without forgetting anything). Every 30 s: `dumpsys activity activities \| grep -m1 mResumedActivity` and `dumpsys dreams \| grep -iE "dreaming\|mCurrentDream"`; `logcat -d -s UrTVSetup` at the end | B3: the setup screen stays in front (resumed) and visible the whole run, never covered by our screensaver or another screen; the done screen shows; no ✕; the community list's name written (from the plan or the UrTVSetup lines) |
| D2 | the adhan alarm after that install: `optimize/music.sh adhan` (`adhan_exact=`), `dumpsys alarm \| grep -c sa.urtv.home` once the adhan is back on (D5) | exact (`adhan_exact` not 0) |
| D3 | open the player for the first time (a station) | the notification question appears ONCE; answer «لا» |
| D4 | play a station after «لا» | `playing=1`; no «cannot play» |
| D5 | restore the settings as in step 18 row 12 (import, the adhan on as before, the rest by command); then the adhan while YouTube is in front: `saver.sh prayeradj` 1–2 min ahead (as in step 16), YouTube open, sound 0; `logcat -s UrTVAdhan`; minutes back right after | the adhan plays with YouTube in front (`play=1` in `adhan_alarm_last`, judged by the log only) |
| E | shots (step 25's demo-clean and privacy rules; keep on the PC in `optimize/run/step28/marketing/`, push none): `ar-sand-17-currencies.png`, `ar-sand-14a-market.png` (after the tidy), `ar-sand-11-football.png` (the new match row) | checked; done or skipped with the reason |
| Z | `saver.sh import optimize/run/step28/settings-before.json` (preview, `--yes`); the made-up holdings, the added team, the demo tasks gone; tray count, prayer minutes, dock, PIN state, lang as at the start; `updatechannel latest`; Home holder as at the start; YouTube force-stopped; muted | "restored: yes/no" |

REPORT.md (`work/tester-report`): "step 28", the build, one line per row (OK or the problem with numbers), the D1b plan counts, step lines with marks and the community list's name, the UrTVMarkets / UrTVMute / UrTVAdhan lines (cleaned), the Downloader prompts, "restored: yes/no". No personal data, no images pushed. Tell the owner in Arabic.

## step 29a — PC network checks (no TV install; TV stays muted; no personal data in the report)
Our cloud sessions are blocked from these hosts, so run from the PC:
1. `ffprobe -v error -show_entries stream=codec_name -of csv=p=0 "https://live.kwikmotion.com/sbrksaquranradiolive/ksaquranradio/playlist.m3u8"` and then `ffmpeg -v error -t 60 -i <same URL> -f null -` — report codec, and whether 60 s read cleanly (wall time).
2. The same for the old one: `https://stream.radiojar.com/0tpy1h0kxtzuv`, plus `curl -sI` on it (show the redirect chain status codes and whether it goes https→http; hosts only, strip tokens).
3. On the TV (muted, then mute kept): `adb shell am start -a android.intent.action.VIEW -d "<kwikmotion URL>" -t audio/x-mpegurl` is NOT needed; instead just report whether `curl -sI` of the kwikmotion URL from the PC returns 200.
4. Fetch `https://query1.finance.yahoo.com/v8/finance/spark?symbols=SAR%3DX,KWD%3DX&range=1d&interval=1d` with curl and report the HTTP status and the first 200 bytes.
Report as «step 29a» in REPORT.md and push.

## step 29 — build 186 (update from the installed build; TV muted throughout; restore everything in Z)
Re-check every step 28b problem plus the new work. Report each as ✓/✗ with the log line or bounds.
A. Update via «تحديث تلفزيوني» (channel next): as step 28b A (no screensaver over the question, home back in front, muted).
B. With the home card «الأسهم» on: ▲ from the tray → card → ▲ reaches the top row; every quick button reachable and opens its page.
C. Matches: «أضف … لفرقي», then reopen the page 3×: the «فرقك» group stays (no force-stop).
D. Markets: «صرف العملات» shows rates (KWD not «—»), and its button and «أخفِ المبالغ» are small, beside مباشر | عادي. In Tadawul hours (if open), live mode parses all symbols (`parsed=N` = asked) and prices move. Number pad in «متوسط التكلفة»: ⌫ points right in Arabic; zero is clear; «٫» key types 12٫5 → saved as 12.5.
E. Station 0 «إذاعة القرآن الكريم - السعودية» plays (new SBA HLS) for 60 s; stations 1–3 play.
F. Favourites tray at sizes 5, 7, 8, «الكل» with ≥10 favourites, AR and EN: whole tiles only, no sliver, tray height fits tiles, the small «more» triangle on the correct side, focus ring inside, «Hold OK for options» on the name's baseline. One screenshot each (demo-clean, PC only).
G. Home: the 6 quick buttons look the same; the prayer card's last line ≥16 px from its bottom; matches rows centred; the «اضغط مطولاً للخيارات» hint legible.
H. Adhan on a muted TV (volume 10%, guard ready): it unmutes, plays, and is muted again at the end; check `tv_muted_before=true` and `adhan end: re-mute done`. Also stop it early with Back: muted again.
Z. Restore as in step 28b Z.
Report as «step 29» in REPORT.md and push.

## step 30 — build 188 (update; TV muted throughout; restore in Z)
1. «فرقك»: «أضف … لفرقي», reopen the matches page 3×: the group stays, with that team's game.
2. Number pad: the bottom row (⌫ ٠ ٫) glyphs whole and centred.
3. The Arabic hint «اضغط مطولاً للخيارات» shows near the tray name (counter reset in this build).
4. Markets and matches pages are DARK by default while the home look is «رملي»; turning «صفحات الأسهم والمباريات داكنة» off makes them follow the home look; on again → dark. Contrast and rings OK. Screenshots of both pages, dark.
5. «رجوع» button at the bottom of a stock's card and of a match's card: ▼ reaches it, OK closes, focus returns to the same row; remote Back does the same.
6. Mute: from the update on, check `UrTVMute` lines; report any `mute changed: muted=false by=not us` with its time.
7. If Tadawul is open (Sun–Thu 10:00–15:00), live prices: `parsed` = asked, and prices move within 10 min.
Z. Restore as in step 28b Z.
Report as «step 30» in REPORT.md and push.

## step 30b — live US prices now (build 188; TV muted; restore in Z)
US market is open (17:30–00:00 Saudi). On «قائمتي» add AAPL, NVDA, TSLA (and see the S&P 500 / Nasdaq indices on «السوق» if shown).
1. Turn on «مباشر». For 10 min: log every `live status=… asked= parsed= reason=` line; parsed must equal asked.
2. Prices must move: note each one's price at +0, +3, +6 and +10 min, and compare with a public quote on the PC (the same minute, ±0.5%).
3. The header reads the US session (open / closes in …), not Tadawul's.
4. Portfolio: add AAPL 2 @ 150 → value in USD and the total in SAR at 3.75. Check the math.
5. The change shown as an amount (if that option is on) and as %.
6. Screenshots (demo-clean): «قائمتي» live and «محفظتي».
Z. Remove the three stocks and the holding; «مباشر» back as it was.
Report as «step 30b» in REPORT.md and push.

## step 31 — build 190 (update; TV muted throughout; owner asleep — keep the screen use quiet; restore in Z)
Re-check step 30's fixes plus the smoothness / low-RAM work:
1. «فرقك»: follow a team, reopen the matches page quickly (10–20 s apart) 12×: the group shows every time.
2. Stock card (2222, 1120, 1211; from «قائمتي» and the movers): fits the screen, «رجوع» visible with first focus, the action button readable; OK on «رجوع» returns to the same row; nothing changes the list by accident.
3. Pad: every key box the same height, borders whole.
4. Greeting: «مساء الخير» in the evening until about 2 h after Isha.
5. Dark pages still dark; «رجوع» in the match card.
6. Smoothness: open settings (time it with logcat Displayed), markets with «مباشر» on for 5 min (count redraws in the log; no redraw when prices didn't change), tray scrolling. Note any jank.
7. Low-RAM: this TV has 2 GB, so Ram.low should be ON: check `UrTVType` shows no key filter outside home, markets «مباشر» off by default on a fresh state, and the screensaver page has `lowram=1` (no seconds). Report whether anything feels worse.
8. Chromecast logic: `optimize/dock.sh cast status` on this TCL must say not a Chromecast and change nothing.
9. Mute log: any `mute changed: muted=false by=not us`.
Z. Restore as in step 28b Z.
Report as «step 31» in REPORT.md and push.

## step 32 — build 193 (update; TV muted; restore in Z)
1. Stock card (2222, 1120, 1211 + AAPL): «رجوع» shows its label, first focus on it with a ring; action button full size; ▼/▲ ring the scroll body only when it overflows.
2. Tray jank: repeat step 31's gfxinfo measure (same keys, same timing, photo look on) and compare with 19.59% / 42 ms median.
3. Live tick with unchanged prices: frames in 190 s (expect ~0 except the minute change).
4. Markets live: `asked` now counts only open exchanges; log lines with `reason=closed` present; parsed = asked.
5. `dock.sh cast status` on this TCL: «not a Google cast device», nothing changed.
6. Field-report fixes: Badr al-Turki in the reciters list and one surah plays (muted check of playback state only); Cairo Quran radio (station 11) plays for 60 s (it's a radiojar redirect — report clearly); own-photos limit reads 10 on this 2 GB TV (low RAM); «صرف العملات» rows show «غرام الذهب …» for KWD/BHD.
7. Unmapped keys: press the TCL remote's special keys (Video/Channel/Web if present) and report the `UrTVKeys unmapped key=` lines.
8. Greeting check if the time allows (evening / night rule).
Z. Restore as in step 28b Z.
Report as «step 32» in REPORT.md and push.

## step 33 — build 196 (update; TV muted; restore in Z)
1. Tray jank: the same gfxinfo script as steps 31/32 (10 ▶ + 10 ◀, 0.35 s, photo look on), 3 runs; compare with 19.6%/42 ms (191) and 31%/53 ms (193). Note the photo shown.
2. Cairo Quran radio (station 11): plays within 20 s? Report `live feed from <host>` or `live feed <host>: <error>` lines. Also station 0 and one qurango station still play.
3. Matches page and screensaver open with no blank/white flash: record with `screenrecord` (5 s each, open 3×), check frames; and nothing stays hidden (page shows within ~1.5 s).
4. Stock card: the ring now has space from the text; «رجوع» label and first focus still OK.
5. If US or Tadawul is open: `parsed = asked`.
6. Evening greeting if the time allows.
Z. Restore as in step 28b Z.
Report as «step 33» in REPORT.md and push.

## step 34 — build 198 (update; mute first; TV muted throughout; lang stays EN as the owner set it; restore everything else in Z)
1. Cairo Quran radio (station 11) plays within 20 s via the local relay; log `live feed from …` and any proxy lines. Stations 0 and 1 still play.
2. Screensaver page opened from the app: no pure-black second before it (recording, as in step 33). Real dream unchanged.
3. Next-prayer card never shows «—» on a cold start (3 cold starts, frame after the page shows).
4. English screensaver: no seam at x≈845 behind the date (shot + column brightness); Arabic still fine.
5. Markets in session (if Tadawul or US open): screensaver/home quotes not older than ~20 min; «delayed»/«متأخر» shows only when old. Also check the market-prices workflow runs today on GitHub (loop runs every ~5 min in session).
6. Quick regression: tray jank (one gfxinfo run), stock card, «فرقك», dark pages.
Z. Restore as in step 28b Z, except lang stays en.
Report as «step 34» in REPORT.md and push. If all pass, write «step 34: ALL PASS» on its own line.

## step 34m — 4 marketing pictures for the 4.2 post on X (after step 34; build 198; demo-clean like before)
1920×1080 screenshots, demo-clean (no personal data: demo city Makkah, no holdings of the owner, demo teams/stocks), Arabic, look «رملي» unless noted, the same style as the earlier `marketing/` shots:
1. `x1-home-football.png` — football home with the three cards and the favourites tray.
2. `x2-markets.png` — «الأسواق» on «السوق» with TASI, the movers and the US indices row; dark pages on.
3. `x3-matches.png` — «المباريات» with «فرقك» and a game card open (dark).
4. `x4-screensaver.png` — the screensaver page with the verse card and the stocks/clock (pick the nicest photo, no person in it).
Save in `optimize/run/step34/marketing/` on the PC, check each by eye, push none, and write «step 34m: 4 pictures ready» in REPORT.md. Restore lang en afterwards (the owner's choice).

## step 35 — build 200, release check (update; TV muted; lang stays en; restore in Z)
1. Update with «تحديث تلفزيوني» (channel next): no sound, home back in front, nothing else opened.
2. Stations: Cairo gone (11 stations); station 0 and 1 play; a TV that had 11 saved falls back without error.
3. Muhammad Ayyub in the reciters; one surah's playback state starts.
4. «السوق» S&P 500 row: gap between percent and price; readable when focused.
5. Stock card (2222, AAPL): the chart is whole on opening, no scroll needed; «رجوع» in the button row with first focus.
6. Quick sweep: home, tray, matches, markets, settings hub, screensaver — no crash, no cut text (AR and EN).
Z. Restore as in step 28b Z, except lang en.
Write «step 35: ALL PASS» if all pass, else list problems. Push.

## step 34p — share the 4 X pictures (no TV work)
The owner wants the 4 demo-clean pictures from step 34m. Check each by eye once more for personal data (none allowed), then commit ONLY these 4 files to a new branch `work/marketing` under `marketing/4.2/` (x1-home-football.png, x2-markets.png, x3-matches.png, x4-screensaver.png) and push. Write «step 34p: pushed» in REPORT.md.

## step 36 — Downloader code check (PC only; nothing installed on the TV)
1. `curl -sIL https://aftv.news/9378345`: report each hop (status + host only) and the final URL. It must end at `github.com/tshi-dev/UrTV/releases/download/apk-latest/urtv-home.apk` (then GitHub's object host).
2. Download that final APK to a temp folder; report its size and `aapt dump badging` (or `apkanalyzer`) versionName/versionCode — expect 4.2 / 204 (or newer). Delete it after.
3. Same for 2927902 (the screensaver fallback): final URL and its version.
Report as «step 36» in REPORT.md and push.

## step 37 — pictures for a second X post (today; build 204 = 4.2 release; TV muted; Arabic, look «رملي» unless noted; restore everything in Z incl. lang en)
Demo-clean exactly as step 34m (Makkah, no owner data, no PIN/code/QR/Wi-Fi name/IP/serial, demo favourites). 1920×1080 TV shots; phone shots from the phone page in a desktop browser at phone size (390×844, 3x) if no phone is handy.
1. `p1-phone-remote.png` — the phone page's remote screen (paired, demo).
2. `p2-phone-search.png` — the phone page typing a YouTube search (no real query history).
3. `t1-reciters.png` — «تلاوة»: the reciters list with favourites on top and the letter index visible.
4. `t2-adhan.png` — the adhan settings/page (times visible, Makkah).
5. `f1-parental.png` — «الرقابة الأبوية» page (no PIN shown; recovery code NEVER shown).
6. `f2-control-center.png` — «مركز التحكم» with Wi-Fi and Bluetooth tiles (blur or hide the Wi-Fi network name if shown).
7. `r1-tasks.png` — «المهام» with 2 demo tasks, one with «ذكّرني» time; plus `r2-reminder.png` the reminder card showing over the screensaver/home if you can trigger it with a demo task 1–2 min ahead.
8. `b1-own-photos.png` — Settings › شاشة التوقف › «صور شاشة التوقف» / «صوري» screen (packs list may be empty: shoot it anyway), and `b2-screensaver.png` — the screensaver with a nice built-in photo, no person.
Check each by eye, then commit ONLY these files to branch `work/marketing` under `marketing/4.2b/` and push. Remove the demo tasks. Report «step 37: pushed N pictures».

## step 37b — redo 2 pictures (same rules as step 37; push to work/marketing marketing/4.2b/, overwrite)
- `p2-phone-search.png`: it came out identical to the remote. Show the phone page's TYPING/search screen with a demo query typed (e.g. «سورة الكهف») sent to YouTube's search, not the remote pad.
- `r1-tasks.png`: no blurred rows. Delete every task first, then add exactly 3 demo tasks («اشتر خبز», «موعد الطبيب» with a reminder time, «اتصل بالوالدة») and shoot; remove them after.
Report «step 37b: pushed».

## step 38 — photo packs live on master (build 204; TV muted; restore in Z)
1. Settings › شاشة التوقف › «صور شاشة التوقف»: 5 packs listed with covers (الفضاء، الطبيعة، البحر والشواطئ، مدن السعودية، الحرمين) + «صورنا». Shot `b1-packs.png` (demo-clean) → push to work/marketing marketing/4.2b/ (overwrite b1-own-photos.png use is fine too).
2. «نزّل» each pack: time, size, any error; all 5 on the TV. «حقوق الصور»: credits show for ksa-cities and haramain (author · licence · link).
3. Start the screensaver for ~10 min: photos rotate across packs + «صورنا»; no photo with text/people as subject; note any that look bad or cropped wrong (pack + number). Shots `b2-pack-1.png`, `b3-pack-2.png` of two nice ones (clock + cards over a pack photo) → push to work/marketing.
4. «حذف» one pack frees its space; re-download works. Low-RAM: no slowdown in tray/home after packs.
Z. Delete the packs you added only if the TV had none before (keep it as found), «صورنا» on, lang en.
Report «step 38» in REPORT.md and push.

## step 39 — 4.2.1 setup fix (TV muted; restore everything in Z)
The bug (4.2): «جهّز كل شي» restarts the TV at «قياس الذاكرة قبل» and never resumes. Fix: the resume mark is saved before the restart.
1. Update to this build (channel next). Version shows 4.2.1.
2. Open «إعداد تلفزيوني», press «جهّز كل شي», and let it run with NO help: the restart line must show BEFORE the TV restarts; after the restart the setup must resume BY ITSELF (screen comes back, reading taken, continues); the second restart before «قياس الذاكرة بعد» likewise; it ends on the done screen. Log times of each restart and resume (`UrTVSetup`).
3. Report every step ✓/✗/skipped, and whether any «تعذّر القياس، كمّلنا.» appeared.
4. Nothing on the TV should differ from before except what the setup is meant to do (same as step 28 D1b).
Z. Restore as in step 28b Z (lang en).
Report «step 39» in REPORT.md; write «step 39: ALL PASS» if so. Push.

## step 40 — speed BASELINE for 4.3 (current build, nothing installed or changed; TV muted)
Measure 3 runs each, same method we will repeat after 4.3, and save the exact commands in the report:
1. Return to home from YouTube (Home key): time to first frame + any visible flicker (screenrecord 5 s, count blank/flash frames).
2. Tray: gfxinfo janky % and 50/90/99th with the step 31 key script.
3. Memory: `dumpsys meminfo sa.urtv.home` and `sa.urtv.screensaver` (PSS) at idle home, and with the screensaver running 2 min.
4. Screensaver: time from start to the page shown (as step 33).
5. Settings hub open (Displayed, cold and warm).
6. Matches page and markets page open (Displayed).
Report «step 40 baseline» with a table. Nothing else.

## step 41 — 4.3 candidate, full check + speed AFTER (TV muted; lang en stays; restore in Z)
A. Update via «تحديث تلفزيوني» (channel next): new update screen design, no sound, home back in front.
B. New: «المظهر والخلفيات» page (both looks; packs for home + saver, «صورنا», «صوري»), packs on the HOME wallpaper, shuffled rotation (no pack-after-pack blocks), «حقوق الصور» (file names LTR, «إغلاق» readable).
C. Quick buttons: hide markets via settings; «الجوال» button opens the phone page; Wi-Fi icon; Settings/Quick Settings key opens TV panel or our Control Center; fav focus ring gold on dark looks.
D. App options: «إلغاء التثبيت» (open dialog only, then Cancel — uninstall nothing), «صفحته في المتجر».
E. Matches: UCL, AFC Champions League Elite, Iraq Stars League in the league list; postponed games not «مباشر»; saver matches card after midnight.
F. Quran: Saudi station (station 0) plays 10 min with no stutter; log player=exo and restart count.
G. Phone: «صوري» upload 2 demo photos from the phone page (/u), shown, then delete; #53 message from the phone shows ~20 s, Back hides it, `dock.sh message off` stops it.
H. Second own adhan for Fajr: skip unless a file is at hand (report).
I. Prayer method list has no Ja'fari.
J. «تحديث جديد» pill: shows only when relevant (report state).
K. Control Center sound tile: no crash; `UrTVControl` log lines.
L. Security sanity: the home card page still works; any `file://` other than the page answers 403 (logcat if visible).
M. Speed AFTER: repeat step 40 exactly (same commands), table next to step 40's numbers.
Z. Restore as in step 28b Z (lang en), remove demo photos/messages.
Report «step 41»; write «step 41: ALL PASS» if so. Push.

## step 42 — setup-restart fix (owner chose A), build 209, TV muted, lang ar stays
The owner OK'd setting adb over TCP here for this test only.
1. Record `getprop persist.adb.tcp.port` (expected empty). `setprop persist.adb.tcp.port 5555` (via adb root/shell as allowed; if refused, report and stop).
2. Run the setup from the phone/PC as in step 39, to the end. Keep a mute watcher running; after EACH restart mute within 5 s (step 39 showed the TV unmutes on boot).
3. Check: the restart line shows before each restart; the setup resumes by itself after each restart (first and second); ends on the done screen with no key press; every step line ✓; memory before/after numbers shown.
4. Note any USB/debug prompt and how long it took.
5. Z: `setprop persist.adb.tcp.port ""` (back to what step 1 recorded), reboot once if needed to confirm it's empty, mute again. Restore as step 39 Z. Uninstall nothing.
Report «step 42»; write «step 42: ALL PASS» if so. Push.

## step 43 — IPTV players hands-on (owner OK'd installing them for this test; TV muted; remove all at the end)
Install (verify sha256 before installing):
- Lumen TV: https://github.com/lumen-tv/lumen-tv/releases/download/v0.9.4/lumen.apk sha256 e37072d7abc498a44360d91209370809bf991530f4be28d24085f3d8498813d3 (com.hayat.lumen)
- OwnTV: https://github.com/ahXN00/OwnTV/releases/download/v5.2.2/OwnTV.apk sha256 9b57f77798eaf9248f61a268cde627b6ccfc4a46ad4c226a9545a56b57cadd60 (tv.own.owntv)
- TiviMate: Play Store, package ar.tvplayer.tv (free tier; skip if Play needs a sign-in you don't have)
Playlist (same in each): https://iptv-org.github.io/iptv/countries/sa.m3u — use ONLY the official rows: Al Quran Al Kareem TV, Al Sunnah Al Nabawiyah TV (Globecast …/saudi_quran, …/saudi_sunnah) and Al Saudiya. Do not open any other channel. Sound stays muted.
Per app: add the M3U; open Quran; Up/Down between the 3 channels; play 5 min; Back and return. Record: time to first picture (screen-record), stalls/rebuffers, remote focus ever lost, Arabic names correct, UI language/RTL, ads or sign-in prompts, permissions asked, PSS while playing, APK size, overall feel (1–5) + 2 screenshots each.
Z: uninstall all three, clear any granted special permissions, confirm packages gone; restore as step 42 Z.
Report «step 43» with a comparison table and your pick.

## step 44 — 4.3 candidate re-check, build 212 (channel next; TV muted; lang ar stays; restore in Z)
1. Update to 212 via «تحديث تلفزيوني»: does our cover stay in front (no Apple TV / recents app flash) until the new home is up? versionName now 4.3?
2. «المظهر والخلفيات»: bottom row + «رجوع» inside the safe area; glass mock shows cards; auto mock split dark/light; home pack tiles show Arabic names; pack download failure state (if you can force one, e.g. Wi-Fi off briefly during a download): «تعذّر التنزيل، حاول مرة ثانية» and OK retries.
3. «صوري» as HOME wallpaper: upload 2 demo photos from the phone page, set the home's photos to «صوري», confirm they show on the home; then delete.
4. Phone page: send with empty message → «اكتب الرسالة أولًا.»; quick-settings header on the phone wraps fine.
5. Album card C17: add «ألبوم صوري» card, both rows («صورة كل دقيقة في ألبوم صوري», «تاريخ الصورة في ألبوم صوري») toggle and behave.
6. «تطبيقات مقترحة» (Settings ← المزيد ← التطبيقات والمفضلة): list shows TV Bro, SmartTube, VLC (store), TiviMate (store); store entries open Play; do NOT install anything.
7. Setup plan screen (open «إعداد تلفزيوني», stop at the plan, don't start): per-app ticks TV Bro + SmartTube, both ticked, toggling works. Back out.
8. Home nudge card: only if we're not the Home on this TV (we are, so expect NOT shown) — confirm not shown. Settings row «اجعل UrTV الشاشة الرئيسية» if present shows «✓ UrTV هو الشاشة الرئيسية».
9. Quran/Sunnah live buttons (tilawah / HaramLive): open each, confirm it opens SBA's official channel (YouTube/SmartTube) — muted, then back.
10. Speed: repeat step 40 item 1 (YouTube → Home, 3 runs) and item 2 (tray jank, 3 runs): are the dark frames gone?
Z. Restore as step 41 Z.
Report «step 44»; write «step 44: ALL PASS» if so. Push.

## step 45 — UrTV Player (our own new app, repo tshi-dev/UrTV-Player) first device test — run AFTER step 44; TV muted; uninstall at the end
The owner OK'd installing our own UrTV Player debug build. Source: GitHub Actions artifact «urtv-player-debug» from https://github.com/tshi-dev/UrTV-Player/actions/runs/42684384141 (or the newest green run on main) — our repo, built by CI from source; record its sha256.
Playlist: the same 3 official rows as step 43 (Al Quran Al Kareem TV, Al Sunnah Al Nabawiyah TV, Al Saudiya) as a local M3U — add it via the app's «إضافة من الجوال» flow if you can reach the LAN page from the PC (best), else typing/local file. No other channels.
Check & report: install size; launch animation smooth and short; cold start time (am start -W); Arabic RTL everywhere; D-pad focus never lost; live list + preview pane; zap Up/Down time (stats overlay: Settings → Playback → stats: time to first frame + zap avg); 5 min Quran playback stalls/dropped frames/resolution/fps; guide; settings (start screen, home rows, themes/accents switch live, text size); PIN; backup screen opens (don't export); PSS idle/playing; any crash/ANR (logcat). Screenshots of home, live, player OSD, appearance.
Z: uninstall com.urtv.player, delete the M3U, restore as step 44 Z.
Report «step 45». 

### step 45b — UrTV Player × the owner's own Jellyfin (same session as 45, before Z)
The owner has a Jellyfin server on the home LAN. Use the app's «خادم المنزل» section:
1. Discovery: does the server appear by itself? (else enter its LAN address — ask the owner, never guess).
2. Login with **Quick Connect** only: the TV shows a code, the OWNER approves it in his Jellyfin (web/phone). Never type or store his password; never write the server address, user name or tokens in the report (say «owner's server»).
3. Home rows (Continue / Next up / Recently added), a library grid, a show page (title once), a movie page with tech badges.
4. Play one movie and one episode (muted): direct play vs transcode (stats overlay), start time, seek, audio track, **Arabic subtitles** (embedded or external SRT/ASS): shaping RTL correct, style change, sync offset ±; skip intro if offered; stop at ~5 min and confirm the resume point shows on the server (and in Continue watching).
5. Report what failed with logcat lines (redact tokens/hosts).
Then at Z also: sign out of the server in the app (revokes the device), uninstall as planned; tell the owner to remove the «UrTV Player» device from Jellyfin → Dashboard → Devices if it remains.

### README photos (owner asked) — during steps 44/45, TV muted
Take clean full-screen captures with `adb exec-out screencap -p` (1920×1080, not phone photos) of the polished screens for the READMEs:
- UrTV: home (glass look, a nice pack wallpaper), «المظهر» page, control center (new side panel when it lands; else skip), setup plan + progress screens if reachable without starting, screensaver with cards, «تطبيقات مقترحة».
- UrTV Player: home with Continue watching, live list + preview, guide, player OSD, appearance/themes, and the home-server home (only if the library shown has no personal items — otherwise skip server shots).
Rules: NO personal data in any picture — no names, Wi-Fi name (hide/crop the network value), IPs/addresses, Jellyfin user/server names, phone codes/QRs that are live, family PIN screens, notifications. Arabic UI. Push the PNGs to branch work/marketing under readme-shots/urtv/ and readme-shots/player/ with a short INDEX.md (file → what it shows). Mention them in the report.


## step 46 — 4.3 candidate, build 214 (channel next; TV muted; owner asleep: do not wait for him; restore in Z)
Thanks for step 44. Update to build 214 (or newer on apk-next), then:
1. Update: is another app (Play Store last time) still in front for seconds while the home is replaced? Time it.
2. Control center: opens as a narrow side panel (not full screen); toggles and short rows; «تخصيص» ticks and reorder; focus never lost; Back closes. Light and dark.
3. Home: small Wi-Fi mark next to the greeting; quick buttons collapsed (2-3) and expand smoothly on focus; cards and apps bar the same glass; no «·» anywhere you see.
4. Loading: open the home after a force-stop, and start the screensaver: is the LAST picture shown at once instead of a blank page, then fades to live? Screenshots at 0.2 s / 1 s / 3 s.
5. «مباريات»: a match sheet (no overlapping text on the «مع الصلاة» line; try 3 games at different hours); a postponed/cancelled game if one exists today is NOT shown as live/upcoming (followed team card says «تأجلت»/«أُلغيت»); a followed team's last result matches its league table/result (pick a team that played in the last 2 days, compare with the league list).
6. «الدوريات كاملة» → «كل الدوريات…»: does the catalogue load (it may still be empty/partial: say what you see), pick one league, its games come; unpick; the cap message at 8.
7. Settings: «اجعل UrTV الشاشة الرئيسية» row shows «✓ UrTV هو الشاشة الرئيسية»; «تطبيق المباريات» row exists (auto/none/app); «أرسل تقرير» asks what it is about (7 topics) and the lines/QR change with the pick (do NOT send).
8. «تطبيقات مقترحة»: tiles in two columns; an installed app says «✓ مثبّت»; UrTV Player tile is NOT shown yet (no player-<version> release exists) — confirm absent.
9. «مخرج الصوت» tile in the control center opens the TV's sound settings and Back returns (no crash).
10. «المظهر والخلفيات»: bottom row and «رجوع» inside the safe area on FIRST focus (step 44 note) — still out?
11. Speed + weight: step 40 items 1 and 2 again; plus screensaver idle CPU (top, 60 s average) and PSS of both apps, home and saver; compare with step 41 numbers.
Z. Restore as step 44 Z. Report «step 46».

### step 45 amendments (run after 46, same night)
- Use the SIGNED pre-release **v0.9.1** from https://github.com/tshi-dev/UrTV-Player/releases (urtv-player-0.9.1.apk + .sha256; private repo, use the owner's gh login as before). Check: sha256 matches the .sha256 file; `apksigner verify --print-certs` SHA-256 = 13ade7d009a07f5ba5bab0b7734bd449df10dd47b53868dfa28e44cd8069dfea; package com.urtv.player (not .debug).
- Language: with UrTV in ar the Player starts Arabic by itself; switch UrTV to en → Player follows on next start; Player's own manual language setting overrides. Put UrTV back to ar.
- «شاهد المباراة»: with the 3-channel M3U loaded, open a match sheet in «مباريات» → «شاهد المباراة» appears (Player installed) → it opens the Player: with no matching channel expect «ما لقينا قناة لهذه المباراة في قوائمك» + «ابحث في القنوات». Then `adb shell am start -a sa.urtv.player.action.FIND_MATCH -n com.urtv.player/...` from the shell must be IGNORED/closed (caller is not UrTV): confirm.
- Tamper/update check: say what the Player's «حول/التحديث» screen shows (official build ✓).
- 45b (Jellyfin): SKIP tonight (needs the owner). README photos section still applies.
Z as written (uninstall, restore). Report «step 45».


## step 47 — build 215 (channel next; after steps 46 and 45; TV muted; owner asleep; restore in Z)
Update to build 215 (or newer) via «تحديث تلفزيوني», then:
1. Update cover: while the HOME app is replaced, does OUR cover stay in front until the new home is up (no Play Store/recents app visible)? Time any gap; logcat lines of the cover if it fails.
2. «تلفزيونك» hub: open the app from the apps list: four windows الأسواق, المباريات, الألعاب, الإعدادات in a 2×2 like the settings' first screen; each opens the right place (الألعاب says «قريبًا»); D-pad moves in all four directions in ar and en; the المباريات line shows the real next match of the followed teams (compare with «مباريات»); الأسواق line open/closed matches the hour; the remote's Settings key and the control center's settings row still open the settings directly. Screenshots ar dark/light + en.
3. «المظهر والخلفيات»: bottom row and «رجوع» inside the safe area on FIRST focus.
4. Home photos row: its own «صوري» choice (with ours / only mine / none) works with 2 demo photos from the phone page; delete them after.
5. «UrTV النظيف» PART A ONLY: the settings row opens a description first; focus starts on «إلغاء»; «فعّل» asks the family PIN if one is set (do NOT type the owner's PIN: if it asks, stop here and report «needs the owner»); if no PIN: turn it on, screenshot the clean home in the current look + one other look + light; «تخصيص» ticks change what stays; turn it off → the home is exactly as before (compare screenshots and the settings lists before/after).
   **Do NOT open or run «تخلّص من واجهة المصنع» (the factory-interface step) tonight, and do not turn on USB debugging prompts for it. That drill waits for the owner.** Only screenshot its entry row if visible.
6. Low-RAM cut is automatic only on weak TVs: just report Ram.low for this TV from logcat/`dumpsys` if printed.
Z. Restore as step 44 Z (clean mode OFF, photos deleted, lang ar, updatechannel as found). Report «step 47».


## step 48 — the phone quiz «الأسرع» (build 216 or newer; after step 47; TV muted for the run, see item 7; owner asleep; restore in Z)
Open «تلفزيونك» → «الألعاب» → «الأسرع». The "phones" are 2-3 separate browser profiles/windows on the PC (same LAN), each at 360 px wide (device toolbar): the real iPhone/Android test waits for the owner.
1. Lobby: QR + 4-digit code shown; the QR's address opens the join page from the PC; join 3 players (Arabic name, English name, a long name, the same name twice): what the TV shows; a wrong code 5 times → locked for a minute (say what the page says).
2. Round setup with the remote (category, level, kids, 5 questions): focus never lost; start.
3. Play 5 questions: the question on the TV is readable (screenshot), 20 ticks fall one per second, phones show 4 answers with text, first tap locks, a late answer after time is refused; reveal shows the right answer + coloured squares of who chose what; leaderboard; podium; «نلعب مرة ثانية» keeps the players.
4. Robustness: close one "phone" tab mid-question and reopen its link (same score?); a 4th player joins between questions; one player leaves; TV Back in the middle (pause card?); HOME then back to the game.
5. The screensaver must NOT start during a game left on a question for longer than the idle time (idle is 45 s on this TV: wait 2 minutes in the lobby and on a question).
6. Digits: with the digits setting on English, are the game's numbers English too? Language en: TV + phone page in English.
7. Sound: the game has 4 short tones. Keep the TV muted; only report whether the in-game sound switch exists and toggles.
8. Weight: PSS of the saver/home app in the lobby and mid-game; any jank (gfxinfo) on reveal; logcat errors.
9. From the PC, with curl against the game address while a game is open (do not write the address in the report): a request with a made-up Host name → refused? `Origin: http://example.com` on the join route → 403? 20 quick wrong codes → locked?
Z. Leave the game (no room left open: the code no longer answers), restore as step 44 Z. Report «step 48» with screenshots (no addresses, no QR that still works).
