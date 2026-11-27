# Automatic round: 4.4b4-654-r5

## Health metrics

| metric | now | limit | previous | change | verdict |
|---|---|---|---|---|---|
| cold_start_home_ms | 0ms | max 4000 |  |  | ok |
| cold_start_saver_ms | 919ms | max 5000 |  |  | ok |
| jank_home_frames | 450 |  |  |  | no limit |
| jank_home_pct | 25.11% | max 15 |  |  | RED |
| jank_home_p90_ms | 65ms |  |  |  | no limit |
| jank_home_p99_ms | 73 | max 120 |  |  | ok |
|  | ms |  |  |  | no limit |
| pss_home_kb | 94494KB | max 300000 |  |  | ok |
| pss_saver_kb | 27684KB | max 300000 |  |  | ok |
| crashes | 0 | zero 0 |  |  | ok |
| anrs | 0 | zero 0 |  |  | ok |
| frozen_lines | 0 | zero 0 |  |  | ok |

## Checks

| step | result | what |
|---|---|---|
| radio-0 | FAIL | : not playing after 30 s (see raw/radio-0.log) |
| radio-1 | FAIL | : not playing after 30 s (see raw/radio-1.log) |
| radio-2 | FAIL | : not playing after 30 s (see raw/radio-2.log) |
| radio-3 | FAIL | : not playing after 30 s (see raw/radio-3.log) |
| radio-4 | FAIL | : not playing after 30 s (see raw/radio-4.log) |
| radio-5 | FAIL | : not playing after 30 s (see raw/radio-5.log) |
| radio-6 | FAIL | : not playing after 30 s (see raw/radio-6.log) |
| radio-7 | FAIL | : not playing after 30 s (see raw/radio-7.log) |
| radio-8 | FAIL | : not playing after 30 s (see raw/radio-8.log) |
| radio-9 | FAIL | : not playing after 30 s (see raw/radio-9.log) |
| radio-10 | FAIL | : not playing after 30 s (see raw/radio-10.log) |
| radio-11 | FAIL | : not playing after 30 s (see raw/radio-11.log) |
| radio-12 | FAIL | : not playing after 30 s (see raw/radio-12.log) |

## Verdict

RED: jank_home_pct = 25.11% (limit max 15); step radio-0 failed; step radio-1 failed; step radio-2 failed; step radio-3 failed; step radio-4 failed; step radio-5 failed; step radio-6 failed; step radio-7 failed; step radio-8 failed; step radio-9 failed; step radio-10 failed; step radio-11 failed; step radio-12 failed
