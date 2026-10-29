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
