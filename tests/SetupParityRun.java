package sa.urtv.home.setup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * The app's ports of optimize/'s scripts, run against the very fake TV the scripts run against (tests/fixtures/setup-faketv.sh), one command per
 * call as AdbClient sends them. The files go to a folder (the app's files/setup/). Used by the parity tests: tests/setup_prep_test.py and others.
 *   java sa.urtv.home.setup.SetupParityRun <step> <faketv.sh> <folder>
 */
public class SetupParityRun {
    static final class Tv implements SetupEngine.Shell {
        final String stub;
        Tv(String stub) { this.stub = stub; }
        public AdbClient.Result run(String command) throws IOException {
            ProcessBuilder pb = new ProcessBuilder("bash", stub, "-I", "optimize/tv.py", command);
            pb.environment().put("RCMODE", "1");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            try (InputStream in = p.getInputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            }
            try { return new AdbClient.Result(b.toString("UTF-8"), p.waitFor()); } catch (InterruptedException e) { throw new IOException(e); }
        }
    }

    static final class Dir implements SetupEngine.Store {
        final File dir;
        Dir(File dir) { this.dir = dir; dir.mkdirs(); }
        public String get(String name) {
            File f = new File(dir, name);
            try { return f.isFile() ? new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8) : null; } catch (IOException e) { return null; }
        }
        public void put(String name, String text) throws IOException { Files.write(new File(dir, name).toPath(), text.getBytes(StandardCharsets.UTF_8)); }
    }

    /** The community step with the app's asset, and UAD-ng's list from the file in $UAD_FILE (none: offline). */
    static SetupCommunity community(Tv tv, Dir store, SetupEngine.Out out) throws IOException {
        String asset = new String(Files.readAllBytes(new File(System.getProperty("assets"), "setup/community.txt").toPath()), StandardCharsets.UTF_8);
        String uad = System.getenv("UAD_FILE");
        final String json = uad == null || uad.isEmpty() ? null : new String(Files.readAllBytes(new File(uad).toPath()), StandardCharsets.UTF_8);
        return new SetupCommunity(tv, store, out, asset, (url, max) -> url.equals(SetupCommunity.UAD) ? json : null);
    }

    public static void main(String[] a) throws Exception {
        String step = a[0];
        Tv tv = new Tv(a[1]);
        Dir store = new Dir(new File(a[2]));
        SetupEngine.Out out = t -> System.out.println("LINE " + t);
        SetupEngine.Result r;
        switch (step) {
            case "tweaks-apply": r = new SetupTweaks(tv, store, out).apply(); break;
            case "tweaks-restore": r = new SetupTweaks(tv, store, out).restore(); break;
            case "backup": r = new SetupPrep(tv, store, out).backup(); break;
            case "keep": r = new SetupPrep(tv, store, out).keep(); break;
            case "autostart": r = new SetupPrep(tv, store, out).autostart(); break;
            case "autostart-undo": new SetupPrep(tv, store, out).autostartUndo(); r = new SetupEngine.Result(SetupEngine.Status.DONE, ""); break;
            case "community": r = community(tv, store, out).apply(); break;
            case "answers": {
                java.util.Map<String, Boolean> ans = new java.util.LinkedHashMap<>();
                for (String kv : System.getenv("ANSWERS").split(",")) if (!kv.isEmpty()) ans.put(kv.substring(0, kv.indexOf('=')), kv.endsWith("=yes"));
                r = community(tv, store, out).answer(ans);
                break;
            }
            default: throw new IllegalArgumentException(step);
        }
        System.out.println("RESULT " + r.status);
    }
}
