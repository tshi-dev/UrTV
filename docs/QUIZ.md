# «الأسرع» (the live quiz): how it is built

Everyone answers on their own phone, faster means more points, the leaderboard is on the TV. Rules: `games/live-quiz/SPEC.md` (branch work/games-data); look: design/43 `quiz-B*` (the owner picked direction B «ورقة»). The name is a working name: **one string**, `QuizActivity.NAME`.

## Where it is
- The hub's **«الألعاب»** window (`YourTvHubActivity`, `GAMES_READY = true`) opens **`QuizGamesActivity`** (the games list: «الأسرع» with «بالجوالات», the others «قريبًا»), which opens **`QuizActivity`** (the game, full screen).
- **`QuizRoom`** (pure Java): the rules of one game: join, tokens, late join, answers, scoring, reveal, pause, end. **`QuizServer`** (pure Java): the game-only server. **`QuizPick`** (pure Java): which questions. **`QuizBank`**: the bank and what is remembered. **`QuizSound`**: four tones made in code. **`QrCode`**: the launcher's QR encoder, unchanged. `assets/quiz/play.html`: the phone page (one file, ar/en, dark/light, 360 px). `assets/quiz/starter.json`: the bundled starter questions.
- Tests: `tests/quiz_test.py` (runs `tests/QuizTest.java` on a plain JVM with a fake clock and the real server on the loopback, plus the pins below), `tests/quiz_phone.test.mjs` (the phone page at 360 px in a real browser).

