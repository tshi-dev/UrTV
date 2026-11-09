package sa.urtv.home.phone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** For tests/phone_draw_test.py: PhoneDraw's rules on made-up batches and clocks. One line per check: name TAB ok|FAIL. */
public final class PhoneDrawRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    static List<Object> entry(long... v) { List<Object> l = new ArrayList<>(); for (long x : v) l.add(x); return l; }
    static List<Object> batch(List<?>... e) { return new ArrayList<>(Arrays.asList(e)); }
    static List<Object> run(long id, int points) { long[] v = new long[2 + 2 * points]; v[0] = id; v[1] = 2; for (int i = 0; i < points; i++) { v[2 + 2 * i] = i % 1001; v[3 + 2 * i] = 500; } return entry(v); }

    public static void main(String[] a) {
        check("a batch: id, colour, x/y pairs from 0 to 1000", PhoneDraw.valid(batch(entry(1, 0, 0, 0, 1000, 1000))));
        check("refused: a colour past the six, a coordinate past 1000, an odd pair, no point, a fraction-free but negative number",
            !PhoneDraw.valid(batch(entry(1, 6, 0, 0))) && !PhoneDraw.valid(batch(entry(1, 0, 1001, 0))) && !PhoneDraw.valid(batch(entry(1, 0, 5)))
            && !PhoneDraw.valid(batch(entry(1, 0))) && !PhoneDraw.valid(batch(entry(-1, 0, 5, 5))) && !PhoneDraw.valid(batch(entry(10000, 0, 5, 5))));
        check("refused: not a list, empty, a text inside", !PhoneDraw.valid("x") && !PhoneDraw.valid(batch()) && !PhoneDraw.valid(batch(Arrays.asList(1L, 0L, "5", 5L))));
        check("an entry at most 30 points, a batch at most 8 entries", PhoneDraw.valid(batch(run(1, 30))) && !PhoneDraw.valid(batch(run(1, 31)))
            && !PhoneDraw.valid(batch(run(1, 1), run(2, 1), run(3, 1), run(4, 1), run(5, 1), run(6, 1), run(7, 1), run(8, 1), run(9, 1))));

        PhoneDraw d = PhoneDraw.board();
        long t = 1_000_000;
        check("off: refused", d.decide(false, true, true, t) == PhoneDraw.Verdict.OFF);
        check("over another app with «فوق التطبيقات» off: refused", d.decide(true, false, false, t) == PhoneDraw.Verdict.BUSY_APP);
        int shown = 0;
        for (int i = 0; i < 40; i++) if (d.decide(true, true, true, t + i * 10) == PhoneDraw.Verdict.SHOW) shown++;
        check("at most 30 batches in one second; the next second goes on", shown == PhoneDraw.RATE && d.decide(true, true, true, t + 1500) == PhoneDraw.Verdict.SHOW);

        d = PhoneDraw.board();
        check("a stroke joins across batches by its id", d.add(batch(entry(7, 1, 10, 10, 20, 20)), t) == PhoneDraw.Verdict.SHOW
            && d.add(batch(entry(7, 1, 30, 30)), t + 50) == PhoneDraw.Verdict.SHOW && d.strokes().size() == 1 && d.strokes().get(0).points() == 3 && d.total() == 3);
        check("a long run split in one batch (the same id twice) is one stroke", d.add(batch(run(8, 30), run(8, 30)), t + 100) == PhoneDraw.Verdict.SHOW
            && d.strokes().size() == 2 && d.strokes().get(1).points() == 60);
        check("drawn while new, fading from 8 s, gone at 9 s", d.alpha(t + 100 + 8000) == 1f && d.alpha(t + 100 + 8500) > 0f && d.alpha(t + 100 + 8500) < 1f && d.alpha(t + 100 + 9000) == 0f);
        d.add(batch(entry(9, 0, 1, 1)), t + 100 + 20_000);
        check("a stroke after the fade starts a new drawing (the old one does not come back)", d.strokes().size() == 1 && d.total() == 1);
        int v = d.version();
        d.clear();
        check("«امسح»: nothing left, and the TV sees the change", d.total() == 0 && d.strokes().isEmpty() && d.alpha(t + 100 + 20_001) == 0f && d.version() != v);

        d = PhoneDraw.board();
        PhoneDraw.Verdict last = PhoneDraw.Verdict.SHOW;
        int batches = 0;
        long id = 0;
        while (last == PhoneDraw.Verdict.SHOW && batches < 1000) { last = d.add(batch(run(id++, 30), run(id++, 30), run(id++, 30), run(id++, 30), run(id++, 30), run(id++, 30), run(id++, 30), run(id++, 30)), t + batches); batches++; }
        check("a drawing stops at its cap (6000 points or 400 strokes): FULL, and nothing of the refused batch is added", last == PhoneDraw.Verdict.FULL
            && d.total() <= PhoneDraw.MAX_POINTS && d.strokes().size() <= PhoneDraw.MAX_STROKES);
        PhoneDraw e = PhoneDraw.board();
        PhoneDraw.Verdict s = PhoneDraw.Verdict.SHOW;
        for (int i = 0; i < 60 && s == PhoneDraw.Verdict.SHOW; i++) s = e.add(batch(run(1, 30)), t + i);
        check("one stroke stops at 1500 points", s == PhoneDraw.Verdict.FULL && e.strokes().get(0).points() == PhoneDraw.MAX_STROKE_POINTS);
        check("the words: shown, full, off, busy, slow", PhoneDraw.code(PhoneDraw.Verdict.SHOW).equals("draw_shown") && PhoneDraw.code(PhoneDraw.Verdict.FULL).equals("draw_full")
            && PhoneDraw.code(PhoneDraw.Verdict.OFF).equals("message_off") && PhoneDraw.code(PhoneDraw.Verdict.BUSY_APP).equals("message_busy_app") && PhoneDraw.code(PhoneDraw.Verdict.SLOW_DOWN).equals("slow_down"));
        check("six colours", PhoneDraw.PALETTE.length == PhoneDraw.COLOURS);
    }
}
