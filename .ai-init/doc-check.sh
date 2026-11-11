#!/usr/bin/env bash
# ai-init doc-check — AGENTS.md and the wiki must describe the repo as it IS.
#
#   doc-check.sh          check paths + commands (fast, no side effects) — run in CI on every MR/PR
#   doc-check.sh --run    also EXECUTE the AGENTS.md commands marked `# doc-check: run`
#
# What it checks:
#   paths     every file/folder AGENTS.md, docs/wiki/ and docs/ONBOARDING.md point at — markdown
#             links, `inline/code/paths.ext` and @imports — must exist
#   commands  every command in AGENTS.md's ```bash blocks must be runnable here: the script/wrapper
#             exists, `npm run X` has script X, `make T` has target T, mvn/gradle/uv/poetry/compose
#             have their build file, `cd dir` points at a real dir; unknown tools are a warning
#   verify    guard.conf VERIFY (if any) still appears in AGENTS.md — one definition of "done"
#
# False positive? Add a glob to .ai-init/doc-check.ignore (one per line, matched against the path).
# Exit 1 on any error; warnings never fail. Managed by ai-init — synced by update.sh.
set -uo pipefail

RUN=0; [ "${1:-}" = --run ] && RUN=1
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
# Prefer the caller's checkout because .ai-init can be centrally symlinked.
ROOT="$(git rev-parse --show-toplevel 2>/dev/null || git -C "$HERE" rev-parse --show-toplevel 2>/dev/null || (cd "$HERE/.." && pwd -P))"
cd "$ROOT" || exit 2
AGENTS="AGENTS.md"
[ -f "$AGENTS" ] || { echo "doc-check: no AGENTS.md at $ROOT" >&2; exit 1; }

errors=0; warnings=0
err()  { errors=$((errors+1));   printf '  ✗ %s\n' "$1"; }
warn() { warnings=$((warnings+1)); printf '  ! %s\n' "$1"; }

IGNORE=()
if [ -f .ai-init/doc-check.ignore ]; then
  while IFS= read -r l; do case "$l" in ''|\#*) ;; *) IGNORE+=("$l") ;; esac; done < .ai-init/doc-check.ignore
fi
ignored() { local g; for g in ${IGNORE[@]+"${IGNORE[@]}"}; do
  # shellcheck disable=SC2254
  case "$1" in $g) return 0 ;; esac; done; return 1; }

# ---- 1. paths ---------------------------------------------------------------
# Prints "kind<TAB>target" for each reference outside fenced code blocks.
refs_of() {
  awk '
    /^[[:space:]]*(```|~~~)/ { fence = !fence; next }
    fence { next }
    {
      line = $0
      while (match(line, /\]\([^) ]+\)/)) {               # markdown links
        print "link\t" substr(line, RSTART + 2, RLENGTH - 3); line = substr(line, RSTART + RLENGTH)
      }
      line = $0
      while (match(line, /`[^`]+`/)) {                     # inline code
        print "code\t" substr(line, RSTART + 1, RLENGTH - 2); line = substr(line, RSTART + RLENGTH)
      }
      line = $0
      while (match(line, /(^|[[:space:],(])@[A-Za-z0-9._\/-]+\.md/)) { # @imports
        t = substr(line, RSTART, RLENGTH); sub(/^[^@]*@/, "", t); print "import\t" t
        line = substr(line, RSTART + RLENGTH)
      }
    }' "$1"
}

looks_like_path() {   # inline code that is plainly a repo path
  case "$1" in
    /*|~*|*' '*|*'*'*|*'<'*|*'{'*|*'$'*|*'='*|-*|*://*|*'@'*|'.'|'..'|*'|'*) return 1 ;;
  esac
  printf '%s' "$1" | grep -Eq '^[A-Za-z0-9._/-]+$' || return 1
  case "$1" in
    */*) return 0 ;;
    *.md|*.java|*.kt|*.py|*.ts|*.tsx|*.js|*.go|*.rs|*.rb|*.cs|*.yml|*.yaml|*.json|*.xml|*.toml|*.sh|*.ps1|*.sql|*.gradle|*.kts|*.properties|*.cfg|*.ini|Dockerfile|Makefile) return 0 ;;
  esac
  return 1
}

