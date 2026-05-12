package com.nmlp.config;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private CommandSender sender;

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("prefix", "");
        yaml.set("risk.use", "generic <id>");
        yaml.set("risk.by_action.sex.use", "specific sex");
        MessagesConfig messagesConfig = new MessagesConfig(yaml);
        messageService = new MessageService();
        messageService.reload(messagesConfig);
    }

    @Test
    void sendRiskKindPrefersByActionTemplate() {
        messageService.sendRiskKind(sender, "sex", "use", "ultimate", Map.of("id", "sex"));

        ArgumentCaptor<net.kyori.adventure.text.Component> cap =
                ArgumentCaptor.forClass(net.kyori.adventure.text.Component.class);
        verify(sender).sendMessage(cap.capture());
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(cap.getValue());
        assertTrue(plain.contains("specific sex"), plain);
    }

    @Test
    void sendRiskKindFallsBackToGenericRisk() {
        messageService.sendRiskKind(sender, "unknown_action", "use", "ultimate-fallback", Map.of("id", "x"));

        ArgumentCaptor<net.kyori.adventure.text.Component> cap =
                ArgumentCaptor.forClass(net.kyori.adventure.text.Component.class);
        verify(sender).sendMessage(cap.capture());
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(cap.getValue());
        assertTrue(plain.contains("generic") && plain.contains("x"), plain);
    }
}
