# SDD: normalizacao-e-identidade-de-links

**Complexity:** M

> Aprovada pelo usuário e implementada no branch local `codex/rss-java27`.

## 1. Context
Duplicatas por `(feed, id)` e por link normalizado estão na SPEC mas não implementadas.

## 2. Goal
Normalizar links e definir identidade de itens.

## 3. Scope
### 3.1 In scope
- Normalização de URL.
- Regra de duplicidade.

### 3.2 Out of scope
- Persistência.

## 4. Requirements
### 4.1 Functional
- FR-01: Itens com mesmo `(feed, id)` são o mesmo item.
- FR-02: Sem id, o link normalizado é a identidade.

### 4.2 Non-functional
- A definir na revisão.

## 5. Architecture and Design
Descarta fragmentos e parâmetros de rastreamento comuns (`utm_*`, `fbclid`, `gclid`), preserva parâmetros funcionais, normaliza host/porta/caminho. A identidade prioriza `(feed, id)` quando há ID e usa URL normalizada quando o ID falta.

## 6. Implementation Plan
Aplicar canonicalização na validação/parsing e deduplicar links por identidade estável.

## 7. Acceptance Criteria
- AC-01: Itens duplicados no mesmo documento viram um só.

## 8. Observability and Operations
- A definir.

## 9. Risks and Mitigations
- A definir.

## 10. Rollback
A definir.

## 11. References
- TODO.md do projeto
