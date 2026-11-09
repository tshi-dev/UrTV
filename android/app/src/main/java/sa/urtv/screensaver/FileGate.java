package sa.urtv.screensaver;

/**
 * S1: what a page in a WebView may read through file:// . The page can read its own file and the app's assets, nothing else: the app's folders (shared_prefs
 * with the private records, the caches, the kept pages and photos) are never given to a page, whatever it asks for (the page allows file access
 * for its own fonts and photos, so without this a script on it could read them). No Android in here: tests/FileGateRun.java runs it.
 */
final class FileGate {
    private FileGate() { }

    /** the app's assets in a file:// address */
    static final String ASSETS = "/android_asset/";

    /** a path of a file:// request: allowed when it is the page itself, or inside the assets (no «..» anywhere in it) */
    static boolean allowed(String path, String pagePath) {
        if (path == null || path.isEmpty() || path.contains("..") || path.contains("%2e") || path.contains("%2E") || path.indexOf('\0') >= 0) return false;
        if (pagePath != null && path.equals(pagePath)) return true;
        return path.startsWith(ASSETS) && path.length() > ASSETS.length();
    }
}
