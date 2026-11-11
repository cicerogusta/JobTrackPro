# Engineering decisions

## Existing Android toolchain

Preserved AGP 9.5.0, Gradle 9.6.0 and SDK 37 from the workspace. Java compilation
targets 17. Kotlin was updated to 2.4.0 after the actual compiler rejected Firebase
35.1.0 metadata when using 2.2.10. Compatibility is established by executed build
results, not solely version assumptions. Hilt 2.60.1 and KSP 2.3.13 generate DI and
Room code. Dependency versions live in the version catalog.

Official references consulted:
- https://dagger.dev/hilt/gradle-setup.html
- https://developer.android.com/build/migrate-to-ksp
- https://developer.android.com/jetpack/androidx/releases/room
- https://developer.android.com/jetpack/androidx/releases/lifecycle
- https://developer.android.com/jetpack/androidx/releases/navigation
- https://developer.android.com/jetpack/androidx/releases/datastore
- https://firebase.google.com/docs/android/setup

## Repository contracts instead of per-operation use cases

Features depend on small authentication, applications, jobs, preferences and
synchronization interfaces in core:model. Infrastructure is hidden in core:data.
Pure validators and conflict ordering are shared domain rules. Extra trivial use
case classes would add indirection without behavior.

## Room is the local source of truth

Offline writes complete locally and keep an outbox marker on the same row. This
avoids a dual-write race between a successful CRUD transaction and scheduling
remote delivery. Firestore is a synchronization transport, not a second UI state
source. Tombstones and deterministic revision ordering make retries idempotent.
Trade-offs and clock assumptions are documented in ARCHITECTURE.md.

## Real XML list

A Fragment and XML RecyclerView coexist with Navigation Compose via
androidx.fragment:fragment-compose. Search and status are ViewModel state,
not filtering logic buried in an adapter. Form and detail remain Compose.

## REST provider and serialization

Arbeitnow publishes a free no-key API suitable for side projects:
https://www.arbeitnow.com/blog/job-board-api
https://documenter.getpostman.com/view/18545278/UVJbJdKh

The verified endpoint is https://www.arbeitnow.com/api/job-board-api.
It returned HTTP success and job fields in this environment on 2026-11-11.
The app uses the first page and explicitly labels search as searching loaded jobs.
The provider focuses on Europe, not specifically Brazilian or Android vacancies.
No fixed numeric request quota was found in the consulted provider documentation;
the app caches successful responses for 15 minutes and does not poll the jobs API.
Recheck provider terms before a public commercial launch. Attribution appears in
the jobs screen and saved notes; the canonical job URL is preserved.

Retrofit 3.0.0 uses Gson through its official converter. DTOs are validated and
mapped into domain models. HTML descriptions are displayed as plain text and are
never executed in a WebView. HTTPS links are validated; API failures expose retry.
Saved imported jobs use deterministic UUIDs per user/provider job, preventing
duplicates on repeated imports. A deleted import may be explicitly saved again.

## Firebase configuration

Production uses the developer's ignored app/google-services.json with the Google
services plugin applied only when the file exists. Without configuration, builds
still compile and clearly show setup instructions. There are no real credentials
in source control.

-PfirebaseEmulators=true enables local-only Firebase options in debug builds.
It targets demo-jobtrackpro through device loopback and ADB reverse and cannot activate in release.
The emulator project does not create or deploy external resources. This allows
authentication, synchronization and authorization tests without a production account.

## Security and local retention

Firestore rules scope each record to its owner and validate allowed fields/types.
Only soft deletion is permitted. Passwords are handled by Firebase Authentication.
Android backups are disabled to avoid copying per-user application records outside
the intended sync mechanism. Release cleartext traffic is disabled. Debug permits
cleartext for localhost Firebase emulator traffic.

Logout retains local per-user rows and pending writes so offline edits are not
destroyed. A subsequent user cannot query another UID through repository methods.
This is logical account isolation; the database does not use additional encryption.
Account removal and retention policies require a separate product decision.

## Verification policy

Unit tests use mocks/fakes and MockWebServer without internet. Real API availability
is a separate manual probe. Instrumentation uses local Firebase and Android
emulators. Production Firebase cannot be declared validated without a real
configured project and deployed rules. Test execution evidence is recorded in
VERIFICATION.md; pending checks remain explicitly pending.
