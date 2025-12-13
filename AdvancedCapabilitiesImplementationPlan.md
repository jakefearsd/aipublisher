# Advanced Capabilities Implementation Plan

This document provides a sequenced, incremental implementation plan for the advanced capabilities described in `AdvancedCapabilities.md`. The plan is optimized for development with Claude Code, with each milestone delivering usable functionality while building toward the complete vision.

---

## Implementation Philosophy

### Guiding Principles

1. **Incremental Value** - Each milestone delivers working functionality that improves content generation
2. **Build on Foundations** - Later features depend on earlier ones; sequence matters
3. **Test as You Go** - Each feature includes tests; integration tests validate end-to-end
4. **Two-Project Coordination** - Changes alternate between AI Publisher and JSPWiki as needed
5. **Claude Code Optimization** - Tasks sized for single-session completion with clear boundaries

### Project Repositories

| Project | Path | Purpose |
|---------|------|---------|
| **AI Publisher** | `/home/jakefear/source/aipublisher` | Content generation engine |
| **JSPWiki** | `/home/jakefear/source/jspwiki` | Wiki platform and plugins |

### Milestone Sizing

Each milestone is designed to be:
- Completable in 1-3 Claude Code sessions
- Independently testable
- Demonstrably valuable
- A foundation for subsequent work

---

## Phase 1: Enhanced Single-Article Generation

**Goal**: Improve the quality of individual articles before tackling multi-article orchestration.

**Duration**: Milestones 1.1 - 1.6 (6 milestones)

---

### Milestone 1.1: Content Type Framework

**Project**: AI Publisher
**Estimated Sessions**: 2
**Prerequisites**: None

**Objective**: Introduce formal content types that shape article structure.

**Deliverables**:

1. **Content Type Enum and Models**
   ```
   src/main/java/com/jakefear/aipublisher/content/
   ├── ContentType.java              # CONCEPT, TUTORIAL, REFERENCE, GUIDE, COMPARISON, TROUBLESHOOTING, OVERVIEW
   ├── ContentTypeTemplate.java      # Template definition for each type
   └── ContentTypeSelector.java      # Logic to choose type from topic
   ```

2. **Template Definitions**
   - Structure templates for each content type
   - Required sections, optional sections, ordering rules
   - Target word counts and example requirements

3. **Integration with TopicBrief**
   - Add `contentType` field to TopicBrief
   - Auto-detect content type if not specified
   - Pass content type to Writer agent

4. **Updated Writer Prompts**
   - Content-type-specific writing instructions
   - Structure enforcement in prompt

**Tests**:
- Unit tests for ContentTypeSelector
- Integration test generating each content type
- Validation that output matches template structure

**Success Criteria**:
- Can specify `--type tutorial` on CLI
- Generated tutorials have Goal → Steps → Verification structure
- Generated concepts have Definition → Context → Details → Examples structure

---

### Milestone 1.2: Glossary and Terminology Management

**Project**: AI Publisher
**Estimated Sessions**: 2
**Prerequisites**: None (can parallel with 1.1)

**Objective**: Establish consistent terminology across generated content.

**Deliverables**:

1. **Glossary Models**
   ```
   src/main/java/com/jakefear/aipublisher/glossary/
   ├── Glossary.java                 # Domain glossary container
   ├── TermDefinition.java           # Canonical term with definition
   ├── GlossaryBuilder.java          # Build glossary from various sources
   └── TerminologyChecker.java       # Validate content against glossary
   ```

2. **Glossary Sources**
   - Load from JSON/YAML file
   - Extract from existing wiki pages (scan for definitions)
   - Build from research phase output

3. **Terminology Validation**
   - Check generated content for non-canonical terms
   - Suggest replacements for synonyms
   - Flag undefined technical terms

4. **CLI Integration**
   - `--glossary path/to/glossary.json` option
   - `--build-glossary` to extract from wiki directory

**Tests**:
- Glossary loading and merging
- Synonym detection
- Integration with editor agent for terminology fixes

**Success Criteria**:
- Glossary loaded at pipeline start
- Writer uses canonical terms
- Editor flags/fixes terminology inconsistencies

---

### Milestone 1.3: Enhanced Linking Intelligence

**Project**: AI Publisher
**Estimated Sessions**: 2
**Prerequisites**: 1.2 (uses glossary for term recognition)

**Objective**: Generate strategic internal links, not just keyword matches.

**Deliverables**:

1. **Link Analysis Models**
   ```
   src/main/java/com/jakefear/aipublisher/linking/
   ├── LinkCandidate.java            # Potential link with context
   ├── LinkEvaluator.java            # Score link value
   ├── LinkingStrategy.java          # Rules for when/how to link
   └── WikiLinkContext.java          # Existing pages and their topics
   ```

2. **Link Evaluation Criteria**
   - First mention preference
   - Contextual relevance scoring
   - Link density targets (3-8%)
   - Bidirectional value assessment

