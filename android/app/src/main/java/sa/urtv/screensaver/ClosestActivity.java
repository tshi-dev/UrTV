package sa.urtv.screensaver;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import sa.urtv.screensaver.games.closest.ClosestGame;
import sa.urtv.screensaver.games.closest.PaperPlaces;

/**
 * «أقرب رقم» (docs/GAMES-NEW.md): a question with a whole number for an answer; everyone types a guess on their phone (a number pad); the closest wins. The phones are
 * the game (2 to 10). The rules and the items are ClosestGame (no Android); the items are the bundled bank assets/games/closest.gb, nothing repeats on this TV.
 */
public class ClosestActivity extends GameActivity {
    private enum Screen { SETUP, LOBBY, ASK, REVEAL, PLACE, END }
    static final long REVEAL_MS = 11_000;
    private Screen screen = Screen.SETUP;
    private GameChips chips; private List<ClosestGame.Item> bank; private ClosestGame game; private GameScore score; private String[] names = new String[0];
    private boolean paper; private PaperPlaces pg; private int pickSel;   // «ريموت واحد»: the numbers are written on paper, the host picks the closest three
    private long phaseAt, askMs, pausedAt; private TickTimer timer; private int sel; private final Random rnd = new Random();

    @Override protected String id() { return "closest"; }
    @Override protected String namesKind() { return remoteOn(chips) ? "teams" : null; }
    @Override protected int namesCount() { return PaperPlaces.MIN_SIDES + chips.picked("sides"); }
    @Override protected boolean inPlay() { return screen == Screen.ASK || screen == Screen.REVEAL || screen == Screen.PLACE; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        bank = ClosestGame.parse(GameBundles.load(this, "closest"));
        String[] qs = new String[ClosestGame.QUESTIONS.length], secs = new String[ClosestGame.SECONDS.length];
        for (int i = 0; i < qs.length; i++) qs[i] = num(ClosestGame.QUESTIONS[i]);
        for (int i = 0; i < secs.length; i++) secs[i] = S.t("%s ثانية", num(ClosestGame.SECONDS[i]));
        String[] sd = new String[5]; for (int i = 0; i < sd.length; i++) sd[i] = num(PaperPlaces.MIN_SIDES + i);
        chips = new GameChips().add("count", S.t("الأسئلة"), qs, 1).add("time", S.t("الوقت"), secs, 1).add("kids", S.t("للأطفال"), new String[]{S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0).add("sides", S.t("الفرق"), sd, 0);
        modeChip(chips, 1);
        remembered(soundChip(chips));
    }
    @Override protected void onPause() { super.onPause(); keep(); }
    private void keep() {
        if (paper && pg != null) { List<String> r = new ArrayList<>(); for (int i = 0; i < Math.min(pg.total, pg.index + 1); i++) r.add(pg.items.get(i).id); Collections.reverse(r); GamePrefs.remember(this, id(), r); return; }
        if (game != null) { List<String> r = new ArrayList<>(game.asked()); Collections.reverse(r); GamePrefs.remember(this, id(), r); } }
    private boolean onPhones() { return phones != null && phones.room.phase() == GamePhoneRoom.Phase.PLAY; }

    @Override protected Runnable remoteWay() { return () -> { screen = Screen.SETUP; redraw(); start(); }; }
    @Override protected GameChips lobbyChips() { return chips; }
    private void start() {
        if (bank == null) { toast(S.t("قريبًا")); return; }
        paper = remoteOn(chips);
        if (paper) { startPaper(); return; }
        if (!openPhones()) return;
        screen = Screen.LOBBY; phoneLobby = true; redraw();
    }
    private void startWithPhones() {
        int n = phones.room.count();
        if (n < ClosestGame.MIN_PLAYERS) return;
        if (n > ClosestGame.MAX_PLAYERS) { toast(S.t("اللعبة تاخذ لين عشرة لاعبين")); return; }
        phones.room.start(); phoneLobby = false;
        names = phones.room.names().toArray(new String[0]);
        List<ClosestGame.Item> picks = ClosestGame.pick(bank, ClosestGame.QUESTIONS[chips.picked("count")], chips.picked("kids") == 1, new HashSet<>(GamePrefs.recent(this, id())), rnd);
        game = new ClosestGame(n, picks); score = new GameScore(n);
        askMs = ClosestGame.SECONDS[chips.picked("time")] * 1000L;
        toAsk();
    }
    /** one remote, paper and pencil: the sides are numbered (or named by the people), the TV is the clock and the reveal, the host picks the closest three */
    private void startPaper() {
        closePhones();
        int n = PaperPlaces.MIN_SIDES + chips.picked("sides");
        names = teamNames(n);
        List<ClosestGame.Item> picks = ClosestGame.pick(bank, ClosestGame.QUESTIONS[chips.picked("count")], chips.picked("kids") == 1, new HashSet<>(GamePrefs.recent(this, id())), rnd);
        pg = new PaperPlaces(n, picks); score = new GameScore(n); game = null;
        askMs = ClosestGame.SECONDS[chips.picked("time")] * 1000L;
        paperAsk();
    }
    private void paperSync() { for (int i = 0; i < pg.sides; i++) score.points[i] = pg.score[i]; }
    private void paperAsk() {
        paperSync();
        if (pg.phase == PaperPlaces.Phase.END) { paperEnd(); return; }
        screen = Screen.ASK; phaseAt = System.currentTimeMillis(); redraw();
    }
    private void paperEnd() { keep(); paperSync(); screen = Screen.END; sel = 0; play(QuizSound.FANFARE); redraw(); }
    private void paperKey(boolean back, boolean ok, int dx, int dy) {
        if (back) { openPause(); return; }
        if (screen == Screen.ASK) { if (ok) { pg.reveal(); screen = Screen.REVEAL; play(QuizSound.CHIME); redraw(); } return; }
        if (screen == Screen.REVEAL) { if (ok) { pg.toPlace(); screen = Screen.PLACE; pickSel = 0; redraw(); } return; }
        int items = pg.sides + 1 + (pg.place == 0 ? 1 : 0), cols = 3;
        if (!ok) {
            int to = pickSel + dx + dy * cols;
            if (to >= 0 && to < items) { pickSel = to; redraw(); }
            return;
        }
        if (pickSel == pg.sides + 1) { pg.setExact(!pg.exact); redraw(); return; }       // «بالضبط»
        if (!pg.pick(pickSel == pg.sides ? -1 : pickSel)) { toast(S.t("اختاروا فريقًا آخر")); return; }   // a side holds one place
        play(QuizSound.CHIME);
        pickSel = 0;
        if (pg.phase == PaperPlaces.Phase.END) paperEnd(); else if (pg.phase == PaperPlaces.Phase.ASK) paperAsk(); else redraw();
    }
    private void drawPaper() {
        timer = null; ClosestGame.Item it = pg.item(); String unit = it.unit(en);
        switch (screen) {
            case ASK:
                question(S.t("السؤال %s من %s", num(pg.index + 1), num(pg.total)), it.text(en), 480);
                timer = new TickTimer((int) (askMs / 1000), 428); timer.set((int) ((askMs - (System.currentTimeMillis() - phaseAt) + 999) / 1000));
                root.addView(centered(S.t("اكتبوا رقمكم على الورقة"), 72, L.ink, true), at(1728, 110, 96, 530));
                root.addView(centered(unit.isEmpty() ? S.t("كل واحد يكتب عددًا صحيحًا، ولا أحد يشوف ورقة غيره") : S.t("الوحدة: %s", unit), 48, L.ink(0xCC), false), at(1728, 80, 96, 660));
                scoreBar(score, names, -1, 820);
                hints(S.t("اختيار"), S.t("اكشف الرقم"), S.t("رجوع"), S.t("إيقاف"));
                break;
            case REVEAL:
                question(S.t("السؤال %s من %s", num(pg.index + 1), num(pg.total)), it.text(en), 380);
                FrameLayout numBox = stamp(L.deep, L.ink, 6, false);
                numBox.addView(centered(numberText(it.value), 140, L.onDeep, true), new FrameLayout.LayoutParams(-1, -1));
                root.addView(numBox, at(960, 220, 384, 450));
                root.addView(centered((unit.isEmpty() ? "" : unit + "   ") + S.t("الأقرب يأخذ %s، وبالضبط %s", num(PaperPlaces.POINTS[0]), num(PaperPlaces.EXACT)), 44, L.ink(0xCC), false), at(1728, 70, 96, 700));
                scoreBar(score, names, -1, 820);
                hints(S.t("اختيار"), S.t("نحدد الترتيب"), S.t("رجوع"), S.t("إيقاف"));
                break;
            default: {
                String[] ord = {S.t("الأول"), S.t("الثاني"), S.t("الثالث")};
                root.addView(centered(S.t("من الأقرب؟ (%s)", ord[Math.min(pg.place, 2)]), 80, L.ink, true), at(1728, 120, 96, 90));
                root.addView(centered(S.t("الجواب: %s", numberText(it.value)) + ". " + S.t("اختاروا صاحب أقرب ورقة."), 48, L.ink(0xCC), false), at(1728, 80, 96, 222));
                int items = pg.sides + 1 + (pg.place == 0 ? 1 : 0), cols = 3; float cw = 520, ch = 120;
                for (int i = 0; i < items; i++) {
                    String label = i < pg.sides ? names[i] : i == pg.sides ? S.t("ما أحد") : (pg.exact ? S.t("بالضبط: نعم") : S.t("بالضبط: لا"));
                    TextView card = focusCard(clip(label, 52, cw - 40), 52, pickSel == i);
                    if (i < pg.sides) { card.setBackground(shapeSide(L.sides[i % L.sides.length])); if (pg.taken(i)) card.setAlpha(.4f); }
                    root.addView(card, at(cw, ch, 96 + 24 + (i % cols) * (cw + 24), 330 + (i / cols) * (ch + 24)));
                }
                hints("▲ ▼ ◀ ▶", S.t("اختاروا"), S.t("اختيار"), S.t("هذا الأقرب"), S.t("رجوع"), S.t("إيقاف"));
            }
        }
    }
    /** a card with the side's colour as a thick top line (the focus stays scale and shadow) */
    private android.graphics.drawable.Drawable shapeSide(int colour) {
        android.graphics.drawable.GradientDrawable base = new android.graphics.drawable.GradientDrawable(); base.setColor(L.tile); base.setStroke(px(6), colour);
        return base;
    }
    private void sync() { for (int i = 0; i < game.players; i++) score.points[i] = game.score[i]; }

    private void toAsk() {
        if (game.phase == ClosestGame.Phase.END) { toEnd(); return; }
        screen = Screen.ASK; phaseAt = System.currentTimeMillis();
        ClosestGame.Item it = game.item(); String unit = it.unit(en);
        for (int i = 0; i < game.players; i++) phones.room.card(i, GamePhoneRoom.TEXT, "", it.text(en), unit.isEmpty() ? "" : S.t("الوحدة: %s", unit), new String[] {unit.isEmpty() ? S.t("رقم") : unit}, null, 1);   // self = 1: the phone shows a number pad
        redraw();
    }
    private void toReveal() {
        game.reveal(); sync(); screen = Screen.REVEAL; phaseAt = System.currentTimeMillis(); play(QuizSound.CHIME);
        phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw();
    }
    private void toEnd() { keep(); sync(); screen = Screen.END; sel = 0; play(QuizSound.FANFARE); if (onPhones()) phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw(); }

    @Override protected void onPauseChanged(boolean paused, long now) { if (paused) pausedAt = now; else if (pausedAt != 0) { phaseAt += now - pausedAt; pausedAt = 0; } }
    @Override protected void onEndRound() { if (paper && pg != null) { pg.finish(); paperEnd(); return; } if (game != null) { game.finish(); toEnd(); } }

    @Override protected void onTick(long now) {
        if (paper) { if (pg != null && screen == Screen.ASK && timer != null) timer.set((int) Math.max(0, (askMs - (now - phaseAt) + 999) / 1000)); return; }
        if (game == null || !onPhones()) return;
        long age = now - phaseAt;
        if (screen == Screen.ASK) {
            for (int p = 0; p < game.players; p++) { String[] t = phones.room.typed(p); if (t != null) game.submit(p, t[0]); }
            if (game.allIn() || age >= askMs) { toReveal(); return; }
            if (timer != null) timer.set((int) ((askMs - age + 999) / 1000));
        } else if (screen == Screen.REVEAL && age >= REVEAL_MS) { game.next(); toAsk(); }
    }

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        if (paper && pg != null && screen != Screen.SETUP && screen != Screen.END) { paperKey(back, ok, dx, dy); return; }
        switch (screen) {
            case SETUP: if (keySetup(chips, back, ok, dx, dy)) start(); return;
            case LOBBY: if (back) { closePhones(); screen = Screen.SETUP; redraw(); } else if (ok) startWithPhones(); return;
            case ASK: if (back) openPause(); else if (ok) toReveal(); return;
            case REVEAL: if (back) openPause(); else if (ok) { game.next(); toAsk(); } return;
            default: {
                if (back) { finish(); return; }
                int v = keyButtons(sel, 3, ok, dx);
                if (v >= 0) { if (v != sel) { sel = v; redraw(); } return; }
                if (v == -1) { closePhones(); game = null; pg = null; screen = Screen.SETUP; start(); } else if (v == -2) { closePhones(); game = null; pg = null; screen = Screen.SETUP; redraw(); } else finish();
            }
        }
    }

