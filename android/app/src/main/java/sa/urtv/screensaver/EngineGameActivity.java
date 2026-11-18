package sa.urtv.screensaver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Plays any game of GameDef (docs/GAME-ENGINE.md): one screen for every game of the engine, opened by the hub with the game's id. It draws the building blocks (choose, true or false, odd one
 * out, order, type a word, fill the blank, match pairs, closest number, draw and guess, buzz, vote) with the kit's blocks and the phone cards of GamePhoneRoom, and keeps the rules in GameEngine
 * (no Android). The phones are the players; the TV reads only the game it was opened for, from memory (GameReleaseStore.def), and a game that is not on closes the screen. Nothing is kept
 * but the chips and the numbers of the items played (so they wait next time).
 */
public class EngineGameActivity extends GameActivity {
    private enum Screen { SETUP, LOBBY, ASK, REVEAL, END }
    static final long REVEAL_MS = 9_000;
    private static final String[] TABS_AR = {"أ", "ب", "ج", "د", "هـ", "و"}, TABS_EN = {"A", "B", "C", "D", "E", "F"};
    private Screen screen = Screen.SETUP;
    private GameDef.Def def; private GameChips chips; private GameEngine eng; private GameScore score; private String[] names = new String[0], sideNames = new String[0];
    private long phaseAt, pausedAt; private TickTimer timer; private int sel, teams; private final Random rnd = new Random(); private List<int[]> strokes = new ArrayList<>(); private int strokeRev = -1; private Board board;

    @Override protected String id() { String g = getIntent() == null ? null : getIntent().getStringExtra("game"); return "e-" + (g == null ? "none" : g); }
    @Override protected String gameName() { return def == null ? "" : def.name.get(en); }
    @Override protected boolean inPlay() { return screen == Screen.ASK || screen == Screen.REVEAL; }

