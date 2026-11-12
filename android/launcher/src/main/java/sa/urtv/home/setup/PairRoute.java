package sa.urtv.home.setup;

import android.content.Context;

import java.io.IOException;

import sa.urtv.home.setup.pair.PairClient;

/**
 * The ONE place the setup package touches wireless-debugging pairing (the package {@code setup.pair}, libadb-android): FactoryActivity asks {@link #available}, shows the pairing page,
 * calls {@link #pair}, and every other place that needs the TV's adb goes through SetupSession.openAny, which calls {@link #open} when the loopback did not answer and a pairing was done.
 * To take pairing out: delete this file, the package, the gradle lines and these calls.
 */
final class PairRoute {
    private PairRoute() { }

    static boolean available() { return PairClient.available(); }

    static boolean paired(Context c) { return PairClient.paired(c); }

    static void forget(Context c) { PairClient.forget(c); }

    private static PairClient.Log log() {
        return new PairClient.Log() { @Override public void line(String text) { android.util.Log.i("UrTVFactory", text); } };   // the code is never in a line
    }

    /** pairs with the six digits (port 0: found by name) */
    static void pair(Context c, String digits, int port) throws IOException { PairClient.pair(c, digits, port, log()); }

    /** a session over the paired connection */
    static SetupSession open(Context c, SetupEngine.Out out) throws IOException {
        final PairClient.Connection conn = PairClient.connect(c, log());
        SetupEngine.Shell shell = new SetupEngine.Shell() {
            @Override public AdbClient.Result run(String command) throws IOException { return AdbClient.withCode(conn.run(command)); }
        };
        return SetupSession.wrap(c, shell, conn, out);
    }
}
