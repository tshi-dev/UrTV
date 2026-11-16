# urtv-reports (private)

The setup reports UrTV's owners chose to send (the Google Form that `optimize/share.py` and the TV's `SetupReport.java` post to), synced
here every night, then cleaned and analysed.

```
apps-script/sync.gs, appsscript.json   the nightly sync, pasted into the form's responses sheet (docs/REPORTS-SYNC.md, Arabic first)
reports/YYYY-MM-DD.csv                 what the sync commits: row, timestamp, report (the raw text, as the form got it)
tools/clean.py                         reports/ → clean/reports.jsonl + clean/dropped.json
tools/analyse.py                       clean/reports.jsonl → Markdown and JSON: stops, causes, regressions, timings
tests/                                 the tools on tests/sample.csv (made-up reports, never real rows) and sync.gs under node
```

## Use

```
python3 tools/clean.py reports --out clean
python3 tools/analyse.py clean/reports.jsonl --md clean/analysis.md --json clean/analysis.json
```

- **clean.py** keeps only the lines UrTV's own allowlist lets into a report (the same `ALLOWED_LINE` as `optimize/share.py` and
  `SetupReport.java`; copy it again when they change) and the two free-text parts («Notes from the agent», «From the owner») scrubbed again
  (emails, phone numbers, IP and MAC addresses, file paths, links). It drops rows that are not a UrTV report, tests (an emulator, or words
  that only say "test"/«تجربة») and duplicates (the same report sent twice), and normalises the TV (maker, model, device, Android, SDK) and
  UrTV's version (`4.3.2`, its build, or `git-<sha>` with its commit date).
- **analyse.py**: stops by step × model × Android × version; causes (stop causes and the scripts' `!!` lines, normalised) as new (first seen
  on or after `--since`, default the newest report's day) or repeated; regressions (a step's stop rate up by 25 points or more against the
  version before, with at least `--min` reports, default 3); the median and 90th percentile of each finished step, and the setup's median
  minutes by version.

Both are Python standard library only. Tests: `python3 tests/test_tools.py` and `node tests/sync_gs.test.mjs` (also on every push,
`.github/workflows/checks.yml`).

## Rules

- Never a real row in tests, docs or commit messages; tests use `tests/sample.csv`, written by hand.
- The token lives only in the Apps Script's Script Properties; never in this repository, a chat or a log.
- A report's raw text stays in `reports/` (this repository is private); anything derived and shared elsewhere comes from `clean/`.
