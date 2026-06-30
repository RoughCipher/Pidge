package ru.roughcipher.pidge.discord;

import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.config.PidgeConfig;
import ru.roughcipher.pidge.util.MessageUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.net.command.TextFormatting;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;

import java.util.List;

public class DiscordChatRelay {

	public static void sendToMinecraft(String author, String message) {
		MinecraftServer server = MinecraftServer.getInstance();
		if (server == null || server.playerList == null) {
			Pidge.info("Discord msg (server not ready): " + author + ": " + message);
			return;
		}
		String formatted = "[" + TextFormatting.PURPLE + "D" + TextFormatting.RESET + "] <" + author + "> " + message;
		Pidge.info(formatted);
		for (String line : formatted.split("\n")) {
			server.playerList.sendEncryptedChatToAllPlayers(line);
		}
	}

	public static void sendToDiscord(String author, String message) {
		StandardGuildMessageChannel channel = DiscordClient.getChannel();
		if (channel == null) return;

		String fullMessage = author + ": " + message;
		List<String> fragments = MessageUtils.splitMessage(fullMessage, 2000);
		for (String fragment : fragments) {
			channel.sendMessage(fragment).queue();
		}
	}

	public static void sendJoinLeaveMessage(String username, boolean joined) {
		StandardGuildMessageChannel channel = DiscordClient.getChannel();
		if (channel == null) return;
		String text = PidgeConfig.getServerName() + "\n" + username + (joined ? " joined" : " left") + " the server";
		channel.sendMessage(text).queue();
	}

	public static void sendDeathMessage(String deathMessage) {
		StandardGuildMessageChannel channel = DiscordClient.getChannel();
		if (channel == null) return;
		String text = PidgeConfig.getServerName() + "\n" + deathMessage;
		channel.sendMessage(text).queue();
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
		String text = PidgeConfig.getServerName() + "\nThe Night was Skipped";
		channel.sendMessage(text).queue();
	}
}
