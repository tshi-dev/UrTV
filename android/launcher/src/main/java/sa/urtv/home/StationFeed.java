package sa.urtv.home;

import android.content.Context;

/**
 * 4.4: the stations check's status (urtv-data stations/status.json, written by .github/tools/check_stations.py) for the player
 * (sa.urtv.home.tilawah.StationStatus): the kept copy at once, fetched again in the background at most every six hours (ContentFile).
 */
public final class StationFeed {
    private StationFeed() { }

    static final String PATH = "stations/status.json";
    static final String NAME = "stations-status.json";

    /** the kept status (null: none yet, or a bad one), and a background refresh when it is old */
    public static java.util.Map<?, ?> status(Context c) {
        Context app = c.getApplicationContext();
        ContentFile.refresh(app, PATH, NAME, 6 * 3600_000L);
        Object o = ContentFile.read(app, NAME);
        return o instanceof java.util.Map ? (java.util.Map<?, ?>) o : null;
    }
}