check_paths_in() {
  local file="$1" dir kind t p
  dir="$(dirname "$file")"
  while IFS="$(printf '\t')" read -r kind t; do
    case "$kind" in
      link)   case "$t" in http:*|https:*|mailto:*|\#*) continue ;; esac
              t="${t%%#*}"; t="${t%%\?*}"; [ -n "$t" ] || continue ;;
      code)   looks_like_path "$t" || continue ;;
      import) ;;
    esac
    t="${t%%:[0-9]*}"; t="${t%/}"
    ignored "$t" && continue
    # links resolve against the file's folder; code/@imports against the repo root, then the folder
    if [ "$kind" = link ]; then p="$dir/$t"; [ -e "$p" ] || p="$t"
    else p="$t"; [ -e "$p" ] || p="$dir/$t"; fi
    case "$t" in /*) p="$t" ;; esac
    [ -e "$p" ] || err "$file → \`$t\` does not exist"
  done <<EOF
$(refs_of "$file" | sort -u)
EOF
}

echo "Paths"
docs=("$AGENTS")
[ -f docs/ONBOARDING.md ] && docs+=(docs/ONBOARDING.md)
if [ -d docs/wiki ]; then
  while IFS= read -r f; do docs+=("$f"); done <<EOF
$(find docs/wiki -name '*.md' -type f | sort)
EOF
fi
before=$errors
for f in "${docs[@]}"; do check_paths_in "$f"; done
[ $errors -eq $before ] && echo "  ✓ every referenced path exists (${#docs[@]} files)"

# ---- 2. commands ------------------------------------------------------------
# Prints the command lines of AGENTS.md's bash/sh/shell fenced blocks.
command_lines() {
  awk '
    /^[[:space:]]*```/ {
      if (!fence) { fence = 1; lang = $0; sub(/^[[:space:]]*```/, "", lang); sh = (lang ~ /^(bash|sh|shell|zsh|console)?[[:space:]]*$/) }
      else fence = 0
      next
    }
    fence && sh { print }' "$AGENTS"
}

package_files() { find . -maxdepth 4 -path '*/node_modules' -prune -o -name package.json -type f -print 2>/dev/null; }
has_package_json() { [ -f package.json ] || [ -n "$(package_files | head -n1)" ]; }
has_pkg_script() {
  [ -f package.json ] && grep -Eq "\"$1\"[[:space:]]*:" package.json && return 0
  local f
  while IFS= read -r f; do grep -Eq "\"$1\"[[:space:]]*:" "$f" && return 0; done <<EOF
$(package_files)
EOF
  return 1
}
compose_file() { local f; for f in compose.yaml compose.yml docker-compose.yaml docker-compose.yml; do [ -f "$f" ] && return 0; done; return 1; }

check_segment() {   # one simple command (no && | ;)
  local seg="$1" w1 w2 w3 arg prev="" m
  # drop leading env assignments / sudo / time
  # shellcheck disable=SC2086
  set -- $seg
  while [ $# -gt 0 ]; do case "$1" in *=*|sudo|time|exec|command) shift ;; *) break ;; esac; done
  [ $# -gt 0 ] || return 0
  w1="$1"; w2="${2:-}"; w3="${3:-}"
  case "$w1" in
    cd)  [ -z "$w2" ] || [ -d "$w2" ] || case "$w2" in -|~*|\$*) ;; *) err "AGENTS.md: \`cd $w2\` — folder does not exist" ;; esac ;;
    ./*|bin/*|scripts/*|*/*) [ -e "$w1" ] || err "AGENTS.md: \`$w1\` does not exist" ;;
    bash|sh|zsh|python|python3|node|pwsh)
      for arg in "${@:2}"; do
        case "$arg" in -*) continue ;; *.sh|*.py|*.js|*.mjs|*.ps1) [ -e "$arg" ] || err "AGENTS.md: \`$w1 $arg\` — script does not exist"; break ;; *) break ;; esac
      done ;;
    npm|pnpm|yarn|bun)
      has_package_json || { err "AGENTS.md: \`$w1 …\` but there is no package.json"; return; }
      case "$w2" in
        run|run-script) [ -z "$w3" ] || has_pkg_script "$w3" || err "AGENTS.md: \`$w1 run $w3\` — no \"$w3\" script in package.json" ;;
        test|start)     has_pkg_script "$w2" || err "AGENTS.md: \`$w1 $w2\` — no \"$w2\" script in package.json" ;;
      esac ;;
    npx) has_package_json || warn "AGENTS.md: \`npx $w2\` without a package.json (downloads on every run)" ;;
    make) [ -f Makefile ] || [ -f makefile ] || [ -f GNUmakefile ] || { err "AGENTS.md: \`make\` but there is no Makefile"; return; }
          case "$w2" in ''|-*) ;; *) grep -Eq "^$w2[[:space:]]*:" Makefile makefile GNUmakefile 2>/dev/null || err "AGENTS.md: \`make $w2\` — no \"$w2\" target in the Makefile" ;; esac ;;
    mvn|mvnd) [ -f pom.xml ] || err "AGENTS.md: \`$w1\` but there is no pom.xml"
          for arg in "${@:2}"; do case "$prev" in -pl|--projects) for m in ${arg//,/ }; do [ -f "${m#:}/pom.xml" ] || [ "${m#:}" != "$m" ] || err "AGENTS.md: \`-pl $m\` — no $m/pom.xml"; done ;; esac; prev="$arg"; done ;;
    gradle) ls build.gradle build.gradle.kts settings.gradle settings.gradle.kts >/dev/null 2>&1 || err "AGENTS.md: \`gradle\` but there is no build.gradle(.kts)" ;;
    uv|poetry|pdm|hatch) [ -f pyproject.toml ] || err "AGENTS.md: \`$w1\` but there is no pyproject.toml" ;;
    pip|pip3) case "$seg" in *" -r "*) arg="${seg#* -r }"; arg="${arg%% *}"; [ -f "$arg" ] || err "AGENTS.md: \`pip -r $arg\` — file does not exist" ;; esac ;;
    docker-compose) compose_file || err "AGENTS.md: \`docker-compose\` but there is no compose file" ;;
    docker) if [ "$w2" = compose ]; then
              case "$seg" in *" -f "*) arg="${seg#* -f }"; arg="${arg%% *}"; [ -f "$arg" ] || err "AGENTS.md: \`docker compose -f $arg\` — file does not exist" ;;
                             *) compose_file || err "AGENTS.md: \`docker compose\` but there is no compose file" ;; esac
            fi ;;
    git|echo|printf|cat|ls|test|\[|true|false|export|source|.|exit) ;;
    *) command -v "$w1" >/dev/null 2>&1 || warn "AGENTS.md: \`$w1\` is not installed here (fine if CI/devs have it)" ;;
  esac
}

