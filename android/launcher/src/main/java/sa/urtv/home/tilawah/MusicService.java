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
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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

    private Catalog cat;
    private Prefs prefs;
    private MediaPlayer player;
    private MediaSession session;
    private AudioManager audio;
    private WifiManager.WifiLock wifi;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean foregroundStarted;
    private boolean resumeOnFocus;
    private int retries;
    private int pendingSeek;
    private int fadeStep;
    /** the 1-second position tick runs only while something plays */
    private boolean ticking;
    private String currentUrl;

    // what was on when the adhan started, so it comes back afterwards
    private boolean resumeAfterAdhan;
    private int adhanVoice = 1;
    private boolean adhanFromCache;
    /** the adhan on is a preview (a few seconds of a muezzin or of the volume), not the one for a prayer */
    private boolean adhanPreview;
    /** the TV's own volume, put back when the adhan ends (-1: nothing to put back) */
    private int savedVolume = -1;
    private int pausedSource = Prefs.SOURCE_NONE, pausedReciter = -1, pausedSurah = 1, pausedStation = -1, pausedPosition;
    private final Runnable previewStop = new Runnable() {
        @Override public void run() { if (source == Prefs.SOURCE_ADHAN) finishAdhan(); }
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
                    updateSession();   // (the place is saved when it stops or pauses, not every few seconds: the disk is flash)
                } catch (IllegalStateException ignored) {}
            }
            handler.postDelayed(this, 1000);
        }
    };
    /** paused for this long (a pause, or another app took the sound), the service stops: nothing is holding the TV awake for it */
    private static final long PAUSED_STOP_MS = 30 * 60_000L;
    private final Runnable pausedStop = new Runnable() {
        @Override public void run() { if (!playing && !buffering) stopPlayback(); }
    };

    private final Runnable sleepStart = new Runnable() {
        @Override public void run() { fadeStep = 0; fadeOut(); }
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
    static int currentSource() { return source; }
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

    // ---- starting it from an activity or the receiver ----
    static void start(Context c, Intent intent) {
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(intent); else c.startService(intent);
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
    static void adhan(Context c, int voice, String name, int previewSeconds) {
        start(c, intent(c, ACTION_PLAY_ADHAN)
                .putExtra(EXTRA_VOICE, voice)
                .putExtra(EXTRA_SECONDS, previewSeconds)
                .putExtra("name", name));
    }

    static void resumeLast(Context c) { start(c, intent(c, ACTION_RESUME)); }
    static void pause(Context c) { start(c, intent(c, ACTION_PAUSE)); }
    static void resume(Context c) { start(c, intent(c, ACTION_RESUME)); }
    static void toggle(Context c) { start(c, intent(c, ACTION_TOGGLE)); }
    static void nextAction(Context c) { start(c, intent(c, ACTION_NEXT)); }
    static void prevAction(Context c) { start(c, intent(c, ACTION_PREV)); }
    static void stopAction(Context c) { start(c, intent(c, ACTION_STOP)); }
    /** For the TV's sleep timer (outside this package): stops whatever is playing; does nothing when nothing is. */
    public static void stopIfPlaying(Context c) { if (playing || buffering) stopAction(c); }
    static void sleep(Context c, int minutes) { start(c, intent(c, ACTION_SLEEP).putExtra(EXTRA_MINUTES, minutes)); }
    static void seekBy(Context c, int delta) { start(c, intent(c, ACTION_SEEK_BY).putExtra(EXTRA_DELTA, delta)); }

    @Override public void onCreate() {
        super.onCreate();
        cat = Catalog.get(this);
        prefs = new Prefs(this);
        audio = (AudioManager) getSystemService(AUDIO_SERVICE);
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
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (action == null) { stopSelf(); return START_NOT_STICKY; }

        // every entry point may come from startForegroundService(): be foreground within 5 s
        foreground();

        switch (action) {
            case ACTION_PLAY_SURAH:
                playSurahById(intent.getStringExtra(EXTRA_RECITER), intent.getIntExtra(EXTRA_SURAH, 1), 0);
                break;
            case ACTION_PLAY_RADIO:
                playRadio(intent.getIntExtra(EXTRA_STATION, 0));
                break;
            case ACTION_PLAY_ADHAN:
                playAdhan(intent.getIntExtra(EXTRA_VOICE, prefs.voice()),
                        intent.getStringExtra("name"), intent.getIntExtra(EXTRA_SECONDS, 0));
                break;
            case ACTION_PAUSE: pause(); break;
            case ACTION_RESUME: resume(); break;
            case ACTION_TOGGLE: toggle(); break;
            case ACTION_NEXT: next(); break;
            case ACTION_PREV: prev(); break;
            case ACTION_SEEK_BY: seekBy(intent.getIntExtra(EXTRA_DELTA, 0)); break;
            case ACTION_SLEEP: setSleep(intent.getIntExtra(EXTRA_MINUTES, 0)); break;
            case ACTION_STOP: stopPlayback(); break;
            default: break;
        }
        return START_NOT_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onDestroy() {
        if (playing) save();
        stopTick();
        handler.removeCallbacksAndMessages(null);
        try { unregisterReceiver(screenOff); } catch (IllegalArgumentException ignored) {}
        adhanVolumeBack();
        releasePlayer();
        abandonFocus();
        if (session != null) { session.release(); session = null; }
        super.onDestroy();
    }

    // ---- playing ----

    private void playSurahById(String id, int n, int startAt) {
        int index = id == null ? -1 : cat.reciterIndex(id);
        if (index < 0) {
            error = getString(R.string.no_connection);
            updateSession();
            updateNotification();
            return;
        }
        playSurah(index, n, startAt);
    }

    private void playSurah(int index, int n, int startAt) {
        if (index < 0 || index >= cat.reciters.size()) return;
        Catalog.Reciter r = cat.reciters.get(index);
        if (n < 1) n = 1;
        if (n > r.surahs) n = r.surahs;
        source = Prefs.SOURCE_SURAH;
        reciter = index;
        surah = n;
        station = -1;
        retries = 0;
        pendingSeek = Math.max(0, startAt);
        prefs.save(source, r.id, surah, station, pendingSeek);
        startUrl(r.server + String.format(Locale.US, "%03d", n) + ".mp3");
    }

    private void playRadio(int index) {
        if (index < 0 || index >= cat.stations.size()) return;
        source = Prefs.SOURCE_RADIO;
        station = index;
        reciter = -1;
        surah = 1;
        retries = 0;
        pendingSeek = 0;
        prefs.save(source, "", surah, station, 0);
        startUrl(cat.stations.get(index).url);
    }

    // ---- the adhan: pause whatever is on, play it, then bring the surah back ----

    private void playAdhan(int wanted, String name, int previewSeconds) {
        handler.removeCallbacks(previewStop);
        boolean wasAdhan = source == Prefs.SOURCE_ADHAN;
        if (wasAdhan && playing) {
            if (adhanPreview && previewSeconds > 0) {   // the volume row again: the new level, and the same few seconds again
                adhanVolumeUp();
                handler.postDelayed(previewStop, previewSeconds * 1000L);
                return;
            }
            // a second adhan on top of the first is ignored (two alarms); a preview gives way to the real one
            if (!adhanPreview || previewSeconds > 0) return;
        }
        final boolean gone = wanted == Prefs.OWN && !OwnAdhan.exists(this);   // the recording was removed: a numbered voice
        final int voice = gone ? prefs.voice() : wanted;
        if (gone) name = null;
        adhanVolumeUp();
        boolean comeBack = prepareAdhanPause();   // false when an adhan was already on
        if (!wasAdhan) resumeAfterAdhan = comeBack;   // the first adhan's answer stays: what it paused still waits
        adhanPreview = previewSeconds > 0;
        source = Prefs.SOURCE_ADHAN;
        adhanVoice = voice;
        adhanFromCache = false;
        adhanName = name != null ? name : Adhan.name(voice);
        retries = 0;
        pendingSeek = 0;
        prefs.setLastAdhan(System.currentTimeMillis());   // for `music.sh adhan`; the duplicate guard is the alarm's own (AdhanReceiver)
        File cached = voice == Prefs.OWN ? OwnAdhan.file(this) : Adhan.cached(this, voice);
        // the person's own file is always on the TV: nothing to fetch, nothing to retry from the network
        if (voice == Prefs.OWN || Adhan.isCached(this, voice)) {
            adhanFromCache = true;
            startUrl(cached.getAbsolutePath());
        } else {
            startUrl(Adhan.url(voice));
            // keep a copy for next time; it also rescues this one if the stream drops
            new Thread(() -> Adhan.download(this, voice), "adhan-fetch").start();
        }
        if (previewSeconds > 0) handler.postDelayed(previewStop, previewSeconds * 1000L);
    }

    /** Pauses the surah (or radio) and remembers where; true when it should come back after the adhan. */
    private boolean prepareAdhanPause() {
        if (source == Prefs.SOURCE_ADHAN || source == Prefs.SOURCE_NONE) return false;
        pausedSource = source;
        pausedReciter = reciter;
        pausedSurah = surah;
        pausedStation = station;
        pausedPosition = position;
        boolean wasPlaying = playing;
        if (player != null) {
            try { player.pause(); } catch (IllegalStateException ignored) {}
        }
        playing = false;
        if (wasPlaying) playbackPaused();   // the adhan that follows starts the tick and the lock again
        return wasPlaying;
    }

    /** The adhan is over: put the surah back where it was, or stop. */
    private void finishAdhan() {
        handler.removeCallbacks(previewStop);
        adhanVolumeBack();
        boolean resume = resumeAfterAdhan;
        resumeAfterAdhan = false;
        adhanName = null;
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
    private void adhanVolumeUp() {
        if (audio == null) return;
        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (savedVolume < 0) savedVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);   // the TV's own, put back at the end
        int want = Math.round(max * prefs.adhanVolume() / 100f);
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, Math.max(1, Math.min(max, want)), 0);
    }

    private void adhanVolumeBack() {
        if (savedVolume < 0) return;
        if (audio != null) audio.setStreamVolume(AudioManager.STREAM_MUSIC, savedVolume, 0);
        savedVolume = -1;
    }

    private void startUrl(String url) {
        releasePlayer();
        currentUrl = url;
        error = null;
        buffering = true;
        playing = false;
        prepared = false;
        position = 0;
        duration = 0;
        foreground();

        player = new MediaPlayer();
        if (Build.VERSION.SDK_INT >= 21) {
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());
        } else {
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }
        player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        player.setVolume(1f, 1f);
        player.setOnInfoListener((mp, what, extra) -> {
            if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                buffering = true;
                updateSession();
                updateNotification();
            } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                buffering = false;
                updateSession();
                updateNotification();
            }
            return false;
        });
        player.setOnPreparedListener(mp -> {
            prepared = true;
            buffering = false;
            retries = 0;
            error = null;
            if (requestFocus()) {
                if (pendingSeek > 0) {
                    try { mp.seekTo(pendingSeek); } catch (IllegalStateException ignored) {}
                    position = pendingSeek;
                    pendingSeek = 0;
                }
                mp.start();
                playing = true;
                playbackStarted();
            } else {
                error = "الصوت مشغول";
            }
            updateSession();
            updateNotification();
        });
        player.setOnCompletionListener(mp -> {
            if (source == Prefs.SOURCE_SURAH) next();
            else if (source == Prefs.SOURCE_ADHAN) finishAdhan();
            else { playing = false; updateSession(); updateNotification(); }
        });
        player.setOnErrorListener((mp, what, extra) -> { onPlaybackError(); return true; });
        try {
            player.setDataSource(url);
            player.prepareAsync();
        } catch (Exception e) {
            Log.w(TAG, "cannot play " + url, e);
            onPlaybackError();
        }
        updateSession();
        updateNotification();
    }

    private void onPlaybackError() {
        playing = false;
        buffering = false;
        prepared = false;
        playbackPaused();
        if (source == Prefs.SOURCE_SURAH && position > 0) pendingSeek = position;
        final String failed = currentUrl;
        if (source == Prefs.SOURCE_ADHAN && retries >= 2) {
            // the stream failed but the background download may have finished: play the saved copy
            File cached = Adhan.cached(this, adhanVoice);
            if (!adhanFromCache && Adhan.isCached(this, adhanVoice)) {
                adhanFromCache = true;
                retries = 0;
                startUrl(cached.getAbsolutePath());
                return;
            }
            finishAdhan();
            return;
        }
        if (failed != null && retries < 2) {
            retries++;
            error = getString(R.string.retrying);
            updateSession();
            updateNotification();
            handler.postDelayed(() -> {
                if (failed.equals(currentUrl) && !playing) startUrl(failed);
            }, 3000);
        } else {
            error = getString(R.string.no_connection);
            updateSession();
            updateNotification();
            save();
        }
    }

    private void playSurahAgain(int index, int n) { playSurah(index, n, 0); }

    void next() {
        if (source == Prefs.SOURCE_ADHAN) return;
        if (source == Prefs.SOURCE_SURAH && reciter >= 0) {
            int total = cat.reciters.get(reciter).surahs;
            playSurahAgain(reciter, surah >= total ? 1 : surah + 1);
        } else if (source == Prefs.SOURCE_RADIO && station >= 0) {
            playRadio((station + 1) % cat.stations.size());
        } else {
            resumeLastInternal();
        }
    }

    void prev() {
        if (source == Prefs.SOURCE_ADHAN) return;
        if (source == Prefs.SOURCE_SURAH && reciter >= 0) {
            if (position > 5000) { seekTo(0); return; }
            int total = cat.reciters.get(reciter).surahs;
            playSurahAgain(reciter, surah <= 1 ? total : surah - 1);
        } else if (source == Prefs.SOURCE_RADIO && station >= 0) {
            playRadio((station - 1 + cat.stations.size()) % cat.stations.size());
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
        if (source == Prefs.SOURCE_SURAH) seekTo(position + delta);
        else if (delta > 0) next();
        else if (delta < 0) prev();
    }

    void toggle() {
        if (player == null) { resumeLastInternal(); return; }
        if (playing) pause(); else resume();
    }

    void pause() {
        if (player != null && playing) {
            player.pause();
            playing = false;
            resumeOnFocus = false;
            abandonFocus();
            playbackPaused();
            save();
            updateSession();
            updateNotification();
        }
    }

    void resume() {
        if (player == null) { resumeLastInternal(); return; }
        if (!prepared) return;
        if (requestFocus()) {
            player.start();
            playing = true;
            error = null;
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

    void stopPlayback() {
        if (source != Prefs.SOURCE_ADHAN) save();
        adhanVolumeBack();
        resumeAfterAdhan = false;
        handler.removeCallbacks(previewStop);
        releasePlayer();
        playing = false;
        buffering = false;
        prepared = false;
        position = 0;
        duration = 0;
        error = null;
        source = Prefs.SOURCE_NONE;
        adhanName = null;
        handler.removeCallbacks(sleepStart);
        handler.removeCallbacks(fade);
        fadeStep = 0;
        handler.removeCallbacks(pausedStop);
        stopTick();
        sleepUntil = 0;
        announce();
        foregroundStarted = false;
        stopForeground(true);
        if (session != null) {
            session.setPlaybackState(new PlaybackState.Builder()
                    .setState(PlaybackState.STATE_NONE, 0, 0f).build());
        }
        stopSelf();
    }

    private void releasePlayer() {
        if (player != null) {
            try { player.reset(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
            player = null;
        }
        releaseWifi();
    }

    private void save() {
        if (source == Prefs.SOURCE_ADHAN) return;   // «متابعة» must still point at the surah, not the adhan
        String id = reciter >= 0 && reciter < cat.reciters.size() ? cat.reciters.get(reciter).id : "";
        prefs.save(source, id, surah, station, position);
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

    private AudioFocusRequest focusRequest;

    private boolean requestFocus() {
        if (audio == null) return false;
        if (Build.VERSION.SDK_INT >= 26) {
            if (focusRequest == null) {
                AudioAttributes attrs = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build();
                focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(attrs)
                        .setOnAudioFocusChangeListener(this)
                        .build();
            }
            return audio.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        return audio.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonFocus() {
        if (audio == null) return;
        if (Build.VERSION.SDK_INT >= 26 && focusRequest != null) {
            audio.abandonAudioFocusRequest(focusRequest);
        } else {
            audio.abandonAudioFocus(this);
        }
    }

    @Override public void onAudioFocusChange(int change) {
        if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            if (player != null && playing) {
                player.pause();
                playing = false;
                resumeOnFocus = true;
                playbackPaused();
                save();
                updateSession();
                updateNotification();
            }
        } else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            if (player != null) player.setVolume(0.2f, 0.2f);
        } else if (change == AudioManager.AUDIOFOCUS_GAIN) {
            if (player != null) player.setVolume(1f, 1f);
            if (resumeOnFocus && player != null && prepared && !playing) {
                player.start();
                playing = true;
                resumeOnFocus = false;
                playbackStarted();
                updateSession();
                updateNotification();
            }
        }
    }

    // ---- MediaSession + notification ----

    /** The home screen's «يُتلى الآن» card hears about it when what it shows may have changed: once per change, never on the clock's tick. */
    private String announced = "";
    private void announce() {
        String now = source + "|" + playing + "|" + buffering + "|" + reciter + "|" + surah + "|" + station + "|" + (error != null) + "|" + (sleepUntil > 0);
        if (now.equals(announced)) return;
        announced = now;
        sendBroadcast(new Intent(NowPlaying.CHANGED).setPackage(getPackageName()));
    }

    private void updateSession() {
        announce();
        if (session == null) return;
        String title = "تلاوة", artist = "";
        if (source == Prefs.SOURCE_SURAH && reciter >= 0 && reciter < cat.reciters.size()) {
            title = "سورة " + cat.surahName(surah);
            artist = cat.reciters.get(reciter).name;
        } else if (source == Prefs.SOURCE_RADIO && station >= 0 && station < cat.stations.size()) {
            title = cat.stations.get(station).name;
            artist = getString(R.string.live);
        } else if (source == Prefs.SOURCE_ADHAN) {
            title = getString(R.string.adhan);
            artist = adhanName != null ? adhanName : "";
        }
        MediaMetadata.Builder meta = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM,
                        source == Prefs.SOURCE_ADHAN ? getString(R.string.adhan) : "القرآن الكريم");
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
        NotificationChannel channel = new NotificationChannel(CHANNEL, getString(R.string.channel_name),
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
        String title = "تلاوة", text = "";
        if (source == Prefs.SOURCE_SURAH && reciter >= 0 && reciter < cat.reciters.size()) {
            title = "سورة " + cat.surahName(surah);
            text = cat.reciters.get(reciter).name;
        } else if (source == Prefs.SOURCE_RADIO && station >= 0 && station < cat.stations.size()) {
            title = cat.stations.get(station).name;
            text = getString(R.string.live);
        } else if (source == Prefs.SOURCE_ADHAN) {
            title = getString(R.string.adhan);
            text = adhanName != null ? adhanName : "";
        }
        if (error != null) text = error;
        else if (buffering) text = getString(R.string.buffering);

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
                .setColor(getResources().getColor(R.color.gold))
                .addAction(R.drawable.ic_prev, getString(R.string.previous), command(ACTION_PREV))
                .addAction(playing ? R.drawable.ic_pause : R.drawable.ic_play,
                        getString(R.string.play_pause), command(ACTION_TOGGLE))
                .addAction(R.drawable.ic_next, getString(R.string.next), command(ACTION_NEXT))
                .addAction(R.drawable.ic_stop, getString(R.string.stop), command(ACTION_STOP));
        if (Build.VERSION.SDK_INT >= 21) {
            builder.setStyle(new Notification.MediaStyle()
                    .setMediaSession(session.getSessionToken())
                    .setShowActionsInCompactView(0, 1, 2));
        }
        return builder.build();
    }

    private void foreground() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIF, notification);
        }
        foregroundStarted = true;
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
