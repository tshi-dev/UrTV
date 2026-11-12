package sa.urtv.screensaver;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The base of every family game played with one remote (docs/GAMES-KIT.md; the look is «ورقة», design/43 quiz-B*, taken from «الأسرع»). A game extends
 * this class, keeps its rules in a pure-Java engine and draws its screens with the blocks below on the 1920 x 1080 design (scaled to the screen, the
 * safe area is 96 px on every side): the green question block, the answer stamps with letter tabs, the tick timer, the sides' bar, the rank rows, the
 * podium, the quick setup chips. The base owns what every game must do the same way:
 * the screen stays on while the game is open; Back never quits a round, it opens the pause card (متابعة، إنهاء الجولة، الرئيسية) and stops the game's
 * clocks; HOME in the middle of a round does the same; the hand-over screen («غطّوا عيونكم») shows a secret to one person only; the language, the text
 * direction and the digits follow UrTV's settings; a low-RAM TV and «animations off» get no animation, a mid one no confetti.
 * A game implements: id(), draw(), onKey(), and, when it has them, onTick(), onPauseChanged(), onEndRound(), inPlay().
 */
public abstract class GameActivity extends Activity {
    /** the hand-over: a secret hides again by itself after this long, and the count before it shows */
    static final int HAND_HIDE_MS = 20000, HAND_COUNT_MS = 2000;
    /** the states of an answer stamp */
    static final int STAMP = 0, STAMP_FOCUS = 1, STAMP_RIGHT = 2, STAMP_WRONG = 3;
    static final String[] LETTERS_AR = {"أ", "ب", "ج", "د"}, LETTERS_EN = {"A", "B", "C", "D"};
    /** the kit's own words that are picked from a list (marked for the English table) */
    static final String[] ON_OFF = {S.n("لا"), S.n("نعم")}, SOUND_OPTS = {S.n("يعمل"), S.n("صامت")};
    private static final String[] TEAM_WORDS = {S.n("الصقور"), S.n("النجوم"), S.n("الأسود"), S.n("الواحة"), S.n("النخيل"), S.n("الفرسان"), S.n("الأمواج"), S.n("القمم"), S.n("الشهب"), S.n("الروّاد")};

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Map<String, Typeface> fonts = new HashMap<>();
    protected GameLook L; protected Prefs prefs; protected FrameLayout root; protected float scale;
    protected boolean en, rtl, latin, animate, confetti, sound; protected int tier;
    // the pause card and the hand-over, drawn over whatever the game shows
    private boolean pauseCard; private int pauseSel;
    private int hand; private String handName = "", handSecret = "", handNote = ""; private Runnable handDone; private long handAt; private TextView handCount;

    private final Runnable loop = new Runnable() {
        @Override public void run() { step(); h.postDelayed(this, 100); }
    };

    // ---------------------------------------------------------------- what a game gives

    /** the game's id in GameRegistry */
    protected abstract String id();
    /** draw the game's current screen (the root is empty) */
    protected abstract void draw();
    /** a key of the remote: `dx` is toward the end of the line (already turned for Arabic), `dy` down. Back during a round: call openPause(). */
    protected abstract void onKey(boolean back, boolean ok, int dx, int dy);
    /** ten times a second while the game is in front and neither the pause card nor the hand-over is up */
    protected void onTick(long now) { }
    /** the pause card opened (true) or closed: stop or resume the game's clocks */
    protected void onPauseChanged(boolean paused, long now) { }
    /** «إنهاء الجولة» on the pause card: end the round and show the result */
    protected void onEndRound() { }
    /** true while a round is being played: HOME then leaves the pause card up for the return */
    protected boolean inPlay() { return false; }

    // ---------------------------------------------------------------- the activity

