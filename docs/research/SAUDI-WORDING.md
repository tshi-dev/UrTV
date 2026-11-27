# Saudi wording pilot (research only; no app string changed)

Goal: test whether a model or system can rewrite the settings and games wording so it sounds natural to a Saudi viewer. The test
used 150 real strings: 100 from settings and 50 from games.

Files:
- `docs/research/saudi-wording-pilot.csv`: all 150 strings with id, file:line, current text, length limit, rewrites A/B/C,
  scores, reason and pick.
- `docs/research/img/saudi-wording-pairs.png`: 12 before/after pairs at 1920×1080, for the owner.

## Verdict

1. **It is worth it, but as a style guide plus a review step, not as a model in the build.** Only 50 of the 150 strings
   improved. The other 100 are already good: short labels, and many strings already in Saudi speech («خلّ»، «بنحط»،
   «هذي اللعبة تحتاج جوالات»).
2. **The real gains are in sentences, not labels**: errors and confirmations («تعذّر…»، «لا يوجد…»، «أُضيف النص مهمةً»),
   passive voice («لم يُحدَّد»), and game instructions («قبل أن»، «التي»).
3. **Route A (Claude with a style guide and the glossary) is the only one that ran.** ALLaM and the other candidates could
   not be reached from this container (see below).
4. **Main risk: too much dialect.** No colloquial wording was found in any source from the big Saudi apps. Absher,
   Tawakkalna, Sehhaty, Nafath, stc and Shahid all use short MSA. Rewrites that use strongly Najdi words («لين»، «وش»، «الحين»،
   «السوالف») need the owner's taste call.
5. **Cost**: about nothing for route A, since it runs as a review step where an agent already works. A hosted ALLaM-13B on
   watsonx would cost about $0.0018 per 1K tokens, which is cents for the whole app, but it needs an IBM Cloud account and an
   API key.

## Recommended way to run it

- Settings **labels and titles stay short MSA**, like the Saudi apps (glossary below): «الإعدادات»، «مؤقت النوم»،
  «إيقاف الرقابة الأبوية».
- **Sentences, errors and game lines use "white" Saudi speech**, the neutral dialect most Saudis understand:
  - «ما فيه» instead of «لا يوجد»
  - «ما قدرنا…» instead of «تعذّر…»
  - «جرّب مرة ثانية» instead of «أعد المحاولة»
  - «عشان» instead of «لـ/كي»
  - «اللي» instead of «التي/الذي»
  - «بس» instead of «فقط»
  - «مين» in the games
  - no passive voice
- Use strongly regional words («لين»، «وش»، «الحين») only in games and party lines, never in errors.
- **A review step, not a CI gate.** When a PR adds or changes an Arabic string, the agent runs the style guide on the new
  strings and puts the suggestions in the PR description, and the owner picks. A CI check can only catch the mechanical
  rules: the middle dot, emoji, length, Arabic-Indic digits, and the formal patterns above as warnings. It cannot judge naturalness.
- Verses, hadith, adhkar and Quran data (`assets/shared/verses.json`, `hadith.json`, `adhkar.json`, the Quran assets) are
  never sent to a rewrite.

## How the pilot was run

- **Extraction**: Arabic string literals were taken from the code, without comments, from these files:
  - `SettingsActivity`, `SettingsHub`, `AppearanceActivity`, `LookPanelActivity`, `ParentalActivity`
  - the phone page `phone/index.html`
  - the games: `GameRegistry`, `GameActivity`, `QuizActivity`, `OddOneActivity`, `NightActivity`, `EngineGameActivity`,
    `GamePacksActivity`, `games/*`, `assets/quiz/*.html`

  That gave 2,950 literals, or 1,283 unique ones of 4 to 110 characters. The 150 strings were picked in a spread across
  files and kinds: labels, statuses, sentences, errors and phone messages. `SettingsHub`'s search synonyms, which are
  misspelled on purpose, were left out.
- **Length limit** per string: the current length plus 10%, and at least 2 more characters. All A rewrites fit, and most
  are shorter.
- **Hard rules, checked by script**:
  - `%s` and «» are kept exactly
  - no the middle dot and no emoji
  - Western digits are never added where the app uses Arabic-Indic ones
  - no religious text is in the set
- **Judging**: each string has two scores from 1 to 5, naturalness for a Saudi viewer and clarity, for both the current
  text and A. Two checks, length fits and meaning kept, are marked yes or no. A short reason and a pick follow.
  - The judge is the same model that wrote A, so the scores lean towards A. A Saudi reader, the owner or a family member,
    should check the 50 picks before any string changes.

