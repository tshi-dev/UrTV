package sa.urtv.screensaver;

import java.util.HashMap;
import java.util.Map;

/**
 * Place names in English for «اللغة» English (docs/ENGLISH.md: names are data, never the table): each country of Cities by its code (the
 * CLDR English names, written here so every TV shows the same), and the built-in cities and regions of the first list (Saudi Arabia, the
 * Gulf, Egypt, the Levant, the Maghreb), whose Cities rows have only the Arabic; the world list carries its own English (row[4]). Plain
 * Java: tests/i18n_saver_test.py runs it (every country and every built-in city has its English).
 */
final class Places {
    private Places() { }

    /** code, English */
    private static final String[] COUNTRIES = {
        "SA", "Saudi Arabia", "BH", "Bahrain", "KW", "Kuwait", "QA", "Qatar", "AE", "United Arab Emirates", "OM", "Oman", "EG", "Egypt",
        "JO", "Jordan", "SY", "Syria", "LB", "Lebanon", "PS", "Palestine", "MA", "Morocco", "DZ", "Algeria", "TN", "Tunisia", "SD", "Sudan",
        "IQ", "Iraq", "YE", "Yemen", "SO", "Somalia", "LY", "Libya", "MR", "Mauritania", "DJ", "Djibouti", "KM", "Comoros", "ID", "Indonesia",
        "IN", "India", "PK", "Pakistan", "BD", "Bangladesh", "NG", "Nigeria", "IR", "Iran", "TR", "Türkiye", "AF", "Afghanistan", "ET", "Ethiopia",
        "UZ", "Uzbekistan", "CN", "China", "NE", "Niger", "RU", "Russia", "ML", "Mali", "MY", "Malaysia", "SN", "Senegal", "TZ", "Tanzania",
        "KZ", "Kazakhstan", "CI", "Ivory Coast", "GN", "Guinea", "TD", "Chad", "BF", "Burkina Faso", "AZ", "Azerbaijan", "TJ", "Tajikistan",
        "KG", "Kyrgyzstan", "MZ", "Mozambique", "PH", "Philippines", "TM", "Turkmenistan", "FR", "France", "GH", "Ghana", "KE", "Kenya",
        "MM", "Myanmar", "TH", "Thailand", "CM", "Cameroon", "UG", "Uganda", "DE", "Germany", "GB", "United Kingdom", "US", "United States",
        "GM", "Gambia", "SL", "Sierra Leone", "AL", "Albania", "XK", "Kosovo", "BA", "Bosnia and Herzegovina", "IT", "Italy", "CA", "Canada",
        "ES", "Spain", "NL", "Netherlands", "BN", "Brunei", "MV", "Maldives", "BR", "Brazil", "JP", "Japan", "MX", "Mexico", "VN", "Vietnam",
        "CD", "DR Congo", "ZA", "South Africa", "KR", "South Korea", "CO", "Colombia", "AR", "Argentina", "UA", "Ukraine", "PL", "Poland",
        "PE", "Peru", "AO", "Angola", "VE", "Venezuela", "NP", "Nepal", "MG", "Madagascar", "KP", "North Korea", "AU", "Australia", "TW", "Taiwan",
        "LK", "Sri Lanka", "RO", "Romania", "CL", "Chile", "MW", "Malawi", "ZM", "Zambia", "GT", "Guatemala", "EC", "Ecuador", "ZW", "Zimbabwe",
        "KH", "Cambodia", "RW", "Rwanda", "BJ", "Benin", "BE", "Belgium", "BO", "Bolivia", "CU", "Cuba", "BI", "Burundi", "HT", "Haiti",
        "GR", "Greece", "DO", "Dominican Republic", "CZ", "Czechia", "PT", "Portugal", "SE", "Sweden", "HU", "Hungary", "HN", "Honduras",
        "BY", "Belarus", "AT", "Austria", "PG", "Papua New Guinea", "CH", "Switzerland", "SS", "South Sudan", "TG", "Togo", "HK", "Hong Kong",
        "LA", "Laos", "BG", "Bulgaria", "RS", "Serbia", "PY", "Paraguay", "NI", "Nicaragua", "SV", "El Salvador", "ER", "Eritrea", "DK", "Denmark",
        "SG", "Singapore", "FI", "Finland", "SK", "Slovakia", "NO", "Norway", "CG", "Congo", "CR", "Costa Rica", "NZ", "New Zealand",
        "IE", "Ireland", "LR", "Liberia", "CF", "Central African Republic", "PA", "Panama", "HR", "Croatia", "GE", "Georgia", "MD", "Moldova",
        "UY", "Uruguay", "PR", "Puerto Rico", "MN", "Mongolia", "AM", "Armenia", "JM", "Jamaica", "LT", "Lithuania", "NA", "Namibia",
        "BW", "Botswana", "GA", "Gabon", "LS", "Lesotho", "MK", "North Macedonia", "SI", "Slovenia", "LV", "Latvia", "GW", "Guinea-Bissau",
        "TT", "Trinidad & Tobago", "EE", "Estonia", "GQ", "Equatorial Guinea", "TL", "Timor-Leste", "MU", "Mauritius", "CY", "Cyprus",
        "SZ", "Eswatini", "FJ", "Fiji", "GY", "Guyana", "RE", "Reunion", "BT", "Bhutan", "SB", "Solomon Islands", "MO", "Macao", "ME", "Montenegro",
        "LU", "Luxembourg", "SR", "Suriname", "CV", "Cape Verde", "MT", "Malta", "GP", "Guadeloupe", "MQ", "Martinique", "BS", "Bahamas",
        "BZ", "Belize", "IS", "Iceland", "VU", "Vanuatu", "BB", "Barbados", "NC", "New Caledonia", "YT", "Mayotte", "PF", "French Polynesia",
        "ST", "Sao Tome and Principe", "WS", "Samoa", "GF", "French Guiana", "LC", "St Lucia", "GU", "Guam", "CW", "Curacao", "KI", "Kiribati",
        "FM", "Micronesia", "GD", "Grenada", "VC", "St Vincent and the Grenadines", "VI", "US Virgin Islands", "AW", "Aruba", "TO", "Tonga",
        "SC", "Seychelles", "AG", "Antigua & Barbuda", "JE", "Jersey", "IM", "Isle of Man", "AD", "Andorra", "DM", "Dominica", "GG", "Guernsey",
        "KY", "Cayman Islands", "BM", "Bermuda", "MH", "Marshall Islands", "MP", "Northern Mariana Islands", "GL", "Greenland",
        "AS", "American Samoa", "KN", "St Kitts and Nevis", "FO", "Faroe Islands", "SX", "Sint Maarten", "MC", "Monaco", "LI", "Liechtenstein",
        "TC", "Turks & Caicos Islands", "MF", "St Martin", "SM", "San Marino", "GI", "Gibraltar", "VG", "British Virgin Islands",
        "AX", "Aland Islands", "CK", "Cook Islands", "PW", "Palau", "WF", "Wallis & Futuna", "AI", "Anguilla", "NR", "Nauru", "TV", "Tuvalu",
        "MS", "Montserrat", "BL", "St Barthelemy", "SH", "St Helena", "PM", "St Pierre and Miquelon", "IO", "British Indian Ocean Territory",
        "FK", "Falkland Islands", "SJ", "Svalbard & Jan Mayen", "NU", "Niue", "NF", "Norfolk Island", "CX", "Christmas Island",
        "VA", "Vatican City", "CC", "Cocos (Keeling) Islands", "TF", "French Southern Territories", "PN", "Pitcairn Islands",
        "GS", "South Georgia and the South Sandwich Islands",
    };

