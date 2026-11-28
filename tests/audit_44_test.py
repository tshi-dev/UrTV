#!/usr/bin/env python3
"""The owner's standard, «ما تخلي شيء مكرر ولا متكرر ولا صعب. كلش بسيط. كلش مضبوط. كلش حلو.» (docs/ui/SIMPLE.md): the fixes of the
one-pass audit of 4.4 (docs/ui/AUDIT-44.md), each pinned so it cannot come back. Run: python3 tests/audit_44_test.py"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
HOME = ROOT / "android/launcher/src/main/java/sa/urtv/home"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")

sa, games, hub = read(APP / "SettingsActivity.java"), read(APP / "GameActivity.java"), read(APP / "QuizGamesActivity.java")
main, card, choice, greet = read(HOME / "MainActivity.java"), read(HOME / "HomeCard.java"), read(HOME / "HomeCardChoice.java"), read(HOME / "Greeting.java")
til, page, tpl = read(HOME / "tilawah/MainActivity.java"), read(ROOT / "android/launcher/src/main/assets/phone/index.html"), read(ROOT / "src/template.html")

# ---- settings
check("settings: a row's name is whole (it takes what it needs, up to 60 % of the pane) and the value wraps beside it in the rest",
      "texts.addView(name, new LinearLayout.LayoutParams(-2, -2));" in sa and "name.setMaxWidth(Math.round((pageWidth - sideWidth) * 0.6f));" in sa
      and "val.setMaxWidth(" not in sa and "LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(0, -2, 1f);" in sa)
check("settings: no hint over the first row on every screen", "hint.setVisibility(View.GONE);" in sa and 'hint.setText(Words.of("اختيار يفتح' not in sa)
check("settings: the buttons hub has one title", 'title.setText(Words.of(screen == null ? "الإعدادات" : SettingsHub.name(screen)));' in sa and 'Words.of("تلفزيونك")' not in sa)
check("settings: no photos is said once (by «صوري»), not again by «احذف صورة»", 'row("احذف صورة", Own.count(this) == 0 ? null : "اختر من صورك",' in sa)
check("settings: the city's note on the categories only, not over the city's own row in «اللغة والموقع»",
      "if (screen == null && prefs.cityNoteDue()) line(Location.note(" in sa and "if (screen == null || on(SettingsHub.LANG))" not in sa)
rows = re.findall(r'row\("([^"]+)", ([^;]*?), R\.drawable', sa)
values = [v for _, x in rows for v in re.findall(r'"([^"]*)"', x)]
mixed = [v for v in values if re.match(r"(يعمل|تعمل|مفعّلة|متوقفة|مطفأ)(\s|،|$)", v)]
check("settings: on and off are «مفعّل» and «متوقف» on every row (not يعمل, تعمل, مفعّلة, متوقفة, مطفأ); «ظاهر/مخفي» stays for what is shown on the screen",
      not mixed, mixed[:5])
def words_after_state(v):
    v = re.sub(r"^(مفعّل|متوقف|ظاهرة?|مخفية?)[،,]?\s*", "", v)
    return len(v.split())
for name in ("تنبيه قبل الأذان", "الشروق بعد الفجر", "تعتيم الليل", "النسخ التجريبية", "المناسبة القادمة", "الأذان", "الجوالات المسموح لها",
             "بطاقة المباريات لليوم فقط", "شريط السنة تحت الساعة"):
    lines = [v for n, x in rows if n == name for v in re.findall(r'"([^"]*)"', x)]
    long = [v for v in lines if words_after_state(v) > 8]
    check(f"settings: «{name}»'s second line is at most 8 words after its state", lines and not long, long or "row not found")
check("settings: «النسخ التجريبية» says its state and, in both, that beta updates come first with small bugs sometimes (said before it is turned on)",
      'prefs.betaOn() ? "مفعّل، تجيك التحديثات قبل الكل، وفيها أخطاء أحيانًا" : "متوقف، تحديثات قبل الكل، وفيها أخطاء أحيانًا"' in sa)

# ---- the games
lob = games[games.index("protected final void drawPhoneLobby"):]
check("games: the room's code once, in Latin digits", lob.count('centered("\\u2066" + phones.code + "\\u2069", 120') == 1 and "latinBox" not in lob)
check("games: the «الكل» header is true (one remote, but for the games marked «جوال»), and only those are marked",
      'S.t("ريموت واحد يدور بينكم، إلا اللي عليها «جوال»")' in hub and "بدون جوالات" not in hub
      and 'filter == 0 && GameRegistry.needsPhones(g.id)' in hub and 'S.t("ريموت")' not in hub)

# ---- تلاوة وأذان
check("تلاوة: the adhan switch says on or off; the time left is on the next prayer's row only",
      "toggle += sa.urtv.home.S.sep()" not in til and "if (isNext) detail += sa.urtv.home.S.sep() + PrayerTimes.remaining(" in til)
check("تلاوة: the surah grid has its own hint (no «الأقسام» on a screen without them)",
      'android:text="@string/hint_surah"' in read(ROOT / "android/launcher/src/main/res/layout/activity_surah.xml")
      and '<string name="hint_surah">اختيار يشغّل السورة، رجوع للقرّاء</string>' in read(ROOT / "android/launcher/src/main/res/values/strings.xml"))

# ---- the phone page
check("phone page: the login says the one thing to do", '<p id="loginInfo">اكتب الرمز الظاهر على التلفزيون.</p>' in page)
check("phone page: one way to the settings (the bottom bar), no gear in the header", 'id="hSet"' not in page and "$('hSet')" not in page)
check("phone page: the adhan's switch on one tab («الإعدادات»), «يعمل الآن» says the next adhan",
      'id="aOn2"' not in page and "$('aOn2')" not in page and page.count('id="aOn"') == 1 and 'id="nextA"' in page)

check("phone page P7 (the owner): the numbers it shows in Arabic-Indic digits as on the TV, on the Arabic page only; what is typed (the login code, any field) and [data-latin] stay",
      "function indic(r) { if (document.documentElement.lang !== 'ar' || !r) return;" in page and "e.closest('input,textarea,script,[data-latin]')" in page
      and "String.fromCharCode(1632 + +d)" in page and ".observe(a, { childList: true, subtree: true, characterData: true }); indic(a);" in page)
check("phone page P2: the update card says it in one line, no signatures and no Android installer", "<p>ننزّل التحديث ونتأكد منه، وأنت تثبّته بالريموت.</p>" in page and "موقّعة بنفس توقيع" not in page)

# ---- the home
check("home H1: the football widgets start under the clock column's lowest drawn line, and again when the zone line comes or goes",
      "int column = clockBlock.getTop() + (clockBlock instanceof ViewGroup ? shownBottom((ViewGroup) clockBlock) : clockBlock.getHeight());" in main
      and "if (zoneWas != other && homeCard != null) handler.post(homeCard::fit);" in main)
check("home H2: under a tall header the football weather card's padding is less, so its last row stays inside it",
      "document.documentElement.classList.toggle('fb-short', FB.top > 280);" in tpl and "html.fb-short #fb-wx .fbc{padding-block:calc(var(--u)*10)}" in tpl)
check("home H11: the focused football widget grows with a shadow, no ring",
      ".fbw.f .fbc{transform:scale(1.03);" in tpl and "inset 0 0 0 calc(var(--u)*3) var(--fring),0 18px 36px" not in tpl.split(".fbw.f .fbc{")[1].split("}")[0])
check("home H5: with today's prayer times on the screen (a prayer card, or the football home), «الغروب» is not said beside «المغرب»",
      "static boolean hasPrayerTimes(String[] choice)" in choice and '"prayers".equals(k) || "prayerrow".equals(k) || "nextprayer".equals(k)' in choice
      and "prayerTimes = football || HomeCardChoice.hasPrayerTimes(choice);" in card
      and 'if (!("الغروب".equals(d.sunLabel) && prayersShown.getAsBoolean())) line += ' in greet
      and "greetingLine.prayersShown(() -> homeCard != null && homeCard.prayerTimes());" in main)

raise SystemExit(1 if failed else 0)
