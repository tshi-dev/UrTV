#!/usr/bin/env python3
"""4.4 the owner: with «كنبة» the settings are couch-tv's, exactly (Seagey/couch-tv, MIT; docs/mockups/settings-couchtv/, approved).
The other looks keep the settings as they are. couch-tv's numbers (its rem: a 120th of the screen), its row kinds, its pages; our rows all there,
the same registry; Arabic mirrored.   python3 tests/settings_couch_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


st, cs, hub = ((APP / f).read_text(encoding="utf-8") for f in ("SettingsActivity.java", "CouchStyle.java", "SettingsHub.java"))
num = lambda name: float(re.search(rf"\b{name} = ([0-9.]+)f", cs).group(1))

# ---- 4.4 beta 2 «كنبة» v2 (the owner approved docs/mockups/settings-style/couch-v2-*): the first page's bars
sec = st[st.index("    private void couchSection("):].split("\n    }\n")[0]
secf = st[st.index("    private void couchSectionFocus("):].split("\n    }\n")[0]
check("v2: a taller bar per section (5.75 rem, was 4.75), the name only: no icon, no bar mark",
      num("SEC2_H") == 5.75 and "card.setMinimumHeight(cs.px(CouchStyle.SEC2_H));" in sec and "ic.setVisibility(View.GONE);" in sec and "bar.setVisibility(View.GONE);" in sec)
check("v2: the focused bar shows its name large (2.75 rem) and one plain line of what is inside (SettingsHub.SHORT) that wraps, never cut",
      num("SEC2_BIG") == 2.75 and "cs.px(has ? CouchStyle.SEC2_BIG : CouchStyle.SEC2_TEXT)" in secf and "line.setVisibility(has ? View.VISIBLE : View.GONE);" in secf
      and "line.setMaxLines(3);" in sec and "setEllipsize" not in sec and "SettingsHub.shortLine(cat)" in sec)
check("v2: the line is never smaller than the couch hint (1.4375 rem)", num("SEC2_LINE") >= num("HINT"))
check("v2: it grows by its words, never by a scale or an animation, so a weak TV has no motion (the owner's light version)",
      "animate()" not in secf and "setScale" not in secf and "if (cs != null) couchSectionFocus(x, has); else" in st)
short = dict(zip(re.findall(r"\w+", hub.split("static final String[] SHORT_KEYS = {")[1].split("};")[0]),
                 re.findall(r'"([^"]+)"', hub.split("static final String[] SHORT_LINES = {")[1].split("};")[0])))
check("v2: every section has its line (search aside), plain words: no «*», no «·», no marks",
      set(short) == {"PRAY", "PHOTOS", "LOOK", "SAVER", "APPS", "CONTENT", "FAMILY", "LANG", "UPDATE"}
      and not any(ch in v for v in short.values() for ch in "*·✦•★") and short["PHOTOS"] == "أضف صورك أو احذفها، والخلفيات", short)
check("couch-tv's sizes, in its rem (a 120th of the screen's width): the safe area 5 x 3.75, the side 36, the gap 4, a section and a row 4.75 high, radius 1.5",
      "rem = c.getResources().getDisplayMetrics().widthPixels / 120f;" in cs
      and (num("SAFE_X"), num("SAFE_Y"), num("SIDE"), num("GAP"), num("SEC_H"), num("ROW_H"), num("ROW_RADIUS"), num("SEC_RADIUS")) == (5, 3.75, 36, 4, 4.75, 4.75, 1.5, 1.5)
      and (num("TITLE"), num("SEC_TEXT"), num("ROW_TEXT"), num("ROW_SUB"), num("HEAD"), num("SHEAD"), num("SEC_ICON_BOX"), num("BOX")) == (2.875, 1.75, 1.75, 1.25, 2.375, 1.25, 3.25, 2.25))
check("…its colours: the veil rgba(10,8,12,.8), the rows 8 % white, the value 62 %, the focus white with #111, the toggle's green #30d158",
      "VEIL = 0xCC0A080C" in cs and "ROW = 0x14FFFFFF" in cs and "VALUE = 0x9EFFFFFF" in cs and "INK = 0xFF111111" in cs and "GREEN = 0xFF30D158" in cs)
check("…its focus: a section x1.04, a row x1.02, in 200 ms on cubic-bezier(.2,.8,.2,1)",
      "SEC_SCALE = 1.04f, ROW_SCALE = 1.02f;" in cs and "static final long MS = Motion.MS;" in cs and "return Motion.ease();" in cs
      and "static final long MS = 200;" in (APP / "Motion.java").read_text(encoding="utf-8") and "new PathInterpolator(0.2f, 0.8f, 0.2f, 1f)" in (APP / "Motion.java").read_text(encoding="utf-8"))
check("CouchStyle is made whenever the 4.3.2 hub is not (4.4 beta 2: «شكل شاشة الإعدادات» = «كنبة», prefs.settingsStyle(); tests/settings_one_form_test.py)",
      "if (!classic) cs = new CouchStyle(this);" in st and 'classic = !"couch".equals(prefs.settingsStyle());' in st)
check("4.4 beta 2: «شكل شاشة الإعدادات» switches the settings at once: they open again in the other style, on its row (a look only when it is a full-design one: «كنبة»)",
      "prefs.setSettingsStyle(Prefs.SETTINGS_STYLES[i]);" in st and "finish();" in st[st.index('row("شكل شاشة الإعدادات"'):][:900]
      and "finish();" not in st[st.index("private void flipCouch()"):][:300])
check("a weak TV («الوضع الخفيف») gets a nearly solid veil instead of the see-through one",
      'boolean lite = "on".equals(prefs.lite()) || ("auto".equals(prefs.lite()) && Prefs.lowRam(this));' in st and "lite ? CouchStyle.VEIL_LITE : CouchStyle.VEIL" in st)
rowfn = st[st.index("    private View row(String label, String value, int icon, final Runnable onClick) {"):].split("\n    }\n")[0]
check("every registry row is drawn: the couch row is the same row (its key, its action, its section), added where ours was",
      rowfn.index("if (!on(cat)) return offscreen();") < rowfn.index("if (cs != null) {") and "if (onClick != null) actions.put(FocusKey.key(label), onClick);" in rowfn
      and "couchRow(label, value, onClick, FocusKey.key(label));" in rowfn and "list.addView(couch, clp);" in rowfn)
side_rows = sorted({r for r, c in re.findall(r'\{"([^"]+)", ([A-Z]+)\}', hub[hub.index("static final String[][] ROWS = {"):hub.index("static final String[][] HEADERS = {")])})
tree = (ROOT / "docs/SETTINGS-TREE.md").read_text(encoding="utf-8")
unesc = lambda r: re.sub(r"\\u([0-9a-fA-F]{4})", lambda m: chr(int(m.group(1), 16)), r).replace("\u2066", "").replace("\u2069", "")
plain_tree = tree.replace("\u2066", "").replace("\u2069", "")
missing = [r for r in side_rows if unesc(r) not in plain_tree]
check(f"…and every row of the side list's registry ({len(side_rows)}) is in docs/SETTINGS-TREE.md, so nothing is missing in the couch-tv settings", not missing, missing)
couchrow = st[st.index("    private LinearLayout couchRow("):].split("\n    }\n")[0]
check("couch-tv's kinds of row, in one line (the owner, build 413): on/off a ring (green with a tick when on) with its words before it, the value at the far end (a long one wraps there), a link a chevron",
      "state > 0 ? CouchStyle.Mark.RING_ON : CouchStyle.Mark.RING_OFF; val = CouchText.after(value, OnOff.wordEnd(value));" in couchrow
      and "else if (value != null && !value.isEmpty()) val = value;" in couchrow and "if (mark == 0 && !CouchText.short_(val) && onClick != null) mark = CouchStyle.Mark.CHEVRON;" in couchrow
      and "SettingsHub.scopeWords(key)" in couchrow and "v.setMaxLines(2);" in st)
check("both styles' rows: one line in one text size, at most about 80-90 px at 1080p, so 5 or 6 show (they were 180 px and only 3 fit)",
      "static final int ROW_SP = 18, ROW_PAD = 8;" in st and "name.setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_SP);" in rowfn and "val.setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_SP);" in rowfn
      and "texts.setOrientation(LinearLayout.HORIZONTAL);" in rowfn and "card.setPadding(dp(20), dp(ROW_PAD), dp(16), dp(ROW_PAD));" in rowfn
      and num("ROW_H") == 4.75 and num("ROW_TEXT") * 16 * 1.3 + 2 * num("ROW_PAD_Y") * 16 <= 96)
dlg = st[st.index("    private static final class Dlg extends WordsBuilder {"):st.index("    void dress(AlertDialog d) {")]
check("a plain picker (one choice, ticks with «حفظ», or a list) is a page in place with the section's breadcrumb; any with a view, a message or buttons of its own stays a dialog",
      "if (owner.cs != null && plain && !raw && items != null && (checks == null || save != null)) {" in dlg and "owner.showPage(p);" in dlg
      and "public AlertDialog.Builder setView(View v) { plain = false;" in dlg and "public AlertDialog.Builder setMessage(CharSequence t) { plain = false;" in dlg
      and "crumb.setText(page != null ? conv(SettingsHub.name(screen)) : \"\");" in st)
check("…Back leaves the page (ticks saved by Back, as the dialog saves them), the focus back on the row that opened it",
      "if (page != null) { closePage(true); return; }" in st and "if (back && p.save != null) p.save.run();" in st and "forceWant = p.back;" in st)
check("the search: a round button beside «الإعدادات» (couch-tv has none), couch-tv's typing box, the rows found as the words are typed",
      "find.setOnClickListener(v -> openSearch());" in st and "g.setStroke(cs.px(0.1875f), CouchStyle.WHITE);" in st and "searchH.postDelayed(searchNow, SEARCH_MS);" in st
      and "if (cs != null) { openSearch(); return; }" in st)
check("the scene packs' rows have their thumbnails (couch-tv's scene rows)", 'p.thumbs = new String[] {"scene-thumbs/saudi.jpg", "scene-thumbs/couch.jpg"};' in st
      and all((ROOT / f"android/app/src/main/assets/scene-thumbs/{k}.jpg").is_file() for k in ("saudi", "couch")))
check("mirrored for Arabic, right in English: the layout's direction, the chevron toward the page (left in Arabic), the row's way to its section's item",
      "new CouchStyle.Mark(this, mark, Dir.rtl())" in st and "if (rtl) { a.moveTo(x + d / 2, y - d); a.lineTo(x - d / 2, y);" in cs
      and "if (Dir.rtl()) couch.setNextFocusRightId(toSide.getId()); else couch.setNextFocusLeftId(toSide.getId());" in rowfn)
if shutil.which("javac"):
    with tempfile.TemporaryDirectory() as d:
        (Path(d) / "Run.java").write_text('''package sa.urtv.screensaver;
public class Run { public static void main(String[] a) {
  System.out.println(CouchText.after("مفعّل (أفق الليل من المغرب إلى الفجر)", 5) + "|" + CouchText.after("متوقف · تتبع مظهر الرئيسية", 5) + "|" + CouchText.after("متوقف", 5)
    + "|" + CouchText.short_("٢٠ ثانية") + "|" + CouchText.short_("ينزّل أحدث صفحة للشاشة فقط، لتحديث التطبيقين") + "|" + CouchText.short_(null));
} }''', encoding="utf-8")
        c = subprocess.run(["javac", "-encoding", "UTF-8", "-d", d, str(APP / "CouchText.java"), str(Path(d) / "Run.java")], capture_output=True)
        check("CouchText compiles as plain Java", c.returncode == 0, c.stderr.decode()[-500:])
        r = subprocess.run(["java", "-Dfile.encoding=UTF-8", "-cp", d, "sa.urtv.screensaver.Run"], capture_output=True)
        out = r.stdout.decode("utf-8").strip()
        check("…an on/off value's words after «مفعّل» or «متوقف» without the brackets or the «·»; a short value at the far end, a long one not",
              out == "أفق الليل من المغرب إلى الفجر|تتبع مظهر الرئيسية||true|false|false", out)
else:
    print("NOTE javac not found: CouchText not run here (CI runs it)")
sys.exit(1 if failed else 0)
