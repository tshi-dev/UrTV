// node design/43b/render.js [out-dir] [name-filter]   (needs playwright + chromium; the previews in preview/ were made with stand-in fonts)
const { chromium } = require('playwright'); const path = require('path'), fs = require('fs');
(async () => {
  const out = process.argv[2] || path.join(__dirname, 'preview'), only = process.argv[3];
  fs.mkdirSync(out, { recursive: true });
  const b = await chromium.launch(), pg = await b.newPage({ viewport: { width: 1920, height: 1080 } });
  const names = await (async () => { await pg.goto('file://' + path.join(__dirname, 'src/index.html')); return pg.evaluate(() => Object.keys(SCREENS)); })();
  for (const n of names) {
    if (only && !n.includes(only)) continue;
    await pg.goto('file://' + path.join(__dirname, 'src/index.html') + '?s=' + encodeURIComponent(n)); await pg.waitForTimeout(400);
    await pg.screenshot({ path: n.startsWith('c-') ? path.join(__dirname, '../43', '2' + (3 + parseInt(n.slice(2, 4)) - 1) + n.slice(4) + '.png') : path.join(n.startsWith('s-') ? path.join(__dirname, 'studies') : out, n + '.png') }); console.log('wrote', n);
  }
  await b.close();
})();
