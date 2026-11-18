#!/usr/bin/env python3
"""4.4 review fixes in the screensaver app (docs/dev/reviews/4.4.md F12-F15): source pins; each fails on the code before its fix."""
import re, sys
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0
def check(what, ok, extra=""):
    global failed
    print(("PASS " if ok else "FAIL ") + what + ("" if ok else "  -> " + str(extra)))
    if not ok: failed += 1
def src(n): return (APP / n).read_text(encoding="utf-8")
def body(s, head):
    i = s.index(head); j = s.find("\n    }\n", i); return s[i:j if j > 0 else len(s)]

# F15: the screensaver start never asks the home's provider on the UI thread
npb, prefs = src("NowPlayingBridge.java"), src("Prefs.java")
ko = body(npb, "static boolean kidsOn(")
check("F15: kidsOn (called by screenUrl on the UI thread) makes no provider query: it reads a kept copy and a thread refreshes it",
      "getContentResolver" not in ko and 'getBoolean("kids_on"' in ko and "new Thread(" in ko and "kidsAsk(" in ko)
check("F15: the blocking query is kidsAsk, used only by that thread", "static boolean kidsAsk(" in npb and "getContentResolver().query(Uri.parse(\"content://sa.urtv.home.nowplaying/kids\")" in body(npb, "static boolean kidsAsk(")
      and len(re.findall(r"kidsAsk\(", npb)) == 2)
check("F15: screenUrl still adds &kids=1 only with the kick-off on and kids on", 'kickoff() && NowPlayingBridge.kidsOn(c) ? "&kids=1" : ""' in prefs)

# F13: the packs list scrolls: every pack can be reached and deleted
gp = src("GamePacksActivity.java")
sc = body(gp, "static int scrollTo(")
def scroll(n, sel, first, fit):   # the same rule as the Java, run here
    if sel < first: first = sel
    if sel >= first + fit: first = sel - fit + 1
    return max(0, min(first, max(0, n - fit)))
check("F13: the rows are drawn from a first-row offset (not the first six), the focus can go to the last pack, and there are more-rows marks",
      "for (int i = first; i < rows.size() && i < first + FIT; i++)" in gp and "i < 6" not in gp and "Math.min(rows.size(), 6)" not in gp and "sel + dy)" in gp and "Math.min(rows.size() - 1, sel + dy)" in gp
      and gp.count("\u25b2") >= 2 and gp.count("\u25bc") >= 2 and "if (first > 0)" in gp and "if (first + FIT < rows.size())" in gp)
check("F13: the offset rule keeps the focused row on the screen for 20 packs, first to last and back",
      "if (sel < first) first = sel;" in sc and "if (sel >= first + fit) first = sel - fit + 1;" in sc and "Math.min(first, Math.max(0, n - fit))" in sc
      and all((lambda f: f <= s < f + 6)(scroll(20, s, 0, 6)) for s in range(20)) and scroll(20, 19, 0, 6) == 14 and scroll(20, 0, 14, 6) == 0)

# F14: a pack is streamed to a temporary file while it is hashed, OutOfMemoryError is caught, a round that starts stops the download
store, net, launcher_net = src("GamePackStore.java"), src("Net.java"), (ROOT / "android/launcher/src/main/java/sa/urtv/home/Net.java").read_text(encoding="utf-8")
ens = body(store, "private static void ensure(")
check("F14: the pack is not buffered whole: no Net.get without a sink for it, the body is never a byte[] (d.body) in the store",
      "d.body" not in store and "o.write(d.body)" not in store and "Net.get(BASE + p.id + \"/pack.gb\", null, 20000, 120000, (int) GamePacks.PACK_MAX, sink)" in store and "MessageDigest.getInstance(\"SHA-256\")" in store and "shaOk(d.body" not in store)
check("F14: the download thread catches Throwable (OutOfMemoryError), not only Exception", "catch (Throwable ignored)" in ens and "catch (Exception ignored)" not in ens)
check("F14: a round re-checked before the big download and on every write while it runs (it stops, and the temporary file goes)",
      ens.count("QuizBank.playing") >= 2 and "if (QuizBank.playing) throw new IllegalStateException" in store and "if (!ok) tmp.delete();" in store and "tmp.length() == p.bytes" in store)
check("F14: Net has a sink call (in both apps, the same file) and counts the cap over what it wrote",
      "static Reply get(String url, Map<String, String> headers, int readMs, int deadlineMs, int maxBytes, OutputStream sink)" in net and "total[0] > cap" in net and net.split("\n", 1)[1] == launcher_net.split("\n", 1)[1])

sys.exit(1 if failed else 0)
