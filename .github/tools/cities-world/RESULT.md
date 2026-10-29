# Worldwide cities — implementation result

Completed Part A before Part B on `work/cities-world`. Base: `f57f132efe93986d5abaee7567b613fee28393ce`. No pull request, other-branch push, TV installation or private-font commit.

## Data review and resulting app

- Reviewed all 3,305 supplied Arabic/English pairs in country `order`, then audited capitals. Corrected **119 existing names**, added **2 missing capitals** from GeoNames records, and retained coordinates, time zones and GeoNames IDs for every existing row. All spelling decisions are reproducible through `overrides.json` and `merge.py`.
- World data: **3,307 cities / 230 countries and territories / 231 capital rows**. Sri Lanka retains Colombo as a commercial capital alongside Sri Jayewardenepura Kotte. Palau's Ngerulmud comes from a credited raw GeoNames record. Six previously unflagged administrative centres are recovered without treating historical/metropolitan aliases as extra capitals.
- The original **374 city rows / 14 countries / 26 regions** remain unchanged. App total: **3,681 cities / 244 countries and territories / 256 regions**. The original rows' canonical SHA-256 is `c48a06f40efe53347b707158686f75679534067a45d0ac70799cfee2a46ccd89`.
- World picker labels contain both names. CLI matching accepts Arabic, English and country-qualified names such as `GB|London`; existing short Arabic selections keep their previous meaning. Country is retained in preferences, geocoding results and settings export/import/undo, so shared time zones cannot select another country's method.
- Search accepts every valid two-letter country code except Palestine's deliberately preserved checked-list exception. Palestinian city zones remain Hebron/Gaza as before.

## Validation

`bash tests/run.sh` completed with exit **0** before the push: **148 test-program invocations** (141 Python, 6 Node, 1 shell compilation runner), **9,631 printed PASS checks**, **0 FAIL**, **0 SKIP**. Printed-line counts describe the suite's reported checks; larger input-case counts are listed below. The main browser suite also preserved the large-clock reference pixel for pixel (zero differing pixels).

After the implementation commit, the 14-digit source build stamp was refreshed and `tests/check.py` / `tests/page_stamp_test.py` were rerun successfully. The stamp refresh changes no page behavior.

- All **6,614** Arabic/English world-name queries find their intended decoded app row inside the requested country; all **244** app countries produce a numeric method fragment and an Arabic authority label.
- **60 independently published Aladhan city-days** cover **20 cities**, three seasons (20 March, 21 June and 21 December 2026) and all ten new methods. Both the Java launcher math and the actual browser page match all six times within **2 minutes**, with worst difference **2 minutes**. Each fixture retains its request URL, published timings and metadata; tests run offline against these responses.
- Cases include London/Oslo summer twilight, Moscow, Ottawa, Reykjavik, Nuuk, Samoa's date line, tropical Moonsighting countries, and Iran using MWL. Ordered polar-day/night fallback and the previous night's Isha after midnight are separately checked. Existing 5,276-row prayer and 1,418-day Hijri crosschecks remain in the full suite; see test output for their exact results.
- Both Android release variants build successfully with Gradle 8.9, JDK 17 and SDK 34. Size comparisons use identical private assets, local debug signing and version code 1 for both revisions. No APK or private font is staged. The pre-existing launcher's target-SDK lint diagnostic is still printed by its non-aborting lint configuration; no target-SDK policy was changed.

## Sizes (bytes)

| Artifact | Before | After | Change |
|---|---:|---:|---:|
| `dist/tv.html` | 679,917 | 675,328 | −4,589 |
| Screensaver release APK | 3,265,569 | 3,354,566 | +88,997 |
| Launcher release APK | 3,303,973 | 3,306,893 | +2,920 |

The HTML limit remains **680,000 bytes**, leaving **4,672 bytes**. Compact JSON separators reclaim space without removing content. No per-country table is embedded in the page: the app sends the resolved method id through its existing settings fragment. World Java strings are packed and chunked to avoid large initializer/string limits. APK byte sizes describe comparable local builds, not a production signing/release upload.

## Calculation methods

New methods use Aladhan's published ids/parameters. The country data, app fragment, browser and launcher agree; Arabic authority labels are shown in settings. Iran resolves to MWL. No Tehran method or new Jafari country mapping is introduced; existing explicit legacy preferences are preserved.

