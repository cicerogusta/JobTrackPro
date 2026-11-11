# JobTrack Pro

A native Android app for tracking job applications and discovering opportunities in Brazil.
Built with Kotlin, Jetpack Compose and XML Views, with user-scoped local storage and
Firebase synchronization.

**Kotlin · Compose + XML · MVVM · Hilt · Room · Retrofit · Firebase**

## Project Overview

JobTrack Pro brings saved vacancies, submitted applications, interviews and offers
into one place. This portfolio project demonstrates a pragmatic multimodule
architecture, observable UI state and interoperability between Compose and Views.

The interface is in Brazilian Portuguese. Code identifiers and the main technical
documentation are in English.

> [!NOTE]
> Product flows are implemented and locally verified. Delivery remains **partially
> completed** pending production Firebase authorization/cross-device validation and
> private release signing. See the [roadmap](docs/ROADMAP.md) and
> [verification log](docs/VERIFICATION.md) for scope and evidence.

[Features](#features) · [Screenshots](#screenshots) · [Setup](#setup-instructions) ·
[Firebase](#firebase-setup) · [Tests](#running-tests) · [Build](#building-apk)

## Features

- **Authentication:** email/password registration, login, password recovery,
  persistent sessions, protected screens and logout.
- **Dashboard:** application totals, interviews, offers and recent applications.
- **Application tracking:** create, edit, inspect and delete applications; search
  by company, role or location; filter by status.
- **Typed statuses:** Saved, Applied, Interview, Offer and Rejected.
- **Brazilian vacancies:** keyword and city/state search, pagination, source links
  and saving vacancies as applications without duplicate imports.
- **Offline data:** Room-backed CRUD for the authenticated account, with durable
  pending changes for synchronization.
- **Cloud synchronization:** Firestore transactions, deterministic conflict handling,
  stable identifiers and deletion tombstones.
- **Preferences:** basic profile, system/light/dark themes persisted with DataStore
  and synchronization feedback.
- **UI feedback:** loading, empty, validation, error and retry states, with shared
  spacing and touch-target tokens.

## Tech Stack

| Area | Technologies |
| --- | --- |
| Language and concurrency | Kotlin, Coroutines, Flow, StateFlow |
| UI | Jetpack Compose, Material 3, XML, ConstraintLayout, RecyclerView, ViewBinding |
| Navigation and lifecycle | Navigation Compose, Fragments, ViewModel, lifecycle-aware collection |
| Dependency injection | Hilt, KSP |
| Local storage | Room, DataStore |
| Networking | Retrofit, OkHttp, Gson |
| Backend | Firebase Authentication, Cloud Firestore |
| Testing | JUnit, MockK, kotlinx-coroutines-test, MockWebServer, Compose Testing, Espresso, Firebase rules tests |
| Build | Gradle Kotlin DSL, Version Catalog, Android Lint |

Versions are defined in [libs.versions.toml](gradle/libs.versions.toml).
The app targets SDK 37 and supports Android 8.0 / API 26 and later.

## Architecture

MVVM with immutable UI state and a unidirectional flow of events:

```text
UI (Compose / XML) → ViewModel → Repository contract
                                      ↓
                            Repository implementation
                              ↙                 ↘
                         Room / DataStore     Retrofit / Firebase
```

Room is the source of truth for application tracking. Repositories coordinate local
and remote sources; synchronization stays outside the presentation layer. Hilt
provides implementations behind cohesive domain contracts. Domain models do not
depend on Android UI resources; visible labels belong to each module's resources.

The application list uses a real Fragment with ConstraintLayout, RecyclerView,
ListAdapter, DiffUtil and ViewBinding, integrated with Compose navigation.

Read the [architecture guide](docs/ARCHITECTURE.md),
[decisions and trade-offs](docs/DECISIONS.md) and
[quality review](docs/QUALITY_REVIEW.md). The
[Portuguese mentoring guide](docs/MENTOR_NOTES.md) explains implementation choices
and interview discussion points.

## Module Structure

| Logical module | Responsibility |
| --- | --- |
| `:app` | Initialization and root navigation |
| `:core:common` | Shared UI state and failure utilities |
| `:core:model` | Domain models, validation and repository contracts |
| `:core:data` | Repositories, preferences and synchronization |
| `:core:database` | Room database, entities, DAOs and local data source |
| `:core:network` | Retrofit service, DTOs and network mapping |
| `:core:designsystem` | Material theme and reusable UI components |
| `:feature:auth` | Login, registration and password recovery |
| `:feature:home` | Compose dashboard |
| `:feature:applications` | XML list, Compose forms and application details |
| `:feature:jobs` | Brazilian vacancy search and imports |
| `:feature:settings` | Profile, theme, synchronization and logout |

`:core:designsystem` lives in `app/designsystem`; `:feature:home` lives in
`app/home`. These physical directories are mapped in
[settings.gradle.kts](settings.gradle.kts).

## Screenshots

Actual captures from the running app on an Android emulator:

| Compose dashboard | Brazilian vacancy search |
| :---: | :---: |
| <img src="docs/screenshots/dashboard.png" alt="Dashboard with application statistics and an add application action" width="280"> | <img src="docs/screenshots/jobs-brazil.png" alt="Brazilian vacancy search with keyword and city filters and a vacancy result" width="280"> |

Additional verification captures:
[XML application list](docs/screenshots/applications-xml.png) ·
[Application form in landscape](docs/screenshots/form-landscape.png).
The application data in those additional captures was created for testing.

## Setup Instructions

1. Clone the repository:

   ```bash
   git clone https://github.com/cicerogusta/JobTrackPro.git
   cd JobTrackPro
   ```

2. Open the root directory in Android Studio and install Android SDK Platform 37.
3. Configure the SDK through Android Studio or an ignored `local.properties` file.
4. Set `JAVA_HOME` to a compatible JDK. The checked-in Gradle daemon criteria select
   Java 25; Java/Kotlin source compatibility targets Java 17. Local builds were
   verified with Android Studio's JBR launcher and the wrapper-managed daemon.
5. Choose a Firebase setup below: your own project or local emulators.
6. Sync Gradle, select an API 26+ device/emulator and run the `app` configuration.

Commands below use Windows PowerShell. On macOS/Linux, use `./gradlew` instead of
`.\gradlew.bat`. Initial dependency/toolchain downloads require internet access.
Node.js and pnpm are only needed for local Firebase services and rules tests.

## Firebase Setup

### Your Firebase project

1. Register an Android app with package name `com.app.jobtrackpro` in Firebase.
2. Download its configuration to `app/google-services.json`.
3. Enable the Email/Password provider in Firebase Authentication.
4. Create a Cloud Firestore database.
5. Review [firestore.rules](firestore.rules) and deploy them to your selected project.
6. Build with `-PfirebaseEmulators=false`, then verify authentication, account
   isolation and synchronization on two devices.

The Google services plugin is applied when `app/google-services.json` exists.
Configuration files, keystores and private keys are ignored by Git. Each developer
must provide their own configuration; backend resources are not provisioned by the build.

**Seeing “Configure o Firebase”?** Place the JSON inside the `app` module, rebuild
and reinstall. A file only in the repository root, or an APK built before adding
it, does not configure the installed app. Builds without configuration show the
setup screen.

### Local Firebase emulators

To exercise authentication and synchronization without a production project:

```powershell
pnpm install --frozen-lockfile
pnpm emulators
```

With an Android device connected, use another terminal:

```powershell
adb reverse tcp:9099 tcp:9099
adb reverse tcp:8080 tcp:8080
.\gradlew.bat :app:installDebug -PfirebaseEmulators=true
```

This debug configuration uses `demo-jobtrackpro`, Authentication on port 9099 and
Firestore on port 8080. Repeat ADB forwarding after reconnecting the device.
Release builds always use normal Firebase configuration.

## API Configuration

The app integrates the [okemprego API](https://www.okemprego.com/pt/api) through
Retrofit. No API key is configured by the application.

| Request setting | App behavior |
| --- | --- |
| Endpoint | `https://www.okemprego.com/api/v1/jobs` |
| Country | `country=BR` on every request; mapper also rejects other countries |
| Search | `q` for keywords and `where` for city/state, submitted with the search button |
| Pagination | 20 results per page, newest first, up to 200 pages per filter set |
| Cache | Successful pages retained for 15 minutes; at most 20 pages in memory |
| Failures | Connectivity/HTTP handling, retry and a specific HTTP 429 message |

Provider IDs are deduplicated. Remote, hybrid and on-site arrangements are preserved;
when the source omits the arrangement, users select it before saving. Descriptions
are displayed as plain text, and every result exposes its source link.

Availability and usage terms depend on the provider. See the
[API decision record](docs/DECISIONS.md) for integration details and constraints.

## Running Tests

### Unit tests and static analysis

```powershell
.\gradlew.bat testDebugUnitTest lintDebug -PfirebaseEmulators=false
```

Unit tests use local fakes/mocks and do not require a live API or Firebase backend.
Follow the [test naming and formatting conventions](docs/TESTING_CONVENTIONS.md).

### Android instrumentation

Start the local Firebase services and connect an Android emulator/device first:

```powershell
adb reverse tcp:9099 tcp:9099
adb reverse tcp:8080 tcp:8080
.\gradlew.bat :core:database:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest :app:connectedDebugAndroidTest -PfirebaseEmulators=true
```

Authentication/synchronization tests require local services and the debug emulator
property; they are skipped when that property is disabled.

### Firestore rules

With existing Firebase emulators stopped:

```powershell
pnpm install --frozen-lockfile
pnpm test:rules
```

Against an already-running Firestore emulator instead:

```powershell
$env:FIRESTORE_EMULATOR_HOST = '127.0.0.1:8080'
node --test tests/firestore.rules.test.cjs
```

**Latest recorded validation (November 11, 2026):** 47 unit tests and 7 focused
Compose UI tests passed, including button spacing and enlarged-font reachability.
Debug/release builds and Lint passed. Earlier Room, Firebase integration, XML UI and
6 Firestore rules tests are recorded separately in the
[verification log](docs/VERIFICATION.md). These results describe local checks; production acceptance remains pending.

## Building APK

```powershell
# Standard debug build (requires Firebase configuration for authenticated flows)
.\gradlew.bat :app:assembleDebug -PfirebaseEmulators=false
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Debug build targeting local Firebase emulators
.\gradlew.bat :app:assembleDebug -PfirebaseEmulators=true

# Unsigned release build
.\gradlew.bat :app:assembleRelease
```

| Build | Output |
| --- | --- |
| Debug | `app/build/outputs/apk/debug/app-debug.apk` |
| Unsigned release | `app/build/outputs/apk/release/app-release-unsigned.apk` |

Debug builds use the Android debug key. Release distribution requires your own
signing configuration; keep keystores and passwords outside version control.
Local `artifacts/` delivery copies are ignored and are not included in a clone.

## Known Limitations

- Production Firebase rules deployment, password-reset delivery, cross-account
  authorization and cross-device behavior still need external acceptance checks.
- Synchronization runs while the authenticated app is in the foreground or through
  manual retry; background WorkManager delivery is not implemented.
- Conflict resolution uses device timestamps with UUID tie-breaking. Incorrect
  clocks can affect ordering; deletion tombstones are retained indefinitely.
- Synchronization reads the full account collection, suited to portfolio-scale data.
- Vacancy coverage depends on the provider catalog. Live pagination can shift as
  postings change; duplicate IDs are filtered.
- Room records are scoped by UID without additional database encryption. Logout
  retains local records and pending changes on the device.
- Broader TalkBack, Switch Access, OEM, tablet and release-distribution validation
  remains pending. The current UI language is Brazilian Portuguese.

## Future Improvements

- Incremental synchronization and WorkManager background delivery.
- Server-authoritative conflict timestamps and retention controls.
- Additional vacancy sources and localization.
- Date picker and account deletion flows.
- Expanded device coverage, continuous integration and signed release distribution.

Planned and completed stages are tracked in the [roadmap](docs/ROADMAP.md).
