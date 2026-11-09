package sa.urtv.home;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import sa.urtv.home.phone.PhoneUpdater;
import sa.urtv.home.phone.QrCode;
import sa.urtv.home.phone.UpdateText;
import sa.urtv.home.phone.QrView;

/**
 * «أرسل تقرير», opened from the screensaver's settings: the report's few lines (Report) shown as they are, and a QR code to docs/share with
 * them. Nothing is sent from the TV: the owner sends it from the phone if they want. Back closes it.
 */
public class ReportActivity extends Activity {
    /** «اللغة» (docs/ENGLISH.md): with Arabic the context itself, with English an English one */
    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Digits.init(this);
        ScrollView scroll = new ScrollView(this);
        GlassStyle.Look look = GlassSource.page(this, scroll);
        scroll.setVerticalScrollBarEnabled(false);
        FrameLayout frame = new FrameLayout(this);
        scroll.addView(frame);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(Dir.layout());
        row.setPadding(0, dp(48), 0, dp(48));
        frame.addView(row, new FrameLayout.LayoutParams(dp(820), -2, Gravity.CENTER_HORIZONTAL));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(text(S.tv(Report.TITLE), 30, "Medium", look.text), lp(dp(14)));
        col.addView(text(S.tv(Report.HOW), 17, "Regular", look.sub), lp(dp(22)));
        long saver = PhoneUpdater.installedCode(this, "sa.urtv.screensaver");   // the build of each app, written as everywhere (UpdateText.versionOf)
        final String homeV = PhoneUpdater.installedName(this), saverV = saver < 0 ? null : UpdateText.versionOf(BuildConfig.MARKETING, (int) saver);
        final List<String> errors = recentErrors();
        // what it is about: one pick from a short list, so a report is never empty (a short row each; OK picks)
        col.addView(text(S.tv(Report.ABOUT), 16, "Medium", look.accent), lp(dp(6)));
        final TextView[] rowsOf = new TextView[Report.TOPICS.length];
        final TextView shown = text("", 15, "Regular", look.sub);
        shown.setTextDirection(View.TEXT_DIRECTION_LTR);   // the report's own lines are English, as share.py writes them
        shown.setGravity(Gravity.START);
        final LinearLayout qrBox = new LinearLayout(this);
        final Runnable[] refresh = new Runnable[1];
        for (int i = 0; i < Report.TOPICS.length; i++) {
            final int at = i;
            TextView r = text(S.tv(Report.TOPICS[i][1]), 17, "Medium", look.text);
            r.setFocusable(true);
            r.setClickable(true);
            r.setPadding(dp(16), dp(10), dp(16), dp(10));
            GlassPaint.card(r, look, 12);
            r.setOnClickListener(v -> { topic = Report.TOPICS[at][0]; refresh[0].run(); });
            rowsOf[i] = r;
            col.addView(r, lp(dp(8)));
        }
        refresh[0] = () -> {
            for (int i = 0; i < rowsOf.length; i++) rowsOf[i].setText((Report.TOPICS[i][0].equals(topic) ? "✓ " : "") + S.tv(Report.TOPICS[i][1]));
            List<String> lines = Report.lines(homeV, saverV, Build.VERSION.RELEASE, Build.VERSION.SDK_INT, webView(), topic,
                    GlassSource.lookName(this), GlassSource.light(this) ? "light" : "dark", HomeCard.footballShown ? "football" : "standard", errors);
            shown.setText(String.join("\n", lines));
            qrBox.removeAllViews();
            String url = Report.url(lines);
            if (url != null) {
                QrView qr = new QrView(this);
                qr.setCode(QrCode.encodeLarge(url));
                qrBox.addView(qr, new LinearLayout.LayoutParams(dp(260), dp(260)));
            }
        };
        refresh[0].run();
        col.addView(text(S.tv(Report.WHAT), 16, "Medium", look.accent), lp(dp(6)));
        col.addView(shown, lp(dp(24)));
        TextView back = text(S.tv(Report.BACK), 18, "Medium", look.text);
        back.setFocusable(true);
        back.setClickable(true);
        back.setPadding(dp(22), dp(12), dp(22), dp(12));
        GlassPaint.card(back, look, 14);   // the focus fill and the focus ink, as every button of the glass look
        back.setOnClickListener(v -> finish());
        col.addView(back, new LinearLayout.LayoutParams(-2, -2));
        row.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(-2, -2);
        qp.setMarginStart(dp(40));
        row.addView(qrBox, qp);
        setContentView(scroll);
        rowsOf[0].post(rowsOf[0]::requestFocus);
    }

    private String topic = "other";   // the pick; «غير ذلك» until the person chooses

    /** the exception class names this app's own log shows (logcat of our own process needs no permission), at most five, nothing else of any line */
    private static List<String> recentErrors() {
        List<String> out = new ArrayList<>();
        try {
            Process p = new ProcessBuilder("logcat", "-d", "-t", "400", "--pid=" + android.os.Process.myPid(), "*:E").redirectErrorStream(true).start();
            java.util.regex.Pattern n = java.util.regex.Pattern.compile("\\b([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*(?:Exception|Error))\\b");
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream(), "UTF-8"))) {
                for (String line; (line = r.readLine()) != null; ) {
                    java.util.regex.Matcher m = n.matcher(line);
                    while (m.find()) { String c = m.group(1); if (!out.contains(c)) out.add(c); }
                }
            }
        } catch (Exception | Error ignored) { }
        return out.size() > 5 ? out.subList(out.size() - 5, out.size()) : out;
    }

    private static String webView() {
        try {
            PackageInfo p = android.webkit.WebView.getCurrentWebViewPackage();
            if (p == null) return null;   // its build number (no versionName is read anywhere: tests/version_name_test.py)
            return String.valueOf(Build.VERSION.SDK_INT >= 28 ? p.getLongVersionCode() : (long) p.versionCode);
        } catch (RuntimeException e) {
            return null;
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