| Country-data method | Aladhan id | Fajr | Isha | Additional adjustments |
|---|---:|---:|---:|---|
| KARACHI | 1 | 18° | 18° | None |
| ISNA | 2 | 15° | 15° | None |
| MWL | 3 | 18° | 17° | None |
| SINGAPORE / MUIS | 11 | 20° | 18° | None |
| FRANCE_UOIF | 12 | 12° | 12° | None |
| DIYANET | 13 | 18° | 17° | Sunrise −7, Dhuhr +5, Asr +4, Maghrib +7 minutes |
| RUSSIA | 14 | 16° | 15° | None |
| MOONSIGHTING | 15 | Seasonal morning twilight | Seasonal evening twilight | General shafaq; published metadata uses latitude adjustment `NONE` |
| JAKIM | 17 | 20° | 18° | None |
| KEMENAG | 20 | 20° | 18° | None |

All ten use shadow-length-one Asr and sunset at 0.833°. None introduces a fixed Isha-minute delay. Moonsighting uses its seasonal piecewise interpolation directly, rather than substituting an 18° angle or a blanket one-seventh rule. With `x = abs(latitude)/55`, the four morning coefficients are `75+28.65x`, `75+19.44x`, `75+32.74x`, `75+48.10x`; evening coefficients are `75+25.60x`, `75+2.05x`, `75−9.21x`, `75+6.14x`, interpolated through solar-season boundaries 91/137/183/229/275/366. Northern solar-season offset is +10 days; the southern offset is −172 days (−173 in leap years).

Existing automatic methods retain their parameters: Umm al-Qura (4: 18.5°, Isha 90 minutes / 120 in Ramadan), Gulf (8: 19.5° / 90 minutes), Kuwait (9: 18°/17.5°), Qatar (10: 18° / 90 minutes), Dubai (16: 18.2°/18.2°, sunrise −3 and Dhuhr/Asr/Maghrib +3), Egypt (5: 19.5°/17.5°), Jordan (23: 18°/18°, Maghrib +5), Morocco (21: 19°/17°, Dhuhr/Maghrib +5), Algeria (19: 18°/17°), Tunisia (18: 18°/18°), and local Oman (100: 18°/18°, Dhuhr/Asr/Maghrib +5; never requested as an Aladhan id).

For angle methods, unreachable/excessive Fajr or Isha twilight is bounded by `angle/60` of the night, capped at half the night, matching Aladhan's angle-based option (`latitudeAdjustmentMethod=3`). Where even sunrise/sunset is absent, a same-hemisphere 48° surrogate at the same longitude/time zone supplies an ordered day and night. This polar fallback is an explicit approximation, not a claimed official timetable. Solar noon is normalized to the local date before preserving after-midnight Maghrib/Isha; both the page and alarm keep the pending previous-night event.

