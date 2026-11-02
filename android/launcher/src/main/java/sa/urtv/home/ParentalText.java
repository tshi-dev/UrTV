package sa.urtv.home;

/**
 * The line under «الرقابة الأبوية مفعّلة» on the family lock's card (docs/UX-REVIEW.md): how many apps are locked, whether the settings
 * are, and kids mode, in one sentence. No Android in here: tests/ParentalTextRun.java runs it.
 */
final class ParentalText {
    private ParentalText() {}

    /** the number of locked apps, as Arabic counts them (0, 1, 2, 3 to 10, 11 and more), in Latin digits (the screen converts them, Digits.of) */
    static String apps(int n) {
        if (n <= 0) return S.t("لا تطبيق مقفل");
        if (n == 1) return S.t("تطبيق واحد مقفل");
        if (n == 2) return S.t("تطبيقان مقفلان");
        if (n <= 10) return S.t("%s تطبيقات مقفلة", n);
        return S.t("%s تطبيقًا مقفلًا", n);
    }

    static String summary(int lockedApps, boolean settingsLocked, boolean kids) {
        return S.t("%s، %s، %s.", apps(lockedApps),
                settingsLocked ? S.t("والإعدادات مقفلة") : S.t("والإعدادات مفتوحة"),
                kids ? S.t("ووضع الأطفال مفعّل") : S.t("ووضع الأطفال متوقف"));
    }
}
