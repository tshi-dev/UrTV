package sa.urtv.screensaver;

/**
 * 4.4 «أجزاء UrTV» and «ابدأ بـ» (the owner, docs/mockups/start-presets/): a part of UrTV switched off disappears entirely, from the home, the
 * screensaver, the settings and their search, until it is switched on again; its own settings are kept as they were (nothing here clears a value).
 * Plain Java with no Android in it, so tests run it as it is (tests/parts_test.py).
 *
 * One saved value, «partsOff» (Prefs): the keys of the parts that are off, comma separated. Empty, the default, is every part on, as before: an
 * update changes nothing until the person (or «ابدأ بـ», or `saver.sh part`) turns a part off.
 */
final class Parts {
    private Parts() { }

    static final String PRAYER = "prayer", TILAWAH = "tilawah", MATCHES = "matches", MARKETS = "markets", WEATHER = "weather", CARDS = "cards",
            SAVER = "saver", GAMES = "games", TASKS = "tasks", DUAS = "duas";

    /** key, its name in «أجزاء UrTV», and its line (at most 8 words, docs/ui/SIMPLE.md): what goes with it */
    static final String[][] ALL = {
        {PRAYER, "المواقيت والأذان", "بطاقة الصلاة، والأذان، والإقامة، وتنبيه قبل الأذان"},
        {TILAWAH, "تلاوة وأذان", "القرّاء والإذاعات، و«يُتلى الآن»"},
        {MATCHES, "المباريات والأندية", "مباريات اليوم، وفرقك، وتخطيط كرة القدم"},
        {MARKETS, "الأسواق", "الأسهم، وشريط الأسواق، ومحفظتي"},
        {WEATHER, "الطقس", "الطقس تحت التحية وفي شاشة التوقف"},
        {CARDS, "البطاقات", "البطاقات جنب الساعة وفي الصف"},
        {SAVER, "شاشة التوقف", "متوقفة: تشتغل شاشة توقف التلفزيون نفسه"},
        {GAMES, "الألعاب", "ألعاب العائلة بالريموت والجوالات"},
        {TASKS, "المهام والتذكيرات", "زر المهام، والتذكير على الشاشة بوقته"},
        {DUAS, "الأدعية", "الأدعية والتذكيرات بأوقاتها"},
    };

    /** the settings rows (FocusKey.key, SettingsHub.ROWS) each part takes with it; a row of no part always shows */
    static final String[][] ROWS = {
        {"الأذان", PRAYER}, {"المؤذن", PRAYER}, {"أذاني من ملف", PRAYER}, {"الأذان لكل صلاة", PRAYER}, {"تنبيه قبل الأذان", PRAYER},
        {"صوتياتي", PRAYER}, {"الشروق بعد الفجر", PRAYER}, {"شريط الوقت بين الصلاتين", PRAYER}, {"طريقة الحساب", PRAYER}, {"تعديل وقت الصلاة", PRAYER},
        {"افتح تلاوة وأذان", TILAWAH}, {"زر تلاوة وأذان", TILAWAH},
        {"مباريات اليوم", MATCHES}, {"الفرق المتابعة", MATCHES}, {"تطبيق المباريات", MATCHES}, {"الدوريات كاملة", MATCHES},
        {"بطاقة المباريات لليوم فقط", MATCHES}, {"رياضات أخرى", MATCHES}, {"إخفاء المباريات المنتهية", MATCHES}, {"تنبيه بداية المباراة", MATCHES},
        {"صوت التنبيه", MATCHES}, {"عند الهدف", MATCHES}, {"صافرة النهاية", MATCHES}, {"فوق تطبيق آخر", MATCHES}, {"جرّب التنبيه", MATCHES},
        {"زر المباريات", MATCHES},
        {"افتح صفحة الأسواق", MARKETS}, {"شريط الأسواق", MARKETS}, {"الأسواق", MARKETS}, {"عرض الأسواق", MARKETS}, {"قائمة الأسهم", MARKETS},
        {"تغيّر الأسهم", MARKETS}, {"زر الأسواق", MARKETS},
        {"الرطوبة والرياح", WEATHER}, {"مكان الطقس", WEATHER},
        {"شكل البطاقة", CARDS}, {"حجم البطاقات", CARDS}, {"زوايا البطاقات", CARDS}, {"البطاقة الأولى", CARDS}, {"البطاقة الثانية", CARDS},
        {"البطاقة الثالثة", CARDS}, {"رجّع بطاقاتك", CARDS},
        {"معاينة الشاشة", SAVER}, {"بدء شاشة التوقف بعد", SAVER}, {"تصميم شاشة التوقف", SAVER}, {"حجم الساعة", SAVER}, {"الشريط في الوسط", SAVER},
        {"مكان الشريط", SAVER}, {"المحتوى", SAVER}, {"بطاقة ثانية", SAVER}, {"تبديل الآية والحديث", SAVER}, {"الآية والحديث", SAVER},
        {"الخانة اليسرى", SAVER}, {"العدّ التنازلي", SAVER}, {"شريط السنة تحت الساعة", SAVER}, {"المناسبة القادمة", SAVER}, {"المناسبات الوطنية", SAVER},
        {"الرجوع يفتح شاشة التوقف", SAVER}, {"رجوع مرتين يفتح شاشة التوقف", SAVER}, {"صور شاشة التوقف", SAVER}, {"تحديث صفحة شاشة التوقف", SAVER},
        {"زر المهام", TASKS},
        {"الأدعية والتذكيرات", DUAS},
    };

