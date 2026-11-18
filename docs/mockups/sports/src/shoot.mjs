// Draws docs/mockups/sports/*.png: every card of cards.mjs in the four places it is seen (960x540 dp at 2x = 1920x1080).
//   FONT_DIR=<the woff2 of @fontsource/ibm-plex-sans-arabic> node docs/mockups/sports/src/shoot.mjs   then: python3 docs/mockups/sports/src/pack.py
import { chromium } from 'playwright';
import { CARDS } from './cards.mjs';
import { PLACES, shooter } from './places.mjs';

const only = process.argv.slice(2);
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 960, height: 540 }, deviceScaleFactor: 2 });
const shot = await shooter(page);
for (const card of CARDS) {
  if (only.length && !only.some(o => card.id.startsWith(o))) continue;
  if (card.states) {   // «مباراة اليوم»: before, live and after on the screensaver; the live one in the other places
    for (const [st, c] of Object.entries(card.states)) await shot(`${card.id}-${st}-saver-tv`, PLACES['s-tv'](c, true));
    const live = card.states.live;
    await shot(`${card.id}-before-home-tv`, PLACES['h-tv'](card.states.before));
    await shot(`${card.id}-live-home-couch`, PLACES['h-couch'](live));
    await shot(`${card.id}-live-saver-couch`, PLACES['s-couch'](live));
    continue;
  }
  await shot(`${card.id}-saver-tv`, PLACES['s-tv'](card));
  await shot(`${card.id}-home-tv`, PLACES['h-tv'](card));
  await shot(`${card.id}-home-couch`, PLACES['h-couch'](card));
  await shot(`${card.id}-saver-couch`, PLACES['s-couch'](card));
}
await browser.close();
