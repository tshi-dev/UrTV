package sa.urtv.home;

/**
 * 4.3.2: the screensaver's settings fragment (the city, the cards, the clock…) kept on disk, so a home that starts while the screensaver app cannot answer
 * (an update in progress, a TV that will not start it from the background) shows the LAST city and cards instead of the page's defaults: a user's adhan
 * times and prayer card went missing after an update that way. What is kept is only ever a real answer; what comes back is shown, never taken for a fresh
 * answer (the caller marks it old, so it cannot become the remembered place or move the adhan alarm). No Android in here (tests/FragmentKeepRun.java).
 */
final class FragmentKeep {
    private FragmentKeep() { }

    /** a real answer is written only when it differs from what is kept (the provider is read every few seconds) */
    static boolean shouldSave(String fresh, String kept) { return fresh != null && !fresh.isEmpty() && !fresh.equals(kept); }

    /** the fragment to show: the fresh answer; none: the kept one; none of either: null (the defaults, as before) */
    static String choose(String fresh, String kept) { return fresh != null ? fresh : (kept == null || kept.isEmpty() ? null : kept); }

    /** is it an old copy (the kept fragment was used, not an answer)? */
    static boolean usedKept(String fresh, String kept) { return fresh == null && kept != null && !kept.isEmpty(); }
}
