# Quality review

Review date: 2026-11-11. Delivery remains partial until real Firebase configuration
and production checks are completed.

Brazil jobs follow-up: country=BR is enforced on every request and validated in
DTO mapping. Search/pagination stay in JobsViewModel and the repository, with a
bounded filter/page cache, duplicate removal, retained rows on paging failure,
and typed work arrangements. Missing arrangements require user selection rather
than guessing. Canonical attribution links and module string resources are used.
Existing per-user applications and authentication are unchanged.

## SOLID

- SRP: ViewModels own presentation state; DAO/local source own SQLite; Firebase
  adapters own SDK operations; FirestoreSyncRepository owns synchronization.
- OCP: features use ApplicationRepository, AuthRepository and JobsRepository, so
  infrastructure can be replaced without rewriting screens.
- LSP: the dashboard fake validates ownership/input and preserves observable CRUD
  semantics; real repositories expose typed failures rather than hiding them.
- ISP: auth, applications, jobs, preferences and sync have separate cohesive contracts.
  ApplicationNavigation exposes only the host callback needed by the XML feature.
- DIP: Hilt binds infrastructure to domain contracts. Features never construct
  Retrofit, Room or Firestore.

## Clean Code and architecture

- Typed ApplicationStatus, WorkMode, ThemeMode, Failure and Session replace business
  strings. Immutable states expose read-only StateFlow.
- Validation, dashboard statistics, filters and conflict ordering are outside UI.
  Jobs search was moved into JobsViewModel during review.
- Coroutine cancellation is rethrown. Technical causes remain attached to
  AppException; user-facing messages do not expose backend details.
- Room writes and acknowledgements use transactions. Stale form revisions cannot
  silently overwrite a newer update; a conflict offers an explicit reload.
- No GlobalScope, forced non-null assertions, UI database access or private credentials.
- Logical module dependencies are acyclic. Only app composes feature modules.
- Old template arithmetic/context tests, unused theme code and unused colors were removed.

## Security and reliability

- Every Room read includes UID. Firestore rules reject unauthenticated access,
  foreign owners, invalid schema/status and hard deletion.
- Offline writes remain dirty until the server-confirmed version is merged locally.
  Remote acknowledgement cannot clear a concurrent newer local revision.
- Tombstones prevent older snapshots from resurrecting deleted records.
- Release disallows cleartext traffic. Debug local services are explicitly isolated.
- Passwords are never persisted by application code.
- Backups and device transfer explicitly exclude local files, preferences and databases.
- Logout retains pending local changes under the original UID; the confirmation
  explains this behavior. Real account deletion/retention policy is outside this scope.

## Accessibility and responsiveness

- Compose fields have text labels and validation feedback. Actions have 48dp minimum
  height. Radio choices use a single labeled selectable row and radio-group semantics.
- Navigation icons are decorative beside visible labels, avoiding duplicate announcements.
- Domain status is displayed as text rather than color alone.
- XML fields expose hints/labels, the loading indicator has a description and error/
  empty feedback uses a polite live region. Recycler rows are focusable and flexible.
- Light/dark primary foreground colors were defined explicitly after screenshot review.
- Layouts scroll, use flexible widths and are checked with large-font and rotation tests.
- Exhaustive TalkBack/Switch Access, OEM, tablet and contrast testing remains manual
  follow-up; the recorded emulator checks are not equivalent to an accessibility certification.

## Remaining delivery boundaries

Production Firebase setup, deployed-rule verification, real cross-device behavior,
release signing/distribution and publishing a GitHub remote require external resources.
The app is not declared fully delivered under AGENTS.MD section 20.
