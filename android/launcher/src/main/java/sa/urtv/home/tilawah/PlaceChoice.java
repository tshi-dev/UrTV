package sa.urtv.home.tilawah;

/**
 * Which settings fragment the prayer place comes from: the one just read from the screensaver, else the last good one this app kept, else none
 * (then the bundled place, only on an install that has never read anything). A read can fail for a while without the screensaver being gone:
 * the TV's memory manager kills its process and the TV (TCL's app-start guard) refuses to start it again from the background. A failed
 * read must never move the place, or the adhan alarm would be set for the bundled one (Riyadh). No Android in here: tests/PlaceChoiceRun.java.
 */
final class PlaceChoice {
    private PlaceChoice() { }

    /** The fragment to use, and whether it is the remembered one (a read failed). fragment is null when there is neither. */
    static final class Pick {
        final String fragment;
        final boolean remembered;
        Pick(String fragment, boolean remembered) { this.fragment = fragment; this.remembered = remembered; }
    }

    static Pick pick(String fresh, String remembered) {
        if (PlaceFragment.parse(fresh) != null) return new Pick(fresh, false);
        if (PlaceFragment.parse(remembered) != null) return new Pick(remembered, true);
        return new Pick(null, false);
    }
}
