// Renders mock.html's screens to PNG at 1080p and 720p: node docs/mockups/league-v2/render.mjs (Playwright's Chromium)
import { createRequire } from 'module';
import { fileURLToPath } from 'url';
import path from 'path';
const require = createRequire(import.meta.url);
const { chromium } = require('playwright');
const dir = path.dirname(fileURLToPath(import.meta.url));
const shots = [['1-league-home', 'home'], ['2-matchweeks', 'rounds'], ['3-table-full', 'table'], ['4-club-quick-view', 'club'], ['6-club-page', 'clubpage'], ['5-league-home-dark', 'home,dark']];
const sizes = [[1920, 1080, '1080p'], [1280, 720, '720p']];
const browser = await chromium.launch();
for (const [w, h, tag] of sizes) {
  const page = await browser.newPage({ viewport: { width: w, height: h }, deviceScaleFactor: 1 });
  for (const [name, hash] of shots) {
    if (tag === '720p' && hash.includes('dark')) continue;
    await page.goto('file://' + path.join(dir, 'mock.html') + '#' + hash);
    await page.reload();
    await page.evaluate(() => document.fonts.ready);
    await page.waitForTimeout(300);
    await page.screenshot({ path: path.join(dir, `${name}-${tag}.png`) });
  }
  await page.close();
}
await browser.close();