Parameter sources: [Aladhan methods API](https://api.aladhan.com/v1/methods), [Aladhan calculation methods](https://aladhan.com/calculation-methods), the request URLs embedded in `tests/prayer_crosscheck_world.json`, and [adhan-js seasonal twilight equations](https://github.com/batoulapps/adhan-js/blob/master/src/Astronomical.ts). The registry snapshot is checked independently of the app's tables.

## File-by-file changes

| File | Change |
|---|---|
| `.github/tools/cities-world/overrides.json` | Reviewed spellings and capital-addition names; preserves an already-correct Ciudad Guayana spelling. |
| `.github/tools/cities-world/merge.py` | Applies overrides, validates Arabic, restores country labels, recovers missing capital flags, recomputes order, and regenerates deterministically. |
| `.github/tools/cities-world/requirements.txt` | Pins `geonamescache==3.0.2` and `Babel==2.18.0`. |
| `.github/tools/cities-world/sources/capitals-geonames.tsv` | Credited raw GeoNames capital record below the cache population cutoff. |
| `.github/tools/cities-world/cities-merged.json` | Regenerated reviewed 3,307-row data; no hand-edited geometry. |
| `.github/tools/cities-world/REVIEW.md` | Every country's checked/fixed/added/capital counts, every changed name, sources and limits. |
| `.github/tools/cities-world/README.md` | Documents integration status and reproducible review/generation commands. |
| `.github/tools/cities-world/Cities-support.java.txt` | Generated Java support for country metadata, packed chunks, labels, lookup and migration. |
| `.github/tools/gen_cities.py` | Appends world regions after the untouched original rows and emits packed Java chunks. |
| `.github/tools/cities_md.py` | Reads the generated packed city representation and lists every country/city/method. |
| `.github/workflows/checks.yml` | Installs pinned world-data test dependencies. |
| `android/app/src/main/java/sa/urtv/screensaver/Cities.java` | Regenerated 3,681-city list and country support. |
| `android/app/src/main/java/sa/urtv/screensaver/CityMatch.java` | Cached bilingual matching, country qualification and legacy-name precedence. |
| `android/app/src/main/java/sa/urtv/screensaver/Location.java` | Builds the full country list and filters regions by country identity. |
| `android/app/src/main/java/sa/urtv/screensaver/PrayerMethod.java` | Resolves countries to method ids, Arabic authority labels and global search eligibility. |
| `android/app/src/main/java/sa/urtv/screensaver/Prefs.java` | Stores/migrates country and passes resolved numeric method in settings. |
| `android/app/src/main/java/sa/urtv/screensaver/SaverReceiver.java` | Uses bilingual lookup, accepts explicit custom country and exports country state. |
| `android/app/src/main/java/sa/urtv/screensaver/SettingsActivity.java` | Full country picker, bilingual city labels and country-preserving geocoding/selections. |
| `android/launcher/src/main/java/sa/urtv/home/ClockZone.java` | Arabic labels for resolved numeric methods. |
| `android/launcher/src/main/java/sa/urtv/home/tilawah/PrayerMath.java` | New parameters, seasonal/angle/polar rules, date-line normalization and ordered API clocks. |
| `android/launcher/src/main/java/sa/urtv/home/tilawah/PrayerTimes.java` | Explicit API high-latitude rule and pending previous-night prayer scheduling. |
| `src/template.html` | Matching methods/labels/math, dated computation and after-midnight prayer display handoff. |
| `src/build.py` | Compact embedded JSON serialization to meet the unchanged page budget. |
| `dist/tv.html` | Rebuilt public page with the new calculations and refreshed source build stamp. |
| `optimize/saver.sh` | Optional country in custom-city command and quoted bilingual/country-qualified input. |
| `optimize/settings_move.py` | Backward-compatible optional country export/import, diff, validation and undo. |
| `docs/CITIES.md` | Regenerated complete country-ordered city/method reference. |
| `docs/FEATURES.md` | Arabic city, country-method and high-latitude behavior documentation. |
| `docs/SETTINGS.md` | Arabic CLI country qualification and portable country settings documentation. |
| `tests/WorldCitiesRun.java` | Executes decoded app rows, all 6,614 world-name queries and country fragments. |
| `tests/cities_world_test.py` | Reproducibility, original-row preservation, Arabic/capital/order/identity checks and actual generator verification. |
| `tests/cities_md_test.py` | Complete generated world-city documentation coverage. |
| `tests/city_match_test.py` | Arabic/English/qualified/ambiguous queries and old-name regressions. |
| `tests/PrayerMethodRun.java` | Country-based method/authority/search exercise alongside existing zone cases. |
| `tests/prayer_method_test.py` | Three-consumer equality plus independent published registry parameters and offsets. |
| `tests/prayer_method_registry.json` | Source-attributed Aladhan parameter snapshot for the ten new methods. |
| `tests/PrayerWorldRun.java` | Published-time, chronological-order, midnight and polar checks against real Java math. |
| `tests/prayer_crosscheck_world_test.py` | Validates fixture coverage and runs the Java comparisons. |
| `tests/prayer_crosscheck_world.json` | 60 published Aladhan responses with request provenance. |
| `tests/prayer_crosscheck_world_page.mjs` | Offline browser comparisons and midnight Isha-to-Fajr handoff. |
| `tests/saver_quote_test.py` | Shell quoting for bilingual/qualified names and custom country. |
| `tests/settings_move_test.py` | Export/import, country-only change, undo, invalid code and old-app compatibility cases. |
| `tests/run.sh` | Runs the new data and world prayer checks with the existing full suite. |
| `.github/tools/cities-world/RESULT.md` | This implementation, validation, sizes, parameters and uncertainty report. |

## Uncertainties and limits

The complete review is a careful language/identity review, not an independently sourced Arabic exonym for every small town. Reasonable transliterations and multiple accepted spellings remain; duplicate settlements/urban districts already present in GeoNames are retained. `REVIEW.md` identifies consulted sources and examples. Country/territory and capital flags describe the data's administrative centres, without making sovereignty claims.

Published API comparisons cover a representative sample rather than every city/date. Local calculations can differ from a local mosque's timetable, especially at polar latitudes. The 48° no-sunrise fallback is deliberately documented as an approximation. No TV hardware was used to validate remote-control interaction or alarms; the real-browser tests, JVM tests, Android framework compilation and APK builds provide the recorded verification.
