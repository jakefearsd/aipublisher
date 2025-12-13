package com.jakefear.aipublisher.linking;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Context about existing wiki pages and their relationships.
 * Used to make intelligent linking decisions.
 */
public class WikiLinkContext {

    // Page name -> set of pages it links to
    private final Map<String, Set<String>> outboundLinks = new ConcurrentHashMap<>();

    // Page name -> set of pages that link to it
    private final Map<String, Set<String>> inboundLinks = new ConcurrentHashMap<>();

    // Page name -> topic/description
    private final Map<String, String> pageTopics = new ConcurrentHashMap<>();

    // All known pages
    private final Set<String> allPages = ConcurrentHashMap.newKeySet();

    /**
     * Register a page in the wiki context.
     */
    public void registerPage(String pageName) {
        if (pageName != null && !pageName.isBlank()) {
            allPages.add(pageName);
        }
    }

    /**
     * Register a page with its topic description.
     */
    public void registerPage(String pageName, String topic) {
        registerPage(pageName);
        if (topic != null && !topic.isBlank()) {
            pageTopics.put(pageName, topic);
        }
    }

    /**
     * Record a link from one page to another.
     */
    public void recordLink(String fromPage, String toPage) {
        if (fromPage == null || toPage == null) return;
        if (fromPage.isBlank() || toPage.isBlank()) return;

        registerPage(fromPage);
        registerPage(toPage);

        outboundLinks.computeIfAbsent(fromPage, k -> ConcurrentHashMap.newKeySet()).add(toPage);
        inboundLinks.computeIfAbsent(toPage, k -> ConcurrentHashMap.newKeySet()).add(fromPage);
    }

    /**
     * Check if a page exists in the wiki.
     */
    public boolean pageExists(String pageName) {
        return pageName != null && allPages.contains(pageName);
    }

    /**
     * Get all pages that link to the given page.
     */
    public Set<String> getInboundLinks(String pageName) {
        if (pageName == null) return Set.of();
        return Set.copyOf(inboundLinks.getOrDefault(pageName, Set.of()));
    }

    /**
     * Get all pages that the given page links to.
     */
    public Set<String> getOutboundLinks(String pageName) {
        if (pageName == null) return Set.of();
        return Set.copyOf(outboundLinks.getOrDefault(pageName, Set.of()));
    }

    /**
     * Get pages that would benefit from a link to this page
     * (i.e., pages that are related but don't currently link here).
     */
    public Set<String> getPagesMissingLinkTo(String pageName) {
        // Find pages that link to pages that this page also links to
        // but don't yet link to this page
        Set<String> relatedPages = new HashSet<>();

        Set<String> myOutbound = getOutboundLinks(pageName);
        for (String linkedPage : myOutbound) {
            Set<String> alsoPagesTo = getInboundLinks(linkedPage);
            for (String related : alsoPagesTo) {
                if (!related.equals(pageName) && !getOutboundLinks(related).contains(pageName)) {
                    relatedPages.add(related);
                }
            }
        }

        return relatedPages;
    }

    /**
     * Get the topic description for a page.
     */
    public Optional<String> getPageTopic(String pageName) {
        return Optional.ofNullable(pageTopics.get(pageName));
    }

    /**
     * Get all known pages.
     */
    public Set<String> getAllPages() {
        return Set.copyOf(allPages);
    }

    /**
     * Get the number of inbound links for a page (popularity indicator).
     */
    public int getInboundLinkCount(String pageName) {
        return inboundLinks.getOrDefault(pageName, Set.of()).size();
    }

    /**
     * Get the most linked-to pages.
     */
    public List<String> getMostLinkedPages(int limit) {
        return allPages.stream()
                .sorted((a, b) -> Integer.compare(
                        getInboundLinkCount(b),
                        getInboundLinkCount(a)))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get total page count.
     */
    public int getPageCount() {
        return allPages.size();
    }

    /**
     * Clear all context data.
     */
    public void clear() {
        outboundLinks.clear();
        inboundLinks.clear();
        pageTopics.clear();
        allPages.clear();
    }
}
