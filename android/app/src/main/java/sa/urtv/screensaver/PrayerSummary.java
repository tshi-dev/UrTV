package sa.urtv.screensaver;

/**
 * «الأذان لكل صلاة»'s value in the settings: the home's five states ("00200", the provider's adhan_prayers) as «كلها صوت وشاشة», the one prayer that differs
 * («الفجر: شاشة فقط»), or how many («٢ مختلفة»). The same rule as the home's PrayerScreen.summary; tests/prayer_summary_cases.tsv pins both. No Android in it.
 */
final class PrayerSummary {
    private PrayerSummary() { }

    private static final String[] NAMES = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};
    private static final String[] WORDS = {"صوت وشاشة", "صوت فقط", "شاشة فقط", "لا شيء"};

    /** the value, or null when the home has not said (not five digits) */
    static String of(String st, java.util.function.IntFunction<String> num) {
        if (st == null || st.length() != 5) return null;
        int differ = 0, first = -1;
        for (int i = 0; i < 5; i++) if (st.charAt(i) != '0') { differ++; if (first < 0) first = i; }
        if (differ == 0) return "كلها صوت وشاشة";
        if (differ == 1) return NAMES[first] + ": " + WORDS[Math.max(0, Math.min(3, st.charAt(first) - '0'))];
        return num.apply(differ) + " مختلفة";
    }
}