3. **Wiki Context Loading**
   - Scan existing wiki pages
   - Build page → topic mapping
   - Track which pages link to which

4. **Integration with Editor Agent**
   - Provide link suggestions to editor
   - Editor adds links during polish phase
   - Track links added in FinalArticle

**Tests**:
- Link candidate identification
- Link density calculation
- Integration test with sample wiki directory

**Success Criteria**:
- Links added only on first substantive mention
- Link density within target range
- No links to non-existent pages

---

### Milestone 1.4: Example Generation Framework

**Project**: AI Publisher
**Estimated Sessions**: 2
**Prerequisites**: 1.1 (content types define example needs)

**Objective**: Systematically plan and generate examples for each article.

**Deliverables**:

1. **Example Planning Models**
   ```
   src/main/java/com/jakefear/aipublisher/examples/
   ├── ExampleSpec.java              # Specification for an example
   ├── ExampleType.java              # MINIMAL, REALISTIC, PROGRESSIVE, ANTI_PATTERN
   ├── ExamplePlan.java              # Plan for examples in an article
   ├── ExamplePlanner.java           # Generate example plan from content type
   └── ExampleValidator.java         # Validate example correctness
   ```

2. **Example Planning Logic**
   - Content type → example requirements mapping
   - Complexity progression for tutorials
   - Anti-pattern inclusion for best practices content

3. **Example Validation**
   - Syntax checking for code examples
   - Consistency with article claims
   - Completeness (imports, context)

4. **Writer Agent Enhancement**
   - Include example plan in writer prompt
   - Structured example output in ArticleDraft

**Tests**:
- Example plan generation for each content type
- Example validation logic
- Integration test checking example presence

**Success Criteria**:
- Tutorials have progressive examples
- Reference docs have minimal examples
- Code examples are syntactically valid

---

### Milestone 1.5: Prerequisite Tracking

**Project**: AI Publisher
**Estimated Sessions**: 1
**Prerequisites**: 1.3 (uses wiki context)

**Objective**: Track and communicate prerequisites for each article.

**Deliverables**:

1. **Prerequisite Models**
   ```
   src/main/java/com/jakefear/aipublisher/prerequisites/
   ├── PrerequisiteType.java         # HARD, SOFT, ASSUMED
   ├── PrerequisiteSet.java          # Prerequisites for a topic
   └── PrerequisiteAnalyzer.java     # Determine prerequisites from content
   ```

2. **Prerequisite Detection**
   - Analyze topic for likely prerequisites
   - Check wiki for existing prerequisite pages
   - Categorize as hard (required) vs soft (helpful)

3. **Content Integration**
   - Add prerequisite section to article templates
   - Generate prerequisite callout in JSPWiki syntax
   - Link to prerequisite pages

**Tests**:
- Prerequisite detection for sample topics
- Prerequisite rendering in output

**Success Criteria**:
- Articles include prerequisite callout when appropriate
- Prerequisites link to existing wiki pages
- Complex topics list hard prerequisites

---

### Milestone 1.6: Structured "See Also" Generation

**Project**: AI Publisher
**Estimated Sessions**: 1
**Prerequisites**: 1.3 (uses wiki context), 1.5 (understands relationships)

**Objective**: Generate intelligent "See Also" sections with categorized recommendations.

**Deliverables**:

1. **See Also Models**
   ```
   src/main/java/com/jakefear/aipublisher/seealso/
   ├── SeeAlsoCategory.java          # GO_DEEPER, GO_BROADER, GO_PRACTICAL, COMPARE, TROUBLESHOOT
   ├── SeeAlsoRecommendation.java    # Recommendation with rationale
   └── SeeAlsoGenerator.java         # Generate categorized recommendations
   ```

2. **Recommendation Logic**
   - Go Deeper: subtopics, advanced aspects
   - Go Broader: parent topics, context
   - Go Practical: tutorials, how-tos
   - Compare: alternatives, trade-offs
   - Troubleshoot: common issues

3. **Integration with Editor**
   - Generate See Also section during editing
   - Include brief "why read this" annotations

**Tests**:
- See Also generation for various topics
- Category appropriateness

**Success Criteria**:
- See Also sections are categorized
- Recommendations are contextually relevant
- Links go to existing pages

---

## Phase 2: Wiki Ecosystem Awareness

**Goal**: Enable AI Publisher to understand and integrate with existing wiki content.

**Duration**: Milestones 2.1 - 2.5 (5 milestones)

---

### Milestone 2.1: Wiki Scanner Service

**Project**: AI Publisher
**Estimated Sessions**: 2
**Prerequisites**: Phase 1 complete

**Objective**: Scan and index existing wiki content for context-aware generation.

**Deliverables**:

1. **Wiki Scanner Models**
   ```
   src/main/java/com/jakefear/aipublisher/wiki/
   ├── WikiScanner.java              # Scan wiki directory
   ├── WikiPage.java                 # Parsed wiki page
   ├── WikiIndex.java                # Index of all pages
   ├── WikiLink.java                 # Link between pages
   └── WikiCategory.java             # Category information
   ```

