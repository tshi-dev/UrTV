package sa.urtv.home;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * 4.3.5 (R7): what the home opens instead of its screen after it died twice right after starting (CrashLoop). Nothing here can fail the way the
 * home did: plain views, no WebView, no other app's data, no photo. «افتح الشاشة الأصلية» opens the TV's own home; «حاول مرة ثانية» clears the
 * count and starts the home screen again. Our claim as the default home is dropped, so the next Home press offers the TV's own home too.
 */
public class SafeActivity extends Activity {
    static final String TITLE = S.n("تلفزيونك توقف أكثر من مرة عند التشغيل"),
            BODY = S.n("افتح الشاشة الأصلية للتلفزيون، أو جرّب مرة ثانية. إذا تكرر، أرسل تقرير من مساعدك"),
            STOCK = S.n("افتح الشاشة الأصلية"), RETRY = S.n("حاول مرة ثانية"),
            NO_STOCK = S.n("ما لقينا شاشة أصلية مفعّلة، افتح الإعدادات من زر الإعدادات في الريموت");

    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        try { getPackageManager().clearPackagePreferredActivities(getPackageName()); } catch (RuntimeException ignored) { }   // our own claim only
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setBackgroundColor(Color.rgb(0x17, 0x18, 0x1B));
        int pad = Math.round(48 * getResources().getDisplayMetrics().density);
        col.setPadding(pad, pad, pad, pad);
        col.addView(text(S.tv(TITLE), 26));
        col.addView(text(S.tv(BODY), 18));
        Button stock = button(S.tv(STOCK));
        stock.setOnClickListener(v -> openStock());
        Button retry = button(S.tv(RETRY));
        retry.setOnClickListener(v -> {
            CrashLoopStore.clear(this);
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
        col.addView(stock);
        col.addView(retry);
        setContentView(col);
        stock.requestFocus();
    }

    /** the TV's own home: the first other app that answers HOME */
    private void openStock() {
        ComponentName c = stockHome();
        if (c == null) { android.widget.Toast.makeText(this, S.tv(NO_STOCK), android.widget.Toast.LENGTH_LONG).show(); return; }
        try {
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setComponent(c).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException e) {
            android.widget.Toast.makeText(this, S.tv(NO_STOCK), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private ComponentName stockHome() {
        List<ResolveInfo> homes = getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
        for (ResolveInfo r : homes) {
            if (r.activityInfo == null || getPackageName().equals(r.activityInfo.packageName)) continue;
            if ("com.android.settings".equals(r.activityInfo.packageName)) continue;   // Settings' FallbackHome: only a stand-in
            return new ComponentName(r.activityInfo.packageName, r.activityInfo.name);
        }
        return null;
    }

    private TextView text(String s, int sp) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, 0, 0, Math.round(16 * getResources().getDisplayMetrics().density));
        return t;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        b.setAllCaps(false);
        return b;
    }
}
