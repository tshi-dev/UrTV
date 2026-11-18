# The games' content: «All rights reserved»

**The code is MIT; the games' content is not.** The question banks, the prompts, the emoji puzzles, the word lists, the topics, the estimate items and every pack of the games («الأسرع» and the party games, `docs/GAMES-*.md`) were written for UrTV and are **© the UrTV owner, all rights reserved**. You may not copy, republish, rebuild or train on them. Using the app on your own TV is allowed, of course.

This does not cover what the content is built from. Facts are free; the sources we checked them against keep their own licences, listed in `games/REVIEW-SOURCES.md` (Tanzil metadata CC BY 3.0, mledoze/countries ODbL, ar.wikipedia CC BY-SA 4.0). The Quran text is the King Fahd Glorious Quran Printing Complex's (see the README credits) and is not ours to license. The recitations are mp3quran.net's (see `docs/GAMES-QURAN.md`); the app does not store or republish them.

## How the content is kept (and what that does not do)
- **The editable source** of every bank and pack is kept in a private repository (`tshi-dev/urtv-games-content`), not in this one. If that repository is not reachable, the source stays out of every public repository.
- **What a TV gets** is only a compiled, compressed and lightly encrypted bundle (`GameBundle`: deflate, AES-256-CBC, HMAC-SHA256, the keys derived inside the app from the bundle's name and pieces kept in the code). The bundled banks are `assets/games/*.gb`; the large ones are downloadable packs (`docs/GAMES-PACKS.md`) that are the same kind of file.
- This stops casual copying: opening the file in an editor, searching the data, scraping the repository. It does **not** stop a determined person who reads the app's code and runs it. It is a fence, not a safe.
- No git history is rewritten: content that was published before this rule (the older banks in `games/`) stays in the history under the same terms as above.

## For contributors
Do not paste content from the games into issues, reports or other projects. Report a wrong item by its id (`share.sh`, AGENTS.md), not by its text.
