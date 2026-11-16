package sa.urtv.home;

/**
 * 4.3.3 «إضافات تلفزيونك» (UrTV Extras, the add-on sa.urtv.extras): the names both apps use to talk to each other, in one place (this file is compiled
 * into both, android/extras/build.gradle). The home calls the add-on's provider ({@link #EXTRAS_AUTHORITY}: what the service does: type, press a key,
 * turn the screen off, Home), the add-on calls the home's ({@link #HOME_AUTHORITY}: what only the home knows: the family lock, its own key path). Both
 * providers ask for {@link #PERMISSION}, a signature permission: only an app signed with our key can call either. No Android in here.
 */
public final class ExtrasWords {
    private ExtrasWords() { }

    public static final String PACKAGE = "sa.urtv.extras";
    /** the add-on's accessibility service, as the TV's list (enabled_accessibility_services) writes it */
    public static final String SERVICE = "sa.urtv.extras/sa.urtv.home.ExtrasA11yService";
    public static final String EXTRAS_AUTHORITY = "sa.urtv.extras.bridge", HOME_AUTHORITY = "sa.urtv.home.extras";
    public static final String PERMISSION = "sa.urtv.permission.EXTRAS";
    /** the release file (the same release as the home and the screensaver) */
    public static final String URL = "https://github.com/tshi-dev/UrTV/releases/download/apk-latest/urtv-extras.apk";

    // the add-on's calls (the home asks)
    public static final String STATUS = "status", TYPE = "type", PRESS = "press", SLEEP = "sleep", GO_HOME = "go_home", WATCH = "watch";
    /** «أزرار الريموت»: both ways. The home tells the add-on the owner's choices when they change ("youtube=PKG,netflix=,…", KeyApps.line); the add-on asks the home for them when it starts */
    public static final String KEY_APPS = "key_apps";
    /** 4.4: WATCH's other two arguments: the person's chosen home is ours (the setup confirmed it) or not any more («تراجع»); WANTED is also the bundle's key */
    public static final String WANTED = "wanted", UNWANTED = "unwanted";
    // the home's calls (the add-on asks)
    public static final String TYPE_CHECK = "type_check", SETTINGS_PIN = "settings_pin", KEY = "key", LEDGER = "ledger";
    /** «أزرار الريموت» over another app: the family lock's answer for the app a button opens (NEEDS_PIN, HIDDEN) */
    public static final String KEY_APP_CHECK = "key_app_check";
    /** the add-on opens the home with this extra (the package) when that app is behind the PIN: the home asks it, then opens the app */
    public static final String EXTRA_KEY_APP = "sa.urtv.home.KEY_APP";
    /** 4.4 «إنقاذ تلفزيونك» (UrTV Rescue): its package and release file */
    public static final String RESCUE_PACKAGE = "sa.urtv.rescue", RESCUE_URL = "https://github.com/tshi-dev/UrTV/releases/download/apk-latest/urtv-rescue.apk";
    // the bundle's keys
    public static final String RESULT = "result", ON = "on", APPEND = "append", SEARCH = "search", NEEDS_PIN = "needs_pin", HIDDEN = "hidden", SYSTEM_SCREEN = "system_screen",
        CODE = "code", ACTION = "action", REPEAT = "repeat", DOWN_TIME = "down_time", EVENT_TIME = "event_time", SCAN = "scan", META = "meta", SOURCE = "source", DEVICE = "device";

    /** the words typing answers with (the phone page knows them): the same as before 4.3.3, when the home held the service */
    public static final String TYPED = "typed_other", TYPED_NO_SEARCH = "typed_other_nosearch", OFF = "service_off", LOCKED = "locked", REFUSED = "refused",
        NO_FIELD = "no_field", PASSWORD = "password_field", FAILED = "failed", SEARCHED = "searched";
}
