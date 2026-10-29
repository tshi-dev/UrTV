package sa.urtv.home;

/**
 * What Back does on the home screen. As the default home it has nowhere to go back to (Back only returns from the grid to the dock). Opened from
 * its tile while the TV keeps its own home (setupwraith / launcherx), Back at the top leaves it, to the home that opened it. No Android in here:
 * tests/HomeBackRun.java.
 */
final class HomeBack {
    private HomeBack() { }

    /** Does Back finish the home screen? Never while it is the default home, and never while the grid is scrolled (Back goes to the dock first). */
    static boolean leaves(boolean defaultHome, boolean gridScrolled) {
        return !defaultHome && !gridScrolled;
    }

    /** Is this app the one Home resolves to? {@code resolvedPackage} is the package of the activity the HOME intent resolves to (null: none, or a chooser). */
    static boolean isDefaultHome(String resolvedPackage, String ownPackage) {
        return resolvedPackage != null && resolvedPackage.equals(ownPackage);
    }
}
