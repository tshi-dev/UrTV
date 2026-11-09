#!/usr/bin/env python3
"""games/check.py accepts a good item and rejects the broken ones."""
import json, os, subprocess, sys, tempfile
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
def item(**kw):
    d = {"id": "q1", "type": "quiz", "category": "geography", "difficulty": 1, "age": "all",
         "question": {"ar": "ما عاصمة مصر؟", "en": "What is the capital of Egypt?"},
         "answer": {"ar": "القاهرة", "en": "Cairo"},
         "wrong": [{"ar": "الإسكندرية", "en": "Alexandria"}, {"ar": "أسوان", "en": "Aswan"}, {"ar": "الأقصر", "en": "Luxor"}],
         "source": ["https://example.org/a"], "verified": True}
    d.update(kw); return d
def run(items):
    with tempfile.TemporaryDirectory() as t:
        json.dump(items, open(os.path.join(t, "quiz.json"), "w", encoding="utf-8"), ensure_ascii=False)
        return subprocess.run([sys.executable, "-I", os.path.join(ROOT, "games", "check.py"), t], capture_output=True).returncode
assert run([item()]) == 0
assert run([item(verified=False)]) == 1
assert run([item(), item()]) == 1
assert run([item(wrong=[{"ar": "القاهرة", "en": "Cairo"}] + item()["wrong"][:2])]) == 1
assert run([item(question={"ar": "س" * 91, "en": "x"})]) == 1
assert run([item(answer={"ar": "ا" * 29, "en": "x"})]) == 1
assert run([item(question={"ar": "هل · هذا", "en": "x"})]) == 1
print("games_check_test ok")
