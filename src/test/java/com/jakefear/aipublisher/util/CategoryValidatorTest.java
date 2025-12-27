package com.jakefear.aipublisher.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CategoryValidator")
class CategoryValidatorTest {

    @Nested
    @DisplayName("filterConflicting()")
    class FilterConflicting {

        @Test
        @DisplayName("Returns original list when no conflicts")
        void returnsOriginalWhenNoConflicts() {
            List<String> categories = List.of("Finance", "Economics");
            List<String> result = CategoryValidator.filterConflicting(categories, "Investing");

            assertEquals(2, result.size());
            assertTrue(result.contains("Finance"));
            assertTrue(result.contains("Economics"));
        }

        @Test
        @DisplayName("Returns empty list for null input")
        void returnsEmptyForNull() {
            List<String> result = CategoryValidator.filterConflicting(null, "Investing");
            assertNull(result);
        }

        @Test
        @DisplayName("Returns empty list for empty input")
        void returnsEmptyForEmptyInput() {
            List<String> result = CategoryValidator.filterConflicting(List.of(), "Investing");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Removes HomeAutomation from Investing domain")
        void removesHomeAutomationFromInvesting() {
            List<String> categories = List.of("Finance", "HomeAutomation", "Economics");
            List<String> result = CategoryValidator.filterConflicting(categories, "InvestingBasics");

            assertFalse(result.contains("HomeAutomation"));
            assertTrue(result.contains("Finance"));
            assertTrue(result.contains("Economics"));
        }

        @Test
        @DisplayName("Removes SmartHome from Finance domain")
        void removesSmartHomeFromFinance() {
            List<String> categories = List.of("InvestingBasics", "SmartHome", "VoiceAssistants");
            List<String> result = CategoryValidator.filterConflicting(categories, "Finance");

            assertFalse(result.contains("SmartHome"));
            assertFalse(result.contains("VoiceAssistants"));
            assertTrue(result.contains("InvestingBasics"));
        }

        @Test
        @DisplayName("Keeps generic categories in any domain")
        void keepsGenericCategories() {
            List<String> categories = List.of("Finance", "Definitions", "Glossary");
            List<String> result = CategoryValidator.filterConflicting(categories, "Investing");

            assertTrue(result.contains("Definitions"));
            assertTrue(result.contains("Glossary"));
        }

        @Test
        @DisplayName("Removes Investing from HomeAutomation domain")
        void removesInvestingFromHomeAutomation() {
            List<String> categories = List.of("SmartHome", "InvestingBasics", "IoT");
            List<String> result = CategoryValidator.filterConflicting(categories, "HomeAutomation");

            assertFalse(result.contains("InvestingBasics"));
            assertTrue(result.contains("SmartHome"));
            assertTrue(result.contains("IoT"));
        }

        @Test
        @DisplayName("Returns original list when domain not recognized")
        void returnsOriginalWhenDomainUnknown() {
            List<String> categories = List.of("Finance", "HomeAutomation");
            List<String> result = CategoryValidator.filterConflicting(categories, "UnknownDomain");

            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("areConflicting()")
    class AreConflicting {

        @Test
        @DisplayName("Returns true for HomeAutomation and Finance")
        void returnsTrueForHomeAutomationAndFinance() {
            assertTrue(CategoryValidator.areConflicting("HomeAutomation", "Finance"));
            assertTrue(CategoryValidator.areConflicting("SmartHome", "InvestingBasics"));
        }

        @Test
        @DisplayName("Returns false for categories in same group")
        void returnsFalseForSameGroup() {
            assertFalse(CategoryValidator.areConflicting("Finance", "Economics"));
            assertFalse(CategoryValidator.areConflicting("HomeAutomation", "SmartHome"));
        }

        @Test
        @DisplayName("Returns false for generic categories")
        void returnsFalseForGenericCategories() {
            assertFalse(CategoryValidator.areConflicting("Finance", "Definitions"));
            assertFalse(CategoryValidator.areConflicting("HomeAutomation", "Glossary"));
        }
    }
}
