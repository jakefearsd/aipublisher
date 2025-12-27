package com.jakefear.aipublisher.gap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jakefear.aipublisher.config.OutputProperties;
import com.jakefear.aipublisher.util.PageNameUtils;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for detecting gap concepts in generated wiki content.
 * Scans articles for internal links and identifies pages that don't exist.
 */
@Service
public class GapDetectionService {

    private static final Logger log = LoggerFactory.getLogger(GapDetectionService.class);

    // Pattern to match JSPWiki internal links: [PageName] or [Display Text|PageName]
    // Excludes external links (http://, https://) and category/metadata syntax
    private static final Pattern LINK_PATTERN = Pattern.compile(
            "\\[(?:([^|\\]]+)\\|)?([^\\]]+)\\]"
    );

    // Pattern to detect external URLs
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?://|mailto:|ftp://)"
    );

    // Pattern to detect JSPWiki metadata/directives
    private static final Pattern DIRECTIVE_PATTERN = Pattern.compile(
            "^\\{(SET|INSERT|ALLOW|Image|TableOfContents)"
    );

    // Patterns for invalid page names
    private static final Pattern NUMERIC_ONLY = Pattern.compile("^\\d+$");
    private static final Pattern TOO_SHORT = Pattern.compile("^.{1,2}$");

    // Common words that shouldn't be separate pages
    private static final Set<String> INVALID_PAGE_NAMES = Set.of(
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "0",
            "a", "an", "the", "and", "or", "but", "is", "are", "was", "were"
    );

    private final OutputProperties outputProperties;
    private final ChatModel categorizationModel;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public GapDetectionService(
            OutputProperties outputProperties,
            @Qualifier("researchChatModel") ChatModel categorizationModel) {
        this.outputProperties = outputProperties;
        this.categorizationModel = categorizationModel;
        this.objectMapper = new ObjectMapper();
    }

    // Constructor for testing
    public GapDetectionService(OutputProperties outputProperties, ChatModel categorizationModel, ObjectMapper objectMapper) {
        this.outputProperties = outputProperties;
        this.categorizationModel = categorizationModel;
        this.objectMapper = objectMapper;
    }

    /**
     * Detect all gap concepts in the output directory.
     *
     * @return List of gap concepts that need attention
     */
    public List<GapConcept> detectGaps() throws IOException {
        Path outputDir = outputProperties.getDirectoryPath();
        String extension = outputProperties.getFileExtension();

        // Get existing pages
        Set<String> existingPages = getExistingPages(outputDir, extension);
        log.info("Found {} existing pages", existingPages.size());

        // Extract all internal links with their source pages
        Map<String, Set<String>> linkToSources = extractAllLinks(outputDir, extension);
        log.info("Found {} unique internal links", linkToSources.size());

        // Find gaps (links that don't have corresponding pages)
        List<GapConcept> gaps = findGaps(linkToSources, existingPages);
        log.info("Detected {} gap concepts", gaps.size());

        return gaps;
    }

    /**
     * Detect gaps and categorize them using LLM.
     *
     * @param universeName The name of the universe (for context)
     * @return List of categorized gap concepts
     */
    public List<GapConcept> detectAndCategorizeGaps(String universeName) throws IOException {
        List<GapConcept> gaps = detectGaps();

        if (gaps.isEmpty()) {
            return gaps;
        }

        // Categorize gaps using LLM
        return categorizeGaps(gaps, universeName);
    }

    /**
     * Get set of existing page names (normalized).
     */
    Set<String> getExistingPages(Path outputDir, String extension) throws IOException {
        if (!Files.exists(outputDir)) {
            return Set.of();
        }

        try (Stream<Path> files = Files.list(outputDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(extension))
                    .map(name -> name.substring(0, name.length() - extension.length()))
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Extract all internal links from all files in the output directory.
     *
     * @return Map of link target -> set of source pages that reference it
     */
    Map<String, Set<String>> extractAllLinks(Path outputDir, String extension) throws IOException {
        Map<String, Set<String>> linkToSources = new HashMap<>();

        if (!Files.exists(outputDir)) {
            return linkToSources;
        }

        try (Stream<Path> files = Files.list(outputDir)) {
            List<Path> wikiFiles = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(extension))
                    .toList();

            for (Path file : wikiFiles) {
                String sourcePage = file.getFileName().toString();
                sourcePage = sourcePage.substring(0, sourcePage.length() - extension.length());

                Set<String> links = extractLinksFromFile(file);
                for (String link : links) {
                    linkToSources.computeIfAbsent(link, k -> new HashSet<>()).add(sourcePage);
                }
            }
        }

        return linkToSources;
    }

    /**
     * Extract internal link targets from a single file.
     */
    Set<String> extractLinksFromFile(Path file) throws IOException {
        String content = Files.readString(file);
        return extractLinksFromContent(content);
    }

    /**
     * Extract internal link targets from content string.
     */
    Set<String> extractLinksFromContent(String content) {
        Set<String> links = new HashSet<>();
        Matcher matcher = LINK_PATTERN.matcher(content);

        while (matcher.find()) {
            // Group 1 is display text (may be null), Group 2 is the link target
            String linkTarget = matcher.group(2);

            if (linkTarget == null || linkTarget.isBlank()) {
                continue;
            }

            linkTarget = linkTarget.trim();

            // Skip external URLs
            if (URL_PATTERN.matcher(linkTarget).find()) {
                continue;
            }

            // Skip JSPWiki directives/metadata
            if (DIRECTIVE_PATTERN.matcher(linkTarget).find()) {
                continue;
            }

            // Skip category syntax
            if (linkTarget.startsWith("Category:") || linkTarget.startsWith("Wikipedia:")) {
                continue;
            }

            // Skip invalid page names (numeric only, too short, common words)
            if (shouldSkipPageName(linkTarget)) {
                continue;
            }

            links.add(linkTarget);
        }

        return links;
    }

    /**
     * Find gaps by comparing links to existing pages.
     */
    List<GapConcept> findGaps(Map<String, Set<String>> linkToSources, Set<String> existingPages) {
        List<GapConcept> gaps = new ArrayList<>();
        Set<String> processedNormalized = new HashSet<>();

        for (Map.Entry<String, Set<String>> entry : linkToSources.entrySet()) {
            String linkTarget = entry.getKey();
            Set<String> sources = entry.getValue();

            // Check for duplicate within this batch
            String normalized = normalizeName(linkTarget);
            if (processedNormalized.contains(normalized)) {
                continue; // Skip duplicate
            }
            processedNormalized.add(normalized);

            // Check for fuzzy match against existing pages
            String canonicalPage = findCanonicalPage(linkTarget, existingPages);
            if (canonicalPage != null) {
                // Page exists (or close variant) - create redirect if name differs
                if (!linkTarget.equals(canonicalPage) && !PageNameUtils.toCamelCaseOrDefault(linkTarget, "").equals(canonicalPage)) {
                    gaps.add(GapConcept.builder(linkTarget)
                            .type(GapType.REDIRECT)
                            .redirectTarget(canonicalPage)
                            .referencedBy(new ArrayList<>(sources))
                            .build());
                }
                continue;
            }

            // Page doesn't exist - this is a gap
            gaps.add(GapConcept.builder(linkTarget)
                    .type(GapType.DEFINITION) // Default, will be categorized later
                    .referencedBy(new ArrayList<>(sources))
                    .build());
        }

        return gaps;
    }

    /**
     * Normalize a page name for comparison.
     */
    String normalizeName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    /**
     * Check if a page name should be skipped (too generic or invalid).
     */
    boolean shouldSkipPageName(String pageName) {
        if (pageName == null || pageName.isBlank()) return true;

        String lower = pageName.toLowerCase().trim();

        // Skip known invalid names
        if (INVALID_PAGE_NAMES.contains(lower)) return true;

        // Skip purely numeric names
        if (NUMERIC_ONLY.matcher(pageName.trim()).matches()) return true;

        // Skip very short names (likely initials or typos)
        if (TOO_SHORT.matcher(pageName.trim()).matches()) return true;

        return false;
    }

    /**
     * Check if a potential gap name is a duplicate of an existing page.
     * Returns the canonical page name if a match is found, null otherwise.
     */
    String findCanonicalPage(String gapName, Set<String> existingPages) {
        String normalized = normalizeName(gapName);

        for (String existing : existingPages) {
            // Exact normalized match
            if (normalized.equals(normalizeName(existing))) {
                return existing;
            }

            // Fuzzy match for common variations
            if (isFuzzyMatch(gapName, existing)) {
                return existing;
            }
        }

        return null;
    }

    /**
     * Check for fuzzy matches that should be treated as duplicates.
     */
    private boolean isFuzzyMatch(String name1, String name2) {
        String n1 = normalizeForFuzzy(name1);
        String n2 = normalizeForFuzzy(name2);

        // Direct match after normalization
        if (n1.equals(n2)) return true;

        // Check for number/word substitutions (401k vs Four01K)
        String digits1 = n1.replaceAll("[^0-9]", "");
        String digits2 = n2.replaceAll("[^0-9]", "");
        String letters1 = n1.replaceAll("[^a-z]", "");
        String letters2 = n2.replaceAll("[^a-z]", "");

        // Same digits and similar letters = likely duplicate
        if (digits1.equals(digits2) && levenshteinDistance(letters1, letters2) <= 2) {
            return true;
        }

        return false;
    }

    private String normalizeForFuzzy(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("[áàâãä]", "a")
                .replaceAll("[éèêë]", "e")
                .replaceAll("[íìîï]", "i")
                .replaceAll("[óòôõö]", "o")
                .replaceAll("[úùûü]", "u")
                .replaceAll("[ñ]", "n")
                .replaceAll("[^a-z0-9]", "");
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Categorize gaps using LLM.
     */
    List<GapConcept> categorizeGaps(List<GapConcept> gaps, String universeName) {
        if (gaps.isEmpty()) {
            return gaps;
        }

        // Build categorization prompt
        String prompt = buildCategorizationPrompt(gaps, universeName);

        try {
            String response = categorizationModel.chat(prompt);
            return parseCategorizationResponse(response, gaps);
        } catch (Exception e) {
            log.warn("Failed to categorize gaps with LLM, using defaults: {}", e.getMessage());
            // Return gaps with default categorization
            return gaps;
        }
    }

    /**
     * Build prompt for LLM to categorize gaps.
     */
    String buildCategorizationPrompt(List<GapConcept> gaps, String universeName) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are categorizing gap concepts for a wiki about \"").append(universeName).append("\".\n\n");
        prompt.append("For each concept below, determine the appropriate category:\n");
        prompt.append("- DEFINITION: Technical term that needs a brief 100-200 word definition page\n");
        prompt.append("- REDIRECT: Alias/synonym for another term (specify the target)\n");
        prompt.append("- FULL_ARTICLE: Significant concept deserving full coverage (flag for later)\n");
        prompt.append("- IGNORE: Too generic, common word that doesn't need its own page\n\n");

        prompt.append("GAP CONCEPTS TO CATEGORIZE:\n");
        for (GapConcept gap : gaps) {
            prompt.append("- \"").append(gap.name()).append("\"");
            if (!gap.referencedBy().isEmpty()) {
                prompt.append(" (referenced by: ").append(String.join(", ", gap.referencedBy())).append(")");
            }
            prompt.append("\n");
        }

        prompt.append("\nRespond with JSON array. Each object should have:\n");
        prompt.append("- \"name\": the concept name exactly as shown above\n");
        prompt.append("- \"type\": one of DEFINITION, REDIRECT, FULL_ARTICLE, IGNORE\n");
        prompt.append("- \"redirectTarget\": (only for REDIRECT) the page name to redirect to\n");
        prompt.append("- \"category\": suggested category like \"Finance\", \"Economics\", etc.\n\n");
        prompt.append("Example response:\n");
        prompt.append("[\n");
        prompt.append("  {\"name\": \"Present Value\", \"type\": \"DEFINITION\", \"category\": \"Finance\"},\n");
        prompt.append("  {\"name\": \"compound interest\", \"type\": \"REDIRECT\", \"redirectTarget\": \"CompoundInterest\", \"category\": \"\"},\n");
        prompt.append("  {\"name\": \"investment\", \"type\": \"IGNORE\", \"category\": \"\"}\n");
        prompt.append("]\n");

        return prompt.toString();
    }

    /**
     * Parse LLM response and update gap categorizations.
     */
    List<GapConcept> parseCategorizationResponse(String response, List<GapConcept> originalGaps) {
        // Clean response - extract JSON array
        String jsonContent = extractJsonArray(response);
        if (jsonContent == null) {
            log.warn("Could not extract JSON array from LLM response");
            return originalGaps;
        }

        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            if (!root.isArray()) {
                log.warn("LLM response is not a JSON array");
                return originalGaps;
            }

            // Build lookup map for original gaps
            Map<String, GapConcept> gapsByName = originalGaps.stream()
                    .collect(Collectors.toMap(
                            g -> g.name().toLowerCase(),
                            g -> g,
                            (a, b) -> a // Keep first if duplicates
                    ));

            List<GapConcept> categorized = new ArrayList<>();

            for (JsonNode node : root) {
                String name = getStringOrNull(node, "name");
                if (name == null) continue;

                GapConcept original = gapsByName.get(name.toLowerCase());
                if (original == null) {
                    // Try exact match
                    original = gapsByName.values().stream()
                            .filter(g -> g.name().equals(name))
                            .findFirst()
                            .orElse(null);
                }

                if (original == null) {
                    log.debug("Gap '{}' from LLM response not found in original gaps", name);
                    continue;
                }

                String typeStr = getStringOrNull(node, "type");
                GapType type = typeStr != null ? GapType.fromString(typeStr) : original.type();
                if (type == null) type = GapType.DEFINITION;

                String redirectTarget = getStringOrNull(node, "redirectTarget");
                String category = getStringOrNull(node, "category");

                categorized.add(new GapConcept(
                        original.name(),
                        original.pageName(),
                        type,
                        original.referencedBy(),
                        redirectTarget != null ? redirectTarget : original.redirectTarget(),
                        category != null ? category : original.category()
                ));

                gapsByName.remove(name.toLowerCase());
            }

            // Add any gaps that weren't in the LLM response
            categorized.addAll(gapsByName.values());

            return categorized;

        } catch (JsonProcessingException e) {
            log.warn("Failed to parse LLM categorization response: {}", e.getMessage());
            return originalGaps;
        }
    }

    /**
     * Extract JSON array from LLM response (handles markdown code blocks).
     */
    String extractJsonArray(String response) {
        if (response == null) return null;

        // Try to find JSON array directly
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');

        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        return null;
    }

    private String getStringOrNull(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        return fieldNode.asText();
    }
}
