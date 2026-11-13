package sa.urtv.home;

/** For tests/key_apps_test.py: KeyApps' rules, one "name<TAB>got" line per check. */
public final class KeyAppsRun {
    private static final StringBuilder out = new StringBuilder();

    private static void say(String name, Object got) { out.append(name).append('\t').append(got).append('\n'); }

    public static void main(String[] args) {
        say("keyOf 190", KeyApps.keyOf(190));
        say("keyOf 4065", KeyApps.keyOf(4065));
        say("keyOf 191", KeyApps.keyOf(191));
        say("keyOf 4062", KeyApps.keyOf(4062));
        say("keyOf 4099", KeyApps.keyOf(4099));
        say("keyOf 21", KeyApps.keyOf(21));
        say("target default", KeyApps.target(KeyApps.YOUTUBE, ""));
        say("target chosen", KeyApps.target(KeyApps.YOUTUBE, "org.smarttube.stable"));
        say("target bad", KeyApps.target(KeyApps.NETFLIX, "rm -rf"));
        say("valid empty", KeyApps.validChoice(""));
        say("valid pkg", KeyApps.validChoice("net.mbc.shahidTV"));
        say("valid one word", KeyApps.validChoice("shahid"));
        say("valid shell", KeyApps.validChoice("a.b; reboot"));

        KeyApps.Outside o = new KeyApps.Outside();
        say("outside default passes", o.decide(4065, 0, 0, false, ""));
        say("outside home passes", o.decide(4065, 0, 0, true, "org.smarttube.stable"));
        say("outside other key passes", o.decide(21, 0, 0, false, "org.smarttube.stable"));
        say("outside chosen opens", o.decide(4065, 0, 0, false, "org.smarttube.stable") + ":" + o.pkg);
        say("outside repeat eaten", o.decide(4065, 0, 1, false, "org.smarttube.stable"));
        say("outside release eaten", o.decide(4065, 1, 0, false, "org.smarttube.stable"));
        say("outside next press opens", o.decide(4065, 0, 0, false, "org.smarttube.stable"));
        say("outside lost release, new press opens", o.decide(4065, 0, 0, false, "org.smarttube.stable"));
        System.out.print(out);
    }
}
