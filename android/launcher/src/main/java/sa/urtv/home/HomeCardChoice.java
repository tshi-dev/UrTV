package sa.urtv.home;

/**
 * Which page the home card shows, from what the screensaver's settings say now and what this app last read from them.
 * Plain Java (no Android classes) so tests/home_card_test.py can run it.
 */
final class HomeCardChoice {
    static final String BASE = "file:///android_asset/web/tv.html#";
    /**
     * «اللغة» English (the settings fragment carries lang=en, Tr.fragment): the English page, Tr.pageName("en"), which copyWeb puts beside
     * tv.html. Written out here so this class stays plain Java on its own (tests/i18n_page_test.py checks it against Tr).
     */
    static final String BASE_EN = "file:///android_asset/web/tv-en.html#";
    static final String PRAYERS = BASE + "panel=prayers&widget=1";

    private HomeCardChoice() { }

    /**
     * The settings to use as {fragment, panel}: the ones just read (both non-null), else the last ones read
     * successfully, else null (nothing known).
     */
    static String[] pick(String frag, String panel, String savedFrag, String savedPanel) {
        if (frag != null && panel != null) return new String[] { frag, panel };
        if (savedFrag != null && savedPanel != null) return new String[] { savedFrag, savedPanel };
        return null;
    }

    /** How many cards a pick shows (1 to 3); the prayers alone when nothing is known. */
    static int count(String[] choice) {
        if (choice == null) return 1;
        return Math.max(1, Math.min(3, choice[1].split(",").length));
    }

    /** Is «المهام» one of the cards a pick shows? Then the card is a button on the home screen (the page is told with live=1). */
    static boolean hasTasks(String[] choice) {
        if (choice == null || "none".equals(choice[1])) return false;
        for (String k : choice[1].split(",")) if ("tasks".equals(k.trim())) return true;
        return false;
    }

    /** The cards' page as the launcher opens it: url(), and live=1 added when «المهام» is among the cards (its mic mark and its ring). */
    static String liveUrl(String[] choice) {
        String u = url(choice);
        return u != null && hasTasks(choice) ? u + "&live=1" : u;
    }

    /**
     * The cards' page for a pick: null when the user turned them off («بدون»), the prayers when nothing is known. The panel is the
     * cards in order, "a,b,c" (a single "a" is what older screensavers said): panel= carries the first, for a page that knows
     * only one, and home_panel= all of them.
     */
    static String url(String[] choice) {
        if (choice == null) return PRAYERS;
        if ("none".equals(choice[1]) || choice[1].isEmpty()) return null;
        String frag = choice[0].replaceAll("(^|&)(panel|week)=[^&]*", "");   // the screensaver's own card choice
        return base(choice[0]) + frag + "&panel=" + choice[1].split(",")[0] + "&home_panel=" + choice[1] + "&widget=1";
    }

    /** The page for the language the settings fragment says (as Tr.fromFragment): BASE for Arabic, as always. */
    static String base(String frag) {
        if (frag != null) for (String kv : frag.split("&")) if (kv.equals("lang=en")) return BASE_EN;
        return BASE;
    }
}
