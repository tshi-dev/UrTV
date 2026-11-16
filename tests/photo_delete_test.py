#!/usr/bin/env python3
"""4.4 «صوري»: delete ONE photo, from the phone page or the TV. OwnPick (run for real): a photo is picked by its number AND its file's time, so a
pick whose photo moved or went is refused, never applied to its neighbour. The source checks: the screensaver app's provider lists, shows small and
deletes behind the phone-only permission; the delete takes the mask with it and refreshes the album and the rotation at once (bumpOwnRev); the
launcher's /api/photos, /api/photo_thumb/<pick> and /api/photo_del go through the paired phone's session; the /u page shows the photos with «حذف»
and a confirmation; the TV's «صوري» has «اختر صورة لحذفها» (a grid, then «حذف هذه الصورة؟») and keeps «حذف كل صوري»; the settings search finds
«صوري» and the hidden apps by their aliases (one table for both layouts).   python3 tests/photo_delete_test.py"""
import re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "android/app/src/main/java/sa/urtv/screensaver"
P = ROOT / "android/launcher/src/main/java/sa/urtv/home/phone"
failed = 0
def check(name, ok, detail=""):
    global failed
    print(("PASS " if ok else "FAIL ") + name + ("" if ok else "  -> " + str(detail)))
    failed += not ok
def read(p): return Path(p).read_text(encoding="utf-8")

own, prov, sett, hub = read(APP / "Own.java"), read(APP / "ConfigProvider.java"), read(APP / "SettingsActivity.java"), read(APP / "SettingsHub.java")
api, schema, acts = read(P / "PhoneApi.java"), read(P / "Schema.java"), read(P / "PhoneActions.java")
page = read(ROOT / "android/launcher/src/main/assets/phone/photos.html")

check("OwnPick is plain Java", "import android" not in read(APP / "OwnPick.java"))
dp = own[own.index("static synchronized boolean deletePick"):]
dp = dp[:dp.index("\n    }\n")]
check("a delete by pick checks the photo is still the same one, then deletes through delete()", "p.same(count(c), stamp(c, p.n)) && delete(c, p.n)" in dp)
de = own[own.index("static synchronized boolean delete(Context c, int n)"):]
de = de[:de.index("\n    }\n")]
check("delete() takes the mask with the photo, moves the ones after up (masks too), and bumps the album's revision",
      "mask(c, n).delete();" in de and "mask(c, k).renameTo(mask(c, k - 1))" in de and "new Prefs(c).bumpOwnRev();" in de)
check("the provider: list, small picture, delete, all behind the phone-only permission (the «phone_» calls)",
      all(f'case "{m}"' in prov for m in ("phone_photo_list", "phone_photo_thumb", "phone_photo_del"))
      and prov.index('throw new SecurityException("phone control only")') < prov.index('case "phone_photo_list"'))
check("a small picture only for a pick that still names its photo", "if (pick != null && pick.same(Own.count(c), Own.stamp(c, pick.n)))" in prov)
check("the launcher: GET /api/photos and /api/photo_thumb/<pick>, POST /api/photo_del {id}, all needing the session",
      'case "/api/photos": return ok(control.actions().photos());' in api and "if (r.path.startsWith(THUMB_PATH)) return photoThumb(" in api
      and 'case "/api/photo_del": endpoint = "photodel"; break;' in api and '"/api/photos", "/api/photo_thumb/1:1", "/api/photo_del"' in api
      and api.index('String secret = session(r);\n        if (secret == null) return json(401') < api.index('case "/api/photos"'))
check("the pick is allow-listed on both sides (n:stamp)", 'only(body, "id")' in schema and '"[1-9][0-9]{0,2}:[0-9]{1,14}"' in schema
      and '"[1-9][0-9]{0,2}:[0-9]{1,14}"' in read(APP / "OwnPick.java") and "if (!Schema.photoPick(id)) return json(404" in api)
