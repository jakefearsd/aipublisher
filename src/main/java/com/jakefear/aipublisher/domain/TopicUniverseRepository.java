package com.jakefear.aipublisher.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Repository for persisting and loading TopicUniverse instances.
 */
@Component
public class TopicUniverseRepository {
    private static final Logger log = LoggerFactory.getLogger(TopicUniverseRepository.class);

    private static final String FILE_EXTENSION = ".universe.json";

    private final ObjectMapper objectMapper;
    private Path storageDirectory;

    @Autowired
    public TopicUniverseRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.storageDirectory = Path.of(System.getProperty("user.home"), ".aipublisher", "universes");
    }

    /**
     * Constructor for testing without Spring.
     */
    public TopicUniverseRepository() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.findAndRegisterModules(); // Auto-register available modules
        this.storageDirectory = Path.of(System.getProperty("user.home"), ".aipublisher", "universes");
    }

    /**
     * Set the storage directory.
     */
    public void setStorageDirectory(Path directory) {
        this.storageDirectory = directory;
    }

    /**
     * Get the storage directory.
     */
    public Path getStorageDirectory() {
        return storageDirectory;
    }

    /**
     * Save a universe to the default storage location.
     */
    public Path save(TopicUniverse universe) throws IOException {
        ensureStorageDirectory();
        Path filePath = storageDirectory.resolve(universe.id() + FILE_EXTENSION);
        return saveToPath(universe, filePath);
    }

    /**
     * Save a universe to a specific path.
     */
    public Path saveToPath(TopicUniverse universe, Path filePath) throws IOException {
        String json = objectMapper.writeValueAsString(universe);
        Files.writeString(filePath, json);
        log.info("Saved universe '{}' to {}", universe.name(), filePath);
        return filePath;
    }

    /**
     * Load a universe by ID from the default storage location.
     */
    public Optional<TopicUniverse> load(String id) {
        Path filePath = storageDirectory.resolve(id + FILE_EXTENSION);
        return loadFromPath(filePath);
    }

    /**
     * Load a universe from a specific path.
     */
    public Optional<TopicUniverse> loadFromPath(Path filePath) {
        if (!Files.exists(filePath)) {
            log.debug("Universe file not found: {}", filePath);
            return Optional.empty();
        }

        try {
            String json = Files.readString(filePath);
            TopicUniverse universe = objectMapper.readValue(json, TopicUniverse.class);
            log.info("Loaded universe '{}' from {}", universe.name(), filePath);
            return Optional.of(universe);
        } catch (IOException e) {
            log.error("Failed to load universe from {}: {}", filePath, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Delete a universe by ID.
     */
    public boolean delete(String id) {
        Path filePath = storageDirectory.resolve(id + FILE_EXTENSION);
        try {
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete universe {}: {}", id, e.getMessage());
            return false;
        }
    }

    /**
     * List all saved universe IDs.
     */
    public java.util.List<String> listAll() {
        if (!Files.exists(storageDirectory)) {
            return java.util.List.of();
        }

        try (var stream = Files.list(storageDirectory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(FILE_EXTENSION))
                    .map(p -> p.getFileName().toString())
                    .map(name -> name.substring(0, name.length() - FILE_EXTENSION.length()))
                    .toList();
        } catch (IOException e) {
            log.error("Failed to list universes: {}", e.getMessage());
            return java.util.List.of();
        }
    }

    /**
     * Check if a universe exists.
     */
    public boolean exists(String id) {
        Path filePath = storageDirectory.resolve(id + FILE_EXTENSION);
        return Files.exists(filePath);
    }

    /**
     * Export universe to JSON string.
     */
    public String toJson(TopicUniverse universe) {
        try {
            return objectMapper.writeValueAsString(universe);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize universe", e);
        }
    }

    /**
     * Import universe from JSON string.
     */
    public TopicUniverse fromJson(String json) {
        try {
            return objectMapper.readValue(json, TopicUniverse.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize universe", e);
        }
    }

    private void ensureStorageDirectory() throws IOException {
        if (!Files.exists(storageDirectory)) {
            Files.createDirectories(storageDirectory);
            log.debug("Created storage directory: {}", storageDirectory);
        }
    }
}
