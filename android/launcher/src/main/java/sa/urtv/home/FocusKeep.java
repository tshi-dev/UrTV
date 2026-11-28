package sa.urtv.home;

/**
 * 4.4 (a report: «the focus jumps and another app opens»): when the favourites and the grid are built again (a package update, the favourites' retry
 * after a start, a look switch), the focus goes back to the same app, by its package. The tiles get new view ids at each build, so a restore by the
 * old id always missed and the focus fell to the first favourite: the person's next OK opened another app. Plain Java (tests/FocusKeepRun.java).
 */
final class FocusKeep {
    private FocusKeep() { }

    /**
     * The package to give the focus back to after the rebuild: one already asked for (after an edit or «إضافة إلى المفضلة») wins; else the app that has the
     * focus now, but only once the person has moved since Home (Home puts the focus on the first app on purpose); null: the first favourite as before.
     */
    static String keep(boolean keyedSinceHome, String focusedPkg, String asked) {
        if (asked != null) return asked;
        return keyedSinceHome ? focusedPkg : null;
    }
}
