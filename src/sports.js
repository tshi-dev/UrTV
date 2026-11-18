// 4.4 «الرياضة»: the sports cards and the page's side of the kick-off alert (docs/mockups/sports/README.md, approved). Built into
// dist/sports.js (and sports-en.js through i18n/en/page.tsv) by src/build.py, and read by the page (loadSportsJs) only when a sports card is
// chosen or the kick-off alert is on; it runs only beside the page of its own build (the stamp below). It uses the page's own helpers
// (matchEvents, teamNext, archLoad, h2hLine…) and adds nothing to tv.html's budget.
const SP_STAMP = 'SPORTS_BUILD:__BUILD__:';
// the clubs' two colours (no crest, no photo): by TheSportsDB id; any other club a neutral pair
const SP_COL = {'136013':['#1E4FA1','#FFFFFF'], '136022':['#F7C600','#1B3A8A'], '136018':['#F2C200','#111111'], '137721':['#0A7A3A','#FFFFFF'],
  '136020':['#FFFFFF','#1A1A1A'], '136017':['#0B8A3E','#D4232B'], '136015':['#C8102E','#F2C200'], '136011':['#1D6B3A','#FFFFFF'], '136012':['#F2C200','#2B5BA8'],
  '139080':['#F2C200','#0A7A3A'], '147445':['#111111','#E2001A'], '136014':['#F28C00','#1A1A1A'], '137852':['#E30613','#111111'], '136200':['#E30613','#FFFFFF'],
  '147444':['#5B2C83','#FFFFFF'], '149112':['#C8102E','#FFFFFF'], '150638':['#1E4FA1','#F2C200'], '150637':['#111111','#2BB5A8'], '136137':['#0A7A3A','#FFFFFF']};
const SP_KSA = '136137';
const spCol = id => SP_COL[String(id)] || ['#8A8F98', '#5A5F68'];
const spSw = id => { const c = spCol(id); return `<i class="spsw" style="--a:${c[0]};--b:${c[1]}"></i>`; };
const spBand = id => { const c = spCol(id); return `<i class="spband" style="--a:${c[0]};--b:${c[1]}"></i>`; };
const spForm = f => f ? `<span class="spfm">${[...String(f).slice(0, 5)].map(x => `<i class="${x === 'w' ? 'w' : x === 'd' ? 'd' : 'l'}"></i>`).join('')}</span>` : '';
const spName = (ev, side) => teamAr(ev['str' + side + 'Team'], ev['id' + side + 'Team']);
const spWhen = ms => (fbDayOf(ms) === cityToday() ? '' : fbDayWord(fbDayOf(ms)) + ' ') + fbClock(ms);
const SPW = {noToday:'لا مباراة لفرقك اليوم', next:'القادمة: %s', startsIn:'تبدأ بعد', kick:'الانطلاقة', venue:'الملعب', channel:'القناة', ended:'انتهت',
  live:'مباشر', minute:'الدقيقة %s', goals:'أهداف %s', noLive:'لا مباريات جارية الآن', noTable:'الترتيب غير متاح الآن', pts:'%s نقطة', noNext:'لا مباريات قادمة لفرقك',
  home:'على أرضه', away:'خارج أرضه', noYesterday:'لا نتائج أمس', yours:'فرقك', noWeekend:'لا مباريات نهاية هذا الأسبوع', noCup:'لا أدوار إقصائية بعد',
  noF1:'لا بيانات للسباق القادم', race:'السباق', fp1:'التجربة الأولى', fp2:'التجربة الثانية', fp3:'التجربة الثالثة', sq:'تأهيل السبرنت', sprint:'السبرنت', q:'التأهيل',
  noNat:'لا مباراة قادمة للمنتخب', lastMeet:'آخر مواجهة', place:'المركز %s، %s نقطة', top:'%s الأول بـ%s نقطة', inRound:'%s في %s', countdown:'بعد %s',
  outIn:'%s ودّع من %s', through:'تأهل', out:'ودّع', agg:'مجموع المباراتين', phase:'مرحلة الدوري', matchday:'الجولة %s من %s', placeOf:'المركز %s من %s، %s نقطة', noScorers:'المصدر لا يذكر أسماء الهدّافين لهذه المباراة'};
