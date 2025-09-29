package com.zetaplugins.netwatchz.velocity.util;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class to initialize the default configuration file for the plugin.
 */
public class ConfigInitializer {

    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public ConfigInitializer(Logger logger, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    /**
     * Initializes the default configuration file if it does not already exist.
     */
    public void initializeDefaultConfig() {
        Path configFile = dataDirectory.resolve("config.yml");

        if (Files.notExists(configFile)) {
            try {
                Files.createDirectories(dataDirectory);

                // Copy default config from JAR resources
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (in == null) {
                        logger.error("Default config.yml not found in plugin resources!");
                        return;
                    }
                    Files.copy(in, configFile);
                    logger.info("Default config.yml created in {}", configFile.toAbsolutePath());
                }
            } catch (IOException e) {
                logger.error("Failed to create default config.yml", e);
            }
        } else {
            logger.info("Config file already exists: {}", configFile.toAbsolutePath());
        }
    }
}