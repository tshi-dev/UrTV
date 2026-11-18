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
        new Game("domino", "دومينو", "الدومينو كما تعرفونه، بالريموت", "DominoActivity", false, true),
        new Game("forehead", "على جبيني", "الجوال على جبينك، والفريق يوصف لك الكلمة", "ForeheadActivity", true, true),
        new Game("author", "من كاتبها؟", "الكل يكتب إجابة، وتخمّنون من كتبها", "AuthorActivity", true, true),
        new Game("closest", "أقرب رقم", "خمّنوا الرقم، والأقرب يكسب", "ClosestActivity", true, true),
        new Game("liar", "واحد يكذب", "ثلاثة يحكون وواحد منهم يكذب، فمن هو؟", "LiarActivity", true, true),
        new Game("emoji", "اقرأ الإيموجي", "إيموجي يدل على كلمة، والأسرع يعرفها", "EmojiActivity", true, true),
        new Game("link", "اربط الكلمات", "تلميح بكلمة واحدة يربط كلمات فريقك على اللوحة", "LinkActivity", true, true),
        new Game("night", "ليل القرية", "ذئاب بين القرويين، من يكشفهم؟", "NightActivity", true, true),
        new Game("draw", "ارسم وخمّن", "ارسم بإصبعك والباقي يخمّن", "DrawActivity", true, true),
        new Game("quran-verse", "كمّل الآية", "أكمل الآية من أربعة خيارات", "QuranVerseActivity", true, true),
        new Game("maths", "سباق الحساب", "أسرع من يحسب أولًا", "MathActivity", true, true),
        new Game("logic", "ألغاز المنطق", "فكّروا قبل أن يسبقكم غيركم", "LogicActivity", true, true),
        new Game("packs", "حزم المحتوى", "حجم ما حمّلتموه من الألعاب، واحتفظوا به أو احذفوه", "GamePacksActivity", false, true),
    };

    /** the games that can be played with the phones (every game that has a phone mode: «الأسرع», the party games and the older games' phase 1): the list's tag and the «بالجوالات» filter use this list, so every game with the phone picture stays when the list is filtered; a game whose phone mode is added later goes here */
    static final String[] PHONE_GAMES = {"quiz", "khanat", "missing-word", "letter-hive", "word5", "odd-one", "charades", "domino", "forehead", "author", "closest", "liar", "emoji", "link", "night", "draw", "quran-verse", "maths", "logic"};
    /** the engine games (GameReleaseStore) are played with phones, and their ids in the hub start with «e:» */
    static boolean inPhones(String id) { if (id.startsWith("e:")) return true; return inPhonesList(id); }
    private static boolean inPhonesList(String id) { for (String g : PHONE_GAMES) if (g.equals(id)) return true; return false; }

    /** the games of this list that are held back: the hub shows one only when the release file (GameReleaseStore.nativeOn) has turned it on for this TV's group; the eight others and «packs» are always shown */
    static final String[] HELD = {"forehead", "author", "closest", "liar", "emoji", "link", "night", "draw", "quran-verse", "maths", "logic"};
    static boolean held(String id) { for (String g : HELD) if (g.equals(id)) return true; return false; }

    static Game byId(String id) { for (Game g : ALL) if (g.id.equals(id)) return g; return null; }

    /** where the list's focus starts, among `list` (what the hub shows now: the engine games are in it when they are on) */
    static int firstFocus(java.util.List<Game> list, String lastId) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).ready && list.get(i).id.equals(lastId)) return i;
        for (int i = 0; i < list.size(); i++) if (list.get(i).ready && !list.get(i).phones) return i;
        return 0;
    }

    /** where the list's focus starts: the game played last when it is still ready, else the first ready game played with no phones (the default way), else the first */
    static int firstFocus(String lastId) {
        for (int i = 0; i < ALL.length; i++) if (ALL[i].ready && ALL[i].id.equals(lastId)) return i;
        for (int i = 0; i < ALL.length; i++) if (ALL[i].ready && !ALL[i].phones) return i;
        return 0;
    }
}
