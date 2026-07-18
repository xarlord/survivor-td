#!/usr/bin/env bash
set -euo pipefail

WORKFLOW="${1:-.github/workflows/auto-merge.yml}"

fail() {
  printf 'FAIL: %s\n' "$1" >&2
  exit 1
}

assert_eq() {
  local expected="$1"
  local actual="$2"
  local description="$3"
  if [[ "$actual" != "$expected" ]]; then
    printf 'expected: %s\nactual:   %s\n' "$expected" "$actual" >&2
    fail "$description"
  fi
}

[[ -f "$WORKFLOW" ]] || fail "workflow not found: $WORKFLOW"
command -v jq >/dev/null 2>&1 || fail "jq is required"

PASSING_NDJSON=$'{"name":"build-and-test","status":"completed","conclusion":"SUCCESS"}\n{"name":"lint","status":"completed","conclusion":"SUCCESS"}\n{"name":"TDD","status":"completed","conclusion":"SUCCESS"}'
FAILURE_NDJSON=$'{"name":"build-and-test","status":"completed","conclusion":"FAILURE"}\n{"name":"lint","status":"completed","conclusion":"SUCCESS"}'
PENDING_NDJSON=$'{"name":"build-and-test","status":"in_progress","conclusion":null}\n{"name":"lint","status":"completed","conclusion":"SUCCESS"}'
NON_OBJECT_JSON=$'null\n"not a check run"\n42\n[]'
INVALID_JSON=$'{"name":"build-and-test"'

# RED evidence: the pre-fix parser receives NDJSON objects one at a time and
# fails because .[] treats object values as strings before .conclusion.
old_parser_error="$(mktemp)"
trap 'rm -f "$old_parser_error"' EXIT
if printf '%s\n' "$PASSING_NDJSON" | jq -r '[.[] | select(.conclusion == "FAILURE")] | length' >/dev/null 2>"$old_parser_error"; then
  fail "the pre-fix parser unexpectedly accepted passing NDJSON"
fi
if ! grep -Fq 'Cannot index string with string' "$old_parser_error"; then
  printf 'pre-fix parser stderr:\n' >&2
  cat "$old_parser_error" >&2
  fail "the pre-fix parser failed for an unexpected reason"
fi

# The workflow must frame CHECKS as an array before applying either count.
if ! failure_line="$(grep -F 'select(type == "object" and .conclusion == "FAILURE")' "$WORKFLOW")"; then
  fail "workflow has no type-safe failure parser"
fi
if ! pending_line="$(grep -F 'select(type == "object" and .status != "completed")' "$WORKFLOW")"; then
  fail "workflow has no type-safe pending parser"
fi
for parser_line in "$failure_line" "$pending_line"; do
  [[ "$parser_line" == *printf* && "$parser_line" == *CHECKS* && "$parser_line" == *'jq -sc'* ]] || \
    fail "workflow parser does not slurp CHECKS NDJSON before filtering"
done

parse_failures() {
  printf '%s\n' "$1" | jq -sc '[.[] | select(type == "object" and .conclusion == "FAILURE")]'
}

parse_pending() {
  printf '%s\n' "$1" | jq -sc '[.[] | select(type == "object" and .status != "completed")]'
}

assert_eq '[]' "$(parse_failures "$PASSING_NDJSON")" \
  'all passing checks produce no failures'
assert_eq '[{"name":"build-and-test","status":"completed","conclusion":"FAILURE"}]' \
  "$(parse_failures "$FAILURE_NDJSON")" 'a FAILURE check is detected'
assert_eq '[]' "$(parse_failures '')" 'empty check input produces no failures'
assert_eq '[]' "$(parse_failures "$NON_OBJECT_JSON")" \
  'non-object JSON values are not counted as failures'

assert_eq '[]' "$(parse_pending "$PASSING_NDJSON")" \
  'all completed checks produce no pending checks'
assert_eq '[{"name":"build-and-test","status":"in_progress","conclusion":null}]' \
  "$(parse_pending "$PENDING_NDJSON")" 'an incomplete check is detected'
assert_eq '[]' "$(parse_pending '')" 'empty check input produces no pending checks'
assert_eq '[]' "$(parse_pending "$NON_OBJECT_JSON")" \
  'non-object JSON values are not counted as pending'

invalid_error="$(mktemp)"
if parse_failures "$INVALID_JSON" >/dev/null 2>"$invalid_error"; then
  cat "$invalid_error" >&2
  rm -f "$invalid_error"
  fail 'malformed JSON was accepted instead of failing closed'
fi
[[ -s "$invalid_error" ]] || fail 'malformed JSON failed without a jq diagnostic'
rm -f "$invalid_error"

printf 'PASS: auto-merge check parser regression coverage\n'
