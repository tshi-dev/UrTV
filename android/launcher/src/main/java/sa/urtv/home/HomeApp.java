package sa.urtv.home;

import android.app.Application;

/** The process of the home: starts what must live as long as it does (MuteKeep's screen and shutdown receivers). */
public final class HomeApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        CrashLoopStore.hook(this);   // 4.3.5 (R7): first, so a crash anywhere after it is counted (CrashLoop, SafeActivity)
        MuteKeep.start(this);
        BetaNotes.hookCrashes(this);   // 4.3.3: the class name of a crash, for the beta note (BetaNotes)
        ZoneFix.logger = line -> android.util.Log.i("UrTVZone", line);   // issue #55: one line when the page corrects the TV's zone data
    }
}
