# Contributing

Thank you. Small, safe changes are the best ones: a fix, a test, a clearer line of text.

## How

1. Fork the repository and make a branch.
2. Open the pull request against **`next`**, never against the release branch.
3. Run the tests before you push (about 15 minutes):

       export CHROMIUM=/opt/pw-browsers/chromium
       bash tests/run.sh

   While you work, run just the test of the file you touched, for example `python3 tests/share_test.py`.

## Rules

- Read [AGENTS.md](AGENTS.md); it applies to people and to AI agents alike. In short: never build or install your own APK,
  never turn auto-update off, never skip or disable a test to make it pass, never override a `!!` guard.
- Never `git add` the font files (`assets/fonts/`): they come from a private repository in CI.
- No personal data anywhere (code, docs, tests, reports): no IP address, serial, Wi-Fi name, user name or file path.
  Tests use obvious placeholders (`2001:db8::`, `192.0.2.x`, `Example Name`).
- Defaults stay as they are unless it is a bug. Nothing that can break a setup in progress or a TV already set up.
- A problem with someone's TV is reported through the form, not as an issue: see [SECURITY.md](SECURITY.md).

## Arabic text

The screens and docs are in plain Saudi-leaning Arabic, short and warm, the way [docs/FEATURES.md](docs/FEATURES.md) is written:
what it is, where to change it, what to tell the agent. Keep the same voice. Religious texts (verses, hadith, adhkar) are never
typed by hand; they come from the checked sources described in [notes/CONTENT-REVIEW.md](notes/CONTENT-REVIEW.md).
