package sa.urtv.home.setup;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * The TV's own address on the local network (a private IPv4 on an interface that is up, not the loopback), found from the network interfaces: adbd sees a client that comes in by
 * it as a NETWORK client, which on some TVs is the one that gets «السماح بتصحيح USB؟» when a loopback client does not. Never logged or shown (docs: no address in logs).
 */
final class LanAddress {
    private LanAddress() { }

    /** a private IPv4 address as text, or null; wlan and eth first */
    static String find() {
        String other = null;
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis != null && nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (!(a instanceof Inet4Address) || !a.isSiteLocalAddress()) continue;
                    String name = ni.getName();
                    if (name.startsWith("wlan") || name.startsWith("eth")) return a.getHostAddress();
                    if (other == null) other = a.getHostAddress();
                }
            }
        } catch (java.net.SocketException e) {
            return null;
        }
        return other;
    }
}
