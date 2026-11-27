# urtv-data `content/`: a fact review (27 Nov 2026)

What `tshi-dev/urtv-data` `content/` says today (main at `1ec236e`), checked against public sources. Nothing in urtv-data was changed.

| File | What it holds | Verdict |
|---|---|---|
| `seasonal.json` | `"packs": []` | empty, nothing to check |
| `announcements.json` | `"notices": []` | empty, nothing to check |
| `prayer-adjustments.json` | `"adjustments": []` | empty, nothing to check |
| `sa-school.json` | the 1448 (2026-27) school breaks | correct, with two notes below |
| `devices.json` | one TV rule (TCL G07, Android 11: the sound screen) | from our own tester's report; no public source exists for it |
| `games-release.json` | which games are released, and hashed codes | not facts about the world; not reviewed |

## `sa-school.json`, break by break

| Break in the file | Sources | Verdict |
|---|---|---|
| إجازة الخريف 2026-11-20 → 11-28 | [ajel](https://ajel.sa/guide/school-calendar/holidays), [Arabian Business](https://www.arabianbusiness.com/business/education/saudi-school-calendar-2026-27), [rcjy.edu.sa](https://rcjy.edu.sa/ar/general-education-calendar/) | matches; see note 1 (an extra day in most regions) |
| منتصف العام 2027-01-08 → 01-16 | [Arabian Business](https://www.arabianbusiness.com/business/education/saudi-school-calendar-2026-27), [rcjy.edu.sa](https://rcjy.edu.sa/ar/general-education-calendar/), [Education Saudi](https://www.education-saudi.com/saudi-school-calendar-2026-2027/) (term 2 starts 17 Jan) | matches. [Time Out Jeddah](https://www.timeoutjeddah.com/news/saudi-academic-calendar-2026-27) and [Destination KSA](https://destinationksa.com/en/saudi-academic-calendar-2026-2027/) list a 20 Dec → 9 Jan winter break instead; it looks like the older three-term calendar, and the Royal Commission's education page agrees with the file |
| يوم التأسيس 2027-02-19 → 02-22 | [ajel](https://ajel.sa/guide/school-calendar/holidays), [Arabian Business](https://www.arabianbusiness.com/business/education/saudi-school-calendar-2026-27) | matches (Time Out Jeddah lists 22 Feb alone) |
| عيد الفطر 2027-02-26 → 03-13 | [ajel](https://ajel.sa/guide/school-calendar/holidays), [Arabian Business](https://www.arabianbusiness.com/business/education/saudi-school-calendar-2026-27) | matches most sources; Time Out Jeddah gives 7 → 13 Mar. The file's note already says an Eid can move by a day |
| عيد الأضحى 2027-05-07 → 05-22 | [ajel](https://ajel.sa/guide/school-calendar/holidays), [Arabian Business](https://www.arabianbusiness.com/business/education/saudi-school-calendar-2026-27) | matches most sources; Time Out Jeddah gives 16 → 22 May |
| نهاية العام 2027-06-25 → 08-21 | the last school day is Thursday 24 June 2027, and students return on Sunday 22 August 2027 ([whichschooladvisor](https://whichschooladvisor.com/saudi/school-news/new-saudi-school-calendar-holidays-for-2026-2027), [What's On Saudi Arabia](https://whatsonsaudiarabia.com/2026/08/key-dates-on-the-saudi-academic-calendar-2026-2027/)) | matches |

**Note 1, the extra days (worth an owner decision).** The file leaves out regional extra days on purpose (its `note`). Sabq reports
these extra days for Riyadh, Qassim and most regions: Sunday 25 Oct 2026, Sunday 29 Nov 2026, Thursday 7 Jan 2027 and Sunday
11 Apr 2027 ([sabq](https://sabq.org/article/9lewsyu), [sabq calendar](https://sabq.org/mawaeed/school-calendar-1448)). The 29 Nov
day joins the autumn break, so in most regions school resumes on Monday 30 Nov, not on Sunday 29 Nov as a TV reading this file would
say. ajel notes that each region's education office announces its own extra days. Options:
- leave the file as it is (it is right for the national calendar); or
- add the extra days as their own entries with a `regions` field once each office announces them (the app would need to read that field).

**Note 2, the source URL.** `source` is the Ministry's calendar page (`moe.gov.sa/ar/education/academic-calendar`). Several of the dates
above were confirmed only through press reports and the Royal Commission's education page, not on the Ministry page itself.

**Already past, not in the file:** the National Day break (23 → 26 Sep 2026). That is right, since the file holds only breaks still to
come.

## `devices.json`

One rule: a TCL TV whose `device` is `G07` on Android 11 opens the general settings for «مخرج الصوت», because that build's settings app
has no sound screen and crashes. This comes from our own tester's report (ControlCenter.java), and no public document covers it. It is
narrow (brand, device and SDK must all match), so a wrong match is unlikely. No change suggested.

## Summary

No wrong fact found. The empty files have nothing to check. The school calendar matches the national 1448 dates. The one gap is the
regional extra day after the autumn break (29 Nov), which is a design choice the file states.
