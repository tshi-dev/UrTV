"""A small, conservative call graph over the apps' Java, for the UI-thread rules (tests/regressions/ui_thread_ipc_test.py) and the
release's API surface diff (.github/tools/api_surface_diff.py). No Java parser: comments and strings are blanked, methods are found by
their header and matching brace, and calls are resolved by name:

- `Foo.bar(` where Foo is one of our classes: Foo's bar;
- `bar(` or `this.bar(`: bar in the same file;
- `x.bar(` where x is declared in the same file with one of our types (`HomeCard homeCard`): that type's bar;
- anything else (a getter's result, an Android object) is not followed.

Code handed to another thread (`new Thread(...)`, `x.execute(...)`, `x.submit(...)`, `x.schedule*(...)`) is not on the UI thread and is
left out, except the parts of it posted back (`runOnUiThread(...)`, `x.post(...)`, `x.postDelayed(...)`), which are kept.

Entry points are the methods Android calls on the UI thread for a key, a return to the screen or the minute tick (ENTRY). Sinks are the
calls that cross a process or touch the disk (SINKS). `reach()` gives, for each entry, every sink it can reach with one path.
"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
# every app under android/ (the screensaver's folder is app/, called «saver» here); a new app (the extras) joins by itself
SRC = {("saver" if d.name == "app" else d.name): d / "src/main/java" for d in sorted((ROOT / "android").iterdir())
       if (d / "src/main/java").is_dir()}

ENTRY = ("onResume", "onStart", "onRestart", "onWindowFocusChanged", "onKeyDown", "onKeyUp", "onKeyLongPress", "dispatchKeyEvent",
         "onKey", "onClick", "onFocusChange", "onReceive")

# name -> regex on blanked code. IPC: a call into another app's process (the screensaver's provider, the home's, Android's TV provider).
SINKS = {
    "ipc:ContentResolver.query": r"\bgetContentResolver\(\)\s*\.\s*query\s*\(|\bresolver\s*\.\s*query\s*\(|\bcr\s*\.\s*query\s*\(",
    "ipc:ContentResolver.call": r"\bgetContentResolver\(\)\s*\.\s*call\s*\(",
    "ipc:ContentResolver.write": r"\bgetContentResolver\(\)\s*\.\s*(insert|update|delete|bulkInsert|openInputStream|openOutputStream|openFileDescriptor)\s*\(",
    "ipc:ConfigSnapshot.read": r"\bConfigSnapshot\s*\.\s*read\s*\(",
    "ipc:UpdateChannel.read": r"\bUpdateChannel\s*\.\s*(read|beta)\s*\(",
    "disk:stream": r"\bnew\s+(FileInputStream|FileOutputStream|FileReader|FileWriter|RandomAccessFile)\s*\(",
    "disk:Files": r"\bFiles\s*\.\s*(read\w*|write\w*|newBuffered\w*|newInput\w*|newOutput\w*|copy|move|lines)\s*\(",
    "disk:openFile": r"\bopenFile(Input|Output)\s*\(",
    "disk:prefs.commit": r"\.\s*commit\s*\(\s*\)",
    "disk:decodeFile": r"\bBitmapFactory\s*\.\s*decode(File|Stream)\s*\(",
}


_TOKEN = re.compile(r'//[^\n]*|/\*.*?\*/|"""(?:.|\n)*?"""|"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])*\'', re.S)


def blank(src):
    """comments and string/char literals turned to spaces (lengths and newlines kept)"""
    return _TOKEN.sub(lambda m: re.sub(r"[^\n]", " ", m.group(0)), src)


def match_paren(s, i):
    """s[i] is an opening bracket: the index just past its match"""
    pair = {"(": ")", "{": "}", "[": "]"}
    o, c = s[i], pair[s[i]]
    depth = 0
    for j in range(i, len(s)):
        ch = s[j]
        if ch == o:
            depth += 1
        elif ch == c:
            depth -= 1
            if depth == 0:
                return j + 1
    return len(s)


# an executor is a field or a call's result (`clockReads.execute(`, `Executors.newX().submit(`), never one of our classes
# (`AdhanScheduler.schedule(c, p)` is a plain call)
_OFF = re.compile(r"\bnew\s+Thread\s*\(|(?:\b[a-z_]\w*|\))\s*\.\s*(execute|submit|schedule|scheduleAtFixedRate|scheduleWithFixedDelay)\s*\(")
_BACK = re.compile(r"\brunOnUiThread\s*\(|\.\s*(post|postDelayed|postAtFrontOfQueue)\s*\(")


def off_ui(text):
    """the code handed to another thread blanked, except what it posts back to the UI thread"""
    drop = bytearray(len(text))
    for m in _OFF.finditer(text):
        start = m.end() - 1
        end = match_paren(text, start)
        k = end   # `new Thread() { public void run() {...} }`: the anonymous body goes too
        while k < len(text) and text[k] in " \t\n":
            k += 1
        if m.group(0).lstrip().startswith("new") and k < len(text) and text[k] == "{":
            end = match_paren(text, k)
        for j in range(start + 1, end - 1):
            drop[j] = 1
        for b in _BACK.finditer(text, start, end):
            bs = b.end() - 1
            for j in range(bs, match_paren(text, bs)):
                drop[j] = 0
    if not any(drop):
        return text
    return "".join(" " if d and ch != "\n" else ch for ch, d in zip(text, drop))


_NAME = re.compile(r"\b([A-Za-z_]\w*)\s*\(")
_AFTER = re.compile(r"\s*(?:throws\s+[\w.,\s]+?)?\s*\{")
_BEFORE = re.compile(r"([\w>\]])\s+$")
_PREV = re.compile(r"(\w+)\s*$")


def arity(args):
    """how many arguments (or parameters) a bracket's text holds, counting only its top-level commas"""
    if not args.strip():
        return 0
    depth, n = 0, 1
    for ch in args:
        if ch in "([{<":
            depth += 1
        elif ch in ")]}>":
            depth -= 1
        elif ch == "," and depth == 0:
            n += 1
    return n


