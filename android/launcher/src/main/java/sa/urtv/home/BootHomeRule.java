package sa.urtv.home;

/**
 * 4.4 (R-410): after a power cut some Changhong and Skyworth TVs boot straight into Live TV (their own «boot input»), not to the home. «ابدأ بالرئيسية
 * بعد التشغيل»: at BOOT_COMPLETED our home is brought to the front. On by default ONLY on those makers' TVs (where it was reported); off elsewhere, and the
 * person's choice (`optimize/dock.sh boot-home on|off`) wins once set. Never while our home is not the person's home, never during a setup or its pending
 * safety switch. The TV's own setting is the better way where it has one (docs/SETTINGS.md). No Android in here (tests/BootHomeRun.java).
 */
public final class BootHomeRule {
    private BootHomeRule() { }

    /** the makers it is on for by default (Build.MANUFACTURER or BRAND, any case) */
    static final String[] MAKERS = {"changhong", "skyworth"};

    static boolean defaultOn(String manufacturer, String brand) {
        for (String m : MAKERS) if (m.equalsIgnoreCase(trim(manufacturer)) || m.equalsIgnoreCase(trim(brand))) return true;
        return false;
    }

    /** the setting: the person's choice when set (null: never set), else the maker's default */
    static boolean on(Boolean chosen, String manufacturer, String brand) { return chosen != null ? chosen : defaultOn(manufacturer, brand); }

    /** bring our home in front at this boot */
    public static boolean act(boolean on, boolean oursHome, boolean setupBusy) { return on && oursHome && !setupBusy; }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
}
