#!/usr/bin/env python3
"""The home screen's «المهام» card as a button (HomeCard.java, MainActivity.java, TasksActivity.java, tests/page.test.mjs for the page):
the focus and OK are the Android side's, the other cards stay unfocusable, the list opens on the mic. Source checks, no TV needed.
Run: python3 tests/tasks_card_test.py"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
card = (HOME / "HomeCard.java").read_text(encoding="utf-8")
main = (HOME / "MainActivity.java").read_text(encoding="utf-8")
tasks = (HOME / "TasksActivity.java").read_text(encoding="utf-8")
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else f"  -> {detail}"))
    failed += not ok

check("HomeCard: still one WebView, never focusable itself, nothing inside it takes the focus",
      card.count("new WebView(") == 1 and "web.setFocusable(false)" in card and "FOCUS_BLOCK_DESCENDANTS" in card)
check("HomeCard: the WebView keeps setTextZoom(100)", "setTextZoom(100)" in card)
check("HomeCard: the focus target exists only when «المهام» is among the cards, and goes with the page",
      "if (tasks) addTarget();" in card and "parent.removeView(target)" in card and "tasks = HomeCardChoice.hasTasks(choice)" in card)
check("HomeCard: the target is a plain focusable View with the page's ring switched by setLiveFocus",
      "target.setFocusable(true)" in card and 'setLiveFocus(" + has + ")"' in card and "target.setOnClickListener(v -> live.open())" in card)
check("HomeCard: the target is moved over the card by what the page says (liveRect, in dp), not by a guess",
      "liveRect" in card and "getDisplayMetrics().density" in card)
check("HomeCard: the page asks for nothing new (no network, no JavaScript interface)", "addJavascriptInterface" not in card)
check("MainActivity: OK on the card opens the list with EXTRA_MIC", "putExtra(TasksActivity.EXTRA_MIC, true)" in main)
check("MainActivity: the card sits between the pills and the dock: pills down -> card, card down -> dock, dock up -> card, card up -> the pill it came from",
      "p.setNextFocusDownId(card != null ? card.getId()" in main and "t.setNextFocusDownId(dockId" in main
      and "int topId = card != null ? card.getId() : pills.get(0).getId();" in main and "card.setNextFocusUpId(v.getId())" in main)
check("MainActivity: with the stocks card a button too, the targets go in the cards' order, one to the next with ◀▶ (the reading order)",
      "homeCard.targets()" in main and "if (Dir.rtl()) { t.setNextFocusLeftId(next.getId()); t.setNextFocusRightId(prev.getId()); }" in main
      and "for (View card : homeCard.targets()) card.setNextFocusUpId(v.getId());" in main)
check("MainActivity: the order is re-wired when the settings bring the card or take it away", "wireFocus();   // the «المهام» card may have come or gone" in main)
check("TasksActivity: from the card the first render focuses «🎤 تكلّم», later ones the list as before",
      'EXTRA_MIC = "mic"' in tasks and "if (micFirst && mic.getVisibility() == View.VISIBLE) f = mic;" in tasks and "micFirst = false;" in tasks)
check("TasksActivity: the mic still starts the voice adding on OK", 'mic = pill(S.t("🎤 تكلّم"), GOLD, v -> listen());' in tasks)
check("the «المهام» pill still opens the list as it did (no mic-first)", 'tasks.setOnClickListener(v -> launchGuarded(ParentalPolicy.PILL_TASKS, new Intent(this, TasksActivity.class), "المهام"))' in main)   # through the family lock; still no mic extra
sys.exit(1 if failed else 0)
