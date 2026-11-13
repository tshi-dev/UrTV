# The match archive (4.3.1)

Every game of the leagues and tournaments UrTV follows, kept by CI on the branch `data-archive`, and small files a client reads
when a page needs them (a club's page, a head-to-head, a league's past results). Made by `.github/tools/match_archive.py`, run by
`.github/workflows/league-fixtures.yml`. This page is the contract a client builds against.

## Where

    https://raw.githubusercontent.com/tshi-dev/UrTV/data-archive/<path>

| Path | What | Size |
|---|---|---|
| `index.json` | every team and league in the archive, with names | ~40 KB |
| `teams/<teamId>.json` | a team's last 10 results and next 5 games | ~3 KB |
| `h2h/<lowId>-<highId>.json` | every finished meeting of two teams, newest first, at most 100 | ~1–10 KB |
| `results/<leagueId>.json` | a league's last 30 finished games, newest first («النتائج السابقة») | ~8 KB |
| `store/<leagueId>/<season>.json` | the store itself: every game of a league's season | ~50–150 KB |
| `health.json` | the last run: counts, size, problems (for maintainers, not for screens) | 1 KB |

Ids are TheSportsDB's (`idTeam`, `idLeague`), the same as `assets/shared/teams.json`, `data-leagues` and the screens use.
`<lowId>-<highId>`: the two team ids in numeric order (`133612-133613`, never `133613-133612`).

## Rules for a client

- **Unknown fields: ignore them.** New fields may appear in any file at any time without notice.
- **`v`** (every file but `health.json`) is the format, now `1`. It goes up only when a field changes meaning or goes away. A client
  that reads a `v` it does not know shows nothing from that file (as if it were missing), never a wrong value.
- **Missing file (404) = no data yet.** A team with no game in the archive has no `teams/<id>.json`; two teams that never met (in the
  archive) have no `h2h` file; a league with no finished game has no `results` file. Show the page's empty state, not an error.
  A file that exists may still have empty lists (`"last": []`, `"next": []`): same empty state for that list.
- **Strings, not numbers**: ids, scores (`"2"`) and rounds are strings, as TheSportsDB gives them. A score is `null` when not known.
- **Times** (`t`): UTC, `YYYY-MM-DDTHH:MM:SSZ`. Show them in the city's zone (ZoneFix on the TV).
- **Names**: `en` from TheSportsDB or teams.json; `ar` from teams.json (empty when there is none: fall back to `en`). The Arabic name
  of the other team in a game comes from `index.json` (`teams[id].ar`), else the game's own English name.

## Freshness and caching

- `updated` in a file is **when that file's content last changed**, not when CI last ran: a run that changes nothing in a file
  leaves it byte for byte the same (same stamp, same ETag on raw.githubusercontent.com). So a client may keep a copy and compare
  `updated` to know if anything is new.
- raw.githubusercontent.com caches for about 5 minutes. Ask a file again at most every 15 minutes while a page is open (the live
  run is every 15 minutes); a team page opened once a day needs one request.
- When the data changes: after every fixtures run (every 6 hours) and every 15 minutes while a game is in play or ended in the
  last 30 minutes. Mondays a clean-up rebuilds every derived file (stamps change only where content changed).

## The files

### index.json

```json
{"v": 1, "updated": "2026-11-13T15:02:10Z",
 "teams":   {"133612": {"en": "Manchester United", "ar": "مانشستر يونايتد", "leagues": ["4328", "4480"]}},
 "leagues": {"4328": {"en": "English Premier League", "ar": "الدوري الإنجليزي"}}}
```

`leagues` of a team: every league id it has a game in (in the archive). A team or league missing from `index.json` has no files.

### teams/&lt;teamId&gt;.json

