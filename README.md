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
- **Domain Discovery Mode** - Interactive AI-assisted session to build comprehensive topic universes
- **Human-in-the-Loop Curation** - AI suggests topics and relationships, you curate and refine
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

## Running Without Maven

Once built, AI Publisher runs as a standalone JAR with no Maven required. This section covers all the ways to run the application.

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
aipublisher --discover -c minimal

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

# Customized article
java -jar aipublisher.jar \
  --topic "Introduction to Docker" \
  --audience "developers new to containers" \
  --words 2000 \
  --output ./wiki/pages

# Scripted/CI usage (no prompts)
java -jar aipublisher.jar -t "GraphQL APIs" --auto-approve -q
```

**Domain Discovery Mode:**
```bash
# Interactive discovery (prompts for cost profile)
java -jar aipublisher.jar --discover

# Quick prototype (~5 minutes, ~$1 API cost)
java -jar aipublisher.jar --discover -c minimal

# Standard project (~15 minutes, ~$5 API cost)
java -jar aipublisher.jar --discover --cost-profile balanced

# Enterprise documentation (~30 minutes, ~$15 API cost)
java -jar aipublisher.jar --discover -c comprehensive
```

**Working with Output:**
```bash
# Custom output directory
java -jar aipublisher.jar -t "My Topic" -o ./docs/wiki

# Output structure
./output/
├── MyTopic.md           # Generated JSPWiki article
└── debug/               # Debug artifacts (if pipeline fails)
    └── MyTopic_draft.json
```

### JVM Options

For large wikis or extended sessions, you may want to adjust JVM memory:

```bash
# Increase heap for large discovery sessions
java -Xmx2g -jar aipublisher.jar --discover -c comprehensive

# Enable garbage collection logging for debugging
java -Xlog:gc -jar aipublisher.jar --discover

# Quiet mode for scripts
java -jar aipublisher.jar -t "Topic" --auto-approve -q 2>/dev/null
```

### Integration with Shell Scripts

```bash
#!/bin/bash
# generate-wiki.sh - Generate articles for multiple topics

TOPICS=("Apache Kafka" "Kubernetes" "Docker" "GraphQL")
OUTPUT_DIR="./wiki/pages"

for topic in "${TOPICS[@]}"; do
  echo "Generating: $topic"
  java -jar aipublisher.jar \
    --topic "$topic" \
    --output "$OUTPUT_DIR" \
    --auto-approve \
    --quiet
done

echo "Generated ${#TOPICS[@]} articles in $OUTPUT_DIR"
```

### Troubleshooting

**Java Version Issues:**
```bash
# Check Java version (requires 21+)
java -version

# Use specific Java version
/path/to/java21/bin/java -jar aipublisher.jar --help

# On macOS with multiple Java versions
/usr/libexec/java_home -v 21 --exec java -jar aipublisher.jar --help
```

**API Key Issues:**
```bash
# Verify API key is set
echo $ANTHROPIC_API_KEY

# Test with verbose mode
java -jar aipublisher.jar -t "Test" -v
```

**Permission Issues:**
```bash
# Make JAR executable
chmod +x aipublisher.jar

# Fix key file permissions
chmod 600 ~/.anthropic-key
```

## Domain Discovery Mode

Domain Discovery is an interactive session that helps you build a comprehensive **Topic Universe** - a structured plan for your entire wiki. Instead of generating articles one at a time, you work with AI to map out your entire domain, curate topics, define relationships, and identify gaps before generating any content.

### Why Use Discovery Mode?

- **Strategic Planning** - Map your entire knowledge domain before writing
- **Human-in-the-Loop** - AI suggests, you curate and refine
- **Relationship Mapping** - Define how topics connect (prerequisites, related concepts)
- **Gap Analysis** - AI identifies missing topics and coverage gaps
- **Generation Ordering** - Topological sort ensures prerequisites are written first
- **Scope Control** - Define what's in/out of scope, assumed knowledge

### Starting a Discovery Session

```bash
java -jar target/aipublisher.jar --discover
```

### Cost Profiles

Cost profiles control how thoroughly the AI explores your domain. Higher profiles generate more topics but consume more API credits.

| Profile | Topics | Rounds | Est. Discovery Cost | Est. Content Cost | Best For |
|---------|--------|--------|---------------------|-------------------|----------|
| **MINIMAL** | 2-4 | 1 | $0.50-2 | $5-15 | Prototyping, testing ideas, small personal wikis |
| **BALANCED** | 9-31 | 3 | $2-5 | $30-75 | Most wikis, documentation projects, team knowledge bases |
| **COMPREHENSIVE** | 25-150 | 5 | $5-15 | $100-250 | Enterprise documentation, complete technical references |

**Specify via CLI:**
```bash
# Quick prototype mode
java -jar target/aipublisher.jar --discover -c minimal

