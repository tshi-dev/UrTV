# Simple and the same: the rules for every screen a person sees

> **«ما تخلي شيء مكرر ولا متكرر ولا صعب. كلش بسيط. كلش مضبوط. كلش حلو.»** (the owner)

This is the test for every screen:
- **مكرر, duplicated:** the same information or control twice on one screen, or two ways to do one thing where one is enough.
- **متكرر, repeated:** the same row, word or explanation coming back on row after row, or page after page.
- **صعب, hard:** anything a person has to read at length, understand or work out.
- **بسيط, simple:** the five questions below.
- **مضبوط, precise:** aligned edges, the same spacing and the same sizes everywhere, nothing touching or clipped.
- **حلو, beautiful:** calm, with room to breathe, one look across all the screens.

The owner, again: «make sure nothing is complicated, that's our interface consistency job». These rules hold for every screen a
person sees: the home, the settings, تلاوة وأذان, the screensaver, the on-TV setup and its rescue and update boxes, the doctor app
(urtv-doctor), the phone page and the games.

The UI session reviews any visual change against them before it lands. To ask for a review, send mockups at 1080p and at 720p
with the font at 1.3. The Arabic wording rules are in [../STYLE-AR.md](../STYLE-AR.md); this file does not repeat them.

## The five questions, for every screen

1. **One primary action.** One thing is obviously the next step, and focus starts on it. Other actions are quiet (at most two),
   and Back is the remote's key, never a button. In a list of choices, focus starts on the current value.
2. **Nothing to understand first.** The person can finish without knowing why. There are no menu paths, versions, IDs, settings
   names or system words before the action. An explanation is one line, and only when the action could surprise them. A long
   instruction (a TV menu to open) is its own step with numbered lines, not a paragraph next to the button.
3. **Few words.**
   - A title is at most 4 words.
   - A row's name is at most 3 words, a noun.
   - A row's second line is at most 8 words.
   - The text above the actions is at most 2 sentences.
   - Counts and details fold into one line («٨ أشياء تمام»), not one row each.
4. **Say it once.** A state appears in one place. «وصلنا تقريرك» goes once at the top, not on every row. Code a state by colour
   or by word plus colour, never a coloured bar and a coloured word and an icon together.
5. **It fits.** At 720p with the font at 1.3, nothing is clipped and the primary action is on screen without scrolling.
   Mockups show both.

## Words the person never reads

- أندرويد، نسخة أندرويد، الإصدار ٤٫٤٫١، المتصفح الداخلي، الذاكرة، الحرارة، خادم، مزوّد خدمة، آليًا، يُحلَّل
- DNS، IP، WebView، launcher، cache، APK، root
- «دور الشاشة الرئيسية»، «مراقبة زر الرئيسية»، «أذونات»، «القائمة الآمنة»، «نسخة احتياطية» (say what it does for them instead)

There are two exceptions:
- the TV's own menu names, in «», when the person must press them («تصحيح USB»);
- a number they must read out to us («رقم التقرير»), on the screen that gives it to them.

**The same word for the same thing, everywhere:**
- تلفزيونك (our app), الرئيسية, الإعدادات, شاشة التوقف, تلاوة وأذان
- رقم التقرير (never «رمز التقرير»)
- مفعّل / متوقف
- تم, كمّل, جرّب مرة ثانية

## The components: one set

| Component | How it looks | Never |
|---|---|---|
| Screen | The look's background. One title at the top on the start side, at most one dim line under it, the 5 % safe area | Two titles, a banner |
| Row | A rounded panel. Its name, then at most one line under it, then its value at the end. An on/off row uses OnOff: three green marks and «مفعّل», or «متوقف» | A row that is only a sentence |
| Button | A rounded rectangle, one size on a screen. The primary is filled, the others are quiet | Pills (capsules), outlines, «·» between buttons |
| Tile / card | A picture, then the name, then at most one line | A second line of details |
| Checkbox | A square that is green when ticked, only in a list where several can be chosen | Checkboxes for a single yes/no (that is an on/off row) |
| State colours | Green: fine. Gold: waiting or worth a look. Red: a problem. The same values as UrTV (`Ui`, `OnOff`) | A second amber or green |
| Type | Thmanyah, at the sizes the screen's kind already uses. In the compact box: 22 / 17 / 14 sp | A new size, a third typeface |

**Focus:**
- Rows and buttons: the look's focus fill (`gFocus`) with dark ink, as in the settings.
- Tiles and cards: a scale of 1.06–1.1 with a soft shadow, as on the home.
- Never a ring, an outline, or a colour change alone.

The owner's «scale + shadow only» is what the home does. Rows keep the fill, which reads from the couch. Making rows scale too is
a 4.5 decision for the owner.

## The patterns

- **A choice:** pictures or rows. Focus previews where it can, OK keeps, and Back puts back what was there.
- **More than four choices:** show the main three or four and «المزيد». The rest go on the next page, not in a longer grid.
- **A question:** one sentence and two buttons. When it removes something, «إلغاء» has the focus.
- **Waiting:** one line and a progress bar. Step names show only when the person has a part in a step.
- **A problem:**
  1. what happened, in one sentence;
  2. the one thing to do;
  3. a button that opens it («افتح إعدادات الواي فاي»).
  The why is one line, only when the step could surprise them.
- **Done:** «تم» and one line. Details such as the report's QR are quiet, under it.

## Where we are now (reviewed November 2026)

- **The doctor app** (urtv-doctor, mockups on work/44-doctor). 15 required changes were sent to its session, among them:
  - one button on the start screen;
  - «تحليل» merged into the results;
  - no versions or IDs on the results;
  - a one-question consent;
  - Thmanyah instead of system sans-serif.
- **The on-TV setup** (the owner's one box, docs/mockups/compact-setup-v2). The box and its one text style stay.
  - **The start** reads three things before its button: the welcome, the USB-debugging path, and the rescue QR with Downloader
    steps. Make the welcome one line and «جهّز كل شي». The USB path becomes a step of its own, shown only when debugging is off.
    The rescue QR moves to its own page before the work («صوّر هذا عشان لو احتجته»).
  - **The plan** lists five technical lines («نسخة احتياطية»، «أذونات»، «القائمة الآمنة»). Use three plain lines of what changes
    for them. The SmartTube offer moves after the setup.
  - **Done** shows «دور الشاشة الرئيسية» and «مراقبة زر الرئيسية». Use «تم. اضغط زر الرئيسية», with the QR quiet under it.
  - **Focus:** the drawings show a blue ring and gold filled buttons. Use the rows' fill and the one primary button style.
- **«ابدأ بـ»** (docs/mockups/start-presets): eight starts became four, «نظيف», «كامل», «كنبة» and «المزيد»; Back keeps the TV as
  it is.