```json
{"v": 1, "id": "133612", "en": "Manchester United", "ar": "مانشستر يونايتد", "updated": "2026-11-13T15:02:10Z",
 "last": [{"id": "2267081", "lg": "4328", "s": "2026-2027", "t": "2026-11-08T16:30:00Z", "home": false, "vs": "133600",
           "vsn": "Fulham", "gf": "1", "ga": "1", "st": "FT", "r": "11"}],
 "next": [{"id": "2267101", "lg": "4328", "s": "2026-2027", "t": "2026-11-22T15:00:00Z", "home": true, "vs": "133613",
           "vsn": "Manchester City", "gf": null, "ga": null, "st": "NS", "r": "12"}]}
```

- `last`: finished games (a final status and both scores), newest first, at most 10.
- `next`: games not started, not postponed, from 3 hours ago on, soonest first, at most 5. A game in play is in neither list.
- From the team's side: `home` (true when it plays at home), `vs`/`vsn` the other team's id and English name, `gf` its goals, `ga`
  the other's. Win/draw/loss = compare `gf` and `ga` as numbers.
- `st`: TheSportsDB's status (`FT`, `AET`, `PEN`, `NS`…). `r`: the round, may be empty. `s`: the season, `2026-2027` (or `2026`).
- `id`: TheSportsDB's `idEvent`; an id starting `d:` (`d:2026-09-20:133600:133612`) is a past game imported from openfootball
  (public domain), with no TheSportsDB event behind it: show it, but do not ask TheSportsDB about it. Its time is the local kick-off
  converted to UTC (12:00 local when the source had no time).

### h2h/&lt;lowId&gt;-&lt;highId&gt;.json

```json
{"v": 1, "pair": "133612-133613", "updated": "2026-11-13T15:02:10Z",
 "meetings": [{"id": "d:2026-09-13:133612:133613", "lg": "4328", "s": "2026-2027", "t": "2026-09-13T15:30:00Z", "r": "",
               "h": "133612", "a": "133613", "hn": "Manchester United", "an": "Manchester City", "hs": "0", "as": "1", "st": "FT"}]}
```

Finished meetings only, newest first, at most 100, every competition. `h`/`a` the home and away ids, `hs`/`as` their scores.

### results/&lt;leagueId&gt;.json

```json
{"v": 1, "id": "4328", "en": "English Premier League", "ar": "الدوري الإنجليزي", "updated": "2026-11-13T15:02:10Z",
 "results": [ /* games as in h2h, newest first, at most 30 */ ]}
```

### store/&lt;leagueId&gt;/&lt;season&gt;.json

```json
{"v": 1, "league": "English Premier League", "id": "4328", "season": "2026-2027", "updated": "…",
 "events": {"2267081": {"id": "2267081", "lg": "4328", "s": "2026-2027", "t": "…", "h": "…", "a": "…", "hn": "…", "an": "…",
                        "hs": "1", "as": "1", "st": "FT", "r": "11", "sp": "Soccer", "pp": false, "src": "tsdb"}}}
```

Every game of the season, keyed by `idEvent` (or the `d:` key), in kick-off order. `pp`: postponed. `src`: `tsdb` or `openfootball`.
For tools and for building new derived files; a screen should not read it (it is the large one).

## How the store is kept (for maintainers)

- Append-only and idempotent: a game is never removed (except seasons past the last 8 of a league, on every run); running again
  with the same input changes nothing. A newer copy replaces an older one unless it is less far on (finished > in play > not started),
  so a corrected score lands and a finished score is never undone.
- A backfilled game gives way to TheSportsDB's copy of the same game (same date and teams).
- A red run means the archive itself broke (an unreadable store, a store that shrank); nothing is pushed then. Problems that are not
  breakage are listed in `health.json` (`problems`).
- Backfill: dispatch «League fixtures» with `mode=backfill`. Names it could not match are in `backfill.json` on the branch; map them
  by hand in `.github/tools/archive_names.json` (`"openfootball name": "TheSportsDB id"`) and dispatch again.
- Requests: the archive build asks nothing; the live run asks one `lookupevent` per game in its window (at most 40 a run, 2.5 s apart)
  and nothing when no game is live.
