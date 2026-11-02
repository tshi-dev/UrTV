/* UrTV on LG webOS and Samsung Tizen: the part that is not the page.
 *
 * Runs first, before the page's own script (it is the first script in <head>). It
 *   1. turns the settings kept in localStorage into the page's settings link (#city=..&lat=..), which is how the Android apps
 *      hand them to the same page, so the page itself is not changed;
 *   2. maps the remote: arrows, OK and Back (LG 461, Samsung 10009; Tizen needs registerKey for the colour and media keys);
 *   3. draws a small settings screen (city, calculation method, the card, the clock, the adhan) that the remote drives;
 *   4. plays the adhan with web audio at each prayer time, while the app is in front, and says so to the page's adhan bar
 *      the way the Android app does (window.UrtvNative);
 *   5. says in Arabic when the TV's browser is too old for the page, instead of leaving a black screen.
 *
 * Plain ES5 on purpose: it must still run on a browser too old for the page, to say so.
 * BETA: not tested on a real LG or Samsung TV (docs/LG-SAMSUNG.md).
 */
(function () {
  'use strict';

  var KEY_SETTINGS = 'urtv.tv.settings.v1';
  var KEY_FIRED = 'urtv.tv.adhanFired';
  var DATA = window.URTV_TV_DATA || { regions: [], voices: [], fajrVoices: [], cards: [] };

  // ---- the platform and its keys -----------------------------------------------------------------------------------------------
  var UA = navigator.userAgent || '';
  var PLATFORM = window.tizen || /Tizen/i.test(UA) ? 'tizen' : (window.webOS || window.PalmSystem || /Web0S|webOS|NetCast/i.test(UA) ? 'webos' : 'browser');
  var K = { LEFT: 37, UP: 38, RIGHT: 39, DOWN: 40, OK: 13, ESC: 27, BACK_WEBOS: 461, BACK_TIZEN: 10009, YELLOW: 405, BLUE: 406,
            VOL_UP: 447, VOL_DOWN: 448, MUTE: 449 };
  // Back: LG 461, Samsung 10009; Esc and Backspace on a computer
  function isBack(c) { return c === K.BACK_WEBOS || c === K.BACK_TIZEN || c === K.ESC || c === 8; }

  // Tizen delivers the arrows, OK and Back to the page by itself; the colour and media keys only after registerKey.
  function registerTizenKeys() {
    var names = ['ColorF0Red', 'ColorF1Green', 'ColorF2Yellow', 'ColorF3Blue', 'MediaPlay', 'MediaPause', 'MediaPlayPause', 'MediaStop'];
    for (var i = 0; i < names.length; i++) {
      try { window.tizen.tvinputdevice.registerKey(names[i]); } catch (e) { /* not on this TV: the others still go */ }
    }
  }
  if (PLATFORM === 'tizen') registerTizenKeys();

  function exitApp() {
    try {
      if (PLATFORM === 'tizen') { window.tizen.application.getCurrentApplication().exit(); return; }
      if (window.webOS && window.webOS.platformBack) { window.webOS.platformBack(); return; }
      if (window.PalmSystem && window.PalmSystem.platformBack) { window.PalmSystem.platformBack(); return; }
      window.close();
    } catch (e) { /* in a browser there is nothing to exit */ }
  }

  // ---- the settings -----------------------------------------------------------------------------------------------------------
  var DEFAULTS = {
    region: 0, city: 'الرياض', lat: 24.6877, lon: 46.7219, tz: 'Asia/Riyadh',
    pm: 'auto', panel: 'verse', clocksize: 'large', clock24: false, digits: 'hindi',
    adhan: false, voice: 1, fajrVoice: 10, volume: 70, adhanbar: true,
    adj: [0, 0, 0, 0, 0]   // «تعديل وقت الصلاة»: minutes added to الفجر الظهر العصر المغرب العشاء, each −20..20, as the Android app's
  };
  // the settings that change the page (it is reloaded when the screen closes); the adhan's act at once
  var PAGE_KEYS = ['city', 'pm', 'panel', 'clocksize', 'clock24', 'digits', 'adhanbar', 'adj'];
  // the page's own settings these replace; any other # setting a caller passes stays (the tests pass &gap=0)
  var MANAGED = ['city', 'lat', 'lon', 'tz', 'pm', 'panel', 'clocksize', 'clock', 'digits', 'adhanbar', 'teams', 'adj'];

  var ADJ_MAX = 20, ADJ_NAMES = ['الفجر', 'الظهر', 'العصر', 'المغرب', 'العشاء'];
  function readSettings() {
    var s = {}, k, saved = null;
    try { saved = JSON.parse(localStorage.getItem(KEY_SETTINGS) || 'null'); } catch (e) { saved = null; }
    for (k in DEFAULTS) s[k] = DEFAULTS[k];
    if (saved && typeof saved === 'object') for (k in DEFAULTS) if (saved[k] !== undefined && typeof saved[k] === typeof DEFAULTS[k]) s[k] = saved[k];
    return clean(s);
  }
  function clean(s) {
    function num(v, lo, hi, d) { v = +v; return isFinite(v) && v >= lo && v <= hi ? v : d; }
    s.lat = num(s.lat, -90, 90, DEFAULTS.lat);
    s.lon = num(s.lon, -180, 180, DEFAULTS.lon);
    s.volume = Math.round(num(s.volume, 10, 100, DEFAULTS.volume) / 10) * 10;
    if (['auto', 'jafari'].indexOf(s.pm) < 0) s.pm = 'auto';
    if (['large', 'medium', 'small'].indexOf(s.clocksize) < 0) s.clocksize = 'large';
    if (['hindi', 'latin'].indexOf(s.digits) < 0) s.digits = 'hindi';
    var kinds = DATA.cards.map(function (c) { return c.id; });
    if (kinds.length && kinds.indexOf(s.panel) < 0) s.panel = 'verse';
    var voices = DATA.voices.map(function (v) { return v.n; }), fajr = DATA.fajrVoices.map(function (v) { return v.n; });
    if (voices.length && voices.indexOf(s.voice) < 0) s.voice = voices[0];
    if (fajr.length && fajr.indexOf(s.fajrVoice) < 0) s.fajrVoice = fajr[0];
    if (typeof s.tz !== 'string' || !/^[A-Za-z_]+\/[A-Za-z_\-]+$/.test(s.tz)) { s.tz = DEFAULTS.tz; }
    if (typeof s.city !== 'string' || !s.city || s.city.length > 40) s.city = DEFAULTS.city;
    var adj = Array.isArray(s.adj) && s.adj.length === 5 ? s.adj : [0, 0, 0, 0, 0];
    s.adj = adj.map(function (v) { v = Math.round(+v); return isFinite(v) ? Math.max(-ADJ_MAX, Math.min(ADJ_MAX, v)) : 0; });
    return s;
  }
  var S = readSettings();
  function saveSettings() { try { localStorage.setItem(KEY_SETTINGS, JSON.stringify(S)); } catch (e) { /* kept for this run only */ } }

  // the page reads its settings from the link's # part (new URLSearchParams(location.hash)); this makes that part from ours
  function buildHash(s, oldHash) {
    var q = [];
    function add(k, v) { q.push(encodeURIComponent(k) + '=' + encodeURIComponent(v)); }
    add('city', s.city); add('lat', s.lat); add('lon', s.lon); add('tz', s.tz);
    add('panel', s.panel); add('clocksize', s.clocksize);
    if (s.clock24) add('clock', '24');
    if (s.digits === 'latin') add('digits', 'latin');
    if (s.pm === 'jafari') add('pm', 'jafari');
    if (!s.adhanbar) add('adhanbar', '0');
    if (s.adj.some(function (v) { return v !== 0; })) add('adj', s.adj.join(','));   // the page's own #adj (CONFIG.prayerAdj); none while all are 0
    add('teams', '');   // no followed clubs here: the team picker is the Android app's
    var old = String(oldHash || '').replace(/^#/, '').split('&');
    for (var i = 0; i < old.length; i++) {
      var kv = old[i];
      if (kv && MANAGED.indexOf(decodeURIComponent(kv.split('=')[0])) < 0) q.push(kv);
    }
    return '#' + q.join('&');
  }
  try { history.replaceState(null, '', location.pathname + location.search + buildHash(S, location.hash)); } catch (e) { location.hash = buildHash(S, location.hash); }

  // ---- the adhan ----------------------------------------------------------------------------------------------------------------
  var live = null;   // {prayer: 0|2|3|4|5} while one plays
  var audio = null, audioTimer = 0, unlocked = false;
  var PRAYER_INDEX = { Fajr: 0, Dhuhr: 2, Asr: 3, Maghrib: 4, Isha: 5 };   // as the page's adhan bar names them

  // what the Android app answers the page: the page asks every 3 s and draws «أذان المغرب · أي زر يوقف الأذان» while it plays
  window.UrtvNative = {
    adhan: function () { return live ? JSON.stringify({ live: true, prayer: live.prayer, any: true }) : ''; },
    nowPlaying: function () { return ''; }
  };

  function voiceUrl(n, list) {
    for (var i = 0; i < list.length; i++) if (list[i].n === n) return list[i].url;
    return '';
  }
  function stopAdhan() {
    clearTimeout(audioTimer);
    if (audio) { try { audio.pause(); audio.removeAttribute('src'); audio.load(); } catch (e) { /* already gone */ } }
    live = null;
    refreshBar();
  }
  function refreshBar() { try { if (typeof window.adhanLiveTick === 'function') window.adhanLiveTick(); } catch (e) { /* the page is not ready */ } }
  function playAdhan(prayer) {
    var fajr = prayer === 0;
    var url = voiceUrl(fajr ? S.fajrVoice : S.voice, fajr ? DATA.fajrVoices : DATA.voices);
    if (!url) return false;
    stopAdhan();
    try {
      audio = audio || new Audio();
      audio.volume = S.volume / 100;
      audio.onended = stopAdhan;
      audio.onerror = stopAdhan;
      audio.src = url;
      live = { prayer: prayer };
      var p = audio.play();
      if (p && p.catch) p.catch(stopAdhan);   // refused (no key pressed yet on a browser that insists): no adhan, no bar
      audioTimer = setTimeout(stopAdhan, 6 * 60 * 1000);   // an adhan is never longer; a stuck stream does not play for ever
      refreshBar();
      return true;
    } catch (e) { stopAdhan(); return false; }
  }
  // a first key press lets the browser play sound later without one (browsers that refuse autoplay)
  function unlockAudio() {
    if (unlocked) return;
    unlocked = true;
    try {
      audio = audio || new Audio();
      audio.muted = true;
      audio.src = 'data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAESsAABErAAABAAgAZGF0YQAAAAA=';
      var p = audio.play();
      var done = function () { audio.muted = false; try { audio.pause(); } catch (e) {} };
      if (p && p.then) p.then(done, done); else done();
    } catch (e) { /* nothing to unlock */ }
  }

  var lastFired = '';
  try { lastFired = localStorage.getItem(KEY_FIRED) || ''; } catch (e) { lastFired = ''; }
  function adhanTick() {
    if (!S.adhan || live) return;
    var prayers, now;
    try { prayers = PRAYERS; now = riyadhParts(); } catch (e) { return; }   // the page's own: its prayer times, and the city's clock
    if (!prayers || !prayers.length) return;
    var mins = now.h * 60 + now.mi;
    for (var i = 0; i < prayers.length; i++) {
      var x = prayers[i];
      if ((x.h * 60 + x.mi) % 1440 !== mins || PRAYER_INDEX[x.key] === undefined) continue;
      var id = now.y + '-' + now.mo + '-' + now.d + ':' + x.key;
      if (id === lastFired) return;   // once a day, also across a reload in the same minute
      lastFired = id;
      try { localStorage.setItem(KEY_FIRED, id); } catch (e) { /* played once more after a reload at worst */ }
      playAdhan(PRAYER_INDEX[x.key]);
      return;
    }
  }
  setInterval(adhanTick, 1000);

  // ---- the settings screen ------------------------------------------------------------------------------------------------------
  var AR = ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'];
  function arNum(n) { return String(n).replace(/[0-9]/g, function (d) { return AR[+d]; }); }
  function region() { return DATA.regions[S.region] || DATA.regions[0]; }
  function cycle(list, i, dir) { return (i + dir + list.length) % list.length; }   // wraps; dir = 1 (left key) or -1 (right key), as in an RTL list
  function indexOfBy(list, key, v) { for (var i = 0; i < list.length; i++) if (list[i][key] === v) return i; return 0; }

  var CLOCK_SIZES = [['large', 'كبيرة'], ['medium', 'متوسطة'], ['small', 'صغيرة']];
  var ROWS = [
    { id: 'region', label: 'الدولة / المنطقة', show: function () { return region() ? region().name : ''; },
      change: function (d) {
        if (!DATA.regions.length) return;
        S.region = cycle(DATA.regions, S.region, d);
        var c = DATA.regions[S.region].cities[0];
        S.city = c.name; S.lat = c.lat; S.lon = c.lon; S.tz = c.tz;
      }, page: true },
    { id: 'city', label: 'المدينة', show: function () { return S.city; },
      change: function (d) {
        var r = region(); if (!r) return;
        var i = cycle(r.cities, indexOfBy(r.cities, 'name', S.city), d), c = r.cities[i];
        S.city = c.name; S.lat = c.lat; S.lon = c.lon; S.tz = c.tz;
      }, page: true },
    { id: 'pm', label: 'طريقة الحساب', show: function () { return S.pm === 'jafari' ? 'الجعفري' : 'تلقائي (حسب بلد المدينة)'; },
      change: function () { S.pm = S.pm === 'jafari' ? 'auto' : 'jafari'; }, page: true },
    adjRow(0), adjRow(1), adjRow(2), adjRow(3), adjRow(4),
    { id: 'panel', label: 'البطاقة بجانب الساعة', show: function () { return DATA.cards[indexOfBy(DATA.cards, 'id', S.panel)].name; },
      change: function (d) { S.panel = DATA.cards[cycle(DATA.cards, indexOfBy(DATA.cards, 'id', S.panel), d)].id; }, page: true },
    { id: 'clocksize', label: 'حجم الساعة', show: function () { return CLOCK_SIZES[indexOfBy2(CLOCK_SIZES, S.clocksize)][1]; },
      change: function (d) { S.clocksize = CLOCK_SIZES[cycle(CLOCK_SIZES, indexOfBy2(CLOCK_SIZES, S.clocksize), d)][0]; }, page: true },
    { id: 'clock24', label: 'ساعة ٢٤', show: function () { return S.clock24 ? 'نعم' : 'لا'; },
      change: function () { S.clock24 = !S.clock24; }, page: true },
    { id: 'digits', label: 'الأرقام', show: function () { return S.digits === 'latin' ? 'لاتينية 123' : 'هندية ١٢٣'; },
      change: function () { S.digits = S.digits === 'latin' ? 'hindi' : 'latin'; }, page: true },
    { id: 'adhanbar', label: 'تنبيه قبل الأذان', show: function () { return S.adhanbar ? 'تشغيل' : 'إيقاف'; },
      change: function () { S.adhanbar = !S.adhanbar; }, page: true },
    { id: 'adhan', label: 'صوت الأذان (والتطبيق أمامك)', show: function () { return S.adhan ? 'تشغيل' : 'إيقاف'; },
      change: function () { S.adhan = !S.adhan; if (!S.adhan) stopAdhan(); } },
    { id: 'voice', label: 'المؤذن', show: function () { return voiceName(S.voice, DATA.voices); },
      change: function (d) { S.voice = DATA.voices[cycle(DATA.voices, indexOfBy(DATA.voices, 'n', S.voice), d)].n; } },
    { id: 'fajrVoice', label: 'مؤذن الفجر', show: function () { return voiceName(S.fajrVoice, DATA.fajrVoices); },
      change: function (d) { S.fajrVoice = DATA.fajrVoices[cycle(DATA.fajrVoices, indexOfBy(DATA.fajrVoices, 'n', S.fajrVoice), d)].n; } },
    { id: 'volume', label: 'مستوى صوت الأذان', show: function () { return arNum(S.volume) + '٪'; },
      change: function (d) { S.volume = Math.max(10, Math.min(100, S.volume - d * 10)); if (audio) audio.volume = S.volume / 100; } },
    { id: 'test', label: 'تجربة الأذان الآن', show: function () { return live ? 'اضغط لإيقافه' : 'اضغط OK'; },
      action: function () { if (live) stopAdhan(); else playAdhan(2); } },
    { id: 'close', label: 'حفظ وإغلاق', show: function () { return ''; }, action: function () { closePanel(); } }
  ];
  // «تعديل وقت الصلاة» for one prayer: the left key adds a minute, the right key takes one away (−20..+20); the page moves the time,
  // its countdown and the adhan with it
  function adjRow(i) {
    return { id: 'adj' + i, label: 'تعديل ' + ADJ_NAMES[i],
      show: function () { var v = S.adj[i]; return (v > 0 ? '+' : v < 0 ? '−' : '') + arNum(Math.abs(v)) + ' د'; },
      change: function (d) { S.adj[i] = Math.max(-ADJ_MAX, Math.min(ADJ_MAX, S.adj[i] + d)); }, page: true };
  }
  function indexOfBy2(pairs, v) { for (var i = 0; i < pairs.length; i++) if (pairs[i][0] === v) return i; return 0; }
  function voiceName(n, list) { var i = indexOfBy(list, 'n', n); return list.length ? list[i].name : ''; }

  var panel = null, sel = 0, dirty = false;
  function paint() {
    if (!panel) return;
    var html = '<div class="t">إعدادات تلفزيونك</div>';
    for (var i = 0; i < ROWS.length; i++) {
      var r = ROWS[i], v = r.show();
      html += '<div class="r' + (i === sel ? ' on' : '') + '" data-row="' + r.id + '"><span class="l">' + r.label + '</span>' +
              '<span class="v">' + (r.change ? '‹ ' : '') + esc(v) + (r.change ? ' ›' : '') + '</span></div>';
    }
    html += '<div class="h">↑ ↓ تنقّل · ← → تغيير · OK تنفيذ · رجوع: حفظ وإغلاق</div>';
    panel.innerHTML = html;
    var on = panel.querySelector('.r.on');   // more rows than the screen holds: the chosen one is kept in sight
    if (on && on.scrollIntoView) on.scrollIntoView({ block: 'nearest' });
  }
  function esc(s) { return String(s).replace(/[&<>"']/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]; }); }
  function openPanel() {
    if (panel || !DATA.regions.length) return;
    panel = document.createElement('div');
    panel.id = 'urtv-tv-settings';
    panel.setAttribute('dir', 'rtl');
    panel.setAttribute('role', 'dialog');
    var st = document.createElement('style');
    st.id = 'urtv-tv-settings-css';
    st.textContent =
      '#urtv-tv-settings{position:fixed;z-index:99999;top:50%;left:50%;transform:translate(-50%,-50%);width:52vw;max-height:94vh;overflow-y:auto;' +
      'background:rgba(8,14,11,.94);border:.2vh solid rgba(214,177,92,.55);border-radius:1.6vh;padding:2vh 2.4vw;color:#fff;' +
      'font:400 2.5vh/1.2 Thmanyah,"Noto Naskh Arabic","Noto Sans Arabic",Tahoma,sans-serif;box-shadow:0 2vh 8vh rgba(0,0,0,.6)}' +
      '#urtv-tv-settings .t{font-size:3.4vh;font-weight:500;text-align:center;margin-bottom:1.4vh;color:#e6c56e}' +
      '#urtv-tv-settings .r{display:flex;justify-content:space-between;align-items:center;gap:2vw;padding:.9vh 1.2vw;border-radius:.9vh;border:.3vh solid transparent}' +
      '#urtv-tv-settings .r.on{background:rgba(214,177,92,.18);border-color:#d6b15c}' +
      '#urtv-tv-settings .v{color:#e6c56e;text-align:left}' +
      '#urtv-tv-settings .h{margin-top:1.4vh;font-size:2vh;text-align:center;color:rgba(255,255,255,.6)}';
    document.head.appendChild(st);
    document.body.appendChild(panel);
    sel = 0; dirty = false;
    paint();
  }
  function closePanel() {
    if (!panel) return;
    panel.parentNode.removeChild(panel);
    var st = document.getElementById('urtv-tv-settings-css');
    if (st) st.parentNode.removeChild(st);
    panel = null;
    saveSettings();
    if (dirty) { dirty = false; location.reload(); }   // the page reads its settings once, at the start: it starts again with the new ones
  }
  function onPanelKey(c) {
    var r = ROWS[sel];
    if (c === K.UP) sel = cycle(ROWS, sel, -1);
    else if (c === K.DOWN) sel = cycle(ROWS, sel, 1);
    else if ((c === K.LEFT || c === K.RIGHT) && r.change) {
      var before = JSON.stringify(S);
      r.change(c === K.LEFT ? 1 : -1);   // RTL: the left key goes on in the list, the right key goes back
      S = clean(S);
      if (JSON.stringify(S) !== before && r.page) dirty = true;
      saveSettings();
    } else if (c === K.OK) { if (r.action) r.action(); else if (r.change) { r.change(1); S = clean(S); if (r.page) dirty = true; saveSettings(); } }
    else if (isBack(c)) { closePanel(); return; }
    paint();
  }

  // ---- the remote ----------------------------------------------------------------------------------------------------------------
  document.addEventListener('keydown', function (e) {
    var c = e.keyCode;
    unlockAudio();
    if (c === K.VOL_UP || c === K.VOL_DOWN || c === K.MUTE) return;   // the TV's own, as on Android: the volume is never held back
    var eat = function () { e.preventDefault(); e.stopPropagation(); if (e.stopImmediatePropagation) e.stopImmediatePropagation(); };
    if (live && !panel) { eat(); stopAdhan(); return; }   // «أي زر يوقف الأذان»: the first key stops it and is spent on that
    if (panel) { eat(); onPanelKey(c); return; }
    if (c === K.OK || c === K.YELLOW || c === K.BLUE) { eat(); openPanel(); return; }
    if (isBack(c)) { eat(); exitApp(); }
  }, true);

  // ---- a browser too old for the page ---------------------------------------------------------------------------------------------
  // The page uses async functions, object spread and Intl's parts: Chromium 63 or newer (LG webOS 5, Samsung Tizen 5.0 and later).
  var modern = true;
  try { new Function('async function f(){} var a = {b: 1}; return {...a, c: 2}'); } catch (e) { modern = false; }
  if (typeof URLSearchParams === 'undefined' || !(window.Intl && Intl.DateTimeFormat && Intl.DateTimeFormat.prototype.formatToParts)) modern = false;
  if (!modern) {
    document.addEventListener('DOMContentLoaded', function () {
      var d = document.createElement('div');
      d.setAttribute('dir', 'rtl');
      d.id = 'urtv-tv-old';
      d.style.cssText = 'position:fixed;inset:0;z-index:99999;background:#07100b;color:#fff;display:flex;align-items:center;justify-content:center;' +
                        'text-align:center;font:400 4vh/1.6 Tahoma,sans-serif;padding:8vw';
      d.textContent = 'متصفح هذا التلفزيون أقدم من أن يشغّل شاشة تلفزيونك. يحتاج LG بنظام webOS 5 أو أحدث، أو Samsung بنظام Tizen 5 أو أحدث.';
      document.body.appendChild(d);
    });
  }

  // for the tests, and for a person who opens the page in a browser to look
  window.UrtvTV = {
    platform: PLATFORM,
    settings: function () { return JSON.parse(JSON.stringify(S)); },
    adhanState: function () { return { live: !!live, prayer: live ? live.prayer : -1, src: audio ? audio.getAttribute('src') || '' : '', volume: audio ? audio.volume : -1 }; },
    panelOpen: function () { return !!panel; },
    modern: modern
  };
})();