// ---- the look: the cards session's (centred, «مربعات», gold for the person's own), sizes in em of the card's own text
(() => {
  const st = document.createElement('style');
  st.textContent = `
.week li.spr{display:block;text-align:center}
.spsw{display:inline-block;width:.62em;height:.62em;border-radius:.14em;margin-inline-end:.35em;vertical-align:.02em;background:linear-gradient(90deg,var(--a) 50%,var(--b) 50%)}
.spband{display:block;width:3.2em;height:.28em;border-radius:.14em;margin:0 auto .3em;background:linear-gradient(90deg,var(--a) 50%,var(--b) 50%)}
.spfm{display:inline-flex;gap:.18em;vertical-align:middle}.spfm i{width:.5em;height:.5em;border-radius:.1em;display:inline-block}
.spfm .w{background:#3BB273}.spfm .d{background:#9A9A9A}.spfm .l{background:#E05A4F}
.spgame{display:flex;align-items:center;justify-content:center;gap:1.2em}
.spgame .spt{display:flex;flex-direction:column;align-items:center;gap:.15em;min-width:5em}
.spgame .spt b{font-size:1.25em;font-weight:700}
.spgame .spm{display:flex;flex-direction:column;align-items:center}
.spgame .spm b{font-size:1.9em;font-weight:700;line-height:1.1;white-space:nowrap}
.sps{font-size:.78em;opacity:.8}
.spbx{display:grid;gap:.4em;margin-top:.5em}
.spbox{border:1px solid rgba(255,255,255,.16);border-radius:.55em;background:rgba(255,255,255,.05);padding:.35em .3em;text-align:center;min-width:0}
.spbox b{display:block;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.spbox.on{background:#EBC77F;border-color:#EBC77F;color:#1A1712}.spbox.on .sps{color:#4A3F2A;opacity:1}
.sprow{display:grid;grid-template-columns:1fr auto 1fr auto;align-items:center;gap:.6em;padding:.18em .5em}
.sprow .h{text-align:end}.sprow .a{text-align:start}.sprow .x{font-weight:700;white-space:nowrap}
.sprow.on,.sptr.on{background:#EBC77F;color:#1A1712;border-radius:.45em}.sprow.on .sps{color:#7A2018;opacity:1}
.sptr{display:grid;grid-template-columns:1.6em 1fr auto 2.4em;align-items:center;gap:.5em;padding:.1em .5em;text-align:start}
.sptr b:last-child{text-align:end}
.spsec{font-size:.78em;opacity:.8;text-align:start;margin-top:.2em}
.spbar{height:.28em;border-radius:.14em;background:rgba(255,255,255,.14);margin:.45em 0 .1em;overflow:hidden}.spbar i{display:block;height:100%;background:#E9C46A}
.splive{color:#FF7A6E;font-weight:700}
.week li.spc{display:none}
html.row .week li.spf{display:none}html.row .week li.spc{display:block;text-align:center}
.spc b{display:block;font-size:1.2em;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
html.sptake:not(.widget) .mid .clockwrap{display:none}
html.sptake:not(.widget) .mid .week.sptk{grid-column:1/-1;font-size:1.25em}
.spclk{font-weight:400;opacity:.85}
/* the notice (spKick): the top right of the screensaver, 8 s, cream text on nearly solid dark glass (nothing reads through it) */
#spkick{position:fixed;top:4.5vh;right:2.5vw;width:min(36vw,700px);z-index:60;border-radius:1.4vh;background:rgba(12,14,18,.97);
  border:1px solid rgba(233,196,106,.6);box-shadow:0 1.2vh 3.2vh rgba(0,0,0,.5);color:#F4EBD8;padding:1.3vh 1.5vw 0;font-size:max(2.5vh,13px);line-height:1.45;overflow:hidden;
  transition:transform .45s ease,opacity .45s ease;text-align:center}
#spkick.out{transform:translateX(calc(100% + 4vw));opacity:0}   /* the top right in both languages (the owner's corner), so it leaves to the right */
#spkick .kt{display:flex;justify-content:space-between;gap:1em;color:#F0D48E;font-weight:500}#spkick .kt i{font-style:normal;color:#E5DCC8;font-weight:400}
#spkick .km{display:flex;align-items:center;justify-content:center;gap:1.1em;margin:1vh 0 1.2vh;font-size:1.55em;font-weight:700;color:#FFF8EC}
#spkick .km .spsw{width:.7em;height:.7em}
#spkick .kb{height:.35vh;margin:0 -1.5vw;background:#E9C46A;transform-origin:inline-end;animation:spkb 8s linear forwards}
@keyframes spkb{to{transform:scaleX(0)}}
#spkick.goal{border-color:#3BB273}
/* on the home the page is only the card's own window: the goal and the final score (the kick-off is the launcher's own, at the screen's corner)
   as a band over the card's top, the card's size; in the couch strip one line, the clubs and the score */
html.widget #spkick{top:.45em;left:.45em;right:.45em;width:auto;font-size:13px;border-radius:.7em;padding:.45em .9em 0}
html.widget #spkick .km{margin:.3em 0 .45em}html.widget #spkick .kb{margin:0 -.9em;height:3px}
html.widget #spkick.out{transform:translateY(-130%)}
html.row #spkick{top:0;bottom:0;left:0;right:0;border-radius:inherit;display:flex;flex-direction:column;justify-content:center;padding:0 1em}
html.row #spkick .kt{display:none}html.row #spkick .km{margin:0}html.row #spkick .kb{position:absolute;bottom:0;inset-inline:0;margin:0}
/* a light look on the home («نظيف»، «رملي»، «شفق»: html.hlt): the boxes, the time bar and a white half of a club's colours keep an edge on white, the live minute a darker red */
html.hlt .week .spbox{border-color:rgba(0,0,0,.14);background:rgba(0,0,0,.035)}html.hlt .week .spbox.on{background:#EBC77F;border-color:#D4A94F}
html.hlt .week .spbar{background:rgba(0,0,0,.1)}html.hlt .week .splive{color:#B3261E}
html.hlt .week .spband,html.hlt .week .spsw,html.hlt .week .spfm i{box-shadow:0 0 0 1px rgba(0,0,0,.22)}`;
  document.head.appendChild(st);
})();

// ---- the data: one file per league (sports/<id>.json, the archive's), the F1 file, the national team's file; at the games' pace
const SP = {lg: {}, f1: null, nat: null, told: new Set(), score: {}};
const spUses = k => CARDS.some(c => c.kind === k);
function spLeagueIds(){   // the followed leagues, then the leagues of the followed teams' games (at most 6)
  const ids = [];
  CONFIG.leagues.forEach(n => { const id = catId(n) || (LEAGUE_DATA[n] && ((LEAGUE_DATA[n].events || []).find(e => e && e.idLeague) || {}).idLeague); if(id) ids.push(String(id)); });
  matchEvents().filter(fbMine).forEach(ev => { if(ev.idLeague) ids.push(String(ev.idLeague)); });
  const ix = (ARCH_C[ARCH_BASE + 'index.json'] || {}).j, teams = ix && ix.teams || {};
  CONFIG.teamIds.forEach(id => ((teams[id] || {}).leagues || []).forEach(l => ids.push(String(l))));   // the cups a club plays in, even with no game of them next
  return [...new Set(ids)].slice(0, 6);
}
async function spLoad(){
  if(spUses('cuppath')) await archLoad('index.json').catch(() => null);
  if(CARDS.some(c => SPORT_KINDS.includes(c.kind) && c.kind !== 'formula' && c.kind !== 'national'))
    await Promise.all(spLeagueIds().map(async id => { const j = await archLoad('sports/' + id + '.json').catch(() => null); if(j) SP.lg[id] = j; }));
  if(spUses('formula')) SP.f1 = await archLoad('f1/next.json').catch(() => null) || SP.f1;
  if(spUses('national')) SP.nat = await archLoad('teams/' + SP_KSA + '.json').catch(() => null) || SP.nat;
  renderPanel(false, ...SPORT_KINDS);
  spHandOff();
  spSaverKick();
  spScoreWatch(Date.now());
}
// a game of a sports file in the page's shape (archEv), its league's names on it
const spEv = g => archEv(g);

