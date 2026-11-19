# SDD Index

The **Status column is the state machine of the cycle** — prompts read it to decide what runs, and `/implement-spec` refuses specs that aren't `Approved`. Keep it accurate.

| Status | Meaning | Who sets it |
|---|---|---|
| `Draft` | Spec written, not approved — may still change | `/new-spec` · `/spec-from-issue` |
| `Approved` | **Gate 1 passed — the human approval act IS changing this cell** (edit it yourself, or tell the agent "approved" and it updates + commits) | **Human only** |
| `In progress` | Being implemented | `/implement-spec` (on start) |
| `Shipped` | MR/PR open, waiting CI + human review (Gate 2) | `/open-mr` |
| `Done` | Merged | `/next` (on detecting the merge) or human |
| `Blocked` | Waiting on something external — say what in Notes | Anyone (state the reason) |

Rules: agents NEVER set `Approved`. A spec that needs rework after approval goes back to `Draft` (approval is re-required). Every spec directory must have a row here — an unlisted spec doesn't exist for the cycle.

| Spec | Issue | Status | Notes |
|------|-------|--------|-------|
| [`20261106-parser-rss-2/`](specs/20261106-parser-rss-2/spec.md) | — | Done | Parser e validador RSS 2.0 com XML endurecido |
| [`20261106-atom-e-validacao-por-url/`](specs/20261106-atom-e-validacao-por-url/spec.md) | — | Done | Atom 1.0, POST /v1/feeds/validate e fetcher com limites anti-SSRF |
| [`20261106-jar-biblioteca/`](specs/20261106-jar-biblioteca/spec.md) | — | Done | Build gera jar comum (biblioteca) e executável -exec |
| [`20261106-renomeacao-rss-collector-mini/`](specs/20261106-renomeacao-rss-collector-mini/spec.md) | — | Done | Renomeação do projeto/pacotes e referências ao poc-dashboard |
| [`20261106-versoes-maven-em-properties/`](specs/20261106-versoes-maven-em-properties/spec.md) | — | Done | Versões de dependências centralizadas em properties |
| [`20261106-testes-do-fetcher-http/`](specs/20261106-testes-do-fetcher-http/spec.md) | — | In progress | Implementado em `codex/rss-java27`; `mvn -B clean verify` passou com 32 testes no JDK 27.0.1.0.3 em 2026-11-11 (um smoke externo permanece ignorado) |
| [`20261106-normalizacao-e-identidade-de-links/`](specs/20261106-normalizacao-e-identidade-de-links/spec.md) | — | In progress | Canonicalização remove fragmentos e parâmetros de rastreamento; identidade por feed/id ou URL normalizada |
| [`20261106-cadastro-e-refresh-de-feeds/`](specs/20261106-cadastro-e-refresh-de-feeds/spec.md) | — | In progress | Cadastro/listagem e refresh manual concluídos no branch local `codex/rss-java27` |
| [`20261106-ai-init-adocao-v2/`](specs/20261106-ai-init-adocao-v2/spec.md) | — | Done | Guardrails e adoção v2 validados localmente (doc-check, hooks e workflows) |
| [`20261110-atualizar-java-27/`](specs/20261110-atualizar-java-27/spec.md) | — | In progress | JDK 27.0.1.0.3, Boot 4.1.2; `mvn -B clean verify` passou com 32 testes (1 smoke externo ignorado) em 2026-11-11; branch local |
| [`20261119-padronizar-maven-wrapper/`](specs/20261119-padronizar-maven-wrapper/spec.md) | — | Done | Spec filha da macro 20261119-padronizar-maven-wrapper; wrapper Maven para 1 POM(s), sem alterar JDK ou comportamento. |