2. **Scanning Capabilities**
   - Parse JSPWiki page files
   - Extract titles, headings, categories
   - Map internal links (outgoing and incoming)
   - Identify page topics

3. **Index Building**
   - Build searchable index of pages
   - Create link graph
   - Calculate page metrics (word count, link count, etc.)

4. **CLI Integration**
   - `--wiki-dir path/to/wiki` to specify wiki location
   - Auto-detect wiki directory from output path

**Tests**:
- Scanning sample wiki directory
- Link extraction accuracy
- Index query performance

**Success Criteria**:
- Can scan JSPWiki pages directory
- Accurate link graph construction
- Sub-second index queries

---

### Milestone 2.2: Content Gap Analysis

**Project**: AI Publisher
**Estimated Sessions**: 2
**Prerequisites**: 2.1

**Objective**: Identify missing content in existing wiki.

**Deliverables**:

1. **Gap Analysis Models**
   ```
   src/main/java/com/jakefear/aipublisher/gaps/
   ├── ContentGap.java               # Identified gap with priority
   ├── GapType.java                  # MISSING_PAGE, THIN_CONTENT, ORPHAN, MISSING_LINK
   ├── GapAnalyzer.java              # Analyze wiki for gaps
   └── GapReport.java                # Report of all gaps
   ```

2. **Gap Detection**
   - Red links (referenced but missing pages)
   - Thin pages (below word count threshold)
   - Orphan pages (no incoming links)
   - Missing reciprocal links
   - Uncategorized pages

3. **Gap Prioritization**
   - Score by reference count (more references = higher priority)
   - Score by topic centrality
   - Score by reader impact

4. **CLI Commands**
   - `--analyze-gaps` to generate gap report
   - `--fill-gap PageName` to generate missing page

**Tests**:
- Gap detection on sample wiki
- Prioritization logic
- Report generation

**Success Criteria**:
- Identifies all red links
- Prioritizes gaps sensibly
- Generates actionable report

---

### Milestone 2.3: Style Learning from Existing Content

**Project**: AI Publisher
**Estimated Sessions**: 2
**Prerequisites**: 2.1

**Objective**: Learn writing style from existing wiki content for consistency.

**Deliverables**:

1. **Style Analysis Models**
   ```
   src/main/java/com/jakefear/aipublisher/style/
   ├── StyleProfile.java             # Captured style characteristics
   ├── StyleAnalyzer.java            # Analyze content for style
   ├── StyleMetrics.java             # Quantified style attributes
   └── StyleGuide.java               # Generated style guide
   ```

2. **Style Metrics**
   - Average sentence length
   - Vocabulary complexity (Flesch-Kincaid)
   - Heading patterns and frequency
   - List usage patterns
   - Code block frequency and style
   - Tone indicators (formal/informal)

3. **Style Guide Generation**
   - Analyze existing wiki pages
   - Extract common patterns
   - Generate style guide for writer agent

4. **Writer Agent Integration**
   - Include style guide in writer prompt
   - Match detected style characteristics

**Tests**:
- Style extraction from sample pages
- Style matching validation
- Consistency scoring

**Success Criteria**:
- Can extract style from 10+ pages
- Generated content matches existing style
- Style guide is human-readable

---

### Milestone 2.4: Category Taxonomy Analysis

**Project**: AI Publisher
**Estimated Sessions**: 1
**Prerequisites**: 2.1

**Objective**: Understand and utilize existing category structure.

**Deliverables**:

1. **Category Analysis Models**
   ```
   src/main/java/com/jakefear/aipublisher/categories/
   ├── CategoryTaxonomy.java         # Full category tree
   ├── CategoryAnalyzer.java         # Analyze category usage
   ├── CategorySuggester.java        # Suggest categories for new content
   └── CategoryHealth.java           # Category balance metrics
   ```

2. **Category Analysis**
   - Build category hierarchy from existing pages
   - Calculate category sizes
   - Identify imbalanced categories
   - Detect uncategorized pages

3. **Category Suggestion**
   - Suggest categories for new content based on topic
   - Ensure consistent categorization
   - Warn about over-categorization

**Tests**:
- Taxonomy extraction
- Category suggestion accuracy
- Balance detection

**Success Criteria**:
- Accurate taxonomy extraction
- Relevant category suggestions
- Category health metrics

---

### Milestone 2.5: Wiki Context Integration

**Project**: AI Publisher
**Estimated Sessions**: 2
**Prerequisites**: 2.1, 2.2, 2.3, 2.4

**Objective**: Integrate all wiki awareness into unified context for generation.

**Deliverables**:

1. **Unified Wiki Context**
   ```
   src/main/java/com/jakefear/aipublisher/wiki/
   ├── WikiContext.java              # Unified context object
   ├── WikiContextBuilder.java       # Build context from scans
   └── WikiContextProvider.java      # Provide context to pipeline
   ```

