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
        String fullMessage = PidgeConfig.getServerName() + "\n" + author + ": " + message;
        TelegramClient.sendMessage(fullMessage);
    }

    public static void sendJoinLeaveMessage(String username, boolean joined) {
        if (!TelegramClient.isInitialized()) return;
        String text = PidgeConfig.getServerName() + "\n" + username + (joined ? " joined" : " left") + " the server";
        TelegramClient.sendMessage(text);
    }

    public static void sendDeathMessage(String deathMessage) {
        if (!TelegramClient.isInitialized()) return;
        String text = PidgeConfig.getServerName() + "\n" + deathMessage;
        TelegramClient.sendMessage(text);
    }

    public static void sendServerStartMessage() {
        if (!TelegramClient.isInitialized()) return;
        String text = PidgeConfig.getServerName() + "\nServer started!";
        TelegramClient.sendMessage(text);
    }

    public static void sendServerStoppedMessage() {
        if (!TelegramClient.isInitialized()) return;
        String text = PidgeConfig.getServerName() + "\nServer stopped!";
        TelegramClient.sendMessage(text);
    }

    public static void sendServerSleepMessage() {
        if (!TelegramClient.isInitialized()) return;
        String text = PidgeConfig.getServerName() + "\nThe Night was Skipped";
        TelegramClient.sendMessage(text);
    }
}