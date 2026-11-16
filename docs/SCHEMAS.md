# Schemas

The forms below are those of the files as UrTV's writers make them (the generators are copied unchanged). `str` ids are TheSportsDB's.
A field may be added to any object; none is removed or changes meaning without a new file name (docs/LAYOUT.md, Versions).

## index.json (v 1)
`{v, updated, branch, datasets: {<folder>: {v, about, cdn, updated, sha256, count, bytes, files: {<path in folder>: {sha256, bytes}}}}}`
— `datasets.<folder>.sha256` is the sha256 of its `files` object (sorted JSON); `updated` moves only when it changes.

## football/leagues/ (v 1)
- `<league-slug>.json`: `{league, id, updated, events: [<TheSportsDB event>]}` — the event objects as TheSportsDB gives them, reduced to
  the fields the page reads (`idEvent, idLeague, idHomeTeam, idAwayTeam, strHomeTeam, strAwayTeam, intHomeScore, intAwayScore, strStatus,
  strTimestamp, dateEvent, strTime, strSport, strSeason, intRound, strPostponed, …`). The slug is the page's `leagueSlug(name)`.
- `tournaments.json`: `{updated, names: {<league id>: <Arabic name>}, events: [<event>], wanted_not_found: [{en, ar}], tried: {<id>: {asked, answered, included}}}`
- `report.json`, `tournaments-report.json`: what each run asked and got (for people).

## football/archive/ (v 1, in each file)
Match row: `{id, lg, s (season), t (UTC time), r (round), h, a (team ids), hn, an (names), hs, as (scores), st (status)}`.
- `index.json`: `{v, updated, teams: {<id>: {en, ar, leagues}}, leagues: {<id>: {en, ar}}}`
- `results/<league>.json`: `{v, id, en, ar, updated, results: [<row>]}`
- `teams/<team>.json`: `{v, id, en, ar, updated, last: [{id, lg, s, t, home, vs, vsn, gf, ga, st, r}], next: [...]}`
- `h2h/<a>-<b>.json` (a < b): `{v, pair, updated, meetings: [<row>]}`
- `store/<league>/<season>.json`: `{v, league, id, season, updated, events: {<key>: <row>}}` — every game kept (8 seasons)
- `names.json`, `backfill.json`: openfootball names matched, the past seasons' games; `health.json`: the last run's counts and problems.

## football/catalogue/ (v 1)
- `catalogue.json`: `{updated, count, leagues: [{id, en, ar, country, country_ar, sport ("Soccer"|"Basketball"|"American Football"), top}]}`
  — the content rules (`tests/league_catalogue_test.py`) hold before it is published.
- `state.json`: where the weekly crawl stopped; `report.md`.

## games/ (v 1)
- `<game>.json`: each game's own form (UrTV `docs/GAMES-DATA.md`), e.g. `odd-one.json` `{v, t}`, `khanat.json` `{B, H, L, R, X, s: {<letter>: {<category>: [str]}}}`;
  each within its size cap and checked by UrTV's `make_games_data.py --verify`.
- `quiz/quiz.json`: `[{i (id), c (category), d (difficulty), k (answer index), q: [question ar, en], a: [answers], w: [[wrong answers]]}]`
  (UrTV `docs/QUIZ.md`), checked by `make_quiz_bank.py --verify`.

## markets/ (v 1)
- `markets.json`: `{updated, src, quotes: {<Yahoo symbol>: {p (price), v (previous close), c: [intraday closes], t}}}`
- `daily.json`: `{updated, daily: {<symbol>: {d: [closes], df, dl, dt}}}` (a year of daily closes)
- `fx.json`: `{updated, rates: {<ISO currency>: <units per US dollar>}}`
- `report.json`: what the run fetched.

## stations/ (v 1)
- `report.json`: `{checked, rows: [{name, url, hops, cleartext_blocked, probe, play, ok}]}`; `report.md`, `candidates.json|md`, `aloula.md` for people.

## photos/ (v 1)
- `index.json`: `{version, packs: [{id, name, name_en, source, source_en, cover, url (UrTV release zip), sha256, count, bytes}]}`
