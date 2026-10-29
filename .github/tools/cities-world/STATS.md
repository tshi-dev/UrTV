# Worldwide cities for the prayer-times app: stats

- Total cities: **1379**
- Countries: **198**
- Capitals in output: **194**
- Countries skipped (hand-checked in the app): SA, BH, KW, QA, AE, OM, EG, JO, SY, LB, PS, MA, DZ, TN
- Cities selected but dropped for lacking an Arabic name: **1958** (see `missing-arabic.csv`)
- Cities dropped as duplicate (country, name_ar), bigger city kept: **5**
  - dropped Kāshān (6861211, IR, كاشان, pop 304487)
  - dropped Marāgheh (124878, IR, مراغه, pop 262604)
  - dropped Sāveh (116667, IR, ساوه, pop 220762)
  - dropped Zawiya (2216885, LY, الزاوية, pop 186123)
  - dropped Rabak (368277, SD, ربك, pop 135281)
- Capitals with no matching GeoNames city: [('AN', 'Willemstad'), ('AQ', ''), ('BQ', ''), ('BV', ''), ('CS', 'Belgrade'), ('HM', ''), ('PW', 'Melekeok'), ('TK', ''), ('UM', '')]
- Capitals dropped for lacking an Arabic name (not transliterated): 35
  - CF Central African Republic: Bangui
  - DO Dominican Republic: Santo Domingo
  - GN Guinea: Conakry
  - JM Jamaica: Kingston
  - MM Myanmar: Nay Pyi Taw
  - MV Maldives: Male
  - US United States: Washington
  - AG Antigua and Barbuda: Saint John’s
  - AI Anguilla: The Valley
  - AX Aland Islands: Mariehamn
  - BI Burundi: Gitega
  - BL Saint Barthelemy: Gustavia
  - CC Cocos Islands: West Island
  - FM Micronesia: Palikir
  - GD Grenada: Saint George's
  - GS South Georgia and the South Sandwich Islands: Grytviken
  - IO British Indian Ocean Territory: Downtown
  - JE Jersey: Saint Helier
  - KI Kiribati: Tarawa
  - LI Liechtenstein: Vaduz
  - MC Monaco: Monaco
  - MF Saint Martin: Marigot
  - MH Marshall Islands: Majuro
  - MP Northern Mariana Islands: Saipan
  - NU Niue: Alofi
  - PA Panama: Panama City
  - PM Saint Pierre and Miquelon: Saint-Pierre
  - RE Reunion: Saint-Denis
  - TC Turks and Caicos Islands: Cockburn Town
  - TF French Southern Territories: Port-aux-Français
  - TO Tonga: Nuku‘alofa
  - TV Tuvalu: Funafuti
  - VA Vatican: Vatican City
  - VU Vanuatu: Port-Vila
  - YT Mayotte: Mamoudzou
- Countries without any capital row: ['AG', 'AI', 'AN', 'AQ', 'AX', 'BI', 'BL', 'BQ', 'BV', 'CC', 'CF', 'CS', 'DO', 'FM', 'GD', 'GN', 'GS', 'HM', 'IO', 'JE', 'JM', 'KI', 'LI', 'MC', 'MF', 'MH', 'MM', 'MP', 'MV', 'NU', 'PA', 'PM', 'PW', 'RE', 'TC', 'TF', 'TK', 'TO', 'TV', 'UM', 'US', 'VA', 'VU', 'YT']
- Overrides used: 19 of 19

## Country order

Order is set by the `MUSLIMS` table in `build.py`: Arab League members first, then the other countries in the table by Muslim population, then the rest by total population. The Muslim-population figures are approximate (Pew-style, rounded, from memory) and only set the order; they do not select cities.

