package sa.urtv.home;

/** For tests/app_search_test.py: AppSearch (which apps take a search intent, the words sent, the class name logged). Prints PASS/FAIL lines. */
public final class AppSearchRun {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        check("YouTube TV takes a search by intent", AppSearch.supports("com.google.android.youtube.tv"));
        check("no other app is assumed to", !AppSearch.supports("com.netflix.ninja") && !AppSearch.supports(null) && !AppSearch.supports("") && !AppSearch.supports("com.google.android.youtube"));
        check("the words are trimmed", "اغاني هادئة".equals(AppSearch.query("  اغاني هادئة \n")));
        check("a control character becomes a space", "a b".equals(AppSearch.query("a\tb")));
        check("nothing to search is null", AppSearch.query(null) == null && AppSearch.query("  \n ") == null && AppSearch.query("") == null);
        StringBuilder long1 = new StringBuilder();
        for (int i = 0; i < 300; i++) long1.append('x');
        check("the words are cut at 100 characters", AppSearch.query(long1.toString()).length() == 100);
        check("a class name is logged as it is", "android.widget.FrameLayout".equals(AppSearch.safeClass("android.widget.FrameLayout")) && "a.B$C_1".equals(AppSearch.safeClass("a.B$C_1")));
        check("no node is 'none'", "none".equals(AppSearch.safeClass(null)) && "none".equals(AppSearch.safeClass("")));
        check("anything else in a name is hidden", "a?b?c".equals(AppSearch.safeClass("a b/c")) && "??".equals(AppSearch.safeClass("اب")));
        check("a class name is cut at 80", AppSearch.safeClass(long1).length() == 80);
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
