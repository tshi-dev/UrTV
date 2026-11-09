package sa.urtv.home.tilawah;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Looper;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy;

/**
 * 4.3 A2: a live HLS station through Media3 ExoPlayer, behind Playback's MediaPlayer-shaped calls (MusicService does not know which plays). Kept
 * in src/exo/java (the Gradle build adds it with the Media3 dependency); Playback.create loads it by name and falls back to MediaPlayer, so the
 * javac compile of src/main/java (tests/android_compile.sh) needs no Media3. What it changes for a station like SBA's (a 12 s live window of 4 s
 * segments, a new connection per segment): a segment that fails is retried (8 times) instead of the stream failing; a playlist that fell behind
 * the live window starts again at the live edge quietly; the buffer starts after 2.5 s and keeps as much as the window holds.
 */
public final class ExoPlayback extends Playback {
    static final int MIN_BUFFER_MS = 15_000, MAX_BUFFER_MS = 60_000, START_MS = 2_500, RESTART_MS = 5_000, RETRIES = 8;
    static final String UA = "UrTV (Android TV)";

    private final ExoPlayer exo;
    private String url;
    private OnPrepared onPrepared;
    private OnInfo onInfo;
    private OnCompletion onCompletion;
    private OnError onError;
    private boolean prepared, rebuffering;

    public ExoPlayback(Context c) {
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setUserAgent(UA)
                .setConnectTimeoutMs(10_000)
                .setReadTimeoutMs(15_000)
                .setAllowCrossProtocolRedirects(false);   // https stays https (the app allows cleartext for radiojar.com only)
        exo = new ExoPlayer.Builder(c)
                .setLooper(Looper.getMainLooper())
                .setLoadControl(new DefaultLoadControl.Builder().setBufferDurationsMs(MIN_BUFFER_MS, MAX_BUFFER_MS, START_MS, RESTART_MS).build())
                .setMediaSourceFactory(new HlsMediaSource.Factory(http).setLoadErrorHandlingPolicy(new DefaultLoadErrorHandlingPolicy(RETRIES)))
                .setHandleAudioBecomingNoisy(false)
                .build();
        exo.setPlayWhenReady(false);
        exo.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    if (!prepared) {
                        prepared = true;
                        if (onPrepared != null) onPrepared.onPrepared(ExoPlayback.this);
                    } else if (rebuffering) {
                        rebuffering = false;
                        if (onInfo != null) onInfo.onInfo(ExoPlayback.this, MediaPlayer.MEDIA_INFO_BUFFERING_END, 0);
                    }
                } else if (state == Player.STATE_BUFFERING && prepared && !rebuffering) {
                    rebuffering = true;
                    if (onInfo != null) onInfo.onInfo(ExoPlayback.this, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
                } else if (state == Player.STATE_ENDED && onCompletion != null) {
                    onCompletion.onCompletion(ExoPlayback.this);
                }
            }

            @Override public void onPlayerError(PlaybackException e) {
                if (e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {   // fell out of the 12 s window: back to the live edge, quietly
                    exo.seekToDefaultPosition();
                    exo.prepare();
                    return;
                }
                if (onError != null) onError.onError(ExoPlayback.this, MediaPlayer.MEDIA_ERROR_UNKNOWN, e.errorCode);
            }
        });
    }

    @Override String kind() { return "exo"; }

    @Override void setAudioAttributes(android.media.AudioAttributes a) {
        // the same as MEDIA_ATTRS (USAGE_MEDIA, music); the service asks for audio focus itself, so ExoPlayer does not
        exo.setAudioAttributes(new androidx.media3.common.AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false);
    }

    @Override void setWakeMode(Context c, int mode) { exo.setWakeMode(C.WAKE_MODE_NETWORK); }   // the CPU and the Wi-Fi stay up while it plays
    @Override void setVolume(float left, float right) { exo.setVolume(Math.max(left, right)); }
    @Override int getAudioSessionId() { return exo.getAudioSessionId(); }
    @Override void setOnPreparedListener(OnPrepared l) { onPrepared = l; }
    @Override void setOnInfoListener(OnInfo l) { onInfo = l; }
    @Override void setOnCompletionListener(OnCompletion l) { onCompletion = l; }
    @Override void setOnErrorListener(OnError l) { onError = l; }
    @Override void setDataSource(String u) { url = u; }
    @Override void prepareAsync() { exo.setMediaItem(MediaItem.fromUri(url)); exo.prepare(); }
    @Override void start() { exo.play(); }
    @Override void pause() { exo.pause(); }
    @Override void seekTo(int ms) { exo.seekTo(ms); }
    @Override int getCurrentPosition() { return (int) Math.max(0, exo.getCurrentPosition()); }
    @Override int getDuration() { long d = exo.getDuration(); return d == C.TIME_UNSET ? -1 : (int) d; }
    @Override void reset() { exo.stop(); prepared = false; rebuffering = false; }
    @Override void release() { exo.release(); }
}
