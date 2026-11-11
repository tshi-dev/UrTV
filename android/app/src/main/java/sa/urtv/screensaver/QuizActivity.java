package sa.urtv.screensaver;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * «الأسرع», the TV side (docs/QUIZ.md, design/43 quiz-B*): the chips, the lobby with the QR and the room code, the question, the reveal, the leaderboard, the
 * podium. The rules live in QuizRoom, the game-only server in QuizServer; this class draws them in the «ورقة» look at the 1920 x 1080 design, scaled to the
 * screen, and reads the remote. The screen is held awake from the lobby to the end (so no screensaver starts mid-game) and released when the activity goes.
 * Low-RAM TVs: no animation, no confetti, no sounds' extra copies; the same screens otherwise.
 */
public class QuizActivity extends Activity {
    static final int PAPER = 0xFFEFE8DA, DEEP = 0xFF0E5A45, SAFF = 0xFFF2A93B, INK = 0xFF16140F, TILE = 0xFFFBF8F1, OKB = 0xFF17A673, PINK = 0xFFE4572E, TICKOFF = 0xFF0A4A39;
    static final int[] PLAYER = {0xFF5AA9F0, 0xFFF2705A, 0xFF7BD88F, 0xFFC28BFF, 0xFFFFD15C, 0xFF4FD1C5, 0xFFFFB86B, 0xFF8FD6FF};
    /** one string: the game's working name (the owner has not confirmed it) */
    static final String NAME = S.n("الأسرع");
    static final String[] LETTERS_AR = {"أ", "ب", "ج", "د"}, LETTERS_EN = {"A", "B", "C", "D"};
    static final int INTRO_MS = 1600, REVEAL_VIEW_MS = 4000, BOARD_ROWS = 5, LIST_ROWS = 6;

    enum Screen { SETUP, LOBBY, INTRO, QUESTION, REVEAL, BOARD, END, LIST, PAUSE }

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Map<String, Typeface> fonts = new HashMap<>();
    private Prefs prefs; private boolean latin, en, rtl, lowRam, animate; private float scale;
    private FrameLayout root; private List<QuizPick.Item> bank = new ArrayList<>();
    private Screen screen = Screen.SETUP; private int sel, selRow;
    // the chips
    private String cat; private int level, countIdx; private boolean kids, sound;
    // the game
    private QuizRoom room; private QuizServer server; private String url = "", code = ""; private long roomOpened, introAt; private List<QuizPick.Item> asked;
    private int shownVersion = -1, shownIndex = -2; private QuizRoom.Phase shownPhase; private long lastSecond = -1; private int lastPlayers = -1;
    // live parts of the question screen
    private TextView timerNum, answeredTxt; private final List<View> ticks = new ArrayList<>(); private LinearLayout answeredSquares; private int shownAnswered = -1;
    private final List<View> items = new ArrayList<>(); private String armedKick = "";

