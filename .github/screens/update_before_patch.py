#!/usr/bin/env python3
"""Gives the master tree's UpdateActivity the same picture hook as this branch's (a debug build draws the state named by the extra "preview"), so the «before» pictures
show master's own layout in the same states. Only the status source and the debug receiver change; nothing of master's drawing does.
    update_before_patch.py MASTER_TREE THIS_TREE"""
import shutil, sys
from pathlib import Path

master, mine = Path(sys.argv[1]), Path(sys.argv[2])
home = "android/launcher/src/main/java/sa/urtv/home"
act = master / home / "UpdateActivity.java"
s = act.read_text(encoding="utf-8")
assert "Map<String, Object> st = PhoneUpdater.status();" in s and "super.onCreate(b);" in s
s = s.replace("super.onCreate(b);", 'super.onCreate(b);\n        if (BuildConfig.DEBUG) preview = getIntent().getStringExtra("preview");', 1)
s = s.replace("Map<String, Object> st = PhoneUpdater.status();", "Map<String, Object> st = previewStatus();", 1)
s = s.replace("    private LinearLayout col;", '''    private String preview;
    private Map<String, Object> previewStatus() {
        if (preview == null) return PhoneUpdater.status();
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("status", preview);
        m.put("build", 1L);
        return m;
    }
    private LinearLayout col;''', 1)
s = s.replace("if (PhoneUpdater.confirmKnown()) {", "if (preview != null || PhoneUpdater.confirmKnown()) {", 1)
act.write_text(s, encoding="utf-8")
shutil.copytree(mine / "android/launcher/src/debug", master / "android/launcher/src/debug", dirs_exist_ok=True)
print("patched", act)
