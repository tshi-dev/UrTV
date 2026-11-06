package sa.urtv.home;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import sa.urtv.home.phone.PhoneUpdater;
import sa.urtv.home.phone.UpdateOrder;
import sa.urtv.home.phone.UpdateRecords;
import sa.urtv.home.phone.UpdateStore;
import sa.urtv.home.phone.UpdateText;

/**
 * «تحديث تلفزيوني»: the version installed, «تحقق من التحديث», and what the check found (the decision is UpdateFlow, the work is PhoneUpdater,
 * the same code the phone page's button calls). When there is a newer version and every check passed, the line «تحديث جديد · اضغط للتحديث» is
 * the button; Android's own install screen follows and the owner confirms with the remote, nothing is installed silently. After the launcher
 * has been replaced, the first start reads the «update in progress» record and opens this screen with how it went (once). No dialogs, like
 * DockActivity. Opened from the launcher's menu or from the screensaver's settings; only our two apps can open it (the signature permission
 * sa.urtv.home.OPEN_UPDATE, so `am start` from a shell is refused).
 */
public class UpdateActivity extends Activity {
    /**
     * The words of UpdateText and UpdateOrder (plain Java, so they cannot mark their own), marked here for the table (i18n/en/home.tsv):
     * this screen says each with S.tv. tests/i18n_home_test.py keeps this list equal to theirs.
     */
    static final String[] UPDATE_WORDS = {
        S.n("تحديث تلفزيوني"),
        S.n("تحقق من التحديث"),
        S.n("ما فيه اتصال بالإنترنت"),
        S.n("التحديث من التلفزيون ما يشتغل على هذا الجهاز.\nاطلب من مساعد الذكاء الاصطناعي على جهازك:\nحدّث تلفزيوني"),
        S.n("لازم تسمح لتطبيق تلفزيونك بتثبيت التحديثات."),
        S.n("فتح شاشة السماح"),
        S.n("جارٍ التحقق…"),
        S.n("جارٍ تنزيل التحديث والتحقق منه…"),
        S.n("أكّد التثبيت على شاشة أندرويد بالريموت."),
        S.n("اعرض شاشة التثبيت"),
        S.n("ما اكتمل التحديث"),
        S.n("ألغيت التثبيت."),
        S.n("الإصدار المثبّت %s"),
        S.n("تلفزيونك محدّث: %s"),
        S.n("تحديث جديد %s · اضغط للتحديث"),
        S.n("تم التحديث ✓ %s"),
        S.n("الخطوة: %s"),
        S.n("تحديث تطبيق الحافظة"),
        S.n("تحديث الواجهة الرئيسية"),
        S.n("التحقق من الملفات"),
    };

    /** «اللغة» (docs/ENGLISH.md): with Arabic the context itself, with English an English one */
    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    private int GOLD, INK, TEXT, DIM, RED;   // from the glass look (glass(), before anything is drawn)
    private GlassStyle.Look look;

