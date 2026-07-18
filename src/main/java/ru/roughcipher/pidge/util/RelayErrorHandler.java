package ru.roughcipher.pidge.util;

import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.telegram.TelegramClient;

public class RelayErrorHandler {
	private static final int DISCORD_LIMIT = 2000;
	private static final int TELEGRAM_LIMIT = 4096;

	private static void logError(String platform, String context, String message) {
		Pidge.LOGGER.error("{} send failed ({}): {}", platform, context, message);
	}

	public static void sendToDiscord(StandardGuildMessageChannel channel, String message, String context) {
		if (channel == null) return;
		for (String fragment : MessageUtils.splitMessage(message, DISCORD_LIMIT)) {
			try {
				channel.sendMessage(fragment).queue(
					null,
					throwable -> logError("Discord", context, throwable.getMessage())
				);
			} catch (Exception e) {
				logError("Discord", context, e.getMessage());
			}
		}
	}

	public static void sendToDiscordSync(StandardGuildMessageChannel channel, String message, String context) {
		if (channel == null) return;
		for (String fragment : MessageUtils.splitMessage(message, DISCORD_LIMIT)) {
			try {
				channel.sendMessage(fragment).complete();
			} catch (Exception e) {
				logError("Discord", context, e.getMessage());
			}
		}
	}

	public static void sendToTelegram(String message, String context) {
		for (String fragment : MessageUtils.splitMessage(message, TELEGRAM_LIMIT)) {
			try {
				TelegramClient.sendMessage(fragment);
			} catch (Exception e) {
				logError("Telegram", context, e.getMessage());
			}
		}
	}
}
