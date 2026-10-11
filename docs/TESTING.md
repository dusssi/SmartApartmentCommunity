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

## Verification performed for the Phase 2 UI change

Recorded for the Phase 2 UI change on branch `arena/f9d05daf-smartapartmentcommunity`. Only the results below were actually obtained.

| Check | Command / method | Result |
|---|---|---|
| Java syntax | `javalang` 0.13 parse of every file under `app/src/main/java` (`python3` one-liner, see below) | **PASSED** (all files parse). `javalang` checks syntax only; it does not resolve types or symbols. |
| Resource references and XML well-formedness | `python3 scripts/verify_resources.py` | **PASSED**: all XML parses; every `@type/name` in XML and every `R.type.name` in Java resolves to a resource in `res/`. The script was also checked against a copy with deliberately broken references and reported them. |
| Gradle debug build (`./gradlew assembleDebug`) | Attempted in the Arena sandbox | **BLOCKED.** The sandbox has no JDK or Gradle, and it cannot reach `services.gradle.org`, Google Maven (`dl.google.com`) or the Android SDK. The Debian apt mirrors were also unreachable. **Java compilation, resource linking and APK packaging have not been verified.** |
| Unit tests | None configured | Not applicable. |
| Emulator or Galaxy M33 install and UI run | Not possible in this sandbox | **NOT RUN.** No claim of device testing is made. |
| Manual checklist below | Not executed | **NOT RUN.** |
| Accessibility scan (Accessibility Scanner / TalkBack) | Not possible here | **NOT RUN.** Contrast ratios were calculated from hex values (see `DESIGN_SYSTEM.md`). |
| Animation frame timing | Not possible here | **NOT RUN.** |

Syntax check command used:

```bash
pip install --break-system-packages javalang
python3 - <<'PY'
import glob, javalang
for f in glob.glob('app/src/main/java/**/*.java', recursive=True):
    javalang.parse.parse(open(f).read())
print('java files ok')
PY
```

Resource check command:

```bash
python3 scripts/verify_resources.py
```

### Required before merge

1. Run `./gradlew assembleDebug` on a machine with JDK 17 and the Android SDK (Platform 35, Build Tools for AGP 8.6.1). Resolve any compile or `aapt2` errors. The unverified code paths are the `TextInputLayout` setter calls (`setPlaceholderText`, `setBoxStrokeColorStateList`, `setErrorTextColor`), `WindowInsetsCompat.isVisible(ime())`, and the `DiffUtil` adapter.
2. Install on a Galaxy M33 (API 34/35) and on one narrow and one large emulator. Check:
   - Keyboard on login, registration, forgot-password and every form: the focused field and submit button stay visible; the bottom navigation hides and returns on dismiss.
   - Edge-to-edge: the top bar clears the status bar, and the bottom navigation sits above gesture/3-button navigation with no extra blank band.
   - Animation disabled (Developer options → Window/Transition/Animator duration scale = off): screens appear without motion and taps still work.
   - Search typing: only changed rows animate, and scroll position is kept.
   - Status change (complaint, request, visitor, RSVP, read toggle): content does not replay the entrance animation.
   - Largest font size (Settings → Display → Font size): titles and chips wrap without clipping.
3. Run TalkBack over the login and role-picker screens.


Known project limitation: there is still no automated Android instrumentation or UI test suite.
