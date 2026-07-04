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

	private static String startIcon = "▶";
	private static String stopIcon = "⏸";
	private static String nightSkippedIcon = "🛏";
	private static String joinIcon = "›";
	private static String leaveIcon = "‹";
	private static String kickIcon = "⚒";
	private static String deathIcon = "☠";
	private static String gameChatIcon = "✉";

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

			startIcon = getString(toml, "start_icon", startIcon);
			stopIcon = getString(toml, "stop_icon", stopIcon);
			nightSkippedIcon = getString(toml, "night_skipped_icon", nightSkippedIcon);
			joinIcon = getString(toml, "join_icon", joinIcon);
			leaveIcon = getString(toml, "leave_icon", leaveIcon);
			kickIcon = getString(toml, "kick_icon", kickIcon);
			deathIcon = getString(toml, "death_icon", deathIcon);
			gameChatIcon = getString(toml, "game_chat_icon", gameChatIcon);
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

                 Icons are added before the message (e.g., "🛏 The Night was Skipped").
                 Icons (leave empty to disable):
                   start_icon = "▶"
                   stop_icon = "⏸"
                   night_skipped_icon = "🛏"
                   join_icon = "›"
                   leave_icon = "‹"
                   kick_icon = "⚒"
                   death_icon = "☠"
                   game_chat_icon = "✉"
                """);
		toml.addEntry("server_start", serverStart);
		toml.addEntry("server_stop", serverStop);
		toml.addEntry("night_skipped", nightSkipped);
		toml.addEntry("player_joined", playerJoined == null ? "" : playerJoined);
		toml.addEntry("player_left", playerLeft == null ? "" : playerLeft);
		toml.addEntry("player_kicked", playerKicked == null ? "" : playerKicked);
		toml.addEntry("start_icon", startIcon);
		toml.addEntry("stop_icon", stopIcon);
		toml.addEntry("night_skipped_icon", nightSkippedIcon);
		toml.addEntry("join_icon", joinIcon);
		toml.addEntry("leave_icon", leaveIcon);
		toml.addEntry("kick_icon", kickIcon);
		toml.addEntry("death_icon", deathIcon);
		toml.addEntry("game_chat_icon", gameChatIcon);

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

	public static String getStartIcon() { return startIcon; }
	public static String getStopIcon() { return stopIcon; }
	public static String getNightSkippedIcon() { return nightSkippedIcon; }
	public static String getJoinIcon() { return joinIcon; }
	public static String getLeaveIcon() { return leaveIcon; }
	public static String getKickIcon() { return kickIcon; }
	public static String getDeathIcon() { return deathIcon; }
	public static String getGameChatIcon() { return gameChatIcon; }

	public static void printConfigValues() {
		Pidge.info("server_start = " + serverStart);
		Pidge.info("server_stop = " + serverStop);
		Pidge.info("night_skipped = " + nightSkipped);
		Pidge.info("player_joined = " + (playerJoined == null ? "(use default)" : playerJoined));
		Pidge.info("player_left = " + (playerLeft == null ? "(use default)" : playerLeft));
		Pidge.info("player_kicked = " + (playerKicked == null ? "(use default)" : playerKicked));
		Pidge.info("start_icon = \"" + startIcon + "\"");
		Pidge.info("stop_icon = \"" + stopIcon + "\"");
		Pidge.info("night_skipped_icon = \"" + nightSkippedIcon + "\"");
		Pidge.info("join_icon = \"" + joinIcon + "\"");
		Pidge.info("leave_icon = \"" + leaveIcon + "\"");
		Pidge.info("kick_icon = \"" + kickIcon + "\"");
		Pidge.info("death_icon = \"" + deathIcon + "\"");
		Pidge.info("game_chat_icon = \"" + gameChatIcon + "\"");
	}
}
