# Setting up a TV without a computer

Three ways, from the easiest. Pick by what the person has.

| | What they have | What runs the setup | What it does |
|---|---|---|---|
| **1. The TV alone** | the TV (and its remote), nothing else | the home app **on the TV**, over the TV's own local ADB | the whole setup from one button, «جهّز كل شي»: everything `optimize/run.sh` does but the photos and sending the report; one-button undo |
| **2. An Android phone** | an Android phone on the same Wi-Fi | **Termux** on the phone: the very same `optimize/` scripts | everything a computer does |
| **3. An iPhone or iPad** | an iPhone/iPad on the same Wi-Fi | **iSH** (an Alpine shell app): the same scripts | everything a computer does, slowly; see «what we could not test» |

Path 1 needs no other device at all, so it is also the answer for an iPad that cannot run path 3.

## 1. The TV alone («إعداد تلفزيوني» ← «جهّز كل شي»)

1. **Debugging on.** Settings → Device Preferences → About → press OK on **Build** seven times; then **Developer options** → **USB debugging** on. (Only the TV's own
   adb is used: this app opens no port of its own.)
2. **Install ONE app with the TV's Downloader app** (the free one from the TV's store; allow it «install unknown apps» when it asks). In Downloader type
   **9378345** and install it: that is the home app, `urtv-home.apk`. The setup downloads the screensaver by itself. The code is a short link (aftv.news) to
   `https://github.com/tshi-dev/UrTV/releases/download/apk-latest/urtv-home.apk`, which also works typed in full.
   - Fallback only, if the TV cannot reach GitHub during the setup: **2927902** is the screensaver (`urtv-screensaver.apk`), installed by hand the same way.
   - Both are built and signed by GitHub Actions: the same pair `optimize/install.sh` installs. Never use an APK built anywhere else: it could not update later,
     and the setup refuses a screensaver that is not signed with the home app's own key.
3. **Open «إعداد تلفزيوني»** from the TV's apps row and press **«جهّز كل شي»**.
4. The TV asks **«السماح بتصحيح USB؟»** (Allow USB debugging?). Choose **«السماح دائماً»** (Always allow) with the remote, once. Stay at the TV: if the question
   does not appear within 20 seconds the screen says «ما ظهرت الرسالة؟ أعد تشغيل التلفزيون وافتح «إعداد تلفزيوني» واضغط «جهّز كل شي» وخلك عند التلفزيون».
5. **The plan.** Before anything is changed, the screen shows what it will do on this TV, with its real counts (for example «نوقف ٢٢ تطبيقاً من القائمة
   الآمنة»), and one box, ticked: «ثبّت أيضاً TV Bro (متصفح) و SmartTube (يوتيوب بلا إعلانات)». Untick it to skip them. Press **«ابدأ»** (or «إلغاء»: nothing changes;
   with no answer for ten minutes it ends by itself, nothing changed).
6. **Wait**, 10 to 20 minutes. One line per step: ✓ done, – تُرك (skipped, with why), ✕ not done (with why). The TV **restarts twice** by itself (the memory is
   measured before and after, like the computer path) and the run goes on by itself after each restart; open «إعداد تلفزيوني» again to watch it.
