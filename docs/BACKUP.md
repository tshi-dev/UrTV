# «نسخة الإعدادات»: the settings backup on the TV (4.5, design)

The owner's direction is no computer needed: a family saves its settings to a USB drive (or Downloads) from the TV and restores them after a
factory reset, or on a second TV. Mockup: docs/mockups/tools-45/04-backup.png. The computer's `optimize/saver.sh export|import` (schema 1)
stays as it is and also reads this file.

## The file (schema 2)

```json
{"schema": 2, "app": "urtv", "made": "2026-11-17", "version": "4.5 (build N)",
 "saver": { the schema-1 keys of optimize/settings_move.py: city, digits, prayer, cards, markets, teams, adhan, … },
 "home":  { "dock": ["pkg", …], "dock_exact": false, "hidden": ["pkg", …], "app_names": {"pkg": "اسم"},
            "input_names": {"id": "PS5"}, "input_hidden": ["id", …], "keys": {"youtube": "pkg", "youtube.2": "@switcher"} } }
```

- One allow-list per part; anything else in a file is ignored and said. Never in it: the parental PIN, Wi-Fi, ADB keys, journals, the tasks,
  the person's photos and own adhan recording (the same rules as schema 1).
- "saver" uses exactly the schema-1 keys, so `settings_move.py` reads a schema-2 file's "saver" part as a schema-1 file
  (tests/settings_schema2_test.py) and leaves "home" to the TV.
- A newer schema is refused; an older one is migrated (schema 1 → 2 is "saver" = the file, "home" empty).

## The link between the apps

The home writes and reads the file (it holds the screens and the USB). The screensaver's settings live in the screensaver app, so the home
asks it through its ConfigProvider, with a signature permission only our two apps hold (PHONE_SETTINGS, already used by the phone control):

| call | does |
|---|---|
| `backup_export` | the "saver" part as JSON text, built from the same allow-list (SettingsFile.java in the screensaver app) |
| `backup_check` (arg: the "saver" JSON) | validates all of it, changes nothing; returns the old → new lines for the preview, or the refusal |
| `backup_apply` (arg: the "saver" JSON) | validates again, keeps the current values, applies all through `SaverReceiver.set` (its own checks); any refusal puts every value back; returns ok or the reason |

The home's own part is applied the same way inside the home (the current values kept first). Order: check both parts, then the screensaver's
apply, then the home's; if the home's fails the screensaver's is put back with the kept values. Before applying, the current settings are
written to `urtv-settings-before-restore.json` beside the file, so a restore can be undone by restoring that file.

## Where the file goes

- A USB drive: the app's own folder on the removable volume (`getExternalFilesDirs`, no storage permission; Android 4.4+). The file is
  `Android/data/sa.urtv.home/files/urtv-settings-<date>.json` on the drive.
- Else Downloads: `MediaStore.Downloads` on Android 10+, the public Download folder before (WRITE_EXTERNAL_STORAGE only up to Android 9).
- Restore lists the files it can read in those places, newest first.

## Tests (each part on a plain JVM where it has no Android)

- the file's allow-lists, validation, migration and diff (BackupFile.java, pure);
- the screensaver's export/check/apply on a fake prefs store, all or nothing (SettingsFile.java, pure core);
- the provider calls need PHONE_SETTINGS (read from the source);
- settings_move.py reads schema 2's "saver" part (tests/settings_schema2_test.py).
