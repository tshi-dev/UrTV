package sa.urtv.home;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * «UrTV النظيف»: the screen the settings' row opens. It says first, in plain words, what will be hidden, what stays, that nothing is deleted and that one press brings everything
 * back; only «فعّل» on it turns the mask on, with the family PIN when one is set, and the focus starts on «إلغاء». When it is on, the same screen says so and brings the full home
 * back (the PIN again when one is set). «تخصيص» is the sheet of what stays (CleanMode.OPTIONS). Nothing here writes over the person's own settings: CleanMode is only read by the home.
 */
public class CleanActivity extends Activity {
    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    private GlassStyle.Look look;
    private LinearLayout col;
    private boolean customising;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Digits.init(this);
        ScrollView scroll = new ScrollView(this);
        look = GlassSource.page(this, scroll);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setFillViewport(true);
        FrameLayout frame = new FrameLayout(this);
        scroll.addView(frame, new FrameLayout.LayoutParams(-1, -1));
        int w = getResources().getDisplayMetrics().widthPixels, h = getResources().getDisplayMetrics().heightPixels;
        frame.setPadding(Math.round(w * 0.05f), Math.round(h * 0.05f) + dp(16), Math.round(w * 0.05f), Math.round(h * 0.05f) + dp(16));   // inside the 5 % safe area
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutDirection(Dir.layout());
        col.setPadding(dp(40), dp(32), dp(40), dp(32));
        col.setBackground(GlassPaint.fill(this, GlassStyle.over(look.dialog, look.base), 22, look.line));
        frame.addView(col, new FrameLayout.LayoutParams(Math.min(dp(880), w - 2 * Math.round(w * 0.05f)), -2, Gravity.CENTER));
        if (BuildConfig.DEBUG && "customise".equals(getIntent().getStringExtra("preview"))) customising = true;   // CI's picture of the sheet (DebugCleanShot)
        setContentView(scroll);
        render();
    }

    /** Home pressed (or another app asked for): this screen is done, not left behind the home to come back to the front when Settings opens later (step 47) */
    @Override protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (customising) { customising = false; render(); return; }   // Back from the sheet goes back to the description
        super.onBackPressed();
    }

    private void render() {
        col.removeAllViews();
        if (customising) { sheet(); return; }
        final boolean on = CleanMode.on(this);
        col.addView(text(on ? S.t("\u2066UrTV\u2069 النظيف مفعّل") : S.t("تفعيل UrTV النظيف؟"), 30, "Medium", look.text), lp(dp(16)));
        for (String line : on ? new String[] {S.t("الشاشة الرئيسية وشاشة التوقف و«تلفزيونك» تعرض الأساسيات فقط الآن."), S.t("ضغطة وحدة على «رجّع الشاشة الكاملة» ترجّع كل شيء كما اخترته أنت.")}
                : new String[] {S.t("يبقى: الساعة والتاريخ وشريط التطبيقات، وزر الإعدادات."), S.t("يختفي: الأزرار الإضافية ومركز التحكم والتنبيهات والصفوف الزائدة."),
                                S.t("شاشة التوقف: الصورة والساعة فقط، وسطر الصلاة القادمة إن اخترته. بدون بطاقات ولا أسواق ولا مباريات ولا مهام ولا حديث."),
                                S.t("«تلفزيونك»: يفتح على الإعدادات مباشرة، بدون الألعاب والأسواق والمباريات."),
                                S.t("يبقى شغّالًا كما ضبطته: الأذان والتذكيرات والقفل العائلي والتحديثات."),
                                S.t("ما يُحذف شيء، وما يتغيّر شيء من إعداداتك. كل ما أخفيناه يرجع بضغطة وحدة من نفس هالشاشة.")})
            col.addView(text(line, 20, "Regular", look.sub), lp(dp(12)));
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setLayoutDirection(Dir.layout());
        LinearLayout.LayoutParams top = lp(0);
        top.topMargin = dp(16);
        col.addView(buttons, top);
        View cancel;
        if (on) {
            View back = button(S.t("رجّع الشاشة الكاملة"), v -> pin(() -> { CleanMode.setOn(this, false); finish(); }));
            buttons.addView(back, bw(false));
            buttons.addView(button(S.t("تخصيص"), v -> { customising = true; render(); }), bw(true));
            buttons.addView(button(S.t("تم"), v -> finish()), bw(true));
            cancel = back;   // the focus of an «on» screen starts on the way back
        } else {
            View turnOn = button(S.t("فعّل"), v -> pin(() -> { CleanMode.setOn(this, true); finish(); }));
            buttons.addView(turnOn, bw(false));
            buttons.addView(button(S.t("تخصيص"), v -> { customising = true; render(); }), bw(true));
            cancel = button(S.t("إلغاء"), v -> finish());
            buttons.addView(cancel, bw(true));
        }
        final View first = cancel;
        first.post(first::requestFocus);   // the focus starts on «إلغاء» (when it is on: on the way back)
    }

    /** «تخصيص»: one tick per thing that may stay; the clock, the date and the apps bar always do */
    private void sheet() {
        col.addView(text(S.t("تخصيص UrTV النظيف"), 30, "Medium", look.text), lp(dp(8)));
        col.addView(text(S.t("اختر القليل اللي يبقى. الساعة والتاريخ وشريط التطبيقات دائمًا."), 20, "Regular", look.sub), lp(dp(16)));
        final Set<String> keep = new LinkedHashSet<>(CleanMode.keep(this));
        View firstRow = null;
        for (final String o : CleanKeep.OPTIONS) {
            final TextView row = button(mark(keep.contains(o)) + name(o), null);
            row.setOnClickListener(v -> {
                if (keep.contains(o)) keep.remove(o); else keep.add(o);
                CleanMode.setKeep(this, keep);
                row.setText(mark(keep.contains(o)) + name(o));
            });
            col.addView(row, lp(dp(8)));
            if (firstRow == null) firstRow = row;
        }
        LinearLayout.LayoutParams dp = lp(0);
        dp.topMargin = dp(16);
        col.addView(button(S.t("تم"), v -> { customising = false; render(); }), dp);
        final View f = firstRow;
        f.post(f::requestFocus);
    }

    private static String mark(boolean on) { return on ? "✓  " : "▢  "; }

    private String name(String o) {
        switch (o) {
            case CleanMode.CARD: return S.t("بطاقة واحدة");
            case CleanMode.WIFI: return S.t("علامة الواي فاي");
            case CleanMode.GREETING: return S.t("التحية");
            case CleanMode.PRAYER: return S.t("الصلاة القادمة");
            default: return S.t("زر الإعدادات");
        }
    }

    /** the family PIN first when one is set; then {@code go} (a wrong or cancelled PIN leaves everything as it was) */
    private void pin(final Runnable go) {
        if (!Parental.state(this).pinSet) { go.run(); return; }
        Parental.confirm(this, Parental.adhanFirst(this), S.tv("\u2066UrTV\u2069 النظيف"), go, () -> { });
    }

    // ---------------------------------------------------------------- pieces

    private TextView button(String label, View.OnClickListener click) {
        TextView t = text(label, 22, "Medium", look.text);
        t.setGravity(Gravity.CENTER_VERTICAL | Dir.start());
        t.setPadding(dp(24), dp(16), dp(24), dp(16));
        t.setFocusable(true);
        t.setClickable(true);
        t.setId(View.generateViewId());
        if (click != null) t.setOnClickListener(click);
        GlassPaint.card(t, look, 14);
        return t;
    }

    private LinearLayout.LayoutParams bw(boolean gap) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1f);
        if (gap) p.setMarginStart(dp(16));
        return p;
    }

    private TextView text(String s, float sp, String weight, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Dir.start());
        t.setLineSpacing(dp(4), 1f);
        try { t.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-" + weight + ".otf")); }
        catch (RuntimeException ignored) { }
        return t;
    }

    private LinearLayout.LayoutParams lp(int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = bottom;
        return p;
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
