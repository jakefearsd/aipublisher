# AI Publisher: Advanced Capabilities Development Plan

This document outlines advanced capabilities for AI Publisher to produce higher-quality content, with specific focus on enhancing documentation for the JSPWiki project and general improvements to the multi-agent content generation approach.

---

## Executive Summary

AI Publisher currently generates wiki articles through a five-agent pipeline (Research → Writer → Fact Checker → Editor → Critic). This document proposes significant enhancements across three dimensions:

1. **Codebase Integration** - Deep integration with source code for accurate technical documentation
2. **Multi-Source Intelligence** - Enhanced research through code analysis, existing docs, and web sources
3. **Context-Aware Generation** - Content that fits naturally within an existing wiki ecosystem

---

## Part 1: JSPWiki-Specific Enhancements

### 1.1 Codebase Analysis Agent

**Purpose**: Automatically analyze Java source code to extract accurate technical information for documentation.

**Capabilities**:

| Feature | Description | Implementation Complexity |
|---------|-------------|--------------------------|
| Class Documentation | Extract Javadoc, method signatures, inheritance | Medium |
| Architecture Mapping | Identify module dependencies and relationships | High |
| Extension Point Discovery | Find interfaces, abstract classes, plugin patterns | Medium |
| API Surface Analysis | Document public APIs with usage examples | High |
| Test Coverage Insights | Identify well-tested vs. undocumented code | Low |

**Implementation Approach**:

```
CodebaseAnalysisAgent
├── JavaParser integration (or Eclipse JDT)
├── Package relationship mapper
├── Javadoc extractor and enhancer
├── Pattern recognizer (Factory, Provider, Command, etc.)
└── Usage example generator from test code
```

**JSPWiki-Specific Value**:
- Auto-generate documentation for all 586+ Java source files
- Document the Provider pattern implementations (PageProvider, AttachmentProvider, SearchProvider)
- Extract and explain the WikiEngine manager architecture
- Document event types and listener patterns from jspwiki-event module

### 1.2 Wiki Ecosystem Analyzer

**Purpose**: Understand existing wiki content to generate articles that integrate naturally.

**Capabilities**:

| Feature | Description | Benefit |
|---------|-------------|---------|
| Content Gap Analysis | Identify undocumented topics | Targeted generation |
| Link Graph Analysis | Map relationships between pages | Better interlinking |
| Style Analysis | Learn voice/tone from existing content | Consistent style |
| Category Taxonomy | Understand organizational structure | Proper categorization |
| Terminology Extraction | Build domain-specific glossary | Accurate language |

**Implementation Approach**:

```
WikiEcosystemAnalyzer
├── Page inventory scanner
├── Link relationship mapper (who links to whom)
├── Content style profiler (sentence length, vocabulary, formality)
├── Category and tag extractor
├── Missing page detector (broken links = content gaps)
└── Terminology frequency analyzer
```

**JSPWiki-Specific Value**:
- Analyze existing pages in `tomcat/jspwiki-pages/`
- Identify red links (referenced but missing pages)
- Learn JSPWiki's documentation conventions
- Suggest new articles based on code not yet documented

### 1.3 JSPWiki Syntax Perfection

**Purpose**: Ensure 100% correct JSPWiki syntax through validation and auto-correction.

**Enhanced Syntax Support**:

| Element | Current Support | Enhanced Support |
|---------|----------------|------------------|
| Basic Headings | ✅ `!`, `!!`, `!!!` | Add heading ID anchors |
| Links | ✅ `[PageName]` | Validate target exists |
| Code Blocks | ✅ `{{{ }}}` | Syntax highlighting hints |
| Plugins | ❌ Limited | Full `[{Plugin params}]` support |
| Variables | ❌ None | `[{$variable}]` support |
| Conditional | ❌ None | `[{IF}]` / `[{SET}]` support |
| Forms | ❌ None | Form plugin support |

**Implementation**:

