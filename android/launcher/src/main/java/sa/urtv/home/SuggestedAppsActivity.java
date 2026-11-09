package sa.urtv.home;

import android.app.Activity;
import android.app.PendingIntent;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
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
        S.n("مشغّل IPTV فارغ من متجر Play: تضيف قائمتك أنت، وما فيه ولا نعطي أي قنوات"), S.n("مشغّل تلفزيونك للفيديو والقوائم، من إصدارات تلفزيونك نفسها"),
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
        frame.addView(col, new FrameLayout.LayoutParams(dp(1000), -2, Gravity.CENTER_HORIZONTAL));
        col.addView(text(S.t("تطبيقات مقترحة"), 30, "Medium", look.text), lp(dp(10)));
        col.addView(text(S.t("تطبيقات نرشّحها لتلفزيونك. كل تطبيق من مصدره هو، ويسألك أندرويد قبل التثبيت."), 16, "Regular", look.sub), lp(dp(24)));
        View first = null;
        // design/43: tiles in two columns, each the app's icon (its own once installed, else a coloured initial), its name, one line, and its state; the whole tile is the button
        final java.util.List<SuggestedApps.App> shown = new java.util.ArrayList<>();
        for (SuggestedApps.App a : SuggestedApps.forTv(Build.VERSION.SDK_INT)) {
            // a MIRROR app (UrTV Player) gets a tile only once a release of it is really there: never a tile that cannot install
            if (a.kind == SuggestedApps.Kind.MIRROR && !SuggestedApps.mirrorShown(mirrorAnswer(a), System.currentTimeMillis())) { checkMirror(a); continue; }
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
            b.setText(on ? "✓ " + S.t("مثبّت") : a.kind == SuggestedApps.Kind.STORE ? S.t("افتح المتجر") : S.t("ثبّت"));
        }
    }

    private void act(SuggestedApps.App a) {
        if (installed(a.pkg)) { open(a.pkg); return; }
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
            if (a.kind == SuggestedApps.Kind.MIRROR) return mirror(a, f);
            String json = fetch(a.address(), 2 * 1024 * 1024);
            String url = json == null ? null : SuggestedApps.pickAsset(a.file, String.join(",", Build.SUPPORTED_ABIS), json);
            if (url == null || !SuggestedApps.githubOk(a, url)) return S.t("ما لقينا نسخة تناسب هذا التلفزيون");
            String sha = download(url, f);
            if (sha == null) return S.t("ما قدرنا ننزّله. جرّب مرة ثانية");
            PackageInfo info = getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(), 0);
            if (info == null || !a.pkg.equals(info.packageName)) return S.t("الملف ليس هذا التطبيق، فما ثبّتناه");
            commit(f, a.pkg);
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

    /** Asks GitHub (in the background) whether a valid release is there, at most every SuggestedApps.MIRROR_CHECK_MS; shows the rows when it is. */
    private void checkMirror(final SuggestedApps.App a) {
        final long now = System.currentTimeMillis();
        if (!SuggestedApps.mirrorAskAgain(mirrorAnswer(a), now)) return;
        new Thread(() -> {
            String json = null;
            try { json = fetch(a.address(), 2 * 1024 * 1024); } catch (Exception e) { Log.w(TAG, "mirror: " + e.getClass().getSimpleName()); }
            if (json == null) return;   // no answer: asked again next time, nothing kept
            final boolean there = SuggestedApps.pickMirror(a, json) != null;
            getSharedPreferences("suggested", MODE_PRIVATE).edit().putString("mirror_" + a.id, (there ? "1|" : "0|") + now).apply();
            if (there) runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) recreate(); });   // the screen again, now with its tile
        }, "suggested-mirror").start();
    }

    /**
     * A MIRROR app (UrTV Player): the newest «player-…» release of this repository, its published SHA-256, then the file; installed only when
     * the file has that SHA-256, is that package and is signed by exactly the pinned certificate (SuggestedApps.mirrorFileOk). null when Android's
     * install screen was asked for.
     */
    private String mirror(SuggestedApps.App a, File f) throws Exception {
        if (!SuggestedApps.mirrorReady(a)) return S.t("ما قدرنا نثبّته. جرّب مرة ثانية");
        String json = fetch(a.address(), 2 * 1024 * 1024);
        String[] urls = json == null ? null : SuggestedApps.pickMirror(a, json);
        if (urls == null || !SuggestedApps.mirrorOk(a, urls[0]) || !SuggestedApps.mirrorOk(a, urls[1])) return S.t("ما لقينا نسخة تناسب هذا التلفزيون");
        String published = SuggestedApps.parseSha(fetch(urls[1], 4096));
        if (published == null) return S.t("ما قدرنا ننزّله. جرّب مرة ثانية");
        String sha = download(urls[0], f);
        if (sha == null) return S.t("ما قدرنا ننزّله. جرّب مرة ثانية");
        int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES | PackageManager.GET_SIGNATURES : PackageManager.GET_SIGNATURES;
        PackageInfo info = getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(), flags);
        java.util.Set<String> signers = new java.util.HashSet<>();
        if (info != null) {
            android.content.pm.Signature[] sigs = null;
            if (Build.VERSION.SDK_INT >= 28 && info.signingInfo != null) sigs = info.signingInfo.getApkContentsSigners();
            if ((sigs == null || sigs.length == 0) && info.signatures != null) sigs = info.signatures;
            if (sigs != null) for (android.content.pm.Signature s : sigs) signers.add(SuggestedApps.hex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray())));
        }
        if (!SuggestedApps.mirrorFileOk(a, sha, published, info == null ? null : info.packageName, signers)) {
            f.delete();
            return S.t("الملف ليس هذا التطبيق، فما ثبّتناه");
        }
        commit(f, a.pkg);
        return null;
    }

    private void commit(File f, String pkg) throws Exception {
        PackageInstaller installer = getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(pkg);
        int id = installer.createSession(params);
        try (PackageInstaller.Session session = installer.openSession(id)) {
            try (OutputStream out = session.openWrite("base.apk", 0, f.length()); InputStream in = new FileInputStream(f)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                session.fsync(out);
            }
            Intent back = new Intent(RESULT).setPackage(getPackageName());
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
            session.commit(PendingIntent.getBroadcast(this, 9300, back, flags).getIntentSender());
        } catch (Exception e) {
            try { installer.abandonSession(id); } catch (Exception ignored) { }
            throw e;
        } finally {
            f.delete();
        }
    }

    /** The file into f: https only, from an allowed place (SuggestedApps), at most UpdateRules.MAX_APK_BYTES. Its SHA-256, or null. */
    private static String download(String url, File f) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setConnectTimeout(10_000);
        con.setReadTimeout(30_000);
        try {
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK || !"https".equals(con.getURL().getProtocol())) return null;
            if (con.getContentLength() > UpdateRules.MAX_APK_BYTES) return null;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            long total = 0;
            try (InputStream in = con.getInputStream(); OutputStream o = new FileOutputStream(f)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (total > UpdateRules.MAX_APK_BYTES) { f.delete(); return null; }
                    md.update(buf, 0, n);
                    o.write(buf, 0, n);
                }
            }
            if (total == 0) { f.delete(); return null; }
            return SuggestedApps.hex(md.digest());
        } finally {
            con.disconnect();
        }
    }

    private static String fetch(String url, int max) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setConnectTimeout(10_000);
        con.setReadTimeout(20_000);
        con.setRequestProperty("Accept", "application/vnd.github+json");
        try {
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            try (InputStream in = con.getInputStream()) {
                java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[16 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) { if (b.size() + n > max) return null; b.write(buf, 0, n); }
                return b.toString("UTF-8");
            }
        } finally {
            con.disconnect();
        }
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
