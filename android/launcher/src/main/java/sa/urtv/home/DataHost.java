package sa.urtv.home;

import java.io.IOException;

/**
 * 4.3.3 Where the apps' data files live: the data repository tshi-dev/urtv-data first, then the old data branch of this repository
 * (docs/DATA-HOST.md). One map, the same in the launcher (this file), the screensaver (sa.urtv.screensaver.DataHost) and the page (DATA_MAP,
 * src/template.html). A caller still names the old address (CI's branches keep running for the apps already on TVs); get() asks the new
 * address first and the old one when the new answers anything but 200 or cannot be reached, so a file not moved yet costs one 404.
 */
final class DataHost {
    private DataHost() {}

    static final String OLD = "https://raw.githubusercontent.com/tshi-dev/UrTV/";
    static final String NEW = "https://raw.githubusercontent.com/tshi-dev/urtv-data/data/";
    /** {the old branch and folder, the folder in urtv-data}: the first that matches wins (the quiz before the other games) */
    static final String[][] MAP = {
        {"data-leagues/leagues/", "football/leagues/"},
        {"data-catalogue/catalogue/", "football/catalogue/"},
        {"data-archive/", "football/archive/"},
        {"data-games/quiz/", "games/quiz/"},
        {"data-games/games/", "games/"},
        {"data-markets/markets/", "markets/"},
        {"data-stations/stations/", "stations/"},
    };

    /** the addresses to try for `url`, in order: the new one first when the map knows it, then `url` itself */
    static String[] urls(String url) { return urls(OLD, NEW, url); }

    static String[] urls(String old, String now, String url) {
        if (url.startsWith(old)) {
            String rest = url.substring(old.length());
            for (String[] m : MAP) if (rest.startsWith(m[0])) return new String[] {now + m[1] + rest.substring(m[0].length()), url};
        }
        return new String[] {url};
    }

    /** Net.get of each address of urls(url) in turn, the deadline shared: the first 200, else the last answer (or its IOException) */
    static Net.Reply get(String url, int readMs, int deadlineMs, int maxBytes) throws IOException {
        return getWith(OLD, NEW, url, readMs, deadlineMs, maxBytes);
    }

    /** get() with the two hosts given (the test's local server) */
    static Net.Reply getWith(String old, String now, String url, int readMs, int deadlineMs, int maxBytes) throws IOException {
        long end = System.nanoTime() + deadlineMs * 1_000_000L;
        String[] v = urls(old, now, url);
        Net.Reply last = null;
        IOException err = null;
        for (int i = 0; i < v.length; i++) {
            int left = (int) ((end - System.nanoTime()) / 1_000_000L);
            if (i > 0 && left < 1000) left = 1000;   // the old address always gets its chance, at least a second
            try {
                Net.Reply r = Net.get(v[i], null, readMs, left, maxBytes);
                if (r.status == 200) return r;
                last = r;
            } catch (IOException e) {
                err = e;
            }
        }
        if (last != null) return last;
        throw err != null ? err : new IOException("no address");
    }

    /**
     * get() through Net.cached (a kept copy with its ETag: an unchanged file is answered from the copy): each address of urls(url) in turn, the deadline shared, the first 200
     * (a copy counts as 200), else the last answer (or its IOException). The copy is kept per address, so a file that moves from the old branch to urtv-data costs one fresh download.
     */
    static Net.Reply cached(java.io.File dir, String url, int readMs, int deadlineMs, int maxBytes, long freshMs, boolean backOff) throws IOException {
        long end = System.nanoTime() + deadlineMs * 1_000_000L;
        String[] v = urls(OLD, NEW, url);
        Net.Reply last = null;
        IOException err = null;
        for (int i = 0; i < v.length; i++) {
            int left = (int) ((end - System.nanoTime()) / 1_000_000L);
            if (i > 0 && left < 1000) left = 1000;
            try {
                Net.Reply r = Net.cached(dir, v[i], readMs, left, maxBytes, freshMs, backOff);
                if (r.status == 200) return r;
                last = r;
            } catch (IOException e) {
                err = e;
            }
        }
        if (last != null) return last;
        throw err != null ? err : new IOException("no address");
    }
}
