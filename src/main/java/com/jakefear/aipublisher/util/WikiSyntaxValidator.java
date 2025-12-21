package com.jakefear.aipublisher.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates content for proper JSPWiki syntax and detects common Markdown patterns.
 *
 * JSPWiki uses different syntax than Markdown:
 * - Headings: ! !! !!! (not # ## ###)
 * - Bold: __text__ (not **text**)
 * - Italic: ''text'' (not *text* or _text_)
 * - Links: [text|url] (not [text](url))
 * - Code blocks: {{{ }}} (not ``` ```)
 * - Inline code: {{code}} (not `code`)
 */
public class WikiSyntaxValidator {

    // Markdown patterns that should NOT appear in JSPWiki content
    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^#{1,6}\\s+", Pattern.MULTILINE);
    private static final Pattern MARKDOWN_BOLD = Pattern.compile("\\*\\*[^*]+\\*\\*");
    private static final Pattern MARKDOWN_ITALIC_ASTERISK = Pattern.compile("(?<!\\*)\\*(?!\\*)[^*]+(?<!\\*)\\*(?!\\*)");
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[[^\\]]+\\]\\([^)]+\\)");
    private static final Pattern MARKDOWN_CODE_BLOCK = Pattern.compile("```");
    private static final Pattern MARKDOWN_INLINE_CODE = Pattern.compile("`[^`]+`");
    private static final Pattern MARKDOWN_TABLE_SEPARATOR = Pattern.compile("^\\|[-:|]+\\|$", Pattern.MULTILINE);

    /**
     * Result of validating wiki content.
     */
    public record ValidationResult(
            boolean valid,
            List<ValidationIssue> issues
    ) {
        public static ValidationResult success() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult failure(List<ValidationIssue> issues) {
            return new ValidationResult(false, List.copyOf(issues));
        }

        public boolean hasMarkdownSyntax() {
            return issues.stream().anyMatch(issue -> issue.type().startsWith("MARKDOWN_"));
        }

        public String getSummary() {
            if (valid) {
                return "Content uses valid JSPWiki syntax";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(issues.size()).append(" syntax issue(s) found:");
            for (ValidationIssue issue : issues) {
                sb.append("\n  - ").append(issue.type()).append(": ").append(issue.description());
                if (issue.example() != null) {
                    sb.append(" (e.g., \"").append(truncate(issue.example(), 40)).append("\")");
                }
            }
            return sb.toString();
        }

        private static String truncate(String s, int maxLen) {
            if (s.length() <= maxLen) return s;
            return s.substring(0, maxLen - 3) + "...";
        }
    }

    /**
     * A single validation issue found in the content.
     */
    public record ValidationIssue(
            String type,
            String description,
            String example,
            int count
    ) {
        public static ValidationIssue markdown(String type, String description, String example, int count) {
            return new ValidationIssue("MARKDOWN_" + type, description, example, count);
        }
    }

    /**
     * Validate content for JSPWiki syntax compliance.
     * Detects common Markdown patterns that should be converted to JSPWiki.
     *
     * @param content The wiki content to validate
     * @return ValidationResult with any issues found
     */
    public static ValidationResult validate(String content) {
        if (content == null || content.isBlank()) {
            return ValidationResult.success();
        }

        List<ValidationIssue> issues = new ArrayList<>();

        // Check for Markdown headings (# ## ### etc.)
        checkPattern(content, MARKDOWN_HEADING, "HEADING",
                "Markdown heading found (use ! !! !!! instead of # ## ###)", issues);

        // Check for Markdown bold (**text**)
        checkPattern(content, MARKDOWN_BOLD, "BOLD",
                "Markdown bold found (use __text__ instead of **text**)", issues);

        // Check for Markdown links [text](url)
        checkPattern(content, MARKDOWN_LINK, "LINK",
                "Markdown link found (use [text|url] instead of [text](url))", issues);

        // Check for Markdown code blocks (```)
        checkPattern(content, MARKDOWN_CODE_BLOCK, "CODE_BLOCK",
                "Markdown code block found (use {{{ }}} instead of ```)", issues);

        // Check for Markdown inline code (`code`)
        checkPattern(content, MARKDOWN_INLINE_CODE, "INLINE_CODE",
                "Markdown inline code found (use {{code}} instead of `code`)", issues);

        // Check for Markdown table separator (|---|---|)
        checkPattern(content, MARKDOWN_TABLE_SEPARATOR, "TABLE",
                "Markdown table separator found (JSPWiki tables don't use separator rows)", issues);

        if (issues.isEmpty()) {
            return ValidationResult.success();
        }

        return ValidationResult.failure(issues);
    }

    /**
     * Check if content contains any Markdown syntax.
     * This is a quick check that returns true if any Markdown pattern is found.
     *
     * @param content The content to check
     * @return true if Markdown syntax is detected
     */
    public static boolean containsMarkdown(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }

        return MARKDOWN_HEADING.matcher(content).find() ||
               MARKDOWN_BOLD.matcher(content).find() ||
               MARKDOWN_LINK.matcher(content).find() ||
               MARKDOWN_CODE_BLOCK.matcher(content).find() ||
               MARKDOWN_INLINE_CODE.matcher(content).find() ||
               MARKDOWN_TABLE_SEPARATOR.matcher(content).find();
    }

    /**
     * Attempt to auto-fix common Markdown patterns to JSPWiki syntax.
     * This is a best-effort conversion and may not handle all edge cases.
     *
     * @param content The content with potential Markdown syntax
     * @return Content with Markdown converted to JSPWiki where possible
     */
    public static String autoFix(String content) {
        if (content == null) {
            return null;
        }

        String result = content;

        // Convert Markdown headings to JSPWiki
        // #### -> ! (level 3 in JSPWiki)
        // ### -> !! (level 2 in JSPWiki)
        // ## -> !! (level 2 in JSPWiki)
        // # -> !!! (level 1 in JSPWiki - but usually we already have this)
        result = result.replaceAll("(?m)^####\\s+", "! ");
        result = result.replaceAll("(?m)^###\\s+", "!! ");
        result = result.replaceAll("(?m)^##\\s+", "!! ");
        result = result.replaceAll("(?m)^#\\s+", "!!! ");

        // Convert Markdown bold to JSPWiki
        result = result.replaceAll("\\*\\*([^*]+)\\*\\*", "__$1__");

        // Convert Markdown links to JSPWiki
        // [text](url) -> [text|url]
        result = result.replaceAll("\\[([^\\]]+)\\]\\(([^)]+)\\)", "[$1|$2]");

        // Remove Markdown table separators
        result = result.replaceAll("(?m)^\\|[-:|]+\\|\\s*$\\n?", "");

        // Convert Markdown code blocks (basic conversion)
        result = result.replaceAll("```\\w*\\n", "{{{\n");
        result = result.replaceAll("```", "}}}");

        // Convert Markdown inline code
        result = result.replaceAll("`([^`]+)`", "{{$1}}");

        return result;
    }

    private static void checkPattern(String content, Pattern pattern, String type,
                                     String description, List<ValidationIssue> issues) {
        Matcher matcher = pattern.matcher(content);
        List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group().trim());
        }
        if (!matches.isEmpty()) {
            issues.add(ValidationIssue.markdown(type, description, matches.get(0), matches.size()));
        }
    }
}
