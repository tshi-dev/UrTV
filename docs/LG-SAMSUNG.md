<div dir="rtl">

# تلفزيونك على LG وSamsung

> **تجريبي، وما انجرّب على تلفزيون LG ولا Samsung حقيقي.** انبنى وانفحص على الكمبيوتر بس.

### وش يقدّم

تلفزيونك هنا **تطبيق** تفتحه من قائمة تطبيقات التلفزيون (وعلى Samsung تقدر تفعّل «Autorun Last App» فيفتح معه لما تشغّل التلفزيون):
مواقيت الصلاة، التاريخ الهجري، شريط الأذان **وصوت الأذان ما دام التطبيق مفتوح**، والطقس، والبطاقات، والصور، بالعربي ومن اليمين لليسار،
وشاشة إعدادات صغيرة بالريموت (المدينة، طريقة الحساب، البطاقة، الساعة، الأذان).

### حدوده

- **ما ينظّف التلفزيون ولا يسرّعه.** أدوات التنظيف عندنا تشتغل بـ ADB حق أندرويد، وLG وSamsung ما فيها هذا، فما يقدر أي تطبيق يوقف تطبيقات الشركة أو إعلاناتها.
- **ما يبدّل الشاشة الرئيسية** (ما يسمحون لأي تطبيق).
- **ما يأتي الأذان والتطبيق مو مفتوح.** تطبيق الويب يتوقف إذا طلعت منه.
- ما فيه تحكم من الجوال، ولا تحديث من التلفزيون، ولا قرآن، ولا قفل عائلة، ولا مباريات وأسهم ومهام (هذي لتطبيقات أندرويد).
- **وضع المطوّر في LG ينتهي بعد نحو ٤٠ يوم**؛ تجدّده بزر EXTEND في تطبيق Developer Mode.
- يحتاج LG بنظام webOS 5 أو أحدث (موديلات ٢٠٢٠)، أو Samsung بنظام Tizen 5 أو أحدث (٢٠١٩). الأقدم يكتب رسالة بدل الشاشة.

### تبي تلفزيونك كامل؟

وصّل بالتلفزيون عن طريق HDMI جهاز **Android TV / Google TV** (مثل Chromecast with Google TV أو Google TV Streamer أو جهاز Xiaomi)
وركّبه مثل أي تلفزيون أندرويد: [README](../README.md). هناك الشاشة الرئيسية والأذان بالخلفية والتنظيف والجوال كلها.

الخطوات بالإنجليزي تحت (التركيب على LG وعلى Samsung).

</div>

---

# UrTV on LG (webOS) and Samsung (Tizen)

