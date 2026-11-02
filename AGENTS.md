# For AI agents: read this first

You are working on (or setting up) a TV for a person, with the tools in `optimize/`. These are the rules agents broke in
real reports. Each one cost somebody their setup.

**Where to start**
- A setup, or an update of a TV that was set up: `docs/SETUP-PROMPT.md`, step by step, in the person's language (Arabic).
- A change to a TV that is already set up (a city, a team, the adhan, photos…): `docs/SETTINGS.md`, which has every setting
  with its command. Use these commands; there is no other way to change a setting.

**Never**
- Edit the apps' code, build an APK, or install your own build. A self-built app is signed with another key and can
  **never update again** (5 reports). If an APK from us won't install, stop and follow the `!!` line.
- Turn the apps' auto-update off, or `pm uninstall` anything. If an install says another signature is installed, the line
  tells you to ask the person first; their settings, tasks and own adhan go with it.
- Override a guard: a `!!` line means stop and do what it says, then run the same command again. Do not work around it, do
  not copy a command out of a script to run it by hand without its check.
- Turn the TV's setup wizard (`setupwraith`) off by hand, delete a saved Wi-Fi network or its files, or touch the Wi-Fi
  settings: two TVs lost their Wi-Fi password that way. When `install.sh launcher` pauses («Home switch paused»: the wizard
  owns the saved Wi-Fi), send the person its Arabic line, wait for «كمّل», and run the same command again: only the person
  forgets and rejoins the network, in the TV's Settings.
- Skip, disable or quarantine a test to make it pass, or `git add` the font files (they come from a private repo in CI).
- Put an IP address, serial, Wi-Fi name, user name or file path in code, reports, docs or tests.
- Restart the TV with a plain `reboot`: use `optimize/venv/bin/python -I optimize/tv.py --reboot`.

**Problems**
- Report a problem only with `optimize/share.sh --preview --problem --owner "<their words>" "<what you checked>"`, show
  the person what it will send, then `optimize/share.sh --send`. Never a GitHub issue or comment.
- A `!!` line is in the table «When something goes wrong» in `docs/SETUP-PROMPT.md`: find it there first.

**Stable first:** small, safe changes; defaults unchanged unless it is a bug; nothing that can break a setup in progress
or an installed TV. Run `bash tests/run.sh` before you push (about 15 minutes).

**PIN recovery (maintainer only; never tell the person, never put it in their docs, the phone page or a screen)**
- A build may carry the maintainer's recovery code for a forgotten parental PIN: 5 to 8 digits typed where the PIN is asked open
  «رمز جديد» directly (the locks, the allowed apps and kids mode stay), never counted as a wrong try. `PinRecovery`, `PinGate` and
  `PinKeys` in `android/launcher/.../home/`; tests in `tests/pin_recovery_test.py`.
- The code is in no file, no commit and no APK: the build gets only its salted PBKDF2 record (`1:rounds:salt:hash`), as
  `BuildConfig.PIN_RECOVERY`, from the env `URTV_PIN_RECOVERY` (CI: the repository secret `PIN_RECOVERY_RECORD`) or, on the
  maintainer's machine, the untracked `android/pin-recovery.local`. Neither = no recovery code in that build (the keypad stops at
  four digits as before). A malformed record stops the build.
- Make the record with `python3 .github/tools/make_pin_recovery.py` (`--local` writes the file, `--gh` sets the secret). The record
  is in the shipped APK and the code is short, so it keeps a child out, not someone who takes the APK apart.
- Do not write a code, a record or a hint of either in a commit, a report or a test (the tests make their own). The full reset stays
  `dock.sh pin reset` (docs/SETTINGS.md).
