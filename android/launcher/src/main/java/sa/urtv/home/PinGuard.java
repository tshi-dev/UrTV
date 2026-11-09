package sa.urtv.home;

import android.app.Activity;

/** The family PIN for the screens of other packages of this app (the setup's FactoryActivity): asked whenever one is set; a wrong or cancelled PIN runs {@code cancelled} and changes nothing. */
public final class PinGuard {
    private PinGuard() { }

    public static void confirm(Activity a, String name, Runnable ok, Runnable cancelled) {
        if (!Parental.state(a).pinSet) { ok.run(); return; }
        Parental.confirm(a, Parental.adhanFirst(a), S.tv(name), ok, cancelled);
    }
}
