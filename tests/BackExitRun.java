package sa.urtv.home;

/** For tests/user_options_test.py: «الخروج من تلفزيونك» on the home (BackExit.java) on a plain JVM, key event by key event. */
public final class BackExitRun {
    static int failed;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : "  -> " + detail));
        if (!ok) failed++;
    }

    /** a whole press: down at t, repeats every 50 ms while held, up at t + held; what each event gave, as letters (E eat, H hint, L leave) */
    static String press(BackExit b, String mode, long t, long held, boolean repeats) {
        StringBuilder s = new StringBuilder();
        s.append(code(b.press(mode, t)));
        if (repeats) for (long r = t + 400; r < t + held; r += 50) { int x = b.repeat(r, t); if (x != BackExit.EAT) s.append(code(x)); }
        s.append(code(b.release(t + held, t)));
        return s.toString();
    }

    static String code(int r) { return r == BackExit.LEAVE ? "L" : r == BackExit.HINT ? "H" : "E"; }

    public static void main(String[] args) {
        check("fragment: hold and three read; anything else (none, an older screensaver, a near miss) is one press",
                BackExit.fromFragment("city=x&backexit=hold&panel=verse").equals("hold") && BackExit.fromFragment("backexit=three").equals("three")
                && BackExit.fromFragment(null).equals("press") && BackExit.fromFragment("city=x").equals("press")
                && BackExit.fromFragment("city=x&backexit=holdx").equals("press") && BackExit.fromFragment("xbackexit=hold").equals("press"), "");
        check("guards: never as the default home (Back never leaves it anyway)", !BackExit.guards("hold", true, false) && !BackExit.guards("three", true, false), "");
        check("guards: never with one press, the default (Back as always)", !BackExit.guards("press", false, false), "");
        check("guards: never from the scrolled grid (Back goes to the dock first)", !BackExit.guards("hold", false, true), "");
        check("guards: hold and three, opened from the tile, at the top", BackExit.guards("hold", false, false) && BackExit.guards("three", false, false), "");

        BackExit b = new BackExit();
        check("hold: a short Back stays, with the hint the first time", press(b, "hold", 1000, 120, true).equals("EH"), "");
        check("hold: a second short Back stays, no hint again", press(b, "hold", 3000, 120, true).equals("EE"), "");
        check("hold: a long Back leaves once, while still held", press(b, "hold", 5000, 1500, true).equals("ELE"), press(new BackExit(), "hold", 0, 1500, true));
        check("hold: a remote that sends no repeats leaves on the release of a long press", press(new BackExit(), "hold", 0, 900, false).equals("EL"), "");
        check("hold: just under the hold is still a short one", press(new BackExit(), "hold", 0, BackExit.HOLD_MS - 1, false).equals("EH"), "");
        BackExit stray = new BackExit();
        check("hold: a release with no press seen here does nothing", code(stray.release(500, 0)).equals("E"), "");

        BackExit t = new BackExit();
        String three = press(t, "three", 0, 100, false) + press(t, "three", 600, 100, false) + press(t, "three", 1200, 100, false);
        check("three: three Backs in a row leave on the third, the hint on the first", three.equals("HEEELE"), three);
        BackExit slow = new BackExit();
        String s = press(slow, "three", 0, 100, false) + press(slow, "three", 2000, 100, false) + press(slow, "three", 4000, 100, false);
        check("three: too far apart, each starts again and nothing leaves (the hint once)", s.equals("HEEEEE") && !s.contains("L"), s);
        BackExit again = new BackExit();
        String a = press(again, "three", 0, 100, false) + press(again, "three", 500, 100, false) + press(again, "three", 2500, 100, false)
                + press(again, "three", 3000, 100, false) + press(again, "three", 3500, 100, false);
        check("three: a pause starts the count again; three after it leave", a.equals("HEEEEEEELE"), a);
        BackExit held = new BackExit();
        check("three: a held Back is one press (its repeats do not count)", press(held, "three", 0, 2000, true).equals("HE"), "");
        BackExit visit = new BackExit();
        press(visit, "three", 0, 100, false);
        visit.reset();
        check("a new visit: the count starts again and the hint shows once more", press(visit, "three", 300, 100, false).equals("HE")
                && press(visit, "three", 600, 100, false).equals("EE"), "");
        if (failed > 0) { System.out.println(failed + " failed"); System.exit(1); }
    }
}
