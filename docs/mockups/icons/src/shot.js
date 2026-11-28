const { chromium } = require('playwright');
(async () => { const b = await chromium.launch(); const p = await b.newPage({ viewport: { width: 1600, height: 900 }, deviceScaleFactor: 1 });
  await p.goto('file://' + process.argv[2]); await p.waitForTimeout(500); await p.screenshot({ path: process.argv[3], fullPage: true }); await b.close(); })();
