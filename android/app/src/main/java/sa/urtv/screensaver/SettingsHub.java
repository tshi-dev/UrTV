package sa.urtv.screensaver;

/**
 * The settings start small (the owner's ask, the 4.0 prototype in notes/settings-proto): a first screen of big buttons, «الأذان» wide on top
 * and the rest two to a row from the right. A button opens its rows, or a screen of more buttons («البطاقات», «المزيد»), or does one thing
 * at once (a DIRECT one: «الأذان» opens the adhan's page, whose first row is its switch). SettingsActivity.render builds every row as before;
 * this says where each one is, by its FocusKey. A row missing from {@link #ROWS} goes with the category of its group's header, so none is
 * ever lost; tests/settings_hub_test.py fails on such a row all the same (every row is named here). Plain Java: tests/SettingsHubRun.java runs it.
 */
final class SettingsHub {
    private SettingsHub() { }

    static final String ADHAN = "adhan", PLACE = "place", CLOCK = "clock", LISTEN = "listen", CARDS = "cards", LOOK = "look", CONTROL = "control",
            PHONE = "phone", UPDATE = "update", REPORT = "report", MORE = "more",
            SAVER = "saver", HOME = "home",                                 // in «البطاقات»
            CONTENT = "content", APPS = "apps", SHOWS = "shows", FAMILY = "family", PARENTAL = "parental", LANG = "lang";   // in «المزيد»

    /**
     * key, name, the line under its title on its own screen, the screen its button is on (null: the first), and the row a DIRECT button runs
     * ("" when it opens a screen). In this order on each screen; the first one of the first screen is the wide one.
     */
    static final String[][] CATEGORIES = {
        {ADHAN, "الأذان", "", null, "الأذان"},
        {PLACE, "الموقع والصلاة", "مدينتك تحدد الوقت والطقس، وطريقة الحساب تحدد مواقيت الصلاة.", null, ""},
        {CLOCK, "الساعة والأرقام", "نظام الساعة ١٢ أو ٢٤، والأرقام عربية أو إنجليزية، وأسماء الأشهر الميلادية.", null, ""},
        {LISTEN, "تلاوة وأذان", "شاشة التلاوة والأذان، وزرها في الشاشة الرئيسية.", null, ""},
        {CARDS, "البطاقات", "ما يظهر على شاشة التوقف وعلى الشاشة الرئيسية.", null, ""},
        {LOOK, "المظهر والخلفيات", "الشكل والصور لشاشة التوقف وللشاشة الرئيسية.", null, ""},
        {CONTROL, "مركز التحكم", "", null, "مركز التحكم"},
        {PHONE, "التحكم من الجوال", "", null, "تحكم من الجوال"},
        {UPDATE, "تحديث تلفزيوني", "تحديث التطبيقين وإصدارهما، وتحديث صفحة شاشة التوقف.", null, ""},
        {REPORT, "أرسل تقرير", "", null, "أرسل تقرير"},
        {MORE, "المزيد", "المباريات والأسواق، والتطبيقات والمفضلة، وجديد المسلسلات، والرقابة الأبوية، وتصفية المواقع.", null, ""},
        {SAVER, "شاشة التوقف", "ما يظهر على شاشة التوقف: الساعة والبطاقة بجانبها، والصور، ومتى تبدأ.", CARDS, ""},
        {HOME, "الشاشة الرئيسية", "بطاقات الشاشة الرئيسية الثلاث، والتحية والطقس، والأزرار.", CARDS, ""},
        {CONTENT, "المباريات والأسواق", "ما تعرضه البطاقات: فرقك ودورياتك، والأسواق، والأسهم.", MORE, ""},
        {APPS, "التطبيقات والمفضلة", "ترتيب التطبيقات المفضلة وتعديلها، وترتيب كل التطبيقات، وأسماء مصادر التلفزيون مثل PS5.", MORE, ""},
        {SHOWS, "جديد المسلسلات", "ما تعرضه بطاقة المسلسلات: التصنيفات واللغات والمسلسلات المختارة.", MORE, ""},
        {PARENTAL, "الرقابة الأبوية", "", MORE, "الرقابة الأبوية"},
        {FAMILY, "تصفية المواقع", "", MORE, "تصفية المواقع"},
        {LANG, "اللغة", "", MORE, "اللغة"},
    };

