package org.catrobat.catroid.plugins;

import java.io.File;

public class PluginInfo {
    public final String packageName;
    public final String name;
    public final String version;
    public final String description;
    public final File pluginDirectory;
    public boolean isEnabled;
    public final File settingsFile;

    public PluginInfo(String packageName, String name, String version, String description, File pluginDirectory) {
        this.packageName = packageName;
        this.name = name;
        this.version = version;
        this.description = description;
        this.pluginDirectory = pluginDirectory;
        this.isEnabled = true;
        this.settingsFile = new File(pluginDirectory, "settings.json");
    }

    public boolean hasSettings() {
        return settingsFile.exists() && settingsFile.canRead();
    }
}