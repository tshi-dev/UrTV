package sa.urtv.home;

import java.util.Arrays;
import java.util.List;

/** For tests/tv_rows_test.py: TvRowsPick on a plain JVM. Prints "name = value" lines. */
public final class TvRowsRun {
    static final StringBuilder out = new StringBuilder();

    static void put(String k, Object v) { out.append(k).append(" = ").append(v).append('\n'); }

    public static void main(String[] args) {
        put("watchnext_title", TvRowsPick.WATCH_NEXT_TITLE);

        put("wn_continue", TvRowsPick.watchNextShown(0, true, true));
        put("wn_next", TvRowsPick.watchNextShown(1, true, true));
        put("wn_new", TvRowsPick.watchNextShown(2, true, true));
        put("wn_watchlist", TvRowsPick.watchNextShown(3, true, true));
        put("wn_hidden", TvRowsPick.watchNextShown(0, false, true));
        put("wn_no_app", TvRowsPick.watchNextShown(0, true, false));

        put("meta_episode", TvRowsPick.meta("2", "1", "Pilot", "Show", "", "Netflix", false));
        put("meta_latin", TvRowsPick.meta("2", "10", "", "Show", "", "Netflix", true));
        put("meta_eptitle", TvRowsPick.meta("", "", "The Long Night", "Show", "", "", false));
        put("meta_same_title", TvRowsPick.meta("", "", "Show", "Show", "desc here", "", false));
        put("meta_nulls", TvRowsPick.meta("null", null, "null", "Show", "null", "", false));

        put("link_package", TvRowsPick.linkAllowed("com.netflix.ninja", "com.netflix.ninja", null));
        put("link_component", TvRowsPick.linkAllowed("com.netflix.ninja", null, "com.netflix.ninja"));
        put("link_other_component", TvRowsPick.linkAllowed("com.netflix.ninja", null, "com.evil.app"));
        put("link_other_package", TvRowsPick.linkAllowed("com.netflix.ninja", "com.evil.app", null));
        put("link_mixed", TvRowsPick.linkAllowed("com.netflix.ninja", "com.evil.app", "com.netflix.ninja"));
        put("link_none", TvRowsPick.linkAllowed("com.netflix.ninja", null, null));
        put("link_no_row", TvRowsPick.linkAllowed(null, "x", "x"));

        // a huge picture (an 8000×6000 JPEG, 190 MB at full size) is decoded at 1/8 and then fitted to 400×400; a tall poster too
        int s = TvRowsPick.sampleSize(8000, 6000, 400, 400);
        put("sample_huge", s + " " + (8000 / s) + "x" + (6000 / s) + " " + ((8000L / s) * (6000 / s) * 4 / 1024) + "KB");
        int[] f = TvRowsPick.fitted(8000 / s, 6000 / s, 400, 400);
        put("fit_huge", f[0] + "x" + f[1]);
        int t = TvRowsPick.sampleSize(1000, 30000, 400, 400);
        put("sample_tall", t + " " + (1000 / t) + "x" + (30000 / t));
        put("fit_tall", String.join("x", String.valueOf(TvRowsPick.fitted(1000 / t, 30000 / t, 400, 400)[0]), String.valueOf(TvRowsPick.fitted(1000 / t, 30000 / t, 400, 400)[1])));
        put("sample_small", TvRowsPick.sampleSize(320, 180, 400, 400) + " " + TvRowsPick.fitted(320, 180, 400, 400)[0]);
        put("sample_none", TvRowsPick.sampleSize(0, 0, 400, 400) + " " + TvRowsPick.sampleSize(-1, 50, 400, 400));
        put("provider_own", TvRowsPick.pictureProviderAllowed("com.netflix.ninja", "com.netflix.ninja"));
        put("provider_other", TvRowsPick.pictureProviderAllowed("com.netflix.ninja", "com.android.providers.media"));
        put("provider_none", TvRowsPick.pictureProviderAllowed("com.netflix.ninja", null));
        put("sig_same", TvRowsPick.signature(Arrays.asList(new String[]{"a", "t1"}, new String[]{"b"})).equals(TvRowsPick.signature(Arrays.asList(new String[]{"a", "t1"}, new String[]{"b"}))));
        put("sig_diff", TvRowsPick.signature(Arrays.asList(new String[]{"a", "t1"}, new String[]{"b"})).equals(TvRowsPick.signature(Arrays.asList(new String[]{"a"}, new String[]{"t1", "b"}))));

        // the row on a 1080p TV (960x540 dp): the hero's 48 dp sides plus the dock band's 20 dp padding, before and after the favourites come
        put("cells_before", TvRowsPick.cells(0, 5));
        put("cells_after", TvRowsPick.cells(7, 5));
        for (int n : new int[] {1, 5, 7, 10})
            put("card_" + n, String.format(java.util.Locale.US, "%.1fx%.1f", TvRowsPick.cellWidth(960, 68, n), TvRowsPick.cardHeight(TvRowsPick.cellWidth(960, 68, n))));

        put("progress_half", TvRowsPick.progress(30, 60));
        put("progress_over", TvRowsPick.progress(90, 60));
        put("progress_unknown", TvRowsPick.progress(0, 60) + " " + TvRowsPick.progress(10, 0));
        System.out.print(out);
    }
}
