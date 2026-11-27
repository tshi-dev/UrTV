package sa.urtv.screensaver;

import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import sa.urtv.screensaver.games.emoji.EmojiGame;
import sa.urtv.screensaver.games.logic.LogicItems;
import sa.urtv.screensaver.games.maths.MathGame;
import sa.urtv.screensaver.games.quran.QuranItems;
import sa.urtv.screensaver.games.remote.CallAloudRound;

/**
 * «نادوا الجواب» (docs/GAMES-REMOTE.md): the one-remote way to play «اقرأ الإيموجي»، «ألغاز المنطق»، «كمّل الآية» and «سباق الحساب» («اللعب بالريموت بس» on their setup page, off by default).
 * The question and its options are on the TV, everybody calls the answer aloud, OK shows the right one, and the host says who called it first (the pick of «خلية الحروف»).
 * The questions are the games' own (their banks, the verses file, the generators): nothing is made up here. The rules are CallAloudRound (no Android).
 */
public class CallAloudActivity extends GameActivity {
    private enum Screen { SETUP, ASK, REVEAL, PICK, END }
    private Screen screen = Screen.SETUP;
    private String src = "emoji";
    private GameChips chips; private List<EmojiGame.Puzzle> bank; private CallAloudRound round; private GameScore score; private String[] names = new String[0];
    private List<String> askedIds = new ArrayList<>();
    private long phaseAt, askMs, pausedAt; private TickTimer timer; private int sel, pick; private final Random rnd = new Random();

    @Override protected String id() { return src + "-remote"; }
    @Override protected String lastId() { return src; }
    @Override protected String gameName() { GameRegistry.Game g = GameRegistry.byId(src); return g == null ? "" : S.tv(g.name); }
    @Override protected boolean inPlay() { return screen == Screen.ASK || screen == Screen.REVEAL || screen == Screen.PICK; }
    @Override protected String namesKind() { return "teams"; }
    @Override protected int namesCount() { return CallAloudRound.MIN_SIDES + chips.picked("sides"); }

    @Override protected void onCreate(Bundle b) {
        String s = getIntent() == null ? null : getIntent().getStringExtra("src");
        src = "logic".equals(s) || "quran-verse".equals(s) || "maths".equals(s) ? s : "emoji";
        super.onCreate(b);
        if (src.equals("emoji")) bank = EmojiGame.parse(GameBundles.load(this, "emoji"));
        else if (src.equals("logic")) bank = LogicItems.bank(GameBundles.load(this, "logic"));
        else if (src.equals("quran-verse")) { try (java.io.InputStream in = getAssets().open("shared/verses.json")) { bank = QuranItems.verses(MiniJson.parse(new String(Bounded.readAll(in, 4_000_000), java.nio.charset.StandardCharsets.UTF_8))); } catch (Exception e) { bank = null; } }
        int[] secsOf = src.equals("maths") ? MathGame.SECONDS : EmojiGame.SECONDS;
        String[] qs = new String[EmojiGame.QUESTIONS.length], secs = new String[secsOf.length], sd = new String[CallAloudRound.MAX_SIDES - CallAloudRound.MIN_SIDES + 1];
        for (int i = 0; i < qs.length; i++) qs[i] = num(EmojiGame.QUESTIONS[i]);
        for (int i = 0; i < secs.length; i++) secs[i] = S.t("%s ثانية", num(secsOf[i]));
        for (int i = 0; i < sd.length; i++) sd[i] = num(CallAloudRound.MIN_SIDES + i);
        chips = new GameChips().add("count", S.t("الأسئلة"), qs, 0).add("time", S.t("الوقت"), secs, 1).add("sides", S.t("الفرق"), sd, 0);
        if (src.equals("emoji")) chips.add("kids", S.t("للأطفال"), new String[]{S.tv(ON_OFF[0]), S.tv(ON_OFF[1])}, 0);
        if (src.equals("maths")) chips.add("level", S.t("المستوى"), new String[]{S.t("سهل"), S.t("متوسط"), S.t("صعب")}, 1);
        remembered(soundChip(chips));
        android.content.Intent in = getIntent();   // the choices of the phone game's setup page go along
        for (String k : new String[] {"count", "time", "kids", "level"}) { GameChips.Row r = chips.get(k); int v = in == null ? -1 : in.getIntExtra(k, -1); if (r != null && v >= 0 && v < r.opts.length) r.picked = v; }
    }
    @Override protected void onPause() { super.onPause(); keep(); }
    /** what was asked waits in the next games on this TV (the same list as the phone game's); a made-up logic puzzle is not kept, as there */
    private void keep() {
        if (round == null) return;
        List<String> r = new ArrayList<>();
        for (int i = 0; i < Math.min(askedIds.size(), round.index + 1); i++) { String x = askedIds.get(i); if (x != null && !(src.equals("logic") && (x.startsWith("lg-order-") || x.startsWith("lg-seq-") || x.startsWith("lg-odd-") || x.startsWith("lg-age-"))) && !src.equals("maths")) r.add(x); }
        Collections.reverse(r); GamePrefs.remember(this, src, r);
    }