```java
// New JSPWikiValidator class
public class JSPWikiValidator {
    // Validate all internal links resolve to real pages
    public List<String> validateLinks(String content, Set<String> existingPages);

    // Validate plugin syntax
    public List<String> validatePlugins(String content, Set<String> installedPlugins);

    // Auto-fix common Markdown→JSPWiki mistakes
    public String autoCorrect(String content);

    // Generate proper anchor IDs for headings
    public String addHeadingAnchors(String content);
}
```

---

## Part 2: Enhanced Research Capabilities

### 2.1 Multi-Source Research Pipeline

**Current State**: Web search via DuckDuckGo with basic reliability scoring.

**Enhanced Pipeline**:

```
ResearchPipeline
├── Primary Sources
│   ├── Codebase Analysis (Javadoc, source, tests)
│   ├── Existing Wiki Content (local pages)
│   └── Official Documentation (Apache docs, GitHub)
│
├── Secondary Sources
│   ├── Web Search (DuckDuckGo, specialized searches)
│   ├── Academic Sources (arXiv, ACM, IEEE)
│   └── Community Sources (Stack Overflow, GitHub Issues)
│
├── Verification Sources
│   ├── Official Specifications
│   ├── RFC Documents
│   └── Authoritative References
│
└── Source Triangulation
    ├── Cross-reference claims across sources
    ├── Weight by source reliability
    └── Flag conflicts for human review
```

### 2.2 Specialized Research Agents

**Proposal**: Replace single Research Agent with specialized sub-agents:

| Agent | Role | Sources | Output |
|-------|------|---------|--------|
| **Code Researcher** | Extract technical details from source | Source code, Javadoc, tests | API signatures, patterns, behaviors |
| **Domain Researcher** | Gather conceptual knowledge | Web, academic papers, specs | Concepts, definitions, context |
| **Precedent Researcher** | Find existing similar content | Wiki, docs, tutorials | Style guides, related pages |
| **Fact Researcher** | Verify specific claims | Official sources, specs | Verification status, citations |

**Coordination Pattern**:

```
TopicBrief
    │
    ├──► Code Researcher ──────────┐
    ├──► Domain Researcher ─────────┼──► Research Synthesizer ──► Writer
    ├──► Precedent Researcher ─────┤
    └──► Fact Researcher ──────────┘
```

### 2.3 Source Citation Enhancement

**Current**: Basic source tracking with reliability levels.

**Enhanced System**:

```java
public record EnhancedCitation(
    String sourceId,              // Unique identifier
    String sourceType,            // CODE, JAVADOC, WEB, ACADEMIC, OFFICIAL
    String sourceLocation,        // File path, URL, DOI
    String exactQuote,            // Direct quote if applicable
    String paraphrase,            // Paraphrased content
    LocalDateTime accessDate,     // When source was accessed
    double confidenceScore,       // 0.0 to 1.0
    List<String> corroboratingSources  // Other sources confirming this
) {}
```

**Citation Rendering**:

```
!!! Apache Kafka

Apache Kafka is a distributed event streaming platform.[^1][^2]

!! References

[^1]: [Apache Kafka Documentation|https://kafka.apache.org/documentation/] (Official, accessed 2025-12-13)
[^2]: Kreps, J. et al. "Kafka: a Distributed Messaging System" (LinkedIn Engineering, 2011)
```

---

## Part 3: Content Quality Improvements

### 3.1 Audience-Adaptive Writing

**Current**: Single audience parameter affects tone.

**Enhanced System**:

| Audience Level | Vocabulary | Explanations | Examples | Assumed Knowledge |
|---------------|------------|--------------|----------|-------------------|
| Beginner | Simple, defined | Everything explained | Many, basic | None |
| Intermediate | Technical allowed | Key concepts only | Relevant, practical | Basics |
| Expert | Full technical | Only novel concepts | Edge cases | Domain expertise |
| Mixed | Layered approach | Progressive disclosure | Both basic and advanced | Varies |

**Implementation**: Dynamic content sections that expand/collapse based on reader level:

