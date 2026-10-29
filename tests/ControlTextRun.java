package sa.urtv.home;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * For tests/control_center_test.py: ControlText.java and ControlLayout.java on tests/control_center_cases.tsv. A line is
 *   mbps       VALUE                       -> the speed test's answer
 *   signal     RSSI                        -> the bars (0..4) of a dBm
 *   network    wired(0|1) wifi(0|1) SSID RSSI -> the network tile's two lines, joined with «|»
 *   output     TYPE,TYPE,... [NAME|NAME|...] -> the sound output tile's line (the names are the products the devices report, one per type)
 *   layout     W H DENSITY N               -> "cols rows tileW tileH panelW panelH compact tiny" of the panel's tiles on that screen
 *   mirror     PKG,PKG,...                 -> the mirroring app «انعكاس الشاشة» opens when the TV can open those packages, or "none"
 */
public final class ControlTextRun {
    public static void main(String[] args) throws IOException {
        StringBuilder out = new StringBuilder();
        for (String line : Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] f = line.split("\t", -1);
            switch (f[0]) {
                case "mbps": out.append(ControlText.mbps(Double.parseDouble(f[1]))); break;
                case "signal": out.append(ControlText.signalLevel(Integer.parseInt(f[1]))); break;
                case "network": {
                    boolean wired = "1".equals(f[1]), wifi = "1".equals(f[2]);
                    out.append(ControlText.network(wired, wifi, f[3].equals("null") ? null : f[3])).append('|')
                            .append(ControlText.networkDetail(wired, wifi, Integer.parseInt(f[4])));
                    break;
                }
                case "output": {
                    String[] t = f[1].isEmpty() ? new String[0] : f[1].split(",");
                    int[] types = new int[t.length];
                    for (int i = 0; i < t.length; i++) types[i] = Integer.parseInt(t[i]);
                    String[] names = f[2].isEmpty() ? null : f[2].split("\\|", -1);
                    out.append(ControlText.outputs(types, names));
                    break;
                }
                case "mirror": {
                    final java.util.Set<String> has = new java.util.HashSet<>(java.util.Arrays.asList(f[1].split(",")));
                    String app = ControlText.mirrorApp(has::contains);
                    out.append(app == null ? "none" : app);
                    break;
                }
                default: {   // layout
                    ControlLayout.Plan p = ControlLayout.plan(Integer.parseInt(f[1]), Integer.parseInt(f[2]), Float.parseFloat(f[3]), Integer.parseInt(f[4]));
                    out.append(p.cols).append(' ').append(p.rows).append(' ').append(p.tileW).append(' ').append(p.tileH).append(' ')
                            .append(p.panelW).append(' ').append(p.panelH).append(' ').append(p.compact).append(' ').append(p.tiny);
                }
            }
            out.append('\n');
        }
        System.out.write(out.toString().getBytes(StandardCharsets.UTF_8));
        System.out.flush();
    }
}