// ---- 1 «مباراة اليوم»: before (the countdown, each club's place and form, the kick-off, the venue, the channel, the clubs' last meetings),
// live (the score, the minute, how far the game is), after (the score, the goals when the data names them, the table's top); on the
// screensaver it takes the middle from 3 hours before the kick-off to an hour after the end (the takeover), the time in its title
function spToday(now){
  const today = cityToday(now), mine = matchEvents().filter(ev => fbMine(ev) && !calledOff(ev) && fbDayOf(fbKick(ev)) === today).sort((a, b) => fbKick(a) - fbKick(b));
  return mine.find(ev => !fbDone(ev, now)) || mine[mine.length - 1] || null;
}
function spRow(ev, id){   // the club's row of its league's table in the sports file: {rank, pts, f}
  const t = SP.lg[String(ev.idLeague)], rows = (t && t.table) || [], i = rows.findIndex(r => r.id === String(id));
  return i < 0 ? null : {rank: i + 1, pts: rows[i].pts, f: rows[i].f};
}
// TheSportsDB's goal lines: «12':Salem;45+2':Fahad;» (its usual form), «Salem 12'», «Salem (OG) 70'»; each as «Salem ١٢»
const spGoal = x => {
  let m = /^(\d+(?:\+\d+)?)'?\s*:?\s*(.+)$/.exec(x);
  if(m) return `${m[2].trim()} ${ar(m[1])}`;
  m = /^(.+?)\s+(\d+(?:\+\d+)?)'?$/.exec(x);
  return m ? `${m[1].trim()} ${ar(m[2])}` : x;
};
const spGoals = s => String(s || '').split(';').map(x => x.replace(/\s+/g, ' ').trim()).filter(Boolean).map(spGoal).join('، ');
function spMatchday(card, now){
  const ev = spToday(now);
  document.documentElement.classList.toggle('sptake', false);
  card.el.classList.remove('sptk');
  if(!ev){
    const n = nextTeamGame(now);
    return {rows: [`<li class="spr spf">${SPW.noToday}${n ? `<div class="sps">${esc(fw(SPW.next, spName(n, 'Home') + ' × ' + spName(n, 'Away') + '، ' + spWhen(fbKick(n))))}</div>` : ''}</li>`,
      `<li class="spc">${SPW.noToday}</li>`]};
  }
  const k = fbKick(ev), live = fbLive(ev, now), done = fbDone(ev, now), m = markOf(ev);
  const side = s => { const id = ev['id' + s + 'Team'], r = spRow(ev, id);
    return `<div class="spt">${spBand(id)}<b>${esc(spName(ev, s))}</b>${!live && !done && r ? `<span class="sps">${fw(SPW.place, ar(r.rank), ar(r.pts))}</span>${spForm(r.f)}` : ''}</div>`; };
  const score = `${ar(+ev.intHomeScore || 0)} - ${ar(+ev.intAwayScore || 0)}`;
  const mid = live ? `<b>${score}</b><span class="sps splive">${esc(fbStatus(ev, now))}</span>`
    : done ? `<b>${fbScored(ev) ? score : '×'}</b><span class="sps">${SPW.ended}</span>` : `<span class="sps">${SPW.startsIn}</span><b>${esc(shortRemaining(k - now))}</b>`;
  const rows = [`<li class="spr spf"><div class="spgame">${side('Home')}<div class="spm">${mid}</div>${side('Away')}</div></li>`];
  if(live) rows.push(`<li class="spr spf"><div class="spbar"><i style="width:${Math.min(100, Math.max(2, (now - k) / FB_DUR * 100)).toFixed(0)}%"></i></div></li>`);
  if(!live && !done){
    rows.push(`<li class="spr spf"><div class="spbx" style="grid-template-columns:repeat(3,1fr)"><div class="spbox"><span class="sps">${SPW.kick}</span><b>${fbClock(k)}</b></div>`
      + `<div class="spbox"><span class="sps">${SPW.venue}</span><b>${esc(ev.strVenue ? venueName(ev.strVenue) : '—')}</b></div><div class="spbox"><span class="sps">${SPW.channel}</span><b>${esc(m ? m.mark : '—')}</b></div></div></li>`);
    const meet = h2hLine(ev);
    if(meet) rows.push(`<li class="spr spf"><span class="sps">${esc(meet)}</span></li>`);
  } else {
    const g = [['Home', ev.strHomeGoalDetails], ['Away', ev.strAwayGoalDetails]].filter(x => spGoals(x[1]));
    if(g.length) rows.push(`<li class="spr spf"><div class="spbx" style="grid-template-columns:repeat(${g.length},1fr)">${g.map(([s, d]) => `<div class="spbox"><span class="sps">${esc(fw(SPW.goals, spName(ev, s)))}</span><b>${esc(spGoals(d))}</b></div>`).join('')}</div></li>`);
    else if(+ev.intHomeScore + +ev.intAwayScore > 0) rows.push(`<li class="spr spf"><span class="sps">${SPW.noScorers}</span></li>`);   // the free source names no scorer for this game: said, not left blank
    if(!g.length && done){ const t = SP.lg[String(ev.idLeague)], r = t && t.table && t.table[0]; if(r) rows.push(`<li class="spr spf"><span class="sps">${esc(fw(SPW.top, r.ar || teamAr(r.n, r.id), ar(r.pts)))}</span></li>`); }
  }
  rows.push(`<li class="spc"><b>${esc(spName(ev, 'Home'))} ${live || done ? score : '×'} ${esc(spName(ev, 'Away'))}</b><span class="sps">${esc(live ? SPW.live + '، ' + fbStatus(ev, now) : done ? SPW.ended : fbClock(k) + '، ' + fw(SPW.countdown, shortRemaining(k - now)))}</span></li>`);
  // the takeover: the screensaver (not the home's cards) from 3 hours before the kick-off to an hour after the end
  const take = !CONFIG.widget && now >= k - 3 * 3600000 && now <= k + FB_DUR + 3600000;
  document.documentElement.classList.toggle('sptake', take);
  card.el.classList.toggle('sptk', take);
  return {title: take ? `<span>${esc(PANEL_TITLE.matchday)}</span><span class="spclk">${esc(fbClock(now))}</span>` : '', titleHtml: take, rows};
}

// ---- 2 «مباشر الآن»: the games under way of the followed teams and leagues (at most 4), the person's own first and gold
function spLivenow(now){
  const live = matchEvents().filter(ev => fbLive(ev, now) && !calledOff(ev)).sort((a, b) => (fbMine(b) - fbMine(a)) || fbKick(a) - fbKick(b)).slice(0, 4);
  if(!live.length) return {rows: [`<li class="spr spf">${SPW.noLive}</li>`, `<li class="spc">${SPW.noLive}</li>`]};
  return {rows: live.map(ev => `<li class="spr spf"><div class="sprow${fbMine(ev) ? ' on' : ''}"><b class="h">${spSw(ev.idHomeTeam)}${esc(spName(ev, 'Home'))}</b><b class="x">${ar(+ev.intHomeScore || 0)} - ${ar(+ev.intAwayScore || 0)}</b>`
    + `<b class="a">${spSw(ev.idAwayTeam)}${esc(spName(ev, 'Away'))}</b><span class="sps splive">${esc(fbStatus(ev, now))}</span></div></li>`)
    .concat(`<li class="spc"><b>${esc(spName(live[0], 'Home'))} ${ar(+live[0].intHomeScore || 0)} - ${ar(+live[0].intAwayScore || 0)} ${esc(spName(live[0], 'Away'))}</b><span class="sps">${esc(fbStatus(live[0], now))}</span></li>`)};
}

