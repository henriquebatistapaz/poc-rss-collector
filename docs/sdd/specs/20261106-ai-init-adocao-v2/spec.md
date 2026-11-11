# SDD: ai-init-adocao-v2

**Security-sensitive:** yes — segredos de CI, guardrails e dados enviados à revisão por IA

**Complexity:** M

## 1. Context
O ai-init evoluiu. Agora traz: guardrails (hooks que impedem o agente de editar secrets, lockfiles e migrations aplicadas, e de encerrar com testes vermelhos); doc-check, que mantém AGENTS.md e wiki verdadeiros; revisão por IA em todo MR/PR; ambiente de setup para agentes na nuvem; `ai-task` (issue com label vira PR a partir de spec aprovada); `/incident` sem supervisão para erros de produção; MCP somente leitura opcional; `@security`; e `/delegate`.

Stack deste projeto: Java 21, Maven, Spring Boot 3.4.
Observação específica: Ambiente do agente com o bloco maven.

## 2. Goal
Trazer este projeto para a versão atual do ai-init, aplicando só o que faz sentido para o stack e para a plataforma (GitHub), sem mudar código de produto.

## 3. Scope
### 3.1 In scope
- Rodar `/ai-init` no projeto (refresh) e responder as perguntas novas da entrevista.
- Guardrails: `.ai-init/hooks/guard.sh` e `guard.conf` com fatos verificados do projeto.
- Doc-check: `.ai-init/doc-check.sh` e o job de CI.
- Revisão por IA em PR (`ci-review`), começando só em modo relatório.
- Ambiente do agente (`.ai-init/agent-setup.sh`), quando houver build.
- Opcionais, só se o time quiser: `ai-task`, incident automation, MCP somente leitura.

### 3.2 Out of scope
- Mudanças em código de produto ou em migrations já aplicadas.
- Job de CI em modo bloqueante antes de uma rodada de relatórios.
- Criar secrets ou labels no GitHub: ações de admin, listadas no relatório.

## 4. Requirements
- RF-01: `.ai-init.json` registra a versão atual do ai-init.
- RF-02: `guard.conf` contém só comandos verificados no projeto; o que não se souber fica sem linha.
- RF-03: doc-check passa no próprio AGENTS.md antes do MR.
- RF-04: o ambiente do agente termina com "Agent environment ready." num checkout limpo (quando aplicável).

## 5. Architecture and Design
- Os arquivos gerados ficam em `ai-init-internal/<projeto>/` e são ligados ao projeto por `link.sh`. O `docs/` continua versionado no próprio projeto.
- Hooks em `.ai-init/hooks/`. Comandos nativos em `.claude/` e `.cursor/` são wrappers para `.github/`.
- Guardrails valem para Copilot só por `pre-commit` e CI; Claude Code e Cursor ganham hooks de edição e de encerramento.

## 6. Implementation Plan
1. Atualizar o clone do ai-init (`git pull` em `~/dev/ai-init`).
2. No editor, rodar `/ai-init` neste projeto e responder as perguntas 11 a 17 da entrevista.
3. Revisar o `guard.conf` e o AGENTS.md gerados; rodar `bash .ai-init/doc-check.sh`.
4. Rodar `bash .ai-init/agent-setup.sh` num checkout limpo (quando aplicável).
5. Abrir o PR com os arquivos versionados no projeto.

## 7. Acceptance Criteria
- AC-01: `.ai-init.json` aponta para a versão atual do ai-init.
- AC-02: `bash .ai-init/doc-check.sh` termina em OK, ou só com avisos documentados.
- AC-03: `guard.sh pre-edit` bloqueia uma edição em um `.env` de teste (exit 2).
- AC-04: nenhum `{placeholder}` restou no AGENTS.md gerado.
- AC-05: nenhuma credencial ou connection string real foi escrita em arquivo versionado.

## 8. Observability and Operations
- Logs da revisão por IA e do doc-check aparecem no próprio CI.
- Revisar o relatório da revisão por IA por 2 ciclos antes de torná-la bloqueante.

## 9. Risks and Mitigations
- Falso positivo do guardrail travar o agente: mitigado pelo `guard.conf` por projeto e pelo limite de tentativas do Stop hook.
- Custo de tokens na revisão por IA: modelo do tier `standard` e um comentário atualizado por push.

### 9.1 Ameaças

| Questão | Ameaça | Mitigação | Verificação |
|---|---|---|---|
| Segredos de CI | Token administrativo ou chave de provedor exposto em log, workflow ou arquivo versionado. | Segredos só no ambiente do GitHub; workflows não ecoam variáveis; nenhum valor em arquivo versionado. | AC-05 |
| Guardrails | Bypass do hook (edição de `.env`, lockfile ou migration aplicada) ou falso negativo. | `guard.conf` só com comandos verificados; teste de bloqueio do `.env`. | AC-03 |
| Dados enviados à IA | Código ou dados sensíveis enviados ao provedor pela revisão por IA em PR. | Revisão em modo relatório; sem dados reais nos diffs revisados. | AC-05 |
| Permissões do workflow | Permissão ampla ou secrets disponíveis em PR de fork. | Permissões mínimas no YAML; secrets não expostos a PR de fork. | Revisão do workflow |

## 10. Rollback
- Remover os hooks do `.claude/settings.json` e do `.cursor/hooks.json` e apagar o job de CI. Os arquivos do ai-init continuam no `ai-init-internal`.

## 11. References
- ai-init (develop): assets/guardrails, assets/automation, assets/templates/harness, assets/templates/agent-env, assets/templates/mcp.
