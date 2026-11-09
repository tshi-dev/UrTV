// The packaged page of the LG and Samsung apps in a real browser, offline, with the keys a webOS or Tizen remote sends.
// BETA: a desktop Chromium standing in for the TVs' browsers; not a real LG or Samsung TV.
//
//   node tests/tvports_page.test.mjs      (needs playwright, as tests/page.test.mjs; CHROMIUM=/opt/pw-browsers/chromium if it must)
//
// It builds both packages (tests/tvports_test.py --extract), opens each one's index.html from disk, and presses keys by dispatching
// keydown events with the TV's key codes (LG Back 461, Samsung Back 10009, arrows 37-40, OK 13).
import { createRequire } from 'node:module';
import { execFileSync } from 'node:child_process';
import { mkdtempSync, rmSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { tmpdir } from 'node:os';
import path from 'node:path';

// require, not import: a Playwright that is installed for the machine (NODE_PATH) is found, as well as one in node_modules
const { chromium } = createRequire(import.meta.url)('playwright');
const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const OUT = mkdtempSync(path.join(tmpdir(), 'urtv-tvports-'));
execFileSync('python3', [path.join(ROOT, 'tests/tvports_test.py'), '--extract', OUT], { stdio: 'ignore' });

let failed = 0;
const check = (name, ok, detail = '') => {
  console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail));
  if (!ok) failed++;
};

const browser = await chromium.launch({
  ...(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {}),
  args: ['--autoplay-policy=no-user-gesture-required'],
});

// 8 seconds of silence, 8 kHz mono 8-bit: long enough that the adhan is still "playing" when the test looks
function wav() {
  const n = 8000 * 8, b = Buffer.alloc(44 + n, 128);
  b.write('RIFF', 0); b.writeUInt32LE(36 + n, 4); b.write('WAVEfmt ', 8); b.writeUInt32LE(16, 16); b.writeUInt16LE(1, 20); b.writeUInt16LE(1, 22);
  b.writeUInt32LE(8000, 24); b.writeUInt32LE(8000, 28); b.writeUInt16LE(1, 32); b.writeUInt16LE(8, 34); b.write('data', 36); b.writeUInt32LE(n, 40);
  return b;
}

