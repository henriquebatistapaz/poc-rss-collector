# SDD: testes-do-fetcher-http

**Complexity:** S

> Aprovada pelo usuário e implementada no branch local `codex/rss-java27`; Java 27 `mvn clean install` passou com 32 testes, 0 falhas/erros e 1 smoke externo ignorado.

## 1. Context
O `BoundedFeedHttpFetcher` não tem testes dedicados.

## 2. Goal
Cobrir redirects, limite de tamanho, troca de DNS e falhas de rede.

## 3. Scope
### 3.1 In scope
- Testes do fetcher com servidor HTTP local/mocks.

### 3.2 Out of scope
- Mudar os limites.

## 4. Requirements
### 4.1 Functional
- FR-01: Teste para cada limite: 3 redirects, 1 MB, timeout, destino não público após redirect/DNS.

### 4.2 Non-functional
- A definir na revisão.

## 5. Architecture and Design
Testes usam servidor HTTP local e respostas controladas, sem rede externa; mantêm os limites de produção sem os alterar.

## 6. Implementation Plan
Cobrir redirecionamento, tamanho máximo, timeout, resolução DNS e falhas de rede.

## 7. Acceptance Criteria
- AC-01: Cada limite tem um teste que falha se o limite for removido.
- AC-02: Suíte continua sem rede externa.

## 8. Observability and Operations
- A definir.

## 9. Risks and Mitigations
- A definir.

## 10. Rollback
A definir.

## 11. References
- TODO.md do projeto (Next small slices)
