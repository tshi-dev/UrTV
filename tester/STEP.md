step: 8
from: UrTV supervisor
what: the retest of step 7's problems on apk-next 3.148 or newer. Part A needs NO owner (do it yourself with adb and the PC's Chrome; do not message the owner for it). Part B is the owner's: two rows he must do himself, plus his look at the home and the glass. Follow tester/RULES.md.

Before anything:
- Write down the start lines: `optimize/saver.sh show`, `optimize/dock.sh show`, `adb shell settings get secure enabled_accessibility_services`, `adb shell settings get global private_dns_mode`.
- Wait until `curl -sL https://github.com/tshi-dev/UrTV/releases/download/apk-next/version.txt` is 3.148 or above. Install with `APK_TAG=apk-next optimize/install.sh all` (as in step 7). Write down the version.

| # | Do | Shots (local) | Right when |
|---|---|---|---|
| A1 | Home screen right after the install, then after Home again, then after opening and leaving YouTube | home1, home2, home3 | the clock/greeting block at the top, the favourites row under it (NOT over the clock), «أكمل المشاهدة» a normal row under the favourites with cards the size of the favourite tiles, nothing huge |
| A2 | Our settings (keyevent 83): in «الموقع والصلاة» move onto a row; go 5 down and 5 up; open «طريقة الحساب» | set-row, set-scroll, set-dialog | the focused row's words are dark on the white fill (readable); the list never moves sideways; the dialog is solid (nothing behind shows through) |
| A3 | Control Center: the «إعدادات تلفزيونك» tile | center-icon | a house icon, not a white square |
| A4 | Pairing stays awake: settings → «الجوال والتحديث» → «تحكم من الجوال» → تشغيل; touch NOTHING for 90 s (no keep-awake keys from the PC this time); then pair from the PC's Chrome with the code and «سماح» (`input keyevent 23`, after moving to «سماح» if focus is on «رفض») | pair-90s, pair-ok2 | no screensaver during the 90 s; phone control still on; pairing works |
| A5 | YouTube search from the page: open YouTube on the TV (search screen or home), type «قرآن» in the page's text field, «بحث»; then `adb shell logcat -d -s UrTVType \| tail -20` | yt-search | YouTube's search opens with «قرآن» and results; the log says searched (paste the UrTVType lines; they hold no text) |
| A6 | Page «الإعدادات»: set the first home card to «الصلاة القادمة», look at the home; set it back to «بدون», look at the home again (no Home press in between) | card-on, card-off | the card appears, then disappears at once |
| A7 | Turn phone control off, remove the PC's pairing | — | off |
| A8 | Slowdown numbers again: A1-A2 of step 7 (idle, and after 5x YouTube→Home) | — | still about 0 % for the screensaver app; write the memory numbers |
| B1 | Owner: look at the home screen and the settings (dark), and say if the glass and the layout are good | — (owner's word) | owner OK |
| B2 | Owner: «تعديل التطبيقات»: long OK on an app in «كل التطبيقات» → «تعديل التطبيقات»; move one app; hide one (× then «تراجع»); hide one and bring it back from «المخفية»; push one up into the favourites (▲ from the first row); Back. Then put things the way he wants | apps-edit, apps-hidden | every part works; the order stays after Home and back |
| B3 | Owner: move fast along the favourites and rows, hold an arrow key | — (owner's word) | smooth; held keys don't lag |
| Z | Restore everything from the start lines (cards, familydns, accessibility services incl. typing, phone control OFF, appearance as the owner had it); `optimize/saver.sh updatechannel latest`; compare `saver.sh show` and `dock.sh show` | — | "restored: yes/no" |

REPORT.md: "step 8", the table, the numbers, the owner's words, any `!!` lines. No personal data. Push after part A and again after part B. After part B, tell the owner in Arabic that the test is finished and how many problems there were. Part B: ask the owner in Arabic, one row at a time, when he says he's ready.
