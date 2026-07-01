package ru.roughcipher.pidge.telegram;

import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.config.PidgeConfig;
import ru.roughcipher.pidge.util.MessageUtils;
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
		if (!TelegramClient.isInitialized()) return;
		TelegramClient.sendMessage(author + ": " + message);
	}

	public static void sendJoinLeaveMessage(String username, boolean joined) {
		if (!TelegramClient.isInitialized()) return;
		String key = joined ? "messages.player_joined" : "messages.player_left";
		String pattern = I18n.getInstance().translateKey(key);
		String text = String.format(pattern, username);
		TelegramClient.sendMessage(text);
	}

	public static void sendKickMessage(String username, String reason) {
		if (!TelegramClient.isInitialized()) return;
		String pattern = I18n.getInstance().translateKey("messages.player_kicked");
		String text = String.format(pattern, username);
		if (reason != null && !reason.isEmpty()) {
			text += " (" + reason + ")";
		}
		TelegramClient.sendMessage(text);
	}

	public static void sendDeathMessage(String translationKey, Object[] args) {
		if (!TelegramClient.isInitialized()) return;
		String pattern = I18n.getInstance().translateKey(translationKey);
		String translated = String.format(pattern, args);
		String clean = MessageUtils.stripColorCodes(translated);
		TelegramClient.sendMessage(clean);
	}

	public static void sendServerStartMessage() {
		if (!TelegramClient.isInitialized()) return;
		TelegramClient.sendMessage(PidgeConfig.getServerName() + "\nServer started!");
	}

	public static void sendServerStoppedMessage() {
		if (!TelegramClient.isInitialized()) return;
		TelegramClient.sendMessage(PidgeConfig.getServerName() + "\nServer stopped!");
	}

	public static void sendServerSleepMessage() {
		if (!TelegramClient.isInitialized()) return;
		TelegramClient.sendMessage("The Night was Skipped");
	}
}
