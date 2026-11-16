package sa.urtv.screensaver;

import android.os.Bundle;

import java.util.Random;

import sa.urtv.screensaver.games.night.NightGame;

/**
 * «ليل القرية» (docs/GAMES-NEW.md): secret roles on the phones (villagers, wolves, a guard, a seer); at night the special roles choose on their phones, by day the village
 * talks and votes on who leaves. 6 to 10 phones. The rules are NightGame (no Android); there is no content to load. The wording is family-safe: someone is «sent away»,
 * nobody is killed. The TV never shows a role until its player leaves or the game ends.
 */
public class NightActivity extends GameActivity {
    private enum Screen { SETUP, LOBBY, ROLES, NIGHT, DAY, VOTE, VOTED, END }
    static final long ROLES_MS = 25_000, NIGHT_MS = 45_000, TALK_MS = 90_000, VOTE_MS = 35_000, VOTED_MS = 9_000;
    private Screen screen = Screen.SETUP;
    private GameChips chips; private NightGame game; private String[] names = new String[0]; private int sel; private boolean[] handled = new boolean[10];
    private long phaseAt, pausedAt; private TickTimer timer; private final Random rnd = new Random();

    @Override protected String id() { return "night"; }
    @Override protected boolean inPlay() { return screen != Screen.SETUP && screen != Screen.LOBBY && screen != Screen.END; }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        chips = new GameChips();
        remembered(soundChip(chips));
    }
    private boolean onPhones() { return phones != null && phones.room.phase() == GamePhoneRoom.Phase.PLAY; }
    private void start() { if (!openPhones()) return; screen = Screen.LOBBY; phoneLobby = true; redraw(); }
    private void startWithPhones() {
        int n = phones.room.count();
        if (n < NightGame.MIN_PLAYERS) return;
        if (n > NightGame.MAX_PLAYERS) { toast(S.t("اللعبة تتسع لعشرة لاعبين")); return; }
        phones.room.start(); phoneLobby = false;
        names = phones.room.names().toArray(new String[0]);
        game = new NightGame(n, rnd); toRoles();
    }
    private String roleName(int r) { return r == NightGame.WOLF ? S.t("ذئب") : r == NightGame.GUARD ? S.t("حارس") : r == NightGame.SEER ? S.t("عرّاف") : S.t("قروي"); }
    private String wolvesText(int except) { StringBuilder b = new StringBuilder(); for (int i = 0; i < game.players; i++) if (game.role[i] == NightGame.WOLF && i != except) b.append(b.length() > 0 ? (en ? ", " : "، ") : "").append(names[i]); return b.toString(); }

    private void toRoles() {
        screen = Screen.ROLES; phaseAt = System.currentTimeMillis();
        for (int i = 0; i < game.players; i++) {
            int r = game.role[i];
            String text = r == NightGame.WOLF ? S.t("أنت ذئب") : r == NightGame.GUARD ? S.t("أنت الحارس") : r == NightGame.SEER ? S.t("أنت العرّاف") : S.t("أنت قروي");
            String note = r == NightGame.WOLF ? (game.wolvesAlive() > 1 ? S.t("معك: %s", wolvesText(i)) : S.t("أنت الذئب الوحيد")) : r == NightGame.GUARD ? S.t("كل ليلة تحمي واحدًا") : r == NightGame.SEER ? S.t("كل ليلة تسأل عن واحد: ذئب أو لا") : S.t("اكتشفوا الذئاب بالكلام والتصويت");
            phones.room.card(i, GamePhoneRoom.SECRET, names[i], text, note, null, null, -1);
        }
        redraw();
    }
    private void toNight() {
        screen = Screen.NIGHT; phaseAt = System.currentTimeMillis(); java.util.Arrays.fill(handled, false);
        for (int i = 0; i < game.players; i++) {
            if (!game.alive[i]) { phones.room.hold(i, S.t("خرجت من القرية، تابع التلفزيون")); continue; }
            int r = game.role[i];
            if (r == NightGame.VILLAGER) { phones.room.hold(i, S.t("القرية نائمة، نم أنت أيضًا")); continue; }
            java.util.List<Integer> acts = new java.util.ArrayList<>();
            for (int j = 0; j < game.players; j++) if (game.alive[j] && j != i && !(r == NightGame.WOLF && game.role[j] == NightGame.WOLF)) acts.add(j);
            if (r == NightGame.GUARD) acts.add(i);   // the guard may protect themselves
            int[] a = new int[acts.size()]; for (int k = 0; k < a.length; k++) a[k] = acts.get(k);
            phones.room.card(i, GamePhoneRoom.PICK, names[i], r == NightGame.WOLF ? S.t("من تبعدون الليلة؟") : r == NightGame.GUARD ? S.t("من تحمي الليلة؟") : S.t("عن من تسأل الليلة؟"), "", names, a, -1);
        }
        redraw();
    }
    private void toDay() {
        game.endNight();
        if (game.phase == NightGame.Phase.END) { toEnd(); return; }
        screen = Screen.DAY; phaseAt = System.currentTimeMillis();
        phones.room.waitAll(S.t("طلع الصباح، تكلموا")); redraw();
    }
    private void toVote() {
        game.startVote(); screen = Screen.VOTE; phaseAt = System.currentTimeMillis();
        for (int i = 0; i < game.players; i++) {
            if (!game.alive[i]) { phones.room.hold(i, S.t("خرجت من القرية، تابع التلفزيون")); continue; }
            java.util.List<Integer> acts = new java.util.ArrayList<>();
            for (int j = 0; j < game.players; j++) if (game.alive[j] && j != i) acts.add(j);
            int[] a = new int[acts.size()]; for (int k = 0; k < a.length; k++) a[k] = acts.get(k);
            phones.room.card(i, GamePhoneRoom.PICK, "", S.t("مين يطلع من القرية؟"), "", names, a, -1);
        }
        redraw();
    }
    private void toVoted() {
        game.endVote();
        if (game.phase == NightGame.Phase.END) { toEnd(); return; }
        screen = Screen.VOTED; phaseAt = System.currentTimeMillis(); play(QuizSound.CHIME);
        phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw();
    }
    private void toEnd() { screen = Screen.END; sel = 0; play(QuizSound.FANFARE); if (onPhones()) phones.room.waitAll(S.t("شوفوا التلفزيون")); redraw(); }

    @Override protected void onPauseChanged(boolean paused, long now) { if (paused) pausedAt = now; else if (pausedAt != 0) { phaseAt += now - pausedAt; pausedAt = 0; } }
    @Override protected void onEndRound() { if (game != null) { game.finish(); toEnd(); } }

    @Override protected void onTick(long now) {
        if (game == null || !onPhones()) return;
        long age = now - phaseAt;
        switch (screen) {
            case ROLES: {
                boolean all = true; for (int i = 0; i < game.players; i++) all &= phones.room.replied(i);
                if (all || age >= ROLES_MS) toNight();
                break;
            }
            case NIGHT:
                for (int p = 0; p < game.players; p++) {
                    if (handled[p] || !game.needsPick(p) || !phones.room.replied(p)) continue;
                    int a = phones.room.reply(p); handled[p] = true;
                    int r = game.role[p];
                    if (r == NightGame.WOLF) game.wolf(p, a);
                    else if (r == NightGame.GUARD) game.guard(p, a);
                    else { Boolean w = game.seer(p, a); if (w != null) phones.room.card(p, GamePhoneRoom.SECRET, names[p], w ? S.t("%s ذئب", names[a]) : S.t("%s ليس ذئبًا", names[a]), S.t("لا تقولها بصوت عالٍ"), null, null, -1); }
                }
                if (game.nightDone() || age >= NIGHT_MS) toDay();
                break;
            case DAY: if (age >= TALK_MS) toVote(); else if (timer != null) timer.set((int) ((TALK_MS - age + 999) / 1000)); break;
            case VOTE:
                for (int p = 0; p < game.players; p++) if (game.alive[p]) { int a = phones.room.reply(p); if (a >= 0) game.vote(p, a); }
                if (game.allVoted() || age >= VOTE_MS) toVoted();
                break;
            case VOTED: if (age >= VOTED_MS) { game.nextNight(); toNight(); } break;
            default: break;
        }
    }

    @Override protected void onKey(boolean back, boolean ok, int dx, int dy) {
        switch (screen) {
            case SETUP: if (keySetup(chips, back, ok, dx, dy)) start(); return;
            case LOBBY: if (back) { closePhones(); screen = Screen.SETUP; redraw(); } else if (ok) startWithPhones(); return;
            case ROLES: if (back) openPause(); else if (ok) toNight(); return;
            case NIGHT: if (back) openPause(); else if (ok) toDay(); return;
            case DAY: if (back) openPause(); else if (ok) toVote(); return;
            case VOTE: if (back) openPause(); else if (ok) toVoted(); return;
            case VOTED: if (back) openPause(); else if (ok) { game.nextNight(); toNight(); } return;
            default: {
                if (back) { finish(); return; }
                int v = keyButtons(sel, 3, ok, dx);
                if (v >= 0) { if (v != sel) { sel = v; redraw(); } return; }
                if (v == -1) { closePhones(); game = null; screen = Screen.SETUP; start(); } else if (v == -2) { closePhones(); game = null; screen = Screen.SETUP; redraw(); } else finish();
            }
        }
    }

    private String aliveLine() { StringBuilder b = new StringBuilder(); for (int i = 0; i < game.players; i++) if (game.alive[i]) b.append(b.length() > 0 ? (en ? ", " : "، ") : "").append(names[i]); return b.toString(); }

    @Override protected void draw() {
        timer = null;
        switch (screen) {
            case SETUP: setup(chips, S.t("ابدأ")); break;
            case LOBBY: drawPhoneLobby(NightGame.MIN_PLAYERS, NightGame.MAX_PLAYERS); break;
            case ROLES:
                question(S.t("ليل القرية"), S.t("شوفوا أدواركم في جوالاتكم"), 420);
                root.addView(centered(S.t("%s لاعبين، منهم %s ذئاب", num(game.players), num(NightGame.wolvesFor(game.players))), 60, L.ink, true), at(1728, 90, 96, 480));
                hints(S.t("اختيار"), S.t("ابدأ الليل"), S.t("رجوع"), S.t("إيقاف"));
                break;
            case NIGHT:
                root.setBackgroundColor(L.deep);
                root.addView(centered(S.t("الليلة %s", num(game.night)), 56, L.saff, true), at(1728, 80, 96, 160));
                root.addView(centered(S.t("القرية نائمة"), 160, L.onDeep, true), at(1728, 220, 96, 320));
                root.addView(centered(S.t("الذئاب والحارس والعرّاف يختارون في جوالاتهم"), 52, L.onDeep, false), at(1728, 90, 96, 600));
                hints(S.t("اختيار"), S.t("طلع الصباح"), S.t("رجوع"), S.t("إيقاف"));
                break;
            case DAY: {
                boolean none = game.outLast < 0;
                question(S.t("طلع الصباح"), none ? (game.savedLast ? S.t("الحارس أنقذ أحدًا، ما أحد طلع") : S.t("ما أحد طلع الليلة")) : S.t("طلع من القرية: %s", names[game.outLast]), 420);
                if (!none) root.addView(centered(S.t("كان %s", roleName(game.role[game.outLast])), 64, L.ink, true), at(1728, 90, 96, 470));
                timer = new TickTimer((int) (TALK_MS / 1000), 370); timer.set((int) ((TALK_MS - (System.currentTimeMillis() - phaseAt) + 999) / 1000));
                root.addView(centered(S.t("باقي في القرية: %s", aliveLine()), 44, L.ink(0xCC), true), at(1728, 110, 96, 620));
                root.addView(centered(S.t("تكلموا: مين الذئب؟"), 64, L.ink, true), at(1728, 90, 96, 780));
                hints(S.t("اختيار"), S.t("نصوّت"), S.t("رجوع"), S.t("إيقاف"));
                break;
            }
            case VOTE:
                question(S.t("مين يطلع من القرية؟"), S.t("صوّتوا من جوالاتكم"), 330);
                root.addView(centered(S.t("صوّت %s من %s", num(game.votes()), num(game.aliveCount())), 64, L.ink, true), at(1728, 90, 96, 460));
                root.addView(centered(S.t("باقي في القرية: %s", aliveLine()), 44, L.ink(0xCC), true), at(1728, 110, 96, 620));
                hints(S.t("اختيار"), S.t("اعدّ الأصوات"), S.t("رجوع"), S.t("إيقاف"));
                break;
            case VOTED:
                question(S.t("نتيجة التصويت"), game.voteOut < 0 ? S.t("تعادل، ما أحد طلع") : S.t("طلع من القرية: %s", names[game.voteOut]), 420);
                if (game.voteOut >= 0) root.addView(centered(S.t("كان %s", roleName(game.role[game.voteOut])), 64, L.ink, true), at(1728, 90, 96, 470));
                root.addView(centered(S.t("باقي في القرية: %s", aliveLine()), 44, L.ink(0xCC), true), at(1728, 110, 96, 620));
                hints(S.t("اختيار"), S.t("الليل"), S.t("رجوع"), S.t("إيقاف"));
                break;
            default: {
                question(S.t("انتهت اللعبة"), game.winner == NightGame.VILLAGE_WINS ? S.t("فازت القرية") : game.winner == NightGame.WOLVES_WIN ? S.t("فاز الذئاب") : S.t("أُنهيت"), 330);
                StringBuilder w = new StringBuilder();
                for (int i = 0; i < game.players; i++) w.append(w.length() > 0 ? (en ? ",  " : "،  ") : "").append(names[i]).append(": ").append(roleName(game.role[i]));
                android.widget.TextView t = centered(w.toString(), 40, L.ink, true); t.setMaxLines(4);
                root.addView(t, at(1728, 240, 96, 400));
                buttons(new String[]{S.t("العب مرة ثانية"), S.t("الإعدادات"), S.t("الألعاب")}, sel, 540, 760);
                hints("◀ ▶", S.t("الاختيار"), S.t("اختيار"), S.t("تأكيد"));
            }
        }
    }
}
