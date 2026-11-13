package sa.urtv.screensaver;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 4.3.1 «اختر دورياتك»: the leagues of the matches and the cards on one screen, no nested pages. The search field on top, then the sections «البطولات الكبرى» (the switch, and its
 * tournaments as ticks the person can untick one by one), «السعودية», «أوروبا», «آسيا وأفريقيا», «العالم»; each row is a tick and the Arabic name; the count of the chosen leagues
 * (up to Prefs.MAX_LEAGUES) is in the header. Without a search a section shows the built-in leagues and the catalogue's best-known ones (top), with a search all of the catalogue.
 * The same settings as before (Prefs.leagues, Prefs.tournaments, Prefs.tournamentsOff); the big tournaments' list is assets/shared/tournaments.json.
 */
public class LeaguesActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        android.content.Context en = S.wrap(base);
        if (S.en()) { super.attachBaseContext(en); return; }
        android.content.res.Configuration c = new android.content.res.Configuration(base.getResources().getConfiguration());
        c.setLocale(new java.util.Locale("ar"));
        c.setLayoutDirection(new java.util.Locale("ar"));
        super.attachBaseContext(base.createConfigurationContext(c));
    }

    /** the sections' titles, in LeagueGroups' order (marked here for «اللغة»; S.tv says them) */
    private static final String[] TITLES = {S.n("السعودية"), S.n("أوروبا"), S.n("آسيا وأفريقيا"), S.n("العالم")};

    private Prefs prefs;
    private FrameLayout root;
    private LinearLayout col, list;
    private GlassStyle.Look look = GlassStyle.DARK;
    private final Map<String, Typeface> fonts = new HashMap<>();
    private List<String[]> tournaments = new ArrayList<>();   // {id, ar, en}
    private List<String[]> catalogue = new ArrayList<>();     // {id, en, ar, country, country_ar, sport, top}
    private String query = "";
    private TextView countView;
    private EditText search;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setWindowAnimations(0);
        prefs = new Prefs(this);
        tournaments = readTournaments();
        java.io.File cache = new java.io.File(getCacheDir(), "league-catalogue.json");
        if (cache.isFile()) catalogue = SettingsActivity.catalogueRows(SettingsActivity.readFile(cache));
        root = new FrameLayout(this);
        root.setLayoutDirection(Dir.layout());
        look = GlassSource.page(this, root);
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutDirection(Dir.layout());
        col.setPadding(dp(16), dp(16), dp(16), dp(16));
        scroll.addView(col, new FrameLayout.LayoutParams(-1, -2));
        int safe = Math.round(getResources().getDisplayMetrics().heightPixels * 0.05f);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.min(getResources().getDisplayMetrics().widthPixels - dp(96), dp(1100)), -1);
        lp.topMargin = safe;
        lp.bottomMargin = safe;
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(scroll, lp);
        setContentView(root);
        col.addView(text(S.t("اختر دورياتك"), 28, "Medium", look.text), line(dp(4)));
        countView = text("", 16, "Regular", look.sub);
        col.addView(countView, line(dp(12)));
        search = new EditText(this);
        search.setHint(S.t("ابحث عن دوري أو بطولة"));
        search.setHintTextColor(look.sub);
        search.setTextColor(look.text);
        search.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        search.setTypeface(font("Regular"));
        search.setSingleLine(true);
        search.setImeOptions(EditorInfo.IME_ACTION_DONE);
        search.setPadding(dp(16), dp(12), dp(16), dp(12));
        search.setTextDirection(Dir.text());
        GlassPaint.card(search, look, 14);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b2, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b2, int c) { }
            @Override public void afterTextChanged(Editable s) { query = s.toString(); fill(); }
        });
        col.addView(search, line(dp(12)));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutDirection(Dir.layout());
        col.addView(list, line(dp(0)));
        View back = button(S.t("رجوع"), v -> finish());
        LinearLayout.LayoutParams bl = line(0);
        bl.topMargin = dp(12);
        col.addView(back, bl);
        fill();
        refreshCatalogue(cache);
    }

    /** the catalogue quietly refreshed when the kept copy is old or missing (the picker works from the kept copy meanwhile) */
    private void refreshCatalogue(final java.io.File cache) {
        if (!catalogue.isEmpty() && System.currentTimeMillis() - cache.lastModified() < CatalogueLeagues.FRESH_MS) return;
        new Thread(() -> {
            final List<String[]> got = SettingsActivity.catalogueRows(SettingsActivity.fetchCatalogue(cache));
            runOnUiThread(() -> { if (!isFinishing() && !got.isEmpty()) { catalogue = got; fill(); } });
        }, "catalogue").start();
    }

    private List<String[]> readTournaments() {
        List<String[]> out = new ArrayList<>();
        try (InputStream in = getAssets().open("shared/tournaments.json")) {
            JSONArray a = new JSONObject(new String(Bounded.readAll(in, Bounded.JSON_MAX), "UTF-8")).getJSONArray("tournaments");
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                if (o.optString("id").matches("\\d{1,6}")) out.add(new String[] {o.getString("id"), o.optString("ar"), o.optString("en")});
            }
        } catch (Exception ignored) { }
        return out;
    }

    // ---- the rows ----

    private String name(String ar, String en) { return S.en() ? (en.isEmpty() ? ar : en) : (ar.isEmpty() ? en : ar); }

    private boolean chosen(String stored) {
        for (String s : prefs.leagues()) if (CatalogueLeagues.same(s, stored)) return true;
        return false;
    }

    private void toggleLeague(String stored) {
        List<String> v = new ArrayList<>(prefs.leagues());
        boolean had = false;
        for (int i = v.size() - 1; i >= 0; i--) if (CatalogueLeagues.same(v.get(i), stored)) { v.remove(i); had = true; }
        if (!had) {
            if (v.size() >= Prefs.MAX_LEAGUES) { toast(S.t("حتى %s دوريات", String.valueOf(Prefs.MAX_LEAGUES))); return; }
            v.add(stored);
        }
        prefs.setList("leagues", v);
        fill();
    }

    private void fill() {
        View focused = getCurrentFocus();
        Object want = focused == null ? null : focused.getTag();
        list.removeAllViews();
        List<String> off = prefs.tournamentsOff();
        List<String> ids = new ArrayList<>();
        for (String[] t : tournaments) ids.add(t[0]);
        boolean searching = query.trim().length() >= 2;
        // the big tournaments: the switch, then each one
        list.addView(section(S.t("البطولات الكبرى")), line(dp(8)));
        final boolean on = prefs.tournaments();
        list.addView(row("tour:all", on, S.t("كل البطولات الكبرى"), S.t("%s من %s", String.valueOf(LeagueGroups.onCount(ids, off)), String.valueOf(ids.size())), v -> { prefs.setTournaments(!prefs.tournaments()); fill(); }), line(dp(8)));
        for (final String[] t : tournaments) {
            if (!LeagueGroups.matches(query, t[1], t[2])) continue;
            final boolean ticked = !off.contains(t[0]);
            list.addView(row("tour:" + t[0], on && ticked, name(t[1], t[2]), "", v -> {
                List<String> now = new ArrayList<>(prefs.tournamentsOff());
                if (!now.remove(t[0])) now.add(t[0]);
                prefs.setTournamentsOff(now);
                fill();
            }), line(dp(8)));
        }
        // the four sections: the built-in leagues, then the catalogue's
        List<List<View>> sections = new ArrayList<>();
        for (int i = 0; i < 4; i++) sections.add(new ArrayList<View>());
        for (final String[] l : Prefs.LEAGUES) {
            if (!LeagueGroups.matches(query, l[1], l[0])) continue;
            sections.get(LeagueGroups.ofBuiltIn(l[0])).add(row("lg:" + l[0], chosen(l[0]), l[1], "", v -> toggleLeague(l[0])));
        }
        for (final String[] r : catalogue) {   // {id, en, ar, country, country_ar, sport, top}
            if (!"Soccer".equals(r[5]) && !r[5].isEmpty()) continue;
            if (ids.contains(r[0]) || builtInHas(r[1]) || (!searching && r[6].isEmpty())) continue;
            if (searching && !LeagueGroups.matches(query, r[2], r[1], r[4], r[3])) continue;
            final String stored = CatalogueLeagues.entry(r[0], r[2], r[1]);
            if (stored == null) continue;
            String shown = name(r[2], r[1]);
            sections.get(LeagueGroups.ofCountry(r[3])).add(row("lg:id" + r[0], chosen("id" + r[0]), shown, S.en() || r[4].isEmpty() ? r[3] : r[4], v -> toggleLeague(stored)));
        }
        for (int i = 0; i < 4; i++) {
            if (sections.get(i).isEmpty()) continue;
            list.addView(section(S.tv(TITLES[i])), line(dp(8)));
            for (View v : sections.get(i)) list.addView(v, line(dp(8)));
        }
        if (searching && list.getChildCount() == 0) list.addView(text(S.t("لا توجد نتائج"), 18, "Regular", look.sub), line(dp(8)));
        countView.setText(S.t("اخترت %s من %s", String.valueOf(prefs.leagues().size()), String.valueOf(Prefs.MAX_LEAGUES)));
        if (want != null && !(want instanceof String && ((String) want).isEmpty())) { View again = list.findViewWithTag(want); if (again != null) again.requestFocus(); }
    }

    private boolean builtInHas(String en) {
        for (String[] l : Prefs.LEAGUES) if (l[0].equalsIgnoreCase(en)) return true;
        return false;
    }

    private View section(String title) {
        TextView t = text(title, 20, "Medium", look.accent);
        t.setPadding(dp(4), dp(12), dp(4), dp(0));
        return t;
    }

    /** a tick, the name, and a small plain note (the country, or the count) */
    private View row(String tag, boolean on, String name, String note, View.OnClickListener click) {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setLayoutDirection(Dir.layout());
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setPadding(dp(16), dp(10), dp(16), dp(10));
        TextView tick = text(on ? "✓" : "", 22, "Medium", on ? look.base : look.text);
        tick.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(8));
        g.setStroke(dp(3), look.sub);
        if (on) g.setColor(look.accent);
        tick.setBackground(g);
        v.addView(tick, new LinearLayout.LayoutParams(dp(36), dp(36)));
        TextView n = text(name, 20, "Medium", look.text);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1f);
        np.setMarginStart(dp(16));
        v.addView(n, np);
        if (!note.isEmpty()) {
            TextView s = text(note, 16, "Regular", look.sub);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
            sp.setMarginStart(dp(16));
            v.addView(s, sp);
        }
        v.setFocusable(true);
        v.setClickable(true);
        v.setTag(tag);
        v.setOnClickListener(click);
        v.setContentDescription(name);
        GlassPaint.card(v, look, 14);
        return v;
    }

    // ---- small things ----

    private String conv(String s) { return DigitsText.convert(s, "latin".equals(prefs.digits())); }

    private TextView text(String s, int sp, String fontName, int color) {
        TextView t = new TextView(this);
        t.setText(conv(s));
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTypeface(font(fontName));
        t.setGravity(Gravity.START);
        t.setTextDirection(Dir.text());
        return t;
    }

    private View button(String label, View.OnClickListener click) {
        TextView t = text(label, 18, "Medium", look.text);
        t.setPadding(dp(24), dp(14), dp(24), dp(14));
        t.setFocusable(true);
        t.setClickable(true);
        t.setOnClickListener(click);
        GlassPaint.card(t, look, 14);
        return t;
    }

    private LinearLayout.LayoutParams line(int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = bottom;
        return p;
    }

    private void toast(String s) { Toast.makeText(this, conv(s), Toast.LENGTH_SHORT).show(); }

    private Typeface font(String name) {
        Typeface tf = fonts.get(name);
        if (tf == null) {
            try { tf = Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-" + name + ".otf"); }
            catch (RuntimeException e) { tf = Typeface.DEFAULT; }
            fonts.put(name, tf);
        }
        return tf;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
