package sa.urtv.screensaver;

/**
 * The settings (4.3.2, the owner's ask: every option in the one section it belongs to). A first screen of ten big buttons and nothing deeper: the city
 * (wide, on top) and the adhan are shortcuts that do one thing at once (a DIRECT button runs the row it names); the eight sections open their rows; the
 * last button is the search. SettingsActivity.render builds every row as before; this says where each one is, by its FocusKey, and in what order
 * ({@link #ORDER}: a header is an entry that starts with H). A row missing from {@link #ROWS} goes with the category of its group's header, so none is ever lost;
 * tests/settings_hub_test.py fails on such a row all the same (every row is named here). The old categories (cards, more, clock, listen…) are
 * {@link #canon}ised to their new section, so a link that still carries one keeps working. Plain Java: tests/SettingsHubRun.java runs it.
 */
final class SettingsHub {
    private SettingsHub() { }

    static final String CITY = "city", ADHAN = "adhan", SEARCH = "search",
            LOOK = "look", HOME = "home", SAVER = "saver", PRAY = "pray", CONTENT = "content", CONTROL = "control", PLACE = "place", UPDATE = "update";

    /**
     * key, name, the line under its title on its own screen, the screen its button is on (null: the first; nothing is deeper), and the row a DIRECT
     * button runs ("" when it opens a screen). In this order on the first screen; the first one is the wide one.
     */
    static final String[][] CATEGORIES = {
        {CITY, "المدينة", "", null, "تغيير المدينة"},   // the wide shortcut: the city is the first thing to find (the same chooser as the row in «المدينة واللغة والوقت»)
        {ADHAN, "الأذان", "", null, "الأذان"},          // a shortcut to the adhan's page; its row is in «الصلاة والأذان والتلاوة»
        {LOOK, "المظهر", "الشكل والألوان والإضاءة، والخلفية والصور، وشكل الساعة والبطاقات، والنص الأوضح وتوهّج التطبيق.", null, ""},
        {HOME, "الشاشة الرئيسية", "بطاقاتها وأزرارها، والتطبيقات المفضلة وترتيبها، وواجهة المصنع، وUrTV النظيف.", null, ""},
        {SAVER, "شاشة التوقف", "ما يظهر على شاشة التوقف: الساعة والبطاقة بجانبها، ومتى تبدأ.", null, ""},
        {PRAY, "الصلاة والأذان والتلاوة", "الأذان وتنبيهاته، وطريقة حساب المواقيت، وشاشة التلاوة.", null, ""},
        {CONTENT, "المباريات والأسواق", "ما تعرضه البطاقات: فرقك ودورياتك، والأسواق والأسهم، وجديد المسلسلات.", null, ""},
        {CONTROL, "التحكم والجوال", "مركز التحكم والتحكم من الجوال، والرقابة الأبوية وتصفية المواقع.", null, ""},
        {PLACE, "المدينة واللغة والوقت", "المدينة والدولة، واللغة، ونظام الساعة ١٢ أو ٢٤.", null, ""},
        {UPDATE, "التحديث وحول", "تحديث التطبيقين وإصدارهما، وتحديث صفحة شاشة التوقف، وإرسال تقرير.", null, ""},
        {SEARCH, "بحث", "", null, "بحث في الإعدادات"},
    };

    /** The old categories (3.x to 4.3.1) and the section each one went to: a stored or passed old key still lands somewhere right. */
    static final String[][] OLD = {
        {"clock", PLACE}, {"listen", PRAY}, {"cards", SAVER}, {"more", CONTENT}, {"apps", HOME}, {"shows", CONTENT},
        {"parental", CONTROL}, {"family", CONTROL}, {"lang", PLACE}, {"phone", CONTROL}, {"report", UPDATE},
    };

    /** A category key as of today: a current one as it is, an old one as its section, anything else null. */
    static String canon(String key) {
        if (key == null) return null;
        for (String[] c : CATEGORIES) if (c[0].equals(key)) return key;
        for (String[] o : OLD) if (o[0].equals(key)) return o[1];
        return null;
    }

