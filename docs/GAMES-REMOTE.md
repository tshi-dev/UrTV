# «اللعب بالريموت بس»: the one-remote way for the phone games

A row in the setup page of seven phone games, **off by default** (kept per game like any chip), so nothing changes until someone turns it on. On, the game does not open a room: it plays on the TV with the remote. The setup page and the tile say «ريموت» then. Mockups: `docs/mockups/games-remote/`. Review and designs: `docs/research/GAMES-REVIEW-CODE.md`. Nothing here adds content: every question comes from the game's own bank, verses file or generator, read in memory through `GameBundles` as before.

| Game | One-remote way | Code |
|---|---|---|
| اقرأ الإيموجي, ألغاز المنطق, كمّل الآية, سباق الحساب | «نادوا الجواب»: the question and four options (a sum has none) on the TV, a clock, everybody calls the answer aloud, OK shows the right one, the host picks WHO called it first with the D-pad (or «ما أحد»): +3. The sides are 2 to 8 teams (names by the people, as everywhere) | `CallAloudActivity`, `games/remote/CallAloudRound` |
| على جبيني | a mode of «بدون كلام»: «ولّ ظهرك للتلفزيون» (the guesser), then the word stays on the TV for everyone else and the clock starts; OK = «صح» and the next word, ◀ = «تخطّي». The «وصّف» words, the charades scores and tie rule | `CharadesActivity` (`behind`), `CharadesGame.behind` |
| واحد يكذب | the remote goes to each of the three tellers (the kit's hand-over: «احكِ قصة حقيقية» or «اكذب»), the clock, then a show of hands: the host picks the teller the room chose or «ما اتفقنا». The room is scored as one voice: every player who did not tell votes the same | `LiarActivity`, `games/liar/LiarRemote` |
| أقرب رقم | paper: everyone writes a number, the TV is the clock and shows the number, the host picks the closest three (3, 2, 1; 5 for an exact first place); a side holds one place | `ClosestActivity`, `games/closest/PaperPlaces` |

**Constants the owner may change** (one line each): `CallAloudRound.POINTS` (3), `CallAloudRound.MAX_SIDES` (8), `LiarRemote.MIN_PLAYERS` (5; the phone game keeps 4), `LiarRemote.NOBODY_FOOLS` («ما اتفقنا» counts as not found), `PaperPlaces.PLACES` / `POINTS` / `EXACT`.

Look: the score is in its own cell with a «نقطة» label (`GameActivity.scoreBar`), the focus is scale and shadow only (`focusCard`), no pills, digits follow the person's setting (Arabic-Indic by default). Tests: `tests/remote_modes_test.py` (rules on a JVM and the wiring by source).
