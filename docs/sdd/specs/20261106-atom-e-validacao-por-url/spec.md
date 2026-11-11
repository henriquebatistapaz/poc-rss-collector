# SDD: atom-e-validacao-por-url

**Complexity:** L

> SDD retroativa: documenta o que já foi entregue (commit 575f7c7 (feat: support Atom feeds and validate feed URLs)). Escrita em 2026-11-06 a partir do código e do histórico do repo; status `Done`.

## 1. Context
Com o RSS 2.0 coberto, faltava suportar Atom e permitir validar um feed a partir da URL, o que exige buscar conteúdo de terceiros com segurança.

## 2. Goal
Expor `POST /v1/feeds/validate`, que busca a URL pública e diz se a resposta atual é RSS 2.0 ou Atom 1.0 válido.

## 3. Scope
### 3.1 In scope
- Parsing Atom 1.0 (id, title, updated obrigatórios; link alternativo HTML; resolução de links relativos).
- Endpoint REST com Swagger/OpenAPI (springdoc).
- `BoundedFeedHttpFetcher` com timeout, limite de resposta, limite de redirects e checagem de destino público.
- Smoke test opcional contra feeds reais.

### 3.2 Out of scope
- Cadastro de feeds, refresh agendado, persistência.
- Testes dedicados do fetcher para redirects/DNS (pendente no TODO).

## 4. Requirements
### 4.1 Functional
- FR-01: `POST /v1/feeds/validate` recebe `{"feedUrl": ...}` e responde `feedUrl`, `valid`, `itemCount`, `message`.
- FR-02: Aceitar somente `http`/`https` para destinos públicos, revalidando DNS antes de cada conexão e redirect.
- FR-03: Suportar Atom 1.0 com as regras de identidade e links da SPEC.

### 4.2 Non-functional
- Timeout de 5 s, resposta de até 1 MB, no máximo 3 redirects.
- Não repassar cookies nem credenciais ao host do feed.

## 5. Architecture and Design
O serviço `FeedUrlValidationService` orquestra `RssFeedLinkValidator` → `BoundedFeedHttpFetcher` (interface `FeedDocumentFetcher`) → `RssFeedValidator`/`RssFeedParser`. O fetcher é atrás de interface para os testes usarem mocks.

## 6. Implementation Plan
Já implementado. Principais pontos no código:
- `api/FeedValidationController`
- `application/FeedUrlValidationService`
- `application/BoundedFeedHttpFetcher`
- `application/FeedDocumentFetcher`
- `RssFeedParser` (ramo Atom)

## 7. Acceptance Criteria
- AC-01: Feeds Atom válidos são aceitos e entradas sem link válido são ignoradas (`RssFeedParserTest`).
- AC-02: O endpoint responde `valid=true/false` com mensagem (`FeedValidationControllerTest`).
- AC-03: URLs não públicas ou com esquema inválido são recusadas (`RssFeedLinkValidatorTest`, `FeedUrlValidationServiceTest`).
- AC-04: `RSS_EXTERNAL_TESTS=true mvn test` valida feeds reais (`RealRssFeedSmokeTest`).

## 8. Observability and Operations
- Sem requisitos específicos além dos logs padrão da aplicação.

## 9. Risks and Mitigations
- SSRF via DNS rebinding → checagem de DNS antes de cada conexão e redirect.
- Feeds enormes → limite de 1 MB e de itens.

## 10. Rollback
Reverter os commits/PRs listados nas referências.

## 11. References
- commit 575f7c7 (feat: support Atom feeds and validate feed URLs)
