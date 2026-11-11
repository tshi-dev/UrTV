"""Questions whose ANSWER starts with a letter that was short in the letter hive (ظ ض ذ خ ح). Facts with page evidence (hand.py)."""
Q = "quiz"
def R(id, d, age, q, a, wrong, ev, cat, numeric=False):
    return dict(id=id, type=Q, cat=cat, d=d, age=age, q=q, a=a, wrong=wrong, ev=ev, numeric=numeric)

ROWS = [
 R("hv-dhahran", 2, "all", ("في أي مدينة سعودية يقع المقر الرئيسي لأرامكو السعودية؟", "In which Saudi city is the Saudi Aramco headquarters?"), ("الظهران", "Dhahran"),
   [("الرياض", "Riyadh"), ("جدة", "Jeddah"), ("ينبع", "Yanbu")], [("Saudi Aramco", r"Dhahran")], "saudi-gulf"),
 R("hv-dhofar", 3, "all", ("ما اسم أكبر محافظات سلطنة عمان مساحة؟", "What is the largest governorate of Oman by area?"), ("ظفار", "Dhofar"),
   [("مسقط", "Muscat"), ("مسندم", "Musandam"), ("الداخلية", "Ad Dakhiliyah")], [("Dhofar Governorate", r"largest of the 11 governorates in the Sultanate of Oman by area")], "saudi-gulf"),
 R("hv-shadow", 1, "kids", ("ماذا نسمّي المنطقة المظلمة التي يحجب فيها جسمٌ الضوء عن سطح؟", "What do we call the dark area where an object blocks light from a surface?"), ("الظل", "Shadow"),
   [("السراب", "Mirage"), ("الشفق", "Twilight"), ("الندى", "Dew")], [("Shadow", r"dark area on a surface where light from a light source is blocked")], "science"),
 R("hv-dhuhr", 2, "all", ("ما الصلاة المفروضة التي تُؤدّى بعد الفجر وقبل العصر؟", "Which obligatory prayer comes after Fajr and before Asr?"), ("الظهر", "Dhuhr"),
   [("المغرب", "Maghrib"), ("العشاء", "Isha"), ("الجمعة", "Jumuah")], [("Dhuhr", r"observed after Fajr and before Asr")], "islamic"),
 R("hv-frog", 1, "kids", ("ما الحيوان البرمائي القصير الجسم عديم الذيل؟", "Which short-bodied amphibian has no tail?"), ("الضفدع", "Frog"),
   [("السلمندر", "Salamander"), ("الحرباء", "Chameleon"), ("السلحفاة", "Turtle")], [("Frog", r"tailless amphibian")], "animals"),
 R("hv-fog", 2, "all", ("ما اسم السحابة المنخفضة من قطرات الماء الدقيقة قرب سطح الأرض؟", "What is the low cloud of tiny water droplets near the ground called?"), ("الضباب", "Fog"),
   [("الندى", "Dew"), ("الصقيع", "Frost"), ("الرعد", "Thunder")], [("Fog", r"tiny water droplets or ice crystals held in the air\s+near the Earth's surface")], "science"),
 R("hv-hyena", 2, "kids", ("أي حيوان مفترس إفريقي يُلقّب بـ«الضاحك» لصوته؟", "Which African predator is nicknamed the laughing one for its call?"), ("الضبع", "Hyena"),
   [("الفهد", "Cheetah"), ("الأسد", "Lion"), ("الذئب", "Wolf")], [("Spotted hyena", r"laughing hyena")], "animals"),
 R("hv-light", 1, "kids", ("ما الذي إذا حجبه جسمٌ تكوّن الظل؟", "What, when blocked by an object, makes a shadow?"), ("الضوء", "Light"),
   [("الصوت", "Sound"), ("الحرارة", "Heat"), ("الهواء", "Air")], [("Shadow", r"light from a light source is blocked")], "science"),
 R("hv-corn", 1, "kids", ("من أي نوع من الحبوب يُصنع الفشار (البوشار)؟", "Which grain is popcorn made from?"), ("الذرة", "Corn"),
   [("القمح", "Wheat"), ("الشعير", "Barley"), ("الأرز", "Rice")], [("Popcorn", r"variety of corn kernel")], "food"),
 R("hv-hijjah", 2, "all", ("ما الشهر الهجري الثاني عشر والأخير؟", "Which is the twelfth and last month of the Hijri year?"), ("ذو الحجة", "Dhu al-Hijjah"),
   [("محرم", "Muharram"), ("رمضان", "Ramadan"), ("شوال", "Shawwal")], [("Eid al-Adha", r"twelfth and final month"), ("Islamic calendar", r"12 lunar months")], "islamic", True),
 R("hv-khwarizmi", 3, "all", ("أي عالم من العصر الذهبي كتب بالعربية في الرياضيات والفلك وسُمّيت الخوارزمية باسمه؟", "Which Islamic Golden Age scholar wrote in mathematics, astronomy and geography, and gave his name to the algorithm?"), ("الخوارزمي", "Al-Khwarizmi"),
   [("الرازي", "Al-Razi"), ("ابن سينا", "Ibn Sina"), ("ابن بطوطة", "Ibn Battuta")], [("Muhammad ibn Musa al-Khwarizmi", r"mathematics, astronomy, and geography"), ("Muhammad ibn Musa al-Khwarizmi", r"algorithm")], "history"),
 R("hv-trench", 2, "all", ("ما اسم الغزوة التي حُفر فيها خندق حول المدينة المنورة؟", "Which battle was fought around a trench dug near Medina?"), ("الخندق", "The Trench"),
   [("بدر", "Badr"), ("أحد", "Uhud"), ("تبوك", "Tabuk")], [("Battle of the Trench", r"Ghazwat al-Khandaq")], "islamic"),
 R("hv-bat", 2, "kids", ("ما الثدييات الوحيدة القادرة على الطيران الحقيقي المستمر؟", "Which mammals are the only ones capable of true sustained flight?"), ("الخفافيش", "Bats"),
   [("السناجب الطائرة", "Flying squirrels"), ("الدلافين", "Dolphins"), ("الخلد", "Moles")], [("Bat", r"the only mammals capable of true and sustained flight")], "animals"),
 R("hv-autumn", 1, "kids", ("ما الفصل الذي ينتقل فيه الطقس من الصيف إلى الشتاء؟", "Which season is the transition from summer to winter?"), ("الخريف", "Autumn"),
   [("الربيع", "Spring"), ("الصيف", "Summer"), ("الشتاء", "Winter")], [("Autumn", r"transition from summer to winter")], "science"),
 R("hv-bread", 1, "kids", ("ما الغذاء الأساسي المصنوع من الماء والدقيق وغالبًا الخميرة؟", "Which staple food is made from water, flour and often yeast?"), ("الخبز", "Bread"),
   [("الجبن", "Cheese"), ("الزبدة", "Butter"), ("العسل", "Honey")], [("Bread", r"made from water, flour, and often yeast")], "food"),
 R("hv-milk", 1, "kids", ("ما الغذاء الذي تنتجه الغدد الثديية للثدييات لتغذية صغارها؟", "Which food do the mammary glands of mammals make to feed their young?"), ("الحليب", "Milk"),
   [("العسل", "Honey"), ("الزيت", "Oil"), ("العصير", "Juice")], [("Milk", r"mammary glands of lactating mammals")], "food"),
]