// ---- 3 the mini table: the top 4, then the followed club with its neighbours, each with its last 5 (small boxes); from the archive's table
function spTableOf(){   // the league of the first followed team that has a table, else the first followed league
  for(const id of CONFIG.teamIds) for(const t of Object.values(SP.lg)) if((t.table || []).some(r => r.id === id) && !t.cup) return {t, me: id};
  const t = Object.values(SP.lg).find(x => (x.table || []).length && !x.cup);
  return t ? {t, me: ''} : null;
}
function spMinitable(){
  const q = spTableOf();
  if(!q) return {rows: [`<li class="spr spf">${SPW.noTable}</li>`, `<li class="spc">${SPW.noTable}</li>`]};
  const rows = q.t.table, mi = rows.findIndex(r => r.id === q.me), pick = new Set([0, 1, 2, 3]);
  if(mi > 3){ [mi - 1, mi, mi + 1].forEach(i => { if(i >= 0 && i < rows.length) pick.add(i); }); } else [4, 5, 6].forEach(i => { if(i < rows.length) pick.add(i); });
  let prev = -1;
  const out = [...pick].sort((a, b) => a - b).map(i => { const r = rows[i], gap = prev >= 0 && i > prev + 1 ? '<div class="sps">…</div>' : ''; prev = i;
    return `${gap}<div class="sptr${i === mi ? ' on' : ''}"><span>${ar(i + 1)}</span><b>${spSw(r.id)}${esc(r.ar || teamAr(r.n, r.id))}</b>${spForm(r.f)}<b>${ar(r.pts)}</b></div>`; }).join('');
  const me = mi >= 0 ? rows[mi] : rows[0];
  return {title: q.t.ar || leagueName({idLeague: q.t.id, strLeague: q.t.en}) || PANEL_TITLE.minitable,
    rows: [`<li class="spr spf">${out}</li>`, `<li class="spc"><b>${esc(me.ar || teamAr(me.n, me.id))} ${fw(SPW.place, ar((mi >= 0 ? mi : 0) + 1), ar(me.pts))}</b></li>`]};
}

// ---- 4 «مباريات فرقك القادمة»: a box for each followed club (at most 3): the other club, the day, the time, the competition, home or away
function spNextmatches(now){
  const games = CONFIG.teamIds.map(id => { const ev = teamNext(id).find(e => e && e.idEvent && !calledOff(e) && fbKick(e) + durMs(e) > now); return ev && {id, ev}; }).filter(Boolean).slice(0, 3);
  if(!games.length) return {rows: [`<li class="spr spf">${SPW.noNext}</li>`, `<li class="spc">${SPW.noNext}</li>`]};
  const box = ({id, ev}) => { const home = String(ev.idHomeTeam) === id, k = fbKick(ev);
    return `<div class="spbox">${spBand(id)}<b>${esc(teamAr(home ? ev.strHomeTeam : ev.strAwayTeam, id))}</b><span class="sps">× ${esc(home ? spName(ev, 'Away') : spName(ev, 'Home'))}</span>`
      + `<b>${esc(fbDayWord(fbDayOf(k)))}</b><b>${fbClock(k)}</b><span class="sps">${esc(fbRound(ev))}</span><span class="sps">${home ? SPW.home : SPW.away}</span></div>`; };
  const f = games[0];
  return {rows: [`<li class="spr spf"><div class="spbx" style="grid-template-columns:repeat(${games.length},1fr)">${games.map(box).join('')}</div></li>`,
    `<li class="spc"><b>${esc(spName(f.ev, 'Home'))} × ${esc(spName(f.ev, 'Away'))}</b><span class="sps">${esc(spWhen(fbKick(f.ev)))}</span></li>`]};
}

// ---- 5 «نتائج أمس»: the followed clubs' results first, then each league's (the followed ones first), the winner in bold, at most 7
function spYesterday(now){
  const day = fbDayAdd(cityToday(now), -1), seen = new Set(), all = [];
  const take = ev => { if(ev && ev.idEvent && !seen.has(String(ev.idEvent)) && fbDayOf(fbKick(ev)) === day && fbDone(ev, now) && fbScored(ev)){ seen.add(String(ev.idEvent)); all.push(ev); } };
  matchEvents().forEach(take);
  Object.values(SP.lg).forEach(t => (t.last || []).map(spEv).forEach(take));
  if(!all.length) return {rows: [`<li class="spr spf">${SPW.noYesterday}</li>`, `<li class="spc">${SPW.noYesterday}</li>`]};
  const mine = all.filter(fbMine), rest = all.filter(ev => !fbMine(ev)), by = {};
  rest.forEach(ev => { const k = String(ev.idLeague || ''); (by[k] = by[k] || []).push(ev); });
  const groups = (mine.length ? [[SPW.yours, mine]] : []).concat(Object.entries(by).map(([k, v]) => [leagueName(v[0]) || '', v]));
  let n = 0;
  const line = ev => { const h = +ev.intHomeScore, a = +ev.intAwayScore;
    return `<div class="sprow${fbMine(ev) ? ' on' : ''}"><span class="h">${h > a ? '<b>' : ''}${spSw(ev.idHomeTeam)}${esc(spName(ev, 'Home'))}${h > a ? '</b>' : ''}</span><b class="x">${ar(h)} - ${ar(a)}</b>`
      + `<span class="a">${a > h ? '<b>' : ''}${spSw(ev.idAwayTeam)}${esc(spName(ev, 'Away'))}${a > h ? '</b>' : ''}</span><span></span></div>`; };
  const html = groups.map(([name, evs]) => { const take = evs.slice(0, Math.max(0, 7 - n)); n += take.length; return take.length ? `<div class="spsec">${esc(name)}</div>` + take.map(line).join('') : ''; }).join('');
  const f = (mine[0] || all[0]);
  return {rows: [`<li class="spr spf">${html}</li>`, `<li class="spc"><b>${esc(spName(f, 'Home'))} ${ar(+f.intHomeScore)} - ${ar(+f.intAwayScore)} ${esc(spName(f, 'Away'))}</b></li>`]};
}

