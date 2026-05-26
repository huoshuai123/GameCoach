#!/usr/bin/env zsh
set -euo pipefail

SYMPHONY_DIR="/Users/bytedance/Documents/Codex/symphony/elixir"
WORKFLOW_FILE="/Users/bytedance/Documents/Codex/2026-05-23/ai-1-android-2-3-4/WORKFLOW.md"
LOGS_ROOT="/Users/bytedance/Documents/Codex/symphony-logs/mahjongcoach"
PORT="${SYMPHONY_PORT:-4000}"

if [[ -z "${LINEAR_API_KEY:-}" ]]; then
  echo "LINEAR_API_KEY is not set."
  echo "Create a Linear personal API key, then run:"
  echo "  export LINEAR_API_KEY=..."
  exit 1
fi

if [[ -z "${LINEAR_PROJECT_SLUG:-}" ]]; then
  echo "LINEAR_PROJECT_SLUG is not set."
  echo "Open your Linear project URL and use the last path segment, then run:"
  echo "  export LINEAR_PROJECT_SLUG=..."
  exit 1
fi

mkdir -p "$LOGS_ROOT"
RUNTIME_WORKFLOW="$LOGS_ROOT/WORKFLOW.runtime.md"

if [[ "$LINEAR_PROJECT_SLUG" == *\"* ]]; then
  echo "LINEAR_PROJECT_SLUG must not contain double quotes."
  exit 1
fi

sed "s/project_slug: \\\$LINEAR_PROJECT_SLUG/project_slug: \"${LINEAR_PROJECT_SLUG}\"/" \
  "$WORKFLOW_FILE" > "$RUNTIME_WORKFLOW"

cd "$SYMPHONY_DIR"

exec mise exec -- ./bin/symphony \
  --i-understand-that-this-will-be-running-without-the-usual-guardrails \
  --logs-root "$LOGS_ROOT" \
  --port "$PORT" \
  "$RUNTIME_WORKFLOW"
