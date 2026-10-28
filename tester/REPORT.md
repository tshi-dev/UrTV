step 9, part A (no owner) — in progress

Version: before 3.148, after "version: 3.149" (install.sh all; «Home is already the new launcher: nothing to switch»; no !! line).

| # | OK/problem | shots | note |
|---|---|---|---|
| A1 | OK | look-row, look-glass, look-graphite, look-navy, look-clean, light-pick | «المظهر» is the first row of «الشاشة الرئيسية» («زجاجي · داكن»); it opens «الشكل» / «الإضاءة»; each of the 4 shapes shows its own preview; «الإضاءة»: داكن، فاتح، تلقائي. Small: in the glass preview the focused mini-tile «الموقع» is white text on white |
| A2 | OK | look-<look>-<dark|light>-<home|hub|row>, looks-<look>-<dark|light>-dialog | all 4 looks x dark/light: main menu, section with a focused row and «طريقة الحساب» dialog all readable; focused item readable (gold/amber/dark/green fill with dark or white text); dialogs solid. The home screen looks the same in every look (only the settings change) |
| B1 | shots made | 32 shots | made with a demo city (مكة المكرمة), real city put back, look glass + dark. Sent to the owner in the chat to choose. NOT pushed to GitHub: the owner's own rule for this run is text only; waiting for his OK. Same for the icons task (X1/X2), and ثمانية and سين are not installed on this TV |
| A3-A5 | waiting | — | pairing needs the owner to type the code |

## Voice (V1-V3)
| # | Result |
|---|---|
| V1 | /v1/models gave 401 (key cannot list models). eleven_v3 with mp3_44100_192 gave 403: "Output format mp3_44100_192 is only available on the Creator tier and above". Ran with EL_MODEL=eleven_v4 and mp3_44100_128 (only change, local copy): 26/26 ok |
| V2 | 26 files, none under 5 KB (1.3 MB total); owner to listen |
| V3 | pushed tester/voice/01.mp3 ... 26.mp3 |