2. **Context Contents**
   - Page index with topics
   - Link graph
   - Glossary (extracted)
   - Style guide (extracted)
   - Category taxonomy
   - Gap analysis results

3. **Pipeline Integration**
   - Load wiki context at pipeline start
   - Provide to all agents
   - Use for linking, categorization, style

4. **Caching**
   - Cache wiki context
   - Invalidate on wiki changes
   - Incremental updates

**Tests**:
- Context building performance
- Cache invalidation
- Agent integration

**Success Criteria**:
- Full context built in <5 seconds
- All agents use wiki context
- Cache works correctly

---

## Phase 3: Topic Universe and Planning

**Goal**: Enable planning and generation of comprehensive topic coverage.

**Duration**: Milestones 3.1 - 3.5 (5 milestones)

---

### Milestone 3.1: Topic Universe Data Model

**Project**: AI Publisher
**Estimated Sessions**: 2
**Prerequisites**: Phase 2 complete

**Objective**: Define data structures for representing domain topic universes.

**Deliverables**:

1. **Topic Universe Models**
   ```
   src/main/java/com/jakefear/aipublisher/universe/
   ├── Topic.java                    # Single topic with attributes
   ├── TopicRelationship.java        # Relationship between topics
   ├── RelationshipType.java         # PREREQUISITE_OF, PART_OF, EXAMPLE_OF, etc.
   ├── TopicCluster.java             # Group of related topics
   ├── TopicUniverse.java            # Complete domain model
   └── TopicUniverseBuilder.java     # Build universe from various sources
   ```

2. **Topic Attributes**
   - Name, description
   - Complexity level
   - Content type recommendation
   - Estimated depth
   - Audience segments

3. **Relationship Types**
   - PREREQUISITE_OF
   - PART_OF
   - EXAMPLE_OF
   - RELATED_TO
   - CONTRASTS_WITH
   - IMPLEMENTS
   - SUPERSEDES

4. **Serialization**
   - Save/load universe to JSON
   - Import from external sources
   - Merge universes

**Tests**:
- Model construction
- Relationship integrity
- Serialization round-trip

**Success Criteria**:
- Can represent complex domain
- Relationships are typed and directional
- Persists correctly

---

### Milestone 3.2: Domain Discovery Agent

**Project**: AI Publisher
**Estimated Sessions**: 3
**Prerequisites**: 3.1

**Objective**: Automatically discover topic universe from seed topics.

**Deliverables**:

1. **Discovery Agent**
   ```
   src/main/java/com/jakefear/aipublisher/discovery/
   ├── DomainDiscoveryAgent.java     # Agent that discovers topics
   ├── DiscoveryPrompts.java         # Prompts for topic discovery
   ├── TopicExpander.java            # Expand seed topics
   └── RelationshipMapper.java       # Map relationships between topics
   ```

2. **Discovery Process**
   - Accept seed topics from user
   - Query LLM for subtopics, prerequisites, related topics
   - Build initial topic list
   - Map relationships between topics
   - Identify clusters

3. **Iterative Refinement**
   - Multiple expansion passes
   - Gap identification
   - Scope limiting

4. **CLI Integration**
   - `--discover "Domain Name" --seeds "Topic1,Topic2"`
   - Output universe to JSON file

**Tests**:
- Discovery from simple domain
- Relationship mapping accuracy
- Scope control

**Success Criteria**:
- Discovers 30-50 topics from 3-5 seeds
- Relationships are meaningful
- Clusters emerge naturally

---

### Milestone 3.3: Coverage Planning

**Project**: AI Publisher
**Estimated Sessions**: 2
**Prerequisites**: 3.1, 3.2, 2.2 (gap analysis)

**Objective**: Plan comprehensive coverage of a topic universe.

**Deliverables**:

1. **Coverage Planning Models**
   ```
   src/main/java/com/jakefear/aipublisher/planning/
   ├── CoveragePlan.java             # Plan for covering a universe
   ├── PagePlan.java                 # Plan for a single page
   ├── CoveragePlanner.java          # Generate coverage plan
   └── CoverageMetrics.java          # Track coverage progress
   ```

2. **Planning Logic**
   - Assign content types to topics
   - Calculate depth targets
   - Plan cross-references
   - Estimate generation costs
   - Prioritize by importance

3. **Existing Content Integration**
   - Check wiki context for existing coverage
   - Plan only for gaps
   - Plan updates for thin content

4. **CLI Integration**
   - `--plan-coverage universe.json`
   - Output plan to JSON/Markdown

**Tests**:
- Plan generation
- Priority ordering
- Existing content detection

**Success Criteria**:
- Comprehensive plan for universe
- Respects existing content
- Priorities make sense

---

### Milestone 3.4: Information Architecture Design

**Project**: AI Publisher
**Estimated Sessions**: 2
**Prerequisites**: 3.1, 3.3

