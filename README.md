# AI Publisher

A multi-agent AI system that generates well-researched, fact-checked articles using Claude AI. The system orchestrates five specialized agents through a publishing pipeline to produce high-quality JSPWiki-formatted documentation.

## Overview

AI Publisher uses a team of AI agents, each with a specialized role, to create articles that are thoroughly researched, clearly written, fact-verified, syntax-validated, and publication-ready. The agents work in sequence, with optional human approval checkpoints at each stage.

### The Agent Team

| Agent | Role | Temperature | Purpose |
|-------|------|-------------|---------|
| **Research Agent** | Information Gatherer | 0.3 | Collects facts, sources, and creates article outlines |
| **Writer Agent** | Technical Writer | 0.7 | Transforms research into well-structured JSPWiki articles |
| **Fact Checker Agent** | Quality Assurance | 0.1 | Verifies every claim against sources, flags issues |
| **Editor Agent** | Final Polish | 0.5 | Refines prose, adds wiki links, calculates quality score |
| **Critic Agent** | Syntax & Quality Reviewer | 0.3 | Validates JSPWiki syntax, reviews structure, final QA |

### Pipeline Flow

```
TopicBrief --> Research --> Draft --> Fact Check --> Edit --> Critique --> Publish
                              ^          |            ^         |
                              |          v            |         v
                              +-- REVISION LOOP ------+---------+
```

Documents progress through six phases:

1. **Research** - Gather key facts, sources, and create an outline
2. **Drafting** - Write the article in JSPWiki markup syntax
3. **Fact Checking** - Verify all claims, identify questionable content
4. **Editing** - Polish prose, integrate wiki links, score quality
5. **Critique** - Validate JSPWiki syntax, review structure, final quality check
6. **Publishing** - Write the final `.txt` file to the output directory

## Features

