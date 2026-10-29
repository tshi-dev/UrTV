package sa.urtv.home.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Runs SetupRunner against the fake TV of SetupEngineRun on a plain JVM. Prints one line per scenario for tests/setup_runner_test.py. */
public class SetupRunnerRun {
    /** A host on the fake TV. inline: the job runs at once on this thread; else it waits in `held` until the test runs it. */
    static final class Host implements SetupRunner.Host {
        final SetupEngineRun.FakeTv tv;
        final SetupEngineRun.Mem store;
        final SetupEngineRun.Dead dead = new SetupEngineRun.Dead();
        boolean inline = true;
        String refuse;              // open() throws this message
        int opens, finished, sleeps;
        final List<Runnable> held = new ArrayList<>();

        Host(SetupEngineRun.FakeTv tv, SetupEngineRun.Mem store) { this.tv = tv; this.store = store; }

        public SetupRunner.Session open(int approvalMs, AdbClient.Approval approval, SetupEngine.Out out) throws IOException {
            opens++;
            if (refuse != null) throw new IOException(refuse);
            if (approval != null) approval.waiting();
            final SetupEngine e = new SetupEngine(tv, store, out, dead, ms -> { }, "2026-10-29 12:00:00");
            return new SetupRunner.Session() {
                public SetupEngine engine() { return e; }
                public SetupEngine.Shell shell() { return tv; }
                public void close() { }
            };
        }
        public SetupEngine.Store store() { return store; }
        public void spawn(Runnable job) { if (inline) job.run(); else held.add(job); }
        public void finished() { finished++; }
        public void sleep(int ms) { sleeps++; }
        public long now() { return 1_000_000L + sleeps; }
    }

    static String states(SetupRunner r) {
        StringBuilder b = new StringBuilder();
        for (SetupRunner.Step s : r.steps()) b.append(b.length() == 0 ? "" : ",").append(s.id).append(':').append(s.state);
        return b.toString();
    }

    static void line(String s) { System.out.println(s); }

