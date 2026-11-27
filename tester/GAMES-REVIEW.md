# UrTV games review — build 629 (versionCode 628), TV, remote only, 27 Nov

How I tested: UrTV → «تلفزيونك» hub → «الألعاب». adb only, one screenshot after every key, TV muted, default settings, the header switch left on its default «بدون جوالات: ريموت واحد يدور بينكم». Nobody typed any name or code; no phone joined. The list has 16 games (4 per row, RTL): الأسرع, خمس خانات, الكلمة المفقودة, واحد مو معنا / بدون كلام, خلية الحروف, خمّن الكلمة, دومينو / على جبيني, من كاتبها؟, أقرب رقم, واحد يكذب / اقرأ الإيموجي, اربط الكلمات, ليل القرية, ارسم وخمّن.

## Things that apply to every game (found while reviewing)
- Every tile in the list carries the same orange phone badge, in the remote-only mode too, so the badge does not tell which games need phones (الأسرع needs them, خمس خانات does not).
- The header has three buttons (phone, dice-like, moon). The phone button switches the style: «بدون جوالات: ريموت واحد يدور بينكم» (default) or «كل واحد بجواله، والريموت للمضيف». The label is the only sign of the current state. Focus in the header is a thin orange ring that is easy to miss.
- The focus is the filled light button (pause menu, setup buttons); on the option rows it is a thin ring on an already orange (selected) button, which is hard to see from 3 m.
- The «!» how-to panel is at the top of every setup page: from the bottom button it takes one UP per row (8 for خمس خانات). It scrolls with DOWN and ends with a clipped box at the bottom (the last line is cut until you scroll).

## Table
| Game | Players | Phones | Remote-only | Works | Top fix |
|---|---|---|---|---|---|
| الأسرع | 2+ phones | yes (all) | **no** | blocked at the room: «ابدأ (لاعبان على الأقل)» stays greyed | hide it or say «يحتاج جوالين» on the tile |
| خمس خانات | 2-4 teams (or singles) | optional | yes | yes | the letter ه looks like the digit ٥ |
| الكلمة المفقودة | 1-4 teams | optional | yes | yes | judge buttons صح/خطأ work, but no timer-out / auto-reveal tested |
| واحد مو معنا | 3-10 players, remote passed round | optional | yes | yes | «لـ لاعب» spacing; names page only sometimes |
| بدون كلام | 2-4 teams | optional | yes | yes | next word is shown on the TV while the timer runs |
| خلية الحروف | 2 teams | optional | yes | yes | letters on the hex board are dark on dark (low contrast) |
| خمّن الكلمة | 1 group | optional | yes | yes | 5 letters = ~20 key presses; invalid words are accepted |
| دومينو | 1-4 players (+ up to 2 computers) | optional | yes | yes | the opponent tile row is tofu boxes (missing glyph) |
| على جبيني | 2-4 teams, 1 phone each (min 2) | **yes** | **no** | blocked at the room: «لازم ٢ لاعبين على الأقل» | tag it on the tile; remote variant below |
| من كاتبها؟ | 3-10 phones | **yes** | **no** | blocked at the room: «لازم ٣ لاعبين على الأقل» | typing a 30-letter answer needs a phone; offer a picked-answer variant |
| أقرب رقم | 2-10 phones | **yes** | **no** | blocked at the room (2 phones) | one remote can do it: number keys + hide/show |
| واحد يكذب | 4-10 phones | **yes** | **no** | not started (how-to: 4 to 10 phones, secret role on each screen) | hidden-answer passing like «واحد مو معنا» |
| اقرأ الإيموجي | 2-10 phones | **yes** | **no** | not started (how-to: each picks 1 of 4 on their phone) | the 4 colour keys on the remote would do |
| اربط الكلمات | 4-10 phones (two spymasters) | **yes** | **no** | not started (how-to: the key card is on two phones) | show the key card to one player at a time |
| ليل القرية | 6-10 phones | **yes** | **no** | not started (how-to: secret roles on phones) | a «moderator» flow like «واحد مو معنا» |
| ارسم وخمّن | 3-10 phones (one draws) | **yes** | **no** | not started (how-to: draw with a finger, guess on phones) | no remote variant: keep for phones |

