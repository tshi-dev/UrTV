package sa.urtv.home.tilawah;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.PowerManager;

import java.io.IOException;

/**
 * 4.3 A2: the one player MusicService drives, with MediaPlayer's own calls and callbacks. A surah and the adhan play through the TV's MediaPlayer
 * as before; a live station (since 4.3 every one, A2 began with the .m3u8 ones) plays through Media3 ExoPlayer when the build has it (ExoPlayback, in src/exo/java): a longer
 * buffer behind the live edge, retries of a segment that failed, and no restart of the whole stream for one late segment. The Saudi Quran Radio is
 * SBA's HLS with a 12 s window of 4 s segments and a new connection per segment (.github/tools/hls_probe.py): MediaPlayer ran dry on it.
 */
abstract class Playback {
    interface OnPrepared { void onPrepared(Playback p); }
    interface OnInfo { boolean onInfo(Playback p, int what, int extra); }
    interface OnCompletion { void onCompletion(Playback p); }
    interface OnError { boolean onError(Playback p, int what, int extra); }

    /** 4.3.1 what the last error carried, for the report and the line shown: the server's HTTP status (0 = none) and the decoder's name ("" = none); ExoPlayback sets them */
    int httpCode;
    String decoder = "";

    static final String EXO = "sa.urtv.home.tilawah.ExoPlayback";

    /**
     * ExoPlayer for every live station when the build carries it (HLS as before, and since a user report on 4.2 the Icecast ones too: the TV's
     * MediaPlayer stopped for good when such a stream dropped, and said «صيغة البث غير مدعومة» for a passing error), else the TV's MediaPlayer.
     * A surah and the adhan stay on MediaPlayer.
     */
    static Playback create(Context c, String url, boolean radio) {
        if (radio) {
            try {
                return (Playback) Class.forName(EXO).getConstructor(Context.class).newInstance(c);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                android.util.Log.w("UrTVMusic", "ExoPlayer not available, MediaPlayer plays the station: " + e);
            }
        }
        return new Platform();
    }

    /** "exo" or "mediaplayer", for `optimize/music.sh status`. */
    abstract String kind();
    abstract void setAudioAttributes(AudioAttributes a);
    abstract void setWakeMode(Context c, int mode);
    abstract void setVolume(float left, float right);
    abstract int getAudioSessionId();
    abstract void setOnPreparedListener(OnPrepared l);
    abstract void setOnInfoListener(OnInfo l);
    abstract void setOnCompletionListener(OnCompletion l);
    abstract void setOnErrorListener(OnError l);
    abstract void setDataSource(String url) throws IOException;
    abstract void prepareAsync();
    abstract void start();
    abstract void pause();
    abstract void seekTo(int ms);
    abstract int getCurrentPosition();
    abstract int getDuration();
    abstract void reset();
    abstract void release();

    /** The TV's MediaPlayer, as MusicService always used it. */
    static final class Platform extends Playback {
        private final MediaPlayer mp = new MediaPlayer();

        @Override String kind() { return "mediaplayer"; }
        @Override void setAudioAttributes(AudioAttributes a) { mp.setAudioAttributes(a); }
        @Override void setWakeMode(Context c, int mode) { mp.setWakeMode(c, mode); }
        @Override void setVolume(float left, float right) { mp.setVolume(left, right); }
        @Override int getAudioSessionId() { return mp.getAudioSessionId(); }
        @Override void setOnPreparedListener(OnPrepared l) { mp.setOnPreparedListener(m -> l.onPrepared(this)); }
        @Override void setOnInfoListener(OnInfo l) { mp.setOnInfoListener((m, w, e) -> l.onInfo(this, w, e)); }
        @Override void setOnCompletionListener(OnCompletion l) { mp.setOnCompletionListener(m -> l.onCompletion(this)); }
        @Override void setOnErrorListener(OnError l) { mp.setOnErrorListener((m, w, e) -> l.onError(this, w, e)); }
        @Override void setDataSource(String url) throws IOException { mp.setDataSource(url); }
        @Override void prepareAsync() { mp.prepareAsync(); }
        @Override void start() { mp.start(); }
        @Override void pause() { mp.pause(); }
        @Override void seekTo(int ms) { mp.seekTo(ms); }
        @Override int getCurrentPosition() { return mp.getCurrentPosition(); }
        @Override int getDuration() { return mp.getDuration(); }
        @Override void reset() { mp.reset(); }
        @Override void release() { mp.release(); }
    }

    static int wakeLock() { return PowerManager.PARTIAL_WAKE_LOCK; }
}