    /** each row of SettingsActivity by its FocusKey.key (the name without its numbers), and its category */
    static final String[][] ROWS = {
        {"معاينة الشاشة", SAVER}, {"تغيير المدينة", CITY}, {"الدولة", PLACE}, {"المدينة", PLACE},
        {"طريقة الحساب", PRAY}, {"تعديل وقت الصلاة", PRAY}, {"الفرق المتابعة", CONTENT}, {"تطبيق المباريات", CONTENT},
        {"الدوريات كاملة", CONTENT}, {"رياضات أخرى", CONTENT}, {"الأرقام", LOOK}, {"تنبيه قبل الأذان", PRAY},
        {"الشروق بعد الفجر", PRAY}, {"شريط الوقت بين الصلاتين", PRAY}, {"الرطوبة والرياح", SAVER}, {"أسماء الأشهر الميلادية", LOOK},
        {"شريط السنة تحت الساعة", SAVER}, {"سرعة ألبوم صوري", LOOK}, {"تاريخ الصورة في ألبوم صوري", LOOK}, {"عنوان ألبوم صوري", LOOK},
        {"شكل صورة ألبوم صوري", LOOK}, {"حجم ألبوم صوري", LOOK}, {"نص أوضح", LOOK}, {"توهّج لون التطبيق", LOOK},
        {"المناسبات الوطنية", SAVER}, {"المناسبة القادمة", SAVER}, {"تعتيم الليل", LOOK}, {"حجم الساعة", LOOK},
        {"نظام الساعة", PLACE}, {"فرق التاريخ الهجري", PLACE}, {"إظهار الثواني", LOOK}, {"الخانة اليسرى", SAVER}, {"العدّ التنازلي", SAVER},
        {"صفحة الأسواق", CONTENT}, {"شريط الأسواق", CONTENT}, {"الأسواق", CONTENT}, {"عرض الأسواق", CONTENT},
        {"المحتوى", SAVER}, {"بطاقة ثانية", SAVER}, {"شكل البطاقة", LOOK}, {"تبديل الآية والحديث", SAVER},
        {"الآية والحديث", SAVER}, {"إعداد تلفزيوني", HOME}, {"البطاقة الأولى", HOME}, {"البطاقة الثانية", HOME},
        {"البطاقة الثالثة", HOME}, {"التحية والطقس", HOME}, {"\u2066UrTV\u2069 النظيف", HOME}, {"اجعل UrTV الشاشة الرئيسية", HOME},
        {"تخلّص من واجهة المصنع", HOME}, {"رجّع واجهة المصنع", HOME}, {"المظهر", LOOK}, {"صفحات الأسهم والمباريات داكنة", LOOK},
        {"افتح تلاوة وأذان", PRAY}, {"زر تلاوة وأذان", HOME}, {"زر المهام", HOME}, {"زر الأسواق", HOME},
        {"زر المباريات", HOME}, {"زر الجوال", HOME}, {"زر مركز التحكم", HOME}, {"الرجوع يفتح شاشة التوقف", HOME},
        {"الخروج من تلفزيونك", HOME}, {"أكمل المشاهدة", HOME}, {"جرس التحديثات", HOME}, {"الأذان", PRAY},
        {"تطبيقات في الشريط", HOME}, {"ترتيب التطبيقات المفضلة", HOME}, {"تطبيقات مقترحة", HOME}, {"أسماء المصادر", HOME},
        {"تعديل المفضلة", HOME}, {"ترتيب كل التطبيقات", HOME}, {"قفل التطبيقات", HOME}, {"تفاصيل التطبيقات", HOME},
        {"تحديث تلفزيوني", UPDATE}, {"تحكم من الجوال", CONTROL}, {"مركز التحكم", CONTROL}, {"أرسل تقرير", UPDATE},
        {"الرقابة الأبوية", CONTROL}, {"تصفية المواقع", CONTROL}, {"اللغة", PLACE}, {"مباريات اليوم", CONTENT},
        {"التصنيفات", CONTENT}, {"اللغات", CONTENT}, {"مسلسلات مختارة", CONTENT}, {"أقل تقييم", CONTENT},
        {"قائمة الأسهم", CONTENT}, {"تغيّر الأسهم", CONTENT}, {"تبديل الصورة كل", LOOK}, {"صور شاشة التوقف", LOOK},
        {"صوري", LOOK}, {"بدء شاشة التوقف بعد", SAVER}, {"التحديث التلقائي للصفحة", UPDATE}, {"تحديث صفحة شاشة التوقف", UPDATE},
        {"بحث في الإعدادات", SEARCH},
    };

