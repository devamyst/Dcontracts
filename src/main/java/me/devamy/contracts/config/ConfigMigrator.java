package me.devamy.contracts.config;

import me.devamy.contracts.utils.Log;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static me.devamy.contracts.Contracts.plugin;

// Compares the bundled default resource against the user's config file
// and reports which keys are missing. The caller handles adding them.
public class ConfigMigrator {

    private static final Yaml YAML = new Yaml();

    /**
     * Check which keys from the default resource are missing in the target file.
     *
     * @param resourcePath path inside the JAR, e.g. {@code "default-config.yml"}
     * @param targetFile   the on-disk config file to check
     * @return set of dotted key paths that are missing (never {@code null})
     */
    public static Set<String> findMissingKeys(String resourcePath, File targetFile) {
        Set<String> missing = new LinkedHashSet<>();

        if (!targetFile.exists()) {
            return missing;
        }

        Map<String, Object> defaultMap;
        try (InputStream in = plugin.getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                Log.warn("Default resource '" + resourcePath + "' not found in JAR");
                return missing;
            }
            defaultMap = YAML.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.error("Failed to load default resource '" + resourcePath + "'", e);
            return missing;
        }

        if (defaultMap == null || defaultMap.isEmpty()) return missing;

        Map<String, Object> existingMap;
        try (Reader reader = new InputStreamReader(new FileInputStream(targetFile), StandardCharsets.UTF_8)) {
            existingMap = YAML.load(reader);
        } catch (Exception e) {
            Log.error("Failed to read existing " + targetFile.getName(), e);
            return missing;
        }

        if (existingMap == null) existingMap = new LinkedHashMap<>();

        List<String> addedKeys = new ArrayList<>();
        deepFindMissing(defaultMap, existingMap, "", addedKeys);
        missing.addAll(addedKeys);

        return missing;
    }

    // Legacy stub — just logs missing keys without writing anything.
    // Use ConfigFile.addDefault() + save() instead.
    @Deprecated
    public static void migrate(String resourcePath, File targetFile) {
        Set<String> missing = findMissingKeys(resourcePath, targetFile);
        if (missing.isEmpty()) {
            Log.info("No new keys to add to " + targetFile.getName());
            return;
        }
        Log.info("Found " + missing.size() + " missing key(s) in " + targetFile.getName() + ":");
        for (String key : missing) {
            Log.info("  + " + key);
        }
        Log.info("Use /contracts admin reload to apply missing defaults");
    }

    @SuppressWarnings("unchecked")
    private static void deepFindMissing(Map<String, Object> source, Map<String, Object> target,
                                        String prefix, List<String> addedKeys) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            String fullPath = prefix.isEmpty() ? key : prefix + "." + key;
            Object defaultValue = entry.getValue();
            Object existingValue = target.get(key);

            if (existingValue == null) {
                addedKeys.add(fullPath);
            } else if (defaultValue instanceof Map<?, ?> && existingValue instanceof Map<?, ?>) {
                deepFindMissing(
                        (Map<String, Object>) defaultValue,
                        (Map<String, Object>) existingValue,
                        fullPath, addedKeys
                );
            }
        }
    }
}
