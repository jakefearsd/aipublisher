# AI Publisher

A multi-agent publishing framework that uses Claude AI to generate high-quality wiki articles from topic briefs. The system coordinates four specialized AI agents to research, write, fact-check, and edit content before publishing to JSPWiki-compatible Markdown format.

## Features

- **Multi-Agent Pipeline**: Four specialized agents work together:
  - **Research Agent**: Gathers facts, sources, and creates an outline
  - **Writer Agent**: Drafts the article using research
  - **Fact Checker Agent**: Verifies claims and flags issues
  - **Editor Agent**: Polishes content and adds internal wiki links

- **Quality Assurance**: Built-in validation, revision cycles, and quality scoring
- **Human Approval Checkpoints**: Optional approval gates at any pipeline stage
- **JSPWiki Output**: Generates Markdown with proper wiki link syntax
- **Pipeline Monitoring**: Events and metrics for tracking execution

## Requirements

- Java 21+
- Maven 3.8+
- Anthropic API key

## Quick Start

1. **Build the project**:
   ```bash
   mvn clean package -DskipTests
   ```

2. **Set your API key**:
   ```bash
   export ANTHROPIC_API_KEY='your-api-key'
   ```

3. **Run with a topic**:
   ```bash
   java -jar target/aipublisher-0.0.1-SNAPSHOT.jar --topic="Apache Kafka" --audience="developers" --length=1000
   ```

## Configuration

### Application Properties

Configure in `src/main/resources/application.yml`:

```yaml
anthropic:
  api-key: ${ANTHROPIC_API_KEY}
  model-name: claude-sonnet-4-20250514

pipeline:
  max-revision-cycles: 3
  approval:
    auto-approve: true        # Set to false for manual approval
    after-research: false
    after-draft: false
    after-factcheck: false
    before-publish: true

quality:
  min-research-facts: 3
  min-writer-length: 100
  min-editor-score: 0.7
  max-questionable-claims: 2

output:
  base-path: ./output
  file-extension: .md
```

### Human Approval

Enable manual approval checkpoints:

```yaml
pipeline:
  approval:
    auto-approve: false
    after-research: true
    after-draft: true
    after-factcheck: true
    before-publish: true
```

When enabled, the pipeline pauses and prompts for approval:
- **[A]pprove** - Continue to next phase
- **[R]eject** - Stop the pipeline
- **[C]hanges** - Request changes and retry

## Command Line Options

```bash
java -jar aipublisher.jar [options]

Options:
  --topic=<topic>        Topic to write about (required)
  --audience=<audience>  Target audience (default: general)
  --length=<words>       Target word count (default: 500)
  --output=<path>        Output directory (overrides config)
```

## Architecture

### Pipeline Flow

```
TopicBrief
    │
    ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Research   │────▶│   Writer    │────▶│ Fact Check  │────▶│   Editor    │
│    Agent    │     │    Agent    │     │    Agent    │     │    Agent    │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                                               │
                                          (if issues)
                                               │
                                               ▼
                                        Revision Loop
```

### Document States

- `CREATED` - Initial state with topic brief
- `RESEARCHING` - Research Agent processing
- `DRAFTING` - Writer Agent processing
- `FACT_CHECKING` - Fact Checker Agent processing
- `EDITING` - Editor Agent processing
- `PUBLISHED` - Final output written
- `REJECTED` - Pipeline stopped

### Agent Outputs

Each agent produces structured JSON that is validated and stored:

- **ResearchBrief**: Key facts, sources, suggested outline
- **ArticleDraft**: Markdown content, summary, categories
- **FactCheckReport**: Verified/questionable claims, recommendation
- **FinalArticle**: Polished content, quality score, wiki links

## Development

### Running Tests

```bash
mvn test
```

### Integration Tests (Requires API Key)

```bash
ANTHROPIC_API_KEY=your-key mvn verify
```

### Project Structure

```
src/main/java/com/jakefear/aipublisher/
├── agent/           # AI agent implementations
├── approval/        # Human approval workflow
├── cli/             # Command line runner
├── config/          # Configuration properties
├── document/        # Domain models and state
├── monitoring/      # Events and metrics
├── output/          # Wiki output service
└── pipeline/        # Pipeline orchestration
```

## License

MIT License
