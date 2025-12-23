# AI Publisher Architecture

This document describes the internal architecture and design of AI Publisher. For user documentation, see [README.md](README.md).

## Table of Contents

- [System Overview](#system-overview)
- [Agent Architecture](#agent-architecture)
- [Pipeline Flow](#pipeline-flow)
- [Package Structure](#package-structure)
- [Design Patterns](#design-patterns)
- [Document State Machine](#document-state-machine)
- [Agent Outputs](#agent-outputs)
- [LLM Integration](#llm-integration)
- [Web Search Integration](#web-search-integration)
- [JSPWiki Output Format](#jspwiki-output-format)
- [Configuration Reference](#configuration-reference)
- [Extension Points](#extension-points)

---

## System Overview

AI Publisher is a multi-agent content generation system built on Spring Boot 3.3 and LangChain4j. It orchestrates five specialized AI agents through a publishing pipeline to produce wiki-style articles with research, fact-checking, editing, and critique phases.

```
┌─────────────────────────────────────────────────────────────────────┐
│                           User Input                                 │
│              (Topic, Universe, or Interactive Session)               │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        CLI Layer (Picocli)                           │
│  ┌─────────────────────┐  ┌──────────────────┐  ┌───────────────┐   │
│  │   Single Article    │  │  Universe Mode   │  │  Stub/Gap     │   │
│  │   Generation        │  │  (batch from     │  │  Generation   │   │
│  │                     │  │   saved plan)    │  │               │   │
│  └─────────────────────┘  └──────────────────┘  └───────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Publishing Pipeline                             │
│  ┌──────────┐  ┌────────┐  ┌───────────┐  ┌────────┐  ┌──────────┐  │
│  │ Research │→ │ Writer │→ │FactCheck │→ │ Editor │→ │  Critic  │  │
│  │  Agent   │  │ Agent  │  │  Agent    │  │ Agent  │  │  Agent   │  │
│  └──────────┘  └────────┘  └───────────┘  └────────┘  └──────────┘  │
│       ↑              ↑            │             ↑           │        │
│       └──────────────┴────────────┴─────────────┴───────────┘        │
│                         REVISION LOOP                                │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
┌──────────────────────────────┐  ┌───────────────────────────────────┐
│       LLM Integration        │  │      Web Search (Optional)        │
│  ┌────────────────────────┐  │  │  ┌─────────────────────────────┐  │
│  │  Anthropic (Claude)    │  │  │  │  DuckDuckGo Search          │  │
│  │  Ollama (qwen3:14b)    │  │  │  │  Source Reliability         │  │
│  └────────────────────────┘  │  │  └─────────────────────────────┘  │
└──────────────────────────────┘  └───────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       Output (JSPWiki .txt)                          │
│                          ./output/<Topic>.txt                        │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Agent Architecture

The system uses five specialized AI agents, each with a distinct role and temperature setting optimized for its task:

| Agent | Role | Temperature | Purpose |
|-------|------|-------------|---------|
| **Research Agent** | Information Gatherer | 0.3 | Collects facts, sources, creates article outlines |
| **Writer Agent** | Technical Writer | 0.7 | Transforms research into JSPWiki articles |
| **Fact Checker Agent** | Quality Assurance | 0.1 | Verifies every claim against sources |
| **Editor Agent** | Final Polish | 0.5 | Refines prose, adds wiki links, scores quality |
| **Critic Agent** | Syntax & Quality Reviewer | 0.3 | Validates JSPWiki syntax, final QA |

### Agent Base Class

All agents extend `BaseAgent` which implements the Template Method pattern:

```java
public abstract class BaseAgent implements Agent {
    // Template method
    public PublishingDocument process(PublishingDocument document) {
        preProcess(document);
        doProcess(document);  // Subclass implements
        postProcess(document);
    }
}
```

### Agent Roles

```java
public enum AgentRole {
    RESEARCHER,
    WRITER,
    FACT_CHECKER,
    EDITOR,
    CRITIC
}
```

---

## Pipeline Flow

Documents progress through six phases with optional revision loops:

```
TopicBrief --> Research --> Draft --> Fact Check --> Edit --> Critique --> Publish
                              ^          |            ^         |
                              |          v            |         v
                              +-- REVISION LOOP ------+---------+
```

### Phase Details

1. **Research** - Gather key facts, sources, and create an outline
2. **Drafting** - Write the article in JSPWiki markup syntax
3. **Fact Checking** - Verify all claims, identify questionable content
4. **Editing** - Polish prose, integrate wiki links, score quality
5. **Critique** - Validate JSPWiki syntax, review structure
6. **Publishing** - Write the final `.txt` file to output directory

### Revision Loop

The pipeline supports automatic revision when quality thresholds aren't met:

- Fact Checker can trigger re-drafting if claims are unverifiable
- Critic can send back to Editor for syntax fixes
- Maximum revision cycles configurable (default: 3)

---

## Package Structure

```
com.jakefear.aipublisher
├── AiPublisherApplication.java      # Spring Boot entry point
│
├── agent/                           # AI agent implementations
│   ├── BaseAgent.java               # Template Method base class
│   ├── ResearchAgent.java           # Information gathering
│   ├── WriterAgent.java             # Article drafting (JSPWiki)
│   ├── FactCheckerAgent.java        # Claim verification
│   ├── EditorAgent.java             # Final polish
│   ├── CriticAgent.java             # Syntax & quality review
│   └── AgentPrompts.java            # System prompts for all agents
│
├── pipeline/                        # Pipeline orchestration
│   ├── PublishingPipeline.java      # Main orchestrator
│   └── PipelineResult.java          # Result with metrics
│
├── document/                        # Document models
│   ├── PublishingDocument.java      # Main entity with state
│   ├── DocumentState.java           # State machine enum
│   ├── TopicBrief.java              # Input specification
│   ├── ResearchBrief.java           # Research output
│   ├── ArticleDraft.java            # Writer output
│   ├── FactCheckReport.java         # Verification results
│   ├── FinalArticle.java            # Editor output
│   └── CriticReport.java            # Critic review results
│
├── domain/                          # Topic universe data model
│   ├── Topic.java                   # Wiki topic with status/priority
│   ├── TopicUniverse.java           # Complete domain container
│   ├── TopicRelationship.java       # Topic connections
│   ├── TopicStatus.java             # PROPOSED/ACCEPTED/REJECTED/etc.
│   ├── Priority.java                # MUST_HAVE/SHOULD_HAVE/etc.
│   ├── RelationshipType.java        # PREREQUISITE_OF/PART_OF/etc.
│   ├── ComplexityLevel.java         # BEGINNER/INTERMEDIATE/ADVANCED
│   ├── ScopeConfiguration.java      # Scope boundaries
│   └── TopicUniverseRepository.java # JSON persistence
│
├── content/                         # Content type handling
│   ├── ContentType.java             # CONCEPT, TUTORIAL, etc.
│   └── ContentTypeSelector.java     # Auto-detection from topic
│
├── gap/                             # Gap detection and stub generation
│   ├── GapDetector.java             # Finds broken wiki links
│   ├── GapConcept.java              # Gap concept model
│   └── StubGenerationService.java   # Generates stub pages
│
├── search/                          # Web search integration
│   ├── WebSearchService.java        # DuckDuckGo search
│   ├── SearchResult.java            # Search result model
│   ├── SearchProvider.java          # Provider interface
│   ├── SearchProviderRegistry.java  # Provider management
│   └── SourceReliability.java       # Source assessment
│
├── approval/                        # Human approval workflow
│   ├── ApprovalService.java         # Approval management
│   ├── ApprovalCallback.java        # Pluggable handlers
│   └── ApprovalDecision.java        # Approve/Reject/Changes
│
├── output/                          # Output generation
│   └── WikiOutputService.java       # JSPWiki file writer
│
├── monitoring/                      # Pipeline observability
│   ├── PipelineMonitoringService.java
│   └── PipelineEventListener.java   # Observer pattern
│
├── config/                          # Spring configuration
│   ├── ClaudeConfig.java            # LangChain4j/LLM beans
│   ├── PipelineProperties.java      # Pipeline settings
│   └── OutputProperties.java        # Output configuration
│
├── cli/                             # Command-line interface
│   ├── AiPublisherCommand.java      # Main Picocli command
│   ├── InteractiveSession.java      # Single-article interactive mode
│   └── strategy/                    # Content type strategies
│       ├── ContentTypeQuestionStrategy.java
│       └── ContentTypeQuestionStrategyRegistry.java
│
└── util/
    ├── PageNameUtils.java           # CamelCase conversion
    └── JsonParsingUtils.java        # JSON helpers
```

---

## Design Patterns

This codebase uses Gang of Four patterns extensively:

### Strategy Pattern
**Location:** `cli/strategy/`, `search/SearchProvider`

```java
public interface ContentTypeQuestionStrategy {
    Set<ContentType> getApplicableTypes();
    List<ContentTypeQuestion> getQuestions();
}
```

### State Pattern
**Location:** `document/DocumentState`

```java
public enum DocumentState {
    CREATED, RESEARCHING, DRAFTING, FACT_CHECKING,
    EDITING, CRITIQUING, PUBLISHED, REJECTED;

    public boolean canTransitionTo(DocumentState target) { }
    public DocumentState getNextInFlow() { }
}
```

### Template Method Pattern
**Location:** `agent/BaseAgent`

Common algorithm structure with varying implementation details per agent.

### Observer Pattern
**Location:** `monitoring/PipelineEventListener`

```java
@FunctionalInterface
public interface PipelineEventListener {
    void onEvent(PipelineEvent event);
}
```

### Builder Pattern
**Location:** `domain/Topic.Builder`, `document/TopicBrief.Builder`

```java
Topic topic = Topic.builder("Machine Learning")
    .withDescription("Introduction to ML concepts")
    .withComplexity(ComplexityLevel.INTERMEDIATE)
    .withPriority(Priority.HIGH)
    .build();
```

### Registry Pattern
**Location:** `cli/strategy/ContentTypeQuestionStrategyRegistry`, `search/SearchProviderRegistry`

Central lookup for typed implementations.

---

## Document State Machine

```
CREATED --> RESEARCHING --> DRAFTING --> FACT_CHECKING --> EDITING --> CRITIQUING --> PUBLISHED
                 ^              |               |              ^            |
                 |              v               v              |            v
                 +------- REVISION LOOP -------+---------------+-----------+

Any state --> AWAITING_APPROVAL --> Continue or REJECTED
```

### State Transitions

| From State | To State | Trigger |
|------------|----------|---------|
| CREATED | RESEARCHING | Pipeline starts |
| RESEARCHING | DRAFTING | Research complete |
| DRAFTING | FACT_CHECKING | Draft complete |
| FACT_CHECKING | EDITING | Claims verified |
| FACT_CHECKING | DRAFTING | Revision needed |
| EDITING | CRITIQUING | Editing complete |
| CRITIQUING | PUBLISHED | Quality approved |
| CRITIQUING | EDITING | Minor fixes needed |
| Any | AWAITING_APPROVAL | Approval checkpoint |
| Any | REJECTED | User/quality rejection |

---

## Agent Outputs

Each agent produces structured output that feeds into the next phase:

| Agent | Output Class | Key Fields |
|-------|--------------|------------|
| Research | `ResearchBrief` | Key facts, sources, outline, glossary, uncertainties |
| Writer | `ArticleDraft` | JSPWiki content, summary, internal links, categories |
| Fact Checker | `FactCheckReport` | Verified claims, questionable items, confidence, action |
| Editor | `FinalArticle` | Polished content, metadata, quality score, edit summary |
| Critic | `CriticReport` | Structure/syntax/readability scores, issues, suggestions |

### Fact Check Outcomes

| Action | Meaning |
|--------|---------|
| `APPROVE` | Content ready to proceed |
| `REVISE` | Send back to Writer for improvements |
| `REJECT` | Content cannot meet quality standards |

### Critic Review Scores

| Score Type | Purpose |
|------------|---------|
| Structure Score | Article organization, heading hierarchy |
| Syntax Score | JSPWiki markup correctness (critical) |
| Readability Score | Paragraph size, tone, clarity |
| Overall Score | Combined quality assessment |

---

## LLM Integration

### Provider Configuration

Supports two LLM providers via LangChain4j:

**Anthropic (Cloud)**
```properties
llm.provider=anthropic
anthropic.api.key=${ANTHROPIC_API_KEY}
anthropic.model=claude-sonnet-4-20250514
anthropic.max-tokens=4096
```

**Ollama (Local)**
```properties
llm.provider=ollama
ollama.base-url=http://localhost:11434
ollama.model=qwen3:14b
ollama.num-predict=4096
ollama.timeout=PT5M
```

### Temperature Settings

Each agent has an optimized temperature:

```properties
llm.temperature.research=0.3    # Factual accuracy
llm.temperature.writer=0.7      # Creative prose
llm.temperature.factchecker=0.3 # Precise verification
llm.temperature.editor=0.5      # Balanced refinement
llm.temperature.critic=0.3      # Consistent evaluation
```

### Model Beans

Spring beans are created per agent role with appropriate temperature:

```java
@Bean("researchChatModel")
public ChatLanguageModel researchChatModel() { }

@Bean("writerChatModel")
public ChatLanguageModel writerChatModel() { }

// ... etc
```

---

## Web Search Integration

Optional DuckDuckGo-based search for enhanced research and fact-checking.

### Source Reliability Levels

| Level | Description | Score | Examples |
|-------|-------------|-------|----------|
| OFFICIAL | Primary documentation | 1.0 | docs.oracle.com, kafka.apache.org |
| ACADEMIC | Peer-reviewed sources | 0.95 | arxiv.org, ACM, IEEE |
| AUTHORITATIVE | Major tech publishers | 0.85 | O'Reilly, Manning, InfoQ |
| REPUTABLE | Community with oversight | 0.7 | Wikipedia, Stack Overflow |
| COMMUNITY | User-generated content | 0.5 | Reddit, Quora, forums |
| UNCERTAIN | Cannot determine | 0.3 | Unknown sources |

### Search Provider Interface

```java
public interface SearchProvider {
    String getProviderName();
    List<SearchResult> search(String query);
    double validateTopic(String topic);
}
```

---

## JSPWiki Output Format

The pipeline generates articles in JSPWiki markup (not Markdown):

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

---

## Configuration Reference

### Pipeline Settings

```properties
# Maximum revision cycles before failing
pipeline.max-revision-cycles=3

# Timeout per phase
pipeline.phase-timeout=PT5M

# Skip phases for faster iteration
pipeline.skip-fact-check=false
pipeline.skip-critique=false

# Human approval checkpoints
pipeline.approval.after-research=false
pipeline.approval.after-draft=false
pipeline.approval.after-factcheck=false
pipeline.approval.before-publish=true
```

### Quality Thresholds

```properties
# Minimum fact check confidence to proceed
quality.min-factcheck-confidence=MEDIUM

# Minimum editor quality score to publish
quality.min-editor-score=0.8

# Minimum critic score to publish
quality.min-critic-score=0.8

# Require verified claims from fact checker
quality.require-verified-claims=true
```

### Output Settings

```properties
output.directory=./output
output.file-extension=.txt
```

---

## Extension Points

### Adding a New Agent

1. **Create test class** with nested structure
2. **Extend `BaseAgent`** implementing `doProcess()`
3. **Register in pipeline** with appropriate order

```java
public class NewAgent extends BaseAgent {
    public NewAgent(ChatLanguageModel model, String systemPrompt) {
        super(model, systemPrompt, AgentRole.NEW_ROLE);
    }

    @Override
    protected void doProcess(PublishingDocument document) { }
}
```

### Adding a Content Type Strategy

1. Implement `ContentTypeQuestionStrategy`
2. Register in `ContentTypeQuestionStrategyRegistry`

### Adding a Search Provider

1. Implement `SearchProvider` interface
2. Add `@Service` annotation
3. Auto-registered via `SearchProviderRegistry`

```java
@Service
public class MySearchService implements SearchProvider {
    @Override
    public String getProviderName() { return "mysearch"; }

    @Override
    public List<SearchResult> search(String query) { ... }

    @Override
    public double validateTopic(String topic) { ... }
}
```

---

## Testing Guidelines

### Test Organization

```java
class SomeClassTest {
    @Nested
    @DisplayName("MethodName")
    class MethodName {
        @Test
        void shouldDoSomethingWhenCondition() { }
    }
}
```

### Integration Tests

```java
@Tag("integration")
@EnabledIfLlmAvailable
class WriterAgentIntegrationTest { }
```

Run integration tests:
```bash
mvn test -Dgroups=integration
```

### Environment Configuration

Integration tests use Ollama by default:
- **Default URL:** `http://inference.jakefear.com:11434`
- **Default model:** `qwen3:14b`
- **Override:** Set `OLLAMA_BASE_URL` and `OLLAMA_MODEL` environment variables

---

## Related Projects

- **[aidiscovery](https://github.com/jakefearsd/aidiscovery)** - Topic universe discovery tool
- Both share the `TopicUniverse` JSON format at `~/.aipublisher/universes/`

---

## Development Guidelines

For detailed development philosophy, TDD practices, and coding conventions, see [CLAUDE.md](CLAUDE.md).

Key principles:
- **Test-Driven Development** - Write tests first
- **Incremental Progress** - Small, focused commits
- **Pattern Usage** - Prefer Gang of Four patterns
- **Interface Segregation** - Keep interfaces focused
