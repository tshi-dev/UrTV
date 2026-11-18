// node render-v2.js : couch-v2-*.html -> ../couch-v2-*.png (720 in the name: 1280x720)
const { chromium } = require('playwright'); const fs = require('fs'), path = require('path');
(async () => { const b = await chromium.launch();
  for (const f of fs.readdirSync(__dirname).filter(f => f.startsWith('couch-v2-') && f.endsWith('.html')).sort()) {
    const s = f.includes('720'); const p = await b.newPage({ viewport: s ? { width: 1280, height: 720 } : { width: 1920, height: 1080 }, deviceScaleFactor: 1 });
    await p.goto('file://' + path.join(__dirname, f), { waitUntil: 'networkidle' }); await p.evaluate(() => document.fonts.ready);
    await p.screenshot({ path: path.join(__dirname, '..', f.replace('.html', '.png')) }); await p.close(); console.log('rendered', f); }
  await b.close(); })();
