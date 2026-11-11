---
name: upgrade-dependency
description: Upgrade a dependency safely (library, framework or tool version bump). Use for any version change — routine bumps, CVE fixes, or major upgrades — in Maven, Gradle, pip/uv/poetry or npm projects.
---

# Upgrade a dependency

Version bumps look trivial and are a top source of subtle breakage. The procedure scales with risk.

## 1. Classify before touching anything

```bash
# What version, and what depends on it?
mvn dependency:tree -Dincludes=<group>:<artifact>     # Maven
pipdeptree -r -p <package>                            # Python
```

- **Patch/minor** (semver respected): low risk, standard procedure below.
- **Major, framework (Spring Boot, etc.), or anything transitively shared**: high risk — needs its own SDD with the breaking-changes list, not a drive-by bump.
- **CVE-driven**: check whether the vulnerable code path is even used; prefer the smallest version that fixes the CVE over jumping to latest.

## 2. Standard procedure

1. **Read the changelog/release notes** between current and target version — actually read them; list breaking changes and deprecations that touch this codebase (grep for the affected APIs).
2. One dependency (or one coherent group, e.g. a BOM) per MR — a 30-bump MR is unreviewable and unbisectable.
3. Bump in the right place: version property / BOM / lockfile per the project's convention — don't pin versions in child modules when a parent property exists.
4. Rebuild the lockfile where applicable (`uv lock`, `poetry lock`, `package-lock.json`) and commit it.
5. Run the FULL suite, not just affected-module tests — transitive breakage doesn't respect module boundaries. Include integration tests if the dependency touches I/O.
6. Grep for deprecation warnings introduced by the new version; fix now or file an issue — don't let them accumulate silently.

## 3. In the MR description

Current → target version, link to release notes, breaking changes considered (or "none affecting us because…"), CVE ids if security-driven.

## Safety rules

- Never upgrade + refactor + feature in one MR.
- Never silence a new warning/error from the upgrade with a suppression to "make it pass" — that's the breakage telling you where to look.
- Version downgrades to "fix" a conflict are a smell — resolve the conflict explicitly (dependencyManagement/constraints) and document why.