| Area | Strings | A picked | Current kept |
|---|---|---|---|
| Settings | 100 | 32 | 68 |
| Games | 50 | 18 | 32 |

### Routes B and C: not run

- B (ALLaM-7B-Instruct): `huggingface.co` is blocked by this session's network proxy (HTTP 403 on CONNECT).
- C: `us-south.ml.cloud.ibm.com` (watsonx), `api.fanar.qa` (Fanar), `ollama.com` and `registry.ollama.ai` are blocked the
  same way.
- No workaround was tried, such as mirrors.

To run B later on a machine that can reach Hugging Face:
- Use a community Q4 GGUF of ALLaM-7B with llama.cpp. A 7B Q4 file is about 3.6 GiB, and a 4-core CPU gives about 2 to 6
  tokens per second, so 150 short strings take about 15 to 30 minutes.
- Put its output in column `B_allam` and judge it the same way.

## Model and system table

| Candidate | Licence | Sizes / weights | How to run | Cost | Saudi dialect |
|---|---|---|---|---|---|
| ALLaM-7B-Instruct-preview (SDAIA) | "see LICENSE"; exact name not confirmed [1] | 7B, open, 4K context [1] | HF safetensors; community GGUF/Ollama builds [2][3] | free locally | No official Saudi-dialect claim. A third-party LoRA raised the Saudi-dialect rate from 48% to 84%; its weights were not released [4] |
| ALLaM 13B on watsonx | IBM model card [5] | 13B, hosted only | IBM Cloud account + key | ≈ $0.0018 per 1K tokens (one IBM page says $0.001908) [6][7] | not stated |
| Jais family 2024 / Jais 2 (Inception) | Apache 2.0; Jais-2-70B is gated [8][9] | 590M to 70B; Jais 2 in 8B and 70B [9][10] | HF weights | free locally | Jais 2: 17 dialects incl. Gulf; Saudi not named [11] |
| AceGPT v2 | Apache 2.0; v1 had a custom licence [12][13] | 7B to 70B | HF weights; community GGUF for v1-7B [14] | free locally | not stated |
| SILMA 9B / Kashif 2B | Gemma licence [15][16] | 9B; Kashif 2B is for RAG only | HF weights; community GGUF [17] | free locally | not stated |
| Fanar-1-9B / Fanar-2-27B (QCRI) | Apache 2.0 [18][19] | 9B, 27B open | HF weights; API by request form [20] | API price not published | "dialectal QA" in the instruct card [20] |

Running a quantised 7B in this container would fit: 15 GB RAM and 4 cores at about 2 to 6 tokens per second, which is
readings from llama.cpp reports, not measured here [21][22]. The weights could not be downloaded here.

## Datasets and benchmarks

| Name | What it is | Licence |
|---|---|---|
| SADA (SDAIA with the Saudi Broadcasting Authority) | about 668 h of Saudi-dialect speech, audio only [23][24] | CC BY-NC-SA 4.0, non-commercial; an HF mirror says CC BY 4.0 |
| AraDiCE (QCRI) | benchmarks in Gulf, Levantine and Egyptian dialects, plus a culture set [25][26] | CC BY-NC-SA 4.0 |
| Palm (UBC-NLP, ACL 2025) | instructions in MSA and dialect for all 22 Arab countries, including Saudi Arabia [27] | not found |
| NADI 2020–2024 | dialect identification of tweets, with Saudi at country and province level [28][29] | not found |

None of these measures how natural UI copy sounds. A small set of our own would serve better: the 50 pairs picked here,
after the owner checks them.

## Reference glossary: real UI wording from Saudi apps

The wording comes from official help pages and guides as quoted by search results. Check the live page before reusing a
string. All of it is short MSA. No colloquial UI wording was found for any app.

| App | Wording (where) | Source |
|---|---|---|
| Absher | «نسيت كلمة المرور» (login) | [30] |
| Absher | «خدماتي»، «استعلامات»، «لوحة المعلومات»، «الخدمات العامة» (menus) | [31][32] |
| Absher | «تجديد رخصة القيادة»، «اختيار الرخصة»، «التالي» (licence-renewal flow) | [33] |
| Tawakkalna | «صحح بياناتك»، «معلوماتي»، «الإعدادات» | [34] |
| Tawakkalna | «الخدمات المفضلة»، «البطاقات المفضلة» | [35] |
| Sehhaty | «المواعيد عن بُعد»، «استعراض الإجازات المرضية والتقارير الطبية» | [36] |
| stc (mystc) | «خطوطي»، «الفواتير»، «ادفع»، «تصدير»، «رصيد الرقم»، «شحن» | [37] |
| Nafath | «تحقق من هويتك باستخدام تطبيق نفاذ» | [38] |
| Jahez | «أسرع تطبيق توصيل طلبات من المطاعم في السعودية» (store description) | [39] |
| HungerStation | «إلغاء الطلب» (third-party guide only) | [40] |
| Shahid | «مساحتي»، «إدارة الأجهزة»، «إلغاء الاشتراك»، «إدارة الملف الشخصي للأطفال» | [41][42][43] |

