# Inventory (16 Nov 2026)

Every data branch of tshi-dev/UrTV, the workflow that writes it, its schedule, who reads it, and its size (files, bytes) on that day.
All of UrTV's data branches hold one commit, force-pushed by their writer. None of these writers needs a repository secret (TheSportsDB's
free key and Yahoo need none; pushes use the workflow's own `GITHUB_TOKEN`).

## Moved here (written by this repository's workflows; the old writers keep running for the apps already on TVs)

| dataset (urtv-data, branch `data`) | old branch / folder | old writer (UrTV) → new writer | schedule (UTC) | readers in UrTV | size |
|---|---|---|---|---|---|
| `football/leagues/` | `data-leagues/leagues/` | `league-fixtures.yml` jobs fixtures + tournaments (`league_fixtures.py`, `tournaments_fixtures.py`, `team_ids.py`) → `football.yml` | fixtures `23 */6 * * *`; tournaments `41 2,5,8,…,23 * * *` and after fixtures; UrTV also on a push of its files to master/next | page `src/template.html` `LEAGUE_BASE` (a file per followed league, `tournaments.json`), every 30 min; the archive job | 22 files, 49 KB |
| `football/archive/` | `data-archive/` | `league-fixtures.yml` job archive (`match_archive.py`) → `football.yml` job archive | after every fixtures run; live window `*/15 * * * *` (seconds and no request when nothing is live); clean-up `41 3 * * 1` (Mondays 03:41); backfill by hand | page `ARCH_BASE` on `work/433-matches` (not yet on a release) | 2,946 files, 6.3 MB |
| `football/catalogue/` | `data-catalogue/catalogue/` | `league-catalogue.yml` (`league_catalogue.py`, checked by `tests/league_catalogue_test.py`) → `catalogue.yml` | `41 2 * * 3` (Wednesdays), up to 5 h | screensaver `CatalogueLeagues.URL` (read by `SettingsActivity.fetchCatalogue` when «كل الدوريات» opens); page `CATALOGUE` on `work/433-matches` | 3 files, 203 KB |
| `games/`, `games/quiz/` | `data-games/games/`, `data-games/quiz/` | `quiz-bank.yml` (`make_quiz_bank.py`, `make_games_data.py`, data on `work/games-data`, tools from `next`) → `games.yml` | UrTV: a push of `games/**` to work/games-data or next, by hand; here daily `13 4 * * *` (a push to UrTV cannot start a workflow here) | screensaver `GameData.BASE` (each game's file, at most every few days), `QuizBank.URL` (`work/433-games` adds `quiz/rev.txt` and a v2 file) | 8 files, 667 KB |
| `markets/` | `data-markets/markets/` | `market-prices.yml` (`market_prices.py`, `market_loop.sh`) → `markets.yml` | `0,20,40 7-11 * * 0-4`, `0 12 * * 0-4`, `0,20,40 13-20 * * 1-5`, `0 21 * * 1-5`, `7,37 * * * *`; a run loops every 5 min to the hour's end while an exchange is open | launcher `MarketData.URL`, `DAILY_URL`, `FX_URL` (via `MarketFile`); page `MARKET_URL` | 4 files, 194 KB |
| `stations/` | `data-stations/stations/` | `stations-check.yml` (`check_stations.py`, `find_streams.py`, `aloula_probe.py`, ffmpeg) → `stations.yml` | UrTV: a push of the station list or the tools, by hand; here weekly `17 5 * * 1` | none in the apps: a report for people (the list itself ships in the launcher) | 5 files, 51 KB |
| `photos/` (metadata only) | `data-photo-packs/packs-built/index.json` | `photo-packs.yml` on `work/photo-packs` (by hand) → `seed.yml` (by hand) | by hand | none on a release: the screensaver reads `photos/packs.json` of its own app branch (`PackStore.INDEX_URL`); the zips are UrTV releases `photos-<id>` | 1 file, 3 KB (the covers and sheets, 1.4 MB, stay in UrTV) |

The generators read files of the app (the leagues it offers in `Prefs.java`, `assets/shared/teams.json`, `teams-ar.json`, `tournaments.json`,
`Places.java` and `cities-world.json` for the catalogue, the page's symbols in `src/template.html`, the launcher's `stations.json`). Here they
are taken from tshi-dev/UrTV at `APP_REF` (master) at the start of each run (`tools/inputs.sh`), so the data follows the app.

## Not moved (reports for review, probes, or inputs to the app's own source)

| branch | writer (UrTV) | schedule | what | size |
|---|---|---|---|---|
| `data-team-ids-<branch>` (master, next, work/sports) | `team-ids.yml` (`team_ids.py`, a matrix of one job per league) | `17 3 1 * *` monthly, a push to master/next | the clubs and their ids and Arabic names, for review; a person copies the result into `assets/shared/teams.json` (an app file) | 27 files, 296 KB |
| `data-teams`, `data-clubs` | older versions of the same (October) | — | superseded by data-team-ids-* | 3 files, 10 KB |
| `data-hadith`, `data-quran-verify`, `data-verses` | `hadith.yml`, `quran-verify.yml`, `verses.yml` | a push to master/next/content | verification reports of texts the app ships | 5 files, 1.9 MB |
| `data-catalogue-probe`, `data-club-probe`, `data-league-probe`, `data-globecast-probe` | the `*-probe.yml` workflows | a push to work branches, by hand | one-off probes for developers | small |
| `photos-raw`, `bg-unsplash`, `bg-candidates`, `bg-hq` | `photos.yml`, `unsplash.yml` | a push to master | candidate pictures for people to pick; the picked ones go into the app | 28 MB, 2 MB |
| `data-screens` | `screens.yml` | a push to work/design | screenshots from an emulator | — |

Cities: no CI job publishes them (the app ships `cities-world` in its assets); nothing to move.

## Apps

From UrTV 4.3.3 (branch `work/433-data`), the apps and the page read `https://raw.githubusercontent.com/tshi-dev/urtv-data/data/<folder>`
first and the old branch second, with one map (UrTV `docs/DATA-HOST.md`). Branches not merged yet that add readers (`work/433-matches`:
`ARCH_BASE`, `CATALOGUE` in the page; `work/433-games`: `QuizBank.URL_V2`, `URL_REV`) should call `fetchData` / `DataHost.get` as well.
