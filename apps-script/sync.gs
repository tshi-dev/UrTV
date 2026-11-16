/**
 * @OnlyCurrentDoc
 *
 * UrTV setup reports → tshi-dev/urtv-reports, every night (docs/REPORTS-SYNC.md).
 *
 * The form that optimize/share.py and the TV's SetupReport.java post to writes one row per report into this spreadsheet
 * (column A the time, column B the report). Each night this script commits the rows added since its last run as
 * reports/YYYY-MM-DD.csv (Riyadh's day of the run) through GitHub's contents API. Nothing else leaves the sheet.
 *
 * - The token is the owner's fine-grained token, limited to tshi-dev/urtv-reports with Contents read and write, kept in
 *   Script Properties as GITHUB_TOKEN. It is never in this file, never logged.
 * - Google scopes: this spreadsheet only (@OnlyCurrentDoc, spreadsheets.currentonly) and outside requests (UrlFetch);
 *   appsscript.json lists exactly these two. The nightly trigger is added by hand in the Triggers panel, so the script
 *   needs no scope to manage triggers.
 * - LAST_ROW (Script Properties) is the last sheet row already sent; it moves only after GitHub said the file is saved,
 *   so a failed night is sent the next night. A run that finds a file of the same day adds its rows to it.
 * - dryRun(): what a run would send, in the log; it reads the sheet and nothing else (no token, no network, no change).
 */

var REPO = 'tshi-dev/urtv-reports';
var BRANCH = 'main';
var DIR = 'reports';
var TZ = 'Asia/Riyadh';
var MAX_ROWS = 2000;                 // per run: a big backlog goes over a few nights
var HEADER = 'row,timestamp,report\n';
var API = 'https://api.github.com/repos/' + REPO + '/contents/';

/** The nightly run (Triggers → Add trigger → syncReports, Time-driven, Day timer). */
function syncReports() {
  return run_(false);
}

/** What syncReports would send, in the log (View → Logs); nothing is sent and nothing is changed. */
function dryRun() {
  return run_(true);
}

function run_(dry) {
  var lock = LockService.getScriptLock();
  if (!lock.tryLock(30000)) {
    Logger.log('Another run is in progress: nothing done.');
    return null;
  }
  try {
    var props = PropertiesService.getScriptProperties();
    var sheet = reportSheet_();
    var last = Number(props.getProperty('LAST_ROW') || '1');   // row 1 is the sheet's header
    if (!(last >= 1)) throw new Error('LAST_ROW is not a row number: ' + props.getProperty('LAST_ROW'));
    var end = sheet.getLastRow();
    if (end <= last) {
      Logger.log('Nothing new since row ' + last + '.');
      return {rows: 0};
    }
    var n = Math.min(end - last, MAX_ROWS);
    var values = sheet.getRange(last + 1, 1, n, 2).getValues();
    var lines = [];
    for (var i = 0; i < values.length; i++) {
      lines.push(csvRow_([String(last + 1 + i), isoTime_(values[i][0]), String(values[i][1] == null ? '' : values[i][1])]));
    }
    var day = Utilities.formatDate(new Date(), TZ, 'yyyy-MM-dd');
    var path = DIR + '/' + day + '.csv';
    if (dry) {
      Logger.log('DRY RUN: rows ' + (last + 1) + '–' + (last + n) + ' (' + n + ') would go to ' + REPO + ':' + path
        + (end - last > n ? ' (' + (end - last - n) + ' more on later nights)' : '') + '. Nothing sent.');
      Logger.log('GITHUB_TOKEN set: ' + (props.getProperty('GITHUB_TOKEN') ? 'yes' : 'NO — add it before the first real run'));
      Logger.log('First row as it would be written:\n' + lines[0].slice(0, 600));
      return {rows: n, path: path, dry: true};
    }
    var token = props.getProperty('GITHUB_TOKEN');
    if (!token) throw new Error('No GITHUB_TOKEN in Script Properties (docs/REPORTS-SYNC.md, step 3).');
    var existing = getFile_(token, path);
    var text = (existing ? ensureNewline_(existing.text) : HEADER) + lines.join('');
    putFile_(token, path, text, existing ? existing.sha : null,
      'Reports: rows ' + (last + 1) + '–' + (last + n) + ' (' + n + ')');
    props.setProperty('LAST_ROW', String(last + n));   // only after GitHub saved the file
    Logger.log('Sent rows ' + (last + 1) + '–' + (last + n) + ' to ' + path + '.');
    return {rows: n, path: path};
  } finally {
    lock.releaseLock();
  }
}

/** The form's responses sheet: the one named in Script Properties SHEET_NAME, else the first sheet. */
function reportSheet_() {
  var book = SpreadsheetApp.getActiveSpreadsheet();
  var name = PropertiesService.getScriptProperties().getProperty('SHEET_NAME');
  var sheet = name ? book.getSheetByName(name) : book.getSheets()[0];
  if (!sheet) throw new Error('No sheet named ' + name);
  return sheet;
}

function isoTime_(v) {
  if (Object.prototype.toString.call(v) === '[object Date]' && !isNaN(v.getTime())) return v.toISOString();
  return String(v == null ? '' : v);
}

/** One CSV row, every field quoted (a report has commas, quotes and line breaks). */
function csvRow_(fields) {
  return fields.map(function (f) { return '"' + String(f).replace(/"/g, '""') + '"'; }).join(',') + '\n';
}

function ensureNewline_(s) {
  return s && s.charAt(s.length - 1) !== '\n' ? s + '\n' : s;
}

function headers_(token) {
  return {
    Authorization: 'Bearer ' + token,
    Accept: 'application/vnd.github+json',
    'X-GitHub-Api-Version': '2022-11-28'
  };
}

/** {sha, text} of the file on the branch, or null when there is none. */
function getFile_(token, path) {
  var r = UrlFetchApp.fetch(API + path + '?ref=' + encodeURIComponent(BRANCH), {
    method: 'get', headers: headers_(token), muteHttpExceptions: true
  });
  var code = r.getResponseCode();
  if (code === 404) return null;
  if (code !== 200) throw new Error('GitHub answered ' + code + ' reading ' + path + ': ' + r.getContentText().slice(0, 300));
  var j = JSON.parse(r.getContentText());
  var bytes = Utilities.base64Decode(String(j.content || '').replace(/\s/g, ''));
  return {sha: j.sha, text: Utilities.newBlob(bytes).getDataAsString('UTF-8')};
}

function putFile_(token, path, text, sha, message) {
  var body = {
    message: message,
    content: Utilities.base64Encode(text, Utilities.Charset.UTF_8),
    branch: BRANCH
  };
  if (sha) body.sha = sha;
  var r = UrlFetchApp.fetch(API + path, {
    method: 'put', headers: headers_(token), contentType: 'application/json',
    payload: JSON.stringify(body), muteHttpExceptions: true
  });
  var code = r.getResponseCode();
  if (code !== 200 && code !== 201) throw new Error('GitHub answered ' + code + ' saving ' + path + ': ' + r.getContentText().slice(0, 300));
}
