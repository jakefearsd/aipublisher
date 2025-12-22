package com.jakefear.aipublisher.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SearchProviderRegistry")
class SearchProviderRegistryTest {

    private SearchProviderRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SearchProviderRegistry();
    }

    @Nested
    @DisplayName("Provider Registration")
    class ProviderRegistration {

        @Test
        @DisplayName("Registers a single provider")
        void registersSingleProvider() {
            MockSearchProvider provider = new MockSearchProvider("test");
            registry.register(provider);

            assertTrue(registry.get("test").isPresent());
            assertEquals(provider, registry.get("test").get());
        }

        @Test
        @DisplayName("Registers multiple providers")
        void registersMultipleProviders() {
            MockSearchProvider provider1 = new MockSearchProvider("provider1");
            MockSearchProvider provider2 = new MockSearchProvider("provider2");

            registry.registerProviders(List.of(provider1, provider2));

            assertEquals(2, registry.getProviderNames().size());
            assertTrue(registry.get("provider1").isPresent());
            assertTrue(registry.get("provider2").isPresent());
        }

        @Test
        @DisplayName("Overwrites existing provider with same name")
        void overwritesExistingProvider() {
            MockSearchProvider original = new MockSearchProvider("test");
            MockSearchProvider replacement = new MockSearchProvider("test");

            registry.register(original);
            registry.register(replacement);

            assertEquals(replacement, registry.get("test").get());
        }

        @Test
        @DisplayName("Handles null provider list gracefully")
        void handlesNullProviderList() {
            assertDoesNotThrow(() -> registry.registerProviders(null));
            assertTrue(registry.getProviderNames().isEmpty());
        }

        @Test
        @DisplayName("Handles empty provider list gracefully")
        void handlesEmptyProviderList() {
            assertDoesNotThrow(() -> registry.registerProviders(List.of()));
            assertTrue(registry.getProviderNames().isEmpty());
        }
    }

    @Nested
    @DisplayName("Default Provider")
    class DefaultProvider {

        @Test
        @DisplayName("Returns default provider when registered")
        void returnsDefaultProviderWhenRegistered() {
            MockSearchProvider wikipedia = new MockSearchProvider("wikipedia");
            registry.register(wikipedia);

            SearchProvider defaultProvider = registry.getDefault();
            assertEquals("wikipedia", defaultProvider.getProviderName());
        }

        @Test
        @DisplayName("Returns first available when default not found")
        void returnsFirstAvailableWhenDefaultNotFound() {
            MockSearchProvider other = new MockSearchProvider("other");
            registry.register(other);

            SearchProvider provider = registry.getDefault();
            assertEquals("other", provider.getProviderName());
        }

        @Test
        @DisplayName("Returns disabled provider when none registered")
        void returnsDisabledProviderWhenNoneRegistered() {
            SearchProvider provider = registry.getDefault();

            assertNotNull(provider);
            assertFalse(provider.isEnabled());
            assertEquals("disabled", provider.getProviderName());
        }

        @Test
        @DisplayName("Can change default provider")
        void canChangeDefaultProvider() {
            MockSearchProvider wikipedia = new MockSearchProvider("wikipedia");
            MockSearchProvider google = new MockSearchProvider("google");
            registry.registerProviders(List.of(wikipedia, google));

            registry.setDefault("google");

            assertEquals("google", registry.getDefault().getProviderName());
        }

        @Test
        @DisplayName("Ignores invalid default provider name")
        void ignoresInvalidDefaultProviderName() {
            MockSearchProvider wikipedia = new MockSearchProvider("wikipedia");
            registry.register(wikipedia);

            registry.setDefault("nonexistent");

            // Should still be wikipedia
            assertEquals("wikipedia", registry.getDefault().getProviderName());
        }
    }

    @Nested
    @DisplayName("Provider Lookup")
    class ProviderLookup {

        @Test
        @DisplayName("Returns empty optional for unknown provider")
        void returnsEmptyForUnknownProvider() {
            assertTrue(registry.get("unknown").isEmpty());
        }

        @Test
        @DisplayName("Lists all provider names")
        void listsAllProviderNames() {
            registry.registerProviders(List.of(
                    new MockSearchProvider("alpha"),
                    new MockSearchProvider("beta"),
                    new MockSearchProvider("gamma")
            ));

            List<String> names = registry.getProviderNames();
            assertEquals(3, names.size());
            assertTrue(names.contains("alpha"));
            assertTrue(names.contains("beta"));
            assertTrue(names.contains("gamma"));
        }
    }

    @Nested
    @DisplayName("Enabled Check")
    class EnabledCheck {

        @Test
        @DisplayName("Reports false when no providers registered")
        void reportsFalseWhenNoProviders() {
            assertFalse(registry.hasEnabledProvider());
        }

        @Test
        @DisplayName("Reports true when enabled provider exists")
        void reportsTrueWhenEnabledProviderExists() {
            MockSearchProvider enabled = new MockSearchProvider("test", true);
            registry.register(enabled);

            assertTrue(registry.hasEnabledProvider());
        }

        @Test
        @DisplayName("Reports false when all providers disabled")
        void reportsFalseWhenAllDisabled() {
            MockSearchProvider disabled = new MockSearchProvider("test", false);
            registry.register(disabled);

            assertFalse(registry.hasEnabledProvider());
        }
    }

    /**
     * Mock search provider for testing.
     */
    private static class MockSearchProvider implements SearchProvider {
        private final String name;
        private final boolean enabled;

        MockSearchProvider(String name) {
            this(name, true);
        }

        MockSearchProvider(String name, boolean enabled) {
            this.name = name;
            this.enabled = enabled;
        }

        @Override
        public List<SearchResult> search(String query) {
            return List.of();
        }

        @Override
        public List<String> getRelatedTopics(String topic) {
            return List.of();
        }

        @Override
        public SearchResult getTopicSummary(String topic) {
            return null;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public String getProviderName() {
            return name;
        }
    }
}
