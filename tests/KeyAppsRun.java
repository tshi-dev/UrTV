package sa.urtv.home;

/** For tests/key_apps_test.py: RemoteButtons' table and KeyApps' rules, one "name<TAB>got" line per check. */
public final class KeyAppsRun {
    private static final StringBuilder out = new StringBuilder();

    private static void say(String name, Object got) { out.append(name).append('\t').append(got).append('\n'); }

    private static String ids(int tv) {
        StringBuilder b = new StringBuilder();
        for (RemoteButtons.Button x : RemoteButtons.buttons(tv)) b.append(b.length() > 0 ? "," : "").append(x.id);
        return b.toString();
    }

    public static void main(String[] args) {
        int tcl = RemoteButtons.tv("TCL", "TCL", "Smart TV Pro", "G10", 31);
        say("tv TCL", tcl);
        say("tv unknown", RemoteButtons.tv("Samsung", "samsung", "X", "x", 31));
        say("tv Sony", RemoteButtons.tv("Sony", "Sony", "BRAVIA 4K VH2", "BRAVIA_VH2", 31));
        say("tv Philips old", RemoteButtons.tv("Philips", "Philips", "50PUS", "x", 30));
        say("tv Philips GTV", RemoteButtons.tv("TPV", "Philips", "Philips Google TV TA5", "x", 34));
        say("tv Xiaomi MiTV-M", RemoteButtons.tv("Xiaomi", "Xiaomi", "MiTV-MSSP3", "x", 28));
        say("tv Mi Box", RemoteButtons.tv("Xiaomi", "Xiaomi", "MIBOX4", "x", 28));
        say("tv Formovie", RemoteButtons.tv("Formovie", "Xiaomi", "MiTV-MOOR0", "x", 34));
        say("tv Chromecast", RemoteButtons.tv("Google", "google", "Chromecast", "sabrina", 34));
        say("rows TCL", ids(tcl));
        say("rows unknown", ids(RemoteButtons.NONE));
        say("rows JVC", ids(RemoteButtons.JVC_SKYWORTH));
        say("rows Sony", ids(RemoteButtons.SONY));
        say("TCL 190", RemoteButtons.idOf(tcl, 190, 0, false, -1, -1));
        say("TCL 4065", RemoteButtons.idOf(tcl, 4065, 0, false, -1, -1));
        say("TCL 4062", RemoteButtons.idOf(tcl, 4062, 0, false, -1, -1));
        say("TCL 4020", RemoteButtons.idOf(tcl, 4020, 0, false, -1, -1));
        say("TCL gamepad 190", RemoteButtons.idOf(tcl, 190, 0, true, -1, -1));
        say("TCL arrow", RemoteButtons.idOf(tcl, 21, 0, false, -1, -1));
        say("Sony 196 scan 496", RemoteButtons.idOf(RemoteButtons.SONY, 196, 496, false, -1, -1));
        say("Sony 191 scan 758", RemoteButtons.idOf(RemoteButtons.SONY, 191, 758, false, -1, -1));
        say("Sony VIDEO_APP scan 485", RemoteButtons.idOf(RemoteButtons.SONY, 290, 485, false, -1, -1));
        say("Sony 3004", RemoteButtons.idOf(RemoteButtons.SONY, 3004, 0, false, -1, -1));
        say("Sony 196 unknown scan", RemoteButtons.idOf(RemoteButtons.SONY, 196, 1, false, -1, -1));
        say("onn other remote", RemoteButtons.idOf(RemoteButtons.ONN, 190, 0, false, 1, 2));
        say("onn its remote", RemoteButtons.idOf(RemoteButtons.ONN, 297, 0, false, 0x0957, 0x0031));
        say("Hisense 295", RemoteButtons.idOf(RemoteButtons.HISENSE, 295, 0, false, -1, -1));
        say("filter TCL 11", RemoteButtons.filterSafe(tcl, 30));
        say("filter TCL 12", RemoteButtons.filterSafe(tcl, 31));
        say("filter Xiaomi AFKR0", RemoteButtons.filterSafe(RemoteButtons.XIAOMI_AFKR0, 30));
        say("filter Xiaomi MiTV-M", RemoteButtons.filterSafe(RemoteButtons.XIAOMI, 28));
        say("filter Xiaomi PatchWall", RemoteButtons.filterSafe(RemoteButtons.XIAOMI_PATCHWALL_ONLY, 30));
        say("filter Sony", RemoteButtons.filterSafe(RemoteButtons.SONY, 30));
        say("valid pkg", KeyApps.validChoice("net.mbc.shahidTV"));
        say("valid shell", KeyApps.validChoice("a.b; reboot"));

        KeyApps.Outside o = new KeyApps.Outside();
        String st = "org.smarttube.stable";
        say("outside default passes", o.decide("youtube", 190, 0, 0, 0, false, "", 0));
        say("outside home passes", o.decide("youtube", 190, 0, 0, 0, true, st, 0));
        say("outside other key passes", o.decide(null, 21, 0, 0, 0, false, st, 0));
        say("outside chosen opens", o.decide("youtube", 190, 0, 0, 0, false, st, 0) + ":" + o.pkg);
        say("outside repeat eaten", o.decide("youtube", 190, 0, 1, 10, false, st, 0));
        say("outside release eaten", o.decide("youtube", 190, 1, 0, 20, false, st, 0));
        say("outside next press opens", o.decide("youtube", 190, 0, 0, 30, false, st, 0));
        KeyApps.Outside x = new KeyApps.Outside();
        say("xiaomi first opens", x.decide("youtube", 190, 0, 0, 5000, false, st, 1000));
        x.decide("youtube", 190, 1, 0, 5010, false, st, 1000);
        say("xiaomi burst eaten", x.decide("youtube", 190, 0, 0, 5200, false, st, 1000));
        x.decide("youtube", 190, 1, 0, 5210, false, st, 1000);
        say("xiaomi later opens", x.decide("youtube", 190, 0, 0, 7000, false, st, 1000));
        // 4.4 (the port to «إضافات تلفزيونك»): the choices' line, the filter's rule, the rest of a Xiaomi burst our home answered
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
        m.put("youtube", st); m.put("netflix", ""); m.put("prime", "a.b; reboot");
        say("line", KeyApps.line(m));
        say("parse", KeyApps.parse("youtube=org.smarttube.stable,netflix=,BAD=x.y,prime=a b,=c.d").toString());
        say("parse null", KeyApps.parse(null).toString());
        say("any chosen", KeyApps.anyChosen(KeyApps.parse("youtube=,netflix=com.x.y")));
        say("none chosen", KeyApps.anyChosen(KeyApps.parse("youtube=,netflix=")));
        say("filter outside chosen", KeyApps.filterWanted(false, true, true));
        say("filter home front", KeyApps.filterWanted(true, true, true));
        say("filter all default", KeyApps.filterWanted(false, false, true));
        say("filter wanted TCL 11", KeyApps.filterWanted(false, true, false));
        KeyApps.Outside q = new KeyApps.Outside();
        q.leftHome(9000, 1000);
        say("after home burst eaten", q.decide("youtube", 190, 0, 0, 9300, false, st, 1000));
        say("after home release eaten", q.decide("youtube", 190, 1, 0, 9310, false, st, 1000));
        say("after home later opens", q.decide("youtube", 190, 0, 0, 10500, false, st, 1000));
        KeyApps.Outside r = new KeyApps.Outside();
        r.leftHome(9000, 0);
        say("no burst window opens", r.decide("youtube", 190, 0, 0, 9300, false, st, 0));
        say("holding after open", r.holding(190));
        r.decide("youtube", 190, 1, 0, 9400, false, st, 0);
        say("not holding after release", r.holding(190));
        // beta 3 (TCL reports): the YouTube button is a global key; on its default it opens its own app from the add-on while our home is the home
        say("global TCL youtube ours", KeyApps.globalOwn(RemoteButtons.TCL, "youtube", true));
        say("global TCL youtube not ours", KeyApps.globalOwn(RemoteButtons.TCL, "youtube", false));
        say("global TCL netflix", KeyApps.globalOwn(RemoteButtons.TCL, "netflix", true));
        say("global TCL prime", KeyApps.globalOwn(RemoteButtons.TCL, "prime", true));   // R-406
        say("global TCL channel", KeyApps.globalOwn(RemoteButtons.TCL, "tclchannel", true));
        say("global Sony youtube", KeyApps.globalOwn(RemoteButtons.SONY, "youtube", true));
        say("global wanted TCL", KeyApps.globalWanted(RemoteButtons.TCL, true, true));
        say("global wanted TCL 11", KeyApps.globalWanted(RemoteButtons.TCL, true, false));
        say("global wanted not ours", KeyApps.globalWanted(RemoteButtons.TCL, false, true));
        KeyApps.Outside g = new KeyApps.Outside();
        say("global over our home opens", g.decide("youtube", 190, 0, 0, 20000, false, KeyApps.globalOwn(RemoteButtons.TCL, "youtube", true), 0) + ":" + g.pkg);
        System.out.print(out);
    }
}
