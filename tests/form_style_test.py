#!/usr/bin/env python3
"""4.4 beta 5 «شكل آخر المباريات» (docs/research/LEAGUE-V2-PHASE-A-PLAN.md §3a3): dots (the default), boxes, boxes-letters; the row in CONTENT →
«المباريات» right after «الدوريات كاملة», in all five places of SettingsHub, the agent's key (an unknown value refused), the page told by
#formstyle= (nothing for the default), export / import, the docs. The page's drawing of each style: tests/league_table.test.mjs.
    python3 tests/form_style_test.py"""
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
A = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
failed = 0


def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok


rd = lambda p: Path(p).read_text(encoding="utf-8")
prefs, act, hub, rcv = rd(A / "Prefs.java"), rd(A / "SettingsActivity.java"), rd(A / "SettingsHub.java"), rd(A / "SaverReceiver.java")
check("Prefs: the three values with their labels, dots the default (an unknown stored value reads as dots)",
      'FORM_STYLES = {{"dots", "نقاط"}, {"boxes", "مربعات"}, {"boxes-letters", "مربعات بحروف"}}' in prefs
      and 'String v = sp.getString("formStyle", "dots"); for (String[] f : FORM_STYLES) if (f[0].equals(v)) return v; return "dots";' in prefs)
check("the page is told #formstyle= only when it is not the default (the default fragment is unchanged)", '("dots".equals(formStyle()) ? "" : "&formstyle=" + formStyle())' in prefs)
i, j, k = act.find('row("الدوريات كاملة"'), act.find('row("شكل آخر المباريات"'), act.find('row("رياضات أخرى"')
check("the row: right after «الدوريات كاملة», before «رياضات أخرى», a choice list", 0 < i < j < k and 'pickChoice("شكل آخر المباريات", Prefs.FORM_STYLES' in act, (i, j, k))
for name, pat in (("ROWS", r'static final String\[\]\[\] ROWS = \{'), ("ORDER", r'static final String\[\]\[\] ORDER = \{'),   # 4.4 beta 4: one tree, so no *_432 tables
                  ("SCOPES", r'SCOPES = \{')):
    m = re.search(pat, hub)
    body = hub[m.end():hub.find("};", m.end())] if m else ""
    check(f"SettingsHub.{name} has it, after «الدوريات كاملة»", "شكل آخر المباريات" in body and body.find("الدوريات كاملة") < body.find("شكل آخر المباريات"))
check("SettingsHub.SCOPES: both (the page of «المباريات» is the home's, its value is the screensaver's setting)", '{"شكل آخر المباريات", "both"}' in hub)
check("the agent's key: validated (one of the three), set, and shown by saver.sh show",
      'if (formStyle != null && !isChoice(Prefs.FORM_STYLES, formStyle))' in rcv and "p.setFormStyle(formStyle)" in rcv and '"\\nformstyle: " + p.formStyle()' in rcv)
sh = rd(ROOT / "optimize/saver.sh")
check("saver.sh formstyle: a set1 line beside cardstyle, and its usage line in the header", 'formstyle)set1 formstyle "${2:?usage: optimize/saver.sh formstyle dots|boxes|boxes-letters}" ;;' in sh
      and "#   optimize/saver.sh formstyle boxes" in sh)
mv = rd(ROOT / "optimize/settings_move.py")
check("export / import carry it (the allow-list, its values)", '"form_style"' in mv and 'FORM_STYLES = ("dots", "boxes", "boxes-letters")' in mv and 'es("formstyle", v)' in mv)
r = subprocess.run(["bash", "-c", "PY=python3 bash optimize/saver.sh formstyle 2>&1 | head -2"], cwd=ROOT, capture_output=True, text=True, timeout=30)
check("saver.sh formstyle with no value: its usage", "usage: optimize/saver.sh formstyle dots|boxes|boxes-letters" in r.stdout + r.stderr, r.stdout[-200:])
check("docs: SETTINGS.md (its command), the tree (made by the tool), FEATURES.md (the agent's line)", "`optimize/saver.sh formstyle <dots, boxes, boxes-letters>`" in rd(ROOT / "docs/SETTINGS.md")
      and "——— شكل آخر المباريات" in rd(ROOT / "docs/SETTINGS-TREE.md") and "**الوكيل:** `optimize/saver.sh formstyle boxes`" in rd(ROOT / "docs/FEATURES.md"))
check("English: the row and its values (i18n/en/saver.tsv)", all(x in rd(ROOT / "i18n/en/saver.tsv") for x in ("شكل آخر المباريات\tRecent form style", "نقاط\tDots", "مربعات بحروف\tBoxes with letters")))
tpl = rd(ROOT / "src/template.html")
check("the page: #formstyle= read (anything else is dots)", "formStyle: (v => ['boxes', 'boxes-letters'].includes(v) ? v : 'dots')(_Q.get('formstyle'))" in tpl)
raise SystemExit(1 if failed else 0)
