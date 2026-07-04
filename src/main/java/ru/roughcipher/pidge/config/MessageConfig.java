package ru.roughcipher.pidge.config;

import net.fabricmc.loader.api.FabricLoader;
import ru.roughcipher.pidge.Pidge;
import turniplabs.halplibe.util.toml.Toml;
import turniplabs.halplibe.util.toml.TomlParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MessageConfig {
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("pidge/messages.toml");

	private static String playerJoined = null;
	private static String playerLeft = null;
	private static String playerKicked = null;
	private static String serverStart = "Server started!";
	private static String serverStop = "Server stopped!";
	private static String nightSkipped = "The Night was Skipped";

	static {
		load();
	}

	public static void load() {
		File file = CONFIG_PATH.toFile();
		if (!file.exists()) {
			createDefaultConfig();
		}
		try {
			String content = new String(Files.readAllBytes(CONFIG_PATH));
			Toml toml = TomlParser.parse(content);
			serverStart = getString(toml, "server_start", serverStart);
			serverStop = getString(toml, "server_stop", serverStop);
			nightSkipped = getString(toml, "night_skipped", nightSkipped);
			playerJoined = getStringOrNull(toml, "player_joined");
			playerLeft = getStringOrNull(toml, "player_left");
			playerKicked = getStringOrNull(toml, "player_kicked");
		} catch (Exception e) {
			Pidge.LOGGER.error("Failed to load message config, using defaults", e);
		}
		save();
	}

	public static void save() {
		Toml toml = new Toml("""
                 Pidge Message Configuration
                 Leave player_* entries empty to use the default game messages from LanguagePack.
                 Use %s as placeholder for player name in player_joined, player_left, player_kicked.
                 For player_kicked you can use two %s: first for player name, second for kick reason.
                 Example:
                   server_start = "Server is online!"
                   server_stop = "Server is offline!"
                   night_skipped = "Everyone slept!"
                   player_joined = "Welcome, %s!"
                   player_left = "Goodbye, %s!"
                   player_kicked = "%s was kicked because: %s"
                 Name only:
                   player_kicked = "%s was kicked"
                """);
		toml.addEntry("server_start", serverStart);
		toml.addEntry("server_stop", serverStop);
		toml.addEntry("night_skipped", nightSkipped);
		toml.addEntry("player_joined", playerJoined == null ? "" : playerJoined);
		toml.addEntry("player_left", playerLeft == null ? "" : playerLeft);
		toml.addEntry("player_kicked", playerKicked == null ? "" : playerKicked);

		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
				writer.write(toml.toString());
			}
		} catch (IOException e) {
			Pidge.LOGGER.error("Failed to save message config", e);
		}
	}

	private static void createDefaultConfig() {
		save();
	}

	private static String getString(Toml toml, String key, String defaultValue) {
		String val = toml.get(key, String.class);
		return val != null ? val : defaultValue;
	}

	private static String getStringOrNull(Toml toml, String key) {
		String val = toml.get(key, String.class);
		return (val == null || val.isEmpty()) ? null : val;
	}

	public static String getPlayerJoined() { return playerJoined; }
	public static String getPlayerLeft() { return playerLeft; }
	public static String getPlayerKicked() { return playerKicked; }
	public static String getServerStart() { return serverStart; }
	public static String getServerStop() { return serverStop; }
	public static String getNightSkipped() { return nightSkipped; }

	public static void printConfigValues() {
		Pidge.info("server_start = " + serverStart);
		Pidge.info("server_stop = " + serverStop);
		Pidge.info("night_skipped = " + nightSkipped);
		Pidge.info("player_joined = " + (playerJoined == null ? "(use default)" : playerJoined));
		Pidge.info("player_left = " + (playerLeft == null ? "(use default)" : playerLeft));
		Pidge.info("player_kicked = " + (playerKicked == null ? "(use default)" : playerKicked));
	}
}
