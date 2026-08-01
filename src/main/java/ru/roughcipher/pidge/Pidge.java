package ru.roughcipher.pidge;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.roughcipher.pidge.config.PidgeConfig;
import ru.roughcipher.pidge.config.MessageConfig;
import ru.roughcipher.pidge.discord.DiscordChatRelay;
import ru.roughcipher.pidge.discord.DiscordClient;
import ru.roughcipher.pidge.telegram.TelegramChatRelay;
import ru.roughcipher.pidge.telegram.TelegramClient;
import ru.roughcipher.pidge.util.MessageUtils;
import ru.roughcipher.pidge.util.RelayErrorHandler;

public class Pidge implements ModInitializer {
	public static final String MOD_ID = "pidge";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static volatile boolean shutdownSent = false;
	private static String btaVersion = null;

	@Override
	public void onInitialize() {
		LOGGER.info("Pidge initializing!");
		PidgeConfig.printConfigValues();
		MessageConfig.printConfigValues();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (!shutdownSent) {
				shutdownSent = true;
				LOGGER.info("Shutdown hook triggered");
				sendShutdownMessages();
			}
		}));

		new Thread(() -> {
			if (DiscordClient.init()) {
				DiscordChatRelay.INSTANCE.sendServerStartMessage();
			}
		}).start();

		new Thread(() -> {
			if (TelegramClient.init()) {
				TelegramChatRelay.INSTANCE.sendServerStartMessage();
			}
		}).start();

		LOGGER.info("Pidge initialized!");
	}

	public static void sendShutdownMessages() {
		if (MessageConfig.isServerStopEnabled()) {
			String stopText = PidgeConfig.getServerName() + "\n" + MessageConfig.getServerStop();
			String finalMessage = MessageUtils.withIcon(MessageConfig.getStopIcon(), stopText);

			var discordChannel = DiscordClient.getChannel();
			if (discordChannel != null) {
				RelayErrorHandler.sendToDiscordSync(discordChannel, finalMessage, "stop");
				LOGGER.info("Server stopped message sent to Discord");
			}

			if (TelegramClient.isInitialized()) {
				TelegramClient.sendMessage(finalMessage);
				LOGGER.info("Server stopped message sent to Telegram");
			}

			try {
				Thread.sleep(2000);
			} catch (InterruptedException ignored) {}
		}

		DiscordClient.shutdown();
		TelegramClient.shutdown();
	}

	public static void markShutdownSent() {
		shutdownSent = true;
	}

	public static void info(String s) {
		LOGGER.info(s);
	}

	public static String getBTAVersion() {
		if (btaVersion != null) return btaVersion;
		try {
			java.lang.reflect.Field field = net.minecraft.core.Version.class.getField("VERSION");
			btaVersion = (String) field.get(null);
		} catch (Exception e) {
			btaVersion = "unknown";
		}
		return btaVersion;
	}
}
