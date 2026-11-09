const {chromium} = require('playwright');
const fs = require('fs'), path = require('path');
(async () => {
  const [src, out] = process.argv.slice(2);
  const b = await chromium.launch(), pg = await b.newPage({viewport: {width: 1920, height: 1080}});
  for (const f of fs.readdirSync(src).filter(f => f.endsWith('.html')).sort()) {
    await pg.goto('file://' + path.resolve(src, f)); await pg.waitForTimeout(300);
    const sz = await pg.evaluate(() => [document.body.offsetWidth, document.body.offsetHeight]); await pg.setViewportSize({width: sz[0], height: sz[1]});   // a page may be taller than 1080 (the motion strip)
    await pg.screenshot({path: path.join(out, f.replace('.html', '.png'))}); console.log('wrote', f);
  }
  await b.close();
})();
