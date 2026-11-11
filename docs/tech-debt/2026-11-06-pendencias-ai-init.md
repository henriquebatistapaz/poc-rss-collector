# Pendências após a instalação do ai-init — 2026-11-06

Itens levantados na instalação/migração do ai-init para o `ai-init-internal`. Cada item vira SDD (`/new-spec`) ou tarefa quando for priorizado.

- [ ] Comandos de build/teste do `AGENTS.md` vieram do README, dos arquivos de build e dos scripts; ainda não foram executados para confirmar.
- [ ] Harness (fase 4 do ai-init) não configurado: CI com relatório de testes/cobertura, formatter, linter e piso de cobertura. Medir o estado atual antes de criar gates.
- [ ] Proteção da plataforma não verificada: branch protegida em `develop`/`main`, CI obrigatório, aprovação obrigatória, CODEOWNERS, secret scanning.
- [ ] Revisar as skills específicas do projeto em `.github/skills/` e a wiki gerada (`docs/wiki/`) contra o código.
- [ ] SDDs retroativas foram escritas a partir do código e do histórico; revisar se os critérios de aceite batem com o comportamento real.
