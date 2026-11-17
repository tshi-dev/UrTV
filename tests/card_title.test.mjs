// 4.4 the owner (his markup on the CI shots): a home card's title must not be cut by the card's rounded corner or crossed by the
// header's rule, and it starts at the card's start (right in Arabic) in every layout. For every card style, with and without the couch
// row (#row=1), and for the prayers, «الساعات القادمة» and «المهام» cards: the title's text box is inside the card, at least 8 CSS px
// (16 px on a 1080p TV) clear of the rounded corner, at the start, and the rule is a flex sibling after it, never over it. Offline.
//   node tests/card_title.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
const CLEAR = 8;   // CSS px; the home's WebView is at density 2 on a 1080p TV

async function titles(file, style, row) {
  const ctx = await browser.newContext({ viewport: row ? { width: 912, height: 108 } : { width: 461, height: 500 } });
  await ctx.route(/^https?:/, r => r.abort());
  const p = await ctx.newPage();
  await p.goto('file://' + path.join(ROOT, 'dist', file) + `#lat=24.7&lon=46.7&city=x&widget=1&panel=prayers&home_panel=prayers,hourly,tasks&cardstyle=${style}${row ? '&row=1' : ''}&hpanel=rgba(28,24,20,0.45)&hline=rgba(255,255,255,0.2)`);
  await p.waitForSelector('.week .hd', { timeout: 8000 });
  await p.waitForTimeout(500);
  const r = await p.evaluate(() => [...document.querySelectorAll('.week')].filter(e => e.getClientRects().length).map(c => {
    const hd = c.querySelector('.hd'); if (!hd) return null;
    const rg = document.createRange(); rg.selectNodeContents(hd);
    const rects = [...rg.getClientRects()].filter(x => x.width > 1 && x.height > 1);
    if (!rects.length) return null;
    const t = { l: Math.min(...rects.map(x => x.left)), r: Math.max(...rects.map(x => x.right)), tp: Math.min(...rects.map(x => x.top)), b: Math.max(...rects.map(x => x.bottom)) };
    const cb = c.getBoundingClientRect(), cs = getComputedStyle(c), rtl = getComputedStyle(hd).direction === 'rtl';
    const R = parseFloat(rtl ? cs.borderTopRightRadius : cs.borderTopLeftRadius) || 0;
    const ex = rtl ? t.r : t.l, cx = rtl ? cb.right - R : cb.left + R, cy = cb.top + R;
    const inCorner = (rtl ? ex > cx : ex < cx) && t.tp < cy;
    const clear = inCorner ? R - Math.hypot(ex - cx, t.tp - cy) : Math.min(rtl ? cb.right - ex : ex - cb.left, t.tp - cb.top);
    const after = getComputedStyle(hd, '::after');
    return { title: hd.textContent.trim().slice(0, 24), clear: +clear.toFixed(1), inside: t.l >= cb.left && t.r <= cb.right && t.tp >= cb.top && t.b <= cb.bottom,
      start: (rtl ? cb.right - t.r : t.l - cb.left) < cb.width / 4, rule: after.display === 'none' || (after.position === 'static' && after.order !== '-1') };
  }).filter(Boolean));
  await ctx.close();
  return r;
}

for (const file of ['tv.html', 'tv-en.html'])
  for (const style of ['current', 'dark1', 'glass1', 'dark3', 'glass3', 'classic'])
    for (const row of [false, true]) {
      const r = await titles(file, style, row);
      const where = `${file} ${style}${row ? ' couch row' : ''}`;
      check(`${where}: three cards with a title`, r.length === 3, JSON.stringify(r));
      check(`${where}: each title's text inside its card`, r.every(x => x.inside), JSON.stringify(r));
      check(`${where}: each title ${CLEAR}+ CSS px clear of the rounded corner (16 px on a 1080p TV)`, r.every(x => x.clear >= CLEAR), JSON.stringify(r.map(x => [x.title, x.clear])));
      check(`${where}: each title at the start of its card, not centred`, r.every(x => x.start), JSON.stringify(r));
      check(`${where}: the header's rule beside the title (a flex sibling after it), never over it`, r.every(x => x.rule), JSON.stringify(r));
    }
await browser.close();
if (failed) { console.log(`\n${failed} failed`); process.exit(1); }
console.log('\nall passed');
