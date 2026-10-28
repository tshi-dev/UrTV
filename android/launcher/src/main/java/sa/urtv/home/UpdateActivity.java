package sa.urtv.home;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Map;

import sa.urtv.home.phone.PhoneUpdater;
import sa.urtv.home.phone.UpdateOrder;
import sa.urtv.home.phone.UpdateRecords;
import sa.urtv.home.phone.UpdateStore;
import sa.urtv.home.phone.UpdateText;

/**
 * «تحديث تلفزيوني»: the version installed, «تحقق من التحديث», and what the check found (the decision is UpdateFlow, the work is PhoneUpdater,
 * the same code the phone page's button calls). When there is a newer version and every check passed, the line «تحديث جديد · اضغط للتحديث» is
 * the button; Android's own install screen follows and the owner confirms with the remote, nothing is installed silently. After the launcher
 * has been replaced, the first start reads the «update in progress» record and opens this screen with how it went (once). No dialogs, like
 * DockActivity. Opened from the launcher's menu or from the screensaver's settings; only our two apps can open it (the signature permission
 * sa.urtv.home.OPEN_UPDATE, so `am start` from a shell is refused).
 */
public class UpdateActivity extends Activity {
    private int GOLD, INK, TEXT, DIM, RED;   // from the glass look (glass(), before anything is drawn)
    private GlassStyle.Look look;

