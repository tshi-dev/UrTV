# Arabic city-name review — 2026-10-29

Read all **3,305** supplied Arabic/English pairs in the supplied `order` sequence (Arab countries first), then checked every capital separately. **119** existing names corrected; **2** missing capitals added and reviewed. Final coverage: **3,307** names in **230** countries and territories.

This is a language and identity review, not a claim that every small-city transliteration has an independently attested Arabic exonym. Reasonable transliterations are retained where no established Arabic name is known. Every pair was inspected for wrong-city substitutions, missing syllables, malformed Arabic, non-Arabic letters and familiar established names. Unicode checks found Cyrillic in Mudanjiang and Thai in Puyang. No coordinates, zones or GeoNames IDs of existing rows changed.

All spelling decisions are in `overrides.json`. `VE|Ciudad Guayana` preserves an existing correct spelling lost by the old merge to an empty correction; it is not a new fix. Sri Lanka’s legislative capital and Palau’s capital are added. Capital aliases are used only when a country still lacks a capital, so historical Abuja/Suleja and metropolitan aliases do not become extra capitals. Six unflagged capitals are recognized through normalized GeoNames names/aliases: AG, VU, TO, GD, IO, TF. Order is recomputed so Guinea and the Maldives no longer appear after the rest of the world.

## Sources and limits

