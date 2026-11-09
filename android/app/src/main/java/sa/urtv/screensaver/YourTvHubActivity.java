package sa.urtv.screensaver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * «تلفزيونك»: what opening the app shows (its launcher entry): four windows in two lines, the same page, grid, spacing, focus and title place as the
 * settings' first screen (SettingsActivity), so the two read as one system. Each window has its own mark (a solid silhouette that reads from 3 m), its name,
 * one line of what is true now, and a colour of its own on the mark: «الأسواق» (the home's markets page, «محفظتي» inside it), «المباريات», «الألعاب»
 * («قريبًا» until the games land: {@link #GAMES_READY}) and «الإعدادات» (SettingsActivity, as before: the remote's Settings key, the control center's row
 * and the home's settings button open that one, not this hub). One press opens a window; Back leaves.
 */
public class YourTvHubActivity extends Activity {
    /** off: «الألعاب» is in the hub and says «قريبًا»; on, with the games' own activity, its OK opens them */
    static final boolean GAMES_READY = false;

    /** the windows, first to last (from the right in Arabic, from the left in English): key, name, the mark, the mark's colour */
    static final String[][] WINDOWS = {
        {"markets", "الأسواق", "ic_hub_markets", "#14705A"},
        {"matches", "المباريات", "ic_hub_matches", "#1F5FBF"},
        {"games", "الألعاب", "ic_hub_games", "#7B3FC9"},
        {"settings", "الإعدادات", "ic_settings", "#5B6677"},
    };

    private Prefs prefs;
    private GlassStyle.Look look = GlassStyle.DARK;
    private final Map<String, Typeface> fonts = new HashMap<>();

    @Override
    protected void attachBaseContext(Context base) {   // the same direction rules as SettingsActivity: Arabic right to left, English left to right
        Context en = S.wrap(base);
        if (S.en()) { super.attachBaseContext(en); return; }
        android.content.res.Configuration c = new android.content.res.Configuration(base.getResources().getConfiguration());
        c.setLocale(new java.util.Locale("ar"));
        c.setLayoutDirection(new java.util.Locale("ar"));
        super.attachBaseContext(base.createConfigurationContext(c));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setWindowAnimations(0);
        prefs = new Prefs(this);
        FrameLayout root = new FrameLayout(this);
        root.setLayoutDirection(Dir.layout());
        look = GlassSource.page(this, root);
        int width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(144), dp(1100));
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setLayoutDirection(Dir.layout());
        LinearLayout page = new LinearLayout(this);   // the title, then the two lines; as the settings' first screen: 64 px above, 16 px inside
        page.setOrientation(LinearLayout.VERTICAL);
        page.setLayoutDirection(Dir.layout());
        page.setPadding(dp(8), dp(32), dp(8), dp(16));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(0, 0, 0, dp(16));
        TextView title = new TextView(this);
        title.setText(Words.of("تلفزيونك"));
        title.setTextColor(look.text);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 44);
        title.setTypeface(font("ThmanyahDisplay-Bold"));
        head.addView(title);
        page.addView(head);

        final List<View> cards = new ArrayList<>();
        LinearLayout line = null;
        for (int i = 0; i < WINDOWS.length; i++) {
            if (i % 2 == 0) {
                line = new LinearLayout(this);
                line.setOrientation(LinearLayout.HORIZONTAL);
                line.setLayoutDirection(Dir.layout());
                page.addView(line, new LinearLayout.LayoutParams(-1, dp(188)));
                ((LinearLayout.LayoutParams) line.getLayoutParams()).bottomMargin = dp(12);
            }
            View c = card(WINDOWS[i]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
            if (i % 2 == 0) lp.setMarginEnd(dp(12));
            line.addView(c, lp);
            cards.add(c);
        }
        // explicit ways: two lines of two, nothing wraps; index 0 is the start side (the right one in Arabic)
        final boolean rtl = Dir.rtl();
        for (int i = 0; i < cards.size(); i++) {
            View c = cards.get(i);
            int col = i % 2, row = i / 2;
            int other = i - col + (1 - col);
            c.setNextFocusUpId(cards.get(row == 0 ? i : i - 2).getId());
            c.setNextFocusDownId(cards.get(row == 1 || i + 2 >= cards.size() ? i : i + 2).getId());
            int toEnd = col == 0 ? other : i, toStart = col == 1 ? other : i;
            c.setNextFocusLeftId(cards.get(rtl ? toEnd : toStart).getId());
            c.setNextFocusRightId(cards.get(rtl ? toStart : toEnd).getId());
        }
        column.addView(page, new LinearLayout.LayoutParams(width, 0, 1f));
        ((LinearLayout.LayoutParams) page.getLayoutParams()).gravity = Gravity.CENTER_HORIZONTAL;
        TextView hint = new TextView(this);
        hint.setText(Words.of("الأسهم بين الأقسام  •  اختيار لفتح القسم  •  رجوع للخروج"));
        hint.setTextColor(look.sub);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        hint.setTypeface(font("Thmanyah-Regular"));
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(dp(72), dp(10), dp(72), dp(8));
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(-1, -2);
        hintLp.bottomMargin = Math.round(getResources().getDisplayMetrics().heightPixels * 0.05f);   // the 5 % safe area, as the settings' hint
        column.addView(hint, hintLp);
        root.addView(column, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        cards.get(0).requestFocus();
    }

    /** One window: its mark at the start, its name and its line of now; the glass tile, the focus fill and ink of the settings' buttons */
    private View card(final String[] w) {
        final LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setLayoutDirection(Dir.layout());
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(24), dp(20), dp(28), dp(20));
        card.setFocusable(true);
        card.setClickable(true);
        card.setId(View.generateViewId());
        FrameLayout mark = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor(android.graphics.Color.parseColor(w[3]));
        mark.setBackground(bg);
        ImageView glyph = new ImageView(this);
        glyph.setImageDrawable(getDrawable(getResources().getIdentifier(w[2], "drawable", getPackageName())).mutate());
        glyph.getDrawable().setTint(0xFFFFFFFF);
        mark.addView(glyph, new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.CENTER));
        card.addView(mark, new LinearLayout.LayoutParams(dp(96), dp(96)));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView n = new TextView(this);
        n.setText(Words.of(w[1]));
        n.setTextColor(look.text);
        n.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        n.setTypeface(font("Thmanyah-Medium"));
        n.setSingleLine(true);
        texts.addView(n);
        TextView v = new TextView(this);
        v.setText(Words.of(live(w[0])));
        if ("matches".equals(w[0])) nextMatchLine(v);
        v.setTextColor(look.sub);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        v.setTypeface(font("Thmanyah-Regular"));
        v.setMaxLines(2);
        v.setEllipsize(android.text.TextUtils.TruncateAt.END);
        v.setTextDirection(Dir.text());
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-2, -2);
        vp.topMargin = dp(6);
        texts.addView(v, vp);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
        tp.setMarginStart(dp(24));
        card.addView(texts, tp);
        card.setContentDescription(Words.of(w[1]));
        card.setOnClickListener(x -> open(w[0]));
        GlassPaint.card(card, look, 14);
        return card;
    }

    /** «المباريات»: the real next match of the followed teams when it is known (NextMatch: cached 30 min, fetched in the background);
     *  until then, or when nothing is known, the generic line stays */
    private void nextMatchLine(final TextView v) {
        String now = NextMatch.cached(this, prefs, () -> runOnUiThread(() -> {
            String l = NextMatch.cached(this, prefs, null);
            if (l != null && !isFinishing()) v.setText(l);
        }));
        if (now != null) v.setText(now);
    }

    /** the one line of what is true now: the market's state (MarketHours), what the matches show, «قريبًا», the look */
    private String live(String key) {
        switch (key) {
            case "markets": return MarketHours.open(System.currentTimeMillis()) ? "السوق مفتوح" : "السوق مغلق";
            case "matches": return prefs.teams().isEmpty() ? "اختر فرقك" : "فرقك أولًا";
            case "games": return GAMES_READY ? "ألعاب العائلة" : "قريبًا";
            default: return Prefs.lookLabel(prefs.look());
        }
    }

    private void open(String key) {
        try {
            switch (key) {
                case "markets": startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.MarketsActivity")); break;
                case "matches": startActivity(new Intent().setClassName("sa.urtv.home", "sa.urtv.home.MatchesActivity")); break;
                case "games":
                    if (GAMES_READY) break;   // the games' own screen goes here when it lands
                    Toast.makeText(this, Words.of("قريبًا"), Toast.LENGTH_SHORT).show();
                    break;
                default: startActivity(new Intent(this, SettingsActivity.class));
            }
        } catch (RuntimeException e) {   // the home app missing or older: the window says so, the hub stays
            Toast.makeText(this, Words.of("تعذّر الفتح"), Toast.LENGTH_SHORT).show();
        }
    }

    private Typeface font(String name) {
        Typeface tf = fonts.get(name);
        if (tf == null) {
            try { tf = Typeface.createFromAsset(getAssets(), "fonts/" + name + ".otf"); }
            catch (RuntimeException e) { tf = Typeface.DEFAULT; }
            fonts.put(name, tf);
        }
        return tf;
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