echo "Commands"
before=$errors; n=0; torun=()
while IFS= read -r line; do
  case "$line" in *'# doc-check: run'*) c="${line%%# doc-check: run*}"; c="${c%"${c##*[![:space:]]}"}"; torun+=("$c") ;; esac
  case "$line" in [[:space:]]*'#'*|'#'*) l2="${line#"${line%%[![:space:]]*}"}"; case "$l2" in '#'*) continue ;; esac ;; esac
  line="${line%% #*}"                                  # strip trailing comments
  line="$(printf '%s' "$line" | sed -e 's/^[[:space:]]*[$>][[:space:]]//' -e 's/[[:space:]]*$//')"
  [ -n "$line" ] || continue
  case "$line" in *'\') continue ;; esac               # continuation lines: checked by --run only
  n=$((n+1))
  while IFS= read -r seg; do
    seg="$(printf '%s' "$seg" | sed 's/^[[:space:]]*//')"; [ -n "$seg" ] && check_segment "$seg"
  done <<EOF
$(printf '%s\n' "$line" | awk '{ gsub(/&&|\|\||;|\|/, "\n"); print }')
EOF
done <<EOF
$(command_lines)
EOF
[ $errors -eq $before ] && echo "  ✓ $n command lines resolve to real scripts, targets and build files"

if [ -f .ai-init/hooks/guard.conf ]; then
  VERIFY=""
  # shellcheck source=/dev/null
  VERIFY="$( (. .ai-init/hooks/guard.conf >/dev/null 2>&1; printf '%s' "${VERIFY:-}") )"
  if [ -n "$VERIFY" ] && ! grep -Fq -- "$VERIFY" "$AGENTS"; then
    warn "guard.conf VERIFY (\`$VERIFY\`) is not in AGENTS.md — agents and the Stop hook use different definitions of done"
  fi
fi

# ---- 3. --run -----------------------------------------------------------------
if [ $RUN -eq 1 ]; then
  echo "Run (lines marked # doc-check: run)"
  if [ ${#torun[@]} -eq 0 ]; then warn "nothing marked \`# doc-check: run\` in AGENTS.md"; fi
  for c in ${torun[@]+"${torun[@]}"}; do
    printf '  $ %s\n' "$c"
    if out="$(bash -c "$c" 2>&1)"; then echo "    ✓ passed"
    else err "AGENTS.md command failed: \`$c\`"; printf '%s\n' "$out" | tail -n 15 | sed 's/^/      /'; fi
  done
fi

echo
if [ $errors -gt 0 ]; then
  echo "doc-check: $errors error(s), $warnings warning(s) — the docs describe a repo that doesn't exist."
  echo "Fix the doc (or the code) in this MR — doc drift is a bug. False positive: .ai-init/doc-check.ignore"
  exit 1
fi
echo "doc-check: OK ($warnings warning(s))"
