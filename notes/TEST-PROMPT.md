# Test UrTV's tools on a TV: the prompt for your AI agent

A quick check that the tools work with your TV and computer, for the developer. Nothing on the TV changes: no
installs, nothing disabled, no settings (only a tiny test file, deleted right after). Paste this to an agent that
runs commands on a computer on the same Wi-Fi as the TV (Codex: choose «Approve for me»):

> Test my TV with the `next` branch of https://github.com/tshi-dev/UrTV and follow its `notes/TEST-PROMPT.md`. You are on a computer on the same Wi-Fi as the TV.

`next` is where changes wait until a test like this passes; only then do they go to `master`, which everyone's
setup uses.

---

Test UrTV's tools on my TV and write a report for the developer. Don't change anything on the TV: no installs,
nothing disabled, no settings, no reset. Talk to me in Arabic; keep commands in English.

- Windows: run every command in Git Bash: `"C:\Program Files\Git\bin\bash.exe" -lc "cd <folder> && <command>"`.
  Codex: run Git Bash outside the sandbox (its sandbox can't start it).
- One command at a time, exactly as written: no extra quotes, `;` or `|` (on Windows PowerShell would run those
  itself). If one fails, note its output and go on to the next. Don't fix anything or write your own scripts.

In the UrTV folder, on the `next` branch (`git clone -b next https://github.com/tshi-dev/UrTV`, on Windows into a
short folder such as `C:/UrTV`; or in an existing folder `git fetch origin`, `git checkout next`, `git pull`):

1. `optimize/setup.sh`: must end with «✓ Python ADB client».
2. `optimize/find-tv.sh --check`, then `optimize/find-tv.sh`.
3. Tell me to watch the TV and press Allow (tick «Always allow»), then without waiting for my reply:
   `optimize/venv/bin/python -I optimize/tv.py --probe`
4. `optimize/selftest.sh`: an upload, Arabic text, the screensaver from ADB, and the apps a home screen can list.
   Copy all its output into the report.
5. Club search, one call each: `optimize/saver.sh find-team الهلال`, then `السعودية`, `النصر`, `ريال مدريد`,
   `الاهلي` (the Saudi club must come first) and `Atletico Madrid`.
6. `optimize/shot.sh home`, and look at the picture.

Then write one report, in English, that I can paste to the developer:
- you (agent and model), the computer's system, and how long the whole test took;
- the TV's maker, model and Android version from step 3;
- each step 1–6: OK or FAILED, with the output lines that matter: every `!!` line and error, all of step 4's
  output, the find-team results;
- anything slow, confusing, or that you had to retry.

Leave out the TV's IP address, my user name and folder paths. Save it as `optimize/run/test-report.md` and show it
to me.
