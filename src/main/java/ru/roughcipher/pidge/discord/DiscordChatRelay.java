package ru.roughcipher.pidge.discord;

import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.config.PidgeConfig;
import ru.roughcipher.pidge.config.MessageConfig;
import ru.roughcipher.pidge.util.MessageUtils;
import ru.roughcipher.pidge.util.RelayErrorHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.net.command.TextFormatting;
import net.minecraft.core.lang.I18n;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;

public class DiscordChatRelay {

	public static void sendToMinecraft(String author, String message) {
		MinecraftServer server = MinecraftServer.getInstance();
		if (server == null || server.playerList == null) {
			Pidge.info("Discord msg (server not ready): " + author + ": " + message);
			return;
		}
		String cleanMessage = MessageUtils.cleanForMinecraft(message);
		String formatted = "[" + TextFormatting.PURPLE + "D" + TextFormatting.RESET + "] <" + author + "> " + cleanMessage;
		Pidge.info(formatted);
		for (String line : formatted.split("\n")) {
			server.playerList.sendEncryptedChatToAllPlayers(line);
		}
	}

	public static void sendGameMessage(String author, String message) {
		StandardGuildMessageChannel channel = DiscordClient.getChannel();
		if (channel == null) return;
		String full = author + ": " + message;
		String icon = MessageConfig.getGameChatIcon();
		if (icon != null && !icon.isEmpty()) {
			full = icon + " " + full;
		}
		RelayErrorHandler.sendToDiscord(channel, full, "gamechat");
	}

	public static void sendToDiscord(String author, String message) {
		StandardGuildMessageChannel channel = DiscordClient.getChannel();
		if (channel == null) return;
		String full = author + ": " + message;
		RelayErrorHandler.sendToDiscord(channel, full, "chat");
	}

	public static void sendJoinLeaveMessage(String username, boolean joined) {
		StandardGuildMessageChannel channel = DiscordClient.getChannel();
		if (channel == null) return;
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
		String icon = joined ? MessageConfig.getJoinIcon() : MessageConfig.getLeaveIcon();
		RelayErrorHandler.sendToDiscord(channel, MessageUtils.withIcon(icon, text), "joinleave");
	}

	public static void sendKickMessage(String username, String reason) {
		StandardGuildMessageChannel channel = DiscordClient.getChannel();
		if (channel == null) return;
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
		RelayErrorHandler.sendToDiscord(channel, MessageUtils.withIcon(MessageConfig.getKickIcon(), text), "kick");
	}

	public static void sendDeathMessage(String translationKey, Object[] args) {
		StandardGuildMessageChannel channel = DiscordClient.getChannel();
		if (channel == null) return;
		String pattern = I18n.getInstance().translateKey(translationKey);
		String text = String.format(pattern, args);
		String clean = MessageUtils.stripColorCodes(text);
		RelayErrorHandler.sendToDiscord(channel, MessageUtils.withIcon(MessageConfig.getDeathIcon(), clean), "death");
	}

	public static void sendServerStartMessage() {
		StandardGuildMessageChannel channel = DiscordClient.getChannel();
		if (channel == null) return;
		String text = PidgeConfig.getServerName() + "\n" + MessageConfig.getServerStart();
		RelayErrorHandler.sendToDiscord(channel, MessageUtils.withIcon(MessageConfig.getStartIcon(), text), "start");
	}

	public static void sendServerStoppedMessage() {
		StandardGuildMessageChannel channel = DiscordClient.getChannel();
		if (channel == null) return;
		String text = PidgeConfig.getServerName() + "\n" + MessageConfig.getServerStop();
		RelayErrorHandler.sendToDiscord(channel, MessageUtils.withIcon(MessageConfig.getStopIcon(), text), "stop");
	}

	public static void sendServerSleepMessage() {
		StandardGuildMessageChannel channel = DiscordClient.getChannel();
		if (channel == null) return;
		String text = MessageConfig.getNightSkipped();
		RelayErrorHandler.sendToDiscord(channel, MessageUtils.withIcon(MessageConfig.getNightSkippedIcon(), text), "sleep");
	}
}
