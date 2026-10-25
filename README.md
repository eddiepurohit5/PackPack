# Pack Pack
A small offline travel packing planner for a two-person BTech Android project. Java + XML, built-in SQLite, no account or backend.

## Run
Open this folder in Android Studio, let Gradle sync complete, select an Android 8.0+ emulator/phone, and click Run. The folder remains `PackProof` so an existing Android Studio checkout stays valid; the app and Gradle project are named **Pack Pack**.

Build tools: compile/target SDK 37, Android Gradle Plugin 9.3.2, Gradle 9.5.0, Java 17 source compatibility. Use Android Studio's bundled Gradle JDK. On another machine, let Android Studio regenerate the ignored local.properties with that computer's SDK path.

## Included
- Destination and dates, 1–12 travellers, leisure/beach/business/adventure.
- Flight/train/bus/car, manual weather, international travel, laundry access.
- Hotel/hostel/apartment/camping/other; manually enter a hotel or select a previously saved name. No live hotel search or booking.
- Indicate whether the stay supplies toiletries and towels.
- Packing quantities for the whole group, shared items, transport and stay-specific additions, explanations for every suggestion.
- Editable items, custom additions, packing progress, saved trips, trip deletion.
- Optional boarding-pass **image attachment** from the Android document picker; thumbnail, enlarge, replace, remove. Local URI access is retained; nothing uploads to a server. If the source image is deleted or access is revoked, reattach it. No OCR or PDF import.
- Rotation restores the in-progress form and the open trip.

## Structure
- MainActivity.java: screens, validation, attachment picker, image preview.
- PackingRules.java: deterministic packing rules and group quantities.
- TripStore.java: background SQLite storage of JSON trip records.
- activity_main.xml: scrolling root; reusable form controls/cards are built in Java.
- PackingRulesTest.java: rule tests.

Package ID remains com.packproof.app so existing locally saved trips are preserved. Older trips use one traveller and reasonable display defaults; new trip fields apply to new trips. Trip details cannot yet be changed after list generation; individual items can be edited.

## Test and build
```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
```
The debug APK is produced at app/build/outputs/apk/debug/app-debug.apk. A local development keystore may be used if present; otherwise Android uses its normal debug signing. No private signing keys belong on GitHub.

## Two-week GitHub workflow
See [14-day plan](docs/14-DAY-PLAN.md). The full implementation is the day-1 baseline; following commits capture real testing, documentation and refinements. Use `scripts/daily-commit.sh` after completing each day's work. It commits locally and does not push or fake dates.

## Demo
Create a five-day beach trip for three people, choose Flight and a hotel with towels, attach a synthetic boarding pass, then generate the list. Compare group clothing quantities with shared sunscreen, read an explanation, add an item, mark items packed, and reopen the trip.

## Scope
No real hotel inventory, bookings, live weather, reminders, AI, cloud sync, or document extraction. Suggestions are editable starting points. Weather and hotel amenities are supplied by the user. This intentionally stays small enough to understand and refine over two weeks.
