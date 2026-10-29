# TVs seen in the field

If your TV isn't here, add a row from your run.

What the first reports from real TVs showed. «Home switch» is `optimize/install.sh launcher`;
«Screensaver from ADB» is whether Android starts our screensaver over ADB (where it doesn't, `shot.sh` shows our
app's preview of the same page and `measure.sh` writes «not measured»). Times are the whole `optimize/run.sh`.
«N reports» is how many setup reports that model appeared in; every one of them switched the home screen.

| TV | Android | Home switch | Screensaver from ADB | ADB | Watch out for | Typical time |
|---|---|---|---|---|---|---|
| TCL with Android 14 (seen on the owner's TV; model not recorded) | 14 | works | not reported | 5555 | background jobs started over ADB end the moment the ADB session closes (nohup and setsid too), so the 5-minute on-TV safety switch cannot stay alive: `install.sh launcher` says so and relies on this computer plus the journal; the stock home is still put back at once if Home does not open ours; it also refuses to start a killed app from the background while another app starts (`TclAppBoot … callee_doesn't_have_OP_ASSOC_START_permission`, «Failed to find provider info» for the screensaver's settings): the home keeps the last good settings and prayer place meanwhile (log tag `UrTVAdhan`); YouTube's profile picker takes no accessibility arrows or OK from the phone remote (they log `route=global result=done` and nothing moves; inside YouTube they work, and the TV's own remote works on the picker) | not reported |
| TCL Smart TV Pro (G05) · 8 reports | 11 | works | yes | 5555 | `com.tcl.smartalexa` ships off outside Alexa's regions, expected | 14 min |
| TCL Smart TV Pro (G07) · 8 reports | 11 | works | yes | 5555 | sometimes slow to come back after a restart; `com.tcl.smartalexa` ships off outside Alexa's regions, expected | 14 min |
| TCL Smart TV Pro (C05) · 2 reports | 11 | works | not reported | not reported | the remote's Guard (cleaner) button opens only `com.tcl.guard`, so that package stays on unless picked | not reported |
| TCL Smart TV Pro (BeyondTV4) · 9 reports | 11 | works | yes | 5555 | `com.tcl.smartalexa` ships off outside Alexa's regions, expected | 14 min |
| TCL BeyondTV · 6 reports | 9 and 11 | works | not reported | 5555 | – | not reported |
| TCL BeyondTV2 | 9 | works | not reported | not reported | – | not reported |
| TCL UnionTV | 9 | works | yes | 5555 | – | 15 min |
| TCL Smart TV Pro (G08) · 18 reports | 14 | works (launcherx + setupwraith off) | no, preview only | 5555; the first probe may hang, run it again | a Wi-Fi drop during the switch: the safety switch restores the stock home and `optimize/install.sh launcher` retries the switch by itself (once); Play Protect refused the home APK over ADB at first (`install.sh` now turns that check off for the install only and puts it back) | 11 min |
| TCL Smart TV Pro (G08) | 12 | works | not reported | not reported | SmartTube (about 25 MB) uploaded to 100%, then the install ended in a read timeout on the TV (slow steps now run in the background on the TV; `SKIP_EXTRAS=1` skips the extras) | not reported |
| TCL Smart TV Pro (G10) | 12 | works | not reported | not reported | SmartTube install timed out on every run (see the G08 row, Android 12); Play Protect refused the home APK over ADB at first (`install.sh` now turns that check off for the install only and puts it back) | not reported |
| TCL 65C7K (Google TV) | not reported | not reported | no: this firmware refuses third-party dreams, even Android's own «Colors» (`com.android.dreams.basic`): with Projectivy as the launcher the system logs «Entering dreamland» and closes the dream at once; with the stock launcher starting it over ADB (`com.android.systemui/.Somnambulator`) shows nothing and logs nothing. Only Google's own Backdrop ambient mode runs, and neither ours nor «Colors» is in the TV's Ambient mode list (`dumpsys dreams`: no current dream, dreaming enabled, no doze component) | not reported | the UrTV home screen works; the screensaver does not run on this model (one report) | not reported |
| Xiaomi Mi Box S (MIBOX4), stock Android TV home · 6 reports | 9 | works | not reported | 5555 | – | not reported |
| Xiaomi MiTV-AESP0 · 4 reports | 9 and 10 | works | not reported | not reported | – | not reported |
| Xiaomi MiTV-MOOR2 · 3 reports | 11 | works | no | 5555 | screenshots may fail | 17 min |
| Xiaomi MiTV-AFKR0 · 3 reports | 11 | works | not reported | not reported | – | not reported |
| Xiaomi MiTV-AYFR0 | 11 | works | not reported | not reported | – | not reported |
| Xiaomi MiTV-AFMU0 | 14 | works | not reported | not reported | Play Protect refused the home APK over ADB at first (`install.sh` now turns that check off for the install only and puts it back); SmartTube (about 25 MB) uploaded to 100%, then the install ended in a read timeout on the TV (slow steps now run in the background on the TV; `SKIP_EXTRAS=1` skips the extras) | not reported |
| Xiaomi MiTV-AXTU0 | 14 | works | not reported | not reported | – | not reported |
| Xiaomi MiTV-MZTU0 · 2 reports | 14 | works | not reported | not reported | – | not reported |
| Xiaomi MiTV-MOOQ0 | not reported | not reported | not reported | not reported | – | not reported |
| Sony BRAVIA 4K UR3 (BRAVIA_UR3) · 5 reports | 10 | works, first time | yes | turn on USB debugging and network debugging first | one TV showed up only as a Cast device until it was unplugged and plugged in again; SmartTube upload over Wi-Fi is slow but completes | 14 min |
| Sony BRAVIA VH1 | 12 | works | not reported | not reported | SmartTube (about 25 MB) uploaded to 100%, then the install ended in a read timeout on the TV (slow steps now run in the background on the TV; `SKIP_EXTRAS=1` skips the extras) | not reported |
| Sony BRAVIA 4K VH2 | not reported | not reported | not reported | not reported | SmartTube (about 25 MB) uploaded to 100%, then the install ended in a read timeout on the TV (the install is now watched with short looks, and an extra that still fails is skipped by itself; `SKIP_EXTRAS=1` skips the extras) | not reported |
| Sony BRAVIA 4K VH22 | not reported | not reported | not reported | not reported | – | not reported |
| Sony BRAVIA 4K GB ATV3 | 9 | works | not reported | not reported | – | not reported |
| Sony BRAVIA 4K GB ATV2 | 9 | works | not reported | not reported | – | not reported |
| Skyworth SWTV-20AE-PRO | 11 | works (tvlauncher off) | no | 5555, an occasional timeout that retries by itself | – | 14 min |
| Skyworth SWTV-20AE | 10 | works | not reported | not reported | – | not reported |
| Skyworth SWTV-22AE | 11 | works | not reported | not reported | – | not reported |
| Skyworth SWTV-22AE-FHD (umeda) | not reported | not reported | not reported | not reported | – | not reported |
| Skyworth IRIS 2K (beomil) | 14 | not reported | not reported | not reported | – | not reported |
| Skyworth SWTV SW3_ATV | 9 | not reported | not reported | not reported | – | not reported |
| Skyworth SWTV-24AE-4K NF | 14 | works | no | port 5555 closed: pair once through Wireless debugging, then `adb tcpip 5555`; the port disappears after every reboot | reconnect after each restart; APK uploads slow | 39 min |
| Skyworth TORNADO 4K | 11 | works | not reported | not reported | – | not reported |
| SHARP Android TV (Skyworth SW4H_FF) · 2 reports | 11 | works | not reported | not reported | – | not reported |
| Skyworth JAWWY TV 2.0 (HP40A, JAWWY-TV-2.0) | 12 | works | not reported | 5555 | operator image: a system service removed every app installed with a plain `pm install` about 2 s later, so `install.sh` installs it again with the Play Store as the installer when that happens; its own launcher keeps coming back; SmartTube (about 25 MB) uploaded to 100%, then the install ended in a read timeout on the TV (slow steps now run in the background on the TV; `SKIP_EXTRAS=1` skips the extras) | not reported |
| Skyworth, Google TV (model not given) | 14 | not reported | not reported | port 5555 closed: pair through Wireless debugging; the port disappears after every reboot | reconnect after each restart | not reported |
| Changhong AI PONT (Wansa / Classpro) | 11 | works | not reported | not reported | seen on several sites | not reported |
| Changhong AI PONT (central) | 14 | works | not reported | not reported | SmartTube (about 25 MB) uploaded to 100%, then the install ended in a read timeout on the TV (slow steps now run in the background on the TV; `SKIP_EXTRAS=1` skips the extras) | not reported |
| Changhong AI PONT (longshan) | not reported | not reported | not reported | not reported | – | not reported |
| Changhong AI PONT (samseong) | not reported | not reported | not reported | not reported | – | not reported |
| Haier Android TV FF Pro | 14 | works | not reported | not reported | – | not reported |
| Haier Android TV DVB (dupont) | not reported | not reported | not reported | not reported | – | not reported |
| Haier MatrixTV BE · 2 reports | 14 | works | not reported | not reported | Play Protect refused the home APK over ADB at first (`install.sh` now turns that check off for the install only and puts it back); SmartTube (about 25 MB) uploaded to 100%, then the install ended in a read timeout on the TV (slow steps now run in the background on the TV; `SKIP_EXTRAS=1` skips the extras) | not reported |
| Hisense HiSmartTV A4 (Toshiba) | 9 | works | not reported | not reported | – | not reported |
| Hisense SmartTV 4K FFM (hengshan) | 12 | not reported | not reported | not reported | SmartTube (about 25 MB) uploaded to 100%, then the install ended in a read timeout on the TV (the install is now watched with short looks, and an extra that still fails is skipped by itself; `SKIP_EXTRAS=1` skips the extras) | not reported |
| Hisense HiSmartTV A4 | 11 | not reported | not reported | not reported | – | not reported |
| Chromecast with Google TV (sabrina) · 2 reports | 14 | works | not reported | no Network debugging, only Wireless debugging: `adb pair`, then `adb tcpip 5555`; every restart turns it off and closes 5555 | pair and run `adb tcpip 5555` again after each restart (wireless debugging must be paired again after a reboot); SmartTube (about 25 MB) uploaded to 100%, then the install ended in a read timeout on the TV (slow steps now run in the background on the TV; `SKIP_EXTRAS=1` skips the extras) | not reported |
| Chromecast HD (boreal) | 14 | works | not reported | same as the sabrina: Wireless debugging only, `adb pair`, then `adb tcpip 5555`; closes at every restart | pair again after each restart; SmartTube (about 25 MB) uploaded to 100%, then the install ended in a read timeout on the TV (slow steps now run in the background on the TV; `SKIP_EXTRAS=1` skips the extras) | not reported |
| Google TV Streamer (kirkwood) | not reported | not reported | not reported | not reported | – | not reported |
| SCBC R4 | 11 | works | not reported | 5555 | the TV's IP changed mid-setup: rediscovery (`optimize/find-tv.sh`) fixed it | not reported |
| SCBC ATV R2 | not reported | not reported | not reported | not reported | – | not reported |
| JPE at30a7 (DANSAT) | 13 | works | not reported | not reported | SmartTube (about 25 MB) uploaded to 100%, then the install ended in a read timeout on the TV (slow steps now run in the background on the TV; `SKIP_EXTRAS=1` skips the extras) | not reported |
| Philips 4K (Funai guui) | 11 | works | not reported | not reported | – | not reported |
| Philips PH1M_EA_9970A | not reported | not reported | not reported | not reported | – | not reported |
| H96_Max_X3 | not reported | not reported | not reported | not reported | – | not reported |
| KTC mt5867 | not reported | not reported | not reported | not reported | – | not reported |
| UGOOS AM8 | not reported | not reported | not reported | not reported | – | not reported |
| Vestel | not reported | not reported | not reported | not reported | – | not reported |
| SEI A1 | not reported | not reported | not reported | not reported | – | not reported |
| MOKA R3G | not reported | not reported | not reported | not reported | – | not reported |
| xangshi XS97 | not reported | not reported | not reported | not reported | – | not reported |
| SHARP AQUOS | not reported | not reported | not reported | not reported | – | not reported |

## The remote's shortcut keys

After the home switch the stock launcher is switched off, and the remote's shortcut keys (Settings, YouTube, Netflix, the star)
stopped working. Six reports: Haier MatrixTV (A14), TCL G08 (three), Xiaomi MiTV-MZTU0, Changhong dangni (A14). A report found the
cause: YouTube and Netflix arrive as `KEYCODE_BUTTON_3` and `KEYCODE_BUTTON_4`, and the only receiver of Android's global-button
broadcast on these TVs is in the stock Google TV launcher (`com.google.android.apps.tv.launcherx`, its `GlobalKeyListener_Receiver`),
which is off with it. Our home now answers these keys **while it is in front**:

| Key | Android key | What our home does | Reported on | Verified |
|---|---|---|---|---|
| YouTube | `KEYCODE_BUTTON_3` (190) | opens `com.google.android.youtube.tv`, only if installed | the six reports above | key number from the report and Android's reference; needs a TV |
| Netflix | `KEYCODE_BUTTON_4` (191) | opens `com.netflix.ninja`, only if installed | the six reports above | same; needs a TV |
| Settings | `KEYCODE_SETTINGS` (176), and on a TCL (seen on the owner's, build 144) `KEYCODE_NOTIFICATION` (83) | opens **our** settings (the screensaver app's `SettingsActivity`, the same screen as the «إعدادات تلفزيونك» tile of «مركز التحكم»), behind «قفل الإعدادات» (`@urtv`); without that app it falls back to the TV's own settings (behind `@tvsettings`). The TV's own settings stay on the «الإعدادات» pill and tile | the six reports above; the TCL's own log line (`UrTVKeys`) said 83; the step-6 test of 3.145 found the key opened Android's own settings (`com.android.tv.settings`), which was the old rule, not a missed key | 176 is Android's reference number; 83 is what a TCL sends. The YouTube and Netflix keys never reach the app there (the system takes them first: they open, our log shows nothing) |
| Star | Linux `KEY_JOURNAL`, scan code 0x242 (578); Android's generic key map has no name for it, so the keycode may be `KEYCODE_UNKNOWN` (0) | what the Menu key does: the «المصادر» list (our choice: the star is the nearest thing to Menu) | TCL G08 | scan code from the report and the Linux headers; the keycode is not known, so the scan code is matched; needs a TV |

How: a key acts on the first press only; its repeats and its release are consumed, so nothing else reacts. An app that is not
installed (or that kids mode hides) leaves the key as it was. Every open goes through the family lock, so a locked app stays locked from
its key. The adhan's any-key rule is asked first.

What we did **not** find a source for, so did not map: `KEYCODE_BUTTON_1`, `_2`, `_5` and up, `KEYCODE_PROG_*`, `KEYCODE_TV_*` and the
`KEYCODE_MEDIA_*` shortcut variants on these remotes (Android's reference gives the numbers, not which TV puts an app on them). Add a key
only with a report that names it.

Honest notes:
- We do **not** register a receiver for the global-button action: the system sends it as an explicit broadcast to the component named
  in the framework's `global_keys` list, so a third-party receiver for the action is never called.
- Unverified, and possibly against us: AOSP's `PhoneWindowManager.interceptKeyBeforeDispatching` calls `GlobalKeyManager.handleGlobalKey`
  **before** the key is dispatched, and consumes the keys listed in `global_keys` even when the receiver it names is disabled. So on a TV that
  lists these keys in `global_keys` the key is probably taken **before our home sees it**, and the handling in our home does nothing there.
  The code stays (it costs nothing and acts only on these keys); **verify on the TV** whether the key reaches `dispatchKeyEvent` at all:
  `adb logcat -s UrTVKeys` prints `key=<keycode> scan=<scancode>` for every first press that reaches our home. Inside other apps nothing
  of ours sees these keys either way; only the accessibility key filter (the separate branch `work/khalid-settingskey`) could see keys in
  other apps (it sees them before the window manager does); this change does not depend on it and leaves it as it is.
- If after this change the Settings key still opens Android's own settings, the TV took the key before our home saw it (no `UrTVKeys` line with `key=83` or `key=176` appears while the key is pressed on our home): then nothing in the app can change it; use the «إعدادات تلفزيونك» tile of «مركز التحكم» instead.
- Nothing here is checked on a real TV yet: each row needs one press on each of the six models.

## D-pad direction on the home screen and its rows

The home screen reads right to left. The favourites row (the app tiles under the pills) puts the **first favourite at the right edge**:
**right** moves toward the first favourite, **left** moves away from it, toward the last. The pills row above reads the same way
(«المصادر» at the right end, or «يُتلى الآن» when it shows). On a TV whose own D-pad feels reversed (four reports, a Sony and a TCL),
the key is not swapped: the row simply starts on the right, like Arabic text, so «right» is «back toward the start». The first fix
(a held key walks the row by hand) is in; the screens built in code now pin their own direction, and the surah grid keeps a key at
its end instead of handing it to Android.

## On the computer

- **macOS:** Homebrew's Python 3.12 or 3.13 (`brew install python@3.12`). python.org's Python has no certificates
  and the system 3.9 can't load the ADB package; `optimize/setup.sh` picks a good one and sets up the certificates.
- **Windows:** clone into a short folder such as `C:\UrTV`; Git and Python come from winget
  (`winget install -e --id Git.Git --source winget`, `winget install -e --id Python.Python.3.12 --source winget`); run every script in Git Bash.
