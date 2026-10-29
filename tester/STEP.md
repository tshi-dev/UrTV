step: 14
from: UrTV supervisor
what: the NEW look of the phone page («تحكم من الجوال»), on apk-next build 158 or newer, before it goes to every TV. Follow tester/RULES.md. No owner needed for any row (pairing from the PC's Chrome, as in steps 8 to 10; the typing service stays on).

Before anything:
- Start lines as before. `optimize/saver.sh updatechannel next`. Wait until apk-next version.txt is 3.158 or above, then update from the TV («تحديث تلفزيوني» → «تحقق من التحديث» → install, ◀ + OK on Android's screens) as in step 13.
- Pair the PC's Chrome to the TV. Use a phone-sized window: Chrome DevTools device toolbar at 390×844.

| # | Do | Shots (local) | Right when |
|---|---|---|---|
| L1 | The page's «الريموت» tab, dark and then light («الوضع الفاتح» in «الإعدادات») | ph-remote-dark, ph-remote-light | header card «التلفزيون» with green «متصل»; big pad with the round gold «اختيار»; rounded رجوع/الرئيسية/تشغيل; volume row; text field with a gold send button; icon tab bar with the open tab in gold. Nothing cut, overlapping or under the tab bar |
| L2 | On the TV's own home: swipe on the pad up/down/left/right, tap «اختيار», hold an arrow, رجوع, الرئيسية | — | focus moves the right way (left is left on the TV); one tap = one OK; holding repeats; Back and Home work |
| L3 | Open YouTube from the «التطبيقات» tab; use the pad inside YouTube; type «قرآن» with «بحث» | ph-apps, ph-yt | the apps open; the pad moves YouTube's focus; YouTube searches «قرآن» |
| L4 | «يعمل الآن»: start a recitation from the page, pause/play, next, the sleep chips; the next-adhan card | ph-now | the controls act on the TV; the next adhan shows its time and «بعد N دقيقة» |
| L5 | «الإعدادات»: change one home card and save, then put it back; the city search shows results | ph-settings | saved on the first press; put back exactly |
| L6 | 360-wide window (DevTools 360×740) | ph-360 | no sideways scroll, nothing cut |
| Z | Phone control off, remove the PC's pairing; `optimize/saver.sh updatechannel latest`; restore everything from the start lines; TV sound stays at 0 | — | "restored: yes/no" |

REPORT.md: "step 14", the table, any `!!`. Push the shots ph-* to the report branch (no personal data on them: no city of the owner, no Wi-Fi name; use the demo city if a shot would show the real one). Tell the owner in Arabic that it is done and how many problems there were.
