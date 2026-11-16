// 4.4 the page's part of «التخطيط: كنبة» (#row=1: the launcher's cards in one low strip above the apps tray) and of «شكل البطاقة: مربعات»
// (#cardstyle=boxes, html.pbx: today's prayers as separate boxes in one row, the next one light with its countdown). Offline.
//   node tests/couch_row.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});

async function page(file, w, h, q) {
  const ctx = await browser.newContext({ viewport: { width: w, height: h } });
  await ctx.route(/^https?:/, r => r.abort());
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.goto('file://' + path.join(ROOT, 'dist', file) + '#lat=24.7&lon=46.7&city=x&widget=1&' + q);
  await p.waitForSelector('.week li', { timeout: 8000 });
  await p.waitForTimeout(600);
  const r = await p.evaluate(() => {
    const box = e => { const b = e.getBoundingClientRect(); return [Math.round(b.left), Math.round(b.top), Math.round(b.right), Math.round(b.bottom)]; };
    const cards = [...document.querySelectorAll('.week')].filter(e => e.getClientRects().length);
    const pr = cards.find(c => c.classList.contains('boxes'));
    const lis = pr ? [...pr.querySelectorAll('ul.pgrid > li')] : [];
    return { cls: document.documentElement.className, cards: cards.map(box), bgs: cards.map(c => getComputedStyle(c).backgroundColor + '|' + getComputedStyle(c).backgroundImage + '|' + getComputedStyle(c, '::before').backgroundColor),
      lis: lis.map(box), next: lis.filter(l => l.classList.contains('next')).map(l => ({ cd: (l.querySelector('.cd') || {}).textContent || '', bg: getComputedStyle(l).backgroundColor })),
      cds: document.querySelectorAll('.cd').length, prBg: pr ? getComputedStyle(pr).backgroundColor : '' };
  });
  await ctx.close();
  return { ...r, errors };
}

for (const file of ['tv.html', 'tv-en.html']) {
  const row = await page(file, 912, 108, 'panel=prayers&home_panel=prayers,verse,tasks&row=1');
  check(`${file} #row=1: three cards side by side in one strip, the same top, inside the box, no error`, row.cls.includes('row') && row.cards.length === 3
    && new Set(row.cards.map(b => b[1])).size === 1 && row.cards.every(b => b[0] >= 0 && b[2] <= 912) && !row.errors.length, JSON.stringify([row.cls, row.cards, row.errors]));
  check(`${file} #row=1: today's six prayers in one row inside their card`, row.lis.length === 6 && new Set(row.lis.map(b => b[1])).size === 1, JSON.stringify(row.lis));
  const plain = await page(file, 922, 500, 'panel=prayers&home_panel=prayers,verse,tasks');
  check(`${file} without #row: the cards as they were (two side by side, the third under them)`, !/\brow\b/.test(plain.cls) && plain.cards.length === 3
    && plain.cards[2][1] > plain.cards[0][1], JSON.stringify(plain.cards));

  const bx = await page(file, 461, 250, 'panel=prayers&home_panel=prayers,verse&cardstyle=boxes&hpanel=rgba(28,24,20,0.45)&hline=rgba(255,255,255,0.2)');
  check(`${file} «مربعات»: html.pbx, not a cs- style (the other cards keep their own card)`, bx.cls.includes('pbx') && !/cs-/.test(bx.cls), bx.cls);
  check(`${file} «مربعات»: six boxes in one row, no card round them`, bx.lis.length === 6 && new Set(bx.lis.map(b => b[1])).size <= 2 && /rgba\(0, 0, 0, 0\)|transparent/.test(bx.prBg), JSON.stringify([bx.lis, bx.prBg]));
  check(`${file} «مربعات»: the next one is light with its countdown under it`, bx.next.length === 1 && (file === "tv.html" ? /^بعد / : /^In /).test(bx.next[0].cd.trim()) && bx.next[0].bg === 'rgb(246, 234, 210)', JSON.stringify(bx.next));
  check(`${file} «مربعات»: the verse card beside it keeps its glass`, bx.bgs.length === 2 && bx.bgs.some(b => /rgba\(28, 24, 20, 0\.45\)/.test(b)), JSON.stringify(bx.bgs));
  const cur = await page(file, 461, 250, 'panel=prayers&home_panel=prayers');
  // 4.4 card review: every style has the countdown in the next box (solid gold); only «مربعات» is pbx
  check(`${file} any other style: the countdown in the next box only, it solid gold, no pbx`, cur.cds === 1 && cur.next.length === 1 && cur.next[0].cd.trim() && cur.next[0].bg === 'rgb(235, 198, 125)' && !cur.cls.includes('pbx'), JSON.stringify([cur.cds, cur.next, cur.cls]));
}
await browser.close();
if (failed) { console.log(`\n${failed} failed`); process.exit(1); }
console.log('\nall passed');