**Objective**: Automatically design wiki structure for a topic universe.

**Deliverables**:

1. **Architecture Models**
   ```
   src/main/java/com/jakefear/aipublisher/architecture/
   ├── WikiArchitecture.java         # Complete architecture
   ├── PageHierarchy.java            # Page organization
   ├── NavigationPlan.java           # Navigation structure
   ├── ArchitecturePattern.java      # HUB_SPOKE, PROGRESSIVE, FACETED, etc.
   └── ArchitectureDesigner.java     # Design architecture from universe
   ```

2. **Architecture Patterns**
   - Hub and Spoke
   - Progressive Disclosure
   - Task-Based
   - Faceted
   - Narrative

3. **Design Logic**
   - Analyze universe structure
   - Recommend appropriate pattern
   - Design landing pages
   - Plan category taxonomy
   - Design learning paths

**Tests**:
- Pattern selection for various domains
- Hierarchy generation
- Landing page planning

**Success Criteria**:
- Sensible pattern selection
- Complete hierarchy design
- Learning paths are coherent

---

### Milestone 3.5: Generation Sequencing

**Project**: AI Publisher
**Estimated Sessions**: 1
**Prerequisites**: 3.3, 3.4

**Objective**: Determine optimal order for generating pages.

**Deliverables**:

1. **Sequencing Models**
   ```
   src/main/java/com/jakefear/aipublisher/sequencing/
   ├── GenerationSequence.java       # Ordered list of pages to generate
   ├── DependencyGraph.java          # Page dependencies
   └── SequencePlanner.java          # Plan generation order
   ```

2. **Sequencing Logic**
   - Topological sort by prerequisites
   - Glossary and landing pages first
   - Core concepts before tutorials
   - Comparisons after options exist

3. **Dependency Tracking**
   - Track which pages exist
   - Update sequence as pages complete
   - Handle circular dependencies

**Tests**:
- Sequence generation
- Dependency satisfaction
- Circular dependency handling

**Success Criteria**:
- Valid generation order
- No broken links during generation
- Prerequisites available when needed

---

## Phase 4: Multi-Article Orchestration

**Goal**: Generate complete wiki sections with coherent, interconnected content.

**Duration**: Milestones 4.1 - 4.5 (5 milestones)

---

### Milestone 4.1: Batch Generation Pipeline

**Project**: AI Publisher
**Estimated Sessions**: 3
**Prerequisites**: Phase 3 complete

**Objective**: Generate multiple articles in a coordinated batch.

**Deliverables**:

1. **Batch Pipeline Models**
   ```
   src/main/java/com/jakefear/aipublisher/batch/
   ├── BatchPipeline.java            # Orchestrate batch generation
   ├── BatchConfig.java              # Batch configuration
   ├── BatchProgress.java            # Track progress
   ├── BatchResult.java              # Batch results
   └── BatchReporter.java            # Progress reporting
   ```

2. **Batch Capabilities**
   - Generate from coverage plan
   - Follow generation sequence
   - Track progress
   - Handle failures gracefully
   - Resume interrupted batches

3. **Resource Management**
   - Rate limiting for API calls
   - Cost tracking
   - Parallelism control

4. **CLI Integration**
   - `--batch plan.json`
   - `--resume batch-id`
   - `--max-pages N`

**Tests**:
- Batch of 5 articles
- Failure recovery
- Progress tracking

**Success Criteria**:
- Can generate 10+ articles in batch
- Handles failures without losing progress
- Reports progress accurately

---

### Milestone 4.2: Cross-Article Coherence Validation

**Project**: AI Publisher
**Estimated Sessions**: 2
**Prerequisites**: 4.1, 1.2 (glossary)

**Objective**: Validate coherence across generated articles.

**Deliverables**:

1. **Coherence Validation Models**
   ```
   src/main/java/com/jakefear/aipublisher/coherence/
   ├── CoherenceValidator.java       # Validate cross-article coherence
   ├── CoherenceIssue.java           # Identified issue
   ├── TerminologyConflict.java      # Conflicting term usage
   ├── FactualConflict.java          # Conflicting facts
   └── CoherenceReport.java          # Full coherence report
   ```

2. **Validation Checks**
   - Terminology consistency
   - Factual consistency (no contradictions)
   - Link validity (all internal links resolve)
   - Style consistency
   - Coverage completeness

3. **Conflict Resolution**
   - Identify conflicting statements
   - Suggest resolutions
   - Flag for human review

**Tests**:
- Conflict detection
- Resolution suggestions
- Report generation

**Success Criteria**:
- Detects terminology conflicts
- Finds factual contradictions
- Generates actionable report

---

### Milestone 4.3: Incremental Enhancement Passes

**Project**: AI Publisher
**Estimated Sessions**: 2
**Prerequisites**: 4.1, 4.2

**Objective**: Improve wiki quality through multiple enhancement passes.