```
!! Understanding Kafka Partitions

[{Expandable title='What is a Partition? (Beginners)'}]
A partition is like a folder that holds messages in order...
[{/Expandable}]

For production deployments, partition count should consider:
* Consumer parallelism requirements
* Expected message throughput
* Rebalancing overhead
```

### 3.2 Content Structure Templates

**Purpose**: Ensure consistent, high-quality structure for different article types.

**Template Library**:

| Template | Sections | Use Case |
|----------|----------|----------|
| **API Reference** | Overview, Signature, Parameters, Returns, Exceptions, Examples, See Also | Documenting classes/methods |
| **Tutorial** | Goal, Prerequisites, Steps, Verification, Troubleshooting | How-to guides |
| **Concept** | Definition, Context, Components, Relationships, Examples | Explaining ideas |
| **Architecture** | Overview, Components, Interactions, Diagrams, Trade-offs | System design docs |
| **Troubleshooting** | Symptom, Cause, Diagnosis, Solution, Prevention | Problem-solving guides |
| **Release Notes** | Summary, New Features, Changes, Deprecations, Migration | Version documentation |

**Template Selection Agent**:

```java
public class TemplateSelector {
    public ArticleTemplate selectTemplate(TopicBrief brief, ResearchBrief research) {
        // Analyze topic and research to choose appropriate template
        if (isAPIDocumentation(brief)) return templates.get("API_REFERENCE");
        if (isHowTo(brief)) return templates.get("TUTORIAL");
        if (isConceptual(brief)) return templates.get("CONCEPT");
        // ...
    }
}
```

### 3.3 Example Generation System

**Current**: Examples depend entirely on LLM generation.

**Enhanced System**:

```
ExampleGenerator
├── Code Example Sources
│   ├── Extract from unit tests
│   ├── Extract from integration tests
│   ├── Extract from documentation comments
│   └── Generate from API signatures
│
├── Example Validation
│   ├── Syntax check (compile if Java)
│   ├── Consistency check (imports, types match)
│   └── Runtime verification (if possible)
│
└── Example Enhancement
    ├── Add explanatory comments
    ├── Show common variations
    └── Highlight best practices
```

**JSPWiki-Specific Value**:
- Extract working examples from `jspwiki-main/src/test/java`
- Validate that example code compiles
- Ensure imports and class references are correct

### 3.4 Diagram Generation

**Purpose**: Automatically generate visual diagrams for architecture and workflows.

**Diagram Types**:

| Type | Tool | Use Case |
|------|------|----------|
| Class Diagrams | PlantUML / Mermaid | API documentation |
| Sequence Diagrams | PlantUML / Mermaid | Workflows, interactions |
| Architecture Diagrams | PlantUML / Mermaid | System overviews |
| State Machines | PlantUML / Mermaid | Document states, pipelines |
| ER Diagrams | PlantUML / Mermaid | Database schemas |

**Integration with JSPWiki**:

```
!! Document State Machine

[{Image src='DocumentStateMachine.png' alt='Document lifecycle states'}]

[{Diagram type='mermaid'}]
stateDiagram-v2
    [*] --> CREATED
    CREATED --> RESEARCHING
    RESEARCHING --> DRAFTING
    DRAFTING --> FACT_CHECKING
    FACT_CHECKING --> EDITING
    FACT_CHECKING --> DRAFTING : revision
    EDITING --> CRITIQUING
    CRITIQUING --> PUBLISHED
    CRITIQUING --> EDITING : revision
[{/Diagram}]
```

---

## Part 4: Agent Architecture Improvements

### 4.1 Agent Memory and Learning

**Current**: Each pipeline execution is independent.

**Enhanced System**:

```
AgentMemorySystem
├── Short-Term Memory
│   ├── Current document context
│   ├── Current conversation/revisions
│   └── Recent decisions and rationale
│
├── Medium-Term Memory (Session)
│   ├── Topics covered in this session
│   ├── Style decisions made
│   └── Quality issues encountered
│
└── Long-Term Memory (Persistent)
    ├── Wiki-specific style guide (learned)
    ├── Common error patterns to avoid
    ├── Successful content patterns
    └── User preference history
```

