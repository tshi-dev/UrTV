package sa.urtv.screensaver;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * «خمس خانات» on the TV (games/khanat/SPEC.md, the remote-only flow; the rules are KhanatEngine, the letters and examples KhanatData, the screens are the
 * kit's blocks: docs/GAMES-KIT.md). The teams write on paper or just say their answers; the remote is the host's: OK stops the writing («خلّصنا»), then
 * each box is judged by the group and marked team by team. In English UrTV the letters are A to Z and the examples the English names.
 */
public class KhanatActivity extends GameActivity {
    enum Screen { SETUP, LOBBY, LETTER, WRITING, WHO, REVEAL, SCORE, END }

    static final int[] ROUNDS = {3, 5, 8}, SECONDS = {60, 90, 0}, COUNTS = {2, 3, 4, 5, 6, 7, 8, 9, 10};
    /** teams: 2 to 4 (the first three counts); individual mode: 2 to 10 players (GameScore.MAX) */
    static final int MAX_TEAMS = 4;
    static final int SPIN_MS = 1500, LETTER_MS = 3000;
    /** the boxes' names, marked for the English table: the five of every round, then the two a sixth can be («أكلة», «مدينة سعودية») */
    static final String[] BOX_KEYS = {"animal", "plant", "object", "country", "name", "food", "saudi_city"};
    static final String[] BOX_NAMES = {S.n("حيوان"), S.n("نبات"), S.n("جماد"), S.n("بلاد"), S.n("اسم"), S.n("أكلة"), S.n("مدينة سعودية")};

    private KhanatData data; private GameChips chips; private KhanatEngine game; private String[] names = new String[0];
    private Screen screen = Screen.SETUP; private int sel; private boolean examples;
    private final GameClock clock = new GameClock(); private TickTimer timer; private long letterAt; private TextView spinView; private boolean settled; private final Random rnd = new Random();

    @Override protected String id() { return "khanat"; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        data = GameData.load(this, "khanat", KhanatData.PUBLISHED, KhanatData.STARTER);
        GameData.refresh(this, "khanat", KhanatData.PUBLISHED, KhanatData.BYTES_MAX);
        chips = new GameChips()
            .add("rounds", S.t("الجولات"), new String[]{num(3), num(5), num(8)}, 1)
            .add("time", S.t("الوقت"), new String[]{S.t("%s ثانية", num(60)), S.t("%s ثانية", num(90)), S.t("بدون وقت")}, 1)
            .add("mode", S.t("نوع اللعب"), new String[]{S.t("فرق"), S.t("أفراد")}, 0)
            .add("count", S.t("العدد"), new String[]{num(2), num(3), num(4), num(5), num(6), num(7), num(8), num(9), num(10)}, 0)
            .add("boxes", S.t("الخانات"), new String[]{num(5), S.t("%s مع أكلة", num(6)), S.t("%s مع مدينة سعودية", num(6))}, 0)
            .add("hard", S.t("حروف صعبة"), new String[]{S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0)
            .add("phones", S.t("اللعب بالجوالات"), new String[]{S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0);
        remembered(soundChip(chips));
        chips.get("phones").picked = getIntent().getBooleanExtra("phones", false) ? 1 : 0;   // off unless the «بالجوالات» view opened this game; never kept from last time
        onChips(chips);
    }

    /** teams are 2 to 4; with «أفراد» every count to ten is offered */
    @Override protected void onChips(GameChips c) { for (int i = MAX_TEAMS - 1; i < COUNTS.length; i++) c.setOff("count", i, c.picked("mode") == 0); }
    private boolean solo() { return chips.picked("mode") == 1; }
    @Override protected String namesKind() { return solo() ? "players" : "teams"; }
    @Override protected int namesCount() { return COUNTS[chips.picked("count")]; }

    @Override protected boolean inPlay() { return screen != Screen.SETUP && screen != Screen.LOBBY && screen != Screen.END; }
    @Override protected void onPauseChanged(boolean paused, long now) { if (paused) clock.pause(now); else clock.resume(now); }
    @Override protected void onEndRound() { if (game != null) { game.finish(); clock.stop(); go(Screen.END); play(QuizSound.FANFARE); } }

    private String boxName(String key) { for (int i = 0; i < BOX_KEYS.length; i++) if (BOX_KEYS[i].equals(key)) return S.tv(BOX_NAMES[i]); return key; }
    private void go(Screen s) {
        if (s == Screen.REVEAL && screen != Screen.REVEAL && onPhones()) { collect(); autoMark(); }
        screen = s; sel = 0; examples = false; redraw();
    }

    // ---------------------------------------------------------------- with phones: every phone is a player and types its answers

    private String[][] typedAns = new String[0][0];   // [side][box]: what each phone typed
    private int firstSeat = -1; private long graceAt;
    private boolean onPhones() { return phones != null && phones.room.phase() == GamePhoneRoom.Phase.PLAY; }
    /** the boxes' forms go to every phone: the round's letter and one field per box */
    private void sendForms() {
        if (!onPhones()) return;
        String[] labels = new String[game.boxes.length];
        for (int i = 0; i < labels.length; i++) labels[i] = boxName(game.boxes[i]);
        for (int i = 0; i < phones.room.count(); i++) phones.room.card(i, GamePhoneRoom.TEXT, "", S.t("كلمة لكل خانة تبدأ بحرف %s", game.letter), "", labels, null, -1);
        firstSeat = -1; graceAt = 0;
    }
    /** what the phones typed, into [side][box] (empty where nothing was typed) */
    private void collect() {
        typedAns = new String[game.score.n][game.boxes.length];
        for (int i = 0; i < typedAns.length; i++) {
            String[] t = i < phones.room.count() ? phones.room.typed(i) : null;
            for (int b = 0; b < game.boxes.length; b++) typedAns[i][b] = t == null || b >= t.length ? "" : t[b];
        }
        phones.room.waitAll(S.t("شوفوا التلفزيون"));
    }
    /** the host's first guess for this box: nothing for an empty answer or one that does not start with the letter, «مكررة» when another player wrote the same word, else «صحيحة»; the host can change any tile */
    private void autoMark() {
        if (!onPhones() || game.phase != KhanatEngine.Phase.REVEAL) return;
        int n = game.score.n, b = game.box; String lead = MissingWord.fold(game.letter);
        String[] f = new String[n];
        for (int i = 0; i < n; i++) f[i] = MissingWord.fold(typedAns[i][b]);
        for (int i = 0; i < n; i++) {
            boolean ok = !f[i].isEmpty() && !lead.isEmpty() && f[i].charAt(0) == lead.charAt(0), dup = false;
            for (int j = 0; j < n; j++) if (j != i && f[j].equals(f[i])) dup = true;
            game.set(i, !ok ? KhanatEngine.NONE : dup ? KhanatEngine.SAME : KhanatEngine.VALID);
        }
    }
    private void startWithPhones() {
        int n = phones.room.count();
        if (n < 2) return;
        if (n > GameScore.MAX) { toast(S.t("اللعبة تتسع لعشرة لاعبين")); return; }
        phones.room.start(); phoneLobby = false;
        begin(n, phones.room.names().toArray(new String[0]));
    }

    private void start() {
        if (chips.picked("phones") == 1) {
            if (!onPhones()) { if (data == null || data.letters(en, false).size() < 10) { toast(S.t("قريبًا")); return; } if (!openPhones()) return; screen = Screen.LOBBY; phoneLobby = true; redraw(); return; }
            begin(phones.room.count(), phones.room.names().toArray(new String[0]));
            return;
        }
        closePhones();   // the switch is off: the remote plays alone, as it always did
        begin(COUNTS[chips.picked("count")], null);
    }
    private void begin(int teams, String[] phoneNames) {
        if (data == null || data.letters(en, false).size() < 10) { toast(S.t("قريبًا")); return; }
        int sixth = chips.picked("boxes"), n = sixth == 0 ? 5 : 6;
        String[] boxes = new String[n];
        System.arraycopy(BOX_KEYS, 0, boxes, 0, 5);
        if (n == 6) boxes[5] = BOX_KEYS[4 + sixth];
        names = phoneNames != null ? phoneNames : solo() ? playerNames(teams) : teamNames(teams);
        game = new KhanatEngine(teams, ROUNDS[chips.picked("rounds")], boxes, data.letters(en, chips.picked("hard") == 1), rnd);
        beginLetter();
    }
    private void beginLetter() { letterAt = System.currentTimeMillis(); settled = !animate; clock.stop(); go(Screen.LETTER); if (settled) play(QuizSound.CHIME); }
    private void beginWriting() {
        game.write();
        clock.start(SECONDS[chips.picked("time")] * 1000L, System.currentTimeMillis());
        go(Screen.WRITING);
        sendForms();
    }

    // ---------------------------------------------------------------- the clock

    @Override protected void onTick(long now) {
        if (screen == Screen.LETTER) {
            long t = now - letterAt;
            if (!settled && t < SPIN_MS) { if (spinView != null) { List<String> all = data.letters(en, false); spinView.setText(all.get(rnd.nextInt(all.size()))); } }   // the wheel: a letter every tick
            else if (!settled) { settled = true; if (spinView != null) spinView.setText(game.letter); play(QuizSound.CHIME); }
            else if (t >= LETTER_MS) beginWriting();
        } else if (screen == Screen.WRITING && clock.running()) {
            if (timer != null) timer.set(clock.leftSeconds(now));
            if (onPhones()) {   // the first phone to send ends the writing for everyone after a short grace; when every phone has sent, at once
                if (firstSeat < 0) { int f = phones.room.firstReplied(); if (f >= 0) { firstSeat = f; graceAt = now + 8000; play(QuizSound.CHIME); redraw(); } }
                if (firstSeat >= 0 && (now >= graceAt || phones.room.repliedCount() >= phones.room.active())) {
                    clock.stop(); game.stop(true); game.finishedFirst(firstSeat); go(Screen.REVEAL); return;
                }
            }
            if (clock.over(now)) { clock.stop(); game.stop(false); play(QuizSound.LOW); go(Screen.REVEAL); }
        }
    }

    // ---------------------------------------------------------------- keys

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        switch (screen) {
            case SETUP: if (keySetup(chips, back, ok, dx, dy)) start(); return;
            case LOBBY: if (back) { closePhones(); go(Screen.SETUP); } else if (ok) startWithPhones(); return;
            case LETTER: if (back) openPause(); else if (ok) { settled = true; beginWriting(); } return;
            case WRITING: if (back) openPause(); else if (ok) { clock.stop(); game.stop(true); go(Screen.WHO); } return;
            case WHO: {
                if (back) { openPause(); return; }
                if (ok) { game.finishedFirst(sel < game.score.n ? sel : -1); go(Screen.REVEAL); return; }
                int to = KhanatLayout.moveWho(game.score.n + 1, sel, dx, dy);
                if (to != sel) { sel = to; redraw(); }
                return;
            }
            case REVEAL: keyReveal(back, ok, dx, dy); return;
            case SCORE:
                if (back) openPause();
                else if (ok) { game.next(); if (game.phase == KhanatEngine.Phase.END) { go(Screen.END); play(QuizSound.FANFARE); } else beginLetter(); }
                return;
            case END: {
                if (back) { finish(); return; }
                int v = keyButtons(sel, 3, ok, dx);
                if (v >= 0) { if (v != sel) { sel = v; redraw(); } return; }
                if (v == -1) start(); else if (v == -2) go(Screen.SETUP); else finish();
                return;
            }
            default:
        }
    }

    /** the reveal's focus: 0..n-1 the sides' tiles (one row up to five, two rows from six), n «التالي», n + 1 «أمثلة» */
    private void keyReveal(boolean back, boolean ok, int dx, int dy) {
        int n = game.score.n;
        if (back) { openPause(); return; }
        if (dx != 0 || dy != 0) { int to = KhanatLayout.moveMark(n, sel, dx, dy); if (to != sel) { sel = to; redraw(); } return; }
        if (!ok) return;
        if (sel < n) { game.cycle(sel); redraw(); }
        else if (sel == n + 1) { examples = !examples; redraw(); }
        else {
            game.nextBox();
            if (game.phase == KhanatEngine.Phase.SCORE) { go(Screen.SCORE); play(QuizSound.CHIME); } else { autoMark(); sel = 0; examples = false; redraw(); }
        }
    }

    // ---------------------------------------------------------------- screens

    private String roundLine() { return game.tiebreak ? S.t("جولة فاصلة") : S.t("الجولة %s من %s", num(game.round + 1), num(game.rounds)); }

    @Override protected void draw() {
        timer = null; spinView = null;
        switch (screen) {
            case SETUP: setup(chips, S.t("ابدأ")); break;
            case LOBBY: drawPhoneLobby(2, GameScore.MAX); break;
            case LETTER: drawLetter(); break;
            case WRITING: drawWriting(); break;
            case WHO: drawWho(); break;
            case REVEAL: drawReveal(); break;
            case SCORE: drawScore(); break;
            case END: podium(game.score, names, sel); break;
            default:
        }
    }

    /** the Arabic letters' names (alef, baa ...): said under the letter, because «ه» and the digit «٥» are drawn alike; in English, or for a letter not in the table, nothing */
    static final String LETTERS = "األف أألف بباء تتاء ثثاء ججيم ححاء خخاء ددال ذذال رراء ززاي سسين ششين صصاد ضضاد ططاء ظظاء ععين غغين ففاء ققاف ككاف للام مميم ننون ههاء وواو يياء";
    static String letterName(String letter, boolean english) {
        if (english || letter == null || letter.length() != 1) return "";
        for (String t : LETTERS.split(" ")) if (t.charAt(0) == letter.charAt(0)) return " " + t.substring(1);
        return "";
    }
    private void drawLetter() {
        root.setBackgroundColor(L.deep);
        root.addView(centered(roundLine(), 56, L.saff, true), at(1728, 80, 96, 120));
        spinView = centered(settled ? game.letter : "", 480, L.onDeep, true);
        root.addView(spinView, at(1728, 600, 96, 220));
        root.addView(centered(S.t("حرف هذه الجولة") + (settled ? letterName(game.letter, en) : ""), 48, L.onDeep, false), at(1728, 72, 96, 860));   // «هاء» under the letter: ه and ٥ are drawn alike
    }

    /** the boxes of this round as a line of tiles at `y` */
    private void boxTiles(float y, float hgt, int current) {
        int n = game.boxes.length; float gap = 16, w = (1728 - (n - 1) * gap) / n;
        for (int i = 0; i < n; i++) {
            boolean on = i == current;
            FrameLayout t = stamp(on ? L.saff : L.tile, L.ink, 6, false);
            t.addView(centered(boxName(game.boxes[i]), n > 5 ? 44 : 64, on ? L.onSaff : L.ink, true), new FrameLayout.LayoutParams(-1, -1));
            root.addView(t, at(w, hgt, 96 + i * (w + gap), y));
        }
    }

    private void drawWriting() {
        band(528);
        root.addView(text(roundLine(), 40, L.saff, true), at(1100, 56, 96, 54));
        root.addView(centered(game.letter, fitSize(game.letter, 300, 360, 336, 1), L.onDeep, true), at(360, 336, 96, 108));   // the whole letter, dots included, above the ticks (452)
        TextView say = text(en ? S.t("كلمة لكل خانة تبدأ بهذا الحرف") : S.t("كلمة لكل خانة تبدأ بحرف %s", letterName(game.letter, false).trim()), 72, L.onDeep, true); say.setMaxLines(2);   // the letter's name too: ه and ٥ look alike
        root.addView(say, at(880, 220, 500, 190));
        int secs = SECONDS[chips.picked("time")];
        if (secs > 0) { timer = new TickTimer(secs, 452); timer.set(clock.leftSeconds(System.currentTimeMillis())); }
        boxTiles(568, 200, -1);
        if (onPhones() && firstSeat >= 0) root.addView(centered(S.t("%s خلّص، باقي الباقين ثواني", phones.room.name(firstSeat)), 56, L.ink, true), at(1728, 80, 96, 690));
        sidesBar(game.score, names, -1, 800);
        hints(S.t("اختيار"), S.t("خلّصنا"), S.t("رجوع"), S.t("إيقاف مؤقت"));
    }

    private void drawWho() {
        title(S.t("من خلّص أول؟"), roundLine());
        int n = game.score.n, total = n + 1;
        String[] labels = new String[total];
        System.arraycopy(names, 0, labels, 0, n); labels[n] = S.t("لا أحد");
        for (int i = 0; i < total; i++) { float[] r = KhanatLayout.who(total, i); root.addView(button(clip(labels[i], 44, r[2] - 40), sel == i), at(r[2], r[3], r[0], r[1])); }
        float noteY = KhanatLayout.who(total, total - 1)[1] + 104 + 40;
        TextView note = text(S.t("من خلّص أول وكل خاناته صحيحة له %s نقاط زيادة", num(KhanatEngine.FIRST_BONUS)), 44, L.ink(0xCC), false); note.setMaxLines(2);
        root.addView(note, at(1728, 130, 96, noteY));
        hints(KhanatLayout.rows(total) > 1 ? "◀ ▶ ▲ ▼" : "◀ ▶", S.t("الاختيار"), S.t("اختيار"), S.t("تأكيد"));
    }

    private void drawReveal() {
        int n = game.score.n; String key = game.boxes[game.box];
        band(300);
        root.addView(text(S.t("الخانة %s من %s", num(game.box + 1), num(game.boxes.length)), 40, L.saff, true), at(1100, 56, 96, 54));
        root.addView(text(S.t("%s بحرف", boxName(key)), 112, L.onDeep, true), at(1300, 150, 96, 122));
        FrameLayout lt = stamp(L.saff, 0, 0, false);
        lt.addView(centered(game.letter, 144, L.onSaff, true), new FrameLayout.LayoutParams(-1, -1));
        root.addView(lt, atEnd(232, 200, 96, 54));
        boolean compact = KhanatLayout.rows(n) > 1;
        for (int i = 0; i < n; i++) {
            int st = game.marks[game.box][i]; boolean focus = sel == i;
            float[] r = KhanatLayout.tile(n, i), w = r;
            FrameLayout t = stamp(st == KhanatEngine.VALID ? L.ok : st == KhanatEngine.SAME ? L.saff : L.tile, L.ink, focus ? 14 : 6, false);
            int words = st == KhanatEngine.VALID ? L.onDeep : st == KhanatEngine.SAME ? L.onSaff : L.ink;
            String verdict = onPhones() && !typedAns[i][game.box].isEmpty() ? typedAns[i][game.box] : st == KhanatEngine.VALID ? S.t("صحيحة") : st == KhanatEngine.SAME ? S.t("مكررة") : S.t("بدون");   // with phones the tile shows what was typed; its colour and points are the verdict
            String plus = "⁦+" + num(KhanatEngine.points(st)) + "⁩";
            if (compact) {   // two rows: the name on top, the verdict and the points below it
                t.addView(centered(clip(names[i], 44, w[2] - 40), 44, words, true), new FrameLayout.LayoutParams(-1, px(64), Gravity.TOP));
                FrameLayout.LayoutParams vp = new FrameLayout.LayoutParams(px(w[2] / 2f - 8), px(56), Gravity.BOTTOM | Gravity.START); vp.bottomMargin = px(14); vp.setMarginStart(px(12));
                t.addView(centered(verdict, 36, words, false), vp);
                TextView pts = centered(plus, 56, words, true); pts.setTextDirection(View.TEXT_DIRECTION_LTR);
                FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(px(w[2] / 2f - 8), px(64), Gravity.BOTTOM | Gravity.END); pp.bottomMargin = px(10); pp.setMarginEnd(px(12));
                t.addView(pts, pp);
            } else {
                t.addView(centered(clip(names[i], 56, w[2] - 60), 56, words, true), new FrameLayout.LayoutParams(-1, px(96), Gravity.TOP));
                FrameLayout.LayoutParams vp = new FrameLayout.LayoutParams(-1, px(80), Gravity.TOP); vp.topMargin = px(96);
                t.addView(centered(verdict, 48, words, false), vp);
                TextView pts = centered(plus, 72, words, true); pts.setTextDirection(View.TEXT_DIRECTION_LTR);
                FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(-1, px(96), Gravity.BOTTOM); pp.bottomMargin = px(16);
                t.addView(pts, pp);
            }
            if (focus) { FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-1, px(14), Gravity.BOTTOM); t.addView(rect(L.ink), bp); }
            root.addView(t, at(r[2], r[3], r[0], r[1]));
        }
        boolean last = game.box + 1 >= game.boxes.length;
        root.addView(button(last ? S.t("نتيجة الجولة") : S.t("الخانة التالية"), sel == n), at(560, 104, 96, 664));
        root.addView(button(S.t("أمثلة"), sel == n + 1), at(360, 104, 96 + 560 + 24, 664));
        if (examples) {
            List<String> ex = data.samples(en, game.letter, key, 5);
            String line = ex.isEmpty() ? S.t("لا أمثلة لهذا الحرف") : android.text.TextUtils.join(en ? ", " : "، ", ex);
            TextView tv = text(line, 52, L.ink, true); tv.setMaxLines(2);
            root.addView(tv, at(1728, 140, 96, 800));
        } else {
            TextView tv = text(solo() ? S.t("اتفقوا بصوت عالٍ، ثم علّموا كل لاعب: صحيحة، مكررة مع لاعب آخر، أو بدون") : S.t("اتفقوا بصوت عالٍ، ثم علّموا كل فريق: صحيحة، مكررة مع فريق آخر، أو بدون"), 40, L.ink(0xCC), false); tv.setMaxLines(2);
            root.addView(tv, at(1728, 120, 96, 800));
        }
        hints("◀ ▶ ▲ ▼", S.t("الاختيار"), S.t("اختيار"), sel < n ? S.t("غيّر الحكم") : S.t("تأكيد"), S.t("رجوع"), S.t("إيقاف مؤقت"));
    }

    private void drawScore() {
        title(S.t("النتيجة"), roundLine());
        board(game.score, names, true);
        if (game.bonusGiven && game.finisher >= 0) root.addView(text((solo() ? S.t("%s خلّص أول: %s نقاط زيادة", names[game.finisher], num(KhanatEngine.FIRST_BONUS)) : S.t("%s خلّصوا أول: %s نقاط زيادة", names[game.finisher], num(KhanatEngine.FIRST_BONUS))), 44, L.ink, true), at(1728, 64, 96, 880));
        boolean tieNext = !game.tiebreak && game.lastRound() && game.score.tie();
        hints(S.t("اختيار"), tieNext ? S.t("جولة فاصلة") : game.lastRound() ? S.t("النتيجة النهائية") : S.t("الجولة التالية"), S.t("رجوع"), S.t("إيقاف مؤقت"));
    }
}
