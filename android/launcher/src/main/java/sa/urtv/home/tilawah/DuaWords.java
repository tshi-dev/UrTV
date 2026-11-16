package sa.urtv.home.tilawah;

import sa.urtv.home.S;

/** 4.4: a dua's rule in words, as the list and its page say it: «كل جمعة، الساعة ١٠:٠٠ ص», «كل يوم، بعد الفجر بـ ١٥ دقيقة». */
final class DuaWords {
    private DuaWords() { }

    static final String[] DAYS = {S.n("الأحد"), S.n("الاثنين"), S.n("الثلاثاء"), S.n("الأربعاء"), S.n("الخميس"), S.n("الجمعة"), S.n("السبت")};
    static final String[] GREG = {S.n("يناير"), S.n("فبراير"), S.n("مارس"), S.n("أبريل"), S.n("مايو"), S.n("يونيو"), S.n("يوليو"), S.n("أغسطس"),
            S.n("سبتمبر"), S.n("أكتوبر"), S.n("نوفمبر"), S.n("ديسمبر")};
    static final String[] HIJRI = {S.n("محرم"), S.n("صفر"), S.n("ربيع الأول"), S.n("ربيع الآخر"), S.n("جمادى الأولى"), S.n("جمادى الآخرة"), S.n("رجب"),
            S.n("شعبان"), S.n("رمضان"), S.n("شوال"), S.n("ذو القعدة"), S.n("ذو الحجة")};

    /** the day part: «كل يوم», «كل جمعة», «الأحد والثلاثاء», «٩ ذو الحجة», «٩ ذو الحجة ١٤٤٨» */
    static String day(DuaRule r) {
        switch (r.day) {
            case DuaRule.WEEKDAYS: {
                StringBuilder b = new StringBuilder();
                int n = 0;
                for (int i = 0; i < 7; i++) if ((r.weekdays & (1 << i)) != 0) n++;
                if (n == 7) return S.t("كل يوم");
                int k = 0;
                for (int i = 0; i < 7; i++) {
                    if ((r.weekdays & (1 << i)) == 0) continue;
                    if (k > 0) b.append(k == n - 1 ? S.t(" و") : S.t("، "));
                    b.append(S.tv(DAYS[i]));
                    k++;
                }
                return n == 1 && r.repeat ? S.t("كل %s", b.toString()) : b.toString();
            }
            case DuaRule.GREGORIAN:
            case DuaRule.HIJRI: {
                String month = S.tv((r.day == DuaRule.HIJRI ? HIJRI : GREG)[Math.max(0, Math.min(11, r.month - 1))]);
                String d = Ui.arabic(r.date) + " " + month;
                if (!r.repeat) d += " " + Ui.arabic(r.year);
                return r.repeat ? S.t("كل سنة في %s", d) : d;
            }
            default: return S.t("كل يوم");
        }
    }

    /** the time part: «الساعة ١٠:٠٠ ص», «بعد العصر بـ ١٠ دقائق», «عند المغرب», «قبل المغرب بـ ٥ دقائق» */
    static String time(DuaRule r) {
        if (r.at == DuaRule.AT_CLOCK) return S.t("الساعة %s", clock(r.clock));
        String prayer = S.tv(PrayerTimes.NAMES[Math.max(0, Math.min(5, r.prayer))]);
        if (r.offset == 0) return S.t("عند %s", prayer);
        String by = minutes(r.offset);
        return r.at == DuaRule.BEFORE ? S.t("قبل %1$s بـ %2$s", prayer, by) : S.t("بعد %1$s بـ %2$s", prayer, by);
    }

    static String clock(int minutes) {
        return PrayerTimes.clockText(String.format(java.util.Locale.US, "%02d:%02d", minutes / 60, minutes % 60));
    }

    static String minutes(int n) {
        if (n % 60 == 0 && n >= 60) return n == 60 ? S.t("ساعة") : n == 120 ? S.t("ساعتين") : S.t("%s ساعات", Ui.arabic(n / 60));
        if (n == 1) return S.t("دقيقة");
        if (n == 2) return S.t("دقيقتين");
        return n >= 3 && n <= 10 ? S.t("%s دقائق", Ui.arabic(n)) : S.t("%s دقيقة", Ui.arabic(n));
    }

    /** the whole line: «كل جمعة، الساعة ١٠:٠٠ ص»; once: «… مرة واحدة» */
    static String line(DuaRule r) {
        String s = S.t("%1$s، %2$s", day(r), time(r));
        boolean onceShown = !r.repeat && r.day != DuaRule.GREGORIAN && r.day != DuaRule.HIJRI;
        return onceShown ? S.t("%s، مرة واحدة", s) : s;
    }
}