// A TV's page: the package's index.html from disk, the network cut (the adhan's recording is answered here), the platform's own objects
// (tizen.* or PalmSystem) made to say what was called.
async function open(platform, { hash = '', init = '', now = null, settings = null } = {}) {
  const ctx = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const requests = [];
  await ctx.route(/^https?:/, r => {
    const u = r.request().url();
    requests.push(u);
    if (/archive\.org\//.test(u)) return r.fulfill({ body: wav(), contentType: 'audio/wav', headers: { 'Access-Control-Allow-Origin': '*' } });
    return r.abort();
  });
  await ctx.addInitScript(() => {
    window.__calls = [];
  });
  if (platform === 'tizen') await ctx.addInitScript(() => {
    window.tizen = {
      tvinputdevice: { registerKey: n => window.__calls.push('registerKey:' + n) },
      application: { getCurrentApplication: () => ({ exit: () => window.__calls.push('exit') }) },
    };
  });
  if (platform === 'webos') await ctx.addInitScript(() => {
    window.PalmSystem = { platformBack: () => window.__calls.push('platformBack') };
  });
  if (settings) await ctx.addInitScript(s => { if (!localStorage.getItem('urtv.tv.settings.v1')) localStorage.setItem('urtv.tv.settings.v1', s); }, JSON.stringify(settings));
  if (init) await ctx.addInitScript(init);
  const p = await ctx.newPage();
  const errors = [];
  p.on('pageerror', e => errors.push(e.message));
  if (now) await p.clock.setFixedTime(now);
  await p.goto('file://' + path.join(OUT, platform, 'index.html') + hash);
  await p.waitForFunction(() => document.documentElement.classList.contains('ready'), null, { timeout: 8000 }).catch(() => errors.push('never revealed'));
  await p.waitForTimeout(300);
  return { p, ctx, errors, requests };
}
const key = (p, code) => p.evaluate(c => document.dispatchEvent(new KeyboardEvent('keydown', { keyCode: c, bubbles: true, cancelable: true })), code);
const calls = p => p.evaluate(() => window.__calls.slice());
const text = (p, sel) => p.evaluate(s => { const e = document.querySelector(s); return e ? e.textContent : null; }, sel);

for (const platform of ['webos', 'tizen']) {
  const back = platform === 'webos' ? 461 : 10009;
  const L = s => `${platform}: ${s}`;

  // ---- the page itself: Arabic, RTL, the things the TV shows
  {
    const { p, ctx, errors, requests } = await open(platform, { hash: '#gap=0' });
    check(L('the page starts with no script error'), errors.length === 0, errors.join(' | '));
    check(L('the platform is recognised'), (await p.evaluate(() => UrtvTV.platform)) === platform);
    check(L('right to left, Arabic'), await p.evaluate(() => document.documentElement.dir === 'rtl' && document.documentElement.lang === 'ar'));
    const clock = await text(p, '#ck-h'), mins = await text(p, '#ck-m');
    check(L('the clock shows, in Arabic digits'), /[٠-٩]/.test(clock || '') && /[٠-٩]/.test(mins || ''), clock + ':' + mins);
    const hijri = await text(p, '#hijri'), greg = await text(p, '#greg');
    check(L('the Hijri and the Gregorian date show'), /هـ/.test(hijri || '') && (greg || '').length > 5, hijri + ' | ' + greg);
    const prayer = await text(p, '#prayer-name'), count = await text(p, '#prayer-count');
    check(L('the next prayer and the time left show (computed offline)'), /الفجر|الظهر|العصر|المغرب|العشاء/.test(prayer || '') && /بعد/.test(count || ''), prayer + ' | ' + count);
    check(L('the city is Riyadh by default and says so under the clock'), /الرياض/.test((await text(p, '#clock-sub')) || ''));
    check(L('a card beside the clock'), await p.evaluate(() => document.querySelectorAll('.week').length >= 1));
    check(L('a photo behind, from the package (not GitHub)'), await p.evaluate(() => /bg\//.test(document.querySelector('.photo').getAttribute('style') || '') || /bg\//.test(getComputedStyle(document.querySelector('.photo')).backgroundImage)),
      await p.evaluate(() => getComputedStyle(document.querySelector('.photo')).backgroundImage));
    check(L('no font or photo asked of raw.githubusercontent.com/.../assets'), !requests.some(u => /\/assets\//.test(u)), requests.filter(u => /\/assets\//.test(u)).join(' '));
    if (platform === 'tizen') {
      const c = await calls(p);
      check(L('registerKey is called for the colour and media keys'), ['ColorF0Red', 'ColorF3Blue', 'MediaPlayPause', 'MediaStop'].every(n => c.includes('registerKey:' + n)), c.join(','));
    }
    // Back with nothing open leaves the app, the TV's own way
    await key(p, back);
    const c = await calls(p);
    check(L('Back leaves the app (' + (platform === 'webos' ? 'PalmSystem.platformBack' : 'application.exit') + ')'), c.includes(platform === 'webos' ? 'platformBack' : 'exit'), c.join(','));
    // arrows and nothing else do nothing: the page is not moved, no panel
    for (const k of [37, 38, 39, 40]) await key(p, k);
    check(L('the arrows alone open nothing'), !(await p.evaluate(() => UrtvTV.panelOpen())));
    await ctx.close();
  }

  // ---- OK opens the settings screen; it is driven by the arrows; Back closes and starts the page again with the new settings
  {
    const { p, ctx, errors } = await open(platform, { hash: '#gap=0' });
    await key(p, 13);
    check(L('OK opens the settings screen'), await p.evaluate(() => UrtvTV.panelOpen()));
    const box = await p.evaluate(() => { const r = document.getElementById('urtv-tv-settings').getBoundingClientRect(); return { l: r.left, t: r.top, r: r.right, b: r.bottom }; });
    check(L('the settings screen fits the 1920x1080 screen'), box.l >= 0 && box.t >= 0 && box.r <= 1920 && box.b <= 1080, JSON.stringify(box));
    check(L('the settings screen is Arabic and right to left'), await p.evaluate(() => { const e = document.getElementById('urtv-tv-settings'); return e.dir === 'rtl' && /الدولة/.test(e.textContent) && /المدينة/.test(e.textContent) && !/طريقة الحساب/.test(e.textContent); }));   // 4.3 A4: no calculation row
    const goRow = async label => { for (let i = 0; i < 20 && (await text(p, '.r.on .l')) !== label; i++) await key(p, 40); return (await text(p, '.r.on .l')) === label; };
    await key(p, 40);   // down: المدينة
    check(L('Down moves to the next row'), (await text(p, '.r.on .l')) === 'المدينة', await text(p, '.r.on .l'));
    const city0 = await text(p, '.r.on .v');
    await key(p, 37);   // left: on in the list
    const city1 = await text(p, '.r.on .v');
    check(L('Left changes the city (the next one of the region)'), city0 !== city1 && /[\u0600-\u06FF]/.test(city1), city0 + ' -> ' + city1);
    await key(p, 39);   // right: back
    check(L('Right goes back'), (await text(p, '.r.on .v')) === city0, await text(p, '.r.on .v'));
    await key(p, 38); await key(p, 38);   // up, up: wraps to the last row
    check(L('Up wraps to the last row'), (await text(p, '.r.on .l')) === 'حفظ وإغلاق', await text(p, '.r.on .l'));
    check(L('the clock row: 12-hour to 24-hour'), await goRow('ساعة ٢٤') && (await text(p, '.r.on .v')).includes('لا'), await text(p, '.r.on .v'));
    await key(p, 37);
    check(L('and it says نعم'), (await text(p, '.r.on .v')).includes('نعم'), await text(p, '.r.on .v'));
    const stored = await p.evaluate(() => JSON.parse(localStorage.getItem('urtv.tv.settings.v1')));
    check(L('the settings are kept in localStorage'), stored && stored.clock24 === true && stored.pm === 'auto', JSON.stringify(stored));
    const nav = p.waitForNavigation({ timeout: 8000 }).catch(() => null);
    await key(p, back);   // Back: save and close; the page starts again
    await nav;
    await p.waitForFunction(() => document.documentElement.classList.contains('ready'), null, { timeout: 8000 }).catch(() => errors.push('never revealed (2)'));
    const hash = await p.evaluate(() => location.hash);
    check(L('the page starts again with the new settings in its link'), /clock=24/.test(hash) && !/pm=/.test(hash) && /gap=0/.test(hash) && /city=/.test(hash), hash);
    check(L('the 24-hour clock shows (no ص / م)'), await p.evaluate(() => getComputedStyle(document.getElementById('ck-ap')).display === 'none'));
    check(L('no calculation named under the clock for a Saudi city (4.3 A4: the country\'s own only)'), !/الجعفري|·/.test((await text(p, '#clock-sub')) || ''), await text(p, '#clock-sub'));
    check(L('no script error after the restart'), errors.length === 0, errors.join(' | '));
    await ctx.close();
  }

  // ---- «تعديل وقت الصلاة»: a minute off a prayer (a user's Fajr is a minute earlier), and the page's times move with it
  {
    const { p, ctx, errors } = await open(platform, { hash: '#gap=0' });
    const fajr = () => p.evaluate(() => { const x = PRAYERS.find(q => q.key === 'Fajr'); return x.h * 60 + x.mi; });
    const isha = () => p.evaluate(() => { const x = PRAYERS.find(q => q.key === 'Isha'); return x.h * 60 + x.mi; });
    const fajr0 = await fajr(), isha0 = await isha();
    await key(p, 13);
    const goRow = async label => { for (let i = 0; i < 30 && (await text(p, '.r.on .l')) !== label; i++) await key(p, 40); return (await text(p, '.r.on .l')) === label; };
    check(L('a row for each prayer: تعديل الفجر … تعديل العشاء'), await p.evaluate(() =>
      ['الفجر', 'الظهر', 'العصر', 'المغرب', 'العشاء'].every(n => [...document.querySelectorAll('#urtv-tv-settings .l')].some(e => e.textContent === 'تعديل ' + n))));
    check(L('تعديل الفجر is at 0 by default'), await goRow('تعديل الفجر') && (await text(p, '.r.on .v')).includes('٠ د'), await text(p, '.r.on .v'));
    await key(p, 39);   // right: a minute less
    check(L('Right takes a minute off: −١ د'), (await text(p, '.r.on .v')).includes('−١ د'), await text(p, '.r.on .v'));
    for (let i = 0; i < 25; i++) await key(p, 39);
    check(L('it stops at −٢٠'), (await text(p, '.r.on .v')).includes('−٢٠ د'), await text(p, '.r.on .v'));
    for (let i = 0; i < 19; i++) await key(p, 37);   // back to −1
    check(L('Left adds a minute back (−١)'), (await text(p, '.r.on .v')).includes('−١ د'), await text(p, '.r.on .v'));
    check(L('the others stay at 0 and the setting is kept'), JSON.stringify((await p.evaluate(() => UrtvTV.settings())).adj) === '[-1,0,0,0,0]',
      JSON.stringify(await p.evaluate(() => UrtvTV.settings())));
    check(L('تعديل العشاء: +2'), await goRow('تعديل العشاء') && (await key(p, 37), await key(p, 37), (await text(p, '.r.on .v')).includes('+٢ د')), await text(p, '.r.on .v'));
    await goRow('حفظ وإغلاق');
    const box = await p.evaluate(() => { const r = document.querySelector('#urtv-tv-settings .r.on').getBoundingClientRect(), b = document.getElementById('urtv-tv-settings').getBoundingClientRect();
      return { in: r.top >= b.top && r.bottom <= b.bottom, fits: b.top >= 0 && b.bottom <= 1080 }; });
    check(L('the longer list still fits the screen and keeps the chosen row in sight'), box.in && box.fits, JSON.stringify(box));
    const nav = p.waitForNavigation({ timeout: 8000 }).catch(() => null);
    await key(p, back);
    await nav;
    await p.waitForFunction(() => document.documentElement.classList.contains('ready'), null, { timeout: 8000 }).catch(() => errors.push('never revealed (adj)'));
    const hash = await p.evaluate(() => location.hash);
    check(L('the page starts again with adj=-1,0,0,0,2 in its link'), /(^|[#&])adj=-1%2C0%2C0%2C0%2C2(&|$)|(^|[#&])adj=-1,0,0,0,2(&|$)/.test(hash), hash);
    const fajr1 = await fajr(), isha1 = await isha();
    check(L('the page\'s Fajr is a minute earlier and its Isha two minutes later'), fajr1 === fajr0 - 1 && isha1 === isha0 + 2, `${fajr0}->${fajr1}, ${isha0}->${isha1}`);
    check(L('no script error'), errors.length === 0, errors.join(' | '));
    await ctx.close();
  }
  {   // all back to 0: nothing in the link (the page as it was)
    const { p, ctx } = await open(platform, { hash: '#gap=0', settings: { adj: [0, 0, 0, 0, 0] } });
    check(L('with every offset at 0 the link has no adj'), !/adj=/.test(await p.evaluate(() => location.hash)), await p.evaluate(() => location.hash));
    await ctx.close();
  }
  {   // a broken saved value is no adjustment
    const { p, ctx } = await open(platform, { hash: '#gap=0', settings: { adj: [99, 'x', 0] } });
    check(L('a saved value that is not five minutes is no adjustment'), JSON.stringify((await p.evaluate(() => UrtvTV.settings())).adj) === '[0,0,0,0,0]',
      JSON.stringify(await p.evaluate(() => UrtvTV.settings())));
    await ctx.close();
  }

  // ---- a city of another country: its coordinates and zone reach the page
  {
    const { p, ctx } = await open(platform, { hash: '#gap=0' });
    await key(p, 13);
    let guard = 0;
    while ((await text(p, '.r.on .v')) && !/مصر/.test(await text(p, '.r.on .v')) && guard++ < 40) await key(p, 37);   // the region row: on to مصر
    check(L('the region list reaches مصر'), /مصر/.test(await text(p, '.r.on .v')), await text(p, '.r.on .v'));
    const s = await p.evaluate(() => UrtvTV.settings());
    check(L('its first city, coordinates and zone were taken with it'), s.tz === 'Africa/Cairo' && Math.abs(s.lat - 30.04) < 1, JSON.stringify(s));
    const nav = p.waitForNavigation({ timeout: 8000 }).catch(() => null);
    await key(p, back);
    await nav;
    await p.waitForFunction(() => document.documentElement.classList.contains('ready'), null, { timeout: 8000 }).catch(() => {});
    check(L('the page uses it: the clock line names the city and the Egyptian calculation'), /القاهرة/.test((await text(p, '#clock-sub')) || '') && /الهيئة المصرية/.test((await text(p, '#clock-sub')) || ''), await text(p, '#clock-sub'));
    await ctx.close();
  }

  // ---- the adhan: at the prayer's time, web audio, the bar, any key stops it
  {
    // first run: find Maghrib's time for the day the page computes, then run again at that moment
    const first = await open(platform, { hash: '#gap=0' });
    const mag = await first.p.evaluate(() => { const x = PRAYERS.find(q => q.key === 'Maghrib'); const p = riyadhParts(); return { h: x.h, mi: x.mi, y: p.y, mo: p.mo, d: p.d }; });
    await first.ctx.close();
    const at = new Date(Date.UTC(mag.y, mag.mo - 1, mag.d, mag.h - 3, mag.mi, 5));   // Riyadh is UTC+3
    const { p, ctx, errors } = await open(platform, { hash: '#gap=0', now: at, settings: { adhan: true, voice: 1, fajrVoice: 10, volume: 50, adhanbar: true } });
    await p.waitForFunction(() => UrtvTV.adhanState().live, null, { timeout: 5000 }).catch(() => {});
    const st = await p.evaluate(() => UrtvTV.adhanState());
    check(L('the adhan starts at Maghrib\'s time, from the public-domain recording'), st.live && st.prayer === 4 && /archive\.org\/download\/azaan-of-world-mosques/.test(st.src), JSON.stringify(st));
    check(L('it plays at the chosen volume'), st.volume === 0.5, JSON.stringify(st))
    await p.waitForTimeout(500);
    const bar = await p.evaluate(() => { const b = document.getElementById('adbar'); return b && !b.hidden ? b.textContent : ''; });
    check(L('the adhan bar names it and says any key stops it'), /أذان المغرب/.test(bar) && /أي زر يوقف الأذان/.test(bar), bar);
    await key(p, 40);   // any key
    const after = await p.evaluate(() => UrtvTV.adhanState());
    check(L('any key stops the adhan, and the key is spent on that (no settings screen opens)'), !after.live && !(await p.evaluate(() => UrtvTV.panelOpen())), JSON.stringify(after));
    await p.waitForTimeout(400);
    const bar2 = await p.evaluate(() => { const b = document.getElementById('adbar'); return b && !b.hidden ? b.textContent : ''; });
    check(L('the bar goes with it'), !/أي زر يوقف/.test(bar2), bar2);
    // once a day: the same minute does not start it again
    await p.waitForTimeout(1500);
    check(L('it does not start again in the same minute'), !(await p.evaluate(() => UrtvTV.adhanState().live)));
    check(L('no script error'), errors.length === 0, errors.join(' | '));
    await ctx.close();
  }

  // ---- the adhan off (the default): nothing plays at the time
  {
    const first = await open(platform, { hash: '#gap=0' });
    const asr = await first.p.evaluate(() => { const x = PRAYERS.find(q => q.key === 'Asr'); const p = riyadhParts(); return { h: x.h, mi: x.mi, y: p.y, mo: p.mo, d: p.d }; });
    await first.ctx.close();
    const at = new Date(Date.UTC(asr.y, asr.mo - 1, asr.d, asr.h - 3, asr.mi, 5));
    const { p, ctx } = await open(platform, { hash: '#gap=0', now: at });
    await p.waitForTimeout(2500);
    check(L('adhan off by default: nothing plays'), !(await p.evaluate(() => UrtvTV.adhanState().live)));
    await ctx.close();
  }
}

await browser.close();
rmSync(OUT, { recursive: true, force: true });
console.log(failed ? `\n${failed} failed` : '\nall checks passed');
process.exit(failed ? 1 : 0);
