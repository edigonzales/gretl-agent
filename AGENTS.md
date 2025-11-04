# Repository Guidelines

Welcome to the GRETL Copilot Agent project. Follow these rules whenever you work within this repository:

## Required Workflow
- **Run the full test suite** with `./gradlew test` before committing.
- **Add or update automated tests** whenever you change behavior or introduce new features.
- **Keep the documentation accurate.** If a change affects architecture, behavior, or public APIs, update `README.md` accordingly. Ensure the Mermaid diagram and the class descriptions stay in sync with the implementation.

## Coding Notes
- Favor clear, maintainable code and keep imports free of `try/catch` wrappers.
- Align with existing formatting and idioms used in the codebase.

## Database
- The vector database can be started with `docker compose up`. It runs on port 54323. The database name is `gretl_rag`. Username and password is `gretl`.
- The DDL of the database scheme and tables is in `./initdb/01_init.sql`.
- The embeddings will be imported on startup.

Thank you for contributing!
