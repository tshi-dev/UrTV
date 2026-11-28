// The family's TV banners (320x180, the app's name in Thmanyah; docs/mockups/icons, the owner's OK). Thmanyah is not in this repository
// (its licence: only inside a built product), so this runs where the private fonts are put in place, as in CI (assets/fonts/*.woff2), and its
// PNGs are committed. Without the fonts it stops and writes nothing (no banner in a stand-in font). The home's banner waits for the owner's pick.
//   node .github/tools/icons/banners.js          writes the banners (drawable-xhdpi 320x180, -xxhdpi 480x270, -xxxhdpi 640x360) and points
//                                                each manifest's android:banner at @drawable/banner
const fs = require('fs'), path = require('path');
const { BG, GOLD, glyphSvg } = require('./make.js');
const ROOT = path.resolve(__dirname, '../../..');
const FONT = path.join(ROOT, 'assets/fonts/ThmanyahDisplay-Bold.woff2');

const SETS = [
  { app: 'rescue', name: 'إنقاذ تلفزيونك', res: 'android/rescue/src/main/res', manifest: 'android/rescue/src/main/AndroidManifest.xml' },   // first: today it stretches its square icon
  { app: 'saver', name: 'شاشة التوقف', res: 'android/app/src/main/res', manifest: 'android/app/src/main/AndroidManifest.xml' },
  { app: 'extras', name: 'إضافات تلفزيونك', res: 'android/extras/src/main/res', manifest: 'android/extras/src/main/AndroidManifest.xml' },
  { app: 'doctor', name: 'فحص تلفزيونك', res: 'docs/mockups/icons/doctor/res' },   // other repositories: their sessions copy res/
  { app: 'player', name: 'مشغّل تلفزيونك', res: 'docs/mockups/icons/player/res' },
];
const SIZES = [['xhdpi', 320], ['xxhdpi', 480], ['xxxhdpi', 640]];

// the layout of today's home banner (the tile on the right, the name to its left, «UrTV» under it), on the family's green
function page(name, app, px) {
  const font = fs.readFileSync(FONT).toString('base64');
  return `<!doctype html><html><head><meta charset="utf-8"><style>
@font-face{font-family:T;src:url(data:font/woff2;base64,${font}) format('woff2')}
html,body{margin:0;background:transparent}svg{display:block}
</style></head><body><svg id="b" xmlns="http://www.w3.org/2000/svg" width="${px}" height="${px * 180 / 320}" viewBox="0 0 320 180"><defs>
<linearGradient id="bg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="${BG[0]}"/><stop offset="1" stop-color="${BG[1]}"/></linearGradient>
<linearGradient id="au" gradientUnits="userSpaceOnUse" x1="29" y1="31" x2="79" y2="78"><stop offset="0" stop-color="${GOLD[0]}"/><stop offset="1" stop-color="${GOLD[1]}"/></linearGradient></defs>
<rect width="320" height="180" fill="url(#bg)"/>
<rect x="220" y="52" width="76" height="76" rx="17" fill="#0B2A1F"/>
<svg x="220" y="52" width="76" height="76" viewBox="18 18 72 72">${glyphSvg(app)}</svg>
<text id="n" x="206" y="100" text-anchor="end" font-family="T" font-size="48" fill="#F7F3EA">${name}</text>
<text x="206" y="128" text-anchor="end" font-family="T" font-size="15" letter-spacing="2" fill="#E3C07A">UrTV</text></svg>
<script>
document.fonts.load('48px T').then(() => {   // the font first; the name fits between the edge and the tile: smaller for a long name, never under 22
  const n = document.getElementById('n'); let s = 48;
  while (n.getComputedTextLength() > 184 && s > 22) { s -= 1; n.style.fontSize = s + 'px'; }
  document.title = 'ready ' + s;
});
</script></body></html>`;
}

async function main() {
  if (!fs.existsSync(FONT)) {
    console.log('!! Thmanyah is not in assets/fonts (it comes from the private fonts repository, as in CI): nothing written');
    process.exit(2);
  }
  const { chromium } = require('playwright');
  const b = await chromium.launch();
  for (const s of SETS) {
    for (const [dir, px] of SIZES) {
      const p = await b.newPage({ viewport: { width: px, height: px * 180 / 320 }, deviceScaleFactor: 1 });
      await p.setContent(page(s.name, s.app, px));
      await p.waitForFunction(() => document.title.startsWith('ready'));
      if (!(await p.evaluate(() => document.fonts.check('48px T')))) throw new Error('Thmanyah did not load');
      const out = path.join(ROOT, s.res, `drawable-${dir}`, 'banner.png');
      fs.mkdirSync(path.dirname(out), { recursive: true });
      await (await p.$('#b')).screenshot({ path: out });
      await p.close();
    }
    if (s.manifest) {   // the application's banner is @drawable/banner (the rescue's was its square icon; the extras had none)
      const m = path.join(ROOT, s.manifest); let x = fs.readFileSync(m, 'utf8');
      if (/android:banner="[^"]*"/.test(x)) x = x.replace(/(<application[\s\S]*?)android:banner="[^"]*"/, '$1android:banner="@drawable/banner"');
      else x = x.replace(/(<application\b[\s\S]*?android:icon="[^"]*")/, '$1\n        android:banner="@drawable/banner"');
      fs.writeFileSync(m, x);
    }
    console.log('banner', s.app, s.name);
  }
  await b.close();
}
main().catch(e => { console.error(e); process.exit(1); });
