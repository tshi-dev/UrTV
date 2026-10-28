package sa.urtv.home;

/**
 * Typing from the phone into an app that has no text field on screen (YouTube TV's search box is not an editable node to Android): for the
 * apps that take a search by Intent.ACTION_SEARCH, the words go to that app's own search instead. Plain Java (no Android classes) so
 * tests/AppSearchRun.java can run it; TypeService starts the intent. Only YouTube TV: another app is added here when it documents a search
 * intent (Netflix and Shahid have none that is public).
 */
final class AppSearch {
    static final String YOUTUBE = "com.google.android.youtube.tv";
    static final int MAX_QUERY = 100;   // characters; a search box takes no more than this in any app

    private AppSearch() { }

    /** Does this app take its search by ACTION_SEARCH with the words in SearchManager.QUERY? */
    static boolean supports(String pkg) { return YOUTUBE.equals(pkg); }

    /** The words to search: control characters become spaces, the ends are trimmed, at most MAX_QUERY characters; null when nothing is left. */
    static String query(String text) {
        if (text == null) return null;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < text.length() && b.length() < MAX_QUERY; i++) {
            char c = text.charAt(i);
            b.append(Character.isISOControl(c) ? ' ' : c);
        }
        String q = b.toString().trim();
        return q.isEmpty() ? null : q;
    }

    /** The web search link YouTube TV opens as its own search (the words percent-encoded, spaces as %20). */
    static String webUrl(String query) {
        try {
            return "https://www.youtube.com/results?search_query=" + java.net.URLEncoder.encode(query, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            return "https://www.youtube.com/results";
        }
    }

    /** A view's class name as the log may carry it (never its text or id): letters, digits, dot, dollar and underscore only, at most 80; "none" for no node. */
    static String safeClass(CharSequence name) {
        if (name == null || name.length() == 0) return "none";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < name.length() && b.length() < 80; i++) {
            char c = name.charAt(i);
            b.append(c < 128 && (Character.isLetterOrDigit(c) || c == '.' || c == '$' || c == '_') ? c : '?');
        }
        return b.toString();
    }
}