# Standard coverage (default if prompted)
java -jar target/aipublisher.jar --discover --cost-profile balanced

# Full enterprise coverage
java -jar target/aipublisher.jar --discover -c comprehensive
```

If you don't specify a cost profile on the command line, the session will prompt you to choose one before starting.

**Profile Settings Breakdown:**

| Setting | MINIMAL | BALANCED | COMPREHENSIVE |
|---------|---------|----------|---------------|
| Max expansion rounds | 1 | 3 | 5 |
| Topics per round | 2 | 3 | 5 |
| Suggestions per topic | 2-6 | 5-9 | 10-14 |
| Max complexity | Intermediate | Advanced | Expert |
| Word count multiplier | 0.6× | 1.0× | 1.5× |
| Gap analysis | Skipped | Enabled | Enabled |
| Relationship depth | Core only | Important | All |

### The 8 Discovery Phases

Discovery mode guides you through 8 phases to build a complete topic universe:

#### Phase 1: Seed Input
Provide your domain name and initial seed topics - the core subjects you definitely want to cover.

```
╔═══════════════════════════════════════════════════════════════════╗
║              AI PUBLISHER - DOMAIN DISCOVERY MODE                 ║
╚═══════════════════════════════════════════════════════════════════╝

What domain or subject area is this wiki about?

Examples:
  • Apache Kafka
  • Cloud Native Development
  • Machine Learning Operations

Domain name: Apache Kafka

Enter your initial seed topics (one per line, empty line to finish):
Seed topic 1: Kafka Producers
  Brief description: How to send messages to Kafka
Seed topic 2: Kafka Consumers
  Brief description: How to read messages from Kafka
Seed topic 3:

Which topic should be the main landing page?
  1. Kafka Producers
  2. Kafka Consumers
Selection [1]: 1

✓ Created domain 'Apache Kafka' with 2 seed topics
```

#### Phase 2: Scope Setup (Optional)
Define boundaries to help AI generate more relevant suggestions.

```
Configure scope? [Y/n/skip]: y

What knowledge should readers already have? (comma-separated)
Examples: Java programming, basic SQL, command line familiarity
Assumed knowledge: Java programming, basic distributed systems

What topics should be explicitly excluded? (comma-separated)
Out of scope: Kafka Streams (separate wiki), Kafka Connect

Any specific areas to prioritize? (comma-separated)
Focus areas: Production deployment, Performance tuning

Target audience description: Backend developers new to event streaming
```

#### Phase 3: Topic Expansion
AI analyzes your seed topics and suggests related topics. You curate each suggestion.

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Expanding from: Kafka Producers
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌──────────────────────────────────────────────────────────┐
│ 1/7: Producer Configuration                              │
├──────────────────────────────────────────────────────────┤
│ Essential settings for Kafka producers including acks,   │
│ retries, batch size, and compression options.            │
│ Category: component           Relevance: ████████░░ 80%  │
│ Type: Reference               Complexity: Intermediate   │
│ Why: Critical for production deployments                 │
└──────────────────────────────────────────────────────────┘

  [A]ccept  [R]eject  [D]efer  [M]odify  [S]kip rest  [Q]uit
  Decision: a
  ✓ Accepted

┌──────────────────────────────────────────────────────────┐
│ 2/7: Message Serialization                               │
├──────────────────────────────────────────────────────────┤
│ How to serialize messages using Avro, JSON, or Protobuf  │
│ Category: prerequisite        Relevance: ███████░░░ 70%  │
│ Type: Concept                 Complexity: Intermediate   │
│ Why: Understanding serialization is essential            │
└──────────────────────────────────────────────────────────┘

  [A]ccept  [R]eject  [D]efer  [M]odify  [S]kip rest  [Q]uit
  Decision: m

  Current name: Message Serialization
  New name [keep current]: Kafka Serialization Formats
  Current description: How to serialize messages using Avro...
  New description [keep current]:
  ✓ Modified and accepted
```

