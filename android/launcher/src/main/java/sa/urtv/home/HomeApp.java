package sa.urtv.home;

import android.app.Application;

/** The process of the home: starts what must live as long as it does (MuteKeep's screen and shutdown receivers). */
public final class HomeApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        MuteKeep.start(this);
        ZoneFix.logger = line -> android.util.Log.i("UrTVZone", line);   // issue #55: one line when the page corrects the TV's zone data
    }
}