    private final Runnable loop = new Runnable() {
        @Override public void run() { step(); h.postDelayed(this, 100); }
    };

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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // no screensaver and no sleep from the lobby to the podium
        prefs = new Prefs(this);
        latin = DigitsText.isLatin(prefs.digits()); en = S.en(); rtl = Dir.rtl();
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        lowRam = am != null && am.isLowRamDevice();
        float anim = 1f;
        try { anim = Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f); } catch (RuntimeException ignored) { /* default */ }
        animate = !lowRam && anim > 0f;   // reduced motion (animations off in the TV's developer settings) and low-RAM TVs: none
        scale = getResources().getDisplayMetrics().widthPixels / 1920f;
        bank = QuizBank.load(this);
        QuizBank.refresh(this);
        cat = QuizBank.category(this); level = QuizBank.level(this); countIdx = QuizBank.count(this); kids = QuizBank.kids(this); sound = QuizBank.sound(this);
        root = new FrameLayout(this);
        root.setLayoutDirection(Dir.layout());
        root.setBackgroundColor(PAPER);
        setContentView(root);
        fixCat();
        show(Screen.SETUP);
    }

    @Override protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // again after every return: the podium holds the screen on too
        h.post(loop);
    }
    /** HOME (or any other app in front) in the middle of a game: the phones are told «اللعبة متوقفة» at once instead of staying on the last screen */
    @Override protected void onPause() {
        super.onPause();
        h.removeCallbacks(loop);
        if (room != null && (room.phase == QuizRoom.Phase.QUESTION || room.phase == QuizRoom.Phase.REVEAL)) room.pause();
    }
    @Override protected void onDestroy() { closeRoom(); super.onDestroy(); }

    // ---------------------------------------------------------------- the clock

    private long netCheckAt;
    private void step() {
        if (room == null) return;
        long now = System.currentTimeMillis();
        if (now - netCheckAt > 2000) {   // the TV lost its local network: the room closes (the phones cannot reach it any more and say so)
            netCheckAt = now;
            if (lanAddress() == null) { closeRoom(); shownPhase = null; Toast.makeText(this, S.t("انقطعت الشبكة، أُغلقت الغرفة"), Toast.LENGTH_LONG).show(); show(Screen.SETUP); return; }
        }
        room.tick();
        QuizRoom.Phase p = room.phase;
        if (screen == Screen.INTRO && System.currentTimeMillis() - introAt >= INTRO_MS) { startRound(); return; }
        if (p != shownPhase || room.index != shownIndex) {
            shownPhase = p; shownIndex = room.index;
            if (p == QuizRoom.Phase.QUESTION) show(Screen.QUESTION);
            else if (p == QuizRoom.Phase.REVEAL) { show(Screen.REVEAL); QuizSound.play(sound, anyRight() ? QuizSound.CHIME : QuizSound.LOW); }
            else if (p == QuizRoom.Phase.END && screen != Screen.LIST) { show(Screen.END); QuizSound.play(sound, QuizSound.FANFARE); }
            else if (p == QuizRoom.Phase.PAUSED) show(Screen.PAUSE);
            else if (p == QuizRoom.Phase.LOBBY && screen != Screen.SETUP && screen != Screen.INTRO) show(Screen.LOBBY);
            return;
        }
        if (screen == Screen.LOBBY && (room.version() != shownVersion)) { shownVersion = room.version(); show(Screen.LOBBY); return; }
        if (screen == Screen.QUESTION) liveQuestion();
        if (screen == Screen.REVEAL && room.revealLeftMs() <= QuizRoom.REVEAL_MS - REVEAL_VIEW_MS) show(Screen.BOARD);
    }

    private boolean anyRight() { for (QuizRoom.Player p : room.players()) if (Boolean.TRUE.equals(p.lastOk)) return true; return false; }

    // ---------------------------------------------------------------- keys

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(e);
        int k = e.getKeyCode();
        boolean back = k == KeyEvent.KEYCODE_BACK || k == KeyEvent.KEYCODE_ESCAPE;
        boolean ok = k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_NUMPAD_ENTER || k == KeyEvent.KEYCODE_BUTTON_A;
        int dx = k == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : k == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : 0;
        if (rtl) dx = -dx;   // «next» is toward the end of the line: left in Arabic
        int dy = k == KeyEvent.KEYCODE_DPAD_UP ? -1 : k == KeyEvent.KEYCODE_DPAD_DOWN ? 1 : 0;
        if (!(back || ok || dx != 0 || dy != 0)) return super.dispatchKeyEvent(e);
        switch (screen) {
            case SETUP: keySetup(back, ok, dx, dy); break;
            case LOBBY: keyLobby(back, ok, dx, dy); break;
            case INTRO: break;
            case QUESTION: if (back) room.pause(); break;
            case REVEAL: case BOARD: if (back) room.pause(); else if (ok) { if (screen == Screen.REVEAL) show(Screen.BOARD); else room.skipReveal(); } break;
            case PAUSE: keyButtons(back, ok, dx, dy, 3); break;
            case END: keyButtons(back, ok, dx, dy, 3); if (dy > 0) show(Screen.LIST); break;
            case LIST: if (back || ok) show(Screen.END); else if (dy != 0) { sel = Math.max(0, Math.min(Math.max(0, room.players().size() - LIST_ROWS), sel + dy * LIST_ROWS)); show(Screen.LIST); } break;
            default: break;
        }
        return true;
    }

    // ---------------------------------------------------------------- building blocks

    private int px(float v) { return Math.round(v * scale); }
    private Typeface font(String n) {
        Typeface t = fonts.get(n);
        if (t == null) { try { t = Typeface.createFromAsset(getAssets(), "fonts/" + n + ".otf"); } catch (RuntimeException e) { t = Typeface.DEFAULT; } fonts.put(n, t); }
        return t;
    }
    private String num(int v) { return DigitsText.convert(String.valueOf(v), latin); }

    /** a view placed on the 1920 x 1080 design: x from the start edge, y from the top */
    private FrameLayout.LayoutParams at(float w, float hgt, float x, float y) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(w < 0 ? -1 : px(w), hgt < 0 ? -1 : px(hgt), Gravity.START | Gravity.TOP);
        p.setMarginStart(px(x)); p.topMargin = px(y);
        return p;
    }
    private FrameLayout.LayoutParams atEnd(float w, float hgt, float x, float y) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(px(w), px(hgt), Gravity.END | Gravity.TOP);
        p.setMarginEnd(px(x)); p.topMargin = px(y);
        return p;
    }
    private View rect(int fill) { View v = new View(this); v.setBackgroundColor(fill); return v; }
    private View box(int fill, int stroke, float strokePx, boolean dashed) {
        View v = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        if (strokePx > 0) { if (dashed) g.setStroke(px(strokePx), stroke, px(14), px(10)); else g.setStroke(px(strokePx), stroke); }
        v.setBackground(g);
        return v;
    }
    private TextView text(String s, float size, int color, boolean display) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextColor(color); v.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * scale);
        v.setTypeface(font(display ? "ThmanyahDisplay-Bold" : "Thmanyah-Medium"));
        v.setIncludeFontPadding(false); v.setTextDirection(Dir.text()); v.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        return v;
    }
    private TextView centered(String s, float size, int color, boolean display) { TextView v = text(s, size, color, display); v.setGravity(Gravity.CENTER); v.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); return v; }
    private FrameLayout stamp(int fill, int stroke, float strokePx, boolean dashed) { FrameLayout f = new FrameLayout(this); GradientDrawable g = new GradientDrawable(); g.setColor(fill); if (strokePx > 0) { if (dashed) g.setStroke(px(strokePx), stroke, px(14), px(10)); else g.setStroke(px(strokePx), stroke); } f.setBackground(g); return f; }

    private void clear() { root.removeAllViews(); items.clear(); ticks.clear(); timerNum = null; answeredTxt = null; answeredSquares = null; shownAnswered = -1; }
    private void band(float h2) { root.addView(rect(DEEP), at(-1, h2, 0, 0)); }
    private void title(String big, String small) {
        band(200);
        root.addView(text(big, 96, 0xFFFFFFFF, true), at(1100, 120, 96, 54));
        if (small != null) { TextView s = text(small, 40, SAFF, false); s.setGravity(Gravity.END); s.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END); root.addView(s, atEnd(700, 56, 96, 84)); }
    }
    private void hints(String... kv) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setLayoutDirection(Dir.layout());
        for (int i = 0; i + 1 < kv.length; i += 2) {
            TextView key = centered(kv[i], 30, INK, false); key.setBackgroundColor(0x24161410); key.setPadding(px(16), 0, px(16), 0); key.setMinWidth(px(64));
            TextView w = text(kv[i + 1], 32, INK, false); w.setAlpha(.85f);
            LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(-2, px(48)); kp.setMarginEnd(px(12));
            row.addView(key, kp);
            LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-2, px(48)); wp.setMarginEnd(px(48)); w.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(w, wp);
        }
        root.addView(row, at(-1, 48, 96, 978));
    }
    /** a name cut with «…» so that it fits `maxW` design pixels at `size` (measured with the game's own font; never a bare cut-off) */
    private String clip(String name, float size, float maxW) {
        android.graphics.Paint paint = new android.graphics.Paint(); paint.setTypeface(font("ThmanyahDisplay-Bold")); paint.setTextSize(size * scale);
        if (paint.measureText(name) <= maxW * scale) return name;
        String cut = name;
        while (cut.length() > 1 && paint.measureText(cut + "…") > maxW * scale) cut = cut.substring(0, cut.length() - 1);
        return cut.trim() + "…";
    }
    private int chooseFont(String s, int big, int mid, int small, int a, int b) { return s.length() <= a ? big : s.length() <= b ? mid : small; }
    private TextView button(String label, boolean focused, int w) {
        TextView v = centered(label, 44, focused ? PAPER : INK, true);
        GradientDrawable g = new GradientDrawable(); g.setColor(focused ? INK : TILE); g.setStroke(px(6), INK); v.setBackground(g);
        return v;
    }
    private String letter(int i) { return en ? LETTERS_EN[i] : LETTERS_AR[i]; }

    // ---------------------------------------------------------------- screens

    private void show(Screen s) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // every screen, the podium included
        if (s != screen) { sel = s == Screen.LOBBY ? -1 : 0; selRow = 0; armedKick = ""; }
        screen = s;
        clear();
        root.setBackgroundColor(PAPER);
        switch (s) {
            case SETUP: drawSetup(); break;
            case LOBBY: drawLobby(); break;
            case INTRO: drawIntro(); break;
            case QUESTION: drawQuestion(false); break;
            case REVEAL: drawQuestion(true); break;
            case BOARD: drawBoard(); break;
            case END: drawEnd(); break;
            case LIST: drawList(); break;
            case PAUSE: drawPause(); break;
            default: break;
        }
    }

    // ---- the chips (B8)
    private static final String[] KIDS_OPTS = {S.n("لا"), S.n("نعم")}, SOUND_OPTS = {S.n("يعمل"), S.n("صامت")};
    /** the chips' words (QuizPick has no Android in it, so they are marked here for the English table) */
    private static final String[] CHIP_WORDS = {S.n("مزيج"), S.n("جغرافيا"), S.n("علوم"), S.n("إسلامية"), S.n("رياضيات ومنطق"), S.n("حيوانات"), S.n("السعودية والخليج"), S.n("سهل"), S.n("متوسط"), S.n("صعب"), S.n("متنوع")};
    private int levelValue() { return QuizPick.LEVEL_VALUE[level]; }
    private int wanted() { return QuizPick.COUNTS[countIdx]; }
    private boolean catOffered(String key) { return QuizPick.offered(bank, key, levelValue(), kids, wanted()); }
    private void fixCat() { if (!catOffered(cat)) cat = QuizPick.MIX; }

    private void drawSetup() {
        title(S.t("جهّز اللعبة"), S.tv(NAME));
        // rows: 0 category, 1 level, 2 questions, 3 kids, 4 sound, 5 the button
        float y = 232;
        y = chipRow(0, S.t("الفئة"), catLabels(), catIndex(), y, true);
        y = chipRow(1, S.t("المستوى"), levelLabels(), level, y, false);
        y = chipRow(2, S.t("الأسئلة"), new String[]{num(5), num(10), num(15)}, countIdx, y, false);
        y = chipRow(3, S.t("للأطفال"), new String[]{S.tv(KIDS_OPTS[0]), S.tv(KIDS_OPTS[1])}, kids ? 1 : 0, y, false);
        y = chipRow(4, S.t("الصوت"), new String[]{S.tv(SOUND_OPTS[0]), S.tv(SOUND_OPTS[1])}, sound ? 0 : 1, y, false);
        TextView go = button(S.t("افتح الغرفة"), selRow == 5, 640);
        root.addView(go, at(640, 104, 96, 856));
        hints("▲ ▼", S.t("السطر"), "◀ ▶", S.t("الاختيار"), S.t("اختيار"), S.t("ابدأ"));
    }
    private String[] catLabels() { String[] v = new String[QuizPick.CATEGORIES.length]; for (int i = 0; i < v.length; i++) v[i] = S.tv(QuizPick.CATEGORIES[i][1]); return v; }
    private String[] levelLabels() { String[] v = new String[QuizPick.LEVELS.length]; for (int i = 0; i < v.length; i++) v[i] = S.tv(QuizPick.LEVELS[i]); return v; }
    private int catIndex() { for (int i = 0; i < QuizPick.CATEGORIES.length; i++) if (QuizPick.CATEGORIES[i][0].equals(cat)) return i; return 0; }

    /** one row of chips: the label above (the first row, long) or at the start; selected = saffron, the focus = ink, not offered = dashed.
     *  The chips are as wide as their words (measured), and the font shrinks a little when a row would not fit the 1728 px of the design. */
    private float chipRow(int row, String label, String[] opts, int picked, float y, boolean stacked) {
        float lx = 96, cx = stacked ? 96 : 420, cy = y, avail = 1728 - (cx - 96);
        TextView l = text(label, 44, INK, true);
        if (stacked) { root.addView(l, at(900, 56, lx, y)); cy = y + 64; } else root.addView(l, at(300, 88, lx, y + 8));
        android.graphics.Paint paint = new android.graphics.Paint(); paint.setTypeface(font("ThmanyahDisplay-Bold"));
        float f = 40; float[] w = new float[opts.length]; float total;
        do {
            paint.setTextSize(f * scale); total = 14 * (opts.length - 1);
            for (int i = 0; i < opts.length; i++) { w[i] = Math.max(112, paint.measureText(opts[i]) / scale + 56); total += w[i]; }
            if (total > avail) f -= 2;
        } while (total > avail && f > 26);
        float x = cx;
        for (int i = 0; i < opts.length; i++) {
            boolean off = row == 0 && !catOffered(QuizPick.CATEGORIES[i][0]);
            boolean focus = selRow == row && sel == i;
            boolean isSel = i == picked;
            int fill = focus ? INK : isSel ? SAFF : off ? PAPER : TILE;
            int txt = focus ? (isSel ? SAFF : PAPER) : off ? 0x88161410 : INK;   // standing on the picked one: saffron words and a saffron frame; on another: paper words
            FrameLayout chip = stamp(fill, focus && isSel ? SAFF : off ? 0x88161410 : INK, focus && isSel ? 12 : 6, off);
            TextView tv = centered(opts[i], f, txt, true); tv.setSingleLine(true);
            chip.addView(tv, new FrameLayout.LayoutParams(-1, -1));
            root.addView(chip, at(w[i], 88, x, cy));
            x += w[i] + 14;
        }
        return stacked ? cy + 88 + 20 : y + 88 + 14;
    }

    private void keySetup(boolean back, boolean ok, int dx, int dy) {
        int rows = 6;
        if (back) { finish(); return; }
        if (dy != 0) { selRow = Math.max(0, Math.min(rows - 1, selRow + dy)); sel = clampSel(selRow, selRowPick(selRow)); show(Screen.SETUP); return; }
        if (selRow == 5) { if (ok) openRoom(); return; }
        int n = rowSize(selRow);
        if (dx != 0) { sel = Math.max(0, Math.min(n - 1, sel + dx)); show(Screen.SETUP); return; }
        if (ok) choose(selRow, sel);
    }
    private int rowSize(int r) { return r == 0 ? QuizPick.CATEGORIES.length : r == 1 ? QuizPick.LEVELS.length : r == 2 ? 3 : 2; }
    private int selRowPick(int r) { return r == 0 ? catIndex() : r == 1 ? level : r == 2 ? countIdx : r == 3 ? (kids ? 1 : 0) : r == 4 ? (sound ? 0 : 1) : 0; }
    private int clampSel(int r, int v) { return Math.max(0, Math.min(Math.max(0, rowSize(Math.min(r, 4)) - 1), v)); }
    private void choose(int row, int i) {
        if (row == 0) { if (!catOffered(QuizPick.CATEGORIES[i][0])) { Toast.makeText(this, S.t("قريبًا"), Toast.LENGTH_SHORT).show(); return; } cat = QuizPick.CATEGORIES[i][0]; }
        else if (row == 1) { level = i; fixCat(); }
        else if (row == 2) { countIdx = i; fixCat(); }
        else if (row == 3) { kids = i == 1; fixCat(); }
        else if (row == 4) { sound = i == 0; }
        QuizBank.save(this, cat, level, countIdx, kids, sound);
        show(Screen.SETUP);
    }

    // ---- the room: QR, code, players (B7)
    private void openRoom() {
        if (room != null) { show(Screen.LOBBY); return; }
        InetAddress lan = lanAddress();
        if (lan == null) { Toast.makeText(this, S.t("لا توجد شبكة محلية. وصّل التلفزيون بالواي فاي"), Toast.LENGTH_LONG).show(); return; }
        Random rnd = new java.security.SecureRandom();   // the room code and the players' tokens are not guessable from a seed
        room = new QuizRoom(QuizRoom.freshCode(rnd, System.currentTimeMillis()), System::currentTimeMillis, rnd);
        room.latinDigits = latin;   // «سارة 2», not «سارة ٢», when the person's digits are Latin
        server = new QuizServer(room, new QuizServer.Assets() {
            @Override public byte[] page() { return asset("quiz/play.html"); }
            @Override public byte[] font(String n) { return asset("fonts/" + n + ".woff2"); }
        }, true);
        try {
            int port = server.start(lan);
            code = room.code;
            url = "http://" + lan.getHostAddress() + ":" + port + "/" + code;
        } catch (java.io.IOException e) {
            closeRoom();
            Toast.makeText(this, S.t("تعذّر فتح الغرفة"), Toast.LENGTH_LONG).show();
            return;
        }
        roomOpened = System.currentTimeMillis(); shownPhase = QuizRoom.Phase.LOBBY; shownVersion = -1; shownIndex = -1;
        show(Screen.LOBBY);
    }
    private byte[] asset(String name) {
        try (java.io.InputStream in = getAssets().open(name)) { return Bounded.readAll(in, 4 << 20); } catch (Exception e) { return null; }
    }
    /** the TV's own address on the local network (Wi-Fi or cable): a private IPv4 one, never a public address */
    static InetAddress lanAddress() {
        try {
            Enumeration<NetworkInterface> all = NetworkInterface.getNetworkInterfaces();
            InetAddress any = null;
            while (all != null && all.hasMoreElements()) {
                NetworkInterface n = all.nextElement();
                if (!n.isUp() || n.isLoopback() || n.isVirtual()) continue;
                for (InetAddress a : Collections.list(n.getInetAddresses())) {
                    if (!(a instanceof Inet4Address) || !a.isSiteLocalAddress()) continue;
                    if (n.getName().startsWith("wlan") || n.getName().startsWith("eth")) return a;
                    if (any == null) any = a;
                }
            }
            return any;
        } catch (java.net.SocketException e) { return null; }
    }
    private void closeRoom() {
        if (server != null) { server.stop(); server = null; }
        if (room != null) { room.close(); room = null; }
    }

    private void drawLobby() {
        band(300);
        root.addView(text(S.tv(NAME), 128, 0xFFFFFFFF, true), at(1200, 160, 96, 54));
        root.addView(text(S.t("الكل يجاوب من جواله، والأسرع يكسب"), 40, SAFF, false), at(1300, 56, 96, 214));
        FrameLayout count = stamp(SAFF, 0, 0, false);
        count.addView(centered(num(room.activeCount()), 112, INK, true), new FrameLayout.LayoutParams(-1, px(140), Gravity.TOP));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, px(44), Gravity.BOTTOM); lp.bottomMargin = px(12);
        count.addView(centered(S.t("لاعبين"), 36, INK, false), lp);
        root.addView(count, atEnd(232, 200, 96, 54));
        // the code, the QR and the address at the start side
        FrameLayout codeBox = stamp(SAFF, INK, 6, false);
        codeBox.addView(centered(DigitsText.convert(code, latin), 168, INK, true), new FrameLayout.LayoutParams(-1, -1));
        root.addView(codeBox, at(620, 208, 96, 330));
        FrameLayout qrBox = stamp(0xFFFFFFFF, INK, 6, false);
        QrView qr = new QrView(this, QrCode.encode(url + (en ? "?l=en" : "")));
        FrameLayout.LayoutParams qp = new FrameLayout.LayoutParams(-1, -1); qp.setMargins(px(14), px(14), px(14), px(14));
        qrBox.addView(qr, qp);
        root.addView(qrBox, at(288, 288, 96, 562));
        TextView hint = text(S.t("امسح الرمز بكاميرا الجوال، أو افتح العنوان"), 34, 0xCC161410, false);
        hint.setMaxLines(3);
        root.addView(hint, at(310, 130, 410, 562));
        // the whole address, host:port/code, left to right and isolated (a right-to-left line drops the last part), big, under the QR
        TextView addr = text("\u2066" + url.replace("http://", "") + "\u2069", 46, INK, true); addr.setTextDirection(View.TEXT_DIRECTION_LTR); addr.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        addr.setSingleLine(true); addr.setHorizontallyScrolling(false);
        root.addView(addr, at(700, 64, 96, 862));
        if (room.activeCount() < 2 && System.currentTimeMillis() - roomOpened > 60000) {
            TextView need = text(S.t("هذي اللعبة تحتاج جوالات، ادخلوا بالجوال"), 34, PINK, true); need.setMaxLines(2);
            root.addView(need, at(700, 80, 96, 934));
        }
        // the players, two columns; the host's focus: the start button or a tile (OK twice removes it)
        List<QuizRoom.Player> ps = room.players();
        boolean compact = ps.size() > 6;
        float rowH = compact ? 72 : 104, gap = compact ? 14 : 16, colW = 492;
        for (int i = 0; i < ps.size(); i++) {
            QuizRoom.Player p = ps.get(i);
            boolean armed = p.token.equals(armedKick), focus = sel == i;
            FrameLayout st = stamp(TILE, focus ? SAFF : INK, 6, false);
            if (focus) st.setBackground(ring(TILE, INK, SAFF));
            View sq = box(PLAYER[p.color % 8], INK, 3, false);
            FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(px(compact ? 32 : 40), px(compact ? 32 : 40), Gravity.START | Gravity.CENTER_VERTICAL); sp.setMarginStart(px(20));
            st.addView(sq, sp);
            String shown = armed ? S.t("أزل؟") : clip(p.name, compact ? 40 : 52, colW - (compact ? 72 : 84) - 12 - 8);
            TextView nm = text(shown, compact ? 40 : 52, armed ? PINK : INK, true); nm.setSingleLine(true); nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
            FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(-1, -2, Gravity.START | Gravity.CENTER_VERTICAL); np.setMarginStart(px(compact ? 72 : 84)); np.setMarginEnd(px(12));
            st.addView(nm, np);
            if (!room.connected(p)) st.setAlpha(.45f);
            int col = i % 2, r = i / 2;
            root.addView(st, atEnd(colW, rowH, 96 + (1 - col) * (colW + 16), 330 + r * (rowH + gap)));
            if (animate && p.joinedAt > System.currentTimeMillis() - 600) { st.setScaleX(.6f); st.setScaleY(.6f); st.animate().scaleX(1f).scaleY(1f).setDuration(240).start(); }
            items.add(st);
        }
        int rows = (ps.size() + 1) / 2;
        float by = Math.max(330 + 3 * (104 + 16) + 24, 330 + rows * (rowH + gap) + 24);
        boolean can = room.canStart();
        TextView go = button(can ? S.t("ابدأ") : S.t("ابدأ (لاعبان على الأقل)"), sel == -1, 1000);
        if (!can) go.setAlpha(.55f);
        go.setTextSize(TypedValue.COMPLEX_UNIT_PX, (can ? 56 : 44) * scale);
        root.addView(go, atEnd(1000, Math.min(120, 960 - by + 0), 96, Math.min(by, 840)));
        hints(S.t("اختيار"), can ? S.t("ابدأ") : S.t("ابدأ (لاعبان على الأقل)"), S.t("رجوع"), S.t("أغلق الغرفة"));
    }
    private GradientDrawable ring(int fill, int stroke, int outer) {
        GradientDrawable g = new GradientDrawable(); g.setColor(fill); g.setStroke(px(10), outer); return g;
    }

    private void keyLobby(boolean back, boolean ok, int dx, int dy) {
        List<QuizRoom.Player> ps = room.players();
        if (back) { closeRoom(); shownPhase = null; show(Screen.SETUP); return; }
        if (dx != 0 || dy != 0) {
            int n = ps.size();
            if (sel == -1) { if (dy < 0 && n > 0) sel = n - 1; }
            else {
                int step = dy != 0 ? 2 * dy : dx;
                int to = sel + step;
                sel = to >= n ? -1 : Math.max(0, to);
            }
            armedKick = "";
            show(Screen.LOBBY);
            return;
        }
        if (!ok) return;
        if (sel == -1) {
            if (room.canStart()) beginIntro(); else Toast.makeText(this, S.t("ابدأ (لاعبان على الأقل)"), Toast.LENGTH_SHORT).show();
        } else if (sel < ps.size()) {
            QuizRoom.Player p = ps.get(sel);
            if (p.token.equals(armedKick)) { room.kick(p.token); armedKick = ""; sel = -1; } else armedKick = p.token;
            show(Screen.LOBBY);
        }
    }

    private void beginIntro() { screen = Screen.INTRO; introAt = System.currentTimeMillis(); show(Screen.INTRO); }
    private void drawIntro() {
        root.setBackgroundColor(DEEP);
        TextView big = centered(S.t("جاهزين؟"), 200, 0xFFFFFFFF, true);
        root.addView(big, new FrameLayout.LayoutParams(-1, -1));
        TextView n = centered(S.tv(NAME), 56, SAFF, true);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-1, px(80), Gravity.BOTTOM); p.bottomMargin = px(200);
        root.addView(n, p);
    }
    /** the bank's Arabic text stores Arabic-Indic digits: with Latin digits set, the question and the four answers (on the TV and on the phones) use 0-9; a sum inside the Arabic sentence is isolated left to right */
    private QuizRoom.Q digits(QuizRoom.Q q) {
        if (en) return q;
        String[] o = new String[q.opts.length];
        for (int i = 0; i < o.length; i++) o[i] = QuizRoom.isolateMath(latin ? DigitsText.convert(q.opts[i], true) : q.opts[i]);
        return new QuizRoom.Q(QuizRoom.isolateMath(latin ? DigitsText.convert(q.text, true) : q.text), o, q.correct);
    }
    private void startRound() {
        Random rnd = new Random();
        List<QuizPick.Item> picks = QuizPick.pick(bank, cat, levelValue(), kids, wanted(), QuizBank.recent(this), rnd);
        List<QuizRoom.Q> qs = new ArrayList<>();
        for (QuizPick.Item it : picks) qs.add(digits(QuizPick.toQ(it, en, rnd)));
        screen = Screen.LOBBY;
        if (qs.isEmpty() || !room.start(qs)) { Toast.makeText(this, S.t("ابدأ (لاعبان على الأقل)"), Toast.LENGTH_SHORT).show(); show(Screen.LOBBY); return; }
        asked = picks;
        QuizBank.remember(this, picks);
        shownPhase = null;   // step() draws the first question
    }

    // ---- the question and its reveal (B1, B2)
    private void drawQuestion(boolean reveal) {
        QuizRoom.Q q = room.current();
        if (q == null) return;
        band(528);
        LinearLayout meta = new LinearLayout(this); meta.setOrientation(LinearLayout.HORIZONTAL); meta.setGravity(Gravity.CENTER_VERTICAL); meta.setLayoutDirection(Dir.layout());
        TextView mt = text(S.t("السؤال %s من %s", num(room.index + 1), num(room.total())), 40, SAFF, true);
        meta.addView(mt);
        if (!reveal) {
            answeredSquares = new LinearLayout(this); answeredSquares.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-2, px(28)); ap.setMarginStart(px(32));
            meta.addView(answeredSquares, ap);
            fillAnswered();
        }
        root.addView(meta, at(1100, 56, 96, 54));
        FrameLayout tm = stamp(SAFF, 0, 0, false);
        if (!reveal) { timerNum = centered(num((int) Math.ceil(room.leftMs() / 1000.0)), 128, INK, true); tm.addView(timerNum, new FrameLayout.LayoutParams(-1, -1)); }
        else tm.addView(new CheckView(this), new FrameLayout.LayoutParams(-1, -1));
        root.addView(tm, atEnd(232, 200, 96, 54));
        int len = q.text.length();
        TextView qt = text(q.text, chooseFont(q.text, 104, 88, 72, 40, 70) - (len > 110 ? 12 : 0), 0xFFFFFFFF, true);
        qt.setMaxLines(3); qt.setLineSpacing(0, .98f); qt.setEllipsize(android.text.TextUtils.TruncateAt.END);
        root.addView(qt, at(1330, 300, 96, 128));
        if (!reveal) {
            float tw = (1728 - 19 * 12) / 20f;
            for (int i = 0; i < 20; i++) { View v = rect(TICKOFF); root.addView(v, at(tw, 28, 96 + i * (tw + 12), 452)); ticks.add(v); }
            lastSecond = -1; liveQuestion();
        } else {
            QuizRoom.Player fast = null;
            for (QuizRoom.Player p : room.players()) if (Boolean.TRUE.equals(p.lastOk) && (fast == null || p.answerMs < fast.answerMs)) fast = p;
            if (fast != null) {
                String sec = String.format(java.util.Locale.US, "%.1f", fast.answerMs / 1000.0);
                sec = DigitsText.convert(sec, latin);
                if (!latin) sec = sec.replace('.', '٫');
                root.addView(text(S.t("الأسرع: %s، %s ثانية", fast.name, sec), 40, SAFF, true), at(1500, 56, 96, 396));
            } else root.addView(text(room.endedEarly ? S.t("جاوب الجميع") : S.t("انتهى الوقت"), 40, SAFF, true), at(1500, 56, 96, 396));   // all answered early: not «time's up»
        }
        List<QuizRoom.Player> ps = room.players();
        for (int i = 0; i < 4; i++) {
            boolean right = reveal && i == q.correct, wrong = reveal && !right;
            FrameLayout st = stamp(right ? OKB : wrong ? PAPER : TILE, wrong ? 0x59161410 : INK, wrong ? 4 : 6, wrong);
            View tab = rect(wrong ? 0x99161410 : INK);
            st.addView(tab, new FrameLayout.LayoutParams(px(96), px(96), Gravity.START | Gravity.TOP));
            TextView lt = centered(letter(i), 64, PAPER, true); lt.setAlpha(wrong ? .6f : 1f);
            st.addView(lt, new FrameLayout.LayoutParams(px(96), px(96), Gravity.START | Gravity.TOP));
            String a = q.opts[i];
            TextView at = text(a, chooseFont(a, 68, 56, 44, 16, 30) - (a.length() > 50 ? 6 : 0), right ? 0xFFFFFFFF : wrong ? 0x73161410 : INK, true);
            at.setMaxLines(3); at.setEllipsize(android.text.TextUtils.TruncateAt.END);
            FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1, -2, Gravity.START | Gravity.CENTER_VERTICAL); tp.setMarginStart(px(136)); tp.setMarginEnd(px(reveal ? 28 + 56 * Math.max(1, countChose(ps, i)) : 28));
            st.addView(at, tp);
            if (reveal) {
                LinearLayout sq = new LinearLayout(this); sq.setOrientation(LinearLayout.HORIZONTAL); sq.setLayoutDirection(Dir.layout());
                int shown = 0;
                for (QuizRoom.Player p : ps) if (p.answer == i && !p.waiting) {
                    View v = box(PLAYER[p.color % 8], INK, 3, false);
                    LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(px(48), px(48)); vp.setMarginStart(px(8));   // drawn at once, full size: nothing to animate in
                    sq.addView(v, vp);
                    shown++;
                }
                FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(-2, px(48), Gravity.END | Gravity.CENTER_VERTICAL); sp.setMarginEnd(px(24));
                st.addView(sq, sp);
            }
            root.addView(st, at(846, 190, 96 + (i % 2) * (846 + 36), 546 + (i / 2) * (190 + 16)));   // ends at y 942: the hint line (978) is below the answers
        }
        if (reveal) hints(S.t("اختيار"), S.t("الترتيب"), S.t("رجوع"), S.t("إيقاف مؤقت"));
    }
    private int countChose(List<QuizRoom.Player> ps, int i) { int n = 0; for (QuizRoom.Player p : ps) if (p.answer == i && !p.waiting) n++; return n; }

    private void fillAnswered() {
        if (answeredSquares == null) return;
        answeredSquares.removeAllViews();
        int n = 0;
        for (QuizRoom.Player p : room.players()) {
            if (p.left || p.waiting) continue;
            boolean done = p.answer >= 0;
            View v = done ? box(PLAYER[p.color % 8], 0, 0, false) : box(0, 0x80EFE8DA, 4, false);
            LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(px(28), px(28)); vp.setMarginEnd(px(8));
            answeredSquares.addView(v, vp);
            if (done) n++;
        }
        shownAnswered = n;
    }

    private void liveQuestion() {
        if (timerNum == null || room == null) return;
        long ms = room.leftMs();
        long sec = (ms + 999) / 1000;
        if (sec != lastSecond) {
            lastSecond = sec;
            timerNum.setText(num((int) sec));
            int on = (int) Math.min(20, sec);
            for (int i = 0; i < ticks.size(); i++) ticks.get(i).setBackgroundColor(i < on ? (sec <= 5 ? PINK : SAFF) : TICKOFF);
            if (sec > 0 && sec <= 5) QuizSound.play(sound, QuizSound.TICK);
        }
        if (room.answeredCount() != shownAnswered) fillAnswered();
    }

    // ---- the leaderboard (B3), the podium (B4), the full list, the pause card
    private String signed(int d) { return "+" + num(d); }

    private LinearLayout playerRow(QuizRoom.Player p, int rank, boolean withDelta, int height) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setLayoutDirection(Dir.layout());
        GradientDrawable g = new GradientDrawable(); g.setColor(TILE); g.setStroke(px(6), INK); row.setBackground(g);
        TextView rk = centered(num(rank), 88, rank == 1 ? INK : PAPER, true); rk.setBackgroundColor(rank == 1 ? SAFF : INK);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(px(128), px(height - 12)); rp.setMargins(px(0), 0, 0, 0);
        row.addView(rk, rp);
        View sq = box(PLAYER[p.color % 8], INK, 3, false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(px(20), px(64)); sp.setMarginStart(px(32)); sp.setMarginEnd(px(24));
        row.addView(sq, sp);
        TextView nm = text(p.name, 64, INK, true); nm.setSingleLine(true); nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(nm, new LinearLayout.LayoutParams(0, -2, 1f));
        if (withDelta) {
            TextView ar = centered(p.score > 0 && p.lastDelta > 0 ? "▲" : p.score > 0 && p.lastDelta < 0 ? "▼" : "", 44, p.lastDelta > 0 ? OKB : PINK, false);
            row.addView(ar, new LinearLayout.LayoutParams(px(70), -2));
            TextView d = centered(signed(p.lastPoints), 44, 0x99161410, false); d.setTextDirection(View.TEXT_DIRECTION_LTR); d.setText("\u2066" + signed(p.lastPoints) + "\u2069");   // «+591», the plus on the left in both languages
            row.addView(d, new LinearLayout.LayoutParams(px(150), -2));
        }
        TextView sc = text(num(p.score), 88, INK, true); sc.setTextDirection(View.TEXT_DIRECTION_LTR); sc.setGravity(Gravity.END | Gravity.CENTER_VERTICAL); sc.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(px(280), -2); cp.setMarginEnd(px(32));
        row.addView(sc, cp);
        if (p.left) row.setAlpha(.5f);
        return row;
    }

    private void drawBoard() {
        title(S.t("الترتيب"), S.t("بعد السؤال %s من %s", num(room.index + 1), num(room.total())));
        List<QuizRoom.Player> r = room.ranked();
        for (int i = 0; i < Math.min(BOARD_ROWS, r.size()); i++) {
            QuizRoom.Player p = r.get(i);
            LinearLayout row = playerRow(p, p.rank, true, 128);   // players on one score share the place
            root.addView(row, at(1728, 128, 96, 236 + i * 142));
            if (animate && p.prevRank > 0 && p.prevRank != i + 1 && p.prevRank <= BOARD_ROWS) { row.setTranslationY((p.prevRank - (i + 1)) * 142f * scale); row.animate().translationY(0).setDuration(300).start(); }
        }
        hints(S.t("اختيار"), room.index + 1 >= room.total() ? S.t("النتيجة النهائية") : S.t("السؤال التالي"), S.t("رجوع"), S.t("إيقاف مؤقت"));
    }

    private void drawEnd() {
        List<QuizRoom.Player> r = room.ranked();
        title(S.t("انتهت اللعبة"), r.isEmpty() ? null : r.get(0).score <= 0 ? S.t("لا فائز هذه المرة") : r.size() > 1 && r.get(1).score == r.get(0).score ? S.t("تعادل في المركز الأول") : S.t("الفائز: %s", r.get(0).name));
        String[] labels = {S.t("العب مرة أخرى"), S.t("تغيير الإعدادات"), S.t("خروج")};
        for (int i = 0; i < 3; i++) root.addView(button(labels[i], sel == i, 520), at(520, 96, 96 + i * (520 + 24), 232));
        int[] cols = {OKB, SAFF, 0xFFF08A6E}; float[] hs = {512, 400, 300}; int[] place = {1, 0, 2};   // heights of 1st, 2nd, 3rd (the winner tallest, in the middle); on the screen 2nd, 1st, 3rd from the left
        for (int k = 0; k < 3; k++) {
            int pi = place[k];
            if (pi >= r.size()) continue;
            QuizRoom.Player p = r.get(pi);
            LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL); col.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            LinearLayout nameBar = new LinearLayout(this); nameBar.setOrientation(LinearLayout.HORIZONTAL); nameBar.setGravity(Gravity.CENTER_VERTICAL); nameBar.setBackgroundColor(INK); nameBar.setPadding(px(32), 0, px(32), 0); nameBar.setLayoutDirection(Dir.layout());
            TextView nm = text(clip(p.name, 56, 380), 56, PAPER, true); nm.setSingleLine(true); nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
            nameBar.addView(nm, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView sc = text(num(p.score), 48, SAFF, false); sc.setTextDirection(View.TEXT_DIRECTION_LTR); nameBar.addView(sc);
            col.addView(nameBar, new LinearLayout.LayoutParams(-1, px(96)));
            FrameLayout blk = stamp(cols[pi], INK, 6, false);
            blk.addView(centered(num(p.rank), Math.min(340, hs[pi] * .75f), INK, true), new FrameLayout.LayoutParams(-1, -1));
            col.addView(blk, new LinearLayout.LayoutParams(-1, px(hs[pi])));
            FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(px(576), -2, Gravity.LEFT | Gravity.BOTTOM); cp.leftMargin = px(96 + k * 576); cp.bottomMargin = px(120);   // the blocks stand on y 960, above the hint line and inside the safe area
            root.addView(col, cp);
        }
        if (animate && !lowRam) root.addView(new ConfettiView(this), new FrameLayout.LayoutParams(-1, -1));
        hints("▼", S.t("الترتيب الكامل"));
    }

    private void drawList() {
        title(S.t("الترتيب الكامل"), S.t("%s لاعبين", num(room.players().size())));
        List<QuizRoom.Player> r = room.ranked();
        for (int i = 0; i < LIST_ROWS && sel + i < r.size(); i++) root.addView(playerRow(r.get(sel + i), r.get(sel + i).rank, false, 112), at(1728, 112, 96, 236 + i * 126));
        hints("▲ ▼", S.t("الصفحة"), S.t("رجوع"), S.t("الترتيب"));
    }

    private void drawPause() {
        title(S.t("اللعبة متوقفة"), null);
        String[] labels = {S.t("متابعة"), S.t("إنهاء اللعبة"), S.t("الرئيسية")};
        for (int i = 0; i < 3; i++) root.addView(button(labels[i], sel == i, 640), at(640, 104, 96, 260 + i * 128));
        hints("▲ ▼", S.t("الاختيار"), S.t("رجوع"), S.t("متابعة"));
    }

    private void keyButtons(boolean back, boolean ok, int dx, int dy, int n) {
        if (screen == Screen.PAUSE && back) { room.resume(); return; }
        if (back) { if (screen == Screen.END) { closeRoom(); finish(); } return; }
        int d = screen == Screen.PAUSE ? dy : dx;
        if (d != 0) { sel = Math.max(0, Math.min(n - 1, sel + d)); show(screen); return; }
        if (!ok) return;
        if (screen == Screen.PAUSE) { if (sel == 0) room.resume(); else if (sel == 1) room.finish(); else { closeRoom(); finish(); } }
        else if (screen == Screen.END) {
            if (sel == 0) { room.playAgain(); shownPhase = null; }
            else if (sel == 1) { room.playAgain(); shownPhase = QuizRoom.Phase.LOBBY; show(Screen.SETUP); }
            else { closeRoom(); finish(); }
        }
    }

    // ---------------------------------------------------------------- small views

    /** the QR: dark modules on white, drawn square so a phone camera reads it from the sofa */
    static final class QrView extends View {
        private final QrCode code; private final Paint paint = new Paint();
        QrView(Context c, QrCode code) { super(c); this.code = code; paint.setColor(0xFF000000); paint.setAntiAlias(false); }
        @Override protected void onDraw(Canvas cv) {
            int n = code.size; float m = (float) Math.min(getWidth(), getHeight()) / n;
            for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) if (code.dark(x, y)) cv.drawRect(x * m, y * m, (x + 1) * m + .5f, (y + 1) * m + .5f, paint);
        }
    }

    /** a check mark in ink (the right answer in the timer's place) */
    static final class CheckView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        CheckView(Context c) { super(c); p.setColor(INK); p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); }
        @Override protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight(), s = Math.min(w, h) * .42f; p.setStrokeWidth(s * .22f);
            float cx = w / 2, cy = h / 2;
            android.graphics.Path path = new android.graphics.Path(); path.moveTo(cx - s * .6f, cy + s * .05f); path.lineTo(cx - s * .15f, cy + s * .5f); path.lineTo(cx + s * .65f, cy - s * .5f);
            cv.drawPath(path, p);
        }
    }

    /** the podium's few seconds of confetti (not on low-RAM TVs, not with animations off) */
    static final class ConfettiView extends View {
        private final float[] x = new float[40], y = new float[40], v = new float[40]; private final int[] c = new int[40]; private final Paint p = new Paint(); private long t0;
        ConfettiView(Context ctx) { super(ctx); Random r = new Random(); for (int i = 0; i < 40; i++) { x[i] = r.nextFloat(); y[i] = -r.nextFloat(); v[i] = .25f + r.nextFloat() * .35f; c[i] = PLAYER[r.nextInt(8)]; } t0 = System.currentTimeMillis(); }
        @Override protected void onDraw(Canvas cv) {
            float dt = (System.currentTimeMillis() - t0) / 1000f; boolean more = false;
            for (int i = 0; i < 40; i++) { float yy = y[i] + v[i] * dt; if (yy < 1.05f) more = true; p.setColor(c[i]); cv.drawRect(x[i] * getWidth(), yy * getHeight(), x[i] * getWidth() + getWidth() * .012f, yy * getHeight() + getWidth() * .012f, p); }
            if (more && dt < 4.5f) postInvalidateOnAnimation();
        }
    }
}