    /** the first list's Arabic names (cities and regions), English */
    private static final String[] FIRST = {
        "منطقة الرياض", "Riyadh Region", "الرياض", "Riyadh", "الخرج", "Al Kharj", "الزلفي", "Az Zulfi", "المجمعة", "Al Majmaah",
        "الدوادمي", "Ad Dawadimi", "الدرعية", "Diriyah", "عفيف", "Afif", "عرقة", "Irqah", "الدلم", "Ad Dilam", "شقراء", "Shaqra",
        "حوطة بني تميم", "Hawtat Bani Tamim", "ليلى (الأفلاج)", "Layla (Al Aflaj)", "رماح", "Rumah", "السليل", "As Sulayyil",
        "الهياثم", "Al Hayathim", "ساجر", "Sajir", "الارطاوية", "Al Artawiyah", "مرات", "Marat", "العيينة", "Al Uyaynah", "تمير", "Tumair",
        "الجبيلة", "Al Jubaylah", "الحلوة", "Al Hilwah", "الرين", "Ar Rayn", "وادي الدواسر", "Wadi ad-Dawasir", "القويعية", "Al Quwayiyah",
        "حوطة سدير", "Hawtat Sudair", "الغاط", "Al Ghat", "ضرما", "Dhurma", "المزاحمية", "Al Muzahimiyah", "حريملاء", "Huraymila", "ثادق", "Thadiq",
        "الحريق", "Al Hariq", "منطقة مكة المكرمة", "Makkah Region", "جدة", "Jeddah", "مكة المكرمة", "Makkah", "الطائف", "Taif", "رابغ", "Rabigh",
        "الشفا", "Al Shafa", "الليث", "Al Lith", "رنية", "Ranyah", "الخرمة", "Al Khurmah", "ثول", "Thuwal", "تربة", "Turabah", "الجموم", "Al Jumum",
        "خليص", "Khulais", "مدينة الملك عبدالله الاقتصادية", "King Abdullah Economic City", "المويه", "Al Muwayh", "الهدا", "Al Hada",
        "القنفذة", "Al Qunfudhah", "الكامل", "Al Kamil", "أضم", "Adham", "ميسان", "Maysan", "العرضيات", "Al Ardiyat", "المظيلف", "Al Muzaylif",
        "بحرة", "Bahrah", "منطقة المدينة المنورة", "Madinah Region", "المدينة المنورة", "Madinah", "ينبع", "Yanbu", "العلا", "AlUla",
        "خيبر", "Khaybar", "بدر", "Badr", "مهد الذهب", "Mahd adh Dhahab", "الحناكية", "Al Hanakiyah", "العيص", "Al Ais",
        "وادي الفرع", "Wadi al Fara", "المنطقة الشرقية", "Eastern Province", "الدمام", "Dammam", "الهفوف", "Al Hofuf", "المبرز", "Al Mubarraz",
        "حفر الباطن", "Hafar Al Batin", "الثقبة", "Thuqbah", "الجبيل", "Jubail", "الخبر", "Khobar", "الظهران", "Dhahran", "القطيف", "Qatif",
        "تاروت", "Tarout", "سيهات", "Saihat", "رأس تنورة", "Ras Tanura", "الخفجي", "Khafji", "صفوى", "Safwa", "بقيق", "Abqaiq",
        "العوامية", "Al Awamiyah", "مدينة الملك خالد العسكرية", "King Khalid Military City", "الطرف", "At Taraf", "القيصومة", "Al Qaisumah",
        "عنك", "Anak", "الاوجام", "Al Awjam", "الجفر", "Al Jafr", "السفانية", "As Saffaniyah", "المطيرفي", "Al Mutayrifi", "الجعيمة", "Al Juaymah",
        "مليجة", "Mulayjah", "العضيلية", "Al Udailiyah", "شيبة", "Shaybah", "رأس الخير", "Ras Al Khair", "النعيرية", "An Nairiyah",
        "قرية العليا", "Qaryat al Ulya", "سلوى", "Salwa", "منطقة القصيم", "Al Qassim Region", "بريدة", "Buraydah", "عنيزة", "Unaizah",
        "الرس", "Ar Rass", "البدائع", "Al Badai", "البكيرية", "Al Bukayriyah", "المذنب", "Al Mithnab", "رياض الخبراء", "Riyadh Al Khabra",
        "الخبراء", "Al Khabra", "عيون الجواء", "Uyun al Jiwa", "النبهانية", "An Nabhaniyah", "قبة", "Qubbah", "عقلة الصقور", "Uqlat as Suqur",
        "المليداء", "Al Mulayda", "عين ابن فهيد", "Ain Ibn Fuhayd", "دخنة", "Dukhnah", "الفويلق", "Al Fuwayliq", "الذيبية", "Adh Dhibiyah",
        "وادي النخيل", "Wadi an Nakhil", "الأسياح", "Al Asyah", "الشماسية", "Ash Shimasiyah", "ضرية", "Dariyah", "منطقة عسير", "Asir Region",
        "خميس مشيط", "Khamis Mushait", "أبها", "Abha", "سبت العلايا", "Sabt Al Alayah", "بيشة", "Bisha", "بارق", "Bariq", "النماص", "An Namas",
        "سراة عبيدة", "Sarat Abidah", "المجاردة", "Al Majardah", "تبالة", "Tabalah", "تثليث", "Tathlith", "محايل عسير", "Muhayil Asir",
        "رجال ألمع", "Rijal Almaa", "تنومة", "Tanomah", "ظهران الجنوب", "Dhahran Al Janub", "أحد رفيدة", "Ahad Rufaidah", "بلقرن", "Balqarn",
        "منطقة تبوك", "Tabuk Region", "تبوك", "Tabuk", "تيماء", "Tayma", "أملج", "Umluj", "الوجه", "Al Wajh", "حقل", "Haql", "ضباء", "Duba",
        "البدع", "Al Bad", "نيوم", "NEOM", "حالة عمار", "Halat Ammar", "منطقة حائل", "Hail Region", "حائل", "Hail", "تربة حائل", "Turubah (Hail)",
        "بقعاء", "Baqaa", "الشملي", "Ash Shamli", "الغزالة", "Al Ghazalah", "الشنان", "Ash Shinan", "موقق", "Mawqaq", "الحائط", "Al Hait",
        "منطقة الحدود الشمالية", "Northern Borders Region", "عرعر", "Arar", "طريف", "Turaif", "رفحاء", "Rafha", "العويقيلة", "Al Uwayqilah",
        "منطقة جازان", "Jazan Region", "صبيا", "Sabya", "جازان", "Jazan", "أبو عريش", "Abu Arish", "فيفاء", "Fifa", "صامطة", "Samtah",
        "ضمد", "Damad", "الظبية", "Adh Dhabyah", "فرسان", "Farasan", "ذهبان", "Dhahban", "الجرادية", "Al Jaradiyah", "مسلية", "Muslliyah",
        "الشقيري", "Ash Shuqayri", "مزهرة", "Mizhirah", "الدرب", "Ad Darb", "الشقيق", "Ash Shuqaiq", "العارضة", "Al Aridah", "بيش", "Baish",
        "الريث", "Ar Rayth", "العيدابي", "Al Idabi", "الحرث", "Al Harth", "منطقة نجران", "Najran Region", "نجران", "Najran",
        "المشعلية", "Al Mashaliyah", "حبونا", "Habuna", "الوديعة", "Al Wadiah", "خباش", "Khubash", "يدمة", "Yadamah", "بدر الجنوب", "Badr Al Janub",
        "شرورة", "Sharurah", "ثار", "Thar", "منطقة الباحة", "Al Bahah Region", "الباحة", "Al Bahah", "بلجرشي", "Baljurashi", "العقيق", "Al Aqiq",
        "المندق", "Al Mandaq", "المخواة", "Al Mikhwah", "قلوة", "Qilwah", "القرى", "Al Qura", "منطقة الجوف", "Al Jawf Region", "سكاكا", "Sakaka",
        "القريات", "Al Qurayyat", "طبرجل", "Tabarjal", "صوير", "Suwayr", "دومة الجندل", "Dumat Al Jandal", 
        "المنامة", "Manama", "المحرق", "Muharraq", "الرفاع", "Riffa", "مدينة حمد", "Hamad Town", "مدينة عيسى", "Isa Town", "سترة", "Sitra",
        "الحد", "Hidd", "البديع", "Budaiya", "مدينة الكويت", "Kuwait City", "حولي", "Hawalli", "السالمية", "Salmiya",
        "الفروانية", "Farwaniya", "الجهراء", "Jahra", "الأحمدي", "Ahmadi", "الفحيحيل", "Fahaheel", "الصباحية", "Sabahiya", 
        "الدوحة", "Doha", "الريان", "Al Rayyan", "الوكرة", "Al Wakrah", "الخور", "Al Khor", "أم صلال", "Umm Salal", "لوسيل", "Lusail",
        "الشمال", "Al Shamal", "دخان", "Dukhan", "مسيعيد", "Mesaieed", "دبي", "Dubai", "أبوظبي", "Abu Dhabi",
        "الشارقة", "Sharjah", "عجمان", "Ajman", "العين", "Al Ain", "رأس الخيمة", "Ras Al Khaimah", "الفجيرة", "Fujairah",
        "أم القيوين", "Umm Al Quwain", "خورفكان", "Khor Fakkan", "كلباء", "Kalba", "ليوا", "Liwa", "مسقط", "Muscat",
        "صلالة", "Salalah", "صحار", "Sohar", "نزوى", "Nizwa", "صور", "Sur", "البريمي", "Al Buraimi", "الرستاق", "Rustaq", "عبري", "Ibri",
        "السيب", "Seeb", "بركاء", "Barka", "بهلاء", "Bahla", "إبراء", "Ibra", "مطرح", "Muttrah", "خصب", "Khasab", "الدقم", "Duqm", 
        "القاهرة", "Cairo", "الإسكندرية", "Alexandria", "الجيزة", "Giza", "شبرا الخيمة", "Shubra El Kheima", "بورسعيد", "Port Said",
        "السويس", "Suez", "الأقصر", "Luxor", "المنصورة", "Mansoura", "طنطا", "Tanta", "أسيوط", "Asyut", "الإسماعيلية", "Ismailia",
        "الفيوم", "Faiyum", "الزقازيق", "Zagazig", "أسوان", "Aswan", "دمياط", "Damietta", "دمنهور", "Damanhur", "المنيا", "Minya",
        "بني سويف", "Beni Suef", "سوهاج", "Sohag", "قنا", "Qena", "الغردقة", "Hurghada", "شرم الشيخ", "Sharm El Sheikh", "العريش", "Arish",
        "مرسى مطروح", "Marsa Matruh", "عمان", "Amman", "الزرقاء", "Zarqa", "إربد", "Irbid", "السلط", "Salt", "العقبة", "Aqaba",
        "مأدبا", "Madaba", "الكرك", "Karak", "معان", "Maan", "جرش", "Jerash", "عجلون", "Ajloun", "المفرق", "Mafraq", "الطفيلة", "Tafilah",
        "الرصيفة", "Russeifa", "الرمثا", "Ramtha", "دمشق", "Damascus", "حلب", "Aleppo", "حمص", "Homs", "اللاذقية", "Latakia",
        "حماة", "Hama", "طرطوس", "Tartus", "دير الزور", "Deir ez-Zor", "الرقة", "Raqqa", "الحسكة", "Al Hasakah", "إدلب", "Idlib", "درعا", "Daraa",
        "السويداء", "As Suwayda", "القامشلي", "Qamishli", "بيروت", "Beirut", "طرابلس", "Tripoli", "صيدا", "Sidon",
        "صور (لبنان)", "Tyre", "النبطية", "Nabatieh", "زحلة", "Zahle", "جونيه", "Jounieh", "بعلبك", "Baalbek", "عاليه", "Aley",
        "القدس", "Jerusalem", "رام الله", "Ramallah", "نابلس", "Nablus", "الخليل", "Hebron", "بيت لحم", "Bethlehem",
        "جنين", "Jenin", "طولكرم", "Tulkarm", "قلقيلية", "Qalqilya", "أريحا", "Jericho", "غزة", "Gaza", "خان يونس", "Khan Yunis", "رفح", "Rafah",
        "دير البلح", "Deir al-Balah", "الدار البيضاء", "Casablanca", "الرباط", "Rabat", "فاس", "Fez", "مراكش", "Marrakesh",
        "طنجة", "Tangier", "أكادير", "Agadir", "مكناس", "Meknes", "وجدة", "Oujda", "القنيطرة", "Kenitra", "تطوان", "Tetouan", "سلا", "Sale",
        "الجديدة", "El Jadida", "بني ملال", "Beni Mellal", "الناظور", "Nador", "الحسيمة", "Al Hoceima", "آسفي", "Safi", "تازة", "Taza",
        "ورزازات", "Ouarzazate", "الصويرة", "Essaouira", "شفشاون", "Chefchaouen", "الجزائر", "Algiers", "وهران", "Oran", "قسنطينة", "Constantine",
        "عنابة", "Annaba", "البليدة", "Blida", "باتنة", "Batna", "سطيف", "Setif", "سيدي بلعباس", "Sidi Bel Abbes", "بسكرة", "Biskra",
        "تلمسان", "Tlemcen", "بجاية", "Bejaia", "ورقلة", "Ouargla", "تيزي وزو", "Tizi Ouzou", "غرداية", "Ghardaia", "جيجل", "Jijel",
        "مستغانم", "Mostaganem", "الأغواط", "Laghouat", "تبسة", "Tebessa", "تونس", "Tunis", "صفاقس", "Sfax", "سوسة", "Sousse",
        "القيروان", "Kairouan", "بنزرت", "Bizerte", "قابس", "Gabes", "أريانة", "Ariana", "قفصة", "Gafsa", "المنستير", "Monastir",
        "مدنين", "Medenine", "نابل", "Nabeul", "الحمامات", "Hammamet", "حومة السوق", "Houmt Souk", "توزر", "Tozeur", "تطاوين", "Tataouine",
        "جندوبة", "Jendouba", "الكاف", "El Kef", "المهدية", "Mahdia",
    };

