# Setting up a TV without a computer

Three ways, from the easiest. Pick by what the person has.

| | What they have | What runs the setup | What it does |
|---|---|---|---|
| **1. The TV alone** | the TV (and its remote), nothing else | the home app **on the TV**, over the TV's own local ADB | a careful subset: checks, grants, the safe list, the screensaver, the home switch; one-button undo |
| **2. An Android phone** | an Android phone on the same Wi-Fi | **Termux** on the phone: the very same `optimize/` scripts | everything a computer does |
| **3. An iPhone or iPad** | an iPhone/iPad on the same Wi-Fi | **iSH** (an Alpine shell app): the same scripts | everything a computer does, slowly; see «what we could not test» |

Path 1 needs no other device at all, so it is also the answer for an iPad that cannot run path 3.

## 1. The TV alone («إعداد تلفزيوني»)

1. **Debugging on.** Settings → About → press OK on **Build** seven times; then **Developer options** → **USB debugging** on. (Only the TV's own
   adb is used: nothing listens on the network.)
2. **Install the two apps with the TV's Downloader app** (the free one from the TV's store; allow it «install unknown apps» when it asks). In
   Downloader type these two codes, one after the other, and install each: **2927902** (the screensaver) and **9378345** (the home app). They are short links (aftv.news) to these two addresses, which also work typed in full:
   - `https://github.com/tshi-dev/UrTV/releases/download/apk-latest/urtv-screensaver.apk`
   - `https://github.com/tshi-dev/UrTV/releases/download/apk-latest/urtv-home.apk`

   (They are built and signed by GitHub Actions: this is the same pair `optimize/install.sh` installs. Never use an APK built anywhere else: it could not update later.)
3. **Open «إعداد تلفزيوني»** from the TV's apps row. The screen shows exactly what it will do, in order. Press **«ابدأ»**.
4. The TV asks **«السماح بتصحيح USB؟»** (Allow USB debugging?). Choose **«السماح دائماً»** (Always allow) with the remote, once.
5. Wait: a few minutes, with lines on the screen. When it says it is done, press Home on the remote.

**What it does**, with the very same lists and rules as `optimize/run.sh` from a computer (the lists are generated from `optimize/lists`):

