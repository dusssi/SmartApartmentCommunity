# Build and testing

## Build commands

Prerequisites are JDK 17 and Android SDK Platform/Build Tools compatible with SDK 35. From the repository root:

```bash
./gradlew --version
./gradlew assembleDebug
```

Expected APK: `app/build/outputs/apk/debug/app-debug.apk`.

Install to a connected device/emulator:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` may be used for a local SDK path but is ignored by Git. Android Studio can also sync and run the `app` module.

## Manual checklist

Run on at least one API 24+ emulator/device and preferably a narrow and a larger screen:

- [ ] Launch into entry screen; open direct demo roles.
- [ ] Login rejects malformed email and short password; valid demo credentials reach role previews.
- [ ] Registration rejects missing/invalid fields; valid demo data reaches role previews.
- [ ] Recovery rejects malformed email and clearly says no email is sent.
- [ ] Select each role and verify its label, navigation destinations and available actions.
- [ ] Dashboard quick actions and bottom navigation open the expected module; test system back.
- [ ] Search each list, apply and clear filters, and verify empty-result messages.
- [ ] Open an announcement and verify read state; toggle it back to unread.
- [ ] Submit valid/invalid complaint, request and visitor forms; verify records appear in their lists.
- [ ] In Maintenance/Management, advance a complaint status; in Management, advance a request status.
- [ ] In Security/Management, progress a visitor through approval/check-in/check-out.
- [ ] RSVP/cancel an upcoming event and verify the updated state.
- [ ] Use the emergency action and confirm it opens the dialer rather than placing a call; sample contacts stay disabled.
- [ ] Open a notification and related module; verify there is no claim of push delivery.
- [ ] Edit profile, toggle settings, and test logout confirmation.
- [ ] Verify document details do not claim a file was opened/downloaded.
- [ ] Check scrolling/keyboard visibility, touch targets, status labels and no clipping on different screen sizes.

## Verification performed in this checkout

- Java source syntax parsed with `javalang`: **passed** for all Java files.
- XML well-formedness and local resource-reference checks: **passed** for the current manifest/resources.
- Gradle build/APK packaging: **blocked before Gradle startup**. `./gradlew assembleDebug` was attempted, but this execution environment has no `java`/`javac` executable; no Android SDK is installed here either. The wrapper's executable bit is now set. Compilation and APK generation must be verified in a JDK 17 + Android SDK environment.
- Emulator/physical-device installation and UI workflow tests: **not run**.

Do not report build, installation, or device tests as passing until they are actually run. Known project limitation: there is not yet an automated Android instrumentation/UI test suite.
