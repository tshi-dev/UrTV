package sa.urtv.screensaver;

import java.util.ArrayList;
import java.util.List;

/**
 * «أزرار الريموت»: which app buttons THIS TV's remote has, and which button a key event is. Each brand sends its own numbers, so a TV
 * gets rows only for the buttons its brand's remote was read to send; a brand nobody has read gets none (no row that would do nothing).
 *
 * The brands, models and codes are Free My TV's (https://github.com/simbasjungles/free-my-tv, app/src/io/freemytv/RemoteKeys.java,
 * Prefs.java, SabrinaRemote.java at release 1.5.0, MIT licence, Copyright (c) 2026 Free My TV contributors: see
 * THIRD_PARTY_NOTICES.md), each read from a real TV's log there. Only the buttons that open an app are taken: a Settings, All Apps,
 * Guide, Mi, PatchWall or Inputs button stays with the TV and with our own home's key path (RemoteKeys, SettingsKey). The same file is
 * in the screensaver app (sa.urtv.screensaver.RemoteButtons), which draws the rows; tests/key_apps_test.py checks the two are the same.
 *
 * No Android in here (KeyEvent's numbers are written out): tests/KeyAppsRun.java runs it.
 */
final class RemoteButtons {
    private RemoteButtons() {}

    /** One app button: its id (the setting's name), its row title, and the app it opens on its own (null: none we know) */
    static final class Button {
        final String id, title, own;

        Button(String id, String title, String own) {
            this.id = id;
            this.title = title;
            this.own = own;
        }
    }

    static final String YOUTUBE_APP = "com.google.android.youtube.tv", NETFLIX_APP = "com.netflix.ninja",
            PRIME_APP = "com.amazon.amazonvideo.livingroom", DISNEY_APP = "com.disney.disneyplus";

    private static Button b(String id, String title, String own) { return new Button(id, title, own); }

    private static final Button YOUTUBE = b("youtube", "زر يوتيوب", YOUTUBE_APP), NETFLIX = b("netflix", "زر نتفليكس", NETFLIX_APP),
            PRIME = b("prime", "زر Prime Video", PRIME_APP), DISNEY = b("disney", "زر ديزني بلس", DISNEY_APP),
            PARAMOUNT = b("paramount", "زر Paramount+", null), FREE_TV = b("freetv", "زر Free TV", null),
            STAR = b("star", "الزر المخصص ★", null), CORE = b("core", "زر BRAVIA CORE", null),
            CRUNCHYROLL = b("crunchyroll", "زر Crunchyroll", "com.crunchyroll.crunchyroid"), PLAY = b("play", "زر Google Play", null),
            SONYLIV = b("sonyliv", "زر SonyLIV", "com.sonyliv"), TABII = b("tabii", "زر Tabii", null),
            TCL_CHANNEL = b("tclchannel", "زر TCL Channel", null), OKKO = b("okko", "زر Okko", null), VK = b("vk", "زر VK", null),
            RUTUBE = b("rutube", "زر Rutube", null), KION = b("kion", "زر KION", null), WINK = b("wink", "زر Wink", null),
            IVI = b("ivi", "زر IVI", null);

    // android.view.KeyEvent's values
    private static final int BUTTON_1 = 188, BUTTON_2 = 189, BUTTON_3 = 190, BUTTON_4 = 191, BUTTON_6 = 193, BUTTON_7 = 194,
            BUTTON_8 = 195, BUTTON_9 = 196, BUTTON_10 = 197, BUTTON_11 = 198, BUTTON_12 = 199, BUTTON_13 = 200, BUTTON_15 = 202,
            BUTTON_16 = 203, STEM_1 = 265, VIDEO_APP_1 = 289, VIDEO_APP_8 = 296, FEATURED_APP_1 = 297, MACRO_1 = 313;

    /** the TVs whose remotes were read (Free My TV's RemoteKeys), in its order: the first that matches wins */
    static final int NONE = 0, SABRINA = 1, GOOGLE_STREAMER = 2, ONN = 3, PHILIPS_GTV = 4, HISENSE = 5, SONY = 6, STEM_NETFLIX = 7,
            KTC = 8, XIAOMI_PATCHWALL_ONLY = 9, XIAOMI_AFKR0 = 10, XIAOMI = 11, TCL = 12, JVC_SKYWORTH = 13;

