step 9, part A — in progress (A3 typing and A5 left)

Version: before 3.148, after "version: 3.149" (install.sh all; «Home is already the new launcher: nothing to switch»; no !! line).

| # | OK/problem | shots | note |
|---|---|---|---|
| A1 | OK | look-row, look-glass, look-graphite, look-navy, look-clean, light-pick | «المظهر» is the first row of «الشاشة الرئيسية» («زجاجي · داكن»); it opens «الشكل» / «الإضاءة»; each of the 4 shapes shows its own preview; «الإضاءة»: داكن، فاتح، تلقائي. Small: in the glass preview the focused mini-tile «الموقع» is white text on white |
| A2 | OK | look-<look>-<dark|light>-<home|hub|row>, looks-<look>-<dark|light>-dialog | all 4 looks x dark/light: main menu, section with a focused row and «طريقة الحساب» dialog all readable; focused item readable (gold/amber/dark/green fill with dark or white text); dialogs solid. The home screen looks the same in every look (only the settings change) |
| B1 | shots made | 32 shots | made with a demo city (مكة المكرمة), real city put back, look glass + dark. Sent to the owner in the chat to choose. NOT pushed to GitHub: the owner's own rule for this run is text only; waiting for his OK. Same for the icons task (X1/X2), and ثمانية and سين are not installed on this TV |
| (3.150) | installed | — | 3.150 installed for A3-A5 |
| A3 pair | OK | p9-ask, p9-allowed | owner typed the code in the PC's Chrome; TV asked «سماح/رفض» showing the PC's address; «سماح» → page «متصل · بقي 30 دقيقة»; no screensaver during pairing |
| A3 type | blocked | p9-yt3, p9-yt4 | YouTube search page open (owner said to pick his profile). «قرآن» sent from the page; log: `text request: mode=replace search=false len=4` / `foreground=other` / `service=off` / `result=service_off`. The typing service «تلفزيونك: الكتابة من الجوال» is Off in Accessibility (start line has only Key Mapper). Turning it on is a security setting: left for the owner (one OK press, he is away). The page shows a clear hint to turn it on: good |
| A4 | problem | — | «الصلاة القادمة» + «حفظ البطاقات»: `UrTVPhone: home_cards sent=1 saved=1 same=true`, home_panel nextprayer: OK. Then «بدون» + «حفظ البطاقات»: FIRST press did nothing (no UrTVPhone line, home_panel still nextprayer); SECOND press: `home_cards sent=0 saved=0 same=true`, home_panel none. Guess: the page's /api/state refresh re-renders the selects (element refs were replaced) and the first save is lost |
| A5 | waiting | — | phone control still on (session ends by itself after 30 min); will turn off and drop the pairing after A3 |
| note | check | p9-acc | the screensaver came on about 3 s after Android's Accessibility page was opened by am start (TV had been idle before) |

## Voice (V1-V3)
| # | Result |
|---|---|
| V1 | /v1/models gave 401 (key cannot list models). eleven_v3 with mp3_44100_192 gave 403: "Output format mp3_44100_192 is only available on the Creator tier and above". Ran with EL_MODEL=eleven_v4 and mp3_44100_128 (only change, local copy): 26/26 ok |
| V2 | 26 files, none under 5 KB (1.3 MB total); owner listened: "That's good" |
| V3 | pushed tester/voice/01.mp3 ... 26.mp3 |

## Voice round 3 (N2)
| # | Result |
|---|---|
| N2 | voice 0 (current) + voices 1-4, each on eleven_v4 and eleven_v3 (mp3_44100_128), 2 lines each: 20 files in tester/voice-test/ (voice-<n>-<1|2>.mp3 = v4, voice-<n>-v3-<1|2>.mp3 = v3). All ok |
| N3 | waiting: v4 files sent to the owner to pick |

## Voice round 4 FINAL (F1-F2)
| # | Result |
|---|---|
| F1 | voice rpGHcNQJvO8dFNNFNj1v, eleven_v4, lines-v3.txt, EL_SPLIT=1: 38 files, none under 5 KB (1.6 MB) |
| F2 | pushed tester/voice-v3/*.mp3 (38) |
