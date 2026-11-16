package sa.urtv.home.tilawah;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * 4.4 «شاشة سوداء» (GitHub #62, designed by its author): a black window over everything while the recitation or the radio goes on in
 * MusicService. The screen is kept on (and as dark as the TV allows, screenBrightness 0) only while something plays; when it stops (a pause,
 * the sleep timer, a stop) it closes itself and the TV may sleep as usual. Any key but volume and mute closes it and goes back to the player.
 * Opened from the player's options, the home's «يُتلى الآن» card and the control center, behind the «تلاوة وأذان» lock (PIN, kids mode).
 */
public class BlackScreenActivity extends Activity {
    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(sa.urtv.home.S.wrap(b)); }
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long quietSince;

    private final BroadcastReceiver changed = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { follow(); }
    };
    private final Runnable look = new Runnable() {
        @Override public void run() { follow(); handler.postDelayed(this, 1000); }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        View black = new View(this);
        black.setBackgroundColor(Color.BLACK);
        black.setFocusable(true);
        setContentView(black);
        black.requestFocus();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;   // 0: the darkest the TV allows (many TVs ignore it: the picture is black anyway)
        getWindow().setAttributes(lp);
    }

    @Override protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter(NowPlaying.CHANGED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(changed, f, Context.RECEIVER_NOT_EXPORTED); else registerReceiver(changed, f);
        quietSince = 0;
        handler.post(look);
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(look);
        try { unregisterReceiver(changed); } catch (IllegalArgumentException ignored) { }
        keepOn(false);
        if (!isFinishing()) finish();   // another screen came over it (the adhan's, a message): the black screen is not left behind
    }

    /** the screen kept on only while something plays; closed after BlackScreen.GRACE_MS of nothing */
    private void follow() {
        boolean on = BlackScreen.playing(MusicService.currentSource(), MusicService.isPlaying(), MusicService.isBuffering());
        long now = SystemClock.elapsedRealtime();
        if (on) quietSince = 0; else if (quietSince == 0) quietSince = now;
        keepOn(on);
        if (BlackScreen.close(on, quietSince, now)) finish();
    }

    private void keepOn(boolean on) {
        if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        if (!BlackScreen.closesOn(e.getKeyCode())) return super.dispatchKeyEvent(e);   // volume and mute: the TV's own
        if (e.getAction() == KeyEvent.ACTION_UP) {
            boolean on = BlackScreen.playing(MusicService.currentSource(), MusicService.isPlaying(), MusicService.isBuffering());
            if (on) startActivity(new Intent(this, PlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        }
        return true;   // the key is the black screen's: it never reaches the player or the home behind it
    }

    /** the way in, for the home's card and the control center (sa.urtv.home): the intent their lock opens */
    public static Intent open(Context c) { return new Intent(c, BlackScreenActivity.class); }
}
