package ru.roughcipher.pidge.discord;

import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.config.PidgeConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.net.command.TextFormatting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DiscordChatRelay {

	private static final int DISCORD_CONTENT_LIMIT = 2000;
	private static JDAWebhookClient webhookClient = null;

	private static JDAWebhookClient getWebhookClient() {
		if (webhookClient == null) {
			webhookClient = DiscordClient.getWebhook();
		}
		return webhookClient;
	}

	public static void sendToMinecraft(String author, String message) {
		MinecraftServer server = MinecraftServer.getInstance();
		if (server == null || server.playerList == null) {
			Pidge.info("Discord message received but server not ready: " + author + ": " + message);
			return;
		}
		message = "[" + TextFormatting.PURPLE + "D" + TextFormatting.RESET + "] <" + author + "> " + message;
		Pidge.info(message);
		String[] lines = message.split("\n");
		for (String chatMessage : lines) {
			server.playerList.sendEncryptedChatToAllPlayers(chatMessage);
		}
	}

	public static void sendToDiscord(String author, String message) {
		JDAWebhookClient client = getWebhookClient();
		if (client == null) return;
		String fullMessage = author + ": " + message;
		List<String> fragments = splitMessage(fullMessage, DISCORD_CONTENT_LIMIT);
		for (String fragment : fragments) {
			WebhookMessageBuilder builder = new WebhookMessageBuilder();
			builder.setUsername(PidgeConfig.discord_servername);
			builder.setContent(fragment);
			client.send(builder.build());
		}
	}

	public static void sendJoinLeaveMessage(String username, boolean joined) {
		JDAWebhookClient client = getWebhookClient();
		if (client == null) return;
		String joinLeaveText = username + (joined ? " joined" : " left") + " the server";
		WebhookEmbed embed = new WebhookEmbedBuilder()
			.setColor(joined ? 0x00FF00 : 0xFF0000)
			.setAuthor(new WebhookEmbed.EmbedAuthor(joinLeaveText, null, null))
			.build();
		sendMessage(null, embed);
	}

	public static void sendDeathMessage(String deathMessage) {
		JDAWebhookClient client = getWebhookClient();
		if (client == null) return;
		WebhookEmbed embed = new WebhookEmbedBuilder()
			.setColor(0xFF0000)
			.setAuthor(new WebhookEmbed.EmbedAuthor(deathMessage, null, null))
			.build();
		sendMessage(null, embed);
	}

	public static void sendServerStartMessage() {
		JDAWebhookClient client = getWebhookClient();
		if (client == null) return;
		WebhookEmbed embed = new WebhookEmbedBuilder()
			.setColor(0x4ae485)
			.setAuthor(new WebhookEmbed.EmbedAuthor("Server started!", null, null))
			.setTimestamp(Instant.now())
			.build();
		sendMessage(null, embed);
	}

	public static void sendServerStoppedMessage() {
		JDAWebhookClient client = getWebhookClient();
		if (client == null) return;
		WebhookEmbed embed = new WebhookEmbedBuilder()
			.setColor(0xf92f60)
			.setAuthor(new WebhookEmbed.EmbedAuthor("Server stopped!", null, null))
			.setTimestamp(Instant.now())
			.build();
		sendMessage(null, embed);
	}

	public static void sendServerSleepMessage() {
		JDAWebhookClient client = getWebhookClient();
		if (client == null) return;
		WebhookEmbed embed = new WebhookEmbedBuilder()
			.setColor(0x222d5a)
			.setAuthor(new WebhookEmbed.EmbedAuthor("The Night was Skipped", null, null))
			.build();
		sendMessage(null, embed);
	}

	private static void sendMessage(String content, WebhookEmbed embed) {
		JDAWebhookClient client = getWebhookClient();
		if (client == null) return;
		WebhookMessageBuilder builder = new WebhookMessageBuilder();
		builder.setUsername(PidgeConfig.discord_servername);
		if (content != null && !content.isEmpty()) {
			builder.setContent(content);
		}
		if (embed != null) {
			builder.addEmbeds(embed);
		}
		client.send(builder.build());
	}

	private static List<String> splitMessage(String text, int limit) {
		List<String> parts = new ArrayList<>();
		if (text.length() <= limit) {
			parts.add(text);
			return parts;
		}
		int start = 0;
		while (start < text.length()) {
			int end = Math.min(start + limit, text.length());
			if (end < text.length()) {
				int lastSpace = text.lastIndexOf(' ', end);
				if (lastSpace > start) {
					end = lastSpace;
				}
			}
			parts.add(text.substring(start, end));
			start = end;
			while (start < text.length() && text.charAt(start) == ' ') start++;
		}
		return parts;
	}
}
