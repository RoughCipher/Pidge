package ru.roughcipher.pidge.discord;

import ru.roughcipher.pidge.util.BaseChatRelay;
import ru.roughcipher.pidge.util.RelayErrorHandler;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.minecraft.core.net.command.TextFormatting;

public class DiscordChatRelay extends BaseChatRelay {

	public static final DiscordChatRelay INSTANCE = new DiscordChatRelay();

	@Override
	protected void sendRaw(String message, String context) {
		StandardGuildMessageChannel channel = DiscordClient.getChannel();
		if (channel == null) return;
		RelayErrorHandler.sendToDiscord(channel, message, context);
	}

	public void sendToMinecraft(String author, String message) {
		sendToMinecraftInternal(author, message, TextFormatting.Base.PURPLE, "D");
	}

	public void sendToDiscord(String author, String message) {
		sendRaw(author + ": " + message, "relay");
	}
}
