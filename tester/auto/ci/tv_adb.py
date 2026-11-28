#!/usr/bin/env python3
"""A stand-in for optimize/tv.py on the CI emulator (tester/auto/ci/run.sh copies it over optimize/tv.py in the CI checkout, which is thrown away): the same
words the scripts use, sent with the `adb` binary of the runner instead of the TV's Wi-Fi client. Only what saver.sh, music.sh, start-saver.sh, settings_move.py and
round.sh ask of it:
    tv.py 'cmd'              one shell command; its output, and its exit code
    tv.py --batch < cmds     one command per line, each printed as «$ cmd» and its output (as tv.py does)
    tv.py --shot out.png     the screen as a PNG
    tv.py --transport        «adb»
    tv.py --pull a b         adb pull
    tv.py --probe            a few getprops
Never used on a person's TV: it is not in optimize/ in the repository.
"""
import subprocess
import sys

ADB = "adb"


HOME_START = "am start -a android.intent.action.MAIN -c android.intent.category.HOME -n sa.urtv.home/.MainActivity"


def rewrite(cmd):
    """The Home key on the emulator brings the emulator's own launcher when it could not be told to prefer ours (run 2 of the round: tvlauncher in front at every
    layout step): there the key is sent (it wakes a dream, as on a TV: the emulator's own backdrop dream stayed in front through run 4) and then as the Home intent to our home, which is what the key does on a TV whose Home it is."""
    return ("input keyevent KEYCODE_HOME; " + HOME_START) if cmd.strip() in ("input keyevent KEYCODE_HOME", "input keyevent 3") else cmd


def shell(cmd):
    cmd = rewrite(cmd)
    r = subprocess.run([ADB, "shell", cmd], capture_output=True, text=True, errors="replace")
    return r.returncode, (r.stdout or "") + (r.stderr or "")


def main(a):
    if not a or a == ["--probe"]:
        for p in ("ro.product.model", "ro.build.version.release", "ro.build.version.sdk"):
            print(f"{p} = {shell('getprop ' + p)[1].strip()}")
        return 0
    if a == ["--transport"]:
        print("adb")
        return 0
    if a[0] == "--batch":
        code = 0
        for line in (l.strip() for l in sys.stdin):
            if not line or line.startswith("#"):
                continue
            c, out = shell(line)
            code = code or c
            print(f"$ {line}\n{out.rstrip()}")
        return 0
    if a[0] == "--shot":
        r = subprocess.run([ADB, "exec-out", "screencap", "-p"], capture_output=True)
        with open(a[1], "wb") as f:
            f.write(r.stdout)
        print(f"saved {a[1]}")
        return 0 if r.stdout else 1
    if a[0] == "--pull":
        return subprocess.run([ADB, "pull", a[1], a[2]]).returncode
    if a[0].startswith("--"):
        sys.stderr.write(f"tv_adb.py: {a[0]} is not offered on the emulator\n")
        return 2
    c, out = shell(" ".join(a))
    sys.stdout.write(out if out.endswith("\n") or not out else out + "\n")
    return c


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
