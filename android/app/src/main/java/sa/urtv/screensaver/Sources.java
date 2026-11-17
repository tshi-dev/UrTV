package sa.urtv.screensaver;

/**
 * 4.4 «مصادر البيانات» (الإعدادات ← حول), the owner's rule for every card and screen: where the data comes from is said once, here, with what each
 * source gives and on what terms, and OK on one goes to the row of what it gives. On the cards a source is at most a tiny dim line (60% of
 * the line it sits by, half its light: the page's .srcl), or nothing; a credit a licence asks for on screen stays, that small (Open-Meteo's).
 * Plain Java: tests/sources_rule_test.py reads it.
 */
final class Sources {
    private Sources() { }

    /** the source as it names itself, what it gives, its terms, and the settings row of what it gives (SettingsHub.ROWS) */
    static final String[][] LIST = {
        {"Open-Meteo", "الطقس والغبار، والساعات والأيام القادمة، وبحث المدن", "ترخيص CC BY 4.0: اسمه يبقى صغيرًا تحت الطقس", "التحية والطقس"},
        {"Aladhan", "مواقيت الصلاة بطريقة الحساب المختارة، وبلا إنترنت تُحسب على التلفزيون", "حسب شروط الموقع", "طريقة الحساب"},
        {"TheSportsDB", "المباريات ونتائجها وشعارات الأندية", "حسب شروط الموقع", "الفرق المتابعة"},
        {"Yahoo Finance / CoinGecko", "الأسواق والأسهم والعملات، بأسعار متأخرة", "حسب شروط الموقعين", "الأسواق"},
        {"TVmaze", "جديد المسلسلات", "ترخيص CC BY-SA 4.0", "التصنيفات"},
        {"QuranEnc / api.alquran.cloud", "نص الآيات و«التفسير الميسر» من مجمع الملك فهد لطباعة المصحف الشريف", "يُنشر كما هو بلا تعديل، مع ذكر الناشر والمصدر", "الآية والحديث"},
        {"Wikidata", "الأسماء العربية للفرق", "ترخيص CC0", "الفرق المتابعة"},
        {"كتب السنة", "الحديث والأذكار والحكمة، وتحت كل نص مصدره", "نصوص مأثورة، لكل نص مصدره", "الآية والحديث"},
        {"المصورون", "صور شاشة التوقف والآفاق، ولكل صورة مصوّرها وترخيصها في «حقوق الصور»", "حسب ترخيص كل صورة", "صور شاشة التوقف"},
    };
}