## الأسرع
- What it is: a question quiz. Everyone answers on their own phone, the fastest and most accurate scores most (500 + up to 500 by time; streak bonus 50/100/150).
- Players: 2 or more, each on a phone. Phones: yes, always («اللعبة كلها بالجوالات، والريموت للمضيف» in the panel).
- Remote-only? **No.** «افتح الغرفة» opens a room with a 4-digit code, a QR and the TV's address; the «ابدأ» button is greyed until 2 phones join.
- Worked / got in the way: the setup page is clear (category, level, number of questions, for kids, sound). The room page is readable at 3 m (code very big). Dead end for a household without phones, and nothing says so before «افتح الغرفة». The room page shows the TV's LAN address (not pushed: boxed out).
- Ideas: say «يحتاج جوالات» on the tile; a remote-only variant: the host reads a question, the TV shows 4 answers with the 4 colour keys, and each player in turn presses «their» colour with the remote handed round (turn order shown, answer hidden until everyone has pressed).

## خمس خانات
- What it is: a letter appears, 5 categories (animal, plant, thing, country, name): write a word for each starting with the letter before the others. 90 s timer (60 s / none possible), 5 rounds (3 / 5 / 8).
- Players: 2 to 4 teams (5 to 10 are dashed = locked) or singles. Phones: optional («اللعب بالجوالات: لا» is the default). Remote-only? **Yes**: write on paper or say it aloud; the host uses the remote only.
- Flow I played: «ابدأ» → team names page (defaults الصقور / النجوم; OK types a name, BACK = «تم») → back on the setup page (needs a second «ابدأ») → round 1: the big letter, 5 category tiles, the timer 90 counting → OK = «خلّصنا» → «من خلّص أول؟» (team / none, +50 for first with all valid) → reveal cell by cell: OK on a team cycles بدون +٠ → صحيحة +١٠ → مكررة +٥, «الخانة التالية», «أمثلة». BACK = pause: متابعة / إنهاء الجولة / الرئيسية.
- Worked: big letter and big timer, the cell-by-cell reveal is a good way to judge as a group, «أمثلة» is a nice help.
- Got in the way: (1) the letter ه is drawn like the digit ٥ (the box beside «+٥» and «الجولة ١ من ٥» use the same shape): easy to read the round as «5». Show the letter's name under it (هاء). (2) After the names page «تم» (BACK) you land on the setup page again and must press «ابدأ» a second time. (3) The first OK on «ابدأ» (focus on it, filled) did not start the game: it opened the team-names page, so «ابدأ» needs two presses. (4) The «اللعب بالجوالات» and «الصوت» rows are hidden under the fold of the setup page (a small ▼ at the left is the only sign).
- Ideas: one OK on «ابدأ» should go straight to the round (names only when asked); a visible team colour on the reveal cards; the timer in the last 10 s turns red; a pass-the-remote note for who presses «خلّصنا».

## الكلمة المفقودة
- What it is: a sentence (a fact, a proverb or a piece of information) with the last word hidden in a dashed box; the team says the word aloud in 15 s (10 / 30 possible); the host presses «اكشف الكلمة», the word appears in gold in the sentence, then «صح» or «خطأ». 10 points, 5 with the half-points hint, steal option («الخطف»).
- Players: 1 to 4 teams (2 default). Phones: optional. Remote-only? **Yes** (the host judges): played 3 questions: «الرمز الكيميائي لعنصر كبريت هو S», «شرّ البلية ما يضحك», «… جرمانيوم هو Ge».
- Worked: the flow is 2 keys per question (reveal, judge), LEFT/RIGHT picks خطأ/صح, the turn passes to the other team by itself, a big countdown.
- Got in the way: the setup page hides «اللعب بالجوالات» and «الصوت» under the fold; «للأطفال: نعم» is dashed (not available) with no explanation; chemistry symbols (S, Ge) and the like are Latin letters the team must spell aloud; the scores on the two cards are small (٠ shown as a diamond ◆, easy to miss).
- Ideas: show the answer's reading under Latin answers; say why «للأطفال» is locked; make the score numbers bigger.

