---
name: add-feed-parsing-rule
description: Adicionar ou ajustar uma regra de parsing de feed RSS/Atom (novo campo, nova validação, novo formato de data) no poc-rss-collector.
---

# Adicionar uma regra de parsing de feed

1. Escreva primeiro o teste em `src/test/java/com/example/rsscollector/application/RssFeedParserTest.java` (ou `RssFeedValidatorTest`), com o XML do caso embutido no teste — nunca busque da rede.
2. Inclua o caso negativo: documento malformado, campo ausente, link inválido. O item ruim deve ser descartado sem derrubar os válidos.
3. Implemente em `RssFeedParser` (RSS e Atom têm caminhos separados); mantenha DTD/entidades externas desligadas e respeite `MAX_DOCUMENT_CHARS`/`MAX_ITEMS`.
4. Se o `FeedItem` mudar, avalie o impacto no `poc-dashboard` (consome o jar) antes de alterar o record.
5. Rode `mvn test`; se mexeu em comportamento visível, atualize README e `docs/wiki/dominio-feeds.md`.
