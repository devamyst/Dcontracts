package me.devamy.contracts.config;

import me.devamy.contracts.utils.Log;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static me.devamy.contracts.Contracts.plugin;

/**
 * Merges new default keys from a bundled JAR resource into an existing config file.
 * Preserves all existing values and comments.
 */
public class ConfigMigrator {

    private static final Yaml YAML;

    static {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setWidth(200);
        YAML = new Yaml(options);
    }

    /**
     * Load the default YAML from the plugin JAR resource, load the existing file
     * from disk, deep-merge any missing keys into the existing file, and write the
     * result back.
     *
     * @param resourcePath path inside the JAR, e.g. {@code "default-config.yml"}
     * @param targetFile   the on-disk config file to update
     */
    @SuppressWarnings("unchecked")
    public static void migrate(String resourcePath, File targetFile) {
        if (!targetFile.exists()) {
            Log.info("Skipping migration for " + targetFile.getName() + " — file does not exist yet");
            return;
        }

        Map<String, Object> defaults;
        try (InputStream in = plugin.getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                Log.warn("Default resource '" + resourcePath + "' not found in JAR, skipping migration");
                return;
            }
            defaults = YAML.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.error("Failed to load default resource '" + resourcePath + "'", e);
            return;
        }

        if (defaults == null || defaults.isEmpty()) return;

        Map<String, Object> existing;
        try (Reader reader = Files.newBufferedReader(targetFile.toPath(), StandardCharsets.UTF_8)) {
            existing = YAML.load(reader);
        } catch (Exception e) {
            Log.error("Failed to read existing " + targetFile.getName(), e);
            return;
        }

        if (existing == null) existing = new LinkedHashMap<>();

        List<String> addedKeys = new java.util.ArrayList<>();
        deepMerge(defaults, existing, "", addedKeys);

        if (addedKeys.isEmpty()) {
            Log.info("No new keys to add to " + targetFile.getName());
            return;
        }

        try (Writer writer = Files.newBufferedWriter(targetFile.toPath(), StandardCharsets.UTF_8)) {
            YAML.dump(existing, writer);
        } catch (Exception e) {
            Log.error("Failed to write merged " + targetFile.getName(), e);
            return;
        }

        Log.info("Merged " + addedKeys.size() + " new key(s) into " + targetFile.getName() + ":");
        for (String key : addedKeys) {
            Log.info("  + " + key);
        }
    }

    @SuppressWarnings("unchecked")
    private static void deepMerge(Map<String, Object> source, Map<String, Object> target,
                                  String prefix, List<String> addedKeys) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            String fullPath = prefix.isEmpty() ? key : prefix + "." + key;
            Object defaultValue = entry.getValue();
            Object existingValue = target.get(key);

            if (existingValue == null) {
                target.put(key, deepCopy(defaultValue));
                addedKeys.add(fullPath);
            } else if (defaultValue instanceof Map && existingValue instanceof Map) {
                deepMerge(
                        (Map<String, Object>) defaultValue,
                        (Map<String, Object>) existingValue,
                        fullPath, addedKeys
                );
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Object deepCopy(Object value) {
        if (value instanceof Map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : ((Map<String, Object>) value).entrySet()) {
                copy.put(e.getKey(), deepCopy(e.getValue()));
            }
            return copy;
        }
        if (value instanceof List) {
            List<Object> copy = new java.util.ArrayList<>();
            for (Object item : (List<Object>) value) {
                copy.add(deepCopy(item));
            }
            return copy;
        }
        return value;
    }
}
