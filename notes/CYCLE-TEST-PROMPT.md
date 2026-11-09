# Install and reset a TV three times: the stress test for your AI agent

Runs the whole setup from the `next` branch and puts the TV back as it came, three times in a row, to prove both
work every time and leave nothing behind. It takes 45 minutes to 2 hours (a TCL on good Wi-Fi: 41 minutes); the TV restarts several times. Only a few minutes
need you, at the start. Paste this to an agent that runs commands on a computer on the same Wi-Fi as the TV
(Codex: choose «Approve for me»):

> Stress-test my TV with the `next` branch of https://github.com/tshi-dev/UrTV and follow its `notes/CYCLE-TEST-PROMPT.md`. You are on a computer on the same Wi-Fi as the TV.

---

Install UrTV on my TV and reset it, three times, and write a report for the developer. Talk to me in Arabic; keep
commands in English. Everything except the start runs without me.

- Windows: run every command in Git Bash: `"C:\Program Files\Git\bin\bash.exe" -lc "cd <folder> && <command>"`.
  Codex: run Git Bash outside the sandbox (its sandbox can't start it).
- Run the commands exactly as written: no extra quotes, `;` or `|` (on Windows PowerShell would run those itself).
- Follow `docs/SETUP-PROMPT.md`'s rules and its table of errors, but skip its questions: the answers are below.
- Long commands (`run.sh`, `reset.sh`) can be silent for 5 minutes; give `run.sh` a 40-minute timeout. If a command
  fails, run the same command again once; if it fails again, write it down and go on to the next step.

## Start (with me, 5 minutes)

1. Get the `next` branch: `git clone -b next https://github.com/tshi-dev/UrTV` (on Windows into a short folder such
   as `C:/UrTV`), or in an existing folder `git fetch origin`, `git checkout next`, `git pull`.
2. `optimize/setup.sh` (must end with «✓ Python ADB client»), `optimize/find-tv.sh --check`, `optimize/find-tv.sh`.
3. Tell me to watch the TV and press Allow (tick «Always allow»), then without waiting for my reply:
   `optimize/venv/bin/python -I optimize/tv.py --probe`
4. `optimize/shot.sh home my-apps`: write down the first 6 apps of my favourites row, left to right.
5. If this TV has UrTV on it already: `optimize/reset.sh`, so cycle 1 starts like new.
6. Tell me I can leave, and that the TV will restart several times over the next hour or two.

## Each cycle (3 times)

Note the clock time at the start and end of each numbered step.

1. Install, with the test build of the apps: `APK_TAG=apk-next optimize/run.sh`
2. My choices: the same each cycle, except the clubs, so the club search gets tried too:
   - `optimize/saver.sh city الرياض`
   - the clubs, new and random each cycle: `optimize/venv/bin/python -I optimize/random-clubs.py` prints a seed and
     6 to 10 names, as people would write them (always a Saudi club in Arabic, a national team, and a club abroad
     in English). Run `optimize/saver.sh find-team <name>` for each, exactly as printed, take the club meant (a
     Saudi one before a namesake abroad), then `optimize/saver.sh teams <id:Name> ...` with all of them, in that
     order. Write the seed and each search's first line in the report
   - `optimize/saver.sh card verse`, `optimize/saver.sh home-card none`
   - `optimize/dock.sh apps`, then `optimize/dock.sh set <the 6 packages>` in my order
   - `optimize/saver.sh update`
3. Check:
   - `optimize/check-home.sh` (must say «OK: Home opens تلفزيونك»)
   - `optimize/dock.sh hidden` (copy its output into the report)
   - `optimize/keep.sh` (must end with «Protected features: all on.»)
   - `optimize/run.sh status` (every step ✓, and «report: no !! lines»)
   - `optimize/shot.sh home`, and look at the picture
4. Send the summary: `optimize/share.sh --agent "<you and your model>" "Cycle test <n> of 3. <what failed or was slow>"`
   (the one command that needs quotes, for its several words and no apostrophes in them; on Windows use single
   quotes inside the Git Bash `-lc "…"` string: `optimize/share.sh --agent '<you and your model>' 'Cycle test …'`)
5. Reset: `optimize/reset.sh`, then check it is really back as it came:
   - `optimize/shot.sh home after-reset-<n>`, and look at it: the TV's own home screen
   - `optimize/venv/bin/python -I optimize/tv.py pm list packages sa.urtv` must print nothing
   - `optimize/venv/bin/python -I optimize/tv.py pm list packages -d`: compare with the list `reset.sh` printed
     at its end in cycle 1's first reset; anything new is something left behind

## The report

Save it as `optimize/run/cycle-report.md`, show it to me, and keep it in English so I can paste it to the developer:
- you (agent and model), the computer's system, and the TV's maker, model and Android version;
- a table: cycle × step, each with its time and OK or FAILED;
- every `!!` line, error and retry, with the step it came from;
- each cycle's seed and club searches: what came back, whether the right club was first, and any name
  that found nothing;
- `dock.sh hidden` from cycle 1, and whether it was the same in all three;
- after each reset: anything left behind (our apps, disabled packages, home or screensaver not the TV's own);
- anything slower, or different, in one cycle than in the others.

Leave out the TV's IP address, my user name and folder paths.