**Deliverables**:

1. **Enhancement Pass Models**
   ```
   src/main/java/com/jakefear/aipublisher/enhancement/
   ├── EnhancementPass.java          # Single enhancement pass
   ├── EnhancementType.java          # DEPTH, LINKING, EXAMPLES, POLISH
   ├── EnhancementRunner.java        # Run enhancement passes
   └── EnhancementResult.java        # Results of enhancement
   ```

2. **Pass Types**
   - **Depth Pass**: Expand thin pages
   - **Linking Pass**: Add missing links, fix orphans
   - **Examples Pass**: Add missing examples
   - **Polish Pass**: Improve prose, fix consistency

3. **Pass Orchestration**
   - Run passes in optimal order
   - Track improvements
   - Limit iterations

**Tests**:
- Individual pass effectiveness
- Multi-pass improvement
- Convergence

**Success Criteria**:
- Measurable improvement per pass
- Converges to quality target
- No regressions

---

### Milestone 4.4: Landing Page Generation

**Project**: AI Publisher
**Estimated Sessions**: 2
**Prerequisites**: 4.1, 3.4 (architecture)

**Objective**: Generate effective landing and hub pages.

**Deliverables**:

1. **Landing Page Models**
   ```
   src/main/java/com/jakefear/aipublisher/landing/
   ├── LandingPageType.java          # DOMAIN, CLUSTER, AUDIENCE, TASK
   ├── LandingPageTemplate.java      # Template for landing pages
   ├── LandingPageGenerator.java     # Generate landing pages
   └── LandingPageContent.java       # Content model for landing
   ```

2. **Landing Page Types**
   - Domain landing (main entry)
   - Cluster landing (topic group)
   - Audience landing (by reader type)
   - Task landing (by goal)

3. **Content Elements**
   - Executive summary
   - Quick start links
   - Topic organization
   - Learning paths
   - See also

4. **Dynamic Elements**
   - Recent changes widget
   - Popular pages widget
   - Search integration

**Tests**:
- Each landing type generation
- Content completeness
- Link validity

**Success Criteria**:
- Professional landing pages
- Effective navigation
- Dynamic elements work

---

### Milestone 4.5: Learning Path Generation

**Project**: AI Publisher
**Estimated Sessions**: 2
**Prerequisites**: 4.4, 3.4

**Objective**: Generate structured learning paths through content.

**Deliverables**:

1. **Learning Path Models**
   ```
   src/main/java/com/jakefear/aipublisher/paths/
   ├── LearningPath.java             # Complete learning path
   ├── PathStep.java                 # Step in path
   ├── PathType.java                 # ONBOARDING, SKILL_BUILDING, etc.
   ├── PathGenerator.java            # Generate paths
   └── PathPage.java                 # Navigation page for path
   ```

2. **Path Types**
   - Onboarding (quick start)
   - Conceptual (deep understanding)
   - Skill Building (progressive capability)
   - Migration (from alternative)
   - Deep Dive (expert level)

3. **Path Features**
   - Time estimates
   - Checkpoints
   - Prerequisites
   - Outcomes

**Tests**:
- Path generation for sample domain
- Path validity (all pages exist)
- Time estimate accuracy

**Success Criteria**:
- Coherent learning paths
- Appropriate difficulty progression
- Clear outcomes

---

## Phase 5: JSPWiki Integration

**Goal**: Enhance JSPWiki to better support AI-generated content.

**Duration**: Milestones 5.1 - 5.4 (4 milestones)

---

### Milestone 5.1: Content Templates Plugin

**Project**: JSPWiki
**Estimated Sessions**: 3
**Prerequisites**: Phase 1 complete

**Objective**: Create JSPWiki plugins for content templates used by AI Publisher.

**Deliverables**:

1. **Template Plugins**
   ```
   jspwiki-main/src/main/java/org/apache/wiki/plugin/
   ├── InfoBoxPlugin.java            # Quick facts sidebar
   ├── PrerequisitePlugin.java       # Prerequisite callout
   ├── ExamplePlugin.java            # Code example with highlighting
   ├── ComparisonTablePlugin.java    # Side-by-side comparison
   └── PathNavigationPlugin.java     # Learning path progress
   ```

2. **Plugin Features**
   - InfoBox: title, facts list, optional icon
   - Prerequisite: required pages, recommended pages
   - Example: language, title, code, explanation
   - Comparison: items, criteria, ratings
   - PathNavigation: path name, current step, total

3. **Styling**
   - CSS for each plugin
   - Consistent visual language
   - Mobile-responsive

**Tests**:
- Each plugin renders correctly
- Parameter validation
- Error handling

**Success Criteria**:
- All plugins work in JSPWiki
- AI Publisher uses plugins in output
- Plugins look professional

---

### Milestone 5.2: Transclusion Enhancement

**Project**: JSPWiki
**Estimated Sessions**: 2
**Prerequisites**: 5.1

