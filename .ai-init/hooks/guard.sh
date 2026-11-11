#!/usr/bin/env bash
# ai-init guardrails — rules the agent cannot skip.
#
# One script, wired into every place an agent (or a human) can slip:
#   pre-edit   before an agent edits a file      → blocks secrets, lockfiles, applied migrations
#   pre-bash   before an agent runs a command    → blocks --no-verify, force push, rm -rf /
#   post-edit  after an agent edits a file       → runs the fast check, feeds errors back
#   stop       before an agent says "done"       → runs VERIFY; a red result sends it back to work
#   pre-commit git hook (any editor, any agent)  → protected / immutable paths on staged files
#   ci         CI job on every MR/PR             → same path rules against the target branch
#   sensitive  CI helper                         → lists changed files in SENSITIVE areas
#   install-git-hook                             → installs the pre-commit hook (once per clone)
#
# Agent protocol: Claude Code by default (block = exit 2 + stderr); add --cursor
# for Cursor's JSON protocol. Project settings live in guard.conf next to this file.
#
# Managed by ai-init — edit guard.conf, not this file (update.sh syncs this one).
set -uo pipefail

EVENT="${1:-}"; shift || true
FORMAT=claude; BASE=""
while [ $# -gt 0 ]; do
  case "$1" in
    --cursor) FORMAT=cursor ;;
    --base) BASE="${2:-}"; shift ;;
  esac
  shift
done

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
# Prefer the caller's checkout: .ai-init is centrally symlinked, so resolving HERE
# physically would otherwise select the ai-init-internal repository instead.
ROOT="$(git rev-parse --show-toplevel 2>/dev/null || git -C "$HERE" rev-parse --show-toplevel 2>/dev/null || (cd "$HERE/../.." && pwd -P))"
ROOT="$(cd "$ROOT" && pwd -P)"
cd "$ROOT" || exit 0

# ---- defaults (guard.conf overrides) --------------------------------------
PROTECTED=(".env" ".env.*" "*/.env" "*/.env.*" "*.pem" "*.key" "*.p12" "*.pfx" "*.jks" "*.keystore" "*id_rsa*" "*id_ed25519*" "*/secrets/*" "secrets/*")
ALLOWED=("*.example" "*.sample" "*.template" "*.dist")
IMMUTABLE=()
GENERATED_ONLY=("package-lock.json" "*/package-lock.json" "yarn.lock" "*/yarn.lock" "pnpm-lock.yaml" "*/pnpm-lock.yaml" "poetry.lock" "*/poetry.lock" "uv.lock" "*/uv.lock" "Pipfile.lock" "Cargo.lock" "go.sum" "Gemfile.lock" "composer.lock")
SENSITIVE=()
FAST_CHECK=""
FAST_CHECK_GLOBS=("*")
FAST_CHECK_TIMEOUT=90
VERIFY=""
VERIFY_TIMEOUT=600
MAX_STOP_BLOCKS=3
BLOCKED_COMMANDS=('--no-verify' 'push( .*)? (--force|-f)( |$)' 'push( .*)? \+[^ ]+' 'rm -[a-zA-Z]*r[a-zA-Z]*f?[a-zA-Z]* +(/|~|\$HOME)( |$)' 'git clean -[a-zA-Z]*f[a-zA-Z]*x')
# shellcheck source=/dev/null
[ -f "$HERE/guard.conf" ] && . "$HERE/guard.conf"

# ---- helpers ---------------------------------------------------------------
INPUT=""
read_input() { [ -t 0 ] || INPUT="$(cat)"; }