    /** a drawing: the strokes of the drawer's card, scaled from the phone's 0 to 1000 to the board */
    private final class Board extends View {
        private final Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG); private List<int[]> list = new ArrayList<>();
        Board(Context c) { super(c); pen.setStyle(Paint.Style.STROKE); pen.setStrokeCap(Paint.Cap.ROUND); pen.setStrokeJoin(Paint.Join.ROUND); }
        void set(List<int[]> s) { list = s; invalidate(); }
        @Override protected void onDraw(Canvas c) {
            c.drawColor(L.tile); pen.setColor(L.ink); pen.setStrokeWidth(Math.max(4f, getWidth() / 90f));
            float sx = getWidth() / 1000f, sy = getHeight() / 1000f;
            for (int[] st : list) {
                if (st.length < 2) continue;
                Path p = new Path(); p.moveTo(st[0] * sx, st[1] * sy);
                if (st.length == 2) p.lineTo(st[0] * sx + 0.1f, st[1] * sy);
                for (int i = 2; i + 1 < st.length; i += 2) p.lineTo(st[i] * sx, st[i + 1] * sy);
                c.drawPath(p, pen);
            }
        }
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        def = GameReleaseStore.def(getIntent().getStringExtra("game"));
        if (def == null) { finish(); return; }   // not on (or not a game the engine can run): there is nothing to show
        chips = new GameChips();
        if (def.teams) {
            String[] t = {S.tv(ON_OFF[0]), num(2), num(3), num(4)};
            chips.add("teams", S.t("الفرق"), t, 0);
        }
        remembered(soundChip(chips));
    }
    private boolean onPhones() { return phones != null && phones.room.phase() == GamePhoneRoom.Phase.PLAY; }
    private String tab(int i) { return (en ? TABS_EN : TABS_AR)[i % 6]; }
    private String q() { return eng.item().q.get(en); }

    private void start() {
        if (def.items.isEmpty()) { toast(S.t("قريبًا")); return; }
        if (!openPhones()) return;
        screen = Screen.LOBBY; phoneLobby = true; redraw();
    }
    private void startWithPhones() {
        int n = phones.room.count();
        if (n < def.minPlayers) return;
        if (n > def.maxPlayers) { toast(S.t("اللعبة تتسع لعدد أقل من اللاعبين")); return; }
        teams = def.teams && chips.get("teams") != null ? chips.picked("teams") == 0 ? 0 : chips.picked("teams") + 1 : 0;
        if (teams > n) teams = 0;
        phones.room.start(); phoneLobby = false;
        names = phones.room.names().toArray(new String[0]);
        try { eng = new GameEngine(def, def.items, n, teams, rnd, new HashSet<>(recent())); } catch (IllegalArgumentException e) { toast(S.t("قريبًا")); closePhones(); screen = Screen.SETUP; redraw(); return; }
        sideNames = def.score.equals("bank") ? new String[] {S.t("الفريق كله")} : teams > 0 ? teamNames(teams) : names;
        score = new GameScore(eng.sides);
        toAsk();
    }
    private List<Integer> recent() { List<Integer> r = new ArrayList<>(); for (String s : GamePrefs.recent(this, id())) { try { r.add(Integer.parseInt(s)); } catch (NumberFormatException ignored) { } } return r; }
    private void keep() { if (eng != null) { List<String> r = new ArrayList<>(); for (int i : eng.usedItems()) r.add(String.valueOf(i)); Collections.reverse(r); GamePrefs.remember(this, id(), r); } }
    private void sync() { for (int i = 0; i < eng.sides; i++) score.points[i] = eng.score[i]; }
    @Override protected void onPause() { super.onPause(); keep(); }

    // ---------------------------------------------------------------- a round: what each phone is shown

    private void toAsk() {
        if (eng.phase == GameEngine.Phase.END) { toEnd(); return; }
        screen = Screen.ASK; phaseAt = System.currentTimeMillis(); strokes = new ArrayList<>(); strokeRev = -1; board = null;
        GameDef.Item it = eng.item(); String[] opts;
        for (int p = 0; p < eng.players; p++) {
            switch (it.t) {
                case "choose": case "odd": opts = new String[it.o.size()]; for (int i = 0; i < opts.length; i++) opts[i] = it.o.get(i).get(en); phones.room.card(p, GamePhoneRoom.PICK, "", q(), "", opts, range(opts.length), -1); break;
                case "tf": phones.room.card(p, GamePhoneRoom.PICK, "", q(), "", new String[] {S.t("صح"), S.t("خطأ")}, range(2), -1); break;
                case "order": opts = new String[it.o.size()]; for (int k = 0; k < opts.length; k++) opts[k] = it.o.get(eng.shown()[k]).get(en); phones.room.card(p, GamePhoneRoom.ORDER, "", q(), S.t("من الأول إلى الأخير"), opts, null, -1); break;
                case "pairs": {
                    int n = it.pairs.size(); opts = new String[n]; StringBuilder left = new StringBuilder();
                    for (int k = 0; k < n; k++) { opts[k] = it.pairs.get(eng.shown()[k])[1].get(en); left.append(k > 0 ? "، " : "").append(num(k + 1)).append(" ").append(it.pairs.get(k)[0].get(en)); }
                    phones.room.card(p, GamePhoneRoom.ORDER, "", q(), S.t("اضغط ما يناسب كل واحد بالترتيب: %s", left.toString()), opts, null, -1); break;
                }
                case "word": case "blank": phones.room.card(p, GamePhoneRoom.TEXT, "", q(), "", new String[] {S.t("إجابتك")}, null, -1); break;
                case "closest": phones.room.card(p, GamePhoneRoom.TEXT, "", q(), "", new String[] {it.unit.isEmpty() ? S.t("رقم") : it.unit}, null, 1); break;
                case "draw":
                    if (p == eng.drawer()) phones.room.card(p, GamePhoneRoom.DRAW, S.t("ارسم"), it.word.get(en), S.t("لا تكتب حروفًا ولا أرقامًا"), null, null, -1);
                    else phones.room.card(p, GamePhoneRoom.TEXT, names[eng.drawer()], S.t("خمّن الرسمة"), "", new String[] {S.t("تخمينك")}, null, -1);
                    break;
                case "buzz": phones.room.card(p, GamePhoneRoom.ACT, "", q(), "", null, new int[] {GamePhoneRoom.ACT_BUZZ}, -1); break;
                default: { int[] acts = new int[eng.players - 1]; int k = 0; for (int j = 0; j < eng.players; j++) if (j != p) acts[k++] = j; phones.room.card(p, GamePhoneRoom.PICK, "", q(), S.t("اختر اللاعب الأنسب"), names, acts, -1); }
            }
        }
        redraw();
    }
    private static int[] range(int n) { int[] r = new int[n]; for (int i = 0; i < n; i++) r[i] = i; return r; }

    private void toReveal() {
        eng.reveal(); sync(); screen = Screen.REVEAL; phaseAt = System.currentTimeMillis(); play(QuizSound.CHIME);
        phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw();
    }
    private void afterReveal() { eng.next(); if (eng.phase == GameEngine.Phase.ASK) toAsk(); else toEnd(); }
    private void toEnd() { keep(); sync(); screen = Screen.END; sel = 0; play(QuizSound.FANFARE); if (onPhones()) phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw(); }
    @Override protected void onPauseChanged(boolean paused, long now) { if (paused) pausedAt = now; else if (pausedAt != 0) { phaseAt += now - pausedAt; pausedAt = 0; } }
    @Override protected void onEndRound() { if (eng != null) { eng.finish(); toEnd(); } }

    // ---------------------------------------------------------------- what the phones send

    private static int[] ints(String s) { String[] v = s.split(","); int[] r = new int[v.length]; for (int i = 0; i < v.length; i++) r[i] = Integer.parseInt(v[i].trim()); return r; }

    @Override protected void onTick(long now) {
        if (eng == null || !onPhones()) return;
        long age = now - phaseAt;
        if (screen == Screen.ASK) {
            GameDef.Item it = eng.item(); boolean changed = false;
            if (it.t.equals("buzz")) {
                if (eng.buzzer < 0) for (int p = 0; p < eng.players; p++) if (phones.room.reply(p) >= 0) { if (eng.buzz(p, age)) { speaker(p); changed = true; break; } }
                if (age >= eng.def.seconds * 1000L && eng.buzzer < 0) { toReveal(); return; }
                if (changed) redraw();
            } else {
                for (int p = 0; p < eng.players; p++) {
                    if (eng.hasAnswered(p)) continue;
                    Object a = null;
                    switch (it.t) {
                        case "choose": case "odd": { int r = phones.room.reply(p); if (r >= 0) a = r; break; }
                        case "tf": { int r = phones.room.reply(p); if (r >= 0) a = r == 0; break; }
                        case "vote": { int r = phones.room.reply(p); if (r >= 0) a = r; break; }
                        case "word": case "blank": case "draw": { String[] t = p == eng.drawer() && it.t.equals("draw") ? null : phones.room.typed(p); if (t != null) a = t[0]; break; }
                        case "closest": { String[] t = phones.room.typed(p); if (t != null) { double v = GameEngine.parseNumber(t[0]); if (!Double.isNaN(v)) a = v; } break; }
                        default: { String[] t = phones.room.typed(p); if (t != null) try { a = ints(t[0]); } catch (RuntimeException e) { a = null; } }   // order and pairs
                    }
                    if (a != null && eng.answer(p, a, age)) changed = true;
                }
                if (it.t.equals("draw")) {
                    int d = eng.drawer(), rev = phones.room.drawRev(d);
                    if (rev != strokeRev) { strokeRev = rev; strokes = phones.room.strokes(d); if (board != null) board.set(strokes); }
                }
                if (eng.allAnswered() || age >= eng.def.seconds * 1000L) { toReveal(); return; }
                if (changed) redraw();
            }
            if (timer != null) timer.set((int) ((eng.def.seconds * 1000L - age + 999) / 1000));
        } else if (screen == Screen.REVEAL && age >= REVEAL_MS) afterReveal();
    }

    /** the phone that pressed first speaks; the others wait, the host calls it right or wrong */
    private void speaker(int p) {
        for (int i = 0; i < eng.players; i++) phones.room.hold(i, i == p ? S.t("جاوب بصوت عالٍ") : S.t("%s يجاوب", names[p]));
        play(QuizSound.TICK);
    }
    private void judge(boolean ok) {
        eng.judgeBuzz(ok);
        if (eng.phase == GameEngine.Phase.REVEAL) { sync(); screen = Screen.REVEAL; phaseAt = System.currentTimeMillis(); play(ok ? QuizSound.CHIME : QuizSound.LOW); phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw(); return; }
        for (int i = 0; i < eng.players; i++) { if (eng.lockedOut(i)) phones.room.hold(i, S.t("جاوبت غلط، انتظر الباقين")); else phones.room.card(i, GamePhoneRoom.ACT, "", q(), "", null, new int[] {GamePhoneRoom.ACT_BUZZ}, -1); }
        play(QuizSound.LOW); redraw();
    }

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        switch (screen) {
            case SETUP: if (keySetup(chips, back, ok, dx, dy)) start(); return;
            case LOBBY: if (back) { closePhones(); screen = Screen.SETUP; redraw(); } else if (ok) startWithPhones(); return;
            case ASK:
                if (back) { openPause(); return; }
                if (eng.item().t.equals("buzz") && eng.buzzer >= 0) { if (ok) judge(true); else if (dy > 0) judge(false); return; }
                if (ok) toReveal();
                return;
            case REVEAL: if (back) openPause(); else if (ok) afterReveal(); return;
            default: {
                if (back) { finish(); return; }
                int v = keyButtons(sel, 3, ok, dx);
                if (v >= 0) { if (v != sel) { sel = v; redraw(); } return; }
                if (v == -1) { closePhones(); eng = null; screen = Screen.SETUP; start(); } else if (v == -2) { closePhones(); eng = null; screen = Screen.SETUP; redraw(); } else finish();
            }
        }
    }

    // ---------------------------------------------------------------- the screens

    @Override protected void draw() {
        timer = null; board = null;
        switch (screen) {
            case SETUP: setup(chips, S.t("ابدأ")); break;
            case LOBBY: drawPhoneLobby(def.minPlayers, def.maxPlayers); break;
            case ASK: drawRound(false); break;
            case REVEAL: drawRound(true); break;
            default: sync(); podium(score, sideNames, sel);
        }
    }

    private void drawRound(boolean reveal) {
        GameDef.Item it = eng.item();
        String meta = S.t("السؤال %s من %s", num(eng.index + 1), num(eng.total()));
        String words = it.t.equals("draw") ? S.t("%s يرسم، والباقي يخمّن", names[eng.drawer()]) : it.t.equals("blank") ? fill(q(), reveal, it) : it.q.get(en).isEmpty() ? "" : q();
        question(meta, words, 400);
        if (!reveal) { timer = new TickTimer(eng.def.seconds, 348); timer.set((int) ((eng.def.seconds * 1000L - (System.currentTimeMillis() - phaseAt) + 999) / 1000)); }
        switch (it.t) {
            case "choose": case "odd": options(it, reveal); break;
            case "tf": stamps(new String[] {S.t("صح"), S.t("خطأ")}, it.tf ? 0 : 1, reveal); break;
            case "order": case "pairs": list(it, reveal); break;
            case "draw": canvas(reveal, it); break;
            case "vote": if (reveal) voteTiles(); else status(S.t("صوّت %s من %s", num(eng.answeredCount()), num(eng.players))); break;
            case "buzz": buzzArea(reveal); break;
            default: typed(it, reveal);   // word, blank, closest
        }
        if (reveal) result(it); else if (!it.t.equals("buzz") && eng.def.hints && it.hint != null && System.currentTimeMillis() - phaseAt > eng.def.seconds * 500L)
            root.addView(centered(S.t("تلميح: %s", it.hint.get(en)), 44, L.ink(0xCC), true), at(1728, 64, 96, 900));
        hints(S.t("اختيار"), reveal ? S.t("اللي بعده") : it.t.equals("buzz") && eng.buzzer >= 0 ? S.t("صح") : S.t("كفاية، نشوف النتيجة"), it.t.equals("buzz") && eng.buzzer >= 0 && !reveal ? "▼" : S.t("رجوع"), it.t.equals("buzz") && eng.buzzer >= 0 && !reveal ? S.t("خطأ") : S.t("إيقاف"));
    }

    /** the statement of a blank: the dashed box, or the answer in its place once revealed */
    private String fill(String s, boolean reveal, GameDef.Item it) { return reveal ? s.replace("___", it.answers.get(0)) : s; }

    private void status(String s) { root.addView(centered(s, 56, L.ink, true), at(1728, 80, 96, 760)); }

    private void options(GameDef.Item it, boolean reveal) {
        String[] o = new String[it.o.size()]; for (int i = 0; i < o.length; i++) o[i] = it.o.get(i).get(en);
        stamps(o, it.a, reveal);
        if (!reveal) status(S.t("أجاب %s من %s", num(eng.answeredCount()), num(eng.players)));
    }
    /** the answer stamps: while asking all plain; revealed the right one green and the others flat */
    private void stamps(String[] o, int right, boolean reveal) {
        for (int i = 0; i < o.length; i++) {
            float[] r = EngineLayout.rect(i, o.length);
            root.addView(answer(tab(i), digits(o[i]), !reveal ? STAMP : i == right ? STAMP_RIGHT : STAMP_WRONG), at(r[2], r[3], r[0], r[1]));
        }
    }
    /** an order or a pairs item: the things as they were shown, numbered, and once revealed in the right order */
    private void list(GameDef.Item it, boolean reveal) {
        int n = it.t.equals("order") ? it.o.size() : it.pairs.size(); int[] sh = eng.shown();
        for (int k = 0; k < n; k++) {
            String s;
            if (it.t.equals("order")) s = it.o.get(reveal ? k : sh[k]).get(en);
            else s = reveal ? it.pairs.get(k)[0].get(en) + "  =  " + it.pairs.get(k)[1].get(en) : it.pairs.get(k)[0].get(en) + "      " + it.pairs.get(sh[k])[1].get(en);
            float[] r = EngineLayout.rect(k, n);
            root.addView(answer(reveal ? num(k + 1) : it.t.equals("order") ? tab(k) : num(k + 1), digits(s), reveal ? STAMP_RIGHT : STAMP), at(r[2], r[3], r[0], r[1]));
        }
        if (!reveal) status(S.t("أجاب %s من %s", num(eng.answeredCount()), num(eng.players)));
    }
    private void typed(GameDef.Item it, boolean reveal) {
        if (!reveal) { root.addView(centered(it.t.equals("closest") ? S.t("اكتبوا الرقم من جوالاتكم") : S.t("اكتبوا الإجابة من جوالاتكم"), 64, L.ink, true), at(1728, 90, 96, 520)); status(S.t("أجاب %s من %s", num(eng.answeredCount()), num(eng.players))); return; }
        String ans = it.t.equals("closest") ? digits(trim(it.num)) + (it.unit.isEmpty() ? "" : " " + it.unit) : it.answers.get(0);
        FrameLayout t = stamp(L.ok, L.ink, 6, false);
        t.addView(centered(ans, 112, L.onDeep, true), new FrameLayout.LayoutParams(-1, -1));
        root.addView(t, at(1728, 200, 96, 470));
    }
    private static String trim(double v) { return v == Math.rint(v) && Math.abs(v) < 1e15 ? String.valueOf((long) v) : String.valueOf(v); }
    private void canvas(boolean reveal, GameDef.Item it) {
        if (reveal) { typedWord(it.word.get(en)); return; }
        board = new Board(this); board.set(strokes);
        FrameLayout f = stamp(L.tile, L.ink, 6, false); f.addView(board, new FrameLayout.LayoutParams(-1, -1));
        root.addView(f, at(1728, 400, 96, 440));
        status(S.t("خمّن %s من %s", num(eng.answeredCount()), num(eng.players - 1)));
    }
    private void typedWord(String s) { FrameLayout t = stamp(L.ok, L.ink, 6, false); t.addView(centered(s, 112, L.onDeep, true), new FrameLayout.LayoutParams(-1, -1)); root.addView(t, at(1728, 200, 96, 470)); }
    private void voteTiles() {
        int n = eng.players, cols = n <= 5 ? n : (n + 1) / 2; float gap = 22, w = (1728 - (cols - 1) * gap) / cols, h = n <= 5 ? 300 : 170;
        for (int i = 0; i < n; i++) {
            boolean top = eng.topVoted().contains(i);
            FrameLayout t = stamp(top ? L.saff : L.tile, L.ink, 6, false);
            TextView nm = centered(clip(names[i], 44, w - 30), 44, top ? L.onSaff : L.ink, true); nm.setSingleLine(true);
            FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(-1, px(70), android.view.Gravity.TOP); np.setMargins(px(15), px(14), px(15), 0); t.addView(nm, np);
            TextView c = centered(num(eng.votesFor(i)), n <= 5 ? 112 : 72, top ? L.onSaff : L.ink, true);
            FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1, px(n <= 5 ? 150 : 90), android.view.Gravity.TOP); cp.setMargins(0, px(90), 0, 0); t.addView(c, cp);
            root.addView(t, at(w, h, 96 + (i % cols) * (w + gap), 440 + (i / cols) * (h + 20)));
        }
    }
    private void buzzArea(boolean reveal) {
        if (reveal) { String w = eng.item().answers.isEmpty() ? "" : eng.item().answers.get(0); if (!w.isEmpty()) typedWord(w); return; }
        if (eng.buzzer >= 0) { FrameLayout t = stamp(L.saff, L.ink, 12, false); t.addView(centered(names[eng.buzzer], 112, L.onSaff, true), new FrameLayout.LayoutParams(-1, -1)); root.addView(t, at(1728, 240, 96, 470)); root.addView(centered(S.t("هل جاوب صح؟"), 56, L.ink, true), at(1728, 80, 96, 740)); }
        else root.addView(centered(S.t("اضغطوا «أنا!» في جوالاتكم، وأسرعكم يجاوب"), 64, L.ink, true), at(1728, 120, 96, 540));
    }
    /** after the round: who was right and what each side has */
    private void result(GameDef.Item it) {
        StringBuilder r = new StringBuilder();
        for (int p = 0; p < eng.players; p++) if (eng.rightOf(p)) r.append(r.length() > 0 ? (en ? ", " : "، ") : "").append(names[p]);
        String line = it.t.equals("vote") ? (eng.topVoted().isEmpty() ? S.t("ما أحد صوّت") : S.t("الأكثر: %s", names[eng.topVoted().get(0)])) : r.length() == 0 ? S.t("ما أحد جاوب صح") : S.t("جاوب صح: %s", r.toString());
        root.addView(centered(line, 48, L.ink, true), at(1728, 64, 96, 836));
        if (eng.sides <= 8) sidesBar(score, sideNames, -1, 892);
    }
}