7. **Only on some TCL and Google TV sets** (the TV's setup wizard owns the saved Wi-Fi): before the home switch the screen asks to forget the Wi-Fi network in the
   TV's Settings ← الشبكة and join it again with the same password, then **«كمّل»**. «كمّل» without doing it is fine too: the switch then goes on with the wizard left on.
8. **Done.** The screen shows the report as a **QR code** (it opens `docs/share`, the same page as `optimize/share.sh`, with nothing personal in it: the person
   sends it from the phone if they want), how to turn debugging off, «لو صار شي», and «تراجع».

**What it does, in this order** (the same lists and rules as `optimize/run.sh` from a computer; the lists are generated from `optimize/lists`):

| step | what |
|---|---|
| precheck | the device is an Android TV and answers; an empty or missing answer is never «fine», and then **nothing is changed** |
| (plan) | reads what it would change (read only) and waits for «ابدأ» |
| backup | `backup.txt`, the TV as it came (`optimize/backup.sh`'s file); refused, and nothing changed, if the TV's package list came back empty |
| keep | protected features turned back on (`optimize/keep.sh`) |
| measure before | `optimize/measure.sh`'s reading after a restart; a TV whose adb would not come back on 5555 after a restart is read without one, and the report says so |
| grants | location for the city, exact alarms for the adhan, «install unknown apps» for the TV's own update |
| safe list | `tcl-safe.txt` (telemetry, ads, promos): only what is installed and on, never `keep.txt` or Android's core (`core.txt`); written to the ledger `disabled-by-us.txt` before the commands |
| community | the CleanYourTV list for exactly this model, with `optimize/community.sh`'s filters and UAD-ng's «Unsafe» rating. **Offline: nothing of it is applied** («تُرك») |
| tweaks | `optimize/tweaks.sh` with its journal: animations at half length, the Play Store kept from the background (its standby bucket on Android 11+ only); caches trimmed only when storage is low |
| screensaver | downloaded from `apk-latest`, installed only when its signer is this app's own (a fresh install) or the installed one's (an update), streamed to `cmd package install -S` (no file on the TV); then set if WebView is 80 or newer |
| extras | TV Bro and SmartTube from their own GitHub releases (the TV's processor's build), unless unticked; one that fails never holds the setup up |
| auto-start | on a TCL: AUTO_START / APP_AUTO_START and ASSOC_START allowed for both apps, then `appops write-settings` (`optimize/autostart.sh`); after the screensaver is installed, so it covers it too |
| home | Home becomes the home app, with `install.sh`'s guards (below) |
| measure after | the second reading |
| report | `report.md` in `share.py`'s form and allowlist; the done screen's QR code |

**The home switch** follows `optimize/install.sh launcher`. It records who held Home before in a **journal** (`home-switch.json`, the same fields, and a last `step` field
written before each step acts: armed, switch or wizard_off / wifi_check / home_start / stock_off, verify). A **safety switch** (an alarm inside the app) is set and checked
**before** anything is changed and puts the stock home back after 5 minutes unless the new home is confirmed on screen. When the setup wizard owns the saved Wi-Fi the
person is asked once (step 7). If the network then moved to the TV's Settings, the switch goes in the **safe order**: the wizard off first while the stock home is still
Home; the Wi-Fi read again (still connected, no saved network fewer; read only, no name kept); our home started and confirmed in front; only then the stock home off and
the HOME role ours. A changed Wi-Fi, or our home not coming up in front, puts the wizard and the stock home back (checked) and the stock home is never turned off.
If the network did not move, the wizard stays on (it would take the network with it).

**The safety switch's deadline** is also saved on disk. If the TV restarts or the app is force-stopped in the middle, the TV's start (a boot receiver) and the setup
screen set it again and try to put the stock home back; each try sets the next one first (after 30 s, then 1, 2, 4, 8 and 10 minutes), and the journal says «restored»
only once Home is **confirmed** to be the stock home. On Android 12 and newer without the exact-alarm permission the alarm can be minutes late, and the screen says so.
A run cut off in the middle says what is true: with a home switch pending, the stock home comes back by itself within 5 minutes; without one, Home was not touched.

**While a run goes** our screensaver does not start (it asks the home app and steps aside), and the home app holds the screen on: a USB-debugging prompt sent while
the screensaver showed stayed pending until the TV restarted (a tester's diagnosis), and a TCL ignored the screen's keep-on flag.

**With a family PIN set**, «جهّز كل شي», «كمّل» and «تراجع» ask for it first. The screen is also kept out of the home screen's own apps grid.

**The ADB key.** While USB debugging is on, adbd listens on TCP 5555 on the whole Wi-Fi, not only on the TV: another device there can make the TV show the "Allow USB
debugging?" prompt, but it cannot get in unless someone accepts it. «السماح دائماً» makes this app's key a standing credential, so the app **deletes its key** when the
run has ended and nothing waits (no safety switch pending, no restart of the measurement, no «كمّل» for the Wi-Fi); a later run asks again. The done screen says to turn
**USB debugging off** and to use **«إلغاء تراخيص تصحيح USB»** (Revoke USB debugging authorizations). «تراجع» from the app needs debugging on again.

**«تراجع»** (Undo) is one button, in `optimize/reset.sh`'s order: the stock home and the HOME role first, then the ledger's apps (and the community list's), the protected
features, the screensaver as the TV had it, the permissions, TCL's auto-start ops, the screensaver timeout, what the setup installed (the screensaver, TV Bro, SmartTube;
never this app), and the speed tweaks last. If the app is closed in the middle of the home switch, opening «إعداد تلفزيوني» again says so, and «تراجع» finishes the job.

**What it cannot do** (a computer or a phone, paths 2 and 3, can):

- **The person's own photos** for the screensaver (`optimize/photos.sh`): they are on a computer or phone, not on the TV.
- **Sending the report by itself**: it is shown as a QR code; the person sends it from their phone if they want (`optimize/share.sh` sends it from a computer).
- **TVs that only have «Wireless debugging»** (some Android 11+ sets, no «USB debugging» on port 5555): the app cannot pair with them. The first pairing needs a phone
  (path 2: `adb pair` in Termux, then `adb tcpip 5555`) or a computer; after that the TV-alone path works until the TV restarts.
- `CACHED_LIMIT` and حماية العائلة (DNS) stay on the computer path.

| It says | Do |
|---|---|
| «تصحيح USB مطفأ» / «debugging is off» | turn USB debugging on (step 1) and press «جهّز كل شي» again |
| «ما ظهرت الرسالة؟ …» (after 20 s) | restart the TV, open «إعداد تلفزيوني», press «جهّز كل شي» and stay at the TV: the prompt appears within seconds. (A prompt left unanswered, e.g. under the screensaver, makes the TV skip every later one until it restarts: «prompt currently pending, skipping» in adbd's log.) |
| the TV never shows the prompt, even after a restart | the TV may only offer **Wireless debugging**: see «What it cannot do» |
| «لم تُقبل رسالة …» | press «جهّز كل شي» again and answer the prompt within three minutes |
| a step is «تُرك» (skipped) | its line says why (no internet for the community list; WebView too old: update «Android System WebView» in the Play Store, then «جهّز كل شي» again) |
| «قبل تبديل الشاشة الرئيسية: شبكة الواي فاي …» | step 7; «كمّل» either way |
| the TV did not come back to the stock home | it will, after 5 minutes by itself; or press «تراجع» |
| the TV restarted and the screen is gone | the run goes on by itself; open «إعداد تلفزيوني» to watch it |

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

### The TV test this path still needs

See the work branch's report (work/phone-setup): a from-scratch run on a TCL after `optimize/reset.sh`, with only `urtv-home.apk` installed.
