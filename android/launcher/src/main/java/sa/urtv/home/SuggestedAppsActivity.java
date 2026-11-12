package sa.urtv.home;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import sa.urtv.home.phone.UpdateRules;

/**
 * 4.3 «تطبيقات مقترحة», opened from the screensaver's settings (الإعدادات ← المزيد ← التطبيقات والمفضلة): each suggested app (SuggestedApps) with its
 * one line, and one button. Installed: «افتح». A Play Store app: «افتح المتجر» (its page; the owner installs it there). Otherwise «ثبّت»: the file
 * is downloaded into this app's cache from its own GitHub releases only (SuggestedApps), at most UpdateRules.MAX_APK_BYTES, over https; the file must
 * be that package; then Android's own install screen asks the owner (PackageInstaller). Back closes.
 */
public class SuggestedAppsActivity extends Activity {
    private static final String TAG = "UrTVSuggested", RESULT = "sa.urtv.home.SUGGESTED_RESULT";

    /** «اللغة» (docs/ENGLISH.md): with Arabic the context itself, with English an English one */
    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    /** SuggestedApps' why lines, marked for «اللغة» here (the catalog is plain Java); S.tv says them */
    static final String[] WHY = {
        S.n("يوتيوب بلا إعلانات، مصمَّم للريموت"), S.n("متصفح إنترنت يشتغل بالريموت"), S.n("يشغّل الفيديو والصوت من ذاكرة USB ومن الشبكة"),
        S.n("مشغّل تلفزيونك للفيديو والقوائم، من إصدارات تلفزيونك نفسها"),
    };

    private GlassStyle.Look look;
    private final Map<String, TextView> buttons = new HashMap<>();
    private volatile boolean busy;

