// Renders mock.html's screens to PNG at 1080p and 720p: node docs/mockups/league-v2/render.mjs (Playwright's Chromium)
import { createRequire } from 'module';
import { fileURLToPath } from 'url';
import path from 'path';
const require = createRequire(import.meta.url);
const { chromium } = require('playwright');
const dir = path.dirname(fileURLToPath(import.meta.url));
const shots = [['1-league-home', 'home'], ['2-matchweeks', 'rounds'], ['3-table-full', 'table'], ['4-club-quick-view', 'club'], ['6-club-page', 'clubpage'], ['7-table-full-large', 'table,large'], ['8-league-home-large', 'home,large'], ['9-form-boxes', 'table,boxes'], ['10-form-letters', 'table,letters'], ['11-form-boxes-large', 'table,boxes,large'], ['12-form-letters-large', 'table,letters,large'], ['13-club-boxes', 'clubpage,boxes'], ['5-league-home-dark', 'home,dark']];
const sizes = [[1920, 1080, '1080p'], [1280, 720, '720p'], [960, 540, '540p']];   // 540p: the size check only, no picture
const browser = await chromium.launch();
for (const [w, h, tag] of sizes) {
  const page = await browser.newPage({ viewport: { width: w, height: h }, deviceScaleFactor: 1 });
  for (const [name, hash] of shots) {
    if (tag !== '1080p' && hash.includes('dark')) continue;
    if (tag !== '720p' && hash.includes('large')) continue;   // the large font: checked at 720p
    if (tag !== '720p' && /boxes|letters/.test(hash)) continue;   // the other form styles: checked at 720p
    await page.goto('file://' + path.join(dir, 'mock.html') + '#' + hash);
    await page.reload();
    await page.evaluate(() => document.fonts.ready);
    await page.waitForTimeout(300);
    // R5 (the UI session's floor): no text smaller than the rows' muted secondary text (.mpi i: 24 at 1080p lengths, 16 px at 720p)
    const small = await page.evaluate(() => {
      const floor = 24 * innerWidth / 1920 - 0.05, out = [];
      document.querySelectorAll('#root *').forEach(el => {
        const own = [...el.childNodes].some(n => n.nodeType === 3 && n.textContent.trim());
        const cs = getComputedStyle(el);
        if (own && cs.visibility !== 'hidden' && parseFloat(cs.fontSize) < floor) out.push(el.className + ' ' + el.textContent.trim().slice(0, 20) + ' ' + cs.fontSize);
      });
      return out;
    });
    // the owner's rule (19 Nov, no AI-look decoration): no gradient anywhere, and no rounded coloured element smaller than a row
    const deco = await page.evaluate(() => {
      const row = 40 * innerWidth / 1920, out = [];
      document.querySelectorAll('#root *').forEach(el => {
        const cs = getComputedStyle(el), r = el.getBoundingClientRect();
        if (/gradient/.test(cs.backgroundImage)) out.push('gradient: ' + el.className);
        const bg = cs.backgroundColor, filled = bg && !/rgba\(0, 0, 0, 0\)|transparent/.test(bg);
        if (el.classList.contains('fdot') || el.classList.contains('fbox')) return;   // the form's markers: the dots and, as a chosen style, the boxes (the owner, 19 Nov); nothing else
        if (filled && parseFloat(cs.borderTopLeftRadius) + parseFloat(cs.borderTopRightRadius) > 0 && r.height > 0 && r.height < row - 0.5)
          out.push('rounded fill smaller than a row: ' + el.className + ' ' + el.textContent.trim().slice(0, 12));
      });
      return out;
    });
    // R5: nothing outside the 5 % safe area (the page itself, under a veil, may run under the sheet)
    const out = await page.evaluate(() => {
      const mx = innerWidth * 0.05 - 0.5, my = innerHeight * 0.05 - 0.5, bad = [];
      document.querySelectorAll('#root .pg *').forEach(el => {
        const r = el.getBoundingClientRect();
        if (r.width && r.height && getComputedStyle(el).visibility !== 'hidden' && (r.top < my || r.bottom > innerHeight - my || r.left < mx || r.right > innerWidth - mx))
          bad.push(el.className + ' ' + el.textContent.trim().slice(0, 16) + ' ' + Math.round(r.top) + '-' + Math.round(r.bottom));
      });
      return bad.slice(0, 5);
    });
    if (out.length && !hash.includes('large')) { console.error(`${name} ${tag}: outside the safe area:`, out); process.exitCode = 1; }
    if (deco.length) { console.error(`${name} ${tag}:`, deco); process.exitCode = 1; }
    if (small.length) { console.error(`${name} ${tag}: text under the floor:`, small); process.exitCode = 1; }
    if (tag === '540p') continue;
    await page.screenshot({ path: path.join(dir, `${name}-${tag}.png`) });
  }
  await page.close();
}
await browser.close();