    /**
     * «اللغة»'s button in «المزيد»: off until the home app's screens are in English too (docs/ENGLISH.md, phase 3); turned on at integration.
     * Off, the button is not shown and nothing else changes (its row is made but on no screen; `optimize/saver.sh lang` sets it all the same).
     */
    static final boolean LANG_TILE = true;

    /** each row of SettingsActivity by its FocusKey.key (the name without its numbers), and its category */
    static final String[][] ROWS = {
        {"معاينة الشاشة", SAVER},
        {"الدولة", PLACE}, {"المدينة", PLACE}, {"طريقة الحساب", PLACE}, {"تعديل وقت الصلاة", PLACE},
        {"الفرق المتابعة", CONTENT}, {"الدوريات كاملة", CONTENT}, {"رياضات أخرى", CONTENT},
        {"الأرقام", CLOCK}, {"تنبيه قبل الأذان", SAVER}, {"الشروق بعد الفجر", SAVER}, {"شريط الوقت بين الصلاتين", SAVER},
        {"الرطوبة والرياح", SAVER}, {"أسماء الأشهر الميلادية", CLOCK}, {"شريط السنة تحت الساعة", SAVER}, {"صورة كل دقيقة في ألبوم صوري", SAVER}, {"تاريخ الصورة في ألبوم صوري", SAVER}, {"المناسبات الوطنية", SAVER}, {"المناسبة القادمة", SAVER}, {"تعتيم الليل", SAVER},
        {"حجم الساعة", SAVER}, {"نظام الساعة", CLOCK}, {"إظهار الثواني", CLOCK}, {"الخانة اليسرى", SAVER}, {"العدّ التنازلي", SAVER},
        {"صفحة الأسواق", CONTENT}, {"شريط الأسواق", CONTENT}, {"الأسواق", CONTENT}, {"عرض الأسواق", CONTENT},
        {"المحتوى", SAVER}, {"بطاقة ثانية", SAVER}, {"شكل البطاقة", SAVER}, {"تبديل الآية والحديث", SAVER}, {"الآية والحديث", SAVER},
        {"البطاقة الأولى", HOME}, {"البطاقة الثانية", HOME}, {"البطاقة الثالثة", HOME}, {"التحية والطقس", HOME}, {"اجعل UrTV الشاشة الرئيسية", HOME}, {"المظهر والخلفيات", LOOK}, {"المظهر", LOOK}, {"صفحات الأسهم والمباريات داكنة", HOME},
        {"افتح تلاوة وأذان", LISTEN}, {"زر تلاوة وأذان", LISTEN}, {"زر المهام", HOME}, {"زر الأسواق", HOME}, {"زر المباريات", HOME}, {"زر الجوال", HOME}, {"زر مركز التحكم", HOME}, {"الرجوع يفتح شاشة التوقف", HOME}, {"الخروج من تلفزيونك", HOME},
        {"أكمل المشاهدة", HOME}, {"جرس التحديثات", HOME},
        {"الأذان", ADHAN},
        {"تطبيقات في الشريط", APPS}, {"ترتيب التطبيقات المفضلة", APPS}, {"تطبيقات مقترحة", APPS}, {"أسماء المصادر", APPS}, {"تعديل المفضلة", APPS}, {"ترتيب كل التطبيقات", APPS}, {"قفل التطبيقات", APPS}, {"تفاصيل التطبيقات", APPS},
        {"تحديث تلفزيوني", UPDATE}, {"تحكم من الجوال", PHONE}, {"مركز التحكم", CONTROL}, {"أرسل تقرير", REPORT},
        {"الرقابة الأبوية", PARENTAL}, {"تصفية المواقع", FAMILY}, {"اللغة", LANG}, {"مباريات اليوم", CONTENT},
        {"التصنيفات", SHOWS}, {"اللغات", SHOWS}, {"مسلسلات مختارة", SHOWS}, {"أقل تقييم", SHOWS}, {"قائمة الأسهم", CONTENT}, {"تغيّر الأسهم", CONTENT},
        {"تبديل الصورة كل", SAVER}, {"صور شاشة التوقف", SAVER}, {"صوري", SAVER}, {"بدء شاشة التوقف بعد", SAVER},
        {"التحديث التلقائي للصفحة", UPDATE}, {"تحديث صفحة شاشة التوقف", UPDATE},
    };