- Arabic Wikipedia identity/spelling checks: [عقرة](https://ar.wikipedia.org/wiki/عقرة), [بحركة](https://ar.wikipedia.org/wiki/بحركة), [عابدواق](https://ar.wikipedia.org/wiki/عابدواق), [لاس عانود](https://ar.wikipedia.org/wiki/لاس_عانود), [ليبرفيل](https://ar.wikipedia.org/wiki/ليبرفيل), [أثينا](https://ar.wikipedia.org/wiki/أثينا), [فاليتا](https://ar.wikipedia.org/wiki/فاليتا). Other corrections are orthographic/identity judgments from the pairs, not individually sourced Wikipedia lookups.
- Sayyān is سيان near Sanaa, not Seiyun: [Yemen city list](https://en.wikipedia.org/wiki/List_of_cities_in_Yemen).
- Sri Lanka: [Kotte municipal council](https://www.kotte.mc.gov.lk/index.php?Itemid=175&id=25&lang=en&option=com_content&view=article), [Parliament](https://www.parliament.lk/en/visit-parliament/visiting-parliament/how-to-get-here). GeoNames cache ID 1238992 supplies coordinates.
- Palau: [Saudi Fund for Development announcement](https://www.afp.com/pt/node/3820236) uses نغيرولمود. GeoNames ID 8063361 is preserved in `sources/capitals-geonames.tsv` with attribution.
- Territory flags mean GeoNames administrative centres, not a statement of sovereignty. Uninhabited territories and obsolete codes absent from the source are not invented.
- Main uncertainty: small-city transliterations, multiple accepted Arabic spellings, and duplicate settlements/districts already in GeoNames.

Capital-name verification also consulted Arabic Wikipedia for [ماسيرو](https://ar.wikipedia.org/wiki/ماسيرو), [غابورون](https://ar.wikipedia.org/wiki/غابورون), [إسكوبية](https://ar.wikipedia.org/wiki/إسكوبية), [هراري](https://ar.wikipedia.org/wiki/هراري), [سول](https://ar.wikipedia.org/wiki/سول), [فريتاون](https://ar.wikipedia.org/wiki/فريتاون), [لواندا](https://ar.wikipedia.org/wiki/لواندا), and [أمستردام](https://ar.wikipedia.org/wiki/أمستردام). [Al Jazeera’s Sri Lanka entry](https://www.aljazeera.net/encyclopedia/2012/1/12/سريلانكا) uses كولومبو; [the UN Arabic pocketbook](https://unstats.un.org/unsd/publications/pocketbook/files/AR-world-stats-pocketbook-2018.pdf) uses تورشهاون.

## Counts per country

Checked includes the new capitals. Fixed counts changes to existing names only. Rows follow corrected app order.

| ISO | Country | Checked | Fixed | Added | Capital rows |
|---|---|---:|---:|---:|---:|
| SD | السودان | 34 | 3 | 0 | 1 |
| IQ | العراق | 63 | 8 | 0 | 1 |
| YE | اليمن | 11 | 1 | 0 | 1 |
| SO | الصومال | 22 | 3 | 0 | 1 |
| LY | ليبيا | 26 | 1 | 0 | 1 |
| MR | موريتانيا | 9 | 1 | 0 | 1 |
| DJ | جيبوتي | 3 | 1 | 0 | 1 |
| KM | جزر القمر | 1 | 0 | 0 | 1 |
| ID | إندونيسيا | 168 | 4 | 0 | 1 |
| IN | الهند | 536 | 19 | 0 | 1 |
| PK | باكستان | 134 | 4 | 0 | 1 |
| BD | بنغلاديش | 55 | 0 | 0 | 1 |
| NG | نيجيريا | 109 | 3 | 0 | 1 |
| IR | إيران | 102 | 8 | 0 | 1 |
| TR | تركيا | 111 | 5 | 0 | 1 |
| AF | أفغانستان | 10 | 0 | 0 | 1 |
| ET | إثيوبيا | 30 | 4 | 0 | 1 |
| UZ | أوزبكستان | 23 | 2 | 0 | 1 |
| CN | الصين | 654 | 5 | 0 | 1 |
| NE | النيجر | 6 | 0 | 0 | 1 |
| RU | روسيا | 214 | 6 | 0 | 1 |
| ML | مالي | 10 | 0 | 0 | 1 |
| MY | ماليزيا | 86 | 3 | 0 | 1 |
| SN | السنغال | 19 | 0 | 0 | 1 |
| TZ | تنزانيا | 34 | 1 | 0 | 1 |
| KZ | كازاخستان | 23 | 0 | 0 | 1 |
| CI | ساحل العاج | 4 | 0 | 0 | 1 |
| GN | غينيا | 12 | 0 | 0 | 1 |
| TD | تشاد | 4 | 1 | 0 | 1 |
| BF | بوركينا فاسو | 7 | 0 | 0 | 1 |
| AZ | أذربيجان | 6 | 0 | 0 | 1 |
| TJ | طاجيكستان | 7 | 0 | 0 | 1 |
| KG | قيرغيزستان | 3 | 0 | 0 | 1 |
| MZ | موزمبيق | 4 | 0 | 0 | 1 |
| PH | الفلبين | 146 | 5 | 0 | 1 |
| TM | تركمانستان | 4 | 0 | 0 | 1 |
| FR | فرنسا | 4 | 0 | 0 | 1 |
| GH | غانا | 2 | 0 | 0 | 1 |
| KE | كينيا | 4 | 1 | 0 | 1 |
| MM | ميانمار | 4 | 0 | 0 | 1 |
| TH | تايلاند | 53 | 0 | 0 | 1 |
| CM | الكاميرون | 2 | 0 | 0 | 1 |
| UG | أوغندا | 2 | 0 | 0 | 1 |
| DE | ألمانيا | 15 | 1 | 0 | 1 |
| GB | المملكة المتحدة | 7 | 1 | 0 | 1 |
| US | الولايات المتحدة | 42 | 1 | 0 | 1 |
| GM | غامبيا | 2 | 0 | 0 | 1 |
| SL | سيراليون | 4 | 1 | 0 | 1 |
| AL | ألبانيا | 4 | 0 | 0 | 1 |
| XK | كوسوفو | 3 | 0 | 0 | 1 |
| BA | البوسنة والهرسك | 5 | 0 | 0 | 1 |
| IT | إيطاليا | 6 | 0 | 0 | 1 |
| CA | كندا | 12 | 0 | 0 | 1 |
| ES | إسبانيا | 6 | 0 | 0 | 1 |
| NL | هولندا | 2 | 1 | 0 | 1 |
| BN | بروناي | 1 | 0 | 0 | 1 |
| MV | جزر المالديف | 1 | 0 | 0 | 1 |
| BR | البرازيل | 41 | 3 | 0 | 1 |
| JP | اليابان | 36 | 2 | 0 | 1 |
| MX | المكسيك | 38 | 0 | 0 | 1 |
| VN | فيتنام | 15 | 1 | 0 | 1 |
| CD | الكونغو - كينشاسا | 10 | 1 | 0 | 1 |
| ZA | جنوب أفريقيا | 12 | 1 | 0 | 1 |
| KR | كوريا الجنوبية | 19 | 1 | 0 | 1 |
| CO | كولومبيا | 10 | 1 | 0 | 1 |
| AR | الأرجنتين | 6 | 0 | 0 | 1 |
| UA | أوكرانيا | 9 | 0 | 0 | 1 |
| PL | بولندا | 5 | 0 | 0 | 1 |
| PE | بيرو | 6 | 0 | 0 | 1 |
| AO | أنغولا | 14 | 1 | 0 | 1 |
| VE | فنزويلا | 7 | 0 | 0 | 1 |
| NP | نيبال | 2 | 0 | 0 | 1 |
| MG | مدغشقر | 1 | 0 | 0 | 1 |
| KP | كوريا الشمالية | 2 | 0 | 0 | 1 |
| AU | أستراليا | 8 | 1 | 0 | 1 |
| TW | تايوان | 6 | 0 | 0 | 1 |
| LK | سريلانكا | 2 | 1 | 1 | 2 |
| RO | رومانيا | 1 | 0 | 0 | 1 |
| CL | تشيلي | 3 | 0 | 0 | 1 |
| MW | ملاوي | 2 | 0 | 0 | 1 |
| ZM | زامبيا | 3 | 0 | 0 | 1 |
| GT | غواتيمالا | 2 | 0 | 0 | 1 |
| EC | الإكوادور | 3 | 0 | 0 | 1 |
| ZW | زيمبابوي | 2 | 1 | 0 | 1 |
| KH | كمبوديا | 2 | 0 | 0 | 1 |
| RW | رواندا | 1 | 0 | 0 | 1 |
| BJ | بنين | 2 | 0 | 0 | 1 |
| BE | بلجيكا | 2 | 0 | 0 | 1 |
| BO | بوليفيا | 4 | 0 | 0 | 1 |
| CU | كوبا | 2 | 0 | 0 | 1 |
| BI | بوروندي | 2 | 0 | 0 | 1 |
| HT | هايتي | 2 | 0 | 0 | 1 |
| GR | اليونان | 1 | 1 | 0 | 1 |
| DO | جمهورية الدومينيكان | 4 | 0 | 0 | 1 |
| CZ | التشيك | 1 | 0 | 0 | 1 |
| PT | البرتغال | 1 | 0 | 0 | 1 |
| SE | السويد | 2 | 0 | 0 | 1 |
| HU | هنغاريا | 3 | 0 | 0 | 1 |
| HN | هندوراس | 2 | 0 | 0 | 1 |
| BY | بيلاروس | 2 | 0 | 0 | 1 |
| IL | إسرائيل | 1 | 0 | 0 | 1 |
| AT | النمسا | 1 | 0 | 0 | 1 |
| PG | بابوا غينيا الجديدة | 1 | 0 | 0 | 1 |
| CH | سويسرا | 1 | 0 | 0 | 1 |
| SS | جنوب السودان | 1 | 0 | 0 | 1 |
| TG | توغو | 1 | 0 | 0 | 1 |
| HK | هونغ كونغ الصينية | 6 | 0 | 0 | 1 |
| LA | لاوس | 1 | 0 | 0 | 1 |
| BG | بلغاريا | 1 | 0 | 0 | 1 |
| RS | صربيا | 1 | 0 | 0 | 1 |
| PY | باراغواي | 1 | 0 | 0 | 1 |
| NI | نيكاراغوا | 1 | 0 | 0 | 1 |
| SV | السلفادور | 1 | 0 | 0 | 1 |
| ER | إريتريا | 1 | 0 | 0 | 1 |
| DK | الدانمرك | 1 | 0 | 0 | 1 |
| SG | سنغافورة | 1 | 0 | 0 | 1 |
| FI | فنلندا | 1 | 0 | 0 | 1 |
| SK | سلوفاكيا | 1 | 0 | 0 | 1 |
| NO | النرويج | 1 | 0 | 0 | 1 |
| CG | الكونغو - برازافيل | 2 | 0 | 0 | 1 |
| CR | كوستاريكا | 1 | 1 | 0 | 1 |
| NZ | نيوزيلندا | 2 | 0 | 0 | 1 |
| IE | أيرلندا | 1 | 0 | 0 | 1 |
| LR | ليبيريا | 1 | 0 | 0 | 1 |
| CF | جمهورية أفريقيا الوسطى | 1 | 0 | 0 | 1 |
| PA | بنما | 1 | 0 | 0 | 1 |
| HR | كرواتيا | 1 | 0 | 0 | 1 |
| GE | جورجيا | 1 | 0 | 0 | 1 |
| MD | مولدوفا | 1 | 0 | 0 | 1 |
| UY | أورغواي | 1 | 0 | 0 | 1 |
| PR | بورتوريكو | 1 | 0 | 0 | 1 |
| MN | منغوليا | 1 | 0 | 0 | 1 |
| AM | أرمينيا | 1 | 0 | 0 | 1 |
| JM | جامايكا | 2 | 0 | 0 | 1 |
| LT | ليتوانيا | 1 | 0 | 0 | 1 |
| NA | ناميبيا | 1 | 0 | 0 | 1 |
| BW | بوتسوانا | 1 | 1 | 0 | 1 |
| GA | الغابون | 1 | 1 | 0 | 1 |
| LS | ليسوتو | 1 | 1 | 0 | 1 |
| MK | مقدونيا الشمالية | 1 | 1 | 0 | 1 |
| SI | سلوفينيا | 1 | 0 | 0 | 1 |
| LV | لاتفيا | 1 | 0 | 0 | 1 |
| GW | غينيا بيساو | 1 | 0 | 0 | 1 |
| TT | ترينيداد وتوباغو | 1 | 0 | 0 | 1 |
| EE | إستونيا | 1 | 0 | 0 | 1 |
| GQ | غينيا الاستوائية | 1 | 0 | 0 | 1 |
| TL | تيمور - ليشتي | 1 | 0 | 0 | 1 |
| MU | موريشيوس | 1 | 0 | 0 | 1 |
| CY | قبرص | 1 | 0 | 0 | 1 |
| SZ | إسواتيني | 1 | 0 | 0 | 1 |
| FJ | فيجي | 1 | 0 | 0 | 1 |
| GY | غيانا | 1 | 0 | 0 | 1 |
| RE | روينيون | 1 | 0 | 0 | 1 |
| BT | بوتان | 1 | 0 | 0 | 1 |
| SB | جزر سليمان | 1 | 0 | 0 | 1 |
| MO | منطقة ماكاو الإدارية الخاصة | 1 | 0 | 0 | 1 |
| ME | الجبل الأسود | 1 | 0 | 0 | 1 |
| LU | لوكسمبورغ | 1 | 0 | 0 | 1 |
| SR | سورينام | 1 | 0 | 0 | 1 |
| CV | الرأس الأخضر | 1 | 0 | 0 | 1 |
| MT | مالطا | 1 | 1 | 0 | 1 |
| GP | غوادلوب | 1 | 0 | 0 | 1 |
| MQ | جزر المارتينيك | 1 | 0 | 0 | 1 |
| BS | جزر البهاما | 1 | 0 | 0 | 1 |
| BZ | بليز | 1 | 0 | 0 | 1 |
| IS | آيسلندا | 1 | 0 | 0 | 1 |
| VU | فانواتو | 1 | 0 | 0 | 1 |
| BB | بربادوس | 1 | 0 | 0 | 1 |
| NC | كاليدونيا الجديدة | 1 | 0 | 0 | 1 |
| YT | مايوت | 1 | 0 | 0 | 1 |
| PF | بولينيزيا الفرنسية | 1 | 0 | 0 | 1 |
| EH | الصحراء الغربية | 1 | 0 | 0 | 1 |
| ST | ساو تومي وبرينسيبي | 1 | 0 | 0 | 1 |
| WS | ساموا | 1 | 0 | 0 | 1 |
| GF | غويانا الفرنسية | 1 | 0 | 0 | 1 |
| LC | سانت لوسيا | 1 | 0 | 0 | 1 |
| GU | غوام | 1 | 0 | 0 | 1 |
| CW | كوراساو | 1 | 0 | 0 | 1 |
| KI | كيريباتي | 1 | 0 | 0 | 1 |
| FM | ميكرونيزيا | 1 | 0 | 0 | 1 |
| GD | غرينادا | 1 | 0 | 0 | 1 |
| VC | سانت فنسنت وجزر غرينادين | 1 | 0 | 0 | 1 |
| VI | جزر فيرجن الأمريكية | 1 | 0 | 0 | 1 |
| AW | أروبا | 1 | 0 | 0 | 1 |
| TO | تونغا | 1 | 0 | 0 | 1 |
| SC | سيشل | 1 | 0 | 0 | 1 |
| AG | أنتيغوا وبربودا | 1 | 0 | 0 | 1 |
| JE | جيرسي | 1 | 0 | 0 | 1 |
| IM | جزيرة مان | 1 | 0 | 0 | 1 |
| AD | أندورا | 1 | 0 | 0 | 1 |
| DM | دومينيكا | 1 | 0 | 0 | 1 |
| GG | غيرنزي | 1 | 0 | 0 | 1 |
| KY | جزر كايمان | 1 | 0 | 0 | 1 |
| BM | برمودا | 1 | 0 | 0 | 1 |
| MH | جزر مارشال | 1 | 0 | 0 | 1 |
| MP | جزر ماريانا الشمالية | 1 | 0 | 0 | 1 |
| GL | غرينلاند | 1 | 0 | 0 | 1 |
| AS | ساموا الأمريكية | 1 | 0 | 0 | 1 |
| KN | سانت كيتس ونيفيس | 1 | 0 | 0 | 1 |
| FO | جزر فارو | 1 | 1 | 0 | 1 |
| SX | سانت مارتن | 1 | 0 | 0 | 1 |
| MC | موناكو | 1 | 0 | 0 | 1 |
| LI | ليختنشتاين | 1 | 0 | 0 | 1 |
| TC | جزر توركس وكايكوس | 1 | 0 | 0 | 1 |
| MF | سان مارتن | 1 | 0 | 0 | 1 |
| SM | سان مارينو | 1 | 0 | 0 | 1 |
| GI | جبل طارق | 1 | 0 | 0 | 1 |
| VG | جزر فيرجن البريطانية | 1 | 0 | 0 | 1 |
| AX | جزر آلاند | 1 | 0 | 0 | 1 |
| CK | جزر كوك | 1 | 0 | 0 | 1 |
| PW | بالاو | 1 | 0 | 1 | 1 |
| WF | جزر والس وفوتونا | 1 | 1 | 0 | 1 |
| AI | أنغويلا | 1 | 0 | 0 | 1 |
| NR | ناورو | 1 | 1 | 0 | 1 |
| TV | توفالو | 1 | 0 | 0 | 1 |
| MS | مونتسرات | 1 | 0 | 0 | 1 |
| BL | سان بارتليمي | 1 | 0 | 0 | 1 |
| SH | سانت هيلينا | 1 | 0 | 0 | 1 |
| PM | سان بيير ومكويلون | 1 | 0 | 0 | 1 |
| IO | الإقليم البريطاني في المحيط الهندي | 1 | 1 | 0 | 1 |
| FK | جزر فوكلاند | 1 | 0 | 0 | 1 |
| SJ | سفالبارد وجان ماين | 1 | 0 | 0 | 1 |
| NU | نيوي | 1 | 0 | 0 | 1 |
| NF | جزيرة نورفولك | 1 | 0 | 0 | 1 |
| CX | جزيرة كريسماس | 1 | 0 | 0 | 1 |
| VA | الفاتيكان | 1 | 0 | 0 | 1 |
| CC | جزر كوكوس | 1 | 0 | 0 | 1 |
| TF | الأقاليم الجنوبية الفرنسية | 1 | 0 | 0 | 1 |
| PN | جزر بيتكيرن | 1 | 0 | 0 | 1 |
| GS | جورجيا الجنوبية وجزر ساندويتش الجنوبية | 1 | 0 | 0 | 1 |

## Corrections to existing names

| ISO / English key | Before | After |
|---|---|---|
| SD / El Geneina Fort | الجنينة فورت | قلعة الجنينة |
| SD / Kadugli | كادقلى | كادقلي |
| SD / Shendi | شندى | شندي |
| IQ / Basrah | بصرة | البصرة |
| IQ / Najaf | نجف | النجف |
| IQ / Al-Kut | كوت | الكوت |
| IQ / Ramadi | الرمادى | الرمادي |
| IQ / As Samawah | سماوه | السماوة |
| IQ / Baḥarkah | بركه | بحركة |
| IQ / 'Ākra | عاكرة | عقرة |
| IQ / Khāliş | خالص | الخالص |
| YE / Sayyān | سيئون | سيان |
| SO / Cabudwaaq | كابودواق | عابدواق |
| SO / Laascaanood | لاسكانود | لاس عانود |
| SO / Garoowe | غاروه | غروي |
| LY / Benghazi | بنغازى | بنغازي |
| MR / Ksar | قصر | لكصر |
| DJ / Balbala | بلبالا | بلبلة |
| ID / Tanjung Pinang | تانجونغ | تانجونغ بينانغ |
| ID / Purwodadi | بورواداي | بوروودادي |
| ID / Pamanukan | باماننوكان | بامانوكان |
| ID / Serang | سيراغ | سيرانغ |
| IN / Mumbai | مومباى | مومباي |
| IN / Madurai | مادورائ | مادوراي |
| IN / Nowrangapur | نواربانغابور | ناوارانغابور |
| IN / Amritsar | امرتسر | أمريتسار |
| IN / Gurugram | قورقان | غوروغرام |
| IN / Kākināda | كاكيناضا | كاكينادا |
| IN / Muzaffarnagar | مظفر نغار | مظفر نغر |
| IN / Bārāsat | باراساتي | باراسات |
| IN / Darbhanga | دربنقا | داربهانغا |
| IN / Begusarai | بجساراى | بيغوساراي |
| IN / Raebareli | راى بريلى | راي بريلي |
| IN / Jaunpur | جونفور | جونبور |
| IN / Bhuj | بهوى | بوج |
| IN / Chitradurga | تشيتراجورغا | تشيترادورغا |
| IN / Haldwani | هالدوانى | هالدواني |
| IN / Srikakulam | سيركاكولم | سريكاكولام |
| IN / Mandya | ماندي | مانديا |
| IN / Barshi | بارسى | بارشي |
| IN / Roorkee | روركى | روركي |
| PK / Muridke | موريدكى | موريدكي |
| PK / Kunri | كوني | كونري |
| PK / Mian Channun | ميان تشنون | ميان تشانون |
| PK / Mandi Bahauddin | منداي بهاء الدين | ماندي بهاء الدين |
| NG / Jos | جاس | جوس |
| NG / Umuahia | اموهيا | أومواهيا |
| NG / Owo | أووو | أوو |
| IR / Eslamshahr | اسلامشهر | إسلام شهر |
| IR / Ardabīl | اردبيل | أردبيل |
| IR / Khomeynī Shahr | خمينيشهر | خميني شهر |
| IR / Sari | سارى | ساري |
| IR / Īlām | يلام | إيلام |
| IR / Iranshahr | ايرانشهر | إيران شهر |
| IR / Pardīs | باردس | برديس |
| IR / Fardīs | فاردس | فرديس |
| TR / İskenderun | اسكندرون | إسكندرون |
| TR / İzmit | ازمت | إزميت |
| TR / Erzincan | ارزنجان | أرزنجان |
| TR / Iğdır | اغدير | إغدير |
| TR / Umraniye | أومرانييه | عمرانية |
| ET / Dire Dawa | ديرة داوا | دير داوا |
| ET / Dessie | ديسى | ديسي |
| ET / Debre Birhan | ديبرى بيرهان | ديبري برهان |
| ET / Debre Tabor | ديبرى تابور | ديبري تابور |
| UZ / Qo‘qon | قوقون | خوقند |
| UZ / Navoiy | نواوي | نوائي |
| CN / Suzhou | سوجو | سوتشو |
| CN / Bozhou | بوجوا | بوتشو |
| CN / Mudanjiang | موданجيانغ | مودانجيانغ |
| CN / Shiyan | شيايان | شييان |
| CN / Puyang | بوياง | بويانغ |
| RU / Yekaterinburg | يكاترينبرج | يكاترينبورغ |
| RU / Volgograd | فولجوجراد | فولغوغراد |
| RU / Saratov | سراتاف | ساراتوف |
| RU / Petropavlovsk-Kamchatsky | بتروبافلوفسك | بتروبافلوفسك كامتشاتسكي |
| RU / Novomoskovsk | نوفوموسكوفيك | نوفوموسكوفسك |
| RU / Elista | الستا | إليستا |
| MY / Selayang Baru Utara | سيلاياغ بارو أوتارا | سيلايانغ بارو أوتارا |
| MY / Maran | مارانغ | ماران |
| MY / Sentul | سنترول | سنتول |
| TZ / Musoma | ماسوما | موسوما |
| TD / Moundou | مؤندؤ | موندو |
| PH / General Santos | جنرل سانتوس | جنرال سانتوس |
| PH / Imus | اموس | إيموس |
| PH / Cotabato | كوتابايتو | كوتاباتو |
| PH / Puerto Princesa | بورتوبرنسس | بويرتو برينسيسا |
| PH / Isulan | اسولان | إيسولان |
| KE / Nairobi | نايروبى | نيروبي |
| DE / Munich | مونخ | ميونخ |
| GB / Glasgow | جلاسجو | غلاسكو |
| US / Los Angeles | لس آنجلس | لوس أنجلوس |
| SL / Freetown | فريتون | فريتاون |
| NL / Amsterdam | آمستردام | أمستردام |
| BR / Rio de Janeiro | ريو دى جانيرو | ريو دي جانيرو |
| BR / Salvador | سلوادور | سلفادور |
| BR / Aparecida de Goiânia | أباريسيدا دي غوياني | أباريسيدا دي غويانيا |
| JP / Tokyo | توكيو | طوكيو |
| JP / Suginami | سوغيناما | سوغينامي |
| VN / Hanoi | هانوى | هانوي |
| CD / Likasi | ليكاسى | ليكاسي |
| ZA / Benoni | بينونى | بينوني |
| KR / Seoul | سئول | سول |
| CO / Cúcuta | سوسوتا | كوكوتا |
| AO / Luanda | لوآندا | لواندا |
| AU / Sydney | سيدنى | سيدني |
| LK / Colombo | كولمبو | كولومبو |
| ZW / Harare | هراره | هراري |
| GR / Athens | آتن | أثينا |
| CR / San José | سان جوز | سان خوسيه |
| BW / Gaborone | جابورون | غابورون |
| GA / Libreville | لبرول | ليبرفيل |
| LS / Maseru | ماسرو | ماسيرو |
| MK / Skopje | سكوبيه | إسكوبية |
| MT / Valletta | والتا | فاليتا |
| FO / Tórshavn | توشهافن | تورشهاون |
| WF / Mata-Utu | ماتا-اتو | ماتا أوتو |
| NR / Yaren | ياران | يارين |
| IO / Downtown | داونتاون | دييغو غارسيا |

## Added capital names

| ISO / GeoNames English name | Arabic | GeoNames ID |
|---|---|---:|
| LK / Sri Jayewardenepura Kotte | سري جاياواردنابورا كوتي | 1238992 |
| PW / Ngerulmud | نغيرولمود | 8063361 |

## After the review

The owner's rule: the app never lists «إسرائيل» as a country (Palestine has its own checked list), and Western Sahara is not a separate country (its cities are Morocco's). `merge.py` drops IL and EH (one row each), and the test checks the data, the list, the page and CITIES.md.
