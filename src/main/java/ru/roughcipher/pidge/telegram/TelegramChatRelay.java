package ru.roughcipher.pidge.telegram;

import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.config.PidgeConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.net.command.TextFormatting;

public class TelegramChatRelay {

	public static void sendToMinecraft(String author, String message) {
		MinecraftServer server = MinecraftServer.getInstance();
		if (server == null || server.playerList == null) {
			Pidge.info("Telegram message received but server not ready: " + author + ": " + message);
			return;
		}
		String formatted = "[" + TextFormatting.LIGHT_BLUE + "T" + TextFormatting.RESET + "] <" + author + "> " + message;
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
		TelegramClient.sendMessage(username + (joined ? " joined" : " left") + " the server");
	}

	public static void sendDeathMessage(String deathMessage) {
		if (!TelegramClient.isInitialized()) return;
		TelegramClient.sendMessage(deathMessage);
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
