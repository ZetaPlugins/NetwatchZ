package com.zetaplugins.netwatchz.velocity.util;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

public class VelocityLocalizationService {

    private final Path dataDirectory;
    private final Logger logger;
    private final List<String> defaultLangs;
    private final String langToUse;
    private final String langFolder;

    private Map<String, Object> langConfig;

    @Inject
    public VelocityLocalizationService(@DataDirectory Path dataDirectory, Logger logger, List<String> defaultLangs, String langToUse) {
        this(dataDirectory, logger, defaultLangs, langToUse, "lang/");
    }

    public VelocityLocalizationService(Path dataDirectory, Logger logger, List<String> defaultLangs, String langToUse, String langFolder) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.defaultLangs = defaultLangs;
        this.langToUse = langToUse;
        this.langFolder = langFolder;
        loadLanguageConfig();
    }

    public void reload() {
        loadLanguageConfig();
    }

    private void loadLanguageConfig() {
        try {
            Path langDir = dataDirectory.resolve(langFolder);
            if (!Files.exists(langDir)) {
                Files.createDirectories(langDir);
            }

            for (String lang : defaultLangs) {
                Path langFile = langDir.resolve(lang + ".yml");
                if (!Files.exists(langFile)) {
                    try (InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(langFolder + lang + ".yml")) {
                        if (resourceStream != null) {
                            Files.copy(resourceStream, langFile, StandardCopyOption.REPLACE_EXISTING);
                            logger.info("Saved default language file: {}", langFile);
                        } else {
                            logger.warn("Default language resource {}.yml not found in JAR", lang);
                        }
                    }
                }
            }

            Path selectedLangFile = langDir.resolve(langToUse + ".yml");
            if (!Files.exists(selectedLangFile)) {
                logger.error("Selected language {} not found. Messages will not be localized.", langToUse);
                return;
            }

            logger.info("Using language file: {}", selectedLangFile);

            try (InputStream is = Files.newInputStream(selectedLangFile)) {
                Yaml yaml = new Yaml();
                langConfig = yaml.load(is);
            }

        } catch (IOException e) {
            logger.error("Failed to load language configuration", e);
        }
    }

    public String getString(String key) {
        return langConfig.getOrDefault(key, key).toString();
    }

    public String getString(String key, String fallback) {
        Object value = langConfig.get(key);
        return value != null ? value.toString() : fallback;
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String key) {
        Object value = langConfig.get(key);
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}