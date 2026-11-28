package sa.urtv.home;

/**
 * 4.4 (R-408): «فلتر أزرار الريموت». Android gives a key to every accessibility service that filters keys, one after the other, and a key one of them keeps
 * never reaches the next: a key remapper the person installed (Button Mapper and the like) lost the Home, Settings and app buttons our add-on keeps. With
 * the switch off the add-on asks for no key at all, whatever else wanted it (the Home catch, the Settings key, «أزرار الريموت», R-404..R-407); on is the
 * default and changes nothing. The same file is in the add-on (android/extras/build.gradle). No Android in here (tests/KeyFilterSwitchRun.java).
 */
final class KeyFilterSwitch {
    private KeyFilterSwitch() { }

    /** the filter is asked for: the switch is on and something of ours wants it */
    static boolean want(boolean switchOn, boolean wanted) { return switchOn && wanted; }
}
