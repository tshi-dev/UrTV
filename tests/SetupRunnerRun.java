package sa.urtv.home.setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Runs SetupRunner against the fake TV of SetupEngineRun on a plain JVM. Prints one line per scenario for tests/setup_runner_test.py. */
public class SetupRunnerRun {
    /** A host on the fake TV. inline: the job runs at once on this thread; else it waits in `held` until the test runs it. */
    static class Host implements SetupRunner.Host {
        final SetupEngineRun.FakeTv tv;
        final SetupEngineRun.Mem store;
        final SetupEngineRun.Dead dead = new SetupEngineRun.Dead();
        boolean inline = true;
        String refuse;              // open() throws this message
        int opens, finished, sleeps, fresh, freshAtOpen = -1;
        final List<Runnable> held = new ArrayList<>();
        SetupRunner r;              // the runner this host serves: the person's answer at the start screen is given from sleep()
        Boolean answer = true;      // «ابدأ» (true), «إلغاء» (false), nothing (null: the ten minutes run out)
        Boolean extrasAnswer;       // the extras box at «ابدأ» (null: as the run was started)
        int confirmSleeps;          // sleeps while the start screen waited
        boolean promptSilent;       // the TV's prompt never gets an answer: open() waits (the clock moves) and then fails
        long clock;                 // added to now()
        final List<Runnable> laters = new ArrayList<>();
        SetupRunner.Preview seen;   // the start screen's plan, as the person saw it

        Host(SetupEngineRun.FakeTv tv, SetupEngineRun.Mem store) { this.tv = tv; this.store = store; }

