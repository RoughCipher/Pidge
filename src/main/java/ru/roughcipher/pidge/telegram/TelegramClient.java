package ru.roughcipher.pidge.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.config.PidgeConfig;
import ru.roughcipher.pidge.discord.DiscordChatRelay;

import java.util.ArrayList;
import java.util.List;

public class TelegramClient {
	public static TelegramBot bot;
	private static boolean initialized = false;
	private static final int TELEGRAM_CONTENT_LIMIT = 4096;

	public static boolean init() {
		if (!PidgeConfig.telegram_enable) {
			return false;
		}

		try {
			bot = new TelegramBot(PidgeConfig.telegram_token);
			bot.setUpdatesListener(updates -> {
				for (Update update : updates) {
					if (update.message() != null) {
						Message message = update.message();
						if (message.chat().id().toString().equals(PidgeConfig.telegram_chat_id)) {
							String username = message.from().username();
							String author;
							if (username != null && !username.isEmpty()) {
								author = username;
							} else {
								author = message.from().firstName() +
									(message.from().lastName() != null ? " " + message.from().lastName() : "");
							}
							String text = message.text();
							if (text != null && !text.isEmpty()) {
								TelegramChatRelay.sendToMinecraft(author, text);
								DiscordChatRelay.sendToDiscord("[T] " + author, text);
							}
						}
					}
				}
				return UpdatesListener.CONFIRMED_UPDATES_ALL;
			});
			initialized = true;
			Pidge.LOGGER.info("Telegram bot started successfully!");
			return true;
		} catch (Throwable t) {
			Pidge.LOGGER.error("Unable to start Telegram bot.", t);
			return false;
		}
	}

	public static boolean isInitialized() {
		return initialized && bot != null;
	}

	public static void sendMessage(String text) {
		if (!isInitialized()) return;
		List<String> fragments = splitMessage(text, TELEGRAM_CONTENT_LIMIT);
		for (String fragment : fragments) {
			bot.execute(new SendMessage(PidgeConfig.telegram_chat_id, fragment));
		}
	}

	private static List<String> splitMessage(String text, int limit) {
		List<String> parts = new ArrayList<>();
		if (text.length() <= limit) {
			parts.add(text);
			return parts;
		}
		int start = 0;
		while (start < text.length()) {
			int end = Math.min(start + limit, text.length());
			if (end < text.length()) {
				int lastSpace = text.lastIndexOf(' ', end);
				if (lastSpace > start) {
					end = lastSpace;
				}
			}
			parts.add(text.substring(start, end));
			start = end;
			while (start < text.length() && text.charAt(start) == ' ') start++;
		}
		return parts;
	}
}
