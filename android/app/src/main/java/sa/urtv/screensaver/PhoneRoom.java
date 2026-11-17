package sa.urtv.screensaver;

/**
 * What the game-only phone server (QuizServer) needs from a room: «الأسرع» is one (QuizRoom), the party games' «اللعب بالجوالات» is the other (GamePhoneRoom).
 * Both are held in memory only; the server routes nothing else (docs/QUIZ.md, docs/GAMES-PHONES.md).
 */
interface PhoneRoom {
    /** the four-digit room code the phone's address carries */
    String code();
    QuizRoom.Result join(String code, String name, int color, String token);
    /** the phone's own state as JSON, in the phone's language */
    String stateFor(String token, boolean en);
    /** `q` is the card or question the phone answers, `a` its choice, `x` free text (unused by most); null = taken, else the short error */
    String answer(String token, int q, int a, String x);
    void leave(String token);
    /** a drawing card's strokes (only the drawing game's room takes them, at /api/draw): null = taken, else the short error */
    default String draw(String token, int q, String x) { return "route"; }
}
