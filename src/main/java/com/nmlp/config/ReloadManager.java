package com.nmlp.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Reloads all YAML configuration without server restart.
 */
public final class ReloadManager {

    private final JavaPlugin plugin;
    private final File dataFolder;
    private MainConfig mainConfig;
    private MessagesConfig messagesConfig;
    private GuiConfig guiConfig;
    private ItemsConfig itemsConfig;
    private BuffsConfig buffsConfig;
    private FileConfiguration rawConfig;
    private FileConfiguration rawMessages;
    private FileConfiguration rawGui;
    private FileConfiguration rawItems;
    private FileConfiguration rawBuffs;
    private final MessageService messageService;

    public ReloadManager(@NotNull JavaPlugin plugin, @NotNull MessageService messageService) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        this.messageService = messageService;
    }

    public void loadAll() {
        rawConfig = YamlFiles.loadMerged(dataFolder, "config.yml", plugin);
        rawMessages = YamlFiles.loadMerged(dataFolder, "messages.yml", plugin);
        rawGui = YamlFiles.loadMerged(dataFolder, "gui.yml", plugin);
        rawItems = YamlFiles.loadMerged(dataFolder, "items.yml", plugin);
        rawBuffs = YamlFiles.loadMerged(dataFolder, "buffs.yml", plugin);
        mainConfig = new MainConfig(rawConfig);
        messagesConfig = new MessagesConfig(rawMessages);
        guiConfig = new GuiConfig(rawGui);
        itemsConfig = new ItemsConfig(rawItems);
        buffsConfig = new BuffsConfig(rawBuffs);
        messageService.reload(messagesConfig);
    }

    public @NotNull MainConfig main() {
        return mainConfig;
    }

    public @NotNull MessagesConfig messagesRaw() {
        return messagesConfig;
    }

    public @NotNull GuiConfig gui() {
        return guiConfig;
    }

    public @NotNull ItemsConfig items() {
        return itemsConfig;
    }

    public @NotNull BuffsConfig buffs() {
        return buffsConfig;
    }

    public @NotNull MessageService messages() {
        return messageService;
    }

    public @NotNull FileConfiguration rawConfig() {
        return rawConfig;
    }
}