    private LinearLayout col;              // the card: title, steps, progress, the message, one button
    private String preview;                // debug builds only: draw a state without the network (CI's pictures of this screen)
    private UpdateRecords.Outcome result;   // what the record of an earlier update says, until the owner checks again
    private boolean askedPermission;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Digits.init(this);
        if (BuildConfig.DEBUG) preview = getIntent().getStringExtra("preview");
        ScrollView scroll = new ScrollView(this);
        glass(scroll);   // the glass look: the home's photo, small and blurred once, under its veil
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setFillViewport(true);
        FrameLayout frame = new FrameLayout(this);
        scroll.addView(frame, new FrameLayout.LayoutParams(-1, -1));
        int w = getResources().getDisplayMetrics().widthPixels, h = getResources().getDisplayMetrics().heightPixels;
        frame.setPadding(Math.round(w * 0.05f), Math.round(h * 0.05f) + dp(16), Math.round(w * 0.05f), Math.round(h * 0.05f) + dp(16));   // inside the 5 % safe area, 16 dp more
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutDirection(Dir.layout());
        col.setPadding(dp(40), dp(32), dp(40), dp(32));
        col.setBackground(GlassPaint.fill(this, GlassStyle.over(look.dialog, look.base), 22, look.line));   // a solid surface, like the page's dialogs
        frame.addView(col, new FrameLayout.LayoutParams(Math.min(dp(760), w - 2 * Math.round(w * 0.05f)), -2, Gravity.CENTER));
        setContentView(scroll);
        // the record of an update that replaced the launcher: shown here once, then removed
        UpdateRecords.Outcome o = UpdateRecords.take(new UpdateStore(this), PhoneUpdater.installedCode(this, getPackageName()));
        if (o.kind != UpdateRecords.Kind.NONE) result = o;
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // the update and install steps must not be cut by the screensaver while this screen is shown
        PhoneUpdater.setVisible(true);
        PhoneUpdater.setListener(new Runnable() {
            @Override public void run() { runOnUiThread(new Runnable() { @Override public void run() { render(); } }); }
        });
        // back from the system screen where the owner allowed it: check again
        if (askedPermission && PhoneUpdater.canInstall(this)) {
            askedPermission = false;
            result = null;
            PhoneUpdater.check(this);
        }
        render();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        PhoneUpdater.setVisible(false);
        PhoneUpdater.setListener(null);
    }

    // ---------------------------------------------------------------- screen

    private void render() {
        col.removeAllViews();
        col.addView(text(S.tv(UpdateText.TITLE), 28, "Medium", TEXT, Dir.start()), right(dp(8)));
        col.addView(text(UpdateText.installed(PhoneUpdater.installedName(this), S::tv), 16, "Regular", DIM, Dir.start()), right(dp(24)));
        View focus;
        if (result != null) {
            focus = renderRecord();
        } else {
            focus = renderState();
        }
        if (focus != null) {
            final View f = focus;
            f.post(new Runnable() { @Override public void run() { f.requestFocus(); } });
        }
    }

    private View renderRecord() {
        if (result.kind == UpdateRecords.Kind.SUCCESS) {
            steps(4);
            col.addView(text(UpdateText.done(UpdateText.versionOf(BuildConfig.MARKETING, result.target), S::tv), 22, "Medium", GOLD, Dir.start()), right(dp(8)));
            return doneButton();
        } else {
            col.addView(text(S.tv(UpdateText.NOT_DONE), 22, "Medium", RED, Dir.start()), right(dp(6)));
            col.addView(text(UpdateText.failedStep(S.tv(UpdateOrder.label(result.step)), S::tv), 15, "Regular", DIM, Dir.start()), right(dp(14)));
            aiMessage();
        }
        return checkButton();
    }

    private View renderState() {
        Map<String, Object> st = status();
        String s = (String) st.get("status");
        String why = st.containsKey("reason") ? (String) st.get("reason") : "";
        String detail = st.containsKey("detail") ? (String) st.get("detail") : "";
        Object b = st.get("build");
        String newest = UpdateText.versionOf(BuildConfig.MARKETING, b == null ? 0 : ((Long) b).intValue());
        switch (s) {
            case "checking":
                col.addView(text(S.tv(UpdateText.WORKING), 18, "Medium", GOLD, Dir.start()), right(dp(8)));
                return null;
            case "downloading": {
                long[] pr = progress();
                steps(pr[2] == 1 ? 1 : 0);
                col.addView(text(S.tv(UpdateText.DOWNLOADING), 18, "Medium", GOLD, Dir.start()), right(dp(16)));
                bar(pr[0], pr[1]);
                return null;
            }
            case "uptodate":
                col.addView(text(UpdateText.upToDate(PhoneUpdater.installedName(this), S::tv), 20, "Medium", GOLD, Dir.start()), right(dp(16)));
                return checkButton();
            case "found":       // the phone page only read version.txt: pressing downloads, checks, then installs
            case "available":
                steps(-1);
                return button(UpdateText.available(newest, S::tv), GOLD, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        // with a family PIN set and «قفل الإعدادات» on, an install from the TV's own screen asks for it too (the phone's does, PhoneApi)
                        Parental.guard(UpdateActivity.this, Parental.adhanFirst(UpdateActivity.this), "تحديث تلفزيوني",
                                ParentalPolicy.targets(ParentalPolicy.UPDATE_SCREEN, null), new Runnable() {
                                    @Override public void run() { PhoneUpdater.install(UpdateActivity.this, false); }
                                }, new Runnable() { @Override public void run() { } });
                    }
                });
            case "waiting_owner":
                steps(2);
                col.addView(text(S.tv(UpdateText.CONFIRM), 20, "Medium", GOLD, Dir.start()), right(dp(6)));
                if (!detail.isEmpty()) col.addView(text(S.tv(UpdateOrder.label(detail)), 15, "Regular", DIM, Dir.start()), right(dp(14)));
                if (PhoneUpdater.confirmWaiting()) {
                    getWindow().getDecorView().post(new Runnable() { @Override public void run() { if (PhoneUpdater.confirmWaiting()) PhoneUpdater.showConfirm(UpdateActivity.this); } });
                }
                if (preview != null || PhoneUpdater.confirmKnown()) {
                    return button(S.tv(UpdateText.SHOW_CONFIRM), GOLD, new View.OnClickListener() {
                        @Override public void onClick(View v) { PhoneUpdater.showConfirm(UpdateActivity.this); }
                    });
                }
                return null;
            case "done":
                steps(4);
                col.addView(text(UpdateText.done(newest, S::tv), 22, "Medium", GOLD, Dir.start()), right(dp(8)));
                return doneButton();
            case "error":
                return renderError(why, detail);
            default:
                return checkButton();
        }
    }

    private View renderError(String why, String detail) {
        switch (why) {
            case "offline":
                col.addView(text(S.tv(UpdateText.OFFLINE), 20, "Medium", RED, Dir.start()), right(dp(16)));
                return checkButton();
            case "need_permission":
                col.addView(text(S.tv(UpdateText.NEED_PERMISSION), 18, "Medium", TEXT, Dir.start()), right(dp(14)));
                return button(S.tv(UpdateText.OPEN_PERMISSION), GOLD, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (PhoneUpdater.openPermissionScreen(UpdateActivity.this)) askedPermission = true;
                        else PhoneUpdater.permissionScreenFailed();   // the screen isn't there: the TV can't do this itself
                    }
                });
            case "declined":
                col.addView(text(S.tv(UpdateText.NOT_DONE), 22, "Medium", RED, Dir.start()), right(dp(6)));
                col.addView(text(S.tv(UpdateText.DECLINED), 15, "Regular", DIM, Dir.start()), right(dp(16)));
                return checkButton();
            case "failed":
                col.addView(text(S.tv(UpdateText.NOT_DONE), 22, "Medium", RED, Dir.start()), right(dp(6)));
                col.addView(text(UpdateText.failedStep(S.tv(UpdateOrder.label(detail)), S::tv), 15, "Regular", DIM, Dir.start()), right(dp(14)));
                aiMessage();
                return checkButton();
            default:   // "cannot", and anything this screen doesn't know: the same message
                aiMessage();
                return checkButton();
        }
    }

    /** The message that sends the owner to their AI assistant, with the repository address small under it. */
    private void aiMessage() {
        col.addView(text(S.tv(UpdateText.AI_MESSAGE), 20, "Medium", TEXT, Dir.start()), right(dp(8)));
        TextView repo = text("⁦" + UpdateText.REPO + "⁩", 12, "Regular", DIM, Dir.start());
        col.addView(repo, right(dp(18)));
    }

    /** «تم»: the end of an update goes to the home itself (the screen that stayed in front after the home install was this one, not the home; step 27) */
    private View doneButton() {
        return button(S.t("تم"), GOLD, new View.OnClickListener() {
            @Override public void onClick(View v) { toHome(); }
        });
    }

    private void toHome() {
        startActivity(new android.content.Intent(this, MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    /** Back from the screen that says the update is done leaves for the home, not for whatever app was in front before */
    @Override
    public void onBackPressed() {
        boolean done = (result != null && result.kind == UpdateRecords.Kind.SUCCESS) || "done".equals(PhoneUpdater.status().get("status"));
        if (done) toHome(); else super.onBackPressed();
    }

    private View checkButton() {
        return button(S.tv(UpdateText.CHECK), GOLD, new View.OnClickListener() {
            @Override public void onClick(View v) {
                result = null;
                PhoneUpdater.reset();
                PhoneUpdater.check(UpdateActivity.this);
                render();
            }
        });
    }

    // ---------------------------------------------------------------- the steps and the progress

    /** the status of the update: PhoneUpdater's, or in a debug build asked for a picture, the state named by the extra "preview" */
    private Map<String, Object> status() {
        if (preview == null) return PhoneUpdater.status();
        Map<String, Object> m = new HashMap<>();
        m.put("status", preview);
        m.put("build", 1L);
        return m;
    }

    private long[] progress() { return preview == null ? PhoneUpdater.progress() : new long[] {12_600_000L, 28_000_000L, 0}; }

    /** «تنزيل · تحقق · تثبيت · تم»: where the update is. at: the step in progress (0 to 3), 4: all done, -1: none begun. A line and four dots, no pills. */
    private void steps(int at) {
        String[] names = {S.t("تنزيل"), S.t("تحقق"), S.t("تثبيت"), S.t("تم")};
        col.addView(new Stepper(names, at), right(dp(24)));
    }

    /** the calm bar: a 8 dp track, the share received in the accent colour, and under it «٤٥٪ · ١٢٫٦ من ٢٨٫٠ م.ب» (nothing under it when the size is not known) */
    private void bar(long done, long total) {
        if (total <= 0) return;
        double share = Math.max(0, Math.min(1, done / (double) total));
        CalmBar track = new CalmBar(this, look.line, GOLD);
        track.setShare(share);
        col.addView(track, new LinearLayout.LayoutParams(-1, dp(8)));
        String mb = S.t("%s من %s م.ب", MarketData.show(MarketData.num(done / 1e6, 1), Digits.latin()), MarketData.show(MarketData.num(total / 1e6, 1), Digits.latin()));
        String pct = Digits.of(String.valueOf(Math.round(share * 100))) + (Digits.latin() ? "%" : "٪");
        TextView t = text(pct + " · " + mb, 15, "Regular", DIM, Dir.start());
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        col.addView(t, right(0));
    }

    /** the four steps: dots on one line, the labels under them, laid out the way the language reads */
    private final class Stepper extends View {
        private final String[] names;
        private final int at;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Typeface regular, medium;

        Stepper(String[] names, int at) {
            super(UpdateActivity.this);
            this.names = names;
            this.at = at;
            Typeface r = null, m = null;
            try { r = Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-Regular.otf"); m = Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-Medium.otf"); }
            catch (RuntimeException ignored) { }
            regular = r;
            medium = m;
        }

        @Override protected void onMeasure(int w, int h) { setMeasuredDimension(MeasureSpec.getSize(w), dp(56)); }

        @Override protected void onDraw(Canvas c) {
            int n = names.length;
            float col = getWidth() / (float) n, y = dp(8), dot = dp(5);
            boolean rtl = Dir.rtl();
            float first = col / 2, last = getWidth() - col / 2;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(look.line);
            c.drawLine(first, y, last, y, paint);   // the rail
            if (at > 0) {   // and the part of it that has been travelled, from the first dot to the one in progress
                float reach = Math.min(at, n - 1) * col;
                paint.setColor(GOLD);
                if (rtl) c.drawLine(last, y, last - reach, y, paint);
                else c.drawLine(first, y, first + reach, y, paint);
            }
            for (int i = 0; i < n; i++) {
                float x = rtl ? getWidth() - (i + 0.5f) * col : (i + 0.5f) * col;
                boolean done = i < at, now = i == at;
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(GlassStyle.over(look.dialog, look.base));
                c.drawCircle(x, y, dot + dp(3), paint);   // the rail does not run through a dot
                paint.setColor(done || now ? GOLD : look.line);
                c.drawCircle(x, y, now ? dot + dp(1) : dot, paint);
                if (!done && !now) {
                    paint.setColor(GlassStyle.over(look.dialog, look.base));
                    c.drawCircle(x, y, dot - dp(2), paint);
                }
                paint.setStyle(Paint.Style.FILL);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics()));
                paint.setTypeface(now ? medium : regular);
                paint.setColor(now ? TEXT : done ? TEXT : DIM);
                c.drawText(names[i], x, y + dp(34), paint);
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    private View button(String label, final int accent, View.OnClickListener click) {
        final TextView p = text(label, 18, "Medium", accent == RED ? RED : TEXT, Gravity.CENTER);   // the warning colour for RED
        p.setFocusable(true);
        p.setClickable(true);
        GlassPaint.card(p, look, 14);   // the panel, the solid focus fill and its ink while focused
        p.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(56));   // the one button of a state: 56 dp, full width, 24 dp below what it answers
        lp.topMargin = dp(24);
        col.addView(p, lp);
        return p;
    }

    private TextView text(String s, float sp, String weight, int color, int gravity) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(gravity);
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
        look = GlassSource.page(this, page);
        GOLD = look.accent; INK = look.onFocus; TEXT = look.text; DIM = look.sub; RED = look.warn;
    }
}
