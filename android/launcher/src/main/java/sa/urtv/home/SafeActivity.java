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
 * count and starts the home screen again. Our place as the default home is never touched (4.3.5 review F1: on Android 9 nothing could put it back,
 * and the setup wizard would take the Home key).
 */
public class SafeActivity extends Activity {
    static final String TITLE = S.n("تلفزيونك توقف أكثر من مرة عند التشغيل"),
            BODY = S.n("افتح الشاشة الأصلية للتلفزيون، أو جرّب مرة ثانية. إذا تكرر، أرسل تقرير من مساعدك"),
            BODY_RETRY = S.n("جرّب مرة ثانية. إذا تكرر، أرسل تقرير من مساعدك"),
            STOCK = S.n("افتح الشاشة الأصلية"), RETRY = S.n("حاول مرة ثانية"),
            NO_STOCK = S.n("ما لقينا شاشة أصلية مفعّلة، افتح الإعدادات من زر الإعدادات في الريموت");

    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setBackgroundColor(Color.rgb(0x17, 0x18, 0x1B));
        int pad = Math.round(48 * getResources().getDisplayMetrics().density);
        col.setPadding(pad, pad, pad, pad);
        col.addView(text(S.tv(TITLE), 26));
        boolean hasStock = stockHome() != null;   // 4.3.5 review F2: a TV whose own home our setup turned off: no dead button, the retry is the way
        col.addView(text(S.tv(hasStock ? BODY : BODY_RETRY), 18));   // review F8: no line about a home that is not there
        Button stock = button(S.tv(STOCK));
        stock.setOnClickListener(v -> openStock());
        Button retry = button(S.tv(RETRY));
        retry.setOnClickListener(v -> {
            CrashLoopStore.clear(this);
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
        if (hasStock) col.addView(stock);
        col.addView(retry);
        setContentView(col);
        (hasStock ? stock : retry).requestFocus();
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
            if (r.activityInfo == null) continue;
            if (!StockHome.real(r.activityInfo.packageName, r.activityInfo.name)) continue;   // never ours, the setup wizard or a Settings stand-in (as the setup's otherHome)
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