**Objective**: Enhance JSPWiki transclusion for AI-generated content reuse.

**Deliverables**:

1. **Transclusion Enhancements**
   - Named sections for partial transclusion
   - Conditional transclusion based on variables
   - Transclusion with parameter substitution

2. **Content Block Plugin**
   ```
   jspwiki-main/src/main/java/org/apache/wiki/plugin/
   └── ContentBlockPlugin.java       # Define reusable content blocks
   ```

3. **Usage Patterns**
   - Shared definitions
   - Common warnings
   - Boilerplate sections

**Tests**:
- Transclusion functionality
- Parameter substitution
- Conditional logic

**Success Criteria**:
- Named section transclusion works
- AI Publisher uses transclusion
- No duplicate content

---

### Milestone 5.3: Navigation Enhancement

**Project**: JSPWiki
**Estimated Sessions**: 2
**Prerequisites**: 5.1

**Objective**: Improve JSPWiki navigation for AI-generated content structure.

**Deliverables**:

1. **Navigation Plugins**
   ```
   jspwiki-main/src/main/java/org/apache/wiki/plugin/
   ├── BreadcrumbPlugin.java         # Hierarchical breadcrumbs
   ├── ClusterNavigationPlugin.java  # Navigate within topic cluster
   └── LearningPathPlugin.java       # Display current position in path
   ```

2. **Navigation Features**
   - Automatic breadcrumbs from page hierarchy
   - Previous/Next navigation within clusters
   - Learning path progress indicator

**Tests**:
- Navigation rendering
- Hierarchy detection
- Path tracking

**Success Criteria**:
- Breadcrumbs show hierarchy
- Cluster navigation works
- Path progress displays

---

### Milestone 5.4: Quality Dashboard Page

**Project**: JSPWiki
**Estimated Sessions**: 2
**Prerequisites**: 5.1, 5.3

**Objective**: Create wiki dashboard showing content quality metrics.

**Deliverables**:

1. **Dashboard Plugins**
   ```
   jspwiki-main/src/main/java/org/apache/wiki/plugin/
   ├── CoverageMetricsPlugin.java    # Show coverage statistics
   ├── ContentHealthPlugin.java      # Show content health indicators
   └── GapListPlugin.java            # List content gaps
   ```

2. **Dashboard Elements**
   - Coverage percentage by category
   - Orphan page list
   - Thin page list
   - Recent AI-generated pages
   - Quality trend over time

**Tests**:
- Metrics calculation
- Dashboard rendering
- Performance

**Success Criteria**:
- Dashboard shows accurate metrics
- Actionable insights
- Fast rendering

---

## Phase 6: Quality and Polish

**Goal**: Production-ready quality and usability.

**Duration**: Milestones 6.1 - 6.4 (4 milestones)

---

### Milestone 6.1: Consistency Rules Engine

**Project**: AI Publisher
**Estimated Sessions**: 2
**Prerequisites**: Phase 4 complete

**Objective**: Configurable rules engine for content consistency.

**Deliverables**:

1. **Rules Engine**
   ```
   src/main/java/com/jakefear/aipublisher/rules/
   ├── ConsistencyRule.java          # Rule interface
   ├── RulesEngine.java              # Execute rules
   ├── RuleViolation.java            # Violation details
   ├── RuleConfig.java               # Rule configuration
   └── rules/                        # Built-in rules
       ├── TerminologyRule.java
       ├── HeadingHierarchyRule.java
       ├── LinkDensityRule.java
       ├── CategoryRequirementRule.java
       └── ExamplePresenceRule.java
   ```

2. **Rule Features**
   - Configurable thresholds
   - Severity levels
   - Auto-fix suggestions
   - Custom rules support

**Tests**:
- Each built-in rule
- Rule configuration
- Auto-fix accuracy

**Success Criteria**:
- Rules catch real issues
- Configurable behavior
- Useful auto-fixes

---

### Milestone 6.2: Quality Reporting

**Project**: AI Publisher
**Estimated Sessions**: 2
**Prerequisites**: 6.1

**Objective**: Comprehensive quality reporting for generated content.

**Deliverables**:

1. **Quality Reports**
   ```
   src/main/java/com/jakefear/aipublisher/quality/
   ├── QualityReport.java            # Complete quality report
   ├── QualityMetrics.java           # Calculated metrics
   ├── QualityReporter.java          # Generate reports
   └── QualityTrend.java             # Track quality over time
   ```

2. **Report Contents**
   - Overall quality score
   - Dimension scores (coverage, coherence, style, etc.)
   - Issue list with priorities
   - Recommendations
   - Comparison to targets

3. **Report Formats**
   - Console output
   - Markdown report
   - JSON for tooling

**Tests**:
- Report generation
- Metric calculation
- Format output

**Success Criteria**:
- Actionable reports
- Accurate metrics
- Multiple formats

---

### Milestone 6.3: Cost Tracking and Optimization

