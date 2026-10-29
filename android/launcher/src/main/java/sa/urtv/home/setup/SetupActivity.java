package sa.urtv.home.setup;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import sa.urtv.home.SetupGate;

import java.util.List;

/**
 * «إعداد تلفزيوني»: the setup, done by the TV itself with no computer. It shows exactly what it will do (SetupEngine.plan), and «ابدأ» runs it over
 * the TV's own local ADB: the person accepts the TV's "Allow USB debugging?" once. The run is SetupRunner's (one at a time, on a thread that outlives
 * this screen): this screen only starts it and shows its lines and its end. «تراجع» undoes it with the same ledger and journal rules as
 * optimize/restore.sh. Opened from the apps row (a second launcher entry of this app), so it is there right after the APK is installed with
 * the TV's Downloader app. Nothing here logs a network name, a serial or an address.
 */
public class SetupActivity extends Activity {
    private int GOLD, INK, TEXT, DIM, RED;   // from the glass look (glass(), before anything is drawn)
    private sa.urtv.home.GlassStyle.Look look;

    private TextView log, status, start, undo;
    private boolean running;   // a run is shown as going (this screen's thread only)
    private long shown;        // the runner's last line shown here

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        glass(scroll);   // the glass look: the home's photo, small and blurred once, under its veil
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(120), dp(36), dp(120), dp(48));
        scroll.addView(col);

        col.addView(text("إعداد تلفزيوني", 26, "Medium", TEXT), right(0));
        col.addView(text("يجهّز التلفزيون نفسه، بدون كمبيوتر. هذا ما سيفعله بالترتيب:", 14, "Regular", DIM), right(dp(10)));
        List<String> plan = SetupEngine.plan();
        for (int i = 0; i < plan.size(); i++) col.addView(text((i + 1) + ". " + plan.get(i), 14, "Regular", TEXT), right(dp(6)));
        col.addView(text("قبل البدء: فعّل «تصحيح USB» (الإعدادات ← تفضيلات الجهاز ← حول ← اضغط «Build» سبع مرات ← خيارات المطوّر ← تصحيح USB). "
            + "عند الضغط على «ابدأ» سيسألك التلفزيون «السماح بتصحيح USB؟»: اختر «السماح دائماً».", 13, "Regular", GOLD), right(dp(14)));

        start = button("ابدأ", TEXT);
        start.setFilterTouchesWhenObscured(true);   // another window over this screen cannot press the buttons
        start.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { go(false); } });
        col.addView(start, right(dp(10)));
        undo = button("تراجع (يرجّع التلفزيون كما كان)", TEXT);
        undo.setFilterTouchesWhenObscured(true);
        undo.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { go(true); } });
        col.addView(undo, right(dp(14)));

        status = text("", 16, "Medium", GOLD);
        col.addView(status, right(dp(8)));
        log = text("", 13, "Regular", DIM);
        col.addView(log, right(0));
        setContentView(scroll);
        start.requestFocus();
        if (SetupRecovery.pending(this) && SetupRecovery.gaveUp(this)) {
            status.setText(SetupRecovery.GAVE_UP_NOTICE + ": اضغط «تراجع».");
        } else if (SetupRecovery.pending(this) && !SetupRunner.get().running()) {
            // a home switch was cut off (the app was closed, the TV restarted): the safety switch is set again from its saved deadline, and one attempt is made now
            status.setText("بدأ تبديل سابق للشاشة الرئيسية ولم يكتمل. نحاول إرجاع الشاشة الأصلية الآن؛ إن لم تظهر وحدها فاضغط «تراجع».");
            SetupRecovery.rearmFromDisk(this);
            new Thread(new Runnable() {
                @Override public void run() {
                    SetupEngine.Result r = SetupRecovery.attempt(SetupActivity.this, new SetupEngine.Out() { @Override public void line(String t) { SetupActivity.this.line(t); } });
                    if (r != null) {
                        final String m = r.message;
                        runOnUiThread(new Runnable() { @Override public void run() { status.setText(m); } });
                    }
                }
            }, "setup-recover").start();
        }
    }

    private void go(final boolean undoIt) {
        if (running) return;
        // with a family PIN set, the PIN first
        SetupGate.guard(this, new Runnable() { @Override public void run() { goAfterPin(undoIt); } }, new Runnable() { @Override public void run() { } });
    }

    private void goAfterPin(final boolean undoIt) {
        if (running) return;
        // the run itself is the runner's (one at a time, on a thread that outlives this screen): this screen starts it and shows what it says
        long before = SetupRunner.get().lastSeq();   // read first: the run's first line can come before start() returns
        SetupRunner.Start s = SetupRunner.get().start(undoIt ? SetupRunner.Kind.UNDO : SetupRunner.Kind.SETUP, new SetupRunner.Options(), new SetupHost(this));
        if (s != SetupRunner.Start.STARTED) {
            status.setText("يعمل إعداد آخر الآن؛ انتظر حتى ينتهي.");
            return;
        }
        log.setText("");
        shown = before;
        busy(true, undoIt);
    }

    /** The lines the runner wrote since the last ones shown, and its end once it has one. Called on this screen's thread. */
    private void render() {
        SetupRunner r = SetupRunner.get();
        for (SetupRunner.Line l : r.linesSince(shown)) {
            log.append("• " + l.text + "\n");
            shown = l.seq;
        }
        if (running && !r.running()) {
            SetupRunner.End e = r.end();
            busy(false, false);
            if (e != null) {
                status.setText(e.text);
                status.setTextColor(e.bad ? RED : GOLD);
            }
        }
    }

    /** While a run is going: the screen stays on (the screensaver would cut it) and the buttons are dimmed. */
    private void busy(boolean on, boolean undoIt) {
        if (on) {
            running = true;
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // only while a run is going (the screensaver would cut it)
            start.setAlpha(.5f);
            undo.setAlpha(.5f);
            status.setTextColor(GOLD);
            status.setText(undoIt ? "جارٍ الرجوع…" : "جارٍ الإعداد… لا تطفئ التلفزيون.");
        } else {
            running = false;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            start.setAlpha(1f);
            undo.setAlpha(1f);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        SetupRunner.get().setListener(new Runnable() {
            @Override public void run() { runOnUiThread(new Runnable() { @Override public void run() { render(); } }); }
        });
        // a run that was started before this screen was opened (or while it was closed) is shown as it is now
        if (!running && SetupRunner.get().running()) {
            shown = 0;
            log.setText("");
            busy(true, SetupRunner.get().kind() == SetupRunner.Kind.UNDO);
        }
        render();
    }

    @Override protected void onPause() {
        SetupRunner.get().setListener(null);
        super.onPause();
    }

    /** A line of the recovery attempt at the start of this screen (not a run: the runner's lines come through render). */
    private void line(final String t) {
        runOnUiThread(new Runnable() { @Override public void run() { log.append("• " + t + "\n"); } });
    }

    /** a button in the glass look: the panel, the solid focus fill and its ink while focused */
    private TextView button(String label, int fg) {
        TextView t = text(label, 18, "Medium", fg);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(16), dp(14), dp(16), dp(14));
        t.setFocusable(true);
        t.setClickable(true);
        sa.urtv.home.GlassPaint.card(t, look, 14);
        return t;
    }

    private TextView text(String s, float sp, String weight, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Gravity.RIGHT);
        try { t.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-" + weight + ".otf")); }
        catch (RuntimeException ignored) { }
        return t;
    }

    private LinearLayout.LayoutParams right(int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = bottom;
        return p;
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    /** The glass look on this page (GlassSource: the home's photo under its veil, or the look's solid colour), and this screen's colours from it. */
    private void glass(View page) {
        look = sa.urtv.home.GlassSource.page(this, page);
        GOLD = look.accent; INK = look.onFocus; TEXT = look.text; DIM = look.sub; RED = look.warn;
    }
}
