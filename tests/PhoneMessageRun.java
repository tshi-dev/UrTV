package sa.urtv.home.phone;

import java.util.HashMap;
import java.util.Map;

/** For tests/phone_message_test.py: PhoneMessage's rules and the /api/message schema on made-up input. One line per check: name TAB ok|FAIL. */
public final class PhoneMessageRun {
    static void check(String name, boolean ok) { System.out.println(name + "\t" + (ok ? "ok" : "FAIL")); }

    static Map<String, Object> body(Object text) { Map<String, Object> m = new HashMap<>(); m.put("text", text); return m; }

    public static void main(String[] a) {
        long t = 1_000_000;
        PhoneMessage m = PhoneMessage.limiter();
        check("a message to a TV with messages on is shown", m.decide("العشاء جاهز", true, true, false, t) == PhoneMessage.Verdict.SHOW);
        check("messages off: never shown", PhoneMessage.limiter().decide("hi there", false, true, true, t) == PhoneMessage.Verdict.OFF);
        check("«not over apps» and another app in front: not shown; on our home: shown",
            PhoneMessage.limiter().decide("hi there", true, false, false, t) == PhoneMessage.Verdict.BUSY_APP
            && PhoneMessage.limiter().decide("hi there", true, false, true, t) == PhoneMessage.Verdict.SHOW);
        check("a second one within 3 s waits", m.decide("again", true, true, false, t + 1000) == PhoneMessage.Verdict.SLOW_DOWN
            && m.decide("again", true, true, false, t + 3000) == PhoneMessage.Verdict.SHOW);
        PhoneMessage b = PhoneMessage.limiter();
        int shown = 0;
        for (int i = 0; i < 40; i++) if (b.decide("x" + i, true, true, false, t + i * 4000L) == PhoneMessage.Verdict.SHOW) shown++;
        check("at most 20 in ten minutes from the phones", shown == PhoneMessage.BURST);
        check("…and more after the ten minutes", b.decide("later", true, true, false, t + 40 * 4000L + PhoneMessage.WINDOW_MS) == PhoneMessage.Verdict.SHOW);
        check("only spaces is nothing to show", PhoneMessage.limiter().decide("   ", true, true, true, t) == PhoneMessage.Verdict.EMPTY);
        String big = new String(new char[300]).replace('\0', 'ب');
        check("clean: spaces folded, at most 200 characters", PhoneMessage.clean("  a   b   c ").equals("a b c") && PhoneMessage.clean(big).length() == 200);
        check("the phone page's words for each verdict", PhoneMessage.code(PhoneMessage.Verdict.SHOW).equals("message_shown") && PhoneMessage.code(PhoneMessage.Verdict.OFF).equals("message_off")
            && PhoneMessage.code(PhoneMessage.Verdict.BUSY_APP).equals("message_busy_app") && PhoneMessage.code(PhoneMessage.Verdict.SLOW_DOWN).equals("slow_down"));
        check("schema: a message of up to 200 characters, one line, nothing else", Schema.check("message", body("أحبك ❤️")).ok()
            && !Schema.check("message", body(big)).ok() && !Schema.check("message", body("a\nb")).ok() && !Schema.check("message", body(5)).ok()
            && !Schema.check("message", body("x‮y")).ok());
        Map<String, Object> extra = body("hi"); extra.put("color", "red");
        check("schema: an unknown field is refused", !Schema.check("message", extra).ok());
    }
}
