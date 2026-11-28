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
        if (choice[1] == null || choice[1].isEmpty() || "none".equals(choice[1])) return 0;   // 4.4 beta 4: «بدون» shows none (it counted 1: the glass clock stayed off with no card at all)
        return Math.max(1, Math.min(3, choice[1].split(",").length));
    }

    /** Is «المهام» one of the cards a pick shows? Then the card is a button on the home screen (the page is told with live=1). */
    /** 4.4 «أجزاء UrTV»: each card and the part it goes with (a card of no part stays whatever is off) */
    static final String[][] CARD_PARTS = {
        {"prayers", "prayer"}, {"prayerrow", "prayer"}, {"nextprayer", "prayer"},
        {"forecast", "weather"}, {"hourly", "weather"},
        {"matches", "matches"}, {"nextmatch", "matches"}, {"matchday", "matches"}, {"livenow", "matches"}, {"minitable", "matches"},
        {"nextmatches", "matches"}, {"yesterday", "matches"}, {"weekend", "matches"}, {"cuppath", "matches"}, {"formula", "matches"},
        {"stocks", "markets"}, {"tasks", "tasks"}, {"adhkar", "duas"},
    };

    /** the cards without those of the parts that are off («البطاقات» off: none); "none" when nothing is left */
    /** a part named in the fragment's «&off=» (4.4 «أجزاء UrTV»); here so this file compiles on its own (PartsHome uses it too) */
    static boolean partOffIn(String frag, String id) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:^|&)off=([a-z,]*)").matcher(frag == null ? "" : frag);
        if (!m.find()) return false;
        for (String k : m.group(1).split(",")) if (k.equals(id)) return true;
        return false;
    }

    static String withoutParts(String panel, String frag) {
        if (panel == null || "none".equals(panel)) return panel;
        if (partOffIn(frag, "cards")) return "none";
        StringBuilder b = new StringBuilder();
        for (String k : panel.split(",")) {
            String part = null;
            for (String[] cp : CARD_PARTS) if (cp[0].equals(k.trim())) part = cp[1];
            if (part != null && partOffIn(frag, part)) continue;
            b.append(b.length() == 0 ? "" : ",").append(k.trim());
        }
        return b.length() == 0 ? "none" : b.toString();
    }

    /** the audit (H5): a card with today's prayer times is among them (its «المغرب» is the sunset, so the greeting's «الغروب» line would say it twice) */
    static boolean hasPrayerTimes(String[] choice) {
        if (choice == null || choice[1] == null || "none".equals(choice[1])) return false;
        for (String k : choice[1].split(",")) { k = k.trim(); if ("prayers".equals(k) || "prayerrow".equals(k) || "nextprayer".equals(k)) return true; }
        return false;
    }

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

    /** 4.3.4: the football home's three widgets are the home's cards on that layout; «بدون» ("none") turns them off as on the other layouts. Nothing known (null) is not the football home, as before. */
    static boolean football(String layout, String[] choice) {
        return "football".equals(layout) && choice != null && choice[1] != null && !choice[1].isEmpty() && !"none".equals(choice[1]);
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
