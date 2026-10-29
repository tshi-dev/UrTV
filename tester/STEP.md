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
what: confirm the fixes from the 159 test AND the new batch (Settings key, 24-hour row, leagues save, mirroring tile, world cities) on the newest apk-next (build 164 or newer: «Accessibility after the app's own update…» and the rest). TV sound 0 the whole time.
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
