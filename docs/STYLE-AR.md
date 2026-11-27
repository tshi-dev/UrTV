# Arabic wording: the Saudi style guide

How UrTV's Arabic should read: like a Saudi family talks, clear on a TV from the couch. This guide comes from the pilot in
`docs/research/SAUDI-WORDING.md`, on branch work/44b4-saudi-wording. The owner approved its 50 rewrites, and they are in the
app.

## The rules

1. **Labels and titles stay short MSA**, as in the big Saudi apps (glossary below):
   - «الإعدادات»، «مؤقت النوم»، «إيقاف الرقابة الأبوية»، «شاشة التوقف»
   - A row's name is a noun, not a sentence.
2. **Sentences use "white" Saudi speech**: the neutral dialect most Saudis understand. This covers a row's state line,
   messages, errors, confirmations, and everything in the games.

   | Instead of | Write |
   |---|---|
   | لا يوجد … | ما فيه … |
   | تعذّر … | ما قدرنا … / ما انفتحت … |
   | أعد المحاولة | جرّب مرة ثانية |
   | مسبقاً | من قبل |
   | فقط (in a sentence) | بس |
   | لـ / كي | عشان |
   | التي / الذي | اللي |
   | قبل أن | قبل لا |
   | الآن (in a message) | الحين |
   | غير صحيح (an error) | غلط |
   | لم يُحدَّد ، أُضيف (passive) | ما فيه … محدد ، انضاف |
   | يُقفل | يتقفل |

   «ينقفل» is not a word.
3. **Strong regional words** («لين»، «وش»، «السوالف») go in games, party lines and friendly states only, never in an
   error.
4. **Games speak to the group**: plurals («شوفوا»، «عدّوا الأصوات»، «اكشفوا») and «مين».
5. **TV space**: a rewrite stays within the old length plus 10%, and at least 2 characters more. Shorter is better.
6. **Never**:
   - the middle dot (U+00B7) or emoji in Arabic text;
   - Western digits where the app shows Arabic-Indic ones (٠-٩; `Digits.of` for numbers);
   - a fact the old wording did not say.
7. **Religious text is never reworded**: verses, hadith, adhkar, the Quran's names and data (`assets/shared/verses.json`,
   `hadith.json`, `adhkar.json`, the Quran assets).
8. **Every copy changes together**:
   - the Java (`S.t`/`S.n` and `Words`)
   - the key of its English line in `i18n/en/*.tsv`, with the English left as it is
   - the phone pages, the quiz pages
   - `docs/SETTINGS.md` and `docs/SETTINGS-TREE.md` (`python3 .github/tools/settings_tree.py`)
   - the tests that pin the text

   Two wordings that become the same keep one table line and one `Words` entry.

## The check (warnings, not a gate)

`python3 .github/tools/ar_style.py [base]` reads the Arabic strings a change writes and warns on:
- the middle dot or an emoji;
- Western digits in a file that shows Arabic-Indic ones;
- a string longer than its old wording allows;
- the formal patterns above.

It always exits 0, because the wording is a person's call. `tests/ar_style_test.py` keeps the check itself honest.

**Review step:** when a change adds or rewords Arabic, the agent runs the check, writes its suggested Saudi wording in the
pull request description, and the owner picks.

## Glossary: what the big Saudi apps say

Collected for the pilot. Their screens are short MSA, and no colloquial UI wording was found. Sources are in
`docs/research/SAUDI-WORDING.md`.

| App | Wording |
|---|---|
| Absher | «نسيت كلمة المرور»، «خدماتي»، «استعلامات»، «لوحة المعلومات»، «التالي» |
| Tawakkalna | «صحح بياناتك»، «معلوماتي»، «الإعدادات»، «الخدمات المفضلة» |
| Sehhaty | «المواعيد عن بُعد»، «استعراض الإجازات المرضية والتقارير الطبية» |
| stc | «خطوطي»، «الفواتير»، «ادفع»، «شحن» |
| Nafath | «تحقق من هويتك باستخدام تطبيق نفاذ» |
| Shahid | «مساحتي»، «إدارة الأجهزة»، «إلغاء الاشتراك»، «قائمتي» |
