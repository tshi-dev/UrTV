// 4.3 «توحيد المظهر» on the page: the home's cards take the launcher's material. #hpanel= and #hline= are the fill and the line (html.hcard); with the switch on #hink=, #hsub= and
// #hacc= are the look's ink too (html.hsync), and on a light look (html.hlt) the words written for dark glass become ones that read on white. Without the three the words are
// the page's own. Offline.   node tests/sync_cards.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => {
  console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail));
  if (!ok) failed++;
};
const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
const lum = c => { const f = v => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }; return 0.2126 * f(c[0]) + 0.7152 * f(c[1]) + 0.0722 * f(c[2]); };
const ratio = (a, b) => { const x = lum(a), y = lum(b); return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05); };
const near = (c, w) => c.slice(0, 3).join() === w.slice(0, 3).join() && Math.abs((c[3] === undefined ? 1 : c[3]) - w[3]) < 0.006;   // the browser rounds an alpha
const over = (top, a, under) => top.map((v, i) => v * a + under[i] * (1 - a));

async function card(file, panel, q) {
  const ctx = await browser.newContext({ viewport: { width: 461, height: 250 } });
  await ctx.route(/^https?:/, r => r.abort());
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.goto('file://' + path.join(ROOT, 'dist', file) + `#widget=1&panel=${panel}&lat=24.7&lon=46.7&city=x${q ? '&' + q : ''}`);
  await p.waitForSelector('.week li', { timeout: 8000 });
  const r = await p.evaluate(() => {
    const n = s => s.match(/[\d.]+/g).map(Number), w = document.querySelector('.week'), cs = getComputedStyle(w);
    const texts = [...w.querySelectorAll('*')].filter(e => e.getClientRects().length && [...e.childNodes].some(x => x.nodeType === 3 && x.textContent.trim()) && +getComputedStyle(e).opacity > 0)
      .map(e => ({ t: e.textContent.trim().slice(0, 12), c: n(getComputedStyle(e).color), cls: e.className + '/' + e.parentNode.className }));
    return { cls: document.documentElement.className, bg: n(cs.backgroundColor), line: n(cs.borderTopColor), shadow: cs.boxShadow, radius: cs.borderTopLeftRadius, hd: n(getComputedStyle(w.querySelector('.hd')).color), texts };
  });
  await ctx.close();
  return { ...r, errors };
}

const LIGHT = 'hpanel=rgba(255,255,255,0.620)&hline=rgba(20,22,26,0.122)&hink=rgba(20,22,26,1)&hsub=rgba(58,62,70,1)&hacc=rgba(11,94,81,1)';
const GRAPHITE = 'hpanel=rgba(32,33,36,0.902)&hline=rgba(255,255,255,0.078)&hink=rgba(242,240,236,1)&hsub=rgba(185,182,176,1)&hacc=rgba(240,166,74,1)';
const OFF = 'hpanel=rgba(255,255,255,0.141)&hline=rgba(255,255,255,0.278)';
for (const file of ['tv.html', 'tv-en.html']) for (const panel of ['prayers', 'verse', 'hadith', 'week']) {
  const at = `${file} ${panel}`;
  const g = await card(file, panel, GRAPHITE);
  check(`${at}, on (graphite): the card is the material's fill and line, no shadow, its heading the look's accent`, g.cls.includes('hcard') && g.cls.includes('hsync') && !g.cls.includes('hlt')
    && near(g.bg, [32, 33, 36, 0.902]) && near(g.line, [255, 255, 255, 0.078]) && g.shadow === 'none' && g.hd.slice(0, 3).join() === '240,166,74' && !g.errors.length, JSON.stringify([g.cls, g.bg, g.line, g.shadow, g.hd, g.errors]));
  const l = await card(file, panel, LIGHT);
  const white = over([255, 255, 255], 0.62, [245, 246, 248]);   // the white card over the light veil
  const low = l.texts.filter(t => ratio(t.c, white) < 4.5);
  const faint = l.texts.filter(t => /past/.test(t.cls));   // a prayer that has passed is dimmer on purpose
  check(`${at}, on (light): the card is the white bar and every word on it is dark ink that reads (4.5:1; a passed prayer may be dimmer)`, l.cls.includes('hlt') && near(l.bg, [255, 255, 255, 0.62])
    && l.hd.slice(0, 3).join() === '11,94,81' && l.texts.length > 1 && low.every(t => faint.includes(t)), JSON.stringify(low.slice(0, 4)));
  const o = await card(file, panel, OFF);
  const plain = await card(file, panel, '');
  check(`${at}, off: the fill and the line only, the words are the page's own`, o.cls.includes('hcard') && !o.cls.includes('hsync') && near(o.bg, [255, 255, 255, 0.141])
    && JSON.stringify(o.texts) === JSON.stringify(plain.texts) && o.hd.join() === plain.hd.join(), JSON.stringify([o.cls, o.bg, o.hd, plain.hd]));
  check(`${at}, nothing from the launcher: the page as it was`, !/hcard|hsync|hlt/.test(plain.cls), plain.cls);
}
const bad = await card('tv.html', 'prayers', 'hpanel=rgba(32,33,36,0.902)&hline=rgba(255,255,255,0.078)&hink=red&hsub=rgba(185,182,176,1)&hacc=rgba(240,166,74,1)');
check('a colour that is not rgba(…): no ink at all (all three or none)', bad.cls.includes('hcard') && !bad.cls.includes('hsync'), bad.cls);
const worse = await card('tv.html', 'prayers', 'hpanel=url(x)&hline=rgba(255,255,255,0.078)&hink=rgba(20,22,26,1)&hsub=rgba(58,62,70,1)&hacc=rgba(11,94,81,1)');
check('no valid fill: nothing is taken', !/hcard|hsync/.test(worse.cls), worse.cls);

await browser.close();
console.log(failed ? `\n${failed} FAILED` : '\nall passed');
process.exit(failed ? 1 : 0);
