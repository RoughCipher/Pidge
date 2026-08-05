package ru.roughcipher.pidge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import ru.roughcipher.pidge.Pidge;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MessageConfig {
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("pidge/messages.json");

	private static String playerJoined = null;
	private static String playerLeft = null;
	private static String playerKicked = null;
	private static String serverStart = "Server started!";
	private static String serverStop = "Server stopped!";
	private static String nightSkipped = null;

	private static String startIcon = "▶";
	private static String stopIcon = "⏸";
	private static String nightSkippedIcon = "🛏";
	private static String joinIcon = "›";
	private static String leaveIcon = "‹";
	private static String kickIcon = "⚒";
	private static String deathIcon = "☠";
	private static String mobDeathIcon = "☠";
	private static String gameChatIcon = "✉";

	private static boolean enablePlayerJoined = true;
	private static boolean enablePlayerLeft = true;
	private static boolean enablePlayerKicked = true;
	private static boolean enableServerStart = true;
	private static boolean enableServerStop = true;
	private static boolean enableNightSkipped = true;
	private static boolean enableDeath = true;
	private static boolean enableMobDeath = true;
	private static boolean enableSay = true;
	private static boolean enableMe = true;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	static {
		load();
	}

	public static void load() {
		File file = CONFIG_PATH.toFile();
		if (!file.exists()) {
			createDefaultConfig();
		}
		try (FileReader reader = new FileReader(file)) {
			MessageData data = GSON.fromJson(reader, MessageData.class);
			if (data != null) {
				if (data.server_start != null) serverStart = data.server_start;
				if (data.server_stop != null) serverStop = data.server_stop;
				if (data.night_skipped != null) nightSkipped = data.night_skipped.isEmpty() ? null : data.night_skipped;
				if (data.player_joined != null) playerJoined = data.player_joined.isEmpty() ? null : data.player_joined;
				if (data.player_left != null) playerLeft = data.player_left.isEmpty() ? null : data.player_left;
				if (data.player_kicked != null) playerKicked = data.player_kicked.isEmpty() ? null : data.player_kicked;
				if (data.start_icon != null) startIcon = data.start_icon;
				if (data.stop_icon != null) stopIcon = data.stop_icon;
				if (data.night_skipped_icon != null) nightSkippedIcon = data.night_skipped_icon;
				if (data.join_icon != null) joinIcon = data.join_icon;
				if (data.leave_icon != null) leaveIcon = data.leave_icon;
				if (data.kick_icon != null) kickIcon = data.kick_icon;
				if (data.death_icon != null) deathIcon = data.death_icon;
				if (data.mob_death_icon != null) mobDeathIcon = data.mob_death_icon;
				if (data.game_chat_icon != null) gameChatIcon = data.game_chat_icon;

				if (data.enable_player_joined != null) enablePlayerJoined = data.enable_player_joined;
				if (data.enable_player_left != null) enablePlayerLeft = data.enable_player_left;
				if (data.enable_player_kicked != null) enablePlayerKicked = data.enable_player_kicked;
				if (data.enable_server_start != null) enableServerStart = data.enable_server_start;
				if (data.enable_server_stop != null) enableServerStop = data.enable_server_stop;
				if (data.enable_night_skipped != null) enableNightSkipped = data.enable_night_skipped;
				if (data.enable_death != null) enableDeath = data.enable_death;
				if (data.enable_mob_death != null) enableMobDeath = data.enable_mob_death;
				if (data.enable_say != null) enableSay = data.enable_say;
				if (data.enable_me != null) enableMe = data.enable_me;
			}
		} catch (IOException e) {
			Pidge.LOGGER.error("Failed to load message config, using defaults", e);
		}
		save();
	}

	public static void save() {
		MessageData data = new MessageData();
		data.server_start = serverStart;
		data.server_stop = serverStop;
		data.night_skipped = nightSkipped == null ? "" : nightSkipped;
		data.player_joined = playerJoined == null ? "" : playerJoined;
		data.player_left = playerLeft == null ? "" : playerLeft;
		data.player_kicked = playerKicked == null ? "" : playerKicked;
		data.start_icon = startIcon;
		data.stop_icon = stopIcon;
		data.night_skipped_icon = nightSkippedIcon;
		data.join_icon = joinIcon;
		data.leave_icon = leaveIcon;
		data.kick_icon = kickIcon;
		data.death_icon = deathIcon;
		data.mob_death_icon = mobDeathIcon;
		data.game_chat_icon = gameChatIcon;

		data.enable_player_joined = enablePlayerJoined;
		data.enable_player_left = enablePlayerLeft;
		data.enable_player_kicked = enablePlayerKicked;
		data.enable_server_start = enableServerStart;
		data.enable_server_stop = enableServerStop;
		data.enable_night_skipped = enableNightSkipped;
		data.enable_death = enableDeath;
		data.enable_mob_death = enableMobDeath;
		data.enable_say = enableSay;
		data.enable_me = enableMe;

		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
				writer.write(GSON.toJson(data));
			}
		} catch (IOException e) {
			Pidge.LOGGER.error("Failed to save message config", e);
		}
	}

	private static void createDefaultConfig() {
		save();
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
	public static String getMobDeathIcon() { return mobDeathIcon; }
	public static String getGameChatIcon() { return gameChatIcon; }

	public static boolean isPlayerJoinedEnabled() { return enablePlayerJoined; }
	public static boolean isPlayerLeftEnabled() { return enablePlayerLeft; }
	public static boolean isPlayerKickedEnabled() { return enablePlayerKicked; }
	public static boolean isServerStartEnabled() { return enableServerStart; }
	public static boolean isServerStopEnabled() { return enableServerStop; }
	public static boolean isNightSkippedEnabled() { return enableNightSkipped; }
	public static boolean isDeathEnabled() { return enableDeath; }
	public static boolean isMobDeathEnabled() { return enableMobDeath; }
	public static boolean isSayEnabled() { return enableSay; }
	public static boolean isMeEnabled() { return enableMe; }

	public static void printConfigValues() {
		Pidge.info("server_start = " + serverStart + " (enabled=" + enableServerStart + ")");
		Pidge.info("server_stop = " + serverStop + " (enabled=" + enableServerStop + ")");
		Pidge.info("night_skipped = " + (nightSkipped == null ? "(use default)" : nightSkipped) + " (enabled=" + enableNightSkipped + ")");
		Pidge.info("player_joined = " + (playerJoined == null ? "(use default)" : playerJoined) + " (enabled=" + enablePlayerJoined + ")");
		Pidge.info("player_left = " + (playerLeft == null ? "(use default)" : playerLeft) + " (enabled=" + enablePlayerLeft + ")");
		Pidge.info("player_kicked = " + (playerKicked == null ? "(use default)" : playerKicked) + " (enabled=" + enablePlayerKicked + ")");
		Pidge.info("enable_death = " + enableDeath);
		Pidge.info("enable_mob_death = " + enableMobDeath);
		Pidge.info("enable_say = " + enableSay);
		Pidge.info("enable_me = " + enableMe);
		Pidge.info("start_icon = \"" + startIcon + "\"");
		Pidge.info("stop_icon = \"" + stopIcon + "\"");
		Pidge.info("night_skipped_icon = \"" + nightSkippedIcon + "\"");
		Pidge.info("join_icon = \"" + joinIcon + "\"");
		Pidge.info("leave_icon = \"" + leaveIcon + "\"");
		Pidge.info("kick_icon = \"" + kickIcon + "\"");
		Pidge.info("death_icon = \"" + deathIcon + "\"");
		Pidge.info("mob_death_icon = \"" + mobDeathIcon + "\"");
		Pidge.info("game_chat_icon = \"" + gameChatIcon + "\"");
	}

	private static class MessageData {
		String player_joined;
		String player_left;
		String player_kicked;
		String server_start;
		String server_stop;
		String night_skipped;
		String start_icon;
		String stop_icon;
		String night_skipped_icon;
		String join_icon;
		String leave_icon;
		String kick_icon;
		String death_icon;
		String mob_death_icon;
		String game_chat_icon;

		Boolean enable_player_joined;
		Boolean enable_player_left;
		Boolean enable_player_kicked;
		Boolean enable_server_start;
		Boolean enable_server_stop;
		Boolean enable_night_skipped;
		Boolean enable_death;
		Boolean enable_mob_death;
		Boolean enable_say;
		Boolean enable_me;
	}
}
