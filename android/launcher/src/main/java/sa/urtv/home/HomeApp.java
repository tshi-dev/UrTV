package sa.urtv.home;

import android.app.Application;

/** The process of the home: starts what must live as long as it does (MuteKeep's screen and shutdown receivers). */
public final class HomeApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        CrashLoopStore.hook(this);   // 4.3.5 (R7): first, so a crash anywhere after it is counted (CrashLoop, SafeActivity)
        if (BuildConfig.DEBUG) strict();   // 4.4 beta 4 (lag switching apps): a debug build logs disk and slow work on the main thread (tag StrictMode), never a release
        Extras.init(this);   // 4.3.3 «إضافات تلفزيونك»: the add-on is asked with this process's context
        MuteKeep.start(this);
        sa.urtv.home.setup.LedgerPush.maybe(this);   // the ledger of what the setup turned off, copied to the rescue app when it changed (LedgerKeep)
        SaverSettingsCopy.refreshSoon(this);   // 4.4: a copy of the screensaver's settings, for a reinstall of it (on a thread)
        BetaNotes.hookCrashes(this);   // 4.3.3: the class name of a crash, for the beta note (BetaNotes)
        ZoneFix.logger = line -> android.util.Log.i("UrTVZone", line);   // issue #55: one line when the page corrects the TV's zone data
        NetLog.logger = new NetLogcat();   // G3: one line per 10 minutes of requests (tag UrTVNet)
    }

    /** debug builds only: the main thread's disk reads and writes, network and custom slow calls, and leaked closables and activities, logged (never a crash) */
    private static void strict() {
        android.os.StrictMode.setThreadPolicy(new android.os.StrictMode.ThreadPolicy.Builder()
                .detectDiskReads().detectDiskWrites().detectNetwork().detectCustomSlowCalls().penaltyLog().build());
        android.os.StrictMode.setVmPolicy(new android.os.StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects().detectActivityLeaks().detectLeakedRegistrationObjects().penaltyLog().build());
    }
}
