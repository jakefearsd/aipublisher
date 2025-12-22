# CLAUDE.md - AI Publisher Development Guidelines

## Project Overview

AI Publisher is a multi-agent content generation system built on Spring Boot 3.3.6 and LangChain4j. It orchestrates specialized AI agents through a publishing pipeline to produce wiki-style articles with research, fact-checking, editing, and critique phases.

**Key stats:** ~17,000 LOC production code, 71 test files, 1,190+ tests, Java 21

## Development Philosophy

### Test-Driven Development (TDD)

**Always write tests first.** This codebase has excellent test coverage - maintain it.

```
RED → GREEN → REFACTOR
```

1. **RED**: Write a failing test that describes the desired behavior
2. **GREEN**: Write the minimum code to make the test pass
3. **REFACTOR**: Improve the code while keeping tests green

**Test organization pattern used in this project:**
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

**Before implementing any feature:**
1. Create test file if it doesn't exist
2. Write test cases for the happy path
3. Write test cases for edge cases and errors
4. Only then implement the production code

### Incremental Progress

**Small, focused commits.** Each commit should:
- Do one thing well
- Include tests for new behavior
- Leave the codebase in a working state
- Have a clear, descriptive message following conventional commits

**Conventional commit format:**
```
type: short description

Longer explanation of what and why (not how).

🤖 Generated with [Claude Code](https://claude.com/claude-code)
Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
```

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`

## Architecture & Design Patterns

This codebase uses Gang of Four patterns extensively. **Prefer these patterns over ad-hoc solutions.**

### Patterns in Active Use

#### Strategy Pattern
**Location:** `cli/strategy/`, `search/SearchProvider`

Use when behavior varies by type or configuration.
```java
// Interface defines the contract
public interface ContentTypeQuestionStrategy {
    Set<ContentType> getApplicableTypes();
    List<ContentTypeQuestion> getQuestions();
}

// Implementations vary the algorithm
class TutorialQuestionStrategy implements ContentTypeQuestionStrategy { }
class ComparisonQuestionStrategy implements ContentTypeQuestionStrategy { }
```

**When to use:** Multiple algorithms/behaviors selectable at runtime.

#### State Pattern
**Location:** `document/DocumentState`

Encapsulates state-specific behavior and transitions.
```java
public enum DocumentState {
    CREATED, RESEARCHING, DRAFTING, FACT_CHECKING, EDITING, CRITIQUING, PUBLISHED, REJECTED;

    public boolean canTransitionTo(DocumentState target) { }
    public DocumentState getNextInFlow() { }
}
```

**When to use:** Objects with complex state machines.

#### Template Method Pattern
**Location:** `agent/BaseAgent`

Defines algorithm skeleton, lets subclasses override specific steps.
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

**When to use:** Common algorithm structure with varying implementation details.

#### Observer Pattern
**Location:** `monitoring/PipelineEventListener`

Decouples event producers from consumers.
```java
@FunctionalInterface
public interface PipelineEventListener {
    void onEvent(PipelineEvent event);
}
```

**When to use:** Multiple components need to react to state changes.

#### Builder Pattern
**Location:** `domain/Topic.Builder`, `domain/DomainContext.Builder`

Constructs complex objects step-by-step.
```java
Topic topic = Topic.builder("Machine Learning")
    .withDescription("Introduction to ML concepts")
    .withComplexity(ComplexityLevel.INTERMEDIATE)
    .withPriority(Priority.HIGH)
    .build();
```

**When to use:** Objects with many optional parameters or complex construction.

#### Registry Pattern
**Location:** `cli/strategy/ContentTypeQuestionStrategyRegistry`, `search/SearchProviderRegistry`

Central lookup for typed implementations.
```java
public class SearchProviderRegistry {
    private final Map<String, SearchProvider> providers = new ConcurrentHashMap<>();

    public void register(SearchProvider provider) { }
    public Optional<SearchProvider> get(String name) { }
    public SearchProvider getDefault() { }
}
```

**When to use:** Need to look up implementations by key/type.

#### Command Pattern
**Location:** Originally in `cli/curation/` (now in aidiscovery project)

Encapsulates actions as objects.

**When to use:** Need to queue, log, or undo operations.

### Pattern Selection Guide

| Situation | Pattern |
|-----------|---------|
| Multiple algorithms, choose at runtime | Strategy |
| Object behavior changes with internal state | State |
| Common structure, varying implementation | Template Method |
| Loose coupling for event notification | Observer |
| Complex object construction | Builder |
| Look up implementations by key | Registry |
| Encapsulate operations as objects | Command |
| Create objects without specifying class | Factory Method |
| Single instance needed globally | Singleton (use Spring `@Component`) |

## Code Style Conventions

### Java Records for Value Objects
```java
// Prefer records for immutable data
public record SearchResult(
    String title,
    String snippet,
    String url,
    SourceReliability reliability
) { }
```

### Enums with Behavior
```java
public enum ContentType {
    CONCEPT("Concept Explanation", "What is X?"),
    TUTORIAL("Step-by-Step Tutorial", "How to X");

