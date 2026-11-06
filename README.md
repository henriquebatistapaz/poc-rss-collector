# RSS Link Collector Mini

A private Java mini project for collecting and deduplicating links published in RSS and Atom feeds.

## Status

Specification and project bootstrap only. Feed fetching, parsing, persistence, and HTTP endpoints are not implemented yet.

## First implementation slice

The proposed first slice is a safe feed parser that accepts a supplied RSS 2.0 or Atom 1.0 document and returns normalized link records. Network fetching and persistence are intentionally deferred.

See [SPEC.md](SPEC.md) and [TODO.md](TODO.md) for scope and planned work.
