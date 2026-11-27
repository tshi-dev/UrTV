# 4.4: what is left before it goes out

The way a release goes is `docs/dev/RELEASE.md` (six steps, in order, on the exact commit). This page is 4.4's own list: each gate, who owns it, its status on the
heads now, and what is still to do. **Keep it updated** (the supervisor reads it; whoever changes a gate's status changes its line here, with the sha and the run).

Where 4.4 stands: two release lines. **Beta 4** (`work/44-beta4`, `next` follows it) is 4.4 as it is. **Beta 5** (`work/44-beta5`) is beta 4 plus the league work
(the football form style, the scorer of the league, the season's zones, the games remote). Beta 5 is kept in step by merging beta 4 into it; `next`, `master` and the
promotion to `apk-beta` are the supervisor's only.

## The gates

| # | Gate | Owner | Beta 4 `46dcad40` | Beta 5 `194af9eb` | What would turn it green |
|---|---|---|---|---|---|
| 1 | **Checks** (`tests/run.sh` in CI) | CI | green (run 46059910402) | running (dispatched after the zones merge) | a green `checks.yml` on the exact sha |
| 2 | **Build APKs** (compiles all four apps, runs the regressions group and the API-surface diff) | CI | green (run 46059913673) | running | a green `android.yml` on the exact sha |
| 3 | **Independent review** (`docs/dev/reviews/4.4.md`, `python3 .github/tools/review_gate.py`) | a fresh reviewer | **FAIL**: the review ends at `987aa209` (18 Nov); many files changed after it (the gate lists them) | same | a fresh reviewer reads `987aa209..<sha>` (the tree, the Saudi wording, the pray rows, the phone page `/y`, photos from a stick, «أذاني من ملف», the adhan per prayer, the tester files, the games and content merges), no blocker or high open, then `review_gate.py` PASS (docs/dev/RELEASE.md, «A fix after the review») |
| 4 | **Emulator round** (`Auto round` workflow: `tester/auto/round.sh` on an Android TV emulator, report in `data-screens/<run>/auto/`) | CI | not run yet | not run yet | the round GREEN on the sha (every step but what the machine cannot do, and the numbers inside the emulator's limits, `tester/auto/ci/thresholds-emulator.tsv`) |
| 5 | **Tester on a real TV** (`tester/RELEASE-SMOKE.md` S-01 to S-39 by hand, and `tester/auto/round.sh` on the owner's PC) | the tester, through the supervisor | not run on this head | not run | every S-step PASS and the real-TV round GREEN (its limits are `tester/auto/thresholds.tsv`); a FAIL names the register row (docs/dev/REGRESSIONS.md) it brought back |
| 6 | **Owner's OK** | the owner | not asked | not asked | his «OK» on the summary of 1 to 5, and the API diff's new lines (docs/dev/RELEASE.md, step 5) |
| 7 | **apk-beta, then master** | the supervisor | not done | not done | `promote-beta.yml` for the green sha (it runs the gate again, without the review), then, after the owner and the real-TV round, the merge to `master` (`android.yml` checks again before it publishes) |

A change after a gate went green starts the list again from gate 1 for the changed files (gate 3 only for them). A merge into the line (a data branch, a fix) is a change.

## What is not a gate but is still open

- **The tester's two known real-TV questions.** The radio from the UI path (the stations tab, the row, OK) was only ever tried through the adb agent path (`Background start not
  allowed` is an adb artifact); the automatic round walks the UI path on the real TV (`radio` step). The mute (`logcat -s UrTVMute` says who) is read by the same step.
- **Data that waits for the tester.** Republishing the released game packs q01 to q03 and the data-games publish stay on hold until the tester passes beta 4 (the supervisor says when).
- **Beta 5 only:** the zones reach TVs with the next archive build after the merge, not with the APK.
- **The CHANGELOG** is complete for what shipped in beta 4 (a line per change, Arabic, no «·»); the supervisor writes the release's own header line.
- **Not decided yet:** the card shape (the UI session's mockup and the owner's OK come first); the sports zones of other leagues; the per-prayer phone control on the main page (it is its own page `/y` because the main page is at its 64 KB limit).

## How to run the emulator round

1. Push the commit to run as a NEW branch, never forced: `git push origin <sha>:refs/heads/work/auto-round-<shortsha>` (a push of a branch named `work/auto-round-*` runs the
   workflow file of that commit; a `workflow_dispatch` needs the file on the default branch, which it has after `master` has it). The same sha again needs a new name
   (`work/auto-round-<shortsha>-2`). Delete the trigger branches when the run is read (`git push origin --delete work/auto-round-<shortsha>`).
2. The run builds the debug apps of that commit, boots the TV emulator and runs `tester/auto/round.sh` (`tester/auto/ci/run.sh`). The job is red when the round is RED.
3. The report: `https://raw.githubusercontent.com/tshi-dev/UrTV/data-screens/<run>/auto/report.md` (the run's name is in the job's summary), with the shots beside it and
   `metrics.tsv`, `steps.tsv`, `raw/`. The verdict is the last section of the report: `GREEN` or `RED` with its reasons.
4. A step the emulator cannot do is left out with `skip` (a workflow input) and shows as `SKIP` in the report, never as a pass.

## Status log

Newest first. One line per change of a gate: the date, the gate, the sha, the result.

- 27 Nov: gates 1 and 2 green on beta 4 `46dcad40`; beta 5 `194af9eb` (beta 4 + games-remote + the sports zones of six leagues) dispatched. Gate 3 found FAIL by `review_gate.py` (the review ends at `987aa209`).
  The emulator round (gate 4) written: `tester/auto/round.sh`, `tester/auto/ci/`, `.github/workflows/auto-round.yml`; first run to come.
- 27 Nov: the emulator round is started by a push of a new `work/auto-round-<shortsha>` branch (no force-push, the supervisor's rule); the first run, 46078178406, was started by the one force-push of `work/auto-round` before the rule was said, that branch stays unused.