// ---- 6 «نهاية الأسبوع»: the games of the coming Thursday, Friday and Saturday (this week's while it lasts) of the followed leagues, a day
// in a box, the followed clubs' gold
function spWeekend(now){
  const t = cityToday(now), wd = new Date(t + 'T12:00:00Z').getUTCDay(), start = fbDayAdd(t, wd >= 4 && wd <= 6 ? 4 - wd : (4 - wd + 7) % 7), days = [0, 1, 2].map(i => fbDayAdd(start, i));
  const seen = new Set(), by = {};
  const take = ev => { if(ev && ev.idEvent && !seen.has(String(ev.idEvent)) && !calledOff(ev) && days.includes(fbDayOf(fbKick(ev)))){ seen.add(String(ev.idEvent)); (by[fbDayOf(fbKick(ev))] = by[fbDayOf(fbKick(ev))] || []).push(ev); } };
  matchEvents().forEach(take);
  Object.values(SP.lg).forEach(x => (x.next || []).map(spEv).forEach(take));
  const total = Object.values(by).reduce((a, v) => a + v.length, 0);
  if(!total) return {rows: [`<li class="spr spf">${SPW.noWeekend}</li>`, `<li class="spc">${SPW.noWeekend}</li>`]};
  const box = d => `<div class="spbox"><b>${esc(fbDayWord(d))}</b>${(by[d] || []).sort((a, b) => (fbMine(b) - fbMine(a)) || fbKick(a) - fbKick(b)).slice(0, 3).map(ev =>
    `<div class="${fbMine(ev) ? 'spbox on' : ''}" style="margin-top:.3em"><div style="font-weight:700;font-size:.85em">${esc(spName(ev, 'Home'))} × ${esc(spName(ev, 'Away'))}</div><span class="sps">${fbClock(fbKick(ev))}</span></div>`).join('')}</div>`;
  const first = days.map(d => (by[d] || []).sort((a, b) => fbKick(a) - fbKick(b))[0]).find(Boolean);
  return {rows: [`<li class="spr spf"><div class="spbx" style="grid-template-columns:repeat(3,1fr)">${days.map(box).join('')}</div></li>`,
    `<li class="spc"><b>${esc(fw(fbCount(total)))}</b><span class="sps">${esc(spName(first, 'Home') + ' × ' + spName(first, 'Away') + '، ' + spWhen(fbKick(first)))}</span></li>`]};
}

// ---- 8 «طريق الكأس»: a followed cup's knockout rounds (the archive's), the round's box with the followed club gold
function spCuppath(){
  const ids = new Set(CONFIG.teamIds), mineT = t => ids.has(String(t.a)) || ids.has(String(t.b));
  const cups = Object.values(SP.lg).filter(x => x.cup && ((x.cup.rounds || []).length || x.cup.phase));
  const ko = cups.find(x => (x.cup.rounds || []).some(r => (r.ties || []).some(mineT)));
  const ph = !ko && cups.find(x => x.cup.phase && x.cup.phase.groups.some(g => g.some(r => ids.has(String(r.id)))));
  const t = ko || ph || cups[0];
  if(!t) return {rows: [`<li class="spr spf">${SPW.noCup}</li>`, `<li class="spc">${SPW.noCup}</li>`]};
  const title = t.ar || leagueName({idLeague: t.id, strLeague: t.en}) || PANEL_TITLE.cuppath;
  const nm = (id, en) => teamAr(en, String(id));
  if(!ko && t.cup.phase && (ph || !(t.cup.rounds || []).length)){   // the league or group phase: the club's group, its place, the matchday
    const p = t.cup.phase, g = p.groups.find(x => x.some(r => ids.has(String(r.id)))) || p.groups[0], mi = g.findIndex(r => ids.has(String(r.id)));
    const pick = [...new Set([0, 1, 2, ...(mi > 2 ? [mi - 1, mi] : [3, 4])])].filter(i => i >= 0 && i < g.length).sort((a, b) => a - b);
    let prev = -1;
    const rows = pick.map(i => { const r = g[i], gap = prev >= 0 && i > prev + 1 ? '<div class="sps">…</div>' : ''; prev = i;
      return `${gap}<div class="sptr${i === mi ? ' on' : ''}"><span>${ar(i + 1)}</span><b>${spSw(r.id)}${esc(r.ar || nm(r.id, r.n))}</b>${spForm(r.f)}<b>${ar(r.pts)}</b></div>`; }).join('');
    const me = mi >= 0 ? g[mi] : null, md = fw(SPW.matchday, ar(p.md), ar(p.of));
    return {title, rows: [`<li class="spr spf"><div class="sps">${esc(SPW.phase + '، ' + md)}</div>${rows}</li>`,
      `<li class="spc"><b>${esc(me ? (me.ar || nm(me.id, me.n)) + ' ' + fw(SPW.placeOf, ar(mi + 1), ar(g.length), ar(me.pts)) : SPW.phase)}</b><span class="sps">${esc(md)}</span></li>`]};
  }
  // the knockout path: the club's tie in each of the last four rounds it played (the cup's latest ties when no club of yours is in it)
  const rounds = (t.cup.rounds || []).filter(r => !ko || (r.ties || []).some(mineT)).slice(-4);
  const side = tie => { const a = ids.has(String(tie.b)) ? 1 : 0, us = a ? [tie.b, tie.bn] : [tie.a, tie.an], them = a ? [tie.a, tie.an] : [tie.b, tie.bn];
    return {us, them, ag: tie.ag ? [tie.ag[a], tie.ag[1 - a]] : null}; };
  const col = r => { const tie = (r.ties || []).find(mineT) || (r.ties || [])[0];
    if(!tie) return `<div class="spbox"><span class="sps">${esc(r.ar)}</span></div>`;
    const s = side(tie), nxt = tie.legs.find(l => l.hs == null || l.hs === ''), legs = tie.legs.length;
    const res = s.ag ? `${ar(s.ag[0])} - ${ar(s.ag[1])}` : nxt ? esc(spWhen(Date.parse(nxt.t))) : '×';
    const verdict = !ko || !s.ag ? '' : s.ag[0] > s.ag[1] ? SPW.through : s.ag[0] < s.ag[1] ? SPW.out : '';
    return `<div class="spbox${ko ? ' on' : ''}"><span class="sps">${esc(r.ar)}</span><b>× ${esc(nm(s.them[0], s.them[1]))}</b><b>${res}</b>`
      + `<span class="sps">${esc([legs > 1 && s.ag ? SPW.agg : '', verdict].filter(Boolean).join('، '))}</span></div>`; };
  const last = rounds[rounds.length - 1], lt = last && ((last.ties || []).find(mineT) || null), club = lt && side(lt).us;
  const lost = lt && lt.ag && side(lt).ag[0] < side(lt).ag[1];
  return {title, rows: [`<li class="spr spf"><div class="spbx" style="grid-template-columns:repeat(${rounds.length || 1},1fr);align-items:start">${rounds.map(col).join('')}</div></li>`,
    `<li class="spc"><b>${esc(club ? fw(lost ? SPW.outIn : SPW.inRound, nm(club[0], club[1]), last.ar) : title)}</b></li>`]};
}