def _heads(s):
    """(name, start of name, index of the body's brace) for every method or constructor header"""
    for m in _NAME.finditer(s):
        name = m.group(1)
        if name in _NOT:
            continue
        close = match_paren(s, m.end() - 1)
        a = _AFTER.match(s, close)
        if not a:
            continue
        pre = s[max(0, m.start() - 80):m.start()]
        if not _BEFORE.search(pre):
            continue
        w = _PREV.search(pre.rstrip())
        if w and w.group(1) in _NOT:
            continue
        yield name, m.start(1), a.end() - 1, arity(s[m.end():close - 1])


# a file whose class is a screen (an Activity, a View, a Dialog): its ENTRY methods and its own receivers run on the UI thread while the
# person holds the remote. A receiver of its own declared in the manifest is not a key's latency and is not an entry.
_UI = re.compile(r"\bextends\s+[\w.]*(Activity|View|Layout|Dialog|DreamService)\b")
_NOT = {"if", "for", "while", "switch", "catch", "synchronized", "return", "new", "else", "do", "try", "throw", "case"}
_CLASS = re.compile(r"\b(?:class|interface|enum|record)\s+(\w+)")


class Method:
    __slots__ = ("cls", "name", "file", "line", "body", "sinks", "calls", "ui", "arity")

    def __init__(self, cls, name, file, line, body):
        self.cls, self.name, self.file, self.line, self.body = cls, name, file, line, body
        self.sinks, self.calls, self.ui, self.arity = [], [], False, -1

    @property
    def key(self):
        return f"{self.cls}.{self.name}"


def methods_of(path, rel):
    raw = path.read_text(encoding="utf-8")
    s = off_ui(blank(raw))
    top = _CLASS.search(s)
    cls = top.group(1) if top else path.stem
    out = []
    for name, at, b0, n in _heads(s):
        b1 = match_paren(s, b0)
        line = s.count("\n", 0, at) + 1
        out.append(Method(cls, name, rel, line, s[b0:b1]))
        out[-1].arity = n
    ui = bool(_UI.search(s))
    for m in out:
        m.ui = ui
    types = {v: t for t, v in _DECL.findall(s)}
    return cls, out, types


# `HomeCard homeCard`, `final ReminderCard reminder =`, `(MusicService svc)`: a name's declared type, per file
_DECL = re.compile(r"\b([A-Z]\w*)(?:<[^<>]*>)?\s+([a-z_]\w*)\s*[=;,)]")
_CALL = re.compile(r"(?:\b([A-Za-z_]\w*)\s*\.\s*)?\b([a-z_]\w*)\s*\(")


def load(apps=tuple(SRC), root=None):
    """every method of the given apps, with its direct sinks and its resolved calls"""
    base = Path(root) if root else ROOT
    allm, byname, byclass, typesof = [], {}, {}, {}
    for app in apps:
        src = base / SRC[app].relative_to(ROOT)
        if not src.exists():
            continue
        for p in sorted(src.rglob("*.java")):
            rel = str(p.relative_to(base))
            cls, ms, types = methods_of(p, rel)
            typesof[rel] = types
            byclass.setdefault(cls, [])
            for m in ms:
                allm.append(m)
                byname.setdefault(m.name, []).append(m)
                byclass[cls].append(m)
    for m in allm:
        for k, rx in SINKS.items():
            if re.search(rx, m.body):
                m.sinks.append(k)
        own = {x.name: x for x in byclass.get(m.cls, []) if x.file == m.file}
        seen = set()
        for c in _CALL.finditer(m.body):
            q, n = c.group(1), c.group(2)
            if n in _NOT:
                continue
            targets = []
            if q and q in byclass:
                targets = [x for x in byclass[q] if x.name == n]
            elif q is None or q == "this":
                targets = [x for x in byname.get(n, []) if x.file == m.file]
            elif q in typesof[m.file] and typesof[m.file][q] in byclass:
                targets = [x for x in byclass[typesof[m.file][q]] if x.name == n]
            if len(targets) > 1:   # overloads: the ones taking as many arguments as the call gives (Place.get(c) is not Place.get(c, f, t))
                n_args = arity(m.body[c.end():match_paren(m.body, c.end() - 1) - 1])
                same = [t for t in targets if t.arity == n_args]
                targets = same or targets
            for t in targets:
                if t is not m and id(t) not in seen:
                    seen.add(id(t))
                    m.calls.append(t)
    return allm


def reach(allm, entries=ENTRY):
    """[(entry, sink kind, path as "A.f -> B.g -> ...")] for every sink an entry reaches (one shortest path each)"""
    found = []
    for e in allm:
        if e.name not in entries or not e.ui:
            continue
        prev = {id(e): None}
        order = [e]
        i = 0
        while i < len(order):
            m = order[i]
            i += 1
            for t in m.calls:
                if id(t) not in prev:
                    prev[id(t)] = m
                    order.append(t)
        done = set()
        for m in order:
            for k in m.sinks:
                if (m.key, k) in done:
                    continue
                done.add((m.key, k))
                path, x = [], m
                while x is not None:
                    path.append(x.key)
                    x = prev[id(x)]
                found.append((e, k, " -> ".join(reversed(path))))
    return found
