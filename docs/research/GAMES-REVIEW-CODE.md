# Games review from the code (build 4.4, branch work/44b4-games-review)

What was read: `docs/GAMES-KIT.md`, `GAMES-PARTY.md`, `GAMES-PHONES.md`, `GAMES-NEW.md`, `GAMES-QURAN.md`, `GAMES-DATA.md`, `DOMINO.md`, `docs/mockups/howto/TEXTS.md`, `GameRegistry`, every `*Activity` of the games and every engine under `games/`.
What was not read: the private repo `tshi-dev/urtv-games-dev` (adding it was refused in this session), so nothing unreleased is covered, and the encrypted banks (`assets/games/*.gb`) were not opened. Nothing here changes a question, a fact or a source. No code was changed.

Words in the screens below are plain Gulf Arabic, for the owner to approve first. Design for every new screen: no pills, no «·», focus is scale and shadow only, colours from `GameLook`.

## The picture in one paragraph

20 entries in `GameRegistry`. 8 can be played with one remote (khanat, missing-word, odd-one, charades, letter-hive, word5, domino, and packs which is not a game). 11 need phones (quiz plus the 10 party games), and of those 11 only quiz is phone-only by nature. The other 10 are phone-only today because the first build used the phone as the answer pad, not because the rule needs it. `guess` («كم تقدر؟») is built and hidden until its topic lists are checked against sources. The engine games (`EngineGameActivity`, released as `e:` games) are all phone-based too.

Four ways to take a phone out, used below:

- **A. «نادوا الجواب»**: the question and the options on the TV, everybody calls the answer aloud, OK reveals, the host picks WHO was first (or «ما أحد»). The same rule as «خلية الحروف» since 4.3.2, so families already know it.
- **B. «سلّموا الريموت»**: the kit's `handOver` (pass screen, OK, 2 s count, the secret alone, OK hides, 20 s auto-hide, app leaving hides). For secret roles and secret words.
- **C. «ورقة»**: players write or draw on paper, the TV is the timer and the reveal, the host picks the winners. «خمس خانات» already does this.
- **D. «الأيادي»**: vote by pointing on the count, the host picks the most pointed-at (or «ما اتفقنا»). This is the vote «واحد مو معنا» already uses.

---

## الأسرع (quiz): phones, stays

- **Goal:** most points from fast, right answers on your own phone.
- **A turn:** TV shows the question (20 s). Each phone taps one answer, the first tap locks. Ends early when all answered. Score screen. Points: 500 plus up to 500 for time left, streak +50, +100, +150.
- **Players:** 2 and more. **Phones:** yes, the whole game; the TV is the board.
- **Breaks or confuses:** nothing found in code; its drawing is pinned line by line in `tests/quiz_test.py` and `step52_test.py`, which makes any change costly.
- **Better:** none now.
- **Remote-only:** do not convert `QuizActivity`. If wanted later, add the same question bank (`assets/quiz/bank.json`, unchanged) as a source for the shared screen of model A below, as a new game, so the pinned quiz stays untouched.

## خمس خانات (khanat): one remote

- **Goal:** for a letter, fill animal, plant, thing, country, name with words starting with it, before the others.
- **A turn:** setup (rounds 3, 5, 8, time, teams 2 to 4 or players 2 to 10) then the letter spins and stops. Everybody writes on paper or says it. The fastest presses OK («خلّصنا»), the host picks who finished first. Boxes revealed one by one, host marks each side «صحيحة» «مكررة» «بدون». 10 for a valid word nobody else gave, 5 if shared, +5 first to finish with all valid. Tie for first: one tie-break round with one box.
- **Players:** 2 to 10. **Phones:** optional (phone types, TV shows at reveal with a first guess of the marks).
- **Breaks or confuses:** the host decides every mark aloud, so a ten-player round is long (10 sides times 5 boxes). Marking is by OK presses, no «كل البقية صحيحة» shortcut.
- **Better:** a «الباقي صحيح» key per box that marks every untouched side valid, and a visible count of marks left. Needs a real TV round to see if ten sides is too slow.

## الكلمة المفقودة (missing-word): one remote

