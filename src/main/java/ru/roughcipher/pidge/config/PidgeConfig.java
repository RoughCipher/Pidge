package ru.roughcipher.pidge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ru.roughcipher.pidge.Pidge;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class PidgeConfig {
	public static final Gson GSON = new GsonBuilder()
		.setPrettyPrinting()
		.create();

	public static boolean discord_enable = false;
	public static String discord_token = "TOKEN";
	public static String discord_channel = "CHANNEL_ID";
	public static String discord_servername = "BTA Server";

	public static boolean telegram_enable = false;
	public static String telegram_token = "TOKEN";
	public static String telegram_chat_id = "CHAT_ID";

	public static void load() {
		File file = getFilePath();

		if (!file.exists()) {
			initFile(file);
		}

		try {
			FileReader reader = new FileReader(file);
			JsonObject obj = GSON.fromJson(reader, JsonObject.class);
			reader.close();

			updateValues(obj);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		save();
	}

	public static void save() {
		File file = getFilePath();
		JsonObject obj = new JsonObject();
		updateValues(obj);

		try {
			FileWriter writer = new FileWriter(file);
			writer.write(GSON.toJson(obj));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void initFile(File file) {
		try {
			boolean ignore = file.createNewFile();
			FileWriter writer = new FileWriter(file);
			writer.write("{}");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T get(JsonObject object, String key, T defaultValue) {
		JsonElement element = object.get(key);
		if (element == null) {
			object.add(key, GSON.toJsonTree(defaultValue));
			return defaultValue;
		}
		if (defaultValue instanceof java.util.List) {
			return (T) GSON.fromJson(element, java.util.ArrayList.class);
		}
		return GSON.fromJson(element, (Class<T>)defaultValue.getClass());
	}

	@SuppressWarnings("unchecked")
	public static void updateValues(JsonObject object) {
		discord_enable = get(object, "discord_enable", discord_enable);
		discord_token = get(object, "discord_token", discord_token);
		discord_channel = get(object, "discord_channel", discord_channel);
		discord_servername = get(object, "discord_servername", discord_servername);

		telegram_enable = get(object, "telegram_enable", telegram_enable);
		telegram_token = get(object, "telegram_token", telegram_token);
		telegram_chat_id = get(object, "telegram_chat_id", telegram_chat_id);
	}

	public static File getFilePath() {
		return FabricLoader.getInstance().getConfigDir().resolve("pidge.json").toFile();
	}

	public static void printConfigValues() {
		Pidge.info("discord.enable = " + discord_enable);
		Pidge.info("telegram.enable = " + telegram_enable);
	}

	static {
		load();
	}
}
