# TODO

## Next small slices

- [ ] Add parser models and RSS 2.0 parsing with focused JUnit tests.
- [ ] Add Atom 1.0 parsing with fixtures and JUnit tests.
- [ ] Implement link normalization and duplicate identity rules.
- [ ] Add safe feed URL validation and SSRF-focused tests before enabling network fetches.
- [ ] Decide whether the first API slice needs persistence.
- [ ] Add feed registration and refresh endpoints after parser contracts stabilize.
- [ ] Revisit ActiveMQ Classic only if refresh jobs need reliable asynchronous delivery; consider Kafka only for replay or multiple consumers.
