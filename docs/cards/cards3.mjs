// batch 3: the cards seen less often, and the card styles
const SAVER = [{ photo: 'typical' }, { photo: 'bright', tag: 'bright' }, { photo: 'dark', tag: 'dark' }];
const HOME = [{ home: true, photo: 'typical' }, { home: true, photo: 'bright', tag: 'bright' }, { home: true, photo: 'dark', tag: 'dark' }, { home: true, glass: true, tag: 'glassclock' }];
const H = t => HOME.map(l => ({ ...l, tag: t + (l.tag || 'typical') }));
const TASKS = encodeURIComponent('اتصل بالسباك\nشراء حليب وخبز\nموعد الأسنان الأحد\nتجديد الاستمارة');
const NP = () => { window.UrtvNative = Object.assign(window.UrtvNative || {}, { nowPlaying: () => JSON.stringify({ title: 'سورة الكهف', who: 'ماهر المعيقلي', live: false }) }); };
export const CARDS = [
  { nn: '11', id: 'tafsir', hash: 'panel=verse&tafsir=1', looks: [...SAVER, ...H('home-')] },
  { nn: '12', id: 'adhkar', hash: 'panel=adhkar', looks: [...SAVER, ...H('home-')] },
  { nn: '13', id: 'tasks', hash: 'panel=tasks&tasks=' + TASKS, looks: [...SAVER, ...H('home-')] },
  { nn: '14', id: 'nowplaying', hash: 'panel=verse', init: NP, looks: SAVER, wait: 2500 },
  { nn: '15', id: 'wisdom', hash: 'panel=wisdom', looks: [...SAVER, ...H('home-')] },
  { nn: '16', id: 'year', hash: 'panel=year', looks: [...SAVER, ...H('home-')] },
  { nn: '17', id: 'today', hash: 'panel=today&teams=136013&tasks=' + TASKS, looks: [...SAVER, ...H('home-')], wait: 2500 },
  { nn: '18', id: 'shows', hash: 'panel=shows&langs=Arabic', looks: [...SAVER, ...H('home-')], wait: 3000 },
  { nn: '19', id: 'pair', hash: 'clocksize=small&panel=verse&panel2=hourly', looks: SAVER },
  { nn: '20', id: 'styles-dark1', hash: 'panel=prayers&cardstyle=dark1', looks: [SAVER[0]] },
  { nn: '21', id: 'styles-glass1', hash: 'panel=prayers&cardstyle=glass1', looks: [SAVER[0]], wait: 2500 },
  { nn: '22', id: 'styles-dark3', hash: 'panel=verse&cardstyle=dark3', looks: [SAVER[0]] },
  { nn: '23', id: 'styles-glass3', hash: 'panel=verse&cardstyle=glass3', looks: [SAVER[0]], wait: 2500 },
];
