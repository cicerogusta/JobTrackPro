# Delivery roadmap

Updated: 2026-11-11.

**Status: partially completed under AGENTS.MD section 20.**
All required product flows have implementation and local verification. Production
Firebase configuration and production cross-device validation remain external prerequisites.
See VERIFICATION.md for actual command results and QUALITY_REVIEW.md for the code audit.

| Stage | Implementation and evidence | Status |
| --- | --- | --- |
| 1. Initialization | 12 logical modules, Gradle catalog, Java/Kotlin, KSP, Hilt; successful Android builds | Completed locally |
| 2. Base architecture | Typed models, repository contracts, immutable StateFlow, protected navigation and DI | Completed locally |
| 3. Design system | Shared Material 3 theme, explicit contrast colors, spacing, controls, accessible radio groups and visual states | Completed locally |
| 4. Dashboard | Repository-backed statistics/recent rows, loading/error/empty, fake-backed ViewModel test | Completed locally |
| 5. XML list | Fragment, ConstraintLayout, ViewBinding, RecyclerView/ListAdapter/DiffUtil, search/status filters and Compose navigation | Completed locally |
| 6. Room/CRUD | Per-user persistence, validated forms/details, concurrent revision protection, confirmed deletion, DAO/repository/UI tests | Completed locally |
| 7. REST | Verified Arbeitnow API, Retrofit/Gson mapping, cache, error/retry, local search and stable imports | Completed locally |
| 8. Authentication | Registration/login/reset/logout/session and protected routes tested against local Authentication emulator | Implemented; real-project setup pending |
| 9. Synchronization | Firestore transactions, server confirmation, durable dirty markers, tombstones, deterministic conflicts, ownership rules; interruption/resumption tested | Implemented; real-project validation pending |
| 10. Finalization | Behavioral tests, Lint, real captures, rotation/large-font checks, SOLID/Clean Code review and English documentation | Local verification completed; external delivery checks pending |

## Remaining external/manual delivery checks

- Supply app/google-services.json from the intended Firebase project.
- Enable Email/Password, create the database and deploy/review Firestore rules.
- Verify production authentication, password-reset delivery and synchronization on
  two real devices/accounts, including denied cross-account access.
- Configure private release signing and validate an installable release build.
- Choose/create a GitHub remote if the portfolio should be published; local Git
  commits do not imply a remote repository exists.
- Broaden manual TalkBack/Switch Access, OEM and tablet coverage before public distribution.

## History

The project began with a static Compose welcome/empty screen; AUDIT.md preserves
that initial assessment. The subsequent implementation replaced the scaffold with
the flows above. MENTOR_NOTES.md explains responsibilities, trade-offs, verification
commands and interview questions for each stage.