    /** each group header and the category it belongs to (a row of no name goes with it); the headers that are DRAWN are the header entries of ORDER */
    static final String[][] HEADERS = {
        {"الموقع", PRAY}, {"المباريات", CONTENT}, {"العرض", SAVER}, {"صفحة الأسواق", CONTENT}, {"الأسواق (أعلى اليسار)", CONTENT}, {"بجانب الساعة", SAVER},
        {"الشاشة الرئيسية (تلفزيونك)", HOME}, {"جديد المسلسلات", CONTENT}, {"الأسهم", CONTENT}, {"التوقيت", LOOK}, {"التطبيق", UPDATE},
    };

    /**
     * The rows of each section in the order the person meets them, by FocusKey; H + text is a header drawn above the rows after it. A row of no entry
     * (the prayers' minute rows, made by adjRow) stays right after the entry before it in the code; a section with no entry here keeps the code's order.
     */
    static final String H = "\u0000";   // an ORDER entry that starts with it is a header
    static final String[][] ORDER = {
        {LOOK, "المظهر", "نص أوضح", "توهّج لون التطبيق", "تعتيم الليل", "صفحات الأسهم والمباريات داكنة", "شكل البطاقة", "حجم الساعة", "الأرقام", "أسماء الأشهر الميلادية",
            "إظهار الثواني", H + "الخلفية والصور", "تبديل الصورة كل", "صور شاشة التوقف", "صوري", "سرعة ألبوم صوري", "شكل صورة ألبوم صوري", "حجم ألبوم صوري", "عنوان ألبوم صوري", "تاريخ الصورة في ألبوم صوري"},
        {HOME, "إعداد تلفزيوني", "اجعل UrTV الشاشة الرئيسية", "تخلّص من واجهة المصنع", "رجّع واجهة المصنع", "⁦UrTV⁩ النظيف", "البطاقة الأولى", "البطاقة الثانية",
            "البطاقة الثالثة", "التحية والطقس", "أكمل المشاهدة", "جرس التحديثات", "الرجوع يفتح شاشة التوقف", "الخروج من تلفزيونك", H + "الأزرار", "زر المهام", "زر الأسواق", "زر المباريات",
            "زر الجوال", "زر مركز التحكم", "زر تلاوة وأذان", H + "التطبيقات والمفضلة", "تطبيقات في الشريط", "ترتيب التطبيقات المفضلة", "تعديل المفضلة", "تطبيقات مقترحة", "أسماء المصادر",
            "ترتيب كل التطبيقات", "قفل التطبيقات", "تفاصيل التطبيقات"},
        {SAVER, "معاينة الشاشة", "بدء شاشة التوقف بعد", H + "بجانب الساعة", "المحتوى", "بطاقة ثانية", "تبديل الآية والحديث", "الآية والحديث", "الخانة اليسرى", "العدّ التنازلي",
            "شريط السنة تحت الساعة", "الرطوبة والرياح", "المناسبة القادمة", "المناسبات الوطنية"},
        {PRAY, "الأذان", "تنبيه قبل الأذان", "الشروق بعد الفجر", "شريط الوقت بين الصلاتين", "طريقة الحساب", "تعديل وقت الصلاة", "افتح تلاوة وأذان"},
        {CONTENT, H + "المباريات", "مباريات اليوم", "الفرق المتابعة", "تطبيق المباريات", "الدوريات كاملة", "رياضات أخرى", H + "صفحة الأسواق", "صفحة الأسواق", "شريط الأسواق", "الأسواق", "عرض الأسواق",
            H + "الأسهم", "قائمة الأسهم", "تغيّر الأسهم", H + "جديد المسلسلات", "التصنيفات", "اللغات", "مسلسلات مختارة", "أقل تقييم"},
        {CONTROL, "مركز التحكم", "تحكم من الجوال", "الرقابة الأبوية", "تصفية المواقع"},
        {PLACE, "المدينة", "الدولة", "اللغة", "نظام الساعة", "فرق التاريخ الهجري"},
        {UPDATE, "تحديث تلفزيوني", "التحديث التلقائي للصفحة", "تحديث صفحة شاشة التوقف", "أرسل تقرير"},
    };

