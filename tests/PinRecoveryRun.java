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
        say("recover", kind(PinGate.check(s, code, rec, null, t)));
        for (int i = 0; i < 7; i++) PinGate.check(s, code, rec, null, t + i);
        say("recover_fails", s.fails());
        say("recover_wait", s.waitLeft(t + 10));
        say("pin4_right", kind(PinGate.check(s, "1234", rec, null, t)));
        say("pin4_wrong", kind(PinGate.check(s, "1111", rec, null, t)));
        say("pin4_wrong_fails", s.fails());
        PinGate.check(s, "1234", rec, null, t);
        String wrongLong = code.equals("98765432") ? "98765431" : "98765432";
        say("long_wrong", kind(PinGate.check(s, wrongLong, rec, null, t)));
        say("long_wrong_fails", s.fails());
        for (int i = 0; i < 4; i++) PinGate.check(s, wrongLong, rec, null, t);
        say("long_wrong_locks", s.waitLeft(t) > 0);
        say("recover_while_locked", kind(PinGate.check(s, code, rec, null, t)));
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
        say("none_long", kind(PinGate.check(s, code, "", null, t)));
        say("none_long_fails", s.fails());
        s.set("1234");
        // the look at four digits is a try, counted once, whatever the keypad does next (R01)
        s.set("1234");
        PinGate g1 = new PinGate(s, rec);
        say("look_wrong", g1.look("1111", t));
        say("look_fails", s.fails());
        say("probed_wrong", kind(g1.submit("1111", t)));
        say("probed_wrong_fails", s.fails());   // the submission after the look: not counted again
        say("submit_twice", kind(g1.submit("1111", t)) + " " + s.fails());   // a second give-up of one keypad counts nothing
        PinGate g2 = new PinGate(s, rec);
        say("look_right", g2.look("1234", t));
        say("probed_right", kind(g2.submit("1234", t)));
        say("probed_right_fails", s.fails());
        // Back / reopen: 20 keypads, each a wrong look then abandoned (never submitted): locked after the 5th, the rest turned away
        s.set("1234");
        StringBuilder loop = new StringBuilder();
        int lockedAt = -1;
        for (int i = 1; i <= 20; i++) {
            if (s.waitLeft(t) > 0) { loop.append('L'); continue; }   // Parental.ask: a locked keypad is not even opened
            PinGate g = new PinGate(s, rec);
            g.look("11" + (10 + i % 90), t);   // abandoned: no submit
            if (s.waitLeft(t) > 0 && lockedAt < 0) lockedAt = i;
            loop.append('.');
        }
        say("back_loop", loop + " " + lockedAt + " " + s.fails());
        // even a look made on a keypad reopened while locked counts nothing and says no (no oracle while locked)
        PinGate gl = new PinGate(s, rec);
        say("look_while_locked", gl.look("1234", t) + " " + s.fails());
        // the look answered after the keypad closed (late callback): it was counted when made, the closed keypad changes nothing
        s.set("1234");
        PinGate late = new PinGate(s, rec);
        boolean lateAnswer = late.look("2222", t);
        say("late_look", lateAnswer + " " + s.fails());
        // cancel after extending the entry: the look on the first four stands
        PinGate ext = new PinGate(s, rec);
        ext.look("3333", t);
        say("cancel_extended", s.fails());
        // extended and submitted: one try for the whole entry, not two
        PinGate ext2 = new PinGate(s, rec);
        ext2.look("4444", t);
        say("extended_submitted", kind(ext2.submit("44445", t)) + " " + s.fails());
        // the recovery code: its first four are looked at (and counted), then refunded when it is given up whole
        PinGate rg = new PinGate(s, rec);
        rg.look(code.substring(0, 4), t);
        say("recover_refund", kind(rg.submit(code, t)) + " " + s.fails());
        // the recovery code typed on a keypad whose look locked it: refunded, the lock gone with it
        s.set("1234");
        for (int i = 0; i < 4; i++) new PinGate(s, rec).look("9999", t);
        PinGate rl = new PinGate(s, rec);
        rl.look(code.substring(0, 4).equals("1234") ? "1235" : code.substring(0, 4), t);
        boolean lockedBy = s.waitLeft(t) > 0;
        say("recover_refund_lock", lockedBy + " " + kind(rl.submit(code, t)) + " " + s.fails() + " " + (s.waitLeft(t) > 0));
        // a submission then the dismissal: dismissal settles nothing more
        s.set("1234");
        PinGate sd = new PinGate(s, rec);
        sd.look("5555", t);
        sd.submit("5555", t);
        sd.look("5556", t);   // a late look from the same, already given-up keypad
        say("submit_dismiss", s.fails());

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