    /**
     * «ابدأ بـ»: key, name, line, the look it sets (Prefs.PRESETS, "" = the look stays), and the parts it turns off (the others on). The first three
     * show first, «المزيد» holds the rest; «same» is the Back of the screen and the default of an update (nothing changes).
     */
    static final String[][] STARTS = {
        {"clean", "نظيف", "الساعة والتطبيقات، وبس", "clean", "cards,matches,markets,tilawah,prayer,saver,games,tasks,duas"},
        {"full", "كامل", "كل شي، وتختار وش تشيل", "tv", ""},
        {"couch", "كنبة", "ساعة كبيرة في الوسط", "couch", "cards,matches"},
        {"prayer", "مصلّي", "المواقيت والأذان وتلاوة وأذان", "family", "matches,markets,games"},
        {"sport", "رياضة", "مباريات اليوم وفرقك", "sport", "markets,games,duas"},
        {"kids", "للصغار", "تطبيقاتهم والألعاب، والباقي مقفول", "tv", "markets,matches"},
        {"simple", "بسيط", "ساعة كبيرة وخمس تطبيقات على الأقل", "calm", "cards,matches,markets,tilawah,saver,games,tasks,duas,weather"},
        {"same", "خلّه مثل ما هو", "ما يتغيّر شي", "", null},
    };
    /** the three shown first (the owner), then «المزيد» */
    static final String[] FIRST = {"clean", "full", "couch"};

    static String[] start(String key) { for (String[] s : STARTS) if (s[0].equals(key)) return s; return null; }

    static boolean known(String id) { for (String[] p : ALL) if (p[0].equals(id)) return true; return false; }

    /** the saved value made clean: known keys only, each once, in ALL's order */
    static String clean(String off) {
        StringBuilder b = new StringBuilder();
        for (String[] p : ALL) if (has(off, p[0])) b.append(b.length() == 0 ? "" : ",").append(p[0]);
        return b.toString();
    }

    static boolean off(String partsOff, String id) { return has(partsOff, id); }

    /** partsOff with one part turned off or on, clean */
    static String with(String partsOff, String id, boolean isOff) {
        if (!known(id)) return clean(partsOff);
        StringBuilder b = new StringBuilder();
        for (String[] p : ALL) {
            boolean o = p[0].equals(id) ? isOff : has(partsOff, p[0]);
            if (o) b.append(b.length() == 0 ? "" : ",").append(p[0]);
        }
        return b.toString();
    }

    /** the part a settings row goes with, or null */
    static String partOfRow(String row) { for (String[] r : ROWS) if (r[0].equals(row)) return r[1]; return null; }

    /** a row of a part that is off: not built, not found by the search */
    static boolean hidesRow(String partsOff, String row) {
        String p = partOfRow(row);
        return p != null && off(partsOff, p);
    }

    /** the search's results without the rows of the parts that are off */
    static java.util.List<String[]> visible(java.util.List<String[]> found, String partsOff) {
        if (partsOff == null || partsOff.isEmpty()) return found;
        java.util.List<String[]> out = new java.util.ArrayList<>();
        for (String[] r : found) if (!hidesRow(partsOff, r[0])) out.add(r);
        return out;
    }

    /** a section whose every row goes with parts that are off (the screensaver's own section, with «شاشة التوقف» off): no tile for it */
    static boolean hidesSection(String partsOff, java.util.List<String> rowsOfSection) {
        if (partsOff == null || partsOff.isEmpty() || rowsOfSection.isEmpty()) return false;
        for (String r : rowsOfSection) if (!hidesRow(partsOff, r)) return false;
        return true;
    }

    private static boolean has(String list, String id) {
        if (list == null || list.isEmpty()) return false;
        for (String k : list.split(",")) if (k.trim().equals(id)) return true;
        return false;
    }
}
