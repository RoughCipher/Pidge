package ru.roughcipher.pidge.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetMeResponse;
import net.minecraft.core.lang.I18n;
import net.minecraft.server.MinecraftServer;
import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.util.MessageUtils;
import ru.roughcipher.pidge.config.PidgeConfig;
import ru.roughcipher.pidge.discord.DiscordChatRelay;
import ru.roughcipher.pidge.util.AdminCommands;
import ru.roughcipher.pidge.util.BaseChatRelay;
import ru.roughcipher.pidge.util.ProxyUtils;

import okhttp3.OkHttpClient;

public class TelegramClient {
	private static TelegramBot bot;
	private static volatile boolean initialized = false;
	private static String botUsername = null;

	public static boolean init() {
		if (!PidgeConfig.isTelegramEnabled()) return false;
		try {
			ProxyUtils.ParsedProxy parsedProxy = ProxyUtils.parse(PidgeConfig.getProxy());
			if (parsedProxy != null) {
				OkHttpClient httpClient = ProxyUtils.buildClient(parsedProxy, "Telegram");
				bot = new TelegramBot.Builder(PidgeConfig.getTelegramToken())
					.okHttpClient(httpClient)
					.build();
			} else {
				bot = new TelegramBot(PidgeConfig.getTelegramToken());
			}

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
						String chatId = message.chat().id().toString();
						if (!chatId.equals(PidgeConfig.getTelegramChatId())) continue;

						String text = message.text();
						if (text == null) continue;

						String lower = text.toLowerCase();
						long userId = message.from().id();
						String authorId = String.valueOf(userId);
						String authorName = message.from().username() != null
							? "@" + message.from().username() + " (" + userId + ")"
							: String.valueOf(userId);

						// /list
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
							Pidge.LOGGER.info("Telegram /list command by {}", authorName);
							bot.execute(new SendMessage(chatId, BaseChatRelay.getPlayerListString()));
							continue;
						}

						// whitelist commands
						MinecraftServer server = MinecraftServer.getInstance();
						if (!PidgeConfig.getTelegramAdminIds().contains(authorId)) {
							Pidge.LOGGER.warn("Telegram unauthorized command by {}", authorName);
							bot.execute(new SendMessage(chatId, "You are not authorized to use this command."));
							continue;
						}
						if (server == null || server.playerList == null) {
							Pidge.LOGGER.error("Telegram command failed: server not ready");
							bot.execute(new SendMessage(chatId, "Server not ready."));
							continue;
						}

						// remove @botusername
						String normalized = text.trim();
						if (botUsername != null) {
							String suffix = "@" + botUsername;
							if (normalized.endsWith(suffix)) {
								normalized = normalized.substring(0, normalized.length() - suffix.length());
							}
						}
						// /whitelist reload
						if (normalized.equals("/whitelist reload")) {
							Pidge.LOGGER.info("Telegram /whitelist reload requested by {}", authorName);
							try {
								server.playerList.reloadWhiteList();
								String msg = I18n.getInstance().translateKey("command.commands.whitelist.reload");
								bot.execute(new SendMessage(chatId, msg));
								Pidge.LOGGER.info("Telegram /whitelist reload succeeded");
							} catch (Exception e) {
								Pidge.LOGGER.error("Telegram /whitelist reload failed", e);
								bot.execute(new SendMessage(chatId, "Failed to reload whitelist: " + e.getMessage()));
							}
							continue;
						}

						// /whitelist on / off
						if (normalized.equals("/whitelist on") || normalized.equals("/whitelist off")) {
							boolean enable = normalized.equals("/whitelist on");
							server.propertyManager.setProperty("white-list", enable);
							server.playerList.whitelistEnforced = enable;
							String key = enable ? "command.commands.whitelist.on.success" : "command.commands.whitelist.off.success";
							String response = I18n.getInstance().translateKey(key);
							bot.execute(new SendMessage(chatId, response));
							Pidge.LOGGER.info("Telegram /whitelist {} executed by {}", normalized, authorName);
							continue;
						}

						// /whitelist remove <player> [ely|mojang]
						if (lower.startsWith("/whitelist remove ")) {
							String[] parts = text.split(" ");
							if (parts.length < 3) {
								bot.execute(new SendMessage(chatId, "Usage: /whitelist remove <player> [ely|mojang]"));
								continue;
							}
							String playerName = parts[2];
							if (playerName.length() > 16) {
								bot.execute(new SendMessage(chatId, "Player name must be 16 characters or less."));
								continue;
							}
							String backend = AdminCommands.parseBackendArg(parts, 3);
							Pidge.LOGGER.info("Telegram /whitelist remove {} {} requested by {}", playerName, backend, authorName);
							AdminCommands.whitelistRemove(playerName, backend, new AdminCommands.Reply() {
								@Override public void success(String message) { bot.execute(new SendMessage(chatId, message)); }
								@Override public void failure(String message) { bot.execute(new SendMessage(chatId, message)); }
							});
							continue;
						}

						// /whitelist add <player> [ely|mojang]
						if (lower.startsWith("/whitelist add ")) {
							String[] parts = text.split(" ");
							if (parts.length < 3) {
								bot.execute(new SendMessage(chatId, "Usage: /whitelist add <player> [ely|mojang]"));
								continue;
							}
							String playerName = parts[2];
							if (playerName.length() > 16) {
								bot.execute(new SendMessage(chatId, "Player name must be 16 characters or less."));
								continue;
							}
							String backend = AdminCommands.parseBackendArg(parts, 3);
							Pidge.LOGGER.info("Telegram /whitelist add {} {} requested by {}", playerName, backend, authorName);
							AdminCommands.whitelistAdd(playerName, backend, new AdminCommands.Reply() {
								@Override public void success(String message) { bot.execute(new SendMessage(chatId, message)); }
								@Override public void failure(String message) { bot.execute(new SendMessage(chatId, message)); }
							});
							continue;
						}

						// /ban <player> [ely|mojang]
						if (lower.startsWith("/ban ") || lower.startsWith("/ban@")) {
							String[] parts = text.trim().split(" ");
							final String playerName;
							playerName = parts.length >= 2 ? parts[1] : "";
							if (playerName.isEmpty()) {
								bot.execute(new SendMessage(chatId, "Usage: /ban <player> [ely|mojang]"));
								continue;
							}
							if (playerName.length() > 16) {
								bot.execute(new SendMessage(chatId, "Player name must be 16 characters or less."));
								continue;
							}
							String backend = AdminCommands.parseBackendArg(parts, 2);
							Pidge.LOGGER.info("Telegram /ban {} {} requested by {}", playerName, backend, authorName);
							AdminCommands.ban(playerName, backend, new AdminCommands.Reply() {
								@Override public void success(String message) { bot.execute(new SendMessage(chatId, message)); }
								@Override public void failure(String message) { bot.execute(new SendMessage(chatId, message)); }
							});
							continue;
						}

						// /unban <player> [ely|mojang]
						if (lower.startsWith("/unban ") || lower.startsWith("/unban@")) {
							String[] parts = text.trim().split(" ");
							final String playerName;
							playerName = parts.length >= 2 ? parts[1] : "";
							if (playerName.isEmpty()) {
								bot.execute(new SendMessage(chatId, "Usage: /unban <player> [ely|mojang]"));
								continue;
							}
							if (playerName.length() > 16) {
								bot.execute(new SendMessage(chatId, "Player name must be 16 characters or less."));
								continue;
							}
							String backend = AdminCommands.parseBackendArg(parts, 2);
							Pidge.LOGGER.info("Telegram /unban {} {} requested by {}", playerName, backend, authorName);
							AdminCommands.unban(playerName, backend, new AdminCommands.Reply() {
								@Override public void success(String message) { bot.execute(new SendMessage(chatId, message)); }
								@Override public void failure(String message) { bot.execute(new SendMessage(chatId, message)); }
							});
							continue;
						}

						String username = message.from().username();
						String author = (username != null && !username.isEmpty())
							? username
							: message.from().firstName() + (message.from().lastName() != null ? " " + message.from().lastName() : "");
						if (!text.isEmpty()) {
							TelegramChatRelay.INSTANCE.sendToMinecraft(author, text);
							DiscordChatRelay.INSTANCE.sendToDiscord("[T] " + author, text);
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
		bot.execute(new SendMessage(PidgeConfig.getTelegramChatId(), MessageUtils.escapeTelegramMentions(text)));
	}
}
