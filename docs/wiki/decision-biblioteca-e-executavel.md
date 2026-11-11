# Decisão: biblioteca e executável no mesmo build

> `mvn package` gera o jar comum (dependência do `poc-dashboard`) e o executável Spring Boot com classifier `exec`.

## Decisions and trade-offs

- Repackage do Spring Boot com `classifier=exec` → manter o jar comum consumível como biblioteca. Alternativa rejeitada: separar em dois repos (custo de manutenção alto para um projeto mini).

## Related

- `docs/sdd/jar-biblioteca/spec.md`
