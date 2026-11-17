# The release checklist

The owner's rule (17 Nov 2026, after 4.3.4): **every new update is validated so nothing comes back.** Nothing reaches `master` (and so
`apk-latest`, which every installed TV updates from) until all six steps hold, **in this order, on the exact commit that will be merged**.
The release branch is `work/rel-<version>`; its tip sha is «the sha» below. A change after a step starts the steps again from step 1 (step 3
only for the changed files, see «A fix after the review»).

| # | Step | Who | Evidence | Stops the release when |
|---|---|---|---|---|
| 1 | **Checks green on the sha** | CI | `checks.yml` on the release branch push: `tests/run.sh` (about 15 min) | any failure |
| 2 | **Regressions group green** | CI, or `bash tests/run.sh regressions` | `regressions.yml` run by hand on the branch (Actions → «Regressions gate» → ref = the sha): every guard of `docs/dev/REGRESSIONS.md`, the gate's rules, the API surface diff in the run's summary | a failed guard; a new UI-thread path to a cross-process call |
| 3 | **Independent review, no blocker/high open** | a fresh reviewer (below) | `docs/dev/reviews/<version>.md`, `python3 .github/tools/review_gate.py` PASS | a blocker or high finding open, or accepted without the owner |
| 4 | **Tester: RELEASE-SMOKE PASS** | the supervisor, through the tester | `tester/RELEASE-SMOKE.md` on the `apk-next` build of the sha, the report's S-01…S-19 lines | any FAIL |
| 5 | **Owner OK** | the owner | their «OK» on the summary (steps 1–4, the API diff's new lines, the review's counts) | no OK |
| 6 | **master** | the supervisor | merge the sha, nothing else; android.yml runs checks and the gate (with step 3's check) again before it publishes | the publish stops by itself if 1–3 fail on master |

CI enforces 1–3 on master by itself: `android.yml`'s `apk` job needs `checks` and `regressions` (with `release: true`, which runs
`review_gate.py`); `promote-beta.yml` runs the gate (without the review) on the commit the promoted build came from. Steps 4 and 5 are
people; they are what CI cannot see (a remote's latency on a TCL, a word cut on a 720p panel).

## Step 2 in detail: what the gate checks

- `tests/regressions/run.py`: every test file named in `docs/dev/REGRESSIONS.md` (one row per bug ever fixed) and the gate's own rules:
  - `ui_thread_ipc_test.py`: no new UI-thread path (a key, a click, onResume/onStart, a screen's receiver such as the minute tick) to a
    cross-process call or the disk, against `ui_thread_baseline.txt`;
  - `key_handlers_test.py`: every key handler can say «not mine»; one that takes every key is listed with a reason;
  - `keep_list_test.py`: no list we turn packages off with names a package of `optimize/lists/keep.txt`;
  - `no_bundled_packs_test.py`, `legacy_guards_test.py`, `tester_guards_test.py`, `release_gate_ci_test.py`, `regressions_index_test.py`.
- `.github/tools/api_surface_diff.py` against the previous `v*` tag: new permissions, providers, exported components, key handlers, the
  work in onResume/onStart, and the UI-thread paths. **It fails on a new UI-thread path to a cross-process call**; the rest is for the
  reviewer and the owner to read (step 5 quotes its new lines).

A new UI-thread path that is really needed gets a line in `ui_thread_baseline.txt` **with a reason written by the reviewer**, in the
release's review, never a bare copy of the printed line. The right answer is almost always to move the work off the UI thread.

## Step 3 in detail: the independent review

Before each release (and again for any fix made after it), a **fresh** reviewer reads the full diff since the last release tag. Fresh
means a new Claude Code session or a subagent that did not write the release: it gets the diff and this prompt, not the authors' summary.

Start it from the release branch with the prompt below (replace `<version>`, `<tag>` and `<sha>`): `<tag>` is the newest `v*` tag on
master (`git tag --merged origin/master --sort=-v:refname --list 'v*' | head -1`), `<sha>` the release branch's tip.

```
You review UrTV release <version> before it reaches every installed TV. Read AGENTS.md first. Do not edit the code, do not push, do not
comment on GitHub. Read the FULL diff: git diff <tag>..<sha> (all of it, file by file; git log <tag>..<sha> for the intent), and the
code around each change. Also run: python3 .github/tools/api_surface_diff.py --base <tag> and bash tests/run.sh regressions.
Look for: anything on the UI thread that can wait (a provider query, IPC, disk, network) on a key, onResume or the minute tick; a key
handler that takes keys it does not use; a setting whose default changed; a list that turns packages off; anything that can break a
setup in progress or an installed TV on update (signatures, providers, permissions, stored data read by the new code); updates (the
channel, version.txt, promote-beta); the guards in docs/dev/REGRESSIONS.md that the diff weakens or removes; tests skipped or loosened.
Write docs/dev/reviews/<version>.md from docs/dev/reviews/TEMPLATE.md: one heading per finding,
"### F<n> [blocker|high|medium|low] [open] <title>", with the file:line, the concrete failure (what a person sees, on which TV), and the
smallest fix. blocker = a TV can be bricked, lose its setup or its updates; high = a feature most people use breaks or the remote
freezes; medium = wrong on some TVs or some settings; low = polish. No finding is «No findings.». Report the counts.
```

Then `python3 .github/tools/review_gate.py <version>`. Findings are fixed on the release branch by its authors (not the reviewer); each
fixed finding becomes `[fixed]` with the commit, a finding the owner decides to ship becomes `[accepted]` with «accepted by the owner»
and their reason. A blocker or high finding is never accepted silently.

**A fix after the review:** the reviewer reads `git diff <reviewed sha>..<new sha>`, adds what it finds, and moves the `Range:` line's
end to the new sha. `review_gate.py` fails while app code changed after the reviewed sha.

## Step 4 in detail: the smoke test

`tester/RELEASE-SMOKE.md`: 15 minutes on a TCL with the `apk-next` build of the sha; every row of the register a person can see is under
one of its 19 checks. The supervisor sends it to the tester session and keeps the report with the review.

## The LESSONS rules (R1–R25)

The supervisor's LESSONS.md: 77 problems, 25 rules. A machine checks these, in the regressions group:

| Rule | Checked by |
|---|---|
| R1 nothing that waits on the UI thread (onCreate, onResume, the tick, keys; subprocesses too) | `ui_thread_ipc_test.py` |
| R3 a key handler gives back keys it does not use | `key_handlers_test.py`, `tests/edit_keys_test.py` |
| R4 no focus listener scrolls by itself | `lessons_test.py` |
| R5 menus fit (partly: the app options box, dialogs) | `tests/options_fit_test.py`, `tests/dialog_fit_test.py`; the rest is smoke S-03/S-06 at 720p |
| R9 target API 34; no new accessibility service in the home | `lessons_test.py` |
| R10 the page's WebView 66 CSS never grows without a fallback; ES2017; no letter-spacing on Arabic | `lessons_test.py`, `tests/check.py`, `tests/arabic_spacing_test.py` |
| R11 a missing beta falls back; the newer of beta and latest | `tests/beta_channel_test.py`, `tests/beta_newest_test.py` |
| R12 no clean-up list touches keep.txt or the core | `keep_list_test.py`, `lessons_test.py` |
| R14 no content pack in an APK | `no_bundled_packs_test.py` |
| R15 apk-latest keeps Latest and its stable names, rescue included | `lessons_test.py` |
| R16 only master and next publish; a local build is versionCode 1 | `lessons_test.py` |
| R17 pinned runners, actions, npm and pip | `lessons_test.py` (open: fonttools, brotli and pillow are not pinned yet, printed as a NOTE) |
| R18 tv.html under its budget; market names generated | `tests/check.py`, `tests/market_names_test.py` |
| R21 no Wi-Fi name, MAC, IP, serial or e-mail in tester/ and docs/ | `privacy_test.py` (a PIN or a recovery code cannot be told from a number: AGENTS.md) |

The rest are this checklist; the reviewer (step 3) and the supervisor confirm each in the review file under «Rules»:

- [ ] **R2** no silent no-op: a feature that needs something missing says so on screen and offers the fix; a failed read keeps the last real answer.
- [ ] **R5** every hidden or special item keeps a reachable way in; every new menu checked at 720p and the large font (5 % safe area, 8 px scale).
- [ ] **R6** Google TV home: the add-on's HOME_KEY catch and the window watch; one bring per press; never a stock home over another app (4.4).
- [ ] **R7** never without a home: a rescue path, a warning before uninstall, a journal outside the app's data.
- [ ] **R8** the setup never turns off the stock home or the wizard that holds the Wi-Fi or cast credentials; protected packages stay on; a skip with a note, not `!!`.
- [ ] **R13** Checks ran on every head that reaches next or master (a green «Build APKs» only means it compiled).
- [ ] **R19** parallel branches merged in one integration worktree, both sides kept in shared registries (run.sh, the register, keep.txt); check.py with the real fonts and every test before a push to next.
- [ ] **R20** no push to master, branch delete or GitHub reply without the owner (signed «— فريق UrTV»); commits as tshi-dev, no AI lines.
- [ ] **R22** no tester or person names in branch names; merged branches deleted.
- [ ] **R23** a session stopped by its usage limit left one line of state in STEP.md.
- [ ] **R24** spend: Sonnet workers, one fix round per batch, no polling.
- [ ] **R25** tester: the TV muted; never the owner's PIN or accounts; no reboot or factory step while the owner uses the TV.

## When a bug is fixed (any time, not only at release)

In the same commit as the fix: a row in `docs/dev/REGRESSIONS.md` with the test that fails when the fix is reverted (revert it once and
see it fail), and, when a person can see it, its row id under a check of `tester/RELEASE-SMOKE.md`.