# json_get <jq-path>... : first non-empty value among the given paths.
json_get() {
  [ -n "$INPUT" ] || return 0
  local p v
  for p in "$@"; do
    if command -v jq >/dev/null 2>&1; then
      v="$(printf '%s' "$INPUT" | jq -r "$p // empty" 2>/dev/null)"
    elif command -v python3 >/dev/null 2>&1; then
      v="$(printf '%s' "$INPUT" | python3 -c 'import json,sys
d=json.load(sys.stdin)
for k in sys.argv[1].lstrip(".").split("."):
    d=d.get(k) if isinstance(d,dict) else None
print("" if d is None else (str(d).lower() if isinstance(d,bool) else d))' "$p" 2>/dev/null)"
    else
      local key="${p##*.}"
      v="$(printf '%s' "$INPUT" | tr '\n' ' ' | sed -n "s/.*\"$key\"[[:space:]]*:[[:space:]]*\"\\([^\"]*\\)\".*/\\1/p" | head -n1)"
      [ -n "$v" ] || v="$(printf '%s' "$INPUT" | tr '\n' ' ' | sed -n "s/.*\"$key\"[[:space:]]*:[[:space:]]*\\(true\\|false\\|[0-9][0-9]*\\).*/\\1/p" | head -n1)"
    fi
    [ -n "$v" ] && { printf '%s' "$v"; return 0; }
  done
}

json_str() {
  if command -v jq >/dev/null 2>&1; then printf '%s' "$1" | jq -Rs .
  elif command -v python3 >/dev/null 2>&1; then printf '%s' "$1" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))'
  else printf '"%s"' "$(printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' | awk 'BEGIN{ORS="\\n"} {print}')"
  fi
}

# matches <path> <glob>... — bash `case` globs; `*` also crosses `/`.
matches() {
  local path="$1" g; shift
  for g in "$@"; do
    # shellcheck disable=SC2254
    case "$path" in $g) return 0 ;; esac
    case "${path##*/}" in $g) return 0 ;; esac
  done
  return 1
}

