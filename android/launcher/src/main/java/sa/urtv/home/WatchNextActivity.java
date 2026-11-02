package sa.urtv.home;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * «أكمل المشاهدة»: the one question the row needs, Android's own «Allow» for the TV's Watch Next list (READ_TV_LISTINGS; the idea from
 * talkies, MIT: THIRD_PARTY_NOTICES.md). Opened by the screensaver's settings row when it is turned on; closes by itself when that is
 * allowed already, or once it is. Behind «قفل الإعدادات» like «ترتيب المفضلة». `am start -n sa.urtv.home/.WatchNextActivity`.
 */
public class WatchNextActivity extends Activity {
    /** «اللغة» (docs/ENGLISH.md): with Arabic the context itself, with English an English one */
    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    private int GOLD, TEXT, DIM;   // from the glass look (glass(), before anything is drawn)
    private GlassStyle.Look look;
    private static final int ASK = 7;

    private View screen, allow;
    private boolean verified;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Digits.init(this);
        FrameLayout root = new FrameLayout(this);
        glass(root);   // the glass look: the home's photo, small and blurred once, under its veil
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(text(S.t("أكمل المشاهدة"), 28, "Medium", TEXT), right(dp(10)));
        col.addView(text(S.t("تحت المفضلة: ما وقفته في نصه والحلقة الجاية، من تطبيقاتك. تحتاج الواجهة إذن قراءة هذه القائمة من التلفزيون مرة واحدة، ولا يُرسل شيء خارج التلفزيون."), 15, "Regular", DIM), right(dp(26)));
        allow = card(S.t("السماح"), v -> requestPermissions(new String[]{TvRows.PERMISSION, TvRows.EPG}, ASK));
        col.addView(allow, new LinearLayout.LayoutParams(dp(220), -2, Dir.start()));
        root.addView(col, new FrameLayout.LayoutParams(dp(640), -2, Gravity.CENTER));
        setContentView(root);
        screen = root;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (TvRows.allowed(this)) { finish(); return; }   // allowed already: nothing to ask
        if (verified) { allow.requestFocus(); return; }
        String[] targets = ParentalPolicy.targets(ParentalPolicy.ARRANGE_SCREEN, null);
        if (!Parental.needsPin(this, targets)) { verified = true; allow.requestFocus(); return; }
        screen.setVisibility(View.INVISIBLE);
        Parental.guard(this, Parental.adhanFirst(this), "أكمل المشاهدة", targets, () -> {
            verified = true;
            screen.setVisibility(View.VISIBLE);
            allow.requestFocus();
        }, () -> finish());
    }

    @Override
    protected void onPause() {
        super.onPause();
        PinPrompt.dismiss(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        verified = false;
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        if (code == ASK && TvRows.allowed(this)) finish();   // the home shows the row when it is back in front
    }

    private View card(String title, View.OnClickListener click) {
        TextView c = text(title, 18, "Medium", TEXT);
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(16), dp(12), dp(16), dp(12));
        c.setFocusable(true);
        c.setClickable(true);
        GlassPaint.card(c, look, 14);   // the panel, the solid focus fill and its ink while focused
        c.setOnClickListener(click);
        return c;
    }

    private TextView text(String s, float sp, String weight, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Dir.start());
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
        GOLD = look.accent; TEXT = look.text; DIM = look.sub;
    }
}
