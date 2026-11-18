package sa.urtv.home.setup;

/**
 * 4.5 (the owner's «with a protective way»): on a TV where `cmd role add-role-holder` alone does not make our home the HOME role holder (Android 10+, the
 * role exists), the stock holder is taken off the role first, and only under these guards:
 * 1. the holder taken off is written OUTSIDE this app's data ({@link #JOURNAL}, read back) before anything is removed: an uninstall, a «clear data» or the rescue
 *    app still finds it;
 * 2. the safety switch (SetupEngine's deadman, 5 minutes) is already armed: the stock home comes back by itself unless ours is confirmed with a real Home press;
 * 3. nothing is disabled or uninstalled on this path: the stock home stays installed and enabled;
 * 4. add-role-holder failing, or the Home press not opening ours, puts the recorded holder back at once (its home activity too) and says so;
 * 5. every restore (the safety switch, «تراجع», «رجّع واجهة المصنع», the rescue app) gives the role back to the recorded holder;
 * 6. the setup wizard and the TV's Settings are never taken off the role.
 * {@link #STOCK_HOMES}: which package is the stock home on which brand, as data only (for detection and for the restore): the package names are the facts listed in
 * tvOS Launcher's Perms.java (github.com/techgenius1/tvOS-Launcher, Apache-2.0), grouped by brand here. No Android in here (tests/HomeRoleRun.java).
 */
final class HomeRole {
    private HomeRole() { }

    /** `cmd role` exists from Android 10 */
    static final int MIN_SDK = 29;
    static final String ROLE = "android.app.role.HOME";
    /** outside the app's data: the shell user's own folder, kept when UrTV is removed or its data cleared; the rescue reads it over the same adb */
    static final String JOURNAL = "/data/local/tmp/urtv-home-role.txt";
    static final String READ = "cat " + JOURNAL + " 2> /dev/null";
    static final String CLEAR = "rm -f " + JOURNAL;

    /** brand, then its stock home packages (tvOS Launcher's list, Apache-2.0; grouped by brand here) */
    static final String[][] STOCK_HOMES = {
        {"google", "com.google.android.apps.tv.launcherx", "com.google.android.tvlauncher", "com.google.android.leanbacklauncher"},
        {"philips", "org.droidtv.ui.home", "org.droidtv.home", "com.tpvision.home"},
        {"tcl", "com.tcl.cyberui", "com.tcl.tv.launcher", "com.tcl.launcher", "com.falcon.launcher", "com.ffalcon.launcher"},
        {"hisense", "com.hisense.tv.launcher"},
        {"xiaomi", "com.mi.tv.launcher", "com.mitv.tvhome"},
        {"sony", "com.sony.dtv.homemenu"},
        {"skyworth", "com.skyworth.tvos.launcher", "com.coocaa.tv.launcher"},
        {"haier", "com.haier.tv.launcher"},
        {"homwee", "com.homwee.launcher"},
        {"whaleos", "com.zeasn.whaleos.launcher"},
        {"sdmc", "com.sdmc.launcher"},
        {"amazon", "com.amazon.tv.launcher", "com.amazon.firehomestarter"},
    };

    /** never taken off the role (rule 6): the setup wizard and the TV's Settings (and anything of ours) */
    static final String[] NEVER = {"com.google.android.tungsten.setupwraith", "com.android.tv.settings", "com.android.settings"};

    static final String NOT_REMOVABLE = "ما بدّلنا الشاشة الرئيسية: صاحبها الحالي من نظام التلفزيون (وسيط الإعداد أو الإعدادات)، وما نلمسه. ما تغيّر شيء.";
    static final String NOT_RECORDED = "ما قدرنا نحفظ اسم الشاشة الرئيسية الأصلية خارج التطبيق قبل التبديل، فما بدّلناها. ما تغيّر شيء.";
    static final String ADD_FAILED = "ما قبل التلفزيون شاشتنا شاشةً رئيسية بعد إخلاء الدور، فرجّعنا الشاشة الأصلية فوراً.";
    static final String VERIFY_FAILED = "زر الرئيسية ما فتح UrTV بعد التبديل، فرجّعنا الشاشة الأصلية فوراً.";
    static final String PUT_BACK_OK = " الشاشة الأصلية رجعت الآن.";
    static final String PUT_BACK_PENDING = " ما تأكدنا من رجوعها بعد: مفتاح الأمان يرجّعها خلال 5 دقائق.";
    static final String DONE = "تلفزيونك الآن هو الشاشة الرئيسية (أخلينا الدور من الشاشة الأصلية وبقيت مثبّتة وشغّالة). اضغط زر الرئيسية في الريموت للتأكد.";

    static boolean pkg(String p) { return p != null && p.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"); }

    /** the brand whose stock home this package is, or "" */
    static String brandOf(String pkg) {
        if (pkg == null) return "";
        for (String[] b : STOCK_HOMES) for (int i = 1; i < b.length; i++) if (b[i].equals(pkg)) return b[0];
        return "";
    }

    static boolean isStockHome(String pkg) { return !brandOf(pkg).isEmpty(); }

    /** whether this holder may be taken off the role (rule 6): a package name, not ours, not the setup wizard, not Settings */
    static boolean removable(String holder) {
        if (!pkg(holder) || holder.startsWith("sa.urtv.")) return false;
        for (String n : NEVER) if (n.equals(holder)) return false;
        return true;
    }

    /** the command that writes the holder outside the app's data (rule 1); only a package name is ever written */
    static String record(String holder) {
        if (!pkg(holder)) throw new IllegalArgumentException("not a package: " + holder);
        return "printf '%s\\n' " + holder + " > " + JOURNAL;
    }

    /** the recorded holder (the first line, a package name), or "" */
    static String parse(String text) {
        if (text == null) return "";
        for (String l : text.replace("\r", "").split("\n")) {
            String s = l.trim();
            if (s.isEmpty()) continue;
            return pkg(s) && removable(s) ? s : "";
        }
        return "";
    }

    static String remove(String holder) { return "cmd role remove-role-holder --user 0 " + ROLE + " " + holder; }
    static String add(String pkg) { return "cmd role add-role-holder --user 0 " + ROLE + " " + pkg; }

    /** «add alone failed»: the role exists (Android 10+) and after add-role-holder ours is not its holder (or the command failed) */
    static boolean addAloneFailed(int sdk, int addCode, String holderAfter, String self) {
        return sdk >= MIN_SDK && (addCode != 0 || !self.equals(holderAfter));
    }
}
