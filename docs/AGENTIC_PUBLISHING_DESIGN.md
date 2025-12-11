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
│                         [Final .txt File]                              │
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

**Purpose**: Polish prose, ensure style consistency, and prepare final output.

**Inputs:**
- Fact-checked article with annotations
- Style guide
- JSPWiki format requirements

**Outputs:**
- Final polished article
- Edit summary (changes made)
- Quality assessment score
- Final metadata

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
```

**Behavioral Characteristics:**
- Conservative editor: improves without rewriting
- Format perfectionist: ensures valid output
- Quality gatekeeper: can reject if below standards

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

### 6.1 Target Syntax

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
- `markup.syntax=markdown`
- `summary=<extracted summary>`
- `author=AI Publisher`
- `changenote=<edit summary>`

### 6.2 Output Generator

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
        // Convert to CamelCase, ensure .txt extension
        return toCamelCase(pageName) + ".txt";
    }
}
```

### 6.3 Link Processing

Internal links are processed to ensure validity:

```java
public class LinkProcessor {
    private final Set<String> existingPages;

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
│ OUTPUT: ApacheKafka.txt                                                     │
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
output.file-extension=.txt

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

### Phase 1: Foundation (Steps 1-5)

**Step 1: Create Document Model**
- [ ] Create `document` package
- [ ] Implement `DocumentState` enum
- [ ] Implement `TopicBrief` record
- [ ] Implement `ResearchBrief` record
- [ ] Implement `ArticleDraft` record
- [ ] Implement `FactCheckReport` record
- [ ] Implement `FinalArticle` record
- [ ] Implement `DocumentMetadata` record
- [ ] Implement `PublishingDocument` main entity
- [ ] Implement `AgentContribution` record
- [ ] Add unit tests for document state transitions

**Step 2: Create Agent Framework**
- [ ] Create `agent` package
- [ ] Define `Agent` interface
- [ ] Define `AgentRole` enum
- [ ] Implement `BaseAgent` abstract class with common functionality
- [ ] Create `AgentPrompts` class with system prompt constants
- [ ] Add unit tests for base agent

**Step 3: Implement Research Agent**
- [ ] Implement `ResearchAgent` extending `BaseAgent`
- [ ] Define research system prompt
- [ ] Implement input preparation (TopicBrief → prompt)
- [ ] Implement output parsing (response → ResearchBrief)
- [ ] Implement validation logic
- [ ] Add integration test with Claude

**Step 4: Implement Writer Agent**
- [ ] Implement `WriterAgent` extending `BaseAgent`
- [ ] Define writer system prompt with JSPWiki format instructions
- [ ] Implement input preparation (ResearchBrief → prompt)
- [ ] Implement output parsing (response → ArticleDraft)
- [ ] Implement JSPWiki link syntax generation
- [ ] Implement validation logic
- [ ] Add integration test with Claude

**Step 5: Implement Fact Checker Agent**
- [ ] Implement `FactCheckerAgent` extending `BaseAgent`
- [ ] Define fact checker system prompt
- [ ] Implement input preparation (ArticleDraft + ResearchBrief → prompt)
- [ ] Implement output parsing (response → FactCheckReport)
- [ ] Implement claim extraction and verification tracking
- [ ] Implement validation logic (confidence thresholds)
- [ ] Add integration test with Claude

### Phase 2: Pipeline & Output (Steps 6-9)

**Step 6: Implement Editor Agent**
- [ ] Implement `EditorAgent` extending `BaseAgent`
- [ ] Define editor system prompt
- [ ] Implement input preparation (ArticleDraft + FactCheckReport → prompt)
- [ ] Implement output parsing (response → FinalArticle)
- [ ] Implement quality scoring
- [ ] Implement validation logic
- [ ] Add integration test with Claude

**Step 7: Create Pipeline Orchestrator**
- [ ] Create `pipeline` package
- [ ] Implement `PipelineConfig` record
- [ ] Implement `PhaseExecutor` for individual phase execution
- [ ] Implement `RejectionHandler` for quality gate failures
- [ ] Implement `PublishingPipeline` main orchestrator
- [ ] Add state transition logic
- [ ] Add retry logic with exponential backoff
- [ ] Add unit tests for pipeline flow

**Step 8: Implement JSPWiki Output Generator**
- [ ] Create `output` package
- [ ] Implement `LinkProcessor` for internal link handling
- [ ] Implement `JSPWikiMarkdownGenerator`
- [ ] Implement `FileExporter` for writing .txt files
- [ ] Add validation for JSPWiki syntax compliance
- [ ] Add unit tests for output generation

**Step 9: Implement Document Repository**
- [ ] Create `repository` package
- [ ] Define `DocumentRepository` interface
- [ ] Implement `FileDocumentRepository` (JSON file storage)
- [ ] Add document versioning support
- [ ] Add search/query capabilities
- [ ] Add unit tests for persistence

### Phase 3: Integration & CLI (Steps 10-12)

**Step 10: Create Configuration**
- [ ] Create `config` package
- [ ] Implement `ClaudeConfig` for LangChain4j beans
- [ ] Implement `PipelineProperties` for externalized config
- [ ] Update `application.properties` with all settings
- [ ] Add configuration validation

**Step 11: Create CLI Interface**
- [ ] Create `cli` package
- [ ] Implement `PublishingRunner` as CommandLineRunner
- [ ] Add interactive topic input
- [ ] Add progress display during pipeline execution
- [ ] Add output summary display
- [ ] Remove or disable HelloClaudeRunner

**Step 12: End-to-End Testing**
- [ ] Create integration test for full pipeline
- [ ] Test with sample topics of varying complexity
- [ ] Verify JSPWiki output compatibility
- [ ] Test error handling and recovery
- [ ] Test rejection and revision cycles
- [ ] Performance testing (timing, token usage)

### Phase 4: Polish & Documentation (Steps 13-15)

**Step 13: Add Human Approval Workflow**
- [ ] Implement approval checkpoint logic
- [ ] Add CLI prompts for approval/rejection
- [ ] Implement feedback routing to agents
- [ ] Add approval status to document model

**Step 14: Add Monitoring & Logging**
- [ ] Add structured logging throughout
- [ ] Implement `PipelineListener` for events
- [ ] Add metrics collection (timing, tokens, quality scores)
- [ ] Create summary report generation

**Step 15: Documentation & Cleanup**
- [ ] Update README with usage instructions
- [ ] Document configuration options
- [ ] Add example topics and outputs
- [ ] Code cleanup and refactoring
- [ ] Final testing pass

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
  "qualityScore": 0.0-1.0
}

EDITING PRIORITIES:
1. Fix any issues flagged by fact-checker
2. Improve clarity and flow
3. Ensure consistent tone throughout
4. Fix grammar, spelling, punctuation
5. Verify JSPWiki Markdown syntax is correct
6. Ensure proper heading hierarchy
7. Verify all internal links use correct syntax

CONSTRAINTS:
- Do NOT change factual content
- Do NOT add new information
- Do NOT remove substantive content
- Preserve the author's voice where possible
- Remove any fact-checker annotations from output
```

---

## Appendix B: Example Output

### B.1 Sample JSPWiki Article Output

**Filename:** `ApacheKafka.txt`

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

*Document Version: 1.0*
*Created: 2025-12-11*
*Author: AI Publisher Design Team*
