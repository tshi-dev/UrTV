# 4.5 honours: champions per season and top scorers (phase 1: research)

Phase 1 is research and data only. There is no UI and no app change in this phase. The owner sees the mockups before any code is written.

The logic that names a champion from our archive is in `.github/tools/honours_study.py`, tested by `tests/honours_study_test.py`.
The research was run on 2026-11-18. The numbers below are a snapshot from that day.

## 1. Champions

### 1.1 From our archive (2021-22 on)

Rules (in `honours_study.py`):

- **A league:** the top of the final table, said only when the season is surely over. That means:
  - every game is played;
  - the last game is at least 21 days old;
  - the archive holds at least 90% of a full double round-robin.

  A league with a split or play-offs, or one whose games the archive lacks, says nothing. That is safer than a wrong champion.
- **A cup:** the final's winner, with both legs of a two-legged final added up. A level final says nothing, because the archive has no penalties or away goals.
- **MLS:** the table leader is the Supporters' Shield, not the champion (the champion is the MLS Cup, a play-off). It is never called champion.

Result: 52 seasons named, every finished season the archive can prove.

| league | 2022-23 | 2023-24 | 2024-25 | 2025-26 |
|---|---|---|---|---|
| Premier League | Man City | Man City | Liverpool | Arsenal |
| Bundesliga | Bayern (level on points with Dortmund, goal difference) | Leverkusen | Bayern | Bayern |
| Serie A | Napoli | Inter | Napoli | Inter |
| Ligue 1 | PSG | PSG | — games missing (307/342) | — 1 game left in the archive |
| La Liga | Barcelona | Real Madrid | Barcelona | Barcelona |
| Eredivisie | — games missing | — games missing | PSV | PSV |
| Süper Lig | Galatasaray | Galatasaray | Galatasaray | Galatasaray |
| Primeira Liga | Benfica | Sporting | Sporting | Porto |
| Botola (Morocco) | FAR Rabat | Raja | — games missing | — games missing |
| Qatar Stars League | Al-Duhail | Al-Sadd | Al-Sadd | — games missing |
| Saudi Pro League | Al-Ittihad | Al-Hilal | Al-Ittihad | Al-Nassr |
| UAE Pro League | Shabab Al-Ahli | Al-Wasl | Shabab Al-Ahli | Al-Ain |
| Egypt | Al-Ahly | — 1 game left | — games missing | — games missing |
| Iraq | Al-Shorta | — 1 game left | — 1 game left | — 7 games left |
| Saudi First Division (Yelo) | — | — | Neom | — 1 game left |
| Champions League | Man City | Real Madrid | PSG | — final level 1-1 (penalties) |
| AFC Champions League (Elite) | Urawa (2022, 2 legs) | Al-Ain (2023, 2 legs) | Al-Ahli | Al-Ahli |
| King Cup | — | — | Al-Ittihad | — no final in the archive |
| Copa del Rey, Coupe de France, Nations League | the archive holds only 2026-27 (no final yet) | | | |
| MLS | never (table leader only) | | | |

"1 game left" is nearly always one game the source never closed (postponed and never updated). The proposed job lists these
seasons in its report so that a person can see them. It never fills them in by guessing.

### 1.2 Cross-check: Wikidata (CC0) and ar.wikipedia

**Against Wikidata** (`P1346` winner on the season item, `P3450` season of league):

- 28 agree;
- **0 disagree**;
- 13 have a season item with no winner (Süper Lig 2022-23, 2024-25, 2025-26; Portugal 2023-24, 2024-25; Saudi 2024-25; UAE ×3; AFC 2022 and 2025-26; Yelo 2024-25; King Cup 2024-25);
- 11 have no season item at all (Morocco, Qatar, Egypt, Iraq, Süper Lig 2023-24, UAE 2023-24, AFC 2023 and 2024-25).

**Against ar.wikipedia** (the «البطل» field of the season infobox):

- every value read agrees with the archive: EPL, Bundesliga, Serie A, Ligue 1, La Liga, Eredivisie, Süper Lig 2025-26, Saudi ×4, UAE 2024-25, Yelo, King Cup;
- no field: UCL, AFC, Portugal, Bundesliga 2025-26, Serie A 2023-24;
- no Arabic article: Süper Lig 2022-23 and 2024-25, Portugal 2025-26.