    private String dig(String s) { return DigitsText.convert(s, latin); }

    private void start() {
        if (!src.equals("maths") && bank == null) { toast(S.t("قريبًا")); finish(); return; }
        int sides = CallAloudRound.MIN_SIDES + chips.picked("sides"), n = EmojiGame.QUESTIONS[chips.picked("count")];
        names = teamNames(sides); score = new GameScore(sides); askedIds = new ArrayList<>();
        round = new CallAloudRound(sides, build(n));
        askMs = (src.equals("maths") ? MathGame.SECONDS : EmojiGame.SECONDS)[chips.picked("time")] * 1000L;
        toAsk();
    }
    private List<CallAloudRound.Item> build(int n) {
        List<CallAloudRound.Item> out = new ArrayList<>();
        if (src.equals("maths")) {
            for (MathGame.Question q : MathGame.generate(chips.picked("level") + 1, n, rnd)) { out.add(new CallAloudRound.Item("", dig(q.expr), null, -1, dig(String.valueOf(q.answer)), q.ar + "  " + q.en)); askedIds.add(null); }
            return out;
        }
        java.util.Set<String> recent = new HashSet<>(GamePrefs.recent(this, src));
        List<EmojiGame.Puzzle> picks, pool = bank;
        if (src.equals("logic")) {
            int fromBank = Math.min(n * 2 / 5, bank.size());
            picks = new ArrayList<>(EmojiGame.pick(bank, fromBank, false, recent, rnd)); picks.addAll(LogicItems.generate(n - picks.size(), rnd)); Collections.shuffle(picks, rnd); pool = picks;
        } else picks = EmojiGame.pick(bank, n, src.equals("emoji") && chips.picked("kids") == 1, recent, rnd);
        EmojiGame eg = new EmojiGame(CallAloudRound.MIN_SIDES, picks, pool, en, rnd);   // it deals the options (look-alike wrong answers of the same category) exactly as the phone game does
        for (int i = 0; i < picks.size(); i++) {
            EmojiGame.Puzzle p = eg.puzzle(); List<String> o = new ArrayList<>();
            for (String x : eg.options()) o.add(src.equals("logic") ? dig(x) : x);
            String prompt = src.equals("logic") ? dig(p.emoji).replace(LogicItems.SEP, '\n') : p.emoji;
            String where = src.equals("quran-verse") ? QuranItems.where(p.id) : null;
            out.add(new CallAloudRound.Item("", prompt, o, eg.rightIndex(), src.equals("logic") ? dig(p.answer(en)) : p.answer(en), where == null ? "" : dig(where)));
            askedIds.add(p.id); eg.reveal(); eg.next();
        }
        return out;
    }

    private void toAsk() {
        if (round.phase == CallAloudRound.Phase.END) { toEnd(); return; }
        screen = Screen.ASK; phaseAt = System.currentTimeMillis(); redraw();
    }
    private void toEnd() { keep(); sync(); screen = Screen.END; sel = 0; play(QuizSound.FANFARE); redraw(); }
    private void sync() { for (int i = 0; i < round.sides; i++) score.points[i] = round.score[i]; }