    private final BroadcastReceiver result = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            int status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, Integer.MIN_VALUE);
            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                Intent screen = i.getParcelableExtra(Intent.EXTRA_INTENT);
                if (screen != null) {
                    try { startActivity(screen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (RuntimeException e) { Log.w(TAG, "confirm: " + e.getClass().getSimpleName()); }
                }
                return;
            }
            busy = false;
            refresh();
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Digits.init(this);
        ScrollView scroll = new ScrollView(this);
        look = GlassSource.page(this, scroll);
        FrameLayout frame = new FrameLayout(this);
        scroll.addView(frame);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutDirection(Dir.layout());
        col.setPadding(0, dp(48), 0, dp(48));
        // 1000 dp at most, and always inside the 5 % safe area of the screen: a 960 dp wide TV is narrower than the column was (the tiles touched both edges)
        int wide = Math.min(dp(1000), Math.round(getResources().getDisplayMetrics().widthPixels * 0.9f));
        frame.addView(col, new FrameLayout.LayoutParams(wide, -2, Gravity.CENTER_HORIZONTAL));
        col.addView(text(S.t("تطبيقات مقترحة"), 30, "Medium", look.text), lp(dp(10)));
        col.addView(text(S.t("تطبيقات نرشّحها لتلفزيونك. كل تطبيق من مصدره هو، ويسألك أندرويد قبل التثبيت."), 16, "Regular", look.sub), lp(dp(24)));
        View first = null;
        // design/43: tiles in two columns, each the app's icon (its own once installed, else a coloured initial), its name, one line, and its state; the whole tile is the button
        final java.util.List<SuggestedApps.App> shown = new java.util.ArrayList<>();
        for (SuggestedApps.App a : SuggestedApps.forTv(Build.VERSION.SDK_INT)) {
            // a MIRROR app (UrTV Player) gets a tile only once a release of it is really there: never a tile that cannot install
            if (a.kind == SuggestedApps.Kind.MIRROR && !SuggestedApps.mirrorShown(mirrorAnswer(a), System.currentTimeMillis())) continue;
            shown.add(a);
        }
        for (int i = 0; i < shown.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutDirection(Dir.layout());
            for (int j = 0; j < 2; j++) {
                if (i + j >= shown.size()) { row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f)); continue; }
                final SuggestedApps.App a = shown.get(i + j);
                LinearLayout tile = new LinearLayout(this);
                tile.setOrientation(LinearLayout.VERTICAL);
                tile.setLayoutDirection(Dir.layout());
                tile.setPadding(dp(16), dp(16), dp(16), dp(16));
                tile.setFocusable(true);
                tile.setClickable(true);
                tile.setOnClickListener(v -> act(a));
                LinearLayout head = new LinearLayout(this);
                head.setLayoutDirection(Dir.layout());
                head.setGravity(Gravity.CENTER_VERTICAL);
                head.addView(iconFor(a), new LinearLayout.LayoutParams(dp(36), dp(36)));
                TextView name = text(a.name, 20, "Medium", look.text);
                LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, -2, 1f);
                np.setMarginStart(dp(12));
                head.addView(name, np);
                tile.addView(head, lp(dp(8)));
                tile.addView(text(S.tv(a.why), 15, "Regular", look.sub), lp(dp(12)));
                TextView state = text("", 17, "Medium", look.accent);
                tile.addView(state, lp(0));
                GlassPaint.card(tile, look, 14);
                LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
                tp.setMargins(dp(8), dp(8), dp(8), dp(8));
                row.addView(tile, tp);
                buttons.put(a.id, state);
                if (first == null) first = tile;
            }
            col.addView(row, lp(0));
        }
        setContentView(scroll);
        checkMirror();   // the Player's tile and its «تحديث متوفر»: the cached answer now, a fresh one for next time
        InternalReceivers.register(this, result, new IntentFilter(RESULT));
        refresh();
        if (first != null) { final View f = first; f.post(f::requestFocus); }
    }

    @Override protected void onResume() { super.onResume(); if (!busy) refresh(); }

    @Override protected void onDestroy() {
        try { unregisterReceiver(result); } catch (RuntimeException ignored) { }
        super.onDestroy();
    }

    private void refresh() {
        for (SuggestedApps.App a : SuggestedApps.all()) {
            TextView b = buttons.get(a.id);
            if (b == null) continue;
            boolean on = installed(a.pkg);
            if (on && a == SuggestedApps.PLAYER_APP && MirrorInstall.known(this) != null) { b.setText(S.t("تحديث متوفر")); continue; }   // a newer Player is published
            b.setText(on ? "✓ " + S.t("مثبّت") : a.kind == SuggestedApps.Kind.STORE ? S.t("افتح المتجر") : S.t("ثبّت"));
        }
    }

    private void act(SuggestedApps.App a) {
        boolean update = a == SuggestedApps.PLAYER_APP && installed(a.pkg) && MirrorInstall.known(this) != null;   // installed over, its data kept
        if (installed(a.pkg) && !update) { open(a.pkg); return; }
        if (a.kind == SuggestedApps.Kind.STORE) { store(a.pkg); return; }
        if (busy) return;
        busy = true;
        final TextView b = buttons.get(a.id);
        if (b != null) b.setText(S.t("ينزّل…"));
        new Thread(() -> {
            String why = install(a);
            if (why != null) runOnUiThread(() -> { busy = false; if (b != null) b.setText(why); });
        }, "suggested-install").start();
    }

    /** null when Android's install screen was asked for; else the words for the button (it did not go) */
    private String install(SuggestedApps.App a) {
        File f = new File(getCacheDir(), "suggested.apk");
        try {
            if (a.kind == SuggestedApps.Kind.MIRROR) return MirrorInstall.mirror(this, a, f, RESULT, 0);
            String json = MirrorInstall.fetch(a.address(), 2 * 1024 * 1024);
            String url = json == null ? null : SuggestedApps.pickAsset(a.file, String.join(",", Build.SUPPORTED_ABIS), json);
            if (url == null || !SuggestedApps.githubOk(a, url)) return S.t("ما لقينا نسخة تناسب هذا التلفزيون");
            String sha = MirrorInstall.download(url, f);
            if (sha == null) return S.t("ما قدرنا ننزّله. جرّب مرة ثانية");
            PackageInfo info = getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(), 0);
            if (info == null || !a.pkg.equals(info.packageName)) return S.t("الملف ليس هذا التطبيق، فما ثبّتناه");
            MirrorInstall.commit(this, f, a.pkg, RESULT);
            return null;
        } catch (Exception e) {
            Log.w(TAG, "install: " + e.getClass().getSimpleName());
            return S.t("ما قدرنا نثبّته. جرّب مرة ثانية");
        }
    }

    /** the last answer about a MIRROR app's releases: "1|<time>" a valid release was there, "0|<time>" none; "" never asked */
    private String mirrorAnswer(SuggestedApps.App a) {
        return getSharedPreferences("suggested", MODE_PRIVATE).getString("mirror_" + a.id, "");
    }

    /**
     * Asks GitHub (in the background, MirrorInstall.refreshAsync) whether a valid Player release is there and which version it is, at most every
     * 6 hours; the screen is drawn again when it answered (the tile appears, or says «تحديث متوفر»).
     */
    private void checkMirror() {
        MirrorInstall.refreshAsync(this, () -> runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) recreate(); }));
    }

    private boolean installed(String pkg) {
        try { getPackageManager().getPackageInfo(pkg, 0); return true; } catch (PackageManager.NameNotFoundException e) { return false; }
    }

    private void open(String pkg) {
        PackageManager pm = getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) return;
        try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (RuntimeException ignored) { }
    }

    /** the app's Play Store page: the store app, else its web page */
    private void store(String pkg) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException e) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
            catch (RuntimeException ignored) { }
        }
    }

    /** the app's own icon when it is on the TV, else a square in a colour of its own with its first letter */
    private android.view.View iconFor(SuggestedApps.App a) {
        try {
            android.widget.ImageView v = new android.widget.ImageView(this);
            v.setImageDrawable(getPackageManager().getApplicationIcon(a.pkg));
            return v;
        } catch (PackageManager.NameNotFoundException e) {
            if (SuggestedApps.PLAYER.equals(a.pkg)) {   // not installed yet: the Player's own icon, bundled (copied from its public repository)
                android.widget.ImageView v = new android.widget.ImageView(this);
                v.setImageDrawable(new android.graphics.drawable.AdaptiveIconDrawable(getDrawable(R.drawable.player_icon_bg), getDrawable(R.drawable.player_icon_fg)));
                return v;
            }
            TextView t = text(a.name.substring(0, 1), 18, "Medium", 0xFFFFFFFF);
            t.setGravity(Gravity.CENTER);
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setColor(0xFF000000 | (Math.abs(a.id.hashCode()) % 0x808080) + 0x303030);
            g.setCornerRadius(dp(8));
            t.setBackground(g);
            return t;
        }
    }

    private TextView text(String s, float sp, String weight, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
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
