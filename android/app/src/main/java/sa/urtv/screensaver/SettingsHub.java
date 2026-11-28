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

    static final String SEARCH = "search", LANG = "lang", PLACE = "place", LOOK = "look", APPS = "apps", BUTTONS = "buttons", PRAY = "pray",
            CONTENT = "content", SAVER = "saver", FAMILY = "family", UPDATE = "update", PHOTOS = "photos";

    /**
     * key, name, the line under its title on its own screen, the screen its button is on (null: the first; nothing is deeper), and the row a DIRECT
     * button runs ("" when it opens a screen). In this order on the first screen; the first one (the search) is the wide one. 4.4 beta 4 (the owner,
     * docs/research/SETTINGS-FINDABILITY.md): nine sections, the most used first, each named for what is in it; the city in one of them only.
     */
    static final String[][] CATEGORIES = {
        {SEARCH, "بحث في الإعدادات", "", null, "بحث في الإعدادات"},
        {PRAY, "الصلاة والأذان", "الأذان والمؤذن وأذان كل صلاة، والتنبيهات والأدعية، وطريقة حساب المواقيت.", null, ""},
        {PHOTOS, "صوري والخلفيات", "إضافة صورك وحذفها، وألبوم صوري، وخلفية الشاشة الرئيسية وصور شاشة التوقف.", null, ""},
        {LOOK, "الشكل", "أشكال جاهزة والتخطيط، والألوان والإضاءة، والبطاقات، والساعة، والتطبيقات.", null, ""},
        {SAVER, "شاشة التوقف", "متى تبدأ، وتصميمها، وما بجانب الساعة، وفتحها بزر الرجوع.", null, ""},
        {APPS, "التطبيقات والأزرار", "واجهة التلفزيون، وشريط التطبيقات والمخفية والأقفال، والأزرار السريعة وأزرار الريموت.", null, ""},
        {CONTENT, "المباريات والأسواق", "فرقك ودورياتك، والأسواق والأسهم، وجديد المسلسلات.", null, ""},
        {FAMILY, "العائلة والجوال", "الرقابة الأبوية، وتصفية المواقع للعائلة، والجوالات المسموح لها.", null, ""},
        {LANG, "اللغة والموقع", "لغة التلفزيون والأرقام والتقويم، والمدينة ومنها الطقس ومواقيت الصلاة.", null, ""},
        {UPDATE, "حول والتحديث", "الإصدار والتحديث، وإعداد التلفزيون، وإرسال تقرير، والأداء.", null, ""},
    };

    /** The old categories (3.x to 4.3.2) and the section each one went to: a stored or passed old key still lands somewhere right. */
    static final String[][] OLD = {
        {PLACE, LANG}, {BUTTONS, APPS},
        {"clock", LANG}, {"listen", PRAY}, {"adhan", PRAY}, {"cards", SAVER}, {"more", CONTENT}, {"shows", CONTENT}, {"home", APPS}, {"city", LANG},
        {"parental", FAMILY}, {"phone", FAMILY}, {"control", APPS}, {"report", UPDATE},
    };

    /** A category key as of today: a current one as it is, an old one as its section, anything else null. */
    static String canon(String key) {
        if (key == null) return null;
        for (String[] c : cats()) if (c[0].equals(key)) return key;
        for (String[] o : OLD) if (o[0].equals(key)) return o[1];
        return null;
    }

    /** each row of SettingsActivity by its FocusKey.key (the name without its numbers), and its category */
    static final String[][] ROWS = {
        {"الأذان", PRAY}, {"المؤذن", PRAY}, {"أذاني من ملف", PRAY}, {"الأذان لكل صلاة", PRAY}, {"تنبيه قبل الأذان", PRAY}, {"الأدعية والتذكيرات", PRAY}, {"صوتياتي", PRAY}, 
        {"الشروق بعد الفجر", PRAY}, {"شريط الوقت بين الصلاتين", PRAY}, {"طريقة الحساب", PRAY}, {"تعديل وقت الصلاة", PRAY}, {"افتح تلاوة وأذان", PRAY},
        {"أضف صوري من الجوال", PHOTOS}, {"أضف من فلاشة أو التنزيلات", PHOTOS}, {"احذف صورة", PHOTOS}, {"صوري", PHOTOS}, {"سرعة ألبوم صوري", PHOTOS},
        {"شكل صورة ألبوم صوري", PHOTOS}, {"حجم ألبوم صوري", PHOTOS}, {"عنوان ألبوم صوري", PHOTOS}, {"تاريخ الصورة في ألبوم صوري", PHOTOS}, {"الخلفية", PHOTOS},
        {"حزم الآفاق", PHOTOS}, {"بقاء كل أفق", PHOTOS}, {"الأفق يتبع الوقت", PHOTOS}, {"الأفق يتبع الطقس", PHOTOS}, {"صور شاشة التوقف", PHOTOS},
        {"تبديل الصورة كل", PHOTOS}, {"المظهر على الرئيسية", LOOK}, {"رجّع الشكل السابق", LOOK}, {"أشكال جاهزة", LOOK}, {"شكل شاشة الإعدادات", LOOK}, {"تخصيص", LOOK},
        {"رجّع بطاقاتك", LOOK}, {"شكل كنبة", LOOK}, {"التخطيط", LOOK}, {"الألوان والإضاءة", LOOK}, {"صفحات الأسهم والمباريات داكنة", LOOK},
        {"لوحة المظهر على الرئيسية", LOOK}, {"زر رجّع الشكل السابق", LOOK}, {"تبديل الشكل بلا وميض", LOOK}, {"شكل البطاقة", LOOK}, {"حجم البطاقات", LOOK},
        {"زوايا البطاقات", LOOK}, {"البطاقة الأولى", LOOK}, {"البطاقة الثانية", LOOK}, {"البطاقة الثالثة", LOOK}, {"كمّل المشاهدة", LOOK}, {"الساعة", LOOK},
        {"إظهار الثواني", LOOK}, {"نص أوضح", LOOK}, {"تعتيم الليل", LOOK}, {"شكل التطبيقات", LOOK}, {"محاذاة التطبيقات", LOOK}, {"حجم شريط tvOS", LOOK},
        {"صفوف شريط tvOS", LOOK}, {"توهّج لون التطبيق", LOOK}, {"معاينة الشاشة", SAVER}, {"بدء شاشة التوقف بعد", SAVER}, {"تصميم شاشة التوقف", SAVER},
        {"حجم الساعة", SAVER}, {"الشريط في الوسط", SAVER}, {"مكان الشريط", SAVER}, {"مكان الطقس", SAVER}, {"المحتوى", SAVER}, {"بطاقة ثانية", SAVER},
        {"تبديل الآية والحديث", SAVER}, {"الآية والحديث", SAVER}, {"الخانة اليسرى", SAVER}, {"العدّ التنازلي", SAVER}, {"شريط السنة تحت الساعة", SAVER},
        {"المناسبة القادمة", SAVER}, {"المناسبات الوطنية", SAVER}, {"الرجوع يفتح شاشة التوقف", SAVER}, {"رجوع مرتين يفتح شاشة التوقف", SAVER},
        {"خلّ UrTV هي الشاشة الرئيسية", APPS}, {"تخلّص من واجهة المصنع", APPS}, {"رجّع واجهة المصنع", APPS}, {"قبل ما تحذف التطبيق…", APPS},
        {"\u2066UrTV\u2069 النظيف", APPS}, {"الخروج من تلفزيونك", APPS}, {"تطبيقات في الشريط", APPS}, {"ترتيب التطبيقات المفضلة", APPS}, {"تعديل المفضلة", APPS},
        {"تطبيقات مقترحة", APPS}, {"ترتيب كل التطبيقات", APPS}, {"التطبيقات المخفية", APPS}, {"أسماء المصادر", APPS}, {"قفل التطبيقات", APPS}, {"تفاصيل التطبيقات", APPS},
        {"مركز التحكم", APPS}, {"زر مركز التحكم", APPS}, {"زر الجوال", APPS}, {"زر البحث", APPS}, {"زر المهام", APPS}, {"زر المصادر", APPS}, {"زر الأسواق", APPS},
        {"زر المباريات", APPS}, {"زر تلاوة وأذان", APPS}, {"جرس التحديثات", APPS}, {"زر الرئيسية يفتح تلفزيونك مباشرة", APPS}, {"زر يوتيوب في الريموت", APPS},
        {"زر نتفليكس في الريموت", APPS}, {"زر Prime Video في الريموت", APPS}, {"زر ديزني بلس في الريموت", APPS}, {"زر Paramount Plus في الريموت", APPS},
        {"زر Free TV في الريموت", APPS}, {"الزر المخصص ★ في الريموت", APPS}, {"زر BRAVIA CORE في الريموت", APPS}, {"زر Crunchyroll في الريموت", APPS},
        {"زر Google Play في الريموت", APPS}, {"زر SonyLIV في الريموت", APPS}, {"زر Tabii في الريموت", APPS}, {"زر TCL Channel في الريموت", APPS},
        {"زر Okko في الريموت", APPS}, {"زر VK في الريموت", APPS}, {"زر Rutube في الريموت", APPS}, {"زر KION في الريموت", APPS}, {"زر Wink في الريموت", APPS},
        {"زر IVI في الريموت", APPS}, {"مباريات اليوم", CONTENT}, {"الفرق المتابعة", CONTENT}, {"تطبيق المباريات", CONTENT}, {"الدوريات كاملة", CONTENT},
        {"بطاقة المباريات لليوم فقط", CONTENT}, {"رياضات أخرى", CONTENT}, {"إخفاء المباريات المنتهية", CONTENT}, {"تنبيه بداية المباراة", CONTENT},
        {"صوت التنبيه", CONTENT}, {"عند الهدف", CONTENT}, {"صافرة النهاية", CONTENT}, {"فوق تطبيق آخر", CONTENT}, {"جرّب التنبيه", CONTENT},
        {"افتح صفحة الأسواق", CONTENT}, {"شريط الأسواق", CONTENT}, {"الأسواق", CONTENT}, {"عرض الأسواق", CONTENT}, {"قائمة الأسهم", CONTENT}, {"تغيّر الأسهم", CONTENT},
        {"التصنيفات", CONTENT}, {"اللغات", CONTENT}, {"مسلسلات مختارة", CONTENT}, {"أقل تقييم", CONTENT}, {"الرقابة الأبوية", FAMILY}, {"تصفية المواقع", FAMILY},
        {"الجوالات المسموح لها", FAMILY}, {"اللغة", LANG}, {"المدينة", LANG}, {"الدولة", LANG}, {"الأرقام", LANG}, {"التاريخ بالأرقام", LANG}, {"التاريخ الهجري", LANG},
        {"أسماء الأشهر الميلادية", LANG}, {"نظام الساعة", LANG}, {"فرق التاريخ الهجري", LANG}, {"التحية والطقس", LANG}, {"اسمك في التحية", LANG},
        {"الرطوبة والرياح", LANG}, {"تحديث تلفزيوني", UPDATE}, {"التحديث التلقائي للصفحة", UPDATE}, {"تحديث صفحة شاشة التوقف", UPDATE}, {"النسخ التجريبية", UPDATE},
        {"أرسل ملاحظة عن النسخة التجريبية", UPDATE}, {"إعداد تلفزيوني", UPDATE}, {"أرسل تقرير", UPDATE}, {"مصادر البيانات", UPDATE}, {"الوضع الخفيف", UPDATE},
        {"مسح الملفات المؤقتة", UPDATE}, {"بحث في الإعدادات", SEARCH},
    };

    /** each group header and the category it belongs to (a row of no name goes with it); the headers that are DRAWN are the header entries of ORDER */
    static final String[][] HEADERS = {
        {"الموقع", PRAY}, {"المباريات", CONTENT}, {"العرض", SAVER}, {"صفحة الأسواق", CONTENT}, {"الأسواق (أعلى اليسار)", CONTENT}, {"بجانب الساعة", SAVER},
        {"الشاشة الرئيسية (تلفزيونك)", APPS}, {"جديد المسلسلات", CONTENT}, {"الأسهم", CONTENT}, {"التوقيت", PHOTOS}, {"التطبيق", UPDATE},
    };

    /**
     * The rows of each section in the order the person meets them, by FocusKey; H + text is a header drawn above the rows after it. A row of no entry
     * (the prayers' minute rows, made by adjRow) stays right after the entry before it in the code; a section with no entry here keeps the code's order.
     */
    static final String H = "\u0000";   // an ORDER entry that starts with it is a header
    static final String[][] ORDER = {
        {PRAY, "الأذان", "المؤذن", "أذاني من ملف", "الأدعية والتذكيرات", "الأذان لكل صلاة", "تنبيه قبل الأذان", "صوتياتي", "الشروق بعد الفجر", "شريط الوقت بين الصلاتين", H + "المواقيت", "طريقة الحساب", "تعديل وقت الصلاة", H + "تلاوة وأذان", "افتح تلاوة وأذان"},
        {PHOTOS, H + "صوري", "أضف صوري من الجوال", "أضف من فلاشة أو التنزيلات", "احذف صورة", "صوري", H + "ألبوم صوري", "سرعة ألبوم صوري", "شكل صورة ألبوم صوري", "حجم ألبوم صوري", "عنوان ألبوم صوري", "تاريخ الصورة في ألبوم صوري", H + "الخلفيات", "الخلفية", "حزم الآفاق", "بقاء كل أفق", "الأفق يتبع الوقت", "الأفق يتبع الطقس", "صور شاشة التوقف", "تبديل الصورة كل"},
        {LOOK, H + "أشكال جاهزة والتخطيط", "المظهر على الرئيسية", "رجّع الشكل السابق", "أشكال جاهزة", "تخصيص", "رجّع بطاقاتك", "شكل كنبة", "التخطيط", "الألوان والإضاءة", H + "البطاقات", "شكل البطاقة", "حجم البطاقات", "زوايا البطاقات", "البطاقة الأولى", "البطاقة الثانية", "البطاقة الثالثة", "كمّل المشاهدة", H + "الساعة والنص", "الساعة", "إظهار الثواني", "نص أوضح", "تعتيم الليل", H + "التطبيقات", "شكل التطبيقات", "محاذاة التطبيقات", "حجم شريط tvOS", "صفوف شريط tvOS", "توهّج لون التطبيق", H + "خيارات أخرى للشكل", "شكل شاشة الإعدادات", "صفحات الأسهم والمباريات داكنة", "لوحة المظهر على الرئيسية", "زر رجّع الشكل السابق", "تبديل الشكل بلا وميض"},
        {SAVER, "معاينة الشاشة", "بدء شاشة التوقف بعد", "تصميم شاشة التوقف", "حجم الساعة", "الشريط في الوسط", "مكان الشريط", "مكان الطقس", H + "بجانب الساعة", "المحتوى", "بطاقة ثانية", "تبديل الآية والحديث", "الآية والحديث", "الخانة اليسرى", "العدّ التنازلي", "شريط السنة تحت الساعة", "المناسبة القادمة", "المناسبات الوطنية", H + "فتحها بزر الرجوع", "الرجوع يفتح شاشة التوقف", "رجوع مرتين يفتح شاشة التوقف"},
        {APPS, H + "الشاشة الرئيسية للتلفزيون", "خلّ UrTV هي الشاشة الرئيسية", "تخلّص من واجهة المصنع", "رجّع واجهة المصنع", "قبل ما تحذف التطبيق…", "⁦UrTV⁩ النظيف", "الخروج من تلفزيونك", H + "شريط التطبيقات", "التطبيقات المخفية", "تطبيقات في الشريط", "ترتيب التطبيقات المفضلة", "تعديل المفضلة", "تطبيقات مقترحة", "ترتيب كل التطبيقات", "أسماء المصادر", "قفل التطبيقات", "تفاصيل التطبيقات", H + "الأزرار السريعة", "مركز التحكم", "زر مركز التحكم", "زر الجوال", "زر البحث", "زر المهام", "زر المصادر", "زر الأسواق", "زر المباريات", "زر تلاوة وأذان", "جرس التحديثات", "زر الرئيسية يفتح تلفزيونك مباشرة", H + "أزرار الريموت", "زر يوتيوب في الريموت", "زر نتفليكس في الريموت", "زر Prime Video في الريموت", "زر ديزني بلس في الريموت", "زر Paramount Plus في الريموت", "زر Free TV في الريموت", "الزر المخصص ★ في الريموت", "زر BRAVIA CORE في الريموت", "زر Crunchyroll في الريموت", "زر Google Play في الريموت", "زر SonyLIV في الريموت", "زر Tabii في الريموت", "زر TCL Channel في الريموت", "زر Okko في الريموت", "زر VK في الريموت", "زر Rutube في الريموت", "زر KION في الريموت", "زر Wink في الريموت", "زر IVI في الريموت"},
        {CONTENT, H + "المباريات", "مباريات اليوم", "الفرق المتابعة", "تطبيق المباريات", "الدوريات كاملة", "بطاقة المباريات لليوم فقط", "رياضات أخرى", "إخفاء المباريات المنتهية", "تنبيه بداية المباراة", "صوت التنبيه", "عند الهدف", "صافرة النهاية", "فوق تطبيق آخر", "جرّب التنبيه", H + "صفحة الأسواق", "افتح صفحة الأسواق", "شريط الأسواق", "الأسواق", "عرض الأسواق", H + "الأسهم", "قائمة الأسهم", "تغيّر الأسهم", H + "جديد المسلسلات", "التصنيفات", "اللغات", "مسلسلات مختارة", "أقل تقييم"},
        {FAMILY, "الرقابة الأبوية", "تصفية المواقع", "الجوالات المسموح لها"},
        {LANG, "اللغة", "المدينة", "الدولة", "الأرقام", "التاريخ بالأرقام", "التاريخ الهجري", "أسماء الأشهر الميلادية", "نظام الساعة", "فرق التاريخ الهجري", H + "الطقس", "التحية والطقس", "اسمك في التحية", "الرطوبة والرياح"},
        {UPDATE, "تحديث تلفزيوني", "التحديث التلقائي للصفحة", "تحديث صفحة شاشة التوقف", "النسخ التجريبية", "أرسل ملاحظة عن النسخة التجريبية", "إعداد تلفزيوني", "أرسل تقرير", "مصادر البيانات", H + "الأداء", "الوضع الخفيف", "مسح الملفات المؤقتة"},
    };


    // ---- 4.4 beta 4 (the owner, the findability review: docs/research/SETTINGS-FINDABILITY.md): ONE tree for every settings style. «أزرار» and tvOS
    // show the sections above as their first screen of buttons (the search the wide one), «كنبة» as its side list; nothing is deeper than section ←
    // row ← its picker. The 4.3.2 keys a link or a saved screen may still carry are canon()ised by OLD.

    /** 4.4: the 4.3.2 screen is the one shown (Prefs.couchOn() false); SettingsActivity sets it before it builds anything */
    static volatile boolean classic;
    static void classic(boolean on) { classic = on; }   // the layout only (buttons or a side list): the tree is the same
    private static String[][] cats() { return CATEGORIES; }
    private static String[][] rows() { return ROWS; }
    private static String[][] order() { return ORDER; }
    private static String[][] heads() { return HEADERS; }

    /** A row's place in its section's order: its index in {@link #ORDER}, -1 for none; a header is looked up as H + text. */
    static int rank(String category, String key) {
        for (String[] o : order()) if (o[0].equals(category)) for (int i = 1; i < o.length; i++) if (o[i].equals(key)) return i;
        return -1;
    }

    /** The headers of a section, in its order. */
    static java.util.List<String> headers(String category) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String[] o : order()) if (o[0].equals(category)) for (int i = 1; i < o.length; i++) if (o[i].startsWith(H)) out.add(o[i].substring(1));
        return out;
    }

    /**
     * Words people type for a row that its name does not hold (the search finds a row by its name, its section's name and these): word, row.
     * Written the way {@link #fold} leaves a word.
     */
    static final String[][] ALIASES = {
        {"صوت", "الأذان"}, {"مؤذن", "المؤذن"}, {"فجر", "الأذان"}, {"اذان الفجر", "الأذان لكل صلاة"}, {"كتم الاذان", "الأذان لكل صلاة"}, {"الاذان في الشاشه", "الأذان لكل صلاة"}, {"نوم", "مركز التحكم"}, {"واي فاي", "مركز التحكم"}, {"شبكه", "مركز التحكم"}, {"بلوتوث", "مركز التحكم"},
        {"رمز", "الرقابة الأبوية"}, {"اطفال", "الرقابة الأبوية"}, {"pin", "الرقابة الأبوية"}, {"لون", "الألوان والإضاءة"}, {"داكن", "الألوان والإضاءة"}, {"فاتح", "الألوان والإضاءة"}, {"خلفيه", "صور شاشة التوقف"},
        {"english", "اللغة"}, {"هجري", "فرق التاريخ الهجري"}, {"انجليزي", "اللغة"}, {"نسخه", "تحديث تلفزيوني"}, {"جوال", "الجوالات المسموح لها"}, {"ريموت", "الجوالات المسموح لها"}, {"تحكم من الجوال", "الجوالات المسموح لها"}, {"ثواني", "إظهار الثواني"},
        {"مباراه", "مباريات اليوم"}, {"اسهم", "قائمة الأسهم"}, {"ذهب", "افتح صفحة الأسواق"},
        {"الشاشه الرئيسيه", "خلّ UrTV هي الشاشة الرئيسية"}, {"واجهه المصنع", "تخلّص من واجهة المصنع"}, {"مشاهد", "حزم الآفاق"}, {"مشهد", "حزم الآفاق"},
        // 4.4: deleting a photo is in «صوري» (one at a time or all); the hidden apps come back in «ترتيب كل التطبيقات» ← «تعديل التطبيقات» ← «المخفية»
        // 4.5 how people ask (a user: «can I remove the Ramadan countdown?»): the next occasion's line is Ramadan, Eid, iftar and suhoor
        {"شكل الاعدادات", "شكل شاشة الإعدادات"}, {"القائمه الجانبيه", "شكل شاشة الإعدادات"}, {"الازرار", "شكل شاشة الإعدادات"}, {"ازرار", "شكل شاشة الإعدادات"},
        {"رمضان", "المناسبة القادمة"}, {"العيد", "المناسبة القادمة"}, {"الأعياد", "المناسبة القادمة"}, {"العد التنازلي", "المناسبة القادمة"}, {"كم باقي", "المناسبة القادمة"},
        {"الإفطار", "المناسبة القادمة"}, {"الإمساك", "المناسبة القادمة"}, {"السحور", "المناسبة القادمة"},
        {"اليوم الوطني", "المناسبات الوطنية"}, {"يوم التأسيس", "المناسبات الوطنية"},
        {"الأسهم", "الأسواق"}, {"تاسي", "الأسواق"}, {"tasi", "الأسواق"}, {"البورصة", "الأسواق"}, {"سوق", "الأسواق"}, {"عملات", "الأسواق"}, {"دولار", "الأسواق"},
        {"سكرين سيفر", "بدء شاشة التوقف بعد"}, {"سكرين سيفر", "تصميم شاشة التوقف"}, {"screensaver", "بدء شاشة التوقف بعد"}, {"screensaver", "تصميم شاشة التوقف"},
        {"ثيم", "أشكال جاهزة"}, {"theme", "أشكال جاهزة"}, {"ستايل", "أشكال جاهزة"}, {"ثيم", "الألوان والإضاءة"},
        {"حرارة", "التحية والطقس"}, {"قرآن", "الآية والحديث"}, {"قفل", "الرقابة الأبوية"}, {"رقم سري", "الرقابة الأبوية"},
        {"سطوع", "تعتيم الليل"}, {"ساعة كبيرة", "حجم الساعة"},
        {"حذف الصور", "احذف صورة"}, {"حذف صوره", "احذف صورة"}, {"مسح صوره", "احذف صورة"}, {"شيل صوره", "احذف صورة"}, {"صوري", "صوري"},
        {"اضف صور", "أضف صوري من الجوال"}, {"اضافه صور", "أضف صوري من الجوال"}, {"رفع صور", "أضف صوري من الجوال"}, {"صور من الجوال", "أضف صوري من الجوال"},
        {"المخفيه", "التطبيقات المخفية"}, {"اظهار التطبيقات المخفيه", "التطبيقات المخفية"}, {"رجع تطبيق", "التطبيقات المخفية"}, {"تعديل التطبيقات", "ترتيب كل التطبيقات"},
        // 4.4 beta 4 (the findability review): what lives in «تلاوة» (the adhan's own screen) is found from here and opens there; the words people use
        {"اذاني", "أذاني من ملف"}, {"اذان خاص", "أذاني من ملف"}, {"مستوي صوت الاذان", "الأذان"}, {"الاقامه", "الأذان"}, {"تنبيه عند الاذان", "الأذان"}, {"اذان الجمعه", "الأذان"}, {"الاذان المختصر", "الأذان"},
        {"دعاء", "الأدعية والتذكيرات"}, {"ادعيه", "الأدعية والتذكيرات"}, {"تذكير", "الأدعية والتذكيرات"}, {"تذكيرات", "الأدعية والتذكيرات"}, {"صوتياتي", "صوتياتي"}, {"ملف صوت", "صوتياتي"}, {"اذان من ملف", "أذاني من ملف"}, {"تسجيل اذان", "أذاني من ملف"}, {"المؤذن", "المؤذن"},
        {"فلاشه", "أضف من فلاشة أو التنزيلات"}, {"usb", "أضف من فلاشة أو التنزيلات"}, {"يو اس بي", "أضف من فلاشة أو التنزيلات"}, {"التنزيلات", "أضف من فلاشة أو التنزيلات"}, {"فلاشه", "الأذان"}, {"usb", "الأذان"},
        // «الألوان والإضاءة» opens its own screen («المظهر»): what is on it
        {"شفافيه", "الألوان والإضاءة"}, {"لون التمييز", "الألوان والإضاءة"}, {"شكل التركيز", "الألوان والإضاءة"}, {"تعتيم الخلفيه", "الألوان والإضاءة"}, {"صور الرئيسيه", "الألوان والإضاءة"}, {"وضع الالوان", "الألوان والإضاءة"},
        {"زوايا", "زوايا البطاقات"}, {"حجم البطاقه", "حجم البطاقات"}, {"كبر البطاقات", "حجم البطاقات"},
        {"جعفري", "طريقة الحساب"}, {"شيعه", "طريقة الحساب"}, {"ام القري", "طريقة الحساب"},
    };

    /**
     * 4.4 the owner: where each row shows: "home" (the home screen), "saver" (the screensaver), "both", or "tv" (the whole TV, not a screen of its own).
     * SettingsActivity.row shows it at the row's end; docs/SETTINGS-TREE.md beside the row. tests/settings_scope_test.py: every row has one.
     */
    static final String[][] SCOPES = {
        {"التاريخ بالأرقام", "both"}, {"التاريخ الهجري", "both"}, {"الوضع الخفيف", "both"}, {"زر البحث", "home"}, {"إخفاء المباريات المنتهية", "home"}, {"تنبيه بداية المباراة", "both"}, {"صوت التنبيه", "both"}, {"عند الهدف", "both"}, {"صافرة النهاية", "both"}, {"فوق تطبيق آخر", "tv"}, {"جرّب التنبيه", "both"},
        {"اللغة", "tv"}, {"الأرقام", "both"}, {"أسماء الأشهر الميلادية", "both"}, {"نظام الساعة", "both"}, {"فرق التاريخ الهجري", "both"},
        {"المدينة", "tv"}, {"الدولة", "tv"}, {"التحية والطقس", "home"}, {"اسمك في التحية", "home"}, {"الرطوبة والرياح", "saver"},
        {"المظهر على الرئيسية", "home"}, {"لوحة المظهر على الرئيسية", "home"}, {"زر رجّع الشكل السابق", "both"}, {"تبديل الشكل بلا وميض", "home"}, {"رجّع الشكل السابق", "both"}, {"أشكال جاهزة", "both"}, {"شكل شاشة الإعدادات", "tv"}, {"تخصيص", "both"}, {"رجّع بطاقاتك", "home"}, {"شكل كنبة", "home"}, {"التخطيط", "home"}, {"الألوان والإضاءة", "home"}, {"صفحات الأسهم والمباريات داكنة", "home"},
        {"الخلفية", "home"}, {"حزم الآفاق", "home"}, {"بقاء كل أفق", "home"}, {"الأفق يتبع الوقت", "home"}, {"الأفق يتبع الطقس", "home"},
        {"الساعة", "home"}, {"حجم الساعة", "saver"}, {"تصميم شاشة التوقف", "saver"}, {"الشريط في الوسط", "saver"}, {"مكان الشريط", "saver"}, {"مكان الطقس", "saver"}, {"إظهار الثواني", "both"}, {"نص أوضح", "home"}, {"تعتيم الليل", "saver"},
        {"شكل التطبيقات", "home"}, {"محاذاة التطبيقات", "home"}, {"حجم شريط tvOS", "home"}, {"صفوف شريط tvOS", "home"}, {"توهّج لون التطبيق", "home"},
        {"شكل البطاقة", "both"}, {"البطاقة الأولى", "home"}, {"البطاقة الثانية", "home"}, {"البطاقة الثالثة", "home"}, {"كمّل المشاهدة", "home"},
        {"صور شاشة التوقف", "both"}, {"صوري", "both"}, {"تبديل الصورة كل", "saver"},
        {"سرعة ألبوم صوري", "both"}, {"شكل صورة ألبوم صوري", "both"}, {"حجم ألبوم صوري", "both"}, {"عنوان ألبوم صوري", "both"}, {"تاريخ الصورة في ألبوم صوري", "both"},
        {"خلّ UrTV هي الشاشة الرئيسية", "tv"}, {"تخلّص من واجهة المصنع", "tv"}, {"رجّع واجهة المصنع", "tv"}, {"قبل ما تحذف التطبيق…", "tv"}, {"\u2066UrTV\u2069 النظيف", "both"},
        {"الخروج من تلفزيونك", "tv"}, {"تطبيقات في الشريط", "home"}, {"ترتيب التطبيقات المفضلة", "home"}, {"تعديل المفضلة", "home"}, {"تطبيقات مقترحة", "home"},
        {"ترتيب كل التطبيقات", "home"}, {"أسماء المصادر", "home"}, {"قفل التطبيقات", "tv"}, {"تفاصيل التطبيقات", "tv"},
        {"مركز التحكم", "home"}, {"زر مركز التحكم", "home"}, {"زر الجوال", "home"}, {"زر المهام", "home"}, {"زر المصادر", "home"}, {"زر الأسواق", "home"}, {"زر المباريات", "home"},
        {"زر تلاوة وأذان", "home"}, {"جرس التحديثات", "home"}, {"الرجوع يفتح شاشة التوقف", "home"}, {"رجوع مرتين يفتح شاشة التوقف", "saver"}, {"زر الرئيسية يفتح تلفزيونك مباشرة", "home"}, {"زر يوتيوب في الريموت", "home"}, {"زر نتفليكس في الريموت", "home"}, {"زر Prime Video في الريموت", "home"}, {"زر ديزني بلس في الريموت", "home"}, {"زر Paramount Plus في الريموت", "home"}, {"زر Free TV في الريموت", "home"}, {"الزر المخصص ★ في الريموت", "home"}, {"زر BRAVIA CORE في الريموت", "home"}, {"زر Crunchyroll في الريموت", "home"}, {"زر Google Play في الريموت", "home"}, {"زر SonyLIV في الريموت", "home"}, {"زر Tabii في الريموت", "home"}, {"زر TCL Channel في الريموت", "home"}, {"زر Okko في الريموت", "home"}, {"زر VK في الريموت", "home"}, {"زر Rutube في الريموت", "home"}, {"زر KION في الريموت", "home"}, {"زر Wink في الريموت", "home"}, {"زر IVI في الريموت", "home"},
        {"الأذان", "tv"}, {"المؤذن", "tv"}, {"أذاني من ملف", "tv"}, {"الأذان لكل صلاة", "tv"}, {"تنبيه قبل الأذان", "both"}, {"الأدعية والتذكيرات", "tv"}, {"صوتياتي", "tv"}, {"الشروق بعد الفجر", "both"}, {"شريط الوقت بين الصلاتين", "saver"}, {"طريقة الحساب", "tv"},
        {"تعديل وقت الصلاة", "tv"}, {"افتح تلاوة وأذان", "tv"},
        {"مباريات اليوم", "tv"}, {"الفرق المتابعة", "both"}, {"تطبيق المباريات", "home"}, {"الدوريات كاملة", "both"}, {"بطاقة المباريات لليوم فقط", "home"}, {"رياضات أخرى", "both"},
        {"افتح صفحة الأسواق", "home"}, {"شريط الأسواق", "saver"}, {"الأسواق", "saver"}, {"عرض الأسواق", "saver"}, {"قائمة الأسهم", "both"}, {"تغيّر الأسهم", "both"},
        {"التصنيفات", "both"}, {"اللغات", "both"}, {"مسلسلات مختارة", "both"}, {"أقل تقييم", "both"},
        {"معاينة الشاشة", "saver"}, {"بدء شاشة التوقف بعد", "saver"}, {"المحتوى", "saver"}, {"بطاقة ثانية", "saver"}, {"تبديل الآية والحديث", "both"},
        {"الآية والحديث", "both"}, {"الخانة اليسرى", "saver"}, {"العدّ التنازلي", "saver"}, {"شريط السنة تحت الساعة", "saver"}, {"المناسبة القادمة", "saver"},
        {"المناسبات الوطنية", "saver"},
        {"الرقابة الأبوية", "tv"}, {"تصفية المواقع", "tv"}, {"الجوالات المسموح لها", "tv"},
        {"تحديث تلفزيوني", "tv"}, {"التحديث التلقائي للصفحة", "both"}, {"تحديث صفحة شاشة التوقف", "both"}, {"النسخ التجريبية", "tv"}, {"أرسل ملاحظة عن النسخة التجريبية", "tv"}, {"إعداد تلفزيوني", "tv"}, {"مسح الملفات المؤقتة", "tv"}, {"مصادر البيانات", "both"},
        {"أرسل تقرير", "tv"},
        // 4.4 beta 4, the findability review: the new rows (and the release session's, placed ahead in ORDER)
        {"أضف صوري من الجوال", "both"}, {"أضف من فلاشة أو التنزيلات", "both"}, {"احذف صورة", "both"}, {"التطبيقات المخفية", "home"},
        {"حجم البطاقات", "both"}, {"زوايا البطاقات", "both"},
    };

    /** the words shown for a scope, at the row's end; "" for none */
    static String scopeWords(String row) {
        for (String[] r : SCOPES) if (r[0].equals(row)) return "home".equals(r[1]) ? "الرئيسية" : "saver".equals(r[1]) ? "شاشة التوقف" : "both".equals(r[1]) ? "الرئيسية وشاشة التوقف" : "كل التلفزيون";
        return "";
    }

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
     * {row key, category}; never the search's own row (it has no screen of its own).
     */
    static java.util.List<String[]> search(String query) {
        String q = fold(query);
        java.util.List<String[]> out = new java.util.ArrayList<>();
        if (q.isEmpty()) return out;
        java.util.List<String[]> starts = new java.util.ArrayList<>(), holds = new java.util.ArrayList<>(), sect = new java.util.ArrayList<>(), alias = new java.util.ArrayList<>();
        for (String[] r : rows()) {
            if (SEARCH.equals(r[1])) continue;
            String name = fold(r[0]);
            String[] hit = {r[0], r[1]};
            if (name.startsWith(q)) starts.add(hit);
            else if (name.contains(q)) holds.add(hit);
            else {
                boolean al = false;
                for (String[] a : ALIASES) if (aliasHit(a[0], q) && fold(a[1]).equals(name)) { al = true; break; }
                if (al) alias.add(hit);
                else if (fold(name(r[1])).contains(q)) sect.add(hit);
            }
        }
        out.addAll(starts); out.addAll(holds); out.addAll(alias); out.addAll(sect);
        return out;
    }

    /** a word without its «ال» (العيد = عيد), when something is left */
    static String bare(String w) { return w.startsWith("ال") && w.length() > 3 ? w.substring(2) : w; }

    /**
     * An alias answers a typed question when it starts with the whole of it (as before), or when a word of the question (three letters or more, with or
     * without «ال») is one of its words; the last word may be half typed. «كم باقي على رمضان», «عيد» and «الإفطار» find «المناسبة القادمة», while
     * «تاسي» does not find «يوم التأسيس» (a word inside a question must be whole).
     */
    static boolean aliasHit(String alias, String q) {
        String a = fold(alias);
        if (a.startsWith(q)) return true;
        String[] ts = q.split("\\s+"), ws = a.split("\\s+");
        for (int i = 0; i < ts.length; i++) {
            String t = bare(ts[i]);
            if (t.length() < 3) continue;
            boolean last = i == ts.length - 1 && ts.length > 1;
            for (String w : ws) if (last ? bare(w).startsWith(t) : bare(w).equals(t)) return true;
        }
        return false;
    }

    /** The category of a row (its FocusKey.key); one not named goes with its group's ({@code group}, a header's category), else «شاشة التوقف». */
    static String of(String key, String group) {
        for (String[] r : rows()) if (r[0].equals(key)) return r[1];
        return group != null ? group : SAVER;
    }

    /** The category of a group header, null for one not named. */
    static String ofHeader(String text) {
        for (String[] h : heads()) if (h[0].equals(text)) return h[1];
        return null;
    }

    private static String[] find(String category) {
        for (String[] c : cats()) if (c[0].equals(category)) return c;
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
        for (String[] c : cats()) if (category.equals(c[3])) return true;
        return false;
    }

    /** The buttons of a screen (null: the first one), in order. */
    static String[] tiles(String screen) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String[] c : cats()) if ((screen == null ? c[3] == null : screen.equals(c[3]))) out.add(c[0]);
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

    /** 4.4 beta 2 «كنبة» v2 (the owner approved docs/mockups/settings-style/couch-v2-*): the one plain line under a focused section's big name,
     *  what is inside, shorter than CATEGORIES' own line; none for the search */
    static final String[] SHORT_KEYS = {PRAY, PHOTOS, LOOK, SAVER, APPS, CONTENT, FAMILY, LANG, UPDATE};
    static final String[] SHORT_LINES = {
        "الأذان والمؤذن والتنبيهات والأدعية",
        "أضف صورك أو احذفها، والخلفيات",
        "الأشكال الجاهزة، والألوان، والبطاقات، والساعة",
        "متى تبدأ، وما يظهر عليها",
        "شريط التطبيقات والمخفية، والأزرار",
        "فرقك ودورياتك، والأسواق، وجديد المسلسلات",
        "الرقابة الأبوية، وتصفية المواقع، والجوالات",
        "اللغة والأرقام، والمدينة والطقس",
        "الإصدار والتحديث، وإرسال تقرير"};
    static String shortLine(String category) { int i = java.util.Arrays.asList(SHORT_KEYS).indexOf(category); return i < 0 ? "" : SHORT_LINES[i]; }
}
