# لعبة الأسئلة الحية (live quiz, phones answer, TV shows the board)

The one game that **needs phones**: everyone answers the same question on their own phone, faster means more points, the leaderboard is on the TV. It is our own game: our own name, look, sounds and wording. No other product's name, answer shapes, colour scheme, music, jingles or phrases are used.

## Name (pick one)
1. **«الأسرع»** (the fastest). Short, says the rule. Recommended.
2. **«سباق الأجوبة»** (answer race).
3. **«ومضة»** (a flash).

## Look and sound rules (so it stays ours)
- The four answers are marked by **the Arabic letters أ ب ج د** on tiles in four of our own palette colours; no triangle, diamond, circle and square set, and no red, blue, yellow, green quartet in that order. The designer picks the four hues from the games palette in design/43b.
- Our own sound set: a short tick for the timer in the last 5 seconds, a soft chime for a correct reveal, a low note for a miss, a short fanfare for the podium. Original, no melody borrowed from any show; all can be muted.
- Wording is ours: «جاهزين؟», «انطلقوا», «سجّلنا جوابك», «انتهى الوقت».

## Join
1. On the TV: games hub, tile «الأسرع», ready screen. The TV starts a **game-only server** on the local network and shows a QR code, the web address and a **short room code** (4 digits, random for each game), under the list of joined players.
2. The phone scans the QR (or types the address) and opens **the TV's own page** (no app, no account, same Wi-Fi). It asks for a **name** (up to 12 characters) and an **avatar colour** (8 choices). A player is shown with an avatar tile and the name.
3. Rules: 2 to 12 players. Late join is allowed **between questions** (the new player waits for the next question and starts with 0). A reconnecting phone (same device token) gets its name, colour and score back.
4. Access is **game-only**. It is not remote-control pairing: the phone page can do nothing but join, answer and see its own result. It cannot press keys, open apps, change settings or read anything from the TV.
5. The TV host (the person with the remote) starts the round with OK once at least 2 players are in.

## Round (chosen on the TV with the remote, before the lobby fills)
- Chips on the ready screen: **الفئة** (مزيج، جغرافيا، علوم، إسلامية، رياضيات ومنطق، حيوانات، السعودية والخليج), **المستوى** (سهل، متوسط، صعب، متنوع), **للأطفال** (on or off; on means `age: kids` items only), **عدد الأسئلة** (5, 10 or 15; default 10).
- The engine picks questions from `games/data/quiz.json` (type `quiz`, 3 wrong options plus the answer) matching the chips, never repeating a question inside one game, and avoids the last 100 questions asked on this TV.
- **Question screen on the TV:** the question large, four answers with their letters, a 20-second ring, an answered counter («٧ من ٩ أجابوا»), the question number. **On each phone:** four big buttons (as wide as the screen) **each showing its answer text** and its letter, plus the timer bar; no question text is needed on the phone but a short repeat is shown small.
- **Lock on first tap:** the first tap on a phone is the answer; the button stays highlighted and the phone says «سجّلنا جوابك»; it cannot be changed.
- The question closes when 20 seconds are over or everyone who is connected has answered.

## Scoring (correct answers only)
- A wrong answer or no answer: 0 points.
- Correct: **base 500** plus a **speed bonus of 500 × (time left ÷ 20 s)**, so the first instant answer is about 1000 and an answer at the last second is about 500. Times are measured **on the TV** from the moment the question appears to the moment the tap arrives; ties are broken by who tapped first.
- **Streak bonus:** from the 3rd correct answer in a row, +50 per question in the streak (3rd: +50, 4th: +100, 5th and later: +150). It is small by design so a late joiner can still catch up.
- Points are whole numbers, shown with Arabic-Indic digits.

## Reveal (after each question, 6 seconds, OK to skip)
- The TV shows the correct answer highlighted, a bar for how many players chose each answer, and the **top 5** with their points and rank change arrows.
- Each phone shows right or wrong, the points won, and **its own rank** («أنت الثالث»), also when the player is outside the top 5.
- A one-line fun fact (`fact`, if the item has one) is optional, never required.

## End
- After the last reveal: the **podium** with the top 3 (confetti, our fanfare), then the full list scrolling.
- «العب مرة أخرى» keeps the players connected with their scores reset to 0 and the chips as they were; «تغيير الإعدادات» goes back to the chips; «خروج» closes the room.

