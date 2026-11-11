---
name: inspect-rest-api
description: Discover the real contract of a REST API (OpenAPI/Swagger, auth, status codes, payload shapes) before writing a client, integration or consumer code. Use whenever a task calls an internal or external HTTP API whose contract is not already known from the code.
---

# Inspect a REST API

Write clients against the discovered contract, not against what the endpoint "probably" returns.

## 0. A read-only MCP server first (optional)

If the project's MCP config (`.mcp.json`, `.vscode/mcp.json`, `.cursor/mcp.json`) has a read-only server for this system and its tools are available in this session, use it for the same questions below. Not configured or not available → continue with step 1, unchanged.

## 1. Find the spec first (cheapest path)

Try the standard spec locations before probing endpoints:

```bash
BASE=<api-base-url>
curl -s $BASE/v3/api-docs            # springdoc default
curl -s $BASE/openapi.json           # common
curl -s $BASE/swagger.json           # legacy
# Swagger UI (human view): $BASE/swagger-ui.html or /doc.html (springdoc custom path)
```

Internal service? Check its repo for the controller code or a committed OpenAPI file — the spec in the repo may lag the deployed one; when they disagree, the deployed `api-docs` wins for integration purposes (and flag the drift).

## 2. No spec? Probe carefully

```bash
# Auth model first: what does an unauthenticated call return? (401 vs 403 vs 200)
curl -s -o /dev/null -w "%{http_code}\n" $BASE/<endpoint>

# Real response shape (with auth from env — never inline secrets)
curl -s -H "Authorization: Bearer $API_TOKEN" $BASE/<endpoint> | head -c 2000

# Error shape too — clients must parse errors, and error bodies are rarely documented
curl -s -H "Authorization: Bearer $API_TOKEN" "$BASE/<endpoint>/nonexistent-id-999999"
```

Record per endpoint: method, auth, status codes seen, response shape (happy AND error), pagination style, rate-limit headers.

## 3. Use the facts

- Generate/write the client from the discovered contract; model error responses explicitly, not just the happy path.
- Note versioning (URL path? header?) and target the versioned form.
- Timeouts and retries are part of the client contract — check rate-limit headers (`429`, `Retry-After`) discovered above; retries only on idempotent methods.
- If integrating two internal services: the contract change process is an SDD, not an ad-hoc field addition.

## Safety rules

- Read-only probing: GET/HEAD/OPTIONS only. Never POST/PUT/DELETE against an API during discovery — even "test" writes can trigger real side effects (emails, billing, events).
- Credentials from env/config only; never paste tokens into files, specs or commits.
- Third-party APIs: respect rate limits while probing (a handful of calls, not a crawl); production internal APIs only with explicit user confirmation.