    /** Build.MANUFACTURER, BRAND, MODEL, DEVICE and VERSION.SDK_INT → one of the TVs above, or NONE */
    static int tv(String manufacturer, String brand, String model, String device, int sdk) {
        model = model == null ? "" : model;
        if ("sabrina".equals(device) && "Chromecast".equals(model)) return SABRINA;
        if (is("Google", manufacturer, brand) && model.equals("Google TV Streamer")) return GOOGLE_STREAMER;
        if (is("onn", manufacturer, brand)) return ONN;
        if (sdk == 30 && "JVC G24 TV".equals(model) && is("skyworth", manufacturer, brand)) return JVC_SKYWORTH;   // only its Inputs and Settings buttons: none here
        if ((is("Philips", manufacturer, brand) || is("TPV", manufacturer, brand) || model.startsWith("Philips")) && sdk >= 33) return PHILIPS_GTV;
        if (is("Hisense", manufacturer, brand)) return HISENSE;
        if (is("Sony", manufacturer, brand)) return SONY;
        boolean formovie = is("Formovie", manufacturer, brand);
        if (is("Vestel", manufacturer, brand) || formovie) return STEM_NETFLIX;
        if (is("KTC", manufacturer, brand)) return KTC;
        if (is("Xiaomi", manufacturer, brand) && model.equals("MiTV-AXSO2")) return XIAOMI_PATCHWALL_ONLY;
        if (is("Xiaomi", manufacturer, brand) && model.equals("MiTV-AFKR0")) return XIAOMI_AFKR0;
        if (is("Xiaomi", manufacturer, brand) && !formovie && model.startsWith("MiTV-M")) return XIAOMI;
        if (is("TCL", manufacturer, brand)) return TCL;
        return NONE;
    }

    private static boolean is(String maker, String manufacturer, String brand) {
        return maker.equalsIgnoreCase(manufacturer) || maker.equalsIgnoreCase(brand);
    }

    /** The app buttons of this TV's remote, in the order Free My TV lists them (its settings' rows); empty: no rows */
    static List<Button> buttons(int tv) {
        List<Button> out = new ArrayList<>();
        switch (tv) {
            case SABRINA: add(out, YOUTUBE); break;
            case GOOGLE_STREAMER: add(out, YOUTUBE, NETFLIX, STAR); break;
            case ONN: add(out, YOUTUBE, NETFLIX, DISNEY, PARAMOUNT, FREE_TV); break;
            case PHILIPS_GTV: add(out, NETFLIX, YOUTUBE, PRIME); break;
            case HISENSE: add(out, PRIME, YOUTUBE, DISNEY); break;
            case SONY: add(out, CORE, NETFLIX, DISNEY, PRIME, YOUTUBE, CRUNCHYROLL, PLAY, SONYLIV); break;
            case STEM_NETFLIX: add(out, NETFLIX); break;
            case KTC: add(out, TABII); break;
            case XIAOMI_AFKR0: add(out, NETFLIX, PRIME, YOUTUBE); break;
            case XIAOMI: add(out, YOUTUBE, OKKO, VK, RUTUBE, KION, WINK, IVI); break;
            case TCL: add(out, NETFLIX, YOUTUBE, PRIME, TCL_CHANNEL); break;
            default: break;   // NONE, JVC_SKYWORTH, XIAOMI_PATCHWALL_ONLY: no app button was read
        }
        return out;
    }

    private static void add(List<Button> out, Button... b) { java.util.Collections.addAll(out, b); }

    static Button byId(int tv, String id) {
        for (Button x : buttons(tv)) if (x.id.equals(id)) return x;
        return null;
    }

    /** onn's remote (Vendor_0957_Product_0031.kl) and the Chromecast's (Vendor_18d1_Product_9450.kl): only these remotes' keys are theirs */
    private static final int ONN_VENDOR = 0x0957, ONN_PRODUCT = 0x0031, SABRINA_VENDOR = 0x18d1, SABRINA_PRODUCT = 0x9450;

