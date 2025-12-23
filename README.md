# AI Publisher

A multi-agent AI system that generates well-researched, fact-checked articles using Claude AI or local Ollama models. Produces JSPWiki-formatted documentation through a 5-agent publishing pipeline.

## Quick Start

### 1. Build the Project

```bash
git clone https://github.com/jakefearsd/aipublisher.git
cd aipublisher
mvn clean package -DskipTests
```

### 2. Configure Your LLM Provider

**Option A: Anthropic (Claude API)**
```bash
export ANTHROPIC_API_KEY='your-api-key-here'
```

**Option B: Ollama (Local Inference - Free)**
```bash
ollama pull qwen3:14b
```

### 3. Generate an Article

**Using the convenience script (local Ollama, highest quality):**
```bash
./publish.sh "Apache Kafka"
./publish.sh --universe my-wiki
```

**Using java directly:**
```bash
# Single article with Anthropic
java -jar target/aipublisher.jar -t "Apache Kafka" -w 1500

# From a topic universe
java -jar target/aipublisher.jar --universe my-wiki

# With Ollama
java -jar target/aipublisher.jar -t "Docker Basics" --llm.provider=ollama
```

---

## The Publishing Pipeline

AI Publisher uses five specialized AI agents working in sequence:

| Agent | Role | What It Does |
|-------|------|--------------|
| **Research** | Information Gatherer | Collects facts, sources, creates outline |
| **Writer** | Technical Writer | Transforms research into JSPWiki article |
| **Fact Checker** | Quality Assurance | Verifies every claim against sources |
| **Editor** | Final Polish | Refines prose, adds wiki links, scores quality |
| **Critic** | Syntax Reviewer | Validates JSPWiki syntax, final QA |

```
TopicBrief --> Research --> Draft --> Fact Check --> Edit --> Critique --> Publish
                              ^          |            ^         |
                              |          v            |         v
                              +-- REVISION LOOP ------+---------+
```

If quality thresholds aren't met, the pipeline automatically revises until standards are reached (or max cycles exceeded).

---

## Convenience Script

The `publish.sh` script is pre-configured for local Ollama inference with highest quality settings:

```bash
# Generate a single article
./publish.sh "Topic Name"
./publish.sh "Apache Kafka" -w 2000 -a "backend developers"

# Generate from a topic universe
./publish.sh --universe my-wiki
./publish.sh -u my-wiki --generate-stubs

# With custom audience
./publish.sh "Docker" --audience "DevOps engineers new to containers"
```

The script uses:
- Local Ollama at `http://inference.jakefear.com:11434`
- Model: `qwen3:14b`
- Full pipeline (no skipped phases)
- Auto-approve mode for unattended generation

---

## Generation Modes

### Single Article Mode

Generate one article at a time:

```bash
# Basic
java -jar target/aipublisher.jar -t "Apache Kafka"

# With all options
java -jar target/aipublisher.jar \
  --topic "Introduction to Docker" \
  --type tutorial \
  --audience "developers new to containers" \
  --words 2000 \
  --output ./wiki/pages \
  --auto-approve
```

### Universe Mode

