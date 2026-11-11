package sa.urtv.screensaver;

/**
 * The family games, first to last as the games list shows them (docs/GAMES-KIT.md; no Android here, tests/GamesKitTest.java). Adding a game is ONE line here
 * (plus its activity in the manifest); the list, its tiles and the «قريبًا» mark come from it. `ready` off = the tile says «قريبًا» and opens nothing.
 * `phones` = the game cannot be played without phones (only «الأسرع»): its tile says «بالجوالات»; every other game is played with one remote.
 * The Arabic words are marked for the English table in QuizGamesActivity.WORDS (this file has no S in it).
 */
final class GameRegistry {
    private GameRegistry() {}

    static final class Game {
        /** the id (games/<id>/SPEC.md, the data file's name), the Arabic name, one line about it, the activity's simple class name in this package */
        final String id, name, line, activity; final boolean phones, ready;
        Game(String id, String name, String line, String activity, boolean phones, boolean ready) { this.id = id; this.name = name; this.line = line; this.activity = activity; this.phones = phones; this.ready = ready; }
    }

    static final Game[] ALL = {
        new Game("quiz", "الأسرع", "الكل يجاوب من جواله، والأسرع يكسب", "QuizActivity", true, true),
        new Game("khanat", "خمس خانات", "حرف واحد، وخانة لكل من حيوان ونبات وجماد وبلاد واسم", "KhanatActivity", false, true),
        new Game("missing-word", "الكلمة المفقودة", "جملة ناقصة كلمة، من يكملها؟", "MissingWordActivity", false, true),
        new Game("odd-one", "واحد مو معنا", "الكل يعرف الكلمة إلا واحد، من هو؟", "OddOneActivity", false, true),
        new Game("charades", "بدون كلام", "مثّل الكلمة وفريقك يخمّنها", "CharadesActivity", false, true),
        new Game("letter-hive", "خلية الحروف", "فريقان يتسابقان على وصل الخلية", "LetterHiveActivity", false, true),
        new Game("word5", "خمّن الكلمة", "كلمة من خمسة حروف في ست محاولات", "Word5Activity", false, true),
        new Game("domino", "دومينو", "الدومينو كما تعرفونه، بالريموت", "DominoActivity", false, false),
    };

    static Game byId(String id) { for (Game g : ALL) if (g.id.equals(id)) return g; return null; }

    /** where the list's focus starts: the game played last when it is still ready, else the first ready game played with no phones (the default way), else the first */
    static int firstFocus(String lastId) {
        for (int i = 0; i < ALL.length; i++) if (ALL[i].ready && ALL[i].id.equals(lastId)) return i;
        for (int i = 0; i < ALL.length; i++) if (ALL[i].ready && !ALL[i].phones) return i;
        return 0;
    }
}
