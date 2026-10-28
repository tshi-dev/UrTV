step: 10
from: UrTV supervisor
what: the last TV check before the release, on apk-next 3.152 or newer: the phone remote in every app, typing into YouTube's search, «بدون» on the first press. Follow tester/RULES.md. No owner needed for any row (the owner OK'd: the tester turns the accessibility service on with ADB and LEAVES IT ON afterwards).

Before anything:
- Start lines as before. Wait until apk-next version.txt is 3.152 or above; install with `APK_TAG=apk-next optimize/install.sh all`. Write down the version.
- `optimize/dock.sh type-setup` (turns on our service; the owner OK'd it and wants it left on). Then `adb shell settings get secure enabled_accessibility_services` must include sa.urtv.home.

| # | Do | Shots (local) | Right when |
|---|---|---|---|
| R1 | Pair the PC's Chrome (as in step 8/9). Open YouTube on the TV. From the page's «الريموت»: ▲ ▼ ◀ ▶ a few times, OK on a video, Back, Home. Then the same in the TV's own settings app and in Shahid if installed. `adb shell logcat -d -s UrTVRemote \| tail -20` | remote-yt, remote-settings | the focus moves in YouTube and the settings; OK opens; Back/Home work; log shows the global actions (no content) |
| R2 | YouTube search from the page: type «قرآن», «بحث». `adb shell logcat -d -s UrTVType \| tail -20` | yt-search3 | YouTube's search opens with «قرآن» (search=started) |
| R3 | Page «الإعدادات»: first card «الصلاة القادمة», «حفظ البطاقات»; then «بدون», «حفظ البطاقات» ONCE. After each: `optimize/saver.sh show \| grep home_panel` and `adb shell logcat -d -s UrTVPhone \| tail -5` | card-on3, card-off3 | nextprayer, then none on the FIRST press |
| R4 | Adhan + phone key: `optimize/music.sh adhan now` while YouTube is in front; press ▼ on the page, then the page's «أوقف الأذان الآن» | — | the adhan plays as before and ▼ doesn't break it; «أوقف الأذان الآن» stops it |
| R5 | Phone control off, remove the PC's pairing. Leave the accessibility service ON (owner's wish) | — | off; service still on |
| Z | Restore everything else from the start lines; `optimize/saver.sh updatechannel latest` | — | "restored: yes/no (service left on, as asked)" |

REPORT.md: "step 10", the table, the UrTVRemote/UrTVType/UrTVPhone lines, any `!!` lines. No personal data. Push; tell the owner in Arabic that the test is finished and how many problems there were.
