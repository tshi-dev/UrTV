step: 13
from: UrTV supervisor
what: a 5-minute check before today's 4.0 release, on apk-next build 156 or newer: the screens say «4.0» alone (no «بناء»). Follow tester/RULES.md. No owner needed.

Before anything:
- Start lines as before. Do NOT use install.sh: the TV updates itself. `optimize/saver.sh updatechannel next`.
- Wait until `curl -sL https://github.com/tshi-dev/UrTV/releases/download/apk-next/version.txt` is 3.156 or above.

| # | Do | Right when |
|---|---|---|
| V1 | settings → «الجوال والتحديث» → «تحديث تلفزيوني» → «تحقق من التحديث» → install (◀ + OK on Android's screens, as in step 12) | both apps update from build 155 |
| V2 | `adb shell dumpsys package sa.urtv.home \| grep -E 'versionName\|versionCode'`, the same for sa.urtv.screensaver; `optimize/saver.sh show` first line | versionName=4.0 and the new versionCode for both; saver.sh says «4.0 (build N)» (the build is fine there) |
| V3 | «تحديث تلفزيوني» again → «تحقق من التحديث»; the «الجوال والتحديث» tile. Screenshots | «الإصدار المثبّت 4.0», «تلفزيونك محدّث: 4.0», «الإصدار الحالي: ٤٫٠»: no «بناء», no double brackets, no 3.1xx |
| Z | `optimize/saver.sh updatechannel latest`; restore everything from the start lines; check the first line of `saver.sh show` | "restored: yes/no"; the newest on latest reads «3.142», not «4.0 (build 142)» |

REPORT.md: "step 13", the table, the versionName/versionCode lines, any `!!`. Push; tell the owner in Arabic that it's done and how many problems there were.