    @Override protected void draw() {
        timer = null;
        if (paper && pg != null && (screen == Screen.ASK || screen == Screen.REVEAL || screen == Screen.PLACE)) { drawPaper(); return; }
        switch (screen) {
            case SETUP: setup(chips, S.t("ابدأ")); break;
            case LOBBY: drawPhoneLobby(ClosestGame.MIN_PLAYERS, ClosestGame.MAX_PLAYERS); break;
            case ASK: {
                ClosestGame.Item it = game.item();
                question(S.t("السؤال %s من %s", num(game.index + 1), num(game.total)), it.text(en), 480);
                timer = new TickTimer((int) (askMs / 1000), 428); timer.set((int) ((askMs - (System.currentTimeMillis() - phaseAt) + 999) / 1000));
                root.addView(centered(S.t("اكتبوا أقرب رقم من جوالاتكم"), 64, L.ink, true), at(1728, 90, 96, 540));
                root.addView(centered(S.t("أجاب %s من %s", num(game.answered()), num(game.players)), 56, L.ink(0xCC), true), at(1728, 80, 96, 650));
                board(score, names, false);
                hints(S.t("اختيار"), S.t("اكشف الجواب"), S.t("رجوع"), S.t("إيقاف"));
                break;
            }
            case REVEAL: {
                ClosestGame.Item it = game.item();
                question(S.t("الجواب"), numberText(it.value) + (it.unit(en).isEmpty() ? "" : " " + it.unit(en)), 330);
                List<Integer> r = game.ranking();
                for (int k = 0; k < r.size() && k < 6; k++) {
                    int p = r.get(k);
                    String line = names[p] + ": " + numberText(game.guessOf(p)) + (game.gained[p] > 0 ? "  +" + num(game.gained[p]) : "");
                    root.addView(text(clip(line, 56, 1100), 56, game.gained[p] > 0 ? L.ink : L.ink(0x99), true), at(1728, 76, 96, 380 + k * 84));
                }
                if (r.isEmpty()) root.addView(centered(S.t("ما أحد أجاب"), 64, L.ink(0xCC), true), at(1728, 90, 96, 480));
                hints(S.t("اختيار"), S.t("اللي بعده"), S.t("رجوع"), S.t("إيقاف"));
                break;
            }
            default: sync(); podium(score, names, sel);
        }
    }
    private String numberText(long v) { return groups(v); }
    /** 9372610 -> 9 372 610 in the person's digits (a thin space between the groups of three) */
    private String groups(long v) {
        String s = String.valueOf(v), out = "";
        for (int i = 0; i < s.length(); i++) { if (i > 0 && (s.length() - i) % 3 == 0) out += " "; out += s.charAt(i); }
        return DigitsText.convert(out, latin);
    }
}
