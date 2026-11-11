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

The original Arbeitnow integration was replaced by okemprego to serve Brazil.
Official documentation and schema:
- https://www.okemprego.com/pt/api
- https://www.okemprego.com/api/v1/openapi.json

Both the Brazil listing and an Android/São Paulo keyword/location request returned
HTTP success on 2026-11-11. All requests use country=BR. The source is free and
unauthenticated, and permits job-search/indexing uses with canonical attribution.
It publishes a per-IP limit of 20 requests per 10 seconds and a maximum of 200 pages,
50 results per page. The app requests 20 items, caches pages for 15 minutes (bounded
to 20 cached pages), and submits searches only on explicit user action.

JobsRepository accepts JobSearch and returns JobPage. DTO mapping validates Brazil,
HTTPS links and required fields; JobOpening carries nullable typed WorkMode so
missing metadata is not silently treated as on-site. The UI asks for an explicit
mode before saving such a vacancy. ViewModels coordinate submitted filters, paging,
retry and saving; Composables render state and forward events. More-page errors
keep already loaded rows and retry the same page. New filters restart at page one.

Gson remains the official Retrofit converter. HTML is displayed as plain text.
Each card links to its canonical okemprego page. Imported IDs use provider-prefixed
slugs and a deterministic user/job UUID. Previously saved Arbeitnow applications
and URLs are retained; there is no Room schema change or bulk import migration.

This is a single provider's catalog, not a claim of coverage of every Brazilian
job board. Postings can expire or be inaccurate. Page-indexed results may shift
when new jobs arrive; the UI deduplicates IDs, and filters should be narrowed when
the page cap is reached. Provider terms were checked before the integration.

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

## Module-owned user-facing strings

UI text lives in each module's src/main/res/values/strings.xml. Shared failure
messages belong to core:common and status/work-mode/theme labels to core:designsystem.
Compose resolves resources with stringResource; Fragment/RecyclerView code uses
getString. Form validation returns a set of invalid domain fields; the applications
feature maps those fields to resource IDs. Domain validation never depends on R,
Context or a translated sentence. Counters/details use positional format arguments.
Localized fallback location and provider notes are resolved by the jobs feature
before saving; network DTOs retain an empty location rather than inventing UI text.
Existing user-entered content, protocol values, routes and persistence keys remain
data/identifiers. UI tests resolve labels from resources instead of assuming literal
Portuguese strings. Display timestamps follow the current configuration locale.

## Verification policy

Unit tests use mocks/fakes and MockWebServer without internet. Real API availability
is a separate manual probe. Instrumentation uses local Firebase and Android
emulators. Production Firebase cannot be declared validated without a real
configured project and deployed rules. Test execution evidence is recorded in
VERIFICATION.md; pending checks remain explicitly pending.
