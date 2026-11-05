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

    /** Is the stocks card («الأسهم») one of them? Then it is a button too: OK opens «الأسواق» (MarketsActivity). */
    static boolean hasStocks(String[] choice) {
        if (choice == null || "none".equals(choice[1])) return false;
        for (String k : choice[1].split(",")) if ("stocks".equals(k.trim())) return true;
        return false;
    }

    /** Does the stocks card come before «المهام» (the targets' order for the D-pad)? */
    static boolean stocksFirst(String[] choice) {
        if (choice == null) return false;
        java.util.List<String> l = java.util.Arrays.asList(choice[1].replace(" ", "").split(","));
        return l.contains("stocks") && (!l.contains("tasks") || l.indexOf("stocks") < l.indexOf("tasks"));
    }

    /**
     * The cards' page as the launcher opens it: url(), and live=1 added when «المهام» or the stocks card is among the cards (the page then
     * draws their ring, and «المهام»'s mic mark).
     */
    static String liveUrl(String[] choice) {
        String u = url(choice);
        return u != null && (hasTasks(choice) || hasStocks(choice)) ? u + "&live=1" : u;
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

    /**
     * The football home's widgets (docs/PROPOSALS-NOV2.md §1): the weather, the prayers and today's games, on the settings' fragment without
     * its card choice; null when nothing is known (the classic home then, as before).
     */
    static String footballUrl(String[] choice) {
        if (choice == null) return null;
        String frag = choice[0].replaceAll("(^|&)(panel|week)=[^&]*", "");
        return base(choice[0]) + frag + "&widget=1&football=1";
    }

    /** «المباريات», the page of the games, on that fragment; event opens one game's card at once (null or "" for none). */
    static String matchesUrl(String frag, String event) {
        String f = frag == null ? "" : frag.replaceAll("(^|&)(panel|week|widget|home_panel)=[^&]*", "");
        return base(frag) + f + "&matchpage=1" + (event != null && event.matches("\\d{1,12}") ? "&event=" + event : "");
    }

    /** The page for the language the settings fragment says (as Tr.fromFragment): BASE for Arabic, as always. */
    static String base(String frag) {
        if (frag != null) for (String kv : frag.split("&")) if (kv.equals("lang=en")) return BASE_EN;
        return BASE;
    }
}
