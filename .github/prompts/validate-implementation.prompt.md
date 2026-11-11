---
description: "Validate an implementation against its spec: acceptance criteria, green build, reviews and safety checks — the quality gate before shipping"
---

# Validate implementation

Validate the implementation of: ${input:sdd:Path to the spec (docs/sdd/specs/<name>/spec.md)}

This is the quality gate. It changes nothing and ships nothing — it produces a verdict. Shipping is `/open-mr`, which should only run after this passes.

## Checks (all blocking)

1. **Acceptance criteria** — read the spec and confirm EACH AC one by one, citing evidence (test name, endpoint, file/line). An AC without evidence is not met. Report as a checklist: AC → status → evidence.
2. **Build and tests** — run the build+test command from `AGENTS.md` for the affected modules. Report the actual result; a failing suite ends the validation here.
3. **Test plan coverage** — if `docs/sdd/specs/<name>/test-plan.md` exists, check the planned tests exist and run. List gaps.
4. **Code review** — apply the `reviewer` agent's criteria (`.github/agents/reviewer.agent.md`).
5. **Design review** — for bug fixes and design-heavy changes, apply the `architect` agent's criteria: root cause vs symptom, complexity, design fit.
6. **Test quality** — apply the `qa` agent's implementation lens: does every AC have a test asserting behavior (not just "no exception")? Negative scenarios covered?
7. **Doc drift** — does the diff contradict `docs/wiki/` or `AGENTS.md`? Those updates are part of the change.
8. **Safety** — inspect the diff for secrets/credentials (`git diff origin/<base>... --stat`, then the changed config files), and for dependencies added but not foreseen in the spec.
9. **Spec fidelity** — decisions that diverged from the spec must already be reflected IN the spec; if not, update it now and flag the change.
10. **Mobile first (quando a entrega mexe em tela)** — se o diff toca front-end, HTML, CSS ou componentes de UI, verificar em largura de celular (375px) antes do veredito: sem rolagem horizontal na página, tabelas largas com rolagem interna, formulários em uma coluna, alvos de toque com pelo menos 40px. Evidência obrigatória: screenshot ou medição (ex.: `scrollWidth` do documento igual à largura da tela). Sem essa evidência a verificação não está feita; não basta dizer que "deve funcionar".

11. **Security review** — if the spec is **Security-sensitive: yes**, or the diff touches a `SENSITIVE` glob from `.ai-init/hooks/guard.conf` or code handling auth, authorization, personal data, payments, secrets, uploads or outbound URLs: apply the `security` agent's implementation review (`.github/agents/security.agent.md`), including the dependency audit when dependencies changed. Not sensitive → one line saying why it was skipped. A diff that is sensitive while its spec says `no` is itself a `[BLOCKER]` (the spec must be corrected, and its Threats filled).

## Output

- Checklist per check: PASS / FAIL with evidence — no bare assertions.
- Findings classified `[BLOCKER]` / `[IMPORTANT]` / `[SUGGESTION]`.
- Verdict: **READY TO SHIP** (no blockers, ACs met, build green) or **NOT READY** with the exact list of what must be resolved.
- **If the implementation was produced in this same session**, append the honesty note to the verdict: "Heads-up: I'm validating work from this same session — context bias makes me lean lenient toward my own code. For critical changes, run `@reviewer`/`@architect` (and `@security`, if sensitive) in a NEW chat for a clean-context second opinion before the MR."

## Rules

- Fix nothing during validation — report first; `/fix-findings` does the fixing, then this runs again fresh. A gate that fixes its own findings stops being a gate.
- Loop budget: at most 3 validation rounds on the same change. Still blocked on the 3rd → escalate to a human with what keeps failing (constitution §7).
- Never soften a check to reach READY. A validation that always passes is theater.

## Usage log — LAST ACTION, ALWAYS

Append ONE line to `docs/sdd/specs/<name>/usage.log` (create if absent). **Append, never rewrite** — prior lines are history:

```bash
echo "$(date -Iseconds) | /validate-implementation | model: <model used in this chat> | in: ~<input tokens> | out: ~<output tokens>" >> docs/sdd/specs/<name>/usage.log
```

Estimates are fine — mark them with `~`. Input = context sent (files read, history); output = text/code generated. Don't know the model? Ask the user once per session.

## Next step

**READY TO SHIP** → `/open-mr docs/sdd/specs/<name>/spec.md`. **NOT READY** → `/fix-findings` applies the blockers (say which of the `[IMPORTANT]` ones you also want), then re-run `/validate-implementation`.
