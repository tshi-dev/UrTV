// 4.4 beta 2 the page's part of the full look «tvOS» (#row=1&tvos=1: the launcher's cards in one row above the tvOS bar, glass; the owner approved
// docs/mockups/tvos-preset/ and the full-look table): each slot as the screensaver's CardFit decides (#fit=: two cards wide, a row of four days), and
// rowOverflow (a row with more cards than slots: the last slot takes turns, «١/٢» in its corner). Offline, the clock pinned.
//   node tests/tvos_row.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
const AT = Date.parse('2026-11-18T07:30:00Z');
const SHOTS = process.env.TVOS_SHOTS;   // a folder: the pictures for a person (not in the repository)

async function page(file, w, h, q, shot) {
  const ctx = await browser.newContext({ viewport: { width: w, height: h } });
  await ctx.route(/^https?:/, r => r.abort());
  const p = await ctx.newPage();
  await p.clock.setFixedTime(AT);
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  await p.goto('file://' + path.join(ROOT, 'dist', file) + '#lat=24.7&lon=46.7&city=x&widget=1&' + q);
  await p.waitForSelector('.week li', { timeout: 8000 });
  await p.waitForTimeout(700);
  if (SHOTS && shot) await p.screenshot({ path: path.join(SHOTS, shot) });
  const r = await p.evaluate(() => {
    const box = e => { const b = e.getBoundingClientRect(); return [Math.round(b.left), Math.round(b.top), Math.round(b.right), Math.round(b.bottom)]; };
    const cards = [...document.querySelectorAll('.week')].filter(e => e.getClientRects().length);
    return { cls: document.documentElement.className, n: cards.length,
      cards: cards.map(c => ({ box: box(c), wide: c.classList.contains('fwide'), row4: c.classList.contains('frow4'), turn: c.classList.contains('turn'),
        off: c.classList.contains('turnoff'), mark: (c.querySelector('.turnmark') || {}).textContent || '', radius: parseFloat(getComputedStyle(c).borderTopLeftRadius),
        bg: getComputedStyle(c).backgroundImage, lis: [...c.querySelectorAll('ul > li')].filter(l => l.getClientRects().length).map(box) })),
      vw: innerWidth, vh: innerHeight };
  });
  await ctx.close();
  return { ...r, errors };
}

for (const file of ['tv.html', 'tv-en.html']) {
  // three cards with the verse two cards wide: the verse stays, the other two take turns in the second slot
  const a = await page(file, 1664, 254, 'panel=prayerrow&home_panel=prayerrow,verse,hourly&row=1&tvos=1&fit=,wide,&slots=3', `tvos-row-wide-${file}.png`);
  const wide = a.cards.find(c => c.wide), turns = a.cards.filter(c => c.turn);
  check(`${file} #tvos=1: the row is tvOS's glass (html.tvos), rectangles with a radius (no pill), a flat fill (the owner: no gradient), no error`, a.cls.includes('tvos') && a.cls.includes('row')
        && a.cards.every(c => c.radius > 0 && c.radius < 40 && c.bg === 'none') && !a.errors.length, JSON.stringify([a.cls, a.errors]));
  check(`${file} #fit=wide: the verse two slots wide, beside the one that takes turns`, wide && turns.length === 2 && wide.box[2] - wide.box[0] > 1.6 * (turns[0].box[2] - turns[0].box[0]), JSON.stringify(a.cards.map(c => c.box)));
  check(`${file} rowOverflow: the two others share the last slot (the same place), one shown, «١/٢» and «٢/٢» in their corners (the English page's own digits); the wide card never turns`,
        turns.length === 2 && JSON.stringify(turns[0].box) === JSON.stringify(turns[1].box) && turns.filter(c => !c.off).length === 1
        && turns.map(c => c.mark).sort().join() === (file === 'tv.html' ? ['١/٢', '٢/٢'] : ['1/2', '2/2']).join() && !wide.turn, JSON.stringify(turns));
  check(`${file} every card inside the row's box`, a.cards.every(c => c.box[0] >= -1 && c.box[2] <= a.vw + 1 && c.box[1] >= -1 && c.box[3] <= a.vh + 1), JSON.stringify(a.cards.map(c => c.box)));
  // the coming days as a row of four
  const b = await page(file, 1664, 254, 'panel=nextprayer&home_panel=nextprayer,forecast,hourly&row=1&tvos=1&fit=,row4,&slots=3', `tvos-row-row4-${file}.png`);
  check(`${file} no wide card, three slots: three across as today, nothing takes turns`, b.cards.length === 3 && b.cards.every(c => !c.turn && !c.mark), JSON.stringify(b.cards.map(c => c.box)));
  // 720p with the large font: two slots, the third takes turns
  const c = await page(file, 1109, 220, 'panel=prayerrow&home_panel=prayerrow,hourly,nextprayer&row=1&tvos=1&fit=,,&slots=2', `tvos-row-720-${file}.png`);
  const ct = c.cards.filter(x => x.turn);
  check(`${file} two slots (720p, the large font): the first stays, the other two take turns in the second`, ct.length === 2 && !c.cards[0].turn && ct.filter(x => !x.off).length === 1, JSON.stringify(c.cards));
  // couch's row and the classic box are not touched by tvOS's styles
  const d = await page(file, 1664, 254, 'panel=prayerrow&home_panel=prayerrow,verse,hourly&row=1', null);
  check(`${file} without #tvos (couch's row): no tvOS class, three across, nothing takes turns`, !d.cls.includes('tvos') && d.cards.every(x => !x.turn), JSON.stringify(d.cls));
}
await browser.close();
process.exit(failed ? 1 : 0);
