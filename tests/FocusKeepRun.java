package sa.urtv.home;

/** For tests/home_focus_keep_test.py: FocusKeep.java on a plain JVM. Prints PASS/FAIL lines. */
public final class FocusKeepRun {
    static int failed;
    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }
    public static void main(String[] args) {
        check("the focused app is kept once the person has moved", "com.netflix.ninja".equals(FocusKeep.keep(true, "com.netflix.ninja", null)), FocusKeep.keep(true, "com.netflix.ninja", null));
        check("after Home (not moved yet): nothing kept, the first favourite as Home wants", FocusKeep.keep(false, "com.netflix.ninja", null) == null, FocusKeep.keep(false, "com.netflix.ninja", null));
        check("an app asked for (an edit, «إضافة إلى المفضلة») wins over the focused one", "com.a".equals(FocusKeep.keep(true, "com.b", "com.a")), FocusKeep.keep(true, "com.b", "com.a"));
        check("the focus on no app (a pill, the clock): nothing kept", FocusKeep.keep(true, null, null) == null, "");
        System.exit(failed == 0 ? 0 : 1);
    }
}
