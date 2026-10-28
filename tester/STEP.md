step: 7
from: UrTV supervisor
what: one test on the newest apk-next (3.147 or newer): part A the fixes from step 6 that need NO owner (do them yourself with adb), part B the owner's short look at the new glass design and the new features. Follow tester/RULES.md.

Part A needs no one: do it now, and do NOT message the owner for it. Part B: when the owner writes «رجعت» (or says go) in this chat, ask him in Arabic, one row at a time.

Before anything:
- Write down the start lines: `optimize/saver.sh show`, `optimize/dock.sh show`, `adb shell settings get secure enabled_accessibility_services`, `adb shell settings get global private_dns_mode`.
- Wait until `curl -sL https://github.com/tshi-dev/UrTV/releases/download/apk-next/version.txt` is 3.147 or above. Download urtv-screensaver.apk and urtv-home.apk from apk-next and install both with `adb install -r` (screensaver FIRST). Write down the version from `optimize/saver.sh show`, first line.

| # | Do | Shots (local) | Right when |
|---|---|---|---|
| A1 | SLOWDOWN, idle: leave the TV on the home screen 2 min untouched, then `adb shell dumpsys cpuinfo \| grep -E 'urtv\|webview\|sandboxed'`, `adb shell cat /proc/meminfo \| grep -E 'MemAvailable\|SwapFree'`, `adb shell dumpsys meminfo sa.urtv.screensaver \| grep -E 'TOTAL\|WebViews\|Views:'`, `adb shell dumpsys activity services sa.urtv.screensaver \| grep ServiceRecord` | — | screensaver CPU near 0 %; WebViews 0; no service but UpdateJob |
| A2 | SLOWDOWN, Home churn: 5 times: open YouTube (`adb shell monkey -p com.google.android.youtube.tv 1`), wait 5 s, press Home (`input keyevent 3`), wait 3 s. Right after: the cpuinfo line again, and `adb shell pidof sa.urtv.screensaver` before and after | — | write the numbers; compare with step 6 (45 % CPU, 570 MB available, swap almost full) |
| A3 | Settings key: on the home screen `input keyevent 83`, then `adb shell dumpsys activity activities \| grep -m1 topResumedActivity`; Back; the same with `input keyevent 176` | key83, key176 | our settings (sa.urtv.screensaver/.SettingsActivity) open, not com.android.tv.settings |
| A3b | PHONE CONTROL from the PC (owner's idea; do this BEFORE A4): on the TV, settings → «الجوال والتحديث» → «تحكم من الجوال» → تشغيل. Read the address and the 6-digit code from a screencap, open the address in Chrome on the PC, type the code; approve «سماح» on the TV with `adb shell input keyevent 23` | pair-pc, pair-ok | the page pairs with the typed code |
| A3c | In Chrome, every phone screen: «الريموت» (arrows, OK, Back, Home, volume: check the TV reacts), «التطبيقات» (real icons; open one app), «يعمل الآن», «الإعدادات» (change one card, then put it back). Toggle light mode once and back. Shots of each | phone-remote2, phone-apps2, phone-now2, phone-settings2 | every button works on the TV; light/dark switches all screens |
| A3d | Typing: `optimize/dock.sh type-setup`; open YouTube's search on the TV; in Chrome's «الريموت» text field type «قرآن» and press «بحث»; then `adb shell logcat -d -s UrTVType \| tail -20` | type-yt2 | the text appears in YouTube's search; the log shows root=present, field=found, result=typed_other (paste the UrTVType lines: they hold no text) |
| A3e | «تحديث تلفزيونك» on the page: افحص only (do NOT install) | phone-check | it says the TV is up to date |
| A4 | Our settings: «الجوال والتحديث» tile. «تحكم من الجوال» is the FIRST row; open it and turn phone control OFF (it was left on after step 6) | phone-row, phone-off | the row is there and first; phone control says it is off |
| A6 | Family DNS: `optimize/dock.sh familydns status`; `familydns on` (it may take up to 90 s); `status`; `adb shell settings get global private_dns_mode`; `adb shell logcat -d \| grep -i family-dns \| tail -20`; then `familydns off`; `status` | — | on says on; off goes back to the start value. If on fails again, paste the family-dns log lines (they hold no personal data) and every `!!` line |
| A7 | Adhkar: `optimize/saver.sh card adhkar`, start the screensaver (`adb shell am start -n com.android.systemui/.Somnambulator` or wait 45 s untouched) | adhkar4 | a dhikr TEXT shows, not only a heading |
| B1 | Owner: the remote's Settings key → our settings. Look at the main menu, open «الموقع والصلاة», go down to the bottom and back up, open «طريقة الحساب» | glass-hub, glass-sub, glass-dialog | glass look (smoked panels over the blurred photo); the title is visible again after going back up; no pages drawn over each other |
| B2 | Settings → الشاشة الرئيسية → المظهر → فاتح, then open the settings again; then back to داكن | glass-light | light = frosted white glass, dark ink, readable; back to dark |
| B3 | Home: long OK on an app in «كل التطبيقات» → «تعديل التطبيقات». Move one app, hide one (×, then «تراجع»), hide another and bring it back from «المخفية», push one up into the favourites (▲ from the first row), then Back | apps-edit, apps-hidden | every part works; the order stays after Home and back; put things back as the owner wants |
| B4 | Home: «أكمل المشاهدة» under the favourites | watchnext | one row only, Arabic title, nothing cut, the background and clock stay |
| B5 | Control Center (round pill): look at it in glass; open «إعدادات تلفزيونك» | center-glass | nothing cut; it opens our settings |
| B6 | «تلاوة وأذان»: open it, the surah list, the player | tilawah-glass | glass look, readable |
| B7 | Move fast along the favourites and the rows, and hold an arrow key | — (owner's word) | the focus grows smoothly; held keys don't lag |
| B8 | «المصادر» (our round pill, not the remote's source key): the HDMI list. If the PS5 or another device is on, then in standby | inputs-cec2 | the device's own name shows; standby shows; the owner's own names win |
| B9 | Pairing screen: settings → «الجوال والتحديث» → «تحكم من الجوال» → تشغيل; look only, then إيقاف | pairing-glass | one page, no scrolling, the code large and clear |
| B10 | Owner: does moving between apps still feel slow? Then yourself: A1-A2 again (cpuinfo idle and after 5 Home presses) | — | owner's word + the numbers |
| Z | Restore everything from the start lines (cards, familydns, accessibility services incl. the typing service, phone control OFF and the PC's pairing removed, appearance dark); `optimize/saver.sh updatechannel latest`; compare `saver.sh show` and `dock.sh show` | — | "restored: yes/no" |

REPORT.md: "step 7", the table with OK / problem per row, the numbers of A1-A2 (and B10) next to step 6's (45 % CPU, 570 MB available, swap almost full), the owner's words, any `!!` lines. No personal data (RULES.md: e.g. the sound output's TYPE only, never a device name). Push after part A, and again after part B. After part B, tell the owner in Arabic that the test is finished and how many problems there were.
