#!/usr/bin/env python3
"""4.3 B10: the report page (docs/share/, opened from the QR code of «أرسل تقرير» and of optimize/share.sh) asks what happened in the owner's own
words and does not send without them (5 characters at least, 500 at most), unless the summary already carries them («From the owner:», from
share.sh --owner). What is sent is the summary, then «From the owner:» and the words. Run on node with a stand-in page (no browser).
    python3 tests/report_words_test.py"""
import json, shutil, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok

html = (ROOT / "docs/share/index.html").read_text(encoding="utf-8")
check("the page has the box, required by default, and «أرسل» starts off", '<textarea id="words" maxlength="500"' in html and 'id="send" type="button" disabled>' in html)
if not shutil.which("node"):
    print("SKIP the page's script: no node")
    sys.exit(1 if failed else 0)
script = html[html.index("<script>") + 8:html.index("</script>")]
harness = r"""
const els = {}; const el = id => els[id] || (els[id] = {id, hidden: true, disabled: id === 'send', textContent: '', value: '', handlers: {},
  addEventListener(k, f) { this.handlers[k] = f; }});
global.document = { getElementById: el };
let posted = null;
global.fetch = (u, o) => { posted = o.body.get(Object.keys(Object.fromEntries(o.body))[0]); return Promise.resolve(); };
global.URLSearchParams = URLSearchParams;
const cases = JSON.parse(process.argv[process.argv.length - 1]), out = [];
(async () => {
for (const c of cases) {
  for (const k of Object.keys(els)) delete els[k];
  posted = null;
  global.location = { hash: '#' + encodeURIComponent(c.summary) };
  new Function(SCRIPT)();
  await new Promise(r => setTimeout(r, 10));
  const r = { start: el('send').disabled };
  el('words').value = c.words; if (el('words').handlers.input) el('words').handlers.input();
  r.after = el('send').disabled;
  el('send').handlers.click.call(el('send'));
  await new Promise(r => setTimeout(r, 10));
  r.posted = posted; r.label = el('words-label').textContent;
  out.push(r);
}
console.log(JSON.stringify(out));
})();
""".replace("SCRIPT", json.dumps(script))
cases = [{"summary": "home: 4.3 (900)\nandroid: 14\n", "words": ""},
         {"summary": "home: 4.3 (900)\nandroid: 14\n", "words": " ok "},
         {"summary": "home: 4.3 (900)\nandroid: 14\n", "words": "the adhan did not play at Fajr " + "x" * 600},
         {"summary": "home: 4.3\nFrom the owner:\nthe USB stick is not read\n", "words": ""}]
r = subprocess.run(["node", "-e", harness, json.dumps(cases)], capture_output=True, text=True)
try:
    got = json.loads(r.stdout)
except ValueError:
    got = None
check("the page's script runs", got is not None and len(got) == 4, r.stderr[-400:])
if got:
    a, b, c, d = got
    check("no words: «أرسل» stays off and nothing is sent", a["start"] and a["after"] and a["posted"] is None, a)
    check("fewer than 5 characters: still off, nothing sent", b["after"] and b["posted"] is None, b)
    check("words: «أرسل» comes on; the summary is sent, then «From the owner:» and the words, cut at 500",
          not c["after"] and c["posted"].startswith("home: 4.3 (900)\nandroid: 14\n\nFrom the owner:\nthe adhan did not play at Fajr ")
          and len(c["posted"].split("From the owner:\n")[1].rstrip("\n")) == 500, c)
    check("the summary already has the owner's words: the box is optional, it sends as it is", not d["start"] and d["posted"] == "home: 4.3\nFrom the owner:\nthe USB stick is not read\n"
          and "اختياري" in d["label"], d)
sys.exit(1 if failed else 0)