**Implementation Approach**:

```java
public interface AgentMemory {
    // Store successful patterns
    void recordSuccess(String documentId, ContentPattern pattern);

    // Store error corrections
    void recordCorrection(String documentId, CorrectionType type, String before, String after);

    // Retrieve relevant patterns for new documents
    List<ContentPattern> retrieveRelevantPatterns(TopicBrief brief);

    // Retrieve common errors to check
    List<ErrorPattern> getCommonErrors();
}
```

### 4.2 Parallel Agent Execution

**Current**: Sequential pipeline (Research → Writer → Fact Checker → Editor → Critic).

**Enhanced Pipeline with Parallelism**:

```
TopicBrief
    │
    ├──► Code Researcher ─────┐
    ├──► Domain Researcher ────┼──► Research Synthesis ──► Writer
    ├──► Precedent Researcher ─┤                              │
    └──► Fact Researcher ──────┘                              │
                                                              │
                                   ┌──────────────────────────┤
                                   │                          │
                              Fact Checker            Style Checker
                                   │                          │
                                   └───────────┬──────────────┘
                                               │
                                           Editor
                                               │
                                           Critic
                                               │
                                          Publisher
```

**Benefits**:
- Faster execution through parallelism
- Specialized checking (facts vs. style)
- Better separation of concerns

### 4.3 Agent Specialization by Domain

**Proposal**: Domain-specific agent configurations.

| Domain | Research Focus | Writing Style | Technical Depth |
|--------|---------------|---------------|-----------------|
| **API Documentation** | Source code, tests | Formal, precise | Very high |
| **Tutorials** | Examples, common issues | Friendly, step-by-step | Medium |
| **Architecture** | Design docs, code structure | Technical, analytical | High |
| **User Guides** | UI, workflows | Accessible, practical | Low-Medium |
| **Release Notes** | Git history, changelogs | Concise, factual | Medium |

**Implementation**:

```java
public class DomainSpecificAgentFactory {
    public AgentTeam createForDomain(DocumentDomain domain) {
        return switch (domain) {
            case API_DOCUMENTATION -> new AgentTeam(
                new CodeFocusedResearcher(),
                new TechnicalWriter(FORMAL_STYLE),
                new APIFactChecker(),
                new TechnicalEditor()
            );
            case TUTORIAL -> new AgentTeam(
                new ExampleFocusedResearcher(),
                new TutorialWriter(FRIENDLY_STYLE),
                new PracticalFactChecker(),
                new ReadabilityEditor()
            );
            // ...
        };
    }
}
```

### 4.4 Iterative Refinement with Feedback

**Current**: Fixed revision cycles with simple approve/revise/reject.

**Enhanced Feedback System**:

```
Feedback Loop Architecture
│
├── Automated Feedback
│   ├── Quality score trends (improving/declining)
│   ├── Syntax error patterns
│   ├── Readability metrics (Flesch-Kincaid, etc.)
│   └── Coverage metrics (did we address all research points?)
│
├── Structured Agent Feedback
│   ├── Specific issues with locations
│   ├── Suggested fixes (not just "revise")
│   ├── Priority ranking of issues
│   └── Rationale for each issue
│
└── Human Feedback Integration
    ├── Inline comments on specific sections
    ├── General direction guidance
    ├── Style preferences
    └── Missing content requests
```

**Implementation**:

```java
public record DetailedFeedback(
    List<InlineIssue> inlineIssues,      // Specific location-based issues
    List<StructuralIssue> structuralIssues,  // Section-level problems
    List<String> missingContent,          // What should be added
    List<String> unnecessaryContent,      // What should be removed
    Map<String, String> suggestedRewrites, // Specific rewrites to consider
    double overallScore,
    RecommendedAction action,
    String actionRationale
) {}
```

---

## Part 5: JSPWiki Integration Features

### 5.1 Direct Wiki Page Management

