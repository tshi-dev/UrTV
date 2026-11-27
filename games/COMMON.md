# Rules shared by every family game

Owner rule: **the default of every game is played with no phones.** One remote, pass-and-play, or say it aloud and press OK to score. The phone mode is an optional addition on top of the same flow, never a requirement.

## Start in under 10 seconds
- Home card «ألعاب العائلة» opens the hub. A game tile opens a **ready screen** with the last-used settings already filled in (teams, round length). One big button «ابدأ» is focused, so OK starts the round. Nothing else is asked: no names form, no sign-in, no pairing.
- Team names come from the 4 default teams (الصقور، النجوم، الأسود، الواحة, as in design/43b). Renaming is a secondary link «غيّروا الأسماء» under the button, never in the way.
- Settings (rounds, timer, difficulty, age) are one row of 3 to 4 chips left or right of the button, changed with the D-pad. They are remembered.

## The remote
- Arrows move focus. **OK** confirms or reveals. **Back** opens the pause card (متابعة، إنهاء الجولة، الرئيسية), never quits at once. Long-press is never required.
- OK is the only key needed during play. Every screen shows its next key in words («اضغطوا OK للإجابة»).
- Every touch target is 48 px at 1080p or larger; text for what is read while moving is 28 px or larger.

## Secret words: the hand-over screen («غطّوا عيونكم»)
Used when one person must see a word the others must not (charades, the odd-one-out game, the 5-letter word set-up).
1. **Pass screen.** Full-screen, no secret: «سلّموا الريموت لـ <name>» and «الباقين: غطّوا عيونكم». Focus on «أنا جاهز».
2. OK shows the **secret screen** after a 2-second count (3, 2, 1 is optional) so the holder can lift the remote. It shows the word, large.
3. OK again hides it and returns to the pass screen for the next person, or starts the round.
Rules: the secret screen never shows two secrets at once; Back on a secret screen hides it first and opens the pause card on the next press; if the TV sleeps or the app goes to the background the secret screen is replaced by the pass screen when it comes back.

## Say-aloud scoring
When the answer is spoken, the screen shows the answer only after OK («اضغطوا OK لكشف الجواب»), then two buttons: «صح» (point to the team) and «خطأ» (no point). Left is «خطأ», right is «صح» for RTL reading. The team that answers is the one whose turn it is; the **steal** option (the other team can answer a missed question) is a setting, off by default.

## Phone mode (optional)
- The TV shows a short code and a QR; opening the page joins the player. Phones are used for: secret words (so the remote is not passed), typed or chosen answers, and a buzzer. The TV can be played alone at any moment: a game started with the remote never waits for a phone.
- A phone that drops out does not stop the round: its turn is skipped and the remote flow takes over.

## Data and content
- All word and question data is stored online and loaded per game, not in the APK; every file passes `games/check.py` or its own check (listed in each SPEC).
- Family-safe, no politics, nothing sectarian, no personal data, no «·» separators in any visible text.
- Arabic is the main language; English is carried for every item for the English UI.