**Curation Options:**
- **Accept** - Add topic to universe as-is
- **Reject** - Skip this topic entirely
- **Defer** - Save to backlog for later consideration
- **Modify** - Change name/description before accepting
- **Skip rest** - Auto-accept high-relevance, defer low-relevance

#### Phase 4: Relationship Mapping
AI suggests how topics relate to each other. You confirm or modify relationships.

```
Analyzing relationships between your 12 topics...

Found 15 potential relationships. Review the important ones:

● Kafka Basics ──[PREREQUISITE_OF]──> Kafka Producers
  └─ Understanding core concepts is essential before producing
  [C]onfirm  [R]eject  [T]ype change: c
  ✓ Confirmed

◐ Producer Configuration ──[PART_OF]──> Kafka Producers
  └─ Configuration is a component of producer setup
  [C]onfirm  [R]eject  [T]ype change: t

  Select relationship type:
    1. Prerequisite Of
    2. Part Of ← current
    3. Example Of
    4. Related To
    5. Contrasts With
    6. Implements
    7. Supersedes
    8. Pairs With
  Selection: 4
  ✓ Confirmed as RELATED_TO
```

**Relationship Types:**
| Type | Meaning | Example |
|------|---------|---------|
| `PREREQUISITE_OF` | Must understand A before B | Java → Spring Boot |
| `PART_OF` | A is a component of B | Partitions → Topics |
| `EXAMPLE_OF` | A is an instance of B | Avro → Serialization |
| `RELATED_TO` | Related but neither prerequisite | Producers ↔ Consumers |
| `CONTRASTS_WITH` | Alternatives or opposites | Kafka vs RabbitMQ |
| `IMPLEMENTS` | A implements concept B | KafkaProducer → Producer API |
| `SUPERSEDES` | A replaces B | New API → Legacy API |
| `PAIRS_WITH` | Commonly used together | Producers + Schema Registry |

#### Phase 5: Gap Analysis
AI analyzes your topic coverage and identifies potential gaps.

```
Analyzing topic coverage for gaps...

Coverage Assessment:
  Coverage:      ████████░░ 80%
  Balance:       ███████░░░ 70%
  Connectedness: █████████░ 90%

Summary: Good coverage of producer topics but consumer side needs more depth.

Found 3 gaps to review:

🔴 [MISSING_PREREQUISITE] Consumer Group Coordination
   Resolution: Add topic explaining how consumer groups coordinate

   Add suggested topic 'Consumer Group Coordination'? [Y/n]: y
   ✓ Topic added

🟡 [COVERAGE_GAP] Error handling patterns not covered
   Resolution: Add troubleshooting guide for common producer errors

   Add suggested topic 'Producer Error Handling'? [Y/n]: y
   ✓ Topic added

🟢 [DEPTH_IMBALANCE] Security topics are shallow
   Resolution: Consider expanding authentication/authorization coverage

   Add suggested topic 'Kafka Security'? [Y/n]: n
   → Skipped
```

**Gap Severity Levels:**
- 🔴 **Critical** - Missing essential prerequisite or core concept
- 🟡 **Moderate** - Notable gap that should be addressed
- 🟢 **Minor** - Nice to have, can be addressed later

#### Phase 6: Depth Calibration (Optional)
Adjust word counts and complexity levels for each topic.

```
Would you like to adjust topic depths?
Calibrate depths? [y/N/skip]: y

Current topics and suggested word counts:

   1. Kafka Producers                Intermediate (1000 words)
   2. Producer Configuration         Intermediate (1000 words)
   3. Kafka Serialization Formats    Intermediate (1000 words)
   4. Consumer Group Coordination    Advanced (1500 words)
   ...

Enter topic number to adjust, or press Enter to finish:
Topic #: 4
  Current: Advanced (1500 words)
  New word count: 2000
  ✓ Updated

Topic #:
```

