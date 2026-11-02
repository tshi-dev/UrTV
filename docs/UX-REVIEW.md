# UX review: a family using the TV with a remote

A review and some prototypes. No app code was changed: the owner picks first. The prototypes are in `notes/ux-review/`
(`proto.html`, 1920×1080 PNGs). I read the code on `next` (launcher: home, Control Center, tasks, tilawah, parental; app:
SettingsHub and SettingsActivity) and the settings prototype on `work/notes`. There is no `data-screens` branch with
emulator screenshots, so I counted presses from the code. A count is the D-pad and OK presses from the home screen with
the first app focused.

Who it is for: a parent who sets things up once, an older relative who only wants the adhan and the Quran, and a kid who
wants YouTube.

## Problems, ranked by impact

| # | Problem | Who it hits | Where | Presses today → after | Fix |
|---|---|---|---|---|---|
| 1 | **Parental control can only be opened by a long press on an app.** No button, tile or settings row opens it (only `MainActivity.showAppMenuNow` has «الرقابة الأبوية»). | Parents | Home, app menu | Hidden (long OK, ▼ ×6, OK) → 6 (Control Center or Settings › المزيد › الرقابة الأبوية) | Add a «الرقابة الأبوية» card in Settings › المزيد and a row in Control Center's «إعدادات تلفزيونك». Make the page a setup card with two buttons (`ux-parental-off.png`, `ux-parental-on.png`). |
| 2 | **Moving, hiding or locking an app needs a long press.** Nothing on screen says the app menu exists. Older relatives never find it. | Everyone | Home, app rows and favourites | Hidden → 2 (▼ to «⋮ خيارات» under the app, OK) | A «⋮ خيارات» button under the focused app, and a «⋮» tile at the end of each row. Both open a small light box just above the app, with no title: three text rows (انقل، أخفِ، اقفل), then one row with two icon buttons. ★ is filled when the app is a favourite (OK removes it) and an outline when it isn't (OK adds it); ⓘ, to its left, opens the app info. See `ux-apps-options-v4.png` (a favourite, focus on ★) and `ux-apps-options-v4-off.png` (not a favourite, focus on ⓘ). OK on the app already opens it, so «افتح» is not in the box. Uninstall goes through the app info. Earlier versions: `ux-apps-options.png`, `-v2.png` and `-v3.png`. Keep the long press as a shortcut to the same box. |
| 3 | **Two names for two locks.** «الرقابة الأبوية» (the app PIN) and «حماية العائلة» (family DNS in Settings › المزيد) sound like the same thing. A parent opens the wrong one. | Parents | Settings › المزيد | — | Rename the DNS one to «تصفية المواقع». Put both on the parental page: one row there opens the site filter. |
| 4 | **Wi-Fi and Bluetooth are far apart in Control Center.** Wi-Fi is tile 3 and Bluetooth tile 7 (after speed test, sound and mirroring). People look for them together. | Everyone | Control Center | Bluetooth: 6 moves → 1 | Put واي فاي first and البلوتوث right next to it (`ux-control-center.png`). |
| 5 | **The tilawah, adhan settings and update are hidden in the app's long-press menu.** When «زر تلاوة وأذان» is hidden, the player and the adhan settings are reachable only from there. «تحديث تلفزيوني» is in that menu too. | Older relatives | App long-press menu | Hidden → 3–4 | Settings already has «الأذان», «تلاوة وأذان» and «تحديث تلفزيوني» buttons. Remove them from the app menu so that menu is only about the app. |
| 6 | **Turning the adhan on takes about 12 presses.** The path is Control Center › إعدادات تلفزيونك › الأذان › the switch. | Older relatives | Home → Settings | ~12 → ~8 | Keep the wide «الأذان» card first in Settings (already on `next`). In Control Center, put «إعدادات تلفزيونك» right after Wi-Fi/Bluetooth's row, not last. |
| 7 | **«أطفئ الآن» says «مطوّلًا: تفعيلها».** When the sleep service is off, the tile asks for a long press to fix it. | Parents | Control Center | Hidden → 2 | OK shows the short notice with a «فعّل الخدمة» button under it. No long press. |
| 8 | **«شاشة التوقف» tile: «مطوّلًا: الإعدادات».** A long press is the only way to its settings from Control Center. | Parents | Control Center | Hidden → 4 (إعدادات تلفزيونك › البطاقات › شاشة التوقف) | The tile only starts the screensaver. Its settings live under Settings › البطاقات, which already exists. |
| 9 | **The Settings key: a short press opens the TV's settings, a long press opens ours.** Nobody knows the long press. | Parents | Remote | Hidden → 2 (Control Center › إعدادات تلفزيونك) | Keep it as a shortcut. The visible way is the Control Center tile. `ParentalPolicy.OUR_SETTINGS` stays. |
| 10 | **Changing the city is deep.** The path is Control Center › إعدادات تلفزيونك › الموقع والصلاة › المدينة › a long list. | Parents | Settings | ~12 → ~10 | Fine once a year. The city search (`CityMatch`) is the right tool; open it with the keyboard already up. |
| 11 | **Dark glass panels everywhere.** The text is grey on dark glass, and the focus is a full white fill that flashes on every move. | Older relatives, people sitting far from the TV | Home, Control Center, Settings | — | The lighter look (`ux-light-home.png`, `ux-light-settings.png`): white cards on sand, hairline borders, and focus shown as a green 3 px ring on a pale green tint. |
| 12 | **«المهام» and «تلاوة وأذان» look the same as «المصادر».** A kid presses everything in the top row. | Kids | Home top row | — | Kids mode already hides «مركز التحكم». Also hide «المهام» in kids mode. |

