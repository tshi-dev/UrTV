# Mockups against the specs

Read: `design/43b/preview/10-games-hub.png` (hub), `11-quiz-question.png` (quiz), `15-word-game.png` (word game) and `design/43b/src/games.js` on `work/marketing` (also `design/43/20` to `22`). The specs in `games/<id>/SPEC.md` follow the mockups where a sane rule allows; where it does not, it is listed here.

## Agree
- Four teams with avatars and a score bar at the bottom; the quiz header «السؤال ٤ من ١٠» and «دور الصقور»; a round timer ring (the specs use 15 s, the mockup shows 12 s counting down).
- Word game: 5 letters, 6 rows, 7 by 4 keyboard in alphabet order, green (right place), yellow (wrong place), dim keys for used letters, a focus ring on one key, RTL rows.
- Big heavy type, saturated cards on deep violet, shape icons on the quiz answers (they also help colour-blind players).

## Contradictions and what the specs do instead
1. **Hub subtitle «بدون إنترنت ...»** says the games work with no internet. The question and word data is stored online and loaded when a game starts (owner brief). Decision needed: either a small pack is cached after the first load (then the line becomes «تعمل بعد التحميل الأول»), or the line changes. Until then each game's ready screen shows «جاري تجهيز الأسئلة» for a second when the data is not cached, and falls back to the last cached file.
2. **«·» separators** appear in the hub subtitle, the card captions and the word-game subtitle («كلمة من خمسة حروف · حاولت ٣ من ٦»). The owner rule is no «·» in any text: use a comma, a dash or two lines.
3. **Colour-button legend on the hub** (red start, green teams, yellow instructions, blue sounds). Many TV remotes have no colour keys and `COMMON.md` says OK is the only key needed. The specs use on-screen buttons only; the legend can stay as a hint where the remote has the keys, never as the only way.
4. **Hub shows three games** (quiz, memory pairs, word game); the owner's list is seven (خانات، واحد مو معنا، بدون كلام / وصّف، الكلمة المفقودة، خلية الحروف، دومينو، خمّن الكلمة) plus the quiz. The hub needs a scrolling row or a 2 by 4 grid; memory pairs stays out for now.
5. **Word game tiles show ة** in «مدرسة» (the mockup answer) while the keyboard has ه and no ة. The normalization rule (ة → ه) means the tiles must show ه while typing and feedback; the reveal line then prints the original spelling «مدرسة». If tiles should show ة, the keyboard needs a ة key and the lists change.
6. **Quiz answers are four colour-and-shape buttons** like a phone buzzer game. With one remote and teams taking turns that is fine; but the quiz also needs the say-aloud mode (OK reveals), because one remote cannot be four buzzers. The specs keep four options as the default and add «ابدأ بدون خيارات» for harder rounds.
7. **Quiz team bar uses +10 steps**; matches the scoring (10 per correct answer, 5 after a hint).
8. **Hub says «١-٢ لاعب» for the word game** while `word5/SPEC.md` allows the group to play together; the label should read «١ إلى ٤ لاعبين، أو الجميع معًا».
