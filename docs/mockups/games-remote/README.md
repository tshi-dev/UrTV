# Remote-only mockups for review (1080p, no code yet)

Made from docs/research/GAMES-REVIEW-CODE.md. They show what the family sees on the TV with one remote and no phones. Paper look from `GameLook.PAPER`; no pills, no middle dots, focus is scale and a shadow only; the font here is a stand-in (the TV uses Thmanyah). Bottom line = the next key in words, as in every game.

Content is only what exists: the question and its four answers are from `assets/quiz/bank.json`; the word «يرقص» is from `assets/games/charades.json`; the liar topic and the closest question (and its answer, 24 x 60 x 60) are the examples written in docs/GAMES-NEW.md. The encrypted banks were not opened, so a real round will show the bank's own items (and for «أقرب رقم» its source line, which is not drawn here). Team names are the kit's defaults, players are «لاعب N».

1. **«نادوا الجواب»** (the shared screen for the emoji, logic, verse and maths games; here with a quiz-bank question): `1-call-aloud-a-question` (question and options, timer, scores), `1-call-aloud-b-reveal` (OK shows the right answer in green, the rest dim), `1-call-aloud-c-who-first` (D-pad picks the team that said it first, or «ما أحد»; the focused card is scaled with a shadow).
2. **«على جبيني» as a mode of «بدون كلام»**: `2-forehead-a-room` (what the room sees: the word, the clock, OK = صح, ◀ = تخطّي; the guesser stands with their back to the TV), `2-forehead-b-guesser-cover` (the screen before the clock starts: tells the guesser to turn round and the others to read the word).
3. **«واحد يكذب»**: `3-liar-a-pass` (hand-over to a teller), `3-liar-b-true` and `3-liar-c-lie` (the secret, shown to that player only; OK hides it), `3-liar-d-talk` (stories and the 90 s clock), `3-liar-e-hands-vote` (everyone points on the count, the host picks), `3-liar-f-result` (the room scored as one: needs the owner's approval).
4. **«أقرب رقم» on paper**: `4-closest-a-paper` (question and timer, write on paper), `4-closest-b-reveal` (the number), `4-closest-c-first-place` (pick the team with the closest paper; the same screen repeats for 2nd and 3rd), `4-closest-d-order` (the order and the points).

Open points for the owner: who answered first is the host's call in 1; the liar scoring in 3f; the number of places picked in 4 (3 now, as the phone game).
