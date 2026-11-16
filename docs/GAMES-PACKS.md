# Content packs of the games (4.4)

Anything big (a large bank, emoji or picture sets, later audio) is a **downloadable pack**, not part of the APK. The small banks that make a game work offline stay in the app (`assets/games/*.gb`).

## Where and what
- A pack is one `GameBundle` file (docs/LICENSE-CONTENT.md: compressed, lightly encrypted) in the public data repository: `urtv-data` at `games/packs/<id>/pack.gb`, listed with its version, size and sha256 in `games/packs/index.json` (`{"v":1,"packs":[{"id","ar","en","ver","bytes","sha"}]}`). Packs are split small, one per game or theme.
- Build one: `python3 games/tools/pack_bundle.py <id> source.json pack.gb`, then `sha256sum pack.gb` and its size go into the index, the version goes up by one. The source JSON is the editable content and is kept in the private repository, never in a public one.

## On the TV
- A game that needs a pack asks for it when it opens (never during a round). The index is fetched, the pack downloaded only if it is missing or older, **only when the TV has room** (three times the pack and 400 MB free), only after its sha256 matches, and written whole before it is used. A game always has its bundled bank to play with while a pack is not there.
- Per pack the person chooses, in «حزم المحتوى» (the last tile of the games list): **«احتفظ دائمًا»** (kept) or **«تلقائي»** (the default: a pack nobody opened for **14 days** deletes itself and downloads again when its game opens). The row shows the size and has «حذف الآن». The list sweeps expired packs when it opens.
- Nothing about the TV is sent: a plain GET of two public files.
