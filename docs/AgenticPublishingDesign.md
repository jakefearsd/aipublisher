# Agentic Publishing Framework: Software Design Document

## Executive Summary

This document describes the architecture and implementation plan for an AI-powered multi-agent publishing workflow system. The system orchestrates four specialized AI agents—Researcher, Writer, Fact Checker, and Editor—to collaboratively produce high-quality articles in JSPWiki-compatible Markdown format.

---

## Table of Contents

1. [Problem Statement](#1-problem-statement)
2. [System Overview](#2-system-overview)
3. [Agent Architecture](#3-agent-architecture)
4. [Document Model](#4-document-model)
5. [Workflow Pipeline](#5-workflow-pipeline)
6. [JSPWiki Output Format](#6-jspwiki-output-format)
7. [Technical Architecture](#7-technical-architecture)
8. [Data Flow](#8-data-flow)
9. [Error Handling & Recovery](#9-error-handling--recovery)
10. [Configuration & Extensibility](#10-configuration--extensibility)
11. [Implementation Plan](#11-implementation-plan)

---

## 1. Problem Statement

Creating high-quality wiki articles requires multiple distinct skills: research gathering, coherent writing, factual verification, and editorial polish. Traditionally, this requires either a single author wearing multiple hats (leading to inconsistent quality) or multiple human collaborators (expensive and slow).

**Goals:**
- Automate the multi-phase article creation process using specialized AI agents
- Maintain quality through separation of concerns (each agent has a focused role)
- Produce output compatible with JSPWiki's custom Markdown syntax
- Enable human oversight at phase transitions
- Support iterative refinement based on feedback

**Non-Goals:**
- Real-time collaborative editing
- Automatic publishing without human approval
- Support for non-JSPWiki output formats (initially)

---

## 2. System Overview

### 2.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        AI Publisher System                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐         │
│  │ Research │───▶│  Writer  │───▶│   Fact   │───▶│  Editor  │         │
│  │  Agent   │    │  Agent   │    │  Checker │    │  Agent   │         │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘         │
│       │              │               │               │                 │
│       ▼              ▼               ▼               ▼                 │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │                    Document State Machine                        │  │
│  │  [RESEARCH] → [DRAFT] → [FACT_CHECK] → [EDITING] → [PUBLISHED]  │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│                                │                                       │
│                                ▼                                       │
│                    ┌───────────────────────┐                          │
│                    │  JSPWiki Markdown     │                          │
│                    │  Output Generator     │                          │
│                    └───────────────────────┘                          │
│                                │                                       │
│                                ▼                                       │
│                         [Final .md File]                               │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Core Principles

1. **Single Responsibility**: Each agent has one job and does it well
2. **Stateful Documents**: Documents carry their full history through the pipeline
3. **Linear Progression**: Documents move forward through defined phases
4. **Human Checkpoints**: Optional approval gates between phases
5. **Audit Trail**: All agent contributions are logged and traceable

---

## 3. Agent Architecture

### 3.1 Agent Design Pattern

Each agent follows a common structure but with specialized prompts and behaviors:

```
┌─────────────────────────────────────────┐
│              Base Agent                  │
├─────────────────────────────────────────┤
│ - name: String                          │
│ - role: AgentRole                       │
│ - systemPrompt: String                  │
│ - model: ChatLanguageModel              │
├─────────────────────────────────────────┤
│ + process(document: Document): Document │
│ + validate(input: Document): boolean    │
│ + getContribution(): AgentContribution  │
└─────────────────────────────────────────┘
           △
           │
    ┌──────┴──────┬──────────────┬──────────────┐
    │             │              │              │
┌───┴───┐   ┌────┴────┐   ┌─────┴─────┐  ┌────┴────┐
│Research│   │ Writer  │   │FactChecker│  │ Editor  │
│ Agent  │   │ Agent   │   │   Agent   │  │ Agent   │
└────────┘   └─────────┘   └───────────┘  └─────────┘
```

### 3.2 Research Agent

**Purpose**: Gather, synthesize, and structure source material for article creation.

**Inputs:**
- Topic/title
- Target audience description
- Scope constraints (word count, depth level)
- Optional: existing source URLs, reference documents

**Outputs:**
- Structured research brief containing:
  - Key facts and statistics
  - Source citations with reliability assessment
  - Suggested article structure/outline
  - Related topics for internal linking
  - Terminology glossary

**System Prompt Essence:**
```
You are a research specialist. Your job is to gather comprehensive,
accurate information on the given topic. You must:
- Identify key facts, statistics, and concepts
- Note sources and assess their reliability
- Suggest a logical structure for presenting the information
- Identify related wiki pages for internal linking
- Flag any areas where information is uncertain or conflicting
```

**Behavioral Characteristics:**
- Conservative: flags uncertainty rather than guessing
- Source-focused: always attributes information
- Structure-oriented: organizes findings hierarchically

### 3.3 Writer Agent

**Purpose**: Transform research into coherent, engaging prose in the target format.

**Inputs:**
- Research brief from Research Agent
- Style guide parameters
- Target wiki page name
- Related page names for linking

**Outputs:**
- Complete article draft in JSPWiki Markdown format
- Metadata (title, summary, author attribution)
- Internal link annotations
- Suggested categories/tags

**System Prompt Essence:**
```
You are a technical writer specializing in wiki content. Your job is to
transform research into clear, well-structured articles. You must:
- Write in an encyclopedic, neutral tone
- Use JSPWiki Markdown syntax correctly
- Create internal links using [PageName]() syntax
- Structure content with clear headings
- Include a table of contents for longer articles
- Write a concise summary for the page metadata
```

**Behavioral Characteristics:**
- Clarity-focused: prioritizes readability
- Format-aware: adheres strictly to JSPWiki syntax
- Link-conscious: creates meaningful internal connections

### 3.4 Fact Checker Agent

**Purpose**: Verify claims, check consistency, and flag potential issues.

**Inputs:**
- Draft article from Writer Agent
- Original research brief
- Access to verification tools (optional)

**Outputs:**
- Annotated article with verification status per claim
- List of verified facts (with confidence level)
- List of unverifiable or questionable claims
- Suggested corrections or clarifications
- Consistency report (internal contradictions)

**System Prompt Essence:**
```
You are a fact-checker and quality assurance specialist. Your job is to
verify the accuracy and consistency of article content. You must:
- Identify every factual claim in the article
- Assess each claim against the provided research
- Flag claims that lack source support
- Check for internal consistency
- Verify that links reference appropriate pages
- Rate overall factual confidence (HIGH/MEDIUM/LOW)
```

**Behavioral Characteristics:**
- Skeptical: assumes claims need verification
- Systematic: processes every assertion
- Non-editorial: focuses on facts, not style

### 3.5 Editor Agent

**Purpose**: Polish prose, ensure style consistency, integrate with existing wiki content, and prepare final output.

**Inputs:**
- Fact-checked article with annotations
- Style guide
- JSPWiki format requirements
- **Existing pages inventory** (list of .md files in target directory)

**Outputs:**
- Final polished article
- Edit summary (changes made)
- Quality assessment score
- Final metadata
- **Integrated internal links** to existing wiki pages

**System Prompt Essence:**
```
You are a senior editor preparing content for publication. Your job is to
polish the article to publication quality. You must:
- Improve clarity and flow without changing meaning
- Ensure consistent style and tone throughout
- Fix any grammar, spelling, or punctuation issues
- Verify all JSPWiki Markdown syntax is correct
- Ensure the article has proper structure (headings, TOC, links)
- Remove any fact-checker annotations from final output
- Prepare publication-ready metadata
- Review the list of existing wiki pages and add relevant internal links
  using [PageName]() syntax where the content naturally references those topics
```

**Behavioral Characteristics:**
- Conservative editor: improves without rewriting
- Format perfectionist: ensures valid output
- Quality gatekeeper: can reject if below standards
- **Link integrator**: connects new content to existing wiki pages

---

## 4. Document Model

### 4.1 Document State Machine

```
                    ┌─────────────────────────────────────────┐
                    │                                         │
                    ▼                                         │
┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   │   ┌──────────┐
│  CREATED │──▶│ RESEARCH │──▶│  DRAFT   │──▶│FACT_CHECK│───┼──▶│ EDITING  │
└──────────┘   └──────────┘   └──────────┘   └──────────┘   │   └──────────┘
                    │              │              │          │        │
                    │              │              │          │        ▼
                    │              │              ▼          │   ┌──────────┐
                    │              │         [REJECTED]──────┘   │PUBLISHED │
                    │              ▼                              └──────────┘
                    │         [REJECTED]─────────────────────────────┘
                    ▼
               [REJECTED]────────────────────────────────────────────┘
```

**States:**
- `CREATED`: Initial state, topic defined
- `RESEARCH`: Research agent is working/complete
- `DRAFT`: Writer agent is working/complete
- `FACT_CHECK`: Fact checker is working/complete
- `EDITING`: Editor is working/complete
- `PUBLISHED`: Final output generated
- `REJECTED`: Failed quality gate, returns to appropriate phase

### 4.2 Document Entity Structure

```java
public class PublishingDocument {
    // Identity
    private UUID id;
    private String pageName;           // CamelCase wiki page name
    private String title;              // Human-readable title

    // State
    private DocumentState state;
    private Instant createdAt;
    private Instant updatedAt;

    // Content at each phase
    private TopicBrief topicBrief;           // Initial input
    private ResearchBrief researchBrief;     // From Research Agent
    private ArticleDraft draft;               // From Writer Agent
    private FactCheckReport factCheckReport;  // From Fact Checker
    private FinalArticle finalArticle;        // From Editor

    // Metadata
    private DocumentMetadata metadata;

    // Audit trail
    private List<AgentContribution> contributions;

    // Quality tracking
    private QualityAssessment qualityAssessment;
}
```

### 4.3 Supporting Data Structures

```java
public record TopicBrief(
    String topic,
    String targetAudience,
    int targetWordCount,
    List<String> requiredSections,
    List<String> relatedPages,
    List<String> sourceUrls
) {}

public record ResearchBrief(
    List<KeyFact> keyFacts,
    List<SourceCitation> sources,
    List<String> suggestedOutline,
    List<String> relatedPageSuggestions,
    Map<String, String> glossary,
    List<String> uncertainAreas
) {}

public record ArticleDraft(
    String markdownContent,
    String summary,
    List<String> internalLinks,
    List<String> categories,
    Map<String, String> metadata
) {}

public record FactCheckReport(
    String annotatedContent,
    List<VerifiedClaim> verifiedClaims,
    List<QuestionableClaim> questionableClaims,
    List<String> consistencyIssues,
    ConfidenceLevel overallConfidence
) {}

public record FinalArticle(
    String markdownContent,
    DocumentMetadata metadata,
    String editSummary,
    QualityScore qualityScore
) {}

public record AgentContribution(
    AgentRole agent,
    Instant timestamp,
    String inputHash,
    String outputHash,
    Duration processingTime,
    Map<String, Object> metrics
) {}
```

---

## 5. Workflow Pipeline

### 5.1 Pipeline Orchestrator

The `PublishingPipeline` orchestrates document flow through agents:

```java
public class PublishingPipeline {
    private final ResearchAgent researchAgent;
    private final WriterAgent writerAgent;
    private final FactCheckerAgent factCheckerAgent;
    private final EditorAgent editorAgent;
    private final DocumentRepository repository;
    private final PipelineConfig config;

    public PublishingDocument process(TopicBrief brief) {
        PublishingDocument doc = createDocument(brief);

        doc = executePhase(doc, researchAgent, DocumentState.RESEARCH);
        doc = executePhase(doc, writerAgent, DocumentState.DRAFT);
        doc = executePhase(doc, factCheckerAgent, DocumentState.FACT_CHECK);
        doc = executePhase(doc, editorAgent, DocumentState.EDITING);

        return finalize(doc);
    }

    private PublishingDocument executePhase(
            PublishingDocument doc,
            Agent agent,
            DocumentState targetState) {

        if (config.requiresApproval(targetState)) {
            awaitHumanApproval(doc, targetState);
        }

        doc = agent.process(doc);
        doc.setState(targetState);
        repository.save(doc);

        if (!agent.validate(doc)) {
            return handleRejection(doc, agent);
        }

        return doc;
    }
}
```

### 5.2 Phase Transitions

Each phase transition includes:

1. **Pre-conditions check**: Verify previous phase completed successfully
2. **Input preparation**: Extract relevant data for the agent
3. **Agent execution**: Run the agent with timeout and retry logic
4. **Output validation**: Verify agent produced valid output
5. **State update**: Transition document to new state
6. **Persistence**: Save document state
7. **Notification**: Emit events for monitoring/UI

### 5.3 Rejection Handling

When an agent rejects a document (quality gate failure):

```
┌─────────────────────────────────────────────────────────────────┐
│                    Rejection Flow                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Fact Checker rejects due to unverifiable claims             │
│     → Return to DRAFT state                                     │
│     → Writer receives rejection reason + specific issues        │
│     → Writer revises, resubmits                                 │
│     → Maximum 3 revision cycles before human escalation         │
│                                                                 │
│  2. Editor rejects due to quality issues                        │
│     → Return to appropriate state based on issue type           │
│     → Factual issues → FACT_CHECK                               │
│     → Structural issues → DRAFT                                 │
│     → Research gaps → RESEARCH                                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.4 Human Checkpoints

Optional approval gates can be configured at any phase boundary:

```java
public record PipelineConfig(
    boolean approvalAfterResearch,
    boolean approvalAfterDraft,
    boolean approvalAfterFactCheck,
    boolean approvalBeforePublish,
    int maxRevisionCycles,
    Duration phaseTimeout
) {}
```

When approval is required:
1. Pipeline pauses
2. Document marked as `AWAITING_APPROVAL`
3. Notification sent to configured channel
4. Human reviews and approves/rejects/requests changes
5. Pipeline resumes or routes feedback to appropriate agent

---

## 6. JSPWiki Output Format

### 6.1 File Format

**File Extension:** `.md`

JSPWiki automatically detects Markdown files by their `.md` extension and routes them
to the Markdown parser. This is defined in `AbstractFileProvider.java`:

```java
public static final String MARKDOWN_EXT = ".md";
```

When JSPWiki encounters a `.md` file, it automatically:
1. Sets `markup.syntax=markdown` for the page
2. Uses `MarkdownParser` instead of the default wiki parser
3. Renders using `MarkdownRenderer`

**File Naming Convention:**
- CamelCase page names: `ApacheKafka.md`, `EventStreaming.md`
- The filename (without extension) becomes the linkable page name

### 6.2 Existing Pages Discovery

Before the Editor Agent runs, the system scans the target output directory for existing
`.md` files. These represent linkable wiki pages that can be referenced using
`[PageName]()` syntax.

```java
// Discover existing pages in target directory
Set<String> existingPages = LinkProcessor.discoverExistingPages(outputDirectory);
// Returns: {"ApacheKafka", "EventStreaming", "MessageQueue", ...}
```

The Editor Agent receives this list and is instructed to add relevant internal links
where the article content naturally references topics covered by existing pages.

### 6.3 Target Syntax

The system generates JSPWiki-compatible Markdown with these specific conventions:

#### Internal Links
```markdown
[PageName]()                    # Link to wiki page
[Display Text](PageName)        # Link with custom text
[PageName#section]()            # Link to section
```

#### Page Structure
```markdown
## Page Title

Summary paragraph for metadata extraction.

[{TableOfContents }]()

## Section Heading

Content with [InternalLinks]() and **formatting**.

### Subsection

More content here.

## See Also

* [RelatedPage]()
* [AnotherPage]()

## References

1. Source citation
2. Another source
```

#### Metadata Properties
Generated as page attributes:
- `markup.syntax=markdown` (automatically set by JSPWiki for .md files)
- `summary=<extracted summary>`
- `author=AI Publisher`
- `changenote=<edit summary>`

### 6.4 Output Generator

```java
public class JSPWikiMarkdownGenerator {

    public String generate(FinalArticle article) {
        StringBuilder sb = new StringBuilder();

        // Title
        sb.append("## ").append(article.metadata().title()).append("\n\n");

        // Summary paragraph (first paragraph becomes metadata summary)
        sb.append(article.metadata().summary()).append("\n\n");

        // Table of contents for longer articles
        if (article.wordCount() > 500) {
            sb.append("[{TableOfContents }]()\n\n");
        }

        // Main content
        sb.append(article.markdownContent());

        // Related pages section
        if (!article.relatedPages().isEmpty()) {
            sb.append("\n\n## See Also\n\n");
            for (String page : article.relatedPages()) {
                sb.append("* [").append(page).append("]()\n");
            }
        }

        return sb.toString();
    }

    public String generateFilename(String pageName) {
        // Convert to CamelCase, use .md extension for Markdown parser detection
        return toCamelCase(pageName) + ".md";
    }
}
```

### 6.5 Link Processing

Internal links are processed to ensure validity and integrate with existing wiki pages:

```java
public class LinkProcessor {
    private final Set<String> existingPages;

    /**
     * Scans the target output directory for existing .md files and builds
     * an inventory of linkable page names.
     */
    public static Set<String> discoverExistingPages(Path outputDirectory) {
        try (Stream<Path> files = Files.list(outputDirectory)) {
            return files
                .filter(p -> p.toString().endsWith(".md"))
                .map(p -> p.getFileName().toString())
                .map(name -> name.substring(0, name.length() - 3)) // Remove .md
                .collect(Collectors.toSet());
        }
    }

    public String processLinks(String content) {
        // Pattern: [text](target) or [text]()
        Pattern linkPattern = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]*)\\)");

        return linkPattern.matcher(content).replaceAll(match -> {
            String text = match.group(1);
            String target = match.group(2);

            if (target.isEmpty()) {
                // Empty target means text is the page name
                target = toCamelCase(text);
            }

            // Validate internal links
            if (!target.startsWith("http") && !existingPages.contains(target)) {
                // Mark as "create page" link (will render with createpage CSS class)
                // This is handled by JSPWiki automatically
            }

            return "[" + text + "](" + target + ")";
        });
    }

    /**
     * Returns the set of existing pages that can be linked to.
     * Used by the Editor Agent to suggest relevant links.
     */
    public Set<String> getExistingPages() {
        return Collections.unmodifiableSet(existingPages);
    }
}
```

---

## 7. Technical Architecture

### 7.1 Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AI Publisher                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         API Layer                                    │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────────┐  │   │
│  │  │  CLI Runner │  │ REST API    │  │ Event Listeners             │  │   │
│  │  │  (current)  │  │ (future)    │  │ (future)                    │  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      Service Layer                                   │   │
│  │  ┌───────────────────┐  ┌───────────────────┐  ┌─────────────────┐  │   │
│  │  │ PublishingService │  │ DocumentService   │  │ ExportService   │  │   │
│  │  │                   │  │                   │  │                 │  │   │
│  │  │ - createArticle() │  │ - save()          │  │ - toJSPWiki()   │  │   │
│  │  │ - getStatus()     │  │ - load()          │  │ - toFile()      │  │   │
│  │  │ - approve()       │  │ - search()        │  │                 │  │   │
│  │  │ - reject()        │  │ - history()       │  │                 │  │   │
│  │  └───────────────────┘  └───────────────────┘  └─────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                       Agent Layer                                    │   │
│  │  ┌──────────────┐ ┌────────────┐ ┌──────────────┐ ┌──────────────┐  │   │
│  │  │ResearchAgent │ │WriterAgent │ │FactCheckAgent│ │ EditorAgent  │  │   │
│  │  └──────────────┘ └────────────┘ └──────────────┘ └──────────────┘  │   │
│  │                          │                                          │   │
│  │                          ▼                                          │   │
│  │              ┌─────────────────────┐                                │   │
│  │              │  AgentOrchestrator  │                                │   │
│  │              │  (Pipeline Control) │                                │   │
│  │              └─────────────────────┘                                │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                     Infrastructure Layer                             │   │
│  │  ┌───────────────────┐  ┌───────────────────┐  ┌─────────────────┐  │   │
│  │  │ LangChain4j       │  │ Document          │  │ JSPWiki         │  │   │
│  │  │ Claude Client     │  │ Repository        │  │ Markdown Gen    │  │   │
│  │  │                   │  │ (File/DB)         │  │                 │  │   │
│  │  └───────────────────┘  └───────────────────┘  └─────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 7.2 Package Structure

```
com.jakefear.aipublisher/
├── AiPublisherApplication.java
├── agent/
│   ├── Agent.java                    # Base agent interface
│   ├── AgentRole.java                # Enum: RESEARCH, WRITER, FACT_CHECK, EDITOR
│   ├── AgentContribution.java        # Record for audit trail
│   ├── BaseAgent.java                # Abstract base implementation
│   ├── ResearchAgent.java
│   ├── WriterAgent.java
│   ├── FactCheckerAgent.java
│   └── EditorAgent.java
├── document/
│   ├── PublishingDocument.java       # Main document entity
│   ├── DocumentState.java            # State enum
│   ├── TopicBrief.java
│   ├── ResearchBrief.java
│   ├── ArticleDraft.java
│   ├── FactCheckReport.java
│   ├── FinalArticle.java
│   └── DocumentMetadata.java
├── pipeline/
│   ├── PublishingPipeline.java       # Main orchestrator
│   ├── PipelineConfig.java
│   ├── PhaseExecutor.java
│   └── RejectionHandler.java
├── output/
│   ├── JSPWikiMarkdownGenerator.java
│   ├── LinkProcessor.java
│   └── FileExporter.java
├── repository/
│   ├── DocumentRepository.java       # Interface
│   └── FileDocumentRepository.java   # File-based implementation
├── config/
│   ├── ClaudeConfig.java             # LangChain4j configuration
│   ├── AgentPrompts.java             # System prompts
│   └── PipelineProperties.java
└── cli/
    └── PublishingRunner.java         # CLI entry point
```

### 7.3 LangChain4j Integration

```java
@Configuration
public class ClaudeConfig {

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.model:claude-sonnet-4-20250514}")
    private String modelName;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(4096)
                .temperature(0.7)
                .build();
    }

    @Bean
    public ResearchAgent researchAgent(ChatLanguageModel model) {
        return new ResearchAgent(model, AgentPrompts.RESEARCH_SYSTEM_PROMPT);
    }

    @Bean
    public WriterAgent writerAgent(ChatLanguageModel model) {
        return new WriterAgent(model, AgentPrompts.WRITER_SYSTEM_PROMPT);
    }

    @Bean
    public FactCheckerAgent factCheckerAgent(ChatLanguageModel model) {
        return new FactCheckerAgent(model, AgentPrompts.FACT_CHECKER_SYSTEM_PROMPT);
    }

    @Bean
    public EditorAgent editorAgent(ChatLanguageModel model) {
        return new EditorAgent(model, AgentPrompts.EDITOR_SYSTEM_PROMPT);
    }
}
```

---

## 8. Data Flow

### 8.1 Complete Flow Example

```
User Input: "Write an article about Apache Kafka for developers new to event streaming"

┌─────────────────────────────────────────────────────────────────────────────┐
│ PHASE 1: RESEARCH                                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│ Input:                                                                      │
│   TopicBrief {                                                              │
│     topic: "Apache Kafka for beginners"                                     │
│     targetAudience: "developers new to event streaming"                     │
│     targetWordCount: 1500                                                   │
│     relatedPages: ["EventStreaming", "MessageQueue", "ApacheZooKeeper"]    │
│   }                                                                         │
│                                                                             │
│ Research Agent Output:                                                      │
│   ResearchBrief {                                                           │
│     keyFacts: [                                                             │
│       "Kafka is distributed event streaming platform",                      │
│       "Created at LinkedIn, open-sourced 2011",                             │
│       "Topics, partitions, consumer groups core concepts",                  │
│       ...                                                                   │
│     ]                                                                       │
│     sources: [                                                              │
│       {url: "kafka.apache.org", reliability: HIGH},                         │
│       ...                                                                   │
│     ]                                                                       │
│     suggestedOutline: [                                                     │
│       "What is Kafka?",                                                     │
│       "Core Concepts",                                                      │
│       "Use Cases",                                                          │
│       "Getting Started",                                                    │
│       "Next Steps"                                                          │
│     ]                                                                       │
│   }                                                                         │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ PHASE 2: WRITING                                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│ Input: ResearchBrief + JSPWiki format requirements                          │
│                                                                             │
│ Writer Agent Output:                                                        │
│   ArticleDraft {                                                            │
│     markdownContent: """                                                    │
│       ## Apache Kafka                                                       │
│                                                                             │
│       Apache Kafka is a distributed event streaming platform...             │
│                                                                             │
│       [{TableOfContents }]()                                                │
│                                                                             │
│       ## What is Event Streaming?                                           │
│                                                                             │
│       [EventStreaming]() is a paradigm where...                             │
│       ...                                                                   │
│     """                                                                     │
│     summary: "Introduction to Apache Kafka for developers..."               │
│     internalLinks: ["EventStreaming", "MessageQueue", "ApacheZooKeeper"]   │
│   }                                                                         │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ PHASE 3: FACT CHECK                                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│ Input: ArticleDraft + ResearchBrief                                         │
│                                                                             │
│ Fact Checker Output:                                                        │
│   FactCheckReport {                                                         │
│     verifiedClaims: [                                                       │
│       {claim: "Created at LinkedIn", status: VERIFIED, source: "..."},      │
│       {claim: "Open-sourced 2011", status: VERIFIED, source: "..."},        │
│       ...                                                                   │
│     ]                                                                       │
│     questionableClaims: [                                                   │
│       {claim: "Handles millions of messages per second",                    │
│        issue: "Depends on configuration, needs qualification"}              │
│     ]                                                                       │
│     overallConfidence: HIGH                                                 │
│   }                                                                         │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ PHASE 4: EDITING                                                            │
├─────────────────────────────────────────────────────────────────────────────┤
│ Input: ArticleDraft + FactCheckReport                                       │
│                                                                             │
│ Editor Agent Output:                                                        │
│   FinalArticle {                                                            │
│     markdownContent: """                                                    │
│       ## Apache Kafka                                                       │
│                                                                             │
│       Apache Kafka is a distributed event streaming platform that           │
│       enables applications to publish, subscribe to, store, and             │
│       process streams of records in real-time.                              │
│                                                                             │
│       [{TableOfContents }]()                                                │
│                                                                             │
│       ## What is Event Streaming?                                           │
│       ...                                                                   │
│     """                                                                     │
│     metadata: {                                                             │
│       title: "Apache Kafka",                                                │
│       summary: "Introduction to Apache Kafka...",                           │
│       author: "AI Publisher"                                                │
│     }                                                                       │
│     qualityScore: 0.92                                                      │
│   }                                                                         │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ OUTPUT: ApacheKafka.md                                                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 9. Error Handling & Recovery

### 9.1 Error Categories

| Category | Examples | Handling Strategy |
|----------|----------|-------------------|
| API Errors | Rate limits, timeouts, auth failures | Exponential backoff, retry up to 3x |
| Agent Errors | Invalid output format, empty response | Retry with clarified prompt |
| Quality Failures | Fact check rejection, low quality score | Route to revision cycle |
| System Errors | File I/O, memory issues | Log, alert, graceful degradation |

### 9.2 Retry Configuration

```java
public record RetryConfig(
    int maxAttempts,           // Default: 3
    Duration initialDelay,     // Default: 1 second
    double backoffMultiplier,  // Default: 2.0
    Duration maxDelay          // Default: 30 seconds
) {}
```

### 9.3 State Recovery

Documents are persisted after each phase, enabling recovery:

```java
public class PipelineRecovery {

    public PublishingDocument recover(UUID documentId) {
        PublishingDocument doc = repository.load(documentId);

        // Resume from last completed state
        return switch (doc.getState()) {
            case RESEARCH -> pipeline.resumeFromResearch(doc);
            case DRAFT -> pipeline.resumeFromDraft(doc);
            case FACT_CHECK -> pipeline.resumeFromFactCheck(doc);
            case EDITING -> pipeline.resumeFromEditing(doc);
            default -> doc;
        };
    }
}
```

---

## 10. Configuration & Extensibility

### 10.1 Application Properties

```properties
# Claude API Configuration
anthropic.api.key=${ANTHROPIC_API_KEY}
anthropic.model=claude-sonnet-4-20250514
anthropic.max-tokens=4096
anthropic.temperature=0.7

# Pipeline Configuration
pipeline.approval.after-research=false
pipeline.approval.after-draft=false
pipeline.approval.after-fact-check=false
pipeline.approval.before-publish=true
pipeline.max-revision-cycles=3
pipeline.phase-timeout=PT5M

# Output Configuration
output.directory=./output
output.format=jspwiki-markdown
output.file-extension=.md

# Quality Thresholds
quality.min-fact-check-confidence=MEDIUM
quality.min-editor-score=0.8
```

### 10.2 Extension Points

The system is designed for extensibility:

1. **Custom Agents**: Implement `Agent` interface for new roles
2. **Output Formats**: Implement `OutputGenerator` for non-JSPWiki formats
3. **Storage Backends**: Implement `DocumentRepository` for database storage
4. **Quality Metrics**: Implement `QualityAssessor` for custom scoring
5. **Notification Hooks**: Implement `PipelineListener` for events

```java
public interface Agent {
    PublishingDocument process(PublishingDocument document);
    boolean validate(PublishingDocument document);
    AgentRole getRole();
}

public interface OutputGenerator {
    String generate(FinalArticle article);
    String getFileExtension();
}

public interface PipelineListener {
    void onPhaseStarted(PublishingDocument doc, DocumentState phase);
    void onPhaseCompleted(PublishingDocument doc, DocumentState phase);
    void onRejection(PublishingDocument doc, DocumentState phase, String reason);
    void onPublished(PublishingDocument doc);
}
```

---

## 11. Implementation Plan

This plan is optimized for implementation by Claude Code, organizing work into larger
coherent units that produce complete, testable features. The approach favors vertical
slices (complete features) over horizontal layers (all interfaces, then all implementations).

### Design Principles

1. **Configuration-first**: Establish all properties before building components
2. **Vertical slices**: Each step produces working, testable functionality
3. **Template-then-replicate**: Build one agent well, then apply the pattern to others
4. **Cohesive packages**: Complete one package fully before moving to the next
5. **Tests with implementation**: Write tests alongside code, not as a separate phase

### Session Structure

The implementation is organized into focused sessions, each producing deployable increments:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         IMPLEMENTATION ROADMAP                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  SESSION 1: Foundation                                                      │
│  ├── Step 1: Configuration & Data Model                                     │
│  └── Step 2: Agent Framework + Research Agent                               │
│                                                                             │
│  SESSION 2: Agent Suite                                                     │
│  └── Step 3: Writer, Fact Checker, and Editor Agents                        │
│                                                                             │
│  SESSION 3: Pipeline Integration                                            │
│  ├── Step 4: Output System                                                  │
│  ├── Step 5: Pipeline Orchestration                                         │
│  └── Step 6: CLI & Integration                                              │
│                                                                             │
│  SESSION 4: Polish (As Needed)                                              │
│  ├── Step 7: Human Approval Workflow                                        │
│  └── Step 8: Monitoring & Documentation                                     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### Session 1: Foundation

#### Step 1: Configuration & Data Model

Establish all configuration and the complete document model in a single cohesive unit.
This creates the "vocabulary" used by all subsequent components.

**Configuration (`config` package)**
- [ ] Create `config` package
- [ ] Implement `ClaudeConfig` class with LangChain4j bean definitions
- [ ] Implement `PipelineProperties` class for pipeline settings
- [ ] Implement `OutputProperties` class for output settings
- [ ] Update `application.properties` with comprehensive settings:
  ```properties
  # Claude API
  anthropic.api.key=${ANTHROPIC_API_KEY}
  anthropic.model=claude-sonnet-4-20250514
  anthropic.max-tokens=4096
  anthropic.temperature.research=0.3
  anthropic.temperature.writer=0.7
  anthropic.temperature.factchecker=0.1
  anthropic.temperature.editor=0.5

  # Pipeline
  pipeline.max-revision-cycles=3
  pipeline.phase-timeout=PT5M
  pipeline.approval.after-research=false
  pipeline.approval.after-draft=false
  pipeline.approval.after-factcheck=false
  pipeline.approval.before-publish=true

  # Output
  output.directory=./output
  output.file-extension=.md

  # Quality
  quality.min-factcheck-confidence=MEDIUM
  quality.min-editor-score=0.8
  ```

**Document Model (`document` package)**
- [ ] Create `document` package
- [ ] Implement `DocumentState` enum with state transition validation:
  - States: CREATED, RESEARCHING, DRAFTING, FACT_CHECKING, EDITING, PUBLISHED, REJECTED
  - Include `canTransitionTo(DocumentState next)` method
- [ ] Implement all data records:
  - `TopicBrief` - initial input from user
  - `ResearchBrief` - output from Research Agent
  - `ArticleDraft` - output from Writer Agent
  - `FactCheckReport` - output from Fact Checker Agent
  - `FinalArticle` - output from Editor Agent
  - `DocumentMetadata` - title, summary, author, timestamps
  - `AgentContribution` - audit trail entry
  - `QualityAssessment` - scores and metrics
- [ ] Implement `PublishingDocument` main entity with:
  - All phase content fields
  - State machine logic
  - Contribution history
- [ ] Add unit tests for:
  - State transition validation
  - Document lifecycle

**Deliverable**: Complete data model and configuration. All subsequent components
can be built with proper externalization from the start.

---

#### Step 2: Agent Framework + Research Agent

Build the complete agent infrastructure and validate it with a working Research Agent.

**Agent Framework (`agent` package)**
- [ ] Create `agent` package
- [ ] Define `Agent` interface:
  ```java
  public interface Agent {
      PublishingDocument process(PublishingDocument document);
      boolean validate(PublishingDocument document);
      AgentRole getRole();
  }
  ```
- [ ] Define `AgentRole` enum: RESEARCHER, WRITER, FACT_CHECKER, EDITOR
- [ ] Create `AgentPrompts` class with ALL four system prompts as constants
- [ ] Implement `BaseAgent` abstract class with:
  - LangChain4j ChatLanguageModel integration
  - Retry logic with exponential backoff
  - JSON response parsing utilities
  - Common validation helpers
  - Contribution recording

**Research Agent**
- [ ] Implement `ResearchAgent` extending `BaseAgent`:
  - System prompt from `AgentPrompts.RESEARCH`
  - `buildUserPrompt(TopicBrief)` method
  - `parseResponse(String json)` → `ResearchBrief`
  - Validation: requires keyFacts, suggestedOutline
- [ ] Add unit tests for:
  - Prompt construction
  - JSON parsing with valid input
  - JSON parsing with malformed input
  - Validation logic
- [ ] Add integration test:
  - Real Claude API call with simple topic
  - Verify ResearchBrief is populated

**Deliverable**: Working agent framework validated by a Research Agent that can
call Claude and return structured data. This establishes the pattern for all agents.

---

### Session 2: Agent Suite

#### Step 3: Writer, Fact Checker, and Editor Agents

Apply the established agent pattern to implement the remaining three agents.

**Writer Agent**
- [ ] Implement `WriterAgent` extending `BaseAgent`:
  - System prompt from `AgentPrompts.WRITER`
  - `buildUserPrompt(ResearchBrief, String pageName)` method
  - `parseResponse(String json)` → `ArticleDraft`
  - JSPWiki Markdown format enforcement
  - Validation: requires markdownContent, summary
- [ ] Add unit tests for prompt construction, parsing, validation
- [ ] Add integration test with Claude API

**Fact Checker Agent**
- [ ] Implement `FactCheckerAgent` extending `BaseAgent`:
  - System prompt from `AgentPrompts.FACT_CHECKER`
  - `buildUserPrompt(ArticleDraft, ResearchBrief)` method
  - `parseResponse(String json)` → `FactCheckReport`
  - Confidence level assessment
  - Validation: requires overallConfidence, recommendedAction
- [ ] Add unit tests for prompt construction, parsing, validation
- [ ] Add integration test with Claude API

**Editor Agent**
- [ ] Implement `EditorAgent` extending `BaseAgent`:
  - System prompt from `AgentPrompts.EDITOR`
  - `buildUserPrompt(ArticleDraft, FactCheckReport, Set<String> existingPages)` method
  - `parseResponse(String json)` → `FinalArticle`
  - Existing page link integration
  - Quality scoring
  - Validation: requires markdownContent, qualityScore >= threshold
- [ ] Add unit tests for prompt construction, parsing, validation, link integration
- [ ] Add integration test with Claude API

**Deliverable**: All four agents working independently. Each can process input
and produce structured output via Claude API calls.

---

### Session 3: Pipeline Integration

#### Step 4: Output System

Build the complete output generation system.

**Output Package (`output` package)**
- [ ] Create `output` package
- [ ] Implement `LinkProcessor`:
  - `discoverExistingPages(Path directory)` → `Set<String>`
  - `processLinks(String content, Set<String> existingPages)` → validated content
  - CamelCase conversion utilities
- [ ] Implement `JSPWikiMarkdownGenerator`:
  - `generate(FinalArticle article)` → complete Markdown string
  - Table of contents insertion for long articles
  - "See Also" section generation
  - Metadata comment block generation
- [ ] Implement `FileExporter`:
  - `export(FinalArticle article, Path outputDirectory)` → written file path
  - Filename generation (CamelCase + .md)
  - Directory creation if needed
- [ ] Add unit tests for:
  - Existing page discovery
  - Link processing and validation
  - Markdown generation with various inputs
  - File export

**Deliverable**: Can generate valid JSPWiki .md files from FinalArticle objects.

---

#### Step 5: Pipeline Orchestration

Build the complete pipeline that orchestrates agents through document phases.

**Pipeline Package (`pipeline` package)**
- [ ] Create `pipeline` package
- [ ] Implement `PipelineConfig` record with all settings
- [ ] Implement `PhaseExecutor`:
  - Execute single agent phase
  - Handle timeouts
  - Record contributions
- [ ] Implement `RejectionHandler`:
  - Route rejections to appropriate earlier phase
  - Track revision cycle count
  - Escalate after max cycles
- [ ] Implement `PublishingPipeline`:
  - Main `process(TopicBrief)` method
  - State machine enforcement
  - Phase sequencing: Research → Write → FactCheck → Edit → Output
  - Optional approval checkpoints
  - Error handling and recovery
- [ ] Add unit tests with mocked agents for:
  - Happy path through all phases
  - Rejection and revision cycles
  - Timeout handling
  - State transition enforcement

**Repository Package (`repository` package)**
- [ ] Create `repository` package
- [ ] Define `DocumentRepository` interface
- [ ] Implement `FileDocumentRepository`:
  - JSON serialization with Jackson
  - Save after each phase (enables recovery)
  - Load by document ID
  - List all documents
- [ ] Add unit tests for persistence operations

**Deliverable**: Pipeline can orchestrate agents through all phases. Documents
are persisted after each phase for recovery.

---

#### Step 6: CLI & Integration

Wire everything together and create the command-line interface.

**CLI Package (`cli` package)**
- [ ] Create `cli` package
- [ ] Implement `PublishingRunner` as CommandLineRunner:
  - Interactive topic input (title, audience, word count)
  - Output directory specification
  - Progress display during pipeline execution
  - Summary display on completion
  - Error reporting
- [ ] Remove or disable `HelloClaudeRunner`

**Spring Wiring**
- [ ] Verify all beans are properly configured
- [ ] Add component scanning for all packages
- [ ] Configure Jackson for JSON processing

**End-to-End Testing**
- [ ] Create integration test for full pipeline:
  - Start with TopicBrief
  - Run through all agents
  - Verify .md file output
  - Verify JSPWiki syntax compliance
- [ ] Test with sample topics:
  - Simple topic (< 500 words target)
  - Complex topic (> 1000 words target)
  - Topic with existing related pages
- [ ] Test error scenarios:
  - API timeout recovery
  - Fact check rejection → revision
  - Quality threshold not met

**Deliverable**: Complete working application. Can be run from command line to
produce wiki articles.

---

### Session 4: Polish (As Needed)

#### Step 7: Human Approval Workflow

Add optional human checkpoints between phases.

- [ ] Implement `ApprovalService`:
  - Pause pipeline at configured checkpoints
  - Display document state for review
  - Accept approve/reject/feedback input
  - Route feedback to appropriate agent
- [ ] Update `PublishingRunner` with approval prompts:
  - Show current document state
  - Options: approve, reject, provide feedback
- [ ] Add `AWAITING_APPROVAL` document state
- [ ] Add tests for approval flow

**Deliverable**: Humans can review and approve documents between phases.

---

#### Step 8: Monitoring & Documentation

Add observability and finalize documentation.

**Monitoring**
- [ ] Implement `PipelineListener` interface
- [ ] Implement `LoggingPipelineListener`:
  - Structured logging for each phase
  - Timing metrics
  - Token usage tracking
- [ ] Implement `MetricsCollector`:
  - Phase durations
  - Revision cycle counts
  - Quality scores
  - Token consumption
- [ ] Add summary report generation

**Documentation**
- [ ] Update README with:
  - Quick start guide
  - Configuration reference
  - Example usage
- [ ] Add sample topics and expected outputs
- [ ] Document extension points

**Deliverable**: Production-ready application with observability and documentation.

---

### Implementation Notes for Claude Code

**File Generation Strategy**

For each step, generate complete files rather than incremental edits:
- All records in a package can be generated in parallel
- Each agent is a self-contained file
- Tests are generated alongside implementations

**Template Replication**

After Step 2 establishes the agent pattern with ResearchAgent, Step 3 replicates
that pattern for the remaining agents. The structure is identical:
1. Extend BaseAgent
2. Reference prompt from AgentPrompts
3. Implement buildUserPrompt()
4. Implement parseResponse()
5. Implement validation
6. Write corresponding tests

**Testing Approach**

Each step includes its own tests. This ensures:
- Tests inform the implementation design
- Regressions are caught immediately
- Each step produces verified, working code

**Verification Points**

After each step, verify:
1. `mvn compile` passes
2. `mvn test` passes
3. New functionality works as expected

This maintains a working codebase throughout development.

---

## Appendix A: System Prompts

### A.1 Research Agent System Prompt

```
You are a meticulous research specialist preparing source material for wiki articles.

YOUR TASK:
Analyze the given topic and produce a comprehensive research brief that will enable
a writer to create an accurate, well-structured wiki article.

OUTPUT FORMAT:
Produce a JSON object with the following structure:
{
  "keyFacts": ["fact1", "fact2", ...],
  "sources": [{"description": "...", "reliability": "HIGH|MEDIUM|LOW"}],
  "suggestedOutline": ["Section 1", "Section 2", ...],
  "relatedPages": ["PageName1", "PageName2", ...],
  "glossary": {"term": "definition", ...},
  "uncertainAreas": ["area needing verification", ...]
}

GUIDELINES:
- Be thorough but focused on the specified scope
- Distinguish between well-established facts and areas of uncertainty
- Suggest page names in CamelCase format for internal wiki links
- Include enough detail for a writer unfamiliar with the topic
- Flag any claims that would benefit from additional verification
```

### A.2 Writer Agent System Prompt

```
You are a technical writer creating wiki articles in JSPWiki Markdown format.

YOUR TASK:
Transform the provided research brief into a clear, well-structured wiki article.

OUTPUT FORMAT:
Produce a JSON object with the following structure:
{
  "markdownContent": "## Title\n\nContent...",
  "summary": "One paragraph summary for metadata",
  "internalLinks": ["PageName1", "PageName2"],
  "categories": ["Category1", "Category2"]
}

JSPWIKI MARKDOWN RULES:
- Use ## for main title, ### for sections, #### for subsections
- Internal wiki links: [PageName]() or [display text](PageName)
- For articles over 500 words, include [{TableOfContents }]() after the intro
- First paragraph should work as a standalone summary
- End with "See Also" section linking to related pages

STYLE GUIDELINES:
- Write in encyclopedic, neutral tone
- Explain concepts before using them
- Use concrete examples where helpful
- Keep paragraphs focused and scannable
- Target the specified audience level
```

### A.3 Fact Checker Agent System Prompt

```
You are a rigorous fact-checker verifying wiki article content.

YOUR TASK:
Analyze the article draft against the research brief and identify any factual issues.

OUTPUT FORMAT:
Produce a JSON object with the following structure:
{
  "verifiedClaims": [
    {"claim": "...", "status": "VERIFIED", "sourceIndex": 0}
  ],
  "questionableClaims": [
    {"claim": "...", "issue": "...", "suggestion": "..."}
  ],
  "consistencyIssues": ["issue1", "issue2"],
  "overallConfidence": "HIGH|MEDIUM|LOW",
  "recommendedAction": "APPROVE|REVISE|REJECT"
}

VERIFICATION PROCESS:
1. Identify every factual claim in the article
2. Check each claim against the research brief sources
3. Flag claims not supported by provided sources
4. Check for internal consistency (contradictions)
5. Verify technical accuracy of any code or commands
6. Assess overall factual reliability

GUIDELINES:
- Be thorough but not pedantic
- Distinguish between factual errors and style preferences
- Provide specific suggestions for fixing issues
- Only recommend REJECT for serious factual problems
```

### A.4 Editor Agent System Prompt

```
You are a senior editor preparing wiki content for publication.

YOUR TASK:
Polish the article to publication quality while preserving factual accuracy.
You will also be provided with a list of existing wiki pages in the target
directory. Where appropriate, add internal links to these existing pages
to connect the new article with the existing wiki content.

OUTPUT FORMAT:
Produce a JSON object with the following structure:
{
  "markdownContent": "## Title\n\nPolished content...",
  "metadata": {
    "title": "Article Title",
    "summary": "Metadata summary",
    "author": "AI Publisher"
  },
  "editSummary": "Brief description of changes made",
  "qualityScore": 0.0-1.0,
  "addedLinks": ["PageName1", "PageName2"]
}

EDITING PRIORITIES:
1. Fix any issues flagged by fact-checker
2. Improve clarity and flow
3. Ensure consistent tone throughout
4. Fix grammar, spelling, punctuation
5. Verify JSPWiki Markdown syntax is correct
6. Ensure proper heading hierarchy
7. Verify all internal links use correct syntax
8. Review the EXISTING_PAGES list and add [PageName]() links where the
   article content naturally references topics covered by those pages

LINK INTEGRATION GUIDELINES:
- Only add links where they enhance understanding
- Use the exact page name from EXISTING_PAGES in the link syntax
- Prefer linking on first mention of a concept
- Do not over-link; one link per concept is sufficient
- Links should feel natural, not forced

CONSTRAINTS:
- Do NOT change factual content
- Do NOT add new information beyond links to existing pages
- Do NOT remove substantive content
- Preserve the author's voice where possible
- Remove any fact-checker annotations from output
```

---

## Appendix B: Example Output

### B.1 Sample JSPWiki Article Output

**Filename:** `ApacheKafka.md`

```markdown
## Apache Kafka

Apache Kafka is a distributed event streaming platform designed for high-throughput,
fault-tolerant handling of real-time data feeds. Originally developed at LinkedIn
and open-sourced in 2011, Kafka has become a foundational technology for building
real-time data pipelines and streaming applications.

[{TableOfContents }]()

## What is Event Streaming?

[EventStreaming]() is a practice of capturing data in real-time from event sources
like databases, sensors, and applications as streams of events. Unlike traditional
batch processing, event streaming enables continuous data flow and immediate
processing.

## Core Concepts

### Topics and Partitions

Kafka organizes messages into **topics**, which are split into **partitions** for
parallelism. Each partition is an ordered, immutable sequence of messages.

### Producers and Consumers

**Producers** publish messages to topics, while **consumers** read from them.
Kafka supports [ConsumerGroups]() for load-balanced consumption.

### Brokers and Clusters

A Kafka **broker** is a server that stores data and serves client requests.
Multiple brokers form a **cluster** for scalability and fault tolerance.

## Common Use Cases

* Real-time analytics and monitoring
* [EventDrivenArchitecture]() implementation
* Log aggregation across distributed systems
* Stream processing with Kafka Streams or [ApacheFlink]()

## Getting Started

To run Kafka locally:

```bash
# Download and extract Kafka
wget https://downloads.apache.org/kafka/3.6.0/kafka_2.13-3.6.0.tgz
tar -xzf kafka_2.13-3.6.0.tgz
cd kafka_2.13-3.6.0

# Start ZooKeeper (required for Kafka)
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka broker
bin/kafka-server-start.sh config/server.properties
```

## See Also

* [EventStreaming]()
* [MessageQueue]()
* [ApacheZooKeeper]()
* [ConsumerGroups]()

## References

1. Apache Kafka Documentation - kafka.apache.org
2. Kafka: The Definitive Guide (O'Reilly)
```

---

*Document Version: 1.1*
*Created: 2025-12-11*
*Updated: 2025-12-11*
*Author: AI Publisher Design Team*