**Purpose**: Generate and publish directly to JSPWiki without manual file copying.

**Capabilities**:

| Feature | Description |
|---------|-------------|
| **Page Creation** | Create new wiki pages via JSPWiki's API |
| **Page Updates** | Update existing pages with new content |
| **Version Control** | Proper versioning with edit summaries |
| **Link Validation** | Verify all internal links before publishing |
| **Category Management** | Ensure proper categorization |

**Implementation Options**:

1. **File-Based** (Current): Write to `jspwiki-pages/` directory
2. **REST API**: Use JSPWiki's REST API (if available)
3. **Direct Provider**: Write to the PageProvider's backing store

### 5.2 Batch Documentation Generation

**Purpose**: Generate comprehensive documentation for entire codebases.

**Workflow**:

```
BatchDocumentationGenerator
│
├── Phase 1: Analysis
│   ├── Scan all Java packages
│   ├── Identify public APIs
│   ├── Map class hierarchies
│   └── Extract existing documentation
│
├── Phase 2: Planning
│   ├── Determine page structure
│   ├── Create topic briefs for each page
│   ├── Establish interlinking strategy
│   └── Estimate token/cost requirements
│
├── Phase 3: Generation
│   ├── Generate pages in dependency order
│   ├── Cross-reference as pages complete
│   ├── Validate internal consistency
│   └── Track progress and costs
│
└── Phase 4: Review
    ├── Generate summary report
    ├── Highlight uncertain content
    ├── Suggest human review priorities
    └── Create maintenance schedule
```

**JSPWiki Documentation Targets**:

| Module | Files | Estimated Pages | Priority |
|--------|-------|-----------------|----------|
| jspwiki-api | 45 | 15-20 | High |
| jspwiki-main | 350 | 80-100 | High |
| jspwiki-event | 25 | 8-10 | Medium |
| jspwiki-cache | 15 | 5-8 | Medium |
| jspwiki-markdown | 20 | 8-10 | Medium |
| jspwiki-http | 30 | 10-12 | Low |

### 5.3 Documentation Maintenance

**Purpose**: Keep documentation in sync with code changes.

**Features**:

| Feature | Trigger | Action |
|---------|---------|--------|
| **Change Detection** | Git commit | Identify affected docs |
| **Staleness Scoring** | Scheduled | Rate doc freshness |
| **Update Suggestions** | On demand | Propose doc updates |
| **Deprecation Handling** | API changes | Update affected pages |

**Implementation**:

```java
public class DocumentationMaintainer {

    // Detect which documentation needs updates based on code changes
    public List<MaintenanceTask> analyzeChanges(List<GitCommit> commits) {
        List<MaintenanceTask> tasks = new ArrayList<>();

        for (GitCommit commit : commits) {
            for (FileChange change : commit.getChanges()) {
                if (isJavaFile(change)) {
                    List<WikiPage> affectedPages = findDocumentingPages(change.getPath());
                    for (WikiPage page : affectedPages) {
                        tasks.add(new MaintenanceTask(
                            page,
                            determineUpdateType(change),
                            extractChangeContext(change)
                        ));
                    }
                }
            }
        }

        return prioritize(tasks);
    }
}
```

---

## Part 6: Quality Assurance Enhancements

### 6.1 Automated Quality Metrics

**Current Metrics**:
- Editor quality score (0.0-1.0)
- Critic structure/syntax/readability scores

**Enhanced Metrics**:

| Metric | Description | Target |
|--------|-------------|--------|
| **Factual Accuracy** | % of claims verified against sources | > 95% |
| **Coverage** | % of research points addressed | > 90% |
| **Readability** | Flesch-Kincaid grade level | Audience-appropriate |
| **Link Validity** | % of internal links that resolve | 100% |
| **Code Correctness** | % of code examples that compile | 100% |
| **Style Consistency** | Deviation from wiki style guide | < 5% |
| **Completeness** | All required sections present | 100% |

### 6.2 Regression Testing for Content

