// 4.4 beta 4: on weak TVs our screensaver used more processor than the TV's own. A guard against the page growing heavier in «الوضع الخفيف»
// (#lowram=1, what CpuClass and the memory tiers turn on): no CSS animation runs at any moment of its first 12 s (each one keeps the TV's
// compositor busy), and the page's own main-thread work is under 2 % of the time once it has started. The full look is measured beside it.
//   node tests/lite_cpu.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const browser = await chromium.launch({ args: ['--allow-file-access-from-files'], ...(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {}) });
async function measure(hash) {
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  await ctx.route(/^https?:/, r => {
    const u = r.request().url(), i = u.indexOf('/master/'), rel = i > 0 ? u.slice(i + 8) : '';
    if (/^(assets\/(backgrounds|fonts)|photos)\//.test(rel)) return r.fulfill({ path: path.join(ROOT, rel), headers: { 'Access-Control-Allow-Origin': '*' } });
    return r.abort();
  });
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  const cdp = await ctx.newCDPSession(p);
  await cdp.send('Performance.enable');
  await p.goto('file://' + path.join(ROOT, 'dist/tv.html') + '#tz=Asia/Riyadh&gap=0' + (hash ? '&' + hash : ''));
  let anims = 0;
  for (let k = 0; k < 12; k++) {   // every second of the first 12: the animations running then
    await p.waitForTimeout(1000);
    anims = Math.max(anims, await p.evaluate(() => document.getAnimations().filter(a => a.playState === 'running').length));
  }
  const metric = async () => Object.fromEntries((await cdp.send('Performance.getMetrics')).metrics.map(x => [x.name, x.value]));
  const m0 = await metric();
  await p.waitForTimeout(15000);
  const m1 = await metric();
  await ctx.close();
  return { anims, share: (m1.TaskDuration - m0.TaskDuration) / 15, errors };
}
const lite = await measure('lowram=1');
const full = await measure('');
console.log(`  (the page's main thread: ${(lite.share * 100).toFixed(2)} % in «الوضع الخفيف», ${(full.share * 100).toFixed(2)} % in the full look)`);
check('«الوضع الخفيف»: no CSS animation runs at any moment of the first 12 s', lite.anims === 0, String(lite.anims));
check('«الوضع الخفيف»: the page\'s main-thread work is under 2 % of the time once it has started', lite.share < 0.02, (lite.share * 100).toFixed(2) + ' %');
check('the full look still moves (its photo drifts), so the check above can tell them apart', full.anims > 0, String(full.anims));
check('no page error', lite.errors.length === 0 && full.errors.length === 0, lite.errors.concat(full.errors).join(' | '));
await browser.close();
process.exit(failed ? 1 : 0);
