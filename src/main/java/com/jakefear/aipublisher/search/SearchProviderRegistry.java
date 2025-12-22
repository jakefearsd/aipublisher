package com.jakefear.aipublisher.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for search providers.
 * Allows selecting between different search backends at runtime.
 */
@Component
public class SearchProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(SearchProviderRegistry.class);

    private final Map<String, SearchProvider> providers = new HashMap<>();
    private String defaultProviderName;

    /**
     * Default constructor for Spring.
     */
    public SearchProviderRegistry() {
        this.defaultProviderName = "wikidata";
    }

    /**
     * Constructor with configurable default provider.
     */
    public SearchProviderRegistry(@Value("${search.default-provider:wikidata}") String defaultProviderName) {
        this.defaultProviderName = defaultProviderName;
    }

    /**
     * Register all available search providers (called by Spring).
     */
    @Autowired(required = false)
    public void registerProviders(List<SearchProvider> providerList) {
        if (providerList == null || providerList.isEmpty()) {
            log.warn("No search providers registered");
            return;
        }

        for (SearchProvider provider : providerList) {
            register(provider);
        }

        log.info("Registered {} search provider(s): {}", providers.size(), providers.keySet());
    }

    /**
     * Register a single provider.
     */
    public void register(SearchProvider provider) {
        String name = provider.getProviderName();
        if (providers.containsKey(name)) {
            log.warn("Overwriting existing provider: {}", name);
        }
        providers.put(name, provider);
        log.debug("Registered search provider: {}", name);
    }

    /**
     * Get the default search provider.
     *
     * @return The default provider, or a no-op provider if none available
     */
    public SearchProvider getDefault() {
        SearchProvider provider = providers.get(defaultProviderName);
        if (provider == null) {
            log.warn("Default provider '{}' not found, trying first available", defaultProviderName);
            provider = providers.values().stream().findFirst().orElse(null);
        }
        if (provider == null) {
            log.warn("No search providers available, returning disabled provider");
            return new DisabledSearchProvider();
        }
        return provider;
    }

    /**
     * Get a specific provider by name.
     *
     * @param name Provider name
     * @return Optional containing the provider if found
     */
    public Optional<SearchProvider> get(String name) {
        return Optional.ofNullable(providers.get(name));
    }

    /**
     * Set the default provider name.
     */
    public void setDefault(String providerName) {
        if (!providers.containsKey(providerName)) {
            log.warn("Provider '{}' not registered, keeping current default '{}'",
                    providerName, defaultProviderName);
            return;
        }
        this.defaultProviderName = providerName;
        log.info("Default search provider set to: {}", providerName);
    }

    /**
     * Get names of all registered providers.
     */
    public List<String> getProviderNames() {
        return List.copyOf(providers.keySet());
    }

    /**
     * Check if any providers are registered and enabled.
     */
    public boolean hasEnabledProvider() {
        return providers.values().stream().anyMatch(SearchProvider::isEnabled);
    }

    /**
     * A no-op search provider used when no real providers are available.
     */
    private static class DisabledSearchProvider implements SearchProvider {
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
            return false;
        }

        @Override
        public String getProviderName() {
            return "disabled";
        }
    }
}