    /**
     * Which button a key event is on this TV, or null (any other key, or a gamepad's: BUTTON_3 is a controller's button too).
     * vendor/product: the input device's ids (-1 when unknown), read by the caller only on onn and the Chromecast.
     */
    static String idOf(int tv, int keyCode, int scanCode, boolean gamepad, int vendor, int product) {
        if (gamepad) return null;
        switch (tv) {
            case SABRINA:
                return vendor == SABRINA_VENDOR && product == SABRINA_PRODUCT && keyCode == BUTTON_3 ? "youtube" : null;
            case GOOGLE_STREAMER:
                return keyCode == BUTTON_3 ? "youtube" : keyCode == BUTTON_4 ? "netflix" : keyCode == MACRO_1 ? "star" : null;
            case ONN:
                if (vendor != ONN_VENDOR || product != ONN_PRODUCT) return null;
                return keyCode == BUTTON_3 ? "youtube" : keyCode == BUTTON_4 ? "netflix" : keyCode == BUTTON_9 ? "disney"
                        : keyCode == BUTTON_10 ? "paramount" : keyCode == FEATURED_APP_1 ? "freetv" : null;
            case PHILIPS_GTV:
                return keyCode == 475 ? "netflix" : keyCode == 781 ? "youtube" : keyCode == 788 ? "prime" : null;
            case HISENSE:
                return keyCode == 295 ? "prime" : keyCode == BUTTON_3 ? "youtube" : keyCode == 607 ? "disney" : null;
            case SONY:
                if (keyCode == 3004) return "sonyliv";
                // BUTTON_9 for five buttons, BUTTON_4 Netflix, BUTTON_8 Google Play, BUTTON_2 and _15 on the VU31, VIDEO_APP_1.._8 on the
                // BRAVIA 8 II: only the scan code tells them apart (Free My TV's Prefs.sonyKey)
                if (keyCode == BUTTON_4 || keyCode == BUTTON_8 || keyCode == BUTTON_9 || keyCode == BUTTON_2 || keyCode == BUTTON_15
                        || keyCode >= VIDEO_APP_1 && keyCode <= VIDEO_APP_8) return sonyScan(scanCode);
                return null;
            case STEM_NETFLIX:
                return keyCode == STEM_1 ? "netflix" : null;
            case KTC:
                return keyCode == BUTTON_8 ? "tabii" : null;
            case XIAOMI_AFKR0:
                return keyCode == BUTTON_6 ? "netflix" : keyCode == BUTTON_7 ? "prime" : keyCode == BUTTON_16 ? "youtube" : null;
            case XIAOMI:
                switch (keyCode) {
                    case BUTTON_3: return "youtube";
                    case BUTTON_13: return "okko";
                    case BUTTON_1: return "vk";
                    case BUTTON_16: return "rutube";
                    case BUTTON_11: return "kion";
                    case BUTTON_10: return "wink";
                    case BUTTON_12: return "ivi";
                    default: return null;
                }
            case TCL:
                // some TCL remotes (BT_RC833) send TCL's own 4065 for the same YouTube button
                return keyCode == 4062 ? "netflix" : keyCode == BUTTON_3 || keyCode == 4065 ? "youtube" : keyCode == 4099 ? "prime"
                        : keyCode == 4020 ? "tclchannel" : null;
            default:
                return null;
        }
    }

    private static String sonyScan(int scan) {
        switch (scan) {
            case 496: return "core";
            case 583: case 758: return "netflix";
            case 491: return "disney";
            case 490: case 652: return "prime";
            case 485: case 653: return "youtube";
            case 507: return "crunchyroll";
            case 587: return "play";
            default: return null;
        }
    }

    /** Xiaomi's MiTV-M… remotes send one press as a burst of 5 to 8: a press of the same button this soon after is the same press */
    static final long XIAOMI_BURST_MS = 1000;

    /**
     * TCL's Android 11 changes the volume twice for one press while any key filter is on (Free My TV #325): there the filter outside our
     * home is never asked for these buttons (TypeService.filterKeys).
     */
    static boolean filterSafe(int tv, int sdk) {
        return !(tv == TCL && sdk == 30);
    }
}
