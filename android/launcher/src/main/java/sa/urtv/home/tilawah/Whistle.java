package sa.urtv.home.tilawah;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

/**
 * 4.4 the kick-off whistle (res/raw/kickoff_whistle.ogg, our own, CC0; .github/tools/make_whistle.py): 1.2 s at the TV's notification
 * volume (it never raises the TV's volume); over an app that plays sound it asks for a short duck of it and gives it back. The caller decides
 * whether it sounds (KickoffPlan.sound): the quiet hours (quietNow, «ساعات الهدوء»), the adhan, kids mode, «بدون صوت».
 */
public final class Whistle {
    private Whistle() { }

    /** «ساعات الهدوء» hold now (the duas' and reminders' window) */
    public static boolean quietNow(Context c) {
        try { return new Prefs(c).quietNow(c); } catch (RuntimeException e) { return false; }
    }

    public static void play(Context c) {
        final Context app = c.getApplicationContext();
        new Handler(Looper.getMainLooper()).post(() -> {
            AudioManager am = (AudioManager) app.getSystemService(Context.AUDIO_SERVICE);
            AudioAttributes attrs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            AudioFocusRequest focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attrs).build();
            MediaPlayer mp = MediaPlayer.create(app, sa.urtv.home.R.raw.kickoff_whistle, attrs, am == null ? 0 : am.generateAudioSessionId());
            if (mp == null) return;
            if (am != null) am.requestAudioFocus(focus);
            mp.setOnCompletionListener(p -> { p.release(); if (am != null) am.abandonAudioFocusRequest(focus); });
            mp.setOnErrorListener((p, w, e) -> { p.release(); if (am != null) am.abandonAudioFocusRequest(focus); return true; });
            mp.start();
        });
    }
}
