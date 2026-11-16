// 4.4 the phone page «صوري»: a photo with its mask (docs/OWN-PHOTO-MASK.md). Served as the TV serves it (its nonce and its Content-Security-Policy),
// with the TV's two routes stubbed: the mask is checked on the phone (the photo's shape within 2 %, black and white, 1 MB), previewed, and sent after the
// photo as a black-and-white PNG at the photo's sent size; a bad one says why and offers the photo alone. Offline.   node tests/photo_mask_page.test.mjs
import { chromium } from 'playwright';
import { fileURLToPath } from 'node:url';
import fs from 'node:fs';
import path from 'node:path';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
let failed = 0;
const check = (name, ok, detail = '') => { console.log((ok ? 'PASS ' : 'FAIL ') + name + (ok ? '' : '  -> ' + detail)); if (!ok) failed++; };
const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
const SRC = fs.readFileSync(path.join(ROOT, 'android/launcher/src/main/assets/phone/photos.html'), 'utf8');
const PHOTO = fs.readFileSync(path.join(ROOT, 'photos', fs.readdirSync(path.join(ROOT, 'photos')).find(f => f.endsWith('.jpg'))));

// masks drawn in a page: {w, h, kind}: 'good' (sky black above, scenery white below), 'grey'
const maker = await browser.newPage();
async function png(w, h, kind) {
  const b64 = await maker.evaluate(([w, h, kind]) => {
    const c = document.createElement('canvas'); c.width = w; c.height = h; const x = c.getContext('2d');
    if (kind === 'grey') { x.fillStyle = '#888'; x.fillRect(0, 0, w, h); x.fillStyle = '#000'; x.fillRect(0, 0, w, h * 0.1); x.fillStyle = '#fff'; x.fillRect(0, h * 0.9, w, h * 0.1); }
    else { x.fillStyle = '#000'; x.fillRect(0, 0, w, h); x.fillStyle = '#fff'; x.beginPath(); x.moveTo(0, h); x.lineTo(0, h * 0.6); x.lineTo(w * 0.5, h * 0.35); x.lineTo(w, h * 0.6); x.lineTo(w, h); x.fill(); }
    return c.toDataURL('image/png').split(',')[1];
  }, [w, h, kind]);
  return Buffer.from(b64, 'base64');
}

async function run(lang, maskBuf, send) {
  const ctx = await browser.newContext({ viewport: { width: 400, height: 900 } });
  const posts = [], errors = [];
  await ctx.route('http://tv.test/**', async r => {
    const u = new URL(r.request().url());
    if (u.pathname === '/u') {
      const n = 'abc123';
      return r.fulfill({ status: 200, headers: { 'content-type': 'text/html; charset=utf-8', 'content-security-policy': "default-src 'none'; script-src 'nonce-" + n + "'; style-src 'unsafe-inline'; connect-src 'self'; img-src 'self' data:; font-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'" },
        body: SRC.replace('__NONCE__', n).replace('<html lang="ar"', '<html lang="' + lang + '"') });
    }
    if (u.pathname === '/api/photo' || u.pathname === '/api/photo-mask') {
      posts.push({ path: u.pathname, type: r.request().headers()['content-type'], session: r.request().headers()['x-urtv-session'], body: r.request().postDataBuffer() });
      return r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ ok: true }) });
    }
    return r.fulfill({ status: 404, body: '' });
  });
  const p = await ctx.newPage();
  p.on('console', m => { if (/Refused|Content Security Policy/.test(m.text())) errors.push(m.text()); });
  p.on('pageerror', e => errors.push(e.message));
  await p.addInitScript(() => sessionStorage.setItem('urtv', 's1'));
  await p.goto('http://tv.test/u');
  await p.setInputFiles('#mph', { name: 'p.jpg', mimeType: 'image/jpeg', buffer: PHOTO });
  await p.waitForSelector('#mstep:not(.hidden)');
  await p.setInputFiles('#mmk', { name: 'm.png', mimeType: 'image/png', buffer: maskBuf });
  await p.waitForSelector('#mchecks p');
  const r = await p.evaluate(() => ({ checks: [...document.querySelectorAll('#mchecks p')].map(e => e.className + ':' + e.textContent),
    send: !document.getElementById('msend').classList.contains('hidden'), bare: !document.getElementById('mbare').classList.contains('hidden'),
    prev: !document.getElementById('mprev').classList.contains('hidden'), words: document.getElementById('m9').textContent }));
  if (send) {
    await p.click(send === 'mask' ? '#msend' : '#mbare');
    await p.waitForFunction(() => /✓|TV/.test(document.getElementById('state').textContent), null, { timeout: 8000 }).catch(() => {});
    r.state = await p.evaluate(() => document.getElementById('state').textContent);
    const m = posts.find(x => x.path === '/api/photo-mask');
    if (m) r.mask = await maker.evaluate(async b64 => {   // the sent mask: its size and its tones
      const img = await createImageBitmap(await (await fetch('data:image/png;base64,' + b64)).blob());
      const c = document.createElement('canvas'); c.width = img.width; c.height = img.height; const x = c.getContext('2d'); x.drawImage(img, 0, 0);
      const d = x.getImageData(0, 0, img.width, img.height).data; let other = 0;
      for (let i = 0; i < d.length; i += 4) if (!((d[i] === 0 || d[i] === 255) && d[i] === d[i + 1] && d[i] === d[i + 2])) other++;
      return { w: img.width, h: img.height, other };
    }, m.body.toString('base64')).catch(e => ({ err: String(e) }));
  }
  r.posts = posts.map(x => x.path + '|' + x.type + '|' + x.session);
  r.errors = errors;
  await ctx.close();
  return r;
}

const good = await png(1280, 720, 'good');
const g = await run('ar', good, 'mask');
check('a good mask: the three checks pass, the preview shows, «أرسلها مع القناع» offered', g.checks.length === 3 && g.checks.every(c => c.startsWith('ok:')) && g.send && g.prev && !g.bare, JSON.stringify(g));
check('sent: the photo (JPEG) then the mask (PNG), both with the session, in that order', g.posts.join() === '/api/photo|image/jpeg|s1,/api/photo-mask|image/png|s1', JSON.stringify(g.posts));
check('the mask sent is pure black and white at the photo\'s sent size (16:9, at most 1920 wide)', g.mask && g.mask.other === 0 && g.mask.w <= 1920 && Math.abs(g.mask.w / g.mask.h - 16 / 9) < 0.02, JSON.stringify(g.mask));
check('the page says both are on the TV; no CSP refusal, no error', /قناعها/.test(g.state) && !g.errors.length, JSON.stringify([g.state, g.errors]));
check('the line that the photo goes to the AI tool the person picks, not to us', /صورتك تروح لهذه الأداة، مو لنا/.test(g.words), g.words);

const shape = await run('ar', await png(800, 600, 'good'), 'bare');
check('a mask of another shape: refused here with the reason, the photo alone offered and sent alone', shape.checks[0].startsWith('bad:') && !shape.send && shape.bare
  && shape.posts.join() === '/api/photo|image/jpeg|s1', JSON.stringify(shape));
const grey = await run('ar', await png(1280, 720, 'grey'), null);
check('a grey picture: refused (not black and white)', grey.checks[1].startsWith('bad:') && !grey.send, JSON.stringify(grey.checks));
const en = await run('en', good, null);
check('English: the box and the checks in English', /not to us/.test(en.words) && en.checks.every(c => !/[؀-ۿ]/.test(c)), JSON.stringify([en.words, en.checks]));

await browser.close();
if (failed) { console.log(`\n${failed} failed`); process.exit(1); }
console.log('\nall passed');