    private static Map<String, String> countries, first, names;

    private static synchronized void load() {
        if (names != null) return;
        countries = pairs(COUNTRIES);
        first = pairs(FIRST);
        Map<String, String> n = new HashMap<>();
        for (Cities.Region r : Cities.REGIONS) for (String[] c : r.cities) if (c.length > 4 && c[4] != null && !c[4].isEmpty()) n.putIfAbsent(c[0], c[4]);
        n.putAll(first);
        for (String[] c : Cities.countries()) { String en = countries.get(c[0]); if (en != null) n.put(c[1], en); }   // a country before a city of its name
        for (Cities.Region r : Cities.REGIONS) {
            String en = r.name.contains("منطقة") ? first.get(r.name) : countries.get(r.country);
            if (en != null) n.put(r.name, en);
        }
        names = n;
    }

    private static Map<String, String> pairs(String[] a) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < a.length; i += 2) m.put(a[i], a[i + 1]);
        return m;
    }

    /** a country's English by its code, null for one not known */
    static String country(String code) { load(); return countries.get(code); }

    /** a region's English (its country's for a country of one region), null for none */
    static String region(Cities.Region r) {
        load();
        String en = r.name.contains("منطقة") ? first.get(r.name) : null;   // the Saudi regions; any other region is a whole country
        return en != null ? en : countries.get(r.country);
    }

    /** a built-in city's English (its row: Arabic, latitude, longitude[, zone, English, country]), null when it has none */
    static String city(String[] row) {
        if (row.length > 4 && row[4] != null && !row[4].isEmpty()) return row[4];
        load();
        return first.get(row[0]);
    }

    /** the saved city's English: the built-in one at these coordinates, null for a place of one's own (it keeps the name it was saved with) */
    static String city(String name, String lat, String lon) {
        String[] row = Cities.find(name, lat, lon);
        return row == null ? null : city(row);
    }

    /** any Arabic place name the screens show (a country first, then a region, then a city), null for none */
    static String name(String ar) { load(); return names.get(ar); }
}
