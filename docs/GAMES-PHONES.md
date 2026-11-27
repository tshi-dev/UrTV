# اللعب بالجوالات (4.3.3, phase 1)

One switch, «اللعب بالجوالات», in the setup of «واحد مو معنا» and «بدون كلام». **Off by default, every time** (it is not remembered from last time; only the
«بالجوالات» page of the games list turns it on for the game it opens). Off, nothing changes: one remote, the hand-over screen, no socket.

## What the person sees
- The games list has a filter at the top, beside the night switch: **«عرض: الكل / بالجوالات»**. «الكل» (the default) shows every game; «بالجوالات» shows only the games
  that can be played with phones (`GameRegistry.PHONE_GAMES`), and opens the party games with the phones switch already on. The choice is kept. In both views the
  party games can be played with one remote (switch off).
- With the switch on, «ابدأ» opens a lobby: the QR, the four-digit room code, the address, the phones that came in (as many as 12). Start needs 3 phones
  for «واحد مو معنا» (10 at most) and 2 for «بدون كلام». The phones are the players, in the order they came; their names are the players' names.
- **واحد مو معنا**: every phone shows its own word at once (the one outside gets «أنت برا السالفة»), then «شفتها». The TV goes on when all have tapped, or when
  the host presses OK. The vote is on the phones (a name, never your own, or «ما أدري»); the TV counts when all have voted, or on «عدّوا الأصوات». The
  most votes wins; a tie, or no name, is «ما اتفقنا». The remote's own vote grid still works.
- **بدون كلام**: the turns go round the phones. The actor's phone shows the word (a secret card, «شفتها» starts the clock), then the word with
  «صح», «تخطّي» and, in «وصّف» with fouls on, «مخالفة». The TV never shows the word in this mode, not even on «شوف الكلمة».
- **الكلمة المفقودة**: each phone is a team (1 to 4), named by the phone's name. The statement with its blank goes to the phone of the team whose turn it is, which types the word (a «text» card; spelling variants meet: أ ا، ة ه، ى ي، «ال»، marks). A right word is marked right at once; any other word goes to the host's «صح / خطف / خطأ» buttons with what was typed beside it. Time up, the hint and the steal work as with one remote.
- «الأسرع» is unchanged (its own room and page, docs/QUIZ.md).

## Next phases
The list in `PHONE_GAMES` grows as each game gets the switch («الكلمة المفقودة», «خلية الحروف», «خمّن الكلمة», «خمس خانات», «دومينو»).

## Security (the same model as docs/QUIZ.md)
- `GamePhoneRoom` is a second `PhoneRoom` behind the same `QuizServer`: the local network only (a private address), a 4-digit random room code from a
  SecureRandom, 16-character tokens, 3 s per request, 1 KB bodies, 4 connections per address, five wrong codes lock an address for a minute, 20 joins a
  minute for the room, 5 answers a second per phone. No route does anything outside the game. Nothing is written to disk.
- A phone gets only its own card. A card carries an id: an answer for a card the TV has already replaced is refused («stale»). A vote cannot be for
  yourself or out of range, a secret is answered once, an actor's buttons are only the ones the card offers.
- Names: letters, digits, single spaces, 12 characters, nothing that reads as a link. Once the game starts nobody new can join; a known phone can come back.
- The room closes with the game's screen (Back from the lobby, leaving the game). A phone that goes away is not waited for forever: the host can go on.

## Files
`PhoneRoom` (what the server needs), `GamePhoneRoom` (the rules of the room: seats, cards, answers), `PhonePlay` (opens the room and the server),
`assets/quiz/game.html` (the phone page, the same look as «الأسرع»), `GameActivity.drawPhoneLobby`. Tests: `tests/phone_room_test.py` (JVM),
`tests/game_phone_page.test.mjs` (the page in a browser against the real room and server).

## Not in phase 1
The other games (the rest stay on one remote), the phone as a keyboard, a different number of players than phones, rejoining a game in play with a new
phone, «الأسرع» inside the new room.