**Disagreements recorded: none.** The 4 found on the first run were missing Wikidata labels in a batch fetch, not real disagreements.

### 1.3 The longer honours list (Wikidata)

Season items per league, with a winner, from Wikidata on 2026-11-18. QIDs are in `docs/research/honours/map.json`.

| league | season items | with a winner | note |
|---|---|---|---|
| Premier League | 36 | 34 | from 1992-93 (the First Division is a different item) |
| Bundesliga | 64 | 63 | |
| Serie A | 125 | 124 | |
| Ligue 1 | 95 | 87 | |
| La Liga | 98 | 96 | |
| Eredivisie | 138 | 135 | |
| Süper Lig | 69 | 61 | recent seasons have no winner |
| Primeira Liga | 103 | 93 | |
| MLS | 36 | 26 | the MLS Cup winner |
| Champions League (incl. European Cup) | 96 | 71 | |
| Copa del Rey | 125 | 124 | |
| Coupe de France | 110 | 108 | |
| Nations League | 15 | 7 | |
| Botola | 72 | 63 | |
| Qatar Stars League | 63 | 26 | thin |
| Saudi Pro League | 53 | 47 | from 1976-77; recent seasons partly blank |
| UAE Pro League | 52 | 10 | thin |
| AFC Champions League | 43 | 39 | |
| Egyptian Premier League | 73 | 22 | thin |
| Iraqi Premier League | 52 | 42 | |
| Saudi First Division | 33 | 5 | thin |
| King Cup | 13 | 8 | thin; the cup is older than its items |

Some winners in Wikidata are "unknown value" (a blank node). Those are left out, not guessed.

**What this means:**

- The European leagues, the cups, Morocco, Iraq and Saudi have a usable list of past champions in Wikidata, which is CC0: no credit is required, though we give it.
- Qatar, UAE, Egypt, Yelo and the King Cup are thin. For them the list would be our archive only (2021-22 on), until Wikidata fills in.
- Wikidata alone lags the latest seasons by months (Süper Lig, Portugal and Saudi 2024-25 have no winner yet). The archive is fresher. The two together cover the recent seasons that each one alone misses.

## 2. Top scorers: sources and coverage

### 2.1 Sources

| source | scorers? | licence | usable |
|---|---|---|---|
| **openfootball** football.json (and its .txt repos) | **no**: 0 matches with a scorer in every league and season 2019-20 to 2025-26; the .txt files have no goal minutes either | CC0 | no (nothing to use) |
| **Wikidata** season item `P3279` top scorer (qualifiers `P54` team, `P1351` goals), or `P12475` | yes, but only for a few leagues (see 2.2) | CC0 | **yes** |
| **en.wikipedia** season infobox «league topscorer» | yes, nearly every season of every league | CC BY-SA 4.0 | yes, with credit and share-alike (2.3) |
| **ar.wikipedia** season infobox «هداف الدوري» | yes, where the Arabic article exists | CC BY-SA 4.0 | yes, with credit and share-alike (2.3) |
| **TheSportsDB** free key | not on the free tier as far as we know: lookuptable covers only some leagues, and the player and timeline calls are premium | its own terms | **unverified**: thesportsdb.com is unreachable from this environment, so its terms page could not be read. Do not rely on it until a person reads the terms |

### 2.2 Coverage: league × season × source (2021-22 to 2025-26)

Read on 2026-11-18 from each season's Wikidata item and its ar and en Wikipedia articles, through the item's sitelinks.

Key:
- **WD**: Wikidata `P3279` (CC0);
- **ar** / **en**: the article's infobox field «هداف الدوري» / "league topscorer" (CC BY-SA 4.0);
- **item, none**: the season item and article exist, but neither field;
- **·**: no Wikidata season item. The article may exist, but it was not found this way. The job would look it up by title.

MLS is a calendar year: its "2021-22" column is 2022, and so on.

