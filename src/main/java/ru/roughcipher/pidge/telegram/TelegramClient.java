package ru.roughcipher.pidge.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.config.PidgeConfig;
import ru.roughcipher.pidge.discord.DiscordChatRelay;
import ru.roughcipher.pidge.util.MessageUtils;

public class TelegramClient {
    private static TelegramBot bot;
    private static volatile boolean initialized = false;

    public static boolean init() {
        if (!PidgeConfig.isTelegramEnabled()) return false;
        try {
            bot = new TelegramBot(PidgeConfig.getTelegramToken());
            bot.setUpdatesListener(updates -> {
                for (Update update : updates) {
                    if (update.message() != null) {
                        Message message = update.message();
                        if (message.chat().id().toString().equals(PidgeConfig.getTelegramChatId())) {
                            String username = message.from().username();
                            String author = (username != null && !username.isEmpty())
                                    ? username
                                    : message.from().firstName() + (message.from().lastName() != null ? " " + message.from().lastName() : "");
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
        for (String fragment : MessageUtils.splitMessage(text, 4096)) {
            bot.execute(new SendMessage(PidgeConfig.getTelegramChatId(), fragment));
        }
    }
}