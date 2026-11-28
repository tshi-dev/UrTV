package sa.urtv.home;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import sa.urtv.home.tilawah.NowPlaying;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Apple TV style home screen: clock top-left, pills top-right, the focused dock app's name over an
 * ambient glow, a dock of pinned apps, and every other app in a grid that scrolls up on Down.
 * Plain framework views only (no AndroidX). Network only for hosted wallpapers and the optional card (HomeCard).
 *
 * Adapted from github.com/mihirgupta0900/tv-home (MIT, see android/launcher/LICENSE-tv-home):
 * Arabic text and digits, Hijri date, Thmanyah fonts, the screensaver's gold palette and Saudi photos,
 * clock on the right, and a pill that starts the screensaver (a long OK opens its settings).
 */
public class MainActivity extends Activity {
    /** «اللغة» (docs/ENGLISH.md): with Arabic the context itself, with English an English one */
    @Override protected void attachBaseContext(android.content.Context b) { super.attachBaseContext(S.wrap(b)); }


    // Dock, hidden apps, Live TV and grid width come from res/values/config.xml.
    private String[] dockApps;
    private String liveTvPkg, inputPickerPkg;
    private int cols;

    static final class App {
        String pkg, label, category;   // category: the one it was found with, LEANBACK_LAUNCHER or LAUNCHER
        ComponentName component;
        Drawable art;
        Drawable icon;   // 4.4 «شكل التطبيقات: أيقونات»: the launcher icon, read only for that way (art is the banner when there is one)
        boolean isBanner;
        /** 4.4 beta 2: no banner, so its art is its icon composed on a neutral tile (TileArt), drawn full-bleed like a banner */
        boolean composed;
        boolean iconComposed;   // «أيقونات»: icon is its square composed by TileArt (full-bleed)
        int color;
    }

    private final Map<String, Integer> colorCache = new java.util.concurrent.ConcurrentHashMap<>();   // filled off the main thread
    // Banners and icons come from the apps at their own size (often 640x360 or more); the tiles are ~1/5 of the
    // screen wide, so they are kept at that size, and the cache holds a few MB at most (sized in KB).
    private final LruCache<String, Drawable> artCache = new LruCache<String, Drawable>(6 * 1024) {
        @Override protected int sizeOf(String k, Drawable d) {
            return d instanceof BitmapDrawable ? ((BitmapDrawable) d).getBitmap().getByteCount() / 1024 : 256;
        }
    };
    private final List<View> dockCards = new ArrayList<>();
    private final List<View> gridCards = new ArrayList<>();
    private final List<View> pills = new ArrayList<>();

    private HomeScrollView scroll;
    private LinearLayout content;
    private DockGrid dockRow;   // 4.5: a LinearLayout that also draws «زجاجي»'s glass and grid
    private FrameLayout hero;
    private LinearLayout grid;
    private View glow;   // null while «توهّج لون التطبيق» is off (nothing allocated, nothing animated)
    private int glowSlot;
    private FrameLayout glowRoot;
    private float glowShare = 0f;
    private GradientDrawable glowDrawable;   // the one gradient, its colours changed in place (no allocation per move)
    private float glowAlphaTo = -1f, shadeTo = 0f;
    private ImageView wallBack, wallFront;
    private boolean scenesOn;   // 4.3.3: the scene home is showing: the ordinary photos are not decoded (their memory is the scene's)
    private SceneHome sceneHome;   // 4.4 «الآفاق»: the scene backdrop with the glass clock behind its scenery; made only while chosen (null otherwise: nothing of it exists)
    private PhotoGlass photoGlass;   // 4.4 «الساعة: زجاجية» over the home's own photos: made only while chosen (null otherwise: no cut-out is read or downloaded)
    private String wallShownKey;   // the home's key of the photo on the screen (pack:, photo:, bundled:, own:), for its cut-out
    private FrameLayout sceneBackdrop;   // where it goes when chosen: over the photos, under the dimming and the scrims
    private View gridShade;
    private String[] wallpapers = new String[0];
    private int wallIndex = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable rotateWallpaper = this::nextWallpaper;
    static final long WALLPAPER_INTERVAL_MS = 10 * 60 * 1000;
    private LinearLayout prayerCard;   // «UrTV النظيف»: today's five in one strip, built on first use
    private TextView greeting, weatherLine, clock, ampm, date, zoneLine, title, appsLabel, prayerLine;   // prayerLine: «UrTV النظيف»'s next-prayer line under the date
    private String langBuilt;   // S.lang() when this screen was built
    // «المظهر» (Look): the parts a light look recolours; built in the dark look's colours, so dark is exactly as it was
    private View scrim;
    private GradientDrawable dockBand;
    private View wallDimView;
    private PillBar pillBar;
    private boolean barHeld;   // the debug picture of the open row (extra bar=open): it stays open
    private final List<TextView> gridLabels = new ArrayList<>();
    private String lookNow = GlassStyle.LOOKS[0];   // «المظهر ← الشكل» as the minute tick last read it (the glass of the home's own pieces)
    private boolean lightNow;   // the look on screen now: dark until a light one is asked for
    private Pill inputsPill;        // «المصادر», for the phone remote's request
    private Pill tilawahPill;       // «تلاوة وأذان»: in the row only while the setting shows it (applyListenPill)
    private Pill tasksPill;         // «المهام»: in the row only while the screensaver app is here and the setting shows it (applyTasksPill)
    private LinearLayout pillRow;    // the top row of pills
    // [updates bell] «جرس التحديثات»: in the row only on evidence of waiting Play Store updates (applyUpdateBell)
    private Pill bellPill;
    private final UpdatesBell updatesBell = new UpdatesBell(new PlayStoreUpdates());
    private boolean bellForce = true;   // the home came to the front: ask again
    // [/updates bell]
    private Greeting greetingLine;
    private int glowColor = 0xFF3A3A3A;
    private int lastFocusedId = View.NO_ID;
    private boolean firstShow = true;
    private int packageSeq;
    /** the settings the clock last drew with (null before the first) */
    private ConfigSnapshot lastCfg;
    private PopupWindow inputsPopup;
    private HomeCard homeCard;
    private SideCards sideCards;   // 4.4 beta 4: «كنبة»'s small cards down the left (off by default: #sidecards=, the owner's pick)
    private NowCard nowCard;   // «يُتلى الآن»: at the right end of the pills row while a surah or a station is on
    private long nowPausedSince;   // 4.4 beta 4: when the card was first seen paused (NowCardRule), 0 while it plays
    private Pill searchPill;   // 4.3.3 «بحث»: the TV's own search inside the apps (GlobalSearch), off until the settings turn it on
    private Pill marketsPill, matchesPill, phonePill, updatePill;   // «تحديث»: an update is worth opening the update screen for (UpdateNotice)   // «الجوال» (C14): the phone remote's page
    private ImageView netIcon;   // C15: the connection in the top row (Wi-Fi, none)   // «الأسواق» and «المباريات»: they take the look like the others («رملي»: they stayed grey, step 28)
    private Pill kidsExitPill;   // 4.4 beta 3 (a user: kids mode had no way out he could see): in the row in centerPill's place while kids mode is on
    private Pill centerPill;   // «مركز التحكم»: gold while the sleep timer runs; in the row except in kids mode (applyCenterPill)
    private ControlCenter control;   // the panel it opens
    private TextView adhanBar;
    /** 4.4: the card while a dua or reminder plays: its name and «إيقاف» (DuaCard) */
    private DuaCard duaCard;   // «أذان المغرب بعد ٨ دقائق», over the top edge; while the adhan plays «أذان المغرب · أي زر يوقف الأذان»
    private final AdhanKeys adhanKeys = new AdhanKeys();
    private ReminderCard reminder;   // the card of a task with a time, over the home; the alarm (ReminderReceiver) and a return to the home bring it
    /** 4.4 a followed club's kick-off: the notice on the home's page; with no page (no card), a text notice */
    private final BroadcastReceiver kickoffDue = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (!resumed) return;
            String line = i.getStringExtra("line");
            // 4.4 the home draws the notice itself, at the screen's top right in its own window (the card's page is only the card's size)
            java.util.List<sa.urtv.home.sports.KickoffPlan.Game> g = sa.urtv.home.sports.KickoffPlan.parse("[" + i.getStringExtra("game") + "]");
            if ((g.isEmpty() || !KickoffOverlay.showIn(MainActivity.this, g.get(0))) && line != null) {
                try { android.widget.Toast.makeText(c, line, android.widget.Toast.LENGTH_LONG).show(); } catch (RuntimeException ignored) { }
            }
        }
    };
    private final BroadcastReceiver reminderDue = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { if (reminder != null && resumed) reminder.refresh(); }
    };
    private View clockBlock, bottomBlock;   // the greeting, clock and date; the app title and the dock under it

    private final BroadcastReceiver tick = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            // a Wi-Fi broadcast redraws only when the minute moved (ClockTick); the settings are read on clockReads first, so the redraw on the
            // UI thread finds them in ConfigSnapshot's 3-second copy and never waits on the screensaver's process (a user report on 4.1: lag)
            if (!ClockTick.redraw(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(i.getAction()), clockDrawnAt, System.currentTimeMillis())) return;
            final Context app = getApplicationContext();
            clockReads.execute(() -> {
                ConfigSnapshot.read(app);
                Extras.askNow();   // 4.4 (QA-6): the add-on's state for the tick's screens, read here, off the UI thread
                notices.load(app);   // 4.4 (QA-10): the notices file, again only when it changed
                runOnUiThread(() -> {
                    if (isDestroyed() || !resumed) return;
                    updateClock();
                    updateNowPlaying();   // the minutes left of its sleep timer
                });
            });
        }
    };
    /** the settings read of the clock's tick, off the UI thread (one at a time) */
    private final java.util.concurrent.ExecutorService clockReads = java.util.concurrent.Executors.newSingleThreadExecutor();
    /** when updateClock last drew (ms since the epoch), for ClockTick */
    private long clockDrawnAt;
    // «تلاوة» started, paused, changed or stopped a surah or a station (MusicService says so; nothing polls): the «يُتلى الآن» card follows
    private final BroadcastReceiver nowPlayingChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            updateNowPlaying();
            // the adhan began or ended: the bar names it, or goes. From the answer this process already has; only with none yet is it read, off the UI thread
            ConfigSnapshot known = ConfigSnapshot.known();
            if (known != null) { updateAdhanBar(known); return; }
            final Context app = getApplicationContext();
            clockReads.execute(() -> {
                final ConfigSnapshot got = ConfigSnapshot.read(app);
                runOnUiThread(() -> { if (!isDestroyed() && !isFinishing()) updateAdhanBar(got); });
            });
        }
    };
    // the sleep timer started, ended or was cancelled (here, or from the computer): the pill follows at once
    private final BroadcastReceiver sleepChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { updateSleepPill(); }
    };
    private final BroadcastReceiver packages = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { PhoneApps.invalidate(); if (!resumed) { appsStale = true; return; } buildApps(); }   // P2: while another app is in front the rows are built at the return, once; buildApps ends «تعديل المفضلة» and «تعديل التطبيقات» without saving: what they edit is stale
    };
    /** the apps changed while the home was not in front: the rows are built again at the return (P2) */
    private boolean appsStale;

    // favourites changed in «ترتيب المفضلة» or from ADB while the home screen is up
    /** a tester flipped a debug switch (dock.sh debug): the home starts over so it is read at once */
    private final BroadcastReceiver debugChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { recreate(); }
    };

    private final BroadcastReceiver dockChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { PhoneApps.invalidate(); dockApps = loadDock(); if (!resumed) { appsStale = true; return; } buildApps(); }
    };

    // the cards were changed from the phone while the home screen is in front: follow now, not at the next return
    private final BroadcastReceiver cardsChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { if (resumed) { homeCard.refresh(); refreshSideCards(); wireFocus(); } }
    };

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (CrashLoopStore.safe(this)) {   // 4.3.5 (R7): the home died twice right after starting: the safe page, never a black screen
            safeRedirect = true;
            openSafePage();
            finish();
            return;
        }
        android.content.res.Configuration built = getResources().getConfiguration();   // the size and density the layout is made for (onConfigurationChanged)
        builtWidthDp = built.screenWidthDp;
        builtHeightDp = built.screenHeightDp;
        builtDensityDpi = built.densityDpi;
        CrashLoopStore.opened(this);   // 4.3.5: the screen must draw its first frame within 20 s, or the next start counts a hang (CrashLoop)
        getWindow().getDecorView().getViewTreeObserver().addOnDrawListener(firstDraw);
        if (DebugFlags.on(this, "keytrace")) KeyTrace.install();
        dbgFocus = DebugFlags.focusMask(this);   // a tester's switches for the tile's focus animation (dock.sh debug plainfocus ...)
        if (Ram.low(this)) dbgFocus |= DebugFlags.LIFT | DebugFlags.SHINE;   // A14 «الوضع الخفيف»: the focused tile without its elevation shadow and its shine (the scale and the highlight stay)
        FamilyDnsAndroid.status(this);   // first use in this process: undoes a Family DNS check that a crash cut short
        sa.urtv.home.setup.SetupHook.atHomeStart(this);   // a home switch that was cut off is put back (it never throws)
        Digits.init(this);   // the screensaver's «الأرقام» setting, for every number this screen writes
        { final Context app = getApplicationContext(); clockReads.execute(() -> DeviceDefaults.apply(app)); }   // 4.3.3: this model's published defaults, only where nothing is set (devices.json); 4.4 QA-13: its file is read on a thread
        langBuilt = S.lang();   // the language this screen is built in (updateClock rebuilds it when «اللغة» changes)
        dockApps = loadDock();
        // G4: the TV's live TV app and source picker are not looked for here (a dozen package manager calls on the main thread before the first frame): the inputs list and «التلفزيون» ask each time they open
        cols = getResources().getInteger(R.integer.columns);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(buildUi());
        showSnapshot();   // a cold start: the home's last picture when there is a good one, at once; it fades out when the live views have painted. Never a skeleton, never black blocks
        resumeAt = SystemClock.elapsedRealtime();
        firstFrameWallpaper();   // the small copy of the last photo in the first frame
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener((was, now) -> { KeyTrace.focusChanged(); placeName(); });   // FavBar: the name only in the bar
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener((was, now) -> {   // design/43: the top row opens while the focus is in it, and closes after
            if (pillBar == null || barHeld) return;
            View v = now;
            while (v != null && v != pillBar && v.getParent() instanceof View) v = (View) v.getParent();
            pillBar.focusInside(v == pillBar);
        });
        // started by the screensaver's «الأذان» row; not again after a restore (the system hands back the same intent)
        if (b == null && (getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
            openRequested(getIntent());
            phoneRequested(getIntent());   // the phone remote's «افتح»
            settingsKeyRequested(getIntent());   // the Settings key outside the home, when the TV's settings need the PIN
            keyAppRequested(getIntent());   // «أزرار الريموت» over another app, when the button's app needs the PIN
        }
        wantEdit = getIntent() != null && getIntent().getBooleanExtra(EXTRA_EDIT, false);   // the settings' «تعديل المفضلة»
        if (b == null) appsRequested(getIntent());   // the settings' «ترتيب كل التطبيقات» (not again after a restore)
        buildApps();

        IntentFilter pf = new IntentFilter();
        pf.addAction(Intent.ACTION_PACKAGE_ADDED);
        pf.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pf.addAction(Intent.ACTION_PACKAGE_CHANGED);
        pf.addDataScheme("package");
        registerReceiver(packages, pf);
        InternalReceivers.register(this, dockChanged, new IntentFilter(Dock.CHANGED));
        InternalReceivers.register(this, debugChanged, new IntentFilter(DebugFlags.CHANGED));
        InternalReceivers.register(this, cardsChanged, new IntentFilter(HomeCard.CHANGED));
        InternalReceivers.register(this, sleepChanged, new IntentFilter(SleepTimer.CHANGED));
        InternalReceivers.register(this, nowPlayingChanged, new IntentFilter(NowPlaying.CHANGED));
        InternalReceivers.register(this, reminderDue, new IntentFilter(ReminderReceiver.SHOW));
        InternalReceivers.register(this, kickoffDue, new IntentFilter(KickoffReceiver.SHOW));
        homeLook = new HomeLook(this);
        // 4.5 the screensaver's settings tell the home a look changed (Prefs.LOOK_CHANGED), only under our signature permission
        IntentFilter lf = new IntentFilter(LOOK_CHANGED);
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(lookChanged, lf, LOOK_PERMISSION, null, Context.RECEIVER_EXPORTED);
        else registerReceiver(lookChanged, lf, LOOK_PERMISSION, null);
        IntentFilter pf2 = new IntentFilter(LOOK_PREVIEW);
        pf2.addAction(LOOK_PANEL);
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(lookPanel, pf2, LOOK_PERMISSION, null, Context.RECEIVER_EXPORTED);
        else registerReceiver(lookPanel, pf2, LOOK_PERMISSION, null);
    }

    // ---- 4.5 (docs/ui/PROPOSAL.md, approved): a look changed shows at once and whole, behind HomeLook's cover when the home is on the screen
    static final String LOOK_CHANGED = "sa.urtv.home.LOOK_CHANGED", LOOK_PERMISSION = "sa.urtv.home.LOOK";
    private HomeLook homeLook;
    /** the home is on the screen (onStart to onStop), even under a see-through screen such as «المظهر» */
    private boolean shown;
    private final BroadcastReceiver lookChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { lookChangedNow(); }
    };

    /** the saved look, read now (not the 3-second copy, not at the next minute), off the UI thread, then shown */
    private void lookChangedNow() {
        lookPreview = null;   // a look was kept (OK) or the saved one is wanted: no preview over it any more
        homeCard.preview(null, null);
        ConfigSnapshot.expire();
        final Context app = getApplicationContext();
        clockReads.execute(() -> {
            final ConfigSnapshot got = ConfigSnapshot.read(app);
            runOnUiThread(() -> { if (got != null && !isDestroyed() && !isFinishing()) showLook(got); });
        });
    }

    /**
     * The home in the look {@code cfg} says, every part of it in one go: on the screen, behind HomeLook's still picture and one fade (a change that
     * throws puts the look shown before back, still behind it); not on the screen (the settings in front), at once, so the first frame of the
     * return is already the new look (it was the old one, then the parts one by one: docs/ui/STUDY.md). The focus stays on the same app.
     */
    private void showLook(final ConfigSnapshot cfg) {
        final String pkg = focusedPkg();
        final ConfigSnapshot before = lastCfg;
        final Runnable apply = () -> {
            updateClock(cfg);
            if (resumed) homeCard.refresh();   // not in front: onResume refreshes it, and the new page shows from its first frame (HomeCard)
            applyClean();
            wireFocus();
            if (pkg != null) { focusPkgAfter = pkg; applyPendingFocus(); }
        };
        if (!shown || homeLook == null) { apply.run(); return; }
        homeLook.change(apply, () -> { if (before != null) updateClock(before); }, () -> appsSettled() && (homeCard == null || homeCard.settled()));
    }

    // «المظهر» (LookPanelActivity, in the screensaver app, see-through over this screen): the home moves aside while it is open, and shows the
    // look under its focus before it is kept. The preview is never saved: it lays its parts over every read of the saved look (updateClock) and
    // over the cards' choice (HomeCard.preview); its end, or Back, shows the saved look again; OK saves (Prefs) and LOOK_CHANGED follows.
    static final String LOOK_PREVIEW = "sa.urtv.home.LOOK_PREVIEW", LOOK_PANEL = "sa.urtv.home.LOOK_PANEL";
    /** layout, bg, clock, apps, light, cards (null each: the saved one); null: no preview */
    private String[] lookPreview;
    private final BroadcastReceiver lookPanel = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (LOOK_PANEL.equals(i.getAction())) { aside(i.getBooleanExtra("open", false)); if (!i.getBooleanExtra("open", false)) previewEnd(); return; }
            if (!i.getBooleanExtra("on", false)) { previewEnd(); return; }
            String[] p = {i.getStringExtra("layout"), i.getStringExtra("bg"), i.getStringExtra("clock"), i.getStringExtra("apps"), i.getStringExtra("light"), i.getStringExtra("cards")};
            if (java.util.Arrays.equals(p, lookPreview)) return;
            lookPreview = p;
            homeCard.preview(p[0], p[5]);
            ConfigSnapshot saved = ConfigSnapshot.known();
            if (saved != null) showPreview(saved);
        }
    };

    /** the preview shown, behind HomeLook's cover; the cards' page follows it even though this screen is paused under the panel */
    private void showPreview(final ConfigSnapshot saved) {
        final String pkg = focusedPkg();
        final ConfigSnapshot before = lastCfg;
        Runnable apply = () -> { updateClock(saved); homeCard.refresh(); applyClean(); if (pkg != null) { focusPkgAfter = pkg; applyPendingFocus(); } };
        if (!shown || homeLook == null) { apply.run(); return; }
        homeLook.change(apply, () -> { lookPreview = null; homeCard.preview(null, null); if (before != null) updateClock(before); }, () -> appsSettled() && (homeCard == null || homeCard.settled()));
    }

    /** no preview any more (Back, the panel closed, or OK and its LOOK_CHANGED): the saved look, read fresh */
    private void previewEnd() {
        if (lookPreview == null) return;
        lookPreview = null;
        homeCard.preview(null, null);
        lookChangedNow();
    }

    /** the whole home a little smaller beside the panel (one transform of its root, no layout), or back; a cut in «الوضع الخفيف» */
    private void aside(boolean open) {
        View root = findViewById(android.R.id.content);
        if (root == null) return;
        float s = open ? ASIDE_SCALE : 1f;
        float edge = root.getWidth() * ASIDE_MARGIN / (1f - ASIDE_SCALE);   // the pivot that leaves that margin once scaled: p + (W - p) s = W - m
        root.setPivotX(Dir.rtl() ? root.getWidth() - edge : edge);
        root.setPivotY(root.getHeight() / 2f);
        root.animate().cancel();
        if (Ram.low(this)) { root.setScaleX(s); root.setScaleY(s); return; }
        root.animate().scaleX(s).scaleY(s).setDuration(Motion.MS).setInterpolator(Motion.ease()).start();
    }
    /** the panel is 700 of 1920 at the start side's other end; the home, scaled, keeps 40 px (of 1920) from its own edge */
    static final float ASIDE_SCALE = 0.6146f, ASIDE_MARGIN = 40f / 1920f;

    /** the app whose tile has the focus, null when the focus is elsewhere */
    private String focusedPkg() {
        View f = getCurrentFocus();
        for (Map.Entry<String, View> e : cardByPkg.entrySet()) {
            for (View v = f; v != null; v = v.getParent() instanceof View ? (View) v.getParent() : null) if (v == e.getValue()) return e.getKey();
        }
        return null;
    }

    /** the photos were let go: decoding the next one starts HERE, at the start of the return (off the main thread, at screen size), not after the first frame's provider queries */
    @Override
    protected void onStart() {
        super.onStart();
        shown = true;
        SaverSettingsCopy.refreshSoon(this);   // 4.4: the copy of the screensaver's settings kept fresh, at most every 30 min, on a thread
        if (wallTrimmed) { wallTrimmed = false; returnWallpaper(); }
    }

    @Override
    protected void onStop() {
        shown = false;
        if (homeLook != null) homeLook.finishNow();   // a cover still up when the screen was left: the change is complete, nothing to fade
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // the Activity only stopped: its live views are all still there, so NO overlay (the picture is for a cold start only)
        resumeAt = SystemClock.elapsedRealtime();
        photoLogged = false;
        sharpLogged = false;
        firstFrameWallpaper();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        if (sceneHome != null) sceneHome.start();
        if (resumeAt == 0) resumeAt = SystemClock.elapsedRealtime();
        firstFrameWallpaper();   // (a return that did not restart: the photo may have been let go by onTrimMemory)
        if (LiteAutoStore.pending() != null) AfterFrame.run(getWindow().getDecorView(), this::liteNotice);   // 4.4 beta 3: «الوضع الخفيف» came on by itself
        handler.postDelayed(snapTick, HomeSnapshot.IDLE_MS);
        if (sa.urtv.home.setup.SetupReopen.take()) {   // «إعداد تلفزيوني» was left behind by the home switch: its progress and its done screen come back (once)
            try { guarded(ParentalPolicy.WIDGET_MATCHES, null, S.t("إعداد تلفزيوني"), () -> launch(new Intent(this, sa.urtv.home.setup.SetupActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))); } catch (RuntimeException ignored) { }   // the person opened this screen already (its own lock was passed then)
        }
        Extras.refresh();   // 4.4 (QA-6): the add-on's state, asked on a thread (the screens read the kept answer)
        checkOverlayOffer();   // 4.4: once, for whoever used «فوق تطبيق آخر» on 4.3.x (the settings and the add-on are asked on a thread)
        { final Context app = getApplicationContext(); clockReads.execute(() -> notices.load(app)); }   // 4.4 (QA-10): the notices file, off the UI thread
        sa.urtv.home.phone.PhoneUpdater.homeIsUp(getApplicationContext());   // an update cover waiting in front for this home closes now
        sa.urtv.home.setup.SetupHook.homeUp(this);   // «تخلّص من واجهة المصنع»: this boot's home came up (BootGuard)
        UpdateCarry.homeShown = true;   // the carry-over after our update asks whether our home is in front
        if (!Parental.state(this).kids) BetaNotes.maybeAsk(this);   // 4.3.3: only on the beta channel, once: after a beta update, or after a crash
        front = new java.lang.ref.WeakReference<>(this);   // TypeService's key hand-over (keyFromService)
        backExit.reset(); backGuarded = false;   // «الخروج من تلفزيونك»: a new visit, its count and its hint start again
        sa.urtv.home.tilawah.AdhanFront.home(true);   // «الأذان على شاشة التوقف فقط»: the adhan's alarm asks what is in front
        try { sa.urtv.home.tilawah.MusicService.keepForeground(this); } catch (RuntimeException ignored) { }   // 4.3.3 G1: a surah started from the background gets its foreground notice now
        A11yHeal.onStart(this);   // once per process: our accessibility service listed but not bound after an update (see A11yHeal)
        Parental.SESSION.clear();   // the home screen is in front again: an app opened with the PIN asks again next time
        if (control != null) control.forgetLast();   // «مركز التحكم» opens on its first tile again
        bellForce = true;   // [updates bell]
        IntentFilter tf = new IntentFilter(Intent.ACTION_TIME_TICK);
        tf.addAction(Intent.ACTION_TIME_CHANGED);
        tf.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        tf.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(tick, tf);
        if (appsStale) { appsStale = false; buildApps(); }   // an app was installed, removed or the favourites changed while another app was in front
        updateClock();
        updateNowPlaying();
        homeCard.refresh();
        refreshSideCards();
        applyClean();   // «UrTV النظيف» changed while another screen was in front (after the card refresh: applyClean may hide the card)
        if (reminder != null) reminder.refresh(true);   // a card that came due while another app was in front; the alarm follows the tasks (both off the UI thread: QA-3)
        readAnimationScale();
        loadTvRows();   // what one watched since: «كمّل المشاهدة» moves on
        showUpdateResult();
        wireFocus();   // the «المهام» card may have come or gone with the settings
        View last = lastFocusedId == View.NO_ID || !keyedSinceHome ? null : findViewById(lastFocusedId);
        if (last == null && !dockCards.isEmpty()) last = dockCards.get(0);
        if (last != null) last.requestFocus();
        if ("open".equals(getIntent().getStringExtra("bar")) && pillBar != null) { barHeld = true; pillBar.focusInside(true); }   // a picture of the open row (DebugBarShot); nothing else sets it
        updateHomeNudge(true);   // the bar: an update of ours (4.4), the city, a notice (no bar asks to make UrTV the home any more)
        // Catch installs/removals that happened while another app was in front. G4: asked after the return's first frame (a package manager call), not before it
        AfterFrame.run(getWindow().getDecorView(), () -> { if (resumed && !isDestroyed() && getPackageManager().getChangedPackages(packageSeq) != null) buildApps(); });
        if (firstShow) {   // the entrance plays once the favourites are on screen (they come from the loader thread)
            firstShow = false;
            if (appsShown) playEntrance(); else entranceDue = true;
        }
        // New photo on return only if the current one has been up for the full interval (WallTurn: the wall clock kept in the prefs); a photo let go
        // (a trim) or a home started again shows the one it left
        if (wallTrimmed || wallIndex < 0 || WallTurn.due(prefs().getLong("wallpaperAt", 0), System.currentTimeMillis(), WALLPAPER_INTERVAL_MS)) { wallTrimmed = false; returnWallpaper(); }
        else {
            scheduleWallpaper(WallTurn.left(prefs().getLong("wallpaperAt", 0), System.currentTimeMillis(), WALLPAPER_INTERVAL_MS));
            checkOwnChanged();   // the owner added or removed photos while another app was in front
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        if (overlayOnScreen) {   // it was on the screen: after a few seconds it counts as seen (once); sooner, it comes again at the next visit
            if (android.os.SystemClock.elapsedRealtime() - overlayShownAt >= 4000) { overlayDue = false; markOverlayOffered(); }
            overlayOnScreen = false;
        }
        noticeOnScreen = null;   // 4.3.3: a notice was seen on this visit; it does not come back
        if (sceneHome != null) sceneHome.stop();   // no timer and no fade while another screen is in front
        updateCardOnScreen = -1;   // 4.4: the same for the card «في تحديث لـ …»
        UpdateCarry.homeShown = false;   // a slow photo check that answers now must not start the 10-minute rotation again
        sa.urtv.home.tilawah.AdhanFront.home(false);
        remoteKeys.reset();   // a shortcut key opened an app: its release went there, not here
        PinPrompt.dismiss(this);
        if (edit != null) leaveEdit();   // another screen came up: what was arranged is saved
        if (appsEdit != null) leaveAppsEdit();   // the same for «تعديل التطبيقات»
        handler.removeCallbacks(rotateWallpaper);
        handler.removeCallbacks(snapTick);
        if (snapOverlay == null && WallFirstFrame.snapshotWorthKeeping(fullPhotoShown, appsShown, wallTrimmed)) HomeSnapshot.capture(this, homeRoot, null);   // the picture the next cold start draws first: never one without the photo or the apps bar
        unregisterReceiver(tick);
        if (inputsPopup != null) inputsPopup.dismiss();
        if (control != null) control.dismiss();   // its speed test stops with it
        if (appOptions != null) { appOptions.dismiss(); appOptions = null; }
        homeCard.pause();
        if (sideCards != null) sideCards.pause();
        if (reminder != null) reminder.hide();
    }

    /**
     * A tight TV (Ram.tight: LOW or MID) with another app in front: the home's photos and its card page let go, so the app in front has that memory (a user report
     * on 4.1: lag). onResume brings them back (a fresh photo at once, the card page through HomeCard.refresh). On other TVs nothing changes.
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        android.util.Log.i("UrTVHome", "trim level " + level + " (resumed=" + resumed + ", ram tier " + Ram.tier(this) + ")");
        if (resumed && level >= TRIM_MEMORY_RUNNING_LOW && level < TRIM_MEMORY_UI_HIDDEN) LiteAutoStore.note(this, LiteAuto.TRIM);   // 4.4 beta 3: memory ran low with the home in front
        if (resumed || !Ram.trimNow(Ram.tier(this), level)) return;   // only when the system really asks (LOW: behind another app; MID: MODERATE and worse; NORMAL: never)
        homeCard.destroy();
        if (sideCards != null) sideCards.drop();   // review F121: «كنبة»'s side cards are a second page in memory: let go with the first (onResume makes it again)
        if (sceneHome != null) sceneHome.trim();   // the scene not showing lets its pictures go
        thinWallpaper();   // the photos go, one tiny stand-in stays so the way back is not black until the next photo decodes
        wallTrimmed = true;
        firstWallpaper = true;   // the short fade when it comes back
    }
    // ---------------------------------------------------------------- the last picture (HomeSnapshot)

    private FrameLayout homeRoot;
    private View snapOverlay;   // the picture or skeleton in front while the live views load
    private long snapShownAt;
    private final Runnable snapTick = new Runnable() {   // while the home is in front and idle: a fresh picture every ten minutes
        @Override public void run() {
            if (resumed && snapOverlay == null && WallFirstFrame.snapshotWorthKeeping(fullPhotoShown, appsShown, wallTrimmed)) HomeSnapshot.capture(MainActivity.this, homeRoot, null);
            handler.postDelayed(this, HomeSnapshot.IDLE_MS);
        }
    };
    private final Runnable snapCheck = new Runnable() {
        @Override public void run() {
            if (snapOverlay == null) return;
            long waited = android.os.SystemClock.elapsedRealtime() - snapShownAt;
            boolean live = waited >= HomeSnapshot.MIN_MS && !(homeCard != null && homeCard.loading());
            if (getIntent() != null && "hold".equals(getIntent().getStringExtra("snap"))) { handler.postDelayed(this, 500); return; }   // DebugSnapShot: the picture stays for a picture of it
            if (live || waited >= HomeSnapshot.LIVE_WAIT_MS) hideSnapshot();
            else handler.postDelayed(this, 120);
        }
    };

    /** the last picture of the home (half size, from the cache), else the skeleton in the look's colours, over everything until the live frame is there */
    private void showSnapshot() {
        if (snapOverlay != null || homeRoot == null) return;
        final android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        final long asked = SystemClock.elapsedRealtime();
        // 4.4 QA-13: the picture (a 300 KB WebP in this app's cache) is decoded on a thread and put over the home when it is here; one that comes
        // after the live frame (SNAP_LATE_MS) is not shown, as when there is none (the live views and the look's own colour, never black blocks)
        clockReads.execute(() -> {
            final Bitmap b = HomeSnapshot.load(this, dm.widthPixels, dm.heightPixels);
            runOnUiThread(() -> {
                if (isDestroyed() || isFinishing() || snapOverlay != null || homeRoot == null) return;
                if (b == null || SystemClock.elapsedRealtime() - asked > SNAP_LATE_MS) {
                    android.util.Log.i("UrTVHome", "snapshot overlay: none (no usable picture)");
                    return;
                }
                ImageView v = new ImageView(this);
                v.setScaleType(ImageView.ScaleType.FIT_XY);
                v.setImageBitmap(b);
                v.setFocusable(false);
                homeRoot.addView(v, new FrameLayout.LayoutParams(-1, -1));
                snapOverlay = v;
                snapShownAt = android.os.SystemClock.elapsedRealtime();
                android.util.Log.i("UrTVHome", "snapshot overlay: picture shown");
                handler.removeCallbacks(snapCheck);
                handler.postDelayed(snapCheck, 120);
            });
        });
    }

    private static final long SNAP_LATE_MS = 400;

    private void hideSnapshot() {
        final View v = snapOverlay;
        if (v == null) return;
        snapOverlay = null;
        android.util.Log.i("UrTVHome", "snapshot overlay: gone after " + (android.os.SystemClock.elapsedRealtime() - snapShownAt) + " ms");
        v.animate().alpha(0f).setDuration(HomeSnapshot.FADE_MS).withEndAction(() -> { if (homeRoot != null) homeRoot.removeView(v); }).start();
        // 4.4 beta 3: the fade's end may never come (no frames: the screen off, a stalled start); the picture goes anyway, a little after it
        handler.postDelayed(() -> { if (v.getParent() instanceof ViewGroup) ((ViewGroup) v.getParent()).removeView(v); }, HomeSnapshot.FADE_MS + 400);
    }

    // ---- 4.4 beta 3, the frozen screen (the owner's TV, 21 Nov, and the tester's «كل التطبيقات»: keys reached the home, OK opened an app,
    // the screen stood still, no ANR): a picture left over the working home. Every cover has a hard end (HomeLook.HARD_MS, the start picture's
    // own); and the watchdog: a key with nothing drawn for FROZEN_MS after it, or a cover older than its end at a key, drops every cover,
    // draws again and logs the fixed word «frozen-screen recovered».
    static final long FROZEN_MS = 2000;
    private long lastDrawAt, keyAt;
    private boolean drawWatch, frozenLogged;   // frozenLogged: the quiet recovery (no cover) was logged once this run
    private final Runnable frozenCheck = () -> { if (lastDrawAt < keyAt) recoverFrozen("no frame after a key"); };

    private void watchKey() {
        if (!drawWatch && getWindow() != null) {
            drawWatch = true;
            getWindow().getDecorView().getViewTreeObserver().addOnDrawListener(() -> lastDrawAt = SystemClock.uptimeMillis());
            getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener((o, n) -> lastDrawAt = SystemClock.uptimeMillis());   // 4.4 beta 4: a focus move is drawn by the render thread (a zoom needs no draw pass): not a frozen screen
        }
        long now = SystemClock.uptimeMillis();
        keyAt = now;
        if (coverStuck(now)) { recoverFrozen("a cover past its end"); return; }
        handler.removeCallbacks(frozenCheck);
        handler.postDelayed(frozenCheck, FROZEN_MS);
    }

    /** a cover still up after the longest it may stay */
    private boolean coverStuck(long now) {
        boolean look = homeLook != null && homeLook.since() > 0 && now - homeLook.since() > HomeLook.HARD_MS;
        boolean snap = snapOverlay != null && SystemClock.elapsedRealtime() - snapShownAt > HomeSnapshot.LIVE_WAIT_MS + HomeSnapshot.FADE_MS + 400;
        return look || snap;
    }

    private void recoverFrozen(String why) {
        boolean any = homeLook != null && homeLook.drop();
        if (snapOverlay != null) { View v = snapOverlay; snapOverlay = null; v.animate().cancel(); if (v.getParent() instanceof ViewGroup) ((ViewGroup) v.getParent()).removeView(v); any = true; }
        if (homeRoot != null) homeRoot.invalidate();
        if (getWindow() != null) getWindow().getDecorView().invalidate();
        // 4.4 beta 4 (the tester's TCL, build 620: one line every few seconds with nothing dropped, its frames unseen by the listener): the redraw
        // runs every time; the line is written when a cover was dropped, and only once a run when nothing was
        if (any || !frozenLogged) android.util.Log.w("UrTVHome", "frozen-screen recovered (" + why + (any ? ", a cover dropped" : "") + ")");
        if (!any) frozenLogged = true;
    }

    /** 4.3.1 hotfix: the small (480 px) copy of the photo in front: in memory and on disk, drawn in the first frame of a start or a return when no photo is in a view (WallFirstFrame) */
    private volatile Bitmap smallPhoto;
    private boolean fullPhotoShown, photoLogged, sharpLogged;
    private long resumeAt;
    private static final String WALL_SMALL = "wall-small.webp";

    private void firstFrameWallpaper() {
        if (wallFront == null) return;
        boolean has = wallFront.getDrawable() != null;
        // 4.4 QA-13: only the copy in memory is drawn in the first frame; the copy on disk is decoded on a thread and drawn when it is here (the
        // look's colour stays behind it meanwhile, never black), and only when no photo has come in the view by then
        int how = WallFirstFrame.source(has, smallPhoto != null, true);
        if (how == WallFirstFrame.MEMORY) {
            Bitmap b = smallPhoto;
            wallFront.setImageBitmap(b);
            wallFront.setAlpha(1f);
            logFirstPhoto("small copy, memory");
            return;
        }
        if (how == WallFirstFrame.DISK) {
            if (wallDiskLoading) return;
            wallDiskLoading = true;
            final java.io.File f = new java.io.File(getFilesDir(), WALL_SMALL);
            clockReads.execute(() -> {
                final Bitmap b = f.isFile() ? android.graphics.BitmapFactory.decodeFile(f.getPath()) : null;
                runOnUiThread(() -> {
                    wallDiskLoading = false;
                    if (b == null || isDestroyed() || wallFront == null || wallFront.getDrawable() != null) { logFirstPhoto("none, the look's colour"); return; }
                    if (smallPhoto == null) smallPhoto = b;
                    wallFront.setImageBitmap(b);
                    wallFront.setAlpha(1f);
                    logFirstPhoto("small copy, disk");
                });
            });
            return;
        }
        logFirstPhoto(has ? "photo kept" : "none, the look's colour");
    }

    private boolean wallDiskLoading;

    /** one line per return for the tester: the time from the home coming back to the first photo frame (tag UrTVHome) */
    private void logFirstPhoto(String how) {
        if (photoLogged) return;
        photoLogged = true;
        android.util.Log.i("UrTVHome", "resume to first photo frame: " + (SystemClock.elapsedRealtime() - resumeAt) + " ms (" + how + "), ram tier " + Ram.tier(this));
    }

    /** the small copy of a photo just shown, made off the main thread */
    private void saveSmallPhoto(Bitmap src) {
        try {
            int w = WallFirstFrame.SMALL_W, h = WallFirstFrame.smallHeight(src.getWidth(), src.getHeight());
            Bitmap small = Bitmap.createScaledBitmap(src, w, h, true);
            smallPhoto = small;
            java.io.File tmp = new java.io.File(getFilesDir(), WALL_SMALL + ".tmp");
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(tmp)) { small.compress(Bitmap.CompressFormat.WEBP, 80, out); }
            if (!tmp.renameTo(new java.io.File(getFilesDir(), WALL_SMALL))) tmp.delete();
        } catch (Exception | OutOfMemoryError ignored) { }
    }

    /**
     * both wallpaper bitmaps are let go but a stand-in always stays (never a bare view): the small copy when there is one, else a 480 px copy of whatever the view draws (a BitmapDrawable or any
     * other drawable, rendered small); when no copy can be made (out of memory) the photos are left as they are, not nulled. CENTER_CROP stretches it soft until the next photo fades in over it.
     */
    private void thinWallpaper() {
        Bitmap thumb = smallPhoto;
        if (thumb == null) {
            android.graphics.drawable.Drawable d = wallFront == null ? null : wallFront.getDrawable();
            try {
                if (d instanceof android.graphics.drawable.BitmapDrawable) {
                    Bitmap b = ((android.graphics.drawable.BitmapDrawable) d).getBitmap();
                    if (b != null && b.getWidth() > 0 && b.getHeight() > 0) thumb = Bitmap.createScaledBitmap(b, WallFirstFrame.SMALL_W, Math.max(1, WallFirstFrame.SMALL_W * b.getHeight() / b.getWidth()), true);
                } else if (d != null && d.getIntrinsicWidth() > 0 && d.getIntrinsicHeight() > 0) {
                    thumb = Bitmap.createBitmap(WallFirstFrame.SMALL_W, Math.max(1, WallFirstFrame.SMALL_W * d.getIntrinsicHeight() / d.getIntrinsicWidth()), Bitmap.Config.ARGB_8888);
                    android.graphics.Canvas cv = new android.graphics.Canvas(thumb);
                    d.setBounds(0, 0, thumb.getWidth(), thumb.getHeight());
                    d.draw(cv);
                }
            } catch (RuntimeException | OutOfMemoryError e) { thumb = null; }
        }
        if (thumb == null) {   // nothing to leave behind: keep what is there rather than a bare view
            android.util.Log.i("UrTVHome", "trim: no stand-in could be made, the photos stay");
            return;
        }
        fullPhotoShown = false;
        for (ImageView v : new ImageView[] {wallFront, wallBack}) if (v != null) v.setImageDrawable(null);   // both photos (not the fade's end: tilawah_test)
        if (wallFront != null) wallFront.setImageBitmap(thumb);
    }
    /** the photos were let go (onTrimMemory): the next onResume shows one at once */
    private boolean wallTrimmed;

    /** 4.4 beta 4 SideCards: the screensaver's settings read on the loader, the column placed (or taken away) on the UI thread */
    private void refreshSideCards() {
        if (cleanShown) { if (sideCards != null) sideCards.drop(); return; }   // review F122: «UrTV النظيف» is bare: no side cards
        if (sideCards == null) sideCards = new SideCards(this, (android.view.ViewGroup) findViewById(android.R.id.content), () -> homeCard == null ? -1 : homeCard.stripTop());
        final SideCards side = sideCards;
        if (loader.isShutdown()) return;
        loader.execute(() -> {
            final String u = side.read();
            runOnUiThread(() -> { if (!isFinishing() && !isDestroyed() && resumed) side.show(u == null || homeCard == null ? u : homeCard.glassed(u)); });   // never after onPause (it resumes the page's timers)
        });
    }

    @Override
    protected void onDestroy() {
        if (safeRedirect) { super.onDestroy(); return; }   // nothing of the screen was made (CrashLoop)
        if (sideCards != null) sideCards.drop();
        unregisterReceiver(packages);
        KeyTrace.release();
        unregisterReceiver(dockChanged);
        try { unregisterReceiver(debugChanged); } catch (IllegalArgumentException ignored) { }
        unregisterReceiver(cardsChanged);
        unregisterReceiver(sleepChanged);
        unregisterReceiver(nowPlayingChanged);
        unregisterReceiver(reminderDue);
        unregisterReceiver(kickoffDue);
        try { unregisterReceiver(lookChanged); } catch (IllegalArgumentException ignored) { }
        try { unregisterReceiver(lookPanel); } catch (IllegalArgumentException ignored) { }
        loader.shutdownNow();
        tvLoader.shutdownNow();
        clockReads.shutdownNow();
        homeCard.destroy();
        if (photoGlass != null) photoGlass.shutdown();
        super.onDestroy();
    }

    private boolean safeRedirect;   // 4.3.5: this instance only opened SafeActivity and finished

    /** 4.3.5: the first frame of the home screen was drawn: marked once (off the draw itself), then the listener goes */
    private final android.view.ViewTreeObserver.OnDrawListener firstDraw = new android.view.ViewTreeObserver.OnDrawListener() {
        @Override public void onDraw() {
            final android.view.ViewTreeObserver.OnDrawListener self = this;
            handler.post(() -> {
                CrashLoopStore.drew(MainActivity.this);
                android.view.ViewTreeObserver o = getWindow().getDecorView().getViewTreeObserver();
                if (o.isAlive()) o.removeOnDrawListener(self);
            });
        }
    };

    /** 4.3.5 (R7): never behind the family lock: a home that keeps dying cannot ask for the PIN, and the page only offers the TV's own home */
    private void openSafePage() {
        launch(new Intent(this, SafeActivity.class));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Parental.SESSION.clear();
        if (edit != null) leaveEdit();   // Home while editing: the order so far is kept
        if (appsEdit != null) leaveAppsEdit();
        if (intent != null && intent.getBooleanExtra(EXTRA_EDIT, false)) {   // the settings' «تعديل المفضلة»
            wantEdit = true;
            if (appsShown) beginPendingEdit();
            return;
        }
        if (appsRequested(intent)) return;   // the settings' «ترتيب كل التطبيقات»
        openRequested(intent);   // the screensaver's «الأذان» row (open=adhan)
        phoneRequested(intent);   // the phone remote opened an app that asks for the PIN, or one of the pills
        settingsKeyRequested(intent);   // the Settings key outside the home, when the TV's settings need the PIN
        keyAppRequested(intent);   // «أزرار الريموت» over another app, when the button's app needs the PIN
        // Home pressed while already home: jump back to the first dock app.
        if (inputsPopup != null) inputsPopup.dismiss();
        if (control != null) { control.dismiss(); control.forgetLast(); }
        keyedSinceHome = false;   // Home: the first app, not the place the focus had before (onResume and the apps' build would put it back)
        lastFocusedId = View.NO_ID;
        if (!dockCards.isEmpty()) {
            dockCards.get(0).requestFocus();
            final View first = dockCards.get(0);   // 4.4 beta 4 (the emulator's rounds 9-10): the window gave the focus back to the top row's last button
            handler.post(() -> { if (first.isAttachedToWindow() && !first.hasFocus()) first.requestFocus(); });   // after it came back from another app
        }
        glideTo(0);
    }

    @Override
    public void onBackPressed() {
        if (nudge != null && nudge.getVisibility() == View.VISIBLE && (nudgeCard.hasFocus() || nudgeLater.hasFocus())) { snoozeNudge(); return; }   // Back on the bar hides it
        // Home has nowhere to go back to; Back from the grid returns to the dock.
        boolean scrolled = scroll.getScrollY() > 0 && !dockCards.isEmpty();
        if (scrolled) dockCards.get(0).requestFocus();
        // opened from its tile while the TV keeps its own home: Back at the top leaves, to the home that opened it
        else if (HomeBack.leaves(isDefaultHome(), false)) finish();
        // «الرجوع يفتح شاشة التوقف» (off by default): nothing to go back to, so the screensaver, behind its own lock as on its pill
        else if (HomeBack.startsSaver(Dock.backSaver(this), true, false, isInstalled(SAVER_PKG))) guarded(ParentalPolicy.PILL_SAVER, SAVER_PKG, "شاشة التوقف", this::startSaver);
        // 4.4 beta 3 «رجوع مرتين يفتح شاشة التوقف» (on by default): a second Back within a second starts the screensaver; the first does nothing more than before
        else {
            long now = android.os.SystemClock.uptimeMillis();
            if (HomeBack.twice(backTwice, now, lastBack, true, false, Parental.state(this).kids, isInstalled(SAVER_PKG))) {
                lastBack = 0;
                guarded(ParentalPolicy.PILL_SAVER, SAVER_PKG, "شاشة التوقف", this::startSaver);
            } else lastBack = now;
        }
    }

    private long lastBack;   // «رجوع مرتين»: when the last Back that did nothing came
    private volatile boolean backTwice = false;   // ConfigSnapshot.backTwice, taken with the settings

    /** Is this app the home the TV goes to on Home? (Not when the stock home is kept and this was opened from its tile.) */
    private boolean isDefaultHome() {
        ResolveInfo ri = getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY);
        return HomeBack.isDefaultHome(ri == null || ri.activityInfo == null ? null : ri.activityInfo.packageName, getPackageName());
    }

    // ---------------------------------------------------------------- layout

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        glowRoot = root;
        root.setBackgroundColor(GlassSource.look(this).base);   // the look's own solid colour, never black while something loads
        homeRoot = root;
        // The home is built in code, so it inherits no direction from the window: set it here and
        // the pills, the favourites and the app rows all read from right to left.
        root.setLayoutDirection(Dir.layout());

        // Wallpaper: two stacked layers so a new photo can crossfade in over the old one.
        // The backdrop (the photos, the dimming and the scrims) is one view that holds them, kept as ONE hardware layer unless the TV is low on RAM or a tester turned it off (dock.sh debug nobackdrop):
        // every frame of the tray's focus move then blits one texture instead of drawing a photo and three translucent full-screen layers (the render thread's cost was 14-18 ms issue + 18-28 ms gpu per frame)
        FrameLayout backdrop = new FrameLayout(this);
        backdrop.setClipChildren(false);
        if (!DebugFlags.on(this, "nobackdrop") && !((android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE)).isLowRamDevice() && !Ram.tight(this)) backdrop.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        root.addView(backdrop, new FrameLayout.LayoutParams(-1, -1));
        wallBack = wallpaperLayer();
        wallFront = wallpaperLayer();
        backdrop.addView(wallBack, new FrameLayout.LayoutParams(-1, -1));
        backdrop.addView(wallFront, new FrameLayout.LayoutParams(-1, -1));
        sceneBackdrop = backdrop;   // 4.4: the scene layer is made in sceneTick only when «الآفاق» is chosen
        // Scrim: darker at the top (clock, pills) and bottom (dock) so text stays readable.
        scrim = new View(this);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xB3000000, 0x40000000, 0x26000000, 0x8C000000, 0xE6000000}));
        wallDimView = new View(this);   // «تعتيم الخلفية»: extra black over the photo, 0 as it was
        wallDimView.setBackgroundColor(Color.BLACK);
        wallDimView.setAlpha(0f);
        backdrop.addView(wallDimView, new FrameLayout.LayoutParams(-1, -1));
        backdrop.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
        gridShade = new View(this);
        gridShade.setBackgroundColor(0xB3000000);
        gridShade.setAlpha(0f);
        backdrop.addView(gridShade, new FrameLayout.LayoutParams(-1, -1));
        try {
            wallpapers = getAssets().list("wallpapers");
        } catch (java.io.IOException ignored) { }

        glowSlot = root.getChildCount();   // «توهّج لون التطبيق»: off by default, then no view is made at all; on, it takes this place
        applyGlowMode();

        scroll = new HomeScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setFillViewport(true);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        scroll.addView(content, new FrameLayout.LayoutParams(-1, -2));

        // the slim bar before the adhan, over the home screen's top edge (the header starts 36 dp down)
        adhanBar = text(12, "sans-serif-medium", 0xFFFFE9B8);
        adhanBar.setGravity(Gravity.CENTER);
        adhanBar.setMaxLines(1);   // «إقامة العشاء بعد ١٣ دقيقة» is the longest: one line, even at 960×540
        adhanBar.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0x00DCB86F, 0x57DCB86F, 0x57DCB86F, 0x00DCB86F}));
        adhanBar.setOnFocusChangeListener((v, focused) -> {   // the bar is focusable only while the adhan plays: it lights up when OK would stop it
            v.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    focused ? new int[]{0x00DCB86F, 0xB3DCB86F, 0xB3DCB86F, 0x00DCB86F} : new int[]{0x00DCB86F, 0x57DCB86F, 0x57DCB86F, 0x00DCB86F}));
            ((TextView) v).setTextColor(focused ? 0xFFFFFFFF : 0xFFFFE9B8);
        });
        adhanBar.setVisibility(View.GONE);
        root.addView(adhanBar, new FrameLayout.LayoutParams(-1, dp(26), Gravity.TOP));
        duaCard = new DuaCard(this);
        root.addView(duaCard.view, duaCard.place(this));
        buildEditViews(root);
        buildHomeNudge(root);

        // Hero: exactly one screen tall, header on top, title + dock at the bottom.
        hero = new FrameLayout(this);
        hero.setClipChildren(false);
        int screenH = getResources().getDisplayMetrics().heightPixels;
        content.addView(hero, new LinearLayout.LayoutParams(-1, screenH));

        hero.addView(buildHeader(), lp(-1, -2, Gravity.TOP, dp(48), dp(36), dp(48), 0));
        // optional card under the pills, same size as beside the screensaver's clock
        homeCard = new HomeCard(this, hero, lp(dp(461), dp(250), Dir.end() | Gravity.TOP, Dir.rtl() ? dp(48) : 0, dp(84), Dir.rtl() ? 0 : dp(48), 0), this::homeCardBox, new HomeCard.Live() {
            @Override public void focus(View target, boolean has) {   // «المهام» on the card takes the focus like a pill
                if (has) { lastFocusedId = target.getId(); glideTo(0); fadeGlow(1f); }
            }
            @Override public void open() {   // OK: the list, with the focus on «🎤 تكلّم» so one more OK starts the voice adding
                launchGuarded(ParentalPolicy.CARD_TASKS, new Intent(MainActivity.this, TasksActivity.class).putExtra(TasksActivity.EXTRA_MIC, true), S.t("المهام"));
            }
            @Override public void markets() {   // OK on the stocks card: «الأسواق» («محفظتي» there has its own lock)
                if (!cleanShown) launch(new Intent(MainActivity.this, MarketsActivity.class));   // «UrTV النظيف»: no way into the markets page
            }
        });
        homeCard.football(new FootballFocus.Host() {   // «المظهر ← الواجهة ← كرة القدم» (docs/PROPOSALS-NOV2.md §1)
            @Override public int headerBottom() {
                View head = clockBlock == null ? null : (View) clockBlock.getParent();
                if (head == null || head.getHeight() == 0) return bar(192);
                return head.getTop() + Math.max(clockBlock.getBottom(), pillRow == null ? 0 : pillRow.getBottom()) + bar(24);
            }
            @Override public boolean light() { return lightNow; }
            @Override public void focus(View target, String which, boolean has) {
                if (!has) return;
                lastFocusedId = target.getId(); glideTo(0); fadeGlow(1f);
                lowerTray(FootballFocus.MT.equals(which));   // the games open into a row: the tray makes room, the other widgets need none
            }
            @Override public void open(String which, String eventId) {
                if (cleanShown && FootballFocus.MT.equals(which)) return;   // «UrTV النظيف»: no way into the games page
                if (FootballFocus.MT.equals(which)) {
                    Intent games = new Intent(MainActivity.this, MatchesActivity.class).putExtra(MatchesActivity.EXTRA_EVENT, eventId);
                    guarded(ParentalPolicy.WIDGET_MATCHES, null, S.t("المباريات"), () -> launch(games));   // our own page: no app of the lists
                } else if (FootballFocus.PR.equals(which)) {
                    if (getResources().getBoolean(R.bool.show_listening)) launchGuarded(ParentalPolicy.PILL_LISTEN, adhanSettingsIntent(), S.t("إعدادات الأذان"));
                } else openOurSettings(false);
            }
        });
        hero.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> homeCard.fit());

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setClipChildren(false);
        bottom.setClipToPadding(false);
        hero.addView(bottom, lp(-1, -2, Gravity.BOTTOM, bar(FavBar.SIDE), 0, bar(FavBar.SIDE), bar(FavBar.REST_BOTTOM)));   // the tray's place (FavBar)
        bottomBlock = bottom;

        title = text(34, "sans-serif-medium", Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_PX, bar(FavBar.NAME_PX));   // FavBar: 30 px at 1080p, scaled by the screen's height
        title.setShadowLayer(dp(18), 0, 0, 0x66000000);
        title.setSingleLine(true);
        // the favourite's name, and beside it the quiet hint for the first few times (optionsHint): «اضغط مطوّلاً للخيارات»
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.TOP);
        titleRow.addView(title, new LinearLayout.LayoutParams(-2, -2));
        optionsHint = text(16, "sans-serif-medium", Color.WHITE);   // 18 sp in the name's own colour: at 14 sp and 85 % in the soft tone it read as a rendering bug (step 28)
        optionsHint.setText(S.tv(OPTIONS_HINT));
        optionsHint.setSingleLine(true);
        optionsHint.setAlpha(0f);
        optionsHint.setShadowLayer(dp(3), 0, dp(1), 0x66000000);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-2, -2);
        hp.setMarginStart(dp(16));
        titleRow.addView(optionsHint, hp);
        titleRow.setBaselineAligned(true);   // the hint stands on the name's own baseline (it sat 8 px higher and was clipped at the top, step 28)
        titleRow.setBaselineAlignedChildIndex(0);
        titleRow.setClipChildren(false);
        bottom.addView(titleRow, new LinearLayout.LayoutParams(-2, -2));
        ((LinearLayout.LayoutParams) titleRow.getLayoutParams()).setMargins(0, 0, 0, bar(FavBar.NAME_GAP));
        ((LinearLayout.LayoutParams) titleRow.getLayoutParams()).setMarginStart(bar(FavBar.PAD_SIDE));   // in line with the tray's inner padding, at its start edge
        nameRow = titleRow;
        titleRow.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> placeName());   // a new name, a new width: centred again

        // the favourites bar (docs/PROPOSALS-NOV2.md §1, FavBar): a tray 218 high, the same on every look and layout; five 304×170 tiles
        // show, the first on the right, and more scroll inside it; the row never leaves it
        dockRow = new DockGrid(this);
        dockRow.setOrientation(LinearLayout.HORIZONTAL);
        dockRow.setGravity(Gravity.CENTER);   // 4.4 (the owner): the row centred in every way; a long one fills the tray and scrolls with the focus
        dockRow.setLayoutDirection(Dir.layout());   // the first favourite on the right in Arabic, on the left in English (tester step 20)
        dockRow.setClipChildren(false);
        dockRow.setClipToPadding(false);
        dockRow.setPadding(bar(FavBar.PAD_SIDE), bar(FavBar.PAD_TOP), bar(FavBar.PAD_SIDE), bar(FavBar.PAD_TOP));
        GradientDrawable band = dockBand = new GradientDrawable();
        band.setCornerRadius(bar(FavBar.RADIUS));
        GlassStyle.Material bandNow = GlassStyle.material(lookNow, lightNow, GlassStyle.BAR);   // the home's one material (applyLook repaints it)
        band.setColor(bandNow.fill);
        band.setStroke(Math.max(1, bar(1)), bandNow.line);
        tray = new TraySnap(this);
        tray.setHorizontalScrollBarEnabled(false);
        tray.setFillViewport(true);
        tray.setClipToPadding(true);   // a tile scrolled half out is cut at the tray's edge, never drawn over the photo
        tray.setClipChildren(true);
        tray.setOutlineProvider(new android.view.ViewOutlineProvider() {   // build 180: a sixth tile was drawn outside the tray; everything the tray holds is cut to its rounded outline
            @Override public void getOutline(View v, android.graphics.Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), bar(FavBar.RADIUS)); }
        });
        tray.setClipToOutline(true);
        tray.setBackground(band);
        tray.addView(dockRow, new FrameLayout.LayoutParams(-2, -1));
        dockRow.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> { if (!trayFitQueued) { trayFitQueued = true; handler.post(this::fitTray); } });   // the bar follows its tiles
        tray.setOnScrollChangeListener((v, x, y, ox, oy) -> placeName());
        tray.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus, newFocus) -> tray.wholeOnly());   // the focused tile is the one that may stand partly out
        bottom.addView(tray, new LinearLayout.LayoutParams(-1, bar(FavBar.TRAY_H)));

        // «كمّل المشاهدة»: its own row below the first screen (the owner: the tray never moves for it), ▼ from the tray scrolls the home up
        // to it and ▲ brings the tray back; the tray's sides and padding, so its cards sit exactly under the favourites' cells
        tvRowsBox = new LinearLayout(this);
        tvRowsBox.setOrientation(LinearLayout.VERTICAL);
        tvRowsBox.setClipChildren(false);
        tvRowsBox.setClipToPadding(false);
        tvRowsBox.setPadding(bar(FavBar.PAD_SIDE), 0, bar(FavBar.PAD_SIDE), 0);   // the tray's own padding: its cells are the favourites' cells, a card a tile's size

        content.addView(tvRowsBox, llp(-1, -2, bar(FavBar.SIDE), 0, bar(FavBar.SIDE), 0));

        appsLabel = text(11, "sans-serif-medium", 0x99FFFFFF);
        appsLabel.setText(S.t("كل التطبيقات"));
        appsLabel.setTextColor(GOLD);
        content.addView(appsLabel, llp(-2, -2, dp(68), dp(8), dp(68), dp(18)));   // inset like the grid on both sides: in RTL it starts over the first tile

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        content.addView(grid, llp(-1, -2, dp(68), 0, dp(68), dp(160)));
        reminder = new ReminderCard(this, root);   // «المهام» with a time: its card is the topmost view
        reminder.under(new ReminderCard.Under() {   // under the clock column, never over it (the tester, step 37), and clear of the big clock's digits
            @Override public int bottom() {
                if (clockBlock == null || clockBlock.getHeight() == 0 || !clockBlock.isShown()) return -1;
                int[] at = new int[2];
                clockBlock.getLocationInWindow(at);
                return at[1] + (clockBlock instanceof ViewGroup ? shownBottom((ViewGroup) clockBlock) : clockBlock.getHeight());   // with the big clock the small one is INVISIBLE and the date lifted: its box ends far below
            }
            @Override public int[] bigClock() {
                if (!bigClockOn) return null;
                android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
                boolean beside = homeCard != null && homeCard.shownCards() > 0 && !homeCard.inRow();
                return ScenePlan.fixedBox(dm.widthPixels, dm.heightPixels, beside, Dir.rtl());
            }
            @Override public int floor() {
                if (tray == null || tray.getHeight() == 0 || !tray.isShown()) return -1;
                int[] at = new int[2];
                tray.getLocationInWindow(at);
                return at[1];
            }
        });
        return root;
    }

    /**
     * The WebView's box for that many cards, in pixels. One card is the size it always was. Two or three need room: a pair is
     * 58% of the screen wide (at 1920 that is 560 + 22 + 540), kept clear of the greeting and clock block at the right; the third
     * card goes under the pair. The box ends above the dock, or above the app title when the longest title of the favourites
     * would reach into it. Before the screen is laid out the numbers come from the screen's size alone.
     */
    private int[] homeCardBox(int cards, boolean row) {
        if (date != null) handler.post(this::applyLift);   // 4.4 beta 4 (run 28: the date beside the weather for the first minute): the cards placed, the date follows now, not at the next tick
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int clockLeft = 0, titleTop = 0, dockTop = 0, wide = 0;
        if (clockBlock != null && clockBlock.getWidth() > 0) clockLeft = ((View) clockBlock.getParent()).getLeft() + clockBlock.getLeft();
        // English: the clock is at the left and the card at the right, so the same gap measured from the right edge
        if (!Dir.rtl() && clockBlock != null && clockBlock.getWidth() > 0) clockLeft = dm.widthPixels - ((View) clockBlock.getParent()).getLeft() - clockBlock.getRight();
        if (bottomBlock != null && bottomBlock.getHeight() > 0 && dockRow != null) {
            titleTop = bottomBlock.getTop();
            dockTop = titleTop + tray.getTop();   // the tray's top (FavBar): the cards above end before it
            for (View c : dockCards) wide = Math.max(wide, Math.round(title.getPaint().measureText(((App) c.getTag()).label)));
        }
        if (row && homeCard != null && homeCard.tvos()) return CardBox.tvosRow(dm.widthPixels, dm.heightPixels, titleTop, getResources().getConfiguration().fontScale);   // «tvOS»: above the bar, its width
        if (row) return CardBox.row(dm.widthPixels, dm.heightPixels, dm.density, titleTop, bigClockOn ? ScenePlan.fixedBox(dm.widthPixels, dm.heightPixels, false, Dir.rtl())[3] : -1);
        return CardBox.box(cards, dm.widthPixels, dm.heightPixels, dm.density, clockLeft, titleTop, dockTop, wide);
    }

    private View buildHeader() {
        FrameLayout header = new FrameLayout(this);
        header.setClipChildren(false);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        // Top right (top left in English), stacked tight: greeting, clock, date. Dir.start(), not START: the views' own direction is not always ours.
        greeting = text(15, "medium", GOLD);
        greeting.setSingleLine(true);
        // the connection is a small status next to the greeting on its line (not a button: it takes no focus, a press does nothing)
        netIcon = new ImageView(this);
        netIcon.setImageResource(R.drawable.ic_wifi);
        netIcon.setContentDescription(S.t("الاتصال بالشبكة"));
        netIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout greetRow = new LinearLayout(this);
        greetRow.setOrientation(LinearLayout.HORIZONTAL);
        greetRow.setGravity(Gravity.CENTER_VERTICAL);
        greetRow.addView(greeting, new LinearLayout.LayoutParams(-2, -2));
        greetRow.addView(netIcon, sideLlp(dp(NET_ICON), dp(NET_ICON), dp(8), 0, 0, 0));
        left.addView(greetRow, sideLlp(-2, -2, dp(4), 0, 0, 0));
        // under it: the weather of the city and the next sunrise or sunset (shown only when the greeting is on and there is an answer)
        weatherLine = text(12, "regular", 0xFFC6BBA4);
        weatherLine.setMaxLines(2);
        weatherLine.setVisibility(View.GONE);
        left.addView(weatherLine, sideLlp(-2, -2, dp(4), dp(1), 0, 0));
        weatherLine.addOnLayoutChangeListener((nv, n1, n2, n3, n4, n5, n6, n7, n8) -> keepNudgeClear());   // 4.4: the nudge strip keeps NUDGE_CLEAR_PX from it
        greetingLine = new Greeting(this, greeting, weatherLine, () -> resumed);

        LinearLayout clockRow = new LinearLayout(this);
        clockRow.setGravity(Gravity.BOTTOM);
        clockRow.setGravity(Gravity.TOP);
        clock = text(58, "display", 0xFFFFFDF6);
        clock.setIncludeFontPadding(false);
        ampm = text(16, "medium", GOLD);
        // the row reads from the right: the time first, then the ص or the م on its left
        clockRow.addView(clock);
        clockRow.addView(ampm, sideLlp(-2, -2, dp(8), dp(6), 0, 0));
        left.addView(clockRow, llp(-2, -2, 0, dp(-2), 0, 0));

        date = text(14, "regular", 0xFFC6BBA4);
        date.setSingleLine(true);
        left.addView(date, sideLlp(-2, -2, dp(4), dp(-6), 0, 0));   // the display font leaves a deep gap under its digits
        // «بتوقيت دبي» (with «· حساب الخليج» when the calculation is not Umm al-Qura), when the city's clock is not the TV's or the
        // calculation is not Umm al-Qura: ClockZone.showLine
        zoneLine = text(12, "regular", GOLD);
        zoneLine.setSingleLine(true);
        zoneLine.setVisibility(View.GONE);
        left.addView(zoneLine, sideLlp(-2, -2, dp(4), dp(1), 0, 0));
        prayerLine = text(14, "regular", 0xFFC6BBA4);   // «UrTV النظيف»: the next prayer, one line under the date (only while the mask is on and the tick is kept)
        prayerLine.setSingleLine(true);
        prayerLine.setVisibility(View.GONE);
        left.addView(prayerLine, sideLlp(-2, -2, dp(4), dp(2), 0, 0));
        for (TextView t : new TextView[]{greeting, weatherLine, clock, ampm, date, zoneLine, prayerLine}) t.setShadowLayer(dp(3), 0, dp(1), 0x80000000);
        left.setGravity(Dir.start());
        for (TextView t : new TextView[]{greeting, weatherLine, date, zoneLine, prayerLine}) t.setGravity(Dir.start());
        header.addView(left, new FrameLayout.LayoutParams(-2, -2, Dir.start() | Gravity.TOP));
        clockBlock = left;

        final PillBar right = new PillBar(this);   // the top row: one bar behind its buttons (design/43)
        pillBar = right;
        right.addOnLayoutChangeListener((nv, n1, n2, n3, n4, n5, n6, n7, n8) -> keepNudgeClear());   // 4.4: the nudge strip keeps NUDGE_CLEAR_PX from the opening row
        right.setGravity(Gravity.CENTER_VERTICAL);
        right.setClipChildren(false);
        Pill inputs = pill(S.t("المصادر"), R.drawable.ic_input);
        inputsPill = inputs;
        inputs.setOnClickListener(v -> showInputs(v, ParentalPolicy.PILL_INPUTS));
        Pill tasks = pill(S.t("المهام"), R.drawable.ic_mic);   // the list lives in the screensaver app
        tasks.setOnClickListener(v -> launchGuarded(ParentalPolicy.PILL_TASKS, new Intent(this, TasksActivity.class), "المهام"));
        // the «تلاوة وأذان» pill: the Quran, the radio and the adhan are part of this app, so it opens them right here
        Pill tilawah = pill(S.t("تلاوة وأذان"), R.drawable.ic_listen);
        tilawah.setOnClickListener(v -> launchGuarded(ParentalPolicy.PILL_LISTEN, new Intent(this, sa.urtv.home.tilawah.MainActivity.class), "تلاوة وأذان"));
        // ---- «مركز التحكم» (ControlCenter): the sleep timer, the screensaver, the network and the TV's settings pills went into its panel
        control = new ControlCenter(this, controlHost());
        centerPill = pill(ControlText.NAME, R.drawable.ic_center);
        centerPill.setOnClickListener(v -> {
            if (cleanShown) openOurSettings(false);   // «UrTV النظيف»: the one button that stays is the settings
            else guarded(ParentalPolicy.PILL_CENTER, null, ControlText.NAME, () -> control.open(v));
        });
        // 4.4 beta 3: kids mode shows its way out where the control centre was: «الرقابة الأبوية» (it asks the PIN when one is set)
        kidsExitPill = pill(S.t("الخروج من وضع الأطفال"), R.drawable.ic_lock);
        kidsExitPill.setOnClickListener(v -> openParental());
        // ---- end «مركز التحكم»
        // «الأسواق» and «المباريات»: the markets page and the games page, two more round buttons (step 27, the owner's TV photos)
        Pill marketsBtn = pill(S.t("الأسواق"), R.drawable.ic_markets);
        marketsPill = marketsBtn;
        marketsBtn.setOnClickListener(v -> { if (!cleanShown) guarded(ParentalPolicy.WIDGET_MATCHES, null, S.t("الأسواق"), () -> launch(new Intent(this, MarketsActivity.class))); });   // our own page: no app of the lists
        Pill matchesBtn = pill(S.t("المباريات"), R.drawable.ic_matches);
        matchesPill = matchesBtn;
        matchesBtn.setOnClickListener(v -> { if (!cleanShown) launchGuarded(ParentalPolicy.WIDGET_MATCHES, new Intent(this, MatchesActivity.class), "المباريات"); });
        // «الجوال» (C14): a round button to the phone remote's page (the QR and the pairing), after «المباريات»
        phonePill = pill(S.t("الجوال"), R.drawable.ic_phone);
        phonePill.setOnClickListener(v -> launchGuarded(ParentalPolicy.OUR_SETTINGS, new Intent(this, PhoneActivity.class), "الجوال"));
        // 4.3.3 «بحث»: in the row only while the settings' «زر البحث» is on and the TV has a search inside the apps (applyQuickPill)
        searchPill = pill(S.t("بحث"), R.drawable.ic_search);
        searchPill.setOnClickListener(v -> {
            Intent s = GlobalSearch.intent(getPackageManager());
            if (s != null && !Parental.state(this).kids) launchGuarded(ParentalPolicy.TILE, s, "بحث");   // locked as the search app's own tile
        });
        // 4.3: the update screen's pill, in the row only while UpdateNotice says so (the screensaver is older than this home, or a newer build is out)
        updatePill = pill(S.t("تحديث جديد"), R.drawable.ic_update);
        updatePill.setAccent(true);
        updatePill.setOnClickListener(v -> launchGuarded(ParentalPolicy.PILL_UPDATES, new Intent(this, UpdateActivity.class), "تحديث تلفزيوني"));
        List<Pill> row = new ArrayList<>(Arrays.asList(inputs));
        tasksPill = tasks;
        if (isInstalled(SAVER_PKG) && ConfigSnapshot.ui(this).tasksPillOn()) row.add(tasks);   // «زر المهام»: hidden in the screensaver's settings, it is not in the row
        if (centerWanted()) row.add(centerPill);
        row.add(marketsBtn);
        row.add(matchesBtn);
        row.add(phonePill);
        tilawahPill = tilawah;
        pillRow = right;
        // [updates bell] built here, put in the row by applyUpdateBell only while it has a reason to be there
        bellPill = pill(S.t("تحديثات التطبيقات"), R.drawable.ic_bell);
        bellPill.setOnClickListener(v -> guarded(ParentalPolicy.PILL_UPDATES, UpdatesBell.PLAY_STORE, S.t("متجر Play"), this::openUpdates));
        // [/updates bell]
        if (listenPillWanted(ConfigSnapshot.ui(this).listenPillOn)) row.add(tilawah);   // «زر تلاوة وأذان»: hidden in the screensaver's settings, it is simply not in the row
        for (Pill p : row) {
            right.addView(p, sideLlp(-2, dp(Pill.HEIGHT), 0, 0, dp(Pill.GAP), 0));
            pills.add(p);
        }
        // «يُتلى الآن»: the first child of the row, so it sits at its right end and the pills keep their places when it comes
        nowCard = new NowCard(this, font("medium"), font("regular"), getDrawable(R.drawable.ic_play), getDrawable(R.drawable.ic_pause));
        nowCard.setId(View.generateViewId());
        nowCard.setVisibility(View.GONE);
        nowCard.setOnClickListener(v -> NowPlaying.toggle(this));   // OK: pause or play
        nowCard.setOnLongClickListener(v -> { launchGuarded(ParentalPolicy.PILL_LISTEN, NowPlaying.player(this), "تلاوة وأذان"); return true; });   // a long OK: the player, behind the same lock as the «تلاوة وأذان» pill
        nowCard.setOnKeyListener((v, code, ev) -> {   // 4.4 #62: the menu key on the card: «شاشة سوداء», behind the same lock
            if (code != KeyEvent.KEYCODE_MENU) return false;
            if (ev.getAction() == KeyEvent.ACTION_UP) launchGuarded(ParentalPolicy.PILL_LISTEN, sa.urtv.home.tilawah.BlackScreenActivity.open(this), "تلاوة وأذان");
            return true;
        });
        nowCard.setOnFocusChangeListener((v, has) -> {
            if (has) { lastFocusedId = v.getId(); glideTo(0); fadeGlow(1f); }
        });
        // taller than a pill by 10 dp, hung 5 dp over the row's top and bottom: the row stays Pill.HEIGHT, so the pills do not move; it
        // reaches from 31 to 75 dp down, clear of the adhan bar (to 26) and the cards (from 84)
        int hang = dp((NowCard.HEIGHT - Pill.HEIGHT) / 2f);
        right.addView(nowCard, 0, sideLlp(-2, dp(NowCard.HEIGHT), 0, -hang, dp(Pill.GAP), -hang));
        right.mains(centerPill, phonePill, kidsExitPill);   // the row is closed to these until the focus comes in (kids mode: its way out always shows)
        header.addView(right, new FrameLayout.LayoutParams(-2, -2, Dir.end() | Gravity.TOP));
        return header;
    }

    /** «تلاوة وأذان» opened on its adhan tab: the adhan's settings, the same screen and the same prefs as the player's. */
    private Intent adhanSettingsIntent() {
        return new Intent(this, sa.urtv.home.tilawah.MainActivity.class).putExtra(sa.urtv.home.tilawah.MainActivity.EXTRA_OPEN, "adhan");
    }

    /**
     * The screensaver's settings row «الأذان» brings the home to the front with open=adhan: the adhan settings open from here, behind the same
     * family lock as the «تلاوة وأذان» button (it asks for the PIN when that is locked), whether the button is shown or hidden.
     * The same way: open=listen («تلاوة وأذان» itself) and open=center («مركز التحكم», behind its pill's lock). An unknown word does nothing.
     */
    private void openRequested(Intent i) {
        String what = i == null ? null : i.getStringExtra("open");
        if (what == null) return;
        i.removeExtra("open");
        switch (what) {
            case "adhan":
                if (getResources().getBoolean(R.bool.show_listening)) handler.post(() -> launchGuarded(ParentalPolicy.PILL_LISTEN, adhanSettingsIntent(), S.t("إعدادات الأذان")));
                break;
            case "photos":   // the settings' «أضف من فلاشة أو التنزيلات» under «صوري»: the photos of a stick or of Downloads, added to «صوري»
                handler.post(() -> launchGuarded(ParentalPolicy.PILL_SAVER_SETTINGS, new Intent(this, PhotoAddActivity.class), S.t("أضف من فلاشة أو التنزيلات")));   // behind the lock of the screensaver's settings
                break;
            case "voice": case "duas": case "sounds": {   // the settings' «المؤذن», «الأدعية والتذكيرات», «صوتياتي»: the تلاوة app's screens, behind the same lock as the adhan's settings
                final String cls = "voice".equals(what) ? "AdhanActivity" : "duas".equals(what) ? "DuaActivity" : "AudioActivity";
                final String shown = "voice".equals(what) ? S.t("المؤذن") : "duas".equals(what) ? S.t("الأدعية والتذكيرات") : S.t("صوتياتي");
                if (getResources().getBoolean(R.bool.show_listening))
                    handler.post(() -> launchGuarded(ParentalPolicy.PILL_LISTEN, new Intent().setClassName(getPackageName(), "sa.urtv.home.tilawah." + cls), shown));
                break;
            }
            case "prayers":   // the settings' «الأذان لكل صلاة»: sound and screen for each of the five, behind the same lock as the adhan's settings
                if (getResources().getBoolean(R.bool.show_listening))
                    handler.post(() -> launchGuarded(ParentalPolicy.PILL_LISTEN, new Intent(this, sa.urtv.home.tilawah.PrayerSoundActivity.class), S.t("الأذان لكل صلاة")));
                break;
            case "ownadhan":   // 4.4 beta 4 the settings' «أذاني من ملف» (the emulator's count: inside «المؤذن» it was ~37 presses): «صوتياتي» asking for the file that becomes the adhan
                if (getResources().getBoolean(R.bool.show_listening))
                    handler.post(() -> launchGuarded(ParentalPolicy.PILL_LISTEN, new Intent(this, sa.urtv.home.tilawah.AudioActivity.class)
                        .putExtra(sa.urtv.home.tilawah.AudioActivity.EXTRA_USE, "own"), S.t("أذاني من ملف")));
                break;
            case "listen":   // the settings' «تلاوة وأذان» ← «افتح تلاوة وأذان»: the same as the button
                if (getResources().getBoolean(R.bool.show_listening))
                    handler.post(() -> launchGuarded(ParentalPolicy.PILL_LISTEN, new Intent(this, sa.urtv.home.tilawah.MainActivity.class), "تلاوة وأذان"));
                break;
            case "center":   // the settings' «مركز التحكم» button: the panel, behind the same lock as its pill; the focus goes back to the pill
                if (control != null && centerPill != null && !Parental.state(this).kids) {   // kids mode has no pill, so no panel either
                    final String ccview = i.getStringExtra("ccview");   // DebugControlShot only: an item or "custom" to start on
                    i.removeExtra("ccview");
                    handler.post(() -> guarded(ParentalPolicy.PILL_CENTER, null, ControlText.NAME, () -> control.open(centerPill, ccview)));
                }
                break;
            default:
                break;
        }
    }

    /**
     * The phone remote asks for something (PhoneApps.EXTRA_LAUNCH): an app's package or a pill's name. It goes through the very guard a tap
     * goes through, so a locked one asks for the PIN here, on the TV; the request is only acted on for what the phone may list (an app of the
     * favourites row, one of the pills) and never twice (the extra is taken off the intent).
     */
    private void phoneRequested(Intent i) {
        if (i == null) return;
        final String t = i.getStringExtra(PhoneApps.EXTRA_LAUNCH);
        if (t == null) return;
        i.removeExtra(PhoneApps.EXTRA_LAUNCH);
        if (t.startsWith("@") && !"@parental".equals(t) && Parental.state(this).kids) return;   // kids mode shows no pills: the phone cannot open one either (its way out it can)
        handler.post(() -> {
            switch (t) {
                case "@parental": openParental(); break;   // 4.4 beta 3: kids mode's way out from the phone; the PIN is asked on the TV
                case "@tasks": launchGuarded(ParentalPolicy.PILL_TASKS, new Intent(this, TasksActivity.class), "المهام"); break;
                case "@saver": guarded(ParentalPolicy.PILL_SAVER, SAVER_PKG, "شاشة التوقف", this::startSaver); break;
                case "@inputs": if (inputsPill != null) showInputs(inputsPill.getParent() == pillRow || pills.isEmpty() ? inputsPill : pills.get(0), ParentalPolicy.PILL_INPUTS); break;   // «زر المصادر» hidden: the list from the first button
                case "@settings": launchGuarded(ParentalPolicy.PILL_TV_SETTINGS, new Intent(Settings.ACTION_SETTINGS), S.t("إعدادات التلفزيون")); break;
                case "@listen":
                    if (getResources().getBoolean(R.bool.show_listening)) launchGuarded(ParentalPolicy.PILL_LISTEN, new Intent(this, sa.urtv.home.tilawah.MainActivity.class), "تلاوة وأذان");
                    break;
                default: {
                    boolean shown = false;
                    for (String[] a : PhoneApps.apps(this)) shown |= a[0].equals(t);
                    if (shown) guarded(ParentalPolicy.REMOTE_APP, t, labelOf(t), () -> openPackage(t));
                }
            }
        });
    }

    /** TypeService.openTvSettings: the remote's Settings key was pressed in another app and the TV's settings are behind the PIN: asked here. */
    static final String EXTRA_SETTINGS_KEY = "sa.urtv.home.SETTINGS_KEY";

    /**
     * «أزرار الريموت» over another app: the add-on found the button's app behind the family PIN and opened the home with it; asked here, as for its tile,
     * then opened. Only an app the owner gave one of this remote's buttons (an intent with any other package does nothing).
     */
    private void keyAppRequested(Intent i) {
        final String pkg = i == null ? null : i.getStringExtra(ExtrasWords.EXTRA_KEY_APP);
        if (pkg == null) return;
        i.removeExtra(ExtrasWords.EXTRA_KEY_APP);
        if (!KeyApps.chosen(pkg) || !Dock.keyApps(this).containsValue(pkg) || !ParentalPolicy.visible(Parental.state(this), pkg)) return;
        handler.post(() -> guarded(ParentalPolicy.REMOTE_APP, pkg, labelOf(pkg), () -> openPackage(pkg)));
    }

    private void settingsKeyRequested(Intent i) {
        if (i == null || !i.getBooleanExtra(EXTRA_SETTINGS_KEY, false)) return;
        i.removeExtra(EXTRA_SETTINGS_KEY);
        final Intent tv = Extras.tvSettingsIntent(this);
        handler.post(() -> launchGuarded(ParentalPolicy.REMOTE_SETTINGS, tv, "إعدادات التلفزيون"));
    }

    /** The button is wanted: the screensaver's setting says shown, and this build has the listening part at all. */
    private boolean listenPillWanted(boolean setting) { return setting && getResources().getBoolean(R.bool.show_listening); }

    /** This build has the listening part, but its button is hidden by the setting: the app menu then has a way in. */
    private boolean listenPillHidden() {
        return getResources().getBoolean(R.bool.show_listening) && (tilawahPill == null || tilawahPill.getParent() != pillRow);
    }

    /**
     * «زر تلاوة وأذان» shown or hidden by the setting, while the screen is up (the minute tick). Hiding takes the button OUT of the row (a
     * hidden view would stop the walk along the row, which needs every sibling visible) and out of `pills`; the focus moves to the pill before
     * it. It comes back at the end of the row, where it always was. Nothing else changes: the adhan, its bar, the card and the Quran stay.
     */
    private void applyListenPill(boolean setting) {
        if (tilawahPill == null || pillRow == null) return;
        boolean want = listenPillWanted(setting), has = tilawahPill.getParent() == pillRow;
        if (want == has) return;
        if (want) {
            pillRow.addView(tilawahPill, sideLlp(-2, dp(Pill.HEIGHT), 0, 0, dp(Pill.GAP), 0));
            pills.add(tilawahPill);
        } else {
            boolean hadFocus = tilawahPill.hasFocus();
            pills.remove(tilawahPill);
            pillRow.removeView(tilawahPill);
            if (hadFocus && !pills.isEmpty()) pills.get(pills.size() - 1).requestFocus();
        }
        wireFocus();   // the row changed: up/down from the pills follow it
    }

    /**
     * «زر المهام» shown or hidden by the screensaver's setting, while the screen is up (the minute tick), the way «زر تلاوة وأذان» is: taken
     * OUT of the row and out of `pills` (never just hidden: the walk along the row needs every sibling visible), the focus to the pill before
     * it; it comes back where it always was, right after «المصادر». Only with the screensaver app here (the list lives in it). The tasks, the
     * «المهام» card and the phone's way in do not change.
     */
    private void applyTasksPill(boolean setting) {
        if (tasksPill == null || pillRow == null || inputsPill == null) return;
        boolean has = tasksPill.getParent() == pillRow;
        if (setting == has) return;   // as it should be: the package manager is not asked every minute
        boolean want = setting && isInstalled(SAVER_PKG);
        if (want == has) return;
        if (want) {
            boolean inputsIn = inputsPill.getParent() == pillRow;   // «زر المصادر» hidden: «المهام» first, after «يُتلى الآن» (child 0)
            pillRow.addView(tasksPill, inputsIn ? pillRow.indexOfChild(inputsPill) + 1 : 1, sideLlp(-2, dp(Pill.HEIGHT), 0, 0, dp(Pill.GAP), 0));
            pills.add(inputsIn ? pills.indexOf(inputsPill) + 1 : 0, tasksPill);
        } else {
            boolean hadFocus = tasksPill.hasFocus();
            pills.remove(tasksPill);
            pillRow.removeView(tasksPill);
            if (hadFocus) { if (inputsPill.getParent() == pillRow) inputsPill.requestFocus(); else if (!pills.isEmpty()) pills.get(0).requestFocus(); }
        }
        wireFocus();   // the row changed: up/down from the pills follow it
    }

    // [updates bell]
    /**
     * «جرس التحديثات» in or out of the row, with the minute tick (and so on every return to the front); the source is asked on a return and
     * at most every 6 hours after that (UpdatesBell), and only while the setting is on, the store is here and may be opened, and it is not
     * kids mode. Like the «تلاوة» button it is taken out of the row and out of `pills`, never just hidden, so the walk along the row and
     * wireFocus stay right; it joins at the end of the row.
     */
    private void applyUpdateBell(boolean setting) {
        if (bellPill == null || pillRow == null) return;
        boolean force = bellForce;
        bellForce = false;
        ParentalPolicy.State family = Parental.state(this);
        boolean usable = !family.kids && ParentalPolicy.visible(family, UpdatesBell.PLAY_STORE) && isInstalled(UpdatesBell.PLAY_STORE);
        UpdatesBell.Evidence e = setting && usable ? updatesBell.evidence(force, SystemClock.elapsedRealtime()) : UpdatesBell.Evidence.UNKNOWN;
        boolean want = UpdatesBell.shown(setting, family.kids, usable, e), has = bellPill.getParent() == pillRow;
        if (want == has) return;
        if (want) {
            pillRow.addView(bellPill, sideLlp(-2, dp(Pill.HEIGHT), 0, 0, dp(Pill.GAP), 0));
            pills.add(bellPill);
        } else {
            boolean hadFocus = bellPill.hasFocus();
            pills.remove(bellPill);
            pillRow.removeView(bellPill);
            if (hadFocus && !pills.isEmpty()) pills.get(pills.size() - 1).requestFocus();
        }
        wireFocus();
    }

    // ---- «مركز التحكم»: the pill's row membership and what the panel may ask of this screen

    /** Kids mode has no «مركز التحكم»: neither its pill nor its panel (the other pills' functions stay behind their locks, outside kids mode). */
    private boolean centerWanted() { return !Parental.state(this).kids && !hiddenQuick.contains("center"); }

    /** the quick buttons hidden in the settings (#52), read with the minute tick */
    private final java.util.Set<String> hiddenQuick = new java.util.HashSet<>();

    /**
     * The pill comes into the row or leaves it as kids mode changes (the minute tick and a return to the home): it goes out of the row and out
     * of `pills` like the «تلاوة وأذان» one (a hidden view would stop the walk), before the «تلاوة وأذان» pill when that is in the row.
     */
    private void applyCenterPill() {
        if (centerPill == null || pillRow == null) return;
        applyKidsExit();
        boolean want = centerWanted(), has = centerPill.getParent() == pillRow;
        if (want == has) return;
        if (want) {
            int at = quickIndex(marketsPill, matchesPill, phonePill, updatePill, tilawahPill);   // before «الأسواق» and «المباريات» (and «تلاوة وأذان»): where it always was
            pills.add(at, centerPill);
            pillRow.addView(centerPill, at + 1, sideLlp(-2, dp(Pill.HEIGHT), 0, 0, dp(Pill.GAP), 0));   // child 0 is «يُتلى الآن»
        } else {
            control.dismiss();
            boolean hadFocus = centerPill.hasFocus();
            int at = pills.indexOf(centerPill);
            pills.remove(centerPill);
            pillRow.removeView(centerPill);
            if (hadFocus && !pills.isEmpty()) pills.get(Math.max(0, Math.min(at, pills.size()) - 1)).requestFocus();
        }
        wireFocus();
    }

    /** 4.4 beta 3: kids mode's way out is a button in the row while kids mode is on (never a dead end); gone when it is off */
    private void applyKidsExit() {
        if (kidsExitPill == null || pillRow == null) return;
        boolean want = Parental.state(this).kids, has = kidsExitPill.getParent() == pillRow;
        if (want == has) return;
        if (want) {
            pills.add(0, kidsExitPill);
            pillRow.addView(kidsExitPill, 1, sideLlp(-2, dp(Pill.HEIGHT), 0, 0, dp(Pill.GAP), 0));   // the first button (child 0 is «يُتلى الآن»)
        } else {
            boolean hadFocus = kidsExitPill.hasFocus();
            pills.remove(kidsExitPill);
            pillRow.removeView(kidsExitPill);
            if (hadFocus && !pills.isEmpty()) pills.get(0).requestFocus();
        }
        wireFocus();
    }

    private static final int NET_ICON = 22, NET_OFFLINE = 0xFFFF6B5E;   // dp; the offline colour
    private int netShown = -1, netInk;   // what the Wi-Fi mark shows now

    /** C15: the Wi-Fi icon says whether the TV is online (a wired TV counts as online too), with the minute tick */
    private void updateNetIcon() {
        if (netIcon == null) return;
        if (DebugFlags.on(this, "nowifimark")) { netIcon.setVisibility(View.GONE); return; }   // a tester's switch
        boolean online = false;
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            android.net.NetworkCapabilities nc = cm == null || cm.getActiveNetwork() == null ? null : cm.getNetworkCapabilities(cm.getActiveNetwork());
            online = nc != null && nc.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (RuntimeException ignored) { }
        int ink = online ? palette().soft : NET_OFFLINE;   // the look's secondary ink; red when the TV is off the network
        if (netShown == (online ? 1 : 0) && netInk == ink) return;   // unchanged: nothing is touched, nothing is redrawn (it runs with every tick)
        netShown = online ? 1 : 0; netInk = ink;
        netIcon.setImageResource(online ? R.drawable.ic_wifi : R.drawable.ic_wifi_off);
        netIcon.setColorFilter(ink);
        netIcon.setAlpha(online ? 1f : 0.85f);
    }

    /** where a quick button comes back in the row: before the first of the buttons that follow it which is in the row, else at the end */
    private int quickIndex(View... later) {
        int at = pills.size();
        for (View l : later) if (l != null && pills.contains(l)) at = Math.min(at, pills.indexOf(l));
        return at;
    }

    /** «الأسواق» and «المباريات» in or out of the row by the setting (#52): out of the row and out of `pills` (a hidden view would stop the walk), back before the buttons that follow them */
    private void applyQuickPill(View p, boolean want, View... later) {
        if (p == null || pillRow == null) return;
        boolean has = p.getParent() == pillRow;
        if (want == has) return;
        if (want) {
            int at = quickIndex(later);
            pills.add(at, p);
            pillRow.addView(p, at + 1, sideLlp(-2, dp(Pill.HEIGHT), 0, 0, dp(Pill.GAP), 0));   // child 0 is «يُتلى الآن»
        } else {
            boolean hadFocus = p.hasFocus();
            int at = pills.indexOf(p);
            pills.remove(p);
            pillRow.removeView(p);
            if (hadFocus && !pills.isEmpty()) pills.get(Math.max(0, Math.min(at, pills.size()) - 1)).requestFocus();
        }
        wireFocus();
    }

    /** OK on the bell: the Play Store's updates screen (its own «my apps» link), else the store itself. */
    private void openUpdates() {
        for (String uri : UpdatesBell.OPEN_URIS) {
            Intent i = uri == null ? getPackageManager().getLeanbackLaunchIntentForPackage(UpdatesBell.PLAY_STORE)
                    : new Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage(UpdatesBell.PLAY_STORE);
            if (i == null || (uri != null && getPackageManager().resolveActivity(i, 0) == null)) continue;
            if (launch(i)) return;
        }
    }
    // [/updates bell]

    private ControlCenter.Host controlHost() {
        return new ControlCenter.Host() {
            @Override public void launchGuarded(int path, Intent i, String name) { MainActivity.this.launchGuarded(path, i, name); }
            @Override public void openSaver() { guarded(ParentalPolicy.PILL_SAVER, SAVER_PKG, "شاشة التوقف", MainActivity.this::startSaver); }   // OK starts it, as optimize/start-saver.sh --preview does
            @Override public void openSaverSettings() { guarded(ParentalPolicy.PILL_SAVER_SETTINGS, SAVER_PKG, "شاشة التوقف", () -> openPackage(SAVER_PKG)); }   // a long OK: its settings
            @Override public void openOurSettings() { MainActivity.this.openOurSettings(false); }
            @Override public void openSettings() { openSettingsScreen(); }
            @Override public void openCity() { MainActivity.this.openCity(); }
            @Override public void openGames() { MainActivity.this.openOurSettings(false, "games"); }
            @Override public String updateLine() { return MainActivity.this.buildMismatchLine(); }
            @Override public void openUpdate() { launchGuarded(ParentalPolicy.PILL_UPDATES, new Intent(MainActivity.this, UpdateActivity.class), "تحديث تلفزيوني"); }
            @Override public void openSleep(View anchor) { guarded(ParentalPolicy.PILL_SLEEP, null, S.t("مؤقت النوم"), () -> showSleep(anchor)); }
            @Override public boolean saverInstalled() { return isInstalled(SAVER_PKG); }
            @Override public long[] adhanTimes() {   // 4.3.3 «الأذان والإقامة»: the adhan bar's own times
                if (lastCfg == null) return null;
                try {
                    sa.urtv.home.tilawah.DayParts d = sa.urtv.home.tilawah.DayParts.now(MainActivity.this, lastCfg.placeFragment(), lastCfg.placeTicket());
                    return new long[] {d.fajr, d.dhuhr, d.asr, d.maghrib, d.isha, d.friday ? 1 : 0};
                } catch (RuntimeException e) { return null; }
            }
            @Override public String clock(long ms) {
                String[] zone = lastCfg == null ? null : lastCfg.cityZone();
                Calendar at = ClockZone.at(ms, zone == null ? java.util.TimeZone.getDefault().getID() : zone[1]);
                return ClockText.time(at.get(Calendar.HOUR_OF_DAY), at.get(Calendar.MINUTE), Digits.clock24(), Digits.latin());
            }
            @Override public Typeface font(String family) { return MainActivity.this.font(family); }
        };
    }
    // ---- end «مركز التحكم»

    /** Shows or hides the «يُتلى الآن» card by what «تلاوة» is doing: a surah or a station (playing or paused), else nothing. */
    private void updateNowPlaying() {
        if (nowCard == null) return;
        if (cleanShown) { nowCard.setVisibility(View.GONE); return; }   // «UrTV النظيف»
        NowPlaying.State s = NowPlaying.read(this);
        long now = System.currentTimeMillis();
        nowPausedSince = NowCardRule.pausedSince(s.show, s.playing, nowPausedSince, now);   // 4.4 beta 4: paused for 10 minutes, the card goes (the clock's tick looks again every minute)
        if (!NowCardRule.visible(s.show, s.playing, nowPausedSince, now)) {
            if (nowCard.getVisibility() == View.VISIBLE && nowCard.hasFocus() && !pills.isEmpty()) pills.get(0).requestFocus();   // it went with the playback
            nowCard.setVisibility(View.GONE);
            return;
        }
        nowCard.bind(s);
        nowCard.setVisibility(View.VISIBLE);
    }

    /** Pill button: its icon only; the one with the focus widens to show its name (see Pill). */
    private Pill pill(String label, int icon) {
        Pill p = new Pill(this, label, icon, font("sans-serif-medium"));
        p.setId(View.generateViewId());
        p.setOnFocusChangeListener((v, has) -> {
            if (has) {
                lastFocusedId = v.getId(); lastPillId = v.getId(); glideTo(0); fadeGlow(1f);
                if (homeCard != null) for (View card : homeCard.targets()) card.setNextFocusUpId(v.getId());   // up from the «المهام» (or stocks) card goes back to the pill it came from
            }
        });
        return p;
    }

    // ---------------------------------------------------------------- apps

    /** One small thread loads the apps' names, banners and glow colours, so the first frame (the clock, the pills, the wallpaper) never waits for them. */
    private final java.util.concurrent.ExecutorService loader = java.util.concurrent.Executors.newSingleThreadExecutor();
    private int appsGen;   // each buildApps() is a new generation: an older load that finishes late is dropped
    private boolean appsShown, entranceDue;
    static final int BATCH_ROWS = 2;   // grid rows added per pass of the UI thread

    private int dockRetries;
    private final Runnable dockRetry = () -> { if (!isDestroyed() && !isFinishing()) buildApps(); };

    /** 4.5 the last buildApps' row is shown (HomeLook waits for it: a new way of the apps is never seen half built) */
    private int appsShownGen;
    private boolean appsSettled() { return appsShownGen == appsGen; }

    private void buildApps() {
        if (edit != null) abandonEdit();
        if (appsEdit != null) abandonAppsEdit();   // the grid it edits is stale: nothing saved, as «تعديل المفضلة»
        gridDone = false;
        PackageManager pm = getPackageManager();
        android.content.pm.ChangedPackages changed = pm.getChangedPackages(packageSeq);
        if (changed != null) { packageSeq = changed.getSequenceNumber(); artCache.evictAll(); }   // an app may have a new icon
        final int gen = ++appsGen;
        final String[] wanted = dockApps.clone();
        if (loader.isShutdown()) return;
        loader.execute(() -> {
            if (FavBar.ICONS.equals(appsLook)) loadFaces(getPackageManager());   // 4.4 beta 4: couch-tv's faces, once, here on the loader
            final List<App> all = loadApps(wanted);
            runOnUiThread(() -> { if (gen == appsGen && !isDestroyed()) showApps(all, wanted, gen); });
        });
    }

    /** Off the main thread: every launchable app with its label, art and colour (nothing here touches a view). */
    private List<App> loadApps(String[] wanted) {
        PackageManager pm = getPackageManager();
        List<App> out = new ArrayList<>();
        Map<String, String> given = AppNames.given(this);   // names the owner's agent set (dock.sh name)
        ParentalPolicy.State family = Parental.state(this);   // «وضع الأطفال»: only the allowed apps, when it is on
        for (ResolveInfo ri : Dock.catalogue(this)) {
            ActivityInfo ai = ri.activityInfo;
            if (!ParentalPolicy.visible(family, ai.packageName)) continue;
            App app = appOf(ri, pm, given);   // its art only for an app that shows
            if (FavBar.GLASS.equals(appsLook)) glassIcon(app, ri, pm);
            out.add(app);
        }
        for (String w : wanted) {   // an input the owner put in the favourites («أسماء المصادر»): a tile of its own (InputFav)
            App in = InputFav.isInput(w) ? inputApp(w) : null;
            if (in != null) out.add(in);
        }
        return out;
    }

    /**
     * A favourite input as a tile: its name (the owner's, else the CEC device's, else the TV's), the sources icon composed on a tile as an app
     * with no banner is. Null when the TV no longer reports it (it keeps its place, as an app that is gone for now). On the loading thread.
     */
    private App inputApp(String entry) {
        String id = InputFav.idOf(entry);
        TvInputInfo found = null;
        try {
            for (TvInputInfo t : InputLabels.reported(this)) if (InputLabels.key(t).equals(id)) { found = t; break; }
        } catch (RuntimeException e) {
            return null;
        }
        if (found == null) return null;
        App a = new App();
        a.pkg = entry;
        a.label = InputLabels.label(this, found, InputLabels.given(this));
        a.category = Intent.CATEGORY_LEANBACK_LAUNCHER;
        Drawable icon = getDrawable(R.drawable.ic_input);
        Drawable t = composeTile(icon);
        a.art = t != null ? t : icon;
        a.composed = t != null;
        if (FavBar.ICONS.equals(appsLook) || FavBar.GLASS.equals(appsLook)) {
            int side = Math.round(getResources().getDisplayMetrics().heightPixels * FavBar.ICON / 1080f * FavBar.ROOM_SCALE);
            Drawable q = composeTile(getDrawable(R.drawable.ic_input), side, side);
            a.icon = q != null ? q : icon;
            a.iconComposed = q != null;
        }
        a.color = sampleColor(entry, a.art);
        return a;
    }

    /** OK on a favourite input: that input, behind the same family lock as the «المصادر» button (it is the same switch). */
    private void openFavouriteInput(App app) {
        String id = InputFav.idOf(app.pkg);
        guarded(ParentalPolicy.PILL_INPUTS, null, app.label, () -> {
            for (TvInputInfo t : InputLabels.reported(this)) {
                if (!InputLabels.key(t).equals(id)) continue;
                if (t.getType() == TvInputInfo.TYPE_TUNER) openLiveTv(); else openPassthroughInput(t);
                return;
            }
            Toast.makeText(this, S.t("التلفزيون ما عاد يعرض هذا المصدر"), Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 4.5 «زجاجي»: the person's own icon (IconStore), else the app's icon as a smooth squircle (GlassArt), cached by package and the kept icon. Only
     * from loadApps, the loader's thread (a file and a render: never on the main thread; one app brought back on it shows in the grid, which has no glass).
     */
    private void glassIcon(App a, ResolveInfo ri, PackageManager pm) {
        String key = "g:" + a.pkg + ":" + IconStore.stamp(this, a.pkg);
        a.icon = artCache.get(key);
        if (a.icon != null) return;
        Bitmap own = IconStore.read(this, a.pkg);
        int size = FavBar.glassCell(getResources().getDisplayMetrics().heightPixels, 1.5f);   // made once at the biggest size, drawn smaller
        artCache.put(key, a.icon = new BitmapDrawable(getResources(), own != null ? own : GlassArt.appIcon(ri.loadIcon(pm), size)));
    }

    /** One app's name, art and colour (off the main thread for the whole list; on it for the one app «المخفية» brings back). */
    /** 4.4 beta 4: couch-tv's faces (CouchFaces) of the installed apps, decoded once on the apps' loader thread, never on the UI thread; appOf only reads them */
    private final java.util.Map<String, android.graphics.Bitmap> faces = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile boolean facesRead;

    private void loadFaces(PackageManager pm) {
        if (facesRead) return;
        facesRead = true;
        for (String[] f : CouchFaces.FACES) {
            try { pm.getPackageInfo(f[0], 0); } catch (PackageManager.NameNotFoundException e) { continue; }   // only an installed app
            try (java.io.InputStream in = getAssets().open(CouchFaces.asset(f[1]))) {
                android.graphics.Bitmap fb = android.graphics.BitmapFactory.decodeStream(in);
                if (fb != null) faces.put(f[0], fb);
            } catch (java.io.IOException | RuntimeException | OutOfMemoryError ignored) { }
        }
    }

    private App appOf(ResolveInfo ri, PackageManager pm, Map<String, String> given) {
        ActivityInfo ai = ri.activityInfo;
        App a = new App();
        a.pkg = ai.packageName;
        a.label = AppNames.label(this, ri, given);
        a.component = new ComponentName(ai.packageName, ai.name);
        a.category = Dock.isTv(ri) ? Intent.CATEGORY_LEANBACK_LAUNCHER : Intent.CATEGORY_LAUNCHER;
        Drawable banner = artCache.get("b:" + a.pkg);
        if (banner == null) {
            banner = ai.loadBanner(pm);
            if (banner == null) banner = ai.applicationInfo.loadBanner(pm);
            if (banner != null) artCache.put("b:" + a.pkg, banner = fitted(banner, true));
        }
        a.isBanner = banner != null;
        Drawable raw = null;
        if (banner != null) a.art = banner;
        else {
            a.art = artCache.get("i:" + a.pkg);
            if (a.art == null) artCache.put("i:" + a.pkg, a.art = fitted(raw = ri.loadIcon(pm), false));
        }
        if (FavBar.ICONS.equals(appsLook)) {   // B: the launcher icon even where there is a banner, cached and sized as the art (fitted)
            if (!a.isBanner) a.icon = a.art;
            else {
                a.icon = artCache.get("i:" + a.pkg);
                if (a.icon == null) artCache.put("i:" + a.pkg, a.icon = fitted(ri.loadIcon(pm), false));
            }
            // 4.4 beta 2 (the owner, build 570: small logos in squares, wide ones squeezed): the icon composed on its own square by TileArt's rules,
            // as the tiles are (an adaptive icon's layers edge to edge, a legacy one trimmed and large on its edge colour), drawn full-bleed
            Drawable q = artCache.get("q:" + a.pkg);
            if (q == null) { android.graphics.Bitmap fb = faces.get(a.pkg); if (fb != null) artCache.put("q:" + a.pkg, q = new BitmapDrawable(getResources(), fb)); }   // 4.4 beta 4: couch-tv's face (decoded on the loader: loadFaces)
            if (q == null) {
                int side = Math.round(getResources().getDisplayMetrics().heightPixels * FavBar.ICON / 1080f * FavBar.ROOM_SCALE);
                q = composeTile(sharpIcon(ri, pm), side, side);
                if (q != null) artCache.put("q:" + a.pkg, q);
            }
            if (q != null) { a.icon = q; a.iconComposed = true; }
        }
        a.color = sampleColor(a.pkg, a.art);
        if (!a.isBanner) {   // 4.4 beta 2 (the owner's test of 525): its own icon, large, on a neutral tile; never a dark tint of its colour
            Drawable t = artCache.get("t:" + a.pkg);
            if (t == null) {
                t = composeTile(sharpIcon(ri, pm));
                if (t != null) artCache.put("t:" + a.pkg, t);
            }
            if (t != null) { a.art = t; a.composed = true; }
        }
        return a;
    }

    /**
     * An app with no banner: its icon filling a 16:9 tile of the focused size, by TileArt's rules (an adaptive icon's background over the whole tile
     * and its foreground large in the middle; a legacy icon trimmed, fitted large, on its own edge colour, or on LIGHT or DARK when its edge is bare).
     * On the apps' loading thread; null (the old way) when it cannot be drawn.
     */
    private Drawable composeTile(Drawable icon) {
        int w = Math.round(getResources().getDisplayMetrics().widthPixels / (float) MIN_DOCK * 1.12f);
        return composeTile(icon, w, w * 9 / 16);
    }

    /** the same rules on a tile of w x h (a square for «أيقونات»: the owner, build 570, «the square icons are not nice») */
    private Drawable composeTile(Drawable icon, int w, int h) {
        if (icon == null || w <= 0 || h <= 0) return null;
        try {
            Bitmap tile = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(tile);
            if (icon instanceof android.graphics.drawable.AdaptiveIconDrawable) {   // the app's own layers, edge to edge
                android.graphics.drawable.AdaptiveIconDrawable ad = (android.graphics.drawable.AdaptiveIconDrawable) icon;
                Drawable bg = ad.getBackground(), fg = ad.getForeground();
                if (bg != null) { bg.setBounds(0, (h - w) / 2, w, (h - w) / 2 + w); bg.draw(c); }   // a square layer covering the 16:9 tile
                else c.drawColor(TileArt.LIGHT);
                int[] box = TileArt.box(TileArt.ADAPTIVE, w, h, 0, 0);
                if (fg != null) { fg.setBounds((w - box[0]) / 2, (h - box[1]) / 2, (w + box[0]) / 2, (h + box[1]) / 2); fg.draw(c); }
                return new BitmapDrawable(getResources(), tile);
            }
            int iw = icon.getIntrinsicWidth(), ih = icon.getIntrinsicHeight();
            Drawable cut = iw > 0 && ih > 0 ? trimmed(icon, iw, ih) : null;   // its transparent padding off first
            if (cut != null) { icon = cut; iw = icon.getIntrinsicWidth(); ih = icon.getIntrinsicHeight(); }
            int[] box = TileArt.box(TileArt.kind(false, false, iw, ih), w, h, iw, ih);
            Bitmap glyph = Bitmap.createBitmap(Math.max(1, box[0]), Math.max(1, box[1]), Bitmap.Config.ARGB_8888);
            icon.setBounds(0, 0, box[0], box[1]);
            icon.draw(new Canvas(glyph));
            int gw = glyph.getWidth(), gh = glyph.getHeight();
            // its own edge: the outermost pixels of the trimmed icon (a native-looking tile); a bare glyph: the backing it stands out from
            int[] edge = new int[2 * gw + 2 * gh];
            int k = 0;
            for (int x = 0; x < gw; x++) { edge[k++] = glyph.getPixel(x, 0); edge[k++] = glyph.getPixel(x, gh - 1); }
            for (int y = 0; y < gh; y++) { edge[k++] = glyph.getPixel(0, y); edge[k++] = glyph.getPixel(gw - 1, y); }
            int bg = TileArt.edgeColour(edge);
            if (bg == 0) {
                double lum = 0; int n = 0, all = 0;
                int step = Math.max(1, Math.min(gw, gh) / 24);
                for (int y = 0; y < gh; y += step) for (int x = 0; x < gw; x += step) {
                    int p = glyph.getPixel(x, y);
                    all++;
                    if (Color.alpha(p) < 128) continue;
                    lum += TileArt.lum(p); n++;
                }
                bg = TileArt.backing(n == 0 ? -1f : (float) (lum / n), all == 0 ? 0f : n / (float) all);
            }
            c.drawColor(bg);
            c.drawBitmap(glyph, (w - gw) / 2f, (h - gh) / 2f, new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG | android.graphics.Paint.ANTI_ALIAS_FLAG));
            glyph.recycle();
            return new BitmapDrawable(getResources(), tile);
        } catch (RuntimeException | OutOfMemoryError e) {
            return null;
        }
    }

    /** a legacy icon cut to its opaque pixels (TileArt.trim decides), or null when it has no margin worth cutting */
    private Drawable trimmed(Drawable icon, int iw, int ih) {
        float k = Math.min(1f, 512f / Math.max(iw, ih));
        int w = Math.max(1, Math.round(iw * k)), h = Math.max(1, Math.round(ih * k));
        try {
            Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            icon.setBounds(0, 0, w, h);
            icon.draw(new Canvas(bm));
            int l = w, t = h, r = -1, btm = -1;
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (Color.alpha(bm.getPixel(x, y)) > 16) { l = Math.min(l, x); t = Math.min(t, y); r = Math.max(r, x); btm = Math.max(btm, y); }
            int[] cut = TileArt.trim(w, h, l, t, r, btm);
            if (cut == null) { bm.recycle(); return null; }
            Bitmap c = Bitmap.createBitmap(bm, cut[0], cut[1], cut[2], cut[3]);
            if (c != bm) bm.recycle();
            return new BitmapDrawable(getResources(), c);
        } catch (RuntimeException | OutOfMemoryError e) {
            return null;
        }
    }

    /** the app's icon at the highest density it has (TileArt.DENSITY, xxxhdpi), so a tile is never drawn from a small picture; else the usual one */
    private Drawable sharpIcon(ResolveInfo ri, PackageManager pm) {
        try {
            int id = ri.getIconResource();
            if (id != 0) {
                Drawable d = pm.getResourcesForApplication(ri.activityInfo.applicationInfo).getDrawableForDensity(id, TileArt.DENSITY, null);
                if (d != null) return d;
            }
        } catch (PackageManager.NameNotFoundException | RuntimeException | OutOfMemoryError ignored) { }
        return ri.loadIcon(pm);
    }

    /** On the main thread: the dock first, then the grid a few rows at a time, so a long list never holds up a frame. */
    private void showApps(List<App> all, String[] wanted, int gen) {
        Map<String, App> byPkg = new HashMap<>();
        for (App a : all) byPkg.put(a.pkg, a);
        List<App> dock = new ArrayList<>();
        for (String p : wanted) if (byPkg.containsKey(p)) dock.add(byPkg.remove(p));
        long again = DockRetry.next(dockRetries, wanted.length, dock.size());   // some chosen favourites not in the system's list yet (a cold start): the row is built again soon
        if (again >= 0) { dockRetries++; handler.postDelayed(dockRetry, again); android.util.Log.i("UrTVHome", "favourites: " + dock.size() + " of " + wanted.length + " found, building the row again in " + again + " ms"); }
        // 4.4 (a report: «the focus jumps and another app opens»): the app that has the focus is kept by its package, not by the view's id: every tile
        // gets a new id here, so the old id was never found and the focus fell to the first favourite, where the next OK opened it (FocusKeep)
        String keepPkg = FocusKeep.keep(keyedSinceHome, focusedPkg(), focusPkgAfter);
        cardByPkg.clear();
        List<App> rest = new ArrayList<>(byPkg.values());   // by name, then in the owner's order (inSavedOrder)
        Collections.sort(rest, (x, y) -> x.label.compareToIgnoreCase(y.label));
        // Fewer than MIN_DOCK of the chosen apps installed: top the dock up from the rest, in the order of their package names, never of their labels: a
        // label follows the screen's language (Downloader in Arabic where English said Apple TV, build 180), the dock must not.
        {
            List<String> have = new ArrayList<>(), other = new ArrayList<>();
            for (App d : dock) have.add(d.pkg);
            for (App x : rest) other.add(x.pkg);
            for (String p : AppsEdit.topUp(have, other, AppsEdit.floor(Dock.exact(this), have.size(), MIN_DOCK))) for (App x : new ArrayList<>(rest)) if (x.pkg.equals(p)) { rest.remove(x); dock.add(x); }   // AppsEdit.topUp: by package, not by label
        }
        rest = inSavedOrder(rest);   // «تعديل التطبيقات»: the owner's order, new apps at the end; nothing saved, by name as always

        dockRow.removeAllViews();
        grid.removeAllViews();
        dockCards.clear();
        gridCards.clear();
        gridLabels.clear();

        for (int i = 0; i < dock.size(); i++) {
            View tile = tile(dock.get(i), true);
            dockRow.addView(tile, dockCell(i, dock.size()));
        }
        addAllAppsTile();
        applyTrayHeight();
        appsLabel.setVisibility(rest.isEmpty() ? View.GONE : View.VISIBLE);
        wireFocus();
        if (!dock.isEmpty()) { title.setText(dock.get(0).label); setGlow(dock.get(0).color); }
        focusPkgAfter = keepPkg;   // FocusKeep: the same app gets the focus back once its tile exists (a grid app comes with its row, addGridRows)
        View last = lastFocusedId == View.NO_ID || !keyedSinceHome ? null : findViewById(lastFocusedId);
        if (last != null) last.requestFocus();
        else if (!dockCards.isEmpty() && (keepPkg == null || cardByPkg.containsKey(keepPkg))) dockCards.get(0).requestFocus();
        appsShown = true;
        appsShownGen = gen;   // 4.5 HomeLook: the row asked last is on the screen
        if (entranceDue) { entranceDue = false; playEntrance(); }
        applyPendingFocus();
        beginPendingEdit();
        if (!tvShown.isEmpty() && tvCells != dockCards.size()) showTvRows(tvShown);   // the favourites came (or changed): the row's cells follow
        addGridRows(rest, 0, gen);
    }

    private void addGridRows(List<App> rest, int from, int gen) {
        if (gen != appsGen || isDestroyed()) return;
        int to = Math.min(rest.size(), from + BATCH_ROWS * cols);
        LinearLayout row = null;
        for (int i = from; i < to; i++) {
            if (i % cols == 0) {
                row = new LinearLayout(this);
                row.setClipChildren(false);
                row.setClipToPadding(false);
                grid.addView(row, llp(-1, -2, 0, 0, 0, dp(26)));
            } else if (row == null) {
                row = (LinearLayout) grid.getChildAt(grid.getChildCount() - 1);
            }
            View tile = tile(rest.get(i), false);
            row.addView(tile, rowCell(i % cols, cols, -2));
        }
        if (to < rest.size()) {
            wireFocus();
            handler.post(() -> addGridRows(rest, to, gen));
            return;
        }
        // Pad the last row so tiles keep their width.
        if (row != null) for (int i = rest.size() % cols; i != 0 && i < cols; i++) row.addView(new View(this), rowCell(i, cols, 1));
        wireFocus();
        applyPendingFocus();
        if (focusPkgAfter != null && getCurrentFocus() == null && !dockCards.isEmpty()) dockCards.get(0).requestFocus();   // FocusKeep: the kept app is gone, the first favourite as before
        focusPkgAfter = null;   // not found (the app is gone): never take the focus later
        gridDone = true;
        beginPendingAppsEdit();
    }

    /** «كل التطبيقات» (given by name) in the order «تعديل التطبيقات» saved (AppsEdit.arrange). */
    private List<App> inSavedOrder(List<App> byName) {
        Map<String, App> by = new HashMap<>();
        List<String> names = new ArrayList<>();
        for (App a : byName) { by.put(a.pkg, a); names.add(a.pkg); }
        List<App> out = new ArrayList<>();
        for (String p : AppsEdit.arrange(names, Dock.appsOrder(this))) out.add(by.get(p));
        return out;
    }

    /**
     * The place of cell {@code i} of {@code n} in a row of the dock or the grid: equal shares, 20 dp between two cells and none at the
     * row's two ends. Start and end, not left and right: the home is right to left, so the first cell's edge is the row's right edge,
     * under the start of «كل التطبيقات» (left and right margins put the outer gap inside and the row a step off, the owner's photo).
     */
    /** A favourite's cell (FavBar): 304 wide (its 16:9 banner 170 high), 24 between two, none at the row's two ends. */
    private LinearLayout.LayoutParams dockCell(int i, int n) { return dockCell(i, n, n, appsLook); }

    /** cell {@code i} of {@code n} in a row whose tiles are as wide as the tray's when {@code favs} favourites are shown (the watch-next rows share the tray's cells) */
    private LinearLayout.LayoutParams dockCell(int i, int n, int favs) { return dockCell(i, n, favs, FavBar.TILES); }   // «كمّل المشاهدة»: the tray's cells, whatever the way

    private LinearLayout.LayoutParams dockCell(int i, int n, int favs, String way) {
        int h = getResources().getDisplayMetrics().heightPixels, gap = FavBar.gap(way, h);   // 4.4 «شكل التطبيقات»: each way its own cell
        if (FavBar.GLASS.equals(way)) {   // 4.5 «زجاجي»: the squircle at the person's size (the grid of DockGrid places it when there are rows)
            int c = FavBar.glassCell(h, glassK), g = FavBar.glassGap(h, glassK);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(c, -2);
            p.setMarginStart(i == 0 ? 0 : g / 2);
            p.setMarginEnd(i == n - 1 ? 0 : g - g / 2);
            return p;
        }
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(FavBar.cellW(way, h, FavBar.shown(barSetting, favs), i), -2);
        p.setMarginStart(i == 0 ? 0 : gap / 2);
        p.setMarginEnd(i == n - 1 ? 0 : gap - gap / 2);
        return p;
    }

    /** «تطبيقات في الشريط» (the screensaver's setting, read with the rest): how many tiles the tray shows whole; the count of favourites matters for "all" */
    private String barSetting = "5";

    private int barShown() { return FavBar.shown(barSetting, dockCards.size()); }

    private int lift() { return FavBar.lift(getResources().getDisplayMetrics().heightPixels, barShown()); }

    /** the tray is as high as its tiles with the same 24 above and below them (FavBar.trayH): its bottom stays where it was, its top follows */
    private void applyTrayHeight() {
        if (tray == null) return;
        int h = glassWay() ? FavBar.glassRowH(getResources().getDisplayMetrics().heightPixels, glassK, glassRowsN, dockRow == null ? 1 : dockRow.getChildCount())
                : FavBar.rowH(appsLook, getResources().getDisplayMetrics().heightPixels, barShown());
        android.view.ViewGroup.LayoutParams lp = tray.getLayoutParams();
        if (lp != null && lp.height != h) { lp.height = h; tray.setLayoutParams(lp); }
    }

    /** the setting changed (or the favourites did): every cell gets its new width, and the tray shows the same favourites in the new size */
    private void applyBarSize(String setting) {
        if (setting == null) return;
        boolean changed = !setting.equals(barSetting);
        barSetting = setting;
        if (dockRow == null) return;
        int n = dockRow.getChildCount();
        if (changed) for (int i = 0; i < n; i++) dockRow.getChildAt(i).setLayoutParams(dockCell(i, n));
        applyTrayHeight();
    }

    /** a length of FavBar's 1080p spec on this screen (scaled by its height, so 720p is two thirds) */
    private int bar(float px1080) { return FavBar.px(px1080, getResources().getDisplayMetrics().heightPixels); }

    private LinearLayout.LayoutParams rowCell(int i, int n, int height) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, height, 1f);
        p.setMarginStart(i == 0 ? 0 : dp(6));   // 6 + 6 dp = 24 px apart at 1080p: the same gap as the favourites tray (FavBar.GAP)
        p.setMarginEnd(i == n - 1 ? 0 : dp(6));
        return p;
    }

    /** After «إضافة إلى المفضلة» and the like: the focus goes to the app that was changed, once its tile exists (the loads are in the background). */
    private String focusPkgAfter;
    private void applyPendingFocus() {
        if (focusPkgAfter == null) return;
        View c = cardByPkg.get(focusPkgAfter);
        if (c != null) { focusPkgAfter = null; c.requestFocus(); }
    }

    /** A 16:9 card with the app's banner; the name shows under it while focused. */
    private View tile(App app, boolean inDock) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setClipChildren(false);
        box.setClipToPadding(false);

        AspectFrame card = new AspectFrame(this);
        final boolean icons = inDock && FavBar.ICONS.equals(appsLook) && app.icon != null;   // B: the icon's rounded square
        if (icons) card.ratio = FavBar.aspect(FavBar.ICONS);
        final boolean glassy = inDock && glassWay() && app.icon != null;   // 4.5 «زجاجي»: the squircle made off the main thread (appOf), drawn whole
        if (glassy) card.ratio = 1f;
        final int corner = glassy ? FavBar.glassRadius(getResources().getDisplayMetrics().heightPixels, glassK) : inDock ? FavBar.radius(icons ? FavBar.ICONS : FavBar.TILES, getResources().getDisplayMetrics().heightPixels) : dp(10);
        card.setId(View.generateViewId());
        card.setFocusable(true);
        card.setClickable(true);
        card.setTag(app);
        card.setClipToOutline(true);
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), corner);
            }
        });
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(app.isBanner && !icons ? 0xFF1C1C1E : app.composed && !icons || icons && app.iconComposed ? 0 : TileArt.LIGHT);   // 4.4 beta 2 (the owner): never a dark tint of the app's colour
        if (glassy) bg.setColor(0);   // the squircle has its own colour
        card.setBackground(bg);

        ImageView img = new ImageView(this);
        img.setImageDrawable(icons ? app.icon : app.art);
        if (glassy) {
            img.setImageDrawable(app.icon);
            img.setScaleType(ImageView.ScaleType.FIT_XY);
            card.addView(img, new FrameLayout.LayoutParams(-1, -1));
        } else if (icons && app.iconComposed) {   // B: its square composed by TileArt (beta 2), full-bleed like the tiles
            img.setScaleType(ImageView.ScaleType.FIT_XY);
            card.addView(img, new FrameLayout.LayoutParams(-1, -1));
        } else if (icons) {   // B: the icon filling its square, a little inside (only when it could not be composed)
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            card.addView(img, new FrameLayout.LayoutParams(-1, -1));
            img.setPadding(dp(10), dp(10), dp(10), dp(10));
        } else if (app.isBanner || app.composed) {   // a banner, or the icon already composed on its tile (TileArt): full-bleed
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(img, new FrameLayout.LayoutParams(-1, -1));
        } else {
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            card.addView(img, new FrameLayout.LayoutParams(-1, -1));
            img.setPadding(dp(22), dp(14), dp(22), dp(14));
        }

        // Focus ring and sheen, drawn above the art.
        GradientDrawable ring = new GradientDrawable();
        ring.setCornerRadius(corner);
        ring.setStroke(inDock ? trayRingW() : dp(2), inDock ? trayRing() : 0xE6FFFFFF);
        ring.setOrientation(GradientDrawable.Orientation.TL_BR);
        ring.setColor(0);   // the ring only (the owner, 19 Nov: no glow, no decoration)
        View shine = new View(this);
        shine.setBackground(ring);   // shown only while an app is carried in «تعديل المفضلة» (liftCard), never as the focus: the owner, «no outline»
        shine.setAlpha(0f);
        card.addView(shine, new FrameLayout.LayoutParams(-1, -1));
        addEditMarks(card, ring, shine);   // the × of «تعديل المفضلة» and «تعديل التطبيقات», shown only in the mode

        box.addView(card, new LinearLayout.LayoutParams(-1, -2));

        TextView label = text(11, "sans-serif-medium", Color.WHITE);
        label.setText(app.label);
        label.setSingleLine(true);
        label.setGravity(Gravity.CENTER);
        label.setAlpha(0f);
        if (!inDock) {
            box.addView(label, llp(-1, -2, 0, dp(10), 0, 0));
            gridLabels.add(label);
            if (lightNow || "sand".equals(lookNow)) label.setTextColor(palette().ink);
        }

        if (InputFav.isInput(app.pkg)) card.setOnClickListener(v -> openFavouriteInput(app));   // a favourite input (InputFav): switch to it
        else card.setOnClickListener(v -> guarded(inDock ? ParentalPolicy.TILE : ParentalPolicy.GRID, app.pkg, app.label, () -> launch(new Intent(Intent.ACTION_MAIN)
                .addCategory(app.category)
                .setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))));
        card.setOnFocusChangeListener((v, has) -> {
            final int dbg = dbgFocus;   // 0 normally: every switch below is off (dock.sh debug nolift|noglow|noshine|noscale|notitle|plainfocus)
            final boolean zoom = inDock && appsZoom;   // 4.4 «شكل التطبيقات: تكبير وظل»: larger with a soft shadow, no ring, the name under it
            float s = has ? (inDock ? (zoom ? FavBar.ZOOM_SCALE : FavBar.FOCUS_SCALE) : 1.1f) : 1f;   // the owner (19 Nov): the focus is the size, also on a weak TV and in «tvOS» (no ring); a weak TV sets it at once (Motion)
            // from the games of the football home: the tray goes back to its resting place (y 768) first, then the tile lifts (FavBar.DROP_MS)
            long wait = inDock && has && trayLowered ? FavBar.DROP_MS : 0;
            if (inDock && has) lowerTray(false);
            if ((dbg & DebugFlags.SCALE) == 0) { if (wait > 0) v.postDelayed(() -> { if (v.hasFocus()) scaleTo(v, s); }, wait); else scaleTo(v, s); }   // a spring: a held arrow keeps its speed from card to card
            float z = (dbg & DebugFlags.LIFT) != 0 ? 0 : has ? dp(zoom ? 2 * GlowLook.LIFT_DP : GlowLook.LIFT_DP) : 0;
            if ((dbg & DebugFlags.PLAIN) == 0) {
                if (inDock) v.animate().translationY(has ? -lift() : 0).translationZ(z)
                        .setInterpolator(Motion.ease()).setStartDelay(wait).setDuration(Motion.move(MainActivity.this)).start();
                else v.animate().translationZ(z).setInterpolator(Motion.ease()).setDuration(Motion.move(MainActivity.this)).start();
                if ((dbg & DebugFlags.SHINE) == 0) shine.setAlpha(0f);   // the owner (19 Nov): no outline, the focus is the size, the lift and the shadow
                label.animate().alpha(has ? 1f : 0f).translationY(has ? dp(4) : 0).setDuration(Motion.MS).setInterpolator(Motion.ease()).start();
            } else if ((dbg & DebugFlags.SHINE) == 0) shine.setAlpha(0f);
            if (glassy && Ram.low(MainActivity.this)) { v.animate().cancel(); v.setScaleX(s); v.setScaleY(s); }   // 4.4 beta 2 (the owner: no outline): on a weak TV the tvOS icon grows too, set at once (nothing else shows the focus)
            if (!has) return;
            lastFocusedId = v.getId();
            optionsHint(inDock);
            if (inDock) {
                nameCard = v;
                if ((dbg & DebugFlags.TITLE) == 0) {
                    title.setText(app.label);
                    placeName();
                    title.animate().alpha(1f).setDuration(Motion.MS).setInterpolator(Motion.ease()).start();
                }
                if ((dbg & DebugFlags.GLOW) == 0) { animateGlow(app.color); fadeGlow(1f); }
                if ((dbg & DebugFlags.PLAIN) == 0) glideTo(0);
            } else {
                if ((dbg & DebugFlags.GLOW) == 0) fadeGlow(0.35f);
                if ((dbg & DebugFlags.TITLE) == 0) title.animate().alpha(0f).setDuration(Motion.MS).setInterpolator(Motion.ease()).start();
                scrollToRow(box);
            }
        });

        // a long OK (or the remote's Menu key, dispatchKeyEvent): the app's options (AppOptions); each of them is in the settings too
        card.setOnLongClickListener(v -> { optionsHintUsed(); showAppMenu(v, app.pkg, inDock); return true; });
        cardByPkg.put(app.pkg, card);

        (inDock ? dockCards : gridCards).add(card);
        return box;
    }

    /** Explicit D-pad targets: one row per press, ends of rows and of the top bar don't wrap. */
    private int dbgFocus;
    private int lastPillId = View.NO_ID;
    /**
     * 4.4 beta 4 (the emulator's «reach» round 12: after Home the focus stood on «الجوال», three buttons from «مركز التحكم»): before the apps
     * came, the top row's first button took the window's first focus and was kept as «the last place»; until the person presses a key after
     * the home opens (or Home), the place is the first app
     */
    private boolean keyedSinceHome;   // the pill that had the focus last: ▲ from a home card goes back to it

    /**
     * 4.4 beta 4 (the emulator's «reach» rounds 8-10: ▲ from the apps row or a card went to the first button of the top row, four buttons away from
     * «مركز التحكم» in «كنبة»): the button ▲ lands on from below is «مركز التحكم» while it is on the top row (our settings are its first row),
     * else the first button there that is shown
     */
    private View upPill() {
        if (onTopRow(centerPill)) return centerPill;   // in the row and not hidden (isShown is false before the window is there: the first wiring)
        for (View p : pills) if (onTopRow(p)) return p;
        return pills.get(0);
    }

    private boolean onTopRow(View p) {
        return p != null && pills.contains(p) && p.getParent() == pillRow && p.getVisibility() == View.VISIBLE && p.isFocusable();
    }

    private void wireFocus() {
        footballHeader();   // the home card was just made again (or not): «الواجهة» may have changed with it
        boolean onHome = homeGrid();
        grid.setVisibility(onHome ? View.VISIBLE : View.GONE);
        if (!onHome) appsLabel.setVisibility(View.GONE);
        int d = dockCards.size(), g = onHome ? gridCards.size() : 0;
        // the «المهام» card (and the stocks card), when among the home cards, is a stop between the pills and the dock
        List<View> targets = homeCard == null ? Collections.<View>emptyList() : homeCard.targets();
        View card = targets.isEmpty() ? null : targets.get(0);
        int dockId = d > 0 ? dockCards.get(Math.min(d - 1, d / 2)).getId() : View.NO_ID;
        // the football home (FootballFocus): the games at the start side, then the prayers, then the weather; ▼ from any of them is the tray,
        // ▲ from the tray the games, ▲ from the open row of games the prayers, ▲ from those two the buttons
        View fMt = homeCard == null ? null : homeCard.footballTarget(FootballFocus.MT);
        View fPr = homeCard == null ? null : homeCard.footballTarget(FootballFocus.PR);
        View fWx = homeCard == null ? null : homeCard.footballTarget(FootballFocus.WX);
        if (fMt != null && fPr != null && fWx != null) {
            View[] order = {fMt, fPr, fWx};   // from the start side (the right in Arabic)
            for (int i = 0; i < order.length; i++) {
                View v = order[i], toStart = i > 0 ? order[i - 1] : v, toEnd = i < order.length - 1 ? order[i + 1] : v;
                v.setNextFocusRightId((Dir.rtl() ? toStart : toEnd).getId());
                v.setNextFocusLeftId((Dir.rtl() ? toEnd : toStart).getId());
                v.setNextFocusUpId(upPill().getId());
                v.setNextFocusDownId(dockId != View.NO_ID ? dockId : v.getId());
            }
            fMt.setNextFocusUpId(fPr.getId());
            card = fPr;   // the buttons and «يُتلى الآن» go down to the prayers; below, the tray goes up to the games
        }
        for (int i = 0; i < pills.size(); i++) {
            View p = pills.get(i);
            p.setNextFocusLeftId(i == 0 ? p.getId() : pills.get(i - 1).getId());
            p.setNextFocusRightId(i == pills.size() - 1 ? p.getId() : pills.get(i + 1).getId());
            p.setNextFocusUpId(p.getId());
            p.setNextFocusDownId(card != null ? card.getId() : dockId != View.NO_ID ? dockId : p.getId());
        }
        wireNudge();   // the bar above the top row (when it shows): UP from the buttons reaches it
        int topId = card != null ? card.getId() : upPill().getId();
        if (fMt != null && fPr != null && fWx != null) topId = fMt.getId();   // the football home: ▲ from the tray is the games
        if (nowCard != null) {   // the «يُتلى الآن» card: down to the cards or the dock, left and up to the pills (right: the end of the row)
            nowCard.setNextFocusLeftId(pills.get(0).getId());
            nowCard.setNextFocusUpId(upPill().getId());
            nowCard.setNextFocusRightId(nowCard.getId());
            nowCard.setNextFocusDownId(card != null ? card.getId() : dockId != View.NO_ID ? dockId : nowCard.getId());
        }
        for (int i = 0; i < targets.size(); i++) {
            View t = targets.get(i);
            // the next card in reading order with the arrow that goes on (left in Arabic), the one before with the other; at an end: stay on it
            View next = i + 1 < targets.size() ? targets.get(i + 1) : t, prev = i > 0 ? targets.get(i - 1) : t;
            if (Dir.rtl()) { t.setNextFocusLeftId(next.getId()); t.setNextFocusRightId(prev.getId()); }
            else { t.setNextFocusRightId(next.getId()); t.setNextFocusLeftId(prev.getId()); }
            t.setNextFocusUpId(upPill().getId());   // a pill's own focus moves this to that pill, see pill()
            t.setOnKeyListener((v, keyCode, ev) -> {   // ▲ from a home card is a pill that is on screen: the pill it last came from may be gone (the bell leaves the row), and the focus then stayed on the card (step 28b)
                if (keyCode != KeyEvent.KEYCODE_DPAD_UP) return false;
                if (ev.getAction() != KeyEvent.ACTION_DOWN) return true;
                View to = null;
                for (View pv : pills) if (pv.isShown() && pv.isFocusable() && pv.getId() == lastPillId) { to = pv; break; }
                if (to == null) to = upPill();
                return to != null && to.requestFocus();
            });
            t.setNextFocusDownId(dockId != View.NO_ID ? dockId : t.getId());
        }
        for (int i = 0; i < d; i++) {
            View c = dockCards.get(i);
            c.setNextFocusUpId(topId);
            c.setNextFocusLeftId(i == 0 ? c.getId() : dockCards.get(i - 1).getId());
            c.setNextFocusRightId(i == d - 1 ? c.getId() : dockCards.get(i + 1).getId());
            c.setNextFocusDownId(!tvCards.isEmpty() ? tvCards.get(0).get(Math.min(i, tvCards.get(0).size() - 1)).getId()
                    : g > 0 ? gridCards.get(Math.min(i, g - 1)).getId() : c.getId());
        }
        if (allAppsCard != null && d > 0) {   // «زجاجي»: «كل التطبيقات» goes up and down as the last favourite
            allAppsCard.setNextFocusUpId(topId);
            allAppsCard.setNextFocusDownId(dockCards.get(d - 1).getNextFocusDownId());
        }
        if (glassWay() && dockRow != null && dockRow.rows() > 1) {   // «زجاجي» in rows: ▲ ▼ inside the grid first (FavBar.glassUpDown), then the home's own
            int n = dockRow.getChildCount();
            for (int i = 0; i < n; i++) {
                View c = ((ViewGroup) dockRow.getChildAt(i)).getChildAt(0);
                int up = FavBar.glassUpDown(i, n, dockRow.rows(), false), dn = FavBar.glassUpDown(i, n, dockRow.rows(), true);
                if (up >= 0) c.setNextFocusUpId(((ViewGroup) dockRow.getChildAt(up)).getChildAt(0).getId());
                if (dn >= 0) c.setNextFocusDownId(((ViewGroup) dockRow.getChildAt(dn)).getChildAt(0).getId());
            }
        }
        for (int r = 0; r < tvCards.size(); r++) {   // «كمّل المشاهدة»: a row up to the one above (the favourites), down to the next (the apps)
            List<View> row = tvCards.get(r);
            for (int i = 0; i < row.size(); i++) {
                View c = row.get(i);
                c.setNextFocusUpId(r > 0 ? tvCards.get(r - 1).get(Math.min(i, tvCards.get(r - 1).size() - 1)).getId()
                        : d > 0 ? dockCards.get(Math.min(i, d - 1)).getId() : topId);
                c.setNextFocusDownId(r < tvCards.size() - 1 ? tvCards.get(r + 1).get(Math.min(i, tvCards.get(r + 1).size() - 1)).getId()
                        : g > 0 ? gridCards.get(Math.min(i, g - 1)).getId() : c.getId());
                c.setNextFocusLeftId(i == row.size() - 1 ? c.getId() : row.get(i + 1).getId());   // the row reads from the right
                c.setNextFocusRightId(i == 0 ? c.getId() : row.get(i - 1).getId());
            }
        }
        int lastRow = (g - 1) / cols;
        for (int j = 0; j < g; j++) {
            View c = gridCards.get(j);
            int r = j / cols, col = j % cols;
            List<View> lastTv = tvCards.isEmpty() ? null : tvCards.get(tvCards.size() - 1);
            c.setNextFocusUpId(r == 0
                    ? (lastTv != null ? lastTv.get(Math.min(col, lastTv.size() - 1)).getId()
                       : d > 0 ? dockCards.get(Math.min(col, d - 1)).getId() : topId)
                    : gridCards.get(j - cols).getId());
            c.setNextFocusDownId(j + cols < g ? gridCards.get(j + cols).getId()
                    : r < lastRow ? gridCards.get(g - 1).getId() : c.getId());
            c.setNextFocusLeftId(col == 0 ? c.getId() : gridCards.get(j - 1).getId());
            c.setNextFocusRightId(col == cols - 1 || j == g - 1 ? c.getId() : gridCards.get(j + 1).getId());
        }
        if (edit != null) wireEditFocus();   // a rewiring while «تعديل المفضلة» is on (the row, the pills) keeps the mode's own
        if (appsEdit != null) wireAppsEditFocus();
    }

    /**
     * While «تعديل المفضلة» is on, Android's own focus search never leaves the row: UP from a tile is «تراجع» while it shows (else the
     * tile itself), DOWN its ×, and from the chip DOWN is the tile and the other ways stay on it. editKey moves the focus itself; this is
     * the net under it, for a key that reaches the views (on the TV the UP to the chip went astray and the seconds ran out).
     */
    private void wireEditFocus() {
        FavEdit e = edit;
        if (e == null || dockCards.isEmpty()) return;
        boolean chip = undoChip.getVisibility() == View.VISIBLE;
        for (View c : dockCards) {
            TextView x = xByCard.get(c);
            c.setNextFocusUpId(chip ? undoChip.getId() : c.getId());
            c.setNextFocusDownId(x != null ? x.getId() : c.getId());
            if (x != null) { x.setNextFocusUpId(c.getId()); x.setNextFocusDownId(x.getId()); }
        }
        View tile = dockCards.get(Math.min(e.at(), dockCards.size() - 1));
        undoChip.setNextFocusDownId(tile.getId());
        undoChip.setNextFocusUpId(undoChip.getId());
        undoChip.setNextFocusLeftId(undoChip.getId());
        undoChip.setNextFocusRightId(undoChip.getId());
    }

    // ---------------------------------------------------------------- motion
    // The focused card's size and the page's scroll run on springs (Spring: from talkies and Free My TV, MIT, THIRD_PARTY_NOTICES.md):
    // a new target keeps the speed it has, so held arrows glide and the rows slide on without a stop at each card. One frame callback
    // steps them all and stops once they have settled. With the TV's animations off (animator scale 0) everything goes straight there.
    private final java.util.Map<View, Spring> scaleSprings = new java.util.WeakHashMap<>();
    private final Spring scrollSpring = new Spring(0.34f, 1f, 0, 0.25f);   // no bounce: a page that overshoots looks wrong
    private boolean motionPosted, scrollGliding;
    private long motionNanos;

    /** The size of a card, by its spring (a little bounce, as the focus had). */
    private void scaleTo(View v, float scale) {
        if (!motionOn()) { scaleSprings.remove(v); v.setScaleX(scale); v.setScaleY(scale); return; }
        Spring sp = scaleSprings.get(v);
        if (sp == null) scaleSprings.put(v, sp = new Spring(0.34f, 0.74f, v.getScaleX()));
        sp.target = scale;
        startMotion();
    }

    /** The page's scroll, by its spring; from where the page is now when nothing was moving it. */
    private void glideTo(int y) {
        if (!motionOn()) { scrollSpring.snap(y); scroll.scrollTo(0, y); return; }
        if (!scrollGliding) scrollSpring.snap(scroll.getScrollY());   // from where the page is (nothing was gliding it)
        scrollSpring.target = y;
        scrollGliding = true;
        startMotion();
    }

    private boolean animationsOn = true;   // the TV's animator scale, read on each return to the front (not on every key)
    private final android.view.Choreographer.FrameCallback motionFrame = this::doMotion;   // one callback, posted again and again

    private boolean motionOn() { return animationsOn; }

    private void readAnimationScale() {
        animationsOn = Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
    }

    private void startMotion() {
        if (motionPosted) return;
        motionPosted = true;
        motionNanos = 0;
        android.view.Choreographer.getInstance().postFrameCallback(motionFrame);
    }

    /** One frame: every spring a step, applied; posts itself again while any still moves. */
    private void doMotion(long now) {
        if (isDestroyed()) { motionPosted = false; return; }
        float dt = motionNanos == 0 ? 1 / 60f : Math.min(0.05f, (now - motionNanos) / 1e9f);
        motionNanos = now;
        boolean moving = scrollSpring.step(dt);
        if (moving || scrollGliding) scroll.scrollTo(0, Math.round(scrollSpring.value));   // and once more as it settles, on its target
        scrollGliding = moving;
        for (java.util.Iterator<java.util.Map.Entry<View, Spring>> it = scaleSprings.entrySet().iterator(); it.hasNext(); ) {
            java.util.Map.Entry<View, Spring> e = it.next();
            Spring sp = e.getValue();
            boolean m = sp.step(dt);
            e.getKey().setScaleX(sp.value);
            e.getKey().setScaleY(sp.value);
            if (m) moving = true;
            else if (sp.target == 1f) it.remove();   // at rest at its own size: nothing to keep
        }
        if (moving) android.view.Choreographer.getInstance().postFrameCallback(motionFrame);
        else motionPosted = false;
    }

    // ---------------------------------------------------------------- «كمّل المشاهدة»
    private LinearLayout tvRowsBox;
    private final List<List<View>> tvCards = new ArrayList<>();   // the row's cards, in order (the focus wiring; one row at most)
    private int tvRowsGen;   // a load that finishes after a newer one started is dropped
    private final List<TextView> tvNames = new ArrayList<>();   // «المظهر» recolours the row's name

    // their own thread: a slow provider or picture never holds up the apps' loader (the dock, the grid)
    private final java.util.concurrent.ExecutorService tvLoader = java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final long TV_PICTURES_MS = 8000;   // the pictures of one visit; what is not there by then keeps its title
    private String tvShownSig = "";
    private List<TvRows.Row> tvShown = new ArrayList<>();
    private int tvCells;   // the cells «كمّل المشاهدة» was cut into (the favourites' count then): built again when that changes
    private final java.util.Map<TvRows.Item, ImageView> tvPics = new java.util.HashMap<>();
    private final java.util.Map<TvRows.Item, TextView> tvPlain = new java.util.HashMap<>();
    private final java.util.Set<TvRows.Item> tvPictured = new java.util.HashSet<>();

    /** The rows' words first (quick), shown; then their pictures as they come, within TV_PICTURES_MS. */
    private void loadTvRows() {
        if (tvRowsBox == null || cleanShown) return;   // «UrTV النظيف»: no extra rows
        final int gen = ++tvRowsGen;
        if (tvLoader.isShutdown()) return;
        tvLoader.execute(() -> {
            // G4: the setting is read here, on the row's own thread: a return no longer waits on the main thread for the screensaver's provider (a cross-process query)
            if (!TvRows.allowed(this) || !ConfigSnapshot.read(getApplicationContext()).watchNextOn) {   // not allowed, or turned off in the settings: no row
                runOnUiThread(() -> { if (gen == tvRowsGen && !isDestroyed() && tvRowsBox.getChildCount() > 0) { tvShownSig = ""; showTvRows(new ArrayList<>()); } });
                return;
            }
            List<TvRows.Row> rows = TvRows.load(this);
            String sig = tvSignature(rows);
            final List<TvRows.Row>[] on = new List[1];
            try {
                java.util.concurrent.CountDownLatch shown = new java.util.concurrent.CountDownLatch(1);
                runOnUiThread(() -> {
                    if (gen == tvRowsGen && !isDestroyed()) {
                        if (!sig.equals(tvShownSig)) { tvShownSig = sig; showTvRows(rows); }   // the same rows: nothing is built again
                        on[0] = tvShown;
                    }
                    shown.countDown();
                });
                shown.await();
            } catch (InterruptedException e) {
                return;
            }
            if (on[0] == null) return;
            List<TvRows.Row> missing = new ArrayList<>();   // only the cards still without their picture
            for (TvRows.Row r : on[0]) {
                TvRows.Row m = new TvRows.Row();
                for (TvRows.Item it : r.items) if (!tvPictured.contains(it)) m.items.add(it);
                missing.add(m);
            }
            TvRows.pictures(this, missing, TV_PICTURES_MS, (it, b) -> runOnUiThread(() -> {
                if (gen != tvRowsGen || isDestroyed()) { b.recycle(); return; }
                ImageView img = tvPics.get(it);
                if (img == null) { b.recycle(); return; }
                img.setImageBitmap(b);
                img.setVisibility(View.VISIBLE);
                TextView plain = tvPlain.get(it);
                if (plain != null) plain.setVisibility(View.GONE);
                tvPictured.add(it);
            }));
        });
    }

    /** What the rows say: a row's key, its cards' titles, lines, links, pictures and progress. */
    private static String tvSignature(List<TvRows.Row> rows) {
        List<String[]> parts = new ArrayList<>();
        for (TvRows.Row r : rows) {
            List<String> p = new ArrayList<>();
            p.add(r.key);
            p.add(r.title);
            for (TvRows.Item it : r.items) p.add(it.title + "\n" + it.meta + "\n" + it.link + "\n" + it.artUri + "\n" + it.progress);
            parts.add(p.toArray(new String[0]));
        }
        return TvRowsPick.signature(parts);
    }

    /**
     * «كمّل المشاهدة»: its name, then as many cards as there are favourites, each a favourite's width right under one (MainActivity.rowCell,
     * the band's padding): no row that runs off the screen, nothing cut. Fewer cards keep the favourites' widths, the rest is empty.
     */
    private void showTvRows(List<TvRows.Row> rows) {
        boolean had = getCurrentFocus() != null && isInTvRows(getCurrentFocus());
        int hadAt = had && !tvCards.isEmpty() ? Math.max(0, tvCards.get(0).indexOf(getCurrentFocus())) : 0;
        tvShown = rows;
        tvShownSig = tvSignature(rows);
        tvPics.clear();
        tvPlain.clear();
        tvPictured.clear();
        tvRowsBox.removeAllViews();
        tvCards.clear();
        tvNames.clear();
        int n = Math.min(barShown(), TvRowsPick.cells(dockCards.size(), MIN_DOCK));   // never one cell (TvRowsPick.cells); at most the cells a tray shows: no scroll here, no card cut
        tvCells = n;
        for (TvRows.Row r : rows) {
            TextView name = text(11, "sans-serif-medium", lightNow || "sand".equals(lookNow) ? palette().gold : GOLD);
            name.setShadowLayer(dp(3), 0, dp(1), palette().shadow);   // a halo in the light look, as the clock's lines
            name.setText(S.tv(TvRowsPick.WATCH_NEXT_TITLE));   // our words, whatever the apps call it
            tvNames.add(name);
            tvRowsBox.addView(name, llp(-2, -2, 0, dp(14), 0, dp(10)));
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setClipChildren(false);
            line.setClipToPadding(false);
            List<View> cards = new ArrayList<>();
            int shown = Math.min(n, r.items.size());
            for (int i = 0; i < shown; i++) {
                View box = tvCard(r.items.get(i));
                line.addView(box, dockCell(i, n, dockCards.size()));
                cards.add(((ViewGroup) box).getChildAt(0));
            }
            for (int i = shown; i < n; i++) line.addView(new View(this), dockCell(i, n, dockCards.size()));   // fewer cards: still a favourite's width each
            tvRowsBox.addView(line, new LinearLayout.LayoutParams(-1, -2));
            tvCards.add(cards);
        }
        wireFocus();
        if (had && !tvCards.isEmpty()) tvCards.get(0).get(Math.min(hadAt, tvCards.get(0).size() - 1)).requestFocus();   // the same card
        else if (had && !dockCards.isEmpty()) dockCards.get(0).requestFocus();   // the row went: back to the favourites
        fitTray();   // the bar is the screen's width while the row is under it, its tiles' width without it
    }

    private boolean isInTvRows(View v) {
        for (View p = v; p != null; p = p.getParent() instanceof View ? (View) p.getParent() : null) if (p == tvRowsBox) return true;
        return false;
    }

    /**
     * A card: the picture (or its title on a plain card until it comes), a gold line of how far in. On the focus it is like a favourite:
     * the page stays at the top with the clock and the photo, and its name (and the episode) is the line above the favourites.
     */
    private View tvCard(TvRows.Item it) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setClipChildren(false);
        box.setClipToPadding(false);
        FrameLayout card = new AspectFrame(this);
        card.setId(View.generateViewId());
        card.setFocusable(true);
        card.setClickable(true);
        card.setClipToOutline(true);
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(10)); }
        });
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1C1C1E);
        card.setBackground(bg);
        TextView plain = text(13, "sans-serif-medium", Color.WHITE);   // the title on the plain card, until (unless) the picture comes
        plain.setText(it.title);
        plain.setGravity(Gravity.CENTER);
        plain.setPadding(dp(12), dp(8), dp(12), dp(8));
        card.addView(plain, new FrameLayout.LayoutParams(-1, -1));
        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setVisibility(View.GONE);
        card.addView(img, new FrameLayout.LayoutParams(-1, -1));
        tvPics.put(it, img);
        tvPlain.put(it, plain);
        if (it.progress >= 0) {   // how far in: a gold line at the foot, from the right
            View track = new View(this);
            track.setBackgroundColor(0x66000000);
            card.addView(track, new FrameLayout.LayoutParams(-1, dp(4), Gravity.BOTTOM));
            FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(-1, dp(4), Gravity.BOTTOM | Dir.start());
            View fill = new View(this);
            fill.setBackgroundColor(GOLD);
            card.addView(fill, fp);
            card.addOnLayoutChangeListener((v, l, t, rr, b, ol, ot, or, ob) -> {
                int w = Math.round((rr - l) * it.progress);
                if (fill.getLayoutParams().width != w) { fill.getLayoutParams().width = w; fill.requestLayout(); }
            });
        }
        GradientDrawable ring = new GradientDrawable();
        ring.setCornerRadius(dp(10));
        ring.setStroke(dp(2), 0xE6FFFFFF);
        View shine = new View(this);
        shine.setBackground(ring);
        shine.setAlpha(0f);
        card.addView(shine, new FrameLayout.LayoutParams(-1, -1));
        box.addView(card, new LinearLayout.LayoutParams(-1, -2));
        if (!it.empty) card.setOnClickListener(v -> guarded(ParentalPolicy.GRID, it.pkg, it.title, () -> TvRows.open(this, it)));   // the app's own lock first; the «ما يشارك» card opens nothing
        card.setOnFocusChangeListener((v, has) -> {
            scaleTo(v, has ? 1.12f : 1f);   // as a favourite
            v.animate().translationZ(has ? dp(GlowLook.LIFT_DP) : 0).setInterpolator(Motion.ease()).setDuration(Motion.move(MainActivity.this)).start();
            shine.animate().alpha(has ? 1f : 0f).setDuration(Motion.MS).setInterpolator(Motion.ease()).start();
            if (!has) return;
            lastFocusedId = v.getId();
            fadeGlow(1f);
            View line = (View) box.getParent();
            int at = tvRowsBox.indexOfChild(line) / 2;   // the box holds each row's name, then its cards
            if (at >= 0 && at < tvNames.size()) tvNames.get(at).setText(S.tv(TvRowsPick.WATCH_NEXT_TITLE) + S.sep() + (it.meta.isEmpty() ? it.title : it.title + S.sep() + it.meta));
            scrollToTvRow(line);   // below the first screen: the home slides up to the row; ▲ to the tray slides it back (glideTo(0))
        });
        return box;
    }

    /** «كمّل المشاهدة» below the first screen: the row (its name over it) a little above the screen's middle. */
    private void scrollToTvRow(View line) {
        int y = tvRowsBox.getTop() + line.getTop();
        int max = Math.max(0, content.getHeight() - scroll.getHeight());
        glideTo(Math.max(0, Math.min(y - (int) (getResources().getDisplayMetrics().heightPixels * 0.42f), max)));
    }

    /** the first grid row on the screen while the focus is in the grid (GridWindow): the page moves a whole row at a time */
    private int gridFirst;

    private void scrollToRow(View tileBox) {
        View row = (View) tileBox.getParent();
        int at = grid.indexOfChild(row);
        int screenH = getResources().getDisplayMetrics().heightPixels;
        int max = Math.max(0, content.getHeight() - scroll.getHeight());
        if (at == 0 && tvCards.isEmpty()) {   // the first grid row: the dock band slides up to the top edge (the tray, then the row under it)
            gridFirst = 0;
            // 4.4 beta 4 (the tester, step 71: the iqama bar over the apps page's first row): the bar keeps the top edge, the tray stops under it
            int barH = adhanBar.getVisibility() == View.VISIBLE ? Math.max(adhanBar.getHeight(), dp(26)) : 0;
            glideTo(Math.max(0, Math.min(hero.getTop() + bottomBlock.getTop() + tray.getTop() - barH, max)));
            return;
        }
        // 4.4 beta 2 (the owner's test of 525: the first row cut at the top, the focused one partly off): whole rows only, the focused one always
        // whole between a safe top and a safe bottom; the page moves only when the focus leaves the rows shown (GridWindow)
        int n = grid.getChildCount();
        int[] tops = new int[n], bottoms = new int[n];
        for (int i = 0; i < n; i++) { View c = grid.getChildAt(i); tops[i] = grid.getTop() + c.getTop(); bottoms[i] = grid.getTop() + c.getBottom(); }
        gridFirst = GridWindow.first(tops, bottoms, at, gridFirst, Math.round(screenH * GridWindow.SAFE_TOP), screenH - Math.round(screenH * GridWindow.SAFE_BOTTOM));
        glideTo(Math.max(0, Math.min(tops[gridFirst] - Math.round(screenH * GridWindow.SAFE_TOP), max)));
    }

    // ---------------------------------------------------------------- inputs

    // ---------------------------------------------------------------- dock editing

    private final Map<String, View> cardByPkg = new HashMap<>();
    static final int MIN_DOCK = Dock.MIN, MAX_DOCK = Dock.MAX;

    /**
     * After «تحديث تلفزيوني» replaced this app, the first start finds the «update in progress» record: the update screen opens once and says how it
     * went (it removes the record). Nothing happens without a record, so a normal start costs one missing-file check.
     */
    private static long updateResultAt;   // when the update screen was last opened from here (the singleTop UpdateActivity is the other half: step 47 saw two within 0.16 s)

    private void showUpdateResult() {
        if (android.os.SystemClock.elapsedRealtime() - updateResultAt < 3000) return;   // asked twice in a row (a resume and the one after it): once
        final android.content.Context app = getApplicationContext();
        // G4: the package manager and the update record are read off the main thread (every return asked them before its first frame); the screen opens after, on it
        new Thread(() -> {
            boolean due;
            try {
                if (sa.urtv.home.phone.PhoneUpdater.isBusy()) return;   // an update running in this very process: its own screen says how it goes
                if (sa.urtv.home.phone.PhoneUpdater.confirmKnown()) return;   // an install confirm is still pending: the record stays unread until it is answered
                long mine = sa.urtv.home.phone.PhoneUpdater.installedCode(app, app.getPackageName());
                due = sa.urtv.home.phone.UpdateRecords.peek(new sa.urtv.home.phone.UpdateStore(app), mine).kind != sa.urtv.home.phone.UpdateRecords.Kind.NONE;
            } catch (RuntimeException e) {
                return;
            }
            if (due) runOnUiThread(() -> {
                if (!resumed || isDestroyed() || android.os.SystemClock.elapsedRealtime() - updateResultAt < 3000) return;
                updateResultAt = android.os.SystemClock.elapsedRealtime();
                try {
                    launchGuarded(ParentalPolicy.UPDATE_SCREEN, new Intent(this, UpdateActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), S.t("تحديث تلفزيوني"));   // the result of an update: behind «قفل الإعدادات» like the rest of the screen
                } catch (RuntimeException ignored) { }
            });
        }, "home-update-result").start();
    }

    /** The launcher's settings file, shared with DockReceiver. */
    private android.content.SharedPreferences prefs() { return Dock.prefs(this); }

    /** The user's dock if they edited it, else res/values/config.xml. */
    private String[] loadDock() {
        String saved = prefs().getString("dock", null);
        return saved != null ? (saved.isEmpty() ? new String[0] : saved.split(","))
                             : getResources().getStringArray(R.array.dock_apps);
    }


    // ---------------------------------------------------------------- «تعديل المفضلة»

    /** The settings' «تعديل المفضلة» asks the home screen for this mode with this extra (FavEdit has the rules). */
    static final String EXTRA_EDIT = "edit_favourites";
    private static final int CORAL = 0xFFEE8B70;

    private FavEdit edit;                 // null: the mode is off
    private List<String> editStart = new ArrayList<>();   // the row the mode started with (the inputs it showed: one of them taken out is the person's choice)
    private boolean wantEdit;             // asked for from the settings, waiting for the favourites to be on screen
    private TextView editHint, undoChip;
    private final Map<View, TextView> xByCard = new HashMap<>();
    private final Map<View, GradientDrawable> ringByCard = new HashMap<>();
    private final Map<View, View> shineByCard = new HashMap<>();
    private long lastEditStep;
    private final Runnable undoExpiry = () -> { if (edit != null) { edit.expire(SystemClock.uptimeMillis()); paintEdit(); } };

    private void buildEditViews(FrameLayout root) {
        editHint = text(13, "sans-serif-medium", 0xFFFFFFFF);
        editHint.setText(S.tv(FavEdit.HINT));
        editHint.setSingleLine(true);
        editHint.setGravity(Gravity.CENTER);
        editHint.setPadding(dp(18), dp(6), dp(18), dp(6));
        editHint.setVisibility(View.GONE);   // its glass: paintEditChrome, as each mode starts
        root.addView(editHint, lp(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, dp(30), 0, 0));

        undoChip = undoChipView();
        undoChip.setOnClickListener(v -> { if (edit != null) applyEdit(edit.undo(SystemClock.uptimeMillis())); });
        root.addView(undoChip, lp(-2, -2, Gravity.BOTTOM | Dir.end(), Dir.rtl() ? dp(68) : 0, 0, Dir.rtl() ? 0 : dp(68), dp(150)));

        appsUndoChip = undoChipView();   // «تعديل التطبيقات»: under the hint line, UP from the grid's first row
        appsUndoChip.setOnClickListener(v -> { if (appsEdit != null) applyAppsEdit(appsEdit.undo(SystemClock.uptimeMillis())); });
        root.addView(appsUndoChip, lp(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, dp(78), 0, 0));
    }

    // ---------------------------------------------------------------- «خلّ تلفزيونك يفتح على UrTV»

    private static final String NUDGE_PREFS = "home_nudge";
    private LinearLayout nudge;
    private TextView nudgeCard, nudgeLater, nudgeAsk;   // nudgeAsk: «وين أنت؟…», the city question's words before its two buttons

    /**
     * the bar at the top while we are not the TV's Home (HomeNudgeRule): ONE short line, so it never covers a card (the first version was a two-line card with a button under it, over the
     * prayer card); OK on it opens «إعداد تلفزيوني» (behind its lock like every way in), «لاحقاً» hides it for 7 days, then 30, then for good. It is reachable with the remote: it takes the
     * focus the first time it shows, UP from the top row reaches it (wireNudge), and Back while it is focused hides it.
     */
    private void buildHomeNudge(FrameLayout root) {
        nudge = new LinearLayout(this);
        nudge.setOrientation(LinearLayout.HORIZONTAL);
        nudge.setLayoutDirection(Dir.layout());
        nudge.setGravity(Gravity.CENTER_VERTICAL);
        nudgeCard = text(NUDGE_SP, "sans-serif-medium", 0xFFFFFFFF);
        nudgeCard.setText(S.t("خلّ تلفزيونك يفتح على UrTV"));
        nudgeCard.setGravity(Gravity.CENTER);
        nudgeCard.setSingleLine(true);
        nudgeCard.setPadding(dp(10), dp(4), dp(10), dp(4));
        nudgeCard.setMaxWidth(dp(420));
        nudgeCard.setFocusable(true);
        nudgeCard.setClickable(true);
        nudgeCard.setId(View.generateViewId());
        nudgeCard.setOnClickListener(v -> {
            if (nudgeIsNotice) { dismissNotice(); return; }   // 4.3.3: «تم» on a notice
            if (nudgeIsResume) { guarded(ParentalPolicy.PILL_LISTEN, null, "تلاوة وأذان", () -> { sa.urtv.home.tilawah.MusicService.resumeCutOff(this); updateHomeNudge(false); }); return; }   // goes on only when the person pressed it
            if (nudgeIsUpdate) { launchGuarded(ParentalPolicy.PILL_UPDATES, new Intent(this, UpdateActivity.class), "تحديث تلفزيوني"); return; }   // 4.3.1: the builds differ
            if (nudgeIsCity) { openCity(); return; }   // 4.3.1: the same bar asks for the city while it is due
            if (nudgeIsStock) {   // 4.4: the TV's own «Home app» choice (no adb needed); where the TV has none, «إعداد تلفزيوني» and its «تراجع»
                sa.urtv.home.setup.SetupHook.stockTaken(this);   // review A2-F1: HOME_WANTED goes too
                Intent hs = new Intent(android.provider.Settings.ACTION_HOME_SETTINGS);
                if (getPackageManager().resolveActivity(hs, 0) != null) launchGuarded(ParentalPolicy.PILL_TV_SETTINGS, hs, "إعدادات التلفزيون");
                else guarded(ParentalPolicy.WIDGET_MATCHES, null, S.t("إعداد تلفزيوني"), () -> launch(new Intent(this, sa.urtv.home.setup.SetupActivity.class)));
                return;
            }
            if (nudgeIsOverlay) { launchGuarded(ParentalPolicy.PILL_TV_SETTINGS, Extras.intent(this, "overlay"), "إضافات تلفزيونك"); overlayAfterOffer(); return; }   // 4.4: the add-on's own offer for «فوق تطبيق آخر»; the bar is done either way
            if (nudgeIsExtras) {   // 4.4: we heal it where we may write the list; else the TV's accessibility settings, behind the sleep lock
                if (A11yHeal.canHeal(this)) { A11yHeal.check(getApplicationContext(), "fix-nudge"); android.widget.Toast.makeText(this, S.t("شغّلنا «إضافات تلفزيونك» من جديد"), android.widget.Toast.LENGTH_LONG).show(); handler.postDelayed(() -> updateHomeNudge(false), 4000); return; }
                android.widget.Toast.makeText(this, S.tv(ExtrasActivity.STUCK_FIX), android.widget.Toast.LENGTH_LONG).show();
                launchGuarded(ParentalPolicy.SLEEP_ACCESSIBILITY, new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS), "إعدادات التلفزيون");
                return;
            }
            try { guarded(ParentalPolicy.WIDGET_MATCHES, null, S.t("إعداد تلفزيوني"), () -> launch(new Intent(this, sa.urtv.home.setup.SetupActivity.class))); } catch (RuntimeException ignored) { }
        });
        nudgeLater = text(NUDGE_SP, "sans-serif-medium", 0xFFFFFFFF);
        nudgeLater.setText(S.t("لاحقاً"));
        nudgeLater.setGravity(Gravity.CENTER);
        nudgeLater.setPadding(dp(10), dp(4), dp(10), dp(4));
        nudgeLater.setFocusable(true);
        nudgeLater.setClickable(true);
        nudgeLater.setId(View.generateViewId());
        nudgeLater.setOnClickListener(v -> snoozeNudge());
        nudgeAsk = text(NUDGE_SP, "regular", 0xFFFFFFFF);   // 4.4 the owner («make it small»): the home's secondary size
        nudgeAsk.setText(S.tv(CITY_ASK));
        nudgeAsk.setMaxLines(2);   // never «…»: the city question is one short line; a longer notice wraps (keepNudgeClear keeps the strip to NUDGE_MAX of the screen)
        nudgeAsk.setPadding(dp(10), dp(4), dp(6), dp(4));
        nudgeAsk.setGravity(Gravity.CENTER);
        nudgeAsk.setVisibility(View.GONE);
        nudge.addView(nudgeAsk, new LinearLayout.LayoutParams(-2, -2));
        nudge.addView(nudgeCard, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(-2, -2);
        lp2.setMarginStart(dp(2));
        nudge.addView(nudgeLater, lp2);
        nudge.setPadding(dp(4), dp(2), dp(4), dp(2));
        nudge.setVisibility(View.GONE);
        root.addView(nudge, lp(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, dp(30), 0, 0));
        nudge.addOnLayoutChangeListener((nv, n1, n2, n3, n4, n5, n6, n7, n8) -> keepNudgeClear());   // and the weather line's own (where it is made)
    }

    /** 4.4 the sweep: px at 1080p between the nudge strip and what stands beside it (the top row of buttons, the weather line under «صباح الخير») */
    static final int NUDGE_CLEAR_PX = 16;
    /** 4.4 the owner («the notification, make it small»): the strip's text size (the home's secondary size), its widest share of the screen */
    static final int NUDGE_SP = 12;
    static final float NUDGE_MAX = 0.40f;
    /** the city question, short enough for one line of the compact strip (never cut with «…») */
    static final String CITY_ASK = S.n("اختر مدينتك للطقس والصلاة");

    /**
     * The compact strip sits on the top row: right after the row of round buttons (on its end side), its middle on the row's middle, NUDGE_CLEAR_PX
     * from it, at most NUDGE_MAX of the screen wide (a longer notice wraps to two lines), and never over the weather line or the clock beside it.
     */
    private void keepNudgeClear() {
        if (nudge == null) return;
        if (nudge.getTranslationX() != 0f) nudge.setTranslationX(0f);
        if (nudge.getTranslationY() != 0f) nudge.setTranslationY(0f);
        if (nudge.getVisibility() != View.VISIBLE || nudge.getWidth() == 0) return;
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int askMax = Math.max(dp(120), Math.round(screenW * NUDGE_MAX) - (nudge.getWidth() - nudgeAsk.getWidth()));
        if (nudgeAsk.getVisibility() == View.VISIBLE && nudgeAsk.getMaxWidth() != askMax) { nudgeAsk.setMaxWidth(askMax); nudge.requestLayout(); return; }   // laid out again, then placed
        int[] n = new int[2];
        nudge.getLocationInWindow(n);
        int gap = Math.round(NUDGE_CLEAR_PX * screenW / 1920f);
        float dx = 0f, dy = 0f;
        if (pillBar != null && pillBar.getVisibility() == View.VISIBLE && pillBar.getWidth() > 0) {
            int[] b = new int[2];
            pillBar.getLocationInWindow(b);
            boolean barLeft = b[0] + pillBar.getWidth() / 2 < screenW / 2;
            dx = barLeft ? b[0] + pillBar.getWidth() + gap - n[0] : b[0] - gap - (n[0] + nudge.getWidth());
            dy = b[1] + pillBar.getHeight() / 2f - (n[1] + nudge.getHeight() / 2f);
        }
        if (weatherLine != null && weatherLine.getVisibility() == View.VISIBLE && weatherLine.getWidth() > 0) {   // never over the weather line (nor the clock block it heads)
            int[] w = new int[2];
            weatherLine.getLocationInWindow(w);
            float left = n[0] + dx, right = left + nudge.getWidth();
            if (w[0] + weatherLine.getWidth() / 2 > right - nudge.getWidth() / 2f) dx += Math.min(0f, w[0] - gap - right);
            else dx += Math.max(0f, w[0] + weatherLine.getWidth() + gap - left);
        }
        if (dx != 0f) nudge.setTranslationX(dx);
        if (dy != 0f) nudge.setTranslationY(dy);
    }

    /** «لاحقاً» (or Back on the bar): hidden for 7 days, then 30 after a second press, then never; the number of presses is kept */
    private void snoozeNudge() {
        if (nudgeIsNotice) { if (!noticeFixed) dismissNotice(); return; }   // 4.3.3: Back on a notice is «تم»
        android.content.SharedPreferences sp = getSharedPreferences(NUDGE_PREFS, MODE_PRIVATE);
        if (nudgeIsResume) { sa.urtv.home.tilawah.MusicService.forgetCutOff(this); updateHomeNudge(false); return; }   // «لاحقاً»: the offer is dropped
        if (nudgeIsOverlay) { overlayAfterOffer(); return; }   // 4.4: «لاحقاً» counts as shown: the offer is made once
        if (nudgeIsStock) { sa.urtv.home.setup.SetupHook.clearStockBackOffer(this); updateHomeNudge(false); return; }   // 4.4: «لاحقاً»: not asked again until two more bad boots
        if (nudgeIsUpdate) { updateCardOnScreen = -1; updateHomeNudge(false); return; }   // 4.4: the card was shown once; «لاحقاً» hides it (it comes back for a newer build or after 3 days)
        String k = nudgeIsCity ? "city_" : nudgeIsExtras ? "extras_" : "";   // the city question and the add-on's fix wait on their own counts
        int presses = sp.getInt(k + "later_count", 0);
        sp.edit().putLong(k + "later_until", HomeNudgeRule.laterUntil(System.currentTimeMillis(), presses)).putInt(k + "later_count", presses + 1).apply();
        updateHomeNudge(false);
    }

    private boolean nudgeIsResume;    // the bar offers «كمّل الإذاعة» / «كمّل التلاوة»: the sound was cut off with the home's process (ResumeOffer)
    private boolean nudgeIsCity;      // the bar is asking for the city (CityAskRule), not for «إعداد تلفزيوني»
    private boolean nudgeIsUpdate;    // 4.4: the bar is the card «في تحديث لـ …» (UpdateNotice.card); OK opens «تحديث تلفزيوني»
    private boolean nudgeIsStock;     // 4.4: the bar offers «رجّع الواجهة الأصلية» after two bad boots of ours (SetupHook.stockBackOffered)
    private boolean nudgeIsOverlay;   // 4.4: the bar offers the add-on for «فوق تطبيق آخر» once (OverlayOfferRule)
    private static volatile boolean overlayDue;   // set on a thread (checkOverlayOffer); read on the UI thread by the bar; per process, so a recreated home still shows it
    private static volatile boolean overlayChecked;   // a real answer was got in this process: not asked again
    private static final java.util.concurrent.atomic.AtomicBoolean overlayChecking = new java.util.concurrent.atomic.AtomicBoolean();
    private long overlayShownAt;   // when this visit first showed the bar (elapsedRealtime)
    private boolean overlayOnScreen;   // this visit shows the offer (it was marked seen as it first appeared)
    private boolean nudgeIsExtras;    // 4.4: the bar says «إضافات تلفزيونك» is on but not running (A11yHeal.stuck), OK fixes it
    private long updateCardOnScreen = -1;   // 4.4: the build the card asks for while this visit shows it (-1: none); it stays up for the visit, then it was seen
    private boolean nudgeIsNotice, noticeFixed;   // 4.3.3: the bar shows a published notice (NoticeRule); fixed: one that is not dismissible (no button)
    private String noticeOnScreen;   // the notice's id while this visit shows it; null once the home goes behind (it was seen) or «تم»

    /** 4.3.3 the notice due now (NoticeRule), never while our tilawah or adhan plays; its id is marked seen as it first appears */
    /** 4.4 (QA-10, R1): the notices file, loaded off the UI thread (the minute tick's reads, and each return) */
    private final ContentFile.Kept notices = new ContentFile.Kept(NoticeRule.NAME);

    private NoticeRule.Notice dueNotice() {
        if (sa.urtv.home.tilawah.MusicService.carryOurPlaying()) return null;   // never over playback
        ContentFile.refresh(getApplicationContext(), NoticeRule.PATH, NoticeRule.NAME, 3 * 3600_000L);
        android.content.SharedPreferences np = getSharedPreferences(NUDGE_PREFS, MODE_PRIVATE);
        java.util.Set<String> seen = np.getStringSet("notice_seen", java.util.Collections.<String>emptySet());
        String day = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(new java.util.Date());
        NoticeRule.Notice n = NoticeRule.pick(notices.value(), day, BuildConfig.MARKETING, seen, noticeOnScreen);   // 4.4 (QA-10): the kept copy (loaded on the tick's thread)
        if (n != null && !n.id.equals(noticeOnScreen)) {
            noticeOnScreen = n.id;
            java.util.Set<String> s = new java.util.HashSet<>(seen);
            s.add(n.id);
            np.edit().putStringSet("notice_seen", s).apply();   // once per id: this visit only
        }
        return n;
    }

    private void dismissNotice() {
        noticeOnScreen = null;
        updateHomeNudge(false);
    }

    /** the build (versionCode) of the screensaver app, 0 when it is not installed */
    private long saverBuild() {
        try {
            android.content.pm.PackageInfo p = getPackageManager().getPackageInfo(SAVER_PKG, 0);
            return android.os.Build.VERSION.SDK_INT >= 28 ? p.getLongVersionCode() : (long) p.versionCode;
        } catch (Exception e) { return 0; }
    }

    /** 4.3.1: the one line when the home and the screensaver are not the same build (a Downloader update brings the home alone); null when they are, or no screensaver */
    private String buildMismatchLine() {
        int m = VersionMismatchRule.which(BuildConfig.VERSION_CODE, saverBuild());
        return m == VersionMismatchRule.SAVER_OLDER ? S.t("حدّث شاشة التوقف عشان تطلع الألعاب والمزايا الجديدة")
            : m == VersionMismatchRule.HOME_OLDER ? S.t("حدّث الشاشة الرئيسية عشان تعمل مزايا شاشة التوقف الجديدة") : null;
    }
    private boolean cityChosenNow = true;   // from the screensaver's last answer: true when it said nothing, so nobody is asked
    private boolean nudgeFocused;   // the bar took the focus once in this run of the home

    /**
     * the remote's way to the bar: UP from every top-row button reaches «خلّ تلفزيونك…», DOWN from the bar goes back to the first button, and the bar's own two buttons are side by side (the order
     * follows the screen's direction). Called whenever the bar shows or hides and after wireFocus sets the buttons' own ids.
     */
    private void wireNudge() {
        if (nudge == null || pills == null || pills.isEmpty()) return;
        boolean on = nudge.getVisibility() == View.VISIBLE && !noticeFixed;   // a notice without a button: nothing up there to reach
        for (View p : pills) p.setNextFocusUpId(on ? nudgeCard.getId() : p.getId());
        int pill0 = pills.get(0).getId();
        nudgeCard.setNextFocusUpId(nudgeCard.getId());
        nudgeLater.setNextFocusUpId(nudgeLater.getId());
        nudgeCard.setNextFocusDownId(pill0);
        nudgeLater.setNextFocusDownId(pill0);
        boolean rtl = Dir.rtl();   // the card is first in the row: at the start side (the right in Arabic)
        nudgeCard.setNextFocusRightId(rtl ? nudgeCard.getId() : nudgeLater.getId());
        nudgeCard.setNextFocusLeftId(rtl ? nudgeLater.getId() : nudgeCard.getId());
        nudgeLater.setNextFocusRightId(rtl ? nudgeCard.getId() : nudgeLater.getId());
        nudgeLater.setNextFocusLeftId(rtl ? nudgeLater.getId() : nudgeCard.getId());
    }

    /**
     * 4.4: the overlay offer, decided OFF the UI thread (the screensaver's settings and the add-on are IPC, the prefs are disk): the kick-off alert is on and set to
     * «فوق تطبيق آخر», and the add-on cannot draw. The result is applied on the UI thread; the bar then shows it (once, not in kids mode, not during a setup).
     */
    private void checkOverlayOffer() {
        final Context app = getApplicationContext();
        if (overlayDue || overlayOnScreen || overlayChecked) return;   // once per process: a real answer is kept, so a TV that never gets the offer does not wake the saver and the add-on at every return
        if (!overlayChecking.compareAndSet(false, true)) return;
        new Thread(() -> {   // its own thread: a cold start of the saver's or the add-on's process must not hold up the clock and the photo queued on the home's reads thread
            try {
                int shown = app.getSharedPreferences(NUDGE_PREFS, MODE_PRIVATE).getInt(OverlayOfferRule.KEY, 0);
                if (shown >= OverlayOfferRule.VERSION) { overlayChecked = true; return; }   // the cheap check first: no IPC for a TV that was offered
                boolean[] set = KickoffReceiver.settings(app);
                boolean kids = Parental.state(app).kids, setup = sa.urtv.home.setup.SetupHold.on();
                Boolean known = set[0] && set[2] && !kids && !setup ? Extras.canDrawKnown() : Boolean.FALSE;
                if (known == null) return;   // the add-on gave no answer: not «cannot draw»; asked again at the next return
                overlayChecked = true;
                if (!OverlayOfferRule.due(set[0], set[2], known, kids, setup, shown)) return;
                overlayDue = true;
                runOnUiThread(() -> { if (!isDestroyed() && !isFinishing() && resumed) updateHomeNudge(false); });
            } catch (RuntimeException ignored) {
            } finally { overlayChecking.set(false); }
        }, "overlay-offer").start();
    }

    /** the offer was answered («شغّلها» or «لاحقاً»): never again for this version */
    private void overlayAfterOffer() {
        overlayDue = false;
        overlayOnScreen = false;
        markOverlayOffered();
        updateHomeNudge(false);
    }

    private void markOverlayOffered() {
        final Context app = getApplicationContext();
        clockReads.execute(() -> app.getSharedPreferences(NUDGE_PREFS, MODE_PRIVATE).edit().putInt(OverlayOfferRule.KEY, OverlayOfferRule.VERSION).apply());
    }

    /** as the home comes to the front: show or hide the bar; {@code takeFocus} puts the focus on it the first time it appears (after the home has laid out and put its own focus), so OK is all it takes */
    private void updateHomeNudge(boolean takeFocus) {
        if (nudge == null) return;
        if (cleanShown) { nudge.setVisibility(View.GONE); wireNudge(); return; }   // «UrTV النظيف»: no notices
        android.content.SharedPreferences np = getSharedPreferences(NUDGE_PREFS, MODE_PRIVATE);
        long nowMs = System.currentTimeMillis();
        boolean kids = Parental.state(this).kids;
        String mismatch = kids ? null : buildMismatchLine();
        // 4.4: «في تحديث لـ …», always on: any of our apps behind another or the person's channel (UpdateNotice), once (again for a newer build or after 3 days), never
        // in kids mode, never over our recitation or the adhan
        boolean quiet = sa.urtv.home.tilawah.MusicService.carryOurPlaying() || sa.urtv.home.tilawah.NowPlaying.adhanLive();
        // 4.4: after two bad boots of ours in a row (SetupBootReceiver.homeGuard) «رجّع الواجهة الأصلية» comes first, before anything else the bar has to say
        boolean askStock = sa.urtv.home.setup.SetupHook.stockBackOffered(this);
        UpdateNotice.Card card = kids || quiet || askStock ? null : UpdateNotice.card(this, updateCardOnScreen, nowMs);
        boolean askUpdate = card != null;
        if (card != null && updateCardOnScreen != card.target) { updateCardOnScreen = card.target; UpdateNotice.shown(this, card.target, nowMs); }
        // 4.4: «إضافات تلفزيونك» turned on but Android does not run it (a TCL refused the bind; a force-stop): the one-line fix, once the TV has been up a while
        boolean askExtras = !askStock && !askUpdate && !kids && android.os.SystemClock.elapsedRealtime() > 90_000 && A11yHeal.stuck(this)
            && HomeNudgeRule.show(false, false, np.getLong("extras_later_until", 0), nowMs);
        // 4.4: «فوق تطبيق آخر» moved to the add-on: offered once to whoever used it (checkOverlayOffer), after the problems above, before the city
        boolean askOverlay = (overlayDue || overlayOnScreen) && !askStock && !askUpdate && !askExtras && !kids && !quiet && !sa.urtv.home.setup.SetupHold.on();
        if (askOverlay && !overlayOnScreen) { overlayOnScreen = true; overlayShownAt = android.os.SystemClock.elapsedRealtime(); }   // marked seen when the person answers it or leaves after a few seconds of seeing it (onPause), not on first appearance
        boolean askResume = !askStock && !askUpdate && !askExtras && !askOverlay && !kids && !cleanShown && sa.urtv.home.tilawah.MusicService.cutOff(this);   // the sound was cut off (the process ended): offered, never started by itself
        boolean askCity = !askStock && !askUpdate && !askExtras && !askOverlay && !askResume && mismatch == null && CityAskRule.show(isInstalled(SAVER_PKG), cityChosenNow, java.util.TimeZone.getDefault().getID(), kids, np.getLong("city_later_until", 0), nowMs);
        // 4.4: no bar asks to make UrTV the home any more (it is not pushed: the settings' row «خلّ UrTV هي الشاشة الرئيسية» is the way in for whoever wants it)
        boolean show = askStock || askUpdate || askExtras || askOverlay || askResume || askCity;   // never two at once: the stock home back, the update, the add-on, then the city
        NoticeRule.Notice notice = show || kids ? null : dueNotice();   // 4.3.3: a notice only when the bar has nothing else to say
        show |= notice != null;
        boolean was = nudge.getVisibility() == View.VISIBLE;
        nudgeIsCity = askCity;
        nudgeIsUpdate = askUpdate;
        nudgeIsExtras = askExtras;
        nudgeIsOverlay = askOverlay;
        nudgeIsResume = askResume;
        nudgeIsStock = askStock;
        nudgeIsNotice = notice != null;
        noticeFixed = notice != null && !notice.dismissible;
        nudgeCard.setText(notice != null ? S.t("تم") : askStock ? S.t("رجّع الواجهة الأصلية") : askResume ? (sa.urtv.home.tilawah.MusicService.cutOffWasRadio(this) ? S.t("كمّل الإذاعة") : S.t("كمّل التلاوة")) : askUpdate ? S.t("حدّث الآن") : askExtras ? S.t("أصلحها") : askOverlay ? S.t("شغّلها") : askCity ? S.t("اختر المدينة") : S.t("خلّ تلفزيونك يفتح على UrTV"));
        if (notice != null) nudgeAsk.setText(notice.text(S.en()));
        else if (askStock) nudgeAsk.setText(S.t("تلفزيونك ما فتح بعد آخر تشغيلين للتلفزيون"));
        else if (askResume) nudgeAsk.setText(sa.urtv.home.tilawah.MusicService.cutOffWasRadio(this) ? S.t("توقفت الإذاعة فجأة") : S.t("توقفت التلاوة فجأة"));
        else if (askUpdate) nudgeAsk.setText(card.line); else if (askExtras) nudgeAsk.setText(S.tv(ExtrasActivity.STUCK_FIX)); else if (askOverlay) nudgeAsk.setText(S.t("«فوق تطبيق آخر» صار في «إضافات تلفزيونك»: شغّلها ليبقى التنبيه فوق التطبيق")); else if (askCity) nudgeAsk.setText(S.tv(CITY_ASK));
        nudgeAsk.setVisibility(askStock || askUpdate || askExtras || askOverlay || askResume || askCity || notice != null ? View.VISIBLE : View.GONE);
        nudgeCard.setVisibility(noticeFixed ? View.GONE : View.VISIBLE);
        nudgeLater.setVisibility(notice != null ? View.GONE : View.VISIBLE);
        if (show && !was) {
            GlassStyle.Look l = homeGlass();
            // 4.4 the owner (no pills): ONE glass strip, plain words in it; only the focused choice has a fill (the focus ring), none a capsule
            nudge.setBackground(GlassPaint.fill(this, opaque(l.dialog), NUDGE_RADIUS, l.line));
            nudgeAsk.setBackground(null);
            nudgeAsk.setTextColor(l.text);
            nudgeCard.setBackground(focusOnly(NUDGE_FOCUS_RADIUS));
            nudgeCard.setTextColor(GlassPaint.focusColors(l.text, l.onFocus));
            nudgeLater.setBackground(focusOnly(NUDGE_FOCUS_RADIUS));
            nudgeLater.setTextColor(GlassPaint.focusColors(l.text, l.onFocus));
        }
        nudge.setVisibility(show ? View.VISIBLE : View.GONE);
        wireNudge();
        if (show && takeFocus && !nudgeFocused && notice == null) nudge.post(() -> { if (nudge.getVisibility() == View.VISIBLE && !nudgeFocused) { nudgeFocused = true; nudgeCard.requestFocus(); } });
        else if (!show && was && (nudgeCard.hasFocus() || nudgeLater.hasFocus()) && !dockCards.isEmpty()) dockCards.get(0).requestFocus();
    }

    /** «تراجع»: a gold chip, filled while it has the focus. */
    private TextView undoChipView() {
        TextView chip = text(14, "sans-serif-medium", 0xFFFFFFFF);
        chip.setText(S.t("تراجع"));
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(22), dp(8), dp(22), dp(8));
        chip.setFocusable(true);
        chip.setClickable(true);
        chip.setId(View.generateViewId());
        chip.setVisibility(View.GONE);   // its glass: paintEditChrome
        return chip;
    }

    // ---------------------------------------------------------------- the glass of the home's own small pieces

    /**
     * The glass look of the home's hint line, «تراجع», «المخفية» and its menus, in the look the home shows. They sit over the home's
     * photo as it is (not the blurred backdrop), so their fills are made opaque over the look's solid colour: GlassStyle checks exactly that.
     */
    private GlassStyle.Look homeGlass() { return GlassStyle.of(lookNow, lightNow); }

    private int opaque(int fill) { return GlassStyle.over(fill, homeGlass().base); }

    /** a piece's background: the opaque dialog glass at rest, the solid focus fill while focused */
    private android.graphics.drawable.StateListDrawable opaqueTile(float radiusDp) {
        GlassStyle.Look l = homeGlass();
        android.graphics.drawable.StateListDrawable s = new android.graphics.drawable.StateListDrawable();
        s.addState(new int[] {android.R.attr.state_focused}, GlassPaint.focused(this, l, GlassStyle.over(l.focus, opaque(l.dialog)), radiusDp));
        s.addState(new int[] {}, GlassPaint.fill(this, opaque(l.dialog), radiusDp, l.line));
        return s;
    }

    /** 4.4: the nudge strip's corners and its choices' focus fill: a rounded panel, never a capsule (half its height) */
    static final float NUDGE_RADIUS = 8, NUDGE_FOCUS_RADIUS = 6;   // 4.4: the compact strip is about 24 dp tall: 8 dp corners, never a capsule

    /** a plain word's background: nothing at rest, the solid focus fill while focused (its focus ring) */
    private android.graphics.drawable.StateListDrawable focusOnly(float radiusDp) {
        GlassStyle.Look l = homeGlass();
        android.graphics.drawable.StateListDrawable s = new android.graphics.drawable.StateListDrawable();
        s.addState(new int[] {android.R.attr.state_focused}, GlassPaint.focused(this, l, GlassStyle.over(l.focus, opaque(l.dialog)), radiusDp));
        s.addState(new int[] {}, new android.graphics.drawable.ColorDrawable(0));
        return s;
    }

    /** As a mode starts: the hint line, the two «تراجع» and «المخفية» in the look of now. */
    private void paintEditChrome() {
        GlassStyle.Look l = homeGlass();
        editHint.setBackground(GlassPaint.fill(this, opaque(l.hint), 18, l.line));
        editHint.setTextColor(l.text);
        for (TextView chip : new TextView[] {undoChip, appsUndoChip}) {
            chip.setBackground(opaqueTile(20));
            chip.setTextColor(GlassPaint.focusColors(l.text, l.onFocus));
        }
        if (hiddenCard != null) dressHiddenCard();
    }

    /** A small × in the corner of a favourite's card; it shows, and takes the focus, only while the row is being edited. */
    private void addEditMarks(FrameLayout card, GradientDrawable ring, View shine) {
        TextView x = text(16, "sans-serif-medium", 0xFFFFFFFF);
        x.setText("×");
        x.setGravity(Gravity.CENTER);
        x.setIncludeFontPadding(false);
        x.setFocusable(false);
        x.setVisibility(View.GONE);
        x.setId(View.generateViewId());
        GradientDrawable xb = new GradientDrawable();
        xb.setShape(GradientDrawable.OVAL);
        x.setBackground(xb);
        x.setOnFocusChangeListener((v, has) -> {
            xb.setColor(has ? CORAL : 0xB3000000);
            xb.setStroke(dp(2), has ? 0xFFFFD9CF : 0x66FFFFFF);
            x.animate().scaleX(has ? 1.3f : 1f).scaleY(has ? 1.3f : 1f).setDuration(Motion.move(MainActivity.this)).setInterpolator(Motion.ease()).start();
        });
        x.getOnFocusChangeListener().onFocusChange(x, false);
        card.addView(x, lp(dp(26), dp(26), Gravity.TOP | Gravity.START, dp(6), dp(6), dp(6), 0));
        xByCard.put(card, x);
        ringByCard.put(card, ring);
        shineByCard.put(card, shine);
    }

    /** The favourites as the row shows them now, by package. */
    private List<String> dockPackages() {
        List<String> out = new ArrayList<>();
        for (View c : dockCards) out.add(((App) c.getTag()).pkg);
        return out;
    }

    /** «تعديل المفضلة» from the long-press menu (on the app that was pressed) or from the settings (pkg null). The family lock asks first. */
    private void beginEdit(String pkg) {
        if (kidsNoEdit()) return;
        guarded(ParentalPolicy.MENU_ARRANGE, null, S.t("تعديل المفضلة"), () -> startEdit(pkg));
    }

    /**
     * Kids mode edits nothing (AppsEdit.allowed): it shows only the allowed apps, so a row or an order saved from it would lose the
     * others. True, with a word on the screen, when that is so.
     */
    private boolean kidsNoEdit() {
        if (AppsEdit.allowed(Parental.state(this).kids)) return false;
        Toast.makeText(this, S.t("ما فيه تعديل في وضع الأطفال"), Toast.LENGTH_SHORT).show();
        return true;
    }

    /** From the settings: once the favourites are on screen. */
    private void beginPendingEdit() {
        if (!wantEdit || dockCards.isEmpty()) return;
        wantEdit = false;
        beginEdit(null);
    }

    private void startEdit(String pkg) {
        if (edit != null || appsEdit != null || dockCards.isEmpty()) return;
        List<String> row = dockPackages();
        int focus = pkg != null ? row.indexOf(pkg) : -1;
        if (focus < 0) {
            View f = getCurrentFocus();
            for (int i = 0; i < dockCards.size(); i++) if (dockCards.get(i) == f) focus = i;
        }
        editStart = new ArrayList<>(row);
        edit = new FavEdit(row, MIN_DOCK, Math.max(0, focus));
        glideTo(0);
        editHint.setText(S.tv(FavEdit.HINT));
        paintEditChrome();
        editHint.setVisibility(View.VISIBLE);
        paintEdit();
    }

    /** Back: the order is saved where «ترتيب المفضلة» and dock.sh keep it, and the row is built again from it. */
    private void leaveEdit() {
        FavEdit e = edit;
        if (e.carrying()) e.back(SystemClock.uptimeMillis());   // an app still in the hand goes back where it was
        List<String> row = e.row();
        int at = row.isEmpty() ? 0 : Math.min(e.at(), row.size() - 1);
        edit = null;
        syncRow(row);
        endEditUi(at);
        if (e.changed()) {
            focusPkgAfter = row.get(at);
            Dock.save(this, InputFav.keepInputs(InputFav.unreported(Dock.saved(this), editStart), row, Dock.MAX));   // a saved input the TV did not report is not lost (review F100, second pass); one the person removed is   // the same store as «ترتيب المفضلة» and dock.sh; its broadcast builds the row from it
        }
    }

    /** The row changed under the mode (an app came or went, or dock.sh set it): nothing is saved, the new row is built. */
    private void abandonEdit() {
        edit = null;
        endEditUi(0);
    }

    private void endEditUi(int focus) {
        handler.removeCallbacks(undoExpiry);
        editHint.setVisibility(View.GONE);
        undoChip.setVisibility(View.GONE);
        for (View c : dockCards) {
            TextView x = xByCard.get(c);
            if (x != null) { x.setVisibility(View.GONE); x.setFocusable(false); }
        }
        wireFocus();   // the row's own ways again (up to the pills, down to the apps)
        if (!dockCards.isEmpty()) dockCards.get(Math.min(focus, dockCards.size() - 1)).requestFocus();
        for (View c : dockCards) liftCard(c, false);
    }

    /** One key while the mode is on. True: it is ours (FavEdit.owns: the arrows, OK, Back and what would move the focus; every other key goes on). */
    private boolean editKey(KeyEvent e) {
        int k = e.getKeyCode();
        if (AdhanKeys.reserved(k) || !FavEdit.owns(k)) return false;
        if (e.getAction() != KeyEvent.ACTION_DOWN) return true;   // a release does nothing; it must not click the tile under the focus
        long now = SystemClock.uptimeMillis();
        int rep = e.getRepeatCount();
        int d = FavEdit.step(k, dockRow.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
        if (d != 0) {
            if (!FavEdit.stepAllowed(rep, lastEditStep, e.getEventTime(), SIDE_REPEAT_MS)) return true;
            lastEditStep = e.getEventTime();
            applyEdit(edit.move(d, now));
        } else if (k == KeyEvent.KEYCODE_DPAD_UP) {
            if (!FavEdit.stepAllowed(rep, lastEditStep, e.getEventTime(), SIDE_REPEAT_MS)) return true;
            lastEditStep = e.getEventTime();
            applyEdit(edit.up(0, now));
        } else if (k == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (!FavEdit.stepAllowed(rep, lastEditStep, e.getEventTime(), SIDE_REPEAT_MS)) return true;
            lastEditStep = e.getEventTime();
            applyEdit(edit.down(now));
        } else if (AdhanKeys.isOk(k)) {
            if (FavEdit.oncePerPress(rep)) applyEdit(edit.ok(now));
        } else if (k == KeyEvent.KEYCODE_BACK) {
            if (FavEdit.oncePerPress(rep)) applyEdit(edit.back(now));
        }
        return true;
    }

    private void applyEdit(int result) {
        if (edit == null) return;
        switch (result) {
            case FavEdit.EXIT:
                leaveEdit();
                return;
            case FavEdit.TOO_FEW:
                Toast.makeText(this, S.t("المفضلة لا تقل عن %s تطبيقات", arabicDigits(String.valueOf(MIN_DOCK))), Toast.LENGTH_SHORT).show();
                break;
            case FavEdit.REMOVED:
                handler.removeCallbacks(undoExpiry);
                handler.postDelayed(undoExpiry, FavEdit.UNDO_MS + 50);
                break;
            default:
                break;
        }
        paintEdit();
    }

    /** Draws the model: the tiles in its order, the × on each, the lifted one, the chip, and the focus where the model has it. */
    private void paintEdit() {
        FavEdit e = edit;
        if (e == null) return;
        long now = SystemClock.uptimeMillis();
        e.expire(now);
        syncRow(e.row());
        boolean chip = e.undoAvailable(now);
        if (chip) {
            int dockTop = bottomBlock.getTop() + tray.getTop();
            FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) undoChip.getLayoutParams();
            p.bottomMargin = Math.max(dp(8), getResources().getDisplayMetrics().heightPixels - dockTop + dp(8));
            undoChip.setLayoutParams(p);
        }
        undoChip.setVisibility(chip ? View.VISIBLE : View.GONE);
        for (View c : dockCards) {
            TextView x = xByCard.get(c);
            if (x != null) { x.setVisibility(View.VISIBLE); x.setFocusable(true); }
        }
        if (dockCards.isEmpty()) return;
        wireEditFocus();
        // the focus first: a card gaining it animates to the ordinary focus size, and the lifted one must come after
        View target = e.onUndo() && chip ? undoChip : e.onX() ? xByCard.get(dockCards.get(e.at())) : dockCards.get(e.at());
        if (target != null && !target.isFocused() && !target.requestFocus() && target == undoChip)
            undoChip.post(() -> { if (edit != null && edit.onUndo() && undoChip.getVisibility() == View.VISIBLE) undoChip.requestFocus(); });   // once laid out
        for (int i = 0; i < dockCards.size(); i++) liftCard(dockCards.get(i), e.carrying() && i == e.at());
    }

    /** The dock row in this order of packages (tiles already built; a removed one stays out of the row but keeps its tile for «تراجع»). */
    private void syncRow(List<String> order) {
        List<View> now = new ArrayList<>(dockCards);
        boolean same = order.size() == now.size();
        for (int i = 0; same && i < order.size(); i++) same = ((App) now.get(i).getTag()).pkg.equals(order.get(i));
        if (same) return;
        dockRow.removeAllViews();
        dockCards.clear();
        for (int i = 0; i < order.size(); i++) {
            View card = cardByPkg.get(order.get(i));
            if (card == null) continue;
            View box = (View) card.getParent();
            if (box.getParent() instanceof ViewGroup) ((ViewGroup) box.getParent()).removeView(box);
            dockRow.addView(box, dockCell(i, order.size()));
            dockCards.add(card);
        }
        if (settingsBox != null) dockRow.addView(settingsBox);   // «أيقونات» (couch-tv): «الإعدادات» then «كل التطبيقات», the last two
        if (allAppsBox != null) dockRow.addView(allAppsBox);   // «زجاجي»: «كل التطبيقات» stays the last
        if (settingsBox != null || allAppsBox != null) relayDock();   // «كنبة»: «الإعدادات» alone at the end in the other ways
        wireFocus();
    }

    /** Picked up: bigger, a gold outline and a little off the row. Dropped: as any tile. */
    private void liftCard(View card, boolean lifted) { liftCard(card, lifted, FavBar.FOCUS_SCALE); }

    private void liftCard(View card, boolean lifted, float focusScale) {
        GradientDrawable ring = ringByCard.get(card);
        View shine = shineByCard.get(card);
        boolean fav = dockCards.contains(card);
        if (ring != null) ring.setStroke(lifted ? dp(4) : fav ? trayRingW() : dp(2), lifted ? GOLD : fav ? trayRing() : 0xE6FFFFFF);
        if (shine != null && lifted) shine.setAlpha(1f);
        if (lifted) {
            scaleTo(card, fav ? FavBar.LIFTED_SCALE : 1.25f);   // a favourite stays inside its tray
            card.animate().translationY(fav ? -lift() : -dp(10)).translationZ(dp(GlowLook.LIFT_DP)).setDuration(Motion.move(MainActivity.this)).setInterpolator(Motion.ease()).start();
        } else {
            boolean has = card.isFocused();
            float s = has ? focusScale : 1f;
            scaleTo(card, s);
            card.animate().translationY(has && fav ? -lift() : 0).translationZ(has ? dp(GlowLook.LIFT_DP) : 0).setDuration(Motion.move(MainActivity.this)).setInterpolator(Motion.ease()).start();
            if (shine != null) shine.setAlpha(has ? 1f : 0f);
        }
    }

    // ---------------------------------------------------------------- «تعديل التطبيقات»

    /** The settings' «ترتيب كل التطبيقات» asks the home screen for the mode, or for the order by name again, with these extras. */
    static final String EXTRA_EDIT_APPS = "edit_apps", EXTRA_RESET_APPS = "reset_apps_order";
    /** 4.3.4, with edit_apps: the settings' «التطبيقات المخفية»: the mode opens on «المخفية» with its list (people found no way back to a hidden app) */
    static final String EXTRA_APPS_HIDDEN = "apps_hidden";
    private boolean wantHiddenList;       // asked for with EXTRA_APPS_HIDDEN, waiting for the mode

    private AppsEdit appsEdit;            // null: the mode is off
    private boolean wantAppsEdit;         // asked for from the settings, waiting for the whole grid to be on screen
    private boolean gridDone;             // every row of the grid is built (addGridRows ran to the end)
    private TextView appsUndoChip;
    private LinearLayout hiddenBox;       // «المخفية»: the grid's last place while the mode is on and something is hidden
    private FrameLayout hiddenCard;
    private TextView hiddenCount;
    private final Map<String, ResolveInfo> hiddenFound = new HashMap<>();   // the apps hidden before the mode began
    private long lastAppsStep;
    private View appsLifted;              // the carried tile, drawn lifted
    private final Runnable appsUndoExpiry = () -> { if (appsEdit != null) { appsEdit.expire(SystemClock.uptimeMillis()); paintAppsEdit(); } };

    /** From the settings: the mode (edit_apps) or «رجوع للترتيب الأصلي» (reset_apps_order). True when the intent asked for one. */
    private boolean appsRequested(Intent i) {
        if (i == null) return false;
        if (i.getBooleanExtra(EXTRA_RESET_APPS, false)) {
            if (kidsNoEdit()) return true;
            guarded(ParentalPolicy.MENU_ARRANGE, null, S.t("ترتيب كل التطبيقات"), () -> {
                Dock.resetAppsOrder(this);   // its broadcast builds the grid again, by name
                Toast.makeText(this, S.t("رجع ترتيب «كل التطبيقات» للأصل (حسب الاسم)"), Toast.LENGTH_SHORT).show();
            });
            return true;
        }
        if (!i.getBooleanExtra(EXTRA_EDIT_APPS, false)) return false;
        wantAppsEdit = true;
        wantHiddenList = i.getBooleanExtra(EXTRA_APPS_HIDDEN, false);
        // 4.3.4: an app changed while the settings were in front: onResume (after this) would build the grid again and end the mode at once,
        // so the grid is built now and the mode starts when it is done
        if (appsStale) { appsStale = false; buildApps(); }
        beginPendingAppsEdit();
        return true;
    }

    /** «تعديل التطبيقات» from the long-press menu (on the app that was pressed) or from the settings (pkg null). The family lock asks first. */
    private void beginAppsEdit(String pkg) {
        if (pkg != null) wantHiddenList = false;   // 4.3.4 review F1: a settings ask for the hidden list that a PIN or kids mode stopped is not the long press's
        if (kidsNoEdit()) return;
        guarded(ParentalPolicy.MENU_ARRANGE, null, S.t("تعديل التطبيقات"), () -> startAppsEdit(pkg));
    }

    /** From the settings: once the whole grid is on screen. */
    private void beginPendingAppsEdit() {
        if (!wantAppsEdit || !gridDone) return;
        wantAppsEdit = false;
        beginAppsEdit(null);
    }

    /** The grid as it shows now, by package. */
    private List<String> gridPackages() {
        List<String> out = new ArrayList<>();
        for (View c : gridCards) out.add(((App) c.getTag()).pkg);
        return out;
    }

    private void startAppsEdit(String pkg) {
        if (appsEdit != null || edit != null) return;
        if (!gridDone) { wantAppsEdit = true; return; }   // the grid's last rows are still being built: once they are
        List<String> order = gridPackages();
        hiddenFound.clear();
        for (ResolveInfo ri : Dock.hiddenApps(this)) hiddenFound.put(ri.activityInfo.packageName, ri);
        boolean toHidden = wantHiddenList;
        wantHiddenList = false;
        if (toHidden && hiddenFound.isEmpty()) {
            Toast.makeText(this, S.t("ما فيه تطبيقات مخفية"), Toast.LENGTH_SHORT).show();
            return;
        }
        if (order.isEmpty() && hiddenFound.isEmpty()) {
            Toast.makeText(this, S.t("كل تطبيقاتك في المفضلة"), Toast.LENGTH_SHORT).show();
            return;
        }
        int focus = toHidden ? order.size() : pkg != null ? order.indexOf(pkg) : -1;   // order.size(): «المخفية» (AppsEdit)
        if (focus < 0) {
            View f = getCurrentFocus();
            for (int i = 0; i < gridCards.size(); i++) if (gridCards.get(i) == f) focus = i;
        }
        appsEdit = new AppsEdit(order, new ArrayList<>(hiddenFound.keySet()), cols, dockCards.size(), MAX_DOCK, Math.max(0, focus));
        editHint.setText(S.tv(AppsEdit.HINT));
        paintEditChrome();
        editHint.setVisibility(View.VISIBLE);
        paintAppsEdit();
        if (toHidden) grid.post(() -> { if (appsEdit != null && appsEdit.onHiddenList() && hiddenCard != null) showHiddenList(); });   // its list at once, once laid out
    }

    /** Back: the order, what was hidden or shown and the favourites are kept (Dock.saveApps, one broadcast that builds the screen again). */
    private void leaveAppsEdit() {
        AppsEdit e = appsEdit;
        if (e.carrying()) e.back(SystemClock.uptimeMillis());   // an app still in the hand goes back where it was
        List<String> order = e.order();
        appsEdit = null;
        syncGrid(order, false);
        String at = order.isEmpty() ? null : order.get(Math.min(e.at(), order.size() - 1));
        endAppsEditUi(at);
        if (!e.changed()) return;
        List<String> row = null;
        if (!e.toFavourites().isEmpty()) {   // at the end of the favourites, as many as they have room for
            row = dockPackages();
            for (String p : e.toFavourites()) if (!row.contains(p) && row.size() < MAX_DOCK) row.add(p);
        }
        focusPkgAfter = at;
        Dock.saveApps(this, e.moved() ? order : null, e.hiddenNow(), e.shownNow(), row);
    }

    /** The grid changed under the mode (an app came or went): nothing is saved, the new grid is built. */
    private void abandonAppsEdit() {
        appsEdit = null;
        endAppsEditUi(null);
    }

    private void endAppsEditUi(String focusPkg) {
        handler.removeCallbacks(appsUndoExpiry);
        editHint.setVisibility(View.GONE);
        appsUndoChip.setVisibility(View.GONE);
        if (hiddenBox != null && hiddenBox.getParent() instanceof ViewGroup) ((ViewGroup) hiddenBox.getParent()).removeView(hiddenBox);
        for (View c : gridCards) {
            TextView x = xByCard.get(c);
            if (x != null) { x.setVisibility(View.GONE); x.setFocusable(false); }
        }
        if (appsLifted != null) liftCard(appsLifted, false, 1.1f);
        appsLifted = null;
        wireFocus();   // the grid's own ways again
        View f = focusPkg == null ? null : cardByPkg.get(focusPkg);
        if (f != null && gridCards.contains(f) && homeGrid()) f.requestFocus();
        else if (!dockCards.isEmpty()) dockCards.get(0).requestFocus();
    }

    /**
     * 4.4 beta 4 (couch-tv, build 620: one DOWN on the home landed in the apps): «أيقونات» is one row ending with «كل التطبيقات», so the home has
     * no apps grid under it; DOWN from the row goes to the cards row or stays. «تعديل التطبيقات» shows the grid while it is on.
     */
    static boolean homeGrid(String appsLook, boolean appsEditOn) { return appsEditOn || !FavBar.ICONS.equals(appsLook); }

    private boolean homeGrid() { return homeGrid(appsLook, appsEdit != null); }

    /** One key while the mode is on. True: it is ours (FavEdit.owns), as in «تعديل المفضلة». */
    private boolean appsEditKey(KeyEvent e) {
        int k = e.getKeyCode();
        if (AdhanKeys.reserved(k) || !FavEdit.owns(k)) return false;
        if (e.getAction() != KeyEvent.ACTION_DOWN) return true;   // a release does nothing; it must not click the tile under the focus
        long now = SystemClock.uptimeMillis();
        int rep = e.getRepeatCount();
        int d = FavEdit.step(k, grid.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
        boolean arrow = d != 0 || k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN;
        if (arrow) {
            if (!FavEdit.stepAllowed(rep, lastAppsStep, e.getEventTime(), SIDE_REPEAT_MS)) return true;
            lastAppsStep = e.getEventTime();
            applyAppsEdit(d != 0 ? appsEdit.move(d, now) : k == KeyEvent.KEYCODE_DPAD_UP ? appsEdit.up(now) : appsEdit.down(now));
        } else if (AdhanKeys.isOk(k)) {
            if (FavEdit.oncePerPress(rep)) applyAppsEdit(appsEdit.ok(now));
        } else if (k == KeyEvent.KEYCODE_BACK) {
            if (FavEdit.oncePerPress(rep)) applyAppsEdit(appsEdit.back(now));
        }
        return true;
    }

    private void applyAppsEdit(int result) {
        AppsEdit e = appsEdit;
        if (e == null) return;
        switch (result) {
            case AppsEdit.EXIT:
                leaveAppsEdit();
                return;
            case AppsEdit.HIDDEN:
                handler.removeCallbacks(appsUndoExpiry);
                handler.postDelayed(appsUndoExpiry, FavEdit.UNDO_MS + 50);
                break;
            case AppsEdit.FAVOURITES_FULL:
                Toast.makeText(this, S.t("المفضلة ممتلئة (%s تطبيقات): احذف واحدًا منها بـ«تعديل المفضلة» أولاً", arabicDigits(String.valueOf(MAX_DOCK))), Toast.LENGTH_LONG).show();
                break;
            case AppsEdit.TO_FAVOURITES: {
                List<String> to = e.toFavourites();
                View card = cardByPkg.get(to.get(to.size() - 1));
                if (card != null) toFavouritesRow((App) card.getTag());
                break;
            }
            case AppsEdit.LIST:
                showHiddenList();
                return;
            default:
                break;
        }
        paintAppsEdit();
    }

    /** UP from the first row: the app's tile leaves the grid and a favourite's tile for it comes at the end of the row (kept on Back). */
    private void toFavouritesRow(App app) {
        View card = cardByPkg.get(app.pkg);
        View oldBox = card == null ? null : (View) card.getParent();
        if (oldBox != null && oldBox.getParent() instanceof ViewGroup) ((ViewGroup) oldBox.getParent()).removeView(oldBox);
        gridCards.remove(card);
        View added = tile(app, true);   // tile() puts it in dockCards and cardByPkg
        View endFirst = settingsBox != null && settingsBox.getParent() == dockRow ? settingsBox : allAppsBox;   // before the end tiles
        dockRow.addView(added, endFirst != null && endFirst.getParent() == dockRow ? dockRow.indexOfChild(endFirst) : dockRow.getChildCount());   // before «كل التطبيقات» of «زجاجي»
        for (int i = 0; i < dockRow.getChildCount(); i++) dockRow.getChildAt(i).setLayoutParams(dockCell(i, dockRow.getChildCount()));
        Toast.makeText(this, S.t("أُضيف إلى المفضلة: %s", app.label), Toast.LENGTH_SHORT).show();
    }

    /** «المخفية»: the hidden apps by name; one chosen comes back where the saved grid will show it. */
    private void showHiddenList() {
        AppsEdit e = appsEdit;
        Map<String, String> given = AppNames.given(this);
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        for (String p : e.hidden()) {
            labels.add(appLabel(p, given));
            actions.add(() -> {
                if (appsEdit == null) return;
                if (!ensureTile(p)) return;
                applyAppsEdit(appsEdit.show(p, placeFor(p), SystemClock.uptimeMillis()));
            });
        }
        showMenu(hiddenCard, labels, actions);
    }

    private String appLabel(String pkg, Map<String, String> given) {
        View c = cardByPkg.get(pkg);
        if (c != null && c.getTag() instanceof App) return ((App) c.getTag()).label;
        ResolveInfo ri = hiddenFound.get(pkg);
        return ri != null ? AppNames.label(this, ri, given) : pkg;
    }

    /** An app hidden before the mode has no tile yet: it gets one, the same as the grid's. */
    private boolean ensureTile(String pkg) {
        if (cardByPkg.containsKey(pkg)) return true;
        ResolveInfo ri = hiddenFound.get(pkg);
        if (ri == null) return false;
        tile(appOf(ri, getPackageManager(), AppNames.given(this)), false);   // not in a row yet: syncGrid places it
        return true;
    }

    /** Where the grid will show an app brought back once saved: by name while no order is kept, else at the end (AppsEdit.arrange). */
    private int placeFor(String pkg) {
        if (Dock.appsOrder(this) != null || appsEdit.moved()) return -1;
        String label = ((App) cardByPkg.get(pkg).getTag()).label;
        int at = 0;
        for (View c : gridCards) if (((App) c.getTag()).label.compareToIgnoreCase(label) < 0) at++;
        return at;
    }

    /** Draws the model: the grid in its order, the × on each tile, «المخفية», the lifted one, the chip, and the focus where the model has it. */
    private void paintAppsEdit() {
        AppsEdit e = appsEdit;
        if (e == null) return;
        long now = SystemClock.uptimeMillis();
        e.expire(now);
        syncGrid(e.order(), e.hiddenListShown());
        if (gridCards.isEmpty() && !e.hiddenListShown()) { leaveAppsEdit(); return; }   // the last app went to the favourites
        boolean chip = e.undoAvailable(now);
        appsUndoChip.setVisibility(chip ? View.VISIBLE : View.GONE);
        for (View c : gridCards) {
            TextView x = xByCard.get(c);
            if (x != null) { x.setVisibility(View.VISIBLE); x.setFocusable(true); }
        }
        if (hiddenCount != null) hiddenCount.setText(arabicDigits(String.valueOf(e.hidden().size())));
        wireAppsEditFocus();
        View tile = e.onHiddenList() ? hiddenCard : gridCards.get(Math.min(e.at(), gridCards.size() - 1));
        View target = e.onUndo() && chip ? appsUndoChip : e.onX() && !e.onHiddenList() ? xByCard.get(tile) : tile;
        if (target != null && !target.isFocused() && !target.requestFocus() && target == appsUndoChip)
            appsUndoChip.post(() -> { if (appsEdit != null && appsEdit.onUndo() && appsUndoChip.getVisibility() == View.VISIBLE) appsUndoChip.requestFocus(); });   // once laid out
        // only the carried tile and the one dropped are drawn again; the carried one each time, after the focus (a tile moved to
        // another row loses the focus and takes it again, and its focus size must not undo the lift)
        View lifted = e.carrying() && !e.onHiddenList() ? tile : null;
        if (appsLifted != null && appsLifted != lifted) liftCard(appsLifted, false, 1.1f);
        if (lifted != null) liftCard(lifted, true, 1.1f);
        appsLifted = lifted;
        View box = (View) tile.getParent();
        grid.post(() -> { if (appsEdit != null && box.getParent() != null) scrollToRow(box); });   // the carried tile may have changed rows
    }

    /** While the mode is on, Android's own focus search stays put: appsEditKey moves the focus itself; this is the net under it. */
    private void wireAppsEditFocus() {
        AppsEdit e = appsEdit;
        if (e == null) return;
        for (View c : gridCards) {
            TextView x = xByCard.get(c);
            stay(c);
            if (x != null) { stay(x); x.setNextFocusUpId(c.getId()); c.setNextFocusDownId(x.getId()); }
        }
        if (hiddenCard != null) stay(hiddenCard);
        stay(appsUndoChip);
    }

    private static void stay(View v) {
        v.setNextFocusUpId(v.getId());
        v.setNextFocusDownId(v.getId());
        v.setNextFocusLeftId(v.getId());
        v.setNextFocusRightId(v.getId());
    }

    /**
     * The grid in this order of packages, «المخفية» last when asked (tiles already built; a hidden one stays out of the grid but keeps its
     * tile for «تراجع»). Only the rows that differ are built again, so a step of the carried app redraws one or two rows.
     */
    private void syncGrid(List<String> order, boolean withHidden) {
        List<View> boxes = new ArrayList<>();
        gridCards.clear();
        for (String p : order) {
            View c = cardByPkg.get(p);
            if (c == null) continue;
            boxes.add((View) c.getParent());
            gridCards.add(c);
        }
        if (withHidden) boxes.add(hiddenBox());
        int rows = (boxes.size() + cols - 1) / cols;
        for (int r = 0; r < rows; r++) {
            LinearLayout row;
            if (r < grid.getChildCount()) row = (LinearLayout) grid.getChildAt(r);
            else {
                row = new LinearLayout(this);
                row.setClipChildren(false);
                row.setClipToPadding(false);
                grid.addView(row, llp(-1, -2, 0, 0, 0, dp(26)));
            }
            boolean same = row.getChildCount() == cols;
            for (int i = 0; same && i < cols; i++) {
                int j = r * cols + i;
                View has = row.getChildAt(i);
                same = j < boxes.size() ? has == boxes.get(j) : !(has instanceof LinearLayout);   // the end of the last row: padding
            }
            if (same) continue;
            row.removeAllViews();
            for (int i = 0; i < cols; i++) {
                int j = r * cols + i;
                if (j >= boxes.size()) { row.addView(new View(this), rowCell(i, cols, 1)); continue; }
                View b = boxes.get(j);
                if (b.getParent() instanceof ViewGroup) ((ViewGroup) b.getParent()).removeView(b);
                row.addView(b, rowCell(i, cols, -2));
            }
        }
        while (grid.getChildCount() > Math.max(rows, 0)) grid.removeViewAt(grid.getChildCount() - 1);
        appsLabel.setVisibility(boxes.isEmpty() ? View.GONE : View.VISIBLE);
        wireFocus();
    }

    /** «المخفية»: a tile like the apps', with the count of the hidden apps on it. */
    private LinearLayout hiddenBox() {
        if (hiddenBox != null) return hiddenBox;
        hiddenBox = new LinearLayout(this);
        hiddenBox.setOrientation(LinearLayout.VERTICAL);
        hiddenBox.setClipChildren(false);
        hiddenBox.setClipToPadding(false);
        hiddenCard = new AspectFrame(this);
        hiddenCard.setId(View.generateViewId());
        hiddenCard.setFocusable(true);
        LinearLayout in = new LinearLayout(this);
        in.setOrientation(LinearLayout.VERTICAL);
        in.setGravity(Gravity.CENTER);
        hiddenCount = text(26, "sans-serif-medium", Color.WHITE);
        hiddenCount.setGravity(Gravity.CENTER);
        TextView name = text(13, "sans-serif-medium", 0xCCFFFFFF);
        name.setText(S.tv(AppsEdit.HIDDEN_TITLE));
        name.setGravity(Gravity.CENTER);
        in.addView(hiddenCount);
        in.addView(name);
        hiddenCard.addView(in, new FrameLayout.LayoutParams(-1, -1));
        hiddenCard.setOnFocusChangeListener((v, has) -> scaleTo(v, has ? 1.1f : 1f));
        dressHiddenCard();
        hiddenBox.addView(hiddenCard, new LinearLayout.LayoutParams(-1, -2));
        TextView room = text(11, "sans-serif-medium", Color.WHITE);   // 4.4 beta 2: the room of a tile's name under it, so its row is as tall as the others
        room.setText(" ");
        room.setAlpha(0f);
        room.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        hiddenBox.addView(room, llp(-1, -2, 0, dp(10), 0, 0));
        return hiddenBox;
    }

    /** «المخفية» in the glass of now: the opaque glass, the solid focus fill and its ink while focused. */
    private void dressHiddenCard() {
        GlassStyle.Look l = homeGlass();
        hiddenCard.setBackground(opaqueTile(10));
        ViewGroup in = (ViewGroup) hiddenCard.getChildAt(0);
        ((TextView) in.getChildAt(0)).setTextColor(l.text);
        ((TextView) in.getChildAt(1)).setTextColor(l.sub);
        GlassPaint.follow(hiddenCard, l);
    }

    /** Only installed apps are kept, so the dock never holds a removed app's slot. */
    private void saveDock(List<String> dock, String focusPkg) {
        List<String> installed = new ArrayList<>();
        for (String p : dock) if (isInstalled(p) || InputFav.isInput(p)) installed.add(p);   // a favourite input stays (InputFav)
        String dockNow = android.text.TextUtils.join(",", installed);
        if (!dockNow.equals(prefs().getString("dock", null))) prefs().edit().putString("dock", dockNow).apply();   // P6: written when it changed, not at every build
        dockApps = installed.toArray(new String[0]);
        focusPkgAfter = focusPkg;
        buildApps();
    }

    private void showAppMenu(View anchor, String pkg, boolean inDock) {
        if (InputFav.isInput(pkg)) return;   // a favourite input has no app menu: it is taken off in «أسماء المصادر» or «تعديل المفضلة»
        guarded(ParentalPolicy.MENU, null, S.t("قائمة التطبيق"), () -> showAppMenuNow(anchor, pkg, inDock));
    }

    /**
     * The app's options (AppOptions, docs/UX-REVIEW.md): «انقل», «أخفِ», «اقفل», then ★ (in or out of the favourites) and ⓘ (the app's
     * details). What the old long-press menu also had lives where it can be seen: «تعديل المفضلة» and «تعديل التطبيقات» are «انقل»,
     * the update, «تلاوة وأذان» and the adhan are in the settings, the default favourites in «ترتيب المفضلة», the family lock in
     * «إعدادات تلفزيونك» ← المزيد and in «مركز التحكم». Kids mode moves and hides nothing (kidsNoEdit).
     */
    private void showAppMenuNow(View anchor, String pkg, boolean inDock) {
        List<String> dock = dockPackages();
        int i = dock.indexOf(pkg);
        boolean kids = Parental.state(this).kids;
        AppOptions.Spec o = new AppOptions.Spec();
        if (!kids) {
            o.move = () -> { if (inDock) beginEdit(pkg); else beginAppsEdit(pkg); };
            o.hide = () -> {
                if (inDock && dock.size() <= MIN_DOCK) { toast(S.t("المفضلة لا تقل عن %s", arabicDigits(String.valueOf(MIN_DOCK)))); return; }
                if (inDock) { dock.remove(pkg); saveDock(dock, dock.isEmpty() ? null : dock.get(Math.max(0, i - 1))); }
                Dock.hideOrShow(this, Collections.singletonList(pkg), true);   // back from «تعديل التطبيقات» ← «المخفية»
                toast(S.t("أُخفي، ويرجع من الإعدادات: «التطبيقات المخفية»"));   // 4.3.4: the old line named a button the menu does not have
            };
        }
        ParentalPolicy.State family = Parental.state(this);
        o.locked = family.locked.contains(pkg);
        o.lock = () -> {
            if (!Parental.state(this).pinSet) {   // no PIN yet: the family lock's own page sets one first
                launchGuarded(ParentalPolicy.PARENTAL_SCREEN, new Intent(this, ParentalActivity.class), "الرقابة الأبوية");
                return;
            }
            boolean now = Parental.state(this).locked.contains(pkg);
            Parental.confirm(this, this::adhanTakes, S.t("الرقابة الأبوية"), () -> {
                Parental.setLocked(this, pkg, !now);
                toast(now ? S.t("فُتح القفل") : S.t("قُفل، ويطلب الرمز"));
            }, null);
        };
        if (kids) {   // 4.3.3: in kids mode a lock per app means nothing (only the allowed apps show): the slot is the way out, «الرقابة الأبوية» behind the PIN
            o.lockLabel = KIDS_EXIT;
            o.lock = this::openParental;
        }
        o.favouriteNow = inDock;
        o.favourite = () -> {
            if (inDock) {
                if (dock.size() <= MIN_DOCK) { toast(S.t("المفضلة لا تقل عن %s", arabicDigits(String.valueOf(MIN_DOCK)))); return; }
                dock.remove(pkg);
                saveDock(dock, dock.get(Math.max(0, i - 1)));
            } else if (dock.size() >= MAX_DOCK) {
                toast(S.t("%s كحد أقصى", arabicDigits(String.valueOf(MAX_DOCK))));   // the 11th is refused with its hint (the tray holds ten at most)
            } else {
                dock.add(pkg);
                saveDock(dock, pkg);
            }
        };
        Intent details = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null));
        o.info = () -> guarded(ParentalPolicy.MENU_APP, pkg, AppActions.DETAILS, () -> launch(details));
        if (!kids && AppStore.canUninstall(this, pkg)) {   // «إلغاء التثبيت»: Android's own question (never for a system app, ours, or in kids mode); the family PIN first when it asks
            Intent del = new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", pkg, null)).putExtra(Intent.EXTRA_RETURN_RESULT, false);
            o.uninstall = () -> guarded(ParentalPolicy.MENU_APP, pkg, S.t("إلغاء التثبيت"), () -> launch(del));
        }
        Intent shop = AppStore.storeIntent(this, pkg);
        if (!kids && shop != null) o.store = () -> guarded(ParentalPolicy.MENU_APP, pkg, S.t("صفحته في المتجر"), () -> launch(shop));
        if (!kids && glassWay()) o.icon = () -> editIcon(pkg);   // 4.5 «تغيير الأيقونة»: the glass bar's icons only
        if (appOptions != null) appOptions.dismiss();
        appOptions = AppOptions.show(this, anchor, inDock ? 1.12f : 1.1f, homeGlass(), font("sans-serif-medium"), o);
    }

    static final String KIDS_EXIT = S.n("الرقابة الأبوية");

    /**
     * 4.3.3: kids mode always has a way out on the TV: «الرقابة الأبوية» (ParentalActivity asks the PIN itself, and «الخروج من وضع الأطفال» there asks
     * it again), from the long OK's box on any app, the Menu key, the star key, and a long Settings key where the screensaver app (its other door) is missing.
     */
    private void openParental() {
        launchGuarded(ParentalPolicy.PARENTAL_SCREEN, new Intent(this, ParentalActivity.class), "الرقابة الأبوية");
    }

    // ---- the hint beside a favourite's name: «اضغط مطوّلاً للخيارات», the first few times only (OPTIONS_HINT_TIMES), never after the box was opened

    /** how many times the hint shows at most, a favourite focused each time */
    static final int OPTIONS_HINT_TIMES = 6;
    /** the hint's line beside the name (built with the title) */
    private TextView optionsHint;
    static final String OPTIONS_HINT = S.n("اضغط مطوّلاً للخيارات");

    /** the count is per language: the six times in Arabic had used up the English home's hint (step 27: «Hold OK for options» never showed) */
    private static String hintKey() { return S.en() ? "options_hint_en" : "options_hint"; }

    /** the hint for this focus: shown while it has times left, counted once per favourite that takes the focus */
    private void optionsHint(boolean show) {
        if (optionsHint == null) return;
        if (prefs().getInt("options_hint_v", 0) < 2)   // the hint was made legible (step 28): everyone sees it again, six times, in either language (the Arabic count of a TV in use was long spent, step 29)
            prefs().edit().putInt("options_hint", 0).putInt("options_hint_en", 0).putInt("options_hint_v", 2).apply();
        int seen = prefs().getInt(hintKey(), 0);
        boolean on = show && seen < OPTIONS_HINT_TIMES && edit == null && appsEdit == null;
        if (on) prefs().edit().putInt(hintKey(), seen + 1).apply();
        optionsHint.setTextColor(palette().text);
        optionsHint.animate().alpha(on ? 1f : 0f).setDuration(Motion.MS).setInterpolator(Motion.ease()).start();
    }

    /** the box was opened: the hint has done its job */
    private void optionsHintUsed() {
        prefs().edit().putInt("options_hint", OPTIONS_HINT_TIMES).putInt("options_hint_en", OPTIONS_HINT_TIMES).apply();   // the box was opened, in either language: the person knows
        if (optionsHint != null) optionsHint.animate().alpha(0f).setDuration(Motion.MS).setInterpolator(Motion.ease()).start();
    }

    // ---- the favourites bar (FavBar): its tray, the name over the focused tile, and the one move it may make

    private TraySnap tray;
    private View nameRow, nameCard;
    private boolean trayLowered;

    /** The name (and the hint beside it) centred over the focused favourite, as it scrolls too; hidden when the focus is not in the bar. */
    private boolean nameShown;   // what the name row is fading to (placeName)
    private boolean nameHiding;  // the row is fading out now (placeName runs on every scroll step: one fade, not one per step)
    private void placeName() {
        if (nameRow == null || nameRow.getWidth() == 0) return;
        View f = getCurrentFocus();
        boolean inBar = f != null && (dockCards.contains(f) || f == allAppsCard || f == settingsCard) && edit == null;
        // 4.4 beta 4 (the supervisor, run 23: «يوتيوب» drawn over the match card): with the big clock the strip comes down to the apps row, over the place
        // of the name above the tray, so the name goes under the tile as in «تكبير وظل»
        boolean under = appsZoom || (bigClockOn && homeCard != null && homeCard.inRow());
        placeZoomName(under && inBar ? f : null);
        if (under) inBar = false;   // «تكبير وظل»: the name is under the tile, not above the tray
        // this runs on every step of the tray's scroll: a new animator each step (cancel and restart, an invalidation each) is exactly what makes a scroll janky;
        // the name's fade starts only when it has to change
        if (nameShown != inBar || (!inBar && nameRow.getAlpha() > 0f && !nameHiding)) {   // 4.4 beta 2: the row also fades when it was never shown (the zoom ways: it stayed beside the name over the icon)
            nameShown = inBar;
            nameHiding = !inBar;
            nameRow.animate().alpha(inBar ? 1f : 0f).setDuration(FavBar.MS).withEndAction(() -> nameHiding = false).start();
        }
        if (!inBar || nameCard == null) return;
        nameRow.setTranslationX(0);   // the name has ONE place: above the tray at its start edge (right in Arabic, left in English), never following a tile (build 180)
    }

    /** 4.4 «شكل التطبيقات: تكبير وظل»: the focused app's name under the tray, centred on its tile (null: hidden); made the first time it is needed */
    private TextView zoomName;
    private int zoomWaits;   // frames waited for the row's first layout (placeZoomName)
    private volatile boolean appsZoom;
    /** 4.4 «شكل التطبيقات»: FavBar.BANNERS, ICONS or TILES (today's tray); read off the main thread by appOf */
    private volatile String appsLook = FavBar.TILES;

    /** A and B have no tray (the row stands on the photo); C wears the home's material as always; the row's height is the way's own */
    private void applyAppsLook() {
        if (tray == null) return;
        tray.setBackground(FavBar.TILES.equals(appsLook) ? dockBand : null);
        int side = FavBar.padSide(appsLook, getResources().getDisplayMetrics().heightPixels);
        int sh = getResources().getDisplayMetrics().heightPixels;
        if (dockRow != null && glassWay()) {   // 4.5 «زجاجي»: the glass round the icons, room for the focused one to grow, the grid's rows
            int edge = FavBar.glassPad(sh, glassK) + FavBar.glassGrow(sh, glassK);
            dockRow.setPadding(edge, edge, edge, edge);
            glassBar.radius(FavBar.glassBarRadius(sh, glassK));
            glassBar.light(lightNow);
            glassBar.flat(Ram.low(this));   // a weak TV: a flat fill and its edge, no picture behind
            dockRow.glass(glassBar, glassRowsN, FavBar.glassCell(sh, glassK), FavBar.glassGap(sh, glassK), FavBar.glassPad(sh, glassK));
            readGlassBackdrop();
        } else if (dockRow != null) {
            dockRow.glass(null, 1, 0, 0, 0);
            dockRow.setPadding(side, bar(FavBar.PAD_TOP), side, bar(FavBar.PAD_TOP));
        }
        applyTrayHeight();
        fitTray();
    }

    // ---- 4.5 «زجاجي»: the bar's size and rows, its glass, and its last tile «كل التطبيقات»

    /** the bar's size factor and rows (FavBar.glassK, glassRows, from «حجم شريط tvOS» and «صفوف شريط tvOS») */
    /**
     * 4.4 beta 2 (the owner): «محاذاة التطبيقات», the apps row in the middle (the default), at the right or at the left of the screen, in every look
     * (the side is the screen's, the same in Arabic and English); a row wider than the screen scrolls with the focus as before. The clock never moves.
     */
    private String appsAlignNow = "center";
    private void alignApps(String align) {
        if (dockRow == null || align == null || align.equals(appsAlignNow)) return;
        appsAlignNow = align;
        dockRow.setGravity(FavBar.alignGravity(align));
        fitTray();
    }

    private boolean trayFitQueued;

    /**
     * 4.4 beta 4 (the tester, step 76: a dock of three apps left a wide empty bar): in «بطاقات», the one way with a bar, the bar is as wide as its
     * tiles and its own padding, placed where the apps are (the centre, the right or the left); a row wider than the screen keeps the full width and
     * scrolls as before. «كمّل المشاهدة» under it moves by the same amount (the same side margins, the same frame), so its cards stay under the cells.
     */
    private void fitTray() {
        trayFitQueued = false;
        if (tray == null || dockRow == null || !(tray.getLayoutParams() instanceof LinearLayout.LayoutParams) || !(tray.getParent() instanceof View)) return;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tray.getLayoutParams();
        View parent = (View) tray.getParent();
        int want = -1;
        int room = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
        if (FavBar.TILES.equals(appsLook) && parent.getWidth() > 0) {
            int w = dockRow.getPaddingLeft() + dockRow.getPaddingRight();
            for (int i = 0; i < dockRow.getChildCount(); i++) {
                View c = dockRow.getChildAt(i);
                ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) c.getLayoutParams();
                w += c.getMeasuredWidth() + m.leftMargin + m.rightMargin;
            }
            if (w > dockRow.getPaddingLeft() + dockRow.getPaddingRight() && w < room) want = w;
        }
        int g = FavBar.alignGravity(appsAlignNow) & Gravity.HORIZONTAL_GRAVITY_MASK;
        if (lp.width != want || lp.gravity != g) { lp.width = want; lp.gravity = g; tray.setLayoutParams(lp); }
        if (tvRowsBox != null && tvRowsBox.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {   // the row under it, the bar's own start and end
            int free = want < 0 ? 0 : Math.max(0, room - want);
            int left = FavBar.alignedLeft(appsAlignNow) ? 0 : FavBar.alignedRight(appsAlignNow) ? free : free / 2;   // the screen's own left and right (absolute, as alignGravity): named by FavBar, not written as a side here
            ViewGroup.MarginLayoutParams t = (ViewGroup.MarginLayoutParams) tvRowsBox.getLayoutParams();
            int l = bar(FavBar.SIDE) + left, r = bar(FavBar.SIDE) + free - left;
            if (t.leftMargin != l || t.rightMargin != r) { t.leftMargin = l; t.rightMargin = r; tvRowsBox.setLayoutParams(t); }
        }
    }

    private volatile float glassK = 1f;
    private int glassRowsN = 1;
    private final GlassBar glassBar = new GlassBar();
    /** «كل التطبيقات», the bar's last tile in «زجاجي» (not a favourite: never in dockCards); null in the other ways */
    private View allAppsBox, allAppsCard, settingsBox, settingsCard;
    /** «كنبة» (HomeCard.couch, known once the home's card is read): the row ends with «الإعدادات» in every way; a change builds the row again */
    private boolean appsCouch;
    static final int REQ_ALL_APPS = 451, REQ_ICON = 452;

    private boolean glassWay() { return FavBar.GLASS.equals(appsLook); }

    /** the bar's picture: the home's backdrop (GlassSource), read off the main thread, given to the bar on it */
    private void readGlassBackdrop() {
        if (!glassWay() || loader.isShutdown() || Ram.low(this)) return;   // a weak TV draws the bar flat: no picture read
        loader.execute(() -> {
            Bitmap b = GlassSource.backdrop(this);
            runOnUiThread(() -> { glassBar.backdrop(b); if (dockRow != null) dockRow.invalidate(); });
        });
    }

    /** every cell of the row its place again (the last has no end margin) */
    private void relayDock() {
        int n = dockRow.getChildCount();
        for (int i = 0; i < n; i++) dockRow.getChildAt(i).setLayoutParams(dockCell(i, n));
    }

    /** «زجاجي»: «كل التطبيقات» at the bar's end, a frosted squircle with a grid of four, its name over it while focused; OK opens the glass window */
    private void addAllAppsTile() {
        allAppsBox = null;
        allAppsCard = null;
        settingsBox = null;
        settingsCard = null;
        boolean icons = FavBar.ICONS.equals(appsLook);   // 4.4 beta 4 (couch-tv): «أيقونات» ends with «الإعدادات» and «كل التطبيقات» (their «المزيد») too
        // 4.4 beta 4 (the tester, build 654: «كنبة» with «تكبير», the row ended at the last app and nothing on the home said «الإعدادات»): in «كنبة»
        // every way of the row ends with «الإعدادات»
        boolean gear = icons || appsCouch;
        if (!glassWay() && !gear) return;
        int sh = getResources().getDisplayMetrics().heightPixels;
        int corner = icons ? FavBar.radius(FavBar.ICONS, sh) : glassWay() ? FavBar.glassRadius(sh, glassK) : FavBar.radius(appsLook, sh);
        if (gear) addSettingsTile(corner, glassWay() ? 1f : FavBar.aspect(appsLook));
        if (!glassWay() && !icons) { relayDock(); return; }   // «كل التطبيقات» stays the glass's and the icons' own
        AspectFrame card = new AspectFrame(this);
        card.ratio = 1f;
        card.setId(View.generateViewId());
        card.setFocusable(true);
        card.setClickable(true);
        card.setContentDescription(S.t("كل التطبيقات"));
        card.setClipToOutline(true);
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), corner); }
        });
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(corner);
        bg.setColor(0x38FFFFFF);
        bg.setStroke(Math.max(1, bar(1.5f)), 0x59FFFFFF);
        card.setBackground(bg);
        card.addView(new AllAppsMark(this), new FrameLayout.LayoutParams(-1, -1));
        card.setOnClickListener(v -> openAllApps());
        card.setOnFocusChangeListener((v, has) -> {
            scaleTo(v, has ? FavBar.ZOOM_SCALE : 1f);   /* the owner (19 Nov, no ring): the size is the focus, also on a weak TV */   // a weak TV: nothing grows
            v.animate().translationZ(has ? dp(2 * GlowLook.LIFT_DP) : 0).setInterpolator(Motion.ease()).setDuration(Motion.move(MainActivity.this)).start();
            if (!has) return;
            lastFocusedId = v.getId();
            nameCard = v;
            fadeGlow(0.35f);
            placeName();
        });
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setClipChildren(false);
        box.setClipToPadding(false);
        box.addView(card, new LinearLayout.LayoutParams(-1, -2));
        allAppsBox = box;
        allAppsCard = card;
        dockRow.addView(box);
        relayDock();
    }

    /** 4.4 beta 4 (couch-tv's bar ends with its settings and «More»): «الإعدادات» in «أيقونات», a quiet square with the gear, OK opens our settings */
    private void addSettingsTile(int corner, float ratio) {
        AspectFrame card = new AspectFrame(this);
        card.ratio = ratio;   // a square in «أيقونات» and «زجاجي», the apps' own card in the other ways
        card.setId(View.generateViewId());
        card.setFocusable(true);
        card.setClickable(true);
        card.setContentDescription(S.t("الإعدادات"));
        card.setClipToOutline(true);
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), corner); }
        });
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(corner);
        bg.setColor(0x38FFFFFF);   // a flat frosted fill, no ring (the owner's rule)
        card.setBackground(bg);
        ImageView gear = new ImageView(this);
        gear.setImageResource(R.drawable.ic_settings);
        gear.setColorFilter(Color.WHITE);
        gear.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int pad = Math.round(FavBar.px(FavBar.ICON, sh()) * 0.3f);
        gear.setPadding(pad, pad, pad, pad);
        card.addView(gear, new FrameLayout.LayoutParams(-1, -1));
        card.setOnClickListener(v -> openSettingsScreen());
        card.setOnFocusChangeListener((v, has) -> {
            scaleTo(v, has ? FavBar.ZOOM_SCALE : 1f);
            v.animate().translationZ(has ? dp(2 * GlowLook.LIFT_DP) : 0).setInterpolator(Motion.ease()).setDuration(Motion.move(MainActivity.this)).start();
            if (has) { lastFocusedId = v.getId(); nameCard = v; title.setText(S.t("الإعدادات")); placeName(); }   // the name above the tray is its own (run 21: the last app's stayed)
        });
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setClipChildren(false);
        box.setClipToPadding(false);
        box.addView(card, new LinearLayout.LayoutParams(-1, -2));
        settingsBox = box;
        settingsCard = card;
        dockRow.addView(box);
    }

    private int sh() { return getResources().getDisplayMetrics().heightPixels; }

    /** «تغيير الأيقونة» (IconEditorActivity); the bar is made again when it saved */
    private void editIcon(String pkg) {
        try { startActivityForResult(new Intent(this, IconEditorActivity.class).putExtra(IconEditorActivity.PKG, pkg).putExtra(IconEditorActivity.LOOK, lookNow).putExtra(IconEditorActivity.LIGHT, lightNow), REQ_ICON); }
        catch (RuntimeException e) { android.util.Log.w("UrTVHome", "icon editor", e); }
    }

    /** the glass window of every app (AllAppsActivity); the app chosen there opens here, through its own tile (the family lock, kids mode, the same launch) */
    private void openAllApps() {
        try { startActivityForResult(new Intent(this, AllAppsActivity.class).putExtra(AllAppsActivity.LOOK, lookNow).putExtra(AllAppsActivity.LIGHT, lightNow), REQ_ALL_APPS); }
        catch (RuntimeException e) { android.util.Log.w("UrTVHome", "all apps", e); }
    }

    @Override protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (req == REQ_ICON && result == RESULT_OK) { buildApps(); return; }   // «تغيير الأيقونة»: the bar again with the new icon
        if (req != REQ_ALL_APPS || result != RESULT_OK || data == null) return;
        String pkg = data.getStringExtra(AllAppsActivity.PKG);
        View c = pkg == null ? null : cardByPkg.get(pkg);
        if (c != null) c.performClick();
    }

    /** the four small squares of «كل التطبيقات» (drawn: no image) */
    static final class AllAppsMark extends View {
        private final android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        AllAppsMark(Context c) { super(c); p.setColor(0xF2FFFFFF); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), s = w * 0.17f, g = w * 0.08f, x0 = (w - 2 * s - g) / 2f, y0 = (getHeight() - 2 * s - g) / 2f, r = s * 0.3f;
            for (int i = 0; i < 4; i++) {
                float x = x0 + (i % 2) * (s + g), y = y0 + (i / 2) * (s + g);
                c.drawRoundRect(x, y, x + s, y + s, r, r, p);
            }
        }
    }

    private void placeZoomName(View f) {
        if (f == null) { if (zoomName != null && zoomName.getAlpha() > 0f) zoomName.animate().alpha(0f).setDuration(FavBar.MS).start(); return; }
        if (zoomName == null) {
            zoomName = text(24, "sans-serif-medium", Color.WHITE);
            zoomName.setTextSize(TypedValue.COMPLEX_UNIT_PX, bar(FavBar.NAME_PX) * 0.8f);
            zoomName.setShadowLayer(dp(12), 0, 0, 0x99000000);
            zoomName.setSingleLine(true);
            zoomName.setAlpha(0f);
            zoomName.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);   // the tile says its name itself
            hero.addView(zoomName, new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START));
            final View first = f;
            zoomName.post(() -> { if (first.hasFocus()) placeZoomName(first); });   // 4.4 beta 2: setX is right only once the name is laid out (its left was 0, and in Arabic it stood 140 px off its icon)
        }
        if (!f.isLaidOut() || f.getWidth() == 0 || !tray.isLaidOut()) {   // 4.4 beta 4 (run 27: «يوتيوب» at the screen's corner): the first focus came before the row was laid out
            if (zoomWaits++ < 30) f.post(() -> { if (f.hasFocus()) placeZoomName(f); });   // a few frames at most, never a loop
            return;
        }
        zoomWaits = 0;
        Object tag = f.getTag();
        if (tag instanceof App) zoomName.setText(((App) tag).label);
        else if (f == allAppsCard) zoomName.setText(S.t("كل التطبيقات"));
        else if (f == settingsCard) zoomName.setText(S.t("الإعدادات"));
        zoomName.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int[] a = new int[2], h = new int[2], t = new int[2];
        f.getLocationInWindow(a);
        hero.getLocationInWindow(h);
        tray.getLocationInWindow(t);
        float centre = a[0] + f.getWidth() * f.getScaleX() / 2f;   // the tile grows about its centre
        zoomName.setX(centre - h[0] - zoomName.getMeasuredWidth() / 2f);   // its place on the screen, whichever side it starts from
        if (glassWay()) zoomName.setTranslationY(t[1] - h[1] + FavBar.glassGrow(getResources().getDisplayMetrics().heightPixels, glassK) - zoomName.getMeasuredHeight() - bar(10));   // «زجاجي»: over the bar, alone
        else zoomName.setTranslationY(t[1] - h[1] + tray.getHeight() + bar(8));
        if (zoomName.getAlpha() < 1f) zoomName.animate().alpha(1f).setDuration(Motion.MS).setInterpolator(Motion.ease()).start();
    }

    /**
     * The one move the tray makes (FavBar.WIDGET_DROP): 40 px down while the focus is in a widget that opens into a scrolling row (the
     * matches widget of the football home, docs/PROPOSALS-NOV2.md §1, FootballFocus), back before a tile lifts. The tray looks the same
     * either way.
     */
    void lowerTray(boolean down) {
        if (tray == null || down == trayLowered) return;
        trayLowered = down;
        tray.animate().translationY(down ? bar(FavBar.WIDGET_DROP) : 0).setInterpolator(Motion.ease())
                .setDuration(Motion.move(MainActivity.this)).start();   // 4.4 the same easing everywhere (Motion)
    }

    /** the app's options while they show (closed when the home goes) */
    private PopupWindow appOptions;

    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_SHORT).show(); }

    /** Small focusable popup in the same style as the Inputs menu. */
    private void showMenu(View anchor, List<String> labels, List<Runnable> actions) {
        showMenu(anchor, labels, actions, Collections.<View>emptyList());
    }

    /** The same, with a row of small buttons under the last item (the app menu's «تفاصيل التطبيق» and «حذف التطبيق»), none for other menus. */
    private void showMenu(View anchor, List<String> labels, List<Runnable> actions, List<View> buttons) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(8), dp(8), dp(8), dp(8));
        list.setBackground(GlassPaint.fill(this, opaque(homeGlass().dialog), 16, homeGlass().line));   // the glass of a dialog
        PopupWindow pw = new PopupWindow(list, dp(230), -2, true);
        List<TextView> rows = new ArrayList<>();
        for (int k = 0; k < labels.size(); k++) rows.add(inputRow(labels.get(k), actions.get(k)));
        for (TextView r : rows) {
            r.setGravity(Gravity.CENTER_VERTICAL | (Dir.rtl() ? Gravity.END : Gravity.LEFT));   // Arabic as it always was; English from the left
            r.setOnClickListener(v -> { pw.dismiss(); ((Runnable) v.getTag()).run(); });
            list.addView(r, new LinearLayout.LayoutParams(-1, dp(36)));
        }
        View last = rows.get(rows.size() - 1);
        for (int k = 0; k < rows.size(); k++) {
            TextView r = rows.get(k);
            r.setNextFocusUpId(rows.get(Math.max(0, k - 1)).getId());
            r.setNextFocusDownId(k == rows.size() - 1 && !buttons.isEmpty() ? buttons.get(0).getId() : rows.get(Math.min(rows.size() - 1, k + 1)).getId());
            r.setNextFocusLeftId(r.getId());
            r.setNextFocusRightId(r.getId());
        }
        int extra = 0;
        if (!buttons.isEmpty()) {
            // side by side, the first at the start, the right in Arabic (the popup's window has no direction of its own); ▲ goes back to the last item
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutDirection(Dir.layout());
            for (int k = 0; k < buttons.size(); k++) {
                View b = buttons.get(k);
                b.setOnClickListener(v -> { pw.dismiss(); ((Runnable) v.getTag()).run(); });
                b.setNextFocusUpId(last.getId());
                b.setNextFocusDownId(b.getId());
                int before = buttons.get(Math.max(0, k - 1)).getId(), after = buttons.get(Math.min(buttons.size() - 1, k + 1)).getId();
                b.setNextFocusRightId(Dir.rtl() ? before : after);   // right to left: the one before is on the right; English the other way
                b.setNextFocusLeftId(Dir.rtl() ? after : before);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(APP_BUTTON_H), 1f);
                if (k > 0) lp.setMarginStart(dp(6));
                row.addView(b, lp);
            }
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
            rlp.topMargin = dp(6);
            list.addView(row, rlp);
            extra = dp(APP_BUTTON_H) + dp(6);
        }
        pw.setOutsideTouchable(true);
        pw.setElevation(dp(16));
        pw.showAsDropDown(anchor, 0, -anchor.getHeight() - dp(8) * (rows.size() + 2) - dp(36) * rows.size() - extra, Gravity.START);
        rows.get(0).requestFocus();
    }

    /** dp: the height of a button in the app menu's bottom row (an icon over a short name) */
    private static final int APP_BUTTON_H = 56;


    /** where the Sources dropdown hangs: the first pill, or the first dock card when every quick button is hidden (review F21); null: nothing to hang it on */
    private View inputsAnchor() {
        if (!pills.isEmpty()) return pills.get(0);
        return dockCards.isEmpty() ? null : dockCards.get(0);
    }

    private void showInputs(View anchor, int path) {
        guarded(path, null, "المصادر", () -> showInputsNow(anchor));
    }

    private void showInputsNow(View anchor) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(8), dp(8), dp(8), dp(8));
        list.setBackground(GlassPaint.fill(this, opaque(homeGlass().dialog), 16, homeGlass().line));   // the glass of a dialog

        PopupWindow pw = new PopupWindow(list, dp(200), -2, true);
        List<TextView> rows = new ArrayList<>();

        findTvApps();   // asked again each time: the TV's own apps can arrive with an update
        List<TvInputInfo> inputs = InputLabels.reported(this);   // in the TV's own names' order, so a name the owner gave moves nothing
        Map<String, String> given = InputLabels.given(this);
        for (TvInputInfo t : inputs) {
            // the tuner is not an HDMI input: opened as a passthrough link it ended on the last HDMI (TCL G10), so «the TV» has its own way (openLiveTv)
            rows.add(inputRow(InputLabels.rowLabel(this, t, given), () -> { if (t.getType() == TvInputInfo.TYPE_TUNER) openLiveTv(); else openPassthroughInput(t); }));   // + the CEC device's name and its standby mark
        }
        if (liveTvPkg != null) rows.add(inputRow(S.t("البث التلفزيوني"), () -> openLiveTv()));
        if (inputs.isEmpty() && inputPickerPkg != null) rows.add(inputRow(S.t("كل المصادر"), () -> guarded(ParentalPolicy.INPUT_PICKER, TvVendor.pkg(inputPickerPkg), "كل المصادر", () -> openPackage(inputPickerPkg))));
        Intent panel = inputPickerPkg == null ? systemInputs() : null;   // #54: no picker of the TV's own: the system's inputs panel, when it answers
        if (panel != null) rows.add(inputRow(S.t("كل المصادر"), () -> launchGuarded(ParentalPolicy.INPUT_PICKER, panel, "كل المصادر")));

        // 4.4 beta 3 (a beta user: no HDMI 1, HDMI 2 or aerial): with no input reported and no picker or panel of the TV's own, the TV's settings are offered even beside
        // «البث التلفزيوني», never a list without a way to the inputs
        if (rows.isEmpty() || (inputs.isEmpty() && inputPickerPkg == null && panel == null))
            rows.add(inputRow(S.t("الإعدادات"), () -> launchGuarded(ParentalPolicy.PILL_TV_SETTINGS, new Intent(Settings.ACTION_SETTINGS), "إعدادات التلفزيون")));
        for (TextView r : rows) {
            r.setOnClickListener(v -> { pw.dismiss(); ((Runnable) v.getTag()).run(); });
            list.addView(r, new LinearLayout.LayoutParams(-1, dp(36)));
        }
        for (int i = 0; i < rows.size(); i++) {
            TextView r = rows.get(i);
            r.setNextFocusUpId(rows.get(Math.max(0, i - 1)).getId());
            r.setNextFocusDownId(rows.get(Math.min(rows.size() - 1, i + 1)).getId());
            r.setNextFocusLeftId(r.getId());
            r.setNextFocusRightId(r.getId());
        }
        pw.setOutsideTouchable(true);
        pw.setElevation(dp(16));
        pw.setOnDismissListener(() -> { inputsPopup = null; anchor.requestFocus(); });
        inputsPopup = pw;
        pw.showAsDropDown(anchor, 0, dp(8), Gravity.START);
        rows.get(0).requestFocus();
    }

    /** «مؤقت النوم»: 15, 30, 60, 90 minutes, or cancel; at the end the TV goes to sleep (SleepTimer says how). */
    private void showSleep(View anchor) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(8), dp(8), dp(8), dp(8));
        list.setBackground(GlassPaint.fill(this, opaque(homeGlass().dialog), 16, homeGlass().line));   // the glass of a dialog
        PopupWindow pw = new PopupWindow(list, dp(220), -2, true);
        List<TextView> rows = new ArrayList<>();
        for (int m : SleepTimer.MINUTES) rows.add(inputRow(S.t("%s دقيقة", arabicDigits(String.valueOf(m))), () -> SleepTimer.start(this, m)));
        if (SleepTimer.minutesLeft(this) > 0) rows.add(inputRow(S.t("إلغاء"), () -> SleepTimer.start(this, 0)));
        if (!Extras.enabled()) {   // without «إضافات تلفزيونك» the end only stops the sound and brings the screensaver: say so, and offer it (4.3.3)
            rows.add(inputRow(S.t("لإطفاء الشاشة: «إضافات تلفزيونك»"), () -> launchGuarded(ParentalPolicy.SLEEP_ACCESSIBILITY, Extras.intent(this, "sleep"), "إعدادات التلفزيون")));
        }
        for (TextView r : rows) {
            r.setOnClickListener(v -> { pw.dismiss(); ((Runnable) v.getTag()).run(); });
            list.addView(r, new LinearLayout.LayoutParams(-1, dp(36)));
        }
        for (int i = 0; i < rows.size(); i++) {
            TextView r = rows.get(i);
            r.setNextFocusUpId(rows.get(Math.max(0, i - 1)).getId());
            r.setNextFocusDownId(rows.get(Math.min(rows.size() - 1, i + 1)).getId());
            r.setNextFocusLeftId(r.getId());
            r.setNextFocusRightId(r.getId());
        }
        pw.setOutsideTouchable(true);
        pw.setElevation(dp(16));
        pw.setOnDismissListener(() -> anchor.requestFocus());
        pw.showAsDropDown(anchor, 0, dp(8), Gravity.END);   // from the pill's left edge towards the right: the pill (the «مركز التحكم» one) is near the left edge of the screen
        rows.get(0).requestFocus();
    }

    /** «مركز التحكم»'s pill is gold while the timer runs (its tile says how long is left). */
    private void updateSleepPill() {
        if (centerPill == null) return;
        centerPill.setAccent(SleepTimer.minutesLeft(this) > 0);   // gold while the timer runs; the minutes are in the panel's tile
    }

    private TextView inputRow(String label, Runnable action) {
        TextView r = text(13, "sans-serif", Color.WHITE);
        r.setText(label);
        r.setTextColor(GlassPaint.focusColors(homeGlass().text, homeGlass().onFocus));   // the glass look's ink, the focus ink on the fill
        r.setSingleLine(true);   // a name the owner gave may be long: one line, cut at the end
        r.setEllipsize(android.text.TextUtils.TruncateAt.END);
        r.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        r.setTag(action);
        r.setId(View.generateViewId());
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(14), 0, dp(14), 0);
        r.setFocusable(true);
        r.setBackground(GlassPaint.row(this, homeGlass(), 10));   // nothing at rest, the solid focus fill while focused
        return r;
    }

    // ---------------------------------------------------------------- status

    // The two date formats are made once and given the city's zone when it changes: the tick runs every minute.
    private android.icu.text.SimpleDateFormat hijriFormat, hijriWeekday, hijriDay;
    private SimpleDateFormat gregFormat;
    private String formatZone = "";

    /** Between sunrise and Maghrib in the screensaver's city (the prayer times', as the line under the greeting); null when not known. */
    private Boolean dayBySun(ConfigSnapshot cfg) {
        if (!Look.AUTO.equals(cfg.appearance())) return null;   // only the automatic look asks
        try {
            return "الغروب".equals(sa.urtv.home.tilawah.DayParts.now(this, cfg.placeFragment(), cfg.placeTicket()).sunLabel);   // «الغروب» is next: the sun is up
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The home in the light or the dark look; nothing is touched while the look stays the same. */
    /** the home's own colours now: the light or the dark, and «رملي»'s own pair when that look is chosen (Look.of) */
    private Look.Palette palette() { return Look.of(lookNow, lightNow); }
    /** the look applyLook last painted (a change of look repaints even when the light stays) */
    private String paletteLook = "";

    /** the focused favourite's ring: the material's (the look's focus colour or the chosen accent, in the focus style), kept by applyLook so a focus step reads two fields */
    private int trayRingColor = 0xFFFFFFFF, trayRingPx = FavBar.RING;
    private int trayRing() { return trayRingColor; }
    private int trayRingW() { return bar(trayRingPx); }

    private void applyLook(boolean light) {
        GlassSource.setHomeLight(this, light);   // «تلقائي» on the other screens follows the home (kept only when it changes)
        String key = lookNow + "|" + GlassStyle.barPercent() + "|" + GlassStyle.dimPercent() + "|" + GlassStyle.focusStyle() + "|" + GlassStyle.of(lookNow, false).accent + "|" + GlassStyle.materialKey();   // a touch of «اللون والتحكم» repaints too
        if (light == lightNow && key.equals(paletteLook)) return;
        applyGlowMode();
        lightNow = light;
        paletteLook = key;
        Look.Palette p = palette();
        clock.setTextColor(p.text);
        title.setTextColor(p.ink);
        title.setShadowLayer(dp(18), 0, 0, p.titleShadow);
        for (TextView t : new TextView[]{weatherLine, date, prayerLine}) t.setTextColor(p.soft);
        for (TextView t : new TextView[]{greeting, ampm, zoneLine, appsLabel}) t.setTextColor(p.gold);
        for (TextView t : new TextView[]{greeting, weatherLine, clock, ampm, date, zoneLine, prayerLine}) t.setShadowLayer(dp(3), 0, dp(1), p.shadow);
        for (TextView t : gridLabels) t.setTextColor(p.ink);
        for (TextView t : tvNames) { t.setTextColor(p.gold); t.setShadowLayer(dp(3), 0, dp(1), p.shadow); }
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, p.scrim));
        gridShade.setBackgroundColor(p.shade);
        boolean noGlass = DebugFlags.on(this, "traynoglass");   // a tester's switch (dock.sh debug traynoglass): no fill and no line behind the tiles
        GlassStyle.Material m = GlassStyle.material(lookNow, light, GlassStyle.BAR);   // «توحيد المظهر»: the apps bar wears the home's one material, as the upper buttons and the cards do
        dockBand.setColor(noGlass ? 0 : m.fill);
        dockBand.setStroke(Math.max(1, bar(1)), noGlass ? 0 : m.line);
        glassBar.light(light);   // 4.5 «زجاجي»: the bar's tint follows the home's light
        trayRingColor = m.ring; trayRingPx = m.ringWidth;
        if (wallDimView != null) wallDimView.setAlpha(GlassStyle.dimPercent() / 100f);
        for (View c : dockCards) { GradientDrawable r = ringByCard.get(c); if (r != null) r.setStroke(trayRingW(), trayRing()); }
        if (homeCard != null) { homeCard.look(lookNow); homeCard.light(light); if (resumed) homeCard.refresh(); }   // the cards' page takes the new material now (its address changed), not at the next return   // the football home's widgets: the same light or dark glass
        updateNetIcon();   // its colour is the look's secondary ink
    }

    // ---------------------------------------------------------------- «UrTV النظيف» (CleanMode)

    private boolean cleanShown;   // the mask is on the screen now

    /**
     * «UrTV النظيف»: the home shows the essentials only, as a mask over what is there (nothing is deleted or written over): the clock, the date and the apps bar always;
     * by the person's «تخصيص» one card, the Wi-Fi mark, the greeting and the settings button. Off: every one of them comes back as the person had it. Idempotent.
     */
    private void applyClean() {
        boolean on = CleanMode.on(this);
        if (!on && !cleanShown) return;
        boolean was = cleanShown;
        cleanShown = on;
        java.util.Set<String> keep = CleanMode.keep(this);
        if (pillBar != null) pillBar.lock(on);   // the row stays closed: only the one button
        maskVis(phonePill, on ? View.GONE : null);
        if (nowCard != null && on) maskVis(nowCard, View.GONE);
        if (centerPill != null) {   // the control center's pill is the settings button while the mask is on
            centerPill.setIcon(on ? R.drawable.ic_settings : R.drawable.ic_center);
            centerPill.setLabel(on ? S.t("الإعدادات") : ControlText.NAME);
            maskVis(centerPill, on && !keep.contains(CleanMode.SETTINGS) ? View.GONE : (on ? View.VISIBLE : null));
        }
        maskVis(netIcon, on && !keep.contains(CleanMode.WIFI) ? View.GONE : (on ? View.VISIBLE : null));
        if (on) {
            maskVis(weatherLine, View.GONE);
            maskVis(zoneLine, View.GONE);
            maskVis(nudge, View.GONE);
            footballHeader();
        }
        maskVis(tvRowsBox, on ? View.GONE : null);
        updatePrayerLine();   // its own view: shown only while the mask is on and the tick is kept, hidden otherwise
        if (homeCard != null) homeCard.hide(on);   // «UrTV النظيف»: the home's own card never shows (the prayer card below is the one card)
        refreshSideCards();   // review F122: nor the side cards (dropped while the mask is on, drawn again when it goes)
        if (was && !on) {   // back to the full home: what the mask hid is drawn again from the person's own settings
            restoreClean();
            updateClock();
            loadTvRows();
            updateHomeNudge(false);
            updateNowPlaying();
            wireFocus();
        }
    }

    /** what every view the mask touches showed before it: put back exactly (not «visible» by default: a button the person had hidden stays hidden), so the home is as it was */
    private final java.util.Map<View, Integer> cleanWas = new java.util.LinkedHashMap<>();

    /** hide or show a view for the mask (vis != null), remembering what it showed the first time; null = leave it (restoreClean puts it back) */
    private void maskVis(View v, Integer vis) {
        if (v == null || vis == null) return;
        if (!cleanWas.containsKey(v)) cleanWas.put(v, v.getVisibility());
        v.setVisibility(vis);
    }

    /** the mask comes off: every view as it was before, the quick-button bar measured again (its width is its buttons' own) */
    private void restoreClean() {
        for (java.util.Map.Entry<View, Integer> e : cleanWas.entrySet()) e.getKey().setVisibility(e.getValue());
        cleanWas.clear();
        if (pillBar != null) { pillBar.lock(false); pillBar.requestLayout(); }
        if (pillRow != null) pillRow.requestLayout();
    }

    /** The football home: the weather has its own card and the clock sits higher, so the greeting and its weather line are not shown. */
    private void footballHeader() {
        if (greeting == null) return;
        if (cleanShown) {   // «UrTV النظيف»: the greeting only when the person kept it, never its weather line
            greeting.setVisibility(CleanMode.keep(this).contains(CleanMode.GREETING) ? View.VISIBLE : View.GONE);
            weatherLine.setVisibility(View.GONE);
            return;
        }
        boolean football = homeCard != null && homeCard.isFootball();
        greeting.setVisibility(football ? View.GONE : View.VISIBLE);
        if (football) weatherLine.setVisibility(View.GONE);
    }

    /**
     * The minute tick. P3: the screensaver's settings are read off the UI thread (a cross-process query that starts its process again when the TV killed it), one read at a time, and
     * the tick runs with them when they come; the first tick of this screen reads at once (nothing is drawn yet). A tick asked while one is being read is that one.
     */
    private boolean clockReading, clockFirstDone;

    private void updateClock() {
        if (!clockFirstDone) {
            clockFirstDone = true;
            // 4.3.4 QA-1: a home made again (a language change, recreate) draws at once from the answer this process already has, and the
            // fresh read below goes off the UI thread; only the process's very first draw asks the screensaver here (nothing is on screen yet)
            ConfigSnapshot known = ConfigSnapshot.known();
            // 4.4 QA-11: a cold process draws this once from the last real answer kept on disk (marked old, never a query); the read below follows at once
            updateClock(known != null ? known : ConfigSnapshot.ui(this));
        }
        if (clockReading) return;
        clockReading = true;
        final android.content.Context app = getApplicationContext();
        new Thread(new Runnable() {
            @Override public void run() {
                final ConfigSnapshot got = ConfigSnapshot.read(app);
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        clockReading = false;
                        if (!isDestroyed() && !isFinishing()) updateClock(got);
                    }
                });
            }
        }, "home-config").start();
    }

    /**
     * 4.3.3 «الآفاق» (the owner's choice in «المظهر ← الواجهة»): the scene and its big glass clock take the place of the photo and of the
     * header's clock (the greeting, the date and the rest of the home stay as they are); any other layout: the home as it was.
     */
    /** «الساعة: زجاجية» as drawn now: the setting, but never while cards show (in any layout they can cover part of it) */
    static boolean glassClock(String homeClock, boolean cardsShown, boolean inRow) { return GlassClockRule.glass(homeClock, cardsShown, inRow); }

    private boolean sceneBlank;   // no scene could be drawn: the wall photos are shown under the scene layer (SceneHome.Backdrop)

    private void sceneTick(ConfigSnapshot cfg) {
        boolean on = "scenes".equals(cfg.homeBg);   // 4.4 «الخلفية ← الآفاق» (any layout); the glass clock is «الساعة»'s own choice
        // 4.4 the owner (sweep 2, option 2; sweep 3: every layout): the cards (the couch strip under the clock, or the box beside it, which can reach
        // the glass clock's box) would sit over the glass clock, so while any card shows the header's regular clock takes its place (the scene stays);
        // the glass clock comes back when the cards are off. Read every minute (updateClock).
        boolean glass = glassClock(cfg.homeClock, homeCard != null && homeCard.shownCards() > 0, homeCard != null && homeCard.inRow());
        photoGlassTick(cfg, !on && glass);
        if (!on && sceneHome == null && !scenesOn) return;   // the home as it was (the default): nothing of the scenes is made, read, timed or downloaded
        if (on && sceneHome == null) {   // chosen: the layer is made now, over the photos and under the dimming and the scrims
            sceneHome = new SceneHome(this, Ram.tier(this), font("display"));
            sceneHome.backdrop(new SceneHome.Backdrop() {   // 4.4 beta 4: no scene draws: the wall photos instead of a black home
                @Override public void blank() { sceneBlank = true; scenesOn = false; wallBack.setVisibility(View.VISIBLE); wallFront.setVisibility(View.VISIBLE); nextWallpaper(); }
                @Override public void drawn() { sceneBlank = false; scenesOn = true; handler.removeCallbacks(rotateWallpaper); wallGen++; for (ImageView w : new ImageView[] {wallBack, wallFront}) { w.setImageDrawable(null); w.setVisibility(View.INVISIBLE); } }
            });
            sceneBackdrop.addView(sceneHome, sceneBackdrop.indexOfChild(wallFront) + 1, new FrameLayout.LayoutParams(-1, -1));
        }
        if (on) {
            sceneHome.set(true, cfg.scenePacks, ScenePlan.stay(cfg.sceneStay), homeCard != null && homeCard.shownCards() > 0 && !homeCard.inRow(), homeCard != null && homeCard.inRow(), Dir.rtl(), glass);   // «كنبة»: the cards are under it, the clock in the middle
            boolean night = false, fog = false;
            if (cfg.sceneByTime) try {   // the city's Maghrib to its Fajr (the same prayer times as the adhan)
                sa.urtv.home.tilawah.DayParts d = sa.urtv.home.tilawah.DayParts.now(this, cfg.placeFragment(), cfg.placeTicket());
                long t = System.currentTimeMillis();
                night = t >= d.maghrib || t < d.fajr;
            } catch (RuntimeException ignored) { }
            if (cfg.sceneByWeather) {   // the greeting's last weather answer (kept 30 minutes; none while the greeting is off)
                android.content.SharedPreferences g = getSharedPreferences("greeting", MODE_PRIVATE);
                fog = System.currentTimeMillis() - g.getLong("at", 0) < 2 * 60 * 60 * 1000L && ScenePlan.fog(g.getInt("code", -1));
            }
            sceneHome.mood(cfg.sceneByTime, night, cfg.sceneByWeather, fog);
        } else if (sceneHome != null) {   // turned off: the layer goes whole (its views, its pictures, its timer and its thread)
            sceneHome.shutdown();
            sceneBackdrop.removeView(sceneHome);
            sceneHome = null;
        }
        if (on != scenesOn && !sceneBlank) {   // (blank: the wall photos stand in until a scene draws)
            scenesOn = on;
            if (on) {   // the ordinary photos let go: no rotation, a photo on its way is not shown, the two layers emptied
                handler.removeCallbacks(rotateWallpaper);
                wallGen++;
                for (ImageView w : new ImageView[] {wallBack, wallFront}) w.setImageDrawable(null);
            } else nextWallpaper();   // back to the home as it was: its photo again
        }
        int show = on && !sceneBlank ? View.INVISIBLE : View.VISIBLE;   // INVISIBLE: the header keeps its size, nothing under it moves
        boolean bigClock = glass;   // the glass clock (the scene's, or the photo's) takes the header clock's place; with «الساعة: عادية» the header clock stays
        clock.setVisibility(bigClock ? View.INVISIBLE : View.VISIBLE);
        if (bigClock) ampm.setVisibility(View.INVISIBLE);
        liftDate(bigClock);
        wallBack.setVisibility(show);
        wallFront.setVisibility(show);
        if (on) { sceneHome.setTime(clock.getText()); if (resumed) sceneHome.start(); }
    }

    /**
     * 4.4 «الساعة: زجاجية خلف الصورة» with «الخلفية: صوري وحزم الصور»: the big glass clock over the photo, and the photo's scenery over the clock when
     * it is one of ours with a cut-out (never «الحرمين», never the person's own). With «الساعة: عادية» (the default) nothing of it exists.
     */
    private void photoGlassTick(ConfigSnapshot cfg, boolean want) {
        if (!want) {
            if (photoGlass == null) return;   // the default: nothing made, read or downloaded
            photoGlass.shutdown();
            sceneBackdrop.removeView(photoGlass);
            photoGlass = null;
            clock.setVisibility(View.VISIBLE);   // the header clock back (sceneTick sets it again when the scenes are on)
            liftDate(false);
            return;
        }
        boolean cards = homeCard != null && homeCard.shownCards() > 0 && !homeCard.inRow();   // «كنبة»: the cards are in the strip under it, the clock in the middle
        boolean row = homeCard != null && homeCard.inRow();
        if (photoGlass == null) {   // over the two photo layers (showWallpaper keeps them at 0 and 1), under the dimming and the scrims
            photoGlass = new PhotoGlass(this, Ram.tier(this), font("display"));
            photoGlass.onZoom(this::zoomWall);
            sceneBackdrop.addView(photoGlass, sceneBackdrop.indexOfChild(wallFront) + 1, new FrameLayout.LayoutParams(-1, -1));
            photoGlass.place(cards, Dir.rtl(), row);
            Drawable d = wallFront.getDrawable();
            if (d instanceof BitmapDrawable && ((BitmapDrawable) d).getBitmap() != null) photoGlass.show(((BitmapDrawable) d).getBitmap(), wallShownKey, NetGate.allowed(resumed, NetGateAndroid.displayOn(this)));
        } else photoGlass.place(cards, Dir.rtl(), row);
        photoGlass.setTime(clock.getText());
        clock.setVisibility(View.INVISIBLE);
        ampm.setVisibility(View.INVISIBLE);
        liftDate(true);
    }

    /**
     * 4.4 the owner (the date ran into the big clock's «2»): while the big glass clock takes the header clock's place, the lines under the header
     * clock (the date, the zone, the prayer) move up into its empty row, clear of the big digits; the header keeps its size (nothing else moves)
     */
    private boolean bigClockOn;   // the big glass clock is on the screen (the scenes' or the photo's): the cards' strip stays clear of its digits

    private void liftDate(boolean up) {
        if (up != bigClockOn) { bigClockOn = up; if (homeCard != null) handler.post(homeCard::fit); }   // the strip moves clear of the digits (or back)
        if (clock == null || date == null) return;
        if (!liftWatch) {   // 4.4 beta 2 (the owner's TV, build 574: the date drawn over «مساء الخير»): the lift is measured again whenever the lines
            liftWatch = true;   // above it move (the weather line coming, going or wrapping, the greeting row): a lift measured once went stale
            View.OnLayoutChangeListener again = (v, l, t, r, b, ol, ot, or, ob) -> { if (t != ot || b != ob) applyLift(); };
            date.addOnLayoutChangeListener(again);
            clock.addOnLayoutChangeListener(again);
        }
        if (up && clock.getHeight() == 0) clock.post(this::applyLift); else applyLift();
    }

    private boolean liftWatch;

    /** the lines under the header clock at the clock's own row while the big clock is on (never above it: the greeting and the weather stay clear), else in place */
    private void applyLift() {
        if (clock == null || date == null) return;
        // 4.4 beta 4 (the tester, build 629, «بدون» as the home card: the date drawn over «مساء الخير» and the Wi-Fi icon): the clock is in its own row
        // (clockRow), the date in the column: the clock's top in the column's own measure is its row's top plus its own, never its own alone (that was 0, so
        // the date went up to the top of the column, over the greeting)
        int clockTop = (clock.getParent() instanceof View ? ((View) clock.getParent()).getTop() : 0) + clock.getTop();
        float dy = bigClockOn ? -Math.max(0, date.getTop() - clockTop) : 0f, dx = 0f;
        // 4.4 beta 4 (the owner: «كنبة» as couch-tv): with the big clock and the cards in their row, the date sits centred just above the digits
        // (couch-tv's date over its clock); the zone and prayer lines follow it. Anything else: the lift as before, no sideways move.
        // 4.4 beta 4 (the tester, build 654, «كنبة» with «بدون»: the date went up into the weather's column, one line under «الشروق»): the big clock is
        // in the same box with no card, so the date sits over it the same way
        if (bigClockOn && homeCard != null && (homeCard.inRow() || homeCard.couch()) && date.getWidth() > 0) {
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            int[] box = ScenePlan.fixedBox(dm.widthPixels, dm.heightPixels, false, Dir.rtl());
            int[] at = new int[2];
            date.getLocationInWindow(at);
            float x0 = at[0] - date.getTranslationX(), y0 = at[1] - date.getTranslationY();   // where the date is laid out, before any move
            dx = (box[0] + box[2]) / 2f - date.getWidth() / 2f - x0;
            dy = box[1] - date.getHeight() - dp(10) - y0;
        }
        for (View v : new View[] {date, zoneLine, prayerLine}) {
            if (v == null) continue;
            if (v.getTranslationY() != dy) v.setTranslationY(dy);
            if (v.getTranslationX() != dx) v.setTranslationX(dx);
        }
    }

    /** 4.4: the photo on the screen framed as its cut-out over the glass clock is (PhotoGlass: its crop, then the zoom k about the top centre); the one fading out keeps its own */
    private void zoomWall(int[] crop, float k) {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        float[] f = ScenePlan.frame(crop, dm.widthPixels, dm.heightPixels, k);
        wallFront.setPivotX(0);
        wallFront.setPivotY(0);
        wallFront.setScaleX(f[0]);
        wallFront.setScaleY(f[0]);
        wallFront.setTranslationX(f[1]);
        wallFront.setTranslationY(f[2]);
    }

    private void updateClock(ConfigSnapshot cfg) {
        if (lookPreview != null && cfg != null) cfg = cfg.withLook(lookPreview[0], lookPreview[1], lookPreview[2], lookPreview[3], lookPreview[4]);   // 4.5 «المظهر»: what is under the focus, never saved
        lastCfg = cfg;   // «مركز التحكم»'s «الأذان والإقامة» reads the times from it
        long ms = System.currentTimeMillis();
        clockDrawnAt = ms;
        String tvTz = java.util.TimeZone.getDefault().getID();
        // one read of the screensaver's settings (its ConfigProvider, same signing key) for the whole tick; null zone = it is missing: the TV's own
        Digits.remember(this, cfg);   // a change of the setting reaches the next tick
        if (langBuilt != null && !langBuilt.equals(S.lang())) {   // «اللغة» changed while this screen was up (tester step 20: the words, the
            langBuilt = S.lang();                                 // favourites' direction and the names stayed as built): built again in it
            recreate();
            return;
        }
        String[] zone = cfg.cityZone();
        if (cfg.cityChosen != cityChosenNow) { cityChosenNow = cfg.cityChosen; updateHomeNudge(false); }   // 4.3.1: the question follows the screensaver's answer
        String tz = zone == null ? tvTz : zone[1];
        Calendar now = ClockZone.at(ms, tz);   // the city's time, like the prayer times beside it
        int h = now.get(Calendar.HOUR_OF_DAY);
        greetingLine.update(h, cfg);
        footballHeader();
        lookNow = cfg.look();
        applyLook(Look.light(cfg.appearance(), dayBySun(cfg), h * 60 + now.get(Calendar.MINUTE)));
        GlassStyle.Look glassNow = homeGlass();
        for (Pill p : new Pill[] {inputsPill, tasksPill, tilawahPill, centerPill, marketsPill, matchesPill, searchPill, phonePill, updatePill, bellPill}) if (p != null) p.look(glassNow);   // «رملي»: its own buttons
        if (pillBar != null) pillBar.look(GlassStyle.material(glassNow, lookNow, GlassStyle.QUICK));
        for (View v : pills) if (v instanceof Pill) ((Pill) v).look(glassNow);   // and every pill of the row, whatever else is added to it: six buttons, one style (step 28: two stayed grey circles)
        boolean h24 = cfg.clock24();   // «ساعة ٢٤»: off (12-hour with the ص or م) unless the owner turned it on
        clock.setText(ClockText.face(h, now.get(Calendar.MINUTE), h24, Digits.latin()));
        ampm.setText(ClockText.suffix(h, h24));
        ampm.setVisibility(h24 ? View.GONE : View.VISIBLE);
        sceneTick(cfg);
        alignApps(cfg.appsAlign);   // 4.4 beta 2 «محاذاة التطبيقات»
        String wayNow = FavBar.look(cfg.appsStyle);
        boolean couchNow = homeCard != null && homeCard.couch();
        boolean gearNew = couchNow != appsCouch;   // the layout turned «كنبة» (or left it): «الإعدادات» joins the row's end (or leaves it)
        appsCouch = couchNow;
        float kNow = FavBar.glassK(cfg.glassSize);
        int rowsNow = FavBar.glassRowsFit(getResources().getDisplayMetrics().heightPixels, kNow, FavBar.glassRows(cfg.glassRows));   // as many as fit at this size
        boolean glassNew = FavBar.GLASS.equals(wayNow) && wayNow.equals(appsLook) && (kNow != glassK || rowsNow != glassRowsN);   // 4.5 «زجاجي»: another size or rows
        glassK = kNow;
        glassRowsN = rowsNow;
        if (glassNew) {
            applyAppsLook();
            buildApps();
        } else if (gearNew && wayNow.equals(appsLook)) {
            buildApps();
        } else if (!wayNow.equals(appsLook)) {   // 4.4 «شكل التطبيقات»: another way of the row: its tray, its cells and their art made again
            appsLook = wayNow;
            appsZoom = FavBar.zoom(cfg.appsStyle);
            applyAppsLook();
            buildApps();
        } else if (appsZoom != FavBar.zoom(cfg.appsStyle)) {   // «شكل التطبيقات» changed: the focused tile takes the new look now
            appsZoom = FavBar.zoom(cfg.appsStyle);
            View f = getCurrentFocus();
            if (f != null && dockCards.contains(f) && f.getOnFocusChangeListener() != null) f.getOnFocusChangeListener().onFocusChange(f, true);
            placeName();
        }
        String cal = zone == null ? "" : ClockZone.calendar(zone[1], zone[2]);
        boolean other = zone != null && ClockZone.showLine(ClockZone.differs(tvTz, tz, ms), cal);
        zoneLine.setVisibility(other ? View.VISIBLE : View.GONE);
        if (other) zoneLine.setText(ClockZone.label(zone[0], cal));
        if (gregFormat == null) {
            gregFormat = new SimpleDateFormat("d MMMM y", new Locale("ar"));   // 4.3.3: with the year, as the screensaver's line
            try {
                hijriFormat = new android.icu.text.SimpleDateFormat("EEEE، d MMMM y", new android.icu.util.ULocale("ar_SA@calendar=islamic-umalqura"));
                hijriDay = new android.icu.text.SimpleDateFormat("d MMMM y", new android.icu.util.ULocale("ar_SA@calendar=islamic-umalqura"));
            } catch (Exception ignored) { }
        }
        {   // every tick: ZoneFix's offset (issue #55: the TV's own tz data can be older than the page's) may move with the page's word
            gregFormat.setTimeZone(ZoneFix.zone(tz));
            if (hijriFormat != null) hijriFormat.setTimeZone(ZoneIcu.of(tz));
            if (hijriDay != null) hijriDay.setTimeZone(ZoneIcu.of(tz));
            formatZone = tz;
        }
        String hijri = "";
        try {
            int shift = HijriShift.fromFragment(cfg.fragment);   // «فرق التاريخ الهجري»: 0 unless the settings say so
            if (hijriFormat != null && shift == 0) hijri = hijriFormat.format(now.getTime()).replaceAll("\\s*هـ\\s*$", "") + " هـ";
            else if (hijriFormat != null) {   // the day, month and year move by the offset; the weekday is today's
                if (hijriWeekday == null) hijriWeekday = new android.icu.text.SimpleDateFormat("EEEE", new android.icu.util.ULocale("ar_SA@calendar=islamic-umalqura"));
                hijriWeekday.setTimeZone(ZoneIcu.of(tz));
                hijri = hijriWeekday.format(now.getTime()) + "، " + hijriDay.format(HijriShift.date(now, shift)).replaceAll("\\s*هـ\\s*$", "") + " هـ";
            }
        } catch (Exception ignored) { }
        String greg = MonthNames.of(gregFormat.format(now.getTime()), MonthNames.levant(cfg.fragment));   // «تشرين الأول» when chosen
        if (DateNum.on(cfg.fragment)) {   // 4.3.3 «التاريخ بالأرقام»: 06/06/1448 هـ and 16/11/2026, in the digits chosen
            String n = numericDates(now, tz, HijriShift.fromFragment(cfg.fragment), DateNum.hijriOff(cfg.fragment));
            if (n != null) { hijri = ""; greg = n; }
        }
        if (DateNum.hijriOff(cfg.fragment)) hijri = "";   // GitHub #15 «التاريخ الهجري» off: the Gregorian date alone
        date.setText(arabicDigits(hijri.isEmpty() ? greg : hijri + "  –  " + greg));
        if (S.en()) date.setText(arabicDigits(englishDate(now, tz, HijriShift.fromFragment(cfg.fragment), DateNum.on(cfg.fragment), DateNum.hijriOff(cfg.fragment))));   // the same line in English (MonthNames.hijriEn)
        updateAdhanBar(cfg);
        updatePrayerLine();
        applyListenPill(cfg.listenPillOn);
        applyTasksPill(cfg.tasksPillOn());
        Dock.followBackSaver(this, cfg.backSaver());   // «الرجوع يفتح شاشة التوقف» changed in the screensaver's settings: read at the next Back
        backTwice = cfg.backTwice();   // 4.4 beta 3 «رجوع مرتين يفتح شاشة التوقف»
        Dock.followKeyApps(this, cfg.keyApps);   // «أزرار الريموت» changed in the screensaver's settings: read at the next key (and told to the add-on)
        applyUpdateBell(cfg.updateBellOn);   // [updates bell]
        applyBarSize(cfg.favBar);   // «تطبيقات في الشريط»
        hiddenQuick.clear();
        for (String k : new String[] {"markets", "matches", "center", "phone", "inputs"}) if (cfg.pillHidden(k)) hiddenQuick.add(k);
        applyQuickPill(inputsPill, !hiddenQuick.contains("inputs"), tasksPill, tilawahPill, centerPill, marketsPill, matchesPill, searchPill, phonePill, updatePill);   // «زر المصادر» (4.4)
        applyQuickPill(marketsPill, !hiddenQuick.contains("markets"), matchesPill, phonePill, updatePill, tilawahPill);
        applyQuickPill(matchesPill, !hiddenQuick.contains("matches"), phonePill, updatePill, tilawahPill);
        applyQuickPill(phonePill, !hiddenQuick.contains("phone"), updatePill, tilawahPill);
        applyQuickPill(searchPill, GlobalSearch.wanted(cfg.fragment, Parental.state(this).kids, cleanShown) && GlobalSearch.intent(getPackageManager()) != null, phonePill, updatePill, tilawahPill);   // 4.3.3 «زر البحث»: before «الجوال»
        applyQuickPill(updatePill, !Parental.state(this).kids && UpdateNotice.wanted(this), tilawahPill);
        UpdateNotice.checkIfDue(getApplicationContext(), () -> runOnUiThread(() -> { applyQuickPill(updatePill, !Parental.state(this).kids && UpdateNotice.wanted(this), tilawahPill); if (resumed) updateHomeNudge(false); }));   // 4.4: the card too, as soon as a newer build is known
        updateNetIcon();
        applyCenterPill();   // kids mode may have been turned on or off, or the button hidden
        sa.urtv.home.tilawah.AdhanSync.onConfig(this, cfg.placeFragment(), cfg.placeTicket());   // «تعديل وقت الصلاة» changed on the TV: the alarm follows
        updateSleepPill();
        applyClean();   // «UrTV النظيف» (a mask over what the settings above just applied)
    }

    /** Once a minute with the clock: the bar before the adhan shows in the last 10 minutes, and until the iqama. */
    private void updateAdhanBar(ConfigSnapshot cfg) {
        String words = AdhanBar.text(this, cfg);
        int was = adhanBar.getVisibility(), now = words == null ? View.GONE : View.VISIBLE;
        adhanBar.setVisibility(now);
        View f = getCurrentFocus();   // the bar came (or went) while the apps page's first row is up: the page makes room under it (or takes it back)
        if (was != now && f != null && gridCards.contains(f) && f.getParent() instanceof View && gridFirst == 0) scrollToRow((View) f.getParent());
        if (words != null) adhanBar.setText(words);
        // any key stops the adhan (AdhanKeys) and the focus stays where it was; with «إيقاف الأذان بأي زر» off the bar is focusable (UP reaches it) and OK on it stops it
        adhanBar.setFocusable(words != null && AdhanBar.playing(cfg));
        if (duaCard != null) duaCard.show(NowPlaying.duaName(), getCurrentFocus());   // 4.4: a dua's card, with the focus on «إيقاف»
    }

    /**
     * «UrTV النظيف»: «بطاقة الصلاة»: the next prayer as one line under the date, or today's five as one strip placed by PrayerCardBox («مكان البطاقة»), or nothing; never both. Read off the UI thread
     * (it asks the screensaver for the place), on the minute tick; the strip is one view that is filled again, nothing is drawn per frame.
     */
    private void updatePrayerLine() {
        if (prayerLine == null) return;
        final String mode = cleanShown ? CleanMode.prayer(this) : CleanKeep.NONE;
        if (!CleanKeep.LINE.equals(mode)) prayerLine.setVisibility(View.GONE);
        if (!CleanKeep.CARD.equals(mode)) { if (prayerCard != null) prayerCard.setVisibility(View.GONE); }
        if (CleanKeep.NONE.equals(mode)) return;
        final android.content.Context app = getApplicationContext();
        new Thread(() -> {
            final String line = CleanKeep.LINE.equals(mode) ? sa.urtv.home.tilawah.NowPlaying.nextPrayerLine(app) : "";
            final String[] five = CleanKeep.CARD.equals(mode) ? sa.urtv.home.tilawah.NowPlaying.todayCard(app) : null;
            runOnUiThread(() -> {
                if (!cleanShown || isFinishing() || isDestroyed() || !mode.equals(CleanMode.prayer(this))) return;
                if (CleanKeep.LINE.equals(mode)) {
                    prayerLine.setText(arabicDigits(line));
                    prayerLine.setVisibility(line.isEmpty() ? View.GONE : View.VISIBLE);
                } else showPrayerCard(five);
            });
        }, "clean-prayer").start();
    }

    private final TextView[] cardNames = new TextView[5], cardTimes = new TextView[5];

    private void showPrayerCard(String[] five) {
        if (five == null) { if (prayerCard != null) prayerCard.setVisibility(View.GONE); return; }
        if (prayerCard == null) {
            prayerCard = new LinearLayout(this);
            prayerCard.setOrientation(LinearLayout.HORIZONTAL);
            prayerCard.setLayoutDirection(Dir.layout());
            prayerCard.setGravity(Gravity.CENTER);
            prayerCard.setPadding(dp(8), dp(8), dp(8), dp(8));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0x66000000);
            bg.setCornerRadius(dp(16));
            prayerCard.setBackground(bg);
            for (int k = 0; k < 5; k++) {
                LinearLayout cell = new LinearLayout(this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                cardNames[k] = text(12, "regular", 0xFFC6BBA4);
                cardTimes[k] = text(15, "sans-serif-medium", 0xFFFFFFFF);
                cardNames[k].setGravity(Gravity.CENTER);
                cardTimes[k].setGravity(Gravity.CENTER);
                cardNames[k].setSingleLine(true);
                cardTimes[k].setSingleLine(true);
                cell.addView(cardNames[k]);
                cell.addView(cardTimes[k]);
                prayerCard.addView(cell, new LinearLayout.LayoutParams(0, -2, 1f));
            }
            prayerCard.setFocusable(false);
            hero.addView(prayerCard, new FrameLayout.LayoutParams(dp(PrayerCardBox.W), dp(PrayerCardBox.H), Gravity.TOP | (Dir.rtl() ? Gravity.RIGHT : Gravity.LEFT)));
            hero.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> placePrayerCard());
        }
        int next = Integer.parseInt(five[10]);
        for (int k = 0; k < 5; k++) {   // the next one in the gold, the others quiet
            cardNames[k].setText(five[2 * k]);
            cardTimes[k].setText(arabicDigits(five[2 * k + 1]));
            cardNames[k].setTextColor(k == next ? GOLD : 0xFFC6BBA4);
            cardTimes[k].setTextColor(k == next ? GOLD : 0xFFFFFFFF);
        }
        prayerCard.setVisibility(View.VISIBLE);
        placePrayerCard();
    }

    /** a view's box in the hero's coordinates {left, top, right, bottom}, or null when it is not under the hero */
    private int[] boxInHero(View v) {
        int l = 0, t = 0;
        View at = v;
        while (at != hero) {
            l += at.getLeft(); t += at.getTop();
            if (!(at.getParent() instanceof View)) return null;
            at = (View) at.getParent();
        }
        return new int[] {l, t, l + v.getWidth(), t + v.getHeight()};
    }

    /** the strip's place from the clock block's real box and the app title's top (PrayerCardBox); only when it moved */
    private void placePrayerCard() {
        if (prayerCard == null || prayerCard.getVisibility() != View.VISIBLE || clockBlock == null || bottomBlock == null || clockBlock.getWidth() == 0) return;
        View header = (View) clockBlock.getParent();
        int[] clockBox = {header.getLeft() + clockBlock.getLeft(), header.getTop() + clockBlock.getTop(), header.getLeft() + clockBlock.getRight(), header.getTop() + clockBlock.getBottom()};
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        // everything else on the top of the home that the strip must clear: the quick buttons (the settings button, the Wi-Fi mark), the Wi-Fi mark when it stands apart, the adhan bar
        java.util.List<int[]> others = new java.util.ArrayList<>();
        for (View v : new View[] {pillBar, pillRow, netIcon}) {
            int[] b = v == null || v.getVisibility() != View.VISIBLE || v.getWidth() == 0 ? null : boxInHero(v);
            if (b != null && b[3] > 0 && b[1] < bottomBlock.getTop()) others.add(b);
        }
        if (adhanBar.getVisibility() == View.VISIBLE) others.add(new int[] {0, 0, dm.widthPixels, dp(26)});   // the bar before the adhan, over the top edge
        int[] r = PrayerCardBox.place(CleanMode.place(this), Dir.rtl(), dm.widthPixels, dm.heightPixels, dm.density, clockBox, others.toArray(new int[0][]), bottomBlock.getTop());
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) prayerCard.getLayoutParams();
        int edge = Dir.rtl() ? dm.widthPixels - r[2] : r[0];   // the margin from the side the card is pinned to
        if ((Dir.rtl() ? p.rightMargin : p.leftMargin) == edge && p.topMargin == r[1] && p.width == r[2] - r[0]) return;
        if (Dir.rtl()) p.rightMargin = edge; else p.leftMargin = edge;
        p.topMargin = r[1];
        p.width = r[2] - r[0];
        p.height = r[3] - r[1];
        prayerCard.setLayoutParams(p);
    }

    private static String arabicDigits(String s) { return Digits.of(s); }   // the setting's digits (hindi or latin)

    /**
     * The date line in English, as the English page writes it: «Monday, 22 Jumada al-Awwal 1448 AH  –  2 November». The Hijri day comes
     * from the TV's Umm al-Qura calendar, moved by «فرق التاريخ الهجري» as the Arabic line; without that calendar, the Gregorian day alone.
     */
    /**
     * 4.3.3 «التاريخ بالأرقام»: «الاثنين، 06/06/1448 هـ  –  16/11/2026», day/month/year in Latin digits (arabicDigits turns them to the digits chosen);
     * null when the TV's ICU has no Umm al-Qura calendar (the line stays in words).
     */
    private String numericDates(Calendar now, String tz, int shift, boolean noHijri) {
        String n = numericParts(now, tz, shift);
        if (n == null) return null;
        SimpleDateFormat day = new SimpleDateFormat("EEEE", new Locale("ar"));
        day.setTimeZone(ZoneFix.zone(tz));
        return noHijri ? DateNum.lineGreg(day.format(now.getTime()), n, true) : DateNum.line(day.format(now.getTime()), n, true);
    }

    /** "06/06/1448|16/11/2026" (Hijri moved by the offset), or null without an Umm al-Qura calendar */
    private static String numericParts(Calendar now, String tz, int shift) {
        try {
            android.icu.util.Calendar h = android.icu.util.Calendar.getInstance(ZoneIcu.of(tz), new android.icu.util.ULocale("en@calendar=islamic-umalqura"));
            if (!"islamic-umalqura".equals(h.getType())) return null;
            h.setTime(shift == 0 ? now.getTime() : HijriShift.date(now, shift));
            Calendar g = Calendar.getInstance(ZoneFix.zone(tz));
            g.setTimeInMillis(now.getTimeInMillis());
            return DateNum.ddmmyyyy(h.get(android.icu.util.Calendar.DAY_OF_MONTH), h.get(android.icu.util.Calendar.MONTH) + 1, h.get(android.icu.util.Calendar.YEAR))
                    + "|" + DateNum.ddmmyyyy(g.get(Calendar.DAY_OF_MONTH), g.get(Calendar.MONTH) + 1, g.get(Calendar.YEAR));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String englishDate(Calendar now, String tz, int shift, boolean numeric, boolean noHijri) {
        java.util.TimeZone zone = ZoneFix.zone(tz);
        SimpleDateFormat greg = new SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH), day = new SimpleDateFormat("EEEE", Locale.ENGLISH);   // 4.3.3: with the year
        greg.setTimeZone(zone);
        day.setTimeZone(zone);
        String g = greg.format(now.getTime());
        if (numeric) {
            String n = numericParts(now, tz, shift);
            if (n != null) return noHijri ? DateNum.lineGreg(day.format(now.getTime()), n, false) : DateNum.line(day.format(now.getTime()), n, false);
        }
        if (noHijri) return day.format(now.getTime()) + ", " + g;
        try {
            android.icu.util.Calendar h = android.icu.util.Calendar.getInstance(ZoneIcu.of(tz),
                    new android.icu.util.ULocale("en@calendar=islamic-umalqura"));
            if (!"islamic-umalqura".equals(h.getType())) return g;
            h.setTime(shift == 0 ? now.getTime() : HijriShift.date(now, shift));
            String hijri = MonthNames.hijriEn(day.format(now.getTime()), h.get(android.icu.util.Calendar.DAY_OF_MONTH),
                    h.get(android.icu.util.Calendar.MONTH), h.get(android.icu.util.Calendar.YEAR));
            return hijri.isEmpty() ? g : hijri + "  –  " + g;
        } catch (RuntimeException e) {
            return g;
        }
    }

    // ---------------------------------------------------------------- glow

    /** «توهّج لون التطبيق»: off (default) makes no view and no gradient, light is a third of the strength, on is full. Applies at once, from the look's repaint. */
    private void applyGlowMode() {
        float share = GlowLook.share(GlassStyle.glowMode());
        if (share == glowShare && (share == 0f) == (glow == null)) return;
        glowShare = share;
        if (share == 0f) {
            if (glow != null) { glow.animate().cancel(); glowRoot.removeView(glow); glow = null; glowDrawable = null; glowAlphaTo = -1f; }
            return;
        }
        if (glow == null) {
            glow = new View(this);
            glowRoot.addView(glow, Math.min(glowSlot, glowRoot.getChildCount()), new FrameLayout.LayoutParams(-1, -1));
            glowAlphaTo = -1f;
        }
        setGlow(glowColor);
        glow.setAlpha(glowAlphaTo < 0 ? 1f : glowAlphaTo);
    }

    private void setGlow(int color) {
        glowColor = color;
        if (glow == null) return;
        if (glowDrawable == null) {
            glowDrawable = new GradientDrawable();
            glowDrawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
            glowDrawable.setGradientCenter(0.5f, 0.78f);
            glowDrawable.setGradientRadius(getResources().getDisplayMetrics().widthPixels * GlowLook.RADIUS);
            glow.setBackground(glowDrawable);
        }
        glowDrawable.setColors(new int[]{withAlpha(color, GlowLook.CENTRE_ALPHA * glowShare), withAlpha(color, GlowLook.MID_ALPHA * glowShare), Color.TRANSPARENT});
    }

    /**
     * The glow takes the focused tile's colour at once: ONE repaint per move. (Step 31 made it a 450 ms fade, a new full-screen gradient on every frame; the cross-fade of two
     * full-screen layers that replaced it made the tray janky on 193: 31 % against 19.6 %. No fade, nothing stacked.) The same colour again does nothing.
     */
    private void animateGlow(int to) {
        if (to != glowColor) setGlow(to);
    }

    private void fadeGlow(float alpha) {
        // each move between tiles asks again for the same values: an animator is started only when the target changes (a held arrow started two every step)
        if (glow != null && glowAlphaTo != alpha) { glowAlphaTo = alpha; glow.animate().alpha(alpha).setDuration(Motion.MS).setInterpolator(Motion.ease()).start(); }
        // The grid sits over the photo's busiest middle part, so dim it while browsing apps.
        float shade = alpha < 1f ? 1f : 0f;
        if (shadeTo != shade) { shadeTo = shade; gridShade.animate().alpha(shade).setDuration(Motion.MS).setInterpolator(Motion.ease()).start(); }
    }

    // ---------------------------------------------------------------- wallpaper

    private ImageView wallpaperLayer() {
        ImageView v = new ImageView(this);
        v.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return v;
    }

    // Hosted photos (photos/index.json in the repo) join the bundled ones. Each is downloaded once into
    // files/photos and reused; offline or on any failure the next bundled photo is shown instead.
    static final String PHOTO_BASE = "https://raw.githubusercontent.com/tshi-dev/UrTV/master/photos/";
    static final long INDEX_MAX_AGE_MS = 24 * 3600 * 1000L;

    static final int PHOTO_SET = 5, PHOTO_PERIOD_DAYS = 4;

    /** This 4-day period's 5 hosted photos: the same Park-Miller draw as photoSet() in the screensaver page. */
    private String[] remotePhotos() {
        String s = prefs().getString("photoIndex", "");
        if (s.isEmpty()) return new String[0];
        String[] all = s.split(",");
        java.util.Calendar c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Riyadh"));
        java.util.Calendar utc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH));
        long day = Math.floorDiv(utc.getTimeInMillis(), 86400000L);
        long seed = (Math.floorDiv(day, PHOTO_PERIOD_DAYS) % 2147483646L) + 1;
        List<String> pick = new ArrayList<>();
        for (int guard = 0; pick.size() < Math.min(PHOTO_SET, all.length) && guard < 1000; guard++) {
            seed = (seed * 48271L) % 2147483647L;
            String f = all[(int) (seed % all.length)];
            if (!pick.contains(f)) pick.add(f);
        }
        return pick.toArray(new String[0]);
    }

    /** Keep only the current set on disk. */
    private void pruneCachedPhotos(String[] keep) {
        java.io.File[] files = new java.io.File(getFilesDir(), "photos").listFiles();
        if (files == null) return;
        java.util.Set<String> k = new java.util.HashSet<>(Arrays.asList(keep));
        for (java.io.File f : files) if (!k.contains(f.getName())) f.delete();
    }

    /** a photo or the index that failed is not asked for again for this long: not at every rotation of the wallpaper */
    static final long RETRY_MS = 60 * 60 * 1000L;
    private final Map<String, Long> photoFailed = new HashMap<>();

    private boolean failedRecently(String what) {
        Long at = photoFailed.get(what);
        return at != null && System.currentTimeMillis() - at < RETRY_MS;
    }

    /** Off the main thread: refresh the hosted photo list once a day (after a failure, again in an hour). */
    private void refreshPhotoIndex() {
        if (System.currentTimeMillis() - prefs().getLong("photoIndexAt", 0) < INDEX_MAX_AGE_MS) return;
        if (failedRecently("index.json")) return;
        try {
            org.json.JSONArray a = new org.json.JSONArray(new String(download(PHOTO_BASE + "index.json", Bounded.JSON_MAX), "UTF-8"));
            List<String> names = new ArrayList<>();
            for (int i = 0; i < a.length(); i++) {
                String f = a.getJSONObject(i).optString("f");
                if (f.matches("[A-Za-z0-9_-]+\\.jpg")) names.add(f);
            }
            if (!names.isEmpty()) prefs().edit()
                    .putString("photoIndex", android.text.TextUtils.join(",", names))
                    .putLong("photoIndexAt", System.currentTimeMillis()).apply();
        } catch (Exception ignored) {
            photoFailed.put("index.json", System.currentTimeMillis());
        }
    }

    /** At most max bytes (Bounded): the photo list is JSON_MAX, a photo IMAGE_MAX; a longer answer is an IOException and nothing is kept. */
    private static byte[] download(String url, int max) throws java.io.IOException {
        java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        NetLog.count(String.valueOf(c.getURL()));   // G3: the request log (NetLog)
        c.setConnectTimeout(10000);
        c.setReadTimeout(30000);
        try {
            if (c.getResponseCode() != 200) throw new java.io.IOException("HTTP " + c.getResponseCode());
            if (c.getContentLength() > max) throw new java.io.IOException("too large");
            try (java.io.InputStream in = c.getInputStream()) { return Bounded.readAll(in, max); }
        } finally {
            c.disconnect();
        }
    }

    /** Whether these bytes are a picture that decodes (its size is read, not the pixels) and is not absurdly large. */
    private static boolean validImage(byte[] data) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, o);
        return o.outWidth > 0 && o.outHeight > 0 && o.outWidth <= 12000 && o.outHeight <= 12000;
    }

    /** Opens the same photo again: the first pass reads only its size. */
    private interface Source { java.io.InputStream open() throws java.io.IOException; }

    /** A photo at about the screen's width: a 4000 px original is not held in memory to be shown on 1920 px. */
    private Bitmap decode(Source source) throws java.io.IOException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (java.io.InputStream in = source.open()) {
            if (in == null) return null;
            BitmapFactory.decodeStream(in, null, o);
        }
        o.inJustDecodeBounds = false;
        o.inSampleSize = Bounded.sampleSize(o.outWidth, getResources().getDisplayMetrics().widthPixels);
        o.inPreferredConfig = Bitmap.Config.RGB_565;
        try (java.io.InputStream in = source.open()) {
            return in == null ? null : BitmapFactory.decodeStream(in, null, o);
        }
    }

    // The owner's own photos («صوري»), read from the screensaver app's ConfigProvider (READ_CONFIG, read only).
    static final Uri OWN = Uri.parse("content://sa.urtv.screensaver.config/own");

    /** {how many, mode, rev}: none, mix and 0 when the screensaver app has none or does not answer */
    private Object[] ownInfo() {
        try (Cursor c = getContentResolver().query(OWN, new String[] {"n", "mode", "rev", "home"}, null, null, null)) {   // no "dates": the screensaver app does not stat every photo for the home's wallpaper
            // an older screensaver app answers any path with its settings instead: no "n" column, no photos
            if (c != null && c.getColumnIndex("n") == 0 && c.moveToFirst()) {
                int h = c.getColumnIndex("home");   // the home's own choice for «صوري» («مع صورنا», «فقط», «بدون»); an older screensaver says only the mode
                String home = h >= 0 && c.getString(h) != null ? c.getString(h) : c.getString(1);
                return new Object[] {"off".equals(home) ? 0 : c.getInt(0), "off".equals(home) ? "mix" : home, c.getLong(2)};
            }
        } catch (RuntimeException ignored) { }
        return new Object[] {0, "mix", 0L};
    }

    // 4.3: the packs the home's wallpaper rotates (the screensaver app's «المظهر والخلفيات»): which packs and how many photos each, whether «صورنا» is in, and a revision
    static final Uri HOME_PACKS = Uri.parse("content://sa.urtv.screensaver.config/homepacks");
    static final Uri HOME_PACK = Uri.parse("content://sa.urtv.screensaver.config/homepack");

    /** {id -> photos (LinkedHashMap), ours (Boolean), rev (String)}; no packs, ours on and no revision when the screensaver app is older or does not answer */
    private Object[] homePackInfo() {
        java.util.Map<String, Integer> packs = new java.util.LinkedHashMap<>();
        boolean ours = true;
        String rev = "";
        try (Cursor c = getContentResolver().query(HOME_PACKS, null, null, null, null)) {
            if (c != null && c.getColumnIndex("id") >= 0) {   // an older screensaver answers any path with its settings: no such column
                int id = c.getColumnIndex("id"), n = c.getColumnIndex("n"), o = c.getColumnIndex("ours"), r = c.getColumnIndex("rev");
                while (c.moveToNext()) {
                    String pack = c.getString(id);
                    if (pack != null && !pack.isEmpty() && c.getInt(n) > 0) packs.put(pack, c.getInt(n));
                    ours = c.getInt(o) != 0;
                    rev = c.getString(r);
                }
            }
        } catch (RuntimeException ignored) { }
        return new Object[] {packs, ours, rev};
    }

    /** Off the main thread: when the owner's photos or the home's packs changed since the wallpaper was last chosen, show one now. */
    private void checkOwnChanged() {
        new Thread(() -> {
            long rev = (Long) ownInfo()[2];
            String packsRev = (String) homePackInfo()[2];
            if (rev != prefs().getLong("ownRev", 0) || !packsRev.equals(prefs().getString("homePacksRev", ""))) runOnUiThread(() -> { if (!isDestroyed() && resumed) nextWallpaper(); });
        }).start();
    }

    private boolean wallStay;   // 4.4 beta 4: the next nextWallpaper() shows the photo it showed (returnWallpaper), not the next one

    /**
     * 4.4 beta 4 (Xiaomi A14: a new photo at every return to the home): the return's photo. The next one only when the interval is up (WallTurn),
     * else the one shown before (decoded again when it was let go), and the timer keeps the rest of its interval.
     */
    private void returnWallpaper() {
        long at = prefs().getLong("wallpaperAt", 0), now = System.currentTimeMillis();
        if (WallTurn.due(at, now, WALLPAPER_INTERVAL_MS)) { nextWallpaper(); return; }
        wallStay = true;
        nextWallpaper();
        scheduleWallpaper(WallTurn.left(at, now, WALLPAPER_INTERVAL_MS));
    }

    /** Decode the next photo off the main thread, then crossfade it in over the current one (the same one again after returnWallpaper). */
    private void nextWallpaper() {
        final boolean stay = wallStay;
        wallStay = false;
        handler.removeCallbacks(rotateWallpaper);
        if (scenesOn) return;   // 4.3.3 «الآفاق»: no ordinary photo is decoded while the scene home shows
        if (wallpapers.length == 0) return;
        if (!stay) prefs().edit().putLong("wallpaperAt", System.currentTimeMillis()).apply();   // the interval starts now (on this thread: the return's check sees it at once)
        final int gen = ++wallGen;   // a photo that arrives after a newer request is not shown
        new Thread(() -> {
            // 4.3.1 hotfix: what is on the device (the screensaver app's provider, the disk) is read HERE, off the main thread: a dead screensaver process cold-starts for the first query and used to block the home's return
            final Object[] own = ownInfo();
            final int ownCount = (Integer) own[0];
            final boolean only = ownCount > 0 && "only".equals(own[1]);
            final Object[] hp = homePackInfo();
            @SuppressWarnings("unchecked") final java.util.Map<String, Integer> packs = only ? new java.util.LinkedHashMap<String, Integer>() : (java.util.Map<String, Integer>) hp[0];
            if ((Long) own[2] != prefs().getLong("ownRev", 0) || !((String) hp[2]).equals(prefs().getString("homePacksRev", ""))) prefs().edit().putLong("ownRev", (Long) own[2]).putString("homePacksRev", (String) hp[2]).apply();
            final String[] remote = only ? new String[0] : remotePhotos();
            int packSum = 0;
            for (int n : packs.values()) packSum += n;
            // ours (bundled and hosted) come first, then the owner's, then the packs'; ours is out when the setting says so, but never when nothing else is there
            final int base = only ? 0 : ((Boolean) hp[1] || ownCount + packSum == 0) ? wallpapers.length + remote.length : 0;
            final int packBase = base + ownCount;
            final int total = only ? ownCount : packBase + packSum;
            if (wallIndex < 0) wallIndex = prefs().getInt("wallpaper", -1);
            if (!stay || wallIndex < 0) {   // the next one; a return within the interval keeps its own (wallIndex % total below: the same photo)
                wallIndex = (wallIndex + 1) % (total * 4096);   // a counter: the position inside a round, and the round
                prefs().edit().putInt("wallpaper", wallIndex).apply();
            } else wallIndex %= total * 4096;
            final int idx = only ? wallIndex % total : WallShuffle.of(total, wallIndex / total)[wallIndex % total];
            final String fallback = "wallpapers/" + wallpapers[idx % wallpapers.length];
            // 1. what is on the device now, at once: the owner's photo, the hosted one already kept, else the bundled one
            Bitmap bm = null;
            java.io.File f = null;
            String packKey = null;
            if (!only && idx >= packBase) {   // a pack's photo: the screensaver app's provider serves it (read only)
                int k = idx - packBase;
                for (java.util.Map.Entry<String, Integer> e : packs.entrySet()) {
                    if (k < e.getValue()) {
                        final Uri photo = HOME_PACK.buildUpon().appendPath(e.getKey()).appendPath(String.valueOf(k + 1)).build();
                        try { bm = decode(() -> getContentResolver().openInputStream(photo)); packKey = "pack:" + e.getKey() + ":" + (k + 1); } catch (java.io.IOException | RuntimeException ignored) { }
                        break;
                    }
                    k -= e.getValue();
                }
            } else if (idx >= base) {
                final Uri photo = OWN.buildUpon().appendPath(String.valueOf(idx - base + 1)).build();
                try {
                    bm = decode(() -> getContentResolver().openInputStream(photo));
                } catch (java.io.IOException | RuntimeException ignored) { }
            } else if (idx >= wallpapers.length && idx - wallpapers.length < remote.length) {
                f = new java.io.File(getFilesDir(), "photos/" + remote[idx - wallpapers.length]);
                if (f.isFile()) {
                    final java.io.File kept = f;
                    try { bm = decode(() -> new java.io.FileInputStream(kept)); } catch (java.io.IOException | RuntimeException ignored) { }
                }
            }
            final boolean needsDownload = f != null && bm == null;
            String key = bm == null ? null : packKey != null ? packKey : idx >= base ? "own:" + (idx - base + 1) + ":" + own[2] : f != null ? "photo:" + f.getName() : null;
            if (bm == null) {
                try { bm = decode(() -> getAssets().open(fallback)); key = "bundled:" + fallback; }
                catch (java.io.IOException ignored) { }
            }
            showWallpaperIfCurrent(bm, gen, key);
            if (gen == wallGen) GlassSource.update(this, bm, key);   // the glass look's backdrop: made again only for another photo
            if (gen == wallGen) readGlassBackdrop();   // 4.5 «زجاجي»: the bar's glass follows it
            if (only) return;   // no network while only the owner's photos show
            if (!NetGate.allowed(resumed, NetGateAndroid.displayOn(this))) return;   // another app came up, or standby: no network; the next rotation on return asks
            // 2. then the network: the photo list (once a day), the cache pruned, and the hosted photo if it was missing
            refreshPhotoIndex();
            pruneCachedPhotos(remotePhotos());
            if (!needsDownload || !Arrays.asList(remotePhotos()).contains(f.getName()) || failedRecently(f.getName())) return;
            try {
                byte[] data;
                try { data = download(PHOTO_BASE + f.getName(), Bounded.IMAGE_MAX); }
                catch (java.io.IOException e) { photoFailed.put(f.getName(), System.currentTimeMillis()); throw e; }
                if (!validImage(data)) { photoFailed.put(f.getName(), System.currentTimeMillis()); return; }   // not kept, not asked for again for an hour
                java.io.File dir = f.getParentFile();
                if (dir.isDirectory() || dir.mkdirs()) {
                    java.io.File tmp = new java.io.File(dir, f.getName() + ".tmp");
                    data = PhotoShrink.toSize(data, Ram.low(this) ? PhotoShrink.LOW : PhotoShrink.screenMax(this));   // P8: kept at screen size; G3: the TV's own (1280 on a 720p screen), 1280 on a weak TV («خفيف»)
                    try (java.io.FileOutputStream out = new java.io.FileOutputStream(tmp)) { out.write(data); }
                    if (!tmp.renameTo(f)) tmp.delete();
                }
                final java.io.File got = f;
                Bitmap fresh = got.isFile() ? decode(() -> new java.io.FileInputStream(got)) : null;
                showWallpaperIfCurrent(fresh, gen, "photo:" + got.getName());
                if (gen == wallGen) GlassSource.update(this, fresh, "photo:" + got.getName());
                if (gen == wallGen) readGlassBackdrop();
            } catch (java.io.IOException | RuntimeException ignored) { }
        }).start();
        scheduleWallpaper(WALLPAPER_INTERVAL_MS);
    }

    private int wallGen;   // counts the requests for a wallpaper (main thread)

    /** From the loading thread: shown only if no newer request has been made since this one started. */
    private void showWallpaperIfCurrent(Bitmap bm, int gen, String key) {
        if (bm != null) runOnUiThread(() -> { if (gen == wallGen) showWallpaper(bm, key); });
    }

    private void showWallpaper(Bitmap bm, String key) {
        if (bm == null || isDestroyed()) return;
        wallShownKey = key;
        // Swap roles: the old front becomes the back, the new photo fades in on top.
        final ImageView old = wallFront;
        wallFront = wallBack;
        wallBack = old;
        ViewGroup root = (ViewGroup) wallFront.getParent();
        root.removeView(wallFront);
        root.addView(wallFront, 1, new FrameLayout.LayoutParams(-1, -1));
        wallFront.setImageBitmap(bm);
        wallFront.setScaleX(1f);   // a layer never comes back with the zoom of the photo it showed before (PhotoGlass zooms the new one when it has a cut-out)
        wallFront.setScaleY(1f);
        wallFront.setTranslationX(0);   // nor its crop (PhotoGlass frames the new one)
        wallFront.setTranslationY(0);
        if (photoGlass != null) photoGlass.show(bm, key, NetGate.allowed(resumed, NetGateAndroid.displayOn(this)));   // after the swap: its zoom is the new photo's
        fullPhotoShown = true;
        logFirstPhoto("full photo");
        if (!sharpLogged) { sharpLogged = true; android.util.Log.i("UrTVHome", "resume to sharp photo: " + (SystemClock.elapsedRealtime() - resumeAt) + " ms"); }
        final Bitmap forSmall = bm;
        new Thread(() -> saveSmallPhoto(forSmall), "wall-small").start();
        wallFront.setAlpha(0f);
        final ImageView shown = wallFront;
        shown.animate().alpha(1f).setDuration(firstWallpaper ? 600 : 2000).withEndAction(() -> {
            // release the previous photo: the view this fade was above, not whichever is behind by now (a newer photo may have swapped them)
            if (old != wallFront && old != shown) old.setImageDrawable(null);
        }).start();
        firstWallpaper = false;
    }

    private boolean firstWallpaper = true;

    private volatile boolean resumed;   // (read by the photo thread too) home is in front: only then does the photo rotation run

    private void scheduleWallpaper(long delay) {
        handler.removeCallbacks(rotateWallpaper);
        if (!resumed) return;
        handler.postDelayed(rotateWallpaper, Math.max(1000, delay));
    }

    /** The art redrawn at the size a tile shows it (never enlarged); a banner fills the 16:9 card, an icon fits inside it. */
    private Drawable fitted(Drawable d, boolean banner) {
        int w = Math.round(getResources().getDisplayMetrics().widthPixels / (float) MIN_DOCK * 1.12f), h = w * 9 / 16;   // the focused tile grows 12%
        int iw = d.getIntrinsicWidth(), ih = d.getIntrinsicHeight();
        if (iw <= 0 || ih <= 0) return d;
        float f = banner ? Math.max(w / (float) iw, h / (float) ih) : Math.min(h / (float) iw, h / (float) ih);
        if (f >= 1f) return d;
        try {
            int bw = banner ? w : Math.max(1, Math.round(iw * f)), bh = banner ? h : Math.max(1, Math.round(ih * f));
            Bitmap bm = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            int dw = Math.round(iw * f), dh = Math.round(ih * f);
            d.setBounds((bw - dw) / 2, (bh - dh) / 2, (bw - dw) / 2 + dw, (bh - dh) / 2 + dh);
            d.draw(c);
            return new BitmapDrawable(getResources(), bm);
        } catch (RuntimeException | OutOfMemoryError e) {
            return d;
        }
    }

    /** Average the banner's vivid pixels, then push saturation so the glow reads as a colour. */
    private int sampleColor(String pkg, Drawable d) {
        Integer cached = colorCache.get(pkg);
        if (cached != null) return cached;
        int color = 0xFF3A3A3A;
        try {
            Bitmap bm = Bitmap.createBitmap(24, 14, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            d.setBounds(0, 0, 24, 14);
            d.draw(c);
            float[] hsv = new float[3];
            double r = 0, g = 0, b = 0, w = 0;
            for (int y = 0; y < 14; y++) for (int x = 0; x < 24; x++) {
                int p = bm.getPixel(x, y);
                if (Color.alpha(p) < 128) continue;
                Color.colorToHSV(p, hsv);
                double weight = 0.05 + hsv[1] * hsv[2];
                r += Color.red(p) * weight; g += Color.green(p) * weight; b += Color.blue(p) * weight; w += weight;
            }
            bm.recycle();
            if (w > 0) {
                Color.colorToHSV(Color.rgb((int) (r / w), (int) (g / w), (int) (b / w)), hsv);
                if (hsv[1] > 0.12f) { hsv[1] = Math.max(hsv[1], 0.75f); hsv[2] = 0.85f; }
                else { hsv[1] = 0f; hsv[2] = 0.45f; }
                color = Color.HSVToColor(hsv);
            }
        } catch (RuntimeException ignored) { }
        colorCache.put(pkg, color);
        return color;
    }

    // ---------------------------------------------------------------- entrance

    private void playEntrance() {
        View header = hero.getChildAt(0);
        header.setAlpha(0f);
        header.setTranslationY(-dp(12));
        header.animate().alpha(1f).translationY(0).setDuration(500).setStartDelay(80).start();
        title.setAlpha(0f);
        title.animate().alpha(1f).setDuration(500).setStartDelay(200).start();
        for (int i = 0; i < dockRow.getChildCount(); i++) {
            View t = dockRow.getChildAt(i);
            t.setAlpha(0f);
            t.setTranslationY(dp(24));
            t.animate().alpha(1f).translationY(0).setInterpolator(new DecelerateInterpolator(2f))
                    .setDuration(450).setStartDelay(160 + i * 60L).start();
        }
    }

    // ---------------------------------------------------------------- helpers

    /** Switch to a passthrough input (HDMI1, HDMI2, AV, …). The standard
     *  TvContract passthrough URI only resolves when a Live-TV app registers
     *  it as implicit; some vendor TV apps (TCL's com.tcl.tv/.TVActivity on
     *  this hardware) handle it only when the intent names the component
     *  explicitly. Try the implicit intent first, then every VIEW activity of
     *  each live TV app this TV has (TvVendor: TCL, CultraView, MediaTek…),
     *  then the TV's own source picker, then its live TV app, then Settings. */
    private void openPassthroughInput(TvInputInfo t) {
        Uri uri = TvContract.buildChannelUriForPassthroughInput(t.getId());
        Intent base = new Intent(Intent.ACTION_VIEW, uri);
        base.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (launch(base)) return;
        for (String live : TvVendor.usable(getString(R.string.live_tv_package), TvVendor.LIVE_TV, c -> isInstalled(TvVendor.pkg(c)))) {
            // Query every activity in the Live-TV package that advertises VIEW
            // and try each with the URI.
            Intent probe = new Intent(Intent.ACTION_VIEW).setPackage(TvVendor.pkg(live));
            List<ResolveInfo> handlers = getPackageManager().queryIntentActivities(probe,
                    PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo ri : handlers) {
                Intent explicit = new Intent(Intent.ACTION_VIEW, uri);
                explicit.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
                explicit.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (launch(explicit)) return;
            }
        }
        Intent panel = systemInputs();
        Intent signal = cvteSignal();
        String step = TvVendor.inputFallback(inputPickerPkg != null, panel != null, signal != null, liveTvPkg != null);   // #54: Skyworth, MediaTek: no app took the link
        String fallback = inputPickerPkg != null ? inputPickerPkg : liveTvPkg;
        if ("inputs".equals(step)) {
            launchGuarded(ParentalPolicy.INPUT_PICKER, panel, "كل المصادر");
        } else if ("signal".equals(step)) {
            launchGuarded(ParentalPolicy.INPUT_PICKER, signal, "كل المصادر");   // a CVTE board's own source screen (PR #17)
        } else if (fallback != null) {
            guarded(ParentalPolicy.INPUT_PICKER, TvVendor.pkg(fallback), "كل المصادر", () -> openPackage(fallback));
        } else {
            launchGuarded(ParentalPolicy.PILL_TV_SETTINGS, new Intent(Settings.ACTION_SETTINGS), "إعدادات التلفزيون");
        }
    }

    /** A CVTE board's own source screen (TvVendor.CVTE_SIGNAL), or null when nothing on this TV answers it. */
    private Intent cvteSignal() {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(TvVendor.CVTE_SIGNAL));
        try {
            return getPackageManager().resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY) != null ? i : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The TV system's own inputs panel (TvVendor.SYSTEM_INPUTS), or null when nothing on this TV answers it. */
    private Intent systemInputs() {
        Intent i = new Intent(TvVendor.SYSTEM_INPUTS);
        try {
            return getPackageManager().resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY) != null ? i : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * «The TV»: the tuner. Where the TV's live TV app only resumes the last input used (TCL's: the last HDMI, GitHub #54) the TV's own source picker opens instead of the wrong
     * input; elsewhere the live TV app opens as it always did (TvVendor.tvStep); with neither, the TV's settings. Behind the same locks as the inputs list.
     */
    private void openLiveTv() {
        findTvApps();
        Intent panel = systemInputs();
        String step = TvVendor.tvStep(TvVendor.pkg(liveTvPkg), inputPickerPkg != null, liveTvPkg != null, panel != null);
        if ("inputs".equals(step)) launchGuarded(ParentalPolicy.INPUT_PICKER, panel, "كل المصادر");
        else if ("live".equals(step)) guarded(ParentalPolicy.INPUT_LIVE_TV, TvVendor.pkg(liveTvPkg), "البث التلفزيوني", () -> openPackage(liveTvPkg));
        else if ("picker".equals(step)) guarded(ParentalPolicy.INPUT_PICKER, TvVendor.pkg(inputPickerPkg), "كل المصادر", () -> openPackage(inputPickerPkg));
        else launchGuarded(ParentalPolicy.PILL_TV_SETTINGS, new Intent(Settings.ACTION_SETTINGS), "إعدادات التلفزيون");
    }

    /** This TV's live TV app and source picker (TvVendor): the configured one if the TV has it, else the first known one it has; null for none. */
    private void findTvApps() {
        liveTvPkg = TvVendor.first(getString(R.string.live_tv_package), TvVendor.LIVE_TV, this::canOpen);
        inputPickerPkg = TvVendor.first(getString(R.string.input_picker_package), TvVendor.PICKERS, this::canOpen);
    }

    /** Can this candidate (a package, or package/activity) be opened on this TV? */
    private boolean canOpen(String candidate) {
        String pkg = TvVendor.pkg(candidate), cls = TvVendor.cls(candidate);
        if (!isInstalled(pkg)) return false;
        PackageManager pm = getPackageManager();
        if (cls != null) return pm.resolveActivity(new Intent().setClassName(pkg, cls), 0) != null;
        return pm.getLeanbackLaunchIntentForPackage(pkg) != null || pm.getLaunchIntentForPackage(pkg) != null
                || !pm.queryIntentActivities(new Intent(Intent.ACTION_VIEW).setPackage(pkg), PackageManager.MATCH_DEFAULT_ONLY).isEmpty();
    }

    // ---------------------------------------------------------------- the family lock (Parental)

    /**
     * Everything the home screen opens for a person comes through here: it runs {@code go} now, or after the PIN when one of the
     * path's targets is locked (ParentalPolicy). With nothing set it is a plain call.
     */
    private void guarded(int path, String pkg, String name, Runnable go) {
        Parental.guard(this, this::adhanTakes, name, ParentalPolicy.targets(path, pkg), go, () -> { });
    }

    /** An intent behind a pill or the menu: locked by the pill's own name, or by the app it opens. */
    private void launchGuarded(int path, Intent i, String name) {
        guarded(path, packageOf(i), name, () -> launch(i));
    }

    private String packageOf(Intent i) {
        if (i.getComponent() != null) return i.getComponent().getPackageName();
        ResolveInfo ri = getPackageManager().resolveActivity(i, 0);
        return ri == null || ri.activityInfo == null ? null : ri.activityInfo.packageName;
    }

    private boolean launch(Intent i) {
        try {
            if (!i.hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean isInstalled(String pkg) {
        if (pkg == null || pkg.isEmpty()) return false;
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** Opens an app by its launcher entry; "package/activity" (TvVendor) opens that activity. */
    private void openPackage(String pkg) {
        String cls = TvVendor.cls(pkg);
        if (cls != null) {
            launch(new Intent(Intent.ACTION_MAIN).setClassName(TvVendor.pkg(pkg), cls));
            return;
        }
        Intent i = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i != null) launch(i);
    }

    /**
     * Our settings: the screensaver app's settings screen, behind «قفل الإعدادات» (@urtv), as the control center's tile and the remote's
     * Settings key ask for it. Without that app (or an old one without the screen) the key falls back to the TV's own settings.
     */
    /** 4.3.1 «المدينة»: the screensaver app's settings, which open its city chooser at once (the same chooser as its first row) */
    private void openCity() {
        Intent i = new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(SAVER_PKG, SAVER_PKG + ".SettingsActivity")).putExtra("pickCity", true);
        if (getPackageManager().resolveActivity(i, 0) != null) guarded(ParentalPolicy.OUR_SETTINGS, null, S.t("المدينة"), () -> launch(i));
        else openSaverInstall();   // 4.3.2: the second app is not here: «ثبّت الجزء الثاني», never nothing
    }

    /** 4.3.2 «ثبّت الجزء الثاني»: the screen that installs the screensaver app (the settings) when a TV has only the home (SaverInstallRule) */
    void openSaverInstall() {
        guarded(ParentalPolicy.OUR_SETTINGS, null, S.t("ثبّت الجزء الثاني"), () -> launch(new Intent(this, InstallSaverActivity.class)));
    }

    private void openOurSettings(boolean orTvSettings) { openOurSettings(orTvSettings, null); }

    /**
     * 4.3.1 «تلفزيونك»: the screensaver app's hub (markets, matches, games, settings): what the control center's row, the remote's Settings key and the
     * clean home's button open now, so the games are one press away. {@code window}: "games" opens that window at once. A screensaver of 4.2 has no hub: its settings, as before.
     */
    private void openOurSettings(boolean orTvSettings, String window) {
        Intent hub = new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(SAVER_PKG, SAVER_PKG + ".YourTvHubActivity"));
        // 4.4: «تلفزيونك» always starts at its own root (a fresh task): the screensaver app keeps ONE task, so a hub resumed after Home came back with the
        // games still on top of it (4.3.4: the same for a screensaver task left open with its settings or preview)
        hub.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (window != null) hub.putExtra("open", window);
        Intent i = getPackageManager().resolveActivity(hub, 0) != null ? hub
                : new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(SAVER_PKG, SAVER_PKG + ".SettingsActivity"));
        if (getPackageManager().resolveActivity(i, 0) != null) {
            guarded(ParentalPolicy.OUR_SETTINGS, null, S.t("تلفزيونك"), () -> launch(i));
        } else if (orTvSettings) {
            launchGuarded(ParentalPolicy.REMOTE_SETTINGS, new Intent(Settings.ACTION_SETTINGS), "إعدادات التلفزيون");
        } else {
            openSaverInstall();   // 4.3.2: «تلفزيونك», the clean home's button, the pill: the second app is missing, so it is offered, never nothing
        }
    }

    /**
     * 4.4 beta 4 (the tester, build 654: in «كنبة» nothing on the home said «الإعدادات», and «تلفزيونك» in the control centre was not read as them):
     * «الإعدادات» of the control centre and of the apps row open our settings screen itself, a fresh task (Back is the home), under the same lock as
     * «تلفزيونك». No settings screen (the second app missing): what «تلفزيونك» does.
     */
    private void openSettingsScreen() {
        Intent i = new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(SAVER_PKG, SAVER_PKG + ".SettingsActivity"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (getPackageManager().resolveActivity(i, 0) == null) { openOurSettings(false); return; }
        guarded(ParentalPolicy.OUR_SETTINGS, null, S.t("الإعدادات"), () -> launch(i));
    }

    /** The screensaver now: its full-screen preview (the same page; Home or Back leaves it). An older app without it opens its settings. */
    private void startSaver() {
        Intent i = new Intent().setComponent(new ComponentName(SAVER_PKG, SAVER_PKG + ".MainActivity"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);   // fresh each time, with today's settings
        if (getPackageManager().resolveActivity(i, 0) != null) launch(i); else openPackage(SAVER_PKG);
    }

    static final int GOLD = 0xFFD6AF6B;
    static final String SAVER_PKG = "sa.urtv.screensaver";
    private final Map<String, Typeface> fonts = new HashMap<>();

    /** Thmanyah from assets/fonts; any old family name maps to the nearest weight. */
    /** 4.4 beta 3: «الوضع الخفيف» came on by itself: said once, after the first frame, never over the adhan or a kick-off notice (then at the next resume) */
    private void liteNotice() {
        String text = LiteAutoStore.pending();
        if (text == null || !resumed || isDestroyed() || isFinishing()) return;
        if (NowPlaying.adhanLive() || KickoffOverlay.showing()) return;
        if (HomeNotice.show(homeRoot, text, font("regular"), Ram.low(this))) LiteAutoStore.shown(this);
    }

    private Typeface font(String family) {
        String file = family.contains("display") ? "ThmanyahDisplay-Bold"
                : family.contains("thin") || family.contains("light") ? "Thmanyah-Light"
                : family.contains("medium") ? "Thmanyah-Medium" : "Thmanyah-Regular";
        Typeface tf = fonts.get(file);
        if (tf == null) {
            try { tf = Typeface.createFromAsset(getAssets(), "fonts/" + file + ".otf"); }
            catch (RuntimeException e) { tf = Typeface.create("sans-serif", Typeface.NORMAL); }
            fonts.put(file, tf);
        }
        return tf;
    }

    private TextView text(float sp, String family, int color) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTypeface(font(family));
        t.setTextColor(color);
        return t;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static FrameLayout.LayoutParams lp(int w, int h, int gravity, int l, int t, int r, int b) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(w, h, gravity);
        p.setMargins(l, t, r, b);
        return p;
    }

    private static LinearLayout.LayoutParams llp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(l, t, r, b);
        return p;
    }

    /** llp with the side margins by reading order: start is the right in Arabic (exactly llp(w, h, end, t, start, b)), the left in English */
    private static LinearLayout.LayoutParams sideLlp(int w, int h, int start, int t, int end, int b) {
        return Dir.rtl() ? llp(w, h, end, t, start, b) : llp(w, h, start, t, end, b);
    }

    private static int withAlpha(int c, float a) {
        return Color.argb(Math.round(a * 255), Color.red(c), Color.green(c), Color.blue(c));
    }


    /** The adhan's rule for a key; the PIN keypad (a dialog, with a key path of its own) asks it first too, so nothing in the family lock can hold the adhan. */
    private boolean adhanTakes(KeyEvent e) {
        boolean adhan = NowPlaying.adhanLive();
        boolean card = duaCard != null && duaCard.showing();   // 4.4: a dua's card is the bar for the keys: OK on its «إيقاف» stops it
        int stop = adhanKeys.decide(e.getKeyCode(), e.getAction(), e.getRepeatCount(), adhan, adhanBar.getVisibility() == View.VISIBLE || card,
                getCurrentFocus() == adhanBar || (card && getCurrentFocus() == duaCard.stop), adhan && NowPlaying.adhanStopKey(this));
        if (stop == AdhanKeys.STOP) NowPlaying.stopAdhan(this);
        return stop != AdhanKeys.PASS;
    }

    /** the home in front (onResume to onPause), for keyFromService */
    private static java.lang.ref.WeakReference<MainActivity> front = new java.lang.ref.WeakReference<>(null);

    /** TypeService saw the Settings key while this home is in front, on a TV whose system keeps it from us (SettingsKey.TO_HOME): our key path */
    static void keyFromService(KeyEvent e) {
        MainActivity a = front.get();
        if (a != null && a.resumed && !a.isDestroyed()) a.dispatchKeyEvent(e);
    }

    /**
     * 4.4 beta 4 (the tester: `wm size 1280x720` at runtime left the home with only the clock and the weather; the app row and the card were laid out in
     * the old size, off the new screen): the home declares configChanges for the screen's size and density, so Android does not make it again; the
     * layout is built from the screen's pixels, so a change of size or of density makes the home again (what a restart does). Nothing else (the dark
     * look, the keyboard) does.
     */
    private int builtWidthDp, builtHeightDp, builtDensityDpi;

    @Override
    public void onConfigurationChanged(android.content.res.Configuration c) {
        super.onConfigurationChanged(c);
        if (builtWidthDp == 0) return;
        if (c.screenWidthDp != builtWidthDp || c.screenHeightDp != builtHeightDp || c.densityDpi != builtDensityDpi) recreate();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) watchKey();   // 4.4 beta 3: the frozen-screen watchdog
        if (e.getAction() == KeyEvent.ACTION_DOWN) keyedSinceHome = true;   // the person has moved: their place is kept from now on
        if (e.getAction() != KeyEvent.ACTION_DOWN || !DebugFlags.on(this, "keytrace")) return dispatchKey(e);
        KeyTrace trace = KeyTrace.begin(this, e);   // debug only: where the time of this key goes (dock.sh debug keytrace on)
        boolean r = dispatchKey(e);
        trace.dispatched();
        return r;
    }

    private boolean dispatchKey(KeyEvent e) {
        // while the adhan plays: OK on its bar stops it, and with «إيقاف الأذان بأي زر» any key does; that press is consumed whole
        if (adhanTakes(e)) return true;
        if (reminder != null && reminder.key(e)) return true;   // a reminder's card is up: the first press is the card's
        // the remote's shortcut keys (YouTube, Netflix, Settings, the star): with the stock launcher off nothing else answers them
        if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0)
            android.util.Log.d("UrTVKeys", "key=" + e.getKeyCode() + " scan=" + e.getScanCode());   // for the TV test: does the shortcut key reach the home screen at all? (codes only)
        // «أزرار الريموت»: a button of this remote the owner gave an app that is on the TV opens it here, first (Xiaomi's burst is one press; kids mode's
        // hidden app opens nothing; the family PIN as for its tile). A button on its default goes to RemoteKeys below, exactly as before anyone chose.
        String keyId = KeyButton.of(e);
        String keyChoice = keyId == null ? null : Dock.keyApp(this, keyId);
        if (KeyApps.chosen(keyChoice) && isInstalled(keyChoice) || homeButtons.holding(e.getKeyCode())) {
            int k = homeButtons.decide(keyId, e.getKeyCode(), e.getAction(), e.getRepeatCount(), e.getEventTime(), false, keyChoice, KeyButton.burstMs());
            if (k == KeyApps.OPEN) {
                final String pkg = homeButtons.pkg;
                if (ParentalPolicy.visible(Parental.state(this), pkg)) guarded(ParentalPolicy.REMOTE_APP, pkg, labelOf(pkg), () -> openPackage(pkg));
            }
            if (k != KeyApps.PASS) return true;
        }
        RemoteKeys.Decision key = remoteKeys.decide(e.getKeyCode(), e.getScanCode(), e.getAction(), e.getRepeatCount(), e.getEventTime() - e.getDownTime(),
                RemoteKeys.opensApp(e.getKeyCode()) ? remoteInstalled() : Collections.<String>emptySet());
        if (key.kind == RemoteKeys.PASS && e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0 && RemoteKeys.unmapped(e.getKeyCode()))
            android.util.Log.i("UrTVKeys", "unmapped key=" + e.getKeyCode() + " scan=" + e.getScanCode());   // codes only: for the next report (RemoteKeys.unmapped)
        if (key.kind != RemoteKeys.PASS) {
            if (key.kind != RemoteKeys.EAT) {
                if (edit != null) leaveEdit();   // a shortcut key in «تعديل المفضلة»: what was arranged is saved, as with Home, then the key acts
                if (appsEdit != null) leaveAppsEdit();   // and in «تعديل التطبيقات»
                openRemoteKey(key);
            }
            return true;
        }
        if (edit != null && editKey(e)) return true;   // «تعديل المفضلة»: the keys it uses (FavEdit.owns); the rest go on
        if (appsEdit != null && appsEditKey(e)) return true;   // «تعديل التطبيقات»: the same
        // the Menu key on an app: its options (the long OK's box); anywhere else the Inputs dropdown, as before
        if (e.getKeyCode() == KeyEvent.KEYCODE_MENU && e.getAction() == KeyEvent.ACTION_UP && edit == null && appsEdit == null
                && getCurrentFocus() != null && getCurrentFocus().getTag() instanceof App) {
            View f = getCurrentFocus();
            optionsHintUsed();
            showAppMenu(f, ((App) f.getTag()).pkg, dockCards.contains(f));
            return true;
        }
        if (e.getKeyCode() == KeyEvent.KEYCODE_MENU && edit == null && appsEdit == null && nowCard != null && getCurrentFocus() == nowCard && !Parental.state(this).kids) {
            // 4.4 review F5 (R3): the card's own listener never saw the key (this method takes it first): Menu on «يُتلى الآن» is «شاشة سوداء», behind the same lock
            if (e.getAction() == KeyEvent.ACTION_UP) launchGuarded(ParentalPolicy.PILL_LISTEN, sa.urtv.home.tilawah.BlackScreenActivity.open(this), "تلاوة وأذان");
            return true;
        }
        if (e.getKeyCode() == KeyEvent.KEYCODE_MENU && e.getAction() == KeyEvent.ACTION_UP && edit == null && appsEdit == null && inputsAnchor() != null && !Parental.state(this).kids) {   // 4.3.4 review F2: not over an edit mode; 4.4 review F21: no pills, the first dock card holds the place
            showInputs(inputsAnchor(), ParentalPolicy.MENU_KEY);
            return true;
        }
        if (e.getKeyCode() == KeyEvent.KEYCODE_MENU && e.getAction() == KeyEvent.ACTION_UP && Parental.state(this).kids) {
            openParental();   // 4.3.3: kids mode has no pills, and maybe no app left to hold OK on: the Menu key is always a way out (the PIN first)
            return true;
        }
        // 4.4 beta 4: Back on the «يُتلى الآن» card closes it: what it holds stops (paused or playing), the focus goes to the pills; the press and its release are both taken here
        if (e.getKeyCode() == KeyEvent.KEYCODE_BACK && nowCard != null && getCurrentFocus() == nowCard && nowCard.getVisibility() == View.VISIBLE) {
            if (e.getAction() == KeyEvent.ACTION_UP && !e.isCanceled()) {
                NowPlaying.close(this);
                if (!pills.isEmpty()) pills.get(0).requestFocus();
                nowCard.setVisibility(View.GONE);   // at once; the service's word (CHANGED) follows
            }
            return true;
        }
        // On the «يُتلى الآن» card the back key (Dir: ▶ in Arabic, ◀ in English) is the previous surah and the forward key the next (a station has none: ◀ goes back to the pills as on any pill)
        if (e.getAction() == KeyEvent.ACTION_DOWN && nowCard != null && getCurrentFocus() == nowCard
                && (e.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT || e.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            NowPlaying.State s = NowPlaying.read(this);
            if (s.show && !s.radio) {
                if (e.getRepeatCount() == 0) {
                    if (e.getKeyCode() == Dir.backKey()) NowPlaying.prev(this); else NowPlaying.next(this);   // 4.4: as the surah grid and the tabs (Arabic: ▶ back)
                }
                return true;
            }
        }
        // In a right to left layout Android looks for the focus the other way round, so the rows are
        // walked by hand here: «right» always moves the highlight to the right. A held key too: its repeats
        // used to reach Android's own search, which went the other way and raced the opening pill.
        if (e.getAction() == KeyEvent.ACTION_DOWN
                && (e.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT || e.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            if (e.getRepeatCount() > 0 && sideRow()) {
                if (e.getEventTime() - lastSideMove < SIDE_REPEAT_MS) return true;   // a step at a time while held
            }
            if (moveFocusSideways(e.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT)) {
                lastSideMove = e.getEventTime();
                return true;
            }
        }
        if (e.getKeyCode() == KeyEvent.KEYCODE_BACK && backExitTakes(e)) return true;
        return super.dispatchKeyEvent(e);
    }

    private final BackExit backExit = new BackExit();
    private boolean backGuarded;   // the Back now pressed goes through BackExit (decided on its first event)

    /**
     * «الخروج من تلفزيونك» (one Back by default, as always): with «ضغطة رجوع مطوّلة» or «ثلاث ضغطات رجوع», on a TV that keeps its own home,
     * Back at the top of ours leaves only when held or pressed three times; the first ignored one of a visit says so. False: Back as always.
     */
    private boolean backExitTakes(KeyEvent e) {
        int r;
        if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) {
            String mode = BackExit.fromFragment(ConfigSnapshot.lastFragment(this));   // 4.3.4 follow-up: never a query to the screensaver on a key (the UI thread)
            boolean scrolled = scroll.getScrollY() > 0 && !dockCards.isEmpty();
            backGuarded = BackExit.guards(mode, isDefaultHome(), scrolled);
            if (!backGuarded) return false;
            r = backExit.press(mode, e.getEventTime());
        } else if (!backGuarded) {
            return false;
        } else if (e.getAction() == KeyEvent.ACTION_DOWN) {
            r = backExit.repeat(e.getEventTime(), e.getDownTime());
        } else if (e.getAction() == KeyEvent.ACTION_UP) {
            r = backExit.release(e.getEventTime(), e.getDownTime());
            backGuarded = false;
        } else {
            return true;
        }
        if (r == BackExit.LEAVE) finish();
        else if (r == BackExit.HINT)
            Toast.makeText(this, BackExit.THREE.equals(backExit.mode()) ? S.t("للخروج من تلفزيونك اضغط رجوع ثلاث مرات")
                    : S.t("للخروج من تلفزيونك اضغط رجوع مطوّلًا"), Toast.LENGTH_SHORT).show();
        return true;
    }

    private final RemoteKeys remoteKeys = new RemoteKeys();
    private final KeyApps.Outside homeButtons = new KeyApps.Outside();

    /** The apps a shortcut key opens that are installed, and that kids mode shows (one it hides counts as not there: the key then does nothing). */
    private Set<String> remoteInstalled() {
        Set<String> out = new HashSet<>();
        ParentalPolicy.State family = Parental.state(this);
        for (String pkg : RemoteKeys.PACKAGES) if (isInstalled(pkg) && ParentalPolicy.visible(family, pkg)) out.add(pkg);
        return out;
    }

    private String labelOf(String pkg) {
        try {
            return String.valueOf(getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg, 0)));
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    /** Opens what a shortcut key asked for, behind the same family lock as the tile, the pill or the Menu key it stands for. */
    private void openRemoteKey(RemoteKeys.Decision key) {
        switch (key.kind) {
            case RemoteKeys.OPEN_APP:
                guarded(ParentalPolicy.REMOTE_APP, key.pkg, labelOf(key.pkg), () -> openPackage(key.pkg));
                break;
            case RemoteKeys.OPEN_OUR_SETTINGS:   // a long press of the Settings key: OUR settings (the TV's own if our app is missing)
                if (Parental.state(this).kids && !isInstalled(SAVER_PKG)) openParental();   // 4.3.3: kids mode with no screensaver app: its «الرقابة الأبوية» row is missing, so the screen itself
                else openOurSettings(true);
                break;
            case RemoteKeys.OPEN_SETTINGS: {   // a short press of the Settings / Quick Settings key: the TV's own quick panel when it has one (behind @tvsettings), else OUR Control Center (#51), else the TV's settings
                Intent panel = new Intent(SettingsKey.NOTIFICATION_PANEL_ACTION);
                if (getPackageManager().resolveActivity(panel, 0) != null) launchGuarded(ParentalPolicy.REMOTE_SETTINGS, panel, "إعدادات التلفزيون");
                else if (control != null && centerPill != null && !Parental.state(this).kids) {
                    final View anchor = centerPill.getParent() == pillRow || pills.isEmpty() ? centerPill : pills.get(0);   // the pill may be hidden (#52): the first pill holds the place
                    guarded(ParentalPolicy.PILL_CENTER, null, ControlText.NAME, () -> control.open(anchor));
                } else launchGuarded(ParentalPolicy.REMOTE_SETTINGS, new Intent(Settings.ACTION_SETTINGS), "إعدادات التلفزيون");
                break;
            }
            case RemoteKeys.OPEN_INPUTS:
                if (inputsAnchor() != null && !Parental.state(this).kids) showInputs(inputsAnchor(), ParentalPolicy.MENU_KEY);
                else if (Parental.state(this).kids) openParental();   // 4.3.3: the star key in kids mode: the way out, as the Menu key
                break;
            case RemoteKeys.OPEN_LIVE_TV:   // the TV button (TCL's 4001): the tuner, as «البث التلفزيوني» opens it
                openLiveTv();
                break;
            default:
                break;
        }
    }

    /** While a left/right key is held, the highlight moves one item per this many ms (the pill opens in 150). */
    private static final long SIDE_REPEAT_MS = 120;
    private long lastSideMove;

    /** Whether the focus is in a row that moveFocusSideways walks (a pill or an app tile). */
    private boolean sideRow() {
        View focused = getCurrentFocus();
        ViewParent p = focused == null ? null : focused.getParent();
        if (isHorizontalRow(p)) return true;
        return p instanceof ViewGroup && isHorizontalRow(((ViewGroup) p).getParent());
    }

    private static boolean isHorizontalRow(ViewParent p) {
        return p instanceof LinearLayout && ((LinearLayout) p).getOrientation() == LinearLayout.HORIZONTAL;
    }

    /** Moves the focus to the next item of the row, on the right (true) or on the left (false). */
    private boolean moveFocusSideways(boolean right) {
        View focused = getCurrentFocus();
        if (focused == null) return false;
        ViewGroup parent = focused.getParent() instanceof ViewGroup ? (ViewGroup) focused.getParent() : null;
        if (parent == null) return false;

        ViewGroup row = null;
        View item = null;
        if (parent instanceof LinearLayout && ((LinearLayout) parent).getOrientation() == LinearLayout.HORIZONTAL) {
            // the focus is on an item of the row itself (a pill)
            row = parent;
            item = focused;
        } else {
            // the app tiles: the focus is on the card inside its box, the row is above the box
            ViewGroup above = parent.getParent() instanceof ViewGroup ? (ViewGroup) parent.getParent() : null;
            if (above instanceof LinearLayout && ((LinearLayout) above).getOrientation() == LinearLayout.HORIZONTAL) {
                row = above;
                item = parent;
            }
        }
        if (row == null) return false;
        int index = row.indexOfChild(item);
        if (index < 0) return false;
        // the row reads from the right in Arabic, so the item before the focused one is the one on the right; in English the left
        boolean rtl = row.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;   // the row as it is drawn (Dir.layout() when it was built)
        int next = FavBar.sideStep(index, row.getChildCount(), right, rtl);
        if (row instanceof DockGrid && ((DockGrid) row).rows() > 1) next = FavBar.glassSideStep(index, row.getChildCount(), ((DockGrid) row).rows(), right, rtl);   // «زجاجي» in rows: never round to the next row
        if (next < 0) return true;   // the edge of the row: stay put
        View target = row.getChildAt(next);
        if (target == null || target.getVisibility() != View.VISIBLE) return true;
        View toFocus = target;
        if (target instanceof ViewGroup) {
            ViewGroup box = (ViewGroup) target;
            for (int i = 0; i < box.getChildCount(); i++) {
                View child = box.getChildAt(i);
                if (child.isFocusable() && child.getVisibility() == View.VISIBLE) {
                    toFocus = child;
                    break;
                }
            }
        }
        if (!toFocus.isFocusable() || toFocus.getVisibility() != View.VISIBLE) return true;
        toFocus.requestFocus();
        return true;
    }

    /** 16:9 container. */
    static final class AspectFrame extends FrameLayout {
        float ratio = 9f / 16f;   // its height over its width: 16:9, or 1 for «أيقونات»' square
        AspectFrame(Context c) { super(c); }
        @Override protected void onMeasure(int w, int h) {
            int width = MeasureSpec.getSize(w);
            super.onMeasure(w, MeasureSpec.makeMeasureSpec(Math.round(width * ratio), MeasureSpec.EXACTLY));
        }
    }

    /** The favourites tray: a focused tile is brought whole into view with the tray's 24 px inset (FavBar.scrollDelta), never flush with the edge and never beside a sliver. */
    final class TraySnap extends android.widget.HorizontalScrollView {
        TraySnap(Context c) { super(c); }
        /** a tile is whole or not drawn: one only partly inside the tray (a rounding pixel of the next tile, or a half-scrolled one) is clipped away, the focused one never */
        void wholeOnly() {
            if (getChildCount() == 0 || !(getChildAt(0) instanceof android.view.ViewGroup)) return;
            android.view.ViewGroup row = (android.view.ViewGroup) getChildAt(0);
            int s = getScrollX(), w = getWidth();
            for (int i = 0; i < row.getChildCount(); i++) {
                View c = row.getChildAt(i);
                boolean whole = c.getLeft() >= s && c.getRight() <= s + w, out = c.getRight() <= s || c.getLeft() >= s + w;
                boolean clip = !(whole || out || c.hasFocus());
                if (clip != (c.getClipBounds() != null)) c.setClipBounds(clip ? NOTHING : null);   // only a tile that changes state is touched (this runs on every scroll step and focus move)
            }
        }
        private final android.graphics.Rect NOTHING = new android.graphics.Rect(0, 0, 0, 0);
        private final android.graphics.Paint chev = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Path chevPath = new android.graphics.Path();
        /**
         * «more favourites this way»: a small triangle (the Sadu mark) in the tray's 24 px inset on a side that has tiles out of view, centred on the tiles. Drawn over
         * the children, never over a tile (the inset is empty) nor the focus ring (6 px wide from 2 px in; a grown focused tile and its ring come within 9 px of the edge
         * at five). Physical sides, so in Arabic «more» is at the left: the first tile is at the right and the rest lie to the left.
         */
        @Override protected void dispatchDraw(android.graphics.Canvas c) {
            super.dispatchDraw(c);
            boolean more = canScrollHorizontally(1), before = canScrollHorizontally(-1);
            if (!more && !before) return;
            chev.setStyle(android.graphics.Paint.Style.FILL);
            chev.setColor(lightNow || "sand".equals(lookNow) ? (palette().ink & 0x00FFFFFF) | 0xCC000000 : 0xCCFFFFFF);
            float cy = getHeight() / 2f, tw = bar(6), th = bar(14), off = bar(2), sx = getScrollX(), w = getWidth();
            if (more) {
                chevPath.reset();
                chevPath.moveTo(sx + w - off, cy);
                chevPath.lineTo(sx + w - off - tw, cy - th / 2);
                chevPath.lineTo(sx + w - off - tw, cy + th / 2);
                chevPath.close();
                c.drawPath(chevPath, chev);
            }
            if (before) {
                chevPath.reset();
                chevPath.moveTo(sx + off, cy);
                chevPath.lineTo(sx + off + tw, cy - th / 2);
                chevPath.lineTo(sx + off + tw, cy + th / 2);
                chevPath.close();
                c.drawPath(chevPath, chev);
            }
        }
        @Override protected void onScrollChanged(int l, int t, int ol, int ot) { super.onScrollChanged(l, t, ol, ot); wholeOnly(); }
        @Override protected void onLayout(boolean changed, int l, int t, int r, int b) { super.onLayout(changed, l, t, r, b); wholeOnly(); }
        @Override protected int computeScrollDeltaToGetChildRectOnScreen(android.graphics.Rect r) {
            if (getChildCount() == 0) return 0;
            return FavBar.scrollDelta(r.left, r.right, getScrollX(), getWidth(), bar(FavBar.PAD_SIDE), getChildAt(0).getWidth());
        }
    }

    /** ScrollView that never auto-scrolls to the focused child; the activity scrolls it by row. */
    static final class HomeScrollView extends android.widget.ScrollView {
        HomeScrollView(Context c) { super(c); }
        @Override protected int computeScrollDeltaToGetChildRectOnScreen(android.graphics.Rect r) { return 0; }
    }

    /** The lowest edge, in the group, of what is drawn in it: INVISIBLE and GONE children skipped, translations counted. */
    static int shownBottom(ViewGroup g) {
        int low = drawnBottom(g);
        return low == 0 ? g.getHeight() : low;
    }

    private static int drawnBottom(ViewGroup g) {   // 0: nothing drawn in it
        int low = 0;
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            if (c.getVisibility() != View.VISIBLE || c.getHeight() == 0) continue;
            int inner = c instanceof ViewGroup ? drawnBottom((ViewGroup) c) : c.getHeight();
            if (inner == 0) continue;
            low = Math.max(low, Math.round(c.getTop() + inner + c.getTranslationY()));
        }
        return low;
    }
}