Not found: Saudi-colloquial UI strings from Jahez or HungerStation, and official Arabic help text from HungerStation.
A survey reported that 85% of Saudis prefer the "white" dialect to regional slang in ads [44].

## Sources

1. https://huggingface.co/ALLaM-AI/ALLaM-7B-Instruct-preview
2. https://huggingface.co/eltay89/ALLaM-7B-Instruct-GGUF
3. https://ollama.com/iKhalid/ALLaM:7b-q4_K_S
4. https://arxiv.org/abs/2508.13525v1
5. https://ibm.com/docs/en/SSYOK8/wsj/analyze-data/assets/ALLaM-1-13b-instruct-model-card.pdf
6. https://www.ibm.com/watsonx/developer/get-started/models
7. https://www.ibm.com/docs/en/SSYOK8/wsj/analyze-data/fm-models.html
8. https://huggingface.co/inceptionai/jais-family-13b
9. https://huggingface.co/inceptionai/Jais-2-70B-Chat
10. https://mbzuai.ac.ae/news/inception-cerebras-and-mbzuai-release-jais-2-the-next-generation-of-the-worlds-leading-arabic-open-weight-llm/
11. https://www.khaleejtimes.com/uae/largest-arabic-ai-model-jais-2-what-it-can-do
12. https://huggingface.co/FreedomIntelligence/AceGPT-v2-8B-Chat
13. https://huggingface.co/octoai/AceGPT-13B-SafeTensors/blob/main/LICENSE.txt
14. https://huggingface.co/abdullah/AceGPT-7B-chat-GGUF
15. https://huggingface.co/silma-ai/SILMA-9B-Instruct-v1.0
16. https://huggingface.co/blog/karimouda/introducing-silma-kashif-2b-instruct-rag-model
17. https://huggingface.co/goodasdgood/SILMA-9B-Instruct-v1.0-IQ4_NL-GGUF
18. https://huggingface.co/QCRI/Fanar-1-9B
19. https://huggingface.co/QCRI/Fanar-2-27B-Instruct
20. https://qcai.qcri.org/?p=3548
21. https://github.com/ggml-org/llama.cpp/discussions/3167
22. https://sources.debian.org/src/llama.cpp/6641%2Bdfsg-1/tools/llama-bench/README.md
23. https://huggingface.co/datasets/m6011/sada2022/blob/main/README.md
24. https://arxiv.org/abs/2508.12968
25. https://huggingface.co/datasets/QCRI/AraDiCE
26. https://aclanthology.org/2025.coling-main.283
27. https://aclanthology.org/2025.acl-long.1579
28. https://arxiv.org/pdf/2010.11334.pdf
29. https://aclanthology.org/2024.arabicnlp-1.79.pdf
30. https://www.capsulaa.sa/saudi-arabia-services/نسيت-كلمة-المرور-في-أبشر-خطوات-تغيير-كل/
31. https://www.absher.sa/NewServices
32. https://absher.sa/
33. https://my.gov.sa/ar/services/99736
34. https://ta.sdaia.gov.sa/faq/
35. https://ta.sdaia.gov.sa/assets/files/Tawakkalnaar.pdf
36. https://www.moh.gov.sa/eServices/Sehhaty/Pages/default.aspx
37. https://www.stc.com.sa/ar/personal/lifestyle/stcapps-and-services/applications/mystc.html
38. https://zatca.gov.sa/ar/e-participation/Co-Creation/Pages/IdeasList.aspx
39. https://apps.apple.com/sa/app/jahez/id1137352156?l=ar
40. https://www.capsulaa.sa/saudi-arabia-services/كيف-اطلب-من-هنقرستيشن-من-التطبيق-والبط/
41. https://help.shahid.mbc.net/s/article/How-can-I-pair-my-smart-TV-with-Shahid-account?language=ar
42. https://help.shahid.mbc.net/s/article/Subscription-Cancellation?language=ar
43. https://help.shahid.mbc.net/s/article/How-can-I-reset-remove-my-parental-pin-code?language=ar
44. https://www.al-madina.com/ampArticle/483394
