package sa.urtv.home;

/** For tests/lang_provider_test.py: LangRule. One line per check: name TAB ok|FAIL. */
public final class LangRuleRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    public static void main(String[] a) {
        check("the one URI", LangRule.isLangUri("sa.urtv.home.lang", "/lang") && LangRule.isLangUri("sa.urtv.home.lang", "/lang/"));
        check("any other path or authority: nothing", !LangRule.isLangUri("sa.urtv.home.lang", "/") && !LangRule.isLangUri("sa.urtv.home.lang", "/lang/x")
            && !LangRule.isLangUri("sa.urtv.home.lang", null) && !LangRule.isLangUri("sa.urtv.home.glass", "/lang") && !LangRule.isLangUri(null, "/lang"));
        check("only ar or en: English is en, anything else ar", LangRule.code("en").equals("en") && LangRule.code("ar").equals("ar")
            && LangRule.code(null).equals("ar") && LangRule.code("fr").equals("ar") && LangRule.code("EN").equals("ar"));
    }
}
