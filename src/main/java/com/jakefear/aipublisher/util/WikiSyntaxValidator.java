package com.jakefear.aipublisher.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    // Additional patterns for Wikipedia-style and other Markdown variants
    private static final Pattern DOUBLE_BRACKET_LINK = Pattern.compile("\\[\\[[^\\]]+\\]\\]");
    private static final Pattern MARKDOWN_LIST_DASH = Pattern.compile("^\\s*-\\s+", Pattern.MULTILINE);
    private static final Pattern MARKDOWN_HR_ASTERISK = Pattern.compile("^\\*\\s*\\*\\s*\\*\\s*$", Pattern.MULTILINE);

    // Chain-of-thought tags that leak from LLM responses
    private static final Pattern THINK_TAGS = Pattern.compile("<think>.*?</think>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    // Pattern for JSPWiki section headings (!! or !!! followed by text)
    private static final Pattern SECTION_HEADING = Pattern.compile("^(!!+)\\s+(.+?)\\s*$", Pattern.MULTILINE);

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

        // Check for double-bracket wiki links (Wikipedia style, not JSPWiki)
        checkPattern(content, DOUBLE_BRACKET_LINK, "DOUBLE_BRACKET",
                "Double-bracket links found (use [PageName] instead of [[PageName]])", issues);

        // Check for Markdown dash lists (- item instead of * item)
        checkPattern(content, MARKDOWN_LIST_DASH, "LIST_DASH",
                "Markdown list dashes found (use * for bullets, not -)", issues);

        // Check for Markdown horizontal rules (* * *)
        checkPattern(content, MARKDOWN_HR_ASTERISK, "HORIZONTAL_RULE",
                "Markdown horizontal rule found (use ---- instead of * * *)", issues);

        // Check for leaked chain-of-thought tags
        checkThinkTags(content, issues);

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
               MARKDOWN_TABLE_SEPARATOR.matcher(content).find() ||
               DOUBLE_BRACKET_LINK.matcher(content).find() ||
               MARKDOWN_LIST_DASH.matcher(content).find() ||
               MARKDOWN_HR_ASTERISK.matcher(content).find() ||
               THINK_TAGS.matcher(content).find();
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

        // Convert double brackets to single
        result = result.replaceAll("\\[\\[([^\\]]+)\\]\\]", "[$1]");

        // Convert dash lists to asterisk lists
        result = result.replaceAll("(?m)^(\\s*)-\\s+", "$1* ");

        // Convert * * * horizontal rules to ----
        result = result.replaceAll("(?m)^\\*\\s*\\*\\s*\\*\\s*$", "----");

        // Remove any leaked chain-of-thought tags
        result = THINK_TAGS.matcher(result).replaceAll("");

        return result;
    }

    /**
     * Remove duplicate sections from wiki content.
     * LLMs sometimes enter repetitive loops and generate the same section multiple times.
     * This method keeps only the first occurrence of each section heading.
     *
     * @param content The wiki content potentially containing duplicate sections
     * @return Content with duplicate sections removed
     */
    public static String removeDuplicateSections(String content) {
        if (content == null) {
            return null;
        }
        if (content.isEmpty()) {
            return content;
        }

        // Split content into lines for processing (don't use -1 to avoid empty trailing element)
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        Set<String> seenSections = new HashSet<>();
        boolean skipping = false;
        String currentSectionLevel = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = SECTION_HEADING.matcher(line);

            if (matcher.matches()) {
                String level = matcher.group(1);  // !! or !!!
                String heading = matcher.group(2).trim();  // Section name
                String sectionKey = level + " " + heading;

                if (seenSections.contains(sectionKey)) {
                    // This is a duplicate section - start skipping
                    skipping = true;
                    currentSectionLevel = level;
                } else {
                    // First occurrence - keep it
                    seenSections.add(sectionKey);
                    skipping = false;
                    currentSectionLevel = null;
                    result.append(line).append("\n");
                }
            } else if (skipping) {
                // Check if we've hit a new section at the same or higher level
                Matcher nextSectionMatcher = SECTION_HEADING.matcher(line);
                if (nextSectionMatcher.matches()) {
                    String nextLevel = nextSectionMatcher.group(1);
                    // If next section is at same or higher level (fewer !'s means higher),
                    // stop skipping and process this line again
                    if (nextLevel.length() <= currentSectionLevel.length()) {
                        skipping = false;
                        currentSectionLevel = null;
                        // Re-process this line
                        i--;
                    }
                    // Otherwise, continue skipping (subsection of duplicate)
                }
                // Skip this line (part of duplicate section)
            } else {
                result.append(line).append("\n");
            }
        }

        // Preserve original trailing newline behavior
        String resultStr = result.toString();
        if (!content.endsWith("\n") && resultStr.endsWith("\n")) {
            resultStr = resultStr.substring(0, resultStr.length() - 1);
        }

        return resultStr;
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

    private static void checkThinkTags(String content, List<ValidationIssue> issues) {
        Matcher matcher = THINK_TAGS.matcher(content);
        int count = 0;
        String example = null;
        while (matcher.find()) {
            count++;
            if (example == null) {
                String match = matcher.group();
                example = match.length() > 50 ? match.substring(0, 50) + "..." : match;
            }
        }
        if (count > 0) {
            issues.add(new ValidationIssue("THINK_TAG",
                    "Chain-of-thought tags leaked from LLM response", example, count));
        }
    }
}
