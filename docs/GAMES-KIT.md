# The family games' kit: how a game is built

Every family game but «الأسرع» is played with **one remote and no phones** (games/COMMON.md). The kit is what makes them one family in the «ورقة» look (design/43 `quiz-B*`): it was taken out of «الأسرع» (docs/QUIZ.md), which stays as it is, with its own pinned tests. The phone mode is a later phase: it will drive the same engines.

**Where:** package `sa.urtv.screensaver` (the saver app, `android/app`), beside the quiz, because the helpers every screen uses (`S`, `Dir`, `DigitsText`, `Prefs`, `Net`, `MiniJson`, `Bounded`) are package-private there. Every kit class starts with `Game`.

## The classes

| Class | Android | What it is |
|---|---|---|
| `GameRegistry` | no | the games in the list's order: id, Arabic name, one line, activity, `phones`, `ready`. **Adding a game is one line here.** |
| `GameScore` | no | 1 to 8 sides (teams or players), points, `last` (won since `newRound()`), shared places (`rank`, `ranked`), `tie()`, `nobody()`, `after(side)`; the eight default team names |
| `GameChips` | no | the quick setup: rows of chips and the start button, the focus (it starts on the button), `move`, `pick`, `setOff`, `save` / `load` |
| `GameClock` | no | a countdown on a clock the caller gives (a test uses a fake one): `start`, `pause`, `resume`, `leftSeconds`, `over`; 0 = no time limit |
| `GameDataCheck` | no | what every data file must pass first (size cap, really JSON of the expected kind) and the small checks (`text`, `whole`, `id`); `Parser<T>` is a game's own whole-file check |
| `GameData` | yes | the loader: the kept copy if it passes, else the bundled starter; the background fetch from `data-games` (docs/GAMES-DATA.md) |
| `GamePrefs` | yes | what is remembered: each game's chips, the game opened last, the sound switch, the last 100 item ids of a game. Never a name or a score |
| `GameLook` | yes | the colours: `paper, ink, tile, deep, tickOff, saff, onSaff, onDeep, onInk, ok, pink`, `ink(alpha)`, `SIDES[8]`; two sets, `PAPER` and `NIGHT` |
| `GameActivity` | yes | the base class a game extends: the screen, the keys, the pause card, the hand-over, and the drawing blocks |
| `QuizGamesActivity` | yes | the games list (the hub's «الألعاب» window): the registry as tiles |

## GameActivity: what a game writes and what it gets

A game implements:

    protected String id()                                   // its id in GameRegistry
    protected void draw()                                   // draw the current screen; the root is empty
    protected void onKey(boolean back, boolean ok, int dx, int dy)   // dx: toward the END of the line (already turned for Arabic); dy: down
    protected void onTick(long now)                         // optional: 10 times a second, not while the pause card or the hand-over is up
    protected void onPauseChanged(boolean paused, long now) // optional: stop and resume its GameClock
    protected void onEndRound()                             // optional: «إنهاء الجولة» was chosen on the pause card
    protected boolean inPlay()                              // optional: true during a round (HOME then leaves the pause card up)
    protected void onChips(GameChips c)                     // optional: a chip was picked (switch off what cannot be offered)

The base does, the same way for every game: the screen stays on; the language, direction and digits follow UrTV's settings (`en`, `rtl`, `latin`, `num(int)`, `digits(String)`); `tier` (Prefs.ramTier), `animate` (false on a low-RAM TV and with animations off) and `confetti` (also false on a mid TV); **Back never quits a round**: the game calls `openPause()` and the base draws the card (متابعة، إنهاء الجولة، الرئيسية) and takes the keys until it closes; `redraw()` draws again.

Everything is placed on the **1920 x 1080 design**, scaled to the screen; the safe area is 96 px on every side (5 %), spacing in steps of 8, square corners only.

| Block | Call |
|---|---|
| place a view | `at(w, h, x, y)` (x from the start edge: the right in Arabic), `atEnd(w, h, x, y)`, `px(v)` |
| words | `text(s, size, color, display)`, `centered(…)`, `clip(s, size, maxW)`, `measure(s, size)`, `sizeFor(s, big, mid, small, a, b)` |
| shapes | `rect(fill)`, `box(fill, stroke, strokePx, dashed)`, `stamp(…)` (a container) |
| title block | `title(big, small)` (200 px), `band(h)` |
| the question block | `question(meta, words, h)`: the green block, a saffron line («السؤال 3 من 10») and the big words (104 / 88 / 72 by length) |
| the tick timer | `new TickTimer(seconds, ticksY)`, then `set(secondsLeft)` on every tick: the saffron box with the number, one tick a second, the last five coral with the tick sound |
| a hidden word | `blanked(words, "___", word, hint, shown)`: the statement with a dashed box as wide as the word (the hint letter inside), or the word in saffron; give it to `question` |
| an answer stamp | `answer(tab, words, STAMP \| STAMP_FOCUS \| STAMP_RIGHT \| STAMP_WRONG)`: the letter tab is drawn on the glyph's ink (the quiz's `InkLetter`); `letter(i)` gives أ ب ج د or A B C D |
| the sides' bar | `sidesBar(score, names, turn, y)`: colour square, name, points; the side whose turn it is in ink |
| rank rows | `board(score, names, withLast)`: the place as a big numeral (saffron for first), «+10», the total; shared places |
| the end | `podium(score, names, sel)`: «انتهت اللعبة», the winner / a tie / nobody, the buttons `endLabels()` (العب مرة أخرى، تغيير الإعدادات، خروج), three blocks (512 / 400 / 300), confetti |
| buttons | `button(label, focused)`, `buttons(labels, sel, w, y)`, `keyButtons(sel, n, ok, d)` (the new focus, or `-1 - index` when OK chose) |
| the bottom line | `hints("▲ ▼", "السطر", …)`: pairs of key and what it does, so every screen says its next key in words |
| quick setup | `setup(chips, startLabel)` draws up to seven rows and the start button; `keySetup(chips, back, ok, dx, dy)` returns true when OK is pressed on the button; `soundChip(chips)` adds «الصوت»; `remembered(chips)` loads last time's |
| the hand-over | `handOver(name, secret, note, done)`: «سلّموا الريموت لـ …» and «الباقين: غطّوا عيونكم», OK, a 2 s count, the secret alone, OK hides it and runs `done`. Back hides it first; it hides by itself after 20 s and when the app leaves the front |
| sounds | `play(QuizSound.TICK \| CHIME \| LOW \| FANFARE)`: the existing tones, behind the one sound switch |
| names | `teamNames(n)`, `gameName()`, `toast(s)` |

