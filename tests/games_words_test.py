#!/usr/bin/env python3
"""The family-games word data passes games/check_words.py and the question bank passes games/check.py."""
import os, subprocess, sys
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
for script in ("check.py", "check_words.py"):
    r = subprocess.run([sys.executable, "-I", os.path.join(ROOT, "games", script)], capture_output=True, text=True)
    assert r.returncode == 0, r.stdout + r.stderr
print("games_words_test ok")
