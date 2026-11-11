# Domínio: feeds RSS e Atom

> Como um documento vira uma lista de `FeedItem(sourceFeed, id, title, link, publishedAt)`.

## Business rules

- Identidade do item: GUID (RSS) ou `id` (Atom); sem identificador, o link normalizado é a identidade.
- Atom exige `id`, `title` e `updated` válido no feed.
- Em entradas Atom, vale o link alternativo HTML; links relativos são resolvidos contra a URL do feed.
- Item sem link HTTP(S) válido é ignorado sem descartar os demais.

## Gotchas

- Deduplicação por `(feed, id)` e normalização avançada de links ainda são pendências (TODO.md).

## Related

- `docs/sdd/parser-rss-2/spec.md`, `docs/sdd/atom-e-validacao-por-url/spec.md`
