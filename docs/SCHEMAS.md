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

## Hand-edited content (`content/` on main, published by content.yml after tools/check_content.py)

Rules the apps keep for all four: a file that cannot be read or has another `v` is ignored (the app works as without it); an entry whose
dates are over is ignored; what the person chose on the TV always wins.

### notices/announcements.json (v 1)
`{v, notices: [{id, text: {ar, en} (≤ 140 characters, no links), from, to (YYYY-MM-DD, the TV's day), min_app?, max_app? ("4.3.3"),
dismissible (true)}]}` — the home shows at most one notice at a time, never over playback, and each id once.

### devices/devices.json (v 1)
`{v, devices: [{id, match: {brand?, model?, device?, sdk_min?, sdk_max?} (Build.BRAND/MODEL/DEVICE, case-insensitive; at least one
of brand, model, device), defaults: {<setting>: <value>}, why}]}` — read at start; a default is applied only to a setting the person has
not set. Known settings: `sound_opens_general_settings` (bool; the home's «مخرج الصوت» opens the general settings). Any other is ignored by
the apps of today (a later app may read it); the weak-TV profile is decided by each app from the TV's memory, not from here.

### seasonal/seasonal.json (v 1)
`{v, packs: [{id, name: {ar, en} (≤ 40), source: {ar, en} (≤ 80), from, to, url (https://github.com/tshi-dev/UrTV/releases/download/photos-<id>/<id>.zip),
sha256, count (1..200), bytes (≤ 80 MB), cover? (https), license: {name, credit?}}]}` — like the photo packs,
offered between `from` and `to`; licensed pictures only (Unsplash, Pexels, CC0, CC BY with credit, or our own).

### prayer/adjustments.json (v 1)
`{v, adjustments: [{id, where: {country (ISO), city? (the city's saved name)}, from, to, minutes: {fajr|dhuhr|asr|maghrib|isha: -20..20} (the sunrise is never moved; ±20 is what the page and the home accept),
source}]}` — `city` is the city's name as the app saves it (e.g. «الرياض»). Added on top of the method's times on those days, the TV's city day (a city's entry wins over its country's); never over the person's own
adjustment of that prayer, and the times screen says that a published correction is applied.

## status/status.json (v 1)
`{v, updated, late: [<dataset>], datasets: {<dataset>: {updated, v, count, bytes, writer, last_good_run, run, late_after_h, late}}}`