check("the launcher asks the screensaver app's provider (PHONE_SETTINGS) for each", all(f'saver("{m}"' in acts for m in ("phone_photo_list", "phone_photo_thumb", "phone_photo_del")))
check("the /u page: the photos on the TV with «حذف», a confirmation, then the list again; the count says n of max",
      "fetch('/api/photos'" in page and "'/api/photo_del'" in page and "confirm(W.ask)" in page and "W.onTv.replace('{n}', ids.length)" in page
      and "'X-UrTV-Session'" in page and "readAsDataURL" in page)
mine = page[page.index("// 4.4: the photos on the TV"):page.index("  $('pick').onchange = function () {")]
check("the new list builds no HTML from data (elements and textContent only), and the page stays one nonce'd script",
      not re.search(r"innerHTML|outerHTML|insertAdjacentHTML|document\.write|\beval\(", mine) and "createElement('img')" in mine and page.count("<script") == 1)
check("the TV: «اختر صورة لحذفها» beside «حذف كل صوري», a grid, then «حذف هذه الصورة؟» with نعم",
      '"اختر صورة لحذفها", "حذف كل صوري"' in sett and "pickOwnToDelete()" in sett and 'd.setTitle("حذف هذه الصورة؟");' in sett
      and 'd.setPositiveButton("نعم"' in sett and "Own.deletePick(this, OwnPick.parse(pick))" in sett and "confirmClearOwn()" in sett)
check("the TV's grid makes its small pictures off the screen's thread", '"own-thumbs").start();' in sett)
for word, row in (("حذف الصور", "صوري"), ("حذف صوره", "صوري"), ("صوري", "صوري"), ("المخفيه", "ترتيب كل التطبيقات"),
                  ("اظهار التطبيقات المخفيه", "ترتيب كل التطبيقات"), ("التطبيقات المخفيه", "ترتيب كل التطبيقات")):
    check(f"search: «{word}» finds «{row}»", '{"%s", "%s"}' % (word, row) in hub)
check("one alias table and one search for both layouts (the 4.3.2 screen and the side list), and the rows are in both",
      hub.count("static final String[][] ALIASES") == 1 and "private static String[][] order() { return classic ? ORDER_432 : ORDER; }" in hub
      and all(f'"{r}"' in hub[hub.index(f"static final String[][] {o} = "):hub.index("};", hub.index(f"static final String[][] {o} = "))]
              for o in ("ORDER", "ORDER_432") for r in ("صوري", "ترتيب كل التطبيقات")))
if shutil.which("javac"):
    run = """package sa.urtv.screensaver;
public class PickRun { static void out(String n, boolean ok) { System.out.println(n + "\\t" + (ok ? "ok" : "bad")); }
public static void main(String[] a) {
  OwnPick p = OwnPick.parse(OwnPick.id(3, 1763300000000L));
  out("the pick goes and comes back", p != null && p.n == 3 && p.stamp == 1763300000000L);
  out("it names the same photo while its number and time are the same", p.same(5, 1763300000000L));
  out("a photo before it was deleted (it moved down, another one has its number): refused", !p.same(5, 1763300999999L));
  out("its number is past the end now: refused", !p.same(2, 1763300000000L));
  out("not ours: refused", OwnPick.parse(null) == null && OwnPick.parse("0:1") == null && OwnPick.parse("3") == null && OwnPick.parse("3:x") == null
      && OwnPick.parse("1000:1") == null && OwnPick.parse("3:123456789012345") == null && OwnPick.parse("../1:1") == null);
} }"""
    with tempfile.TemporaryDirectory() as tmp:
        Path(tmp, "PickRun.java").write_text(run)
        r = subprocess.run(["javac", "-d", tmp, str(APP / "OwnPick.java"), str(Path(tmp, "PickRun.java"))], capture_output=True, text=True)
        check("OwnPick compiles on its own (plain Java)", r.returncode == 0, r.stderr)
        if r.returncode == 0:
            out = subprocess.run(["java", "-cp", tmp, "sa.urtv.screensaver.PickRun"], capture_output=True, text=True).stdout
            for line in out.splitlines():
                name, res = line.rsplit("\t", 1)
                check(name, res == "ok")
            check("every case ran", len(out.splitlines()) == 5, len(out.splitlines()))
sys.exit(1 if failed else 0)
