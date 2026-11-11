# Architecture

JobTrack Pro uses Kotlin, MVVM, unidirectional state and a pragmatic Clean Architecture.
The app composes independent feature modules; features do not depend on one another.

## Module responsibilities

- app: Hilt application, protected Navigation Compose graph and lifecycle-bound synchronization.
- core:model: Android-independent domain types, validation, repository contracts and deterministic conflict policy.
- core:common: shared immutable UI state and user-facing failure messages.
- core:database: Room entity, DAO, schema and local data source.
- core:network: Retrofit endpoint, Gson DTO mapping and HTTPS client configuration.
- core:data: repository implementations, Firebase adapters, error classification, DataStore and synchronization.
- core:designsystem: Material 3 theme, spacing, common components and typed display labels.
- feature:home: dashboard state, statistics and Compose UI.
- feature:applications: XML list and Compose form/detail flows.
- feature:auth: login, registration and password recovery.
- feature:jobs: remote jobs listing, local search and saving.
- feature:settings: profile, theme persistence, sync feedback and logout.

The existing physical directories app/home and app/designsystem are mapped to
feature:home and core:designsystem in settings.gradle.kts. This preserves the
existing files while making the logical graph match their responsibilities.

## Data flow

UI -> ViewModel -> domain repository contract -> repository -> data source.
Room Flow -> immutable StateFlow -> lifecycle-aware Compose/Fragment collection.

ViewModels never construct Room, Retrofit or Firebase. Hilt binds implementations
and scopes authentication, preferences and synchronization to the application.
There are no one-line use case wrappers: validation and conflict resolution are the
domain operations that justify their own abstractions.

## Authentication boundaries

A missing Firebase configuration produces an explicit setup screen. There is no
silently authenticated demo account. Signed-out users see only authentication.
The authenticated navigation tree is keyed by UID and destroyed on logout.
Repositories derive the account from AuthRepository rather than accepting an
arbitrary UI-selected owner. Room primary keys and every query include userId.
Passwords are not saved in SavedStateHandle, preferences, Room or logs.

## Compose/Views interoperability

MainActivity is a Hilt FragmentActivity. Navigation Compose hosts
ApplicationsFragment through AndroidFragment. The fragment inflates ConstraintLayout
with ViewBinding, uses RecyclerView/ListAdapter/DiffUtil and collects state with
repeatOnLifecycle. Navigation back to Compose details goes through the small
ApplicationNavigation host contract. Binding and adapter references are cleared in
onDestroyView. Compose theme changes are also applied to the host AppCompat delegate
when this is configured; runtime verification is tracked in VERIFICATION.md.

## Offline-first writes and synchronization

A local write atomically sets a new revision and dirty flag. UI success means the
local transaction succeeded, not that the server acknowledged it. Deletion is a
tombstone; hidden rows remain available for propagation to offline devices.

A foreground loop attempts synchronization immediately on authenticated STARTED
lifecycle and every 30 seconds while active. Settings provides a manual retry.
No work continues indefinitely when the activity is stopped. Pending records
survive process death and are retried on the next foreground session.

For each pending record, a Firestore transaction compares (updatedAt, revision),
writes the deterministic winner and preserves the existing creation timestamp.
A server read confirms the merged revision before acknowledgement. The
acknowledged winner is merged locally in a Room transaction: a newer local
edit cannot be cleared by an older acknowledgement. The server snapshot is then
pulled with Source.SERVER and merged using the same ordering, including tombstones.
No hard server deletes occur. UID checks guard every local merge during account
switches. A mutex prevents overlapping synchronization cycles.

Conflict policy is last-write-wins with a UUID lexical tie break. Device clocks
must be reasonably synchronized; there is no server-authoritative hybrid clock.
Tombstones have no automatic expiry, since deleting them could resurrect data on a
long-offline device. Full account snapshots suit this portfolio scale; pagination,
incremental cursors and scheduled background work are future scaling decisions.

## Failures and cancellation

AppException carries a typed Failure. The data layer distinguishes connectivity,
timeouts, authentication, HTTP/server, malformed payload and SQLite failures.
CancellationException is always rethrown. Technical exception messages are never
rendered directly to the user. Mutation failures are shown alongside retained form
data; read states distinguish loading, success/empty and error/retry.

## Database evolution

Schema version 1 is exported under core/database/schemas. There is no historical
database version requiring a migration. Future schema changes must add explicit
migrations and migration tests; destructive fallback is not enabled.