// ---- 10a «فورمولا ١»: the next race and its sessions in the city's time, the next session gold, the time left
function spF1(now){
  const f = SP.f1;
  if(!f || !Array.isArray(f.sessions) || !f.sessions.length) return {rows: [`<li class="spr spf">${SPW.noF1}</li>`, `<li class="spc">${SPW.noF1}</li>`]};
  const ss = f.sessions.map(s => ({k: s.k, t: Date.parse(s.t)})).filter(s => !isNaN(s.t)), race = ss.find(s => s.k === 'race') || ss[ss.length - 1];
  const show = ss.filter(s => ['q', 'sprint', 'race'].includes(s.k) || ss.length <= 3).slice(-3), nxt = ss.find(s => s.t + 2 * 3600000 > now) || race;
  const name = f.ar || f.name;
  return {title: PANEL_TITLE.formula, rows: [`<li class="spr spf"><b style="font-size:1.2em">${esc(name)}</b><div class="sps">${esc(f.circuit || '')}</div>`
      + `<div class="spbx" style="grid-template-columns:repeat(${show.length + 1},1fr)">${show.map(s => `<div class="spbox${s === nxt ? ' on' : ''}"><span class="sps">${esc(SPW[s.k] || s.k)}</span><b>${esc(fbDayWord(fbDayOf(s.t)))}</b><b>${fbClock(s.t)}</b></div>`).join('')}`
      + `<div class="spbox"><span class="sps">${esc(SPW.race)}</span><b>${race.t > now ? esc(fw(SPW.countdown, shortRemaining(race.t - now).split(' و ')[0])) : SPW.ended}</b></div></div></li>`,
    `<li class="spc"><b>${esc(name)}</b><span class="sps">${esc(SPW.race + ' ' + spWhen(race.t))}</span></li>`]};
}

// ---- 10c «المنتخب»: the Saudi national team's next game (followed or not: its archive file), the other side, the day and the time, the
// competition, their last meeting and the two forms
function spNational(now){
  const own = teamNext(SP_KSA).find(e => e && e.idEvent && !calledOff(e) && fbKick(e) + durMs(e) > now);
  const arch = SP.nat && (SP.nat.next || []).map(g => archEv(g, SP.nat)).find(e => e && fbKick(e) + durMs(e) > now);
  const ev = own || arch;
  if(!ev) return {rows: [`<li class="spr spf">${SPW.noNat}</li>`, `<li class="spc">${SPW.noNat}</li>`]};
  const k = fbKick(ev), other = String(ev.idHomeTeam) === SP_KSA ? 'Away' : 'Home', r = h2hOf(SP_KSA, ev['id' + other + 'Team']);
  const side = s => `<div class="spt">${spBand(ev['id' + s + 'Team'])}<b>${esc(spName(ev, s))}</b>${spForm(formOf(ev['id' + s + 'Team']).join(''))}</div>`;
  const boxes = (leagueName(ev) ? [`<div class="spbox"><span class="sps">${esc(leagueName(ev))}</span><b>${esc(fbRoundNo(ev) || leagueName(ev))}</b></div>`] : [])
    .concat(r ? [`<div class="spbox"><span class="sps">${SPW.lastMeet}</span><b>${esc(h2hLine(ev).split(': ').pop())}</b></div>`] : []);
  return {rows: [`<li class="spr spf"><div class="spgame">${side('Home')}<div class="spm"><b style="font-size:1.4em">${esc(fbDayWord(fbDayOf(k)))}</b><span class="sps">${fbClock(k)}</span></div>${side('Away')}</div></li>`]
    .concat(boxes.length ? [`<li class="spr spf"><div class="spbx" style="grid-template-columns:repeat(${boxes.length},1fr)">${boxes.join('')}</div></li>`] : []).concat([
    `<li class="spc"><b>${esc(spName(ev, 'Home'))} × ${esc(spName(ev, 'Away'))}</b><span class="sps">${esc(spWhen(k))}</span></li>`])};
}

// ---- the kick-off alert, the page's side. The launcher keeps the times and decides (the sound, the rules, over another app): the page
// hands it the followed clubs' coming games each time its games load, and draws the corner notice when the launcher asks (spKick)
// the launcher asks for them (spKickList) with evaluateJavascript: the page is given no JavaScript interface
let spHanded = '';
function spHandOff(){
  if(!CONFIG.widget || _Q.get('ko') === '0') return;
  const now = Date.now(), seen = new Set();
  const list = matchEvents().filter(ev => fbMine(ev) && !calledOff(ev) && fbKick(ev) > now && fbKick(ev) - now < 8 * 86400000 && /^\d+$/.test(String(ev.idEvent)) && !seen.has(ev.idEvent) && seen.add(ev.idEvent))
    .slice(0, 12).map(ev => ({id: String(ev.idEvent), t: String(fbKick(ev)), h: spName(ev, 'Home'), a: spName(ev, 'Away'), hc: spCol(ev.idHomeTeam), ac: spCol(ev.idAwayTeam),
      lg: fbRound(ev)}));
  spHanded = JSON.stringify(list);
}
function spKickList(){ return spHanded; }
// the notice: {kind: 'start'|'goal'|'end', h, a, hc, ac, lg, hs, as, min} for 8 s, then it slides away (the screensaver's kick-off; a goal or the final score on the home and the screensaver)
function spKick(json){
  let d; try{ d = typeof json === 'string' ? JSON.parse(json) : json; }catch(e){ return false; }
  if(!d || !d.h || !d.a) return false;
  const old = document.getElementById('spkick'); if(old) old.remove();
  const sw = c => `<i class="spsw" style="--a:${esc((c || [])[0] || '#888')};--b:${esc((c || [])[1] || '#555')}"></i>`;
  const top = d.kind === 'goal' ? (d.min ? fw(SPW.minute, ar(d.min)) : SPW.live) : d.kind === 'end' ? SPW.ended : SPW.kickoff;
  const mid = d.kind === 'start' || !d.kind ? '×' : `${ar(+d.hs || 0)} - ${ar(+d.as || 0)}`;
  document.body.insertAdjacentHTML('beforeend', `<div id="spkick" class="out${d.kind === 'goal' ? ' goal' : ''}"><div class="kt"><span>${esc(top)}</span><i>${esc(d.lg || '')}</i></div>`
    + `<div class="km"><span>${sw(d.hc)}${esc(d.h)}</span><span>${mid}</span><span>${esc(d.a)}${sw(d.ac)}</span></div><div class="kb"></div></div>`);
  const el = document.getElementById('spkick');
  requestAnimationFrame(() => el.classList.remove('out'));
  setTimeout(() => { el.classList.add('out'); setTimeout(() => el.remove(), 600); }, 8000);
  return true;
}
SPW.kickoff = 'بدأت المباراة';

