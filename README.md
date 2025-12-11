# AI Publisher

A multi-agent AI system that generates well-researched, fact-checked articles using Claude AI. The system orchestrates four specialized agents through a publishing pipeline to produce high-quality wiki-style documentation.

## Overview

AI Publisher uses a team of AI agents, each with a specialized role, to create articles that are thoroughly researched, clearly written, fact-verified, and publication-ready. The agents work in sequence, with optional human approval checkpoints at each stage.

### The Agent Team

| Agent | Role | Temperature | Purpose |
|-------|------|-------------|---------|
| **Research Agent** | Information Gatherer | 0.3 | Collects facts, sources, and creates article outlines |
| **Writer Agent** | Technical Writer | 0.7 | Transforms research into well-structured wiki articles |
| **Fact Checker Agent** | Quality Assurance | 0.1 | Verifies every claim against sources, flags issues |
| **Editor Agent** | Final Polish | 0.5 | Refines prose, adds wiki links, calculates quality score |

### Pipeline Flow

```
TopicBrief --> Research --> Draft --> Fact Check --> Edit --> Publish --> .md file
```

Documents progress through five phases:

1. **Research** - Gather key facts, sources, and create an outline
2. **Drafting** - Write the article in JSPWiki-compatible Markdown
3. **Fact Checking** - Verify all claims, identify questionable content
4. **Editing** - Polish prose, integrate wiki links, score quality
5. **Publishing** - Write the final `.md` file to the output directory

## Features

- **Multi-Agent Architecture** - Four specialized AI agents with role-appropriate temperature settings
- **Automatic Revision Loop** - Fact checker can trigger re-drafting for quality issues (up to 3 cycles)
- **Human Approval Workflow** - Optional review checkpoints at each phase boundary
- **Quality Scoring** - Editor assigns 0.0-1.0 quality scores with configurable thresholds
- **Wiki Integration** - Automatic internal linking to existing pages, CamelCase naming
- **Comprehensive Fact Checking** - Every claim verified, confidence levels, recommended actions
- **Pipeline Monitoring** - Event system for tracking progress and metrics
- **Flexible API Key Management** - Environment variable, CLI flag, or key file

## Requirements

