# League page v2, phase (a): the full table (build plan, not built yet)

**Status:** a plan only. Building starts when the owner answers the six questions (§8 of
[LEAGUE-DESIGN-STUDY.md](LEAGUE-DESIGN-STUDY.md)). Until then it uses the safe defaults below. Any answer that differs changes one
switch or one line, never the structure.

**The design:** [3-table-full](../mockups/league-v2/3-table-full-1080p.png), and the table's columns in
[1-league-home](../mockups/league-v2/1-league-home-1080p.png).

## 0. Safe defaults (until the owner answers)

| Question | Default in phase (a) |
|---|---|
| Zones | **Only where sure.** Zones come from a hand-kept list. Each entry names its league **and season**, and the owner checks it. A league or season not in the list shows no zone bar and no legend. A new season shows no zones until it is added, so a stale rule never shows. |
| Live table | **Off by default.** The table is the finished games' table. A toggle «مع المباريات الجارية» shows provisional points (in red, with the minute). It appears only while a game of that league is live, and it resets when the page closes. |
| Form order | Newest **next to the points** (the start side of the form block). The archive's `f` is already newest first. |
| Digits | The person's setting through `ar()`, as everywhere. `LATIN` gives 0-9. |
| Home tab name, phase order | Not used by phase (a). |

## 1. What phase (a) changes

1. **Data (CI):** `sports/<id>.json` gains two fields:
   - `mv` per table row: places moved since the last round;
   - `zones` per file: only for leagues and seasons in the list.
2. **Page:** the league's «جدول الترتيب» tab becomes the full table. It has these columns:
   - rank, movement, badge, club, P, W, D, L, GF, GA, GD, Pts, last 5;
   - a zone bar on each zoned row, with a legend in words underneath.

   OK on a row opens the club (`mpClubView`, as today). The quick-view sheet comes in phase (d).
3. **Focused mode:** OK on the tab, or Down into the table, hides the page header so 18 rows fit without a scroll. Back shows the
   header again. A league with more than 20 clubs scrolls with the focus, as the list does today.
4. **The live toggle:** as in §0.

Not in phase (a): the league home, the live strip, the season strip, the club sheet, and home/away tables (phase a2, optional, below).

## 2. Data: `.github/tools/sports_files.py`

- **`table(games, names)`** stays. A new **`movement(games, names)`** works out the previous position:
  - It takes the highest round (`r`, numeric) that has a finished game.
  - It computes the table without that round's games.
  - It sets `mv = previous_rank - rank` on each row: positive is up, negative is down, `0` means the same place.
  - **No `mv` at all** when the rounds are not numeric, when the round is the first, or when the file is a cup's. The page then shows no
    arrow, not «–».
  - A club with no previous row (its first game) gets no `mv`.