**Purpose**: Ensure content quality doesn't degrade over model updates or prompt changes.

**Implementation**:

```java
public class ContentRegressionTest {

    // Golden dataset of expected outputs
    private final List<GoldenSample> goldenSamples;

    public RegressionReport runRegression() {
        List<RegressionResult> results = new ArrayList<>();

        for (GoldenSample sample : goldenSamples) {
            PipelineResult actual = pipeline.execute(sample.topicBrief());

            results.add(new RegressionResult(
                sample.id(),
                compareStructure(sample.expectedStructure(), actual),
                compareFactualContent(sample.expectedFacts(), actual),
                compareSyntax(sample.expectedSyntax(), actual),
                compareQuality(sample.expectedQuality(), actual)
            ));
        }

        return new RegressionReport(results);
    }
}
```

### 6.3 Human Review Integration

**Purpose**: Efficiently integrate human review into the pipeline.

**Review Workflow**:

```
Pipeline Output
      │
      ▼
┌─────────────────────────────────────┐
│     Automated Triage                │
│  ┌───────────────────────────────┐  │
│  │ High Confidence (>0.9)        │──┼──► Auto-publish
│  │ Medium Confidence (0.7-0.9)   │──┼──► Quick review queue
│  │ Low Confidence (<0.7)         │──┼──► Full review queue
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
      │
      ▼
Human Review Interface
├── Side-by-side source/output view
├── Inline commenting
├── Section-by-section approval
├── Quick fixes for common issues
└── Feedback capture for learning
```

---

## Part 7: Implementation Roadmap

### Phase 1: Foundation (Weeks 1-4)

| Task | Description | Deliverable |
|------|-------------|-------------|
| 1.1 | JSPWiki syntax validator | `JSPWikiValidator` class |
| 1.2 | Wiki page scanner | `WikiEcosystemAnalyzer` service |
| 1.3 | Enhanced source citations | Updated `ResearchBrief` model |
| 1.4 | Content templates | Template library + selector |

### Phase 2: Code Integration (Weeks 5-8)

| Task | Description | Deliverable |
|------|-------------|-------------|
| 2.1 | Java source code parser | `CodeAnalyzer` service |
| 2.2 | Javadoc extractor | Integration with Code Researcher |
| 2.3 | Test example extractor | Example generation from tests |
| 2.4 | API documentation template | Specialized API doc pipeline |

### Phase 3: Advanced Research (Weeks 9-12)

| Task | Description | Deliverable |
|------|-------------|-------------|
| 3.1 | Multi-source research pipeline | Parallel researcher agents |
| 3.2 | Source triangulation | Cross-reference verification |
| 3.3 | Academic source integration | arXiv/ACM search support |
| 3.4 | Codebase-aware fact checking | Code-verified fact checker |

### Phase 4: Quality & Scale (Weeks 13-16)

| Task | Description | Deliverable |
|------|-------------|-------------|
| 4.1 | Batch documentation mode | Bulk generation pipeline |
| 4.2 | Regression test framework | Quality assurance tests |
| 4.3 | Human review interface | Review workflow system |
| 4.4 | Documentation maintenance | Change detection system |

---

## Part 8: Success Metrics

### Quantitative Goals

| Metric | Current | Target | Timeline |
|--------|---------|--------|----------|
| Syntax accuracy | ~90% | 99%+ | Phase 1 |
| Factual accuracy | ~85% | 95%+ | Phase 3 |
| Code example validity | Unknown | 95%+ | Phase 2 |
| Human review time | N/A | <5 min/article | Phase 4 |
| Generation throughput | 1 article/min | 5 articles/min | Phase 4 |

### Qualitative Goals

- Generated documentation indistinguishable from human-written
- Seamless integration with existing JSPWiki content
- Self-maintaining documentation that stays current
- Developer trust in AI-generated technical content

---

## Appendix A: Technical Specifications

### A.1 Code Analysis Integration

