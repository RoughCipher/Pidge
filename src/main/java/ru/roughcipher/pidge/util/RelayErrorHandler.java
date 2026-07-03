package ru.roughcipher.pidge.util;

import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.telegram.TelegramClient;

public class RelayErrorHandler {
	private static final int DISCORD_LIMIT = 2000;
	private static final int TELEGRAM_LIMIT = 4096;

	public static void sendToDiscord(StandardGuildMessageChannel channel, String message, String context) {
		if (channel == null) return;
		for (String fragment : MessageUtils.splitMessage(message, DISCORD_LIMIT)) {
			try {
				channel.sendMessage(fragment).queue(
					null,
					throwable -> Pidge.LOGGER.error("Discord send failed ({}): {}", context, throwable.getMessage())
				);
			} catch (Exception e) {
				Pidge.LOGGER.error("Discord send failed ({}): {}", context, e.getMessage());
			}
		}
	}

	public static void sendToTelegram(String message, String context) {
		if (!TelegramClient.isInitialized()) return;
		for (String fragment : MessageUtils.splitMessage(message, TELEGRAM_LIMIT)) {
			try {
				TelegramClient.sendMessage(fragment);
			} catch (Exception e) {
				Pidge.LOGGER.error("Telegram send failed ({}): {}", context, e.getMessage());
			}
		}
	}
}
