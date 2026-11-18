# Content packs of the games (4.4)

Anything big (a large bank, emoji or picture sets, later audio) is a **downloadable pack**, not part of the APK. The small banks that make a game work offline stay in the app (`assets/games/*.gb`).

## Where and what
- A pack is one `GameBundle` file (docs/LICENSE-CONTENT.md: compressed, lightly encrypted) in the public data repository: `urtv-data` at `games/packs/<id>/pack.gb`, listed with its version, size and sha256 in `games/packs/index.json` (`{"v":1,"packs":[{"id","ar","en","ver","bytes","sha"}]}`). Packs are split small, one per game or theme.
- Build one: `python3 games/tools/pack_bundle.py <id> source.json pack.gb`, then `sha256sum pack.gb` and its size go into the index, the version goes up by one. The source JSON is the editable content and is kept in the private repository, never in a public one.

## On the TV
- A game that needs a pack asks for it when it opens (never during a round). The index is fetched, the pack downloaded only if it is missing or older, **only when the TV has room** (three times the pack and 400 MB free), only after its sha256 matches, and written whole before it is used. A game always has its bundled bank to play with while a pack is not there.
- Per pack the person chooses, in «حزم المحتوى» (the last tile of the games list): **«احتفظ دائمًا»** (kept) or **«تلقائي»** (the default: a pack nobody opened for **14 days** deletes itself and downloads again when its game opens). The row shows the size and has «حذف الآن». The list sweeps expired packs when it opens.
- Nothing about the TV is sent: a plain GET of two public files.

## Packs of questions, released every two weeks (4.4)
- A pack of kind «quiz» is a bank file `{"v":2,"items":[...]}` (the quiz's compact items, plus `s`, the source) sealed as a GameBundle under the pack's own id (`q01`, `q02`...). Its index entry also has `kind`, `date` (the day it was published) and `n` (its questions); an older app ignores them.
- **Nothing ships early.** The questions live in the private repository `urtv-games-content`; its schedule (`schedule/schedule.json`: one pack every second Thursday from 2026-12-03) and a workflow with no AI (`release-pack.yml`, every Thursday) publish the oldest due pack to urtv-data (`games/packs/<id>/pack.gb`, `index.json`). No question is bundled in the app.
- The quiz bank adds every installed pack of questions to whatever bank it chose (`QuizBank.addExtra`): an id the bank already has is kept; a missing or bad pack adds nothing. A quiz screen fetches new packs (`GamePackStore.ensureQuiz`): only when the TV has room, only with a matching sha256, never during a round. «حزم المحتوى» lists them with their size and number of questions, **«جديد»** for 21 days after the pack's date, and the same «احتفظ دائمًا» / «تلقائي» / «حذف الآن».
