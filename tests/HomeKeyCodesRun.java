package sa.urtv.home;

/** For tests/all_apps_home_test.py: RemoteButtons.homeKey (which keys are the Home key on which TV). Prints PASS/FAIL lines. */
public final class HomeKeyCodesRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        check("on a Xiaomi TV (maker or brand, any case, any model) ALL_APPS (284) is the Home key", RemoteButtons.homeKey(284, "Xiaomi", "Xiaomi")
            && RemoteButtons.homeKey(284, "xiaomi", "MiTV") && RemoteButtons.homeKey(284, "unknown", "Xiaomi"));
        check("KEYCODE_HOME is the Home key everywhere", RemoteButtons.homeKey(3, "TCL", "TCL") && RemoteButtons.homeKey(3, null, null) && RemoteButtons.homeKey(3, "Xiaomi", "Xiaomi"));
        check("elsewhere ALL_APPS stays itself", !RemoteButtons.homeKey(284, "TCL", "TCL") && !RemoteButtons.homeKey(284, null, null)
            && !RemoteButtons.homeKey(284, "Philips", "Philips") && !RemoteButtons.homeKey(284, "Formovie", "Formovie"));
        check("other keys are not the Home key", !RemoteButtons.homeKey(4, "Xiaomi", "Xiaomi") && !RemoteButtons.homeKey(82, "Xiaomi", "Xiaomi"));
        System.out.println(failed == 0 ? "ALL PASS" : failed + " FAILED");
    }
}
