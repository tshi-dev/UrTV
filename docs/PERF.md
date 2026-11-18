# Speed and network: how to measure (4.3.3, G3 G4 A14)

For the tester. Every step runs once on the old build (4.3.2) and once on the new one, on the same TV, with the same settings, the TV on its usual Wi-Fi. Write the numbers in a table: old, new, the change in %. Nothing here changes a setting of the person's except step F, which puts it back.

`H=sa.urtv.home`, `S=sa.urtv.screensaver`. Before each step: `adb logcat -c`.

## A. Network requests (G3)

The new build writes one line per 10 minutes of requests, per app, tag `UrTVNet`:
`net 10 min: 14 requests (1.4/min), in 220 KB, out 9 KB; raw.githubusercontent.com 8, api.open-meteo.com 4, ...`
A quiet app writes nothing; its next line's minutes cover the quiet time.

1. `adb logcat -v time,process -s UrTVNet > net.txt &` (the process column tells the two apps apart: `adb shell pidof sa.urtv.home sa.urtv.screensaver`).
2. The home idle: press Home and leave the TV for 30 minutes. Then the screensaver: start it (`adb shell am start -n com.android.systemui/.Somnambulator`, or wait for the TV's own timeout) and leave it 30 minutes. Stop the logcat.
3. Note for each app and each half hour: requests per minute, KB in, the hosts. A line's window can start in one half hour and end in the next: take the lines whose window is wholly inside it.
4. The old build has no such line. Its bytes: `adb shell dumpsys package sa.urtv.screensaver | grep userId` (and the home's), then `adb shell dumpsys netstats detail` before and after each half hour; the `rb=` (received bytes) of that uid, summed, after minus before. Do the same on the new build to compare like with like.
5. Offline: with the screensaver on, unplug the router's internet for 10 minutes, then plug it back. The page does not try its periodic loads while the TV says it is offline, and does each once when it is back; the apps' own data files (the markets, the next match) wait 15 s, then 30 s, ... up to 5 minutes between tries. Expect far fewer requests in the line that covers the outage than in a normal one; the screen must show its data again within a few minutes of the network coming back.

## B. Cold start of the home (G4)

```
for i in 1 2 3 4 5; do
  adb shell am force-stop sa.urtv.home; sleep 3
  adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME -n sa.urtv.home/.MainActivity | grep -E "TotalTime|WaitTime"
  sleep 8
done
```
The median TotalTime. Target: at least 10% lower than the old build.

## C. Return to the home (G4)

```
for i in 1 2 3 4 5; do
  adb shell am start -W -a android.settings.SETTINGS > /dev/null; sleep 5
  adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.HOME | grep -E "TotalTime|WaitTime"
  sleep 3
done
adb logcat -d -s UrTVHome | grep "resume to first photo frame"
```
The median TotalTime, and the median of the «resume to first photo frame» lines. Target: at least 10% lower.

## D. Tray jank (G4)

```
adb shell input keyevent KEYCODE_HOME; sleep 3
adb shell dumpsys gfxinfo sa.urtv.home reset > /dev/null
for r in 1 2; do
  for i in $(seq 8); do adb shell input keyevent KEYCODE_DPAD_RIGHT; sleep 0.4; done
  for i in $(seq 8); do adb shell input keyevent KEYCODE_DPAD_LEFT; sleep 0.4; done
done
adb shell dumpsys gfxinfo sa.urtv.home | grep -E "Total frames rendered|Janky frames|50th|90th|95th|99th"
```
Three runs; the median «Janky frames» %. Baselines: 4.3.1 19%; 4.3.2 from the tester's run. Target: at least 10% lower (relative).

## E. Memory and CPU

```
adb shell dumpsys meminfo sa.urtv.home | grep -E "TOTAL PSS|TOTAL RSS"
adb shell dumpsys meminfo sa.urtv.screensaver | grep -E "TOTAL PSS|TOTAL RSS"
adb shell top -b -n 6 -d 10 | grep -E "sa\.urtv"
```
Once with the home idle for 5 minutes, once with the screensaver on for 5 minutes. Note the PSS and the mean %CPU of each app.

## F. «الوضع الخفيف» (A14)

1. `optimize/saver.sh show | grep -E "^lite|^ramtier"`: `lite: auto (light now: yes|no)`. «yes» on a TV with Android's low-RAM flag, under 2000 MB or 2 cores or fewer (`adb shell nproc`, `adb shell cat /proc/meminfo | head -1`).
2. Screensaver settings: the row «الوضع الخفيف» says «تلقائي، ومفعّل على هذا التلفزيون» or «تلقائي، ومطفأ على هذا التلفزيون». OK cycles: «مفعّل», «مطفأ», back to «تلقائي…».
3. `optimize/saver.sh lite on`, then `adb shell am force-stop sa.urtv.home` and press Home. Look: on the home the focused app grows and is highlighted but has no shadow under it and no shine passing over it; the screensaver (start it) has no seconds, its changes happen at once (no fades), one photo at a time, the card beside the clock without the blurred photo behind it. Repeat D and E with it on.
4. `optimize/saver.sh lite off` on a TV that is «light now: yes»: `show` says `light now: no`; the seconds and the fades are back.
5. `optimize/saver.sh lite auto` to finish (the person's setting as it was).

## What to send back

The table of A to E (old, new, %), the `UrTVNet` lines of A, and for F what was seen at each step and any frame or step that looked wrong.
