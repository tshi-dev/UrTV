# The automatic tester round

One command on the owner's PC (the TV on the network, adb working: `optimize/find-tv.sh` did it):

    tester/auto/round.sh                 install apk-next over the installed build (data kept), run every check, write the report
    tester/auto/round.sh --no-install    the build already on the TV
    tester/auto/round.sh --only layouts,health     some steps only (install layouts settings saver radio display font adhan games health)
    tester/auto/round.sh --tag apk-latest --build 4.4b4-629 --prev tester/auto/out/<older build>

It writes `tester/auto/out/<build>/`: `report.md` (the verdict first read at the bottom, GREEN or RED with the reasons), `shots/` (every screen; with Pillow a
`-city.png` copy with the city boxed where it is on the screen), `metrics.tsv`, `steps.tsv`, and `raw/` (every answer of the TV, so a doubt can be settled).
The report has a column with the previous build's numbers (`--prev`, else the newest other folder of `out/`) and the change.

When it is done, to hand it to the others: `git add tester/auto/out/<build>` and push it to the branch `work/tester-report` (the folder is not committed to the
release branches).

## What it checks

| step | what |
|---|---|
| install | `APK_TAG=<tag> optimize/install.sh saver` and `launcher`: over the installed ones, so the data stays; a `!!` line stops the round before anything else changed |
| layouts | every home layout (classic, couch, football) × every card (none, stocks, matches): the home comes to the front, a screenshot |
| settings | each settings style (buttons, couch) opened to each top section; the section's marker row must be on the screen |
| saver | `start-saver.sh`, then `dumpsys dreams` says a dream runs |
| radio | each station started the way a person starts it: the تلاوة screen, the stations tab, walk down until the focused row is the station's name, OK; 30 s with `MusicService` / `UrTVMute` logs; playing, and the mute not changed |
| display | 1280×720 at density 213: the home and a settings section; put back |
| font | font scale 1.3: the home and the settings; put back |
| adhan | the per-prayer state (both, sound, screen, none) of each of the five prayers set by command and read back from the status |
| games | the games list (`QuizGamesActivity`) is on the screen; the badges are for a person to look at in `shots/games-list.png` |
| health | cold start of the home and of the saver (`am start -W`), PSS (`dumpsys meminfo`), jank over a 20 s scripted D-pad walk (`dumpsys gfxinfo … framestats`), crashes (`logcat -b crash`), ANRs and crashes in dropbox since the round began, frozen-screen lines |

The limits that turn the report red are in `thresholds.tsv`; a FAIL step turns it red whatever the numbers. What is opened is in `screens.tsv` (the layouts, the cards,
the settings sections with the row each must show, the styles, the prayers): when the settings tree changes, change that file, not the script.

## What it never does

- never presses a key blind: a key is sent only after the screen it is for was read (the front app, or the screen's own text through `uiautomator`); a screen that is not
  the expected one is a FAIL, not a retry with more keys. The radio walk presses «down» only while the focused row is not the station's name
- never touches the family PIN, the TV's setup wizard (`setupwraith`) or the old launcher, never changes the Wi-Fi, never restarts the TV, never uninstalls an app
- never presses the mute key: the volume is put back with `cmd media_session volume`, and a mute that is not as it was is reported (a FAIL step) for the person to put back
- puts back at the end, also when it stops half way: the settings (`saver.sh export` before, `import --yes` after), the volume, the display size and density, the font scale

## Tested without a TV

`tests/tester_auto_test.py` feeds `lib.py` saved answers of a TV (meminfo, gfxinfo, logcat, dropbox, dumpsys dreams and audio, uiautomator) and runs the whole `round.sh` against a
stand-in TV (`tests/tester_auto/fake.py`): a clean round, a janky walk, a station that mutes the music, and no app coming to the front (then no D-pad key is sent).
