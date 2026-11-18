# Quran games

## كمّل الآية (Finish the Verse): in the games list

Two to ten phones. The start of a verse is shown on the TV and on every phone; each phone picks its end among four. The ends of the wrong options are the ends of other verses of about the same length. The first right pick scores 3, the second 2, the others 1; a wrong pick scores nothing and plays no sound. The TV shows the surah and the verse number at the end of each question.

- Text: only `assets/shared/verses.json`, field `uthmani`, the mushaf text the project already checks against the KFGQPC / QPC Hafs text in CI (`.github/tools/verify_uthmani.py`). A verse is cut at a space only (the tests check that start + space + end is exactly the stored verse); nothing is edited, shortened or re-spelled. Ranges (`112:1-4`) are skipped; 389 of the 500 stored verses (6 to 24 words) qualify.
- No pack for the text: it is already in the app (about 500 verses). A larger text pack («quran-text», all the surahs) waits for the text to be verified in CI like these.
- Rules: the shared multiple-choice engine `EmojiGame`; items: `games/quran/QuranItems.java`; screen: `QuranVerseActivity`.
- Tester: check on a TV that the marks (waqf signs, small letters) render in the question and on the phones; a missing mark would be a font problem, report it, do not edit the text.

## من هو القارئ؟ (Who Is the Reciter): built, HIDDEN

A clip of a recitation plays on the TV (the first 12 seconds of a short surah, from the same files the app's «تلاوة» already streams: `android/launcher/src/main/assets/quran/reciters.json`), and each phone picks the reciter among four.

- It is hidden: `QuranReciterActivity` is in the manifest, but it has no entry in `GameRegistry` and none in the phone filter. To show it, add `new Game("reciter", "من هو القارئ؟", "...", "QuranReciterActivity", true, true)` to the registry and `"reciter"` to `PHONE_GAMES`, plus the names to `QuizGamesActivity.WORDS`.
- Why: the terms of mp3quran.net could not be read from the build environment (the host does not resolve), so it is not known whether they allow streaming a short clip inside another app. Nothing is bundled, stored, cached or republished: the clip is streamed at play time and stopped after 12 seconds (`QuranItems.CLIP_SECONDS`). If the terms allow it, record them here and show the game; if they forbid it, delete the activity.
- Not done: seeking to the start of a chosen ayah (it needs mp3quran's ayah timing data, also unreachable here); the clips start at the beginning of a short surah.
