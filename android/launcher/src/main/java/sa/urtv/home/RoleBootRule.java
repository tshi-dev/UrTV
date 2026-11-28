package sa.urtv.home;

/**
 * 4.4 (R-401): the HOME role did not stick on some TVs (ignored, back to the stock home after a reboot, or a Chromecast's setup wizard took it), and the
 * home re-applied it once, then said nothing. At BOOT_COMPLETED the holder is read again (SetupHook.roleAtBoot) and logged, and while the person's home
 * is ours (HOME_WANTED) and the role is not: the role is given back once (over our own kept adb link), and when that was done already or did not hold,
 * the home says so ONCE at its next start (RoleNotice), until the role is ours again. Never over the setup wizard (setupwraith: a Chromecast's casting and
 * a TV's saved Wi-Fi hang on it; the add-on's Home catch is the way there), never while a setup runs. No Android in here (tests/RoleBootRun.java).
 */
public final class RoleBootRule {
    private RoleBootRule() { }

    public static final int NONE = 0, REASSERT = 1, NOTICE = 2;

    /** the notice file's words (SetupHook.ROLE_NOTICE): nothing, waiting for the home's next start, said */
    public static final String PENDING = "pending", TOLD = "told";

    /**
     * @param wanted the person's home is ours (HOME_WANTED)   @param ours our home holds HOME now   @param holder the package Android opens for HOME
     * @param reapplied the role was given back once since the person's last switch (HOME_REAPPLIED)   @param notice the notice file's word
     * @param busy a setup runs, the safety switch waits, or the person was offered the stock home back
     */
    public static int decide(boolean wanted, boolean ours, String holder, boolean reapplied, String notice, boolean busy) {
        if (!wanted || ours || busy) return NONE;
        boolean told = TOLD.equals(notice) || PENDING.equals(notice);
        if (StockHome.WRAITH.equals(holder)) return told ? NONE : NOTICE;
        if (!reapplied) return REASSERT;
        return told ? NONE : NOTICE;
    }

    /** the log's word for the holder: ours, the setup wizard, a settings stand-in, a known stock home by its package, another app, none (never an app the person installed by name) */
    public static String holderWord(String pkg, String cls, String self) {
        if (pkg == null || pkg.isEmpty()) return "none";
        if (pkg.equals(self)) return "ours";
        if (pkg.equals(StockHome.WRAITH)) return "setup wizard";
        if (!StockHome.real(pkg, cls)) return "settings";
        for (String k : StockHome.KNOWN) if (k.equals(pkg)) return k;
        return "another app";
    }
}
