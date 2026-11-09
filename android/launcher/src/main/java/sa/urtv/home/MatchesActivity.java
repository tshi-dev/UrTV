package sa.urtv.home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Collections;

/**
 * «المباريات» (docs/PROPOSALS-NOV2.md §1): the page of the games, opened with OK on the football home's games (on the chosen one's card) and
 * from the settings («المزيد ← مباريات اليوم»). It is the screensaver's page in its #matchpage=1 mode, on the light look: the days, the
 * leagues with «فرقك» first, a game's card with its venue, its channel and its place among the prayers. The page has the focus and the keys;
 * Back closes a game's card before it leaves. Two things it asks of the app (window.UrtvMatches):
 *  - remind: «ذكّرني قبلها بنصف ساعة» adds a task (the screensaver's «المهام») with the game and its time, and a reminder: due at kick-off,
 *    «قبلها بـ» 30, silent (MatchReminder, as `saver.sh tasks add '<text>' --at '…' --before 30` does).
 *  - follow: «تابع» adds the club to the followed teams (the screensaver's provider, phone_follow: the same list as «الفرق المتابعة»).
 *
 * <p>A second WebView of this app beside HomeCard's: pauseTimers()/resumeTimers() are process-wide, so this one resumes them in onResume and
 * pauses them in onPause, as HomeCard does; the home and this page are never both in front (tests/home_card_test.py).
 */
