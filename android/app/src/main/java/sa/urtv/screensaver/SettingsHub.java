package sa.urtv.screensaver;

/**
 * The settings start small (the owner's ask): a first screen of eight categories, each a big tile with its current value, and each
 * opening its own rows. SettingsActivity.render builds every row as before; this says which category each one is in, by its FocusKey.
 * A row missing from {@link #ROWS} goes with the category of its group's header, so none is ever lost; tests/settings_hub_test.py
 * fails on such a row all the same (every row is named here). Plain Java: tests/SettingsHubRun.java runs it.
 */
final class SettingsHub {
    private SettingsHub() { }

    static final String PLACE = "place", ADHAN = "adhan", SAVER = "saver", HOME = "home", CONTENT = "content", APPS = "apps",
            FAMILY = "family", PHONE = "phone";

    /** key, name, the line under its title on its own screen: the first screen in this order, two to a row from the right */
    static final String[][] CATEGORIES = {
        {PLACE, "الموقع والصلاة", "مدينتك تحدد الوقت والطقس، وطريقة الحساب تحدد مواقيت الصلاة."},
        {ADHAN, "الأذان", "الأذان في وقته: تشغيله وصوته ومستواه، والتنبيه قبله على الشاشة."},
        {SAVER, "شاشة التوقف", "ما يظهر على شاشة التوقف: الساعة والبطاقة بجانبها، والأرقام، والصور، ومتى تبدأ."},
        {HOME, "الشاشة الرئيسية", "بطاقات الشاشة الرئيسية الثلاث، والتحية والطقس، وزر تلاوة وأذان."},
        {CONTENT, "المباريات والأسواق", "ما تعرضه البطاقات: فرقك ودورياتك، والأسواق، والمسلسلات، والأسهم."},
        {APPS, "التطبيقات والمفضلة", "ترتيب التطبيقات المفضلة وتعديلها، وترتيب كل التطبيقات، وأسماء مصادر التلفزيون مثل PS5."},
        {FAMILY, "العائلة", "حماية العائلة على الشبكة: تفعيلها أو إيقافها، ويرجع الإعداد القديم تلقائياً."},
        {PHONE, "الجوال والتحديث", "التحكم من الجوال، وتحديث تلفزيوني، وتحديث الشاشة، والإصدار."},
    };

    /** each row of SettingsActivity by its FocusKey.key (the name without its numbers), and its category */
    static final String[][] ROWS = {
        {"معاينة الشاشة", SAVER},
        {"الدولة", PLACE}, {"المدينة", PLACE}, {"طريقة الحساب", PLACE}, {"تعديل وقت الصلاة", PLACE},
        {"الفرق المتابعة", CONTENT}, {"الدوريات كاملة", CONTENT}, {"رياضات أخرى", CONTENT},
        {"الأرقام", SAVER}, {"تنبيه قبل الأذان", ADHAN}, {"الشروق بعد الفجر", SAVER}, {"شريط الوقت بين الصلاتين", SAVER},
        {"الرطوبة والرياح", SAVER}, {"أسماء الأشهر الميلادية", SAVER}, {"شريط السنة تحت الساعة", SAVER}, {"المناسبات الوطنية", SAVER},
        {"حجم الساعة", SAVER}, {"نظام الساعة", SAVER}, {"الخانة اليسرى", SAVER}, {"العدّ التنازلي", SAVER},
        {"شريط الأسواق", CONTENT}, {"الأسواق", CONTENT}, {"عرض الأسواق", CONTENT},
        {"المحتوى", SAVER}, {"بطاقة ثانية", SAVER}, {"شكل البطاقة", SAVER},
        {"البطاقة الأولى", HOME}, {"البطاقة الثانية", HOME}, {"البطاقة الثالثة", HOME}, {"التحية والطقس", HOME}, {"المظهر", HOME}, {"زر تلاوة وأذان", HOME},
        {"أكمل المشاهدة", HOME}, {"جرس التحديثات", HOME},
        {"الأذان", ADHAN},
        {"ترتيب التطبيقات المفضلة", APPS}, {"أسماء المصادر", APPS}, {"تعديل المفضلة", APPS}, {"ترتيب كل التطبيقات", APPS},
        {"تحديث تلفزيوني", PHONE}, {"تحكم من الجوال", PHONE},
        {"حماية العائلة", FAMILY},
        {"التصنيفات", CONTENT}, {"اللغات", CONTENT}, {"مسلسلات مختارة", CONTENT}, {"أقل تقييم", CONTENT}, {"قائمة الأسهم", CONTENT},
        {"تبديل الصورة كل", SAVER}, {"صوري", SAVER}, {"بدء شاشة التوقف بعد", SAVER},
        {"التحديث التلقائي للصفحة", PHONE}, {"تحديث الشاشة الآن", PHONE},
    };

    /** each group header and the category it belongs to: shown only there, above that category's rows of the group */
    static final String[][] HEADERS = {
        {"الموقع", PLACE}, {"المباريات", CONTENT}, {"العرض", SAVER}, {"الأسواق (أعلى اليسار)", CONTENT}, {"بجانب الساعة", SAVER},
        {"الشاشة الرئيسية (تلفزيونك)", HOME}, {"جديد المسلسلات", CONTENT}, {"الأسهم", CONTENT}, {"التوقيت", SAVER}, {"التطبيق", PHONE},
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

    /** A category's name, null for none. */
    static String name(String category) {
        for (String[] c : CATEGORIES) if (c[0].equals(category)) return c[1];
        return null;
    }

    /** The line under a category's title on its own screen, "" for none. */
    static String about(String category) {
        for (String[] c : CATEGORIES) if (c[0].equals(category)) return c[2];
        return "";
    }

    /** Where a category's tile is on the first screen, -1 for none. */
    static int index(String category) {
        for (int i = 0; i < CATEGORIES.length; i++) if (CATEGORIES[i][0].equals(category)) return i;
        return -1;
    }

    /**
     * The D-pad on the first screen, two tiles to a row, the first on the right (an Arabic screen): the tile a key goes to from tile
     * {@code i} of {@code n}, or {@code i} itself at an edge (nothing wraps). dir: 0 up, 1 down, 2 left, 3 right.
     */
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
