# Verification evidence

Date: 2026-11-11. Tested in C:\Projetos\Android\JobTrackPro on Windows,
with Android Studio JBR, the configured Gradle Java 25 daemon toolchain, Android SDK 37
and Medium_Phone_API_37.0 (Android 17/API 37).

## Executed checks

| Check | Observed result |
| --- | --- |
| Complete debug build | Passed |
| Release build, unsigned | Passed in the final standard configuration |
| Unit tests | 31 passed, zero failures/errors |
| Room instrumentation | 5 passed, including pending writes after closing/reopening the on-disk database |
| Data/Firebase instrumentation | 4 passed: Authentication, DataStore, two-store sync/deletion and interrupted connection/resumption |
| Compose/XML app instrumentation, emulator configuration | 7 passed; 1 setup-only test skipped by explicit assumption |
| Default unconfigured app instrumentation | 5 passed; 3 emulator-auth flow tests skipped by explicit assumptions |
| Firestore authorization/schema tests | 6 passed, zero failures/skips |
| Android Lint | Passed, zero errors; 11 dependency-update notices in the app report |
| pnpm frozen lockfile installation | Passed |
| Actual Arbeitnow endpoint probe | HTTP success; received documented job fields |
| Screenshot review | Actual dashboard, XML list and landscape form captures inspected |
| Git whitespace check | Passed |

Across both app configurations and the data/database suites, 17 distinct
instrumentation tests passed. Skipped tests were configuration-specific and were
executed successfully in the appropriate opposite configuration. The executed
behavioral checks total 31 unit + 17 instrumentation + 6 rules tests.

The 31 unit tests cover domain validation, typed status/statistics/filter rules,
conflict ordering, failure/cancellation behavior, repository isolation/outbox/cache,
network DTOs with MockWebServer and feature ViewModels. Generated arithmetic tests
were removed; stale old build report files are not counted.

## Reproducible commands

PowerShell setup used in this environment:

```powershell
$env:JAVA_HOME = 'C:\DEV\IDES\AndroidStudio\jbr'
$env:Path = "$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:JAVA_HOME\bin;$env:Path"
```

Successful final emulator build/test/lint invocation:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug :app:connectedDebugAndroidTest :app:assembleDebug -PfirebaseEmulators=true --console=plain
```

Firebase services used the local demo-jobtrackpro project. Device loopback was
forwarded to the host using:

```powershell
adb reverse tcp:9099 tcp:9099
adb reverse tcp:8080 tcp:8080
```

The initial 10.0.2.2 route timed out on API 37. The final configuration uses loopback
forwarding, avoiding dependence on Android 17 LAN permission for developer testing.
Production continues to use Firebase HTTPS endpoints.

Backend instrumentation:

```powershell
.\gradlew.bat :core:data:connectedDebugAndroidTest -PfirebaseEmulators=true
```

Rules tests were run against the existing Firestore emulator:

```powershell
$env:FIRESTORE_EMULATOR_HOST = '127.0.0.1:8080'
node --test tests/firestore.rules.test.cjs
```

Final standard-build verification command:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:connectedDebugAndroidTest :core:database:connectedDebugAndroidTest -PfirebaseEmulators=false --console=plain
```

## Important findings resolved

- Kotlin 2.2.10 rejected Firebase 35.1.0 metadata. Kotlin 2.4.0 compiled successfully.
- Debug/test manifest cleartext policies were configured for local emulator traffic;
  release retains HTTPS-only traffic.
- The interruption test exposed premature local acknowledgement. A server read now
  confirms delivery before clearing the dirty flag; the repeated interruption/resume
  integration test passed.
- Accessibility review replaced separate unlabeled radio targets with labeled
  selectable rows and defined contrasting foreground colors.
- Screenshot capture waits for UI/IME idle, avoiding captures during keyboard animation.
- XML theme follows persisted light/dark preference through AppCompat.
- Search filtering was moved out of JobsScreen into JobsViewModel.
- Backups/device transfer now explicitly exclude local application data.

## Artifacts and report locations

