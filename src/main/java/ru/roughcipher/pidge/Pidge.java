package ru.roughcipher.pidge;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.roughcipher.pidge.config.PidgeConfig;
import ru.roughcipher.pidge.discord.DiscordChatRelay;
import ru.roughcipher.pidge.discord.DiscordClient;
import ru.roughcipher.pidge.telegram.TelegramChatRelay;
import ru.roughcipher.pidge.telegram.TelegramClient;
import turniplabs.halplibe.HalpLibe;

public class Pidge implements ModInitializer {
    public static final String MOD_ID = HalpLibe.registerMod("pidge", true);
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static volatile boolean shutdownSent = false;

    @Override
    public void onInitialize() {
        LOGGER.info("Pidge initializing!");
        PidgeConfig.printConfigValues();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!shutdownSent) {
                shutdownSent = true;
                LOGGER.info("Shutdown hook triggered");
                sendShutdownMessages();
            }
        }));

        new Thread(() -> {
            if (DiscordClient.init()) {
                DiscordChatRelay.sendServerStartMessage();
            }
        }).start();

        new Thread(() -> {
            if (TelegramClient.init()) {
                TelegramChatRelay.sendServerStartMessage();
            }
        }).start();

        LOGGER.info("Pidge initialized!");
    }

    public static void sendShutdownMessages() {
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        DiscordChatRelay.sendServerStoppedMessage();
        TelegramChatRelay.sendServerStoppedMessage();
        DiscordClient.shutdown();
        TelegramClient.shutdown();
    }

    public static void markShutdownSent() {
        shutdownSent = true;
    }

    public static void info(String s) {
        LOGGER.info(s);
    }
}