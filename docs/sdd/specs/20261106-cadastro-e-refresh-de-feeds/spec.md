# SDD: cadastro-e-refresh-de-feeds

**Complexity:** L

> Aprovada pelo usuário e implementada no branch local `codex/rss-java27`.

## 1. Context
A SPEC prevê `POST/GET /v1/feeds`, refresh manual e `GET /v1/links`.

## 2. Goal
Cadastrar feeds e coletar links periodicamente.

## 3. Scope
### 3.1 In scope
- Endpoints de cadastro, listagem, refresh e links.

### 3.2 Out of scope
- Broker de mensagens (só se o refresh exigir).

## 4. Requirements
### 4.1 Functional
- FR-01: Cadastrar feed válido.
- FR-02: Refresh manual coleta links novos.

### 4.2 Non-functional
- A definir na revisão.

## 5. Architecture and Design
Implementação inicial mantém feeds e resultados em memória para a POC. O coletor RSS continua sendo biblioteca; dashboard pode fornecer persistência durável no ambiente integrado sem duplicar este contrato básico.

## 6. Implementation Plan
Expor registro/listagem de feeds e refresh manual; validar URL/limites antes da chamada e deduplicar novos links.

## 7. Acceptance Criteria
- AC-01: Feed cadastrado aparece na listagem.
- AC-02: Refresh não duplica links.

## 8. Observability and Operations
- A definir.

## 9. Risks and Mitigations
- A definir.

## 10. Rollback
A definir.

## 11. References
- TODO.md do projeto e SPEC.md (API futura)