Colours come only from `L` (`L.paper`, `L.ink`, `L.deep`, `L.saff`…): a game writes no colour of its own (the test refuses a `0xFF……` literal in a game's activity).

## How to add a game

1. **The rules**, a pure-Java class (`<Name>Engine`, no Android, no `S`): phases, whose turn, points through a `GameScore`. Randomness comes in as a `Random`, time as a `long`, so a test is exact.
2. **The data** (if any): a parser `GameDataCheck.Parser<T>` that returns null for anything outside the limits of docs/GAMES-DATA.md, one for a download and one for the starter (the same form, a lower floor of items). Load with `GameData.load(this, "<file>", PUBLISHED, STARTER)` and `GameData.refresh(this, "<file>", PUBLISHED, BYTES_MAX)`.
3. **The screen**: `public class <Name>Activity extends GameActivity`. Keep one `enum Screen` and a `draw()` that switches on it; start on the setup (`setup(chips, S.t("ابدأ"))`), so tile, OK, OK starts a round. Every word through `S.t("…")` with its English in `i18n/en/saver.tsv`; words picked from a list are marked with `S.n` and shown with `S.tv`.
4. **The manifest** (`android/app/src/main/AndroidManifest.xml`), beside the other games: `android:exported="false"`, `android:screenOrientation="landscape"`, the quiz's `configChanges`.
5. **The registry**: set `ready` to true on the game's line in `GameRegistry` (the name and the line are already there and already marked for English in `QuizGamesActivity.WORDS`; change both places together).
6. **The tests**: add the engine to `ENGINES` in `tests/games_kit_test.py` and its checks to `tests/GamesKitTest.java` (or a test of its own, listed in `tests/run.sh` under the games block). `KhanatActivity` and `MissingWordActivity` are the two examples: one with a clock and marking, one with items, turns and a verdict.

## The test pattern

`python3 tests/games_kit_test.py`: compiles the kit's pure classes and every engine with `javac` alone (so nothing of Android can slip in), runs `tests/GamesKitTest.java` (a fixed `Random`, a fake clock, the bundled starters as the data), then pins the Android side by reading the source: the registry against the list's marked words and the English table, a ready game's activity and manifest entry, Back and the pause card, the hand-over, low-RAM and reduced motion, the data loader's order, no «·» or «•», no rounded shape, no colour literal in a game.

## Decisions and open points

- **Dark and light.** `GameLook` has the two sets and every block draws from it, but `GameLook.of` gives `PAPER` to every UrTV look for now: «الأسرع» has shipped on paper under the dark home, and the owner approved that look. Switching the dark looks to `NIGHT` is that one line (the quiz keeps its own constants and would stay on paper).
- **«الأسرع» is not refactored onto the kit.** Its drawing code is pinned line by line in `tests/quiz_test.py` and `tests/step52_test.py`; the kit reuses its `InkLetter`, `CheckView`, `ConfettiView` and `QuizSound` instead of moving them. It is in the registry like the others.
- **The games list** keeps the class name `QuizGamesActivity` (the hub and its tests point at it).
- **Seven rows of chips at most** fit the setup screen (from the seventh the rows are 76 px instead of 88); a game with more settings groups them.

## What needs a real TV

The look at 3 m with the real fonts (the kit loads `fonts/ThmanyahDisplay-Bold.otf` and `fonts/Thmanyah-Medium.otf`); the focus on every screen with the remote; the hand-over's timing with real people; the tick and the fanfare through the TV's speakers; a 1.5 GB TV (no animation) and a 2.4 GB one (no confetti).
