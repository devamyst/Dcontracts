package me.karven.orderium.config.util;

import io.github.thatsmusic99.configurationmaster.api.ConfigFile;
import org.jetbrains.annotations.NotNull;

public interface IConfigFile {

    void reload();

    void save();

    void setDefault();

    void migrateV5(final @NotNull ConfigFile oldConfig);

    void applyDefaultValues();
}
