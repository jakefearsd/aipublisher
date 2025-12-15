package com.jakefear.aipublisher.gap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jakefear.aipublisher.config.OutputProperties;
import com.jakefear.aipublisher.util.PageNameUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
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

    private final OutputProperties outputProperties;
    private final ChatLanguageModel categorizationModel;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public GapDetectionService(
            OutputProperties outputProperties,
            @Qualifier("researchChatModel") ChatLanguageModel categorizationModel) {
        this.outputProperties = outputProperties;
        this.categorizationModel = categorizationModel;
        this.objectMapper = new ObjectMapper();
    }

    // Constructor for testing
    public GapDetectionService(OutputProperties outputProperties, ChatLanguageModel categorizationModel, ObjectMapper objectMapper) {
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

            links.add(linkTarget);
        }

        return links;
    }

    /**
     * Find gaps by comparing links to existing pages.
     */
    List<GapConcept> findGaps(Map<String, Set<String>> linkToSources, Set<String> existingPages) {
        List<GapConcept> gaps = new ArrayList<>();

        // Create a normalized lookup for existing pages
        Map<String, String> normalizedToActual = new HashMap<>();
        for (String page : existingPages) {
            normalizedToActual.put(normalizeName(page), page);
        }

        for (Map.Entry<String, Set<String>> entry : linkToSources.entrySet()) {
            String linkTarget = entry.getKey();
            Set<String> sources = entry.getValue();

            String normalized = normalizeName(linkTarget);

            // Check if page exists (with normalized comparison)
            if (normalizedToActual.containsKey(normalized)) {
                // Page exists - check if it's an alias that could use a redirect
                String actualPage = normalizedToActual.get(normalized);
                if (!linkTarget.equals(actualPage) && !PageNameUtils.toCamelCaseOrDefault(linkTarget, "").equals(actualPage)) {
                    // This is an alias - might need a redirect
                    gaps.add(GapConcept.builder(linkTarget)
                            .type(GapType.REDIRECT)
                            .redirectTarget(actualPage)
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
     * Categorize gaps using LLM.
     */
    List<GapConcept> categorizeGaps(List<GapConcept> gaps, String universeName) {
        if (gaps.isEmpty()) {
            return gaps;
        }

        // Build categorization prompt
        String prompt = buildCategorizationPrompt(gaps, universeName);

        try {
            String response = categorizationModel.generate(prompt);
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
