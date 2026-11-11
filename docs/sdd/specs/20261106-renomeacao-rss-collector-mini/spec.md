# SDD: renomeacao-rss-collector-mini

**Complexity:** S

> SDD retroativa: documenta o que já foi entregue (commits 896ebbd, 97cb813, d6857aa). Escrita em 2026-11-06 a partir do código e do histórico do repo; status `Done`.

## 1. Context
O projeto nasceu como `rss-link-collector-mini` e o consumidor mudou de nome para `poc-dashboard`.

## 2. Goal
Alinhar nome do artefato, pacotes e documentação aos nomes atuais.

## 3. Scope
### 3.1 In scope
- Renomear projeto e pacotes para `poc-rss-collector` / `com.example.rsscollector`.
- Atualizar referências de integração para `poc-dashboard`.

### 3.2 Out of scope
- Mudanças funcionais.

## 4. Requirements
### 4.1 Functional
- FR-01: Artefato Maven `poc-rss-collector`.
- FR-02: Docs e comentários do pom citam `poc-dashboard`.

### 4.2 Non-functional
- —

## 5. Architecture and Design
Refatoração mecânica de nomes, sem mudança de comportamento.

## 6. Implementation Plan
Já implementado. Principais pontos no código:
- `pom.xml`
- pacote `com.example.rsscollector`
- README.md

## 7. Acceptance Criteria
- AC-01: `mvn test` verde após a renomeação.
- AC-02: Nenhuma referência ao nome antigo do consumidor no README/pom.

## 8. Observability and Operations
- Sem requisitos específicos além dos logs padrão da aplicação.

## 9. Risks and Mitigations
- —

## 10. Rollback
Reverter os commits/PRs listados nas referências.

## 11. References
- commits 896ebbd, 97cb813, d6857aa
