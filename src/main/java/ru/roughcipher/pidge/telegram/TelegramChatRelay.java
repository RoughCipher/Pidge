package ru.roughcipher.pidge.telegram;

import ru.roughcipher.pidge.util.BaseChatRelay;
import ru.roughcipher.pidge.util.RelayErrorHandler;
import net.minecraft.core.net.command.TextFormatting;

public class TelegramChatRelay extends BaseChatRelay {

	public static final TelegramChatRelay INSTANCE = new TelegramChatRelay();

	@Override
	protected void sendRaw(String message, String context) {
		RelayErrorHandler.sendToTelegram(message, context);
	}

	public void sendToMinecraft(String author, String message) {
		sendToMinecraftInternal(author, message, TextFormatting.Base.LIGHT_BLUE, "T");
	}

	public void sendToTelegram(String author, String message) {
		sendRaw(author + ": " + message, "relay");
	}
}
