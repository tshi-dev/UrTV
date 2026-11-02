# Optimizing a TCL Google TV / Android TV — what we learned

From hands-on work on three TCL sets (Android 11, 12 and 14, 1.7–2.3 GB RAM, 32-bit). The tools are in `optimize/`.

## What works

| Change | Effect | How |
|---|---|---|
| Disable TCL/Google telemetry, promos and demos | +85 to +215 MB free RAM, nothing lost | `optimize/lists/tcl-safe.txt` |
| Restrict the Play Store in the background (never disable it) | its idle CPU fell from ~31% to ~6% on one set; the app still opens and updates | `optimize/tweaks.sh apply` |
| Animations at 0.5× | menus feel faster | `optimize/tweaks.sh apply` |
| Cached apps frozen (no CPU in the background); Android's own limit on how many closed apps stay in memory | apps reopen fast. Lowering the limit to 16 (`CACHED_LIMIT=16`) is off until measured again: Android 14 raised it on purpose (up to 30% fewer cold starts); a TCL Google TV (Android 14) boots with 1024, and there the lowered setting was gone after a restart, so the first after-measurements ran without it | `optimize/tweaks.sh apply` (`restore` undoes it) |
| The stock home's helpers off once our home screen works (`tclhome_passive` ~80 MB) | dead weight with another home screen | done by `optimize/install.sh launcher` after the check; `optimize/restore.sh` re-enables |
| Free storage | a set at 96–100% full was slow everywhere; app download folders (e.g. a video app's offline downloads) were the cause | check `df /sdcard` and `du -sh /sdcard/Android/data/*`; ask before deleting anything |

Judge results by `MemAvailable`, the Play Store's CPU and how the remote feels — not by load average,
which sits high on these sets from I/O wait even when idle.

## What our own apps do to stay light

Field readings put the screensaver at 120–250 MB of free RAM while it runs, mostly the WebView. So:

- The page writes to the screen only when the text changed (the clock once a minute, not 60 times), saves its
  panels only when they differ, and asks for live scores only inside a followed match's window. Nothing animates
  once the photo has settled.
- The WebView is taken out of its window and destroyed when the dream ends; nothing static keeps it alive.
- The page and version check runs only when the screensaver starts, and only if the last one is 6 hours old or
  more (on a background thread, one at a time); an idle TV makes no network calls for it. The persisted 6-hour job
  stays, but it only starts the app's process and finishes at once: it also wakes the app after a reboot, which
  some TCL sets need for the first screensaver to start.
- The home screen's card (if turned on) is paused, its timers too, when an app is in front, and destroyed with the
  activity.
  App tiles are kept at the size they are shown, in a cache of 6 MB at most.
- `optimize/measure.sh` prints the two apps' memory (TOTAL PSS) under its table, to compare releases.

## Running it

`optimize/run.sh` does the whole setup in order and remembers each finished step: over slow or dropping ADB just
run it again. It ends with «Done, with problems:» and exit code 1 when `report.md` has `!!` lines (fix them, run it again:
only the report is redone), and it starts from the first step when the TV (its serial) is not the one of the last run.
Results go to `optimize/run/` (before/after table in `summary.md`, `report.md`). `optimize/reset.sh`
puts a TV back as it came, including older versions of these apps. The ADB client (`optimize/tv.py`) retries and
reconnects by itself, uploads APKs in verified pieces that resume after a drop, and restarts the TV only through
`tv.py --reboot` (sent once, so a retry can never restart it twice; the TV counts as back only from a new boot, by the
kernel's boot id or the uptime, so one that is slow to shut down is not reported back too soon).
It also remembers which TV the setup started on (serial, make and model, in `tv-id.json` next to the ADB key in `~/.urtv`, which stays on this
computer and outside the clone, so a new clone is the same computer): a command that changes the TV first checks, once per connection, that the device answering is that one, and
stops with a `!!` line naming both models if not (two devices on one Wi-Fi, a box and the TV). The same TV on a new
address passes; `tv.py --new-tv` is the deliberate switch.

## What must stay on

`optimize/lists/keep.txt`, enforced by `disable.sh`. In short: **AirPlay, Chromecast, screen mirroring, voice
assistant, the remote and keyboard, HDMI inputs and the tuner, the quick panel, firmware updates, the Play Store,
WebView and Google services.** One set had AirPlay, mirroring and voice disabled; those were mistakes. A set
optimized before still had voice, mirroring and firmware updates off after a reset, so `optimize/keep.sh` turns
everything in keep.txt back on; `run.sh` and `reset.sh` run it.

## Home screen problems (what we have seen, and the fix)

**Why Home can't simply be changed on a TCL Google TV.** Everything the system offers reports success and changes
nothing: `cmd package set-home-activity`, `cmd role add-role-holder android.app.role.HOME <pkg>` and the official
picker (`am start -a android.settings.HOME_SETTINGS`) all leave Home on the stock launcher. The HOME candidates
rank `launcherx` (2) > `setupwraith`, the setup app's RecoveryActivity (1) > any installed launcher (0) >
FallbackHome (-1000) (`cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME`),
and an app can't rank itself higher: a rebuilt launcher with `android:priority="15"` on its HOME filter still
reported 0. On every Home press and boot the firmware starts the stock launcher by name (WindowManager: `start home
ainfo is com.google.android.apps.tv.launcherx reason is startDockOrHome`; `TclBootAppManager:
startBootAppBeforeLauncher`). So ours only becomes and stays home with `launcherx` **and** `setupwraith` disabled,
and that is the risky part: disable `launcherx` alone and `setupwraith` takes Home; disable both while ours is
missing or broken and the TV has no home at all. `install.sh launcher` does it the safe way (below), and it works:
it is how the first TCL set got our home screen after the black screen.

| What you see | Why | Fix |
|---|---|---|
| Black screen, reboot loop, Wi-Fi gone for ~10 minutes (so ADB too) | the stock home (`launcherx` or `tvlauncher`) and the setup app (`setupwraith`) were disabled with no working replacement (3 October, before these tools) | someone at the TV rejoins Wi-Fi from the remote's quick settings; then ADB answers again and `optimize/restore.sh` turns both back on (what worked then: the same `pm enable`s in a background loop on the TV). Prevented by `install.sh launcher`, below |
| No home at all after our home app was removed: black screen, Home does nothing | ours was the only home enabled: removed by hand, or replaced by a build signed with another key | `optimize/restore.sh` (no list: only the home screen), then `optimize/install.sh launcher`. Since the move to the new signing key (October), `install.sh` turns the stock home on before it replaces a home app signed with another key |
| Our home is installed, but Home opens Google TV | ADB was slower than the 5-minute safety switch, which put the stock home back (`!! The safety switch put the stock home back`, printed only when Home, the stock home's state and the journal all show it; otherwise the line says what the TV shows: the stock home back, still off, or Home already ours) | `optimize/install.sh launcher` again; for «still turned OFF», `optimize/restore-home.sh` first |
| The home switch pauses: `Home switch paused… the setup wizard owns the saved Wi-Fi network` | the network was saved in the first-run wizard, so turning the wizard off would delete it | the person forgets the network in the TV's Settings and joins it again, says «كمّل», and `optimize/install.sh launcher` (or `run.sh`) goes on: in the safe order (the wizard off, the Wi-Fi checked, our home in front, then the stock home off) if the network moved; as before if not |
| Casting (`com.google.android.apps.mediashell`) stops every few minutes after the stock home was turned off, on Android 14 (**unverified**) | upstream (Gathin23/cleanyourtv) infers that Android 14's app freezer stops Cast once the stock launcher is disabled; we have not reproduced it | try `optimize/tweaks.sh restore` (puts the cached-apps freezer back to what the TV had) and cast again; if that is the cause, tell us through `share.sh --problem` with the TV's model. Do not turn Cast's package off |
| An ADB job started on the TV ends the moment the ADB session closes (a TCL on Android 14; **confirmed**: `nohup` and `setsid` both) | the firmware ends what a shell leaves running | nothing to fix: `install.sh` then has no on-TV safety switch and says so; the safety net is this computer plus the journal (see «The safe way to switch» below) |
| The same, twice: `!! Home did not open the new launcher` | a firmware that still opens something else with both disabled (not seen on our TCL sets) | keep the stock home: `SKIP_LAUNCHER=1 optimize/run.sh`; open تلفزيونك from the apps row, by voice («open تلفزيونك»), or map a remote key to it with Key Mapper (FOSS, `io.github.sds100.keymapper`, e.g. double-press Home) |
| Home settings, the role or `set-home-activity` say our launcher is home, but Home still opens Google TV | the firmware pins the stock launcher (above) | don't keep trying those; `optimize/install.sh launcher` |
| Google TV home and none of our apps, after `optimize/reset.sh` | that is what reset does | set up again from the prompt |

The safe way to switch, built into `optimize/install.sh launcher`:
1. Install the new home first.
2. Write the journal (`optimize/run/home-switch.json`: the stock home, the setup app and who held Home before), then start
   `optimize/deadman.sh` on the TV with a nonce of this switch: unless `/data/local/tmp/home_ok.<nonce>` appears within 300 s it
   re-enables the stock home and the setup app and presses Home (a marker of an older switch does not count). It writes its pid
   and a heartbeat every 10 s (a nonce is required: an old plain `home_ok` can never disarm a switch). A restart of the TV kills it (stock Android cannot
   prevent that): the next `install.sh` or `run.sh` reads the journal and, if Home is not ours and the marker is not there,
   puts the stock home back; `optimize/restore-home.sh` does it on demand (ADB needed).
   On some TVs (a TCL with Android 14) a job started over ADB ends with the ADB session, so this safety switch cannot stay alive on the
   TV (setsid and nohup do not help). `install.sh` then says so in a plain line and carries on: the safety net is this computer (it
   checks Home and puts the stock home back at once) plus the journal, which records `safety_switch: unavailable`.
3. Disable the setup app, then the stock home (this order avoided the black screen).
4. Press Home and check the new launcher is in front (3 tries); only then create `home_ok.<nonce>`, and confirm the safety
   switch hadn't already fired.

## TCL quirks

- After a reboot TCL keeps third-party apps frozen until they are opened once: boot broadcasts don't arrive and
  the first screensaver start can hang. Open شاشة تلفزيونك once after a power cut.
- Disabling a package is per user and reversible: `pm enable <package>`. Don't `pm uninstall` system apps.
- Remote access tools (VPNs etc.) that are put in the restricted bucket stop reconnecting after a reboot — only
  restrict them once remote access is no longer needed.
