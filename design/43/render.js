const {chromium} = require('playwright');
const fs = require('fs'), path = require('path');
(async () => {
  const [src, out] = process.argv.slice(2);
  const b = await chromium.launch(), pg = await b.newPage({viewport: {width: 1920, height: 1080}});
  for (const f of fs.readdirSync(src).filter(f => f.endsWith('.html')).sort()) {
    await pg.goto('file://' + path.resolve(src, f)); await pg.waitForTimeout(300);
    await pg.screenshot({path: path.join(out, f.replace('.html', '.png'))}); console.log('wrote', f);
  }
  await b.close();
})();
