// The family's app icons (docs/mockups/icons, the owner's OK): one model -> the adaptive icon as vector drawables (background, foreground,
// monochrome; read on Android 8+) and the flattened PNGs for older TVs (mipmap xhdpi/xxhdpi/xxxhdpi). No font: the banners, which carry the
// app's name in Thmanyah, are made separately. Run: node .github/tools/icons/make.js   (Playwright's Chromium draws the PNGs)
const fs = require('fs'), path = require('path');
const ROOT = path.resolve(__dirname, '../../..');

const W = 5;   // one stroke weight for every glyph, in the adaptive icon's 108 units (the 66 safe circle is 21..87)
const TV = [
  { d: 'M35,33h38a6,6 0 0 1 6,6v24a6,6 0 0 1 -6,6h-38a6,6 0 0 1 -6,-6v-24a6,6 0 0 1 6,-6z', stroke: W },
  { d: 'M47,78h14M54,69v9', stroke: W, cap: 'round' },
];
const GLYPH = {
  saver: [...TV, { d: 'M57,41a10,10 0 1,0 9,15a8,8 0 1,1 -9,-15z', fill: true }],
  rescue: [...TV, { d: 'M47,48a8.5,8.5 0 1,1 1.5,9', stroke: W * 0.8, cap: 'round' },
                  { d: 'M42,44l5,4.5l-5.5,3.5', stroke: W * 0.8, cap: 'round', join: 'round' }],
  extras: [...TV, { d: 'M54,43v16M46,51h16', stroke: W * 0.9, cap: 'round' }],
};
// the apps of this repository that take the family now (the home waits for the owner's pick of its banner)
const APPS = [
  { app: 'saver', module: 'app', name: 'ic_launcher' },
  { app: 'rescue', module: 'rescue', name: 'ic_rescue' },
  { app: 'extras', module: 'extras', name: 'ic_extras' },
];
const BG = ['#14402F', '#061A12'], GOLD = ['#F6DC9E', '#C79A43'];

const AAPT = 'xmlns:android="http://schemas.android.com/apk/res/android" xmlns:aapt="http://schemas.android.com/aapt"';
const grad = (c, x1, y1, x2, y2) => `<aapt:attr name="android:%s"><gradient android:type="linear" android:startX="${x1}" android:startY="${y1}" android:endX="${x2}" android:endY="${y2}" android:startColor="${c[0]}" android:endColor="${c[1]}"/></aapt:attr>`;
const vd = (body, ns = AAPT) => `<?xml version="1.0" encoding="utf-8"?>\n<!-- made by .github/tools/icons/make.js: change it there -->\n<vector ${ns}\n    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n${body}</vector>\n`;
function vdPath(e, mono) {
  const paint = mono ? `"#FFFFFFFF"` : null;
  let s = `    <path android:pathData="${e.d}"`;
  if (e.fill) s += mono ? ` android:fillColor=${paint}/>\n` : `>\n        ${grad(GOLD, 29, 31, 79, 78).replace('%s', 'fillColor')}\n    </path>\n`;
  else {
    s += ` android:strokeWidth="${e.stroke}"${e.cap ? ` android:strokeLineCap="${e.cap}"` : ''}${e.join ? ` android:strokeLineJoin="${e.join}"` : ''}`;
    s += mono ? ` android:strokeColor=${paint}/>\n` : `>\n        ${grad(GOLD, 29, 31, 79, 78).replace('%s', 'strokeColor')}\n    </path>\n`;
  }
  return s;
}
const background = () => vd(`    <path android:pathData="M0,0h108v108h-108z">\n        ${grad(BG, 0, 0, 108, 108).replace('%s', 'fillColor')}\n    </path>\n`);
const foreground = (app) => vd(GLYPH[app].map(e => vdPath(e, false)).join(''));
const monochrome = (app) => vd(GLYPH[app].map(e => vdPath(e, true)).join(''), 'xmlns:android="http://schemas.android.com/apk/res/android"');
const adaptive = (n) => `<?xml version="1.0" encoding="utf-8"?>\n<!-- made by .github/tools/icons/make.js: change it there -->\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@drawable/${n}_bg"/>\n    <foreground android:drawable="@drawable/${n}_fg"/>\n    <monochrome android:drawable="@drawable/${n}_mono"/>\n</adaptive-icon>\n`;

// the same model as SVG, for the flattened PNGs (the full 108 square: a legacy icon shows it all, with rounded corners)
function svg(app) {
  const els = GLYPH[app].map(e => e.fill ? `<path d="${e.d}" fill="url(#au)"/>`
    : `<path d="${e.d}" fill="none" stroke="url(#au)" stroke-width="${e.stroke}" stroke-linecap="${e.cap || 'butt'}" stroke-linejoin="${e.join || 'miter'}"/>`).join('');
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="14 14 80 80"><defs>
<linearGradient id="bg" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="108" y2="108"><stop offset="0" stop-color="${BG[0]}"/><stop offset="1" stop-color="${BG[1]}"/></linearGradient>
<linearGradient id="au" gradientUnits="userSpaceOnUse" x1="29" y1="31" x2="79" y2="78"><stop offset="0" stop-color="${GOLD[0]}"/><stop offset="1" stop-color="${GOLD[1]}"/></linearGradient>
<clipPath id="r"><rect x="14" y="14" width="80" height="80" rx="18"/></clipPath></defs>
<g clip-path="url(#r)"><rect width="108" height="108" fill="url(#bg)"/>${els}</g></svg>`;
}

function write(rel, text) { const p = path.join(ROOT, rel); fs.mkdirSync(path.dirname(p), { recursive: true }); fs.writeFileSync(p, text); }

(async () => {
  for (const { app, module, name } of APPS) {
    const res = `android/${module}/src/main/res`;
    // drawable-v26: the gradients need API 24, and only the adaptive icon (API 26) reads them; older TVs take the PNGs
    write(`${res}/drawable-v26/${name}_bg.xml`, background());
    write(`${res}/drawable-v26/${name}_fg.xml`, foreground(app));
    write(`${res}/drawable-v26/${name}_mono.xml`, monochrome(app));
    write(`${res}/mipmap-anydpi-v26/${name}.xml`, adaptive(name));
  }
  const { chromium } = require('playwright');
  const b = await chromium.launch();
  for (const { app, module, name } of APPS) for (const [dir, px] of [['xhdpi', 96], ['xxhdpi', 144], ['xxxhdpi', 192]]) {
    const p = await b.newPage({ viewport: { width: px, height: px }, deviceScaleFactor: 1 });
    await p.setContent(`<html><body style="margin:0;background:transparent">${svg(app).replace('<svg ', `<svg width="${px}" height="${px}" `)}</body></html>`);
    await p.screenshot({ path: path.join(ROOT, `android/${module}/src/main/res/mipmap-${dir}/${name}.png`), omitBackground: true });
    await p.close();
  }
  await b.close();
})();
