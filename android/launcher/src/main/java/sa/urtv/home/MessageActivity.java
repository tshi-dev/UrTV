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

import sa.urtv.home.phone.PhoneDraw;
import sa.urtv.home.phone.PhoneMessage;

/**
 * 4.3 #53 «اكتب وارسم», the text: a paired phone's short message (PhoneMessage, through /api/message) shown on the TV in a card at the top
 * of the screen, over what plays (a see-through window: the film or the match goes on under it), for 20 s. Back hides it at once (Back is used
 * up: the app below does not get it); a new message replaces the one shown and its 20 s start again. Nothing is kept anywhere.
 * «رسائل الجوال» and «فوق التطبيقات» are its two switches (optimize/dock.sh message …), here because the phone code reads them.
 * 4.3 «ارسم»: the same window draws a paired phone's strokes LIVE over the whole screen (PhoneDraw.TV, PhoneDraw), and the drawing fades
 * away PhoneDraw.FADE_AFTER_MS after the last stroke; «امسح» on the phone clears it, Back hides and clears it. The window closes when neither a
 * message nor a drawing is left.
 */
public class MessageActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable close = new Runnable() { @Override public void run() {   // not this::finish: check.py type-checks without the SDK
        textShown = false;
        if (PhoneDraw.TV.total() == 0) finish(); else text.setVisibility(View.GONE);   // a drawing still up keeps the window
    } };
    private TextView text;
    private DrawLayer layer;
    private boolean textShown;
    private int seen = -1;
    /** true while this window is up: the strokes then follow by themselves (PhoneActions.draw starts it only when it is not) */
    private static volatile boolean up;

    public static boolean drawing() { return up; }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            long now = android.os.SystemClock.elapsedRealtime();
            PhoneDraw d = PhoneDraw.TV;
            float a = d.alpha(now);
            if (d.version() != seen || (a > 0f && a < 1f)) { seen = d.version(); layer.invalidate(); }
            if (a == 0f && d.total() > 0) { d.clear(); seen = d.version(); layer.invalidate(); }   // faded away: gone
            if (a == 0f && !textShown) { finish(); return; }
            handler.postDelayed(this, 50);
        }
    };

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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);   // not KEEP_SCREEN_ON: 20 s over what plays never holds the TV awake
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
        layer = new DrawLayer(this);
        root.addView(layer, new FrameLayout.LayoutParams(-1, -1));   // under the message card
        root.addView(text, lp);
        setContentView(root);
        up = true;
        show(getIntent());
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        show(i);
    }

    private void show(Intent i) {
        if (i != null && i.getBooleanExtra("draw", false)) {   // «ارسم»: the strokes come by themselves; the card stays as it is
            if (!textShown) text.setVisibility(View.GONE);
            handler.removeCallbacks(tick);
            handler.post(tick);
            return;
        }
        String t = PhoneMessage.clean(i == null ? null : i.getStringExtra("text"));
        if (t.isEmpty()) { if (!textShown && PhoneDraw.TV.total() == 0) finish(); return; }
        text.setText(t);
        text.setVisibility(View.VISIBLE);
        textShown = true;
        handler.removeCallbacks(close);
        handler.postDelayed(close, PhoneMessage.SHOW_MS);
    }

    @Override public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_BACK) { PhoneDraw.TV.clear(); finish(); return true; }   // the drawing goes with it   // Back hides it; any other key goes on to what is under it
        return super.onKeyDown(code, e);
    }

    @Override protected void onDestroy() {
        up = false;
        handler.removeCallbacks(tick);
        handler.removeCallbacks(close);
        super.onDestroy();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    /** The strokes (0..PhoneDraw.COORD each way) scaled to the screen, round lines in the six colours, at the drawing's alpha (its fade). */
    private static final class DrawLayer extends View {
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Path path = new android.graphics.Path();

        DrawLayer(Context c) {
            super(c);
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
            paint.setStrokeWidth(8 * c.getResources().getDisplayMetrics().density);
            paint.setShadowLayer(3 * c.getResources().getDisplayMetrics().density, 0, 0, 0x99000000);   // readable on a bright picture too
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        @Override protected void onDraw(android.graphics.Canvas canvas) {
            float a = PhoneDraw.TV.alpha(android.os.SystemClock.elapsedRealtime());
            if (a <= 0f) return;
            float w = getWidth() / (float) PhoneDraw.COORD, h = getHeight() / (float) PhoneDraw.COORD;
            for (PhoneDraw.Stroke s : PhoneDraw.TV.strokes()) {
                int n = s.points();
                if (n == 0) continue;
                int colour = PhoneDraw.PALETTE[Math.max(0, Math.min(PhoneDraw.PALETTE.length - 1, s.colour))];
                paint.setColor(colour);
                paint.setAlpha(Math.round(255 * a));
                path.reset();
                path.moveTo(s.x(0) * w, s.y(0) * h);
                if (n == 1) path.lineTo(s.x(0) * w + 0.1f, s.y(0) * h);   // a dot
                for (int i = 1; i < n; i++) path.lineTo(s.x(i) * w, s.y(i) * h);
                canvas.drawPath(path, paint);
            }
        }
    }
}
