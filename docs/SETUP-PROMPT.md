# Set up a TV with تلفزيونك: the prompt for your AI agent

**On the TV first:** Settings → System → About → press OK on **Build** 7 times → back → Developer options →
turn on **USB debugging** (and **Network debugging** if it is listed). Keep the TV on.

**No computer?** The person can set the TV up from the TV itself («إعداد تلفزيوني», a subset), or with these same scripts from an Android phone (Termux) or an iPhone/iPad (iSH): `docs/PHONE-SETUP.md`.

**Then** open an AI agent that runs commands **on a computer on the same Wi-Fi** as the TV (for example Claude Code
installed on it; a chat in the browser or a cloud session can't reach your TV), and paste everything below the line.
On Windows, everything it needs installs from PowerShell in three lines (then close PowerShell, open a new one,
type `claude` and log in):
```
winget install -e --id Git.Git --source winget
winget install -e --id Python.Python.3.12 --source winget
irm https://claude.ai/install.ps1 | iex
```
A deep folder breaks Windows paths («Filename too long»): clone into a short one such as `C:\UrTV` (below), and once run
`git config --global core.longpaths true`. If `winget` fails with a certificate error on its store source
(`0x8a15005e`, «server certificate had a validation error»), the lines above already use `--source winget`: use it on every
`winget` command.
Or in the Claude desktop app: the **Code** tab, with a **local** folder on this computer (not a cloud environment).

---

Set up my Android TV / Google TV with **تلفزيونك** from https://github.com/tshi-dev/UrTV: a faster TV
(bloat off, safely), an Arabic home screen, and the screensaver «شاشة تلفزيونك». Talk to me in Arabic (or my
language); keep commands in English.

## Start here: three short messages, word for word, before you touch the TV

Everything that needs me comes first; after that you work alone. Your first three messages are fixed: send them in
this order, each on its own, word for word, and wait for my answer after each. Never skip one (not even the first:
you can't see my approval settings, so never assume they are already right), never merge them, never assume my
answers, and never skip or work around anything that asks me (an approval on this computer, «Allow USB
debugging?» on the TV). Cloning the repo and reading this file before message 1 is fine; nothing else.

**Message 1, approvals.** If you are Codex:

> قبل نبدأ: من القائمة اللي تحت خانة الكتابة، اختر «Approve for me» للموافقات، عشان ما أوقف أسألك عن كل أمر.
> وقلّي «تم».

Any other agent: the same in your words: ask me to allow running commands and network access in this folder for
the whole task, and to say «تم».

**Message 2, the city:**

> وش مدينتك؟
> (لمواقيت الصلاة والطقس. مثال: الرياض، جدة، الرس)

**Message 3, the clubs and the defaults:**

> وتتابع مباريات أي أندية؟ اكتب أسماءها، أو «لا شي».
>
> والباقي بخليه على الافتراضي، وتقدر تطلب تغيير أي شي منه الحين أو بعدين:
> • جنب الساعة: «آية وحديث اليوم» (أو: مواقيت الصلاة، المسلسلات، الطقس، المباريات، لا شي)
> • المفضلة: أول ٦ تطبيقات من واجهتك الحالية، بنفس الترتيب
> • بركّب TV Bro (متصفح للتلفزيون) و SmartTube (يوتيوب بدون إعلانات)، مجانيين من GitHub
> • ما فيه بطاقة في الواجهة الرئيسية، ووقت شاشة التوقف يبقى مثل ما هو، وما أعطّل إضافات TCL الاختيارية

After my answer to message 3, start steps 0 and 1 at once, in the same turn: don't end your turn to wait for me
again. The only other thing I do is press Allow on the TV, and step 1 tells me when, without stopping.

Those defaults, unless I change them:
- the card beside the clock: verse and hadith;
- the favourites row: the first 6 apps of my current home screen (you note them in step 1), in that order; an icon
  you couldn't name is left out;
- TV Bro and SmartTube installed (`optimize/run.sh` does it; if I don't want them: `SKIP_EXTRAS=1 optimize/run.sh`);
- no card on the home screen; the TV's own screensaver delay is kept;
- nothing from `optimize/lists/tcl-optional.txt` is disabled (`optimize/lists/mine.txt` stays empty);
- if I pick the episodes card: all genres and languages.

My city is checked later against `android/app/src/main/java/sa/urtv/screensaver/Cities.java`: Saudi Arabia and the main
cities of Bahrain, Kuwait, Qatar, the UAE, Oman, Egypt, Jordan, Syria, Lebanon, Palestine, Morocco, Algeria and Tunisia are
in it, each with its country's time zone and its prayer calculation, so a city by name (`city الدوحة`, `city القاهرة`) just works. If it isn't there, step 4 sets it with its coordinates
and time zone (`city <name> <lat> <lon> <zone>`) without asking me again; the zone of a place in those countries picks
that country's calculation (`Asia/Kuwait`, `Asia/Qatar`, `Asia/Dubai`, `Asia/Bahrain`, `Asia/Muscat`, `Africa/Cairo`, `Asia/Amman`,
`Asia/Damascus`, `Asia/Beirut`, `Asia/Hebron` or `Asia/Gaza`, `Africa/Casablanca`, `Africa/Algiers`, `Africa/Tunis`), so give the right one. If I ask for the Shia
Ja'fari times: `optimize/saver.sh prayermethod jafari`. Oman gets its Ministry of Awqaf's method by itself; `optimize/saver.sh prayermethod gulf` gives the older Gulf method.

## 0. Can you reach my TV? (check this first, 1 minute)

I am on a computer on the same Wi-Fi as the TV, and your commands most likely run on it. A clone or download that
fails at first, or a network error before the check below, is your own sandbox not allowed yet (see «Permissions
first»): ask, then try again. Never decide you can't reach the TV from that; only the check below decides.

ADB needs a direct connection, from the terminal where your commands run, to the TV on my Wi-Fi. Many agents run
their commands somewhere else: a cloud container, a sandbox, or a Linux virtual machine on this computer (a
"connected folder" or `device_bash`-style tool works that way). From there the TV can never be reached, however
often you retry, so find out before anything else.

**Permissions first.** Some agents run commands in a sandbox that blocks the network or starting programs until I
allow it (Codex on Windows does). The signs are errors like `getaddrinfo() thread failed to start`,
`Bash/E_ACCESSDENIED` or `Operation not permitted`. They mean "not allowed yet", not "can't reach the TV":
message 1 is how I allow them (network access and running git, bash and python in this folder, for the whole
task); if one still appears, send me message 1 again, then go on. On Windows, run every script in Git Bash (they
find a working Python themselves, past the Microsoft Store shortcut); if `bash` isn't found, use its full path:
`"C:\Program Files\Git\bin\bash.exe" -lc "cd <this folder> && optimize/find-tv.sh --check"`. Run the commands
here exactly as written and add no quotes, `;` or `|` of your own: PowerShell runs those itself before Git Bash
sees them (that is why every check has its own script).

**Codex: the network from the first try.** Its sandbox blocks the internet and the home network, so a command that
needs them fails there first and wastes minutes (a `git clone` that sits with no progress, then a retry). Run
`git clone`, `git pull` and every Git Bash command **outside the sandbox from the first try**: request escalated
permissions straight away, with the justification below. If a request gets no answer within a minute, or is
refused twice, don't wait or look for another way: tell me in one line which command needs approval and why, and
ask me to approve it myself (or to allow it for the rest of the task).

**Codex on Windows:** its sandbox can never start Git Bash (`0xC0000022`, `NtCreateDirectoryObject`: a known Codex
bug, openai/codex#54449). So run every Git Bash command **outside the sandbox**: request escalated permissions for
it, with this justification: "Git Bash cannot start inside the Codex Windows sandbox (openai/codex#54449); this runs
the UrTV setup script the user asked for, in this folder, to reach their TV on their Wi-Fi." With «Approve for me»
the reviewer decides; when it offers to allow `bash.exe` from now on, take it. I don't need Full access for this.
A refusal that never reached a reviewer (`you cannot ask for escalated permissions`, `rejected by policy`,
`sandbox_approval is false`) means approvals are not on «Approve for me»: Codex then rejects every escalation by
itself (openai/codex#32563). Send me message 1 again, wait for «تم», then run the same command. If a reviewer turns
it down, ask once more with the same justification and the exact command; if it is turned down again, stop and tell
me exactly what was refused. Don't port the scripts to PowerShell or look for another way: they need Git Bash.

**Missing Git or Python is yours to fix, not mine.** If Git Bash isn't there, or a script says
`!! No working Python 3 here`, ask me once, then install it yourself and carry on:
`winget install -e --id Git.Git --source winget --accept-source-agreements --accept-package-agreements` or
`winget install -e --id Python.Python.3.12 --source winget --accept-source-agreements --accept-package-agreements`. Then run the
same command again: the scripts find the new Python without a new terminal. Don't stop and hand me the install.
On a Mac with only the system Python 3.9 and no Homebrew, `uv python install 3.12` needs no admin password (get uv
with `curl -LsSf https://astral.sh/uv/install.sh | sh`); the scripts find uv's Python by themselves.
```
git config --global core.longpaths true   # Windows only, once (a deep folder breaks git: «Filename too long»)
git clone --depth 1 https://github.com/tshi-dev/UrTV     # only the latest version: much faster. On Windows into a short folder: git clone --depth 1 https://github.com/tshi-dev/UrTV C:/UrTV (a deep one breaks Python and git: «Filename too long»)
cd UrTV                                # on Windows: cd C:/UrTV
git pull                               # always the latest (a command of its own, not joined with the others)
optimize/find-tv.sh --check            # Windows: always through Git Bash (below), never plain `python`
```
- `OK: this terminal can reach the home network` → go on with step 1.
- `!! This terminal cannot reach the home network` → **stop**. Tell me, in plain words, that you can't reach the TV
  from where your commands run, and that I need an agent running on this computer itself (for example Claude Code
  installed on it, or the Claude desktop app's Code tab on a local folder; on Windows first, in PowerShell:
  `winget install -e --id Git.Git --source winget`, `winget install -e --id Python.Python.3.12 --source winget`, `irm https://claude.ai/install.ps1 | iex`,
  then a new PowerShell window and `claude`). A chat that only links to my computer's folders or screen does not
  run commands on it, so it does not count. Give me the one line to paste there:
  «Set up my TV with https://github.com/tshi-dev/UrTV: clone it and follow `docs/SETUP-PROMPT.md` step by step. You are on a computer on the same Wi-Fi as the TV.»
  Don't look for workarounds.

**Android 11 or newer, and port 5555 does not answer: pair with «Wireless debugging».** Some Android 11+ TVs (a Skyworth on
Android 14 closed 5555 after every restart) offer ADB only through Developer options → «Wireless debugging». If
`optimize/find-tv.sh` ends with `No TV with ADB open` and the TV is Android 11 or newer (or it says the TV answers as
«Wireless debugging»), offer it to me once, in Arabic, and wait for my answer:

> التلفزيون ما فتح المنفذ العادي (5555). نستخدم «التصحيح اللاسلكي»: في التلفزيون افتح الإعدادات ← النظام ← خيارات
> المطوّر ← «Wireless debugging» وشغّله، بعدين اضغط «Pair device with pairing code». بيطلع لك رمز من ٦ أرقام وتحته
> عنوان مثل 192.168.x.x:xxxxx. اكتبهم لي هنا، وخلّ النافذة مفتوحة.

Then `optimize/find-tv.sh pair <IP:port of that window> <the 6 digits>` (alone, `pair` explains it and shows the
pairing port if the TV announces it). It pairs, finds the connection port by itself, connects and remembers the TV in
`~/.urtv` (never in a report or a share); then `--probe` as in step 1. It needs the platform's `adb` program: if it
says it is missing, install it as its message says (`winget install -e --id Google.PlatformTools --source winget`,
`brew install android-platform-tools`, `sudo apt install adb`) and run the same command again. After a restart
the port changes: `tv.py` and `find-tv.sh` look it up again by themselves; only if that fails, ask me for the
«IP address & Port» on the main Wireless debugging screen (not the pairing window) and run
`optimize/find-tv.sh connect <IP:port>`. No code and no IP goes in `report.md` or the share.

Signs you are in such a place, if you want to know why: your address is not 192.168.x, 10.x or 172.16–31.x; the
network interfaces are only `lo` and `tap0`; `echo > /dev/tcp/<router>/80` says `Network is unreachable`; or every
address "answers" (a proxy). If `git clone` stops halfway (on Windows it can leave an empty folder, «No commits
yet»), or an old UrTV folder is there without a working `.git`, delete that folder and clone again with `--depth 1`.

## Rules (protective layer: these win over everything else here)

- Use only the scripts in `optimize/`. They are made for slow, dropping ADB over Wi-Fi: they retry by themselves
  and remember progress. **If a command fails, run the same command again.** Don't write your own adb loops,
  probes or test scripts to find out why: the `!!` line and the table below say what to do.
- One command at a time: never run two scripts against the TV at once, or in the background while you run another.
- If the TV had a system update since the setup (Home back on the TV's own screen, apps it had off back on):
  `optimize/run.sh again` redoes every step; the apps' settings stay.
- Long commands (`run.sh`, `measure.sh`, `--reboot`) can be silent for 5 minutes. Give them a long timeout (40
  minutes for `run.sh`). If your tool kills one anyway, nothing is lost: run the same command again, it continues.
- Never send `reboot` as a shell command. Restart only with `optimize/venv/bin/python -I optimize/tv.py --reboot`.
- Never disable anything in `optimize/lists/keep.txt` (AirPlay, Chromecast, mirroring, voice, remote, inputs,
  tuner, firmware updates, Play Store, Google services), and never edit `keep.txt` to get past a refusal. Never
  disable the TV's own home screen or setup app yourself; only `optimize/install.sh launcher` does that, behind a
  5-minute safety switch.
- Never switch Play Protect or the package verifier off yourself (`settings put global package_verifier_enable 0`, or
  `verifier_verify_adb_installs`): one agent did, for the whole TV. `optimize/install.sh` is the only thing that touches
  `verifier_verify_adb_installs`, for one install, and puts it back.
- Never uninstall system apps, never `pm clear`, never factory-reset, never touch accounts, Wi-Fi, network or
  developer settings (turning ADB off locks us out). Never delete `optimize/run/` or `optimize/backup-*.txt`: they
  are how we undo. Everything is reversible: `optimize/reset.sh` puts the TV back as it came.
- Anything I ask for about the screensaver, the home screen or the TV (now or any time later): look it up in
  `docs/SETTINGS.md` and use its command. Never edit the apps' code, build or install your own APK, or turn the
  automatic update off to make something; if it isn't in `docs/SETTINGS.md`, tell me it isn't in تلفزيونك yet and
  pass my words on in the share message (step 8).
- **A problem goes to the developer's form, never to GitHub.** When something doesn't work, or I say «بلّغ المطوّر
  عن مشكلة» (now or any time later, from this computer or another one): look into it first with this prompt's tools,
  then `optimize/share.sh --preview --problem --agent "<you and your model>" --owner "<my words>" "<what you checked
  and found, two to five plain lines>"`, show me the whole text, ask «أرسله؟», and only if I say yes
  `optimize/share.sh --send`. Never open a GitHub issue, discussion, comment or pull request for me, never use `gh`
  or the GitHub website on my behalf: the developer reads the form, and a GitHub post is public and can carry my
  details. If the TV doesn't answer, send it anyway: the report says so.
- Ask me before anything not in this prompt. If something fails twice, and the table's fix didn't help, stop,
  show me the output and propose a fix.
- Never tell me something worked without its proof: the script's own success line, or a screenshot you looked at.

## When something goes wrong (expected, and what to do)

Lines starting with `!!` are the scripts telling you to stop and act. These are the ones we have seen:

| You see | It means | Do |
|---|---|---|
| `getaddrinfo() thread failed to start`, `Bash/E_ACCESSDENIED`, `Operation not permitted` | your sandbox hasn't been allowed network or programs yet | ask me to allow them for this folder (see step 0), then the same command |
| `0xC0000022` or `NtCreateDirectoryObject` starting Git Bash (Codex) | Codex's sandbox can't start Git Bash | run it outside the sandbox (step 0, «Codex on Windows») |
| `you cannot ask for escalated permissions`, `rejected by policy` (Codex) | Codex's approvals are not on «Approve for me» | message 1 again, wait for «تم», then the same command |
| `ModuleNotFoundError: No module named 'adb_shell'`, or `!! The Python packages aren't in optimize/venv` | `optimize/setup.sh` didn't finish (often pip had no network) | `optimize/setup.sh` again until it ends with «✓ Python ADB client»; its `!!` line says what is wrong. Then the same command; don't stop for this |
| `!! This folder's path is too long for Windows`, `Filename too long`, or `DLL load failed … too long` | the UrTV folder sits too deep for Windows | clone it again into a short folder: `git clone -c core.longpaths=true https://github.com/tshi-dev/UrTV C:/UrTV`, and carry on there (or once, `git config --global core.longpaths true`, then clone again; a short folder like `C:/UrTV` is still best) |
| `!! No working Python 3 here`, or `python` opens the Microsoft Store | Python not installed, or only Windows' Store shortcut | ask me once, install it yourself (`winget install -e --id Python.Python.3.12 --source winget --accept-source-agreements --accept-package-agreements`), then the same command again in Git Bash |
| `INSTALL_FAILED_VERIFICATION_FAILURE: Install not allowed` (seen on Android 12–14 for the home app, which targets an older SDK than Play Protect likes) | Play Protect checks apps installed over ADB and refused it | tell me, in Arabic, to watch the TV and press «تثبيت على أي حال» (Install anyway) if Play Protect shows a message (`install.sh` prints these words). `install.sh` switches `verifier_verify_adb_installs` to 0 for that install only, retries once and puts the old value back (it says so in one line); if it still fails, the line shows the TV's own message and the same words. **Never** turn the verifier off for the whole TV |
| `Could not download …` on Windows with `CRYPT_E_NO_REVOCATION_CHECK` / `revocation` | Windows' curl (Schannel) can't reach the certificate revocation server | nothing: `install.sh` retries 3 times and, on that error, once more with `--ssl-revoke-best-effort`; if it keeps failing, check the internet/proxy and run the same command again |
| `CERTIFICATE_VERIFY_FAILED` (macOS) | python.org's Python has no CA certificates | `optimize/setup.sh` again: it installs certifi into `optimize/venv` and every script uses it; if the line stays, delete `optimize/venv` first |
| `symbol _ERR_get_error_all`, or a cryptography `DLL`/`dlopen` error (macOS) | an old system Python (3.9) was used, and the cryptography wheel won't load on it | `brew install python@3.12`, or with no admin password and no Homebrew `uv python install 3.12` (install uv first: `curl -LsSf https://astral.sh/uv/install.sh \| sh`), delete `optimize/venv`, then `optimize/setup.sh`: uv's Python is found by itself |
| `!! Python 3.14 is too new` | no wheels for it yet, so pip would build them | `brew install python@3.12` (Windows: the `winget` line above), delete `optimize/venv`, then `optimize/setup.sh` |
| `(connection problem: …; retrying in 5s)` | Wi-Fi ADB dropped for a moment | nothing, it retries by itself |
| `resuming upload at 40%` | an earlier upload was cut off | nothing, it continues and checks the file |
| no output for several minutes | a restart, or the 3-minute settle before measuring | wait; don't interrupt |
| the first connection hangs for up to 2 minutes | the TV is showing «Allow USB debugging?» | ask me to press Allow and tick «Always allow» |
| `--probe` silent for 5 minutes and the TV shows no «Allow USB debugging?» | the first connection to a TCL sometimes just hangs | stop it and run the same command again: the second time it connects at once |
| `--probe` times out and the TV shows no «Allow USB debugging?» after a new clone of the repo | the ADB key the TV trusted stayed in the older folder's `optimize/adbkey`; a new clone makes a new key and the TV never saw it | the key now lives outside any clone, in `~/.urtv` (Windows: `%USERPROFILE%\.urtv`), and `tv.py` moves an older `optimize/adbkey` there by itself. If the older folder is not the one you are in, copy its `optimize/adbkey/adbkey` and `adbkey.pub` into `~/.urtv`, then the same command. Never print or paste that path: it holds my user name. The saved TV (`tv-id.json`) is there too |
| `reset.sh`: `mv: cannot move 'optimize/run' … Permission denied` (Windows) | something holds a file open inside `optimize/run` (a log you write there, an editor, an image viewer) | close it, never write your own logs into `optimize/run`; the same command again (the apps are already removed, it carries on) |
| `!! The import failed (…). The TV was put back as it was` (`saver.sh import --yes`) | the TV refused one of the values, or did not show it when read back; everything was put back | nothing changed: read the reason in the line, fix the file (or update the apps: `optimize/install.sh`), then the same command |
| `!! The import failed (…) and the TV could NOT be put back completely` | the TV refused the put-back too (it went off, or its apps are older) | `optimize/saver.sh import optimize/run/settings-before-import.json --yes` (the path is in the line), and if that fails too, `optimize/saver.sh show` and set the rest with the commands in `docs/SETTINGS.md` |
| `!! This terminal cannot reach the home network` | your commands run in a cloud, sandbox or virtual machine | stop, tell me (step 0) |
| the TV forgot its Wi-Fi after the first restart (two TCL sets did) | some firmware drops its saved network on a restart | **before the first restart** (step 3 starts with one) ask me to have the Wi-Fi password at hand. If the TV comes back with no network: tell me to open Settings → Network & Internet, pick the network and type the password, and say «تم»; then `optimize/find-tv.sh` (the address may change) and the same command. Never touch the Wi-Fi settings yourself |
| a «choose a home app» list, or a «who is watching» profile picker, shows on the TV during the home switch (a Sony with two profiles, a Nokia) | the TV asks which home to use, or which profile to open | look at the screen (`optimize/shot.sh home`) and ask me to press OK on «تلفزيونك» then «دائماً» (Always) in the list; on a profile picker my usual profile, then Home. Then the same command if it stopped |
| `Home switch skipped: this TV refuses to turn its own home screen off` (`This command is illegal`, a Nokia Streaming Box 8000) | that TV's firmware does not let `pm disable-user` touch its launcher | nothing is broken: the stock home is back and the rest of the setup goes on. Say so to me in Arabic (the TV keeps its own home screen; the screensaver works); `report.md` has the line «Home switch: skipped»; don't try `SKIP_LAUNCHER` or other ways. **In your final message** tell me where things are now: the screensaver settings open from the app «تلفزيونك» in the TV's own apps row (it has a launcher entry); the «تلاوة وأذان» screens (Quran, radio) are reached only from our home's pill, so they are not reachable while the TV keeps its own home (our home app has no launcher entry on purpose) |
| `Don't know your TV's IP` | `find-tv.sh` was not run or found nothing | `optimize/find-tv.sh`, or ask me for the IP (TV: Settings → Network → the Wi-Fi) and save it: `echo <ip> > optimize/.tv_host` |
| `No TV with ADB open` | debugging off, TV asleep, or the computer is on another (guest) Wi-Fi | ask me to check all three, then `optimize/find-tv.sh` again (it scans the whole network, the router's known devices and mDNS, and knocks twice, slowly the second time); on Android 11+ with 5555 still closed: pair (step 0, «Wireless debugging») |
| `No TV with ADB open on port 5555` and «Wireless debugging» in the next line, or `ADB open` never appears after a restart of a TV that worked before | Android 11+ TVs may close 5555 after every restart (seen on a Skyworth, Android 14) | `optimize/find-tv.sh pair` (step 0); a TV that was paired once is reconnected by `optimize/find-tv.sh` itself |
| `!! Wireless debugging needs the platform's own adb program` | pairing and the Wireless-debugging connection go through `adb`, which isn't installed (the Python client has no pairing) | install it as the line says (`winget install -e --id Google.PlatformTools --source winget`, `brew install android-platform-tools`, `sudo apt install adb`), a new Git Bash window on Windows, then the same command |
| `!! Pairing failed: the code is wrong or it has expired` / `the pairing dialog must be open` | the 6 digits changed (they do whenever the dialog is closed and opened), or the IP:port is not the one in the dialog | ask me to read the dialog again, then `optimize/find-tv.sh pair <IP:port> <code>` |
| `the TV's Wireless-debugging port does not answer and was not found on the Wi-Fi` | the port changed after a restart and the router doesn't pass mDNS (multicast) | ask me for «IP address & Port» on the main Wireless debugging screen, then `optimize/find-tv.sh connect <IP:port>` |
| `!! This is not the TV the setup started on` | two devices answer on this Wi-Fi (a box and the TV), or the address now belongs to another one; the script stopped **before changing anything** | ask me which one is my TV (Settings → Network on the TV shows its address); `optimize/find-tv.sh use <IP>`, then the same command. The same TV on a new address is fine. If the line says the TV «did not show its serial this time», the saved TV had a serial or Android id that this answer lacks (the property read failed): the tools never fall back to the model then; try again in a minute, and only if it is certainly the same TV, `--new-tv`. Only if I say it really is a new TV: `optimize/venv/bin/python -I optimize/tv.py --new-tv`, then `optimize/run.sh` (it starts from the first step); or work from a fresh clone of the repo |
| `!! TV not answering after 6 tries` (10 for an upload or install) | TV went to standby, lost Wi-Fi (weak signal: seen on a TCL G10, Android 12), or got a new IP. The tries wait 5, 10, 15, 20, 30 s | ask me to wake the TV, and if the signal is weak to move the router or the TV closer or use a cable; run `optimize/find-tv.sh` if the IP may have changed; then the same command (an upload resumes where it stopped; so do the extras: TV Bro and SmartTube already on the TV are not sent again) |
| `!! The TV did not come back within 10 minutes` | slow restart, or it stayed in standby | ask me to turn it on, then `optimize/venv/bin/python -I optimize/tv.py --wait`, then the same command |
| `!! upload keeps failing` or `md5 mismatch` | bad Wi-Fi, or the TV's storage is full (the line says how much space is left) | the same command once; if again, show me that line |
| `!! … is not a folder on the TV`, or a TV path like `C:/Program Files/Git/data/…` | an old copy: Git Bash rewrote the TV's path before Python saw it (now undone by `tv.py`) | `git pull`, then the same command |
| `UnicodeEncodeError` or `charmap` (Windows) | an old copy: Arabic text in Windows' own code page | `git pull`, then the same command |
| `Refusing: these are in …keep.txt` | a protected package got into `mine.txt` | take that line out of `mine.txt`, run again |
| `!! … has a package rated Unsafe` | `mine.txt` has a package the TV may need | take it out of `mine.txt`, run again |
| `(unknown model: no optional list. …)` from the community step | our lists are TCL's and no community list fits this brand and model (or the device code is shared by several sizes: a guess is never applied) | nothing: the setup goes on without an optional list. Say in step 7 that nothing was disabled beyond the common Google extras |
| `Unknown package` or `not installed` while disabling | that package is not on this model | nothing, normal |
| `!! … is already installed, signed with another key` (`install.sh`) | an app with our name but another signature is on the TV (a copy built by hand, or from another source); ours can't replace it, and removing it deletes its settings, tasks and own adhan (Android can't keep them across signatures). Nothing was removed | **stop and ask me**, in Arabic (the line has the words): remove it and install the official one (it then updates itself)? Only after my yes: `pm uninstall <package>`, then the same command. If I say no, leave it and go on without that app. Never build or install your own APK |
| `!! Install of … failed` | usually storage, or a very old Android | stop and show me |
| `!! The TV stopped answering while … was installing` (install.sh), or `!! … was skipped: the TV stopped answering while it installed` (SmartTube, TV Bro) | some sets take minutes to install a big APK and drop ADB meanwhile; the install may even have finished | for an extra: nothing, the setup goes on (add it later with `optimize/install.sh extras`; one that finished is found). For the screensaver or the home: wake the TV, wait a minute, then the same command (a finished install is not repeated) |
| `!! Install of urtv-screensaver.apk failed: cmd: Can't find service: package` | the TV's package service is not up yet (just after a restart, or the TV is still booting) | wait a minute and run the same command again; if it persists, `optimize/venv/bin/python -I optimize/tv.py --reboot`, then the same command |
| `!! The TV did not come back within 10 minutes` (install.sh, during the home switch) | ADB dropped during the switch and the TV did not answer for 10 minutes (off, standby, or the Wi-Fi went) | ask me to check the TV is on and on Wi-Fi (`optimize/find-tv.sh` if the address may have changed); the safety switch restores the stock home by itself; then the same command |
| `!! The screensaver is installed but this run did NOT switch it on…: the TV did not say which «Android System WebView» it has` | the WebView version could not be read (an unknown version is treated like an old one: the page would not run on WebView 66) | nothing is broken, the TV keeps its own screensaver; run `optimize/install.sh saver` again; if it says this again, `optimize/precheck.sh webview-store` opens the WebView page for the person to update |
| `Another address than the last run (no serial to tell if it is the same TV)…` (run.sh) | the TV has no usable serial and its address changed (DHCP) | nothing to do: the steps run again, the backup and journals are kept; if it was really another TV, `optimize/reset.sh` first on the old one
| `!! saver.sh: no answer to the update (it may still be downloading)` | `saver.sh update` got no answer while the download runs | wait a minute, `optimize/saver.sh show` shows the version; run it again if it did not change |
| `!! saver.sh off: the TV did not list its screensavers…` | the TV answered nothing to the list of screensavers, so it is not known whether it has one of its own (switching all screensavers off would be a guess) | nothing was changed; the same command again |
| `!! Could not tell which app is the TV's home now, so the home was NOT switched…` | no home-role holder and no answer to «what answers Home»: undoing a switch could not put the right app back | nothing was changed (no safety switch was armed); the same command again once the TV answers |
| `!! The TV did not say what Home is now; the safety switch is already off…` | the TV answered nothing after the home switch was confirmed | press Home on the remote; if it is not the new launcher run `optimize/restore-home.sh`; the journal still says «switching», so the next `install.sh launcher` looks again |
| `!! Could not ask the TV which packages are off, so «<pkg> on again» was not confirmed` / `!! still off, and there is no optimize/run/backup.txt…` (keep.sh) | the list of disabled packages could not be read, or there is no backup to show whether the TV itself had the package off before | nothing was remembered as kept; the same command again; for the second one tell the person, do not call it «the TV's own» |
| `!! Auto-start (TCL): the TV did not answer…` | `appops get` said nothing for our apps (not «no such op») | `optimize/autostart.sh` again; the report shows `Auto-start (TCL): … allow` when it is known |
| `- ?? <package>: the TV did not answer` (report.md, «Features that must stay on») | the question about a protected package got no (or a cut) answer | not a finding about the package: `optimize/keep.sh` checks them again |
| `!! dock.sh: no answer from the TV's home app` / `!! saver.sh: no answer from the TV's screensaver app` / `!! photos.sh: …` | the app's receiver never answered (app missing, another signature, just updated, or ADB dropped), so nothing is known to have changed (a PIN, a setting) | never say «done»; `optimize/install.sh launcher` (or `saver`), then the same command again |
| `!! The screensaver is installed but NOT switched on` | the TV's WebView is older than 80, so the page would not run; the TV keeps its own screensaver | tell me in Arabic to update «Android System WebView» (`optimize/precheck.sh webview-store`), then `optimize/install.sh saver` again. Don't select the screensaver by hand |
| `!! Could not read which TV this is` (tv.py) | the TV gave no serial, make or model (still waking, or a busy shell); nothing was changed | wait a minute, then the same command; never skip the check |
| `!! The TV stopped answering while it ran this command … NOT sent again` | a command that must not run twice (a task added or deleted, a photo deleted by number, a key press) lost its reply; it may or may not have happened | look first (`optimize/saver.sh tasks list`, `optimize/photos.sh list`, or a screenshot), run it again only if it did not happen |
| `!! Could not download …` (install.sh) | no such build at that tag, or no internet on this computer | check `APK_TAG` (unset = the latest) and the releases page https://github.com/tshi-dev/UrTV/releases, and that the computer is online; then the same command |
| `!! GitHub rate limit, try later` (install.sh extras) | GitHub allows only a few anonymous requests an hour | wait a few minutes, then `optimize/run.sh` again (it redoes the extras step) |
| `Done, with problems:` and `optimize/run.sh` exits 1 | every step ran, but `report.md` has `!!` lines (printed under it) | read them, fix what each says (this table), then `optimize/run.sh` again: only the report step is redone |
| `ADB dropped during the switch; the TV is back, trying the switch again` | Wi-Fi ADB dropped while the home was being switched; the script waited for the TV (up to 10 minutes) and is doing the switch a second time, the safety switch armed again | nothing, wait; only if it ends in `!!` follow that line |
| `!! The safety switch put the stock home back` | ADB was slower than the 5-minute switch | nothing is broken: the same command again |
| `This TV ends background jobs when the ADB session closes, so there is no safety switch on the TV itself…` (install.sh, during the home switch) | seen on a TCL with Android 14: a job started over ADB (nohup or setsid) is ended the moment the ADB session closes, so the 5-minute safety switch cannot stay alive on the TV. `install.sh` checks for its heartbeat, says this in a plain line (not a `!!`) and goes on: the switch is checked and undone from this computer at once if Home does not open ours, and the journal (`optimize/run/home-switch.json`, which records `safety_switch: unavailable`) restores the stock home on the next run if this computer stopped in the middle | nothing to do; the report says «Home switch: no safety switch on the TV itself». Keep the computer and the TV on until `Launcher is home`. If the run is cut off, run `optimize/install.sh launcher` or `optimize/restore-home.sh` again. Do not start `deadman.sh` yourself. When Home is already ours, `install.sh` prints «Home is already the new launcher: nothing to switch» and starts no safety switch at all |
| `An earlier home switch was cut off (the TV or this computer restarted)… putting the home back from optimize/run/home-switch.json` | `install.sh` and `run.sh` write a journal of the switch (`optimize/run/home-switch.json`: the stock home, the setup wizard and who held Home before) and read it first on the next run. A restart kills the safety switch on the TV, so with the journal still «switching», Home not ours and no marker on the TV, they put the stock home back from it. Ours being home, or the switch having confirmed itself, is left alone | nothing: wait, the switch is done again if it is a step of this run. If it then says `!! Could not restore the home from the journal`: the TV did not answer; when it is on and on Wi-Fi run `optimize/restore-home.sh` |
| «استرجاع الشاشة الرئيسية» (the person wants the stock home back, or Home is wrong after a switch) | the home app has no button for it (with no home there is nothing to press): it is a command and it needs ADB | `optimize/restore-home.sh`: puts the stock home, the setup wizard and the previous HOME holder back from the journal (and the stock home's helpers); with no journal it does what `optimize/restore.sh` does. The TV must be on and on Wi-Fi. It only ever turns the wizard on, never off |
| `!! window_animation_scale: this TV did not take the value (asked for 0.5, it says …)` (or `transition_animation_scale`, `animator_duration_scale`, `cached_apps_freezer`, `max_cached_processes`, and the Play Store's `standby_bucket`, `appop_RUN_IN_BACKGROUND`, `appop_RUN_ANY_IN_BACKGROUND`) | `optimize/tweaks.sh` reads every setting back after setting it; this firmware keeps its own value | nothing is broken and the setup goes on; mention it in the report. Do not set it again by hand. `optimize/tweaks.sh status` shows what each setting was (`optimize/run/tweaks-journal.txt`) and is now; `optimize/tweaks.sh restore` puts back exactly the recorded values (a value that was not set is deleted) |
| `!! window_animation_scale: could not read what this TV has, so it is left as it is` (or another setting) | a read of the setting failed or came back empty, so `tweaks.sh` neither records it as «not set» nor changes it (a wrong «not set» would make restore delete the TV's real value) | nothing is broken; the same command later reads it again (the journal says `unknown` until then). Do not set it by hand |
| `!! standby_bucket: the TV's own value was not recorded` (or `appop_…`) | this firmware did not say what the Play Store's standby bucket or background appop was before `tweaks.sh apply`, so `restore` puts `active` / `allow` back (forced: Android may move the bucket again); a bucket Android exempted (the Play Store is exempt from standby) is never changed | nothing is broken; mention it in the report |
| `!! screensaver_components: this TV did not take our screensaver` | the firmware keeps its own screensaver whatever is written (seen on some operator TVs) | tell me in Arabic to choose «شاشة تلفزيونك» in the TV's own settings (الإعدادات ← تفضيلات الجهاز ← شاشة التوقف); the screensaver app is installed |
| `!! enabled_accessibility_services: this TV did not take the sleep service` (`dock.sh sleep-setup`) | the TV refused the accessibility entry | the sleep timer still counts down but its end will not turn the screen off; say so. `optimize/reset.sh` takes only our entry out of the list |
| the person says «التلفزيون ما ينام» or «الريموت يصحّيه ببطء» (the TV does not sleep, or the remote wakes it slowly) | something may be holding the TV awake: a wake lock, an alarm or a job | run `optimize/measure.sh power` (read-only: it changes nothing on the TV): it lists Doze, the wake locks, and the alarms and jobs that name UrTV. The 6-hour update job and the adhan alarm are expected (`docs/POWER.md` says why each exists); a wake lock naming `sa.urtv` while nothing plays is not. Report what it says; it shows what is held at that moment, not watts, and only a power meter measures those. Do not turn off the update job or the adhan |
| `!! familydns: this TV cannot reach family.adguard-dns.com on port 853` (or `could not read`, `did not take`, `the internet did not work … put back`, `older than 9`, `no permission: run optimize/dock.sh familydns grant`, `حماية العائلة تحتاج تطبيق الواجهة`) | the person asked for «حماية العائلة»; the network blocks DNS over TLS (port 853), or the TV could not be tested, or the setting was refused, or the internet stopped working through the service. In every case the TV's old Private DNS setting is as it was | nothing is broken (for `تحتاج تطبيق الواجهة`: run `optimize/install.sh launcher` to get the home app that has this feature, then `on` again). For `no permission`, run `optimize/dock.sh familydns grant` once, then `on` again. Tell the person in Arabic that the family filter could not be switched on here and why in one line; do not set `private_dns_mode` by hand. `optimize/dock.sh familydns status` shows the state; `off` puts back what was recorded |
| `!! left out of the restore scripts (a character they do not send)` | `optimize/backup.sh scripts` leaves out a value with a character that means something to a shell (`$`, a quote, `%`) instead of sending it | nothing is broken; the value is one the TV itself reported. Give the person `optimize/run/restore-scripts/` only after you read it, and say which setting was left out |
| `!! More than one device answers with ADB open` (find-tv) | two or more devices on the Wi-Fi have ADB open (another TV, a box, a VM); the script never picks one (9 field reports picked the wrong one) | show the person the numbered list (address, brand, model, Cast name), ask which one is their TV (Settings → Network on the TV shows its address), then `optimize/find-tv.sh use <IP>`. Skipped on purpose: virtual-machine networks, the router, this computer |
| `!! kept.txt could not be read` | the file of apps the person asked to keep on cannot be read, so «nothing kept» would be a guess | nothing was changed; fix the file's permissions (or delete it if it is empty on purpose) and run the same command again |
| `!! enabled_accessibility_services: the TV did not say …` (dock.sh sleep-setup, reset.sh) | the TV answered nothing for the accessibility list; an empty answer is not «no service on» (it would overwrite the person's own list or switch their accessibility off) | nothing was changed; run the same command again |
| `!! screen_off_timeout: the TV did not say its value` (measure.sh) | the TV's sleep timeout could not be read, so it was not stretched | the reading may be spoiled if the screensaver starts; run the measurement again |
| `!! The TV's package list came back empty (or nearly) while the backup was saved` (run.sh) | a backup without a package list would be kept for ever and make `reset.sh` look for apps to remove | nothing was changed; check the TV is awake and run the same command again |
| `!! community list not applied: the Unsafe check (UAD-ng) needs the internet…` or `…the TV did not say which app is its home screen…` | the third-party community list is applied only with the safety rating and with the home app known | nothing was turned off; run `optimize/community.sh` again (online) |
| `!! Nothing accepts a connection at <IP> on port 5555…` (find-tv `use`) | the address the person read is wrong, the TV is off or asleep, or Developer options → USB debugging (Wireless debugging) is off | nothing was saved; ask me to read the address again (Settings → Network on the TV) and to check debugging is on, then `optimize/find-tv.sh use <IP>` again |
| `!! «<IP>» is not a home network IPv4 address…` (find-tv `use`) | what was typed is not an address of a home network (a typo, an IPv6 address, a name) | ask me to read the TV's address again, then `optimize/find-tv.sh use <IP>` |
| `!! familydns: the home app did not put the old Private DNS back (…)` | the home app on the TV could not restore the Private DNS it recorded, or did not answer (the TV was busy or asleep) | the record is kept: run `optimize/dock.sh familydns off` again; `reset.sh` stops until it works. Do not set `private_dns_mode` by hand |
| `!! familydns: this TV did not allow the permission…` | some operator builds refuse the permission the on/off needs | nothing was changed; tell the person in Arabic that the family filter cannot be switched on from here; do not work around it |
| `!! familydns: the TV did not answer whether the home app…` | the question about the home app got no answer (ADB dropped), so the script does not guess «nothing to put back» | nothing was changed; the same command again |
| `!! Could not read this TV's package list…` (install.sh) or `!! restore: could not read the TV's package list…` | the TV gave no (or a cut) answer to `pm list packages`, so its own home screen cannot be found; install.sh stopped before the home switch, restore left the safety switch armed | nothing was changed; the same command again |
| `!! disable.sh: the TV did not say which packages it has or which were already off, so NOTHING was turned off…` | the package lists came back empty or cut; a restore could not be exact, so no package was turned off | nothing is broken and nothing changed; run the same command again once the TV answers (`run.sh` stops at that step, then `optimize/run.sh` continues from it) |
| `!! restore: the home was put back, but these files could not be read…` | a list or `optimize/run/disabled-by-us.txt` is unreadable (a permission or disk error); the stock home was put back first, only the package part was skipped | fix the file (or its permissions), then `optimize/restore.sh` again; do not delete the ledger: it is what keeps a restore from turning on apps the TV had off itself |
| `Note: there is no record of what our setup turned off…` (restore.sh) | an install from before the ledger and no backup: every package of the lists is turned on, including any the TV itself had off | tell the person if an app they had turned off by hand is back; nothing to fix |
| `!! restore: could not tell which app is this TV's own home…` | no stock home is known (not in the journal, not in the package list), so no home was set and the safety switch was left as it was | press Home on the remote; if nothing opens, tell the person to use the TV's own settings (Apps, Default apps, Home app); do not name a home by guess |
| `left on until the person says (ask in one Arabic line…): <packages>` (community.sh) | the community list names an update service, voice, MiLink or a video player (`optimize/lists/community-ask.txt`), things owners often want | ask the person in one Arabic line; on a yes add the packages to `optimize/lists/mine.txt` and run `optimize/run.sh again`. YouTube Music is never turned off |
| `!! standby_bucket: could not read what this TV has for the Play Store` (or `appop_…`) | the Play Store's bucket or background setting could not be read, so it was left as it is (nothing to put back) | nothing to do. `info: standby_bucket stays … on this test build` is the same on a userdebug / test-keys TV: not a problem |
| `!! The TV did not say what it is (its feature list came back empty)` (precheck) | the TV answered nothing (still starting?), which is not the same as «not an Android TV» | wait a minute, check it is awake and on Wi-Fi, run `optimize/precheck.sh` again |
| `!! familydns: could not ask the TV whether the home app is installed` (reset.sh) | the question failed; removing the app could lose its record of the original Private DNS | nothing was removed; run `optimize/reset.sh` again |
| `!! <package> is in optimize/lists/keep.txt …` or `… is Android's own core …` (keep.sh unkeep) | `unkeep` has the same refusals as `disable.sh` | nothing to do |
| `!! Could not tell which app is this TV's own home screen …` (install.sh) or `!! restore: … is not installed on this TV, so it is not named as the home` | the launcher packages are not installed and no single other app answers Home (seen on a ctv m7332 box); a guessed one would be «restored» to nothing | the home switch was not made and nothing changed; UrTV works as an app and the screensaver |
| `!! photos.sh: the TV did not say its photo mode` | the photo mode could not be read, so it would have been replaced by a guess | run the same command again |
| `(the saved address <old> does not answer, but one device on this Wi-Fi has ADB open at <new>: …)` (while waiting for the TV after a restart) | the TV came back with another address (the router gave it a new one) | ask the person whether that device is their TV, then `optimize/find-tv.sh use <new>`; the tools never switch address by themselves |
| `!! skipped after 2 timeouts: <app>; to add it later: optimize/install.sh extras` | an optional app (TV Bro, SmartTube) could not be uploaded or installed in two tries (slow Wi-Fi, a slow set); the setup goes on without it | nothing is broken; tell the person in one line; `optimize/install.sh extras` tries again later. The screensaver and the home app are never skipped |
| `!! No build of <repo> for this TV (<abis>): skipped` | no download matches the TV's processor types (read up to 3 times, with fallbacks, so a flaky answer is not mistaken for «no build») | nothing to do; the app is optional |
| `!! Play Protect refused the install over ADB, but the TV did not say what its ADB check is set to` | the TV answered nothing for the setting, so it was not switched off (it could not be put back exactly) | nothing was changed; run the same command again. `!! Play Protect's ADB check could not be put back` means the connection dropped after it was switched off: run the command it prints |
| `!! This device is not an Android TV` (`run.sh: stopped at precheck`) | no `android.software.leanback`: a phone, a tablet or a box that is not Android TV | nothing was changed; give me the Arabic message in the line, and stop |
| `!! WebView: system WebView is 66` (or any number below 80) | the screensaver page needs WebView/Chrome 80+ (its CSS: clamp, gap, inset) and would not run | tell me in Arabic to update «Android System WebView» from the Play Store (`optimize/precheck.sh webview-store` opens its page); then `optimize/precheck.sh` again. The setup and the report go on (our screensaver is installed but the TV's own stays selected until the WebView is new enough), the line stays until it is fixed |
| `The setup wizard of this TV owns its saved Wi-Fi network: left on` or `The setup wizard answers Home and owns the saved Wi-Fi network: it is left on` | disabling `com.google.android.tungsten.setupwraith` can delete the Wi-Fi network saved during the first-run wizard (two TCL sets lost their password; CleanYourTV documents it). `install.sh` turns the wizard off only when it really answers Home and the network is not its own; otherwise it switches the home with the wizard left on | nothing to do. Never turn the wizard off yourself, never delete a saved network or its files, and never override this: if Home then does not open the new launcher (`!! Home did not open`), the TV keeps its stock home; say so. `optimize/restore.sh` and the safety switch always turn the wizard back on |
| `!! Auto-start (TCL): not allowed for sa.urtv.home(ignore)` | TCL firmware starts apps at boot only if they hold its auto-start op; ours need it (adhan alarms after a restart, the sleep service) | `optimize/autostart.sh` again; the report shows `Auto-start (TCL): … allow` after the restart. Not a TCL: «Auto-start: not a TCL», nothing to do |
| `!! Home did not open the new launcher. The stock home was put back. (role holder: …; Home resolves to: …; on screen after Home: …)` | the role and the home activity were asked twice and Home still went to the stock home. The brackets say why: who holds the Home role, what Home resolves to, and what is on screen after Home (a Home chooser or a permission dialog, or the stock launcher). Seen on 5 TVs on build 142 (MOKA R10G, TCL G13, Xiaomi AFKR0 and AFMU0, TCL BeyondTV4) | run the same command once more (it worked at the second run on a Xiaomi). If the same line comes again, this TV keeps its own home: continue with `SKIP_LAUNCHER=1 optimize/run.sh`; UrTV works as an app and the screensaver. Never keep retrying. If the line before it said «An on-TV dialog is open», ask the person, in Arabic, to choose «تلفزيونك» then «دائماً» / «السماح» with the remote (the script waits 40 s for it) |
| `Home switch skipped: this TV keeps its own home screen. Its setup wizard must stay on to keep the saved Wi-Fi password…` | the setup wizard owns the saved Wi-Fi network, so it must stay enabled, and Home still goes to the stock home. Not an error | nothing to do, and do not retry: UrTV works as an app and as the screensaver, the stock home is back as it was, and the run goes on with the other steps. Tell the person in one line; never turn the wizard off by hand |
| the TV has no home at all (black screen, Home does nothing) | our home app is gone while the stock home is off | `optimize/restore.sh` (no list: only the home screen), then `optimize/install.sh launcher`; more in `docs/OPTIMIZE.md`, «Home screen problems» |
| `!! The stock home is not on screen` (reset) | the stock home was slow to start | ask me to press Home, then `optimize/reset.sh` again |
| `!! still off` (keep.sh) | a protected feature would not turn back on | **stop**, show me; don't continue |
| `left off by the TV itself` (keep.sh) | a protected package was already off before UrTV and the TV won't turn it on (TCL ships Alexa off outside its regions) | nothing to do: go on |
| `start-saver.sh --preview` prints `dream` on some runs and `preview` on others on the same TV | the real screensaver starts sometimes, the app's preview the other times; both draw the same page | nothing to fix, either shot is good |
| the TV's own screensaver screenshot is black | Android hides it from screenshots | expected; ours can be shot |
| our screensaver shot has no weather or matches yet | data still loading | `optimize/saver.sh update`, wait a minute, shoot again |
| `find-team` finds nothing | a spelling our list doesn't have, or a club outside the app's leagues | once with the English name; or `optimize/saver.sh find-team --list <league word>` (e.g. `Saudi`, `Premier`) and pick it from there |
| `screensaver: preview` (shot.sh), or «not measured» for a screensaver | this Android won't start the screensaver from ADB (some Android 14 sets) | expected: the shot is our app's preview of the same page; don't look for other ways to start it |
| `optimize/run.sh status` has no «protected features» step | an old copy of this repo | `git pull`, then `optimize/run.sh` |

Anything else, or anything twice: stop, show me the output and what you think it is.

## 1. Connect and look (5 minutes)

```
optimize/setup.sh
optimize/find-tv.sh                       # accept "Allow USB debugging?" on the TV, tick "Always allow"
optimize/venv/bin/python -I optimize/tv.py --probe     # first tell me to watch the TV (below)
optimize/precheck.sh                      # is it an Android TV, is its WebView 80 or newer (run.sh does it as its first step too)
optimize/shot.sh home my-apps
```
Right before `--probe`, tell me, word for word, and **don't end your turn or wait for my reply**: run `--probe` straight
after it. It waits by itself (silent, up to 2 minutes) until I press Allow; that is the wait, not a fault. If it times
out, tell me again and run it once more.

> الحين بتطلع لك على التلفزيون رسالة «Allow USB debugging?»: اضغط Allow وعلّم «Always allow». بعدها تقدر
> تتركني، أكمّل لحالي حوالي ٤٠ دقيقة.

- `optimize/precheck.sh` decides first whether to go on. **Exit 3, «not an Android TV»**: stop, change nothing, and give
  me its Arabic message as it is (Samsung Tizen and LG webOS TVs can't run these apps; a phone or tablet is not a TV).
  **Fire TV** (Amazon): tell me in Arabic it may work only partly and the home switch may be refused; go on.
  **`!! WebView: system WebView is NN`** (below 80; the screensaver page's CSS (clamp, gap, inset) needs 80, and on 66 it never ran): tell
  me its Arabic text, and offer to open the page myself (`optimize/precheck.sh webview-store` opens «Android System
  WebView» in the TV's Play Store; I press Update; then `optimize/precheck.sh` again). The setup can go on meanwhile, and
  `report.md` keeps the `!!` line until the WebView is new enough. Never try to change the page for an old WebView.
- Tell me the model and Android version.
- Read the row for this TV model in `docs/TVS.md` before going on: it says what was seen on that model (ADB port,
  what to watch out for).
- Look at `optimize/run/shots/my-apps-small.png` (the 960 px copy; the full-size `my-apps.png` is often too large
  to attach) and write down the first **6 apps** of my apps / favourites row, left to right, by name: they become
  the favourites of the new home screen in step 4.
- Only then, if this TV was set up with these tools before (also an older version of it), run
  `optimize/reset.sh` so we start like new.

## 2. Before you run it: how to undo

Before you start step 3, tell me in plain Arabic how to undo everything, so I know it is safe: one command,
`optimize/reset.sh`, run from this folder (say the folder's full path), puts the TV back as it came (the
disabled apps back on, the TV's own home screen and screensaver back, our two apps removed, TV Bro and
SmartTube removed if the setup installed them, the speed tweaks back to the values the TV had before, from `optimize/run/tweaks-journal.txt` (written by the first `tweaks.sh apply`) or `optimize/run/backup.txt`, else Android's defaults; a value the TV did not have set is deleted); my photos, accounts and other apps are not touched. Any agent on this computer can
run it for me later.

Also tell me, in Arabic, before the first restart: «التلفزيون بيعيد التشغيل مرتين. بعض تلفزيونات TCL نسيت الواي فاي بعد
أول إعادة تشغيل، فجهّز كلمة سر الواي فاي. لو رجع التلفزيون بدون إنترنت: الإعدادات ← الشبكة والإنترنت، اختر شبكتك
واكتب كلمة السر، وقلّي «تم».» (Table: «the TV forgot its Wi-Fi».)

## 3. Run it (20–30 minutes, the TV restarts twice)

```
optimize/run.sh
```
It saves the TV's state, measures memory and the TV's own screensaver, disables telemetry/ads/demos and my picks,
applies the speed and memory tweaks, installs the screensaver and the home screen (behind the safety switch) and
TV Bro and SmartTube (from their own GitHub releases, the build for this TV's processor),
measures again and writes `optimize/run/report.md`. Before the optional picks, on a TV whose model our own lists do not
know (a Sony, Hisense, Haier, Xiaomi…), `optimize/community.sh` applies the tested list for exactly that model from
CleanYourTV (MIT, `optimize/lists/community/`) if there is one: never a list for another model, never anything in
`keep.txt`, a launcher or a setup wizard; `report.md` shows them («Community list: …») and `optimize/keep.sh off` names them. If it stops for any reason: `optimize/run.sh` again (it
continues), and `optimize/run.sh status` shows where it is.

If the home-screen step says Home did not switch, leave the TV alone for 5 minutes (the stock home comes back by
itself) and tell me. Some TCL firmware keeps its own home: then `SKIP_LAUNCHER=1 optimize/run.sh`.

## 4. Apply my choices (5 minutes; no remote needed)

Every setting, and what to say when I ask for something else: `docs/SETTINGS.md`.

```
optimize/saver.sh cities                          # find my city (Saudi Arabia, the Gulf, Egypt, the Levant, North Africa), then: optimize/saver.sh city <name>, even for Riyadh (until a city is set the screensaver shows a «change your city» note)   (not listed: city <name> <lat> <lon> <time zone, e.g. Asia/Riyadh, Asia/Kuwait or Africa/Cairo; a place that is not in the list is refused without it>)
optimize/saver.sh prayermethod jafari             # only if I asked for the Shia Ja'fari times (auto = my country's own, the default; gulf = the Gulf method for any place, Oman's older one)
optimize/saver.sh find-team <club as I wrote it>  # Arabic is fine (الهلال): one call per club, Saudi clubs first;
                                                  # then: optimize/saver.sh teams <id:Name> <id:Name> ...  (my order); «لا شي»: optimize/saver.sh teams none
optimize/saver.sh card <one of: verse, wisdom, adhkar, stocks, tasks, year, shows, prayers, forecast, matches, none>   # beside the clock (verse unless I said otherwise)
optimize/saver.sh home-card none                  # home screen: no card
# «المهام» (only if I ask): optimize/saver.sh tasks add 'نص' | list | done N | del N | clear, and card tasks
optimize/dock.sh apps                             # match my 6 apps to packages, then: optimize/dock.sh set <pkg> ... (in that order)
optimize/dock.sh hidden                           # apps the home screen leaves out, and why
optimize/dock.sh names                            # every app's name: package | shown | its own | from; then name in Arabic,
                                                  # in ONE call, each app whose shown name has no Arabic letters (rules below):
                                                  # optimize/dock.sh name <pkg> '<الاسم>' <pkg> '<الاسم>' ...
optimize/saver.sh photos <5, 10, 15, 30 or 60>   # ONLY if I asked how often the photo changes (default 30 minutes: leave it)
optimize/saver.sh idle <1, 2, 5, 10, 15 or 30>   # ONLY if I asked when the screensaver starts after the TV idles (the TV's own timer: leave it)
optimize/saver.sh cardstyle <current, dark1, glass1, dark3 or glass3>   # ONLY if I asked for another look for the card beside the clock (default current: leave it)
optimize/saver.sh national <auto or off>          # ONLY if I asked to turn off the green look on National Day and Founding Day (default auto: leave it)
optimize/saver.sh prayeradj <fajr +2 maghrib -1 …, then on>   # ONLY if I said my mosque's adhan is minutes early or late (default off: leave it); off goes back to the calculated times
optimize/music.sh adhan fajr-voice <1-21>          # ONLY if I asked for another muezzin at Fajr (default 10, «فجر الحرم المكي»: leave it); adhan voice <n> is for the other four prayers (never 10, 13, 17, 20)
optimize/music.sh adhan short <on or off>        # ONLY if I asked for a short adhan, stopping after its first minute (default off: leave it)
optimize/music.sh adhan stopkey off              # ONLY if I asked that a key does NOT stop the adhan (default on: any key stops it; leave it)
optimize/saver.sh hijrioffset <-2 to 2>          # ONLY if I said the Hijri date is a day or two off mine (moon sighting: Egypt, Jordan, the Levant, Morocco) «التاريخ الهجري يتأخر/يتقدم يوم» (default 0: leave it; do not guess it from my country)
optimize/saver.sh clock24 <on or off>             # ONLY if I asked for a 24-hour clock (19:30 instead of 7:30 م; default off: leave it)
optimize/saver.sh digits <hindi or latin>        # ONLY if I asked for 123 instead of ١٢٣ (default hindi: leave it)
optimize/saver.sh listenpill <on or off>          # ONLY if I asked to remove the «تلاوة وأذان» button from the home screen (default on: leave it; the adhan keeps working, the screen opens from the app menu)
optimize/saver.sh updatebell <on or off>        # ONLY if I asked to remove the «جرس التحديثات» bell from the home screen (default on: leave it; it shows only while the Play Store has updates waiting)
optimize/saver.sh markets <up to 4 of tasi, aramco, spx, btc, gold, silver, xrp, comma-separated>   # ONLY if I asked for other markets at the top left (default tasi,spx,btc: leave it)
optimize/saver.sh markets off                    # ONLY if I asked to hide the markets line at the top and keep just the weather (`markets on` brings it back)
optimize/saver.sh clocksize <large, medium or small>   # ONLY if I asked for a smaller screensaver clock (default large: leave it)
optimize/saver.sh mkstyle <arrows or prices>      # ONLY if I asked to see the prices, not just arrows (default arrows: leave it)
optimize/saver.sh stocks <Tadawul codes and US tickers, e.g. 2222 1120 AAPL>   # ONLY with card stocks: my watchlist, up to 8 (default 2222 1120 7010 2010: leave it)
optimize/saver.sh slot <matches, weather, fasting, countdown, markets or none>   # ONLY if I asked what the bottom-left slot shows (default matches: leave it; with no team it shows the weather by itself)
optimize/saver.sh countdown <YYYY-MM-DD> '<name>'   # ONLY if I asked for a countdown (also: slot countdown); "countdown clear" removes it
# own photos (only if I ask, or say yes to the offer in step 7): optimize/photos.sh add FILE... | list | del N | clear | mode mix|only | show
optimize/saver.sh update
optimize/saver.sh show                            # read both back to me
optimize/dock.sh show
```
The home screen lists what the TV's own home listed: next to a Google home only TV apps, so the maker's helpers
stay out. Change that only by these rules: `optimize/dock.sh show <pkg>` for an app it left out that is one of
mine (on the step 1 screenshot) or that I asked for; `optimize/dock.sh hide <pkg>` for an app it shows that is in
`optimize/lists/tcl-safe.txt` or `tcl-optional.txt`; never hide one in `keep.txt`. Any other you are unsure about:
list it for me in step 7 instead of choosing.

App names: the home screen shows an app's name in Arabic when the app has one or our list knows it; give every
other app (shown name with no Arabic letters) its Arabic name yourself with `dock.sh name`. Use the name Arabic
speakers use for it: the brand's own Arabic name if it has one (شاهد، ثمانية، أنغامي), else the usual Arabic
spelling (نتفليكس، يوتيوب، ديزني +، ابل تي في), and for a tool, what it does (الإعدادات، الملفات، المتصفح، المصادر).
Up to 30 characters, no ' ; or |. Leave an app whose shown name is already Arabic. In step 7, list the names you
gave so I can change any (`dock.sh name <pkg> '<اسم آخر>'`, or `''` for its usual name).

Take every id from `find-team`'s output, which comes from our list of every club in the app's leagues and the
national teams; never guess one. If it gives several, pick the one I meant (a Saudi club before a namesake abroad).
If I said «لا شي» to clubs, skip `find-team` and clear the defaults (الهلال والمنتخب السعودي would stay otherwise):
`optimize/saver.sh teams none`. The same any time later if I stop following a team and follow no other. `optimize/saver.sh` without arguments lists every option.

## 5. Check (5 minutes)

```
optimize/shot.sh home
optimize/shot.sh screensaver
```
Look at both screenshots (in `optimize/run/shots/`; open the `-small.png` ones, 960 px wide: the full-size files are often too large for an agent to attach) and describe them to me. Then check Home from inside an app
yourself (I may have left): open one, press Home, and the new home must be in front:
```
optimize/check-home.sh          # opens YouTube (else my first favourite), presses Home; or: optimize/check-home.sh <another app of mine>
```

## 6. Before you tell me it's done

Check each, and fix or report what fails:
```
optimize/keep.sh                                  # must end with «Protected features: all on.»
optimize/run.sh status                            # every step ✓, and «report: no !! lines»
```
(One `!!` line is allowed there: «the screensaver may slow this TV». It is advice for me, not a failure: see step 7.)
- `report.md` shows Home as `sa.urtv.home` (or the stock home if we used `SKIP_LAUNCHER=1`) and the screensaver
  as `sa.urtv.screensaver`.
- You looked at the step 5 screenshots yourself and they show my city, my card and my favourites.

## 7. Tell me

- The before/after table from `optimize/run/report.md` and its feature check.
- **If `report.md` has the line `!! the screensaver may slow this TV: …`** (our screensaver took much of this TV's
  memory or processor next to the TV's own), tell me so in Arabic, in one or two sentences, and **ask before you do
  anything**. For example: «على تلفزيونك هذا شاشة التوقف حقتنا تستهلك ذاكرة ومعالج كثير وممكن تبطّئه. تقدر نطفيها
  ونرجعها في أي وقت، تبي أطفيها؟» Only if I say yes, run `optimize/saver.sh off` (the TV's own screensaver comes
  back; ours stays installed) and read `optimize/saver.sh show` back to me. If I say no, leave it; `optimize/saver.sh on`
  turns ours on any time. Don't turn it off on your own, and don't hide the line.
- What was disabled, my settings, and anything that looked wrong or slow.
- **What was turned off, by name, and a chance to keep one (once; it must not hold anything up).** Run
  `optimize/keep.sh off` (or read `report.md`, «Turned off by the setup») and show me the apps by their usual names,
  in Arabic («تنبيهات TCL»، «حساب TCL»، «عروض الشركاء»…), in a short list. Then ask once, after the summary:
  «هذي التطبيقات اللي أطفيتها. إذا تبي واحد منها ارجعه قلّي «أبي هذا» واسمه.» Most people say nothing: then go straight on
  to the next point and never ask again. If I name one (or several), find its package in that list and run
  `optimize/keep.sh restore <package> …`; it turns it on and remembers it, so a later `optimize/run.sh again` leaves it
  on. (`optimize/keep.sh unkeep <package>` if I change my mind.) Never turn an app on by hand with your own `pm` commands.
- When I'm at the TV, to try voice from the remote and casting (AirPlay or Cast) from a phone once, and to tell
  you if either doesn't work (`optimize/keep.sh` turns them back on).
- **Network debugging after the setup (once; default: leave it on).** Ask me in Arabic, in one line: «للخصوصية تقدر
  تقفل تصحيح الأخطاء. البيانات تتحدّث بدونه، لكن تحديث التطبيقات وأي تغيير من الكمبيوتر يحتاجه، فتفتحه وقتها.
  تبي تقفله؟» The page's data and content update by themselves without ADB; a new version of the apps (design and developer
  updates) and any change of settings from the computer need it. If I say no or say nothing: leave it on and go on. If I say
  yes: I turn it off myself on the TV (Settings → System → Developer options → USB debugging, Network debugging and
  Wireless debugging off; never you: turning ADB off locks the tools out), then `optimize/find-tv.sh --closed` confirms
  the port is closed (`OK: ADB is closed`; `!! ADB still answers` says what is still on; `?? Can't tell` means the TV is
  asleep). To change anything later I turn it back on and you start from `optimize/find-tv.sh`.
- In one line, what I can still ask you to change any time: the favourites, the card on the home screen, the
  screensaver delay, and the optional TCL extras (the TCL tab and channels, game bar, T-Cast…).
- How to undo, again: the folder's full path and the exact command to paste there
  (`optimize/reset.sh`; on Windows in Git Bash), what it brings back, and that if I ever want it, I can open an
  agent on this computer and say «رجّع التلفزيون مثل ما كان: شغّل optimize/reset.sh في مجلد UrTV».

### Offer: my own photos (once, after step 7; skip it if I'm in a hurry)

Ask me in one line, in my language: «تبغى صورك أنت خلفية للشاشة؟ لاعبك المفضل، بلدك، عائلتك… من ١ إلى ١٠ صور.»
If I say no or don't answer, say nothing more about it. If I say yes:
- **Ask me where the files are** (a folder or the file names) and wait. Never look for them yourself: don't list, search
  or open my Pictures, Downloads, Desktop, phone backups or any other folder I haven't named. Use only the files I named.
- 1 to 10 photos. Ask whether they should **join** the Saudi photos (`mix`, the default) or be **the only ones** (`only`).
- Run `optimize/photos.sh add <the files I named>`. It makes each one ready on this computer (upright, at the TV's screen
  size, JPEG, all metadata such as location and camera removed) before anything is uploaded, so tell me that's done
  and that nothing leaves this computer except to my TV. If I chose `only`: `optimize/photos.sh mode only`.
- Show me the result: `optimize/photos.sh list`, then `optimize/photos.sh show` and look at the screenshot yourself.
  Never keep, copy or upload my photos anywhere else, and never put them in this folder.
- Tell me I can change it any time: `optimize/photos.sh del N`, `clear` (the TV goes back to our photos only), `mode mix|only`,
  or in the screensaver app's settings, row «صوري» (mode, remove all). Adding photos is only through you.

### Offer: a second TV (only if I say I have another one)

If I mention another TV that already has تلفزيونك, offer once, in one line: «تبغى أنقل إعداداتك من التلفزيون الثاني؟»
If yes: `optimize/saver.sh export > settings.json` on the one that has them, then on this one `optimize/saver.sh import settings.json`
(a preview), show me the list, and only after I agree `--yes` (`docs/SETTINGS.md`, row «نقل الإعدادات»). Never my photos, tasks or own adhan.

## 8. Ask me to share how it went (the very last thing)

Only after everything above, undo included, and nowhere earlier: your last message, on its own, word for word:

> آخر شي: تبغى ترسل لصاحب المشروع ملخص كيف مشى التركيب؟ يساعده يحسّنه للناس. فيه بس نوع التلفزيون ونسخة
> أندرويد، وكم أخذت كل خطوة ووش اللي تعطّل، والذاكرة قبل وبعد. ما فيه أي شي شخصي: لا مدينة ولا أندية ولا
> تطبيقات ولا عناوين. وإذا عندك كلمة تبغى توصله (عجبك؟ شي ضايقك؟)، اكتبها مع ردك.

If I say yes, write the report first and send nothing: your name and model as the agent; my words if I wrote any (as I wrote them, minus
anything personal); and your own notes: what failed or was slow, how long it cost and what fixed it, in two to five
plain lines, with no names, addresses or paths:
```
optimize/share.sh --preview --agent "<you and your model>" --owner "<my words, or leave it out>" "<your notes>"
```
This is the one command here that needs quotes: the agent name and the notes are several words (no apostrophes
in them). On Windows, inside the Git Bash `-lc "…"` string use single quotes for each:
`-lc "cd <this folder> && optimize/share.sh --preview --agent '<you and your model>' --owner '<my words>' '<your notes>'"`.
`--preview` prints exactly what would be sent and sends nothing. Show me that whole text, ask me «أرسله؟», and only
if I say yes run `optimize/share.sh --send` (it sends the file as it is; no notes, no other words). If I want
something changed or removed, run `--preview` again with the change and show me the new text; never send a text I
haven't seen. `--send` sends the summary to the author (anonymously, no account) and prints `Sent to the author`:
tell me it went. If it couldn't send it, it opens a QR code on this computer's screen instead
(`optimize/run/share-qr.png`; if it didn't open, open it for me): tell me to scan it with my phone's camera, and the
page that opens has a button that sends it. Never run it if I say no.
