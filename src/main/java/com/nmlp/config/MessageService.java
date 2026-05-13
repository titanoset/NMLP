package com.nmlp.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
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

    /**
     * Strong first-time notice: title, chat lines, action bar, sound (MiniMessage paths {@code wizard.*}).
     */
    public void showGenderWizardPrompt(@NotNull Player player) {
        Component main = deserialize("wizard.title_main", "<bold><yellow>Выбери пол</yellow></bold>");
        Component sub = deserialize("wizard.title_sub", "<white>/gender male</white> <gray>или</gray> <white>/gender female</white>");
        player.showTitle(Title.title(
                main,
                sub,
                Title.Times.times(Duration.ofMillis(400), Duration.ofSeconds(9), Duration.ofMillis(800))));
        player.sendMessage(deserialize("wizard.chat_spacer", "<dark_gray> </dark_gray>"));
        player.sendMessage(deserialize("wizard.chat_head", "<gold><bold>▶</bold></gold> <yellow>Нужно выбрать пол</yellow>"));
        player.sendMessage(deserialize("wizard.welcome", "<yellow>Добро пожаловать!</yellow>"));
        player.sendMessage(deserialize("wizard.chat_hint", "<gray>Если кнопки не жмутся — набери команду в чат.</gray>"));
        player.sendActionBar(deserialize("wizard.actionbar", "<yellow>/gender male</yellow> <gray>|</gray> <yellow>/gender female</yellow>"));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.MASTER, 1f, 1.15f);
    }

    private static TagResolver[] mapResolvers(@NotNull Map<String, String> placeholders) {
        return placeholders.entrySet().stream()
                .map(e -> Placeholder.unparsed(e.getKey(), e.getValue()))
                .toArray(TagResolver[]::new);
    }
}