## The game
1. **Chips** (the remote): «الفئة» (مزيج، جغرافيا، علوم، إسلامية، رياضيات ومنطق، حيوانات، السعودية والخليج), «المستوى» (سهل، متوسط، صعب، متنوع), «الأسئلة» (5, 10, 15), «للأطفال», «الصوت». A category is offered only with 40 or more items for the level and filter (else dashed, «قريبًا»); «مزيج» always works. Remembered for the next game.
2. **«افتح الغرفة»**: the server starts on the TV's private address, a random 4-digit code (never the same within the hour), a QR and the address. Players pop in (name, colour). OK on «ابدأ» with 2 or more players; OK twice on a tile removes that player.
3. «جاهزين؟» for 1.6 s, then each question: 20 s, one tap locks, the question closes early when everyone connected has answered. **Reveal 6 s** (OK skips): 3 s the right answer and who chose what (squares in the players' colours), 3 s the top 5 with movement. Back opens the pause card (متابعة، إنهاء اللعبة، الرئيسية); the clock stops.
4. The podium (top 3, confetti and the fanfare unless low RAM), DOWN for the full list; «العب مرة أخرى» (same players, scores 0), «تغيير الإعدادات», «خروج» (the room closes, everything is dropped).

## Scoring (on the TV)
Right answer: 500 + 500 × (time left ÷ 20 s), the time measured on the TV from the question's start to the tap's arrival; streak +50 from the 3rd right answer in a row, +100 the 4th, +150 from the 5th; wrong or none 0 and the streak breaks. Ranks: score, then who tapped first, then who joined first.

## The phone protocol (game-only)
`GET /<code>` the page; `GET /f/<font>.woff2` the fonts (a plain name that exists); `POST /api/join` (c, n, k, t, l), `GET /api/state?c=&t=` (polled every 700 ms), `POST /api/answer` (c, t, q, a), `POST /api/leave`. Forms only, 1 KB at most, one answer per connection. The state never contains the right answer before the reveal. A page opened with an old or wrong code says «الرمز غير صحيح» and offers nothing else. The server answers private, link-local and loopback addresses only; five wrong codes lock joining for 60 s; 20 join attempts a minute for the room; 5 messages a second per player; 12 players; a name is letters, digits and spaces, 12 characters, duplicates get a number. A phone coming back with its token gets its name, colour and score; a late joiner waits for the next question and starts at 0. Names, tokens and scores exist in memory for the game only; the app remembers the chips, the sound switch and the ids of the last 100 questions asked, nothing else. The page is plain HTTP on the home Wi-Fi (the SPEC's known limit: the code is random per game and the host sees every name).

## The question bank: how it is published
- Source of truth: `games/data/quiz.json` (branch work/games-data, verified items only).
- **Proposal**: a CI step (on a change of that file) runs `python3 .github/tools/make_quiz_bank.py games/data/quiz.json quiz/quiz.json` and pushes it to the branch **`data-games`** as `quiz/quiz.json` (compact: 1,234 items are 337 KB, sorted by id, so a rebuild with the same bank is the same file).
- The app (`QuizBank`) fetches `https://raw.githubusercontent.com/tshi-dev/UrTV/data-games/quiz/quiz.json` when the quiz screen opens and the kept copy is older than 3 days, keeps it in its files, and uses it **from the next game on**; a bad file never replaces a good one (it needs 40 valid items).
- Offline, or before the branch exists: the **starter set** (`assets/quiz/starter.json`, 210 items, 57 KB: 70 of each level, 71 for kids, a few of every category) so the first game works, with «مزيج» and any level or the kids filter. The starter is rebuilt with `make_quiz_bank.py games/data/quiz.json assets/quiz/starter.json --starter` when the bank changes much.

## Conflicts between the SPEC and the mockups (the SPEC wins on rules, the mockups on look)
1. **Answer colours**: the SPEC says four of our own hues, one per letter; the mockups (B) use four identical stamps with the letter and keep colour for the players. Follows the mockups (the owner's pick); the letters أ ب ج د are as the SPEC.
2. **Setup and lobby**: the SPEC has one ready screen (chips, QR, list); the mockups two screens. Built as two (chips first, then the lobby); «تغيير الإعدادات» comes back to the chips with the room kept.
3. **Chips**: the mockup's categories (عام، رياضة، تاريخ، علوم، أفلام) are replaced by the SPEC's. A fifth row «الصوت» is added (the SPEC says the sounds can be muted).
4. **Reveal**: the SPEC has one reveal of 6 s with bars of votes and the top 5; the mockups two screens and squares of the players' colours for who chose what. Built as one 6 s phase showing both in turn (3 s each); the phone gets the four counts.
5. **Timer**: the SPEC says a 20-second ring; the mockups twenty ticks. Ticks (the look), the same 20 s; the last 5 turn coral with a tick sound.
6. **Streak bonus** is in the SPEC, not in the mockups: built (the phone shows the points won).
7. **Avatar**: the SPEC says an avatar tile; the mockups a colour square. Colour square.
8. **No-phones fallback**: the SPEC offers «سؤال وجواب» (remote only) after 60 s with fewer than 2 phones. That game does not exist in the app yet, so the lobby shows «هذي اللعبة تحتاج جوالات، ادخلوا بالجوال» and stays; the fallback lands with that game.
9. **Wording**: «انطلقوا» is on the phone (the question opens); the TV says «جاهزين؟» before the first question.
10. **Max players**: the SPEC says 12, the mockups show 6; the lobby switches to smaller tiles above 6.

## Low-RAM TVs: what is cut
No animation (pop-in, slide, reveal squares), no confetti, no extra buffering; the sounds are 45 to 600 ms of static audio made on the spot and released after playing; the question bank is parsed once per screen and the phone polls (no sockets held open). The server uses at most 8 threads and 1 KB bodies. The screens are the same.

## What needs the real TV and phones
The look at 3 m with the real fonts (stand-in fonts only in the mockups; the TV loads `fonts/*.otf`, the phone `/f/*.woff2`); the remote's focus on every screen; the QR read by real phone cameras from the sofa; the server's address on the real Wi-Fi and on a cable; join, answer and reconnect with real phones (iOS Safari and Android Chrome), including a phone that sleeps; the screensaver not starting during a long game; the tick and fanfare through the TV's speakers; low-RAM behaviour on a real 1 GB TV.