// the screensaver's own notice at a followed club's kick-off (the home app has no hand-off there: it plays the whistle itself);
// one timer for the next kick-off within the hour, checked again at its time: not called off, the network up; else nothing
let spSaverT = 0;
function spSaverKick(){
  if(CONFIG.widget || _Q.get('ko') === '0' || _Q.get('kids') === '1' || _Q.get('matchpage') === '1') return;   // kids mode: the home's notice only
  clearTimeout(spSaverT);
  const now = Date.now();
  const ev = matchEvents().filter(e => fbMine(e) && !calledOff(e) && !timeTbd(e) && fbKick(e) > now - 30000 && fbKick(e) - now < 3600000).sort((a, b) => fbKick(a) - fbKick(b))[0];
  if(!ev) return;
  const id = String(ev.idEvent);
  spSaverT = setTimeout(() => {
    const now2 = Date.now(), e2 = matchEvents().find(e => String(e.idEvent) === id) || ev;
    if(SP.told.has('s' + id) || calledOff(e2) || navigator.onLine === false || Math.abs(fbKick(e2) - now2) > 900000) return;
    SP.told.add('s' + id);
    spKick({kind: 'start', h: spName(e2, 'Home'), a: spName(e2, 'Away'), hc: spCol(e2.idHomeTeam), ac: spCol(e2.idAwayTeam), lg: fbRound(e2)});
  }, Math.max(0, fbKick(ev) - now));
}
// a goal and the final whistle of a followed club (&kogoal=1, &koend=1): from the scores the page already reloads; never on the first look
function spScoreWatch(now){
  const goal = _Q.get('kogoal') === '1', end = _Q.get('koend') === '1';
  if(!goal && !end || _Q.get('ko') === '0' || (!CONFIG.widget && _Q.get('kids') === '1')) return;
  matchEvents().filter(fbMine).forEach(ev => {
    const id = String(ev.idEvent), hs = +ev.intHomeScore || 0, as = +ev.intAwayScore || 0, live = fbLive(ev, now), done = fbDone(ev, now) && ev.intHomeScore != null && ev.intHomeScore !== '';
    const was = SP.score[id];
    SP.score[id] = {hs, as, live, done};
    if(!was || calledOff(ev)) return;
    const base = {h: spName(ev, 'Home'), a: spName(ev, 'Away'), hc: spCol(ev.idHomeTeam), ac: spCol(ev.idAwayTeam), lg: fbRound(ev), hs, as};
    if(end && done && !was.done && was.live) spKick({...base, kind: 'end'});
    else if(goal && live && (hs + as) > (was.hs + was.as)) spKick({...base, kind: 'goal', min: String(ev.strProgress || '').replace(/\D/g, '')});
  });
}

// ---- 4.5 the honours (docs/HONOURS-STUDY.md), the part of «المباريات» the league and club pages call (SPORTS.hon.league, SPORTS.hon.club): a league's past seasons from the archive (sports/<id>/<season>.json: the final table, every result, the champion it is sure of),
// its champions season by season and its top scorers from the weekly files (data-honours, urtv-data first: champions/ cross-checked, scorers/
// CC BY-SA 4.0, their source said under them). A club's count of titles only when its league's list is complete; else its latest title only.
const HW = {nowS:'الحالي', tableOf:'جدول الترتيب النهائي %s', resultsOf:'نتائج %s', scorersOf:'الهدّافون %s', champMark:'البطل',
  undecided:'لم يُحسم بعد، افتح الجدول', ptsGap:'%s نقطة، بفارق %s عن %s', ptsLevel:'%s نقطة، بالتساوي مع %s', finalWon:'النهائي %s على %s',
  goalsFew:'%s أهداف', goalsMany:'%s هدفًا', fromArchive:'من أرشيفنا', older:'قبل ذلك', srcChamps:'المصدر: نتائج المباريات وويكي بيانات',
  srcChampsWp:'المصدر: نتائج المباريات وويكي بيانات وويكيبيديا (CC BY-SA 4.0)', srcScorers:'المصدر: ويكيبيديا وويكي بيانات (CC BY-SA 4.0)',
  noScorers:'لا نعرف هدّاف هذا الموسم بعد', noChamps:'لا نعرف أبطال هذا الدوري بعد', honours:'البطولات', lastTitle:'آخرها %s',
  title1:'لقب واحد', title2:'لقبان', titleFew:'%s ألقاب', titleMany:'%s لقبًا', titlesLast:'%s، آخرها %s',};
