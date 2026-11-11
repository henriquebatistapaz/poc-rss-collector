---
description: "Ship a validated implementation: branch, commits, push and a Merge/Pull Request described from the spec"
---

# Open MR/PR

Ship the implementation of: ${input:sdd:Path to the spec (docs/sdd/specs/<name>/spec.md), and issue number if any}

This step packages and proposes the change. Quality is decided by `/validate-implementation` — run it first.

Identify the platform (GitLab or GitHub) from `AGENTS.md` or `git remote -v` and use the matching CLI. "MR" and "PR" are the same thing here.

## 1. Confirm it's shippable

- Ask for (or check) the `/validate-implementation` verdict. No validation run yet, or verdict NOT READY → stop and point there. Do not re-run the full validation here; do confirm the build is currently green — never ship red.

## 2. Branch and commits

1. Branch named `feature/<issue-number>-<kebab-name>` (or `fix/...`). Work sitting on another branch (or on the default branch) → move it before anything else; never push to the protected branch.
2. Review what's staged: only the change's files — no stray debug prints, `.env`, IDE files or unrelated edits. Include the spec, test plan and `usage.log` from `docs/sdd/specs/<name>/`.
3. Commits with descriptive messages referencing the issue (`#<number>`). Squash noise ("wip", "fix typo") into meaningful commits when the history is messy.

## 3. Description and open

```bash
git push -u origin <branch>
glab mr create --title "<spec title> (#<issue>)" --description-file <generated-file>   # GitLab
gh pr create   --title "<spec title> (#<issue>)" --body-file <generated-file>          # GitHub
```

Generate the description from the spec:

- Summary — the spec's goal in 2-3 lines
- Link to the spec (`docs/sdd/specs/<name>/spec.md`)
- Acceptance Criteria checklist with status (from the validation report)
- How to test — commands + infrastructure prerequisites
- Notes for the reviewer: files with non-trivial decisions, anything deliberately out of scope
- Issue reference, taken from the spec's References section: `Closes #<id>` when the issue lives in THIS repo. If it lives in another project/instance, `Closes #<id>` does nothing — link the full URL instead (GitLab cross-project form: `Closes <group/project>#<id>`) and note that closing it may be manual.

No CLI available → present the description ready to paste, plus the exact `git push` command.

After the MR/PR exists, set the spec's status in `docs/sdd/INDEX.md` to **`Shipped`** (include that edit in the branch).

## Rules

- Never open an MR/PR with a red build or a NOT READY validation.
- Never push directly to the main branch.
- The MR description reports what IS — never claim a criterion is met because the spec says it should be.

## Usage log — LAST ACTION, ALWAYS

Append ONE line to `docs/sdd/specs/<name>/usage.log` (create if absent). **Append, never rewrite** — prior lines are history:

```bash
echo "$(date -Iseconds) | /open-mr | model: <model used in this chat> | in: ~<input tokens> | out: ~<output tokens>" >> docs/sdd/specs/<name>/usage.log
```

Estimates are fine — mark them with `~`. Commit this file with the branch, so the MR carries the feature's AI cost history.

## Next step

End your response with: **Next step →** wait for CI and human review (docs/ONBOARDING.md, Gate 2). CI red? Use the `debug-ci-failure` skill. After merge the issue closes automatically — `/next` shows what to pick up.
