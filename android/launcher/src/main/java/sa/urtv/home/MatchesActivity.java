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
 *  - remind: «ذكّرني قبلها بربع ساعة» adds a task (the screensaver's «المهام», as `saver.sh tasks add` does) with the game and its time.
 *    MatchReminder is the one place to change when reminders of their own come (the hook stays: text and the moment to remind).
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
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(0xF4, 0xF2, 0xEC));   // the page's sand, before its first frame
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
        web.addJavascriptInterface(new Bridge(getApplicationContext()), "UrtvMatches");
        web.setWebViewClient(new WebViewClient() {
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
        ConfigSnapshot cfg = ConfigSnapshot.read(this);
        String event = getIntent().getStringExtra(EXTRA_EVENT);
        web.loadUrl(HomePage.fresh(this, HomeCardChoice.matchesUrl(cfg.fragment, event)));
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
        Bridge(Context c) { this.c = c; }

        @JavascriptInterface public boolean remind(String text, double at) { return MatchReminder.add(c, text, (long) at); }

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

    /** «ذكّرني قبلها»: today a task with the game and its time (the tasks of «المهام»); the place a reminder of its own will take. */
    static final class MatchReminder {
        private MatchReminder() { }

        static boolean add(Context c, String text, long at) {
            String t = text == null ? "" : text.trim();
            if (t.isEmpty() || t.length() > 120) return false;
            ContentValues v = new ContentValues();
            v.put("t", t);
            try {
                return c.getContentResolver().insert(TASKS, v) != null;
            } catch (RuntimeException e) {
                return false;
            }
        }
    }
}
