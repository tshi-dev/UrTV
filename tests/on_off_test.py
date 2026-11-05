#!/usr/bin/env python3
"""On/off settings look one way everywhere (the owner's choice I, notes/settings-proto/v5-toggle-I.png): no switch; three green triangles at the
row's start while it is on, and its state word in the DGA green (#1B8354 on a light fill, #54C08A on a dark one). Checked from the source and,
for both apps' OnOff, run on a JVM: every row that OK flips in the screensaver's settings shows an on/off value OnOff reads, the home's adhan
rows too, and the adhan's page opens on its switch. The phone page is checked in tests/phone_page.test.mjs.
Needs javac for the JVM part.   Run: python3 tests/on_off_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

a = (APP / "OnOff.java").read_text(encoding="utf-8")
h = (HOME / "OnOff.java").read_text(encoding="utf-8")
norm = lambda s: re.sub(r"\bpublic\s+", "", s.replace("package sa.urtv.home;", "package X;").replace("package sa.urtv.screensaver;", "package X;")
                        .replace("The launcher has the same class (sa.urtv.home.OnOff)", "").replace("The screensaver app has the same class (sa.urtv.screensaver.OnOff)", ""))
check("the two apps' OnOff are the same class (but its package and who may call it)", norm(a) == norm(h))
check("the greens are the DGA platforms' sa-flag ones: 600 #1B8354 on a light fill, 400 #54C08A on a dark one",
      "GREEN_ON_LIGHT = 0xFF1B8354, GREEN_ON_DARK = 0xFF54C08A" in a)
for app in ("app", "launcher"):
    d = ROOT / f"android/{app}/src/main/res/drawable/ic_on.xml"
    check(f"{app}: the mark is three triangles pointing into the row, in the DGA green", d.exists()
          and 'android:pathData="M18,0L4,9L18,18zM18,18L4,27L18,36zM18,36L4,45L18,54z"' in d.read_text(encoding="utf-8")
          and "#FF25935F" in d.read_text(encoding="utf-8"))

st = (APP / "SettingsActivity.java").read_text(encoding="utf-8")
rowfn = st[st.index("private View row(String label, String value, int icon, final Runnable onClick)"):].split("\n    }\n")[0]
check("settings: every row has the mark's place at its start (shown while on), the icon after it, so the icons stay in a line",
      "final int state = OnOff.state(value);" in rowfn and "final ImageView mark = onMark(state);" in rowfn
      and rowfn.index("card.addView(mark,") < rowfn.index("card.addView(ic, ip);"))
check("settings: the word and the mark take the green of what is under them, at rest and while focused",
      "paintState(val, mark, shown, state, false);" in rowfn and "paintState(val, mark, shown, state, has);" in rowfn
      and "int bg = focused ? look.focus : (look.light ? 0xFFFFFFFF : 0xFF000000);" in st)
check("settings: no switch widget anywhere (no Switch, CheckBox or ToggleButton)", not re.search(r"\b(Switch|CheckBox|ToggleButton|SwitchCompat)\b", st))
check("settings: adjRow finds the name by its tag, not by the row's children (the mark came first)", 'card.findViewWithTag("name")' in st and "getChildAt(1)" not in st)

# every row that OK flips (a setter with !prefs.x()) shows a value OnOff reads as on or off, both ways
flips = re.findall(r'row\("([^"]+)",\s*(prefs\.\w+\(\))\s*\?\s*("(?:[^"\\]|\\.)*")\s*:\s*("(?:[^"\\]|\\.)*"),[^\n]*\n\s*new Runnable\(\) \{ public void run\(\) \{\s*prefs\.set\w+\(!prefs\.\w+\(\)\)', st)
check("the source has the flip rows (the test reads what it should)", len(flips) >= 12, len(flips))
values = []
for name, _, on, off in flips: values += [(name, "on", on[1:-1]), (name, "off", off[1:-1])]
til = (HOME / "tilawah/MainActivity.java").read_text(encoding="utf-8")
strings = dict(re.findall(r'<string name="(adhan_[a-z_]+)">([^<]*)</string>', (ROOT / "android/launcher/src/main/res/values/strings.xml").read_text(encoding="utf-8")))
for k in ("adhan_on", "adhan_short_on", "adhan_stopkey_on"): values.append(("home " + k, "on", strings[k]))
for k in ("adhan_off", "adhan_short_off", "adhan_stopkey_off"): values.append(("home " + k, "off", strings[k]))
for w in ("مفعّل", "متوقف"): values.append(("plain", "on" if w == "مفعّل" else "off", w))
values.append(("not one", "none", "شاشة التوقف · الشاشة الرئيسية"))
values.append(("not one", "none", "يسار + · يمين − · موافق للتصفير"))

if not shutil.which("javac"):
    print("SKIP on/off: no javac here")
    sys.exit(1 if failed else 0)
with tempfile.TemporaryDirectory() as tmp:
    feed = Path(tmp) / "v.txt"
    feed.write_text("\n".join(v for _, _, v in values) + "\n", encoding="utf-8")
    outs = {}
    for cls, src in (("sa.urtv.screensaver.OnOff", APP / "OnOff.java"), ("sa.urtv.home.OnOff", HOME / "OnOff.java")):
        d = Path(tmp) / cls
        d.mkdir()
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(d), str(src), str(ROOT / "tests/OnOffRun.java")], check=True, capture_output=True)
        outs[cls] = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", str(d), "OnOffRun", cls, str(feed)], check=True, capture_output=True).stdout.decode("utf-8")
check("both apps read every value the same", outs["sa.urtv.screensaver.OnOff"] == outs["sa.urtv.home.OnOff"])
lines = outs["sa.urtv.home.OnOff"].split("\n")
want = {"on": "1", "off": "0", "none": "-1"}
bad = [(n, k, v, lines[i]) for i, (n, k, v) in enumerate(values) if lines[i].split("\t")[0] != want[k]]
check("every row OK flips reads as on when on and off when off (the home's adhan rows too); other values are not on/off", not bad, bad)
ends = [(v, lines[i]) for i, (n, k, v) in enumerate(values) if k != "none" and v[:int(lines[i].split("\t")[1])] not in ("يعمل", "تعمل", "مفعّل", "مفعّلة", "ظاهر", "ظاهرة", "متوقف", "متوقفة", "مخفي", "مخفية")]
check("the green covers the state word only (it ends at the first space, «·» or «•»)", not ends, ends)
greens = dict(re.findall(r"green (\w+) = (\w+)", outs["sa.urtv.home.OnOff"]))
check("on white and on the dark look's white focus fill: the dark green; on black and the light look's ink fill: the light green; on amber: the dark green",
      greens == {"FFFFFFFF": "FF1B8354", "FF000000": "FF54C08A", "F2FFFFFF": "FF1B8354", "F214161A": "FF54C08A", "FF17181B": "FF54C08A", "FFF0A64A": "FF1B8354"}, greens)

# ---- the home's adhan page: its switch first (the settings' «الأذان» button lands on it: OK, OK), the rows drawn the same way
adhan = til[til.index("// the adhan section:"):til.index("notifyDataSetChanged();", til.index("// the adhan section:"))]
check("the adhan's page starts with its switch (before today's times), so the first row focused is the switch",
      adhan.index("KIND_ADHAN_TOGGLE") < adhan.index("for (int prayer : PrayerTimes.PRAYERS)") and "firstReciter()" in til
      and "if (adapter.rows.get(i).kind == KIND_RECITER) return i;" in til)
view = til[til.index("@Override public View getView(int position"):]
check("the home's rows: the mark at the start while on, the line in the green of the panel or the focus fill",
      "int state = OnOff.state(row.detail);" in view and "mark.setVisibility(state == 1 ? View.VISIBLE : View.INVISIBLE);" in view
      and "detail.setTextColor(green);" in view and "mark.setImageTintList(green);" in view and "R.attr.gFocus" in til)
lay = (ROOT / "android/launcher/src/main/res/layout/item_row.xml").read_text(encoding="utf-8")
check("the home's row layout has the mark first (the row's start), hidden by default", lay.index('android:id="@+id/mark"') < lay.index('android:id="@+id/name"')
      and 'android:visibility="invisible"' in lay.split('android:id="@+id/mark"')[1].split("/>")[0])
prov = (HOME / "tilawah/NowPlayingProvider.java").read_text(encoding="utf-8")
seg = prov[prov.index('"adhan_on".equals(uri.getLastPathSegment())'):].split("return a;")[0]
check("the home answers .../adhan_on with its switch only (read only, the provider's own read permission)", 'new String[]{"on"}' in seg
      and "adhanOn() ? 1 : 0" in seg and "update(Uri uri, ContentValues v, String s, String[] a) { return 0; }" in prov)
par = (HOME / "ParentalActivity.java").read_text(encoding="utf-8")
paint = par[par.index("private void paint(TextView t, boolean on, boolean focused)"):].split("\n    }\n")[0]
check("the family lock's on/off cells: the mark and the green word when on, no gold ring any more; kids mode is one of them",
      "R.drawable.ic_on" in paint and "OnOff.green(" in paint and "setStroke" not in paint and "state(s.kids, v -> kids())" in par)
fam = (HOME / "FamilyDnsActivity.java").read_text(encoding="utf-8")
check("«حماية العائلة»: its state line has the mark and the green while on", "onState(s.state == FamilyDns.State.ON);" in fam and "R.drawable.ic_on" in fam)
sys.exit(1 if failed else 0)
