package sa.urtv.home.safe;

import java.util.Arrays;
import java.util.List;

/** For tests/safe_mode_test.py: the crash-loop counter (CrashLoop) and the safe mode's report link (SafeReport) on a plain JVM. One line per check. */
public final class CrashLoopRun {
    static int failed;

    static void check(String name, boolean ok, Object detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }

    public static void main(String[] args) {
        long s = 1000L, m = 60_000L, t0 = 1_700_000_000_000L;

        CrashLoop c = CrashLoop.parse(null);
        check("nothing written: no loop, no line for the safe screen", !c.tripped() && c.count() == 0 && c.what().isEmpty() && c.newest() == 0, c.write());
        c.add("crash", t0);
        c.add("crash", t0 + 30 * s);
        check("two crashes in a minute: not yet a loop", !c.tripped(), c.write());
        c.add("crash", t0 + 70 * s);
        check("the third crash inside two minutes: a loop", c.tripped(), c.write());
        check("the line says what happened, in Arabic", c.what().equals("الشاشة الرئيسية توقفت فجأة 3 مرات متتالية، ففتحنا لك «الوضع الآمن».") , c.what());

        CrashLoop spread = CrashLoop.parse(null);
        spread.add("crash", t0);
        spread.add("crash", t0 + 61 * s);
        spread.add("crash", t0 + 2 * m + 1);
        check("three crashes over more than two minutes: not a loop", !spread.tripped(), spread.write());
        spread.add("crash", t0 + 2 * m + 30 * s);
        check("a fourth makes three inside two minutes (the window slides)", spread.tripped(), spread.write());

        CrashLoop edge = CrashLoop.parse(null);
        edge.add("crash", t0);
        edge.add("crash", t0 + m);
        edge.add("crash", t0 + 2 * m);
        check("exactly two minutes from the first to the third: a loop (3 in 2 minutes)", edge.tripped(), edge.write());

        CrashLoop round = CrashLoop.parse(c.write());
        check("the file reads back as written", round.write().equals(c.write()) && round.tripped(), round.write());
        round.clear();
        check("cleared (a stable minute, or «افتح الشاشة الرئيسية من جديد»): no loop, nothing to say", !round.tripped() && round.count() == 0 && round.what().isEmpty(), round.write());

        CrashLoop junk = CrashLoop.parse("crash 12\nreboot 5\ncrash x\n\nhang -4\nanr 7 8\n  hang 20  \n");
        check("unknown, broken or negative lines are dropped; the good ones are kept", junk.count() == 2 && junk.write().equals("crash 12\nhang 20\n"), junk.write());

        CrashLoop order = CrashLoop.parse(null);
        order.add("crash", t0 + 50 * s);
        order.add("hang", t0);
        check("kept in time order, whatever order they come in", order.write().startsWith("hang " + t0 + "\n"), order.write());
        check("newest is the latest trouble", order.newest() == t0 + 50 * s, order.newest());

        CrashLoop many = CrashLoop.parse(null);
        for (int i = 0; i < 25; i++) many.add("crash", t0 + i * 10 * m);
        check("at most KEEP lines are kept (the oldest go)", many.count() == CrashLoop.KEEP && many.write().startsWith("crash " + (t0 + 15 * 10 * m) + "\n"), many.write());

        CrashLoop hangs = CrashLoop.parse(null);
        hangs.add("hang", t0);
        check("Android's ANR for a hang we wrote already is the same trouble: not counted twice", !hangs.addExit("anr", t0 + 6 * s) && hangs.count() == 1, hangs.write());
        check("an ANR with no hang of ours near it is counted", hangs.addExit("anr", t0 + 50 * s) && hangs.count() == 2, hangs.write());
        check("an exit reason already read is not read again (seen)", !hangs.addExit("anr", t0 + 50 * s) && !hangs.addExit("native", t0 + 40 * s) && hangs.count() == 2, hangs.write());
        check("a native crash is counted, and the three make a loop", hangs.addExit("native", t0 + 70 * s) && hangs.tripped(), hangs.write());
        check("the line for a mix of stops and hangs", hangs.what().equals("الشاشة الرئيسية توقفت أو علّقت 3 مرات متتالية، ففتحنا لك «الوضع الآمن».") , hangs.what());
        CrashLoop seen = CrashLoop.parse(hangs.write());
        seen.clear();
        check("«seen» survives a clear and a read back: an old exit reason never brings the loop back", seen.seen() == t0 + 70 * s && !seen.addExit("anr", t0 + 70 * s)
            && CrashLoop.parse(seen.write()).seen() == t0 + 70 * s, seen.write());

        CrashLoop onlyHangs = CrashLoop.parse("hang 1\nhang 2\nanr 100000\n");
        check("the line for hangs only", onlyHangs.tripped() && onlyHangs.what().equals("الشاشة الرئيسية علّقت ولم تستجب 3 مرات متتالية، ففتحنا لك «الوضع الآمن».") , onlyHangs.what());
        boolean refused = false;
        try { c.add("reboot", t0); } catch (IllegalArgumentException e) { refused = true; }
        boolean refused2 = false;
        try { c.addExit("crash", t0); } catch (IllegalArgumentException e) { refused2 = true; }
        check("only the four kinds can be written; a Java crash is never taken from the exit reasons (our handler wrote it)", refused && refused2, "");
        check("the numbers: 3 in 2 minutes, a stable minute, a hang is 8 s", CrashLoop.LIMIT == 3 && CrashLoop.WINDOW_MS == 120_000L && CrashLoop.STABLE_MS == 60_000L && CrashLoop.HANG_MS == 8_000L, "");

        // the report
        List<String> lines = SafeReport.lines(c, 34, "14", "4.0-123", "done");
        for (String l : lines) System.out.println("LINE " + l);
        String url = SafeReport.url(lines);
        System.out.println("URL " + url);
        check("the report link fits the TV's QR encoder", url != null && url.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= SafeReport.MAX_BYTES, url);
        check("the report counts the troubles", lines.contains("Home: 3 crash, 0 hang"), lines);
        List<String> odd = SafeReport.lines(c, 34, "14 <script>\nx", "4.0 build/with spaces and a very long tail", "Switching!");
        check("free text cannot get in: the release and version keep only letters, digits, dots and dashes, a strange journal state is left out",
            odd.get(2).equals("Versions: home 4.0buildwithspacesan") && odd.get(3).equals("Device: Android 14scriptx, API 34") && odd.size() == 4, odd);
        String longUrl = SafeReport.url(Arrays.asList("UrTV problem report, safe mode", "Home: 3 crash, 0 hang", "Versions: " + new String(new char[200]).replace('\0', 'x')));
        check("a report too long for the QR code loses lines from the end", longUrl != null && !longUrl.contains("Versions"), longUrl);
        check("the quote is urllib's", SafeReport.quote("a b/~é\n").equals("a%20b/~%C3%A9%0A"), SafeReport.quote("a b/~é\n"));
        System.out.println(failed == 0 ? "ALL PASS" : failed + " FAILED");
    }
}
