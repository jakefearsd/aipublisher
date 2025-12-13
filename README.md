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
6. **Publishing** - Write the final `.md` file to the output directory

## Features

- **Multi-Agent Architecture** - Five specialized AI agents with role-appropriate temperature settings
- **JSPWiki Syntax Output** - Native JSPWiki markup format (not Markdown) for wiki compatibility
- **Automatic Revision Loop** - Fact checker and critic can trigger re-drafting for quality issues
- **Syntax Validation** - Critic agent catches any Markdown syntax that should be JSPWiki
- **Human Approval Workflow** - Optional review checkpoints at each phase boundary
- **Quality Scoring** - Editor and Critic assign 0.0-1.0 quality scores with configurable thresholds
- **Wiki Integration** - Automatic internal linking to existing pages, CamelCase naming
- **Comprehensive Fact Checking** - Every claim verified, confidence levels, recommended actions
- **Web Search Integration** - Optional DuckDuckGo-based web search for research and verification
- **Source Reliability Assessment** - Automatic classification of source trustworthiness
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
anthropic.temperature.critic=0.3
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
output.file-extension=.md
```

### Quality Thresholds

```properties
# Minimum fact check confidence to proceed
quality.min-factcheck-confidence=MEDIUM

# Minimum editor quality score to publish
quality.min-editor-score=0.8

# Minimum critic score to publish (syntax validation threshold)
quality.min-critic-score=0.8
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
│   ├── FinalArticle.java        # Editor output
│   └── CriticReport.java        # Critic review results
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

Phase 5: Critique
  Structure: 0.92, Syntax: 0.98, Readability: 0.89
  Overall: 0.92, Action: APPROVE

Phase 6: Publishing
  Output: ./output/ApacheKafka.md

════════════════════════════════════════════════════════════
SUCCESS! Article published.

Output: ./output/ApacheKafka.md
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

# Include integration tests (requires API key)
ANTHROPIC_API_KEY='your-key' mvn test -Dgroups=integration

# Only integration tests
ANTHROPIC_API_KEY='your-key' mvn test -Dgroups=integration

# Skip integration tests explicitly
mvn test -DexcludeGroups=integration

# Specific test class
mvn test -Dtest=PublishingPipelineTest
```

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