| league | 2021-22 | 2022-23 | 2023-24 | 2024-25 | 2025-26 |
|---|---|---|---|---|---|
| Premier League | WD ar en | WD ar en | WD ar en | WD en | en |
| Bundesliga | ar en | en | ar en | ar en | en |
| Serie A | ar en | ar en | item, none | ar en | en |
| Ligue 1 | ar en | en | ar en | ar en | en |
| La Liga | ar en | WD ar en | ar en | ar en | ar en |
| Eredivisie | en | en | en | ar en | ar en |
| Süper Lig | en | en | · | en | ar en |
| Primeira Liga | · | item, none | en | en | en |
| MLS (calendar year) | en | en | · | en | ar |
| Champions League | en | WD en | WD en | WD en | WD en |
| Copa del Rey | en | en | · | · | · |
| Coupe de France | ar en | · | · | · | · |
| Nations League | en | WD en | · | WD en | · |
| Botola | · | · | · | · | · |
| Qatar Stars League | ar en | · | · | · | · |
| Saudi Pro League | ar en | ar en | ar en | ar en | ar en |
| UAE Pro League | ar | en | · | ar en | ar en |
| AFC Champions League | en | · | · | · | en |
| Egypt | ar en | · | · | · | · |
| Iraq | ar en | · | · | · | item, none |
| Saudi First Division | · | ar en | ar en | ar en | ar en |
| King Cup | · | · | ar | ar | ar |

What the table shows:

- **Wikipedia covers nearly everything that has an article.** en has the scorer in nearly every season of the European leagues, the Saudi leagues, UAE and the UCL. ar has the field in most Saudi, Spanish, German, Italian and French seasons.
- **Wikidata is thin.** Its top scorer is only there for the EPL to 2024-25, La Liga 2022-23, the UCL and the Nations League. On its own it is not enough.
- **The gaps** are Morocco, Qatar, Egypt and Iraq after 2021-22, and the cups. There is no season item, so the articles were not reached this way. A lookup by title can fill some of them (ar articles are named «الدوري المصري الممتاز 2023–24» and so on).
- **ar and en disagree** in 2 seasons:
  - UAE 2024-25: ar 20 goals, en 16;
  - Iraq 2021-22: ar 15, en 22.

  In Ligue 1 2024-25, ar names Dembélé alone (with a note) and en names Dembélé and Greenwood level. The job must **publish a season only when the sources agree** (both articles, or an article and Wikidata), on both the name and the goals. A season with one source only is published when the field is plain (a name and a number). Every disagreement goes to the report, never to the screen.
- **Arabic names:** where the ar article has the field, its link text is the Arabic name (حمد الله، إيغالو، كينيونيس…). Otherwise it is the player's Wikidata Arabic label. With neither, the Latin name.

The raw rows are in `docs/research/honours/wp_scorers.json`.

### 2.3 Licence: what Wikipedia needs from us

Wikipedia text is CC BY-SA 4.0. Its reuse page asks for four things:

1. credit by a link to the article(s);
2. a notice of the licence with its link;
3. the same licence on what we change or build from it (share-alike);
4. a note that it was changed.

A top scorer's name and goal count is a bare fact, and facts are not copyrightable in most places. But the infobox is a selection,
and the owner's rule is "everything sourced". So we treat it as CC BY-SA and credit it. That costs little:

- **urtv-data `DATA-LICENSE.md`** gets one row:
  `| Wikipedia (ar, en) | football/scorers/ | CC BY-SA 4.0; each season file names its article(s) |`.
  The `scorers/` folder is then CC BY-SA 4.0 as a whole, which is the share-alike. Our CC BY 4.0 elsewhere is unchanged.
- **Every season file** carries `src` (the article URLs, ar and en) and `lic: "CC BY-SA 4.0"`. The job records a change ("name from en, put into Arabic") in `note`.
- **On screen**, under the «الهدّافون» list, there is one small line: «المصدر: ويكيبيديا (CC BY-SA 4.0)». English: "Source: Wikipedia (CC BY-SA 4.0)".
  A TV cannot follow a link, so the URL lives in the file and on the phone page's «المصادر» list, which is linkable.
