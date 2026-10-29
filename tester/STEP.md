step: 12
from: UrTV supervisor
what: the LAST check before the 4.0 release, on the newest apk-next (build 3.154+ in version.txt, named «4.0» on screen): the update from the installed 3.153 to the build named 4.0 must work, the version shows as 4.0 everywhere, and the new «تلفزيونك» tile. Follow tester/RULES.md. No owner needed.

Before anything:
- Start lines as before. Do NOT install the new build with install.sh: this step tests the TV's own update. `optimize/saver.sh updatechannel next`.
- Wait until `curl -sL https://github.com/tshi-dev/UrTV/releases/download/apk-next/version.txt` is above 3.153 (it still says "3.<build>": that is right, it is machine-only).

| # | Do | Right when |
|---|---|---|
| U1 | On the TV (keys over adb are fine): settings → «الجوال والتحديث» → «تحديث تلفزيوني» → افحص | it finds the new version (the 3.153 apps must see it as newer) |
| U2 | ثبّت; confirm Android's install screen(s) with `input keyevent` | both apps install |
| U3 | `optimize/saver.sh show` first line; `adb shell dumpsys package sa.urtv.home \| grep -E 'versionName\|versionCode'` and the same for sa.urtv.screensaver | versionName=4.0 for both; versionCode = the new build; saver.sh shows «4.0 (build N)» |
| U4 | Settings «الإصدار» / «تحديث تلفزيوني» screen, and «افحص» again | «4.0 (بناء N)», «محدّث»; no "3.1xx" anywhere a person reads |
| T1 | The «تلفزيونك» tile: open the TV's own apps list (Android's, not ours: `adb shell am start -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER` won't show it; use the stock launcher's Apps row or `adb shell cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER \| grep -A2 sa.urtv.home`). Screenshot of the tile if visible | the tile «تلفزيونك» is listed, with our banner |
| T2 | Start it: `adb shell am start -n sa.urtv.home/.HomeTile` (or OK on the tile); Home; Back | our home opens; Home and Back behave normally (our home is the default on this TV, so Back stays) |
| T3 | Our own «كل التطبيقات» | our app is not listed in it |
| Z | `optimize/saver.sh updatechannel latest`; restore everything from the start lines | "restored: yes/no" |

REPORT.md: "step 12", the table, the versionName/versionCode lines, any `!!`. Push; tell the owner in Arabic that the last check is done and how many problems there were.
