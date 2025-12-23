#!/bin/bash
# publish.sh - AI Publisher convenience script for local Ollama inference
#
# Uses local Ollama server with highest quality settings (full pipeline).
#
# Usage:
#   ./publish.sh "Topic Name"                    # Single article
#   ./publish.sh --universe my-wiki              # From topic universe
#   ./publish.sh -u my-wiki --generate-stubs     # With stub generation
#   ./publish.sh "Topic" -w 2000 -a "developers" # With options

set -e

# Configuration
OLLAMA_URL="${OLLAMA_BASE_URL:-http://inference.jakefear.com:11434}"
OLLAMA_MODEL="${OLLAMA_MODEL:-qwen3:14b}"

# Find the JAR file
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_PATH="$SCRIPT_DIR/target/aipublisher.jar"

if [[ ! -f "$JAR_PATH" ]]; then
    echo "ERROR: JAR file not found at $JAR_PATH"
    echo "Build the project first: mvn clean package -DskipTests"
    exit 1
fi

# Check if first argument looks like an option or a topic
TOPIC_ARG=""
EXTRA_ARGS=()

if [[ $# -gt 0 ]]; then
    case "$1" in
        --universe|-u|--stubs-only|--analyze-gaps|--help|-h|--version|-V)
            # First arg is an option, pass all args through
            EXTRA_ARGS=("$@")
            ;;
        -*)
            # Some other option, pass through
            EXTRA_ARGS=("$@")
            ;;
        *)
            # First arg is the topic
            TOPIC_ARG="$1"
            shift
            EXTRA_ARGS=("$@")
            ;;
    esac
fi

# Build the command
CMD=(
    java -jar "$JAR_PATH"
    --llm.provider=ollama
    --ollama.base-url="$OLLAMA_URL"
    --ollama.model="$OLLAMA_MODEL"
    # Full pipeline - highest quality (don't skip any phases)
    --pipeline.skip-fact-check=false
    --pipeline.skip-critique=false
    # Auto-approve for unattended generation
    --auto-approve
    # Verbose output
    -v
)

# Add topic if provided
if [[ -n "$TOPIC_ARG" ]]; then
    CMD+=(-t "$TOPIC_ARG")
fi

# Add any extra arguments
if [[ ${#EXTRA_ARGS[@]} -gt 0 ]]; then
    CMD+=("${EXTRA_ARGS[@]}")
fi

# Show what we're running
echo "Running: ${CMD[*]}"
echo ""

# Execute
exec "${CMD[@]}"
