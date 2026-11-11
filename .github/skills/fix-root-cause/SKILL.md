---
name: fix-root-cause
description: Fix the cause of a bug, not its symptom. Use whenever fixing an exception (NPE, IndexOutOfBounds, ClassCast), wrong output, or any defect — BEFORE writing the fix. An exception is a signal that an earlier invariant broke; silencing the signal is not a fix.
---

# Fix the root cause, not the symptom

The classic failure: an NPE at line X → add `if (x != null)` at line X → exception gone, bug intact. The null came from somewhere; now it travels silently and breaks elsewhere — or worse, produces wrong data quietly. **The exception was doing you a favor.**

## 1. Reproduce first

Write the failing test that reproduces the defect BEFORE touching code. No reliable reproduction = you cannot know you fixed it.

## 2. Trace the signal to its origin

For an NPE (same logic for any bad-value defect): the question is not "where did it throw?" but **"where did the null enter the system?"** Walk backwards:

- Where was this value produced? (query returning no rows, map.get miss, uninitialized field, optional API response, deserialized JSON missing a key)
- At each step ask: **was null a legitimate value here, or was an invariant already broken?**
- Keep walking until you find the boundary where invalid data entered or the point where a "can't happen" assumption failed. That's the root. (Informal five-whys — usually 2-4 hops.)

## 3. Classify the root — each has a different correct fix

| Root cause | Correct fix | Symptom fix (refuse) |
|---|---|---|
| Invalid input crossed a boundary unvalidated | Validate at the boundary (controller/consumer/deserializer) — reject early with a clear error | null check deep inside |
| Object allowed to exist in invalid state | Enforce invariants at construction (required args, builder validation, `final` fields) — make invalid state unrepresentable | scattered defensive checks |
| Absence IS legitimate but the type lies | Make it explicit: `Optional<T>` / nullable annotations / distinct type — force every caller to decide | assume non-null and hope |
| Contract misunderstanding (API may return null/empty) | Handle at the CALL SITE of that contract, once, converting to your domain's representation | checks at every use site |
| Broken data already persisted | Fix the data (migration) AND the writer that produced it | tolerate it forever in read paths |

## 4. Fix at the root, assert the behavior

- The fix goes at the origin identified in step 3 — often a different file than the stack trace.
- The test asserts correct BEHAVIOR (right output, right rejection with right error), not merely "no exception thrown".
- Same root pattern elsewhere? `grep` for sibling call sites/producers — a root cause rarely has exactly one victim. Fix or flag them.
- If the invalid data came from another service/team: fix your boundary AND report upstream (issue) — your validation is containment, not resolution.

## Anti-patterns — refuse these as "the fix"

- `if (x != null)` at the throw site with no answer to "why was it null?"
- `catch (Exception e) { log/ignore }` — converts a loud failure into silent corruption
- Broadening a catch block until the error disappears
- Default values that mask absence (`""`, `0`, empty list) where absence means a broken invariant
- Retry as a fix for a deterministic failure
- In reviews (`@reviewer`/`@qa`): a diff that only adds defensive checks at the symptom site is the tell — ask "where does the null come from?"

## When a guard IS the right fix

Truly optional data (per the domain), external input at its designated validation boundary, or documented API contracts. The difference: you can state WHY absence is legitimate there. Write that reason in the test name or a comment — "defensive" is not a reason.
