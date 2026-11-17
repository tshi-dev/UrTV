# step 88 — the most reliable way to make UrTV the main home on the owner's Google TV (TCL «Smart TV Pro» G08, Android 14)

**Winner: R2g — «إضافات تلفزيونك» window watch with a fixed loop guard** (test add-on build **409**, branch `work/tester-exp` **e37f132c**, diff = 2 files, +51/−4: `HomeWatch.java` and `ExtrasA11yService.java`; built only as a branch artifact, never published).
It needs: the add-on installed + its accessibility service on + «wanted» = yes (the setup's word that the person chose UrTV). It does NOT need the HOME role (Google TV takes the role back within 2 min anyway and the Home key goes to Google TV whatever the role says — step 87).

## Results (Home = `input keyevent KEYCODE_HOME`; «front» read 2 s and 6 s after)
| angle | Home → UrTV | boot (first Home / Home from YouTube) | stress / mash | notes |
|---|---|---|---|---|
| R0 nothing | 0/4 | — | — | |
| R1 role only | 0/3 | — | — | role taken back ≤ 2 min; Home → Google TV even with the role (step 87 B, E) |
| R1+R2 (376 add-on as released) | **2/20 (10 %)** | — | — | 2–3 stock windows per press each counted → «3 in 20 s» guard → 5 min paused |
| R1+R2b (381: one count per 1.5 s) | 20/20 | 3/3 / 3/3 | **Home every 6 s ×10: 3/10** (guard trips) | |
| R2b with the role on Google TV | 20/20 | — | | the watch alone is enough |
| R2c (397: a press over an app never counts) | — | — | every 6 s from YouTube 10/10; **on our home ×4 → paused** | |
| R2d (403) | — | — | on ours ×8 at 3 s 8/8, from YouTube 10/10; **mash 1/s on ours → paused** | |
| R2e (404: pause 30 s; presses over apps pass even in a pause) | — | — | mash still pauses, but recovers on the next press from an app or after 30 s | |
| R2f (405: one count per burst of stock windows (0.7 s); pause only at 6 bursts in 4 s) | 19/20 | — | **mash 1/s ×10 and 0.5 s ×10 on ours: no pause** | the 1 miss: YouTube put itself back ~0.7 s after our bring (YouTube quirk) |
| **R2g (409: + if the app Home was pressed over comes back within 1.5 s of our bring, bring ours once more)** | **20/20 + YouTube 40/40 = 60/60 (100 %)** | **3/3 / 3/3 (role = launcherx at boot)** | mash and stress as R2f | flash ≈ 0.7–0.9 s (below); 30-min idle: see the end |
| R3 catch KEYCODE_HOME in the add-on (380+) | **not measurable with adb** | | | injected Home never reaches `onKeyEvent` here; `sendevent` to the IR device is blocked by SELinux. Needs the real remote (logcat -s UrTVKey) |
| R4 start at boot | not needed | | | after every boot our screensaver/home is in front and the first Home goes to UrTV |

**Flash of Google TV** (screenrecord at 50 fps, 3 runs): YouTube → Google TV fades in (~0.15 s), Google TV fully shown ~0.5–0.6 s, then fades to UrTV: **≈ 0.7–0.9 s of Google TV** each Home press. Only R3 (catching the key before Google TV) could remove it — needs the real-remote check.
**Side effects (409):** Settings key in YouTube → Google TV's own settings panel (dashboard), left alone ✓; Assistant (219) and Input (178) → their overlays ✓; Search in YouTube → YouTube's search ✓; Back works in apps and on our home ✓. A real firmware «fight» could not be reproduced on this TV (it does not put Google TV back by itself); the burst guard (6 in 4 s → 30 s off) is the safety net.

## The fix to ship (what R2g changes)
1. `HomeWatch.decide(..., boolean fight)`: **a stock home that came over another app (the person pressed Home there) is always brought and never counted** (also during a pause).
2. Over our own home: the 2–3 stock windows one Home press makes within ~0.7 s count **once** (a burst); pause only at **6 bursts in 4 s**, and only **30 s** (was: 3 brings in 20 s → 5 min).
3. `ExtrasA11yService`: remembers the app Home was pressed over; if that app comes back within 1.5 s of our bring (YouTube does this sometimes), ours is brought once more.
4. (The tester-exp build also forces the key filter on and catches KEYCODE_HOME — for R3 only; do NOT ship that part until the real-remote check.)
And the setup must leave «wanted» = yes and the add-on on (in 86 B the setup finished with `wanted no → ours not chosen`, so nothing was brought).

## 30-minute idle (R2g, 03:53 → 04:24)
After 30 min untouched: our screensaver in front, add-on still bound, role = launcherx. First Home → UrTV; then Home from YouTube ×3 → UrTV, UrTV, UrTV. **4/4.**
**Final verdict: R2g = 100 % Home (64/64 presses), 3/3 boots, 30-min idle OK, no side effects; only the ~0.8 s Google TV flash remains.**
