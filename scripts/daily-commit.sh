#!/usr/bin/env bash
# Run once after completing and reviewing real work. No automatic or backdated commits.
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"
dry=false
if [[ "${1:-}" == "--dry-run" ]]; then dry=true; shift; fi
if [[ $# -lt 2 ]]; then
  echo 'Usage: ./scripts/daily-commit.sh [--dry-run] DAY "Actual work completed" [paths to stage ...]'
  exit 1
fi
day="$1"; summary="$2"; shift 2
if [[ ! "$day" =~ ^([1-9]|0[1-9]|1[0-4])$ ]]; then echo 'DAY must be 1–14.'; exit 1; fi
if [[ ${#summary} -lt 12 ]]; then echo 'Describe the real work in at least 12 characters.'; exit 1; fi
printf -v number '%02d' "$((10#${day}))"
note="docs/journal/day-${number}.md"
if $dry; then
  printf 'Would run build/tests, write %s, and commit: day %s: %s\n' "$note" "$number" "$summary"
  printf 'Would stage journal plus these explicit paths: '; printf '%s ' "$@"; printf '\nNo files changed. No commit or push performed.\n'
  exit 0
fi
if [[ ! -d .git ]]; then echo 'Run git init here first, or use a cloned repository.'; exit 1; fi
if ! git diff --cached --quiet; then echo 'The index already contains staged work. Review and unstage it first; pass paths to this script explicitly.'; exit 1; fi
if [[ -e "$note" ]]; then echo "Day $number already has a journal entry. Choose the next day, or review the existing entry."; exit 1; fi
for path in "$@"; do
 if [[ "$path" == /* || "$path" == *..* || "$path" == -* ]]; then echo 'Use relative paths inside this project.'; exit 1; fi
done
./gradlew testDebugUnitTest assembleDebug --console=plain
mkdir -p docs/journal
printf '# Day %s — %s\n\n## Work completed\n%s\n\n## Validation\n- Packing rule tests and debug build passed.\n\n## Reflection\n- Add what you learned, manual checks, and each teammate’s contribution before your next commit.\n' "$number" "$(date +%Y-%m-%d)" "$summary" > "$note"
git add -- "$note"
if [[ $# -gt 0 ]]; then git add -- "$@"; fi
git diff --cached --stat
# Current real timestamp; does not change git author configuration or contact GitHub.
git commit -m "day $number: $summary"
echo 'Committed locally. Review with git show; run git push when ready.'
