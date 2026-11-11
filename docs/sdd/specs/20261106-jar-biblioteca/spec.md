# SDD: jar-biblioteca

**Complexity:** S

> SDD retroativa: documenta o que já foi entregue (commit ec45e7c (build: publish plain jar as library)). Escrita em 2026-11-06 a partir do código e do histórico do repo; status `Done`.

## 1. Context
O hub de coleta (hoje `poc-dashboard`) passou a reaproveitar o parser como dependência Maven.

## 2. Goal
Permitir que outro projeto use o código como biblioteca sem perder o executável Spring Boot.

## 3. Scope
### 3.1 In scope
- Repackage do Spring Boot com classifier `exec`.
- README explicando os dois jars e o `mvn install`.

### 3.2 Out of scope
- Publicação em repositório Maven remoto.

## 4. Requirements
### 4.1 Functional
- FR-01: `mvn package` gera o jar comum e o `-exec`.
- FR-02: `mvn install` instala a versão local para o consumidor.

### 4.2 Non-functional
- —

## 5. Architecture and Design
Configuração do `spring-boot-maven-plugin` com `<classifier>exec</classifier>`.

## 6. Implementation Plan
Já implementado. Principais pontos no código:
- `pom.xml` (spring-boot-maven-plugin)

## 7. Acceptance Criteria
- AC-01: `mvn package` produz os dois artefatos em `target/`.
- AC-02: O `poc-dashboard` compila contra o jar comum instalado localmente.

## 8. Observability and Operations
- Sem requisitos específicos além dos logs padrão da aplicação.

## 9. Risks and Mitigations
- —

## 10. Rollback
Reverter os commits/PRs listados nas referências.

## 11. References
- commit ec45e7c (build: publish plain jar as library)