        public SetupRunner.Session open(int approvalMs, AdbClient.Approval approval, SetupEngine.Out out) throws IOException {
            opens++;
            if (refuse != null) throw new IOException(refuse);
            if (approval != null) approval.waiting();
            if (promptSilent) {   // nobody answers: 21 s pass, the notice is due; then the 180 s run out
                clock += 21_000;
                for (Runnable l : new ArrayList<>(laters)) l.run();
                late = r != null && r.promptLate();
                StringBuilder said = new StringBuilder();
                if (r != null) for (SetupRunner.Line l : r.linesSince(0)) said.append(l.text).append('\n');
                lateLines = said.toString();
                throw new IOException("debugging permission was not accepted on the TV");
            }
            final SetupEngine e = new SetupEngine(tv, store, out, dead, ms -> { }, "2026-10-29 12:00:00");
            return new SetupRunner.Session() {
                public SetupEngine engine() { return e; }
                public SetupEngine.Shell shell() { return tv; }
                public String exec(String command, java.io.InputStream data, long size) throws IOException {
                    execs.add(command);
                    long n = 0;
                    byte[] buf = new byte[4096];
                    int r;
                    while (n < size && (r = data.read(buf, 0, (int) Math.min(buf.length, size - n))) > 0) n += r;
                    if (n != size) return "Failure [short]\n";
                    String pkg = new String(lastApk, java.nio.charset.StandardCharsets.UTF_8);
                    tv.installed.add(pkg);
                    return "Success\n";
                }
                public void close() { }
            };
        }
        public SetupEngine.Store store() { return store; }
        public void spawn(Runnable job) { if (inline) job.run(); else held.add(job); }
        public void finished() { finished++; }
        public void freshKey() { fresh++; }
        boolean late;
        String lateLines = "";
        public void sleep(int ms) {
            sleeps++;
            if (r != null && r.awaitingConfirm()) {
                confirmSleeps++;
                if (seen == null) seen = r.preview();
                if (answer == null) return;
                if (answer) r.confirm(extrasAnswer != null ? extrasAnswer : r.options().extras); else r.cancel();
            }
        }
        public long now() { return 1_000_000L + sleeps + clock; }
        public void later(int ms, Runnable job) { laters.add(job); }
        String uad;                 // what the internet gives for UAD-ng's list (null: offline)
        public String asset(String name) {
            try { return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(System.getProperty("assets"), name)), java.nio.charset.StandardCharsets.UTF_8); }
            catch (IOException | RuntimeException e) { return null; }
        }
        public String fetch(String url, int maxBytes) {
            if (url.startsWith("https://api.github.com/repos/truefedex/tv-bro/")) return "{\"assets\": [{\"name\": \"x\", \"browser_download_url\": \"https://github.com/truefedex/tv-bro/releases/download/v2.0.1/tvbro-2.0.1-generic-geckoExcluded.apk\"}, {\"browser_download_url\": \"https://github.com/truefedex/tv-bro/releases/download/v2.0.1/tvbro-2.0.1-generic-geckoIncluded.apk\"}]}";
            if (url.startsWith("https://api.github.com/repos/yuliskov/SmartTube/")) return "{\"assets\": [{\"browser_download_url\": \"https://github.com/yuliskov/SmartTube/releases/download/29.1/SmartTube_stable_29.1_armeabi-v7a.apk\"}, {\"browser_download_url\": \"https://github.com/yuliskov/SmartTube/releases/download/29.1/SmartTube_stable_29.1_arm64-v8a.apk\"}]}";
            return uad;
        }
        final List<String> execs = new ArrayList<>(), downloads = new ArrayList<>();
        byte[] lastApk = new byte[0];
        String selfSig = "ab".repeat(32), apkSig = "ab".repeat(32);
        public SetupInstall.Apks apks() {
            return new SetupInstall.Apks() {
                public SetupInstall.Apk download(String url) {
                    downloads.add(url);
                    String pkg = url.endsWith("urtv-screensaver.apk") ? "sa.urtv.screensaver" : url.contains("tv-bro") ? "com.phlox.tvwebbrowser" : url.contains("SmartTube") ? "org.smarttube.stable" : "x";
                    final byte[] bytes = pkg.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    return new SetupInstall.Apk(pkg, 200, new java.util.HashSet<>(java.util.Collections.singleton(pkg.startsWith("sa.") ? apkSig : "cd".repeat(32))), bytes.length,
                        () -> { lastApk = bytes; return new java.io.ByteArrayInputStream(bytes); }, () -> { });
                }
                public java.util.Set<String> selfSigners() { return new java.util.HashSet<>(java.util.Collections.singleton(selfSig)); }
                public long installedCode(String pkg) { return tv.installed.contains(pkg) ? 100 : -1; }
                public java.util.Set<String> installedSigners(String pkg) { return selfSigners(); }
            };
        }
        public String appVersion() { return "4.0 (build 151)"; }
    }

    static String states(SetupRunner r) {
        StringBuilder b = new StringBuilder();
        for (SetupRunner.Step s : r.steps()) b.append(b.length() == 0 ? "" : ",").append(s.id).append(':').append(s.state);
        return b.toString();
    }

    static void line(String s) { System.out.println(s); }

    /** Starts a run with the person at the TV (the host answers the start screen for them). */
    static SetupRunner.Start go(SetupRunner r, Host h, SetupRunner.Kind k, SetupRunner.Options o) {
        h.r = r;
        return r.start(k, o, h);
    }

    static boolean resume(SetupRunner r, Host h) {
        h.r = r;
        return r.resume(h);
    }

    static String FULLDONE(SetupRunner r) {
        StringBuilder b = new StringBuilder();
        for (String id : SetupRunner.ids(SetupRunner.plan(SetupRunner.Kind.FULL))) b.append(b.length() == 0 ? "" : ",").append(id).append(":DONE");
        return b.toString();
    }

    static int changes(SetupEngineRun.FakeTv t) {
        return t.count("pm disable-user") + t.count("settings put") + t.count("cmd role add") + t.count("appops set") + t.count("pm grant") + t.count("pm enable") + t.count("cmd appops set") + t.count("am set-standby-bucket");
    }

    static int indexStarting(List<String> log, String prefix) {
        for (int i = 0; i < log.size(); i++) if (log.get(i).startsWith(prefix)) return i;
        return -1;
    }

    static int count(List<SetupRunner.Line> lines, String part) {
        int n = 0;
        for (SetupRunner.Line l : lines) if (l.text.contains(part)) n++;
        return n;
    }

    /** The steps in the order they started (steps.log's «start x» lines). */
    static List<String> startedOrder(String log) {
        List<String> out = new ArrayList<>();
        if (log == null) return out;
        for (String l : log.split("\n")) {
            int i = l.indexOf(" start ");
            if (i > 0) out.add(l.substring(i + 7).trim());
        }
        return out;
    }

    /** A store that keeps every «step» the home journal was given, in order (home-journal.sh's jstep: written before the step acts). */
    static final class StepMem extends SetupEngineRun.Mem {
        final List<String> steps = new ArrayList<>();
        @Override public void put(String name, String text) {
            if (name.equals("home-switch.json")) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"step\": \"([a-z_]+)\"").matcher(text);
                if (m.find() && (steps.isEmpty() || !steps.get(steps.size() - 1).equals(m.group(1)))) steps.add(m.group(1));
            }
            super.put(name, text);
        }
    }

    public static void main(String[] a) throws Exception {
        line("PLAN " + String.join(",", SetupRunner.ids(SetupRunner.plan(SetupRunner.Kind.FULL))));
        // 1. the TV's button on a healthy TV: the five steps, in order, done; the end is the screen's old sentence; the key is let go once
        SetupRunner r = new SetupRunner();
        SetupEngineRun.FakeTv t = SetupEngineRun.tv(); SetupEngineRun.Mem m = new SetupEngineRun.Mem(); Host h = new Host(t, m);
        final int[] calls = {0};
        r.setListener(() -> calls[0]++);
        SetupRunner.Start s = go(r, h, SetupRunner.Kind.FULL, new SetupRunner.Options());
        line("R1 " + s + " " + states(r) + " running=" + r.running() + " finished=" + h.finished + " bad=" + r.end().bad + " listened=" + (calls[0] > 5));
        line("R1end " + r.end().text);
        // tester step 18: the second run did not ask: a key a run kept is let go again BEFORE a fresh run and an undo (when nothing is pending)
        SetupRunner rk = new SetupRunner(); Host hk = new Host(SetupEngineRun.tv(), new SetupEngineRun.Mem());
        go(rk, hk, SetupRunner.Kind.FULL, new SetupRunner.Options());
        int afterFull = hk.fresh;
        go(rk, hk, SetupRunner.Kind.UNDO, new SetupRunner.Options());
        int afterUndo = hk.fresh;
        go(rk, hk, SetupRunner.Kind.ANSWERS, new SetupRunner.Options());
        line("RK " + afterFull + " " + afterUndo + " " + hk.fresh);
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
        SetupRunner.Start first = go(r2, h2, SetupRunner.Kind.FULL, new SetupRunner.Options());
        SetupRunner.Start second = go(r2, h2, SetupRunner.Kind.UNDO, new SetupRunner.Options());
        SetupRunner.Start third = go(r2, h2, SetupRunner.Kind.FULL, new SetupRunner.Options());
        int queued = h2.held.size();
        SetupRunner.Kind during = r2.kind();
        h2.held.get(0).run();
        SetupRunner.Start after = go(r2, h2, SetupRunner.Kind.UNDO, new SetupRunner.Options());
        line("R2 " + first + " " + second + " " + third + " queued=" + queued + " kind=" + during + " after=" + after);
        // 3. the ring keeps the newest lines, with their numbers
        SetupRunner r3 = new SetupRunner();
        for (int i = 0; i < SetupRunner.RING + 50; i++) r3.line("l" + i);
        List<SetupRunner.Line> kept = r3.linesSince(0);
        line("R3 " + kept.size() + " " + kept.get(0).seq + " " + kept.get(0).text + " " + r3.lastSeq() + " " + r3.linesSince(r3.lastSeq() - 3).size());
        // 4. not an Android TV: the precheck fails, nothing else runs (no change sent), the old «stopped before any change» end
        SetupRunner r4 = new SetupRunner();
        SetupEngineRun.FakeTv t4 = SetupEngineRun.tv(); t4.silent = true; Host h4 = new Host(t4, new SetupEngineRun.Mem());
        go(r4, h4, SetupRunner.Kind.FULL, new SetupRunner.Options());
        line("R4 " + states(r4) + " changes=" + (t4.count("pm disable-user") + t4.count("settings put") + t4.count("cmd role add") + t4.count("appops set") + t4.count("pm grant"))
            + " bad=" + r4.end().bad + " " + r4.end().text);
        // 5. a connection that cannot be made: the old sentences, red, the key still let go
        SetupRunner r5 = new SetupRunner();
        Host h5 = new Host(SetupEngineRun.tv(), new SetupEngineRun.Mem()); h5.refuse = "debugging is off on the TV (port 5555 closed)";
        go(r5, h5, SetupRunner.Kind.FULL, new SetupRunner.Options());
        Host h5b = new Host(SetupEngineRun.tv(), new SetupEngineRun.Mem()); h5b.refuse = "debugging permission was not accepted on the TV";
        SetupRunner r5b = new SetupRunner(); go(r5b, h5b, SetupRunner.Kind.UNDO, new SetupRunner.Options());
        line("R5 " + r5.end().bad + " " + h5.finished + " " + r5.end().text.startsWith("تصحيح USB مطفأ") + " " + r5b.end().text.startsWith("لم تُقبل رسالة") + " running=" + r5.running());
        // 6. the undo through the runner: one step, the old sentence
        SetupRunner r6 = new SetupRunner();
        SetupEngineRun.FakeTv t6 = SetupEngineRun.tv(); SetupEngineRun.Mem m6 = new SetupEngineRun.Mem(); Host h6 = new Host(t6, m6);
        go(r6, h6, SetupRunner.Kind.FULL, new SetupRunner.Options());
        go(r6, h6, SetupRunner.Kind.UNDO, new SetupRunner.Options());
        line("R6 " + states(r6) + " " + t6.roleHolder + " " + r6.end().text.startsWith("تم الرجوع"));
        // 7. a full run cut in the middle of the home switch: after the restart the home switch is NOT run again (said), the rest goes on
        SetupRunner r7 = new SetupRunner();
        SetupEngineRun.FakeTv t7 = SetupEngineRun.tv(); SetupEngineRun.Mem m7 = new SetupEngineRun.Mem(); Host h7 = new Host(t7, m7);
        m7.put(SetupRunner.STATE_FILE, "kind=FULL\nextras=0\nat=home\nphase=started\ntime=" + System.currentTimeMillis() + "\ns.precheck=DONE\tok\ns.grants=DONE\tok\ns.safe=DONE\tok\ns.screensaver=DONE\tok\n");
        boolean resumable = SetupRunner.resumable(m7);
        SetupRunner.Start s7 = go(r7, h7, SetupRunner.Kind.FULL, new SetupRunner.Options());
        line("R7 " + resumable + " " + s7 + " " + states(r7) + " roleAdds=" + t7.count("cmd role add-role-holder") + " disables=" + t7.count("pm disable-user")
            + " extras=" + r7.options().extras + " cleared=" + "".equals(m7.get(SetupRunner.STATE_FILE)) + " resumableAfter=" + SetupRunner.resumable(m7) + " opens=" + h7.opens);
        // 8. a full run cut in the middle of the safe list: that step runs again (its ledger is written before it acts), the ledger is the same
        SetupEngineRun.FakeTv t8 = SetupEngineRun.tv(); SetupEngineRun.Mem m8 = new SetupEngineRun.Mem(); Host h8 = new Host(t8, m8);
        SetupRunner r8a = new SetupRunner();
        go(r8a, h8, SetupRunner.Kind.FULL, new SetupRunner.Options());
        String ledger1 = m8.get("disabled-by-us.txt");
        SetupRunner r8 = new SetupRunner();
        m8.put(SetupRunner.STATE_FILE, "kind=FULL\nextras=1\nat=safe\nphase=started\ntime=" + System.currentTimeMillis() + "\ns.precheck=DONE\tok\ns.grants=DONE\tok\n");
        go(r8, h8, SetupRunner.Kind.FULL, new SetupRunner.Options());
        line("R8 " + states(r8) + " sameLedger=" + ledger1.equals(m8.get("disabled-by-us.txt")));
        // 9. while a home switch is still «switching» after the restart, the resumed run waits (the boot path puts the stock home back first)
        SetupEngineRun.FakeTv t9 = SetupEngineRun.tv(); SetupEngineRun.Mem m9 = new SetupEngineRun.Mem(); Host h9 = new Host(t9, m9);
        m9.put("home-switch.json", "{\n  \"state\": \"switching\",\n}\n");
        m9.put(SetupRunner.STATE_FILE, "kind=FULL\nextras=1\nat=home\nphase=started\ntime=" + System.currentTimeMillis() + "\ns.precheck=DONE\t\ns.grants=DONE\t\ns.safe=DONE\t\ns.screensaver=DONE\t\n");
        SetupRunner r9 = new SetupRunner();
        resume(r9, h9);
        line("R9 waited=" + (h9.sleeps >= SetupRunner.RESUME_TRIES) + " " + states(r9));
        // 10. the TV does not come back after the restart: tried for ten minutes, then said; the pointer is kept for «ابدأ»
        SetupEngineRun.Mem m10 = new SetupEngineRun.Mem(); Host h10 = new Host(SetupEngineRun.tv(), m10); h10.refuse = "debugging is off on the TV (port 5555 closed)";
        m10.put(SetupRunner.STATE_FILE, "kind=FULL\nextras=1\nat=safe\nphase=started\ntime=" + System.currentTimeMillis() + "\ns.precheck=DONE\t\n");
        SetupRunner r10 = new SetupRunner();
        resume(r10, h10);
        line("R10 opens=" + h10.opens + " " + r10.end().bad + " " + r10.end().text.startsWith("ما رجع الاتصال") + " kept=" + SetupRunner.resumable(m10) + " auto=" + SetupRunner.awaitingRestart(m10));
        // 11. a pointer older than a day is not resumed: «ابدأ» starts afresh
        SetupEngineRun.Mem m11 = new SetupEngineRun.Mem();
        m11.put(SetupRunner.STATE_FILE, "kind=FULL\nextras=1\nat=safe\nphase=started\ntime=" + (System.currentTimeMillis() - 25L * 3600_000L) + "\nawait=restart\n");
        line("R11 " + SetupRunner.resumable(m11) + " " + SetupRunner.awaitingRestart(m11));
        // 12. the backup is the gate of the full run: a package list that came back nearly empty saves nothing, and nothing after it runs
        SetupEngineRun.FakeTv t12 = SetupEngineRun.tv(); t12.installed.clear(); t12.installed.add("sa.urtv.home");
        SetupEngineRun.Mem m12 = new SetupEngineRun.Mem(); Host h12 = new Host(t12, m12);
        SetupRunner r12 = new SetupRunner();
        go(r12, h12, SetupRunner.Kind.FULL, new SetupRunner.Options());
        // 13. the full run on a TCL, then the undo through the runner: the TV is as it was (tweaks, auto-start, apps, home, screensaver), the journals retired
        SetupEngineRun.FakeTv t13 = SetupEngineRun.tv(); t13.maker = "TCL"; t13.autoStartOps = true;
        t13.settings.put("global/window_animation_scale", "1.0"); t13.settings.put("global/transition_animation_scale", "1.0"); t13.settings.put("global/animator_duration_scale", "1.0");
        String before13 = SetupEngineRun.snapshot(t13) + "|" + t13.bucket;
        SetupEngineRun.Mem m13 = new SetupEngineRun.Mem(); Host h13 = new Host(t13, m13);
        SetupRunner r13 = new SetupRunner();
        go(r13, h13, SetupRunner.Kind.FULL, new SetupRunner.Options());
        String during13 = t13.settings.get("global/window_animation_scale") + "/" + t13.bucket + "/" + t13.appops.get("sa.urtv.home/AUTO_START");
        String states13 = states(r13);
        go(r13, h13, SetupRunner.Kind.UNDO, new SetupRunner.Options());
        line("R13 " + states13.equals(FULLDONE(r13)) + " during=" + during13 + " same=" + (SetupEngineRun.snapshot(t13) + "|" + t13.bucket).equals(before13) + " " + states(r13)
            + " journal=" + m13.get("tweaks-journal.txt").isEmpty() + " done=" + !m13.get("tweaks-journal.done").isEmpty() + " auto=" + m13.get("autostart-before.txt").isEmpty());
        // 14. the community questions and the answers through the runner (one run at a time)
        SetupEngineRun.FakeTv t14 = SetupEngineRun.tv(); t14.installed.add("com.example.voiceassistant"); t14.installed.add("com.example.updater");
        SetupEngineRun.Mem m14 = new SetupEngineRun.Mem(); Host h14 = new Host(t14, m14);
        m14.put("community-ask.txt", "com.example.voiceassistant\ncom.example.updater\n");
        List<String[]> qs = SetupRunner.questions(m14);
        java.util.Map<String, Boolean> ans = new java.util.LinkedHashMap<>(); ans.put("com.example.voiceassistant", true); ans.put("com.example.updater", false);
        SetupRunner r14 = new SetupRunner();
        SetupRunner.Start s14 = r14.answer(ans, h14);
        line("R14 " + qs.size() + " " + qs.get(0)[1].startsWith("نوقف «com.example.voiceassistant»؟") + " " + s14 + " " + states(r14) + " off=" + t14.off.contains("com.example.voiceassistant")
            + "/" + t14.off.contains("com.example.updater") + " left=[" + m14.get("community-ask.txt").trim() + "] " + r14.end().bad);
        // 15. the accurate measurement: a restart before and after, the run resumed after each boot (the runner of a new process each time)
        SetupEngineRun.FakeTv t15 = SetupEngineRun.tv(); t15.installed.remove("sa.urtv.screensaver"); t15.adbPort = "5555";
        SetupEngineRun.Mem m15 = new SetupEngineRun.Mem(); Host h15 = new Host(t15, m15);
        SetupRunner r15 = new SetupRunner();
        go(r15, h15, SetupRunner.Kind.FULL, new SetupRunner.Options());
        String first15 = r15.end() == null ? "null" : r15.end().text;
        boolean await = SetupRunner.awaitingRestart(m15);
        int rb1 = t15.reboots;
        String phase = m15.get("measure-phase.txt").split(" ")[0];
        t15.bootId = "boot-2";
        SetupRunner r15b = new SetupRunner();
        resume(r15b, h15);
        String second15 = r15b.end() == null ? "null" : r15b.end().text;
        int rb2 = t15.reboots;
        t15.bootId = "boot-3";
        SetupRunner r15c = new SetupRunner();
        resume(r15c, h15);
        line("R15 first=" + first15 + " await=" + await + " reboots=" + rb1 + " phase=" + phase + " second=" + second15 + " reboots=" + rb2 + " third=" + states(r15c) + " reboots=" + t15.reboots
            + " kinds=[" + m15.get("measure-kinds.txt").trim().replace("\n", "|") + "] rows=" + (m15.get("summary.md").contains("| before |") && m15.get("summary.md").contains("| after |")));
        line("R15i " + h15.downloads + " " + h15.execs + " installedByUs=" + SetupEngine.ledgerNames(m15.get("installed-by-us.txt")) + " saver=" + t15.settings.get("secure/screensaver_components")
            + " timeout=" + (t15.log.contains("settings put system screen_off_timeout 1800000") ? "1800000" : "?") + "->" + (t15.log.contains("settings delete system screen_off_timeout") ? "back" : "?"));
        line("R15t timeoutFile=[" + m15.get("screen-off-timeout.txt").trim() + "] value=" + t15.settings.getOrDefault("system/screen_off_timeout", "null") + " awaitAfter=" + SetupRunner.awaitingRestart(m15));
        java.nio.file.Files.write(java.nio.file.Paths.get(System.getProperty("out"), "report.md"), m15.get("report.md").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        // 19. the undo of that run removes what it installed
        SetupRunner r19 = new SetupRunner();
        go(r19, h15, SetupRunner.Kind.UNDO, new SetupRunner.Options());
        line("R19 " + states(r19) + " saver=" + t15.installed.contains("sa.urtv.screensaver") + " tvbro=" + t15.installed.contains("com.phlox.tvwebbrowser") + " smarttube="
            + t15.installed.contains("org.smarttube.stable") + " home=" + t15.installed.contains("sa.urtv.home") + " record=[" + m15.get("installed-by-us.txt").trim() + "]");
        // 16. adb would not be on 5555 after a restart: no restart, readings at once
        SetupEngineRun.FakeTv t16 = SetupEngineRun.tv(); SetupEngineRun.Mem m16 = new SetupEngineRun.Mem(); Host h16 = new Host(t16, m16);
        SetupRunner r16 = new SetupRunner();
        go(r16, h16, SetupRunner.Kind.FULL, new SetupRunner.Options());
        line("R16 " + states(r16) + " reboots=" + t16.reboots + " kinds=[" + m16.get("measure-kinds.txt").trim().replace("\n", "|") + "]");
        java.nio.file.Files.write(java.nio.file.Paths.get(System.getProperty("out"), "report-now.md"), m16.get("report.md").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        // 17. the TV ignored the restart: resumed on the same boot, it is not asked again
        SetupEngineRun.FakeTv t17 = SetupEngineRun.tv(); t17.adbPort = "5555"; SetupEngineRun.Mem m17 = new SetupEngineRun.Mem(); Host h17 = new Host(t17, m17);
        go(new SetupRunner(), h17, SetupRunner.Kind.FULL, new SetupRunner.Options());
        t17.adbPort = "";   // (so the reading after the setup does not restart either)
        SetupRunner r17 = new SetupRunner();
        resume(r17, h17);
        line("R17 reboots=" + t17.reboots + " kinds=[" + m17.get("measure-kinds.txt").trim().split("\n")[0] + "] beforeDone=" + states(r17).contains("before:DONE"));
        // 18. a fresh install signed with another key is refused
        SetupEngineRun.FakeTv t18 = SetupEngineRun.tv(); t18.installed.remove("sa.urtv.screensaver"); SetupEngineRun.Mem m18 = new SetupEngineRun.Mem(); Host h18 = new Host(t18, m18);
        h18.apkSig = "ef".repeat(32);
        SetupRunner r18 = new SetupRunner();
        go(r18, h18, SetupRunner.Kind.FULL, new SetupRunner.Options());
        String st18 = states(r18);
        line("R18 " + st18.substring(st18.indexOf("install-saver:"), st18.indexOf(",", st18.indexOf("install-saver:"))) + " saverInstalled=" + t18.installed.contains("sa.urtv.screensaver") + " recorded=" + SetupEngine.ledgerNames(m18.get("installed-by-us.txt")).contains("sa.urtv.screensaver"));
        // 20. no extras
        SetupEngineRun.FakeTv t20 = SetupEngineRun.tv(); t20.installed.remove("sa.urtv.screensaver"); SetupEngineRun.Mem m20 = new SetupEngineRun.Mem(); Host h20 = new Host(t20, m20);
        SetupRunner.Options o20 = new SetupRunner.Options(); o20.extras = false;
        SetupRunner r20 = new SetupRunner();
        go(r20, h20, SetupRunner.Kind.FULL, o20);
        String st20 = states(r20);
        line("R20 " + st20.substring(st20.indexOf("extras:"), st20.indexOf(",", st20.indexOf("extras:"))) + " downloads=" + h20.downloads.size());
        // 21. the start screen: after the precheck the run reads what it would change (nothing changed yet) and waits for «ابدأ»
        SetupEngineRun.FakeTv t21 = SetupEngineRun.tv(); t21.installed.remove("sa.urtv.screensaver"); SetupEngineRun.Mem m21 = new SetupEngineRun.Mem(); Host h21 = new Host(t21, m21);
        final int[] changesAtPlan = {-1};
        final SetupRunner r21 = new SetupRunner();
        r21.setListener(() -> { if (r21.awaitingConfirm() && changesAtPlan[0] < 0) changesAtPlan[0] = changes(t21); });
        h21.r = r21;
        r21.start(SetupRunner.Kind.FULL, new SetupRunner.Options(), h21);
        int sw = 0;
        for (String p : SetupEngine.filterSafe(SetupLists.SAFE)) if (t21.installed.contains(p) && !"com.tcl.bi".equals(p)) sw++;
        final int safeWant = sw;
        SetupRunner.Preview p21 = h21.seen;
        List<String> plan21 = SetupRunner.planLines(p21);
        line("R21 plan=" + (p21 != null) + " safe=" + (p21.safe == safeWant) + " saver=" + p21.saverInstalled + " homeOurs=" + p21.homeOurs + " wifiAsk=" + p21.wifiAsk
            + " changesBefore=" + changesAtPlan[0] + " asked=" + (h21.confirmSleeps > 0) + " line=" + plan21.stream().anyMatch(x -> x.startsWith("نوقف " + safeWant + " تطبيقاً"))
            + " saverLine=" + plan21.stream().anyMatch(x -> x.startsWith("نثبّت شاشة التوقف")) + " " + states(r21).equals(FULLDONE(r21)));
        // 22. «إلغاء» at the start screen, and no answer for ten minutes: the run ends, not one change, nothing to resume, the key let go
        SetupEngineRun.FakeTv t22 = SetupEngineRun.tv(); SetupEngineRun.Mem m22 = new SetupEngineRun.Mem(); Host h22 = new Host(t22, m22); h22.answer = false;
        SetupRunner r22 = new SetupRunner();
        go(r22, h22, SetupRunner.Kind.FULL, new SetupRunner.Options());
        SetupEngineRun.FakeTv t22b = SetupEngineRun.tv(); Host h22b = new Host(t22b, new SetupEngineRun.Mem()); h22b.answer = null;
        SetupRunner r22b = new SetupRunner();
        go(r22b, h22b, SetupRunner.Kind.FULL, new SetupRunner.Options());
        line("R22 " + states(r22).replace("precheck:SKIPPED,", "").matches("([a-z-]+:SKIPPED,?)+") + " changes=" + changes(t22) + "/" + changes(t22b) + " end=" + r22.end().text.equals(SetupRunner.NOT_STARTED)
            + "/" + r22b.end().text.equals(SetupRunner.NOT_STARTED) + " bad=" + r22.end().bad + " waited=" + h22b.confirmSleeps + " resumable=" + SetupRunner.resumable(m22) + " finished=" + h22.finished + h22b.finished);
        // 23. the prompt gets no answer: after 20 s the notice line, then the old «not accepted» sentence with the restart advice; nothing ran
        SetupEngineRun.FakeTv t23 = SetupEngineRun.tv(); Host h23 = new Host(t23, new SetupEngineRun.Mem()); h23.promptSilent = true;
        SetupRunner r23 = new SetupRunner();
        go(r23, h23, SetupRunner.Kind.FULL, new SetupRunner.Options());
        line("R23 late=" + h23.late + " said=" + h23.lateLines.contains(SetupRunner.PROMPT_LATE) + " end=" + r23.end().text.contains("أعد تشغيل التلفزيون") + " bad=" + r23.end().bad
            + " lateAfter=" + r23.promptLate() + " changes=" + changes(t23) + " holds=" + r23.running());
        // 23b. the screensaver steps aside exactly while a run goes (SetupHold reads the runner)
        SetupRunner hold = SetupRunner.get();
        hold.resetForTest();
        Host h23b = new Host(SetupEngineRun.tv(), new SetupEngineRun.Mem()); h23b.inline = false;
        boolean idle = SetupHold.on();
        go(hold, h23b, SetupRunner.Kind.FULL, new SetupRunner.Options());
        boolean holding = SetupHold.on();
        h23b.held.get(0).run();
        line("R23b " + idle + " " + holding + " " + SetupHold.on());
        hold.resetForTest();
        // 24. the setup wizard owns the saved Wi-Fi: the run pauses before the home switch (nothing of it done), the person is asked once, the key stays
        SetupEngineRun.FakeTv t24 = SetupEngineRun.tv(); t24.wizardAnswersHome = true; t24.wizardOwnsWifi = true;
        StepMem m24 = new StepMem(); Host h24 = new Host(t24, m24);
        SetupRunner r24 = new SetupRunner();
        go(r24, h24, SetupRunner.Kind.FULL, new SetupRunner.Options());
        String st24 = states(r24);
        line("R24 wifi=" + r24.end().wifi + " bad=" + r24.end().bad + " ask=" + r24.end().text.contains("انسَ الشبكة") + " home=" + st24.substring(st24.indexOf("home:"), st24.indexOf(",", st24.indexOf("home:")))
            + " beforeHomeDone=" + st24.substring(0, st24.indexOf("home:")).replaceAll("[a-z-]+:DONE,", "").isEmpty() + " roleAdds=" + t24.count("cmd role add-role-holder")
            + " stockOff=" + t24.off.contains("com.google.android.apps.tv.launcherx") + " wizardOff=" + t24.off.contains("com.google.android.tungsten.setupwraith")
            + " asked=" + m24.get("wifi-ask.txt").startsWith("asked ") + " await=" + SetupRunner.awaitingWifi(m24) + " keyKept=" + SetupRunner.awaiting(m24) + " autoBoot=" + SetupRunner.awaitingRestart(m24));
        // 25. the person forgot the network and joined it again (it moved to Settings): «كمّل» resumes at the switch, in the safe order
        t24.wizardOwnsWifi = false; t24.savedNetworks = 2;
        SetupRunner r25 = new SetupRunner();
        SetupRunner.Start s25 = go(r25, h24, SetupRunner.Kind.FULL, new SetupRunner.Options());
        int iWiz = t24.log.indexOf("pm disable-user --user 0 com.google.android.tungsten.setupwraith"), iWifi = t24.log.lastIndexOf("cmd wifi status 2> /dev/null; echo W_END; cmd wifi list-networks 2> /dev/null; echo L_END");
        int iStart = indexStarting(t24.log, "am start -W"), iStock = t24.log.indexOf("pm disable-user --user 0 com.google.android.apps.tv.launcherx"), iRole = indexStarting(t24.log, "cmd role add-role-holder --user 0 android.app.role.HOME sa.urtv.home");
        line("R25 " + s25 + " " + states(r25).equals(FULLDONE(r25)) + " order=" + (iWiz >= 0 && iWiz < iWifi && iWifi < iStart && iStart < iStock && iStock < iRole) + " steps=" + m24.steps
            + " journal=" + new SetupEngine(t24, m24, x -> { }, h24.dead, ms -> { }, "").journal("state") + " home=" + t24.home() + " askCleared=" + m24.get("wifi-ask.txt").isEmpty()
            + " wifi=" + t24.wifiConnected + "/" + t24.savedNetworks + " asks=" + count(r25.linesSince(0), "انسَ الشبكة") + " keyLetGo=" + !SetupRunner.awaiting(m24));
        // 26. safe order, but the Wi-Fi went with the wizard: everything back (the wizard on, the stock home never off), said in red, never this order again
        SetupEngineRun.FakeTv t26 = SetupEngineRun.tv(); t26.wizardAnswersHome = true; t26.wizardOwnsWifi = true; t26.wifiGoesWithWizard = true;
        StepMem m26 = new StepMem(); Host h26 = new Host(t26, m26);
        go(new SetupRunner(), h26, SetupRunner.Kind.FULL, new SetupRunner.Options());
        t26.wizardOwnsWifi = false;
        SetupRunner r26 = new SetupRunner();
        go(r26, h26, SetupRunner.Kind.FULL, new SetupRunner.Options());
        String st26 = states(r26);
        line("R26 home=" + st26.substring(st26.indexOf("home:"), st26.indexOf(",", st26.indexOf("home:"))) + " stockEverOff=" + t26.log.contains("pm disable-user --user 0 com.google.android.apps.tv.launcherx")
            + " wizardOn=" + !t26.off.contains("com.google.android.tungsten.setupwraith") + " roleBack=" + t26.roleHolder + " journal=" + new SetupEngine(t26, m26, x -> { }, h26.dead, ms -> { }, "").journal("state")
            + " steps=" + m26.steps + " never=" + m26.get("wifi-ask.txt").contains("wifi-changed") + " disarmed=" + (h26.dead.disarmed > 0) + " said=" + count(r26.linesSince(0), "الواي فاي ما بقي كما كان"));
        // 27. safe order, but our home does not come up in front when started: put back, the stock home never off, «تُرك» (not an error)
        SetupEngineRun.FakeTv t27 = SetupEngineRun.tv(); t27.wizardAnswersHome = true; t27.wizardOwnsWifi = true; t27.homeNotInFront = true;
        StepMem m27 = new StepMem(); Host h27 = new Host(t27, m27);
        go(new SetupRunner(), h27, SetupRunner.Kind.FULL, new SetupRunner.Options());
        t27.wizardOwnsWifi = false;
        SetupRunner r27 = new SetupRunner();
        go(r27, h27, SetupRunner.Kind.FULL, new SetupRunner.Options());
        String st27 = states(r27);
        line("R27 home=" + st27.substring(st27.indexOf("home:"), st27.indexOf(",", st27.indexOf("home:"))) + " stockEverOff=" + t27.log.contains("pm disable-user --user 0 com.google.android.apps.tv.launcherx")
            + " wizardOn=" + !t27.off.contains("com.google.android.tungsten.setupwraith") + " journal=" + new SetupEngine(t27, m27, x -> { }, h27.dead, ms -> { }, "").journal("state") + " steps=" + m27.steps);
        // 28. the person did not forget the network («كمّل» anyway): not asked again, the switch as before with the wizard left on
        SetupEngineRun.FakeTv t28 = SetupEngineRun.tv(); t28.wizardAnswersHome = true; t28.wizardOwnsWifi = true;
        StepMem m28 = new StepMem(); Host h28 = new Host(t28, m28);
        go(new SetupRunner(), h28, SetupRunner.Kind.FULL, new SetupRunner.Options());
        SetupRunner r28 = new SetupRunner();
        go(r28, h28, SetupRunner.Kind.FULL, new SetupRunner.Options());
        line("R28 " + (r28.end().wifi ? "paused-again" : "went-on") + " wizardEverOff=" + t28.log.contains("pm disable-user --user 0 com.google.android.tungsten.setupwraith")
            + " steps=" + m28.steps + " said=" + count(r28.linesSince(0), "ما زالت تتبع وسيط الإعداد") + " " + states(r28).equals(FULLDONE(r28)));
        // 29. a TV with only the home app (Downloader, one code), a TCL: the full run in order end to end, the screensaver downloaded, checked and
        //     installed, auto-start for both apps; then the undo: the TV as it was, what the run installed removed, this app kept
        SetupEngineRun.FakeTv t29 = SetupEngineRun.tv(); t29.installed.remove("sa.urtv.screensaver"); t29.maker = "TCL"; t29.autoStartOps = true;
        t29.settings.put("global/window_animation_scale", "1.0"); t29.settings.put("global/transition_animation_scale", "1.0"); t29.settings.put("global/animator_duration_scale", "1.0");
        String before29 = SetupEngineRun.snapshot(t29) + "|" + t29.bucket + "|" + new java.util.TreeSet<>(t29.installed);
        SetupEngineRun.Mem m29 = new SetupEngineRun.Mem(); Host h29 = new Host(t29, m29);
        SetupRunner r29 = new SetupRunner();
        go(r29, h29, SetupRunner.Kind.FULL, new SetupRunner.Options());
        String order29 = String.join(",", startedOrder(m29.get("steps.log")));
        String during29 = t29.installed.contains("sa.urtv.screensaver") + "/" + t29.settings.get("secure/screensaver_components") + "/" + t29.appops.get("sa.urtv.screensaver/AUTO_START")
            + "/" + t29.appops.get("sa.urtv.home/ASSOC_START") + "/" + t29.home();
        SetupRunner r29u = new SetupRunner();
        go(r29u, h29, SetupRunner.Kind.UNDO, new SetupRunner.Options());
        line("R29 " + states(r29).equals(FULLDONE(r29)) + " order=" + order29.equals(String.join(",", SetupRunner.ids(SetupRunner.plan(SetupRunner.Kind.FULL)))) + " during=" + during29
            + " downloads=" + h29.downloads.get(0).endsWith("/apk-latest/urtv-screensaver.apk") + " " + states(r29u) + " same=" + (SetupEngineRun.snapshot(t29) + "|" + t29.bucket + "|" + new java.util.TreeSet<>(t29.installed)).equals(before29)
            + " home=" + t29.home() + " self=" + t29.installed.contains("sa.urtv.home"));
        // 30. the same, end to end over the real ADB client and a fake adbd (the framing, the RSA key, the prompt once, `exec:` streaming): «السماح دائماً»
        //     once, the full run, then the undo on a second connection with no second prompt
        SetupEngineRun.FakeTv t30 = SetupEngineRun.tv(); t30.installed.remove("sa.urtv.screensaver");
        String before30 = SetupEngineRun.snapshot(t30) + "|" + new java.util.TreeSet<>(t30.installed);
        AdbClientRun.FakeAdbd adbd = new AdbClientRun.FakeAdbd("accept");
        adbd.tv = t30::run;
        adbd.installed = bytes -> t30.installed.add(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        java.security.KeyPair key = AdbClientRun.newKey();
        SetupEngineRun.Mem m30 = new SetupEngineRun.Mem();
        Host h30 = new Host(t30, m30) {
            @Override public SetupRunner.Session open(int approvalMs, AdbClient.Approval approval, SetupEngine.Out out) throws IOException {
                opens++;
                final AdbClient adb = AdbClient.connect("127.0.0.1", adbd.port(), key, 34, 5000, approval, AdbClientRun.B64);
                final SetupEngine e = new SetupEngine(adb::run, store, out, dead, ms -> { }, "2026-11-02 12:00:00");
                return new SetupRunner.Session() {
                    public SetupEngine engine() { return e; }
                    public SetupEngine.Shell shell() { return adb::run; }
                    public String exec(String command, java.io.InputStream data, long size) throws IOException { execs.add(command); return adb.exec(command, data, size); }
                    public void close() { adb.close(); }
                };
            }
        };
        SetupRunner r30 = new SetupRunner();
        go(r30, h30, SetupRunner.Kind.FULL, new SetupRunner.Options());
        String full30 = states(r30), saver30 = t30.installed.contains("sa.urtv.screensaver") + "/" + t30.settings.get("secure/screensaver_components") + "/" + t30.home();
        int promptsAfterRun = adbd.prompts.get();
        SetupRunner r30u = new SetupRunner();
        go(r30u, h30, SetupRunner.Kind.UNDO, new SetupRunner.Options());
        line("R30 " + full30.equals(FULLDONE(r30)) + " " + saver30 + " execs=" + h30.execs.size() + " prompts=" + promptsAfterRun + "/" + adbd.prompts.get() + " " + states(r30u)
            + " same=" + (SetupEngineRun.snapshot(t30) + "|" + new java.util.TreeSet<>(t30.installed)).equals(before30) + " shellCmds=" + (adbd.commands.size() > 100));
        line("R12 " + states(r12) + " backup=" + m12.get("backup.txt") + " changes=" + (t12.count("pm disable-user") + t12.count("settings put") + t12.count("cmd role add") + t12.count("appops set") + t12.count("pm grant") + t12.count("pm enable"))
            + " " + r12.end().bad + " " + r12.end().text);
    }
}