## واحد مو معنا
- What it is: everyone gets the same word except one player who only gets the topic («أنت برا السالفة»). Each says one sentence about the word without saying it, then everyone points on «three» at who they think is out; the host picks the one with most fingers; the odd one still gets one guess.
- Players: 3 to 10 (4 default), one remote passed round; rounds 3 or 5; 1 or 2 languages. Phones: optional («كل جوال يشوف كلمته فورًا»). Remote-only? **Yes**: played the full flow with 4 players: «سلّموا الريموت لـ لاعب ١ — الباقين غطّوا عيونكم» → «أنا جاهز» → «لك وحدك» + the word + «اختيار: أخفِ الكلمة» (one per player) → «دور لاعب ١…» with a 28 s timer and the topic («الموضوع: العائلة») → «اللي بعده» → «مين اللي مو معنا؟ أشروا كلكم على ثلاثة» (vote) → «اخترتوا لاعب ١، مين كان؟ → كشف» → «باقي له فرصة: يخمّن الكلمة» → «اكشف الكلمة».
- Worked: the best remote-only flow of the ones I played: every private screen says «لك وحدك», the buttons name the next action, «نعيد توزيع الكلمات» is available.
- Got in the way: «لـ لاعب ١» has a stray space/tatweel (should read «للاعب ١»); on the names page the first OK opens names (games 2 and 4), while games 5 and 6 start straight away: inconsistent; the how-to's wrapped lines are cut by the next step's number box.
- Ideas: let the host change a player's name on the passing screen; show who has the remote as a name chip; on the vote screen show «٣ ٢ ١ أشيروا» as a real countdown.

