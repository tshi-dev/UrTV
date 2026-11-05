package sa.urtv.home.setup;

/**
 * «إعداد تلفزيوني» left behind: after the home switch the stock home came in front and then ours, and the family never saw the run's progress or the
 * done screen (step 27). Our home asks this when it comes in front; true once when the runner says the screen should be opened again (the switch is
 * done, or a full run ended) and it has not been shown since. The home may start the screen (it has a window in front); the run's service may not.
 */
public final class SetupReopen {
    private SetupReopen() { }

    public static boolean take() { return SetupRunner.get().takeReopen(); }
}