- **Multi-Agent Architecture** - Five specialized AI agents with role-appropriate temperature settings
- **Multiple LLM Providers** - Support for Anthropic (Claude) and Ollama (local inference)
- **JSPWiki Syntax Output** - Native JSPWiki markup format (not Markdown) for wiki compatibility
- **Automatic Revision Loop** - Fact checker and critic can trigger re-drafting for quality issues
- **Syntax Validation** - Critic agent catches any Markdown syntax that should be JSPWiki
- **Configurable Pipeline** - Skip phases (fact-check, critique) for faster iteration
- **Human Approval Workflow** - Optional review checkpoints at each phase boundary
- **Quality Scoring** - Editor and Critic assign 0.0-1.0 quality scores with configurable thresholds
- **Wiki Integration** - Automatic internal linking to existing pages, CamelCase naming
- **Comprehensive Fact Checking** - Every claim verified, confidence levels, recommended actions
- **Web Search Integration** - Optional DuckDuckGo-based web search for research and verification
- **Source Reliability Assessment** - Automatic classification of source trustworthiness
- **Pipeline Monitoring** - Event system for tracking progress and metrics
- **Universe-Based Generation** - Generate articles from topic universes created with [aidiscovery](https://github.com/jakefearsd/aidiscovery)
- **Gap Detection & Stub Generation** - Automatically detect and fill gaps in wiki content

## Requirements

- Java 21 or later
- Maven 3.8+
- One of the following LLM providers:
  - **Anthropic API key** ([Get one here](https://console.anthropic.com/settings/keys)) - Cloud-based, paid
  - **Ollama server** ([Install Ollama](https://ollama.ai)) - Local inference, free

## Quick Start

### 1. Build the Project

```bash
git clone https://github.com/jakefearsd/aipublisher.git
cd aipublisher
mvn clean package -DskipTests
```

### 2. Configure Your LLM Provider

Choose either Anthropic (cloud) or Ollama (local):

**Option A: Anthropic (Claude API)**
```bash
# Environment variable (recommended)
export ANTHROPIC_API_KEY='your-api-key-here'

# Or command line flag
java -jar target/aipublisher.jar -t "Topic" -k "your-api-key-here"

# Or key file
echo "your-api-key-here" > ~/.anthropic-key
java -jar target/aipublisher.jar -t "Topic" --key-file ~/.anthropic-key
```

**Option B: Ollama (Local Inference - Free)**
```bash
# Start Ollama and pull a model
ollama pull qwen2.5:14b

# Run with Ollama (automatically uses localhost:11434)
java -jar target/aipublisher.jar -t "Topic" --llm.provider=ollama

# Or with custom Ollama server
java -jar target/aipublisher.jar -t "Topic" \
  --llm.provider=ollama \
  --ollama.base-url=http://your-server:11434 \
  --ollama.model=qwen2.5:14b
```

### 3. Generate an Article

```bash
# Basic usage
java -jar target/aipublisher.jar --topic "Apache Kafka"

# With all options
java -jar target/aipublisher.jar \
  --topic "Apache Kafka" \
  --audience "developers new to event streaming" \
  --words 1500 \
  --auto-approve

# Interactive mode (prompts for topic)
java -jar target/aipublisher.jar
```

## Running Without Maven

Once built, AI Publisher runs as a standalone JAR with no Maven required.

### Executable JAR

The build creates a fat JAR (Spring Boot uber-jar) that includes all dependencies:

```bash
# Build once (requires Maven)
mvn clean package -DskipTests

# The JAR is now at: target/aipublisher.jar
# Copy it anywhere and run with just Java:
java -jar target/aipublisher.jar --help
```

### Setting Up a Shell Alias

For convenience, create an alias or script:

```bash
# Option 1: Shell alias (add to ~/.bashrc or ~/.zshrc)
alias aipublisher='java -jar /path/to/aipublisher.jar'

# Then use:
aipublisher --topic "My Topic"
aipublisher --universe my-wiki

# Option 2: Executable script
cat > /usr/local/bin/aipublisher << 'EOF'
#!/bin/bash
java -jar /path/to/aipublisher.jar "$@"
EOF
chmod +x /usr/local/bin/aipublisher
```

### API Key Configuration

Configure your API key using one of these methods (in priority order):

```bash
# Method 1: Environment variable (recommended for security)
export ANTHROPIC_API_KEY='sk-ant-api03-...'
java -jar aipublisher.jar --topic "Kubernetes"

# Method 2: Command line flag (visible in process list - use with caution)
java -jar aipublisher.jar --topic "Kubernetes" -k "sk-ant-api03-..."

# Method 3: Key file (good for shared systems)
echo "sk-ant-api03-..." > ~/.anthropic-key
chmod 600 ~/.anthropic-key
java -jar aipublisher.jar --topic "Kubernetes" --key-file ~/.anthropic-key

# Method 4: One-liner with inline env var
ANTHROPIC_API_KEY='sk-ant-api03-...' java -jar aipublisher.jar --topic "Kubernetes"
```

### Common Usage Patterns

**Single Article Generation:**
```bash
# Basic article
java -jar aipublisher.jar -t "Apache Kafka"

# Customized article with content type
java -jar aipublisher.jar \
  --topic "Introduction to Docker" \
  --type tutorial \
  --audience "developers new to containers" \
  --words 2000 \
  --output ./wiki/pages

# Scripted/CI usage (no prompts)
java -jar aipublisher.jar -t "GraphQL APIs" --auto-approve -q
```

**Universe-Based Generation:**
```bash
# Generate articles from a saved topic universe
java -jar aipublisher.jar --universe my-wiki

# Generate articles and stubs for gaps
java -jar aipublisher.jar --universe my-wiki --generate-stubs
```

**Working with Output:**
```bash
# Custom output directory
java -jar aipublisher.jar -t "My Topic" -o ./docs/wiki

# Output structure
./output/
├── MyTopic.txt           # Generated JSPWiki article
└── debug/                # Debug artifacts (if pipeline fails)
    └── MyTopic_draft.json
```

### JVM Options

For large wikis or extended sessions, you may want to adjust JVM memory:

```bash
# Increase heap for large universes
java -Xmx2g -jar aipublisher.jar --universe my-wiki

# Quiet mode for scripts
java -jar aipublisher.jar -t "Topic" --auto-approve -q 2>/dev/null
```

## Gap Detection and Stub Generation

After generating articles, your wiki may have internal links that point to pages that don't yet exist (gap concepts). AI Publisher can detect these gaps and automatically generate brief stub/definition pages to fill them.

### What Are Gap Concepts?

Gap concepts are terms referenced in your wiki (via `[PageName]` links) that don't have corresponding articles. For example, an article about "Compound Interest" might link to `[PresentValue]` and `[DiscountRate]`, but if those pages don't exist, they become gaps.

### Gap Types

The system categorizes each gap into one of four types:

| Type | Description | Action |
|------|-------------|--------|
| **DEFINITION** | Technical term needing a brief 100-200 word definition | Generate stub page |
| **REDIRECT** | Alias or alternate spelling of an existing page | Create redirect page |
| **FULL_ARTICLE** | Significant concept deserving comprehensive coverage | Flag for review |
| **IGNORE** | Too generic or common (e.g., "money", "time") | Skip |

### Analyzing Gaps (Report Only)

To see what gaps exist without generating anything:

```bash
java -jar target/aipublisher.jar --analyze-gaps

# With universe context (loads name and metadata from saved universe)
java -jar target/aipublisher.jar --analyze-gaps --universe investing-basics

# Or with manual domain context
java -jar target/aipublisher.jar --analyze-gaps --context "Finance"
```

### Generating Stubs Only

To generate stub pages for existing wiki content without generating main articles:

```bash
# Basic usage - prompts for confirmation
java -jar target/aipublisher.jar --stubs-only

# With universe context (recommended - loads name and audience from saved universe)
java -jar target/aipublisher.jar --stubs-only --universe investing-basics

# Or with manual domain context
java -jar target/aipublisher.jar --stubs-only --context "Investing Basics"

# With specific target audience (overrides universe audience)
java -jar target/aipublisher.jar --stubs-only --universe investing-basics --audience "financial professionals"
```

### Generating Stubs After Universe Generation

To automatically generate stubs after generating articles from a universe:

```bash
java -jar target/aipublisher.jar --universe investing-basics --generate-stubs
```

This will:
1. Generate all main articles from the universe
2. Create a summary page
3. Detect gaps in the generated content
4. Generate stub pages for gap concepts
5. Report any concepts that need full articles

### Stub Generation Workflow

The recommended workflow for comprehensive wiki coverage:

1. **Create a topic universe** (using aidiscovery):
   ```bash
   aidiscovery -c balanced
   ```

2. **Generate main articles**:
   ```bash
   java -jar target/aipublisher.jar --universe my-wiki
   ```

3. **Analyze gaps** (optional - to preview what stubs will be created):
   ```bash
   java -jar target/aipublisher.jar --analyze-gaps --universe my-wiki
   ```

4. **Generate stubs** to fill the gaps:
   ```bash
   java -jar target/aipublisher.jar --stubs-only --universe my-wiki
   ```

5. **Review flagged topics** - Any concepts flagged as FULL_ARTICLE should be added to your universe for comprehensive coverage.

## Command Line Reference

```
Usage: aipublisher [-hiqvV] [--auto-approve] [--analyze-gaps] [--stubs-only]
                   [--generate-stubs] [-a=<audience>] [-k=<apiKey>]
                   [--key-file=<keyFile>] [-o=<outputDirectory>] [-t=<topic>]
                   [--type=<contentType>] [-u=<universe>] [-w=<wordCount>]
                   [--context=<context>] [--goal=<goal>]
                   [--related=<relatedPages>]... [--sections=<requiredSections>]...

Generate well-researched, fact-checked articles using AI agents.

Options:
  -t, --topic=<topic>        Topic to write about (prompts interactively if not
                               specified)
      --type=<type>          Content type: concept, tutorial, reference, guide,
                               comparison, troubleshooting, overview
  -u, --universe=<id>        Generate articles from a saved topic universe
  -i, --interactive          Force interactive mode even with topic specified
  -a, --audience=<audience>  Target audience for the article
                               (default: general readers)
  -w, --words=<wordCount>    Target word count (default: 800)
  -o, --output=<outputDirectory>
                             Output directory for generated articles
                               (default: ./output)
      --context=<context>    Domain context (e.g., 'e-commerce', 'microservices')
      --goal=<goal>          Specific goal or outcome for tutorials and guides
      --sections=<sec1,sec2> Required sections (comma-separated)
      --related=<page1,page2>
                             Related pages for internal linking (comma-separated)
      --auto-approve         Skip all approval prompts (for scripting)
  -q, --quiet                Suppress non-essential output
  -v, --verbose              Enable verbose output
  -h, --help                 Show this help message and exit.
  -V, --version              Print version information and exit.

LLM Provider Options:
      --llm.provider=<provider>
                             LLM provider: "anthropic" or "ollama" (default: anthropic)
      --ollama.base-url=<url>
                             Ollama server URL (default: http://localhost:11434)
      --ollama.model=<model> Ollama model name (default: qwen2.5:14b)
      --ollama.timeout=<duration>
                             Request timeout, ISO-8601 (default: PT5M)
      --anthropic.model=<model>
                             Anthropic model name (default: claude-sonnet-4-20250514)

Pipeline Options:
      --pipeline.skip-fact-check=<bool>
                             Skip fact-checking phase (default: false)
      --pipeline.skip-critique=<bool>
                             Skip critique phase (default: false)
      --pipeline.max-revision-cycles=<n>
                             Max revision cycles before failing (default: 3)

Quality Options:
      --quality.require-verified-claims=<bool>
                             Require verified claims from fact-checker (default: true)
      --quality.min-factcheck-confidence=<level>
                             Minimum confidence: LOW, MEDIUM, HIGH (default: MEDIUM)
      --quality.min-editor-score=<n>
                             Minimum editor score 0.0-1.0 (default: 0.8)

Stub Generation Options:
      --analyze-gaps         Analyze wiki for gaps (report only, no generation)
      --stubs-only           Generate stub pages for existing wiki content
      --generate-stubs       Generate stubs after universe article generation
                             Note: Use -u/--universe with --analyze-gaps or --stubs-only
                             to load domain context from a saved universe

API Key Options:
  -k, --key=<apiKey>         Anthropic API key (overrides environment variable)
      --key-file=<keyFile>   Path to file containing Anthropic API key
      ANTHROPIC_API_KEY      Environment variable (default)

Content Types:
  concept         Explains what something is
  tutorial        Step-by-step guide
  reference       Quick lookup information
  guide           Decision support and best practices
  comparison      Analyzes alternatives
  troubleshooting Problem diagnosis and solutions
  overview        High-level introduction

Examples:
  aipublisher                                    # Interactive mode
  aipublisher -t "Apache Kafka"                  # Simple topic
  aipublisher -t "How to use Docker" --type tutorial
  aipublisher -t "Kafka vs RabbitMQ" --type comparison
  aipublisher -t "Docker" -a "DevOps engineers" -w 1000 --auto-approve
  aipublisher --universe my-wiki                 # Generate from universe
  aipublisher --universe my-wiki --generate-stubs
  aipublisher --analyze-gaps --universe my-wiki
  aipublisher --stubs-only --universe my-wiki

  # Using Ollama (local inference)
  aipublisher -t "Topic" --llm.provider=ollama
  aipublisher -t "Topic" --llm.provider=ollama --ollama.model=llama3.2

  # Fast iteration (skip validation phases)
  aipublisher -t "Topic" --pipeline.skip-fact-check=true --pipeline.skip-critique=true
```

## Configuration

Configuration is managed through `src/main/resources/application.properties` or via command-line arguments.

### LLM Provider Selection

AI Publisher supports two LLM providers: Anthropic (Claude) and Ollama (local inference).

```properties
# Provider selection: "anthropic" or "ollama"
llm.provider=anthropic

# Temperature settings per agent (0.0=deterministic, 1.0=creative)
# Used by both providers
llm.temperature.research=0.3
llm.temperature.writer=0.7
llm.temperature.factchecker=0.3
llm.temperature.editor=0.5
llm.temperature.critic=0.3
```

**Command-line override:**
```bash
java -jar target/aipublisher.jar -t "Topic" --llm.provider=ollama
```

### Anthropic (Claude) Settings

```properties
# API key (via environment variable recommended)
anthropic.api.key=${ANTHROPIC_API_KEY}

# Model to use
anthropic.model=claude-sonnet-4-20250514

# Maximum tokens per response
anthropic.max-tokens=4096
```

**Command-line override:**
```bash
java -jar target/aipublisher.jar -t "Topic" \
  --anthropic.model=claude-opus-4-20250514
```

### Ollama Settings

```properties
# Ollama server URL
ollama.base-url=http://localhost:11434

# Model to use (must be pulled first: ollama pull <model>)
ollama.model=qwen2.5:14b

# Maximum tokens to predict
ollama.num-predict=4096

# Request timeout (ISO-8601 duration)
ollama.timeout=PT5M
```

**Command-line override:**
```bash
java -jar target/aipublisher.jar -t "Topic" \
  --llm.provider=ollama \
  --ollama.base-url=http://your-server:11434 \
  --ollama.model=llama3.2
```

**Environment variables (useful for CI/testing):**
```bash
export OLLAMA_BASE_URL=http://your-server:11434
export OLLAMA_MODEL=qwen2.5:14b
```

### Pipeline Settings

```properties
# Maximum revision cycles before failing
pipeline.max-revision-cycles=3

# Timeout per phase
pipeline.phase-timeout=PT5M

# Skip phases for faster iteration (useful for testing/development)
pipeline.skip-fact-check=false
pipeline.skip-critique=false

# Human approval checkpoints (enable/disable each)
pipeline.approval.after-research=false
pipeline.approval.after-draft=false
pipeline.approval.after-factcheck=false
pipeline.approval.before-publish=true
```

**Command-line override:**
```bash
# Skip fact-checking and critique phases for rapid iteration
java -jar target/aipublisher.jar -t "Topic" \
  --pipeline.skip-fact-check=true \
  --pipeline.skip-critique=true
```

### Web Search Settings

```properties
# Enable/disable web search for research augmentation
search.enabled=true

# Maximum search results to return
search.max-results=5
```

### Output Settings

```properties
# Where to write generated articles
output.directory=./output
output.file-extension=.txt
```

### Quality Thresholds

```properties
# Minimum fact check confidence to proceed
quality.min-factcheck-confidence=MEDIUM

# Minimum editor quality score to publish
quality.min-editor-score=0.8

# Minimum critic score to publish (syntax validation threshold)
quality.min-critic-score=0.8

# Require at least one verified claim from fact checker
# Set to false to accept APPROVE even when model returns empty claim arrays
# Useful for models that express "no issues found" with empty arrays
quality.require-verified-claims=true
```

**Command-line override for lenient validation:**
```bash
# Accept fact-check APPROVE even without verified claims
java -jar target/aipublisher.jar -t "Topic" \
  --quality.require-verified-claims=false
```

## Architecture

### Document State Machine

Documents progress through these states:

```
CREATED --> RESEARCHING --> DRAFTING --> FACT_CHECKING --> EDITING --> CRITIQUING --> PUBLISHED
                 ^              |               |              ^            |
                 |              v               v              |            v
                 +------- REVISION LOOP -------+---------------+-----------+

Any state --> AWAITING_APPROVAL --> Continue or REJECTED
```

### Agent Outputs

Each agent produces structured output that feeds into the next phase:

| Agent | Output | Key Fields |
|-------|--------|------------|
| Research | `ResearchBrief` | Key facts, sources, outline, glossary, uncertainties |
| Writer | `ArticleDraft` | JSPWiki content, summary, internal links, categories |
| Fact Checker | `FactCheckReport` | Verified claims, questionable items, confidence, action |
| Editor | `FinalArticle` | Polished content, metadata, quality score, edit summary |
| Critic | `CriticReport` | Structure/syntax/readability scores, issues, suggestions |

### JSPWiki Markup Format

The pipeline generates articles in JSPWiki markup format, which differs from Markdown:

| Element | JSPWiki Syntax | Markdown Equivalent |
|---------|---------------|---------------------|
| H1 Heading | `!!! Title` | `# Title` |
| H2 Heading | `!! Section` | `## Section` |
| H3 Heading | `! Subsection` | `### Subsection` |
| Bold | `__bold__` | `**bold**` |
| Italic | `''italic''` | `*italic*` |
| Inline Code | `{{code}}` | `` `code` `` |
| Code Block | `{{{ code }}}` | ` ```code``` ` |
| Internal Link | `[PageName]` | `[text](PageName)` |
| External Link | `[text\|url]` | `[text](url)` |
| Bullet List | `* item` | `- item` |
| Numbered List | `# item` | `1. item` |
| Table Header | `\|\| H1 \|\| H2` | `\| H1 \| H2 \|` |
| Table of Contents | `[{TableOfContents}]` | N/A |
| Categories | `[{SET categories='Cat1,Cat2'}]` | N/A |

### Web Search Integration

The pipeline includes optional web search capabilities for enhanced research and fact-checking:

**Search Features:**
- DuckDuckGo-based search (no API key required)
- Automatic source reliability assessment
- Specialized search methods for verification and documentation

**Source Reliability Levels:**

| Level | Description | Score | Examples |
|-------|-------------|-------|----------|
| OFFICIAL | Primary documentation | 1.0 | docs.oracle.com, kafka.apache.org |
| ACADEMIC | Peer-reviewed sources | 0.95 | arxiv.org, ACM, IEEE |
| AUTHORITATIVE | Major tech publishers | 0.85 | O'Reilly, Manning, InfoQ |
| REPUTABLE | Community with oversight | 0.7 | Wikipedia, Stack Overflow |
| COMMUNITY | User-generated content | 0.5 | Reddit, Quora, forums |
| UNCERTAIN | Cannot determine | 0.3 | Unknown sources |

### Fact Check Outcomes

The Fact Checker evaluates every claim and provides:

- **Claim Status**: VERIFIED, QUESTIONABLE, or flagged for consistency issues
- **Confidence Level**: HIGH, MEDIUM, or LOW
- **Recommended Action**:
  - `APPROVE` - Content is ready to proceed
  - `REVISE` - Send back to Writer for improvements
  - `REJECT` - Content cannot meet quality standards

### Critic Review Process

The Critic Agent performs final quality assurance before publication:

**Review Categories:**
1. **Structure Score** - Article organization, heading hierarchy, sections
2. **Syntax Score** - JSPWiki markup correctness (critical - flags any Markdown)
3. **Readability Score** - Paragraph size, tone, technical clarity
4. **Overall Score** - Combined quality assessment

**Critic Actions:**
- `APPROVE` - Article ready for publication (score >= 0.8, no critical syntax issues)
- `REVISE` - Minor issues need fixing (triggers revision loop back to Editor)
- `REJECT` - Major problems require rework

### Human Approval Workflow

When approval checkpoints are enabled, the pipeline pauses and prompts:

```
Approval required: Ready to publish
[A]pprove - Continue to next phase
[R]eject  - Stop and reject the document
[C]hanges - Request changes and retry
Enter decision (A/R/C):
```

Use `--auto-approve` to skip all prompts for automated/CI usage.

## Project Structure

```
src/main/java/com/jakefear/aipublisher/
├── agent/                    # AI agent implementations
│   ├── ResearchAgent.java    # Information gathering
│   ├── WriterAgent.java      # Article drafting (JSPWiki)
│   ├── FactCheckerAgent.java # Claim verification
│   ├── EditorAgent.java      # Final polish
│   ├── CriticAgent.java      # Syntax & quality review
│   ├── BaseAgent.java        # Common agent functionality
│   └── AgentPrompts.java     # System prompts for all agents
│
├── domain/                   # Topic universe data model
│   ├── Topic.java               # Wiki topic with status/priority
│   ├── TopicUniverse.java       # Complete domain container
│   ├── TopicRelationship.java   # Topic connections
│   ├── TopicStatus.java         # PROPOSED/ACCEPTED/REJECTED/etc.
│   ├── Priority.java            # MUST_HAVE/SHOULD_HAVE/etc.
│   ├── RelationshipType.java    # PREREQUISITE_OF/PART_OF/etc.
│   ├── ComplexityLevel.java     # BEGINNER/INTERMEDIATE/ADVANCED
│   ├── ScopeConfiguration.java  # Scope boundaries
│   └── TopicUniverseRepository.java # JSON persistence
│
├── pipeline/                 # Pipeline orchestration
│   ├── PublishingPipeline.java  # Main orchestrator
│   └── PipelineResult.java      # Result with metrics
│
├── document/                 # Document models
│   ├── PublishingDocument.java  # Main entity with state
│   ├── TopicBrief.java          # Input specification
│   ├── ResearchBrief.java       # Research output
│   ├── ArticleDraft.java        # Writer output
│   ├── FactCheckReport.java     # Verification results
│   ├── FinalArticle.java        # Editor output
│   └── CriticReport.java        # Critic review results
│
├── gap/                      # Gap detection and stub generation
│   ├── GapDetector.java         # Finds broken wiki links
│   ├── GapConcept.java          # Gap concept model
│   └── StubGenerationService.java # Generates stub pages
│
├── search/                   # Web search integration
│   ├── WebSearchService.java    # DuckDuckGo search
│   ├── SearchResult.java        # Search result model
│   └── SourceReliability.java   # Source assessment
│
├── approval/                 # Human approval workflow
│   ├── ApprovalService.java     # Approval management
│   └── ApprovalCallback.java    # Pluggable handlers
│
├── output/                   # Output generation
│   └── WikiOutputService.java   # JSPWiki file writer
│
├── monitoring/               # Pipeline observability
│   └── PipelineMonitoringService.java
│
├── config/                   # Spring configuration
│   ├── ClaudeConfig.java        # LangChain4j beans
│   └── PipelineProperties.java  # Pipeline settings
│
└── cli/                      # Command-line interface
    ├── AiPublisherCommand.java  # Main CLI
    └── InteractiveSession.java  # Single-article interactive mode
```

## Example Session

Running the pipeline for "Apache Kafka" produces output like:

```
╔═══════════════════════════════════════════════════════════╗
║                      AI PUBLISHER                         ║
║     Generate well-researched articles with AI agents      ║
╚═══════════════════════════════════════════════════════════╝

Topic:        Apache Kafka
Content Type: Concept Explanation
Audience:     developers new to event streaming
Target words: 1500

Phase 1: Research
  Gathered 8 key facts from 6 sources

Phase 2: Drafting
  Created 1,523 word article with 5 sections

Phase 3: Fact Checking
  Verified 23 claims, 0 questionable
  Confidence: HIGH, Action: APPROVE

Phase 4: Editing
  Quality score: 0.91
  Added 3 internal links

Phase 5: Critique
  Structure: 0.92, Syntax: 0.98, Readability: 0.89
  Overall: 0.92, Action: APPROVE

Phase 6: Publishing
  Output: ./output/ApacheKafka.txt

════════════════════════════════════════════════════════════
SUCCESS! Article published.

Output: ./output/ApacheKafka.txt
Quality score: 0.92
Word count: 1,523
Total time: 52,341 ms
════════════════════════════════════════════════════════════
```

### Sample Output (JSPWiki Format)

```
!!! Apache Kafka

Apache Kafka is an open-source distributed event streaming platform originally
developed by LinkedIn. It provides high-throughput, fault-tolerant handling of
real-time data feeds.

[{TableOfContents}]

!! Core Concepts

! Topics and Partitions

A __topic__ is a category for organizing messages. Topics are split into
{{partitions}} for parallel processing. Each partition maintains an ordered,
immutable sequence of records.

! Producers and Consumers

[Producers|KafkaProducers] write data to topics, while [consumers|KafkaConsumers]
read from them. Consumer groups enable parallel processing across multiple
instances.

!! See Also

* [EventStreaming]
* [MessageQueues]
* [DistributedSystems]

[{SET categories='Streaming,Messaging,BigData'}]
```

## Development

### Running Tests

```bash
# All unit tests (no API key required)
mvn test

# Skip integration tests explicitly
mvn test -DexcludeGroups=integration

# Specific test class
mvn test -Dtest=PublishingPipelineTest
```

**Integration tests with Anthropic (paid):**
```bash
ANTHROPIC_API_KEY='your-key' mvn test -Dgroups=integration
```

**Integration tests with Ollama (free, recommended):**
```bash
# Using local Ollama server
OLLAMA_BASE_URL='http://localhost:11434' mvn test -Dgroups=integration

# Using remote Ollama server with specific model
OLLAMA_BASE_URL='http://your-server:11434' \
OLLAMA_MODEL='qwen2.5:14b' \
mvn test -Dgroups=integration
```

Note: Integration tests prefer Ollama over Anthropic when `OLLAMA_BASE_URL` is set. The default model is `qwen2.5:14b`.

### Building

```bash
# Build without tests
mvn clean package -DskipTests

# Build with unit tests only
mvn clean package

# Build with all tests (requires API key)
ANTHROPIC_API_KEY='your-key' mvn clean package
```

### Running from Source

If you prefer to run directly from source code (useful during development):

```bash
# Using Maven Spring Boot plugin
ANTHROPIC_API_KEY='your-key' mvn spring-boot:run \
  -Dspring-boot.run.arguments="--topic='Your Topic'"

# With multiple arguments
ANTHROPIC_API_KEY='your-key' mvn spring-boot:run \
  -Dspring-boot.run.arguments="--topic='Docker',--audience='beginners',--words=1500"
```

Note: Running via Maven is slower due to compilation overhead. For regular use, build the JAR once and use `java -jar`.

## Technology Stack

- **Java 21** - Modern Java with records, pattern matching
- **Spring Boot 3.3** - Application framework and dependency injection
- **LangChain4j 0.36** - LLM integration framework
- **LLM Providers**:
  - **Claude Sonnet 4** - Anthropic's cloud API (default)
  - **Ollama** - Local inference with models like Qwen 2.5, Llama 3.2, etc.
- **Picocli 4.7** - Professional CLI framework
- **JUnit 5** - Testing framework
- **Mockito** - Mocking for unit tests

### Recommended Ollama Models

| Model | Size | Quality | Speed | Best For |
|-------|------|---------|-------|----------|
| `qwen2.5:14b` | 14B | High | Medium | Production use (default) |
| `qwen2.5:7b` | 7B | Good | Fast | Development/testing |
| `llama3.2` | 3B | Moderate | Very Fast | Quick prototyping |
| `mistral` | 7B | Good | Fast | General use |

Note: Larger models generally produce better structured JSON and follow prompts more reliably.

## Related Projects

- **[aidiscovery](https://github.com/jakefearsd/aidiscovery)** - Topic universe discovery tool
  - Create topic universes through interactive AI-assisted discovery
  - Both tools share the `TopicUniverse` JSON format at `~/.aipublisher/universes/`

## License

This project is for educational purposes.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## Acknowledgments

- Built with [Claude](https://claude.ai) by Anthropic
- Uses [LangChain4j](https://github.com/langchain4j/langchain4j) for LLM integration
- CLI powered by [Picocli](https://picocli.info/)