```java
// Proposed interface for code analysis
public interface CodebaseAnalyzer {

    // Analyze a single Java file
    JavaFileAnalysis analyzeFile(Path javaFile);

    // Analyze an entire package
    PackageAnalysis analyzePackage(String packageName);

    // Extract documentation for a class
    ClassDocumentation extractDocumentation(String fullyQualifiedClassName);

    // Find usage examples in tests
    List<CodeExample> findExamples(String className, String methodName);

    // Map class relationships
    ClassRelationshipGraph buildRelationshipGraph(String rootPackage);
}
```

### A.2 Wiki Integration API

```java
// Proposed interface for wiki integration
public interface WikiIntegration {

    // Check if a page exists
    boolean pageExists(String pageName);

    // Get list of all pages
    Set<String> getAllPages();

    // Get pages linking to a specific page
    Set<String> getIncomingLinks(String pageName);

    // Get pages linked from a specific page
    Set<String> getOutgoingLinks(String pageName);

    // Analyze page content style
    StyleProfile analyzeStyle(String pageName);

    // Publish a page
    PublishResult publish(String pageName, String content, String editSummary);
}
```

### A.3 Quality Metrics API

```java
// Proposed quality measurement system
public interface QualityAnalyzer {

    // Compute comprehensive quality metrics
    QualityReport analyze(String content, TopicBrief brief, ResearchBrief research);

    // Check factual accuracy against sources
    FactualAccuracyReport checkFacts(String content, List<Citation> sources);

    // Validate code examples
    CodeValidationReport validateCode(String content, String language);

    // Check readability metrics
    ReadabilityReport checkReadability(String content, AudienceLevel audience);

    // Validate against style guide
    StyleReport checkStyle(String content, StyleGuide guide);
}
```

---

## Appendix B: JSPWiki Documentation Inventory

### Existing Pages (tomcat/jspwiki-pages/)

| Category | Count | Example Pages |
|----------|-------|---------------|
| Configuration | 5 | LoggingConfig, PostgreSQLLocalDeployment |
| Deployment | 3 | DockerDeployment, JspwikiDeployment |
| Development | 2 | DevelopingWithPostgresql, MvnCheatSheet |
| Features | 10+ | Various feature documentation |
| Help | 5+ | EditPageHelp, LoginHelp |

### Documentation Gaps (Suggested New Pages)

| Topic | Priority | Complexity | Notes |
|-------|----------|------------|-------|
| WikiEngine Architecture | High | High | Core system documentation |
| Provider Pattern Guide | High | Medium | Extension point docs |
| Event System Overview | Medium | Medium | jspwiki-event module |
| Plugin Development | High | Medium | How to write plugins |
| Filter Development | Medium | Medium | Content filtering |
| Security Model | High | High | Auth, ACLs, permissions |
| Caching System | Low | Low | jspwiki-cache module |
| Markdown Support | Medium | Low | jspwiki-markdown module |
| REST API Reference | Medium | Medium | API documentation |
| Workflow System | Medium | High | Approval workflows |

---

## Appendix C: Resource Requirements

### Computational Resources

| Phase | API Calls/Article | Est. Tokens/Article | Est. Cost/Article |
|-------|-------------------|---------------------|-------------------|
| Current | 5-8 | ~25,000 | ~$0.50 |
| Phase 1 | 6-10 | ~30,000 | ~$0.60 |
| Phase 2 | 8-12 | ~40,000 | ~$0.80 |
| Phase 3 | 10-15 | ~50,000 | ~$1.00 |
| Phase 4 | 10-15 | ~50,000 | ~$1.00 |

### Development Resources

| Phase | Effort (Person-Weeks) | Skills Required |
|-------|----------------------|-----------------|
| Phase 1 | 4 | Java, JSPWiki syntax |
| Phase 2 | 6 | Java parsing, AST analysis |
| Phase 3 | 6 | ML/AI, information retrieval |
| Phase 4 | 4 | DevOps, testing frameworks |

---

*Document Version: 1.0*
*Last Updated: 2025-12-13*
*Authors: AI Publisher Development Team*