| order | country_ar | ISO | count |
|---|---|---|---|
| 1 | السودان | SD | 1 |
| 2 | العراق | IQ | 1 |
| 3 | اليمن | YE | 1 |
| 4 | الصومال | SO | 1 |
| 5 | ليبيا | LY | 1 |
| 6 | موريتانيا | MR | 1 |
| 7 | جيبوتي | DJ | 1 |
| 8 | جزر القمر | KM | 1 |
| 9 | إندونيسيا | ID | 1 |
| 10 | الهند | IN | 1 |
| 11 | باكستان | PK | 1 |
| 12 | بنغلاديش | BD | 1 |
| 13 | نيجيريا | NG | 1 |
| 14 | إيران | IR | 1 |
| 15 | تركيا | TR | 1 |
| 16 | أفغانستان | AF | 1 |
| 17 | إثيوبيا | ET | 1 |
| 18 | أوزبكستان | UZ | 1 |
| 19 | الصين | CN | 1 |
| 20 | النيجر | NE | 1 |
| 21 | روسيا | RU | 1 |
| 22 | مالي | ML | 1 |
| 23 | ماليزيا | MY | 1 |
| 24 | السنغال | SN | 1 |
| 25 | تنزانيا | TZ | 1 |
| 26 | كازاخستان | KZ | 1 |
| 27 | ساحل العاج | CI | 1 |
| 28 | تشاد | TD | 1 |
| 29 | بوركينا فاسو | BF | 1 |
| 30 | أذربيجان | AZ | 1 |
| 31 | طاجيكستان | TJ | 1 |
| 32 | قيرغيزستان | KG | 1 |
| 33 | موزمبيق | MZ | 1 |
| 34 | الفلبين | PH | 1 |
| 35 | تركمانستان | TM | 1 |
| 36 | فرنسا | FR | 1 |
| 37 | غانا | GH | 1 |
| 38 | كينيا | KE | 1 |
| 39 | ميانمار | MM | 1 |
| 40 | تايلاند | TH | 1 |
| 41 | الكاميرون | CM | 1 |
| 42 | أوغندا | UG | 1 |
| 43 | ألمانيا | DE | 1 |
| 44 | المملكة المتحدة | GB | 1 |
| 45 | الولايات المتحدة | US | 1 |
| 46 | غامبيا | GM | 1 |
| 47 | سيراليون | SL | 1 |
| 48 | ألبانيا | AL | 1 |
| 49 | كوسوفو | XK | 1 |
| 50 | البوسنة والهرسك | BA | 1 |
| 51 | إيطاليا | IT | 1 |
| 52 | كندا | CA | 1 |
| 53 | إسبانيا | ES | 1 |
| 54 | هولندا | NL | 1 |
| 55 | بروناي | BN | 1 |
| 56 | البرازيل | BR | 1 |
| 57 | اليابان | JP | 1 |
| 58 | المكسيك | MX | 1 |
| 59 | فيتنام | VN | 1 |
| 60 | الكونغو - كينشاسا | CD | 1 |
| 61 | جنوب أفريقيا | ZA | 1 |
| 62 | كوريا الجنوبية | KR | 1 |
| 63 | كولومبيا | CO | 1 |
| 64 | الأرجنتين | AR | 1 |
| 65 | أوكرانيا | UA | 1 |
| 66 | بولندا | PL | 1 |
| 67 | بيرو | PE | 1 |
| 68 | أنغولا | AO | 1 |
| 69 | فنزويلا | VE | 1 |
| 70 | نيبال | NP | 1 |
| 71 | مدغشقر | MG | 1 |
| 72 | كوريا الشمالية | KP | 1 |
| 73 | أستراليا | AU | 1 |
| 74 | تايوان | TW | 1 |
| 75 | سريلانكا | LK | 1 |
| 76 | رومانيا | RO | 1 |
| 77 | تشيلي | CL | 1 |
| 78 | ملاوي | MW | 1 |
| 79 | زامبيا | ZM | 1 |
| 80 | غواتيمالا | GT | 1 |
| 81 | الإكوادور | EC | 1 |
| 82 | زيمبابوي | ZW | 1 |
| 83 | كمبوديا | KH | 1 |
| 84 | رواندا | RW | 1 |
| 85 | بنين | BJ | 1 |
| 86 | بلجيكا | BE | 1 |
| 87 | بوليفيا | BO | 1 |
| 88 | كوبا | CU | 1 |
| 89 | بوروندي | BI | 1 |
| 90 | هايتي | HT | 1 |
| 91 | اليونان | GR | 1 |
| 92 | جمهورية الدومينيكان | DO | 1 |
| 93 | التشيك | CZ | 1 |
| 94 | البرتغال | PT | 1 |
| 95 | السويد | SE | 1 |
| 96 | هنغاريا | HU | 1 |
| 97 | هندوراس | HN | 1 |
| 98 | بيلاروس | BY | 1 |
| 99 | إسرائيل | IL | 1 |
| 100 | النمسا | AT | 1 |
| 101 | بابوا غينيا الجديدة | PG | 1 |
| 102 | سويسرا | CH | 1 |
| 103 | جنوب السودان | SS | 1 |
| 104 | توغو | TG | 1 |
| 105 | هونغ كونغ الصينية | HK | 1 |
| 106 | لاوس | LA | 1 |
| 107 | بلغاريا | BG | 1 |
| 108 | صربيا | RS | 1 |
| 109 | باراغواي | PY | 1 |
| 110 | نيكاراغوا | NI | 1 |
| 111 | السلفادور | SV | 1 |
| 112 | إريتريا | ER | 1 |
| 113 | الدانمرك | DK | 1 |
| 114 | سنغافورة | SG | 1 |
| 115 | فنلندا | FI | 1 |
| 116 | سلوفاكيا | SK | 1 |
| 117 | النرويج | NO | 1 |
| 118 | الكونغو - برازافيل | CG | 1 |
| 119 | كوستاريكا | CR | 1 |
| 120 | نيوزيلندا | NZ | 1 |
| 121 | أيرلندا | IE | 1 |
| 122 | ليبيريا | LR | 1 |
| 123 | كرواتيا | HR | 1 |
| 124 | جورجيا | GE | 1 |
| 125 | مولدوفا | MD | 1 |
| 126 | أورغواي | UY | 1 |
| 127 | بورتوريكو | PR | 1 |
| 128 | منغوليا | MN | 1 |
| 129 | أرمينيا | AM | 1 |
| 130 | ليتوانيا | LT | 1 |
| 131 | ناميبيا | NA | 1 |
| 132 | بوتسوانا | BW | 1 |
| 133 | الغابون | GA | 1 |
| 134 | ليسوتو | LS | 1 |
| 135 | مقدونيا الشمالية | MK | 1 |
| 136 | سلوفينيا | SI | 1 |
| 137 | لاتفيا | LV | 1 |
| 138 | غينيا بيساو | GW | 1 |
| 139 | ترينيداد وتوباغو | TT | 1 |
| 140 | إستونيا | EE | 1 |
| 141 | غينيا الاستوائية | GQ | 1 |
| 142 | تيمور - ليشتي | TL | 1 |
| 143 | موريشيوس | MU | 1 |
| 144 | قبرص | CY | 1 |
| 145 | إسواتيني | SZ | 1 |
| 146 | فيجي | FJ | 1 |
| 147 | غيانا | GY | 1 |
| 148 | بوتان | BT | 1 |
| 149 | جزر سليمان | SB | 1 |
| 150 | منطقة ماكاو الإدارية الخاصة | MO | 1 |
| 151 | الجبل الأسود | ME | 1 |
| 152 | لوكسمبورغ | LU | 1 |
| 153 | سورينام | SR | 1 |
| 154 | الرأس الأخضر | CV | 1 |
| 155 | مالطا | MT | 1 |
| 156 | غوادلوب | GP | 1 |
| 157 | جزر المارتينيك | MQ | 1 |
| 158 | جزر البهاما | BS | 1 |
| 159 | بليز | BZ | 1 |
| 160 | آيسلندا | IS | 1 |
| 161 | بربادوس | BB | 1 |
| 162 | كاليدونيا الجديدة | NC | 1 |
| 163 | بولينيزيا الفرنسية | PF | 1 |
| 164 | الصحراء الغربية | EH | 1 |
| 165 | ساو تومي وبرينسيبي | ST | 1 |
| 166 | ساموا | WS | 1 |
| 167 | غويانا الفرنسية | GF | 1 |
| 168 | سانت لوسيا | LC | 1 |
| 169 | غوام | GU | 1 |
| 170 | كوراساو | CW | 1 |
| 171 | سانت فنسنت وجزر غرينادين | VC | 1 |
| 172 | جزر فيرجن الأمريكية | VI | 1 |
| 173 | أروبا | AW | 1 |
| 174 | سيشل | SC | 1 |
| 175 | جزيرة مان | IM | 1 |
| 176 | أندورا | AD | 1 |
| 177 | دومينيكا | DM | 1 |
| 178 | غيرنزي | GG | 1 |
| 179 | جزر كايمان | KY | 1 |
| 180 | برمودا | BM | 1 |
| 181 | غرينلاند | GL | 1 |
| 182 | ساموا الأمريكية | AS | 1 |
| 183 | سانت كيتس ونيفيس | KN | 1 |
| 184 | جزر فارو | FO | 1 |
| 185 | سانت مارتن | SX | 1 |
| 186 | سان مارينو | SM | 1 |
| 187 | جبل طارق | GI | 1 |
| 188 | جزر فيرجن البريطانية | VG | 1 |
| 189 | جزر كوك | CK | 1 |
| 190 | جزر والس وفوتونا | WF | 1 |
| 191 | ناورو | NR | 1 |
| 192 | مونتسرات | MS | 1 |
| 193 | سانت هيلينا | SH | 1 |
| 194 | جزر فوكلاند | FK | 1 |
| 195 | سفالبارد وجان ماين | SJ | 1 |
| 196 | جزيرة نورفولك | NF | 1 |
| 197 | جزيرة كريسماس | CX | 1 |
| 198 | جزر بيتكيرن | PN | 1 |

