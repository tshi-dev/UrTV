step: 9
from: UrTV supervisor
what: the last short test before the release, on apk-next 3.149 or newer: the four looks in «المظهر», YouTube search from the phone page, and «بدون». Part A needs no owner; part B is the owner's look at the four looks. Follow tester/RULES.md.

Before anything:
- Start lines as in step 8. Wait until apk-next version.txt is 3.149 or above; install with `APK_TAG=apk-next optimize/install.sh all`. Write down the version.

| # | Do | Shots (local) | Right when |
|---|---|---|---|
| A1 | Our settings → «الشاشة الرئيسية»: «المظهر» is the FIRST row. Open «الشكل»: move over the 4 options (زجاجي، فحمي، كحلي، نظيف), shot of each preview; then «الإضاءة» (داكن، فاتح، تلقائي) | look-row, look-glass, look-graphite, look-navy, look-clean, light-pick | first row; each option shows its own preview |
| A2 | For each look (`optimize/saver.sh look glass|graphite|navy|clean`), dark then light (`saver.sh appearance dark|light`): shots of the settings main menu with a focused tile, a section with a focused row, and the «طريقة الحساب» dialog. End on glass + dark | looks-*.png | every screen readable; the focused item readable; dialogs solid |
| A3 | Pair the PC's Chrome as in step 8 (keep-awake works now). YouTube search: open YouTube (pick the profile it shows only if it needs no owner choice; otherwise leave it on the profile screen), type «قرآن» in the page, «بحث»; `adb shell logcat -d -s UrTVType \| tail -20` | yt-search2 | YouTube's search opens with «قرآن» (log: search=started) |
| A4 | Page «الإعدادات»: first card «الصلاة القادمة», save; then «بدون», save. After each: `optimize/saver.sh show \| grep home_panel`, and `adb shell logcat -d -s UrTVPhone \| tail -10`; write what the page showed (any toast) | card-on2, card-off2 | home_panel nextprayer, then none; paste the UrTVPhone lines (counts only) |
| A5 | Turn phone control off, remove the PC's pairing | — | off |
| B1 | NO owner needed (owner's change): make the shots for him to choose from. First set a neutral demo city (`optimize/saver.sh city 'مكة المكرمة'` or the nearest the command accepts; write down the real one to put back) so no shot shows his city. Then for EACH look (glass, graphite, navy, clean) × (dark, light): 1) the HOME screen (favourites + «أكمل المشاهدة» visible), 2) settings main menu with a focused tile, 3) a section with a focused row. 24 shots at full 1920x1080, named look-<look>-<dark|light>-<home|hub|row>.png. Check each shot: no city other than the demo one, no Wi-Fi name, no person's name, no account/profile picture; if «أكمل المشاهدة» shows a show the family watched, that is fine only if it is a children-safe title, else crop the row out of the home shot. Push ONLY these shots to work/tester-report under tester/looks/. Put the real city and his look/appearance back afterwards | the 24 shots | pushed, nothing personal in them |
| Z | Restore everything from the start lines except the look/appearance the owner chose; `optimize/saver.sh updatechannel latest` | — | "restored: yes/no" |

REPORT.md: "step 9", the table, the owner's words, the UrTVType and UrTVPhone lines, any `!!` lines. No personal data. Push after part A and after part B; then tell the owner in Arabic that the test is finished and how many problems there were.

## Extra (owner OK'd, no owner needed): the real app icons for the release video
| # | Do | Right when |
|---|---|---|
| X1 | On the home screen, `adb exec-out screencap -p > home-full.png` (keep it LOCAL). Crop ONLY the four app tiles, each on its own, at full resolution: ثمانية, شاهد, YouTube, سين (find them in the favourites or «كل التطبيقات»; move focus away from them first so none has the focus ring). Save as icons/thmanyah.png, icons/shahid.png, icons/youtube.png, icons/seen.png | four clean tiles, nothing else from the screen in them |
| X2 | Push ONLY those four PNGs to work/tester-report under tester/icons/ (never the full screenshot) | pushed |

## Update from the supervisor (owner's answers, 28 Oct)
- The owner OK'd pushing the 32 looks photos (demo city, checked for nothing personal) to work/tester-report under tester/looks/, so the supervisor shows them to him. Only those photos; nothing else.
- IMPORTANT: work/tester-report was REWRITTEN (force-pushed) to take a city name out of its history. Before your next push: `git fetch origin work/tester-report && git reset --hard origin/work/tester-report` in your report checkout (your REPORT.md text is kept there, without the city), then add the photos and push normally.
- Never name a city in REPORT.md, not even the real one being "put back": write "real city put back".
- Icons: ثمانية and سين aren't installed, so crop only شاهد and YouTube (if they show) into tester/icons/; skip the others.

## Update 2 (28 Oct, afternoon)
- B1 is DONE by the owner himself on the TV ("all are good"): do NOT make or push the looks photos.
- A new build 3.150 is coming (a preview-colour fix). Install it (`APK_TAG=apk-next optimize/install.sh all`) once version.txt shows 3.150, then do A3 (YouTube search), A4 («بدون» with the UrTVPhone lines) and A5, plus X1/X2 for شاهد and YouTube only. Then Z, REPORT.md, push.

## Voice for the release video (owner's request, no owner needed)
The owner wants the narration made with his ElevenLabs account. The key is in his Windows environment as ELEVENLABS_API_KEY.
| # | Do | Right when |
|---|---|---|
| V1 | `python tester/voice/make_voice.py` (it picks ElevenLabs' newest model with Arabic on the account, v4 if there; write the model name it prints in REPORT.md; if the newest model fails with an error, run again with EL_MODEL=eleven_v3 and say so) from the repo checkout of work/tester (it reads tester/voice/lines.txt, 26 lines). NEVER print, echo, log or commit the key. If it says the key isn't set, open a new terminal; if still empty, tell the owner in Arabic that the key isn't in this terminal | 26 files in tester/voice/out/ (01.mp3 … 26.mp3) |
| V2 | Listen to 2-3 lines yourself if you can (or check sizes: none under 5 KB). If a line came out empty or broken, run it again | all 26 OK |
| V3 | Push ONLY tester/voice/out/*.mp3 to work/tester-report under tester/voice/ (fetch and reset to origin/work/tester-report first, as before). Never the key, never anything else | pushed |

## Voice, round 2 (owner's corrected text)
| # | Do | Right when |
|---|---|---|
| G1 | ق/g TEST first: `EL_MODEL=eleven_v4 EL_LINES=lines-gtest.txt EL_OUT=out-gtest python tester/voice/make_voice.py` (4 files: 01 = line 3 with ق, 02 = line 3 with گ, 03 = line 19 with ق, 04 = line 19 with گ). Use mp3_44100_128 as before. Play them to the owner (or push them: tester/voice-gtest/01-04.mp3) and ask him in Arabic: «أي نطق أقرب للنجدي: الأول (ق) أو الثاني (گ)؟» | owner picks |
| G2 | After his pick ONLY: tell the supervisor (REPORT.md "voice: ق" or "voice: گ"); the supervisor sends the final text file. Do not make the 26 lines before that | — |

## A free-to-use adhan for the video (owner's choice, no owner needed)
| # | Do | Right when |
|---|---|---|
| H1 | Find ONE adhan recording whose OWN recordist/uploader marked it CC0 or Public Domain: Wikimedia Commons (category "Adhan", open the file page and read its licence box: only "CC0" or "Public domain" by its author; NOT CC-BY-SA, NOT "PD" claimed for someone else's recording) or freesound.org with the licence filter "Creative Commons 0". Prefer a city/mosque adhan recorded from a distance (outdoor, with space), a full one or at least the first «الله أكبر» phrases, Sunni wording only (no extra phrases) | one file with a clean licence |
| H2 | Download the original file (wav/flac/ogg/mp3). Write tester/adhan/SOURCE.md: the page URL, title, author, licence exactly as shown, and the download date | done |
| H3 | Push ONLY that file and SOURCE.md to work/tester-report under tester/adhan/ (reset to origin/work/tester-report first, as before) | pushed |

## Voice, round 3: other Najdi voices (owner: the ق/گ spellings sounded ~90% the same)
The owner wants to try OTHER Saudi/Najdi voices from ElevenLabs' Voice Library.
| # | Do | Right when |
|---|---|---|
| N1 | Ask the owner in Arabic to open elevenlabs.io → Voices → Voice Library, filter Language: Arabic, Accent: Saudi (or search «نجدي» / "Najdi" / "Saudi"), Gender: male, and pick 3-4 voices that sound like a warm Najdi father; add each to "My Voices" and copy its Voice ID (not secret) into this chat. (If the key can read `GET https://api.elevenlabs.io/v1/shared-voices?language=ar&gender=male&search=saudi`, you may list candidates for him first; if it answers 401, just ask him.) | 3-4 voice IDs |
| N2 | For EACH id: `EL_VOICE=<id> EL_MODEL=eleven_v4 EL_LINES=lines-voicetest.txt EL_OUT=out-voice-<n> python tester/voice/make_voice.py` (2 lines: the home line and the prayer line). Also try the same with `EL_MODEL=eleven_v3` for the best one, since v3 sometimes keeps dialect better | 2 files per voice |
| N3 | Play them to the owner (name them clearly: voice 1, 2, 3…) and ask which one sounds most Najdi. Write in REPORT.md: the chosen voice id, the model (v4 or v3), and the owner's words. Then make all 26 lines of lines-v2.txt with that voice and model (`EL_VOICE=<id> EL_MODEL=<m> EL_LINES=lines-v2.txt EL_OUT=out-v2`), and push ONLY out-v2/*.mp3 to work/tester-report under tester/voice-v2/ | 26 files pushed |
