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
            CONTENT = "content", SAVER = "saver", FAMILY = "family", UPDATE = "update";

    /**
     * key, name, the line under its title on its own screen, the screen its button is on (null: the first; nothing is deeper), and the row a DIRECT
     * button runs ("" when it opens a screen). In this order on the first screen; the first one (the search) is the wide one. 4.3.3: ten sections,
     * the city in one of them only («الموقع والطقس»), and no shortcut beside it (docs/mockups/SETTINGS-MAP.md).
     */
    static final String[][] CATEGORIES = {
        {SEARCH, "بحث في الإعدادات", "", null, "بحث في الإعدادات"},
        {LANG, "اللغة", "لغة التلفزيون، والأرقام والتقويم ونظام الساعة.", null, ""},
        {PLACE, "الموقع والطقس", "المدينة هنا فقط: منها الطقس ومواقيت الصلاة والتحية وشاشة التوقف ومواعيد المباريات.", null, ""},
        {LOOK, "المظهر", "شكل كنبة والتخطيط، والخلفية والآفاق، والساعة والنص، والتطبيقات، والبطاقات، والصور.", null, ""},
        {APPS, "التطبيقات", "شريط التطبيقات وترتيبه وأقفاله، وواجهة المصنع.", null, ""},
        {BUTTONS, "الأزرار السريعة", "أزرار الشاشة الرئيسية ومركز التحكم.", null, ""},
        {PRAY, "الصلاة والأذان", "الأذان وتنبيهاته، وطريقة حساب المواقيت، وشاشة التلاوة.", null, ""},
        {CONTENT, "المباريات والأسواق", "فرقك ودورياتك، والأسواق والأسهم، وجديد المسلسلات.", null, ""},
        {SAVER, "شاشة التوقف", "ما يظهر على شاشة التوقف: الساعة والبطاقة بجانبها، ومتى تبدأ.", null, ""},
        {FAMILY, "العائلة والأمان", "الرقابة الأبوية، وتصفية المواقع للعائلة، والجوالات المرتبطة.", null, ""},
        {UPDATE, "حول", "الإصدار والتحديث، وإعداد التلفزيون، وإرسال تقرير.", null, ""},
    };

    /** The old categories (3.x to 4.3.2) and the section each one went to: a stored or passed old key still lands somewhere right. */
    static final String[][] OLD = {
        {"clock", LANG}, {"listen", PRAY}, {"adhan", PRAY}, {"cards", SAVER}, {"more", CONTENT}, {"shows", CONTENT}, {"home", APPS}, {"city", PLACE},
        {"parental", FAMILY}, {"phone", FAMILY}, {"control", BUTTONS}, {"report", UPDATE},
    };

    /** A category key as of today: a current one as it is, an old one as its section, anything else null. */
    static String canon(String key) {
        if (key == null) return null;
        for (String[] c : cats()) if (c[0].equals(key)) return key;
        for (String[] o : classic ? OLD_432 : OLD) if (o[0].equals(key)) return o[1];
        return null;
    }

    /** each row of SettingsActivity by its FocusKey.key (the name without its numbers), and its category */
    static final String[][] ROWS = {
        {"التاريخ بالأرقام", LANG}, {"الوضع الخفيف", SAVER}, {"زر البحث", BUTTONS}, {"إخفاء المباريات المنتهية", CONTENT}, {"تنبيه بداية المباراة", CONTENT}, {"صوت التنبيه", CONTENT}, {"عند الهدف", CONTENT}, {"صافرة النهاية", CONTENT}, {"فوق تطبيق آخر", CONTENT},   // 4.3.3 rows of the features, performance and matches branches
        {"اللغة", LANG}, {"الأرقام", LANG}, {"أسماء الأشهر الميلادية", LANG}, {"نظام الساعة", LANG}, {"فرق التاريخ الهجري", LANG},
        {"المدينة", PLACE}, {"الدولة", PLACE}, {"التحية والطقس", PLACE}, {"الرطوبة والرياح", PLACE},
        {"شكل كنبة", LOOK}, {"التخطيط", LOOK}, {"الخلفية", LOOK}, {"الساعة", LOOK}, {"شكل التطبيقات", LOOK}, {"حزم الآفاق", LOOK}, {"بقاء كل أفق", LOOK}, {"الأفق يتبع الوقت", LOOK}, {"الأفق يتبع الطقس", LOOK}, {"وضع الألوان", LOOK}, {"حجم الساعة", LOOK}, {"إظهار الثواني", LOOK}, {"نص أوضح", LOOK}, {"توهّج لون التطبيق", LOOK}, {"تعتيم الليل", LOOK},
        {"صفحات الأسهم والمباريات داكنة", LOOK}, {"شكل البطاقة", LOOK}, {"البطاقة الأولى", LOOK}, {"البطاقة الثانية", LOOK}, {"البطاقة الثالثة", LOOK},
        {"أكمل المشاهدة", LOOK}, {"تبديل الصورة كل", LOOK}, {"صور شاشة التوقف", LOOK}, {"صوري", LOOK}, {"سرعة ألبوم صوري", LOOK},
        {"شكل صورة ألبوم صوري", LOOK}, {"حجم ألبوم صوري", LOOK}, {"عنوان ألبوم صوري", LOOK}, {"تاريخ الصورة في ألبوم صوري", LOOK},
        {"تطبيقات في الشريط", APPS}, {"ترتيب التطبيقات المفضلة", APPS}, {"تعديل المفضلة", APPS}, {"تطبيقات مقترحة", APPS}, {"ترتيب كل التطبيقات", APPS},
        {"أسماء المصادر", APPS}, {"قفل التطبيقات", APPS}, {"تفاصيل التطبيقات", APPS}, {"اجعل UrTV الشاشة الرئيسية", APPS}, {"تخلّص من واجهة المصنع", APPS},
        {"رجّع واجهة المصنع", APPS}, {"قبل ما تحذف التطبيق…", APPS}, {"\u2066UrTV\u2069 النظيف", APPS}, {"الخروج من تلفزيونك", APPS},
        {"مركز التحكم", BUTTONS}, {"زر مركز التحكم", BUTTONS}, {"زر الجوال", BUTTONS}, {"زر المهام", BUTTONS}, {"زر الأسواق", BUTTONS}, {"زر المباريات", BUTTONS},
        {"زر تلاوة وأذان", BUTTONS}, {"جرس التحديثات", BUTTONS}, {"الرجوع يفتح شاشة التوقف", BUTTONS},
        {"الأذان", PRAY}, {"تنبيه قبل الأذان", PRAY}, {"الشروق بعد الفجر", PRAY}, {"شريط الوقت بين الصلاتين", PRAY}, {"طريقة الحساب", PRAY},
        {"تعديل وقت الصلاة", PRAY}, {"افتح تلاوة وأذان", PRAY},
        {"مباريات اليوم", CONTENT}, {"الفرق المتابعة", CONTENT}, {"تطبيق المباريات", CONTENT}, {"الدوريات كاملة", CONTENT}, {"رياضات أخرى", CONTENT},
        {"افتح صفحة الأسواق", CONTENT}, {"شريط الأسواق", CONTENT}, {"الأسواق", CONTENT}, {"عرض الأسواق", CONTENT}, {"قائمة الأسهم", CONTENT}, {"تغيّر الأسهم", CONTENT},
        {"التصنيفات", CONTENT}, {"اللغات", CONTENT}, {"مسلسلات مختارة", CONTENT}, {"أقل تقييم", CONTENT},
        {"معاينة الشاشة", SAVER}, {"بدء شاشة التوقف بعد", SAVER}, {"المحتوى", SAVER}, {"بطاقة ثانية", SAVER}, {"تبديل الآية والحديث", SAVER},
        {"الآية والحديث", SAVER}, {"الخانة اليسرى", SAVER}, {"العدّ التنازلي", SAVER}, {"شريط السنة تحت الساعة", SAVER}, {"المناسبة القادمة", SAVER},
        {"المناسبات الوطنية", SAVER},
        {"الرقابة الأبوية", FAMILY}, {"تصفية المواقع", FAMILY}, {"الجوالات المسموح لها", FAMILY},
        {"تحديث تلفزيوني", UPDATE}, {"التحديث التلقائي للصفحة", UPDATE}, {"تحديث صفحة شاشة التوقف", UPDATE}, {"إعداد تلفزيوني", UPDATE}, {"مسح الملفات المؤقتة", UPDATE}, {"أرسل تقرير", UPDATE},
        {"بحث في الإعدادات", SEARCH},
    };

    /** each group header and the category it belongs to (a row of no name goes with it); the headers that are DRAWN are the header entries of ORDER */
    static final String[][] HEADERS = {
        {"الموقع", PRAY}, {"المباريات", CONTENT}, {"العرض", SAVER}, {"صفحة الأسواق", CONTENT}, {"الأسواق (أعلى اليسار)", CONTENT}, {"بجانب الساعة", SAVER},
        {"الشاشة الرئيسية (تلفزيونك)", APPS}, {"جديد المسلسلات", CONTENT}, {"الأسهم", CONTENT}, {"التوقيت", LOOK}, {"التطبيق", UPDATE},
    };

    /**
     * The rows of each section in the order the person meets them, by FocusKey; H + text is a header drawn above the rows after it. A row of no entry
     * (the prayers' minute rows, made by adjRow) stays right after the entry before it in the code; a section with no entry here keeps the code's order.
     */
    static final String H = "\u0000";   // an ORDER entry that starts with it is a header
    static final String[][] ORDER = {
        {LANG, "اللغة", "الأرقام", "التاريخ بالأرقام", "أسماء الأشهر الميلادية", "نظام الساعة", "فرق التاريخ الهجري"},
        {PLACE, "المدينة", "الدولة", H + "الطقس", "التحية والطقس", "الرطوبة والرياح"},
        // 4.4 the owner's review: «المظهر» in six groups; the colours with the preset and the layout; the photos' source apart from the album's display
        {LOOK, H + "شكل كنبة والتخطيط", "شكل كنبة", "التخطيط", "وضع الألوان", "صفحات الأسهم والمباريات داكنة",
            H + "الخلفية والآفاق", "الخلفية", "حزم الآفاق", "بقاء كل أفق", "الأفق يتبع الوقت", "الأفق يتبع الطقس",
            H + "الساعة والنص", "الساعة", "حجم الساعة", "إظهار الثواني", "نص أوضح", "تعتيم الليل",
            H + "التطبيقات", "شكل التطبيقات", "توهّج لون التطبيق",
            H + "البطاقات", "شكل البطاقة", "البطاقة الأولى", "البطاقة الثانية", "البطاقة الثالثة", "أكمل المشاهدة",
            H + "الصور", "صور شاشة التوقف", "صوري", "تبديل الصورة كل",
            H + "ألبوم صوري", "سرعة ألبوم صوري", "شكل صورة ألبوم صوري", "حجم ألبوم صوري", "عنوان ألبوم صوري", "تاريخ الصورة في ألبوم صوري"},
        // the owner: what makes UrTV the TV's home (and back) first, under its own header, then the apps bar
        {APPS, H + "الشاشة الرئيسية للتلفزيون", "اجعل UrTV الشاشة الرئيسية", "تخلّص من واجهة المصنع", "رجّع واجهة المصنع", "قبل ما تحذف التطبيق…", "⁦UrTV⁩ النظيف", "الخروج من تلفزيونك",
            H + "شريط التطبيقات", "تطبيقات في الشريط", "ترتيب التطبيقات المفضلة", "تعديل المفضلة", "تطبيقات مقترحة", "ترتيب كل التطبيقات", "أسماء المصادر", "قفل التطبيقات", "تفاصيل التطبيقات"},
        {BUTTONS, "مركز التحكم", "زر مركز التحكم", "زر الجوال", "زر البحث", "زر المهام", "زر الأسواق", "زر المباريات", "زر تلاوة وأذان", "جرس التحديثات", "الرجوع يفتح شاشة التوقف"},
        {PRAY, "الأذان", "تنبيه قبل الأذان", "الشروق بعد الفجر", "شريط الوقت بين الصلاتين", "طريقة الحساب", "تعديل وقت الصلاة", "افتح تلاوة وأذان"},
        {CONTENT, H + "المباريات", "مباريات اليوم", "الفرق المتابعة", "تطبيق المباريات", "الدوريات كاملة", "رياضات أخرى", "إخفاء المباريات المنتهية", "تنبيه بداية المباراة", "صوت التنبيه", "عند الهدف", "صافرة النهاية", "فوق تطبيق آخر", H + "صفحة الأسواق", "افتح صفحة الأسواق", "شريط الأسواق", "الأسواق", "عرض الأسواق",
            H + "الأسهم", "قائمة الأسهم", "تغيّر الأسهم", H + "جديد المسلسلات", "التصنيفات", "اللغات", "مسلسلات مختارة", "أقل تقييم"},
        {SAVER, "معاينة الشاشة", "بدء شاشة التوقف بعد", "الوضع الخفيف", H + "بجانب الساعة", "المحتوى", "بطاقة ثانية", "تبديل الآية والحديث", "الآية والحديث", "الخانة اليسرى", "العدّ التنازلي",
            "شريط السنة تحت الساعة", "المناسبة القادمة", "المناسبات الوطنية"},
        {FAMILY, "الرقابة الأبوية", "تصفية المواقع", "الجوالات المسموح لها"},
        {UPDATE, "تحديث تلفزيوني", "التحديث التلقائي للصفحة", "تحديث صفحة شاشة التوقف", "إعداد تلفزيوني", "مسح الملفات المؤقتة", "أرسل تقرير"},
    };


    // ---- 4.4 the owner: «شكل كنبة» off = the 4.3.2 settings screen (master's hub of big buttons, «البطاقات» and «المزيد» screens of buttons), on = the side list
    // above. The same rows (SettingsActivity.render builds each once); only where they sit differs. Made from master's SettingsHub and render() order by
    // a script (each 4.3.2 row where 4.3.2 had it, its headers where 4.3.2 drew them); the 4.4 rows in their natural 4.3.2 section (the switch and the
    // look's rows first in «المظهر والخلفيات», the Hijri shift in «الساعة والأرقام», the caches in «تحديث تلفزيوني», the search on the first screen).

    /** key, name, its line, the screen its button is on (null: the first), the row a DIRECT button runs ("" when it opens a screen); 4.3.2's */
    static final String[][] CATEGORIES_432 = {
        {"city", "المدينة", "", null, "المدينة"},
        {"adhan", "الأذان", "", null, "الأذان"},
        {"place", "الموقع والصلاة", "مدينتك تحدد الوقت والطقس، وطريقة الحساب تحدد مواقيت الصلاة.", null, ""},
        {"clock", "الساعة والأرقام", "نظام الساعة ١٢ أو ٢٤، والأرقام عربية أو إنجليزية، وأسماء الأشهر الميلادية.", null, ""},
        {"listen", "تلاوة وأذان", "شاشة التلاوة والأذان، وزرها في الشاشة الرئيسية.", null, ""},
        {"cards", "البطاقات", "ما يظهر على شاشة التوقف وعلى الشاشة الرئيسية.", null, ""},
        {"look", "المظهر والخلفيات", "الشكل والصور لشاشة التوقف وللشاشة الرئيسية.", null, ""},
        {"control", "مركز التحكم", "", null, "مركز التحكم"},
        {"phone", "التحكم من الجوال", "", null, "الجوالات المسموح لها"},
        {"update", "تحديث تلفزيوني", "تحديث التطبيقين وإصدارهما، وتحديث صفحة شاشة التوقف.", null, ""},
        {"report", "أرسل تقرير", "", null, "أرسل تقرير"},
        {"more", "المزيد", "المباريات والأسواق، والتطبيقات والمفضلة، وجديد المسلسلات، والرقابة الأبوية، وتصفية المواقع.", null, ""},
        {"saver", "شاشة التوقف", "ما يظهر على شاشة التوقف: الساعة والبطاقة بجانبها، والصور، ومتى تبدأ.", "cards", ""},
        {"home", "الشاشة الرئيسية", "بطاقات الشاشة الرئيسية الثلاث، والتحية والطقس، والأزرار.", "cards", ""},
        {"content", "المباريات والأسواق", "ما تعرضه البطاقات: فرقك ودورياتك، والأسواق، والأسهم.", "more", ""},
        {"apps", "التطبيقات والمفضلة", "ترتيب التطبيقات المفضلة وتعديلها، وترتيب كل التطبيقات، وأسماء مصادر التلفزيون مثل PS5.", "more", ""},
        {"shows", "جديد المسلسلات", "ما تعرضه بطاقة المسلسلات: التصنيفات واللغات والمسلسلات المختارة.", "more", ""},
        {"parental", "الرقابة الأبوية", "", "more", "الرقابة الأبوية"},
        {"family", "تصفية المواقع", "", "more", "تصفية المواقع"},
        {"lang", "اللغة", "", "more", "اللغة"},
        {"search", "بحث في الإعدادات", "", null, "بحث في الإعدادات"},   // 4.4: the search, as in the side list
    };

    /** each row and its 4.3.2 section */
    static final String[][] ROWS_432 = {
        {"الأذان", "adhan"}, {"الدولة", "place"}, {"المدينة", "place"}, {"طريقة الحساب", "place"}, {"تعديل وقت الصلاة", "place"},
        {"الأرقام", "clock"}, {"التاريخ بالأرقام", "clock"}, {"أسماء الأشهر الميلادية", "clock"}, {"نظام الساعة", "clock"}, {"إظهار الثواني", "clock"}, {"فرق التاريخ الهجري", "clock"},
        {"افتح تلاوة وأذان", "listen"}, {"زر تلاوة وأذان", "listen"}, {"شكل كنبة", "look"}, {"التخطيط", "look"}, {"الخلفية", "look"},
        {"الساعة", "look"}, {"شكل التطبيقات", "look"}, {"حزم الآفاق", "look"}, {"بقاء كل أفق", "look"}, {"الأفق يتبع الوقت", "look"},
        {"الأفق يتبع الطقس", "look"}, {"وضع الألوان", "look"}, {"مركز التحكم", "control"}, {"الجوالات المسموح لها", "phone"}, {"تحديث تلفزيوني", "update"},
        {"التحديث التلقائي للصفحة", "update"}, {"تحديث صفحة شاشة التوقف", "update"}, {"مسح الملفات المؤقتة", "update"}, {"أرسل تقرير", "report"}, {"معاينة الشاشة", "saver"},
        {"تنبيه قبل الأذان", "saver"}, {"الشروق بعد الفجر", "saver"}, {"شريط الوقت بين الصلاتين", "saver"}, {"الرطوبة والرياح", "saver"}, {"شريط السنة تحت الساعة", "saver"},
        {"سرعة ألبوم صوري", "saver"}, {"تاريخ الصورة في ألبوم صوري", "saver"}, {"عنوان ألبوم صوري", "saver"}, {"شكل صورة ألبوم صوري", "saver"}, {"حجم ألبوم صوري", "saver"},
        {"توهّج لون التطبيق", "saver"}, {"نص أوضح", "saver"}, {"المناسبة القادمة", "saver"}, {"تعتيم الليل", "saver"}, {"المناسبات الوطنية", "saver"},
        {"حجم الساعة", "saver"}, {"الخانة اليسرى", "saver"}, {"العدّ التنازلي", "saver"}, {"المحتوى", "saver"}, {"بطاقة ثانية", "saver"},
        {"شكل البطاقة", "saver"}, {"تبديل الآية والحديث", "saver"}, {"الآية والحديث", "saver"}, {"تبديل الصورة كل", "saver"}, {"صور شاشة التوقف", "saver"},
        {"صوري", "saver"}, {"بدء شاشة التوقف بعد", "saver"}, {"الوضع الخفيف", "saver"}, {"اجعل UrTV الشاشة الرئيسية", "home"}, {"إعداد تلفزيوني", "home"}, {"رجّع واجهة المصنع", "home"},
        {"تخلّص من واجهة المصنع", "home"}, {"قبل ما تحذف التطبيق…", "home"}, {"\u2066UrTV\u2069 النظيف", "home"}, {"صفحات الأسهم والمباريات داكنة", "home"}, {"البطاقة الأولى", "home"},
        {"البطاقة الثانية", "home"}, {"البطاقة الثالثة", "home"}, {"التحية والطقس", "home"}, {"زر الأسواق", "home"}, {"زر المباريات", "home"},
        {"زر الجوال", "home"}, {"زر البحث", "home"}, {"زر مركز التحكم", "home"}, {"زر المهام", "home"}, {"الرجوع يفتح شاشة التوقف", "home"}, {"الخروج من تلفزيونك", "home"},
        {"جرس التحديثات", "home"}, {"أكمل المشاهدة", "home"}, {"مباريات اليوم", "content"}, {"الفرق المتابعة", "content"}, {"تطبيق المباريات", "content"},
        {"الدوريات كاملة", "content"}, {"رياضات أخرى", "content"}, {"إخفاء المباريات المنتهية", "content"}, {"تنبيه بداية المباراة", "content"}, {"صوت التنبيه", "content"}, {"عند الهدف", "content"}, {"صافرة النهاية", "content"}, {"فوق تطبيق آخر", "content"}, {"افتح صفحة الأسواق", "content"}, {"شريط الأسواق", "content"}, {"الأسواق", "content"},
        {"عرض الأسواق", "content"}, {"قائمة الأسهم", "content"}, {"تغيّر الأسهم", "content"}, {"تطبيقات في الشريط", "apps"}, {"ترتيب التطبيقات المفضلة", "apps"},
        {"تطبيقات مقترحة", "apps"}, {"أسماء المصادر", "apps"}, {"تعديل المفضلة", "apps"}, {"ترتيب كل التطبيقات", "apps"}, {"قفل التطبيقات", "apps"},
        {"تفاصيل التطبيقات", "apps"}, {"التصنيفات", "shows"}, {"اللغات", "shows"}, {"مسلسلات مختارة", "shows"}, {"أقل تقييم", "shows"},
        {"الرقابة الأبوية", "parental"}, {"تصفية المواقع", "family"}, {"اللغة", "lang"}, {"بحث في الإعدادات", "search"},
    };

    /** 4.3.2's group headers and their section */
    static final String[][] HEADERS_432 = {
        {"الموقع", "place"},
        {"المباريات", "content"},
        {"العرض", "saver"},
        {"صفحة الأسواق", "content"},
        {"الأسواق (أعلى اليسار)", "content"},
        {"بجانب الساعة", "saver"},
        {"الشاشة الرئيسية (تلفزيونك)", "home"},
        {"جديد المسلسلات", "shows"},
        {"الأسهم", "content"},
        {"التوقيت", "saver"},
        {"التطبيق", "update"},
    };

    /** each 4.3.2 section's rows in the order 4.3.2 showed them, with its headers */
    static final String[][] ORDER_432 = {
        {"adhan", "الأذان"},
        {"place", H + "الموقع", "الدولة", "المدينة", "طريقة الحساب", "تعديل وقت الصلاة"},
        {"clock", "الأرقام", "التاريخ بالأرقام", "أسماء الأشهر الميلادية", "نظام الساعة", "إظهار الثواني", "فرق التاريخ الهجري"},
        {"listen", "افتح تلاوة وأذان", "زر تلاوة وأذان"},
        {"look", "شكل كنبة", "التخطيط", "الخلفية", "الساعة", "شكل التطبيقات", "حزم الآفاق", "بقاء كل أفق", "الأفق يتبع الوقت", "الأفق يتبع الطقس", "وضع الألوان"},
        {"control", "مركز التحكم"},
        {"phone", "الجوالات المسموح لها"},
        {"update", "تحديث تلفزيوني", H + "التطبيق", "التحديث التلقائي للصفحة", "تحديث صفحة شاشة التوقف", "مسح الملفات المؤقتة"},
        {"report", "أرسل تقرير"},
        {"saver", "معاينة الشاشة", "الوضع الخفيف", H + "العرض", "تنبيه قبل الأذان", "الشروق بعد الفجر", "شريط الوقت بين الصلاتين", "الرطوبة والرياح", "شريط السنة تحت الساعة", "سرعة ألبوم صوري", "تاريخ الصورة في ألبوم صوري", "عنوان ألبوم صوري", "شكل صورة ألبوم صوري", "حجم ألبوم صوري", "توهّج لون التطبيق", "نص أوضح", "المناسبة القادمة", "تعتيم الليل", "المناسبات الوطنية", "حجم الساعة", "الخانة اليسرى", "العدّ التنازلي", H + "بجانب الساعة", "المحتوى", "بطاقة ثانية", "شكل البطاقة", "تبديل الآية والحديث", "الآية والحديث", H + "التوقيت", "تبديل الصورة كل", "صور شاشة التوقف", "صوري", "بدء شاشة التوقف بعد"},
        {"home", H + "الشاشة الرئيسية (تلفزيونك)", "اجعل UrTV الشاشة الرئيسية", "إعداد تلفزيوني", "رجّع واجهة المصنع", "تخلّص من واجهة المصنع", "قبل ما تحذف التطبيق…", "\u2066UrTV\u2069 النظيف", "صفحات الأسهم والمباريات داكنة", "البطاقة الأولى", "البطاقة الثانية", "البطاقة الثالثة", "التحية والطقس", "زر الأسواق", "زر المباريات", "زر الجوال", "زر البحث", "زر مركز التحكم", "زر المهام", "الرجوع يفتح شاشة التوقف", "الخروج من تلفزيونك", "جرس التحديثات", "أكمل المشاهدة"},
        {"content", H + "المباريات", "مباريات اليوم", "الفرق المتابعة", "تطبيق المباريات", "الدوريات كاملة", "رياضات أخرى", "إخفاء المباريات المنتهية", "تنبيه بداية المباراة", "صوت التنبيه", "عند الهدف", "صافرة النهاية", "فوق تطبيق آخر", H + "صفحة الأسواق", "افتح صفحة الأسواق", H + "الأسواق (أعلى اليسار)", "شريط الأسواق", "الأسواق", "عرض الأسواق", H + "الأسهم", "قائمة الأسهم", "تغيّر الأسهم"},
        {"apps", "تطبيقات في الشريط", "ترتيب التطبيقات المفضلة", "تطبيقات مقترحة", "أسماء المصادر", "تعديل المفضلة", "ترتيب كل التطبيقات", "قفل التطبيقات", "تفاصيل التطبيقات"},
        {"shows", H + "جديد المسلسلات", "التصنيفات", "اللغات", "مسلسلات مختارة", "أقل تقييم"},
        {"parental", "الرقابة الأبوية"},
        {"family", "تصفية المواقع"},
        {"lang", "اللغة"},
        {"search", "بحث في الإعدادات"},
    };

    /** a section key of the side list, as the 4.3.2 screen has it (a link or a stored key that names a 4.4 section still lands somewhere right) */
    static final String[][] OLD_432 = {
        {"pray", "place"}, {"buttons", "home"}, {"cards", "cards"}, {"more", "more"},
    };

    /** 4.4: the 4.3.2 screen is the one shown (Prefs.couchOn() false); SettingsActivity sets it before it builds anything */
    static volatile boolean classic;
    static void classic(boolean on) { classic = on; }
    private static String[][] cats() { return classic ? CATEGORIES_432 : CATEGORIES; }
    private static String[][] rows() { return classic ? ROWS_432 : ROWS; }
    private static String[][] order() { return classic ? ORDER_432 : ORDER; }
    private static String[][] heads() { return classic ? HEADERS_432 : HEADERS; }

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
        {"صوت", "الأذان"}, {"مؤذن", "الأذان"}, {"فجر", "الأذان"}, {"نوم", "مركز التحكم"}, {"واي فاي", "مركز التحكم"}, {"شبكه", "مركز التحكم"}, {"بلوتوث", "مركز التحكم"},
        {"رمز", "الرقابة الأبوية"}, {"اطفال", "الرقابة الأبوية"}, {"pin", "الرقابة الأبوية"}, {"لون", "وضع الألوان"}, {"داكن", "وضع الألوان"}, {"فاتح", "وضع الألوان"}, {"خلفيه", "صور شاشة التوقف"},
        {"english", "اللغة"}, {"هجري", "فرق التاريخ الهجري"}, {"انجليزي", "اللغة"}, {"نسخه", "تحديث تلفزيوني"}, {"جوال", "الجوالات المسموح لها"}, {"ريموت", "الجوالات المسموح لها"}, {"تحكم من الجوال", "الجوالات المسموح لها"}, {"ثواني", "إظهار الثواني"},
        {"مباراه", "مباريات اليوم"}, {"اسهم", "قائمة الأسهم"}, {"ذهب", "افتح صفحة الأسواق"},
        {"الشاشه الرئيسيه", "اجعل UrTV الشاشة الرئيسية"}, {"واجهه المصنع", "تخلّص من واجهة المصنع"}, {"مشاهد", "حزم الآفاق"}, {"مشهد", "حزم الآفاق"},
    };

    /**
     * 4.4 the owner: where each row shows: "home" (the home screen), "saver" (the screensaver), "both", or "tv" (the whole TV, not a screen of its own).
     * SettingsActivity.row shows it at the row's end; docs/SETTINGS-TREE.md beside the row. tests/settings_scope_test.py: every row has one.
     */
    static final String[][] SCOPES = {
        {"التاريخ بالأرقام", "both"}, {"الوضع الخفيف", "both"}, {"زر البحث", "home"}, {"إخفاء المباريات المنتهية", "home"}, {"تنبيه بداية المباراة", "both"}, {"صوت التنبيه", "both"}, {"عند الهدف", "both"}, {"صافرة النهاية", "both"}, {"فوق تطبيق آخر", "tv"},
        {"اللغة", "tv"}, {"الأرقام", "both"}, {"أسماء الأشهر الميلادية", "both"}, {"نظام الساعة", "both"}, {"فرق التاريخ الهجري", "both"},
        {"المدينة", "tv"}, {"الدولة", "tv"}, {"التحية والطقس", "home"}, {"الرطوبة والرياح", "saver"},
        {"شكل كنبة", "home"}, {"التخطيط", "home"}, {"وضع الألوان", "home"}, {"صفحات الأسهم والمباريات داكنة", "home"},
        {"الخلفية", "home"}, {"حزم الآفاق", "home"}, {"بقاء كل أفق", "home"}, {"الأفق يتبع الوقت", "home"}, {"الأفق يتبع الطقس", "home"},
        {"الساعة", "home"}, {"حجم الساعة", "saver"}, {"إظهار الثواني", "both"}, {"نص أوضح", "home"}, {"تعتيم الليل", "saver"},
        {"شكل التطبيقات", "home"}, {"توهّج لون التطبيق", "home"},
        {"شكل البطاقة", "both"}, {"البطاقة الأولى", "home"}, {"البطاقة الثانية", "home"}, {"البطاقة الثالثة", "home"}, {"أكمل المشاهدة", "home"},
        {"صور شاشة التوقف", "both"}, {"صوري", "both"}, {"تبديل الصورة كل", "saver"},
        {"سرعة ألبوم صوري", "both"}, {"شكل صورة ألبوم صوري", "both"}, {"حجم ألبوم صوري", "both"}, {"عنوان ألبوم صوري", "both"}, {"تاريخ الصورة في ألبوم صوري", "both"},
        {"اجعل UrTV الشاشة الرئيسية", "tv"}, {"تخلّص من واجهة المصنع", "tv"}, {"رجّع واجهة المصنع", "tv"}, {"قبل ما تحذف التطبيق…", "tv"}, {"\u2066UrTV\u2069 النظيف", "both"},
        {"الخروج من تلفزيونك", "tv"}, {"تطبيقات في الشريط", "home"}, {"ترتيب التطبيقات المفضلة", "home"}, {"تعديل المفضلة", "home"}, {"تطبيقات مقترحة", "home"},
        {"ترتيب كل التطبيقات", "home"}, {"أسماء المصادر", "home"}, {"قفل التطبيقات", "tv"}, {"تفاصيل التطبيقات", "tv"},
        {"مركز التحكم", "home"}, {"زر مركز التحكم", "home"}, {"زر الجوال", "home"}, {"زر المهام", "home"}, {"زر الأسواق", "home"}, {"زر المباريات", "home"},
        {"زر تلاوة وأذان", "home"}, {"جرس التحديثات", "home"}, {"الرجوع يفتح شاشة التوقف", "home"},
        {"الأذان", "tv"}, {"تنبيه قبل الأذان", "both"}, {"الشروق بعد الفجر", "both"}, {"شريط الوقت بين الصلاتين", "saver"}, {"طريقة الحساب", "tv"},
        {"تعديل وقت الصلاة", "tv"}, {"افتح تلاوة وأذان", "tv"},
        {"مباريات اليوم", "tv"}, {"الفرق المتابعة", "both"}, {"تطبيق المباريات", "home"}, {"الدوريات كاملة", "both"}, {"رياضات أخرى", "both"},
        {"افتح صفحة الأسواق", "home"}, {"شريط الأسواق", "saver"}, {"الأسواق", "saver"}, {"عرض الأسواق", "saver"}, {"قائمة الأسهم", "both"}, {"تغيّر الأسهم", "both"},
        {"التصنيفات", "both"}, {"اللغات", "both"}, {"مسلسلات مختارة", "both"}, {"أقل تقييم", "both"},
        {"معاينة الشاشة", "saver"}, {"بدء شاشة التوقف بعد", "saver"}, {"المحتوى", "saver"}, {"بطاقة ثانية", "saver"}, {"تبديل الآية والحديث", "both"},
        {"الآية والحديث", "both"}, {"الخانة اليسرى", "saver"}, {"العدّ التنازلي", "saver"}, {"شريط السنة تحت الساعة", "saver"}, {"المناسبة القادمة", "saver"},
        {"المناسبات الوطنية", "saver"},
        {"الرقابة الأبوية", "tv"}, {"تصفية المواقع", "tv"}, {"الجوالات المسموح لها", "tv"},
        {"تحديث تلفزيوني", "tv"}, {"التحديث التلقائي للصفحة", "both"}, {"تحديث صفحة شاشة التوقف", "both"}, {"إعداد تلفزيوني", "tv"}, {"مسح الملفات المؤقتة", "tv"},
        {"أرسل تقرير", "tv"},
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
}