    @Override protected void onPauseChanged(boolean paused, long now) { if (paused) pausedAt = now; else if (pausedAt != 0) { phaseAt += now - pausedAt; pausedAt = 0; } }
    @Override protected void onEndRound() { if (round != null) { round.finish(); toEnd(); } }
    @Override protected void onTick(long now) {
        if (round == null || screen != Screen.ASK || timer == null) return;
        timer.set((int) Math.max(0, (askMs - (now - phaseAt) + 999) / 1000));   // at 0 the clock only stops: OK shows the answer
    }

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        switch (screen) {
            case SETUP: if (keySetup(chips, back, ok, dx, dy)) start(); return;
            case ASK: if (back) openPause(); else if (ok) { round.reveal(); screen = Screen.REVEAL; play(QuizSound.CHIME); redraw(); } return;
            case REVEAL: if (back) openPause(); else if (ok) { round.toPick(); screen = Screen.PICK; pick = 0; redraw(); } return;
            case PICK: {
                if (back) { openPause(); return; }
                int items = round.sides + 1, cols = 3;
                if (!ok) { int to = pick + dx + dy * cols; if (to >= 0 && to < items) { pick = to; redraw(); } return; }
                round.pick(pick == round.sides ? -1 : pick); sync(); play(QuizSound.CHIME);
                toAsk(); return;
            }
            default: {
                if (back) { finish(); return; }
                int v = keyButtons(sel, 3, ok, dx);
                if (v >= 0) { if (v != sel) { sel = v; redraw(); } return; }
                if (v == -1) { round = null; screen = Screen.SETUP; start(); } else if (v == -2) { round = null; screen = Screen.SETUP; redraw(); } else finish();
            }
        }
    }

    @Override protected void draw() {
        timer = null;
        switch (screen) {
            case SETUP: setup(chips, S.t("ابدأ")); break;
            case ASK: case REVEAL: {
                boolean rev = screen == Screen.REVEAL; CallAloudRound.Item it = round.item(); boolean opts = !it.options.isEmpty();
                question(S.t("السؤال %s من %s", num(round.index + 1), num(round.total)), it.prompt, 380);
                if (!rev) { timer = new TickTimer((int) (askMs / 1000), 328); timer.set((int) ((askMs - (System.currentTimeMillis() - phaseAt) + 999) / 1000)); }
                if (opts) {
                    for (int i = 0; i < it.options.size() && i < 4; i++)
                        root.addView(answer(letter(i), it.options.get(i), rev ? (i == it.right ? STAMP_RIGHT : STAMP_WRONG) : STAMP), at(840, 128, 96 + (i % 2) * 888, 424 + (i / 2) * 152));
                } else if (rev) {
                    android.widget.FrameLayout box = stamp(L.deep, L.ink, 6, false);
                    box.addView(centered(it.answer, 140, L.onDeep, true), new android.widget.FrameLayout.LayoutParams(-1, -1));
                    root.addView(box, at(960, 200, 384, 450));
                    if (!it.note.isEmpty()) root.addView(centered(it.note, 44, L.ink(0xCC), false), at(1728, 64, 96, 670));
                } else root.addView(centered(it.note, 56, L.ink(0xCC), true), at(1728, 90, 96, 560));
                if (rev && opts && !it.note.isEmpty()) root.addView(centered(it.note, 44, L.ink(0xCC), false), at(1728, 56, 96, 736));
                scoreBar(score, names, -1, 800);
                if (rev) hints(S.t("اختيار"), S.t("من قالها أول؟"), S.t("رجوع"), S.t("إيقاف"));
                else hints(S.t("اختيار"), S.t("اكشف الجواب"), S.t("رجوع"), S.t("إيقاف"));
                break;
            }
            case PICK: {
                CallAloudRound.Item it = round.item();
                root.addView(centered(S.t("من قالها أول؟"), 96, L.ink, true), at(1728, 130, 96, 80));
                root.addView(centered(S.t("الجواب: %s", it.answer), 48, L.ink(0xCC), false), at(1728, 70, 96, 230));
                int items = round.sides + 1; float cw = 540, ch = 150;
                for (int i = 0; i < items; i++) {
                    TextView c = focusCard(i < round.sides ? clip(names[i], 64, cw - 40) : S.t("ما أحد"), 64, pick == i);
                    if (i < round.sides) { android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable(); g.setColor(L.tile); g.setStroke(px(6), L.sides[i % L.sides.length]); c.setBackground(g); }
                    root.addView(c, at(cw, ch, 96 + 24 + (i % 3) * (cw + 24), 340 + (i / 3) * (ch + 28)));
                }
                hints("▲ ▼ ◀ ▶", S.t("اختاروا"), S.t("اختيار"), S.t("%s نقاط له", num(CallAloudRound.POINTS)), S.t("رجوع"), S.t("إيقاف"));
                break;
            }
            default: sync(); podium(score, names, sel);
        }
    }
}
