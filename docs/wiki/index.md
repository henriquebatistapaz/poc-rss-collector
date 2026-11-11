# RSS Collector Mini — Knowledge Wiki

Base de conhecimento curada para humanos e agentes. Fatos estruturais estão no código; esta wiki guarda o que o código não expressa: **por que** as decisões foram tomadas, regras de negócio, fluxos e pegadinhas.

Segue o [padrão LLM-wiki do Karpathy](https://gist.github.com/karpathy/442a6bf555914893e9891c11519de94f): este índice liga todas as páginas por wikilinks, agrupadas por categoria.

## Architecture

- [[architecture-overview]] — visão geral e fluxo principal
- (nenhuma página ainda)

## Domains

- [[dominio-feeds]] — parsing RSS/Atom e regras de identidade dos itens

## Decisions

- [[decision-biblioteca-e-executavel]] — por que o build gera dois jars

## Operations

- (nenhuma página ainda)

---

## Rules for this wiki

- Uma página por tópico, nome em kebab-case igual ao wikilink.
- Toda página listada aqui — página fora do índice não existe.
- **Doc drift é bug**: se uma mudança deixa uma página errada, atualizar a página faz parte da mudança.
- Prefira diagramas mermaid a prosa para fluxos e dependências.
