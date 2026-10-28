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

## Adhan (H1-H3)
| # | Result |
|---|---|
| H1 | Wikimedia Commons, Category:Adhan: "Adhan in Istanbul", own work, {{self|cc-zero}}, 50 s city adhan. Only CC0 own-work file in the category (others are CC BY / BY-SA, or PD claimed for a singer) |
| H2 | downloaded; tester/adhan/SOURCE.md written |
| H3 | pushed the audio track (copied, not re-encoded) + SOURCE.md; video not pushed (63 MB) |

## Voice FINAL by the owner (replaces voice-v3)
| # | Result |
|---|---|
| O1 | The owner cloned his own voice (ElevenLabs Instant Voice Clone "UrTV owner", id wfj1guOwi64ozn1ggUGY, from his own 7:41 recording) and made the 26 lines himself on the ElevenLabs site with eleven_v4, with his own small wording fixes (e.g. 11 «أي زر تضغط راح يوقفه», 14 «على قد ما نقدر», 18 «راح يطلب رمز») |
| O2 | Cut at «…» into 38 parts NN-k.mp3 (same names as voice-v3); every part checked by transcription; 18 cut at its real pause |
| O3 | pushed tester/voice-owner/*.mp3 (38). Owner listened: "That's good". Use these instead of tester/voice-v3/ |

## step 10 (apk-next 3.152): R1-R3, R5, Z done; R4 left for daytime
Version: home 3.152, screensaver 3.152 (install.sh all; no new !! line).
Typing service: the owner ran `optimize/dock.sh type-setup` himself; TypeService now in enabled_accessibility_services. Left ON as asked.

| # | OK/problem | shots | note |
|---|---|---|---|
| R1 YouTube | problem (small) | remote-yt…yt6 | On YouTube's profile picker the page's OK and ▶ log `route=global result=done` but nothing moves or opens. Inside YouTube: ▶ moves the focus, OK opens the video, Back returns, Home goes to our home: OK |
| R1 TV settings | OK | remote-settings | ▼▼ moves the focus two rows; Back leaves; Home OK |
| R1 Shahid | OK | remote-shahid2-3 | ▼ and ▲ move the focus on Shahid's first screen; Home OK |
| R1 log | — | — | `UrTVRemote: key=ok/right/down/back/home route=global result=done` (no content). Before the service was on, the page keys did nothing in other apps (expected) |
| R2 | OK | yt-search3 | `text request: mode=replace search=true len=4` / `foreground=other` / `service=on` / `root=present` / `search=started` / `result=searched`; YouTube shows the results for «قرآن» |
| R3 | OK (fixed) | — | «الصلاة القادمة»+save: `home_cards sent=1 saved=1 same=true`, home_panel nextprayer. «بدون»+save ONCE: `home_cards sent=0 saved=0 same=true`, home_panel none on the FIRST press |
| R4 | not done | — | it plays the adhan aloud at night; the owner asked to keep the volume at 0. To do in daytime (needs a new pairing) |
| R5 | OK | p10-r5b | «إيقاف التحكم من الجوال»: back to «تشغيل», the PC's pairing gone; service still on |
| Z | restored: yes (service left on, as asked) | — | home_panel none, glass + dark, updatechannel latest; TV sound muted (owner's wish) |
| note 1 | to look at | — | when YouTube opened, the TCL system killed sa.urtv.screensaver and then refused the home app starting its ConfigProvider: `TclAppBoot … calleeComponent='sa.urtv.screensaver/.ConfigProvider' … isAllow='false', reason='callee_does't_have_OP_ASSOC_START_permission'` and `Failed to find provider info for sa.urtv.screensaver.config` (repeated) |
| note 2 | OK | remote-shahid0 | the screensaver came on ~50 s after the last key while Shahid loaded (45 s setting); one ▼ from the page closed it |

Problems: 1 small (page keys don't act on YouTube's profile picker) + note 1. R4 left.
