package sa.urtv.home.tilawah;

/** For tests/soak_test.py: SessionPush.java on a plain JVM. Prints PASS/FAIL lines. */
public final class SessionPushRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }
    public static void main(String[] a) {
        SessionPush p = new SessionPush();
        check("the first metadata goes; the same again does not", p.metaChanged("سورة البقرة", "العفاسي", "القرآن الكريم", 7_000_000) && !p.metaChanged("سورة البقرة", "العفاسي", "القرآن الكريم", 7_000_000));
        check("a new surah, reciter or duration goes", p.metaChanged("سورة آل عمران", "العفاسي", "القرآن الكريم", 7_000_000) && p.metaChanged("سورة آل عمران", "العفاسي", "القرآن الكريم", 6_000_000));
        int playing = 3, buffering = 6; long acts = 0x37;
        boolean first = p.stateDue(playing, acts, 1f, 0, 1_000);
        int sent = 0; for (long t = 2_000, pos = 1_000; t <= 3_601_000; t += 1_000, pos += 1_000) if (p.stateDue(playing, acts, 1f, pos, t)) sent++;
        check("an hour of steady play: the state went once, not 3600 times", first && sent == 0);
        check("a seek (the position jumps): it goes", p.stateDue(playing, acts, 1f, 1_800_000, 3_602_000));
        check("a rebuffer (another state): it goes, and the play after it", p.stateDue(buffering, acts, 0f, 1_801_000, 3_603_000) && p.stateDue(playing, acts, 1f, 1_801_000, 3_605_000));
        SessionPush r = new SessionPush(); r.stateDue(playing, acts, 0f, 0, 0); int radio = 0;
        for (long t = 1_000; t <= 86_400_000L; t += 1_000) if (r.stateDue(playing, acts, 0f, 0, t)) radio++;
        check("a day of radio (position 0, nothing moves): no state after the first", radio == 0);
        check("setActive once; a new session sends everything again", p.activate() && !p.activate() && resetAll(p));
        System.exit(failed == 0 ? 0 : 1);
    }
    static boolean resetAll(SessionPush p) { p.reset(); return p.activate() && p.metaChanged("a", "b", "c", 0) && p.stateDue(3, 1, 1f, 0, 0); }
}
