package ru.roughcipher.pidge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ru.roughcipher.pidge.Pidge;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PidgeConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(PidgeConfig.class);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static boolean discordEnable = false;
	private static String discordToken = "TOKEN";
	private static String discordChannel = "CHANNEL_ID";
	private static List<String> discordAdminIds = new ArrayList<>();

	private static boolean telegramEnable = false;
	private static String telegramToken = "TOKEN";
	private static String telegramChatId = "CHAT_ID";
	private static List<String> telegramAdminIds = new ArrayList<>();

	private static String serverName = "BTA Server";

	public static boolean isDiscordEnabled() { return discordEnable; }
	public static String getDiscordToken() { return discordToken; }
	public static String getDiscordChannel() { return discordChannel; }
	public static List<String> getDiscordAdminIds() { return discordAdminIds; }

	public static boolean isTelegramEnabled() { return telegramEnable; }
	public static String getTelegramToken() { return telegramToken; }
	public static String getTelegramChatId() { return telegramChatId; }
	public static List<String> getTelegramAdminIds() { return telegramAdminIds; }

	public static String getServerName() { return serverName; }

	public static void load() {
		File file = getFilePath();
		if (!file.exists()) initFile(file);
		try (FileReader reader = new FileReader(file)) {
			JsonObject obj = GSON.fromJson(reader, JsonObject.class);
			updateValues(obj);
		} catch (IOException e) {
			throw new RuntimeException("Failed to load config", e);
		}
		save();
	}

	public static void save() {
		File file = getFilePath();
		JsonObject obj = new JsonObject();
		updateValues(obj);
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(GSON.toJson(obj));
		} catch (IOException e) {
			LOGGER.error("Failed to save config", e);
		}
	}

	private static void initFile(File file) {
		try {
			Path parent = file.getParentFile().toPath();
			if (!Files.exists(parent)) Files.createDirectories(parent);
			if (file.createNewFile()) {
				try (FileWriter writer = new FileWriter(file)) {
					writer.write("{}");
				}
			}
		} catch (IOException e) {
			LOGGER.error("Failed to initialize config file", e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T get(JsonObject object, String key, T defaultValue) {
		JsonElement element = object.get(key);
		if (element == null) {
			object.add(key, GSON.toJsonTree(defaultValue));
			return defaultValue;
		}
		if (defaultValue instanceof List) {
			return (T) GSON.fromJson(element, ArrayList.class);
		}
		return GSON.fromJson(element, (Class<T>) defaultValue.getClass());
	}

	private static void updateValues(JsonObject object) {
		serverName = get(object, "server_name", serverName);

		discordEnable = get(object, "discord_enable", discordEnable);
		discordToken = get(object, "discord_token", discordToken);
		discordChannel = get(object, "discord_channel", discordChannel);
		discordAdminIds = get(object, "discord_admin_ids", discordAdminIds);

		telegramEnable = get(object, "telegram_enable", telegramEnable);
		telegramToken = get(object, "telegram_token", telegramToken);
		telegramChatId = get(object, "telegram_chat_id", telegramChatId);
		telegramAdminIds = get(object, "telegram_admin_ids", telegramAdminIds);
	}

	private static File getFilePath() {
		Path configDir = FabricLoader.getInstance().getConfigDir().resolve("pidge");
		try {
			Files.createDirectories(configDir);
		} catch (IOException e) {
			LOGGER.error("Failed to create config directory", e);
		}
		return configDir.resolve("pidge.json").toFile();
	}

	public static void printConfigValues() {
		Pidge.info("server.name = " + serverName);
		Pidge.info("discord.enable = " + discordEnable);
		Pidge.info("telegram.enable = " + telegramEnable);
		Pidge.info("discord.admin_ids = " + discordAdminIds);
		Pidge.info("telegram.admin_ids = " + telegramAdminIds);
	}

	static {
		load();
	}
}
