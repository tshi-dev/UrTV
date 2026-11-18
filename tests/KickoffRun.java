package sa.urtv.home.sports;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** tests/kickoff_test.py: the kick-off alert's rules (KickoffPlan) on a plain JVM. */
public class KickoffRun {
    static int failed = 0;
    static void check(String name, boolean ok, Object detail) { System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail)); if (!ok) failed++; }

    public static void main(String[] a) {
        long now = 1_800_000_000_000L;
        String json = "[{\"id\":\"11\",\"t\":\"" + (now + 3_600_000) + "\",\"h\":\"الهلال\",\"a\":\"النصر\",\"hc\":[\"#1E4FA1\",\"#FFFFFF\"],\"ac\":[\"#F7C600\",\"#1B3A8A\"],\"lg\":\"دوري روشن\"},"
            + "{\"id\":\"12\",\"t\":\"" + (now + 60_000) + "\",\"h\":\"الاتحاد\",\"a\":\"الشباب\",\"hc\":[\"red\",\"#000000\"],\"ac\":[],\"lg\":\"x\"},"
            + "{\"id\":\"bad\",\"t\":\"" + now + "\",\"h\":\"a\",\"a\":\"b\"},{\"id\":\"13\",\"t\":12,\"h\":\"a\",\"a\":\"b\"},{\"id\":\"14\",\"t\":\"" + now + "\",\"h\":\"\",\"a\":\"b\"}]";
        List<KickoffPlan.Game> g = KickoffPlan.parse(json);
        check("parse: the good games only (an id of digits, a 13-digit time, two names)", g.size() == 2 && g.get(0).id.equals("11") && g.get(1).id.equals("12"), g.size());
        check("parse: a colour that is not #RRGGBB becomes the neutral one", g.get(1).hc[0].equals("#8A8F98") && g.get(1).ac[1].equals("#5A5F68"), g.get(1).hc[0]);
        check("parse: not JSON, not a list: nothing", KickoffPlan.parse("{x").isEmpty() && KickoffPlan.parse("{\"a\":1}").isEmpty() && KickoffPlan.parse(null).isEmpty(), "");
        StringBuilder many = new StringBuilder("[");
        for (int i = 0; i < 20; i++) many.append(i > 0 ? "," : "").append("{\"id\":\"").append(100 + i).append("\",\"t\":\"").append(now + i).append("\",\"h\":\"a\",\"a\":\"b\"}");
        check("parse: at most 12", KickoffPlan.parse(many.append("]").toString()).size() == 12, "");
        Set<String> none = new HashSet<>();
        check("next: the soonest kick-off not announced", KickoffPlan.next(g, now, none) == now + 60_000, KickoffPlan.next(g, now, none));
        check("next: an announced one is skipped", KickoffPlan.next(g, now, new HashSet<>(Collections.singletonList("12"))) == now + 3_600_000, "");
        check("next: none ahead: 0", KickoffPlan.next(g, now + 2 * 3_600_000, none) == 0, "");
        check("due: at the kick-off (30 s early is on time), not before", KickoffPlan.due(g, now + 30_000, none).size() == 1 && KickoffPlan.due(g, now, none).isEmpty(), "");
        check("due: 15 minutes late is too late (a TV that was off says nothing old)", KickoffPlan.due(g, now + 60_000 + KickoffPlan.LATE_MS, none).isEmpty(), "");
        check("due: once (an announced game is not due again)", KickoffPlan.due(g, now + 61_000, new HashSet<>(Collections.singletonList("12"))).isEmpty(), "");
        check("confirmed: an answer that is not called off", KickoffPlan.confirmed(true, "NS", false) && KickoffPlan.confirmed(true, "1H", false) && KickoffPlan.confirmed(true, "", false), "");
        check("confirmed: no answer (no network): nothing", !KickoffPlan.confirmed(false, "", false), "");
        check("confirmed: postponed, cancelled, abandoned: nothing", !KickoffPlan.confirmed(true, "PST", false) && !KickoffPlan.confirmed(true, "Match Postponed", false)
            && !KickoffPlan.confirmed(true, "CANC", false) && !KickoffPlan.confirmed(true, "ABD", false) && !KickoffPlan.confirmed(true, "NS", true), "");
        check("where: the home always (kids mode too)", KickoffPlan.where(0, false, false, false) == KickoffPlan.HOME && KickoffPlan.where(0, true, true, true) == KickoffPlan.HOME, "");
        check("where: the screensaver draws its own", KickoffPlan.where(1, false, false, false) == KickoffPlan.SAVER, "");
        check("where: another app only with «فوق تطبيق آخر»: the overlay with the permission, else a text notice", KickoffPlan.where(2, false, false, true) == KickoffPlan.NONE
            && KickoffPlan.where(2, false, true, true) == KickoffPlan.OVERLAY && KickoffPlan.where(2, false, true, false) == KickoffPlan.TOAST, "");
        check("where: kids mode: the home only (no screensaver notice, nothing over an app)", KickoffPlan.where(1, true, true, true) == KickoffPlan.NONE && KickoffPlan.where(2, true, true, true) == KickoffPlan.NONE, "");
        check("sound: the whistle when chosen and a notice shows", KickoffPlan.sound(true, false, false, false, KickoffPlan.HOME), "");
        check("sound: never «بدون صوت», kids mode, the quiet hours, the adhan, or no notice", !KickoffPlan.sound(false, false, false, false, KickoffPlan.HOME) && !KickoffPlan.sound(true, true, false, false, KickoffPlan.HOME)
            && !KickoffPlan.sound(true, false, true, false, KickoffPlan.HOME) && !KickoffPlan.sound(true, false, false, true, KickoffPlan.HOME) && !KickoffPlan.sound(true, false, false, false, KickoffPlan.NONE), "");
        String n = g.get(0).noticeJson();
        check("the notice's data for spKick (names, colours, competition), quotes escaped", n.equals("{\"kind\":\"start\",\"h\":\"الهلال\",\"a\":\"النصر\",\"hc\":[\"#1E4FA1\",\"#FFFFFF\"],\"ac\":[\"#F7C600\",\"#1B3A8A\"],\"lg\":\"دوري روشن\"}")
            && KickoffPlan.parse("[{\"id\":\"1\",\"t\":\"" + now + "\",\"h\":\"a\\\"<b>\",\"a\":\"c\"}]").get(0).noticeJson().contains("\"h\":\"a\\\" b\""), n);
        KickoffPlan.Game back = KickoffPlan.parse("[" + g.get(0).json() + "]").get(0);
        check("a game's json() is read back by parse() unchanged (the home's broadcast)", back.id.equals(g.get(0).id) && back.t == g.get(0).t && back.h.equals(g.get(0).h)
            && back.a.equals(g.get(0).a) && back.lg.equals(g.get(0).lg) && back.hc[0].equals(g.get(0).hc[0]) && back.ac[1].equals(g.get(0).ac[1]), g.get(0).json());
        check("the text notice over another app", g.get(0).line("بدأت المباراة").equals("بدأت المباراة: الهلال × النصر"), g.get(0).line("بدأت المباراة"));
        System.exit(failed == 0 ? 0 : 1);
    }
}
