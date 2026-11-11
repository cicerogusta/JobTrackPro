# AGENTS.md readiness audit

Historical initial audit, before implementation. See VERIFICATION.md, ROADMAP.md
and QUALITY_REVIEW.md for the current implementation and delivery status.

Audit date: 2026-11-11.

## Verdict

**Partially completed; not ready for delivery under AGENTS.MD section 20.**
The current implementation is an initial Compose UI scaffold. Missing functionality
requires implementation, not just external Firebase configuration.

## Validation performed

Executed from the project root in PowerShell:

```powershell
$env:JAVA_HOME = 'C:\DEV\IDES\AndroidStudio\jbr'
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug --console=plain
```

- Result: BUILD SUCCESSFUL; 150 actionable tasks executed.
- Debug APK generated at `app/build/outputs/apk/debug/app-debug.apk`.
- Three unit tests passed, one per module. All are generated arithmetic examples
  (`assertEquals(4, 2 + 2)`); they provide no coverage of application behavior.
- Lint completed with warnings. `ObsoleteLintCustomCheck` reports incompatible
  lifecycle lint APIs, causing `UnsafeLifecycleWhenUsage` and
  `RepeatOnLifecycleWrongUsage` checks to be skipped. A successful lint task
  therefore does not establish complete lifecycle validation.
- `adb devices` returned no connected device. Instrumentation tests, runtime
  behavior, accessibility, large fonts and screen-size adaptation were not tested.
- The first build attempt could not locate Java. The successful attempt used the
  existing Android Studio JBR through a process-local environment variable;
  no system Java configuration was changed.
- Release builds were not tested.

## Evidence and stage assessment

| Stage | Status | Evidence / missing acceptance criteria |
| --- | --- | --- |
| 1. Initialization | Partial | Gradle Kotlin DSL, version catalog and three compiling modules exist. Hilt is absent. Only `:app`, `:app:home` and `:app:designsystem` are included in `settings.gradle.kts`; the specified core/features structure and architectural justification are absent. |
| 2. Base architecture | Missing | No domain models, typed application status, repository contracts, ViewModels, StateFlow UI states, DI or Navigation Compose graph. `JobTrackApp` renders `HomeScreen` directly. |
| 3. Design system | Partial | `JobTrackTopAppBar` and `EmptyState` exist. Light/dark theme follows the system, but theme resides in the app module. Loading/error states and shared spacing/shape tokens are missing. |
| 4. Compose dashboard | Partial | `HomeScreen` shows welcome text and an unconditional empty-state card. No repository-backed statistics, recent applications, add action, ViewModel or observable state. |
| 5. XML applications | Missing | No application Fragment, XML layouts, RecyclerView, ListAdapter, DiffUtil, ViewBinding, search, status filtering or Compose/Views navigation integration. XML manifests/themes/backup resources do not satisfy this UI requirement. |
| 6. Room and CRUD | Missing | No Room entities, DAO, database, data source, repository, per-user isolation, form validation, creation, editing, details or confirmed deletion. |
| 7. REST jobs API | Missing | No Retrofit/OkHttp service, DTOs, mappers, API documentation, job search/list, retry or save-job flow. No documented local demonstration alternative. |
| 8. Authentication | Missing | No Firebase Authentication dependency or implementation for login, registration, password recovery, session persistence, protected routes or logout. |
| 9. Synchronization | Missing | No Firestore integration, security rules, stable identifiers, update/conflict strategy or interrupted/resumed connection tests. |
| 10. Finalization | Missing | No behavioral unit/integration/UI tests, required English README, ARCHITECTURE.md, DECISIONS.md, ROADMAP.md or APK installation instructions. No runtime accessibility validation. |

Settings requirements are also missing: user profile, persisted theme selection
through DataStore, logout and application information screen.

## Architecture and code-quality assessment

The existing UI components have limited, understandable responsibilities and use
Material 3. This is a useful starting point, but it is insufficient evidence of
the required Clean Architecture, MVVM or SOLID repository contracts: those layers
have not been implemented. Substitutability and interface segregation cannot be
meaningfully assessed without the required contracts and implementations.

Remaining scaffold code includes `Greeting` and `GreetingPreview` in
`MainActivity.kt`, unused imports, and template comments in the theme. The top bar
component also owns the full Scaffold, which should be made explicit in its name
or separated when the design system is expanded. These cleanup items are secondary
to the absent product flows.

The workspace has no Git repository: `git status --short` returned
`fatal: not a git repository`. Commit history and GitHub setup could not be audited.

## Recommended implementation order

1. Complete initialization with Hilt and a documented module/dependency structure.
2. Add typed domain models, cohesive repository contracts, navigation and immutable
   lifecycle-aware UI state; test dashboard behavior with a repository fake.
3. Implement the XML applications list and Room-backed Compose CRUD, including
   validation, filtering and details.
4. Integrate and document a verified jobs API; implement mapping and failure states.
5. Implement Firebase authentication and isolated per-user synchronization with
   security rules and a documented conflict policy. External project configuration
   and validation must be recorded separately from code implementation.
6. Add DataStore settings and behavioral integration/UI tests, validate on a device,
   complete accessibility checks and the required professional documentation.

This audit added only this report to source files; it did not implement missing
features or change application code, dependencies or system configuration.