#### Phase 7: Prioritization
Assign generation priorities to control which topics are written first.

```
Review topic priorities:

  Priority levels:
    1. MUST_HAVE   - Essential, generate first
    2. SHOULD_HAVE - Important, generate second
    3. NICE_TO_HAVE - Optional, generate if time permits
    4. BACKLOG     - Future consideration

  MUST_HAVE (3 topics):
    • Kafka Producers
    • Kafka Consumers
    • Kafka Basics

  SHOULD_HAVE (8 topics):
    • Producer Configuration
    • Consumer Configuration
    • Kafka Serialization Formats
    ...

Adjust priorities? [y/N]: y

Enter topic name and new priority (e.g., 'Kafka Security 3'):
> Kafka Security 1
  ✓ Updated
>
```

#### Phase 8: Review
Final review before saving the topic universe.

```
╔═══════════════════════════════════════════════════════════════════╗
║                        DISCOVERY SUMMARY                          ║
╚═══════════════════════════════════════════════════════════════════╝

  Domain:        Apache Kafka
  Topics:        14 accepted
  Relationships: 23 mapped
  Backlog:       3 items

  Suggested generation order:
     1. Kafka Basics [MUST_HAVE]
     2. Kafka Producers [MUST_HAVE]
     3. Kafka Consumers [MUST_HAVE]
     4. Producer Configuration [SHOULD_HAVE]
     5. Consumer Configuration [SHOULD_HAVE]
     6. Kafka Serialization Formats [SHOULD_HAVE]
     7. Consumer Group Coordination [SHOULD_HAVE]
     8. Producer Error Handling [SHOULD_HAVE]
     9. Kafka Security [MUST_HAVE]
    10. Message Retention [SHOULD_HAVE]
    ... and 4 more

───────────────────────────────────────────────────────────────────

Finalize this topic universe? [Y/n]: y

✓ Topic universe finalized!

Session ID: a1b2c3d4

═══════════════════════════════════════════════════════════════════
Topic universe saved!

  ID:       kafka-wiki-2024-01
  Name:     Apache Kafka
  Topics:   14 accepted
  Location: ~/.aipublisher/universes/kafka-wiki-2024-01.universe.json

To generate articles from this universe, use:
  aipublisher --universe kafka-wiki-2024-01
═══════════════════════════════════════════════════════════════════
```

### Topic Universe Data Model

The discovery session creates a `TopicUniverse` - a structured representation of your wiki's content plan:

```
TopicUniverse
├── id: "kafka-wiki-2024-01"
├── name: "Apache Kafka"
├── description: "Comprehensive Kafka documentation for developers"
├── topics: [
│   ├── Topic {
│   │   id: "KafkaProducers"
│   │   name: "Kafka Producers"
│   │   status: ACCEPTED
│   │   priority: MUST_HAVE
│   │   contentType: TUTORIAL
│   │   complexity: INTERMEDIATE
│   │   estimatedWords: 1500
│   │   emphasize: ["performance", "error handling"]
│   │   skip: ["legacy APIs"]
│   │   isLandingPage: true
│   │   }
│   └── ...
│   ]
├── relationships: [
│   ├── TopicRelationship {
│   │   source: "KafkaBasics"
│   │   target: "KafkaProducers"
│   │   type: PREREQUISITE_OF
│   │   status: CONFIRMED
│   │   }
│   └── ...
│   ]
├── scope: {
│   assumedKnowledge: ["Java programming"]
│   outOfScope: ["Kafka Streams"]
│   focusAreas: ["Production deployment"]
│   audienceDescription: "Backend developers new to event streaming"
│   }
└── backlog: ["Kafka Connect Integration", ...]
```

### Saved Universe Location

Topic universes are saved to:
```
~/.aipublisher/universes/<universe-id>.universe.json
```

You can:
- View saved universes with any JSON viewer
- Edit them manually if needed
- Share them with team members
- Version control them alongside your wiki

### Best Practices for Discovery