## 30 random rows (seed 20261029)

| country | name_ar | name_en | population | capital | tz | geonameid |
|---|---|---|---|---|---|---|
| اليابان | يوكوهاما | Yokohama | 3777491 |  | Asia/Tokyo | 1848354 |
| الفلبين | مانىلا | Manila | 1600000 | yes | Asia/Manila | 1701668 |
| تركيا | أنطاليا | Antalya | 1335002 |  | Europe/Istanbul | 323777 |
| منغوليا | أولان باتور | Ulan Bator | 844818 | yes | Asia/Ulaanbaatar | 2028462 |
| روسيا | سورغوت | Surgut | 300367 |  | Asia/Yekaterinburg | 1490624 |
| ميانمار | يانجون | Yangon | 4477638 |  | Asia/Yangon | 1298824 |
| الصين | فو جو | Fuzhou | 3740000 |  | Asia/Shanghai | 1810821 |
| نيجيريا | ماكوردي | Makurdi | 390000 |  | Africa/Lagos | 2331140 |
| غانا | كوماسي | Kumasi | 2544530 |  | Africa/Accra | 2298890 |
| كازاخستان | بتروبافل | Petropavl | 200920 |  | Asia/Almaty | 1520172 |
| الهند | جايبور | Jaipur | 3046163 |  | Asia/Kolkata | 1269515 |
| الصين | تشنغدو | Chengdu | 13568357 |  | Asia/Shanghai | 1815286 |
| إثيوبيا | ولديا | Weldiya | 104000 |  | Africa/Addis_Ababa | 326406 |
| باكستان | كويته | Quetta | 1565546 |  | Asia/Karachi | 1167528 |
| كازاخستان | كوستناي | Kostanay | 210000 |  | Asia/Qostanay | 1519928 |
| غوام | هاغاتنيا | Hagåtña | 1051 | yes | Pacific/Guam | 4044012 |
| تركيا | عشاق | Uşak | 369433 |  | Europe/Istanbul | 298299 |
| سيراليون | كويدو | Koidu | 128030 |  | Africa/Freetown | 2407656 |
| الهند | آبوهار | Abohar | 145302 |  | Asia/Kolkata | 1279403 |
| أفغانستان | قندز | Kunduz | 161902 |  | Asia/Kabul | 1135689 |
| روسيا | أباكان | Abakan | 184168 |  | Asia/Krasnoyarsk | 1512236 |
| بولندا | ورشو | Warsaw | 1702139 | yes | Europe/Warsaw | 756135 |
| الهند | جام نجر | Jamnagar | 600943 |  | Asia/Kolkata | 1269317 |
| تركيا | جاناكالي | Çanakkale | 143622 |  | Europe/Istanbul | 749780 |
| الهند | بنغالور | Bengaluru | 8495492 |  | Asia/Kolkata | 1277333 |
| الصين | ليجيانج | Lijiang | 211151 |  | Asia/Shanghai | 1813253 |
| الصين | جنان | Jinan | 4335989 |  | Asia/Shanghai | 1805753 |
| إيران | قزوين | Qazvin | 333635 |  | Asia/Tehran | 119505 |
| قيرغيزستان | جلال آباد | Manas | 123239 |  | Asia/Bishkek | 1528249 |
| ماليزيا | سرمبان | Seremban | 372917 |  | Asia/Kuala_Lumpur | 1734810 |

## Suspicious name_ar (longer than 25 characters, or the same as another country's city)

| country | name_ar | name_en | length | reason |
|---|---|---|---|---|
| AF | جلال آباد | Jalālābād | 9 | same as other country: KG |
| KG | جلال آباد | Manas | 9 | same as other country: AF |
| CA | هاميلتون | Hamilton | 8 | same as other country: BM |
| ES | بلنسية | Valencia | 6 | same as other country: VE |
| VE | بلنسية | Valencia | 6 | same as other country: ES |
| DO | سانتياغو دي لوس كابالييروس | Santiago de los Caballeros | 26 | long |
| GY | جورج تاون | Georgetown | 9 | same as other country: KY |
| KY | جورج تاون | George Town | 9 | same as other country: GY |
| BM | هاميلتون | Hamilton | 8 | same as other country: CA |
