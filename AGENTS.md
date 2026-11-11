# AGENTS.md — RSS Collector Mini

Guia operacional para agentes de IA (Copilot, Claude Code, Codex, Cursor etc.).
Contexto detalhado: @README.md, @SPEC.md, @TODO.md, @docs/ONBOARDING.md, @docs/sdd/constitution.md, @docs/wiki/index.md.

## O projeto em uma linha

Serviço Java 27 / Spring Boot 4.1.2 (Maven, módulo único, pacote raiz `com.example.rsscollector`) que valida URLs, cadastra feeds e extrai itens de RSS 2.0 e Atom 1.0, também publicado como biblioteca para o `poc-dashboard`; código em inglês, docs em pt-BR.

## Comandos essenciais

```bash
# Infraestrutura local
Nenhuma — sem banco, sem broker.

# Build completo (gera o jar comum, usado como biblioteca, e o executável com sufixo -exec)
mvn -DskipTests package

# Instalar no repositório Maven local (para o poc-dashboard enxergar a versão local)
mvn install

# Testes (sempre rodar antes de considerar uma tarefa concluída) — sem rede
mvn test # doc-check: run

# Teste externo opcional contra feeds reais (NASA, BBC)
RSS_EXTERNAL_TESTS=true mvn test

# Formatação / lint
Não configurado.

# Rodar localmente (Swagger em http://localhost:8080/swagger-ui/index.html)
mvn spring-boot:run
```

## Convenções obrigatórias

- Camadas por pacote: `api` (controllers REST, ex. `FeedValidationController`), `application` (parser, validadores, fetcher, serviço) e `domain` (records imutáveis, ex. `FeedItem`).
- Conteúdo de feed é entrada hostil: XML sem DTD e sem entidades externas; limites de documento (`MAX_DOCUMENT_CHARS`) e de itens (`MAX_ITEMS`) no `RssFeedParser`.
- Toda busca HTTP passa pelo `BoundedFeedHttpFetcher`: só HTTP(S) público, checagem de DNS antes de cada conexão e redirect, timeout de 5 s, até 3 redirects, resposta de no máximo 1 MB, sem cookies nem credenciais.
- Itens inválidos são descartados sem derrubar os válidos do mesmo documento.
- Testes JUnit 5 não acessam a rede: XML local e mocks; o smoke test real só roda com `RSS_EXTERNAL_TESTS=true`.
- O artefato precisa continuar utilizável como biblioteca: não quebre a API pública de `RssFeedParser`, `RssFeedValidator` e `RssFeedLinkValidator` sem combinar com o `poc-dashboard`.
- Versões de dependências ficam em `<properties>` no `pom.xml`.

## Project skills

Procedimentos recorrentes deste projeto em `.github/skills/`: `add-feed-parsing-rule`.

## Workflow (Spec-Driven Development)

Plataforma: GitHub (`gh` CLI). Branch de integração: `develop`; `main` recebe releases.

Toda spec nasce em `docs/sdd/specs/<YYYYMMDD>-<slug-kebab>/spec.md` (`<YYYYMMDD>` = data da criação, nunca renomeada; `<slug-kebab>` = nome curto) e é registrada no `docs/sdd/INDEX.md`. Specs antigas no formato `docs/sdd/<nome>/` ficaram como ponteiros de compatibilidade.

O trabalho entra por uma issue (`/spec-from-issue <url>`), um pedido direto, ou um achado de `/tech-debt`/`/retro`/`/metrics` (`/new-spec`) — bug trivial pula a spec (teste falhando → correção na raiz → validar).

A partir da spec, um funil só: revisão `@qa` + aprovação humana → `/test-plan` → `/implement-spec` → `/validate-implementation` (gate: ACs, build, `@reviewer`/`@qa`/`@architect`) → `/open-mr` (branch + PR) → CI → revisão humana → merge.

Veredito NOT READY (ou findings bloqueantes)? `/fix-findings` aplica — na raiz, mantendo a spec em sincronia — e o gate roda de novo. Agentes de revisão apontam; nunca editam.

Atalho: `/next` detecta o estado atual e executa o passo adequado, parando nos gates humanos.

Correções de bug: causa raiz, não sintoma — veja a skill `fix-root-cause` antes de escrever qualquer correção.

1. **Spec primeiro**: toda feature não trivial começa com uma SDD em `docs/sdd/<nome>/spec.md`, seguindo `docs/sdd/TEMPLATE.sdd.md`, registrada em `docs/sdd/INDEX.md`. O que já existia antes do ai-init está documentado em SDDs retroativas com status `Done`.
2. **Princípios**: respeite `docs/sdd/constitution.md` em qualquer plano ou implementação.
3. **Implementação**: siga o plano da SDD; valide contra os critérios de aceite antes de terminar (`/implement-spec`).
4. **Entrega**: `/validate-implementation` precisa retornar READY TO SHIP antes de `/open-mr`.

## Model routing (controle de custo)

Tiers disponíveis: use o seletor de modelos — rápido para edições rotineiras de SDD, padrão por padrão, raciocínio mais forte para design transversal ou sensível a segurança. Sem nomes fixos de modelo.

| Etapa | S | M | L |
|---|---|---|---|
| Escrita de spec / test plan | light | standard | standard |
| Implementação | standard | standard | **heavy** |
| Validação / reviews (@qa @reviewer @architect) | light | standard | **heavy** |
| Fix findings / causa raiz | standard | standard | **heavy** |
| Periódicos (/metrics /tech-debt /retro) | standard | standard | standard |

Escale por RACIOCÍNIO (design, causa raiz, contratos), não por volume; nunca revise abaixo do tier que implementou; na dúvida, um tier acima.

## Armadilhas (aprendidas do jeito difícil)

Problemas do projeto que já custaram uma correção. **Leia antes de planejar ou codar** — se a mudança tocar uma área listada aqui, siga a regra e diga isso no plano. Entradas são adicionadas pelo `/fix-findings` e pelo `/retro`.

<!-- Uma linha cada: - **<área>** — <o que quebra e por quê>. Regra: <o que fazer>. (fonte: <spec ou MR/PR>, <AAAA-MM-DD>) -->
_Nenhuma ainda._

## Não faça

- Não salve specs fora de `docs/sdd/` (a visão de produto em `SPEC.md`, se existir, continua na raiz).
- Não comite segredos ou credenciais (`.env` fica fora do git).
- Não introduza dependências sem justificar na SDD correspondente.
- Não considere uma tarefa concluída com testes falhando ou build quebrado.
- Não deixe `docs/wiki/`, `README.md` ou este arquivo desatualizados quando uma mudança alterar o que descrevem.
- Os arquivos do ai-init (`.ai-init.json`, `AGENTS.md`, `CLAUDE.md`, `.github/copilot-instructions.md`, `.github/prompts|agents|instructions|skills`) e os docs do ciclo (`docs/sdd`, `docs/wiki`, `docs/ONBOARDING.md`) são links para `../ai-init-internal/poc-rss-collector/` e NÃO são versionados neste repo. Edite normalmente; o commit vai no repo `ai-init-internal`.
- Não faça chamadas de rede reais na suíte padrão de testes.
- Não relaxe os limites de segurança do fetcher/parser sem SDD.
