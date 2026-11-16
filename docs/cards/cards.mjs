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
];
