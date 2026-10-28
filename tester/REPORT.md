step 8, part A (no owner)

Version: before 3.147, after "version: 3.148" (`APK_TAG=apk-next optimize/install.sh all`; «Home is already the new launcher: nothing to switch»; no !! line).

| # | OK/problem | shots | note |
|---|---|---|---|
| A1 | OK | home1, home2, home3 | fixed: clock/greeting at the top, favourites row under it, «أكمل المشاهدة» a normal row of favourite-size cards; same after Home and after YouTube→Home |
| A2 | OK | set-row, set-scroll, set-dialog | fixed: focused row dark text on white (readable); after 5 down / 5 up the list stays in the same place, title visible; «طريقة الحساب» dialog solid |
| A3 | OK | center-icon | house icon |
| A4 | OK | pair-start, pair-90s, pair-allow8, pair-allow9, pair-ok2 | fixed: 90 s with no key at all: no screensaver (mCurrentDream=null), phone control still on. Owner typed the code in the PC's Chrome (400x860); «سماح» (moved from «رفض» first) → page «متصل · بقي 30 دقيقة» |
| A5 | problem | yt-search | YouTube opened on its profile picker (did not pick a profile for the owner); «قرآن» + «بحث» did nothing. UrTVType: «text request: mode=replace search=true len=4», «foreground=other», «service=on», «root=present», «field=none», «focus_class=android.widget.FrameLayout», «result=no_field» (same twice) |
| A6 | problem | card-on, card-off | page set the first card to «الصلاة القادمة»: TV home_panel nextprayer (works). Page set it back to «بدون» + «حفظ البطاقات»: TV still home_panel nextprayer (same as step 7). (The screensaver was up in both shots, so the home itself was not seen) |
| A7 | OK | z8-off | PC pairing removed («إلغاء»), «توقف التحكم من الجوال لأن آخر جوال انتهت جلسته» |
| A8 | check | — | idle 2 min (screensaver showing by then): screensaver 1.3%, home 0.8%; MemAvailable 770 MB, SwapFree 287 MB. After 5x YouTube→Home: same pid, screensaver 18% (+ its WebView 6%), home 0.9%, total 23%; MemAvailable 825 MB, SwapFree 354 MB. Step 6 was 45% / 570 MB / swap almost full; step 7 was 0% |

Restored: home card none, accessibility services back to the start value (typing off), phone control off and the PC pairing removed. Appearance dark, card verse, updatechannel latest (unchanged).
Part B (owner: look, «تعديل التطبيقات», speed) not done yet.
