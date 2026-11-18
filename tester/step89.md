# step 89 — full 4.4 audit (partial, part 1: speed/stability numbers)
TV: TCL G08, Android 14, 4.4 build 484, muted. Wi-Fi: connected.

| item | result |
|---|---|
| home cold start (3x, force-stop then am start -W) | WaitTime 22 / 12 / 14 ms (TotalTime 0: process was already warm from the launcher) |
| home memory | TOTAL PSS 113 MB (Java 10 MB, native 38 MB) idle |
| CPU idle on home | top: our app not in the top 6; the TCL system service com.tcl.ttvs uses ~94% of one core by itself |
| crash buffer | only an old init SIGABRT from boot (not ours) |
| ANR | no new files; /data/anr holds only June files |

Findings so far: none blocking. Observation (not ours): com.tcl.ttvs is busy at idle.
Still to do (need long runs or the remote): meminfo after 30 min, offline test, UX walk, settings tree.