- **Zones:** a new reviewed data file, `.github/tools/league_zones.json`:
  ```json
  {"4668": {"2026-2027": [{"from": 1, "to": 3, "k": "acle"}, {"from": 4, "to": 4, "k": "acl2"}, {"from": 16, "to": 18, "k": "down"}]}}
  ```
  - The kinds are a closed list, each with its Arabic and English words in the page: `acle` «دوري أبطال آسيا للنخبة», `acl2`
    «دوري أبطال آسيا ٢», `ucl`, `uel`, `uecl`, `po` «الملحق», `down` «الهبوط».
  - `league_file` copies the season's list as `zones`, and only when `season_now` matches a key.
  - A file whose ranges overlap, run past the club count, or use an unknown kind **stops the build** (a test checks it).
  - **The list ships empty.** Each entry is added in its own commit, with the source of the rule (the competition's regulations page)
    in the commit message, after the owner says yes. Rules from memory never go in.
- **Size:** `mv` is about 6 bytes a row and `zones` under 200 bytes. A league file grows by about 0.2 KB.
- `V` stays `1`: these are new fields, and an older page ignores them.
- **docs/MATCH-ARCHIVE.md:** one line on each field, in the `sports/<leagueId>.json` row.

## 3. Page: `src/template.html` («المباريات», `#matchpage=1`)

- **`mpLeagueView`, tab 0:**
  - The table group gets a new kind, `{key: 't', table: rows, zones, live}`. `mpRender` draws it as a `<ul class="mpt">` of rows
    (markup as in the mockup: `.tr`, `.rk`, `.mv`, `.bd`, `.nm`, `.c`, `.pt`, `.fo`), not as `mpItem` lines.
  - The rows stay the focusable list (`MP.zone = 'rows'`, `MP.r`), so the keys, the ring and the scroll are the ones the page has.
- **The fallback** when the archive has no table (TheSportsDB's `lookuptable`) keeps today's plain lines: it has no `f`, no `mv` and no
  zones. Nothing new is invented for it.
- **Form:** `f` from the file, drawn as squares (`.sqf`, a 6 px corner, white ف ت خ on the zone green / grey / zone red). The existing
  `formHtml` (coloured letters, no box) stays for the club view and the game card. A table row with no `f` shows no squares.
- **Movement:** ▲n in green, ▼n in red, «–» in grey only when `mv === 0`. Nothing when `mv` is absent.
- **Badges:** the same runtime badge the game rows load. Until it arrives, and if it never does, a circle with the club's first letter
  holds its place (no layout move when it loads: a fixed 34 × 34 box).
- **Zones:** `border-right` (`border-left` in `tv-en.html`'s LTR) with a 6 px bar in `--z1`, `--z2` or `--z3`, by kind:
  - continental places are green (`--z1`), the second continental tier blue (`--z2`), the relegation and playoff places red (`--z3`).
  - The legend shows only the kinds this table uses.
  - The tokens are added to both `#mp` looks, light and `#pagedark=1`, with the values in the mockup.
- **The followed club's row:** `--rowf` background and bold name, as the mockup shows. It also opens with the focus on it when the
  person follows a club of this league; otherwise the focus is on row 1.
- **The live toggle:**
  - It is a second tab row, «الكل» and «مع المباريات الجارية». It shows only while `fbGames()` has a live game of this league.
  - On, it adds each live game's current score to a copy of the rows (client side, 3/1/0 points, GD, GF), sorts again with the CI's
    order (points, goal difference, goals for, name), and marks those rows: red points and the minute after the name.
  - Off, which is the default, it shows the file as it is.
  - It never writes back to anything.
- **Focused mode:** a class on `#mp` (`mp-focus`) hides `header` and `#mp-days`. Up from row 1 or Back takes it off. The page's Back order
  stays: the card, then focused mode, then the view under it.
- **R10:**
  - The new CSS uses margins, not `gap`, and no `inset`, `clamp` or `min()`.
  - `unicode-bidi:isolate` on number runs (it has been in Chrome since 48).
  - No letter-spacing.
  - `tests/webview66_test.py` and `tests/arabic_spacing_test.py` already cover the page.
- **R5:**
  - At 720p the row is 29 px and the text 17 px.
  - With the large font, focused mode scrolls with the ring. The list already does this.
  - Check S-03/S-06 at 720p.
- **The clock never moves:** the live minute and points are fixed-width boxes, so a changing number never pushes the name.
- **Weight:** about 4 KB of CSS and 4 KB of JS. `tv.html` stays under 500 KB (`tests/check.py` measures it).
- **English (`tv-en.html`):**
  - New strings go in `i18n/en/page.tsv`: column heads, the legend words, the kinds, the toggle.
  - The layout mirrors through the existing LTR rules (`en_css`).

## 4. Tests (written first, kept in `tests/run.sh`)

| Test | What it proves |
|---|---|
| `tests/sports_files_test.py` (+) | `mv` from a made-up season: one club up 2, one down 2, the rest 0. No `mv` on round 1, with text rounds or on a cup. `zones` only when the season key matches; an overlapping, out-of-range or unknown-kind list raises. `league_zones.json` passes the same check. |
| `tests/football_page.test.mjs` (+) | The table tab draws 18 `.tr` rows with 13 cells. Zone classes only for the file's ranges, and no legend without `zones`. The form squares are newest first next to the points. The arrows match `mv`, and an absent `mv` draws nothing. OK on row 7 opens that club. Focused mode hides the header, and Up from row 1 / Back restores it. The toggle is absent with no live game. With a live 1:1 it adds a point to both clubs and re-sorts, and off it shows the file again. |
| `tests/matches_archive.test.mjs` (+) | The fallback (no archive table) still draws today's lines. |
| `tests/sports_hist.test.mjs` | A past season keeps its table and shows no movement or zones from the current season. |
| 720p screenshot (Playwright) | The 18 rows fit at 1280×720, and no row, cell or legend leaves the 5 % safe area. |
| `tests/webview66_test.py`, `tests/arabic_spacing_test.py`, `tests/check.py` | Unchanged; they must pass on the new CSS and on the size. |

Then `bash tests/run.sh` in full before the push (about 15 minutes, in CI where the fonts are).

## 5. Steps (one commit each, on `work/45-league-design`)

1. CI: `movement()` + its tests.
2. CI: `league_zones.json` (empty) + the check + its tests; the docs/MATCH-ARCHIVE.md lines.
3. Page: the table markup, the form squares, the movement, the badge placeholders, the zone bar and the legend (light and dark), and
   the English strings, with their tests.
4. Page: focused mode and the Back order, with their tests.
5. Page: the live toggle, with its tests.
6. `dist/` rebuilt from the template (`src/build.py`), 720p and 1080p screenshots in `docs/shots/league-v2/`, and a CHANGELOG line.
7. Each zone entry the owner approves: its own commit, with the regulation's URL in the message.

## 6. Phase a2 (optional, after a)

**Home and away tables** («على أرضه»، «خارج أرضه»):
- CI adds `hm` and `aw` per row: the same counts over home games only and away games only, with their own order. That is about
  60 bytes a row, about 1 KB a league.
- On the page they are two more entries in the toggle row.
- They are left out of (a) to keep the first step small.

## 7. Risks and how the plan meets them

| Risk | Answer |
|---|---|
| A wrong zone (rules change by season, cup winners take places) | Seasons as keys, an empty list at first, each entry sourced and owner-approved; no list = no bar. |
| Movement wrong when a round is spread over weeks or games are postponed | "Previous" is the table without the highest finished round's games. A postponed game played later counts in the round it belongs to. The test covers a postponed game. |
| The live table jumping on a weak TV | Off by default; rows redraw in place, no animation (`lowram` already cuts transitions). |
| A heavier page | About 8 KB; the size test stops the build over 500 KB. |
| Breaking a TV that is set up | The page reads new fields only when present. An older data file draws the table without arrows or zones, and an older page ignores the new fields. |
