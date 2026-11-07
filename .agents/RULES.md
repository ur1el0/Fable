# Fable - AI Coding Agent Rules & Reference Index

All agent sessions must consult and adhere to the project standards defined across these documents:

* **System Overview & Architecture:** `architecture/01-system-overview.md`
* **Product Requirements Document:** `.agents/PRD.md`
* **Engineering Phases & Roadmap:** `.agents/PHASES.md`
* **Master Engineering Backlog:** `.agents/backlog.md`
* **Operational Playbooks & Gradle Commands:** `.agents/SKILLS.md`
* **Domain & Technical Glossary:** `.agents/GLOSSARY.md`
* **Troubleshooting Runbook:** `.agents/TROUBLESHOOTING.md`
* **Data Privacy & Security Policy:** `.agents/SECURITY.md`

### Specific Technical Rules (`.agents/rules/`):
* `rules/01-architecture.md`: Offline-first Clean Architecture, UDF, and Room single source of truth.
* `rules/02-kotlin-compose.md`: Compose state hoisting, recomposition efficiency, and stability.
* `rules/03-room-database.md`: Room schema migrations, DAO queries, transactions, and Flow streams.
* `rules/04-testing.md`: Unit testing standards, Turbine for Flows, and Compose UI tests.
* `rules/05-git-workflow.md`: Conventional atomic commits and branch hygiene.
* `rules/06-reader-engine-and-parsing.md`: Jsoup sanitization, text rendering, and pagination rules.
* `rules/07-background-workers.md`: WorkManager CoroutineWorkers, constraints, and foreground notifications.

