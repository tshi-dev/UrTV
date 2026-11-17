# UrTV 4.3.3 poster for X (for the owner's approval)

`urtv-4.3.3.png` is 2160×2700 (4:5). It is in the 4.3.1 family: dark green with a gold glow, the UrTV icon at the top right, and the Downloader code at the bottom. The hero is one big gold «4.3.3» with a light sweep across it.

The source is `poster-433.html`. To render it, put the IBM Plex Sans Arabic woff2 files (OFL, from `@fontsource/ibm-plex-sans-arabic`) in `a/` and screenshot the page at 2160×2700. The page names Thmanyah first, so a machine that has the app's private fonts renders it in Thmanyah; this PNG is in IBM Plex Sans Arabic because the fonts are not in this repository. The Downloader icon is cropped from the approved 4.3.1 poster.

## The number (the owner: «the 4.3.3 in the middle needs fixing»)

The first sheen ran across the number and washed the middle «.3» almost white. «4.3.3» now reads as one even gold, with even spacing (0.02em) and the soft glow behind it kept.

- `urtv-4.3.3-a.png` (also `urtv-4.3.3.png`, source `poster-433.html`): an even gold with a faint sheen from the top, the same on every digit, at most light gold #EBCD8C.
- `urtv-4.3.3-b.png` (source `poster-433-b.html`): a flat gold #E2BC6E with a soft outer glow only.

## Headline (two options)

1. «نسخة تجريبية؟ شغّلها من تلفزيونك» (on the poster)
2. «جرّب الجديد قبل الكل، من تلفزيونك»

## Every line checked against CHANGELOG 4.3.3

- «النسخ التجريبية»: a new row in «تحديث تلفزيوني»; notes are sent from the TV. Turning it off never installs an older build.
- «إنقاذ تلفزيونك»: a small separate app whose one button, «رجّع التلفزيون كما كان», brings the TV back.
- Fixes:
  - stock names on the home's markets page (4164 النهدي, 4002 المواساة, GitHub #60);
  - the photo no longer goes black (`--pdim`);
  - the screensaver's bottom strip stays still with the matches (work/433-strip, merged into rel-433-3 and on master; the CHANGELOG line is missing, and the supervisor is adding it);
  - «تحقق من التحديث» says «تلفزيونك محدّث» (`UpdateActivity`, «تلفزيونك محدّث: %s»; the tester's 4.3.3 run, picture 20-b2).
- Left out:
  - «after a crash it offers to send a report»: in 4.3.3, but only for people who turned on «النسخ التجريبية».
  - The «بدء شاشة التوقف بعد» fix: true, but too small for this poster.

## X post

Arabic (60 words):

وصل تلفزيونك 4.3.3. الجديد «النسخ التجريبية» في تحديث تلفزيوني: تجيك المزايا قبل الكل، وترسل ملاحظاتك من التلفزيون، وتوقفها متى ما تبي وما يرجع تلفزيونك لنسخة أقدم. وتطبيق «إنقاذ تلفزيونك» يرجّع التلفزيون كما كان بزر واحد، وإصلاحات: أسماء الأسهم في الأسواق، وصورة شاشة التوقف ما عادت تسودّ، وخطها السفلي ثابت. حدّث من الإعدادات ← تحديث تلفزيوني، أو بكود Downloader ‏9378345.
github.com/tshi-dev/UrTV

English:

UrTV 4.3.3: opt-in beta updates straight from your TV, a one-button Rescue app, and fixes. Update in Settings → Update your TV, or Downloader code 9378345.
