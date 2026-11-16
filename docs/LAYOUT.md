# Layout

## Folders (branch `data`)

```
index.json                     every dataset: v, updated, sha256, count, bytes, and each file's sha256 and size
football/leagues/              <league-slug>.json (a week of games and the last day's results), tournaments.json, report.json, tournaments-report.json
football/archive/              index.json, results/<league>.json, teams/<team>.json, h2h/<a>-<b>.json, store/<league>/<season>.json,
                               names.json, backfill.json, health.json
football/catalogue/            catalogue.json, state.json (where the slow weekly crawl stopped), report.md
games/                         <game>.json (charades, khanat, letter-hive, missing-word, odd-one, word5-answers, word5-guesses)
games/quiz/                    quiz.json
markets/                       markets.json (prices), daily.json (a year of closes), fx.json (currencies), report.json
stations/                      report.json, report.md, candidates.json, candidates.md, aloula.md
photos/                        index.json (the packs: name, cover, release url, sha256); the pictures stay as UrTV's releases
```

The folder names under `football/` follow the old branches one to one (fixtures, results, live, teams, h2h and tables are the archive's
`results/`, `teams/`, `h2h/` and `store/`; there is no separate `live/`: the live window is written into the archive's files, as in UrTV).
Cities are not here: the app ships them (`assets/`), no CI job publishes them.

## Why a `data` branch of one commit, and not `main`

The writers commit often: markets every 5 minutes for 13 hours a weekday, the live window every 15 minutes, fixtures every 6 hours,
tournaments every 3. On `main` that is more than 50,000 commits a year of a 7 MB tree that changes everywhere at once (the archive's 2,946
files), and `main` must never be rewritten. So the data lives on its own orphan branch, `data`, which holds exactly ONE commit: every publish
makes a new parentless commit and replaces the branch (force push by the bot, `tools/publish.py` only). There is no public history of
snapshots (UrTV's data branches were the same: one commit each). `main` keeps only code and docs, committed by people.

## Many writers, one branch, no race

`tools/publish.py --put SRC:FOLDER ...` fetches the branch, takes its current tree, replaces only its own folders, rebuilds `index.json`, and
pushes with `--force-with-lease` on the commit it started from. If another writer pushed in between, the lease fails and it starts again
from the newer tree (8 tries). So a writer never drops another one's files and the workflows need no shared lock; each still has its own
concurrency group (as in UrTV) so two runs of the same writer never overlap:

| group | workflow, job |
|---|---|
| `football-fixtures` | Football: fixtures, tournaments (6-hourly, 3-hourly, by hand) |
| `football-archive` | Football: live window, weekly clean-up, archive/live/backfill by hand |
| `football-archive-write` | Football: the archive job, whoever started it |
| `football-catalogue` | League catalogue |
| `markets` | Markets |
| `stations` | Stations |
| `games` | Games data |
| `seed` | Seed from UrTV |

Nothing is pushed when no file changed (`index.json`'s times aside), and a dataset's `updated` in `index.json` moves only when its files do.

## Versions

Every dataset has a `v` in `index.json` (`tools/publish.py`, `DATASETS`). The files moved from UrTV keep their exact form (the apps on TVs
read the same bytes from either place); some carry their own `v` already (the archive's files, the games' files). A form made here carries
`"v"` in the file. A breaking change never changes a file in place: it gets a new name (or folder) with a new `v`, and the old file goes on
being written until the apps that read it are gone.