    /** A row's place in its section's order: its index in {@link #ORDER}, -1 for none; a header is looked up as H + text. */
    static int rank(String category, String key) {
        for (String[] o : ORDER) if (o[0].equals(category)) for (int i = 1; i < o.length; i++) if (o[i].equals(key)) return i;
        return -1;
    }

    /** The headers of a section, in its order. */
    static java.util.List<String> headers(String category) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String[] o : ORDER) if (o[0].equals(category)) for (int i = 1; i < o.length; i++) if (o[i].startsWith(H)) out.add(o[i].substring(1));
        return out;
    }

    /**
     * Words people type for a row that its name does not hold (the search finds a row by its name, its section's name and these): word, row.
     * Written the way {@link #fold} leaves a word.
     */
    static final String[][] ALIASES = {
        {"صوت", "الأذان"}, {"مؤذن", "الأذان"}, {"فجر", "الأذان"}, {"نوم", "مركز التحكم"}, {"واي فاي", "مركز التحكم"}, {"شبكه", "مركز التحكم"}, {"بلوتوث", "مركز التحكم"},
        {"رمز", "الرقابة الأبوية"}, {"اطفال", "الرقابة الأبوية"}, {"pin", "الرقابة الأبوية"}, {"لون", "المظهر"}, {"داكن", "المظهر"}, {"فاتح", "المظهر"}, {"خلفيه", "صور شاشة التوقف"},
        {"english", "اللغة"}, {"هجري", "فرق التاريخ الهجري"}, {"انجليزي", "اللغة"}, {"نسخه", "تحديث تلفزيوني"}, {"جوال", "تحكم من الجوال"}, {"ريموت", "تحكم من الجوال"}, {"ثواني", "إظهار الثواني"},
        {"مباراه", "مباريات اليوم"}, {"اسهم", "قائمة الأسهم"}, {"ذهب", "صفحة الأسواق"},
    };

    /** Arabic letters in one form for matching: no tashkeel or tatweel, alef/ya/ta-marbuta folded, lower case. */
    static String fold(String t) {
        StringBuilder b = new StringBuilder();
        for (char c : t.toLowerCase(java.util.Locale.ROOT).toCharArray()) {
            if (c >= 0x064B && c <= 0x065F || c == 0x0640 || c == 0x0670 || c == 0x2066 || c == 0x2069) continue;
            if (c == 'أ' || c == 'إ' || c == 'آ' || c == 'ٱ') c = 'ا';
            else if (c == 'ى') c = 'ي';
            else if (c == 'ة') c = 'ه';
            b.append(c);
        }
        return b.toString().trim();
    }

    /**
     * The rows that answer a typed word, best first: the name starts with it, the name holds it, an alias, the section's name holds it. Each result is
     * {row key, category}; never the two shortcut rows that have no screen of their own.
     */
    static java.util.List<String[]> search(String query) {
        String q = fold(query);
        java.util.List<String[]> out = new java.util.ArrayList<>();
        if (q.isEmpty()) return out;
        java.util.List<String[]> starts = new java.util.ArrayList<>(), holds = new java.util.ArrayList<>(), sect = new java.util.ArrayList<>(), alias = new java.util.ArrayList<>();
        for (String[] r : ROWS) {
            if (CITY.equals(r[1]) || SEARCH.equals(r[1])) continue;
            String name = fold(r[0]);
            String[] hit = {r[0], r[1]};
            if (name.startsWith(q)) starts.add(hit);
            else if (name.contains(q)) holds.add(hit);
            else {
                boolean al = false;
                for (String[] a : ALIASES) if (fold(a[0]).startsWith(q) && fold(a[1]).equals(name)) { al = true; break; }
                if (al) alias.add(hit);
                else if (fold(name(r[1])).contains(q)) sect.add(hit);
            }
        }
        out.addAll(starts); out.addAll(holds); out.addAll(alias); out.addAll(sect);
        return out;
    }

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
        for (String[] c : CATEGORIES) if ((screen == null ? c[3] == null : screen.equals(c[3]))) out.add(c[0]);
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
