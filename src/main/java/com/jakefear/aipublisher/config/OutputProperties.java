package com.jakefear.aipublisher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration properties for output file generation.
 */
@Component
@ConfigurationProperties(prefix = "output")
public class OutputProperties {

    /**
     * Directory where generated wiki files will be written.
     */
    private String directory = "./output";

    /**
     * File extension for generated files (.txt for JSPWiki).
     */
    private String fileExtension = ".txt";

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Path getDirectoryPath() {
        return Paths.get(directory);
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }
}
