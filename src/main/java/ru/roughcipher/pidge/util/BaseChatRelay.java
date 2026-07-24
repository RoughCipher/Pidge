package ru.roughcipher.pidge.util;

import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.config.MessageConfig;
import ru.roughcipher.pidge.config.PidgeConfig;
import net.minecraft.core.net.command.TextFormatting;
import net.minecraft.core.lang.I18n;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.entity.player.PlayerServer;

import java.util.List;

public abstract class BaseChatRelay {

	protected abstract void sendRaw(String message, String context);

	protected static void sendToMinecraftInternal(String author, String message, TextFormatting.Base color, String prefix) {
		MinecraftServer server = MinecraftServer.getInstance();
		if (server == null || server.playerList == null) {
			Pidge.info("Message received but server not ready: " + author + ": " + message);
			return;
		}
		String cleanMessage = MessageUtils.cleanForMinecraft(message);
		String formatted = "[" + color + prefix + TextFormatting.RESET + "] <" + author + "> " + cleanMessage;
		Pidge.info(formatted);
		for (String line : formatted.split("\n")) {
			server.playerList.sendEncryptedChatToAllPlayers(line);
		}
	}

	public static String getPlayerListString() {
		MinecraftServer server = MinecraftServer.getInstance();
		if (server == null || server.playerList == null) {
			return I18n.getInstance().translateKey("command.commands.list.exception_failure");
		}
		List<PlayerServer> players = server.playerList.playerEntities;
		int count = players.size();
		if (count == 0) {
			return I18n.getInstance().translateKey("command.commands.list.exception_failure");
		}
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < count; i++) {
			if (i > 0) builder.append(", ");
			PlayerServer player = players.get(i);
			String username = player.username;
			String nickname = player.nickname;
			if (nickname != null && !nickname.isEmpty()) {
				String cleanNick = MessageUtils.stripColorCodes(nickname);
				builder.append(cleanNick).append(" (").append(username).append(")");
			} else {
				builder.append(username);
			}
		}
		String list = MessageUtils.stripColorCodes(builder.toString());
		if (count == 1) {
			return String.format(I18n.getInstance().translateKey("command.commands.list.success_single"), count, list);
		} else {
			return String.format(I18n.getInstance().translateKey("command.commands.list.success_multiple"), count, list);
		}
	}

	public void sendGameMessage(String author, String message) {
		String full = author + ": " + message;
		String icon = MessageConfig.getGameChatIcon();
		if (icon != null && !icon.isEmpty()) {
			full = icon + " " + full;
		}
		sendRaw(full, "gamechat");
	}

	public void sendJoinLeaveMessage(String username, boolean joined) {
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
		sendRaw(MessageUtils.withIcon(icon, text), "joinleave");
	}

	public void sendKickMessage(String username, String reason) {
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
		sendRaw(MessageUtils.withIcon(MessageConfig.getKickIcon(), text), "kick");
	}

	public void sendDeathMessage(String translationKey, Object[] args) {
		String pattern = I18n.getInstance().translateKey(translationKey);
		String text = String.format(pattern, args);
		String clean = MessageUtils.stripColorCodes(text);
		sendRaw(MessageUtils.withIcon(MessageConfig.getDeathIcon(), clean), "death");
	}

	public void sendServerStartMessage() {
		String version = Pidge.getBTAVersion();
		String text = PidgeConfig.getServerName() + " (v" + version + ")\n" + MessageConfig.getServerStart();
		sendRaw(MessageUtils.withIcon(MessageConfig.getStartIcon(), text), "start");
	}

	public void sendServerStoppedMessage() {
		String text = PidgeConfig.getServerName() + "\n" + MessageConfig.getServerStop();
		sendRaw(MessageUtils.withIcon(MessageConfig.getStopIcon(), text), "stop");
	}

	public void sendServerSleepMessage() {
		String text = MessageConfig.getNightSkipped();
		sendRaw(MessageUtils.withIcon(MessageConfig.getNightSkippedIcon(), text), "sleep");
	}
}
