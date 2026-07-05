package ru.roughcipher.pidge.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetMeResponse;
import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.config.PidgeConfig;
import ru.roughcipher.pidge.discord.DiscordChatRelay;
import ru.roughcipher.pidge.util.BaseChatRelay;

public class TelegramClient {
	private static TelegramBot bot;
	private static volatile boolean initialized = false;
	private static String botUsername = null;

	public static boolean init() {
		if (!PidgeConfig.isTelegramEnabled()) return false;
		try {
			bot = new TelegramBot(PidgeConfig.getTelegramToken());

			GetMeResponse me = bot.execute(new GetMe());
			if (me.isOk() && me.user() != null) {
				botUsername = me.user().username();
				Pidge.LOGGER.info("Telegram bot username: @{}", botUsername);
			} else {
				Pidge.LOGGER.warn("Failed to get bot username, commands without @ will still work");
			}

			bot.setUpdatesListener(updates -> {
				for (Update update : updates) {
					if (update.message() != null) {
						Message message = update.message();
						if (message.chat().id().toString().equals(PidgeConfig.getTelegramChatId())) {
							String text = message.text();
							if (text != null) {
								String lower = text.toLowerCase();
								if (lower.startsWith("/list")) {
									if (botUsername != null) {
										int atIndex = lower.indexOf('@');
										if (atIndex > 0) {
											String mentionedBot = lower.substring(atIndex + 1);
											if (!mentionedBot.equalsIgnoreCase(botUsername)) {
												continue;
											}
										}
									}
									bot.execute(new SendMessage(PidgeConfig.getTelegramChatId(), BaseChatRelay.getPlayerListString()));
									continue;
								}
							}

							String username = message.from().username();
							String author = (username != null && !username.isEmpty())
								? username
								: message.from().firstName() + (message.from().lastName() != null ? " " + message.from().lastName() : "");
							if (text != null && !text.isEmpty()) {
								TelegramChatRelay.INSTANCE.sendToMinecraft(author, text);
								DiscordChatRelay.INSTANCE.sendToDiscord("[T] " + author, text);
							}
						}
					}
				}
				return UpdatesListener.CONFIRMED_UPDATES_ALL;
			});
			initialized = true;
			Pidge.LOGGER.info("Telegram client started");
			return true;
		} catch (Throwable t) {
			Pidge.LOGGER.error("Telegram init failed", t);
			return false;
		}
	}

	public static void shutdown() {
		if (bot != null) {
			try {
				bot.removeGetUpdatesListener();
				initialized = false;
				Pidge.LOGGER.info("Telegram client shut down");
			} catch (Exception e) {
				Pidge.LOGGER.error("Telegram shutdown error", e);
			}
		}
	}

	public static boolean isInitialized() {
		return initialized && bot != null;
	}

	public static void sendMessage(String text) {
		if (!isInitialized()) return;
		bot.execute(new SendMessage(PidgeConfig.getTelegramChatId(), text));
	}
}
