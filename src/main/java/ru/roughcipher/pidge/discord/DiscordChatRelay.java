package ru.roughcipher.pidge.discord;

import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.config.PidgeConfig;
import ru.roughcipher.pidge.util.MessageUtils;
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

	public static void sendToDiscord(String author, String message) {
		StandardGuildMessageChannel channel = DiscordClient.getChannel();
		if (channel == null) return;
		String fullMessage = author + ": " + message;
		for (String fragment : MessageUtils.splitMessage(fullMessage, 2000)) {
			channel.sendMessage(fragment).queue();
		}
	}

	public static void sendJoinLeaveMessage(String username, boolean joined) {
		StandardGuildMessageChannel channel = DiscordClient.getChannel();
		if (channel == null) return;
		String key = joined ? "messages.player_joined" : "messages.player_left";
		String pattern = I18n.getInstance().translateKey(key);
		String text = String.format(pattern, username);
		channel.sendMessage(text).queue();
	}

	public static void sendDeathMessage(String translationKey, Object[] args) {
		StandardGuildMessageChannel channel = DiscordClient.getChannel();
		if (channel == null) return;
		String pattern = I18n.getInstance().translateKey(translationKey);
		String translated = String.format(pattern, args);
		String clean = MessageUtils.stripColorCodes(translated);
		channel.sendMessage(clean).queue();
	}

	public static void sendServerStartMessage() {
		StandardGuildMessageChannel channel = DiscordClient.getChannel();
		if (channel == null) return;
		String text = PidgeConfig.getServerName() + "\nServer started!";
		channel.sendMessage(text).queue();
	}

	public static void sendServerStoppedMessage() {
		StandardGuildMessageChannel channel = DiscordClient.getChannel();
		if (channel == null) return;
		String text = PidgeConfig.getServerName() + "\nServer stopped!";
		channel.sendMessage(text).queue();
	}

	public static void sendServerSleepMessage() {
		StandardGuildMessageChannel channel = DiscordClient.getChannel();
		if (channel == null) return;
		channel.sendMessage("The Night was Skipped").queue();
	}
}
