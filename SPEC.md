# RSS Link Collector Mini — Initial Specification

## Goal

Define a small Java service that can collect article and resource links published by user-registered RSS or Atom feeds. The initial product should be easy to run locally and keep feed discovery separate from downstream processing.

## First release scope

- Accept RSS 2.0 and Atom 1.0 feeds.
- Extract each entry's stable identifier, title, canonical link, publication time, and source feed.
- Normalize URLs and identify duplicates first by `(feed URL, feed item identifier)`, then by normalized article URL when an identifier is absent.
- Report malformed feeds and entries without discarding valid entries from the same document.
- Keep collection in memory for the first implementation slice; define persistence as a later step.
- Expose a small REST API only after parser behavior is covered by unit tests.

## Current implementation slice

- RSS 2.0 parsing is implemented for an XML document supplied by the caller; it does not fetch feeds over the network.
- Feed URL validation checks absolute HTTP(S) URL syntax and rejects credentials, local hostnames, and private IP literals.
- RSS document validation checks the URL and confirms an RSS 2.0 root with a channel.
- XML parsing disables DTDs and external entities and limits document size and item count.
- Atom parsing, live feed fetching, persistence, and API endpoints remain future work.

## Initial API direction (not implemented)

- `POST /v1/feeds` — register a feed URL.
- `GET /v1/feeds` — list registered feeds and their collection status.
- `POST /v1/feeds/{id}/refresh` — request a manual refresh.
- `GET /v1/links` — list collected links with optional feed and date filters.

These endpoints are a direction for a later implementation, not a commitment to a final API shape.

## Safety and operating limits

- Only allow `http` and `https` feed URLs.
- Reject loopback, private, link-local, multicast, and other non-public destinations, including after DNS resolution and redirects, to reduce server-side request forgery risk.
- Set connection and read timeouts, maximum redirect count, response size, and entries processed per feed.
- Never forward credentials or cookies to feed hosts.
- Treat feed content as untrusted input; disable external XML entities and DTD processing.
- Keep secrets and local environment files out of version control.

## Design decisions

- Start as a single Spring Boot Java application with small modules for feed registration, parsing, link normalization, and collection.
- Use in-memory storage for the first working slice; select a database only when persistence requirements are agreed.
- Do not introduce a message broker for the parser-only first slice. ActiveMQ Classic can be considered if refresh work needs reliable asynchronous delivery. Kafka is relevant only if replayable event history or multiple independent consumers become a real requirement.
- Keep the project private while it is being developed.

## Acceptance criteria for the first implementation slice

1. Unit tests cover RSS 2.0 and Atom 1.0 examples, duplicate identity, missing optional metadata, malformed entries, invalid URLs, and hostile XML features.
2. Parser output is deterministic and contains the source feed, stable identity, title when present, normalized link, and publication time when valid.
3. No network access or persistence is required to run parser tests.
4. Security limits for network fetching are specified before any fetcher is implemented.

## References

- [RSS 2.0 Specification](https://www.rssboard.org/rss-specification)
- [RFC 4287: The Atom Syndication Format](https://www.rfc-editor.org/rfc/rfc4287)