    private LinearLayout col;
    private UpdateRecords.Outcome result;   // what the record of an earlier update says, until the owner checks again
    private boolean askedPermission;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Digits.init(this);
        ScrollView scroll = new ScrollView(this);
        glass(scroll);   // the glass look: the home's photo, small and blurred once, under its veil
        scroll.setVerticalScrollBarEnabled(false);
        FrameLayout frame = new FrameLayout(this);
        scroll.addView(frame);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, dp(36), 0, dp(48));
        frame.addView(col, new FrameLayout.LayoutParams(dp(640), -2, Gravity.CENTER_HORIZONTAL));
        setContentView(scroll);
        // the record of an update that replaced the launcher: shown here once, then removed
        UpdateRecords.Outcome o = UpdateRecords.take(new UpdateStore(this), PhoneUpdater.installedCode(this, getPackageName()));
        if (o.kind != UpdateRecords.Kind.NONE) result = o;
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // the update and install steps must not be cut by the screensaver while this screen is shown
        PhoneUpdater.setVisible(true);
        PhoneUpdater.setListener(new Runnable() {
            @Override public void run() { runOnUiThread(new Runnable() { @Override public void run() { render(); } }); }
        });
        // back from the system screen where the owner allowed it: check again
        if (askedPermission && PhoneUpdater.canInstall(this)) {
            askedPermission = false;
            result = null;
            PhoneUpdater.check(this);
        }
        render();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        PhoneUpdater.setVisible(false);
        PhoneUpdater.setListener(null);
    }

    // ---------------------------------------------------------------- screen

    private void render() {
        col.removeAllViews();
        col.addView(text(UpdateText.TITLE, 26, "Medium", TEXT, Gravity.RIGHT), right(0));
        col.addView(text(UpdateText.installed(PhoneUpdater.installedName(this)), 16, "Regular", DIM, Gravity.RIGHT), right(dp(22)));
        View focus;
        if (result != null) {
            focus = renderRecord();
        } else {
            focus = renderState();
        }
        if (focus != null) {
            final View f = focus;
            f.post(new Runnable() { @Override public void run() { f.requestFocus(); } });
        }
    }

    private View renderRecord() {
        if (result.kind == UpdateRecords.Kind.SUCCESS) {
            col.addView(text(UpdateText.done(UpdateText.versionOf(result.target)), 22, "Medium", GOLD, Gravity.RIGHT), right(dp(18)));
        } else {
            col.addView(text(UpdateText.NOT_DONE, 22, "Medium", RED, Gravity.RIGHT), right(dp(6)));
            col.addView(text(UpdateText.failedStep(UpdateOrder.label(result.step)), 15, "Regular", DIM, Gravity.RIGHT), right(dp(14)));
            aiMessage();
        }
        return checkButton();
    }

    private View renderState() {
        Map<String, Object> st = PhoneUpdater.status();
        String s = (String) st.get("status");
        String why = st.containsKey("reason") ? (String) st.get("reason") : "";
        String detail = st.containsKey("detail") ? (String) st.get("detail") : "";
        Object b = st.get("build");
        String newest = UpdateText.versionOf(b == null ? 0 : ((Long) b).intValue());
        switch (s) {
            case "checking":
                col.addView(text(UpdateText.WORKING, 18, "Medium", GOLD, Gravity.RIGHT), right(dp(8)));
                return null;
            case "downloading":
                col.addView(text(UpdateText.DOWNLOADING, 18, "Medium", GOLD, Gravity.RIGHT), right(dp(8)));
                return null;
            case "uptodate":
                col.addView(text(UpdateText.upToDate(PhoneUpdater.installedName(this)), 20, "Medium", GOLD, Gravity.RIGHT), right(dp(16)));
                return checkButton();
            case "found":       // the phone page only read version.txt: pressing downloads, checks, then installs
            case "available":
                return button(UpdateText.available(newest), GOLD, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        // with a family PIN set and «قفل الإعدادات» on, an install from the TV's own screen asks for it too (the phone's does, PhoneApi)
                        Parental.guard(UpdateActivity.this, Parental.adhanFirst(UpdateActivity.this), "تحديث تلفزيوني",
                                ParentalPolicy.targets(ParentalPolicy.UPDATE_SCREEN, null), new Runnable() {
                                    @Override public void run() { PhoneUpdater.install(UpdateActivity.this, false); }
                                }, new Runnable() { @Override public void run() { } });
                    }
                }, 60);
            case "waiting_owner":
                col.addView(text(UpdateText.CONFIRM, 20, "Medium", GOLD, Gravity.RIGHT), right(dp(6)));
                if (!detail.isEmpty()) col.addView(text(UpdateOrder.label(detail), 15, "Regular", DIM, Gravity.RIGHT), right(dp(14)));
                if (PhoneUpdater.confirmWaiting()) {
                    getWindow().getDecorView().post(new Runnable() { @Override public void run() { if (PhoneUpdater.confirmWaiting()) PhoneUpdater.showConfirm(UpdateActivity.this); } });
                }
                if (PhoneUpdater.confirmKnown()) {
                    return button(UpdateText.SHOW_CONFIRM, GOLD, new View.OnClickListener() {
                        @Override public void onClick(View v) { PhoneUpdater.showConfirm(UpdateActivity.this); }
                    }, 52);
                }
                return null;
            case "done":
                col.addView(text(UpdateText.done(newest), 22, "Medium", GOLD, Gravity.RIGHT), right(dp(16)));
                return null;
            case "error":
                return renderError(why, detail);
            default:
                return checkButton();
        }
    }

    private View renderError(String why, String detail) {
        switch (why) {
            case "offline":
                col.addView(text(UpdateText.OFFLINE, 20, "Medium", RED, Gravity.RIGHT), right(dp(16)));
                return checkButton();
            case "need_permission":
                col.addView(text(UpdateText.NEED_PERMISSION, 18, "Medium", TEXT, Gravity.RIGHT), right(dp(14)));
                return button(UpdateText.OPEN_PERMISSION, GOLD, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (PhoneUpdater.openPermissionScreen(UpdateActivity.this)) askedPermission = true;
                        else PhoneUpdater.permissionScreenFailed();   // the screen isn't there: the TV can't do this itself
                    }
                }, 52);
            case "declined":
                col.addView(text(UpdateText.NOT_DONE, 22, "Medium", RED, Gravity.RIGHT), right(dp(6)));
                col.addView(text(UpdateText.DECLINED, 15, "Regular", DIM, Gravity.RIGHT), right(dp(16)));
                return checkButton();
            case "failed":
                col.addView(text(UpdateText.NOT_DONE, 22, "Medium", RED, Gravity.RIGHT), right(dp(6)));
                col.addView(text(UpdateText.failedStep(UpdateOrder.label(detail)), 15, "Regular", DIM, Gravity.RIGHT), right(dp(14)));
                aiMessage();
                return checkButton();
            default:   // "cannot", and anything this screen doesn't know: the same message
                aiMessage();
                return checkButton();
        }
    }

    /** The message that sends the owner to their AI assistant, with the repository address small under it. */
    private void aiMessage() {
        col.addView(text(UpdateText.AI_MESSAGE, 20, "Medium", TEXT, Gravity.RIGHT), right(dp(8)));
        TextView repo = text("⁦" + UpdateText.REPO + "⁩", 12, "Regular", DIM, Gravity.RIGHT);
        col.addView(repo, right(dp(18)));
    }

    private View checkButton() {
        return button(UpdateText.CHECK, GOLD, new View.OnClickListener() {
            @Override public void onClick(View v) {
                result = null;
                PhoneUpdater.reset();
                PhoneUpdater.check(UpdateActivity.this);
                render();
            }
        }, 52);
    }

    // ---------------------------------------------------------------- helpers

    private View button(String label, final int accent, View.OnClickListener click, int heightDp) {
        final TextView p = text(label, 18, "Medium", accent == RED ? RED : TEXT, Gravity.CENTER);   // the warning colour for RED
        p.setFocusable(true);
        p.setClickable(true);
        GlassPaint.card(p, look, 14);   // the panel, the solid focus fill and its ink while focused
        p.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(heightDp));
        lp.bottomMargin = dp(8);
        col.addView(p, lp);
        return p;
    }

    private TextView text(String s, float sp, String weight, int color, int gravity) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(gravity);
        try { t.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Thmanyah-" + weight + ".otf")); }
        catch (RuntimeException ignored) { }
        return t;
    }

    private LinearLayout.LayoutParams right(int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = bottom;
        return p;
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    /** The glass look on this page (GlassSource: the home's photo under its veil, or the look's solid colour), and this screen's colours from it. */
    private void glass(View page) {
        look = GlassSource.page(this, page);
        GOLD = look.accent; INK = look.onFocus; TEXT = look.text; DIM = look.sub; RED = look.warn;
    }
}
