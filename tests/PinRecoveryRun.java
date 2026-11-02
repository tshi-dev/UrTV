package sa.urtv.home;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** For tests/pin_recovery_test.py: PinRecovery, PinGate, PinStore's quiet look and PinKeys, with a test-only record made by the test.
 *  args: RECORD CODE (the test's own code for that record). Prints one "name=value" line per check. */
public final class PinRecoveryRun {
    static StringBuilder out = new StringBuilder();
    static void say(String k, Object v) { out.append(k).append('=').append(v).append('\n'); }

    static String kind(PinStore.Result r) {
        return r.kind == PinGate.RECOVER ? "RECOVER" : r.kind == PinStore.OK ? "OK" : r.kind == PinStore.NOT_SET ? "NOT_SET"
                : r.kind == PinStore.LOCKED ? "LOCKED" : "WRONG";
    }

    public static void main(String[] a) {
        String rec = a[0], code = a[1];
        Map<String, String> kept = new HashMap<>();
        PinStore.Backing b = new PinStore.Backing() {
            @Override public String get(String key) { return kept.get(key); }
            @Override public void put(String key, String value) { kept.put(key, value); }
            @Override public void remove(String key) { kept.remove(key); }
        };
        say("on", PinRecovery.on(rec));
        say("on_empty", PinRecovery.on(""));
        say("on_garbage", PinRecovery.on("1:10:zz:zz"));
        say("matches", PinRecovery.matches(code, rec));
        say("matches_other", PinRecovery.matches("12345678", rec));
        say("matches_no_record", PinRecovery.matches(code, ""));

        PinStore s = new PinStore(b);
        s.set("1234");
        // what Parental keeps beside the PIN: the locks, the allowed apps, kids mode
        kept.put("locked", "com.example.a,@wifi");
        kept.put("allowed", "com.example.kids");
        kept.put("kids", "true");
        long t = 1000;
        say("recover", kind(PinGate.check(s, code, rec, null, false, t)));
        for (int i = 0; i < 7; i++) PinGate.check(s, code, rec, null, false, t + i);
        say("recover_fails", s.fails());
        say("recover_wait", s.waitLeft(t + 10));
        say("pin4_right", kind(PinGate.check(s, "1234", rec, null, false, t)));
        say("pin4_wrong", kind(PinGate.check(s, "1111", rec, null, false, t)));
        say("pin4_wrong_fails", s.fails());
        PinGate.check(s, "1234", rec, null, false, t);
        String wrongLong = code.equals("98765432") ? "98765431" : "98765432";
        say("long_wrong", kind(PinGate.check(s, wrongLong, rec, null, false, t)));
        say("long_wrong_fails", s.fails());
        for (int i = 0; i < 4; i++) PinGate.check(s, wrongLong, rec, null, false, t);
        say("long_wrong_locks", s.waitLeft(t) > 0);
        say("recover_while_locked", kind(PinGate.check(s, code, rec, null, false, t)));
        // recovery is the choose step: a new PIN; the rest stays
        s.set("5678");
        say("after_new_pin", kind(s.verify("5678", t)));
        say("after_old_pin", kind(s.verify("1234", t)));
        say("kept_locked", kept.get("locked"));
        say("kept_allowed", kept.get("allowed"));
        say("kept_kids", kept.get("kids"));
        say("plain_kept", kept.values().stream().anyMatch(v -> v.contains(code)));

        // no record: no recovery code, a long entry is a wrong try
        s.set("1234");
        say("none_long", kind(PinGate.check(s, code, "", null, false, t)));
        say("none_long_fails", s.fails());
        s.set("1234");
        // the quiet look counts nothing, its answer is counted once
        say("look_wrong", s.matches("1111", t));
        say("look_right", s.matches("1234", t));
        say("look_fails", s.fails());
        say("probed_wrong", kind(PinGate.check(s, "1111", rec, "1111", false, t)));
        say("probed_wrong_fails", s.fails());
        say("probed_right", kind(PinGate.check(s, "1234", rec, "1234", true, t)));
        say("probed_right_fails", s.fails());

        // the keypad: a build without a recovery code gives four digits up on the fourth
        PinKeys k = new PinKeys(4);
        StringBuilder w = new StringBuilder();
        for (char c : "12345".toCharArray()) w.append(k.digit(c));
        say("keys4", w + " " + k.code());
        // with one: the PIN goes once the look says so
        k = new PinKeys(8);
        w = new StringBuilder();
        for (char c : "1234".toCharArray()) w.append(k.digit(c));
        w.append(k.answer("1234", true));
        say("keys8_pin", w + " " + k.code() + " " + k.shown());
        // a wrong four, the look back first, then quiet
        k = new PinKeys(8);
        for (char c : "1111".toCharArray()) k.digit(c);
        int g = k.gen();
        say("keys8_wrong4", k.answer("1111", false) + "" + k.idle(g) + " " + k.code());
        // a wrong four, quiet before the look is back
        k = new PinKeys(8);
        for (char c : "1111".toCharArray()) k.digit(c);
        g = k.gen();
        say("keys8_slow_look", k.idle(g) + "" + k.answer("1111", false) + " " + k.code());
        // the longer code: its digits keep coming, then quiet
        k = new PinKeys(8);
        w = new StringBuilder();
        for (char c : code.toCharArray()) w.append(k.digit(c));
        w.append(k.answer(code.substring(0, 4), false));
        int stale = k.gen() - 1;
        w.append(k.idle(stale));
        w.append(k.idle(k.gen()));
        say("keys8_long", w + " " + k.code().equals(code) + " " + k.shown());
        // eight digits go at once, a ninth is not taken
        k = new PinKeys(8);
        w = new StringBuilder();
        for (char c : "123456789".toCharArray()) w.append(k.digit(c));
        say("keys8_eight", w + " " + k.code());
        // an erase back to four waits again
        k = new PinKeys(8);
        for (char c : "11112".toCharArray()) k.digit(c);
        k.answer("1111", false);
        say("keys8_erase", k.erase() + "" + k.idle(k.gen()) + " " + k.code());
        System.out.write(out.toString().getBytes(StandardCharsets.UTF_8), 0, out.length());
        System.out.flush();
    }
}
