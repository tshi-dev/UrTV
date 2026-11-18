// node render.js : every *.html here -> ../<name>.png (720 in the name: 1280x720, else 1920x1080), in name order (01 uses 02's picture: run twice)
const { chromium } = require('playwright');
const fs = require('fs'), path = require('path');
(async () => {
  const b = await chromium.launch();
  for (const f of fs.readdirSync(__dirname).filter(f => f.endsWith('.html')).sort()) {
    const small = f.includes('720');
    const p = await b.newPage({ viewport: small ? { width: 1280, height: 720 } : { width: 1920, height: 1080 }, deviceScaleFactor: 1 });
    await p.goto('file://' + path.join(__dirname, f), { waitUntil: 'networkidle' });
    await p.evaluate(() => document.fonts.ready);
    await p.screenshot({ path: path.join(__dirname, '..', f.replace('.html', '.png')) });
    await p.close();
    console.log('rendered', f);
  }
  await b.close();
})();
