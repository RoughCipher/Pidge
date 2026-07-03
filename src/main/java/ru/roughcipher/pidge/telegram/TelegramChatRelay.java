package ru.roughcipher.pidge.telegram;

import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.config.PidgeConfig;
import ru.roughcipher.pidge.util.MessageUtils;
import ru.roughcipher.pidge.util.RelayErrorHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.net.command.TextFormatting;
import net.minecraft.core.lang.I18n;

public class TelegramChatRelay {

	public static void sendToMinecraft(String author, String message) {
		MinecraftServer server = MinecraftServer.getInstance();
		if (server == null || server.playerList == null) {
			Pidge.info("Telegram message received but server not ready: " + author + ": " + message);
			return;
		}
		String cleanMessage = MessageUtils.cleanForMinecraft(message);
		String formatted = "[" + TextFormatting.LIGHT_BLUE + "T" + TextFormatting.RESET + "] <" + author + "> " + cleanMessage;
		Pidge.info(formatted);
		for (String line : formatted.split("\n")) {
			server.playerList.sendEncryptedChatToAllPlayers(line);
		}
	}

	public static void sendToTelegram(String author, String message) {
		RelayErrorHandler.sendToTelegram(author + ": " + message, "chat");
	}

	public static void sendJoinLeaveMessage(String username, boolean joined) {
		String key = joined ? "messages.player_joined" : "messages.player_left";
		String pattern = I18n.getInstance().translateKey(key);
		String text = String.format(pattern, username);
		RelayErrorHandler.sendToTelegram(text, "joinleave");
	}

	public static void sendKickMessage(String username, String reason) {
		String pattern = I18n.getInstance().translateKey("messages.player_kicked");
		String text = String.format(pattern, username);
		if (reason != null && !reason.isEmpty()) text += " (" + reason + ")";
		RelayErrorHandler.sendToTelegram(text, "kick");
	}

	public static void sendDeathMessage(String translationKey, Object[] args) {
		String pattern = I18n.getInstance().translateKey(translationKey);
		String translated = String.format(pattern, args);
		String clean = MessageUtils.stripColorCodes(translated);
		RelayErrorHandler.sendToTelegram(clean, "death");
	}

	public static void sendServerStartMessage() {
		RelayErrorHandler.sendToTelegram(PidgeConfig.getServerName() + "\nServer started!", "start");
	}

	public static void sendServerStoppedMessage() {
		RelayErrorHandler.sendToTelegram(PidgeConfig.getServerName() + "\nServer stopped!", "stop");
	}

	public static void sendServerSleepMessage() {
		RelayErrorHandler.sendToTelegram("The Night was Skipped", "sleep");
	}
}