**Project**: AI Publisher
**Estimated Sessions**: 1
**Prerequisites**: 4.1

**Objective**: Track and optimize API costs for generation.

**Deliverables**:

1. **Cost Tracking**
   ```
   src/main/java/com/jakefear/aipublisher/cost/
   ├── CostTracker.java              # Track API costs
   ├── CostEstimator.java            # Estimate costs before generation
   ├── CostReport.java               # Cost reporting
   └── CostOptimizer.java            # Suggestions for cost reduction
   ```

2. **Features**
   - Token counting
   - Cost per article
   - Cost per batch
   - Estimation before generation
   - Budget limits

**Tests**:
- Cost tracking accuracy
- Estimation accuracy
- Budget enforcement

**Success Criteria**:
- Accurate cost tracking
- Useful estimates
- Budget limits work

---

### Milestone 6.4: CLI Polish and Documentation

**Project**: AI Publisher
**Estimated Sessions**: 2
**Prerequisites**: All previous milestones

**Objective**: Polish CLI experience and complete documentation.

**Deliverables**:

1. **CLI Improvements**
   - Consistent command structure
   - Helpful error messages
   - Progress indicators
   - Interactive mode enhancements

2. **Documentation**
   - Updated README
   - Command reference
   - Tutorial: Single article
   - Tutorial: Batch generation
   - Tutorial: Wiki planning
   - Configuration reference

3. **Examples**
   - Sample glossary
   - Sample universe definition
   - Sample coverage plan

**Tests**:
- CLI usability
- Documentation accuracy
- Example validity

**Success Criteria**:
- Intuitive CLI
- Complete documentation
- Working examples

---

## Appendix A: Session Planning Guide

### Before Starting a Milestone

1. Read milestone description completely
2. Review prerequisites
3. Check current state of code
4. Identify files to create/modify
5. Plan test approach

### During Development

1. Create models/interfaces first
2. Implement core logic
3. Write unit tests
4. Integrate with existing code
5. Write integration tests
6. Update CLI if applicable

### After Completing a Milestone

1. Run full test suite
2. Update documentation
3. Commit with descriptive message
4. Note any issues for next session
5. Update this plan if needed

---

## Appendix B: Dependency Graph

```
Phase 1 (Single Article)
├── 1.1 Content Types
├── 1.2 Glossary ──────────────────┐
├── 1.3 Linking ◄──────────────────┤
├── 1.4 Examples ◄── 1.1           │
├── 1.5 Prerequisites ◄── 1.3      │
└── 1.6 See Also ◄── 1.3, 1.5      │
                                   │
Phase 2 (Wiki Awareness)           │
├── 2.1 Wiki Scanner               │
├── 2.2 Gap Analysis ◄── 2.1       │
├── 2.3 Style Learning ◄── 2.1     │
├── 2.4 Category Analysis ◄── 2.1  │
└── 2.5 Wiki Context ◄── 2.1-2.4 ──┘

Phase 3 (Planning)
├── 3.1 Topic Universe
├── 3.2 Discovery ◄── 3.1
├── 3.3 Coverage Planning ◄── 3.1, 3.2, 2.2
├── 3.4 Architecture ◄── 3.1, 3.3
└── 3.5 Sequencing ◄── 3.3, 3.4

Phase 4 (Orchestration)
├── 4.1 Batch Pipeline ◄── Phase 3
├── 4.2 Coherence Validation ◄── 4.1, 1.2
├── 4.3 Enhancement Passes ◄── 4.1, 4.2
├── 4.4 Landing Pages ◄── 4.1, 3.4
└── 4.5 Learning Paths ◄── 4.4, 3.4

Phase 5 (JSPWiki) - Can parallel with Phase 4
├── 5.1 Template Plugins
├── 5.2 Transclusion ◄── 5.1
├── 5.3 Navigation ◄── 5.1
└── 5.4 Dashboard ◄── 5.1, 5.3

Phase 6 (Polish)
├── 6.1 Rules Engine ◄── Phase 4
├── 6.2 Quality Reporting ◄── 6.1
├── 6.3 Cost Tracking ◄── 4.1
└── 6.4 CLI Polish ◄── All
```

---

## Appendix C: Milestone Checklist Template

```markdown
## Milestone X.Y: [Name]

### Status: [ ] Not Started / [ ] In Progress / [ ] Complete

### Prerequisites Check
- [ ] Prerequisite A complete
- [ ] Prerequisite B complete

### Deliverables
- [ ] Model classes created
- [ ] Core logic implemented
- [ ] Unit tests passing
- [ ] Integration complete
- [ ] Integration tests passing
- [ ] CLI updated (if applicable)
- [ ] Documentation updated

### Notes
[Session notes here]

### Issues/Blockers
[Any issues encountered]

### Next Steps
[What comes next]
```

---

*Document Version: 1.0*
*Last Updated: 2025-12-13*
*Purpose: Implementation Roadmap for Advanced Capabilities*
