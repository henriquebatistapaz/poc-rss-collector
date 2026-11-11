# SDD: parser-rss-2

**Complexity:** M

> SDD retroativa: documenta o que já foi entregue (commit 47934b6 (feat: parse and validate RSS 2.0 feeds)). Escrita em 2026-11-06 a partir do código e do histórico do repo; status `Done`.

## 1. Context
O projeto começou com uma SPEC inicial (SPEC.md) para coletar links de feeds RSS/Atom. A primeira fatia precisava provar o parsing antes de qualquer API ou persistência.

## 2. Goal
Ler documentos RSS 2.0 fornecidos pelo chamador e extrair itens determinísticos, rejeitando XML hostil.

## 3. Scope
### 3.1 In scope
- Parser RSS 2.0 com `FeedItem` (feed de origem, id, título, link, data).
- Validação de URL HTTP(S) pública (`RssFeedLinkValidator`).
- Validador de documento (`RssFeedValidator`).
- README com uso local.

### 3.2 Out of scope
- Busca HTTP de URLs.
- Atom.
- Persistência.

## 4. Requirements
### 4.1 Functional
- FR-01: Extrair GUID (ou link como identidade), título, link normalizado e data de publicação de cada `<item>`.
- FR-02: Ignorar itens sem link HTTP(S) válido, mantendo os demais.
- FR-03: Rejeitar documentos com DTD ou entidades externas.

### 4.2 Non-functional
- Limite de tamanho de documento e de quantidade de itens.
- Saída determinística.

## 5. Architecture and Design
Classes Java puras no pacote `application`, sem dependência de Spring, para reuso como biblioteca. XML lido com fábrica configurada para desligar DTD e entidades externas.

## 6. Implementation Plan
Já implementado. Principais pontos no código:
- `RssFeedParser`
- `RssFeedValidator`
- `RssFeedLinkValidator`
- `domain/FeedItem`

## 7. Acceptance Criteria
- AC-01: Exemplos RSS 2.0 válidos geram os itens esperados (`RssFeedParserTest`).
- AC-02: Metadado opcional ausente não quebra o parse.
- AC-03: Item com link inválido é descartado e os válidos permanecem.
- AC-04: Documento com DTD/entidade externa é rejeitado.
- AC-05: Testes rodam sem rede.

## 8. Observability and Operations
- Sem requisitos específicos além dos logs padrão da aplicação.

## 9. Risks and Mitigations
- —

## 10. Rollback
Reverter os commits/PRs listados nas referências.

## 11. References
- commit 47934b6 (feat: parse and validate RSS 2.0 feeds)
