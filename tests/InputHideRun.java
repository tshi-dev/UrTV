package sa.urtv.home;

import java.util.*;

/** For tests/input_hide_test.py: InputHide (the inputs the owner hid from the sources popup). Prints PASS/FAIL lines. */
public final class InputHideRun {
    static int failed;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS " : "FAIL ") + name); if (!ok) failed++; }

    public static void main(String[] a) {
        String h1 = "com.tcl.tvinput/.TvPassThroughService/HW15", h2 = "com.tcl.tvinput/.TvPassThroughService/HW16", av = "com.tcl.tvinput/.TvPassThroughService/HW5";
        Set<String> s = InputHide.with(InputHide.with(new LinkedHashSet<>(), h2, true), av, true);
        check("hide two: the popup shows the rest, in the TV's order", InputHide.shown(Arrays.asList(h1, h2, av), s).equals(Collections.singletonList(0)));
        check("kept and read back", InputHide.parse(InputHide.serialize(s)).equals(s));
        check("shown again", InputHide.shown(Arrays.asList(h1, h2, av), InputHide.with(s, h2, false)).equals(Arrays.asList(0, 1)));
        check("an id with a $ or a % (as the TV writes some) is kept", InputHide.validId("com.x/.Svc$Inner/HDMI1") && InputHide.validId("a%2Fb"));
        check("anything else never: a space, a new line, a quote, empty, too long", !InputHide.validId("a b") && !InputHide.validId("a\nb") && !InputHide.validId("a'b")
            && !InputHide.validId("") && !InputHide.validId(null) && !InputHide.validId(new String(new char[201]).replace('\0', 'a')));
        check("a bad line in the file is dropped, the good ones stay", InputHide.parse("ok.one\nbad one\n\nok/two\n").equals(new LinkedHashSet<>(Arrays.asList("ok.one", "ok/two"))));
        check("nothing hidden: everything shown", InputHide.shown(Arrays.asList(h1, h2), InputHide.parse("")).equals(Arrays.asList(0, 1)));
        System.out.println(failed == 0 ? "ALL PASS" : "FAILED " + failed);
        if (failed != 0) System.exit(1);
    }
}