- Java 21 or later
- Maven 3.8+
- Anthropic API key ([Get one here](https://console.anthropic.com/settings/keys))

## Quick Start

### 1. Build the Project

```bash
git clone https://github.com/jakefearsd/aipublisher.git
cd aipublisher
mvn clean package -DskipTests
```

### 2. Set Your API Key

Choose one of these methods:

```bash
# Option 1: Environment variable (recommended)
export ANTHROPIC_API_KEY='your-api-key-here'

# Option 2: Command line flag
java -jar target/aipublisher.jar -t "Topic" -k "your-api-key-here"

# Option 3: Key file
echo "your-api-key-here" > ~/.anthropic-key
java -jar target/aipublisher.jar -t "Topic" --key-file ~/.anthropic-key
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

## Command Line Reference

```
Usage: aipublisher [-hqvV] [--auto-approve] [-a=<audience>] [-k=<apiKey>]
                   [--key-file=<keyFile>] [-o=<outputDirectory>] [-t=<topic>]
                   [-w=<wordCount>] [--related=<relatedPages>]...
                   [--sections=<requiredSections>]...

Generate well-researched, fact-checked articles using AI agents.

Options:
  -t, --topic=<topic>        Topic to write about (prompts interactively if not
                               specified)
  -a, --audience=<audience>  Target audience for the article
                               (default: general readers)
  -w, --words=<wordCount>    Target word count (default: 800)
  -o, --output=<outputDirectory>
                             Output directory for generated articles
                               (default: ./output)
      --sections=<sec1,sec2> Required sections (comma-separated)
      --related=<page1,page2>
                             Related pages for internal linking (comma-separated)
      --auto-approve         Skip all approval prompts (for scripting)
  -q, --quiet                Suppress non-essential output
  -v, --verbose              Enable verbose output
  -h, --help                 Show this help message and exit.
  -V, --version              Print version information and exit.

API Key Options:
  -k, --key=<apiKey>         Anthropic API key (overrides environment variable)
      --key-file=<keyFile>   Path to file containing Anthropic API key
      ANTHROPIC_API_KEY      Environment variable (default)

Examples:
  aipublisher -t "Apache Kafka"
  aipublisher --topic "Machine Learning" --audience "beginners" --words 1500
  aipublisher -t "Docker" -a "DevOps engineers" -w 1000 --auto-approve
  aipublisher -t "Kubernetes" -k sk-ant-api03-xxxxx
  aipublisher -t "Kubernetes" --key-file ~/.anthropic-key
```

## Configuration

Configuration is managed through `src/main/resources/application.properties`:

### Claude API Settings

```properties
# API Configuration
anthropic.api.key=${ANTHROPIC_API_KEY}
anthropic.model=claude-sonnet-4-20250514
anthropic.max-tokens=4096

# Temperature settings per agent (0.0=deterministic, 1.0=creative)
anthropic.temperature.research=0.3
anthropic.temperature.writer=0.7
anthropic.temperature.factchecker=0.1
anthropic.temperature.editor=0.5
```

### Pipeline Settings

```properties
# Maximum revision cycles before failing
pipeline.max-revision-cycles=3

# Timeout per phase
pipeline.phase-timeout=PT5M

# Human approval checkpoints (enable/disable each)
pipeline.approval.after-research=false
pipeline.approval.after-draft=false
pipeline.approval.after-factcheck=false
pipeline.approval.before-publish=true
```

### Output Settings

```properties
# Where to write generated articles
output.directory=./output
output.file-extension=.md
```

### Quality Thresholds

```properties
# Minimum fact check confidence to proceed
quality.min-factcheck-confidence=MEDIUM

# Minimum editor quality score to publish
quality.min-editor-score=0.8
```

## Architecture

### Document State Machine

Documents progress through these states:

```
CREATED --> RESEARCHING --> DRAFTING --> FACT_CHECKING --> EDITING --> PUBLISHED
                 ^              |               |
                 |              v               v
                 +------ REVISION LOOP --------+

Any state --> AWAITING_APPROVAL --> Continue or REJECTED
```

### Agent Outputs

Each agent produces structured output that feeds into the next phase:

| Agent | Output | Key Fields |
|-------|--------|------------|
| Research | `ResearchBrief` | Key facts, sources, outline, glossary, uncertainties |
| Writer | `ArticleDraft` | Markdown content, summary, internal links, categories |
| Fact Checker | `FactCheckReport` | Verified claims, questionable items, confidence, action |
| Editor | `FinalArticle` | Polished content, metadata, quality score, edit summary |

### Fact Check Outcomes

The Fact Checker evaluates every claim and provides:

- **Claim Status**: VERIFIED, QUESTIONABLE, or flagged for consistency issues
- **Confidence Level**: HIGH, MEDIUM, or LOW
- **Recommended Action**:
  - `APPROVE` - Content is ready to proceed
  - `REVISE` - Send back to Writer for improvements
  - `REJECT` - Content cannot meet quality standards

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
│   ├── WriterAgent.java      # Article drafting
│   ├── FactCheckerAgent.java # Claim verification
│   ├── EditorAgent.java      # Final polish
│   └── AgentPrompts.java     # System prompts for all agents
│
├── pipeline/                 # Pipeline orchestration
│   ├── PublishingPipeline.java  # Main orchestrator
│   └── PipelineResult.java      # Result with metrics
│
├── document/                 # Domain models
│   ├── PublishingDocument.java  # Main entity with state
│   ├── TopicBrief.java          # Input specification
│   ├── ResearchBrief.java       # Research output
│   ├── ArticleDraft.java        # Writer output
│   ├── FactCheckReport.java     # Verification results
│   └── FinalArticle.java        # Publication-ready content
│
├── approval/                 # Human approval workflow
│   ├── ApprovalService.java     # Approval management
│   └── ApprovalCallback.java    # Pluggable handlers
│
├── output/                   # Output generation
│   └── WikiOutputService.java   # JSPWiki Markdown writer
│
├── monitoring/               # Pipeline observability
│   └── PipelineMonitoringService.java
│
├── config/                   # Spring configuration
│   ├── ClaudeConfig.java        # LangChain4j beans
│   └── PipelineProperties.java  # Pipeline settings
│
└── cli/                      # Command-line interface
    └── AiPublisherCommand.java  # Picocli CLI
```

## Example Session

Running the pipeline for "Apache Kafka" produces output like:

```
╔═══════════════════════════════════════════════════════════╗
║                      AI PUBLISHER                         ║
║     Generate well-researched articles with AI agents      ║
╚═══════════════════════════════════════════════════════════╝

Topic: Apache Kafka
Audience: developers new to event streaming
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

Phase 5: Publishing
  Output: ./output/ApacheKafka.md

════════════════════════════════════════════════════════════
SUCCESS! Article published.

Output: ./output/ApacheKafka.md
Quality score: 0.91
Word count: 1,523
Total time: 45,231 ms
════════════════════════════════════════════════════════════
```

## Development

### Running Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=PublishingPipelineTest

# Skip integration tests (require API key)
mvn test -DexcludeGroups=integration
```

### Building

```bash
# Build without tests
mvn clean package -DskipTests

# Build with tests (unit tests only)
mvn clean package
```

### Running from Source

```bash
# Using Maven
ANTHROPIC_API_KEY='your-key' mvn spring-boot:run -Dspring-boot.run.arguments="--topic='Your Topic'"
```

## Technology Stack

- **Java 21** - Modern Java with records, pattern matching
- **Spring Boot 3.3** - Application framework and dependency injection
- **LangChain4j 0.36** - LLM integration framework
- **Claude Sonnet 4** - Anthropic's AI model
- **Picocli 4.7** - Professional CLI framework
- **JUnit 5** - Testing framework
- **Mockito** - Mocking for unit tests

## License

This project is for educational purposes.

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## Acknowledgments

- Built with [Claude](https://claude.ai) by Anthropic
- Uses [LangChain4j](https://github.com/langchain4j/langchain4j) for LLM integration
- CLI powered by [Picocli](https://picocli.info/)
