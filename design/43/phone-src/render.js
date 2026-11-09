// NODE_PATH=$(npm root -g) node render.js HTMLDIR OUTDIR  (needs playwright + chromium; real fonts are private: the pictures use stand-in fonts)
const { chromium } = require('playwright'), fs = require('fs'), path = require('path');
(async () => {
  const [src, out] = process.argv.slice(2), b = await chromium.launch(), pg = await b.newPage({ viewport: { width: 840, height: 700 }, deviceScaleFactor: 2 });
  for (const f of fs.readdirSync(src).filter(x => x.endsWith('.html'))) {
    await pg.goto('file://' + path.resolve(src, f)); await pg.waitForTimeout(300);
    await pg.screenshot({ path: path.join(out, f.replace('.html', '.png')), fullPage: true }); console.log('wrote', f);
  }
  await b.close();
})();
