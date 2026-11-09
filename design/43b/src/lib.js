/* 4.3 round 2b: shared pieces. Pure HTML/JS, no build: open index.html?s=<screen> (see screens.js) at 1920x1080.
   Fonts: the real ones are 'Thmanyah Sans' (UI), 'Thmanyah Serif Display' (editorial headings); the fallbacks are only stand-ins. */
const FONT_SANS = "'Thmanyah Sans','Thmanyah','Noto Sans Arabic','DejaVu Sans',system-ui,sans-serif";
const FONT_SERIF = "'Thmanyah Serif Display','Thmanyah Serif','Noto Naskh Arabic','FreeSerif','DejaVu Serif',serif";
const FONT_MONO = "'Thmanyah Sans','DejaVu Sans Mono',ui-monospace,monospace";
const photo = n => `url('../photos/${n}.jpg')`;

const ICONS = {
  book: '<path d="M3 5.5C5.5 4.5 8.5 4.5 12 6c3.5-1.5 6.5-1.5 9-.5V19c-2.5-1-5.5-1-9 .5C8.5 18 5.5 18 3 19z"/><path d="M12 6v13.5"/>',
  chart: '<path d="M3 17l6-6 4 4 8-9"/><path d="M15 6h6v6"/>',
  phone: '<rect x="7" y="2.5" width="10" height="19" rx="2.5"/><path d="M11 18.5h2"/>',
  wifi: '<path d="M2.5 9a14 14 0 0119 0"/><path d="M5.5 12.5a9.5 9.5 0 0113 0"/><path d="M8.7 16a5 5 0 016.6 0"/><circle cx="12" cy="19.2" r="1" fill="currentColor"/>',
  sliders: '<path d="M4 7h9M17 7h3M4 17h3M11 17h9"/><circle cx="15" cy="7" r="2"/><circle cx="9" cy="17" r="2"/>',
  tv: '<rect x="3" y="4.5" width="18" height="12.5" rx="2.5"/><path d="M8 20.5h8"/>',
  play: '<path d="M8 5l11 7-11 7z" fill="currentColor"/>',
  movie: '<rect x="3" y="4.5" width="18" height="15" rx="2.5"/><path d="M10 9.5v5l4.5-2.5z" fill="currentColor"/>',
  music: '<path d="M9 18V6l10-2v12"/><circle cx="6.5" cy="18" r="2.5"/><circle cx="16.5" cy="16" r="2.5"/>',
  game: '<rect x="2.5" y="7" width="19" height="11" rx="4"/><path d="M7 10.5v4M5 12.5h4M16 11.5h.01M18 13.5h.01"/>',
  photo: '<rect x="3" y="4.5" width="18" height="15" rx="2.5"/><circle cx="9" cy="10" r="1.8"/><path d="M4 18l5.5-5 4 3.5 3-2.5 4 4"/>',
  cast: '<path d="M3 8V6.5A2.5 2.5 0 015.5 4h13A2.5 2.5 0 0121 6.5v11a2.5 2.5 0 01-2.5 2.5H14"/><path d="M3 12a9 9 0 019 9M3 16.5a4.5 4.5 0 014.5 4.5"/><circle cx="3.6" cy="20.4" r=".9" fill="currentColor"/>',
  star: '<path d="M12 3.5l2.6 5.4 5.9.8-4.3 4.1 1.1 5.9L12 16.9l-5.3 2.8 1.1-5.9L3.5 9.7l5.9-.8z"/>',
  check: '<path d="M5 12.5l4.5 4.5L19 7.5"/>',
  mosque: '<path d="M12 3c1.5 2 3.5 3 3.5 5.5A3.5 3.5 0 0112 12 3.5 3.5 0 018.5 8.5C8.5 6 10.5 5 12 3z"/><path d="M4 21V12h16v9M9 21v-4a3 3 0 016 0v4"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2.5v2.5M12 19v2.5M2.5 12H5M19 12h2.5M5.3 5.3l1.8 1.8M16.9 16.9l1.8 1.8M5.3 18.7l1.8-1.8M16.9 7.1l1.8-1.8"/>',
  moon: '<path d="M20 14.5A8.5 8.5 0 019.5 4 8.5 8.5 0 1020 14.5z"/>',
};
const ic = (n, s = 32, w = 1.8) => `<svg width="${s}" height="${s}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="${w}" stroke-linecap="round" stroke-linejoin="round">${ICONS[n]}</svg>`;

/* the app tiles every look shows: name, glyph, colour */
const APPS = [
  ['يوتيوب', 'movie', '#E5484D'], ['التلفزيون', 'tv', '#3B82F6'], ['الموسيقى', 'music', '#14B8A6'], ['ألعاب العائلة', 'game', '#F59E0B'],
  ['الصور', 'photo', '#8B5CF6'], ['البث', 'cast', '#10B981'], ['المفضلة', 'star', '#EC4899'],
];
const QUICK = ['book', 'chart', 'phone', 'wifi', 'sliders'];
const PRAYERS = [['الفجر', '٤:٣٨'], ['الشروق', '٥:٥٩'], ['الظهر', '١١:٥٢'], ['العصر', '٢:٤٦'], ['المغرب', '٥:١١'], ['العشاء', '٦:٤١']];

/* absolute-position shorthand */
const at = (css, inner, extra = '') => `<div style="position:absolute;${css};${extra}">${inner}</div>`;
const mini = (html, w) => {
  const s = w / 1920, h = Math.round(w * 9 / 16);
  return `<div style="position:relative;width:${w}px;height:${h}px;overflow:hidden;border-radius:12px;flex:none;box-shadow:0 0 0 1px rgba(255,255,255,.18)"><div style="position:absolute;left:0;top:0;width:1920px;height:1080px;transform:scale(${s});transform-origin:top left">${html}</div></div>`;
};
