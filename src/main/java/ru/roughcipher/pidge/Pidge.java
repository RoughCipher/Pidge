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

	@Override
	public void onInitialize() {
		LOGGER.info("Pidge initializing!");
		PidgeConfig.printConfigValues();
		new Thread(() -> {
			LOGGER.info("Starting Discord client...");
			if (DiscordClient.init()) {
				LOGGER.info("Discord client started successfully!");
				DiscordChatRelay.sendServerStartMessage();
			} else {
				LOGGER.warn("Discord client failed to start or is disabled.");
			}
		}).start();
		new Thread(() -> {
			LOGGER.info("Starting Telegram client...");
			if (TelegramClient.init()) {
				LOGGER.info("Telegram client started successfully!");
				TelegramChatRelay.sendServerStartMessage();
			} else {
				LOGGER.warn("Telegram client failed to start or is disabled.");
			}
		}).start();
		LOGGER.info("Pidge initialized!");
	}

	public static void info(String s) {
		LOGGER.info(s);
	}
}