## بدون كلام
- What it is: charades. One player holds the remote and sees the word, hides it, and acts it (or describes it in «وصّف» mode) for the team to guess in 45 / 60 / 90 s. OK = correct and next, ◄ = pass (3 per turn), ▲ = look at the word again, ▼ = a violation («مخالفة», in «وصّف» mode).
- Players: 2 to 4 teams. Phones: optional (with phones the word is only on the actor's phone). Remote-only? **Yes**: played «عسل» and «عصفور».
- Worked: «سلّموا الريموت لـ الصقور — الباقين غطّوا عيونكم» → «أنا جاهز» → the word alone → «أخفِ الكلمة» → timer 57 and a counter «جبتوا ٠»; the key legend is always visible.
- Got in the way: after each correct answer the next word is shown on the TV («الكلمة لك وحدك — الباقين: غطّوا عيونكم») while the timer keeps running (25, 21 …): the guessing team must look away from the actor while the actor fumbles for OK to hide it again, and the TV is where everyone looks. «جبتوا ٠» is a score; the zero is drawn as a small diamond. The list tile says «مثّل الكلمة وفريقك يخمّنها» but «وصّف» mode is only on the setup page.
- Ideas: after OK show the next word for 1 s only on a «tap to reveal» screen; stop the timer during the reveal; show the passes left as dots; in phone mode keep the TV on «أيش هي؟» only.

## خلية الحروف
- What it is: a 5 x 5 board of hexagons with Arabic letters; one team connects right-to-left, the other top-to-bottom. A team picks a letter, a question starting with that letter is asked, a correct answer colours the cell. First chain wins; no points.
- Players: 2 teams. Phones: optional (the bell button). Remote-only? **Yes**: played 3 questions; the question («ما عاصمة ليبيا؟», answer «طرابلس»), 15 s timer, «اكشف الجواب», «خيارات» (four options), then «من جاوب؟ الصقور / النجوم / ما أحد»; the cell turns blue and the team keeps the turn («من يجاوب صح يختار الحرف»).
- Worked: arrows move the highlighted hexagon in 6 directions, OK picks, the board is beautiful and the win condition is visible from the coloured edges.
- Got in the way: **the letters of the cells you are not on are dark grey on dark grey (nearly unreadable from 3 m)**; the score «١ / ٣» in the team card is tiny and top-left; the setup page needs 8 DOWN presses to reach «ابدأ» (the first OK I pressed landed on «الصوت» and changed nothing).
- Ideas: raise the letter contrast; a short «أول واحد يضغط» alternative for the bell; mark the last picked cell.

## خمّن الكلمة
- What it is: Wordle in Arabic: guess a 5-letter word in 6 tries; tiles turn orange for the right letter in the right place and grey for absent ones (the keys too).
- Players: one group, together. Phones: optional (typing on phones). Remote-only? **Yes**, with a TV keyboard.
- Worked: the board and keyboard are big and readable; colours are also shown on the keyboard.
- Got in the way: typing a 5-letter word takes about 20 key presses (letter by letter with arrows; the focus did not always land where I expected after typing the fifth letter: 4 DOWN moved 3 rows and 3 LEFT landed on «ي»); **the game accepts any 5 letters as a guess** («ابتثج» was accepted and cost a try); BACK deletes a letter, and only after the row is empty does it pause (I needed 6 presses to get to the pause menu); two colours only (no «wrong place» colour seen in my two guesses).
- Ideas: jump the focus to «إدخال» with the colour keys; accept only words from the list; type with the number keys or give the whole row as a letter picker (UP/DOWN cycle the letter, RIGHT/LEFT move the cell).

## دومينو
- What it is: classic domino to a target score (50 / 100 / 150); the one who empties their hand, or has the fewest points, wins.
- Players: 1 to 4 humans (2 default) and 0 to 2 computers (3 is locked), level easy / medium / hard. Phones: optional. Remote-only? **Yes**, even alone against the computer (no passing needed).
- Worked: the real board with the gold open ends, the 7 tiles drawn as pictures, «ضع الحجر» / «سحب» / pass, the draw count («السحب: ١٤») and goal («الهدف: ١٠٠»). The passing screen is the same as the other games.
- Got in the way: **the other team's hand is drawn as a row of empty boxes (▯▯▯▯▯▯▯), a missing-glyph (tofu) on this TV, next to the count**; two-digit counts and the Arabic-Indic numerals mix with Latin colons («السحب: ١٤»).
- Ideas: draw the opponents' hand as small tile backs; show «you can place / must draw» in words.

## Phone-only games (9 to 16): what I could verify without a phone
For these eight the setup page has **no «اللعب بالجوالات» row**, and where I pressed «ابدأ» (على جبيني, من كاتبها؟, أقرب رقم, and the first game الأسرع) the TV opens «اللعب بالجوالات»: a QR code, a 4-digit room code, the TV's address (`http://<ip>:<port>/<code>`), and a greyed button «لازم N لاعبين على الأقل» that never enables without phones. Back on that page = «أغلق الغرفة». I did not start the other four (the how-to text says phones are needed and the setup page has no remote option); I did not join a room from any phone.
Common issues on those pages: (1) the header line «امسح الرمز بكاميرا الجوال. ما فتح؟ اكتب» is cut after «اكتب» (the instruction to type the address is lost; the address is shown below in small Latin text); (2) the room code is shown in Arabic-Indic digits and a leading zero is drawn as a diamond («◆٨٢١») while the address below uses Latin digits («0821»): easy to misread; (3) nothing on the list tile or the setup page says «تحتاج جوالات» before you open the room; (4) the how-to's two-line steps are clipped by the next step's number box (seen in 3, 4, 7, 8, 12, 14, 15).

### على جبيني
- What it is: heads-up; the phone is held on the forehead and the team describes the word (45 / 60 / 90 s, 2 to 4 rounds each). The TV never shows the word. 1 point per word.
- Remote-only idea: the remote is the forehead card: the TV shows the word to everyone except the guesser, who sits with their back to the TV; OK = correct, LEFT = pass; the clock on the TV.

### من كاتبها؟
- What it is: each player types a funny answer (30 letters max) on their phone; the TV shows answers without names and players vote on who wrote each. 1 point for a right guess, 1 for the author of a fooled guess.
- Remote-only idea: the question is on the TV; each player in turn gets the remote and picks an answer from 8 prepared funny options (hidden from the others); then vote with the colour keys.

### أقرب رقم
- What it is: a numeric question («كم ثانية في اليوم؟»); guess a number on the phone's number pad; the nearest gets 3 points (5 if exact), ties share.
- Remote-only idea: each player in turn enters the number with the remote's number keys while the others look away; reveal all at once. Pace: 20 / 30 / 45 s per question.

### واحد يكذب
- What it is: 3 named storytellers, each secretly told (on their phone) to tell a true story or lie; the others question them and vote. 2 points for finding the liar, 1 for the liar per fooled voter.
- Remote-only idea: the same «سلّموا الريموت» screens as «واحد مو معنا» (the role is shown only to the holder), a timer for the story, a vote with the colour keys.

### اقرأ الإيموجي
- What it is: an emoji line stands for a word (🍚🍗 = كبسة); pick the right answer of four in 10 / 15 / 20 s. 3 / 2 / 1 points by order of correct answers. The emoji are drawn correctly on this TV (the how-to shows them).
- Remote-only idea: show the 4 answers coloured red / green / yellow / blue and let each player press their colour key on the remote in turn, hidden by the passing screen; or play one team at a time.

### اربط الكلمات
- What it is: Codenames-style board (words on the TV); two spymasters (on phones) see the key colours and give a one-word hint and a number; the rest tap words. A neutral word ends the turn, the other team's word scores for them and ends the turn, the «end» word loses at once (all from the how-to; I did not play it).
- Remote-only idea: the key card is shown to the spymaster alone on the TV with the «لك وحدك» screen, then hidden; the team picks words with the arrows and OK.

### ليل القرية
- What it is: Mafia/Werewolf: 6 to 10 players; secret roles (villager, wolf, guard, seer) on phones; at night the wolves choose, the guard protects, the seer asks; in the morning the result and 90 s discussion, then a vote. No points.
- Remote-only idea: the TV as the moderator with «غطّوا عيونكم» + one role per person passed the remote; works because the app already has that screen pattern.

### ارسم وخمّن
- What it is: one player gets a word and draws it with a finger on their phone; the TV shows the lines as they are drawn; the others type guesses on their phones. 3 / 2 / 1 points for the first three right guesses, the drawer 1 per right guess.
- Remote-only idea: not recommended (drawing needs a touch screen); at most a «pick from a list» version with a cursor.

## Ranked fixes (top 10)
1. **Mark phone-only games on the list** (and on the setup page): all 16 tiles carry the same phone badge in both modes; 9 of the 16 games cannot be played without phones. Show «جوال» only on those, and show «ريموت» on the rest.
2. **The first OK on «ابدأ» is inconsistent**: sometimes the names page (خمس خانات, واحد مو معنا), sometimes the game (بدون كلام, خلية الحروف, دومينو). Make «ابدأ» always start, with names as an optional «الأسماء» button.
3. **Domino: the opponent hand is tofu boxes** (▯▯▯▯▯▯▯): draw small tile backs.
4. **Hex board letters are unreadable** (dark on dark) for the cells you are not on.
5. **«بدون كلام» shows the next word on the TV after each correct answer** while the clock runs.
6. **Letter ه is drawn like the digit ٥**, and the digit zero is drawn as a diamond (scores «◆», room code «◆٨٢١»).
7. **«خمّن الكلمة»: ~20 key presses per guess, any 5 letters accepted**: give it a colour-key shortcut for «إدخال» and check the word list.
8. **Wrapped how-to steps are clipped** by the next number box (the second line loses its dots).
9. **Room page header cut after «اكتب»** and the address is tiny; make the address big and show the code in Latin digits too.
10. **Setup pages hide «اللعب بالجوالات» and «الصوت» under the fold** (only a small ▼ shows it); and BACK in «خمّن الكلمة» deletes letters, so 6 presses are needed to pause.

## Shots
`tester/games/<game-id>/` has, for every game, the setup page and the how-to panel (`a-*.png`, `how-1*.png`); for the games I played also the first round screens (`go-1`, `s-*`, `t-*`, `m-*`, `g1b/sub/g2` for the word game). Room screens of the phone games are `room-boxed.png` (the TV's address and the QR code are painted black). Ids: g01-sprint (الأسرع), g02-five-cells, g03-missing-word, g04-odd-one-out, g05-charades, g06-hex-letters, g07-word-guess, g08-domino, g09-heads-up, g10-who-wrote-it, g11-closest-number, g12-one-liar, g13-emoji, g14-link-words, g15-village-night, g16-draw-guess; the list and the hub are in `tester/games/` itself.
No player names, accounts or addresses are in the pushed shots; all names are the defaults (الصقور, النجوم, لاعب ١…).

## What I did not do
- No phone joined any room: the eight phone games were checked up to the room page (4 of them) or the how-to + setup page (4 of them).
- «خمس خانات», «الكلمة المفقودة», «واحد مو معنا», «بدون كلام», «خلية الحروف», «خمّن الكلمة», «دومينو» were played for 1 to 3 turns, not to a final result screen; the end-of-game and score screens were not seen.
- The «للأطفال: نعم» option was not tried; the header's dark-mode (moon) and the middle (dice) buttons were not pressed. I toggled the phone button once to read its two labels and put it back (default restored).