- Wikidata values (CC0) need no line, but the same line covers them when both are used.
- Champions from our archive are our own data (CC BY 4.0, from TheSportsDB's results). The Wikidata history is CC0. The champions list needs no new line.

## 3. Proposal

### 3.1 Which sources, honestly

- **Champions:**
  - the archive for 2021-22 on (our rules, sure seasons only);
  - Wikidata `P1346` for older seasons and to fill gaps;
  - where both have a season, they must agree. A disagreement is never published: it goes into the job's report for a person to check.
- **Top scorers:**
  - Wikidata `P3279`/`P12475` first (CC0);
  - otherwise the season's Wikipedia infobox: the Arabic article first for the Arabic name, and the English article for the goal count when the Arabic has none;
  - TheSportsDB not until its terms are read;
  - openfootball has nothing.
- **Names in Arabic:** the club's Arabic name from the archive's existing names (`ar` in `teams/`); a player's name from the ar.wikipedia link text or the Wikidata Arabic label. With neither, the Latin name is shown, never a machine transliteration.

### 3.2 The CI job (in urtv-data)

- `.github/workflows/honours.yml`: weekly (Tuesday 03:17 UTC) and on demand. Actions are pinned to a sha, like the repo's other jobs.
- `tools/honours.py`: the logic in `honours_study.py` (moved there and tested there), plus:
  - the Wikidata API (`wbgetentities`), not WDQS SPARQL, because the query service limits a client to about one query a minute;
  - the Wikipedia API (`action=parse`, section 0), spaced 3 s apart and honouring `retry-after`;
  - a user agent that names the project and its repository URL, as Wikimedia asks.
- Each season is written only when it is sure. A file is never emptied because a source failed: the last good file stays.
- The weekly report lists the seasons with no champion, the disagreements, and the "1 game left" seasons.
- Size: about 22 leagues × about 100 seasons of champions in 22 small files, plus about 110 scorer files a year.

### 3.3 Data files (`football/` on the data branch)

`champions/<lg>.json`:

```json
{ "v": 1, "id": "4668", "updated": "…",
  "seasons": [ { "s": "2025-2026", "id": "136014", "en": "Al-Nassr", "ar": "النصر", "src": "archive", "wd": "Q…" },
               { "s": "1977-1978", "en": "Al-Ahli", "ar": "الأهلي", "src": "wikidata", "wd": "Q…" } ] }
```

- `id` is TheSportsDB's club id when the club is in our archive. It is what opens the club page.
- `src` is `archive`, `wikidata` or `both`. Newest season first.

`scorers/<lg>/<season>.json`:

```json
{ "v": 1, "id": "4668", "s": "2023-2024", "updated": "…",
  "top": [ { "en": "Cristiano Ronaldo", "ar": "كريستيانو رونالدو", "team": "136014", "goals": 35 } ],
  "src": ["https://ar.wikipedia.org/wiki/…", "https://en.wikipedia.org/wiki/…"], "lic": "CC BY-SA 4.0" }
```

- `top` is a list: a shared top scorer has two or more entries. The infobox gives only the top scorer, not a top 10.
- A top-10 list would need per-match scorers. No free licensed source has them (2.1).

A club's titles for its page come from the champions files. No extra file is needed: the page gathers the club's id across the leagues it follows.

### 3.4 The UI: mockups for the owner (no code until the owner says yes)

The mockups are drawn on the real page (`dist/tv.html`, its own styles), with sample data taken from the research. They are in
`docs/shots/honours-mock/`. The fonts are stand-ins, because the real font files are not in this checkout.

The season picker is the page's own side column, not a new control. The remote already goes right into it and up and down in it,
so it needs no new key. There are 8 seasons: the current one first, marked «الحالي».

| # | view | file |
|---|---|---|
| 1 | League page, «جدول الترتيب» of a past season: the final table, the champion said on its row | `1-league-past-table-1080p.png`, `-720p.png` |
| 2 | League page, «الأبطال»: newest first; the archive's seasons with points and margin; older seasons from Wikidata with the name only; the current season «لم يُحسم بعد» | `2-league-champions-1080p.png`, `-720p.png` |
| 3 | League page, «الهدّافون» of a season, with the source line | `3-league-scorers-1080p.png` |
| 4 | A shared top scorer (EPL 2021-22: Salah and Son, 23 each) | `4-epl-scorers-shared-1080p.png` |
| 5 | Club page, a third group «البطولات»: each competition, its latest title, and how many before it **that we know** | `5-club-titles-1080p.png`, `-720p.png` |
| 6 | English | `6-league-scorers-en-1080p.png` |

Four choices for the owner:

1. **A season with no champion yet:** show «لم يُحسم بعد», or leave the row out?
2. **Title counts on the club page:** Wikidata has gaps. Al-Hilal has 16 Saudi league titles in it, but its real total is higher, because 2019-20 to 2021-22 have no item or no winner. The mockup says «و١٥ موسمًا قبله نعرفها» rather than a total that would be wrong. Is that acceptable, or should the club page show only the latest title until the gaps are filled? (Filling them from Wikipedia's lists of champions is possible, under CC BY-SA.)
3. **Older seasons in «الأبطال»:** name only. Wikidata has no points.
4. **The source line:** small, under the list, only on «الهدّافون» and «الأبطال». Is that its place?

## 4. Not done in this phase

- No UI, no app change, no urtv-data job yet: proposal only.
- The TheSportsDB terms were not read (the site is unreachable from here).
- The CC BY-SA 4.0 licence text itself could not be fetched (creativecommons.org is unreachable). The four points in 2.3 are Wikipedia's own reuse page.
- Seasons that miss games in the archive (Ligue 1 2024-25, Eredivisie 2022-24, Morocco, Egypt, Qatar 2025-26) have no champion from the archive. Wikidata fills some of them.

## 5. Built (4.5), after the owner's answers

The owner's answers to the four choices:

- (a) «لم يُحسم بعد» for the season under way, and OK on it opens its table.
- (b) No title count until a league's list is complete. Until then the club page shows the latest title only.
- (c) Older champions by name only.
- (d) The source line under the list, and the scorers rule above.

What was built:

| part | where | what |
|---|---|---|
| a league's seasons and champions | `.github/tools/sports_files.py` (every archive build) | `sports/<id>.json` gets `seasons` (newest first) and `champs` (each season the archive is sure of: `honours_study.champion`, with points, the margin and the runner-up, or a cup final's score). `sports/<id>/<season>.json` is each past season: its final table, every result, its champion. |
| the weekly honours | `.github/tools/honours.py`, `.github/workflows/honours.yml` (Tuesdays, pinned, branch `data-honours`) | `champions/<id>.json`: the archive's, Wikidata's and (for a season Wikidata has no winner of) the season article's champion. Two sources must agree, or the season is left out (`held`) and goes to `report.json`. `complete` and `titles` are there only when the list has no gap from Wikidata's first season. `scorers/<id>.json`: the finished seasons' top scorers (the last 8), published only when the sources agree. `LICENSE.md` says the terms. |
| the page | `src/template.html` (`hon*`, one block; the league and club pages call it) | The side's seasons; a past season's table and results; «الأبطال»; «الهدّافون» with «المصدر: ويكيبيديا وويكي بيانات (CC BY-SA 4.0)»; the club page's «البطولات». Self-contained, for the league page's redesign. |

The scorer files are one file per league, with the seasons inside it, rather than the proposed `scorers/<lg>/<season>.json`. The page shows 8
seasons, and one download is better than eight on a TV.

**urtv-data.** This session could not be given urtv-data, so the job runs here and publishes the branch `data-honours`, which the page
reads. Moving the job there means:

1. copy `honours.py`, `honours_study.py` and the workflow, and publish to `football/honours/`;
2. add `['data-honours/', 'football/honours/']` to the page's `DATA_MAP` and the apps' DataHost map together (tests/data_host_test.py keeps them the same);
3. add this row to its `DATA-LICENSE.md`:
   `| Wikipedia (ar, en) / Wikidata | football/honours/scorers/, a champion whose "src" says wikipedia | CC BY-SA 4.0 (share-alike); each season names its articles |`

Tests:
- `tests/sports_files_test.py`: the season files and the champions;
- `tests/honours_test.py`: the infobox reading, the cross-check, `complete`, the agreement rule, a failed source, the workflow;
- `tests/honours.test.mjs`: the page in a browser, Arabic and English, 720p.
