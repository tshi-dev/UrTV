package sa.urtv.home.tilawah;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.PowerManager;
import android.util.Log;

import sa.urtv.home.R;
import java.io.File;
import java.util.Locale;

/**
 * Plays a surah or a live stream, keeps playing when the screensaver starts, and answers the
 * remote's media keys through a MediaSession. State is static because the screens read it too
 * (same process); the service owns the player and updates it.
 */
public class MusicService extends Service implements AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "Listening";
    private static final String CHANNEL = "playback";
    private static final int NOTIF = 41;

    static final String ACTION_PLAY_SURAH = "sa.urtv.home.PLAY_SURAH";
    static final String ACTION_PLAY_RADIO = "sa.urtv.home.PLAY_RADIO";
    static final String ACTION_PLAY_ADHAN = "sa.urtv.home.PLAY_ADHAN";
    static final String ACTION_PAUSE = "sa.urtv.home.PAUSE";
    static final String ACTION_RESUME = "sa.urtv.home.RESUME";
    static final String ACTION_TOGGLE = "sa.urtv.home.TOGGLE";
    static final String ACTION_NEXT = "sa.urtv.home.NEXT";
    static final String ACTION_PREV = "sa.urtv.home.PREV";
    static final String ACTION_STOP = "sa.urtv.home.STOP";
    static final String ACTION_SLEEP = "sa.urtv.home.SLEEP";
    static final String ACTION_SEEK_BY = "sa.urtv.home.SEEK_BY";
    /** the person stopped the adhan that is playing (OK on its bar, or any key): only that adhan ends, never the alarm for the next one */
    static final String ACTION_STOP_ADHAN = "sa.urtv.home.STOP_ADHAN";
    /** 4.3.3 G1: the home or the player is in front while ours plays without a foreground notice: ask Android for one now (FocusRule.promote) */
    static final String ACTION_KEEP = "sa.urtv.home.KEEP_FOREGROUND";
    /** 4.4 «الأدعية والتذكيرات»: a dua plays the adhan's way (the recitation paused and brought back, the bar and the card, its keys) */
    static final String ACTION_PLAY_DUA = "sa.urtv.home.PLAY_DUA";

    static final String EXTRA_RECITER = "reciter";
    static final String EXTRA_SURAH = "surah";
    static final String EXTRA_STATION = "station";
    static final String EXTRA_VOICE = "voice";
    static final String EXTRA_SECONDS = "seconds";
    static final String EXTRA_MINUTES = "minutes";
    static final String EXTRA_DELTA = "delta";

    private static volatile int source = Prefs.SOURCE_NONE;
    private static volatile boolean playing, buffering, prepared;
    private static volatile int reciter = -1, surah = 1, station = -1;
    private static volatile int position, duration;
    private static volatile String error;
    private static volatile long sleepUntil;
    private static volatile String adhanName;
    /** the prayer the adhan on is for (PrayerTimes' number), -1 when it was played from the computer or is a preview */
    private static volatile int adhanPrayer = -1;
    /** an adhan for a prayer is playing (not a preview): the bars on the home and the screensaver say so and how to stop it */
    private static volatile boolean adhanLive;
    /** 4.4: the name of the dua or reminder that plays now (it plays as an adhan without a prayer), null otherwise */
    private static volatile String duaNow;
    /** the dua asked for by playDua, taken by playAdhan; its volume */
    private String pendingDua;
    private int duaVolume = 30;
    /** 4.4 touch «يبدأ بهدوء»: this dua rises from silence in FadeIn.MS; when its fade began */
    private boolean duaFade, pendingFade;
    private long fadeInStart;
    private final Runnable fadeIn = new Runnable() {
        @Override public void run() {
            if (source != Prefs.SOURCE_ADHAN || duaNow == null || player == null) return;
            float g = FadeIn.gain(SystemClock.elapsedRealtime() - fadeInStart);
            try { player.setVolume(g, g); } catch (IllegalStateException ignored) { return; }
            if (g < 1f) handler.postDelayed(this, FadeIn.STEP_MS);
        }
    };
    /** for `optimize/music.sh status`: what happened since this app started, so a report of «يقطّع» says what the player saw */
    private static volatile int rebuffers, errors;
    private static volatile long rebufferMs, lastErrorAt;
    private static volatile String lastError = "";
    private static volatile String radioFault = "";   // 4.3.1 the last station fault for the report: RadioFault.record (no address)
    /** which player plays now ("exo" for a live HLS station, else "mediaplayer") and how many quiet restarts the stall watchdog made */
    private static volatile String playerKind = "";
    private static volatile int stallRestarts;
    /** 4.4: joins made because the network came back */
    private static volatile int reconnects;
    /** 4.3 A2: a station that stalls is restarted quietly (StallWatch), at most three times in two minutes */
    private final StallWatch stall = new StallWatch();
    /** 4.4: a surah that failed goes on from its place after a growing wait, and is said only after SurahRecover.MAX_FAILS in ten minutes */
    private final SurahRecover surahRecover = new SurahRecover();

    private Catalog cat;
    private Prefs prefs;
    private Playback player;   // the TV's MediaPlayer, or ExoPlayer for a live HLS station (4.3 A2)
    /** the player's own gain with a limiter (Loudness): the recordings are quiet next to TV sound; null where the TV has no such effect */
    private LoudnessEnhancer louder;
    private MediaSession session;
    private AudioManager audio;
    private WifiManager.WifiLock wifi;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean foregroundStarted;
    /** the same, for the screens and `music.sh status`: a service without it is ended by Android once our home is not in front (4.3.3 G1) */
    private static volatile boolean inForeground;
    /** paused by another sound, waiting for it to end (FocusRule) */
    private static volatile boolean waitingFocus;
    private boolean resumeOnFocus;
    /** 4.3.3 G1: after a loss for good, when it happened and how many looks in a row found nothing else playing (FocusRule.look) */
    private long focusLostAt;
    private int quietLooks;
    private final Runnable focusLook = new Runnable() {
        @Override public void run() { lookForFocus(); }
    };
    private int retries;
    /** what the person asked for (playing or not), apart from what the MediaPlayer is doing; and which player's callbacks are still current */
    private final PlayIntent wish = new PlayIntent();
    private long rebufferStart;
    private long shortStart;
    private int pendingSeek;
    private int fadeStep;
    /** the 1-second position tick runs only while something plays */
    private boolean ticking;
    private String currentUrl;
    private String radioAddress;   // the station's own address (StreamUrl resolves it again for every try: a redirect's address may not last)
    private String radioAlt;   // the source tried after the one in use (4.4: the next of radioSources), null when there is only one
    /** 4.4: all of the station's sources and the one in use (StationSources) */
    private java.util.List<String> radioSources = new java.util.ArrayList<>();
    private int radioSource;
    private final RadioPolicy.State radioState = new RadioPolicy.State();   // 4.3: what the pick has been through (RadioPolicy)

    // what was on when the adhan started, so it comes back afterwards
    private boolean resumeAfterAdhan;
    private int adhanVoice = 1;
    private boolean adhanFromCache;
    /** the adhan on is a preview (a few seconds of a muezzin or of the volume), not the one for a prayer */
    private boolean adhanPreview;
    /** the TV's own volume, put back when the adhan ends (-1: nothing to put back) */
    private int savedVolume = -1;
    /** the level the adhan set the volume to, so a volume the person changed meanwhile is left alone (-1: not known) */
    private int adhanLevel = -1;
    /** where the adhan on is cut, in ms from its start, and how long the fade at the end is; 0: not cut («أذان مختصر» or «تكبير فقط» is on and it is not a preview) */
    private long adhanCutMs, adhanFadeMs;
    private int pausedSource = Prefs.SOURCE_NONE, pausedReciter = -1, pausedSurah = 1, pausedStation = -1, pausedPosition;
    private final Runnable previewStop = new Runnable() {
        @Override public void run() { if (source == Prefs.SOURCE_ADHAN) finishAdhan(); }
    };
    /** «أذان مختصر» and «تكبير فقط»: the last seconds before the cut are a fade, then it ends like any adhan */
    private final Runnable shortTick = new Runnable() {
        @Override public void run() {
            if (source != Prefs.SOURCE_ADHAN || adhanCutMs <= 0) return;
            long elapsed = SystemClock.elapsedRealtime() - shortStart;
            if (elapsed >= adhanCutMs) { finishAdhan(); return; }
            float gain = AdhanVoices.gain(elapsed, adhanCutMs, adhanFadeMs);
            if (player != null) { try { player.setVolume(gain, gain); } catch (IllegalStateException ignored) { } }
            handler.postDelayed(this, 250);
        }
    };
    /** nothing plays to a dark screen: the adhan stops when the TV goes to standby */
    private final BroadcastReceiver screenOff = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (source == Prefs.SOURCE_ADHAN) finishAdhan();
        }
    };
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!ticking) return;
            if (player != null && prepared) {
                try {
                    position = player.getCurrentPosition();
                    int d = player.getDuration();
                    if (d > 0) duration = d;
                    updateSession();
                } catch (IllegalStateException ignored) {}
                // 4.4: and every 30 s while it plays (two small writes a minute; before, only a pause or a stop saved it)
                if (playing && source != Prefs.SOURCE_ADHAN && SystemClock.elapsedRealtime() - lastSaved >= SAVE_EVERY_MS) save();
            }
            if (watchStall()) return;   // a stalled station was restarted (its own tick starts again with it)
            handler.postDelayed(this, 1000);
        }
    };
    /** paused for this long (a pause, or another app took the sound), the service stops: nothing is holding the TV awake for it */
    private static final long PAUSED_STOP_MS = 30 * 60_000L;
    private final Runnable pausedStop = new Runnable() {
        @Override public void run() { if (!playing && !buffering) stopPlayback(); }
    };

    private final Runnable sleepStart = new Runnable() {
        @Override public void run() {
            if (source == Prefs.SOURCE_ADHAN) { handler.postDelayed(this, 5_000); return; }   // 4.4: never cut an adhan: the recitation it brings back fades after it
            fadeStep = 0;
            fadeOut();
        }
    };
    /** the sleep timer's fade is one Runnable, so cancelling the timer can stop it half way */
    private final Runnable fade = new Runnable() {
        @Override public void run() { fadeOut(); }
    };

    private void startTick() {
        if (ticking) return;
        ticking = true;
        handler.post(tick);
    }

    private void stopTick() {
        ticking = false;
        handler.removeCallbacks(tick);
    }

    /** Playing began (or came back): the tick and the Wi-Fi lock on, no pause timer. */
    private void playbackStarted() {
        handler.removeCallbacks(pausedStop);
        endFocusWatch();
        if (!foregroundStarted) foreground();   // Android refused it at the start (a command from the background): asked again while it plays
        acquireWifi();
        startTick();
    }

    /** Playing stopped for now (a pause, another app's sound, an error): the tick and the lock off, the pause timer on. */
    private void playbackPaused() {
        stopTick();
        releaseWifi();
        handler.removeCallbacks(pausedStop);
        handler.postDelayed(pausedStop, PAUSED_STOP_MS);
    }

    // ---- state for the screens and optimize/music.sh ----
    /** for UpdateCarry: is the adhan sounding or loading, and is anything of ours playing (a pause key must not stop it) */
    public static boolean carryAdhanActive() { return source == Prefs.SOURCE_ADHAN && (playing || buffering); }
    public static boolean carryOurPlaying() { return playing || buffering; }

    static int currentSource() { return source; }
    /** 4.3.3 G1: the service holds its foreground notice (Android keeps it alive while the home is behind the screensaver or the TV's settings) */
    static boolean inForeground() { return inForeground; }
    /** 4.3.3 G1: paused because another sound took the TV's audio; it goes on by itself when that sound ends (FocusRule) */
    static boolean waitingFocus() { return waitingFocus; }
    static boolean isPlaying() { return playing; }
    static boolean isBuffering() { return buffering; }
    static int reciterIndex() { return reciter; }
    static int currentSurah() { return surah; }
    static int currentStation() { return station; }
    static int currentPosition() { return position; }
    static int currentDuration() { return duration; }
    static String currentError() { return error; }
    static long sleepAt() { return sleepUntil; }
    static String adhanName() { return adhanName; }
    static boolean adhanLive() { return adhanLive && source == Prefs.SOURCE_ADHAN; }
    static int adhanPrayer() { return adhanPrayer; }
    /** 4.4: the dua or reminder playing now, null when none (an adhan, a surah, nothing) */
    static String duaName() { return adhanLive() ? duaNow : null; }
    static int rebufferCount() { return rebuffers; }
    /** "exo" or "mediaplayer": what plays now (4.3 A2) */
    static String playerKind() { return playerKind; }
    /** quiet restarts of a stalled station since the app started (4.3 A2) */
    static int stallRestartCount() { return stallRestarts; }
    /** 4.4: joins made because the network came back, since the app started */
    static int reconnectCount() { return reconnects; }
    static int errorCount() { return errors; }
    static long rebufferMillis() { return rebufferMs; }
    static String lastErrorCode() { return lastError; }
    static String radioFaultRecord() { return radioFault; }
    static long lastErrorAt() { return lastErrorAt; }

    // ---- starting it from an activity or the receiver ----
    static void start(Context c, Intent intent) {
        if (Build.VERSION.SDK_INT < 26) { c.startService(intent); return; }
        try {
            c.startForegroundService(intent);
        } catch (IllegalStateException e) {
            // targetSdk 31+ on Android 12+: a foreground start from the background is refused unless Android allows it (an exact alarm such as the
            // adhan's, our accessibility service bound, one of our screens in front). Nothing crashes: a service already playing keeps the
            // process active, so the plain start still reaches it (a pause, a stop); one that cannot start at all is logged and skipped.
            Log.w(TAG, "foreground start refused (" + e.getClass().getSimpleName() + "): " + intent.getAction());
            try { c.startService(intent); } catch (IllegalStateException | SecurityException again) { Log.w(TAG, "service start refused too: " + intent.getAction()); }
        }
    }

    private static Intent intent(Context c, String action) {
        return new Intent(c, MusicService.class).setAction(action);
    }

    static void playSurah(Context c, String reciterId, int surah) {
        start(c, intent(c, ACTION_PLAY_SURAH).putExtra(EXTRA_RECITER, reciterId).putExtra(EXTRA_SURAH, surah));
    }

    static void playRadio(Context c, int index) {
        start(c, intent(c, ACTION_PLAY_RADIO).putExtra(EXTRA_STATION, index));
    }

    /** Plays the adhan now; `previewSeconds` > 0 stops it again after that long (the voice picker). */
    static void adhan(Context c, int voice, String name, int previewSeconds) { adhan(c, voice, name, previewSeconds, -1); }

    /** `prayer`: PrayerTimes' number of the prayer this adhan is for (the bars name it), -1 when none */
    static void adhan(Context c, int voice, String name, int previewSeconds, int prayer) {
        start(c, intent(c, ACTION_PLAY_ADHAN)
                .putExtra("prayer", prayer)
                .putExtra(EXTRA_VOICE, voice)
                .putExtra(EXTRA_SECONDS, previewSeconds)
                .putExtra("name", name));
    }

    /** 4.4: a dua or reminder: its file's path or its surah's address, its name, its own volume (percent) */
    static void dua(Context c, String source, String name, int volume) { dua(c, source, name, volume, false); }

    /** fade: «يبدأ بهدوء» (4.4 touch) */
    static void dua(Context c, String source, String name, int volume, boolean fade) {
        start(c, intent(c, ACTION_PLAY_DUA).putExtra("source", source).putExtra("name", name).putExtra("volume", volume).putExtra("fade", fade));
    }

    static void resumeLast(Context c) { start(c, intent(c, ACTION_RESUME)); }
    static void pause(Context c) { start(c, intent(c, ACTION_PAUSE)); }
    static void resume(Context c) { start(c, intent(c, ACTION_RESUME)); }
    static void toggle(Context c) { start(c, intent(c, ACTION_TOGGLE)); }
    static void nextAction(Context c) { start(c, intent(c, ACTION_NEXT)); }
    static void prevAction(Context c) { start(c, intent(c, ACTION_PREV)); }
    static void stopAction(Context c) { start(c, intent(c, ACTION_STOP)); }
    static void stopAdhan(Context c) { start(c, intent(c, ACTION_STOP_ADHAN)); }
    /** From a screen of ours in front (its onResume): a surah or a station that plays without the foreground notice gets it now (FocusRule.promote). */
    public static void keepForeground(Context c) {
        if (FocusRule.promote(playing || buffering || waitingFocus, inForeground)) start(c, intent(c, ACTION_KEEP));
    }
    /** For the TV's sleep timer (outside this package): stops whatever is playing; does nothing when nothing is. */
    public static void stopIfPlaying(Context c) { if (playing || buffering) stopAction(c); }
    static void sleep(Context c, int minutes) { start(c, intent(c, ACTION_SLEEP).putExtra(EXTRA_MINUTES, minutes)); }
    static void seekBy(Context c, int delta) { start(c, intent(c, ACTION_SEEK_BY).putExtra(EXTRA_DELTA, delta)); }

    @Override public void onCreate() {
        super.onCreate();
        sa.urtv.home.S.init(this);   // «اللغة»: the notification's and the card's words
        cat = Catalog.get(this);
        prefs = new Prefs(this);
        audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        // an adhan that never finished here (the player was stopped mid-adhan): the TV's own volume comes back now
        savedVolume = prefs.savedTvVolume();
        adhanVolumeBack();
        createChannel();
        session = new MediaSession(this, "urtv-tilawah");
        session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { resume(); }
            @Override public void onPause() { pause(); }
            @Override public void onSkipToNext() { next(); }
            @Override public void onSkipToPrevious() { prev(); }
            @Override public void onStop() { stopPlayback(); }
            @Override public void onSeekTo(long pos) { seekTo((int) pos); }
        });
        session.setActive(true);
        registerReceiver(screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        watchNetwork();   // 4.4: join again the moment the network is back
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (action == null) { stopSelf(); return START_NOT_STICKY; }

        // every entry point may come from startForegroundService(): be foreground within 5 s
        foreground();

        switch (action) {
            case ACTION_PLAY_SURAH: {   // 4.4: a surah heard half way goes on from its place (SurahPlaces); the next surah after an end starts at 0
                String rid = intent.getStringExtra(EXTRA_RECITER);
                int n = intent.getIntExtra(EXTRA_SURAH, 1);
                playSurahById(rid, n, SurahPlaces.get(prefs.surahPlaces(), rid, n));
                break;
            }
            case ACTION_PLAY_RADIO:
                playRadio(intent.getIntExtra(EXTRA_STATION, 0));
                break;
            case ACTION_PLAY_ADHAN:
                playAdhan(intent.getIntExtra(EXTRA_VOICE, prefs.voice()),
                        intent.getStringExtra("name"), intent.getIntExtra(EXTRA_SECONDS, 0), intent.getIntExtra("prayer", -1));
                break;
            case ACTION_PLAY_DUA:
                pendingFade = intent.getBooleanExtra("fade", false);
                playDua(intent.getStringExtra("source"), intent.getStringExtra("name"), intent.getIntExtra("volume", 30));
                break;
            case ACTION_PAUSE: pause(); break;
            case ACTION_RESUME: resume(); break;
            case ACTION_TOGGLE: toggle(); break;
            case ACTION_NEXT: next(); break;
            case ACTION_PREV: prev(); break;
            case ACTION_SEEK_BY: seekBy(intent.getIntExtra(EXTRA_DELTA, 0)); break;
            case ACTION_SLEEP: setSleep(intent.getIntExtra(EXTRA_MINUTES, 0)); break;
            case ACTION_STOP: stopPlayback(); break;
            case ACTION_STOP_ADHAN: if (source == Prefs.SOURCE_ADHAN) finishAdhan(); break;   // what it paused comes back, as when it ends by itself
            case ACTION_KEEP: break;   // foreground() above did it
            default: break;
        }
        // a command that started nothing (a pause with nothing loaded, a timer of 0, an unknown reciter) must not leave the service
        // foreground for good; a command that plays (radio, adhan) is left to its own course
        boolean mayLeaveNothing = !ACTION_PLAY_RADIO.equals(action) && !ACTION_PLAY_ADHAN.equals(action) && !ACTION_PLAY_DUA.equals(action) && !ACTION_STOP.equals(action);
        if (IdleCommand.leavesNothing(mayLeaveNothing, source == Prefs.SOURCE_NONE, player != null, sleepUntil)) stopIdle();
        return START_NOT_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onDestroy() {
        if (playing) save();
        stopTick();
        handler.removeCallbacksAndMessages(null);
        try { unregisterReceiver(screenOff); } catch (IllegalArgumentException ignored) {}
        if (netBack != null) {
            try { ((android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).unregisterNetworkCallback(netBack); } catch (RuntimeException ignored) { }
            netBack = null;
        }
        adhanVolumeBack();
        releasePlayer();
        abandonFocus();
        if (session != null) { session.release(); session = null; }
        inForeground = false;
        waitingFocus = false;
        source = Prefs.SOURCE_NONE;   // 4.4: the home's card reads these: a service Android ended is not «يُتلى الآن» any more
        playing = false;
        buffering = false;
        sleepUntil = 0;
        announce();
        super.onDestroy();
    }

    // ---- playing ----

    private void playSurahById(String id, int n, int startAt) {
        int index = id == null ? -1 : cat.reciterIndex(id);
        if (index < 0) {
            error = Ui.str(this, R.string.no_connection);
            updateSession();
            updateNotification();
            return;
        }
        playSurah(index, n, startAt);
    }

    private void playSurah(int index, int n, int startAt) {
        if (index < 0 || index >= cat.reciters.size()) return;
        leaveAdhan();
        Catalog.Reciter r = cat.reciters.get(index);
        int asked = n;
        n = SurahSet.atOrAfter(n, r.missing);   // 4.3.3: a surah the reciter has no recording of is stepped over (the next one it has)
        if (n != asked) startAt = 0;   // (a remembered place belongs to the surah it was in)
        source = Prefs.SOURCE_SURAH;
        reciter = index;
        surah = n;
        station = -1;
        retries = 0;
        pendingSeek = Math.max(0, startAt);
        surahRecover.reset();   // a pick (or the network back): nothing held against it
        wish.ask();
        prefs.save(source, r.id, surah, station, pendingSeek);
        startUrl(r.server + String.format(Locale.US, "%03d", n) + ".mp3");
    }

    private void playRadio(int index) {
        if (index < 0 || index >= cat.stations.size()) return;
        leaveAdhan();
        source = Prefs.SOURCE_RADIO;
        station = index;
        reciter = -1;
        surah = 1;
        retries = 0;
        pendingSeek = 0;
        wish.ask();
        prefs.save(source, "", surah, station, 0);
        Catalog.Station st = cat.stations.get(index);   // 4.4: every source of the station, in turn (StationSources)
        radioSources = StationSources.of(st.url, st.alt, st.mirrors, StationStatus.mirrors(sa.urtv.home.StationFeed.status(this), st.key, System.currentTimeMillis()));
        radioSource = 0;
        radioAddress = radioSources.isEmpty() ? st.url : radioSources.get(0);
        radioAlt = radioSources.size() > 1 ? radioSources.get(1) : null;
        resetRadioState();
        stall.reset();   // the person's pick: no quiet restart of an earlier one is held against it
        startRadio(radioAddress, 0);
    }

    /**
     * A station: its address is made ready first (StreamUrl: the redirects followed by us, so radiojar's https -> http one works; a 5xx tried
     * again after 2 s, then 4 s), then the player gets the final address. «تعذّر» is said only after the last try (tester step 19).
     */
    private void startRadio(final String url, final int tries) {
        releasePlayer();
        if (tries == 0) radioDirectTried = false;
        final int gen = wish.newPlayer();
        currentUrl = url;
        error = tries > 0 ? Ui.str(this, R.string.retrying) : reconnecting ? RECONNECTING : null;   // 4.4: a quiet restart says it, never silence
        reconnecting = false;
        buffering = true;
        playing = false;
        prepared = false;
        position = 0;
        duration = 0;
        acquireWifi();   // 4.4: the Wi-Fi stays awake while it joins again too, not only while it plays
        foreground();
        updateSession();
        updateNotification();
        new Thread(() -> {
            String ready = null;
            boolean passing = false;
            try {
                ready = StreamUrl.resolve(url, 10_000);
            } catch (java.io.IOException | RuntimeException e) {
                passing = e.getMessage() != null && e.getMessage().startsWith("server error");
                Log.w(TAG, "station address: " + e.getMessage());
            }
            final String go = ready;
            final boolean busy = passing;
            handler.post(() -> {
                if (!wish.current(gen) || source != Prefs.SOURCE_RADIO || !url.equals(currentUrl)) return;   // stopped, or another pick meanwhile
                if (go != null && radioSlowLeft > 0) {   // radioslow: this try stalls and fails (the player is never given the address)
                    radioSlowLeft--;
                    handler.postDelayed(() -> { if (wish.current(gen) && source == Prefs.SOURCE_RADIO && url.equals(currentUrl)) onPlaybackError(MediaPlayer.MEDIA_ERROR_UNKNOWN, PlayerFault.TIMED_OUT); }, 4000);
                    return;
                }
                if (go != null) { startUrl(go); return; }
                if (busy && StreamUrl.again(tries + 1)) {
                    error = Ui.str(this, R.string.retrying);
                    updateSession();
                    updateNotification();
                    handler.postDelayed(() -> {
                        if (wish.current(gen) && source == Prefs.SOURCE_RADIO && url.equals(currentUrl)) startRadio(url, tries + 1);
                    }, StreamUrl.backoffMs(tries + 1));
                    return;
                }
                if (busy) {   // the server failed every try: this source is done (RadioPolicy: the second source, else said)
                    if (!radioState.played) radioState.tries = StreamUrl.TRIES - 1;
                    onPlaybackError(0, PlayerFault.IO);
                    lastError = "radio server_error (" + StreamUrl.TRIES + " tries)";
                    return;
                }
                startUrl(url);   // the address could not be checked (no answer to our request): the player tries it itself
            });
        }, "station-url").start();
    }

    // ---- the adhan: pause whatever is on, play it, then bring the surah back ----

    /** 4.4: a dua plays through playAdhan with its own sound, name and volume; never over an adhan for a prayer (DuaReceiver asks first, too) */
    private void playDua(String sourceUrl, String name, int volume) {
        if (sourceUrl == null || (source == Prefs.SOURCE_ADHAN && adhanLive && duaNow == null)) return;
        pendingDua = sourceUrl;
        duaVolume = Math.max(10, Math.min(100, volume));
        playAdhan(prefs.voice(), name, 0, -1);
    }

    private void playAdhan(int wanted, String name, int previewSeconds, int prayer) {
        final String duaUrl = pendingDua;   // 4.4: set by playDua for this one call only
        pendingDua = null;
        handler.removeCallbacks(previewStop);
        handler.removeCallbacks(shortTick);
        // the owner's choice (step 28b): a muted TV is unmuted for the adhan and muted again when it ends (adhanVolumeBack, every way the adhan can end).
        // The mute is read FIRST, before anything unmutes, and said in the log before the unmute shows up in the system's own
        final boolean mutedBefore = tvMuted();
        boolean wasAdhan = source == Prefs.SOURCE_ADHAN;
        if (wasAdhan && playing) {
            if (adhanPreview && previewSeconds > 0 && wanted == adhanVoice) {   // the volume row again: the new level, and the same few seconds again (4.4: the same muezzin only)
                volumePercent = prefs.volumeFor(prayer);   // 4.4: the prayer's own level, or the general one
                adhanVolumeUp();
                handler.postDelayed(previewStop, previewSeconds * 1000L);
                return;
            }
            // a second adhan on top of the first is ignored (two alarms); a preview gives way to the real one; 4.4: a dua gives way to an adhan,
            // a second dua takes the place of the first, and a preview of another muezzin takes the place of the first preview (it kept playing)
            if (duaNow == null && !adhanPreview) return;
        }
        final boolean gone = OwnAdhan.isOwn(wanted) && !OwnAdhan.existsOf(this, wanted);   // the recording was removed: a numbered voice
        final int voice = gone ? prefs.voice() : wanted;
        int length = previewSeconds == 0 ? AdhanVoices.length(prefs.adhanTakbir(), prefs.adhanShort()) : AdhanVoices.FULL;
        adhanCutMs = duaUrl != null ? 0 : AdhanVoices.cutMs(length, voice);   // a dua plays whole
        adhanFadeMs = duaUrl != null ? 0 : AdhanVoices.fadeMs(length);
        if (gone && duaUrl == null) name = null;
        volumePercent = duaUrl != null ? duaVolume : prefs.volumeFor(prayer);   // 4.4 «مستوى صوت الأذان»: the prayer's own, or the general (TV: left as it is)
        Log.i("UrTVAdhan", "adhan start: prayer=" + prayer + (previewSeconds > 0 ? " preview=" + previewSeconds + "s" : " live") + " tv_muted_before=" + mutedBefore);   // before the unmute: the first read
        adhanVolumeUp();
        boolean comeBack = prepareAdhanPause();   // false when an adhan was already on
        if (!wasAdhan) resumeAfterAdhan = comeBack;   // the first adhan's answer stays: what it paused still waits
        adhanPreview = previewSeconds > 0;
        source = Prefs.SOURCE_ADHAN;
        adhanVoice = voice;
        adhanFromCache = false;
        adhanName = name != null ? name : Adhan.name(voice);
        adhanLive = previewSeconds == 0;
        adhanPrayer = previewSeconds == 0 ? prayer : -1;
        duaNow = duaUrl != null ? (name != null ? name : "") : null;
        duaFade = duaUrl != null && pendingFade;
        pendingFade = false;
        handler.removeCallbacks(fadeIn);
        retries = 0;
        pendingSeek = 0;
        wish.ask();
        if (duaUrl != null) {   // the dua's own file (or its surah's address): no muezzin, no copy to fetch, no «last adhan»
            adhanFromCache = true;
            Log.i("UrTVDua", "dua start");
            startUrl(duaUrl);
            updateSession();
            updateNotification();
            return;
        }
        prefs.setLastAdhan(System.currentTimeMillis());   // for `music.sh adhan`; the duplicate guard is the alarm's own (AdhanReceiver)
        File cached = OwnAdhan.isOwn(voice) ? OwnAdhan.fileOf(this, voice) : Adhan.cached(this, voice);
        // the person's own files are always on the TV: nothing to fetch, nothing to retry from the network
        int stand;
        if (OwnAdhan.isOwn(voice) || Adhan.isCached(this, voice)) {
            adhanFromCache = true;
            startUrl(cached.getAbsolutePath());
        } else if (!online() && (stand = standIn(voice)) > 0) {
            // beta 3: not on the TV and no network: another voice that is saved plays now, not a stream that cannot come
            Log.i("UrTVAdhan", "adhan: voice " + voice + " not saved and offline: voice " + stand + " instead");
            playStandIn(stand);
        } else {
            startUrl(Adhan.url(voice));
            // keep a copy for next time; it also rescues this one if the stream drops
            new Thread(() -> Adhan.download(this, voice), "adhan-fetch").start();
        }
        if (previewSeconds > 0) handler.postDelayed(previewStop, previewSeconds * 1000L);
    }

    /**
     * beta 3: a saved voice to play instead of {@code voice}, or -1. The saved list is the one the keep check wrote (Prefs.adhanSaved); the one file
     * picked is looked at again before it is used, as the start above looks at the chosen one.
     */
    private int standIn(int voice) {
        int other = voice == prefs.fajrVoice() ? prefs.voice() : prefs.fajrVoice();
        int v = AdhanKeep.stand(voice, other, AdhanKeep.parseSaved(prefs.adhanSaved(), Adhan.VOICES), adhanPrayer() == PrayerTimes.FAJR);   // F92: the Fajr recording never stands in at another prayer
        return v > 0 && Adhan.isCached(this, v) ? v : -1;
    }

    private void playStandIn(int stand) {
        adhanFromCache = true;
        adhanName = Adhan.name(stand);
        startUrl(Adhan.cached(this, stand).getAbsolutePath());
    }

    /** Pauses the surah (or radio) and remembers where; true when it should come back after the adhan. */
    private boolean prepareAdhanPause() {
        if (source == Prefs.SOURCE_ADHAN || source == Prefs.SOURCE_NONE) return false;
        pausedSource = source;
        pausedReciter = reciter;
        pausedSurah = surah;
        pausedStation = station;
        pausedPosition = position;
        boolean wasPlaying = FocusRule.onBeforeAdhan(playing, buffering && wish.wants(), resumeOnFocus && wish.wants());   // loading, or waiting for another sound to end, counts
        resumeOnFocus = false;
        waitingFocus = false;
        endFocusWatch();
        if (player != null) {
            try { player.pause(); } catch (IllegalStateException ignored) {}
        }
        playing = false;
        if (wasPlaying) playbackPaused();   // the adhan that follows starts the tick and the lock again
        return wasPlaying;
    }

    /** A surah or a station picked while the adhan plays: the adhan ends here, and the TV's volume comes back. */
    private void leaveAdhan() {
        if (source != Prefs.SOURCE_ADHAN) return;
        handler.removeCallbacks(previewStop);
        handler.removeCallbacks(shortTick);
        adhanCutMs = 0;
        adhanVolumeBack();
        resumeAfterAdhan = false;
        adhanName = null; adhanLive = false; adhanPrayer = -1; duaNow = null;
    }

    /** The adhan is over: put the surah back where it was, or stop. */
    private boolean tvMuted() {
        try { return ((android.media.AudioManager) getSystemService(AUDIO_SERVICE)).isStreamMute(android.media.AudioManager.STREAM_MUSIC); } catch (RuntimeException e) { return false; }
    }

    private void finishAdhan() {
        Log.i("UrTVAdhan", "adhan stop: tv_muted_now=" + tvMuted());
        handler.removeCallbacks(previewStop);
        handler.removeCallbacks(shortTick);
        adhanCutMs = 0;
        adhanVolumeBack();
        boolean resume = resumeAfterAdhan;
        resumeAfterAdhan = false;
        adhanName = null; adhanLive = false; adhanPrayer = -1; duaNow = null;
        if (resume && pausedSource == Prefs.SOURCE_SURAH) {
            playSurah(pausedReciter, pausedSurah, pausedPosition);
        } else if (resume && pausedSource == Prefs.SOURCE_RADIO) {
            playRadio(pausedStation);
        } else {
            stopPlayback();
        }
    }

    /**
     * The adhan has its own volume (30% by default): the TV's volume is remembered and put back
     * when the adhan ends, so a quiet TV is loud for the adhan alone and quiet again after.
     */
    /** the level the adhan (or the dua) plays at, in percent of the TV's maximum: set by playAdhan before adhanVolumeUp */
    private int volumePercent = 30;

    private void adhanVolumeUp() {
        if (audio == null) return;
        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (savedVolume < 0) {   // the TV's own, put back at the end (also after a restart of the player: on disk)
            // a muted TV: Android says 0 for a muted stream, so it is unmuted first (the adhan would unmute it anyway) to read the real level,
            // and muted again at the end; before, the end put the level back to 0 and the TV stayed silent at 0 even after an unmute
            boolean muted = audio.isStreamMute(AudioManager.STREAM_MUSIC);
            if (muted) {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
                sa.urtv.home.MuteKeep.ours(this, "adhan_unmute");
                Log.i("UrTVAdhan", "adhan volume: the TV was muted, unmuted for the adhan (it is muted again at its end)");
            }
            savedVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            prefs.setSavedTvMuted(muted);
            prefs.setSavedTvVolume(savedVolume);
        }
        int want = AdhanLevel.index(volumePercent, max);   // the adhan's own level, or a dua's (4.4); -1 «مثل صوت التلفزيون»: left as it is
        if (want >= 0) {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, want, 0);
            sa.urtv.home.MuteKeep.ours(this, "adhan_volume_set");   // a volume set can end a mute by itself on some TVs: the state after it is in the log
        }
        adhanLevel = audio.getStreamVolume(AudioManager.STREAM_MUSIC);   // what it took, which can differ from what was asked
        prefs.setAdhanLevel(adhanLevel);
    }

    /** The one way back for the TV's volume: any end of an adhan (over, replaced, failed, stopped, the service gone) comes here. */
    private void adhanVolumeBack() {
        if (savedVolume < 0) return;
        if (audio != null) {
            int level = adhanLevel >= 0 ? adhanLevel : prefs.adhanLevel();
            int to = AdhanVolume.restore(savedVolume, level, audio.getStreamVolume(AudioManager.STREAM_MUSIC));
            if (to >= 0) { audio.setStreamVolume(AudioManager.STREAM_MUSIC, to, 0); sa.urtv.home.MuteKeep.ours(this, "adhan_volume_back"); }   // else the person turned it themselves: it stays
            boolean wasMuted = prefs.savedTvMuted();
            if (AdhanVolume.muteAgain(wasMuted, to)) {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
                sa.urtv.home.MuteKeep.ours(this, "adhan_remute");
                Log.i("UrTVAdhan", "adhan end: re-mute " + (audio.isStreamMute(AudioManager.STREAM_MUSIC) ? "done" : "FAILED (still unmuted)") + ", volume back to " + to);
            } else if (wasMuted) {
                Log.i("UrTVAdhan", "adhan end: re-mute not done: the volume was changed during the adhan (the person chose to hear it)");
            }
        }
        prefs.setSavedTvMuted(false);
        savedVolume = -1;
        adhanLevel = -1;
        prefs.setSavedTvVolume(-1);
        prefs.setAdhanLevel(-1);
    }

    private void startUrl(String url) {
        releasePlayer();
        final int gen = wish.newPlayer();
        currentUrl = url;
        error = null;
        buffering = true;
        playing = false;
        prepared = false;
        position = 0;
        duration = 0;
        foreground();

        player = Playback.create(this, url, source == Prefs.SOURCE_RADIO, source == Prefs.SOURCE_SURAH);   // 4.4: a surah through ExoPlayer too (its file buffer)
        playerKind = player.kind();
        stall.newPlayer();
        player.setAudioAttributes(MEDIA_ATTRS);   // minSdk 26; USAGE_MEDIA is STREAM_MUSIC, the stream the adhan's own volume (adhanVolumeUp) sets
        player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        player.setVolume(1f, 1f);
        louder = lift(player.getAudioSessionId(), Loudness.targetMb(source));
        player.setOnPreparedListener(mp -> {
            if (!wish.current(gen)) return;   // an older player's: this one has been replaced or stopped
            prepared = true;
            buffering = false;
            retries = 0;
            error = null;
            if (pendingSeek > 0) {
                try { mp.seekTo(pendingSeek); } catch (IllegalStateException ignored) {}
                position = pendingSeek;
                pendingSeek = 0;
            }
            if (!wish.wants()) {   // paused while it was loading: it waits, ready, for an explicit resume
                playing = false;
                playbackPaused();
                save();
            } else if (requestFocus()) {
                boolean rise = source == Prefs.SOURCE_ADHAN && duaNow != null && duaFade;   // 4.4 «يبدأ بهدوء»: from silence
                if (rise) { try { mp.setVolume(0f, 0f); } catch (IllegalStateException ignored) { } fadeInStart = SystemClock.elapsedRealtime(); handler.post(fadeIn); }
                mp.start();
                playing = true;
                resumeOnFocus = false;
                waitingFocus = false;
                playbackStarted();
                if (source == Prefs.SOURCE_RADIO) RadioPolicy.started(radioState);   // a later failure is a reconnect, said only after RadioPolicy's tries
                if (source == Prefs.SOURCE_ADHAN && adhanCutMs > 0) {
                    shortStart = SystemClock.elapsedRealtime();
                    handler.removeCallbacks(shortTick);
                    handler.postDelayed(shortTick, adhanCutMs - adhanFadeMs);
                }
            } else if (source == Prefs.SOURCE_ADHAN) {
                finishAdhan();   // nobody gave the sound: the TV's volume comes back and what was paused goes on
                return;
            } else {
                error = sa.urtv.home.S.t("الصوت مشغول");
            }
            updateSession();
            updateNotification();
        });
        player.setOnInfoListener((mp, what, extra) -> {
            if (!wish.current(gen)) return true;
            if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START && prepared && playing) {   // it ran dry while playing: a rebuffer
                rebuffers++;
                if (source == Prefs.SOURCE_RADIO) week(RadioWeek.REBUFFER);
                rebufferStart = SystemClock.elapsedRealtime();
                buffering = true;
                updateSession();
                updateNotification();
            } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END && buffering && prepared) {
                if (rebufferStart > 0) rebufferMs += SystemClock.elapsedRealtime() - rebufferStart;
                rebufferStart = 0;
                buffering = false;
                updateSession();
                updateNotification();
            }
            return true;
        });
        player.setOnCompletionListener(mp -> {
            if (!wish.current(gen)) return;
            if (source == Prefs.SOURCE_SURAH) { forgetPlace(); next(); }   // 4.4: an ended surah has no place left
            else if (source == Prefs.SOURCE_ADHAN) finishAdhan();
            else if (source == Prefs.SOURCE_RADIO) onPlaybackError(MediaPlayer.MEDIA_ERROR_UNKNOWN, PlayerFault.IO);   // a live stream has no end: the server closed it, joined again
            else { playing = false; updateSession(); updateNotification(); }
        });
        player.setOnErrorListener((mp, what, extra) -> {
            if (wish.current(gen)) onPlaybackError(what, extra);
            return true;
        });
        try {
            player.setDataSource(url);
            player.prepareAsync();
            if (source == Prefs.SOURCE_RADIO) {   // a station that gives no sound and no error (step 28: station 0 stayed «جارٍ التحميل…» for minutes): said after 20 s
                handler.postDelayed(() -> {
                    if (!wish.current(gen) || prepared || source != Prefs.SOURCE_RADIO || !url.equals(currentUrl)) return;
                    if (!radioDirectTried && radioAddress != null && !url.equals(radioAddress)) {   // the address we resolved gave nothing: once, the station's own address straight to the player (its redirect handled by the platform)
                        radioDirectTried = true;
                        Log.i(TAG, "station gave nothing from the resolved address in " + RADIO_WAIT_MS / 1000 + " s: " + hostOf(url) + "; trying " + hostOf(radioAddress) + " as given");
                        startUrl(radioAddress);
                        return;
                    }
                    Log.w(TAG, "station gave nothing in " + RADIO_WAIT_MS / 1000 + " s: " + hostOf(url));
                    releasePlayer();
                    onPlaybackError(MediaPlayer.MEDIA_ERROR_UNKNOWN, PlayerFault.TIMED_OUT);   // RadioPolicy: tried again, then the second source, then said
                    lastError = "radio no_answer (" + RADIO_WAIT_MS / 1000 + " s, host " + hostOf(url) + ")";   // the host only: the address carries a token
                }, RADIO_WAIT_MS);
            }
        } catch (Exception e) {
            Log.w(TAG, "cannot play " + url, e);
            onPlaybackError(-1, 0);
        }
        updateSession();
        updateNotification();
    }

    /** the resolved address of a station got no sound: its own address was given to the player once (reset by every pick) */
    private boolean radioDirectTried;


    /**
     * 4.3 A2: a live station that ran dry for 12 s, or stands still for 15 s while it says it plays, is started again quietly from its own address
     * (the person hears it come back, no error is shown); a fourth stall in two minutes is a failure, said as before. True when it acted.
     */
    /** 4.4: the line the screens show while a station joins again (a quiet restart, the network back) */
    static final String RECONNECTING = sa.urtv.home.S.n("يعيد الاتصال…");
    /** the next startRadio is a reconnect: it says RECONNECTING */
    private boolean reconnecting;
    /** 4.4: when the last reconnect for a network that came back was made (NetReconnect.MIN_GAP_MS) */
    private long lastNetReconnect;
    private android.net.ConnectivityManager.NetworkCallback netBack;

    /** 4.4: the network came back (or another one took over): a station that went quiet joins again at once, a surah goes on from its place */
    private void onNetworkBack() {
        long now = SystemClock.elapsedRealtime();
        NetReconnect.Act act = NetReconnect.onNetworkBack(source, wish.wants(), playing, error != null || buffering,
                lastNetReconnect == 0 ? Long.MAX_VALUE : now - lastNetReconnect);
        if (act == NetReconnect.Act.NOTHING) return;
        lastNetReconnect = now;
        reconnects++;
        week(RadioWeek.RECONNECT);
        Log.i(TAG, "network back: " + act + " joins again");
        if (act == NetReconnect.Act.RADIO && radioAddress != null) {
            stall.reset();
            reconnecting = true;
            startRadio(radioAddress, 0);
        } else if (act == NetReconnect.Act.SURAH && reciter >= 0) {
            playSurah(reciter, surah, Math.max(position, pendingSeek));
        }
    }

    private void watchNetwork() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return;
            netBack = new android.net.ConnectivityManager.NetworkCallback() {
                private boolean lost;
                @Override public void onLost(android.net.Network n) { lost = true; }
                @Override public void onCapabilitiesChanged(android.net.Network n, android.net.NetworkCapabilities c) {
                    if (!lost || !c.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return;   // only a network that came BACK, and works
                    lost = false;
                    handler.post(MusicService.this::onNetworkBack);
                }
            };
            cm.registerDefaultNetworkCallback(netBack);
        } catch (RuntimeException e) {   // a TV that refuses it: the timed tries of RadioPolicy stay, as before
            netBack = null;
        }
    }

    private boolean watchStall() {
        boolean live = source == Prefs.SOURCE_RADIO && radioAddress != null && wish.wants();
        StallWatch.Verdict v = stall.check(SystemClock.elapsedRealtime(), live, prepared, playing, rebufferStart, position);
        if (v == StallWatch.Verdict.OK && stall.idleTooLong(SystemClock.elapsedRealtime(), live, prepared, playing, buffering, waitingFocus || resumeOnFocus)) {
            Log.w(TAG, "station ready but not playing for " + StallWatch.IDLE_MS / 1000 + " s (" + playerKind + ", host " + hostOf(currentUrl) + ", error " + error + "): started again");
            v = StallWatch.Verdict.RESTART;   // 4.4 beta 4: ready, wanted, silent and not loading: the same quiet restart as a stall
        }
        if (v == StallWatch.Verdict.OK) return false;
        if (v == StallWatch.Verdict.RESTART) {
            stallRestarts++;
            week(RadioWeek.STALL);
            Log.w(TAG, "station stalled (" + (rebufferStart > 0 ? "dry " + (SystemClock.elapsedRealtime() - rebufferStart) / 1000 + " s" : "position still") + ", " + playerKind + ", host " + hostOf(currentUrl) + "): started again quietly");
            if (rebufferStart > 0) rebufferMs += SystemClock.elapsedRealtime() - rebufferStart;
            rebufferStart = 0;
            ticking = false;
            reconnecting = true;   // 4.4: «يعيد الاتصال…» on the screens while it joins again
            if (radioSources.size() > 1) {   // 4.4 beta 4: the source that ran dry is left for the next one (the «alt»), round and round
                radioSource = StationSources.afterStall(radioSource, radioSources.size());
                radioAddress = radioSources.get(radioSource);
                radioAlt = radioSources.get(StationSources.next(radioSource, radioSources.size()));
                Log.i(TAG, "stall: source " + (radioSource + 1) + " of " + radioSources.size() + ", " + hostOf(radioAddress));
            }
            startRadio(radioAddress, 0);
            return true;
        }
        Log.w(TAG, "station stalled again (" + StallWatch.MAX_RESTARTS + " quiet restarts in " + StallWatch.WINDOW_MS / 60_000 + " min): said as a failure");
        stall.reset();
        onPlaybackError(MediaPlayer.MEDIA_ERROR_UNKNOWN, -110);   // -110: timed out (PlayerFault)
        return false;
    }

    /** a station that has not started in this long is said to have failed */
    static final long RADIO_WAIT_MS = 20_000;

    private static String hostOf(String url) {
        try { return new java.net.URL(url).getHost(); } catch (java.io.IOException e) { return "?"; }
    }

    /** a new pick of a station: nothing held against it */
    /** 4.3.1 a tester's switch (dock.sh debug radioslow): the first RADIO_SLOW_FAILS tries of each pick stall for 4 s and fail, so the recovery can be watched */
    private static final int RADIO_SLOW_FAILS = 4;
    private int radioSlowLeft;

    private boolean radioSlowOn() {
        try { return getSharedPreferences("debug_flags", MODE_PRIVATE).getBoolean("radioslow", false); } catch (RuntimeException e) { return false; }
    }

    private void resetRadioState() {
        radioState.played = false;
        radioState.tries = 0;
        radioState.onAlt = false;
        radioState.reconnects = 0;
        radioState.formatEveryTime = true;
        radioState.spent = false;
        radioState.failures = 0;
        radioState.patient = 0;
        radioSlowLeft = radioSlowOn() ? RADIO_SLOW_FAILS : 0;
    }

    /** the TV has a network that reaches the internet now (a permission the app already holds); true when it cannot tell */
    private boolean online() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.Network n = cm == null ? null : cm.getActiveNetwork();
            android.net.NetworkCapabilities c = n == null ? null : cm.getNetworkCapabilities(n);
            return cm == null || (c != null && c.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED));
        } catch (RuntimeException e) {
            return true;
        }
    }

    /** 4.4: one more of `kind` in the stations' last seven days (RadioWeek), kept across restarts */
    private void week(int kind) {
        prefs.radioWeek(RadioWeek.add(prefs.radioWeek(), weekDay(this), kind));
    }

    /** today in days since 1970 in the TV's place (ZoneFix, as every clock of the app), for RadioWeek */
    static long weekDay(android.content.Context c) {
        long now = System.currentTimeMillis();
        return Math.floorDiv(now + sa.urtv.home.ZoneFix.offset(Place.get(c).tz, now), 86_400_000L);
    }

    private void onPlaybackError(int what, int extra) {
        errors++;
        if (source == Prefs.SOURCE_RADIO) week(RadioWeek.ERROR);
        lastError = (source == Prefs.SOURCE_RADIO ? "radio" : source == Prefs.SOURCE_ADHAN ? "adhan" : "surah")
                + " " + PlayerFault.code(what, extra) + " (what=" + what + ", extra=" + extra + ")";
        lastErrorAt = System.currentTimeMillis() / 1000;
        rebufferStart = 0;
        playing = false;
        buffering = false;
        prepared = false;
        playbackPaused();
        if (source == Prefs.SOURCE_SURAH && position > 0) pendingSeek = position;
        final String failed = currentUrl;
        if (source == Prefs.SOURCE_RADIO && radioAddress != null) {   // 4.3 (a user report on 4.2): retried, joined again, the second source, and only then said
            RadioPolicy.Step step = RadioPolicy.after(radioState, what, extra, radioAlt != null, SystemClock.elapsedRealtime());
            final String faultCode = PlayerFault.code(what, extra);
            final int httpCode = player == null ? 0 : player.httpCode;
            radioFault = RadioFault.record("radio-" + station, faultCode, httpCode, Build.VERSION.SDK_INT, player == null ? "" : player.decoder);
            if (step == RadioPolicy.Step.FAIL && !RadioPolicy.finalFault(radioState, what)) step = RadioPolicy.patient(radioState, radioAlt != null);   // 4.3.1 never given up while the person wants it
            if (step == RadioPolicy.Step.FAIL) {
                final int made = radioState.failures;   // every failure since the pick, both sources
                error = what == PlayerFault.SERVER_DIED ? RadioPolicy.message(radioState, what, extra)
                    : RadioFault.message(RadioFault.reason(online(), faultCode, httpCode, radioState.played, radioState.formatEveryTime, made), made);
                updateSession();
                updateNotification();
                save();
                return;
            }
            if (step == RadioPolicy.Step.ALT) {
                radioSource = StationSources.next(radioSource, radioSources.size());   // 4.4: the next source, round and round
                Log.i(TAG, "station failed every try at " + hostOf(radioAddress) + ": source " + (radioSource + 1) + " of " + radioSources.size() + ", " + hostOf(radioSources.get(radioSource)));
                radioAddress = radioSources.get(radioSource);
                radioAlt = radioSources.get(StationSources.next(radioSource, radioSources.size()));
            }
            error = RadioFault.retryMessage(RadioFault.reason(online(), faultCode, httpCode, radioState.played, radioState.formatEveryTime, radioState.failures));
            updateSession();
            updateNotification();
            final int gen = wish.currentGeneration();
            final String address = radioAddress;
            handler.postDelayed(() -> {
                if (!wish.retry(gen, playing) || source != Prefs.SOURCE_RADIO || !address.equals(radioAddress)) return;   // not after a stop or another pick
                startRadio(address, 0);
            }, RadioPolicy.waitMs(radioState));
            return;
        }
        if (source == Prefs.SOURCE_ADHAN && retries >= 2) {
            // the stream failed but the background download may have finished: play the saved copy
            File cached = Adhan.cached(this, adhanVoice);
            if (!adhanFromCache && Adhan.isCached(this, adhanVoice)) {
                adhanFromCache = true;
                retries = 0;
                startUrl(cached.getAbsolutePath());
                return;
            }
            int stand = !adhanFromCache && duaNow == null ? standIn(adhanVoice) : -1;
            if (stand > 0) {   // beta 3: another voice that is saved, rather than no adhan
                Log.i("UrTVAdhan", "adhan: voice " + adhanVoice + " failed to stream: voice " + stand + " instead");
                retries = 0;
                playStandIn(stand);
                return;
            }
            if (adhanLive) {   // beta 3: never a silent no-op: «الأذان» says it for a day
                Log.w("UrTVAdhan", "adhan: no recording could play (not saved, stream failed)");
                prefs.setAdhanMissed(System.currentTimeMillis());
            }
            finishAdhan();
            return;
        }
        if (source == Prefs.SOURCE_SURAH && failed != null) {   // 4.4: from its place after a growing wait, never a loop of prepares
            long wait = surahRecover.after(SystemClock.elapsedRealtime());
            if (wait >= 0) {
                Log.i(TAG, "surah failed (" + PlayerFault.code(what, extra) + ", " + playerKind + ", host " + hostOf(failed) + "): on from " + pendingSeek / 1000 + " s in " + wait / 1000 + " s");
                error = RECONNECTING;
                updateSession();
                updateNotification();
                save();
                final int gen = wish.currentGeneration();
                handler.postDelayed(() -> {
                    if (!wish.retry(gen, playing) || source != Prefs.SOURCE_SURAH || !failed.equals(currentUrl)) return;   // not after a stop, a pause or another pick
                    startUrl(failed);   // pendingSeek: the place it stopped at
                    error = RECONNECTING;   // until it plays again (onPrepared clears it)
                    updateSession();
                    updateNotification();
                }, wait);
                return;
            }
            Log.w(TAG, "surah failed " + SurahRecover.MAX_FAILS + " times in " + SurahRecover.WINDOW_MS / 60_000 + " min: said");
            error = PlayerFault.message(false, what, extra);
            updateSession();
            updateNotification();
            save();
            return;
        }
        if (failed != null && retries < 2) {
            retries++;
            error = Ui.str(this, R.string.retrying);
            updateSession();
            updateNotification();
            final int gen = wish.currentGeneration();
            final boolean radio = source == Prefs.SOURCE_RADIO && radioAddress != null;
            final String station = radioAddress;
            handler.postDelayed(() -> {
                if (!wish.retry(gen, playing) || !failed.equals(currentUrl)) return;   // not after a stop, a pause-and-leave or another pick
                if (radio) startRadio(station, 0); else startUrl(failed);   // a station's address is resolved again (its redirect may not last)
            }, radio ? StreamUrl.backoffMs(retries) : 3000);
        } else {
            error = PlayerFault.message(false, what, extra);   // a surah or the adhan: a station took RadioPolicy's way above
            updateSession();
            updateNotification();
            save();
        }
    }

    private void playSurahAgain(int index, int n) { playSurah(index, n, 0); }

    void next() {
        if (source == Prefs.SOURCE_ADHAN) return;
        if (source == Prefs.SOURCE_SURAH && reciter >= 0) {
            playSurahAgain(reciter, SurahSet.next(surah, cat.reciters.get(reciter).missing));   // after 114 from 1; a missing surah stepped over
        } else if (source == Prefs.SOURCE_RADIO && station >= 0) {
            playRadio(StationStatus.step(station, 1, hiddenStations()));   // 4.4: a station the check found dead is stepped over
        } else {
            resumeLastInternal();
        }
    }

    /** 4.4: the stations the check found dead (StationStatus), never the one in use */
    private boolean[] hiddenStations() {
        java.util.List<String> keys = new java.util.ArrayList<>();
        for (Catalog.Station s : cat.stations) keys.add(s.key);
        return StationStatus.hidden(sa.urtv.home.StationFeed.status(this), keys, station, System.currentTimeMillis());
    }

    void prev() {
        if (source == Prefs.SOURCE_ADHAN) return;
        if (source == Prefs.SOURCE_SURAH && reciter >= 0) {
            if (position > 5000) { seekTo(0); return; }
            playSurahAgain(reciter, SurahSet.prev(surah, cat.reciters.get(reciter).missing));
        } else if (source == Prefs.SOURCE_RADIO && station >= 0) {
            playRadio(StationStatus.step(station, -1, hiddenStations()));
        }
    }

    void seekTo(int ms) {
        if (source != Prefs.SOURCE_SURAH || player == null || !prepared) return;
        if (ms < 0) ms = 0;
        if (duration > 0 && ms > duration) ms = duration;
        try { player.seekTo(ms); } catch (IllegalStateException ignored) {}
        position = ms;
        updateSession();
    }

    void seekBy(int delta) {
        if (source == Prefs.SOURCE_ADHAN) return;
        // the screen paces a held key (10 s per 250 ms); SeekBurst drops a burst that gets past it (a script, a stuck key)
        int now = seekBurst.offer(delta, SystemClock.elapsedRealtime());
        if (now != 0) applySeek(now);
    }

    private final SeekBurst seekBurst = new SeekBurst();

    private void resetSeekBurst() { seekBurst.reset(); }

    private void applySeek(int delta) {
        if (source == Prefs.SOURCE_SURAH) seekTo(SeekBurst.target(position, delta, duration));   // a surah only; a station is live, a step there is next/prev
        else if (delta > 0) next();
        else if (delta < 0) prev();
    }

    void toggle() {
        if (fadeStep > 0) { setSleep(0); return; }   // 4.4: OK (or play) in the sleep timer's fade is «I am awake»: the timer ends, the sound back to full, it plays on
        if (player == null) { resumeLastInternal(); return; }
        if (playing || (buffering && wish.wants())) pause(); else resume();
    }

    void pause() {
        if (player != null && !playing && wish.wants()) {   // still loading, or held by another app's sound: remember the wish; the media stays ready
            wish.pause();
            resumeOnFocus = false;
            waitingFocus = false;
            endFocusWatch();
            updateSession();
            updateNotification();
            return;
        }
        if (player != null && playing) {
            wish.pause();
            player.pause();
            playing = false;
            resumeOnFocus = false;
            waitingFocus = false;
            abandonFocus();
            playbackPaused();
            save();
            updateSession();
            updateNotification();
        }
    }

    void resume() {
        if (player == null) { resumeLastInternal(); return; }
        if (!prepared) {   // still loading: it plays when it arrives
            wish.ask();
            updateSession();
            updateNotification();
            return;
        }
        if (requestFocus()) {
            wish.ask();
            player.start();
            playing = true;
            error = null;
            resumeOnFocus = false;   // the person's play: no other sound is waited for any more
            waitingFocus = false;
            playbackStarted();
            updateSession();
            updateNotification();
        }
    }

    private void resumeLastInternal() {
        int last = prefs.source();
        if (last == Prefs.SOURCE_SURAH) {
            playSurahById(prefs.reciter(), prefs.surah(), prefs.position());
        } else if (last == Prefs.SOURCE_RADIO) {
            playRadio(prefs.station());
        } else if (!cat.reciters.isEmpty()) {
            playSurah(0, 1, 0);
        }
    }

    /** Nothing was started: leave without save(), which would wipe the remembered surah and position. */
    private void stopIdle() {
        foregroundStarted = false;
        inForeground = false;
        stopForeground(true);
        stopSelf();
    }

    void stopPlayback() {
        resetSeekBurst();
        if (source != Prefs.SOURCE_ADHAN) save();
        adhanVolumeBack();
        resumeAfterAdhan = false;
        wish.stop();
        adhanCutMs = 0;
        handler.removeCallbacks(previewStop);
        handler.removeCallbacks(shortTick);
        releasePlayer();
        playing = false;
        buffering = false;
        prepared = false;
        position = 0;
        duration = 0;
        error = null;
        source = Prefs.SOURCE_NONE;
        adhanName = null; adhanLive = false; adhanPrayer = -1; duaNow = null;
        handler.removeCallbacks(sleepStart);
        handler.removeCallbacks(fade);
        fadeStep = 0;
        handler.removeCallbacks(pausedStop);
        resumeOnFocus = false;
        waitingFocus = false;
        endFocusWatch();
        stopTick();
        sleepUntil = 0;
        announce();
        foregroundStarted = false;
        inForeground = false;
        stopForeground(true);
        if (session != null) {
            session.setPlaybackState(new PlaybackState.Builder()
                    .setState(PlaybackState.STATE_NONE, 0, 0f).build());
        }
        stopSelf();
    }

    private void releasePlayer() {
        resetSeekBurst();
        if (louder != null) {
            try { louder.release(); } catch (RuntimeException ignored) {}
            louder = null;
        }
        if (player != null) {
            try { player.reset(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
        releaseWifi();
    }

    /**
     * The gain inside our player only (Loudness says why and how much): the TV's volume, its mute and the adhan's own level are not
     * touched. A TV without the effect, or one that refuses it, plays as before (null).
     */
    private static LoudnessEnhancer lift(int session, int mb) {
        if (mb <= 0) return null;
        LoudnessEnhancer e = null;
        try {
            e = new LoudnessEnhancer(session);
            e.setTargetGain(mb);
            e.setEnabled(true);
            return e;
        } catch (RuntimeException | UnsatisfiedLinkError err) {
            if (e != null) { try { e.release(); } catch (RuntimeException ignored) {} }
            return null;
        }
    }

    private void save() {
        if (source == Prefs.SOURCE_ADHAN) return;   // «متابعة» must still point at the surah, not the adhan
        String id = reciter >= 0 && reciter < cat.reciters.size() ? cat.reciters.get(reciter).id : "";
        prefs.save(source, id, surah, station, position);
        if (source == Prefs.SOURCE_SURAH && !id.isEmpty() && prepared) prefs.setSurahPlaces(SurahPlaces.put(prefs.surahPlaces(), id, surah, position, duration));   // 4.4
        lastSaved = SystemClock.elapsedRealtime();
    }

    /** 4.4: the place is saved every SAVE_EVERY_MS while it plays too (a power cut or a killed app kept only the last pause) */
    private static final long SAVE_EVERY_MS = 30_000;
    private long lastSaved;

    private void forgetPlace() {
        String id = reciter >= 0 && reciter < cat.reciters.size() ? cat.reciters.get(reciter).id : "";
        if (!id.isEmpty()) prefs.setSurahPlaces(SurahPlaces.forget(prefs.surahPlaces(), id, surah));
    }

    // ---- sleep timer: a 15-second gentle fade, then stop ----

    void setSleep(int minutes) {
        handler.removeCallbacks(sleepStart);
        if (fadeStep > 0) {   // cancelled (or set again) in the middle of the fade: it stops, and the sound is back to full
            handler.removeCallbacks(fade);
            fadeStep = 0;
            if (player != null) { try { player.setVolume(1f, 1f); } catch (IllegalStateException ignored) { } }
        }
        if (minutes <= 0) {
            sleepUntil = 0;
            updateSession();
            updateNotification();
            return;
        }
        sleepUntil = System.currentTimeMillis() + minutes * 60_000L;
        handler.postDelayed(sleepStart, minutes * 60_000L);
        updateSession();
        updateNotification();
    }

    private void fadeOut() {
        if (player == null || !playing) { sleepUntil = 0; stopPlayback(); return; }
        fadeStep++;
        float volume = Math.max(0f, 1f - fadeStep / 30f);
        try { player.setVolume(volume, volume); } catch (IllegalStateException ignored) {}
        if (fadeStep < 30) handler.postDelayed(fade, 500);
        else { sleepUntil = 0; stopPlayback(); }
    }

    // ---- audio focus ----

    /** surah, radio and adhan all play as media (the adhan's volume is the music stream's, see adhanVolumeUp); minSdk is 26, so no API guard */
    private static final AudioAttributes MEDIA_ATTRS = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();
    private AudioFocusRequest focusRequest;

    private boolean requestFocus() {
        if (audio == null) return false;
        if (focusRequest == null) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(MEDIA_ATTRS)
                    .setOnAudioFocusChangeListener(this, handler)   // the same listener, now on the main thread's handler as before
                    .build();
        }
        return audio.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonFocus() {
        if (audio != null && focusRequest != null) audio.abandonAudioFocusRequest(focusRequest);
    }

    @Override public void onAudioFocusChange(int change) {
        boolean ours = (player != null && playing) || resumeOnFocus;
        switch (FocusRule.on(change, ours)) {
            case PAUSE_UNTIL_GAIN:   // a call, the voice search: it comes back with the focus
            case PAUSE_AND_WATCH:    // another sound for good: watched for a while, taken back when it ends (FocusRule)
                if (player != null && playing) {
                    player.pause();
                    playing = false;
                    resumeOnFocus = true;
                    playbackPaused();
                    save();
                    updateSession();
                    updateNotification();
                }
                waitingFocus = resumeOnFocus;
                if (change == FocusRule.LOSS && resumeOnFocus) startFocusWatch();
                Log.i(TAG, "focus lost (" + change + "): paused" + (change == FocusRule.LOSS ? ", watching for " + FocusRule.TAKE_BACK_MS / 1000 + " s" : " until it comes back"));
                break;
            case DUCK:
                if (player != null) { try { player.setVolume(FocusRule.DUCK, FocusRule.DUCK); } catch (IllegalStateException ignored) { } }
                break;
            case GAIN:
                if (player != null && fadeStep == 0) { try { player.setVolume(1f, 1f); } catch (IllegalStateException ignored) { } }
                endFocusWatch();
                if (resumeOnFocus && player != null && prepared && !playing && wish.wants()) {
                    player.start();
                    playing = true;
                    resumeOnFocus = false;
                    waitingFocus = false;
                    playbackStarted();
                    updateSession();
                    updateNotification();
                }
                waitingFocus = resumeOnFocus;
                break;
            default:
                break;
        }
    }

    private void startFocusWatch() {
        handler.removeCallbacks(focusLook);
        focusLostAt = SystemClock.elapsedRealtime();
        quietLooks = 0;
        handler.postDelayed(focusLook, FocusRule.CHECK_MS);
    }

    private void endFocusWatch() {
        handler.removeCallbacks(focusLook);
        quietLooks = 0;
        focusLostAt = 0;
    }

    /** One look after a loss for good: nothing else plays (twice in a row, no call), so the sound is taken back and it goes on (FocusRule.look). */
    private void lookForFocus() {
        if (focusLostAt == 0) return;
        boolean others = audio != null && audio.isMusicActive();
        int mode = audio == null ? FocusRule.MODE_NORMAL : audio.getMode();
        FocusRule.Look look = FocusRule.look(SystemClock.elapsedRealtime() - focusLostAt, others, mode, quietLooks,
                resumeOnFocus && wish.wants() && player != null && prepared && !playing);
        quietLooks = FocusRule.quietAfter(others, mode, quietLooks);
        if (look == FocusRule.Look.AGAIN) { handler.postDelayed(focusLook, FocusRule.CHECK_MS); return; }
        endFocusWatch();
        if (look == FocusRule.Look.GIVE_UP) { Log.i(TAG, "focus: another player kept the sound; paused until the person plays it"); return; }
        if (!requestFocus()) { Log.i(TAG, "focus: asked back, refused"); return; }
        Log.i(TAG, "focus: the other sound ended, taken back");
        try { player.setVolume(1f, 1f); player.start(); } catch (IllegalStateException e) { return; }
        playing = true;
        resumeOnFocus = false;
        waitingFocus = false;
        waitingFocus = false;
        playbackStarted();
        updateSession();
        updateNotification();
    }

    // ---- MediaSession + notification ----

    /** The home screen's «يُتلى الآن» card hears about it when what it shows may have changed: once per change, never on the clock's tick. */
    private String announced = "";
    private void announce() {
        String now = source + "|" + playing + "|" + buffering + "|" + reciter + "|" + surah + "|" + station + "|" + error + "|" + (sleepUntil / 60_000);   // 4.4: a new sleep time is said at once (the card)
        if (now.equals(announced)) return;
        announced = now;
        sendBroadcast(new Intent(NowPlaying.CHANGED).setPackage(getPackageName()));
    }

    private void updateSession() {
        announce();
        if (session == null) return;
        String title = sa.urtv.home.S.t("تلاوة"), artist = "";
        if (source == Prefs.SOURCE_SURAH && reciter >= 0 && reciter < cat.reciters.size()) {
            title = sa.urtv.home.S.t("سورة %s", cat.surahName(surah));
            artist = cat.reciters.get(reciter).name;
        } else if (source == Prefs.SOURCE_RADIO && station >= 0 && station < cat.stations.size()) {
            title = cat.stations.get(station).name;
            artist = Ui.str(this, R.string.live);
        } else if (source == Prefs.SOURCE_ADHAN && duaNow != null) {   // 4.4
            title = duaNow;
            artist = "";
        } else if (source == Prefs.SOURCE_ADHAN) {
            title = Ui.str(this, R.string.adhan);
            artist = adhanName != null ? adhanName : "";
        }
        MediaMetadata.Builder meta = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM,
                        source == Prefs.SOURCE_ADHAN ? Ui.str(this, R.string.adhan) : sa.urtv.home.S.t("القرآن الكريم"));
        if (source == Prefs.SOURCE_SURAH && duration > 0) {
            meta.putLong(MediaMetadata.METADATA_KEY_DURATION, duration);
        }
        session.setMetadata(meta.build());

        int state = error != null ? PlaybackState.STATE_ERROR
                : buffering ? PlaybackState.STATE_BUFFERING
                : playing ? PlaybackState.STATE_PLAYING
                : prepared ? PlaybackState.STATE_PAUSED
                : PlaybackState.STATE_NONE;
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE
                | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_STOP;
        if (source != Prefs.SOURCE_ADHAN) {
            actions |= PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        }
        if (source == Prefs.SOURCE_SURAH) actions |= PlaybackState.ACTION_SEEK_TO;
        session.setPlaybackState(new PlaybackState.Builder()
                .setActions(actions)
                .setState(state, source == Prefs.SOURCE_RADIO ? 0 : position, playing ? 1f : 0f)
                .build());
        session.setActive(true);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL, Ui.str(this, R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW);
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
    }

    private static int pendingFlags() {
        return PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
    }

    private PendingIntent command(String action) {
        return PendingIntent.getService(this, action.hashCode(),
                intent(this, action), pendingFlags());
    }

    private Notification buildNotification() {
        String title = sa.urtv.home.S.t("تلاوة"), text = "";
        if (source == Prefs.SOURCE_SURAH && reciter >= 0 && reciter < cat.reciters.size()) {
            title = sa.urtv.home.S.t("سورة %s", cat.surahName(surah));
            text = cat.reciters.get(reciter).name;
        } else if (source == Prefs.SOURCE_RADIO && station >= 0 && station < cat.stations.size()) {
            title = cat.stations.get(station).name;
            text = Ui.str(this, R.string.live);
        } else if (source == Prefs.SOURCE_ADHAN) {
            title = Ui.str(this, R.string.adhan);
            text = adhanName != null ? adhanName : "";
        }
        if (error != null) text = error;
        else if (buffering) text = Ui.str(this, R.string.buffering);

        Intent open = new Intent(this, PlayerActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = PendingIntent.getActivity(this, 0, open, pendingFlags());

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_note)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(content)
                .setOngoing(playing)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(getResources().getColor(R.color.notify_accent))   // a plain colour: a notification has no theme
                .addAction(R.drawable.ic_prev, Ui.str(this, R.string.previous), command(ACTION_PREV))
                .addAction(playing ? R.drawable.ic_pause : R.drawable.ic_play,
                        Ui.str(this, R.string.play_pause), command(ACTION_TOGGLE))
                .addAction(R.drawable.ic_next, Ui.str(this, R.string.next), command(ACTION_NEXT))
                .addAction(R.drawable.ic_stop, Ui.str(this, R.string.stop), command(ACTION_STOP));
        if (Build.VERSION.SDK_INT >= 21) {
            builder.setStyle(new Notification.MediaStyle()
                    .setMediaSession(session.getSessionToken())
                    .setShowActionsInCompactView(0, 1, 2));
        }
        return builder.build();
    }

    private void foreground() {
        Notification notification = buildNotification();
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIF, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(NOTIF, notification);
            }
        } catch (IllegalStateException e) {   // started plainly (see start) while Android refuses a foreground start: it plays on without one
            Log.w(TAG, "startForeground refused: " + e.getClass().getSimpleName());
            return;
        }
        foregroundStarted = true;
        inForeground = true;
    }

    private void updateNotification() {
        if (!foregroundStarted) return;
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIF, buildNotification());
    }

    // ---- Wi-Fi stays awake while streaming ----

    private void acquireWifi() {
        try {
            if (wifi == null) {
                WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                if (manager != null) {
                    wifi = manager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "urtv:listening");
                    wifi.setReferenceCounted(false);
                }
            }
            if (wifi != null && !wifi.isHeld()) wifi.acquire();
        } catch (Exception ignored) {}
    }

    private void releaseWifi() {
        try { if (wifi != null && wifi.isHeld()) wifi.release(); } catch (Exception ignored) {}
    }
}
