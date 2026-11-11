---
name: add-integration-test
description: Add an integration test that runs against real infrastructure (database, Kafka, Redis, Elasticsearch) using Testcontainers, self-contained and out of the default unit cycle. Use when a task needs to verify persistence, messaging or search behavior that mocks cannot prove.
---

# Add an integration test

Unit tests prove logic; integration tests prove the contract with real infrastructure. The goal: self-contained (no "docker-compose must be running"), deterministic, and out of the fast unit cycle.

## 1. Check what the project already has

Look for existing integration test setup: Testcontainers dependencies, base classes, tags/markers, a separate Maven profile or pytest marker. **Extend the existing pattern; don't create a second one.** No setup at all? Introducing Testcontainers is an SDD (it changes the harness for everyone) — propose it, implement after approval.

## 2. Java (Testcontainers + JUnit 5)

```java
@Testcontainers
class OrderRepositoryIT {          // *IT suffix → failsafe, not surefire
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");   // match the REAL version from docker-compose/prod

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }
}
```

- `static @Container` = one container per class (fast); a shared singleton base class is better still for suites.
- Migrations run against the container (Flyway auto-runs) — this doubles as the migration test.
- Kafka: `KafkaContainer`; Redis: `GenericContainer("redis:7-alpine")`; Elasticsearch: `ElasticsearchContainer`.

## 3. Python (testcontainers-python + pytest)

```python
@pytest.mark.integration
def test_saves_order(postgres_container):   # fixture in conftest.py, session-scoped
    ...
```

Marker `integration` + default `addopts = "-m 'not integration'"` keeps it out of the fast cycle (see python-quality playbook).

## 4. Rules that keep these tests trustworthy

- **Pin real versions**: container image version = the version in docker-compose/production. Testing against `latest` proves nothing about your environment.
- Each test creates its own data and does not depend on execution order or data from other tests.
- Assert behavior through the public API of the component (repository method, consumer handler) — not by peeking at container internals.
- External SaaS (LLM APIs, payment providers) are NOT integration-testable this way — those stay mocked; the contract with them is verified by contract tests or in staging.
- CI: these run in the dedicated job (docker-in-docker) per the harness template — never in the unit job.

## Anti-patterns to refuse

- Integration test that requires manually started local infra
- Sleeping (`Thread.sleep`) instead of proper waits (`Awaitility` / container wait strategies)
- One giant test that walks the whole pipeline — test one contract per test
