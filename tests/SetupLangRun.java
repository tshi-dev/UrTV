package sa.urtv.home;

/** For tests/setup_lang_test.py: the language rules with the JVM (and so "the TV") set to English: still Arabic unless the setting says en. */
public final class SetupLangRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        java.util.Locale.setDefault(java.util.Locale.ENGLISH);
        check("a TV whose language is English: nothing set means Arabic", Tr.norm(null).equals("ar") && Tr.norm("").equals("ar") && Tr.norm("en_US").equals("ar") && Tr.norm("EN").equals("ar"));
        check("only the setting «en» makes English", Tr.norm("en").equals("en") && Tr.norm("ar").equals("ar"));
        check("the settings fragment of a fresh install says nothing: Arabic", Tr.fromFragment("city=Riyadh").equals("ar") && Tr.fromFragment(null).equals("ar") && Tr.fragment("ar").isEmpty());
        check("Arabic text is given back as it is, whatever the JVM language", Tr.t("ar", null, "جهّز كل شي").equals("جهّز كل شي"));
        check("the pages: Arabic by default, English only for en", Tr.pageName("ar").equals("tv.html") && Tr.pageName(Tr.norm(null)).equals("tv.html") && Tr.pageName("en").equals("tv-en.html"));
        check("the setting accepts ar and en only", Tr.valid("ar") && Tr.valid("en") && !Tr.valid("fr") && !Tr.valid(null));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }
}
