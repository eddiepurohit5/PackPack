# Verification

- Debug APK: built successfully.
- Packing rules: 9 unit tests passed.
- Android lint: passed with no errors; remaining warnings concern localisation, manifest metadata, and available tool updates.
- Commit helper: shell syntax and dry run passed. An isolated test repository verified explicit path staging and duplicate-day rejection. The fixture uses a stub build command; the actual Android build and tests were run separately.
- Not yet verified interactively: device/emulator screens, Android image picker, hotel suggestions, rotation, and restart persistence. These are included in the 14-day manual testing plan.

Use synthetic boarding passes for demonstrations and screenshots.
