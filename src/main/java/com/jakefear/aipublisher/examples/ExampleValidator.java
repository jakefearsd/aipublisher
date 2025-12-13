package com.jakefear.aipublisher.examples;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates examples in article content.
 */
@Component
public class ExampleValidator {

    // Pattern to find code blocks in JSPWiki syntax: {{{code}}} or {{{ code }}}
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
            "\\{\\{\\{([^}]*(?:\\}(?!\\}\\})[^}]*)*)\\}\\}\\}",
            Pattern.DOTALL
    );

    // Pattern to find Markdown-style code blocks: ```language\ncode\n```
    private static final Pattern MARKDOWN_CODE_PATTERN = Pattern.compile(
            "```(\\w*)\\n([^`]*(?:`(?!``)[^`]*)*)\\n```",
            Pattern.DOTALL
    );

    /**
     * Validate that content meets the example plan requirements.
     *
     * @param content The article content
     * @param plan The example plan to validate against
     * @return Validation result with issues found
     */
    public ValidationResult validate(String content, ExamplePlan plan) {
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Count code blocks
        int codeBlockCount = countCodeBlocks(content);

        // Check minimum count
        if (!plan.meetsMinimum(codeBlockCount)) {
            issues.add(String.format(
                    "Insufficient examples: found %d, required minimum %d",
                    codeBlockCount, plan.minimumCount()
            ));
        }

        // Check for required examples (by concept mention)
        for (ExampleSpec spec : plan.getRequiredExamples()) {
            if (!contentMentionsConcept(content, spec.concept())) {
                warnings.add(String.format(
                        "Required example '%s' may be missing: concept '%s' not found in content",
                        spec.id(), spec.concept()
                ));
            }
        }

        // Check progressive examples are in order
        if (!plan.getProgressiveExamples().isEmpty()) {
            validateProgressiveExamples(content, plan, warnings);
        }

        // Check for anti-pattern examples if required
        if (plan.hasAntiPatterns()) {
            if (!hasAntiPatternIndicators(content)) {
                warnings.add("Anti-pattern example expected but no 'don't', 'avoid', or 'wrong' indicators found");
            }
        }

        // Validate code syntax for known languages
        List<CodeBlock> blocks = extractCodeBlocks(content);
        for (CodeBlock block : blocks) {
            List<String> syntaxIssues = validateSyntax(block);
            issues.addAll(syntaxIssues);
        }

        return new ValidationResult(
                issues.isEmpty(),
                issues,
                warnings,
                codeBlockCount
        );
    }

    /**
     * Count code blocks in content.
     */
    public int countCodeBlocks(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }

        int count = 0;

        Matcher jspwikiMatcher = CODE_BLOCK_PATTERN.matcher(content);
        while (jspwikiMatcher.find()) {
            count++;
        }

        Matcher markdownMatcher = MARKDOWN_CODE_PATTERN.matcher(content);
        while (markdownMatcher.find()) {
            count++;
        }

        return count;
    }

    /**
     * Extract all code blocks from content.
     */
    public List<CodeBlock> extractCodeBlocks(String content) {
        List<CodeBlock> blocks = new ArrayList<>();

        if (content == null || content.isBlank()) {
            return blocks;
        }

        // JSPWiki style
        Matcher jspwikiMatcher = CODE_BLOCK_PATTERN.matcher(content);
        while (jspwikiMatcher.find()) {
            String code = jspwikiMatcher.group(1).trim();
            blocks.add(new CodeBlock(code, null, jspwikiMatcher.start()));
        }

        // Markdown style
        Matcher markdownMatcher = MARKDOWN_CODE_PATTERN.matcher(content);
        while (markdownMatcher.find()) {
            String language = markdownMatcher.group(1);
            String code = markdownMatcher.group(2).trim();
            blocks.add(new CodeBlock(code, language.isBlank() ? null : language, markdownMatcher.start()));
        }

        // Sort by position
        blocks.sort(Comparator.comparingInt(CodeBlock::position));

        return blocks;
    }

    /**
     * Validate syntax for a code block.
     */
    private List<String> validateSyntax(CodeBlock block) {
        List<String> issues = new ArrayList<>();

        if (block.code().isBlank()) {
            issues.add("Empty code block found");
            return issues;
        }

        // Check for common syntax issues based on language
        if ("java".equals(block.language())) {
            issues.addAll(validateJavaSyntax(block.code()));
        } else if ("python".equals(block.language())) {
            issues.addAll(validatePythonSyntax(block.code()));
        } else if ("javascript".equals(block.language()) || "js".equals(block.language())) {
            issues.addAll(validateJavaScriptSyntax(block.code()));
        }

        return issues;
    }

    /**
     * Basic Java syntax validation.
     */
    private List<String> validateJavaSyntax(String code) {
        List<String> issues = new ArrayList<>();

        // Check for unbalanced braces
        int braceCount = 0;
        for (char c : code.toCharArray()) {
            if (c == '{') braceCount++;
            if (c == '}') braceCount--;
        }
        if (braceCount != 0) {
            issues.add("Java code has unbalanced braces");
        }

        // Check for unbalanced parentheses
        int parenCount = 0;
        for (char c : code.toCharArray()) {
            if (c == '(') parenCount++;
            if (c == ')') parenCount--;
        }
        if (parenCount != 0) {
            issues.add("Java code has unbalanced parentheses");
        }

        // Check for common incomplete statements
        if (code.contains("public class") && !code.contains("}")) {
            issues.add("Java class definition appears incomplete");
        }

        return issues;
    }

    /**
     * Basic Python syntax validation.
     */
    private List<String> validatePythonSyntax(String code) {
        List<String> issues = new ArrayList<>();

        // Check for unbalanced parentheses
        int parenCount = 0;
        for (char c : code.toCharArray()) {
            if (c == '(') parenCount++;
            if (c == ')') parenCount--;
        }
        if (parenCount != 0) {
            issues.add("Python code has unbalanced parentheses");
        }

        // Check for unbalanced brackets
        int bracketCount = 0;
        for (char c : code.toCharArray()) {
            if (c == '[') bracketCount++;
            if (c == ']') bracketCount--;
        }
        if (bracketCount != 0) {
            issues.add("Python code has unbalanced brackets");
        }

        return issues;
    }

    /**
     * Basic JavaScript syntax validation.
     */
    private List<String> validateJavaScriptSyntax(String code) {
        List<String> issues = new ArrayList<>();

        // Check for unbalanced braces
        int braceCount = 0;
        for (char c : code.toCharArray()) {
            if (c == '{') braceCount++;
            if (c == '}') braceCount--;
        }
        if (braceCount != 0) {
            issues.add("JavaScript code has unbalanced braces");
        }

        // Check for unbalanced parentheses
        int parenCount = 0;
        for (char c : code.toCharArray()) {
            if (c == '(') parenCount++;
            if (c == ')') parenCount--;
        }
        if (parenCount != 0) {
            issues.add("JavaScript code has unbalanced parentheses");
        }

        return issues;
    }

    /**
     * Check if content mentions a concept.
     */
    private boolean contentMentionsConcept(String content, String concept) {
        return content.toLowerCase().contains(concept.toLowerCase());
    }

    /**
     * Validate progressive examples appear in order.
     */
    private void validateProgressiveExamples(String content, ExamplePlan plan, List<String> warnings) {
        List<ExampleSpec> progressive = plan.getProgressiveExamples();
        if (progressive.size() <= 1) {
            return;
        }

        // For now, just check that we have enough code blocks for progressive examples
        int codeBlockCount = countCodeBlocks(content);
        if (codeBlockCount < progressive.size()) {
            warnings.add(String.format(
                    "Progressive examples expected %d steps, but only %d code blocks found",
                    progressive.size(), codeBlockCount
            ));
        }
    }

    /**
     * Check for anti-pattern indicators in content.
     */
    private boolean hasAntiPatternIndicators(String content) {
        String lower = content.toLowerCase();
        return lower.contains("don't") ||
                lower.contains("avoid") ||
                lower.contains("wrong") ||
                lower.contains("incorrect") ||
                lower.contains("anti-pattern") ||
                lower.contains("mistake") ||
                lower.contains("bad practice");
    }

    /**
     * A code block extracted from content.
     */
    public record CodeBlock(String code, String language, int position) {}

    /**
     * Result of example validation.
     */
    public record ValidationResult(
            boolean valid,
            List<String> issues,
            List<String> warnings,
            int exampleCount
    ) {
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }
}
