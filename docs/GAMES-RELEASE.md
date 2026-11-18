# The games' remote release switch (4.4)

The games of the engine (`docs/GAME-ENGINE.md`) are played from data and appear on a TV only when a small file on urtv-data says so, so a game can be turned on, and off again, without an app update. Until then the games hub is exactly what it was: no tile, no «قريبًا», no count, no locked tile, no changelog line, nothing in the settings.

## The file
`release/games.json` on the branch `data` of **tshi-dev/urtv-data** (raw.githubusercontent.com, the same address form as the other data: `DataHost.NEW + "release/games.json"`). It is hand-edited as `content/games-release.json` on main there and published by content.yml after `tools/check_content.py`; `tools/games_release.py` makes the edits.

    {"v": 1,
     "games": {"<id>": {"a": "2026-12-01T00:00:00Z", "b": "now"}},
     "defs":  {"<id>": {the game}},
     "bdefs": {"<id>": "<the game sealed with the group key, hex>"},
     "codes": ["2:<rounds>:<salt hex>:<check hex>:<wrapped group key hex>"]}

- `games` lists the games and when they are on. `a` is the time from which a game is on **for everyone**, `b` from which it is on **for group B**. A time is `"now"` or a UTC time `"yyyy-MM-ddTHH:mm:ssZ"`. **Anything else is off**: `false`, `null`, a missing key, a missing game. Group B has a game from its `b` time **or** from the `a` time. To turn a game off again set both to `false` or remove it.
- `defs` are the games' definitions for everyone, `bdefs` the ones for group B only (below). A game with neither, or whose definition the engine cannot run, is not on.
- **No file, a broken file, a file without `"v": 1` and a first start before any answer: nothing is on** (R11's spirit: never turn a thing on by accident). Offline, the last good copy is used.
- The file is read like the other hand-edited files (`ContentFile`): the kept copy at once from memory, fetched again in the background at most every 3 hours, a bad answer never replaces a good one, at most 64 KB (a bigger game is a pack). The hub asks only memory on the UI thread (R1: `GameReleaseStore.visible`); the reading, the fetch, the packs and the code check run on their own threads, and the hub draws again when what is on has changed (the selected game stays selected).

## The app's own games held back
Some games of the app's own list (`GameRegistry.HELD`) are held back the same way: the hub shows one only when `games` of this file has it as `{"native": true, "a": ..., "b": ...}` and turns it on for the TV's group (`a` for everyone, `b` for group B, `false` or a missing entry is off, a revoked pass hides it again). They take no entry in `defs` or `bdefs` (one given is ignored). Without `"native": true` an entry for such an id does nothing. The id is in the clear in `games` for a TV of either group (only a definition can be sealed; a native game has none, so there is nothing to seal): the file does not hide that a game exists, only the app does. Their activities are not exported (an intent from outside cannot open one) and the phones' page has no list of games. The games shown from the start and «حزم المحتوى» are never held, whatever the file says. No file, a broken file or no answer yet: they are hidden. The hub asks memory only (`GameReleaseStore.nativeOn`) and draws again when the answer changes.

## Group B: the owner's pass
Two groups and nothing says so anywhere: **A** is everyone, **B** holds a pass.
- **Getting the pass:** Settings, the section «حول», **OK seven times in a row on the version lines** (each within four seconds of the last): a box for a code opens. A right code puts the TV in group B and says «تم»; a wrong one leaves nothing behind and says nothing special. Five tries a minute at most. A code is **exactly 6 signs**: A to Z without I, L and O, and the digits 2 to 9 (31 signs, nothing that can be read as another); case, spaces and Arabic-Indic digits do not matter. The box is six square boxes under «اكتب الرمز»; the check starts by itself at the sixth sign.
- **What the file holds for a code** is made from the code with **one run of PBKDF2-HMAC-SHA256** (300000 rounds by default, 100000 to 1000000 allowed, a **random salt for each record**) and never the code. Two things are named from that one result by HMAC: a **check** (is this code in the record) and a **wrap key** (a different name, so the public check is never the key), and with the wrap key the **group key** (32 random bytes, the same for every code of the file) is sealed with AES-256-GCM. A definition for group B only is sealed with the group key (`bdefs`); a TV reads it only after a code of the file gave it the key. Comparison is in constant time; a wrong code gives nothing and no error. A TV pays the slow part **once for each record** it tries, off the UI thread, so a file holds **30 records at most**.
- **How strong this is, honestly.** A 6-sign code has 31^6, about 887 million, possibilities (under 30 bits). The file is public, so anyone can try codes offline against it; the rounds only make each try cost 300000 hashes (about 70 ms on a laptop core, more on a TV; the rounds were chosen by arithmetic, not measured on a weak TV: measure there). A person with a GPU can still try every code of one record in days to weeks, and a code found opens the group key, so **everything sealed for group B is readable by whoever finds one code**. Treat group B content as protected from casual readers, not from someone who wants it; keep the really private games out of the public file altogether.
- **What the TV keeps:** the record and the group key (a private preference file), never the code.
- **Revoking:** remove the record from `codes`. At the next read the TV finds its record is no longer listed, forgets it and the key, and is group A again. To make what a revoked TV already read useless for **later** files, seal them again with a new group key (the tool's `rekey`).
- The file is public (a raw address): the hashes are slow and salted, so a long random code cannot be guessed from them. 

## Privacy
The TV only **downloads** one file. No account, no device id, no query, no header and no upload of any kind: the request looks the same from every TV. The group is decided on the TV.

## Tools (urtv-data, `tools/games_release.py`)
    code [--add] [--rounds N]   makes a random 6-sign code, prints it ONCE with its record (never stores the code); --add puts the record in the file
    code --ask            the same for a code you type (hidden)
    on ID --a WHEN --b WHEN       WHEN is now, a UTC time, or off
    off ID                off for both groups
    revoke HASH           removes a record
    check [--at TIME]     what is on for each group at a time
    seal ID FILE          seals a game's JSON with the group key into «bdefs» (needs the key: from the private notes, never printed)
    rekey                 a new group key: every record is wrapped again for the codes you give, every sealed game is sealed again

## What the tests pin (`tests/game_engine_test.py`)
Hidden by default (no file, an empty file, a broken file); the `a` / `b` timing to the second; turning a game off again; the pass (the right code in any spelling, wrong, short, null); group B's decryption and a wrong code that is silent; revoking; the offline copy; the hub unchanged with everything off; R1 for the read and the fetch; no request but the one file.
