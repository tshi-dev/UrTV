package sa.urtv.home;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import sa.urtv.home.phone.PhoneControl;
import sa.urtv.home.phone.PhoneHost;
import sa.urtv.home.phone.Pairing;
import sa.urtv.home.phone.PrivateScreen;
import sa.urtv.home.phone.QrCode;
import sa.urtv.home.phone.QrView;
import sa.urtv.home.phone.Sessions;

/**
 * «تحكم من الجوال»: the owner's switch and the screen for it. Off, it only offers «تشغيل»; nothing listens. On, it shows the QR code for the
 * pairing address (and the address with a 6-digit code under it), asks «جوال جديد يطلب التحكم» with سماح / رفض, lists «الجوالات المتصلة» with
 * إلغاء for each, and has «إيقاف التحكم من الجوال». Leaving this screen with no phone connected turns it off again. No dialogs, like
 * DockActivity: rows are built right-aligned by hand. Opened from the screensaver's settings (behind the signature permission sa.urtv.home.OPEN_PHONE) or from «محفظتي».
 * It is a PrivateScreen: keys sent from a phone never reach it, so a phone can't press سماح for another one.
 */
public class PhoneActivity extends Activity implements PrivateScreen {
    /** «اللغة» (docs/ENGLISH.md): with Arabic the context itself, with English an English one */
    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }

    private int GOLD, INK, TEXT, DIM, RED;   // from the glass look (glass(), before anything is drawn)
    private GlassStyle.Look look;
    /** from «محفظتي» («أسهل من الجوال»): the QR and the address open the phone's portfolio page (/m), which pairs the same way */
    static final String EXTRA_PORTFOLIO = "portfolio";
    private boolean portfolio;   // the extra asked for «محفظتي»'s view; shown only once the window is open (portfolioShown)
    private boolean asking;      // the PIN keypad for «محفظتي» is on the screen

    private PhoneControl control;
    private LinearLayout col;
    private TextView countdown;
    private String shownToken = "";
    private boolean left;   // the person left this screen (Home, another app): not the screensaver coming over it
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            update();
            main.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Digits.init(this);
        control = PhoneHost.get(this);
        portfolio = getIntent().getBooleanExtra(EXTRA_PORTFOLIO, false);
        control.setOnSessionEnd(Portfolio::closeEditing);   // a phone revoked, logged out or control stopped: «محفظتي»'s window closes
        control.setPinGate(new sa.urtv.home.phone.PinStoreGate(Parental.pin(this), System::currentTimeMillis));   // with a family PIN, the phone's changes ask for it (same lockout as the TV's keypad)
        ScrollView scroll = new ScrollView(this);
        glass(scroll);   // the glass look: the home's photo, small and blurred once, under its veil
        scroll.setVerticalScrollBarEnabled(false);
        FrameLayout frame = new FrameLayout(this);
        scroll.addView(frame);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, dp(28), 0, dp(16));
        // wide enough for the code's two columns (the QR beside the address and the code) on one TV screen, no scrolling
        int wide = Math.min(Math.round(getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density) - 96, 1000);
        frame.addView(col, new FrameLayout.LayoutParams(dp(wide), -2, Gravity.CENTER_HORIZONTAL));
        scroll.setLayoutDirection(Dir.layout());   // its buttons and D-pad ids are right-to-left (English: left-to-right, ids swapped): pinned, so a TV set to the other language does not mirror them
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        left = false;
        if (portfolio) authorizePortfolio();   // the extra is a wish: «محفظتي» opens only as its own tab would (the PIN, with «اقفل محفظتي»)
        PhoneHost.setUi(new Runnable() {
            @Override public void run() { runOnUiThread(new Runnable() { @Override public void run() { render(); } }); }
        });
        render();
        main.postDelayed(tick, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        keepAwake(false);
        main.removeCallbacks(tick);
        PhoneHost.setUi(null);
        if (asking) {   // the keypad goes with the screen; coming back asks again
            PinPrompt.dismiss(this);
            asking = false;
        }
        // the code is shown only here: leaving with nobody connected turns it off, so nothing keeps listening for a code nobody can see.
        // The screensaver coming over it is not leaving (the TV test of 159: it came while a phone was pairing and switched control off): Android
        // starts the dream without the user-leaving hint, so control stays on and the code is there again on the way back; the «unused» stop
        // after 15 minutes still applies.
        if (control.isOn() && control.phones().isEmpty() && (left || isFinishing())) {
            control.withdrawOffer();
            control.stop("owner");
        }
    }

    /**
     * «محفظتي» from this screen: never on the extra alone (any caller can set it). Already opened by the owner on the TV (the tab, «أسهل من
     * الجوال»): shown as it is. With «اقفل محفظتي» and a PIN: the PIN first, here; cancelled, the plain phone view, nothing opened. Otherwise
     * this screen on the TV opens it, as the tab itself would.
     */
    private void authorizePortfolio() {
        if (asking) return;
        int entry = PortfolioWindow.entry(Portfolio.editing(Portfolio.now()), Portfolio.locked(this), Parental.state(this).pinSet);
        if (entry == PortfolioWindow.OPEN) Portfolio.openEditing(Portfolio.now());
        if (entry != PortfolioWindow.ASK) return;
        asking = true;
        Parental.confirm(this, Parental.adhanFirst(this), S.n("محفظتي"), () -> {
            asking = false;
            Portfolio.openEditing(Portfolio.now());
            render();
        }, () -> {
            asking = false;
            portfolio = false;   // cancelled: the plain phone view; the window stays shut
            render();
        });
    }

    /** The portfolio's view (/m) only while its window is open: the extra alone never shows it. */
    private boolean portfolioShown() { return portfolio && Portfolio.editing(Portfolio.now()); }

    /** Home, or another app started over this screen by the person: Android's user-leaving hint (never for the screensaver). */
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        left = true;
    }

    // ---------------------------------------------------------------- screen

    /**
     * The screensaver must not start while the code is on screen or a phone waits for «سماح / رفض» (it closed this screen and switched phone control off
     * in the TV test): the window keeps the screen awake while phone control is on and this screen is shown, and never otherwise.
     */
    private void keepAwake(boolean on) {
        if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void render() {
        keepAwake(control.isOn());
        col.removeAllViews();
        shownToken = "";
        col.addView(text(S.t("تحكم من الجوال"), 26, "Medium", TEXT, Dir.start()), right(0));
        View focus;
        if (!control.isOn()) {
            col.addView(text(S.t("يشغّل هذا الخيار خادمًا صغيرًا يعمل على شبكة الواي فاي في بيتك فقط. لا يعمل شيء حتى تضغط «تشغيل»، ويتوقف وحده عندما ينتهي آخر جوال."),
                14, "Regular", DIM, Dir.start()), right(dp(8)));
            col.addView(text(S.t("تمسح رمز الاستجابة السريعة بالجوال، ثم تضغط «سماح» هنا بالريموت."), 14, "Regular", DIM, Dir.start()), right(dp(20)));
            String why = control.lastStop();
            if (!why.isEmpty()) col.addView(text(stopText(why), 14, "Medium", GOLD, Dir.start()), right(dp(14)));
            focus = button(S.t("تشغيل"), GOLD, new View.OnClickListener() {
                @Override public void onClick(View v) { turnOn(); }
            });
            col.addView(focus, button());
        } else {
            focus = renderOn();
        }
        if (focus != null) {
            final View f = focus;
            f.post(new Runnable() { @Override public void run() { f.requestFocus(); } });
        }
    }

    private View renderOn() {
        PhoneControl.Pending pending = control.pending();
        List<Sessions.Info> phones = control.phones();
        View focus = null;
        LinearLayout below = col;   // where the phones and «إيقاف» go: under the code, in the right column, when the code is shown
        if (pending != null && pending.waiting()) {
            col.addView(text(S.t("جوال جديد يطلب التحكم"), 22, "Medium", GOLD, Dir.start()), right(dp(4)));
            col.addView(text(S.t("العنوان: %s. اضغط «سماح» فقط إذا كان هذا جوالك.", ltr(pending.ip)), 15, "Regular", TEXT, Dir.start()), right(dp(14)));
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            View deny = button(S.t("رفض"), RED, new View.OnClickListener() {
                @Override public void onClick(View v) { control.deny(); }
            });
            View allow = button(S.t("سماح"), GOLD, new View.OnClickListener() {
                @Override public void onClick(View v) { control.approve(); }
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(52), 1f);
            p.setMarginEnd(dp(12));   // the screen is pinned right-to-left (English left-to-right): the end is between the two buttons
            row.addView(allow, p);   // the start button first: the screen reads right to left (English: left to right)
            row.addView(deny, new LinearLayout.LayoutParams(0, dp(52), 1f));
            col.addView(row, button());
            focus = deny;   // the safe answer is the one under a stray press
            if (Dir.rtl()) deny.setNextFocusRightId(allow.getId()); else deny.setNextFocusLeftId(allow.getId());
            if (Dir.rtl()) allow.setNextFocusLeftId(deny.getId()); else allow.setNextFocusRightId(deny.getId());
        } else {
            Pairing.Offer o = control.offer();
            if (o == null) {
                col.addView(text(control.lastStop().isEmpty() ? S.t("لا يوجد رمز جاهز.") : stopText(control.lastStop()), 16, "Medium", GOLD, Dir.start()), right(dp(14)));
            } else {
                // one page, two columns (the owner, on the TV test: no scrolling, the address and the code stand out): on the right the
                // address and the 6-digit code each in their own large box, the countdown, the phones and «إيقاف»; on the left the QR code
                shownToken = o.token;
                LinearLayout body = new LinearLayout(this);
                body.setOrientation(LinearLayout.HORIZONTAL);   // right to left, as the screen: the first column is the right one
                LinearLayout info = new LinearLayout(this);
                info.setOrientation(LinearLayout.VERTICAL);
                info.addView(text(S.t("افتح هذا العنوان في متصفح الجوال"), 15, "Regular", DIM, Dir.start()), right(dp(6)));
                info.addView(boxed(ltr(control.address() + (portfolioShown() ? "/m" : "/p")), 28, GOLD, 0f), right(dp(14)));
                info.addView(text(S.t("واكتب هذا الرمز"), 15, "Regular", DIM, Dir.start()), right(dp(6)));
                info.addView(boxed(ltr(spaced(o.code)), 48, TEXT, 0.08f), right(dp(8)));
                countdown = text("", 13, "Regular", DIM, Dir.start());
                info.addView(countdown, right(dp(14)));
                below = info;
                body.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));
                LinearLayout qrCol = new LinearLayout(this);
                qrCol.setOrientation(LinearLayout.VERTICAL);
                qrCol.setGravity(Gravity.CENTER_HORIZONTAL);
                qrCol.addView(text(S.t("أو امسح الرمز بكاميرا الجوال"), 15, "Regular", DIM, Gravity.CENTER), new LinearLayout.LayoutParams(-1, -2));
                QrView qr = new QrView(this);
                qr.setCode(QrCode.encode(portfolioShown() ? control.pairUrl(o).replace("/p#", "/m#") : control.pairUrl(o)));
                LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(dp(240), dp(240));
                qp.topMargin = dp(8);
                qrCol.addView(qr, qp);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dp(280), -2);
                cp.setMarginStart(dp(36));
                body.addView(qrCol, cp);
                col.addView(body, right(dp(8)));
                update();
            }
        }

        below.addView(text(S.t("الجوالات المتصلة"), 16, "Medium", GOLD, Dir.start()), right(dp(4)));
        if (phones.isEmpty()) below.addView(text(S.t("لا يوجد جوال متصل."), 14, "Regular", DIM, Dir.start()), right(dp(8)));
        int n = 0;
        for (final Sessions.Info s : phones) {
            n++;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            View cancel = button(S.t("إلغاء"), RED, new View.OnClickListener() {
                @Override public void onClick(View v) {
                    control.revoke(s.id);
                    toast(S.t("أُلغي اتصال الجوال"));
                }
            });
            row.addView(cancel, new LinearLayout.LayoutParams(dp(110), dp(46)));
            TextView label = text(S.t("جوال %s، %s", Digits.of(String.valueOf(n)), ltr(s.ip)), 16, "Medium", TEXT, Dir.start());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.setMarginStart(dp(4));
            row.addView(label, lp);
            below.addView(row, rowParams());
            if (focus == null) focus = cancel;
        }
        View stop = button(S.t("إيقاف التحكم من الجوال"), RED, new View.OnClickListener() {
            @Override public void onClick(View v) {
                control.withdrawOffer();
                control.stop("owner");
            }
        });
        LinearLayout.LayoutParams sp = button();
        sp.topMargin = below == col ? dp(18) : dp(4);
        below.addView(stop, sp);
        if (focus == null) focus = stop;
        col.addView(text(S.t("الجلسة تنتهي بعد %s دقيقة بلا استخدام أو %s ساعات كحد أقصى، ولا يُحفظ شيء بعد إعادة التشغيل.", Digits.of("30"), Digits.of("4")), 12, "Regular", DIM, Dir.start()), right(dp(6)));
        return focus;
    }

    /** Once a second: the countdown of the code, and a new code when it ran out. */
    private void update() {
        if (!control.isOn()) return;
        Pairing.Offer o = control.offer();
        if (o == null) return;
        if (!shownToken.isEmpty() && !o.token.equals(shownToken) && control.pending() == null) { render(); return; }
        if (countdown != null) {
            long left = Math.max(0, (o.expiresAt - System.currentTimeMillis()) / 1000);
            countdown.setText(S.t("ينتهي هذا الرمز بعد %s ويتجدد وحده، ويصلح لجوال واحد مرة واحدة.", Digits.of(String.format(java.util.Locale.ROOT, "%d:%02d", left / 60, left % 60))));
        }
    }

    private void turnOn() {
        PhoneControl.Start r = PhoneHost.turnOn(this);
        if (r == PhoneControl.Start.NO_WIFI) toast(S.t("اتصل بشبكة واي فاي أولًا: هذا الخيار يعمل على الواي فاي فقط"));
        else if (r == PhoneControl.Start.FAILED) toast(S.t("تعذّر التشغيل، حاول من جديد"));
        render();
    }

    private static String stopText(String why) {
        switch (why) {
            case "locked": return S.t("توقف التحكم من الجوال لأن الرمز أُدخل خطأ خمس مرات.");
            case "ended": return S.t("توقف التحكم من الجوال لأن آخر جوال انتهت جلسته.");
            case "unused": return S.t("توقف التحكم من الجوال لأنه لم يتصل أي جوال.");
            default: return "";
        }
    }

    // ---------------------------------------------------------------- helpers

    /** A value that must stand out (the address, the code): large, in a framed box of its own. */
    private TextView boxed(String s, float sp, int color, float spacing) {
        TextView t = text(s, sp, "Medium", color, Gravity.CENTER);
        t.setLetterSpacing(spacing);
        t.setPadding(dp(16), dp(10), dp(16), dp(12));
        t.setSingleLine(true);
        t.setBackground(GlassPaint.panel(this, look, 16));   // a glass panel of its own
        return t;
    }

    /** «123 456»: the six digits read in two halves. */
    static String spaced(String code) {
        return code != null && code.length() == 6 ? code.substring(0, 3) + " " + code.substring(3) : code;
    }

    /** Latin text (an address, a code) inside an Arabic line: kept in its own direction. */
    private static String ltr(String s) { return "⁦" + s + "⁩"; }

    private LinearLayout.LayoutParams button() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52));
        p.bottomMargin = dp(8);
        return p;
    }

    /** a button: its word in the warning colour when {@code accent} is RED (stop, deny, cancel), the solid focus fill while focused */
    private View button(String label, final int accent, View.OnClickListener click) {
        final TextView p = text(label, 17, "Medium", accent == RED ? RED : TEXT, Gravity.CENTER);
        p.setFocusable(true);
        p.setClickable(true);
        p.setId(View.generateViewId());
        GlassPaint.card(p, look, 14);
        p.setOnClickListener(click);
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

    private LinearLayout.LayoutParams rowParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(8);
        return p;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    /** The glass look on this page (GlassSource: the home's photo under its veil, or the look's solid colour), and this screen's colours from it. */
    private void glass(View page) {
        look = GlassSource.page(this, page);
        GOLD = look.accent; INK = look.onFocus; TEXT = look.text; DIM = look.sub; RED = look.warn;
    }
}
