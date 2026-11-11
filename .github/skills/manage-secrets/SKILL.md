---
name: manage-secrets
description: Add or rotate a secret (API key, password, token, connection string) the right way. Use whenever a task introduces a credential, or when a secret is found hardcoded and must be extracted.
---

# Manage secrets

The rule "never commit secrets" only works when the right way is easier than the wrong way. This is the right way.

## 1. Where secrets live (in order of resolution)

1. **Local dev**: `.env` / `application-local.properties` — MUST be in `.gitignore` (verify, don't assume). Provide a committed `.env.example` / `application-local.properties.example` with the KEY NAMES and placeholder values, never real ones.
2. **CI**: masked/protected CI variables (GitLab: Settings → CI/CD → Variables, masked+protected; GitHub: Settings → Secrets and variables → Actions).
3. **Runtime**: environment variables injected by the platform (compose `env_file`, k8s secrets, vault) — code reads `${VAR_NAME}` with no default for real secrets (a default is a silent misconfiguration).

## 2. Adding a new secret — checklist

- Name it clearly and consistently: `<SERVICE>_<PURPOSE>` (`OPENAI_API_KEY`, `DB_PASSWORD`) — follow the existing convention in the project.
- Reference in config: `password: ${DB_PASSWORD}` (Spring) / `os.environ["DB_PASSWORD"]` (Python) — fail fast if missing.
- Add the key name to `.env.example` + a line in the README/AGENTS.md prerequisites if devs need it locally.
- Add to CI variables if the pipeline needs it (and mark masked).
- NEVER: in code, in committed config, in docker-compose.yml values (use `env_file`), in Dockerfiles (build args leak into image history), in test fixtures, in MR descriptions or issue comments.

## 3. Found a hardcoded secret?

Treat as an incident, not a cleanup:

1. Extract to env var (steps above) — but the git history still has it.
2. **The secret is burned: rotate it** at the provider. Removing from HEAD does not un-leak it.
3. Tell the user explicitly that rotation is required; offer to check history reach (`git log -S '<fragment>' --oneline | head`).

## Safety rules

- Never print a secret's value in terminal output, logs or files you write — handle names, not values.
- Secret scanning in CI (gitleaks or platform-native) is the harness for this skill — suggest it if absent.
- Rotation procedures involving the provider (revoking keys) are proposed to the user, not executed unilaterally.