const HON_BASE = DATA_OLD + 'data-honours/';   // the weekly job's branch (.github/workflows/honours.yml); a move to urtv-data adds it to DATA_MAP and the apps' DataHost together
const honWant = (kind, id) => archWant(HON_BASE + kind + '/' + id + '.json');
const honSeasonName = x => ar(String(x));
const honOk = x => /^\d{4}(-\d{4})?$/.test(String(x || ''));
const honGoals = n => fw(n <= 10 ? HW.goalsFew : HW.goalsMany, ar(n));
const honTitles = n => fw(n === 1 ? HW.title1 : n === 2 ? HW.title2 : n <= 10 ? HW.titleFew : HW.titleMany, ar(n));
const honSeasons = sp => ((sp && Array.isArray(sp.seasons) && sp.seasons) || []).filter(honOk).map(String).slice(0, 8);
// the side's seasons: ▲ ▼ walk them, OK (or ◀) chooses one, the current one is marked
function honPins(v, list, chosen){
  if(list.length < 2) return [];
  return list.map((x, i) => ({key: 'season' + x, side: honSeasonName(x), mark: i ? '' : HW.nowS, sel: x === chosen, games: [],
    pick: () => { v.season = x; MP.zone = 'rows'; MP.r = 0; mpRender(); }}));
}
function honTable(v, id){
  const f = archWant('sports/' + id + '/' + v.season + '.json'), ch = f && f.champ, rows = [];
  ((f && !f.cup && Array.isArray(f.table) && f.table) || []).filter(x => x && /^\d+$/.test(String(x.id)) && x.p > 0).forEach((x, i) =>
    rows.push(mpItem(ar(i + 1) + '. ' + clubNm(x.ar, x.n, String(x.id)), (ch && String(ch.id) === String(x.id) ? HW.champMark + '، ' : '')
      + fw(FB_W.pts, ar(+x.pts || 0)) + '، ' + fw(FB_W.played, ar(+x.p || 0)), () => mpGo({k: 'club', id: String(x.id)}))));
  if(!rows.length && ch) rows.push(honChampRow(v, ch));   // a cup's season: its champion
  return [{key: 't', name: fw(HW.tableOf, honSeasonName(v.season)), games: rows, none: f === undefined ? FB_W.loading : FB_W.noData}];
}
function honResults(v, id){
  const f = archWant('sports/' + id + '/' + v.season + '.json'), by = new Map(), groups = [];
  ((f && f.results) || []).map(g => archEv(g)).filter(Boolean).forEach(ev => { const k = fbRound(ev) || FB_W.results; if(!by.has(k)) by.set(k, []); by.get(k).push(ev); });
  by.forEach((games, k) => groups.push({key: 'r' + groups.length, name: k, games, dated: 1}));
  return groups.length ? groups : [{key: 'r', name: fw(HW.resultsOf, honSeasonName(v.season)), games: [], none: f === undefined ? FB_W.loading : FB_W.noData}];
}
// a league's champions, newest first: the weekly file's (cross-checked), and the archive's own for a season it does not have yet (one it
// left out for a disagreement stays out)
function honChampList(id, sp){
  const h = honWant('champions', id), list = ((h && Array.isArray(h.seasons) && h.seasons) || []).filter(c => c && honOk(c.s));
  const have = new Set(list.map(c => String(c.s)).concat((h && h.held) || []));
  const own = ((sp && sp.champs) || []).filter(c => c && honOk(c.s) && !have.has(String(c.s))).map(c => Object.assign({src: 'archive'}, c, {en: c.n}));
  return {list: own.concat(list).sort((a, b) => String(b.s).localeCompare(String(a.s))), file: h};
}
function honChampRow(v, c){
  const name = clubNm(c.ar, c.en || c.n, c.id ? String(c.id) : ''), them = c.vs || c.vn ? clubNm(c.var, c.vn, String(c.vs || '')) : '';
  const sub = c.pts != null && them ? fw(c.gap ? HW.ptsGap : HW.ptsLevel, ar(c.pts), ...(c.gap ? [ar(c.gap), them] : [them]))
    : Array.isArray(c.score) && them ? fw(HW.finalWon, ar(c.score[0]) + '-' + ar(c.score[1]), them) : '';
  return mpItem(honSeasonName(c.s) + ': ' + name, sub, c.id ? () => mpGo({k: 'club', id: String(c.id)}) : () => {});
}
function honChamps(v, id, sp){
  const {list, file} = honChampList(id, sp), cur = sp && honOk(sp.season) ? String(sp.season) : '';
  const recent = [], older = [];
  if(cur && !list.some(c => String(c.s) === cur))   // the season under way: OK opens its table as it stands
    recent.push(mpItem(honSeasonName(cur), HW.undecided, () => { v.tab = 0; v.season = undefined; MP.l = MP.r = 0; mpRender(); }));
  list.forEach(c => (/archive/.test(String(c.src || '')) ? recent : older).push(honChampRow(v, c)));
  const foot = list.some(c => /wikipedia/.test(String(c.src || ''))) ? HW.srcChampsWp : HW.srcChamps;
  if(!list.length) return [{key: 'c', name: FB_W.champs, games: recent, none: file === undefined || sp === undefined ? FB_W.loading : HW.noChamps}];
  return [{key: 'c', name: HW.fromArchive, games: recent, foot: older.length ? '' : foot}].filter(g => g.games.length)
    .concat(older.length ? [{key: 'o', name: HW.older, games: older, foot}] : []);
}
// the seasons «الهدّافون» can show: the scorers file's and the archive's, newest first, 8
function honScorerSeasons(sc, sp){
  const all = new Set(honSeasons(sp).concat(Object.keys((sc && sc.seasons) || {}).filter(honOk)));
  return [...all].sort((a, b) => b.localeCompare(a)).slice(0, 8);
}
const honTop = (sc, x) => ((sc && sc.seasons && sc.seasons[x] && sc.seasons[x].top) || []).filter(t => t && (t.ar || t.en) && +t.goals > 0);
function honScorers(sc, x){
  const top = honTop(sc, x);
  return [{key: 's', name: x ? fw(HW.scorersOf, honSeasonName(x)) : FB_W.scorers, games: top.map(t => mpItem(plNm(t), honGoals(+t.goals), () => {})),
    none: sc === undefined ? FB_W.loading : HW.noScorers, foot: top.length ? HW.srcScorers : ''}];
}
// the club page's «البطولات»: each competition it has won, its latest title, and the count only when that league's list is complete
function honClub(id, j){
  const lgs = new Set();
  ((j && j.last) || []).concat((j && j.next) || []).forEach(g => g && g.lg && lgs.add(String(g.lg)));
  fbGames().forEach(ev => { if(String(ev.idHomeTeam) === id || String(ev.idAwayTeam) === id) lgs.add(String(ev.idLeague || '')); });
  const rows = [];
  [...lgs].filter(x => /^\d+$/.test(x)).forEach(lg => {
    const {list, file} = honChampList(lg, archWant('sports/' + lg + '.json')), won = list.filter(c => String(c.id || '') === id);
    if(!won.length) return;
    const n = file && file.complete && file.titles && +file.titles[id];
    rows.push(mpItem(lgName(lg), n > 0 ? fw(HW.titlesLast, honTitles(n), honSeasonName(won[0].s)) : fw(HW.lastTitle, honSeasonName(won[0].s)),
      () => { mpGo({k: 'league', id: lg, tab: 3}); MP.d = Math.max(0, (MP.tabs || []).findIndex(x => x.on)); }));
  });
  return rows.length ? [{key: 'hon', name: HW.honours, games: rows}] : [];
}

// which of the league's honours have data (a tab without it is not shown): «الأبطال» when the weekly file or the archive names a champion,
// «الهدّافون» when the scorers file has a season
function honHas(id){
  const sp = archWant('sports/' + id + '.json'), {list} = honChampList(id, sp), sc = honWant('scorers', id);
  return {champs: list.length > 0, scorers: Object.keys((sc && sc.seasons) || {}).some(x => honTop(sc, x).length)};
}
// the league page: {pins} (the side's seasons) and, when this part draws the tab, {groups}: a past season's table or results, «الأبطال», «الهدّافون»
function honLeague(v, id, tab){
  const sp0 = archWant('sports/' + id + '.json'), cur = sp0 && sp0.season ? String(sp0.season) : '';
  if(tab === 3) return {pins: [], groups: honChamps(v, id, sp0)};
  if(tab === 4){ const sc = honWant('scorers', id), list = honScorerSeasons(sc, sp0), pick = v.season || list.find(x => honTop(sc, x).length) || list[0];
    return {pins: honPins(v, list, pick), groups: honScorers(sc, pick)}; }
  if(tab > 1) return {pins: []};
  const pins = honPins(v, honSeasons(sp0), v.season || cur);
  return v.season && v.season !== cur ? {pins, groups: tab ? honResults(v, id) : honTable(v, id)} : {pins};
}

SPORTS = {
  stamp: SP_STAMP,
  hon: {league: honLeague, club: honClub, has: honHas},
  load: spLoad,
  card(card, P){
    const now = Date.now();
    const q = P === 'matchday' ? spMatchday(card, now) : P === 'livenow' ? spLivenow(now) : P === 'minitable' ? spMinitable() : P === 'nextmatches' ? spNextmatches(now)
      : P === 'yesterday' ? spYesterday(now) : P === 'weekend' ? spWeekend(now) : P === 'cuppath' ? spCuppath() : P === 'formula' ? spF1(now) : spNational(now);
    return q;
  },
};
