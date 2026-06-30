package ru.roughcipher.pidge.discord;

import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import ru.roughcipher.pidge.Pidge;
import ru.roughcipher.pidge.config.PidgeConfig;
import ru.roughcipher.pidge.util.MessageUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.net.command.TextFormatting;

import java.time.Instant;

public class DiscordChatRelay {
    private static final int DISCORD_CONTENT_LIMIT = 2000;

    private static JDAWebhookClient getWebhookClient() {
        return DiscordClient.getWebhook();
    }

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
        JDAWebhookClient client = getWebhookClient();
        if (client == null) return;

        String full = author + ": " + message;
        for (String fragment : MessageUtils.splitMessage(full, DISCORD_CONTENT_LIMIT)) {
            WebhookMessageBuilder builder = new WebhookMessageBuilder();
            builder.setUsername(PidgeConfig.getServerName());
            builder.setContent(fragment);
            try {
                client.send(builder.build());
            } catch (Exception e) {
                Pidge.LOGGER.error("Failed to send message to Discord", e);
            }
        }
    }

    public static void sendJoinLeaveMessage(String username, boolean joined) {
        JDAWebhookClient client = getWebhookClient();
        if (client == null) return;
        String text = username + (joined ? " joined" : " left") + " the server";
        WebhookEmbed embed = new WebhookEmbedBuilder()
                .setColor(joined ? 0x00FF00 : 0xFF0000)
                .setAuthor(new WebhookEmbed.EmbedAuthor(text, null, null))
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
        builder.setUsername(PidgeConfig.getServerName());
        if (content != null && !content.isEmpty()) {
            builder.setContent(content);
        }
        if (embed != null) {
            builder.addEmbeds(embed);
        }
        try {
            client.send(builder.build());
        } catch (Exception e) {
            Pidge.LOGGER.error("Failed to send webhook message", e);
        }
    }
}