relpath() {
  local p="$1" d
  case "$p" in
    /*) d="$(cd "$(dirname "$p")" 2>/dev/null && pwd -P)" && p="$d/$(basename "$p")"
        p="${p#"$ROOT"/}" ;;
  esac
  printf '%s' "${p#./}"
}

is_tracked() { git cat-file -e "HEAD:$1" 2>/dev/null; }

# path_violation <relpath> <mode: edit|change> — prints the reason, or nothing.
path_violation() {
  local f="$1" mode="$2"
  if matches "$f" ${PROTECTED[@]+"${PROTECTED[@]}"} && ! matches "$f" ${ALLOWED[@]+"${ALLOWED[@]}"}; then
    echo "$f is PROTECTED (secrets/credentials). Agents never edit it — ask the human, and follow the manage-secrets skill."
  elif [ "$mode" = edit ] && matches "$f" ${GENERATED_ONLY[@]+"${GENERATED_ONLY[@]}"}; then
    echo "$f is a lockfile — only the package manager writes it. Run the install/upgrade command instead of editing it."
  elif matches "$f" ${IMMUTABLE[@]+"${IMMUTABLE[@]}"} && is_tracked "$f"; then
    echo "$f is IMMUTABLE once committed (e.g. an applied migration). Create a NEW file instead (see the create-db-migration skill)."
  fi
}

run_with_timeout() {
  local secs="$1"; shift
  if command -v timeout >/dev/null 2>&1; then timeout "$secs" bash -c "$*"
  elif command -v gtimeout >/dev/null 2>&1; then gtimeout "$secs" bash -c "$*"
  elif command -v perl >/dev/null 2>&1; then perl -e 'alarm shift; exec @ARGV' "$secs" bash -c "$*"
  else bash -c "$*"
  fi
}

tail_out() { printf '%s\n' "$1" | tail -n 40; }

# block_agent <message> — refuse the action / send the agent back to work.
block_agent() {
  if [ "$FORMAT" = cursor ]; then
    case "$EVENT" in
      pre-bash) printf '{"permission":"deny","userMessage":%s,"agentMessage":%s}\n' "$(json_str "ai-init guard: blocked")" "$(json_str "$1")" ;;
      stop)     printf '{"followup_message":%s}\n' "$(json_str "$1")" ;;
      *)        printf '%s\n' "$1" >&2 ;;
    esac
    exit 0
  fi
  printf 'ai-init guard: %s\n' "$1" >&2
  exit 2
}

allow() {
  if [ "$FORMAT" = cursor ] && [ "$EVENT" = pre-bash ]; then printf '{"permission":"allow"}\n'; fi
  exit 0
}

STATE="$(git rev-parse --git-path ai-init-guard 2>/dev/null || echo "$HERE/.state")"
case "$STATE" in /*) ;; *) STATE="$ROOT/$STATE" ;; esac
state_get() { [ -f "$STATE" ] && sed -n "s/^$1=//p" "$STATE" | head -n1; }
state_set() { local k="$1" v="$2"; { [ -f "$STATE" ] && grep -v "^$k=" "$STATE"; echo "$k=$v"; } > "$STATE.tmp" 2>/dev/null; mv "$STATE.tmp" "$STATE"; }

worktree_hash() {
  {
    git diff HEAD 2>/dev/null || git diff 2>/dev/null
    git ls-files --others --exclude-standard -z 2>/dev/null | while IFS= read -r -d '' f; do
      printf '%s %s\n' "$f" "$(git hash-object "$f" 2>/dev/null)"
    done
  } | git hash-object --stdin
}

changed_files() {   # working tree vs HEAD, incl. untracked
  { git diff --name-only HEAD 2>/dev/null; git ls-files --others --exclude-standard 2>/dev/null; } | sort -u
}

# ---- events ----------------------------------------------------------------
case "$EVENT" in
  pre-edit)
    read_input
    f="$(json_get .tool_input.file_path .tool_input.notebook_path .file_path)"
    [ -n "$f" ] || allow
    reason="$(path_violation "$(relpath "$f")" edit)"
    [ -z "$reason" ] || block_agent "$reason"
    allow ;;

  pre-bash)
    read_input
    cmd="$(json_get .tool_input.command .command)"
    [ -n "$cmd" ] || allow
    for re in ${BLOCKED_COMMANDS[@]+"${BLOCKED_COMMANDS[@]}"}; do
      if printf '%s' "$cmd" | grep -Eq -- "$re"; then
        block_agent "command blocked by project policy (pattern: $re). It is irreversible or bypasses a safeguard — fix the cause instead, or ask the human to run it themselves."
      fi
    done
    allow ;;

  post-edit)
    read_input
    [ -n "$FAST_CHECK" ] || exit 0
    f="$(json_get .tool_input.file_path .file_path)"
    [ -n "$f" ] || exit 0
    rel="$(relpath "$f")"
    [ -f "$rel" ] || exit 0
    matches "$rel" ${FAST_CHECK_GLOBS[@]+"${FAST_CHECK_GLOBS[@]}"} || exit 0
    q="'${rel//\'/\'\\\'\'}'"
    out="$(run_with_timeout "$FAST_CHECK_TIMEOUT" "${FAST_CHECK//\{file\}/$q}" 2>&1)" && exit 0
    [ "$FORMAT" = cursor ] && exit 0   # Cursor ignores afterFileEdit output; the stop check catches it
    block_agent "fast check failed for $rel — fix it before moving on:
$(tail_out "$out")" ;;

  stop)
    read_input
    files="$(changed_files)"
    [ -n "$files" ] || exit 0
    h="$(worktree_hash)"
    [ "$(state_get pass)" = "$h" ] && exit 0
    # First stop of a turn (not one we already bounced) starts the count again.
    again="$(json_get .stop_hook_active .loop_count)"
    if [ -z "$again" ] || [ "$again" = false ] || [ "$again" = 0 ]; then n=1; else n="$(state_get fails)"; n=$(( ${n:-0} + 1 )); fi
    if [ "$n" -gt "$MAX_STOP_BLOCKS" ]; then
      # Loop guard — never trap the agent forever; hand it to the human.
      [ "$FORMAT" = claude ] && printf '{"systemMessage":%s}\n' "$(json_str "ai-init guard: still failing after $MAX_STOP_BLOCKS attempts — stopping the loop; needs a human look.")"
      exit 0
    fi
    problems=""
    while IFS= read -r f; do
      [ -n "$f" ] || continue
      r="$(path_violation "$f" change)"; [ -z "$r" ] || problems="$problems
- $r"
    done <<EOF
$files
EOF
    if [ -z "$problems" ] && [ -n "$VERIFY" ]; then
      out="$(run_with_timeout "$VERIFY_TIMEOUT" "$VERIFY" 2>&1)" || problems="
- verification failed (\`$VERIFY\`):
$(tail_out "$out")"
    fi
    if [ -z "$problems" ]; then state_set pass "$h"; state_set fails 0; exit 0; fi
    state_set fails "$n"
    block_agent "you are not done yet — the project guardrails found problems (attempt $n/$MAX_STOP_BLOCKS):$problems

Fix the cause (do not weaken tests or checks), then finish again." ;;

  pre-commit)
    problems=""
    while IFS= read -r line; do
      [ -n "$line" ] || continue
      st="${line%%	*}"; f="${line#*	}"
      r=""
      case "$st" in
        A) r="$(path_violation "$f" new)" ;;                        # new files: only PROTECTED applies
        D) matches "$f" ${IMMUTABLE[@]+"${IMMUTABLE[@]}"} && is_tracked "$f" \
             && r="$f is IMMUTABLE once committed — it cannot be deleted either." ;;
        *) r="$(path_violation "$f" change)" ;;
      esac
      [ -z "$r" ] || problems="$problems
  - $r"
    done <<EOF
$(git diff --cached --name-status --diff-filter=ACMRD | awk -F'\t' '{print substr($1,1,1) "\t" $NF}')
EOF
    [ -z "$problems" ] && exit 0
    printf 'ai-init guard — commit refused:%s\n\nUnstage those files (git restore --staged <file>). A real exception goes through review: change .ai-init/hooks/guard.conf in its own MR.\n' "$problems" >&2
    exit 1 ;;

  ci)
    [ -n "$BASE" ] || { echo "usage: guard.sh ci --base <ref>" >&2; exit 64; }
    problems=""
    while IFS= read -r line; do
      [ -n "$line" ] || continue
      st="${line%%	*}"; f="${line#*	}"
      if [ "$st" != D ] && matches "$f" ${PROTECTED[@]+"${PROTECTED[@]}"} && ! matches "$f" ${ALLOWED[@]+"${ALLOWED[@]}"}; then
        problems="$problems
- \`$f\` — protected path (secrets/credentials) committed"
      fi
      if [ "$st" != A ] && matches "$f" ${IMMUTABLE[@]+"${IMMUTABLE[@]}"} && git cat-file -e "$BASE:$f" 2>/dev/null; then
        problems="$problems
- \`$f\` — immutable file already on the target branch was modified or removed (add a new one instead)"
      fi
    done <<EOF
$(git diff --name-status --diff-filter=ACMRD "$BASE...HEAD" | awk -F'\t' '{print substr($1,1,1) "\t" $NF}')
EOF
    if [ -n "$problems" ]; then printf '### ai-init guard: path rules violated\n%s\n' "$problems"; exit 1; fi
    echo "ai-init guard: path rules OK"; exit 0 ;;

  sensitive)
    [ -n "$BASE" ] || { echo "usage: guard.sh sensitive --base <ref>" >&2; exit 64; }
    git diff --name-only "$BASE...HEAD" | while IFS= read -r f; do
      matches "$f" ${SENSITIVE[@]+"${SENSITIVE[@]}"} && echo "$f"
    done
    exit 0 ;;

  install-git-hook)
    hook="$(git rev-parse --git-path hooks/pre-commit)"
    case "$hook" in /*) ;; *) hook="$ROOT/$hook" ;; esac
    line='bash "$(git rev-parse --show-toplevel)/.ai-init/hooks/guard.sh" pre-commit || exit 1'
    if [ -f "$hook" ] && grep -q 'ai-init/hooks/guard.sh' "$hook"; then echo "pre-commit hook already installed: $hook"; exit 0; fi
    if [ -f "$hook" ]; then
      printf 'A pre-commit hook already exists (%s) — not overwriting it.\nAdd this line to it (or to your husky / pre-commit framework config):\n  %s\n' "$hook" "$line"
      exit 0
    fi
    mkdir -p "$(dirname "$hook")"
    printf '#!/usr/bin/env bash\n# installed by ai-init (guard.sh install-git-hook)\n%s\n' "$line" > "$hook"
    chmod +x "$hook"
    echo "pre-commit hook installed: $hook"; exit 0 ;;

  *)
    sed -n '2,16p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
    exit 64 ;;
esac