public class MatchesActivity extends Activity {
    static final String EXTRA_EVENT = "event";
    private static final Uri CONFIG = Uri.parse("content://sa.urtv.screensaver.config");
    private static final Uri TASKS = Uri.parse("content://sa.urtv.screensaver.config/tasks");
    private static final String FONTS = "https://raw.githubusercontent.com/tshi-dev/UrTV/master/assets/fonts/";
    private WebView web;

    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ConfigSnapshot cfg = ConfigSnapshot.read(this);
        final int ground = cfg.pagesDark ? Color.rgb(0x17, 0x18, 0x1B) : Color.rgb(0xF4, 0xF2, 0xEC);   // the page's own ground (dark, or sand), before its first frame
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(ground));   // and the window's, so no other colour shows while the view is not yet drawn
        web = new WebView(this);
        web.setBackgroundColor(ground);
        web.setAlpha(0f);   // not shown (alpha, not INVISIBLE: a view that is not drawn gets no visual-state callback) until the page has painted its first frame
        final Runnable reveal = () -> { if (web != null) web.setAlpha(1f); };
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);   // the leagues' files and the crests the home already keeps
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        s.setTextZoom(100);   // as HomeCard: the TV's font size must not push the page out of its boxes
        web.addJavascriptInterface(new Bridge(getApplicationContext(), this), "UrtvMatches");
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String u) {
                if (android.os.Build.VERSION.SDK_INT >= 23) v.postVisualStateCallback(1, new WebView.VisualStateCallback() {
                    @Override public void onComplete(long id) { reveal.run(); }
                });
                else reveal.run();
            }

            @Override public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail detail) {
                if (v == web) web = null;
                v.destroy();
                finish();   // the renderer died: back to the home, never a blank page
                return true;
            }

            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {   // the fonts from the APK, as HomeCard
                String u = r.getUrl().toString();
                if (u.startsWith(FONTS)) {
                    String name = u.substring(FONTS.length());
                    if (name.matches("[A-Za-z0-9_-]+\\.woff2")) {
                        try {
                            return new WebResourceResponse("font/woff2", null, 200, "OK",
                                    Collections.singletonMap("Access-Control-Allow-Origin", "*"), getAssets().open("fonts/" + name));
                        } catch (java.io.IOException ignored) { }
                    }
                }
                return super.shouldInterceptRequest(v, r);
            }
        });
        setContentView(web);
        web.postDelayed(reveal, 1500);   // a page that never finishes still shows what it has
        String event = getIntent().getStringExtra(EXTRA_EVENT);
        web.loadUrl(HomePage.fresh(this, HomeCardChoice.matchesUrl(cfg.fragment, event) + (cfg.pagesDark ? "&pagedark=1" : "")));   // «صفحات الأسهم والمباريات داكنة»: the page in its dark theme
        web.requestFocus();
    }

    @Override protected void onResume() {
        super.onResume();
        if (web != null) { web.onResume(); web.resumeTimers(); }
    }

    @Override protected void onPause() {
        super.onPause();
        if (web != null) { web.onPause(); web.pauseTimers(); }
    }

    @Override protected void onDestroy() {
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }

    /** Back: a game's card closes first (the page answers "closed"); with none open, the page is left. */
    @Override public void onBackPressed() {
        if (web == null) { super.onBackPressed(); return; }
        web.evaluateJavascript("typeof mpBack==='function'?mpBack():'out'", v -> {
            if (!"closed".equals(FootballFocus.unquote(v))) finish();
        });
    }

    /** What the page may ask of the app: nothing else is reachable from it. */
    static final class Bridge {
        private final Context c;
        /** «شاهد المباراة» starts the Player from this activity, for a result (MatchApp's contract); held weakly, the page may outlive it */
        private final java.lang.ref.WeakReference<android.app.Activity> act;
        Bridge(Context c, android.app.Activity a) { this.c = c; this.act = new java.lang.ref.WeakReference<>(a); }

        @JavascriptInterface public boolean remind(String text, double at) { return MatchReminder.add(c, text, (long) at); }

        /** the first of these packages (comma separated) that is installed and can be opened, else "" */
        @JavascriptInterface public String appOf(String csv) { return installedOf(c.getPackageManager(), csv); }

        /** opens the app (its TV launcher entry, else its launcher entry): no deep link is known for the match, so the app itself opens */
        @JavascriptInterface public boolean open(String csv) {
            String pkg = installedOf(c.getPackageManager(), csv);
            if (pkg.isEmpty()) return false;
            android.content.Intent i = c.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            if (i == null) i = c.getPackageManager().getLaunchIntentForPackage(pkg);
            try { c.startActivity(i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)); return true; } catch (RuntimeException e) { return false; }
        }

        /** 4.3 «شاهد المباراة»: true when «تطبيق المباريات» names an app that is there now (UrTV Player only when it is ours) */
        @JavascriptInterface public boolean matchApp() { return MatchApp.chosen(c, ConfigSnapshot.read(c).matchApp) != null; }

        /** «شاهد المباراة»: the game's facts (MatchFacts checks them) to the chosen app; false when refused or nothing opened */
        @JavascriptInterface public boolean findMatch(String json) {
            MatchFacts f;
            try { f = MatchFacts.of(sa.urtv.home.phone.Json.parseObject(json == null ? "" : json), System.currentTimeMillis()); }
            catch (sa.urtv.home.phone.Json.Bad e) { return false; }
            final android.app.Activity a = act.get();
            final String setting = ConfigSnapshot.read(c).matchApp;
            if (f == null || a == null || a.isFinishing() || MatchApp.chosen(c, setting) == null) return false;
            final MatchFacts facts = f;
            a.runOnUiThread(new Runnable() { public void run() { if (!a.isFinishing()) MatchApp.open(a, setting, facts); } });   // the bridge's thread is not the UI's
            return true;
        }

        @JavascriptInterface public boolean unfollow(String id) {
            if (id == null || !id.matches("\\d{1,9}")) return false;
            try {
                Bundle out = c.getContentResolver().call(CONFIG, "phone_unfollow", id, null);
                return out != null && out.getBoolean("ok");
            } catch (RuntimeException e) {
                return false;
            }
        }

        @JavascriptInterface public boolean follow(String id, String name) {
            if (id == null || !id.matches("\\d{1,9}")) return false;
            try {
                Bundle out = c.getContentResolver().call(CONFIG, "phone_follow", id + ":" + (name == null ? "" : name.replace(":", " ").trim()), null);
                return out != null && out.getBoolean("ok");
            } catch (RuntimeException e) {   // the screensaver is missing or older: nothing changed
                return false;
            }
        }
    }

    /** the bridge's other asks: «أزل … من فرقي», and the broadcaster's app («شاهد على …») */
    static String installedOf(android.content.pm.PackageManager pm, String csv) {
        if (csv == null) return "";
        for (String pkg : csv.split(",")) {
            pkg = pkg.trim();
            if (!pkg.matches("[A-Za-z0-9_.]{3,120}")) continue;
            if (pm.getLeanbackLaunchIntentForPackage(pkg) != null || pm.getLaunchIntentForPackage(pkg) != null) return pkg;
        }
        return "";
    }

    /** «ذكّرني قبلها»: a task with the game and its time, its reminder at kick-off with «قبلها بـ» 30 and no sound (ReminderPlan.forMatch);
     *  an older screensaver app with no reminders keeps the plain task. */
    static final class MatchReminder {
        private MatchReminder() { }

        static boolean add(Context c, String text, long at) {
            String t = text == null ? "" : text.trim();
            long[] r = ReminderPlan.forMatch(at);
            if (t.isEmpty() || t.length() > 120 || r == null) return false;
            ContentValues v = new ContentValues();
            v.put("t", t);
            v.put("due", r[0]);
            v.put("bf", (int) r[1]);
            v.put("rp", (int) r[2]);
            v.put("snd", (int) r[3]);
            v.put("wh", (int) r[4]);
            try {
                boolean ok = c.getContentResolver().insert(TASKS, v) != null;
                if (ok) ReminderScheduler.schedule(c);   // the home's card and the alarm, as a task with a time from the phone
                return ok;
            } catch (RuntimeException e) {
                return false;
            }
        }
    }
}
