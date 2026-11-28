# 4.3.7 rc (apk-437rc, build 687) — TV test, 28 Nov night

TV muted throughout (Muted: true after every step). Wi-Fi/personal data left out; city boxed.

| # | Item | Result | Notes |
|---|------|--------|-------|
| 1 | Install | PASS | `APK_TAG=apk-437rc install.sh all` over beta 654. home 4.3.7 (687), screensaver 4.3.7 (687), **rescue 4.3.3 (versionCode 460) — not 4.3.7**: check whether the rc ships a newer rescue. install.sh printed "safety switch put the stock home back (ADB too slow)" but Home holder was sa.urtv.home afterwards (false alarm). |
| 2 | 30 D-pad keys + Settings | PASS (with a finding) | First try: keys hit a beta dialog «أنت على النسخة التجريبية، إذا شفت شي غريب ارسل ملاحظة» (أرسل ملاحظة / نعم) on top of the home — it stole the focus (keys-first-try-beta-dialog.png). I pressed «نعم»; then 30 keys, focus kept moving and the row scrolled (keys-30.png). Settings → BACK → HOME: home in front, no crash. A beta-channel dialog on a release candidate is worth a look. |
| 3 | Reboot | PASS on persistence; the grep condition as written FAILS | `reboot` at 21:07, checked at +2 min: HOME holder sa.urtv.home, screensaver component sa.urtv.screensaver/.ScreenDream, idle 45000, sleep_timeout and the whole disabled list identical before/after (raw/d1-*.txt). HOME pressed: our home in front (reboot-home.png). `pm list packages -d | grep partnersetup\|usercenter\|partnercustomizer` is NOT empty (partnersetup, usercenter, partnercustomizer disabled) — but identically before the reboot, so not caused by the update. wifi/connectivity/network: empty (PASS). Crash buffer only has init SIGABRT at boot 03:00:03 (pre-clock, pid 309). |
| 4 | Accounts / Assistant | PARTIAL | `android.settings.SYNC_SETTINGS` shows an Android chooser «Complete action using» with two entries both named «Settings» (accounts-chooser.png) — no error dialog, but the user must pick one. Assistant: `com.google.android.katniss/.setting.v2.SettingsActivity` does not exist on this TV, so not opened; need the right component or remote path. |
| 5 | Screensaver | PASS | start-saver.sh → dream; page drawn fully: clock, weather, stocks ticker, hadith, matches, next prayer, Ramadan count (saver.png). HOME afterwards returns to our home. |
| 6 | Radio | PASS (via music.sh) | UI path (تلاوة وأذان → الإذاعات → السعودية) not walked; `music.sh radio 0` = السعودية, played 2 min: rebuffers 0, errors 0, radio_fault empty; stopped; logcat -b crash has no new entries. |
| 7 | keep.sh | PASS | `keep.sh` → «Protected features: all on.» The `status` argument does not exist (usage: off / restore / unkeep). |

Settings: not changed except idle untouched; TV muted, home in front. `saver.sh show` before: raw saved locally (b687/show-before.txt).