- **Goal:** say the missing word of a proverb, fact or verse.
- **A turn:** the sentence with a blank, the team in turn says the word aloud in time, «تلميح» once (first letter, half points), «اكشف الكلمة», host marks «صح» «خطأ», with «الخطف» the next team tries (5 points). Right = 10, after the hint 5.
- **Players:** 1 to 4 teams. **Phones:** optional (each phone is a team and types, a right word is marked at once, others go to the host).
- **Breaks or confuses:** whether the answer is right is the host's call, who may also be a player: a fairness risk on one remote. Verses are shown as stored and must stay so.
- **Better:** when the host is also a player, offer «المضيف يعلّم، وما يلعب هالسؤال»; not needed for phones mode.

## واحد مو معنا (odd-one): one remote

- **Goal:** all know the word but one; find him, or if you are him hide that you do not know.
- **A turn:** hand-over round: each sees topic and word alone (the odd one sees «أنت برا السالفة»). Talk round, 30 s each as a guide, OK moves on. Vote by hands (D): host picks the most pointed-at or «ما اتفقنا». Caught: one spoken guess, host marks «صح» «خطأ». Caught and wrong: 1 to every other; caught and right: 1 to the odd one; not caught: 2 to the odd one.
- **Players:** 3 to 10 (default 4), rounds 3 or 5, one lap or two. **Phones:** optional.
- **Breaks or confuses:** «ما اتفقنا» counts as not caught, so a room that dodges the vote hands the odd one 2 points. The secret vote (each picks a suspect, tie vote again) exists in the rules and has no screen.
- **Better:** give the «ما اتفقنا» a second go once (the rules already have a re-vote for ties) before it counts. Use the secret vote screen with B for rooms that are afraid to point.

## بدون كلام / وصّف (charades): one remote

