package ru.roughcipher.pidge.telegram;

import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.config.PidgeConfig;
import ru.roughcipher.pidge.config.MessageConfig;
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
		String text;
		if (joined && MessageConfig.getPlayerJoined() != null) {
			text = String.format(MessageConfig.getPlayerJoined(), username);
		} else if (!joined && MessageConfig.getPlayerLeft() != null) {
			text = String.format(MessageConfig.getPlayerLeft(), username);
		} else {
			String key = joined ? "messages.player_joined" : "messages.player_left";
			String pattern = I18n.getInstance().translateKey(key);
			text = String.format(pattern, username);
		}
		RelayErrorHandler.sendToTelegram(text, "joinleave");
	}

	public static void sendKickMessage(String username, String reason) {
		String text;
		if (MessageConfig.getPlayerKicked() != null) {
			String reasonText = (reason != null && !reason.isEmpty()) ? reason : "";
			text = String.format(MessageConfig.getPlayerKicked(), username, reasonText);
		} else {
			String pattern = I18n.getInstance().translateKey("messages.player_kicked");
			text = String.format(pattern, username);
			if (reason != null && !reason.isEmpty()) {
				text += " (" + reason + ")";
			}
		}
		RelayErrorHandler.sendToTelegram(text, "kick");
	}

	public static void sendDeathMessage(String translationKey, Object[] args) {
		String pattern = I18n.getInstance().translateKey(translationKey);
		String translated = String.format(pattern, args);
		String clean = MessageUtils.stripColorCodes(translated);
		RelayErrorHandler.sendToTelegram(clean, "death");
	}

	public static void sendServerStartMessage() {
		RelayErrorHandler.sendToTelegram(PidgeConfig.getServerName() + "\n" + MessageConfig.getServerStart(), "start");
	}

	public static void sendServerStoppedMessage() {
		RelayErrorHandler.sendToTelegram(PidgeConfig.getServerName() + "\n" + MessageConfig.getServerStop(), "stop");
	}

	public static void sendServerSleepMessage() {
		RelayErrorHandler.sendToTelegram(MessageConfig.getNightSkipped(), "sleep");
	}
}
