package sa.urtv.home;

/** LangProvider's rules, plain Java (tests/lang_provider_test.py): the one URI it answers, and the only two values it ever gives. */
final class LangRule {
    private LangRule() { }

    /** content://sa.urtv.home.lang/lang only (a trailing slash allowed); any other path or authority gets nothing */
    static boolean isLangUri(String authority, String path) {
        return "sa.urtv.home.lang".equals(authority) && ("/lang".equals(path) || "/lang/".equals(path));
    }

    /** "en" only for English; anything else (unset, unknown) is "ar", the default */
    static String code(String lang) { return "en".equals(lang) ? "en" : "ar"; }
}