- Standard debug APK: artifacts/JobTrackPro-debug.apk.
- Local Firebase debug APK: artifacts/JobTrackPro-emulator-debug.apk.
- Unsigned release APK: artifacts/JobTrackPro-release-unsigned.apk.
- SHA-256 artifact hashes: artifacts/SHA256SUMS.txt.
- Screenshots: docs/screenshots/.
- Unit HTML reports: each tested module's build/reports/tests/testDebugUnitTest/.
- Instrumentation reports: app, core/data and core/database under build/reports/androidTests/connected/debug/.
- Lint report: app/build/reports/lint-results-debug.html.

Build outputs/logs are ignored by Git. Production Firebase credentials were neither
provided nor created. No real Firebase project deployment or real-device cross-device
validation is claimed. Release signing, GitHub publication and broader assistive-device
validation remain external/manual delivery steps.

Final emulator invocation: BUILD SUCCESSFUL, 653 actionable tasks, 174 executed.
Final standard invocation: BUILD SUCCESSFUL, 815 actionable tasks, 119 executed.
No failing tests were accepted as passed. Earlier test/build failures were corrected
and the affected checks rerun; evidence above describes the successful final runs.

## Follow-up: module string resources

User-facing texts, shared labels, failure messages and validation feedback were moved
to the owning modules' strings.xml files. Domain validation now returns typed invalid
fields; interface tests resolve translated labels through resources. Protocol values,
routes and persistence keys are unchanged. Display dates follow LocalConfiguration's
locale; Lint caught and prompted correction of an initial direct Context configuration read.

Executed after this refactor:

```powershell
.\gradlew.bat :app:assembleDebug testDebugUnitTest lintDebug :app:assembleDebugAndroidTest --console=plain
.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:connectedDebugAndroidTest -PfirebaseEmulators=true --console=plain
```

Both invocations passed. All 31 unit tests passed; app instrumentation passed 7 tests
with the setup-only test intentionally skipped in emulator mode. Lint reported no
errors (11 dependency-update notices). A production-source scan found no remaining
literal Portuguese UI text. All three delivery APKs and their SHA-256 hashes were
refreshed. Backend/rules test results above are from the original delivery validation;
those suites were not rerun for this presentation-resource refactor.

## Follow-up: Kotlin test formatting and names

All 48 Kotlin test methods now use the operationOrBehavior_condition_expectedResult
convention described in TESTING_CONVENTIONS.md. @Test, @Before and @After are on
separate lines above their functions. ktfmt 0.64 formatted test/support files with
four-space Kotlin style; cases and assertions were preserved.

Executed:

```powershell
.\gradlew.bat testDebugUnitTest :app:assembleDebugAndroidTest :core:data:assembleDebugAndroidTest :core:database:assembleDebugAndroidTest --console=plain
.\gradlew.bat :core:model:testDebugUnitTest --console=plain
```

The first invocation passed with 31 successful unit tests and all three
instrumentation APKs built. The model-only suite was rerun after a final test-name
wording adjustment. A source check confirmed 31 unit and 17 instrumentation
annotations, the three-part naming pattern and separate-line @Test placement.
Device instrumentation and Firebase rules tests were not rerun for this
formatting/naming-only change. Production source and application APKs were unchanged.

## Follow-up: supplied Firebase configuration

The supplied google-services.json was found in the project root, while the app's
Google services plugin checks app/google-services.json. The file had one client
matching com.app.jobtrackpro and the required configuration fields. It was copied
to the app module while retaining the original; both locations are ignored by Git.
No configuration values or API keys were added to source control.

Executed successfully:

```powershell
.\gradlew.bat :app:installDebug -PfirebaseEmulators=false --console=plain
```

Google services generated the app ID, API key and project-ID resources. The build
and installation passed (280 actionable tasks, 20 executed). The existing connected
Pixel_9_Pro emulator was launched and its UI hierarchy checked: the setup screen
was absent and the login, email and password-recovery controls were present.
The standard debug delivery APK and checksum were updated.

This validates local SDK configuration and startup. No real account was created,
password reset sent, rules deployed or production synchronization exercised in
this check. Those backend acceptance checks remain pending.
