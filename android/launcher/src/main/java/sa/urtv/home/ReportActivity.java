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

import java.util.List;

import sa.urtv.home.phone.QrCode;
import sa.urtv.home.phone.QrView;

/**
 * «أرسل تقرير», opened from the screensaver's settings: the report's few lines (Report) shown as they are, and a QR code to docs/share with
 * them. Nothing is sent from the TV: the owner sends it from the phone if they want. Back closes it.
 */
public class ReportActivity extends Activity {
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
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(0, dp(48), 0, dp(48));
        frame.addView(row, new FrameLayout.LayoutParams(dp(820), -2, Gravity.CENTER_HORIZONTAL));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(text(Report.TITLE, 30, "Medium", look.text), lp(dp(14)));
        col.addView(text(Report.HOW, 17, "Regular", look.sub), lp(dp(22)));
        List<String> lines = Report.lines(version(getPackageName()), version("sa.urtv.screensaver"), Build.VERSION.RELEASE, Build.VERSION.SDK_INT, webView());
        col.addView(text(Report.WHAT, 16, "Medium", look.accent), lp(dp(6)));
        TextView shown = text(String.join("\n", lines), 15, "Regular", look.sub);
        shown.setTextDirection(View.TEXT_DIRECTION_LTR);   // the report's own lines are English, as share.py writes them
        shown.setGravity(Gravity.START);
        col.addView(shown, lp(dp(24)));
        TextView back = text(Report.BACK, 18, "Medium", look.text);
        back.setFocusable(true);
        back.setClickable(true);
        back.setPadding(dp(22), dp(12), dp(22), dp(12));
        GlassPaint.card(back, look, 14);   // the focus fill and the focus ink, as every button of the glass look
        back.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2);
        col.addView(back, bp);
        row.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));

        String url = Report.url(lines);
        if (url != null) {
            QrView qr = new QrView(this);
            qr.setCode(QrCode.encodeLarge(url));
            LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(dp(260), dp(260));
            qp.setMarginStart(dp(40));
            row.addView(qr, qp);
        }
        setContentView(scroll);
        back.post(back::requestFocus);
    }

    private String version(String pkg) {
        try {
            PackageInfo p = getPackageManager().getPackageInfo(pkg, 0);
            return p.versionName;
        } catch (Exception e) {
            return null;
        }
    }

    private static String webView() {
        try {
            PackageInfo p = android.webkit.WebView.getCurrentWebViewPackage();
            return p == null ? null : p.versionName;
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