- **Goal:** act (or describe) the word so your team guesses it.
- **A turn:** pass screen, OK shows the actor the word, OK hides it and the clock starts. OK = right and next word (ignored for 0.6 s after a new word), left = skip (3 a turn), up = show the word 3 s, down = foul (describe mode only, -1, never below 0). At 0: bell, word shown to all, turn passes. 3 turns a team, 45, 60 or 90 s.
- **Players:** 2 to 4 teams. **Phones:** optional (the word only on the actor's phone).
- **Breaks or confuses:** «أطفال» takes level 1 only; the first word needs a hand-over, later words show 4 s «غطّوا عيونكم» which a team can read over the shoulder.
- **Better:** a word list per category (the meta already has categories with kids counts) so an adult can choose «حيوانات» only. The data is there; the chip is not. Also a hard stop on a ten-second warning sound.

## خلية الحروف (letter-hive): one remote

- **Goal:** join your two sides of a hexagon board before the other team.
- **A turn:** the team in turn moves the frame to a free cell, OK, a question appears whose answer starts with the cell's letter. Aloud answer, «خيارات» shows four options, «اكشف الجواب», host picks who answered (team 1, team 2, «ما أحد»). The winner picks the next letter (or by turns).
- **Players:** 2 teams. **Phones:** optional (buzzer on each phone).
- **Breaks or confuses:** «ما أحد» leaves the cell free, so a hard letter can come back forever; no skip of a cell that two teams both fail.
- **Better:** after two «ما أحد» in a row on the same cell show a new question for it, or let the turn pass. A hint of how many cells are left to the first line, so the end is felt.

## خمّن الكلمة (word5): one remote

- **Goal:** find a five-letter Arabic word in 6 tries (5 or 7 chosen).
- **A turn:** on-screen letter keyboard with arrows and OK, «حذف», «إدخال», green yellow grey after each try. An unknown word says «ما نعرف هذي الكلمة» and costs no try.
- **Players:** 1 and more, cooperative. **Phones:** optional (anyone types, the first to send is entered).
- **Breaks or confuses:** a D-pad keyboard is slow for six tries of five letters; with phones off one person types for all. No score, no teams, so it feels like a puzzle, not a game.
- **Better:** teams that take tries in turn with a team score, and a «اقتراح» key that fills the next legal word. Both need no new data.

## دومينو (domino): one remote

- **Goal:** empty your hand first, or hold the fewest pips (draw game, double-six, to 50, 100 or 150).
- **A turn:** 2 to 4 players, people and 0 to 3 computer players (3 levels). The remote goes round: «أظهر أحجاري», left and right pick a tile, OK places it, the end is chosen when both fit. No fit: draw until one fits, then pass.
- **Players:** 2 to 4. **Phones:** optional (the hand is on the phone, never on the TV).
- **Breaks or confuses:** with people only, every move is a hand-over: show, choose, hide, pass. With four people that is a lot of remote swapping. DOMINO.md says the screen was not seen on a real TV yet.
- **Better:** with phones off and at most one person plus computer players, skip the hand-over entirely (the only human is always the one holding the remote). Partners (2 against 2) is the real table game in the Gulf and is listed as not built.

## على جبيني (forehead): phones

- **Goal:** your team describes the word on your forehead, you guess.
- **A turn:** phone to forehead, «جاهز», count of three, word shows. Tilt down or «صح» = point and next. Tilt up or «تخطّي» = next. 45, 60 or 90 s. 2 to 4 teams, 2 to 4 turns each, at least one phone a team (12 max). TV never shows the word. The words are the «وصّف» bank of «بدون كلام».
- **Players:** 2 to 4 teams. **Phones:** yes, because the phone is the forehead card.
- **Breaks or confuses:** with one phone a team, the same player holds it every turn of the team. The tilt needs iPhone permission on a tap.
- **Better:** let the turns go by player, not by phone.
- **Remote-only design:** it is «وصّف» with the word turned round. The TV shows the word big to everybody except the guesser, who sits with his back to the TV (the hand-over's «غطّوا عيونكم» line becomes «اللي يخمّن: ولّ ظهرك للتلفزيون»). The remote stays with a describer: OK = صح and next, left = تخطّي, same 3 skips, same clock, same bank, same scores as «بدون كلام». It is a mode of `CharadesGame` («على الظهر»), not a new engine. Hidden-answer trick: none, the guesser's back is the trick. Cut: the tilt, the «جاهز» count, the 12 phones. Risk: a mirror or a peeking child; say so in the how-to.

## من كاتبها؟ (author): phones

- **Goal:** write a funny answer, then guess who wrote each.
- **A turn:** a question on the TV, each types up to 30 characters in 45, 60 or 90 s (ends early when all wrote). Answers come up one by one without names, all but the writer vote who wrote it. Right guess = 1 to the voter, every wrong guess = 1 to the writer. 6, 8 or 10 questions. The bank is 129 prompts (120 kids), `author.gb`.
- **Players:** 3 to 10. **Phones:** yes, for typing and for secret votes.
- **Breaks or confuses:** a player who types the same as another gives two identical answers on the TV (not seen handled in code, to check on a real round).
- **Remote-only design:** C + D. Players write on paper, the TV shows the question and the timer, then «الإجابة 3 من 6: اقرؤوها»; the reader is the host. Each answer: the room points at the suspect (D), the host presses the real writer (the writer says it), then toggles who guessed right. Points as today. Hidden-answer trick: the paper. Cut: voting secrecy (hands are seen), the 30-character limit, the early end. It is slower and weaker than the phone version: convert last. An on-screen keyboard version (as the names page has) would take about a minute a player and is not recommended.

## أقرب رقم (closest): phones

- **Goal:** the guess closest to a number answer wins.
- **A turn:** the question («كم ثانية في اليوم؟»), a number pad on each phone (20, 30 or 45 s). Closest 3 (5 if exact), then 2, then 1, same distance shares. 8, 10 or 12 questions. 133 items with sources in `closest.gb`; the sources stay.
- **Players:** 2 to 10. **Phones:** yes, for the secret guess.
- **Breaks or confuses:** big numbers in digits (area of a country) are hard to read aloud; the screen groups them in threes, which is good.
- **Remote-only design:** C. Everybody writes a number on paper (or says it in turn, secret not needed if one writes first on paper). The TV shows the question and timer; «اكشف» shows the right number (and its source line as the bank gives it); the host picks 1st, 2nd, 3rd place from the sides grid (ties by choosing the same place twice) and a toggle for «بالضبط» (5). Hidden-answer trick: the paper. Cut: the pad. A dial (▲▼ digits) behind a hand-over would need a new kit block `handOverInput`: skip it.

## واحد يكذب (liar): phones

- **Goal:** three tell a story on a topic, two true and one made up; the others find the liar.
- **A turn:** the TV names three players (the ones who told least) and a topic. Each of the three sees a secret on his phone («احكِ قصة حقيقية» or «اكذب»). They tell (60, 90 or 120 s), the others question them, then vote. Finding the liar = 2 to each right voter, the liar gets 1 per fooled voter. 3, 5 or 6 rounds, 82 topics in `liar.gb`.
- **Players:** 4 to 10. **Phones:** yes, for the secret roles and secret votes.
- **Breaks or confuses:** with 4 players there are 3 tellers and 1 voter, so one person's vote decides the round and scores 2 or 0. The minimum should probably be 5 (the owner's call).
- **Remote-only design:** B for the three tellers (the remote goes to each of the three in turn: «احكِ قصة حقيقية» or «اكذب», 20 s auto-hide, the rest cover their eyes), D for the vote (the room points; the host picks the most pointed teller). Scoring needs one change: with a show of hands the app cannot know each voter, so score the room as one: liar found = every non-teller +2, not found = the liar +1 per non-teller. Hidden-answer trick: the deal order is random and the host sees nothing of it. Cut: the per-voter secret vote. Effort small: three hand-overs and the Odd One's grid.

## اقرأ الإيموجي (emoji): phones

- **Goal:** know the word a line of emoji stands for.
- **A turn:** a line of emoji (🍚🍗 = كبسة), four options on the phone, 10, 15 or 20 s. First right 3, second 2, others 1. 10, 15 or 20 puzzles from 151 in `emoji.gb`.
- **Players:** 2 to 10. **Phones:** yes, for answering together.
- **Breaks or confuses:** `EmojiGame` constants are reused by «كمّل الآية» and «ألغاز المنطق» (their start checks read `EmojiGame.MIN_PLAYERS`, and logic hard-codes 8), so a change to Emoji's limits silently changes two other games.
- **Remote-only design:** A. The TV shows the emoji line and options أ ب ج د, a timer, «اكشف الجواب» marks the right one, the host picks who called it first (sides or players), +3, or «ما أحد». Hidden-answer trick: none, speed is the game. Cut: second and third place points and the 10-player seat limit stays. This one screen serves emoji, logic and verse (below) with zero new content.

## اربط الكلمات (link): phones

- **Goal:** two teams uncover their 9 and 8 words on a 25-word board, led by captains' one-word clues, avoid the ending word.
- **A turn:** the captain of the team in turn sees the key on his phone, says a one-word clue aloud and picks 1 to 9 on the phone. The team's other phones touch words (first tap counts, «أنهينا» ends). Own word: go on up to number plus one. Neutral: turn ends. Other team's word: counts for them, turn ends. Ending word: you lose.
- **Players:** 4 to 10, two teams; the first two phones are the captains. **Phones:** yes, because the key must stay secret from one team.
- **Breaks or confuses:** a captain's phone that drops out leaves the key with nobody (not seen handled in code: check on a real round).
- **Remote-only design:** B at each turn. The remote goes to the captain of the team in turn; the other team and his own team cover their eyes; the TV shows the key grid for up to 30 s (the kit's 20 s is too short to read nine words, make the hide time a parameter); OK hides; the captain says the clue aloud and presses a number 1 to 9 with up and down; then the team moves the frame over the 25 cells with the D-pad and OK touches, «أنهينا» on the pass key. Hidden-answer trick: the key lives only on the secret screen. Cut: the captain's phone, the touch of many phones. Medium effort (a new board screen, the engine `LinkGame` is reusable as it stands).

## ليل القرية (night): phones

- **Goal:** villagers find the wolves; wolves reach parity with the villagers.
- **A turn:** secret roles (villager, 1 to 3 wolves, one guard, one seer). Night: wolves pick who leaves, the guard protects one (not the same one twice running), the seer asks about one and hears «ذئب» or «ليس ذئبًا». Morning: who left and his role, 90 s talk, vote on the phones, a tie sends nobody. 6 to 10 players, no bank.
- **Players:** 6 to 10. **Phones:** yes today.
- **Breaks or confuses:** nothing in code; a vote tie sends nobody, so a pair of wolves can force stalemates.
- **Remote-only design:** the TV is the narrator. B for the role deal. Night: a dark screen «القرية نامت» with a low tone, then «الذئاب يصحون» to the first wolf holding the remote (wolves see each other's names), who picks a name on a grid of seat numbers and OK; «الحارس» alone; «العرّاف» alone and the answer shows only to him. Everyone else: eyes closed and heads down, said on every dark screen. Morning and vote: D (the host picks the most pointed-at; tie = nobody). Hidden-answer trick: the dark night screen shows no names at all until the actor opens it. Cut: nothing of the rules. Effort large but it is the family's only 6 to 10 player game and the owner will want it at a majlis with no phones.

## ارسم وخمّن (draw): phones

- **Goal:** draw a word, the others guess.
- **A turn:** one phone gets a word (60, 90 or 120 s), draws with a finger, the TV shows the strokes live, others type guesses (the English word counts too). First right 3, second 2, rest 1, the drawer 1 for each right guess. 3 to 10 phones. Words: the charades bank.
- **Players:** 3 to 10. **Phones:** yes, for the finger canvas and the typed guesses.
- **Breaks or confuses:** a drawing route of 8 KB, 300 strokes is a tight limit set by the server.
- **Remote-only design:** B for the drawer (word shown alone) and C for the drawing on paper, shown to the room. The TV runs the timer, «من خمّن؟» lets the host pick the first, second, third. The drawer scores 1 for each. Hidden-answer trick: B. Cut: the live strokes board and the typed guesses. A D-pad drawing board (8 px steps, OK to put the pen down) is possible but slow for children and not recommended.

## كمّل الآية (quran-verse): phones

- **Goal:** pick the end of a verse from four.
- **A turn:** the start of a verse, four ends on the phone, first right 3, second 2, others 1, a wrong pick scores nothing and plays no sound. After each the surah and verse number. The text is `uthmani` from `assets/shared/verses.json`, cut at a space, never edited.
- **Players:** 2 to 10. **Phones:** yes.
- **Breaks or confuses:** logic shares Emoji's limits (see above).
- **Remote-only design:** A, without the speed pressure and with no timer sound: the host picks «من قالها» and gives 3. Cut: nothing; the text is the same file.

## سباق الحساب (maths): phones

- **Goal:** do the sum before the others.
- **A turn:** a question made by code in Arabic and English on the TV, a number pad on each phone, 15, 25 or 40 s. First right 3, second 2, others 1, wrong costs nothing. 3 levels. 10, 15 or 20 questions. No bank.
- **Players:** 2 to 8. **Phones:** yes.
- **Remote-only design:** A with no options: the TV shows the sum and a timer, «اكشف» shows the result, the host picks the first right caller. No wrong options to invent (they would be generated values, which is the owner's decision, not made here). Cut: the number pad, 2nd and 3rd place points.

## ألغاز المنطق (logic): phones

- **Goal:** solve a logic puzzle, shown in Arabic and English.
- **A turn:** four options on the phone, 10, 15 or 20 s, first 3, second 2, rest 1. 40 % from `logic.gb` (25 hand-worked puzzles), the rest made by code and re-solved by the tests.
- **Players:** 2 to 8. **Phones:** yes.
- **Remote-only design:** A, exactly as emoji. The puzzles already have four options. Cut: nothing else. Logic needs thinking time, so make it 20 s and no speed bonus: the first correct caller gets 3, others who also had it 1 by a second press of OK on their side.

## كم تقدر؟ (guess): phones, hidden

- **Goal:** how many things of a topic can you name; the highest bidder names them, each checked against the topic's list.
- **A turn:** topic, each bids 1 to 10, highest bidder (a tie by chance) types names, naming the bid or more scores the bid, fewer scores nothing and each other gets 1. 22 topics in `guess.gb`, the lists written from common knowledge and not yet checked against sources.
- **Players:** 2 to 10. **Phones:** yes today. **Status:** hidden on purpose; leave it hidden until the lists are sourced.
- **Remote-only design (when it is shown):** the bids are called aloud in turn, rising (D-pad ▲▼ on the host's number, OK to lock the highest), the bidder says the names aloud, the TV then shows the list and the host presses «كم صحيح؟» with ▲▼. Hidden-answer trick: the list stays unseen until the bidder is done. Cut: typing, the early end.

## حزم المحتوى (packs)

Not a game: it lists downloaded content with keep or delete. No review needed, except that the list should say which games a pack belongs to (not checked).

---

## Findings that apply to several games

1. **Docs contradict each other.** `GAMES-KIT.md` line 3 says every game but «الأسرع» is one remote and no phones. The registry and `howto/TEXTS.md` say ten party games are phone-only. `GAMES-PHONES.md` says «the party games can be played with one remote (switch off)» for odd-one and charades only. Fix after the conversions.
2. **A lobby with too few phones does nothing on Start** (every party game `startWithPhones()` just returns). Whether the lobby says «ناقص جوال» is a thing for the tester to see.
3. **Limits are shared by import, not by name** (`EmojiGame.MIN_PLAYERS` read by verse and logic, `8` hard-coded in logic). Give each game its own constants before changing any.
4. **`handOver` hides after 20 s**, fixed in the kit. «اربط الكلمات» (key) and «ليل القرية» (roles) need a longer, per-call limit.
5. **No remote-only way to make a secret choice** (a pick that the room must not see). The kit's `handOver` shows a secret but cannot take a choice back. A `handOverPick` block (names grid, OK, hide) would serve night, the secret vote of odd-one and liar. This is the one new kit block this review asks for.

## What the real-TV tester should check (short)

- The lobby message when phones are too few (finding 2); a phone leaving in «اربط الكلمات» (captain) and in «على جبيني».
- Domino with four people: how long a move takes; is the hand-over too much.
- «خمس خانات» with ten sides: how long marking takes.
- «بدون كلام»: can the room read the 4-second word over the actor's shoulder.
- «واحد مو معنا»: does a room settle the hand vote or fall to «ما اتفقنا».
- After conversion only: the 30 s key screen of «اربط الكلمات», the dark night screen at 3 m, the guesser's back in «على الظهر».

## Ranking: impact for the effort, and what to convert first

Effort: S about a day of code on the kit (screen on an existing engine); M a new screen and one kit change; L a new flow with several hand-overs. Impact: how many families get a game they cannot play now.

| Rank | Game | Remote-only design | Effort | Impact | Why |
|---|---|---|---|---|---|
| 1 | اقرأ الإيموجي, ألغاز المنطق, كمّل الآية, سباق الحساب | A, one shared screen | S to M | High (4 games at once) | One screen on the existing `EmojiGame` rules; no new content, no secrets; the «من جاوب؟» pick is already in «خلية الحروف» |
| 2 | واحد يكذب | B three tellers, D vote | S | High | Needs only the hand-over and the odd-one vote grid; one scoring change to approve |
| 3 | على جبيني | «على الظهر» mode of charades | S | High | Reuses `CharadesGame` and its bank; same game feel, no phone |
| 4 | أقرب رقم | C paper and reveal | S | Medium | A timer, a reveal and a three-place pick; no secrets to keep |
| 5 | ارسم وخمّن | B word, C paper | S | Medium | Same pieces as 4; the loss is the live board |
| 6 | اربط الكلمات | B captain turn, board by D-pad | M | Medium to high | Needs a longer secret time and a board screen; the engine is ready |
| 7 | ليل القرية | TV narrator, B and D | L | High but late | The only 6 to 10 player game; needs the `handOverPick` block first |
| 8 | من كاتبها؟ | C paper, D | M | Low | Weak on paper; keep it for phones |
| 9 | كم تقدر؟ | later | M | Low | Hidden until the lists are sourced |
| 10 | الأسرع | none (new mode on the shared screen only if asked) | M | Low | Pinned tests, works as it is |

**Convert first:** rank 1, then 2 and 3 (together they take six phone-only games to one remote with three small changes), then 4 and 5. Build `handOverPick` and the long secret time at the same time as 6 and 7. The owner's approvals needed before code: the liar scoring change, the liar minimum (4 or 5), the maths question without options, and the mockups of the shared screen A and the narrator screen of the night.