1. **Start with 3-5 seed topics** - Don't try to enumerate everything upfront
2. **Be specific in descriptions** - Helps AI generate better suggestions
3. **Define scope early** - Prevents AI from suggesting off-topic content
4. **Use the backlog** - Defer interesting but non-essential topics
5. **Review relationships carefully** - They determine generation order
6. **Address critical gaps** - These are often missing prerequisites
7. **Prioritize ruthlessly** - MUST_HAVE should be your core content

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

Output example:
```
╔═══════════════════════════════════════════════════════════════════╗
║              AI PUBLISHER - GAP ANALYSIS MODE                     ║
╚═══════════════════════════════════════════════════════════════════╝

Analyzing wiki content for gaps...

Detected 8 gap concepts:

Definition (5):
  - Present Value [Definition] (referenced by: CompoundInterest, TimeValueOfMoney)
  - Discount Rate [Definition] (referenced by: CompoundInterest)
  - Principal [Definition] (referenced by: CompoundInterest, SimpleInterest)
  - Annual Percentage Rate [Definition] (referenced by: CompoundInterest)
  - Amortization [Definition] (referenced by: LoanBasics)

Redirect (1):
  - APR [Redirect] -> AnnualPercentageRate (referenced by: LoanBasics)

Full Article (1):
  - Inflation [Full Article] (referenced by: InvestingBasics, CompoundInterest)

Ignore (1):
  - money [Ignore]

═══════════════════════════════════════════════════════════════════
Total gaps: 8

To generate stubs for these gaps, use:
  aipublisher --stubs-only
  aipublisher --stubs-only --context "Finance"
═══════════════════════════════════════════════════════════════════
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

Output example:
```
╔═══════════════════════════════════════════════════════════════════╗
║              AI PUBLISHER - STUB GENERATION MODE                  ║
╚═══════════════════════════════════════════════════════════════════╝

Loaded universe: investing-basics
Domain context: Investing Basics
Target audience: general readers

Generate stub pages for gap concepts? [Y/n]: y

Analyzing wiki content for gaps...
Detected 8 gap concepts
  - Definitions to generate: 5
  - Redirects to create: 1
  - Full articles needed: 1 (flagged for review)
  - Ignored (too generic): 1

Generating definition stubs...
  ✓ Generated: PresentValue.txt
  ✓ Generated: DiscountRate.txt
  ✓ Generated: Principal.txt
  ✓ Generated: AnnualPercentageRate.txt
  ✓ Generated: Amortization.txt

Creating redirect pages...
  ✓ Generated: APR.txt

Gaps requiring full articles (add to universe for generation):
  - Inflation (referenced by: InvestingBasics, CompoundInterest)

═══════════════════════════════════════════════════════════════════
Stub generation complete!

  Gaps detected:    8
  Stubs generated:  5
  Redirects:        1
  Ignored:          1
  Flagged review:   1
═══════════════════════════════════════════════════════════════════
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

### Example Stub Page Output

**Definition stub (PresentValue.txt):**
```
!!! Present Value

__Present Value__ (PV) is the current worth of a future sum of money or stream of cash flows, given a specified rate of return. It represents how much a future payment is worth in today's dollars.

The concept is fundamental to financial analysis and investment decisions. A dollar received today is worth more than a dollar received in the future because of its earning potential.

!! See Also
* [CompoundInterest]
* [TimeValueOfMoney]
* [DiscountRate]

[{SET categories='Finance,InvestingConcepts'}]
```

**Redirect page (APR.txt):**
```
This page redirects to [AnnualPercentageRate].

If you are not automatically redirected, click the link above.

[{SET categories='Redirects'}]
```

### Stub Generation Workflow

The recommended workflow for comprehensive wiki coverage:

1. **Create a topic universe** (optional but recommended):
   ```bash
   java -jar target/aipublisher.jar --discover -c balanced
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

### Best Practices for Stub Generation

1. **Run analysis first** - Use `--analyze-gaps` to preview before generating
2. **Use universe context** - Use `--universe` to load domain name and audience from your saved universe
3. **Review redirects** - Verify redirect targets are correct
4. **Track full article flags** - Add significant gaps to your topic universe
5. **Iterate** - After adding stub pages, run analysis again to catch new gaps

## Command Line Reference

```
Usage: aipublisher [-hiqvV] [--auto-approve] [--discover] [--analyze-gaps]
                   [--stubs-only] [--generate-stubs] [-c=<costProfile>]
                   [-a=<audience>] [-k=<apiKey>] [--key-file=<keyFile>]
                   [-o=<outputDirectory>] [-t=<topic>] [-u=<universe>]
                   [-w=<wordCount>] [--context=<context>]
                   [--related=<relatedPages>]... [--sections=<requiredSections>]...

