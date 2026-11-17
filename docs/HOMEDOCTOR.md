# The Home doctor (4.4)

**For the tester and agents, on a computer connected to the TV (`optimize/tv.py` works).** It finds out why UrTV does not stay the TV's main
screen on a given TV: it records the TV's state, tries one way ("angle") at a time, tests it the person's way, puts the TV back, and stops at the
first angle that passes. The winning angle can then become the setup's default for that model and Android version.

```
optimize/venv/bin/python -I optimize/homedoctor.py --dry-run     # the plan, nothing sent
optimize/venv/bin/python -I optimize/homedoctor.py               # every angle in order until one passes (about 3 minutes each)
optimize/venv/bin/python -I optimize/homedoctor.py --angle D     # one angle
    --reboot   the angle that passes is checked again after a restart (through tv.py --reboot)
    --keep     the angle that passed stays (else the TV is put back as it was before the run)
```

**The angles**, in order, each one put back before the next:

| | What it does |
|---|---|
| A | the HOME role to UrTV (`cmd role add-role-holder --user 0 --flags 1`) |
| B | the home activity to UrTV (`cmd package set-home-activity`), the role untouched |
| C | launcherx's claim cleared (its role, its preferred activities), then A |
| D | A + «إضافات تلفزيونك»'s Home watch: turned on and bound, «wanted» set (the home's `HOME_WANTED` receiver action); its `UrTVWatch` lines read |
| E | the TV's own default-home screen is opened; **the tester** chooses UrTV there if it is offered and presses Enter (type what it offered) |
| F | A + «wanted», then a restart: the home gives the role back once at boot (4.4) |

**The test after each angle:** YouTube, then Home, three times; which app is in front 2 s and 6 s after each press; two minutes later the HOME
role and one more Home press. An angle passes when every press lands on UrTV and the one two minutes later too (A, C and F also need the role
to be UrTV's then).

**Never:** a disable of launcherx or setupwraith (the couch-tv lesson: some Google TVs then cannot start), a factory reset, the family PIN. The
script refuses to send such a command.

**What it saves**, under `optimize/run/homedoctor/<time>/` (local, git-ignored): before and after each angle the HOME role holders,
`resolve-activity` HOME, `dumpsys role`, the preferred activities, `dumpsys accessibility` (bound services), the safety switch in
`dumpsys alarm`, the activity in front, `pm list packages -d`, the accessibility list and switch, the log lines of RoleService,
RoleController, ActivityTaskManager's HOME starts, UrTVSetup, UrTVWatch and launcherx, and a screenshot. Then `report.md` and `report.json`:
per angle applied, result and why (with the log lines that explain it), and the winner. No address, serial or name of the TV is written;
send the report only with `optimize/share.sh` (AGENTS.md).

**R3, the Home key catch (4.4; ON by default on a Google TV from build 413):** «إضافات تلفزيونك» catches the remote's Home key itself and opens
UrTV, only while the person's home is ours; Google TV's home then never starts (no flash). The tester's T9 with the owner's real remote: 3 of 3
presses, a long press and Home in a YouTube video caught, the Settings and Assistant keys untouched. The window watch stays as the fallback for
remotes and TVs whose Home never reaches the filter. adb's own key presses never go through it, so only the real remote tests it
(`logcat -s UrTVWatch`: «home key caught ours opened»). The switch: «زر الرئيسية يفتح تلفزيونك مباشرة» in the settings, or
`optimize/dock.sh home-key on|off|status`.