Generate articles from a saved topic universe (created with [aidiscovery](https://github.com/jakefearsd/aidiscovery)):

```bash
# Generate all articles from universe
java -jar target/aipublisher.jar --universe my-wiki

# Also generate stub pages for gap concepts
java -jar target/aipublisher.jar --universe my-wiki --generate-stubs
```

### Interactive Mode

When no topic is specified, launches an interactive session:

```bash
java -jar target/aipublisher.jar
```

Prompts for topic, content type, audience, word count, and other options.

---

## Content Types

| Type | Best For | Typical Structure |
|------|----------|-------------------|
| `concept` | Explaining ideas | Definition → Explanation → Examples |
| `tutorial` | Step-by-step learning | Overview → Prerequisites → Steps |
| `reference` | API docs, specs | Synopsis → Parameters → Examples |
| `guide` | Best practices | Context → Recommendations → Examples |
| `comparison` | Evaluating options | Overview → Criteria → Comparison Table |
| `troubleshooting` | Problem solving | Symptom → Cause → Solution |
| `overview` | High-level intro | What → Why → How → Next Steps |

```bash
java -jar target/aipublisher.jar -t "How to use Docker" --type tutorial
java -jar target/aipublisher.jar -t "Kafka vs RabbitMQ" --type comparison
```

---

## Gap Detection and Stubs

After generating articles, detect and fill broken wiki links:

```bash
# Report gaps only (no generation)
java -jar target/aipublisher.jar --analyze-gaps
java -jar target/aipublisher.jar --analyze-gaps --universe my-wiki

# Generate stub pages
java -jar target/aipublisher.jar --stubs-only
java -jar target/aipublisher.jar --stubs-only --universe my-wiki

# Generate stubs after universe generation
java -jar target/aipublisher.jar --universe my-wiki --generate-stubs
```

### Gap Types

| Type | Description | Action |
|------|-------------|--------|
| **DEFINITION** | Term needing 100-200 word definition | Generate stub |
| **REDIRECT** | Alias of existing page | Create redirect |
| **FULL_ARTICLE** | Significant concept | Flag for review |
| **IGNORE** | Too generic (e.g., "time") | Skip |

---

## Command Line Reference

```
Usage: aipublisher [OPTIONS]

Topic Input:
  -t, --topic <topic>           Topic to write about
      --type <type>             Content type: concept, tutorial, reference,
                                guide, comparison, troubleshooting, overview
  -i, --interactive             Force interactive mode

Universe Mode:
  -u, --universe <id>           Generate from saved topic universe
      --generate-stubs          Generate stubs after universe generation

Stub Generation:
      --analyze-gaps            Report gaps only (no generation)
      --stubs-only              Generate stubs for existing wiki content

Content Options:
  -a, --audience <audience>     Target audience (default: general readers)
  -w, --words <count>           Target word count (default: 800)
      --context <context>       Domain context (e.g., 'microservices')
      --goal <goal>             Specific goal for tutorials
      --sections <s1,s2,...>    Required sections (comma-separated)
      --related <p1,p2,...>     Related pages for linking (comma-separated)

Output:
  -o, --output <dir>            Output directory (default: ./output)
  -v, --verbose                 Enable verbose output
  -q, --quiet                   Suppress non-essential output
      --auto-approve            Skip approval prompts (for scripting)

LLM Provider:
      --llm.provider=<p>        "anthropic" or "ollama" (default: anthropic)
      --ollama.base-url=<url>   Ollama server URL
      --ollama.model=<model>    Ollama model (default: qwen3:14b)
      --anthropic.model=<m>     Anthropic model (default: claude-sonnet-4-20250514)

Pipeline Control:
      --pipeline.skip-fact-check=<bool>
                                Skip fact-checking phase
      --pipeline.skip-critique=<bool>
                                Skip critique phase
      --pipeline.max-revision-cycles=<n>
                                Max revision cycles (default: 3)

Quality Thresholds:
      --quality.min-editor-score=<n>
                                Minimum editor score 0.0-1.0 (default: 0.8)
      --quality.min-factcheck-confidence=<level>
                                Minimum: LOW, MEDIUM, HIGH (default: MEDIUM)
      --quality.require-verified-claims=<bool>
                                Require verified claims (default: true)

API Key (Anthropic only):
  -k, --key <key>               API key (overrides environment)
      --key-file <path>         Read API key from file
      ANTHROPIC_API_KEY         Environment variable (default)

  -h, --help                    Show help
  -V, --version                 Show version
```

---

## Examples

### Single Articles

```bash
# Simple article
java -jar target/aipublisher.jar -t "Apache Kafka"

# Tutorial with audience
java -jar target/aipublisher.jar \
  -t "How to Deploy with Docker" \
  --type tutorial \
  -a "DevOps beginners" \
  -w 1500

# Comparison article
java -jar target/aipublisher.jar \
  -t "Kafka vs RabbitMQ" \
  --type comparison \
  --auto-approve

# Fast iteration (skip validation)
java -jar target/aipublisher.jar \
  -t "Quick Test" \
  --pipeline.skip-fact-check=true \
  --pipeline.skip-critique=true
```

### Using Ollama

```bash
# Local Ollama
java -jar target/aipublisher.jar -t "Topic" --llm.provider=ollama

# Remote Ollama server
java -jar target/aipublisher.jar -t "Topic" \
  --llm.provider=ollama \
  --ollama.base-url=http://your-server:11434 \
  --ollama.model=qwen3:14b

# Using the convenience script (pre-configured)
./publish.sh "Topic"
```

### Universe Workflow

```bash
# 1. Create universe with aidiscovery
aidiscovery -c balanced

# 2. Generate articles
java -jar target/aipublisher.jar --universe my-wiki

# 3. Fill gaps with stubs
java -jar target/aipublisher.jar --stubs-only --universe my-wiki

# Or do it all at once
java -jar target/aipublisher.jar --universe my-wiki --generate-stubs
```

### Scripting/CI Usage

```bash
# Fully automated (no prompts)
java -jar target/aipublisher.jar -t "Topic" --auto-approve -q

# With inline env var
ANTHROPIC_API_KEY='key' java -jar target/aipublisher.jar -t "Topic" --auto-approve
```

---

## Output

Articles are saved as JSPWiki-formatted `.txt` files:

```
./output/
├── ApacheKafka.txt           # Generated article
├── DockerBasics.txt
└── debug/                    # Debug artifacts (if pipeline fails)
    └── FailedTopic_draft.json
```

### Sample Output

```
!!! Apache Kafka

Apache Kafka is an open-source distributed event streaming platform...

[{TableOfContents}]

!! Core Concepts

! Topics and Partitions

A __topic__ is a category for organizing messages...

[{SET categories='Streaming,Messaging'}]
```

---

## Recommended Ollama Models

| Model | Size | Quality | Speed | Best For |
|-------|------|---------|-------|----------|
| `qwen3:14b` | 14B | High | Medium | Production (recommended) |
| `qwen2.5:7b` | 7B | Good | Fast | Development/testing |
| `llama3.2` | 3B | Moderate | Very Fast | Quick prototyping |
| `mistral` | 7B | Good | Fast | General use |

---

## Development

### Building

```bash
mvn clean package -DskipTests  # Quick build
mvn clean package              # With tests
```

### Running from Source

```bash
ANTHROPIC_API_KEY='key' mvn spring-boot:run
mvn spring-boot:run -Dspring-boot.run.arguments="--llm.provider=ollama"
```

### Running Tests

```bash
mvn test                           # Unit tests
mvn test -Dgroups=integration      # Integration tests (needs Ollama)
```

---

## Architecture

For internal design details, see [ARCHITECTURE.md](ARCHITECTURE.md):
- Agent architecture and pipeline flow
- Package structure and design patterns
- Document state machine
- LLM integration details
- Extension points

For development guidelines, see [CLAUDE.md](CLAUDE.md):
- TDD practices and test organization
- Coding conventions
- Pattern usage guide

---

## Technology Stack

- **Java 21** - Modern Java with records, pattern matching
- **Spring Boot 3.3** - Application framework
- **LangChain4j 0.36** - LLM integration
- **Picocli 4.7** - CLI framework
- **JUnit 5** - Testing

---

## Related Projects

- **[aidiscovery](https://github.com/jakefearsd/aidiscovery)** - Topic universe discovery tool
  - Create topic universes through interactive or autonomous AI-assisted discovery
  - Both tools share the `TopicUniverse` JSON format at `~/.aipublisher/universes/`

---

## License

This project is for educational purposes.

## Acknowledgments

- Built with [Claude](https://claude.ai) by Anthropic
- Uses [LangChain4j](https://github.com/langchain4j/langchain4j) for LLM integration
- CLI powered by [Picocli](https://picocli.info/)
