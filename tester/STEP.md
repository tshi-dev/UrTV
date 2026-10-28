step: 9
from: UrTV supervisor
what: the last short test before the release, on apk-next 3.149 or newer: the four looks in «المظهر», YouTube search from the phone page, and «بدون». Part A needs no owner; part B is the owner's look at the four looks. Follow tester/RULES.md.

Before anything:
- Start lines as in step 8. Wait until apk-next version.txt is 3.149 or above; install with `APK_TAG=apk-next optimize/install.sh all`. Write down the version.

| # | Do | Shots (local) | Right when |
|---|---|---|---|
| A1 | Our settings → «الشاشة الرئيسية»: «المظهر» is the FIRST row. Open «الشكل»: move over the 4 options (زجاجي، فحمي، كحلي، نظيف), shot of each preview; then «الإضاءة» (داكن، فاتح، تلقائي) | look-row, look-glass, look-graphite, look-navy, look-clean, light-pick | first row; each option shows its own preview |
| A2 | For each look (`optimize/saver.sh look glass|graphite|navy|clean`), dark then light (`saver.sh appearance dark|light`): shots of the settings main menu with a focused tile, a section with a focused row, and the «طريقة الحساب» dialog. End on glass + dark | looks-*.png | every screen readable; the focused item readable; dialogs solid |
| A3 | Pair the PC's Chrome as in step 8 (keep-awake works now). YouTube search: open YouTube (pick the profile it shows only if it needs no owner choice; otherwise leave it on the profile screen), type «قرآن» in the page, «بحث»; `adb shell logcat -d -s UrTVType \| tail -20` | yt-search2 | YouTube's search opens with «قرآن» (log: search=started) |
| A4 | Page «الإعدادات»: first card «الصلاة القادمة», save; then «بدون», save. After each: `optimize/saver.sh show \| grep home_panel`, and `adb shell logcat -d -s UrTVPhone \| tail -10`; write what the page showed (any toast) | card-on2, card-off2 | home_panel nextprayer, then none; paste the UrTVPhone lines (counts only) |
| A5 | Turn phone control off, remove the PC's pairing | — | off |
| B1 | Owner: open «المظهر», try the four looks and the lighting, and say which he likes; put back what he wants | — (owner's word) | owner OK |
| Z | Restore everything from the start lines except the look/appearance the owner chose; `optimize/saver.sh updatechannel latest` | — | "restored: yes/no" |

REPORT.md: "step 9", the table, the owner's words, the UrTVType and UrTVPhone lines, any `!!` lines. No personal data. Push after part A and after part B; then tell the owner in Arabic that the test is finished and how many problems there were.

## Extra (owner OK'd, no owner needed): the real app icons for the release video
| # | Do | Right when |
|---|---|---|
| X1 | On the home screen, `adb exec-out screencap -p > home-full.png` (keep it LOCAL). Crop ONLY the four app tiles, each on its own, at full resolution: ثمانية, شاهد, YouTube, سين (find them in the favourites or «كل التطبيقات»; move focus away from them first so none has the focus ring). Save as icons/thmanyah.png, icons/shahid.png, icons/youtube.png, icons/seen.png | four clean tiles, nothing else from the screen in them |
| X2 | Push ONLY those four PNGs to work/tester-report under tester/icons/ (never the full screenshot) | pushed |