    public static void main(String[] a) throws Exception {
        // 1. the TV's button on a healthy TV: the five steps, in order, done; the end is the screen's old sentence; the key is let go once
        SetupRunner r = new SetupRunner();
        SetupEngineRun.FakeTv t = SetupEngineRun.tv(); SetupEngineRun.Mem m = new SetupEngineRun.Mem(); Host h = new Host(t, m);
        final int[] calls = {0};
        r.setListener(() -> calls[0]++);
        SetupRunner.Start s = r.start(SetupRunner.Kind.SETUP, new SetupRunner.Options(), h);
        line("R1 " + s + " " + states(r) + " running=" + r.running() + " finished=" + h.finished + " bad=" + r.end().bad + " listened=" + (calls[0] > 5));
        line("R1end " + r.end().text);
        List<SetupRunner.Line> all = r.linesSince(0);
        boolean ordered = true;
        for (int i = 1; i < all.size(); i++) if (all.get(i).seq != all.get(i - 1).seq + 1) ordered = false;
        long mid = all.get(all.size() / 2).seq;
        line("R1lines " + (all.size() > 4) + " " + ordered + " " + (r.linesSince(mid).size() == all.size() - all.size() / 2 - 1) + " " + r.linesSince(r.lastSeq()).size()
            + " prompt=" + all.get(0).text.contains("السماح بتصحيح USB"));
        line("R1nostate " + m.get(SetupRunner.STATE_FILE));
        // 2. a second start while one runs is BUSY, and nothing of it happens
        SetupRunner r2 = new SetupRunner();
        Host h2 = new Host(SetupEngineRun.tv(), new SetupEngineRun.Mem()); h2.inline = false;
        SetupRunner.Start first = r2.start(SetupRunner.Kind.SETUP, new SetupRunner.Options(), h2);
        SetupRunner.Start second = r2.start(SetupRunner.Kind.UNDO, new SetupRunner.Options(), h2);
        SetupRunner.Start third = r2.start(SetupRunner.Kind.FULL, new SetupRunner.Options(), h2);
        int queued = h2.held.size();
        SetupRunner.Kind during = r2.kind();
        h2.held.get(0).run();
        SetupRunner.Start after = r2.start(SetupRunner.Kind.UNDO, new SetupRunner.Options(), h2);
        line("R2 " + first + " " + second + " " + third + " queued=" + queued + " kind=" + during + " after=" + after);
        // 3. the ring keeps the newest lines, with their numbers
        SetupRunner r3 = new SetupRunner();
        for (int i = 0; i < SetupRunner.RING + 50; i++) r3.line("l" + i);
        List<SetupRunner.Line> kept = r3.linesSince(0);
        line("R3 " + kept.size() + " " + kept.get(0).seq + " " + kept.get(0).text + " " + r3.lastSeq() + " " + r3.linesSince(r3.lastSeq() - 3).size());
        // 4. not an Android TV: the precheck fails, nothing else runs (no change sent), the old «stopped before any change» end
        SetupRunner r4 = new SetupRunner();
        SetupEngineRun.FakeTv t4 = SetupEngineRun.tv(); t4.silent = true; Host h4 = new Host(t4, new SetupEngineRun.Mem());
        r4.start(SetupRunner.Kind.SETUP, new SetupRunner.Options(), h4);
        line("R4 " + states(r4) + " changes=" + (t4.count("pm disable-user") + t4.count("settings put") + t4.count("cmd role add") + t4.count("appops set") + t4.count("pm grant"))
            + " bad=" + r4.end().bad + " " + r4.end().text);
        // 5. a connection that cannot be made: the old sentences, red, the key still let go
        SetupRunner r5 = new SetupRunner();
        Host h5 = new Host(SetupEngineRun.tv(), new SetupEngineRun.Mem()); h5.refuse = "debugging is off on the TV (port 5555 closed)";
        r5.start(SetupRunner.Kind.SETUP, new SetupRunner.Options(), h5);
        Host h5b = new Host(SetupEngineRun.tv(), new SetupEngineRun.Mem()); h5b.refuse = "debugging permission was not accepted on the TV";
        SetupRunner r5b = new SetupRunner(); r5b.start(SetupRunner.Kind.UNDO, new SetupRunner.Options(), h5b);
        line("R5 " + r5.end().bad + " " + h5.finished + " " + r5.end().text.startsWith("تصحيح USB مطفأ") + " " + r5b.end().text.startsWith("لم تُقبل رسالة") + " running=" + r5.running());
        // 6. the undo through the runner: one step, the old sentence
        SetupRunner r6 = new SetupRunner();
        SetupEngineRun.FakeTv t6 = SetupEngineRun.tv(); SetupEngineRun.Mem m6 = new SetupEngineRun.Mem(); Host h6 = new Host(t6, m6);
        r6.start(SetupRunner.Kind.SETUP, new SetupRunner.Options(), h6);
        r6.start(SetupRunner.Kind.UNDO, new SetupRunner.Options(), h6);
        line("R6 " + states(r6) + " " + t6.roleHolder + " " + r6.end().text.startsWith("تم الرجوع"));
        // 7. a full run cut in the middle of the home switch: after the restart the home switch is NOT run again (said), the rest goes on
        SetupRunner r7 = new SetupRunner();
        SetupEngineRun.FakeTv t7 = SetupEngineRun.tv(); SetupEngineRun.Mem m7 = new SetupEngineRun.Mem(); Host h7 = new Host(t7, m7);
        m7.put(SetupRunner.STATE_FILE, "kind=FULL\nextras=0\nat=home\nphase=started\ntime=" + System.currentTimeMillis() + "\ns.precheck=DONE\tok\ns.grants=DONE\tok\ns.safe=DONE\tok\ns.screensaver=DONE\tok\n");
        boolean resumable = SetupRunner.resumable(m7);
        SetupRunner.Start s7 = r7.start(SetupRunner.Kind.FULL, new SetupRunner.Options(), h7);
        line("R7 " + resumable + " " + s7 + " " + states(r7) + " roleAdds=" + t7.count("cmd role add-role-holder") + " disables=" + t7.count("pm disable-user")
            + " extras=" + r7.options().extras + " cleared=" + "".equals(m7.get(SetupRunner.STATE_FILE)) + " resumableAfter=" + SetupRunner.resumable(m7) + " opens=" + h7.opens);
        // 8. a full run cut in the middle of the safe list: that step runs again (its ledger is written before it acts), the ledger is the same
        SetupEngineRun.FakeTv t8 = SetupEngineRun.tv(); SetupEngineRun.Mem m8 = new SetupEngineRun.Mem(); Host h8 = new Host(t8, m8);
        SetupRunner r8a = new SetupRunner();
        r8a.start(SetupRunner.Kind.FULL, new SetupRunner.Options(), h8);
        String ledger1 = m8.get("disabled-by-us.txt");
        SetupRunner r8 = new SetupRunner();
        m8.put(SetupRunner.STATE_FILE, "kind=FULL\nextras=1\nat=safe\nphase=started\ntime=" + System.currentTimeMillis() + "\ns.precheck=DONE\tok\ns.grants=DONE\tok\n");
        r8.start(SetupRunner.Kind.FULL, new SetupRunner.Options(), h8);
        line("R8 " + states(r8) + " sameLedger=" + ledger1.equals(m8.get("disabled-by-us.txt")));
        // 9. while a home switch is still «switching» after the restart, the resumed run waits (the boot path puts the stock home back first)
        SetupEngineRun.FakeTv t9 = SetupEngineRun.tv(); SetupEngineRun.Mem m9 = new SetupEngineRun.Mem(); Host h9 = new Host(t9, m9);
        m9.put("home-switch.json", "{\n  \"state\": \"switching\",\n}\n");
        m9.put(SetupRunner.STATE_FILE, "kind=FULL\nextras=1\nat=home\nphase=started\ntime=" + System.currentTimeMillis() + "\ns.precheck=DONE\t\ns.grants=DONE\t\ns.safe=DONE\t\ns.screensaver=DONE\t\n");
        SetupRunner r9 = new SetupRunner();
        r9.resume(h9);
        line("R9 waited=" + (h9.sleeps >= SetupRunner.RESUME_TRIES) + " " + states(r9));
        // 10. the TV does not come back after the restart: tried for ten minutes, then said; the pointer is kept for «ابدأ»
        SetupEngineRun.Mem m10 = new SetupEngineRun.Mem(); Host h10 = new Host(SetupEngineRun.tv(), m10); h10.refuse = "debugging is off on the TV (port 5555 closed)";
        m10.put(SetupRunner.STATE_FILE, "kind=FULL\nextras=1\nat=safe\nphase=started\ntime=" + System.currentTimeMillis() + "\ns.precheck=DONE\t\n");
        SetupRunner r10 = new SetupRunner();
        r10.resume(h10);
        line("R10 opens=" + h10.opens + " " + r10.end().bad + " " + r10.end().text.startsWith("ما رجع الاتصال") + " kept=" + SetupRunner.resumable(m10) + " auto=" + SetupRunner.awaitingRestart(m10));
        // 11. a pointer older than a day is not resumed: «ابدأ» starts afresh
        SetupEngineRun.Mem m11 = new SetupEngineRun.Mem();
        m11.put(SetupRunner.STATE_FILE, "kind=FULL\nextras=1\nat=safe\nphase=started\ntime=" + (System.currentTimeMillis() - 25L * 3600_000L) + "\nawait=restart\n");
        line("R11 " + SetupRunner.resumable(m11) + " " + SetupRunner.awaitingRestart(m11));
    }
}
