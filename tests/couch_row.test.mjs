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

// 4.5: the page's clock is pinned (the countdown is not drawn while a prayer is between its adhan and its iqama, so a run at that minute
// of the real day failed): Wednesday 18 November 2026, 10:30 in Riyadh, after sunrise and over an hour before the Dhuhr adhan (no window)
const AT = Date.parse('2026-11-18T07:30:00Z');
async function page(file, w, h, q, at = AT) {
  const ctx = await browser.newContext({ viewport: { width: w, height: h } });
  await ctx.route(/^https?:/, r => r.abort());
  const p = await ctx.newPage();
  await p.clock.setFixedTime(at);
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
    const centred = cards.every(c => getComputedStyle(c.querySelector('.hd')).justifyContent === 'flex-start' && [...c.querySelectorAll('ul:not(.pgrid) > li')].every(l => getComputedStyle(l).justifyContent === 'center'));   // 4.4 the owner: the titles at the start (right) like everywhere, the lines centred
    return { centred, cls: document.documentElement.className, cards: cards.map(box), bgs: cards.map(c => getComputedStyle(c).backgroundColor + '|' + getComputedStyle(c).backgroundImage + '|' + getComputedStyle(c, '::before').backgroundColor),
      lis: lis.map(box), next: lis.filter(l => l.classList.contains('next')).map(l => ({ cd: (l.querySelector('.cd') || {}).textContent || '', bg: getComputedStyle(l).backgroundColor })),
      cds: document.querySelectorAll('.cd').length, prBg: pr ? getComputedStyle(pr).backgroundColor : '', asr: typeof DAY_TIMES !== 'undefined' && DAY_TIMES ? String(DAY_TIMES.Asr) : '' };
  });
  await ctx.close();
  return { ...r, errors };
}

for (const file of ['tv.html', 'tv-en.html']) {
  const row = await page(file, 912, 108, 'panel=prayers&home_panel=prayers,verse,tasks&row=1');
  check(`${file} #row=1: three cards side by side in one strip, the same top, inside the box, no error`, row.cls.includes('row') && row.cards.length === 3
    && new Set(row.cards.map(b => b[1])).size === 1 && row.cards.every(b => b[0] >= 0 && b[2] <= 912) && !row.errors.length, JSON.stringify([row.cls, row.cards, row.errors]));
  check(`${file} #row=1: the cards' lines centred, their titles at the start (the owner, sweep 3: right-aligned everywhere)`, row.centred, '');
  check(`${file} without #row: not centred (the cards as they were)`, !(await page(file, 922, 500, 'panel=prayers&home_panel=verse,tasks')).centred, '');
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
  // inside an iqama window (5 minutes after the Asr adhan, its iqama 20 minutes after it): Asr is the lit box and no box has a countdown
  const [ah, am] = cur.asr.split(':').map(Number);
  const inWin = await page(file, 461, 250, 'panel=prayers&home_panel=prayers', Date.parse('2026-11-18T00:00:00+03:00') + ((ah * 60 + am + 5) * 60000));
  check(`${file} between the Asr adhan and its iqama: Asr is lit and no box has a countdown`, /^\d{1,2}:\d{2}/.test(cur.asr) && inWin.next.length === 1 && inWin.cds === 0 && !inWin.next[0].cd,
    JSON.stringify([cur.asr, inWin.cds, inWin.next]));
}
// 4.4 beta 4 (the owner: «كنبة» as couch-tv, the big clock always centred): the strip is 208 px at 1080p and 139 at 720p (CardBox.row); the cards fill it
// less 6.25vh above and below (182 of 208), the same height, inside the box at 720p too; the focus is a 3% scale and a short shadow, no ring
for (const [w, h] of [[1676, 208], [1117, 139]]) {
  const s = await page('tv.html', w, h, 'panel=prayers&home_panel=prayers,verse&row=1');
  const top = Math.round(h * .0625), bot = h - top;
  check(`tv.html #row=1 at ${w}x${h}: the cards ${top} to ${bot} (6.25vh of room above and below for the focus), side by side, inside the box`,
    s.cards.length === 2 && s.cards.every(b => Math.abs(b[1] - top) <= 1 && Math.abs(b[3] - bot) <= 1 && b[0] >= 0 && b[2] <= w), JSON.stringify(s.cards));
}
{
  const css = (await import('node:fs')).readFileSync(path.join(ROOT, 'src/template.html'), 'utf8');
  const rule = css.split('\n').find(l => l.includes('html.row.widget .week.lf,')) || '';
  check('the strip\'s focus: a 3% scale and a short shadow, no ring', /transform:scale\(1\.03\)/.test(rule) && /box-shadow:0 4px 10px/.test(rule) && !/inset/.test(rule), rule);
}
// 4.4 beta 4 SideCards: #side=1 is one column of 340 x 434 (1080p) or 227 x 289 (720p); A's two and B's three cards one under another, inside it
for (const [w, h, cards] of [[340, 434, 'nextprayer,hourly'], [340, 434, 'nextprayer,hourly,nextmatch'], [227, 289, 'nextprayer,hourly,nextmatch']]) {
  const s = await page('tv.html', w, h, `panel=nextprayer&home_panel=${cards}&side=1`);
  const n = cards.split(',').length;
  check(`tv.html #side=1 at ${w}x${h}: ${n} cards one under another, inside the column`, s.cls.includes('side') && s.cards.length === n
    && s.cards.every((b, i) => b[0] >= 0 && b[2] <= w && b[1] >= 0 && b[3] <= h + 1 && (i === 0 || b[1] >= s.cards[i - 1][3])), JSON.stringify(s.cards));
}
await browser.close();
if (failed) { console.log(`\n${failed} failed`); process.exit(1); }
console.log('\nall passed');