Generate well-researched, fact-checked articles using AI agents.

Options:
  -t, --topic=<topic>        Topic to write about (prompts interactively if not
                               specified)
      --discover             Launch interactive domain discovery session
  -u, --universe=<id>        Generate articles from a saved topic universe
  -c, --cost-profile=<profile>
                             Cost profile for discovery: MINIMAL, BALANCED, or
                               COMPREHENSIVE (prompts if not specified)
  -i, --interactive          Force interactive mode even with topic specified
  -a, --audience=<audience>  Target audience for the article
                               (default: general readers)
  -w, --words=<wordCount>    Target word count (default: 800)
  -o, --output=<outputDirectory>
                             Output directory for generated articles
                               (default: ./output)
      --context=<context>    Domain context for stub generation
      --sections=<sec1,sec2> Required sections (comma-separated)
      --related=<page1,page2>
                             Related pages for internal linking (comma-separated)
      --auto-approve         Skip all approval prompts (for scripting)
  -q, --quiet                Suppress non-essential output
  -v, --verbose              Enable verbose output
  -h, --help                 Show this help message and exit.
  -V, --version              Print version information and exit.

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

Cost Profiles (for --discover):
  MINIMAL                    Quick prototype, 2-4 topics, ~$0.50-2
  BALANCED                   Good coverage, 9-31 topics, ~$2-5 (default)
  COMPREHENSIVE              Full coverage, 25-150 topics, ~$5-15

Examples:
  aipublisher                                    # Interactive mode
  aipublisher --discover                         # Domain discovery (prompts for profile)
  aipublisher --discover -c minimal              # Quick prototype mode
  aipublisher --discover --cost-profile balanced # Standard coverage
  aipublisher -t "Apache Kafka"                  # Simple topic
  aipublisher --topic "Machine Learning" --audience "beginners" --words 1500
  aipublisher -t "Docker" -a "DevOps engineers" -w 1000 --auto-approve
  aipublisher -t "Kubernetes" -k sk-ant-api03-xxxxx
  aipublisher -t "Kubernetes" --key-file ~/.anthropic-key
  aipublisher -u my-wiki --generate-stubs        # Generate articles and stubs
  aipublisher --analyze-gaps -u my-wiki          # Report gaps with universe context
  aipublisher --stubs-only -u my-wiki            # Generate stubs with universe context
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
├── discovery/                # Domain discovery system
│   ├── DiscoverySession.java       # Session state management
│   ├── DiscoveryPhase.java         # 8-phase state machine
│   ├── TopicExpander.java          # AI topic suggestion service
│   ├── RelationshipSuggester.java  # AI relationship mapping
│   ├── GapAnalyzer.java            # Coverage gap analysis
│   ├── TopicSuggestion.java        # Suggested topic record
│   └── RelationshipSuggestion.java # Suggested relationship record
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
    ├── AiPublisherCommand.java         # Main CLI
    ├── InteractiveSession.java         # Single-article interactive mode
    └── DiscoveryInteractiveSession.java # Domain discovery CLI
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

If you prefer to run directly from source code (useful during development):

```bash
# Using Maven Spring Boot plugin
ANTHROPIC_API_KEY='your-key' mvn spring-boot:run \
  -Dspring-boot.run.arguments="--topic='Your Topic'"

# Discovery mode from source
ANTHROPIC_API_KEY='your-key' mvn spring-boot:run \
  -Dspring-boot.run.arguments="--discover -c minimal"

# With multiple arguments
ANTHROPIC_API_KEY='your-key' mvn spring-boot:run \
  -Dspring-boot.run.arguments="--topic='Docker',--audience='beginners',--words=1500"
```

Note: Running via Maven is slower due to compilation overhead. For regular use, build the JAR once and use `java -jar` as described in the "Running Without Maven" section.

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
