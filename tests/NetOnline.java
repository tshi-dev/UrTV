package sa.urtv.screensaver;

/** For tests/net_test.py: Net over real https (the TLS path no scripted server here can show). Exit 0 ok, 1 a real failure (TLS, host name, status), 2 no way out from this machine (skipped). */
public final class NetOnline {
    public static void main(String[] a) {
        try {
            Net.Reply r = Net.get("https://raw.githubusercontent.com/tshi-dev/UrTV/master/VERSION", null, 10000, 15000, 4096);
            boolean ok = r.status == 200 && r.body.length > 0 && r.body.length < 100;
            System.out.println((ok ? "PASS " : "FAIL ") + "https: raw.githubusercontent.com answered " + r.status + " (" + r.body.length + " bytes) through Net, certificate checked against the host name");
            // the certificate is checked against the host name: the same server asked by its numeric address has no certificate for that name, so it must be refused
            String ip = java.net.InetAddress.getByName("raw.githubusercontent.com").getHostAddress();
            boolean refused = false;
            try { Net.get("https://" + ip + "/tshi-dev/UrTV/master/VERSION", null, 10000, 15000, 4096); } catch (javax.net.ssl.SSLException e) { refused = true; }
            System.out.println((refused ? "PASS " : "FAIL ") + "https: a certificate that is not for the host asked is refused (the numeric address of the same server)");
            System.exit(ok && refused ? 0 : 1);
        } catch (java.net.UnknownHostException | java.net.ConnectException | java.net.SocketTimeoutException | java.net.NoRouteToHostException e) {
            System.out.println("SKIP https: no way out from this machine (" + e.getClass().getSimpleName() + ")");
            System.exit(2);
        } catch (java.io.IOException e) {
            if (String.valueOf(e.getMessage()).contains("no address") || e.getMessage() == null) { System.out.println("SKIP https: " + e); System.exit(2); }
            System.out.println("FAIL https: " + e);
            System.exit(1);
        }
    }
}
