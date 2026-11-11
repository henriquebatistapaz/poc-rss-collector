# SDD: versoes-maven-em-properties

**Complexity:** S

> SDD retroativa: documenta o que já foi entregue (commit 2faccae (build: move maven versions to properties)). Escrita em 2026-11-06 a partir do código e do histórico do repo; status `Done`.

## 1. Context
Versões de dependências estavam espalhadas no `pom.xml`.

## 2. Goal
Centralizar versões em `<properties>` para facilitar upgrades.

## 3. Scope
### 3.1 In scope
- Mover versões explícitas para `<properties>`.

### 3.2 Out of scope
- Upgrade de versões.

## 4. Requirements
### 4.1 Functional
- FR-01: Toda versão explícita referencia uma property.

### 4.2 Non-functional
- —

## 5. Architecture and Design
Padrão `${artefato.version}` no `pom.xml`.

## 6. Implementation Plan
Já implementado. Principais pontos no código:
- `pom.xml`

## 7. Acceptance Criteria
- AC-01: `mvn test` verde.
- AC-02: Nenhuma `<version>` literal fora de `<properties>` e do parent.

## 8. Observability and Operations
- Sem requisitos específicos além dos logs padrão da aplicação.

## 9. Risks and Mitigations
- —

## 10. Rollback
Reverter os commits/PRs listados nas referências.

## 11. References
- commit 2faccae (build: move maven versions to properties)
