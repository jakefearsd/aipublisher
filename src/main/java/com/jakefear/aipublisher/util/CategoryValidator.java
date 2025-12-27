package com.jakefear.aipublisher.util;

import java.util.List;
import java.util.Set;

/**
 * Validates and filters categories to detect conflicting assignments.
 * For example, a Finance article should not have "HomeAutomation" as a category.
 */
public class CategoryValidator {

    // Define incompatible category groups
    private static final List<Set<String>> INCOMPATIBLE_GROUPS = List.of(
            Set.of("HomeAutomation", "SmartHome", "IoT", "VoiceAssistants"),
            Set.of("Investing", "InvestingBasics", "Finance", "Economics", "PersonalFinance"),
            Set.of("PortugueseHistory", "PoliticalHistory", "ColonialHistory"),
            Set.of("GermanHistory", "BerlinHistory", "ColdWar", "WorldWarII"),
            Set.of("Technology", "Programming", "Software")
    );

    // Generic categories that can apply to any domain
    private static final Set<String> GENERIC_CATEGORIES = Set.of(
            "Definitions", "Glossary", "Concepts", "References",
            "History", "Overview", "Introduction"
    );

    /**
     * Validate that categories don't conflict based on the article's domain.
     *
     * @param categories The category list
     * @param expectedDomain The expected domain (from universe name)
     * @return Filtered list with conflicting categories removed
     */
    public static List<String> filterConflicting(List<String> categories, String expectedDomain) {
        if (categories == null || categories.isEmpty()) {
            return categories;
        }

        // Determine which category group is expected
        Set<String> expectedGroup = findMatchingGroup(expectedDomain);

        if (expectedGroup.isEmpty()) {
            // No known domain, keep original
            return categories;
        }

        // Keep only categories in expected group or generic ones
        return categories.stream()
                .filter(cat -> expectedGroup.contains(cat) || isGenericCategory(cat) || !isInAnyGroup(cat))
                .toList();
    }

    /**
     * Check if two categories are in conflict (from different domain groups).
     */
    public static boolean areConflicting(String cat1, String cat2) {
        if (isGenericCategory(cat1) || isGenericCategory(cat2)) {
            return false;
        }

        Set<String> group1 = findGroupContaining(cat1);
        Set<String> group2 = findGroupContaining(cat2);

        // Conflict if they're in different non-empty groups
        if (!group1.isEmpty() && !group2.isEmpty() && !group1.equals(group2)) {
            return true;
        }

        return false;
    }

    private static Set<String> findMatchingGroup(String domain) {
        if (domain == null) return Set.of();

        String lowerDomain = domain.toLowerCase();

        for (Set<String> group : INCOMPATIBLE_GROUPS) {
            for (String cat : group) {
                if (lowerDomain.contains(cat.toLowerCase())) {
                    return group;
                }
            }
        }

        return Set.of();
    }

    private static Set<String> findGroupContaining(String category) {
        if (category == null) return Set.of();

        for (Set<String> group : INCOMPATIBLE_GROUPS) {
            if (group.contains(category)) {
                return group;
            }
        }

        return Set.of();
    }

    private static boolean isInAnyGroup(String category) {
        return !findGroupContaining(category).isEmpty();
    }

    private static boolean isGenericCategory(String category) {
        return GENERIC_CATEGORIES.contains(category);
    }
}
