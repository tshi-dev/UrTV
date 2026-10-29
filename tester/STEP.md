step: 11
from: UrTV supervisor
what: the adhan-time fix on apk-next 3.153 or newer (the TCL "app start" block must never move the adhan to Riyadh's times), the daytime adhan row left from step 10, and the two leftovers (the owner's exact narration text; the official icons). Follow tester/RULES.md. No owner needed except where it says.

Before anything:
- Start lines as before (include `optimize/music.sh adhan` output: the next adhan line). Wait until apk-next version.txt is 3.153 or above; install with `APK_TAG=apk-next optimize/install.sh all`. Write down the version.

| # | Do | Right when |
|---|---|---|
| P1 | Set a DEMO city that is not Riyadh: `optimize/saver.sh city 'الكويت'` (write down the real one to put back; never name it in REPORT.md). Wait 1 min on the home. `optimize/music.sh adhan` → note the next adhan time (Kuwait's) | the next time is Kuwait's, not Riyadh's |
| P2 | Simulate the TCL block: `adb shell am force-stop sa.urtv.screensaver`, then open YouTube (`adb shell monkey -p com.google.android.youtube.tv 1`), wait 2 min; `adb shell logcat -d \| grep -E 'TclAppBoot.*ConfigProvider\|UrTVAdhan' \| tail -10`; `optimize/music.sh adhan` again | the next time is STILL Kuwait's; if the block happened, one `UrTVAdhan: place read failed: the last good place is used` line |
| P3 | `adb shell am broadcast -a android.intent.action.TIME_SET` (a clock-change reschedule) while YouTube is still in front; `optimize/music.sh adhan` | still Kuwait's time |
| P4 | Press Home, wait 1 min; `optimize/music.sh adhan` | still Kuwait's time |
| P5 | Put the REAL city back: `optimize/saver.sh city '<the real one>'`; wait 1 min; `optimize/music.sh adhan` | the next time follows the real city again |
| R4 | DAYTIME ONLY, the owner's volume rule allows it: pair the PC's Chrome, `optimize/music.sh adhan now` with YouTube in front, press ▼ on the page, then «أوقف الأذان الآن»; then phone control off and the PC's pairing removed | the adhan plays; ▼ doesn't break it; the button stops it |
| L1 | Push the owner's EXACT final narration text as tester/voice-owner/lines-final.txt (ask him in Arabic if you don't have it) | pushed |
| Z | Restore everything from the start lines; `optimize/saver.sh updatechannel latest` | "restored: yes/no" |

REPORT.md: "step 11", the table with the next-adhan times written as "Kuwait time ok / Riyadh time WRONG" (no real city named), the UrTVAdhan/TclAppBoot lines, any `!!` lines. Push; tell the owner in Arabic that the test is finished and how many problems there were.

## Official app icons for the video and posters (owner: "find online, all need to be good"; no owner needed)
| # | Do | Right when |
|---|---|---|
| I1 | For each app, open its OFFICIAL Google Play page in the PC's browser (or fetch it) and take the store icon at the highest size: the page's `og:image` / the icon's `play-lh.googleusercontent.com/...` URL with `=s512` (or `=w512-h512`) at the end. Apps: **ثمانية** (search Play for "ثمانية" / "Thmanyah", publisher Thmanyah), **شاهد** (Shahid, publisher MBC; package net.mbc.shahid or its TV app), **YouTube** (com.google.android.youtube.tv, or com.google.android.youtube), **سين** (com.incube.seen.tv). If the TV app has a wide TV banner image on its page or in the Play "Android TV" listing, save that too | 4 icons (+ banners if any) |
| I2 | Check each: square PNG/WebP at least 512×512, the real current logo (not a fan copy, not a screenshot crop with UI around it), no rounded-corner shadow baked in if a clean one exists. Convert WebP to PNG | clean files |
| I3 | Save as tester/icons/thmanyah.png, shahid.png, youtube.png, seen.png (and *-banner.png if found) + tester/icons/SOURCE.md: for each, the Play page URL, the image URL, the size, and the date | done |
| I4 | Push ONLY tester/icons/ to work/tester-report (reset to origin/work/tester-report first) | pushed |
