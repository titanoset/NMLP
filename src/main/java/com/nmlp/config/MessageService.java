package com.nmlp.config;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Resolves MiniMessage templates from {@link MessagesConfig}.
 */
public final class MessageService {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private MessagesConfig messages;
    private String prefixRaw;

    public void reload(@NotNull MessagesConfig messages) {
        this.messages = messages;
        this.prefixRaw = messages.raw("prefix", "");
    }

    public void send(@NotNull CommandSender to, @NotNull String path, @NotNull String def, TagResolver... extra) {
        to.sendMessage(deserialize(path, def, extra));
    }

    public void send(@NotNull CommandSender to, @NotNull String path, @NotNull String def, @NotNull Map<String, String> placeholders) {
        to.sendMessage(deserialize(path, def, mapResolvers(placeholders)));
    }

    /**
     * Resolves {@code risk.by_action.<actionId>.<kind>} then {@code risk.<kind>} then {@code ultimateDef}.
     */
    public void sendRiskKind(@NotNull CommandSender to, @NotNull String actionId, @NotNull String kind,
                             @NotNull String ultimateDef, @NotNull Map<String, String> placeholders) {
        String specificPath = "risk.by_action." + actionId + "." + kind;
        String genericPath = "risk." + kind;
        String def = messages.raw(specificPath, messages.raw(genericPath, ultimateDef));
        send(to, specificPath, def, placeholders);
    }

    public @NotNull net.kyori.adventure.text.Component deserialize(@NotNull String path, @NotNull String def, TagResolver... extra) {
        String raw = messages.raw(path, def);
        List<TagResolver> list = new ArrayList<>();
        list.add(Placeholder.component("prefix", MINI.deserialize(prefixRaw)));
        list.addAll(Arrays.asList(extra));
        return MINI.deserialize(raw, TagResolver.resolver(list));
    }

    public @NotNull net.kyori.adventure.text.Component deserialize(@NotNull String path, @NotNull String def, @NotNull Map<String, String> placeholders) {
        return deserialize(path, def, mapResolvers(placeholders));
    }

    private static TagResolver[] mapResolvers(@NotNull Map<String, String> placeholders) {
        return placeholders.entrySet().stream()
                .map(e -> Placeholder.unparsed(e.getKey(), e.getValue()))
                .toArray(TagResolver[]::new);
    }
}