- checks the device is an Android TV and that it answers; an empty or missing answer is never taken as «fine», and then **nothing is changed**;
- gives the home app the permissions it needs (location for the city, exact alarms for the adhan, «install unknown apps» for the TV's own update);
- turns off the **safe list** (`tcl-safe.txt`: telemetry, ads, promos): only what is installed and was not already off, never anything in `keep.txt` or Android's own core
  (`core.txt`); what it turned off is written to a **ledger** (`disabled-by-us.txt`, the same file name as on a computer);
- turns on the **screensaver** if it is installed and the TV's WebView is 80 or newer; otherwise it leaves the TV's own and says why;
- makes the home app **Home**, with the same guards as `install.sh`: it will not touch the setup wizard when that owns the saved Wi-Fi, it records who held Home
  before in a **journal** (`home-switch.json`, the same fields), and a **safety switch** (an alarm inside the app) puts the stock home back after 5 minutes unless the new home
  is confirmed on screen.

**The safety switch** is an alarm inside the app, set (and checked) **before** anything is changed: if the alarm cannot be set, the home switch does not start. Its deadline is also saved on disk. If the TV restarts or the app is
force-stopped in the middle, the TV's start (a boot receiver) and the setup screen set it again and try to put the stock home back; each try sets the next one first (after 30 s, then 1, 2, 4, 8 and 10 minutes), and the journal
says «restored» only once Home is **confirmed** to be the stock home. On Android 12 and newer without the exact-alarm permission the alarm can be minutes late, and the screen says so.

**With a family PIN set**, «ابدأ» and «تراجع» ask for it first. The screen is also kept out of the home screen's own apps grid.

**The ADB key.** While USB debugging is on, adbd listens on TCP 5555 on the whole Wi-Fi, not only on the TV: another device there can make the TV show the "Allow USB debugging?" prompt, but it cannot get in unless
someone accepts it. «السماح دائماً» makes this app's key a standing credential, so the app **deletes its key** when the setup (or the undo) has finished and no safety switch is pending; a later run asks again.
On the done screen the app says to turn **USB debugging off** (Developer options) and to use **«إلغاء تراخيص تصحيح USB»** (Revoke USB debugging authorizations) if the person does not need them. «تراجع» from the app needs debugging on again.

**«تراجع»** (Undo) is one button: it puts the stock home and the HOME role back first, then turns the ledger's apps on again, the screensaver as the TV had it, and the permissions as they were. It follows `optimize/restore.sh`.
If the app is closed in the middle of the home switch, opening «إعداد تلفزيوني» again says so, and «تراجع» finishes the job.

**What it leaves to a computer or a phone** (paths 2 and 3, or a computer): TV Bro and SmartTube, the speed tweaks, the community lists for other brands,
حماية العائلة (DNS), the measurements and the report. Nothing is lost by starting here: the computer path can follow later (`optimize/run.sh`).

| It says | Do |
|---|---|
| «تصحيح USB مطفأ» / «debugging is off» | turn USB debugging on (step 1) and press «ابدأ» again |
| the TV never shows the prompt | the TV may only offer **Wireless debugging** (some Android 11+ sets): use path 2 or 3 or a computer; they pair that way |
| «لم تُقبل رسالة …» | press «ابدأ» again and answer the prompt within three minutes. **If the prompt never appeared:** restart the TV, then open «إعداد تلفزيوني», press «ابدأ» and stay at the TV: it appears within seconds. (A prompt left unanswered, e.g. under the screensaver, makes the TV skip every later one until it restarts: «prompt currently pending, skipping» in adbd's log.) |
| a step is «تُركت» (skipped) | the line above says why (WebView too old: update «Android System WebView» in the Play Store, then «ابدأ» again) |
| the TV did not come back to the stock home | it will, after 5 minutes by itself; or press «تراجع» |

## 2. An Android phone: Termux

Install **Termux from F-Droid or from its GitHub releases** (the Play Store one is out of date). The phone must be on the **same Wi-Fi** as the TV (not mobile data, not a guest network).

```
pkg update
pkg install python git curl python-cryptography python-pillow
pkg install android-tools          # only if the TV pairs by «Wireless debugging» (some Android 11+ sets)
git clone https://github.com/tshi-dev/UrTV && cd UrTV
optimize/setup.sh                  # Termux is recognised: the venv uses Termux's own cryptography and Pillow
```

Then, with the TV's debugging on (path 1, step 1):

```
optimize/find-tv.sh use <the TV's address>      # Settings → Network on the TV shows it
optimize/run.sh
```

- Keep Termux **in front, with the screen on**, during the run: `termux-wake-lock` (part of Termux) stops Android putting it to sleep; Android 12+ may end a background Termux.
- `find-tv.sh` cannot read the Wi-Fi's neighbours on a phone (Android blocks it): that is why the address is given by hand. It still checks that the address answers.
- Everything else is the computer's guide: `docs/SETUP-PROMPT.md` (the agent steps), `docs/SETTINGS.md` (changes later). The ADB key stays in `~/.urtv` inside Termux.
- If `pip` tries to build `cryptography`, `pkg install python-cryptography` was skipped: install it, delete `optimize/venv`, run `optimize/setup.sh` again.

## 3. An iPhone or iPad: iSH

Install **iSH** from the App Store. iOS asks once for **Local Network** access: allow it (Settings → iSH → Local Network), or the TV cannot be reached. The iPhone/iPad must be on the **same Wi-Fi** as the TV.

```
apk add bash git curl python3 py3-pip py3-cryptography py3-pillow
apk add android-tools              # only for «Wireless debugging» pairing; may not exist for iSH, see below
git clone https://github.com/tshi-dev/UrTV && cd UrTV
bash optimize/setup.sh             # Alpine is recognised: the venv uses Alpine's own cryptography and Pillow
optimize/find-tv.sh use <the TV's address>
optimize/run.sh
```

- iSH **stops when it is not in front**: keep it open, the screen on (Settings → Display & Brightness → Auto-Lock: Never) until it finishes.
- iSH emulates an old 32-bit PC, so Python is slow: the setup takes longer than on a computer, and the first `optimize/setup.sh` can take several minutes.
- There is no mDNS and no `ip neigh` in iSH: the TV's address is always given by hand.

### What we could not test, and what to do

- **Tested here:** the scripts run in an **Alpine-like userland**: only BusyBox's applets (the same BusyBox 1.36 as Alpine 3.20) plus bash and Python on the `PATH`; a group of the shell tests passes that way
  (`python3 tests/phone_userland_test.py`), and the scripts contain nothing Alpine or Termux lacks (no `sudo`, no `/tmp`, no GNU-only options).
- **Not tested:** a real iSH (no registry or device here): the `apk add` names above come from Alpine's package index and were not run on iSH; `py3-cryptography` has to exist for 32-bit x86 in the Alpine
  that iSH uses (if `apk` cannot find it, `optimize/setup.sh` stops with a clear line, and the answer is path 1); `android-tools` (the `adb` program, needed only for Wireless-debugging pairing) may be missing for iSH.
- **If something does not work on iSH, use path 1:** it needs no other device at all, and the TV does the work itself.

### The TV test this path still needs (nothing here has run on a real TV)

1. A first run with «السماح دائماً»: the prompt appears once; all five steps; Home is ours; the journal/ledger files exist in the app's folder.
2. A second run: the TV asks again (the key was deleted after run 1: check that, and that the old authorization can be revoked), then «تلفزيونك هو الشاشة الرئيسية أصلاً» or a clean repeat.
3. **Deny** the prompt: «لم تُقبل رسالة …», nothing changed.
4. **USB debugging off**: «تصحيح USB مطفأ», nothing changed.
5. The safety switch: start the home switch and **restart the TV** in the middle; then again with **Force-stop** of the app in the middle. In both, the stock home must come back by itself, and the journal must say «restored».
6. «تراجع» after a good run: everything as before (stock home, apps, screensaver, permissions).
7. With a family PIN set: both buttons ask for it.