Tasks that already work well: open an app (1 press: OK), pick a reciter (تلاوة وأذان → the reciters grid, about 3
presses), use the phone remote (Settings › التحكم من الجوال shows the QR), and send a report (Settings › أرسل تقرير).

## Every long press, and where it should go

| Long press | What it does today | Where it should go | Keep the long press? |
|---|---|---|---|
| OK on an app card (`MainActivity` card listener) | The app menu: edit favourites, edit apps, update, add/remove favourite, reset favourites, tilawah, adhan settings, parental | A «⋮ خيارات» button under the focused app, plus a «⋮» tile at each row's end, opening the box: انقل، أخفِ، اقفل, then ★ (favourite on or off) and ⓘ (app info). The global items move to Settings. | Yes, as a shortcut to the same sheet |
| OK on «يُتلى الآن» (`nowCard`) | Opens the player | OK opens the player. ◀ ▶ stay previous and next. Play and pause are in the player. | No |
| OK on Control Center «أطفئ الآن» | Accessibility settings, to turn the sleep service on | OK shows the notice with a «فعّل الخدمة» button | No |
| OK on Control Center «شاشة التوقف» | The screensaver's settings | Settings › البطاقات › شاشة التوقف | No |
| The remote's Settings key | Short press: the TV's settings. Long press: UrTV settings. | The Control Center tile «إعدادات تلفزيونك» | Yes, as a shortcut |
| (old) OK on the screensaver pill | Its settings (`PillText.SAVER` still says «مطوّلًا: الإعدادات») | The pill is gone. Delete the stale string. | — |

Holding ◀ ▶ to seek in the player, and holding the arrows to glide along a row, are normal TV behaviour. Keep them.

## The lighter design

- **Surfaces:** sand paper `#F6F4EF`, white cards, `#E3DFD5` hairline borders. On the home screen the photo stays at the
  top and fades into sand toward the app row. The panels are white at 86–90 %, not dark glass.
- **Focus:** a 3 px DGA green `#1B8354` ring on a pale green `#EEF6F1` tint. Apps get a ring and a 4 % lift. No white
  fill and no glow.
- **«On»:** toggle I (three Sadu triangles before the name, and «مفعّل» in green). «Off» is grey «متوقف» with no
  triangles.
- **Buttons:** rounded rectangles with a 14 px radius. No pills, eyebrows, chevrons or hint lines.
- **Font in the PNGs:** IBM Plex Sans Arabic stands in for Thmanyah, which is in a private repo.

## Skills used

- **theme-factory:** none of its 10 presets fit a fixed DGA palette, so I used its "make your own theme" step. It gave
  one named token set, «نهار نجد» (Najd Daylight), defined once in `:root` and used by all six screens. That is what
  keeps the screens consistent.
- **artifact-design, canvas-design, frontend design:** not used. They are for published pages or poster art, and these
  are TV mockups rendered with Playwright.
