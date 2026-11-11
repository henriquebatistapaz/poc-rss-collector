# TODO

## Implemented

- [x] Add parser models and RSS 2.0 and Atom 1.0 parsing with focused JUnit tests.
- [x] Add validation for public HTTP(S) URL syntax and supplied RSS/Atom XML.
- [x] Expose `POST /v1/feeds/validate` and Swagger/OpenAPI documentation.
- [x] Fetch a submitted public feed URL with timeout, response-size, redirect, and destination checks.
- [x] Record public RSS feed URLs for optional manual smoke checks.

## Next small slices

- [x] Add dedicated HTTP fetcher tests for redirects, size caps, DNS changes, and network failures.
- [x] Normalize article links by removing fragments and known tracking parameters while preserving functional query parameters; deduplicate by feed and item ID.
- [x] Keep registered feeds and collected links in bounded in-memory storage for this slice; durable persistence remains deferred.
- [x] Add feed registration, listing, manual refresh, and collected-link endpoints after parser contracts stabilized.
- [ ] Revisit ActiveMQ Classic only if refresh jobs need reliable asynchronous delivery; consider Kafka only for replay or multiple consumers.