## No phones fallback
The game **needs phones**. If fewer than 2 phones have joined after 60 seconds (or the TV has no network), the TV says «هذي اللعبة تحتاج جوالات» and offers **«سؤال وجواب»** (the remote-only quiz, `games/COMMON.md`) with the same chips. The hub tile carries a phone icon and the caption «بالجوالات» so nobody starts it by mistake.

## Edge cases
- **Phone sleeps or the browser is backgrounded:** the phone shows «رجعنا» when it wakes; if the question is still open it can still answer; a missed question is a 0, not a removal. A keep-awake request is made on the phone page where the browser allows it.
- **Wi-Fi drops:** the page reconnects by itself with the device token for up to 2 minutes; the TV shows the player dimmed with «انقطع». After 2 minutes the player is removed from the lobby but keeps their score if they return before the game ends.
- **TV screensaver:** the app holds the screen awake for the whole game, from the lobby to the podium, and releases it at the end.
- **Duplicate names:** the TV appends a number («سارة ٢»); the phone shows the final name.
- **A player leaves:** pressing «خروج» on the phone removes them at once; their points stay in the end list marked as left (gray) so ranks do not jump.
- **Host leaves or Back is pressed:** the pause card opens (متابعة، إنهاء اللعبة، الرئيسية); the question clock stops while it is open; ending shows the podium so far.
- **Only one player left:** the game continues as a solo race for the score; it does not end by itself.
- **TV loses network mid-game:** the room closes with a message on every phone.
- **A phone page opened with a wrong or old code:** «الرمز غير صحيح»; the page offers nothing else.
- **Large text, RTL, small phones:** buttons never shrink below 56 px high; answer text wraps up to 3 lines and then shrinks to a floor of 18 px.

## Data
Uses the verified quiz bank (`games/data/quiz.json`, items of type `quiz`). To avoid repeats the engine needs **at least 40 questions per category and level**. What qualifies today (quiz items, count in brackets):
- Qualify at a single level: **جغرافيا** (سهل ٦٨، متوسط ١٣٥، صعب ٢٨٩), **علوم** (متوسط ٤٣، صعب ٩٨), **رياضيات ومنطق** (متوسط ٤٩).
- Qualify only as a whole category (all levels mixed, total in brackets), so the level chip is hidden for them: **إسلامية** (٦٨), **رياضيات ومنطق** (سهل ٣٨، قريب)، **حيوانات** (١٦, only kids level 1; below 40, shown only inside «مزيج»), **السعودية والخليج** (١٩, below 40, only inside «مزيج»), food, history and the rest (below 40).
- **مزيج** with any level or the kids filter always works (the whole bank is 800 or more quiz items).
- For the kids filter: 66 geography, 63 maths and logic, 15 Islamic and 15 animal items are `kids`; the engine mixes them.
The engine rule: a category chip is offered only when it holds at least 40 items for the chosen level and filter; otherwise it is grayed with «قريبًا». The question bank is expected to grow, and nothing in this spec changes when it does.

## Security and privacy
- **Room code:** 4 digits, random for each game, valid for that game only and for 10 minutes after the podium; never reused in the same hour. The same code is in the QR address.
- **LAN only:** the game server listens on the TV's private (local) address and refuses requests from public addresses; the game needs no internet, and nothing about it leaves the local network.
- **Answers are validated on the TV:** the phone sends only «answer index N for question Q with player token T»; the TV checks that the question is open, the player has not answered, and N is 0 to 3. The phone never receives the correct answer before the reveal, and the question data is sent without it.
- **Rate limit:** at most 5 messages per second per player and 20 join attempts per minute for the whole room; a code guessed wrong 5 times locks joining for 60 seconds. Message size is capped at 1 KB.
- **No personal data kept:** only a chosen display name, an avatar colour and a random device token exist, in memory for the length of the game. Nothing is written to disk and no analytics are sent. After the game is closed (or the app is left) everything is dropped. Names are limited to letters, digits and spaces, and no link-like text; they are never shown without escaping.
- **No access to the TV:** the page cannot call any other function of the TV app; the game-only server has no route that does anything outside the game.
- **Known limit:** the page is served over plain HTTP on the home Wi-Fi, so anyone on that network could join if they have the code. That is why the room code is random per game and the host sees every name in the lobby, and can remove a player with OK on a tile.