    @Override protected void attachBaseContext(Context base) {
        Context e = S.wrap(base);
        if (S.en()) { super.attachBaseContext(e); return; }
        android.content.res.Configuration c = new android.content.res.Configuration(base.getResources().getConfiguration());
        c.setLocale(new java.util.Locale("ar")); c.setLayoutDirection(new java.util.Locale("ar"));
        super.attachBaseContext(base.createConfigurationContext(c));
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setWindowAnimations(0);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // no screensaver and no sleep while a game is open
        prefs = new Prefs(this);
        L = GameLook.of(this);
        latin = DigitsText.isLatin(prefs.digits()); en = S.en(); rtl = Dir.rtl();
        tier = Prefs.ramTier(this);
        float anim = 1f;
        try { anim = Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f); } catch (RuntimeException ignored) { /* default */ }
        animate = tier != Prefs.RAM_LOW && anim > 0f;   // reduced motion (animations off) and low-RAM TVs: none
        confetti = animate && tier == Prefs.RAM_NORMAL;   // a mid TV keeps the small moves and drops the confetti
        sound = GamePrefs.sound(this);
        scale = getResources().getDisplayMetrics().widthPixels / 1920f;
        root = new FrameLayout(this);
        root.setLayoutDirection(Dir.layout());
        root.setBackgroundColor(L.paper);
        setContentView(root);
        GamePrefs.setLast(this, id());
    }

    @Override protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        h.removeCallbacks(loop); h.post(loop);
        redraw();
    }
    /** HOME or another app in front: a round is paused, and a secret on the screen goes back behind its pass screen */
    @Override protected void onPause() {
        super.onPause();
        h.removeCallbacks(loop);
        if (hand > 1) { hand = 1; handCount = null; }
        if (inPlay() && !pauseCard && hand == 0) openPause();
    }

    private void step() {
        long now = System.currentTimeMillis();
        if (pauseCard) return;
        if (hand == 2) {
            long left = HAND_COUNT_MS - (now - handAt);
            if (left <= 0) { hand = 3; handAt = now; redraw(); }
            else if (handCount != null) handCount.setText(num((int) ((left + 999) / 1000)));
            return;
        }
        if (hand == 3) { if (now - handAt >= HAND_HIDE_MS) { hand = 1; redraw(); } return; }   // a secret left on the screen hides by itself
        if (hand == 0) onTick(now);
    }

    /** draw again: the pause card, the hand-over, or the game's own screen */
    protected final void redraw() {
        if (root == null) return;
        root.removeAllViews(); handCount = null;
        root.setBackgroundColor(L.paper);
        if (pauseCard) drawPause(); else if (hand > 0) drawHand(); else draw();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        boolean back = k == KeyEvent.KEYCODE_BACK || k == KeyEvent.KEYCODE_ESCAPE;
        boolean ok = k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_NUMPAD_ENTER || k == KeyEvent.KEYCODE_BUTTON_A;
        int dx = k == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : k == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : 0;
        if (rtl) dx = -dx;   // «next» is toward the end of the line: left in Arabic
        int dy = k == KeyEvent.KEYCODE_DPAD_UP ? -1 : k == KeyEvent.KEYCODE_DPAD_DOWN ? 1 : 0;
        if (!(back || ok || dx != 0 || dy != 0)) return super.dispatchKeyEvent(e);
        if (e.getAction() != KeyEvent.ACTION_DOWN) return true;   // the key is ours whole: its release never reaches another screen
        if (pauseCard) keyPause(back, ok, dy);
        else if (hand > 0) keyHand(back, ok);
        else onKey(back, ok, dx, dy);
        return true;
    }

    // ---------------------------------------------------------------- the pause card

    /** Back during a round: the card, and the clocks stop */
    protected final void openPause() {
        if (pauseCard) return;
        pauseCard = true; pauseSel = 0;
        onPauseChanged(true, System.currentTimeMillis());
        redraw();
    }
    protected final boolean paused() { return pauseCard; }
    private void closePause() { pauseCard = false; onPauseChanged(false, System.currentTimeMillis()); redraw(); }

    private void drawPause() {
        title(S.t("اللعبة متوقفة"), gameName());
        String[] labels = {S.t("متابعة"), S.t("إنهاء الجولة"), S.t("الرئيسية")};
        for (int i = 0; i < 3; i++) root.addView(button(labels[i], pauseSel == i), at(640, 104, 96, 260 + i * 128));
        hints("▲ ▼", S.t("الاختيار"), S.t("رجوع"), S.t("متابعة"));
    }
    private void keyPause(boolean back, boolean ok, int dy) {
        if (back) { closePause(); return; }
        if (dy != 0) { pauseSel = Math.max(0, Math.min(2, pauseSel + dy)); redraw(); return; }
        if (!ok) return;
        if (pauseSel == 0) closePause();
        else if (pauseSel == 1) { pauseCard = false; cancelHandOver(); onPauseChanged(false, System.currentTimeMillis()); onEndRound(); redraw(); }   // a hand-over that was up goes with the round
        else finish();
    }

    // ---------------------------------------------------------------- the hand-over («غطّوا عيونكم»)

    /**
     * One person must see `secret` and the others must not (games/COMMON.md). The pass screen names who takes the remote; OK, a two-second count, the
     * secret alone and large; OK hides it and runs `done`. Back on the secret hides it first; a secret left up hides by itself; never two secrets at once.
     * `note` is a short line under the secret (may be empty).
     */
    protected final void handOver(String name, String secret, String note, Runnable done) {
        handName = name; handSecret = secret; handNote = note == null ? "" : note; handDone = done; hand = 1;
        redraw();
    }
    protected final boolean handingOver() { return hand > 0; }
    /** drop a hand-over without showing its secret and without running its `done` (the round ended, the game moved on); the caller redraws */
    protected final void cancelHandOver() { hand = 0; handDone = null; handCount = null; handSecret = ""; }

    private void drawHand() {
        if (hand == 3) {
            title(handName, S.t("لك وحدك"));
            TextView w = centered(handSecret, handSecret.length() <= 12 ? 200 : handSecret.length() <= 24 ? 136 : 96, L.ink, true); w.setMaxLines(2);
            root.addView(w, at(1728, 440, 96, 300));
            if (!handNote.isEmpty()) root.addView(centered(handNote, 48, L.ink(0xCC), false), at(1728, 72, 96, 760));
            hints(S.t("اختيار"), S.t("أخفِ الكلمة"));
            return;
        }
        root.setBackgroundColor(L.deep);
        root.addView(centered(S.t("سلّموا الريموت لـ %s", handName), 128, L.onDeep, true), at(1728, 180, 96, 220));
        root.addView(centered(S.t("الباقين: غطّوا عيونكم"), 72, L.saff, true), at(1728, 110, 96, 430));
        if (hand == 2) { handCount = centered(num(HAND_COUNT_MS / 1000), 200, L.onDeep, true); root.addView(handCount, at(1728, 260, 96, 600)); }
        else root.addView(button(S.t("أنا جاهز"), true), at(640, 120, 640, 660));
    }
    private void keyHand(boolean back, boolean ok) {
        if (hand == 3) {
            if (back) { hand = 1; redraw(); }   // hidden first; the next Back is the pause card
            else if (ok) { hand = 0; Runnable d = handDone; handDone = null; if (d != null) d.run(); redraw(); }
        } else if (hand == 1) {
            if (back) openPause();
            else if (ok) { hand = 2; handAt = System.currentTimeMillis(); redraw(); }
        }
    }

    // ---------------------------------------------------------------- small helpers

    protected final int px(float v) { return Math.round(v * scale); }
    protected final Typeface font(String n) {
        Typeface t = fonts.get(n);
        if (t == null) { try { t = Typeface.createFromAsset(getAssets(), "fonts/" + n + ".otf"); } catch (RuntimeException e) { t = Typeface.DEFAULT; } fonts.put(n, t); }
        return t;
    }
    protected final Typeface display() { return font("ThmanyahDisplay-Bold"); }
    /** a number in the person's digits */
    protected final String num(int v) { return DigitsText.convert(String.valueOf(v), latin); }
    /** an Arabic text of the data in the person's digits (the English text is left as it is) */
    protected final String digits(String s) { return en ? s : DigitsText.convert(s, latin); }
    protected final String gameName() { GameRegistry.Game g = GameRegistry.byId(id()); return g == null ? "" : S.tv(g.name); }
    protected final void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    /** one of the existing tones (QuizSound.TICK, CHIME, LOW, FANFARE), when the sound chip is on */
    protected final void play(int which) { QuizSound.play(sound, which); }
    protected final String letter(int i) { return en ? LETTERS_EN[i % 4] : LETTERS_AR[i % 4]; }
    /** the default names of `n` sides, in UrTV's language */
    protected final String[] teamNames(int n) { String[] v = new String[n]; for (int i = 0; i < n; i++) v[i] = S.tv(GameScore.TEAMS[i % GameScore.MAX]); return v; }

    /** a view placed on the 1920 x 1080 design: x from the start edge (the right in Arabic), y from the top; a negative width or height fills */
    protected final FrameLayout.LayoutParams at(float w, float hgt, float x, float y) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(w < 0 ? -1 : px(w), hgt < 0 ? -1 : px(hgt), Gravity.START | Gravity.TOP);
        p.setMarginStart(px(x)); p.topMargin = px(y);
        return p;
    }
    /** the same, x from the end edge */
    protected final FrameLayout.LayoutParams atEnd(float w, float hgt, float x, float y) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(px(w), px(hgt), Gravity.END | Gravity.TOP);
        p.setMarginEnd(px(x)); p.topMargin = px(y);
        return p;
    }
    protected final View rect(int fill) { View v = new View(this); v.setBackgroundColor(fill); return v; }
    private GradientDrawable shape(int fill, int stroke, float strokePx, boolean dashed) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        if (strokePx > 0) { if (dashed) g.setStroke(px(strokePx), stroke, px(14), px(10)); else g.setStroke(px(strokePx), stroke); }
        return g;
    }
    /** a square-cornered box: a fill and an ink line (dashed for «not offered» and «wrong») */
    protected final View box(int fill, int stroke, float strokePx, boolean dashed) { View v = new View(this); v.setBackground(shape(fill, stroke, strokePx, dashed)); return v; }
    /** the same as a container */
    protected final FrameLayout stamp(int fill, int stroke, float strokePx, boolean dashed) { FrameLayout f = new FrameLayout(this); f.setBackground(shape(fill, stroke, strokePx, dashed)); return f; }
    /** words: `display` is the heavy face (titles, answers, numbers), else the text face */
    protected final TextView text(CharSequence s, float size, int color, boolean display) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextColor(color); v.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * scale);
        v.setTypeface(font(display ? "ThmanyahDisplay-Bold" : "Thmanyah-Medium"));
        v.setIncludeFontPadding(false); v.setTextDirection(Dir.text()); v.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        return v;
    }
    protected final TextView centered(CharSequence s, float size, int color, boolean display) { TextView v = text(s, size, color, display); v.setGravity(Gravity.CENTER); v.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); return v; }
    /** a text cut with «…» so that it fits `maxW` design pixels at `size` in the heavy face */
    protected final String clip(String s, float size, float maxW) {
        android.graphics.Paint paint = new android.graphics.Paint(); paint.setTypeface(display()); paint.setTextSize(size * scale);
        if (paint.measureText(s) <= maxW * scale) return s;
        String cut = s;
        while (cut.length() > 1 && paint.measureText(cut + "…") > maxW * scale) cut = cut.substring(0, cut.length() - 1);
        return cut.trim() + "…";
    }
    /** the width of a text in design pixels at `size` in the heavy face */
    protected final float measure(String s, float size) { android.graphics.Paint paint = new android.graphics.Paint(); paint.setTypeface(display()); paint.setTextSize(size * scale); return paint.measureText(s) / scale; }
    /** a size by length: `big` up to `a` characters, `mid` up to `b`, else `small` */
    protected final int sizeFor(String s, int big, int mid, int small, int a, int b) { return s.length() <= a ? big : s.length() <= b ? mid : small; }

    // ---------------------------------------------------------------- the blocks

    /** the green block across the top, `hgt` design pixels high */
    protected final void band(float hgt) { root.addView(rect(L.deep), at(-1, hgt, 0, 0)); }
    /** a screen's title in a 200 px block: the big words, and a small saffron line at the end side (may be null) */
    protected final void title(String big, String small) {
        band(200);
        root.addView(text(big, 96, L.onDeep, true), at(1100, 120, 96, 54));
        if (small != null) { TextView s = text(small, 40, L.saff, false); s.setGravity(Gravity.END); s.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END); root.addView(s, atEnd(700, 56, 96, 84)); }
    }
    /** the bottom line: pairs of (key, what it does), so every screen says its next key in words */
    protected final void hints(String... kv) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setLayoutDirection(Dir.layout());
        for (int i = 0; i + 1 < kv.length; i += 2) {
            TextView key = centered(kv[i], 30, L.ink, false); key.setBackgroundColor(L.ink(0x24)); key.setPadding(px(16), 0, px(16), 0); key.setMinWidth(px(64));
            TextView w = text(kv[i + 1], 32, L.ink, false); w.setAlpha(.85f);
            LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(-2, px(48)); kp.setMarginEnd(px(12));
            row.addView(key, kp);
            LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-2, px(48)); wp.setMarginEnd(px(48)); w.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(w, wp);
        }
        root.addView(row, at(-1, 48, 96, 978));
    }
    /** a button: ink line, filled with ink when it has the focus */
    protected final TextView button(String label, boolean focused) {
        TextView v = centered(label, 44, focused ? L.onInk : L.ink, true);
        v.setSingleLine(true);
        v.setBackground(shape(focused ? L.ink : L.tile, L.ink, 6, false));
        return v;
    }
    /** a row of buttons from the start edge at `y`; the one at `sel` has the focus */
    protected final void buttons(String[] labels, int sel, float w, float y) {
        for (int i = 0; i < labels.length; i++) root.addView(button(labels[i], sel == i), at(w, 104, 96 + i * (w + 24), y));
    }

    /** the question block: the green block `hgt` high, a saffron line above (`meta`: «السؤال 3 من 10», whose turn) and the big words; returns the words' view */
    protected final TextView question(String meta, CharSequence words, float hgt) {
        band(hgt);
        if (meta != null) root.addView(text(meta, 40, L.saff, true), at(1400, 56, 96, 54));
        String s = words.toString();
        float boxH = hgt - 228;
        TextView q = text(words, fitSize(words, sizeFor(s, 104, 88, 72, 40, 70) - (s.length() > 110 ? 12 : 0), 1330, boxH, 3), L.onDeep, true);
        q.setMaxLines(3); q.setLineSpacing(0, .98f); q.setEllipsize(android.text.TextUtils.TruncateAt.END);
        root.addView(q, at(1330, boxH, 96, 128));
        return q;
    }

    /**
     * The biggest size (design pixels, down from `size`, never under 40) at which `words` stands in a box `w` x `h` design pixels in the heavy face with no line cut: a TextView
     * clips what is taller than its box, which cut the dots of a big letter and the line under a short block (the time bar's ticks sit right under it). The kit's one answer, so no game clips.
     */
    protected final float fitSize(CharSequence words, float size, float w, float h, int maxLines) {
        android.text.TextPaint paint = new android.text.TextPaint(); paint.setTypeface(display());
        for (float f = size; f > 40; f -= 4) {
            paint.setTextSize(f * scale);
            android.text.StaticLayout lay = new android.text.StaticLayout(words, paint, Math.max(1, px(w)), android.text.Layout.Alignment.ALIGN_NORMAL, .98f, 0, false);
            if (lay.getLineCount() <= maxLines && lay.getHeight() <= px(h)) return f;
        }
        return 40;
    }

    /**
     * A statement with one word hidden: `words` holds `mark` (three underscores) once. Hidden, the place is a dashed box as wide as the word (with `hint`
     * inside when given, the first letter); shown, the word stands there in saffron. Give the result to `question`.
     */
    protected final CharSequence blanked(String words, String mark, String word, String hint, boolean shown) {
        int at = words.indexOf(mark);
        if (at < 0) return words;
        if (shown) {
            android.text.SpannableString sp = new android.text.SpannableString(words.substring(0, at) + word + words.substring(at + mark.length()));
            sp.setSpan(new android.text.style.ForegroundColorSpan(L.saff), at, at + word.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return sp;
        }
        android.text.SpannableString sp = new android.text.SpannableString(words);
        sp.setSpan(new BlankSpan(Math.max(3, Math.min(10, word.length())), hint == null ? "" : hint, L.onDeep, L.saff, px(5)), at, at + mark.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sp;
    }
    /** the dashed box that stands for the hidden word */
    static final class BlankSpan extends android.text.style.ReplacementSpan {
        private final int letters, line, hintColor; private final String hint; private final float stroke;
        BlankSpan(int letters, String hint, int line, int hintColor, float stroke) { this.letters = letters; this.hint = hint; this.line = line; this.hintColor = hintColor; this.stroke = stroke; }
        @Override public int getSize(android.graphics.Paint p, CharSequence t, int s, int e, android.graphics.Paint.FontMetricsInt fm) { return Math.round(p.getTextSize() * (0.5f * letters + 0.6f)); }
        @Override public void draw(android.graphics.Canvas cv, CharSequence t, int s, int e, float x, int top, int y, int bottom, android.graphics.Paint p) {
            float size = p.getTextSize(), w = size * (0.5f * letters + 0.6f), pad = size * 0.12f;
            android.graphics.Paint q = new android.graphics.Paint(p);
            q.setStyle(android.graphics.Paint.Style.STROKE); q.setStrokeWidth(stroke); q.setColor(line);
            q.setPathEffect(new android.graphics.DashPathEffect(new float[]{stroke * 3, stroke * 2}, 0));
            cv.drawRect(x + pad, y - size * 0.82f, x + w - pad, y + size * 0.22f, q);
            if (!hint.isEmpty()) {
                android.graphics.Paint h2 = new android.graphics.Paint(p); h2.setColor(hintColor); h2.setTextAlign(android.graphics.Paint.Align.CENTER);
                cv.drawText(hint, x + w / 2f, y, h2);
            }
        }
    }

    /** the timer: a saffron box with the seconds at the end side of the block and one tick per second under the words; `set` each tick of the clock */
    protected final class TickTimer {
        private final TextView numView; private final List<View> ticks = new ArrayList<>(); private final int total; private int shown = -1;
        TickTimer(int totalSeconds, float ticksY) {
            total = Math.max(1, totalSeconds);
            FrameLayout tm = stamp(L.saff, 0, 0, false);
            numView = centered("", 128, L.onSaff, true);
            tm.addView(numView, new FrameLayout.LayoutParams(-1, -1));
            root.addView(tm, atEnd(232, 200, 96, 54));
            int n = Math.min(total, 30);   // 30 ticks at most fit the line; a longer clock shares them
            float tw = (1728 - (n - 1) * 12) / (float) n;
            for (int i = 0; i < n; i++) { View v = rect(L.tickOff); root.addView(v, at(tw, 28, 96 + i * (tw + 12), ticksY)); ticks.add(v); }
        }
        /** the seconds left; the last five are coral with the tick sound */
        void set(int seconds) {
            if (seconds == shown) return;
            shown = seconds;
            numView.setText(num(seconds));
            int on = (int) Math.ceil(seconds * (double) ticks.size() / total);
            for (int i = 0; i < ticks.size(); i++) ticks.get(i).setBackgroundColor(i < on ? (seconds <= 5 ? L.pink : L.saff) : L.tickOff);
            if (seconds > 0 && seconds <= 5) play(QuizSound.TICK);
        }
    }

    /** an answer stamp: an ink-lined tile with a letter tab at the start; right = green, wrong = flat and dashed */
    protected final FrameLayout answer(String tab, String words, int state) {
        boolean right = state == STAMP_RIGHT, wrong = state == STAMP_WRONG, focus = state == STAMP_FOCUS;
        FrameLayout st = stamp(right ? L.ok : wrong ? L.paper : focus ? L.saff : L.tile, wrong ? L.ink(0x59) : L.ink, wrong ? 4 : focus ? 12 : 6, wrong);
        st.addView(rect(wrong ? L.ink(0x99) : L.ink), new FrameLayout.LayoutParams(px(96), px(96), Gravity.START | Gravity.TOP));
        QuizActivity.InkLetter lt = new QuizActivity.InkLetter(this, tab, 64 * scale, L.onInk, display()); lt.setAlpha(wrong ? .6f : 1f);   // centred on the glyph's ink
        st.addView(lt, new FrameLayout.LayoutParams(px(96), px(96), Gravity.START | Gravity.TOP));
        TextView a = text(words, sizeFor(words, 68, 56, 44, 16, 30) - (words.length() > 50 ? 6 : 0), right ? L.onDeep : wrong ? L.ink(0x73) : focus ? L.onSaff : L.ink, true);
        a.setMaxLines(3); a.setEllipsize(android.text.TextUtils.TruncateAt.END);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1, -2, Gravity.START | Gravity.CENTER_VERTICAL); tp.setMarginStart(px(136)); tp.setMarginEnd(px(28));
        st.addView(a, tp);
        return st;
    }

    /** the sides' bar: one tile per side (colour square, name, points), the side whose turn it is filled with ink; 88 px high at `y`.
     *  From seven sides on a tile is too narrow for a name at a readable size: the colour square and the points only. */
    protected final void sidesBar(GameScore s, String[] names, int turn, float y) {
        float gap = 16, w = Math.min(560, (1728 - (s.n - 1) * gap) / s.n);
        boolean small = s.n > 4;
        for (int i = 0; i < s.n; i++) {
            boolean on = i == turn;
            LinearLayout t = new LinearLayout(this); t.setOrientation(LinearLayout.HORIZONTAL); t.setGravity(Gravity.CENTER_VERTICAL); t.setLayoutDirection(Dir.layout());
            t.setBackground(shape(on ? L.ink : L.tile, L.ink, 6, false)); t.setPadding(px(small ? 14 : 20), 0, px(small ? 14 : 20), 0);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(px(small ? 24 : 36), px(small ? 24 : 36)); sp.setMarginEnd(px(small ? 10 : 16));
            t.addView(box(GameLook.SIDES[i % GameLook.SIDES.length], on ? L.onInk : L.ink, 3, false), sp);
            float f = small ? 30 : 40;
            TextView nm = text(s.n > 6 ? "" : clip(names[i], f, w - (small ? 130 : 200)), f, on ? L.onInk : L.ink, true); nm.setSingleLine(true);
            t.addView(nm, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView sc = text(num(s.points[i]), small ? 40 : 56, on ? L.saff : L.ink, true); sc.setTextDirection(View.TEXT_DIRECTION_LTR);
            t.addView(sc);
            root.addView(t, at(w, 88, 96 + i * (w + gap), y));
        }
    }

    /** the rank rows from y 236: the place as a big numeral in an ink box (saffron for first), the side, what it just won («+10») and its total; shared places */
    protected final void board(GameScore s, String[] names, boolean withLast) {
        boolean small = s.n > 5, tiny = s.n > 8;   // 5 rows of 128, 8 of 80, 10 of 64: the last row always ends above y 960
        int hgt = tiny ? 64 : small ? 80 : 128; float step = tiny ? 72 : small ? 90 : 142, big = tiny ? 44 : small ? 56 : 88, nameSize = tiny ? 36 : small ? 44 : 64;
        int[] order = s.ranked();
        for (int i = 0; i < order.length; i++) {
            int side = order[i], rank = s.rank(side);
            LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setLayoutDirection(Dir.layout());
            row.setBackground(shape(L.tile, L.ink, 6, false));
            row.setPadding(px(6), px(6), px(6), px(6));   // the children stand inside the 6 px line
            TextView rk = centered(num(rank), big, rank == 1 ? L.onSaff : L.onInk, true); rk.setBackgroundColor(rank == 1 ? L.saff : L.ink);
            row.addView(rk, new LinearLayout.LayoutParams(px(128 - 6), px(hgt - 12)));
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(px(20), px(tiny ? 36 : small ? 44 : 64)); sp.setMarginStart(px(32)); sp.setMarginEnd(px(24));
            row.addView(box(GameLook.SIDES[side % GameLook.SIDES.length], L.ink, 3, false), sp);
            TextView nm = text(names[side], nameSize, L.ink, true); nm.setSingleLine(true); nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(nm, new LinearLayout.LayoutParams(0, -2, 1f));
            if (withLast && s.last[side] > 0) {
                TextView d = centered("⁦+" + num(s.last[side]) + "⁩", small ? 36 : 44, L.ok, true); d.setTextDirection(View.TEXT_DIRECTION_LTR);   // «+10», the plus on the left in both languages
                row.addView(d, new LinearLayout.LayoutParams(px(170), -2));
            }
            TextView sc = text(num(s.points[side]), big, L.ink, true); sc.setTextDirection(View.TEXT_DIRECTION_LTR); sc.setGravity(Gravity.END | Gravity.CENTER_VERTICAL); sc.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(px(280), -2); cp.setMarginEnd(px(32));
            row.addView(sc, cp);
            root.addView(row, at(1728, hgt, 96, 236 + i * step));
        }
    }

    /** the three buttons of every end screen */
    protected final String[] endLabels() { return new String[]{S.t("العب مرة أخرى"), S.t("تغيير الإعدادات"), S.t("خروج")}; }

    /** the end: «انتهت اللعبة», who won (or a tie, or nobody), the three buttons (`sel` has the focus), the podium of the first three and the fanfare's confetti */
    protected final void podium(GameScore s, String[] names, int sel) {
        int[] r = s.ranked();
        String small = s.n == 1 ? S.t("%s نقطة", num(s.points[0])) : s.nobody() ? S.t("لا فائز هذه المرة") : s.tie() ? S.t("تعادل في المركز الأول") : S.t("الفائز: %s", names[r[0]]);
        title(S.t("انتهت اللعبة"), small);
        buttons(endLabels(), sel, 520, 232);
        int[] cols = {L.ok, L.saff, 0xFFF08A6E}; float[] hs = {512, 400, 300}; int[] place = {1, 0, 2};   // 1st tallest and in the middle; on the screen 2nd, 1st, 3rd
        for (int k = 0; k < 3; k++) {
            int pi = place[k];
            if (pi >= r.length) continue;
            int side = r[pi];
            LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL); col.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            LinearLayout nameBar = new LinearLayout(this); nameBar.setOrientation(LinearLayout.HORIZONTAL); nameBar.setGravity(Gravity.CENTER_VERTICAL); nameBar.setBackgroundColor(L.ink); nameBar.setPadding(px(32), 0, px(32), 0); nameBar.setLayoutDirection(Dir.layout());
            TextView nm = text(clip(names[side], 56, 380), 56, L.onInk, true); nm.setSingleLine(true);
            nameBar.addView(nm, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView sc = text(num(s.points[side]), 48, L.saff, false); sc.setTextDirection(View.TEXT_DIRECTION_LTR); nameBar.addView(sc);
            col.addView(nameBar, new LinearLayout.LayoutParams(-1, px(96)));
            FrameLayout blk = stamp(cols[pi], L.ink, 6, false);
            blk.addView(centered(num(s.rank(side)), Math.min(340, hs[pi] * .75f), L.onSaff, true), new FrameLayout.LayoutParams(-1, -1));
            col.addView(blk, new LinearLayout.LayoutParams(-1, px(hs[pi])));
            FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(px(576), -2, Gravity.LEFT | Gravity.BOTTOM); cp.leftMargin = px(96 + k * 576); cp.bottomMargin = px(120);   // the blocks stand on y 960, inside the safe area
            root.addView(col, cp);
        }
        if (confetti && !s.nobody()) root.addView(new QuizActivity.ConfettiView(this), new FrameLayout.LayoutParams(-1, -1));
        hints("◀ ▶", S.t("الاختيار"), S.t("اختيار"), S.t("تأكيد"));
    }

    // ---------------------------------------------------------------- the quick setup

    /** the chips every game shares: «الصوت» (one switch for all the remote-only games) */
    protected final GameChips soundChip(GameChips c) { c.add("sound", S.t("الصوت"), new String[]{S.tv(SOUND_OPTS[0]), S.tv(SOUND_OPTS[1])}, 0); c.get("sound").picked = sound ? 0 : 1; return c; }
    /** the chips of last time for this game (call after every add) */
    protected final GameChips remembered(GameChips c) { c.load(GamePrefs.chips(this, id())); if (c.get("sound") != null) c.get("sound").picked = sound ? 0 : 1; c.row = c.rows.size(); return c; }

    /** the ready screen: up to seven rows of chips (tighter from the seventh) and the big start button, which has the focus when the screen opens (OK starts with last time's choices) */
    protected final void setup(GameChips c, String startLabel) {
        title(gameName(), S.t("جهّز اللعبة"));
        boolean tight = c.rows.size() > 6;
        float y = tight ? 224 : 232;
        for (int r = 0; r < c.rows.size(); r++) y = chipRow(c, r, y, tight ? 76 : 88, tight ? 14 : 14);
        root.addView(button(startLabel, c.onButton()), at(640, 104, 96, 856));
        hints("▲ ▼", S.t("السطر"), "◀ ▶", S.t("الاختيار"), S.t("اختيار"), c.onButton() ? startLabel : S.t("تأكيد"), S.t("رجوع"), S.t("الألعاب"));
    }
    /** one row: the label at the start, then the chips, as wide as their words; picked = saffron, the focus = ink, not offered = dashed */
    private float chipRow(GameChips c, int r, float y, float hgt, float gap) {
        GameChips.Row row = c.rows.get(r);
        float cx = 420, avail = 1728 - (cx - 96);
        root.addView(text(row.label, hgt >= 88 ? 44 : 40, L.ink, true), at(300, hgt, 96, y + 8));
        android.graphics.Paint paint = new android.graphics.Paint(); paint.setTypeface(display());
        float f = hgt >= 88 ? 40 : 36; float[] w = new float[row.opts.length]; float total;
        do {
            paint.setTextSize(f * scale); total = 14 * (row.opts.length - 1);
            for (int i = 0; i < row.opts.length; i++) { w[i] = Math.max(112, paint.measureText(row.opts[i]) / scale + 56); total += w[i]; }
            if (total > avail) f -= 2;
        } while (total > avail && f > 26);
        float x = cx;
        for (int i = 0; i < row.opts.length; i++) {
            boolean off = row.off[i], focus = c.row == r && c.col == i, isSel = i == row.picked;
            int fill = focus ? L.ink : isSel ? L.saff : off ? L.paper : L.tile;
            int words = focus ? (isSel ? L.saff : L.onInk) : off ? L.ink(0x88) : isSel ? L.onSaff : L.ink;   // standing on the picked one: saffron words and a saffron frame
            FrameLayout chip = stamp(fill, focus && isSel ? L.saff : off ? L.ink(0x88) : L.ink, focus && isSel ? 12 : 6, off);
            TextView tv = centered(row.opts[i], f, words, true); tv.setSingleLine(true);
            chip.addView(tv, new FrameLayout.LayoutParams(-1, -1));
            root.addView(chip, at(w[i], hgt, x, y));
            x += w[i] + 14;
        }
        return y + hgt + gap;
    }
    /** the keys of the ready screen: true when OK was pressed on the start button. Back leaves the game; a picked chip is remembered and `onChips` runs. */
    protected final boolean keySetup(GameChips c, boolean back, boolean ok, int dx, int dy) {
        if (back) { finish(); return false; }
        if (dx != 0 || dy != 0) { if (c.move(dx, dy)) redraw(); return false; }
        if (!ok) return false;
        if (c.onButton()) return true;
        if (!c.pick()) { toast(S.t("قريبًا")); return false; }
        if (c.get("sound") != null) { sound = c.picked("sound") == 0; GamePrefs.setSound(this, sound); }
        onChips(c);
        GamePrefs.saveChips(this, id(), c.save());
        redraw();
        return false;
    }
    /** a chip was picked: switch off what can no longer be offered (GameChips.setOff) */
    protected void onChips(GameChips c) { }

    /** the keys of a row of `n` buttons at `sel`: the new focus, or -1 - index when OK chose one (so `-1 - v` is the choice) */
    protected final int keyButtons(int sel, int n, boolean ok, int d) {
        if (ok) return -1 - sel;
        return Math.max(0, Math.min(n - 1, sel + d));
    }
}