    private final String displayName;
    private final String questionFormat;

    // Methods that operate on enum values
    public String formatQuestion(String topic) { }
}
```

### Interface Segregation
Keep interfaces focused and cohesive:
```java
// Good: Focused interface
public interface Agent {
    PublishingDocument process(PublishingDocument document);
    boolean validate(PublishingDocument document);
    AgentRole getRole();
}

// Avoid: Kitchen sink interfaces
```

### Null Safety
- Use `Optional<T>` for return types that may be absent
- Use `@Nullable` annotations when null is valid
- Validate inputs at public API boundaries
- Prefer empty collections over null

## Testing Guidelines

### Test File Naming
```
src/main/java/.../SomeClass.java
src/test/java/.../SomeClassTest.java
```

### Integration Tests
```java
@Tag("integration")
@EnabledIfLlmAvailable
@DisplayName("WriterAgent Integration")
class WriterAgentIntegrationTest {
    // Uses real Ollama instance
}
```

Run integration tests:
```bash
mvn test -Dgroups=integration
```

### Test Data Builders
Create test fixtures using builders:
```java
private PublishingDocument createTestDocument() {
    return new PublishingDocument(
        TopicBrief.builder("Test Topic")
            .withContentType(ContentType.CONCEPT)
            .build()
    );
}
```

### Assertions
Use AssertJ for fluent assertions:
```java
assertThat(result.getState()).isEqualTo(DocumentState.PUBLISHED);
assertThat(topics).hasSize(3).extracting(Topic::name).contains("Expected");
```

## Common Tasks

### Adding a New Agent

1. **Test first:**
```java
class NewAgentTest {
    @Nested class Process {
        @Test void shouldTransformDocumentCorrectly() { }
        @Test void shouldHandleEmptyInput() { }
        @Test void shouldValidateOutput() { }
    }
}
```

2. **Implement agent:**
```java
public class NewAgent extends BaseAgent {
    public NewAgent(ChatLanguageModel model, String systemPrompt) {
        super(model, systemPrompt, AgentRole.NEW_ROLE);
    }

    @Override
    protected void doProcess(PublishingDocument document) { }
}
```

3. **Register in pipeline** (if needed)

### Adding a New Content Type Strategy

1. **Create strategy test**
2. **Implement `ContentTypeQuestionStrategy`**
3. **Register in `ContentTypeQuestionStrategyRegistry`**

### Adding a New Search Provider

1. **Create provider test**
2. **Implement `SearchProvider` interface**
3. **Register in `SearchProviderRegistry`**

## Micro-Design Decision Framework

When making design decisions, evaluate against these criteria:

### 1. Testability
- Can I test this in isolation?
- Do I need to mock external dependencies?
- Are test setup requirements reasonable?

### 2. Single Responsibility
- Does this class/method do one thing well?
- Can I describe its purpose in one sentence without "and"?

### 3. Open/Closed
- Can I extend behavior without modifying existing code?
- Am I using interfaces/abstract classes appropriately?

### 4. Dependency Direction
- Do dependencies point toward abstractions?
- Am I depending on interfaces rather than implementations?

### 5. Cohesion
- Do all methods in this class relate to its core purpose?
- Should this be split into multiple classes?

## Anti-Patterns to Avoid

### God Classes
**Problem:** `AiPublisherCommand.java` at 1,060 lines is at the limit.
**Solution:** Extract cohesive functionality into separate classes.

### Primitive Obsession
```java
// Avoid
void process(String topic, String type, int complexity, boolean featured);

// Prefer
void process(TopicBrief brief);
```

### Feature Envy
```java
// Avoid: Method uses another object's data extensively
void format(Document doc) {
    doc.getTitle() + doc.getAuthor() + doc.getDate()...
}

// Prefer: Put method where the data lives
class Document {
    String format() { }
}
```

### Shotgun Surgery
If changing one feature requires modifying many files, consider:
- Better abstraction
- Facade pattern
- More cohesive packaging

## Build & Test Commands

```bash
# Run all tests
mvn test

# Run unit tests only (fast)
mvn test -DexcludeGroups=integration

# Run integration tests only (needs Ollama)
mvn test -Dgroups=integration

# Build JAR
mvn package -DskipTests

# Run the CLI
java -jar target/aipublisher.jar --help
```

## Environment Configuration

Integration tests use Ollama by default:
- **Default URL:** `http://inference.jakefear.com:11434`
- **Default model:** `qwen2.5:14b`
- **Override:** Set `OLLAMA_BASE_URL` and `OLLAMA_MODEL` environment variables

## Related Projects

- **aidiscovery** - Topic universe discovery (sibling project)
- Both share the `TopicUniverse` JSON format at `~/.aipublisher/universes/`

## Notes for Future Development

1. **Keep AiPublisherCommand.java from growing** - Extract new features into dedicated classes
2. **Maintain test ratio** - Current ratio is ~71 test files for ~100 production files
3. **Document patterns** - When adding new patterns, document them here
4. **Incremental refactoring** - Address technical debt in small, tested steps

---

*Last updated: 2025-12-22 (transition commit 2d8b288)*
