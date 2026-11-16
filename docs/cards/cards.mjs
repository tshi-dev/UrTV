// the cards of the review, in the order they are seen most
const SAVER = [{ photo: 'typical' }, { photo: 'bright', tag: 'bright' }, { photo: 'dark', tag: 'dark' }];
const HOME = [{ home: true, photo: 'typical' }, { home: true, photo: 'bright', tag: 'bright' }, { home: true, photo: 'dark', tag: 'dark' }, { home: true, glass: true, tag: 'glassclock' }];
const H = (t) => HOME.map(l => ({ ...l, tag: t + (l.tag || 'typical') }));
export const CARDS = [
  { nn: '01', id: 'nextprayer', hash: 'panel=nextprayer', looks: HOME },
  { nn: '02', id: 'prayers', hash: 'panel=prayers', looks: [...SAVER, ...H('home-')] },
  { nn: '03', id: 'verse', hash: 'panel=verse', looks: [...SAVER, ...H('home-')] },
  { nn: '04', id: 'forecast', hash: 'panel=forecast', looks: [...SAVER, ...H('home-')] },
  { nn: '05', id: 'hourly', hash: 'panel=hourly', looks: [...SAVER, ...H('home-')] },
  { nn: '06', id: 'prayerrow', hash: 'panel=prayerrow', looks: [...SAVER, ...H('home-')] },
  { nn: '07', id: 'matches', hash: 'panel=matches&teams=136013&leagues=' + encodeURIComponent('Saudi-Arabian Pro League'), looks: [...SAVER, ...H('home-')], wait: 2500 },
  { nn: '08', id: 'nextmatch', hash: 'panel=nextmatch&teams=136013', looks: [...SAVER, ...H('home-')], wait: 2500 },
  { nn: '09', id: 'stocks', hash: 'panel=stocks&stocks=2222,1120,AAPL,NVDA&mkstyle=prices', looks: [...SAVER, ...H('home-')], wait: 2500 },
  { nn: '10', id: 'countdown', hash: 'panel=verse&slot=countdown&cd=2026-12-18&cdname=' + encodeURIComponent('إجازة نهاية الفصل'), looks: SAVER },
];
