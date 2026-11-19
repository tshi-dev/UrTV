package sa.urtv.home.setup;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import sa.urtv.home.PairGuard;

/**
 * For tests/pair_commands_test.py (4.4 beta 3): «shapes FILE» prints the shape of every command in FILE (one per line, «\n» and «\\» escaped as the fake TV
 * wrote them) plus the commands made outside the fake TV (the idle delay's, the installs'); «cases» prints the guard's cases, «name<TAB>value».
 */
public final class PairGuardRun {
    static String unescape(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) { char n = s.charAt(++i); b.append(n == 'n' ? '\n' : n); } else b.append(c);
        }
        return b.toString();
    }

    public static void main(String[] a) throws Exception {
        if (a[0].equals("shapes")) {
            List<String> cmds = new ArrayList<>();
            for (String l : Files.readAllLines(Paths.get(a[1]), StandardCharsets.UTF_8)) if (!l.isEmpty()) cmds.add(unescape(l));
            cmds.add(IdleSetRule.PROBE);
            for (String m : IdleSetRule.MINUTES) cmds.addAll(IdleSetRule.commands(IdleSetRule.ms(m), "600000\n0"));
            cmds.add("cmd package install -r -S 12345678");                          // SetupInstall.stream (the exec path)
            cmds.add("cmd package install -i com.android.vending -r -S 12345678");
            TreeSet<String> out = new TreeSet<>();
            for (String c : cmds) if (c.indexOf('\n') < 0) out.add(PairGuard.shape(c));   // a command of several lines is never sent over the paired route
            for (String s : out) System.out.println(s);
            return;
        }
        String own = "ab12", other = "cd34";
        p("caller_home", PairGuard.caller(new String[] {"sa.urtv.home"}, own, own));
        p("caller_other_pkg", PairGuard.caller(new String[] {"com.evil.app"}, own, own));
        p("caller_screensaver", PairGuard.caller(new String[] {"sa.urtv.screensaver"}, own, own));
        p("caller_shared_uid", PairGuard.caller(new String[] {"sa.urtv.home", "com.evil.app"}, own, own));
        p("caller_wrong_cert", PairGuard.caller(new String[] {"sa.urtv.home"}, other, own));
        p("caller_unread_cert", PairGuard.caller(new String[] {"sa.urtv.home"}, null, own) || PairGuard.caller(new String[] {"sa.urtv.home"}, own, null));
        p("caller_none", PairGuard.caller(null, own, own) || PairGuard.caller(new String[0], own, own));
        p("cmd_known", PairGuard.allowed("pm disable-user --user 0 com.tcl.logkit"));
        p("cmd_known_other_pkg", PairGuard.allowed("pm disable-user --user 0 com.example.other"));
        p("cmd_install", PairGuard.allowed("cmd package install -r -S 4096"));
        p("cmd_unknown", PairGuard.allowed("rm -rf /sdcard"));
        p("cmd_injected", PairGuard.allowed("pm disable-user --user 0 com.tcl.logkit; rm -rf /sdcard"));
        p("cmd_subst", PairGuard.allowed("pm disable-user --user 0 $(id)"));
        p("cmd_newline", PairGuard.allowed("pm list packages\nrm -rf /sdcard"));
        p("cmd_null", PairGuard.allowed(null));
        p("shape", PairGuard.shape("pm disable-user --user 0 com.tcl.logkit") + "|" + PairGuard.shape("for p in a.b c.d e.f; do x; done"));
    }

    static void p(String n, Object v) { System.out.println(n + "\t" + v); }
}
