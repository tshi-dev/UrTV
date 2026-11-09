package sa.urtv.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import sa.urtv.home.phone.PhoneMessage;

/**
 * 4.3 #53 «اكتب وارسم», the text: a paired phone's short message (PhoneMessage, through /api/message) shown on the TV in a card at the top
 * of the screen, over what plays (a see-through window: the film or the match goes on under it), for 20 s. Back hides it at once (Back is used
 * up: the app below does not get it); a new message replaces the one shown and its 20 s start again. Nothing is kept anywhere.
 * «رسائل الجوال» and «فوق التطبيقات» are its two switches (optimize/dock.sh message …), here because the phone code reads them.
 */
public class MessageActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable close = this::finish;
    private TextView text;

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("MainActivity", Context.MODE_PRIVATE); }   // the launcher's file (Dock.prefs)

    /** «رسائل الجوال»: on by default. */
    public static boolean on(Context c) { return prefs(c).getBoolean("phone_message", true); }
    static void setOn(Context c, boolean on) { prefs(c).edit().putBoolean("phone_message", on).apply(); }
    /** «فوق التطبيقات»: on by default; off = only on our home and its screens, never over a film or a match. */
    public static boolean overApps(Context c) { return prefs(c).getBoolean("phone_message_over_apps", true); }
    static void setOverApps(Context c, boolean on) { prefs(c).edit().putBoolean("phone_message_over_apps", on).apply(); }

    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout root = new FrameLayout(this);
        text = new TextView(this);
        text.setTextColor(Color.WHITE);
        text.setTextSize(34);
        text.setGravity(Gravity.CENTER);
        text.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        text.setMaxLines(PhoneMessage.MAX_LINES + 1);
        text.setPadding(dp(40), dp(24), dp(40), dp(24));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6141414);
        bg.setCornerRadius(dp(28));
        bg.setStroke(dp(2), 0x33FFFFFF);
        text.setBackground(bg);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lp.topMargin = dp(64);
        lp.leftMargin = lp.rightMargin = dp(96);
        root.addView(text, lp);
        setContentView(root);
        show(getIntent());
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        show(i);
    }

    private void show(Intent i) {
        String t = PhoneMessage.clean(i == null ? null : i.getStringExtra("text"));
        if (t.isEmpty()) { finish(); return; }
        text.setText(t);
        handler.removeCallbacks(close);
        handler.postDelayed(close, PhoneMessage.SHOW_MS);
    }

    @Override public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_BACK) { finish(); return true; }   // Back hides it; any other key goes on to what is under it
        return super.onKeyDown(code, e);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(close);
        super.onDestroy();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
