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
step: 16 (do it when the owner says «ابدأ الخطوة 16»)
what: confirm the 4.2 batch on `next` (the adhan options, the text field in «المهام», the TCL remote keys and the safe fixes, and a read-only look at the Wi-Fi owner) on the newest apk-next: build 169 or newer (`version.txt` 3.169; the build after 167). Follow tester/RULES.md. TV sound stays at 0 the whole time: judge the adhan by logcat and the state, never by ear.
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
| T1 | Open «المهام» | the text field has the focus (gold); OK opens the TV keyboard; type «تجربة»; the action key reads «إضافة»; the task is added and the field is empty again |
| T2 | OK on «إضافة» with the field empty | «اكتب المهمة أولاً»; nothing added |
| T3 | D-pad ▼▲◀▶ between the field, «إضافة» and the mic | every one is reachable and shows focus; no dead spot |
| T4 | `adb shell am start -a android.speech.action.RECOGNIZE_SPEECH` (say whether a recognizer opens), then back in «المهام» the mic button, if shown | the button either listens, or the window closes and the button hides with a message; no dead button. Delete the test task |
| K1 | `adb logcat -c`; our home in front; press the remote's Netflix, YouTube and Prime Video keys (Home between them); `adb logcat -d -s UrTVKeys` | each opens its app; the log shows `key=4062` / `4065` / `4099` |
| K2 | The Settings key on our home; `logcat -d -s UrTVKeys` | `key=4077` or `83`, and OUR settings open |
| K3 | `optimize/tweaks.sh apply`, then `settings get global cached_apps_freezer` | no change on Android 14 (no `!!`); the freezer value the same as at the start |
| K4 | The screensaver (preview from settings); look at the Arabic line under the clock and the card labels | the Arabic letters are joined, no gaps between them |
| K5 | `adb shell pm list packages -d` | `com.android.sdksandbox` is not in it; the list the same as at the start |
| W1 | `optimize/tv.py "dumpsys wifi" \| grep -o 'cname=[^ ]*' \| sort \| uniq -c` | write the counts only (package names, never a network name) |
| W2 | `adb shell cmd wifi status`, then `adb shell cmd wifi list-networks` | «connected»; a header and rows (count the rows; do not copy names) |
| W3 | `optimize/install.sh launcher` | «nothing to switch» (Home is already ours), no pause, no `!!` |
| Z | All four adhan options as they were at the start; the prayer minutes back exactly; the adhan on/off as at the start; the test task gone; `optimize/saver.sh updatechannel latest`; the disabled-apps list and Home holder as at the start; sound 0 | "restored: yes/no" with the Home holder line and the disabled-apps count before and after |

REPORT.md: "step 16", the build, the table, the `adhan_alarm_last=` values (front/play/notice/saver only, never a prayer time), the UrTVKeys key numbers, the W1 counts, any `!!`. No personal data: no city, prayer times, task text beyond «تجربة», Wi-Fi names, IPs or serials. Push; tell the owner in Arabic that it is done and how many problems there were.