    /** each group header and the category it belongs to: shown only there, above that category's rows of the group */
    static final String[][] HEADERS = {
        {"الموقع", PLACE}, {"المباريات", CONTENT}, {"العرض", SAVER}, {"صفحة الأسواق", CONTENT}, {"الأسواق (أعلى اليسار)", CONTENT}, {"بجانب الساعة", SAVER},
        {"الشاشة الرئيسية (تلفزيونك)", HOME}, {"جديد المسلسلات", SHOWS}, {"الأسهم", CONTENT}, {"التوقيت", SAVER}, {"التطبيق", UPDATE},
    };

    /** The category of a row (its FocusKey.key); one not named goes with its group's ({@code group}, a header's category), else «شاشة التوقف». */
    static String of(String key, String group) {
        for (String[] r : ROWS) if (r[0].equals(key)) return r[1];
        return group != null ? group : SAVER;
    }

    /** The category of a group header, null for one not named. */
    static String ofHeader(String text) {
        for (String[] h : HEADERS) if (h[0].equals(text)) return h[1];
        return null;
    }

    private static String[] find(String category) {
        for (String[] c : CATEGORIES) if (c[0].equals(category)) return c;
        return null;
    }

    /** A category's name, null for none. */
    static String name(String category) {
        String[] c = find(category);
        return c == null ? null : c[1];
    }

    /** The line under a category's title on its own screen, "" for none. */
    static String about(String category) {
        String[] c = find(category);
        return c == null ? "" : c[2];
    }

    /** The screen a category's button is on: null for the first screen (and for none). */
    static String parent(String category) {
        String[] c = find(category);
        return c == null ? null : c[3];
    }

    /** The row a DIRECT button runs at once, null for a button that opens a screen. */
    static String direct(String category) {
        String[] c = find(category);
        return c == null || c[4].isEmpty() ? null : c[4];
    }

    /** A screen of buttons (the first, «البطاقات», «المزيد»): a category some others are in. */
    static boolean hasTiles(String category) {
        if (category == null) return true;
        for (String[] c : CATEGORIES) if (category.equals(c[3])) return true;
        return false;
    }

    /** The buttons of a screen (null: the first one), in order. */
    static String[] tiles(String screen) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String[] c : CATEGORIES) if ((screen == null ? c[3] == null : screen.equals(c[3])) && (LANG_TILE || !LANG.equals(c[0]))) out.add(c[0]);
        return out.toArray(new String[0]);
    }

    /** Where a category's button is on its screen, -1 for none. */
    static int index(String category) {
        String[] on = tiles(parent(category));
        for (int i = 0; i < on.length; i++) if (on[i].equals(category)) return i;
        return -1;
    }

    /** The first screen's first button is wide, alone on its line; the other screens have none. */
    static boolean wide(String screen) { return screen == null; }

    /**
     * The D-pad on a screen of buttons, two to a row, the first on the right (an Arabic screen); with {@code wide} the first button is alone
     * on the top line: the button a key goes to from button {@code i} of {@code n}, or {@code i} itself at an edge (nothing wraps).
     * dir: 0 up, 1 down, 2 left, 3 right.
     */
    static int next(int i, int n, int dir, boolean wide) {
        if (!wide) return next(i, n, dir);
        if (i == 0) return dir == 1 && n > 1 ? 1 : 0;              // the wide one: down to the right one under it
        int j = next(i - 1, n - 1, dir);                         // the pairs under it, counted from 0
        if (dir == 0 && i - 1 < 2) return 0;                     // up from the first pair: the wide one
        return j + 1;
    }

    /** {@link #next(int, int, int, boolean)} without a wide button. */
    static int next(int i, int n, int dir) {
        int col = i % 2;
        switch (dir) {
            case 0: return i >= 2 ? i - 2 : i;
            case 1: return i + 2 < n ? i + 2 : i;
            case 2: return col == 0 && i + 1 < n ? i + 1 : i;   // the second of a row is on the left of the first
            case 3: return col == 1 ? i - 1 : i;
            default: return i;
        }
    }
}
