#!/usr/bin/env python3
"""The tester's step 52 findings (build 228, a real TCL) that are pinned in the source: the screensaver's first frame before the WebView and its log, the debug switches for
the home's drawing (dock.sh debug), the top row's needless layout at every tray step, the dark patch under the stocks card, the clean row refreshed on resume, the gap above
the auto-update switch, and the once-a-minute spike. (The quiz's findings are in quiz_test.py.)"""
import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
SAVER = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
def src(p): return Path(p).read_text(encoding="utf-8")

dream, snap = src(SAVER / "ScreenDream.java"), src(SAVER / "SaverSnapshot.java")
ss = src(SAVER / "SaverStart.java"); mact = src(SAVER / "MainActivity.java")
check("screensaver start (SaverStart, the dream's and the app's own page): the window gets its content (the last picture or the ground colour), the first frame is drawn, and only then is the WebView created (onPreDraw, then post)",
      ss.index("host.setContent(box);") < ss.index("addOnPreDrawListener") < ss.index("box.post(() -> addPage(") and "host.createPage()" in ss.split("private static void addPage(")[1] and "host.createPage()" not in ss.split("private static void addPage(")[0].split("static void begin(")[1]
      and "SaverStart.begin(" in dream and "SaverStart.begin(" in mact and "web = ScreenView.create(" in dream.split("createPage()")[1][:200] and "web = ScreenView.create(" in mact.split("createPage()")[1][:200]
      and "SaverSnapshot.capture(MainActivity.this, getWindow(), shownUrl, web)" in mact)
check("screensaver start: one log line each (tag UrTVSaver) for what is on disk and why it is or is not used, the first frame, the page view's creation time, the page live, and the copy's result",
      all(k in ss for k in ('"snapshot: " + SaverSnapshot.why(', '"snapshot: first frame (', '"snapshot: the page\'s view created in ', '"snapshot: the page is live at ')) and "static String why(" in snap
      and all(k in snap for k in ('"snapshot: copy of the window ', '"snapshot: saved ', '"snapshot: not taken (')))

dbg = src(HOME / "DebugFlags.java")
names = re.search(r'NAMES = \{([^}]*)\}', dbg).group(1)
check("debug switches: traynoglass, nopillpanel, nowifimark, nosnapshot, nocardglass; `all` only off; nothing on by default",
      all(f'"{n}"' in names for n in ("traynoglass", "nopillpanel", "nowifimark", "nosnapshot", "nocardglass")) and 'getBoolean(name, false)' in dbg and 'if (value) return false;' in dbg)
main, pill, card, hs, rcv = src(HOME / "MainActivity.java"), src(HOME / "PillBar.java"), src(HOME / "HomeCard.java"), src(HOME / "HomeSnapshot.java"), src(HOME / "DockReceiver.java")
check("each switch turns exactly its piece off: the tray's fill and line, the top bar's panel, the Wi-Fi mark, the home's snapshot (shown and taken), the cards' tray glass; the home restarts at a change",
      'DebugFlags.on(this, "traynoglass")' in main and "dockBand.setColor(noGlass ? 0" in main and 'DebugFlags.on(getContext(), "nopillpanel")' in pill and 'DebugFlags.on(this, "nowifimark")' in main
      and hs.count('DebugFlags.on(') == 2 and 'DebugFlags.on(activity, "nocardglass")' in card and "debugChanged" in main and "recreate();" in main.split("debugChanged = ")[1][:200])
check("dock.sh debug <flag> [on|off], debug all off, debug show reach the home (DockReceiver DEBUG, in the manifest); a wrong word is refused with the usage line",
      'debug:*) case "$2" in' in src(ROOT / "optimize/dock.sh") and "traynoglass|nopillpanel|nowifimark|nosnapshot|nocardglass" in src(ROOT / "optimize/dock.sh") and 'case "sa.urtv.home.DEBUG"' in rcv
      and "sa.urtv.home.DEBUG" in src(ROOT / "android/launcher/src/main/AndroidManifest.xml"))
check("top row: a focus step on the tray no longer schedules the bar's close and layout 160 ms later (only when it is open or moving; and a bar that is already shaped is not laid out again)",
      "else if (open > 0f || anim != null && anim.isRunning()) postDelayed(closer" in pill and "if (open == to) { if (applied != to) apply(); return; }" in pill and "applied = open;" in pill)

tpl = src(ROOT / "src/template.html")
check("home card: the card styles' drop shadow is off on the home (a straight-edged dark patch under the glass), the focus ring (.lf) stays",
      "html.hcard .week:not(.lf){box-shadow:none!important}" in tpl)
check("the idle page: the 60 s render covers only the adhkar and stocks, 7 s off the 15 s beat (the two ran in one frame with the clock's minute: the once-a-minute spike)",
      "setTimeout(() => setInterval(() => renderPanel(false, 'adhkar', 'stocks'), 60*1000), 7000);" in tpl)
st = src(SAVER / "SettingsActivity.java")
check("settings: the «UrTV النظيف» row is asked again at each return and 1.5 s later (it said «مفعّل» only after reopening); the auto-update switch has a 32 px gap above it",
      "cleanAsked = false;   // asked again each time" in st and "root.postDelayed(() -> { if (!isFinishing() && !isDestroyed()) { cleanAsked = false; homeCleanSeen(); } }, 1500);" in st
      and "topMargin += dp(32)" in st and 'View autoRow = row("التحديث التلقائي للصفحة"' in st)

# ---- step 54
kt = src(HOME / "KeyTrace.java"); typ = src(HOME / "TypeService.java")
check("keytrace + nokeyfilter: `dock.sh debug keytrace on` logs per key (tag UrTVKeyTrace) queue, dispatch, focus, layout, draw and six frame intervals; `nokeyfilter` stops the accessibility key filter",
      '"keytrace"' in dbg and '"nokeyfilter"' in dbg and 'android.util.Log.i("UrTVKeyTrace"' in kt and all(k in kt for k in ("queue=", "dispatch=", "focus@=", "layout@=", "draw@=", "frames=[", "janky="))
      and 'DebugFlags.on(this, "keytrace")' in main and "KeyTrace.focusChanged();" in main and 'DebugFlags.on(this, "nokeyfilter")' in typ and "nokeyfilter|keytrace" in src(ROOT / "optimize/dock.sh"))
mact = src(SAVER / "MainActivity.java")
check("the saver app's own page starts like the dream: SaverStart (picture, first frame, then the WebView), the same log lines, and its own snapshot capture",
      "SaverStart.begin(" in mact and "SaverSnapshot.capture(MainActivity.this" in mact)
ca = src(SAVER / "QuizActivity.java"); room_ = src(SAVER / "QuizRoom.java"); page_ = src(ROOT / "assets/quiz/play.html")
check("quiz: «جاوب الجميع» in the header of the reveal when it ended early (beside «الأسرع»); every sign of the bank (+ U+2212 × ÷, dashes) isolates the whole expression left to right; the phone follows the TV's digits (state dg)",
      'S.t("جاوب الجميع")' in ca.split("meta.addView(mt);")[1][:300] and "u2212" in room_.split("MATH = ")[1][:300] and 'append(",\\"dg\\":")' in room_
      and "r.dg === 1" in page_ and "AR && !LATIN" in page_)
sett = src(SAVER / "SettingsActivity.java")
check("settings: the row that opened another screen gets the focus back on return (launchedRow)", "launchedRow = (String) card.getTag(); onClick.run();" in sett and "restoreFocus(back, 0, scroll.getScrollY())" in sett)
sys.exit(1 if failed else 0)
