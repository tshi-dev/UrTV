# The English interface («اللغة»)

The owner lifted the "Arabic UI only" rule: the apps, the page and the phone page get an English interface. **Arabic stays the default
and does not change at all for Arabic users**: not a word, not a pixel, not a byte of the page link. English is opt-in, per TV, with the
«اللغة» setting. The plan is `notes/RESEARCH-NOV2b.md` §1 (branch `work/notes`); this file is how the work is split and done.

Phase 0 (this file's commit) is the plumbing only. Nothing a person sees changes: `lang en` switches to a table with no entries yet.

## What phase 0 gives every phase

| Piece | Where | What it does |
|---|---|---|
| the setting | `Prefs.lang()` / `setLang()` (screensaver) | `ar` (default) or `en`; anything else reads as `ar` (`Tr.norm`) |
| the computer | `optimize/saver.sh lang ar\|en` → `SaverReceiver` `--es lang` | checked before anything is saved; `saver.sh show` prints `lang:` |
| the page and the home | `&lang=en` in `Prefs.fragment()` (`Tr.fragment`) | **only for English**: with Arabic the fragment is exactly what it was |
| the home app | `ConfigSnapshot.lang()` → `S.remember()` (called from `Digits.init`/`Digits.remember`) | kept in the home's own prefs (`urtv_lang`) for screens opened alone |
| the phone | `POST /api/set {"lang":"en"}` (`Schema`, `PhoneActions.set`, `ConfigProvider` `phone_set`) and `"lang"` in `/api/state` | through the receiver's own checks |
| words in Java | `S.t("…")`, `S.n("…")`, `S.tv(value)` in both apps | Arabic: the text itself, the table never read. English: the entry |
| direction in Java | `Dir` in both apps | with Arabic the very constant each screen hard-codes today |
| a screen's locale | `S.wrap(base)` for `attachBaseContext` | Arabic: `base` itself. English: an English configuration |
| file names | `Tr.pageName(lang)` = `tv.html` / `tv-en.html`; `Tr.phonePageName(lang)` = `phone/index.html` / `phone/index.en.html` | the one place the names are written |
| the table | `i18n/en/<area>.tsv`, copied into both APKs as `assets/i18n/en/` (`copyI18n` in both `build.gradle`) | read by `S` the first time English needs it |
| the HTML hook | `i18n/tr.py`: `load()`, `translate(html, table)` | writes the English copy of a page; the Arabic file is never touched |
| the test | `tests/i18n_test.py` (+ `tests/TrRun.java`) | completeness, Arabic byte for byte, the roads above |

`Tr.java` is plain Java (no Android) and is the same file in both apps but its package; `Dir.java` too. The test compares them.

## The rules (every phase)

1. **Arabic is untouched.** Every helper returns, for Arabic, exactly what the code did before. A diff that changes what an Arabic user
   sees is a bug, whatever else it fixes. Defaults stay; no setting moves.
2. **Wrap, don't move.** The Arabic literal stays in the source (the ~7,200 test lines that pin Arabic keep passing):
   - `"حفظ"` → `S.t("حفظ")`
   - `"بقي " + n + " دقيقة"` → `S.t("بقي %s دقيقة", n)`: with Arabic this is byte for byte the concatenation (`String.valueOf` of each
     argument). `%s` takes the next argument, `%1$s`…`%9$s` one by number (English may reorder), `%%` is one `%`. A text with no
     arguments is never scanned for `%`, so `"٥٠%"` is safe.
   - `S.t(` and `S.n(` take **one plain literal** first; the test fails on `S.t(name)` or `S.t("أ" + b)`, because it could not see the
     text. A label in a constant array: write it `S.n("هندية ١٢٣")` (gives the text back, marks it for the table) and show it with
     `S.tv(label)`. Data (city, team, muezzin and station names) is not UI: it uses the English names we already have, never the table.
   - Digits: wrap first, convert after, as today: `Digits.of(S.t("بقي %s دقيقة", n))`.
3. **Direction.** Replace a hard-coded side with `Dir`, never with `Gravity.START/END` (those follow the view's own direction, which on
   the Arabic screens is not always RTL today):

   | today | write |
   |---|---|
   | `Gravity.RIGHT` | `Dir.start()` |
   | `Gravity.LEFT` | `Dir.end()` |
   | `View.LAYOUT_DIRECTION_RTL` | `Dir.layout()` |
   | `View.TEXT_DIRECTION_RTL` | `Dir.text()` |
   | `KEYCODE_DPAD_LEFT` meaning "next in reading order" | `Dir.forwardKey()` (and `Dir.backKey()`) |

   Each has a `(boolean rtl)` form for pure layout math (as `ControlLayout` is tested). Combined gravities keep their other half:
   `Gravity.RIGHT | Gravity.CENTER_VERTICAL` → `Dir.start() | Gravity.CENTER_VERTICAL`.
4. **Each Activity** of a converted screen gets
   `@Override protected void attachBaseContext(Context b) { super.attachBaseContext(S.wrap(b)); }`. A service or receiver that shows
   words calls `S.init(c)` first.
5. **No `values-en/`** (or any language folder) in `res/`: Android picks it by the TV's **system** language, and most of our Arabic
   homes have TVs set to English, so they would all flip. The test fails on one. An XML string is shown as `S.tv(getString(R.string.x))`
   and its English goes in the table like any other. App labels the system shows (the launcher's name, `app_names.xml`) stay Arabic.
6. **The table.** `i18n/en/<area>.tsv`, UTF-8, one `arabic<TAB>english` per line, `#` comment lines. In both fields `\t`, `\n`, `\\`,
   `\uXXXX` are the character (write a leading or trailing space as ` `). English `=` means "stays Arabic" (a verse, a club's name:
   for the HTML pages, see below). Both sides need the same placeholders; the English side has no Arabic letters. The same Arabic may
   be in two area files only with the same English (two phases may need «حفظ»: each adds it to its own file). Every entry must be used.
7. **The HTML pages: one file per language, generated.** The Arabic file stays the source and stays byte for byte what it is (tests read
   it directly). `i18n/tr.py`'s `translate(html, table)` returns the English copy: every **whole** Arabic literal (`'…'`, `"…"`, `` `…` ``
   on one line, no `${…}` inside) and every whole text node (`>…<`) that has an entry is replaced, and `<html lang="ar" dir="rtl">`
   becomes `lang="en" dir="ltr"`. It also returns the Arabic it found no entry for. A fragment such as `'بقي ' + n + ' دقيقة'` is three
   literals: restructure the code so the sentence is one literal (`'بقي %s دقيقة'.replace('%s', n)`) only where the Arabic output stays
   the same; the page's own tests are the judge. An entry a page uses must be safe in JS quotes and HTML: none of `' " `` ` `` < > &`, no
   backslash, no line break (`’` for an apostrophe). Once an area's table has its first entry, `tests/i18n_test.py` requires an entry for
   **every** Arabic literal `translate` finds in that file (`=` for the ones that stay Arabic): that is the "missing translation fails
   the build" for the pages.
8. **Never** edit `i18n/tr.py`, `Tr.java`, `S.java`, `Dir.java`, `tests/i18n_test.py` or this file in a phase: they are shared. If one
   needs a change, say so in the report (the supervisor makes it once, on `work/en`, and every phase rebases).

## The phases and the files each one owns

The phases run in parallel on top of `work/en`. A phase edits **only its own files** below, plus its own `i18n/en/<area>.tsv` and its own
line block in `tests/run.sh` (under its «English phase N» comment). If a phase needs a line in another phase's file, it reports it
instead.

| Phase | Area table | Owns |
|---|---|---|
| **1. Screensaver app** (~400 strings) | `i18n/en/saver.tsv` | `android/app/src/main/java/sa/urtv/screensaver/**` **except** `Updater.java`, `PageStamp.java` and `Prefs.pageBase()`; `android/app/src/main/res/**`; the visible «اللغة» row in `SettingsActivity` (العربية / English); `optimize/settings_move.py` (lang in export/import); `docs/SETTINGS.md` (the «اللغة» row, when the English screens are ready to offer) and the `lang` lines of `optimize/saver.sh`; «الأرقام» reading "Digits: 123 / ١٢٣" in English; new tests `tests/i18n_saver_*` |
| **2. Page** (`tv-en.html`) | `i18n/en/page.tsv` | `src/build.py` (writes `dist/tv-en.html` with `tr.translate`, beside `dist/tv.html`, which stays byte for byte); `dist/tv-en.html`; `src/template.html` (code restructuring only, the Arabic output unchanged); `tests/check.py` (its own 680 KB budget for `tv-en.html`); `Updater.java` and `PageStamp.java` (download `dist/` + `Tr.pageName(lang)` into `web/` + the same name; stamps per file); `Prefs.pageBase()` (`file:///android_asset/` + `Tr.pageName(lang)`, falling back to the Arabic file when the English one is missing); `copyPage` in `android/app/build.gradle` and `copyWeb` in `android/launcher/build.gradle` (also `tv-en.html`); `android/launcher/.../HomeCardChoice.java` (the home card's page); new tests `tests/i18n_page_*` and `tests/page.test.mjs` cases for the English file |
| **3. Home app** (~550 strings, 58 `Gravity.RIGHT`) | `i18n/en/home.tsv` | `android/launcher/src/main/java/sa/urtv/home/**` **except** `phone/**`, `setup/**`, `HomeCardChoice.java`, `S.java`, `Tr.java`, `Dir.java`; `android/launcher/src/main/res/**`; `android/launcher/src/main/assets/quran/**`; `tests/dpad_rtl_test.py` (the D-pad follows `Dir`); new tests `tests/i18n_home_*` |
| **4. Phone page** (`index.en.html`) | `i18n/en/phone.tsv` | a new generator `src/phone_en.py` (writes `android/launcher/src/main/assets/phone/index.en.html` with `tr.translate`; committed, and a test checks it is current); that file; `android/launcher/src/main/java/sa/urtv/home/phone/**` (`PhoneHost.page()` serves `Tr.phonePageName(S.lang())`, the Arabic file when the English one is missing; `UpdateText` and the other phone words); the «English / العربية» link, which goes in **only** through the generator (the Arabic `index.html` has 253 bytes left of its 64 KB); `tests/phone_page.test.mjs` cases for the English file; new tests `tests/i18n_phone_*` |
| 5. Setup on the TV (started: the done screen) | `i18n/en/setup.tsv` | `android/launcher/.../setup/**`, the «للشخص» rule in `docs/SETUP-PROMPT.md`. The done screen's own lines and the run's fixed end sentences are in; the start, plan and progress screens and the engine's lines are next. |

**Left for phase 1** (found in phase 2; the page is ready for both):
- [x] Digits: `Prefs.fragment()` always sends `&digits=` (`hindi` by default), so an English TV shows ١٢٣ on the page. With «اللغة» English and
  «الأرقام» never chosen, `Prefs.digits()` should default to `latin`. `tv-en.html` already reads a missing `#digits=` as Latin.
- [x] The city: the page shows `#city=` as it comes (the Arabic name). With «اللغة» English, `Prefs.fragment()` should send the city's English
  name (`Cities` world entries have one; check the Gulf entries that have only Arabic).

Shared, owned by none of the phases: `i18n/tr.py`, both `Tr.java`, `S.java`, `Dir.java`, `tests/i18n_test.py`, `tests/TrRun.java`,
`docs/ENGLISH.md`, `CHANGELOG.md`, `VERSION`. `Prefs.java` is shared between 1 (labels) and 2 (`pageBase()` only): keep to your part.

Content stays Arabic in every phase: Quran verses always; hadith and adhkar (no checked English source); the Hijri months are
transliterated (Muharram…); city and team names use the English names we have (`Cities` world entries, `teams-cache.json`).

## Commands

```sh
python3 tests/i18n_test.py            # the table, completeness, Arabic byte for byte, the roads (a few seconds; needs javac for the JVM part)
python3 i18n/tr.py                    # how many entries the tables have
bash tests/android_compile.sh         # both apps compile (javac against android-all; it fetches the jar once)
bash tests/run.sh                     # everything, before a push (about 15 minutes; the page tests need the fonts, which only CI has)
optimize/saver.sh lang en             # on a test TV: the setting (ar puts it back); `optimize/saver.sh show` prints lang:
```

To see what a page still lacks while a phase works on it:

```sh
python3 -I -c 'import sys; sys.path.insert(0, "i18n"); import tr; _, left = tr.translate(open("src/template.html", encoding="utf-8").read(), tr.load()); print("\n".join(left))'
```

Before a phase reports: `python3 tests/i18n_test.py` and `bash tests/run.sh` pass, the Arabic screens are unchanged (a screenshot in each
language for every converted screen, plan §1.4), and the diff touches only the phase's own files.
