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
        boolean isBanner;
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
    private LinearLayout content, dockRow;
    private FrameLayout hero;
    private LinearLayout grid;
    private View glow;
    private GradientDrawable glowDrawable;   // the one gradient, its colours changed in place (no allocation per move)
    private float glowAlphaTo = -1f, shadeTo = 0f;
    private ImageView wallBack, wallFront;
    private View gridShade;
    private String[] wallpapers = new String[0];
    private int wallIndex = -1;
    private long wallChangedAt;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable rotateWallpaper = this::nextWallpaper;
    static final long WALLPAPER_INTERVAL_MS = 10 * 60 * 1000;
    private TextView greeting, weatherLine, clock, ampm, date, zoneLine, title, appsLabel;
    private String langBuilt;   // S.lang() when this screen was built
    // «المظهر» (Look): the parts a light look recolours; built in the dark look's colours, so dark is exactly as it was
    private View scrim;
    private GradientDrawable dockBand;
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
    private PopupWindow inputsPopup;
    private HomeCard homeCard;
    private NowCard nowCard;   // «يُتلى الآن»: at the right end of the pills row while a surah or a station is on
    private Pill marketsPill, matchesPill;   // «الأسواق» and «المباريات»: they take the look like the others («رملي»: they stayed grey, step 28)
    private Pill centerPill;   // «مركز التحكم»: gold while the sleep timer runs; in the row except in kids mode (applyCenterPill)
    private ControlCenter control;   // the panel it opens
    private TextView adhanBar;   // «أذان المغرب بعد ٨ دقائق», over the top edge; while the adhan plays «أذان المغرب · أي زر يوقف الأذان»
    private final AdhanKeys adhanKeys = new AdhanKeys();
    private ReminderCard reminder;   // the card of a task with a time, over the home; the alarm (ReminderReceiver) and a return to the home bring it
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
            updateAdhanBar(ConfigSnapshot.read(MainActivity.this));   // the adhan began or ended: the bar names it, or goes
        }
    };
    // the sleep timer started, ended or was cancelled (here, or from the computer): the pill follows at once
    private final BroadcastReceiver sleepChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { updateSleepPill(); }
    };
    private final BroadcastReceiver packages = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { PhoneApps.invalidate(); buildApps(); }   // buildApps ends «تعديل المفضلة» and «تعديل التطبيقات» without saving: what they edit is stale
    };
    // favourites changed in «ترتيب المفضلة» or from ADB while the home screen is up
    private final BroadcastReceiver dockChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { PhoneApps.invalidate(); dockApps = loadDock(); buildApps(); }
    };

    // the cards were changed from the phone while the home screen is in front: follow now, not at the next return
    private final BroadcastReceiver cardsChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { if (resumed) { homeCard.refresh(); wireFocus(); } }
    };

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        FamilyDnsAndroid.status(this);   // first use in this process: undoes a Family DNS check that a crash cut short
        sa.urtv.home.setup.SetupHook.atHomeStart(this);   // a home switch that was cut off is put back (it never throws)
        Digits.init(this);   // the screensaver's «الأرقام» setting, for every number this screen writes
        langBuilt = S.lang();   // the language this screen is built in (updateClock rebuilds it when «اللغة» changes)
        dockApps = loadDock();
        findTvApps();
        cols = getResources().getInteger(R.integer.columns);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(buildUi());
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener((was, now) -> placeName());   // FavBar: the name only in the bar
        // started by the screensaver's «الأذان» row; not again after a restore (the system hands back the same intent)
        if (b == null && (getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
            openRequested(getIntent());
            phoneRequested(getIntent());   // the phone remote's «افتح»
            settingsKeyRequested(getIntent());   // the Settings key outside the home, when the TV's settings need the PIN
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
        InternalReceivers.register(this, cardsChanged, new IntentFilter(HomeCard.CHANGED));
        InternalReceivers.register(this, sleepChanged, new IntentFilter(SleepTimer.CHANGED));
        InternalReceivers.register(this, nowPlayingChanged, new IntentFilter(NowPlaying.CHANGED));
        InternalReceivers.register(this, reminderDue, new IntentFilter(ReminderReceiver.SHOW));
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        if (sa.urtv.home.setup.SetupReopen.take()) {   // «إعداد تلفزيوني» was left behind by the home switch: its progress and its done screen come back (once)
            try { guarded(ParentalPolicy.WIDGET_MATCHES, null, S.t("إعداد تلفزيوني"), () -> launch(new Intent(this, sa.urtv.home.setup.SetupActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))); } catch (RuntimeException ignored) { }   // the person opened this screen already (its own lock was passed then)
        }
        UpdateCarry.homeShown = true;   // the carry-over after our update asks whether our home is in front
        front = new java.lang.ref.WeakReference<>(this);   // TypeService's key hand-over (keyFromService)
        backExit.reset(); backGuarded = false;   // «الخروج من تلفزيونك»: a new visit, its count and its hint start again
        sa.urtv.home.tilawah.AdhanFront.home(true);   // «الأذان على شاشة التوقف فقط»: the adhan's alarm asks what is in front
        TypeService.homeFront(true);   // the Settings key is ours here (RemoteKeys): the service stops asking for keys
        A11yHeal.onStart(this);   // once per process: our accessibility service listed but not bound after an update (see A11yHeal)
        Parental.SESSION.clear();   // the home screen is in front again: an app opened with the PIN asks again next time
        if (control != null) control.forgetLast();   // «مركز التحكم» opens on its first tile again
        bellForce = true;   // [updates bell]
        IntentFilter tf = new IntentFilter(Intent.ACTION_TIME_TICK);
        tf.addAction(Intent.ACTION_TIME_CHANGED);
        tf.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        tf.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(tick, tf);
        updateClock();
        updateNowPlaying();
        homeCard.refresh();
        if (reminder != null) { reminder.refresh(); ReminderScheduler.schedule(this); }   // a card that came due while another app was in front; the alarm follows the tasks
        readAnimationScale();
        loadTvRows();   // what one watched since: «أكمل المشاهدة» moves on
        showUpdateResult();
        wireFocus();   // the «المهام» card may have come or gone with the settings
        View last = lastFocusedId == View.NO_ID ? null : findViewById(lastFocusedId);
        if (last == null && !dockCards.isEmpty()) last = dockCards.get(0);
        if (last != null) last.requestFocus();
        // Catch installs/removals that happened while another app was in front.
        if (getPackageManager().getChangedPackages(packageSeq) != null) buildApps();
        if (firstShow) {   // the entrance plays once the favourites are on screen (they come from the loader thread)
            firstShow = false;
            if (appsShown) playEntrance(); else entranceDue = true;
        }
        // New photo on return if the current one has been up for the full interval.
        if (wallTrimmed || wallIndex < 0 || SystemClock.elapsedRealtime() - wallChangedAt >= WALLPAPER_INTERVAL_MS) { wallTrimmed = false; nextWallpaper(); }
        else {
            scheduleWallpaper(WALLPAPER_INTERVAL_MS - (SystemClock.elapsedRealtime() - wallChangedAt));
            checkOwnChanged();   // the owner added or removed photos while another app was in front
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        UpdateCarry.homeShown = false;   // a slow photo check that answers now must not start the 10-minute rotation again
        sa.urtv.home.tilawah.AdhanFront.home(false);
        TypeService.homeFront(false);   // another app is coming to the front: the service answers the Settings key there (SettingsKey)
        remoteKeys.reset();   // a shortcut key opened an app: its release went there, not here
        PinPrompt.dismiss(this);
        if (edit != null) leaveEdit();   // another screen came up: what was arranged is saved
        if (appsEdit != null) leaveAppsEdit();   // the same for «تعديل التطبيقات»
        handler.removeCallbacks(rotateWallpaper);
        unregisterReceiver(tick);
        if (inputsPopup != null) inputsPopup.dismiss();
        if (control != null) control.dismiss();   // its speed test stops with it
        if (appOptions != null) { appOptions.dismiss(); appOptions = null; }
        homeCard.pause();
        if (reminder != null) reminder.hide();
    }

    /**
     * A weak TV (Ram.low) with another app in front: the home's photos and its card page let go, so the app in front has that memory (a user report
     * on 4.1: lag). onResume brings them back (a fresh photo at once, the card page through HomeCard.refresh). On other TVs nothing changes.
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level < TRIM_MEMORY_UI_HIDDEN || resumed || !Ram.low(this)) return;
        homeCard.destroy();
        for (ImageView v : new ImageView[] {wallFront, wallBack}) if (v != null) v.setImageDrawable(null);   // both photos (not the fade's end: tilawah_test)
        wallTrimmed = true;
        firstWallpaper = true;   // the short fade when it comes back
    }
    /** the photos were let go (onTrimMemory): the next onResume shows one at once */
    private boolean wallTrimmed;

    @Override
    protected void onDestroy() {
        unregisterReceiver(packages);
        unregisterReceiver(dockChanged);
        unregisterReceiver(cardsChanged);
        unregisterReceiver(sleepChanged);
        unregisterReceiver(nowPlayingChanged);
        unregisterReceiver(reminderDue);
        loader.shutdownNow();
        tvLoader.shutdownNow();
        clockReads.shutdownNow();
        homeCard.destroy();
        super.onDestroy();
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
        // Home pressed while already home: jump back to the first dock app.
        if (inputsPopup != null) inputsPopup.dismiss();
        if (control != null) { control.dismiss(); control.forgetLast(); }
        if (!dockCards.isEmpty()) dockCards.get(0).requestFocus();
        glideTo(0);
    }

    @Override
    public void onBackPressed() {
        // Home has nowhere to go back to; Back from the grid returns to the dock.
        boolean scrolled = scroll.getScrollY() > 0 && !dockCards.isEmpty();
        if (scrolled) dockCards.get(0).requestFocus();
        // opened from its tile while the TV keeps its own home: Back at the top leaves, to the home that opened it
        else if (HomeBack.leaves(isDefaultHome(), false)) finish();
        // «الرجوع يفتح شاشة التوقف» (off by default): nothing to go back to, so the screensaver, behind its own lock as on its pill
        else if (HomeBack.startsSaver(Dock.backSaver(this), true, false, isInstalled(SAVER_PKG))) guarded(ParentalPolicy.PILL_SAVER, SAVER_PKG, "شاشة التوقف", this::startSaver);
    }

    /** Is this app the home the TV goes to on Home? (Not when the stock home is kept and this was opened from its tile.) */
    private boolean isDefaultHome() {
        ResolveInfo ri = getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY);
        return HomeBack.isDefaultHome(ri == null || ri.activityInfo == null ? null : ri.activityInfo.packageName, getPackageName());
    }

    // ---------------------------------------------------------------- layout

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        // The home is built in code, so it inherits no direction from the window: set it here and
        // the pills, the favourites and the app rows all read from right to left.
        root.setLayoutDirection(Dir.layout());

        // Wallpaper: two stacked layers so a new photo can crossfade in over the old one.
        wallBack = wallpaperLayer();
        wallFront = wallpaperLayer();
        root.addView(wallBack, new FrameLayout.LayoutParams(-1, -1));
        root.addView(wallFront, new FrameLayout.LayoutParams(-1, -1));
        // Scrim: darker at the top (clock, pills) and bottom (dock) so text stays readable.
        scrim = new View(this);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xB3000000, 0x40000000, 0x26000000, 0x8C000000, 0xE6000000}));
        root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
        gridShade = new View(this);
        gridShade.setBackgroundColor(0xB3000000);
        gridShade.setAlpha(0f);
        root.addView(gridShade, new FrameLayout.LayoutParams(-1, -1));
        try {
            wallpapers = getAssets().list("wallpapers");
        } catch (java.io.IOException ignored) { }

        glow = new View(this);
        root.addView(glow, new FrameLayout.LayoutParams(-1, -1));
        setGlow(glowColor);

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
        buildEditViews(root);

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
                launch(new Intent(MainActivity.this, MarketsActivity.class));
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
        dockRow = new LinearLayout(this);
        dockRow.setOrientation(LinearLayout.HORIZONTAL);
        dockRow.setGravity(Gravity.CENTER_VERTICAL | Dir.start());
        dockRow.setLayoutDirection(Dir.layout());   // the first favourite on the right in Arabic, on the left in English (tester step 20)
        dockRow.setClipChildren(false);
        dockRow.setClipToPadding(false);
        dockRow.setPadding(bar(FavBar.PAD_SIDE), bar(FavBar.PAD_TOP), bar(FavBar.PAD_SIDE), bar(FavBar.PAD_TOP));
        GradientDrawable band = dockBand = new GradientDrawable();
        band.setCornerRadius(bar(FavBar.RADIUS));
        band.setColor(FavBar.FILL_DARK);
        band.setStroke(Math.max(1, bar(1)), FavBar.LINE_DARK);
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
        tray.setOnScrollChangeListener((v, x, y, ox, oy) -> placeName());
        tray.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus, newFocus) -> tray.wholeOnly());   // the focused tile is the one that may stand partly out
        bottom.addView(tray, new LinearLayout.LayoutParams(-1, bar(FavBar.TRAY_H)));

        // «أكمل المشاهدة»: its own row below the first screen (the owner: the tray never moves for it), ▼ from the tray scrolls the home up
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
        return root;
    }

    /**
     * The WebView's box for that many cards, in pixels. One card is the size it always was. Two or three need room: a pair is
     * 58% of the screen wide (at 1920 that is 560 + 22 + 540), kept clear of the greeting and clock block at the right; the third
     * card goes under the pair. The box ends above the dock, or above the app title when the longest title of the favourites
     * would reach into it. Before the screen is laid out the numbers come from the screen's size alone.
     */
    private int[] homeCardBox(int cards) {
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
        left.addView(greeting, sideLlp(-2, -2, dp(4), 0, 0, 0));
        // under it: the weather of the city and the next sunrise or sunset (shown only when the greeting is on and there is an answer)
        weatherLine = text(12, "regular", 0xFFC6BBA4);
        weatherLine.setMaxLines(2);
        weatherLine.setVisibility(View.GONE);
        left.addView(weatherLine, sideLlp(-2, -2, dp(4), dp(1), 0, 0));
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
        for (TextView t : new TextView[]{greeting, weatherLine, clock, ampm, date, zoneLine}) t.setShadowLayer(dp(3), 0, dp(1), 0x80000000);
        left.setGravity(Dir.start());
        for (TextView t : new TextView[]{greeting, weatherLine, date, zoneLine}) t.setGravity(Dir.start());
        header.addView(left, new FrameLayout.LayoutParams(-2, -2, Dir.start() | Gravity.TOP));
        clockBlock = left;

        LinearLayout right = new LinearLayout(this);
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
        centerPill.setOnClickListener(v -> guarded(ParentalPolicy.PILL_CENTER, null, ControlText.NAME, () -> control.open(v)));
        // ---- end «مركز التحكم»
        // «الأسواق» and «المباريات»: the markets page and the games page, two more round buttons (step 27, the owner's TV photos)
        Pill marketsBtn = pill(S.t("الأسواق"), R.drawable.ic_markets);
        marketsPill = marketsBtn;
        marketsBtn.setOnClickListener(v -> guarded(ParentalPolicy.WIDGET_MATCHES, null, S.t("الأسواق"), () -> launch(new Intent(this, MarketsActivity.class))));   // our own page: no app of the lists
        Pill matchesBtn = pill(S.t("المباريات"), R.drawable.ic_matches);
        matchesPill = matchesBtn;
        matchesBtn.setOnClickListener(v -> launchGuarded(ParentalPolicy.WIDGET_MATCHES, new Intent(this, MatchesActivity.class), "المباريات"));
        List<Pill> row = new ArrayList<>(Arrays.asList(inputs));
        tasksPill = tasks;
        if (isInstalled(SAVER_PKG) && ConfigSnapshot.read(this).tasksPillOn()) row.add(tasks);   // «زر المهام»: hidden in the screensaver's settings, it is not in the row
        if (centerWanted()) row.add(centerPill);
        row.add(marketsBtn);
        row.add(matchesBtn);
        tilawahPill = tilawah;
        pillRow = right;
        // [updates bell] built here, put in the row by applyUpdateBell only while it has a reason to be there
        bellPill = pill(S.t("تحديثات التطبيقات"), R.drawable.ic_bell);
        bellPill.setOnClickListener(v -> guarded(ParentalPolicy.PILL_UPDATES, UpdatesBell.PLAY_STORE, S.t("متجر Play"), this::openUpdates));
        // [/updates bell]
        if (listenPillWanted(ConfigSnapshot.read(this).listenPillOn)) row.add(tilawah);   // «زر تلاوة وأذان»: hidden in the screensaver's settings, it is simply not in the row
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
        nowCard.setOnFocusChangeListener((v, has) -> {
            if (has) { lastFocusedId = v.getId(); glideTo(0); fadeGlow(1f); }
        });
        // taller than a pill by 10 dp, hung 5 dp over the row's top and bottom: the row stays Pill.HEIGHT, so the pills do not move; it
        // reaches from 31 to 75 dp down, clear of the adhan bar (to 26) and the cards (from 84)
        int hang = dp((NowCard.HEIGHT - Pill.HEIGHT) / 2f);
        right.addView(nowCard, 0, sideLlp(-2, dp(NowCard.HEIGHT), 0, -hang, dp(Pill.GAP), -hang));
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
            case "listen":   // the settings' «تلاوة وأذان» ← «افتح تلاوة وأذان»: the same as the button
                if (getResources().getBoolean(R.bool.show_listening))
                    handler.post(() -> launchGuarded(ParentalPolicy.PILL_LISTEN, new Intent(this, sa.urtv.home.tilawah.MainActivity.class), "تلاوة وأذان"));
                break;
            case "center":   // the settings' «مركز التحكم» button: the panel, behind the same lock as its pill; the focus goes back to the pill
                if (control != null && centerPill != null && !Parental.state(this).kids)   // kids mode has no pill, so no panel either
                    handler.post(() -> guarded(ParentalPolicy.PILL_CENTER, null, ControlText.NAME, () -> control.open(centerPill)));
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
        if (t.startsWith("@") && Parental.state(this).kids) return;   // kids mode shows no pills: the phone cannot open one either
        handler.post(() -> {
            switch (t) {
                case "@tasks": launchGuarded(ParentalPolicy.PILL_TASKS, new Intent(this, TasksActivity.class), "المهام"); break;
                case "@saver": guarded(ParentalPolicy.PILL_SAVER, SAVER_PKG, "شاشة التوقف", this::startSaver); break;
                case "@inputs": if (inputsPill != null) showInputs(inputsPill, ParentalPolicy.PILL_INPUTS); break;
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

    private void settingsKeyRequested(Intent i) {
        if (i == null || !i.getBooleanExtra(EXTRA_SETTINGS_KEY, false)) return;
        i.removeExtra(EXTRA_SETTINGS_KEY);
        final Intent tv = TypeService.tvSettingsIntent(this);
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
            pillRow.addView(tasksPill, pillRow.indexOfChild(inputsPill) + 1, sideLlp(-2, dp(Pill.HEIGHT), 0, 0, dp(Pill.GAP), 0));
            pills.add(pills.indexOf(inputsPill) + 1, tasksPill);
        } else {
            boolean hadFocus = tasksPill.hasFocus();
            pills.remove(tasksPill);
            pillRow.removeView(tasksPill);
            if (hadFocus) inputsPill.requestFocus();
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
        boolean want = centerWanted(), has = centerPill.getParent() == pillRow;
        if (want == has) return;
        if (want) {
            int at = quickIndex(marketsPill, matchesPill, tilawahPill);   // before «الأسواق» and «المباريات» (and «تلاوة وأذان»): where it always was
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
            @Override public void openSleep(View anchor) { guarded(ParentalPolicy.PILL_SLEEP, null, S.t("مؤقت النوم"), () -> showSleep(anchor)); }
            @Override public boolean saverInstalled() { return isInstalled(SAVER_PKG); }
            @Override public Typeface font(String family) { return MainActivity.this.font(family); }
        };
    }
    // ---- end «مركز التحكم»

    /** Shows or hides the «يُتلى الآن» card by what «تلاوة» is doing: a surah or a station (playing or paused), else nothing. */
    private void updateNowPlaying() {
        if (nowCard == null) return;
        NowPlaying.State s = NowPlaying.read(this);
        if (!s.show) {
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
            out.add(appOf(ri, pm, given));   // its art only for an app that shows
        }
        return out;
    }

    /** One app's name, art and colour (off the main thread for the whole list; on it for the one app «المخفية» brings back). */
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
        if (banner != null) a.art = banner;
        else {
            a.art = artCache.get("i:" + a.pkg);
            if (a.art == null) artCache.put("i:" + a.pkg, a.art = fitted(ri.loadIcon(pm), false));
        }
        a.color = sampleColor(a.pkg, a.art);
        return a;
    }

    /** On the main thread: the dock first, then the grid a few rows at a time, so a long list never holds up a frame. */
    private void showApps(List<App> all, String[] wanted, int gen) {
        Map<String, App> byPkg = new HashMap<>();
        for (App a : all) byPkg.put(a.pkg, a);
        List<App> dock = new ArrayList<>();
        for (String p : wanted) if (byPkg.containsKey(p)) dock.add(byPkg.remove(p));
        cardByPkg.clear();
        List<App> rest = new ArrayList<>(byPkg.values());   // by name, then in the owner's order (inSavedOrder)
        Collections.sort(rest, (x, y) -> x.label.compareToIgnoreCase(y.label));
        // Fewer than MIN_DOCK of the chosen apps installed: top the dock up from the rest, in the order of their package names, never of their labels: a
        // label follows the screen's language (Downloader in Arabic where English said Apple TV, build 180), the dock must not.
        {
            List<String> have = new ArrayList<>(), other = new ArrayList<>();
            for (App d : dock) have.add(d.pkg);
            for (App x : rest) other.add(x.pkg);
            for (String p : AppsEdit.topUp(have, other, MIN_DOCK)) for (App x : new ArrayList<>(rest)) if (x.pkg.equals(p)) { rest.remove(x); dock.add(x); }   // AppsEdit.topUp: by package, not by label
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
        applyTrayHeight();
        appsLabel.setVisibility(rest.isEmpty() ? View.GONE : View.VISIBLE);
        wireFocus();
        if (!dock.isEmpty()) { title.setText(dock.get(0).label); setGlow(dock.get(0).color); }
        View last = lastFocusedId == View.NO_ID ? null : findViewById(lastFocusedId);
        if (last != null) last.requestFocus();
        else if (!dockCards.isEmpty()) dockCards.get(0).requestFocus();
        appsShown = true;
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
    private LinearLayout.LayoutParams dockCell(int i, int n) { return dockCell(i, n, n); }

    /** cell {@code i} of {@code n} in a row whose tiles are as wide as the tray's when {@code favs} favourites are shown (the watch-next rows share the tray's cells) */
    private LinearLayout.LayoutParams dockCell(int i, int n, int favs) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(FavBar.tileW(getResources().getDisplayMetrics().heightPixels, FavBar.shown(barSetting, favs), i), -2);
        p.setMarginStart(i == 0 ? 0 : bar(FavBar.GAP) / 2);
        p.setMarginEnd(i == n - 1 ? 0 : bar(FavBar.GAP) - bar(FavBar.GAP) / 2);
        return p;
    }

    /** «تطبيقات في الشريط» (the screensaver's setting, read with the rest): how many tiles the tray shows whole; the count of favourites matters for "all" */
    private String barSetting = "5";

    private int barShown() { return FavBar.shown(barSetting, dockCards.size()); }

    private int lift() { return FavBar.lift(getResources().getDisplayMetrics().heightPixels, barShown()); }

    /** the tray is as high as its tiles with the same 24 above and below them (FavBar.trayH): its bottom stays where it was, its top follows */
    private void applyTrayHeight() {
        if (tray == null) return;
        int h = FavBar.trayH(getResources().getDisplayMetrics().heightPixels, barShown());
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

        FrameLayout card = new AspectFrame(this);
        card.setId(View.generateViewId());
        card.setFocusable(true);
        card.setClickable(true);
        card.setTag(app);
        card.setClipToOutline(true);
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), inDock ? bar(FavBar.TILE_RADIUS) : dp(10));
            }
        });
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(app.isBanner ? 0xFF1C1C1E : darken(app.color, 0.45f));
        card.setBackground(bg);

        ImageView img = new ImageView(this);
        img.setImageDrawable(app.art);
        if (app.isBanner) {
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(img, new FrameLayout.LayoutParams(-1, -1));
        } else {
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            card.addView(img, new FrameLayout.LayoutParams(-1, -1));
            img.setPadding(dp(22), dp(14), dp(22), dp(14));
        }

        // Focus ring and sheen, drawn above the art.
        GradientDrawable ring = new GradientDrawable();
        ring.setCornerRadius(inDock ? bar(FavBar.TILE_RADIUS) : dp(10));
        ring.setStroke(inDock ? bar(FavBar.RING) : dp(2), inDock ? FavBar.ring(lightNow) : 0xE6FFFFFF);
        ring.setOrientation(GradientDrawable.Orientation.TL_BR);
        ring.setColors(new int[]{0x26FFFFFF, 0x00FFFFFF, 0x00FFFFFF});
        View shine = new View(this);
        shine.setBackground(ring);
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

        card.setOnClickListener(v -> guarded(inDock ? ParentalPolicy.TILE : ParentalPolicy.GRID, app.pkg, app.label, () -> launch(new Intent(Intent.ACTION_MAIN)
                .addCategory(app.category)
                .setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))));
        card.setOnFocusChangeListener((v, has) -> {
            float s = has ? (inDock ? FavBar.FOCUS_SCALE : 1.1f) : 1f;
            // from the games of the football home: the tray goes back to its resting place (y 768) first, then the tile lifts (FavBar.DROP_MS)
            long wait = inDock && has && trayLowered ? FavBar.DROP_MS : 0;
            if (inDock && has) lowerTray(false);
            if (wait > 0) v.postDelayed(() -> { if (v.hasFocus()) scaleTo(v, s); }, wait); else scaleTo(v, s);   // a spring: a held arrow keeps its speed from card to card
            if (inDock) v.animate().translationY(has ? -lift() : 0).translationZ(has ? dp(GlowLook.LIFT_DP) : 0)
                    .setInterpolator(new DecelerateInterpolator()).setStartDelay(wait).setDuration(FavBar.MS).start();
            else v.animate().translationZ(has ? dp(GlowLook.LIFT_DP) : 0).setInterpolator(new DecelerateInterpolator()).setDuration(has ? 320 : 200).start();
            shine.animate().alpha(has ? 1f : 0f).setDuration(160).start();
            label.animate().alpha(has ? 1f : 0f).translationY(has ? dp(4) : 0).setDuration(200).start();
            if (!has) return;
            lastFocusedId = v.getId();
            optionsHint(inDock);
            if (inDock) {
                nameCard = v;
                title.setText(app.label);
                placeName();
                title.animate().alpha(1f).setDuration(200).start();
                animateGlow(app.color);
                fadeGlow(1f);
                glideTo(0);
            } else {
                fadeGlow(0.35f);
                title.animate().alpha(0f).setDuration(200).start();
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
    private int lastPillId = View.NO_ID;   // the pill that had the focus last: ▲ from a home card goes back to it

    private void wireFocus() {
        footballHeader();   // the home card was just made again (or not): «الواجهة» may have changed with it
        int d = dockCards.size(), g = gridCards.size();
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
                v.setNextFocusUpId(pills.get(0).getId());
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
        int topId = card != null ? card.getId() : pills.get(0).getId();
        if (fMt != null && fPr != null && fWx != null) topId = fMt.getId();   // the football home: ▲ from the tray is the games
        if (nowCard != null) {   // the «يُتلى الآن» card: down to the cards or the dock, left and up to the pills (right: the end of the row)
            nowCard.setNextFocusLeftId(pills.get(0).getId());
            nowCard.setNextFocusUpId(pills.get(0).getId());
            nowCard.setNextFocusRightId(nowCard.getId());
            nowCard.setNextFocusDownId(card != null ? card.getId() : dockId != View.NO_ID ? dockId : nowCard.getId());
        }
        for (int i = 0; i < targets.size(); i++) {
            View t = targets.get(i);
            // the next card in reading order with the arrow that goes on (left in Arabic), the one before with the other; at an end: stay on it
            View next = i + 1 < targets.size() ? targets.get(i + 1) : t, prev = i > 0 ? targets.get(i - 1) : t;
            if (Dir.rtl()) { t.setNextFocusLeftId(next.getId()); t.setNextFocusRightId(prev.getId()); }
            else { t.setNextFocusRightId(next.getId()); t.setNextFocusLeftId(prev.getId()); }
            t.setNextFocusUpId(pills.get(0).getId());   // a pill's own focus moves this to that pill, see pill()
            t.setOnKeyListener((v, keyCode, ev) -> {   // ▲ from a home card is a pill that is on screen: the pill it last came from may be gone (the bell leaves the row), and the focus then stayed on the card (step 28b)
                if (keyCode != KeyEvent.KEYCODE_DPAD_UP) return false;
                if (ev.getAction() != KeyEvent.ACTION_DOWN) return true;
                View to = null;
                for (View pv : pills) if (pv.isShown() && pv.isFocusable()) { if (to == null) to = pv; if (pv.getId() == lastPillId) { to = pv; break; } }
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
        for (int r = 0; r < tvCards.size(); r++) {   // «أكمل المشاهدة»: a row up to the one above (the favourites), down to the next (the apps)
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

    // ---------------------------------------------------------------- «أكمل المشاهدة»
    private LinearLayout tvRowsBox;
    private final List<List<View>> tvCards = new ArrayList<>();   // the row's cards, in order (the focus wiring; one row at most)
    private int tvRowsGen;   // a load that finishes after a newer one started is dropped
    private final List<TextView> tvNames = new ArrayList<>();   // «المظهر» recolours the row's name

    // their own thread: a slow provider or picture never holds up the apps' loader (the dock, the grid)
    private final java.util.concurrent.ExecutorService tvLoader = java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final long TV_PICTURES_MS = 8000;   // the pictures of one visit; what is not there by then keeps its title
    private String tvShownSig = "";
    private List<TvRows.Row> tvShown = new ArrayList<>();
    private int tvCells;   // the cells «أكمل المشاهدة» was cut into (the favourites' count then): built again when that changes
    private final java.util.Map<TvRows.Item, ImageView> tvPics = new java.util.HashMap<>();
    private final java.util.Map<TvRows.Item, TextView> tvPlain = new java.util.HashMap<>();
    private final java.util.Set<TvRows.Item> tvPictured = new java.util.HashSet<>();

    /** The rows' words first (quick), shown; then their pictures as they come, within TV_PICTURES_MS. */
    private void loadTvRows() {
        if (tvRowsBox == null) return;
        if (!TvRows.allowed(this) || !ConfigSnapshot.read(this).watchNextOn) {   // not allowed, or turned off in the settings: no row
            if (tvRowsBox.getChildCount() > 0) { tvShownSig = ""; showTvRows(new ArrayList<>()); }
            return;
        }
        final int gen = ++tvRowsGen;
        if (tvLoader.isShutdown()) return;
        tvLoader.execute(() -> {
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
     * «أكمل المشاهدة»: its name, then as many cards as there are favourites, each a favourite's width right under one (MainActivity.rowCell,
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
        card.setOnClickListener(v -> guarded(ParentalPolicy.GRID, it.pkg, it.title, () -> TvRows.open(this, it)));   // the app's own lock first
        card.setOnFocusChangeListener((v, has) -> {
            scaleTo(v, has ? 1.12f : 1f);   // as a favourite
            v.animate().translationZ(has ? dp(GlowLook.LIFT_DP) : 0).setInterpolator(new DecelerateInterpolator()).setDuration(has ? 320 : 200).start();
            shine.animate().alpha(has ? 1f : 0f).setDuration(160).start();
            if (!has) return;
            lastFocusedId = v.getId();
            fadeGlow(1f);
            View line = (View) box.getParent();
            int at = tvRowsBox.indexOfChild(line) / 2;   // the box holds each row's name, then its cards
            if (at >= 0 && at < tvNames.size()) tvNames.get(at).setText(S.tv(TvRowsPick.WATCH_NEXT_TITLE) + "  ·  " + (it.meta.isEmpty() ? it.title : it.title + "  ·  " + it.meta));
            scrollToTvRow(line);   // below the first screen: the home slides up to the row; ▲ to the tray slides it back (glideTo(0))
        });
        return box;
    }

    /** «أكمل المشاهدة» below the first screen: the row (its name over it) a little above the screen's middle. */
    private void scrollToTvRow(View line) {
        int y = tvRowsBox.getTop() + line.getTop();
        int max = Math.max(0, content.getHeight() - scroll.getHeight());
        glideTo(Math.max(0, Math.min(y - (int) (getResources().getDisplayMetrics().heightPixels * 0.42f), max)));
    }

    private void scrollToRow(View tileBox) {
        View row = (View) tileBox.getParent();
        int y = grid.getTop() + row.getTop();
        int screenH = getResources().getDisplayMetrics().heightPixels;
        // First grid row: slide the dock band up to the top edge; later rows centre on screen.
        int target = grid.indexOfChild(row) == 0 && tvCards.isEmpty()
                ? hero.getTop() + bottomBlock.getTop() + tray.getTop()   // the tray at the top edge
                : y - (int) (screenH * 0.42f);
        int max = Math.max(0, content.getHeight() - scroll.getHeight());
        glideTo(Math.max(0, Math.min(target, max)));
    }

    // ---------------------------------------------------------------- inputs

    // ---------------------------------------------------------------- dock editing

    private final Map<String, View> cardByPkg = new HashMap<>();
    static final int MIN_DOCK = Dock.MIN, MAX_DOCK = Dock.MAX;

    /**
     * After «تحديث تلفزيوني» replaced this app, the first start finds the «update in progress» record: the update screen opens once and says how it
     * went (it removes the record). Nothing happens without a record, so a normal start costs one missing-file check.
     */
    private void showUpdateResult() {
        try {
            if (sa.urtv.home.phone.PhoneUpdater.isBusy()) return;   // an update running in this very process: its own screen says how it goes
            if (sa.urtv.home.phone.PhoneUpdater.confirmKnown()) return;   // an install confirm is still pending: the record stays unread until it is answered
            long mine = sa.urtv.home.phone.PhoneUpdater.installedCode(this, getPackageName());
            if (sa.urtv.home.phone.UpdateRecords.peek(new sa.urtv.home.phone.UpdateStore(this), mine).kind != sa.urtv.home.phone.UpdateRecords.Kind.NONE) {
                launchGuarded(ParentalPolicy.UPDATE_SCREEN, new Intent(this, UpdateActivity.class), S.t("تحديث تلفزيوني"));   // the result of an update: behind «قفل الإعدادات» like the rest of the screen
            }
        } catch (RuntimeException ignored) { }
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
            x.animate().scaleX(has ? 1.3f : 1f).scaleY(has ? 1.3f : 1f).setDuration(120).start();
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
            Dock.save(this, row);   // the same store as «ترتيب المفضلة» and dock.sh; its broadcast builds the row from it
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

    /** One key while the mode is on. True: it is ours (every key but the volume and power ones). */
    private boolean editKey(KeyEvent e) {
        int k = e.getKeyCode();
        if (AdhanKeys.reserved(k)) return false;
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
        wireFocus();
    }

    /** Picked up: bigger, a gold outline and a little off the row. Dropped: as any tile. */
    private void liftCard(View card, boolean lifted) { liftCard(card, lifted, FavBar.FOCUS_SCALE); }

    private void liftCard(View card, boolean lifted, float focusScale) {
        GradientDrawable ring = ringByCard.get(card);
        View shine = shineByCard.get(card);
        boolean fav = dockCards.contains(card);
        if (ring != null) ring.setStroke(lifted ? dp(4) : fav ? bar(FavBar.RING) : dp(2), lifted ? GOLD : fav ? FavBar.ring(lightNow) : 0xE6FFFFFF);
        if (shine != null && lifted) shine.setAlpha(1f);
        if (lifted) {
            scaleTo(card, fav ? FavBar.LIFTED_SCALE : 1.25f);   // a favourite stays inside its tray
            card.animate().translationY(fav ? -lift() : -dp(10)).translationZ(dp(GlowLook.LIFT_DP)).setDuration(160).start();
        } else {
            boolean has = card.isFocused();
            float s = has ? focusScale : 1f;
            scaleTo(card, s);
            card.animate().translationY(has && fav ? -lift() : 0).translationZ(has ? dp(GlowLook.LIFT_DP) : 0).setDuration(160).start();
            if (shine != null) shine.setAlpha(has ? 1f : 0f);
        }
    }

    // ---------------------------------------------------------------- «تعديل التطبيقات»

    /** The settings' «ترتيب كل التطبيقات» asks the home screen for the mode, or for the order by name again, with these extras. */
    static final String EXTRA_EDIT_APPS = "edit_apps", EXTRA_RESET_APPS = "reset_apps_order";

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
        beginPendingAppsEdit();
        return true;
    }

    /** «تعديل التطبيقات» from the long-press menu (on the app that was pressed) or from the settings (pkg null). The family lock asks first. */
    private void beginAppsEdit(String pkg) {
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
        if (order.isEmpty() && hiddenFound.isEmpty()) {
            Toast.makeText(this, S.t("كل تطبيقاتك في المفضلة"), Toast.LENGTH_SHORT).show();
            return;
        }
        int focus = pkg != null ? order.indexOf(pkg) : -1;
        if (focus < 0) {
            View f = getCurrentFocus();
            for (int i = 0; i < gridCards.size(); i++) if (gridCards.get(i) == f) focus = i;
        }
        appsEdit = new AppsEdit(order, new ArrayList<>(hiddenFound.keySet()), cols, dockCards.size(), MAX_DOCK, Math.max(0, focus));
        editHint.setText(S.tv(AppsEdit.HINT));
        paintEditChrome();
        editHint.setVisibility(View.VISIBLE);
        paintAppsEdit();
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
        if (f != null && gridCards.contains(f)) f.requestFocus();
        else if (!dockCards.isEmpty()) dockCards.get(0).requestFocus();
    }

    /** One key while the mode is on. True: it is ours (every key but the volume and power ones), as in «تعديل المفضلة». */
    private boolean appsEditKey(KeyEvent e) {
        int k = e.getKeyCode();
        if (AdhanKeys.reserved(k)) return false;
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
        dockRow.addView(tile(app, true));   // tile() puts it in dockCards and cardByPkg
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
        for (String p : dock) if (isInstalled(p)) installed.add(p);
        prefs().edit().putString("dock", android.text.TextUtils.join(",", installed)).apply();
        dockApps = installed.toArray(new String[0]);
        focusPkgAfter = focusPkg;
        buildApps();
    }

    private void showAppMenu(View anchor, String pkg, boolean inDock) {
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
                toast(S.t("أُخفي، ويرجع من «تعديل التطبيقات»"));
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
        if (!kids && AppActions.canUninstall(this, pkg)) {   // «إلغاء التثبيت»: Android's own question (never for a system app, ours, or in kids mode); the family PIN first when it asks
            Intent del = new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", pkg, null)).putExtra(Intent.EXTRA_RETURN_RESULT, false);
            o.uninstall = () -> guarded(ParentalPolicy.MENU_APP, pkg, S.t("إلغاء التثبيت"), () -> launch(del));
        }
        Intent shop = AppActions.storeIntent(this, pkg);
        if (!kids && shop != null) o.store = () -> guarded(ParentalPolicy.MENU_APP, pkg, S.t("صفحته في المتجر"), () -> launch(shop));
        if (appOptions != null) appOptions.dismiss();
        appOptions = AppOptions.show(this, anchor, inDock ? 1.12f : 1.1f, homeGlass(), font("sans-serif-medium"), o);
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
        optionsHint.animate().alpha(on ? 1f : 0f).setDuration(200).start();
    }

    /** the box was opened: the hint has done its job */
    private void optionsHintUsed() {
        prefs().edit().putInt("options_hint", OPTIONS_HINT_TIMES).putInt("options_hint_en", OPTIONS_HINT_TIMES).apply();   // the box was opened, in either language: the person knows
        if (optionsHint != null) optionsHint.animate().alpha(0f).setDuration(150).start();
    }

    // ---- the favourites bar (FavBar): its tray, the name over the focused tile, and the one move it may make

    private TraySnap tray;
    private View nameRow, nameCard;
    private boolean trayLowered;

    /** The name (and the hint beside it) centred over the focused favourite, as it scrolls too; hidden when the focus is not in the bar. */
    private void placeName() {
        if (nameRow == null || nameRow.getWidth() == 0) return;
        View f = getCurrentFocus();
        boolean inBar = f != null && dockCards.contains(f) && edit == null;
        nameRow.animate().alpha(inBar ? 1f : 0f).setDuration(FavBar.MS).start();
        if (!inBar || nameCard == null) return;
        nameRow.setTranslationX(0);   // the name has ONE place: above the tray at its start edge (right in Arabic, left in English), never following a tile (build 180)
    }

    /**
     * The one move the tray makes (FavBar.WIDGET_DROP): 40 px down while the focus is in a widget that opens into a scrolling row (the
     * matches widget of the football home, docs/PROPOSALS-NOV2.md §1, FootballFocus), back before a tile lifts. The tray looks the same
     * either way.
     */
    void lowerTray(boolean down) {
        if (tray == null || down == trayLowered) return;
        trayLowered = down;
        tray.animate().translationY(down ? bar(FavBar.WIDGET_DROP) : 0).setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                .setDuration(FavBar.DROP_MS).start();
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
            rows.add(inputRow(InputLabels.rowLabel(this, t, given), () -> openPassthroughInput(t)));   // + the CEC device's name and its standby mark
        }
        if (liveTvPkg != null) rows.add(inputRow(S.t("البث التلفزيوني"), () -> guarded(ParentalPolicy.INPUT_LIVE_TV, TvVendor.pkg(liveTvPkg), "البث التلفزيوني", () -> openPackage(liveTvPkg))));
        if (inputs.isEmpty() && inputPickerPkg != null) rows.add(inputRow(S.t("كل المصادر"), () -> guarded(ParentalPolicy.INPUT_PICKER, TvVendor.pkg(inputPickerPkg), "كل المصادر", () -> openPackage(inputPickerPkg))));

        if (rows.isEmpty()) rows.add(inputRow(S.t("الإعدادات"), () -> launchGuarded(ParentalPolicy.PILL_TV_SETTINGS, new Intent(Settings.ACTION_SETTINGS), "إعدادات التلفزيون")));
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
        if (!SleepService.enabled()) {   // without it the end only stops the sound and brings the screensaver: say so, and where to turn it on
            rows.add(inputRow(S.t("لإطفاء الشاشة: فعّل «%s»", getString(R.string.sleep_service_label)), () -> launchGuarded(ParentalPolicy.SLEEP_ACCESSIBILITY, new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), "إعدادات التلفزيون")));
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

    private void applyLook(boolean light) {
        GlassSource.setHomeLight(this, light);   // «تلقائي» on the other screens follows the home (kept only when it changes)
        if (light == lightNow && lookNow.equals(paletteLook)) return;
        lightNow = light;
        paletteLook = lookNow;
        Look.Palette p = palette();
        clock.setTextColor(p.text);
        title.setTextColor(p.ink);
        title.setShadowLayer(dp(18), 0, 0, p.titleShadow);
        for (TextView t : new TextView[]{weatherLine, date}) t.setTextColor(p.soft);
        for (TextView t : new TextView[]{greeting, ampm, zoneLine, appsLabel}) t.setTextColor(p.gold);
        for (TextView t : new TextView[]{greeting, weatherLine, clock, ampm, date, zoneLine}) t.setShadowLayer(dp(3), 0, dp(1), p.shadow);
        for (TextView t : gridLabels) t.setTextColor(p.ink);
        for (TextView t : tvNames) { t.setTextColor(p.gold); t.setShadowLayer(dp(3), 0, dp(1), p.shadow); }
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, p.scrim));
        gridShade.setBackgroundColor(p.shade);
        dockBand.setColor(light ? FavBar.FILL_LIGHT : FavBar.FILL_DARK);   // FavBar: the same tray on every look
        dockBand.setStroke(Math.max(1, bar(1)), light ? FavBar.LINE_LIGHT : FavBar.LINE_DARK);
        for (View c : dockCards) { GradientDrawable r = ringByCard.get(c); if (r != null) r.setStroke(bar(FavBar.RING), FavBar.ring(light)); }
        if (homeCard != null) homeCard.light(light);   // the football home's widgets: the same light or dark glass
    }

    /** The football home: the weather has its own card and the clock sits higher, so the greeting and its weather line are not shown. */
    private void footballHeader() {
        if (greeting == null) return;
        boolean football = homeCard != null && homeCard.isFootball();
        greeting.setVisibility(football ? View.GONE : View.VISIBLE);
        if (football) weatherLine.setVisibility(View.GONE);
    }

    private void updateClock() {
        long ms = System.currentTimeMillis();
        clockDrawnAt = ms;
        String tvTz = java.util.TimeZone.getDefault().getID();
        // one read of the screensaver's settings (its ConfigProvider, same signing key) for the whole tick; null zone = it is missing: the TV's own
        ConfigSnapshot cfg = ConfigSnapshot.read(this);
        Digits.remember(this, cfg);   // a change of the setting reaches the next tick
        if (langBuilt != null && !langBuilt.equals(S.lang())) {   // «اللغة» changed while this screen was up (tester step 20: the words, the
            langBuilt = S.lang();                                 // favourites' direction and the names stayed as built): built again in it
            recreate();
            return;
        }
        String[] zone = cfg.cityZone();
        String tz = zone == null ? tvTz : zone[1];
        Calendar now = ClockZone.at(ms, tz);   // the city's time, like the prayer times beside it
        int h = now.get(Calendar.HOUR_OF_DAY);
        greetingLine.update(h, cfg);
        footballHeader();
        lookNow = cfg.look();
        applyLook(Look.light(cfg.appearance(), dayBySun(cfg), h * 60 + now.get(Calendar.MINUTE)));
        GlassStyle.Look glassNow = homeGlass();
        for (Pill p : new Pill[] {inputsPill, tasksPill, tilawahPill, centerPill, marketsPill, matchesPill, bellPill}) if (p != null) p.look(glassNow);   // «رملي»: its own buttons
        for (View v : pills) if (v instanceof Pill) ((Pill) v).look(glassNow);   // and every pill of the row, whatever else is added to it: six buttons, one style (step 28: two stayed grey circles)
        boolean h24 = cfg.clock24();   // «ساعة ٢٤»: off (12-hour with the ص or م) unless the owner turned it on
        clock.setText(ClockText.face(h, now.get(Calendar.MINUTE), h24, Digits.latin()));
        ampm.setText(ClockText.suffix(h, h24));
        ampm.setVisibility(h24 ? View.GONE : View.VISIBLE);
        String cal = zone == null ? "" : ClockZone.calendar(zone[1], zone[2]);
        boolean other = zone != null && ClockZone.showLine(ClockZone.differs(tvTz, tz, ms), cal);
        zoneLine.setVisibility(other ? View.VISIBLE : View.GONE);
        if (other) zoneLine.setText(ClockZone.label(zone[0], cal));
        if (gregFormat == null) {
            gregFormat = new SimpleDateFormat("d MMMM", new Locale("ar"));
            try {
                hijriFormat = new android.icu.text.SimpleDateFormat("EEEE، d MMMM y", new android.icu.util.ULocale("ar_SA@calendar=islamic-umalqura"));
                hijriDay = new android.icu.text.SimpleDateFormat("d MMMM y", new android.icu.util.ULocale("ar_SA@calendar=islamic-umalqura"));
            } catch (Exception ignored) { }
        }
        if (!tz.equals(formatZone)) {
            gregFormat.setTimeZone(java.util.TimeZone.getTimeZone(tz));
            if (hijriFormat != null) hijriFormat.setTimeZone(android.icu.util.TimeZone.getTimeZone(tz));
            if (hijriDay != null) hijriDay.setTimeZone(android.icu.util.TimeZone.getTimeZone(tz));
            formatZone = tz;
        }
        String hijri = "";
        try {
            int shift = HijriShift.fromFragment(cfg.fragment);   // «فرق التاريخ الهجري»: 0 unless the settings say so
            if (hijriFormat != null && shift == 0) hijri = hijriFormat.format(now.getTime()).replaceAll("\\s*هـ\\s*$", "") + " هـ";
            else if (hijriFormat != null) {   // the day, month and year move by the offset; the weekday is today's
                if (hijriWeekday == null) hijriWeekday = new android.icu.text.SimpleDateFormat("EEEE", new android.icu.util.ULocale("ar_SA@calendar=islamic-umalqura"));
                hijriWeekday.setTimeZone(android.icu.util.TimeZone.getTimeZone(tz));
                hijri = hijriWeekday.format(now.getTime()) + "، " + hijriDay.format(HijriShift.date(now, shift)).replaceAll("\\s*هـ\\s*$", "") + " هـ";
            }
        } catch (Exception ignored) { }
        String greg = MonthNames.of(gregFormat.format(now.getTime()), MonthNames.levant(cfg.fragment));   // «تشرين الأول» when chosen
        date.setText(arabicDigits(hijri.isEmpty() ? greg : hijri + "  –  " + greg));
        if (S.en()) date.setText(arabicDigits(englishDate(now, tz, HijriShift.fromFragment(cfg.fragment))));   // the same line in English (MonthNames.hijriEn)
        updateAdhanBar(cfg);
        applyListenPill(cfg.listenPillOn);
        applyTasksPill(cfg.tasksPillOn());
        Dock.followBackSaver(this, cfg.backSaver());   // «الرجوع يفتح شاشة التوقف» changed in the screensaver's settings: read at the next Back
        applyUpdateBell(cfg.updateBellOn);   // [updates bell]
        applyBarSize(cfg.favBar);   // «تطبيقات في الشريط»
        hiddenQuick.clear();
        for (String k : new String[] {"markets", "matches", "center"}) if (cfg.pillHidden(k)) hiddenQuick.add(k);
        applyQuickPill(marketsPill, !hiddenQuick.contains("markets"), matchesPill, tilawahPill);
        applyQuickPill(matchesPill, !hiddenQuick.contains("matches"), tilawahPill);
        applyCenterPill();   // kids mode may have been turned on or off, or the button hidden
        sa.urtv.home.tilawah.AdhanSync.onConfig(this, cfg.placeFragment(), cfg.placeTicket());   // «تعديل وقت الصلاة» changed on the TV: the alarm follows
        updateSleepPill();
    }

    /** Once a minute with the clock: the bar before the adhan shows in the last 10 minutes, and until the iqama. */
    private void updateAdhanBar(ConfigSnapshot cfg) {
        String words = AdhanBar.text(this, cfg);
        adhanBar.setVisibility(words == null ? View.GONE : View.VISIBLE);
        if (words != null) adhanBar.setText(words);
        // any key stops the adhan (AdhanKeys) and the focus stays where it was; with «إيقاف الأذان بأي زر» off the bar is focusable (UP reaches it) and OK on it stops it
        adhanBar.setFocusable(words != null && AdhanBar.playing(cfg));
    }

    private static String arabicDigits(String s) { return Digits.of(s); }   // the setting's digits (hindi or latin)

    /**
     * The date line in English, as the English page writes it: «Monday, 22 Jumada al-Awwal 1448 AH  –  2 November». The Hijri day comes
     * from the TV's Umm al-Qura calendar, moved by «فرق التاريخ الهجري» as the Arabic line; without that calendar, the Gregorian day alone.
     */
    private static String englishDate(Calendar now, String tz, int shift) {
        java.util.TimeZone zone = java.util.TimeZone.getTimeZone(tz);
        SimpleDateFormat greg = new SimpleDateFormat("d MMMM", Locale.ENGLISH), day = new SimpleDateFormat("EEEE", Locale.ENGLISH);
        greg.setTimeZone(zone);
        day.setTimeZone(zone);
        String g = greg.format(now.getTime());
        try {
            android.icu.util.Calendar h = android.icu.util.Calendar.getInstance(android.icu.util.TimeZone.getTimeZone(tz),
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

    private void setGlow(int color) {
        glowColor = color;
        if (glowDrawable == null) {
            glowDrawable = new GradientDrawable();
            glowDrawable.setGradientType(GradientDrawable.RADIAL_GRADIENT);
            glowDrawable.setGradientCenter(0.5f, 0.78f);
            glowDrawable.setGradientRadius(getResources().getDisplayMetrics().widthPixels * GlowLook.RADIUS);
            glow.setBackground(glowDrawable);
        }
        glowDrawable.setColors(new int[]{withAlpha(color, GlowLook.CENTRE_ALPHA), withAlpha(color, GlowLook.MID_ALPHA), Color.TRANSPARENT});
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
        if (glowAlphaTo != alpha) { glowAlphaTo = alpha; glow.animate().alpha(alpha).setDuration(300).start(); }
        // The grid sits over the photo's busiest middle part, so dim it while browsing apps.
        float shade = alpha < 1f ? 1f : 0f;
        if (shadeTo != shade) { shadeTo = shade; gridShade.animate().alpha(shade).setDuration(300).start(); }
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
        try (Cursor c = getContentResolver().query(OWN, null, null, null, null)) {
            // an older screensaver app answers any path with its settings instead: no "n" column, no photos
            if (c != null && c.getColumnIndex("n") == 0 && c.moveToFirst()) return new Object[] {c.getInt(0), c.getString(1), c.getLong(2)};
        } catch (RuntimeException ignored) { }
        return new Object[] {0, "mix", 0L};
    }

    /** Off the main thread: when the owner's photos changed since the wallpaper was last chosen, show one now. */
    private void checkOwnChanged() {
        new Thread(() -> {
            long rev = (Long) ownInfo()[2];
            if (rev != prefs().getLong("ownRev", 0)) runOnUiThread(() -> { if (!isDestroyed() && resumed) nextWallpaper(); });
        }).start();
    }

    /** Decode the next photo off the main thread, then crossfade it in over the current one. */
    private void nextWallpaper() {
        handler.removeCallbacks(rotateWallpaper);
        if (wallpapers.length == 0) return;
        final Object[] own = ownInfo();
        final int ownCount = (Integer) own[0];
        final boolean only = ownCount > 0 && "only".equals(own[1]);
        prefs().edit().putLong("ownRev", (Long) own[2]).apply();
        final String[] remote = only ? new String[0] : remotePhotos();
        final int base = only ? 0 : wallpapers.length + remote.length;   // ours come first, then the owner's
        final int total = only ? ownCount : base + ownCount;
        if (wallIndex < 0) wallIndex = prefs().getInt("wallpaper", -1);
        wallIndex = (wallIndex + 1) % total;
        prefs().edit().putInt("wallpaper", wallIndex).apply();
        wallChangedAt = SystemClock.elapsedRealtime();
        final int idx = wallIndex;
        final String fallback = "wallpapers/" + wallpapers[idx % wallpapers.length];
        final int gen = ++wallGen;   // a photo that arrives after a newer request is not shown
        new Thread(() -> {
            // 1. what is on the device now, at once: the owner's photo, the hosted one already kept, else the bundled one
            Bitmap bm = null;
            java.io.File f = null;
            if (idx >= base) {
                final Uri photo = OWN.buildUpon().appendPath(String.valueOf(idx - base + 1)).build();
                try {
                    bm = decode(() -> getContentResolver().openInputStream(photo));
                } catch (java.io.IOException | RuntimeException ignored) { }
            } else if (idx >= wallpapers.length) {
                f = new java.io.File(getFilesDir(), "photos/" + remote[idx - wallpapers.length]);
                if (f.isFile()) {
                    final java.io.File kept = f;
                    try { bm = decode(() -> new java.io.FileInputStream(kept)); } catch (java.io.IOException | RuntimeException ignored) { }
                }
            }
            final boolean needsDownload = f != null && bm == null;
            String key = bm == null ? null : idx >= base ? "own:" + (idx - base + 1) + ":" + own[2] : f != null ? "photo:" + f.getName() : null;
            if (bm == null) {
                try { bm = decode(() -> getAssets().open(fallback)); key = "bundled:" + fallback; }
                catch (java.io.IOException ignored) { }
            }
            showWallpaperIfCurrent(bm, gen);
            if (gen == wallGen) GlassSource.update(this, bm, key);   // the glass look's backdrop: made again only for another photo
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
                    try (java.io.FileOutputStream out = new java.io.FileOutputStream(tmp)) { out.write(data); }
                    if (!tmp.renameTo(f)) tmp.delete();
                }
                final java.io.File got = f;
                Bitmap fresh = got.isFile() ? decode(() -> new java.io.FileInputStream(got)) : null;
                showWallpaperIfCurrent(fresh, gen);
                if (gen == wallGen) GlassSource.update(this, fresh, "photo:" + got.getName());
            } catch (java.io.IOException | RuntimeException ignored) { }
        }).start();
        scheduleWallpaper(WALLPAPER_INTERVAL_MS);
    }

    private int wallGen;   // counts the requests for a wallpaper (main thread)

    /** From the loading thread: shown only if no newer request has been made since this one started. */
    private void showWallpaperIfCurrent(Bitmap bm, int gen) {
        if (bm != null) runOnUiThread(() -> { if (gen == wallGen) showWallpaper(bm); });
    }

    private void showWallpaper(Bitmap bm) {
        if (bm == null || isDestroyed()) return;
        // Swap roles: the old front becomes the back, the new photo fades in on top.
        final ImageView old = wallFront;
        wallFront = wallBack;
        wallBack = old;
        ViewGroup root = (ViewGroup) wallFront.getParent();
        root.removeView(wallFront);
        root.addView(wallFront, 1, new FrameLayout.LayoutParams(-1, -1));
        wallFront.setImageBitmap(bm);
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
        String fallback = inputPickerPkg != null ? inputPickerPkg : liveTvPkg;
        if (fallback != null) {
            guarded(ParentalPolicy.INPUT_PICKER, TvVendor.pkg(fallback), "كل المصادر", () -> openPackage(fallback));
        } else {
            launchGuarded(ParentalPolicy.PILL_TV_SETTINGS, new Intent(Settings.ACTION_SETTINGS), "إعدادات التلفزيون");
        }
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
    private void openOurSettings(boolean orTvSettings) {
        Intent i = new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(SAVER_PKG, SAVER_PKG + ".SettingsActivity"));
        if (getPackageManager().resolveActivity(i, 0) != null) {
            guarded(ParentalPolicy.OUR_SETTINGS, null, S.t("إعدادات تلفزيونك"), () -> launch(i));
        } else if (orTvSettings) {
            launchGuarded(ParentalPolicy.REMOTE_SETTINGS, new Intent(Settings.ACTION_SETTINGS), "إعدادات التلفزيون");
        }
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

    private static int darken(int c, float f) {
        return Color.rgb((int) (Color.red(c) * f), (int) (Color.green(c) * f), (int) (Color.blue(c) * f));
    }

    /** The adhan's rule for a key; the PIN keypad (a dialog, with a key path of its own) asks it first too, so nothing in the family lock can hold the adhan. */
    private boolean adhanTakes(KeyEvent e) {
        boolean adhan = NowPlaying.adhanLive();
        int stop = adhanKeys.decide(e.getKeyCode(), e.getAction(), e.getRepeatCount(), adhan, adhanBar.getVisibility() == View.VISIBLE,
                getCurrentFocus() == adhanBar, adhan && NowPlaying.adhanStopKey(this));
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

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        // while the adhan plays: OK on its bar stops it, and with «إيقاف الأذان بأي زر» any key does; that press is consumed whole
        if (adhanTakes(e)) return true;
        if (reminder != null && reminder.key(e)) return true;   // a reminder's card is up: the first press is the card's
        // the remote's shortcut keys (YouTube, Netflix, Settings, the star): with the stock launcher off nothing else answers them
        if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0)
            android.util.Log.d("UrTVKeys", "key=" + e.getKeyCode() + " scan=" + e.getScanCode());   // for the TV test: does the shortcut key reach the home screen at all? (codes only)
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
        if (edit != null && editKey(e)) return true;   // «تعديل المفضلة»: every key but the volume and power ones is ours
        if (appsEdit != null && appsEditKey(e)) return true;   // «تعديل التطبيقات»: the same
        // the Menu key on an app: its options (the long OK's box); anywhere else the Inputs dropdown, as before
        if (e.getKeyCode() == KeyEvent.KEYCODE_MENU && e.getAction() == KeyEvent.ACTION_UP && edit == null && appsEdit == null
                && getCurrentFocus() != null && getCurrentFocus().getTag() instanceof App) {
            View f = getCurrentFocus();
            optionsHintUsed();
            showAppMenu(f, ((App) f.getTag()).pkg, dockCards.contains(f));
            return true;
        }
        if (e.getKeyCode() == KeyEvent.KEYCODE_MENU && e.getAction() == KeyEvent.ACTION_UP && !pills.isEmpty()) {
            showInputs(pills.get(0), ParentalPolicy.MENU_KEY);
            return true;
        }
        // On the «يُتلى الآن» card ◀ is the previous surah and ▶ the next (a station has none: ◀ goes back to the pills as on any pill)
        if (e.getAction() == KeyEvent.ACTION_DOWN && nowCard != null && getCurrentFocus() == nowCard
                && (e.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT || e.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            NowPlaying.State s = NowPlaying.read(this);
            if (s.show && !s.radio) {
                if (e.getRepeatCount() == 0) {
                    if (e.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) NowPlaying.prev(this); else NowPlaying.next(this);
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
            String mode = BackExit.fromFragment(ConfigSnapshot.read(this).fragment);
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
                openOurSettings(true);
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
                if (!pills.isEmpty()) showInputs(pills.get(0), ParentalPolicy.MENU_KEY);
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
        AspectFrame(Context c) { super(c); }
        @Override protected void onMeasure(int w, int h) {
            int width = MeasureSpec.getSize(w);
            super.onMeasure(w, MeasureSpec.makeMeasureSpec(width * 9 / 16, MeasureSpec.EXACTLY));
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
}
