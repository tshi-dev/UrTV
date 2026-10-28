step 7, part A (no owner)

Version: before 3.145, after "version: 3.147" (installed with `APK_TAG=apk-next optimize/install.sh all`, screensaver first; «Home is already the new launcher: nothing to switch»; no !! line).

| # | OK/problem | shots | note |
|---|---|---|---|
| A1 | OK | — | idle (no screensaver yet): sa.urtv.screensaver 0% CPU, WebViews 0, no ServiceRecord. MemAvailable 781 MB, SwapFree 182 MB. After 2 min on Home the real screensaver started by itself; measured then: 0% CPU, WebViews 1, services ScreenDream + its sandboxed WebView (expected while it shows) |
| A2 | OK | — | 5x YouTube→Home: screensaver pid the same before/after (no restart); its CPU 0%; total 14%; MemAvailable 742 MB, SwapFree 257 MB. cpuinfo lines looked unrefreshed (same fault counts as A1) |
| A3 | OK / problem | key83, key176 | keyevent 83 → sa.urtv.screensaver/.SettingsActivity (ours). keyevent 176 → nothing, home stayed in front |
| A3b-e | not done | — | pairing needs the 6-digit code typed into the page; the agent does not type pairing codes into a browser. Left for the owner (phone) in part B |
| A4 | OK | phone-row, phone-ctl | «الجوال والتحديث» → «تحكم من الجوال» is the first row; it shows «تشغيل», so phone control is already OFF after the update. Not switched on |
| A6 | OK | — | familydns on: «is on (family.adguard-dns.com); the internet works through it»; private_dns_mode hostname; off: «put back by the home app», status off, private_dns_mode null (start value). Log: started → RUNNING; working → on (us); ENABLED → RESTORED |
| A7 | OK | adhkar4 | «أذكار المساء» with the full dhikr text, «مرة واحدة», page dots. Card set back to verse. (Somnambulator did not start the dream; used --preview) |

New problems seen in part A:
- HOME LAYOUT BROKEN on 3.147 (shots key176, home-now, home-after-home): the favourites row sits at the top over the greeting/clock/date (they show through it), and ONE huge «أكمل المشاهدة» card fills the lower 2/3 of the screen. Stays after pressing Home again.
- GLASS SETTINGS, focused tile unreadable (a4-hub, a4-pair, phone-row): the focused tile/row is solid white and its title, icon and summary are white too, so nothing on it can be read. Seen on every tile and on the rows inside sections.

Numbers vs step 6: screensaver CPU 45% → 0%; MemAvailable 570 → 742-781 MB; SwapFree 84 → 182-257 MB.
!! lines: none.
