---
applyTo: "**/src/test/**/*.java"
description: "Conventions for Java test code"
---

# Java test conventions

- JUnit 5 (Jupiter). Do not use JUnit 4 (`org.junit.Test`).
- Test names describe the behavior: `shouldReturnErrorWhenCredentialIsInvalid()` (follow the project's naming language from AGENTS.md).
- Arrange-Act-Assert structure with blank lines separating the blocks.
- Mocks (Mockito) only for external dependencies (HTTP, messaging, third-party services); do not mock entities or DTOs.
- Calls to external services are NEVER real in tests — always mock/stub the project's abstraction.
- Tests requiring infrastructure (database, queue) must be annotated/separated so they don't run in the default unit cycle without the infra available.
- A test should fail for a single reason — avoid multiple unrelated asserts.
