package eu.cpvpprac.practicebot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

/**
 * Helpers for sending colour-formatted messages using the Adventure API.
 */
public class MessageUtil {

    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();

    private final ConfigManager config;

    public MessageUtil(ConfigManager config) {
        this.config = config;
    }

    /** Prepend the configured prefix and send to a sender. */
    public void send(CommandSender sender, String message) {
        sender.sendMessage(parse(config.prefix + message));
    }

    /** Parse a legacy & colour-code string into an Adventure Component. */
    public static Component parse(String text) {
        return SERIALIZER.deserialize(text);
    }
}