> **BETA. NOT TESTED ON A REAL LG OR SAMSUNG TV.**
> Everything here was built and checked on a computer: the packages build, their manifests are valid, and the page runs in a desktop
> Chromium with the keys a webOS or Tizen remote sends (`tests/tvports_test.py`, `tests/tvports_page.test.mjs`). Nobody has put it
> on a television yet. Expect surprises, and tell us what you saw (see [Reporting](#reporting)).

**What it is.** UrTV as an **app** on the TV: open it from the app list (on Samsung, **Autorun Last App** opens it when the TV turns
on). Prayer times, the Hijri date, the adhan bar and the **adhan sound while the app is open**, weather, cards and photos, in Arabic.

**What it is not.** It does **not** clean or speed up the TV: our tools work through Android's ADB, which LG and Samsung do not have,
so no app can turn off the maker's apps or ads. It does **not** replace the home screen (neither maker allows it), and it cannot play
the adhan in the background. No phone control, no update from the TV.

**For the full UrTV on an LG or Samsung TV**, plug an Android TV / Google TV box into it over HDMI (Chromecast with Google TV, Google
TV Streamer, a Xiaomi box) and set that box up like any Android TV (see the [README](../README.md)).

The app's packaging is adapted from [Free My TV](https://github.com/simbasjungles/free-my-tv) (MIT); see
[THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md).

## What works, what does not

| | |
|---|---|
| Arabic, right to left, the clock in 3 sizes, 12 or 24 hours, Hindi or Latin digits | works (checked in a desktop browser) |
| Prayer times (computed on the TV, no account), the next prayer, Hijri date (Umm al-Qura), the slim adhan bar | works (checked) |
| The cards: verse and hadith, wisdom, adhkar, prayer times, prayers in a row, forecast, hourly, the year | works (checked) |
| Weather, markets line, photos | need the TV's internet; the five bundled photos work without it |
| The adhan **as sound** | works **only while UrTV is the app in front**: plays at each prayer time, any key stops it |
| The settings (city, calculation, card, clock, adhan) | on the TV, with the remote; kept on the TV |
| **Replacing the home screen** | **no**: LG and Samsung do not allow it |
| **The adhan when the app is not in front** | **no**: a web app is stopped when you leave it. Keep it open (or use the Android setup instead) |
| Phone control, the Quran player, the family lock, the followed clubs and matches, tasks, stocks | **no**: those are the Android apps' |
| The private Thmanyah fonts | only if you have them in `assets/fonts/` when you build; otherwise the TV's own Arabic font, which looks different but reads fine |
| LG Developer Mode | **expires**: the app disappears after about 40 days unless you press **EXTEND** in the Developer Mode app (see below) |
| Staying on: the TV's own screen saver or sleep timer | **unknown**: the app does not ask the TV to stay awake; test it and tell us |

**Which TVs.** The page needs a browser from about Chromium 63: **LG webOS 5 (2020) and newer, Samsung Tizen 5.0 (2019) and newer**. The
installers speak to older ones too (webOS 3 and 4, Tizen 3 and 4) and the app opens there, but it shows a short Arabic message
instead of the screen: the page cannot run in those browsers.

## The settings screen

Press **OK** on the remote. Up and Down choose a row; **Left** goes on in the list and **Right** goes back (the screen is right to
left); OK runs a row such as «تجربة الأذان الآن»; **Back** saves and closes (the page starts again with the new settings, a few
seconds). Rows: country or region, city (the same list as the Android screensaver, with each city's time zone), the calculation
(automatic by country), «تعديل الفجر» … «تعديل العشاء» (each prayer moved by whole minutes, −20 to +20, as in the Android
app: Left adds a minute, Right takes one off; the times, the countdown and the adhan move with it), the card beside the clock, the clock size, 24 hours, digits, the bar before the adhan, the adhan
(on or off, the muezzin, Fajr's muezzin, the volume).

**Back** with nothing open leaves the app. The adhan is **off** until you turn it on.

The adhan recordings are the ones the Android app uses, streamed from the public-domain «Azaan of World Mosques» collection on
Archive.org when needed; nothing is bundled. It needs the TV's internet.

## Install on LG (webOS)

You need: the TV and a computer on the **same Wi-Fi**, Python 3 and OpenSSH (`ssh`, `ssh-keygen`) on the computer.

1. **On the TV:** install **Developer Mode** from the LG Content Store. Sign in with a free LG developer account
   (<https://webostv.developer.lge.com>). Turn **Dev Mode Status** on; the TV restarts. Open the Developer Mode app again and turn
   **Key Server** on. It shows the TV's IP address and a **passphrase**.
2. **On the computer:**

   ```
   git clone https://github.com/tshi-dev/UrTV && cd UrTV
   python3 webos/build.py
   python3 webos/install.py <TV-IP>
   ```

   The first time, `install.py` fetches the TV's key from the Key Server and asks for the passphrase (typed here, not stored); the
   unlocked key stays in your home folder's `.ssh` directory so updates need neither. It copies the package over SSH (port 9922), installs it
   with the TV's own installer and opens it.
3. The app is named «تلفزيونك» in the TV's app list. To remove it: `python3 webos/install.py <TV-IP> --uninstall`.

**The 40 days.** Developer Mode keeps its apps only for a limited time (about 40 days, and the Developer Mode app shows the time left).
Before it ends, open the **Developer Mode** app and press **EXTEND**; if it already ran out, turn Dev Mode Status on again and run
`webos/install.py` again. This is LG's rule, not ours; the only way around it is an app from the LG store, which this is not.

## Install on Samsung (Tizen)

Samsung TVs only install **signed** apps. Two ways:

**A. Signed build, this computer to the TV (needs Tizen Studio).**

1. In Samsung's **Tizen Studio**, make a certificate profile (Certificate Manager: a Samsung certificate for a TV, with the TV's DUID
   added). `tizen/build.py` does not make or keep any key.
2. **On the TV:** open **Apps**, type **1 2 3 4 5** on the remote, turn **Developer mode** on, type **this computer's IP** as **Host PC
   IP**, and restart the TV.
3. **On the computer:**

   ```
   python3 tizen/build.py --sign <profile>
   python3 tizen/install.py <TV-IP>
   ```

   `install.py` talks Samsung's SDB (port 26101) itself, so Tizen Studio is only needed for the signing. To remove the app:
   `python3 tizen/install.py <TV-IP> --uninstall`, or on the TV: Apps, Settings, UrTV, Delete.

**B. TizenBrew Installer.** A third-party tool that signs and installs a `.wgt` with your own Samsung certificate on 2023 and newer
TVs. Build the package with `python3 tizen/build.py` (it is not signed; the installer signs it) and follow that tool's own
instructions. We have not tried it.

To make the TV start into UrTV: turn on **Autorun Last App** in the TV's settings (General, Smart Features; where it is changes by
model year).

## Build notes

- `python3 webos/build.py` writes `UrTV-webos.ipk`; `python3 tizen/build.py` writes `UrTV-tizen.wgt`. Both take `--preview` to leave the
  files in `out/webos` or `out/tizen`, to open `index.html` in a desktop browser (open it from a small web server if the browser
  blocks local files).
- The page is `dist/tv.html` (built by `python3 src/build.py`), with its fonts and bundled photos packaged instead of fetched from
  GitHub, plus `tvports/shell.js` (settings, keys, adhan) and `tvports/data.js` (the city list and the adhan voices, made from the
  Android apps' own lists at build time). The package is about 2 MB.
- **Fonts are private.** If `assets/fonts/Thmanyah*.woff2` are on your machine they are packaged; if not, the page uses the TV's
  Arabic system font. Never commit them.
- Tests: `python3 tests/tvports_test.py` (packages, manifests, sizes) and `node tests/tvports_page.test.mjs` (the page with simulated
  webOS and Tizen keys); both are in `bash tests/run.sh`.

## What needs a real TV

Not known until someone tries: that each installer completes on each model; that the app's keys behave (Back leaves the app on both
platforms; Tizen's `registerKey`); that sound plays by itself at prayer time (some browsers refuse sound until a key was pressed once
since the app opened; the app unlocks it on the first key press); that the Hijri date (`Intl` with Umm al-Qura) is there on every TV's
browser; that the page is fast enough on a low-end TV; whether the TV's screen saver or sleep timer interrupts the app; the font look
without Thmanyah; and how long Developer Mode really lasts on your model.

## Reporting

If you tried it: say the TV's brand, model year and system version, what happened at each step and what you saw on the screen.
If an agent is helping you, it reports a problem only with `optimize/share.sh --preview --problem --owner "<your words>" "<what was
checked>"`, shows you what it will send, then `optimize/share.sh --send`: never a GitHub issue or comment (see [AGENTS.md](../AGENTS.md)).
No IP address, Wi-Fi name or serial goes in a report.
